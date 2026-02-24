package com.shinku.reader.core.migration.migrations

import com.shinku.reader.core.migration.Migration
import com.shinku.reader.core.migration.MigrationContext
import com.shinku.reader.core.common.preference.getAndSet
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.library.service.LibraryPreferences

class RemoveBatteryNotLowRestrictionMigration : Migration {
    override val version: Float = 56f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return@withIOContext false
        val pref = libraryPreferences.autoUpdateDeviceRestrictions()
        if (pref.isSet() && "battery_not_low" in pref.get()) {
            pref.getAndSet { it - "battery_not_low" }
        }

        return@withIOContext true
    }
}
