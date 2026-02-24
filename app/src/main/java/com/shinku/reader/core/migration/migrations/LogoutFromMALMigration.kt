package com.shinku.reader.core.migration.migrations

import com.shinku.reader.data.track.TrackerManager
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext

class LogoutFromMALMigration : Migration {
    override val version: Float = 12f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        // Force MAL log out due to login flow change
        migrationContext.get<TrackerManager>()?.myAnimeList?.logout()

        return@withIOContext true
    }
}
