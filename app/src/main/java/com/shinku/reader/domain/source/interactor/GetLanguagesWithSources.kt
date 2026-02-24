package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.util.system.LocaleHelper
import com.shinku.reader.exh.source.BlacklistedSources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.shinku.reader.domain.source.model.Source
import com.shinku.reader.domain.source.repository.SourceRepository
import java.util.SortedMap

class GetLanguagesWithSources(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<SortedMap<String, List<Source>>> {
        return combine(
            preferences.enabledLanguages().changes(),
            preferences.disabledSources().changes(),
            repository.getOnlineSources(),
        ) { enabledLanguage, disabledSource, onlineSources ->
            val sortedSources = onlineSources.filterNot { it.id in BlacklistedSources.HIDDEN_SOURCES }.sortedWith(
                compareBy<Source> { it.id.toString() in disabledSource }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
            )

            sortedSources
                .groupBy { it.lang }
                .toSortedMap(
                    compareBy<String> { it !in enabledLanguage }.then(LocaleHelper.comparator),
                )
        }
    }
}
