package com.shinku.reader.presentation.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import kotlinx.coroutines.delay

@Composable
fun ReaderCompletionBanner(
    chapterId: Long?,
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
    doublePages: Boolean = false,
) {
    var bannerTrigger by remember { mutableStateOf<Long?>(null) }
    var celebratedChapterIds by remember { mutableStateOf(setOf<Long>()) }
    var visible by remember { mutableStateOf(false) }

    val isLastPage = totalPages > 0 && (
        currentPage >= totalPages || (doublePages && currentPage >= totalPages - 1)
    )

    // Trigger celebration when reaching the last page of an uncelebrated chapter
    LaunchedEffect(chapterId, isLastPage) {
        if (chapterId != null && isLastPage && !celebratedChapterIds.contains(chapterId)) {
            celebratedChapterIds = celebratedChapterIds + chapterId
            bannerTrigger = chapterId
        }
    }

    // Auto-dismiss timeout: isolated to bannerTrigger so page flips and chapter transitions
    // cannot cancel the timer and leave the banner permanently stuck on screen.
    LaunchedEffect(bannerTrigger) {
        val trigger = bannerTrigger ?: return@LaunchedEffect
        visible = true
        try {
            delay(2800L)
        } finally {
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 16.dp),
    ) {
        Surface(
            onClick = { visible = false },
            shape = SanctuaryShapes.pill,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            ),
            shadowElevation = SanctuaryElevation.card,
            modifier = Modifier.heightIn(min = 44.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Chapter Complete • +25 XP",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
