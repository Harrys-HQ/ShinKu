package com.shinku.reader.ui.reader

import androidx.lifecycle.SavedStateHandle

internal class ReaderPosition(private val savedState: SavedStateHandle) {
    var chapterId = savedState.get<Long>("chapter_id")?.takeIf { it >= 0 }
        private set

    private var pageIndex = savedState.get<Int>("page_index")?.takeIf { it >= 0 }

    fun initialPage(intentChapterId: Long, intentPage: Int?): Int? {
        val savedChapterId = chapterId ?: return intentPage
        return pageIndex ?: intentPage.takeIf { savedChapterId == intentChapterId }
    }

    fun save(chapterId: Long, pageIndex: Int?) {
        this.chapterId = chapterId
        this.pageIndex = pageIndex?.takeIf { it >= 0 }
        savedState["chapter_id"] = chapterId
        savedState["page_index"] = this.pageIndex
    }
}
