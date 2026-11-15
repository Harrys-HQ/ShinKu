package com.shinku.reader.presentation.widget.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.shinku.reader.core.common.Constants
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.widget.util.calculateRowAndColumnCount
import kotlinx.collections.immutable.ImmutableList

@Composable
fun UpdatesWidget(
    data: ImmutableList<UpdatesWidgetItem>?,
    contentColor: ColorProvider,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        if (data == null) {
            CircularProgressIndicator(color = contentColor)
        } else if (data.isEmpty()) {
            val intent = Intent(LocalContext.current, Class.forName(Constants.MAIN_ACTIVITY)).apply {
                action = Constants.SHORTCUT_UPDATES
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(MR.strings.information_no_recent),
                    style = TextStyle(color = contentColor),
                )
                Box(GlanceModifier.fillMaxWidth().height(48.dp).clickable(actionStartActivity(intent)), contentAlignment = Alignment.Center) {
                    Text(stringResource(MR.strings.widget_open_updates), style = TextStyle(color = contentColor))
                }
            }
        } else {
            val (rowCount, columnCount) = LocalSize.current.calculateRowAndColumnCount(topPadding, bottomPadding)
            Column(
                modifier = GlanceModifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                (0..<rowCount).forEach { i ->
                    val coverRow = (0..<columnCount).mapNotNull { j ->
                        data.getOrNull(j + (i * columnCount))
                    }
                    if (coverRow.isNotEmpty()) {
                        Row(
                            modifier = GlanceModifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            coverRow.forEach { item ->
                                val mangaId = item.mangaId
                                Box(
                                    modifier = GlanceModifier
                                        .padding(horizontal = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val intent = Intent(
                                        LocalContext.current,
                                        Class.forName(Constants.MAIN_ACTIVITY),
                                    ).apply {
                                        action = Constants.SHORTCUT_MANGA
                                        putExtra(Constants.MANGA_EXTRA, mangaId)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                                        // https://issuetracker.google.com/issues/238793260
                                        addCategory(mangaId.toString())
                                    }
                                    UpdatesMangaCover(
                                        cover = item.cover,
                                        title = item.title,
                                        modifier = GlanceModifier.clickable(actionStartActivity(intent)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
