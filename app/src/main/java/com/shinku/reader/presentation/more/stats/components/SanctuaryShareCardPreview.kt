package com.shinku.reader.presentation.more.stats.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shinku.reader.R
import com.shinku.reader.presentation.core.components.SanctuaryProgressBar
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun SanctuaryShareCardPreview(
    theme: ShareCardTheme,
    avatarUri: String,
    nickname: String,
    activeTitle: String,
    content: ShareCardContent,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SanctuaryShapes.card,
        color = theme.surfaceColor,
        contentColor = theme.onSurfaceColor,
        border = BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.background(
                Brush.verticalGradient(listOf(theme.secondaryColor, theme.surfaceColor)),
            ).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(54.dp).clip(SanctuaryShapes.medium)
                        .background(theme.primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarUri.isNotBlank()) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_shinku_logo),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(activeTitle, style = MaterialTheme.typography.bodySmall, color = theme.primaryColor)
                }
            }
            content.persona?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            content.levelSummary?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = theme.primaryColor)
            }
            content.levelProgress?.let {
                SanctuaryProgressBar(
                    progress = it,
                    height = 6.dp,
                    color = theme.primaryColor,
                    trackColor = theme.primaryColor.copy(alpha = 0.15f),
                )
            }
            content.metrics.forEach { metric ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(metric.label, style = MaterialTheme.typography.labelSmall)
                    Text(metric.value, style = MaterialTheme.typography.titleSmall, color = theme.primaryColor, fontWeight = FontWeight.Bold)
                }
            }
            if (content.genres.isNotEmpty()) {
                Text(content.genres.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = theme.primaryColor)
            }
        }
    }
}
