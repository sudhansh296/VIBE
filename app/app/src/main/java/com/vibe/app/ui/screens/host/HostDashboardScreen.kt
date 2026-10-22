package com.vibe.app.ui.screens.host

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vibe.app.data.HostDemo
import com.vibe.app.ui.components.BackButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PillChip
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.VibeSwitch
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

private val DarkCard = Color(0xFF0B0B12)

/** Host dashboard: online switch, earnings, chart, rates, recent earnings, agency. DEMO numbers; the server computes all of it. */
@Composable
fun HostDashboardScreen(onBack: () -> Unit, onRates: () -> Unit, onWithdraw: () -> Unit, onGifts: () -> Unit) {
    var online by remember { mutableStateOf(true) }
    var week by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf("all") }
    var agencyCode by remember { mutableStateOf<String?>(null) }
    var askAgency by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Green, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        GlowBlob(VibeColors.Pink, 240.dp, Modifier.align(Alignment.CenterStart).offset((-100).dp, 60.dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton(onBack)
                Text("Host dashboard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable(onClick = onRates),
                    contentAlignment = Alignment.Center,
                ) { Icon(MeIcons.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OnlineCard(online) { online = !online }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (online) { Box(Modifier.size(8.dp).clip(CircleShape).background(VibeColors.Green)); Box(Modifier.width(8.dp)) }
                    Text(if (online) "Waiting for calls… keep VIBE open" else "Turn on Online to start receiving calls", color = VibeColors.TextDim, fontSize = 12.sp)
                }

                // available to withdraw
                val wShape = RoundedCornerShape(24.dp)
                Column(
                    Modifier.fillMaxWidth().clip(wShape).background(Brush.linearGradient(listOf(Color(0x66784C0A), VibeColors.Card, VibeColors.Card)))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), wShape).padding(20.dp),
                ) {
                    Text("AVAILABLE TO WITHDRAW", color = Color(0xFFD1D5DB), fontSize = 10.sp, letterSpacing = 1.6.sp)
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoinDot(36.dp)
                        Text("%,d".format(HostDemo.AVAILABLE), color = VibeColors.Gold, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    }
                    Text("About ₹%,.0f".format(HostDemo.AVAILABLE * HostDemo.COIN_INR), color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Box(
                        Modifier.padding(top = 16.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(VibeColors.Gold).clickable(onClick = onWithdraw),
                        contentAlignment = Alignment.Center,
                    ) { Text("Withdraw", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("674", "Today", Modifier.weight(1f))
                    Stat("34", "Minutes", Modifier.weight(1f))
                    Stat("4.8 ★", "Rating", Modifier.weight(1f))
                }

                // chart
                val cShape = RoundedCornerShape(20.dp)
                Column(Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Earnings", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        PillChip("This week", week, { week = true }, gold = true)
                        Box(Modifier.width(6.dp))
                        PillChip("Month", !week, { week = false }, gold = true)
                    }
                    EarningsChart(if (week) HostDemo.week else HostDemo.month, highlightLast = week)
                }

                // rates
                Column(Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).clickable(onClick = onRates).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("My rates", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("Edit ›", color = VibeColors.Pink, fontSize = 12.sp)
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RateTile("Voice", "30/min", Modifier.weight(1f))
                        RateTile("Video", "60/min", Modifier.weight(1f))
                        RateTile("Message", "200", Modifier.weight(1f))
                    }
                    Text("Higher rates unlock as your host level grows.", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
                }

                // recent
                Column(Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).padding(16.dp)) {
                    Text("Recent earnings", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("all" to "All", "call" to "Calls", "gift" to "Gifts", "msg" to "Messages").forEach { (k, l) ->
                            PillChip(l, filter == k, { filter = k }, gold = true)
                        }
                    }
                    val list = HostDemo.recent.filter { filter == "all" || it.kind == filter }
                    if (list.isEmpty()) Text("Nothing here yet", color = VibeColors.TextMute, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
                    list.forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.title, color = Color.White, fontSize = 14.sp)
                                Text(e.time + if (e.detail.isNotEmpty()) " · ${e.detail}" else "", color = VibeColors.TextMute, fontSize = 11.sp)
                            }
                            Text(if (e.amount == 0) "0" else "+${e.amount}", color = if (e.amount == 0) VibeColors.TextMute else VibeColors.Green, fontWeight = FontWeight.Bold)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                    }
                    Text("Calls shorter than the minimum set by VIBE do not earn.", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                }

                // gifts
                Row(
                    Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).clickable(onClick = onGifts).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Gold.copy(alpha = 0.10f)).border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(CallIcons.Gift, null, tint = VibeColors.Gold, modifier = Modifier.size(22.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text("Gifts", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Received and sent", color = VibeColors.TextMute, fontSize = 12.sp)
                    }
                    Text("›", color = Color(0xFF4B5563), fontSize = 18.sp)
                }

                // agency
                Row(
                    Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Gold.copy(alpha = 0.10f)).border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(MeIcons.Group, null, tint = VibeColors.Gold, modifier = Modifier.size(22.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text("Agency", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(agencyCode?.let { "${HostDemo.agencies[it] ?: "Agency"} · $it" } ?: "Not in an agency", color = VibeColors.TextMute, fontSize = 12.sp)
                    }
                    Box(
                        Modifier.height(36.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { if (agencyCode == null) askAgency = true else agencyCode = null }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (agencyCode == null) "Join with code" else "Leave", color = Color.White, fontSize = 12.sp) }
                }
                Text(
                    "Earnings, share and payouts are calculated by the VIBE server.", color = VibeColors.TextMute, fontSize = 11.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                )
            }
        }

        if (askAgency) AgencyDialog(onDismiss = { askAgency = false }, onJoin = { agencyCode = it; askAgency = false })
    }
}

@Composable
private fun OnlineCard(online: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val ripple by rememberInfiniteTransition(label = "ripple").animateFloat(0.8f, 1.5f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "r")
    Row(
        Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (online) Box(Modifier.size(48.dp).graphicsLayer { scaleX = ripple; scaleY = ripple; alpha = (1.5f - ripple).coerceIn(0f, 1f) * 0.6f }.clip(CircleShape).background(VibeColors.Green))
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(VibeColors.Dark).border(2.dp, if (online) VibeColors.Green else Color(0xFF4B5563), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(CallIcons.Online, null, tint = if (online) VibeColors.Green else Color(0xFF6B7280), modifier = Modifier.size(24.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Text(if (online) "You are online" else "You are offline", color = if (online) VibeColors.Green else VibeColors.TextDim, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(if (online) "Users can call and message you" else "Switch on to receive calls", color = VibeColors.TextDim, fontSize = 12.sp)
        }
        VibeSwitch(online, onToggle)
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RateTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(DarkCard).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.sp)
        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CoinDot(14.dp)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EarningsChart(data: List<Pair<String, Int>>, highlightLast: Boolean) {
    var start by remember(data) { mutableStateOf(false) }
    LaunchedEffect(data) { start = true }
    val max = data.maxOf { it.second }.toFloat()
    Column(Modifier.padding(top = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(128.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            data.forEachIndexed { i, (_, v) ->
                val h by animateFloatAsState(if (start) v / max else 0f, tween(800), label = "bar")
                val gold = highlightLast && i == data.lastIndex
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(h.coerceAtLeast(0.05f)).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(Brush.verticalGradient(if (gold) listOf(VibeColors.Gold, Color(0xFFEAB308)) else listOf(VibeColors.Pink, VibeColors.Violet))),
                    )
                }
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            data.forEach { (d, _) -> Text(d, color = VibeColors.TextMute, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun AgencyDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    val known = HostDemo.agencies[code.uppercase()]
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(24.dp)
        Column(Modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(20.dp)) {
            Text("Join an agency", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Enter the code your agency gave you. You can leave any time.", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            VibeField(code, { code = it.uppercase().take(10) }, "e.g. AG1045", caps = true)
            Text(
                if (code.isEmpty()) "" else if (known != null) "✓ $known" else "Checking the code…",
                color = if (known != null) VibeColors.Green else VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            NeonButton("Join", { onJoin(code) }, enabled = known != null)
        }
    }
}
