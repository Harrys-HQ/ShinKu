package com.shinku.reader.exh.ui.metadata

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.util.system.copyToClipboard
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.screens.LoadingScreen
import com.shinku.reader.presentation.core.util.clickableNoIndication
import com.shinku.reader.presentation.core.util.plus

class MetadataViewScreen(private val mangaId: Long, private val sourceId: Long) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { MetadataViewScreenModel(mangaId, sourceId) }
        val navigator = LocalNavigator.currentOrThrow

        val state by screenModel.state.collectAsState()
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = screenModel.manga.collectAsState().value?.title,
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            when (
                @Suppress("NAME_SHADOWING")
                val state = state
            ) {
                MetadataViewState.Loading -> LoadingScreen()
                MetadataViewState.MetadataNotFound -> EmptyScreen(MR.strings.no_results_found)
                MetadataViewState.SourceNotFound -> EmptyScreen(MR.strings.source_empty_screen)
                is MetadataViewState.Success -> {
                    val context = LocalContext.current
                    val items = remember(state.meta) { state.meta.getExtraInfoPairs(context) }
                    ScrollbarLazyColumn(
                        contentPadding =
                        paddingValues + WindowInsets.navigationBars.asPaddingValues() + topSmallPaddingValues,
                    ) {
                        items(items) { (title, text) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickableNoIndication(
                                        onLongClick = {
                                            context.copyToClipboard(
                                                title,
                                                text,
                                            )
                                        },
                                        onClick = {},
                                    )
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    title,
                                    modifier = Modifier
                                        .width(140.dp)
                                        .padding(start = 16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LocalContentColor.current.copy(alpha = 0.7F),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
