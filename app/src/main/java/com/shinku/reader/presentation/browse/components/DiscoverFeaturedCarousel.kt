package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.overlineBadgeSmall
import com.shinku.reader.presentation.core.theme.sanctuaryShadow
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
            .padding(vertical = MaterialTheme.padding.small),
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
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
                            .clip(SanctuaryShapes.microIndicator)
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
            .sanctuaryShadow(
                elevation = SanctuaryElevation.hero,
                shape = SanctuaryShapes.hero,
                spotAlpha = 0.35f,
                ambientAlpha = 0.15f,
            ),
        shape = SanctuaryShapes.hero,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
        ) {
            MangaCover.Book(
                data = currentManga,
                contentDescription = currentManga.title,
                shape = SanctuaryShapes.hero,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (currentManga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f),
            )

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

            if (currentManga.favorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(MaterialTheme.padding.medium),
                ) {
                    InLibraryBadge(enabled = true)
                }
            }

            val topGenre = currentManga.genre?.firstOrNull()?.uppercase() ?: "FEATURED"
            Surface(
                shape = SanctuaryShapes.pill,
                color = Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MaterialTheme.padding.medium),
            ) {
                Text(
                    text = topGenre,
                    style = MaterialTheme.typography.overlineBadgeSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(if (isTablet) 22.dp else 18.dp),
            ) {
                Text(
                    text = currentManga.title,
                    style = if (isTablet) {
                        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    } else {
                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    },
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val summaryText = currentManga.description
                    ?.lineSequence()
                    ?.firstOrNull { it.isNotBlank() }
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: currentManga.author?.trim().orEmpty()

                if (summaryText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.84f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
