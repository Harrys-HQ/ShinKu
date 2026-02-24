package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetSourceCategories(
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<String>> {
        return preferences.sourcesTabCategories().changes().map { it.sortedWith(String.CASE_INSENSITIVE_ORDER) }
    }
}
