package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.ui.reader.setting.ReaderOrientation
import com.shinku.reader.ui.reader.setting.ReadingMode
import com.shinku.reader.domain.manga.model.MangaUpdate
import com.shinku.reader.domain.manga.repository.MangaRepository

class SetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetReadingMode(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, ReadingMode.MASK.toLong()),
            ),
        )
    }

    suspend fun awaitSetOrientation(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, ReaderOrientation.MASK.toLong()),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
