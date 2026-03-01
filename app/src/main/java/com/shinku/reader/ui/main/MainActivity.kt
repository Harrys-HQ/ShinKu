package com.shinku.reader.ui.main

import android.animation.ValueAnimator
import android.app.SearchManager
import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.animation.doOnEnd
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.source.interactor.GetIncognitoState
import com.shinku.reader.presentation.components.AppStateBanners
import com.shinku.reader.presentation.components.DownloadedOnlyBannerBackgroundColor
import com.shinku.reader.presentation.components.IncognitoModeBannerBackgroundColor
import com.shinku.reader.presentation.components.IndexingBannerBackgroundColor
import com.shinku.reader.presentation.more.settings.screen.about.WhatsNewDialog
import com.shinku.reader.presentation.more.settings.screen.browse.ExtensionReposScreen
import com.shinku.reader.presentation.more.settings.screen.data.RestoreBackupScreen
import com.shinku.reader.presentation.util.AssistContentScreen
import com.shinku.reader.presentation.util.DefaultNavigatorScreenTransition
import com.shinku.reader.BuildConfig
import com.shinku.reader.data.cache.ChapterCache
import com.shinku.reader.data.download.DownloadCache
import com.shinku.reader.data.notification.NotificationReceiver
import com.shinku.reader.data.updater.AppUpdateChecker
import com.shinku.reader.extension.api.ExtensionApi
import com.shinku.reader.ui.base.activity.BaseActivity
import com.shinku.reader.ui.browse.source.browse.BrowseSourceScreen
import com.shinku.reader.ui.browse.source.globalsearch.GlobalSearchScreen
import com.shinku.reader.ui.deeplink.DeepLinkScreen
import com.shinku.reader.ui.home.HomeScreen
import com.shinku.reader.ui.manga.MangaScreen
import com.shinku.reader.ui.more.NewUpdateScreen
import com.shinku.reader.ui.more.OnboardingScreen
import com.shinku.reader.util.system.dpToPx
import com.shinku.reader.util.system.isNavigationBarNeedsScrim
import com.shinku.reader.util.system.isPreviewBuildType
import com.shinku.reader.util.system.setHighRefreshRate
import com.shinku.reader.util.view.setComposeContent
import com.shinku.reader.exh.debug.DebugToggles
import com.shinku.reader.exh.log.DebugModeOverlay
import com.shinku.reader.exh.source.BlacklistedSources
import com.shinku.reader.exh.source.ExhPreferences
import com.shinku.reader.exh.syDebugVersion
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import logcat.LogPriority
import com.shinku.reader.core.migration.Migrator
import com.shinku.reader.core.common.Constants
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.release.interactor.GetApplicationRelease
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.util.collectAsState
import uy.kohesive.injekt.injectLazy
import java.util.LinkedList

class MainActivity : BaseActivity() {

    private val libraryPreferences: LibraryPreferences by injectLazy()
    private val preferences: BasePreferences by injectLazy()

    // SY -->
    private val exhPreferences: ExhPreferences by injectLazy()
    // SY <--

    private val downloadCache: DownloadCache by injectLazy()
    private val chapterCache: ChapterCache by injectLazy()

    private val getIncognitoState: GetIncognitoState by injectLazy()

    // To be checked by splash screen. If true then splash screen will be removed.
    var ready = false
    var migrationDone = false

    private var navigator: Navigator? = null

    init {
        registerSecureActivity(this)
    }

    // SY -->
    // Idle-until-urgent
    private var firstPaint = false
    private val iuuQueue = LinkedList<() -> Unit>()

    private fun initWhenIdle(task: () -> Unit) {
        // Avoid sync issues by enforcing main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("Can only be called on main thread!")
        }

        if (firstPaint) {
            task()
        } else {
            iuuQueue += task
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isLaunch = savedInstanceState == null

        // Prevent splash screen showing up on configuration changes
        val splashScreen = if (isLaunch) installSplashScreen() else null

        super.onCreate(savedInstanceState)

        window.setHighRefreshRate(preferences.highRefreshRate().get())

        // Do not let the launcher create a new activity http://stackoverflow.com/questions/16283079
        if (!isTaskRoot) {
            finish()
            return
        }

        // SY -->
        @Suppress("KotlinConstantConditions")
        val hasDebugOverlay = (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "releaseTest")
        // SY <--

        setComposeContent {
            val context = LocalContext.current

            var didMigration by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (isLaunch) {
                    addAnalytics()
                    didMigration = Migrator.await()
                    migrationDone = true
                    Migrator.release()
                } else {
                    migrationDone = true
                }
            }

            var incognito by remember { mutableStateOf(getIncognitoState.await(null)) }
            val downloadOnly by preferences.downloadedOnly().collectAsState()
            val indexing by downloadCache.isInitializing.collectAsState()

            val isSystemInDarkTheme = isSystemInDarkTheme()
            val statusBarBackgroundColor = when {
                indexing -> IndexingBannerBackgroundColor
                downloadOnly -> DownloadedOnlyBannerBackgroundColor
                incognito -> IncognitoModeBannerBackgroundColor
                else -> MaterialTheme.colorScheme.surface
            }
            LaunchedEffect(isSystemInDarkTheme, statusBarBackgroundColor) {
                // Draw edge-to-edge and set system bars color to transparent
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                enableEdgeToEdge(
                    statusBarStyle = if (statusBarBackgroundColor.luminance() > 0.5) lightStyle else darkStyle,
                    navigationBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
                )
            }

            Navigator(
                screen = HomeScreen,
                disposeBehavior = NavigatorDisposeBehavior(disposeNestedNavigators = false, disposeSteps = true),
            ) { navigator ->
                LaunchedEffect(navigator) {
                    this@MainActivity.navigator = navigator

                    if (isLaunch) {
                        // Set start screen
                        handleIntentAction(intent, navigator)

                        // Reset Incognito Mode on relaunch
                        preferences.incognitoMode().set(false)
                    }
                }
                LaunchedEffect(navigator.lastItem) {
                    (navigator.lastItem as? BrowseSourceScreen)?.sourceId
                        .let(getIncognitoState::subscribe)
                        .collectLatest { incognito = it }
                }

                val scaffoldInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)
                Scaffold(
                    topBar = {
                        AppStateBanners(
                            downloadedOnlyMode = downloadOnly,
                            incognitoMode = incognito,
                            indexing = indexing,
                            modifier = Modifier.windowInsetsPadding(scaffoldInsets),
                        )
                    },
                    contentWindowInsets = scaffoldInsets,
                ) { contentPadding ->
                    // Consume insets already used by app state banners
                    Box {
                        // Shows current screen
                        DefaultNavigatorScreenTransition(
                            navigator = navigator,
                            modifier = Modifier
                                .padding(contentPadding)
                                .consumeWindowInsets(contentPadding),
                        )

                        // Draw navigation bar scrim when needed
                        if (remember { isNavigationBarNeedsScrim() }) {
                            Spacer(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                                    .alpha(0.8f)
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                            )
                        }
                    }
                }

                // Pop source-related screens when incognito mode is turned off
                LaunchedEffect(Unit) {
                    preferences.incognitoMode().changes()
                        .drop(1)
                        .filter { !it }
                        .onEach {
                            val currentScreen = navigator.lastItem
                            if (currentScreen is BrowseSourceScreen ||
                                (currentScreen is MangaScreen && currentScreen.fromSource)
                            ) {
                                navigator.popUntilRoot()
                            }
                        }
                        .launchIn(this)
                }

                HandleOnNewIntent(context = context, navigator = navigator)

                CheckForUpdates()
                ShowOnboarding()

                if (didMigration) {
                    // SY -->
                    WhatsNewDialog(onDismissRequest = { didMigration = false })
                    // SY <--
                }
            }
        }

        val startTime = System.currentTimeMillis()
        splashScreen?.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            elapsed <= SPLASH_MIN_DURATION || (!ready && elapsed <= SPLASH_MAX_DURATION) || !migrationDone
        }
        setSplashScreenExitAnimation(splashScreen)

        if (isLaunch && libraryPreferences.autoClearChapterCache().get()) {
            lifecycleScope.launchIO {
                chapterCache.clear()
            }
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        when (val screen = navigator?.lastItem) {
            is AssistContentScreen -> {
                screen.onProvideAssistUrl()?.let { outContent.webUri = it.toUri() }
            }
        }
    }

    @Composable
    private fun HandleOnNewIntent(context: Context, navigator: Navigator) {
        LaunchedEffect(Unit) {
            callbackFlow {
                val componentActivity = context as ComponentActivity
                val consumer = Consumer<Intent> { trySend(it) }
                componentActivity.addOnNewIntentListener(consumer)
                awaitClose { componentActivity.removeOnNewIntentListener(consumer) }
            }
                .collectLatest { handleIntentAction(it, navigator) }
        }
    }

    @Composable
    private fun CheckForUpdates() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        // App updates
        LaunchedEffect(Unit) {
            if (BuildConfig.INCLUDE_UPDATER) {
                try {
                    val result = AppUpdateChecker().checkForUpdate(context)
                    if (result is GetApplicationRelease.Result.NewUpdate) {
                        val updateScreen = NewUpdateScreen(
                            versionName = result.release.version,
                            changelogInfo = result.release.info,
                            releaseLink = result.release.releaseLink,
                            downloadLink = result.release.getDownloadLink(),
                        )
                        navigator.push(updateScreen)
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e)
                }
            }
        }

        // Extensions updates
        LaunchedEffect(Unit) {
            try {
                ExtensionApi().checkForUpdates(context)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    @Composable
    private fun ShowOnboarding() {
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            if (!preferences.shownOnboardingFlow().get() && navigator.lastItem !is OnboardingScreen) {
                navigator.push(OnboardingScreen())
            }
        }
    }

    /**
     * Sets custom splash screen exit animation on devices prior to Android 12.
     *
     * When custom animation is used, status and navigation bar color will be set to transparent and will be restored
     * after the animation is finished.
     */
    @Suppress("Deprecation")
    private fun setSplashScreenExitAnimation(splashScreen: SplashScreen?) {
        val root = findViewById<View>(android.R.id.content)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && splashScreen != null) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT

            splashScreen.setOnExitAnimationListener { splashProvider ->
                // For some reason the SplashScreen applies (incorrect) Y translation to the iconView
                splashProvider.iconView.translationY = 0F

                val activityAnim = ValueAnimator.ofFloat(1F, 0F).apply {
                    interpolator = LinearOutSlowInInterpolator()
                    duration = SPLASH_EXIT_ANIM_DURATION
                    addUpdateListener { va ->
                        val value = va.animatedValue as Float
                        root.translationY = value * 16.dpToPx
                    }
                }

                val splashAnim = ValueAnimator.ofFloat(1F, 0F).apply {
                    interpolator = FastOutSlowInInterpolator()
                    duration = SPLASH_EXIT_ANIM_DURATION
                    addUpdateListener { va ->
                        val value = va.animatedValue as Float
                        splashProvider.view.alpha = value
                    }
                    doOnEnd {
                        splashProvider.remove()
                    }
                }

                activityAnim.start()
                splashAnim.start()
            }
        }
    }

    private fun handleIntentAction(intent: Intent, navigator: Navigator): Boolean {
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId > -1) {
            NotificationReceiver.dismissNotification(
                applicationContext,
                notificationId,
                intent.getIntExtra("groupId", 0),
            )
        }

        val tabToOpen = when (intent.action) {
            Constants.SHORTCUT_LIBRARY -> HomeScreen.Tab.Library()
            Constants.SHORTCUT_MANGA -> {
                val idToOpen = intent.extras?.getLong(Constants.MANGA_EXTRA) ?: return false
                navigator.popUntilRoot()
                HomeScreen.Tab.Library(idToOpen)
            }
            Constants.SHORTCUT_UPDATES -> HomeScreen.Tab.Updates
            Constants.SHORTCUT_HISTORY -> HomeScreen.Tab.History
            Constants.SHORTCUT_SOURCES -> HomeScreen.Tab.Browse(false)
            Constants.SHORTCUT_EXTENSIONS -> HomeScreen.Tab.Browse(true)
            Constants.SHORTCUT_DOWNLOADS -> {
                navigator.popUntilRoot()
                HomeScreen.Tab.More(toDownloads = true)
            }
            Intent.ACTION_SEARCH, Intent.ACTION_SEND, "com.google.android.gms.actions.SEARCH_ACTION" -> {
                // If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search *query* on *Tachiyomi*" in Google Search/Google Assistant)

                // Get the search query provided in extras, and if not null, perform a global search with it.
                val query = intent.getStringExtra(SearchManager.QUERY) ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!query.isNullOrEmpty()) {
                    navigator.popUntilRoot()
                    navigator.push(DeepLinkScreen(query))
                }
                null
            }
            INTENT_SEARCH -> {
                val query = intent.getStringExtra(INTENT_SEARCH_QUERY)
                if (!query.isNullOrEmpty()) {
                    val filter = intent.getStringExtra(INTENT_SEARCH_FILTER)
                    navigator.popUntilRoot()
                    navigator.push(GlobalSearchScreen(query, filter))
                }
                null
            }
            Intent.ACTION_VIEW -> {
                // Handling opening of backup files
                if (intent.data.toString().endsWith(".tachibk")) {
                    navigator.popUntilRoot()
                    navigator.push(RestoreBackupScreen(intent.data.toString()))
                }
                // Deep link to add extension repo
                else if ((intent.scheme == "com.shinku.reader" || intent.scheme == "tachiyomi" || intent.scheme == "mihon") && intent.data?.host == "add-repo") {
                    intent.data?.getQueryParameter("url")?.let { repoUrl ->
                        navigator.popUntilRoot()
                        navigator.push(ExtensionReposScreen(repoUrl))
                    }
                }
                null
            }
            else -> return false
        }

        if (tabToOpen != null) {
            lifecycleScope.launch { HomeScreen.openTab(tabToOpen) }
        }

        ready = true
        return true
    }

    // SY -->
    private fun addAnalytics() {
        if (!BuildConfig.DEBUG && isPreviewBuildType) {
            Firebase.analytics.setUserProperty("preview_version", syDebugVersion)
        }
    }
    // SY <--

    companion object {
        const val INTENT_SEARCH = "com.shinku.reader.SEARCH"
        const val INTENT_SEARCH_QUERY = "query"
        const val INTENT_SEARCH_FILTER = "filter"
    }
}

// Splash screen
private const val SPLASH_MIN_DURATION = 500 // ms
private const val SPLASH_MAX_DURATION = 5000 // ms
private const val SPLASH_EXIT_ANIM_DURATION = 400L // ms
