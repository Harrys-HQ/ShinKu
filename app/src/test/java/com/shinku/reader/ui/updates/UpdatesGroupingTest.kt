package com.shinku.reader.ui.updates

import com.shinku.reader.data.download.model.Download
import com.shinku.reader.domain.updates.model.UpdatesWithRelations
import com.shinku.reader.presentation.updates.UpdatesUiModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class UpdatesGroupingTest {
    private val today = LocalDate.of(2026, 11, 26)

    @Test
    fun `consecutive chapters group within dates without reordering intervening titles`() {
        val first = chapter(6, manga = 1)
        val second = chapter(5, manga = 1, selected = true)
        val other = chapter(4, manga = 2)
        val third = chapter(3, manga = 1)
        val yesterday = chapter(2, manga = 1, date = today.minusDays(1))

        val state = UpdatesScreenModel.State(items = persistentListOf(first, second, other, third, yesterday))

        assertEquals(
            listOf(
                UpdatesUiModel.Header(today),
                UpdatesUiModel.Group(listOf(first, second)),
                UpdatesUiModel.Group(listOf(other)),
                UpdatesUiModel.Group(listOf(third)),
                UpdatesUiModel.Header(today.minusDays(1)),
                UpdatesUiModel.Group(listOf(yesterday)),
            ),
            state.getUiModel(),
        )
        assertEquals(listOf(second), state.selected)
    }

    @Test
    fun `adding a newer chapter retains the group's expansion key`() {
        val middle = chapter(2, manga = 1)
        val oldest = chapter(1, manga = 1)
        val before = UpdatesScreenModel.State(items = persistentListOf(middle, oldest))
        val after = before.copy(items = before.items.add(0, chapter(3, manga = 1)))

        assertEquals(
            before.getUiModel().filterIsInstance<UpdatesUiModel.Group>().single().id,
            after.getUiModel().filterIsInstance<UpdatesUiModel.Group>().single().id,
        )
    }

    private fun chapter(id: Long, manga: Long, date: LocalDate = today, selected: Boolean = false): UpdatesItem {
        val update = mockk<UpdatesWithRelations> {
            every { chapterId } returns id
            every { mangaId } returns manga
            every { dateFetch } returns date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return UpdatesItem(update, { Download.State.NOT_DOWNLOADED }, { 0 }, selected)
    }
}
