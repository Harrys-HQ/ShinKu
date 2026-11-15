package com.shinku.reader.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.LabeledCheckbox
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
fun DeleteLibraryMangaDialog(
    containsLocalManga: Boolean,
    selectedCount: Int = 1,
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit,
) {
    var removeFromLibrary by rememberSaveable { mutableStateOf(false) }
    var deleteDownloads by rememberSaveable(containsLocalManga) { mutableStateOf(false) }
    val deleteFiles = deleteDownloads && !containsLocalManga
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = SanctuaryShapes.hero,
        title = { Text(stringResource(MR.strings.remove_content_heading)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(MR.strings.remove_content_count, selectedCount))
                Text(stringResource(MR.strings.remove_content_scope))
                LabeledCheckbox(
                    label = stringResource(MR.strings.manga_from_library), checked = removeFromLibrary,
                    onCheckedChange = { removeFromLibrary = it },
                )
                if (!containsLocalManga) {
                    LabeledCheckbox(
                        label = stringResource(MR.strings.downloaded_chapters), checked = deleteFiles,
                        onCheckedChange = { deleteDownloads = it },
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) } },
        confirmButton = {
            TextButton(
                enabled = removeFromLibrary || deleteFiles,
                onClick = {
                    onDismissRequest()
                    onConfirm(removeFromLibrary, deleteFiles)
                },
            ) {
                Text(
                    stringResource(
                        when {
                            removeFromLibrary && deleteFiles -> MR.strings.remove_content_both
                            deleteFiles -> MR.strings.remove_content_downloads
                            else -> MR.strings.action_remove
                        },
                    ),
                    color = if (removeFromLibrary || deleteFiles) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        },
    )
}
