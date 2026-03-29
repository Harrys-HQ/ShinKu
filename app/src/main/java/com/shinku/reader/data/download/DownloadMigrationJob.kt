package com.shinku.reader.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.hippo.unifile.UniFile
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.storage.service.StorageManager
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import com.shinku.reader.util.storage.DiskUtil

class DownloadMigrationJob(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val storageManager: StorageManager = Injekt.get()
    private val downloadCache: DownloadCache = Injekt.get()
    private val notifier = DownloadMigrationNotifier(context)

    override suspend fun doWork(): Result {
        logcat(LogPriority.INFO) { "Starting download migration" }
        notifier.showIndeterminateProgress()
        
        try {
            val baseDir = storageManager.getDownloadsDirectory()?.parentFile ?: throw Exception("Could not find base downloads directory")
            val targetDownloadsDir = storageManager.getDownloadsDirectory() ?: throw Exception("Could not find target downloads directory")
            
            val legacyFolders = listOf("Mihon", "Tachiyomi")
            var migratedCount = 0

            legacyFolders.forEach { folderName ->
                val legacyRoot = baseDir.findFile(folderName)
                if (legacyRoot != null && legacyRoot.isDirectory) {
                    // Check for 'downloads' inside legacy root
                    val legacyDownloads = legacyRoot.findFile("downloads") ?: legacyRoot
                    
                    if (legacyDownloads.isDirectory) {
                        legacyDownloads.listFiles().orEmpty().forEach { sourceDir ->
                            if (sourceDir.isDirectory) {
                                val targetSourceDir = targetDownloadsDir.createDirectory(sourceDir.name ?: "")
                                if (targetSourceDir != null) {
                                    sourceDir.listFiles().orEmpty().forEach { mangaDir ->
                                        if (mangaDir.isDirectory) {
                                            val targetMangaDir = targetSourceDir.createDirectory(mangaDir.name ?: "")
                                            if (targetMangaDir != null) {
                                                // Move chapters
                                                mangaDir.listFiles().orEmpty().forEach { chapterFile ->
                                                    moveUniFile(chapterFile, targetMangaDir)
                                                    migratedCount++
                                                    if (migratedCount % 5 == 0) {
                                                        notifier.updateProgress(migratedCount)
                                                    }
                                                }
                                                // Delete empty manga dir
                                                mangaDir.delete()
                                            }
                                        }
                                    }
                                    // Delete empty source dir
                                    sourceDir.delete()
                                }
                            }
                        }
                    }
                }
            }

            if (migratedCount > 0) {
                logcat(LogPriority.INFO) { "Migrated $migratedCount chapters" }
                downloadCache.invalidateCache()
                notifier.showComplete(migratedCount)
            } else {
                notifier.dismissProgress()
            }

            return Result.success()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Download migration failed" }
            notifier.showError(e.message ?: "Unknown error")
            return Result.failure()
        }
    }

    private fun moveUniFile(file: UniFile, targetDir: UniFile) {
        val targetFile = targetDir.createFile(file.name ?: return) ?: return
        
        try {
            file.openInputStream().use { input ->
                targetFile.openOutputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.delete()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to move file ${file.name}" }
            targetFile.delete()
        }
    }

    companion object {
        private const val TAG = "DownloadMigration"

        fun startNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DownloadMigrationJob>()
                .addTag(TAG)
                .build()
            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }
    }
}
