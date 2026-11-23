package com.shinku.reader.ui.sourcehealth

import android.app.Application
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.data.diagnostics.DiagnosticsStore
import com.shinku.reader.data.diagnostics.SourceHealthProber
import com.shinku.reader.domain.diagnostics.DiagnosticRedactor
import com.shinku.reader.domain.diagnostics.ExtensionLoadIssue
import com.shinku.reader.domain.source.interactor.GetSourceHealth
import com.shinku.reader.domain.source.interactor.UpdateSourceHealth
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.extension.ExtensionManager
import com.shinku.reader.extension.model.Extension
import com.shinku.reader.presentation.more.sourcehealth.ExtensionDimensionStatus
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthItem
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthScreenState
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthSort
import com.shinku.reader.util.system.isOnline
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.local.LocalSource
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

class SourceHealthScreenModel(
    private val application: Application = Injekt.get(),
    private val getSourceHealth: GetSourceHealth = Injekt.get(),
    private val updateSourceHealth: UpdateSourceHealth = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val networkHelper: NetworkHelper = Injekt.get(),
    private val diagnosticsStore: DiagnosticsStore = Injekt.get(),
) : StateScreenModel<SourceHealthScreenState>(SourceHealthScreenState.Loading) {

    private val _sortMode = MutableStateFlow(SourceHealthSort.Health)
    private val _testingIds = MutableStateFlow<Set<Long>>(emptySet())
    private val singleProbeJobs = ConcurrentHashMap<Long, Job>()

    private data class ExtensionSnapshot(
        val installedSources: List<CatalogueSource>,
        val availableExtensions: List<Extension.Available>,
        val installedExtensions: List<Extension.Installed>,
        val untrustedExtensions: List<Extension.Untrusted>,
        val failedExtensions: List<ExtensionLoadIssue>,
    )

    private data class DiagnosticInputs(
        val healthList: List<com.shinku.reader.domain.source.model.SourceHealth>,
        val liveScan: com.shinku.reader.data.diagnostics.LiveHealthScanState,
        val repoChecks: Map<String, com.shinku.reader.domain.diagnostics.RepositoryHealthCheck>,
        val sortMode: SourceHealthSort,
        val testingIds: Set<Long>,
    )

    init {
        screenModelScope.launchIO {
            extensionManager.findAvailableExtensions()
        }

        val extensionFlow = combine(
            sourceManager.catalogueSources,
            extensionManager.availableExtensionsFlow,
            extensionManager.installedExtensionsFlow,
            extensionManager.untrustedExtensionsFlow,
            extensionManager.failedExtensionsFlow,
        ) { installedSources, availableExtensions, installedExtensions, untrustedExtensions, failedExtensions ->
            ExtensionSnapshot(
                installedSources = installedSources,
                availableExtensions = availableExtensions,
                installedExtensions = installedExtensions,
                untrustedExtensions = untrustedExtensions,
                failedExtensions = failedExtensions,
            )
        }

        val diagnosticFlow = combine(
            getSourceHealth.subscribeAll(),
            diagnosticsStore.liveScanState,
            diagnosticsStore.repoHealthChecks,
            _sortMode,
            _testingIds,
        ) { healthList, liveScan, repoChecks, sortMode, testingIds ->
            DiagnosticInputs(
                healthList = healthList,
                liveScan = liveScan,
                repoChecks = repoChecks,
                sortMode = sortMode,
                testingIds = testingIds,
            )
        }

        combine(extensionFlow, diagnosticFlow) { ext, diag ->
            buildState(ext, diag)
        }
            .launchIn(screenModelScope)
    }

    private fun buildState(ext: ExtensionSnapshot, diag: DiagnosticInputs): SourceHealthScreenState.Success {
        val healthMap = diag.healthList.associateBy { it.sourceId }
        val installedSourceIds = ext.installedSources.map { it.id }.toSet()

        val installedExtBySourceId = buildMap {
            ext.installedExtensions.forEach { installedExt ->
                installedExt.sources.forEach { put(it.id, installedExt) }
            }
        }
        val availableExtBySourceId = buildMap {
            ext.availableExtensions.forEach { availExt ->
                availExt.sources.forEach { put(it.id, availExt) }
            }
        }
        val untrustedByName = ext.untrustedExtensions.associateBy { it.name.lowercase() }
        val failedByName = ext.failedExtensions.associateBy { it.name.lowercase() }

        val repoSources = ext.availableExtensions.flatMap { available ->
            available.sources.filter { it.lang == "en" || it.id in installedSourceIds }
        }

        val allSourceGroups = (
            ext.installedSources.map { it.id to it.name } +
                repoSources.map { it.id to it.name }
            )
            .filterNot { it.first == LocalSource.ID }
            .groupBy {
                it.second.substringBeforeLast(" (")
                    .substringBeforeLast(" [")
                    .trim()
            }

        val allItems = allSourceGroups.map { (name, sources) ->
            val representativeSource = sources.find { healthMap.containsKey(it.first) } ?: sources.first()
            val id = representativeSource.first
            val dummySource = object : Source {
                override val id: Long = id
                override val name: String = name
                override val lang: String = ""
                override suspend fun getMangaDetails(manga: SManga) = throw UnsupportedOperationException()
                override suspend fun getChapterList(manga: SManga) = throw UnsupportedOperationException()
                override suspend fun getPageList(chapter: SChapter) = throw UnsupportedOperationException()
            }

            val isInstalled = sources.any { it.first in installedSourceIds }
            val installedExt = sources.firstNotNullOfOrNull { installedExtBySourceId[it.first] }
            val availableExt = sources.firstNotNullOfOrNull { availableExtBySourceId[it.first] }
            val untrustedExt = untrustedByName[name.lowercase()]
            val failedExt = failedByName[name.lowercase()]

            val (extStatus, extDetail) = when {
                installedExt?.isObsolete == true -> ExtensionDimensionStatus.OBSOLETE to "Extension not found in active repositories"
                installedExt?.hasUpdate == true -> ExtensionDimensionStatus.UPDATE_AVAILABLE to "Update available (${installedExt.versionName})"
                installedExt != null -> ExtensionDimensionStatus.LOADED to "${installedExt.name} v${installedExt.versionName}"
                failedExt != null -> ExtensionDimensionStatus.LOAD_FAILED to (failedExt.detail ?: failedExt.reason.name)
                untrustedExt != null -> ExtensionDimensionStatus.UNTRUSTED to "Extension package awaits trust approval"
                availableExt != null && !isInstalled -> ExtensionDimensionStatus.NOT_INSTALLED to "Available in repository (${availableExt.versionName})"
                isInstalled -> ExtensionDimensionStatus.LOADED to null
                else -> ExtensionDimensionStatus.UNKNOWN to null
            }

            val repoUrl = installedExt?.repoUrl ?: availableExt?.repoUrl
            val repoHost = repoUrl?.let(DiagnosticRedactor::redactRepoUrl)

            isInstalled to SourceHealthItem(
                source = dummySource,
                health = healthMap[id],
                extensionName = installedExt?.name ?: availableExt?.name ?: failedExt?.name,
                extensionLoadStatus = extStatus,
                extensionDetail = extDetail,
                repoHost = repoHost,
            )
        }

        val (installed, repo) = allItems.partition { it.first }
        val comparator = when (diag.sortMode) {
            SourceHealthSort.Health -> compareByDescending<SourceHealthItem> {
                if (it.health?.hasCheck == true) it.health.performanceScore else -1
            }.thenBy { it.source.name }
            SourceHealthSort.Speed -> compareBy<SourceHealthItem> {
                val health = it.health
                if (health == null || !health.hasCheck || health.avgLatency <= 0L) Long.MAX_VALUE else health.avgLatency
            }.thenBy { it.source.name }
            SourceHealthSort.Name -> compareBy { it.source.name }
        }

        val newState = SourceHealthScreenState.Success(
            installedList = installed.map { it.second }.sortedWith(comparator),
            repoList = repo.map { it.second }.sortedWith(comparator),
            sortMode = diag.sortMode,
            testingSourceIds = diag.testingIds,
            liveScan = diag.liveScan,
            repoChecks = diag.repoChecks.values.sortedBy { it.repoName.lowercase() },
        )
        mutableState.update { newState }
        return newState
    }

    fun setSortMode(mode: SourceHealthSort) {
        _sortMode.value = mode
    }

    fun cancelSingleSource(sourceId: Long) {
        singleProbeJobs.remove(sourceId)?.cancel()
        _testingIds.update { it - sourceId }
    }

    fun testSingleSource(sourceId: Long) {
        if (sourceId in _testingIds.value) return
        _testingIds.update { it + sourceId }

        val job = screenModelScope.launchIO {
            try {
                val installedHttpSource = sourceManager.get(sourceId) as? HttpSource
                val repoSource = extensionManager.availableExtensionsFlow.value
                    .asSequence()
                    .flatMap { it.sources.asSequence() }
                    .firstOrNull { it.id == sourceId }

                val targetUrl = installedHttpSource?.baseUrl ?: repoSource?.baseUrl.orEmpty()
                val sourceName = installedHttpSource?.name ?: repoSource?.name ?: "Source $sourceId"
                val correlationId = diagnosticsStore.newCorrelationId()

                val outcome = SourceHealthProber.probeSourceBaseUrl(
                    client = networkHelper.nonCloudflareClient,
                    targetUrl = targetUrl,
                    isNetworkAvailable = application::isOnline,
                    correlationId = correlationId,
                )
                updateSourceHealth.await(sourceId, outcome)
                diagnosticsStore.recordSourceProbe(sourceName, outcome)
            } catch (e: CancellationException) {
                throw e
            } finally {
                singleProbeJobs.remove(sourceId)
                _testingIds.update { it - sourceId }
            }
        }
        singleProbeJobs[sourceId] = job
    }
}
