package com.shinku.reader.ui.browse.feed

/** A failed request never advances this cursor. Seed titles are deduplicated against page one. */
internal class SourcePageCursor {
    var nextPage = 1
        private set
    var hasMore = true
        private set

    fun accept(hasNextPage: Boolean) {
        check(hasMore)
        nextPage++
        hasMore = hasNextPage
    }
}
