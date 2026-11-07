package com.shinku.reader.ui.download

import android.view.LayoutInflater
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.core.common.util.lang.launchUI
import com.shinku.reader.data.download.model.Download
import com.shinku.reader.databinding.DownloadListBinding
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.components.DropdownMenu
import com.shinku.reader.presentation.components.NestedMenuItem
import com.shinku.reader.presentation.core.components.Pill
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.util.Screen
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

object DownloadQueueScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { DownloadQueueScreenModel() }
        val downloadList by screenModel.state.collectAsState()
        val downloadCount by remember {
            derivedStateOf { downloadList.sumOf { it.subItems.size } }
        }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            topBar = {
                AppBar(
                    titleContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(MR.strings.label_download_queue),
                                maxLines = 1,
                                modifier = Modifier.weight(1f, false),
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (downloadCount > 0) {
                                Pill(
                                    text = "$downloadCount",
                                    modifier = Modifier.padding(start = 6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    },
                    navigateUp = navigator::pop,
                    actions = {
                        if (downloadList.isNotEmpty()) {
                            var sortExpanded by remember { mutableStateOf(false) }
                            val onDismissRequest = { sortExpanded = false }
                            DropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = onDismissRequest,
                            ) {
                                NestedMenuItem(
                                    text = { Text(text = stringResource(MR.strings.action_order_by_upload_date)) },
                                    children = { closeMenu ->
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(MR.strings.action_newest)) },
                                            onClick = {
                                                screenModel.reorderQueue(
                                                    { it.download.chapter.dateUpload },
                                                    true,
                                                )
                                                closeMenu()
                                                onDismissRequest()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(MR.strings.action_oldest)) },
                                            onClick = {
                                                screenModel.reorderQueue(
                                                    { it.download.chapter.dateUpload },
                                                    false,
                                                )
                                                closeMenu()
                                                onDismissRequest()
                                            },
                                        )
                                    },
                                )
                                NestedMenuItem(
                                    text = { Text(text = stringResource(MR.strings.action_order_by_chapter_number)) },
                                    children = { closeMenu ->
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(MR.strings.action_asc)) },
                                            onClick = {
                                                screenModel.reorderQueue(
                                                    { it.download.chapter.chapterNumber },
                                                    false,
                                                )
                                                closeMenu()
                                                onDismissRequest()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(MR.strings.action_desc)) },
                                            onClick = {
                                                screenModel.reorderQueue(
                                                    { it.download.chapter.chapterNumber },
                                                    true,
                                                )
                                                closeMenu()
                                                onDismissRequest()
                                            },
                                        )
                                    },
                                )
                            }

                            AppBarActions(
                                persistentListOf(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_sort),
                                        icon = Icons.AutoMirrored.Outlined.Sort,
                                        onClick = { sortExpanded = true },
                                    ),
                                    AppBar.OverflowAction(
                                        title = stringResource(MR.strings.action_cancel_all),
                                        onClick = { screenModel.clearQueue() },
                                    ),
                                ),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            if (downloadList.isEmpty()) {
                EmptyScreen(
                    stringRes = MR.strings.information_no_downloads,
                    modifier = Modifier.padding(contentPadding),
                )
                return@Scaffold
            }

            val isRunning by screenModel.isDownloaderRunning.collectAsState()
            val bottom = with(LocalDensity.current) { 8.dp.toPx().roundToInt() }
            val downloadingCount = remember(downloadList) {
                downloadList.sumOf { header ->
                    header.subItems.count { it.download.status == Download.State.DOWNLOADING }
                }
            }
            val queuedCount = remember(downloadList, downloadingCount) {
                (downloadCount - downloadingCount).coerceAtLeast(0)
            }
            val aggregateProgress = remember(downloadList) {
                val items = downloadList.flatMap { it.subItems }
                if (items.isEmpty()) 0f else items.map { it.download.progress / 100f }.average().toFloat().coerceIn(0f, 1f)
            }

            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = SanctuaryShapes.deckCard,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                    border = SanctuaryBrushes.subtleBorder(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = SanctuaryShapes.medium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(
                                        if (isRunning) MR.strings.ext_downloading else MR.strings.download_notifier_download_paused,
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                )
                                Text(
                                    text = "$downloadingCount downloading · $queuedCount queued",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { aggregateProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(SanctuaryShapes.pill),
                        )
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = SanctuaryShapes.medium,
                            onClick = {
                                if (isRunning) screenModel.pauseDownloads() else screenModel.startDownloads()
                            },
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(
                                    if (isRunning) MR.strings.action_pause_all_downloads else MR.strings.action_resume_all_downloads,
                                ),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                AndroidView(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    factory = { context ->
                        screenModel.controllerBinding = DownloadListBinding.inflate(LayoutInflater.from(context))
                        screenModel.adapter = DownloadAdapter(screenModel.listener)
                        screenModel.controllerBinding.root.adapter = screenModel.adapter
                        screenModel.adapter?.isHandleDragEnabled = true
                        screenModel.controllerBinding.root.layoutManager = LinearLayoutManager(context)

                        ViewCompat.setNestedScrollingEnabled(screenModel.controllerBinding.root, true)

                        scope.launchUI {
                            screenModel.getDownloadStatusFlow()
                                .collect(screenModel::onStatusChange)
                        }
                        scope.launchUI {
                            screenModel.getDownloadProgressFlow()
                                .collect(screenModel::onUpdateDownloadedPages)
                        }

                        screenModel.controllerBinding.root
                    },
                    update = {
                        screenModel.controllerBinding.root
                            .updatePadding(
                                left = 0,
                                top = 0,
                                right = 0,
                                bottom = bottom,
                            )

                        screenModel.adapter?.updateDataSet(downloadList)
                    },
                )
            }
        }
    }
}
