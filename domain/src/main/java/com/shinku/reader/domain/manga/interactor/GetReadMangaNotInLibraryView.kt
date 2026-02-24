package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.library.model.LibraryManga
import com.shinku.reader.domain.manga.repository.MangaRepository

class GetReadMangaNotInLibraryView(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): List<LibraryManga> {
        return mangaRepository.getReadMangaNotInLibraryView()
    }
}
