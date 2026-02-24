package com.shinku.reader.exh.ui.metadata

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.online.MetadataSource
import com.shinku.reader.exh.metadata.metadata.RaisedSearchMetadata
import com.shinku.reader.exh.source.getMainSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.manga.interactor.GetFlatMetadataById
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MetadataViewScreenModel(
    val mangaId: Long,
    val sourceId: Long,
    private val getFlatMetadataById: GetFlatMetadataById = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : StateScreenModel<MetadataViewState>(MetadataViewState.Loading) {
    private val _manga = MutableStateFlow<Manga?>(null)
    val manga = _manga.asStateFlow()

    init {
        screenModelScope.launchIO {
            _manga.value = getManga.await(mangaId)
        }

        screenModelScope.launchIO {
            val metadataSource = sourceManager.get(sourceId)?.getMainSource<MetadataSource<*, *>>()
            if (metadataSource == null) {
                mutableState.value = MetadataViewState.SourceNotFound
                return@launchIO
            }

            mutableState.value = when (val flatMetadata = getFlatMetadataById.await(mangaId)) {
                null -> MetadataViewState.MetadataNotFound
                else -> MetadataViewState.Success(flatMetadata.raise(metadataSource.metaClass))
            }
        }
    }
}

sealed class MetadataViewState {
    data object Loading : MetadataViewState()
    data class Success(val meta: RaisedSearchMetadata) : MetadataViewState()
    data object MetadataNotFound : MetadataViewState()
    data object SourceNotFound : MetadataViewState()
}
