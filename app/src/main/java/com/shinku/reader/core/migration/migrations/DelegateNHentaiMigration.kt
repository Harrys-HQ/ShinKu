package com.shinku.reader.core.migration.migrations

import eu.kanade.tachiyomi.source.online.all.NHentai
import com.shinku.reader.core.migration.MigrateUtils
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext

class DelegateNHentaiMigration : Migration {
    override val version: Float = 6f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        MigrateUtils.updateSourceId(migrationContext, NHentai.otherId, 6907)

        return@withIOContext true
    }
}
