package com.shinku.reader.ui.more

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.shinku.reader.R
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.core.preference.asState
import com.shinku.reader.data.download.DownloadManager
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.history.model.SanctuaryLevelInfo
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.more.MoreScreen
import com.shinku.reader.presentation.more.settings.screen.SettingsShinKuSettingsScreen
import com.shinku.reader.presentation.util.Tab
import com.shinku.reader.ui.download.DownloadQueueScreen
import com.shinku.reader.ui.history.HistoryTab
import com.shinku.reader.ui.setting.SettingsScreen
import com.shinku.reader.ui.stats.StatsScreen
import com.shinku.reader.ui.updates.UpdatesTab
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object MoreTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_more_enter)
            return TabOptions(
                index = 4u,
                title = stringResource(MR.strings.label_more),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(SettingsScreen())
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MoreScreenModel() }
        val downloadQueueState by screenModel.downloadQueueState.collectAsState()
        val readChapters by screenModel.readChapters.collectAsState()
        val readDuration by screenModel.readDuration.collectAsState()
        val readStreak by screenModel.readStreak.collectAsState()
        val levelInfo by screenModel.levelInfo.collectAsState()
        MoreScreen(
            downloadQueueStateProvider = { downloadQueueState },
            downloadedOnly = screenModel.downloadedOnly,
            onDownloadedOnlyChange = { screenModel.downloadedOnly = it },
            incognitoMode = screenModel.incognitoMode,
            onIncognitoModeChange = { screenModel.incognitoMode = it },
            // SY -->
            showNavUpdates = screenModel.showNavUpdates,
            showNavHistory = screenModel.showNavHistory,
            readChapters = readChapters,
            readDuration = readDuration,
            readStreak = readStreak,
            levelInfo = levelInfo,
            // SY <--
            onClickDownloadQueue = { navigator.push(DownloadQueueScreen) },
            onClickDataAndStorage = { navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage)) },
            onClickConfigureFeatures = { navigator.push(SettingsShinKuSettingsScreen) },
            onClickStats = { navigator.push(StatsScreen()) },
            onClickSettings = { navigator.push(SettingsScreen()) },
            onClickAbout = { navigator.push(SettingsScreen(SettingsScreen.Destination.About)) },
            onClickUpdates = { navigator.push(UpdatesTab) },
            onClickHistory = { navigator.push(HistoryTab) },
        )
    }
}

private class MoreScreenModel(
    private val downloadManager: DownloadManager = Injekt.get(),
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getReadingStats: com.shinku.reader.domain.history.interactor.GetReadingStats = Injekt.get(),
    preferences: BasePreferences = Injekt.get(),
    // SY -->
    uiPreferences: UiPreferences = Injekt.get(),
    // SY <--
) : ScreenModel {

    var downloadedOnly by preferences.downloadedOnly().asState(screenModelScope)
    var incognitoMode by preferences.incognitoMode().asState(screenModelScope)

    // SY -->
    val showNavUpdates by uiPreferences.showNavUpdates().asState(screenModelScope)
    val showNavHistory by uiPreferences.showNavHistory().asState(screenModelScope)

    private val _readChapters = MutableStateFlow(0)
    val readChapters = _readChapters.asStateFlow()

    private val _readDuration = MutableStateFlow(0L)
    val readDuration = _readDuration.asStateFlow()

    private val _readStreak = MutableStateFlow(0)
    val readStreak = _readStreak.asStateFlow()

    private val _levelInfo = MutableStateFlow<SanctuaryLevelInfo?>(null)
    val levelInfo = _levelInfo.asStateFlow()
    // SY <--

    private var _downloadQueueState: MutableStateFlow<DownloadQueueState> = MutableStateFlow(DownloadQueueState.Stopped)
    val downloadQueueState: StateFlow<DownloadQueueState> = _downloadQueueState.asStateFlow()

    init {
        // SY -->
        screenModelScope.launchIO {
            val libraryManga = getLibraryManga.await().distinctBy { it.id }
            val chaptersReadCount = libraryManga.sumOf { it.readCount }.toInt()
            val completedCount = libraryManga.count {
                (it.manga.status.toInt() == SManga.COMPLETED || it.manga.status.toInt() == SManga.PUBLISHING_FINISHED) && it.unreadCount == 0L
            }
            val stats = getReadingStats.await(
                libraryMangaCount = libraryManga.size,
                completedMangaCount = completedCount,
                readChapterCount = chaptersReadCount,
            )
            _readChapters.value = chaptersReadCount
            _readDuration.value = stats.totalReadDuration
            _readStreak.value = stats.currentStreak
            _levelInfo.value = stats.levelInfo
        }
        // SY <--
        // Handle running/paused status change and queue progress updating
        screenModelScope.launchIO {
            combine(
                downloadManager.isDownloaderRunning,
                downloadManager.queueState,
            ) { isRunning, downloadQueue -> Pair(isRunning, downloadQueue.size) }
                .collectLatest { (isDownloading, downloadQueueSize) ->
                    val pendingDownloadExists = downloadQueueSize != 0
                    _downloadQueueState.value = when {
                        !pendingDownloadExists -> DownloadQueueState.Stopped
                        !isDownloading -> DownloadQueueState.Paused(downloadQueueSize)
                        else -> DownloadQueueState.Downloading(downloadQueueSize)
                    }
                }
        }
    }
}

sealed interface DownloadQueueState {
    data object Stopped : DownloadQueueState
    data class Paused(val pending: Int) : DownloadQueueState
    data class Downloading(val pending: Int) : DownloadQueueState
}
