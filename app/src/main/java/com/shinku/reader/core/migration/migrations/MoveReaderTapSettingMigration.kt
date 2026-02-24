package com.shinku.reader.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import com.shinku.reader.ui.reader.setting.ReaderPreferences
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext

class MoveReaderTapSettingMigration : Migration {
    override val version: Float = 32f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val readerPreferences = migrationContext.get<ReaderPreferences>() ?: return@withIOContext false
        val oldReaderTap = prefs.getBoolean("reader_tap", false)
        if (!oldReaderTap) {
            readerPreferences.navigationModePager().set(5)
            readerPreferences.navigationModeWebtoon().set(5)
        }

        return@withIOContext true
    }
}
