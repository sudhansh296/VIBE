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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.vibe.app.R
import com.vibe.app.data.Demo
import com.vibe.app.data.MeDemo
import com.vibe.app.ui.components.CallIconButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.IconTile
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.SpinBorder
import com.vibe.app.ui.components.TileIcon
import com.vibe.app.ui.components.VibeTabBar
import com.vibe.app.ui.components.neonBorder
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import androidx.compose.ui.graphics.vector.ImageVector

/** Me tab: profile, coin balance, host call-to-action and the menu. DEMO values; the server provides the real ones. */
@Composable
fun MeScreen(
    onHome: () -> Unit,
    onChats: () -> Unit,
    onNotifications: () -> Unit,
    onWallet: () -> Unit,
    onVip: () -> Unit,
    onCheckIn: () -> Unit,
    onLevel: () -> Unit,
    onBecomeHost: () -> Unit,
    onSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onVisitors: () -> Unit,
    onHelp: () -> Unit,
    onFollowing: () -> Unit,
    onFollowers: () -> Unit,
    onInvite: () -> Unit,
) {
    val me = com.vibe.app.data.api.AuthStore.me
    val myName = me?.optString("name")?.takeIf { it.isNotBlank() && it != "null" } ?: "You"
    val myId = me?.optLong("id")?.takeIf { it > 0 }?.toString() ?: "-"
    // following / followers / visitors from the server
    var counts by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<org.json.JSONObject?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { com.vibe.app.data.api.Api.get("/me/counts") }.onSuccess { counts = it } }
    // fresh copy from the server each time Me opens (name, coins ...)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { com.vibe.app.data.api.Api.get("/me") }.onSuccess { com.vibe.app.data.api.AuthStore.saveUser(it.getJSONObject("user")) }
    }
    val ctx = LocalContext.current
    val floatY by rememberFloat()
    // an approved host earns instead of spending: earnings + dashboard here, no coin wallet / VIP / check-in / "Become a Host"
    val isHost = me?.optString("role") == "host"
    val meVip = !isHost && me?.optBoolean("vip") == true
    var hostEarnings by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    androidx.compose.runtime.LaunchedEffect(isHost) { if (isHost) runCatching { com.vibe.app.data.WalletRepo.earnings() }.onSuccess { hostEarnings = it } }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        GlowBlob(VibeColors.Cyan, 240.dp, Modifier.align(Alignment.CenterEnd).offset(100.dp, (-120).dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                Box {
                    CallIconButton(CallIcons.Bell, onNotifications, size = 40.dp)
                    if (com.vibe.app.data.NotifRepo.unread > 0) Box(Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp).size(12.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.dp).clip(CircleShape).background(VibeColors.Pink))
                }
                Box(Modifier.width(8.dp))
                CallIconButton(MeIcons.Settings, onSettings, size = 40.dp)
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 150.dp)) {
                // profile
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpinBorder(Modifier.size(88.dp)) {
                        Box(
                            Modifier.size(80.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFD9E6), Color(0xFFFFC2D8)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            com.vibe.app.ui.components.MyDp(Modifier.fillMaxSize(), myName.take(1), 32.sp)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(myName, color = VibeColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Box(
                                Modifier.clip(CircleShape).background(VibeColors.Gold.copy(alpha = 0.10f)).border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                            ) { Text("Lv.${me?.optInt("level", 1) ?: 1}", color = VibeColors.Gold, fontSize = 11.sp) }
                            if (meVip) Text("VIP", color = Color(0xFF7A4B00), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(CircleShape).background(Color(0xFFFFE9A8)).padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Text(listOfNotNull("ID: $myId", me?.optInt("age", -1)?.takeIf { it > 0 }?.toString(), me?.optString("city")?.takeIf { it.isNotBlank() && it != "null" }).joinToString(" · "), color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        Row(
                            Modifier.padding(top = 8.dp).height(32.dp).clip(CircleShape).border(1.dp, VibeColors.Ink.copy(alpha = 0.15f), CircleShape)
                                .clickable(onClick = onEditProfile).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(MeIcons.Edit, null, tint = VibeColors.Ink, modifier = Modifier.size(14.dp))
                            Text("Edit profile", color = VibeColors.Ink, fontSize = 11.sp)
                        }
                    }
                }

                // following / followers / visitors
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fun n(k: String) = counts?.optInt(k)?.toString() ?: "–"
                    StatCard(n("following"), "Following", Modifier.weight(1f), onFollowing)
                    StatCard(n("followers"), "Followers", Modifier.weight(1f), onFollowers)
                    StatCard(n("visitors"), "Visitors", Modifier.weight(1f), onVisitors)
                }

                // wallet (users) / earnings (hosts: the button opens the host dashboard)
                val wShape = RoundedCornerShape(24.dp)
                if (isHost) Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(wShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF1FA463), Color(0xFF3CCB8A)))).clickable(onClick = onBecomeHost).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CoinDot(48.dp)
                    Column(Modifier.weight(1f)) {
                        Text("MY EARNINGS", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, letterSpacing = 1.6.sp)
                        Text(hostEarnings?.let { "%,d".format(it) } ?: "–", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(Modifier.height(40.dp).clip(CircleShape).background(Color.White).clickable(onClick = onBecomeHost).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        Text("Dashboard", color = Color(0xFF1FA463), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(wShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF4D8D), Color(0xFFFF7AB0)))).clickable(onClick = onWallet).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CoinDot(48.dp)
                    Column(Modifier.weight(1f)) {
                        Text("COIN BALANCE", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, letterSpacing = 1.6.sp)
                        Text("%,d".format(Demo.BALANCE), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(Modifier.height(40.dp).clip(CircleShape).background(Color.White).clickable(onClick = onWallet).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        Text("Recharge", color = VibeColors.Pink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // become a host (users only)
                if (!isHost) Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp))
                        .clickable(onClick = onBecomeHost).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(painterResource(R.drawable.ghost_profile), null, Modifier.size(56.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Column(Modifier.weight(1f)) {
                        Text("Become a Host", color = VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Open for everyone 18+ · Set your own rates and earn per minute", color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text("›", color = VibeColors.Pink, fontSize = 22.sp)
                }

                // menu
                val mShape = RoundedCornerShape(16.dp)
                Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(mShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, mShape)) {
                    if (!isHost) {   // coin perks are for users
                        MenuRow("VIP", if (meVip) "Active · until ${vipDate(me?.optString("vipUntil").orEmpty())}" else "Cheaper calls · free chat", VibeColors.Gold, onVip, tile = {
                            IconTile(VibeColors.Gold) { Image(painterResource(R.drawable.ic_vip), null, Modifier.size(24.dp)) }
                        })
                        MenuDivider()
                        MenuRow("Daily check-in", "Free coins every day", VibeColors.Green, onCheckIn, tile = { TileIcon(MeIcons.Calendar, VibeColors.Green) })
                        MenuDivider()
                    }
                    MenuRow("My level", "Lv.${me?.optInt("level", 1) ?: 1} · ${"%,d".format(me?.optInt("xp", 0) ?: 0)} XP", VibeColors.TextMute, onLevel, tile = { TileIcon(MeIcons.Star, VibeColors.Cyan) })
                    MenuDivider()
                    MenuRow("Invite friends", "Both get 100 coins", VibeColors.TextMute, onInvite, tile = { TileIcon(CallIcons.Follow, VibeColors.Pink) })
                    MenuDivider()
                    MenuRow("Help & support", "", VibeColors.TextMute, onHelp, tile = { TileIcon(MeIcons.Help, Color(0xFF5A3F4D)) })
                }
            }
        }

        VibeTabBar(
            selected = 2, unread = com.vibe.app.data.ChatRepo.unread,
            onSelect = { if (it == 0) onHome() else if (it == 1) onChats() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun MenuRow(title: String, hint: String, hintColor: Color, onClick: () -> Unit, tile: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        tile()
        Text(title, color = VibeColors.Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (hint.isNotEmpty()) Text(hint, color = hintColor, fontSize = 12.sp)
        Text("›", color = Color(0xFFB093A2), fontSize = 18.sp)
    }
}

@Composable
private fun MenuDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFFFE6EE)))
