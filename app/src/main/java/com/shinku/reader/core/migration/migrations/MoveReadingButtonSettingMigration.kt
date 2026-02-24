package com.shinku.reader.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.library.service.LibraryPreferences

class MoveReadingButtonSettingMigration : Migration {
    override val version: Float = 43f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return@withIOContext false
        if (prefs.getBoolean("start_reading_button", false)) {
            libraryPreferences.showContinueReadingButton().set(true)
        }

        return@withIOContext true
    }
}
