package com.shinku.reader.domain.source.interactor

import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GeminiVibeSearch(
    private val networkHelper: NetworkHelper,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val shinkuPreferences: ShinKuPreferences = Injekt.get()
    private val aiEngineRegistry: com.shinku.reader.domain.ai.AiEngineRegistry = Injekt.get()

    private val vibeCache = java.util.Collections.synchronizedMap(object : LinkedHashMap<String, List<String>>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean = size > 50
    })

    private val similarCache = java.util.Collections.synchronizedMap(object : LinkedHashMap<String, List<String>>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean = size > 50
    })

    private val recapCache = java.util.Collections.synchronizedMap(object : LinkedHashMap<String, String>(30, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 30
    })

    private val translationCache = java.util.Collections.synchronizedMap(object : LinkedHashMap<String, List<TranslationBlockOutput>>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<TranslationBlockOutput>>?): Boolean = size > 50
    })

    suspend fun getMangaTitles(query: String, apiKey: String, model: String): List<String> {
        val cacheKey = query.trim().lowercase()
        vibeCache[cacheKey]?.let { return it }

        return withIOContext {
            try {
                val activeEngine = aiEngineRegistry.getActiveEngine()
                val prompt = """
                    You are a manga discovery expert. Based on the following user description, provide a list of up to 10 real manga titles that match the "vibe".
                    User Description: "$query"
                    Return ONLY a JSON array of strings containing the titles. No extra text.
                    Example: ["Title 1", "Title 2"]
                """.trimIndent()
                val result = activeEngine.generateTitles(prompt)
                val titles = result.getOrDefault(callGemini(query, apiKey, model))
                if (titles.isNotEmpty()) {
                    vibeCache[cacheKey] = titles
                }
                titles
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getLatestUserAgent(apiKey: String, model: String, currentUa: String): String {
        return withIOContext {
            if (!shinkuPreferences.isAiConfigured() && apiKey.isBlank()) return@withIOContext ""
            try {
                val prompt = "Based on this User-Agent string: '$currentUa', provide the latest stable version of it for the same browser and platform. Return ONLY the updated User-Agent string, no extra text."
                val text = callGeminiForText(prompt, apiKey, model)
                if (text.startsWith("Error:")) "" else text.trim().removeSurrounding("\"")
            } catch (e: Exception) {
                ""
            }
        }
    }

    suspend fun getSimilarManga(title: String, description: String, apiKey: String, model: String): List<String> {
        val cacheKey = "$title::$description".lowercase()
        similarCache[cacheKey]?.let { return it }

        return withIOContext {
            if (!shinkuPreferences.isAiConfigured() && apiKey.isBlank()) return@withIOContext emptyList()
            try {
                val prompt = """
                    Based on the manga title "$title" and its description: "$description",
                    suggest 5-10 other real manga titles that have a similar "vibe", tone, or themes.
                    Return ONLY a JSON array of strings.
                """.trimIndent()
                val text = callGeminiForText(prompt, apiKey, model)
                val start = text.indexOf("[")
                val end = text.lastIndexOf("]") + 1
                if (start != -1 && end > start) {
                    val jsonArray = text.substring(start, end)
                    val titles = json.decodeFromString<List<String>>(jsonArray)
                    if (titles.isNotEmpty()) {
                        similarCache[cacheKey] = titles
                    }
                    titles
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun explainFootnotes(text: String, apiKey: String, model: String): String {
        return callGeminiForText(
            "Explain any cultural references, puns, or interesting nuances in this manga text: \"$text\". Be concise.",
            apiKey,
            model,
        )
    }

    fun cleanMangaTitle(title: String): String {
        return title
            .replace("\\[.*?]".toRegex(), "")
            .replace("\\(.*?\\)".toRegex(), "")
            .replace("\\{.*?\\}".toRegex(), "")
            .replace("(?i)^(\\s*ch\\.?\\s*\\d+.*?-|chapter\\s*\\d+.*?-)", "")
            .trim()
            .ifBlank { title }
    }

    suspend fun explainCharacterAndLoreCodex(
        text: String,
        mangaTitle: String,
        chapterName: String,
        apiKey: String,
        model: String,
        synopsis: String? = null,
    ): String {
        val cleanTitle = cleanMangaTitle(mangaTitle)
        val prompt = buildString {
            appendLine("You are a spoiler-safe manga/manhwa companion for the series \"$cleanTitle\" (currently reading \"$chapterName\").")
            if (!synopsis.isNullOrBlank()) {
                appendLine("Series Premise / Synopsis: ${synopsis.trim().take(600)}")
            }
            appendLine("Based on the dialogue/text from the current page below, provide a concise, bulleted \"Who's Here & Lore Codex\" covering:")
            appendLine("1. Any character names, titles, or factions mentioned or implied in this scene and their role up to \"$chapterName\".")
            appendLine("2. Any in-universe terminology, martial arts/magic systems, or plot callbacks referenced on this page.")
            appendLine()
            appendLine("CRITICAL RULE: Strictly NO SPOILERS beyond \"$chapterName\". Do NOT confuse with other series. Keep it concise and easy to scan.")
            appendLine()
            appendLine("Page Text:")
            appendLine("\"$text\"")
        }
        return callGeminiForText(prompt, apiKey, model)
    }

    suspend fun translateText(text: String, apiKey: String, model: String, targetLanguage: String = "English"): String {
        return callGeminiForText(
            "Translate this manga text to $targetLanguage, preserving the tone and style: \"$text\". Return ONLY the translation.",
            apiKey,
            model,
        )
    }

    @kotlinx.serialization.Serializable
    data class TranslationBlockInput(
        val id: Int,
        val text: String,
    )

    @kotlinx.serialization.Serializable
    data class TranslationBlockOutput(
        val id: Int,
        val text: String,
    )

    suspend fun translateBlocks(
        blocks: List<TranslationBlockInput>,
        apiKey: String,
        model: String,
        targetLanguage: String,
        sourceLanguage: String = "Auto-Detect",
        mangaTitle: String? = null,
        mangaGenres: List<String>? = null,
    ): List<TranslationBlockOutput> {
        if (blocks.isEmpty()) return emptyList()
        val blocksJson = json.encodeToString(blocks)
        val cacheKey = "$sourceLanguage::$targetLanguage::${mangaTitle.orEmpty()}::$blocksJson"
        translationCache[cacheKey]?.let { return it }

        val translationContext = if (sourceLanguage == "Auto-Detect") {
            "to $targetLanguage"
        } else {
            "from $sourceLanguage to $targetLanguage"
        }
        val seriesContext = buildString {
            if (!mangaTitle.isNullOrBlank()) {
                append("Series title: \"$mangaTitle\". ")
            }
            if (!mangaGenres.isNullOrEmpty()) {
                append("Genres: ${mangaGenres.take(5).joinToString(", ")}. ")
            }
        }
        val prompt = """
            You are an expert manga/manhwa scanlation translator and typesetter. Translate the following OCR text blocks $translationContext.
            $seriesContext
            Guidelines:
            1. The input comes from optical character recognition (OCR) of speech bubbles; silently infer and heal minor OCR character mistakes or broken words using the surrounding dialogue context.
            2. Keep translations natural, punchy, and concise so they fit comfortably inside comic speech balloons without wordy filler.
            3. Preserve character voice, honorifics/terminology where appropriate for the series, and dramatic tone.
            4. Return ONLY a valid JSON array of objects with the exact same integer "id" and translated "text" (no markdown code fences or commentary).

            Input:
            $blocksJson
        """.trimIndent()

        val resultText = callGeminiForText(prompt, apiKey, model)
        if (resultText.startsWith("Error:")) {
            throw Exception(resultText)
        }

        val cleanResult = resultText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        return try {
            val start = cleanResult.indexOf("[")
            val end = cleanResult.lastIndexOf("]") + 1
            if (start != -1 && end > start) {
                val jsonArrayStr = cleanResult.substring(start, end)
                try {
                    val element = json.parseToJsonElement(jsonArrayStr)
                    val outputs = element.jsonArray.mapNotNull { item ->
                        try {
                            val obj = item.jsonObject
                            val id = obj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
                            val text = obj["text"]?.jsonPrimitive?.content ?: return@mapNotNull null
                            TranslationBlockOutput(id, text.trim())
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (outputs.isNotEmpty()) {
                        translationCache[cacheKey] = outputs
                        return outputs
                    }
                } catch (_: Exception) {
                }
            }

            // Fallback Regex: handle quotes and any property ordering
            val regex = Regex("""\{\s*"id"\s*:\s*"?(\d+)"?\s*,\s*"text"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            val regexReversed = Regex("""\{\s*"text"\s*:\s*"((?:[^"\\]|\\.)*)"\s*,\s*"id"\s*:\s*"?(\d+)"?""")
            var parsed = regex.findAll(cleanResult).map { match ->
                val id = match.groupValues[1].toInt()
                val text = match.groupValues[2]
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\\\", "\\")
                TranslationBlockOutput(id, text.trim())
            }.toList()

            if (parsed.isEmpty()) {
                parsed = regexReversed.findAll(cleanResult).map { match ->
                    val text = match.groupValues[1]
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\\\", "\\")
                    val id = match.groupValues[2].toInt()
                    TranslationBlockOutput(id, text.trim())
                }.toList()
            }

            if (parsed.isNotEmpty()) {
                translationCache[cacheKey] = parsed
                parsed
            } else {
                throw Exception("Failed to parse translation JSON: $cleanResult")
            }
        } catch (e: Exception) {
            throw Exception("Failed to parse translation: ${e.message}. Raw response: $resultText")
        }
    }

    suspend fun enrichMetadata(title: String, currentDescription: String, apiKey: String, model: String): EnrichedMetadata? {
        return withIOContext {
            if (!shinkuPreferences.isAiConfigured() && apiKey.isBlank()) return@withIOContext null
            try {
                val prompt = """
                    You are a manga librarian. Based on the title "$title" and description "$currentDescription",
                    provide an enriched version of the metadata.

                    Return ONLY a JSON object with:
                    1. "description": A high-quality, professional, and engaging summary in English.
                    2. "genres": A list of relevant genre tags (e.g., ["Action", "Psychological"]).
                    3. "tags": A list of specific content tags (e.g., ["Time Travel", "Gore", "Romance"]).

                    JSON format:
                    {"description": "...", "genres": ["...", "..."], "tags": ["...", "..."]}
                """.trimIndent()

                val text = callGeminiForText(prompt, apiKey, model)
                val start = text.indexOf("{")
                val end = text.lastIndexOf("}") + 1
                if (start != -1 && end > start) {
                    val jsonStr = text.substring(start, end)
                    json.decodeFromString<EnrichedMetadata>(jsonStr)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getForYouRecommendations(
        historyTitles: List<String>,
        topGenres: List<String>,
        apiKey: String,
        model: String,
    ): List<String> {
        val prompt = """
            You are a manga discovery expert. Based on my recent reading history and favorite genres, suggest 10 real manga titles I might enjoy.

            My recent titles: ${historyTitles.joinToString(", ")}
            My favorite genres: ${topGenres.joinToString(", ")}

            Return ONLY a JSON array of strings containing the suggested titles.
        """.trimIndent()

        return withIOContext {
            try {
                callGemini(prompt, apiKey, model)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getChapterRecap(
        title: String,
        chapterNames: List<String>,
        apiKey: String,
        model: String,
        synopsis: String? = null,
        author: String? = null,
        genres: List<String>? = null,
        originalTitle: String? = null,
    ): String {
        val cleanTitle = cleanMangaTitle(title)
        val cacheKey = "${cleanTitle.trim()}::${chapterNames.joinToString(",")}::${author.orEmpty()}::${synopsis?.hashCode() ?: 0}"
        recapCache[cacheKey]?.let { return it }

        val prompt = buildString {
            appendLine("You are an expert manga and comic story recapper.")
            appendLine("Series Title: \"$cleanTitle\"")
            if (!originalTitle.isNullOrBlank() && originalTitle != cleanTitle && originalTitle != title) {
                appendLine("Alternative / Original Title: \"$originalTitle\"")
            }
            if (!author.isNullOrBlank()) {
                appendLine("Author / Creator: $author")
            }
            if (!genres.isNullOrEmpty()) {
                appendLine("Genres: ${genres.joinToString(", ")}")
            }
            if (!synopsis.isNullOrBlank()) {
                appendLine("Official Synopsis / Premise: ${synopsis.trim().take(1000)}")
            }
            appendLine()
            appendLine("The reader is currently reading up to these chapters:")
            appendLine(chapterNames.joinToString(", "))
            appendLine()
            appendLine("TASK:")
            appendLine("Provide a concise, engaging, spoiler-safe story recap of the events in \"$cleanTitle\" up to the specified chapters.")
            appendLine()
            appendLine("CRITICAL RULES:")
            appendLine("1. Recount the events OF THIS SPECIFIC SERIES ONLY, as identified by the title, author, and synopsis above.")
            appendLine("2. Never substitute, confuse, or hallucinate a different series with a similar title or premise.")
            appendLine("3. Keep the summary under 3 concise paragraphs in English.")
            appendLine("4. Spoilers are allowed ONLY up to the specified chapters (do not spoil events after these chapters).")
            appendLine("5. Focus on the main narrative progress, key characters, and major turning points.")
        }

        val result = callGeminiForText(prompt, apiKey, model)
        if (!result.startsWith("Error:")) {
            recapCache[cacheKey] = result
        }
        return result
    }

    @kotlinx.serialization.Serializable
    data class EnrichedMetadata(
        val description: String,
        val genres: List<String>,
        val tags: List<String> = emptyList(),
    )

    suspend fun searchByImage(base64Image: String, apiKey: String, model: String): List<String> = withIOContext {
        try {
            val response = com.shinku.reader.domain.ai.GeminiApi(networkHelper.client).generateImageText(
                "Identify the manga in this image. Return ONLY a JSON array of up to 5 real manga titles that match this image or its style.",
                base64Image, apiKey, model,
            )
            parseTitles(response.text)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun callGeminiForText(prompt: String, apiKey: String, model: String): String = withIOContext {
        val activeEngine = aiEngineRegistry.getActiveEngine()
        if (activeEngine.id == "openai_compatible" && activeEngine.isAvailable) {
            val result = activeEngine.generateText(prompt)
            if (result.isSuccess) return@withIOContext result.getOrThrow()
            if (apiKey.isBlank()) return@withIOContext "Error: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
        }
        try {
            com.shinku.reader.domain.ai.GeminiApi(networkHelper.client).generateText(prompt, apiKey, model).text
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            "Error: ${e.message ?: "Gemini request failed"}"
        }
    }

    private fun callGemini(query: String, apiKey: String, model: String): List<String> {
        val prompt = """
            You are a manga discovery expert. Based on the following user description, provide a list of up to 10 real manga titles that match the "vibe".
            User Description: "$query"
            Return ONLY a JSON array of strings containing the titles. No extra text.
            Example: ["Title 1", "Title 2"]
        """.trimIndent()
        val activeEngine = aiEngineRegistry.getActiveEngine()
        if (activeEngine.id == "openai_compatible" && activeEngine.isAvailable) {
            val result = kotlinx.coroutines.runBlocking { activeEngine.generateTitles(prompt) }
            if (result.isSuccess && result.getOrThrow().isNotEmpty()) return result.getOrThrow()
        }
        return try {
            parseTitles(com.shinku.reader.domain.ai.GeminiApi(networkHelper.client).generateText(prompt, apiKey, model).text)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseTitles(text: String): List<String> {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']') + 1
        return if (start >= 0 && end > start) json.decodeFromString<List<String>>(text.substring(start, end)) else emptyList()
    }
}
