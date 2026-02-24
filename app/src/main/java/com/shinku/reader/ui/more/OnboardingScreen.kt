package com.shinku.reader.ui.more

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.presentation.more.onboarding.OnboardingScreen
import com.shinku.reader.presentation.more.settings.screen.SearchableSettings
import com.shinku.reader.presentation.more.settings.screen.SettingsDataScreen
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.ui.setting.SettingsScreen
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class OnboardingScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val shownOnboardingFlow by basePreferences.shownOnboardingFlow().collectAsState()

        val finishOnboarding: () -> Unit = {
            basePreferences.shownOnboardingFlow().set(true)
            navigator.pop()
        }

        val restoreSettingKey = stringResource(SettingsDataScreen.restorePreferenceKeyString)

        BackHandler(enabled = !shownOnboardingFlow) {
            // Prevent exiting if onboarding hasn't been completed
        }

        OnboardingScreen(
            onComplete = finishOnboarding,
            onRestoreBackup = {
                finishOnboarding()
                SearchableSettings.highlightKey = restoreSettingKey
                navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage))
            },
        )
    }
}
