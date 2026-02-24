package com.shinku.reader.ui.reader.chapter

import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.manga.model.Manga
import java.time.format.DateTimeFormatter

data class ReaderChapterItem(
    val chapter: Chapter,
    val manga: Manga,
    val isCurrent: Boolean,
    val dateFormat: DateTimeFormatter,
)
