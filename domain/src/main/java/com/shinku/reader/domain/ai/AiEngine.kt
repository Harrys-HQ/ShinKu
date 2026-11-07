package com.shinku.reader.domain.ai

import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

sealed interface AiEngine {
    val id: String
    val name: String
    val isAvailable: Boolean

    suspend fun generateText(prompt: String): Result<String>
    suspend fun generateTitles(prompt: String): Result<List<String>>
}

class CloudGeminiEngine(
    private val networkHelper: NetworkHelper,
    private val shinkuPreferences: ShinKuPreferences,
) : AiEngine {
    override val id: String = "cloud"
    override val name: String = "Google Gemini (Cloud API)"

    override val isAvailable: Boolean
        get() = shinkuPreferences.geminiApiKey().get().isNotBlank()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success(
                GeminiApi(networkHelper.client).generateText(
                    prompt, shinkuPreferences.geminiApiKey().get(), shinkuPreferences.geminiModel().get(),
                ).text,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
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
        if (preferred in listOf("openrouter", "deepseek", "groq", "openai", "custom", "openai_compatible")) {
            engines.firstOrNull { it.id == "openai_compatible" }?.let { return it }
        }
        if (preferred == "cloud" || preferred == "gemini") {
            engines.firstOrNull { it.id == "cloud" }?.let { return it }
        }

        val cloudEngine = engines.firstOrNull { it.id == "cloud" }
        if (cloudEngine?.isAvailable == true) return cloudEngine

        val openAiEngine = engines.firstOrNull { it.id == "openai_compatible" }
        if (openAiEngine?.isAvailable == true) return openAiEngine

        return engines.firstOrNull { it.isAvailable } ?: cloudEngine ?: engines.first()
    }
}
