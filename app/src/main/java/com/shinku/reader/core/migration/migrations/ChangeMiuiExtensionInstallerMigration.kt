package com.shinku.reader.core.migration.migrations

import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.util.system.DeviceUtil
import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.util.lang.withIOContext

class ChangeMiuiExtensionInstallerMigration : Migration {
    override val version: Float = 27f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val basePreferences = migrationContext.get<BasePreferences>() ?: return@withIOContext false
        if (
            DeviceUtil.isMiui &&
            basePreferences.extensionInstaller().get() == BasePreferences.ExtensionInstaller
                .PACKAGEINSTALLER
        ) {
            basePreferences.extensionInstaller().set(BasePreferences.ExtensionInstaller.LEGACY)
        }

        return@withIOContext true
    }
}
