package com.shinku.reader.data.sync.service

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.shinku.reader.domain.sync.SyncPreferences
import com.shinku.reader.data.backup.models.Backup
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DropboxSyncService(
    context: Context,
    json: Json,
    syncPreferences: SyncPreferences,
) : SyncService(context, json, syncPreferences) {

    private var client: DbxClientV2? = null

    val isLoggedIn: Boolean
        get() = syncPreferences.dropboxAccessToken().get().isNotBlank()

    init {
        val accessToken = syncPreferences.dropboxAccessToken().get()
        if (accessToken.isNotBlank()) {
            val config = DbxRequestConfig.newBuilder("ShinKu").build()
            client = DbxClientV2(config, accessToken)
        }
    }

    override suspend fun doSync(syncData: SyncData): Backup? {
        val client = client ?: throw Exception("Dropbox not signed in")

        return try {
            // 1. Download remote data
            val remoteBackup = downloadRemoteData(client)

            if (remoteBackup != null) {
                // 2. Merge local and remote
                val mergedSyncData = mergeSyncData(syncData, SyncData(deviceId = "", backup = remoteBackup))
                val mergedBackup = mergedSyncData.backup ?: syncData.backup

                // 3. Upload merged data if changed
                if (mergedBackup != null && mergedBackup != remoteBackup) {
                    uploadData(client, mergedBackup)
                }
                mergedBackup
            } else {
                // No remote data, upload local data
                syncData.backup?.let { uploadData(client, it) }
                syncData.backup
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Dropbox sync failed" }
            null
        }
    }

    private fun downloadRemoteData(client: DbxClientV2): Backup? {
        return try {
            val outputStream = ByteArrayOutputStream()
            client.files().downloadBuilder(SYNC_FILE_PATH).download(outputStream)
            val byteArray = outputStream.toByteArray()
            ProtoBuf.decodeFromByteArray(Backup.serializer(), byteArray)
        } catch (e: com.dropbox.core.v2.files.DownloadErrorException) {
            // Path not found is common for first sync
            if (e.toString().contains("path") && e.toString().contains("not_found")) {
                logcat(LogPriority.DEBUG) { "Remote sync file not found" }
                null
            } else {
                throw e
            }
        }
    }

    private fun uploadData(client: DbxClientV2, backup: Backup) {
        val byteArray = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)
        ByteArrayInputStream(byteArray).use { inputStream ->
            client.files().uploadBuilder(SYNC_FILE_PATH)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(inputStream)
        }
        logcat(LogPriority.DEBUG) { "Uploaded sync data to Dropbox" }
    }

    fun startLogin(context: Context) {
        Auth.startOAuth2Authentication(context, DROPBOX_APP_KEY)
    }

    fun handleLoginResponse(): Boolean {
        val accessToken = Auth.getOAuth2Token()
        if (accessToken != null) {
            syncPreferences.dropboxAccessToken().set(accessToken)
            val config = DbxRequestConfig.newBuilder("ShinKu").build()
            client = DbxClientV2(config, accessToken)
            return true
        }
        return false
    }

    suspend fun deleteSyncDataFromDropbox(): DeleteSyncDataStatus {
        val client = client ?: return DeleteSyncDataStatus.NOT_INITIALIZED
        return try {
            client.files().deleteV2(SYNC_FILE_PATH)
            DeleteSyncDataStatus.SUCCESS
        } catch (e: com.dropbox.core.v2.files.DeleteErrorException) {
            if (e.toString().contains("path") && e.toString().contains("not_found")) {
                DeleteSyncDataStatus.NO_FILES
            } else {
                DeleteSyncDataStatus.ERROR
            }
        } catch (e: Exception) {
            DeleteSyncDataStatus.ERROR
        }
    }

    enum class DeleteSyncDataStatus {
        SUCCESS,
        NO_FILES,
        NOT_INITIALIZED,
        ERROR,
    }

    companion object {
        private const val SY_FOLDER = "/ShinKu"
        private const val SYNC_FILE_PATH = "$SY_FOLDER/sync_data.proto.gz"
        private const val DROPBOX_APP_KEY = "6h2m965uod1itio"
    }
}
