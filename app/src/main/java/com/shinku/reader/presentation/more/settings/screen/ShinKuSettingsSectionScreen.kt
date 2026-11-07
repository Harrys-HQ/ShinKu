package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.more.settings.Preference
import dev.icerock.moko.resources.StringResource

enum class ShinKuSettingsSection(val titleRes: StringResource, val summaryRes: StringResource, val icon: ImageVector) {
    AI(MR.strings.shinku_ai_translation, MR.strings.shinku_ai_translation_summary, Icons.Outlined.AutoAwesome),
    READING(MR.strings.shinku_reading_appearance, MR.strings.shinku_reading_appearance_summary, Icons.Outlined.MenuBook),
    PERFORMANCE(MR.strings.shinku_performance, MR.strings.shinku_performance_summary, Icons.Outlined.Speed),
    MAINTENANCE(MR.strings.shinku_maintenance, MR.strings.shinku_maintenance_summary, Icons.Outlined.Build),
    ADVANCED(MR.strings.shinku_advanced, MR.strings.shinku_advanced_summary, Icons.Outlined.Tune),
}

data class ShinKuSettingsSectionScreen(val section: ShinKuSettingsSection) : SearchableSettings {
    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = section.titleRes

    @Composable
    override fun getPreferences(): List<Preference> = SettingsShinKuSettingsScreen.getSectionPreferences(section)
}
