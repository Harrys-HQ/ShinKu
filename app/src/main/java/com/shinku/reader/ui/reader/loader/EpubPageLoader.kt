package com.shinku.reader.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import com.shinku.reader.ui.reader.model.ReaderPage
import com.shinku.reader.util.storage.EpubFile
import com.shinku.reader.core.common.archive.ArchiveReader

/**
 * Loader used to load a chapter from a .epub file.
 */
internal class EpubPageLoader(reader: ArchiveReader) : PageLoader() {

    private val epub = EpubFile(reader)

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> {
        return epub.getImagesFromPages()
            .mapIndexed { i, path ->
                val streamFn = { epub.getInputStream(path)!! }
                ReaderPage(i).apply {
                    stream = streamFn
                    status = Page.State.Ready
                }
            }
    }

    override suspend fun loadPage(page: ReaderPage) {
        check(!isRecycled)
    }

    override fun recycle() {
        super.recycle()
        epub.close()
    }
}
