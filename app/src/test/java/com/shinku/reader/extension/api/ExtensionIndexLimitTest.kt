package com.shinku.reader.extension.api

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ExtensionIndexLimitTest {
    @Test
    fun `plain and gzip index payloads return the same bytes`() {
        val payload = byteArrayOf(0x0a, 0x03, 0x61, 0x62, 0x63)
        val compressed = Buffer()
        GzipSink(compressed).buffer().use { it.write(payload) }

        assertArrayEquals(payload, response(payload).readProtobufIndexBytes())
        assertArrayEquals(payload, response(compressed.readByteArray()).readProtobufIndexBytes())
    }

    @Test
    fun `index size is enforced for raw and decoded payloads`() {
        val bytes = ByteArray(MAX_EXTENSION_INDEX_BYTES.toInt() + 1)
        val compressed = Buffer()
        GzipSink(compressed).buffer().use { it.write(bytes) }

        assertThrows(ExtensionIndexTooLargeException::class.java) { response(bytes).readProtobufIndexBytes() }
        assertThrows(ExtensionIndexTooLargeException::class.java) {
            response(compressed.readByteArray()).readProtobufIndexBytes()
        }
    }

    private fun response(bytes: ByteArray) = Response.Builder()
        .request(Request.Builder().url("https://example.test/index.pb").build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(bytes.toResponseBody())
        .build()
}
