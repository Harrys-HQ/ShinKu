package com.shinku.reader.core.migration.migrations

import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.data.DatabaseHandler

class DeleteOldMangaDexTracksMigration : Migration {
    override val version: Float = 17f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val handler = migrationContext.get<DatabaseHandler>() ?: return@withIOContext false
        // Delete old mangadex trackers
        handler.await { ehQueries.deleteBySyncId(6) }
        return@withIOContext true
    }
}
