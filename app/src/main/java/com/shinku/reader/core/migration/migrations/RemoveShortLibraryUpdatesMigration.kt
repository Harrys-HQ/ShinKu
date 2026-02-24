package com.shinku.reader.core.migration.migrations

import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.library.service.LibraryPreferences

class RemoveShortLibraryUpdatesMigration : Migration {
    override val version: Float = 22f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return@withIOContext false
        val updateInterval = libraryPreferences.autoUpdateInterval().get()
        if (updateInterval in listOf(3, 4, 6, 8)) {
            libraryPreferences.autoUpdateInterval().set(12)
        }

        return@withIOContext true
    }
}
