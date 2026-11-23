package com.shinku.reader.ui.browse

import com.shinku.reader.core.common.preference.CheckboxState
import com.shinku.reader.core.common.preference.mapAsCheckboxState
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.category.interactor.SetMangaCategories
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.chapter.interactor.SetMangaDefaultChapterFlags
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.interactor.GetDuplicateLibraryManga
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.toMangaUpdate
import com.shinku.reader.domain.track.interactor.AddTracks
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.source.Source
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant

sealed interface BulkAddPreparation {
    data class Ready(val categoryIds: List<Long>) : BulkAddPreparation
    data class NeedsCategoryChoice(val categories: List<Category>) : BulkAddPreparation
}

class BulkAddFavoritesHelper(
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val getDuplicateLibraryManga: GetDuplicateLibraryManga = Injekt.get(),
    private val shinkuPreferences: ShinKuPreferences = Injekt.get(),
) {
    suspend fun prepareCategories(): BulkAddPreparation {
        val categories = getCategories.subscribe()
            .firstOrNull()
            ?.filterNot { it.isSystemCategory }
            .orEmpty()
        val defaultCategoryId = libraryPreferences.defaultCategory().get()
        val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

        return when {
            defaultCategory != null -> BulkAddPreparation.Ready(listOf(defaultCategory.id))
            defaultCategoryId == 0 || categories.isEmpty() -> BulkAddPreparation.Ready(emptyList())
            else -> BulkAddPreparation.NeedsCategoryChoice(categories)
        }
    }

    suspend fun addFavorites(
        mangas: List<Manga>,
        categoryIds: List<Long>,
        sourceProvider: (Long) -> Source?,
    ): BulkFavoriteSummary {
        var added = 0
        var skippedAlreadyFavorite = 0
        var skippedDuplicates = 0
        var failed = 0
        val mergeGroupsEnabled = shinkuPreferences.enableLibraryMergeGroups().get()
        val filteredCategoryIds = categoryIds.filter { it != 0L }

        for (manga in mangas.distinctBy { BrowseEntryKey.of(it) }) {
            try {
                val current = if (manga.id > 0L) getManga.await(manga.id) ?: manga else manga
                if (current.favorite) {
                    skippedAlreadyFavorite++
                    continue
                }

                val duplicates = getDuplicateLibraryManga.invoke(current)
                val activeDuplicates = if (mergeGroupsEnabled) {
                    duplicates.filterNot { shinkuPreferences.isManualUnmerge(current.id, it.manga.id) }
                } else {
                    duplicates
                }
                if (activeDuplicates.isNotEmpty()) {
                    skippedDuplicates++
                    continue
                }

                val updated = current.copy(
                    favorite = true,
                    dateAdded = Instant.now().toEpochMilli(),
                )
                val saved = updateManga.await(updated.toMangaUpdate())
                if (!saved) {
                    failed++
                    continue
                }

                setMangaCategories.await(
                    mangaId = current.id,
                    categoryIds = filteredCategoryIds,
                )
                setMangaDefaultChapterFlags.await(current)
                sourceProvider(current.source)?.let { source ->
                    try {
                        addTracks.bindEnhancedTrackers(current, source)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Tracker binding is best-effort once manga is favorited
                    }
                }
                added++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed++
            }
        }

        return BulkFavoriteSummary(
            added = added,
            skippedAlreadyFavorite = skippedAlreadyFavorite,
            skippedDuplicates = skippedDuplicates,
            failed = failed,
        )
    }
}

class BulkFavoriteCoordinator(
    private val helper: BulkAddFavoritesHelper = BulkAddFavoritesHelper(),
) {
    private val _events = Channel<BulkFavoriteSummary>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var activeJob: Job? = null

    fun requestBulkAdd(
        scope: CoroutineScope,
        selected: List<Manga>,
        sourceProvider: (Long) -> Source?,
        onNeedsCategoryChoice: (List<Manga>, ImmutableList<CheckboxState.State<Category>>) -> Unit,
        onFinished: () -> Unit,
    ) {
        if (selected.isEmpty() || activeJob?.isActive == true) return
        activeJob = scope.launchIO {
            when (val prep = helper.prepareCategories()) {
                is BulkAddPreparation.Ready -> {
                    execute(selected, prep.categoryIds, sourceProvider, onFinished)
                }
                is BulkAddPreparation.NeedsCategoryChoice -> {
                    onNeedsCategoryChoice(
                        selected,
                        prep.categories.mapAsCheckboxState { false }.toImmutableList(),
                    )
                }
            }
        }
    }

    fun confirmBulkAdd(
        scope: CoroutineScope,
        mangas: List<Manga>,
        categoryIds: List<Long>,
        sourceProvider: (Long) -> Source?,
        onFinished: () -> Unit,
    ) {
        if (mangas.isEmpty() || activeJob?.isActive == true) return
        activeJob = scope.launchIO {
            execute(mangas, categoryIds, sourceProvider, onFinished)
        }
    }

    private suspend fun execute(
        mangas: List<Manga>,
        categoryIds: List<Long>,
        sourceProvider: (Long) -> Source?,
        onFinished: () -> Unit,
    ) {
        val summary = helper.addFavorites(
            mangas = mangas,
            categoryIds = categoryIds,
            sourceProvider = sourceProvider,
        )
        onFinished()
        _events.send(summary)
    }
}
