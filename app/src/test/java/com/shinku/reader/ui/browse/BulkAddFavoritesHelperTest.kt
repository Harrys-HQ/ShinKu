package com.shinku.reader.ui.browse

import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.category.interactor.SetMangaCategories
import com.shinku.reader.domain.chapter.interactor.SetMangaDefaultChapterFlags
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.interactor.GetDuplicateLibraryManga
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.MangaWithChapterCount
import com.shinku.reader.domain.track.interactor.AddTracks
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.source.Source
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BulkAddFavoritesHelperTest {

    private fun testManga(
        id: Long,
        source: Long,
        title: String,
        favorite: Boolean = false,
    ): Manga {
        return Manga.create().copy(
            id = id,
            source = source,
            url = "/manga/$id",
            ogTitle = title,
            favorite = favorite,
        )
    }

    private fun createHelper(
        setMangaCategories: SetMangaCategories = mockk(relaxed = true),
        setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags = mockk(relaxed = true),
        addTracks: AddTracks = mockk(relaxed = true),
        getManga: GetManga = mockk(relaxed = true),
        updateManga: UpdateManga = mockk(),
        getDuplicateLibraryManga: GetDuplicateLibraryManga = mockk(),
        shinkuPreferences: ShinKuPreferences = mockk(),
        mergeGroupsEnabled: Boolean = false,
    ): BulkAddFavoritesHelper {
        val mergePref = mockk<Preference<Boolean>>()
        every { mergePref.get() } returns mergeGroupsEnabled
        every { shinkuPreferences.enableLibraryMergeGroups() } returns mergePref

        return BulkAddFavoritesHelper(
            libraryPreferences = mockk<LibraryPreferences>(relaxed = true),
            getCategories = mockk<GetCategories>(relaxed = true),
            setMangaCategories = setMangaCategories,
            setMangaDefaultChapterFlags = setMangaDefaultChapterFlags,
            addTracks = addTracks,
            getManga = getManga,
            updateManga = updateManga,
            getDuplicateLibraryManga = getDuplicateLibraryManga,
            shinkuPreferences = shinkuPreferences,
        )
    }

    @Test
    fun `addFavorites separates added, stale already favorite, duplicates, and failed items atomically`() = runTest {
        val setMangaCategories = mockk<SetMangaCategories>(relaxed = true)
        val setMangaDefaultChapterFlags = mockk<SetMangaDefaultChapterFlags>(relaxed = true)
        val addTracks = mockk<AddTracks>(relaxed = true)
        val getManga = mockk<GetManga>()
        val updateManga = mockk<UpdateManga>()
        val getDuplicateLibraryManga = mockk<GetDuplicateLibraryManga>()
        val shinkuPreferences = mockk<ShinKuPreferences>()

        val m1Added = testManga(1L, 10L, "First")
        val m2Duplicate = testManga(2L, 10L, "Duplicate")
        val m3Fail = testManga(3L, 10L, "Fail")
        val m4StaleFavorite = testManga(4L, 10L, "AlreadyFavoriteInDb", favorite = false)
        val m4Fresh = m4StaleFavorite.copy(favorite = true)

        coEvery { getManga.await(1L) } returns m1Added
        coEvery { getManga.await(2L) } returns m2Duplicate
        coEvery { getManga.await(3L) } returns m3Fail
        coEvery { getManga.await(4L) } returns m4Fresh

        coEvery { getDuplicateLibraryManga.invoke(m1Added) } returns emptyList()
        coEvery { getDuplicateLibraryManga.invoke(m2Duplicate) } returns listOf(mockk<MangaWithChapterCount>(relaxed = true))
        coEvery { getDuplicateLibraryManga.invoke(m3Fail) } returns emptyList()

        coEvery { updateManga.await(match { it.id == 1L }) } returns true
        coEvery { updateManga.await(match { it.id == 3L }) } returns false

        val source = mockk<Source>(relaxed = true)
        val helper = createHelper(
            setMangaCategories = setMangaCategories,
            setMangaDefaultChapterFlags = setMangaDefaultChapterFlags,
            addTracks = addTracks,
            getManga = getManga,
            updateManga = updateManga,
            getDuplicateLibraryManga = getDuplicateLibraryManga,
            shinkuPreferences = shinkuPreferences,
        )

        val summary = helper.addFavorites(
            mangas = listOf(m1Added, m2Duplicate, m3Fail, m4StaleFavorite),
            categoryIds = listOf(5L),
            sourceProvider = { source },
        )

        assertEquals(1, summary.added)
        assertEquals(1, summary.skippedAlreadyFavorite)
        assertEquals(1, summary.skippedDuplicates)
        assertEquals(1, summary.failed)
        assertTrue(summary.hasFailures)

        coVerify(exactly = 1) { setMangaCategories.await(1L, listOf(5L)) }
        coVerify(exactly = 1) { setMangaDefaultChapterFlags.await(m1Added) }
        coVerify(exactly = 1) { addTracks.bindEnhancedTrackers(m1Added, source) }

        // Atomic failure guard: failed updateManga must not write categories, chapter flags, or trackers
        coVerify(exactly = 0) { setMangaCategories.await(3L, any()) }
        coVerify(exactly = 0) { setMangaDefaultChapterFlags.await(m3Fail) }
        coVerify(exactly = 0) { addTracks.bindEnhancedTrackers(m3Fail, any()) }

        // Stale UI entry already favorited in DB must not be touched
        coVerify(exactly = 0) { updateManga.await(match { it.id == 4L }) }
        coVerify(exactly = 0) { setMangaCategories.await(4L, any()) }
    }

    @Test
    fun `tracker binding failure after updateManga succeeds still counts item as added`() = runTest {
        val addTracks = mockk<AddTracks>()
        val getManga = mockk<GetManga>()
        val updateManga = mockk<UpdateManga>()
        val getDuplicateLibraryManga = mockk<GetDuplicateLibraryManga>()
        val shinkuPreferences = mockk<ShinKuPreferences>()

        val m1 = testManga(10L, 20L, "TrackerError")
        val source = mockk<Source>(relaxed = true)
        coEvery { getManga.await(10L) } returns m1
        coEvery { getDuplicateLibraryManga.invoke(m1) } returns emptyList()
        coEvery { updateManga.await(match { it.id == 10L }) } returns true
        coEvery { addTracks.bindEnhancedTrackers(m1, source) } throws IllegalStateException("tracker offline")

        val helper = createHelper(
            addTracks = addTracks,
            getManga = getManga,
            updateManga = updateManga,
            getDuplicateLibraryManga = getDuplicateLibraryManga,
            shinkuPreferences = shinkuPreferences,
        )

        val summary = helper.addFavorites(
            mangas = listOf(m1),
            categoryIds = emptyList(),
            sourceProvider = { source },
        )

        assertEquals(1, summary.added)
        assertEquals(0, summary.failed)
        assertFalse(summary.hasFailures)
    }

    @Test
    fun `manually unmerged duplicates are added when merge groups are enabled`() = runTest {
        val getManga = mockk<GetManga>()
        val updateManga = mockk<UpdateManga>()
        val getDuplicateLibraryManga = mockk<GetDuplicateLibraryManga>()
        val shinkuPreferences = mockk<ShinKuPreferences>()

        val candidate = testManga(11L, 1L, "Solo Leveling")
        val existing = testManga(99L, 2L, "Solo Leveling", favorite = true)
        val existingWithCount = mockk<MangaWithChapterCount>(relaxed = true)
        every { existingWithCount.manga } returns existing
        every { shinkuPreferences.isManualUnmerge(11L, 99L) } returns true

        coEvery { getManga.await(11L) } returns candidate
        coEvery { getDuplicateLibraryManga.invoke(candidate) } returns listOf(existingWithCount)
        coEvery { updateManga.await(match { it.id == 11L }) } returns true

        val helper = createHelper(
            getManga = getManga,
            updateManga = updateManga,
            getDuplicateLibraryManga = getDuplicateLibraryManga,
            shinkuPreferences = shinkuPreferences,
            mergeGroupsEnabled = true,
        )

        val summary = helper.addFavorites(
            mangas = listOf(candidate),
            categoryIds = listOf(0L, 3L),
            sourceProvider = { null },
        )

        assertEquals(1, summary.added)
        assertEquals(0, summary.skippedDuplicates)
    }
}
