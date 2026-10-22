package com.vibe.app.data

import kotlinx.coroutines.launch
import android.content.Context

/**
 * Logged in = this phone holds a server session (tokens in encrypted storage, see AuthStore) and the profile is filled in.
 * The server decides whether the session is still valid; an expired one sends the user back to login.
 */
object Session {
    private const val PREFS = "vibe_session"
    private const val KEY_ONBOARDED = "onboarded"

    fun isLoggedIn(ctx: Context): Boolean {
        com.vibe.app.data.api.AuthStore.init(ctx)
        return com.vibe.app.data.api.AuthStore.hasSession && com.vibe.app.data.api.AuthStore.profileComplete
    }

    fun markLoggedIn(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ONBOARDED, true).apply()

    fun logout(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        com.vibe.app.data.api.AuthStore.init(ctx)
        val app = ctx.applicationContext
        // nothing of this account stays on the phone: its saved DP copy and demo statuses go too
        app.filesDir.listFiles { f -> f.name.startsWith("dp_") || f.name.startsWith("status_") }?.forEach { it.delete() }
        com.vibe.app.ui.components.MyProfile.dp = null
        StatusStore.posts.clear()
        // tell the server (revokes the refresh token) and forget everything on this phone
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch { com.vibe.app.data.api.AuthRepo.logout(app) }
    }
}
