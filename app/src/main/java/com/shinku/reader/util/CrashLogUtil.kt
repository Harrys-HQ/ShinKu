package com.shinku.reader.util

import android.content.Context
import android.os.Build
import com.shinku.reader.BuildConfig
import com.shinku.reader.core.common.util.lang.withNonCancellableContext
import com.shinku.reader.core.common.util.lang.withUIContext
import com.shinku.reader.domain.diagnostics.DiagnosticRedactor
import com.shinku.reader.exh.syDebugVersion
import com.shinku.reader.extension.ExtensionManager
import com.shinku.reader.util.storage.getUriCompat
import com.shinku.reader.util.system.WebViewUtil
import com.shinku.reader.util.system.createFileInCacheDir
import com.shinku.reader.util.system.toShareIntent
import com.shinku.reader.util.system.toast
import kotlinx.coroutines.flow.first
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.OffsetDateTime
import java.time.ZoneId

class CrashLogUtil(
    private val context: Context,
    private val extensionManager: ExtensionManager = Injekt.get(),
) {

    suspend fun dumpLogs(exception: Throwable? = null) = withNonCancellableContext {
        try {
            val file = context.createFileInCacheDir("tachiyomi_sy_crash_logs.txt")

            file.appendText(DiagnosticRedactor.redact(getDebugInfo()) + "\n\n")
            getExtensionsInfo()?.let { file.appendText("${DiagnosticRedactor.redact(it)}\n\n") }
            exception?.let { file.appendText("${DiagnosticRedactor.redact(it.stackTraceToString())}\n\n") }

            val recentErrors = readRedactedRecentErrors(maxLines = 200)
            if (recentErrors.isNotEmpty()) {
                file.appendText(recentErrors.joinToString("\n") + "\n")
            }

            val uri = file.getUriCompat(context)
            context.startActivity(uri.toShareIntent(context, "text/plain"))
        } catch (e: Throwable) {
            withUIContext { context.toast("Failed to get logs") }
        }
    }

    suspend fun shareDiagnosticReport(reportText: String) = withNonCancellableContext {
        try {
            val file = context.createFileInCacheDir("shinku_diagnostics_report.txt")
            file.writeText(DiagnosticRedactor.redact(reportText))
            val uri = file.getUriCompat(context)
            context.startActivity(uri.toShareIntent(context, "text/plain"))
        } catch (_: Throwable) {
            withUIContext { context.toast("Failed to export diagnostics") }
        }
    }

    fun readRedactedRecentErrors(maxLines: Int = 40): List<String> {
        return runCatching {
            val process = ProcessBuilder(
                "logcat",
                "*:E",
                "-d",
                "-v",
                "year",
                "-v",
                "zone",
                "-t",
                maxLines.coerceIn(1, 300).toString(),
            ).redirectErrorStream(true).start()

            val lines = process.inputStream.bufferedReader().use { reader ->
                reader.lineSequence()
                    .filter { it.isNotBlank() && !it.startsWith("---------") }
                    .take(maxLines)
                    .map(DiagnosticRedactor::redact)
                    .toList()
            }
            process.waitFor()
            lines
        }.getOrDefault(emptyList())
    }

    fun getDebugInfo(): String {
        return """
            App ID: ${BuildConfig.APPLICATION_ID}
            App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}, ${BuildConfig.COMMIT_SHA}, ${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TIME})
            Preview build: $syDebugVersion
            Upstream version: ${BuildConfig.UPSTREAM_VERSION}
            Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}; build ${Build.DISPLAY})
            Device brand: ${Build.BRAND}
            Device manufacturer: ${Build.MANUFACTURER}
            Device name: ${Build.DEVICE} (${Build.PRODUCT})
            Device model: ${Build.MODEL}
            WebView: ${WebViewUtil.getVersion(context)}
            Current time: ${OffsetDateTime.now(ZoneId.systemDefault())}
        """.trimIndent()
    }

    private suspend fun getExtensionsInfo(): String? {
        val availableExtensions = extensionManager.availableExtensionsFlow.value.associateBy { it.pkgName }

        val extensionInfoList = extensionManager.installedExtensionsFlow.first()
            .sortedBy { it.name }
            .mapNotNull {
                val availableExtension = availableExtensions[it.pkgName]
                val hasUpdate = (availableExtension?.versionCode ?: 0) > it.versionCode

                if (!hasUpdate && !it.isObsolete) return@mapNotNull null

                """
                    - ${it.name}
                      Installed: ${it.versionName} / Available: ${availableExtension?.versionName ?: "?"}
                      Obsolete: ${it.isObsolete}
                """.trimIndent()
            }

        return if (extensionInfoList.isNotEmpty()) {
            (listOf("Problematic extensions:") + extensionInfoList)
                .joinToString("\n")
        } else {
            null
        }
    }
}
