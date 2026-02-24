package com.shinku.reader.data.backup.restore.restorers

import com.shinku.reader.data.backup.models.BackupSavedSearch
import com.shinku.reader.exh.util.nullIfBlank
import com.shinku.reader.data.DatabaseHandler
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SavedSearchRestorer(
    private val handler: DatabaseHandler = Injekt.get(),
) {
    suspend fun restoreSavedSearches(backupSavedSearches: List<BackupSavedSearch>) {
        if (backupSavedSearches.isEmpty()) return

        val currentSavedSearches = handler.awaitList {
            saved_searchQueries.selectNamesAndSources()
        }

        handler.await {
            backupSavedSearches.filter { backupSavedSearch ->
                currentSavedSearches.none { it.source == backupSavedSearch.source && it.name == backupSavedSearch.name }
            }.forEach {
                saved_searchQueries.insert(
                    source = it.source,
                    name = it.name,
                    query = it.query.nullIfBlank(),
                    filtersJson = it.filterList.nullIfBlank()
                        ?.takeUnless { it == "[]" },
                )
            }
        }
    }
}
