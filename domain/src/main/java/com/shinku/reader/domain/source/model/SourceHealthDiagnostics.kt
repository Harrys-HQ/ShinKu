package com.shinku.reader.domain.source.model

import com.shinku.reader.domain.diagnostics.DiagnosticRedactor
import eu.kanade.tachiyomi.network.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException

enum class SourceHealthCategory {
    UNKNOWN,
    HEALTHY,
    OFFLINE,
    DNS_FAILURE,
    CONNECT_TIMEOUT,
    READ_TIMEOUT,
    TLS_ERROR,
    HTTP_UNAUTHORIZED,
    HTTP_FORBIDDEN,
    CLOUDFLARE_BLOCKED,
    HTTP_NOT_FOUND,
    HTTP_RATE_LIMITED,
    HTTP_SERVER_ERROR,
    HTTP_REDIRECT_BLOCKED,
    REPO_INDEX_INVALID,
    EXTENSION_UNAVAILABLE,
    PARSER_ERROR,
    ;

    companion object {
        fun fromStorage(value: String?): SourceHealthCategory {
            if (value.isNullOrBlank()) return UNKNOWN
            return entries.firstOrNull { it.name == value } ?: UNKNOWN
        }
    }
}

enum class HealthProbeType {
    UNKNOWN,
    BASE_URL_GET,
    REPO_INDEX_FETCH,
    EXTENSION_LOAD,
    SOURCE_OPERATION,
    ;

    companion object {
        fun fromStorage(value: String?): HealthProbeType {
            if (value.isNullOrBlank()) return UNKNOWN
            return entries.firstOrNull { it.name == value } ?: UNKNOWN
        }
    }
}

enum class DiagnosticPhase {
    IDLE,
    REPOSITORY_FETCH,
    INDEX_PARSE,
    EXTENSION_LOAD,
    SOURCE_REQUEST,
    DATABASE_UPDATE,
    COMPLETED,
    CANCELLED,
    FAILED,
    ;

    companion object {
        fun fromStorage(value: String?): DiagnosticPhase {
            if (value.isNullOrBlank()) return IDLE
            return entries.firstOrNull { it.name == value } ?: IDLE
        }
    }
}

enum class SourceHealthStatus {
    UNKNOWN,
    HEALTHY,
    STALE,
    DEGRADED,
    UNREACHABLE,
    SUSPECTED_DEAD,
}

data class SourceProbeOutcome(
    val isSuccess: Boolean,
    val category: SourceHealthCategory,
    val statusCode: Int? = null,
    val latencyMs: Long = 0L,
    val probeType: HealthProbeType = HealthProbeType.UNKNOWN,
    val phase: DiagnosticPhase = DiagnosticPhase.SOURCE_REQUEST,
    val sanitizedSummary: String? = null,
    val sanitizedTarget: String? = null,
    val correlationId: String? = null,
    val countAsFailure: Boolean = !isSuccess && category != SourceHealthCategory.OFFLINE,
)

object SourceHealthClassifier {

    fun classifyHttpResponse(
        statusCode: Int,
        headers: Map<String, String> = emptyMap(),
        probeType: HealthProbeType = HealthProbeType.BASE_URL_GET,
        phase: DiagnosticPhase = DiagnosticPhase.SOURCE_REQUEST,
        latencyMs: Long = 0L,
        targetUrl: String? = null,
        correlationId: String? = null,
    ): SourceProbeOutcome {
        val normalizedHeaders = headers.entries.associate { (k, v) -> k.lowercase() to v }
        val cfMitigated = normalizedHeaders["cf-mitigated"]
        val server = normalizedHeaders["server"]
        val hasCloudflareSignal = cfMitigated.equals("challenge", ignoreCase = true) ||
            (server?.contains("cloudflare", ignoreCase = true) == true && (cfMitigated != null || normalizedHeaders.containsKey("cf-ray")))

        val category = when (statusCode) {
            in 200..299 -> SourceHealthCategory.HEALTHY
            in 300..399 -> SourceHealthCategory.HTTP_REDIRECT_BLOCKED
            401 -> SourceHealthCategory.HTTP_UNAUTHORIZED
            403 -> if (hasCloudflareSignal) SourceHealthCategory.CLOUDFLARE_BLOCKED else SourceHealthCategory.HTTP_FORBIDDEN
            404, 410 -> SourceHealthCategory.HTTP_NOT_FOUND
            429 -> SourceHealthCategory.HTTP_RATE_LIMITED
            503 -> if (cfMitigated.equals("challenge", ignoreCase = true)) {
                SourceHealthCategory.CLOUDFLARE_BLOCKED
            } else {
                SourceHealthCategory.HTTP_SERVER_ERROR
            }
            in 500..599 -> SourceHealthCategory.HTTP_SERVER_ERROR
            else -> SourceHealthCategory.UNKNOWN
        }

        val isSuccess = category == SourceHealthCategory.HEALTHY
        val summary = if (isSuccess) {
            null
        } else {
            when (category) {
                SourceHealthCategory.CLOUDFLARE_BLOCKED -> "HTTP $statusCode (Cloudflare challenge)"
                SourceHealthCategory.HTTP_REDIRECT_BLOCKED -> "HTTP $statusCode (Unexpected redirect)"
                else -> "HTTP $statusCode"
            }
        }

        return SourceProbeOutcome(
            isSuccess = isSuccess,
            category = category,
            statusCode = statusCode,
            latencyMs = latencyMs,
            probeType = probeType,
            phase = phase,
            sanitizedSummary = summary,
            sanitizedTarget = DiagnosticRedactor.sanitizeUrlTarget(targetUrl),
            correlationId = correlationId,
        )
    }

    fun classifyThrowable(
        throwable: Throwable,
        isNetworkAvailable: Boolean = true,
        probeType: HealthProbeType = HealthProbeType.BASE_URL_GET,
        phase: DiagnosticPhase = DiagnosticPhase.SOURCE_REQUEST,
        latencyMs: Long = 0L,
        targetUrl: String? = null,
        correlationId: String? = null,
    ): SourceProbeOutcome {
        if (throwable is CancellationException && throwable !is TimeoutCancellationException) {
            throw throwable
        }

        val sanitizedTarget = DiagnosticRedactor.sanitizeUrlTarget(targetUrl)

        if (!isNetworkAvailable) {
            return SourceProbeOutcome(
                isSuccess = false,
                category = SourceHealthCategory.OFFLINE,
                latencyMs = latencyMs,
                probeType = probeType,
                phase = phase,
                sanitizedSummary = "Device appears offline",
                sanitizedTarget = sanitizedTarget,
                correlationId = correlationId,
                countAsFailure = false,
            )
        }

        val chain = collectCauseChain(throwable)
        if (chain.any { it is CancellationException && it !is TimeoutCancellationException }) {
            throw throwable
        }

        val httpException = chain.filterIsInstance<HttpException>().firstOrNull()
        if (httpException != null) {
            return classifyHttpResponse(
                statusCode = httpException.code,
                probeType = probeType,
                phase = phase,
                latencyMs = latencyMs,
                targetUrl = targetUrl,
                correlationId = correlationId,
            )
        }

        val category = classifyFromChain(chain, phase, probeType)
        val rawMessage = chain.mapNotNull { it.message?.takeIf(String::isNotBlank) }.firstOrNull()
            ?: throwable::class.simpleName
            ?: "Unknown error"

        return SourceProbeOutcome(
            isSuccess = false,
            category = category,
            latencyMs = latencyMs,
            probeType = probeType,
            phase = phase,
            sanitizedSummary = DiagnosticRedactor.redact(rawMessage).take(240),
            sanitizedTarget = sanitizedTarget,
            correlationId = correlationId,
        )
    }

    private fun classifyFromChain(
        chain: List<Throwable>,
        phase: DiagnosticPhase,
        probeType: HealthProbeType,
    ): SourceHealthCategory {
        for (error in chain) {
            val className = error::class.simpleName.orEmpty()
            when {
                className.contains("CloudflareBypass", ignoreCase = true) ||
                    className.contains("Cloudflare", ignoreCase = true) -> return SourceHealthCategory.CLOUDFLARE_BLOCKED
                error is UnknownHostException -> return SourceHealthCategory.DNS_FAILURE
                error is SSLException || error is CertificateException || error is CertPathValidatorException ->
                    return SourceHealthCategory.TLS_ERROR
                error is ConnectException || error is NoRouteToHostException || error is PortUnreachableException ->
                    return SourceHealthCategory.CONNECT_TIMEOUT
                error is SocketTimeoutException -> {
                    return if (error.message?.contains("connect", ignoreCase = true) == true) {
                        SourceHealthCategory.CONNECT_TIMEOUT
                    } else {
                        SourceHealthCategory.READ_TIMEOUT
                    }
                }
                error is TimeoutCancellationException -> return SourceHealthCategory.READ_TIMEOUT
                error is ProtocolException -> return SourceHealthCategory.HTTP_REDIRECT_BLOCKED
                error is SerializationException -> {
                    return if (phase == DiagnosticPhase.INDEX_PARSE) {
                        SourceHealthCategory.REPO_INDEX_INVALID
                    } else {
                        SourceHealthCategory.PARSER_ERROR
                    }
                }
                error is LinkageError ||
                    error is ClassNotFoundException ||
                    error is SourceNotInstalledException ||
                    className.contains("SourceNotInstalled", ignoreCase = true) ->
                    return SourceHealthCategory.EXTENSION_UNAVAILABLE
            }
        }

        val combinedMessage = chain.mapNotNull { it.message }.joinToString(" | ")
        val inferred = inferLegacyCategory(combinedMessage)
        if (inferred != SourceHealthCategory.UNKNOWN) return inferred

        return when {
            phase == DiagnosticPhase.INDEX_PARSE -> SourceHealthCategory.REPO_INDEX_INVALID
            phase == DiagnosticPhase.EXTENSION_LOAD -> SourceHealthCategory.EXTENSION_UNAVAILABLE
            phase == DiagnosticPhase.SOURCE_REQUEST && probeType == HealthProbeType.SOURCE_OPERATION ->
                SourceHealthCategory.PARSER_ERROR
            else -> SourceHealthCategory.UNKNOWN
        }
    }

    private fun collectCauseChain(root: Throwable): List<Throwable> {
        val result = mutableListOf<Throwable>()
        val visited = HashSet<Throwable>()
        var current: Throwable? = root
        while (current != null && visited.add(current)) {
            result += current
            current = current.cause
        }
        return result
    }

    fun inferLegacyCategory(lastError: String?): SourceHealthCategory {
        if (lastError.isNullOrBlank()) return SourceHealthCategory.UNKNOWN
        val msg = lastError.lowercase()
        return when {
            msg.contains("unable to resolve host") || msg.contains("unknownhostexception") || msg.contains("dns") ->
                SourceHealthCategory.DNS_FAILURE
            msg.contains("cloudflare") -> SourceHealthCategory.CLOUDFLARE_BLOCKED
            msg.contains("403") -> SourceHealthCategory.HTTP_FORBIDDEN
            msg.contains("401") -> SourceHealthCategory.HTTP_UNAUTHORIZED
            msg.contains("404") || msg.contains("410") -> SourceHealthCategory.HTTP_NOT_FOUND
            msg.contains("429") || msg.contains("rate limit") -> SourceHealthCategory.HTTP_RATE_LIMITED
            msg.contains("522") || msg.contains("524") || msg.contains("502") || msg.contains("503") || msg.contains("504") || msg.contains("500") ->
                SourceHealthCategory.HTTP_SERVER_ERROR
            msg.contains("ssl") || msg.contains("certificate") || msg.contains("handshake") ->
                SourceHealthCategory.TLS_ERROR
            msg.contains("connect") && msg.contains("timeout") -> SourceHealthCategory.CONNECT_TIMEOUT
            msg.contains("timeout") || msg.contains("timed out") -> SourceHealthCategory.READ_TIMEOUT
            msg.contains("redirect") -> SourceHealthCategory.HTTP_REDIRECT_BLOCKED
            msg.contains("offline") || msg.contains("no internet") -> SourceHealthCategory.OFFLINE
            msg.contains("not installed") || msg.contains("extension") -> SourceHealthCategory.EXTENSION_UNAVAILABLE
            else -> SourceHealthCategory.UNKNOWN
        }
    }

    fun plainLanguageNextStep(
        category: SourceHealthCategory,
        consecutiveFailures: Int,
        statusCode: Int? = null,
    ): String {
        return when (category) {
            SourceHealthCategory.HEALTHY -> "Source responded normally during the last check."
            SourceHealthCategory.UNKNOWN -> "No verified diagnostic result yet. Run a check to inspect reachability."
            SourceHealthCategory.OFFLINE -> "Device had no active network during this check. Reconnect and retry."
            SourceHealthCategory.DNS_FAILURE -> if (consecutiveFailures >= 3) {
                "Host could not be resolved across $consecutiveFailures checks. Check DNS/DoH settings, open in WebView, or consider migrating if the domain permanently changed."
            } else {
                "Host could not be resolved on this attempt. One DNS error does not prove the source is dead; check DNS/DoH or retry."
            }
            SourceHealthCategory.CONNECT_TIMEOUT -> "Connection timed out before the server replied. The host or local network may be temporarily unreachable."
            SourceHealthCategory.READ_TIMEOUT -> "Server connected but took too long to respond. Retry later or test individually."
            SourceHealthCategory.TLS_ERROR -> "Secure TLS/certificate validation failed. Check device date/time, VPN/proxy, or open in WebView to inspect."
            SourceHealthCategory.HTTP_UNAUTHORIZED -> "Server requested authentication (HTTP ${statusCode ?: 401}). Open the source in WebView to check login requirements."
            SourceHealthCategory.HTTP_FORBIDDEN -> "Server rejected automated access (HTTP ${statusCode ?: 403}). Open in WebView to check if access or verification is needed."
            SourceHealthCategory.CLOUDFLARE_BLOCKED -> "Protected by a bot/Cloudflare challenge. Open the source in WebView to pass verification and update cookies."
            SourceHealthCategory.HTTP_NOT_FOUND -> if (consecutiveFailures >= 3) {
                "Target returned Not Found (HTTP ${statusCode ?: 404}) repeatedly. The URL may have changed; check extension updates or migrate."
            } else {
                "Target returned Not Found (HTTP ${statusCode ?: 404}). Retry before assuming the source is dead."
            }
            SourceHealthCategory.HTTP_RATE_LIMITED -> "Too many requests sent (HTTP 429). ShinKu slows down requests automatically; wait a few minutes and retry."
            SourceHealthCategory.HTTP_SERVER_ERROR -> "Remote site returned a server error (HTTP ${statusCode ?: 500}). Wait and retry; the host is likely experiencing downtime."
            SourceHealthCategory.HTTP_REDIRECT_BLOCKED -> "Server returned an unexpected redirect (HTTP ${statusCode ?: 302}). Check in WebView to see if the domain moved or requires verification."
            SourceHealthCategory.REPO_INDEX_INVALID -> "Extension repository index could not be parsed or has an incompatible format. Verify the repository configuration."
            SourceHealthCategory.EXTENSION_UNAVAILABLE -> "Extension package is missing, disabled, untrusted, or incompatible with this app version."
            SourceHealthCategory.PARSER_ERROR -> "Site replied, but parsing chapters or pages failed. Check if an extension update is available."
        }
    }
}
