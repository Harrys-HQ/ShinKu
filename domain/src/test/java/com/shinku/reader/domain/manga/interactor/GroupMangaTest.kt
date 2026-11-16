package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.MergeGroupRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GroupMangaTest {

    private val repository = mockk<MergeGroupRepository>(relaxed = true)
    private val groupManga = GroupManga(repository)

    @Test
    fun `same manga id does nothing`() = runTest {
        groupManga.await(10L, 10L)
        coVerify(exactly = 0) { repository.createGroup(any()) }
        coVerify(exactly = 0) { repository.addMember(any(), any(), any()) }
    }

    @Test
    fun `neither manga has group creates new group and adds both`() = runTest {
        coEvery { repository.getGroupId(10L) } returns null
        coEvery { repository.getGroupId(20L) } returns null
        coEvery { repository.createGroup() } returns 99L

        groupManga.await(10L, 20L)

        coVerify(exactly = 1) { repository.createGroup() }
        coVerify(exactly = 1) { repository.addMember(99L, 10L, 0) }
        coVerify(exactly = 1) { repository.addMember(99L, 20L, 1) }
    }

    @Test
    fun `manga1 has group adds manga2 to existing group`() = runTest {
        coEvery { repository.getGroupId(10L) } returns 50L
        coEvery { repository.getGroupId(20L) } returns null

        groupManga.await(10L, 20L)

        coVerify(exactly = 0) { repository.createGroup(any()) }
        coVerify(exactly = 1) { repository.addMember(50L, 20L) }
    }

    @Test
    fun `manga2 has group adds manga1 to existing group`() = runTest {
        coEvery { repository.getGroupId(10L) } returns null
        coEvery { repository.getGroupId(20L) } returns 70L

        groupManga.await(10L, 20L)

        coVerify(exactly = 0) { repository.createGroup(any()) }
        coVerify(exactly = 1) { repository.addMember(70L, 10L) }
    }

    @Test
    fun `both have different groups merges group2 into group1`() = runTest {
        coEvery { repository.getGroupId(10L) } returns 50L
        coEvery { repository.getGroupId(20L) } returns 70L
        coEvery { repository.getMembers(70L) } returns listOf(20L, 21L)

        groupManga.await(10L, 20L)

        coVerify(exactly = 1) { repository.removeMember(20L) }
        coVerify(exactly = 1) { repository.addMember(50L, 20L) }
        coVerify(exactly = 1) { repository.removeMember(21L) }
        coVerify(exactly = 1) { repository.addMember(50L, 21L) }
        coVerify(exactly = 1) { repository.deleteGroup(70L) }
    }
}
