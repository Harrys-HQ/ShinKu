package com.shinku.reader.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.icerock.moko.resources.StringResource
import com.shinku.reader.domain.manga.model.readingMode
import com.shinku.reader.presentation.components.AdaptiveSheet
import com.shinku.reader.presentation.reader.components.ModeSelectionDialog
import com.shinku.reader.presentation.theme.TachiyomiPreviewTheme
import com.shinku.reader.ui.reader.setting.ReaderSettingsScreenModel
import com.shinku.reader.ui.reader.setting.ReadingMode
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SettingsIconGrid
import com.shinku.reader.presentation.core.components.material.IconToggleButton
import com.shinku.reader.presentation.core.i18n.stringResource

private val ReadingModesWithoutDefault = ReadingMode.entries - ReadingMode.DEFAULT

@Composable
fun ReadingModeSelectDialog(
    onDismissRequest: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
    onChange: (StringResource) -> Unit,
) {
    val manga by screenModel.mangaFlow.collectAsState()
    val readingMode = remember(manga) { ReadingMode.fromPreference(manga?.readingMode?.toInt()) }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        DialogContent(
            readingMode = readingMode,
            onChangeReadingMode = {
                screenModel.onChangeReadingMode(it)
                onChange(it.stringRes)
                onDismissRequest()
            },
        )
    }
}

@Composable
private fun DialogContent(
    readingMode: ReadingMode,
    onChangeReadingMode: (ReadingMode) -> Unit,
) {
    var selected by remember { mutableStateOf(readingMode) }

    ModeSelectionDialog(
        onUseDefault = { onChangeReadingMode(ReadingMode.DEFAULT) }.takeIf { readingMode != ReadingMode.DEFAULT },
        onApply = { onChangeReadingMode(selected) },
    ) {
        SettingsIconGrid(MR.strings.pref_category_reading_mode) {
            items(ReadingModesWithoutDefault) { mode ->
                IconToggleButton(
                    checked = mode == selected,
                    onCheckedChange = {
                        selected = mode
                    },
                    modifier = Modifier.fillMaxWidth(),
                    imageVector = ImageVector.vectorResource(mode.iconRes),
                    title = stringResource(mode.stringRes),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DialogContentPreview() {
    TachiyomiPreviewTheme {
        Surface {
            Column {
                DialogContent(
                    readingMode = ReadingMode.DEFAULT,
                    onChangeReadingMode = {},
                )

                DialogContent(
                    readingMode = ReadingMode.LEFT_TO_RIGHT,
                    onChangeReadingMode = {},
                )
            }
        }
    }
}
