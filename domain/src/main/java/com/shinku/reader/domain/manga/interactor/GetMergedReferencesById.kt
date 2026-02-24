package com.shinku.reader.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.manga.model.MergedMangaReference
import com.shinku.reader.domain.manga.repository.MangaMergeRepository

class GetMergedReferencesById(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    suspend fun await(id: Long): List<MergedMangaReference> {
        return try {
            mangaMergeRepository.getReferencesById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    suspend fun subscribe(id: Long): Flow<List<MergedMangaReference>> {
        return mangaMergeRepository.subscribeReferencesById(id)
    }
}
