package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.more.components.CommandGroup
import com.shinku.reader.presentation.more.components.CommandRowItem
import com.shinku.reader.presentation.more.settings.screen.about.AboutScreen
import com.shinku.reader.presentation.more.settings.widget.TextPreferenceWidget
import com.shinku.reader.presentation.util.LocalBackPress
import com.shinku.reader.presentation.util.Screen
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.persistentListOf
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

object SettingsMainScreen : Screen() {

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    private fun getPalerSurface(): Color {
        val surface = MaterialTheme.colorScheme.surface
        val dark = isSystemInDarkTheme()
        return remember(surface, dark) {
            val arr = FloatArray(3)
            ColorUtils.colorToHSL(surface.toArgb(), arr)
            arr[2] = if (dark) {
                arr[2] - 0.05f
            } else {
                arr[2] + 0.02f
            }.coerceIn(0f, 1f)
            Color.hsl(arr[0], arr[1], arr[2])
        }
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val containerColor = if (twoPane) getPalerSurface() else MaterialTheme.colorScheme.surface
        val topBarState = rememberTopAppBarState()

        Scaffold(
            topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState),
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_settings),
                    navigateUp = backPress::invoke,
                    actions = {
                        AppBarActions(
                            persistentListOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_search),
                                    icon = Icons.Outlined.Search,
                                    onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = containerColor,
            content = { contentPadding ->
                val state = rememberLazyListState()
                val visibleItems = items.filter { it.screen !is SearchableSettings || it.screen.isEnabled() }

                if (twoPane) {
                    val indexSelected = visibleItems.indexOfFirst { it.screen::class == navigator.items.first()::class }
                        .also {
                            LaunchedEffect(Unit) {
                                state.animateScrollToItem(it)
                                if (it > 0) {
                                    topBarState.contentOffset = topBarState.heightOffsetLimit
                                }
                            }
                        }

                    LazyColumn(
                        state = state,
                        contentPadding = contentPadding,
                    ) {
                        itemsIndexed(
                            items = visibleItems,
                            key = { _, item -> item.hashCode() },
                        ) { index, item ->
                            val selected = indexSelected == index
                            val modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clip(SanctuaryShapes.hero)
                                .then(
                                    if (selected) {
                                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                    } else {
                                        Modifier
                                    },
                                )
                            val contentColor = if (selected) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                LocalContentColor.current
                            }
                            CompositionLocalProvider(LocalContentColor provides contentColor) {
                                TextPreferenceWidget(
                                    modifier = modifier,
                                    title = stringResource(item.titleRes),
                                    subtitle = item.formatSubtitle(),
                                    icon = item.icon,
                                    onPreferenceClick = { navigator.navigate(item.screen, twoPane) },
                                )
                            }
                        }
                    }
                } else {
                    val experienceGroup = visibleItems.take(3)
                    val contentSyncGroup = visibleItems.drop(3).take(3)
                    val systemGroup = visibleItems.drop(6)

                    LazyColumn(
                        state = state,
                        contentPadding = PaddingValues(
                            top = contentPadding.calculateTopPadding() + 4.dp,
                            bottom = contentPadding.calculateBottomPadding() + 24.dp,
                        ),
                    ) {
                        if (experienceGroup.isNotEmpty()) {
                            item {
                                CommandGroup(title = "Visuals & Reading Experience") {
                                    experienceGroup.forEach { item ->
                                        CommandRowItem(
                                            title = stringResource(item.titleRes),
                                            subtitle = item.formatSubtitle(),
                                            icon = item.icon,
                                            onClick = { navigator.navigate(item.screen, twoPane) },
                                        )
                                    }
                                }
                            }
                        }
                        if (contentSyncGroup.isNotEmpty()) {
                            item {
                                CommandGroup(title = "Content, Sources & Sync") {
                                    contentSyncGroup.forEach { item ->
                                        CommandRowItem(
                                            title = stringResource(item.titleRes),
                                            subtitle = item.formatSubtitle(),
                                            icon = item.icon,
                                            onClick = { navigator.navigate(item.screen, twoPane) },
                                        )
                                    }
                                }
                            }
                        }
                        if (systemGroup.isNotEmpty()) {
                            item {
                                CommandGroup(title = "Storage, Security & System") {
                                    systemGroup.forEach { item ->
                                        CommandRowItem(
                                            title = stringResource(item.titleRes),
                                            subtitle = item.formatSubtitle(),
                                            icon = item.icon,
                                            onClick = { navigator.navigate(item.screen, twoPane) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    private data class Item(
        val titleRes: StringResource,
        val subtitleRes: StringResource,
        val formatSubtitle: @Composable () -> String = { stringResource(subtitleRes) },
        val icon: ImageVector,
        val screen: VoyagerScreen,
    )

    private val items = listOf(
        Item(
            titleRes = MR.strings.pref_category_appearance,
            subtitleRes = MR.strings.pref_appearance_summary,
            icon = Icons.Outlined.Palette,
            screen = SettingsAppearanceScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_library,
            subtitleRes = MR.strings.pref_library_summary,
            icon = Icons.Outlined.CollectionsBookmark,
            screen = SettingsLibraryScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_reader,
            subtitleRes = MR.strings.pref_reader_summary,
            icon = Icons.AutoMirrored.Outlined.ChromeReaderMode,
            screen = SettingsReaderScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_downloads,
            subtitleRes = MR.strings.pref_downloads_summary,
            icon = Icons.Outlined.GetApp,
            screen = SettingsDownloadScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_tracking,
            subtitleRes = MR.strings.pref_tracking_summary,
            icon = Icons.Outlined.Sync,
            screen = SettingsTrackingScreen,
        ),
        Item(
            titleRes = MR.strings.browse,
            subtitleRes = MR.strings.pref_browse_summary,
            icon = Icons.Outlined.Explore,
            screen = SettingsBrowseScreen,
        ),
        Item(
            titleRes = MR.strings.label_data_storage,
            subtitleRes = MR.strings.pref_backup_summary,
            icon = Icons.Outlined.Storage,
            screen = SettingsDataScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_security,
            subtitleRes = MR.strings.pref_security_summary,
            icon = Icons.Outlined.Security,
            screen = SettingsSecurityScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_advanced,
            subtitleRes = MR.strings.pref_advanced_summary,
            icon = Icons.Outlined.Code,
            screen = SettingsAdvancedScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_about,
            subtitleRes = StringResource(0),
            formatSubtitle = {
                "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}"
            },
            icon = Icons.Outlined.Info,
            screen = AboutScreen,
        ),
    )
}
