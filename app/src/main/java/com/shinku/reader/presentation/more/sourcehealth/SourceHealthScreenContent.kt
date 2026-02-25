package com.shinku.reader.presentation.more.sourcehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.components.Pill
import com.shinku.reader.presentation.core.components.material.padding

@Composable
fun SourceHealthScreenContent(
    healthList: List<SourceHealthItem>,
    paddingValues: PaddingValues,
) {
    if (healthList.isEmpty()) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(MaterialTheme.padding.medium),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "No sources found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(healthList, key = { it.source.id }) { item ->
            SourceHealthItemRow(item)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SourceHealthItemRow(item: SourceHealthItem) {
    val health = item.health
    val performanceScore = health?.performanceScore ?: -1
    
    val (statusLabel, statusColor) = when {
        health == null -> "NEVER SCANNED" to Color.Gray
        performanceScore >= 95 -> "EXCELLENT" to Color(0xFF4CAF50)
        performanceScore >= 80 -> "STABLE" to Color(0xFF8BC34A)
        performanceScore >= 60 -> "UNSTABLE" to Color(0xFFFFC107)
        else -> "CRITICAL" to Color(0xFFF44336)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.padding.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Text(
                text = item.source.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            HealthRatingStars(performanceScore)
        }

        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )

            if (health?.isSensitive == true) {
                Pill(
                    text = "THROTTLED",
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (health != null) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
            ) {
                HealthStat(
                    icon = Icons.Default.HealthAndSafety,
                    label = "${health.healthScore}%",
                    color = statusColor
                )
                HealthStat(
                    icon = Icons.Default.Speed,
                    label = "${health.avgLatency}ms",
                )
                Text(
                    text = "S: ${health.successCount} / F: ${health.failureCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (health.lastError != null) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = health.lastError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthRatingStars(score: Int) {
    if (score == -1) return
    
    val rating = (score / 20f) // 0-5 scale
    Row {
        repeat(5) { index ->
            val starIcon = when {
                rating >= index + 1 -> Icons.Default.Star
                rating >= index + 0.5f -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }
            Icon(
                imageVector = starIcon,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HealthStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
