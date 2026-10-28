package com.shinku.reader.presentation.browse.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.browse.FeedItemUI
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
fun FeedFilterDialog(
    currentSourceFilter: String,
    currentLanguageFilter: String,
    currentShowSourceFeeds: Boolean,
    customFeeds: List<FeedItemUI>?,
    enabledLanguages: Set<String>,
    onDismiss: () -> Unit,
    onApply: (sourceFilter: String, languageFilter: String, showSourceFeeds: Boolean) -> Unit,
    onAddFeed: () -> Unit,
    onDeleteFeed: (FeedSavedSearch) -> Unit,
) {
    var sourceFilter by remember { mutableStateOf(currentSourceFilter) }
    var languageFilter by remember { mutableStateOf(currentLanguageFilter) }
    var showSourceFeeds by remember { mutableStateOf(currentShowSourceFeeds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SanctuaryShapes.hero,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(SanctuaryShapes.medium)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    SanctuaryOverlineBadge(text = "SANCTUARY DISCOVER", compact = true)
                    Text(
                        text = "Feed Settings & Filter",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // 1. Source Feeds Toggle (Original SY Feeds)
                Surface(
                    shape = SanctuaryShapes.deckCard,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    border = SanctuaryBrushes.subtleBorder(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Source Feeds & Searches",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Display custom sources and saved searches on the feed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = showSourceFeeds,
                            onCheckedChange = { showSourceFeeds = it },
                        )
                    }
                }

                if (showSourceFeeds) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = SanctuaryShapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SanctuaryShapes.medium)
                            .clickable(onClick = onAddFeed),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                .size(28.dp)
                                .clip(SanctuaryShapes.tag)
                                .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add Source Feed",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Add a source or saved search to feed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (!customFeeds.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SanctuaryOverlineBadge(
                            text = "ACTIVE FEEDS (${customFeeds.size})",
                            compact = true,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        customFeeds.forEach { item ->
                            Surface(
                                shape = SanctuaryShapes.pill,
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (item.subtitle.isNotBlank()) {
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteFeed(item.feed) },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = stringResource(MR.strings.action_delete),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SanctuaryOverlineBadge(text = "SOURCE SCOPE", compact = true)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = sourceFilter == "all",
                        onClick = { sourceFilter = "all" },
                        shape = SanctuaryShapes.pill,
                        label = { Text("All Healthy Sources") },
                    )
                    FilterChip(
                        selected = sourceFilter == "pinned",
                        onClick = { sourceFilter = "pinned" },
                        shape = SanctuaryShapes.pill,
                        label = { Text("Pinned Only") },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SanctuaryOverlineBadge(text = "LANGUAGE FILTER", compact = true)
                Text(
                    text = "Restrict suggestions and highlights to selected language:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = languageFilter == "en",
                        onClick = { languageFilter = "en" },
                        shape = SanctuaryShapes.pill,
                        label = { Text("English (en)") },
                    )
                    FilterChip(
                        selected = languageFilter == "all",
                        onClick = { languageFilter = "all" },
                        shape = SanctuaryShapes.pill,
                        label = { Text("All Languages") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(sourceFilter, languageFilter, showSourceFeeds) },
            ) {
                Text(text = "Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
