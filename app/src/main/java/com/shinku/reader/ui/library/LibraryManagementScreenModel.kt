package com.shinku.reader.ui.library

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.library.model.LibraryManga
import com.shinku.reader.domain.manga.interactor.DeleteMergeGroupMember
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.domain.manga.interactor.GetMergeGroupMemberships
import com.shinku.reader.domain.manga.interactor.GroupManga
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.exh.source.ShinKuPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Immutable
data class ActiveMergeGroupUi(
    val groupId: Long,
    val members: ImmutableList<LibraryManga>,
)

class LibraryManagementScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getMergeGroupMemberships: GetMergeGroupMemberships = Injekt.get(),
    private val groupManga: GroupManga = Injekt.get(),
    private val deleteMergeGroupMember: DeleteMergeGroupMember = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val shinkuPreferences: ShinKuPreferences = Injekt.get(),
) : StateScreenModel<LibraryManagementScreenModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            combine(
                getLibraryManga.subscribe(),
                getMergeGroupMemberships.subscribe(),
                shinkuPreferences.mangaManualUnmerges().changes(),
            ) { libraryList, memberships, manualUnmerges ->
                val uniqueLibrary = libraryList.distinctBy { it.id }
                val libraryById = uniqueLibrary.associateBy { it.id }

                val duplicateGroups = LibraryDuplicateScanner
                    .findDuplicateGroups(uniqueLibrary, memberships, manualUnmerges)
                    .toImmutableList()
                val ignoredDuplicateCount = LibraryDuplicateScanner
                    .countIgnoredDuplicateGroups(uniqueLibrary, memberships, manualUnmerges)

                val activeGroups = memberships.entries
                    .groupBy({ it.value }, { it.key })
                    .mapNotNull { (groupId, memberIds) ->
                        val members = memberIds.mapNotNull { libraryById[it] }
                            .sortedByDescending { it.totalChapters }
                            .toImmutableList()
                        if (members.size < 2) null else ActiveMergeGroupUi(groupId, members)
                    }
                    .sortedBy { it.members.firstOrNull()?.manga?.title?.lowercase().orEmpty() }
                    .toImmutableList()

                val unavailableSourceEntries = uniqueLibrary
                    .filter { sourceManager.get(it.manga.source) == null }
                    .sortedBy { it.manga.title.lowercase() }
                    .toImmutableList()

                Triple(
                    Pair(duplicateGroups, ignoredDuplicateCount),
                    activeGroups,
                    unavailableSourceEntries,
                )
            }.collectLatest { (dupPair, activeGroups, unavailable) ->
                mutableState.update { current ->
                    current.copy(
                        isLoading = false,
                        duplicateGroups = dupPair.first,
                        ignoredDuplicateCount = dupPair.second,
                        activeMergeGroups = activeGroups,
                        unavailableSourceEntries = unavailable,
                    )
                }
            }
        }
    }

    fun getSourceName(sourceId: Long): String {
        return sourceManager.getOrStub(sourceId).name
    }

    fun groupDuplicates(group: DuplicateMangaGroup) {
        val entries = group.entries
        if (entries.size < 2) return
        val primary = entries.first()
        screenModelScope.launchIO {
            entries.drop(1).forEach { candidate ->
                groupManga.await(primary.id, candidate.id)
            }
        }
    }

    fun ignoreDuplicateGroup(group: DuplicateMangaGroup) {
        val ids = group.entries.map { it.id }
        if (ids.size < 2) return
        screenModelScope.launchIO {
            shinkuPreferences.addManualUnmerges(ids)
        }
    }

    fun requestRemoveFromMergeGroup(manga: LibraryManga) {
        mutableState.update { it.copy(dialog = Dialog.ConfirmRemoveMember(manga)) }
    }

    fun confirmRemoveFromMergeGroup(mangaId: Long) {
        dismissDialog()
        screenModelScope.launchIO {
            deleteMergeGroupMember.await(mangaId)
        }
    }

    fun requestResetIgnored() {
        val count = state.value.ignoredDuplicateCount
        if (count <= 0) return
        mutableState.update { it.copy(dialog = Dialog.ConfirmResetIgnored(count)) }
    }

    fun confirmResetIgnored() {
        dismissDialog()
        screenModelScope.launchIO {
            shinkuPreferences.clearManualUnmerges()
        }
    }

    fun dismissDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    sealed interface Dialog {
        data class ConfirmRemoveMember(val manga: LibraryManga) : Dialog
        data class ConfirmResetIgnored(val count: Int) : Dialog
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val duplicateGroups: ImmutableList<DuplicateMangaGroup> = persistentListOf(),
        val ignoredDuplicateCount: Int = 0,
        val activeMergeGroups: ImmutableList<ActiveMergeGroupUi> = persistentListOf(),
        val unavailableSourceEntries: ImmutableList<LibraryManga> = persistentListOf(),
        val dialog: Dialog? = null,
    )
}
