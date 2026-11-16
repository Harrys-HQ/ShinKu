package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.source.model.FeedCategory
import kotlinx.collections.immutable.ImmutableList

@Composable
fun FeedCategoryTabs(
    categories: ImmutableList<FeedCategory>,
    selectedCategoryIndex: Int,
    onSelectCategory: (Int) -> Unit,
    onManageCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.size <= 1) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex.coerceIn(0, categories.size - 1),
            edgePadding = 16.dp,
            modifier = Modifier.weight(1f),
            divider = {},
        ) {
            categories.forEachIndexed { index, category ->
                val selected = index == selectedCategoryIndex
                Tab(
                    selected = selected,
                    onClick = { onSelectCategory(index) },
                    text = {
                        Text(
                            text = category.name,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                )
            }
        }
        IconButton(
            onClick = onManageCategories,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Manage Categories",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
