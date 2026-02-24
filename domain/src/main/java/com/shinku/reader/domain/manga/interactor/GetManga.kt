package com.shinku.reader.domain.manga.interactor

import eu.kanade.tachiyomi.source.online.MetadataSource
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.repository.MangaRepository

class GetManga(
    private val mangaRepository: MangaRepository,
) : MetadataSource.GetMangaId {

    suspend fun await(id: Long): Manga? {
        return try {
            mangaRepository.getMangaById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun subscribe(id: Long): Flow<Manga> {
        return mangaRepository.getMangaByIdAsFlow(id)
    }

    fun subscribe(url: String, sourceId: Long): Flow<Manga?> {
        return mangaRepository.getMangaByUrlAndSourceIdAsFlow(url, sourceId)
    }

    // SY -->
    suspend fun await(url: String, sourceId: Long): Manga? {
        return mangaRepository.getMangaByUrlAndSourceId(url, sourceId)
    }

    override suspend fun awaitId(url: String, sourceId: Long): Long? {
        return await(url, sourceId)?.id
    }
    // SY <--
}
