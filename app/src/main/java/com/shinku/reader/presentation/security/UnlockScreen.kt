package com.shinku.reader.presentation.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
internal fun UnlockScreen() {
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Column(
            Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(MR.strings.unlock_app_title, stringResource(MR.strings.app_name)), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(stringResource(MR.strings.unlock_device_guidance), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
    }
}
