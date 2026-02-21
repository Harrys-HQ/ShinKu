package tachiyomi.domain.category.interactor

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.first
import tachiyomi.domain.category.interactor.UpdateCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.manga.interactor.GetLibraryManga

class SmartCategorizer(
    private val getLibraryManga: GetLibraryManga,
    private val getCategories: GetCategories,
    private val createCategoryWithName: CreateCategoryWithName,
    private val setMangaCategories: SetMangaCategories,
    private val deleteCategory: DeleteCategory,
    private val updateCategory: UpdateCategory,
) {

    data class CategoryNames(
        val all: String,
        val reading: String,
        val queue: String,
        val finished: String,
        val dropped: String,
    )

    suspend fun await(names: CategoryNames, onProgress: (Int, Int) -> Unit) {
        val libraryManga = getLibraryManga.await()
        var categories = getCategories.subscribe().first()

        // 1. Cleanup: Delete all categories that are not our smart categories
        val smartNames = setOf(
            names.all.trim().lowercase(),
            names.reading.trim().lowercase(),
            names.queue.trim().lowercase(),
            names.finished.trim().lowercase(),
            names.dropped.trim().lowercase()
        )

        categories.filterNot { it.isSystemCategory }.forEach { category ->
            if (category.name.trim().lowercase() !in smartNames) {
                deleteCategory.await(category.id)
            }
        }

        // Refresh categories list after deletion
        categories = getCategories.subscribe().first()

        // 2. Ensure categories exist in specific order
        val allCat = ensureCategory(categories, names.all)
        val readingCat = ensureCategory(categories, names.reading)
        val queueCat = ensureCategory(categories, names.queue)
        val droppedCat = ensureCategory(categories, names.dropped)
        val finishedCat = ensureCategory(categories, names.finished)

        // Force ordering in database
        val categoryUpdates = listOfNotNull(
            allCat?.let { CategoryUpdate(id = it.id, order = 0L) },
            readingCat?.let { CategoryUpdate(id = it.id, order = 1L) },
            queueCat?.let { CategoryUpdate(id = it.id, order = 2L) },
            droppedCat?.let { CategoryUpdate(id = it.id, order = 3L) },
            finishedCat?.let { CategoryUpdate(id = it.id, order = 4L) },
        )
        categoryUpdates.forEach { updateCategory.await(it) }

        // 3. Process each manga
        libraryManga.forEachIndexed { index, mangaWithChapters ->
            val manga = mangaWithChapters.manga
            val readCount = mangaWithChapters.readCount

            val statusCategory = when {
                // Priority 1: Finished
                manga.status.toInt() == SManga.COMPLETED || manga.status.toInt() == SManga.PUBLISHING_FINISHED -> finishedCat
                // Priority 2: Dropped
                manga.status.toInt() == SManga.CANCELLED || manga.status.toInt() == SManga.ON_HIATUS -> droppedCat
                // Priority 3: Reading
                readCount >= 5 -> readingCat
                // Priority 4: Queue
                else -> queueCat
            }

            val targetCategories = listOfNotNull(allCat?.id, statusCategory?.id)
            if (targetCategories.isNotEmpty()) {
                setMangaCategories.await(manga.id, targetCategories)
            }

            onProgress(index + 1, libraryManga.size)
        }
    }

    private suspend fun ensureCategory(categories: List<Category>, name: String): Category? {
        val existing = categories.find { it.name.trim().lowercase() == name.trim().lowercase() }
        if (existing != null) return existing

        return when (val result = createCategoryWithName.await(name)) {
            is CreateCategoryWithName.Result.Success -> result.category
            else -> null
        }
    }
}
