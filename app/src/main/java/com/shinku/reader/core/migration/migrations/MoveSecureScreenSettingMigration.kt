package com.shinku.reader.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import com.shinku.reader.core.security.SecurityPreferences
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext

class MoveSecureScreenSettingMigration : Migration {
    override val version: Float = 27f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val securityPreferences = migrationContext.get<SecurityPreferences>() ?: return@withIOContext false
        val oldSecureScreen = prefs.getBoolean("secure_screen", false)
        if (oldSecureScreen) {
            securityPreferences.secureScreen().set(SecurityPreferences.SecureScreenMode.ALWAYS)
        }

        return@withIOContext true
    }
}
