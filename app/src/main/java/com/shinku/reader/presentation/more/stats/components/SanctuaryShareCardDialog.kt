package com.shinku.reader.presentation.more.stats.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.model.SanctuaryLevelInfo
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.util.system.toShareIntent
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun SanctuaryShareCardDialog(
    nickname: String,
    activeTitle: String,
    levelInfo: SanctuaryLevelInfo,
    readDurationText: String,
    chaptersRead: Int,
    streakDays: Int,
    topGenres: List<String>,
    unlockedBadgesCount: Int,
    readingPersona: String = "Sanctuary Scholar",
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
    val avatarUri by shinkuPreferences.readerAvatarUri().collectAsState()
    var selectedTheme by remember { mutableStateOf(ShareCardTheme.CRIMSON) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {}
            shinkuPreferences.readerAvatarUri().set(uri.toString())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SanctuaryShapes.hero,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SanctuaryOverlineBadge(text = "SANCTUARY CHRONICLES", compact = true)
                Text(
                    text = "Reading Journey Card",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Theme Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShareCardTheme.entries.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SanctuaryShapes.pill)
                                .clickable { selectedTheme = theme },
                            shape = SanctuaryShapes.pill,
                            color = if (isSelected) theme.primaryColor else theme.surfaceColor,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) theme.primaryColor else theme.primaryColor.copy(alpha = 0.35f),
                            ),
                        ) {
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else theme.onSurfaceColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                // Live Preview Card
                SanctuaryShareCardPreview(
                    theme = selectedTheme,
                    avatarUri = avatarUri,
                    nickname = nickname,
                    activeTitle = activeTitle,
                    levelInfo = levelInfo,
                    readingPersona = readingPersona,
                    readDurationText = readDurationText,
                    chaptersRead = chaptersRead,
                    streakDays = streakDays,
                    unlockedBadgesCount = unlockedBadgesCount,
                    topGenres = topGenres,
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                    shape = SanctuaryShapes.medium,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Avatar")
                }
                Button(
                    onClick = {
                        scope.launch {
                            val uri = renderAndSaveShareCard(
                                context = context,
                                avatarUriString = avatarUri,
                                nickname = nickname,
                                activeTitle = activeTitle,
                                readingPersona = readingPersona,
                                level = levelInfo.level,
                                totalXp = levelInfo.totalXp,
                                levelProgress = levelInfo.levelProgress,
                                readDurationText = readDurationText,
                                chaptersRead = chaptersRead,
                                streakDays = streakDays,
                                unlockedBadgesCount = unlockedBadgesCount,
                                topGenres = topGenres.take(3),
                                primaryColor = selectedTheme.primaryColor.toArgb(),
                                secondaryColor = selectedTheme.secondaryColor.toArgb(),
                                surfaceColor = selectedTheme.surfaceColor.toArgb(),
                                onSurfaceColor = selectedTheme.onSurfaceColor.toArgb(),
                            )
                            if (uri != null) {
                                val intent = uri.toShareIntent(
                                    context = context,
                                    message = "My ShinKu Reading Sanctuary Journey ✨",
                                )
                                context.startActivity(Intent.createChooser(intent, "Share Journey Card"))
                            }
                        }
                    },
                    shape = SanctuaryShapes.medium,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Image")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
