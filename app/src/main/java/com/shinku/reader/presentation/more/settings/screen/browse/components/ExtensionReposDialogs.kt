package com.shinku.reader.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.delay
import com.shinku.reader.domain.extensionrepo.model.ExtensionRepo
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import kotlin.time.Duration.Companion.seconds

@Composable
fun ExtensionRepoCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    repoUrls: ImmutableSet<String>,
) {
    var name by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name) { repoUrls.contains(name) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                enabled = name.isNotEmpty() && !nameAlreadyExists,
                shape = SanctuaryShapes.medium,
                onClick = {
                    onCreate(name)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_add_repo))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(MR.strings.action_add_repo_message, stringResource(MR.strings.app_name)))

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = name,
                    onValueChange = { name = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                        )
                    },
                    placeholder = {
                        Text(text = "https://example.com/repo")
                    },
                    label = {
                        Text(text = stringResource(MR.strings.label_add_repo_input))
                    },
                    supportingText = {
                        val msgRes = if (name.isNotEmpty() && nameAlreadyExists) {
                            MR.strings.error_repo_exists
                        } else {
                            MR.strings.information_required_plain
                        }
                        Text(text = stringResource(msgRes))
                    },
                    isError = name.isNotEmpty() && nameAlreadyExists,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                )

                Surface(
                    shape = SanctuaryShapes.medium,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                    border = SanctuaryBrushes.subtleBorder(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Add repositories from trusted sources only. Extensions can execute code and access network resources.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )

    LaunchedEffect(focusRequester) {
        // TODO: https://issuetracker.google.com/issues/204502668
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun ExtensionRepoDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    repo: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_delete_repo))
        },
        text = {
            Text(text = stringResource(MR.strings.delete_repo_confirmation, repo))
        },
    )
}

@Composable
fun ExtensionRepoConflictDialog(
    oldRepo: ExtensionRepo,
    newRepo: ExtensionRepo,
    onDismissRequest: () -> Unit,
    onMigrate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onMigrate()
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_replace_repo))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_replace_repo_title))
        },
        text = {
            Text(text = stringResource(MR.strings.action_replace_repo_message, newRepo.name, oldRepo.name))
        },
    )
}

@Composable
fun ExtensionRepoConfirmDialog(
    onDismissRequest: () -> Unit,
    onCreate: () -> Unit,
    repo: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.action_add_repo))
        },
        text = {
            Text(text = stringResource(MR.strings.add_repo_confirmation, repo))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate()
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
