package com.shinku.reader.ui.reader.viewer

import com.shinku.reader.data.database.models.toDomainChapter
import com.shinku.reader.ui.reader.model.ReaderChapter
import com.shinku.reader.domain.chapter.service.calculateChapterGap as domainCalculateChapterGap

fun calculateChapterGap(higherReaderChapter: ReaderChapter?, lowerReaderChapter: ReaderChapter?): Int {
    return domainCalculateChapterGap(
        higherReaderChapter?.chapter?.toDomainChapter(),
        lowerReaderChapter?.chapter?.toDomainChapter(),
    )
}
