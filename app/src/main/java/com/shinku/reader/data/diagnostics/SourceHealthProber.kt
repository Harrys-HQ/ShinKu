package com.shinku.reader.data.diagnostics

import com.shinku.reader.domain.diagnostics.DiagnosticCheckStatus
import com.shinku.reader.domain.diagnostics.DiagnosticRedactor
import com.shinku.reader.domain.diagnostics.RepositoryHealthCheck
import com.shinku.reader.domain.extensionrepo.model.ExtensionRepo
import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.domain.source.model.HealthProbeType
import com.shinku.reader.domain.source.model.SourceHealthCategory
import com.shinku.reader.domain.source.model.SourceHealthClassifier
import com.shinku.reader.domain.source.model.SourceProbeOutcome
import com.shinku.reader.extension.api.ExtensionIndexDto
import com.shinku.reader.extension.api.ExtensionJsonObject
import com.shinku.reader.extension.api.decompressIfGzipped
import com.shinku.reader.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Buffer
import okio.BufferedSource
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Bounded, read-only prober for source base URLs and extension repository indexes.
 * Strips interactive WebView Cloudflare interceptors on copied clients so background/manual probes
 * observe the real server response without hijacking UI focus.
 */
object SourceHealthProber {

    const val DEFAULT_TIMEOUT_MS = 15_000L
    internal const val MAX_INDEX_BYTES = 16L * 1024 * 1024

    private fun probeClientFrom(baseClient: OkHttpClient, timeoutMs: Long): OkHttpClient =
        baseClient.newBuilder()
            .cookieJar(CookieJar.NO_COOKIES)
            .cache(null)
            .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .apply { interceptors().removeAll { it is CloudflareInterceptor } }
            .build()

    suspend fun probeSourceBaseUrl(
        client: OkHttpClient,
        targetUrl: String,
        headers: Headers = Headers.headersOf(),
        isNetworkAvailable: () -> Boolean,
        correlationId: String? = null,
        probeType: HealthProbeType = HealthProbeType.BASE_URL_GET,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): SourceProbeOutcome = withContext(Dispatchers.IO) {
        val sanitizedTarget = DiagnosticRedactor.sanitizeUrlTarget(targetUrl)
        if (targetUrl.isBlank()) {
            return@withContext SourceProbeOutcome(
                isSuccess = false,
                category = SourceHealthCategory.EXTENSION_UNAVAILABLE,
                probeType = probeType,
                phase = DiagnosticPhase.SOURCE_REQUEST,
                sanitizedSummary = "Source has no configured base URL",
                sanitizedTarget = sanitizedTarget,
                correlationId = correlationId,
                countAsFailure = false,
            )
        }
        if (!isNetworkAvailable()) {
            return@withContext SourceProbeOutcome(
                isSuccess = false,
                category = SourceHealthCategory.OFFLINE,
                probeType = probeType,
                phase = DiagnosticPhase.SOURCE_REQUEST,
                sanitizedSummary = "Device appears offline",
                sanitizedTarget = sanitizedTarget,
                correlationId = correlationId,
                countAsFailure = false,
            )
        }

        val safeClient = probeClientFrom(client, timeoutMs)
        var outcome: SourceProbeOutcome? = null
        val elapsed = measureTimeMillis {
            try {
                withTimeout(timeoutMs) {
                    safeClient.newCall(GET(targetUrl, headers)).await().use { response ->
                        outcome = SourceHealthClassifier.classifyHttpResponse(
                            statusCode = response.code,
                            headers = response.headerMap(),
                            probeType = probeType,
                            phase = DiagnosticPhase.SOURCE_REQUEST,
                            latencyMs = 0L,
                            targetUrl = targetUrl,
                            correlationId = correlationId,
                        )
                    }
                }
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    outcome = SourceHealthClassifier.classifyThrowable(
                        throwable = e,
                        isNetworkAvailable = isNetworkAvailable(),
                        probeType = probeType,
                        phase = DiagnosticPhase.SOURCE_REQUEST,
                        latencyMs = 0L,
                        targetUrl = targetUrl,
                        correlationId = correlationId,
                    )
                } else {
                    throw e
                }
            } catch (t: Throwable) {
                outcome = SourceHealthClassifier.classifyThrowable(
                    throwable = t,
                    isNetworkAvailable = isNetworkAvailable(),
                    probeType = probeType,
                    phase = DiagnosticPhase.SOURCE_REQUEST,
                    latencyMs = 0L,
                    targetUrl = targetUrl,
                    correlationId = correlationId,
                )
            }
        }

        val resolved = checkNotNull(outcome)
        resolved.copy(latencyMs = elapsed)
    }

    suspend fun probeExtensionRepo(
        client: OkHttpClient,
        repo: ExtensionRepo,
        protoBuf: ProtoBuf,
        json: Json,
        isNetworkAvailable: () -> Boolean,
        correlationId: String? = null,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): RepositoryHealthCheck = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val displayName = repo.shortName?.takeIf { it.isNotBlank() } ?: repo.name.ifBlank { "Repository" }
        val repoHost = DiagnosticRedactor.redactRepoUrl(repo.baseUrl)
        val cleanBaseUrl = repo.baseUrl.trim().removeSuffix("/")

        if (!isNetworkAvailable()) {
            return@withContext RepositoryHealthCheck(
                repoName = displayName,
                repoHost = repoHost,
                status = DiagnosticCheckStatus.NOT_CHECKED,
                category = SourceHealthCategory.OFFLINE,
                checkedAt = now,
                correlationId = correlationId,
                phase = DiagnosticPhase.REPOSITORY_FETCH,
                summary = "Device offline; repository index was not checked",
                nextStep = SourceHealthClassifier.plainLanguageNextStep(SourceHealthCategory.OFFLINE, 0),
            )
        }

        val safeClient = probeClientFrom(client, timeoutMs)
        val pbResult = probeProtobufIndex(safeClient, cleanBaseUrl, protoBuf, isNetworkAvailable, correlationId, timeoutMs)
        if (pbResult != null && pbResult.category == SourceHealthCategory.HEALTHY) {
            return@withContext pbResult.toRepoCheck(displayName, repoHost, now, correlationId)
        }

        val jsonMinResult = probeJsonIndex(safeClient, "$cleanBaseUrl/index.min.json", json, isNetworkAvailable, correlationId, timeoutMs)
        if (jsonMinResult.category == SourceHealthCategory.HEALTHY) {
            return@withContext jsonMinResult.toRepoCheck(displayName, repoHost, now, correlationId)
        }

        val jsonFallbackResult = if (jsonMinResult.statusCode == 404 || jsonMinResult.category == SourceHealthCategory.HTTP_NOT_FOUND) {
            probeJsonIndex(safeClient, "$cleanBaseUrl/index.json", json, isNetworkAvailable, correlationId, timeoutMs)
        } else {
            null
        }
        if (jsonFallbackResult != null && jsonFallbackResult.category == SourceHealthCategory.HEALTHY) {
            return@withContext jsonFallbackResult.toRepoCheck(displayName, repoHost, now, correlationId)
        }

        val chosenFailure = when {
            pbResult?.category == SourceHealthCategory.REPO_INDEX_INVALID -> pbResult
            jsonMinResult.category == SourceHealthCategory.REPO_INDEX_INVALID -> jsonMinResult
            jsonFallbackResult?.category == SourceHealthCategory.REPO_INDEX_INVALID -> jsonFallbackResult
            jsonFallbackResult != null && jsonFallbackResult.statusCode != 404 -> jsonFallbackResult
            pbResult != null && pbResult.statusCode != 404 -> pbResult
            else -> jsonMinResult
        }

        chosenFailure.toRepoCheck(displayName, repoHost, now, correlationId)
    }

    private suspend fun probeProtobufIndex(
        client: OkHttpClient,
        cleanBaseUrl: String,
        protoBuf: ProtoBuf,
        isNetworkAvailable: () -> Boolean,
        correlationId: String?,
        timeoutMs: Long,
    ): RepoAttempt? {
        val pbUrl = "$cleanBaseUrl/index.pb"
        return executeRepoAttempt(client, pbUrl, isNetworkAvailable, correlationId, timeoutMs) { bytes ->
            val indexDto = Buffer().write(bytes).decompressIfGzipped().use { source ->
                protoBuf.decodeFromByteArray<ExtensionIndexDto>(source.readIndexBytes())
            }
            val extensions = when {
                indexDto.extensionList != null -> indexDto.extensionList.extensions
                !indexDto.extensionListUrl.isNullOrBlank() -> {
                    val listUrl = resolveRelativeUrl(cleanBaseUrl, indexDto.extensionListUrl)
                    val listBytes = fetchBytesOrThrow(client, listUrl, timeoutMs)
                    Buffer().write(listBytes).decompressIfGzipped().use { listSource ->
                        protoBuf.decodeFromByteArray<ExtensionIndexDto.ExtensionList>(listSource.readIndexBytes()).extensions
                    }
                }
                else -> emptyList()
            }
            if (extensions.isEmpty() && indexDto.name.isBlank() && indexDto.signingKey.isBlank()) {
                throw SerializationException("Protobuf index contains no extensions or metadata")
            }
            val compatible = extensions.count { ext ->
                val lib = ext.extensionLib.toDoubleOrNull()
                    ?: ext.versionName.substringBeforeLast('.').toDoubleOrNull()
                    ?: return@count false
                lib in ExtensionLoader.LIB_VERSION_MIN..ExtensionLoader.LIB_VERSION_MAX
            }
            "Protobuf index OK (${extensions.size} listed, $compatible compatible)"
        }
    }

    private suspend fun probeJsonIndex(
        client: OkHttpClient,
        url: String,
        json: Json,
        isNetworkAvailable: () -> Boolean,
        correlationId: String?,
        timeoutMs: Long,
    ): RepoAttempt {
        return executeRepoAttempt(client, url, isNetworkAvailable, correlationId, timeoutMs) { bytes ->
            val items = json.decodeFromString<List<ExtensionJsonObject>>(bytes.decodeToString())
            val compatible = items.count { ext ->
                val lib = ext.version.substringBeforeLast('.').toDoubleOrNull() ?: 0.0
                lib in ExtensionLoader.LIB_VERSION_MIN..ExtensionLoader.LIB_VERSION_MAX
            }
            "JSON index OK (${items.size} listed, $compatible compatible)"
        }
    }

    private suspend fun executeRepoAttempt(
        client: OkHttpClient,
        url: String,
        isNetworkAvailable: () -> Boolean,
        correlationId: String?,
        timeoutMs: Long,
        parse: suspend (ByteArray) -> String,
    ): RepoAttempt {
        var phase = DiagnosticPhase.REPOSITORY_FETCH
        var statusCode: Int? = null
        var attemptResult: RepoAttempt? = null
        val elapsed = measureTimeMillis {
            try {
                val bytes = withTimeout(timeoutMs) {
                    client.newCall(GET(url)).await().use { response ->
                        statusCode = response.code
                        if (!response.isSuccessful) {
                            val httpOutcome = SourceHealthClassifier.classifyHttpResponse(
                                statusCode = response.code,
                                headers = response.headerMap(),
                                probeType = HealthProbeType.REPO_INDEX_FETCH,
                                phase = DiagnosticPhase.REPOSITORY_FETCH,
                                targetUrl = url,
                                correlationId = correlationId,
                            )
                            attemptResult = RepoAttempt(
                                category = httpOutcome.category,
                                statusCode = response.code,
                                phase = DiagnosticPhase.REPOSITORY_FETCH,
                                summary = httpOutcome.sanitizedSummary ?: "HTTP ${response.code}",
                            )
                            return@use null
                        }
                        runInterruptible { response.body.source().readIndexBytes() }
                    }
                }
                if (bytes != null) {
                    phase = DiagnosticPhase.INDEX_PARSE
                    val summary = parse(bytes)
                    attemptResult = RepoAttempt(
                        category = SourceHealthCategory.HEALTHY,
                        statusCode = statusCode,
                        phase = DiagnosticPhase.COMPLETED,
                        summary = summary,
                    )
                }
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    val outcome = SourceHealthClassifier.classifyThrowable(
                        throwable = e,
                        isNetworkAvailable = isNetworkAvailable(),
                        probeType = HealthProbeType.REPO_INDEX_FETCH,
                        phase = phase,
                        targetUrl = url,
                        correlationId = correlationId,
                    )
                    attemptResult = RepoAttempt(
                        category = outcome.category,
                        statusCode = statusCode,
                        phase = phase,
                        summary = outcome.sanitizedSummary ?: "Timed out",
                    )
                } else {
                    throw e
                }
            } catch (e: IndexTooLargeException) {
                attemptResult = RepoAttempt(
                    category = SourceHealthCategory.REPO_INDEX_INVALID,
                    statusCode = statusCode,
                    phase = phase,
                    summary = "Index exceeds the 16 MiB diagnostic limit",
                )
            } catch (t: Exception) {
                val outcome = SourceHealthClassifier.classifyThrowable(
                    throwable = t,
                    isNetworkAvailable = isNetworkAvailable(),
                    probeType = HealthProbeType.REPO_INDEX_FETCH,
                    phase = phase,
                    targetUrl = url,
                    correlationId = correlationId,
                )
                val finalCategory = if (phase == DiagnosticPhase.INDEX_PARSE && outcome.category == SourceHealthCategory.UNKNOWN) {
                    SourceHealthCategory.REPO_INDEX_INVALID
                } else {
                    outcome.category
                }
                attemptResult = RepoAttempt(
                    category = finalCategory,
                    statusCode = statusCode ?: outcome.statusCode,
                    phase = phase,
                    summary = finalCategory.name,
                )
            }
        }
        val resolved = checkNotNull(attemptResult)
        return resolved.copy(latencyMs = elapsed)
    }

    private suspend fun fetchBytesOrThrow(
        client: OkHttpClient,
        url: String,
        timeoutMs: Long,
    ): ByteArray = withTimeout(timeoutMs) {
        client.newCall(GET(url)).await().use { response ->
            if (!response.isSuccessful) {
                throw eu.kanade.tachiyomi.network.HttpException(response.code)
            }
            runInterruptible { response.body.source().readIndexBytes() }
        }
    }

    private fun BufferedSource.readIndexBytes(): ByteArray {
        if (request(MAX_INDEX_BYTES + 1)) throw IndexTooLargeException()
        return readByteArray()
    }

    private class IndexTooLargeException : Exception()

    private fun resolveRelativeUrl(cleanBaseUrl: String, relativeOrFull: String): String {
        return when {
            relativeOrFull.startsWith("http://") || relativeOrFull.startsWith("https://") -> relativeOrFull
            relativeOrFull.startsWith("/") -> "$cleanBaseUrl$relativeOrFull"
            else -> "$cleanBaseUrl/$relativeOrFull"
        }
    }

    private fun Response.headerMap(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for (i in 0 until headers.size) {
            result[headers.name(i)] = headers.value(i)
        }
        return result
    }

    private data class RepoAttempt(
        val category: SourceHealthCategory,
        val statusCode: Int? = null,
        val phase: DiagnosticPhase,
        val summary: String,
        val latencyMs: Long = 0L,
    ) {
        fun toRepoCheck(
            displayName: String,
            repoHost: String,
            checkedAt: Long,
            correlationId: String?,
        ): RepositoryHealthCheck {
            val status = when (category) {
                SourceHealthCategory.HEALTHY -> DiagnosticCheckStatus.OK
                SourceHealthCategory.OFFLINE -> DiagnosticCheckStatus.NOT_CHECKED
                SourceHealthCategory.HTTP_RATE_LIMITED,
                SourceHealthCategory.CLOUDFLARE_BLOCKED,
                SourceHealthCategory.READ_TIMEOUT,
                -> DiagnosticCheckStatus.WARNING
                else -> DiagnosticCheckStatus.FAILED
            }
            return RepositoryHealthCheck(
                repoName = displayName,
                repoHost = repoHost,
                status = status,
                category = category,
                statusCode = statusCode,
                latencyMs = latencyMs,
                checkedAt = checkedAt,
                correlationId = correlationId,
                phase = phase,
                summary = DiagnosticRedactor.redact(summary),
                nextStep = SourceHealthClassifier.plainLanguageNextStep(category, if (category == SourceHealthCategory.HEALTHY) 0 else 1, statusCode),
            )
        }
    }
}
