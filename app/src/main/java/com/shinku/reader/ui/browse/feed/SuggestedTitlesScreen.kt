package com.shinku.reader.ui.browse.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.manga.components.MangaCover
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.ui.browse.source.globalsearch.GlobalSearchScreen
import com.shinku.reader.ui.manga.MangaScreen
import kotlinx.collections.immutable.persistentListOf

class SuggestedTitlesScreen(
    private val title: String,
    private val mangas: List<Manga>,
    private val mode: String = "",
    private val query: String = "",
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel {
            SuggestedTitlesScreenModel(
                title = title,
                initialMangas = mangas,
                mode = mode,
                query = query,
            )
        }
        val state by screenModel.state.collectAsState()
        val gridState = rememberLazyGridState()

        val shouldLoadMore by remember {
            derivedStateOf {
                val totalItems = gridState.layoutInfo.totalItemsCount
                val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleIndex >= totalItems - 4
            }
        }

        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore && !state.isLoadingMore && state.hasMore) {
                screenModel.loadNextPage()
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = title,
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        val searchGenre = title
                            .removeSuffix(" Highlights")
                            .removePrefix("Top ")
                            .removeSuffix(" Titles")
                            .trim()
                        if (searchGenre.isNotBlank() && searchGenre != "Titles For You" && searchGenre != "Recommendations") {
                            AppBarActions(
                                persistentListOf(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_search),
                                        icon = Icons.Outlined.Search,
                                        onClick = {
                                            navigator.push(GlobalSearchScreen(searchGenre))
                                        },
                                    ),
                                ),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            if (state.mangas.isEmpty()) {
                EmptyScreen(MR.strings.no_results_found)
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding() + 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.mangas, key = { it.id }) { manga ->
                        SuggestedTitleCard(
                            manga = manga,
                            onClick = { navigator.push(MangaScreen(manga.id, true)) },
                        )
                    }

                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SuggestedTitleCard(
        manga: Manga,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f),
            ) {
                MangaCover.Book(
                    data = manga,
                    contentDescription = manga.title,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = manga.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 2.dp),
            )

            if (!manga.author.isNullOrBlank()) {
                Text(
                    text = manga.author.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}
