package com.shinku.reader.presentation.category

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shinku.reader.presentation.category.components.CategoryFloatingActionButton
import com.shinku.reader.presentation.category.components.genre.SortTagContent
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.ui.category.genre.SortTagScreenState
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.util.plus

@Composable
fun SortTagScreen(
    state: SortTagScreenState.Success,
    onClickCreate: () -> Unit,
    onClickDelete: (String) -> Unit,
    onClickMoveUp: (String, Int) -> Unit,
    onClickMoveDown: (String, Int) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                navigateUp = navigateUp,
                title = stringResource(SYMR.strings.action_edit_tags),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            CategoryFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            EmptyScreen(
                SYMR.strings.information_empty_tags,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        SortTagContent(
            tags = state.tags,
            lazyListState = lazyListState,
            paddingValues = paddingValues + topSmallPaddingValues +
                PaddingValues(horizontal = MaterialTheme.padding.medium),
            onClickDelete = onClickDelete,
            onMoveUp = onClickMoveUp,
            onMoveDown = onClickMoveDown,
        )
    }
}
