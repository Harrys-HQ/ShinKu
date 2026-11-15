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
import androidx.compose.ui.unit.dp
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
import com.shinku.reader.ui.browse.source.globalsearch.SearchScreenModel
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
    fun mangaDetailsPullRefreshInvokesRefreshOnPhoneAndTabletLayouts() {
        val refreshes = AtomicInteger()
        val state = com.shinku.reader.ui.manga.MangaScreenModel.State.Success(
            manga = Manga.create().copy(ogTitle = "Refresh fixture", source = 123L),
            source = com.shinku.reader.domain.source.model.StubSource(123L, "en", "Fixture"),
            isFromSource = true,
            chapters = emptyList(),
            availableScanlators = kotlinx.collections.immutable.persistentSetOf(),
            excludedScanlators = kotlinx.collections.immutable.persistentSetOf(),
            meta = null,
            mergedData = null,
            showRecommendationsInOverflow = true,
            showMergeInOverflow = true,
            showMergeWithAnother = false,
            pagePreviewsState = com.shinku.reader.ui.manga.PagePreviewState.Unused,
            alwaysShowReadingProgress = false,
            previewsRowCount = 0,
        )
        for (tablet in listOf(false, true)) {
            show {
                com.shinku.reader.presentation.manga.MangaScreen(
                    state = state,
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                    nextUpdate = null,
                    isTabletUi = tablet,
                    chapterSwipeStartAction = LibraryPreferences.ChapterSwipeAction.Disabled,
                    chapterSwipeEndAction = LibraryPreferences.ChapterSwipeAction.Disabled,
                    previewsRowCount = 0,
                    navigateUp = {},
                    onChapterClicked = {},
                    onDownloadChapter = null,
                    onAddToLibraryClicked = {},
                    onWebViewClicked = null,
                    onWebViewLongClicked = null,
                    onTrackingClicked = {},
                    onTagSearch = {},
                    onCopyTagToClipboard = {},
                    onFilterClicked = {},
                    onRefresh = { refreshes.incrementAndGet() },
                    onContinueReading = {},
                    onSearch = { _, _ -> },
                    onCoverClicked = {},
                    onShareClicked = null,
                    onDownloadActionClicked = null,
                    onEditCategoryClicked = null,
                    onEditIntervalClicked = null,
                    onMigrateClicked = null,
                    onEditNotesClicked = {},
                    onMetadataViewerClicked = {},
                    onEditInfoClicked = {},
                    onRecommendClicked = {},
                    onVibeClicked = {},
                    onMergedSettingsClicked = {},
                    onMergeClicked = {},
                    onMergeWithAnotherClicked = {},
                    onOpenPagePreview = {},
                    onMorePreviewsClicked = {},
                    onMultiBookmarkClicked = { _, _ -> },
                    onMultiMarkAsReadClicked = { _, _ -> },
                    onMarkPreviousAsReadClicked = {},
                    onMultiDeleteClicked = {},
                    onChapterSwipe = { _, _ -> },
                    onChapterSelected = { _, _, _, _ -> },
                    onAllChapterSelected = {},
                    onInvertSelection = {},
                    onGenerateRecap = null,
                    onDismissRecap = null,
                )
            }
            eventually { hasText("Refresh fixture") }
            val before = refreshes.get()
            val metrics = context.resources.displayMetrics
            val x = metrics.widthPixels * 3 / 4
            val y = metrics.heightPixels / 5
            instrumentation.uiAutomation.executeShellCommand("input swipe $x $y $x ${metrics.heightPixels * 4 / 5} 700").close()
            eventually { refreshes.get() == before + 1 }
        }
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

    @Test
    fun journeyActivityRangesUseOnlyReadingDaysInTheSelectedWindow() {
        val today = java.time.LocalDate.now()
        val zone = java.time.ZoneId.systemDefault()
        val history = listOf(today, today.minusDays(30)).associate {
            java.util.Date.from(it.atStartOfDay(zone).toInstant()) to 60_000L
        }
        showJourney { com.shinku.reader.presentation.more.stats.components.ActivityHeatmapCard(history) }
        eventually { hasText(context.getString(R.string.journey_activity_summary, 2, 84)) }
        click(context.getString(R.string.journey_activity_days, 7))
        eventually { hasText(context.getString(R.string.journey_activity_summary, 1, 7)) }
        click(context.getString(R.string.journey_activity_days, 28))
        eventually { hasText(context.getString(R.string.journey_activity_summary, 1, 28)) }
        screenshot("journey-activity-28-days")
        click(context.getString(R.string.journey_activity_days, 84))
        eventually { hasText(context.getString(R.string.journey_activity_summary, 2, 84)) }
    }

    @Test
    fun journeyEmptyHistoryExplainsActivityAndPatterns() {
        showJourney {
            androidx.compose.foundation.lazy.LazyColumn {
                item { com.shinku.reader.presentation.more.stats.components.ActivityHeatmapCard(emptyMap()) }
                item {
                    com.shinku.reader.presentation.more.stats.components.CircadianAndVelocityPanel(
                        com.shinku.reader.presentation.more.stats.data.StatsData.TimeStats(emptyMap()),
                        com.shinku.reader.presentation.more.stats.data.StatsData.VelocityStats(0.0, emptyMap()),
                    )
                }
            }
        }
        eventually { hasText(context.getString(R.string.journey_activity_empty)) }
        screenshot("journey-empty-activity")
        scrollTo(context.getString(R.string.journey_patterns_empty))
        screenshot("journey-empty-patterns")
    }

    @Test
    fun journeyTotalsAndAppearanceSampleKeepFullText() {
        showJourney {
            androidx.compose.foundation.lazy.LazyColumn {
                item {
                    com.shinku.reader.presentation.more.stats.components.JourneyOverviewMetrics(
                        libraryCount = "19,415", readDuration = "623 hours 5 minutes", completedCount = "12,345",
                    )
                }
                item {
                    com.shinku.reader.presentation.more.stats.components.SanctuaryBentoOverview(
                        overview = com.shinku.reader.presentation.more.stats.data.StatsData.Overview(19415, 12345, 0L),
                        titles = com.shinku.reader.presentation.more.stats.data.StatsData.Titles(0, 15000, 0),
                        chapters = com.shinku.reader.presentation.more.stats.data.StatsData.Chapters(1234567, 123456, 12345),
                        trackers = com.shinku.reader.presentation.more.stats.data.StatsData.Trackers(12345, 8.75, 3),
                    )
                }
                item { com.shinku.reader.presentation.more.settings.screen.AppearanceReadingPreview() }
            }
        }
        eventually { hasText("623 hours 5 minutes") }
        screenshot("journey-complete-totals")
        scrollTo(context.getString(R.string.journey_chapter_counts, "123,456", "1,234,567"))
        screenshot("journey-chapter-totals")
        scrollTo(context.getString(R.string.appearance_preview_sample))
        assertTrue(!hasText("Chapter 42 Ã‚Â· In progress"))
        scrollTo(context.getString(R.string.appearance_preview_summary))
        screenshot("appearance-sample-preview")
    }

    @Test
    fun sourceLanguagePickerSearchesAndOnlyAppliesConfirmedDraft() {
        val open = mutableStateOf(true)
        val selected = mutableStateOf(setOf("en", "custom-fixture"))
        val writes = AtomicInteger(0)
        showJourney {
            if (open.value) {
                com.shinku.reader.presentation.more.settings.widget.SourceLanguageDialog(
                    entries = linkedMapOf("en" to "English", "fr" to "French"),
                    selected = selected.value,
                    onApply = {
                        selected.value = it
                        writes.incrementAndGet()
                    },
                    onDismiss = { open.value = false },
                )
            } else {
                Text("Picker closed")
            }
        }
        click("French")
        click(context.getString(R.string.action_cancel))
        eventually { !open.value && hasText("Picker closed") }
        assertEquals(0, writes.get())
        assertEquals(setOf("en", "custom-fixture"), selected.value)
        instrumentation.runOnMainSync { open.value = true }
        eventually { hasText("French") }
        val search = nodes().last { it.isEditable }
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "fren")
        }
        assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
        eventually { hasText("French") && !hasText("English") }
        click("French")
        screenshot("boards13-language-search")
        click(context.getString(R.string.source_language_apply, 3))
        eventually { !open.value && writes.get() == 1 }
        assertEquals(setOf("en", "fr", "custom-fixture"), selected.value)
    }

    @Test
    fun performancePillsUseTheExistingStoredProfile() {
        val preference = Injekt.get<BasePreferences>().performanceMode()
        val restore = preserve(preference)
        try {
            preference.set(BasePreferences.PerformanceMode.STANDARD.value)
            showJourney { com.shinku.reader.presentation.more.settings.screen.PerformanceModeSelector(preference) }
            click(context.getString(R.string.pref_performance_mode_low_power))
            eventually { preference.get() == BasePreferences.PerformanceMode.LOW_POWER.value }
            screenshot("boards14-performance-pills")
            click(context.getString(R.string.pref_performance_mode_eink))
            eventually { preference.get() == BasePreferences.PerformanceMode.E_INK.value }
        } finally {
            restore()
        }
    }

    @Test
    fun bubbleInspectorChangesTheVisibleBubbleAndRetainsPageRetry() {
        val open = mutableStateOf(true)
        val retries = AtomicInteger(0)
        val blocks = listOf("Alpha", "Beta").map {
            com.shinku.reader.ui.reader.model.TranslatedBlock(
                originalText = "Original $it", translatedText = "Translated $it", boundingBox = null,
                bgColor = android.graphics.Color.WHITE, textColor = android.graphics.Color.BLACK,
            )
        }
        showJourney {
            if (open.value) {
                com.shinku.reader.presentation.reader.dialog.BubbleInspectorDialog(
                    pageNumber = 7, targetLanguage = "English", blocks = blocks,
                    onDismissRequest = { open.value = false }, onRetranslatePage = { retries.incrementAndGet() },
                )
            }
        }
        eventually { hasText("Original Alpha") && !hasText("Original Beta") }
        click(context.getString(R.string.reader_next_bubble))
        eventually { hasText("Original Beta") && !hasText("Original Alpha") }
        screenshot("boards07-bubble-inspector")
        click(context.getString(R.string.reader_previous_bubble))
        eventually { hasText("Original Alpha") }
        click(context.getString(R.string.reader_inspect_retry))
        eventually { !open.value && retries.get() == 1 }
    }

    @Test
    fun welcomeAndBackupPreviewUseReachableActionsAndRealCounts() {
        val setups = AtomicInteger(0)
        val restores = AtomicInteger(0)
        show { com.shinku.reader.presentation.more.onboarding.WelcomeScreen({ setups.incrementAndGet() }, { restores.incrementAndGet() }) }
        click(context.getString(R.string.pref_restore_backup))
        assertEquals(1, restores.get())
        assertEquals(0, setups.get())
        screenshot("boards09-welcome")
        click(context.getString(R.string.onboarding_setup))
        assertEquals(1, setups.get())
        showJourney {
            com.shinku.reader.presentation.more.settings.screen.data.BackupContentsCard(
                com.shinku.reader.data.backup.BackupContentsSummary(3, 1234L, 2, 17L, 0),
            )
        }
        eventually { hasText(context.getString(R.string.backup_contents_chapters, 1234L)) }
        assertTrue(hasText(context.getString(R.string.backup_contents_settings, 0)))
        screenshot("boards10-backup-contents")
    }

    @Test
    fun settingsHighlightTargetsVisibleRowsAfterDisabledPreferences() {
        com.shinku.reader.presentation.more.settings.screen.SearchableSettings.highlightKey = "Visible target"
        val before = (1..20).map { com.shinku.reader.presentation.more.settings.Preference.PreferenceItem.TextPreference(title = "Before $it") }
        val hidden = (1..20).map { com.shinku.reader.presentation.more.settings.Preference.PreferenceItem.TextPreference(title = "Hidden $it", enabled = false) }
        val target = com.shinku.reader.presentation.more.settings.Preference.PreferenceItem.TextPreference(title = "Visible target")
        val items = listOf(
            com.shinku.reader.presentation.more.settings.Preference.PreferenceItem.TextPreference(title = "Hidden top", enabled = false),
            com.shinku.reader.presentation.more.settings.Preference.PreferenceGroup(
                title = "Fixture group", preferenceItems = (before + hidden + target).toImmutableList(),
            ),
        )
        try {
            showJourney { com.shinku.reader.presentation.more.settings.PreferenceScreen(items) }
            eventually { hasText("Visible target") }
            assertTrue(!hasText("Hidden top") && !hasText("Hidden 1"))
            screenshot("boards12-highlight-target")
        } finally {
            com.shinku.reader.presentation.more.settings.screen.SearchableSettings.highlightKey = null
        }
    }

    @Test
    fun journeyShareStatisticsAppearOnlyAfterSelectionAndDisappearWhenRemoved() {
        val selected = mutableStateOf(emptySet<com.shinku.reader.presentation.more.stats.components.ShareStatistic>())
        val time = com.shinku.reader.presentation.more.stats.components.ShareStatistic.TIME
        val content = com.shinku.reader.presentation.more.stats.components.ShareCardContent(
            levelSummary = "Level 42 Ã‚Â· 123456 XP", levelProgress = 0.75f, persona = "Fixture persona",
            metrics = listOf(com.shinku.reader.presentation.more.stats.components.ShareCardMetric(time, "Reading time", "123456 hours 59 minutes")),
            genres = listOf("Fixture hidden genre"),
        )
        showJourney {
            androidx.compose.foundation.lazy.LazyColumn {
                item {
                    com.shinku.reader.presentation.more.stats.components.ShareCardOptions(selected.value) {
                        selected.value = if (it in selected.value) selected.value - it else selected.value + it
                    }
                }
                item {
                    com.shinku.reader.presentation.more.stats.components.SanctuaryShareCardPreview(
                        com.shinku.reader.presentation.more.stats.components.ShareCardTheme.OBSIDIAN, "", "Fixture Reader", "Chosen title",
                        content.selected(selected.value),
                    )
                }
            }
        }
        assertTrue(!hasText("123456 hours 59 minutes"))
        click(context.getString(R.string.journey_share_time))
        eventually { selected.value == setOf(time) }
        scrollTo("123456 hours 59 minutes")
        assertTrue(!hasText("Level 42 Ã‚Â· 123456 XP") && !hasText("Fixture persona") && !hasText("Fixture hidden genre"))
        screenshot("journey15-selected-preview")
        repeat(5) {
            nodes().lastOrNull { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            SystemClock.sleep(150)
        }
        click(context.getString(R.string.journey_share_time))
        eventually { selected.value.isEmpty() }
        scrollTo("Fixture Reader")
        eventually { !hasText("123456 hours 59 minutes") }
        screenshot("journey15-deselected-preview")
    }

    @Test
    fun journeyShareDialogKeepsActionsReachableWithoutSharing() {
        val open = mutableStateOf(true)
        val level = com.shinku.reader.domain.history.model.SanctuaryXpEngine.calculate(0, 0L, 0, 0)
        show {
            if (open.value) {
                com.shinku.reader.presentation.more.stats.components.SanctuaryShareCardDialog(
                    nickname = "A fixture reader with a deliberately long display name",
                    activeTitle = "A deliberately long selected reader title", levelInfo = level,
                    readDurationText = "123456 hours 59 minutes", chaptersRead = 987654, streakDays = 1234,
                    topGenres = listOf("Fixture genre"), unlockedBadgesCount = 999,
                    onDismiss = { open.value = false },
                )
            }
        }
        eventually { hasText(context.getString(R.string.journey_share_image)) }
        val closeBounds = Rect().also { bounds ->
            nodes().first { it.text?.toString() == context.getString(R.string.action_close) }.getBoundsInScreen(bounds)
        }
        val shareBounds = Rect().also { bounds ->
            nodes().first { it.text?.toString() == context.getString(R.string.journey_share_image) }.getBoundsInScreen(bounds)
        }
        assertTrue("Close stays below Share without overlapping it", closeBounds.top >= shareBounds.bottom)
        assertTrue("Close stays within the display", closeBounds.bottom <= context.resources.displayMetrics.heightPixels)
        screenshot("journey15-share-dialog")
        scrollTo(context.getString(R.string.journey_share_patterns))
        screenshot("journey15-share-options")
        click(context.getString(R.string.action_close))
        eventually { !open.value }
    }

    @Test
    fun journeyExportWrapsLongIdentityAndUsesSeparateFiles() {
        val metric = com.shinku.reader.presentation.more.stats.components.ShareCardMetric(
            com.shinku.reader.presentation.more.stats.components.ShareStatistic.TIME, "Reading time", "123456 hours and 59 minutes",
        )
        val content = com.shinku.reader.presentation.more.stats.components.ShareCardContent(
            levelSummary = "Level 42 Ã‚Â· 123456789 XP", levelProgress = 0f,
            persona = "A fixture reading persona that wraps without overlapping the identity",
            metrics = listOf(metric, metric.copy(statistic = com.shinku.reader.presentation.more.stats.components.ShareStatistic.CHAPTERS, label = "Chapters", value = "987654321")),
            genres = listOf("A long genre description", "Another long genre description", "Adventure"),
        )
        val avatarFile = File(context.cacheDir, "journey-fixture-avatar.png")
        val avatar = android.graphics.Bitmap.createBitmap(2000, 1000, android.graphics.Bitmap.Config.ARGB_8888)
        avatar.eraseColor(android.graphics.Color.GREEN)
        android.graphics.Canvas(avatar).apply {
            drawRect(0f, 0f, 500f, 1000f, android.graphics.Paint().apply { color = android.graphics.Color.RED })
            drawRect(1500f, 0f, 2000f, 1000f, android.graphics.Paint().apply { color = android.graphics.Color.BLUE })
        }
        avatarFile.outputStream().use { avatar.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        avatar.recycle()
        fun render(payload: com.shinku.reader.presentation.more.stats.components.ShareCardContent, avatarUri: String = "") = kotlinx.coroutines.runBlocking {
            com.shinku.reader.presentation.more.stats.components.renderAndSaveShareCard(
                context, avatarUri, "A fixture reader with a deliberately long display name that must wrap",
                "A deliberately long selected reader title that must stay inside the card", payload,
                0xFFB8A0FF.toInt(), 0xFF1F1638.toInt(), 0xFF0B0C12.toInt(), 0xFFECEFF4.toInt(),
            )
        }
        val identityUri = checkNotNull(render(content.selected(emptySet())))
        val selectedUri = checkNotNull(render(content, android.net.Uri.fromFile(avatarFile).toString()))
        assertTrue(identityUri != selectedUri)
        val output = File(context.getExternalFilesDir(null), "mockup-review").apply { mkdirs() }
        val heights = listOf("journey15-export-identity" to identityUri, "journey15-export-selected" to selectedUri).map { (name, uri) ->
            val bytes = checkNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytes() }
            File(output, "$name.png").writeBytes(bytes)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertEquals(960, bitmap.width)
            if (uri == selectedUri) {
                assertEquals("Avatar uses its center crop", android.graphics.Color.GREEN, bitmap.getPixel(85, 136))
                assertEquals("Avatar retains its image", android.graphics.Color.GREEN, bitmap.getPixel(187, 136))
            }
            bitmap.height.also { bitmap.recycle() }
        }
        assertTrue("Selected statistics expand export height", heights[1] > heights[0])
    }

    @Test
    fun milestoneCardsWrapTitlesAndKeepDetailsReachable() {
        val badge = com.shinku.reader.domain.history.model.Badge(
            id = "fixture", name = "A deliberately long milestone title with complete readable words",
            description = "A long milestone description that should wrap instead of being squeezed into a narrow fixed-height card.",
            progress = 0.55f, progressText = "123456 / 987654 chapters",
        )
        showJourney {
            androidx.compose.foundation.lazy.LazyColumn {
                item {
                    com.shinku.reader.presentation.more.stats.MilestoneSection(
                        com.shinku.reader.presentation.more.stats.data.StatsData.Milestones(listOf(badge, badge.copy(id = "earned", name = "Unlocked fixture", isEarned = true))),
                    )
                }
            }
        }
        scrollTo(badge.name)
        screenshot("journey15-milestone-grid")
        click(badge.name)
        eventually { hasText(badge.description) }
        screenshot("journey15-milestone-details")
        click("Close")
    }

    @Test
    fun emptyLibraryKeepsBrowseRestoreAndGuideReachable() {
        val browse = AtomicInteger()
        val restore = AtomicInteger()
        val guide = AtomicInteger()
        showJourney {
            com.shinku.reader.presentation.library.components.LibraryEmptyState(
                onBrowseSources = { browse.incrementAndGet() },
                onRestoreBackup = { restore.incrementAndGet() },
                onOpenGuide = { guide.incrementAndGet() },
                modifier = androidx.compose.ui.Modifier.padding(bottom = 84.dp),
            )
        }
        eventually { hasText(context.getString(R.string.library_empty_heading)) }
        scrollTo(context.getString(R.string.library_empty_browse_sources))
        screenshot("states16-library-empty")
        click(context.getString(R.string.library_empty_browse_sources))
        scrollTo(context.getString(R.string.library_empty_restore_backup))
        click(context.getString(R.string.library_empty_restore_backup))
        scrollTo(context.getString(R.string.getting_started_guide))
        screenshot("states16-library-actions")
        click(context.getString(R.string.getting_started_guide))
        assertEquals(1, browse.get())
        assertEquals(1, restore.get())
        assertEquals(1, guide.get())
    }

    @Test
    fun searchDistinguishesNoMatchesLoadingAndSourceFailures() {
        val source = StateFixtureSource(9101, "Fixture source")
        val result = mutableStateOf<com.shinku.reader.ui.browse.source.globalsearch.SearchItemResult>(
            com.shinku.reader.ui.browse.source.globalsearch.SearchItemResult.Success(emptyList()),
        )
        val reset = AtomicInteger()
        showJourney {
            com.shinku.reader.presentation.browse.GlobalSearchContent(
                items = kotlinx.collections.immutable.persistentMapOf(source to result.value),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                getManga = { androidx.compose.runtime.remember(it) { mutableStateOf(it) } },
                onClickSource = {}, onClickItem = {}, onLongClickItem = {},
                searchQuery = "A deliberately long fixture search query with complete readable words",
                progress = if (result.value == com.shinku.reader.ui.browse.source.globalsearch.SearchItemResult.Loading) 0 else 1,
                total = 1, onShowAllSources = { reset.incrementAndGet() },
            )
        }
        eventually { hasText(context.getString(R.string.search_no_matching_titles)) }
        screenshot("states16-no-results")
        click(context.getString(R.string.search_show_all_sources))
        assertEquals(1, reset.get())
        instrumentation.runOnMainSync { result.value = com.shinku.reader.ui.browse.source.globalsearch.SearchItemResult.Loading }
        eventually { !hasText(context.getString(R.string.search_no_matching_titles)) }
        screenshot("states16-search-loading")
        instrumentation.runOnMainSync {
            result.value = com.shinku.reader.ui.browse.source.globalsearch.SearchItemResult.Error(java.net.UnknownHostException("Fixture source unavailable"))
        }
        eventually { hasText(context.getString(R.string.search_sources_failed_without_results, 1)) }
        assertTrue(!hasText(context.getString(R.string.search_no_matching_titles)))
        screenshot("states16-search-failure")
    }

    @Test
    fun filteredSearchRetainsResultsAndReportsHiddenSourceFailures() {
        val source = StateFixtureSource(9101, "Available source")
        val failed = StateFixtureSource(9102, "Failed source")
        val manga = Manga.create().copy(id = 9101L, source = source.id, url = "/fixture", ogTitle = "Available fixture title")
        val toggle = AtomicInteger()
        val opened = AtomicInteger()
        val state = SearchScreenModel.State(
            searchQuery = "Fixture search", sourceFilter = com.shinku.reader.ui.browse.source.globalsearch.SourceFilter.All,
            onlyShowHasResults = true,
            items = kotlinx.collections.immutable.persistentMapOf(
                source to com.shinku.reader.ui.browse.source.globalsearch.SearchItemResult.Success(listOf(manga)),
                failed to com.shinku.reader.ui.browse.source.globalsearch.SearchItemResult.Error(java.io.IOException("Fixture failure")),
            ),
        )
        show {
            com.shinku.reader.presentation.browse.GlobalSearchScreen(
                state = state, navigateUp = {}, onChangeSearchQuery = {}, onSearch = {}, onChangeSearchFilter = {},
                onToggleResults = { toggle.incrementAndGet() }, onClickImageSearch = {},
                getManga = { androidx.compose.runtime.remember(it) { mutableStateOf(it) } },
                onClickSource = {}, onClickItem = { opened.incrementAndGet() }, onLongClickItem = {},
            )
        }
        eventually { hasText(context.getString(R.string.search_sources_unavailable, 1)) }
        click(context.getString(R.string.search_show_all_sources))
        assertEquals(1, toggle.get())
        scrollTo(manga.title)
        screenshot("states16-search-partial")
        click(manga.title)
        assertEquals(1, opened.get())
    }

    @Test
    fun discoverKeepsLoadedShelfBesideFailuresAndLoadingFeed() {
        val source = StateFixtureSource(9101, "Fixture source to review")
        val manga = Manga.create().copy(id = 9101L, source = source.id, url = "/fixture", ogTitle = "Available fixture title")
        val feed = com.shinku.reader.presentation.browse.FeedItemUI(
            com.shinku.reader.domain.source.model.FeedSavedSearch(9101L, source.id, null, true), null, source,
            "Pending fixture feed", "Still loading this source", null,
        )
        val state = mutableStateOf(
            com.shinku.reader.ui.browse.feed.FeedScreenState(
                forYouManga = kotlinx.collections.immutable.persistentListOf(manga),
                items = kotlinx.collections.immutable.persistentListOf(feed),
                failedSources = kotlinx.collections.immutable.persistentListOf(source), isInitialLoadDone = true,
            ),
        )
        val retries = AtomicInteger()
        val sources = AtomicInteger()
        val opened = AtomicInteger()
        showJourney {
            com.shinku.reader.presentation.browse.FeedScreen(
                state = state.value, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                onClickSavedSearch = { _, _ -> }, onClickSource = { sources.incrementAndGet() }, onClickDelete = {},
                onClickManga = { opened.incrementAndGet() }, onRefresh = { retries.incrementAndGet() },
                getMangaState = { androidx.compose.runtime.remember(it) { mutableStateOf(it) } },
            )
        }
        eventually { hasText(context.getString(R.string.feed_partial_failure, 1)) }
        screenshot("states16-discover-failure-summary")
        click(context.getString(R.string.action_retry))
        click(source.name)
        scrollTo(manga.title)
        screenshot("states16-discover-loaded")
        click(manga.title)
        assertEquals(1, opened.get())
        scrollTo("Pending fixture feed")
        screenshot("states16-discover-pending")
        instrumentation.runOnMainSync {
            state.value = state.value.copy(forYouManga = kotlinx.collections.immutable.persistentListOf(), items = kotlinx.collections.immutable.persistentListOf())
        }
        eventually { hasText(context.getString(R.string.feed_empty_failure)) }
        scrollTo(context.getString(R.string.feed_open_source))
        screenshot("states16-discover-unavailable")
        click(context.getString(R.string.action_retry))
        click(context.getString(R.string.feed_open_source))
        assertEquals(2, retries.get())
        assertEquals(2, sources.get())
    }

    @Test
    fun searchWithoutSelectedSourcesOffersTheRealSourceFilterAction() {
        val filters = AtomicInteger()
        show {
            com.shinku.reader.presentation.browse.GlobalSearchScreen(
                state = SearchScreenModel.State(searchQuery = "Fixture search", noSourcesSelected = true),
                navigateUp = {}, onChangeSearchQuery = {}, onSearch = {},
                onChangeSearchFilter = { if (it == com.shinku.reader.ui.browse.source.globalsearch.SourceFilter.All) filters.incrementAndGet() },
                onToggleResults = {}, onClickImageSearch = {},
                getManga = { androidx.compose.runtime.remember(it) { mutableStateOf(it) } },
                onClickSource = {}, onClickItem = {}, onLongClickItem = {},
            )
        }
        eventually { hasText(context.getString(R.string.search_no_sources_heading)) }
        assertTrue(!hasText(context.getString(R.string.search_no_matching_titles)))
        screenshot("states16-no-sources")
        click(context.getString(R.string.search_show_all_sources))
        assertEquals(1, filters.get())
    }

    private class StateFixtureSource(override val id: Long, override val name: String) : eu.kanade.tachiyomi.source.CatalogueSource {
        override val lang = "en"
        override val supportsLatest = true
        override fun getFilterList() = eu.kanade.tachiyomi.source.model.FilterList()
        override suspend fun getPopularManga(page: Int): eu.kanade.tachiyomi.source.model.MangasPage = error("No fixture network calls")
        override suspend fun getSearchManga(page: Int, query: String, filters: eu.kanade.tachiyomi.source.model.FilterList): eu.kanade.tachiyomi.source.model.MangasPage = error("No fixture network calls")
        override suspend fun getLatestUpdates(page: Int): eu.kanade.tachiyomi.source.model.MangasPage = error("No fixture network calls")
        override suspend fun getMangaDetails(manga: eu.kanade.tachiyomi.source.model.SManga): eu.kanade.tachiyomi.source.model.SManga = error("Unused")
        override suspend fun getChapterList(manga: eu.kanade.tachiyomi.source.model.SManga): List<eu.kanade.tachiyomi.source.model.SChapter> = error("Unused")
        override suspend fun getPageList(chapter: eu.kanade.tachiyomi.source.model.SChapter): List<eu.kanade.tachiyomi.source.model.Page> = error("Unused")
    }

    private fun showJourney(content: @Composable () -> Unit) {
        show {
            androidx.compose.material3.Scaffold { contentPadding ->
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(contentPadding)) {
                    content()
                }
            }
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
        eventually { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == context.packageName }
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
