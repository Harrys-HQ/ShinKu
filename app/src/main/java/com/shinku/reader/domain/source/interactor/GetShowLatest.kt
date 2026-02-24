package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.ui.UiPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetShowLatest(
    private val preferences: UiPreferences,
) {

    fun subscribe(hasSmartSearchConfig: Boolean): Flow<Boolean> {
        return preferences.useNewSourceNavigation().changes()
            .map {
                !hasSmartSearchConfig && !it
            }
    }
}
