package com.vibe.app.ui.screens.calls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.CallResult
import com.vibe.app.data.Demo
import com.vibe.app.data.Gift
import com.vibe.app.ui.components.BalancePill
import com.vibe.app.ui.components.CallButton
import com.vibe.app.ui.components.CallIconButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CallPill
import com.vibe.app.ui.components.CallShade
import com.vibe.app.ui.components.CameraStandIn
import com.vibe.app.ui.components.EndCallButton
import com.vibe.app.ui.components.EndCallSheet
import com.vibe.app.ui.components.GiftBurst
import com.vibe.app.ui.components.GiftSheet
import com.vibe.app.ui.components.LowBalanceBanner
import com.vibe.app.ui.components.NeonRingAvatar
import com.vibe.app.ui.components.TopUpSheet
import com.vibe.app.ui.components.dockLine
import com.vibe.app.ui.components.formatCallTime
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class ChatLine(val who: String, val text: String) // who: me | them | sys

/**
 * Connected video call. DEMO billing only: the SERVER charges the host's video rate per minute,
 * applies the who-ended-the-call rule, pushes the balance and ends the call at zero.
 */
@Composable
fun VideoCallScreen(hostId: Int, onSwitchToVoice: (Int) -> Unit, onFinished: (CallResult) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val h = remember(hostId) { Demo.host(hostId) }
    var balance by remember { mutableIntStateOf(Demo.BALANCE) }
    var spent by remember { mutableIntStateOf(0) }
    var gifts by remember { mutableIntStateOf(0) }
    var secs by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var muted by remember { mutableStateOf(false) }
    var camOff by remember { mutableStateOf(false) }
    var mirrored by remember { mutableStateOf(false) }
    var followed by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var burst by remember { mutableStateOf<Pair<Gift, Int>?>(null) }
    var input by remember { mutableStateOf("") }
    val chat = remember { mutableStateListOf<ChatLine>() }
    val coinFloat = remember { Animatable(1f) }
    var lastCharge by remember { mutableIntStateOf(0) }

    fun charge(n: Int, label: String? = null) {
        balance -= n; spent += n; lastCharge = n
        if (label != null) chat.add(ChatLine("sys", label))
        scope.launch { coinFloat.snapTo(0f); coinFloat.animateTo(1f, tween(1400)) }
    }
    fun finish() { running = false; onFinished(CallResult(h.id, secs, spent, gifts, audio = false)) }
    fun send() {
        val t = input.trim(); if (t.isEmpty()) return
        chat.add(ChatLine("me", t)); input = ""
        scope.launch { delay(1300); chat.add(ChatLine("them", listOf("😂", "Sahi baat!", "Aur batao?", "❤️").random())) }
    }

    LaunchedEffect(Unit) {
        charge(h.rate) // first minute at pick-up
        delay(1500); chat.add(ChatLine("them", "Hiii 👋 kaise ho?"))
    }
    LaunchedEffect(running) {
        while (running) {
            delay(1000); secs++
            if (secs % 60 == 0) {
                charge(h.rate)
                if (balance < h.rate) { chat.add(ChatLine("sys", "Balance finished · call ending")); delay(1500); finish(); break }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        val density = LocalDensity.current
        val pipW = 96.dp; val pipH = 136.dp
        val maxX = with(density) { (maxWidth - pipW - 32.dp).toPx() }
        val maxY = with(density) { (maxHeight - pipH - 320.dp).toPx() }
        val pipX = remember { Animatable(0f) }  // 0 = right edge, -maxX = left edge
        val pipY = remember { Animatable(0f) }

        CameraStandIn(Color(0xFF4B3A5E), Color(0xFF120E18))
        CallShade()

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            // top bar
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.Top) {
                Row(
                    Modifier.clip(CircleShape).background(VibeColors.Dark.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                        .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeonRingAvatar(h.name.take(1), 38.dp, 13.sp)
                    Column(Modifier.padding(start = 8.dp, end = 6.dp)) {
                        Text(h.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Light, letterSpacing = 0.4.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(VibeColors.Green))
                            Text("  ${formatCallTime(secs)}", color = Color(0xFFC7C7D1), fontSize = 10.sp, letterSpacing = 1.2.sp)
                            Spacer(Modifier.width(5.dp))
                            listOf(4, 6, 10).forEach { bar ->
                                Box(Modifier.padding(end = 2.dp).width(2.dp).height(bar.dp).clip(RoundedCornerShape(1.dp)).background(VibeColors.Green))
                            }
                        }
                    }
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(if (followed) Color.White else Color.Transparent)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .clickable(enabled = !followed) { followed = true; chat.add(ChatLine("sys", "You followed ${h.name}")) },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (followed) "✓" else "+", color = if (followed) VibeColors.Dark else Color.White, fontSize = if (followed) 12.sp else 16.sp, fontWeight = FontWeight.Light) }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BalancePill(balance, h.rate)
                    CallPill(Modifier.clickable { running = false; ctx.toast("Switched to voice · ${h.audioRate} coins/min"); onSwitchToVoice(h.id) },
                        borderColor = VibeColors.Green.copy(alpha = 0.35f)) {
                        Icon(CallIcons.Phone, null, tint = VibeColors.Green, modifier = Modifier.size(14.dp))
                        Text("Switch to voice", color = VibeColors.Green, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            val left = balance / h.rate
            if (left < 3) LowBalanceBanner(left, { sheet = "topup" }, Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp))

            Spacer(Modifier.weight(1f))

            // chat feed
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 80.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chat.takeLast(4).forEach { line ->
                    Text(
                        buildAnnotatedString {
                            when (line.who) {
                                "sys" -> withStyle(SpanStyle(color = VibeColors.Gold)) { append(line.text) }
                                else -> {
                                    withStyle(SpanStyle(color = if (line.who == "me") VibeColors.Cyan else VibeColors.Pink, fontWeight = FontWeight.SemiBold)) {
                                        append(if (line.who == "me") "You " else "${h.name} ")
                                    }
                                    append(line.text)
                                }
                            }
                        },
                        color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(VibeColors.Dark.copy(alpha = 0.55f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(44.dp).clip(CircleShape).background(VibeColors.Dark.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (input.isEmpty()) Text("Say something…", color = Color(0xFF6B6B7B), fontSize = 14.sp, fontWeight = FontWeight.Light)
                    BasicTextField(
                        value = input, onValueChange = { input = it.take(80) }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = SolidColor(VibeColors.Pink),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(8.dp))
                CallIconButton(CallIcons.Send, { send() }, size = 44.dp)
            }

            // controls
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp).dockLine().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                CallButton(CallIcons.Mic, "Mute", { muted = !muted; ctx.toast(if (muted) "Mic off" else "Mic on") }, on = muted, small = true)
                CallButton(CallIcons.Camera, "Camera", { camOff = !camOff }, on = camOff, small = true)
                CallButton(CallIcons.Flip, "Flip", { mirrored = !mirrored; ctx.toast("Camera flipped") }, small = true)
                CallButton(CallIcons.Gift, "Gift", { sheet = "gift" }, pink = true, small = true)
                EndCallButton({ sheet = "end" }, small = true)
            }
        }

        // draggable self view, snaps to the nearest side
        Box(
            Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 132.dp)
                .offset { IntOffset(pipX.value.roundToInt(), pipY.value.roundToInt()) }
                .size(pipW, pipH).clip(RoundedCornerShape(18.dp))
                .background(if (camOff) Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card)) else Brush.radialGradient(listOf(Color(0xFF45435F), Color(0xFF15141F))))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .graphicsLayer { scaleX = if (mirrored) -1f else 1f }
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch { pipX.animateTo(if (pipX.value < -maxX / 2) -maxX else 0f, tween(250)) }
                            scope.launch { pipY.animateTo(pipY.value.coerceIn(0f, maxY), tween(250)) }
                        },
                    ) { change, drag ->
                        change.consume()
                        scope.launch { pipX.snapTo((pipX.value + drag.x).coerceIn(-maxX, 0f)) }
                        scope.launch { pipY.snapTo((pipY.value + drag.y).coerceIn(-40f, maxY)) }
                    }
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(if (camOff) "CAMERA OFF" else "YOU", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, letterSpacing = 1.4.sp,
                modifier = Modifier.padding(bottom = 6.dp).graphicsLayer { scaleX = if (mirrored) -1f else 1f })
        }

        // "-60" floating up after each charge
        if (coinFloat.value < 1f) Text(
            "−$lastCharge", color = VibeColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 88.dp)
                .graphicsLayer { translationY = -22.dp.toPx() * coinFloat.value; alpha = 1f - coinFloat.value },
        )

        GiftBurst(burst?.first, burst?.second ?: 1) { burst = null }
    }

    when (sheet) {
        "gift" -> GiftSheet(balance, { sheet = null }) { g, n ->
            sheet = null
            val total = g.price * n
            if (total > balance) { ctx.toast("Not enough coins"); sheet = "topup" }
            else {
                gifts += n; charge(total, "You sent ${if (n > 1) "$n× " else ""}${g.name}"); burst = g to n
                scope.launch { delay(1600); chat.add(ChatLine("them", "Thank you for the ${g.name}! 🥹")) }
            }
        }
        "topup" -> TopUpSheet({ sheet = null }) { coins -> sheet = null; balance += coins; chat.add(ChatLine("sys", "Recharge successful · +%,d".format(coins))) }
        "end" -> EndCallSheet("${formatCallTime(secs)} · %,d coins".format(spent), { sheet = null }) { sheet = null; finish() }
    }
}
