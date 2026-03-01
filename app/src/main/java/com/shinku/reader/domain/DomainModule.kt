package com.shinku.reader.domain

import com.shinku.reader.domain.chapter.interactor.GetAvailableScanlators
import com.shinku.reader.domain.chapter.interactor.SetReadStatus
import com.shinku.reader.domain.chapter.interactor.SyncChaptersWithSource
import com.shinku.reader.domain.download.interactor.DeleteDownload
import com.shinku.reader.domain.extension.interactor.GetExtensionLanguages
import com.shinku.reader.domain.extension.interactor.GetExtensionSources
import com.shinku.reader.domain.extension.interactor.GetExtensionsByType
import com.shinku.reader.domain.extension.interactor.TrustExtension
import com.shinku.reader.domain.manga.interactor.GetExcludedScanlators
import com.shinku.reader.domain.manga.interactor.SetExcludedScanlators
import com.shinku.reader.domain.manga.interactor.SetMangaViewerFlags
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.source.interactor.GeminiVibeSearch
import com.shinku.reader.domain.source.interactor.TextRecognitionInteractor
import com.shinku.reader.domain.source.interactor.PanelDetectionInteractor
import com.shinku.reader.domain.source.interactor.GetEnabledSources
import com.shinku.reader.domain.source.interactor.GetIncognitoState
import com.shinku.reader.domain.source.interactor.GetLanguagesWithSources
import com.shinku.reader.domain.source.interactor.GetSourcesWithFavoriteCount
import com.shinku.reader.domain.source.interactor.SetMigrateSorting
import com.shinku.reader.domain.source.interactor.ToggleIncognito
import com.shinku.reader.domain.source.interactor.ToggleLanguage
import com.shinku.reader.domain.source.interactor.ToggleSource
import com.shinku.reader.domain.source.interactor.ToggleSourcePin
import com.shinku.reader.domain.track.interactor.AddTracks
import com.shinku.reader.domain.track.interactor.RefreshTracks
import com.shinku.reader.domain.track.interactor.SyncChapterProgressWithTrack
import com.shinku.reader.domain.track.interactor.TrackChapter
import com.shinku.reader.di.InjektModule
import com.shinku.reader.di.addFactory
import com.shinku.reader.di.addSingletonFactory
import com.shinku.reader.data.repository.ExtensionRepoRepositoryImpl
import com.shinku.reader.domain.chapter.interactor.FilterChaptersForDownload
import com.shinku.reader.domain.extensionrepo.interactor.CreateExtensionRepo
import com.shinku.reader.domain.extensionrepo.interactor.DeleteExtensionRepo
import com.shinku.reader.domain.extensionrepo.interactor.GetExtensionRepo
import com.shinku.reader.domain.extensionrepo.interactor.GetExtensionRepoCount
import com.shinku.reader.domain.extensionrepo.interactor.ReplaceExtensionRepo
import com.shinku.reader.domain.extensionrepo.interactor.UpdateExtensionRepo
import com.shinku.reader.domain.extensionrepo.repository.ExtensionRepoRepository
import com.shinku.reader.domain.extensionrepo.service.ExtensionRepoService
import com.shinku.reader.domain.migration.usecases.MigrateMangaUseCase
import com.shinku.reader.domain.upcoming.interactor.GetUpcomingManga
import com.shinku.reader.data.category.CategoryRepositoryImpl
import com.shinku.reader.data.chapter.ChapterRepositoryImpl
import com.shinku.reader.data.history.HistoryRepositoryImpl
import com.shinku.reader.data.manga.MangaRepositoryImpl
import com.shinku.reader.data.release.ReleaseServiceImpl
import com.shinku.reader.data.source.SourceHealthRepositoryImpl
import com.shinku.reader.data.source.SourceRepositoryImpl
import com.shinku.reader.data.source.StubSourceRepositoryImpl
import com.shinku.reader.data.track.TrackRepositoryImpl
import com.shinku.reader.data.updates.UpdatesRepositoryImpl
import com.shinku.reader.domain.category.interactor.CreateCategoryWithName
import com.shinku.reader.domain.category.interactor.DeleteCategory
import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.category.interactor.RenameCategory
import com.shinku.reader.domain.category.interactor.ReorderCategory
import com.shinku.reader.domain.category.interactor.ResetCategoryFlags
import com.shinku.reader.domain.category.interactor.SetDisplayMode
import com.shinku.reader.domain.category.interactor.SetMangaCategories
import com.shinku.reader.domain.category.interactor.SetSortModeForCategory
import com.shinku.reader.domain.category.interactor.SmartCategorizer
import com.shinku.reader.domain.category.interactor.UpdateCategory
import com.shinku.reader.domain.category.repository.CategoryRepository
import com.shinku.reader.domain.chapter.interactor.GetChapter
import com.shinku.reader.domain.chapter.interactor.GetChapterByUrlAndMangaId
import com.shinku.reader.domain.chapter.interactor.GetChaptersByMangaId
import com.shinku.reader.domain.chapter.interactor.SetMangaDefaultChapterFlags
import com.shinku.reader.domain.chapter.interactor.ShouldUpdateDbChapter
import com.shinku.reader.domain.chapter.interactor.UpdateChapter
import com.shinku.reader.domain.chapter.repository.ChapterRepository
import com.shinku.reader.domain.history.interactor.GetHistory
import com.shinku.reader.domain.history.interactor.GetReadingStats
import com.shinku.reader.domain.history.interactor.GetNextChapters
import com.shinku.reader.domain.history.interactor.GetTotalReadDuration
import com.shinku.reader.domain.history.interactor.RemoveHistory
import com.shinku.reader.domain.history.interactor.UpsertHistory
import com.shinku.reader.domain.history.repository.HistoryRepository
import com.shinku.reader.domain.manga.interactor.FetchInterval
import com.shinku.reader.domain.manga.interactor.GetDuplicateLibraryManga
import com.shinku.reader.domain.manga.interactor.GetFavorites
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.GetMangaByUrlAndSourceId
import com.shinku.reader.domain.manga.interactor.GetMangaWithChapters
import com.shinku.reader.domain.manga.interactor.NetworkToLocalManga
import com.shinku.reader.domain.manga.interactor.ResetViewerFlags
import com.shinku.reader.domain.manga.interactor.SetMangaChapterFlags
import com.shinku.reader.domain.manga.interactor.UpdateMangaNotes
import com.shinku.reader.domain.manga.repository.MangaRepository
import com.shinku.reader.domain.release.interactor.GetApplicationRelease
import com.shinku.reader.domain.release.service.ReleaseService
import com.shinku.reader.domain.source.interactor.GetRemoteManga
import com.shinku.reader.domain.source.interactor.GetSourceHealth
import com.shinku.reader.domain.source.interactor.GetSourcesWithNonLibraryManga
import com.shinku.reader.domain.source.interactor.UpdateSourceHealth
import com.shinku.reader.domain.source.repository.SourceHealthRepository
import com.shinku.reader.domain.source.repository.SourceRepository
import com.shinku.reader.domain.source.repository.StubSourceRepository
import com.shinku.reader.domain.track.interactor.DeleteTrack
import com.shinku.reader.domain.track.interactor.GetTracks
import com.shinku.reader.domain.track.interactor.GetTracksPerManga
import com.shinku.reader.domain.track.interactor.InsertTrack
import com.shinku.reader.domain.track.repository.TrackRepository
import com.shinku.reader.domain.updates.interactor.GetUpdates
import com.shinku.reader.domain.updates.repository.UpdatesRepository
import uy.kohesive.injekt.api.InjektRegistrar

class DomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<CategoryRepository> { CategoryRepositoryImpl(get()) }
        addFactory { GetCategories(get()) }
        addFactory { ResetCategoryFlags(get(), get()) }
        addFactory { SetDisplayMode(get()) }
        addFactory { SetSortModeForCategory(get(), get()) }
        addFactory { CreateCategoryWithName(get(), get()) }
        addFactory { RenameCategory(get()) }
        addFactory { ReorderCategory(get()) }
        addFactory { SmartCategorizer(get(), get(), get(), get(), get(), get()) }
        addFactory { UpdateCategory(get()) }
        addFactory { DeleteCategory(get(), get(), get()) }

        addSingletonFactory<MangaRepository> { MangaRepositoryImpl(get()) }
        addFactory { GetDuplicateLibraryManga(get()) }
        addFactory { GetFavorites(get()) }
        addFactory { GetLibraryManga(get()) }
        addFactory { GetMangaWithChapters(get(), get()) }
        addFactory { GetMangaByUrlAndSourceId(get()) }
        addFactory { GetManga(get()) }
        addFactory { GetNextChapters(get(), get(), get(), get()) }
        addFactory { GetUpcomingManga(get()) }
        addFactory { ResetViewerFlags(get()) }
        addFactory { SetMangaChapterFlags(get()) }
        addFactory { FetchInterval(get()) }
        addFactory { SetMangaDefaultChapterFlags(get(), get(), get()) }
        addFactory { SetMangaViewerFlags(get()) }
        addFactory { NetworkToLocalManga(get()) }
        addFactory { UpdateManga(get(), get()) }
        addFactory { UpdateMangaNotes(get()) }
        addFactory { SetMangaCategories(get()) }
        addFactory { GetExcludedScanlators(get()) }
        addFactory { SetExcludedScanlators(get()) }
        addFactory {
            MigrateMangaUseCase(
                get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            )
        }

        addSingletonFactory<ReleaseService> { ReleaseServiceImpl(get(), get()) }
        addFactory { GetApplicationRelease(get(), get()) }

        addSingletonFactory<TrackRepository> { TrackRepositoryImpl(get()) }
        addFactory { TrackChapter(get(), get(), get(), get()) }
        addFactory { AddTracks(get(), get(), get(), get()) }
        addFactory { RefreshTracks(get(), get(), get(), get()) }
        addFactory { DeleteTrack(get()) }
        addFactory { GetTracksPerManga(get(), get()) }
        addFactory { GetTracks(get()) }
        addFactory { InsertTrack(get()) }
        addFactory { SyncChapterProgressWithTrack(get(), get(), get()) }

        addSingletonFactory<ChapterRepository> { ChapterRepositoryImpl(get()) }
        addFactory { GetChapter(get()) }
        addFactory { GetChaptersByMangaId(get()) }
        addFactory { GetChapterByUrlAndMangaId(get()) }
        addFactory { UpdateChapter(get()) }
        addFactory { SetReadStatus(get(), get(), get(), get(), get()) }
        addFactory { ShouldUpdateDbChapter() }
        addFactory { SyncChaptersWithSource(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        addFactory { GetAvailableScanlators(get()) }
        addFactory { FilterChaptersForDownload(get(), get(), get(), get()) }

        addSingletonFactory<HistoryRepository> { HistoryRepositoryImpl(get()) }
        addFactory { GetHistory(get()) }
        addFactory { UpsertHistory(get()) }
        addFactory { RemoveHistory(get()) }
        addFactory { GetTotalReadDuration(get()) }
        addFactory { GetReadingStats(get(), get(), get()) }

        addFactory { DeleteDownload(get(), get()) }

        addFactory { GetExtensionsByType(get(), get()) }
        addFactory { GetExtensionSources(get()) }
        addFactory { GetExtensionLanguages(get(), get()) }

        addSingletonFactory<UpdatesRepository> { UpdatesRepositoryImpl(get()) }
        addFactory { GetUpdates(get()) }

        addSingletonFactory<SourceRepository> { SourceRepositoryImpl(get(), get()) }
        addSingletonFactory<StubSourceRepository> { StubSourceRepositoryImpl(get()) }
        addSingletonFactory<SourceHealthRepository> { SourceHealthRepositoryImpl(get()) }
        addFactory { GetEnabledSources(get(), get()) }
        addFactory { GetLanguagesWithSources(get(), get()) }
        addFactory { GetRemoteManga(get()) }
        addFactory { GetSourcesWithFavoriteCount(get(), get()) }
        addFactory { GetSourcesWithNonLibraryManga(get()) }
        addFactory { SetMigrateSorting(get()) }
        addFactory { ToggleLanguage(get()) }
        addFactory { ToggleSource(get()) }
        addFactory { ToggleSourcePin(get()) }
        addFactory { TrustExtension(get(), get()) }
        addFactory { GeminiVibeSearch(get()) }
        addFactory { TextRecognitionInteractor() }
        addFactory { PanelDetectionInteractor() }
        addFactory { GetSourceHealth(get()) }
        addFactory { UpdateSourceHealth(get()) }

        addSingletonFactory<ExtensionRepoRepository> { ExtensionRepoRepositoryImpl(get()) }

        addFactory { ExtensionRepoService(get(), get()) }
        addFactory { GetExtensionRepo(get()) }
        addFactory { GetExtensionRepoCount(get()) }
        addFactory { CreateExtensionRepo(get(), get()) }
        addFactory { DeleteExtensionRepo(get()) }
        addFactory { ReplaceExtensionRepo(get()) }
        addFactory { UpdateExtensionRepo(get(), get()) }
        addFactory { ToggleIncognito(get()) }
        addFactory { GetIncognitoState(get(), get(), get()) }
    }
}
