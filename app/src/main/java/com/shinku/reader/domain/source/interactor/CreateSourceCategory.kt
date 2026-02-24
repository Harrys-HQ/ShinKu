package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.core.common.preference.plusAssign

class CreateSourceCategory(private val preferences: SourcePreferences) {

    fun await(category: String): Result {
        if (category.contains("|")) {
            return Result.InvalidName
        }

        // Create category.
        preferences.sourcesTabCategories() += category

        return Result.Success
    }

    sealed class Result {
        data object InvalidName : Result()
        data object Success : Result()
    }
}
