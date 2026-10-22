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
                    LANGUAGES.forEach { (k, name, english) ->
                        val on = k == pick
                        val shape = RoundedCornerShape(18.dp)
                        Row(
                            Modifier.fillMaxWidth().clip(shape)
                                .background(if (on) Brush.linearGradient(listOf(VibeColors.Pink.copy(alpha = 0.16f), VibeColors.Card)) else Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card)))
                                .border(1.5.dp, if (on) VibeColors.Pink else VibeColors.Line, shape).clickable { pick = k }.padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(Modifier.size(20.dp).border(2.dp, if (on) VibeColors.Pink else Color(0xFF3A3A4A), CircleShape), contentAlignment = Alignment.Center) {
                                if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(VibeColors.Pink))
                            }
                            Column {
                                Text(name, color = Color.White, fontSize = 16.sp)
                                Text(english, color = VibeColors.TextMute, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Text(
                    "More languages are added over time. Some text such as host bios stays in the language the host wrote it.", color = Color(0xFF4B5563), fontSize = 11.sp,
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
// Change phone number

/** Change phone number: new number, OTP (demo code 123456), done. The server sends and checks the OTP and rejects numbers that already have an account. */
@Composable
fun ChangePhoneScreen(onBack: () -> Unit, onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf(30) }

    LaunchedEffect(step, seconds) {
        if (step == 1 && seconds > 0) { delay(1000); seconds-- }
    }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(step) { if (step == 1) runCatching { focus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Change phone number", { if (step == 1) step = 0 else onBack() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                when (step) {
                    0 -> {
                        val shape = RoundedCornerShape(16.dp)
                        Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(16.dp)) {
                            Text("Current number", color = VibeColors.TextMute, fontSize = 13.sp)
                            Text("+91 98••••4321", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                        }
                        Text(
                            "Enter your new number. We will send a 6 digit code to it. Your account, coins and chats stay the same.", color = VibeColors.TextDim, fontSize = 14.sp,
                            lineHeight = 21.sp, modifier = Modifier.padding(top = 20.dp, bottom = 16.dp),
                        )
                        VibeField(phone, { phone = it.filter { c -> c.isDigit() }.take(10); error = "" }, "New mobile number", keyboard = KeyboardType.Phone, minHeight = 56.dp)
                        Text(error, color = VibeColors.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    1 -> {
                        Text("Enter the 6 digit code sent to +91 $phone", color = VibeColors.TextDim, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
                        BasicTextField(
                            value = otp, onValueChange = { otp = it.filter { c -> c.isDigit() }.take(6); error = "" },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), cursorBrush = SolidColor(Color.Transparent),
                            textStyle = TextStyle(color = Color.Transparent), modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            decorationBox = { inner ->
                                Box {
                                    Box(Modifier.size(1.dp)) { inner() }   // the real (invisible) field must be laid out to receive typing
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        repeat(6) { i ->
                                            val shape = RoundedCornerShape(14.dp)
                                            Box(
                                                Modifier.weight(1f).height(56.dp).clip(shape).background(VibeColors.Card)
                                                    .border(1.5.dp, if (i == otp.length) VibeColors.Pink else VibeColors.Line, shape),
                                                contentAlignment = Alignment.Center,
                                            ) { Text(otp.getOrNull(i)?.toString() ?: "", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
                                        }
                                    }
                                }
                            },
                        )
                        Text(error, color = VibeColors.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                        Text(
                            if (seconds > 0) "Resend code in ${seconds}s" else "Resend code", color = if (seconds > 0) VibeColors.TextMute else VibeColors.Pink, fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp).clickable(enabled = seconds == 0) { seconds = 30 },
                        )
                    }
                    else -> Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(96.dp).clip(CircleShape).border(2.dp, VibeColors.Green, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(MeIcons.Tick, null, tint = VibeColors.Green, modifier = Modifier.size(48.dp))
                        }
                        Text("Number updated", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
                        Text("You will now log in with +91 $phone", color = VibeColors.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            val enabled = when (step) { 0 -> phone.length == 10; 1 -> otp.length == 6; else -> true }
            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton(listOf("Send code", "Verify", "Done")[step], enabled = enabled, onClick = {
                    when (step) {
                        0 -> if (Regex("^[6-9]\\d{9}$").matches(phone)) { step = 1; seconds = 30; otp = "" } else error = "Enter a valid 10 digit mobile number"
                        1 -> if (otp == "123456") step = 2 else { error = "Wrong code. Demo code is 123456"; otp = "" }
                        else -> onDone()
                    }
                })
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
                        if (auto) "VIP ACTIVE" else "ENDS 15 JAN", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
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
                        Text("Auto renew", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(if (auto) "VIP renews automatically" else "Auto renew is off", color = VibeColors.TextMute, fontSize = 12.sp)
                    }
                    VibeSwitch(auto, { if (auto) askCancel = true else { auto = true; ctx.toast("Auto renew on") } })
                }
                Text("YOUR VIP BENEFITS", color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.6.sp, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    listOf("Cheaper voice and video calls", "Free chat with every host", "VIP badge and profile frame", "Full visitors list").forEach {
                        Row(Modifier.padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("✓", color = VibeColors.Gold); Text(it, color = Color(0xFFD1D5DB), fontSize = 13.sp)
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
                    "If you cancel, VIP stays until the end of the period you paid for. No refund for the time left.", color = Color(0xFF4B5563), fontSize = 11.sp,
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

private class GiftRow(val gift: String, val who: String, val time: String, val coins: Int)

private val RECEIVED = listOf(
    GiftRow("Rose x5", "Rahul", "Today, 2:10 pm", 90), GiftRow("Teddy", "Sam", "Today, 11:40 am", 120), GiftRow("Coffee", "Karan", "Yesterday", 30),
    GiftRow("Ring", "Arjun", "12 Oct", 300), GiftRow("Rose", "Vikram", "10 Oct", 18),
)
private val SENT = listOf(GiftRow("Rose", "Priya", "Today, 2:10 pm", 20), GiftRow("Coffee", "Ananya", "Yesterday", 50), GiftRow("Teddy", "Kabir", "11 Oct", 200))

/** Gifts received and sent. DEMO; gifts are created and priced in the admin panel and the history comes from the server. */
@Composable
fun GiftsHistoryScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf("recv") }
    val rows = if (tab == "recv") RECEIVED else SENT
    val total = rows.sumOf { it.coins }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Gold, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Gifts", onBack)
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("recv" to "Received", "sent" to "Sent").forEach { (k, l) ->
                    Column(Modifier.noRippleClick { tab = k }) {
                        Text(l.uppercase(), color = if (tab == k) Color.White else VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            Modifier.padding(top = 10.dp).size(width = 70.dp, height = 2.dp)
                                .then(if (tab == k) Modifier.background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))) else Modifier),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(Color(0x4D784C0A), VibeColors.Card, VibeColors.Card))).border(1.dp, VibeColors.Gold.copy(alpha = 0.3f), shape).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (tab == "recv") "YOU EARNED FROM GIFTS" else "YOU SPENT ON GIFTS", color = VibeColors.TextDim, fontSize = 10.sp, letterSpacing = 1.6.sp)
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoinDot(28.dp)
                            Text("%,d".format(total), color = VibeColors.Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("${rows.size} gifts", color = VibeColors.TextDim, fontSize = 12.sp)
                }
                rows.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1A1A26)).border(1.dp, Color(0xFF2A2A3A), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(CallIcons.Gift, null, tint = VibeColors.Gold, modifier = Modifier.size(24.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(r.gift, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text((if (tab == "recv") "From " else "To ") + "${r.who} · ${r.time}", color = VibeColors.TextMute, fontSize = 11.sp)
                        }
                        Text((if (tab == "recv") "+" else "−") + "${r.coins}", color = if (tab == "recv") VibeColors.Green else Color(0xFFD1D5DB), fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                }
            }
        }
    }
}
