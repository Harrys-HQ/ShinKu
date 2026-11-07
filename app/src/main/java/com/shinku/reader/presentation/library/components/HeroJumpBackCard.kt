package com.shinku.reader.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.sanctuaryShadow
import com.shinku.reader.presentation.manga.components.MangaCover

@Composable
fun HeroJumpBackCarousel(
    items: List<com.shinku.reader.ui.library.LibraryScreenModel.LastReadItem>,
    onClickContinue: (Long, Long) -> Unit,
    onClickDetails: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(0) { items.size }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.extraSmall),
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val item = items.getOrNull(page) ?: return@HorizontalPager
            HeroJumpBackCardCompact(
                manga = item.manga,
                chapter = item.chapter,
                currentPage = page,
                totalPages = items.size,
                onClickContinue = { onClickContinue(item.manga.id, item.chapter.id) },
                onClickDetails = { onClickDetails(item.manga.id) },
            )
        }
    }
}

@Composable
private fun HeroJumpBackCardCompact(
    manga: Manga,
    chapter: Chapter,
    currentPage: Int,
    totalPages: Int,
    onClickContinue: () -> Unit,
    onClickDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .sanctuaryShadow(
                elevation = SanctuaryElevation.compact,
                shape = SanctuaryShapes.card,
                spotAlpha = 0.25f,
                ambientAlpha = 0.10f,
            ),
        shape = SanctuaryShapes.card,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SanctuaryBrushes.compactHorizontalGradient())
                .clickable(onClick = onClickDetails)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(96.dp)
                        .clip(SanctuaryShapes.medium)
                        .shadow(SanctuaryElevation.subtle, SanctuaryShapes.medium),
                ) {
                    MangaCover.Book(
                        data = manga,
                        contentDescription = manga.title,
                        shape = SanctuaryShapes.medium,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SanctuaryOverlineBadge(
                            text = stringResource(MR.strings.library_continue_reading),
                            compact = true,
                        )

                        if (totalPages > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                repeat(totalPages) { index ->
                                    val isSelected = currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 6.dp else 4.dp)
                                            .clip(SanctuaryShapes.microIndicator)
                                            .background(
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                },
                                            ),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.padding.extraSmall))

                    Text(
                        text = manga.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = formatChapterText(chapter),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.padding.small))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilledTonalButton(
                            onClick = onClickContinue,
                            shape = SanctuaryShapes.medium,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(MR.strings.action_resume),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        OutlinedButton(
                            onClick = onClickDetails,
                            shape = SanctuaryShapes.medium,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(MR.strings.library_open_details), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroJumpBackCard(
    manga: Manga,
    chapter: Chapter,
    onClickContinue: () -> Unit,
    onClickDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            )
            .sanctuaryShadow(
                elevation = SanctuaryElevation.card,
                shape = SanctuaryShapes.hero,
                spotAlpha = 0.35f,
                ambientAlpha = 0.15f,
            ),
        shape = SanctuaryShapes.hero,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SanctuaryBrushes.cardHorizontalGradient())
                .clickable(onClick = onClickDetails)
                .padding(MaterialTheme.padding.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .clip(SanctuaryShapes.coverLarge)
                        .shadow(SanctuaryElevation.compact, SanctuaryShapes.coverLarge),
                ) {
                    MangaCover.Book(
                        data = manga,
                        contentDescription = manga.title,
                        shape = SanctuaryShapes.coverLarge,
                    )
                }

                Spacer(modifier = Modifier.width(MaterialTheme.padding.medium))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    SanctuaryOverlineBadge(
                        text = "JUMP BACK IN",
                        compact = true,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.padding.extraSmall))

                    Text(
                        text = manga.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formatChapterText(chapter),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalButton(
                            onClick = onClickContinue,
                            shape = SanctuaryShapes.medium,
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.heightIn(min = 40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Resume",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }

                        OutlinedButton(
                            onClick = onClickDetails,
                            shape = SanctuaryShapes.medium,
                            modifier = Modifier.heightIn(min = 40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = "Details",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun formatChapterText(chapter: Chapter): String {
    val numberStr = if (chapter.chapterNumber >= 0) {
        val num = chapter.chapterNumber
        val formatted = if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
        "Chapter $formatted"
    } else {
        chapter.name
    }

    val pageProgress = if (chapter.lastPageRead > 0) {
        " • Page ${chapter.lastPageRead + 1}"
    } else {
        ""
    }

    return "$numberStr$pageProgress"
}
