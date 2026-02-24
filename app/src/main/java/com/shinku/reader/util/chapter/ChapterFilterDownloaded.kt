package com.shinku.reader.util.chapter

import com.shinku.reader.data.download.DownloadCache
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.manga.model.Manga
import eu.kanade.tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Returns a copy of the list with not downloaded chapters removed.
 */
fun List<Chapter>.filterDownloaded(manga: Manga/* SY --> */, mangaMap: Map<Long, Manga>?): List<Chapter> {
    if (manga.isLocal()) return this

    val downloadCache: DownloadCache = Injekt.get()

    // SY -->
    return filter {
        val chapterManga = mangaMap?.get(it.mangaId) ?: manga
        downloadCache.isChapterDownloaded(it.name, it.scanlator, it.url, chapterManga.ogTitle, chapterManga.source, false)
    }
    // SY <--
}
