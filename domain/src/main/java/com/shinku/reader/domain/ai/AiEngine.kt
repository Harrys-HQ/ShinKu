package com.shinku.reader.domain.ai

import android.content.Context
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

sealed interface AiEngine {
    val id: String
    val name: String
    val isAvailable: Boolean

    suspend fun generateText(prompt: String): Result<String>
    suspend fun generateTitles(prompt: String): Result<List<String>>
}

class GoogleAiCoreEngine(
    private val context: Context,
) : AiEngine {
    override val id: String = "aicore"
    override val name: String = "Native OS AI (Google AICore / OxygenOS / Galaxy AI)"

    override val isAvailable: Boolean
        get() {
            return try {
                val pm = context.packageManager
                val hasAiCorePackage = pm.getLaunchIntentForPackage("com.google.android.aicore") != null ||
                    pm.getLaunchIntentForPackage("com.coloros.ai") != null ||
                    pm.getLaunchIntentForPackage("com.samsung.android.rubin.app") != null
                
                val hasSystemFeature = android.os.Build.VERSION.SDK_INT >= 34
                hasAiCorePackage || hasSystemFeature
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun generateText(prompt: String): Result<String> {
        return Result.failure(UnsupportedOperationException("AICore runtime pending system service binding"))
    }

    override suspend fun generateTitles(prompt: String): Result<List<String>> {
        return Result.failure(UnsupportedOperationException("AICore runtime pending system service binding"))
    }
}

class CloudGeminiEngine(
    private val networkHelper: NetworkHelper,
    private val shinkuPreferences: ShinKuPreferences,
) : AiEngine {
    override val id: String = "cloud"
    override val name: String = "Google Gemini (Cloud API)"

    override val isAvailable: Boolean
        get() = shinkuPreferences.geminiApiKey().get().isNotBlank()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = shinkuPreferences.geminiApiKey().get()
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalStateException("API Key not set"))

        try {
            val model = resolveModel(shinkuPreferences.geminiModel().get())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val bodyJson = """
                {
                  "contents": [{
                    "parts":[{"text": ${Json.encodeToString(prompt)}}]
                  }]
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            networkHelper.client.newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
                }
                val result = json.parseToJsonElement(responseBody)
                val candidate = result.jsonObject["candidates"]?.jsonArray?.getOrNull(0)
                val text = candidate?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.content
                if (text != null) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Empty content from Gemini response"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateTitles(prompt: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val textResult = generateText(prompt)
        textResult.map { text ->
            try {
                val start = text.indexOf("[")
                val end = text.lastIndexOf("]") + 1
                if (start != -1 && end > start) {
                    val jsonArray = text.substring(start, end)
                    json.decodeFromString<List<String>>(jsonArray)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun resolveModel(model: String): String {
        return when (model) {
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-2.0-flash",
            "gemini-2.0-pro",
            "gemini-2.0-flash-exp",
            "gemini-3.0-flash",
            "gemini-3.0-pro",
            "gemini-3.1-pro" -> "gemini-3.5-flash"
            else -> model
        }
    }
}

class AiEngineRegistry(
    private val engines: List<AiEngine>,
    private val shinkuPreferences: ShinKuPreferences,
) {
    fun getAvailableEngines(): List<AiEngine> {
        return engines.filter { it.isAvailable }
    }

    fun getActiveEngine(): AiEngine {
        val preferred = shinkuPreferences.aiEngineProvider().get()
        if (preferred != "auto") {
            engines.firstOrNull { it.id == preferred }?.let { return it }
        }
        val nativeEngine = engines.firstOrNull { it.id == "aicore" && it.isAvailable }
        if (nativeEngine != null) return nativeEngine

        return engines.firstOrNull { it.id == "cloud" } ?: engines.first()
    }
}
