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

private val DarkCard = Color(0xFFFFFFFF)

/** Host dashboard: online switch, earnings, chart, rates, recent earnings, agency. DEMO numbers; the server computes all of it. */
@Composable
fun HostDashboardScreen(onBack: () -> Unit, onRates: () -> Unit, onWithdraw: () -> Unit, onGifts: () -> Unit) {
    var week by remember { mutableStateOf(true) }
    // earnings and their history from the server (unlocks now; calls, gifts and messages join in later phases)
    var earnings by remember { mutableStateOf<Long?>(null) }
    var items by remember { mutableStateOf<List<com.vibe.app.data.LedgerItem>>(emptyList()) }
    var rates by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { com.vibe.app.data.WalletRepo.history(1, earnings = true) }.onSuccess { items = it.first }
        runCatching { com.vibe.app.data.WalletRepo.earnings() }.onSuccess { earnings = it }
        runCatching { com.vibe.app.data.api.Api.get("/host/rates") }.onSuccess { r -> rates = r.optInt("callRate") to r.optInt("messagePrice") }
    }
    val kindOf: (String) -> String = { k -> when { k.startsWith("media") -> "unlock"; k.startsWith("call") -> "call"; k.startsWith("gift") -> "gift"; else -> "msg" } }
    var filter by remember { mutableStateOf("all") }
    var agencyCode by remember { mutableStateOf<String?>(null) }
    var askAgency by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Green, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        GlowBlob(VibeColors.Pink, 240.dp, Modifier.align(Alignment.CenterStart).offset((-100).dp, 60.dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton(onBack)
                Text("Host dashboard", color = VibeColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable(onClick = onRates),
                    contentAlignment = Alignment.Center,
                ) { Icon(MeIcons.Settings, null, tint = VibeColors.Ink, modifier = Modifier.size(20.dp)) }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // no switch: a host is online exactly while VIBE is open (the server sees the live connection)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, VibeColors.Ink.copy(alpha = 0.15f), RoundedCornerShape(16.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(VibeColors.Green)); Box(Modifier.width(8.dp))
                    Text("You are online while VIBE is open · waiting for calls", color = VibeColors.TextDim, fontSize = 12.sp)
                }

                // available to withdraw
                val wShape = RoundedCornerShape(24.dp)
                Column(
                    Modifier.fillMaxWidth().clip(wShape).background(Brush.linearGradient(listOf(Color(0x40F2B84B), VibeColors.Card, VibeColors.Card)))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), wShape).padding(20.dp),
                ) {
                    Text("AVAILABLE TO WITHDRAW", color = Color(0xFF5A3F4D), fontSize = 10.sp, letterSpacing = 1.6.sp)
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoinDot(36.dp)
                        Text(earnings?.let { "%,d".format(it) } ?: "…", color = VibeColors.Gold, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    }
                    Text("Your share of what users spend on you", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Box(
                        Modifier.padding(top = 16.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(VibeColors.Gold).clickable(onClick = onWithdraw),
                        contentAlignment = Alignment.Center,
                    ) { Text("Withdraw", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }

                // today / this week from the history (call minutes and rating come with calls, Phase 5)
                val day = 86_400_000L
                val now = System.currentTimeMillis()
                val ts = items.map { it to (runCatching { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(it.at.take(19))?.time }.getOrNull() ?: 0L) }
                val offset = java.util.TimeZone.getDefault().getOffset(now)
                val todayNo = (now + offset) / day
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("%,d".format(ts.filter { (it.second + offset) / day == todayNo }.sumOf { it.first.coins }), "Today", Modifier.weight(1f))
                    Stat("%,d".format(ts.filter { now - it.second < 7 * day }.sumOf { it.first.coins }), "This week", Modifier.weight(1f))
                    Stat("%,d".format(items.size), "Earnings", Modifier.weight(1f))
                }

                // chart
                val cShape = RoundedCornerShape(20.dp)
                Column(Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Earnings", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        PillChip("This week", week, { week = true }, gold = true)
                        Box(Modifier.width(6.dp))
                        PillChip("Month", !week, { week = false }, gold = true)
                    }
                    val bars = if (week) (6 downTo 0).map { back ->
                        val d = todayNo - back
                        java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).format(java.util.Date(d * day - offset + day / 2)) to
                            ts.filter { (it.second + offset) / day == d }.sumOf { it.first.coins }.toInt()
                    } else (3 downTo 0).map { back -> "W${4 - back}" to ts.filter { val age = (now - it.second) / day; age >= back * 7 && age < (back + 1) * 7 }.sumOf { it.first.coins }.toInt() }
                    EarningsChart(bars, highlightLast = week)
                }

                // rates
                Column(Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).clickable(onClick = onRates).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("My rates", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("Edit ›", color = VibeColors.Pink, fontSize = 12.sp)
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RateTile("Video", rates?.let { "${it.first}/min" } ?: "…", Modifier.weight(1f))
                        RateTile("Message", rates?.let { "${it.second}" } ?: "…", Modifier.weight(1f))
                    }
                    Text("Set any rate within the range VIBE allows.", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
                }

                // recent
                Column(Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).padding(16.dp)) {
                    Text("Recent earnings", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("all" to "All", "unlock" to "Unlocks", "call" to "Calls", "gift" to "Gifts").forEach { (k, l) ->
                            PillChip(l, filter == k, { filter = k }, gold = true)
                        }
                    }
                    val list = items.filter { filter == "all" || kindOf(it.kind) == filter }
                    if (list.isEmpty()) Text("Nothing here yet", color = VibeColors.TextMute, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
                    list.forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.title, color = VibeColors.Ink, fontSize = 14.sp)
                                Text(com.vibe.app.data.WalletRepo.niceTime(e.at), color = VibeColors.TextMute, fontSize = 11.sp)
                            }
                            Text(when { e.coins == 0L -> "0"; e.coins > 0 -> "+${"%,d".format(e.coins)}"; else -> "−${"%,d".format(-e.coins)}" },   // a returned minute is negative
                                color = when { e.coins == 0L -> VibeColors.TextMute; e.coins > 0 -> VibeColors.Green; else -> VibeColors.Pink }, fontWeight = FontWeight.Bold)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.05f)))
                    }
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
                        Text("Gifts", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("What users sent you", color = VibeColors.TextMute, fontSize = 12.sp)
                    }
                    Text("›", color = Color(0xFFB093A2), fontSize = 18.sp)
                }

                // agency: comes with the agency panel (Phase 6); hidden until agencies are real
                if (false) Row(
                    Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Gold.copy(alpha = 0.10f)).border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(MeIcons.Group, null, tint = VibeColors.Gold, modifier = Modifier.size(22.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text("Agency", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(agencyCode?.let { "${HostDemo.agencies[it] ?: "Agency"} · $it" } ?: "Not in an agency", color = VibeColors.TextMute, fontSize = 12.sp)
                    }
                    Box(
                        Modifier.height(36.dp).clip(CircleShape).border(1.dp, VibeColors.Ink.copy(alpha = 0.15f), CircleShape)
                            .clickable { if (agencyCode == null) askAgency = true else agencyCode = null }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (agencyCode == null) "Join with code" else "Leave", color = VibeColors.Ink, fontSize = 12.sp) }
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
private fun Stat(value: String, label: String, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RateTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(DarkCard).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.sp)
        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CoinDot(14.dp)
            Text(value, color = VibeColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EarningsChart(data: List<Pair<String, Int>>, highlightLast: Boolean) {
    var start by remember(data) { mutableStateOf(false) }
    LaunchedEffect(data) { start = true }
    val max = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1).toFloat()   // all zero (a new host): empty bars, never 0 / 0
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
            Text("Join an agency", color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
