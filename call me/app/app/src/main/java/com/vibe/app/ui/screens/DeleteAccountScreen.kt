package com.vibe.app.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Session
import com.vibe.app.data.SettingsDemo
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

/** Delete account: warnings + type DELETE. DEMO; the server re-authenticates the user and deletes or anonymises the data. */
@Composable
fun DeleteAccountScreen(onBack: () -> Unit, onWithdraw: () -> Unit, onDeleted: () -> Unit) {
    var deleting by remember { mutableStateOf(false) }
    val delScope = androidx.compose.runtime.rememberCoroutineScope()
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    val me = com.vibe.app.data.api.AuthStore.me
    val unpaid = if (me?.optString("role") == "host") me.optInt("earnings", 0) else 0
    val ok = text.trim().uppercase() == "DELETE"

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Red, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Delete account", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                val rShape = RoundedCornerShape(16.dp)
                Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(rShape).background(VibeColors.Red.copy(alpha = 0.05f)).border(1.dp, VibeColors.Red.copy(alpha = 0.30f), rShape).padding(16.dp)) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = VibeColors.Red, fontWeight = FontWeight.Bold)) { append("This can't be undone. ") }
                            append("Deleting your account will permanently remove:")
                        },
                        color = Color(0xFF5A3F4D), fontSize = 14.sp, lineHeight = 20.sp,
                    )
                    listOf(
                        "your profile, photos and videos", "chats and call history", "coin balance and VIP (no refund)",
                        "for hosts: unpaid earnings are lost, so withdraw first",
                    ).forEach { Text("•  $it", color = Color(0xFF5A3F4D), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp)) }
                }

                if (unpaid > 0) {
                    val gShape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.padding(top = 16.dp).fillMaxWidth().clip(gShape).background(VibeColors.Gold.copy(alpha = 0.05f)).border(1.dp, VibeColors.Gold.copy(alpha = 0.30f), gShape).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(CallIcons.Coin, null, tint = VibeColors.Gold, modifier = Modifier.size(20.dp))
                        Column {
                            Text("You have ${"%,d".format(unpaid)} coins of unpaid earnings.", color = Color(0xFF5A3F4D), fontSize = 12.sp, lineHeight = 17.sp)
                            Text("Withdraw them first", color = VibeColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp).clickable(onClick = onWithdraw))
                        }
                    }
                }

                Text(
                    buildAnnotatedString {
                        append("Type ")
                        withStyle(SpanStyle(color = VibeColors.Ink, fontWeight = FontWeight.Bold)) { append("DELETE") }
                        append(" to confirm")
                    },
                    color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                VibeField(text, { text = it }, "DELETE", caps = true)
                Spacer(Modifier.height(24.dp))
            }
            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(56.dp).alpha(if (ok) 1f else 0.4f).clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(VibeColors.Red, Color(0xFFB3122F)))).clickable(enabled = ok && !deleting) {
                            deleting = true
                            delScope.launch {
                                try {
                                    com.vibe.app.data.api.Api.delete("/me")
                                    ctx.toast("Account deleted")
                                    com.vibe.app.data.api.AuthStore.clear()
                                    com.vibe.app.ui.components.MyProfile.dp = null
                                    Session.logout(ctx)
                                    onDeleted()
                                } catch (e: com.vibe.app.data.api.ApiException) {
                                    ctx.toast(e.message)
                                }
                                deleting = false
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) { Text("Delete my account forever", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
        }
    }
}
