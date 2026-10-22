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
    onCall: (id: Int, audio: Boolean) -> Unit,
    onIncomingDemo: () -> Unit,
    onNotifications: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenMe: () -> Unit,
    onStatusMine: () -> Unit,
    onStatusView: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf("discover") }
    var quick by rememberSaveable { mutableStateOf(if (filter.show == "all") "all" else filter.show) }
    val blockedIds = com.vibe.app.data.SafetyStore.blocked.toList()
    val list = remember(filter, quick, blockedIds) { filter.sort(Demo.hosts.filter { it.id !in blockedIds && filter.matches(it, quick) }) }

    fun call(h: Host, audio: Boolean) = when (h.status) {
        HostStatus.BUSY -> ctx.toast("${h.name} is on another call")
        HostStatus.OFFLINE -> ctx.toast("${h.name} is offline · we'll notify you")
        else -> onCall(h.id, audio)
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.offset((-90).dp, (-90).dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // header
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("VIBE", style = TextStyle(brush = VibeBrush.Text, fontSize = 24.sp, fontWeight = FontWeight.Black))
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.height(36.dp).clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0x4D713F12), Color(0x1A713F12))))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.3f), CircleShape)
                        .clickable { ctx.toast("Wallet comes in Batch 5") }.padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoinDot(24.dp); Spacer(Modifier.width(8.dp))
                    Text("%,d".format(Demo.BALANCE), color = VibeColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("  +", color = VibeColors.Gold, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Card)
                        .border(1.dp, VibeColors.Line, RoundedCornerShape(12.dp)).combinedClickable(onClick = onNotifications, onLongClick = onIncomingDemo), // long-press = incoming-call demo
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔔", fontSize = 15.sp)
                    Box(Modifier.align(Alignment.TopEnd).offset(3.dp, (-3).dp).size(11.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.dp).clip(CircleShape).background(VibeColors.Pink))
                }
            }

            // top tabs + filter
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("discover" to "Discover", "following" to "Following", "new" to "New").forEach { (k, label) ->
                    TopTab(label, tab == k) {
                        tab = k
                        if (k == "new") ctx.toast("Showing newest members first")
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

            if (tab == "following") FollowingEmpty { tab = "discover" }
            else LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 190.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("all" to "✨ All", "voice" to "📞 Voice", "F" to "♀ Girls", "M" to "♂ Boys", "online" to "🟢 Online")) { (k, label) ->
                            SelectChip(label, quick == k, { quick = k })
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    com.vibe.app.ui.screens.status.StatusRow(onMine = onStatusMine, onView = onStatusView, onHost = onOpenHost)
                }
                items(list, key = { it.id }) { h -> HostCard(h, onClick = { onOpenHost(h.id) }, onCall = { audio -> call(h, audio) }) }
                if (list.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No one matches your filters right now.", color = VibeColors.TextMute, fontSize = 14.sp)
                        Text("Clear filters", color = VibeColors.Cyan, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp).clickable { quick = "all"; onResetFilter() })
                    }
                }
            }
        }

        // random match button above the tab bar
        SpinBorder(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 112.dp).height(56.dp).noRippleClick(onRandomMatch),
        ) {
            Row(Modifier.padding(start = 8.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(VibeBrush.Button), contentAlignment = Alignment.Center) {
                    Text("⇄", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Random Match", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Meet someone new", color = VibeColors.Gold, fontSize = 11.sp)
                }
            }
        }

        VibeTabBar(
            selected = 0, unread = Demo.UNREAD,
            onSelect = { if (it == 1) onOpenChats() else if (it == 2) onOpenMe() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TopTab(label: String, on: Boolean, onClick: () -> Unit) {
    val bar by animateFloatAsState(if (on) 1f else 0f, label = "bar")
    Column(Modifier.width(IntrinsicSize.Max).noRippleClick(onClick)) {
        Text(label, color = if (on) Color.White else VibeColors.TextMute, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier.padding(top = 4.dp).height(4.dp).fillMaxWidth().graphicsLayer { scaleX = 0.3f + 0.7f * bar; alpha = bar }
                .clip(CircleShape).background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))),
        )
    }
}

@Composable
private fun BecomeHostBanner(onClick: () -> Unit) {
    val y by rememberFloat()
    Row(
        Modifier.fillMaxWidth().neonBorder().background(Brush.linearGradient(listOf(Color(0xEB12121C), Color(0xD91E0F17))))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ghost_profile), null, Modifier.size(56.dp).graphicsLayer { translationY = y.dp.toPx() })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Become a Host · Earn money", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Open for everyone 18+ · Boys & Girls", color = VibeColors.TextDim, fontSize = 12.sp)
        }
        Text("›", color = VibeColors.Pink, fontSize = 24.sp)
    }
}

@Composable
private fun OnlineStory(h: Host, onClick: () -> Unit) {
    // premium story: dark glass avatar; online = rotating neon ring, busy = plain ring; status dot on all
    val live = h.status == HostStatus.ONLINE
    val spin by rememberInfiniteTransition(label = "story").animateFloat(
        0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "ring",
    )
    Column(Modifier.noRippleClick(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(62.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = 2.dp.toPx()
                if (live) rotate(spin) {
                    drawCircle(VibeColors.Pink.copy(alpha = 0.25f), radius = size.minDimension / 2 - stroke, style = Stroke(stroke * 3))
                    drawCircle(Brush.sweepGradient(listOf(VibeColors.Pink, VibeColors.Cyan, VibeColors.Pink)), radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
                } else drawCircle(Color.White.copy(alpha = 0.15f), radius = size.minDimension / 2 - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
            }
            Box(
                Modifier.matchParentSize().padding(4.dp).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(h.c1.copy(alpha = 0.28f), Color(0xFF14141F))))
                    .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(40f, 30f), radius = 70f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(h.name.take(1), color = Color.White.copy(alpha = 0.9f), fontSize = 19.sp, fontWeight = FontWeight.SemiBold) }
            Box(
                Modifier.align(Alignment.BottomEnd).offset((-3).dp, (-3).dp).size(13.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.5.dp)
                    .clip(CircleShape).background(h.status.dot),
            )
        }
        Text(h.name, color = Color(0xFFA1A1B0), fontSize = 11.sp, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun HostCard(h: Host, onClick: () -> Unit, onCall: (audio: Boolean) -> Unit) {
    Box(Modifier.fillMaxWidth().height(256.dp).neonBorder().clickable(onClick = onClick)) {
        Avatar(h, Modifier.matchParentSize(), 72.sp)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.2f), 0.45f to Color.Black.copy(alpha = 0.1f), 1f to Color.Black.copy(alpha = 0.9f))))
        com.vibe.app.ui.components.StatusPill(h.status, Modifier.align(Alignment.TopStart).padding(10.dp))
        Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { HostBadge(h) }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 12.dp, end = 60.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${h.name}, ${h.age} ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(genderMark(h.gender), color = genderColor(h.gender), fontSize = 14.sp)
            }
            Text("📍 ${h.city} · ⭐ ${h.rating}", color = Color(0xFFD1D5DB), fontSize = 11.sp, maxLines = 1)
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                CoinDot(12.dp); Spacer(Modifier.width(4.dp))
                // prices for the call types this host accepts: voice (green phone) · video (pink camera)
                // compact so it never wraps next to the call buttons
                if (h.voice) {
                    androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.Phone, "Voice", tint = VibeColors.Green, modifier = Modifier.size(10.dp))
                    Text("${h.audioRate}", color = VibeColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(start = 2.dp))
                }
                if (h.voice && h.video) Text(" · ", color = VibeColors.TextMute, fontSize = 11.sp, maxLines = 1)
                if (h.video) {
                    androidx.compose.material3.Icon(com.vibe.app.ui.components.VibeIcons.Video, "Video", tint = VibeColors.Pink, modifier = Modifier.size(10.dp))
                    Text("${h.rate}", color = VibeColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(start = 2.dp))
                }
                if (!(h.voice && h.video)) Text(if (h.voice) " voice" else " video", color = VibeColors.TextDim, fontSize = 9.sp, maxLines = 1)
            }
        }
        // voice (glass, green) above video (neon); only the call types the host accepts
        if (h.voice) Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = if (h.video) 60.dp else 12.dp).size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.45f)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .clickable { onCall(true) },
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.Phone, "Voice call",
                tint = if (h.status.canCall) VibeColors.Green else VibeColors.TextMute, modifier = Modifier.size(18.dp))
        }
        if (h.video) Box(
            Modifier.align(Alignment.BottomEnd).padding(12.dp).size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(if (h.status.canCall) VibeBrush.Button else Brush.linearGradient(listOf(Color(0xCC374151), Color(0xCC374151))))
                .clickable { onCall(false) },
            contentAlignment = Alignment.Center,
        ) { androidx.compose.material3.Icon(com.vibe.app.ui.components.VibeIcons.Video, "Video call", tint = Color.White, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun FollowingEmpty(onDiscover: () -> Unit) {
    val y by rememberFloat()
    Column(Modifier.fillMaxWidth().padding(top = 40.dp, start = 32.dp, end = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.ghost_empty), null, Modifier.size(176.dp).graphicsLayer { translationY = y.dp.toPx() })
        Text("No one here yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Text("Follow people you like and they'll show up here when they're online.", color = VibeColors.TextDim, fontSize = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Box(
            Modifier.padding(top = 24.dp).height(44.dp).clip(CircleShape).background(VibeBrush.Button).clickable(onClick = onDiscover).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Discover people", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
    }
}
