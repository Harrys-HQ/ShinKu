package com.shinku.reader.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.shinku.reader.data.AndroidDatabaseHandler
import com.shinku.reader.data.Database
import com.shinku.reader.data.DateColumnAdapter
import com.shinku.reader.data.History
import com.shinku.reader.data.Mangas
import com.shinku.reader.data.StringListColumnAdapter
import com.shinku.reader.data.UpdateStrategyColumnAdapter
import com.shinku.reader.data.backup.models.BackupExtensionRepos
import com.shinku.reader.data.backup.restore.restorers.ExtensionRepoRestorer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExtensionRepoRestoreTest {
    @Test
    fun restoresIdenticalRepositoriesOnceAndPreservesTheExistingSigningKey() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AndroidSqliteDriver(Database.Schema, context, name = null).use { driver ->
            val db = Database(
                driver = driver,
                historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
                mangasAdapter = Mangas.Adapter(genreAdapter = StringListColumnAdapter, update_strategyAdapter = UpdateStrategyColumnAdapter),
            )
            val restorer = ExtensionRepoRestorer(AndroidDatabaseHandler(db, driver))
            val original = BackupExtensionRepos("https://example.test/repo", "Original", null, "", "original-key")
            restorer(original)
            restorer(original)
            restorer(BackupExtensionRepos("", "Invalid", null, "", "invalid-key"))

            val conflict = runCatching {
                restorer(BackupExtensionRepos(original.baseUrl, "Other", null, "", "other-key"))
            }
            assertTrue(conflict.exceptionOrNull() is IllegalStateException)
            assertEquals(1L, db.extension_reposQueries.count().executeAsOne())
            assertEquals(original.signingKeyFingerprint, db.extension_reposQueries.findAll().executeAsOne().signing_key_fingerprint)
        }
    }
}
