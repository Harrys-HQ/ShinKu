package com.shinku.reader.data.sync.service

import android.content.Context
import com.shinku.reader.domain.sync.SyncPreferences
import com.shinku.reader.data.backup.models.Backup
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

open class WebDavSyncService(
    context: Context,
    json: Json,
    syncPreferences: SyncPreferences,
    private val client: OkHttpClient = OkHttpClient(),
) : SyncService(context, json, syncPreferences) {

    override suspend fun doSync(syncData: SyncData): Backup? {
        val url = syncPreferences.webdavUrl().get()
        val username = syncPreferences.webdavUsername().get()
        val password = syncPreferences.webdavPassword().get()

        if (url.isBlank() || username.isBlank() || password.isBlank()) {
            return null
        }

        val auth = Credentials.basic(username, password)
        val fullUrl = if (url.endsWith("/")) "${url}sync_data.proto.gz" else "$url/sync_data.proto.gz"

        return try {
            // 1. Download remote data
            val remoteBackup = downloadRemoteData(fullUrl, auth)

            if (remoteBackup != null) {
                // 2. Merge local and remote
                val mergedSyncData = mergeSyncData(syncData, SyncData(deviceId = "", backup = remoteBackup))
                val mergedBackup = mergedSyncData.backup ?: syncData.backup

                // 3. Upload merged data if changed
                if (mergedBackup != null && mergedBackup != remoteBackup) {
                    uploadData(fullUrl, auth, mergedBackup)
                }
                mergedBackup
            } else {
                // No remote data, upload local data
                syncData.backup?.let { uploadData(fullUrl, auth, it) }
                syncData.backup
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "WebDAV sync failed" }
            null
        }
    }

    private fun downloadRemoteData(url: String, auth: String): Backup? {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", auth)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val byteArray = response.body?.bytes() ?: return null
                    ProtoBuf.decodeFromByteArray(Backup.serializer(), byteArray)
                } else if (response.code == 404) {
                    logcat(LogPriority.DEBUG) { "Remote sync file not found (404)" }
                    null
                } else {
                    throw IOException("Unexpected code $response")
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to download WebDAV data" }
            null
        }
    }

    private fun uploadData(url: String, auth: String, backup: Backup) {
        val byteArray = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)
        val requestBody = byteArray.toRequestBody("application/octet-stream".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", auth)
            .put(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }
        }
        logcat(LogPriority.DEBUG) { "Uploaded sync data to WebDAV" }
    }
}
