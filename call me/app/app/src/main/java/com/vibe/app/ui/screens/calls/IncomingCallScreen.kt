package com.vibe.app.ui.screens.calls

import com.vibe.app.ui.components.CallDark
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
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.launch

/** What a host sees when someone calls them. Earnings and the host's rates are decided by the server. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomingCallScreen(callerId: Int, onAccept: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val h = remember(callerId) { Demo.host(callerId) }
    // the rate of THIS call (the host's own rate, or the fixed Random match rate) and the host's share, both from the server
    val myRate: Int = com.vibe.app.data.Realtime.activeCall?.rate?.takeIf { r -> r > 0 } ?: 60
    val isRandom = com.vibe.app.data.Realtime.activeCall?.kind == "random"
    val iAmHost = com.vibe.app.data.Realtime.activeCall?.role != "user"   // "user" = a host is calling me
    var sharePct by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) { runCatching { com.vibe.app.data.api.Api.get("/config", auth = false).getJSONObject("config").optInt("host_share_pct", 60) }.onSuccess { sharePct = it } }
    var left by remember { mutableIntStateOf(30) }
    // a call from the server: the server runs the 30 second ring and tells us if the caller gives up
    val callId = remember { com.vibe.app.data.Realtime.activeCallId }
    val live = h.real && callId != null
    var closed by remember { mutableStateOf(false) }
    fun leave() { if (!closed) { closed = true; onDone() } }
    // a user picking up a host's call: wait for the server's yes (it checks the coins for the first minute)
    var accepting by remember { mutableStateOf(false) }
    var myCoins by remember { mutableIntStateOf(Demo.BALANCE) }
    LaunchedEffect(iAmHost) {
        if (!iAmHost) runCatching { com.vibe.app.data.api.Api.get("/me") }.onSuccess { com.vibe.app.data.api.AuthStore.saveUser(it.getJSONObject("user")); myCoins = Demo.BALANCE }
    }
    var showTopUp by remember { mutableStateOf(false) }
    var toppingAsked by remember { mutableStateOf(false) }
    fun askRecharge() {
        showTopUp = true
        if (!toppingAsked && callId != null) { toppingAsked = true; com.vibe.app.data.Realtime.toppingUp(callId) }   // rings longer meanwhile
    }

    LaunchedEffect(Unit) {
        if (live) {
            com.vibe.app.data.Realtime.events.collect { e ->
                when {
                    e is com.vibe.app.data.LiveEvent.Ended && e.callId == callId -> {
                        if (!closed) ctx.toast(if (e.reason == "cancelled") "${h.name} cancelled the call" else "Missed call"); leave()
                    }
                    e is com.vibe.app.data.LiveEvent.Accepted && e.callId == callId && accepting && !closed -> { closed = true; onAccept() }
                    e is com.vibe.app.data.LiveEvent.NeedCoins && e.callId == callId -> { accepting = false; askRecharge() }
                    e is com.vibe.app.data.LiveEvent.Topping && e.callId == callId -> left = e.seconds
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        while (left > 0) { delay(1000); left-- }
        if (!live && !closed) leave()   // no call from the server: nothing to show
    }
    // camera + microphone first; then the server starts the call (and the first minute)
    val askPerms = rememberCallPermissionAsker { ok ->
        // camera first (it takes a moment to start), then tell the server; the connection is already built while it rang
        if (ok && callId != null) {
            if (!iAmHost && myCoins < myRate) { askRecharge(); return@rememberCallPermissionAsker }   // not enough for the first minute
            if (iAmHost) { com.vibe.app.data.CallEngine.startLocal(ctx); com.vibe.app.data.Realtime.acceptCall(callId); onAccept() }
            else { accepting = true; com.vibe.app.data.Realtime.acceptCall(callId) }   // a user goes on when the server says yes (or asks to recharge)
        } else ctx.toast("Allow the camera and microphone to take video calls")
    }
    fun decline(msg: String) { if (live && callId != null) com.vibe.app.data.Realtime.declineCall(callId); ctx.toast(msg); leave() }
    androidx.activity.compose.BackHandler { decline("Call declined") }   // back while it rings = decline

    CallBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (iAmHost) CallPill(borderColor = VibeColors.Green.copy(alpha = 0.35f)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF7CFFB2)))
                    Text("Online as host", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                } else com.vibe.app.ui.components.BalancePill(myCoins)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { EncryptedLabel() }
                CallPill { Text("${left}s", color = Color(0xFFFFE3EF), fontSize = 12.sp) }
            }

            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(40.dp))
                Text(if (isRandom) "RANDOM MATCH · VIDEO CALL" else "INCOMING VIDEO CALL", color = Color(0xFFFFC2DD), fontSize = 11.sp, letterSpacing = 1.8.sp)
                NeonRingAvatar(h.name.take(1), 138.dp, 48.sp, ringing = true, modifier = Modifier.padding(top = 28.dp), host = h)
                Text(com.vibe.app.ui.components.nameAge(h.name, h.age), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 28.dp))
                FlowRow(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // only what the caller really has (a new user may have no city or languages yet)
                    listOfNotNull(if (h.vip) "★ VIP" else null, h.city.takeIf { it.isNotBlank() }, "Lv.${h.level}", h.langs.firstOrNull()).forEach { Chip(it) }
                }
                if (!iAmHost) Box(Modifier.padding(top = 24.dp)) { com.vibe.app.ui.components.CallRateLine(myRate, "after pick-up") }
                if (!iAmHost && myCoins < myRate) Text(
                    "You need ${myRate - myCoins} more coins to pick up", color = Color(0xFFFFD166), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (iAmHost) Row(Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CoinDot(12.dp)
                    Text("  YOU EARN ", color = Color(0xFFFFB3D1), fontSize = 10.sp, letterSpacing = 1.sp)
                    Text("$myRate", color = Color(0xFFFFD166), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text(" / MIN · KEEP $sharePct%", color = Color(0xFFFFB3D1), fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }

            Column(Modifier.fillMaxWidth().dockLine().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 30.dp)) {
                if (iAmHost) FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Busy, call later 🙏", "Give me 5 min", "Text me instead").forEach { msg ->
                        Chip(msg) {
                            // the quick reply really goes to the caller's chat (the screen closes, so it is sent from an outside scope)
                            val to = callerId
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                runCatching { com.vibe.app.data.ChatRepo.send(to, msg, java.util.UUID.randomUUID().toString()) }
                                    .onFailure { ctx.toast("Could not send the message") }
                            }
                            decline("Declined · sent \"$msg\"")
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 28.dp, start = 24.dp, end = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    EndCallButton({ decline("Call declined") }, label = "Decline")
                    AcceptCallButton(VibeIcons.Video, { askPerms() })
                }
            }
        }
    }
    if (showTopUp) com.vibe.app.ui.components.TopUpSheet({ showTopUp = false }, ringing = true) { _ ->
        showTopUp = false; myCoins = Demo.BALANCE
        ctx.toast("Coins added · tap Accept to pick up")
    }
}

@Composable
private fun Chip(text: String, onClick: (() -> Unit)? = null) {
    Text(
        text, color = Color(0xFFFFE3EF), fontSize = 11.sp,
        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.22f)).border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
