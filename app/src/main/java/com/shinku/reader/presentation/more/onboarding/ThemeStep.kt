package com.shinku.reader.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.domain.ui.model.setAppCompatDelegateThemeMode
import com.shinku.reader.presentation.more.settings.widget.AppThemeModePreferenceWidget
import com.shinku.reader.presentation.more.settings.widget.AppThemePreferenceWidget
import com.shinku.reader.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ThemeStep : OnboardingStep {

    override val isComplete: Boolean = true

    private val uiPreferences: UiPreferences = Injekt.get()

    @Composable
    override fun Content() {
        val themeModePref = uiPreferences.themeMode()
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme()
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled()
        val amoled by amoledPref.collectAsState()

        Column {
            AppThemeModePreferenceWidget(
                value = themeMode,
                onItemClick = {
                    themeModePref.set(it)
                    setAppCompatDelegateThemeMode(it)
                },
            )

            AppThemePreferenceWidget(
                value = appTheme,
                amoled = amoled,
                onItemClick = { appThemePref.set(it) },
            )
        }
    }
}
