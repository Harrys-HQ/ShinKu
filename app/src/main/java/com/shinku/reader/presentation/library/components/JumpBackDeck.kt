package com.shinku.reader.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.repository.HistoryRepository
import com.shinku.reader.presentation.core.components.SanctuarySectionHeader
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.manga.components.MangaCover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun JumpBackDeck(
    items: List<com.shinku.reader.ui.library.LibraryScreenModel.LastReadItem>,
    onClickContinue: (Long, Long) -> Unit,
    onClickDetails: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val streakDays by produceState(initialValue = 0, key1 = items.firstOrNull()?.chapter?.id) {
        value = withContext(Dispatchers.IO) {
            try {
                val history = Injekt.get<HistoryRepository>().getAllHistory()
                fun truncate(d: Date): Date {
                    val c = Calendar.getInstance().apply {
                        time = d
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return c.time
                }
                val readDates = history.mapNotNull { it.readAt }.map(::truncate).distinct().sortedDescending()
                if (readDates.isEmpty()) return@withContext 0
                val today = truncate(Date())
                val yesterday = Date(today.time - TimeUnit.DAYS.toMillis(1))
                val twoDaysAgo = Date(today.time - TimeUnit.DAYS.toMillis(2))
                if (readDates[0] != today && readDates[0] != yesterday && readDates[0] != twoDaysAgo) return@withContext 0
                var streak = 1
                var shieldsUsed = if (readDates[0] == twoDaysAgo) 1 else 0
                for (i in 0 until readDates.size - 1) {
                    val diff = TimeUnit.MILLISECONDS.toDays(readDates[i].time - readDates[i + 1].time)
                    if (diff <= 1L) {
                        streak++
                    } else if (diff == 2L && shieldsUsed < (1 + streak / 7)) {
                        shieldsUsed++
                        streak++
                    } else {
                        break
                    }
                }
                streak
            } catch (_: Exception) {
                0
            }
        }
    }

    val badgeText = if (streakDays > 0) {
        val shield = if (streakDays >= 7) " 🛡️" else ""
        "🔥 ${streakDays}D$shield • ${items.size} ONGOING"
    } else {
        "${items.size} ONGOING"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.extraSmall),
    ) {
        SanctuarySectionHeader(
            title = "CONTINUE READING",
            trailingBadge = badgeText,
        )

        LazyRow(
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.extraSmall,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.manga.id }) { item ->
                JumpBackDeckCard(
                    item = item,
                    onClickContinue = { onClickContinue(item.manga.id, item.chapter.id) },
                    onClickDetails = { onClickDetails(item.manga.id) },
                )
            }
        }
    }
}

@Composable
private fun JumpBackDeckCard(
    item: com.shinku.reader.ui.library.LibraryScreenModel.LastReadItem,
    onClickContinue: () -> Unit,
    onClickDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(120.dp)
            .clip(SanctuaryShapes.deckCard)
            .clickable(onClick = onClickContinue),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        border = SanctuaryBrushes.subtleBorder(),
        shadowElevation = SanctuaryElevation.compact,
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(SanctuaryShapes.medium),
            ) {
                MangaCover.Book(
                    data = item.manga,
                    contentDescription = item.manga.title,
                    shape = SanctuaryShapes.medium,
                    modifier = Modifier.matchParentSize(),
                )

                // Quick Play Pill button overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(SanctuaryShapes.tag)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onClickContinue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Resume",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                // Theme-aware Info pill top-end to open details
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(SanctuaryShapes.tag)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                        .clickable(onClick = onClickDetails),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.manga.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = formatChapterText(item.chapter),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
