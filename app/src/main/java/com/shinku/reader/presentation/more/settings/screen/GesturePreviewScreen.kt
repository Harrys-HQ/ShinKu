package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.ui.reader.setting.ReaderPreferences
import com.shinku.reader.ui.reader.viewer.ViewerNavigation
import com.shinku.reader.ui.reader.viewer.navigation.DisabledNavigation
import com.shinku.reader.ui.reader.viewer.navigation.EdgeNavigation
import com.shinku.reader.ui.reader.viewer.navigation.KindlishNavigation
import com.shinku.reader.ui.reader.viewer.navigation.LNavigation
import com.shinku.reader.ui.reader.viewer.navigation.RightAndLeftNavigation
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GesturePreviewScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val readerPreferences = remember { Injekt.get<ReaderPreferences>() }
        val navigator = LocalNavigator.currentOrThrow

        val navMode by readerPreferences.navigationModePager().collectAsState()
        val invertMode by readerPreferences.pagerNavInverted().collectAsState()

        val navigation = remember(navMode, invertMode) {
            val nav: ViewerNavigation = when (navMode) {
                0 -> LNavigation() // Default
                1 -> LNavigation()
                2 -> KindlishNavigation()
                3 -> EdgeNavigation()
                4 -> RightAndLeftNavigation()
                5 -> DisabledNavigation()
                else -> LNavigation()
            }
            nav.invertMode = invertMode
            nav
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(MR.strings.pref_viewer_nav)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                val textMeasurer = rememberTextMeasurer()
                val regions = navigation.getRegions()
                val regionLabels = regions.map { stringResource(it.type.nameRes) }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    regions.forEachIndexed { index, region ->
                        val rect = region.rectF
                        val left = rect.left * w
                        val top = rect.top * h
                        val width = (rect.right - rect.left) * w
                        val height = (rect.bottom - rect.top) * h

                        val regionColor = Color(region.type.color)

                        drawRect(
                            color = regionColor,
                            topLeft = Offset(left, top),
                            size = Size(width, height)
                        )

                        // Draw Text
                        val textLayoutResult = textMeasurer.measure(
                            text = regionLabels[index],
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        val textSize = textLayoutResult.size.toSize()

                        drawText(
                            textLayoutResult = textLayoutResult,
                            color = Color.Black,
                            topLeft = Offset(
                                left + width / 2 - textSize.width / 2,
                                top + height / 2 - textSize.height / 2
                            )
                        )
                        
                        drawText(
                            textLayoutResult = textLayoutResult,
                            color = Color.White,
                            topLeft = Offset(
                                left + width / 2 - textSize.width / 2 + 1,
                                top + height / 2 - textSize.height / 2 + 1
                            )
                        )
                    }
                }
            }
        }
    }
}
