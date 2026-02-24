package com.shinku.reader.ui.library

import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.preference.asState
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.data.track.TrackerManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.core.common.preference.TriState
import com.shinku.reader.core.common.preference.getAndSet
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.category.interactor.SetDisplayMode
import com.shinku.reader.domain.category.interactor.SetSortModeForCategory
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.library.model.LibraryDisplayMode
import com.shinku.reader.domain.library.model.LibrarySort
import com.shinku.reader.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class LibrarySettingsScreenModel(
    val preferences: BasePreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val setDisplayMode: SetDisplayMode = Injekt.get(),
    private val setSortModeForCategory: SetSortModeForCategory = Injekt.get(),
    trackerManager: TrackerManager = Injekt.get(),
) : ScreenModel {

    val trackersFlow = trackerManager.loggedInTrackersFlow()
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
            initialValue = trackerManager.loggedInTrackers(),
        )

    // SY -->
    val grouping by libraryPreferences.groupLibraryBy().asState(screenModelScope)

    // SY <--
    fun toggleFilter(preference: (LibraryPreferences) -> Preference<TriState>) {
        preference(libraryPreferences).getAndSet {
            it.next()
        }
    }

    fun toggleTracker(id: Int) {
        toggleFilter { libraryPreferences.filterTracking(id) }
    }

    fun setDisplayMode(mode: LibraryDisplayMode) {
        setDisplayMode.await(mode)
    }

    fun setSort(category: Category?, mode: LibrarySort.Type, direction: LibrarySort.Direction) {
        screenModelScope.launchIO {
            setSortModeForCategory.await(category, mode, direction)
        }
    }

    // SY -->
    fun setGrouping(grouping: Int) {
        screenModelScope.launchIO {
            libraryPreferences.groupLibraryBy().set(grouping)
        }
    }
    // SY <--
}
