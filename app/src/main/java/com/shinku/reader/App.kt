package com.shinku.reader

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Looper
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.Printer
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import com.shinku.reader.domain.DomainModule
import com.shinku.reader.domain.SYDomainModule
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.sync.SyncPreferences
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.domain.ui.model.setAppCompatDelegateThemeMode
import com.shinku.reader.core.security.PrivacyPreferences
import com.shinku.reader.crash.CrashActivity
import com.shinku.reader.crash.GlobalExceptionHandler
import com.shinku.reader.data.coil.BufferedSourceFetcher
import com.shinku.reader.data.coil.MangaCoverFetcher
import com.shinku.reader.data.coil.MangaCoverKeyer
import com.shinku.reader.data.coil.MangaKeyer
import com.shinku.reader.data.coil.PagePreviewFetcher
import com.shinku.reader.data.coil.PagePreviewKeyer
import com.shinku.reader.data.coil.TachiyomiImageDecoder
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.data.sync.SyncDataJob
import com.shinku.reader.di.AppModule
import com.shinku.reader.di.InjektKoinBridge
import com.shinku.reader.di.PreferenceModule
import com.shinku.reader.di.SYPreferenceModule
import com.shinku.reader.di.importModule
import com.shinku.reader.di.initExpensiveComponents
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import com.shinku.reader.ui.base.delegate.SecureActivityDelegate
import com.shinku.reader.util.system.DeviceUtil
import com.shinku.reader.util.system.GLUtil
import com.shinku.reader.util.system.WebViewUtil
import com.shinku.reader.util.system.animatorDurationScale
import com.shinku.reader.util.system.cancelNotification
import com.shinku.reader.util.system.notify
import com.shinku.reader.exh.log.CrashlyticsPrinter
import com.shinku.reader.exh.log.EHLogLevel
import com.shinku.reader.exh.log.EnhancedFilePrinter
import com.shinku.reader.exh.log.XLogLogcatLogger
import com.shinku.reader.exh.log.xLogD
import com.shinku.reader.exh.syDebugVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import logcat.LogcatLogger
import com.shinku.reader.core.firebase.FirebaseConfig
import com.shinku.reader.core.migration.Migrator
import com.shinku.reader.core.migration.migrations.migrations
import org.conscrypt.Conscrypt
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.core.common.preference.PreferenceStore
import com.shinku.reader.core.common.util.system.ImageUtil
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.storage.service.StorageManager
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.widget.WidgetManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.security.Security
import java.text.SimpleDateFormat
import java.util.Locale

class App : Application(), DefaultLifecycleObserver, SingletonImageLoader.Factory {

    private val basePreferences: BasePreferences by injectLazy()
    private val privacyPreferences: PrivacyPreferences by injectLazy()
    private val networkPreferences: NetworkPreferences by injectLazy()

    private val disableIncognitoReceiver = DisableIncognitoReceiver()

    @SuppressLint("LaunchActivityFromNotification")
    override fun onCreate() {
        super<Application>.onCreate()
        FirebaseConfig.init(applicationContext)

        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        // TLS 1.3 support for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        // Avoid potential crashes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }

        Injekt.importModule(PreferenceModule(this))
        Injekt.importModule(AppModule(this))
        Injekt.importModule(DomainModule())
        // SY -->
        Injekt.importModule(SYPreferenceModule(this))
        Injekt.importModule(SYDomainModule())
        InjektKoinBridge.startKoin(this)
        initExpensiveComponents(this)
        // SY <--

        setupExhLogging() // EXH logging
        LogcatLogger.install()
        LogcatLogger.loggers += XLogLogcatLogger() // SY Redirect Logcat to XLog

        setupNotificationChannels()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val scope = ProcessLifecycleOwner.get().lifecycleScope

        // Show notification to disable Incognito Mode when it's enabled
        basePreferences.incognitoMode().changes()
            .onEach { enabled ->
                if (enabled) {
                    disableIncognitoReceiver.register()
                    notify(
                        Notifications.ID_INCOGNITO_MODE,
                        Notifications.CHANNEL_INCOGNITO_MODE,
                    ) {
                        setContentTitle(stringResource(MR.strings.pref_incognito_mode))
                        setContentText(stringResource(MR.strings.notification_incognito_text))
                        setSmallIcon(R.drawable.ic_glasses_24dp)
                        setOngoing(true)

                        val pendingIntent = PendingIntent.getBroadcast(
                            this@App,
                            0,
                            Intent(ACTION_DISABLE_INCOGNITO_MODE).setPackage(BuildConfig.APPLICATION_ID),
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                        )
                        setContentIntent(pendingIntent)
                    }
                } else {
                    disableIncognitoReceiver.unregister()
                    cancelNotification(Notifications.ID_INCOGNITO_MODE)
                }
            }
            .launchIn(scope)

        privacyPreferences.analytics()
            .changes()
            .onEach(FirebaseConfig::setAnalyticsEnabled)
            .launchIn(scope)

        privacyPreferences.crashlytics()
            .changes()
            .onEach(FirebaseConfig::setCrashlyticsEnabled)
            .launchIn(scope)

        basePreferences.hardwareBitmapThreshold().let { preference ->
            if (!preference.isSet()) preference.set(GLUtil.DEVICE_TEXTURE_LIMIT)
        }

        basePreferences.hardwareBitmapThreshold().changes()
            .onEach { ImageUtil.hardwareBitmapThreshold = it }
            .launchIn(scope)

        setAppCompatDelegateThemeMode(Injekt.get<UiPreferences>().themeMode().get())

        // Updates widget update
        WidgetManager(Injekt.get(), Injekt.get()).apply { init(scope) }

        /*if (!LogcatLogger.isInstalled && networkPreferences.verboseLogging().get()) {
            LogcatLogger.install(AndroidLogcatLogger(LogPriority.VERBOSE))
        }*/

        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(this, Configuration.Builder().build())
        }
        val syncPreferences: SyncPreferences = Injekt.get()
        val syncTriggerOpt = syncPreferences.getSyncTriggerOptions()
        if (syncPreferences.isSyncEnabled() && syncTriggerOpt.syncOnAppStart) {
            SyncDataJob.startNow(this@App)
        }

        initializeMigrator()
    }

    private fun initializeMigrator() {
        val preferenceStore = Injekt.get<PreferenceStore>()
        // SY -->
        val preference = preferenceStore.getInt(Preference.appStateKey("eh_last_version_code"), 0)
        // SY <--
        logcat { "Migration from ${preference.get()} to ${BuildConfig.VERSION_CODE}" }
        Migrator.initialize(
            old = preference.get(),
            new = BuildConfig.VERSION_CODE,
            migrations = migrations,
            onMigrationComplete = {
                logcat { "Updating last version to ${BuildConfig.VERSION_CODE}" }
                preference.set(BuildConfig.VERSION_CODE)
            },
        )
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(this).apply {
            val callFactoryLazy = lazy { Injekt.get<NetworkHelper>().client }
            components {
                // NetworkFetcher.Factory
                add(OkHttpNetworkFetcherFactory(callFactoryLazy::value))
                // Decoder.Factory
                add(TachiyomiImageDecoder.Factory())
                // Fetcher.Factory
                add(BufferedSourceFetcher.Factory())
                add(MangaCoverFetcher.MangaCoverFactory(callFactoryLazy))
                add(MangaCoverFetcher.MangaFactory(callFactoryLazy))
                // SY -->
                add(PagePreviewFetcher.Factory(callFactoryLazy))
                // SY <--
                // Keyer
                add(MangaCoverKeyer())
                add(MangaKeyer())
                // SY -->
                add(PagePreviewKeyer())
                // SY <--
            }

            memoryCache(
                MemoryCache.Builder()
                    .maxSizePercent(context)
                    .build(),
            )

            crossfade((300 * this@App.animatorDurationScale).toInt())
            allowRgb565(DeviceUtil.isLowRamDevice(this@App))
            if (networkPreferences.verboseLogging().get()) logger(DebugLogger())

            // Coil spawns a new thread for every image load by default
            fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
            decoderCoroutineContext(Dispatchers.IO.limitedParallelism(3))
        }
            .build()
    }

    override fun onStart(owner: LifecycleOwner) {
        SecureActivityDelegate.onApplicationStart()

        val syncPreferences: SyncPreferences = Injekt.get()
        val syncTriggerOpt = syncPreferences.getSyncTriggerOptions()
        if (syncPreferences.isSyncEnabled() && syncTriggerOpt.syncOnAppResume) {
            SyncDataJob.startNow(this@App)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        SecureActivityDelegate.onApplicationStopped()
    }

    override fun getPackageName(): String {
        // This causes freezes in Android 6/7 for some reason
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Override the value passed as X-Requested-With in WebView requests
                val stackTrace = Looper.getMainLooper().thread.stackTrace
                val isChromiumCall = stackTrace.any { trace ->
                    trace.className.lowercase() in setOf("org.chromium.base.buildinfo", "org.chromium.base.apkinfo") &&
                        trace.methodName.lowercase() in setOf("getall", "getpackagename", "<init>")
                }

                if (isChromiumCall) return WebViewUtil.spoofedPackageName(applicationContext)
            } catch (_: Exception) {
            }
        }

        return super.getPackageName()
    }

    private fun setupNotificationChannels() {
        try {
            Notifications.createChannels(this)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to modify notification channels" }
        }
    }

    // EXH
    private fun setupExhLogging() {
        EHLogLevel.init(this)

        val logLevel = when {
            EHLogLevel.shouldLog(EHLogLevel.EXTREME) -> LogLevel.ALL
            EHLogLevel.shouldLog(EHLogLevel.EXTRA) || BuildConfig.DEBUG -> LogLevel.DEBUG
            else -> LogLevel.WARN
        }

        val logConfig = LogConfiguration.Builder()
            .logLevel(logLevel)
            .disableStackTrace()
            .disableBorder()
            .build()

        val printers = mutableListOf<Printer>(AndroidPrinter())

        val logFolder = Injekt.get<StorageManager>().getLogsDirectory()

        if (logFolder != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

            printers += EnhancedFilePrinter
                .Builder(logFolder) {
                    fileNameGenerator = object : DateFileNameGenerator() {
                        override fun generateFileName(logLevel: Int, timestamp: Long): String {
                            return super.generateFileName(
                                logLevel,
                                timestamp,
                            ) + "-${BuildConfig.BUILD_TYPE}.txt"
                        }
                    }
                    flattener { timeMillis, level, tag, message ->
                        "${dateFormat.format(timeMillis)} ${LogLevel.getShortLevelName(level)}/$tag: $message"
                    }
                    backupStrategy = NeverBackupStrategy()
                }
        }

        // Install Crashlytics in prod
        if (!BuildConfig.DEBUG) {
            printers += CrashlyticsPrinter(LogLevel.ERROR)
        }

        XLog.init(
            logConfig,
            *printers.toTypedArray(),
        )

        xLogD("Application booting...")
        xLogD(
            """
                App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}, ${BuildConfig.COMMIT_SHA}, ${BuildConfig.VERSION_CODE})
                Preview build: $syDebugVersion
                Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
                Android build ID: ${Build.DISPLAY}
                Device brand: ${Build.BRAND}
                Device manufacturer: ${Build.MANUFACTURER}
                Device name: ${Build.DEVICE}
                Device model: ${Build.MODEL}
                Device product name: ${Build.PRODUCT}
            """.trimIndent(),
        )
    }

    private inner class DisableIncognitoReceiver : BroadcastReceiver() {
        private var registered = false

        override fun onReceive(context: Context, intent: Intent) {
            basePreferences.incognitoMode().set(false)
        }

        fun register() {
            if (!registered) {
                ContextCompat.registerReceiver(
                    this@App,
                    this,
                    IntentFilter(ACTION_DISABLE_INCOGNITO_MODE),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                registered = true
            }
        }

        fun unregister() {
            if (registered) {
                unregisterReceiver(this)
                registered = false
            }
        }
    }
}

private const val ACTION_DISABLE_INCOGNITO_MODE = "tachi.action.DISABLE_INCOGNITO_MODE"
