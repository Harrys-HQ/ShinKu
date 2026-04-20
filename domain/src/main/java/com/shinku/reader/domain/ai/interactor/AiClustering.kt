package com.shinku.reader.domain.ai.interactor

import com.shinku.reader.domain.ai.model.MangaEmbedding
import com.shinku.reader.domain.category.interactor.CreateCategoryWithName
import com.shinku.reader.domain.category.interactor.SetMangaCategories
import com.shinku.reader.domain.category.repository.CategoryRepository
import com.shinku.reader.domain.manga.repository.MangaRepository
import kotlin.math.sqrt

class AiClustering(
    private val mangaRepository: MangaRepository,
    private val categoryRepository: CategoryRepository,
    private val createCategoryWithName: CreateCategoryWithName,
    private val setMangaCategories: SetMangaCategories,
    private val getMangaEmbeddings: GetMangaEmbeddings,
) {
    suspend fun await() {
        val embeddings = getMangaEmbeddings.all().filter { it.embedding.isNotEmpty() }
        if (embeddings.size < 10) return // Not enough data to cluster

        val k = (sqrt(embeddings.size.toDouble()) / 2).toInt().coerceIn(3, 8)
        val clusters = kMeans(embeddings, k)

        clusters.forEachIndexed { index, cluster ->
            if (cluster.isEmpty()) return@forEachIndexed
            
            // Generate a name based on dominant genres in the cluster
            val mangaInCluster = cluster.map { mangaRepository.getMangaById(it.mangaId) }
            val dominantGenre = mangaInCluster
                .flatMap { it.genre ?: emptyList() }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "Cluster ${index + 1}"

            val categoryName = "AI: $dominantGenre"
            val categories = categoryRepository.getAll()
            val categoryId = categories.find { it.name == categoryName }?.id 
                ?: (createCategoryWithName.await(categoryName) as? CreateCategoryWithName.Result.Success)?.category?.id
                ?: return@forEachIndexed

            cluster.forEach { 
                setMangaCategories.await(it.mangaId, listOf(categoryId))
            }
        }
    }

    private fun kMeans(embeddings: List<MangaEmbedding>, k: Int): List<List<MangaEmbedding>> {
        if (embeddings.isEmpty()) return emptyList()
        
        // Randomly initialize centroids
        var centroids = embeddings.shuffled().take(k).map { it.embedding }
        val clusters = MutableList(k) { mutableListOf<MangaEmbedding>() }

        repeat(10) { // Fixed iterations for simplicity
            clusters.forEach { it.clear() }
            
            // Assign to nearest centroid
            embeddings.forEach { embedding ->
                val nearestIndex = centroids.indices.minByOrNull { i ->
                    cosineDistance(embedding.embedding, centroids[i])
                } ?: 0
                clusters[nearestIndex].add(embedding)
            }

            // Update centroids
            centroids = clusters.map { cluster ->
                if (cluster.isEmpty()) centroids[clusters.indexOf(cluster)]
                else {
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
