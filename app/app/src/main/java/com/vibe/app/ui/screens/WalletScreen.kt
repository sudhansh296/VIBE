package com.vibe.app.ui.screens

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.CoinPack
import com.vibe.app.data.Demo
import com.vibe.app.data.MeDemo
import com.vibe.app.data.WalletTx
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.Segmented
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Wallet: balance, coin packs, pay method, history. DEMO; packs, prices, bonuses and the balance come from the server. */
@Composable
fun WalletScreen(onBack: () -> Unit, onPaymentFailed: () -> Unit, onReceipt: () -> Unit) {
    var balance by remember { mutableStateOf(Demo.BALANCE) }
    var tab by remember { mutableStateOf("recharge") }
    var pick by remember { mutableStateOf(2) }
    var method by remember { mutableStateOf(MeDemo.payMethods.first()) }
    var history by remember { mutableStateOf(MeDemo.history) }
    var paying by remember { mutableStateOf(false) }
    var successCoins by remember { mutableStateOf(0) }
    var showOk by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val packs = MeDemo.packs

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Gold, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))

        Column(Modifier.fillMaxSize()) {
            PageHeader("Wallet", onBack)

            // balance
            val cardShape = RoundedCornerShape(24.dp)
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(cardShape)
                    .background(Brush.linearGradient(listOf(Color(0x66784C0A), VibeColors.Card, VibeColors.Card)))
                    .border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), cardShape).padding(20.dp),
            ) {
                SectionLabel("Coin balance")
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CoinDot(40.dp)
                    Text("%,d".format(balance), color = VibeColors.Gold, fontSize = 36.sp, fontWeight = FontWeight.Black)
                }
                // demo estimate only; the real minutes come from the server rates
                Text(
                    "About ${balance / 60} min of video or ${balance / 30} min of voice calls", color = VibeColors.TextDim, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // tabs
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("recharge" to "Recharge", "history" to "History").forEach { (k, l) ->
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
                if (tab == "recharge") {
                    val g = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(g).background(VibeColors.Green.copy(alpha = 0.10f)).border(1.dp, VibeColors.Green.copy(alpha = 0.30f), g).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(com.vibe.app.ui.components.CallIcons.Gift, null, tint = VibeColors.Green, modifier = Modifier.size(20.dp))
                        Text(
                            androidx.compose.ui.text.buildAnnotatedString {
                                append("First recharge bonus: ")
                                pushStyle(androidx.compose.ui.text.SpanStyle(color = VibeColors.Green, fontWeight = FontWeight.Bold))
                                append("+50% extra coins")
                                pop()
                            },
                            color = Color.White, fontSize = 14.sp,
                        )
                    }

                    SectionLabel("Choose a pack", Modifier.padding(top = 20.dp, bottom = 16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (row in 0..1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (col in 0..2) {
                                    val i = row * 3 + col
                                    PackCard(packs[i], i == pick, Modifier.weight(1f)) { pick = i }
                                }
                            }
                        }
                    }

                    SectionLabel("Pay with", Modifier.padding(top = 24.dp, bottom = 12.dp))
                    Segmented(MeDemo.payMethods.map { it to it }, method, { method = it })
                    Text(
                        "Secure payment by Razorpay. Coins are added only after the server confirms your payment.",
                        color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    )
                } else {
                    history.forEach { HistoryRow(it, onClick = if (it.title == "Recharge") onReceipt else null) }
                }
            }

            if (tab == "recharge") {
                Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                    NeonButton("Pay ₹${packs[pick].price}", loading = paying, onClick = {
                        if (method == "Card") { onPaymentFailed(); return@NeonButton }   // demo: the card always fails
                        paying = true
                        scope.launch {
                            delay(1100)   // demo: the real flow opens Razorpay and waits for the server to confirm
                            val coins = packs[pick].coins
                            balance += coins
                            history = listOf(WalletTx(coins, "Recharge", "Just now")) + history
                            successCoins = coins
                            paying = false
                            showOk = true
                        }
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
                Text("Payment successful", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
                Text("+${"%,d".format(successCoins)} coins added", color = VibeColors.Gold, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                Box(
                    Modifier.padding(top = 32.dp).height(48.dp).clip(CircleShape).background(Color.White).clickable { showOk = false }.padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Done", color = VibeColors.Dark, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun PackCard(pack: CoinPack, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
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
            Text("₹${pack.price}", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
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
private fun HistoryRow(tx: WalletTx, onClick: (() -> Unit)? = null) {
    val plus = tx.coins > 0
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).border(1.dp, if (plus) VibeColors.Green.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(if (plus) MeIcons.Up else MeIcons.Down, null, tint = if (plus) VibeColors.Green else VibeColors.TextDim, modifier = Modifier.size(16.dp)) }
        Column(Modifier.weight(1f)) {
            Text(tx.title, color = Color.White, fontSize = 14.sp)
            Text(tx.time, color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            (if (plus) "+" else "−") + "%,d".format(kotlin.math.abs(tx.coins)),
            color = if (plus) VibeColors.Green else Color(0xFFD1D5DB), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
}
