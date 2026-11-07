package com.shinku.reader.domain.ai

import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class GeminiApiTest {
    private val requests = mutableListOf<Request>()
    private val success = """{"candidates":[{"content":{"parts":[{"text":"OK"}]}}]}"""

    private fun catalog(vararg ids: String) = "{\"models\":[" + ids.joinToString(",") {
        "{\"name\":\"models/$it\",\"displayName\":\"$it\",\"supportedGenerationMethods\":[\"generateContent\"]}"
    } + "]}"

    private fun client(
        catalogResponse: Pair<Int, String>? = 200 to catalog(GeminiModels.DEFAULT, "gemini-3.6-flash"),
        responder: (Request) -> Pair<Int, String>,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            requests.add(chain.request())
            val (code, body) = if (chain.request().method == "GET" && catalogResponse != null) catalogResponse else responder(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("Fixture")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()

    @Test
    fun `3x selection is preserved and credentials never enter the request URL`() {
        val api = GeminiApi(client { 200 to success })
        val response = api.generateText("A quote: \"hello\"\nNext line", " fixture-key ", " models/gemini-3.6-flash ")
        assertEquals("gemini-3.6-flash", response.model)
        assertEquals("OK", response.text)
        val request = requests.single()
        assertTrue(request.url.encodedPath.endsWith("/models/gemini-3.6-flash:generateContent"))
        assertEquals(null, request.url.query)
        assertFalse(request.url.toString().contains("fixture-key"))
        assertEquals("fixture-key", request.header("x-goog-api-key"))
        val payload = Json.parseToJsonElement(Buffer().also { request.body!!.writeTo(it) }.readUtf8()).jsonObject
        val content = payload.getValue("contents").jsonArray.single().jsonObject
        assertEquals("user", content.getValue("role").jsonPrimitive.content)
        assertEquals("A quote: \"hello\"\nNext line", content.getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content)
        assertFalse(payload.containsKey("generationConfig"))
    }

    @Test
    fun `restricted saved legacy model falls back to a supported current model`() {
        val api = GeminiApi(
            client { request ->
                if (request.url.encodedPath.contains("gemini-2.5-flash:")) 404 to "{}" else 200 to success
            },
        )
        assertEquals(GeminiModels.DEFAULT, api.generateText("Fixture", "fixture-key", "gemini-2.5-flash").model)
        assertEquals(3, requests.size)
        assertTrue(requests.none { it.url.encodedPath.contains("gemini-2.0") || it.url.encodedPath.contains("gemini-1.5") })
    }

    @Test
    fun `automatic selection tries each current fallback once`() {
        val api = GeminiApi(
            client { request ->
                if (request.url.encodedPath.contains(GeminiModels.DEFAULT)) 404 to "{}" else 200 to success
            },
        )
        assertEquals("gemini-3.6-flash", api.generateText("Fixture", "fixture-key", "auto").model)
        assertEquals(3, requests.size)
    }

    @Test
    fun `connection test reports selected model failure without testing another model`() {
        val api = GeminiApi(client { 404 to "{}" })
        val failure = assertThrows(GeminiApiException::class.java) {
            api.testConnection("fixture-key", "gemini-3.6-flash")
        }
        assertEquals(404, failure.statusCode)
        assertTrue(failure.message!!.contains("choose Automatic"))
        assertEquals(1, requests.size)
    }

    @Test
    fun `authentication failures stop immediately and do not expose error response secrets`() {
        val api = GeminiApi(client { 403 to """{"error":{"message":"fixture-secret"}}""" })
        val failure = assertThrows(GeminiApiException::class.java) { api.generateText("Fixture", "fixture-secret", "gemini-3.6-flash") }
        assertEquals(403, failure.statusCode)
        assertFalse(failure.message!!.contains("fixture-secret"))
        assertEquals(1, requests.size)
    }

    @Test
    fun `rate limits stop rather than switching models to retry quota`() {
        val api = GeminiApi(client { 429 to "{}" })
        val failure = assertThrows(GeminiApiException::class.java) { api.generateText("Fixture", "fixture-key", "auto") }
        assertEquals(429, failure.statusCode)
        assertEquals(1, requests.count { it.method == "POST" })
    }

    @Test
    fun `unavailable automatic models are each attempted once`() {
        val api = GeminiApi(client { 404 to "{}" })
        assertThrows(GeminiApiException::class.java) { api.generateText("Fixture", "fixture-key", "auto") }
        assertEquals(2, requests.count { it.method == "POST" })
        assertEquals(2, requests.filter { it.method == "POST" }.map { it.url }.distinct().size)
    }

    @Test
    fun `thought parts are excluded while answer text parts are combined`() {
        val api = GeminiApi(
            client {
                200 to """{"candidates":[{"content":{"parts":[{"thought":true,"text":"Private reasoning"},{"text":"Bon"},{"text":"jour"}]}}]}"""
            },
        )
        assertEquals("Bonjour", api.generateText("Fixture", "fixture-key", "auto").text)
    }

    @Test
    fun `blocked empty success is reported as failure`() {
        val api = GeminiApi(client { 200 to """{"promptFeedback":{"blockReason":"SAFETY"}}""" })
        assertThrows(IOException::class.java) { api.generateText("Fixture", "fixture-key", "auto") }
        assertEquals(1, requests.count { it.method == "POST" })
    }

    @Test
    fun `image search retains inline image content and current model selection`() {
        val api = GeminiApi(client { 200 to success })
        api.generateImageText("Identify fixture", "ZmFrZQ==", "fixture-key", "gemini-3.6-flash")
        val payload = Json.parseToJsonElement(Buffer().also { requests.single().body!!.writeTo(it) }.readUtf8()).jsonObject
        val parts = payload.getValue("contents").jsonArray.single().jsonObject.getValue("parts").jsonArray
        assertEquals("image/jpeg", parts[1].jsonObject.getValue("inline_data").jsonObject.getValue("mime_type").jsonPrimitive.content)
        assertEquals("ZmFrZQ==", parts[1].jsonObject.getValue("inline_data").jsonObject.getValue("data").jsonPrimitive.content)
    }

    @Test
    fun `future model IDs work without being rewritten and invalid paths are rejected before a request`() {
        val api = GeminiApi(client { 200 to success })
        assertEquals("gemini-future-flash", api.generateText("Fixture", "fixture-key", "gemini-future-flash").model)
        assertThrows(IllegalArgumentException::class.java) { api.generateText("Fixture", "fixture-key", "../private?key=bad") }
        assertEquals(1, requests.size)
    }

    @Test
    fun `cloud engine preserves cancellation rather than retrying or returning an ordinary error`() {
        val transport = client { throw CancellationException("Fixture cancellation") }
        val network = mockk<NetworkHelper> { every { client } returns transport }
        val key = mockk<Preference<String>> { every { get() } returns "fixture-key" }
        val model = mockk<Preference<String>> { every { get() } returns "gemini-3.6-flash" }
        val preferences = mockk<ShinKuPreferences> {
            every { geminiApiKey() } returns key
            every { geminiModel() } returns model
        }
        assertThrows(CancellationException::class.java) { runBlocking { CloudGeminiEngine(network, preferences).generateText("Fixture") } }
        assertEquals(1, requests.size)
    }

    @Test
    fun `automatic does not request the old default when Google only lists another model`() {
        val api = GeminiApi(client(200 to catalog("gemini-3.6-flash")) { 200 to success })
        assertEquals("gemini-3.6-flash", api.generateText("Fixture", "fixture-key", "auto").model)
        assertEquals(1, requests.count { it.method == "GET" })
        assertTrue(requests.filter { it.method == "POST" }.all { it.url.encodedPath.contains("gemini-3.6-flash:") })
    }

    @Test
    fun `catalog follows pages filters specialist models and deduplicates names`() {
        val api = GeminiApi(
            client(catalogResponse = null) { request ->
                assertEquals("1000", request.url.queryParameter("pageSize"))
                assertEquals("fixture-key", request.header("x-goog-api-key"))
                assertFalse(request.url.toString().contains("fixture-key"))
                if (request.url.queryParameter("pageToken") == null) {
                    200 to """{"models":[
                    {"name":"models/gemini-3.6-flash","supportedGenerationMethods":["generateContent"]},
                    {"name":"models/gemini-3-pro-image","supportedGenerationMethods":["generateContent"]},
                    {"name":"models/gemini-embedding-001","supportedGenerationMethods":["embedContent"]},
                    {"name":"models/gemini-3.5-live","supportedGenerationMethods":["bidiGenerateContent"]}
                ],"nextPageToken":"next+ token"}"""
                } else {
                    assertEquals("next+ token", request.url.queryParameter("pageToken"))
                    200 to catalog("gemini-3.6-flash", "gemini-3.5-flash-lite", "gemini-3.5-tts")
                }
            },
        )
        assertEquals(listOf("gemini-3.5-flash-lite", "gemini-3.6-flash"), api.listTextModels("fixture-key").map { it.id })
        assertEquals(2, requests.size)
    }

    @Test
    fun `catalog cache is scoped to the credential and explicit refresh bypasses it`() {
        val api = GeminiApi(
            client(catalogResponse = null) { request ->
                200 to catalog(if (request.header("x-goog-api-key") == "key-a") "gemini-3.6-flash" else "gemini-3.5-flash-lite")
            },
        )
        assertEquals("gemini-3.6-flash", api.listTextModels("key-a").single().id)
        assertEquals("gemini-3.5-flash-lite", api.listTextModels("key-b").single().id)
        assertEquals("gemini-3.6-flash", api.listTextModels("key-a").single().id)
        assertEquals(2, requests.size)
        api.listTextModels("key-a", forceRefresh = true)
        assertEquals(3, requests.size)
    }

    @Test
    fun `empty catalog does not guess a model or send a generation request`() {
        val api = GeminiApi(client(200 to "{}") { 200 to success })
        assertThrows(IOException::class.java) { api.generateText("Fixture", "fixture-key", "auto") }
        assertTrue(requests.all { it.method == "GET" })
    }

    @Test
    fun `catalog authentication reports structured nonsecret diagnostics without inference`() {
        val api = GeminiApi(client(403 to """{"error":{"status":"PERMISSION_DENIED","message":"fixture-secret","details":[{"reason":"API_KEY_SERVICE_BLOCKED"}]}}""") { 200 to success })
        val error = assertThrows(GeminiApiException::class.java) { api.generateText("Fixture", "fixture-secret", "auto") }
        assertTrue(error.message!!.contains("PERMISSION_DENIED"))
        assertTrue(error.message!!.contains("API_KEY_SERVICE_BLOCKED"))
        assertFalse(error.message!!.contains("fixture-secret"))
        assertTrue(requests.all { it.method == "GET" })
    }

    @Test
    fun `repeated pagination tokens fail without an endless catalog loop`() {
        val api = GeminiApi(client(catalogResponse = null) { 200 to """{"nextPageToken":"same"}""" })
        assertThrows(IOException::class.java) { api.listTextModels("fixture-key") }
        assertEquals(2, requests.size)
    }

    @Test
    fun `automatic connection test follows catalog based fallback and reports the model that answered`() {
        val api = GeminiApi(
            client { request ->
                if (request.url.encodedPath.contains(GeminiModels.DEFAULT)) 503 to "{}" else 200 to success
            },
        )
        val result = api.testConnection("fixture-key", "auto")
        assertEquals("gemini-3.6-flash", result.model)
        assertEquals(2, requests.count { it.method == "POST" })
    }

    @Test
    fun `failed explicit refresh invalidates the cached model list`() {
        var allowed = true
        val api = GeminiApi(
            client(catalogResponse = null) { request ->
                assertEquals("GET", request.method)
                if (allowed) 200 to catalog("gemini-3.6-flash") else 403 to "{}"
            },
        )
        api.listTextModels("fixture-key")
        allowed = false
        assertThrows(GeminiApiException::class.java) { api.listTextModels("fixture-key", forceRefresh = true) }
        assertThrows(GeminiApiException::class.java) { api.generateText("Fixture", "fixture-key", "auto") }
        assertEquals(3, requests.size)
    }
}
