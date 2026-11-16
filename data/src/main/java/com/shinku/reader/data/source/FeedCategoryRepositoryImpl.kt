package com.shinku.reader.data.source

import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.source.model.FeedCategory
import com.shinku.reader.domain.source.repository.FeedCategoryRepository
import kotlinx.coroutines.flow.Flow

class FeedCategoryRepositoryImpl(
    private val handler: DatabaseHandler,
) : FeedCategoryRepository {

    override suspend fun getAll(): List<FeedCategory> {
        return handler.awaitList { feed_categoriesQueries.selectAll(FeedCategoryMapper::map) }
    }

    override fun getAllAsFlow(): Flow<List<FeedCategory>> {
        return handler.subscribeToList { feed_categoriesQueries.selectAll(FeedCategoryMapper::map) }
    }

    override suspend fun getById(id: Long): FeedCategory? {
        return handler.awaitOneOrNull { feed_categoriesQueries.selectById(id, FeedCategoryMapper::map) }
    }

    override suspend fun insert(name: String, lang: String, explore: Boolean): Long {
        return handler.awaitOneExecutable(true) {
            feed_categoriesQueries.insert(name, lang, explore)
            feed_categoriesQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun update(id: Long, name: String, lang: String) {
        handler.await { feed_categoriesQueries.update(name, lang, id) }
    }

    override suspend fun updateSort(id: Long, sort: Long) {
        handler.await { feed_categoriesQueries.updateSort(sort, id) }
    }

    override suspend fun delete(id: Long) {
        handler.await { feed_categoriesQueries.delete(id) }
    }
}
