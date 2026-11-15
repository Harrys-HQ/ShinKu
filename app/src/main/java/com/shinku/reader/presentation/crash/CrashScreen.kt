package com.shinku.reader.presentation.crash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.InfoScreen
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.util.CrashLogUtil
import com.shinku.reader.util.system.copyToClipboard
import kotlinx.coroutines.launch

@Composable
fun CrashScreen(exception: Throwable?, onRestartClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val emptyDetails = stringResource(MR.strings.recovery_no_details)
    val details = remember(exception, emptyDetails) { exception?.stackTraceToString() ?: emptyDetails }
    CrashRecoveryContent(
        details = details,
        onRestart = onRestartClick,
        onCopy = { context.copyToClipboard("ShinKu", details) },
        onShare = { scope.launch { CrashLogUtil(context).dumpLogs(exception) } },
    )
}

@Composable
internal fun CrashRecoveryContent(details: String, onRestart: () -> Unit, onCopy: () -> Unit, onShare: () -> Unit) {
    InfoScreen(
        icon = Icons.Outlined.BugReport,
        headingText = stringResource(MR.strings.crash_screen_title),
        subtitleText = stringResource(MR.strings.crash_screen_description, stringResource(MR.strings.app_name)),
        acceptText = stringResource(MR.strings.crash_screen_restart_application), onAcceptClick = onRestart,
        rejectText = stringResource(MR.strings.recovery_copy_error), onRejectClick = onCopy,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(MR.strings.recovery_review_diagnostics), style = MaterialTheme.typography.bodyMedium)
            Surface(
                modifier = Modifier.fillMaxWidth(), shape = SanctuaryShapes.deckCard,
                color = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface,
                border = SanctuaryBrushes.subtleBorder(),
            ) {
                SelectionContainer { Text(details, Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) }
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.heightIn(min = 48.dp), shape = SanctuaryShapes.medium) {
                Text(stringResource(MR.strings.recovery_share_diagnostics))
            }
        }
    }
}
