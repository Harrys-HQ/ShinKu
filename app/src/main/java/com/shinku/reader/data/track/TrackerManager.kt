package com.shinku.reader.data.track

import com.shinku.reader.data.track.anilist.Anilist
import com.shinku.reader.data.track.bangumi.Bangumi
import com.shinku.reader.data.track.kavita.Kavita
import com.shinku.reader.data.track.kitsu.Kitsu
import com.shinku.reader.data.track.komga.Komga
import com.shinku.reader.data.track.mangaupdates.MangaUpdates
import com.shinku.reader.data.track.mdlist.MdList
import com.shinku.reader.data.track.myanimelist.MyAnimeList
import com.shinku.reader.data.track.shikimori.Shikimori
import com.shinku.reader.data.track.suwayomi.Suwayomi
import kotlinx.coroutines.flow.combine

class TrackerManager {

    companion object {
        const val ANILIST = 2L
        const val KITSU = 3L
        const val KAVITA = 8L

        // SY --> Mangadex from Neko
        const val MDLIST = 60L
        // SY <--
    }

    val mdList = MdList(MDLIST)

    val myAnimeList = MyAnimeList(1L)
    val aniList = Anilist(ANILIST)
    val kitsu = Kitsu(KITSU)
    val shikimori = Shikimori(4L)
    val bangumi = Bangumi(5L)
    val komga = Komga(6L)
    val mangaUpdates = MangaUpdates(7L)
    val kavita = Kavita(KAVITA)
    val suwayomi = Suwayomi(9L)

    val trackers =
        listOf(mdList, myAnimeList, aniList, kitsu, shikimori, bangumi, komga, mangaUpdates, kavita, suwayomi)

    fun loggedInTrackers() = trackers.filter { it.isLoggedIn }

    fun loggedInTrackersFlow() = combine(trackers.map { it.isLoggedInFlow }) {
        it.mapIndexedNotNull { index, isLoggedIn ->
            if (isLoggedIn) trackers[index] else null
        }
    }

    fun get(id: Long) = trackers.find { it.id == id }

    fun getAll(ids: Set<Long>) = trackers.filter { it.id in ids }
}
