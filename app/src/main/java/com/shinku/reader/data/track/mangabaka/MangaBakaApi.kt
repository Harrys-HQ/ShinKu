package com.shinku.reader.data.track.mangabaka

import android.net.Uri
import androidx.core.net.toUri
import com.shinku.reader.BuildConfig
import com.shinku.reader.data.database.models.Track
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaItem
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaItemResult
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaListResult
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaOAuth
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaSearchResult
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaUserProfile
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaUserProfileResponse
import com.shinku.reader.data.track.model.TrackMangaMetadata
import com.shinku.reader.data.track.model.TrackSearch
import com.shinku.reader.domain.track.model.Track as DomainTrack
import com.shinku.reader.util.OAuthStateStore
import com.shinku.reader.util.PkceUtil
import com.shinku.reader.util.lang.toLocalDate
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.PUT
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.Headers.Companion.headersOf
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.injectLazy
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class MangaBakaApi(
    private val trackerId: Long,
    baseClient: OkHttpClient,
    interceptor: MangaBakaInterceptor,
) {

    private val json: Json by injectLazy()

    private val client = baseClient.newBuilder().addInterceptor {
        it.request().newBuilder()
            .header(
                "User-Agent",
                buildString {
                    append("ShinKu/v${BuildConfig.VERSION_NAME} ")
                    append("(${BuildConfig.APPLICATION_ID} ${BuildConfig.COMMIT_SHA}) ")
                    append("(Android) (https://github.com/Harrys-HQ/ShinKu)")
                },
            )
            .build()
            .let(it::proceed)
    }.build()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun addLibManga(track: Track): Track {
        return withContext(Dispatchers.IO) {
            val url = "$LIBRARY_API_URL/${track.remote_id}"
            val body = buildJsonObject {
                put("is_private", track.private)
                put("state", track.toApiStatus())
                if (track.last_chapter_read > 0.0) {
                    put("progress_chapter", track.last_chapter_read)
                }
                if (track.score > 0) {
                    put("rating", track.score.toInt().coerceIn(0, 100))
                }
                if (track.started_reading_date > 0) {
                    put("start_date", track.started_reading_date.toLocalDate().toString())
                }
                if (track.finished_reading_date > 0) {
                    put("finish_date", track.finished_reading_date.toLocalDate().toString())
                }
            }
                .toString()
                .toRequestBody()

            authClient
                .newCall(POST(url, body = body, headers = headersOf("Content-Type", APP_JSON)))
                .awaitSuccess().close()

            track
        }
    }

    suspend fun deleteLibManga(track: DomainTrack) {
        withContext(Dispatchers.IO) {
            val url = "$LIBRARY_API_URL/${track.remoteId}"

            authClient
                .newCall(DELETE(url))
                .awaitSuccess().close()
        }
    }

    suspend fun findLibManga(track: Track): Track? {
        return withContext(Dispatchers.IO) {
            with(json) {
                try {
                    val url = "$LIBRARY_API_URL/${track.remote_id}"
                    val userData = authClient.newCall(GET(url))
                        .awaitSuccess()
                        .parseAs<MangaBakaListResult>()
                        .data

                    val additionalData = authClient.newCall(GET("$API_BASE_URL/v1/series/${track.remote_id}"))
                        .awaitSuccess()
                        .parseAs<MangaBakaItemResult>()
                        .data

                    track.apply {
                        status = userData.getStatus()
                        last_chapter_read = userData.progressChapter ?: 0.0
                        score = userData.rating?.toDouble() ?: 0.0
                        title = additionalData.chooseBestTitle()
                        started_reading_date = parseIsoDateAsLocalStartOfDay(userData.startDate) ?: 0L
                        finished_reading_date = parseIsoDateAsLocalStartOfDay(userData.finishDate) ?: 0L
                    }
                } catch (e: HttpException) {
                    if (e.code == 404) {
                        return@with null
                    }
                    throw e
                }
            }
        }
    }

    suspend fun updateLibManga(track: Track): Track {
        return withContext(Dispatchers.IO) {
            val url = "$LIBRARY_API_URL/${track.remote_id}"
            val body = buildJsonObject {
                put("state", track.toApiStatus())
                put("is_private", track.private)
                if (track.last_chapter_read > 0.0) {
                    put("progress_chapter", track.last_chapter_read)
                } else {
                    put("progress_chapter", null)
                }
                if (track.score > 0) {
                    put("rating", track.score.toInt().coerceIn(0, 100))
                } else {
                    put("rating", null)
                }
                if (track.started_reading_date > 0) {
                    put("start_date", track.started_reading_date.toLocalDate().toString())
                } else {
                    put("start_date", null)
                }
                if (track.finished_reading_date > 0) {
                    put("finish_date", track.finished_reading_date.toLocalDate().toString())
                } else {
                    put("finish_date", null)
                }
            }
                .toString()
                .toRequestBody()

            authClient
                .newCall(PUT(url, body = body, headers = headersOf("Content-Type", APP_JSON)))
                .awaitSuccess().close()

            track
        }
    }

    suspend fun search(search: String): List<TrackSearch> {
        return withContext(Dispatchers.IO) {
            val url = "$API_BASE_URL/v1/series/search".toUri().buildUpon()
                .appendQueryParameter("q", search)
                .appendQueryParameter("type_not", "novel")
                .build()
            with(json) {
                client.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MangaBakaSearchResult>()
                    .data
                    .map { parseSearchItem(it) }
            }
        }
    }

    private fun parseSearchItem(item: MangaBakaItem): TrackSearch {
        return TrackSearch.create(trackerId).apply {
            remote_id = item.id
            title = item.chooseBestTitle()
            summary = item.description?.trim().orEmpty()
            score = item.rating?.toBigDecimal()?.setScale(2, RoundingMode.HALF_UP)?.toDouble() ?: -1.0
            cover_url = item.cover.x250.x1.orEmpty()
            tracking_url = "$BASE_URL/${item.id}"
            start_date = item.published.startDate.orEmpty()
            publishing_status = item.status
            publishing_type = item.type.replaceFirstChar { c ->
                if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString()
            }
            authors = item.authors.orEmpty()
            artists = item.artists.orEmpty()
        }
    }

    suspend fun getMangaDetails(id: Int): TrackSearch? {
        return withContext(Dispatchers.IO) {
            val url = "$API_BASE_URL/v1/series".toUri().buildUpon()
                .appendPath(id.toString())
                .build()
            with(json) {
                try {
                    authClient.newCall(GET(url.toString()))
                        .awaitSuccess()
                        .parseAs<MangaBakaItemResult>()
                        .data
                        .let { parseSearchItem(it) }
                } catch (e: HttpException) {
                    if (e.code == 404) {
                        return@with null
                    }
                    throw e
                }
            }
        }
    }

    suspend fun getMangaMetadata(track: DomainTrack): TrackMangaMetadata {
        return withContext(Dispatchers.IO) {
            val url = "$API_BASE_URL/v1/series".toUri().buildUpon()
                .appendPath(track.remoteId.toString())
                .build()
            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MangaBakaItemResult>()
                    .data
                    .let {
                        TrackMangaMetadata(
                            remoteId = it.id,
                            title = it.chooseBestTitle(),
                            thumbnailUrl = it.cover.raw.url,
                            description = it.description,
                            authors = it.authors?.joinToString(separator = ", "),
                            artists = it.artists?.joinToString(separator = ", "),
                        )
                    }
            }
        }
    }

    suspend fun getCurrentUser(): MangaBakaUserProfile {
        return withContext(Dispatchers.IO) {
            with(json) {
                authClient.newCall(GET("$API_BASE_URL/v1/my/profile"))
                    .awaitSuccess()
                    .parseAs<MangaBakaUserProfileResponse>()
                    .data
            }
        }
    }

    suspend fun getAccessToken(code: String): MangaBakaOAuth {
        return withContext(Dispatchers.IO) {
            val formBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("code", code)
                .add("code_verifier", codeVerifier)
                .add("code_challenge_method", "S256")
                .add("grant_type", "authorization_code")
                .add("redirect_uri", REDIRECT_URI)
                .add("scope", SCOPES)
                .build()

            with(json) {
                client.newCall(POST("$OAUTH_URL/token", body = formBody))
                    .awaitSuccess().parseAs()
            }
        }
    }

    fun verifyOAuthState(state: String): Boolean = OAuthStateStore.verify("mangabaka-auth", state)

    private fun parseIsoDateAsLocalStartOfDay(isoDate: String?): Long? {
        return isoDate
            ?.substringBefore("T")
            ?.let { LocalDate.parse(it) }
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
    }

    companion object {
        private const val CLIENT_ID = "zEZYMHXLWsLsafgbvJHXqzGvqQNOdkpo"

        private const val BASE_URL = "https://mangabaka.org"
        private const val API_BASE_URL = "https://api.mangabaka.org"
        private const val LIBRARY_API_URL = "$API_BASE_URL/v1/my/library"
        private const val OAUTH_URL = "$BASE_URL/auth/oauth2"
        private const val SCOPES = "library.read library.write offline_access openid"

        private const val REDIRECT_URI = "mihon://mangabaka-auth"

        private const val APP_JSON = "application/json"

        private var codeVerifier: String = ""

        fun authUrl(): Uri = "$OAUTH_URL/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("code_challenge", getPkceS256ChallengeCode())
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("state", OAuthStateStore.generate("mangabaka-auth"))
            .build()

        fun refreshTokenRequest(token: String) = POST(
            "$OAUTH_URL/token",
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", CLIENT_ID)
                .add("refresh_token", token)
                .add("redirect_uri", REDIRECT_URI)
                .build(),
        )

        private fun getPkceS256ChallengeCode(): String {
            val codes = PkceUtil.generateS256Codes()
            codeVerifier = codes.codeVerifier
            return codes.codeChallenge
        }
    }
}
