package com.shinku.reader.core.migration.migrations

import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.library.service.LibraryPreferences

class RemoveShorterLibraryUpdatesMigration : Migration {
    override val version: Float = 18f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return@withIOContext false
        val updateInterval = libraryPreferences.autoUpdateInterval().get()
        if (updateInterval == 1 || updateInterval == 2) {
            libraryPreferences.autoUpdateInterval().set(3)
        }

        return@withIOContext true
    }
}
