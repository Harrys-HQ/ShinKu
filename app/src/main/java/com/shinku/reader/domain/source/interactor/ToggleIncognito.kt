package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.core.common.preference.getAndSet

class ToggleIncognito(
    private val preferences: SourcePreferences,
) {
    fun await(extensions: String, enable: Boolean) {
        preferences.incognitoExtensions().getAndSet {
            if (enable) it.plus(extensions) else it.minus(extensions)
        }
    }
}
