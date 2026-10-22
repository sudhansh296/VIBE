package com.vibe.app.ui.screens.extras

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.SkeletonBox
import com.vibe.app.ui.components.StateScreen
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

private val KINDS = listOf("loading" to "Loading", "offline" to "No internet", "error" to "Error", "maint" to "Maintenance", "update" to "Update", "force" to "Must update")

/**
 * Loading skeleton, no internet, error, maintenance, optional update and forced update.
 * The top switcher is for design review only (demo). In the real app: Loading shows while the API answers; offline comes from the
 * network check; Maintenance and forced Update come from the SERVER (admin panel > Settings: maintenance on/off + message, minimum version).
 */
@Composable
fun AppStatesScreen(initial: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var kind by remember { mutableStateOf(initial) }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        when (kind) {
            "loading" -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Switcher(kind) { kind = it }
                LoadingSkeleton()
            }
            "offline" -> StateScreen(MeIcons.WifiOff, VibeColors.Red, "No internet connection", "Check your Wi-Fi or mobile data and try again.", "Try again", { ctx.toast("Checking connection…") }, top = { Switcher(kind) { kind = it } })
            "error" -> StateScreen(MeIcons.Warning, Color(0xFFFF9A3C), "Something went wrong", "We could not load this right now. Please try again in a moment.", "Try again", { ctx.toast("Trying again…") },
                secondary = "Go to Home", onSecondary = onClose, top = { Switcher(kind) { kind = it } })
            "maint" -> StateScreen(MeIcons.Wrench, VibeColors.Gold, "We are making VIBE better", "VIBE is under maintenance. We will be back soon. Your coins and chats are safe.", "Check again", { ctx.toast("Checking…") },
                note = "Back by about 4:30 pm", top = { Switcher(kind) { kind = it } })
            "update" -> StateScreen(MeIcons.Download, VibeColors.Cyan, "A new version is ready", "Update for the latest features and fixes.", "Update now", { ctx.toast("Play Store opens") },
                secondary = "Later", onSecondary = onClose, note = "Version 1.1.0", top = { Switcher(kind) { kind = it } })
            else -> StateScreen(MeIcons.Download, VibeColors.Pink, "Please update VIBE", "This version is too old and no longer works. Update from the Play Store to continue.", "Update on Play Store", { ctx.toast("Play Store opens") },
                note = "Required · Version 1.1.0", top = { Switcher(kind) { kind = it } })
        }
    }
}

@Composable
private fun Switcher(selected: String, onPick: (String) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        KINDS.forEach { (k, l) ->
            Box(
                Modifier.height(30.dp).clip(CircleShape).background(if (k == selected) Color(0xFF26263A) else VibeColors.Card)
                    .border(1.dp, VibeColors.Line, CircleShape).clickable { onPick(k) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(l, color = if (k == selected) Color.White else Color(0xFF8B8B9A), fontSize = 11.sp) }
        }
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonBox(Modifier.size(width = 150.dp, height = 40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { repeat(5) { SkeletonBox(Modifier.size(64.dp), radius = 32.dp) } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { repeat(2) { SkeletonBox(Modifier.weight(1f).height(230.dp), radius = 24.dp) } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { repeat(2) { SkeletonBox(Modifier.weight(1f).height(230.dp), radius = 24.dp) } }
    }
}
