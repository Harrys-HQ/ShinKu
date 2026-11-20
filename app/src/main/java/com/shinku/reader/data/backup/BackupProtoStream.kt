package com.shinku.reader.data.backup

import com.shinku.reader.data.backup.models.BackupCategory
import com.shinku.reader.data.backup.models.BackupExtensionRepos
import com.shinku.reader.data.backup.models.BackupManga
import com.shinku.reader.data.backup.models.BackupPreference
import com.shinku.reader.data.backup.models.BackupSavedSearch
import com.shinku.reader.data.backup.models.BackupSource
import com.shinku.reader.data.backup.models.BackupSourcePreferences
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSource
import okio.buffer
import okio.gzip
import java.io.EOFException
import java.io.IOException

/**
 * Streaming protobuf wire-format reader for Mihon/ShinKu backups.
 *
 * Avoids allocating a full-file decompressed [ByteArray] or materializing the entire
 * [com.shinku.reader.data.backup.models.Backup] object graph at once, keeping peak heap
 * usage bounded even for multi-hundred-megabyte backups on 512 MiB heap devices.
 */
internal object BackupProtoStream {

    private const val MAGIC_GZIP = 0x1f8b
    private const val MAGIC_JSON_SIGNATURE1 = 0x7b7d // `{}`
    private const val MAGIC_JSON_SIGNATURE2 = 0x7b22 // `{"`
    private const val MAGIC_JSON_SIGNATURE3 = 0x7b0a // `{\n`

    // Backup field numbers from Backup.kt
    private const val FIELD_BACKUP_MANGA = 1
    private const val FIELD_BACKUP_CATEGORIES = 2
    private const val FIELD_BACKUP_SOURCES = 101
    private const val FIELD_BACKUP_PREFERENCES = 104
    private const val FIELD_BACKUP_SOURCE_PREFERENCES = 105
    private const val FIELD_BACKUP_EXTENSION_REPO = 106
    private const val FIELD_BACKUP_SAVED_SEARCHES = 600

    // BackupManga field numbers from BackupManga.kt
    private const val FIELD_MANGA_CHAPTERS = 16
    private const val FIELD_MANGA_TRACKING = 18
    private const val FIELD_MANGA_HISTORY = 104

    // BackupTracking field numbers from BackupTracking.kt
    private const val FIELD_TRACKING_SYNC_ID = 1

    private const val WIRE_VARINT = 0
    private const val WIRE_64BIT = 1
    private const val WIRE_LENGTH_DELIMITED = 2
    private const val WIRE_32BIT = 5

    // Guard against corrupt varints attempting multi-gigabyte single-message allocations.
    private const val MAX_SINGLE_MESSAGE_BYTES = 64L * 1024L * 1024L

    data class InspectionResult(
        val sources: List<BackupSource>,
        val trackerIds: Set<Int>,
        val summary: BackupContentsSummary,
    )

    data class MetadataResult(
        val mangaCount: Int,
        val backupCategories: List<BackupCategory>,
        val backupSources: List<BackupSource>,
        val backupPreferences: List<BackupPreference>,
        val backupSourcePreferences: List<BackupSourcePreferences>,
        val backupExtensionRepo: List<BackupExtensionRepos>,
        val backupSavedSearches: List<BackupSavedSearch>,
    )

    fun openPayloadSource(
        rawSource: BufferedSource,
        jsonErrorMessage: String,
        unknownErrorMessage: String,
    ): BufferedSource {
        val peeked = rawSource.peek()
        try {
            peeked.require(2)
        } catch (e: EOFException) {
            throw IOException(unknownErrorMessage, e)
        }
        return when (peeked.readShort().toInt() and 0xFFFF) {
            MAGIC_GZIP -> rawSource.gzip().buffer()
            MAGIC_JSON_SIGNATURE1, MAGIC_JSON_SIGNATURE2, MAGIC_JSON_SIGNATURE3 -> {
                throw IOException(jsonErrorMessage)
            }
            else -> rawSource
        }
    }

    fun inspect(
        source: BufferedSource,
        parser: ProtoBuf,
        unknownErrorMessage: String,
    ): InspectionResult {
        val sources = mutableListOf<BackupSource>()
        val trackerIds = linkedSetOf<Int>()
        var titles = 0
        var chapters = 0L
        var categories = 0
        var historyEntries = 0L
        var settings = 0

        readTopLevelFields(source, unknownErrorMessage) { fieldNumber, wireType ->
            when {
                fieldNumber == FIELD_BACKUP_MANGA && wireType == WIRE_LENGTH_DELIMITED -> {
                    titles++
                    val mangaLen = readBoundedLength(source, unknownErrorMessage)
                    val counts = scanMangaEntry(source, mangaLen, trackerIds)
                    chapters += counts.first
                    historyEntries += counts.second
                }
                fieldNumber == FIELD_BACKUP_CATEGORIES && wireType == WIRE_LENGTH_DELIMITED -> {
                    categories++
                    val len = readBoundedLength(source, unknownErrorMessage)
                    source.skip(len)
                }
                fieldNumber == FIELD_BACKUP_SOURCES && wireType == WIRE_LENGTH_DELIMITED -> {
                    val bytes = readMessageBytes(source, unknownErrorMessage)
                    sources.add(decodeMessage(parser, BackupSource.serializer(), bytes, unknownErrorMessage))
                }
                fieldNumber == FIELD_BACKUP_PREFERENCES && wireType == WIRE_LENGTH_DELIMITED -> {
                    settings++
                    val len = readBoundedLength(source, unknownErrorMessage)
                    source.skip(len)
                }
                else -> source.skipProtobufField(wireType)
            }
        }

        return InspectionResult(
            sources = sources,
            trackerIds = trackerIds,
            summary = BackupContentsSummary(
                titles = titles,
                chapters = chapters,
                categories = categories,
                historyEntries = historyEntries,
                settings = settings,
            ),
        )
    }

    fun decodeMetadata(
        source: BufferedSource,
        parser: ProtoBuf,
        unknownErrorMessage: String,
    ): MetadataResult {
        var mangaCount = 0
        val categories = mutableListOf<BackupCategory>()
        val sources = mutableListOf<BackupSource>()
        val preferences = mutableListOf<BackupPreference>()
        val sourcePreferences = mutableListOf<BackupSourcePreferences>()
        val extensionRepos = mutableListOf<BackupExtensionRepos>()
        val savedSearches = mutableListOf<BackupSavedSearch>()

        readTopLevelFields(source, unknownErrorMessage) { fieldNumber, wireType ->
            when {
                fieldNumber == FIELD_BACKUP_MANGA && wireType == WIRE_LENGTH_DELIMITED -> {
                    mangaCount++
                    val len = readBoundedLength(source, unknownErrorMessage)
                    source.skip(len)
                }
                fieldNumber == FIELD_BACKUP_CATEGORIES && wireType == WIRE_LENGTH_DELIMITED -> {
                    val bytes = readMessageBytes(source, unknownErrorMessage)
                    categories.add(decodeMessage(parser, BackupCategory.serializer(), bytes, unknownErrorMessage))
                }
                fieldNumber == FIELD_BACKUP_SOURCES && wireType == WIRE_LENGTH_DELIMITED -> {
                    val bytes = readMessageBytes(source, unknownErrorMessage)
                    sources.add(decodeMessage(parser, BackupSource.serializer(), bytes, unknownErrorMessage))
                }
                fieldNumber == FIELD_BACKUP_PREFERENCES && wireType == WIRE_LENGTH_DELIMITED -> {
                    val bytes = readMessageBytes(source, unknownErrorMessage)
                    preferences.add(decodeMessage(parser, BackupPreference.serializer(), bytes, unknownErrorMessage))
                }
                fieldNumber == FIELD_BACKUP_SOURCE_PREFERENCES && wireType == WIRE_LENGTH_DELIMITED -> {
                    val bytes = readMessageBytes(source, unknownErrorMessage)
                    sourcePreferences.add(
                        decodeMessage(parser, BackupSourcePreferences.serializer(), bytes, unknownErrorMessage),
                    )
                }
                fieldNumber == FIELD_BACKUP_EXTENSION_REPO && wireType == WIRE_LENGTH_DELIMITED -> {
                    val bytes = readMessageBytes(source, unknownErrorMessage)
                    extensionRepos.add(
                        decodeMessage(parser, BackupExtensionRepos.serializer(), bytes, unknownErrorMessage),
                    )
                }
                fieldNumber == FIELD_BACKUP_SAVED_SEARCHES && wireType == WIRE_LENGTH_DELIMITED -> {
                    val bytes = readMessageBytes(source, unknownErrorMessage)
                    savedSearches.add(decodeMessage(parser, BackupSavedSearch.serializer(), bytes, unknownErrorMessage))
                }
                else -> source.skipProtobufField(wireType)
            }
        }

        return MetadataResult(
            mangaCount = mangaCount,
            backupCategories = categories,
            backupSources = sources,
            backupPreferences = preferences,
            backupSourcePreferences = sourcePreferences,
            backupExtensionRepo = extensionRepos,
            backupSavedSearches = savedSearches,
        )
    }

    suspend fun forEachManga(
        source: BufferedSource,
        parser: ProtoBuf,
        unknownErrorMessage: String,
        onManga: suspend (BackupManga) -> Unit,
    ) {
        try {
            while (!source.exhausted()) {
                val tag = source.readVarint64()
                val fieldNumber = (tag ushr 3).toInt()
                val wireType = (tag and 0x07L).toInt()
                if (fieldNumber == FIELD_BACKUP_MANGA && wireType == WIRE_LENGTH_DELIMITED) {
                    val bytes = readMessageBytes(source, unknownErrorMessage)
                    val manga = decodeMessage(parser, BackupManga.serializer(), bytes, unknownErrorMessage)
                    onManga(manga)
                } else {
                    source.skipProtobufField(wireType)
                }
            }
        } catch (e: IOException) {
            throw e
        } catch (e: SerializationException) {
            throw IOException(unknownErrorMessage, e)
        }
    }

    private inline fun readTopLevelFields(
        source: BufferedSource,
        unknownErrorMessage: String,
        onField: (fieldNumber: Int, wireType: Int) -> Unit,
    ) {
        try {
            while (!source.exhausted()) {
                val tag = source.readVarint64()
                val fieldNumber = (tag ushr 3).toInt()
                val wireType = (tag and 0x07L).toInt()
                onField(fieldNumber, wireType)
            }
        } catch (e: EOFException) {
            throw IOException(unknownErrorMessage, e)
        } catch (e: SerializationException) {
            throw IOException(unknownErrorMessage, e)
        }
    }

    private fun scanMangaEntry(
        source: BufferedSource,
        mangaLen: Long,
        trackerIds: MutableSet<Int>,
    ): Pair<Long, Long> {
        var chapters = 0L
        var historyEntries = 0L
        var remaining = mangaLen
        while (remaining > 0L) {
            val (tag, tagBytes) = source.readVarintWithLength()
            remaining -= tagBytes
            val fieldNumber = (tag ushr 3).toInt()
            val wireType = (tag and 0x07L).toInt()
            when {
                fieldNumber == FIELD_MANGA_CHAPTERS && wireType == WIRE_LENGTH_DELIMITED -> {
                    chapters++
                    val (len, lenBytes) = source.readVarintWithLength()
                    if (len < 0L || len > remaining - lenBytes) {
                        throw IOException("Malformed chapter length in backup")
                    }
                    source.skip(len)
                    remaining -= lenBytes + len
                }
                fieldNumber == FIELD_MANGA_HISTORY && wireType == WIRE_LENGTH_DELIMITED -> {
                    historyEntries++
                    val (len, lenBytes) = source.readVarintWithLength()
                    if (len < 0L || len > remaining - lenBytes) {
                        throw IOException("Malformed history length in backup")
                    }
                    source.skip(len)
                    remaining -= lenBytes + len
                }
                fieldNumber == FIELD_MANGA_TRACKING && wireType == WIRE_LENGTH_DELIMITED -> {
                    val (len, lenBytes) = source.readVarintWithLength()
                    if (len < 0L || len > remaining - lenBytes) {
                        throw IOException("Malformed tracking length in backup")
                    }
                    scanTrackingSyncId(source, len)?.let { trackerIds.add(it) }
                    remaining -= lenBytes + len
                }
                else -> {
                    val skipped = source.skipProtobufFieldWithLength(wireType)
                    remaining -= skipped
                }
            }
        }
        if (remaining != 0L) {
            throw IOException("Truncated BackupManga message in backup")
        }
        return chapters to historyEntries
    }

    private fun scanTrackingSyncId(source: BufferedSource, trackingLen: Long): Int? {
        var remaining = trackingLen
        var syncId: Int? = null
        while (remaining > 0L) {
            val (tag, tagBytes) = source.readVarintWithLength()
            remaining -= tagBytes
            val fieldNumber = (tag ushr 3).toInt()
            val wireType = (tag and 0x07L).toInt()
            if (fieldNumber == FIELD_TRACKING_SYNC_ID && wireType == WIRE_VARINT) {
                val (value, valBytes) = source.readVarintWithLength()
                syncId = value.toInt()
                remaining -= valBytes
            } else {
                remaining -= source.skipProtobufFieldWithLength(wireType)
            }
        }
        if (remaining != 0L) {
            throw IOException("Truncated BackupTracking message in backup")
        }
        return syncId
    }

    private fun <T> decodeMessage(
        parser: ProtoBuf,
        deserializer: kotlinx.serialization.DeserializationStrategy<T>,
        bytes: ByteArray,
        unknownErrorMessage: String,
    ): T {
        return try {
            parser.decodeFromByteArray(deserializer, bytes)
        } catch (e: SerializationException) {
            throw IOException(unknownErrorMessage, e)
        }
    }

    private fun readMessageBytes(source: BufferedSource, unknownErrorMessage: String): ByteArray {
        val len = readBoundedLength(source, unknownErrorMessage)
        return source.readByteArray(len)
    }

    private fun readBoundedLength(source: BufferedSource, unknownErrorMessage: String): Long {
        val len = source.readVarint64()
        if (len < 0L || len > MAX_SINGLE_MESSAGE_BYTES) {
            throw IOException(unknownErrorMessage)
        }
        return len
    }

    private fun BufferedSource.readVarint64(): Long = readVarintWithLength().first

    private fun BufferedSource.readVarintWithLength(): Pair<Long, Int> {
        var shift = 0
        var result = 0L
        var bytesRead = 0
        while (shift < 64) {
            val b = readByte().toInt() and 0xFF
            bytesRead++
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) return result to bytesRead
            shift += 7
        }
        throw IOException("Malformed protobuf varint")
    }

    private fun BufferedSource.skipProtobufField(wireType: Int) {
        skipProtobufFieldWithLength(wireType)
    }

    private fun BufferedSource.skipProtobufFieldWithLength(wireType: Int): Long {
        return when (wireType) {
            WIRE_VARINT -> readVarintWithLength().second.toLong()
            WIRE_64BIT -> {
                skip(8L)
                8L
            }
            WIRE_LENGTH_DELIMITED -> {
                val (len, lenBytes) = readVarintWithLength()
                if (len < 0L) throw IOException("Negative protobuf field length")
                skip(len)
                lenBytes + len
            }
            WIRE_32BIT -> {
                skip(4L)
                4L
            }
            else -> throw IOException("Unsupported protobuf wire type: $wireType")
        }
    }
}
