package com.shinku.reader.core.migration.migrations

import android.app.Application
import com.shinku.reader.data.sync.SyncDataJob
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext

class SetupSyncDataMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        SyncDataJob.setupTask(context)
        return true
    }
}
