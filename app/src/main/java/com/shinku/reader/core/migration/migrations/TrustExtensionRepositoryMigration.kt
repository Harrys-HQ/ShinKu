package com.shinku.reader.core.migration.migrations

import com.shinku.reader.domain.source.service.SourcePreferences
import logcat.LogPriority
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.domain.extensionrepo.exception.SaveExtensionRepoException
import com.shinku.reader.domain.extensionrepo.repository.ExtensionRepoRepository
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.system.logcat

class TrustExtensionRepositoryMigration : Migration {
    override val version: Float = 67f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val sourcePreferences = migrationContext.get<SourcePreferences>() ?: return@withIOContext false
        val extensionRepositoryRepository =
            migrationContext.get<ExtensionRepoRepository>() ?: return@withIOContext false
        for ((index, source) in sourcePreferences.extensionRepos().get().withIndex()) {
            try {
                extensionRepositoryRepository.upsertRepo(
                    source,
                    "Repo #${index + 1}",
                    null,
                    source,
                    "NOFINGERPRINT-${index + 1}",
                )
            } catch (e: SaveExtensionRepoException) {
                logcat(LogPriority.ERROR, e) { "Error Migrating Extension Repo with baseUrl: $source" }
            }
        }
        sourcePreferences.extensionRepos().delete()
        return@withIOContext true
    }
}
