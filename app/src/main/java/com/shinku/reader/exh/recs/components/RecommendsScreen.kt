package com.shinku.reader.exh.recs.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.shinku.reader.presentation.browse.components.GlobalSearchCardRow
import com.shinku.reader.presentation.browse.components.GlobalSearchErrorResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchLoadingResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchResultItem
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.util.formattedMessage
import com.shinku.reader.exh.recs.RecommendationItemResult
import com.shinku.reader.exh.recs.RecommendsScreenModel
import com.shinku.reader.exh.recs.sources.RecommendationPagingSource
import kotlinx.collections.immutable.ImmutableMap
import nl.adaptivity.xmlutil.core.impl.multiplatform.name
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun RecommendsScreen(
    title: String,
    state: RecommendsScreenModel.State,
    navigateUp: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (RecommendationPagingSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigateUp = navigateUp,
            )
        },
    ) { paddingValues ->
        RecommendsContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
internal fun RecommendsContent(
    items: ImmutableMap<RecommendationPagingSource, RecommendationItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (RecommendationPagingSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items.forEach { (source, recResult) ->
            item(key = "${source::class.name}-${source.name}-${source.category.resourceId}") {
                GlobalSearchResultItem(
                    title = source.name,
                    subtitle = stringResource(source.category),
                    onClick = { onClickSource(source) },
                ) {
                    when (recResult) {
                        RecommendationItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is RecommendationItemResult.Success -> {
                            GlobalSearchCardRow(
                                titles = recResult.result,
                                getManga = getManga,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is RecommendationItemResult.Error -> {
                            GlobalSearchErrorResultItem(
                                message = with(LocalContext.current) {
                                    recResult.throwable.formattedMessage
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
