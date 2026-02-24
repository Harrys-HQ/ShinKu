package com.shinku.reader.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.repository.MangaMergeRepository

class GetMergedManga(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    suspend fun await(): List<Manga> {
        return try {
            mangaMergeRepository.getMergedManga()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    suspend fun subscribe(): Flow<List<Manga>> {
        return mangaMergeRepository.subscribeMergedManga()
    }
}
