package com.shinku.reader.ui.browse.extension.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.presentation.browse.ExtensionDetailsScreen
import com.shinku.reader.presentation.browse.components.PrivateExtensionUninstallDialog
import com.shinku.reader.presentation.core.screens.LoadingScreen
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.util.system.isPackageInstalled
import kotlinx.coroutines.flow.collectLatest

data class ExtensionDetailsScreen(
    private val pkgName: String,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val screenModel = rememberScreenModel { ExtensionDetailsScreenModel(pkgName = pkgName, context = context) }
        val state by screenModel.state.collectAsState()
        var showUninstallDialog by remember { mutableStateOf(false) }

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        val navigator = LocalNavigator.currentOrThrow

        ExtensionDetailsScreen(
            navigateUp = navigator::pop,
            state = state,
            onClickSourcePreferences = { navigator.push(SourcePreferencesScreen(it)) },
            onClickEnableAll = { screenModel.toggleSources(true) },
            onClickDisableAll = { screenModel.toggleSources(false) },
            onClickClearCookies = screenModel::clearCookies,
            onClickUninstall = {
                val extension = state.extension
                if (extension != null && !context.isPackageInstalled(extension.pkgName)) {
                    showUninstallDialog = true
                } else {
                    screenModel.uninstallExtension()
                }
            },
            onClickSource = screenModel::toggleSource,
            onClickIncognito = screenModel::toggleIncognito,
        )

        val extension = state.extension
        if (showUninstallDialog && extension != null) {
            PrivateExtensionUninstallDialog(
                extension = extension,
                onConfirm = screenModel::uninstallExtension,
                onDismissRequest = { showUninstallDialog = false },
            )
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                if (event is ExtensionDetailsEvent.Uninstalled) {
                    navigator.pop()
                }
            }
        }
    }
}
