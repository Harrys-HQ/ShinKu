package com.shinku.reader.domain.ai.interactor

import com.shinku.reader.domain.ai.model.MangaEmbedding
import com.shinku.reader.domain.category.interactor.CreateCategoryWithName
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.category.repository.CategoryRepository
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AiClusteringTest {
    private val mangaRepository = mockk<MangaRepository>(relaxUnitFun = true)
    private val categoryRepository = mockk<CategoryRepository>()
    private val createCategory = mockk<CreateCategoryWithName>()
    private val getEmbeddings = mockk<GetMangaEmbeddings>()
    private val clustering = AiClustering(mangaRepository, categoryRepository, createCategory, getEmbeddings)

    @Test
    fun `grouping is repeatable and uses additive writes with the created category IDs`() = runTest {
        val embeddings = (1L..12L).map { id ->
            MangaEmbedding(id, FloatArray(3) { if (it == (id % 3).toInt()) 1f else 0f }, 0)
        }
        val ids = embeddings.map { it.mangaId }
        coEvery { getEmbeddings.all() } returnsMany listOf(embeddings, embeddings.reversed())
        coEvery { mangaRepository.getMangaByIds(ids) } returns ids.dropLast(1).reversed().map { mangaId ->
            mockk<Manga> {
                coEvery { id } returns mangaId
                coEvery { genre } returns listOf("Genre ${mangaId % 3}")
            }
        }
        val categories = mutableListOf<Category>()
        coEvery { categoryRepository.getAll() } answers { categories.toList() }
        coEvery { createCategory.await(any()) } answers {
            val category = Category(100L + categories.size, firstArg(), categories.size.toLong(), 0)
            categories.add(category)
            CreateCategoryWithName.Result.Success(category)
        }
        val writes = mutableListOf<Pair<List<Long>, Long>>()
        coEvery { mangaRepository.addMangaToCategory(any(), any()) } answers {
            writes.add(firstArg<List<Long>>() to secondArg<Long>())
            Unit
        }

        clustering.await()
        val firstRun = writes.toList()
        assertEquals(ids.dropLast(1), firstRun.flatMap { it.first }.sorted())
        assertEquals(categories.map { it.id }.toSet(), firstRun.map { it.second }.toSet())

        writes.clear()
        clustering.await()
        assertEquals(firstRun, writes)
        coVerify(exactly = 2) { mangaRepository.getMangaByIds(ids) }
        coVerify(exactly = 0) { mangaRepository.setMangaCategories(any(), any()) }
        coVerify(exactly = categories.size) { createCategory.await(any()) }
    }
}
