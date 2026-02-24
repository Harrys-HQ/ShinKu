package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.core.common.preference.getAndSet
import com.shinku.reader.domain.source.model.Source

class ToggleSourcePin(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source) {
        val isPinned = source.id.toString() in preferences.pinnedSources().get()
        preferences.pinnedSources().getAndSet { pinned ->
            if (isPinned) pinned.minus("${source.id}") else pinned.plus("${source.id}")
        }
    }
}
