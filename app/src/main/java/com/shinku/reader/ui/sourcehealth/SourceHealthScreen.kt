package com.shinku.reader.ui.sourcehealth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.data.library.RepoHealthScanJob
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.TabText
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.LoadingScreen
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthScreenContent
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthScreenState
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthSort
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

class SourceHealthScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val screenModel = rememberScreenModel { SourceHealthScreenModel() }
        val state by screenModel.state.collectAsState()
        
        val pagerState = rememberPagerState { 2 }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = "Source Health",
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        AppBarActions(
                            persistentListOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_update_library),
                                    icon = Icons.Outlined.Refresh,
                                    onClick = { 
                                        // Scan Installed only if on tab 0, else scan All (English)
                                        RepoHealthScanJob.startNow(context, onlyInstalled = pagerState.currentPage == 0) 
                                    },
                                ),
                                AppBar.OverflowAction(
                                    title = "Sort by Reliability",
                                    onClick = { screenModel.setSortMode(SourceHealthSort.Health) },
                                ),
                                AppBar.OverflowAction(
                                    title = "Sort by Speed",
                                    onClick = { screenModel.setSortMode(SourceHealthSort.Speed) },
                                ),
                                AppBar.OverflowAction(
                                    title = "Sort by Name",
                                    onClick = { screenModel.setSortMode(SourceHealthSort.Name) },
                                ),
                            ),
                        )
                    },
                )
            },
        ) { paddingValues ->
            if (state is SourceHealthScreenState.Loading) {
                LoadingScreen()
                return@Scaffold
            }

            val successState = state as SourceHealthScreenState.Success

            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { TabText(text = "Installed", badgeCount = successState.installedList.size) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { TabText(text = "Repository", badgeCount = successState.repoList.size) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> SourceHealthScreenContent(
                            healthList = successState.installedList,
                            paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                        )
                        1 -> SourceHealthScreenContent(
                            healthList = successState.repoList,
                            paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                        )
                    }
                }
            }
        }
    }
}
