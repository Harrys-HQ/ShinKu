package com.shinku.reader.domain.ai.interactor

import com.shinku.reader.domain.ai.model.MangaEmbedding
import com.shinku.reader.domain.ai.repository.AiRepository
import kotlinx.coroutines.flow.Flow

class GetMangaEmbeddings(
    private val repository: AiRepository,
) {
    suspend fun await(mangaId: Long): MangaEmbedding? {
        return repository.getEmbedding(mangaId)
    }

    suspend fun all(): List<MangaEmbedding> {
        return repository.getAllEmbeddings()
    }

    fun subscribeLibrary(): Flow<List<MangaEmbedding>> {
        return repository.getLibraryEmbeddingsAsFlow()
    }
}
