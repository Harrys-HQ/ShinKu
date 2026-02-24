package com.shinku.reader.test

import dev.icerock.moko.resources.StringResource
import com.shinku.reader.R
import com.shinku.reader.data.track.Tracker
import com.shinku.reader.data.track.model.TrackSearch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.OkHttpClient
import com.shinku.reader.domain.track.model.Track
import com.shinku.reader.i18n.MR

data class DummyTracker(
    override val id: Long,
    override val name: String,
    override val supportsReadingDates: Boolean = false,
    override val supportsPrivateTracking: Boolean = false,
    override val isLoggedIn: Boolean = false,
    override val isLoggedInFlow: Flow<Boolean> = flowOf(false),
    val valLogo: Int = R.drawable.brand_anilist,
    val valStatuses: List<Long> = (1L..6L).toList(),
    val valReadingStatus: Long = 1L,
    val valRereadingStatus: Long = 1L,
    val valCompletionStatus: Long = 2L,
    val valScoreList: ImmutableList<String> = (0..10).map(Int::toString).toImmutableList(),
    val val10PointScore: Double = 5.4,
    val valSearchResults: List<TrackSearch> = listOf(),
) : Tracker {

    override val client: OkHttpClient
        get() = TODO("Not yet implemented")

    override fun getLogo(): Int = valLogo

    override fun getStatusList(): List<Long> = valStatuses

    override fun getStatus(status: Long): StringResource? = when (status) {
        1L -> MR.strings.reading
        2L -> MR.strings.plan_to_read
        3L -> MR.strings.completed
        4L -> MR.strings.on_hold
        5L -> MR.strings.dropped
        6L -> MR.strings.repeating
        else -> null
    }

    override fun getReadingStatus(): Long = valReadingStatus

    override fun getRereadingStatus(): Long = valRereadingStatus

    override fun getCompletionStatus(): Long = valCompletionStatus

    override fun getScoreList(): ImmutableList<String> = valScoreList

    override fun get10PointScore(track: Track): Double = val10PointScore

    override fun indexToScore(index: Int): Double = getScoreList()[index].toDouble()

    override fun displayScore(track: Track): String =
        track.score.toString()

    override suspend fun update(
        track: com.shinku.reader.data.database.models.Track,
        didReadChapter: Boolean,
    ): com.shinku.reader.data.database.models.Track = track

    override suspend fun bind(
        track: com.shinku.reader.data.database.models.Track,
        hasReadChapters: Boolean,
    ): com.shinku.reader.data.database.models.Track = track

    override suspend fun search(query: String): List<TrackSearch> = valSearchResults

    override suspend fun refresh(
        track: com.shinku.reader.data.database.models.Track,
    ): com.shinku.reader.data.database.models.Track = track

    override suspend fun login(username: String, password: String) = Unit

    override fun logout() = Unit

    override fun getUsername(): String = "username"

    override fun getPassword(): String = "passw0rd"

    override fun saveCredentials(username: String, password: String) = Unit

    override suspend fun register(
        item: com.shinku.reader.data.database.models.Track,
        mangaId: Long,
    ) = Unit

    override suspend fun setRemoteStatus(
        track: com.shinku.reader.data.database.models.Track,
        status: Long,
    ) = Unit

    override suspend fun setRemoteLastChapterRead(
        track: com.shinku.reader.data.database.models.Track,
        chapterNumber: Int,
    ) = Unit

    override suspend fun setRemoteScore(
        track: com.shinku.reader.data.database.models.Track,
        scoreString: String,
    ) = Unit

    override suspend fun setRemoteStartDate(
        track: com.shinku.reader.data.database.models.Track,
        epochMillis: Long,
    ) = Unit

    override suspend fun setRemoteFinishDate(
        track: com.shinku.reader.data.database.models.Track,
        epochMillis: Long,
    ) = Unit

    override suspend fun setRemotePrivate(
        track: com.shinku.reader.data.database.models.Track,
        private: Boolean,
    ) = Unit

    override suspend fun getMangaMetadata(
        track: com.shinku.reader.domain.track.model.Track,
    ): com.shinku.reader.data.track.model.TrackMangaMetadata = com.shinku.reader.data.track.model.TrackMangaMetadata(
        0, "test", "test", "test", "test", "test",
    )

    override suspend fun searchById(id: String) = null
}
