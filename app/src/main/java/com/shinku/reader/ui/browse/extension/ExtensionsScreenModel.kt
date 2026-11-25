package com.shinku.reader.ui.browse.extension

import android.app.Application
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.extension.interactor.GetExtensionsByType
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.presentation.components.SEARCH_DEBOUNCE_MILLIS
import com.shinku.reader.extension.ExtensionManager
import com.shinku.reader.extension.model.Extension
import com.shinku.reader.extension.model.InstallStep
import eu.kanade.tachiyomi.source.online.HttpSource
import com.shinku.reader.util.system.LocaleHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class ExtensionsScreenModel(
    preferences: SourcePreferences = Injekt.get(),
    basePreferences: BasePreferences = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val getExtensions: GetExtensionsByType = Injekt.get(),
) : StateScreenModel<ExtensionsScreenModel.State>(State()) {

    private val currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())
    private var batchJob: Job? = null

    init {
        val context = Injekt.get<Application>()
        val extensionMapper: (Map<String, InstallStep>) -> ((Extension) -> ExtensionUiModel.Item) = { map ->
            {
                ExtensionUiModel.Item(it, map[it.installPackageName] ?: InstallStep.Idle)
            }
        }
        val queryFilter: (String) -> ((Extension) -> Boolean) = { query ->
            filter@{ extension ->
                if (query.isEmpty()) return@filter true
                query.split(",").any { _input ->
                    val input = _input.trim()
                    if (input.isEmpty()) return@any false
                    when (extension) {
                        is Extension.Available -> {
                            extension.sources.any {
                                it.name.contains(input, ignoreCase = true) ||
                                    it.baseUrl.contains(input, ignoreCase = true) ||
                                    it.id == input.toLongOrNull()
                            } ||
                                extension.name.contains(input, ignoreCase = true)
                        }
                        is Extension.Installed -> {
                            extension.sources.any {
                                it.name.contains(input, ignoreCase = true) ||
                                    it.id == input.toLongOrNull() ||
                                    if (it is HttpSource) {
                                        it.baseUrl.contains(input, ignoreCase = true)
                                    } else {
                                        false
                                    }
                            } ||
                                extension.name.contains(input, ignoreCase = true)
                        }
                        is Extension.Untrusted -> extension.name.contains(input, ignoreCase = true)
                    }
                }
            }
        }

        screenModelScope.launchIO {
            combine(
                state.map { it.searchQuery }.distinctUntilChanged().debounce(SEARCH_DEBOUNCE_MILLIS),
                currentDownloads,
                getExtensions.subscribe(),
            ) { query, downloads, (_updates, _installed, _available, _untrusted) ->
                val searchQuery = query ?: ""

                val itemsGroups: ItemGroups = mutableMapOf()

                val updates = _updates.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                if (updates.isNotEmpty()) {
                    itemsGroups[ExtensionUiModel.Header.Resource(MR.strings.ext_updates_pending)] = updates
                }

                val installed = _installed.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                val untrusted = _untrusted.filter(queryFilter(searchQuery)).map(extensionMapper(downloads))
                if (installed.isNotEmpty() || untrusted.isNotEmpty()) {
                    itemsGroups[ExtensionUiModel.Header.Resource(MR.strings.ext_installed)] = installed + untrusted
                }

                val languagesWithExtensions = _available
                    .filter(queryFilter(searchQuery))
                    .groupBy { it.lang }
                    .toSortedMap(LocaleHelper.comparator)
                    .map { (lang, exts) ->
                        ExtensionUiModel.Header.Text(LocaleHelper.getSourceDisplayName(lang, context)) to
                            exts.map(extensionMapper(downloads))
                    }
                if (languagesWithExtensions.isNotEmpty()) {
                    itemsGroups.putAll(languagesWithExtensions)
                }

                itemsGroups
            }
                .collectLatest {
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            items = it,
                        )
                    }
                }
        }

        screenModelScope.launchIO { findAvailableExtensions() }

        preferences.extensionUpdatesCount().changes()
            .onEach { mutableState.update { state -> state.copy(updates = it) } }
            .launchIn(screenModelScope)

        basePreferences.extensionInstaller().changes()
            .onEach { mutableState.update { state -> state.copy(installer = it) } }
            .launchIn(screenModelScope)
    }

    fun search(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    fun updateAllExtensions() {
        val updates = state.value.items.values.flatten().map { it.extension }
            .filterIsInstance<Extension.Installed>().filter { it.hasUpdate }
        runBatch(updates)
    }

    fun enterSelectionMode() {
        mutableState.update { it.copy(isSelecting = true, batchSummary = null) }
    }

    fun exitSelectionMode() {
        if (batchJob?.isActive == true) return
        mutableState.update { it.copy(isSelecting = false, selected = emptyMap()) }
    }

    fun toggleSelection(extension: Extension) {
        if (!extension.canBatchInstall() || batchJob?.isActive == true) return
        mutableState.update {
            val packageName = extension.installPackageName
            val selected = if (packageName in it.selected) it.selected - packageName
            else it.selected + (packageName to extension)
            it.copy(isSelecting = true, selected = selected, batchSummary = null)
        }
    }

    fun installSelected() = runBatch(state.value.selected.values.toList())

    fun cancelBatch() {
        batchJob?.cancel()
    }

    private fun runBatch(extensions: List<Extension>) {
        if (extensions.isEmpty() || batchJob?.isActive == true) return
        val queue = extensions.distinctBy { it.installPackageName }
        val remaining = queue.associateBy { it.installPackageName }.toMutableMap()
        mutableState.update { it.copy(batchRunning = true, batchSummary = null) }
        batchJob = screenModelScope.launch(Dispatchers.Main.immediate) {
            var completed = 0
            var failed = 0
            try {
                for (extension in queue) {
                    if (runInstall(extension) == InstallStep.Installed) {
                        remaining.remove(extension.installPackageName)
                        completed++
                    } else {
                        failed++
                    }
                }
            } finally {
                mutableState.update {
                    it.copy(
                        batchRunning = false,
                        isSelecting = remaining.isNotEmpty(),
                        selected = remaining.toMap(),
                        batchSummary = BatchSummary(completed, failed, remaining.size - failed),
                    )
                }
            }
        }
    }

    fun installExtension(extension: Extension.Available) {
        screenModelScope.launch(Dispatchers.Main.immediate) {
            runInstall(extension)
        }
    }

    fun updateExtension(extension: Extension.Installed) {
        screenModelScope.launch(Dispatchers.Main.immediate) {
            runInstall(extension)
        }
    }

    fun cancelInstallUpdateExtension(extension: Extension) {
        extensionManager.cancelInstallUpdateExtension(extension)
    }

    private fun addDownloadState(extension: Extension, installStep: InstallStep) {
        currentDownloads.update { it + Pair(extension.installPackageName, installStep) }
    }

    private fun removeDownloadState(extension: Extension) {
        currentDownloads.update { it - extension.installPackageName }
    }

    private suspend fun runInstall(extension: Extension): InstallStep {
        if (extension.installPackageName in currentDownloads.value) return InstallStep.Error
        addDownloadState(extension, InstallStep.Pending)
        try {
            val steps: Flow<InstallStep> = when (extension) {
                is Extension.Available -> extensionManager.installExtension(extension)
                is Extension.Installed -> extensionManager.updateExtension(extension)
                is Extension.Untrusted -> return InstallStep.Error
            }
            var finalStep = InstallStep.Error
            withContext(Dispatchers.IO) {
                steps.collect { step ->
                    finalStep = step
                    addDownloadState(extension, step)
                }
            }
            return finalStep
        } catch (e: CancellationException) {
            extensionManager.cancelInstallUpdateExtension(extension)
            throw e
        } catch (_: Exception) {
            return InstallStep.Error
        } finally {
            removeDownloadState(extension)
        }
    }

    fun uninstallExtension(extension: Extension) {
        extensionManager.uninstallExtension(extension)
    }

    fun findAvailableExtensions() {
        screenModelScope.launchIO {
            mutableState.update { it.copy(isRefreshing = true) }

            extensionManager.findAvailableExtensions()

            // Fake slower refresh so it doesn't seem like it's not doing anything
            delay(1.seconds)

            mutableState.update { it.copy(isRefreshing = false) }
        }
    }

    fun trustExtension(extension: Extension.Untrusted) {
        screenModelScope.launch {
            extensionManager.trust(extension)
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val items: ItemGroups = mutableMapOf(),
        val updates: Int = 0,
        val installer: BasePreferences.ExtensionInstaller? = null,
        val searchQuery: String? = null,
        val isSelecting: Boolean = false,
        val selected: Map<String, Extension> = emptyMap(),
        val batchRunning: Boolean = false,
        val batchSummary: BatchSummary? = null,
    ) {
        val isEmpty = items.isEmpty()
    }

    data class BatchSummary(val completed: Int, val failed: Int, val cancelled: Int)
}

internal fun Extension.canBatchInstall(): Boolean = this is Extension.Available || this is Extension.Installed && hasUpdate

typealias ItemGroups = MutableMap<ExtensionUiModel.Header, List<ExtensionUiModel.Item>>

object ExtensionUiModel {
    sealed interface Header {
        data class Resource(val textRes: StringResource) : Header
        data class Text(val text: String) : Header
    }

    data class Item(
        val extension: Extension,
        val installStep: InstallStep,
    )
}
