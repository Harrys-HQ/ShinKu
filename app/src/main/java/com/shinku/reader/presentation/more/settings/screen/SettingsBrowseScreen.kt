package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.core.preference.asState
import com.shinku.reader.domain.extensionrepo.interactor.GetExtensionRepoCount
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.i18n.pluralStringResource
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.presentation.more.settings.Preference
import com.shinku.reader.presentation.more.settings.screen.browse.ExtensionReposScreen
import com.shinku.reader.ui.category.sources.SourceCategoryScreen
import com.shinku.reader.util.system.AuthenticatorUtil.authenticate
import kotlinx.collections.immutable.persistentListOf
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsBrowseScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val getExtensionRepoCount = remember { Injekt.get<GetExtensionRepoCount>() }

        val reposCount by getExtensionRepoCount.subscribe().collectAsState(0)

        return listOf(
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(MR.strings.browse),
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
                                    imageVector = Icons.Outlined.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(MR.strings.browse),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(MR.strings.pref_browse_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_sources),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.hideInLibraryItems(),
                        title = stringResource(MR.strings.pref_hide_in_library_items),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.showNsfwSource(),
                        title = stringResource(MR.strings.pref_category_nsfw_content),
                        subtitle = stringResource(MR.strings.pref_show_nsfw_source),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.label_extension_repos),
                        subtitle = pluralStringResource(MR.plurals.num_repos, reposCount, reposCount),
                        onClick = {
                            navigator.push(ExtensionReposScreen())
                        },
                    ),
                ),
            ),
        )
    }
}
