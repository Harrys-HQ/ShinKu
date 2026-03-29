package com.shinku.reader.data.sync.service

import android.content.Context
import com.shinku.reader.domain.sync.SyncPreferences
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class NextcloudSyncService(
    context: Context,
    json: Json,
    syncPreferences: SyncPreferences,
    client: OkHttpClient = OkHttpClient(),
) : WebDavSyncService(context, json, syncPreferences, client) {
    // Nextcloud might need specific handling or just use the WebDAV implementation
    // For now, it's a marker class that could be expanded if needed.
}
