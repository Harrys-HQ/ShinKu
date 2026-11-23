package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.shinku.reader.extension.model.Extension
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource

/**
 * Confirms uninstalling a privately installed extension, laid out like the system package installer's prompt.
 */
@Composable
fun PrivateExtensionUninstallDialog(
    extension: Extension,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(text = stringResource(MR.strings.ext_confirm_uninstall))
        },
        text = {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExtensionIcon(
                    extension = extension,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = extension.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.ext_uninstall))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
