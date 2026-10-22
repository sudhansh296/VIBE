package com.vibe.app.ui.screens

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

/** VIP: benefits + plans. DEMO plans; the server (admin panel) owns plans, prices and bonus coins. */
@Composable
fun VipScreen(onBack: () -> Unit, onManage: () -> Unit) {
    val ctx = LocalContext.current
    var pick by remember { mutableStateOf(1) }
    val plans = MeDemo.vipPlans
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
                        color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                    Text("Talk more, pay less", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        "Already VIP? Manage your plan", color = VibeColors.Gold, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp).clip(CircleShape).border(1.dp, VibeColors.Gold.copy(alpha = 0.4f), CircleShape).clickable(onClick = onManage).padding(horizontal = 16.dp, vertical = 7.dp),
                    )
                }

                Column(Modifier.padding(top = 20.dp)) {
                    MeDemo.vipBenefits.forEachIndexed { i, b ->
                        Row(Modifier.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TileIcon(icons[i], VibeColors.Gold, 36.dp)
                            Column(Modifier.weight(1f)) {
                                Text(b.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(b.sub, color = VibeColors.TextMute, fontSize = 12.sp)
                            }
                        }
                        if (i < MeDemo.vipBenefits.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
                    }
                }

                SectionLabel("Choose a plan", Modifier.padding(top = 20.dp, bottom = 16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    plans.forEachIndexed { i, p -> PlanCard(p, i == pick) { pick = i } }
                }
                Text(
                    "Renews automatically. You can cancel anytime from Settings. Prices and bonus coins are set by VIBE.",
                    color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                )
            }

            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(GoldBrush)
                        .clickable { ctx.toast("Payment opens (Razorpay) · VIP starts after the server confirms") },
                    contentAlignment = Alignment.Center,
                ) { Text("Get VIP · ₹${"%,d".format(plans[pick].price)}", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
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
            Box(Modifier.size(20.dp).border(2.dp, if (on) VibeColors.Gold else Color(0xFF3A3A4A), CircleShape), contentAlignment = Alignment.Center) {
                if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(VibeColors.Gold))
            }
            Column(Modifier.weight(1f)) {
                Text(plan.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    CoinDot(14.dp)
                    Text(
                        "  +${"%,d".format(plan.bonusCoins)} bonus coins · ${plan.perMonth}", color = VibeColors.TextDim, fontSize = 12.sp,
                    )
                }
            }
            Text("₹${"%,d".format(plan.price)}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Light)
        }
        plan.tag?.let {
            Box(
                Modifier.align(Alignment.TopEnd).offset((-16).dp, (-10).dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Violet))).padding(horizontal = 10.dp, vertical = 3.dp),
            ) { Text(it.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp) }
        }
    }
}
