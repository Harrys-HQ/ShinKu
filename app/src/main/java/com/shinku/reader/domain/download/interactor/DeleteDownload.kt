package com.shinku.reader.domain.download.interactor

import com.shinku.reader.data.download.DownloadManager
import com.shinku.reader.core.common.util.lang.withNonCancellableContext
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.service.SourceManager

class DeleteDownload(
    private val sourceManager: SourceManager,
    private val downloadManager: DownloadManager,
) {

    suspend fun awaitAll(manga: Manga, vararg chapters: Chapter) = withNonCancellableContext {
        sourceManager.get(manga.source)?.let { source ->
            downloadManager.deleteChapters(chapters.toList(), manga, source)
        }
    }
}
