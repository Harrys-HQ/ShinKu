package com.shinku.reader.presentation.browse.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.material.ExtendedFloatingActionButton
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun BrowseSourceFloatingActionButton(
    isVisible: Boolean,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        text = {
            Text(
                text = if (isVisible) {
                    stringResource(MR.strings.action_filter)
                } else {
                    stringResource(SYMR.strings.saved_searches)
                },
            )
        },
        icon = { Icon(Icons.Outlined.FilterList, contentDescription = "") },
        onClick = onFabClick,
    )
}
