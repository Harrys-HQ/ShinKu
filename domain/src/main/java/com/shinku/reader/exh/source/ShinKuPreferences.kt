package com.shinku.reader.exh.source

import com.shinku.reader.core.common.preference.PreferenceStore

class ShinKuPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun geminiApiKey() = preferenceStore.getString("pref_gemini_api_key", "")

    fun geminiModel() = preferenceStore.getString("pref_gemini_model", "auto")

    fun translationTargetLanguage() = preferenceStore.getString("pref_translation_target_language", "English")

    fun translationSourceLanguage() = preferenceStore.getString("pref_translation_source_language", "Auto-Detect")

    fun liveTranslation() = preferenceStore.getBoolean("pref_live_translation", false)

    fun atmosphericAudio() = preferenceStore.getBoolean("pref_atmospheric_audio", false)

    fun atmosphericAudioVolume() = preferenceStore.getInt("pref_atmospheric_audio_volume", 30)

    fun atmosphericAudioHostUrl() = preferenceStore.getString(
        "pref_atmospheric_audio_host_url",
        "https://raw.githubusercontent.com/Harrys-HQ/ShinKu-Assets/main/audio/atmosphere/",
    )

    fun atmosphericAudioOverride() = preferenceStore.getString("pref_atmospheric_audio_override", "auto")

    fun moodLighting() = preferenceStore.getBoolean("pref_mood_lighting", false)

    fun backdropBlur() = preferenceStore.getBoolean("pref_backdrop_blur", false)

    fun hapticFeedback() = preferenceStore.getBoolean("pref_haptic_feedback", false)

    fun predictiveLoading() = preferenceStore.getBoolean("pref_predictive_loading", false)

    fun aiUpscaling() = preferenceStore.getBoolean("pref_ai_upscaling", false)

    fun adaptiveNightRead() = preferenceStore.getBoolean("pref_adaptive_night_read", false)

    fun aiEngineProvider() = preferenceStore.getString("pref_ai_engine_provider", "auto")

    fun openRouterApiKey() = preferenceStore.getString("pref_openrouter_api_key", "")

    fun openRouterModel() = preferenceStore.getString("pref_openrouter_model", "auto")

    fun openAiApiKey() = preferenceStore.getString("pref_openai_api_key", "")

    fun openAiBaseUrl() = preferenceStore.getString("pref_openai_base_url", "https://api.openai.com/v1")

    fun openAiModel() = preferenceStore.getString("pref_openai_model", "auto")

    fun translationEngine() = preferenceStore.getString("pref_translation_engine", "auto")

    fun readerNickname() = preferenceStore.getString("pref_reader_nickname", "")

    fun readerAvatarUri() = preferenceStore.getString("pref_reader_avatar_uri", "")

    fun equippedBadgeTitle() = preferenceStore.getString("pref_equipped_badge_title", "")

    fun streakShieldCount() = preferenceStore.getInt("pref_streak_shield_count", 1)

    fun feedSourceFilter() = preferenceStore.getString("pref_feed_source_filter", "all")

    fun feedLanguageFilter() = preferenceStore.getString("pref_feed_language_filter", "en")

    fun feedShowSourceFeeds() = preferenceStore.getBoolean("pref_feed_show_source_feeds", true)

    fun showHeroJumpBack() = preferenceStore.getBoolean("pref_show_hero_jump_back", true)

    fun autoHideBottomBar() = preferenceStore.getBoolean("pref_auto_hide_bottom_bar", true)

    fun autoTranslateChapterOnOpen() = preferenceStore.getBoolean("pref_auto_translate_chapter_on_open", false)

    fun dynamicLibraryTheme() = preferenceStore.getBoolean("pref_dynamic_library_theme", true)

    fun dynamicLibrarySeedColor() = preferenceStore.getInt("pref_dynamic_library_seed_color", 0)

    fun isAiConfigured(): Boolean {
        return when (aiEngineProvider().get()) {
            "openrouter" -> openRouterApiKey().get().isNotBlank()
            "deepseek", "groq", "openai" -> openAiApiKey().get().isNotBlank()
            "custom" -> openAiBaseUrl().get().isNotBlank()
            "cloud" -> geminiApiKey().get().isNotBlank()
            else -> geminiApiKey().get().isNotBlank() ||
                openRouterApiKey().get().isNotBlank() ||
                openAiApiKey().get().isNotBlank()
        }
    }
}
