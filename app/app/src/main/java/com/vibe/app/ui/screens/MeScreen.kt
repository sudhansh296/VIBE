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
    val ctx = LocalContext.current
    val floatY by rememberFloat()

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        GlowBlob(VibeColors.Cyan, 240.dp, Modifier.align(Alignment.CenterEnd).offset(100.dp, (-120).dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                Box {
                    CallIconButton(CallIcons.Bell, onNotifications, size = 40.dp)
                    Box(Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp).size(12.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.dp).clip(CircleShape).background(VibeColors.Pink))
                }
                Box(Modifier.width(8.dp))
                CallIconButton(MeIcons.Settings, onSettings, size = 40.dp)
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 150.dp)) {
                // profile
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpinBorder(Modifier.size(88.dp)) {
                        Box(
                            Modifier.size(80.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF1D1A28), Color(0xFF121019)))),
                            contentAlignment = Alignment.Center,
                        ) { Text(MeDemo.NAME.take(1), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light) }
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(MeDemo.NAME, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Box(
                                Modifier.clip(CircleShape).background(VibeColors.Gold.copy(alpha = 0.10f)).border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                            ) { Text("Lv.${MeDemo.LEVEL}", color = VibeColors.Gold, fontSize = 11.sp) }
                        }
                        Text("ID: ${MeDemo.USER_ID} · 26 · Delhi", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        Row(
                            Modifier.padding(top = 8.dp).height(32.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable(onClick = onEditProfile).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(MeIcons.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("Edit profile", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // following / followers / visitors
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("48", "Following", Modifier.weight(1f), onFollowing)
                    StatCard("12", "Followers", Modifier.weight(1f), onFollowers)
                    StatCard("7", "Visitors", Modifier.weight(1f), onVisitors)
                }

                // wallet
                val wShape = RoundedCornerShape(24.dp)
                Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(wShape)
                        .background(Brush.linearGradient(listOf(Color(0x59784C0A), VibeColors.Card, VibeColors.Card)))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.30f), wShape).clickable(onClick = onWallet).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CoinDot(48.dp)
                    Column(Modifier.weight(1f)) {
                        Text("COIN BALANCE", color = VibeColors.TextDim, fontSize = 10.sp, letterSpacing = 1.6.sp)
                        Text("%,d".format(Demo.BALANCE), color = VibeColors.Gold, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(Modifier.height(40.dp).clip(CircleShape).background(VibeColors.Gold).clickable(onClick = onWallet).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        Text("Recharge", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // become a host
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().neonBorder(16.dp).background(Color(0xFF14101A))
                        .clickable(onClick = onBecomeHost).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(painterResource(R.drawable.ghost_profile), null, Modifier.size(56.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Column(Modifier.weight(1f)) {
                        Text("Become a Host", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Open for everyone 18+ · Set your own rates and earn per minute", color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text("›", color = VibeColors.Pink, fontSize = 22.sp)
                }

                // menu
                val mShape = RoundedCornerShape(16.dp)
                Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(mShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, mShape)) {
                    MenuRow("VIP", "Cheaper calls · free chat", VibeColors.Gold, onVip, tile = {
                        IconTile(VibeColors.Gold) { Image(painterResource(R.drawable.ic_vip), null, Modifier.size(24.dp)) }
                    })
                    MenuDivider()
                    MenuRow("Daily check-in", "+20 free coins", VibeColors.Green, onCheckIn, tile = { TileIcon(MeIcons.Calendar, VibeColors.Green) })
                    MenuDivider()
                    MenuRow("My level", "Lv.${MeDemo.LEVEL} · ${MeDemo.XP * 100 / MeDemo.XP_NEXT}%", VibeColors.TextMute, onLevel, tile = { TileIcon(MeIcons.Star, VibeColors.Cyan) })
                    MenuDivider()
                    MenuRow("Invite friends", "Both get 100 coins", VibeColors.TextMute, onInvite, tile = { TileIcon(CallIcons.Follow, VibeColors.Pink) })
                    MenuDivider()
                    MenuRow("Help & support", "", VibeColors.TextMute, onHelp, tile = { TileIcon(MeIcons.Help, Color(0xFFD1D5DB)) })
                }
            }
        }

        VibeTabBar(
            selected = 2, unread = Demo.UNREAD,
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
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
        Text(title, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (hint.isNotEmpty()) Text(hint, color = hintColor, fontSize = 12.sp)
        Text("›", color = Color(0xFF4B5563), fontSize = 18.sp)
    }
}

@Composable
private fun MenuDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1C1C28)))
