package com.shinku.reader.data.backup

import com.shinku.reader.data.backup.models.Backup
import com.shinku.reader.data.backup.models.BackupCategory
import com.shinku.reader.data.backup.models.BackupExtensionRepos
import com.shinku.reader.data.backup.models.BackupManga
import com.shinku.reader.data.backup.models.BackupPreference
import com.shinku.reader.data.backup.models.BackupSavedSearch
import com.shinku.reader.data.backup.models.BackupSource
import com.shinku.reader.data.backup.models.BackupSourcePreferences
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSource
import java.io.IOException

/**
 * Decodes the top-level Backup message one length-delimited item at a time.
 *
 * kotlinx.serialization's ProtoBuf API accepts a ByteArray for the complete message. Mihon backups
 * store the large library as repeated top-level BackupManga messages, so decoding those records
 * individually avoids retaining a second, complete decompressed copy of the backup while the
 * Backup object is being built.
 */
internal class BackupStreamDecoder(
    private val parser: ProtoBuf,
    private val maxRecordBytes: Long = defaultMaxRecordBytes(),
) {
    init {
        require(maxRecordBytes > 0) { "maxRecordBytes must be positive" }
    }

    fun decode(source: BufferedSource): Backup {
        val manga = ArrayList<BackupManga>()
        val categories = ArrayList<BackupCategory>()
        val sources = ArrayList<BackupSource>()
        val preferences = ArrayList<BackupPreference>()
        val sourcePreferences = ArrayList<BackupSourcePreferences>()
        val extensionRepos = ArrayList<BackupExtensionRepos>()
        val savedSearches = ArrayList<BackupSavedSearch>()

        while (!source.exhausted()) {
            val tag = readVarint(source)
            if (tag <= 0L) throw IOException("Invalid protobuf tag")

            val fieldNumber = tag ushr 3
            val wireType = (tag and WIRE_TYPE_MASK).toInt()
            if (fieldNumber == 0L || fieldNumber > MAX_FIELD_NUMBER) throw IOException("Invalid protobuf field number")

            when (fieldNumber) {
                BACKUP_MANGA_FIELD -> manga += readMessage(source, wireType, BackupManga.serializer())
                BACKUP_CATEGORIES_FIELD -> categories += readMessage(source, wireType, BackupCategory.serializer())
                BACKUP_SOURCES_FIELD -> sources += readMessage(source, wireType, BackupSource.serializer())
                BACKUP_PREFERENCES_FIELD -> preferences += readMessage(source, wireType, BackupPreference.serializer())
                BACKUP_SOURCE_PREFERENCES_FIELD -> sourcePreferences += readMessage(
                    source,
                    wireType,
                    BackupSourcePreferences.serializer(),
                )
                BACKUP_EXTENSION_REPOS_FIELD -> extensionRepos += readMessage(
                    source,
                    wireType,
                    BackupExtensionRepos.serializer(),
                )
                BACKUP_SAVED_SEARCHES_FIELD -> savedSearches += readMessage(
                    source,
                    wireType,
                    BackupSavedSearch.serializer(),
                )
                else -> skipField(source, fieldNumber, wireType)
            }
        }

        return Backup(
            backupManga = manga,
            backupCategories = categories,
            backupSources = sources,
            backupPreferences = preferences,
            backupSourcePreferences = sourcePreferences,
            backupExtensionRepo = extensionRepos,
            backupSavedSearches = savedSearches,
        )
    }

    private fun <T> readMessage(
        source: BufferedSource,
        wireType: Int,
        deserializer: DeserializationStrategy<T>,
    ): T {
        if (wireType != WIRE_TYPE_LENGTH_DELIMITED) {
            throw IOException("Invalid wire type for backup message")
        }

        val length = readLength(source)
        if (length > maxRecordBytes) throw BackupRecordTooLargeException(length, maxRecordBytes)

        val bytes = source.readByteArray(length)
        return parser.decodeFromByteArray(deserializer, bytes)
    }

    private fun skipField(source: BufferedSource, fieldNumber: Long, wireType: Int) {
        when (wireType) {
            WIRE_TYPE_VARINT -> readVarint(source)
            WIRE_TYPE_FIXED64 -> source.skip(FIXED64_BYTES)
            WIRE_TYPE_LENGTH_DELIMITED -> source.skip(readLength(source))
            WIRE_TYPE_START_GROUP -> skipGroup(source, fieldNumber)
            WIRE_TYPE_END_GROUP -> throw IOException("Unexpected protobuf end-group tag")
            WIRE_TYPE_FIXED32 -> source.skip(FIXED32_BYTES)
            else -> throw IOException("Invalid protobuf wire type")
        }
    }

    private fun skipGroup(source: BufferedSource, openingFieldNumber: Long) {
        // Groups are deprecated but unknown fields may still use them. Bound nesting so a hostile
        // input cannot exhaust the call stack; retain field numbers to verify matching end tags.
        val groups = ArrayList<Long>()
        groups.add(openingFieldNumber)
        while (groups.isNotEmpty()) {
            val tag = readVarint(source)
            if (tag <= 0L) throw IOException("Invalid protobuf tag in group")

            val fieldNumber = tag ushr 3
            val wireType = (tag and WIRE_TYPE_MASK).toInt()
            if (fieldNumber == 0L || fieldNumber > MAX_FIELD_NUMBER) {
                throw IOException("Invalid protobuf field number in group")
            }

            when (wireType) {
                WIRE_TYPE_END_GROUP -> {
                    if (fieldNumber != groups.last()) throw IOException("Mismatched protobuf end-group tag")
                    groups.removeAt(groups.lastIndex)
                }
                WIRE_TYPE_START_GROUP -> {
                    if (groups.size >= MAX_GROUP_DEPTH) throw IOException("Protobuf group nesting limit exceeded")
                    groups.add(fieldNumber)
                }
                else -> skipField(source, fieldNumber, wireType)
            }
        }
    }

    private fun readLength(source: BufferedSource): Long {
        val length = readVarint(source)
        if (length < 0L) throw IOException("Invalid protobuf length")
        return length
    }

    private fun readVarint(source: BufferedSource): Long {
        var result = 0L
        var shift = 0

        repeat(MAX_VARINT_BYTES) {
            val byte = source.readByte().toInt() and 0xff
            if (shift == MAX_VARINT_SHIFT && byte > 1) throw IOException("Malformed protobuf varint")

            result = result or ((byte and VARINT_VALUE_MASK).toLong() shl shift)
            if ((byte and VARINT_CONTINUATION_MASK) == 0) return result
            shift += VARINT_SHIFT_BITS
        }

        throw IOException("Malformed protobuf varint")
    }

    companion object {
        private fun defaultMaxRecordBytes(): Long =
            (Runtime.getRuntime().maxMemory() / MAX_HEAP_DIVISOR).coerceAtMost(MAX_RECORD_BYTES)
                .coerceAtLeast(MIN_RECORD_BYTES)

        private const val BACKUP_MANGA_FIELD = 1L
        private const val BACKUP_CATEGORIES_FIELD = 2L
        private const val BACKUP_SOURCES_FIELD = 101L
        private const val BACKUP_PREFERENCES_FIELD = 104L
        private const val BACKUP_SOURCE_PREFERENCES_FIELD = 105L
        private const val BACKUP_EXTENSION_REPOS_FIELD = 106L
        private const val BACKUP_SAVED_SEARCHES_FIELD = 600L

        private const val WIRE_TYPE_VARINT = 0
        private const val WIRE_TYPE_FIXED64 = 1
        private const val WIRE_TYPE_LENGTH_DELIMITED = 2
        private const val WIRE_TYPE_START_GROUP = 3
        private const val WIRE_TYPE_END_GROUP = 4
        private const val WIRE_TYPE_FIXED32 = 5

        private const val WIRE_TYPE_MASK = 0x7L
        private const val VARINT_VALUE_MASK = 0x7f
        private const val VARINT_CONTINUATION_MASK = 0x80
        private const val VARINT_SHIFT_BITS = 7
        private const val MAX_VARINT_BYTES = 10
        private const val MAX_VARINT_SHIFT = 63
        private const val MAX_FIELD_NUMBER = (1L shl 29) - 1
        private const val MAX_GROUP_DEPTH = 64
        private const val FIXED64_BYTES = 8L
        private const val FIXED32_BYTES = 4L

        // A single record larger than this can expand substantially while deserializing. The cap
        // is per record, not per file, so large libraries containing many normal records continue
        // to decode without an arbitrary whole-backup size limit.
        private const val MAX_HEAP_DIVISOR = 16L
        private const val MIN_RECORD_BYTES = 4L * 1024 * 1024
        private const val MAX_RECORD_BYTES = 32L * 1024 * 1024
    }
}

internal class BackupRecordTooLargeException(
    val recordBytes: Long,
    val allowedBytes: Long,
) : IOException()
