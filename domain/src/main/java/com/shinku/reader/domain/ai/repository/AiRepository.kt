package com.shinku.reader.domain.ai.repository

import com.shinku.reader.domain.ai.model.MangaEmbedding
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    suspend fun getEmbedding(mangaId: Long): MangaEmbedding?
    suspend fun getAllEmbeddings(): List<MangaEmbedding>
    fun getLibraryEmbeddingsAsFlow(): Flow<List<MangaEmbedding>>
    suspend fun updateEmbedding(embedding: MangaEmbedding)
    suspend fun deleteEmbedding(mangaId: Long)
}
