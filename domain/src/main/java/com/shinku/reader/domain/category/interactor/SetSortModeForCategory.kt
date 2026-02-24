package com.shinku.reader.domain.category.interactor

import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.category.model.CategoryUpdate
import com.shinku.reader.domain.category.repository.CategoryRepository
import com.shinku.reader.domain.library.model.LibraryGroup
import com.shinku.reader.domain.library.model.LibrarySort
import com.shinku.reader.domain.library.model.plus
import com.shinku.reader.domain.library.service.LibraryPreferences
import kotlin.random.Random

class SetSortModeForCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await(categoryId: Long?, type: LibrarySort.Type, direction: LibrarySort.Direction) {
        // SY -->
        if (preferences.groupLibraryBy().get() != LibraryGroup.BY_DEFAULT) {
            preferences.sortingMode().set(LibrarySort(type, direction))
            return
        }
        // SY <--
        val category = categoryId?.let { categoryRepository.get(it) }
        val flags = (category?.flags ?: 0) + type + direction
        if (type == LibrarySort.Type.Random) {
            preferences.randomSortSeed().set(Random.nextInt())
        }
        if (category != null && preferences.categorizedDisplaySettings().get()) {
            categoryRepository.updatePartial(
                CategoryUpdate(
                    id = category.id,
                    flags = flags,
                ),
            )
        } else {
            preferences.sortingMode().set(LibrarySort(type, direction))
            categoryRepository.updateAllFlags(flags)
        }
    }

    suspend fun await(
        category: Category?,
        type: LibrarySort.Type,
        direction: LibrarySort.Direction,
    ) {
        await(category?.id, type, direction)
    }
}
