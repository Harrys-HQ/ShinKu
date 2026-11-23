package com.shinku.reader.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.source.interactor.TranslationGlossary
import com.shinku.reader.presentation.reader.dialog.AreaOcrCropOverlay
import com.shinku.reader.presentation.reader.dialog.AreaTranslationResultDialog
import com.shinku.reader.presentation.reader.dialog.BatchTranslationProgressDialog
import com.shinku.reader.presentation.reader.dialog.ReaderRemoteTranslationDialog
import com.shinku.reader.presentation.reader.dialog.TranslationOptionsDialog
import com.shinku.reader.presentation.reader.dialog.TranslationGlossaryDialog
import com.shinku.reader.presentation.theme.TachiyomiTheme
import com.shinku.reader.ui.main.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ReaderTranslationAuditTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var activity: MainActivity
    private var previousNightMode: Int? = null

    @Before fun setup() {
        check(context.packageName.endsWith(".debug"))
        if (InstrumentationRegistry.getArguments().getString("reviewLight") == "true") {
            previousNightMode = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode()
            instrumentation.runOnMainSync { androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) }
        }
        Injekt.get<BasePreferences>().shownOnboardingFlow().set(true)
        activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        // Let startup side effects finish before replacing the activity's composition.
        await {
            var startupComplete = false
            instrumentation.runOnMainSync { startupComplete = activity.ready && activity.migrationDone }
            startupComplete
        }
        if (previousNightMode != null) assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_NO, activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
    }

    @After fun cleanup() {
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finish() }
        previousNightMode?.let { mode -> instrumentation.runOnMainSync { androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode) } }
    }
    private fun show(content: @Composable () -> Unit) = instrumentation.runOnMainSync {
        activity.setContent { TachiyomiTheme { Surface(Modifier.fillMaxSize()) { content() } } }
    }

    @Test fun seriesGlossaryRemovesAnEntryAndSavesHonorificSetting() {
        val initial = TranslationGlossary(listOf(TranslationGlossary.Entry("Hana", "Hanaa")), preserveHonorifics = true)
        val saved = java.util.concurrent.atomic.AtomicReference<TranslationGlossary>()
        show {
            TranslationGlossaryDialog(
                glossary = initial,
                onSave = saved::set,
                onDismissRequest = {},
            )
        }
        await { has("Series glossary") && has("Remove") }
        click("Remove")
        click("Save")
        assertEquals(TranslationGlossary(preserveHonorifics = true), saved.get())
    }

    @Test fun remoteTranslationPreviewShowsDestinationAndKeepsSendAndCancelDistinct() {
        val sent = AtomicInteger()
        val cancelled = AtomicInteger()
        val prompt = "Translate this OCR fixture. Authorization: [redacted]"
        show {
            ReaderRemoteTranslationDialog(
                destination = "fixture.example",
                prompt = prompt,
                onConfirm = { sent.incrementAndGet() },
                onDismissRequest = { cancelled.incrementAndGet() },
            )
        }
        await { has("This request sends the text below to fixture.example.") && has(prompt) }
        click("Cancel")
        assertEquals(1, cancelled.get())
        assertEquals(0, sent.get())
        click("Send text")
        assertEquals(1, sent.get())
    }

    @Test fun cloudChapterProgressKeepsCancelAvailableWithoutBackgroundAction() {
        val cancelled = AtomicInteger()
        show {
            BatchTranslationProgressDialog(
                currentPage = 1,
                totalPages = 4,
                translatedCount = 0,
                targetLanguage = "Spanish",
                onRunInBackground = null,
                onCancel = { cancelled.incrementAndGet() },
            )
        }
        await { has("Cancel") }
        assertTrue(!has("Run in background"))
        click("Cancel")
        assertEquals(1, cancelled.get())
    }

    @Test fun areaResultRetainsBothTextsAndExplicitActions() {
        val applied = AtomicInteger()
        val reselected = AtomicInteger()
        val closed = AtomicInteger()
        show { AreaTranslationResultDialog("Original fixture dialogue. ".repeat(50), "Translated fixture dialogue. ".repeat(50), "A deliberately long language name for wrapping", { closed.incrementAndGet() }, { reselected.incrementAndGet() }, { applied.incrementAndGet() }) }
        await { has("Overlay") && has("Reselect") }
        await { has("Copy translation") }
        screenshot("reader-result-long")
        scrollTo("Copy original text", "Overlay")
        screenshot("reader-result-original")
        click("Reselect")
        click("Overlay")
        click("Close")
        assertEquals(1, reselected.get())
        assertEquals(1, applied.get())
        assertEquals(1, closed.get())
    }

    @Test fun chapterProgressKeepsBackgroundAndCancellationDistinct() {
        val cancelled = AtomicInteger()
        val background = AtomicInteger()
        val overlay = mutableStateOf(true)
        show { BatchTranslationProgressDialog(123456, 999999, 123450, "A deliberately long language name for wrapping", overlay.value, { overlay.value = !overlay.value }, { background.incrementAndGet() }, { cancelled.incrementAndGet() }) }
        await { has("Run in background") }
        screenshot("reader-batch-progress")
        scrollTo("Show translated text")
        click("Show translated text")
        await { !overlay.value }
        screenshot("reader-batch-overlay")
        click("Run in background")
        click("Cancel")
        assertEquals(1, background.get())
        assertEquals(1, cancelled.get())
    }

    @Test fun cropToolsKeepDetailsTranslateResetAndCancelReachable() {
        val snapshot = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.LTGRAY) }
        var translated: Rect? = null
        var inspected: Rect? = null
        val closed = AtomicInteger()
        show { AreaOcrCropOverlay(snapshot, 7, { closed.incrementAndGet() }, { translated = it }, { inspected = it }) }
        await { has("Translate") && has("Details") }
        screenshot("reader-crop-tools")
        click("Reset Selection")
        click("Details")
        click("Translate")
        click("Cancel")
        await { translated != null && inspected != null }
        assertEquals(inspected, translated)
        assertTrue(checkNotNull(translated).let { it.left >= 0 && it.top >= 0 && it.right <= 800 && it.bottom <= 1200 && it.width() > 0 && it.height() > 0 })
        assertEquals(1, closed.get())
        instrumentation.runOnMainSync { activity.setContent {} }
        snapshot.recycle()
    }

    @Test fun translationOptionsRetainCurrentPageAndChapterCallbacks() {
        val page = AtomicInteger()
        val chapter = AtomicInteger()
        val closed = AtomicInteger()
        show { TranslationOptionsDialog(999999, "A deliberately long language name for wrapping", false, onDismissRequest = { closed.incrementAndGet() }, onTranslateCurrentPage = { page.incrementAndGet() }, onTranslateEntireChapter = { chapter.incrementAndGet() }, onChangeLanguages = {}, onClearTranslations = {}) }
        await { has("Translate Current Page") }
        screenshot("reader-translation-options")
        click("Translate Current Page")
        scrollTo("Pre-Translate Entire Chapter")
        click("Pre-Translate Entire Chapter")
        assertEquals(1, page.get())
        assertEquals(1, chapter.get())
        assertEquals(2, closed.get())
    }

    @Test fun languageChangesRemainDraftUntilConfirmed() {
        val open = mutableStateOf(true)
        var confirmed: Pair<String, String>? = null
        show {
            if (open.value) {
                com.shinku.reader.presentation.reader.dialog.ReaderTranslationLanguageDialog(
                    "Japanese", "Chinese (Traditional)", { source, target -> confirmed = source to target }, { open.value = false },
                )
            } else {
                androidx.compose.material3.Text("Closed fixture")
            }
        }
        await { has("Swap Languages") }
        screenshot("reader-language-pair")
        val targetName = "Chinese Traditional (\u7e41\u9ad4\u4e2d\u6587)"
        scrollTo(targetName)
        screenshot("reader-language-target")
        click(targetName)
        await { has("English") }
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        assertTrue(open.value)
        scrollTo("Swap Languages")
        click("Swap Languages")
        click(context.getString(com.shinku.reader.R.string.action_cancel))
        await { has("Closed fixture") }
        assertEquals(null, confirmed)
        instrumentation.runOnMainSync { open.value = true }
        scrollTo("Swap Languages")
        click("Swap Languages")
        click(context.getString(com.shinku.reader.R.string.action_ok))
        await { has("Closed fixture") }
        assertEquals("Chinese" to "Japanese", confirmed)
    }

    @Test fun processingCanBeDismissedAndLongErrorsRemainReadable() {
        val open = mutableStateOf(true)
        show {
            if (open.value) {
                com.shinku.reader.presentation.reader.dialog.ReaderAiProcessingDialog { open.value = false }
            } else {
                androidx.compose.material3.Text("Closed fixture")
            }
        }
        await { has("Analyzing Page Content") }
        screenshot("reader-ai-processing")
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        await { has("Closed fixture") }
        show { com.shinku.reader.presentation.reader.dialog.ReaderAiInsightDialog("Translation Error", "Fixture provider error details. ".repeat(100), { open.value = true }) }
        await { has("Translation Error") }
        screenshot("reader-ai-error")
        click(context.getString(com.shinku.reader.R.string.action_ok))
        await { open.value }
    }
    private fun nodes(): List<AccessibilityNodeInfo> {
        // Android 14+ can retain stale virtual descendants after a Compose scroll.
        if (android.os.Build.VERSION.SDK_INT >= 34) instrumentation.uiAutomation.clearCache()
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            result.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let(::visit)
        }
        instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
        // Optional physical-device review only: acknowledge the known debug warning without
        // selecting its persistent "Don't Show Again" option or treating compatibility as passed.
        if (InstrumentationRegistry.getArguments().getString("reviewCompatibilityWarning") == "true" &&
            result.any { it.text?.toString() == "Android App Compatibility" } &&
            result.any { it.text?.toString()?.contains("debuggable app") == true }
        ) {
            val confirm = result.firstOrNull { it.text?.toString() == "OK" && it.isClickable }
            if (confirm?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                val directory = checkNotNull(context.getExternalFilesDir("reader-translation-review")).apply { mkdirs() }
                File(directory, "debug-compatibility-warning.txt").appendText("Acknowledged debug compatibility warning with OK; native compatibility remains unverified.\n")
            }
            return emptyList()
        }
        return result.filter { it.packageName?.toString() == context.packageName }
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
            val item = target(label)
            val bounds = Rect().also(item::getBoundsInScreen)
            var top = 0
            var bottom = aboveLabel?.let { Rect().also(target(it)::getBoundsInScreen).top } ?: context.resources.displayMetrics.heightPixels
            var ancestor = item.parent
            while (ancestor != null) {
                if (ancestor.isScrollable) {
                    val viewport = Rect().also(ancestor::getBoundsInScreen)
                    top = maxOf(top, viewport.top)
                    bottom = minOf(bottom, viewport.bottom)
                }
                ancestor = ancestor.parent
            }
            if (InstrumentationRegistry.getArguments().getString("debugScroll") == "true") File(checkNotNull(context.getExternalFilesDir("reader-translation-review")), "reader-visible.txt").appendText("$label bounds=$bounds footer=$bottom\n")
            return bounds.height() > 0 && bounds.top >= top && bounds.bottom <= bottom
        }
        for (direction in listOf(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
            repeat(20) {
                if (InstrumentationRegistry.getArguments().getString("debugScroll") == "true") {
                    screenshot("reader-step-$direction-$it")
                    val info = nodes().filter { node -> node.text != null || node.contentDescription != null }.joinToString(" | ") { node -> "${node.text?.take(35) ?: node.contentDescription}:${Rect().also(node::getBoundsInScreen)}" }
                    File(checkNotNull(context.getExternalFilesDir("reader-translation-review")), "reader-steps.txt").appendText("$direction-$it has=${has(label)} $info\n")
                }
                if (visible()) return
                val scroll = nodes().lastOrNull { it.isScrollable && it.actionList.any { action -> action.id == direction } }
                if (scroll != null) {
                    val bounds = Rect().also(scroll::getBoundsInScreen)
                    val low = bounds.top + bounds.height() / 4
                    val high = bounds.top + bounds.height() * 3 / 4
                    val start = if (direction == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) high else low
                    val end = if (direction == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) low else high
                    val command = "input swipe ${bounds.centerX()} $start ${bounds.centerX()} $end 800"
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
                    // Let the swipe/fling and accessibility-tree updates settle before checking a narrow header.
                    instrumentation.waitForIdleSync()
                    SystemClock.sleep(1200)
                }
            }
        }
        if (!visible()) {
            screenshot("reader-scroll-failed")
            File(checkNotNull(context.getExternalFilesDir("reader-translation-review")), "reader-scroll-failed.txt").writeText(
                nodes().joinToString("\n") { node ->
                    val bounds = Rect().also(node::getBoundsInScreen)
                    "${node.text ?: node.contentDescription} bounds=$bounds visible=${node.isVisibleToUser} scroll=${node.isScrollable} clickable=${node.isClickable} actions=${node.actionList}"
                },
            )
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
        val directory = checkNotNull(context.getExternalFilesDir("reader-translation-review")).apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
