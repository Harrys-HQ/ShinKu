package com.shinku.reader.domain.source.interactor

import eu.kanade.tachiyomi.network.NetworkHelper
import com.shinku.reader.exh.source.ShinKuPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.shinku.reader.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GeminiVibeSearch(
    private val networkHelper: NetworkHelper,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
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
            if (!shinkuPreferences.aiProTier().get()) {
                kotlinx.coroutines.delay(1000)
            }
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
            if (apiKey.isBlank()) return@withIOContext ""
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
            if (apiKey.isBlank()) return@withIOContext emptyList()
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
        sourceLanguage: String = "Auto-Detect"
    ): List<TranslationBlockOutput> {
        if (blocks.isEmpty()) return emptyList()
        val blocksJson = json.encodeToString(blocks)
        val cacheKey = "$sourceLanguage::$targetLanguage::$blocksJson"
        translationCache[cacheKey]?.let { return it }

        val translationContext = if (sourceLanguage == "Auto-Detect") {
            "to $targetLanguage"
        } else {
            "from $sourceLanguage to $targetLanguage"
        }
        val prompt = """
            You are a professional manga translator. Translate the following text blocks (JSON array of ID and text) $translationContext, preserving the tone, style, character nuances, and context of the page.
            Return the translations in a JSON array of objects with the exact same 'id' and the translated 'text'.
            Return ONLY the valid JSON array, do not wrap it in markdown block or any additional text.
            
            Input:
            ${blocksJson}
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
                val jsonArray = cleanResult.substring(start, end)
                try {
                    val outputs = json.decodeFromString<List<TranslationBlockOutput>>(jsonArray)
                    if (outputs.isNotEmpty()) {
                        translationCache[cacheKey] = outputs
                    }
                    outputs
                } catch (_: Exception) {
                    val regex = Regex("""\{\s*"id"\s*:\s*(\d+)\s*,\s*"text"\s*:\s*"((?:[^"\\]|\\.)*)"""")
                    val parsed = regex.findAll(cleanResult).map { match ->
                        val id = match.groupValues[1].toInt()
                        val text = match.groupValues[2]
                            .replace("\\\"", "\"")
                            .replace("\\n", "\n")
                            .replace("\\\\", "\\")
                        TranslationBlockOutput(id, text)
                    }.toList()
                    if (parsed.isNotEmpty()) {
                        translationCache[cacheKey] = parsed
                        parsed
                    } else {
                        throw Exception("Failed to parse translation JSON: $cleanResult")
                    }
                }
            } else {
                throw Exception("Invalid response format from Gemini: $resultText")
            }
        } catch (e: Exception) {
            throw Exception("Failed to parse translation: ${e.message}. Raw response: $resultText")
        }
    }

    suspend fun enrichMetadata(title: String, currentDescription: String, apiKey: String, model: String): EnrichedMetadata? {
        return withIOContext {
            if (apiKey.isBlank()) return@withIOContext null
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
        model: String
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
        model: String
    ): String {
        val cacheKey = "$title::${chapterNames.joinToString(",")}"
        recapCache[cacheKey]?.let { return it }

        val prompt = """
            You are a manga story recapper. Based on the manga "$title", 
            provide a short, engaging, and professional story recap/summary of the events up to or including these chapters:
            ${chapterNames.joinToString(", ")}
            
            Keep the summary in English, spoilers allowed (as it is for someone who already read them but forgot), 
            and keep it under 3 paragraphs. Focus on key plot points and character developments.
        """.trimIndent()
        
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

    suspend fun searchByImage(base64Image: String, apiKey: String, model: String): List<String> {
        val resolvedModel = resolveModel(model)
        val modelsToTry = listOf(resolvedModel, "gemini-2.5-flash", "gemini-1.5-flash").distinct()
        
        val bodyJson = """
            {
              "contents": [{
                "parts":[
                  {"text": "Identify the manga in this image. Return ONLY a JSON array of up to 5 real manga titles that match this image or its style."},
                  {"inline_data": {
                    "mime_type":"image/jpeg",
                    "data": "$base64Image"
                  }}
                ]
              }]
            }
        """.trimIndent()

        return withIOContext {
            for (m in modelsToTry) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(bodyJson.toRequestBody("application/json".toMediaType()))
                        .build()

                    networkHelper.client.newCall(request).execute().use { response ->
                        if (response.code in listOf(429, 404, 503)) return@use
                        if (!response.isSuccessful) return@withIOContext emptyList()
                        val responseBody = response.body.string()
                        val result = json.parseToJsonElement(responseBody)
                        val text = result.jsonObject["candidates"]!!
                            .jsonArray[0].jsonObject["content"]!!
                            .jsonObject["parts"]!!
                            .jsonArray[0].jsonObject["text"]!!
                            .jsonPrimitive.content

                        val start = text.indexOf("[")
                        val end = text.lastIndexOf("]") + 1
                        if (start != -1 && end > start) {
                            val jsonArray = text.substring(start, end)
                            return@withIOContext json.decodeFromString<List<String>>(jsonArray)
                        } else {
                            return@withIOContext emptyList()
                        }
                    }
                } catch (_: Exception) {
                }
            }
            emptyList()
        }
    }

    private suspend fun callGeminiForText(prompt: String, apiKey: String, model: String): String {
        return withIOContext {
            if (apiKey.isBlank()) return@withIOContext "API Key not set"

            val preferredModel = resolveModel(model)
            val modelsToTry = listOf(preferredModel, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash").distinct()

            var lastError = "Unknown error"
            for (m in modelsToTry) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$apiKey"
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
                        if (response.code in listOf(429, 404, 503)) {
                            lastError = "HTTP ${response.code}: $responseBody"
                            return@use
                        }
                        if (!response.isSuccessful) {
                            return@withIOContext "Error: API returned HTTP ${response.code}. Response: $responseBody"
                        }
                        val result = json.parseToJsonElement(responseBody)
                        val candidates = result.jsonObject["candidates"]?.jsonArray
                        if (candidates.isNullOrEmpty()) {
                            val promptFeedback = result.jsonObject["promptFeedback"]
                            return@withIOContext "Error: No candidates returned. Prompt feedback: $promptFeedback"
                        }
                        val parts = candidates[0].jsonObject["content"]?.jsonObject["parts"]?.jsonArray
                        if (parts.isNullOrEmpty()) {
                            return@withIOContext "Error: Content parts are empty."
                        }
                        val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content
                        if (text != null) {
                            return@withIOContext text
                        } else {
                            return@withIOContext "Error: Text content is null."
                        }
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: "Unknown exception"
                }
            }
            "Error: $lastError"
        }
    }

    private fun callGemini(query: String, apiKey: String, model: String): List<String> {
        val preferredModel = resolveModel(model)
        val modelsToTry = listOf(preferredModel, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash").distinct()

        val prompt = """
            You are a manga discovery expert. Based on the following user description, provide a list of up to 10 real manga titles that match the "vibe".
            User Description: "$query"
            Return ONLY a JSON array of strings containing the titles. No extra text.
            Example: ["Title 1", "Title 2"]
        """.trimIndent()

        val bodyJson = """
            {
              "contents": [{
                "parts":[{"text": ${Json.encodeToString(prompt)}}]
              }]
            }
        """.trimIndent()

        for (m in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
                    .build()

                networkHelper.client.newCall(request).execute().use { response ->
                    if (response.code in listOf(429, 404, 503)) return@use
                    if (!response.isSuccessful) throw Exception("API Error: ${response.code}")

                    val responseBody = response.body.string()
                    val result = json.parseToJsonElement(responseBody)
                    val text = result.jsonObject["candidates"]!!
                        .jsonArray[0].jsonObject["content"]!!
                        .jsonObject["parts"]!!
                        .jsonArray[0].jsonObject["text"]!!
                        .jsonPrimitive.content

                    // Extract titles from JSON array in the text
                    val start = text.indexOf("[")
                    val end = text.lastIndexOf("]") + 1
                    if (start != -1 && end > start) {
                        val jsonArray = text.substring(start, end)
                        return json.decodeFromString<List<String>>(jsonArray)
                    } else {
                        return emptyList()
                    }
                }
            } catch (_: Exception) {
            }
        }
        return emptyList()
    }

    private fun resolveModel(model: String): String {
        return when (model) {
            "gemini-3.5-flash",
            "gemini-3.5-pro",
            "gemini-3.1-flash-lite",
            "gemini-3.1-pro-preview",
            "gemini-3-pro-preview",
            "gemini-3-flash-preview",
            "gemini-3.0-flash",
            "gemini-3.0-pro",
            "gemini-3.1-pro",
            "" -> "gemini-2.5-flash"
            else -> model
        }
    }
}
