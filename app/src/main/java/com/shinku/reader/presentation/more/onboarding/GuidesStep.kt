package com.shinku.reader.presentation.more.onboarding

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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.theme.TachiyomiPreviewTheme

internal class GuidesStep(
    private val onRestoreBackup: () -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val handler = LocalUriHandler.current

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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
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
                                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(MR.strings.getting_started_guide),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(MR.strings.onboarding_guides_new_user, stringResource(MR.strings.app_name)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        shape = SanctuaryShapes.medium,
                        onClick = { handler.openUri(GETTING_STARTED_URL) },
                    ) {
                        Text(stringResource(MR.strings.getting_started_guide))
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = SanctuaryShapes.medium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.SettingsBackupRestore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(MR.strings.pref_restore_backup),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(MR.strings.onboarding_guides_returning_user, stringResource(MR.strings.app_name)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        shape = SanctuaryShapes.medium,
                        onClick = onRestoreBackup,
                    ) {
                        Text(stringResource(MR.strings.pref_restore_backup))
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SanctuaryShapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = SanctuaryBrushes.subtleBorder(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(MR.strings.onboarding_optional_ai),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

const val GETTING_STARTED_URL = "https://harrys-hq.github.io/ShinKu/guides/getting-started"

@PreviewLightDark
@Composable
private fun GuidesStepPreview() {
    TachiyomiPreviewTheme {
        GuidesStep(
            onRestoreBackup = {},
        ).Content()
    }
}
