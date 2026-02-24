package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.MangaUpdate
import com.shinku.reader.domain.manga.repository.MangaRepository

class UpdateMangaNotes(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(mangaId: Long, notes: String): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = mangaId,
                notes = notes,
            ),
        )
    }
}
