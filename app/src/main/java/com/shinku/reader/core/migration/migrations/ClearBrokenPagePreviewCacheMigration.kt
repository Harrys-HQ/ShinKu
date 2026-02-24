package com.shinku.reader.core.migration.migrations

import android.app.Application
import com.shinku.reader.data.cache.PagePreviewCache
import logcat.LogPriority
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.system.logcat
import java.io.File

class ClearBrokenPagePreviewCacheMigration : Migration {
    override val version: Float = 58f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val pagePreviewCache = migrationContext.get<PagePreviewCache>() ?: return@withIOContext false
        pagePreviewCache.clear()
        File(context.cacheDir, PagePreviewCache.PARAMETER_CACHE_DIRECTORY).listFiles()?.forEach {
            if (it.name == "journal" || it.name.startsWith("journal.")) {
                return@forEach
            }

            try {
                it.delete()
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed to remove file from cache" }
            }
        }

        return@withIOContext true
    }
}
