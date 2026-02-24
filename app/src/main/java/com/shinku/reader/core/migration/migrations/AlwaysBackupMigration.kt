package com.shinku.reader.core.migration.migrations

import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.backup.service.BackupPreferences

class AlwaysBackupMigration : Migration {
    override val version: Float = 40f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val backupPreferences = migrationContext.get<BackupPreferences>() ?: return@withIOContext false
        if (backupPreferences.backupInterval().get() == 0) {
            backupPreferences.backupInterval().set(12)
        }

        return@withIOContext true
    }
}
