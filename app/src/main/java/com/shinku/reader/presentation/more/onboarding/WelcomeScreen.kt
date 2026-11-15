package com.shinku.reader.presentation.more.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.shinku.reader.R
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

/** Uses the app's existing crest, rather than adding generated artwork or sample reading records. */
@Composable
internal fun WelcomeScreen(onSetup: () -> Unit, onRestore: () -> Unit) {
    Scaffold(
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onSetup, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = SanctuaryShapes.medium) {
                    Text(stringResource(MR.strings.onboarding_setup))
                }
                OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = SanctuaryShapes.medium) {
                    Text(stringResource(MR.strings.pref_restore_backup))
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxWidth().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = SanctuaryShapes.hero, border = SanctuaryBrushes.subtleBorder(), contentColor = MaterialTheme.colorScheme.onSurface) {
                Column(
                    Modifier.fillMaxWidth().background(SanctuaryBrushes.heroVerticalGradient()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 144.dp), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ic_shinku_logo), contentDescription = null, modifier = Modifier.size(112.dp))
                    }
                    Text(stringResource(MR.strings.app_name), style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(MR.strings.onboarding_welcome_tagline), style = MaterialTheme.typography.titleMedium)
                }
            }
            WelcomeBenefit(Icons.AutoMirrored.Outlined.MenuBook, stringResource(MR.strings.welcome_read_title), stringResource(MR.strings.welcome_read_summary))
            WelcomeBenefit(Icons.Outlined.Palette, stringResource(MR.strings.welcome_customize_title), stringResource(MR.strings.welcome_customize_summary))
            WelcomeBenefit(Icons.Outlined.PhoneAndroid, stringResource(MR.strings.welcome_control_title), stringResource(MR.strings.welcome_control_summary))
            Text(stringResource(MR.strings.onboarding_optional_ai), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WelcomeBenefit(icon: ImageVector, title: String, summary: String) {
    Surface(
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
