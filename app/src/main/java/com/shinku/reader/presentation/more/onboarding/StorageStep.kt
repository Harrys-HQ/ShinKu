package com.shinku.reader.presentation.more.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.storage.service.StoragePreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.more.settings.screen.SettingsDataScreen
import com.shinku.reader.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class StorageStep : OnboardingStep {

    private val storagePref = Injekt.get<StoragePreferences>().baseStorageDirectory()

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handler = LocalUriHandler.current

        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)
        val isConfigured = storagePref.isSet()

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SanctuaryShapes.deckCard,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                border = SanctuaryBrushes.subtleBorder(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = SanctuaryShapes.medium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(MR.strings.pref_storage_location),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = SettingsDataScreen.storageLocationText(storagePref),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                        Surface(
                            shape = SanctuaryShapes.tag,
                            color = if (isConfigured) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                        ) {
                            Text(
                                text = if (isConfigured) "Configured" else "Required",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isConfigured) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                            )
                        }
                    }

                    Text(
                        text = stringResource(
                            MR.strings.onboarding_storage_info,
                            stringResource(MR.strings.app_name),
                            "",
                        ).trimEnd(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = SanctuaryShapes.medium,
                        onClick = {
                            try {
                                pickStorageLocation.launch(null)
                            } catch (e: ActivityNotFoundException) {
                                context.toast(MR.strings.file_picker_error)
                            }
                        },
                    ) {
                        Text(stringResource(MR.strings.onboarding_storage_action_select))
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SanctuaryShapes.deckCard,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                border = SanctuaryBrushes.subtleBorder(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(MR.strings.onboarding_storage_help_info, stringResource(MR.strings.app_name)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        shape = SanctuaryShapes.medium,
                        onClick = { handler.openUri(SettingsDataScreen.HELP_URL) },
                    ) {
                        Text(stringResource(MR.strings.onboarding_storage_help_action))
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            storagePref.changes()
                .collectLatest { _isComplete = storagePref.isSet() }
        }
    }
}
