package com.shinku.reader.ui.setting.track

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.data.track.comick.ComickMissingScopesException
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
                "anilist-auth" -> handleAniList(data["access_token"])
                "comick-auth" -> handleComick(data["code"], data["state"])
                "bangumi-auth" -> handleBangumi(data["code"])
                "myanimelist-auth" -> handleMyAnimeList(data["code"])
                "shikimori-auth" -> handleShikimori(data["code"])
            }
            returnToSettings()
        }
    }

    private suspend fun handleComick(code: String?, state: String?) {
        if (state == null) {
            logcat(LogPriority.WARN) { "Did not receive state parameter from Comick OAuth" }
            return
        }
        if (code != null) {
            if (!trackerManager.comick.verifyOAuthState(state)) {
                logcat(LogPriority.WARN) { "Received wrong OAuth state back from Comick" }
                return
            }
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

    private suspend fun handleAniList(accessToken: String?) {
        if (accessToken != null) {
            trackerManager.aniList.login(accessToken)
        } else {
            trackerManager.aniList.logout()
        }
    }

    private suspend fun handleBangumi(code: String?) {
        if (code != null) {
            trackerManager.bangumi.login(code)
        } else {
            trackerManager.bangumi.logout()
        }
    }

    private suspend fun handleMyAnimeList(code: String?) {
        if (code != null) {
            trackerManager.myAnimeList.login(code)
        } else {
            trackerManager.myAnimeList.logout()
        }
    }

    private suspend fun handleShikimori(code: String?) {
        if (code != null) {
            trackerManager.shikimori.login(code)
        } else {
            trackerManager.shikimori.logout()
        }
    }
}
