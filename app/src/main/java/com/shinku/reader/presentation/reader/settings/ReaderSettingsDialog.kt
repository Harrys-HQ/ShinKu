package com.shinku.reader.presentation.reader.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.TabbedDialog
import com.shinku.reader.presentation.components.TabbedDialogPaddings
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.ui.reader.setting.ReaderSettingsScreenModel
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ReaderSettingsDialog(
    onDismissRequest: () -> Unit,
    onShowMenus: () -> Unit,
    onHideMenus: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
    translationContent: @Composable () -> Unit,
) {
    val tabTitles = persistentListOf(
        stringResource(MR.strings.reader_tab_reading),
        stringResource(MR.strings.action_display),
        stringResource(MR.strings.reader_tab_translation),
    )
    val pagerState = rememberPagerState { tabTitles.size }

    BoxWithConstraints {
        TabbedDialog(
            modifier = Modifier.heightIn(max = maxHeight * 0.75f),
            onDismissRequest = {
                onDismissRequest()
                onShowMenus()
            },
            tabTitles = tabTitles,
            pagerState = pagerState,
            header = {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(MR.strings.action_settings),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "Customize reading mode, display and translation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            footer = {
                Button(
                    onClick = {
                        onDismissRequest()
                        onShowMenus()
                    },
                    shape = SanctuaryShapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(MR.strings.action_done))
                }
            },
        ) { page ->
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window

            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage == 1) {
                    window?.setDimAmount(0f)
                    onHideMenus()
                } else {
                    window?.setDimAmount(0.5f)
                    onShowMenus()
                }
            }

            if (page == 2) {
                androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 24.dp)) {
                    translationContent()
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(vertical = TabbedDialogPaddings.Vertical)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (page) {
                        0 -> ReadingModePage(screenModel)
                        1 -> {
                            ReaderThemeSettings(screenModel)
                            ColorFilterPage(screenModel)
                            var advanced by remember { mutableStateOf(false) }
                            TextButton(onClick = { advanced = !advanced }) {
                                Text(stringResource(MR.strings.pref_category_advanced))
                            }
                            if (advanced) GeneralPage(screenModel)
                        }
                    }
                }
            }
        }
    }
}
