package com.shinku.reader.data.backup.create.creators

import com.shinku.reader.data.backup.models.BackupCategory
import com.shinku.reader.data.backup.models.backupCategoryMapper
import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.category.model.Category
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoriesBackupCreator(
    private val getCategories: GetCategories = Injekt.get(),
) {

    suspend operator fun invoke(): List<BackupCategory> {
        return getCategories.await()
            .filterNot(Category::isSystemCategory)
            .map(backupCategoryMapper)
    }
}
