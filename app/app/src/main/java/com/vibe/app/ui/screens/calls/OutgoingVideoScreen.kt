package com.vibe.app.ui.screens.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Demo
import com.vibe.app.ui.components.BalancePill
import com.vibe.app.ui.components.CallButton
import com.vibe.app.ui.components.CallIconButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CallLabel
import com.vibe.app.ui.components.CallRateLine
import com.vibe.app.ui.components.CallShade
import com.vibe.app.ui.components.CallStatus
import com.vibe.app.ui.components.CameraStandIn
import com.vibe.app.ui.components.EncryptedLabel
import com.vibe.app.ui.components.EndCallButton
import com.vibe.app.ui.components.NeonRingAvatar
import com.vibe.app.ui.components.dockLine
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay

/** Outgoing video call: your own front camera fills the screen while it rings (WebRTC local preview in the real app). */
@Composable
fun OutgoingVideoScreen(hostId: Int, onConnected: (Int) -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    val h = remember(hostId) { Demo.host(hostId) }
    var phase by remember { mutableStateOf("Video calling…") }
    var mirrored by remember { mutableStateOf(false) }
    var camOff by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }

    // demo: the real app waits for the server's "accepted" event
    LaunchedEffect(Unit) {
        delay(1500); phase = "Ringing…"
        delay(2000); phase = "Connecting"
        delay(700); onConnected(h.id)
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        if (camOff) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("CAMERA OFF", color = CallLabel, fontSize = 11.sp, letterSpacing = 1.6.sp)
        } else CameraStandIn(Color(0xFF34324A), Color(0xFF0B0B12), Modifier.graphicsLayer { scaleX = if (mirrored) -1f else 1f })
        CallShade()

        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                CallIconButton(CallIcons.Chevron, { ctx.toast("Call cancelled · no coins charged"); onCancel() })
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { EncryptedLabel() }
                BalancePill(Demo.BALANCE)
            }
            Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NeonRingAvatar(h.name.take(1), 98.dp, 32.sp, ringing = true)
                Text(h.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 20.dp))
                Box(Modifier.padding(top = 8.dp)) { CallStatus(phase, live = phase == "Connecting") }
                Box(Modifier.padding(top = 12.dp)) { CallRateLine(h.rate, "after pick-up") }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().dockLine().padding(start = 40.dp, end = 40.dp, top = 18.dp, bottom = 30.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CallButton(CallIcons.Flip, "Flip", { mirrored = !mirrored })
                    CallButton(CallIcons.Camera, "Camera", { camOff = !camOff }, on = camOff)
                    CallButton(CallIcons.Mic, "Mute", { muted = !muted }, on = muted)
                }
                Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    EndCallButton({ ctx.toast("Call cancelled · no coins charged"); onCancel() }, label = "Cancel")
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
