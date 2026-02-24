package com.shinku.reader.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.core.migration.MigrateUtils
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.core.common.preference.PreferenceStore
import com.shinku.reader.core.common.preference.getAndSet
import com.shinku.reader.core.common.util.lang.withIOContext

class MoveExtensionRepoSettingsMigration : Migration {
    override val version: Float = 60f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return@withIOContext false
        val sourcePreferences = migrationContext.get<SourcePreferences>() ?: return@withIOContext false
        sourcePreferences.extensionRepos().getAndSet {
            it.map { "https://raw.githubusercontent.com/$it/repo" }.toSet()
        }
        MigrateUtils.replacePreferences(
            preferenceStore = preferenceStore,
            filterPredicate = { it.key.startsWith("pref_mangasync_") || it.key.startsWith("track_token_") },
            newKey = { Preference.privateKey(it) },
        )
        prefs.edit {
            remove(Preference.appStateKey("trusted_signatures"))
        }

        return@withIOContext true
    }
}
