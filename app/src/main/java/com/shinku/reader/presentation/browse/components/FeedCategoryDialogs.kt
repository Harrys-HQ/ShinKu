package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.source.model.FeedCategory
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import kotlinx.collections.immutable.ImmutableList

@Composable
fun FeedManageCategoriesDialog(
    categories: ImmutableList<FeedCategory>,
    onDismiss: () -> Unit,
    onCreateCategory: () -> Unit,
    onEditCategory: (FeedCategory) -> Unit,
    onDeleteCategory: (FeedCategory) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SanctuaryShapes.hero,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(SanctuaryShapes.medium)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    SanctuaryOverlineBadge(text = "FEED TABS", compact = true)
                    Text(
                        text = "Feed Categories",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    val isGlobal = category._id == 1L
                    Surface(
                        shape = SanctuaryShapes.medium,
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val langLabel = category.lang.takeIf { it != "all" }
                                if (isGlobal || langLabel != null) {
                                    Text(
                                        text = if (isGlobal) "Default Tab" else "Language: $langLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (!isGlobal) {
                                IconButton(
                                    onClick = { onEditCategory(category) },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit category",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCategory(category) },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete category",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onCreateCategory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SanctuaryShapes.medium,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Category",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_close))
            }
        },
    )
}

@Composable
fun FeedCreateCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, lang: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("all") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SanctuaryShapes.hero,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SanctuaryOverlineBadge(text = "NEW TAB", compact = true)
                Text(
                    text = "Create Category",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Manhwa, Romance, Updates") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SanctuaryShapes.medium,
                )

                OutlinedTextField(
                    value = lang,
                    onValueChange = { lang = it },
                    label = { Text("Language Filter (optional)") },
                    placeholder = { Text("all, en, ja, ko, etc.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SanctuaryShapes.medium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), lang.trim().ifBlank { "all" })
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(text = stringResource(MR.strings.action_ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun FeedEditCategoryDialog(
    category: FeedCategory,
    onDismiss: () -> Unit,
    onConfirm: (name: String, lang: String) -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var lang by remember { mutableStateOf(category.lang) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SanctuaryShapes.hero,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SanctuaryOverlineBadge(text = "EDIT TAB", compact = true)
                Text(
                    text = "Edit Category",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SanctuaryShapes.medium,
                )

                OutlinedTextField(
                    value = lang,
                    onValueChange = { lang = it },
                    label = { Text("Language Filter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SanctuaryShapes.medium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), lang.trim().ifBlank { "all" })
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(text = stringResource(MR.strings.action_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun FeedDeleteCategoryDialog(
    category: FeedCategory,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SanctuaryShapes.hero,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SanctuaryOverlineBadge(text = "DELETE TAB", compact = true)
                Text(
                    text = "Delete \"${category.name}\"?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to delete this category? All source feeds organized in this category tab will also be removed.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(MR.strings.action_delete),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
