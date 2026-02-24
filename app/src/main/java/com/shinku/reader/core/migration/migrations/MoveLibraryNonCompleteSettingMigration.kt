package com.shinku.reader.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.preference.minusAssign
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.library.service.LibraryPreferences

class MoveLibraryNonCompleteSettingMigration : Migration {
    override val version: Float = 23f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return@withIOContext false
        val oldUpdateOngoingOnly = prefs.getBoolean("pref_update_only_non_completed_key", true)
        if (!oldUpdateOngoingOnly) {
            libraryPreferences.autoUpdateMangaRestrictions() -= LibraryPreferences.MANGA_NON_COMPLETED
        }

        return@withIOContext true
    }
}
