package com.shinku.reader.ui.browse.source.globalsearch

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.exh.ui.ifSourcesLoaded
import com.shinku.reader.presentation.browse.GlobalSearchScreen
import com.shinku.reader.presentation.category.components.ChangeCategoryDialog
import com.shinku.reader.presentation.core.screens.LoadingScreen
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.ui.browse.source.browse.BrowseSourceScreen
import com.shinku.reader.ui.category.CategoryScreen
import com.shinku.reader.ui.manga.MangaScreen
import kotlinx.coroutines.flow.collectLatest

class GlobalSearchScreen(
    val searchQuery: String = "",
    private val extensionFilter: String? = null,
) : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val snackbarHostState = remember { SnackbarHostState() }

        val screenModel = rememberScreenModel {
            GlobalSearchScreenModel(
                initialQuery = searchQuery,
                initialExtensionFilter = extensionFilter,
            )
        }
        val state by screenModel.state.collectAsState()

        BackHandler(enabled = state.selection.selectionMode) {
            screenModel.exitSelectionMode()
        }

        LaunchedEffect(Unit) {
            screenModel.bulkFavoriteEvents.collectLatest { summary ->
                snackbarHostState.showSnackbar(summary.formatMessage(context))
            }
        }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            if (uri != null) {
                screenModel.searchImage(uri, context)
            }
        }

        var showSingleLoadingScreen by remember {
            mutableStateOf(searchQuery.isNotEmpty() && !extensionFilter.isNullOrEmpty() && state.total == 1)
        }

        if (showSingleLoadingScreen) {
            LoadingScreen()

            LaunchedEffect(state.items) {
                when (val result = state.items.values.singleOrNull()) {
                    SearchItemResult.Loading -> return@LaunchedEffect
                    is SearchItemResult.Success -> {
                        val manga = result.result.singleOrNull()
                        if (manga != null) {
                            navigator.replace(MangaScreen(manga.id, true))
                        } else {
                            // Backoff to result screen
                            showSingleLoadingScreen = false
                        }
                    }
                    else -> showSingleLoadingScreen = false
                }
            }
        } else {
            GlobalSearchScreen(
                state = state,
                navigateUp = navigator::pop,
                onChangeSearchQuery = screenModel::updateSearchQuery,
                onSearch = { screenModel.search() },
                getManga = { screenModel.getManga(it) },
                onChangeSearchFilter = screenModel::setSourceFilter,
                onToggleResults = screenModel::toggleFilterResults,
                onClickImageSearch = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClickSource = {
                    navigator.push(BrowseSourceScreen(it.id, state.searchQuery))
                },
                onClickItem = { manga ->
                    if (state.selection.selectionMode) {
                        screenModel.toggleSelection(manga)
                    } else {
                        navigator.push(MangaScreen(manga.id, true))
                    }
                },
                onLongClickItem = { manga ->
                    screenModel.toggleSelection(manga)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                snackbarHostState = snackbarHostState,
                onEnterSelectionMode = screenModel::enterSelectionMode,
                onCancelSelectionMode = screenModel::exitSelectionMode,
                onSelectAllLoaded = screenModel::selectAllLoaded,
                onInvertLoaded = screenModel::invertLoaded,
                onAddSelectedToLibrary = screenModel::requestBulkAddToLibrary,
            )

            when (val dialog = state.dialog) {
                is SearchScreenModel.Dialog.BulkChangeMangaCategory -> {
                    ChangeCategoryDialog(
                        initialSelection = dialog.initialSelection,
                        onDismissRequest = screenModel::clearDialog,
                        onEditCategories = { navigator.push(CategoryScreen()) },
                        onConfirm = { include, _ ->
                            screenModel.confirmBulkAddToLibrary(dialog.mangas, include)
                        },
                    )
                }
                else -> {}
            }
        }
    }
}
