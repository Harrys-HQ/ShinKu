package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.core.common.preference.getAndSet
import com.shinku.reader.core.common.preference.minusAssign

class DeleteSourceCategory(private val preferences: SourcePreferences) {

    fun await(category: String) {
        preferences.sourcesTabSourcesInCategories().getAndSet { sourcesInCategories ->
            sourcesInCategories.filterNot { it.substringAfter("|") == category }.toSet()
        }
        preferences.sourcesTabCategories() -= category
    }
}
