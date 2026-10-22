package com.vibe.app.ui.screens.calls

import com.vibe.app.ui.components.CallGlyph
import com.vibe.app.ui.components.CallDark
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

    // a person from the server: the server rings the host and tells us when they pick up, decline or miss it
    val callId = remember { com.vibe.app.data.Realtime.activeCallId }
    val live = h.real && callId != null
    var closed by remember { mutableStateOf(false) }   // leave the screen only once (button + server event can both end it)
    fun leave() { if (!closed) { closed = true; onCancel() } }
    LaunchedEffect(Unit) {
        if (live) {
            phase = "Ringing…"
            com.vibe.app.data.Realtime.events.collect { e ->
                when {
                    e is com.vibe.app.data.LiveEvent.Accepted && e.callId == callId && !closed -> { closed = true; phase = "Connecting"; onConnected(h.id) }
                    e is com.vibe.app.data.LiveEvent.Topping && e.callId == callId -> { phase = "Recharging…"; ctx.toast("${h.name} is adding coins to pick up") }
                    e is com.vibe.app.data.LiveEvent.Ended && e.callId == callId -> {
                        if (!closed) ctx.toast(when (e.reason) { "declined" -> "${h.name} can't take the call right now"; "missed" -> "${h.name} did not pick up"; "cancelled" -> "Call cancelled"; else -> "Call ended" } + " · no coins charged")
                        leave()
                    }
                }
            }
        } else leave()   // no call from the server (it ended already): nothing to show
    }
    fun cancel() {
        if (closed) return
        if (live && callId != null) com.vibe.app.data.Realtime.cancelCall(callId)
        ctx.toast("Call cancelled · no coins charged"); leave()
    }
    // real call: camera + microphone (asked once); the preview is my own front camera
    var camReady by remember { mutableStateOf(false) }
    val ask = rememberCallPermissionAsker { ok ->
        if (ok) { com.vibe.app.data.CallEngine.startLocal(ctx); camReady = true }
        else { ctx.toast("Allow the camera and microphone to make video calls"); cancel() }
    }
    LaunchedEffect(live) { if (live) ask() }
    androidx.activity.compose.BackHandler { cancel() }   // back while ringing = cancel the call

    com.vibe.app.ui.components.CallStatusBarIcons()
    Box(Modifier.fillMaxSize().background(CallDark)) {
        if (camOff) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("CAMERA OFF", color = CallLabel, fontSize = 11.sp, letterSpacing = 1.6.sp)
        } else if (live && camReady) com.vibe.app.data.VideoView(Modifier.fillMaxSize(), remote = false, mirror = com.vibe.app.data.CallEngine.frontCamera)
        else CameraStandIn(Color(0xFFFF9EC3), Color(0xFFC2185B), Modifier.graphicsLayer { scaleX = if (mirrored) -1f else 1f })
        CallShade()

        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                CallIconButton(CallIcons.Chevron, { cancel() })
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { EncryptedLabel() }
                if (com.vibe.app.data.Realtime.activeCall?.role != "host") BalancePill(Demo.BALANCE)
            }
            Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NeonRingAvatar(h.name.take(1), 98.dp, 32.sp, ringing = true, host = h)
                Text(h.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 20.dp))
                Box(Modifier.padding(top = 8.dp)) { CallStatus(phase, live = phase == "Connecting") }
                Box(Modifier.padding(top = 12.dp)) { CallRateLine(com.vibe.app.data.Realtime.activeCall?.rate?.takeIf { r -> r > 0 } ?: h.rate, when { com.vibe.app.data.Realtime.activeCall?.kind == "random" -> "Random match · after pick-up"; com.vibe.app.data.Realtime.activeCall?.role == "host" -> "they pay · after pick-up"; else -> "after pick-up" }) }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().dockLine().padding(start = 40.dp, end = 40.dp, top = 18.dp, bottom = 30.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CallButton(CallGlyph.Flip, "Flip", { mirrored = !mirrored; if (live) com.vibe.app.data.CallEngine.flipCamera() })
                    CallButton(if (camOff) CallGlyph.VideoOff else CallGlyph.Video, "Camera", { camOff = !camOff; if (live) com.vibe.app.data.CallEngine.setCamera(!camOff) }, on = camOff)
                    CallButton(if (muted) CallGlyph.MicOff else CallGlyph.Mic, "Mute", { muted = !muted; if (live) com.vibe.app.data.CallEngine.setMic(!muted) }, on = muted)
                }
                Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    EndCallButton({ cancel() }, label = "Cancel")
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
