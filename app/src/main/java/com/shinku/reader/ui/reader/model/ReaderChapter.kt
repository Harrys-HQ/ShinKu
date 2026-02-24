package com.shinku.reader.ui.reader.model

import com.shinku.reader.domain.chapter.model.toDbChapter
import com.shinku.reader.data.database.models.Chapter
import com.shinku.reader.ui.reader.loader.PageLoader
import kotlinx.coroutines.flow.MutableStateFlow
import com.shinku.reader.core.common.util.system.logcat

data class ReaderChapter(val chapter: Chapter) {

    val stateFlow = MutableStateFlow<State>(State.Wait)
    var state: State
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }

    val pages: List<ReaderPage>?
        get() = (state as? State.Loaded)?.pages

    var pageLoader: PageLoader? = null

    var requestedPage: Int = 0

    private var references = 0

    constructor(chapter: com.shinku.reader.domain.chapter.model.Chapter) : this(chapter.toDbChapter())

    fun ref() {
        references++
    }

    fun unref() {
        references--
        if (references == 0) {
            if (pageLoader != null) {
                logcat { "Recycling chapter ${chapter.name}" }
            }
            pageLoader?.recycle()
            pageLoader = null
            state = State.Wait
        }
    }

    sealed interface State {
        data object Wait : State
        data object Loading : State
        data class Error(val error: Throwable) : State
        data class Loaded(val pages: List<ReaderPage>) : State
    }
}
