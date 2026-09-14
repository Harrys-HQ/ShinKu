package com.shinku.reader.presentation.more

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import com.shinku.reader.presentation.more.components.ReadingJourneyCard
import com.shinku.reader.presentation.more.settings.widget.SwitchPreferenceWidget
import com.shinku.reader.presentation.more.settings.widget.TextPreferenceWidget
import com.shinku.reader.R
import com.shinku.reader.ui.more.DownloadQueueState
import com.shinku.reader.core.common.Constants
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.pluralStringResource
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    // SY -->
    showNavUpdates: Boolean,
    showNavHistory: Boolean,
    readChapters: Int,
    readDuration: Long,
    readStreak: Int,
    // SY <--
    onClickDownloadQueue: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickConfigureFeatures: () -> Unit,
    onClickStats: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
    onClickUpdates: () -> Unit,
    onClickHistory: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val downloadQueueState = downloadQueueStateProvider()
    val downloadSummary = when (downloadQueueState) {
        DownloadQueueState.Stopped -> null
        is DownloadQueueState.Paused -> {
            val pending = downloadQueueState.pending
            if (pending == 0) {
                stringResource(MR.strings.paused)
            } else {
                "${stringResource(MR.strings.paused)} (${pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)})"
            }
        }
        is DownloadQueueState.Downloading -> {
            val pending = downloadQueueState.pending
            pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
        }
    }

    Scaffold { contentPadding ->
        ScrollbarLazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
            ),
        ) {
            // 1. Reading Identity & Stats Header
            item {
                com.shinku.reader.presentation.more.components.CommandProfileHeader(
                    readChapters = readChapters,
                    readDuration = readDuration,
                    readStreak = readStreak,
                    onClick = onClickStats,
                )
            }

            // 2. Quick Command Grid (Offline, Incognito, Queue, Storage)
            item {
                com.shinku.reader.presentation.more.components.QuickCommandGrid(
                    downloadedOnly = downloadedOnly,
                    onDownloadedOnlyChange = onDownloadedOnlyChange,
                    incognitoMode = incognitoMode,
                    onIncognitoModeChange = onIncognitoModeChange,
                    downloadQueueSummary = downloadSummary,
                    onClickDownloadQueue = onClickDownloadQueue,
                    onClickDataAndStorage = onClickDataAndStorage,
                )
            }

            // 3. ShinKu Engine
            item {
                com.shinku.reader.presentation.more.components.CommandGroup(
                    title = "ShinKu Engine",
                ) {
                    com.shinku.reader.presentation.more.components.CommandRowItem(
                        title = stringResource(MR.strings.action_configure_features),
                        subtitle = "Manage Gemini AI, performance, and advanced features",
                        icon = Icons.Outlined.AutoAwesome,
                        onClick = onClickConfigureFeatures,
                    )
                    if (!showNavUpdates) {
                        com.shinku.reader.presentation.more.components.CommandRowItem(
                            title = stringResource(MR.strings.label_recent_updates),
                            icon = Icons.Outlined.NewReleases,
                            onClick = onClickUpdates,
                        )
                    }
                    if (!showNavHistory) {
                        com.shinku.reader.presentation.more.components.CommandRowItem(
                            title = stringResource(MR.strings.label_recent_manga),
                            icon = Icons.Outlined.History,
                            onClick = onClickHistory,
                        )
                    }
                }
            }

            // 4. Core System & Preferences
            item {
                com.shinku.reader.presentation.more.components.CommandGroup(
                    title = "Preferences & Storage",
                ) {
                    com.shinku.reader.presentation.more.components.CommandRowItem(
                        title = stringResource(MR.strings.label_settings),
                        subtitle = "Reader, library, downloads, and security",
                        icon = Icons.Outlined.Settings,
                        onClick = onClickSettings,
                    )
                    com.shinku.reader.presentation.more.components.CommandRowItem(
                        title = stringResource(MR.strings.label_data_storage),
                        subtitle = "Backups, storage limits, and cache clearing",
                        icon = Icons.Outlined.Storage,
                        onClick = onClickDataAndStorage,
                    )
                }
            }

            // 5. System Info & Help
            item {
                com.shinku.reader.presentation.more.components.CommandGroup(
                    title = "About & Support",
                ) {
                    com.shinku.reader.presentation.more.components.CommandRowItem(
                        title = stringResource(MR.strings.pref_category_about),
                        subtitle = "Version, release notes, and licenses",
                        icon = Icons.Outlined.Info,
                        onClick = onClickAbout,
                    )
                    com.shinku.reader.presentation.more.components.CommandRowItem(
                        title = stringResource(MR.strings.label_help),
                        subtitle = "User guide and website",
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = { uriHandler.openUri(Constants.URL_HELP) },
                    )
                }
            }
        }
    }
}
