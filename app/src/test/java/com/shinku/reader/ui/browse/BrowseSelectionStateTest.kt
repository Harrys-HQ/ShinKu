package com.shinku.reader.ui.browse

import com.shinku.reader.domain.manga.model.Manga
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrowseSelectionStateTest {

    private fun testManga(id: Long, source: Long, url: String, title: String): Manga {
        return Manga.create().copy(
            id = id,
            source = source,
            url = url,
            ogTitle = title,
            favorite = false,
        )
    }

    @Test
    fun `selectAllLoaded selects current loaded items and invertLoaded toggles cleanly`() {
        val a = testManga(1L, 100L, "/manga/a", "Alpha")
        val b = testManga(2L, 100L, "/manga/b", "Beta")
        val c = testManga(3L, 100L, "/manga/c", "Gamma")

        val stateAfterSelect = BrowseSelectionState().selectAllLoaded(listOf(a, b))
        assertTrue(stateAfterSelect.selectionMode)
        assertTrue(stateAfterSelect.isSelected(a))
        assertTrue(stateAfterSelect.isSelected(b))
        assertFalse(stateAfterSelect.isSelected(c))
        assertEquals(2, stateAfterSelect.selectedCount)

        val inverted = stateAfterSelect.invertLoaded(listOf(a, b, c))
        assertFalse(inverted.isSelected(a))
        assertFalse(inverted.isSelected(b))
        assertTrue(inverted.isSelected(c))
        assertEquals(1, inverted.selectedCount)
    }

    @Test
    fun `entries with identical titles across distinct sources stay separate`() {
        val fromSource1 = testManga(10L, 1L, "/same-title", "Berserk")
        val fromSource2 = testManga(20L, 2L, "/same-title", "Berserk")

        val state = BrowseSelectionState()
            .toggle(fromSource1)
            .toggle(fromSource2)

        assertEquals(2, state.selectedCount)
        assertTrue(state.isSelected(fromSource1))
        assertTrue(state.isSelected(fromSource2))

        val afterUntoggle = state.toggle(fromSource1)
        assertEquals(1, afterUntoggle.selectedCount)
        assertFalse(afterUntoggle.isSelected(fromSource1))
        assertTrue(afterUntoggle.isSelected(fromSource2))
    }

    @Test
    fun `clear keeps selectionMode active while exitSelectionMode clears both`() {
        val a = testManga(1L, 100L, "/manga/a", "Alpha")
        val selected = BrowseSelectionState().toggle(a)
        assertEquals(1, selected.selectedCount)

        val cleared = selected.clear()
        assertTrue(cleared.selectionMode)
        assertEquals(0, cleared.selectedCount)

        val exited = selected.exitSelectionMode()
        assertFalse(exited.selectionMode)
        assertEquals(0, exited.selectedCount)
    }

    @Test
    fun `bulk favorite summary omits zero counts and reports partial failures honestly`() {
        val partial = BulkFavoriteSummary(
            added = 2,
            skippedAlreadyFavorite = 1,
            skippedDuplicates = 1,
            failed = 1,
        )
        assertTrue(partial.hasFailures)
        assertEquals(
            "Added 2 · 1 already in library · 1 duplicates skipped · 1 failed",
            partial.formatMessage(
                formatAdded = { "Added $it" },
                formatAlreadyFavorite = { "$it already in library" },
                formatDuplicatesSkipped = { "$it duplicates skipped" },
                formatFailed = { "$it failed" },
            ),
        )

        val clean = BulkFavoriteSummary(
            added = 3,
            skippedAlreadyFavorite = 0,
            skippedDuplicates = 0,
            failed = 0,
        )
        assertFalse(clean.hasFailures)
        assertEquals(
            "Added 3",
            clean.formatMessage(
                formatAdded = { "Added $it" },
                formatAlreadyFavorite = { "$it already in library" },
                formatDuplicatesSkipped = { "$it duplicates skipped" },
                formatFailed = { "$it failed" },
            ),
        )
    }
}
