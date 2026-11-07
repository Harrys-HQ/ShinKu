package com.shinku.reader.ui.download

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shinku.reader.R
import com.shinku.reader.core.common.Constants
import com.shinku.reader.data.download.DownloadJob
import com.shinku.reader.data.download.DownloadManager
import com.shinku.reader.data.download.model.Download
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.download.service.DownloadPreferences
import com.shinku.reader.domain.history.repository.HistoryRepository
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.storage.service.StorageManager
import com.shinku.reader.domain.storage.service.StoragePreferences
import com.shinku.reader.ui.main.MainActivity
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
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

/** Exercises real queue controls with local generated images in the separate devDebug app. */
@RunWith(AndroidJUnit4::class)
class DownloadQueueTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var manager: DownloadManager
    private lateinit var source: HeldImageSource
    private lateinit var downloads: List<Download>

    @Before
    fun setup() {
        check(context.packageName.endsWith(".debug")) { "Queue fixtures must never run against personal app data" }
        instrumentation.uiAutomation.serviceInfo = instrumentation.uiAutomation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        Injekt.get<BasePreferences>().shownOnboardingFlow().set(true)
        val root = File(context.cacheDir, "queue-test-${System.nanoTime()}").apply { mkdirs() }
        Injekt.get<StoragePreferences>().baseStorageDirectory().set(Uri.fromFile(root).toString())
        val storage = Injekt.get<StorageManager>()
        eventually("Private fixture download directory") {
            storage.getDownloadsDirectory()?.uri?.path?.startsWith(context.cacheDir.path) == true
        }
        Injekt.get<DownloadPreferences>().apply {
            parallelPageLimit().set(1)
            downloadThreadsPerPage().set(1)
            saveChaptersAsCBZ().set(false)
            downloadOnlyOverWifi().set(false)
        }
        manager = Injekt.get()
        manager.clearQueue()
        source = HeldImageSource()
        val manga = Manga.create().copy(id = 90200, source = source.id, ogTitle = "Queue verification fixture")
        downloads = listOf(3, 1, 2).map { number ->
            Download(
                source,
                manga,
                Chapter.create().copy(
                    id = 90200L + number,
                    mangaId = manga.id,
                    url = "/chapter-$number",
                    name = "Fixture chapter $number",
                    chapterNumber = number.toDouble(),
                    dateUpload = number * 1000L,
                ),
            ).apply {
                pages = List(3) { Page(it, imageUrl = "${source.baseUrl}/$number/$it.png") }
            }
        }
        manager.reorderQueue(downloads)
        instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java)
                .setAction(Constants.SHORTCUT_DOWNLOADS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        eventually("Queue screen") { hasText(context.getString(R.string.action_resume_all_downloads)) }
    }

    @After
    fun cleanup() {
        if (::manager.isInitialized) manager.clearQueue()
        if (::source.isInitialized) source.release.complete(Unit)
    }

    @Test
    fun activeEngineShowsPauseEvenWhenWorkerIsNotRunning() = runBlocking {
        eventually("No scheduled worker remains") { !DownloadJob.isRunning(context) }
        assertTrue(manager.downloaderStart())
        eventually("Actual downloader is active") { manager.isRunning }
        eventually("Image is held for a deterministic state check") { source.entered.isCompleted }
        eventually("Active engine must expose Pause all") {
            hasText(context.getString(R.string.action_pause_all_downloads))
        }
        click(context.getString(R.string.action_pause_all_downloads))
        eventually("Paused engine") { !manager.isRunning }
        eventually("Resume all after pause") { hasText(context.getString(R.string.action_resume_all_downloads)) }
        assertEquals(3, manager.queueState.value.size)
    }

    @Test
    fun pauseResumeSortDragCancelAndComplete() = runBlocking {
        val originalHistoryCount = Injekt.get<HistoryRepository>().getAllHistory().size
        click(context.getString(R.string.action_resume_all_downloads))
        eventually("Resume starts the downloader") { manager.isRunning && source.entered.isCompleted }
        click(context.getString(R.string.action_pause_all_downloads))
        eventually("Pause retains queue") { !manager.isRunning && manager.queueState.value.size == 3 }
        click(context.getString(R.string.action_sort))
        click(context.getString(R.string.action_order_by_chapter_number))
        click(context.getString(R.string.action_asc))
        eventually("Ascending chapter sort") { order() == listOf(1, 2, 3) }
        click(context.getString(R.string.action_sort))
        click(context.getString(R.string.action_order_by_upload_date))
        click(context.getString(R.string.action_newest))
        eventually("Newest upload sort") { order() == listOf(3, 2, 1) }
        SystemClock.sleep(500)
        android.util.Log.i(
            "QueueVerification",
            "Rendered rows: " + nodes().filter { it.text?.toString()?.startsWith("Fixture chapter ") == true }
                .map { "${it.text} @ ${bounds(it)} id=${it.viewIdResourceName}" },
        )
        eventually("Sorted rows rendered") {
            nodes().filter { it.text?.toString()?.startsWith("Fixture chapter ") == true }
                .sortedBy { bounds(it).top }.map { it.text?.toString() }.distinct() ==
                listOf("Fixture chapter 3", "Fixture chapter 2", "Fixture chapter 1")
        }
        // RecyclerView animates the sorted rows after the engine queue has already changed.
        SystemClock.sleep(500)

        eventually("Three row drag handles") { nodesWithId("reorder").size == 4 }
        val handles = nodesWithId("reorder").map(::bounds).sortedBy { it.top }.drop(1)
        android.util.Log.i("QueueVerification", "Drag handle bounds: $handles")
        shell("input swipe ${handles.first().centerX()} ${handles.first().centerY()} ${handles.last().centerX()} ${handles.last().bottom + handles.last().height()} 1500")
        android.util.Log.i("QueueVerification", "Queue order after drag: ${order()}")
        eventually("Drag moves first chapter to the end") { order() == listOf(2, 1, 3) }
        eventually("Dragged rows rendered") {
            nodes().filter { it.text?.toString()?.startsWith("Fixture chapter ") == true }
                .sortedBy { bounds(it).top }.map { it.text?.toString() }.distinct() ==
                listOf("Fixture chapter 2", "Fixture chapter 1", "Fixture chapter 3")
        }
        SystemClock.sleep(500)

        eventually("Row menu") { nodesWithId("menu").size == 3 }
        tap(bounds(nodesWithId("menu").minBy { bounds(it).top }))
        click(context.getString(R.string.action_cancel))
        eventually("Cancel removes only that chapter") { order() == listOf(1, 3) }
        click(context.getString(R.string.action_resume_all_downloads))
        eventually("Resume after cancellation") { manager.isRunning }
        source.release.complete(Unit)
        eventually("Remaining fixture downloads finish", 30000) { manager.queueState.value.isEmpty() }
        eventually("Empty state replaces queue controls") { hasText(context.getString(R.string.information_no_downloads)) }
        assertFalse(manager.isRunning)
        assertEquals(originalHistoryCount, Injekt.get<HistoryRepository>().getAllHistory().size)
    }

    private fun order() = manager.queueState.value.map { it.chapter.chapterNumber.toInt() }

    private fun eventually(message: String, timeout: Long = 15000, check: () -> Boolean) {
        val until = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < until) {
            if (check()) {
                android.util.Log.i("QueueVerification", message)
                return
            }
            SystemClock.sleep(100)
        }
        val visible = nodes().mapNotNull { it.text?.toString() ?: it.contentDescription?.toString() }.distinct().take(20)
        val rows = nodes().filter { it.text?.toString()?.startsWith("Fixture chapter ") == true }
            .map { "${it.text} @ ${bounds(it)}" }
        assertTrue("$message; engine order: ${if (::manager.isInitialized) order() else emptyList<Int>()}; rows: $rows; visible controls: $visible", check())
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.packageName?.toString() == context.packageName) result.add(node)
            repeat(node.childCount) { node.getChild(it)?.let(::visit) }
        }
        instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
        return result
    }

    private fun nodesWithId(id: String) = nodes().filter { it.viewIdResourceName?.endsWith(":id/$id") == true }
    private fun hasText(text: String) = nodes().any { it.text?.toString() == text || it.contentDescription?.toString() == text }
    private fun bounds(node: AccessibilityNodeInfo) = Rect().also(node::getBoundsInScreen)

    private fun click(text: String) {
        eventually("Visible control: $text") { hasText(text) }
        tap(bounds(nodes().first { it.text?.toString() == text || it.contentDescription?.toString() == text }))
    }

    private fun tap(rect: Rect) = shell("input tap ${rect.centerX()} ${rect.centerY()}")
    private fun shell(command: String) {
        check(instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == context.packageName) {
            "Refusing input outside the isolated debug app"
        }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .use { it.readBytes() }
        SystemClock.sleep(250)
    }

    private class HeldImageSource : HttpSource() {
        override val name = "Queue verification"
        override val lang = "en"
        override val supportsLatest = false
        override val baseUrl = "https://queue-fixture.invalid"
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        private val png = java.io.ByteArrayOutputStream().use { output ->
            val bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(android.graphics.Color.WHITE)
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            } finally {
                bitmap.recycle()
            }
        }

        override suspend fun getImage(page: Page, existingSize: Long): Response {
            entered.complete(Unit)
            release.await()
            delay(150)
            return Response.Builder().request(Request.Builder().url(page.imageUrl!!).build())
                .protocol(Protocol.HTTP_1_1).code(200).message("Fixture image")
                .body(png.toResponseBody("image/png".toMediaType())).build()
        }

        override fun popularMangaRequest(page: Int): Request = error("No external fixture requests")
        override fun popularMangaParse(response: Response): MangasPage = error("Unused")
        override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = error("Unused")
        override fun searchMangaParse(response: Response): MangasPage = error("Unused")
        override fun latestUpdatesRequest(page: Int): Request = error("Unused")
        override fun latestUpdatesParse(response: Response): MangasPage = error("Unused")
        override fun mangaDetailsParse(response: Response): SManga = error("Unused")
        override fun chapterListParse(response: Response): List<SChapter> = error("Unused")
        override fun chapterPageParse(response: Response): SChapter = error("Unused")
        override fun pageListParse(response: Response): List<Page> = error("Unused")
        override fun imageUrlParse(response: Response): String = error("Unused")
    }
}
