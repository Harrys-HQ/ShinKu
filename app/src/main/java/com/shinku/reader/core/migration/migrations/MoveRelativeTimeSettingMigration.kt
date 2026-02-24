package com.shinku.reader.core.migration.migrations

import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.preference.PreferenceStore
import com.shinku.reader.core.common.util.lang.withIOContext

class MoveRelativeTimeSettingMigration : Migration {
    override val version: Float = 57f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return@withIOContext false
        val uiPreferences = migrationContext.get<UiPreferences>() ?: return@withIOContext false
        val pref = preferenceStore.getInt("relative_time", 7)
        if (pref.get() == 0) {
            uiPreferences.relativeTime().set(false)
        }

        return@withIOContext true
    }
}
