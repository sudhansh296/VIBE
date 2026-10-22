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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Demo
import com.vibe.app.data.HostDemo
import com.vibe.app.data.api.Api
import com.vibe.app.data.api.ApiException
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.PillChip
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.launch
import org.json.JSONObject

/** The allowed range for one price. It comes from the server (set by the admin); the server checks every value again. */
private data class PriceRange(val min: Int, val max: Int, val step: Int, val chips: List<Int>)

/**
 * Call settings (hosts): the video call price per minute and the paid-chat price per message. The host can tap a preset,
 * use + / -, or type any amount; it must stay inside the range the admin set. Saved on the server.
 */
@Composable
fun HostRatesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var video by remember { mutableIntStateOf(0) }
    var msg by remember { mutableIntStateOf(0) }
    var videoRange by remember { mutableStateOf(PriceRange(40, 400, 10, emptyList())) }
    var msgRange by remember { mutableStateOf(PriceRange(50, 1000, 50, emptyList())) }
    var sharePct by remember { mutableIntStateOf(HostDemo.SHARE_PCT) }

    LaunchedEffect(Unit) {
        try {
            val r = Api.get("/host/rates")
            val l = r.getJSONObject("limits")
            videoRange = PriceRange(l.getInt("callRateMin"), l.getInt("callRateMax"), 10, listOf(40, 60, 80, 120, 200, 400))
            msgRange = PriceRange(l.getInt("messagePriceMin"), l.getInt("messagePriceMax"), 50, listOf(100, 200, 300, 400, 600))
            sharePct = l.getInt("sharePct")
            video = r.getInt("callRate"); msg = r.getInt("messagePrice")
            loaded = true
        } catch (e: ApiException) {
            ctx.toast(e.message); onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Call settings", onBack)
            if (loaded) Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RateCard(
                    "Video call rate", "Price users pay per minute", CallIcons.Camera, VibeColors.Cyan, videoRange, video, "coins per minute",
                    onChange = { video = it }, earnPct = sharePct,
                )
                RateCard(
                    "Paid chat", "Price per message", CallIcons.Send, VibeColors.Gold, msgRange, msg, "coins per message",
                    onChange = { msg = it }, earnPct = null,
                    footnote = "Every user gets ${Demo.FREE_MESSAGES} free messages with you. After that they pay this price for each message.",
                )
                Text(
                    "The allowed range and your earning share are set by VIBE. Changes apply to new calls only.",
                    color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
            } else Box(Modifier.weight(1f))
            val valid = video in videoRange.min..videoRange.max && msg in msgRange.min..msgRange.max
            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton("Save settings", enabled = loaded && valid && !saving, loading = saving, onClick = {
                    saving = true
                    scope.launch {
                        try {
                            Api.put("/host/rates", JSONObject().put("callRate", video).put("messagePrice", msg))
                            ctx.toast("Saved · applies to new calls")
                            onBack()
                        } catch (e: ApiException) {
                            ctx.toast(e.message)
                        }
                        saving = false
                    }
                })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RateCard(
    title: String, sub: String, icon: ImageVector, tint: Color, range: PriceRange, value: Int, unit: String,
    onChange: (Int) -> Unit, earnPct: Int?, footnote: String? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    // what the host types; may be outside the range while typing, the save button waits until it is valid
    var typed by remember(value) { mutableStateOf(if (value > 0) value.toString() else "") }
    val inRange = value in range.min..range.max
    Column(Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.10f)).border(1.dp, tint.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, color = VibeColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(sub, color = VibeColors.TextMute, fontSize = 12.sp)
            }
        }
        Column(Modifier.padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StepBtn("−") { onChange((value - range.step).coerceIn(range.min, range.max)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CoinDot(24.dp)
                        // tap the number and type any amount
                        BasicTextField(
                            value = typed,
                            onValueChange = { t -> val d = t.filter(Char::isDigit).take(5); typed = d; onChange(d.toIntOrNull() ?: 0) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(color = if (inRange) VibeColors.Ink else VibeColors.Red, fontSize = 36.sp, fontWeight = FontWeight.ExtraLight, textAlign = TextAlign.Center),
                            cursorBrush = SolidColor(VibeColors.Pink),
                            modifier = Modifier.width(110.dp),
                        )
                    }
                    Text(unit + "  ·  tap to type", color = VibeColors.TextMute, fontSize = 11.sp)
                }
                StepBtn("+") { onChange((value + range.step).coerceIn(range.min, range.max)) }
            }
            Text(
                if (inRange) "Allowed range ${range.min} to ${range.max}" else "Enter an amount between ${range.min} and ${range.max}",
                color = if (inRange) VibeColors.TextMute else VibeColors.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp),
            )
            val chips = range.chips.filter { it in range.min..range.max }
            if (chips.isNotEmpty()) FlowRow(
                Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { c -> PillChip("$c", c == value, { onChange(c) }, gold = true) }
            }
            if (earnPct != null && inRange) {
                val coins = Math.round(value * earnPct / 100f)
                Text(
                    "You earn about $coins coins every minute", color = VibeColors.Green, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        if (footnote != null) Text(footnote, color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.size(40.dp).clip(shape).background(Color(0xFFFFEDF3)).border(1.dp, VibeColors.Line, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = VibeColors.Pink, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
}
