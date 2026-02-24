package com.shinku.reader.ui.browse.migration.dead

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.presentation.browse.components.BaseSourceItem
import com.shinku.reader.presentation.browse.components.SourceIcon
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.data.migration.DeadSourceScannerJob
import com.shinku.reader.ui.browse.migration.manga.MigrateMangaScreen
import com.shinku.reader.util.system.toast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.domain.source.model.Source
import com.shinku.reader.domain.source.model.StubSource
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.Badge
import com.shinku.reader.presentation.core.components.BadgeGroup
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeadSourceScannerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { DeadSourceScannerModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.dead_source_scanner_title),
                    navigateUp = navigator::pop,
                    actions = {
                        AppBar.Action(
                            title = stringResource(MR.strings.action_retry),
                            icon = Icons.Outlined.Refresh,
                            onClick = {
                                DeadSourceScannerJob.startNow(context)
                                context.toast(MR.strings.updating_library)
                            },
                        )
                    },
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
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) : StateScreenModel<DeadSourceScannerModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            libraryPreferences.deadSourceIds().changes()
                .collectLatest { deadIds ->
                    val libraryManga = getLibraryManga.await()
                    val deadSources = deadIds
                        .mapNotNull { it.toLongOrNull() }
                        .map { sourceId ->
                            val source = sourceManager.getOrStub(sourceId)
                            source to libraryManga.count { it.manga.source == sourceId }
                        }
                        .sortedByDescending { it.second }

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
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: ImmutableList<Pair<Source, Long>> = kotlinx.collections.immutable.persistentListOf(),
    )
}
