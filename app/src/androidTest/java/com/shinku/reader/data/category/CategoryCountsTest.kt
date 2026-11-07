package com.shinku.reader.data.category

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.shinku.reader.data.CategoriesQueries
import com.shinku.reader.data.Database
import com.shinku.reader.data.Mangas_categoriesQueries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/** Isolated in-memory database: never opens or mutates the installed app's library. */
@RunWith(AndroidJUnit4::class)
class CategoryCountsTest {
    @Test
    fun countsIncludeOnlyDistinctLibraryTitlesAndInvalidateOnMembershipChanges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val driver = AndroidSqliteDriver(Database.Schema, context, name = null)
        try {
            val categories = CategoriesQueries(driver)
            val memberships = Mangas_categoriesQueries(driver)
            categories.insert("Reading", 0, 0)
            val readingId = categories.selectLastInsertedRowId().executeAsOne()
            categories.insert("Empty", 1, 0)
            val emptyId = categories.selectLastInsertedRowId().executeAsOne()
            for (id in 1..3) {
                driver.execute(
                    null,
                    """INSERT INTO mangas(_id, source, url, title, status, favorite, initialized, viewer, chapter_flags, cover_last_modified, date_added)
                       VALUES ($id, 0, '/fixture/$id', 'Synthetic title $id', 0, ${if (id == 3) 0 else 1}, 1, 0, 0, 0, 0)""",
                    0,
                )
            }
            val query = categories.getLibraryCounts { id, count -> id to count }
            val changes = AtomicInteger(0)
            val listener = Query.Listener { changes.incrementAndGet() }
            query.addListener(listener)
            try {
                assertEquals(0L, query.executeAsList().toMap()[emptyId])
                memberships.insert(1, readingId)
                memberships.insert(1, readingId) // Legacy duplicate memberships must not inflate the count.
                memberships.insert(2, readingId)
                memberships.insert(3, readingId) // Not in library.
                assertEquals(2L, query.executeAsList().toMap()[readingId])
                assertTrue(changes.get() > 0)
                val beforeRemove = changes.get()
                memberships.deleteMangaCategoryByMangaId(2)
                assertEquals(1L, query.executeAsList().toMap()[readingId])
                assertTrue(changes.get() > beforeRemove)
                assertEquals(0L, query.executeAsList().toMap()[emptyId])
            } finally {
                query.removeListener(listener)
            }
        } finally {
            driver.close()
        }
    }
}
