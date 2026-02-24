package com.shinku.reader.core.migration.migrations

import com.shinku.reader.ui.reader.setting.ReaderPreferences
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext

class RemoveOldReaderThemeMigration : Migration {
    override val version: Float = 18f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val readerPreferences = migrationContext.get<ReaderPreferences>() ?: return@withIOContext false
        val readerTheme = readerPreferences.readerTheme().get()
        if (readerTheme == 4) {
            readerPreferences.readerTheme().set(3)
        }

        return@withIOContext true
    }
}
