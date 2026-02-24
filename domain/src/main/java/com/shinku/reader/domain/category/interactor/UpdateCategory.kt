package com.shinku.reader.domain.category.interactor

import com.shinku.reader.core.common.util.lang.withNonCancellableContext
import com.shinku.reader.domain.category.model.CategoryUpdate
import com.shinku.reader.domain.category.repository.CategoryRepository

class UpdateCategory(
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await(payload: CategoryUpdate): Result = withNonCancellableContext {
        try {
            categoryRepository.updatePartial(payload)
            Result.Success
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class Error(val error: Exception) : Result
    }
}
