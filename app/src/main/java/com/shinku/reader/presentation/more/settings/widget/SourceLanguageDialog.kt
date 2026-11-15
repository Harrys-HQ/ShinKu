package com.shinku.reader.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.LabeledCheckbox
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun SourceLanguageDialog(
    entries: Map<String, String>,
    selected: Set<String>,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(selected.toSet()) }
    val filtered = entries.filter { (key, label) -> label.contains(query, ignoreCase = true) || key.contains(query, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SanctuaryShapes.hero,
        title = { Text(stringResource(MR.strings.source_language_preferences)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(MR.strings.source_language_scope), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(MR.strings.source_language_search)) },
                    singleLine = true,
                    shape = SanctuaryShapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(filtered.toList(), key = { it.first }) { (key, label) ->
                        LabeledCheckbox(
                            label = label,
                            checked = key in draft,
                            onCheckedChange = { checked -> draft = if (checked) draft + key else draft - key },
                        )
                    }
                    if (filtered.isEmpty()) item { Text(stringResource(MR.strings.no_results_found)) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(draft)
                onDismiss()
            }, shape = SanctuaryShapes.medium, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(MR.strings.source_language_apply, draft.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
