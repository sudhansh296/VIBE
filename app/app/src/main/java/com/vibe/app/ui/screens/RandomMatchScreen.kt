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

private enum class MatchStep { Choose, Searching, Found }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomMatchScreen(onClose: () -> Unit, onConnect: (Int) -> Unit) {
    val ctx = LocalContext.current
    var step by remember { mutableStateOf(MatchStep.Choose) }
    var who by remember { mutableStateOf("all") }
    var lang by remember { mutableStateOf("any") }
    var found by remember { mutableStateOf<Host?>(null) }
    val seen = remember { mutableStateListOf<Int>() }
    var searchKey by remember { mutableIntStateOf(0) }

    // demo matching: the real server picks the match
    LaunchedEffect(step, searchKey) {
        if (step != MatchStep.Searching) return@LaunchedEffect
        delay(2600)
        val pool = Demo.hosts.filter { it.status.canCall && (who == "all" || it.gender == who[0]) && (lang == "any" || lang in it.langs) }
        val fresh = pool.filter { it.id !in seen }
        if (fresh.isEmpty()) {
            seen.clear(); step = MatchStep.Choose
            ctx.toast(if (pool.isEmpty()) "No one matches right now · try \"Anyone\"" else "You've seen everyone · starting over")
        } else {
            found = fresh.random().also { seen.add(it.id) }
            step = MatchStep.Found
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
                ) { Text("✕", color = Color.White, fontSize = 16.sp) }
                Text("Random Match", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Row(
                    Modifier.height(36.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Gold.copy(alpha = 0.3f), CircleShape).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { CoinDot(16.dp); Spacer(Modifier.width(6.dp)); Text("%,d".format(Demo.BALANCE), color = VibeColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }

            AnimatedContent(step, Modifier.weight(1f), transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(200)) }, label = "step") { s ->
                when (s) {
                    MatchStep.Choose -> ChooseStep(who, { who = it }, lang, { lang = it }) { step = MatchStep.Searching; searchKey++ }
                    MatchStep.Searching -> SearchingStep { step = MatchStep.Choose }
                    MatchStep.Found -> found?.let { h ->
                        FoundStep(h, onSkip = { step = MatchStep.Searching; searchKey++ }, onConnect = {
                            onConnect(h.id)
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChooseStep(who: String, onWho: (String) -> Unit, lang: String, onLang: (String) -> Unit, onStart: () -> Unit) {
    val y by rememberFloat()
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                RingWave(VibeColors.Pink.copy(alpha = 0.4f), 176.dp)
                RingWave(VibeColors.Cyan.copy(alpha = 0.4f), 176.dp, delayMs = 1000)
                Image(painterResource(R.drawable.ghost_search), null, Modifier.size(176.dp).graphicsLayer { translationY = y.dp.toPx() })
            }
            Text("Meet someone new", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("We connect you with a real person who is online and accepting calls right now.", color = VibeColors.TextDim,
                fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp))
        }
        Text("Match me with", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        Segmented(listOf("all" to "✨ Anyone", "F" to "♀ Girls", "M" to "♂ Boys"), who, onWho)
        Text("Language", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("any" to "Any", "Hindi" to "Hindi", "English" to "English", "Tamil" to "Tamil", "Bengali" to "Bengali")) { (k, l) ->
                SelectChip(l, lang == k, { onLang(k) })
            }
        }
        Row(Modifier.padding(top = 20.dp).fillMaxWidth().cardSurface().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("💡", fontSize = 18.sp)
            Text("You only pay after the other person picks up. Price is shown before connecting.", color = VibeColors.TextDim,
                fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
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
    val online = remember { Demo.hosts.filter { it.status != HostStatus.OFFLINE } }

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
        Text("Finding someone…", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 28.dp))
        Text("${online.size} people online · 0:%02d".format(seconds), color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Box(
            Modifier.padding(top = 36.dp).height(48.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape)
                .clickable(onClick = onCancel).padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Cancel", color = VibeColors.ChipText, fontWeight = FontWeight.SemiBold) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoundStep(h: Host, onSkip: () -> Unit, onConnect: () -> Unit) {
    var count by remember(h.id) { mutableIntStateOf(5) }
    LaunchedEffect(h.id) {
        while (count > 0) { delay(1000); count-- }
        onConnect()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("✨ It's a match!", color = VibeColors.Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier.padding(top = 16.dp).size(152.dp).clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(VibeColors.Gold, VibeColors.Pink, VibeColors.Cyan, VibeColors.Gold))).padding(3.dp)
                    .clip(CircleShape).background(VibeColors.Dark).padding(4.dp),
            ) { Avatar(h, Modifier.matchParentSize().clip(CircleShape), 60.sp) }
            Text("${h.name}, ${h.age}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 20.dp))
            Text("📍 ${h.city} · ⭐ ${h.rating} · Lv.${h.level}", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            FlowRow(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                h.langs.forEach { Text(it, color = Color.White, fontSize = 12.sp, modifier = Modifier.cardSurface(99.dp).padding(horizontal = 12.dp, vertical = 5.dp)) }
            }
            Row(
                Modifier.padding(top = 24.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Gold.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { CoinDot(16.dp); Spacer(Modifier.width(8.dp)); Text("${h.rate} coins/min · charged after pick-up", color = VibeColors.Gold, fontSize = 13.sp) }
        }
        Text("Connecting in $count…", color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
        Row(Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(56.dp).cardSurface().clickable(onClick = onSkip), contentAlignment = Alignment.Center) {
                Text("Skip", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            NeonButton("Connect now", onClick = onConnect, modifier = Modifier.weight(2f))
        }
    }
}
