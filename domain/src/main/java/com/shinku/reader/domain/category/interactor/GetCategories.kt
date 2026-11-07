package com.shinku.reader.domain.category.interactor

import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.category.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategories(
    private val categoryRepository: CategoryRepository,
) {

    fun subscribe(): Flow<List<Category>> {
        return categoryRepository.getAllAsFlow()
    }

    fun subscribeLibraryCounts(): Flow<Map<Long, Long>> = categoryRepository.getLibraryCountsAsFlow()

    fun subscribe(mangaId: Long): Flow<List<Category>> {
        return categoryRepository.getCategoriesByMangaIdAsFlow(mangaId)
    }

    suspend fun await(): List<Category> {
        return categoryRepository.getAll()
    }

    suspend fun await(mangaId: Long): List<Category> {
        return categoryRepository.getCategoriesByMangaId(mangaId)
    }
}
