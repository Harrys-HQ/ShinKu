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
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.util.system.notificationManager
import com.shinku.reader.util.system.workManager
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.TimeUnit

class MangaEmbeddingJob(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val getLibraryManga: GetLibraryManga = Injekt.get()
    private val updateMangaEmbedding: UpdateMangaEmbedding = Injekt.get()
    private val networkHelper: NetworkHelper = Injekt.get()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, "universal_sentence_encoder.tflite")
            if (!modelFile.exists()) {
                downloadModel(modelFile)
            }

            val options = TextEmbedderOptions.builder()
                .setBaseOptions(
                    com.google.mediapipe.tasks.core.BaseOptions.builder()
                        .setModelAssetPath(modelFile.absolutePath)
                        .build()
                )
                .build()

            val textEmbedder = TextEmbedder.createFromOptions(context, options)
            val libraryManga = getLibraryManga.await()
            val total = libraryManga.size

            val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_LIBRARY_PROGRESS)
                .setSmallIcon(R.drawable.ic_tachi)
                .setContentTitle("AI: Analyzing Library")
                .setOngoing(true)
                .setOnlyAlertOnce(true)

            libraryManga.forEachIndexed { index, libraryManga ->
                val manga = libraryManga.manga
                builder.setContentText(manga.title)
                    .setProgress(total, index, false)
                context.notificationManager.notify(Notifications.ID_LIBRARY_PROGRESS, builder.build())

                val textToEmbed = "${manga.title}. ${manga.description ?: ""} ${manga.genre?.joinToString(" ") ?: ""}"
                if (textToEmbed.isNotBlank()) {
                    val result = textEmbedder.embed(textToEmbed)
                    val embedding = result.embeddingResult().embeddings().first().floatEmbedding()
                    
                    updateMangaEmbedding.await(
                        MangaEmbedding(
                            mangaId = manga.id,
                            embedding = embedding,
                            lastMetadataUpdate = manga.lastMetadataUpdate
                        )
                    )
                }
            }

            textEmbedder.close()
            context.notificationManager.notify(
                Notifications.ID_LIBRARY_PROGRESS,
                builder.setContentTitle("AI: Library Analysis Complete")
                    .setContentText("Found ${total} vibes")
                    .setOngoing(false)
                    .setProgress(0, 0, false)
                    .build()
            )
            Result.success()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to run MangaEmbeddingJob" }
            Result.retry()
        }
    }

    private fun downloadModel(target: File) {
        logcat { "Downloading MediaPipe model..." }
        val request = Request.Builder()
            .url("https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/1/universal_sentence_encoder.tflite")
            .build()

        networkHelper.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download model: ${response.code}")
            response.body.source().use { input ->
                target.outputStream().use { output ->
                    input.inputStream().copyTo(output)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MangaEmbeddingJob"

        fun setupTask(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build()

            val request = PeriodicWorkRequestBuilder<MangaEmbeddingJob>(
                7, TimeUnit.DAYS,
                1, TimeUnit.HOURS,
            )
                .addTag(TAG)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            context.workManager.enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
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
