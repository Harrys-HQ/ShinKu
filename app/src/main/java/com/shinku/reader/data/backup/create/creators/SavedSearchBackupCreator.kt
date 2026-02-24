package com.shinku.reader.data.backup.create.creators

import com.shinku.reader.data.backup.models.BackupSavedSearch
import com.shinku.reader.data.backup.models.backupSavedSearchMapper
import com.shinku.reader.data.DatabaseHandler
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SavedSearchBackupCreator(
    private val handler: DatabaseHandler = Injekt.get(),
) {

    suspend operator fun invoke(): List<BackupSavedSearch> {
        return handler.awaitList { saved_searchQueries.selectAll(backupSavedSearchMapper) }
    }
}
