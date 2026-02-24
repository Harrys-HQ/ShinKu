package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.core.common.preference.getAndSet
import com.shinku.reader.domain.source.model.Source

class SetSourceCategories(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source, sourceCategories: List<String>) {
        val sourceIdString = source.id.toString()
        preferences.sourcesTabSourcesInCategories().getAndSet { sourcesInCategories ->
            val currentSourceCategories = sourcesInCategories.filterNot {
                it.substringBefore('|') == sourceIdString
            }
            val newSourceCategories = currentSourceCategories + sourceCategories.map {
                "$sourceIdString|$it"
            }
            newSourceCategories.toSet()
        }
    }
}
