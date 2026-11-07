package com.shinku.reader.data.ai

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Serial callers own the target. A failed download or validation never replaces it. */
internal object ModelFilePublisher {
    suspend fun publish(
        target: File,
        download: suspend (File) -> Unit,
        validate: suspend (File) -> Unit,
    ) {
        val temporary = File(target.path + ".download")
        try {
            download(temporary)
            currentCoroutineContext().ensureActive()
            validate(temporary)
            currentCoroutineContext().ensureActive()
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }
}
