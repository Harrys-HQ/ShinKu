package com.shinku.reader.domain.ai.model

data class MangaEmbedding(
    val mangaId: Long,
    val embedding: FloatArray,
    val lastMetadataUpdate: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MangaEmbedding
        if (mangaId != other.mangaId) return false
        if (!embedding.contentEquals(other.embedding)) return false
        return lastMetadataUpdate == other.lastMetadataUpdate
    }

    override fun hashCode(): Int {
        var result = mangaId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + lastMetadataUpdate.hashCode()
        return result
    }
}
