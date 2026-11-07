package com.shinku.reader.data.ai

import java.security.MessageDigest

internal object EmbeddingInputFingerprint {
    // Bump when the pinned model or input construction changes. No credentials enter this hash.
    private const val MODEL_INPUT_VERSION = "use-float32-1/text-v1"

    fun of(text: String, version: String = MODEL_INPUT_VERSION): String =
        MessageDigest.getInstance("SHA-256").digest("$version\u0000$text".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
