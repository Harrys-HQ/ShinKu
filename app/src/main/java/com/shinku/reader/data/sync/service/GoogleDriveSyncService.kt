package com.shinku.reader.data.sync.service

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.data.backup.models.Backup
import com.shinku.reader.domain.sync.SyncPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GoogleDriveSyncService(
    context: Context,
    json: Json,
    syncPreferences: SyncPreferences,
) : SyncService(context, json, syncPreferences) {

    val isConfigured: Boolean
        get() = syncPreferences.googleDriveSyncUri().get().isNotBlank()

    override suspend fun doSync(syncData: SyncData): Backup? {
        val uriString = syncPreferences.googleDriveSyncUri().get()
        if (uriString.isBlank()) {
            logcat(LogPriority.WARN) { "Google Drive sync URI not configured" }
            return null
        }

        val rootDir = try {
            UniFile.fromUri(context, Uri.parse(uriString))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to resolve Google Drive folder URI" }
            return null
        }

        if (rootDir == null || !rootDir.exists()) {
            logcat(LogPriority.ERROR) { "Google Drive folder does not exist or access was revoked" }
            return null
        }

        return try {
            // 1. Download/Read remote data
            val remoteBackup = readRemoteData(rootDir)

            if (remoteBackup != null) {
                // 2. Merge local and remote
                val mergedSyncData = mergeSyncData(syncData, SyncData(deviceId = "", backup = remoteBackup))
                val mergedBackup = mergedSyncData.backup ?: syncData.backup

                // 3. Upload/Write merged data if changed
                if (mergedBackup != null && mergedBackup != remoteBackup) {
                    writeRemoteData(rootDir, mergedBackup)
                }
                mergedBackup
            } else {
                // No remote data, write local data
                syncData.backup?.let { writeRemoteData(rootDir, it) }
                syncData.backup
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Google Drive sync failed" }
            null
        }
    }

    private fun readRemoteData(rootDir: UniFile): Backup? {
        val file = rootDir.findFile(SYNC_FILE_NAME) ?: return null
        return try {
            val rawBytes = file.openInputStream().use { it.readBytes() }
            if (rawBytes.isEmpty()) return null

            val bytes = if (rawBytes.size >= 2 && rawBytes[0] == 0x1f.toByte() && rawBytes[1] == 0x8b.toByte()) {
                GZIPInputStream(ByteArrayInputStream(rawBytes)).use { it.readBytes() }
            } else {
                rawBytes
            }
            ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to decode remote Google Drive backup" }
            null
        }
    }

    private fun writeRemoteData(rootDir: UniFile, backup: Backup) {
        val file = rootDir.findFile(SYNC_FILE_NAME) ?: rootDir.createFile(SYNC_FILE_NAME)
            ?: throw IllegalStateException("Failed to create $SYNC_FILE_NAME in Google Drive folder")

        val rawBytes = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)
        val byteStream = ByteArrayOutputStream()
        GZIPOutputStream(byteStream).use { it.write(rawBytes) }
        val gzippedBytes = byteStream.toByteArray()

        file.openOutputStream().use { outputStream ->
            outputStream.write(gzippedBytes)
            outputStream.flush()
        }
        logcat(LogPriority.DEBUG) { "Saved sync data to Google Drive folder (${gzippedBytes.size} bytes)" }
    }

    suspend fun deleteSyncData(): DeleteSyncDataStatus {
        val uriString = syncPreferences.googleDriveSyncUri().get()
        if (uriString.isBlank()) return DeleteSyncDataStatus.NOT_INITIALIZED

        return try {
            val rootDir = UniFile.fromUri(context, Uri.parse(uriString))
                ?: return DeleteSyncDataStatus.NOT_INITIALIZED
            val file = rootDir.findFile(SYNC_FILE_NAME)
                ?: return DeleteSyncDataStatus.NO_FILES

            if (file.delete()) {
                DeleteSyncDataStatus.SUCCESS
            } else {
                DeleteSyncDataStatus.ERROR
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to delete Google Drive sync data" }
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
        const val SYNC_FILE_NAME = "sync_data.proto.gz"
    }
}
