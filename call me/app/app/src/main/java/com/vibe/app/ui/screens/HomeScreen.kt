package com.vibe.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.Demo
import com.vibe.app.data.Host
import com.vibe.app.data.HostFilter
import com.vibe.app.data.HostStatus
import com.vibe.app.data.HostRepo
import com.vibe.app.data.api.ApiException
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.HostBadge
import com.vibe.app.ui.components.SelectChip
import com.vibe.app.ui.components.SpinBorder
import com.vibe.app.ui.components.VibeTabBar
import com.vibe.app.ui.components.genderColor
import com.vibe.app.ui.components.genderMark
import com.vibe.app.ui.components.neonBorder
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    filter: HostFilter,
    onOpenFilter: () -> Unit,
    onOpenHost: (Int) -> Unit,
    onRandomMatch: () -> Unit,
    onResetFilter: () -> Unit,
    onCall: (id: Int) -> Unit,
    onNotifications: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenMe: () -> Unit,
    onStatusMine: () -> Unit,
    onStatusView: (Int) -> Unit,
    onWallet: () -> Unit = {},
) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf("discover") }
    var quick by rememberSaveable { mutableStateOf(if (filter.show == "all") "all" else filter.show) }
    val blockedIds = com.vibe.app.data.SafetyStore.blocked.toList()
    // the list comes from the server (it applies the filters, hides blocked people and decides who may be seen)
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(tab, filter, quick, retry) {
        loading = true; failed = null
        try { HostRepo.loadHome(tab, filter, quick) } catch (e: ApiException) { failed = e.message }
        loading = false
    }
    // after the live link (re)connects, who is online may have changed while it was down: reload quietly
    val connects = com.vibe.app.data.Realtime.connects
    androidx.compose.runtime.LaunchedEffect(connects) {
        if (connects > 0 && !loading) runCatching { HostRepo.loadHome(tab, filter, quick) }
    }
    val list =filter.sort(HostRepo.home.mapNotNull { HostRepo.cache[it] }.filter { it.id !in blockedIds })

    fun call(h: Host) = when (h.status) {
        HostStatus.BUSY -> ctx.toast("${h.name} is on another call")
        HostStatus.OFFLINE -> ctx.toast(if (h.verified) "${h.name} is offline · we'll notify you" else "${h.name} is offline")
        else -> onCall(h.id)
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.offset((-90).dp, (-90).dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // header
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("VIBE", style = TextStyle(brush = VibeBrush.Text, fontSize = 24.sp, fontWeight = FontWeight.Black))
                Spacer(Modifier.weight(1f))
                // hosts earn and never spend: no coin chip and no Random match for them
                val iAmHost = com.vibe.app.data.api.AuthStore.me?.optString("role") == "host"
                if (!iAmHost) Row(
                    Modifier.height(36.dp).clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, VibeColors.Line, CircleShape)
                        .clickable(onClick = onWallet).padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoinDot(24.dp); Spacer(Modifier.width(8.dp))
                    Text("%,d".format(Demo.BALANCE), color = VibeColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("  +", color = VibeColors.Gold, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Card)
                        .border(1.dp, VibeColors.Line, RoundedCornerShape(12.dp)).clickable(onClick = onNotifications),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(com.vibe.app.ui.components.Glyph.Bell, "Notifications", tint = Color(0xFF5A3F4D), modifier = Modifier.size(19.dp))
                    if (com.vibe.app.data.NotifRepo.unread > 0) Box(Modifier.align(Alignment.TopEnd).offset(3.dp, (-3).dp).size(11.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.dp).clip(CircleShape).background(VibeColors.Pink))
                }
            }

            // top tabs + filter
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("discover" to "Discover", "following" to "Following", "new" to "New").forEach { (k, label) ->
                    TopTab(label, tab == k) {
                        tab = k
                    }
                    Spacer(Modifier.width(22.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.height(36.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape)
                        .clickable(onClick = onOpenFilter).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⚙  Filter", color = VibeColors.ChipText, fontSize = 13.sp)
                    if (!filter.isDefault) Box(Modifier.padding(start = 6.dp).size(8.dp).clip(CircleShape).background(VibeColors.Pink))
                }
            }

            if (tab == "following" && list.isEmpty() && !loading && failed == null) FollowingEmpty { tab = "discover" }
            else LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 170.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("all" to "All", "F" to "Girls", "M" to "Boys", "online" to "Online now")) { (k, label) ->
                            SelectChip(label, quick == k, { quick = k })
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    com.vibe.app.ui.screens.status.StatusRow(onMine = onStatusMine, onView = onStatusView, onHost = onOpenHost)
                }
                items(list, key = { it.id }) { h -> HostRowCard(h, onClick = { onOpenHost(h.id) }, onCall = { call(h) }) }
                if (loading && list.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                    }
                } else if (failed != null) item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(failed.orEmpty(), color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Text("Try again", color = VibeColors.Pink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp).clickable { retry++ })
                    }
                } else if (list.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val plain = filter.isDefault && quick == "all"
                        Text(if (plain) "No one here yet. Please check back soon." else "No one matches your filters right now.", color = VibeColors.TextMute, fontSize = 14.sp)
                        if (!plain) Text("Clear filters", color = VibeColors.Cyan, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp).clickable { quick = "all"; onResetFilter() })
                    }
                }
                if (HostRepo.nextPage != null && list.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    androidx.compose.runtime.LaunchedEffect(HostRepo.home.size) {
                        runCatching { HostRepo.loadHome(tab, filter, quick, more = true) }
                    }
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // random match: centred above the tab bar
        if (com.vibe.app.data.api.AuthStore.me?.optString("role") != "host") RandomMatchPill(onRandomMatch, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 106.dp))

        VibeTabBar(
            selected = 0, unread = com.vibe.app.data.ChatRepo.unread,
            onSelect = { if (it == 1) onOpenChats() else if (it == 2) onOpenMe() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TopTab(label: String, on: Boolean, onClick: () -> Unit) {
    val bar by animateFloatAsState(if (on) 1f else 0f, label = "bar")
    Column(Modifier.width(IntrinsicSize.Max).noRippleClick(onClick)) {
        Text(label, color = if (on) VibeColors.Ink else VibeColors.TextMute, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier.padding(top = 4.dp).height(4.dp).fillMaxWidth().graphicsLayer { scaleX = 0.3f + 0.7f * bar; alpha = bar }
                .clip(CircleShape).background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))),
        )
    }
}

@Composable
private fun FollowingEmpty(onDiscover: () -> Unit) {
    val y by rememberFloat()
    Column(Modifier.fillMaxWidth().padding(top = 40.dp, start = 32.dp, end = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.ghost_empty), null, Modifier.size(176.dp).graphicsLayer { translationY = y.dp.toPx() })
        Text("No one here yet", color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Text("Follow people you like and they'll show up here when they're online.", color = VibeColors.TextDim, fontSize = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Box(
            Modifier.padding(top = 24.dp).height(44.dp).clip(CircleShape).background(VibeBrush.Button).clickable(onClick = onDiscover).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Discover people", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
    }
}


/** Random Match button (call-me edition): gradient capsule with a pulsing pink glow, white bolt badge and two lines of text. */
@Composable
private fun RandomMatchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "match")
    val pulse by t.animateFloat(
        0.92f, 1.14f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1300), androidx.compose.animation.core.RepeatMode.Reverse), label = "p",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(width = 210.dp, height = 54.dp).graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = 0.55f }
                .clip(CircleShape).background(Color(0x66FF4D8D)),
        )
        Row(
            Modifier.clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFFFF4D8D), Color(0xFFFF7AB0)))).noRippleClick(onClick)
                .padding(start = 7.dp, end = 22.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(com.vibe.app.ui.components.Glyph.Bolt, "Random match", tint = VibeColors.Pink, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text("Random match", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                val rate = com.vibe.app.ui.screens.rememberRandomRate()
                Text(if (rate > 0) "Video chat with someone new · $rate coins/min" else "Video chat with someone new", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
    }
}

