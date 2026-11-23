package com.shinku.reader.domain.ai.interactor

import com.shinku.reader.domain.ai.model.MangaEmbedding
import com.shinku.reader.domain.category.interactor.CreateCategoryWithName
import com.shinku.reader.domain.category.repository.CategoryRepository
import com.shinku.reader.domain.manga.repository.MangaRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.sqrt
import kotlin.random.Random

class AiClustering(
    private val mangaRepository: MangaRepository,
    private val categoryRepository: CategoryRepository,
    private val createCategoryWithName: CreateCategoryWithName,
    private val getMangaEmbeddings: GetMangaEmbeddings,
) {
    suspend fun await() {
        val embeddings = getMangaEmbeddings.all()
            .filter { it.embedding.isNotEmpty() }
            .sortedBy { it.mangaId }
        if (embeddings.size < 10) return // Not enough data to cluster

        val k = (sqrt(embeddings.size.toDouble()) / 2).toInt().coerceIn(3, 8)
        val clusters = kMeans(embeddings, k)
        val mangaById = mangaRepository.getMangaByIds(embeddings.map { it.mangaId }).associateBy { it.id }
        val categoriesByName = categoryRepository.getAll().associateBy { it.name }.toMutableMap()

        clusters.forEachIndexed { index, cluster ->
            currentCoroutineContext().ensureActive()
            val mangaInCluster = cluster.mapNotNull { mangaById[it.mangaId] }
            if (mangaInCluster.isEmpty()) return@forEachIndexed

            val dominantGenre = mangaInCluster
                .flatMap { it.genre.orEmpty() }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "Cluster ${index + 1}"

            val categoryName = "AI: $dominantGenre"
            val category = categoriesByName[categoryName] ?: when (val result = createCategoryWithName.await(categoryName)) {
                is CreateCategoryWithName.Result.Success -> result.category.also { categoriesByName[categoryName] = it }
                is CreateCategoryWithName.Result.InternalError -> throw result.error
            }
            mangaRepository.addMangaToCategory(mangaInCluster.map { it.id }, category.id)
        }
    }

    private suspend fun kMeans(embeddings: List<MangaEmbedding>, k: Int): List<List<MangaEmbedding>> {
        val context = currentCoroutineContext()
        var centroids = embeddings.shuffled(Random(0)).take(k).map { it.embedding }
        val clusters = MutableList(k) { mutableListOf<MangaEmbedding>() }

        repeat(10) { // Fixed iterations for simplicity
            clusters.forEach { it.clear() }

            // Assign to nearest centroid
            embeddings.forEach { embedding ->
                context.ensureActive()
                val nearestIndex = centroids.indices.minByOrNull { i ->
                    cosineDistance(embedding.embedding, centroids[i])
                } ?: 0
                clusters[nearestIndex].add(embedding)
            }

            // Update centroids
            centroids = clusters.mapIndexed { index, cluster ->
                if (cluster.isEmpty()) {
                    centroids[index]
                } else {
                    val dim = cluster[0].embedding.size
                    val newCentroid = FloatArray(dim)
                    cluster.forEach { emb ->
                        for (i in 0 until dim) newCentroid[i] += emb.embedding[i]
                    }
                    for (i in 0 until dim) newCentroid[i] /= cluster.size.toFloat()
                    newCentroid
                }
            }
        }
        return clusters
    }

    private fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return 1f - (dot / (sqrt(normA) * sqrt(normB)))
    }
}
