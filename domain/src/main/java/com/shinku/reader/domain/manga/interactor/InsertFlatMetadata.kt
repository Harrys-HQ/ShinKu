package com.shinku.reader.domain.manga.interactor

import eu.kanade.tachiyomi.source.online.MetadataSource
import com.shinku.reader.exh.metadata.metadata.RaisedSearchMetadata
import com.shinku.reader.exh.metadata.metadata.base.FlatMetadata
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.manga.repository.MangaMetadataRepository

class InsertFlatMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) : MetadataSource.InsertFlatMetadata {

    suspend fun await(flatMetadata: FlatMetadata) {
        try {
            mangaMetadataRepository.insertFlatMetadata(flatMetadata)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    override suspend fun await(metadata: RaisedSearchMetadata) {
        try {
            mangaMetadataRepository.insertMetadata(metadata)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
