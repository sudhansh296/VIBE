package com.vibe.app.ui.screens.safety

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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Demo
import com.vibe.app.data.SafetyStore
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.theme.VibeColors

/** Report a user. DEMO: the report goes to the admin moderation queue (reason, text, screenshot, reporter, reported, where it happened). */
@Composable
fun ReportUserScreen(hostId: Int, onBack: () -> Unit, onSafetyCenter: () -> Unit) {
    val h = remember(hostId) { Demo.host(hostId) }
    var pick by remember { mutableStateOf(-1) }
    var text by remember { mutableStateOf("") }
    var shot by remember { mutableStateOf(false) }
    var alsoBlock by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    val reasons = SafetyStore.reportReasons
    val canSend = pick >= 0 && (pick != reasons.lastIndex || text.trim().length >= 5)

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Red, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        if (!sent) Column(Modifier.fillMaxSize()) {
            PageHeader("Report", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.padding(top = 8.dp).fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(h, Modifier.size(44.dp).clip(CircleShape), fontSize = 18.sp)
                    Column(Modifier.weight(1f)) {
                        Text("${h.name}, ${h.age}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Your report is private. They will not know it was you.", color = VibeColors.TextMute, fontSize = 11.sp)
                    }
                }

                SectionLabel("What is the problem?", Modifier.padding(top = 24.dp, bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reasons.forEachIndexed { i, (title, sub) -> ReasonRow(title, sub, i == pick) { pick = i } }
                }

                SectionLabel("Tell us more", Modifier.padding(top = 24.dp, bottom = 12.dp))
                VibeField(text, { text = it.take(300) }, "What happened? (optional, but it helps us act faster)", singleLine = false, minHeight = 90.dp)
                val sShape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).clip(sShape)
                        .border(1.dp, if (shot) VibeColors.Green.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f), sShape).clickable { shot = !shot },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(CallIcons.Photo, null, tint = if (shot) VibeColors.Green else VibeColors.TextDim, modifier = Modifier.size(20.dp))
                    Text(if (shot) "  Screenshot added ✓" else "  Add a screenshot (optional)", color = if (shot) VibeColors.Green else VibeColors.TextDim, fontSize = 14.sp)
                }

                Row(Modifier.padding(top = 20.dp).clickable { alsoBlock = !alsoBlock }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(if (alsoBlock) VibeColors.Pink else Color.Transparent)
                            .border(2.dp, if (alsoBlock) VibeColors.Pink else Color(0xFF4B5563), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) { if (alsoBlock) Icon(MeIcons.Tick, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                    Text("Also block this person", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                }
                Text(
                    "If someone is in danger, contact your local emergency number first. Our team reviews every report, usually within 24 hours.",
                    color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                )
            }
            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton("Submit report", enabled = canSend, onClick = {
                    if (alsoBlock) SafetyStore.block(hostId)
                    sent = true
                })
            }
        } else Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(96.dp).clip(CircleShape).border(2.dp, VibeColors.Green, CircleShape), contentAlignment = Alignment.Center) {
                Icon(MeIcons.Tick, null, tint = VibeColors.Green, modifier = Modifier.size(48.dp))
            }
            Text("Thanks for telling us", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
            Text(
                (if (alsoBlock) "${h.name} is blocked. " else "") + "Our safety team will review this within 24 hours. You will get a notification when we take action.",
                color = VibeColors.TextDim, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
            )
            Box(
                Modifier.padding(top = 32.dp).height(48.dp).clip(CircleShape).background(Color.White).clickable(onClick = onBack).padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Done", color = VibeColors.Dark, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            Text("Visit the Safety center", color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp).clickable(onClick = onSafetyCenter))
        }
    }
}

@Composable
private fun ReasonRow(title: String, sub: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (on) Brush.linearGradient(listOf(VibeColors.Pink.copy(alpha = 0.16f), VibeColors.Card)) else Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card)))
            .border(1.5.dp, if (on) VibeColors.Pink else VibeColors.Line, shape).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(20.dp).border(2.dp, if (on) VibeColors.Pink else Color(0xFF3A3A4A), CircleShape), contentAlignment = Alignment.Center) {
            if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(VibeColors.Pink))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            if (sub.isNotEmpty()) Text(sub, color = VibeColors.TextMute, fontSize = 11.sp)
        }
    }
}
