package com.shinku.reader.domain.manga.model

/**
 * One item per merge group: the first item whose manga belongs to a group keeps it, later members of
 * that group drop out, and an ungrouped manga always stays.
 *
 * A single definition of "count a merged series once", so the library grid, the badges, the updates
 * screen and the feed rows cannot disagree about the total. Order decides the survivor, so hand in a
 * deterministically ordered list (the group's `source_priority`, then id).
 */
fun <T, K> List<T>.dedupeByMergeGroup(membership: Map<K, Long>, id: (T) -> K): List<T> {
    if (membership.isEmpty()) return this
    val seenGroups = HashSet<Long>()
    return filter { item -> membership[id(item)]?.let(seenGroups::add) ?: true }
}
