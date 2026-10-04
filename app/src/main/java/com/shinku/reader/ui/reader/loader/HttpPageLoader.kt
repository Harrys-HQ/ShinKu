package com.shinku.reader.ui.reader.loader

import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.data.cache.ChapterCache
import com.shinku.reader.data.database.models.toDomainChapter
import com.shinku.reader.domain.manga.model.toSManga
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import com.shinku.reader.ui.reader.model.ReaderChapter
import com.shinku.reader.ui.reader.model.ReaderPage
import com.shinku.reader.ui.reader.setting.ReaderPreferences
import com.shinku.reader.exh.source.isEhBasedSource
import com.shinku.reader.exh.util.DataSaver
import com.shinku.reader.exh.util.DataSaver.Companion.getImage
import eu.kanade.tachiyomi.network.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.PriorityBlockingQueue
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.math.min

/**
 * Loader used to load chapters from an online source.
 */
@OptIn(DelicateCoroutinesApi::class)
internal class HttpPageLoader(
    private val chapter: ReaderChapter,
    private val source: HttpSource,
    private val chapterCache: ChapterCache = Injekt.get(),
    // SY -->
    private val readerPreferences: ReaderPreferences = Injekt.get(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val avgPageTimeProvider: (() -> Long)? = null,
    // SY <--
) : PageLoader() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * A queue used to manage requests one by one while allowing priorities.
     */
    private val queue = PriorityBlockingQueue<PriorityPage>()

    private val preloadSize = run {
        val performanceMode = Injekt.get<com.shinku.reader.domain.base.BasePreferences>().performanceMode().get()
        if (performanceMode == 2) 2 else readerPreferences.preloadSize().get()
    }

    private fun getDynamicPreloadSize(): Int {
        if (!Injekt.get<com.shinku.reader.exh.source.ShinKuPreferences>().predictiveLoading().get()) {
            return preloadSize
        }

        val avgTime = avgPageTimeProvider?.invoke() ?: 0L
        return when {
            avgTime in 500..5_000 -> (preloadSize * 2).coerceAtMost(30)
            avgTime in 5_001..12_000 -> (preloadSize + 2).coerceAtMost(25)
            avgTime > 45_000 -> (preloadSize / 2).coerceAtLeast(2)
            else -> preloadSize
        }
    }

    // SY -->
    private val dataSaver = DataSaver(source, sourcePreferences)
    // SY <--

    init {
        // EXH -->
        repeat(readerPreferences.readerThreads().get()) {
            // EXH <--
            scope.launchIO {
                flow {
                    while (true) {
                        emit(runInterruptible { queue.take() }.page)
                    }
                }
                    .filter { it.status == Page.State.Queue }
                    .collect(::internalLoadPage)
            }
            // EXH -->
        }
        // EXH <--
    }

    override var isLocal: Boolean = false

    /**
     * Returns the page list for a chapter. It tries to return the page list from the local cache,
     * otherwise fallbacks to network.
     */
    override suspend fun getPages(): List<ReaderPage> {
        val pages = try {
            val cachedPages = chapterCache.getPageListFromCache(chapter.chapter.toDomainChapter()!!)
            if (cachedPages.isEmpty() || cachedPages.any { it.imageUrl.isNullOrBlank() && it.url.isBlank() || it.imageUrl?.contains("mangadex.org/data") == true }) {
                error("Invalid or corrupted cached page list")
            }
            cachedPages
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            var memo = chapter.chapter.memo?.takeIf { !it.isEmpty() }
                ?: eu.kanade.tachiyomi.source.online.ChapterMemoCache.get(source.id, chapter.chapter.url)

            val needsNetworkFetch = memo == null ||
                (memo["slug"] as? kotlinx.serialization.json.JsonPrimitive)?.content.isNullOrBlank() ||
                (memo["number"] as? kotlinx.serialization.json.JsonPrimitive)?.content.isNullOrBlank()

            if (needsNetworkFetch) {
                val manga = chapter.manga
                if (manga != null) {
                    try {
                        val networkChapters = source.getChapterList(manga.toSManga())
                        networkChapters.forEach { netChapter ->
                            netChapter.memo?.let { m ->
                                eu.kanade.tachiyomi.source.online.ChapterMemoCache.put(source.id, netChapter.url, m)
                            }
                        }
                    } catch (t: Throwable) {
                        // ignore
                    }
                }
            }

            val manga = chapter.manga
            chapter.chapter.memo = eu.kanade.tachiyomi.source.online.ChapterMemoCache.ensureChapterMemo(
                sourceId = source.id,
                chapter = chapter.chapter,
                mangaUrl = manga?.url,
                mangaTitle = manga?.ogTitle,
                mangaMemo = manga?.toSManga()?.memo,
            )

            source.getPageList(chapter.chapter)
        }
        // SY -->
        val rp = pages.mapIndexed { index, page ->
            // Don't trust sources and use our own indexing
            ReaderPage(index, page.url, page.imageUrl)
        }
        if (readerPreferences.aggressivePageLoading().get()) {
            rp.forEach {
                if (it.status == Page.State.Queue) {
                    queue.offer(PriorityPage(it, 0))
                }
            }
        }
        return rp
        // SY <--
    }

    /**
     * Loads a page through the queue. Handles re-enqueueing pages if they were evicted from the cache.
     */
    override suspend fun loadPage(page: ReaderPage) = withIOContext {
        val imageUrl = page.imageUrl

        // Check if the image has been deleted
        if (page.status == Page.State.Ready && imageUrl != null && !chapterCache.isImageInCache(imageUrl)) {
            page.status = Page.State.Queue
        }

        // Automatically retry failed pages when subscribed to this page
        if (page.status is Page.State.Error) {
            page.status = Page.State.Queue
        }

        val queuedPages = mutableListOf<PriorityPage>()
        if (page.status == Page.State.Queue) {
            queuedPages += PriorityPage(page, 2).also { queue.offer(it) }
        }
        queuedPages += preloadNextPages(page, getDynamicPreloadSize())

        suspendCancellableCoroutine<Nothing> { continuation ->
            continuation.invokeOnCancellation {
                queuedPages.forEach {
                    if (it.page.status == Page.State.Queue) {
                        queue.remove(it)
                    }
                }
            }
        }
    }

    /**
     * Retries a page. This method is only called from user interaction on the viewer.
     */
    override fun retryPage(page: ReaderPage) {
        if (page.status is Page.State.Error) {
            page.status = Page.State.Queue
        }
        // Force re-fetch of CDN image URL on retry only for EHentai-based sources that dynamically renew leases
        if (source.isEhBasedSource()) {
            page.imageUrl = null
        }

        if (readerPreferences.readerInstantRetry().get()) {
            boostPage(page)
        } else {
            queue.offer(PriorityPage(page, 2))
        }
    }

    override fun recycle() {
        super.recycle()
        scope.cancel()
        queue.clear()

        // Cache current page list progress for online chapters to allow a faster reopen
        chapter.pages?.let { pages ->
            if (pages.isEmpty() || pages.any { it.imageUrl.isNullOrBlank() && it.url.isBlank() }) {
                return@let
            }
            launchIO {
                try {
                    // Convert to pages without reader information
                    val pagesToSave = pages.map { Page(it.index, it.url, it.imageUrl) }
                    chapterCache.putPageListToCache(chapter.chapter.toDomainChapter()!!, pagesToSave)
                } catch (e: Throwable) {
                    if (e is CancellationException) {
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Preloads the given [amount] of pages after the [currentPage] with a lower priority.
     *
     * @return a list of [PriorityPage] that were added to the [queue]
     */
    private fun preloadNextPages(currentPage: ReaderPage, amount: Int): List<PriorityPage> {
        val pageIndex = currentPage.index
        val pages = currentPage.chapter.pages ?: return emptyList()
        if (pageIndex == pages.lastIndex) return emptyList()

        return pages
            .subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size))
            .mapNotNull {
                if (it.status == Page.State.Queue) {
                    PriorityPage(it, 0).apply { queue.offer(this) }
                } else {
                    null
                }
            }
    }

    /**
     * Loads the page, retrieving the image URL and downloading the image if necessary.
     * Downloaded images are stored in the chapter cache.
     *
     * @param page the page whose source image has to be downloaded.
     */
    private suspend fun internalLoadPage(page: ReaderPage) {
        var attempts = 0
        val maxAttempts = 3
        var lastError: Throwable? = null

        while (attempts < maxAttempts) {
            try {
                attempts++
                if (page.imageUrl.isNullOrEmpty()) {
                    page.status = Page.State.LoadPage
                    val resolvedUrl = if (page.url.isNotBlank()) {
                        try {
                            source.getImageUrl(page)
                        } catch (e: UnsupportedOperationException) {
                            null
                        }
                    } else {
                        null
                    }

                    if (!resolvedUrl.isNullOrEmpty()) {
                        page.imageUrl = resolvedUrl
                    } else {
                        // Refresh the entire page list from source to retrieve fresh URLs
                        val freshPages = source.getPageList(chapter.chapter)
                        val freshPage = freshPages.getOrNull(page.index)
                        if (freshPage != null && !freshPage.imageUrl.isNullOrEmpty()) {
                            page.imageUrl = freshPage.imageUrl
                            // Also update other pages in chapter if their imageUrl was null
                            chapter.pages?.forEachIndexed { idx, p ->
                                if (p.imageUrl.isNullOrEmpty()) {
                                    freshPages.getOrNull(idx)?.imageUrl?.let { p.imageUrl = it }
                                }
                            }
                        } else {
                            throw IllegalArgumentException("Source failed to provide a valid image URL for page ${page.index + 1}")
                        }
                    }
                }
                val imageUrl = page.imageUrl!!

                if (!chapterCache.isImageInCache(imageUrl)) {
                    page.status = Page.State.DownloadImage
                    val imageResponse = source.getImage(page, dataSaver)
                    chapterCache.putImageToCache(imageUrl, imageResponse)
                }

                page.stream = { chapterCache.getImageFile(imageUrl).inputStream() }
                page.status = Page.State.Ready
                return
            } catch (e: Throwable) {
                lastError = e
                if (e is CancellationException) {
                    throw e
                }

                // Invalidate cached CDN URL on failure so the next attempt fetches a fresh CDN node only if the source supports resolving image URLs
                if (source.isEhBasedSource()) {
                    page.imageUrl = null
                }

                // If source throws IndexOutOfBoundsException, website structure changed; don't retry endlessly
                if (e is IndexOutOfBoundsException) {
                    lastError = Exception("Source parsing failed (Website layout modified)")
                    break
                }

                if (attempts < maxAttempts) {
                    val delayMs = if (e is HttpException && (e.code == 429 || e.code == 503)) {
                        // Rate-limited or Cloudflare challenge: back off exponentially with jitter to avoid IP bans
                        2_000L * attempts + (100L..500L).random()
                    } else {
                        500L * attempts
                    }
                    kotlinx.coroutines.delay(delayMs)
                }
            }
        }

        page.status = Page.State.Error(lastError ?: Exception("CDN attempts failed"))
    }

    // EXH -->
    fun boostPage(page: ReaderPage) {
        if (page.status == Page.State.Queue) {
            queue.offer(PriorityPage(page, 2))
        }
    }
    // EXH <--
}

/**
 * Data class used to keep ordering of pages in order to maintain priority.
 */
@OptIn(ExperimentalAtomicApi::class)
private class PriorityPage(
    val page: ReaderPage,
    val priority: Int,
) : Comparable<PriorityPage> {
    companion object {
        private val idGenerator = AtomicInt(0)
    }

    private val identifier = idGenerator.incrementAndFetch()

    override fun compareTo(other: PriorityPage): Int {
        val p = other.priority.compareTo(priority)
        return if (p != 0) p else identifier.compareTo(other.identifier)
    }
}
