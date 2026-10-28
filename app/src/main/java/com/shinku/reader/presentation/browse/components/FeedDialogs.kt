package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.model.SavedSearch
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun FeedAddDialog(
    sources: ImmutableList<CatalogueSource>,
    onDismiss: () -> Unit,
    onClickAdd: (CatalogueSource?) -> Unit,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        shape = SanctuaryShapes.hero,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SanctuaryOverlineBadge(text = "DISCOVER FEED", compact = true)
                Text(
                    text = stringResource(SYMR.strings.feed),
                    fontWeight = FontWeight.Black,
                )
            }
        },
        text = {
            RadioSelector(options = sources, selected = selected) {
                selected = it
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickAdd(selected?.let { sources[it] }) }) {
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
fun FeedAddSearchDialog(
    source: CatalogueSource,
    savedSearches: ImmutableList<SavedSearch?>,
    onDismiss: () -> Unit,
    onClickAdd: (CatalogueSource, SavedSearch?) -> Unit,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val searches = remember {
        savedSearches.toMutableList()
            .apply { add(0, null) }
            .toImmutableList()
    }
    val defaultTitle = stringResource(MR.strings.latest)
    AlertDialog(
        shape = SanctuaryShapes.hero,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SanctuaryOverlineBadge(text = "FEED SEARCH", compact = true)
                Text(
                    text = source.name,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        text = {
            RadioSelector(
                options = searches,
                selected = selected,
                title = {
                    it?.name ?: defaultTitle
                },
            ) {
                selected = it
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onClickAdd(
                        source,
                        selected?.let { searches[it] },
                    )
                },
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
fun <T> RadioSelector(
    options: ImmutableList<T>,
    selected: Int?,
    title: (T) -> String = { (it as? CatalogueSource)?.name ?: it.toString() },
    onSelect: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        options.forEachIndexed { index, option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(index) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == index,
                    onClick = { onSelect(index) },
                )
                Text(
                    text = title(option),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
fun FeedDeleteConfirmDialog(
    feed: FeedSavedSearch,
    onDismiss: () -> Unit,
    onClickDeleteConfirm: (FeedSavedSearch) -> Unit,
) {
    AlertDialog(
        shape = SanctuaryShapes.hero,
        title = {
            Text(
                text = stringResource(SYMR.strings.feed),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(text = stringResource(SYMR.strings.feed_delete))
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickDeleteConfirm(feed) }) {
                Text(text = stringResource(MR.strings.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
