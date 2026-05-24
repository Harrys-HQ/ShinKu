package com.shinku.reader.presentation.browse.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.secondaryItemAlpha
import com.shinku.reader.presentation.core.util.shimmer

@Composable
fun GlobalSearchResultItem(
    title: String,
    // SY -->
    subtitle: String?,
    // SY <--
    onClick: () -> Unit,
    // SY -->
    onLongClick: (() -> Unit)? = null,
    // SY <--
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(
                    start = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.extraSmall,
                )
                .fillMaxWidth()
                // SY -->
                .let {
                    if (onLongClick == null) {
                        it.clickable(onClick = onClick)
                    } else {
                        it.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    }
                },
            // SY <--
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.secondaryItemAlpha(),
                    )
                }
            }
            IconButton(onClick = onClick) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
        }
        content()
    }
}

@Composable
fun GlobalSearchLoadingResultItem() {
    Row(
        modifier = Modifier
            .padding(MaterialTheme.padding.small)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmer(),
            )
        }
    }
}

@Composable
fun GlobalSearchErrorResultItem(message: String?) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = Icons.Outlined.Error, contentDescription = null)
        Spacer(Modifier.height(4.dp))
        Text(
            text = message ?: stringResource(MR.strings.unknown_error),
            textAlign = TextAlign.Center,
        )
    }
}
