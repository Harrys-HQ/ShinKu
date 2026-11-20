package com.shinku.reader.data.backup

import android.content.Context
import android.net.Uri
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.data.backup.models.Backup
import com.shinku.reader.data.backup.models.BackupManga
import com.shinku.reader.i18n.MR
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSource
import okio.buffer
import okio.gzip
import okio.source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException

class BackupDecoder(
    private val context: Context,
    private val parser: ProtoBuf = Injekt.get(),
) {
    /**
     * Decode a potentially-gzipped backup.
     */
    fun decode(uri: Uri): Backup {
        return context.contentResolver.openInputStream(uri)!!.use { inputStream ->
            val source = inputStream.source().buffer()

            val peeked = source.peek().apply {
                require(2)
            }
            val id1id2 = peeked.readShort()
            val backupString = when (id1id2.toInt()) {
                0x1f8b -> source.gzip().buffer() // 0x1f8b is gzip magic bytes
                MAGIC_JSON_SIGNATURE1, MAGIC_JSON_SIGNATURE2, MAGIC_JSON_SIGNATURE3 -> {
                    throw IOException(context.stringResource(MR.strings.invalid_backup_file_json))
                }
                else -> source
            }.use { it.readByteArray() }

            try {
                parser.decodeFromByteArray(Backup.serializer(), backupString)
            } catch (_: SerializationException) {
                throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown))
            }
        }
    }

    /**
     * Stream-inspect backup contents for validation without buffering the full payload in memory.
     */
    internal fun inspect(uri: Uri): BackupProtoStream.InspectionResult {
        return usePayloadSource(uri) { payloadSource ->
            BackupProtoStream.inspect(
                source = payloadSource,
                parser = parser,
                unknownErrorMessage = context.stringResource(MR.strings.invalid_backup_file_unknown),
            )
        }
    }

    /**
     * Stream-decode non-manga metadata and count manga entries without materializing [BackupManga]s.
     */
    internal fun decodeMetadata(uri: Uri): BackupProtoStream.MetadataResult {
        return usePayloadSource(uri) { payloadSource ->
            BackupProtoStream.decodeMetadata(
                source = payloadSource,
                parser = parser,
                unknownErrorMessage = context.stringResource(MR.strings.invalid_backup_file_unknown),
            )
        }
    }

    /**
     * Stream each [BackupManga] entry one at a time so peak heap stays bounded during restore.
     */
    internal suspend fun forEachManga(
        uri: Uri,
        onManga: suspend (BackupManga) -> Unit,
    ) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown))
        stream.use { inputStream ->
            val rawSource = inputStream.source().buffer()
            BackupProtoStream.openPayloadSource(
                rawSource = rawSource,
                jsonErrorMessage = context.stringResource(MR.strings.invalid_backup_file_json),
                unknownErrorMessage = context.stringResource(MR.strings.invalid_backup_file_unknown),
            ).use { payloadSource ->
                BackupProtoStream.forEachManga(
                    source = payloadSource,
                    parser = parser,
                    unknownErrorMessage = context.stringResource(MR.strings.invalid_backup_file_unknown),
                    onManga = onManga,
                )
            }
        }
    }

    private fun <T> usePayloadSource(
        uri: Uri,
        block: (BufferedSource) -> T,
    ): T {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown))
        return stream.use { inputStream ->
            val rawSource = inputStream.source().buffer()
            BackupProtoStream.openPayloadSource(
                rawSource = rawSource,
                jsonErrorMessage = context.stringResource(MR.strings.invalid_backup_file_json),
                unknownErrorMessage = context.stringResource(MR.strings.invalid_backup_file_unknown),
            ).use(block)
        }
    }

    companion object {
        private const val MAGIC_JSON_SIGNATURE1 = 0x7b7d // `{}`
        private const val MAGIC_JSON_SIGNATURE2 = 0x7b22 // `{"`
        private const val MAGIC_JSON_SIGNATURE3 = 0x7b0a // `{\n`
    }
}
