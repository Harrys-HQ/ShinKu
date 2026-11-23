package com.shinku.reader.data.diagnostics

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.shinku.reader.data.Database
import com.shinku.reader.data.Source_healthQueries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SourceHealthMigrationTest {
    @Test
    fun migrationPreservesLegacyCountersAndAcceptsTypedProbeResults() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val name = "source-health-migration-${UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
                db.execSQL(
                    """
                    CREATE TABLE source_health (
                        source_id INTEGER NOT NULL PRIMARY KEY,
                        last_success INTEGER DEFAULT 0,
                        last_failure INTEGER DEFAULT 0,
                        failure_count INTEGER NOT NULL DEFAULT 0,
                        success_count INTEGER NOT NULL DEFAULT 0,
                        avg_latency INTEGER NOT NULL DEFAULT 0,
                        last_error TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL("INSERT INTO source_health VALUES (42, 100, 200, 3, 5, 125, 'legacy failure')")
                db.version = 44
            }
            val driver = AndroidSqliteDriver(Database.Schema, context, name)
            try {
                val queries = Source_healthQueries(driver)
                val legacy = queries.findOne(42).executeAsOne()
                assertEquals(3L, legacy.failure_count)
                assertEquals(5L, legacy.success_count)
                assertEquals(125L, legacy.avg_latency)
                assertEquals("legacy failure", legacy.last_error)
                assertEquals(0L, legacy.consecutive_failures)
                assertEquals("UNKNOWN", legacy.last_category)
                assertNull(legacy.last_status_code)

                queries.updateOutcome(
                    id = 42,
                    last_success = 300,
                    last_failure = 0,
                    success_delta = 1,
                    failure_delta = 0,
                    latency = 175,
                    error = null,
                    category = "HEALTHY",
                    status_code = 200,
                    probe_type = "BASE_URL_GET",
                    probe_phase = "SOURCE_REQUEST",
                    timestamp = 300,
                    correlation_id = "diag-fixture",
                    probe_target = "https://example.org/",
                )
                val updated = queries.findOne(42).executeAsOne()
                assertEquals(3L, updated.failure_count)
                assertEquals(6L, updated.success_count)
                assertEquals(150L, updated.avg_latency)
                assertEquals(300L, updated.last_checked)
                assertEquals("HEALTHY", updated.last_category)
                assertEquals("diag-fixture", updated.last_correlation_id)
            } finally {
                driver.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
