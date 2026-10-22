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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.Demo
import com.vibe.app.data.SettingsDemo
import com.vibe.app.data.Visitor
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.VibeIcons
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

/** Visitors: who viewed your profile. Non-VIP users see only the first few; the server hides the rest. DEMO list. */
@Composable
fun VisitorsScreen(onBack: () -> Unit, onChat: (Int) -> Unit, onCall: (Int, Boolean) -> Unit, onVip: () -> Unit) {
    var vip by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Visitors", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Row {
                    Text("${SettingsDemo.TOTAL_VISITORS}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(" people viewed your profile", color = VibeColors.TextDim, fontSize = 14.sp)
                }

                var last = ""
                SettingsDemo.visitors.forEachIndexed { i, v ->
                    if (v.group != last) {
                        last = v.group
                        Text(v.group, color = VibeColors.TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                    }
                    VisitorRow(v, hidden = !vip && i >= SettingsDemo.FREE_VISIBLE_VISITORS, onChat = onChat, onCall = onCall)
                }

                if (!vip) {
                    val shape = RoundedCornerShape(20.dp)
                    Column(
                        Modifier.padding(top = 20.dp).fillMaxWidth().clip(shape)
                            .background(Brush.linearGradient(listOf(Color(0x59784C0A), VibeColors.Card, VibeColors.Card)))
                            .border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), shape).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(painterResource(R.drawable.ic_vip), null, Modifier.size(56.dp))
                        Text("See everyone who visited you", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Text(
                            "Get the full list with names and time, and message or call them in one tap.", color = VibeColors.TextDim, fontSize = 12.sp,
                            textAlign = TextAlign.Center, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp),
                        )
                        Box(
                            Modifier.padding(top = 16.dp).height(44.dp).clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(VibeColors.Gold, Color(0xFFEAB308)))).clickable(onClick = onVip).padding(horizontal = 28.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Unlock with VIP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
                Text(
                    if (vip) "Preview as a normal user" else "Preview as VIP", color = VibeColors.TextMute, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { vip = !vip }.padding(vertical = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun VisitorRow(v: Visitor, hidden: Boolean, onChat: (Int) -> Unit, onCall: (Int, Boolean) -> Unit) {
    val host = Demo.host(v.hostId)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.weight(1f).then(if (hidden) Modifier.blur(7.dp) else Modifier),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(52.dp)) {
                if (hidden) Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF3A3A4C)), contentAlignment = Alignment.Center) {
                    Text("?", color = Color.White.copy(alpha = 0.7f), fontSize = 20.sp, fontWeight = FontWeight.Black)
                } else Avatar(host, Modifier.size(52.dp).clip(CircleShape), fontSize = 20.sp)
                if (v.online && !hidden) Box(Modifier.align(Alignment.BottomEnd).size(13.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.dp).clip(CircleShape).background(VibeColors.Green))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (hidden) "Someone" else "${host.name}, ${host.age}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (v.fresh && !hidden) {
                        Box(Modifier.padding(start = 6.dp).size(8.dp).clip(CircleShape).background(VibeColors.Pink))
                    }
                }
                Text(v.line, color = VibeColors.TextMute, fontSize = 11.sp)
            }
        }
        if (!hidden) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Card).border(1.dp, VibeColors.Line, RoundedCornerShape(12.dp)).clickable { onChat(v.hostId) },
                contentAlignment = Alignment.Center,
            ) { Icon(MeIcons.Chat, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(20.dp)) }
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(VibeBrush.Button).clickable { onCall(v.hostId, false) },
                contentAlignment = Alignment.Center,
            ) { Icon(VibeIcons.Video, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
}
