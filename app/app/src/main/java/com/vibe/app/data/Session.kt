package com.vibe.app.data

import android.content.Context

/**
 * Remembers that onboarding (login + profile setup) is done, so the app opens straight to Home next time.
 * DEMO ONLY: when the backend is wired, store the server's auth token in EncryptedSharedPreferences instead,
 * and let the server decide whether the session is still valid.
 */
object Session {
    private const val PREFS = "vibe_session"
    private const val KEY_ONBOARDED = "onboarded"

    fun isLoggedIn(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDED, false)

    fun markLoggedIn(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ONBOARDED, true).apply()

    fun logout(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
}
