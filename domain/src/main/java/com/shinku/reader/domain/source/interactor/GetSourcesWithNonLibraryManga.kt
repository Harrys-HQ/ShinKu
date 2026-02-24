package com.shinku.reader.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.source.model.SourceWithCount
import com.shinku.reader.domain.source.repository.SourceRepository

class GetSourcesWithNonLibraryManga(
    private val repository: SourceRepository,
) {

    fun subscribe(): Flow<List<SourceWithCount>> {
        return repository.getSourcesWithNonLibraryManga()
    }
}
