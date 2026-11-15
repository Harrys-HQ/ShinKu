package com.shinku.reader.presentation.reader.dialog

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.ui.reader.model.TranslatedBlock

@Composable
fun BubbleInspectorDialog(
    pageNumber: Int,
    targetLanguage: String,
    blocks: List<TranslatedBlock>,
    onDismissRequest: () -> Unit,
    onRetranslatePage: () -> Unit,
) {
    var selectedIndex by rememberSaveable(pageNumber, targetLanguage) { mutableIntStateOf(0) }
    val index = selectedIndex.coerceIn(0, blocks.lastIndex.coerceAtLeast(0))
    val context = LocalContext.current
    fun copy(text: String) {
        context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText("ShinKu", text))
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = SanctuaryShapes.hero,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.card),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(MR.strings.reader_inspect_bubble), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(MR.strings.reader_inspect_page, pageNumber, targetLanguage), style = MaterialTheme.typography.bodySmall)
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (blocks.isEmpty()) {
                    Text(stringResource(MR.strings.reader_inspect_empty))
                } else {
                    val block = blocks[index]
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            enabled = index > 0,
                            onClick = { selectedIndex = index - 1 },
                            shape = SanctuaryShapes.medium,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(stringResource(MR.strings.reader_previous_bubble)) }
                        OutlinedButton(
                            enabled = index < blocks.lastIndex,
                            onClick = { selectedIndex = index + 1 },
                            shape = SanctuaryShapes.medium,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(stringResource(MR.strings.reader_next_bubble)) }
                    }
                    Text(stringResource(MR.strings.reader_inspect_position, index + 1, blocks.size), style = MaterialTheme.typography.labelMedium)
                    BubbleTextCard(stringResource(MR.strings.translation_original), block.originalText, ::copy)
                    BubbleTextCard(stringResource(MR.strings.translation_result), block.translatedText, ::copy)
                    TextButton(onClick = { copy("${block.originalText}\n${block.translatedText}") }) {
                        Text(stringResource(MR.strings.reader_inspect_copy_both))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissRequest()
                onRetranslatePage()
            }) {
                Text(stringResource(MR.strings.reader_inspect_retry))
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_close)) } },
    )
}

@Composable
private fun BubbleTextCard(label: String, text: String, onCopy: (String) -> Unit) {
    val copyLabel = stringResource(MR.strings.reader_inspect_copy, label)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { onCopy(text) }) { Icon(Icons.Outlined.ContentCopy, contentDescription = copyLabel) }
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
