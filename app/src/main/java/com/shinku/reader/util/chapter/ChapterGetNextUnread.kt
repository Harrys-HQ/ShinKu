package com.shinku.reader.util.chapter

import com.shinku.reader.domain.chapter.model.applyFilters
import com.shinku.reader.data.download.DownloadManager
import com.shinku.reader.ui.manga.ChapterList
import com.shinku.reader.exh.source.isEhBasedManga
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.manga.model.Manga

/**
 * Gets next unread chapter with filters and sorting applied
 */
fun List<Chapter>.getNextUnread(
    manga: Manga,
    downloadManager: DownloadManager /* SY --> */,
    mergedManga: Map<Long, Manga>, /* SY <-- */
): Chapter? {
    return applyFilters(manga, downloadManager/* SY --> */, mergedManga/* SY <-- */).let { chapters ->
        // SY -->
        if (manga.isEhBasedManga()) {
            return@let if (manga.sortDescending()) {
                chapters.firstOrNull()?.takeUnless { it.read }
            } else {
                chapters.lastOrNull()?.takeUnless { it.read }
            }
        }
        // SY <--
        if (manga.sortDescending()) {
            chapters.findLast { !it.read }
        } else {
            chapters.find { !it.read }
        }
    }
}

/**
 * Gets next unread chapter with filters and sorting applied
 */
fun List<ChapterList.Item>.getNextUnread(manga: Manga): Chapter? {
    return applyFilters(manga).let { chapters ->
        // SY -->
        if (manga.isEhBasedManga()) {
            return@let if (manga.sortDescending()) {
                chapters.firstOrNull()?.takeUnless { it.chapter.read }
            } else {
                chapters.lastOrNull()?.takeUnless { it.chapter.read }
            }
        }
        // SY <--
        if (manga.sortDescending()) {
            chapters.findLast { !it.chapter.read }
        } else {
            chapters.find { !it.chapter.read }
        }
    }?.chapter
}
