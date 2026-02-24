package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.CustomMangaInfo
import com.shinku.reader.domain.manga.repository.CustomMangaRepository

class SetCustomMangaInfo(
    private val customMangaRepository: CustomMangaRepository,
) {

    fun set(mangaInfo: CustomMangaInfo) = customMangaRepository.set(mangaInfo)
}
