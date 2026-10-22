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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.HostDemo
import com.vibe.app.data.Payout
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.PillChip
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.theme.VibeColors

private class PayDetails(val upi: String, val name: String, val pan: String)

private fun inr(v: Double) = "₹" + "%,.2f".format(v)

/**
 * Withdraw: amount, fee + tax preview, UPI + PAN (saved once), request timeline and history.
 * DEMO. Coin value, minimum, fee %, TDS % are admin settings; the server computes the payout and validates every request.
 */
@Composable
fun WithdrawScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf("new") }
    var avail by remember { mutableStateOf(HostDemo.AVAILABLE) }
    var amtText by remember { mutableStateOf("") }
    var pay by remember { mutableStateOf<PayDetails?>(PayDetails("priya@okaxis", "Priya Sharma", "ABCDE1234F")) }
    var editing by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<Payout>().apply { addAll(HostDemo.payouts) } }
    var showOk by remember { mutableStateOf(false) }
    var okCoins by remember { mutableStateOf(0) }

    val amt = amtText.toIntOrNull() ?: 0
    val pending = history.any { it.status == "Pending" }
    val gross = amt * HostDemo.COIN_INR
    val fee = gross * HostDemo.FEE_PCT / 100
    val tds = gross * HostDemo.TDS_PCT / 100
    val net = gross - fee - tds
    val error = when {
        pending -> "You already have a request in progress. Only one at a time."
        amt > 0 && amt < HostDemo.MIN_WITHDRAW -> "Minimum withdrawal is ${"%,d".format(HostDemo.MIN_WITHDRAW)} coins"
        amt > avail -> "More than your available balance"
        else -> ""
    }
    val canSend = amt > 0 && error.isEmpty() && pay != null && !editing

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Gold, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Withdraw", onBack)
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("new" to "Withdraw", "hist" to "History").forEach { (k, l) ->
                    Column(Modifier.width(IntrinsicSize.Max).noRippleClick { tab = k }) {
                        Text(l.uppercase(), color = if (tab == k) Color.White else VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            Modifier.padding(top = 10.dp).height(2.dp).fillMaxWidth()
                                .then(if (tab == k) Modifier.background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))) else Modifier),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                if (tab == "new") {
                    val wShape = RoundedCornerShape(24.dp)
                    Column(
                        Modifier.fillMaxWidth().clip(wShape).background(Brush.linearGradient(listOf(Color(0x66784C0A), VibeColors.Card, VibeColors.Card)))
                            .border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), wShape).padding(20.dp),
                    ) {
                        Text("AVAILABLE", color = Color(0xFFD1D5DB), fontSize = 10.sp, letterSpacing = 1.6.sp)
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoinDot(36.dp)
                            Text("%,d".format(avail), color = VibeColors.Gold, fontSize = 36.sp, fontWeight = FontWeight.Black)
                        }
                        Text("About ${inr(avail * HostDemo.COIN_INR)}", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    SectionLabel("Amount (coins)", Modifier.padding(top = 22.dp, bottom = 10.dp))
                    VibeField(
                        amtText, { amtText = it.filter { c -> c.isDigit() }.take(7) }, "0", keyboard = KeyboardType.Number, minHeight = 60.dp,
                        trailing = null,
                    )
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5000, 10000).forEach { q ->
                            PillChip("%,d".format(q), amt == minOf(q, avail), { amtText = minOf(q, avail).toString() }, Modifier.weight(1f), gold = true)
                        }
                        PillChip("All", amt == avail && amt > 0, { amtText = avail.toString() }, Modifier.weight(1f), gold = true)
                    }

                    val cShape = RoundedCornerShape(20.dp)
                    Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (amt == 0) Text("Enter an amount to see what you will receive", color = VibeColors.TextMute, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        else {
                            CalcRow("Value of coins", inr(gross))
                            CalcRow("Service fee (${"%.0f".format(HostDemo.FEE_PCT)}%)", "− ${inr(fee)}")
                            CalcRow("Tax deducted (${"%.0f".format(HostDemo.TDS_PCT)}%)", "− ${inr(tds)}")
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))
                            Row(Modifier.fillMaxWidth()) {
                                Text("You receive", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(inr(net), color = VibeColors.Green, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text(error, color = VibeColors.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))

                    SectionLabel("Pay to", Modifier.padding(top = 14.dp, bottom = 10.dp))
                    val current = pay
                    if (current != null && !editing) {
                        Row(
                            Modifier.fillMaxWidth().clip(cShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, cShape).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(MeIcons.Bank, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(22.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(current.upi, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("UPI · ${current.name} · PAN ✓", color = VibeColors.TextMute, fontSize = 11.sp)
                            }
                            Text("Change", color = VibeColors.Pink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { editing = true })
                        }
                    } else {
                        PayForm(onSave = { pay = it; editing = false })
                    }

                    val gShape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.padding(top = 20.dp).fillMaxWidth().clip(gShape).background(VibeColors.Green.copy(alpha = 0.05f)).border(1.dp, VibeColors.Green.copy(alpha = 0.30f), gShape).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(com.vibe.app.ui.components.CallIcons.Shield, null, tint = VibeColors.Green, modifier = Modifier.size(20.dp))
                        Text(
                            "Identity verified. Money is sent only to an account in your own name. If a payout fails, your coins come back automatically.",
                            color = Color(0xFFD1D5DB), fontSize = 12.sp, lineHeight = 17.sp,
                        )
                    }
                    Text(
                        "Coin value, minimum amount, fees and tax deduction are set by VIBE. The final payout is shown before you confirm.",
                        color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    )
                } else {
                    history.forEach { p -> PayoutRow(p) }
                }
            }

            if (tab == "new") {
                Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                    NeonButton("Request withdrawal", enabled = canSend, onClick = {
                        history.add(0, Payout(amt, "Pending", "Today · ${pay?.upi.orEmpty()}"))
                        avail -= amt
                        okCoins = amt
                        amtText = ""
                        showOk = true
                    })
                }
            }
        }

        AnimatedVisibility(showOk, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
            Column(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).noRippleClick { }.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(96.dp).border(2.dp, VibeColors.Green, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(MeIcons.Tick, null, tint = VibeColors.Green, modifier = Modifier.size(48.dp))
                }
                Text("Request sent", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
                Text("${"%,d".format(okCoins)} coins", color = VibeColors.Gold, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                val tShape = RoundedCornerShape(16.dp)
                Column(Modifier.padding(top = 20.dp).fillMaxWidth().clip(tShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, tShape).padding(horizontal = 16.dp)) {
                    TimelineRow("✓", "Requested", "Just now", VibeColors.Green, Color.Black)
                    TimelineRow("2", "Processing", "within 48 h", VibeColors.Gold, Color.Black)
                    TimelineRow("3", "Paid to your account", "—", VibeColors.Line, VibeColors.TextDim, dim = true, last = true)
                }
                Box(
                    Modifier.padding(top = 28.dp).height(48.dp).clip(CircleShape).background(Color.White).clickable { showOk = false }.padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Done", color = VibeColors.Dark, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun CalcRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = VibeColors.TextDim, fontSize = 14.sp)
    }
}

@Composable
private fun TimelineRow(mark: String, title: String, time: String, markBg: Color, markFg: Color, dim: Boolean = false, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(markBg), contentAlignment = Alignment.Center) { Text(mark, color = markFg, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Text(title, color = if (dim) VibeColors.TextDim else Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(time, color = VibeColors.TextMute, fontSize = 12.sp)
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
}

@Composable
private fun PayForm(onSave: (PayDetails) -> Unit) {
    var upi by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var panName by remember { mutableStateOf("") }
    val upiOk = Regex("^[\\w.\\-]{2,}@[\\w]{2,}$").matches(upi)
    val panOk = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$").matches(pan)
    val ok = upiOk && name.trim().length > 2 && panOk && panName.trim().length > 2
    val shape = RoundedCornerShape(16.dp)
    Column {
        Text(
            "Add these once. Money is sent only to a UPI ID in your own name. PAN is needed by law for payouts (tax).",
            color = VibeColors.TextDim, fontSize = 11.sp, lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, Color.White.copy(alpha = 0.10f), shape).padding(12.dp),
        )
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val validMark: @Composable () -> Unit = { Text("✓ Valid", color = VibeColors.Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            VibeField(upi, { upi = it.trim() }, "UPI ID (name@bank)", trailing = if (upiOk) validMark else null)
            VibeField(name, { name = it }, "Name as in bank / UPI")
            VibeField(pan, { pan = it.uppercase().take(10) }, "PAN number", caps = true)
            VibeField(panName, { panName = it }, "Name on PAN")
        }
        Box(Modifier.padding(top = 16.dp)) { NeonButton("Save and continue", { onSave(PayDetails(upi, name, pan)) }, enabled = ok) }
        Text("Only the VIBE payouts team can see these details", color = VibeColors.TextMute, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun PayoutRow(p: Payout) {
    val (bg, fg) = when (p.status) {
        "Paid" -> VibeColors.Green.copy(alpha = 0.15f) to VibeColors.Green
        "Pending" -> VibeColors.Gold.copy(alpha = 0.15f) to VibeColors.Gold
        else -> VibeColors.Red.copy(alpha = 0.15f) to VibeColors.Red
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(MeIcons.Down, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoinDot(14.dp)
                Text("%,d".format(p.coins), color = Color.White, fontWeight = FontWeight.Bold)
                Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)) {
                    Text(p.status.uppercase(), color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                }
            }
            Text(p.note, color = VibeColors.TextMute, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
        Text(inr(p.coins * HostDemo.COIN_INR * (100 - HostDemo.FEE_PCT - HostDemo.TDS_PCT) / 100), color = Color(0xFFD1D5DB), fontSize = 14.sp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
}
