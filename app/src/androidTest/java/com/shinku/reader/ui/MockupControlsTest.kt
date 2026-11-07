package com.shinku.reader.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shinku.reader.R
import com.shinku.reader.core.common.preference.TriState
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.model.Source
import com.shinku.reader.presentation.browse.MigrateSourceScreen
import com.shinku.reader.presentation.library.LibrarySettingsDialog
import com.shinku.reader.presentation.reader.ChapterListDialog
import com.shinku.reader.presentation.reader.ReaderPageActionsDialog
import com.shinku.reader.presentation.reader.dialog.TranslationOptionsContent
import com.shinku.reader.presentation.reader.settings.ReaderSettingsDialog
import com.shinku.reader.presentation.theme.TachiyomiTheme
import com.shinku.reader.ui.browse.migration.sources.MigrateSourceScreenModel
import com.shinku.reader.ui.library.LibrarySettingsScreenModel
import com.shinku.reader.ui.main.MainActivity
import com.shinku.reader.ui.reader.ReaderViewModel
import com.shinku.reader.ui.reader.chapter.ReaderChapterItem
import com.shinku.reader.ui.reader.setting.ReaderSettingsScreenModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

/** Controlled UI fixtures; the home check switches tabs without opening a title or starting reading. */
@RunWith(AndroidJUnit4::class)
class MockupControlsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        check(context.packageName.endsWith(".debug"))
        Injekt.get<BasePreferences>().shownOnboardingFlow().set(true)
        activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as MainActivity
    }

    @After
    fun cleanup() {
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finish() }
    }

    @Test
    fun libraryFilterChoicesAreExplicitAndResettable() {
        val preferences = Injekt.get<LibraryPreferences>()
        Injekt.get<BasePreferences>().downloadedOnly().set(false)
        val model = LibrarySettingsScreenModel()
        model.resetFilters()
        val open = mutableStateOf(true)
        show {
            Text("Fixture")
            if (open.value) LibrarySettingsDialog({ open.value = false }, model, null, false)
        }
        click("Include")
        eventually { preferences.filterDownloaded().get() == TriState.ENABLED_IS }
        click("Exclude")
        eventually { preferences.filterDownloaded().get() == TriState.ENABLED_NOT }
        screenshot("library-filters")
        eventually { hasText("Library filters") && hasText(context.getString(R.string.library_filters_active, 1)) }
        click("Reset")
        eventually { preferences.filterDownloaded().get() == TriState.DISABLED }
        eventually { hasText(context.getString(R.string.library_filters_active, 0)) }
        click("More filters")
        eventually { nodes().any { it.text?.toString() == "Started" } }
        screenshot("library05-more-filters-before-scroll")
        scrollTo("Started")
        scrollTo("Bookmarked")
        screenshot("library05-more-filters")
        click("Display")
        screenshot("library-display")
        click("Done")
        eventually { !open.value }
        model.onDispose()
    }

    @Test
    fun categoryCardsShowCountsAndKeepActionsAndEmptyAddReachable() {
        val created = AtomicInteger(0)
        val renamed = AtomicInteger(0)
        val deleted = AtomicInteger(0)
        val empty = mutableStateOf(false)
        val categories = listOf(
            com.shinku.reader.domain.category.model.Category(901, "Reading fixture", 0, 0),
            com.shinku.reader.domain.category.model.Category(902, "Queue fixture", 1, 0),
            com.shinku.reader.domain.category.model.Category(903, "Finished fixture", 2, 0),
        ).toImmutableList()
        show {
            com.shinku.reader.presentation.category.CategoryScreen(
                state = com.shinku.reader.ui.category.CategoryScreenState.Success(
                    categories = if (empty.value) kotlinx.collections.immutable.persistentListOf() else categories,
                    titleCounts = kotlinx.collections.immutable.persistentMapOf(901L to 19L, 902L to 1L, 903L to 0L),
                ),
                onClickCreate = { created.incrementAndGet() }, onClickRename = { renamed.incrementAndGet() },
                onClickDelete = { deleted.incrementAndGet() }, onChangeOrder = { _, _ -> }, navigateUp = {},
            )
        }
        eventually { hasText("19 titles") && hasText("1 title") && hasText("0 titles") }
        screenshot("library05-categories")
        click("Reading fixture")
        eventually { renamed.get() == 1 }
        val more = nodes().first { it.contentDescription?.toString() == "More" }
        var target = more
        while (!target.isClickable && target.parent != null) target = target.parent
        assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        click("Delete")
        eventually { deleted.get() == 1 && !hasText("Delete") }
        click("Add category")
        eventually { created.get() == 1 }
        instrumentation.runOnMainSync { empty.value = true }
        eventually { !hasText("Reading fixture") }
        click("Add category")
        eventually { created.get() == 2 }
        screenshot("library05-categories-empty")
    }

    @Test
    fun libraryBulkToolbarKeepsFourPrimaryActionsAndClosesOverflow() {
        val category = AtomicInteger(0)
        val read = AtomicInteger(0)
        val unread = AtomicInteger(0)
        val deletes = AtomicInteger(0)
        val migrate = AtomicInteger(0)
        val downloads = AtomicInteger(0)
        show {
            com.shinku.reader.presentation.core.components.material.Scaffold(
                bottomBar = {
                    com.shinku.reader.presentation.manga.components.LibraryBottomActionMenu(
                        visible = true,
                        onChangeCategoryClicked = { category.incrementAndGet() }, onMarkAsReadClicked = { read.incrementAndGet() },
                        onMarkAsUnreadClicked = { unread.incrementAndGet() }, onDeleteClicked = { deletes.incrementAndGet() },
                        onDownloadClicked = { downloads.incrementAndGet() }, onMigrateClicked = { migrate.incrementAndGet() },
                        onClickCleanTitles = null, onClickCollectRecommendations = null,
                        onClickAddToMangaDex = null, onClickResetInfo = null,
                    )
                },
            ) { padding ->
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(padding).fillMaxSize())
            }
        }
        eventually { hasText("Download") && hasText("Category") && hasText("Mark as read") && hasText("More") }
        val moreBounds = Rect().also(nodes().first { it.text?.toString() == "More" }::getBoundsInScreen)
        assertTrue(moreBounds.centerY() > context.resources.displayMetrics.heightPixels * 0.8)
        assertTrue(!hasText("Delete") && !hasText("Mark as unread"))
        screenshot("library05-bulk-toolbar")
        click("Category")
        click("Mark as read")
        eventually { category.get() == 1 && read.get() == 1 }
        click("Download")
        click(context.getString(R.string.download_unread))
        eventually { downloads.get() == 1 }
        click("More")
        screenshot("library05-bulk-more")
        click("Mark as unread")
        eventually { unread.get() == 1 && !hasText("Delete") }
        click("More")
        click("Delete")
        eventually { deletes.get() == 1 && !hasText("Delete") }
        click("More")
        click("Migrate")
        eventually { migrate.get() == 1 && !hasText("Delete") }
    }

    @Test
    fun libraryDownloadedOnlyKeepsFilterLockedAfterReset() {
        val base = Injekt.get<BasePreferences>()
        val restoreDownloaded = preserve(base.downloadedOnly())
        val model = LibrarySettingsScreenModel()
        val open = mutableStateOf(true)
        try {
            base.downloadedOnly().set(true)
            show {
                if (open.value) LibrarySettingsDialog({ open.value = false }, model, null, false)
            }
            eventually { hasText("Library filters") && hasText("Done") && hasText("Reset") }
            screenshot("library05-filters-locked")
            // The first card is Downloaded; its Any choice must remain disabled in downloaded-only mode.
            val any = nodes().first { it.text?.toString() == "Any" && it.isVisibleToUser }
            var target = any
            while (!target.isClickable && target.parent != null) target = target.parent
            assertTrue(!target.isEnabled || !target.isClickable)
            click("Reset")
            assertTrue(base.downloadedOnly().get())
            click("Done")
            eventually { !open.value }
        } finally {
            restoreDownloaded()
            model.onDispose()
        }
    }

    @Test
    fun categoryDragPersistsCategoryIndexRatherThanHeaderIndex() {
        val movedIndex = AtomicInteger(-1)
        val movedId = java.util.concurrent.atomic.AtomicLong(-1)
        show {
            com.shinku.reader.presentation.category.CategoryScreen(
                state = com.shinku.reader.ui.category.CategoryScreenState.Success(
                    categories = listOf(
                        com.shinku.reader.domain.category.model.Category(901, "First fixture", 0, 0),
                        com.shinku.reader.domain.category.model.Category(902, "Second fixture", 1, 0),
                        com.shinku.reader.domain.category.model.Category(903, "Third fixture", 2, 0),
                    ).toImmutableList(),
                ),
                onClickCreate = {}, onClickRename = {}, onClickDelete = {}, navigateUp = {},
                onChangeOrder = { category, index ->
                    movedId.set(category.id)
                    movedIndex.set(index)
                },
            )
        }
        eventually { nodes().any { it.contentDescription?.toString() == "Reorder First fixture" } }
        val first = Rect().also(nodes().first { it.contentDescription?.toString() == "Reorder First fixture" }::getBoundsInScreen)
        val second = Rect().also(nodes().first { it.contentDescription?.toString() == "Reorder Second fixture" }::getBoundsInScreen)
        val start = SystemClock.uptimeMillis()
        fun motion(action: Int, y: Float) {
            val event = android.view.MotionEvent.obtain(start, SystemClock.uptimeMillis(), action, first.exactCenterX(), y, 0)
                .apply { source = android.view.InputDevice.SOURCE_TOUCHSCREEN }
            try {
                assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        motion(android.view.MotionEvent.ACTION_DOWN, first.exactCenterY())
        SystemClock.sleep(600)
        for (step in 1..20) {
            motion(android.view.MotionEvent.ACTION_MOVE, first.exactCenterY() + (second.exactCenterY() - first.exactCenterY()) * step / 20)
            SystemClock.sleep(25)
        }
        motion(android.view.MotionEvent.ACTION_UP, second.exactCenterY())
        eventually { movedId.get() == 901L && movedIndex.get() == 1 }
        screenshot("library05-categories-reordered")
    }

    @Test
    fun readerTabsAndCacheConfirmationUseRealCallbacks() {
        val model = readerModel()
        val open = mutableStateOf(true)
        val clears = AtomicInteger(0)
        val overlay = mutableStateOf(false)
        show {
            Text("Fixture")
            if (open.value) {
                ReaderSettingsDialog(
                    onDismissRequest = { open.value = false },
                    onShowMenus = {}, onHideMenus = {}, screenModel = model,
                    translationContent = {
                        TranslationOptionsContent(
                            totalPages = 24, targetLanguage = "English", hasCachedTranslations = true,
                            isOverlayVisible = overlay.value, onToggleOverlay = { overlay.value = !overlay.value },
                            onDismissRequest = { open.value = false },
                            onTranslateCurrentPage = {}, onTranslateEntireChapter = {},
                            onChangeLanguages = {}, onClearTranslations = { clears.incrementAndGet() },
                        )
                    },
                )
            }
        }
        click("Display")
        screenshot("reader-display")
        click("Translation")
        click(context.getString(R.string.reader_show_translation_overlays))
        eventually { overlay.value && open.value }
        scrollTo(context.getString(R.string.clear_chapter_translations))
        click(context.getString(R.string.clear_chapter_translations))
        eventually { hasText(context.getString(R.string.clear_chapter_translations_question)) }
        screenshot("translation-clear-confirmation")
        assertEquals(0, clears.get())
        click("Cancel")
        assertEquals(0, clears.get())
        click(context.getString(R.string.clear_chapter_translations))
        click(context.getString(R.string.action_clear_translation_cache))
        eventually { clears.get() == 1 && !open.value }
    }

    @Test
    fun chapterSearchAndPagePickerKeepPageIndices() {
        val selected = AtomicInteger(-1)
        val open = mutableStateOf(true)
        val manga = Manga.create().copy(id = 90400, ogTitle = "Reader fixture")
        val chapters = (1..3).map { number ->
            ReaderChapterItem(
                Chapter.create().copy(id = 90400L + number, mangaId = manga.id, name = "Fixture chapter $number"),
                manga, number == 2, DateTimeFormatter.ISO_LOCAL_DATE,
            )
        }.toImmutableList()
        val model = readerModel()
        show {
            Text("Fixture")
            if (open.value) {
                ChapterListDialog(
                    onDismissRequest = { open.value = false }, screenModel = model, chapters = chapters,
                    onClickChapter = {}, onBookmark = {}, dateRelativeTime = false,
                    currentPage = 12, totalPages = 24, onClickPage = selected::set,
                )
            }
        }
        eventually { hasText("Fixture chapter 2") }
        val search = nodes().last {
            it.isEditable && it.actionList.any { action -> action.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
        }
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "chapter 3")
        }
        assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
        screenshot("chapter-search-input-diagnostic")
        android.util.Log.i("MockupSearch", "Editable actions=${nodes().filter { it.isEditable }.map { it.actionList.map { action -> action.id } }}")
        eventually { hasText("Fixture chapter 3") && !hasText("Fixture chapter 2") }
        screenshot("chapter-search")
        click("Pages")
        eventually {
            val current = nodes().firstOrNull { it.text?.toString() == "12" } ?: return@eventually false
            val viewport = nodes().lastOrNull { it.isScrollable } ?: return@eventually false
            val pageBounds = Rect().also(current::getBoundsInScreen)
            val viewportBounds = Rect().also(viewport::getBoundsInScreen)
            pageBounds.top >= viewportBounds.top && pageBounds.bottom <= viewportBounds.bottom
        }
        screenshot("page-picker")
        click("14")
        eventually { selected.get() == 13 && !open.value }
    }

    @Test
    fun homeDockSwitchesTabsAndKeepsLabelsVisible() {
        click("Library", bottomMost = true)
        eventually { hasText("Done") }
        click("Done")
        screenshot("dock-library")
        listOf("Updates", "History", "Browse", "More", "Library").forEach { tab ->
            click(tab, bottomMost = true)
            SystemClock.sleep(700)
            screenshot("dock-${tab.lowercase()}")
        }
    }

    @Test
    fun pageActionsKeepSaveReachableAndConfirmCoverReplacement() {
        val open = mutableStateOf(true)
        val saves = AtomicInteger(0)
        val covers = AtomicInteger(0)
        show {
            Text("Fixture")
            if (open.value) {
                ReaderPageActionsDialog(
                    onDismissRequest = { open.value = false },
                    onSetAsCover = { covers.incrementAndGet() },
                    onShare = { _, _ -> }, onSave = { saves.incrementAndGet() },
                    onShareCombined = {}, onSaveCombined = {}, onFootnotes = {}, onTranslation = {},
                    hasExtraPage = false,
                )
            }
        }
        eventually { hasText("Save") }
        screenshot("page-actions")
        click("Set Cover")
        eventually { hasText(context.getString(R.string.confirm_set_image_as_cover)) }
        assertEquals(0, covers.get())
        click("Cancel")
        assertEquals(0, covers.get())
        click("Save")
        eventually { saves.get() == 1 && !open.value }
    }

    @Test
    fun migrationFilterShowsOnlyMissingSourcesAndOpensFailedUpdates() {
        val opens = AtomicInteger(0)
        val sources = listOf(
            Source(90411, "en", "Fixture missing", false, true) to 2L,
            Source(90412, "en", "Fixture installed", false, false) to 3L,
        ).toImmutableList()
        show {
            MigrateSourceScreen(
                state = MigrateSourceScreenModel.State(isLoading = false, items = sources),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                onClickItem = {}, onFailedUpdates = { opens.incrementAndGet() },
                onToggleSortingDirection = {}, onToggleSortingMode = {}, onClickAll = {},
            )
        }
        eventually { hasText("Fixture missing") && hasText("Fixture installed") }
        click("Missing sources (1)")
        eventually { hasText("Fixture missing") && !hasText("Fixture installed") }
        screenshot("migration-missing")
        click("Failed updates")
        eventually { opens.get() == 1 }
    }

    @Test
    fun sensitivePreferencesRequireExplicitRevealAndCancelDoesNotSave() {
        val saves = AtomicInteger(0)
        val secret = "synthetic-test-key-90400"
        show {
            com.shinku.reader.presentation.more.settings.widget.EditTextPreferenceWidget(
                title = "Fixture API key", subtitle = "%s", icon = null,
                value = secret, isSensitive = true, onConfirm = {
                    saves.incrementAndGet()
                    true
                },
            )
        }
        eventually { hasText(context.getString(R.string.secret_configured)) }
        assertTrue(!hasText(secret))
        click("Fixture API key")
        eventually { nodes().any { it.isPassword } }
        val reveal = context.getString(R.string.secret_show)
        eventually { nodes().any { it.contentDescription?.toString() == reveal } }
        var revealButton = nodes().first { it.contentDescription?.toString() == reveal }
        while (!revealButton.isClickable && revealButton.parent != null) revealButton = revealButton.parent
        assertTrue(revealButton.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        eventually { hasText(secret) }
        screenshot("api-key-explicit-reveal-fixture")
        click("Cancel")
        eventually { !hasText(secret) }
        assertEquals(0, saves.get())
        click("Fixture API key")
        eventually { nodes().any { it.isPassword } }
        click("Cancel")
    }

    @Test
    fun shinkuHubRoutesToEverySettingsSection() {
        var navigator: cafe.adriel.voyager.navigator.Navigator? = null
        show {
            cafe.adriel.voyager.navigator.Navigator(
                com.shinku.reader.presentation.more.settings.screen.SettingsShinKuSettingsScreen,
            ) {
                navigator = it
                cafe.adriel.voyager.navigator.CurrentScreen()
            }
        }
        eventually { hasText(context.getString(R.string.settings_save_automatically)) }
        screenshot("shinku-settings-hub")
        for (section in com.shinku.reader.presentation.more.settings.screen.ShinKuSettingsSection.entries) {
            val title = context.getString(section.titleRes.resourceId)
            scrollTo(title)
            click(title)
            eventually {
                (navigator?.lastItem as? com.shinku.reader.presentation.more.settings.screen.ShinKuSettingsSectionScreen)?.section == section
            }
            eventually { !hasText(context.getString(R.string.settings_save_automatically)) }
            screenshot("shinku-section-${section.name.lowercase()}")
            instrumentation.runOnMainSync { navigator!!.pop() }
            eventually { hasText(context.getString(R.string.settings_save_automatically)) }
        }
        instrumentation.runOnMainSync {
            navigator!!.push(com.shinku.reader.presentation.more.settings.screen.SettingsSearchScreen())
        }
        eventually { nodes().any { it.isEditable } }
        val search = nodes().last { it.isEditable }
        val query = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "predictive")
        }
        assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, query))
        val result = context.getString(com.shinku.reader.i18n.sy.SYMR.strings.pref_predictive_loading.resourceId)
        eventually { hasText(result) }
        click(result)
        eventually {
            (navigator?.lastItem as? com.shinku.reader.presentation.more.settings.screen.ShinKuSettingsSectionScreen)?.section ==
                com.shinku.reader.presentation.more.settings.screen.ShinKuSettingsSection.PERFORMANCE
        }
        eventually { hasText(result) }
        eventually { nodes().none { it.isEditable } }
        screenshot("shinku-search-performance")
    }

    @Test
    fun onboardingRestoreStillRequiresStorageSetup() {
        val pref = Injekt.get<com.shinku.reader.domain.storage.service.StoragePreferences>().baseStorageDirectory()
        val wasSet = pref.isSet()
        val previous = pref.get()
        val restores = AtomicInteger(0)
        val finishes = AtomicInteger(0)
        try {
            pref.delete()
            show {
                com.shinku.reader.presentation.more.onboarding.OnboardingScreen(
                    onComplete = { finishes.incrementAndGet() },
                    onRestoreBackup = { restores.incrementAndGet() },
                )
            }
            click(context.getString(R.string.pref_restore_backup))
            eventually { hasText(context.getString(R.string.onboarding_step_counter, 1, 4)) }
            val next = context.getString(R.string.onboarding_action_next)
            eventually { hasText(next) }
            var node = nodes().first { it.text?.toString() == next }
            while (!node.isClickable && node.parent != null) node = node.parent
            assertTrue(!node.isEnabled)
            assertEquals(0, restores.get())
            assertEquals(0, finishes.get())
            screenshot("onboarding-storage-required")
        } finally {
            if (wasSet) pref.set(previous) else pref.delete()
        }
    }

    @Test
    fun restoreConfirmationOnlyStartsAfterExplicitConfirmation() {
        val starts = AtomicInteger(0)
        val open = mutableStateOf(true)
        show {
            Text("Restore fixture")
            if (open.value) {
                com.shinku.reader.presentation.more.settings.screen.data.RestoreConfirmationDialog(
                    onDismiss = { open.value = false },
                    onConfirm = {
                        starts.incrementAndGet()
                        open.value = false
                    },
                )
            }
        }
        eventually { hasText(context.getString(R.string.restore_confirm_title)) }
        screenshot("restore-confirmation")
        click("Cancel")
        eventually { !open.value }
        assertEquals(0, starts.get())
        instrumentation.runOnMainSync { open.value = true }
        click(context.getString(R.string.action_restore))
        eventually { starts.get() == 1 && !open.value }
    }

    @Test
    fun coverThemeCanBeDisabledIndependentlyOfReaderMoodLighting() {
        val preferences = Injekt.get<com.shinku.reader.exh.source.ShinKuPreferences>()
        val ui = Injekt.get<com.shinku.reader.domain.ui.UiPreferences>()
        val restore = listOf(
            preserve(preferences.moodLighting()), preserve(preferences.dynamicLibraryTheme()),
            preserve(preferences.dynamicLibrarySeedColor()), preserve(ui.appTheme()),
        )
        val palette = java.util.concurrent.atomic.AtomicReference<String>()
        try {
            preferences.moodLighting().set(false)
            preferences.dynamicLibraryTheme().set(false)
            preferences.dynamicLibrarySeedColor().set(0xFF1565C0.toInt())
            ui.appTheme().set(com.shinku.reader.domain.ui.model.AppTheme.LAVENDER)
            show {
                cafe.adriel.voyager.navigator.Navigator(
                    com.shinku.reader.presentation.more.settings.screen.SettingsColorEffectsScreen,
                ) {
                    val colors = androidx.compose.material3.MaterialTheme.colorScheme
                    val marker = "${colors.primary.toArgb()}:${colors.surface.toArgb()}:${colors.background.toArgb()}"
                    androidx.compose.runtime.SideEffect { palette.set(marker) }
                    cafe.adriel.voyager.navigator.CurrentScreen()
                }
            }
            eventually { palette.get() != null && hasText(context.getString(R.string.color_effects_scope)) }
            SystemClock.sleep(800)
            val staticPalette = palette.get()
            preferences.dynamicLibrarySeedColor().set(0xFF2E7D32.toInt())
            SystemClock.sleep(800)
            assertEquals(staticPalette, palette.get())
            click(context.getString(R.string.reader_mood_lighting))
            eventually { preferences.moodLighting().get() }
            assertTrue(!preferences.dynamicLibraryTheme().get())
            assertEquals(staticPalette, palette.get())
            screenshot("issue-26-reader-on-app-theme-off")
            click(context.getString(R.string.reader_mood_lighting))
            eventually { !preferences.moodLighting().get() }
            click(context.getString(R.string.cover_based_app_theme))
            eventually { preferences.dynamicLibraryTheme().get() && palette.get() != staticPalette }
            assertTrue(!preferences.moodLighting().get())
            SystemClock.sleep(800)
            val firstDynamicPalette = palette.get()
            preferences.dynamicLibrarySeedColor().set(0xFF1565C0.toInt())
            eventually { palette.get() != firstDynamicPalette }
            click(context.getString(R.string.cover_based_app_theme))
            eventually { !preferences.dynamicLibraryTheme().get() && palette.get() == staticPalette }
            preferences.dynamicLibrarySeedColor().set(0xFFD84315.toInt())
            SystemClock.sleep(800)
            assertEquals(staticPalette, palette.get())
            screenshot("issue-26-static-app-colors")
        } finally {
            restore.forEach { it() }
        }
    }

    @Test
    fun geminiModelPickerOffersCurrentModelsAndRetainsCustomIds() {
        val preferences = Injekt.get<com.shinku.reader.exh.source.ShinKuPreferences>()
        val restore = preserve(preferences.geminiModel())
        val loads = AtomicInteger(0)
        try {
            preferences.geminiModel().set("auto")
            show {
                com.shinku.reader.presentation.more.settings.PreferenceScreen(
                    items = com.shinku.reader.presentation.more.settings.widget.geminiModelPreferences(
                        modelPreference = preferences.geminiModel(), apiKey = "fixture-key",
                        loadModels = { _, _ ->
                            loads.incrementAndGet()
                            listOf(
                                com.shinku.reader.domain.ai.GeminiModel("gemini-3.6-flash", "Gemini 3.6 Flash"),
                                com.shinku.reader.domain.ai.GeminiModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite"),
                            )
                        },
                    ),
                )
            }
            eventually { loads.get() == 1 }
            scrollTo(context.getString(R.string.pref_gemini_model))
            click(context.getString(R.string.pref_gemini_model))
            val flash = "Gemini 3.6 Flash (gemini-3.6-flash)"
            eventually { hasText(flash) && hasText("Gemini 2.5 Flash-Lite (gemini-2.5-flash-lite)") }
            assertTrue(!hasText("Gemini 3.5 Flash-Lite (gemini-3.5-flash-lite)"))
            screenshot("gemini-key-specific-models")
            click(flash)
            eventually { preferences.geminiModel().get() == "gemini-3.6-flash" }
            scrollTo(context.getString(R.string.gemini_custom_model))
            click(context.getString(R.string.gemini_custom_model))
            eventually { nodes().any { it.isEditable } }
            val args = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "models/gemini-future-flash")
            }
            assertTrue(nodes().last { it.isEditable }.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
            click(context.getString(R.string.action_ok))
            eventually { preferences.geminiModel().get() == "models/gemini-future-flash" }
            click(context.getString(R.string.pref_gemini_model))
            eventually { hasText("models/gemini-future-flash (saved model ID)") }
            click(context.getString(R.string.action_cancel))
        } finally {
            restore()
        }
    }

    @Test
    fun modelCatalogChangesWithKeyAndRetainsRefreshFailureDetails() {
        val preferences = Injekt.get<com.shinku.reader.exh.source.ShinKuPreferences>()
        val restore = preserve(preferences.geminiModel())
        val key = mutableStateOf("fixture-key-a")
        val loads = AtomicInteger(0)
        try {
            preferences.geminiModel().set("auto")
            show {
                com.shinku.reader.presentation.more.settings.PreferenceScreen(
                    items = com.shinku.reader.presentation.more.settings.widget.geminiModelPreferences(
                        modelPreference = preferences.geminiModel(), apiKey = key.value,
                        loadModels = { currentKey, refresh ->
                            loads.incrementAndGet()
                            if (refresh) throw java.io.IOException("HTTP 403 (PERMISSION_DENIED): Fixture access error")
                            if (currentKey == "fixture-key-a") {
                                listOf(com.shinku.reader.domain.ai.GeminiModel("gemini-3.6-flash", "Fixture Flash"))
                            } else {
                                listOf(com.shinku.reader.domain.ai.GeminiModel("gemini-3.8-flash", "Fixture New Flash"))
                            }
                        },
                    ),
                )
            }
            eventually { loads.get() == 1 }
            click(context.getString(R.string.pref_gemini_model))
            eventually { hasText("Fixture Flash (gemini-3.6-flash)") }
            click(context.getString(R.string.action_cancel))
            instrumentation.runOnMainSync { key.value = "fixture-key-b" }
            eventually { loads.get() == 2 }
            click(context.getString(R.string.pref_gemini_model))
            eventually { hasText("Fixture New Flash (gemini-3.8-flash)") }
            assertTrue(!hasText("Fixture Flash (gemini-3.6-flash)"))
            click(context.getString(R.string.action_cancel))
            click(context.getString(R.string.gemini_catalog_refresh))
            eventually { hasText("HTTP 403 (PERMISSION_DENIED): Fixture access error") }
            assertEquals(3, loads.get())
            screenshot("gemini-catalog-error-details")
        } finally {
            restore()
        }
    }

    private fun <T> preserve(preference: com.shinku.reader.core.common.preference.Preference<T>): () -> Unit {
        val wasSet = preference.isSet()
        val value = preference.get()
        return { if (wasSet) preference.set(value) else preference.delete() }
    }

    private fun readerModel() = ReaderSettingsScreenModel(MutableStateFlow(ReaderViewModel.State()), {}, {})

    private fun show(content: @Composable () -> Unit) {
        instrumentation.runOnMainSync { activity.setContent { TachiyomiTheme(content = content) } }
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.packageName?.toString() == context.packageName) result.add(node)
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        visit(root)
        return result
    }

    private fun hasText(text: String) = nodes().any { it.text?.toString() == text && it.isVisibleToUser }

    private fun click(text: String, bottomMost: Boolean = false) {
        if (bottomMost) {
            eventually { hasText(text) }
            val candidate = nodes().filter { it.text?.toString() == text && it.isVisibleToUser }
                .maxBy { Rect().also(it::getBoundsInScreen).bottom }
            val bounds = Rect().also(candidate::getBoundsInScreen)
            android.util.Log.i("MockupDock", "$text clickable=${candidate.isClickable} selected=${candidate.isSelected} bounds=$bounds actions=${candidate.actionList.map { it.id }}")
            check(instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == context.packageName)
            check(bounds.bottom > context.resources.displayMetrics.heightPixels * 0.8)
            val downTime = SystemClock.uptimeMillis()
            listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP).forEach { action ->
                val event = android.view.MotionEvent.obtain(
                    downTime, SystemClock.uptimeMillis(), action, bounds.exactCenterX(), bounds.exactCenterY(), 0,
                ).apply { source = android.view.InputDevice.SOURCE_TOUCHSCREEN }
                try {
                    assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
                } finally {
                    event.recycle()
                }
                SystemClock.sleep(60)
            }
            return
        }
        fun target(): AccessibilityNodeInfo? {
            val candidates = nodes().filter { it.text?.toString() == text && it.isVisibleToUser }
                .mapNotNull {
                    var candidate = it
                    while (!candidate.isClickable && candidate.parent != null) candidate = candidate.parent
                    candidate.takeIf { it.isClickable && it.isEnabled }
                }
            return if (bottomMost) {
                candidates.maxByOrNull {
                    Rect().also(it::getBoundsInScreen).bottom
                }
            } else {
                candidates.firstOrNull()
            }
        }
        eventually { target() != null }
        val node = checkNotNull(target())
        check(node.packageName?.toString() == context.packageName)
        assertTrue("Click $text", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun scrollTo(text: String) {
        repeat(8) {
            if (hasText(text)) return
            val scrollable = nodes().lastOrNull { node ->
                node.isScrollable && node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id }
            } ?: nodes().lastOrNull { it.isScrollable }
            scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(300)
        }
        eventually { hasText(text) }
    }

    private fun screenshot(name: String) {
        SystemClock.sleep(500)
        val root = instrumentation.uiAutomation.rootInActiveWindow
        check(root?.packageName?.toString() == context.packageName)
        val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = checkNotNull(context.getExternalFilesDir("mockup-review")).apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    private fun eventually(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        assertTrue("UI condition timed out", condition())
    }
}
