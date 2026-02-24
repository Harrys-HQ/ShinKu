package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.CustomMangaRepository

class GetCustomMangaInfo(
    private val customMangaRepository: CustomMangaRepository,
) {

    fun get(mangaId: Long) = customMangaRepository.get(mangaId)
}
