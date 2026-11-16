package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.model.FeedCategory
import com.shinku.reader.domain.source.repository.FeedCategoryRepository
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FeedCategoryInteractorsTest {

    private val categoryRepository = mockk<FeedCategoryRepository>(relaxed = true)
    private val savedSearchRepository = mockk<FeedSavedSearchRepository>(relaxed = true)

    private val insertFeedCategory = InsertFeedCategory(categoryRepository)
    private val updateFeedCategory = UpdateFeedCategory(categoryRepository)
    private val deleteFeedCategory = DeleteFeedCategory(categoryRepository, savedSearchRepository)
    private val getFeedCategories = GetFeedCategories(categoryRepository)
    private val countFeedSavedSearch = CountFeedSavedSearchGlobalByCategory(savedSearchRepository)

    @Test
    fun `insert trims category name and delegates to repository`() = runTest {
        coEvery { categoryRepository.insert("Manhwa", "ko", false) } returns 2L

        val result = insertFeedCategory.await("  Manhwa  ", "ko", false)

        result shouldBe 2L
        coVerify(exactly = 1) { categoryRepository.insert("Manhwa", "ko", false) }
    }

    @Test
    fun `update trims category name and delegates to repository`() = runTest {
        updateFeedCategory.await(2L, "  Action Manga  ", "en")

        coVerify(exactly = 1) { categoryRepository.update(2L, "Action Manga", "en") }
    }

    @Test
    fun `updateSort delegates to repository`() = runTest {
        updateFeedCategory.updateSort(2L, 5L)

        coVerify(exactly = 1) { categoryRepository.updateSort(2L, 5L) }
    }

    @Test
    fun `delete ignores root global category id 1`() = runTest {
        deleteFeedCategory.await(1L)

        coVerify(exactly = 0) { savedSearchRepository.deleteByCategory(any()) }
        coVerify(exactly = 0) { categoryRepository.delete(any()) }
    }

    @Test
    fun `delete cascades to feeds and removes custom category`() = runTest {
        deleteFeedCategory.await(42L)

        coVerify(exactly = 1) { savedSearchRepository.deleteByCategory(42L) }
        coVerify(exactly = 1) { categoryRepository.delete(42L) }
    }

    @Test
    fun `getFeedCategories returns repository flow`() = runTest {
        val sample = listOf(
            FeedCategory(1L, "Global", 0L, "all", false),
            FeedCategory(2L, "Manhwa", 1L, "ko", false),
        )
        coEvery { categoryRepository.getAllAsFlow() } returns flowOf(sample)

        getFeedCategories.subscribe()
        coVerify(exactly = 1) { categoryRepository.getAllAsFlow() }
    }

    @Test
    fun `countFeedSavedSearchGlobalByCategory queries repository`() = runTest {
        coEvery { savedSearchRepository.countGlobalByCategory(2L) } returns 5L

        val count = countFeedSavedSearch.await(2L)

        count shouldBe 5L
        coVerify(exactly = 1) { savedSearchRepository.countGlobalByCategory(2L) }
    }
}
