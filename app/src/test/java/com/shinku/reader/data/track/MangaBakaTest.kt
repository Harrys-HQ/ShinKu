package com.shinku.reader.data.track

import com.shinku.reader.data.database.models.Track
import com.shinku.reader.data.track.mangabaka.MangaBaka
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaCover
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaItem
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaItemResult
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaItemTitle
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaListEntry
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaOAuth
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaPublishData
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaRawCover
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaScaledCover
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaUserProfile
import com.shinku.reader.data.track.mangabaka.dto.MangaBakaUserProfileResponse
import com.shinku.reader.data.track.mangabaka.toApiStatus
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MangaBakaTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testTitleSelectionPrioritizesEnglishAndPrimary() {
        val item = MangaBakaItem(
            id = 42L,
            cover = MangaBakaCover(MangaBakaRawCover("https://raw.jpg"), MangaBakaScaledCover("https://250.jpg")),
            authors = listOf("Author One"),
            artists = listOf("Artist One"),
            description = "A wonderful test manga",
            published = MangaBakaPublishData("2024-01-01"),
            status = "releasing",
            type = "manga",
            rating = 88.5,
            titles = listOf(
                MangaBakaItemTitle(language = "ja", traits = listOf("native"), title = "日本語タイトル", isPrimary = false),
                MangaBakaItemTitle(language = "en", traits = listOf("official"), title = "English Title Official", isPrimary = true),
                MangaBakaItemTitle(language = "ko", traits = emptyList(), title = "한국어", isPrimary = false),
            ),
        )

        assertEquals("English Title Official", item.chooseBestTitle())
    }

    @Test
    fun testStatusMappingBidirectional() {
        val statuses = listOf(
            MangaBaka.READING to "reading",
            MangaBaka.COMPLETED to "completed",
            MangaBaka.PAUSED to "paused",
            MangaBaka.DROPPED to "dropped",
            MangaBaka.PLAN_TO_READ to "plan_to_read",
            MangaBaka.REREADING to "rereading",
            MangaBaka.CONSIDERING to "considering",
        )

        for ((code, apiStr) in statuses) {
            val track = Track.create(11L).apply {
                status = code
            }
            assertEquals(apiStr, track.toApiStatus())

            val entry = MangaBakaListEntry(
                state = apiStr,
                startDate = "2024-01-01",
                finishDate = null,
                isPrivate = false,
                progressChapter = 5.0,
                rating = 80L,
            )
            assertEquals(code, entry.getStatus())
        }
    }

    @Test
    fun testOAuthParsingAndExpiration() {
        val rawJson = """
            {
                "access_token": "test_access_token",
                "refresh_token": "test_refresh_token",
                "expires_at": 9999999999
            }
        """.trimIndent()

        val oauth = json.decodeFromString<MangaBakaOAuth>(rawJson)
        assertEquals("test_access_token", oauth.accessToken)
        assertEquals("test_refresh_token", oauth.refreshToken)
        assertFalse(oauth.isExpired(), "Token far in future should not be expired")

        val expiredJson = """
            {
                "access_token": "test_access_token",
                "refresh_token": "test_refresh_token",
                "expires_at": 1000
            }
        """.trimIndent()
        val expiredOauth = json.decodeFromString<MangaBakaOAuth>(expiredJson)
        assertTrue(expiredOauth.isExpired(), "Token in the past should be expired")
    }

    @Test
    fun testUserProfileParsing() {
        val rawJson = """
            {
                "data": {
                    "id": "user_123",
                    "rating_steps": 10,
                    "nickname": "MangaFan",
                    "preferred_username": "mfan"
                }
            }
        """.trimIndent()

        val profileResponse = json.decodeFromString<MangaBakaUserProfileResponse>(rawJson)
        assertEquals("user_123", profileResponse.data.id)
        assertEquals(10, profileResponse.data.ratingSteps)
        assertEquals("MangaFan", profileResponse.data.nickname)
    }
}
