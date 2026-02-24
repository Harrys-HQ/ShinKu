package com.shinku.reader.data.source

import kotlinx.coroutines.flow.Flow
import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.source.model.StubSource
import com.shinku.reader.domain.source.repository.StubSourceRepository

class StubSourceRepositoryImpl(
    private val handler: DatabaseHandler,
) : StubSourceRepository {

    override fun subscribeAll(): Flow<List<StubSource>> {
        return handler.subscribeToList { sourcesQueries.findAll(::mapStubSource) }
    }

    override suspend fun getStubSource(id: Long): StubSource? {
        return handler.awaitOneOrNull { sourcesQueries.findOne(id, ::mapStubSource) }
    }

    override suspend fun upsertStubSource(id: Long, lang: String, name: String) {
        handler.await { sourcesQueries.upsert(id, lang, name) }
    }

    private fun mapStubSource(
        id: Long,
        lang: String,
        name: String,
    ): StubSource = StubSource(id = id, lang = lang, name = name)
}
