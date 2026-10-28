package com.shinku.reader.domain.category.interactor

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.first
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.category.model.CategoryUpdate
import com.shinku.reader.domain.manga.interactor.GetLibraryManga

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

        // 1. Ensure smart categories exist without deleting user-created custom categories
        val allCat = ensureCategory(categories, names.all)
        categories = getCategories.subscribe().first()
        val readingCat = ensureCategory(categories, names.reading)
        categories = getCategories.subscribe().first()
        val queueCat = ensureCategory(categories, names.queue)
        categories = getCategories.subscribe().first()
        val droppedCat = ensureCategory(categories, names.dropped)
        categories = getCategories.subscribe().first()
        val finishedCat = ensureCategory(categories, names.finished)
        categories = getCategories.subscribe().first()

        val smartCategoryList = listOfNotNull(allCat, readingCat, queueCat, droppedCat, finishedCat)
        val smartCategoryIds = smartCategoryList.map { it.id }.toSet()

        // 2. Order smart categories first (0..4), followed by any user custom categories in their existing order
        val customCategories = categories
            .filterNot { it.isSystemCategory || it.id in smartCategoryIds }
            .sortedBy { it.order }

        val categoryUpdates = buildList {
            smartCategoryList.forEachIndexed { index, cat ->
                add(CategoryUpdate(id = cat.id, order = index.toLong()))
            }
            customCategories.forEachIndexed { index, cat ->
                add(CategoryUpdate(id = cat.id, order = (smartCategoryList.size + index).toLong()))
            }
        }
        categoryUpdates.forEach { updateCategory.await(it) }

        // 3. Process each manga based on actual reading progress + publication status, preserving custom categories
        libraryManga.forEachIndexed { index, mangaWithChapters ->
            val manga = mangaWithChapters.manga
            val readCount = mangaWithChapters.readCount
            val totalChapters = mangaWithChapters.totalChapters
            val unreadCount = mangaWithChapters.unreadCount
            val status = manga.status.toInt()

            val isFullyRead = totalChapters > 0L && readCount > 0L && unreadCount <= 0L
            val isDroppedOrHiatus = status == SManga.CANCELLED || status == SManga.ON_HIATUS

            val statusCategory = when {
                // Priority 1: Actually finished reading (all chapters read, and not an ongoing weekly series)
                isFullyRead && status != SManga.ONGOING -> finishedCat
                // Priority 2: Cancelled or on hiatus (and not already finished)
                isDroppedOrHiatus -> droppedCat
                // Priority 3: Started reading (at least 1 chapter read)
                readCount > 0L -> readingCat
                // Priority 4: Unread backlog / Plan to read
                else -> queueCat
            }

            val preservedCustomCategories = mangaWithChapters.categories.filter {
                it != Category.UNCATEGORIZED_ID && it !in smartCategoryIds
            }
            val targetCategories = (listOfNotNull(allCat?.id, statusCategory?.id) + preservedCustomCategories).distinct()
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
