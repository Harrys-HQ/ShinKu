package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.presentation.library.components.CommonMangaItemDefaults
import com.shinku.reader.presentation.manga.components.MangaCover
import kotlinx.coroutines.delay

@Composable
fun DiscoverFeaturedCarousel(
    featuredManga: List<Manga>,
    onClickManga: (Manga) -> Unit,
    getMangaState: (@Composable (Manga) -> State<Manga>)? = null,
    modifier: Modifier = Modifier,
) {
    if (featuredManga.isEmpty()) return

    val pagerState = rememberPagerState { featuredManga.size }

    // Auto-advance hero carousel with gentle 6-second cadence when not actively interacted with
    LaunchedEffect(pagerState.pageCount, pagerState.isScrollInProgress) {
        if (featuredManga.size > 1 && !pagerState.isScrollInProgress) {
            delay(6000)
            val nextPage = (pagerState.currentPage + 1) % featuredManga.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val manga = featuredManga[page]
            FeaturedCard(
                manga = manga,
                onClick = { onClickManga(manga) },
                getMangaState = getMangaState,
            )
        }

        if (featuredManga.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(featuredManga.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(
                                width = if (isSelected) 20.dp else 6.dp,
                                height = 6.dp,
                            )
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    manga: Manga,
    onClick: () -> Unit,
    getMangaState: (@Composable (Manga) -> State<Manga>)? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val currentManga = getMangaState?.invoke(manga)?.value ?: manga
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val cardHeight = if (isTablet) 240.dp else 200.dp

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
        ) {
            // Full-bleed Cover Background
            MangaCover.Book(
                data = currentManga,
                contentDescription = currentManga.title,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (currentManga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f),
            )

            // Cinematic Multi-Stop Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.92f),
                            ),
                        ),
                    ),
            )

            // In-Library indicator badge in top-end
            if (currentManga.favorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                ) {
                    InLibraryBadge(enabled = true)
                }
            }

            // Content Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(if (isTablet) 22.dp else 18.dp),
            ) {
                // "FEATURED" pill badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FEATURED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.1.sp,
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Manga Title
                Text(
                    text = currentManga.title,
                    style = if (isTablet) MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    else MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!currentManga.author.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentManga.author!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverGenreCloud(
    selectedGenre: String? = null,
    onSelectGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val genres = listOf(
        "Action",
        "Adventure",
        "Comedy",
        "Fantasy",
        "Isekai",
        "Psychological",
        "Romance",
        "Sci-Fi",
        "Seinen",
        "Shonen",
        "Slice of Life",
        "Supernatural",
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Leading "All / Genres" filter pill
        item {
            val isAllSelected = selectedGenre.isNullOrEmpty()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                border = BorderStroke(
                    1.dp,
                    if (isAllSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectGenre("") },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = if (isAllSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isAllSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        items(genres) { genre ->
            val isSelected = selectedGenre.equals(genre, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectGenre(genre) },
            ) {
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
fun DiscoverSectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onClearClick != null) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (onSeeAllClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSeeAllClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "See",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverMangaRow(
    mangas: List<Manga>,
    onClickManga: (Manga) -> Unit,
    onLongClickManga: ((Manga) -> Unit)? = null,
    getMangaState: (@Composable (Manga) -> State<Manga>)? = null,
    modifier: Modifier = Modifier,
) {
    if (mangas.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(mangas, key = { it.id }) { manga ->
            DiscoverMangaCard(
                manga = manga,
                onClick = { onClickManga(manga) },
                onLongClick = onLongClickManga?.let { { it(manga) } },
                getMangaState = getMangaState,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverMangaCard(
    manga: Manga,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    getMangaState: (@Composable (Manga) -> State<Manga>)? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val currentManga = getMangaState?.invoke(manga)?.value ?: manga
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val cardWidth = if (isTablet) 128.dp else 116.dp
    val cardHeight = if (isTablet) 180.dp else 162.dp

    Column(
        modifier = modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick?.invoke() ?: onClick()
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                MangaCover.Book(
                    data = currentManga,
                    contentDescription = currentManga.title,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (currentManga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f),
                )
            }

            if (currentManga.favorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                ) {
                    InLibraryBadge(enabled = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = currentManga.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
