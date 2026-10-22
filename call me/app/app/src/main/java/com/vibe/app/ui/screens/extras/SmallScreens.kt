package com.vibe.app.ui.screens.extras

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.ConfirmDialog
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.KeyValueCard
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.VibeSwitch
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay

// ------------------------------------------------------------------------------------------------------------------------------
// Language

private val LANGUAGES = listOf(
    Triple("en", "English", "English"), Triple("hi", "हिंदी", "Hindi"), Triple("pa", "ਪੰਜਾਬੀ", "Punjabi"), Triple("bn", "বাংলা", "Bengali"),
    Triple("ta", "தமிழ்", "Tamil"), Triple("te", "తెలుగు", "Telugu"), Triple("mr", "मराठी", "Marathi"),
)

/** App language. DEMO: only the chosen language code is saved (on the server); the app ships its own text for each language. */
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var pick by remember { mutableStateOf("en") }
    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Cyan, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Language", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text(
                    "Choose the language of the app. This does not change the languages you speak on your profile.", color = VibeColors.TextMute, fontSize = 12.sp,
                    lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LANGUAGES.filter { it.first == "en" }.forEach { (k, name, english) ->
                        val on = k == pick
                        val shape = RoundedCornerShape(18.dp)
                        Row(
                            Modifier.fillMaxWidth().clip(shape)
                                .background(if (on) Brush.linearGradient(listOf(VibeColors.Pink.copy(alpha = 0.16f), VibeColors.Card)) else Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card)))
                                .border(1.5.dp, if (on) VibeColors.Pink else VibeColors.Line, shape).clickable { pick = k }.padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(Modifier.size(20.dp).border(2.dp, if (on) VibeColors.Pink else Color(0xFFFFD6E3), CircleShape), contentAlignment = Alignment.Center) {
                                if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(VibeColors.Pink))
                            }
                            Column {
                                Text(name, color = VibeColors.Ink, fontSize = 16.sp)
                                Text(english, color = VibeColors.TextMute, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Text(
                    "VIBE is in English for now. More languages will be added later.", color = Color(0xFFB093A2), fontSize = 11.sp,
                    lineHeight = 16.sp, modifier = Modifier.padding(vertical = 20.dp),
                )
            }
            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton("Save", onClick = { ctx.toast("Language saved"); onBack() })
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------------------------------------
// Manage VIP

/** Manage VIP: plan details, auto renew, change plan, receipts, cancel. DEMO; the server (and the payment provider) own the plan and dates. */
@Composable
fun VipManageScreen(onBack: () -> Unit, onChangePlan: () -> Unit, onReceipts: () -> Unit) {
    val ctx = LocalContext.current
    val floatY by rememberFloat()
    var auto by remember { mutableStateOf(true) }
    var askCancel by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Gold, 320.dp, Modifier.align(Alignment.TopCenter).offset(0.dp, (-120).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("My VIP", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_vip), null, Modifier.size(96.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Text(
                        if (auto) "VIP ACTIVE" else "ENDS 15 JAN", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 8.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(VibeColors.Gold, Color(0xFFEAB308)))).padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                KeyValueCard(
                    listOf("Plan" to "3 Months", (if (auto) "Renews on" else "Ends on") to "15 Jan 2027", "Price" to "₹699 / 3 months", "Paid with" to "UPI · rahul@okaxis"),
                    modifier = Modifier.padding(top = 20.dp),
                )
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto renew", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(if (auto) "VIP renews automatically" else "Auto renew is off", color = VibeColors.TextMute, fontSize = 12.sp)
                    }
                    VibeSwitch(auto, { if (auto) askCancel = true else { auto = true; ctx.toast("Auto renew on") } })
                }
                Text("YOUR VIP BENEFITS", color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.6.sp, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    listOf("Cheaper video calls", "Free chat with every host", "VIP badge and profile frame", "Full visitors list").forEach {
                        Row(Modifier.padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("✓", color = VibeColors.Gold); Text(it, color = Color(0xFF5A3F4D), fontSize = 13.sp)
                        }
                    }
                }
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostBtn("Change plan", Modifier.weight(1f), onChangePlan)
                    GhostBtn("Receipts", Modifier.weight(1f), onReceipts)
                }
                if (auto) Text(
                    "Cancel VIP", color = VibeColors.Red, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().noRippleClick { askCancel = true }.padding(top = 16.dp),
                )
                Text(
                    "If you cancel, VIP stays until the end of the period you paid for. No refund for the time left.", color = Color(0xFFB093A2), fontSize = 11.sp,
                    lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                )
            }
        }
    }

    if (askCancel) ConfirmDialog(
        title = "Cancel VIP?", message = "You will keep VIP until 15 Jan 2027. After that you lose cheaper calls, free chat and the full visitors list.", confirmText = "Cancel VIP",
        onConfirm = { auto = false; askCancel = false; ctx.toast("Auto renew turned off") }, onDismiss = { askCancel = false },
    )
}

// ------------------------------------------------------------------------------------------------------------------------------
// Gifts history

/**
 * Gift history from the server. Only hosts receive gifts and only users send them, so a host sees what was received
 * (with what it earned) and a user sees what was sent (with what it cost).
 */
@Composable
fun GiftsHistoryScreen(onBack: () -> Unit) {
    val iAmHost = com.vibe.app.data.api.AuthStore.me?.optString("role") == "host"
    var rows by remember { mutableStateOf<List<com.vibe.app.data.LedgerItem>?>(null) }
    var failed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        // gift lines from the host's earnings / the user's coin history (a few pages is plenty here)
        runCatching {
            val out = mutableListOf<com.vibe.app.data.LedgerItem>()
            var page: Int? = 1
            while (page != null && page <= 5) {
                val (items, next) = com.vibe.app.data.WalletRepo.history(page, earnings = iAmHost)
                out += items.filter { it.kind.startsWith("gift") }
                page = next
            }
            out
        }.onSuccess { rows = it }.onFailure { failed = it.message ?: "Could not load gifts" }
    }
    val list = rows.orEmpty()
    val total = list.sumOf { kotlin.math.abs(it.coins) }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Gold, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader(if (iAmHost) "Gifts received" else "Gifts sent", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(Color(0x33F2B84B), VibeColors.Card, VibeColors.Card))).border(1.dp, VibeColors.Gold.copy(alpha = 0.3f), shape).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (iAmHost) "YOU EARNED FROM GIFTS" else "YOU SPENT ON GIFTS", color = VibeColors.TextDim, fontSize = 10.sp, letterSpacing = 1.6.sp)
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoinDot(28.dp)
                            Text("%,d".format(total), color = VibeColors.Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("${list.size} gifts", color = VibeColors.TextDim, fontSize = 12.sp)
                }
                when {
                    failed != null -> Text(failed!!, color = VibeColors.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp))
                    rows == null -> Text("Loading…", color = VibeColors.TextMute, fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp))
                    list.isEmpty() -> Text(if (iAmHost) "No gifts received yet" else "You have not sent any gifts yet", color = VibeColors.TextMute, fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp))
                }
                list.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFEDF3)).border(1.dp, Color(0xFFFFD6E3), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(CallIcons.Gift, null, tint = VibeColors.Gold, modifier = Modifier.size(24.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(r.title, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(giftTime(r.at), color = VibeColors.TextMute, fontSize = 11.sp)
                        }
                        Text((if (r.coins >= 0) "+" else "−") + "%,d".format(kotlin.math.abs(r.coins)), color = if (r.coins >= 0) VibeColors.Green else Color(0xFF5A3F4D), fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.05f)))
                }
            }
        }
    }
}

/** "22 Oct 2026, 2:17 pm" in the phone's time zone from the server's UTC time. */
private fun giftTime(at: String): String = runCatching {
    val utc = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(at.take(19))!!
    java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.ENGLISH).format(utc).replace("AM", "am").replace("PM", "pm")
}.getOrDefault(at)
