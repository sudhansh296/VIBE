package com.vibe.app.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * Our backend tokens and the signed-in user, kept in encrypted storage on the phone.
 * The phone never holds any secret of the server; these tokens only prove who this user is.
 */
object AuthStore {
    private var prefs: SharedPreferences? = null

    fun init(ctx: Context) {
        if (prefs != null) return
        val key = MasterKey.Builder(ctx.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        prefs = EncryptedSharedPreferences.create(
            ctx.applicationContext, "vibe_auth", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        me = user
    }

    /** The signed-in user for the UI (recomposes when the server sends a fresh copy). */
    var me by mutableStateOf<JSONObject?>(null)
        private set

    /** Goes up by one whenever the server ends the session (expired, logged out elsewhere, account blocked or deleted). */
    var sessionEnded by mutableIntStateOf(0)
        private set

    /** Why the last session ended, shown once on the login screen. */
    var endReason: String = ""
        private set

    fun endSession(reason: String = "Please log in again") { clear(); endReason = reason; sessionEnded++ }

    private fun p() = prefs ?: error("AuthStore.init() not called")

    val accessToken: String? get() = p().getString("access", null)
    val refreshToken: String? get() = p().getString("refresh", null)
    val hasSession: Boolean get() = refreshToken != null

    /** The user as the server last described it (public id, name, coins, profileComplete ...). */
    val user: JSONObject? get() = p().getString("user", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
    val profileComplete: Boolean get() = user?.optBoolean("profileComplete", false) == true

    fun save(access: String, refresh: String, user: JSONObject?) {
        p().edit().putString("access", access).putString("refresh", refresh).apply {
            if (user != null) putString("user", user.toString())
        }.apply()
        if (user != null) me = user
    }

    fun saveUser(user: JSONObject) { p().edit().putString("user", user.toString()).apply(); me = user }

    fun clear() {
        p().edit().clear().apply(); me = null
        com.vibe.app.ui.components.AuthImages.clear()
        com.vibe.app.data.Realtime.stop()
    }
}
