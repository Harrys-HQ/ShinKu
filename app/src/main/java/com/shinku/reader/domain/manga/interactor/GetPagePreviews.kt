package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.chapter.model.toSChapter
import com.shinku.reader.domain.manga.model.PagePreview
import com.shinku.reader.domain.manga.model.toSManga
import com.shinku.reader.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.Source
import com.shinku.reader.exh.source.getMainSource
import com.shinku.reader.domain.chapter.interactor.GetChaptersByMangaId
import com.shinku.reader.domain.manga.model.Manga

class GetPagePreviews(
    private val pagePreviewCache: PagePreviewCache,
    private val getChaptersByMangaId: GetChaptersByMangaId,
) {

    suspend fun await(manga: Manga, source: Source, page: Int): Result {
        @Suppress("NAME_SHADOWING")
        val source = source.getMainSource<PagePreviewSource>() ?: return Result.Unused
        val chapters = getChaptersByMangaId.await(manga.id).sortedByDescending { it.sourceOrder }
        val chapterIds = chapters.map { it.id }
        return try {
            val pagePreviews = try {
                pagePreviewCache.getPageListFromCache(manga, chapterIds, page)
            } catch (_: Exception) {
                source.getPagePreviewList(manga.toSManga(), chapters.map { it.toSChapter() }, page).also {
                    pagePreviewCache.putPageListToCache(manga, chapterIds, it)
                }
            }
            Result.Success(
                pagePreviews.pagePreviews.map {
                    PagePreview(it.index, it.imageUrl, source.id)
                },
                pagePreviews.hasNextPage,
                pagePreviews.pagePreviewPages,
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    sealed class Result {
        data object Unused : Result()
        data class Success(
            val pagePreviews: List<PagePreview>,
            val hasNextPage: Boolean,
            val pageCount: Int?,
        ) : Result()
        data class Error(val error: Throwable) : Result()
    }
}
