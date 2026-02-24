package com.shinku.reader.exh.pref

import com.shinku.reader.core.common.preference.PreferenceStore

class DelegateSourcePreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun delegateSources() = preferenceStore.getBoolean("eh_delegate_sources", true)

    fun useJapaneseTitle() = preferenceStore.getBoolean("use_jp_title", false)
}
