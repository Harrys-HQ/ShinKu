package com.shinku.reader.presentation.core.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList

data class EmptyScreenAction(
    val stringRes: StringResource,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun EmptyScreen(
    stringRes: StringResource,
    modifier: Modifier = Modifier,
    actions: ImmutableList<EmptyScreenAction>? = null,
) {
    EmptyScreen(
        message = stringResource(stringRes),
        modifier = modifier,
        actions = actions,
    )
}

@Composable
fun EmptyScreen(
    message: String,
    modifier: Modifier = Modifier,
    actions: ImmutableList<EmptyScreenAction>? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = SanctuaryShapes.hero,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = SanctuaryBrushes.subtleBorder(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SanctuaryBrushes.heroVerticalGradient())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SanctuaryOverlineBadge(
                    text = "SANCTUARY ARCHIVES",
                    compact = true,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .clip(SanctuaryShapes.card)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (!actions.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    ) {
                        actions.fastForEach { action ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                onClick = action.onClick,
                                shape = SanctuaryShapes.medium,
                            ) {
                                Icon(action.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(action.stringRes), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}
