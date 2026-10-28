package com.shinku.reader.presentation.more.settings.screen.about

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.BuildConfig
import com.shinku.reader.R
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.lang.withUIContext
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.data.updater.AppUpdateChecker
import com.shinku.reader.domain.release.interactor.GetApplicationRelease
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.exh.syDebugVersion
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.core.components.LinkIcon
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.icons.CustomIcons
import com.shinku.reader.presentation.core.icons.Discord
import com.shinku.reader.presentation.core.icons.Github
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.sanctuaryShadow
import com.shinku.reader.presentation.more.components.CommandGroup
import com.shinku.reader.presentation.more.components.CommandRowItem
import com.shinku.reader.presentation.util.LocalBackPress
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.ui.more.NewUpdateScreen
import com.shinku.reader.util.CrashLogUtil
import com.shinku.reader.util.lang.toDateTimestampString
import com.shinku.reader.util.system.copyToClipboard
import com.shinku.reader.util.system.isPreviewBuildType
import com.shinku.reader.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
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
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MaterialTheme.padding.medium,
                                vertical = MaterialTheme.padding.small,
                            )
                            .sanctuaryShadow(
                                elevation = SanctuaryElevation.hero,
                                shape = SanctuaryShapes.hero,
                            ),
                        shape = SanctuaryShapes.hero,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SanctuaryBrushes.heroVerticalGradient())
                                .clickable {
                                    val deviceInfo = CrashLogUtil(context).getDebugInfo()
                                    context.copyToClipboard("Debug information", deviceInfo)
                                }
                                .padding(20.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    modifier = Modifier.size(60.dp),
                                    shape = SanctuaryShapes.deckCard,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    ),
                                    shadowElevation = SanctuaryElevation.compact,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_shinku_logo),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(40.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    SanctuaryOverlineBadge(
                                        text = "SHINKU SANCTUARY ENGINE",
                                        compact = true,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(MR.strings.app_name),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = getVersionName(withBuildDate = true),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    CommandGroup(title = "Release & Updates") {
                        if (BuildConfig.INCLUDE_UPDATER) {
                            CommandRowItem(
                                title = stringResource(MR.strings.check_for_updates),
                                subtitle = if (isCheckingUpdates) "Checking for updates..." else "Verify latest ShinKu release",
                                icon = Icons.Outlined.SystemUpdate,
                                onClick = {
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

                        CommandRowItem(
                            title = stringResource(MR.strings.whats_new),
                            subtitle = "View recent release highlights and changelog",
                            icon = Icons.Outlined.AutoAwesome,
                            onClick = { showWhatsNewDialog = true },
                        )
                    }
                }

                item {
                    CommandGroup(title = "Resources, Support & Legal") {
                        CommandRowItem(
                            title = stringResource(MR.strings.label_help),
                            subtitle = "Guides, FAQ, and troubleshooting",
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            onClick = { uriHandler.openUri("https://harrys-hq.github.io/ShinKu/guides/getting-started") },
                        )
                        CommandRowItem(
                            title = "Support development",
                            subtitle = "Support ShinKu on Ko-fi",
                            icon = Icons.Outlined.FavoriteBorder,
                            onClick = { uriHandler.openUri("https://ko-fi.com/B0B21RXKG4") },
                        )
                        CommandRowItem(
                            title = stringResource(MR.strings.licenses),
                            subtitle = "Open-source libraries and attributions",
                            icon = Icons.Outlined.Code,
                            onClick = { navigator.push(OpenSourceLicensesScreen()) },
                        )
                        CommandRowItem(
                            title = stringResource(MR.strings.privacy_policy),
                            subtitle = "Data handling and privacy commitments",
                            icon = Icons.Outlined.Policy,
                            onClick = { uriHandler.openUri("https://harrys-hq.github.io/ShinKu/legal/privacy") },
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
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
                            url = "https://github.com/Harrys-HQ/ShinKu",
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
