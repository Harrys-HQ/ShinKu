package com.shinku.reader.presentation.reader.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.source.interactor.TranslationGlossary
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun TranslationGlossaryDialog(
    glossary: TranslationGlossary,
    onSave: (TranslationGlossary) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val entries = remember(glossary) { mutableStateListOf<TranslationGlossary.Entry>().apply { addAll(glossary.entries) } }
    var preserveHonorifics by remember(glossary) { mutableStateOf(glossary.preserveHonorifics) }
    val canSave = entries.none { it.source.isBlank() != it.preferred.isBlank() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.reader_translation_glossary)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(MR.strings.reader_translation_glossary_honorifics), Modifier.weight(1f))
                    Switch(checked = preserveHonorifics, onCheckedChange = { preserveHonorifics = it })
                }
                entries.forEachIndexed { index, entry ->
                    OutlinedTextField(
                        value = entry.source,
                        onValueChange = { entries[index] = entry.copy(source = it) },
                        label = { Text(stringResource(MR.strings.reader_translation_glossary_original)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = entry.preferred,
                        onValueChange = { entries[index] = entry.copy(preferred = it) },
                        label = { Text(stringResource(MR.strings.reader_translation_glossary_preferred)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { entries.removeAt(index) }) { Text(stringResource(MR.strings.action_remove)) }
                }
                TextButton(onClick = { entries.add(TranslationGlossary.Entry()) }) {
                    Text(stringResource(MR.strings.reader_translation_glossary_add))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = { onSave(TranslationGlossary(entries.toList(), preserveHonorifics).normalized()) }) {
                Text(stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
