package com.shinku.reader.feature.migration.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.core.common.preference.getAndSet
import com.shinku.reader.core.common.preference.toggle
import com.shinku.reader.core.util.fastFilterNot
import com.shinku.reader.domain.migration.models.MigrationFlag
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.feature.common.utils.getLabel
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AdaptiveSheet
import com.shinku.reader.presentation.core.components.material.Button
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.active
import com.shinku.reader.presentation.core.theme.header
import com.shinku.reader.presentation.core.util.collectAsState

@Composable
fun MigrationConfigScreenSheet(
    preferences: SourcePreferences,
    onDismissRequest: () -> Unit,
    onStartMigration: (extraSearchQuery: String?) -> Unit,
) {
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var extraSearchQuery by rememberSaveable { mutableStateOf("") }
    val migrationFlags by preferences.migrationFlags().collectAsState()
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = MaterialTheme.padding.medium, bottom = MaterialTheme.padding.small),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium)) {
                    Text(
                        text = stringResource(MR.strings.migrationConfigScreen_dataToMigrateHeader),
                        style = MaterialTheme.typography.header,
                    )
                    Text(
                        text = "Choose which reading data and library metadata to transfer to the new source.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.padding.medium),
                    shape = SanctuaryShapes.deckCard,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                    border = SanctuaryBrushes.subtleBorder(),
                ) {
                    Column {
                        MigrationFlag.entries
                            .fastFilterNot { it == MigrationFlag.REMOVE_DOWNLOAD }
                            .fastForEach { flag ->
                                val selected = flag in migrationFlags
                                MigrationSheetSwitchItem(
                                    title = stringResource(flag.getLabel()),
                                    subtitle = null,
                                    checked = selected,
                                    onClick = {
                                        preferences.migrationFlags().getAndSet { currentFlags ->
                                            if (flag in currentFlags) {
                                                currentFlags - flag
                                            } else {
                                                currentFlags + flag
                                            }
                                        }
                                    },
                                )
                            }
                        val removeDownloads = MigrationFlag.REMOVE_DOWNLOAD in migrationFlags
                        MigrationSheetSwitchItem(
                            title = stringResource(MR.strings.migrationConfigScreen_removeDownloadsTitle),
                            subtitle = null,
                            checked = removeDownloads,
                            onClick = {
                                preferences.migrationFlags().getAndSet {
                                    if (removeDownloads) {
                                        it - MigrationFlag.REMOVE_DOWNLOAD
                                    } else {
                                        it + MigrationFlag.REMOVE_DOWNLOAD
                                    }
                                }
                            },
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.padding.medium),
                    shape = SanctuaryShapes.deckCard,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                    border = SanctuaryBrushes.subtleBorder(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "When a matching title is found, your reading progress, categories and tracking references will be preserved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.padding.medium)
                        .clip(SanctuaryShapes.deckCard),
                    shape = SanctuaryShapes.deckCard,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                    border = SanctuaryBrushes.subtleBorder(),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvanced = !showAdvanced }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(MR.strings.pref_category_advanced),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Icon(
                                imageVector = if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (showAdvanced) {
                            MigrationSheetDividerItem()
                            OutlinedTextField(
                                value = extraSearchQuery,
                                onValueChange = { extraSearchQuery = it },
                                label = { Text(stringResource(MR.strings.migrationConfigScreen_additionalSearchQueryLabel)) },
                                supportingText = {
                                    Text(stringResource(MR.strings.migrationConfigScreen_additionalSearchQuerySupportingText))
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = MaterialTheme.padding.medium,
                                        vertical = MaterialTheme.padding.extraSmall,
                                    ),
                            )
                            MigrationSheetSwitchItem(
                                title = stringResource(MR.strings.migrationConfigScreen_hideUnmatchedTitle),
                                subtitle = null,
                                preference = preferences.migrationHideUnmatched(),
                            )
                            MigrationSheetSwitchItem(
                                title = stringResource(MR.strings.migrationConfigScreen_hideWithoutUpdatesTitle),
                                subtitle = stringResource(MR.strings.migrationConfigScreen_hideWithoutUpdatesSubtitle),
                                preference = preferences.migrationHideWithoutUpdates(),
                            )
                            MigrationSheetDividerItem()
                            MigrationSheetWarningItem(stringResource(MR.strings.migrationConfigScreen_enhancedOptionsWarning))
                            MigrationSheetSwitchItem(
                                title = stringResource(MR.strings.migrationConfigScreen_deepSearchModeTitle),
                                subtitle = stringResource(MR.strings.migrationConfigScreen_deepSearchModeSubtitle),
                                preference = preferences.migrationDeepSearchMode(),
                            )
                            MigrationSheetSwitchItem(
                                title = stringResource(MR.strings.migrationConfigScreen_prioritizeByChaptersTitle),
                                subtitle = stringResource(MR.strings.migrationConfigScreen_prioritizeByChaptersSubtitle),
                                preference = preferences.migrationPrioritizeByChapters(),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            HorizontalDivider()
            Button(
                onClick = {
                    val cleanedExtraSearchQuery = extraSearchQuery.trim().ifBlank { null }
                    onStartMigration(cleanedExtraSearchQuery)
                },
                shape = SanctuaryShapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.padding.medium,
                        vertical = MaterialTheme.padding.small,
                    ),
            ) {
                Text(text = stringResource(MR.strings.migrationConfigScreen_continueButtonText))
            }
        }
    }
}

@Composable
private fun MigrationSheetSwitchItem(
    title: String,
    subtitle: String?,
    preference: Preference<Boolean>,
) {
    MigrationSheetSwitchItem(
        title = title,
        subtitle = subtitle,
        checked = preference.collectAsState().value,
        onClick = { preference.toggle() },
    )
}

@Composable
private fun MigrationSheetSwitchItem(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = subtitle?.let { { Text(text = subtitle) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun MigrationSheetDividerItem() {
    HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.padding.extraSmall))
}

@Composable
private fun MigrationSheetWarningItem(
    text: String,
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.active,
            )
        },
        headlineContent = {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
