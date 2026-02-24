package com.shinku.reader.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext

class ChangeThemeModeToUppercaseMigration : Migration {
    override val version: Float = 42f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val uiPreferences = migrationContext.get<UiPreferences>() ?: return@withIOContext false
        if (uiPreferences.themeMode().isSet()) {
            prefs.edit {
                val themeMode = prefs.getString(uiPreferences.themeMode().key(), null) ?: return@edit
                putString(uiPreferences.themeMode().key(), themeMode.uppercase())
            }
        }

        return@withIOContext true
    }
}
