package com.vibe.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Demo
import com.vibe.app.data.HostFilter
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.SelectChip
import com.vibe.app.ui.components.Segmented
import com.vibe.app.ui.components.cardSurface
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterScreen(initial: HostFilter, onClose: () -> Unit, onApply: (HostFilter) -> Unit) {
    val ctx = LocalContext.current
    var f by remember { mutableStateOf(initial) }
    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White, activeTrackColor = VibeColors.Pink, inactiveTrackColor = VibeColors.Line,
        activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
    )

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Cyan, 260.dp, Modifier.align(Alignment.TopEnd).offset(80.dp, (-80).dp))
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Card)
                        .border(1.dp, VibeColors.Line, RoundedCornerShape(12.dp)).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = Color.White, fontSize = 16.sp) }
                Text("Filter", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 16.dp))
                Text("Reset", color = VibeColors.Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { f = HostFilter(); ctx.toast("Filters reset") })
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Section("Show me") {
                    Segmented(listOf("all" to "✨ Everyone", "F" to "♀ Girls", "M" to "♂ Boys"), f.show, { f = f.copy(show = it) })
                }

                Section("Call type") {
                    Segmented(listOf("any" to "Any", "voice" to "Voice", "video" to "Video"), f.callType, { f = f.copy(callType = it) })
                }

                Column {
                    Row { Label("Age", Modifier.weight(1f)); Text("${f.minAge} – ${f.maxAge}${if (f.maxAge == 60) "+" else ""}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    RangeSlider(
                        value = f.minAge.toFloat()..f.maxAge.toFloat(),
                        onValueChange = { r -> f = f.copy(minAge = r.start.roundToInt(), maxAge = r.endInclusive.roundToInt().coerceAtLeast(r.start.roundToInt() + 1)) },
                        valueRange = 18f..60f, colors = sliderColors,
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Label("Max call price", Modifier.weight(1f))
                        CoinDot(14.dp); Spacer(Modifier.width(6.dp))
                        Text(if (f.maxRate >= 200) "Any" else "${f.maxRate}/min", color = VibeColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Slider(
                        value = f.maxRate.toFloat(), onValueChange = { f = f.copy(maxRate = ((it / 10).roundToInt() * 10)) },
                        valueRange = 30f..200f, steps = 16, colors = sliderColors,
                    )
                    Row { Text("30", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.weight(1f)); Text("200", color = VibeColors.TextMute, fontSize = 11.sp) }
                }

                Section("Speaks") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Demo.allLanguages.forEach { l ->
                            SelectChip(l, l in f.langs, { f = f.copy(langs = if (l in f.langs) f.langs - l else f.langs + l) })
                        }
                    }
                }

                Column(Modifier.fillMaxWidth().cardSurface()) {
                    ToggleRow("🟢", "Online now only", "Hide people who are offline", f.onlineOnly) { f = f.copy(onlineOnly = it) }
                    HorizontalDivider(color = VibeColors.Line)
                    ToggleRow("✅", "Verified only", "Face-verified profiles", f.verifiedOnly) { f = f.copy(verifiedOnly = it) }
                }

                Section("Sort by") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Segmented(listOf("online" to "🟢 Online first", "rating" to "⭐ Top rated"), f.sort, { f = f.copy(sort = it) }, 44.dp)
                        Segmented(listOf("price" to "🪙 Lowest price", "new" to "✨ Newest"), f.sort, { f = f.copy(sort = it) }, 44.dp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Box(Modifier.fillMaxWidth().background(VibeColors.Card.copy(alpha = 0.85f)).padding(20.dp)) {
                NeonButton("Show results", onClick = { onApply(f) })
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column { Label(title, Modifier.padding(bottom = 10.dp)); content() }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, color = VibeColors.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = modifier)
}

@Composable
private fun ToggleRow(icon: String, title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = VibeColors.TextMute, fontSize = 12.sp)
        }
        Switch(
            checked = on, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = VibeColors.Pink, checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF2A2A3A), uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}
