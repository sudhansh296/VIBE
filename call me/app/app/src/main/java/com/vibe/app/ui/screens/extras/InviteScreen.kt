package com.vibe.app.ui.screens.extras

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.launch

/**
 * Invite friends: my code (made by the server), share it, stats, and enter a friend's code once (before the first purchase).
 * Both people get the reward coins (admin setting) only when the invited friend buys coins for the first time.
 */
@Composable
fun InviteScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val floatY by rememberFloat()
    var friend by remember { mutableStateOf("") }
    var myCode by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var reward by remember { mutableStateOf(100) }
    var invited by remember { mutableStateOf(0) }
    var rewarded by remember { mutableStateOf(0) }
    var canEnter by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    suspend fun load() {
        runCatching { com.vibe.app.data.api.Api.get("/invite") }.onSuccess { r ->
            myCode = r.optString("code"); link = r.optString("link"); reward = r.optInt("rewardCoins", 100); invited = r.optInt("invited"); rewarded = r.optInt("rewarded")
            canEnter = r.optBoolean("canEnterCode"); entered = r.optBoolean("enteredCode")
        }.onFailure { ctx.toast(it.message ?: "Could not load your invite code") }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { load() }
    // the link fills the code in by itself; the code is in the text too, for typing it by hand
    val message = "Join me on VIBE, the video chat app! Tap to join: $link\nOr enter my code $myCode when you sign up. After your first coin purchase we both get $reward free coins."

    fun copy(text: String, toast: String) {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("VIBE", text))
        ctx.toast(toast)
    }
    fun share(pkg: String? = null) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message); if (pkg != null) setPackage(pkg) }
        runCatching { ctx.startActivity(if (pkg == null) Intent.createChooser(send, "Invite friends") else send) }.onFailure { ctx.toast("App not installed") }
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Gold, 300.dp, Modifier.align(Alignment.TopCenter).offset(0.dp, (-120).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Invite friends", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ghost_empty), null, Modifier.size(112.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("You both get", color = VibeColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Light)
                        CoinDot(24.dp)
                        Text("$reward", color = VibeColors.Gold, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Free coins when your friend joins with your code and buys coins for the first time.", color = VibeColors.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }

                val shape = RoundedCornerShape(20.dp)
                Column(
                    Modifier.padding(top = 20.dp).fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(Color(0x33F2B84B), VibeColors.Card, VibeColors.Card)))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.4f), shape).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("YOUR INVITE CODE", color = VibeColors.TextDim, fontSize = 10.sp, letterSpacing = 1.6.sp)
                    Text(myCode.ifBlank { "······" }, color = VibeColors.Gold, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.height(40.dp).clip(CircleShape).background(Color.White).clickable(enabled = myCode.isNotBlank()) { copy(myCode, "Code $myCode copied") }.padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Copy code", color = VibeColors.Pink, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        Box(
                            Modifier.height(40.dp).clip(CircleShape).background(Color.White).clickable(enabled = link.isNotBlank()) { copy(link, "Invite link copied") }.padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Copy link", color = VibeColors.Pink, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }

                Row(Modifier.padding(top = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ShareBtn("WhatsApp", MeIcons.Chat, Color(0xFF25D366)) { share("com.whatsapp") }
                    ShareBtn("SMS", MeIcons.Mail, VibeColors.Cyan) { share() }
                    ShareBtn("Copy text", MeIcons.Link, VibeColors.Gold) { copy(message, "Invite text copied") }
                    ShareBtn("More", MeIcons.Share, VibeColors.Pink) { share() }
                }

                Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("$invited", "Joined", Modifier.weight(1f))
                    StatBox("$rewarded", "Bought coins", Modifier.weight(1f))
                    StatBox("%,d".format(rewarded * reward), "Earned", Modifier.weight(1f), VibeColors.Gold)
                }

                SectionLabel("How it works", Modifier.padding(top = 28.dp, bottom = 8.dp))
                Step("1", "Share your code", "Send it to friends on WhatsApp or SMS")
                Step("2", "They join with it", "They tap your link (the code goes in by itself) or type your code, before their first purchase (18+ only)")
                Step("3", "You both get coins", "Added when they buy coins for the first time")

                if (entered) Text(
                    "✓ You joined with a friend's code. You both get $reward coins on your first coin purchase.", color = VibeColors.Green, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Green.copy(alpha = 0.3f), shape).padding(16.dp),
                )
                if (!canEnter && !entered && myCode.isNotBlank()) Text(
                    "A friend's code can be added only before your first coin purchase (at sign-up or here).", color = VibeColors.TextMute, fontSize = 12.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (canEnter) Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Ink.copy(alpha = 0.10f), shape).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Have a friend's code?", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("You can add one only once", color = VibeColors.TextMute, fontSize = 12.sp)
                    }
                    Box(Modifier.size(width = 110.dp, height = 44.dp)) { VibeField(friend, { friend = it.uppercase().take(8) }, "CODE", caps = true, minHeight = 44.dp) }
                    Box(
                        Modifier.height(44.dp).alpha(if (friend.length >= 4) 1f else 0.45f).clip(RoundedCornerShape(12.dp)).background(VibeBrush.Button)
                            .clickable(enabled = friend.length >= 4 && !applying) {
                                applying = true
                                scope.launch {
                                    runCatching { com.vibe.app.data.api.Api.post("/invite/apply", org.json.JSONObject().put("code", friend)) }
                                        .onSuccess { ctx.toast("Code added · coins come with your first purchase"); friend = ""; load() }
                                        .onFailure { ctx.toast(it.message ?: "Could not use this code") }
                                    applying = false
                                }
                            }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Apply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
                Text(
                    "Reward amount and rules are set by VIBE. Fake or repeated accounts earn nothing.", color = Color(0xFFB093A2), fontSize = 11.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun ShareBtn(label: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(label, color = VibeColors.ChipText, fontSize = 11.sp)
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier, color: Color = VibeColors.Ink) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun Step(n: String, title: String, sub: String) {
    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(VibeColors.Pink.copy(alpha = 0.10f)).border(1.5.dp, VibeColors.Pink, CircleShape), contentAlignment = Alignment.Center) {
            Text(n, color = VibeColors.Pink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column {
            Text(title, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
