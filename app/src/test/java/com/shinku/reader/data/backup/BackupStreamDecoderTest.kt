package com.shinku.reader.data.backup

import com.shinku.reader.data.backup.models.Backup
import com.shinku.reader.data.backup.models.BackupCategory
import com.shinku.reader.data.backup.models.BackupChapter
import com.shinku.reader.data.backup.models.BackupExtensionRepos
import com.shinku.reader.data.backup.models.BackupHistory
import com.shinku.reader.data.backup.models.BackupManga
import com.shinku.reader.data.backup.models.BackupPreference
import com.shinku.reader.data.backup.models.BackupSavedSearch
import com.shinku.reader.data.backup.models.BackupSource
import com.shinku.reader.data.backup.models.BackupSourcePreferences
import com.shinku.reader.data.backup.models.BackupTracking
import com.shinku.reader.data.backup.models.StringPreferenceValue
import java.io.IOException
import java.nio.file.Files
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer
import okio.BufferedSink
import okio.Sink
import okio.Source
import okio.buffer
import okio.gzip
import okio.sink
import okio.source
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

class BackupStreamDecoderTest {
    @Test
    fun `decodes all known top-level fields with the existing protobuf schema`() {
        val expected = sampleBackup()
        val encoded = ProtoBuf.encodeToByteArray(Backup.serializer(), expected)

        val decoded = BackupStreamDecoder(ProtoBuf).decode(Buffer().write(encoded))

        assertArrayEquals(encoded, ProtoBuf.encodeToByteArray(Backup.serializer(), decoded))
    }

    @Test
    fun `decodes gzip input after decompression without changing the backup`() {
        val encoded = ProtoBuf.encodeToByteArray(Backup.serializer(), sampleBackup())
        val compressed = Buffer()
        val compressedSink: Sink = compressed
        compressedSink.gzip().buffer().use { it.write(encoded) }

        val compressedSource: Source = compressed.clone()
        val decoded = compressedSource.gzip().buffer().use { source ->
            BackupStreamDecoder(ProtoBuf).decode(source)
        }

        assertArrayEquals(encoded, ProtoBuf.encodeToByteArray(Backup.serializer(), decoded))
    }

    @Test
    fun `skips unknown top-level protobuf fields`() {
        val encoded = ProtoBuf.encodeToByteArray(Backup.serializer(), sampleBackup())
        val input = Buffer().write(encoded)
        writeVarint(input, (900L shl 3) or WIRE_TYPE_VARINT)
        writeVarint(input, 123L)
        writeVarint(input, (901L shl 3) or WIRE_TYPE_LENGTH_DELIMITED)
        writeVarint(input, 4L)
        input.write(byteArrayOf(1, 2, 3, 4))

        val decoded = BackupStreamDecoder(ProtoBuf).decode(input)

        assertArrayEquals(encoded, ProtoBuf.encodeToByteArray(Backup.serializer(), decoded))
    }

    @Test
    fun `rejects truncated records and malformed tags`() {
        val truncatedRecord = Buffer()
            .writeByte(((BACKUP_MANGA_FIELD shl 3) or WIRE_TYPE_LENGTH_DELIMITED).toInt())
            .writeByte(5)
            .write(byteArrayOf(1, 2))
        assertThrows(IOException::class.java) {
            BackupStreamDecoder(ProtoBuf).decode(truncatedRecord)
        }

        val invalidTag = Buffer().writeByte(0)
        assertThrows(IOException::class.java) {
            BackupStreamDecoder(ProtoBuf).decode(invalidTag)
        }
    }

    @Test
    fun `skips nested unknown groups and rejects malformed or excessive nesting`() {
        val input = Buffer()
        writeVarint(input, (900L shl 3) or 3L)
        writeVarint(input, (901L shl 3) or 3L)
        writeVarint(input, (902L shl 3) or 0L)
        writeVarint(input, 42L)
        writeVarint(input, (901L shl 3) or 4L)
        writeVarint(input, (900L shl 3) or 4L)
        assertTrue(BackupStreamDecoder(ProtoBuf).decode(input).backupManga.isEmpty())

        val mismatched = Buffer()
        writeVarint(mismatched, (900L shl 3) or 3L)
        writeVarint(mismatched, (901L shl 3) or 4L)
        assertThrows(IOException::class.java) { BackupStreamDecoder(ProtoBuf).decode(mismatched) }

        val tooDeep = Buffer()
        repeat(65) { writeVarint(tooDeep, (900L shl 3) or 3L) }
        repeat(65) { writeVarint(tooDeep, (900L shl 3) or 4L) }
        assertThrows(IOException::class.java) { BackupStreamDecoder(ProtoBuf).decode(tooDeep) }

        val tooLargeField = Buffer()
        writeVarint(tooLargeField, (1L shl 32) or 0L)
        writeVarint(tooLargeField, 1L)
        assertThrows(IOException::class.java) { BackupStreamDecoder(ProtoBuf).decode(tooLargeField) }
    }

    @Test
    fun `rejects a single record above the heap-aware safety cap before allocating it`() {
        val record = ProtoBuf.encodeToByteArray(
            BackupManga.serializer(),
            BackupManga(source = 1L, url = "/large-record"),
        )
        val input = Buffer()
        writeVarint(input, (BACKUP_MANGA_FIELD shl 3) or WIRE_TYPE_LENGTH_DELIMITED)
        writeVarint(input, record.size.toLong())
        input.write(record)

        assertThrows(BackupRecordTooLargeException::class.java) {
            BackupStreamDecoder(ProtoBuf, maxRecordBytes = record.size.toLong() - 1L).decode(input)
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SHINKU_RUN_LARGE_BACKUP_STRESS", matches = "1")
    fun `streams a large uncompressed backup with many normal-sized records`() {
        val file = Files.createTempFile("shinku-large-backup-", ".proto")
        val recordCount = 1_000
        val description = "x".repeat(160 * 1024)

        try {
            Files.newOutputStream(file).use { output ->
                val sink = output.sink().buffer()
                repeat(recordCount) { index ->
                    val record = ProtoBuf.encodeToByteArray(
                        BackupManga.serializer(),
                        BackupManga(source = 1L, url = "/$index", description = description),
                    )
                    writeVarint(sink, (BACKUP_MANGA_FIELD shl 3) or WIRE_TYPE_LENGTH_DELIMITED)
                    writeVarint(sink, record.size.toLong())
                    sink.write(record)
                }
                sink.close()
            }
            assertTrue(Files.size(file) > 150L * 1024 * 1024)

            val decoded = file.toFile().source().buffer().use { source ->
                BackupStreamDecoder(ProtoBuf).decode(source)
            }

            assertEquals(recordCount, decoded.backupManga.size)
            assertEquals(description.length, decoded.backupManga.last().description?.length)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun sampleBackup() = Backup(
        backupManga = listOf(
            BackupManga(
                source = 1L,
                url = "/series/one",
                title = "One",
                chapters = listOf(BackupChapter(url = "/series/one/1", name = "Chapter 1")),
                tracking = listOf(BackupTracking(syncId = 2, libraryId = 3L, title = "Tracked")),
                history = listOf(BackupHistory(url = "/series/one/1", lastRead = 123L)),
            ),
        ),
        backupCategories = listOf(BackupCategory(name = "Favorites", order = 2L, id = 3L, flags = 4L)),
        backupSources = listOf(BackupSource(name = "Fixture source", sourceId = 5L)),
        backupPreferences = listOf(BackupPreference(key = "app.key", value = StringPreferenceValue("value"))),
        backupSourcePreferences = listOf(
            BackupSourcePreferences(
                sourceKey = "5",
                prefs = listOf(BackupPreference(key = "source.key", value = StringPreferenceValue("source-value"))),
            ),
        ),
        backupExtensionRepo = listOf(
            BackupExtensionRepos(
                baseUrl = "https://example.invalid/index",
                name = "Fixture repo",
                shortName = "fixture",
                website = "https://example.invalid",
                signingKeyFingerprint = "fixture-fingerprint",
            ),
        ),
        backupSavedSearches = listOf(BackupSavedSearch(name = "Fixture search", query = "term")),
    )

    private fun writeVarint(sink: BufferedSink, value: Long) {
        var remaining = value
        while ((remaining and VARINT_VALUE_MASK.toLong().inv()) != 0L) {
            sink.writeByte(((remaining and VARINT_VALUE_MASK.toLong()) or VARINT_CONTINUATION_MASK.toLong()).toInt())
            remaining = remaining ushr VARINT_SHIFT_BITS
        }
        sink.writeByte(remaining.toInt())
    }

    private companion object {
        const val BACKUP_MANGA_FIELD = 1L
        const val WIRE_TYPE_VARINT = 0L
        const val WIRE_TYPE_LENGTH_DELIMITED = 2L
        const val VARINT_VALUE_MASK = 0x7f
        const val VARINT_CONTINUATION_MASK = 0x80
        const val VARINT_SHIFT_BITS = 7
    }
}
