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

    @Test
    fun `inspect, decodeMetadata, and forEachManga skip unknown protobuf fields across all wire types`() = runTest {
        val backup = sampleBackup(mangaCount = 2, chaptersPerManga = 3)
        val singleMangaBytes = protoBuf.encodeToByteArray(BackupManga.serializer(), backup.backupManga.first())
        val mangaWithUnknown = Buffer().apply {
            write(singleMangaBytes)
            writeUnknownFields(this, baseField = 950)
        }.readByteArray()

        val baseBackupBytes = protoBuf.encodeToByteArray(Backup.serializer(), backup)
        val combinedBuffer = Buffer().apply {
            write(baseBackupBytes)
            // Append an extra BackupManga entry containing unknown nested fields
            writeVarint(this, ((1 shl 3) or 2).toLong())
            writeVarint(this, mangaWithUnknown.size.toLong())
            write(mangaWithUnknown)
            // Append top-level unknown fields across wire types 0, 1, 2, 5
            writeUnknownFields(this, baseField = 980)
        }

        val rawBytes = combinedBuffer.readByteArray()

        val inspection = BackupProtoStream.inspect(Buffer().write(rawBytes), protoBuf, "unknown")
        inspection.summary.titles shouldBe 3
        inspection.summary.chapters shouldBe 9L
        inspection.summary.historyEntries shouldBe 3L
        inspection.trackerIds shouldBe setOf(1, 2)

        val metadata = BackupProtoStream.decodeMetadata(Buffer().write(rawBytes), protoBuf, "unknown")
        metadata.mangaCount shouldBe 3
        metadata.backupSources shouldContainExactly backup.backupSources

        val streamed = mutableListOf<BackupManga>()
        BackupProtoStream.forEachManga(Buffer().write(rawBytes), protoBuf, "unknown") {
            streamed.add(it)
        }
        streamed.size shouldBe 3
        streamed.last().title shouldBe backup.backupManga.first().title
    }

    @Test
    fun `inspect, decodeMetadata, and forEachManga reject oversized record length before allocation`() = runTest {
        val oversizedHeader = Buffer().apply {
            writeVarint(this, ((1 shl 3) or 2).toLong())
            writeVarint(this, 65L * 1024L * 1024L) // > 64 MiB guard
            writeInt(0x12345678)
        }.readByteArray()

        shouldThrow<IOException> {
            BackupProtoStream.inspect(Buffer().write(oversizedHeader), protoBuf, "unknown")
        }
        shouldThrow<IOException> {
            BackupProtoStream.decodeMetadata(Buffer().write(oversizedHeader), protoBuf, "unknown")
        }
        shouldThrow<IOException> {
            BackupProtoStream.forEachManga(Buffer().write(oversizedHeader), protoBuf, "unknown") {}
        }
    }

    @Test
    fun `inspect and forEachManga handle chapter-heavy single entry and 200k-chapter backup with bounded heap`() = runTest {
        // 1. Single chapter-heavy entry: 1 manga with 15,000 chapters
        val singleHeavy = encodeGzipped(sampleBackup(mangaCount = 1, chaptersPerManga = 15_000))
        val t0 = System.nanoTime()
        val singleInspect = BackupProtoStream.openPayloadSource(singleHeavy.clone(), "json", "unknown").use {
            BackupProtoStream.inspect(it, protoBuf, "unknown")
        }
        val singleInspectMs = (System.nanoTime() - t0) / 1_000_000
        singleInspect.summary.titles shouldBe 1
        singleInspect.summary.chapters shouldBe 15_000L

        var singleRestored = 0
        val t1 = System.nanoTime()
        BackupProtoStream.openPayloadSource(singleHeavy, "json", "unknown").use {
            BackupProtoStream.forEachManga(it, protoBuf, "unknown") { manga ->
                singleRestored += manga.chapters.size
            }
        }
        val singleStreamMs = (System.nanoTime() - t1) / 1_000_000
        singleRestored shouldBe 15_000

        // 2. Many-entry large backup: 2,000 manga x 100 chapters = 200,000 chapters
        val largeGzipped = encodeGzipped(sampleBackup(mangaCount = 2_000, chaptersPerManga = 100))
        val compressedBytes = largeGzipped.size

        val runtime = Runtime.getRuntime()
        System.gc()
        val heapBefore = runtime.totalMemory() - runtime.freeMemory()

        val t2 = System.nanoTime()
        val largeInspect = BackupProtoStream.openPayloadSource(largeGzipped.clone(), "json", "unknown").use {
            BackupProtoStream.inspect(it, protoBuf, "unknown")
        }
        val largeInspectMs = (System.nanoTime() - t2) / 1_000_000
        val heapAfterInspect = runtime.totalMemory() - runtime.freeMemory()

        largeInspect.summary.titles shouldBe 2_000
        largeInspect.summary.chapters shouldBe 200_000L

        var streamedTitles = 0
        var streamedChapters = 0L
        val t3 = System.nanoTime()
        BackupProtoStream.openPayloadSource(largeGzipped, "json", "unknown").use {
            BackupProtoStream.forEachManga(it, protoBuf, "unknown") { manga ->
                streamedTitles++
                streamedChapters += manga.chapters.size
            }
        }
        val largeStreamMs = (System.nanoTime() - t3) / 1_000_000
        val heapAfterStream = runtime.totalMemory() - runtime.freeMemory()

        streamedTitles shouldBe 2_000
        streamedChapters shouldBe 200_000L

        println(
            "[BackupProtoStream Benchmark] maxHeap=${runtime.maxMemory() / (1024 * 1024)} MiB | " +
                "single(15k ch): inspect=${singleInspectMs}ms, stream=${singleStreamMs}ms | " +
                "large(2k x 100 ch, gz=${compressedBytes / 1024} KiB): " +
                "inspect=${largeInspectMs}ms, stream=${largeStreamMs}ms, " +
                "heapBefore=${heapBefore / (1024 * 1024)} MiB, " +
                "heapAfterInspect=${heapAfterInspect / (1024 * 1024)} MiB, " +
                "heapAfterStream=${heapAfterStream / (1024 * 1024)} MiB",
        )
    }

    private fun writeUnknownFields(sink: Buffer, baseField: Int) {
        // Wire type 0: varint
        writeVarint(sink, (((baseField) shl 3) or 0).toLong())
        writeVarint(sink, 987654321L)
        // Wire type 1: 64-bit fixed
        writeVarint(sink, (((baseField + 1) shl 3) or 1).toLong())
        sink.writeLongLe(0x0102030405060708L)
        // Wire type 2: length-delimited
        val payload = "unknown-forward-compat".encodeToByteArray()
        writeVarint(sink, (((baseField + 2) shl 3) or 2).toLong())
        writeVarint(sink, payload.size.toLong())
        sink.write(payload)
        // Wire type 5: 32-bit fixed
        writeVarint(sink, (((baseField + 3) shl 3) or 5).toLong())
        sink.writeIntLe(0x12345678)
    }

    private fun writeVarint(sink: Buffer, value: Long) {
        var v = value
        while (true) {
            if ((v and 0x7FL.inv()) == 0L) {
                sink.writeByte(v.toInt())
                return
            }
            sink.writeByte(((v.toInt() and 0x7F) or 0x80))
            v = v ushr 7
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
