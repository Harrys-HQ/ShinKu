package com.shinku.reader.data.ai

import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.ai.model.MangaEmbedding
import com.shinku.reader.domain.ai.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AiRepositoryImpl(
    private val handler: DatabaseHandler,
) : AiRepository {

    override suspend fun getEmbedding(mangaId: Long): MangaEmbedding? {
        return handler.await {
            manga_embeddingsQueries.getEmbeddingByMangaId(mangaId) { id, bytes, lastUpdate ->
                MangaEmbedding(id, bytes.toFloatArray(), lastUpdate)
            }.executeAsOneOrNull()
        }
    }

    override suspend fun getAllEmbeddings(): List<MangaEmbedding> {
        return handler.awaitList {
            manga_embeddingsQueries.getAllEmbeddings { id, bytes, lastUpdate ->
                MangaEmbedding(id, bytes.toFloatArray(), lastUpdate)
            }
        }
    }

    override fun getLibraryEmbeddingsAsFlow(): Flow<List<MangaEmbedding>> {
        return handler.subscribeToList {
            manga_embeddingsQueries.getEmbeddingsForLibrary { id, bytes, lastUpdate ->
                MangaEmbedding(id, bytes.toFloatArray(), lastUpdate)
            }
        }
    }

    override suspend fun updateEmbedding(embedding: MangaEmbedding) {
        handler.await {
            manga_embeddingsQueries.insert(
                mangaId = embedding.mangaId,
                embedding = embedding.embedding.toByteArray(),
                lastMetadataUpdate = embedding.lastMetadataUpdate,
            )
        }
    }

    override suspend fun deleteEmbedding(mangaId: Long) {
        handler.await {
            manga_embeddingsQueries.deleteByMangaId(mangaId)
        }
    }

    private fun FloatArray.toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
        forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun ByteArray.toFloatArray(): FloatArray {
        val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
        val result = FloatArray(size / 4)
        for (i in result.indices) {
            result[i] = buffer.float
        }
        return result
    }
}
