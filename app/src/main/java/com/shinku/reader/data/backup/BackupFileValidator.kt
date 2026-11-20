package com.shinku.reader.data.backup

import android.content.Context
import android.net.Uri
import com.shinku.reader.data.track.TrackerManager
import com.shinku.reader.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupFileValidator(
    private val context: Context,

    private val sourceManager: SourceManager = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
) {

    /**
     * Checks for critical backup file data.
     *
     * @return List of missing sources or missing trackers.
     */
    fun validate(uri: Uri): Results {
        val inspection = try {
            BackupDecoder(context).inspect(uri)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        val sources = inspection.sources.associate { it.sourceId to it.name }
        val missingSources = sources
            .filter { sourceManager.get(it.key) == null }
            .values.map {
                val id = it.toLongOrNull()
                if (id == null) {
                    it
                } else {
                    sourceManager.getOrStub(id).toString()
                }
            }
            .distinct()
            .sorted()

        val missingTrackers = inspection.trackerIds
            .mapNotNull { trackerManager.get(it.toLong()) }
            .filter { !it.isLoggedIn }
            .map { it.name }
            .sorted()

        return Results(missingSources, missingTrackers, inspection.summary)
    }

    data class Results(
        val missingSources: List<String>,
        val missingTrackers: List<String>,
        val contents: BackupContentsSummary? = null,
    )
}
