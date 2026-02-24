package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.core.common.preference.getAndSet
import com.shinku.reader.domain.source.model.Source

class ToggleExcludeFromDataSaver(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source) {
        preferences.dataSaverExcludedSources().getAndSet {
            if (source.id.toString() in it) {
                it - source.id.toString()
            } else {
                it + source.id.toString()
            }
        }
    }
}
