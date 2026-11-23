package com.shinku.reader.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.shinku.reader.R
import com.shinku.reader.core.preference.asState
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.TabbedScreen
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.util.Tab
import com.shinku.reader.ui.browse.extension.ExtensionsScreenModel
import com.shinku.reader.ui.browse.extension.extensionsTab
import com.shinku.reader.ui.browse.feed.feedTab
import com.shinku.reader.ui.browse.migration.sources.migrateSourceTab
import com.shinku.reader.ui.browse.source.globalsearch.GlobalSearchScreen
import com.shinku.reader.ui.browse.source.sourcesTab
import com.shinku.reader.ui.main.MainActivity
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalSearchScreen())
    }

    private enum class Section { SOURCES, EXTENSIONS }
    private val switchToTabChannel = Channel<Section>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToTabChannel.trySend(Section.EXTENSIONS)
    }

    fun showSources() {
        switchToTabChannel.trySend(Section.SOURCES)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        // SY -->
        val hideFeedTab by remember { Injekt.get<UiPreferences>().hideFeedTab().asState(scope) }
        val feedTabInFront by remember { Injekt.get<UiPreferences>().feedTabInFront().asState(scope) }
        // SY <--

        // Hoisted for extensions tab's search bar
        val extensionsScreenModel = rememberScreenModel { ExtensionsScreenModel() }
        val extensionsState by extensionsScreenModel.state.collectAsState()

        // SY -->
        val tabOrder = remember(hideFeedTab, feedTabInFront) {
            computeBrowseTabOrder(hideFeedTab = hideFeedTab, feedTabInFront = feedTabInFront)
        }
        val tabs = tabOrder.map { kind ->
            when (kind) {
                BrowseTabKind.FEED -> feedTab()
                BrowseTabKind.SOURCES -> sourcesTab()
                BrowseTabKind.EXTENSIONS -> extensionsTab(extensionsScreenModel)
                BrowseTabKind.MIGRATE -> migrateSourceTab()
            }
        }.toImmutableList()
        // SY <--

        val state = rememberPagerState { tabs.size }

        TabbedScreen(
            titleRes = MR.strings.browse,
            tabs = tabs,
            state = state,
            searchQuery = extensionsState.searchQuery,
            onChangeSearchQuery = extensionsScreenModel::search,
        )
        LaunchedEffect(tabOrder) {
            switchToTabChannel.receiveAsFlow().collectLatest { section ->
                val targetKind = when (section) {
                    Section.SOURCES -> BrowseTabKind.SOURCES
                    Section.EXTENSIONS -> BrowseTabKind.EXTENSIONS
                }
                state.scrollToPage(indexForBrowseTab(tabOrder, targetKind))
            }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
