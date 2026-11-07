package com.shinku.reader.presentation.category.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.DropdownMenu
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.pluralStringResource
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.sanctuaryShadow
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
fun ReorderableCollectionItemScope.CategoryListItem(
    category: Category,
    titleCount: Long,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .sanctuaryShadow(
                elevation = SanctuaryElevation.subtle,
                shape = SanctuaryShapes.deckCard,
            )
            .border(
                border = SanctuaryBrushes.subtleBorder(),
                shape = SanctuaryShapes.deckCard,
            ),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SanctuaryBrushes.cardHorizontalGradient())
                .clickable(onClick = onRename)
                .padding(vertical = MaterialTheme.padding.small)
                .padding(
                    start = MaterialTheme.padding.small,
                    end = MaterialTheme.padding.medium,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = stringResource(MR.strings.category_reorder, category.name),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(MaterialTheme.padding.medium)
                    .draggableHandle(),
            )
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    pluralStringResource(MR.plurals.category_titles, titleCount.toInt(), titleCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(MR.strings.action_rename_category),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.foundation.layout.Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(MR.strings.label_more))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(MR.strings.action_delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
