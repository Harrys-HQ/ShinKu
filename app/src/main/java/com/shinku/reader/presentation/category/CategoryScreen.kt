package com.shinku.reader.presentation.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.category.components.CategoryListItem
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.plus
import com.shinku.reader.ui.category.CategoryScreenState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun CategoryScreen(
    state: CategoryScreenState.Success,
    onClickCreate: () -> Unit,
    onClickRename: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onChangeOrder: (Category, Int) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.categories),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        CategoryContent(
            categories = state.categories,
            titleCounts = state.titleCounts,
            onClickCreate = onClickCreate,
            lazyListState = lazyListState,
            paddingValues = paddingValues,
            onClickRename = onClickRename,
            onClickDelete = onClickDelete,
            onChangeOrder = onChangeOrder,
        )
    }
}

@Composable
private fun CategoryContent(
    categories: ImmutableList<Category>,
    titleCounts: ImmutableMap<Long, Long>,
    onClickCreate: () -> Unit,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickRename: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onChangeOrder: (Category, Int) -> Unit,
) {
    val categoriesState = remember { categories.toMutableStateList() }
    val reorderableState = rememberReorderableLazyListState(lazyListState, paddingValues) { from, to ->
        // Header and Add are list items too; persist category positions, not lazy-list indices.
        val fromIndex = categoriesState.indexOfFirst { it.key == from.key }
        val toIndex = categoriesState.indexOfFirst { it.key == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            val item = categoriesState.removeAt(fromIndex)
            categoriesState.add(toIndex, item)
            onChangeOrder(item, toIndex)
        }
    }

    LaunchedEffect(categories) {
        if (!reorderableState.isAnyItemDragging) {
            categoriesState.clear()
            categoriesState.addAll(categories)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = paddingValues +
            topSmallPaddingValues +
            PaddingValues(horizontal = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        item(key = "category-intro") {
            Text(
                stringResource(MR.strings.category_organization_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        if (categories.isEmpty()) {
            item(key = "category-empty") {
                Text(stringResource(MR.strings.information_empty_category), modifier = Modifier.padding(vertical = 16.dp))
            }
        }
        items(
            items = categoriesState,
            key = { category -> category.key },
        ) { category ->
            ReorderableItem(reorderableState, category.key) {
                CategoryListItem(
                    modifier = Modifier.animateItem(),
                    category = category,
                    titleCount = titleCounts[category.id] ?: 0L,
                    onRename = { onClickRename(category) },
                    onDelete = { onClickDelete(category) },
                )
            }
        }
        item(key = "category-add") {
            OutlinedButton(
                onClick = onClickCreate,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 4.dp, bottom = 24.dp),
                shape = com.shinku.reader.presentation.core.theme.SanctuaryShapes.medium,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(stringResource(MR.strings.action_add_category), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

private val Category.key inline get() = "category-$id"
