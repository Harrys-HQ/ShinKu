package com.shinku.reader.di

import com.shinku.reader.ui.reader.ReaderViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { parameters ->
        ReaderViewModel(
            app = get(),
            savedState = parameters.get(),
            sourceManager = get(),
            downloadManager = get(),
            downloadProvider = get(),
            tempFileManager = get(),
            imageSaver = get(),
            readerPreferences = get(),
            basePreferences = get(),
            downloadPreferences = get(),
            trackPreferences = get(),
            trackChapter = get(),
            getManga = get(),
            getChaptersByMangaId = get(),
            getNextChapters = get(),
            upsertHistory = get(),
            updateChapter = get(),
            setMangaViewerFlags = get(),
            getIncognitoState = get(),
            libraryPreferences = get(),
            syncPreferences = get(),
            uiPreferences = get(),
            getFlatMetadataById = get(),
            getMergedMangaById = get(),
            getMergedReferencesById = get(),
            getMergedChaptersByMangaId = get(),
            setReadStatus = get(),
            geminiVibeSearch = get(),
            textRecognitionInteractor = get(),
            panelDetectionInteractor = get(),
            shinkuPreferences = get(),
        )
    }
}
