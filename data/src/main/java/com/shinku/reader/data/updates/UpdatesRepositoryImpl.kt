package com.shinku.reader.data.updates

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.shinku.reader.data.AndroidDatabaseHandler
import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.manga.model.MangaCover
import com.shinku.reader.domain.updates.model.UpdatesWithRelations
import com.shinku.reader.domain.updates.repository.UpdatesRepository
import com.shinku.reader.view.UpdatesView

class UpdatesRepositoryImpl(
    private val databaseHandler: DatabaseHandler,
) : UpdatesRepository {

    override suspend fun awaitWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): List<UpdatesWithRelations> {
        return databaseHandler.awaitList {
            updatesViewQueries.getUpdatesByReadStatus(
                read = read,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
        }
    }

    override fun subscribeAll(after: Long, limit: Long): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.getRecentUpdates(after, limit, ::mapUpdatesWithRelations)
        }.map {
            databaseHandler.awaitListExecutable {
                (databaseHandler as AndroidDatabaseHandler).getUpdatesQuery(after, limit)
            }
                .map(::mapUpdatesView)
        }
    }

    override fun subscribeWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.getUpdatesByReadStatus(
                read = read,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
        }
    }

    private fun mapUpdatesWithRelations(
        mangaId: Long,
        mangaTitle: String,
        chapterId: Long,
        chapterName: String,
        scanlator: String?,
        chapterUrl: String,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Long,
        sourceId: Long,
        favorite: Boolean,
        thumbnailUrl: String?,
        coverLastModified: Long,
        dateUpload: Long,
        dateFetch: Long,
    ): UpdatesWithRelations = UpdatesWithRelations(
        mangaId = mangaId,
        // SY -->
        ogMangaTitle = mangaTitle,
        // SY <--
        chapterId = chapterId,
        chapterName = chapterName,
        scanlator = scanlator,
        chapterUrl = chapterUrl,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = MangaCover(
            mangaId = mangaId,
            sourceId = sourceId,
            isMangaFavorite = favorite,
            ogUrl = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )

    fun mapUpdatesView(updatesView: UpdatesView): UpdatesWithRelations {
        return UpdatesWithRelations(
            mangaId = updatesView.mangaId,
            ogMangaTitle = updatesView.mangaTitle,
            chapterId = updatesView.chapterId,
            chapterName = updatesView.chapterName,
            scanlator = updatesView.scanlator,
            chapterUrl = updatesView.chapterUrl,
            read = updatesView.read,
            bookmark = updatesView.bookmark,
            lastPageRead = updatesView.last_page_read,
            sourceId = updatesView.source,
            dateFetch = updatesView.datefetch,
            coverData = MangaCover(
                mangaId = updatesView.mangaId,
                sourceId = updatesView.source,
                isMangaFavorite = updatesView.favorite,
                ogUrl = updatesView.thumbnailUrl,
                lastModified = updatesView.coverLastModified,
            ),
        )
    }
}
