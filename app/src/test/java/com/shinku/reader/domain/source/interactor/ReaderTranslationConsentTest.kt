package com.shinku.reader.domain.source.interactor

import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.domain.ai.AiEngineRegistry
import com.shinku.reader.domain.ai.OpenAiCompatibleEngine
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class ReaderTranslationConsentTest {
    @Test
    fun `only the approved prompt is sent to the destination captured before confirmation`() = runTest {
        val requests = LinkedBlockingQueue<Triple<String, String?, String>>()
        val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        val listener = thread(isDaemon = true) {
            val socket = try {
                server.accept()
            } catch (_: java.net.SocketException) {
                return@thread
            }
            socket.use {
                it.soTimeout = 5_000
                val reader = it.getInputStream().bufferedReader()
                val path = reader.readLine().split(' ')[1]
                val headers = generateSequence(reader::readLine).takeWhile(String::isNotEmpty).associate { header ->
                    header.substringBefore(':').lowercase() to header.substringAfter(':').trim()
                }
                val requestBody = buildString {
                    repeat(headers.getValue("content-length").toInt()) { append(reader.read().toChar()) }
                }
                requests.add(Triple(path, headers["cookie"], requestBody))
                val response = """{"choices":[{"message":{"content":"[{\"id\":0,\"text\":\"Hola\"}]"}}]}""".toByteArray()
                it.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Length: ${response.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    write(response)
                    flush()
                }
            }
        }
        try {
            val origin = "http://127.0.0.1:${server.localPort}"
            var endpoint = "$origin/approved"
            val preferences = mockk<ShinKuPreferences> {
                every { aiEngineProvider() } returns pref("custom")
                every { openAiApiKey() } returns pref("fixture-auth-key")
                every { openAiModel() } returns pref("fixture-model")
                every { openAiBaseUrl() } returns mockk<Preference<String>> { every { get() } answers { endpoint } }
            }
            val sharedClient = OkHttpClient.Builder().addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Cookie", "shared-session=secret").build())
            }.build()
            val network = mockk<NetworkHelper> { every { client } returns sharedClient }
            val registry = AiEngineRegistry(listOf(OpenAiCompatibleEngine(network, preferences)), preferences)
            val translator = GeminiVibeSearch(network, preferences, registry)
            val provider = checkNotNull(translator.readerTranslationProvider("", ""))
            val text = listOf(GeminiVibeSearch.TranslationBlockInput(0, "Hello!\nAuthorization: Bearer example-secret"))
            val glossary = TranslationGlossary(listOf(TranslationGlossary.Entry("Hello", "Preferred greeting")))

            try {
                translator.translateBlocks(text, "Spanish", provider = provider) { _, _ ->
                    assertTrue(requests.isEmpty())
                    false
                }
                error("Declining should cancel translation")
            } catch (_: ReaderTranslationDeclined) {
                assertTrue(requests.isEmpty())
            }

            var approvedPrompt = ""
            val result = translator.translateBlocks(text, "Spanish", glossary = glossary, provider = provider) { destination, prompt ->
                assertTrue(destination.startsWith(origin))
                assertTrue(requests.isEmpty())
                assertTrue(prompt.contains("Hello!"))
                assertTrue(prompt.contains("Preferred greeting"))
                assertFalse(prompt.contains("example-secret"))
                approvedPrompt = prompt
                endpoint = "$origin/changed"
                true
            }

            val (path, cookie, body) = requests.remove()
            assertEquals("/approved/chat/completions", path)
            assertNull(cookie)
            assertEquals(approvedPrompt, Json.parseToJsonElement(body).jsonObject["messages"]!!.jsonArray.single().jsonObject["content"]!!.jsonPrimitive.content)
            assertEquals(listOf(GeminiVibeSearch.TranslationBlockOutput(0, "Hola")), result)
            assertTrue(requests.isEmpty())
        } finally {
            server.close()
            listener.join(5_000)
        }
    }

    @Test
    fun `sanitization removes credentials from headers links and malformed credential lines`() {
        val clean = sanitizeReaderTranslationText(
            """
            Keep this dialogue.
            Cookie: login=alpha
            Proxy-Authorization=Bearer bravo
            See https://user:charlie@example.test/story?search=delta
            access_token="echo
            Also Bearer foxtrot and ghp_1234567890123456.
            """.trimIndent(),
        )
        assertTrue(clean.contains("Keep this dialogue."))
        assertTrue(clean.contains("https://example.test/story"))
        listOf("alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "ghp_").forEach {
            assertFalse(clean.contains(it), "Found $it in sanitized text")
        }
        assertEquals(clean, sanitizeReaderTranslationText(clean))
    }

    @Test
    fun `on-device routing bypasses consent and a decline cancels automatic cloud fallback`() = runTest {
        val local = translateReaderContext(
            engine = "mlkit",
            cloudConfigured = { error("Local mode does not need a cloud configuration") },
            translateLocally = { "on-device result" },
            translateRemotely = { error("Local mode reached cloud translation") },
        )
        assertEquals("on-device result", local)

        try {
            translateReaderContext(
                engine = "auto",
                cloudConfigured = { true },
                translateLocally = { "fallback" },
                translateRemotely = { throw ReaderTranslationDeclined() },
            )
            error("Decline must propagate")
        } catch (_: ReaderTranslationDeclined) {
        }
    }

    private fun pref(value: String): Preference<String> = mockk { every { get() } returns value }
}
