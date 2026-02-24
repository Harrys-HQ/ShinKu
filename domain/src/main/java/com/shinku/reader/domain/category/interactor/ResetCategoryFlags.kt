package com.shinku.reader.domain.category.interactor

import com.shinku.reader.domain.category.repository.CategoryRepository
import com.shinku.reader.domain.library.model.plus
import com.shinku.reader.domain.library.service.LibraryPreferences

class ResetCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await() {
        val sort = preferences.sortingMode().get()
        categoryRepository.updateAllFlags(sort.type + sort.direction)
    }
}
