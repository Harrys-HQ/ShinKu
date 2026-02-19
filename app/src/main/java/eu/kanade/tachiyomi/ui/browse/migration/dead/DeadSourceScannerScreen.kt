package eu.kanade.tachiyomi.ui.browse.migration.dead

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrateMangaScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeadSourceScannerScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { DeadSourceScannerModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.dead_source_scanner_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when {
                state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
                state.items.isEmpty() -> EmptyScreen(
                    stringRes = MR.strings.dead_source_scanner_empty,
                    modifier = Modifier.padding(contentPadding),
                )
                else -> {
                    DeadSourceList(
                        items = state.items,
                        contentPadding = contentPadding,
                        onClickItem = { source ->
                            navigator.push(MigrateMangaScreen(source.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeadSourceList(
    items: ImmutableList<Pair<Source, Long>>,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = items,
            key = { (source, _) -> "dead-${source.id}" },
        ) { (source, count) ->
            BaseSourceItem(
                modifier = Modifier.clickable { onClickItem(source) },
                source = source,
                showLanguageInContent = false,
                onClickItem = { onClickItem(source) },
                onLongClickItem = { },
                icon = { SourceIcon(source = source) },
                action = {
                    BadgeGroup {
                        Badge(text = count.toString())
                    }
                },
                content = { _, _ ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = source.name.ifBlank { source.id.toString() },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(MR.strings.not_installed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        }
    }
}

private class DeadSourceScannerModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
) : StateScreenModel<DeadSourceScannerModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            val libraryManga = getLibraryManga.await()
            val deadSources = libraryManga
                .asSequence()
                .map { it.manga.source }
                .distinct()
                .map { sourceId ->
                    val source = sourceManager.getOrStub(sourceId)
                    source to libraryManga.count { it.manga.source == sourceId }
                }
                .filter { (source, _) -> source is StubSource && source.id != 1L } // 1L is Local Source
                .sortedByDescending { it.second }
                .toList()

            mutableState.update {
                it.copy(
                    isLoading = false,
                    items = deadSources.map { (s, count) ->
                        Source(
                            id = s.id,
                            lang = "",
                            name = s.name,
                            supportsLatest = false,
                            isStub = s is StubSource,
                        ) to count.toLong()
                    }.toImmutableList(),
                )
            }
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: ImmutableList<Pair<Source, Long>> = kotlinx.collections.immutable.persistentListOf(),
    )
}
