package com.vibe.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import kotlinx.coroutines.launch
import com.vibe.app.data.MeDemo
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

private val DarkCard = Color(0xFFFFFFFF)

/** Daily check-in: 6 coin days + a full-width Day 7 jackpot. The server decides the day (India time) and the coins (admin settings). */
@Composable
fun CheckInScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var rewards by remember { mutableStateOf(List(7) { 0 }) }
    var today by remember { mutableStateOf(0) }            // index of today's box
    var collected by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var gained by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        runCatching { com.vibe.app.data.api.Api.get("/checkin") }.onSuccess { r ->
            val a = r.getJSONArray("rewards"); rewards = List(a.length()) { a.getInt(it) }
            collected = r.optBoolean("collectedToday")
            val done = r.optInt("done")
            today = if (collected) (done - 1).coerceAtLeast(0) else done.coerceAtMost(6)
            loaded = true
        }.onFailure { ctx.toast(it.message ?: "Could not load the check-in") }
    }
    val fly = remember { Animatable(0f) }
    var flying by remember { mutableStateOf(false) }
    LaunchedEffect(flying) {
        if (flying) {
            fly.snapTo(0f)
            fly.animateTo(1f, tween(1400))
            flying = false
        }
    }
    val floatY by rememberFloat()

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopEnd).offset(90.dp, (-100).dp))
        GlowBlob(VibeColors.Cyan, 260.dp, Modifier.align(Alignment.BottomStart).offset((-100).dp, 40.dp))

        Column(Modifier.fillMaxSize()) {
            PageHeader("Daily check-in", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp)) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ghost_empty), null, Modifier.size(112.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Text(
                        if (!loaded) "Daily check-in" else if (collected) "Day ${today + 1} collected" else "Day ${today + 1} today", color = VibeColors.Ink, fontSize = 24.sp,
                        fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 4.dp),
                    )
                    Text("Free coins every day. No purchase needed.", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (row in 0..1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (col in 0..2) {
                                val i = row * 3 + col
                                val done = i < today || (i == today && collected)
                                DayCard(
                                    label = "Day ${i + 1}", coins = rewards[i], done = done, isToday = i == today && !collected,
                                    coinSize = if (i < 2) 36.dp else if (i < 4) 40.dp else 48.dp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    val jackDone = 6 < today || (6 == today && collected)
                    JackpotCard(rewards[6], jackDone, isToday = 6 == today && !collected)
                }

                Row(
                    Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFE3EF))
                        .border(1.dp, VibeColors.Pink.copy(alpha = 0.25f), RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(CallIcons.Gift, null, tint = VibeColors.Pink, modifier = Modifier.size(24.dp))
                    Text("Miss a day and the streak restarts from Day 1.", color = Color(0xFF5A3F4D), fontSize = 14.sp, lineHeight = 19.sp)
                }
                Text(
                    "Rewards are decided by VIBE and added by the server.", color = VibeColors.TextMute, fontSize = 11.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }

            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton(
                    text = when { !loaded -> "Loading…"; collected -> "Come back tomorrow"; sending -> "Collecting…"; else -> "Collect today's ${rewards[today]} coins" },
                    enabled = loaded && !collected && !sending,
                    onClick = {
                        sending = true
                        scope.launch {
                            runCatching { com.vibe.app.data.api.Api.post("/checkin") }.onSuccess { r ->
                                gained = r.optInt("coins"); collected = true; flying = true
                                com.vibe.app.data.WalletRepo.setCoins(r.optInt("balance"))
                                ctx.toast("+$gained coins added to your wallet")
                            }.onFailure { ctx.toast(it.message ?: "Could not collect") }
                            sending = false
                        }
                    },
                )
            }
        }

        if (flying) {
            Text(
                "+$gained", color = VibeColors.Gold, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center).graphicsLayer {
                    alpha = 1f - fly.value
                    translationY = -120.dp.toPx() * fly.value
                    val s = 0.8f + 0.6f * fly.value
                    scaleX = s
                    scaleY = s
                },
            )
        }
    }
}

@Composable
private fun DayCard(label: String, coins: Int, done: Boolean, isToday: Boolean, coinSize: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    val t = rememberInfiniteTransition(label = "day")
    val nudge by t.animateFloat(0f, -3f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "nudge")
    val spin by t.animateFloat(0f, 180f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "spin")
    val pinkGrad = Brush.linearGradient(listOf(VibeColors.Pink, VibeColors.Cyan))
    val border = if (done) VibeColors.Pink.copy(alpha = 0.35f) else if (isToday) Color.Transparent else VibeColors.Line
    val fill = when {
        isToday -> pinkGrad
        done -> Brush.verticalGradient(listOf(Color(0xFFFFE3EF), Color(0xFFFFF0F6)))
        else -> Brush.verticalGradient(listOf(Color.White, Color.White))
    }
    Column(
        modifier.graphicsLayer { if (isToday) translationY = nudge.dp.toPx() }
            .clip(shape).background(fill).border(1.5.dp, border, shape).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (isToday) "TODAY" else label.uppercase(), letterSpacing = 1.6.sp, fontSize = 10.sp,
            color = if (isToday) Color.White else VibeColors.TextMute, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
        )
        Box(Modifier.padding(top = 6.dp, bottom = 4.dp).fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.alpha(if (done) 0.5f else 1f).graphicsLayer { if (isToday) rotationY = spin }) { CoinDot(coinSize) }
            if (done) Tick(Modifier.align(Alignment.BottomEnd))
        }
        Text(
            if (done) "Collected" else "+$coins", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
            color = if (done) VibeColors.Pink else if (isToday) Color.White else VibeColors.Gold,
        )
    }
}

@Composable
private fun JackpotCard(coins: Int, done: Boolean, isToday: Boolean) {
    val shape = RoundedCornerShape(22.dp)
    val fill = if (done) Brush.horizontalGradient(listOf(Color(0xFFFFE3EF), Color(0xFFFFF0F6)))
    else Brush.linearGradient(listOf(VibeColors.Pink, VibeColors.Cyan))
    val ink = if (done) VibeColors.Ink else Color.White
    Box(Modifier.fillMaxWidth().clip(shape).background(fill).border(1.5.dp, if (done) VibeColors.Pink.copy(alpha = 0.35f) else Color.Transparent, shape)) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(if (done) Color.White else Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Icon(CallIcons.Gift, null, tint = if (done) VibeColors.Pink else Color.White, modifier = Modifier.size(34.dp).alpha(if (done) 0.6f else 1f))
                if (done) Tick(Modifier.align(Alignment.BottomEnd))
            }
            Column {
                Text("DAY 7 · STREAK BONUS", color = ink.copy(alpha = 0.8f), fontSize = 10.sp, letterSpacing = 1.6.sp)
                Text(
                    if (done) "Collected" else "+$coins coins", color = if (done) VibeColors.Pink else Color.White,
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                )
                Text(if (done) "See you next week" else "Check in 7 days in a row", color = ink.copy(alpha = 0.85f), fontSize = 11.sp)
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd)
                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomStart = 12.dp, bottomEnd = 0.dp))
                .background(Color.White)
                .padding(horizontal = 9.dp, vertical = 3.dp),
        ) { Text("JACKPOT", color = VibeColors.Pink, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
    }
}

@Composable
private fun Tick(modifier: Modifier = Modifier) {
    Box(modifier.size(22.dp).clip(CircleShape).background(VibeColors.Pink).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
        Icon(MeIcons.Tick, null, tint = Color.White, modifier = Modifier.size(13.dp))
    }
}
