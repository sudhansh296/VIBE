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

private val NODE = 48.dp
private val ROW_H = 78.dp
private val PAD = 28.dp

/** My level: XP ring, ways to earn XP and the rewards as a winding path. DEMO; XP/levels/rewards come from the server. */
@Composable
fun LevelScreen(onBack: () -> Unit) {
    val cur = MeDemo.LEVEL
    var start by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { start = true }
    val progress by animateFloatAsState(if (start) MeDemo.XP / MeDemo.XP_NEXT.toFloat() else 0f, tween(1200), label = "xp")

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
                            Text("$cur", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraLight)
                        }
                    }
                    Row(Modifier.padding(top = 12.dp)) {
                        Text("${MeDemo.XP}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(" / ${"%,d".format(MeDemo.XP_NEXT)} XP to Level ${cur + 1}", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                    }
                    Text("Level ${cur + 1} unlocks: 5% off calls", color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                SectionLabel("How to earn XP", Modifier.padding(top = 28.dp, bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EarnCard("Calls", "10 XP every minute", VibeColors.Pink, Modifier.weight(1f))
                        EarnCard("Chats", "2 XP per message", VibeColors.Cyan, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EarnCard("Gifts", "1 XP per 10 coins", VibeColors.Gold, Modifier.weight(1f))
                        EarnCard("Check-in", "20 XP daily", VibeColors.Green, Modifier.weight(1f))
                    }
                }

                SectionLabel("Rewards", Modifier.padding(top = 28.dp, bottom = 12.dp))
                RewardPath(MeDemo.rewards, cur)
                Text(
                    "XP, levels and rewards are decided by VIBE. The app only shows them.", color = VibeColors.TextMute, fontSize = 11.sp,
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

private fun nodeFrac(i: Int) = if (i % 2 == 1) 0.86f else 0.14f

/** Rewards zig-zag left/right along a curved road; the part already travelled glows pink to cyan. */
@Composable
private fun RewardPath(rewards: List<LevelReward>, cur: Int) {
    val n = rewards.size
    val reached = rewards.indexOfLast { it.level <= cur }
    val total = PAD * 2 + ROW_H * (n - 1)

    BoxWithConstraints(Modifier.fillMaxWidth().height(total)) {
        val w = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            val h = ROW_H.toPx()
            val pad = PAD.toPx()
            fun px(i: Int) = size.width * nodeFrac(i)
            fun py(i: Int) = pad + h * i
            fun Path.segment(i: Int) = cubicTo(px(i - 1), py(i - 1) + h * 0.55f, px(i), py(i) - h * 0.55f, px(i), py(i))

            val full = Path().apply {
                moveTo(px(0), py(0))
                for (i in 1 until n) segment(i)
            }
            drawPath(
                full, Color(0xFF2A2A3A),
                style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 11.dp.toPx()))),
            )
            if (reached > 0) {
                val done = Path().apply {
                    moveTo(px(0), py(0))
                    for (i in 1..reached) segment(i)
                }
                val brush = Brush.verticalGradient(listOf(VibeColors.Pink, VibeColors.Cyan), startY = py(0), endY = py(reached))
                drawPath(done, VibeColors.Pink.copy(alpha = 0.25f), style = Stroke(11.dp.toPx(), cap = StrokeCap.Round))
                drawPath(done, brush, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        rewards.forEachIndexed { i, r ->
            val cx = w * nodeFrac(i)
            val cy = PAD + ROW_H * i
            val done = r.level <= cur
            val next = i == reached + 1
            LevelNode(r.level, done, next, Modifier.offset(cx - NODE / 2, cy - NODE / 2))

            val labelW = w * 0.62f
            val nodeOnLeft = i % 2 == 0
            val lx = if (nodeOnLeft) cx + w * 0.10f else cx - w * 0.10f - labelW
            Column(
                Modifier.offset(lx, cy - 20.dp).width(labelW).height(40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (nodeOnLeft) Alignment.Start else Alignment.End,
            ) {
                Text(r.title, color = if (done || next) Color.White else VibeColors.TextDim, fontSize = 13.sp, lineHeight = 15.sp)
                val xp = "%,d XP".format(r.xp)
                Text(
                    if (next) "NEXT · $xp" else if (done) "✓ $xp" else xp, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp),
                    color = if (next) VibeColors.Pink else if (done) VibeColors.Green else Color(0xFF4B5563),
                    fontWeight = if (next) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun LevelNode(level: Int, done: Boolean, next: Boolean, modifier: Modifier) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
    val bg = if (done) VibeBrush.Button else Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card))
    val border = if (done || next) VibeColors.Pink else Color(0xFF2C2C3C)
    Box(
        modifier.size(NODE).alpha(if (next) pulse else 1f).clip(CircleShape).background(bg).border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (done || next) Text("Lv$level", color = if (done) Color.White else VibeColors.Pink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        else Icon(CallIcons.Lock, null, tint = Color(0xFF4B5563), modifier = Modifier.size(20.dp))
    }
}
