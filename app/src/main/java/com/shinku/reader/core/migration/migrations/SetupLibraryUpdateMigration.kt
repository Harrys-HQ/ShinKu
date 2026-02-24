package com.shinku.reader.core.migration.migrations

import android.app.Application
import com.shinku.reader.data.library.LibraryUpdateJob
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext

class SetupLibraryUpdateMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        LibraryUpdateJob.setupTask(context)
        return true
    }
}
