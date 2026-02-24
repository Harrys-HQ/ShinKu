package com.shinku.reader.data.updater

import android.content.Context
import com.shinku.reader.BuildConfig
import com.shinku.reader.util.system.isPreviewBuildType
import com.shinku.reader.exh.syDebugVersion
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.release.interactor.GetApplicationRelease
import uy.kohesive.injekt.injectLazy

class AppUpdateChecker {

    private val getApplicationRelease: GetApplicationRelease by injectLazy()

    suspend fun checkForUpdate(context: Context, forceCheck: Boolean = false): GetApplicationRelease.Result {
        // Disable app update checks for older Android versions that we're going to drop support for
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        //     return GetApplicationRelease.Result.OsTooOld
        // }

        return withIOContext {
            val result = getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    // SY -->
                    isPreviewBuildType,
                    // SY <--
                    BuildConfig.COMMIT_COUNT.toInt(),
                    BuildConfig.VERSION_NAME,
                    GITHUB_REPO,
                    // SY -->
                    syDebugVersion,
                    // SY <--
                    forceCheck,
                ),
            )

            when (result) {
                is GetApplicationRelease.Result.NewUpdate -> AppUpdateNotifier(context).promptUpdate(result.release)
                else -> {}
            }

            result
        }
    }
}

val GITHUB_REPO: String by lazy {
    // SY -->
    if (isPreviewBuildType) {
        "Harrys-HQ/ShinKu"
    } else {
        "Harrys-HQ/ShinKu"
    }
    // SY <--
}
