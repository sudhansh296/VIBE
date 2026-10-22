package com.vibe.app.ui.screens.host

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.api.Api
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.PillChip
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.launch
import org.json.JSONObject

/** One withdrawal as the server keeps it (beans only). */
private class Request(val id: Long, val beans: Int, val status: String, val note: String?, val at: String)

private val BeanDark = Color(0xFF256B3A)

/** The bean mark (beans appear only on the withdraw page; everything else in the app is VIBE coins). */
@Composable
fun BeanDot(size: Dp) {
    Box(
        Modifier.size(size * 0.78f, size).clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(Color(0xFF9BE7A0), Color(0xFF3FA65A), BeanDark))),
        contentAlignment = Alignment.Center,
    ) { Box(Modifier.size(size * 0.12f, size * 0.55f).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.45f))) }
}

private fun day(at: String): String = runCatching {
    val d = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(at.take(19))!!
    java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.ENGLISH).format(d).replace("AM", "am").replace("PM", "pm")
}.getOrDefault(at.take(10))

/**
 * Withdraw, in BEANS only (no coins, no rupees here). The server turns the host's earned coins into beans (admin rate), takes
 * the request and holds it; the VIBE team pays the UPI ID outside the app and marks it paid, or returns the beans.
 */
@Composable
fun WithdrawScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("new") }
    var avail by remember { mutableStateOf(0) }
    var minBeans by remember { mutableStateOf(0) }
    var upi by remember { mutableStateOf<String?>(null) }
    var upiName by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<Request>() }
    var loaded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf<String?>(null) }
    var amtText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var showOk by remember { mutableStateOf(false) }
    var okBeans by remember { mutableStateOf(0) }

    suspend fun load() {
        runCatching { Api.get("/host/payout") }.onSuccess { r ->
            avail = r.optInt("beans"); minBeans = r.optInt("minBeans")
            upi = if (r.isNull("upi")) null else r.optString("upi").takeIf { it.isNotBlank() }
            upiName = if (r.isNull("upiName")) null else r.optString("upiName").takeIf { it.isNotBlank() }
            val a = r.getJSONArray("history")
            history.clear()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                history.add(Request(o.getLong("id"), o.getInt("beans"), o.getString("status"), if (o.isNull("note")) null else o.optString("note"), o.optString("at")))
            }
            loaded = true; failed = null
        }.onFailure { failed = it.message ?: "Could not load" }
    }
    LaunchedEffect(Unit) { load() }
    // the VIBE team paid (or returned) a request while this page is open
    LaunchedEffect(Unit) {
        com.vibe.app.data.Realtime.events.collect { e -> if (e is com.vibe.app.data.LiveEvent.Notified && e.notice.kind.startsWith("withdraw")) load() }
    }

    val amt = amtText.toIntOrNull() ?: 0
    val pending = history.any { it.status == "pending" }
    val error = when {
        pending -> "You already have a request in progress. Only one at a time."
        amt > 0 && amt < minBeans -> "The smallest withdrawal is ${"%,d".format(minBeans)} beans"
        amt > avail -> "More than you have"
        else -> ""
    }
    val canSend = loaded && amt > 0 && error.isEmpty() && upi != null && !editing && !sending

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Green, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Withdraw", onBack)
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("new" to "Withdraw", "hist" to "History").forEach { (k, l) ->
                    Column(Modifier.width(IntrinsicSize.Max).noRippleClick { tab = k }) {
                        Text(l.uppercase(), color = if (tab == k) VibeColors.Ink else VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                        Box(Modifier.padding(top = 10.dp).height(2.dp).fillMaxWidth().then(if (tab == k) Modifier.background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))) else Modifier))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.10f)))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                failed?.let { Text("$it · tap to retry", color = VibeColors.Red, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp).clickable { scope.launch { load() } }) }
                if (tab == "new") {
                    val wShape = RoundedCornerShape(24.dp)
                    Column(
                        Modifier.fillMaxWidth().clip(wShape).background(Brush.linearGradient(listOf(Color(0x403FA65A), VibeColors.Card, VibeColors.Card)))
                            .border(1.dp, VibeColors.Green.copy(alpha = 0.40f), wShape).padding(20.dp),
                    ) {
                        Text("AVAILABLE BEANS", color = Color(0xFF5A3F4D), fontSize = 10.sp, letterSpacing = 1.6.sp)
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BeanDot(34.dp)
                            Text(if (loaded) "%,d".format(avail) else "–", color = BeanDark, fontSize = 36.sp, fontWeight = FontWeight.Black)
                        }
                        if (minBeans > 0) Text("Smallest withdrawal: ${"%,d".format(minBeans)} beans", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    SectionLabel("Beans to withdraw", Modifier.padding(top = 22.dp, bottom = 10.dp))
                    VibeField(amtText, { amtText = it.filter { c -> c.isDigit() }.take(9) }, "0", keyboard = KeyboardType.Number, minHeight = 60.dp, trailing = null)
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOfNotNull(minBeans.takeIf { it in 1..avail }, (minBeans * 2).takeIf { it in 1..avail }).distinct().forEach { q ->
                            PillChip("%,d".format(q), amt == q, { amtText = q.toString() }, Modifier.weight(1f), gold = true)
                        }
                        PillChip("All", amt == avail && amt > 0, { amtText = avail.toString() }, Modifier.weight(1f), gold = true)
                    }
                    Text(error, color = VibeColors.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))

                    SectionLabel("Pay to", Modifier.padding(top = 14.dp, bottom = 10.dp))
                    val cShape = RoundedCornerShape(20.dp)
                    if (upi != null && !editing) {
                        Row(
                            Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Ink.copy(alpha = 0.05f)).border(1.dp, VibeColors.Ink.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(MeIcons.Bank, null, tint = Color(0xFF5A3F4D), modifier = Modifier.size(22.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(upi.orEmpty(), color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("UPI · ${upiName.orEmpty()}", color = VibeColors.TextMute, fontSize = 11.sp)
                            }
                            if (!pending) Text("Change", color = VibeColors.Pink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { editing = true })
                        }
                    } else if (loaded) {
                        PayForm { u, n ->
                            scope.launch {
                                runCatching { Api.put("/host/payout", JSONObject().put("upi", u).put("name", n)) }
                                    .onSuccess { r -> upi = r.optString("upi"); upiName = r.optString("upiName"); editing = false; ctx.toast("UPI ID saved") }
                                    .onFailure { ctx.toast(it.message ?: "Could not save") }
                            }
                        }
                    }
                    Text(
                        "Beans are paid only to a UPI ID in your own name. The VIBE team checks and pays each request; if it cannot be paid, your beans come back.",
                        color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    )
                } else {
                    if (loaded && history.isEmpty()) Text("No withdrawals yet", color = VibeColors.TextMute, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                    history.forEach { p -> RequestRow(p) }
                }
            }

            if (tab == "new") {
                Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                    NeonButton(if (sending) "Sending…" else "Request withdrawal", enabled = canSend, onClick = {
                        sending = true
                        scope.launch {
                            runCatching { Api.post("/host/withdrawals", JSONObject().put("beans", amt)) }
                                .onSuccess { okBeans = amt; amtText = ""; load(); showOk = true }
                                .onFailure { ctx.toast(it.message ?: "Could not send the request") }
                            sending = false
                        }
                    })
                }
            }
        }

        AnimatedVisibility(showOk, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
            Column(
                Modifier.fillMaxSize().background(Color(0xFFFFF5F8)).noRippleClick { }.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(96.dp).border(2.dp, VibeColors.Green, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(MeIcons.Tick, null, tint = VibeColors.Green, modifier = Modifier.size(48.dp))
                }
                Text("Request sent", color = VibeColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BeanDot(18.dp)
                    Text("  ${"%,d".format(okBeans)} beans", color = BeanDark, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                val tShape = RoundedCornerShape(16.dp)
                Column(Modifier.padding(top = 20.dp).fillMaxWidth().clip(tShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, tShape).padding(horizontal = 16.dp)) {
                    TimelineRow("✓", "Requested", "Just now", VibeColors.Green, Color.Black)
                    TimelineRow("2", "Checked by VIBE", "usually within 48 h", VibeColors.Gold, Color.Black)
                    TimelineRow("3", "Paid to your UPI", "—", VibeColors.Line, VibeColors.TextDim, dim = true, last = true)
                }
                Box(
                    Modifier.padding(top = 28.dp).height(48.dp).clip(CircleShape).background(com.vibe.app.ui.theme.VibeBrush.Button).clickable { showOk = false }.padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun TimelineRow(mark: String, title: String, time: String, markBg: Color, markFg: Color, dim: Boolean = false, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(markBg), contentAlignment = Alignment.Center) { Text(mark, color = markFg, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Text(title, color = if (dim) VibeColors.TextDim else VibeColors.Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(time, color = VibeColors.TextMute, fontSize = 12.sp)
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.05f)))
}

@Composable
private fun PayForm(onSave: (upi: String, name: String) -> Unit) {
    var upi by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val upiOk = Regex("^[\\w.\\-]{2,}@[a-zA-Z][\\w]{1,}$").matches(upi)
    val ok = upiOk && name.trim().length > 2
    val shape = RoundedCornerShape(16.dp)
    Column {
        Text(
            "Add this once. Beans are paid only to a UPI ID in your own name.",
            color = VibeColors.TextDim, fontSize = 11.sp, lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Ink.copy(alpha = 0.10f), shape).padding(12.dp),
        )
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val validMark: @Composable () -> Unit = { Text("✓ Valid", color = VibeColors.Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            VibeField(upi, { upi = it.trim() }, "UPI ID (name@bank)", trailing = if (upiOk) validMark else null)
            VibeField(name, { name = it }, "Name as in bank / UPI")
        }
        Box(Modifier.padding(top = 16.dp)) { NeonButton("Save", { onSave(upi, name.trim()) }, enabled = ok) }
        Text("Only the VIBE payouts team can see this", color = VibeColors.TextMute, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun RequestRow(p: Request) {
    val (label, bg, fg) = when (p.status) {
        "paid" -> Triple("PAID", VibeColors.Green.copy(alpha = 0.15f), VibeColors.Green)
        "pending" -> Triple("IN PROGRESS", VibeColors.Gold.copy(alpha = 0.15f), VibeColors.Gold)
        else -> Triple("NOT PAID · RETURNED", VibeColors.Red.copy(alpha = 0.15f), VibeColors.Red)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BeanDot(22.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("%,d beans".format(p.beans), color = VibeColors.Ink, fontWeight = FontWeight.Bold)
                Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)) { Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp) }
            }
            Text(listOfNotNull(day(p.at), p.note).joinToString(" · "), color = VibeColors.TextMute, fontSize = 11.sp, maxLines = 2, modifier = Modifier.padding(top = 2.dp))
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.05f)))
}
