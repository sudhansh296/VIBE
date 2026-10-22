package com.vibe.app.ui.screens.extras

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.KeyValueCard
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.StateScreen
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

/** Loads one purchase from the server; while it is still waiting, checks again every few seconds (the webhook may confirm it). */
@Composable
private fun rememberPayment(orderId: String?, keepChecking: Boolean): com.vibe.app.data.PaymentInfo? {
    var p by remember(orderId) { mutableStateOf<com.vibe.app.data.PaymentInfo?>(null) }
    androidx.compose.runtime.LaunchedEffect(orderId) {
        if (orderId == null) {
            // no order given: the newest paid purchase
            p = runCatching {
                val a = com.vibe.app.data.api.Api.get("/wallet/payments").getJSONArray("payments")
                (0 until a.length()).map { a.getJSONObject(it) }.firstOrNull { it.optString("status") == "paid" }?.getString("orderId")?.let { com.vibe.app.data.WalletRepo.payment(it) }
            }.getOrNull()
            return@LaunchedEffect
        }
        var tries = 0
        while (true) {
            runCatching { com.vibe.app.data.WalletRepo.payment(orderId) }.onSuccess { p = it }
            if (!keepChecking || p?.status != "created" || tries++ > 40) break
            kotlinx.coroutines.delay(3000)
        }
        if (p?.status == "paid") runCatching { com.vibe.app.data.WalletRepo.balance() }
    }
    return p
}

/**
 * Payment result: success / failed / pending, with the real order from the server. The app never adds coins; a pending payment
 * turns into "successful" by itself when the server confirms it (Razorpay webhook).
 */
@Composable
fun PaymentResultScreen(kind: String, orderId: String?, onWallet: () -> Unit, onReceipt: () -> Unit, onSupport: () -> Unit) {
    val p = rememberPayment(orderId, keepChecking = kind == "pending")
    val shown = when (p?.status) { "paid" -> "success"; "failed" -> if (kind == "pending") "failed" else kind; else -> kind }
    val amount = p?.let { com.vibe.app.data.WalletRepo.price(it.amountInr) } ?: "…"
    val ref = p?.orderId ?: orderId.orEmpty()
    when (shown) {
        "success" -> StateScreen(
            icon = MeIcons.Tick, tint = VibeColors.Green, circle = true, title = "Payment successful",
            subtitle = if (p != null) "${"%,d".format(p.coins)} coins have been added to your wallet." else "Coins have been added to your wallet.",
            primary = "Done", onPrimary = onWallet, secondary = "View receipt", onSecondary = onReceipt,
            content = { KeyValueCard(listOf("Coins" to (p?.let { "%,d".format(it.coins) } ?: "…"), "Amount paid" to amount, "Order id" to ref)) },
        )
        "pending" -> StateScreen(
            icon = MeIcons.Clock, tint = VibeColors.Gold, circle = true, title = "Payment is pending",
            subtitle = "We are waiting for your bank to confirm. Coins are added automatically as soon as it is confirmed. Please do not pay again.",
            primary = "Back to Wallet", onPrimary = onWallet, secondary = "Contact support", onSecondary = onSupport,
            content = { KeyValueCard(listOf("Amount" to amount, "Status" to "Waiting for confirmation", "Order id" to ref)) },
        )
        else -> StateScreen(
            icon = MeIcons.Close, tint = VibeColors.Red, circle = true, title = "Payment failed",
            subtitle = "Your payment did not go through. If money was deducted it is refunded to your account by your bank, usually within a few days. No coins were added.",
            primary = "Try again", onPrimary = onWallet, secondary = "Contact support", onSecondary = onSupport,
            content = { KeyValueCard(listOf("Amount" to amount, "Reason" to (p?.failReason ?: "Payment failed"), "Order id" to ref)) },
        )
    }
}

/** Receipt for one coin purchase, from the server. [orderId] null = the newest paid purchase. */
@Composable
fun ReceiptScreen(orderId: String?, onBack: () -> Unit, onSupport: () -> Unit) {
    val ctx = LocalContext.current
    val p = rememberPayment(orderId, keepChecking = false)
    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Green, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Receipt", onBack)
            if (p == null) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (orderId == null) "No purchases yet" else "Loading…", color = VibeColors.TextMute, fontSize = 14.sp)
            } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                val paid = p.status == "paid"
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier.padding(top = 8.dp).fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(Color(0xFFFFE3EF), Color(0xFFFFFFFF)))).border(1.dp, VibeColors.Line, shape).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(56.dp).clip(CircleShape).border(2.dp, if (paid) VibeColors.Green else VibeColors.Gold, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if (paid) MeIcons.Tick else MeIcons.Clock, null, tint = if (paid) VibeColors.Green else VibeColors.Gold, modifier = Modifier.size(28.dp))
                    }
                    Text(if (paid) "Payment received" else "Payment ${p.status}", color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                    Text(com.vibe.app.data.WalletRepo.price(p.amountInr), color = VibeColors.Ink, fontSize = 36.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CoinDot(20.dp)
                        Text("+${"%,d".format(p.coins)} coins", color = VibeColors.Gold, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.padding(vertical = 16.dp).fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.12f)))
                    val rows = listOf(
                        "Date" to com.vibe.app.data.WalletRepo.niceTime(p.paidAt ?: p.createdAt), "Coins" to "%,d".format(p.coins),
                        "Order id" to p.orderId, "Payment id" to (p.paymentId ?: "-"), "Status" to p.status.replaceFirstChar { it.uppercase() },
                    )
                    KeyValueCard(rows, valueColor = { if (it == 4) (if (paid) VibeColors.Green else VibeColors.Gold) else VibeColors.Ink })
                }
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostBtn("Share", Modifier.weight(1f)) {
                        val text = "VIBE coin purchase\n${com.vibe.app.data.WalletRepo.price(p.amountInr)} · ${"%,d".format(p.coins)} coins\n" +
                            "Date: ${com.vibe.app.data.WalletRepo.niceTime(p.paidAt ?: p.createdAt)}\nOrder: ${p.orderId}\nPayment: ${p.paymentId ?: "-"}\nStatus: ${p.status}"
                        ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text), "Share receipt"))
                    }
                }
                Text(
                    "Problem with this payment?", color = VibeColors.TextMute, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSupport).padding(top = 16.dp),
                )
                Text(
                    "Coins are digital credits used inside VIBE. This receipt is generated by the VIBE server.",
                    color = Color(0xFFB093A2), fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                )
            }
        }
    }
}

@Composable
internal fun GhostBtn(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(16.dp)).background(VibeColors.Card).border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}
