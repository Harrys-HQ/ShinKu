package com.shinku.reader.domain.diagnostics

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object DiagnosticRedactor {

    private val credentialHeader = Regex(
        """(?im)\b(?:proxy-authorization|authorization|set-cookie|cookie|x-api-key|api-key|x-auth-token)\s*[:=][^\r\n]*""",
    )
    private val authSchemeToken = Regex("""(?i)\b(Bearer|Basic)[ \t]+[^\s,;]+""")
    private val urlCredentials = Regex("""(?i)(https?://)[^/\s?#]+@""")
    private val urlQueryOrFragment = Regex("""(?i)(https?://[^\s?#'"<>]+)[?#][^\s'"<>]*""")
    private val secretKeyValue = Regex(
        """(?i)\b((?:access[-_ ]?|refresh[-_ ]?|id[-_ ]?)token|api[-_ ]?key|apikey|password|passwd|secret|session[-_ ]?id|session|cookie)[ \t]*[:=][ \t]*(?:"[^"\r\n]*"?|'[^'\r\n]*'?|[^\s,;\r\n]+)""",
    )
    private val recognizableKey = Regex(
        """\b(?:AIza[0-9A-Za-z_-]{20,}|sk-or-v1-[0-9A-Za-z_-]{12,}|sk-[0-9A-Za-z_-]{12,}|gh[pousr]_[0-9A-Za-z_]{12,}|eyJ[0-9A-Za-z_-]{8,}\.eyJ[0-9A-Za-z_-]{8,}\.[0-9A-Za-z_-]{8,})\b""",
    )

    fun redact(text: String): String {
        if (text.isEmpty()) return text
        val headersRemoved = credentialHeader.replace(text, "[redacted header]")
        val tokensRemoved = authSchemeToken.replace(headersRemoved) { "${it.groupValues[1]} [redacted]" }
        val urlsWithoutCreds = urlCredentials.replace(tokensRemoved) { it.groupValues[1] }
        val urlsWithoutQuery = urlQueryOrFragment.replace(urlsWithoutCreds) { it.groupValues[1] }
        val secretsRemoved = secretKeyValue.replace(urlsWithoutQuery) { "${it.groupValues[1]}=[redacted]" }
        return recognizableKey.replace(secretsRemoved, "[redacted]")
    }

    fun sanitizeUrlTarget(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val parsed = trimmed.toHttpUrlOrNull()
        if (parsed != null) {
            val portSuffix = if ((parsed.scheme == "http" && parsed.port == 80) || (parsed.scheme == "https" && parsed.port == 443)) {
                ""
            } else {
                ":${parsed.port}"
            }
            val cleanPath = parsed.encodedPath.takeIf { it.isNotEmpty() && it != "/" }.orEmpty()
            return "${parsed.scheme}://${parsed.host}$portSuffix$cleanPath"
        }
        return redact(trimmed)
    }

    fun redactRepoUrl(rawUrl: String?): String {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return "[unconfigured]"
        val parsed = trimmed.toHttpUrlOrNull()
        if (parsed != null) {
            val portSuffix = if ((parsed.scheme == "http" && parsed.port == 80) || (parsed.scheme == "https" && parsed.port == 443)) {
                ""
            } else {
                ":${parsed.port}"
            }
            return "${parsed.host}$portSuffix"
        }
        return "[invalid repository]"
    }
}
