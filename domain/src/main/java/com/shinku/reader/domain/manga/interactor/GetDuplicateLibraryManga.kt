package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.MangaWithChapterCount
import com.shinku.reader.domain.manga.repository.MangaRepository

class GetDuplicateLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(manga: Manga): List<MangaWithChapterCount> {
        return mangaRepository.getDuplicateLibraryManga(manga.id, manga.title.lowercase())
    }
}
