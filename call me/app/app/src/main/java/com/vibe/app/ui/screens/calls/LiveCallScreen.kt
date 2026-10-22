package com.vibe.app.ui.screens.calls

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vibe.app.data.CallEngine
import com.vibe.app.data.CallResult
import com.vibe.app.data.Demo
import com.vibe.app.data.HostRepo
import com.vibe.app.data.LiveEvent
import com.vibe.app.data.Realtime
import com.vibe.app.data.VideoView
import com.vibe.app.ui.components.BalancePill
import com.vibe.app.ui.components.CallButton
import com.vibe.app.ui.components.CallDark
import com.vibe.app.ui.components.CallGlyph
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CallPill
import com.vibe.app.ui.components.CallStatus
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.EndCallButton
import com.vibe.app.ui.components.EndCallSheet
import com.vibe.app.ui.components.GiftSheet
import com.vibe.app.ui.components.LowBalanceBanner
import com.vibe.app.ui.components.NeonRingAvatar
import com.vibe.app.ui.components.TopUpSheet
import com.vibe.app.ui.components.formatCallTime
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Camera + microphone allowed? (needed before a call can show video). */
fun hasCallPermissions(ctx: android.content.Context) = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    .all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }

/** Asks for camera + microphone; [onResult] true when both are allowed. */
@Composable
fun rememberCallPermissionAsker(onResult: (Boolean) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onResult(hasCallPermissions(ctx)) }
    return { if (hasCallPermissions(ctx)) onResult(true) else launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }
}


/**
 * A real video call. The video goes phone to phone (WebRTC); the clock and the coins are the server's: it charges at the start of
 * every minute and tells this screen (balance, earned, low balance). When the call ends [onFinished] gets the server's numbers.
 */
@Composable
fun LiveCallScreen(peerId: Int, onFinished: (CallResult, String?) -> Unit, onMinimize: () -> Unit = {}) {
    val ctx = LocalContext.current
    // this call's details, taken once when the screen opens (they are cleared from Realtime the moment the call ends,
    // but the screen still needs them until the server's final numbers have arrived)
    val opened = remember { Realtime.activeCall }
    val h = HostRepo.cache[peerId] ?: Demo.host(peerId)
    // no call at all (it ended before this screen opened): leave at once
    if (opened == null) { LaunchedEffect(Unit) { onFinished(CallResult(peerId, 0, 0, 0), null) }; return }
    val call = opened
    val iAmHost = call.role == "host"
    val callId = call.callId
    var seconds by remember { mutableIntStateOf(0) }
    // start from the minute already charged at pick-up (it arrives before this screen opens)
    val firstBill = Realtime.lastBilling?.takeIf { it.callId == Realtime.activeCall?.callId }
    var balance by remember { mutableIntStateOf(firstBill?.coinsLeft ?: Demo.BALANCE) }
    var earned by remember { mutableIntStateOf(firstBill?.earned ?: 0) }
    var spent by remember { mutableIntStateOf(firstBill?.coins ?: 0) }
    var low by remember { mutableStateOf(firstBill?.low ?: false) }
    var lost by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    remember(call.callId) { Realtime.callMemory(call.callId) }
    val lines = Realtime.callLines
    var finished by remember { mutableStateOf(false) }
    var swapped by remember { mutableStateOf(false) }   // true = I am big, the other person small
    var followed by remember { mutableStateOf(h.following) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun finish(r: CallResult) { if (!finished) { finished = true; onFinished(r, callId) } }

    // connect the video: the user's phone makes the offer, the host's phone answers
    LaunchedEffect(callId) {
        CallEngine.connect(ctx, callId, isCaller = !iAmHost)
        com.vibe.app.data.CallService.start(ctx, h.name)   // keeps the call running if the app goes to the background
    }
    // the clock shown here starts at pick-up (the server's clock is the one that charges)
    LaunchedEffect(callId) {
        val start = call.startedAt ?: System.currentTimeMillis()
        while (true) { seconds = ((System.currentTimeMillis() - start) / 1000).toInt(); delay(500) }
    }
    LaunchedEffect(callId) {
        Realtime.events.collect { e ->
            when {
                e is LiveEvent.Billing && e.callId == callId -> {
                    if (iAmHost) earned = e.earned else { spent = e.coins; e.coinsLeft?.let { balance = it }; low = e.low }
                }
                e is LiveEvent.PeerLost && e.callId == callId -> lost = true
                e is LiveEvent.PeerBack && e.callId == callId -> lost = false
                e is LiveEvent.Earned && e.callId == callId -> if (iAmHost) earned = e.earned
                // host: everything earned (minutes + gifts) and gifts received; user: coins spent and gifts sent
                e is LiveEvent.Ended && e.callId == callId -> finish(CallResult(peerId, e.seconds, if (iAmHost) e.earned else e.coins, if (iAmHost) e.gifts else Realtime.callGifts))
            }
        }
    }
    // the latest bill as state: a bill that came while this screen was opening (pick-up = first minute) is never missed
    LaunchedEffect(Realtime.lastBilling) {
        val b = Realtime.lastBilling ?: return@LaunchedEffect
        if (b.callId != callId) return@LaunchedEffect
        if (iAmHost) earned = maxOf(earned, b.earned) else { spent = b.coins; b.coinsLeft?.let { balance = it }; low = b.low }
    }
    // keep the screen on during the call
    val activity = ctx as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    // back: the call gets small (a bar at the top of every screen) and keeps running; the red button ends it
    BackHandler(enabled = sheet == null) { onMinimize() }
    com.vibe.app.ui.components.CallStatusBarIcons()

    // the small window: dragged anywhere between the top bar and the buttons, then snaps to the nearer side
    var screen by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var pipSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var pip by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }   // from its first spot (top right)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pipAnim by androidx.compose.animation.core.animateOffsetAsState(pip, label = "pip")
    var dragging by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(CallDark).onSizeChanged { screen = it }) {
        // big: the other person (or me after a tap on the small window); small: the other one. Tap the small one to swap.
        val remoteReady = CallEngine.remoteShowing && CallEngine.remoteVideo != null
        VideoView(Modifier.fillMaxSize(), remote = !swapped, mirror = swapped && CallEngine.frontCamera)
        if (!swapped && !remoteReady) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            NeonRingAvatar(h.name.take(1), 110.dp, 36.sp, ringing = true, host = h)
            Box(Modifier.padding(top = 16.dp)) { CallStatus(if (lost) "Reconnecting…" else "Connecting video…") }
        }
        if (swapped && !CallEngine.cameraOn) Box(Modifier.align(Alignment.Center)) { CallStatus("Your camera is off") }
        Box(
            Modifier.systemBarsPadding().padding(top = 70.dp, end = 14.dp).align(Alignment.TopEnd)
                .offset { val o = if (dragging) pip else pipAnim; androidx.compose.ui.unit.IntOffset(o.x.toInt(), o.y.toInt()) }
                .size(104.dp, 150.dp).onSizeChanged { pipSize = it }
                .pointerInput(Unit) {
                    fun clamp(o: androidx.compose.ui.geometry.Offset): androidx.compose.ui.geometry.Offset {
                        val side = with(density) { 14.dp.toPx() }
                        val minX = -(screen.width - pipSize.width - 2 * side).toFloat()
                        val bottomRoom = with(density) { 330.dp.toPx() }      // keep clear of the chat box and buttons
                        val maxY = (screen.height - pipSize.height - bottomRoom).coerceAtLeast(0f)
                        return androidx.compose.ui.geometry.Offset(o.x.coerceIn(minX.coerceAtMost(0f), 0f), o.y.coerceIn(0f, maxY))
                    }
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            val minX = -(screen.width - pipSize.width - 2 * with(density) { 14.dp.toPx() }).toFloat()
                            pip = pip.copy(x = if (pip.x < minX / 2) minX else 0f)   // snap to the nearer side
                        },
                        onDragCancel = { dragging = false },
                    ) { change, drag -> change.consume(); pip = clamp(pip + drag) }
                }
                .clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.35f))
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .noRippleClick { swapped = !swapped },
            contentAlignment = Alignment.Center,
        ) {
            VideoView(Modifier.fillMaxSize(), remote = swapped, mirror = !swapped && CallEngine.frontCamera)
            // what the small window shows when there is no picture for it
            if (!swapped && !CallEngine.cameraOn) Text("Camera off", color = Color.White, fontSize = 11.sp)
            if (swapped && !remoteReady) NeonRingAvatar(h.name.take(1), 56.dp, 18.sp, host = h)
        }

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            // top: who + clock (the same design as before), money on the right
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.Top) {
                Row(
                    Modifier.clip(CircleShape).background(CallDark.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                        .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeonRingAvatar(h.name.take(1), 38.dp, 13.sp, host = h)
                    Column(Modifier.padding(start = 8.dp, end = 6.dp)) {
                        Text(h.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Light, letterSpacing = 0.4.sp, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // green = video connected, pink = still connecting / reconnecting
                            val live = CallEngine.connected && !lost
                            val dot = if (live) VibeColors.Green else Color(0xFFFFC2DD)
                            Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
                            Text("  ${formatCallTime(seconds)}", color = Color(0xFFFFE3EF), fontSize = 10.sp, letterSpacing = 1.2.sp)
                            androidx.compose.foundation.layout.Spacer(Modifier.size(5.dp))
                            listOf(4, 6, 10).forEach { bar ->
                                Box(Modifier.padding(end = 2.dp).size(width = 2.dp, height = bar.dp).clip(RoundedCornerShape(1.dp)).background(dot))
                            }
                        }
                    }
                    // follow from the call (users follow hosts)
                    if (!iAmHost) Box(
                        Modifier.size(28.dp).clip(CircleShape).background(if (followed) Color.White else Color.Transparent)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .noRippleClick {
                                if (!followed) { followed = true; scope.launch { runCatching { HostRepo.follow(peerId, true) } } }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (followed) "✓" else "+", color = if (followed) CallDark else Color.White, fontSize = if (followed) 12.sp else 16.sp, fontWeight = FontWeight.Light) }
                }
                Box(Modifier.weight(1f))
                if (iAmHost) CallPill { CoinDot(14.dp); Text(" +%,d earned".format(earned), color = Color(0xFFFFD166), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                else BalancePill(balance, call.rate)
            }
            if (lost) Text(
                "Connection lost · waiting for ${h.name}…", color = Color.White, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 14.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            if (!iAmHost && low) LowBalanceBanner(0, { sheet = "topup" }, Modifier.padding(start = 14.dp, end = 130.dp, top = 6.dp))

            Box(Modifier.weight(1f))

            // short messages inside the call
            Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lines.forEach { l ->
                    Text(
                        (if (l.mine) "You: " else "${h.name}: ") + l.text, color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (input.isEmpty()) Text("Say something…", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    BasicTextField(
                        input, { input = it.take(300) }, singleLine = true, textStyle = TextStyle(color = Color.White, fontSize = 13.sp), cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { val t = input.trim(); if (t.isNotEmpty()) { Realtime.callChat(callId, t); Realtime.addCallLine(callId, true, t); input = "" } }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(
                    Modifier.padding(start = 8.dp).size(42.dp).clip(CircleShape).background(Color.White).noRippleClick {
                        val t = input.trim(); if (t.isNotEmpty()) { Realtime.callChat(callId, t); Realtime.addCallLine(callId, true, t); input = "" }
                    },
                    contentAlignment = Alignment.Center,
                ) { Icon(CallGlyph.Send, "Send", tint = VibeColors.Pink, modifier = Modifier.size(18.dp)) }
            }

            // controls
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CallButton(if (CallEngine.micOn) CallGlyph.Mic else CallGlyph.MicOff, "Mute", { CallEngine.setMic(!CallEngine.micOn) }, on = !CallEngine.micOn, small = true)
                CallButton(if (CallEngine.cameraOn) CallGlyph.Video else CallGlyph.VideoOff, "Camera", { CallEngine.setCamera(!CallEngine.cameraOn) }, on = !CallEngine.cameraOn, small = true)
                CallButton(CallGlyph.Flip, "Flip", { CallEngine.flipCamera() }, small = true)
                CallButton(CallIcons.News, "Speaker", { CallEngine.setSpeaker(!CallEngine.speakerOn) }, on = !CallEngine.speakerOn, small = true)
                if (!iAmHost) CallButton(CallGlyph.Gift, "Gift", { sheet = "gift" }, pink = true, small = true)
                EndCallButton({ sheet = "end" }, small = true)
            }
        }
    }

    when (sheet) {
        "gift" -> GiftSheet(peerId, "call", onDismiss = { sheet = null }, onNeedCoins = { sheet = "topup" }) { g, n ->
            Realtime.callGifts += n; balance = Demo.BALANCE; rememberBalance(callId, balance)
            Realtime.addCallLine(callId, true, "sent ${if (n > 1) "$n× " else "a "}${g.name} 🎁")
        }
        "topup" -> TopUpSheet({ sheet = null }) { _ -> sheet = null; balance = Demo.BALANCE; low = false; rememberBalance(callId, balance) }
        "end" -> EndCallSheet(
            if (iAmHost) "${formatCallTime(seconds)} · +%,d earned".format(earned) else "${formatCallTime(seconds)} · %,d coins".format(spent),
            { sheet = null },
        ) { sheet = null; Realtime.endCall(callId) }
    }
}

/** A gift or a top-up changed the coins mid-minute: keep it in the last bill, which a reopened call screen starts from. */
private fun rememberBalance(callId: String, coins: Int) {
    Realtime.lastBilling = Realtime.lastBilling?.takeIf { it.callId == callId }?.copy(coinsLeft = coins) ?: Realtime.lastBilling
}
