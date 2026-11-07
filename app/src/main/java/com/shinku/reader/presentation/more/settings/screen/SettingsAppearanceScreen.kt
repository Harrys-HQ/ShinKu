package com.shinku.reader.presentation.more.settings.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.domain.ui.model.TabletUiMode
import com.shinku.reader.domain.ui.model.ThemeMode
import com.shinku.reader.domain.ui.model.setAppCompatDelegateThemeMode
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.i18n.pluralStringResource
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.presentation.more.settings.Preference
import com.shinku.reader.presentation.more.settings.screen.appearance.AppLanguageScreen
import com.shinku.reader.presentation.more.settings.widget.AppThemeModePreferenceWidget
import com.shinku.reader.presentation.more.settings.widget.AppThemePreferenceWidget
import com.shinku.reader.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate

object SettingsAppearanceScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }

        return listOf(
            getThemeGroup(uiPreferences = uiPreferences),
            getDisplayGroup(uiPreferences = uiPreferences),
        )
    }

    @Composable
    private fun getThemeGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val themeModePref = uiPreferences.themeMode()
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme()
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled()
        val amoled by amoledPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_theme),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_app_theme),
                ) {
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
                },
                Preference.PreferenceItem.CustomPreference(title = stringResource(MR.strings.appearance_preview)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = SanctuaryShapes.deckCard,
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                        border = SanctuaryBrushes.subtleBorder(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Surface(
                                shape = SanctuaryShapes.tag,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(width = 44.dp, height = 62.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(MR.strings.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Chapter 42 · In progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(MR.strings.appearance_preview_summary),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            Surface(
                                shape = SanctuaryShapes.pill,
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = "Resume",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                },
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.color_effects),
                    subtitle = stringResource(MR.strings.color_effects_scope),
                    onClick = { navigator.push(SettingsColorEffectsScreen) },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = amoledPref,
                    title = stringResource(MR.strings.pref_dark_theme_pure_black),
                    enabled = themeMode != ThemeMode.LIGHT,
                    onValueChanged = {
                        (context as? Activity)?.let { ActivityCompat.recreate(it) }
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getDisplayGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val now = remember { LocalDate.now() }

        val dateFormat by uiPreferences.dateFormat().collectAsState()
        val formattedNow = remember(dateFormat) {
            UiPreferences.dateFormat(dateFormat).format(now)
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_app_language),
                    onClick = { navigator.push(AppLanguageScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.tabletUiMode(),
                    entries = TabletUiMode.entries
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_tablet_ui_mode),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.dateFormat(),
                    entries = DateFormats
                        .associateWith {
                            val formattedDate = UiPreferences.dateFormat(it).format(now)
                            "${it.ifEmpty { stringResource(MR.strings.label_default) }} ($formattedDate)"
                        }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_date_format),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.relativeTime(),
                    title = stringResource(MR.strings.pref_relative_format),
                    subtitle = stringResource(
                        MR.strings.pref_relative_format_summary,
                        stringResource(MR.strings.relative_time_today),
                        formattedNow,
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.imagesInDescription(),
                    title = stringResource(MR.strings.pref_display_images_description),
                ),
            ),
        )
    }

    // SY <--
}

private val DateFormats = listOf(
    "", // Default
    "MM/dd/yy",
    "dd/MM/yy",
    "yyyy-MM-dd",
    "dd MMM yyyy",
    "MMM dd, yyyy",
)
