package com.shinku.reader.domain.ai

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

class OpenAiCompatibleEngine(
    private val networkHelper: NetworkHelper,
    private val shinkuPreferences: ShinKuPreferences,
) : AiEngine {
    override val id: String = "openai_compatible"
    override val name: String = "OpenAI-Compatible (OpenRouter / DeepSeek / Groq / OpenAI / Custom)"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class ResolvedConfig(
        val provider: String,
        val baseUrl: String,
        val apiKey: String,
        val model: String,
        val isOpenRouter: Boolean,
    )

    fun resolveConfig(): ResolvedConfig {
        val provider = shinkuPreferences.aiEngineProvider().get()
        return when (provider) {
            "openrouter" -> {
                val modelPref = shinkuPreferences.openRouterModel().get()
                ResolvedConfig(
                    provider = "openrouter",
                    baseUrl = "https://openrouter.ai/api/v1",
                    apiKey = shinkuPreferences.openRouterApiKey().get().trim(),
                    model = resolveModel(modelPref, "openrouter"),
                    isOpenRouter = true,
                )
            }
            "deepseek" -> {
                val modelPref = shinkuPreferences.openAiModel().get()
                ResolvedConfig(
                    provider = "deepseek",
                    baseUrl = "https://api.deepseek.com/v1",
                    apiKey = shinkuPreferences.openAiApiKey().get().trim(),
                    model = resolveModel(modelPref, "deepseek"),
                    isOpenRouter = false,
                )
            }
            "groq" -> {
                val modelPref = shinkuPreferences.openAiModel().get()
                ResolvedConfig(
                    provider = "groq",
                    baseUrl = "https://api.groq.com/openai/v1",
                    apiKey = shinkuPreferences.openAiApiKey().get().trim(),
                    model = resolveModel(modelPref, "groq"),
                    isOpenRouter = false,
                )
            }
            "openai" -> {
                val modelPref = shinkuPreferences.openAiModel().get()
                ResolvedConfig(
                    provider = "openai",
                    baseUrl = "https://api.openai.com/v1",
                    apiKey = shinkuPreferences.openAiApiKey().get().trim(),
                    model = resolveModel(modelPref, "openai"),
                    isOpenRouter = false,
                )
            }
            "custom" -> {
                val customUrl = shinkuPreferences.openAiBaseUrl().get().trim().ifBlank { "https://api.openai.com/v1" }
                val modelPref = shinkuPreferences.openAiModel().get()
                ResolvedConfig(
                    provider = "custom",
                    baseUrl = customUrl.removeSuffix("/"),
                    apiKey = shinkuPreferences.openAiApiKey().get().trim(),
                    model = resolveModel(modelPref, "custom"),
                    isOpenRouter = customUrl.contains("openrouter.ai", ignoreCase = true),
                )
            }
            else -> {
                // Default fallback to OpenRouter config
                val modelPref = shinkuPreferences.openRouterModel().get()
                ResolvedConfig(
                    provider = "openrouter",
                    baseUrl = "https://openrouter.ai/api/v1",
                    apiKey = shinkuPreferences.openRouterApiKey().get().trim(),
                    model = resolveModel(modelPref, "openrouter"),
                    isOpenRouter = true,
                )
            }
        }
    }

    override val isAvailable: Boolean
        get() {
            val config = resolveConfig()
            // In custom setups (e.g. local Ollama), API key might be empty but URL is present
            return if (config.provider == "custom") {
                config.baseUrl.isNotBlank()
            } else {
                config.apiKey.isNotBlank()
            }
        }

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val config = resolveConfig()
        if (!isAvailable) {
            return@withContext Result.failure(IllegalStateException("API Key not set for ${config.provider}"))
        }

        val endpoint = if (config.baseUrl.endsWith("/chat/completions")) {
            config.baseUrl
        } else {
            "${config.baseUrl.removeSuffix("/")}/chat/completions"
        }

        val modelsToTry = when (config.provider) {
            "openrouter" -> listOf(
                config.model,
                "google/gemini-2.0-flash-001",
                "openai/gpt-4o-mini",
                "meta-llama/llama-3.3-70b-instruct",
            ).distinct()
            "groq" -> listOf(
                config.model,
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
            ).distinct()
            "deepseek" -> listOf(
                config.model,
                "deepseek-chat",
            ).distinct()
            else -> listOf(config.model).distinct()
        }

        var lastError: Exception? = null
        for (model in modelsToTry) {
            try {
                val bodyJson = """
                    {
                      "model": ${Json.encodeToString(model)},
                      "messages": [
                        {
                          "role": "user",
                          "content": ${Json.encodeToString(prompt)}
                        }
                      ]
                    }
                """.trimIndent()

                val reqBuilder = Request.Builder()
                    .url(endpoint)
                    .addHeader("Content-Type", "application/json")
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))

                if (config.apiKey.isNotBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
                }

                if (config.isOpenRouter) {
                    reqBuilder.addHeader("HTTP-Referer", "https://shinku.app")
                    reqBuilder.addHeader("X-Title", "ShinKu Reader")
                }

                networkHelper.client.newCall(reqBuilder.build()).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        val parsedMessage = try {
                            val errObj = json.parseToJsonElement(responseBody).jsonObject["error"]?.jsonObject
                            errObj?.get("message")?.jsonPrimitive?.content
                        } catch (_: Exception) {
                            null
                        }
                        val detail = parsedMessage ?: responseBody
                        if (response.code in listOf(400, 404, 429, 500, 502, 503)) {
                            lastError = Exception("HTTP ${response.code}: $detail")
                            return@use // try fallback model
                        }
                        return@withContext Result.failure(Exception("HTTP ${response.code}: $detail"))
                    }

                    val result = json.parseToJsonElement(responseBody)
                    val text = result.jsonObject["choices"]?.jsonArray?.getOrNull(0)
                        ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                    if (!text.isNullOrBlank()) {
                        return@withContext Result.success(text)
                    } else {
                        return@withContext Result.failure(Exception("Empty message content returned from LLM"))
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: Exception("Failed to generate text from ${config.provider}"))
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
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    companion object {
        fun resolveModel(model: String, provider: String): String {
            val trimmed = model.trim()
            if (trimmed.isNotBlank() && !trimmed.equals("auto", ignoreCase = true)) {
                return trimmed
            }
            return when (provider) {
                "openrouter" -> "google/gemini-2.0-flash-001"
                "deepseek" -> "deepseek-chat"
                "groq" -> "llama-3.3-70b-versatile"
                "openai" -> "gpt-4o-mini"
                else -> "gpt-4o-mini"
            }
        }
    }
}
