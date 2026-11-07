package com.shinku.reader.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.security.SecureRandom
import java.util.Base64

object OAuthStateStore {

    private const val PREF_NAME = "tracker_oauth_state"

    private fun getPrefs(): SharedPreferences {
        val context = Injekt.get<Application>()
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Generates a cryptographically secure random state token and persists it for the given [key].
     */
    fun generate(key: String): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        getPrefs().edit().putString(key, state).apply()
        return state
    }

    /**
     * Verifies whether [state] matches the stored nonce for [key].
     * Once verified, the stored nonce is immediately invalidated to prevent replay attacks.
     */
    fun verify(key: String, state: String?): Boolean {
        if (state.isNullOrBlank()) return false
        val prefs = getPrefs()
        val expected = prefs.getString(key, null) ?: return false
        prefs.edit().remove(key).apply()
        return expected == state
    }
}
