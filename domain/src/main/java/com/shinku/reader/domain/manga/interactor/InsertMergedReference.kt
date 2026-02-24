package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.MergedMangaReference
import com.shinku.reader.domain.manga.repository.MangaMergeRepository

class InsertMergedReference(
    private val mangaMergedRepository: MangaMergeRepository,
) {

    suspend fun await(reference: MergedMangaReference): Long? {
        return mangaMergedRepository.insert(reference)
    }

    suspend fun awaitAll(references: List<MergedMangaReference>) {
        mangaMergedRepository.insertAll(references)
    }
}
