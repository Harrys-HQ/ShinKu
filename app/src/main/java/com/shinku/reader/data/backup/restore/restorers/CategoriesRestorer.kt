package com.shinku.reader.data.backup.restore.restorers

import com.shinku.reader.data.backup.models.BackupCategory
import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoriesRestorer(
    private val handler: DatabaseHandler = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) {

    suspend operator fun invoke(backupCategories: List<BackupCategory>) {
        if (backupCategories.isNotEmpty()) {
            val dbCategories = getCategories.await()
            val dbCategoriesByName = dbCategories.associateBy { it.name }
            var nextOrder = dbCategories.maxOfOrNull { it.order }?.plus(1) ?: 0

            val categories = backupCategories
                .sortedBy { it.order }
                .map {
                    val dbCategory = dbCategoriesByName[it.name]
                    if (dbCategory != null) return@map dbCategory
                    val order = nextOrder++
                    handler.awaitOneExecutable {
                        categoriesQueries.insert(it.name, order, it.flags)
                        categoriesQueries.selectLastInsertedRowId()
                    }
                        .let { id -> it.toCategory(id).copy(order = order) }
                }

            libraryPreferences.categorizedDisplaySettings().set(
                (dbCategories + categories)
                    .distinctBy { it.flags }
                    .size > 1,
            )
        }
    }
}
