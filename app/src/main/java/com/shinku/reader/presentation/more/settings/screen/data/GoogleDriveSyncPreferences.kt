package com.shinku.reader.presentation.more.settings.screen.data

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hippo.unifile.UniFile
import com.shinku.reader.data.sync.service.GoogleDriveSyncService
import com.shinku.reader.domain.sync.SyncPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.more.settings.Preference
import com.shinku.reader.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun getGoogleDrivePreferences(syncPreferences: SyncPreferences): List<Preference> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncUriPref = syncPreferences.googleDriveSyncUri()
    var currentUriString by remember { mutableStateOf(syncUriPref.get()) }
    val googleDriveSync = remember { GoogleDriveSyncService(context, Injekt.get(), syncPreferences) }
    var showPurgeDialog by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                logcat("GoogleDrive", LogPriority.ERROR) { "Failed to take persistable URI permission: ${e.message}" }
            }

            val uriStr = uri.toString()
            syncUriPref.set(uriStr)
            currentUriString = uriStr
            context.toast(SYMR.strings.google_drive_login_success)
        }
    }

    val folderDisplayName = remember(currentUriString) {
        if (currentUriString.isBlank()) {
            null
        } else {
            try {
                UniFile.fromUri(context, Uri.parse(currentUriString))?.name ?: currentUriString
            } catch (e: Exception) {
                currentUriString
            }
        }
    }

    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            title = { Text(text = stringResource(SYMR.strings.pref_purge_confirmation_title)) },
            text = { Text(text = stringResource(SYMR.strings.pref_purge_confirmation_message)) },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPurgeDialog = false
                        scope.launch {
                            when (googleDriveSync.deleteSyncData()) {
                                GoogleDriveSyncService.DeleteSyncDataStatus.SUCCESS -> {
                                    context.toast(SYMR.strings.google_drive_sync_data_purged)
                                }
                                GoogleDriveSyncService.DeleteSyncDataStatus.NO_FILES -> {
                                    context.toast(SYMR.strings.google_drive_sync_data_not_found)
                                }
                                GoogleDriveSyncService.DeleteSyncDataStatus.NOT_INITIALIZED -> {
                                    context.toast(SYMR.strings.google_drive_not_signed_in)
                                }
                                GoogleDriveSyncService.DeleteSyncDataStatus.ERROR -> {
                                    context.toast(SYMR.strings.google_drive_sync_data_purge_error)
                                }
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
        )
    }

    return persistentListOf(
        Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.pref_google_drive_folder),
            subtitle = folderDisplayName ?: stringResource(SYMR.strings.pref_google_drive_folder_summ),
            onClick = { folderLauncher.launch(null) },
        ),
        Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.pref_google_drive_purge_sync_data),
            onClick = { showPurgeDialog = true },
        ),
    )
}
