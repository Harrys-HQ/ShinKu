package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository
import com.shinku.reader.domain.source.service.FeedLimits
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class InsertFeedSavedSearchTest {

    private val repository = mockk<FeedSavedSearchRepository>(relaxed = true)
    private val insertFeedSavedSearch = InsertFeedSavedSearch(repository)

    private val limit = FeedLimits.MAX_SOURCES_PER_FEED

    private fun feed(source: Long, category: Long = 1L, global: Boolean = true) = FeedSavedSearch(
        id = -1,
        source = source,
        savedSearch = null,
        global = global,
        category = category,
    )

    @Test
    fun `await inserts while the tab is below the cap`() = runTest {
        coEvery { repository.countGlobalByCategory(1L) } returns (limit - 1).toLong()
        coEvery { repository.insert(any()) } returns 42L

        insertFeedSavedSearch.await(feed(source = 7)) shouldBe 42L

        coVerify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun `await refuses at the cap without touching the repository`() = runTest {
        coEvery { repository.countGlobalByCategory(1L) } returns limit.toLong()

        insertFeedSavedSearch.await(feed(source = 7)) shouldBe null

        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `the cap is per tab, so a full tab does not block another one`() = runTest {
        coEvery { repository.countGlobalByCategory(1L) } returns limit.toLong()
        coEvery { repository.countGlobalByCategory(2L) } returns 0L
        coEvery { repository.insert(any()) } returns 43L

        insertFeedSavedSearch.await(feed(source = 7, category = 2L)) shouldBe 43L

        coVerify(exactly = 1) { repository.insert(match { it.category == 2L }) }
    }

    @Test
    fun `source specific feeds are not capped`() = runTest {
        coEvery { repository.insert(any()) } returns 44L

        insertFeedSavedSearch.await(feed(source = 7, global = false)) shouldBe 44L

        coVerify(exactly = 0) { repository.countGlobalByCategory(any()) }
    }

    @Test
    fun `awaitAll is not capped so migrations and restores cannot lose entries`() = runTest {
        val batch = (1L..(limit * 2)).map { feed(source = it) }

        insertFeedSavedSearch.awaitAll(batch)

        coVerify(exactly = 1) { repository.insertAll(batch) }
        coVerify(exactly = 0) { repository.countGlobalByCategory(any()) }
    }

    @Test
    fun `await still returns null when the repository throws`() = runTest {
        coEvery { repository.countGlobalByCategory(1L) } returns 0L
        coEvery { repository.insert(any()) } throws IllegalStateException("db")

        insertFeedSavedSearch.await(feed(source = 7)) shouldBe null
    }
}
