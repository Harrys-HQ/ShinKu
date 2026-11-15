package com.shinku.reader.presentation.more.stats.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.model.SanctuaryLevelInfo
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.util.system.toShareIntent
import kotlinx.coroutines.CancellationException
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
    var selectedTheme by remember { mutableStateOf(ShareCardTheme.OBSIDIAN) }
    var selectedStatistics by remember { mutableStateOf(emptySet<ShareStatistic>()) }
    var isSharing by remember { mutableStateOf(false) }
    val content = ShareCardContent(
        levelSummary = stringResource(MR.strings.journey_share_level_value, levelInfo.level, levelInfo.totalXp),
        levelProgress = levelInfo.levelProgress,
        persona = readingPersona,
        metrics = listOf(
            ShareCardMetric(ShareStatistic.TIME, stringResource(MR.strings.journey_share_time), readDurationText),
            ShareCardMetric(ShareStatistic.CHAPTERS, stringResource(MR.strings.journey_share_chapters), chaptersRead.toString()),
            ShareCardMetric(ShareStatistic.STREAK, stringResource(MR.strings.journey_share_streak), stringResource(MR.strings.journey_share_days, streakDays)),
            ShareCardMetric(ShareStatistic.BADGES, stringResource(MR.strings.journey_share_badges), unlockedBadgesCount.toString()),
        ),
        genres = topGenres.take(3),
    ).selected(selectedStatistics)
    val failedMessage = stringResource(MR.strings.journey_share_failed)
    val shareTitle = stringResource(MR.strings.journey_share_title)
    val shareMessage = stringResource(MR.strings.journey_share_message)
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            shinkuPreferences.readerAvatarUri().set(uri.toString())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SanctuaryShapes.hero,
        title = { Text(shareTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareCardTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = selectedTheme == theme,
                            onClick = { selectedTheme = theme },
                            modifier = Modifier.heightIn(min = 48.dp),
                            label = { Text(theme.label) },
                            leadingIcon = if (selectedTheme == theme) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                }
                SanctuaryShareCardPreview(selectedTheme, avatarUri, nickname, activeTitle, content)
                ShareCardOptions(selectedStatistics) { statistic ->
                    selectedStatistics = if (statistic in selectedStatistics) selectedStatistics - statistic else selectedStatistics + statistic
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                    enabled = !isSharing,
                    shape = SanctuaryShapes.medium,
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(MR.strings.journey_share_avatar))
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSharing,
                    onClick = {
                        isSharing = true
                        scope.launch {
                            try {
                                val uri = renderAndSaveShareCard(
                                    context = context,
                                    avatarUriString = avatarUri,
                                    nickname = nickname,
                                    activeTitle = activeTitle,
                                    content = content,
                                    primaryColor = selectedTheme.primaryColor.toArgb(),
                                    secondaryColor = selectedTheme.secondaryColor.toArgb(),
                                    surfaceColor = selectedTheme.surfaceColor.toArgb(),
                                    onSurfaceColor = selectedTheme.onSurfaceColor.toArgb(),
                                )
                                if (uri == null) {
                                    Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show()
                                } else {
                                    context.startActivity(Intent.createChooser(uri.toShareIntent(context, shareMessage), shareTitle))
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show()
                            } finally {
                                isSharing = false
                            }
                        }
                    },
                    shape = SanctuaryShapes.medium,
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (isSharing) MR.strings.journey_share_preparing else MR.strings.journey_share_image))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(MR.strings.action_close))
                }
            }
        },
    )
}
