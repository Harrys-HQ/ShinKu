package com.shinku.reader.ui.browse.extension

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.presentation.browse.ExtensionScreen
import com.shinku.reader.presentation.browse.components.PrivateExtensionUninstallDialog
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.TabContent
import com.shinku.reader.presentation.more.settings.screen.browse.ExtensionReposScreen
import com.shinku.reader.extension.model.Extension
import com.shinku.reader.ui.browse.extension.details.ExtensionDetailsScreen
import com.shinku.reader.ui.webview.WebViewScreen
import com.shinku.reader.util.system.isPackageInstalled
import kotlinx.collections.immutable.persistentListOf
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun extensionsTab(
    extensionsScreenModel: ExtensionsScreenModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current

    val state by extensionsScreenModel.state.collectAsState()
    var privateExtensionToUninstall by remember { mutableStateOf<Extension?>(null) }

    return TabContent(
        titleRes = MR.strings.label_extensions,
        badgeNumber = state.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = Icons.Outlined.FilterList,
                onClick = { navigator.push(ExtensionFilterScreen()) },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.label_extension_repos),
                icon = Icons.Outlined.Settings,
                onClick = { navigator.push(ExtensionReposScreen()) },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.action_select_items),
                icon = Icons.Outlined.SelectAll,
                enabled = !state.batchRunning,
                onClick = extensionsScreenModel::enterSelectionMode,
            ),
        ),
        content = { contentPadding, _ ->
            BackHandler(enabled = state.isSelecting || state.batchRunning) {
                if (state.batchRunning) extensionsScreenModel.cancelBatch() else extensionsScreenModel.exitSelectionMode()
            }
            ExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = { extension ->
                    when (extension) {
                        is Extension.Available -> extensionsScreenModel.toggleSelection(extension)
                        else -> {
                            if (extension.canBatchInstall()) {
                                extensionsScreenModel.toggleSelection(extension)
                            } else if (context.isPackageInstalled(extension.pkgName)) {
                                extensionsScreenModel.uninstallExtension(extension)
                            } else {
                                privateExtensionToUninstall = extension
                            }
                        }
                    }
                },
                onClickItemCancel = extensionsScreenModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsScreenModel::updateAllExtensions,
                onToggleSelection = extensionsScreenModel::toggleSelection,
                onInstallSelected = extensionsScreenModel::installSelected,
                onExitSelection = extensionsScreenModel::exitSelectionMode,
                onCancelBatch = extensionsScreenModel::cancelBatch,
                onOpenWebView = { extension ->
                    extension.sources.getOrNull(0)?.let {
                        navigator.push(
                            WebViewScreen(
                                url = it.baseUrl,
                                initialTitle = it.name,
                                sourceId = it.id,
                            ),
                        )
                    }
                },
                onInstallExtension = extensionsScreenModel::installExtension,
                onOpenExtension = { navigator.push(ExtensionDetailsScreen(it.pkgName)) },
                onTrustExtension = { extensionsScreenModel.trustExtension(it) },
                onUninstallExtension = { extensionsScreenModel.uninstallExtension(it) },
                onUpdateExtension = extensionsScreenModel::updateExtension,
                onRefresh = extensionsScreenModel::findAvailableExtensions,
            )

            privateExtensionToUninstall?.let { extension ->
                PrivateExtensionUninstallDialog(
                    extension = extension,
                    onConfirm = {
                        extensionsScreenModel.uninstallExtension(extension)
                    },
                    onDismissRequest = {
                        privateExtensionToUninstall = null
                    },
                )
            }
        },
    )
}
