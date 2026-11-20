package com.shinku.reader.data.backup

import com.shinku.reader.data.backup.models.Backup
import com.shinku.reader.data.backup.models.BackupCategory
import com.shinku.reader.data.backup.models.BackupChapter
import com.shinku.reader.data.backup.models.BackupHistory
import com.shinku.reader.data.backup.models.BackupManga
import com.shinku.reader.data.backup.models.BackupPreference
import com.shinku.reader.data.backup.models.BackupSource
import com.shinku.reader.data.backup.models.BackupTracking
import com.shinku.reader.data.backup.models.IntPreferenceValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer
import okio.gzip
import okio.buffer
import org.junit.jupiter.api.Test
import java.io.IOException

class BackupProtoStreamTest {

    private val protoBuf = ProtoBuf { }

    @Test
    fun `inspect matches full decode on gzipped backup with chapters, history, and tracking`() {
        val backup = sampleBackup(mangaCount = 25, chaptersPerManga = 40)
        val gzipped = encodeGzipped(backup)

        val inspection = BackupProtoStream.openPayloadSource(
            rawSource = gzipped,
            jsonErrorMessage = "json",
            unknownErrorMessage = "unknown",
        ).use { payload ->
            BackupProtoStream.inspect(payload, protoBuf, "unknown")
        }

        inspection.summary shouldBe BackupContentsSummary.from(backup)
        inspection.sources shouldContainExactly backup.backupSources
        inspection.trackerIds shouldBe setOf(1, 2)
    }

    @Test
    fun `decodeMetadata and forEachManga stream every entry accurately`() = runTest {
        val backup = sampleBackup(mangaCount = 15, chaptersPerManga = 10)
        val rawBytes = protoBuf.encodeToByteArray(Backup.serializer(), backup)

        val metadata = BackupProtoStream.openPayloadSource(
            rawSource = Buffer().write(rawBytes),
            jsonErrorMessage = "json",
            unknownErrorMessage = "unknown",
        ).use { payload ->
            BackupProtoStream.decodeMetadata(payload, protoBuf, "unknown")
        }

        metadata.mangaCount shouldBe backup.backupManga.size
        metadata.backupCategories.map { it.name to it.order } shouldContainExactly
            backup.backupCategories.map { it.name to it.order }
        metadata.backupSources shouldContainExactly backup.backupSources
        metadata.backupPreferences shouldContainExactly backup.backupPreferences

        val streamedManga = mutableListOf<BackupManga>()
        BackupProtoStream.openPayloadSource(
            rawSource = Buffer().write(rawBytes),
            jsonErrorMessage = "json",
            unknownErrorMessage = "unknown",
        ).use { payload ->
            BackupProtoStream.forEachManga(payload, protoBuf, "unknown") { manga ->
                streamedManga.add(manga)
            }
        }

        streamedManga shouldContainExactly backup.backupManga
    }

    @Test
    fun `inspect handles large chapter-heavy synthetic backup without error`() {
        val backup = sampleBackup(mangaCount = 300, chaptersPerManga = 200)
        val gzipped = encodeGzipped(backup)

        val inspection = BackupProtoStream.openPayloadSource(
            rawSource = gzipped,
            jsonErrorMessage = "json",
            unknownErrorMessage = "unknown",
        ).use { payload ->
            BackupProtoStream.inspect(payload, protoBuf, "unknown")
        }

        inspection.summary.titles shouldBe 300
        inspection.summary.chapters shouldBe 60_000L
        inspection.summary.historyEntries shouldBe 300L
        inspection.summary.categories shouldBe 2
        inspection.summary.settings shouldBe 1
    }

    @Test
    fun `openPayloadSource rejects json and empty streams with IOException`() {
        shouldThrow<IOException> {
            BackupProtoStream.openPayloadSource(
                rawSource = Buffer().writeUtf8("{\"version\": 1}"),
                jsonErrorMessage = "json",
                unknownErrorMessage = "unknown",
            )
        }.message shouldBe "json"

        shouldThrow<IOException> {
            BackupProtoStream.openPayloadSource(
                rawSource = Buffer(),
                jsonErrorMessage = "json",
                unknownErrorMessage = "unknown",
            )
        }.message shouldBe "unknown"
    }

    @Test
    fun `inspect rejects truncated protobuf stream with IOException`() {
        val rawBytes = protoBuf.encodeToByteArray(Backup.serializer(), sampleBackup(5, 5))
        val truncated = Buffer().write(rawBytes, 0, rawBytes.size / 2)

        shouldThrow<IOException> {
            BackupProtoStream.inspect(truncated, protoBuf, "unknown")
        }
    }

    private fun encodeGzipped(backup: Backup): Buffer {
        val raw = protoBuf.encodeToByteArray(Backup.serializer(), backup)
        val sink = Buffer()
        okio.GzipSink(sink).buffer().use { it.write(raw) }
        return sink
    }

    private fun sampleBackup(mangaCount: Int, chaptersPerManga: Int): Backup {
        val mangas = (1..mangaCount).map { idx ->
            val chapters = (1..chaptersPerManga).map { ch ->
                BackupChapter(
                    url = "/manga/$idx/ch/$ch",
                    name = "Chapter $ch",
                    chapterNumber = ch.toFloat(),
                )
            }
            BackupManga(
                source = if (idx % 2 == 0) 100L else 200L,
                url = "/manga/$idx",
                title = "Series $idx",
                chapters = chapters,
                categories = listOf(0L),
                tracking = listOf(
                    BackupTracking(
                        syncId = if (idx % 2 == 0) 1 else 2,
                        libraryId = idx.toLong(),
                    ),
                ),
                history = listOf(
                    BackupHistory(
                        url = "/manga/$idx/ch/1",
                        lastRead = 1_700_000_000L + idx,
                        readDuration = 120L,
                    ),
                ),
            )
        }
        return Backup(
            backupManga = mangas,
            backupCategories = listOf(
                BackupCategory(name = "Reading", order = 0),
                BackupCategory(name = "Plan to Read", order = 1),
            ),
            backupSources = listOf(
                BackupSource(name = "Source A", sourceId = 100L),
                BackupSource(name = "Source B", sourceId = 200L),
            ),
            backupPreferences = listOf(
                BackupPreference(key = "pref_test", value = IntPreferenceValue(42)),
            ),
        )
    }
}
