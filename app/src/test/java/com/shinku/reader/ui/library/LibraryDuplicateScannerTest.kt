package com.shinku.reader.ui.library

import com.shinku.reader.domain.library.model.LibraryManga
import com.shinku.reader.domain.manga.model.Manga
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibraryDuplicateScannerTest {

    private fun libManga(id: Long, source: Long, title: String, chapters: Long): LibraryManga {
        val manga = Manga.create().copy(
            id = id,
            source = source,
            url = "/m/$id",
            ogTitle = title,
            favorite = false,
        )
        return LibraryManga(
            manga = manga,
            categories = listOf(0L),
            totalChapters = chapters,
            readCount = 0L,
            bookmarkCount = 0L,
            latestUpload = 0L,
            chapterFetchedAt = 0L,
            lastRead = 0L,
        )
    }

    @Test
    fun `groups duplicate titles ignoring case and extra whitespace and excludes fully grouped sets`() {
        val a1 = libManga(1L, 10L, "Solo Leveling", 180L)
        val a2 = libManga(2L, 20L, "  solo   leveling ", 200L)
        val b1 = libManga(3L, 10L, "Already Grouped", 50L)
        val b2 = libManga(4L, 20L, "already grouped", 40L)
        val unique = libManga(5L, 30L, "Unique Title", 12L)
        val blank = libManga(6L, 40L, "   ", 5L)
        val blank2 = libManga(7L, 50L, "", 5L)

        val memberships = mapOf(
            3L to 99L,
            4L to 99L,
        )

        val duplicates = LibraryDuplicateScanner.findDuplicateGroups(
            libraryManga = listOf(a1, a2, b1, b2, unique, blank, blank2),
            memberships = memberships,
        )

        assertEquals(1, duplicates.size)
        val group = duplicates.first()
        assertEquals("solo leveling", group.normalizedTitle)
        assertEquals(listOf(2L, 1L), group.entries.map { it.id })
        assertTrue(group.mergeGroupIds.isEmpty())
    }

    @Test
    fun `retains partially grouped sets when another duplicate is ungrouped`() {
        val m1 = libManga(1L, 10L, "Tower of God", 500L)
        val m2 = libManga(2L, 20L, "tower of god", 450L)
        val m3 = libManga(3L, 30L, "Tower Of God", 400L)

        val memberships = mapOf(
            1L to 99L,
            2L to 99L,
        )

        val duplicates = LibraryDuplicateScanner.findDuplicateGroups(
            libraryManga = listOf(m1, m2, m3),
            memberships = memberships,
        )

        assertEquals(1, duplicates.size)
        val group = duplicates.first()
        assertEquals(listOf(1L, 2L, 3L), group.entries.map { it.id })
        assertEquals(setOf(99L), group.mergeGroupIds)
        assertEquals(setOf(1L, 2L), group.groupedMemberIds)
    }

    @Test
    fun `symmetric ignore excludes group regardless of chapter count order and counts ignored`() {
        val a1 = libManga(1L, 10L, "Solo Leveling", 180L)
        val a2 = libManga(2L, 20L, "solo leveling", 250L)
        val ignoredPairs = setOf(LibraryDuplicateScanner.pairKey(1L, 2L))

        val visible = LibraryDuplicateScanner.findDuplicateGroups(
            libraryManga = listOf(a1, a2),
            memberships = emptyMap(),
            manualUnmergePairs = ignoredPairs,
        )
        val ignoredCount = LibraryDuplicateScanner.countIgnoredDuplicateGroups(
            libraryManga = listOf(a1, a2),
            memberships = emptyMap(),
            manualUnmergePairs = ignoredPairs,
        )

        assertTrue(visible.isEmpty())
        assertEquals(1, ignoredCount)
    }

    @Test
    fun `adding a third unignored duplicate re-surfaces the group`() {
        val a1 = libManga(1L, 10L, "Solo Leveling", 180L)
        val a2 = libManga(2L, 20L, "solo leveling", 250L)
        val a3 = libManga(3L, 30L, "Solo  Leveling", 190L)
        val ignoredPairs = setOf(LibraryDuplicateScanner.pairKey(1L, 2L))

        val visible = LibraryDuplicateScanner.findDuplicateGroups(
            libraryManga = listOf(a1, a2, a3),
            memberships = emptyMap(),
            manualUnmergePairs = ignoredPairs,
        )
        val ignoredCount = LibraryDuplicateScanner.countIgnoredDuplicateGroups(
            libraryManga = listOf(a1, a2, a3),
            memberships = emptyMap(),
            manualUnmergePairs = ignoredPairs,
        )

        assertEquals(1, visible.size)
        assertEquals(listOf(2L, 3L, 1L), visible.first().entries.map { it.id })
        assertEquals(0, ignoredCount)
    }
}
