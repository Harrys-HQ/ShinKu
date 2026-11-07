package com.shinku.reader.presentation.reader.dialog

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.shinku.reader.R
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.ui.reader.model.TranslatedBlock
import com.shinku.reader.util.system.toast
import kotlinx.coroutines.launch

@Composable
fun BubbleInspectorDialog(
    pageNumber: Int,
    targetLanguage: String,
    blocks: List<TranslatedBlock>,
    onDismissRequest: () -> Unit,
    onRetranslatePage: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = SanctuaryShapes.hero,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.card),
        tonalElevation = SanctuaryElevation.card,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = SanctuaryShapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_shinku_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(SanctuaryShapes.tag),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inspect bubbles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Page $pageNumber · ${blocks.size} detected · ${targetLanguage.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            if (blocks.isEmpty()) {
                Text(
                    text = "No translated speech bubbles on this page yet. Tap 'Re-Translate Page' below to run fresh OCR and translation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(blocks) { index, block ->
                        Surface(
                            shape = SanctuaryShapes.medium,
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                            border = SanctuaryBrushes.subtleBorder(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SanctuaryOverlineBadge(
                                        text = "BUBBLE #${index + 1} • ${targetLanguage.uppercase()}",
                                        compact = true,
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService<ClipboardManager>()
                                            val clipText = "${block.originalText}\n${block.translatedText}"
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("ShinKu Bubble", clipText))
                                            context.toast("Copied original & translated text")
                                        },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy text",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(stringResource(MR.strings.translation_original), style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = block.originalText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(stringResource(MR.strings.translation_result), style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = block.translatedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onRetranslatePage()
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Re-Translate Page", maxLines = 1, softWrap = false)
            }
        },
        confirmButton = {
            Column {
                if (blocks.size > 1) {
                    Row {
                        TextButton(
                            enabled = listState.firstVisibleItemIndex > 0,
                            onClick = { scope.launch { listState.animateScrollToItem((listState.firstVisibleItemIndex - 1).coerceAtLeast(0)) } },
                        ) { Text(stringResource(MR.strings.reader_previous_bubble)) }
                        TextButton(
                            enabled = listState.firstVisibleItemIndex < blocks.lastIndex,
                            onClick = { scope.launch { listState.animateScrollToItem((listState.firstVisibleItemIndex + 1).coerceAtMost(blocks.lastIndex)) } },
                        ) { Text(stringResource(MR.strings.reader_next_bubble)) }
                    }
                }
                TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_close)) }
            }
        },
    )
}
