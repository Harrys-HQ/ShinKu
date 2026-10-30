package com.shinku.reader.presentation.reader.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.util.system.copyToClipboard

@Composable
fun ReaderAiProcessingDialog(
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = SanctuaryShapes.hero,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.card),
        confirmButton = {},
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SanctuaryOverlineBadge(
                    text = "SHINKU AI ENGINE",
                    compact = true,
                )
                Text(
                    text = "Analyzing Page Content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
        text = {
            Surface(
                shape = SanctuaryShapes.medium,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                border = SanctuaryBrushes.subtleBorder(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text(
                        text = "Running script-aware OCR & neural synthesis...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
fun ReaderAiInsightDialog(
    title: String,
    content: String,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = SanctuaryShapes.hero,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.card),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SanctuaryOverlineBadge(
                    text = "SHINKU SANCTUARY AI",
                    compact = true,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        text = {
            Surface(
                shape = SanctuaryShapes.card,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                border = SanctuaryBrushes.subtleBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { context.copyToClipboard(title, content) },
                shape = SanctuaryShapes.medium,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy", maxLines = 1, softWrap = false)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                shape = SanctuaryShapes.medium,
            ) {
                Text(stringResource(MR.strings.action_ok), maxLines = 1, softWrap = false)
            }
        },
    )
}

@Composable
fun ReaderTranslationLanguageDialog(
    currentSourceLanguage: String,
    currentTargetLanguage: String,
    onConfirm: (source: String, target: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var selectedSource by remember { mutableStateOf(currentSourceLanguage) }
    var selectedTarget by remember { mutableStateOf(currentTargetLanguage) }

    val sourceLanguages = remember {
        listOf(
            "Auto-Detect" to "Auto-Detect (自动检测)",
            "Japanese" to "Japanese (日本語)",
            "Chinese" to "Chinese (中文)",
            "Korean" to "Korean (한국어)",
            "English" to "English",
            "Spanish" to "Spanish (Español)",
            "French" to "French (Français)",
            "German" to "German (Deutsch)",
            "Portuguese" to "Portuguese (Português)",
            "Italian" to "Italian (Italiano)",
            "Russian" to "Russian (Русский)",
            "Indonesian" to "Indonesian (Bahasa Indonesia)",
            "Vietnamese" to "Vietnamese (Tiếng Việt)",
            "Thai" to "Thai (ภาษาไทย)",
        )
    }

    val targetLanguages = remember {
        listOf(
            "English" to "English",
            "Japanese" to "Japanese (日本語)",
            "Chinese (Simplified)" to "Chinese Simplified (简体中文)",
            "Chinese (Traditional)" to "Chinese Traditional (繁體中文)",
            "Korean" to "Korean (한국어)",
            "Spanish" to "Spanish (Español)",
            "French" to "French (Français)",
            "German" to "German (Deutsch)",
            "Portuguese" to "Portuguese (Português)",
            "Italian" to "Italian (Italiano)",
            "Russian" to "Russian (Русский)",
            "Indonesian" to "Indonesian (Bahasa Indonesia)",
            "Vietnamese" to "Vietnamese (Tiếng Việt)",
            "Thai" to "Thai (ภาษาไทย)",
            "Tagalog" to "Tagalog (Filipino)",
            "Arabic" to "Arabic (العربية)",
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = SanctuaryShapes.hero,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.card),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SanctuaryOverlineBadge(
                    text = "LIVE TRANSLATION MATRIX",
                    compact = true,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "Language Pair",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LanguageDropdownField(
                    label = "Translate From (OCR Script)",
                    selectedCode = selectedSource,
                    options = sourceLanguages,
                    onSelect = { selectedSource = it },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(
                        onClick = {
                            val currentSrc = selectedSource
                            if (currentSrc != "Auto-Detect") {
                                val normalizedTarget = if (selectedTarget.startsWith("Chinese")) "Chinese" else selectedTarget
                                val normalizedSourceForTarget = if (currentSrc == "Chinese") "Chinese (Simplified)" else currentSrc
                                if (sourceLanguages.any { it.first == normalizedTarget } &&
                                    targetLanguages.any { it.first == normalizedSourceForTarget }
                                ) {
                                    selectedSource = normalizedTarget
                                    selectedTarget = normalizedSourceForTarget
                                }
                            }
                        },
                        enabled = selectedSource != "Auto-Detect",
                        shape = SanctuaryShapes.pill,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Swap Languages", maxLines = 1, softWrap = false)
                    }
                }

                LanguageDropdownField(
                    label = "Translate To (Typeset Output)",
                    selectedCode = selectedTarget,
                    options = targetLanguages,
                    onSelect = { selectedTarget = it },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel), maxLines = 1, softWrap = false)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedSource, selectedTarget)
                    onDismissRequest()
                },
                shape = SanctuaryShapes.medium,
            ) {
                Text(stringResource(MR.strings.action_ok), maxLines = 1, softWrap = false)
            }
        },
    )
}

@Composable
private fun LanguageDropdownField(
    label: String,
    selectedCode: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = options.find { it.first == selectedCode }?.second ?: selectedCode

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SanctuaryShapes.medium)
                    .clickable { expanded = true },
                shape = SanctuaryShapes.medium,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelect(code)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
