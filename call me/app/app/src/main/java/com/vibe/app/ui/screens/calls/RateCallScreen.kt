package com.vibe.app.ui.screens.calls

import kotlinx.coroutines.launch

import com.vibe.app.ui.components.CallDark
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.CallResult
import com.vibe.app.data.Demo
import com.vibe.app.ui.components.CallBackground
import com.vibe.app.ui.components.CallLabel
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.NeonRingAvatar
import com.vibe.app.ui.components.formatCallTime
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

private val starLabels = listOf("", "Bad 😞", "Not great 😕", "Okay 🙂", "Good 😊", "Amazing! 🤩")
private val goodTags = listOf("😊 Friendly", "🎵 Great vibe", "😂 Funny", "🧠 Smart", "🗣️ Good listener", "✨ Respectful")
private val badTags = listOf("🔇 Bad audio", "📵 Bad video", "😶 Didn't talk", "⏱️ Ended early", "🚫 Rude", "⚠️ Inappropriate")

/** Call summary + rating. The rating and report go to the server. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RateCallScreen(result: CallResult, onDone: () -> Unit, onCallAgain: (CallResult) -> Unit, callId: String? = null, onReport: (Int) -> Unit = {}) {
    val ctx = LocalContext.current
    val h = com.vibe.app.data.HostRepo.cache[result.hostId] ?: Demo.host(result.hostId)
    val iAmHost = com.vibe.app.data.api.AuthStore.me?.optString("role") == "host"
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var stars by remember { mutableIntStateOf(0) }
    val picked = remember { mutableStateListOf<String>() }
    var note by remember { mutableStateOf("") }
    var followed by remember { mutableStateOf(false) }

    CallBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.End) {
                Text("SKIP", color = CallLabel, fontSize = 11.sp, letterSpacing = 1.6.sp, modifier = Modifier.clickable(onClick = onDone))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NeonRingAvatar(h.name.take(1), 104.dp, 36.sp, host = h)
                Text("Call with ${h.name} ended", color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 18.dp))

                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat(formatCallTime(result.secs), "Duration", Modifier.weight(1f))
                    Stat("%,d".format(result.spent), if (iAmHost) "Coins earned" else "Coins spent", Modifier.weight(1f), coin = true)
                    if (!iAmHost) Stat("${result.gifts}", "Gifts sent", Modifier.weight(1f))
                    else if (result.gifts > 0) Stat("${result.gifts}", "Gifts", Modifier.weight(1f))   // their share is in "Coins earned"
                }

                if (!iAmHost) Text("HOW WAS YOUR CALL?", color = Color(0xFFFFE3EF), fontSize = 11.sp, letterSpacing = 1.8.sp, modifier = Modifier.padding(top = 30.dp))
                if (!iAmHost) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { i ->
                        val s by animateFloatAsState(if (i == stars) 1.25f else 1f, spring(dampingRatio = 0.35f), label = "star")
                        Text("★", fontSize = 38.sp, color = if (i <= stars) Color(0xFFFFD166) else Color(0x80FFFFFF),
                            modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s }.noRippleClick { stars = i; picked.clear() })
                    }
                }
                if (!iAmHost) Text(starLabels[stars], color = Color(0xFFFFC2DD), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp).height(18.dp))

                if (stars > 0) FlowRow(
                    Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (if (stars >= 4) goodTags else badTags).forEach { t ->
                        val on = t in picked
                        Text(t, color = if (on) Color.White else Color(0xFFFFE3EF), fontSize = 12.sp,
                            modifier = Modifier.clip(CircleShape).background(if (on) VibeColors.Pink.copy(alpha = 0.12f) else Color.Transparent)
                                .border(1.dp, if (on) VibeColors.Pink else Color.White.copy(alpha = 0.16f), CircleShape)
                                .clickable { if (on) picked.remove(t) else picked.add(t) }.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }

                if (!iAmHost) Box(
                    Modifier.padding(top = 18.dp).fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp)).padding(14.dp),
                ) {
                    if (note.isEmpty()) Text("Anything else? (optional, only our team sees this)", color = Color(0xFFFFB3D1), fontSize = 13.sp)
                    BasicTextField(note, { note = it.take(140) }, textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        cursorBrush = SolidColor(VibeColors.Pink), modifier = Modifier.fillMaxSize())
                }

                if (!iAmHost && !h.following) Row(
                    Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeonRingAvatar(h.name.take(1), 44.dp, 15.sp, host = h)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Follow ${h.name}", color = Color.White, fontSize = 14.sp)
                        Text("Get notified when they're online", color = Color(0xFFFFC2DD), fontSize = 11.sp)
                    }
                    Box(
                        Modifier.height(34.dp).clip(CircleShape).background(if (followed) Color.Transparent else Color.White)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape).clickable {
                                followed = true
                                if (h.real) scope.launch { runCatching { com.vibe.app.data.HostRepo.follow(h.id, true) } }
                            }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (followed) "✓ Following" else "Follow", color = if (followed) Color.White else CallDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }

                Text("🚩 Report a problem", color = Color(0xFFFFD0DC), fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 18.dp).clickable { onReport(h.id) })
            }

            if (iAmHost) Box(Modifier.fillMaxWidth().padding(20.dp)) {
                Box(Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(Color.White).clickable(onClick = onDone), contentAlignment = Alignment.Center) {
                    Text("Done", color = VibeColors.Pink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.weight(1f).height(52.dp).clip(CircleShape).border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape).clickable { onCallAgain(result) },
                    contentAlignment = Alignment.Center,
                ) { Text("Call again", color = Color.White, fontSize = 14.sp) }
                Box(
                    Modifier.weight(1.4f).height(52.dp).clip(CircleShape).background(if (stars > 0) Color.White else Color.White.copy(alpha = 0.2f))
                        .clickable(enabled = stars > 0) {
                            // the rating goes to the server (it shows as the host's stars); tags are sent without their emoji
                            if (callId != null) scope.launch {
                                runCatching {
                                    com.vibe.app.data.api.Api.post("/calls/$callId/rate", org.json.JSONObject().put("stars", stars)
                                        .put("tags", org.json.JSONArray(picked.map { it.substringAfter(' ') })))
                                }
                            }
                            ctx.toast("Thanks for your feedback! 💖"); onDone()
                        },
                    contentAlignment = Alignment.Center,
                ) { Text("Submit", color = if (stars > 0) VibeColors.Pink else Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, coin: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (coin) { CoinDot(14.dp); Spacer(Modifier.width(4.dp)) }
            Text(value, color = if (coin) Color(0xFFFFD166) else Color.White, fontSize = 17.sp, fontWeight = FontWeight.Light)
        }
        Text(label.uppercase(), color = Color(0xFFFFB3D1), fontSize = 9.sp, letterSpacing = 1.2.sp)
    }
}
