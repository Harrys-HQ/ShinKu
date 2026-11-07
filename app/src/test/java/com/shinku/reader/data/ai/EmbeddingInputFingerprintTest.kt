package com.shinku.reader.data.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class EmbeddingInputFingerprintTest {
    @Test
    fun `unchanged text remains stable while metadata and model changes invalidate it`() {
        val first = EmbeddingInputFingerprint.of("Title. Description Action")
        assertEquals(first, EmbeddingInputFingerprint.of("Title. Description Action"))
        assertNotEquals(first, EmbeddingInputFingerprint.of("Title. Edited description Action"))
        assertNotEquals(first, EmbeddingInputFingerprint.of("Title. Description Action", "next-model"))
    }
}
