package com.vibe.app.ui.screens

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.CoinPackItem
import com.vibe.app.data.LedgerItem
import com.vibe.app.data.WalletRepo
import com.vibe.app.data.api.ApiException
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.launch

/**
 * Wallet: balance, coin packs and history, all from the server. Buying: the server makes the order (price and coins fixed there),
 * Razorpay takes the payment, and the server adds the coins only after checking Razorpay's proof. [onResult] opens the result screen.
 */
@Composable
fun WalletScreen(onBack: () -> Unit, onResult: (kind: String, orderId: String) -> Unit, onReceipt: (orderId: String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var balance by remember { mutableStateOf(com.vibe.app.data.Demo.BALANCE) }
    var tab by remember { mutableStateOf("recharge") }
    var packs by remember { mutableStateOf<List<CoinPackItem>>(emptyList()) }
    var enabled by remember { mutableStateOf(true) }
    var pick by remember { mutableStateOf(-1) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<LedgerItem>() }
    var nextPage by remember { mutableStateOf<Int?>(1) }
    var historyLoading by remember { mutableStateOf(false) }

    suspend fun loadHistory(reset: Boolean) {
        if (historyLoading) return
        historyLoading = true
        try {
            val page = if (reset) 1 else (nextPage ?: return)
            val (items, next) = WalletRepo.history(page)
            if (reset) history.clear()
            history.addAll(items); nextPage = next
        } catch (e: ApiException) { ctx.toast(e.message) }
        historyLoading = false
    }

    LaunchedEffect(Unit) {
        try {
            balance = WalletRepo.balance()
            val (p, on) = WalletRepo.packs()
            packs = p; enabled = on
            pick = p.indexOfFirst { it.tag != null }.takeIf { it >= 0 } ?: 0
        } catch (e: ApiException) { failed = e.message }
        loading = false
    }
    LaunchedEffect(tab) { if (tab == "history") loadHistory(reset = true) }

    val checkout = com.vibe.app.ui.components.rememberCoinCheckout()

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Gold, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))

        Column(Modifier.fillMaxSize()) {
            PageHeader("Wallet", onBack)

            val cardShape = RoundedCornerShape(24.dp)
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(cardShape)
                    .background(Brush.linearGradient(listOf(Color(0x40F2B84B), VibeColors.Card, VibeColors.Card)))
                    .border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), cardShape).padding(20.dp),
            ) {
                SectionLabel("Coin balance")
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CoinDot(40.dp)
                    Text("%,d".format(balance), color = VibeColors.Gold, fontSize = 36.sp, fontWeight = FontWeight.Black)
                }
                Text("Coins are used for video calls, gifts and unlocking photos and videos", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("recharge" to "Recharge", "history" to "History").forEach { (k, l) ->
                    Column(Modifier.width(IntrinsicSize.Max).noRippleClick { tab = k }) {
                        Text(l.uppercase(), color = if (tab == k) VibeColors.Ink else VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            Modifier.padding(top = 10.dp).height(2.dp).fillMaxWidth()
                                .then(if (tab == k) Modifier.background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))) else Modifier),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.10f)))

            val scroll = rememberScrollState()
            Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 16.dp)) {
                if (tab == "recharge") {
                    when {
                        loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 3.dp, modifier = Modifier.size(30.dp)) }
                        failed != null -> Text(failed.orEmpty(), color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
                        else -> {
                            if (!enabled) Text(
                                "Buying coins is paused for a while. Please try again later.", color = VibeColors.Red, fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(VibeColors.Red.copy(alpha = 0.06f)).padding(12.dp),
                            )
                            SectionLabel("Choose a pack", Modifier.padding(top = 4.dp, bottom = 16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                packs.indices.chunked(3).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        row.forEach { i -> PackCard(packs[i], i == pick, Modifier.weight(1f)) { pick = i } }
                                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                                    }
                                }
                            }
                            Text(
                                "Secure payment by Razorpay: UPI, cards or netbanking. Coins are added only after VIBE confirms your payment.",
                                color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                            )
                        }
                    }
                } else {
                    if (history.isEmpty() && !historyLoading) Text("No coin activity yet", color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp))
                    history.forEach { tx -> HistoryRow(tx, onClick = tx.orderId?.let { id -> { onReceipt(id) } }) }
                    if (historyLoading) Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 2.dp, modifier = Modifier.size(22.dp)) }
                    else if (nextPage != null && history.isNotEmpty()) Text(
                        "Show more", color = VibeColors.Pink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable { scope.launch { loadHistory(reset = false) } }.padding(16.dp),
                    )
                }
            }

            if (tab == "recharge" && packs.isNotEmpty() && pick in packs.indices) {
                Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                    NeonButton("Pay ${WalletRepo.price(packs[pick].priceInr)}", loading = checkout.paying, enabled = enabled, onClick = {
                        checkout.buy(packs[pick]) { kind, order -> balance = com.vibe.app.data.Demo.BALANCE; onResult(kind, order) }
                    })
                }
            }
        }
    }

    checkout.TestSheet()
}

@Composable
private fun PackCard(pack: CoinPackItem, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().clip(shape)
                .background(if (on) Brush.linearGradient(listOf(VibeColors.Gold.copy(alpha = 0.14f), VibeColors.Card)) else Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card)))
                .border(1.5.dp, if (on) VibeColors.Gold else VibeColors.Line, shape)
                .clickable(onClick = onClick).padding(top = 22.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CoinDot(32.dp)
            Text("%,d".format(pack.coins), color = VibeColors.Gold, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
            if (pack.bonus > 0) Text("+${"%,d".format(pack.bonus)} bonus", color = VibeColors.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(WalletRepo.price(pack.priceInr), color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        pack.tag?.let {
            Box(
                Modifier.align(Alignment.TopCenter).offset(0.dp, (-9).dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Violet))).padding(horizontal = 8.dp, vertical = 2.dp),
            ) { Text(it, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
        }
    }
}

@Composable
private fun HistoryRow(tx: LedgerItem, onClick: (() -> Unit)? = null) {
    val plus = tx.coins > 0
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).border(1.dp, if (plus) VibeColors.Green.copy(alpha = 0.40f) else VibeColors.Ink.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(if (plus) MeIcons.Up else MeIcons.Down, null, tint = if (plus) VibeColors.Green else VibeColors.TextDim, modifier = Modifier.size(16.dp)) }
        Column(Modifier.weight(1f)) {
            Text(tx.title, color = VibeColors.Ink, fontSize = 14.sp)
            Text(WalletRepo.niceTime(tx.at) + if (onClick != null) " · Receipt ›" else "", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            (if (plus) "+" else "−") + "%,d".format(kotlin.math.abs(tx.coins)),
            color = if (plus) VibeColors.Green else Color(0xFF5A3F4D), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.05f)))
}
