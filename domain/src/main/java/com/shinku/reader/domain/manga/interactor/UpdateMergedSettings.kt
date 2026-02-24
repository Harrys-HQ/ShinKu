package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.MergeMangaSettingsUpdate
import com.shinku.reader.domain.manga.repository.MangaMergeRepository

class UpdateMergedSettings(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    suspend fun await(mergeUpdate: MergeMangaSettingsUpdate): Boolean {
        return mangaMergeRepository.updateSettings(mergeUpdate)
    }

    suspend fun awaitAll(values: List<MergeMangaSettingsUpdate>): Boolean {
        return mangaMergeRepository.updateAllSettings(values)
    }
}
