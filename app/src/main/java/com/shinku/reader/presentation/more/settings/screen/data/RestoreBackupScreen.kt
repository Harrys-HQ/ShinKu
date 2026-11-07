package com.shinku.reader.presentation.more.settings.screen.data

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.data.backup.BackupFileValidator
import com.shinku.reader.data.backup.restore.BackupRestoreJob
import com.shinku.reader.data.backup.restore.RestoreOptions
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.WarningBanner
import com.shinku.reader.presentation.core.components.LabeledCheckbox
import com.shinku.reader.presentation.core.components.LazyColumnWithAction
import com.shinku.reader.presentation.core.components.SectionCard
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.util.system.DeviceUtil
import kotlinx.coroutines.flow.update

@Composable
fun RestoreConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.restore_confirm_title)) },
        text = { Text(stringResource(MR.strings.restore_confirm_summary)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(MR.strings.action_restore)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}

class RestoreBackupScreen(
    private val uri: String,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { RestoreBackupScreenModel(context, uri) }
        val state by model.state.collectAsState()
        var confirmRestore by remember { mutableStateOf(false) }
        if (confirmRestore) {
            RestoreConfirmationDialog(
                onDismiss = { confirmRestore = false },
                onConfirm = {
                    confirmRestore = false
                    model.startRestore()
                    navigator.pop()
                },
            )
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.pref_restore_backup),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            LazyColumnWithAction(
                contentPadding = contentPadding,
                actionLabel = stringResource(MR.strings.action_restore),
                actionEnabled = state.canRestore && state.options.canRestore(),
                onClickAction = {
                    confirmRestore = true
                },
            ) {
                item {
                    SectionCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Surface(
                                shape = SanctuaryShapes.medium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                modifier = Modifier.size(42.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.SettingsBackupRestore,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = android.net.Uri.parse(uri).lastPathSegment?.substringAfterLast('/') ?: stringResource(MR.strings.pref_restore_backup),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(MR.strings.restore_confirm_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                shape = SanctuaryShapes.tag,
                                color = if (state.canRestore) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                            ) {
                                Text(
                                    text = if (state.canRestore) "Validated" else "Invalid",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (state.canRestore) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    },
                                )
                            }
                        }
                    }
                }
                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                    item {
                        WarningBanner(MR.strings.restore_miui_warning)
                    }
                }

                if (state.canRestore) {
                    item {
                        SectionCard {
                            RestoreOptions.options.forEach { option ->
                                LabeledCheckbox(
                                    label = stringResource(option.label),
                                    checked = option.getter(state.options),
                                    onCheckedChange = {
                                        model.toggle(option.setter, it)
                                    },
                                )
                            }
                        }
                    }
                }

                if (state.error != null) {
                    errorMessageItem(state.error)
                }
            }
        }
    }

    private fun LazyListScope.errorMessageItem(
        error: Any?,
    ) {
        item {
            SectionCard {
                Column(
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    val msg = buildAnnotatedString {
                        when (error) {
                            is MissingRestoreComponents -> {
                                appendLine(stringResource(MR.strings.backup_restore_content_full))
                                if (error.sources.isNotEmpty()) {
                                    appendLine()
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        appendLine(stringResource(MR.strings.backup_restore_missing_sources))
                                    }
                                    error.sources.joinTo(
                                        this,
                                        separator = "\n- ",
                                        prefix = "- ",
                                    )
                                }
                                if (error.trackers.isNotEmpty()) {
                                    appendLine()
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        appendLine(stringResource(MR.strings.backup_restore_missing_trackers))
                                    }
                                    error.trackers.joinTo(
                                        this,
                                        separator = "\n- ",
                                        prefix = "- ",
                                    )
                                }
                            }

                            is InvalidRestore -> {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    appendLine(stringResource(MR.strings.invalid_backup_file))
                                }
                                appendLine(error.uri.toString())

                                appendLine()

                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    appendLine(stringResource(MR.strings.invalid_backup_file_error))
                                }
                                appendLine(error.message)
                            }

                            else -> {
                                appendLine(error.toString())
                            }
                        }
                    }

                    SelectionContainer {
                        Text(text = msg)
                    }
                }
            }
        }
    }
}

private class RestoreBackupScreenModel(
    private val context: Context,
    private val uri: String,
) : StateScreenModel<RestoreBackupScreenModel.State>(State()) {

    init {
        validate(uri.toUri())
    }

    fun toggle(setter: (RestoreOptions, Boolean) -> RestoreOptions, enabled: Boolean) {
        mutableState.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
    }

    fun startRestore() {
        BackupRestoreJob.start(
            context = context,
            uri = uri.toUri(),
            options = state.value.options,
        )
    }

    private fun validate(uri: Uri) {
        val results = try {
            BackupFileValidator(context).validate(uri)
        } catch (e: Exception) {
            setError(
                error = InvalidRestore(uri, e.message.toString()),
                canRestore = false,
            )
            return
        }

        if (results.missingSources.isNotEmpty() || results.missingTrackers.isNotEmpty()) {
            setError(
                error = MissingRestoreComponents(uri, results.missingSources, results.missingTrackers),
                canRestore = true,
            )
            return
        }

        setError(error = null, canRestore = true)
    }

    private fun setError(error: Any?, canRestore: Boolean) {
        mutableState.update {
            it.copy(
                error = error,
                canRestore = canRestore,
            )
        }
    }

    @Immutable
    data class State(
        val error: Any? = null,
        val canRestore: Boolean = false,
        val options: RestoreOptions = RestoreOptions(),
    )
}

private data class MissingRestoreComponents(
    val uri: Uri,
    val sources: List<String>,
    val trackers: List<String>,
)

private data class InvalidRestore(
    val uri: Uri? = null,
    val message: String,
)
