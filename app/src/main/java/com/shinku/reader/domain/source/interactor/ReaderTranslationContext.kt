package com.shinku.reader.domain.source.interactor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private val credentialHeader = Regex(
    """(?im)\b(?:proxy-authorization|authorization|set-cookie|cookie|x-api-key)\s*[:=][^\r\n]*""",
)
private val bearerToken = Regex("""(?i)\bBearer[ \t]+[^\s,;]+""")
private val urlCredentials = Regex("""(?i)(https?://)[^/\s?#]+@""")
private val urlQuery = Regex("""(?i)(https?://[^\s?="'<>]+)\?[^\s"'<>]*""")
private val secretValue = Regex(
    """(?i)\b((?:access[-_ ]?|refresh[-_ ]?|id[-_ ]?)token|api[-_ ]?key|password|secret)[ \t]*[:=][ \t]*(?:"[^"\r\n]*"?|'[^'\r\n]*'?|[^\s,;]+)""",
)
private val recognizableKey = Regex("""\b(?:AIza[0-9A-Za-z_-]{20,}|sk-[0-9A-Za-z_-]{12,}|gh[pousr]_[0-9A-Za-z_]{12,})\b""")

internal fun sanitizeReaderTranslationText(text: String): String {
    val headersRemoved = credentialHeader.replace(text, "[redacted header]")
    val tokensRemoved = bearerToken.replace(headersRemoved, "Bearer [redacted]")
    val urlsWithoutCredentials = urlCredentials.replace(tokensRemoved) { it.groupValues[1] }
    val urlsWithoutQueries = urlQuery.replace(urlsWithoutCredentials) { it.groupValues[1] }
    val secretsRemoved = secretValue.replace(urlsWithoutQueries) { "${it.groupValues[1]}=[redacted]" }
    return recognizableKey.replace(secretsRemoved, "[redacted]")
}

internal class ReaderTranslationDeclined : CancellationException("Reader translation declined")

internal suspend fun <T> translateReaderContext(
    engine: String,
    cloudConfigured: () -> Boolean,
    translateLocally: suspend () -> T,
    translateRemotely: suspend () -> T,
): T {
    if (engine == "mlkit" || engine == "aicore") return translateLocally()

    val cloudOnly = engine == "gemini_cloud" || engine == "cloud"
    if (!cloudConfigured()) {
        check(!cloudOnly) { "AI API Key Required. Please configure your AI Provider in Settings." }
        return translateLocally()
    }

    return try {
        translateRemotely()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (cloudOnly) throw e
        translateLocally()
    }
}

internal suspend fun sendReaderTranslation(
    destination: String,
    prompt: String,
    authorize: suspend (destination: String, prompt: String) -> Boolean,
    send: suspend (String) -> String,
): String {
    if (!authorize(destination, prompt)) throw ReaderTranslationDeclined()
    currentCoroutineContext().ensureActive()
    return send(prompt)
}
