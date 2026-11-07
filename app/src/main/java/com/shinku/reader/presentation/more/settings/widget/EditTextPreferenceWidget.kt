package com.shinku.reader.presentation.more.settings.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.DialogProperties
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import kotlinx.coroutines.launch

@Composable
fun EditTextPreferenceWidget(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    value: String,
    isSensitive: Boolean = false,
    widget: @Composable (() -> Unit)? = null,
    onConfirm: suspend (String) -> Boolean,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = if (isSensitive) {
            stringResource(
                if (value.isBlank()) MR.strings.secret_not_configured else MR.strings.secret_configured,
            )
        } else {
            subtitle?.format(value)
        },
        icon = icon,
        widget = widget,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val scope = rememberCoroutineScope()
        val onDismissRequest = { isDialogShown = false }
        var showSecret by remember { mutableStateOf(false) }
        val editState = if (isSensitive) {
            remember { mutableStateOf(TextFieldValue(value)) }
        } else {
            rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(value)) }
        }
        var textFieldValue by editState
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = title) },
            text = {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    visualTransformation = if (isSensitive && !showSecret) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = if (isSensitive) KeyboardType.Password else KeyboardType.Text),
                    trailingIcon = {
                        Row {
                            if (isSensitive) {
                                IconButton(onClick = { showSecret = !showSecret }) {
                                    Icon(
                                        imageVector = if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = stringResource(if (showSecret) MR.strings.secret_hide else MR.strings.secret_show),
                                    )
                                }
                            }
                            if (textFieldValue.text.isBlank()) {
                                Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                            } else {
                                IconButton(onClick = { textFieldValue = TextFieldValue("") }) {
                                    Icon(imageVector = Icons.Filled.Cancel, contentDescription = null)
                                }
                            }
                        }
                    },
                    isError = textFieldValue.text.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = true,
            ),
            confirmButton = {
                TextButton(
                    enabled = textFieldValue.text != value && textFieldValue.text.isNotBlank(),
                    onClick = {
                        scope.launch {
                            if (onConfirm(textFieldValue.text)) {
                                onDismissRequest()
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}
