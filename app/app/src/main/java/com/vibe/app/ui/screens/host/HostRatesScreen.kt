package com.vibe.app.ui.screens.host

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Demo
import com.vibe.app.data.HostDemo
import com.vibe.app.data.RateCfg
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.PillChip
import com.vibe.app.ui.components.VibeSwitch
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

/** Call settings: voice / video on-off, per-minute rates, message price. DEMO config; the server owns ranges and re-checks values. */
@Composable
fun HostRatesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var voiceOn by remember { mutableStateOf(true) }
    var videoOn by remember { mutableStateOf(true) }
    var voice by remember { mutableStateOf(HostDemo.voice.start) }
    var video by remember { mutableStateOf(HostDemo.video.start) }
    var msg by remember { mutableStateOf(HostDemo.message.start) }

    fun clamp(cfg: RateCfg, v: Int): Int? {
        if (v > cfg.unlocked) { ctx.toast("Higher rates unlock as your host level grows"); return null }
        return v.coerceIn(cfg.min, cfg.max)
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Call settings", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RateCard(
                    "Voice calls", "Accept audio-only calls", CallIcons.Mic, VibeColors.Pink, HostDemo.voice, voice, "coins per minute",
                    switch = voiceOn, onSwitch = { if (voiceOn && !videoOn) ctx.toast("Keep at least one call type on") else voiceOn = !voiceOn },
                    onChange = { v -> clamp(HostDemo.voice, v)?.let { voice = it } }, showEarn = true,
                )
                RateCard(
                    "Video calls", "Accept video calls", CallIcons.Camera, VibeColors.Cyan, HostDemo.video, video, "coins per minute",
                    switch = videoOn, onSwitch = { if (videoOn && !voiceOn) ctx.toast("Keep at least one call type on") else videoOn = !videoOn },
                    onChange = { v -> clamp(HostDemo.video, v)?.let { video = it } }, showEarn = true,
                )
                RateCard(
                    "Paid chat", "Price per message", CallIcons.Send, VibeColors.Gold, HostDemo.message, msg, "coins per message",
                    switch = null, onSwitch = {}, onChange = { v -> clamp(HostDemo.message, v)?.let { msg = it } }, showEarn = false,
                    footnote = "Every user gets ${Demo.FREE_MESSAGES} free messages with you. After that they pay this price for each message.",
                )
                Text(
                    "Your earning share, the allowed price range and the free message count are set by VIBE. Changes apply to new calls only.",
                    color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
            }
            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton("Save settings", onClick = { ctx.toast("Saved · applies to new calls"); onBack() })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RateCard(
    title: String, sub: String, icon: ImageVector, tint: Color, cfg: RateCfg, value: Int, unit: String,
    switch: Boolean?, onSwitch: () -> Unit, onChange: (Int) -> Unit, showEarn: Boolean, footnote: String? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    val enabled = switch != false
    Column(Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.10f)).border(1.dp, tint.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(sub, color = VibeColors.TextMute, fontSize = 12.sp)
            }
            if (switch != null) VibeSwitch(switch, onSwitch)
        }
        Column(Modifier.padding(top = 16.dp).alpha(if (enabled) 1f else 0.4f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StepBtn("−", enabled) { onChange(value - cfg.step) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CoinDot(24.dp)
                        Text("$value", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraLight)
                    }
                    Text(unit, color = VibeColors.TextMute, fontSize = 11.sp)
                }
                StepBtn("+", enabled) { onChange(value + cfg.step) }
            }
            Text("Allowed range ${cfg.min} to ${cfg.max}", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
            FlowRow(
                Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                cfg.chips.forEach { c -> PillChip("$c", c == value, { if (enabled) onChange(c) }, locked = c > cfg.unlocked, gold = true) }
            }
            if (showEarn) {
                val coins = Math.round(value * HostDemo.SHARE_PCT / 100f)
                Text(
                    "You earn about $coins coins (₹${"%.1f".format(coins * HostDemo.COIN_INR)}) every minute", color = VibeColors.Green, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        if (footnote != null) Text(footnote, color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun StepBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.size(40.dp).clip(shape).background(Color(0xFF1A1A26)).border(1.dp, VibeColors.Line, shape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 20.sp) }
}
