package com.shinku.reader.data.diagnostics

import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.domain.ai.AiEngineRegistry
import com.shinku.reader.domain.ai.OpenAiCompatibleEngine
import com.shinku.reader.domain.diagnostics.AppDiagnosticsReport
import com.shinku.reader.domain.diagnostics.DiagnosticCheckResult
import com.shinku.reader.domain.diagnostics.DiagnosticCheckStatus
import com.shinku.reader.domain.diagnostics.DiagnosticReportBuilder
import com.shinku.reader.domain.diagnostics.DiagnosticSectionId
import com.shinku.reader.domain.extensionrepo.model.ExtensionRepo
import com.shinku.reader.domain.source.interactor.GeminiVibeSearch
import com.shinku.reader.domain.source.interactor.UpdateSourceHealth
import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.domain.source.model.SourceHealthCategory
import com.shinku.reader.domain.source.model.SourceHealthClassifier
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.ui.diagnostics.AppDiagnosticsScreenModel
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.protobuf.ProtoBuf
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPOutputStream
import kotlin.concurrent.thread

@OptIn(ExperimentalCoroutinesApi::class)
class SourceHealthProberTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val protoBuf = ProtoBuf

    @Test
    fun `probeSourceBaseUrl classifies HTTP statuses redirects and strips URL credentials`() = runBlocking {
        val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
        val worker = thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                socket.use {
                    val reader = it.getInputStream().bufferedReader()
                    val requestLine = reader.readLine().orEmpty()
                    val path = requestLine.split(' ').getOrNull(1).orEmpty()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    val (statusLine, extraHeader) = when {
                        path.startsWith("/ok") -> "200 OK" to ""
                        path.startsWith("/unauth") -> "401 Unauthorized" to ""
                        path.startsWith("/forbidden") -> "403 Forbidden" to ""
                        path.startsWith("/cf") -> "403 Forbidden" to "cf-mitigated: challenge\r\nServer: cloudflare\r\n"
                        path.startsWith("/missing") -> "404 Not Found" to ""
                        path.startsWith("/limit") -> "429 Too Many Requests" to ""
                        path.startsWith("/badgw") -> "502 Bad Gateway" to ""
                        path.startsWith("/redir") -> "302 Found" to "Location: /other\r\n"
                        else -> "200 OK" to ""
                    }
                    val body = "ok".toByteArray()
                    it.getOutputStream().apply {
                        write("HTTP/1.1 $statusLine\r\n${extraHeader}Content-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(body)
                        flush()
                    }
                }
            }
        }
        try {
            val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build()
            val base = "http://127.0.0.1:${server.localPort}"

            val ok = SourceHealthProber.probeSourceBaseUrl(client, "$base/ok?token=secret", isNetworkAvailable = { true }, correlationId = "diag-1")
            assertTrue(ok.isSuccess)
            assertEquals(SourceHealthCategory.HEALTHY, ok.category)
            assertEquals("$base/ok", ok.sanitizedTarget)

            assertEquals(SourceHealthCategory.HTTP_UNAUTHORIZED, SourceHealthProber.probeSourceBaseUrl(client, "$base/unauth", isNetworkAvailable = { true }).category)
            assertEquals(SourceHealthCategory.HTTP_FORBIDDEN, SourceHealthProber.probeSourceBaseUrl(client, "$base/forbidden", isNetworkAvailable = { true }).category)
            assertEquals(SourceHealthCategory.CLOUDFLARE_BLOCKED, SourceHealthProber.probeSourceBaseUrl(client, "$base/cf", isNetworkAvailable = { true }).category)
            assertEquals(SourceHealthCategory.HTTP_NOT_FOUND, SourceHealthProber.probeSourceBaseUrl(client, "$base/missing", isNetworkAvailable = { true }).category)
            assertEquals(SourceHealthCategory.HTTP_RATE_LIMITED, SourceHealthProber.probeSourceBaseUrl(client, "$base/limit", isNetworkAvailable = { true }).category)
            assertEquals(SourceHealthCategory.HTTP_SERVER_ERROR, SourceHealthProber.probeSourceBaseUrl(client, "$base/badgw", isNetworkAvailable = { true }).category)
            assertEquals(SourceHealthCategory.HTTP_REDIRECT_BLOCKED, SourceHealthProber.probeSourceBaseUrl(client, "$base/redir", isNetworkAvailable = { true }).category)
        } finally {
            server.close()
            worker.join(3_000)
        }
    }

    @Test
    fun `offline probe makes zero connections and does not count as failure`() = runBlocking {
        val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        val accepted = AtomicInteger(0)
        val worker = thread(isDaemon = true) {
            try {
                server.accept().use { accepted.incrementAndGet() }
            } catch (_: Exception) {
            }
        }
        try {
            val outcome = SourceHealthProber.probeSourceBaseUrl(
                client = OkHttpClient(),
                targetUrl = "http://127.0.0.1:${server.localPort}/",
                isNetworkAvailable = { false },
            )
            assertFalse(outcome.isSuccess)
            assertEquals(SourceHealthCategory.OFFLINE, outcome.category)
            assertFalse(outcome.countAsFailure)
            assertEquals(0, accepted.get())
        } finally {
            server.close()
            worker.join(3_000)
        }
    }

    @Test
    fun `cancelling an in-flight probe propagates cancellation and updates live scan store`() = runBlocking {
        val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        val worker = thread(isDaemon = true) {
            try {
                server.accept().use {
                    Thread.sleep(2_000)
                }
            } catch (_: Exception) {
            }
        }
        val store = DiagnosticsStore()
        try {
            store.startScan("diag-cancel", totalCount = 2)
            val deferred = async {
                SourceHealthProber.probeSourceBaseUrl(
                    client = OkHttpClient(),
                    targetUrl = "http://127.0.0.1:${server.localPort}/slow",
                    isNetworkAvailable = { true },
                    correlationId = "diag-cancel",
                    timeoutMs = 10_000L,
                )
            }
            delay(50)
            deferred.cancel()
            val caught = try {
                deferred.await()
                false
            } catch (_: CancellationException) {
                true
            }
            assertTrue(caught)
            store.finishScan("diag-cancel", DiagnosticPhase.CANCELLED)
            assertTrue(store.liveScanState.value.isCancelled)
            assertFalse(store.liveScanState.value.isRunning)
            assertEquals(DiagnosticPhase.CANCELLED, store.liveScanState.value.currentPhase)
        } finally {
            server.close()
            worker.join(3_000)
        }
    }

    @Test
    fun `standalone probes and stale workers cannot change active scan progress`() {
        val store = DiagnosticsStore()
        val outcome = SourceHealthClassifier.classifyHttpResponse(
            statusCode = 200,
            correlationId = "active",
        )
        val workId = UUID.randomUUID()
        assertTrue(store.startScan("active", totalCount = 2, workId = workId))
        assertFalse(store.startScan("other", totalCount = 10))
        store.recordSourceProbe("Standalone", outcome.copy(correlationId = "other"))
        store.recordScanProbe(3L, "Stale worker", outcome.copy(correlationId = "other"))
        store.finishScan("other", DiagnosticPhase.FAILED)
        assertTrue(store.liveScanState.value.isRunning)
        assertEquals(workId, store.liveScanState.value.workId)
        assertEquals(0, store.liveScanState.value.completedCount)

        store.recordScanProbe(1L, "First source", outcome)
        store.recordScanProbe(2L, "Second source", outcome)
        store.recordScanProbe(2L, "Second source", outcome)
        store.finishScan("active", DiagnosticPhase.COMPLETED)
        store.recordScanProbe(3L, "Late worker", outcome)
        assertEquals(2, store.liveScanState.value.completedCount)
        assertEquals(2, store.liveScanState.value.totalCount)
        assertEquals(DiagnosticPhase.COMPLETED, store.liveScanState.value.currentPhase)
        assertNull(store.liveScanState.value.workId)
    }

    @Test
    fun `repository diagnostic limits downloaded and expanded indexes`() = runBlocking {
        val chunk = ByteArray(1024)
        val compressedIndex = ByteArrayOutputStream().let { output ->
            GZIPOutputStream(output).use { gzip ->
                repeat((SourceHealthProber.MAX_INDEX_BYTES / chunk.size).toInt()) { gzip.write(chunk) }
                gzip.write(0)
            }
            output.toByteArray()
        }
        val oversizedIndex = ByteArray(SourceHealthProber.MAX_INDEX_BYTES.toInt() + 1)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = when (request.url.encodedPath) {
                "/expanded/index.pb" -> compressedIndex
                "/downloaded/index.min.json" -> oversizedIndex
                else -> null
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(if (body == null) 404 else 200)
                .message(if (body == null) "Not Found" else "OK")
                .body((body ?: ByteArray(0)).toResponseBody())
                .build()
        }.build()

        for ((path, expectedPhase) in listOf("expanded" to DiagnosticPhase.INDEX_PARSE, "downloaded" to DiagnosticPhase.REPOSITORY_FETCH)) {
            val repo = ExtensionRepo("https://example.org/$path", "Test repository", null, "", "")
            val check = SourceHealthProber.probeExtensionRepo(client, repo, protoBuf, json, isNetworkAvailable = { true })
            assertEquals(SourceHealthCategory.REPO_INDEX_INVALID, check.category)
            assertEquals(expectedPhase, check.phase)
            assertEquals("Index exceeds the 16 MiB diagnostic limit", check.summary)
        }
    }

    @Test
    fun `probeExtensionRepo separates fetch failures from invalid index parse errors and redacts full URL`() = runBlocking {
        val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
        val worker = thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                socket.use {
                    val reader = it.getInputStream().bufferedReader()
                    val path = reader.readLine().orEmpty().split(' ').getOrNull(1).orEmpty()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    val (statusLine, bodyText) = when (path) {
                        "/valid/index.pb" -> "404 Not Found" to "missing"
                        "/valid/index.min.json" -> "200 OK" to """[{"name":"ShinKu: Demo","pkg":"eu.kanade.tachiyomi.extension.en.demo","apk":"demo.apk","lang":"en","code":1,"version":"1.5.1"}]"""
                        "/badjson/index.pb" -> "404 Not Found" to "missing"
                        "/badjson/index.min.json" -> "200 OK" to """{"not":"an array"""
                        "/down/index.pb" -> "500 Internal Server Error" to "err"
                        "/down/index.min.json" -> "500 Internal Server Error" to "err"
                        else -> "404 Not Found" to "missing"
                    }
                    val bytes = bodyText.toByteArray()
                    it.getOutputStream().apply {
                        write("HTTP/1.1 $statusLine\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(bytes)
                        flush()
                    }
                }
            }
        }
        try {
            val client = OkHttpClient()
            val base = "http://127.0.0.1:${server.localPort}"

            val validRepo = ExtensionRepo("$base/valid", "Valid Repo", "Valid", "https://example.org", "abc")
            val validCheck = SourceHealthProber.probeExtensionRepo(client, validRepo, protoBuf, json, isNetworkAvailable = { true }, correlationId = "diag-r1")
            assertEquals(DiagnosticCheckStatus.OK, validCheck.status)
            assertEquals(SourceHealthCategory.HEALTHY, validCheck.category)
            assertEquals("127.0.0.1:${server.localPort}", validCheck.repoHost)
            assertFalse(validCheck.repoHost.contains("/valid"))

            val badIndexRepo = ExtensionRepo("$base/badjson", "Broken Index", null, "https://example.org", "abc")
            val badIndexCheck = SourceHealthProber.probeExtensionRepo(client, badIndexRepo, protoBuf, json, isNetworkAvailable = { true }, correlationId = "diag-r2")
            assertEquals(DiagnosticCheckStatus.FAILED, badIndexCheck.status)
            assertEquals(SourceHealthCategory.REPO_INDEX_INVALID, badIndexCheck.category)
            assertEquals(DiagnosticPhase.INDEX_PARSE, badIndexCheck.phase)

            val downRepo = ExtensionRepo("$base/down", "Down Repo", null, "https://example.org", "abc")
            val downCheck = SourceHealthProber.probeExtensionRepo(client, downRepo, protoBuf, json, isNetworkAvailable = { true }, correlationId = "diag-r3")
            assertEquals(DiagnosticCheckStatus.FAILED, downCheck.status)
            assertEquals(SourceHealthCategory.HTTP_SERVER_ERROR, downCheck.category)
            assertEquals(DiagnosticPhase.REPOSITORY_FETCH, downCheck.phase)
        } finally {
            server.close()
            worker.join(3_000)
        }
    }

    @Test
    fun `diagnostics AI explanation requires explicit consent cancels with zero calls and sends redacted prompt without cookies`() = runBlocking {
        val requests = LinkedBlockingQueue<Pair<String?, String>>()
        val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        val listener = thread(isDaemon = true) {
            val socket = try {
                server.accept()
            } catch (_: Exception) {
                return@thread
            }
            socket.use {
                val reader = it.getInputStream().bufferedReader()
                reader.readLine()
                val headers = generateSequence(reader::readLine).takeWhile(String::isNotEmpty).associate { h ->
                    h.substringBefore(':').lowercase() to h.substringAfter(':').trim()
                }
                val body = buildString {
                    repeat(headers.getValue("content-length").toInt()) { append(reader.read().toChar()) }
                }
                requests.add(headers["cookie"] to body)
                val resp = """{"choices":[{"message":{"content":"Advisory summary citing [SOURCE_HEALTH]."}}]}""".toByteArray()
                it.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Length: ${resp.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    write(resp)
                    flush()
                }
            }
        }
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            val origin = "http://127.0.0.1:${server.localPort}"
            val preferences = mockk<ShinKuPreferences> {
                every { aiEngineProvider() } returns pref("custom")
                every { openAiApiKey() } returns pref("test-key")
                every { openAiModel() } returns pref("test-model")
                every { openAiBaseUrl() } returns pref(origin)
                every { geminiApiKey() } returns pref("")
                every { geminiModel() } returns pref("auto")
            }
            val sharedClient = OkHttpClient.Builder().addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Cookie", "shared=secret").build())
            }.build()
            val network = mockk<NetworkHelper> { every { client } returns sharedClient }
            val registry = AiEngineRegistry(listOf(OpenAiCompatibleEngine(network, preferences)), preferences)
            val vibe = GeminiVibeSearch(network, preferences, registry)

            val report = AppDiagnosticsReport(
                correlationId = "diag-ai1",
                generatedAt = 1_700_000_000_000L,
                checks = listOf(
                    DiagnosticCheckResult(
                        id = DiagnosticSectionId.SOURCE_HEALTH,
                        title = DiagnosticSectionId.SOURCE_HEALTH.title,
                        status = DiagnosticCheckStatus.WARNING,
                        timestamp = 1_700_000_000_000L,
                        correlationId = "diag-ai1",
                        phase = DiagnosticPhase.SOURCE_REQUEST,
                        summary = "Timeout with Bearer leak-token",
                    ),
                ),
            )
            val collector = mockk<AppDiagnosticsCollector> {
                coEvery { collectLocalReport(any()) } returns report
            }
            val model = AppDiagnosticsScreenModel(
                collector = collector,
                diagnosticsStore = DiagnosticsStore(),
                updateSourceHealth = mockk<UpdateSourceHealth>(relaxed = true),
                geminiVibeSearch = vibe,
                shinkuPreferences = preferences,
            )

            val deadline = System.currentTimeMillis() + 3_000L
            while (model.state.value.report == null && System.currentTimeMillis() < deadline) {
                delay(10)
            }
            assertNotNull(model.state.value.report)

            model.requestAiExplanation()
            val pendingDeclined = checkNotNull(model.state.value.pendingAiConsent)
            assertFalse(pendingDeclined.prompt.contains("leak-token"))
            assertTrue(pendingDeclined.destination.startsWith(origin))
            model.dismissAiConsent()
            assertNull(model.state.value.pendingAiConsent)
            assertTrue(requests.isEmpty())

            model.requestAiExplanation()
            val expectedPrompt = checkNotNull(model.state.value.pendingAiConsent).prompt
            model.confirmAiExplanation()
            val aiDeadline = System.currentTimeMillis() + 5_000L
            while (model.state.value.aiSummary == null && System.currentTimeMillis() < aiDeadline) {
                delay(10)
            }

            assertEquals("Advisory summary citing [SOURCE_HEALTH].", model.state.value.aiSummary)
            val (cookie, body) = requests.remove()
            assertNull(cookie)
            val sentPrompt = Json.parseToJsonElement(body).jsonObject["messages"]!!.jsonArray.single().jsonObject["content"]!!.jsonPrimitive.content
            assertEquals(expectedPrompt, sentPrompt)
        } finally {
            Dispatchers.resetMain()
            server.close()
            listener.join(3_000)
        }
    }

    private fun pref(value: String): Preference<String> = mockk { every { get() } returns value }
}
