package com.shinku.reader.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.more.stats.StatsScreenContent
import com.shinku.reader.presentation.more.stats.StatsScreenState
import com.shinku.reader.presentation.util.Screen
import kotlinx.collections.immutable.persistentListOf
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.LoadingScreen

class StatsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { StatsScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_stats),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                    // SY -->
                    actions = {
                        val allRead by screenModel.allRead.collectAsState()
                        AppBarActions(
                            persistentListOf(
                                AppBar.OverflowAction(
                                    title = if (allRead) {
                                        stringResource(SYMR.strings.ignore_non_library_entries)
                                    } else {
                                        stringResource(SYMR.strings.include_all_read_entries)
                                    },
                                    onClick = screenModel::toggleReadManga,
                                ),
                            ),
                        )
                    },
                    // SY <--
                )
            },
        ) { paddingValues ->
            if (state is StatsScreenState.Loading) {
                LoadingScreen()
                return@Scaffold
            }

            StatsScreenContent(
                state = state as? StatsScreenState.Success ?: return@Scaffold,
                paddingValues = paddingValues,
            )
        }
    }
}
