package com.shinku.reader.exh.md.network

import com.shinku.reader.domain.track.service.TrackPreferences
import com.shinku.reader.data.track.mdlist.MdList
import com.shinku.reader.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import com.shinku.reader.exh.md.utils.MdApi
import com.shinku.reader.exh.md.utils.MdConstants
import com.shinku.reader.exh.md.utils.MdUtil
import logcat.LogPriority
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import com.shinku.reader.core.common.util.system.logcat

class MangaDexLoginHelper(
    private val client: OkHttpClient,
    private val preferences: TrackPreferences,
    private val mdList: MdList,
    private val mangaDexAuthInterceptor: MangaDexAuthInterceptor,
) {

    /**
     *  Login given the generated authorization code
     */
    suspend fun login(authorizationCode: String): Boolean {
        val loginFormBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("grant_type", MdConstants.Login.authorizationCode)
            .add("code", authorizationCode)
            .add("code_verifier", MdUtil.getPkceChallengeCode())
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val error = kotlin.runCatching {
            val data = with(MdUtil.jsonParser) {
                client.newCall(
                    POST(MdApi.baseAuthUrl + MdApi.token, body = loginFormBody),
                ).awaitSuccess().parseAs<MALOAuth>()
            }
            mangaDexAuthInterceptor.setAuth(data)
        }.exceptionOrNull()

        return when (error == null) {
            true -> true
            false -> {
                logcat(LogPriority.ERROR, error) { "Error logging in" }
                mdList.logout()
                false
            }
        }
    }

    suspend fun logout(): Boolean {
        val oauth = MdUtil.loadOAuth(preferences, mdList)
        val sessionToken = oauth?.accessToken
        val refreshToken = oauth?.refreshToken
        if (refreshToken.isNullOrEmpty() || sessionToken.isNullOrEmpty()) {
            mdList.logout()
            return true
        }

        val formBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("refresh_token", refreshToken)
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val error = kotlin.runCatching {
            client.newCall(
                POST(
                    url = MdApi.baseAuthUrl + MdApi.logout,
                    headers = Headers.Builder().add("Authorization", "Bearer $sessionToken")
                        .build(),
                    body = formBody,
                ),
            ).awaitSuccess()
            mdList.logout()
        }.exceptionOrNull()

        return when (error == null) {
            true -> {
                mangaDexAuthInterceptor.setAuth(null)
                true
            }
            false -> {
                logcat(LogPriority.ERROR, error) { "Error logging out" }
                false
            }
        }
    }
}
