package com.shinku.reader.core.migration.migrations

import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.exh.source.HBROWSE_SOURCE_ID
import com.shinku.reader.core.migration.MigrateUtils
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.manga.interactor.GetMangaBySource
import com.shinku.reader.domain.manga.model.MangaUpdate

class DelegateHBrowseMigration : Migration {
    override val version: Float = 4f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val getMangaBySource = migrationContext.get<GetMangaBySource>() ?: return@withIOContext false
        val updateManga = migrationContext.get<UpdateManga>() ?: return@withIOContext false
        MigrateUtils.updateSourceId(migrationContext, HBROWSE_SOURCE_ID, 6912)

        // Migrate BHrowse URLs
        val hBrowseManga = getMangaBySource.await(HBROWSE_SOURCE_ID)
        val mangaUpdates = hBrowseManga.map {
            MangaUpdate(it.id, url = it.url + "/c00001/")
        }
        updateManga.awaitAll(mangaUpdates)
        return@withIOContext true
    }
}
