package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.lang.withUIContext
import com.shinku.reader.core.security.SecurityPreferences
import com.shinku.reader.data.category.SmartCategorizerJob
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.domain.ui.model.ThemeMode
import com.shinku.reader.domain.ui.model.setAppCompatDelegateThemeMode
import com.shinku.reader.exh.debug.SettingsDebugScreen
import com.shinku.reader.exh.pref.DelegateSourcePreferences
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.exh.util.toAnnotatedString
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.i18n.pluralStringResource
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.presentation.more.components.ReaderIdentityDialog
import com.shinku.reader.presentation.more.settings.Preference
import com.shinku.reader.presentation.more.settings.screen.appearance.AppLanguageScreen
import com.shinku.reader.presentation.more.settings.widget.AppThemeModePreferenceWidget
import com.shinku.reader.presentation.more.settings.widget.AppThemePreferenceWidget
import com.shinku.reader.ui.browse.migration.dead.DeadSourceScannerScreen
import com.shinku.reader.ui.browse.migration.failed.FailedUpdatesMigrationScreen
import com.shinku.reader.ui.reader.setting.ReaderBottomButton
import com.shinku.reader.ui.reader.setting.ReaderPreferences
import com.shinku.reader.ui.reader.viewer.pager.PagerConfig
import com.shinku.reader.ui.sourcehealth.SourceHealthScreen
import com.shinku.reader.util.system.toast
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.AndroidSourceManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsShinKuSettingsScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.action_configure_features

    @Composable
    override fun androidx.compose.foundation.layout.RowScope.AppBarAction() {
        val navigator = LocalNavigator.currentOrThrow
        androidx.compose.material3.IconButton(onClick = { navigator.push(SettingsSearchScreen()) }) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Search, stringResource(MR.strings.action_search))
        }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        return ShinKuSettingsSection.entries.map { section ->
            Preference.PreferenceItem.CustomPreference(title = stringResource(section.titleRes)) {
                androidx.compose.material3.Surface(
                    modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = com.shinku.reader.presentation.core.theme.SanctuaryShapes.card,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    com.shinku.reader.presentation.more.components.CommandRowItem(
                        title = stringResource(section.titleRes),
                        subtitle = stringResource(section.summaryRes),
                        icon = section.icon,
                        onClick = { navigator.push(ShinKuSettingsSectionScreen(section)) },
                    )
                }
            }
        } + Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.settings_save_automatically))
    }

    @Composable
    fun getSectionPreferences(section: ShinKuSettingsSection): List<Preference> {
        val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val readerPreferences = remember { Injekt.get<ReaderPreferences>() }
        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val securityPreferences = remember { Injekt.get<SecurityPreferences>() }
        val delegateSourcePreferences = remember { Injekt.get<DelegateSourcePreferences>() }

        return when (section) {
            ShinKuSettingsSection.AI -> listOf(getGeminiGroup(shinkuPreferences), getTranslationGroup(shinkuPreferences))
            ShinKuSettingsSection.READING -> listOf(
                getImmersionGroup(shinkuPreferences, readerPreferences),
                getReaderEnhancementsGroup(readerPreferences),
                getNavigationGroup(uiPreferences, shinkuPreferences),
                getInterfaceGroup(uiPreferences, shinkuPreferences),
                getBrowsingGroup(sourcePreferences, uiPreferences),
                getLibraryGroup(libraryPreferences, shinkuPreferences),
            )
            ShinKuSettingsSection.PERFORMANCE -> listOf(getPerformanceGroup(shinkuPreferences, basePreferences))
            ShinKuSettingsSection.MAINTENANCE -> listOf(getMaintenanceToolsGroup())
            ShinKuSettingsSection.ADVANCED -> listOf(
                getSecurityDebugGroup(securityPreferences, sourcePreferences, delegateSourcePreferences),
                getAdvancedFeaturesGroup(readerPreferences, sourcePreferences),
            )
        }
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
        basePreferences: BasePreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "Performance mode",
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
                    title = "Enhanced scaling",
                    subtitle = "Sharper upscaled pages; may be slower on older devices",
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
        val aiEngineRegistry = remember { Injekt.get<com.shinku.reader.domain.ai.AiEngineRegistry>() }

        val aiEngineProviderPref = shinkuPreferences.aiEngineProvider()
        val aiEngineProvider by aiEngineProviderPref.collectAsState()

        val activeKeyPref = when (aiEngineProvider) {
            "openrouter" -> shinkuPreferences.openRouterApiKey()
            "deepseek", "groq", "openai", "custom" -> shinkuPreferences.openAiApiKey()
            else -> shinkuPreferences.geminiApiKey()
        }
        val activeModelPref = when (aiEngineProvider) {
            "openrouter" -> shinkuPreferences.openRouterModel()
            "deepseek", "groq", "openai", "custom" -> shinkuPreferences.openAiModel()
            else -> shinkuPreferences.geminiModel()
        }
        val activeKey by activeKeyPref.collectAsState()
        val activeModel by activeModelPref.collectAsState()
        val activeUrl by shinkuPreferences.openAiBaseUrl().collectAsState()
        var testStatus by remember(aiEngineProvider, activeKey, activeModel, activeUrl) { mutableStateOf(MR.strings.connection_not_tested) }
        var isTesting by remember(aiEngineProvider, activeKey, activeModel, activeUrl) { mutableStateOf(false) }
        var testDetail by remember(aiEngineProvider, activeKey, activeModel, activeUrl) { mutableStateOf<String?>(null) }

        val items = buildList<Preference.PreferenceItem<out Any, out Any>> {
            add(
                Preference.PreferenceItem.ListPreference(
                    preference = aiEngineProviderPref,
                    title = "AI Engine Provider",
                    subtitle = "Select AI backend (Google Gemini, OpenRouter, DeepSeek, Groq, OpenAI, or Custom)",
                    entries = persistentMapOf(
                        "auto" to "Automatic (Recommended - Best Available)",
                        "cloud" to "Google Gemini (Official Cloud API)",
                        "openrouter" to "OpenRouter (Claude, GPT, DeepSeek, Llama)",
                        "deepseek" to "DeepSeek (api.deepseek.com)",
                        "groq" to "Groq (api.groq.com - Ultra Fast)",
                        "openai" to "OpenAI (api.openai.com)",
                        "custom" to "Custom (OpenAI-Compatible / Ollama)",
                    ),
                ),
            )

            when (aiEngineProvider) {
                "openrouter" -> {
                    add(
                        Preference.PreferenceItem.EditTextPreference(
                            preference = shinkuPreferences.openRouterApiKey(),
                            isSensitive = true,
                            title = "OpenRouter API Key",
                            subtitle = "Enter your OpenRouter key (sk-or-v1-...) to access Claude, GPT, DeepSeek, etc.",
                        ),
                    )
                    add(
                        Preference.PreferenceItem.ListPreference(
                            preference = shinkuPreferences.openRouterModel(),
                            title = "OpenRouter Model",
                            subtitle = "Select model to route via OpenRouter",
                            entries = persistentMapOf(
                                "auto" to "Auto (Recommended: Gemini 2.0 Flash / GPT-4o-mini)",
                                "google/gemini-2.0-flash-001" to "Gemini 2.0 Flash (Fast & Cheap)",
                                "anthropic/claude-3.5-sonnet" to "Claude 3.5 Sonnet (High Nuance)",
                                "anthropic/claude-3-haiku" to "Claude 3 Haiku (Fast & Cheap)",
                                "deepseek/deepseek-chat" to "DeepSeek V3 (Economic & Accurate)",
                                "deepseek/deepseek-r1" to "DeepSeek R1 (Reasoning)",
                                "openai/gpt-4o-mini" to "GPT-4o Mini (Fast)",
                                "openai/gpt-4o" to "GPT-4o (High Accuracy)",
                                "meta-llama/llama-3.3-70b-instruct" to "Llama 3.3 70B",
                                "meta-llama/llama-3.3-70b-instruct:free" to "Llama 3.3 70B (Free Tier)",
                            ),
                        ),
                    )
                }
                "deepseek" -> {
                    add(
                        Preference.PreferenceItem.EditTextPreference(
                            preference = shinkuPreferences.openAiApiKey(),
                            isSensitive = true,
                            title = "DeepSeek API Key",
                            subtitle = "Enter your API key from platform.deepseek.com",
                        ),
                    )
                    add(
                        Preference.PreferenceItem.ListPreference(
                            preference = shinkuPreferences.openAiModel(),
                            title = "DeepSeek Model",
                            subtitle = "Select DeepSeek model",
                            entries = persistentMapOf(
                                "auto" to "Auto (deepseek-chat)",
                                "deepseek-chat" to "DeepSeek Chat (V3)",
                                "deepseek-reasoner" to "DeepSeek Reasoner (R1)",
                            ),
                        ),
                    )
                }
                "groq" -> {
                    add(
                        Preference.PreferenceItem.EditTextPreference(
                            preference = shinkuPreferences.openAiApiKey(),
                            isSensitive = true,
                            title = "Groq API Key",
                            subtitle = "Enter your API key from console.groq.com (Ultra-fast inference)",
                        ),
                    )
                    add(
                        Preference.PreferenceItem.ListPreference(
                            preference = shinkuPreferences.openAiModel(),
                            title = "Groq Model",
                            subtitle = "Select Groq model",
                            entries = persistentMapOf(
                                "auto" to "Auto (llama-3.3-70b-versatile)",
                                "llama-3.3-70b-versatile" to "Llama 3.3 70B Versatile",
                                "llama-3.1-8b-instant" to "Llama 3.1 8B Instant (Instant)",
                                "mixtral-8x7b-32768" to "Mixtral 8x7B",
                            ),
                        ),
                    )
                }
                "openai" -> {
                    add(
                        Preference.PreferenceItem.EditTextPreference(
                            preference = shinkuPreferences.openAiApiKey(),
                            isSensitive = true,
                            title = "OpenAI API Key",
                            subtitle = "Enter your API key from platform.openai.com",
                        ),
                    )
                    add(
                        Preference.PreferenceItem.ListPreference(
                            preference = shinkuPreferences.openAiModel(),
                            title = "OpenAI Model",
                            subtitle = "Select OpenAI model",
                            entries = persistentMapOf(
                                "auto" to "Auto (gpt-4o-mini)",
                                "gpt-4o-mini" to "GPT-4o Mini (Fast & Economical)",
                                "gpt-4o" to "GPT-4o (Flagship)",
                            ),
                        ),
                    )
                }
                "custom" -> {
                    add(
                        Preference.PreferenceItem.EditTextPreference(
                            preference = shinkuPreferences.openAiBaseUrl(),
                            title = "API Base URL",
                            subtitle = "Base URL (e.g. http://10.0.2.2:11434/v1 for local Ollama)",
                        ),
                    )
                    add(
                        Preference.PreferenceItem.EditTextPreference(
                            preference = shinkuPreferences.openAiApiKey(),
                            isSensitive = true,
                            title = "API Key",
                            subtitle = "API key (optional for local Ollama)",
                        ),
                    )
                    add(
                        Preference.PreferenceItem.EditTextPreference(
                            preference = shinkuPreferences.openAiModel(),
                            title = "Model Name",
                            subtitle = "Model name to pass in requests (e.g. llama3, mistral, gpt-4o)",
                        ),
                    )
                }
                else -> {
                    // "cloud" or "auto" (Gemini)
                    add(
                        Preference.PreferenceItem.EditTextPreference(
                            preference = shinkuPreferences.geminiApiKey(),
                            isSensitive = true,
                            title = stringResource(MR.strings.pref_gemini_api_key),
                            subtitle = stringResource(MR.strings.pref_gemini_api_key_summary),
                        ),
                    )
                    addAll(
                        com.shinku.reader.presentation.more.settings.widget.geminiModelPreferences(
                            modelPreference = shinkuPreferences.geminiModel(),
                            apiKey = activeKey,
                            autoLoad = LocalNavigator.current?.lastItem == ShinKuSettingsSectionScreen(ShinKuSettingsSection.AI),
                            loadModels = { key, refresh ->
                                withIOContext {
                                    com.shinku.reader.domain.ai.GeminiApi(networkHelper.client).listTextModels(key, refresh)
                                }
                            },
                        ),
                    )
                }
            }

            add(Preference.PreferenceItem.InfoPreference(listOfNotNull(stringResource(testStatus), testDetail).joinToString("\n")))
            add(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.connection_test),
                    enabled = !isTesting,
                    subtitle = "Sends a lightweight prompt to verify credentials and check roundtrip latency",
                    onClick = connectionTest@{
                        if (isTesting) return@connectionTest
                        isTesting = true
                        testDetail = null
                        testStatus = MR.strings.connection_testing
                        scope.launch {
                            try {
                                val startTime = System.currentTimeMillis()
                                if (aiEngineProvider in listOf("openrouter", "deepseek", "groq", "openai", "custom")) {
                                    val engine = aiEngineRegistry.getActiveEngine()
                                    val result = engine.generateText("Respond with 'OK'")
                                    val latency = System.currentTimeMillis() - startTime
                                    testStatus = if (result.isSuccess) MR.strings.connection_succeeded else MR.strings.connection_failed
                                    if (result.isSuccess) {
                                        context.toast("Connected to $aiEngineProvider successfully! (${latency}ms)")
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: "Unknown error"
                                        context.toast(context.stringResource(MR.strings.api_key_test_failed, err))
                                    }
                                } else {
                                    val apiKey = shinkuPreferences.geminiApiKey().get().trim()
                                    if (apiKey.isBlank()) {
                                        testStatus = MR.strings.connection_failed
                                        testDetail = context.stringResource(MR.strings.gemini_catalog_key_needed)
                                        context.toast("API Key cannot be empty")
                                        return@launch
                                    }
                                    val (isSuccess, rawDetail) = testGeminiConnection(
                                        networkHelper,
                                        apiKey,
                                        shinkuPreferences.geminiModel().get().trim(),
                                    )
                                    val detail = rawDetail.replace(apiKey, "[redacted]").take(400)
                                    testStatus = if (isSuccess) MR.strings.connection_succeeded else MR.strings.connection_failed
                                    testDetail = detail
                                    if (isSuccess) {
                                        context.toast(detail)
                                    } else {
                                        context.toast(context.stringResource(MR.strings.api_key_test_failed, detail))
                                    }
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                testStatus = MR.strings.connection_failed
                                testDetail = (e.message ?: "Unknown error").let { message ->
                                    if (activeKey.isNotBlank()) message.replace(activeKey.trim(), "[redacted]") else message
                                }.take(400)
                                context.toast(context.stringResource(MR.strings.api_key_test_failed, e.message ?: "Unknown error"))
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                ),
            )
        }

        return Preference.PreferenceGroup(
            title = "ShinKu AI & Engines",
            preferenceItems = items.toPersistentList(),
        )
    }

    private suspend fun testGeminiConnection(
        networkHelper: NetworkHelper,
        apiKey: String,
        rawModel: String,
    ): Pair<Boolean, String> = withIOContext {
        val startTime = System.currentTimeMillis()
        try {
            val result = com.shinku.reader.domain.ai.GeminiApi(networkHelper.client).testConnection(apiKey, rawModel)
            Pair(true, "${result.model} is active and working! (${System.currentTimeMillis() - startTime}ms)")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Pair(false, e.message ?: "Gemini request failed")
        }
    }

    @Composable
    private fun getTranslationGroup(shinkuPreferences: ShinKuPreferences): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        return Preference.PreferenceGroup(
            title = "AI Translation Settings",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.liveTranslation(),
                    title = stringResource(MR.strings.pref_live_translation),
                    subtitle = stringResource(MR.strings.pref_live_translation_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = shinkuPreferences.translationSourceLanguage(),
                    title = "Translation Source Language",
                    subtitle = "Select the default language to translate from",
                    entries = persistentMapOf(
                        "Auto-Detect" to "Auto-Detect (自动检测)",
                        "Japanese" to "Japanese (日本語)",
                        "Chinese" to "Chinese (中文)",
                        "Korean" to "Korean (한국어)",
                        "English" to "English",
                        "Spanish" to "Spanish (Español)",
                        "French" to "French (Français)",
                        "German" to "German (Deutsch)",
                        "Italian" to "Italian (Italiano)",
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = shinkuPreferences.translationTargetLanguage(),
                    title = "Translation Target Language",
                    subtitle = "Select the language to translate manga pages into",
                    entries = persistentMapOf(
                        "English" to "English",
                        "Spanish" to "Spanish (Español)",
                        "French" to "French (Français)",
                        "German" to "German (Deutsch)",
                        "Portuguese" to "Portuguese (Português)",
                        "Italian" to "Italian (Italiano)",
                        "Russian" to "Russian (Русский)",
                        "Indonesian" to "Indonesian (Bahasa Indonesia)",
                        "Tagalog" to "Tagalog (Filipino)",
                        "Arabic" to "Arabic (العربية)",
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = shinkuPreferences.translationEngine(),
                    title = "Translation Engine",
                    subtitle = "Select translation provider (Cloud AI or Offline ML Kit)",
                    entries = persistentMapOf(
                        "auto" to "Automatic (Cloud AI if configured, with ML Kit fallback)",
                        "gemini_cloud" to "Cloud AI (Gemini / OpenRouter / DeepSeek / Groq)",
                        "mlkit" to "Standard ML Kit (Offline On-Device - Instant Speed)",
                    ),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Clear Translation Cache",
                    subtitle = "Purge cached overlay files to free up storage",
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { com.shinku.reader.ui.reader.model.TranslationCache.clear() }
                            context.toast("Translation cache cleared")
                        }
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getImmersionGroup(
        shinkuPreferences: ShinKuPreferences,
        readerPreferences: ReaderPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val atmosphericAudioManager = remember { Injekt.get<com.shinku.reader.ui.reader.audio.AtmosphericAudioManager>() }
        val atmosphericAudioEnabled by shinkuPreferences.atmosphericAudio().collectAsState()
        val atmosphericAudioVolume by shinkuPreferences.atmosphericAudioVolume().collectAsState()
        var cacheSizeText by remember { mutableStateOf(atmosphericAudioManager.getFormattedCacheSize()) }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_immersion_reader),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.atmosphericAudio(),
                    title = stringResource(SYMR.strings.pref_atmospheric_audio),
                    subtitle = stringResource(SYMR.strings.pref_atmospheric_audio_summary),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = atmosphericAudioVolume,
                    valueRange = 0..100,
                    steps = 19,
                    title = stringResource(SYMR.strings.pref_atmospheric_audio_volume),
                    valueString = "$atmosphericAudioVolume%",
                    enabled = atmosphericAudioEnabled,
                    onValueChanged = {
                        shinkuPreferences.atmosphericAudioVolume().set(it)
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = shinkuPreferences.atmosphericAudioOverride(),
                    entries = persistentMapOf(
                        "auto" to stringResource(SYMR.strings.pref_atmospheric_audio_vibe_auto),
                        "action" to "Action (Battle Campfire & Winds)",
                        "historical" to "Martial Arts (Bamboo Temple & Chimes)",
                        "horror" to "Horror (Dark & Creepy Drone)",
                        "mystery" to "Mystery (Noir & Rainy Window)",
                        "cyberpunk" to "Cyberpunk (Synth & Ambience)",
                        "cafe" to "Slice of Life (Cozy Cafe & Lofi)",
                        "rain" to "Romance (Gentle Rainfall)",
                        "forest" to "Fantasy (Ancient Forest Wilderness)",
                    ),
                    title = stringResource(SYMR.strings.pref_atmospheric_audio_vibe),
                    enabled = atmosphericAudioEnabled,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(SYMR.strings.pref_atmospheric_audio_download_all),
                    subtitle = stringResource(SYMR.strings.pref_atmospheric_audio_download_all_summary),
                    enabled = atmosphericAudioEnabled,
                    onClick = {
                        context.toast(SYMR.strings.pref_atmospheric_audio_downloading)
                        scope.launch {
                            val success = atmosphericAudioManager.downloadAllSoundscapes()
                            cacheSizeText = atmosphericAudioManager.getFormattedCacheSize()
                            if (success) {
                                context.toast(SYMR.strings.pref_atmospheric_audio_downloaded)
                            } else {
                                context.toast(SYMR.strings.pref_atmospheric_audio_download_failed)
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(SYMR.strings.pref_atmospheric_audio_clear_cache),
                    subtitle = "Cached soundscapes: $cacheSizeText",
                    enabled = atmosphericAudioEnabled && cacheSizeText != "0.0 KB",
                    onClick = {
                        atmosphericAudioManager.clearCache()
                        cacheSizeText = atmosphericAudioManager.getFormattedCacheSize()
                        context.toast("Soundscape cache cleared")
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Audio Sources & Attribution",
                    subtitle = "View original CC0 public domain recordings, artists, and repository credits",
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/Harrys-HQ/ShinKu-Assets#audio-sources--attribution"),
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.toast("Could not open browser")
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.reader_mood_lighting),
                    subtitle = stringResource(MR.strings.reader_mood_lighting_summary),
                    onClick = { navigator.push(SettingsColorEffectsScreen) },
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
                    preference = shinkuPreferences.adaptiveNightRead(),
                    title = "Ambient Adaptive Night-Read",
                    subtitle = "Optimize contrast and reduce screen brightness automatically in low-light environments.",
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
    private fun getLibraryGroup(
        libraryPreferences: LibraryPreferences,
        shinkuPreferences: ShinKuPreferences,
    ): Preference.PreferenceGroup {
        val navigator = LocalNavigator.currentOrThrow
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_library),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.cover_based_app_theme),
                    subtitle = stringResource(MR.strings.cover_based_app_theme_summary),
                    onClick = { navigator.push(SettingsColorEffectsScreen) },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.showHeroJumpBack(),
                    title = "Show 'Continue Reading' Jump-Back Deck",
                    subtitle = "Display recent in-progress titles at the top of the Library tab.",
                ),
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
    private fun getNavigationGroup(
        uiPreferences: UiPreferences,
        shinkuPreferences: ShinKuPreferences,
    ): Preference.PreferenceGroup {
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
                Preference.PreferenceItem.SwitchPreference(
                    preference = shinkuPreferences.autoHideBottomBar(),
                    title = "Auto-Hide Navigation Dock on Scroll",
                    subtitle = "Automatically collapse the bottom dock when scrolling down for greater reading and discovery immersion.",
                ),
            ),
        )
    }

    @Composable
    private fun getInterfaceGroup(
        uiPreferences: UiPreferences,
        shinkuPreferences: com.shinku.reader.exh.source.ShinKuPreferences,
    ): Preference.PreferenceGroup {
        val previewsRowCount by uiPreferences.previewsRowCount().collectAsState()
        val nickname by shinkuPreferences.readerNickname().collectAsState()
        val equippedTitle by shinkuPreferences.equippedBadgeTitle().collectAsState()
        var showIdentityDialog by remember { mutableStateOf(false) }

        if (showIdentityDialog) {
            ReaderIdentityDialog(
                onDismiss = { showIdentityDialog = false },
            )
        }

        val identitySubtitle = buildString {
            append(nickname.ifBlank { "Reader" })
            if (equippedTitle.isNotBlank()) {
                append(" • ")
                append(equippedTitle)
            }
            append(" (Tap to customize avatar, nickname & title)")
        }

        return Preference.PreferenceGroup(
            "Interface Enhancements",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = "Sanctuary Reader Identity",
                    subtitle = identitySubtitle,
                    onClick = { showIdentityDialog = true },
                ),
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
        delegateSourcePreferences: DelegateSourcePreferences,
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
                ),
            ),
        )
    }
}
