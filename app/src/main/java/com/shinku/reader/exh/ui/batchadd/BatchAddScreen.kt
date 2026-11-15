package com.shinku.reader.exh.ui.batchadd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.BatchInputConfirmation
import com.shinku.reader.presentation.core.components.material.Button
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.plus
import com.shinku.reader.presentation.util.Screen

class BatchAddScreen : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { BatchAddScreenModel() }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        var confirmInput by rememberSaveable { mutableStateOf(false) }
        if (confirmInput) {
            BatchInputConfirmation(state.galleries, onConfirm = {
                confirmInput = false
                screenModel.addGalleries(context)
            }, onDismiss = { confirmInput = false })
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(SYMR.strings.batch_add),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            when (state.state) {
                BatchAddScreenModel.State.INPUT -> {
                    Column(
                        Modifier
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(MaterialTheme.padding.medium),
                    ) {
                        Text(text = stringResource(SYMR.strings.eh_batch_add_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(MR.strings.batch_add_scope_hint), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            minLines = 5,
                            maxLines = 10,
                            shape = SanctuaryShapes.medium,
                            value = state.galleries,
                            onValueChange = screenModel::updateGalleries,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = stringResource(SYMR.strings.eh_batch_add_description),
                                )
                            },
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                            textStyle = MaterialTheme.typography.bodyLarge,

                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.galleries.isNotBlank(),
                            onClick = { confirmInput = true },
                        ) {
                            Text(text = stringResource(SYMR.strings.eh_batch_add_button))
                        }
                    }
                }
                BatchAddScreenModel.State.PROGRESS -> {
                    LazyColumn(
                        contentPadding = paddingValues + PaddingValues(MaterialTheme.padding.medium),
                    ) {
                        item(key = "top") {
                            Column {
                                Text(text = stringResource(SYMR.strings.eh_batch_add_adding_galleries), style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    Modifier.fillMaxWidth(),
                                ) {
                                    val progress = state.progress.toFloat()
                                    if (state.progressTotal > 0 && !progress.isNaN()) {
                                        val realProgress = progress / state.progressTotal
                                        if (!realProgress.isNaN()) {
                                            LinearProgressIndicator(
                                                progress = { realProgress },
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .fillMaxWidth(),
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(MR.strings.batch_processed_count, state.progress, state.progressTotal),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        itemsIndexed(
                            state.events,
                            key = { index, text -> index + text.hashCode() },
                        ) { _, text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (state.progress == state.progressTotal) {
                            item(key = "finish") {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = screenModel::finish,
                                    ) {
                                        Text(text = stringResource(SYMR.strings.eh_batch_add_finish))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val onDismissRequest = screenModel::dismissDialog
        when (state.dialog) {
            BatchAddScreenModel.Dialog.NoGalleriesSpecified -> AlertDialog(
                onDismissRequest = onDismissRequest,
                confirmButton = {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                },
                title = {
                    Text(text = stringResource(SYMR.strings.batch_add_no_valid_galleries))
                },
                text = {
                    Text(text = stringResource(SYMR.strings.batch_add_no_valid_galleries_message))
                },
            )
            null -> Unit
        }
    }
}
