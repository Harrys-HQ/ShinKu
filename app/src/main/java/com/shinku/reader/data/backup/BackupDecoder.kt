package com.shinku.reader.data.backup

import android.content.Context
import android.net.Uri
import com.shinku.reader.data.backup.models.Backup
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import okio.buffer
import okio.gzip
import okio.source
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.i18n.MR
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
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown))

        return try {
            inputStream.use { stream ->
                val source = stream.source().buffer()
                val signature = source.peek().use { peeked ->
                    if (!peeked.request(2)) {
                        throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown))
                    }
                    peeked.readShort().toInt()
                }

                val decodedSource = when (signature) {
                    GZIP_SIGNATURE -> source.gzip().buffer()
                    MAGIC_JSON_SIGNATURE1, MAGIC_JSON_SIGNATURE2, MAGIC_JSON_SIGNATURE3 -> {
                        throw IOException(context.stringResource(MR.strings.invalid_backup_file_json))
                    }
                    else -> source
                }

                decodedSource.use { BackupStreamDecoder(parser).decode(it) }
            }
        } catch (_: SerializationException) {
            throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown))
        } catch (e: BackupRecordTooLargeException) {
            throw IOException(context.stringResource(MR.strings.backup_file_record_too_large), e)
        }
    }

    companion object {
        private const val GZIP_SIGNATURE = 0x1f8b
        private const val MAGIC_JSON_SIGNATURE1 = 0x7b7d // `{}`
        private const val MAGIC_JSON_SIGNATURE2 = 0x7b22 // `{"`
        private const val MAGIC_JSON_SIGNATURE3 = 0x7b0a // `{\n`
    }
}
