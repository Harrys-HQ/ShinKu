package com.shinku.reader.exh.source

import com.shinku.reader.core.common.preference.PreferenceStore

class ShinKuPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun geminiApiKey() = preferenceStore.getString("pref_gemini_api_key", "")

    fun geminiModel() = preferenceStore.getString("pref_gemini_model", "gemini-2.0-flash-exp")

    fun aiProTier() = preferenceStore.getBoolean("pref_ai_pro_tier", false)

    fun liveTranslation() = preferenceStore.getBoolean("pref_live_translation", false)
}
