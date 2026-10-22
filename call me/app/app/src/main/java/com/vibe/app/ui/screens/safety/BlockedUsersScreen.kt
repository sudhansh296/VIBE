package com.vibe.app.ui.screens.safety

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.Demo
import com.vibe.app.data.Host
import com.vibe.app.data.SafetyStore
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.ConfirmDialog
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

/** Blocked users: list with Unblock, empty state. DEMO; the block list lives on the server. */
@Composable
fun BlockedUsersScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val floatY by rememberFloat()
    var target by remember { mutableStateOf<Host?>(null) }
    val hosts = SafetyStore.blocked.map { Demo.host(it) }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Red, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Blocked users", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text(
                    "People you block can't message or call you and can't see you online. They are not told that you blocked them.",
                    color = VibeColors.TextMute, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                if (hosts.isEmpty()) Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ghost_empty), null, Modifier.size(160.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Text("No blocked users", color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    Text(
                        "If someone bothers you, open their profile or chat and tap Block.", color = VibeColors.TextDim, fontSize = 14.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
                    )
                }
                hosts.forEach { h ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(h, Modifier.size(48.dp).clip(CircleShape).alpha(0.7f), fontSize = 18.sp)
                        Column(Modifier.weight(1f)) {
                            Text(com.vibe.app.ui.components.nameAge(h.name, h.age), color = VibeColors.Ink, fontWeight = FontWeight.SemiBold)
                            Text(SafetyStore.blockedAt[h.id] ?: "Blocked", color = VibeColors.TextMute, fontSize = 11.sp)
                        }
                        Box(
                            Modifier.height(36.dp).clip(CircleShape).border(1.dp, VibeColors.Ink.copy(alpha = 0.2f), CircleShape).clickable { target = h }.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Unblock", color = VibeColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.05f)))
                }
            }
        }
    }

    target?.let { h ->
        ConfirmDialog(
            title = "Unblock ${h.name}?", message = "They will be able to message and call you again.", confirmText = "Unblock",
            onConfirm = { SafetyStore.unblock(h.id); target = null; ctx.toast("${h.name} unblocked") },
            onDismiss = { target = null },
        )
    }
}
