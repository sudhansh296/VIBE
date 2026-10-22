package com.vibe.app.ui.screens.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Demo
import com.vibe.app.ui.components.AcceptCallButton
import com.vibe.app.ui.components.CallBackground
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CallPill
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.EncryptedLabel
import com.vibe.app.ui.components.EndCallButton
import com.vibe.app.ui.components.NeonRingAvatar
import com.vibe.app.ui.components.VibeIcons
import com.vibe.app.ui.components.dockLine
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay

/** What a host sees when someone calls them. Earnings and the host's rates are decided by the server. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomingCallScreen(callerId: Int, audio: Boolean, onAccept: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val h = remember(callerId) { Demo.host(callerId) }
    val myRate = if (audio) 30 else 60 // the host's own voice/video rates (host settings, enforced by server)
    var left by remember { mutableIntStateOf(30) }

    LaunchedEffect(Unit) {
        while (left > 0) { delay(1000); left-- }
        ctx.toast("Missed call"); onDone()
    }

    CallBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                CallPill(borderColor = VibeColors.Green.copy(alpha = 0.35f)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(VibeColors.Green))
                    Text("Online as host", color = VibeColors.Green, fontSize = 12.sp)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { EncryptedLabel() }
                CallPill { Text("${left}s", color = Color(0xFFC7C7D1), fontSize = 12.sp) }
            }

            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(40.dp))
                Text(if (audio) "INCOMING VOICE CALL" else "INCOMING VIDEO CALL", color = VibeColors.TextDim, fontSize = 11.sp, letterSpacing = 1.8.sp)
                NeonRingAvatar(h.name.take(1), 138.dp, 48.sp, ringing = true, modifier = Modifier.padding(top = 28.dp))
                Text("${h.name}, ${h.age}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 28.dp))
                FlowRow(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(h.city, "Lv.${h.level}", "Good caller", h.langs.first()).forEach { Chip(it) }
                }
                Row(Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CoinDot(12.dp)
                    Text("  YOU EARN ", color = Color(0xFF6B6B7B), fontSize = 10.sp, letterSpacing = 1.sp)
                    Text("$myRate", color = VibeColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text(" / MIN · KEEP 60%", color = Color(0xFF6B6B7B), fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }

            Column(Modifier.fillMaxWidth().dockLine().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 30.dp)) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Busy, call later 🙏", "Give me 5 min", "Text me instead").forEach { msg ->
                        Chip(msg) { ctx.toast("Declined · sent \"$msg\""); onDone() }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 28.dp, start = 24.dp, end = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    EndCallButton({ ctx.toast("Call declined"); onDone() }, label = "Decline")
                    AcceptCallButton(if (audio) CallIcons.Phone else VibeIcons.Video, onAccept)
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, onClick: (() -> Unit)? = null) {
    Text(
        text, color = Color(0xFFC7C7D1), fontSize = 11.sp,
        modifier = Modifier.clip(CircleShape).background(VibeColors.Dark.copy(alpha = 0.4f)).border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
