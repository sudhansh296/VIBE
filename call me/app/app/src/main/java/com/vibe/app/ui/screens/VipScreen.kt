package com.vibe.app.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.MeDemo
import com.vibe.app.data.VipPlan
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.TileIcon
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

private val GoldBrush = Brush.horizontalGradient(listOf(VibeColors.Gold, Color(0xFFEAB308)))

/** VIP: benefits + plans from the server (the admin sets plans, prices, bonus coins and the call discount). */
@Composable
fun VipScreen(onBack: () -> Unit, onManage: () -> Unit) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val checkout = com.vibe.app.ui.components.rememberCoinCheckout()
    var pick by remember { mutableStateOf(1) }
    var plans by remember { mutableStateOf<List<ServerVipPlan>>(emptyList()) }
    var vipUntil by remember { mutableStateOf<String?>(null) }
    var discount by remember { mutableStateOf(20) }
    var failed by remember { mutableStateOf<String?>(null) }
    suspend fun load() {
        runCatching { com.vibe.app.data.api.Api.get("/vip") }.onSuccess { r ->
            val a = r.getJSONArray("plans")
            plans = List(a.length()) { i -> a.getJSONObject(i).let { ServerVipPlan(it.getInt("id"), it.getString("name"), it.getInt("days"), it.getDouble("price").toInt(), it.optInt("bonusCoins"), it.optString("tag").takeIf { t -> t.isNotBlank() && t != "null" }) } }
            vipUntil = if (r.optBoolean("vip")) r.optString("vipUntil") else null
            discount = r.optInt("callDiscountPct", 20)
            if (pick >= plans.size) pick = 0
            failed = null
        }.onFailure { failed = it.message ?: "Could not load VIP plans" }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { load() }
    val benefits = listOf(
        com.vibe.app.data.Benefit("Cheaper calls", "Video calls cost $discount% less with every host"),
        com.vibe.app.data.Benefit("Free chat with every host", "No per-message charge, ever"),
        com.vibe.app.data.Benefit("Bonus coins", "Coins added the moment you buy"),
        com.vibe.app.data.Benefit("VIP badge", "Shown on your profile and to hosts"),
        com.vibe.app.data.Benefit("See who visited you", "Full visitor list with time"),
    )
    val floatY by rememberFloat()
    val icons = listOf(MeIcons.Discount, MeIcons.Chat, CallIcons.Coin, MeIcons.Star, MeIcons.Eye)

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Gold, 320.dp, Modifier.align(Alignment.TopCenter).offset(0.dp, (-120).dp))

        Column(Modifier.fillMaxSize()) {
            PageHeader("", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Column(Modifier.fillMaxWidth().padding(top = 0.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_vip), null, Modifier.size(96.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = VibeColors.Gold)) { append("VIBE") }
                            append(" VIP")
                        },
                        color = VibeColors.Ink, fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                    Text("Talk more, pay less", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    vipUntil?.let { until ->
                        Text(
                            "★ VIP active until ${vipDate(until)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp).clip(CircleShape).background(GoldBrush).padding(horizontal = 16.dp, vertical = 7.dp),
                        )
                    }
                    failed?.let { Text("$it · tap to retry", color = VibeColors.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp).clickable { scope.launch { load() } }) }
                }

                Column(Modifier.padding(top = 20.dp)) {
                    benefits.forEachIndexed { i, b ->
                        Row(Modifier.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TileIcon(icons[i], VibeColors.Gold, 36.dp)
                            Column(Modifier.weight(1f)) {
                                Text(b.title, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(b.sub, color = VibeColors.TextMute, fontSize = 12.sp)
                            }
                        }
                        if (i < benefits.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.06f)))
                    }
                }

                SectionLabel("Choose a plan", Modifier.padding(top = 20.dp, bottom = 16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    plans.forEachIndexed { i, p -> PlanCard(VipPlan(p.name, p.price, p.bonusCoins, perMonth(p), p.tag), i == pick) { pick = i } }
                }
                Text(
                    "One-time payment, no auto-renew. Buying again adds the days to your VIP. Prices and bonus coins are set by VIBE.",
                    color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                )
            }

            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(GoldBrush)
                        .clickable {
                            val plan = plans.getOrNull(pick) ?: return@clickable
                            checkout.buyVip(plan.id) { kind, _ ->
                                scope.launch {
                                    load()
                                    runCatching { com.vibe.app.data.api.Api.get("/me") }.onSuccess { com.vibe.app.data.api.AuthStore.saveUser(it.getJSONObject("user")) }
                                    ctx.toast(when (kind) { "success" -> "You are VIP! ★"; "pending" -> "Payment received · VIP starts in a moment"; else -> "Payment failed · no money taken" })
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val p = plans.getOrNull(pick)
                    Text(
                        when { checkout.paying -> "Opening payment…"; p == null -> "Loading plans…"; vipUntil != null -> "Extend VIP · ₹${"%,d".format(p.price)}"; else -> "Get VIP · ₹${"%,d".format(p.price)}" },
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
    checkout.TestSheet()
}

@Composable
private fun PlanCard(plan: VipPlan, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box {
        Row(
            Modifier.fillMaxWidth().clip(shape)
                .background(if (on) Brush.linearGradient(listOf(VibeColors.Gold.copy(alpha = 0.14f), VibeColors.Card)) else Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card)))
                .border(1.5.dp, if (on) VibeColors.Gold else VibeColors.Line, shape)
                .clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(20.dp).border(2.dp, if (on) VibeColors.Gold else Color(0xFFFFD6E3), CircleShape), contentAlignment = Alignment.Center) {
                if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(VibeColors.Gold))
            }
            Column(Modifier.weight(1f)) {
                Text(plan.name, color = VibeColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    CoinDot(14.dp)
                    Text(
                        "  +${"%,d".format(plan.bonusCoins)} bonus coins · ${plan.perMonth}", color = VibeColors.TextDim, fontSize = 12.sp,
                    )
                }
            }
            Text("₹${"%,d".format(plan.price)}", color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Light)
        }
        plan.tag?.let {
            Box(
                Modifier.align(Alignment.TopEnd).offset((-16).dp, (-10).dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Violet))).padding(horizontal = 10.dp, vertical = 3.dp),
            ) { Text(it.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp) }
        }
    }
}

/** A VIP plan as the server sends it. */
private data class ServerVipPlan(val id: Int, val name: String, val days: Int, val price: Int, val bonusCoins: Int, val tag: String?)

private fun perMonth(p: ServerVipPlan): String {
    val months = (p.days / 30).coerceAtLeast(1)
    return if (months <= 1) "₹${p.price}/month" else "₹${p.price / months}/month"
}

/** "21 Nov 2026" from the server's date. */
internal fun vipDate(at: String): String = runCatching {
    val d = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(at.take(19))!!
    java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.ENGLISH).format(d)
}.getOrDefault(at.take(10))
