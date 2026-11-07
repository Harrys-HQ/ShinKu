package com.shinku.reader.domain.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Shared by discovery, translation and the settings connection check. */
object GeminiModels {
    const val DEFAULT = "gemini-3.5-flash-lite"

    fun resolve(rawModel: String): String {
        val model = rawModel.trim().removePrefix("models/")
        if (model.isBlank() || model.equals("auto", ignoreCase = true)) return DEFAULT
        require(model.matches(Regex("gemini-[A-Za-z0-9._-]+"))) { "Enter a Gemini model ID, such as $DEFAULT." }
        return model
    }

    fun isAutomatic(rawModel: String) = rawModel.isBlank() || rawModel.trim().equals("auto", ignoreCase = true)

    fun isLegacy(model: String) = model.startsWith("gemini-1.") || model.startsWith("gemini-2.")
}

data class GeminiText(val model: String, val text: String)
data class GeminiModel(val id: String, val displayName: String)

class GeminiApiException(val statusCode: Int, message: String) : IOException(message)

/** Stateless generateContent calls; API keys are sent in a header, never in the URL. */
class GeminiApi(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl = "https://generativelanguage.googleapis.com/v1beta/".toHttpUrl(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** The catalog describes listed capability, not successful inference, quota or billing. */
    fun listTextModels(apiKey: String, forceRefresh: Boolean = false): List<GeminiModel> {
        val key = apiKey.trim()
        require(key.isNotBlank()) { "Gemini API key is not configured." }
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        val cacheKey = CatalogKey(client, baseUrl.toString(), digest)
        if (forceRefresh) synchronized(catalogCache) { catalogCache.remove(cacheKey) }
        if (!forceRefresh) {
            synchronized(catalogCache) {
                catalogCache[cacheKey]?.takeIf { System.nanoTime() - it.createdAt < TimeUnit.MINUTES.toNanos(10) }
                    ?.let { return it.models }
            }
        }
        val models = linkedMapOf<String, GeminiModel>()
        val seenTokens = mutableSetOf<String>()
        var pageToken: String? = null
        var pages = 0
        do {
            if (++pages > 20) throw IOException("Gemini model catalog exceeded the pagination limit. Retry loading models.")
            val url = baseUrl.newBuilder().addPathSegment("models").addQueryParameter("pageSize", "1000")
                .apply { pageToken?.let { addQueryParameter("pageToken", it) } }.build()
            val request = Request.Builder().url(url).header("x-goog-api-key", key).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw apiError(response.code, response.body.string(), null)
                val body = json.parseToJsonElement(response.body.string()).jsonObject
                for (item in body["models"]?.jsonArray.orEmpty()) {
                    val data = item.jsonObject
                    val id = (data["name"] as? JsonPrimitive)?.content?.removePrefix("models/") ?: continue
                    val textMethod = data["supportedGenerationMethods"]?.jsonArray.orEmpty()
                        .any { (it as? JsonPrimitive)?.content == "generateContent" }
                    val specialized = Regex("(^|-)(image|audio|tts|live|transcribe|embedding|banana)(-|$)").containsMatchIn(id)
                    if (!textMethod || specialized || !id.matches(Regex("gemini-[A-Za-z0-9._-]+"))) continue
                    models[id] = GeminiModel(id, (data["displayName"] as? JsonPrimitive)?.content ?: id)
                }
                pageToken = (body["nextPageToken"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                if (pageToken != null && !seenTokens.add(pageToken!!)) throw IOException("Gemini returned a repeated catalog page. Retry loading models.")
            }
        } while (pageToken != null)
        val ranked = models.values.sortedWith(
            compareBy<GeminiModel> { currentModelRank(it.id) }
                .thenBy { it.id.contains("preview") || it.id.contains("exp") }
                .thenBy { !it.id.contains("flash") }
                .thenBy { it.id },
        )
        synchronized(catalogCache) { catalogCache[cacheKey] = CachedCatalog(System.nanoTime(), ranked) }
        return ranked
    }

    fun generateText(prompt: String, apiKey: String, model: String, allowFallback: Boolean = true): GeminiText =
        generate(listOf(buildJsonObject { put("text", prompt) }), apiKey, model, allowFallback)

    fun testConnection(apiKey: String, model: String): GeminiText =
        generateText("Respond with 'OK'", apiKey, model, allowFallback = GeminiModels.isAutomatic(model))

    fun generateImageText(prompt: String, base64Image: String, apiKey: String, model: String): GeminiText = generate(
        listOf(
            buildJsonObject { put("text", prompt) },
            buildJsonObject {
                put(
                    "inline_data",
                    buildJsonObject {
                        put("mime_type", "image/jpeg")
                        put("data", base64Image)
                    },
                )
            },
        ),
        apiKey,
        model,
        allowFallback = true,
    )

    private fun generate(parts: List<JsonObject>, apiKey: String, rawModel: String, allowFallback: Boolean): GeminiText {
        val key = apiKey.trim()
        require(key.isNotBlank()) { "Gemini API key is not configured." }
        val automatic = GeminiModels.isAutomatic(rawModel)
        val selected = GeminiModels.resolve(rawModel)
        val models = ArrayDeque(
            if (automatic) {
                val listed = listTextModels(key).map { it.id }
                if (listed.isEmpty()) throw IOException("No Gemini text models were listed for this API key. Check the key and project access.")
                listed.take(if (allowFallback) 3 else 1)
            } else {
                listOf(selected)
            },
        )
        val attempted = mutableSetOf<String>()
        val body = buildJsonObject {
            put(
                "contents",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("role", "user")
                            put("parts", JsonArray(parts))
                        },
                    ),
                ),
            )
        }.toString().toRequestBody("application/json".toMediaType())
        var lastError: GeminiApiException? = null
        while (models.isNotEmpty() && attempted.size < 3) {
            val model = models.removeFirst()
            if (!attempted.add(model)) continue
            val request = Request.Builder()
                .url(baseUrl.newBuilder().addPathSegment("models").addPathSegment("$model:generateContent").build())
                .header("x-goog-api-key", key)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val error = apiError(response.code, response.body.string(), model)
                    if (response.code !in listOf(404, 500, 503)) throw error
                    lastError = error
                    if (allowFallback && !automatic && GeminiModels.isLegacy(selected) && attempted.size == 1) {
                        models.addAll(listTextModels(key).map { it.id }.filterNot { it in attempted }.take(2))
                    }
                } else {
                    val result = json.parseToJsonElement(response.body.string()).jsonObject
                    val content = result["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject?.get("parts")?.jsonArray
                    val text = content.orEmpty().mapNotNull { part ->
                        val value = part.jsonObject
                        if (value["thought"]?.jsonPrimitive?.booleanOrNull == true) {
                            null
                        } else {
                            (value["text"] as? JsonPrimitive)?.content
                        }
                    }.joinToString("").trim()
                    if (text.isBlank()) throw IOException("Gemini returned no text. The response may have been blocked.")
                    return GeminiText(model, text)
                }
            }
        }
        throw checkNotNull(lastError)
    }

    private fun apiError(code: Int, body: String, model: String?): GeminiApiException {
        val error = runCatching { json.parseToJsonElement(body).jsonObject["error"]?.jsonObject }.getOrNull()
        fun safeCode(value: String?) = value?.takeIf { it.matches(Regex("[A-Z][A-Z0-9_]{0,79}")) }
        val status = safeCode((error?.get("status") as? JsonPrimitive)?.content)
        val reason = runCatching {
            error?.get("details")?.jsonArray?.firstNotNullOfOrNull {
                safeCode((it.jsonObject["reason"] as? JsonPrimitive)?.content)
            }
        }.getOrNull()
        val detail = listOfNotNull(status, reason).distinct().joinToString(" / ").let { if (it.isBlank()) "" else " ($it)" }
        val message = when (code) {
            400 -> "Gemini rejected the request. Check the API key, model ID and project configuration."
            404 -> "Gemini model ${model ?: "catalog"} is unavailable. Load models for this API key or choose Automatic."
            401, 403 -> "Gemini authentication or access failed. Check the API key and project permissions."
            429 -> "Gemini quota or rate limit reached. Check the project's quota before retrying."
            500, 503 -> "Gemini is temporarily unavailable. Retry later or select another listed model."
            else -> "Gemini request failed. Check the project's API configuration."
        }
        return GeminiApiException(code, "HTTP $code$detail: $message")
    }

    private fun currentModelRank(id: String): Int = listOf(
        "gemini-3.5-flash-lite", "gemini-3.6-flash", "gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.1-flash-lite",
    ).indexOf(id).let { if (it < 0) Int.MAX_VALUE else it }

    private data class CatalogKey(val client: OkHttpClient, val baseUrl: String, val keyDigest: String)
    private data class CachedCatalog(val createdAt: Long, val models: List<GeminiModel>)

    companion object {
        private val catalogCache = object : LinkedHashMap<CatalogKey, CachedCatalog>(8, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CatalogKey, CachedCatalog>?) = size > 8
        }
    }
}
