package com.shinku.reader.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

object TabbedDialogPaddings {
    val Horizontal = 24.dp
    val Vertical = 8.dp
}

@Composable
fun TabbedDialog(
    onDismissRequest: () -> Unit,
    tabTitles: ImmutableList<String>,
    modifier: Modifier = Modifier,
    tabOverflowMenuContent: (@Composable ColumnScope.(() -> Unit) -> Unit)? = null,
    pagerState: PagerState = rememberPagerState { tabTitles.size },
    footer: @Composable () -> Unit = {},
    header: @Composable () -> Unit = {},
    content: @Composable (Int) -> Unit,
) {
    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Sanctuary Sheet Drag Handle
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(SanctuaryShapes.pill)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
            )

            header()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = SanctuaryShapes.deckCard,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    border = SanctuaryBrushes.subtleBorder(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .horizontalScroll(rememberScrollState())
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        tabTitles.fastForEachIndexed { index, tab ->
                            val isSelected = pagerState.currentPage == index
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(SanctuaryShapes.medium)
                                    .selectable(
                                        selected = isSelected,
                                        role = Role.Tab,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                    ),
                                shape = SanctuaryShapes.medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            ) {
                                Text(
                                    text = tab,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                                )
                            }
                        }
                    }
                }

                tabOverflowMenuContent?.let { MoreMenu(it) }
            }

            HorizontalPager(
                modifier = Modifier.weight(1f, fill = false).animateContentSize(),
                state = pagerState,
                verticalAlignment = Alignment.Top,
                pageContent = { page -> content(page) },
            )
            footer()
        }
    }
}

@Composable
private fun MoreMenu(
    content: @Composable ColumnScope.(() -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(MR.strings.label_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            content { expanded = false }
        }
    }
}
