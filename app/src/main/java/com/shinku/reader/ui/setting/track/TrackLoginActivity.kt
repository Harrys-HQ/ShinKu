package com.shinku.reader.ui.setting.track

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.data.track.comick.ComickMissingScopesException
import com.shinku.reader.util.OAuthStateStore
import com.shinku.reader.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority

class TrackLoginActivity : BaseOAuthLoginActivity() {

    override fun handleResult(uri: Uri) {
        val data = when {
            !uri.encodedQuery.isNullOrBlank() -> uri.encodedQuery
            !uri.encodedFragment.isNullOrBlank() -> uri.encodedFragment
            else -> null
        }
            ?.split("&")
            ?.filter { it.isNotBlank() }
            ?.associate {
                val parts = it.split("=", limit = 2).map<String, String>(Uri::decode)
                parts[0] to parts.getOrNull(1)
            }
            .orEmpty()

        lifecycleScope.launch {
            when (uri.host) {
                "anilist-auth" -> handleAniList(data["access_token"], data["state"])
                "comick-auth" -> handleComick(data["code"], data["state"])
                "bangumi-auth" -> handleBangumi(data["code"], data["state"])
                "myanimelist-auth" -> handleMyAnimeList(data["code"], data["state"])
                "shikimori-auth" -> handleShikimori(data["code"], data["state"])
            }
            returnToSettings()
        }
    }

    private suspend fun handleComick(code: String?, state: String?) {
        if (state == null || !trackerManager.comick.verifyOAuthState(state)) {
            logcat(LogPriority.WARN) { "Received invalid or missing OAuth state back from Comick" }
            return
        }
        if (code != null) {
            try {
                trackerManager.comick.login(code)
            } catch (e: ComickMissingScopesException) {
                withContext(Dispatchers.Main) {
                    this@TrackLoginActivity.toast(e.message, Toast.LENGTH_LONG)
                }
                return
            }
        } else {
            trackerManager.comick.logout()
        }
    }

    private suspend fun handleAniList(accessToken: String?, state: String?) {
        if (state == null || !OAuthStateStore.verify("anilist-auth", state)) {
            logcat(LogPriority.WARN) { "Received invalid or missing OAuth state back from AniList" }
            return
        }
        if (accessToken != null) {
            trackerManager.aniList.login(accessToken)
        } else {
            trackerManager.aniList.logout()
        }
    }

    private suspend fun handleBangumi(code: String?, state: String?) {
        if (state == null || !OAuthStateStore.verify("bangumi-auth", state)) {
            logcat(LogPriority.WARN) { "Received invalid or missing OAuth state back from Bangumi" }
            return
        }
        if (code != null) {
            trackerManager.bangumi.login(code)
        } else {
            trackerManager.bangumi.logout()
        }
    }

    private suspend fun handleMyAnimeList(code: String?, state: String?) {
        if (state == null || !OAuthStateStore.verify("myanimelist-auth", state)) {
            logcat(LogPriority.WARN) { "Received invalid or missing OAuth state back from MyAnimeList" }
            return
        }
        if (code != null) {
            trackerManager.myAnimeList.login(code)
        } else {
            trackerManager.myAnimeList.logout()
        }
    }

    private suspend fun handleShikimori(code: String?, state: String?) {
        if (state == null || !OAuthStateStore.verify("shikimori-auth", state)) {
            logcat(LogPriority.WARN) { "Received invalid or missing OAuth state back from Shikimori" }
            return
        }
        if (code != null) {
            trackerManager.shikimori.login(code)
        } else {
            trackerManager.shikimori.logout()
        }
    }
}
