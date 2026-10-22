package com.vibe.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.LevelReward
import com.vibe.app.data.MeDemo
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors


/** My level: XP ring, ways to earn XP and the coins each level gives, all from the server (the admin sets XP, levels and coins). */
@Composable
fun LevelScreen(onBack: () -> Unit) {
    var cur by remember { mutableStateOf(1) }
    var xp by remember { mutableStateOf(0) }
    var levelXp by remember { mutableStateOf(0) }
    var nextXp by remember { mutableStateOf<Int?>(null) }
    var nextReward by remember { mutableStateOf(0) }
    var rules by remember { mutableStateOf<List<LevelReward>>(emptyList()) }
    var earn by remember { mutableStateOf(listOf(0, 0, 0, 0)) }
    var start by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching { com.vibe.app.data.api.Api.get("/levels") }.onSuccess { r ->
            cur = r.optInt("level", 1); xp = r.optInt("xp"); levelXp = r.optInt("levelXp")
            nextXp = if (r.isNull("nextXp")) null else r.optInt("nextXp"); nextReward = r.optInt("nextReward")
            val a = r.getJSONArray("rules")
            rules = List(a.length()) { i -> a.getJSONObject(i).let { LevelReward(it.getInt("level"), it.getInt("xp"), if (it.optInt("rewardCoins") > 0) "+${"%,d".format(it.optInt("rewardCoins"))} coins" else "Level badge") } }
            val e = r.getJSONObject("earn")
            earn = listOf(e.optInt("callMinute"), e.optInt("message"), e.optInt("giftPer10Coins"), e.optInt("checkin"))
        }
        start = true
    }
    // progress inside the current level
    val span = (nextXp ?: xp).minus(levelXp).coerceAtLeast(1)
    val progress by animateFloatAsState(if (start) (if (nextXp == null) 1f else (xp - levelXp).toFloat() / span) else 0f, tween(1200), label = "xp")

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Cyan, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        GlowBlob(VibeColors.Pink, 240.dp, Modifier.align(Alignment.CenterEnd).offset(100.dp, (-60).dp))

        Column(Modifier.fillMaxSize()) {
            PageHeader("My level", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(160.dp)) {
                            val sw = 10.dp.toPx()
                            val arcSize = Size(size.width - sw, size.height - sw)
                            val topLeft = Offset(sw / 2, sw / 2)
                            drawArc(VibeColors.Line, 0f, 360f, false, topLeft, arcSize, style = Stroke(sw))
                            drawArc(
                                Brush.linearGradient(listOf(VibeColors.Pink, VibeColors.Cyan)), -90f, 360f * progress, false,
                                topLeft, arcSize, style = Stroke(sw, cap = StrokeCap.Round),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LEVEL", color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.8.sp)
                            Text("$cur", color = VibeColors.Ink, fontSize = 48.sp, fontWeight = FontWeight.ExtraLight)
                        }
                    }
                    Row(Modifier.padding(top = 12.dp)) {
                        Text("%,d".format(xp), color = VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(nextXp?.let { " / ${"%,d".format(it)} XP to Level ${cur + 1}" } ?: " XP · top level", color = Color(0xFF5A3F4D), fontSize = 14.sp)
                    }
                    if (nextXp != null && nextReward > 0) Text("Level ${cur + 1} gives +${"%,d".format(nextReward)} coins", color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                SectionLabel("How to earn XP", Modifier.padding(top = 28.dp, bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EarnCard("Calls", "${earn[0]} XP every minute", VibeColors.Pink, Modifier.weight(1f))
                        EarnCard("Chats", "${earn[1]} XP per message", VibeColors.Cyan, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EarnCard("Gifts", "${earn[2]} XP per 10 coins", VibeColors.Gold, Modifier.weight(1f))
                        EarnCard("Check-in", "${earn[3]} XP daily", VibeColors.Green, Modifier.weight(1f))
                    }
                }

                SectionLabel("Rewards", Modifier.padding(top = 28.dp, bottom = 12.dp))
                RewardPath(rules, cur)
                Text(
                    "XP, levels and coin rewards are set by VIBE. Each level's coins are added once, the moment you reach it.", color = VibeColors.TextMute, fontSize = 11.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 32.dp),
                )
            }
        }
    }
}

@Composable
private fun EarnCard(title: String, sub: String, color: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(12.dp)) {
        Text(title, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(sub, color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Rewards as a clean list: unlocked ones are ticked, the next one is highlighted, the rest are locked. */
@Composable
private fun RewardPath(rewards: List<LevelReward>, cur: Int) {
    val reached = rewards.indexOfLast { it.level <= cur }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rewards.forEachIndexed { i, r ->
            val done = r.level <= cur
            val next = i == reached + 1
            val shape = RoundedCornerShape(20.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape)
                    .background(if (done) Color(0xFFFFF0F6) else VibeColors.Card)
                    .border(if (next) 1.5.dp else 1.dp, if (next) VibeColors.Pink else if (done) VibeColors.Pink.copy(alpha = 0.25f) else VibeColors.Line, shape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LevelNode(r.level, done, next, Modifier)
                Column(Modifier.weight(1f)) {
                    Text(r.title, color = if (done || next) VibeColors.Ink else VibeColors.TextDim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Level ${r.level} · ${"%,d".format(r.xp)} XP", color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                val (label, fg, bg) = when {
                    done -> Triple("Reached", VibeColors.Green, Color(0xFFE6F8EE))
                    next -> Triple("Next", Color.White, VibeColors.Pink)
                    else -> Triple("Locked", VibeColors.TextMute, Color(0xFFFFF0F6))
                }
                Text(
                    label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LevelNode(level: Int, done: Boolean, next: Boolean, modifier: Modifier) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
    val bg = if (done) VibeBrush.Button else Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card))
    val border = if (done || next) VibeColors.Pink else Color(0xFFFFD6E3)
    Box(
        modifier.size(48.dp).alpha(if (next) pulse else 1f).clip(CircleShape).background(bg).border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (done || next) Text("Lv$level", color = if (done) Color.White else VibeColors.Pink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        else Icon(CallIcons.Lock, null, tint = Color(0xFFB093A2), modifier = Modifier.size(20.dp))
    }
}
