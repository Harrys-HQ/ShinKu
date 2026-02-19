package eu.kanade.tachiyomi.ui.browse.migration.failed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateSearchScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FailedUpdatesMigrationScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { FailedUpdatesMigrationModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.failed_updates_migration_title),
                    navigateUp = navigator::pop,
                    actions = {
                        if (state.items.isNotEmpty()) {
                            IconButton(onClick = screenModel::clearList) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(MR.strings.action_reset),
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when {
                state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
                state.items.isEmpty() -> EmptyScreen(
                    stringRes = MR.strings.failed_updates_migration_empty,
                    modifier = Modifier.padding(contentPadding),
                )
                else -> {
                    FailedUpdateList(
                        items = state.items,
                        contentPadding = contentPadding,
                        onClickItem = { manga ->
                            navigator.push(MigrateSearchScreen(manga.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FailedUpdateList(
    items: ImmutableList<Manga>,
    contentPadding: PaddingValues,
    onClickItem: (Manga) -> Unit,
) {
    val sourceManager: SourceManager = Injekt.get()
    ScrollbarLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = items,
            key = { it.id },
        ) { manga ->
            val source = sourceManager.getOrStub(manga.source)
            val domainSource = tachiyomi.domain.source.model.Source(
                id = source.id,
                lang = source.lang,
                name = source.name,
                supportsLatest = false,
                isStub = source is StubSource,
            )
            BaseSourceItem(
                modifier = Modifier.clickable { onClickItem(manga) },
                source = domainSource,
                showLanguageInContent = false,
                onClickItem = { onClickItem(manga) },
                onLongClickItem = { },
                icon = { SourceIcon(source = domainSource) },
                action = { },
                content = { _, _ ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = manga.title,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
            )
        }
    }
}

private class FailedUpdatesMigrationModel(
    private val preferences: LibraryPreferences = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : StateScreenModel<FailedUpdatesMigrationModel.State>(State()) {

    init {
        loadFailedManga()
    }

    private fun loadFailedManga() {
        screenModelScope.launchIO {
            val failedIds = preferences.failedUpdatesMangaIds().get().mapNotNull { it.toLongOrNull() }
            val mangaList = failedIds.mapNotNull { getManga.await(it) }
            mutableState.update {
                it.copy(
                    isLoading = false,
                    items = mangaList.toImmutableList(),
                )
            }
        }
    }

    fun clearList() {
        preferences.failedUpdatesMangaIds().delete()
        mutableState.update { it.copy(items = kotlinx.collections.immutable.persistentListOf()) }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: ImmutableList<Manga> = kotlinx.collections.immutable.persistentListOf(),
    )
}
