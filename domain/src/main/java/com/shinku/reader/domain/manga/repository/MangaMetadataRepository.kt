package com.shinku.reader.domain.manga.repository

import com.shinku.reader.exh.metadata.metadata.RaisedSearchMetadata
import com.shinku.reader.exh.metadata.metadata.base.FlatMetadata
import com.shinku.reader.exh.metadata.sql.models.SearchMetadata
import com.shinku.reader.exh.metadata.sql.models.SearchTag
import com.shinku.reader.exh.metadata.sql.models.SearchTitle
import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.manga.model.Manga

interface MangaMetadataRepository {
    suspend fun getMetadataById(id: Long): SearchMetadata?

    fun subscribeMetadataById(id: Long): Flow<SearchMetadata?>

    suspend fun getTagsById(id: Long): List<SearchTag>

    fun subscribeTagsById(id: Long): Flow<List<SearchTag>>

    suspend fun getTitlesById(id: Long): List<SearchTitle>

    fun subscribeTitlesById(id: Long): Flow<List<SearchTitle>>

    suspend fun insertFlatMetadata(flatMetadata: FlatMetadata)

    suspend fun insertMetadata(metadata: RaisedSearchMetadata) = insertFlatMetadata(metadata.flatten())

    suspend fun getExhFavoriteMangaWithMetadata(): List<Manga>

    suspend fun getIdsOfFavoriteMangaWithMetadata(): List<Long>

    suspend fun getSearchMetadata(): List<SearchMetadata>
}
