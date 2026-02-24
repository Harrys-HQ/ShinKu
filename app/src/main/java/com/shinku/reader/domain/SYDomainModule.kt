package com.shinku.reader.domain

import android.app.Application
import com.shinku.reader.domain.manga.interactor.CreateSortTag
import com.shinku.reader.domain.manga.interactor.DeleteSortTag
import com.shinku.reader.domain.manga.interactor.GetPagePreviews
import com.shinku.reader.domain.manga.interactor.GetSortTag
import com.shinku.reader.domain.manga.interactor.ReorderSortTag
import com.shinku.reader.domain.source.interactor.CreateSourceCategory
import com.shinku.reader.domain.source.interactor.DeleteSourceCategory
import com.shinku.reader.domain.source.interactor.GetExhSavedSearch
import com.shinku.reader.domain.source.interactor.GetShowLatest
import com.shinku.reader.domain.source.interactor.GetSourceCategories
import com.shinku.reader.domain.source.interactor.RenameSourceCategory
import com.shinku.reader.domain.source.interactor.SetSourceCategories
import com.shinku.reader.domain.source.interactor.ToggleExcludeFromDataSaver
import com.shinku.reader.di.InjektModule
import com.shinku.reader.di.addFactory
import com.shinku.reader.di.addSingletonFactory
import eu.kanade.tachiyomi.source.online.MetadataSource
import com.shinku.reader.exh.search.SearchEngine
import com.shinku.reader.data.manga.CustomMangaRepositoryImpl
import com.shinku.reader.data.manga.FavoritesEntryRepositoryImpl
import com.shinku.reader.data.manga.MangaMergeRepositoryImpl
import com.shinku.reader.data.manga.MangaMetadataRepositoryImpl
import com.shinku.reader.data.source.FeedSavedSearchRepositoryImpl
import com.shinku.reader.data.source.SavedSearchRepositoryImpl
import com.shinku.reader.domain.chapter.interactor.DeleteChapters
import com.shinku.reader.domain.chapter.interactor.GetChapterByUrl
import com.shinku.reader.domain.chapter.interactor.GetMergedChaptersByMangaId
import com.shinku.reader.domain.manga.interactor.DeleteByMergeId
import com.shinku.reader.domain.manga.interactor.DeleteFavoriteEntries
import com.shinku.reader.domain.manga.interactor.DeleteMangaById
import com.shinku.reader.domain.manga.interactor.DeleteMergeById
import com.shinku.reader.domain.manga.interactor.GetAllManga
import com.shinku.reader.domain.manga.interactor.GetCustomMangaInfo
import com.shinku.reader.domain.manga.interactor.GetExhFavoriteMangaWithMetadata
import com.shinku.reader.domain.manga.interactor.GetFavoriteEntries
import com.shinku.reader.domain.manga.interactor.GetFlatMetadataById
import com.shinku.reader.domain.manga.interactor.GetIdsOfFavoriteMangaWithMetadata
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.GetMangaBySource
import com.shinku.reader.domain.manga.interactor.GetMergedManga
import com.shinku.reader.domain.manga.interactor.GetMergedMangaById
import com.shinku.reader.domain.manga.interactor.GetMergedMangaForDownloading
import com.shinku.reader.domain.manga.interactor.GetMergedReferencesById
import com.shinku.reader.domain.manga.interactor.GetReadMangaNotInLibraryView
import com.shinku.reader.domain.manga.interactor.GetSearchMetadata
import com.shinku.reader.domain.manga.interactor.GetSearchTags
import com.shinku.reader.domain.manga.interactor.GetSearchTitles
import com.shinku.reader.domain.manga.interactor.InsertFavoriteEntries
import com.shinku.reader.domain.manga.interactor.InsertFavoriteEntryAlternative
import com.shinku.reader.domain.manga.interactor.InsertFlatMetadata
import com.shinku.reader.domain.manga.interactor.InsertMergedReference
import com.shinku.reader.domain.manga.interactor.SetCustomMangaInfo
import com.shinku.reader.domain.manga.interactor.UpdateMergedSettings
import com.shinku.reader.domain.manga.repository.CustomMangaRepository
import com.shinku.reader.domain.manga.repository.FavoritesEntryRepository
import com.shinku.reader.domain.manga.repository.MangaMergeRepository
import com.shinku.reader.domain.manga.repository.MangaMetadataRepository
import com.shinku.reader.domain.source.interactor.CountFeedSavedSearchBySourceId
import com.shinku.reader.domain.source.interactor.CountFeedSavedSearchGlobal
import com.shinku.reader.domain.source.interactor.DeleteFeedSavedSearchById
import com.shinku.reader.domain.source.interactor.DeleteSavedSearchById
import com.shinku.reader.domain.source.interactor.GetFeedSavedSearchBySourceId
import com.shinku.reader.domain.source.interactor.GetFeedSavedSearchGlobal
import com.shinku.reader.domain.source.interactor.GetSavedSearchById
import com.shinku.reader.domain.source.interactor.GetSavedSearchBySourceId
import com.shinku.reader.domain.source.interactor.GetSavedSearchBySourceIdFeed
import com.shinku.reader.domain.source.interactor.GetSavedSearchGlobalFeed
import com.shinku.reader.domain.source.interactor.InsertFeedSavedSearch
import com.shinku.reader.domain.source.interactor.InsertSavedSearch
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository
import com.shinku.reader.domain.source.repository.SavedSearchRepository
import com.shinku.reader.domain.track.interactor.IsTrackUnfollowed
import uy.kohesive.injekt.api.InjektRegistrar
import xyz.nulldev.ts.api.http.serializer.FilterSerializer

class SYDomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addFactory { GetShowLatest(get()) }
        addFactory { ToggleExcludeFromDataSaver(get()) }
        addFactory { SetSourceCategories(get()) }
        addFactory { GetAllManga(get()) }
        addFactory { GetMangaBySource(get()) }
        addFactory { DeleteChapters(get()) }
        addFactory { DeleteMangaById(get()) }
        addFactory { FilterSerializer() }
        addFactory { GetChapterByUrl(get()) }
        addFactory { GetSourceCategories(get()) }
        addFactory { CreateSourceCategory(get()) }
        addFactory { RenameSourceCategory(get(), get()) }
        addFactory { DeleteSourceCategory(get()) }
        addFactory { GetSortTag(get()) }
        addFactory { CreateSortTag(get(), get()) }
        addFactory { DeleteSortTag(get(), get()) }
        addFactory { ReorderSortTag(get(), get()) }
        addFactory { GetPagePreviews(get(), get()) }
        addFactory { SearchEngine() }
        addFactory { IsTrackUnfollowed() }
        addFactory { GetReadMangaNotInLibraryView(get()) }

        // Required for [MetadataSource]
        addFactory<MetadataSource.GetMangaId> { GetManga(get()) }
        addFactory<MetadataSource.GetFlatMetadataById> { GetFlatMetadataById(get()) }
        addFactory<MetadataSource.InsertFlatMetadata> { InsertFlatMetadata(get()) }

        addSingletonFactory<MangaMetadataRepository> { MangaMetadataRepositoryImpl(get()) }
        addFactory { GetFlatMetadataById(get()) }
        addFactory { InsertFlatMetadata(get()) }
        addFactory { GetExhFavoriteMangaWithMetadata(get()) }
        addFactory { GetSearchMetadata(get()) }
        addFactory { GetSearchTags(get()) }
        addFactory { GetSearchTitles(get()) }
        addFactory { GetIdsOfFavoriteMangaWithMetadata(get()) }

        addSingletonFactory<MangaMergeRepository> { MangaMergeRepositoryImpl(get()) }
        addFactory { GetMergedManga(get()) }
        addFactory { GetMergedMangaById(get()) }
        addFactory { GetMergedReferencesById(get()) }
        addFactory { GetMergedChaptersByMangaId(get(), get()) }
        addFactory { InsertMergedReference(get()) }
        addFactory { UpdateMergedSettings(get()) }
        addFactory { DeleteByMergeId(get()) }
        addFactory { DeleteMergeById(get()) }
        addFactory { GetMergedMangaForDownloading(get()) }

        addSingletonFactory<FavoritesEntryRepository> { FavoritesEntryRepositoryImpl(get()) }
        addFactory { GetFavoriteEntries(get()) }
        addFactory { InsertFavoriteEntries(get()) }
        addFactory { DeleteFavoriteEntries(get()) }
        addFactory { InsertFavoriteEntryAlternative(get()) }

        addSingletonFactory<SavedSearchRepository> { SavedSearchRepositoryImpl(get()) }
        addFactory { GetSavedSearchById(get()) }
        addFactory { GetSavedSearchBySourceId(get()) }
        addFactory { DeleteSavedSearchById(get()) }
        addFactory { InsertSavedSearch(get()) }
        addFactory { GetExhSavedSearch(get(), get(), get()) }

        addSingletonFactory<FeedSavedSearchRepository> { FeedSavedSearchRepositoryImpl(get()) }
        addFactory { InsertFeedSavedSearch(get()) }
        addFactory { DeleteFeedSavedSearchById(get()) }
        addFactory { GetFeedSavedSearchGlobal(get()) }
        addFactory { GetFeedSavedSearchBySourceId(get()) }
        addFactory { CountFeedSavedSearchGlobal(get()) }
        addFactory { CountFeedSavedSearchBySourceId(get()) }
        addFactory { GetSavedSearchGlobalFeed(get()) }
        addFactory { GetSavedSearchBySourceIdFeed(get()) }

        addSingletonFactory<CustomMangaRepository> { CustomMangaRepositoryImpl(get<Application>()) }
        addFactory { GetCustomMangaInfo(get()) }
        addFactory { SetCustomMangaInfo(get()) }
    }
}
