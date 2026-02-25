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
    private val json = Json { ignoreUnknownKeys = true }
    private val shinkuPreferences: ShinKuPreferences = Injekt.get()

    suspend fun getMangaTitles(query: String, apiKey: String, model: String): List<String> {
        return withIOContext {
            // AI Pro Tier logic: Delay if not pro
            if (!shinkuPreferences.aiProTier().get()) {
                kotlinx.coroutines.delay(1000)
            }
            try {
                callGemini(query, apiKey, model)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getLatestUserAgent(apiKey: String, model: String, currentUa: String): String {
        return withIOContext {
            if (apiKey.isBlank()) return@withIOContext ""
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val prompt = "Based on this User-Agent string: '$currentUa', provide the latest stable version of it for the same browser and platform. Return ONLY the updated User-Agent string, no extra text."
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
                    if (!response.isSuccessful) return@withIOContext ""
                    val responseBody = response.body.string()
                    val result = json.parseToJsonElement(responseBody)
                    result.jsonObject["candidates"]!!
                        .jsonArray[0].jsonObject["content"]!!
                        .jsonObject["parts"]!!
                        .jsonArray[0].jsonObject["text"]!!
                        .jsonPrimitive.content.trim().removeSurrounding("\"")
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    suspend fun getSimilarManga(title: String, description: String, apiKey: String, model: String): List<String> {
        return withIOContext {
            if (apiKey.isBlank()) return@withIOContext emptyList()
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val prompt = """
                    Based on the manga title "$title" and its description: "$description",
                    suggest 5-10 other real manga titles that have a similar "vibe", tone, or themes.
                    Return ONLY a JSON array of strings.
                """.trimIndent()
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
                        json.decodeFromString<List<String>>(jsonArray)
                    } else {
                        emptyList()
                    }
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

    suspend fun translateText(text: String, apiKey: String, model: String): String {
        return callGeminiForText(
            "Translate this manga text to English, preserving the tone and style: \"$text\". Return ONLY the translation.",
            apiKey,
            model,
        )
    }

    suspend fun enrichMetadata(title: String, currentDescription: String, apiKey: String, model: String): EnrichedMetadata? {
        return withIOContext {
            if (apiKey.isBlank()) return@withIOContext null
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val prompt = """
                    You are a manga librarian. Based on the title "$title" and description "$currentDescription", 
                    provide an enriched version of the metadata.
                    
                    Return ONLY a JSON object with:
                    1. "description": A high-quality, professional, and engaging summary in English.
                    2. "genres": A list of relevant genre tags (e.g., ["Action", "Psychological"]).
                    
                    JSON format:
                    {"description": "...", "genres": ["...", "..."]}
                """.trimIndent()
                
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
                    if (!response.isSuccessful) return@withIOContext null
                    val responseBody = response.body.string()
                    val result = json.parseToJsonElement(responseBody)
                    val text = result.jsonObject["candidates"]!!
                        .jsonArray[0].jsonObject["content"]!!
                        .jsonObject["parts"]!!
                        .jsonArray[0].jsonObject["text"]!!
                        .jsonPrimitive.content

                    val start = text.indexOf("{")
                    val end = text.lastIndexOf("}") + 1
                    if (start != -1 && end > start) {
                        val jsonStr = text.substring(start, end)
                        json.decodeFromString<EnrichedMetadata>(jsonStr)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    @kotlinx.serialization.Serializable
    data class EnrichedMetadata(
        val description: String,
        val genres: List<String>,
    )

    private suspend fun callGeminiForText(prompt: String, apiKey: String, model: String): String {
        return withIOContext {
            if (apiKey.isBlank()) return@withIOContext "API Key not set"
            try {
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
                    if (!response.isSuccessful) return@withIOContext "Error: ${response.code}"
                    val responseBody = response.body.string()
                    val result = json.parseToJsonElement(responseBody)
                    result.jsonObject["candidates"]!!
                        .jsonArray[0].jsonObject["content"]!!
                        .jsonObject["parts"]!!
                        .jsonArray[0].jsonObject["text"]!!
                        .jsonPrimitive.content
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    private fun callGemini(query: String, apiKey: String, model: String): List<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

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

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        networkHelper.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("API Error: ${response.code}")

            val responseBody = response.body.string()
            val result = json.parseToJsonElement(responseBody)
            val text = result.jsonObject["candidates"]!!
                .jsonArray[0].jsonObject["content"]!!
                .jsonObject["parts"]!!
                .jsonArray[0].jsonObject["text"]!!
                .jsonPrimitive.content

            // Extract titles from JSON array in the text
            return try {
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
