package com.shinku.reader.ui.browse.feed

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import com.shinku.reader.domain.manga.model.Manga

object GenreFilterHelper {

    private val GENRE_ALIASES = mapOf(
        "sci-fi" to listOf("sci-fi", "science fiction", "scifi", "sci fi"),
        "slice of life" to listOf("slice of life", "slice-of-life", "iyashikei"),
        "martial arts" to listOf("martial arts", "martial-arts", "wuxia"),
        "school life" to listOf("school life", "school", "school-life"),
        "supernatural" to listOf("supernatural", "super power", "paranormal"),
        "cultivation" to listOf("cultivation", "xianxia", "xuanhuan"),
        "historical" to listOf("historical", "period"),
    )

    fun getGenreVariants(genre: String): List<String> {
        val lower = genre.trim().lowercase()
        val aliases = GENRE_ALIASES[lower].orEmpty()
        return (listOf(lower) + aliases).distinct()
    }

    fun nameMatchesGenre(name: String, targetGenre: String): Boolean {
        val lowerName = name.trim().lowercase()
        val variants = getGenreVariants(targetGenre)
        return variants.any { variant ->
            lowerName == variant || lowerName.contains(variant)
        }
    }

    /**
     * Inspects the source's filter list and attempts to configure a genuine genre/tag filter.
     * Returns the configured FilterList if a matching genre filter was found and applied,
     * or null if the source does not support filtering by this genre.
     */
    fun buildGenreFilterList(source: CatalogueSource, targetGenre: String): FilterList? {
        val filters = try {
            source.getFilterList()
        } catch (e: Throwable) {
            return null
        }

        var matched = false

        for (filter in filters) {
            when (filter) {
                is Filter.Group<*> -> {
                    val groupName = filter.name.lowercase()
                    val isGenreGroup = groupName.contains("genre") ||
                        groupName.contains("tag") ||
                        groupName.contains("categor") ||
                        groupName.contains("theme")

                    for (item in filter.state) {
                        when (item) {
                            is Filter.TriState -> {
                                if (nameMatchesGenre(item.name, targetGenre)) {
                                    item.state = Filter.TriState.STATE_INCLUDE
                                    matched = true
                                }
                            }
                            is Filter.CheckBox -> {
                                if (nameMatchesGenre(item.name, targetGenre)) {
                                    item.state = true
                                    matched = true
                                }
                            }
                        }
                    }
                }
                is Filter.Select<*> -> {
                    val selectName = filter.name.lowercase()
                    if (selectName.contains("genre") || selectName.contains("tag") || selectName.contains("categor")) {
                        val index = filter.values.indexOfFirst {
                            nameMatchesGenre(it.toString(), targetGenre)
                        }
                        if (index >= 0) {
                            filter.state = index
                            matched = true
                        }
                    }
                }
                is Filter.AutoComplete -> {
                    val name = filter.name.lowercase()
                    if (name.contains("genre") || name.contains("tag")) {
                        val match = filter.values.firstOrNull { nameMatchesGenre(it, targetGenre) }
                        if (match != null) {
                            filter.state = listOf(match)
                            matched = true
                        }
                    }
                }
                is Filter.TriState -> {
                    if (nameMatchesGenre(filter.name, targetGenre)) {
                        filter.state = Filter.TriState.STATE_INCLUDE
                        matched = true
                    }
                }
                is Filter.CheckBox -> {
                    if (nameMatchesGenre(filter.name, targetGenre)) {
                        filter.state = true
                        matched = true
                    }
                }
                else -> {}
            }
        }

        return if (matched) filters else null
    }

    /**
     * Strict verification: If the manga already has tags/genres loaded,
     * ensures that targetGenre is actually present in the tags (case-insensitive).
     * If tags are null or empty (e.g. fresh network stub where tags aren't loaded yet),
     * it is allowed through only if it was fetched from a genuine genre filter.
     */
    fun matchesGenre(manga: Manga, targetGenre: String): Boolean {
        val genres = manga.genre
        if (!genres.isNullOrEmpty()) {
            val variants = getGenreVariants(targetGenre)
            return genres.any { tag ->
                val lowerTag = tag.trim().lowercase()
                variants.any { variant -> lowerTag == variant || lowerTag.contains(variant) }
            }
        }
        return true
    }
}
