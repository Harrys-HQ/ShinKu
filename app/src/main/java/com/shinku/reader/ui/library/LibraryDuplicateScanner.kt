package com.shinku.reader.ui.library

import androidx.compose.runtime.Immutable
import com.shinku.reader.domain.library.model.LibraryManga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class DuplicateMangaGroup(
    val normalizedTitle: String,
    val displayTitle: String,
    val entries: ImmutableList<LibraryManga>,
    val mergeGroupIds: Set<Long>,
    val groupedMemberIds: Set<Long> = emptySet(),
)

object LibraryDuplicateScanner {
    private val multiWhitespaceRegex = Regex("\\s+")

    fun normalizeTitle(title: String): String {
        return title.trim().replace(multiWhitespaceRegex, " ").lowercase()
    }

    fun pairKey(id1: Long, id2: Long): String {
        return "${minOf(id1, id2)},${maxOf(id1, id2)}"
    }

    fun findDuplicateGroups(
        libraryManga: List<LibraryManga>,
        memberships: Map<Long, Long>,
        manualUnmergePairs: Set<String> = emptySet(),
    ): List<DuplicateMangaGroup> {
        return scanCandidates(libraryManga, memberships)
            .filterNot { isGroupFullyIgnored(it.entries, memberships, manualUnmergePairs) }
    }

    fun countIgnoredDuplicateGroups(
        libraryManga: List<LibraryManga>,
        memberships: Map<Long, Long>,
        manualUnmergePairs: Set<String>,
    ): Int {
        if (manualUnmergePairs.isEmpty()) return 0
        return scanCandidates(libraryManga, memberships)
            .count { isGroupFullyIgnored(it.entries, memberships, manualUnmergePairs) }
    }

    private fun scanCandidates(
        libraryManga: List<LibraryManga>,
        memberships: Map<Long, Long>,
    ): List<DuplicateMangaGroup> {
        val grouped = libraryManga
            .distinctBy { it.id }
            .groupBy { normalizeTitle(it.manga.title) }
            .filter { (key, entries) -> key.isNotEmpty() && entries.size > 1 }

        return grouped.mapNotNull { (normalizedTitle, entries) ->
            val sortedEntries = entries.sortedWith(
                compareByDescending<LibraryManga> { it.totalChapters }
                    .thenBy { it.manga.source }
                    .thenBy { it.id },
            )
            val entryGroupIds = sortedEntries.mapNotNull { memberships[it.id] }.toSet()
            val membersWithGroup = sortedEntries.filter { it.id in memberships }.map { it.id }.toSet()
            val allInSameGroup = entryGroupIds.size == 1 && membersWithGroup.size == sortedEntries.size
            if (allInSameGroup) return@mapNotNull null

            DuplicateMangaGroup(
                normalizedTitle = normalizedTitle,
                displayTitle = sortedEntries.first().manga.title,
                entries = sortedEntries.toImmutableList(),
                mergeGroupIds = entryGroupIds,
                groupedMemberIds = membersWithGroup,
            )
        }.sortedBy { it.displayTitle.lowercase() }
    }

    private fun isGroupFullyIgnored(
        entries: List<LibraryManga>,
        memberships: Map<Long, Long>,
        manualUnmergePairs: Set<String>,
    ): Boolean {
        if (manualUnmergePairs.isEmpty()) return false
        for (i in 0 until entries.size) {
            for (j in i + 1 until entries.size) {
                val first = entries[i]
                val second = entries[j]
                val group1 = memberships[first.id]
                val group2 = memberships[second.id]
                val sameGroup = group1 != null && group1 == group2
                if (!sameGroup && pairKey(first.id, second.id) !in manualUnmergePairs) {
                    return false
                }
            }
        }
        return true
    }
}
