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

private val DarkCard = Color(0xFF10101A)

/** Daily check-in: 6 coin days + a full-width Day 7 jackpot. DEMO; the server decides the day and the coins. */
@Composable
fun CheckInScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val rewards = MeDemo.checkInRewards
    val today = MeDemo.CHECKIN_TODAY
    var collected by remember { mutableStateOf(false) }
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
        GlowBlob(VibeColors.Green, 280.dp, Modifier.align(Alignment.TopEnd).offset(90.dp, (-100).dp))
        GlowBlob(VibeColors.Gold, 260.dp, Modifier.align(Alignment.BottomStart).offset((-100).dp, 40.dp))

        Column(Modifier.fillMaxSize()) {
            PageHeader("Daily check-in", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp)) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ghost_empty), null, Modifier.size(112.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Text(
                        "Day ${if (collected) today + 1 else today} streak", color = Color.White, fontSize = 24.sp,
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
                    Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VibeColors.Gold.copy(alpha = 0.05f))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.30f), RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(CallIcons.Gift, null, tint = VibeColors.Gold, modifier = Modifier.size(24.dp))
                    Text("Miss a day and the streak restarts from Day 1.", color = Color(0xFFD1D5DB), fontSize = 14.sp, lineHeight = 19.sp)
                }
                Text(
                    "Rewards are decided by VIBE and added by the server.", color = VibeColors.TextMute, fontSize = 11.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }

            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton(
                    text = if (collected) "Come back tomorrow" else "Collect today's ${rewards[today]} coins",
                    enabled = !collected,
                    onClick = {
                        collected = true
                        flying = true
                        ctx.toast("Coins added to your wallet")
                    },
                )
            }
        }

        if (flying) {
            Text(
                "+${rewards[today]}", color = VibeColors.Gold, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
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
    val border = if (done) VibeColors.Green.copy(alpha = 0.40f) else if (isToday) VibeColors.Pink else VibeColors.Line
    val fill = when {
        done -> Brush.verticalGradient(listOf(VibeColors.Green.copy(alpha = 0.14f), DarkCard))
        isToday -> Brush.verticalGradient(listOf(VibeColors.Pink.copy(alpha = 0.22f), DarkCard))
        else -> Brush.verticalGradient(listOf(Color(0xFF171724), DarkCard))
    }
    Column(
        modifier.graphicsLayer { if (isToday) translationY = nudge.dp.toPx() }
            .clip(shape).background(fill).border(1.5.dp, border, shape).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (isToday) "TODAY" else label.uppercase(), letterSpacing = 1.6.sp, fontSize = 10.sp,
            color = if (isToday) VibeColors.Pink else VibeColors.TextMute, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
        Box(Modifier.padding(top = 6.dp, bottom = 4.dp).fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.alpha(if (done) 0.45f else 1f).graphicsLayer { if (isToday) rotationY = spin }) { CoinDot(coinSize) }
            if (done) Tick(Modifier.align(Alignment.BottomEnd))
        }
        Text(
            if (done) "Collected" else "+$coins", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
            color = if (done) VibeColors.Green else VibeColors.Gold,
        )
    }
}

@Composable
private fun JackpotCard(coins: Int, done: Boolean, isToday: Boolean) {
    val shape = RoundedCornerShape(22.dp)
    val fill = if (done) Brush.horizontalGradient(listOf(VibeColors.Green.copy(alpha = 0.16f), DarkCard))
    else Brush.horizontalGradient(listOf(VibeColors.Gold.copy(alpha = 0.22f), VibeColors.Pink.copy(alpha = 0.10f), DarkCard))
    val border = if (done) VibeColors.Green.copy(alpha = 0.40f) else if (isToday) VibeColors.Pink else VibeColors.Gold.copy(alpha = 0.50f)
    Box(Modifier.fillMaxWidth().clip(shape).background(fill).border(1.5.dp, border, shape)) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Icon(CallIcons.Gift, null, tint = VibeColors.Gold, modifier = Modifier.size(40.dp).alpha(if (done) 0.45f else 1f))
                if (done) Tick(Modifier.align(Alignment.BottomEnd))
            }
            Column {
                Text("DAY 7 · STREAK BONUS", color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.6.sp)
                Text(
                    if (done) "Collected" else "+$coins coins", color = if (done) VibeColors.Green else VibeColors.Gold,
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                )
                Text(if (done) "See you next week" else "Check in 7 days in a row", color = VibeColors.TextDim, fontSize = 11.sp)
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd)
                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomStart = 12.dp, bottomEnd = 0.dp))
                .background(Brush.horizontalGradient(listOf(VibeColors.Gold, Color(0xFFEAB308))))
                .padding(horizontal = 9.dp, vertical = 3.dp),
        ) { Text("JACKPOT", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
    }
}

@Composable
private fun Tick(modifier: Modifier = Modifier) {
    Box(modifier.size(22.dp).clip(CircleShape).background(VibeColors.Green).border(2.dp, DarkCard, CircleShape), contentAlignment = Alignment.Center) {
        Icon(MeIcons.Tick, null, tint = Color(0xFF06150D), modifier = Modifier.size(13.dp))
    }
}
