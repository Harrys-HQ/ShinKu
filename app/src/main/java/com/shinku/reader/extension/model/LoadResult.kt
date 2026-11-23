package com.shinku.reader.extension.model

import com.shinku.reader.domain.diagnostics.ExtensionLoadFailureReason

sealed interface LoadResult {
    data class Success(val extension: Extension.Installed) : LoadResult
    data class Untrusted(val extension: Extension.Untrusted) : LoadResult
    data class Error(
        val pkgName: String? = null,
        val name: String? = null,
        val reason: ExtensionLoadFailureReason = ExtensionLoadFailureReason.UNKNOWN,
        val detail: String? = null,
    ) : LoadResult
}
