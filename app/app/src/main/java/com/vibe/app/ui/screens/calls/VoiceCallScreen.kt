package com.vibe.app.ui.screens.calls

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.CallResult
import com.vibe.app.data.Demo
import com.vibe.app.data.Gift
import com.vibe.app.ui.components.BalancePill
import com.vibe.app.ui.components.CallBackground
import com.vibe.app.ui.components.CallButton
import com.vibe.app.ui.components.CallIconButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CallRateLine
import com.vibe.app.ui.components.CallStatus
import com.vibe.app.ui.components.EncryptedLabel
import com.vibe.app.ui.components.EndCallButton
import com.vibe.app.ui.components.EndCallSheet
import com.vibe.app.ui.components.GiftBurst
import com.vibe.app.ui.components.GiftSheet
import com.vibe.app.ui.components.LowBalanceBanner
import com.vibe.app.ui.components.NeonRingAvatar
import com.vibe.app.ui.components.TopUpSheet
import com.vibe.app.ui.components.VibeIcons
import com.vibe.app.ui.components.dockLine
import com.vibe.app.ui.components.formatCallTime
import com.vibe.app.ui.components.rememberVoiceLevel
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * WhatsApp-style voice call: Calling → Ringing → live timer on one screen.
 * DEMO billing only: the server rings the host, charges the host's voice rate per minute after pick-up,
 * applies the who-ended-the-call rule and ends the call at zero balance. The app just shows it.
 */
@Composable
fun VoiceCallScreen(
    hostId: Int,
    startConnected: Boolean,
    onSwitchToVideo: (Int) -> Unit,
    onFinished: (CallResult?) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val h = remember(hostId) { Demo.host(hostId) }
    var balance by remember { mutableIntStateOf(Demo.BALANCE) }
    var spent by remember { mutableIntStateOf(0) }
    var gifts by remember { mutableIntStateOf(0) }
    var secs by remember { mutableIntStateOf(0) }
    var connected by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("Calling…") }
    var speaker by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var burst by remember { mutableStateOf<Pair<Gift, Int>?>(null) }
    val level = rememberVoiceLevel(connected)

    fun charge(n: Int) { balance -= n; spent += n }
    fun finish() = onFinished(if (connected) CallResult(h.id, secs, spent, gifts, audio = true) else null)

    // calling -> ringing -> connected
    LaunchedEffect(Unit) {
        if (!startConnected) { delay(1500); phase = "Ringing…"; delay(2500) }
        connected = true
        charge(h.audioRate)
    }
    // per-minute billing while connected
    LaunchedEffect(connected) {
        while (connected) {
            delay(1000); secs++
            if (secs % 60 == 0) {
                charge(h.audioRate)
                if (balance < h.audioRate) { ctx.toast("Balance finished · call ending"); delay(1200); finish(); break }
            }
        }
    }

    CallBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                CallIconButton(CallIcons.Chevron, { ctx.toast("Call keeps running in the background") })
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { EncryptedLabel() }
                BalancePill(balance)
            }

            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Box(contentAlignment = Alignment.Center) {
                    // voice activity rings
                    listOf(0.14f to VibeColors.Pink, 0.3f to VibeColors.Cyan).forEach { (s, c) ->
                        Box(
                            Modifier.size(138.dp).graphicsLayer {
                                val k = 1f + s * level; scaleX = k; scaleY = k; alpha = if (level > 0.35f) 0.9f else 0f
                            }.border(1.dp, c.copy(alpha = 0.5f), CircleShape),
                        )
                    }
                    NeonRingAvatar(h.name.take(1), 138.dp, 48.sp, ringing = !connected)
                }
                Text(h.name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 32.dp))
                Box(Modifier.padding(top = 12.dp).height(20.dp)) {
                    when {
                        asking -> CallStatus("Asking to switch to video…", color = VibeColors.Cyan)
                        connected -> CallStatus(formatCallTime(secs), live = true)
                        else -> CallStatus(phase)
                    }
                }
                Box(Modifier.padding(top = 16.dp)) { CallRateLine(h.audioRate, if (connected) "charging per minute" else "after pick-up") }
                val left = balance / h.audioRate
                if (connected && left < 3) LowBalanceBanner(left, { sheet = "topup" }, Modifier.padding(top = 24.dp))
            }

            Column(Modifier.fillMaxWidth().dockLine().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 30.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CallButton(CallIcons.Speaker, "Speaker", { speaker = !speaker; ctx.toast(if (speaker) "Speaker on" else "Speaker off") }, on = speaker)
                    CallButton(VibeIcons.Video, "Video", {
                        if (!connected) ctx.toast("Wait until they pick up")
                        else if (!asking) scope.launch {
                            asking = true; delay(2000)
                            ctx.toast("${h.name} turned on video · ${h.rate} coins/min")
                            onSwitchToVideo(h.id)
                        }
                    })
                    CallButton(CallIcons.Mic, "Mute", { muted = !muted; ctx.toast(if (muted) "Muted" else "Unmuted") }, on = muted)
                    CallButton(CallIcons.Gift, "Gift", { if (connected) sheet = "gift" else ctx.toast("You can send gifts once connected") }, pink = true)
                }
                Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    EndCallButton({
                        if (connected) sheet = "end" else { ctx.toast("Call cancelled · no coins charged"); onFinished(null) }
                    })
                }
            }
        }
        GiftBurst(burst?.first, burst?.second ?: 1) { burst = null }
    }

    when (sheet) {
        "gift" -> GiftSheet(balance, { sheet = null }) { g, n ->
            sheet = null
            val total = g.price * n
            if (total > balance) { ctx.toast("Not enough coins"); sheet = "topup" }
            else { charge(total); gifts += n; burst = g to n }
        }
        "topup" -> TopUpSheet({ sheet = null }) { coins -> sheet = null; balance += coins; ctx.toast("+%,d coins added".format(coins)) }
        "end" -> EndCallSheet("${formatCallTime(secs)} · %,d coins".format(spent), { sheet = null }) { sheet = null; finish() }
    }
}
