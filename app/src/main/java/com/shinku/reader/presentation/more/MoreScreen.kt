package com.shinku.reader.presentation.more

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Dangerous
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import com.shinku.reader.presentation.more.components.ReadingJourneyCard
import com.shinku.reader.presentation.more.settings.widget.PreferenceGroupHeader
import com.shinku.reader.presentation.more.settings.widget.SwitchPreferenceWidget
import com.shinku.reader.presentation.more.settings.widget.TextPreferenceWidget
import com.shinku.reader.R
import com.shinku.reader.ui.more.DownloadQueueState
import com.shinku.reader.core.common.Constants
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
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
    onClickSmartCategorizer: () -> Unit,
    onClickDeadSourceScanner: () -> Unit,
    onClickFailedUpdatesMigration: () -> Unit,
    onClickConfigureFeatures: () -> Unit,
    onClickSourceHealth: () -> Unit,
    onClickStats: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
    onClickUpdates: () -> Unit,
    onClickHistory: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.padding(contentPadding),
        ) {
            item {
                LogoHeader()
            }
            item {
                ReadingJourneyCard(
                    readChapters = readChapters,
                    readDuration = readDuration,
                    readStreak = readStreak,
                    onClick = onClickStats,
                )
            }
            item {
                SwitchPreferenceWidget(
                    title = stringResource(MR.strings.label_downloaded_only),
                    subtitle = stringResource(MR.strings.downloaded_only_summary),
                    icon = Icons.Outlined.CloudOff,
                    checked = downloadedOnly,
                    onCheckedChanged = onDownloadedOnlyChange,
                )
            }
            item {
                SwitchPreferenceWidget(
                    title = stringResource(MR.strings.pref_incognito_mode),
                    subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
                    icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
                    checked = incognitoMode,
                    onCheckedChanged = onIncognitoModeChange,
                )
            }

            item { HorizontalDivider() }

            // SY -->
            if (!showNavUpdates) {
                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_recent_updates),
                        icon = Icons.Outlined.NewReleases,
                        onPreferenceClick = onClickUpdates,
                    )
                }
            }
            if (!showNavHistory) {
                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_recent_manga),
                        icon = Icons.Outlined.History,
                        onPreferenceClick = onClickHistory,
                    )
                }
            }
            // SY <--

            item {
                PreferenceGroupHeader(title = stringResource(MR.strings.label_shinku_features))
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.pref_smart_categorizer),
                    subtitle = stringResource(MR.strings.pref_smart_categorizer_summary),
                    icon = Icons.Outlined.AutoAwesome,
                    onPreferenceClick = onClickSmartCategorizer,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.dead_source_scanner_title),
                    subtitle = stringResource(MR.strings.dead_source_scanner_summary),
                    icon = Icons.Outlined.Dangerous,
                    onPreferenceClick = onClickDeadSourceScanner,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.failed_updates_migration_title),
                    subtitle = stringResource(MR.strings.failed_updates_migration_summary),
                    icon = Icons.Outlined.ErrorOutline,
                    onPreferenceClick = onClickFailedUpdatesMigration,
                )
            }
            item {
                TextPreferenceWidget(
                    title = "Source Health",
                    subtitle = "Monitor and rank source stability",
                    icon = Icons.Outlined.HealthAndSafety,
                    onPreferenceClick = onClickSourceHealth,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.action_configure_features),
                    icon = Icons.Outlined.AutoAwesome,
                    onPreferenceClick = onClickConfigureFeatures,
                )
            }

            item { HorizontalDivider() }

            item {
                val downloadQueueState = downloadQueueStateProvider()
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_download_queue),
                    subtitle = when (downloadQueueState) {
                        DownloadQueueState.Stopped -> null
                        is DownloadQueueState.Paused -> {
                            val pending = downloadQueueState.pending
                            if (pending == 0) {
                                stringResource(MR.strings.paused)
                            } else {
                                "${stringResource(MR.strings.paused)} • ${
                                    pluralStringResource(
                                        MR.plurals.download_queue_summary,
                                        count = pending,
                                        pending,
                                    )
                                }"
                            }
                        }
                        is DownloadQueueState.Downloading -> {
                            val pending = downloadQueueState.pending
                            pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
                        }
                    },
                    icon = Icons.Outlined.GetApp,
                    onPreferenceClick = onClickDownloadQueue,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_data_storage),
                    icon = Icons.Outlined.Storage,
                    onPreferenceClick = onClickDataAndStorage,
                )
            }

            item { HorizontalDivider() }

            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_settings),
                    icon = Icons.Outlined.Settings,
                    onPreferenceClick = onClickSettings,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.pref_category_about),
                    icon = Icons.Outlined.Info,
                    onPreferenceClick = onClickAbout,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_help),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onPreferenceClick = { uriHandler.openUri(Constants.URL_HELP) },
                )
            }
        }
    }
}
