package com.shinku.reader.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.shinku.reader.presentation.browse.components.BaseSourceItem
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.more.settings.widget.SwitchPreferenceWidget
import com.shinku.reader.presentation.util.animateItemFastScroll
import com.shinku.reader.ui.browse.source.SourcesFilterScreenModel
import com.shinku.reader.util.system.LocaleHelper
import com.shinku.reader.domain.source.model.Source
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.FastScrollLazyColumn
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen

@Composable
fun SourcesFilterScreen(
    navigateUp: () -> Unit,
    state: SourcesFilterScreenModel.State.Success,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
    // SY -->
    onClickSources: (Boolean, List<Source>) -> Unit,
    // SY <--
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_sources),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.source_filter_empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            return@Scaffold
        }
        SourcesFilterContent(
            contentPadding = contentPadding,
            state = state,
            onClickLanguage = onClickLanguage,
            onClickSource = onClickSource,
            // SY -->
            onClickSources = onClickSources,
            // SY <--
        )
    }
}

@Composable
private fun SourcesFilterContent(
    contentPadding: PaddingValues,
    state: SourcesFilterScreenModel.State.Success,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
    // SY -->
    onClickSources: (Boolean, List<Source>) -> Unit,
    // SY <--
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        state.items.forEach { (language, sources) ->
            val enabled = language in state.enabledLanguages
            item(
                key = language,
                contentType = "source-filter-header",
            ) {
                SourcesFilterHeader(
                    modifier = Modifier.animateItemFastScroll(),
                    language = language,
                    enabled = enabled,
                    onClickItem = onClickLanguage,
                )
            }
            if (enabled) {
                // SY -->
                item(
                    key = "toggle-$language",
                    contentType = "source-filter-toggle",
                ) {
                    val toggleEnabled = remember(state.disabledSources) {
                        sources.none { it.id.toString() in state.disabledSources }
                    }
                    SourcesFilterToggle(
                        modifier = Modifier.animateItem(),
                        isEnabled = toggleEnabled,
                        onClickItem = {
                            onClickSources(!toggleEnabled, sources)
                        },
                    )
                }
                // SY <--
                items(
                    items = sources,
                    key = { "source-filter-${it.key()}" },
                    contentType = { "source-filter-item" },
                ) { source ->
                    SourcesFilterItem(
                        modifier = Modifier.animateItemFastScroll(),
                        source = source,
                        enabled = "${source.id}" !in state.disabledSources,
                        onClickItem = onClickSource,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcesFilterHeader(
    language: String,
    enabled: Boolean,
    onClickItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwitchPreferenceWidget(
        modifier = modifier,
        title = LocaleHelper.getSourceDisplayName(language, LocalContext.current),
        checked = enabled,
        onCheckedChanged = { onClickItem(language) },
    )
}

// SY -->
@Composable
fun SourcesFilterToggle(
    modifier: Modifier,
    isEnabled: Boolean,
    onClickItem: () -> Unit,
) {
    SwitchPreferenceWidget(
        modifier = modifier,
        title = stringResource(SYMR.strings.pref_category_all_sources),
        checked = isEnabled,
        onCheckedChanged = { onClickItem() },
    )
}

// SY <--

@Composable
private fun SourcesFilterItem(
    source: Source,
    enabled: Boolean,
    onClickItem: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        showLanguageInContent = false,
        onClickItem = { onClickItem(source) },
        action = {
            Checkbox(checked = enabled, onCheckedChange = null)
        },
    )
}
