package com.shinku.reader.presentation.more.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.more.settings.screen.SearchableSettings
import com.shinku.reader.presentation.more.settings.widget.PreferenceGroupHeader
import com.shinku.reader.presentation.more.settings.widget.SanctuaryPreferenceSurface
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    ScrollbarLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        items.fastForEachIndexed { i, preference ->
            if (!preference.enabled) return@fastForEachIndexed
            when (preference) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.enabled) return@fastForEachIndexed

                    item {
                        Column {
                            PreferenceGroupHeader(title = preference.title)
                        }
                    }
                    val visibleItems = preference.preferenceItems.filter { it.enabled }
                    itemsIndexed(visibleItems) { index, item ->
                        SanctuaryPreferenceSurface(first = index == 0, last = index == visibleItems.lastIndex) {
                            PreferenceItem(item = item, highlightKey = highlightKey)
                        }
                    }
                    item {
                        if (i < items.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Create Preference Item
                is Preference.PreferenceItem<*, *> -> item {
                    if (preference.enabled) {
                        if (preference is Preference.PreferenceItem.CustomPreference || preference is Preference.PreferenceItem.InfoPreference) {
                            PreferenceItem(item = preference, highlightKey = highlightKey)
                        } else {
                            SanctuaryPreferenceSurface(standalone = true) {
                                PreferenceItem(item = preference, highlightKey = highlightKey)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMapIndexed { index, preference ->
        if (!preference.enabled) return@flatMapIndexed emptyList()
        val it = preference
        if (it is Preference.PreferenceGroup) {
            buildList<String?> {
                add(null) // Header
                addAll(it.preferenceItems.filter { groupItem -> groupItem.enabled }.map { groupItem -> groupItem.title })
                if (index < lastIndex) add(null) // Rendered spacer
            }
        } else {
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}
