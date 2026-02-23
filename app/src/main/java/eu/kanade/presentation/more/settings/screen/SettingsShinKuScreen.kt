package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.toast
import exh.source.ShinKuPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsShinKuScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.label_shinku_features

    @Composable
    override fun getPreferences(): List<Preference> {
        val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }

        return listOf(
            getGeminiGroup(shinkuPreferences),
            getImmersionGroup(shinkuPreferences),
            getLibraryGroup(libraryPreferences),
        )
    }

    @Composable
    private fun getGeminiGroup(shinkuPreferences: ShinKuPreferences): Preference.PreferenceGroup {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val networkHelper = remember { Injekt.get<NetworkHelper>() }

        return Preference.PreferenceGroup(
            title = "Gemini AI",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.EditTextPreference(
                    preference = shinkuPreferences.geminiApiKey(),
                    title = stringResource(MR.strings.pref_gemini_api_key),
                    subtitle = stringResource(MR.strings.pref_gemini_api_key_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = shinkuPreferences.geminiModel(),
                    title = stringResource(MR.strings.pref_gemini_model),
                    subtitle = stringResource(MR.strings.pref_gemini_model_summary),
                    entries = persistentMapOf(
                        "gemini-2.5-flash" to "Gemini 2.5 Flash",
                        "gemini-2.5-pro" to "Gemini 2.5 Pro",
                        "gemini-3.0-flash" to "Gemini 3.0 Flash",
                        "gemini-3.0-pro" to "Gemini 3.0 Pro",
                        "gemini-3.1-pro" to "Gemini 3.1 Pro (Preview)",
                    ),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.action_test_api_key),
                    onClick = {
                        val apiKey = shinkuPreferences.geminiApiKey().get()
                        if (apiKey.isBlank()) {
                            context.toast("API Key cannot be empty")
                            return@TextPreference
                        }

                        scope.launch {
                            try {
                                val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
                                val request = Request.Builder().url(url).build()
                                val success = withIOContext {
                                    networkHelper.client.newCall(request).execute().use { response ->
                                        response.isSuccessful
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    if (success) {
                                        context.toast(context.stringResource(MR.strings.api_key_test_success))
                                    } else {
                                        context.toast(context.stringResource(MR.strings.api_key_test_failed, "Invalid Key"))
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    context.toast(context.stringResource(MR.strings.api_key_test_failed, e.message ?: "Unknown error"))
                                }
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.aiProTier(),
                    title = stringResource(MR.strings.pref_ai_pro_tier),
                    subtitle = stringResource(MR.strings.pref_ai_pro_tier_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getImmersionGroup(shinkuPreferences: ShinKuPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_immersion_reader),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.liveTranslation(),
                    title = stringResource(MR.strings.pref_live_translation),
                    subtitle = stringResource(MR.strings.pref_live_translation_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getLibraryGroup(libraryPreferences: LibraryPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_library),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.libraryUpdateSpeed(),
                    title = stringResource(MR.strings.pref_library_update_speed),
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.update_speed_standard),
                        1 to stringResource(MR.strings.update_speed_boost),
                        2 to stringResource(MR.strings.update_speed_extreme),
                    ),
                ),
            ),
        )
    }
}
