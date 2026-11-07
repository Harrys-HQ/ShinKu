package com.shinku.reader.data.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

class ModelFilePublisherTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `interrupted download preserves previous model and retry publishes validated replacement`() = runTest {
        val target = File(directory, "model.tflite").apply { writeText("previous") }
        var validations = 0
        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking {
                ModelFilePublisher.publish(target, { staging ->
                    staging.writeText("partial")
                    throw IOException("interrupted")
                }, { validations++ })
            }
        }
        assertEquals(0, validations)
        assertEquals("previous", target.readText())
        assertFalse(File(target.path + ".download").exists())
        ModelFilePublisher.publish(target, { it.writeText("complete") }, {
            assertEquals("previous", target.readText())
            assertEquals("complete", it.readText())
            validations++
        })
        assertEquals(1, validations)
        assertEquals("complete", target.readText())
        assertFalse(File(target.path + ".download").exists())
    }

    @Test
    fun `validation failure does not publish a corrupt model`() = runTest {
        val target = File(directory, "model.tflite")
        try {
            ModelFilePublisher.publish(target, { it.writeText("invalid") }, { throw IOException("invalid model") })
            throw AssertionError("Expected validation failure")
        } catch (_: IOException) {
            assertFalse(target.exists())
            assertFalse(File(target.path + ".download").exists())
        }
    }

    @Test
    fun `cancellation during validation keeps the published model intact`() = runTest {
        val target = File(directory, "model.tflite").apply { writeText("previous") }
        try {
            ModelFilePublisher.publish(target, { it.writeText("complete") }, { throw CancellationException("stopped") })
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) {
            assertEquals("previous", target.readText())
            assertFalse(File(target.path + ".download").exists())
        }
    }
}
