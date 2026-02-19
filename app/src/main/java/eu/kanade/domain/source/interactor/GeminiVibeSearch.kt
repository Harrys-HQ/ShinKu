package eu.kanade.domain.source.interactor

import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext

class GeminiVibeSearch(
    private val networkHelper: NetworkHelper,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getMangaTitles(query: String, apiKey: String): List<String> {
        return withIOContext {
            // Primary model
            try {
                callGemini(query, apiKey, "gemini-2.0-flash")
            } catch (e: Exception) {
                // Backup model
                try {
                    callGemini(query, apiKey, "gemini-2.0-flash-lite-preview-02-05")
                } catch (e2: Exception) {
                    emptyList()
                }
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
