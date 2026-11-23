package com.shinku.reader.presentation.browse.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.core.i18n.stringResource
import kotlinx.collections.immutable.persistentListOf

@Composable
fun BrowseSelectionToolbar(
    selectedCount: Int,
    onCancelSelectionMode: () -> Unit,
    onAddSelectedToLibrary: () -> Unit,
    onSelectAllLoaded: (() -> Unit)? = null,
    onInvertLoaded: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    AppBar(
        titleContent = {
            Text(
                text = "$selectedCount",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        isActionMode = true,
        onCancelActionMode = onCancelSelectionMode,
        actions = {
            AppBarActions(
                persistentListOf<AppBar.AppBarAction>().builder().apply {
                    if (onSelectAllLoaded != null) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_all),
                                icon = Icons.Outlined.SelectAll,
                                onClick = onSelectAllLoaded,
                            ),
                        )
                    }
                    if (onInvertLoaded != null) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_inverse),
                                icon = Icons.Outlined.FlipToBack,
                                onClick = onInvertLoaded,
                            ),
                        )
                    }
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.add_to_library),
                            icon = Icons.Outlined.LibraryAdd,
                            enabled = selectedCount > 0,
                            onClick = onAddSelectedToLibrary,
                        ),
                    )
                }.build(),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}
