package com.vibe.app.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vibe.app.data.Session
import com.vibe.app.data.SettingsDemo
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.SettingsDivider
import com.vibe.app.ui.components.SettingsGroup
import com.vibe.app.ui.components.SettingsHeading
import com.vibe.app.ui.components.SettingsRow
import com.vibe.app.ui.components.VibeSwitch
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

/** Settings. DEMO values; preferences, phone, login type and the block list are stored by the server. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onCallSettings: () -> Unit,
    onWithdraw: () -> Unit,
    onHelp: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignedOut: () -> Unit,
    onBlocked: () -> Unit,
    onSafety: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onGuidelines: () -> Unit,
    onLanguage: () -> Unit,
    onStatesDemo: () -> Unit,
) {
    val isHost = com.vibe.app.data.api.AuthStore.me?.optString("role") == "host"
    val prefScope = androidx.compose.runtime.rememberCoroutineScope()
    // the phone number is fixed for the life of the account (it cannot be changed)
    val myPhone = com.vibe.app.data.api.AuthStore.me?.optString("phone")?.takeIf { it.startsWith("+") }
    val maskedPhone = myPhone?.let { p -> val d = p.removePrefix("+91"); if (d.length == 10) "+91 ${d.take(2)}••••${d.takeLast(4)}" else p } ?: "Not added"
    val ctx = LocalContext.current
    var msgs by remember { mutableStateOf(true) }
    var missed by remember { mutableStateOf(true) }
    var offers by remember { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { com.vibe.app.data.api.Api.get("/me/settings").getJSONObject("settings") }.onSuccess {
            msgs = it.optBoolean("notifyMessages", true); missed = it.optBoolean("notifyMissedCalls", true); offers = it.optBoolean("notifyOffers", true)
        }
    }
    // saves one switch; if the server refuses (or no internet) the switch flips back
    fun save(key: String, value: Boolean, undo: () -> Unit) {
        prefScope.launch {
            try { com.vibe.app.data.api.Api.put("/me/settings", org.json.JSONObject().put(key, value)) }
            catch (e: com.vibe.app.data.api.ApiException) { undo(); ctx.toast(e.message) }
        }
    }
    var askSignOut by remember { mutableStateOf(false) }
    var askOthers by remember { mutableStateOf(false) }
    val soon = { ctx.toast("Coming with the backend") }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Cyan, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Settings", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                SettingsHeading("Account")
                SettingsGroup {
                    SettingsRow(MeIcons.Edit, VibeColors.Pink, "Edit profile", onClick = onEditProfile)
                    SettingsDivider()
                    SettingsRow(MeIcons.Phone, VibeColors.Cyan, "Phone number", sub = "Can't be changed", value = maskedPhone)
                    SettingsDivider()
                    SettingsRow(CallIcons.Lock, VibeColors.Gold, "Login", value = if (myPhone != null) "Phone OTP" else "Google")
                    SettingsDivider()
                    SettingsRow(MeIcons.SignOut, VibeColors.Pink, "Log out of other devices", sub = "Keeps you logged in on this phone", onClick = { askOthers = true })
                }

                if (isHost) {
                    SettingsHeading("Host")
                    SettingsGroup {
                        SettingsRow(MeIcons.Settings, VibeColors.Pink, "Call settings", value = "Video rate · message price", onClick = onCallSettings)
                        SettingsDivider()
                        SettingsRow(MeIcons.Bank, VibeColors.Gold, "UPI & PAN", onClick = onWithdraw)
                        SettingsDivider()
                        SettingsRow(MeIcons.Bank, VibeColors.Green, "Withdrawals", onClick = onWithdraw)
                    }
                }

                SettingsHeading("Notifications")
                SettingsGroup {
                    SettingsRow(MeIcons.Chat, VibeColors.Cyan, "Messages", trailing = { VibeSwitch(msgs, { msgs = !msgs; save("notifyMessages", msgs) { msgs = !msgs } }) })
                    SettingsDivider()
                    SettingsRow(CallIcons.Missed, VibeColors.Pink, "Missed calls", trailing = { VibeSwitch(missed, { missed = !missed; save("notifyMissedCalls", missed) { missed = !missed } }) })
                    SettingsDivider()
                    SettingsRow(CallIcons.Gift, VibeColors.Gold, "Offers & rewards", trailing = { VibeSwitch(offers, { offers = !offers; save("notifyOffers", offers) { offers = !offers } }) })
                }

                SettingsHeading("Privacy & safety")
                SettingsGroup {
                    SettingsRow(CallIcons.Ban, VibeColors.Red, "Blocked users", value = "${com.vibe.app.data.SafetyStore.blocked.size}", onClick = onBlocked)
                    SettingsDivider()
                    SettingsRow(CallIcons.Shield, VibeColors.Cyan, "Safety center", onClick = onSafety)
                }

                SettingsHeading("General")
                SettingsGroup {
                    SettingsRow(MeIcons.Globe, VibeColors.Cyan, "Language", value = "English", onClick = onLanguage)
                    SettingsDivider()
                    SettingsRow(MeIcons.Help, VibeColors.Green, "Help & support", onClick = onHelp)
                    SettingsDivider()
                    SettingsRow(MeIcons.Doc, Color(0xFF5A3F4D), "Terms of Service", onClick = onTerms)
                    SettingsDivider()
                    SettingsRow(MeIcons.Doc, Color(0xFF5A3F4D), "Privacy Policy", onClick = onPrivacy)
                    SettingsDivider()
                    SettingsRow(MeIcons.Doc, Color(0xFF5A3F4D), "Community guidelines", onClick = onGuidelines)
                }

                SettingsGroup(Modifier.padding(top = 24.dp)) {
                    SettingsRow(MeIcons.SignOut, Color(0xFF5A3F4D), "Sign out", onClick = { askSignOut = true })
                    SettingsDivider()
                    SettingsRow(CallIcons.Trash, VibeColors.Red, "Delete account", titleColor = VibeColors.Red, onClick = onDeleteAccount)
                }
                Text(
                    "VIBE · version ${com.vibe.app.BuildConfig.VERSION_NAME}", color = Color(0xFFB093A2), fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onLongPress = { onStatesDemo() }) }.padding(vertical = 24.dp),   // long press: preview loading / offline / error screens (design review only)
                )
            }
        }

        if (askSignOut) Dialog(onDismissRequest = { askSignOut = false }) {
            val shape = RoundedCornerShape(24.dp)
            Column(Modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(24.dp)) {
                Text("Sign out?", color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(
                    "You will need to log in again with your phone or Google.", color = VibeColors.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFEDF3)).border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp))
                            .noRippleClick { askSignOut = false },
                        contentAlignment = Alignment.Center,
                    ) { Text("Cancel", color = VibeColors.Ink, fontWeight = FontWeight.SemiBold) }
                    Box(Modifier.weight(1f)) {
                        NeonButton("Sign out", onClick = { askSignOut = false; Session.logout(ctx); onSignedOut() }, modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }
    if (askOthers) com.vibe.app.ui.components.ConfirmDialog(
        title = "Log out of other devices?",
        message = "Every other phone where your account is open will be logged out. This phone stays logged in.",
        confirmText = "Log out",
        onConfirm = {
            askOthers = false
            prefScope.launch {
                try {
                    val n = com.vibe.app.data.api.Api.post("/auth/logout-others").optInt("loggedOut")
                    ctx.toast(if (n > 0) "Logged out of $n other device${if (n == 1) "" else "s"}" else "No other devices were logged in")
                } catch (e: com.vibe.app.data.api.ApiException) {
                    ctx.toast(e.message)
                }
            }
        },
        onDismiss = { askOthers = false },
    )
}
