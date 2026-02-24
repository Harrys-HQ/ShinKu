package com.shinku.reader.feature.migration.list.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun MigrationExitDialog(
    onDismissRequest: () -> Unit,
    exitMigration: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.migrationListScreen_exitDialogTitle))
        },
        confirmButton = {
            TextButton(onClick = exitMigration) {
                Text(text = stringResource(MR.strings.migrationListScreen_exitDialog_stopLabel))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.migrationListScreen_exitDialog_cancelLabel))
            }
        },
    )
}
