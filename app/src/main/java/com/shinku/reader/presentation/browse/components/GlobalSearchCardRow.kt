package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.MangaCover
import com.shinku.reader.domain.manga.model.asMangaCover
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.library.components.CommonMangaItemDefaults
import com.shinku.reader.presentation.library.components.MangaComfortableGridItem

@Composable
fun GlobalSearchCardRow(
    titles: List<Manga>,
    getManga: @Composable (Manga) -> State<Manga>,
    onClick: (Manga) -> Unit,
    onLongClick: (Manga) -> Unit,
) {
    if (titles.isEmpty()) {
        EmptyResultItem()
        return
    }

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(titles) {
            val title by getManga(it)
            MangaItem(
                title = title.title,
                subtitle = title.genre?.take(2)?.joinToString(" · "),
                cover = title.asMangaCover(),
                isFavorite = title.favorite,
                onClick = { onClick(title) },
                onLongClick = { onLongClick(title) },
            )
        }
    }
}

@Composable
private fun MangaItem(
    title: String,
    subtitle: String? = null,
    cover: MangaCover,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(modifier = Modifier.width(112.dp)) {
        MangaComfortableGridItem(
            title = title,
            titleMaxLines = 3,
            captionSurface = true,
            coverData = cover,
            coverBadgeStart = {
                InLibraryBadge(enabled = isFavorite)
            },
            coverAlpha = if (isFavorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
            onClick = onClick,
            onLongClick = onLongClick,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun EmptyResultItem() {
    Column(
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(MR.strings.no_results_found),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
