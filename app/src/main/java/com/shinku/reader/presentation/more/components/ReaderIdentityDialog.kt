package com.shinku.reader.presentation.more.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shinku.reader.R
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun SanctuaryAvatarBox(
    size: Dp = 56.dp,
    levelBadge: Int? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
    val avatarUri by shinkuPreferences.readerAvatarUri().collectAsState()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        Surface(
            modifier = Modifier
                .size(size)
                .clip(SanctuaryShapes.deckCard)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = SanctuaryShapes.deckCard,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            ),
            shadowElevation = SanctuaryElevation.compact,
        ) {
            if (avatarUri.isNotBlank()) {
                AsyncImage(
                    model = Uri.parse(avatarUri),
                    contentDescription = "Reader Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shinku_logo),
                        contentDescription = "ShinKu Crest",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(size * 0.62f),
                    )
                }
            }
        }

        if (levelBadge != null) {
            Surface(
                shape = SanctuaryShapes.tag,
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                shadowElevation = 2.dp,
            ) {
                Text(
                    text = "Lv.$levelBadge",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@Composable
fun ReaderIdentityDialog(
    unlockedTitles: List<String> = emptyList(),
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
    val currentNickname by shinkuPreferences.readerNickname().collectAsState()
    val currentAvatarUri by shinkuPreferences.readerAvatarUri().collectAsState()
    val currentEquippedTitle by shinkuPreferences.equippedBadgeTitle().collectAsState()

    var tempName by remember(currentNickname) { mutableStateOf(currentNickname) }
    var tempAvatar by remember(currentAvatarUri) { mutableStateOf(currentAvatarUri) }
    var tempTitle by remember(currentEquippedTitle) { mutableStateOf(currentEquippedTitle) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {
                // Ignore if provider doesn't support persistable permission
            }
            tempAvatar = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sanctuary Identity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1. Custom Picture Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(SanctuaryShapes.deckCard)
                            .clickable { imagePickerLauncher.launch(arrayOf("image/*")) },
                        shape = SanctuaryShapes.deckCard,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        if (tempAvatar.isNotBlank()) {
                            AsyncImage(
                                model = Uri.parse(tempAvatar),
                                contentDescription = "Custom Avatar Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_shinku_logo),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(38.dp),
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                            shape = SanctuaryShapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Picture", maxLines = 1)
                        }
                        if (tempAvatar.isNotBlank()) {
                            TextButton(
                                onClick = { tempAvatar = "" },
                                modifier = Modifier.align(Alignment.Start),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset to ShinKu Crest", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 2. Nickname Field
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Reader Nickname") },
                    placeholder = { Text("e.g. Solo Leveler, Shadow Reader") },
                    singleLine = true,
                    shape = SanctuaryShapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 3. Equipped Badge Title Selector
                if (unlockedTitles.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Equipped Title",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilterChip(
                                selected = tempTitle.isBlank(),
                                onClick = { tempTitle = "" },
                                shape = SanctuaryShapes.pill,
                                label = { Text("Auto Rank") },
                            )
                            unlockedTitles.forEach { badgeTitle ->
                                FilterChip(
                                    selected = tempTitle == badgeTitle,
                                    onClick = { tempTitle = badgeTitle },
                                    shape = SanctuaryShapes.pill,
                                    label = { Text(badgeTitle, maxLines = 1) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    shinkuPreferences.readerNickname().set(tempName.trim())
                    shinkuPreferences.readerAvatarUri().set(tempAvatar)
                    shinkuPreferences.equippedBadgeTitle().set(tempTitle)
                    onDismiss()
                },
            ) {
                Text("Save Identity", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
