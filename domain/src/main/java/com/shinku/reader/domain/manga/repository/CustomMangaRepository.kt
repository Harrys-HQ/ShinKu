package com.shinku.reader.domain.manga.repository

import com.shinku.reader.domain.manga.model.CustomMangaInfo

interface CustomMangaRepository {

    fun get(mangaId: Long): CustomMangaInfo?

    fun set(mangaInfo: CustomMangaInfo)
}
