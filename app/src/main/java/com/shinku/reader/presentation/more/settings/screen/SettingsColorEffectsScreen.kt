package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.more.settings.Preference
import kotlinx.collections.immutable.persistentListOf
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsColorEffectsScreen : SearchableSettings {
    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.color_effects

    @Composable
    override fun getPreferences(): List<Preference> {
        val preferences = remember { Injekt.get<ShinKuPreferences>() }
        return listOf(
            Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.color_effects_scope)),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_reader),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.moodLighting(),
                        title = stringResource(MR.strings.reader_mood_lighting),
                        subtitle = stringResource(MR.strings.reader_mood_lighting_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_library),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.dynamicLibraryTheme(),
                        title = stringResource(MR.strings.cover_based_app_theme),
                        subtitle = stringResource(MR.strings.cover_based_app_theme_summary),
                    ),
                ),
            ),
        )
    }
}
