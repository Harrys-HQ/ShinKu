package com.shinku.reader.ui.library

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.feature.migration.config.MigrationConfigScreen
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.library.management.LibraryManagementContent
import com.shinku.reader.presentation.library.management.LibraryManagementTab
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.ui.manga.MangaScreen

class LibraryManagementScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { LibraryManagementScreenModel() }
        val state by screenModel.state.collectAsState()
        var selectedTab by rememberSaveable { mutableStateOf(LibraryManagementTab.DUPLICATES) }

        LibraryManagementContent(
            state = state,
            selectedTab = selectedTab,
            onSelectTab = { selectedTab = it },
            onNavigateUp = navigator::pop,
            getSourceName = screenModel::getSourceName,
            onOpenManga = { navigator.push(MangaScreen(it.id)) },
            onGroupAll = screenModel::groupDuplicates,
            onIgnoreDuplicateGroup = screenModel::ignoreDuplicateGroup,
            onRequestRemoveMember = screenModel::requestRemoveFromMergeGroup,
            onRequestResetIgnored = screenModel::requestResetIgnored,
            onMigrateManga = { navigator.push(MigrationConfigScreen(listOf(it.id))) },
            onMigrateAll = { entries -> navigator.push(MigrationConfigScreen(entries.map { it.id })) },
        )

        when (val dialog = state.dialog) {
            is LibraryManagementScreenModel.Dialog.ConfirmRemoveMember -> {
                AlertDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    title = {
                        Text(text = stringResource(MR.strings.library_management_remove_member_title))
                    },
                    text = {
                        Text(
                            text = stringResource(
                                MR.strings.library_management_remove_member_message,
                                dialog.manga.manga.title,
                            ),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { screenModel.confirmRemoveFromMergeGroup(dialog.manga.id) }) {
                            Text(text = stringResource(MR.strings.action_remove))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = screenModel::dismissDialog) {
                            Text(text = stringResource(MR.strings.action_cancel))
                        }
                    },
                )
            }
            is LibraryManagementScreenModel.Dialog.ConfirmResetIgnored -> {
                AlertDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    title = {
                        Text(text = stringResource(MR.strings.library_management_restore_ignored_title))
                    },
                    text = {
                        Text(
                            text = stringResource(
                                MR.strings.library_management_restore_ignored_message,
                                dialog.count,
                            ),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = screenModel::confirmResetIgnored) {
                            Text(text = stringResource(MR.strings.action_reset))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = screenModel::dismissDialog) {
                            Text(text = stringResource(MR.strings.action_cancel))
                        }
                    },
                )
            }
            null -> Unit
        }
    }
}
