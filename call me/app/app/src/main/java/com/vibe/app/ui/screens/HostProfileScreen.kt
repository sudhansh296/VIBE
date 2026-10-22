package com.vibe.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
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
import com.vibe.app.data.HostRepo
import com.vibe.app.data.ServerMedia
import com.vibe.app.data.Realtime
import com.vibe.app.data.api.ApiException
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
    hostId: Int, onBack: () -> Unit, onCall: (id: Int) -> Unit, onChat: (Int) -> Unit,
    onWallet: () -> Unit, onReport: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val h = HostRepo.cache[hostId] ?: Demo.host(hostId)
    val real = hostId > 8                       // a person from the server; ids 1..8 are the built-in test hosts
    val photos = remember(h) { listOf(h.c1 to h.c2, h.c2 to h.c1, Color(0xFFFFB3CF) to h.c1, h.c1 to VibeColors.Dark, h.c2 to VibeColors.Dark, Color(0xFFFF8FB8) to h.c2) }
    // server profile: photos first, then videos, as the server lists them (locked ones carry the price, never the file)
    var items by remember(hostId) { mutableStateOf<List<ServerMedia>>(emptyList()) }
    var loaded by remember(hostId) { mutableStateOf(!real) }
    var loadError by remember(hostId) { mutableStateOf<String?>(null) }
    var stats by remember(hostId) { mutableStateOf<com.vibe.app.data.ProfileStats?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val opened = remember(hostId) { mutableStateListOf<Int>() }     // media ids paid for in this visit
    androidx.compose.runtime.LaunchedEffect(hostId, reload) {
        if (real) try { val p = HostRepo.profile(hostId); items = p.media; stats = p.stats; loaded = true; loadError = null } catch (e: ApiException) { loadError = e.message }
    }
    // the server counts "stayed on this profile" and tells the host / starts the call by itself
    // a host viewing a user: the call is at the host's own rate (the user pays it)
    val hostViewer = com.vibe.app.data.api.AuthStore.me?.optString("role") == "host"
    var myRate by remember { mutableStateOf(0) }
    var vipOff by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(hostViewer) {
        if (hostViewer) {
            runCatching { com.vibe.app.data.api.Api.get("/host/rates").optInt("callRate") }.onSuccess { myRate = it }
            runCatching { com.vibe.app.data.api.Api.get("/config", auth = false).getJSONObject("config").optInt("vip_call_discount_pct") }.onSuccess { vipOff = it }
        }
    }
    // a VIP user pays less, also when the host calls (rounded the same way the server charges it)
    val callRate = if (!hostViewer) h.rate else if (h.vip && myRate > 0) maxOf(1, Math.round(myRate * (100 - vipOff) / 100.0).toInt()) else myRate
    androidx.compose.runtime.DisposableEffect(hostId) {
        if (real) Realtime.profileOpened(hostId)
        onDispose { if (real) Realtime.profileClosed() }
    }
    val total = if (real) maxOf(items.size, 1) else photos.size + 2   // photos first, then the host's videos, all in one left/right slider
    val pager = rememberPagerState { total }
    var following by remember(h.following, hostId) { mutableStateOf(h.following) }
    // a host looking at a user: "Favourite" instead of Follow (kept privately; no follower count on a user's profile)
    val favourite = real && !h.verified
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
    /** What slide [i] is: for server people from the server's list, for the test hosts from the built-in demo. */
    fun slide(i: Int): Slide = if (real) {
        val m = items.getOrNull(i)
        if (m == null) Slide(false, false, 0, 0, null) else Slide(m.video, m.locked && m.id !in opened, m.price, m.durationSec, m)
    } else {
        val vi = i - photos.size
        if (vi >= 0) Slide(true, vi == 1 && "v2" !in unlocked, MediaCfg.PRICE_VIDEO, if (vi == 0) 18 else 24, null)
        else Slide(false, photoLocked(i), MediaCfg.PRICE_PHOTO, 0, null)
    }

    fun call() = when {
        h.status == HostStatus.BUSY -> ctx.toast("${h.name} is on another call")
        h.status == HostStatus.OFFLINE -> ctx.toast(if (hostViewer) "${h.name} is offline" else "We'll notify you when ${h.name} is online")
        else -> onCall(h.id)
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
            // photo slider
            Box(Modifier.fillMaxWidth().height(440.dp)) {
                HorizontalPager(pager, Modifier.matchParentSize()) { i ->
                    val sl = slide(i)
                    val vi = i - photos.size   // test hosts: >= 0 means a video slide
                    val colors = if (!real && vi >= 0) (if (vi == 0) photos[1] else photos[3]) else if (real) (h.c1 to h.c2) else photos[i]
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(colors.first, colors.second))), contentAlignment = Alignment.Center) {
                        if (real) {
                            // the file only arrives when the server lets it through (free, unlocked or paid)
                            if (sl.item != null && !sl.locked) com.vibe.app.ui.components.ServerImage(sl.item.path, Modifier.fillMaxSize(), -0.3f)
                            else if (sl.item == null) com.vibe.app.ui.components.HostPhoto(h, Modifier.fillMaxSize(), 0, -0.3f)
                        } else com.vibe.app.ui.components.HostPhoto(h, Modifier.fillMaxSize(), if (vi >= 0) 1 + vi else i, -0.3f)
                        if (sl.video) {
                            if (sl.locked) {
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                                Column(Modifier.clickable { unlockTarget = "video" to i }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                        androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                    Row(Modifier.height(40.dp).clip(CircleShape).background(VibeBrush.Button).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Unlock video · ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        CoinDot(16.dp)
                                        Text(" ${sl.price}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            } else if (real && sl.item != null) {
                                InlineVideo(sl.item, active = pager.currentPage == i, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(
                                    Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).clickable { player = if (vi == 0) "intro" else "video2" },
                                    contentAlignment = Alignment.Center,
                                ) { Text("▶", color = VibeColors.Pink, fontSize = 28.sp) }
                                Text("0:%02d".format(sl.dur), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center).offset(y = 58.dp))
                            }
                        } else if (sl.locked) {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
                            Column(Modifier.clickable { unlockTarget = "photo" to i }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).border(1.dp, VibeColors.Ink.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                                Row(Modifier.height(40.dp).clip(CircleShape).background(VibeBrush.Button).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Unlock · ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    CoinDot(16.dp)
                                    Text(" ${sl.price}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.4f), 0.4f to Color.Transparent, 1f to VibeColors.Dark)))

                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
                    GlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                    Spacer(Modifier.weight(1f))
                    GlassButton(com.vibe.app.ui.components.MeIcons.Share, "Share") { ctx.toast("Profile link copied") }
                    Spacer(Modifier.width(8.dp))
                    GlassButton(null, "More") { showSafety = true }
                }
                // dots + thumbnails only when there is more than one photo / video (a user has just the profile photo)
                if (total > 1) Row(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(total) { i ->
                        val w by animateDpAsState(if (pager.currentPage == i) 20.dp else 6.dp, label = "dot")
                        Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(Color.White.copy(alpha = if (pager.currentPage == i) 1f else 0.4f)))
                    }
                }
                StatusPill(h.status, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 92.dp))
                if (total > 1) Row(Modifier.align(Alignment.BottomStart).horizontalScroll(rememberScrollState()).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until total).forEach { i ->
                        val sl = slide(i)
                        val vi = i - photos.size
                        val (a, b) = if (!real && vi >= 0) (if (vi == 0) photos[1] else photos[3]) else if (real) (h.c1 to h.c2) else photos[i]
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(a, b)))
                                .border(2.dp, if (pager.currentPage == i) VibeColors.Pink else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { scope.launch { pager.animateScrollToPage(i) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (real) { if (sl.item != null && !sl.locked) com.vibe.app.ui.components.ServerImage(sl.item.path, Modifier.fillMaxSize(), -0.5f) }
                            else com.vibe.app.ui.components.HostPhoto(h, Modifier.fillMaxSize(), if (vi >= 0) 1 + vi else i, -0.5f)
                            if (sl.locked) {
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.Lock, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            } else if (sl.video) Text("▶", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (loadError != null) Row(Modifier.fillMaxWidth().cardSurface().clickable { reload++ }.padding(14.dp)) {
                    Text("${loadError}  ·  Tap to retry", color = VibeColors.Red, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(h.name, color = VibeColors.Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(8.dp))
                            if (h.verified) VerifiedTick()
                        }
                        Text("ID: ${if (real) h.id else 40362380 + h.id}  ⧉", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.clickable { ctx.toast("ID copied") })
                    }
                    Box(
                        Modifier.height(40.dp).clip(CircleShape)
                            .background(if (following) Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card)) else VibeBrush.Button)
                            .border(1.dp, if (following) VibeColors.Line else Color.Transparent, CircleShape)
                            .clickable {
                                val now = !following; following = now
                                if (favourite) ctx.toast(if (now) "Added to your favourites" else "Removed from your favourites")
                                if (real) scope.launch { try { HostRepo.follow(h.id, now) } catch (e: ApiException) { following = !now; ctx.toast(e.message) } }
                            }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            when { favourite && following -> "★ Favourite"; favourite -> "☆ Favourite"; following -> "✓ Following"; else -> "＋ Follow" },
                            color = if (following) VibeColors.ChipText else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val gc = genderColor(h.gender)
                    Tag("${genderMark(h.gender)} ${h.age}", gc, gc.copy(alpha = 0.13f), gc.copy(alpha = 0.4f))
                    Tag("📍 ${h.city}", Color(0xFF5A3F4D), VibeColors.Card, VibeColors.Line)
                    if (!real) Tag("★ Lv.${h.level}", VibeColors.Gold, VibeColors.Gold.copy(alpha = 0.15f), VibeColors.Gold.copy(alpha = 0.4f))
                    if (!real || h.ratingCount > 0) Tag("⭐ ${h.rating} (${"%,d".format(h.ratingCount)})", VibeColors.Cyan, VibeColors.Cyan.copy(alpha = 0.1f), VibeColors.Cyan.copy(alpha = 0.4f))
                    if (h.vip) Tag("VIP", Color.Black, VibeColors.Gold, VibeColors.Gold)
                }

                if (!real || h.verified) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat(h.followers, "Followers", Modifier.weight(1f))
                    val st = stats
                    if (!real) Stat("%,d".format(h.calls), "Calls", Modifier.weight(1f))
                    else if (h.verified) Stat(st?.let { "%,d".format(it.calls) } ?: "–", "Calls", Modifier.weight(1f))
                    if (!real) Stat("${h.answerRate}%", "Answer rate", Modifier.weight(1f))
                    else if (h.verified) Stat(st?.answerRate?.let { "$it%" } ?: "–", "Answer rate", Modifier.weight(1f))
                }

                if (h.bio.isNotBlank()) Row(Modifier.fillMaxWidth().cardSurface().padding(16.dp)) {
                    Text("“", color = VibeColors.Gold, fontSize = 30.sp, modifier = Modifier.padding(end = 10.dp))
                    Text(h.bio, color = Color(0xFF5A3F4D), fontSize = 14.sp, lineHeight = 21.sp)
                }

                Block("Speaks") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        h.langs.forEach { Text(it, color = VibeColors.Ink, fontSize = 14.sp, modifier = Modifier.cardSurface(12.dp).padding(horizontal = 12.dp, vertical = 6.dp)) }
                    }
                }
                // what users said after their calls (from the rating tags), most said first
                val said = if (real) stats?.tags.orEmpty().map { (t, n) -> "${tagEmoji[t] ?: "✨"} $t · ${"%,d".format(n)}" }
                    else listOf("😊 Friendly · 842", "🎵 Great vibe · 610", "😂 Funny · 355", "🧠 Smart · 201")
                // both sections always show on a host's profile (as in the design); empty = a short note
                if (!real || h.verified) Block("What people say") {
                    if (said.isEmpty()) EmptyNote("No reviews yet · they appear after rated calls")
                    else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        said.forEach {
                            Text(it, color = VibeColors.Ink, fontSize = 12.sp, modifier = Modifier.cardSurface(99.dp).padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
                val got = if (real) stats?.gifts.orEmpty().map { (name, emoji, n) -> (emoji ?: giftEmoji[name] ?: "🎁") to n }
                    else listOf("🌹" to 64, "☕" to 31, "🧸" to 18, "💍" to 9, "🏎️" to 6)
                val gotTotal = if (real) stats?.giftsTotal ?: 0 else 128
                if (!real || h.verified) Block("Gifts received", "${"%,d".format(gotTotal)} total") {
                    if (got.isEmpty()) EmptyNote("No gifts yet")
                    else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        got.forEach { (e, n) ->
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
            // hosts do not send gifts
            if (!hostViewer) Box(Modifier.size(56.dp).cardSurface().clickable { showGifts = true }, contentAlignment = Alignment.Center) { Text("🎁", fontSize = 24.sp) }
            val main = when {
                h.status == HostStatus.BUSY -> CallState.BUSY
                h.status == HostStatus.OFFLINE -> CallState.OFFLINE
                else -> CallState.VIDEO
            }
            // live call button: gentle pulse, a light sweep across it and the camera icon wiggling like a ringing phone
            val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "callbtn")
            val pulse by t.animateFloat(
                1f, 1.035f,
                androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
                label = "pulse",
            )
            val sweep by t.animateFloat(
                -0.4f, 1.4f,
                androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2200, easing = androidx.compose.animation.core.LinearEasing)),
                label = "sweep",
            )
            val wiggle by t.animateFloat(
                0f, 0f,
                androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.keyframes {
                    durationMillis = 2000
                    0f at 0; -14f at 100; 14f at 200; -10f at 300; 10f at 400; 0f at 500; 0f at 2000
                }),
                label = "wiggle",
            )
            if (h.status.canCall) Box(
                Modifier.weight(1f).height(56.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(RoundedCornerShape(18.dp)).background(VibeBrush.Button)
                    .drawWithContent {
                        drawContent()
                        val x = size.width * sweep
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                                start = androidx.compose.ui.geometry.Offset(x - 90f, 0f), end = androidx.compose.ui.geometry.Offset(x + 90f, size.height),
                            ),
                        )
                    }
                    .clickable { call() }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { CallLabel(main, callRate, onPink = true, wiggle = wiggle) }
            else Box(Modifier.weight(1f).height(56.dp).cardSurface().clickable { call() }, contentAlignment = Alignment.Center) {
                CallLabel(main, callRate, hostCalling = hostViewer)
            }
        }
    }

    if (showBlock) com.vibe.app.ui.components.BlockConfirmDialog(
        name = h.name,
        onConfirm = { SafetyStore.block(h.id); showBlock = false; ctx.toast("${h.name} blocked"); onBack() },
        onDismiss = { showBlock = false },
    )

    unlockTarget?.let { (kind, idx) ->
        val sl = slide(idx)
        val price = sl.price
        val balance = Demo.BALANCE
        val enough = balance >= price
        ModalBottomSheet(onDismissRequest = { unlockTarget = null }, containerColor = VibeColors.Card) {
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (kind == "photo") "Unlock this photo" else "Unlock this video", color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
                    else if (real) {
                        val m = sl.item
                        if (m != null) scope.launch {
                            // the server charges, credits the host and only then lets the file through
                            try {
                                HostRepo.unlock(m.id); opened.add(m.id)
                                ctx.toast("Unlocked · $price coins used")
                            } catch (e: ApiException) { if (e.code == "not_enough_coins") onWallet() else ctx.toast(e.message) }
                        }
                    } else {
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

    // gifts from the server's catalogue; the animation plays over the screen on both phones once the server has charged it
    if (showGifts) com.vibe.app.ui.components.GiftSheet(h.id, "profile", onDismiss = { showGifts = false }, onNeedCoins = { showGifts = false; onWallet() })

    if (showSafety) {
        ModalBottomSheet(onDismissRequest = { showSafety = false }, containerColor = VibeColors.Card) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetRow("🚩  Report", VibeColors.Ink) { showSafety = false; onReport(h.id) }
                SheetRow("⛔  Block", VibeColors.Red) { showSafety = false; showBlock = true }
                SheetRow("Cancel", VibeColors.ChipText) { showSafety = false }
            }
        }
    }
}

@Composable
private fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector?, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) androidx.compose.material3.Icon(icon, label, tint = VibeColors.Pink, modifier = Modifier.size(22.dp))
        else Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { repeat(3) { Box(Modifier.size(4.dp).clip(CircleShape).background(VibeColors.Pink)) } }
    }
}

@Composable
private fun Tag(text: String, color: Color, bg: Color, border: Color) {
    Text(text, color = color, fontSize = 13.sp, modifier = Modifier.clip(CircleShape).background(bg).border(1.dp, border, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp))
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.cardSurface().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = VibeColors.TextMute, fontSize = 11.sp)
    }
}

@Composable
private fun Block(title: String, trailing: String? = null, content: @Composable () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(title, color = Color(0xFF5A3F4D), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (trailing != null) Text(trailing, color = VibeColors.TextMute, fontSize = 12.sp)
        }
        content()
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().cardSurface().padding(horizontal = 14.dp, vertical = 12.dp))
}

private enum class CallState { VIDEO, OFFLINE, BUSY }

/** Main call button content: icon + one-line title + one-line subtitle, matching the host's state. */
@Composable
private fun CallLabel(state: CallState, rate: Int, onPink: Boolean = false, wiggle: Float = 0f, hostCalling: Boolean = false) {
    val orange = Color(0xFFFB923C)
    val (icon, title, sub) = when (state) {
        CallState.VIDEO -> Triple(com.vibe.app.ui.components.VibeIcons.Video, "Video Call", "$rate coins/min")
        // a host cannot be "notified" about a user; the user is simply offline
        CallState.OFFLINE -> if (hostCalling) Triple(com.vibe.app.ui.components.VibeIcons.Video, "Offline", "Try again later")
            else Triple(com.vibe.app.ui.components.CallIcons.Bell, "Notify me", "Offline now")
        CallState.BUSY -> Triple(com.vibe.app.ui.components.VibeIcons.Video, "On a call", "Try again soon")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).graphicsLayer { rotationZ = wiggle }.clip(CircleShape).background(
                when (state) {
                    CallState.VIDEO -> if (onPink) SolidColor(Color.White) else VibeBrush.Button
                    CallState.BUSY -> SolidColor(orange.copy(alpha = 0.18f))
                    CallState.OFFLINE -> SolidColor(VibeColors.Line)
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                icon, null, modifier = Modifier.size(19.dp),
                tint = when (state) {
                    CallState.VIDEO -> if (onPink) VibeColors.Pink else Color.White
                    CallState.BUSY -> orange
                    CallState.OFFLINE -> VibeColors.ChipText
                },
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = if (onPink) Color.White else VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val priced = state == CallState.VIDEO
                if (priced) { CoinDot(11.dp); Spacer(Modifier.width(4.dp)) }
                Text(sub, color = if (onPink) Color.White.copy(alpha = 0.9f) else if (priced) VibeColors.Gold else VibeColors.TextDim, fontSize = 12.sp, maxLines = 1)
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
                Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).border(1.dp, VibeColors.Ink.copy(alpha = 0.25f), CircleShape).clickable(onClick = onClose),
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


/** What one slide of the photo / video slider shows. */
private data class Slide(val video: Boolean, val locked: Boolean, val price: Int, val dur: Int, val item: ServerMedia?)

/**
 * A host's video inside the slider: plays by itself while its slide is on screen, a tap pauses it and the next tap plays it again.
 * The request carries the login token, so a locked file can never be played without paying. It stops when the slide or the app goes away.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun InlineVideo(m: ServerMedia, active: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var userPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val player = remember(m.id) {
        val http = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer ${com.vibe.app.data.api.AuthStore.accessToken.orEmpty()}"))
        androidx.media3.exoplayer.ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(http)).build().apply {
                setMediaItem(androidx.media3.common.MediaItem.fromUri(com.vibe.app.ui.components.AuthImages.url(m.path)))
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE   // short intro clips loop like a story
                prepare()
            }
    }
    androidx.compose.runtime.DisposableEffect(player) {
        val l = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    // plays only while its slide is the one on screen (and the user has not paused it)
    androidx.compose.runtime.LaunchedEffect(active, userPaused) { player.playWhenReady = active && !userPaused }
    androidx.compose.runtime.LaunchedEffect(active) { if (!active) userPaused = false }
    // the app going to the background pauses it
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) player.playWhenReady = false
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) player.playWhenReady = active && !userPaused
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    androidx.compose.runtime.LaunchedEffect(player) {
        while (true) {
            val d = player.duration
            progress = if (d > 0) (player.currentPosition.toFloat() / d).coerceIn(0f, 1f) else 0f
            kotlinx.coroutines.delay(200)
        }
    }
    Box(modifier.clickable { userPaused = playing }, contentAlignment = Alignment.Center) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { c ->
                androidx.media3.ui.PlayerView(c).apply {
                    this.player = player; useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)   // the first-frame picture shows until the video starts
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (!playing) Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
            Text("▶", color = VibeColors.Pink, fontSize = 28.sp)
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 136.dp).fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color.White))
        }
    }
}

// the emoji shown next to each rating tag (the server sends the words only)
private val tagEmoji = mapOf("Friendly" to "😊", "Great vibe" to "🎵", "Funny" to "😂", "Smart" to "🧠", "Good listener" to "🗣️", "Respectful" to "✨")
// gifts without an emoji from the admin yet
private val giftEmoji = mapOf("Rose" to "🌹", "Chai" to "☕", "Choco" to "🍫", "Teddy" to "🧸", "Bouquet" to "💐", "Ring" to "💍", "Car" to "🏎️", "Castle" to "🏰")
