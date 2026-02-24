package com.shinku.reader.exh.recs

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.presentation.browse.BrowseSourceContent
import com.shinku.reader.presentation.browse.components.BrowseSourceSimpleToolbar
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.ui.browse.source.SourcesScreen
import com.shinku.reader.ui.manga.MangaScreen
import com.shinku.reader.ui.webview.WebViewActivity
import com.shinku.reader.exh.recs.batch.RankedSearchResults
import com.shinku.reader.exh.ui.ifSourcesLoaded
import com.shinku.reader.presentation.core.util.collectAsLazyPagingItems
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.screens.LoadingScreen
import java.io.Serializable

class BrowseRecommendsScreen(
    private val args: Args,
    private val isExternalSource: Boolean,
) : Screen() {

    sealed interface Args : Serializable {
        data class SingleSourceManga(
            val mangaId: Long,
            val sourceId: Long,
            val recommendationSourceName: String,
        ) : Args
        data class MergedSourceMangas(val results: RankedSearchResults) : Args
    }

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { BrowseRecommendsScreenModel(args) }
        val snackbarHostState = remember { SnackbarHostState() }

        val onClickItem = { manga: Manga ->
            navigator.push(
                when (isExternalSource) {
                    true -> SourcesScreen(SourcesScreen.SmartSearchConfig(manga.ogTitle))
                    false -> MangaScreen(manga.id, true)
                },
            )
        }

        val onLongClickItem = { manga: Manga ->
            when (isExternalSource) {
                true -> WebViewActivity.newIntent(context, manga.url, title = manga.title).let(context::startActivity)
                false -> onClickItem(manga)
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                val title = remember {
                    val recSource = screenModel.recommendationSource
                    "${recSource.name} (${recSource.category.getString(context)})"
                }

                BrowseSourceSimpleToolbar(
                    navigateUp = navigator::pop,
                    title = title,
                    displayMode = screenModel.displayMode,
                    onDisplayModeChange = { screenModel.displayMode = it },
                    scrollBehavior = scrollBehavior,
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            BrowseSourceContent(
                source = screenModel.source,
                mangaList = screenModel.mangaPagerFlowFlow.collectAsLazyPagingItems(),
                columns = screenModel.getColumnsPreference(LocalConfiguration.current.orientation),
                // SY -->
                ehentaiBrowseDisplayMode = false,
                // SY <--
                displayMode = screenModel.displayMode,
                snackbarHostState = snackbarHostState,
                contentPadding = paddingValues,
                onWebViewClick = null,
                onHelpClick = null,
                onLocalSourceHelpClick = null,
                onMangaClick = onClickItem,
                onMangaLongClick = onLongClickItem,
            )
        }
    }
}
