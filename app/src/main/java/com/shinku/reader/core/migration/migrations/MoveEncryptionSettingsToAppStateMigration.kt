package com.shinku.reader.core.migration.migrations

import android.app.Application
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.shinku.reader.util.system.toast
import com.shinku.reader.core.migration.MigrateUtils
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.core.common.preference.PreferenceStore
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.lang.withUIContext

class MoveEncryptionSettingsToAppStateMigration : Migration {
    override val version: Float = 66f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return@withIOContext false
        if (prefs.getBoolean(Preference.privateKey("encrypt_database"), false)) {
            withUIContext {
                context.toast(
                    "Restart the app to load your encrypted library",
                    Toast.LENGTH_LONG,
                )
            }
        }

        val appStatePrefsToReplace = listOf(
            "__PRIVATE_sql_password",
            "__PRIVATE_encrypt_database",
            "__PRIVATE_cbz_password",
        )

        MigrateUtils.replacePreferences(
            preferenceStore = preferenceStore,
            filterPredicate = { it.key in appStatePrefsToReplace },
            newKey = { Preference.appStateKey(it.replace("__PRIVATE_", "").trim()) },
        )

        return@withIOContext true
    }
}
