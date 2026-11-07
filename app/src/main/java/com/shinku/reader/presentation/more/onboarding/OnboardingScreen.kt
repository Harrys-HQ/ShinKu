package com.shinku.reader.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.InfoScreen
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val slideDistance = rememberSlideDistance()

    var welcomed by rememberSaveable { mutableStateOf(false) }
    var restoreAfterSetup by rememberSaveable { mutableStateOf(false) }
    if (!welcomed) {
        InfoScreen(
            icon = Icons.Outlined.RocketLaunch,
            headingText = stringResource(MR.strings.app_name),
            subtitleText = stringResource(MR.strings.onboarding_welcome_tagline),
            acceptText = stringResource(MR.strings.onboarding_setup),
            onAcceptClick = {
                restoreAfterSetup = false
                welcomed = true
            },
            rejectText = stringResource(MR.strings.pref_restore_backup),
            onRejectClick = {
                restoreAfterSetup = true
                welcomed = true
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WelcomeFeatureCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = stringResource(MR.strings.browse),
                    description = stringResource(MR.strings.onboarding_description),
                )
                WelcomeFeatureCard(
                    icon = Icons.Outlined.Translate,
                    title = stringResource(MR.strings.action_live_translation),
                    description = stringResource(MR.strings.pref_live_translation_summary),
                )
                WelcomeFeatureCard(
                    icon = Icons.Outlined.CloudSync,
                    title = stringResource(MR.strings.label_data_storage),
                    description = stringResource(MR.strings.pref_storage_location_info),
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
        return
    }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember {
        listOf(
            StorageStep(),
            PermissionStep(),
            ThemeStep(),
            GuidesStep(onRestoreBackup = onRestoreBackup),
        )
    }
    val isLastStep = currentStep == steps.lastIndex

    BackHandler {
        if (currentStep > 0) currentStep-- else welcomed = false
    }

    InfoScreen(
        icon = Icons.Outlined.RocketLaunch,
        headingText = stringResource(MR.strings.onboarding_heading),
        subtitleText = stringResource(MR.strings.onboarding_description),
        acceptText = stringResource(
            if (isLastStep && restoreAfterSetup) {
                MR.strings.pref_restore_backup
            } else if (isLastStep) {
                MR.strings.onboarding_action_finish
            } else {
                MR.strings.onboarding_action_next
            },
        ),
        canAccept = steps[currentStep].isComplete,
        onAcceptClick = {
            if (isLastStep) {
                if (restoreAfterSetup) onRestoreBackup() else onComplete()
            } else {
                currentStep++
            }
        },
    ) {
        Text(stringResource(MR.strings.onboarding_step_counter, currentStep + 1, steps.size))
        LinearProgressIndicator(progress = { (currentStep + 1f) / steps.size }, modifier = Modifier.fillMaxWidth())
        Box(
            modifier = Modifier
                .padding(vertical = MaterialTheme.padding.small)
                .clip(SanctuaryShapes.hero)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(
                    border = SanctuaryBrushes.subtleBorder(),
                    shape = SanctuaryShapes.hero,
                ),
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    materialSharedAxisX(
                        forward = targetState > initialState,
                        slideDistance = slideDistance,
                    )
                },
                label = "stepContent",
            ) {
                steps[it].Content()
            }
        }
    }
}

@Composable
private fun WelcomeFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = SanctuaryShapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
