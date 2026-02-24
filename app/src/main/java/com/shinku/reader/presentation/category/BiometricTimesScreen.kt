package com.shinku.reader.presentation.category

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shinku.reader.presentation.category.components.CategoryFloatingActionButton
import com.shinku.reader.presentation.category.components.biometric.BiometricTimesContent
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.ui.category.biometric.BiometricTimesScreenState
import com.shinku.reader.ui.category.biometric.TimeRangeItem
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.util.plus

@Composable
fun BiometricTimesScreen(
    state: BiometricTimesScreenState.Success,
    onClickCreate: () -> Unit,
    onClickDelete: (TimeRangeItem) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                navigateUp = navigateUp,
                title = stringResource(SYMR.strings.biometric_lock_times),
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
                SYMR.strings.biometric_lock_times_empty,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        BiometricTimesContent(
            timeRanges = state.timeRanges,
            lazyListState = lazyListState,
            paddingValues = paddingValues + topSmallPaddingValues +
                PaddingValues(horizontal = MaterialTheme.padding.medium),
            onClickDelete = onClickDelete,
        )
    }
}
