package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.MangaRepository

class DeleteMangaById(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(id: Long) {
        return mangaRepository.deleteManga(id)
    }
}
