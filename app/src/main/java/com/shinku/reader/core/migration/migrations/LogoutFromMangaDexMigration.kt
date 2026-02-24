package com.shinku.reader.core.migration.migrations

import com.shinku.reader.data.track.TrackerManager
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext

class LogoutFromMangaDexMigration : Migration {
    override val version: Float = 45f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        // Force MangaDex log out due to login flow change
        migrationContext.get<TrackerManager>()?.mdList?.logout()

        return@withIOContext true
    }
}
