package com.shinku.reader.exh.source

import com.shinku.reader.core.common.preference.PreferenceStore

class ShinKuPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun geminiApiKey() = preferenceStore.getString("pref_gemini_api_key", "")

    fun geminiModel() = preferenceStore.getString("pref_gemini_model", "gemini-3.5-flash")

    fun translationTargetLanguage() = preferenceStore.getString("pref_translation_target_language", "English")

    fun translationSourceLanguage() = preferenceStore.getString("pref_translation_source_language", "Auto-Detect")

    fun aiProTier() = preferenceStore.getBoolean("pref_ai_pro_tier", false)

    fun liveTranslation() = preferenceStore.getBoolean("pref_live_translation", false)

    fun atmosphericAudio() = preferenceStore.getBoolean("pref_atmospheric_audio", false)

    fun moodLighting() = preferenceStore.getBoolean("pref_mood_lighting", false)

    fun backdropBlur() = preferenceStore.getBoolean("pref_backdrop_blur", false)

    fun hapticFeedback() = preferenceStore.getBoolean("pref_haptic_feedback", false)

    fun predictiveLoading() = preferenceStore.getBoolean("pref_predictive_loading", false)

    fun aiUpscaling() = preferenceStore.getBoolean("pref_ai_upscaling", false)

    fun adaptiveNightRead() = preferenceStore.getBoolean("pref_adaptive_night_read", false)
}
