package com.shinku.reader.ui.category.sources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.presentation.category.SourceCategoryScreen
import com.shinku.reader.presentation.category.components.CategoryCreateDialog
import com.shinku.reader.presentation.category.components.CategoryDeleteDialog
import com.shinku.reader.presentation.category.components.CategoryRenameDialog
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.LoadingScreen

class SourceCategoryScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SourceCategoryScreenModel() }

        val state by screenModel.state.collectAsState()

        if (state is SourceCategoryScreenState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as SourceCategoryScreenState.Success

        SourceCategoryScreen(
            state = successState,
            onClickCreate = { screenModel.showDialog(SourceCategoryDialog.Create) },
            onClickRename = { screenModel.showDialog(SourceCategoryDialog.Rename(it)) },
            onClickDelete = { screenModel.showDialog(SourceCategoryDialog.Delete(it)) },
            navigateUp = navigator::pop,
        )

        when (val dialog = successState.dialog) {
            null -> {}
            SourceCategoryDialog.Create -> {
                CategoryCreateDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onCreate = { screenModel.createCategory(it) },
                    // SY -->
                    categories = successState.categories,
                    title = stringResource(MR.strings.action_add_category),
                    // SY <--
                )
            }
            is SourceCategoryDialog.Rename -> {
                CategoryRenameDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onRename = { screenModel.renameCategory(dialog.category, it) },
                    // SY -->
                    categories = successState.categories,
                    category = dialog.category,
                    // SY <--
                )
            }
            is SourceCategoryDialog.Delete -> {
                CategoryDeleteDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onDelete = { screenModel.deleteCategory(dialog.category) },
                    // SY -->
                    title = stringResource(MR.strings.delete_category),
                    text = stringResource(MR.strings.delete_category_confirmation, dialog.category),
                    // SY <--
                )
            }
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                if (event is SourceCategoryEvent.LocalizedMessage) {
                    context.toast(event.stringRes)
                }
            }
        }
    }
}
