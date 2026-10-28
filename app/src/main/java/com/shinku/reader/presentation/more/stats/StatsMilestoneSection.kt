package com.shinku.reader.presentation.more.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.model.Badge
import com.shinku.reader.domain.history.model.BadgeCategory
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.presentation.core.components.SanctuaryProgressBar
import com.shinku.reader.presentation.core.components.SanctuarySectionHeader
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

enum class MilestoneStatusFilter(val label: String) {
    ALL("All"),
    UNLOCKED("Unlocked"),
    IN_PROGRESS("In Progress"),
}

@Composable
internal fun MilestoneSection(
    data: com.shinku.reader.presentation.more.stats.data.StatsData.Milestones,
    modifier: Modifier = Modifier,
) {
    if (data.badges.isEmpty()) return

    val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
    val equippedTitle by shinkuPreferences.equippedBadgeTitle().collectAsState()

    var statusFilter by remember { mutableStateOf(MilestoneStatusFilter.ALL) }
    var categoryFilter by remember { mutableStateOf(BadgeCategory.ALL) }
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val totalBadges = data.badges.size
    val earnedBadges = data.earnedBadges.size
    val completionPercent = if (totalBadges > 0) (earnedBadges * 100) / totalBadges else 0

    val filteredBadges = remember(data.badges, statusFilter, categoryFilter) {
        data.badges.filter { badge ->
            val matchesStatus = when (statusFilter) {
                MilestoneStatusFilter.ALL -> true
                MilestoneStatusFilter.UNLOCKED -> badge.isEarned
                MilestoneStatusFilter.IN_PROGRESS -> !badge.isEarned
            }
            val matchesCategory = categoryFilter == BadgeCategory.ALL || badge.category == categoryFilter
            matchesStatus && matchesCategory
        }.sortedWith(
            compareByDescending<Badge> { it.isEarned }
                .thenByDescending { it.progress },
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SanctuaryShapes.card,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SanctuarySectionHeader(
                    title = "$earnedBadges / $totalBadges Badges ($completionPercent%)",
                    overline = "SANCTUARY ACHIEVEMENTS & TITLES",
                )
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = "Milestone Guide",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            SanctuaryProgressBar(
                progress = (earnedBadges.toFloat() / totalBadges.toFloat()).coerceIn(0f, 1f),
                height = 7.dp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MilestoneStatusFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        MilestoneStatusFilter.ALL -> totalBadges
                        MilestoneStatusFilter.UNLOCKED -> earnedBadges
                        MilestoneStatusFilter.IN_PROGRESS -> totalBadges - earnedBadges
                    }
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { statusFilter = filter },
                        shape = SanctuaryShapes.pill,
                        label = { Text("${filter.label} ($count)", maxLines = 1, softWrap = false) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BadgeCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { categoryFilter = cat },
                        shape = SanctuaryShapes.pill,
                        label = { Text(cat.label, maxLines = 1, softWrap = false) },
                    )
                }
            }

            filteredBadges.chunked(2).forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowBadges.forEach { badge ->
                        MilestoneCard(
                            badge = badge,
                            isEquipped = equippedTitle == badge.name,
                            onClick = { selectedBadge = badge },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowBadges.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    selectedBadge?.let { badge ->
        val isEquipped = equippedTitle == badge.name
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${badge.tier.icon} ${badge.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "${badge.tier.label} Tier • ${badge.category.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = badge.description, style = MaterialTheme.typography.bodyMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (badge.isEarned) "Unlocked 🎉" else "${(badge.progress * 100).toInt()}% Complete",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(text = badge.progressText, style = MaterialTheme.typography.labelSmall)
                        }
                        LinearProgressIndicator(
                            progress = { badge.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(SanctuaryShapes.pill),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    if (badge.hint.isNotBlank()) {
                        Text(
                            text = "💡 ${badge.hint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                if (badge.isEarned) {
                    Button(
                        onClick = {
                            shinkuPreferences.equippedBadgeTitle().set(if (isEquipped) "" else badge.name)
                            selectedBadge = null
                        },
                        shape = SanctuaryShapes.medium,
                    ) {
                        Text(if (isEquipped) "Unequip Title" else "Equip as Title")
                    }
                } else {
                    TextButton(onClick = { selectedBadge = null }) { Text("Close") }
                }
            },
            dismissButton = {
                if (badge.isEarned) {
                    TextButton(onClick = { selectedBadge = null }) { Text("Close") }
                }
            },
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("🏆 Sanctuary Mastery & Titles", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Unlock Bronze (+50 XP), Silver (+100 XP), Gold (+200 XP), Platinum (+350 XP), and Mythic (+500 XP) badges. Tap any unlocked badge to equip it as your custom Reader Title on your Profile and Share Card!",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got it") }
            },
        )
    }
}

@Composable
private fun MilestoneCard(
    badge: Badge,
    isEquipped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (badge.iconId) {
        "time" -> Icons.Outlined.Schedule
        "streak" -> Icons.Outlined.History
        "genre" -> Icons.Outlined.CollectionsBookmark
        "chapter", "library" -> Icons.Outlined.LocalLibrary
        "special" -> Icons.Outlined.EmojiEvents
        else -> Icons.Outlined.Star
    }

    Surface(
        modifier = modifier
            .clip(SanctuaryShapes.deckCard)
            .clickable(onClick = onClick),
        shape = SanctuaryShapes.deckCard,
        color = if (badge.isEarned) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
        },
        border = if (isEquipped) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else if (badge.isEarned) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        } else {
            SanctuaryBrushes.subtleBorder()
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(SanctuaryShapes.tag)
                        .background(
                            if (badge.isEarned) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (badge.isEarned) icon else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (badge.isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(text = badge.tier.icon, style = MaterialTheme.typography.labelSmall)
            }

            Text(
                text = badge.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 34.dp),
            )

            if (badge.isEarned) {
                Text(
                    text = if (isEquipped) "★ Equipped Title" else "Unlocked • Tap to Equip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = badge.progressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Text(
                            text = "${(badge.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    SanctuaryProgressBar(
                        progress = badge.progress,
                        height = 5.dp,
                    )
                }
            }
        }
    }
}
