package com.shinku.reader.ui

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.category.components.genre.SortTagListItem
import com.shinku.reader.presentation.components.BatchInputConfirmation
import com.shinku.reader.presentation.components.SourceMetadataCard
import com.shinku.reader.presentation.core.theme.LocalSanctuaryStaticContrast
import com.shinku.reader.presentation.crash.CrashRecoveryContent
import com.shinku.reader.presentation.library.DeleteLibraryMangaDialog
import com.shinku.reader.presentation.more.settings.screen.about.WhatsNewDialog
import com.shinku.reader.presentation.reader.settings.ReaderSettingsDialog
import com.shinku.reader.presentation.security.UnlockScreen
import com.shinku.reader.presentation.theme.TachiyomiTheme
import com.shinku.reader.ui.main.MainActivity
import com.shinku.reader.ui.reader.ReaderViewModel
import com.shinku.reader.ui.reader.setting.ReaderSettingsScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import com.shinku.reader.core.common.i18n.stringResource as localized

@RunWith(AndroidJUnit4::class)
class RemainingBoardsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var activity: MainActivity
    private var previousNightMode: Int? = null

    @Before fun setup() {
        check(context.packageName.endsWith(".debug"))
        if (InstrumentationRegistry.getArguments().getString("reviewLight") == "true") {
            previousNightMode = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode()
            instrumentation.runOnMainSync {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        Injekt.get<BasePreferences>().shownOnboardingFlow().set(true)
        activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        if (previousNightMode != null) {
            assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_NO, activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
        }
    }

    @After fun cleanup() {
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finish() }
        previousNightMode?.let { mode -> instrumentation.runOnMainSync { androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode) } }
    }

    @Test fun removalRequiresChoiceAndKeepsDeletionScopesSeparate() {
        val cases = listOf(Triple(false, true, false), Triple(false, false, true), Triple(false, true, true), Triple(true, true, false))
        cases.forEachIndexed { index, (local, library, downloads) ->
            val open = mutableStateOf(true)
            var confirmed: Pair<Boolean, Boolean>? = null
            show {
                androidx.compose.runtime.key(index) {
                    if (open.value) {
                        DeleteLibraryMangaDialog(local, 3, { open.value = false }) { a, b -> confirmed = a to b }
                    } else {
                        Text("Closed fixture")
                    }
                }
            }
            await { has(context.localized(MR.strings.remove_content_heading)) }
            assertFalse(target(context.localized(MR.strings.action_remove)).isEnabled)
            if (local) assertFalse(has(context.localized(MR.strings.downloaded_chapters)))
            if (library) click(context.localized(MR.strings.manga_from_library))
            if (downloads) click(context.localized(MR.strings.downloaded_chapters))
            val label = when {
                library && downloads -> MR.strings.remove_content_both
                downloads -> MR.strings.remove_content_downloads
                else -> MR.strings.action_remove
            }
            if (index == 2) screenshot("boards17-remove-scopes")
            click(context.localized(label))
            await { has("Closed fixture") }
            assertEquals(library to downloads, confirmed)
        }
    }

    @Test fun crashRecoveryKeepsRestartAndCopyReachableWithoutSharingLogs() {
        val restart = AtomicInteger()
        val copy = AtomicInteger()
        val share = AtomicInteger()
        show { CrashRecoveryContent((1..16).joinToString("\n") { "Fixture diagnostic line $it with deliberately long readable error details" }, { restart.incrementAndGet() }, { copy.incrementAndGet() }, { share.incrementAndGet() }) }
        await { has(context.localized(MR.strings.recovery_copy_error)) }
        click(context.localized(MR.strings.crash_screen_restart_application))
        click(context.localized(MR.strings.recovery_copy_error))
        scrollTo(context.localized(MR.strings.recovery_share_diagnostics), context.localized(MR.strings.crash_screen_restart_application))
        screenshot("boards18-recovery")
        click(context.localized(MR.strings.recovery_share_diagnostics))
        assertEquals(1, restart.get())
        assertEquals(1, copy.get())
        assertEquals(1, share.get())
    }

    @Test fun lockedBackgroundContainsGuidanceAndNoReadingContent() {
        show { UnlockScreen() }
        await { has(context.localized(MR.strings.unlock_device_guidance)) }
        screenshot("boards17-lock-background")
        assertFalse(has("Fixture private title"))
    }

    @Test fun sourceMetadataWrapsAndRetainsLongPressCopy() {
        val copied = AtomicInteger()
        val value = "A long source metadata value with readable words and multiple lines. ".repeat(8)
        page { SourceMetadataCard("Source-provided description", value) { copied.incrementAndGet() } }
        await { has(value) }
        screenshot("boards21-source-metadata")
        var node = nodes().first { it.text?.toString() == value }
        while (!node.isLongClickable && node.parent != null) node = node.parent
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK))
        assertEquals(1, copied.get())
    }

    @Test fun batchReviewOnlyAddsAfterExplicitConfirmation() {
        val added = AtomicInteger()
        val cancelled = AtomicInteger()
        val open = mutableStateOf(true)
        show {
            if (open.value) {
                BatchInputConfirmation("https://e-hentai.org/g/123/fixture/\nhttps://e-hentai.org/g/456/fixture/", {
                    added.incrementAndGet()
                    open.value = false
                }, {
                    cancelled.incrementAndGet()
                    open.value = false
                })
            } else {
                Text("Closed fixture")
            }
        }
        await { has(context.localized(MR.strings.batch_add_confirm_title)) }
        assertEquals(0, added.get())
        screenshot("boards23-batch-review")
        click(context.localized(MR.strings.action_cancel))
        await { has("Closed fixture") }
        assertEquals(1, cancelled.get())
        assertEquals(0, added.get())
        instrumentation.runOnMainSync { open.value = true }
        click(context.localized(SYMR.strings.eh_batch_add_button))
        assertEquals(1, added.get())
    }

    @Test fun organizationButtonsExposeLabelsAndDisabledOrderBounds() {
        val moved = AtomicInteger()
        val deleted = AtomicInteger()
        page { SortTagListItem(Modifier, "A deliberately long source category or sorting tag that must wrap", true, false, { moved.incrementAndGet() }, {}, { deleted.incrementAndGet() }) }
        await { nodes().any { it.contentDescription?.toString() == context.localized(MR.strings.organize_move_up) } }
        assertFalse(target(context.localized(MR.strings.organize_move_down)).isEnabled)
        click(context.localized(MR.strings.organize_move_up))
        click(context.localized(MR.strings.action_delete))
        screenshot("boards23-organization")
        assertEquals(1, moved.get())
        assertEquals(1, deleted.get())
    }

    @Test fun einkProfileUsesMonochromeAndRestoresThePreviousPreference() {
        val preference = Injekt.get<BasePreferences>().performanceMode()
        val wasSet = preference.isSet()
        val old = preference.get()
        val flag = AtomicInteger()
        val color = AtomicInteger()
        try {
            preference.set(BasePreferences.PerformanceMode.E_INK.value)
            page {
                val static = LocalSanctuaryStaticContrast.current
                val primary = MaterialTheme.colorScheme.primary.toArgb()
                SideEffect {
                    color.set(primary)
                    flag.set(if (static) 1 else 0)
                }
                SourceMetadataCard("E-Ink profile", "Static surfaces with clear boundaries") {}
            }
            await { flag.get() == 1 }
            val rgb = color.get()
            assertEquals(android.graphics.Color.red(rgb), android.graphics.Color.green(rgb))
            assertEquals(android.graphics.Color.green(rgb), android.graphics.Color.blue(rgb))
            screenshot("boards20-eink")
            preference.set(BasePreferences.PerformanceMode.STANDARD.value)
            await { flag.get() == 0 }
        } finally {
            if (wasSet) preference.set(old) else preference.delete()
        }
    }

    @Test fun readerControlsAdaptToWideLandscapeAndKeepDoneReachable() {
        val open = mutableStateOf(true)
        val model = ReaderSettingsScreenModel(MutableStateFlow(ReaderViewModel.State()), {}, {})
        page(scroll = false) { if (open.value) ReaderSettingsDialog({ open.value = false }, {}, {}, model) { Text("Fixture translation settings") } }
        await { has(context.localized(MR.strings.action_done)) }
        if (context.resources.configuration.screenWidthDp >= 600 && context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            val bounds = Rect().also(target(context.localized(MR.strings.action_done))::getBoundsInScreen)
            assertTrue("Wide panel sits on the trailing side", bounds.left > context.resources.displayMetrics.widthPixels / 4)
        }
        screenshot("boards19-reader-controls")
        click(context.localized(MR.strings.action_done))
        await { !open.value }
    }

    @Test fun homeNavigationUsesTheAvailableWidth() {
        await { has("Library") && has("More") }
        val bounds = Rect().also(target("More")::getBoundsInScreen)
        if (context.resources.configuration.smallestScreenWidthDp >= 600) {
            assertTrue("Tablet uses leading navigation rail", bounds.centerX() < context.resources.displayMetrics.widthPixels / 4)
        } else {
            assertTrue("Phone uses bottom navigation", bounds.bottom > context.resources.displayMetrics.heightPixels * 0.7)
        }
        screenshot("boards19-home-navigation")
    }

    @Test fun changesDialogLoadsRealReleaseEntriesAndCloses() {
        val open = mutableStateOf(true)
        show { if (open.value) WhatsNewDialog { open.value = false } }
        val version = context.resources.openRawResource(com.shinku.reader.R.raw.changelog_release).use { stream ->
            val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(stream, null)
            while (parser.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && parser.name != "changelogversion") parser.next()
            checkNotNull(parser.getAttributeValue(null, "versionName"))
        }
        await { nodes().any { it.text?.toString()?.contains(version) == true } }
        screenshot("boards18-changes")
        click(context.localized(MR.strings.action_close))
        await { !open.value }
    }

    @Test fun notesFormattingExposesItsSelectedStateAndLabel() {
        val selected = mutableStateOf(false)
        page {
            com.shinku.reader.presentation.manga.components.MangaNotesTextAreaButton(
                onClick = { selected.value = !selected.value },
                icon = androidx.compose.material.icons.Icons.Outlined.FormatBold,
                isSelected = selected.value, description = context.localized(MR.strings.notes_format_bold),
            )
        }
        await { has(context.localized(MR.strings.notes_format_bold)) }
        assertFalse(target(context.localized(MR.strings.notes_format_bold)).isChecked)
        click(context.localized(MR.strings.notes_format_bold))
        await { target(context.localized(MR.strings.notes_format_bold)).isChecked }
        screenshot("boards22-notes-formatting")
    }

    @Test fun unreadableBackupOffersAnotherFileWithoutStartingRestore() {
        show {
            cafe.adriel.voyager.navigator.Navigator(
                com.shinku.reader.presentation.more.settings.screen.data.RestoreBackupScreen("content://fixture.invalid/unreadable.backup"),
            )
        }
        await { has(context.localized(MR.strings.restore_choose_another_file)) }
        screenshot("boards18-unreadable-backup")
    }

    @Test fun advancedSourceToolsOpenWithoutStartingImportsOrEditingCategories() {
        show {
            cafe.adriel.voyager.navigator.Navigator(
                com.shinku.reader.presentation.more.settings.screen.ShinKuSettingsSectionScreen(
                    com.shinku.reader.presentation.more.settings.screen.ShinKuSettingsSection.ADVANCED,
                ),
            )
        }
        scrollTo(context.localized(SYMR.strings.batch_add))
        click(context.localized(SYMR.strings.batch_add))
        await { has(context.localized(SYMR.strings.eh_batch_add_title)) }
        assertFalse(target(context.localized(SYMR.strings.eh_batch_add_button)).isEnabled)
        screenshot("boards23-batch-route")
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        await { has(context.localized(SYMR.strings.batch_add)) && !has(context.localized(SYMR.strings.eh_batch_add_title)) }
        scrollTo(context.localized(MR.strings.source_categories_title))
        click(context.localized(MR.strings.source_categories_title))
        await { has(context.localized(MR.strings.action_edit_categories)) }
        screenshot("boards23-source-category-route")
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        await { has(context.localized(MR.strings.source_categories_title)) && !has(context.localized(MR.strings.action_edit_categories)) }
    }

    private fun show(content: @Composable () -> Unit) = instrumentation.runOnMainSync {
        activity.setContent { TachiyomiTheme { Surface(Modifier.fillMaxSize()) { content() } } }
    }
    private fun page(scroll: Boolean = true, content: @Composable () -> Unit) = show {
        Scaffold { padding -> Column(Modifier.fillMaxSize().padding(padding).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)) { content() } }
    }
    private fun nodes(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.packageName?.toString() == context.packageName) result.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let(::visit)
        }
        instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
        return result
    }
    private fun has(label: String) = nodes().any { it.isVisibleToUser && (it.text?.toString() == label || it.contentDescription?.toString() == label) }
    private fun target(label: String): AccessibilityNodeInfo {
        var node = nodes().first { it.isVisibleToUser && (it.text?.toString() == label || it.contentDescription?.toString() == label) }
        while (!node.isClickable && node.parent != null) node = node.parent
        return node
    }
    private fun click(label: String) {
        await { has(label) && target(label).isEnabled }
        assertTrue("Click $label", target(label).performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
    private fun scrollTo(label: String, aboveLabel: String? = null) {
        fun visible(): Boolean {
            if (!has(label)) return false
            val bounds = Rect().also(target(label)::getBoundsInScreen)
            val bottom = aboveLabel?.let { Rect().also(target(it)::getBoundsInScreen).top } ?: context.resources.displayMetrics.heightPixels
            return bounds.height() > 0 && bounds.top >= 0 && bounds.bottom <= bottom
        }
        repeat(20) {
            if (visible()) return
            val scroll = nodes().lastOrNull { it.isScrollable && it.actionList.any { action -> action.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id } } ?: nodes().lastOrNull { it.isScrollable }
            scroll?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(250)
        }
        await { visible() }
    }
    private fun await(condition: () -> Boolean) {
        val until = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < until) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        assertTrue("UI condition timed out", condition())
    }
    private fun screenshot(name: String) {
        SystemClock.sleep(500)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = checkNotNull(context.getExternalFilesDir("remaining-boards-review")).apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
