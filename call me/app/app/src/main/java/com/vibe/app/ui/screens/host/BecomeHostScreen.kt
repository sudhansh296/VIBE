package com.vibe.app.ui.screens.host

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.theme.VibeColors

/** Become a host: perks, how it works, agency hint. Hosting is open to everyone 18+; the agency code is optional. */
@Composable
fun BecomeHostScreen(onBack: () -> Unit, onApply: () -> Unit) {
    val floatY by rememberFloat()
    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 300.dp, Modifier.align(Alignment.TopStart).offset((-90).dp, (-110).dp))
        GlowBlob(VibeColors.Cyan, 240.dp, Modifier.align(Alignment.CenterEnd).offset(100.dp, (-20).dp))

        Column(Modifier.fillMaxSize()) {
            PageHeader("", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ghost_profile), null, Modifier.size(128.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Text(
                        buildAnnotatedString {
                            append("Earn by ")
                            withStyle(SpanStyle(color = VibeColors.Pink, fontWeight = FontWeight.SemiBold)) { append("talking") }
                        },
                        color = VibeColors.Ink, fontSize = 30.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Become a VIBE host. Open for everyone 18+, whether you are a girl or a guy.", color = VibeColors.TextDim, fontSize = 14.sp,
                        textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    )
                }

                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Perk("Your rates", "Set your own video call rate per minute", VibeColors.Pink, Modifier.weight(1f))
                        Perk("Paid chat", "Earn from messages after the free ones", VibeColors.Cyan, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Perk("Gifts", "Fans send you gifts during calls", VibeColors.Gold, Modifier.weight(1f))
                        Perk("Withdraw", "Cash out to your UPI account", VibeColors.Green, Modifier.weight(1f))
                    }
                }

                SectionLabel("How it works", Modifier.padding(top = 28.dp, bottom = 16.dp))
                Column {
                    Step(1, "Fill the application", "Photos, intro video, a short bio and languages", last = false)
                    Step(2, "Verify yourself", "A live selfie, PAN and an ID so users know you are real", last = false)
                    Step(3, "Get approved", "The VIBE team reviews it, usually within 24 hours", last = false)
                    Step(4, "Go online and earn", "Switch your status on and answer calls", last = true)
                }

                val gShape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.padding(top = 8.dp).fillMaxWidth().clip(gShape).background(VibeColors.Gold.copy(alpha = 0.05f))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.30f), gShape).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(MeIcons.Group, null, tint = VibeColors.Gold, modifier = Modifier.size(24.dp))
                    Column {
                        Text("Have an agency code?", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "If an agency invited you, add its code in the application. You can also join or leave an agency later from your host dashboard.",
                            color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Text(
                    "Rate limits and your earning share are set by VIBE.", color = VibeColors.TextMute, fontSize = 11.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }

            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton("Apply to be a host", onApply)
            }
        }
    }
}

@Composable
private fun Perk(title: String, sub: String, color: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(14.dp)) {
        Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(sub, color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Step(n: Int, title: String, sub: String, last: Boolean) {
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                if (!last) drawLine(
                    Brush.verticalGradient(listOf(VibeColors.Pink, VibeColors.Line)),
                    Offset(18.dp.toPx(), 40.dp.toPx()), Offset(18.dp.toPx(), size.height), strokeWidth = 2.dp.toPx(),
                )
            }
            .padding(bottom = if (last) 12.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(VibeColors.Pink.copy(alpha = 0.12f)).border(1.5.dp, VibeColors.Pink, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("$n", color = VibeColors.Pink, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        Column {
            Text(title, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
