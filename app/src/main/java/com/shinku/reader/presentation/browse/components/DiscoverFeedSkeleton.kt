package com.shinku.reader.presentation.browse.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.plus
import com.shinku.reader.presentation.core.util.shimmer

@Composable
fun DiscoverFeedSkeleton(
    contentPadding: PaddingValues,
    selectedGenre: String? = null,
    onClickGenre: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val heroHeight = if (isTablet) 240.dp else 200.dp
    val cardWidth = if (isTablet) 128.dp else 116.dp
    val cardHeight = if (isTablet) 180.dp else 162.dp
    val skeletonCardCount = if (isTablet) 7 else 4

    ScrollbarLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = true,
    ) {
        // 1. Shimmer Featured Hero Carousel Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(heroHeight)
                    .clip(SanctuaryShapes.hero)
                    .shimmer(),
            )
        }

        // 2. Interactive Discover Genre Cloud
        item {
            DiscoverGenreCloud(
                selectedGenre = selectedGenre,
                onSelectGenre = onClickGenre,
            )
        }

        // 3. Section 1 Shimmer: "Titles For You"
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .width(140.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(skeletonCardCount) {
                    Column(modifier = Modifier.width(cardWidth)) {
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                                .clip(SanctuaryShapes.coverLarge)
                                .shimmer(),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer(),
                        )
                    }
                }
            }
        }

        // 4. Section 2 Shimmer: Highlights
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .width(160.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(skeletonCardCount) {
                    Column(modifier = Modifier.width(cardWidth)) {
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                                .clip(SanctuaryShapes.coverLarge)
                                .shimmer(),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer(),
                        )
                    }
                }
            }
        }
    }
}
