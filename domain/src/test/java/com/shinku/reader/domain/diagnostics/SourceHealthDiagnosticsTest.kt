package com.shinku.reader.domain.diagnostics

import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.domain.source.model.HealthProbeType
import com.shinku.reader.domain.source.model.SourceHealth
import com.shinku.reader.domain.source.model.SourceHealthCategory
import com.shinku.reader.domain.source.model.SourceHealthClassifier
import com.shinku.reader.domain.source.model.SourceHealthStatus
import com.shinku.reader.domain.source.model.SourceNotInstalledException
import eu.kanade.tachiyomi.network.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ConnectException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLHandshakeException

class SourceHealthDiagnosticsTest {

    @Test
    fun `classifyHttpResponse distinguishes HTTP families and Cloudflare challenge`() {
        assertEquals(SourceHealthCategory.HEALTHY, SourceHealthClassifier.classifyHttpResponse(200).category)
        assertTrue(SourceHealthClassifier.classifyHttpResponse(200).isSuccess)

        assertEquals(SourceHealthCategory.HTTP_REDIRECT_BLOCKED, SourceHealthClassifier.classifyHttpResponse(302).category)
        assertEquals(SourceHealthCategory.HTTP_UNAUTHORIZED, SourceHealthClassifier.classifyHttpResponse(401).category)
        assertEquals(SourceHealthCategory.HTTP_FORBIDDEN, SourceHealthClassifier.classifyHttpResponse(403).category)
        assertEquals(
            SourceHealthCategory.CLOUDFLARE_BLOCKED,
            SourceHealthClassifier.classifyHttpResponse(
                statusCode = 403,
                headers = mapOf("cf-mitigated" to "challenge", "server" to "cloudflare"),
            ).category,
        )
        assertEquals(SourceHealthCategory.HTTP_NOT_FOUND, SourceHealthClassifier.classifyHttpResponse(404).category)
        assertEquals(SourceHealthCategory.HTTP_RATE_LIMITED, SourceHealthClassifier.classifyHttpResponse(429).category)
        assertEquals(SourceHealthCategory.HTTP_SERVER_ERROR, SourceHealthClassifier.classifyHttpResponse(502).category)
        assertEquals(
            SourceHealthCategory.CLOUDFLARE_BLOCKED,
            SourceHealthClassifier.classifyHttpResponse(
                statusCode = 503,
                headers = mapOf("cf-mitigated" to "challenge"),
            ).category,
        )
    }

    @Test
    fun `classifyThrowable inspects cause chain for DNS TLS timeouts redirects offline and parse errors`() {
        val wrappedDns = IOException("failed", UnknownHostException("dead.invalid"))
        assertEquals(SourceHealthCategory.DNS_FAILURE, SourceHealthClassifier.classifyThrowable(wrappedDns).category)

        assertEquals(
            SourceHealthCategory.TLS_ERROR,
            SourceHealthClassifier.classifyThrowable(IOException("wrap", SSLHandshakeException("bad cert"))).category,
        )
        assertEquals(
            SourceHealthCategory.TLS_ERROR,
            SourceHealthClassifier.classifyThrowable(CertPathValidatorException("expired")).category,
        )
        assertEquals(
            SourceHealthCategory.CONNECT_TIMEOUT,
            SourceHealthClassifier.classifyThrowable(ConnectException("refused")).category,
        )
        assertEquals(
            SourceHealthCategory.CONNECT_TIMEOUT,
            SourceHealthClassifier.classifyThrowable(SocketTimeoutException("failed to connect to host")).category,
        )
        assertEquals(
            SourceHealthCategory.READ_TIMEOUT,
            SourceHealthClassifier.classifyThrowable(SocketTimeoutException("timeout")).category,
        )
        assertEquals(
            SourceHealthCategory.HTTP_REDIRECT_BLOCKED,
            SourceHealthClassifier.classifyThrowable(ProtocolException("Too many follow-up requests: 21")).category,
        )
        assertEquals(
            SourceHealthCategory.HTTP_RATE_LIMITED,
            SourceHealthClassifier.classifyThrowable(HttpException(429)).category,
        )
        assertEquals(
            SourceHealthCategory.REPO_INDEX_INVALID,
            SourceHealthClassifier.classifyThrowable(
                SerializationException("bad json"),
                phase = DiagnosticPhase.INDEX_PARSE,
            ).category,
        )
        assertEquals(
            SourceHealthCategory.PARSER_ERROR,
            SourceHealthClassifier.classifyThrowable(
                SerializationException("bad chapter html"),
                phase = DiagnosticPhase.SOURCE_REQUEST,
            ).category,
        )
        assertEquals(
            SourceHealthCategory.EXTENSION_UNAVAILABLE,
            SourceHealthClassifier.classifyThrowable(SourceNotInstalledException()).category,
        )

        val offline = SourceHealthClassifier.classifyThrowable(UnknownHostException("offline"), isNetworkAvailable = false)
        assertEquals(SourceHealthCategory.OFFLINE, offline.category)
        assertFalse(offline.countAsFailure)

        assertThrows(CancellationException::class.java) {
            SourceHealthClassifier.classifyThrowable(CancellationException("cancelled"))
        }
    }

    @Test
    fun `one failed probe does not mark a source dead and offline stays unknown`() {
        val unchecked = SourceHealth(1L, 0L, 0L, 0, 0, 0L, null)
        assertEquals(SourceHealthStatus.UNKNOWN, unchecked.effectiveStatus())

        val offline = SourceHealth(
            sourceId = 1L,
            lastSuccess = 0L,
            lastFailure = 0L,
            failureCount = 0,
            successCount = 0,
            avgLatency = 0L,
            lastError = "Device appears offline",
            lastCategory = SourceHealthCategory.OFFLINE,
            lastChecked = 10_000L,
        )
        assertEquals(SourceHealthStatus.UNKNOWN, offline.effectiveStatus(nowMillis = 12_000L))

        val singleDns = SourceHealth(
            sourceId = 2L,
            lastSuccess = 0L,
            lastFailure = 10_000L,
            failureCount = 1,
            successCount = 0,
            avgLatency = 0L,
            lastError = "Unable to resolve host",
            consecutiveFailures = 1,
            lastCategory = SourceHealthCategory.DNS_FAILURE,
            lastChecked = 10_000L,
        )
        assertEquals(SourceHealthStatus.UNREACHABLE, singleDns.effectiveStatus(nowMillis = 12_000L))
        assertFalse(singleDns.isLikelyDead)

        val twoNotFound = singleDns.copy(consecutiveFailures = 2, lastCategory = SourceHealthCategory.HTTP_NOT_FOUND)
        assertEquals(SourceHealthStatus.UNREACHABLE, twoNotFound.effectiveStatus(nowMillis = 12_000L))
        assertFalse(twoNotFound.isLikelyDead)

        val threeDns = singleDns.copy(consecutiveFailures = 3)
        assertEquals(SourceHealthStatus.SUSPECTED_DEAD, threeDns.effectiveStatus(nowMillis = 12_000L))

        val repeatedRateLimit = singleDns.copy(consecutiveFailures = 5, lastCategory = SourceHealthCategory.HTTP_RATE_LIMITED)
        assertEquals(SourceHealthStatus.DEGRADED, repeatedRateLimit.effectiveStatus(nowMillis = 12_000L))
        assertFalse(repeatedRateLimit.isLikelyDead)
    }

    @Test
    fun `stale checks are flagged after threshold`() {
        val now = 2_000_000_000L
        val staleTime = now - SourceHealth.DEFAULT_STALE_THRESHOLD_MS - 1_000L
        val staleHealthy = SourceHealth(
            sourceId = 3L,
            lastSuccess = staleTime,
            lastFailure = 0L,
            failureCount = 0,
            successCount = 2,
            avgLatency = 120L,
            lastError = null,
            lastCategory = SourceHealthCategory.HEALTHY,
            lastProbeType = HealthProbeType.BASE_URL_GET,
            lastChecked = staleTime,
        )
        assertTrue(staleHealthy.isStale(now))
        assertEquals(SourceHealthStatus.STALE, staleHealthy.effectiveStatus(now))

        val migratedFailure = staleHealthy.copy(
            lastSuccess = 0L,
            lastFailure = staleTime,
            lastCategory = SourceHealthCategory.UNKNOWN,
            lastChecked = 0L,
            lastError = "Unable to resolve host",
        )
        assertEquals(staleTime, migratedFailure.effectiveLastChecked)
        assertEquals(SourceHealthStatus.STALE, migratedFailure.effectiveStatus(now))

        val staleDns = migratedFailure.copy(
            lastChecked = staleTime,
            lastCategory = SourceHealthCategory.DNS_FAILURE,
            consecutiveFailures = 3,
        )
        assertEquals(SourceHealthStatus.STALE, staleDns.effectiveStatus(now))

        val freshHealthy = staleHealthy.copy(lastSuccess = now - 60_000L, lastChecked = now - 60_000L)
        assertFalse(freshHealthy.isStale(now))
        assertEquals(SourceHealthStatus.HEALTHY, freshHealthy.effectiveStatus(now))
    }

    @Test
    fun `redactor strips credentials headers queries tokens and full repository urls deterministically`() {
        val raw = """
            Authorization: Bearer secret-bearer-123
            Cookie: sid=alpha
            Proxy-Authorization=Basic dXNlcjpwYXNz
            Url: https://alice:secretpass@repo.example.org/private/index.min.json?token=querysecret#frag
            access_token="unterminated-quote
            Key: AIzaSyDummySecretValue1234567890123456
            GitHub: ghp_1234567890abcdef1234
        """.trimIndent()

        val redacted = DiagnosticRedactor.redact(raw)
        listOf("secret-bearer-123", "alpha", "dXNlcjpwYXNz", "alice", "secretpass", "querysecret", "frag", "unterminated-quote", "AIzaSy", "ghp_").forEach { secret ->
            assertFalse(redacted.contains(secret), "Secret '$secret' leaked into: $redacted")
        }
        assertTrue(redacted.contains("https://repo.example.org/private/index.min.json"))
        assertEquals(redacted, DiagnosticRedactor.redact(redacted))

        assertEquals(
            "repo.example.org",
            DiagnosticRedactor.redactRepoUrl("https://user:pass@repo.example.org/org/team/repo?key=123"),
        )
    }

    @Test
    fun `report builder defaults exclude recent errors removes unselected sections and redacts payload`() {
        val report = AppDiagnosticsReport(
            correlationId = "diag-test01",
            generatedAt = 1_700_000_000_000L,
            checks = listOf(
                DiagnosticCheckResult(
                    id = DiagnosticSectionId.APP_ENVIRONMENT,
                    title = DiagnosticSectionId.APP_ENVIRONMENT.title,
                    status = DiagnosticCheckStatus.OK,
                    timestamp = 1_700_000_000_000L,
                    correlationId = "diag-test01",
                    phase = DiagnosticPhase.COMPLETED,
                    summary = "ShinKu 2.8.5",
                ),
                DiagnosticCheckResult(
                    id = DiagnosticSectionId.SOURCE_HEALTH,
                    title = DiagnosticSectionId.SOURCE_HEALTH.title,
                    status = DiagnosticCheckStatus.WARNING,
                    timestamp = 1_700_000_000_000L,
                    correlationId = "diag-test01",
                    phase = DiagnosticPhase.SOURCE_REQUEST,
                    summary = "Saw error at https://user:pw@host.test/api?api_key=leak",
                ),
                DiagnosticCheckResult(
                    id = DiagnosticSectionId.RECENT_ERRORS,
                    title = DiagnosticSectionId.RECENT_ERRORS.title,
                    status = DiagnosticCheckStatus.WARNING,
                    timestamp = 1_700_000_000_000L,
                    correlationId = "diag-test01",
                    phase = DiagnosticPhase.COMPLETED,
                    summary = "1 error line",
                    evidence = listOf("Cookie: secret=xyz"),
                ),
            ),
        )

        val defaultSelected = DiagnosticReportBuilder.defaultSelectedSections(report)
        assertTrue(DiagnosticSectionId.APP_ENVIRONMENT in defaultSelected)
        assertTrue(DiagnosticSectionId.SOURCE_HEALTH in defaultSelected)
        assertFalse(DiagnosticSectionId.RECENT_ERRORS in defaultSelected)

        val defaultPayload = DiagnosticReportBuilder.buildExportPayload(report, defaultSelected)
        assertTrue(defaultPayload.contains("[APP_ENVIRONMENT]"))
        assertTrue(defaultPayload.contains("[SOURCE_HEALTH]"))
        assertTrue(defaultPayload.contains("https://host.test/api"))
        assertFalse(defaultPayload.contains("RECENT_ERRORS"))
        assertFalse(defaultPayload.contains("pw"))
        assertFalse(defaultPayload.contains("leak"))
        assertFalse(defaultPayload.contains("xyz"))

        val filteredPayload = DiagnosticReportBuilder.buildExportPayload(
            report,
            setOf(DiagnosticSectionId.APP_ENVIRONMENT),
        )
        assertTrue(filteredPayload.contains("[APP_ENVIRONMENT]"))
        assertFalse(filteredPayload.contains("[SOURCE_HEALTH]"))

        val aiPrompt = DiagnosticReportBuilder.buildAiAdvisoryPrompt(report, defaultSelected)
        assertTrue(aiPrompt.contains("Never claim a source or extension is permanently dead from a single failed probe"))
        assertTrue(aiPrompt.contains("NOT_CHECKED"))
        assertFalse(aiPrompt.contains("leak"))
    }
}
