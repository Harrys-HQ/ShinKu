package com.shinku.reader.data.backup.restore

import android.content.Context
import android.net.Uri
import com.shinku.reader.data.backup.BackupDecoder
import com.shinku.reader.data.backup.BackupNotifier
import com.shinku.reader.data.backup.models.BackupCategory
import com.shinku.reader.data.backup.models.BackupExtensionRepos
import com.shinku.reader.data.backup.models.BackupManga
import com.shinku.reader.data.backup.models.BackupPreference
import com.shinku.reader.data.backup.models.BackupSavedSearch
import com.shinku.reader.data.backup.models.BackupSourcePreferences
import com.shinku.reader.data.backup.restore.restorers.CategoriesRestorer
import com.shinku.reader.data.backup.restore.restorers.ExtensionRepoRestorer
import com.shinku.reader.data.backup.restore.restorers.MangaRestorer
import com.shinku.reader.data.backup.restore.restorers.PreferenceRestorer
import com.shinku.reader.data.backup.restore.restorers.SavedSearchRestorer
import com.shinku.reader.util.system.createFileInCacheDir
import com.shinku.reader.exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestorer(
    private val context: Context,
    private val notifier: BackupNotifier,
    private val isSync: Boolean,

    private val categoriesRestorer: CategoriesRestorer = CategoriesRestorer(),
    private val preferenceRestorer: PreferenceRestorer = PreferenceRestorer(context),
    private val extensionRepoRestorer: ExtensionRepoRestorer = ExtensionRepoRestorer(),
    private val mangaRestorer: MangaRestorer = MangaRestorer(isSync),
    // SY -->
    private val savedSearchRestorer: SavedSearchRestorer = SavedSearchRestorer(),
    // SY <--
) {

    private var restoreAmount = 0
    private var restoreProgress = 0
    private val errors = mutableListOf<Pair<Date, String>>()

    /**
     * Mapping of source ID to source name from backup data
     */
    private var sourceMapping: Map<Long, String> = emptyMap()

    suspend fun restore(uri: Uri, options: RestoreOptions) {
        val startTime = System.currentTimeMillis()

        restoreFromFile(uri, options)

        val time = System.currentTimeMillis() - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(
            time,
            errors.size,
            logFile.parent,
            logFile.name,
            isSync,
        )
    }

    private suspend fun restoreFromFile(uri: Uri, options: RestoreOptions) {
        val decoder = BackupDecoder(context)
        val metadata = decoder.decodeMetadata(uri)

        // Store source mapping for error messages
        sourceMapping = metadata.backupSources.associate { it.sourceId to it.name }

        if (options.libraryEntries) {
            restoreAmount += metadata.mangaCount
        }
        if (options.categories) {
            restoreAmount += 1
        }
        // SY -->
        if (options.savedSearches) {
            restoreAmount += 1
        }
        // SY <--
        if (options.appSettings) {
            restoreAmount += 1
        }
        if (options.extensionRepoSettings) {
            restoreAmount += metadata.backupExtensionRepo.size
        }
        if (options.sourceSettings) {
            restoreAmount += 1
        }

        coroutineScope {
            val restoreCategoriesJob = if (options.categories) {
                restoreCategories(metadata.backupCategories)
            } else {
                null
            }
            // SY -->
            if (options.savedSearches) {
                restoreSavedSearches(metadata.backupSavedSearches)
            }
            // SY <--
            if (options.appSettings) {
                restoreAppPreferences(
                    metadata.backupPreferences,
                    metadata.backupCategories.takeIf { options.categories },
                    restoreCategoriesJob,
                )
            }
            if (options.sourceSettings) {
                restoreSourcePreferences(metadata.backupSourcePreferences)
            }
            if (options.libraryEntries) {
                restoreManga(
                    decoder,
                    uri,
                    if (options.categories) metadata.backupCategories else emptyList(),
                    restoreCategoriesJob,
                )
            }
            if (options.extensionRepoSettings) {
                restoreExtensionRepos(metadata.backupExtensionRepo)
            }

            // TODO: optionally trigger online library + tracker update
        }
    }

    private fun CoroutineScope.restoreCategories(backupCategories: List<BackupCategory>) = launch {
        ensureActive()
        categoriesRestorer(backupCategories)

        restoreProgress += 1
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.categories),
            restoreProgress,
            restoreAmount,
            isSync,
        )
    }

    // SY -->
    private fun CoroutineScope.restoreSavedSearches(backupSavedSearches: List<BackupSavedSearch>) = launch {
        ensureActive()
        savedSearchRestorer.restoreSavedSearches(backupSavedSearches)

        restoreProgress += 1
        notifier.showRestoreProgress(
            context.stringResource(SYMR.strings.saved_searches),
            restoreProgress,
            restoreAmount,
            isSync,
        )
    }
    // SY <--

    private fun CoroutineScope.restoreManga(
        decoder: BackupDecoder,
        uri: Uri,
        backupCategories: List<BackupCategory>,
        restoreCategoriesJob: Job?,
    ) = launch {
        restoreCategoriesJob?.join()
        val deferredMergedMangas = mutableListOf<BackupManga>()
        decoder.forEachManga(uri) { manga ->
            ensureActive()
            if (manga.source == MERGED_SOURCE_ID) {
                deferredMergedMangas.add(manga)
            } else {
                restoreSingleManga(manga, backupCategories)
            }
        }
        deferredMergedMangas.forEach { manga ->
            ensureActive()
            restoreSingleManga(manga, backupCategories)
        }
    }

    private suspend fun restoreSingleManga(
        manga: BackupManga,
        backupCategories: List<BackupCategory>,
    ) {
        try {
            mangaRestorer.restore(manga, backupCategories)
        } catch (e: Exception) {
            val sourceName = sourceMapping[manga.source] ?: manga.source.toString()
            errors.add(Date() to "${manga.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1
        notifier.showRestoreProgress(manga.title, restoreProgress, restoreAmount, isSync)
    }

    private fun CoroutineScope.restoreAppPreferences(
        preferences: List<BackupPreference>,
        categories: List<BackupCategory>?,
        restoreCategoriesJob: Job?,
    ) = launch {
        restoreCategoriesJob?.join()
        ensureActive()
        preferenceRestorer.restoreApp(
            preferences,
            categories,
        )

        restoreProgress += 1
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.app_settings),
            restoreProgress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreSourcePreferences(preferences: List<BackupSourcePreferences>) = launch {
        ensureActive()
        preferenceRestorer.restoreSource(preferences)

        restoreProgress += 1
        notifier.showRestoreProgress(
            context.stringResource(MR.strings.source_settings),
            restoreProgress,
            restoreAmount,
            isSync,
        )
    }

    private fun CoroutineScope.restoreExtensionRepos(
        backupExtensionRepo: List<BackupExtensionRepos>,
    ) = launch {
        backupExtensionRepo
            .forEach {
                ensureActive()

                try {
                    extensionRepoRestorer(it)
                } catch (e: Exception) {
                    errors.add(Date() to "Error Adding Repo: ${it.name} : ${e.message}")
                }

                restoreProgress += 1
                notifier.showRestoreProgress(
                    context.stringResource(MR.strings.extensionRepo_settings),
                    restoreProgress,
                    restoreAmount,
                    isSync,
                )
            }
    }

    private fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("shinku_restore_error.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                file.bufferedWriter().use { out ->
                    errors.forEach { (date, message) ->
                        out.write("[${sdf.format(date)}] $message\n")
                    }
                }
                return file
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }
}
