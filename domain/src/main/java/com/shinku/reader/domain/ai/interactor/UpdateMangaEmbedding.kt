package com.shinku.reader.domain.ai.interactor

import com.shinku.reader.domain.ai.model.MangaEmbedding
import com.shinku.reader.domain.ai.repository.AiRepository

class UpdateMangaEmbedding(
    private val repository: AiRepository,
) {
    suspend fun await(embedding: MangaEmbedding) {
        repository.updateEmbedding(embedding)
    }

    suspend fun delete(mangaId: Long) {
        repository.deleteEmbedding(mangaId)
    }
}
