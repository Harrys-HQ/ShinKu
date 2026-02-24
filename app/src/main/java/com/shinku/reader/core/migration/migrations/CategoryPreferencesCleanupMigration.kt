package com.shinku.reader.core.migration.migrations

import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.download.service.DownloadPreferences
import com.shinku.reader.domain.library.service.LibraryPreferences

class CategoryPreferencesCleanupMigration : Migration {
    override val version: Float = 72f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return@withIOContext false
        val downloadPreferences = migrationContext.get<DownloadPreferences>() ?: return@withIOContext false

        val getCategories = migrationContext.get<GetCategories>() ?: return@withIOContext false
        val allCategories = getCategories.await().map { it.id.toString() }.toSet()

        val defaultCategory = libraryPreferences.defaultCategory().get()
        if (defaultCategory.toString() !in allCategories) {
            libraryPreferences.defaultCategory().delete()
        }

        val categoryPreferences = listOf(
            libraryPreferences.updateCategories(),
            libraryPreferences.updateCategoriesExclude(),
            downloadPreferences.removeExcludeCategories(),
            downloadPreferences.downloadNewChapterCategories(),
            downloadPreferences.downloadNewChapterCategoriesExclude(),
        )
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            val garbageIds = ids.minus(allCategories)
            if (garbageIds.isEmpty()) return@forEach
            preference.set(ids.minus(garbageIds))
        }
        return@withIOContext true
    }
}
