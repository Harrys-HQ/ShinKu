package com.shinku.reader.domain.ai.interactor

import com.shinku.reader.domain.ai.model.MangaEmbedding
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.repository.MangaRepository
import kotlin.math.sqrt

class GetSimilarManga(
    private val mangaRepository: MangaRepository,
    private val getMangaEmbeddings: GetMangaEmbeddings,
) {
    suspend fun await(mangaId: Long, limit: Int = 10): List<Manga> {
        val targetEmbedding = getMangaEmbeddings.await(mangaId) ?: return emptyList()
        val allEmbeddings = getMangaEmbeddings.all().filter { it.mangaId != mangaId }

        if (allEmbeddings.isEmpty()) return emptyList()

        val topIds = allEmbeddings
            .asSequence()
            .map { it to cosineSimilarity(targetEmbedding.embedding, it.embedding) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first.mangaId }
            .toList()

        return topIds.mapNotNull { mangaRepository.getMangaById(it) }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0f) 0f else dot / denominator
    }
}
