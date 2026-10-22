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

private const val MY_CODE = "RAHUL26"   // DEMO. The server creates each user's code and decides the reward (100 coins each).

/** Invite friends: code, share (real Android share sheet), stats, enter a friend's code. Coins are added only after the server verifies it. */
@Composable
fun InviteScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val floatY by rememberFloat()
    var friend by remember { mutableStateOf("") }
    val message = "Join me on VIBE! Use my code $MY_CODE and we both get 100 free coins. https://vibe.app/invite/$MY_CODE"

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
                        Text("You both get", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
                        CoinDot(24.dp)
                        Text("100", color = VibeColors.Gold, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Free coins when your friend joins VIBE with your code.", color = VibeColors.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }

                val shape = RoundedCornerShape(20.dp)
                Column(
                    Modifier.padding(top = 20.dp).fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(Color(0x4D784C0A), VibeColors.Card, VibeColors.Card)))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.4f), shape).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("YOUR INVITE CODE", color = VibeColors.TextDim, fontSize = 10.sp, letterSpacing = 1.6.sp)
                    Text(MY_CODE, color = VibeColors.Gold, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, modifier = Modifier.padding(top = 4.dp))
                    Box(
                        Modifier.padding(top = 12.dp).height(40.dp).clip(CircleShape).background(Color.White).clickable { copy(MY_CODE, "Code $MY_CODE copied") }.padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Copy code", color = VibeColors.Dark, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }

                Row(Modifier.padding(top = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ShareBtn("WhatsApp", MeIcons.Chat, Color(0xFF25D366)) { share("com.whatsapp") }
                    ShareBtn("SMS", MeIcons.Mail, VibeColors.Cyan) { share() }
                    ShareBtn("Copy link", MeIcons.Link, VibeColors.Gold) { copy(message, "Link copied") }
                    ShareBtn("More", MeIcons.Share, VibeColors.Pink) { share() }
                }

                Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("5", "Invited", Modifier.weight(1f))
                    StatBox("3", "Joined", Modifier.weight(1f))
                    StatBox("300", "Earned", Modifier.weight(1f), VibeColors.Gold)
                }

                SectionLabel("How it works", Modifier.padding(top = 28.dp, bottom = 8.dp))
                Step("1", "Share your code", "Send it to friends on WhatsApp or SMS")
                Step("2", "They join with it", "They enter the code when they sign up (18+ only)")
                Step("3", "You both get coins", "Added after their first verified login")

                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, Color.White.copy(alpha = 0.10f), shape).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Have a friend's code?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("You can add one only once", color = VibeColors.TextMute, fontSize = 12.sp)
                    }
                    Box(Modifier.size(width = 110.dp, height = 44.dp)) { VibeField(friend, { friend = it.uppercase().take(8) }, "CODE", caps = true, minHeight = 44.dp) }
                    Box(
                        Modifier.height(44.dp).alpha(if (friend.length >= 4) 1f else 0.45f).clip(RoundedCornerShape(12.dp)).background(VibeBrush.Button)
                            .clickable(enabled = friend.length >= 4) { ctx.toast("Code applied · 100 coins added"); friend = "" }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Apply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
                Text(
                    "Reward amount and rules are set by VIBE. Fake or repeated accounts earn nothing.", color = Color(0xFF4B5563), fontSize = 11.sp,
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
private fun StatBox(value: String, label: String, modifier: Modifier, color: Color = Color.White) {
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
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
