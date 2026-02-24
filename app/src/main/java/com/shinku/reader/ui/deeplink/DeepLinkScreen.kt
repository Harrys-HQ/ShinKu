package com.shinku.reader.ui.deeplink

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.ui.browse.source.globalsearch.GlobalSearchScreen
import com.shinku.reader.ui.manga.MangaScreen
import com.shinku.reader.ui.reader.ReaderActivity
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.LoadingScreen

class DeepLinkScreen(
    val query: String = "",
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel {
            DeepLinkScreenModel(query = query)
        }
        val state by screenModel.state.collectAsState()
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.action_search_hint),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when (state) {
                is DeepLinkScreenModel.State.Loading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                is DeepLinkScreenModel.State.NoResults -> {
                    navigator.replace(GlobalSearchScreen(query))
                }
                is DeepLinkScreenModel.State.Result -> {
                    val resultState = state as DeepLinkScreenModel.State.Result
                    if (resultState.chapterId == null) {
                        navigator.replace(
                            MangaScreen(
                                resultState.manga.id,
                                true,
                            ),
                        )
                    } else {
                        navigator.pop()
                        ReaderActivity.newIntent(
                            context,
                            resultState.manga.id,
                            resultState.chapterId,
                        ).also(context::startActivity)
                    }
                }
            }
        }
    }
}
