package com.shinku.reader.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.shinku.reader.data.track.TrackerManager
import com.shinku.reader.util.system.workManager
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.preference.PreferenceStore
import com.shinku.reader.core.common.preference.TriState
import com.shinku.reader.core.common.preference.getEnum
import com.shinku.reader.core.common.util.lang.withIOContext

class RemoveUpdateCheckerJobsMigration : Migration {
    override val version: Float = 52f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return@withIOContext false
        val trackerManager = migrationContext.get<TrackerManager>() ?: return@withIOContext false
        // Removed background jobs
        context.workManager.cancelAllWorkByTag("UpdateChecker")
        context.workManager.cancelAllWorkByTag("ExtensionUpdate")
        prefs.edit {
            remove("automatic_ext_updates")
        }
        val prefKeys = listOf(
            "pref_filter_library_downloaded",
            "pref_filter_library_unread",
            "pref_filter_library_started",
            "pref_filter_library_bookmarked",
            "pref_filter_library_completed",
            "pref_filter_library_lewd",
        ) + trackerManager.trackers.map { "pref_filter_library_tracked_${it.id}" }

        prefKeys.forEach { key ->
            val pref = prefs.getInt(key, 0)
            prefs.edit {
                remove(key)

                val newValue = when (pref) {
                    1 -> TriState.ENABLED_IS
                    2 -> TriState.ENABLED_NOT
                    else -> TriState.DISABLED
                }

                preferenceStore.getEnum("${key}_v2", TriState.DISABLED).set(newValue)
            }
        }

        return@withIOContext true
    }
}
