package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.domain.category.interactor.GetCategories
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.download.service.DownloadPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.category.visualName
import com.shinku.reader.presentation.core.i18n.pluralStringResource
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.presentation.more.settings.Preference
import com.shinku.reader.presentation.more.settings.widget.TriStateListDialog
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsDownloadScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_downloads

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        val downloadPreferences = remember { Injekt.get<DownloadPreferences>() }
        val parallelSourceLimit by downloadPreferences.parallelSourceLimit().collectAsState()
        val parallelPageLimit by downloadPreferences.parallelPageLimit().collectAsState()
        val downloadThreadsPerPage by downloadPreferences.downloadThreadsPerPage().collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        return listOf(
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(MR.strings.pref_category_downloads),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = SanctuaryShapes.deckCard,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                    border = SanctuaryBrushes.subtleBorder(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            shape = SanctuaryShapes.medium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.size(42.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.GetApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(MR.strings.pref_category_downloads),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(MR.strings.pref_downloads_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.label_data_storage),
                subtitle = stringResource(MR.strings.download_storage_settings_summary),
                onClick = { navigator.push(SettingsDataScreen) },
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_downloads),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = downloadPreferences.downloadOnlyOverWifi(),
                        title = stringResource(MR.strings.connected_to_wifi),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = downloadPreferences.saveChaptersAsCBZ(),
                        title = stringResource(MR.strings.save_chapter_as_cbz),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = downloadPreferences.splitTallImages(),
                        title = stringResource(MR.strings.split_tall_images),
                        subtitle = stringResource(MR.strings.split_tall_images_summary),
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = parallelSourceLimit,
                        valueRange = 1..10,
                        title = stringResource(MR.strings.pref_download_concurrent_sources),
                        onValueChanged = { downloadPreferences.parallelSourceLimit().set(it) },
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = parallelPageLimit,
                        valueRange = 1..15,
                        title = stringResource(MR.strings.pref_download_concurrent_pages),
                        subtitle = stringResource(MR.strings.pref_download_concurrent_pages_summary),
                        onValueChanged = { downloadPreferences.parallelPageLimit().set(it) },
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = downloadThreadsPerPage,
                        valueRange = 1..8,
                        title = "Download threads per page",
                        subtitle = "Number of parallel connections for a single image",
                        onValueChanged = { downloadPreferences.downloadThreadsPerPage().set(it) },
                    ),
                ),
            ),
            getDeleteChaptersGroup(
                downloadPreferences = downloadPreferences,
                categories = allCategories,
            ),
            getAutoDownloadGroup(
                downloadPreferences = downloadPreferences,
                allCategories = allCategories,
            ),
            getDownloadAheadGroup(downloadPreferences = downloadPreferences),
        )
    }

    @Composable
    private fun getDeleteChaptersGroup(
        downloadPreferences: DownloadPreferences,
        categories: List<Category>,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_delete_chapters),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.removeAfterMarkedAsRead(),
                    title = stringResource(MR.strings.pref_remove_after_marked_as_read),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.removeAfterReadSlots(),
                    entries = persistentMapOf(
                        -1 to stringResource(MR.strings.disabled),
                        0 to stringResource(MR.strings.last_read_chapter),
                        1 to stringResource(MR.strings.second_to_last),
                        2 to stringResource(MR.strings.third_to_last),
                        3 to stringResource(MR.strings.fourth_to_last),
                        4 to stringResource(MR.strings.fifth_to_last),
                    ),
                    title = stringResource(MR.strings.pref_remove_after_read),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.removeBookmarkedChapters(),
                    title = stringResource(MR.strings.pref_remove_bookmarked_chapters),
                ),
                getExcludedCategoriesPreference(
                    downloadPreferences = downloadPreferences,
                    categories = { categories },
                ),
            ),
        )
    }

    @Composable
    private fun getExcludedCategoriesPreference(
        downloadPreferences: DownloadPreferences,
        categories: () -> List<Category>,
    ): Preference.PreferenceItem.MultiSelectListPreference {
        return Preference.PreferenceItem.MultiSelectListPreference(
            preference = downloadPreferences.removeExcludeCategories(),
            entries = categories()
                .associate { it.id.toString() to it.visualName }
                .toImmutableMap(),
            title = stringResource(MR.strings.pref_remove_exclude_categories),
        )
    }

    @Composable
    private fun getAutoDownloadGroup(
        downloadPreferences: DownloadPreferences,
        allCategories: List<Category>,
    ): Preference.PreferenceGroup {
        val downloadNewChaptersPref = downloadPreferences.downloadNewChapters()
        val downloadNewUnreadChaptersOnlyPref = downloadPreferences.downloadNewUnreadChaptersOnly()
        val downloadNewChapterCategoriesPref = downloadPreferences.downloadNewChapterCategories()
        val downloadNewChapterCategoriesExcludePref = downloadPreferences.downloadNewChapterCategoriesExclude()

        val downloadNewChapters by downloadNewChaptersPref.collectAsState()

        val included by downloadNewChapterCategoriesPref.collectAsState()
        val excluded by downloadNewChapterCategoriesExcludePref.collectAsState()
        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.categories),
                message = stringResource(MR.strings.pref_download_new_categories_details),
                items = allCategories,
                initialChecked = included.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                initialInversed = excluded.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    downloadNewChapterCategoriesPref.set(newIncluded.fastMap { it.id.toString() }.toSet())
                    downloadNewChapterCategoriesExcludePref.set(newExcluded.fastMap { it.id.toString() }.toSet())
                    showDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_auto_download),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadNewChaptersPref,
                    title = stringResource(MR.strings.pref_download_new),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadNewUnreadChaptersOnlyPref,
                    title = stringResource(MR.strings.pref_download_new_unread_chapters_only),
                    enabled = downloadNewChapters,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allCategories,
                        included = included,
                        excluded = excluded,
                    ),
                    enabled = downloadNewChapters,
                    onClick = { showDialog = true },
                ),
            ),
        )
    }

    @Composable
    private fun getDownloadAheadGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.download_ahead),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.autoDownloadWhileReading(),
                    entries = listOf(0, 2, 3, 5, 10)
                        .associateWith {
                            if (it == 0) {
                                stringResource(MR.strings.disabled)
                            } else {
                                pluralStringResource(MR.plurals.next_unread_chapters, count = it, it)
                            }
                        }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.auto_download_while_reading),
                ),
                Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.download_ahead_info)),
            ),
        )
    }
}
