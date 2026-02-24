package com.shinku.reader.presentation.browse

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.components.TabContent
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun BrowseTabWrapper(tab: TabContent, onBackPressed: (() -> Unit)? = null) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(tab.titleRes),
                actions = {
                    AppBarActions(tab.actions)
                },
                navigateUp = onBackPressed,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        tab.content(paddingValues, snackbarHostState)
    }
}
