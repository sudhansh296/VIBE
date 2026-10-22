package com.vibe.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.Demo
import com.vibe.app.data.Host
import com.vibe.app.data.HostStatus
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.RingWave
import com.vibe.app.ui.components.Segmented
import com.vibe.app.ui.components.SelectChip
import com.vibe.app.ui.components.cardSurface
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private enum class MatchStep { Choose, Searching }

/** The fixed Random match price (admin setting `random_match_rate`, the same for every host), from the server's public config. */
@Composable
fun rememberRandomRate(): Int {
    var rate by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        runCatching {
            val c = com.vibe.app.data.api.Api.get("/config", auth = false).getJSONObject("config")
            val base = c.optInt("random_match_rate", 0)
            // a VIP pays less, rounded the same way the server charges it
            if (com.vibe.app.data.api.AuthStore.me?.optBoolean("vip") == true) maxOf(1, Math.round(base * (100 - c.optInt("vip_call_discount_pct", 0)) / 100.0).toInt()) else base
        }.onSuccess { rate = it }
    }
    return rate
}

/**
 * Random match: the server picks a real host who is online and free right now and the call rings at once (the ringing screen
 * opens by itself). The price is the fixed Random match rate, charged at the start of every minute after pick-up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomMatchScreen(onClose: () -> Unit, onConnect: (Int) -> Unit, onWallet: () -> Unit = {}) {
    val ctx = LocalContext.current
    var step by remember { mutableStateOf(MatchStep.Choose) }
    var who by remember { mutableStateOf("all") }
    val seen = remember { mutableStateListOf<Int>() }     // hosts already met in this visit: not picked again
    var searchKey by remember { mutableIntStateOf(0) }
    val rate = rememberRandomRate()

    // the server picks the match; when it rings, the ringing screen opens through the live connection
    LaunchedEffect(step, searchKey) {
        if (step != MatchStep.Searching) return@LaunchedEffect
        delay(1200)   // a short search animation, so it does not flash by
        com.vibe.app.data.Realtime.randomStart(if (who == "F") "female" else if (who == "M") "male" else null, seen.toList()) { err ->
            when (err) {
                null -> { com.vibe.app.data.Realtime.activeCall?.peerId?.let { seen.add(it) }; step = MatchStep.Choose }
                "no_hosts" -> {
                    step = MatchStep.Choose
                    if (seen.isNotEmpty()) { seen.clear(); ctx.toast("You have met everyone who is free · starting over") }
                    else ctx.toast("No one is free right now · try again in a moment")
                }
                "not_enough_coins" -> { step = MatchStep.Choose; ctx.toast("Not enough coins · $rate coins per minute"); onWallet() }
                else -> { step = MatchStep.Choose; ctx.toast(err) }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 300.dp, Modifier.offset((-90).dp, (-90).dp))
        GlowBlob(VibeColors.Cyan, 300.dp, Modifier.align(Alignment.BottomEnd).offset(110.dp, 40.dp))

        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Card)
                        .border(1.dp, VibeColors.Line, RoundedCornerShape(12.dp)).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = VibeColors.Ink, fontSize = 16.sp) }
                Text("Random Match", color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Row(
                    Modifier.height(36.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Gold.copy(alpha = 0.3f), CircleShape).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { CoinDot(16.dp); Spacer(Modifier.width(6.dp)); Text("%,d".format(Demo.BALANCE), color = VibeColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }

            AnimatedContent(step, Modifier.weight(1f), transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(200)) }, label = "step") { s ->
                when (s) {
                    MatchStep.Choose -> ChooseStep(who, { who = it }, rate) { step = MatchStep.Searching; searchKey++ }
                    MatchStep.Searching -> SearchingStep { step = MatchStep.Choose }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChooseStep(who: String, onWho: (String) -> Unit, rate: Int, onStart: () -> Unit) {
    val y by rememberFloat()
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                RingWave(VibeColors.Pink.copy(alpha = 0.4f), 176.dp)
                RingWave(VibeColors.Cyan.copy(alpha = 0.4f), 176.dp, delayMs = 1000)
                Image(painterResource(R.drawable.ghost_search), null, Modifier.size(176.dp).graphicsLayer { translationY = y.dp.toPx() })
            }
            Text("Meet someone new", color = VibeColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("We connect you with a real person who is online and accepting calls right now.", color = VibeColors.TextDim,
                fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp))
        }
        Text("Match me with", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        Segmented(listOf("all" to "✨ Anyone", "F" to "♀ Girls", "M" to "♂ Boys"), who, onWho)
        Row(Modifier.padding(top = 20.dp).fillMaxWidth().cardSurface().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CoinDot(20.dp)
            Text(
                (if (rate > 0) "$rate coins per minute for every match. " else "") + "You only pay after the other person picks up.",
                color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp),
            )
        }
        NeonButton("Start matching", onClick = onStart, modifier = Modifier.padding(vertical = 20.dp))
    }
}

@Composable
private fun SearchingStep(onCancel: () -> Unit) {
    val t = rememberInfiniteTransition(label = "radar")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "sweep")
    val orbit by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "orbit")
    val y by rememberFloat()
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); seconds++ } }
    val online = com.vibe.app.data.HostRepo.cache.values.filter { it.real && it.verified && it.status == HostStatus.ONLINE }.take(8)

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                listOf(1f, 0.72f, 0.45f).forEach { drawCircle(Color.White.copy(alpha = 0.06f), radius = size.minDimension / 2 * it, style = Stroke(1.dp.toPx())) }
            }
            Canvas(Modifier.matchParentSize().rotate(sweep)) {
                drawCircle(Brush.sweepGradient(listOf(Color.Transparent, Color.Transparent, VibeColors.Pink.copy(alpha = 0.35f))))
            }
            // avatars orbiting the radar
            online.forEachIndexed { i, h ->
                val outer = i < 4
                val count = if (outer) 4 else online.size - 4
                val idx = if (outer) i else i - 4
                val r = if (outer) 128f else 84f
                // position is read in the layout lambda, so the orbit animates without recomposing the screen
                Avatar(h, Modifier.offset {
                    val spin = if (outer) orbit.toDouble() else -orbit * 0.7
                    val a = Math.toRadians(idx * 360.0 / count + spin)
                    androidx.compose.ui.unit.IntOffset((cos(a) * r).dp.roundToPx(), (sin(a) * r).dp.roundToPx())
                }.size(44.dp).clip(CircleShape).border(2.dp, VibeColors.Dark, CircleShape), 16.sp)
            }
            Image(painterResource(R.drawable.ghost_search), null, Modifier.size(128.dp).graphicsLayer { translationY = y.dp.toPx() })
        }
        Text("Finding someone…", color = VibeColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 28.dp))
        Text((if (online.isNotEmpty()) "${online.size}+ people online · " else "") + "0:%02d".format(seconds), color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Box(
            Modifier.padding(top = 36.dp).height(48.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape)
                .clickable(onClick = onCancel).padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Cancel", color = VibeColors.ChipText, fontWeight = FontWeight.SemiBold) }
    }
}
