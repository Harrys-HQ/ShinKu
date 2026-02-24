package com.shinku.reader.core.security

import com.shinku.reader.core.common.preference.PreferenceStore

class PrivacyPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun crashlytics() = preferenceStore.getBoolean("crashlytics", true)

    fun analytics() = preferenceStore.getBoolean("analytics", true)
}
