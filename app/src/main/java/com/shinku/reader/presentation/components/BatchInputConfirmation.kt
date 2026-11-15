package com.shinku.reader.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun BatchInputConfirmation(input: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, shape = SanctuaryShapes.hero,
        title = { Text(stringResource(MR.strings.batch_add_confirm_title)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(MR.strings.batch_add_confirm_scope))
                Text(input, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(SYMR.strings.eh_batch_add_button)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) } },
    )
}
