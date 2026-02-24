package com.shinku.reader.presentation.more.settings.screen.about

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.more.LogoHeader
import com.shinku.reader.presentation.more.settings.widget.TextPreferenceWidget
import com.shinku.reader.presentation.util.LocalBackPress
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.BuildConfig
import com.shinku.reader.data.updater.AppUpdateChecker
import com.shinku.reader.ui.more.NewUpdateScreen
import com.shinku.reader.util.CrashLogUtil
import com.shinku.reader.util.lang.toDateTimestampString
import com.shinku.reader.util.system.copyToClipboard
import com.shinku.reader.util.system.isPreviewBuildType
import com.shinku.reader.util.system.toast
import com.shinku.reader.exh.syDebugVersion
import kotlinx.coroutines.launch
import logcat.LogPriority
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.lang.withUIContext
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.release.interactor.GetApplicationRelease
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.LinkIcon
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.icons.CustomIcons
import com.shinku.reader.presentation.core.icons.Discord
import com.shinku.reader.presentation.core.icons.Facebook
import com.shinku.reader.presentation.core.icons.Github
import com.shinku.reader.presentation.core.icons.Reddit
import com.shinku.reader.presentation.core.icons.X
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object AboutScreen : Screen() {

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        var isCheckingUpdates by remember { mutableStateOf(false) }

        // SY -->
        var showWhatsNewDialog by remember { mutableStateOf(false) }
        // SY <--

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_category_about),
                    navigateUp = if (handleBack != null) handleBack::invoke else null,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            ScrollbarLazyColumn(
                contentPadding = contentPadding,
            ) {
                item {
                    LogoHeader()
                }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.version),
                        subtitle = getVersionName(withBuildDate = true),
                        onPreferenceClick = {
                            val deviceInfo = CrashLogUtil(context).getDebugInfo()
                            context.copyToClipboard("Debug information", deviceInfo)
                        },
                    )
                }

                if (BuildConfig.INCLUDE_UPDATER) {
                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.check_for_updates),
                            widget = {
                                AnimatedVisibility(visible = isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp,
                                    )
                                }
                            },
                            onPreferenceClick = {
                                if (!isCheckingUpdates) {
                                    scope.launch {
                                        isCheckingUpdates = true

                                        checkVersion(
                                            context = context,
                                            onAvailableUpdate = { result ->
                                                val updateScreen = NewUpdateScreen(
                                                    versionName = result.release.version,
                                                    changelogInfo = result.release.info,
                                                    releaseLink = result.release.releaseLink,
                                                    downloadLink = result.release.getDownloadLink(),
                                                )
                                                navigator.push(updateScreen)
                                            },
                                            onFinish = {
                                                isCheckingUpdates = false
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }
                }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.whats_new),
                        // SY -->
                        onPreferenceClick = { showWhatsNewDialog = true },
                        // SY <--
                    )
                }

                // item {
                //     TextPreferenceWidget(
                //         title = stringResource(MR.strings.help_translate),
                //         onPreferenceClick = { uriHandler.openUri("https://shinku.app/docs/contribute#translation") },
                //     )
                // }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.licenses),
                        onPreferenceClick = { navigator.push(OpenSourceLicensesScreen()) },
                    )
                }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.privacy_policy),
                        onPreferenceClick = { uriHandler.openUri("https://shinku.app/privacy/") },
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        LinkIcon(
                            label = stringResource(MR.strings.website),
                            icon = Icons.Outlined.Public,
                            url = "https://harrys-hq.github.io/ShinKu/",
                        )
                        LinkIcon(
                            label = "Discord",
                            icon = CustomIcons.Discord,
                            url = "https://discord.gg/shinku",
                        )
                        LinkIcon(
                            label = "GitHub",
                            icon = CustomIcons.Github,
                            // SY -->
                            url = "https://github.com/Harrys-HQ/ShinKu",
                            // SY <--
                        )
                    }
                }
            }
        }

        // SY -->
        if (showWhatsNewDialog) {
            WhatsNewDialog(onDismissRequest = { showWhatsNewDialog = false })
        }
        // SY <--
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private suspend fun checkVersion(
        context: Context,
        onAvailableUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
        onFinish: () -> Unit,
    ) {
        val updateChecker = AppUpdateChecker()
        withUIContext {
            try {
                when (val result = withIOContext { updateChecker.checkForUpdate(context, forceCheck = true) }) {
                    is GetApplicationRelease.Result.NewUpdate -> {
                        onAvailableUpdate(result)
                    }
                    is GetApplicationRelease.Result.NoNewUpdate -> {
                        context.toast(MR.strings.update_check_no_new_updates)
                    }
                    is GetApplicationRelease.Result.OsTooOld -> {
                        context.toast(MR.strings.update_check_eol)
                    }
                }
            } catch (e: Exception) {
                context.toast(e.message)
                logcat(LogPriority.ERROR, e)
            } finally {
                onFinish()
            }
        }
    }

    fun getVersionName(withBuildDate: Boolean): String {
        return when {
            BuildConfig.DEBUG -> {
                "Debug ${BuildConfig.COMMIT_SHA}".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime()})"
                    } else {
                        it
                    }
                }
            }
            // SY -->
            isPreviewBuildType -> {
                "ShinKu r$syDebugVersion".let {
                    if (withBuildDate) {
                        "$it (${BuildConfig.COMMIT_SHA}, ${getFormattedBuildTime()})"
                    } else {
                        "$it (${BuildConfig.COMMIT_SHA})"
                    }
                }
            }
            // SY <--
            else -> {
                "Stable ${BuildConfig.VERSION_NAME}".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime()})"
                    } else {
                        it
                    }
                }
            }
        }
    }

    internal fun getFormattedBuildTime(): String {
        return try {
            LocalDateTime.ofInstant(
                Instant.parse(BuildConfig.BUILD_TIME),
                ZoneId.systemDefault(),
            )
                .toDateTimestampString(
                    UiPreferences.dateFormat(
                        Injekt.get<UiPreferences>().dateFormat().get(),
                    ),
                )
        } catch (e: Exception) {
            BuildConfig.BUILD_TIME
        }
    }
}
