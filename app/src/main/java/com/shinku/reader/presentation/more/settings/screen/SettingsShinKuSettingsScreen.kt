package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.presentation.more.settings.Preference
import eu.kanade.tachiyomi.network.NetworkHelper
import com.shinku.reader.util.system.toast
import com.shinku.reader.exh.source.ShinKuPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.ui.reader.setting.ReaderPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.data.category.SmartCategorizerJob
import com.shinku.reader.data.ai.AiClusteringJob
import com.shinku.reader.data.ai.MangaEmbeddingJob
import com.shinku.reader.ui.browse.migration.dead.DeadSourceScannerScreen
import com.shinku.reader.ui.browse.migration.failed.FailedUpdatesMigrationScreen
import com.shinku.reader.ui.sourcehealth.SourceHealthScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

import com.shinku.reader.core.security.SecurityPreferences
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.domain.ui.model.ThemeMode
import com.shinku.reader.domain.ui.model.setAppCompatDelegateThemeMode
import com.shinku.reader.presentation.more.settings.screen.appearance.AppLanguageScreen
import com.shinku.reader.presentation.more.settings.widget.AppThemeModePreferenceWidget
import com.shinku.reader.presentation.more.settings.widget.AppThemePreferenceWidget
import com.shinku.reader.exh.debug.SettingsDebugScreen
import androidx.compose.runtime.getValue
import com.shinku.reader.presentation.core.util.collectAsState
import androidx.core.text.HtmlCompat
import com.shinku.reader.exh.util.toAnnotatedString
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.shinku.reader.presentation.core.i18n.pluralStringResource

import com.shinku.reader.ui.reader.setting.ReaderBottomButton
import com.shinku.reader.ui.reader.viewer.pager.PagerConfig

import com.shinku.reader.exh.pref.DelegateSourcePreferences
import eu.kanade.tachiyomi.source.AndroidSourceManager
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableList

object SettingsShinKuSettingsScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.action_configure_features

    @Composable
    override fun getPreferences(): List<Preference> {
        val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val readerPreferences = remember { Injekt.get<ReaderPreferences>() }
        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val securityPreferences = remember { Injekt.get<SecurityPreferences>() }
        val delegateSourcePreferences = remember { Injekt.get<DelegateSourcePreferences>() }

        return listOf(
            getMaintenanceToolsGroup(),
            getGeminiGroup(shinkuPreferences),
            getPerformanceGroup(shinkuPreferences, basePreferences),
            getImmersionGroup(shinkuPreferences, readerPreferences),
            getReaderEnhancementsGroup(readerPreferences),
            getNavigationGroup(uiPreferences),
            getInterfaceGroup(uiPreferences),
            getBrowsingGroup(sourcePreferences, uiPreferences),
            getLibraryGroup(libraryPreferences),
            getSecurityDebugGroup(securityPreferences, sourcePreferences, delegateSourcePreferences),
            getAdvancedFeaturesGroup(readerPreferences, sourcePreferences),
        )
    }

    @Composable
    private fun getMaintenanceToolsGroup(): Preference.PreferenceGroup {



        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        return Preference.PreferenceGroup(
            title = "Maintenance & Tools",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_smart_categorizer),
                    subtitle = stringResource(MR.strings.pref_smart_categorizer_summary),
                    onClick = { SmartCategorizerJob.startNow(context) },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "AI Categorizer (Experimental)",
                    subtitle = "Use AI to group library into 'vibe-based' categories",
                    onClick = {
                        MangaEmbeddingJob.startNow(context)
                        AiClusteringJob.startNow(context)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.dead_source_scanner_title),
                    subtitle = stringResource(MR.strings.dead_source_scanner_summary),
                    onClick = { navigator.push(DeadSourceScannerScreen()) },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.failed_updates_migration_title),
                    subtitle = stringResource(MR.strings.failed_updates_migration_summary),
                    onClick = { navigator.push(FailedUpdatesMigrationScreen()) },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Source Health",
                    subtitle = "Monitor and rank source stability",
                    onClick = { navigator.push(SourceHealthScreen()) },
                ),
            ),
        )
    }

    @Composable
    private fun getPerformanceGroup(
        shinkuPreferences: ShinKuPreferences,
        basePreferences: BasePreferences
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "Performance & AI",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = basePreferences.highRefreshRate(),
                    title = stringResource(MR.strings.pref_high_refresh_rate),
                    subtitle = stringResource(MR.strings.pref_high_refresh_rate_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = basePreferences.performanceMode(),
                    title = stringResource(MR.strings.pref_performance_mode),
                    entries = persistentMapOf(
                        BasePreferences.PerformanceMode.STANDARD.value to stringResource(MR.strings.pref_performance_mode_standard),
                        BasePreferences.PerformanceMode.E_INK.value to stringResource(MR.strings.pref_performance_mode_eink),
                        BasePreferences.PerformanceMode.LOW_POWER.value to stringResource(MR.strings.pref_performance_mode_low_power),
                    ),
                    subtitleProvider = { value, entries ->
                        when (value) {
                            BasePreferences.PerformanceMode.E_INK.value -> stringResource(MR.strings.pref_performance_mode_eink_summary)
                            BasePreferences.PerformanceMode.LOW_POWER.value -> stringResource(MR.strings.pref_performance_mode_low_power_summary)
                            else -> entries[value].orEmpty()
                        }
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.aiUpscaling(),
                    title = stringResource(SYMR.strings.pref_ai_upscaling),
                    subtitle = stringResource(SYMR.strings.pref_ai_upscaling_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.predictiveLoading(),
                    title = stringResource(SYMR.strings.pref_predictive_loading),
                    subtitle = stringResource(SYMR.strings.pref_predictive_loading_summary),
                ),
            ),
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
                        "gemini-2.0-flash" to "Gemini 2.0 Flash",
                        "gemini-2.0-pro" to "Gemini 2.0 Pro",
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
    private fun getImmersionGroup(
        shinkuPreferences: ShinKuPreferences,
        readerPreferences: ReaderPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_immersion_reader),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.liveTranslation(),
                    title = stringResource(MR.strings.pref_live_translation),
                    subtitle = stringResource(MR.strings.pref_live_translation_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.atmosphericAudio(),
                    title = stringResource(SYMR.strings.pref_atmospheric_audio),
                    subtitle = stringResource(SYMR.strings.pref_atmospheric_audio_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.moodLighting(),
                    title = "Mood Lighting",
                    subtitle = "Dynamic background colors that match the current page's artwork.",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.backdropBlur(),
                    title = "Backdrop Blur",
                    subtitle = "Apply a soft blur to background elements for better focus.",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.hapticFeedback(),
                    title = "Haptic Feedback",
                    subtitle = "Subtle vibrations during page turns and interface interactions.",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.guidedView(),
                    title = "Guided View (Smart Panel Zoom)",
                    subtitle = "Automatically detect and zoom into manga panels for a more focused reading experience.",
                ),
            ),
        )
    }

    @Composable
    private fun getReaderEnhancementsGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val pageLayout by readerPreferences.pageLayout().collectAsState()
        return Preference.PreferenceGroup(
            title = "Reader Enhancements",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.readerInstantRetry(),
                    title = stringResource(SYMR.strings.skip_queue_on_retry),
                    subtitle = stringResource(SYMR.strings.skip_queue_on_retry_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.preserveReadingPosition(),
                    title = stringResource(SYMR.strings.preserve_reading_position),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.useAutoWebtoon(),
                    title = stringResource(SYMR.strings.auto_webtoon_mode),
                    subtitle = stringResource(SYMR.strings.auto_webtoon_mode_summary),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = readerPreferences.readerBottomButtons(),
                    title = stringResource(SYMR.strings.reader_bottom_buttons),
                    subtitle = stringResource(SYMR.strings.reader_bottom_buttons_summary),
                    entries = ReaderBottomButton.entries
                        .associate { it.value to stringResource(it.stringRes) }
                        .toImmutableMap(),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.pageLayout(),
                    title = stringResource(SYMR.strings.page_layout),
                    subtitle = stringResource(SYMR.strings.automatic_can_still_switch),
                    entries = ReaderPreferences.PageLayouts
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.invertDoublePages(),
                    title = stringResource(SYMR.strings.invert_double_pages),
                    enabled = pageLayout != PagerConfig.PageLayout.SINGLE_PAGE,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.centerMarginType(),
                    title = stringResource(SYMR.strings.center_margin),
                    subtitle = stringResource(SYMR.strings.pref_center_margin_summary),
                    entries = ReaderPreferences.CenterMarginTypes
                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.archiveReaderMode(),
                    title = stringResource(SYMR.strings.pref_archive_reader_mode),
                    subtitle = stringResource(SYMR.strings.pref_archive_reader_mode_summary),
                    entries = ReaderPreferences.archiveModeTypes
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                ),
            ),
        )
    }

    @Composable
    private fun getBrowsingGroup(sourcePreferences: SourcePreferences, uiPreferences: UiPreferences): Preference.PreferenceGroup {
        val hideFeedTab by uiPreferences.hideFeedTab().collectAsState()
        return Preference.PreferenceGroup(
            title = "Browsing & Feed",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = sourcePreferences.sourcesTabCategoriesFilter(),
                    title = stringResource(SYMR.strings.pref_source_source_filtering),
                    subtitle = stringResource(SYMR.strings.pref_source_source_filtering_summery),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.useNewSourceNavigation(),
                    title = stringResource(SYMR.strings.pref_source_navigation),
                    subtitle = stringResource(SYMR.strings.pref_source_navigation_summery),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = sourcePreferences.allowLocalSourceHiddenFolders(),
                    title = stringResource(SYMR.strings.pref_local_source_hidden_folders),
                    subtitle = stringResource(SYMR.strings.pref_local_source_hidden_folders_summery),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.hideFeedTab(),
                    title = stringResource(SYMR.strings.pref_hide_feed),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.feedTabInFront(),
                    title = stringResource(SYMR.strings.pref_feed_position),
                    subtitle = stringResource(SYMR.strings.pref_feed_position_summery),
                    enabled = hideFeedTab.not(),
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

    @Composable
    private fun getNavigationGroup(uiPreferences: UiPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            stringResource(SYMR.strings.pref_category_navbar),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.showNavUpdates(),
                    title = stringResource(SYMR.strings.pref_hide_updates_button),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.showNavHistory(),
                    title = stringResource(SYMR.strings.pref_hide_history_button),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.bottomBarLabels(),
                    title = stringResource(SYMR.strings.pref_show_bottom_bar_labels),
                ),
            ),
        )
    }

    @Composable
    private fun getInterfaceGroup(uiPreferences: UiPreferences): Preference.PreferenceGroup {
        val previewsRowCount by uiPreferences.previewsRowCount().collectAsState()

        return Preference.PreferenceGroup(
            "Interface Enhancements",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.expandFilters(),
                    title = stringResource(SYMR.strings.toggle_expand_search_filters),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.recommendsInOverflow(),
                    title = stringResource(SYMR.strings.put_recommends_in_overflow),
                    subtitle = stringResource(SYMR.strings.put_recommends_in_overflow_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.mergeInOverflow(),
                    title = stringResource(SYMR.strings.put_merge_in_overflow),
                    subtitle = stringResource(SYMR.strings.put_merge_in_overflow_summary),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = previewsRowCount,
                    title = stringResource(SYMR.strings.pref_previews_row_count),
                    subtitle = if (previewsRowCount > 0) {
                        pluralStringResource(
                            SYMR.plurals.row_count,
                            previewsRowCount,
                            previewsRowCount,
                        )
                    } else {
                        stringResource(MR.strings.disabled)
                    },
                    valueRange = 0..10,
                    onValueChanged = {
                        uiPreferences.previewsRowCount().set(it)
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getSecurityDebugGroup(
        securityPreferences: SecurityPreferences,
        sourcePreferences: SourcePreferences,
        delegateSourcePreferences: DelegateSourcePreferences
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        var enableEncryptDatabase by remember { mutableStateOf(false) }

        return Preference.PreferenceGroup(
            title = "Security & Developer Tools",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = delegateSourcePreferences.delegateSources(),
                    title = stringResource(SYMR.strings.toggle_delegated_sources),
                    subtitle = stringResource(
                        SYMR.strings.toggle_delegated_sources_summary,
                        stringResource(MR.strings.app_name),
                        AndroidSourceManager.DELEGATED_SOURCES.values.map { it.sourceName }.distinct()
                            .joinToString(),
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = sourcePreferences.enableSourceBlacklist(),
                    title = stringResource(SYMR.strings.enable_source_blacklist),
                    subtitle = stringResource(
                        SYMR.strings.enable_source_blacklist_summary,
                        stringResource(MR.strings.app_name),
                    ),
                ),
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(SYMR.strings.encrypt_database),
                ) {
                    if (enableEncryptDatabase) {
                        val dismiss = { enableEncryptDatabase = false }
                        AlertDialog(
                            onDismissRequest = dismiss,
                            title = { Text(text = stringResource(SYMR.strings.encrypt_database)) },
                            text = {
                                Text(
                                    text = remember {
                                        HtmlCompat.fromHtml(
                                            context.stringResource(SYMR.strings.encrypt_database_message),
                                            HtmlCompat.FROM_HTML_MODE_COMPACT,
                                        ).toAnnotatedString()
                                    },
                                )
                            },
                            dismissButton = {
                                TextButton(onClick = dismiss) {
                                    Text(text = stringResource(MR.strings.action_cancel))
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        dismiss()
                                        securityPreferences.encryptDatabase().set(true)
                                    },
                                ) {
                                    Text(text = stringResource(MR.strings.action_ok))
                                }
                            },
                        )
                    }
                    Preference.PreferenceItem.SwitchPreference(
                        title = stringResource(SYMR.strings.encrypt_database),
                        preference = securityPreferences.encryptDatabase(),
                        subtitle = stringResource(SYMR.strings.encrypt_database_subtitle),
                        onValueChanged = {
                            if (it) {
                                enableEncryptDatabase = true
                                false
                            } else {
                                true
                            }
                        },
                    )
                },
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(SYMR.strings.open_debug_menu),
                    subtitle = remember {
                        HtmlCompat.fromHtml(
                            context.stringResource(SYMR.strings.open_debug_menu_summary),
                            HtmlCompat.FROM_HTML_MODE_COMPACT,
                        ).toAnnotatedString()
                    },
                    onClick = { navigator.push(SettingsDebugScreen()) },
                ),
            ),
        )
    }

    @Composable
    private fun getAdvancedFeaturesGroup(

        readerPreferences: ReaderPreferences,
        sourcePreferences: SourcePreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        return Preference.PreferenceGroup(
            title = "Advanced Options",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.flashOnPageChange(),
                    title = stringResource(MR.strings.pref_flash_page),
                    subtitle = stringResource(MR.strings.pref_flash_page_summ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.aggressivePageLoading(),
                    title = stringResource(SYMR.strings.aggressively_load_pages),
                    subtitle = stringResource(SYMR.strings.aggressively_load_pages_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = sourcePreferences.dataSaverImageFormatJpeg(),
                    title = "Transcode to WebP",
                    subtitle = "Convert images to WebP format for better compression and data savings.",
                    onValueChanged = { !it }, // Invert since true means Jpeg in source prefs
                ),
                Preference.PreferenceItem.TextPreference(
                   title = "Migrate legacy downloads",
                   subtitle = "Mass migrate your old downloads to the current internal structure",
                   onClick = {
                       com.shinku.reader.data.download.DownloadMigrationJob.startNow(context)
                       context.toast("Migration started in background")
                   },
                ),            ),
        )
    }
}
