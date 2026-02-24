package com.shinku.reader.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.more.settings.widget.SwitchPreferenceWidget
import com.shinku.reader.ui.browse.extension.ExtensionFilterState
import com.shinku.reader.util.system.LocaleHelper
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen

@Composable
fun ExtensionFilterScreen(
    navigateUp: () -> Unit,
    state: ExtensionFilterState.Success,
    onClickToggle: (String) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_extensions),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            return@Scaffold
        }
        ExtensionFilterContent(
            contentPadding = contentPadding,
            state = state,
            onClickLang = onClickToggle,
        )
    }
}

@Composable
private fun ExtensionFilterContent(
    contentPadding: PaddingValues,
    state: ExtensionFilterState.Success,
    onClickLang: (String) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items(state.languages) { language ->
            SwitchPreferenceWidget(
                modifier = Modifier.animateItem(),
                title = LocaleHelper.getSourceDisplayName(language, context),
                checked = language in state.enabledLanguages,
                onCheckedChanged = { onClickLang(language) },
            )
        }
    }
}
