package com.vibe.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener

/**
 * A friend's invite code that came with a link, so nobody has to type it:
 * - the app opened from an invite link (vibecallme://invite/CODE, sent by the server's /i/CODE page), or
 * - the app was installed from the Play Store link of that page (the code comes as the install referrer "invite=CODE").
 * The code waits here (kept on the phone, also over a logout) until it is applied: at the end of sign-up, or at once
 * for someone already signed in. Typing a code by hand still works as before.
 */
object InviteLink {
    private const val PREFS = "vibe_invite"
    private val valid = Regex("^[A-Z0-9]{4,12}$")

    var pending by mutableStateOf<String?>(null)
        private set

    fun init(ctx: Context) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        pending = p.getString("code", null)
        if (!p.getBoolean("referrerChecked", false)) readInstallReferrer(ctx.applicationContext)
    }

    /** The app was opened by an invite link. */
    fun fromIntent(ctx: Context, intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "vibecallme" || uri.host != "invite") return
        save(ctx, uri.pathSegments.firstOrNull())
    }

    fun clear(ctx: Context) {
        pending = null
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("code").apply()
    }

    private fun save(ctx: Context, raw: String?) {
        val code = raw?.trim()?.uppercase()?.takeIf { valid.matches(it) } ?: return
        pending = code
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("code", code).apply()
    }

    /** Asked once after install: did the Play Store bring an invite code along? */
    private fun readInstallReferrer(ctx: Context) {
        val client = runCatching { InstallReferrerClient.newBuilder(ctx).build() }.getOrNull() ?: return
        runCatching {
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(code: Int) {
                    val ref = if (code == InstallReferrerClient.InstallReferrerResponse.OK) runCatching { client.installReferrer.installReferrer }.getOrNull() else null
                    // OK (with or without a code) or not available: never ask again; a lost connection is asked next start
                    if (code != InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED) {
                        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("referrerChecked", true).apply()
                    }
                    ref?.let { r -> Uri.parse("https://x/?$r").getQueryParameter("invite") }?.let { c ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post { save(ctx, c) }
                    }
                    runCatching { client.endConnection() }
                }
                override fun onInstallReferrerServiceDisconnected() {}
            })
        }
    }

    /** Someone already signed in opened an invite link: the code is applied now (the server says if it is too late). */
    suspend fun applyNow(ctx: Context) {
        val code = pending ?: return
        if (!com.vibe.app.data.api.AuthStore.hasSession || !com.vibe.app.data.api.AuthStore.profileComplete) return
        val r = runCatching { com.vibe.app.data.api.Api.post("/invite/apply", org.json.JSONObject().put("code", code)) }
        if ((r.exceptionOrNull() as? com.vibe.app.data.api.ApiException)?.status == 0) return   // offline: tried again next start
        clear(ctx)
        val msg = r.fold(
            { "Friend's code $code added · you both get coins on your first purchase" },
            { "Invite code $code: ${it.message ?: "could not be used"}" },
        )
        android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
    }
}
