package com.shinku.reader.presentation.more.settings.screen

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.extension.interactor.TrustExtension
import com.shinku.reader.domain.source.interactor.GeminiVibeSearch
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.presentation.more.settings.Preference
import com.shinku.reader.presentation.more.settings.screen.advanced.ClearDatabaseScreen
import com.shinku.reader.presentation.more.settings.screen.debug.DebugInfoScreen
import com.shinku.reader.presentation.more.settings.widget.BasePreferenceWidget
import com.shinku.reader.presentation.more.settings.widget.PrefsHorizontalPadding
import com.shinku.reader.presentation.more.settings.widget.TextPreferenceWidget
import com.shinku.reader.R
import com.shinku.reader.core.security.SecurityPreferences
import com.shinku.reader.data.download.DownloadCache
import com.shinku.reader.data.download.DownloadManager
import com.shinku.reader.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_360
import eu.kanade.tachiyomi.network.PREF_DOH_ADGUARD
import eu.kanade.tachiyomi.network.PREF_DOH_ALIDNS
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.network.PREF_DOH_CONTROLD
import eu.kanade.tachiyomi.network.PREF_DOH_DNSPOD
import eu.kanade.tachiyomi.network.PREF_DOH_GOOGLE
import eu.kanade.tachiyomi.network.PREF_DOH_MULLVAD
import eu.kanade.tachiyomi.network.PREF_DOH_NJALLA
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD101
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD9
import eu.kanade.tachiyomi.network.PREF_DOH_SHECAN
import eu.kanade.tachiyomi.source.AndroidSourceManager
import com.shinku.reader.ui.more.OnboardingScreen
import com.shinku.reader.util.CrashLogUtil
import com.shinku.reader.util.storage.DiskUtil
import com.shinku.reader.util.system.GLUtil
import com.shinku.reader.util.system.isDevFlavor
import com.shinku.reader.util.system.isPreviewBuildType
import com.shinku.reader.util.system.isShizukuInstalled
import com.shinku.reader.util.system.powerManager
import com.shinku.reader.util.system.setDefaultSettings
import com.shinku.reader.util.system.toast
import com.shinku.reader.exh.debug.SettingsDebugScreen
import com.shinku.reader.exh.log.EHLogLevel
import com.shinku.reader.exh.pref.DelegateSourcePreferences
import com.shinku.reader.exh.source.BlacklistedSources
import com.shinku.reader.exh.source.EH_SOURCE_ID
import com.shinku.reader.exh.source.EXH_SOURCE_ID
import com.shinku.reader.exh.source.ExhPreferences
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.exh.util.toAnnotatedString
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.Headers
import com.shinku.reader.core.common.i18n.pluralStringResource
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.core.common.util.lang.launchNonCancellable
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.lang.withUIContext
import com.shinku.reader.core.common.util.system.ImageUtil
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.chapter.interactor.GetChaptersByMangaId
import com.shinku.reader.domain.download.service.DownloadPreferences
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.interactor.GetAllManga
import com.shinku.reader.domain.manga.interactor.ResetViewerFlags
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.LabeledCheckbox
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

object SettingsAdvancedScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val networkPreferences = remember { Injekt.get<NetworkPreferences>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val downloadPreferences = remember { Injekt.get<DownloadPreferences>() }

        return listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_dump_crash_logs),
                subtitle = stringResource(MR.strings.pref_dump_crash_logs_summary),
                onClick = {
                    scope.launch {
                        CrashLogUtil(context).dumpLogs()
                    }
                },
            ),
            /* SY --> Preference.PreferenceItem.SwitchPreference(
                preference = networkPreferences.verboseLogging(),
                title = stringResource(MR.strings.pref_verbose_logging),
                subtitle = stringResource(MR.strings.pref_verbose_logging_summary),
                onValueChanged = {
                    context.toast(MR.strings.requires_app_restart)
                    true
                },
            ), SY <-- */
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_debug_info),
                onClick = { navigator.push(DebugInfoScreen()) },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_onboarding_guide),
                onClick = { navigator.push(OnboardingScreen()) },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_manage_notifications),
                onClick = {
                    // SY -->
                    val intent = Intent().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        } else {
                            setAction("android.settings.APP_NOTIFICATION_SETTINGS")
                            putExtra("app_package", context.packageName)
                            putExtra("app_uid", context.applicationInfo.uid)
                        }
                    }
                    // SY <--
                    context.startActivity(intent)
                },
            ),
            getBackgroundActivityGroup(),
            getDataGroup(),
            getNetworkGroup(networkPreferences = networkPreferences),
            getLibraryGroup(libraryPreferences = libraryPreferences),
            getDownloadsGroup(downloadPreferences = downloadPreferences),
            getReaderGroup(basePreferences = basePreferences),
            getExtensionsGroup(basePreferences = basePreferences),
            getDeveloperToolsGroup(),
        )
    }

    @Composable
    private fun getBackgroundActivityGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_background_activity),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_disable_battery_optimization),
                    subtitle = stringResource(MR.strings.pref_disable_battery_optimization_summary),
                    onClick = {
                        val packageName: String = context.packageName
                        if (!context.powerManager.isIgnoringBatteryOptimizations(packageName)) {
                            try {
                                @SuppressLint("BatteryLife")
                                val intent = Intent().apply {
                                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    data = "package:$packageName".toUri()
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                context.toast(MR.strings.battery_optimization_setting_activity_not_found)
                            }
                        } else {
                            context.toast(MR.strings.battery_optimization_disabled)
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Don't kill my app!",
                    subtitle = stringResource(MR.strings.about_dont_kill_my_app),
                    onClick = { uriHandler.openUri("https://dontkillmyapp.com/") },
                ),
            ),
        )
    }

    @Composable
    private fun getDataGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_data),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_invalidate_download_cache),
                    subtitle = stringResource(MR.strings.pref_invalidate_download_cache_summary),
                    onClick = {
                        Injekt.get<DownloadCache>().invalidateCache()
                        context.toast(MR.strings.download_cache_invalidated)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_clear_database),
                    subtitle = stringResource(MR.strings.pref_clear_database_summary),
                    onClick = { navigator.push(ClearDatabaseScreen()) },
                ),
            ),
        )
    }

    @Composable
    private fun getNetworkGroup(
        networkPreferences: NetworkPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val networkHelper = remember { Injekt.get<NetworkHelper>() }
        val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
        val geminiVibeSearch = remember { Injekt.get<GeminiVibeSearch>() }

        val userAgentPref = networkPreferences.defaultUserAgent()
        val userAgent by userAgentPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_network),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_clear_cookies),
                    onClick = {
                        networkHelper.cookieJar.removeAll()
                        context.toast(MR.strings.cookies_cleared)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_clear_webview_data),
                    onClick = {
                        try {
                            WebView(context).run {
                                setDefaultSettings()
                                clearCache(true)
                                clearFormData()
                                clearHistory()
                                clearSslPreferences()
                            }
                            WebStorage.getInstance().deleteAllData()
                            context.applicationInfo?.dataDir?.let {
                                File("$it/app_webview/").deleteRecursively()
                            }
                            context.toast(MR.strings.webview_data_deleted)
                        } catch (e: Throwable) {
                            logcat(LogPriority.ERROR, e)
                            context.toast(MR.strings.cache_delete_error)
                        }
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = networkPreferences.dohProvider(),
                    entries = persistentMapOf(
                        -1 to stringResource(MR.strings.disabled),
                        PREF_DOH_CLOUDFLARE to "Cloudflare",
                        PREF_DOH_GOOGLE to "Google",
                        PREF_DOH_ADGUARD to "AdGuard",
                        PREF_DOH_QUAD9 to "Quad9",
                        PREF_DOH_ALIDNS to "AliDNS",
                        PREF_DOH_DNSPOD to "DNSPod",
                        PREF_DOH_360 to "360",
                        PREF_DOH_QUAD101 to "Quad 101",
                        PREF_DOH_MULLVAD to "Mullvad",
                        PREF_DOH_CONTROLD to "Control D",
                        PREF_DOH_NJALLA to "Njalla",
                        PREF_DOH_SHECAN to "Shecan",
                    ),
                    title = stringResource(MR.strings.pref_dns_over_https),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_user_agent_string),
                    content = {
                        var showDialog by rememberSaveable { mutableStateOf(false) }
                        if (showDialog) {
                            var currentUa by rememberSaveable { mutableStateOf(userAgent) }
                            AlertDialog(
                                onDismissRequest = { showDialog = false },
                                title = { Text(text = stringResource(MR.strings.pref_user_agent_string)) },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = currentUa,
                                            onValueChange = { currentUa = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 5,
                                        )
                                    }
                                },
                                confirmButton = {
                                    Row {
                                        TextButton(
                                            onClick = {
                                                currentUa = userAgentPref.defaultValue()
                                            },
                                        ) {
                                            Text(text = stringResource(MR.strings.action_default))
                                        }
                                        TextButton(
                                            onClick = {
                                                val apiKey = shinkuPreferences.geminiApiKey().get()
                                                val model = shinkuPreferences.geminiModel().get()
                                                scope.launch {
                                                    withContext(Dispatchers.Main) {
                                                        context.toast(MR.strings.vibe_search_loading)
                                                    }
                                                    val latest = geminiVibeSearch.getLatestUserAgent(apiKey, model, currentUa)
                                                    if (latest.isNotBlank()) {
                                                        currentUa = latest
                                                    } else {
                                                        withContext(Dispatchers.Main) {
                                                            context.toast("Failed to fetch latest UA")
                                                        }
                                                    }
                                                }
                                            },
                                        ) {
                                            Text(text = stringResource(MR.strings.action_update))
                                        }
                                        TextButton(
                                            onClick = {
                                                try {
                                                    Headers.Builder().add("User-Agent", currentUa)
                                                    userAgentPref.set(currentUa)
                                                    context.toast(MR.strings.requires_app_restart)
                                                    showDialog = false
                                                } catch (_: IllegalArgumentException) {
                                                    context.toast(MR.strings.error_user_agent_string_invalid)
                                                }
                                            },
                                        ) {
                                            Text(text = stringResource(MR.strings.action_ok))
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDialog = false }) {
                                        Text(text = stringResource(MR.strings.action_cancel))
                                    }
                                },
                                properties = DialogProperties(usePlatformDefaultWidth = true),
                            )
                        }

                        TextPreferenceWidget(
                            title = stringResource(MR.strings.pref_user_agent_string),
                            subtitle = userAgent,
                            onPreferenceClick = { showDialog = true },
                        )
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getLibraryGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_library),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_refresh_library_covers),
                    onClick = { LibraryUpdateJob.startNow(context, target = LibraryUpdateJob.Target.COVERS) },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_reset_viewer_flags),
                    subtitle = stringResource(MR.strings.pref_reset_viewer_flags_summary),
                    onClick = {
                        scope.launchNonCancellable {
                            val success = Injekt.get<ResetViewerFlags>().await()
                            withUIContext {
                                val message = if (success) {
                                    MR.strings.pref_reset_viewer_flags_success
                                } else {
                                    MR.strings.pref_reset_viewer_flags_error
                                }
                                context.toast(message)
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.updateMangaTitles(),
                    title = stringResource(MR.strings.pref_update_library_manga_titles),
                    subtitle = stringResource(MR.strings.pref_update_library_manga_titles_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.disallowNonAsciiFilenames(),
                    title = stringResource(MR.strings.pref_disallow_non_ascii_filenames),
                    subtitle = stringResource(MR.strings.pref_disallow_non_ascii_filenames_details),
                ),
            ),
        )
    }

    // SY ->
    @Composable
    private fun getDownloadsGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_downloads),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.includeChapterUrlHash(),
                    title = stringResource(SYMR.strings.pref_include_chapter_url_hash),
                    subtitle = stringResource(SYMR.strings.pref_include_chapter_url_hash_desc),
                ),
            ),
        )
    }
    // <- SY

    @Composable
    private fun getReaderGroup(
        basePreferences: BasePreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val chooseColorProfile = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                basePreferences.displayProfile().set(uri.toString())
            }
        }
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_reader),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = basePreferences.hardwareBitmapThreshold(),
                    entries = GLUtil.CUSTOM_TEXTURE_LIMIT_OPTIONS
                        .mapIndexed { index, option ->
                            val display = if (index == 0) {
                                stringResource(MR.strings.pref_hardware_bitmap_threshold_default, option)
                            } else {
                                option.toString()
                            }
                            option to display
                        }
                        .toMap()
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_hardware_bitmap_threshold),
                    subtitleProvider = { value, options ->
                        stringResource(MR.strings.pref_hardware_bitmap_threshold_summary, options[value].orEmpty())
                    },
                    enabled = !ImageUtil.HARDWARE_BITMAP_UNSUPPORTED &&
                        GLUtil.DEVICE_TEXTURE_LIMIT > GLUtil.SAFE_TEXTURE_LIMIT,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = basePreferences.alwaysDecodeLongStripWithSSIV(),
                    title = stringResource(MR.strings.pref_always_decode_long_strip_with_ssiv_2),
                    subtitle = stringResource(MR.strings.pref_always_decode_long_strip_with_ssiv_summary),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_display_profile),
                    subtitle = basePreferences.displayProfile().get(),
                    onClick = {
                        chooseColorProfile.launch(arrayOf("*/*"))
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getExtensionsGroup(
        basePreferences: BasePreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val extensionInstallerPref = basePreferences.extensionInstaller()
        var shizukuMissing by rememberSaveable { mutableStateOf(false) }
        val trustExtension = remember { Injekt.get<TrustExtension>() }

        if (shizukuMissing) {
            val dismiss = { shizukuMissing = false }
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text(text = stringResource(MR.strings.ext_installer_shizuku)) },
                text = { Text(text = stringResource(MR.strings.ext_installer_shizuku_unavailable_dialog)) },
                dismissButton = {
                    TextButton(onClick = dismiss) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dismiss()
                            uriHandler.openUri("https://shizuku.rikka.app/download")
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                },
            )
        }
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_extensions),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = extensionInstallerPref,
                    entries = extensionInstallerPref.entries
                        .filter {
                            // TODO: allow private option in stable versions once URL handling is more fleshed out
                            if (isPreviewBuildType || isDevFlavor) {
                                true
                            } else {
                                it != BasePreferences.ExtensionInstaller.PRIVATE
                            }
                        }
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.ext_installer_pref),
                    onValueChanged = {
                        if (it == BasePreferences.ExtensionInstaller.SHIZUKU &&
                            !context.isShizukuInstalled
                        ) {
                            shizukuMissing = true
                            false
                        } else {
                            true
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.ext_revoke_trust),
                    onClick = {
                        trustExtension.revokeAll()
                        context.toast(MR.strings.requires_app_restart)
                    },
                ),
            ),
        )
    }

    // SY -->
    @Composable
    private fun getDeveloperToolsGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val delegateSourcePreferences = remember { Injekt.get<DelegateSourcePreferences>() }
        val securityPreferences = remember { Injekt.get<SecurityPreferences>() }
        return Preference.PreferenceGroup(
            title = stringResource(SYMR.strings.developer_tools),
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
                kotlin.run {
                    var enableEncryptDatabase by rememberSaveable { mutableStateOf(false) }

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
}
