package com.vibe.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.Demo
import com.vibe.app.data.HostStatus
import com.vibe.app.data.MediaCfg
import com.vibe.app.data.SafetyStore
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.SpinBorder
import com.vibe.app.ui.components.StatusPill
import com.vibe.app.ui.components.VerifiedTick
import com.vibe.app.ui.components.cardSurface
import com.vibe.app.ui.components.genderColor
import com.vibe.app.ui.components.genderMark
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HostProfileScreen(
    hostId: Int, onBack: () -> Unit, onCall: (id: Int, audio: Boolean) -> Unit, onChat: (Int) -> Unit,
    onWallet: () -> Unit, onReport: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val h = remember(hostId) { Demo.host(hostId) }
    val photos = remember(h) { listOf(h.c1 to h.c2, h.c2 to h.c1, Color(0xFF1E0F17) to h.c1, h.c1 to VibeColors.Dark, h.c2 to VibeColors.Dark, Color(0xFF2A1038) to h.c2) }
    val pager = rememberPagerState { photos.size }
    var following by remember { mutableStateOf(false) }
    var showGifts by remember { mutableStateOf(false) }
    var showSafety by remember { mutableStateOf(false) }
    var showBlock by remember { mutableStateOf(false) }
    // paid locked media: the first MediaCfg.FREE_PHOTOS photos and the intro video are free; the rest are unlocked with coins.
    // DEMO: the server charges the user, credits the host and only then sends the real file.
    val unlocked = remember(hostId) { mutableStateListOf<String>() }
    var balance by remember { mutableStateOf(Demo.BALANCE) }
    var unlockTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }   // "photo" or "video" + index
    var player by remember { mutableStateOf<String?>(null) }                      // "intro" or "video2"
    fun photoLocked(i: Int) = i >= MediaCfg.FREE_PHOTOS && "p$i" !in unlocked

    fun call(audio: Boolean = false) = when {
        audio && !h.voice -> ctx.toast("${h.name} takes video calls only")
        !audio && !h.video -> ctx.toast("${h.name} takes voice calls only")
        h.status == HostStatus.BUSY -> ctx.toast("${h.name} is on another call")
        h.status == HostStatus.OFFLINE -> ctx.toast("We'll notify you when ${h.name} is online")
        else -> onCall(h.id, audio)
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
            // photo slider
            Box(Modifier.fillMaxWidth().height(440.dp)) {
                HorizontalPager(pager, Modifier.matchParentSize()) { i ->
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(photos[i].first, photos[i].second))), contentAlignment = Alignment.Center) {
                        if (photoLocked(i)) {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
                            Column(Modifier.clickable { unlockTarget = "photo" to i }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                                Row(Modifier.height(40.dp).clip(CircleShape).background(VibeBrush.Button).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Unlock · ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    CoinDot(16.dp)
                                    Text(" ${MediaCfg.PRICE_PHOTO}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else Text(if (i == 0) h.name.take(1) else "📷", color = Color.White.copy(alpha = 0.8f), fontSize = 150.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.4f), 0.4f to Color.Transparent, 1f to VibeColors.Dark)))

                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
                    GlassButton("←", onBack)
                    Spacer(Modifier.weight(1f))
                    GlassButton("↗") { ctx.toast("Profile link copied") }
                    Spacer(Modifier.width(8.dp))
                    GlassButton("⋯") { showSafety = true }
                }
                Box(
                    Modifier.statusBarsPadding().padding(start = 16.dp, top = 72.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
                        .clickable { player = "intro" }.padding(horizontal = 12.dp, vertical = 6.dp),
                ) { Text("▶ Intro video · 0:18", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                Row(
                    Modifier.statusBarsPadding().padding(start = 16.dp, top = 108.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
                        .clickable { if ("v2" in unlocked) player = "video2" else unlockTarget = "video" to 0 }.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if ("v2" in unlocked) Text("▶ Video 2 · 0:24", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    else {
                        androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.Lock, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(" Video 2 · ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        CoinDot(14.dp)
                        Text(" ${MediaCfg.PRICE_VIDEO}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Row(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(photos.size) { i ->
                        val w by animateDpAsState(if (pager.currentPage == i) 20.dp else 6.dp, label = "dot")
                        Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(Color.White.copy(alpha = if (pager.currentPage == i) 1f else 0.4f)))
                    }
                }
                StatusPill(h.status, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 92.dp))
                Row(Modifier.align(Alignment.BottomStart).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.forEachIndexed { i, (a, b) ->
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(a, b)))
                                .border(2.dp, if (pager.currentPage == i) VibeColors.Pink else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { scope.launch { pager.animateScrollToPage(i) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (photoLocked(i)) {
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.Lock, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(h.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(8.dp))
                            if (h.verified) VerifiedTick()
                        }
                        Text("ID: ${40362380 + h.id}  ⧉", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.clickable { ctx.toast("ID copied") })
                    }
                    Box(
                        Modifier.height(40.dp).clip(CircleShape)
                            .background(if (following) Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card)) else VibeBrush.Button)
                            .border(1.dp, if (following) VibeColors.Line else Color.Transparent, CircleShape)
                            .clickable { following = !following }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (following) "✓ Following" else "＋ Follow", color = if (following) VibeColors.ChipText else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val gc = genderColor(h.gender)
                    Tag("${genderMark(h.gender)} ${h.age}", gc, gc.copy(alpha = 0.13f), gc.copy(alpha = 0.4f))
                    Tag("📍 ${h.city}", Color(0xFFD1D5DB), VibeColors.Card, VibeColors.Line)
                    Tag("★ Lv.${h.level}", VibeColors.Gold, VibeColors.Gold.copy(alpha = 0.15f), VibeColors.Gold.copy(alpha = 0.4f))
                    Tag("⭐ ${h.rating} (${"%,d".format(h.ratingCount)})", VibeColors.Cyan, VibeColors.Cyan.copy(alpha = 0.1f), VibeColors.Cyan.copy(alpha = 0.4f))
                    if (h.vip) Tag("VIP", Color.Black, VibeColors.Gold, VibeColors.Gold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat(h.followers, "Followers", Modifier.weight(1f))
                    Stat("%,d".format(h.calls), "Calls", Modifier.weight(1f))
                    Stat("${h.answerRate}%", "Answer rate", Modifier.weight(1f))
                }

                Row(Modifier.fillMaxWidth().cardSurface().padding(16.dp)) {
                    Text("“", color = VibeColors.Gold, fontSize = 30.sp, modifier = Modifier.padding(end = 10.dp))
                    Text(h.bio, color = Color(0xFFD1D5DB), fontSize = 14.sp, lineHeight = 21.sp)
                }

                Block("Speaks") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        h.langs.forEach { Text(it, color = Color.White, fontSize = 14.sp, modifier = Modifier.cardSurface(12.dp).padding(horizontal = 12.dp, vertical = 6.dp)) }
                    }
                }
                Block("What people say") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("😊 Friendly · 842", "🎵 Great vibe · 610", "😂 Funny · 355", "🧠 Smart · 201").forEach {
                            Text(it, color = Color.White, fontSize = 12.sp, modifier = Modifier.cardSurface(99.dp).padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
                Block("Gifts received", "128 total") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("🌹" to 64, "☕" to 31, "🧸" to 18, "💍" to 9, "🏎️" to 6).forEach { (e, n) ->
                            Column(Modifier.width(60.dp).cardSurface().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(e, fontSize = 24.sp); Text("×$n", color = VibeColors.TextDim, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // dark fade behind the status bar so scrolled content stays readable
        Box(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(VibeColors.Dark, VibeColors.Dark.copy(alpha = 0f))))
                .statusBarsPadding().height(12.dp),
        )

        // action bar
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, VibeColors.Dark.copy(alpha = 0.95f), VibeColors.Dark)))
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(56.dp).cardSurface().clickable { onChat(h.id) }, contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ghost_chat), "Chat", Modifier.size(44.dp))
            }
            Box(Modifier.size(56.dp).cardSurface().clickable { showGifts = true }, contentAlignment = Alignment.Center) { Text("🎁", fontSize = 24.sp) }
            // small voice button only when the host takes BOTH; a voice-only host gets the big button as "Voice Call"
            if (h.voice && h.video) Column(
                Modifier.size(56.dp).cardSurface().clickable { call(audio = true) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.Phone, "Voice call",
                    tint = if (h.status.canCall) VibeColors.Green else VibeColors.TextMute, modifier = Modifier.size(22.dp))
                Text("${h.audioRate}/min", color = VibeColors.Gold, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
            val mainAudio = !h.video
            val main = when {
                h.status == HostStatus.BUSY -> CallState.BUSY
                h.status == HostStatus.OFFLINE -> CallState.OFFLINE
                mainAudio -> CallState.VOICE
                else -> CallState.VIDEO
            }
            if (h.status.canCall) SpinBorder(Modifier.weight(1f).height(56.dp).clickable { call(audio = mainAudio) }, RoundedCornerShape(16.dp)) {
                CallLabel(main, if (mainAudio) h.audioRate else h.rate)
            }
            else Box(Modifier.weight(1f).height(56.dp).cardSurface().clickable { call(audio = mainAudio) }, contentAlignment = Alignment.Center) {
                CallLabel(main, h.rate)
            }
        }
    }

    if (showBlock) com.vibe.app.ui.components.BlockConfirmDialog(
        name = h.name,
        onConfirm = { SafetyStore.block(h.id); showBlock = false; ctx.toast("${h.name} blocked"); onBack() },
        onDismiss = { showBlock = false },
    )

    unlockTarget?.let { (kind, idx) ->
        val price = if (kind == "photo") MediaCfg.PRICE_PHOTO else MediaCfg.PRICE_VIDEO
        val enough = balance >= price
        ModalBottomSheet(onDismissRequest = { unlockTarget = null }, containerColor = VibeColors.Card) {
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (kind == "photo") "Unlock this photo" else "Unlock this video", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoinDot(32.dp)
                    Text("$price", color = VibeColors.Gold, fontSize = 40.sp, fontWeight = FontWeight.Black)
                }
                Text("Your balance: ${"%,d".format(balance)} coins", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                Text(
                    if (enough) "Once unlocked, you can see it any time. ${h.name} earns from every unlock." else "You need ${price - balance} more coins.",
                    color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                )
                NeonButton(if (enough) "Unlock for $price coins" else "Not enough coins · Top up", modifier = Modifier.padding(top = 20.dp), onClick = {
                    unlockTarget = null
                    if (!enough) onWallet()
                    else {
                        balance -= price
                        unlocked.add(if (kind == "photo") "p$idx" else "v2")
                        ctx.toast("Unlocked · $price coins used")
                        if (kind == "video") player = "video2"
                    }
                })
            }
        }
    }

    player?.let { p ->
        MediaPlayerOverlay(
            title = "${h.name} · ${if (p == "intro") "Intro video" else "Video 2"}",
            seconds = if (p == "intro") 18 else 24, colors = if (p == "intro") photos[1] else photos[3], onClose = { player = null },
        )
    }

    if (showGifts) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var pick by remember { mutableIntStateOf(0) }
        ModalBottomSheet(onDismissRequest = { showGifts = false }, sheetState = sheet, containerColor = VibeColors.Card) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Send a gift", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    CoinDot(18.dp); Spacer(Modifier.width(6.dp))
                    Text("%,d".format(Demo.BALANCE), color = VibeColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                LazyVerticalGrid(GridCells.Fixed(4), Modifier.padding(top = 16.dp).height(220.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(Demo.gifts) { i, g ->
                        val on = i == pick
                        Column(
                            Modifier.aspectRatio(0.82f).clip(RoundedCornerShape(16.dp))
                                .background(if (on) Brush.verticalGradient(listOf(VibeColors.Pink.copy(alpha = 0.2f), VibeColors.Dark)) else Brush.linearGradient(listOf(VibeColors.Dark, VibeColors.Dark)))
                                .border(1.5.dp, if (on) VibeColors.Pink else VibeColors.Line, RoundedCornerShape(16.dp)).clickable { pick = i },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Text(g.emoji, fontSize = 28.sp)
                            Text(g.name, color = Color(0xFFD1D5DB), fontSize = 11.sp)
                            Text("${g.price}", color = VibeColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                val g = Demo.gifts[pick]
                NeonButton("Send ${g.name} · ${g.price} coins", modifier = Modifier.padding(top = 16.dp), onClick = {
                    showGifts = false
                    ctx.toast(if (g.price > Demo.BALANCE) "Not enough coins · recharge in Wallet" else "${g.emoji} ${g.name} sent to ${h.name}!")
                })
            }
        }
    }

    if (showSafety) {
        ModalBottomSheet(onDismissRequest = { showSafety = false }, containerColor = VibeColors.Card) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetRow("🚩  Report", Color.White) { showSafety = false; onReport(h.id) }
                SheetRow("⛔  Block", VibeColors.Red) { showSafety = false; showBlock = true }
                SheetRow("Cancel", VibeColors.ChipText) { showSafety = false }
            }
        }
    }
}

@Composable
private fun GlassButton(icon: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.4f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(icon, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Tag(text: String, color: Color, bg: Color, border: Color) {
    Text(text, color = color, fontSize = 13.sp, modifier = Modifier.clip(CircleShape).background(bg).border(1.dp, border, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp))
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.cardSurface().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = VibeColors.TextMute, fontSize = 11.sp)
    }
}

@Composable
private fun Block(title: String, trailing: String? = null, content: @Composable () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(title, color = Color(0xFFD1D5DB), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (trailing != null) Text(trailing, color = VibeColors.TextMute, fontSize = 12.sp)
        }
        content()
    }
}

private enum class CallState { VIDEO, VOICE, OFFLINE, BUSY }

/** Main call button content: icon + one-line title + one-line subtitle, matching the host's state. */
@Composable
private fun CallLabel(state: CallState, rate: Int) {
    val orange = Color(0xFFFB923C)
    val (icon, title, sub) = when (state) {
        CallState.VIDEO -> Triple(com.vibe.app.ui.components.VibeIcons.Video, "Video Call", "$rate coins/min")
        CallState.OFFLINE -> Triple(com.vibe.app.ui.components.CallIcons.Bell, "Notify me", "Offline now")
        CallState.BUSY -> Triple(com.vibe.app.ui.components.CallIcons.Phone, "On a call", "Try again soon")
        CallState.VOICE -> Triple(com.vibe.app.ui.components.CallIcons.Phone, "Voice Call", "$rate coins/min")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(
                when (state) {
                    CallState.VIDEO -> VibeBrush.Button
                    CallState.BUSY -> SolidColor(orange.copy(alpha = 0.18f))
                    CallState.VOICE -> SolidColor(VibeColors.Green)
                    CallState.OFFLINE -> SolidColor(VibeColors.Line)
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                icon, null, modifier = Modifier.size(19.dp),
                tint = when (state) {
                    CallState.VIDEO -> Color.White
                    CallState.BUSY -> orange
                    CallState.VOICE -> VibeColors.Dark
                    CallState.OFFLINE -> VibeColors.ChipText
                },
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val priced = state == CallState.VIDEO || state == CallState.VOICE
                if (priced) { CoinDot(11.dp); Spacer(Modifier.width(4.dp)) }
                Text(sub, color = if (priced) VibeColors.Gold else VibeColors.TextDim, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SheetRow(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(VibeColors.Dark)
            .border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, color = color, fontSize = 15.sp) }
}

/**
 * Full-screen video player stand-in (play / pause, progress, time). DEMO: a gradient plays for [seconds]; the real app streams the host's
 * intro video, or the extra video after it has been unlocked.
 */
@Composable
private fun MediaPlayerOverlay(title: String, seconds: Int, colors: Pair<Color, Color>, onClose: () -> Unit) {
    var t by remember { mutableStateOf(0f) }
    var playing by remember { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(playing) {
        var last = androidx.compose.runtime.withFrameNanos { it }
        while (playing) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            t += (now - last) / 1_000_000_000f
            last = now
            if (t >= seconds) { t = 0f; playing = false }
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(colors.first, colors.second))).clickable { playing = !playing }) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
        Row(Modifier.statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { androidx.compose.material3.Icon(com.vibe.app.ui.components.MeIcons.Close, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
        if (!playing) Text("▶", color = Color.White.copy(alpha = 0.85f), fontSize = 64.sp, modifier = Modifier.align(Alignment.Center))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp)) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth((t / seconds).coerceIn(0f, 1f)).background(Color.White))
            }
            Text("0:%02d / 0:%02d".format(t.toInt(), seconds), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
    }
}
