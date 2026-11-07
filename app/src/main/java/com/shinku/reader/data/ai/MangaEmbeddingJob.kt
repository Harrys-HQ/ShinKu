package com.shinku.reader.data.ai

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import com.shinku.reader.R
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.domain.ai.interactor.UpdateMangaEmbedding
import com.shinku.reader.domain.ai.model.MangaEmbedding
import com.shinku.reader.domain.ai.repository.AiRepository
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.util.system.notificationManager
import com.shinku.reader.util.system.workManager
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.concurrent.TimeUnit

class MangaEmbeddingJob(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val getLibraryManga: GetLibraryManga = Injekt.get()
    private val updateMangaEmbedding: UpdateMangaEmbedding = Injekt.get()
    private val aiRepository: AiRepository = Injekt.get()
    private val networkHelper: NetworkHelper = Injekt.get()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        workMutex.withLock {
            try {
                val versions = aiRepository.getEmbeddingVersions()
                val indexFile = File(context.filesDir, "embedding-inputs-v1.properties")
                val fingerprints = Properties().apply {
                    if (indexFile.exists()) runCatching { indexFile.inputStream().use(::load) }
                }
                val pending = getLibraryManga.await().map { it.manga }.filter { manga ->
                    val fingerprint = EmbeddingInputFingerprint.of(inputText(manga))
                    versions[manga.id] != manga.lastMetadataUpdate || fingerprints.getProperty(manga.id.toString()) != fingerprint
                }
                if (pending.isEmpty()) return@withLock Result.success()

                val modelFile = File(context.filesDir, "universal_sentence_encoder.tflite")
                if (!modelFile.exists()) downloadModel(modelFile)
                val textEmbedder = try {
                    createEmbedder(modelFile)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Older builds could leave a partial file at the final path.
                    downloadModel(modelFile)
                    createEmbedder(modelFile)
                }
                val total = pending.size
                val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_LIBRARY_PROGRESS)
                    .setSmallIcon(R.drawable.ic_tachi)
                    .setContentTitle("AI: Analyzing Library")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                try {
                    pending.forEachIndexed { index, manga ->
                        currentCoroutineContext().ensureActive()
                        context.notificationManager.notify(
                            Notifications.ID_LIBRARY_PROGRESS,
                            builder.setContentText(manga.title).setProgress(total, index, false).build(),
                        )
                        val text = inputText(manga)
                        val embedding = textEmbedder.embed(text).embeddingResult().embeddings().first().floatEmbedding()
                        currentCoroutineContext().ensureActive()
                        updateMangaEmbedding.await(MangaEmbedding(manga.id, embedding, manga.lastMetadataUpdate))
                        fingerprints.setProperty(manga.id.toString(), EmbeddingInputFingerprint.of(text))
                    }
                    // Commit only after database writes. A crash can cause safe rework, never a false skip.
                    val temporary = File(indexFile.path + ".tmp")
                    try {
                        temporary.outputStream().use { output ->
                            fingerprints.store(output, "Model and input fingerprints; safe to regenerate")
                            output.fd.sync()
                        }
                        Files.move(temporary.toPath(), indexFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    } finally {
                        temporary.delete()
                    }
                } finally {
                    textEmbedder.close()
                    context.notificationManager.cancel(Notifications.ID_LIBRARY_PROGRESS)
                }
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to run MangaEmbeddingJob" }
                Result.retry()
            }
        }
    }

    private fun inputText(manga: com.shinku.reader.domain.manga.model.Manga) =
        "${manga.title}. ${manga.description.orEmpty()} ${manga.genre?.joinToString(" ").orEmpty()}"

    private fun createEmbedder(file: File): TextEmbedder = TextEmbedder.createFromOptions(
        context,
        TextEmbedderOptions.builder()
            .setBaseOptions(
                com.google.mediapipe.tasks.core.BaseOptions.builder().setModelAssetPath(file.absolutePath).build(),
            )
            .build(),
    )

    private suspend fun downloadModel(target: File) {
        ModelFilePublisher.publish(target, download = { temporary ->
            val request = Request.Builder()
                .url("https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/1/universal_sentence_encoder.tflite")
                .build()
            networkHelper.client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Failed to download model: ${response.code}" }
                val expected = response.body.contentLength()
                response.body.byteStream().use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val size = input.read(buffer)
                            if (size < 0) break
                            output.write(buffer, 0, size)
                        }
                        output.fd.sync()
                    }
                }
                check(temporary.length() > 0 && (expected < 0 || temporary.length() == expected)) { "Incomplete model download" }
            }
        }, validate = { temporary ->
            // Validate with the actual native loader before publishing the staged model.
            createEmbedder(temporary).close()
        })
    }

    companion object {
        private const val TAG = "MangaEmbeddingJob"
        private val workMutex = Mutex()

        fun setupTask(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    .setRequiresDeviceIdle(true)
                    .build()

                val request = PeriodicWorkRequestBuilder<MangaEmbeddingJob>(
                    7, TimeUnit.DAYS,
                    1, TimeUnit.HOURS,
                )
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .setInitialDelay(7, TimeUnit.DAYS)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(
                    TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            } catch (e: Exception) {
                logcat(LogPriority.WARN) { "Failed to setup MangaEmbeddingJob: ${e.message}" }
            }
        }

        fun startNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<MangaEmbeddingJob>()
                .addTag(TAG + "_manual")
                .build()

            context.workManager.enqueueUniqueWork(
                TAG + "_manual",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
