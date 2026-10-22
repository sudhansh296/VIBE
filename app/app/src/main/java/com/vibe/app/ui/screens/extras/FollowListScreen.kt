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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.Demo
import com.vibe.app.data.HostStatus
import com.vibe.app.data.SafetyStore
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.VibeIcons
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

private val SINCE = mapOf(
    1 to "Since 2 Oct", 3 to "Since 28 Sep", 6 to "Since 15 Sep", 2 to "Since 3 Sep", 7 to "Since 20 Aug",
    5 to "Followed you 2h ago", 8 to "Followed you yesterday", 4 to "Followed you 3 days ago",
)

/**
 * My people: Following / Followers with search, sort, chat + video call buttons and follow back.
 * DEMO lists; the server returns the real ones with "since" times (blocked people never appear).
 */
@Composable
fun FollowListScreen(
    startTab: String, onBack: () -> Unit, onOpenHost: (Int) -> Unit, onDiscover: () -> Unit,
    onChat: (Int) -> Unit, onCall: (Int, Boolean) -> Unit,
) {
    val ctx = LocalContext.current
    val floatY by rememberFloat()
    var tab by remember { mutableStateOf(if (startTab == "followers") "followers" else "following") }
    var sortOnline by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    val following = remember { mutableStateListOf(1, 3, 6, 2, 7) }
    val followers = remember { mutableStateListOf(5, 8, 4) }
    val followingBack = remember { mutableStateListOf(5, 2) }   // followers I already follow

    val all = (if (tab == "following") following else followers).filter { !SafetyStore.isBlocked(it) }.map { Demo.host(it) }
    val q = query.trim().lowercase()
    val shown = all.filter { q.isEmpty() || (it.name + " " + it.city).lowercase().contains(q) }
        .let { list -> if (sortOnline) list.sortedBy { if (it.status == HostStatus.OFFLINE) 1 else 0 } else list.sortedBy { it.name } }
    val onlineCount = all.count { it.status == HostStatus.ONLINE }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        GlowBlob(VibeColors.Cyan, 240.dp, Modifier.align(Alignment.CenterEnd).offset(110.dp, (-60).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("My people", onBack)

            // big segmented tabs with counts
            val segShape = RoundedCornerShape(18.dp)
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(segShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, segShape).padding(4.dp)) {
                listOf("following" to following.size, "followers" to followers.size).forEach { (k, n) ->
                    val on = tab == k
                    Column(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp))
                            .then(if (on) Modifier.background(Brush.linearGradient(listOf(VibeColors.Pink.copy(alpha = 0.25f), VibeColors.Cyan.copy(alpha = 0.12f)))).border(1.dp, VibeColors.Pink.copy(alpha = 0.5f), RoundedCornerShape(14.dp)) else Modifier)
                            .noRippleClick { tab = k },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Text("$n", color = if (on) Color.White else Color(0xFF8B8B9A), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text(k.uppercase(), color = if (on) Color.White else Color(0xFF8B8B9A), fontSize = 10.sp, letterSpacing = 1.4.sp)
                    }
                }
            }

            // search + sort
            val fShape = RoundedCornerShape(16.dp)
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth().height(48.dp).clip(fShape).background(VibeColors.Card).border(1.5.dp, VibeColors.Line, fShape).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(CallIcons.Search, null, tint = VibeColors.TextMute, modifier = Modifier.size(20.dp))
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = SolidColor(VibeColors.Pink), modifier = Modifier.weight(1f),
                    decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search name or city", color = VibeColors.TextMute, fontSize = 14.sp); inner() } },
                )
            }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip("Online first", sortOnline) { sortOnline = true }
                SortChip("A to Z", !sortOnline) { sortOnline = false }
                Box(Modifier.weight(1f))
                Text("$onlineCount online now", color = VibeColors.Green, fontSize = 11.sp)
            }

            if (all.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painterResource(R.drawable.ghost_empty), null, Modifier.size(160.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                Text(if (tab == "following") "You are not following anyone" else "No followers yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text(
                    if (tab == "following") "Follow people you like and they show up here when they are online." else "When people follow you, you will see them here.",
                    color = VibeColors.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )
                NeonButton("Discover people", onDiscover)
            } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (shown.isEmpty()) Text("No one matches your search", color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp))
                shown.forEach { h ->
                    val isF = tab == "following" || h.id in followingBack
                    PersonCard(
                        h, since = SINCE[h.id].orEmpty(), isNew = tab == "followers" && h.id == 5, following = isF,
                        onOpen = { onOpenHost(h.id) }, onChat = { onChat(h.id) },
                        onCall = { if (h.status == HostStatus.ONLINE) onCall(h.id, false) else ctx.toast("${h.name} is ${if (h.status == HostStatus.BUSY) "busy" else "offline"} now") },
                        onToggle = {
                            if (isF) { if (tab == "following") following.remove(h.id) else followingBack.remove(h.id); ctx.toast("Unfollowed") }
                            else { followingBack.add(h.id); if (h.id !in following) following.add(h.id); ctx.toast("Following") }
                        },
                    )
                }
                Box(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SortChip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text, color = if (on) Color.White else VibeColors.ChipText, fontSize = 11.sp,
        modifier = Modifier.height(30.dp).clip(CircleShape).background(if (on) VibeColors.Pink.copy(alpha = 0.15f) else VibeColors.Card)
            .border(1.dp, if (on) VibeColors.Pink else VibeColors.Line, CircleShape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun PersonCard(
    h: com.vibe.app.data.Host, since: String, isNew: Boolean, following: Boolean,
    onOpen: () -> Unit, onChat: () -> Unit, onCall: () -> Unit, onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val live = h.status == HostStatus.ONLINE
    Row(
        Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, Color(0xFF1F1F2D), shape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(58.dp).clip(CircleShape).background(if (live) Brush.sweepGradient(listOf(VibeColors.Green, VibeColors.Cyan, VibeColors.Green)) else Brush.linearGradient(listOf(Color(0xFF2A2A3A), Color(0xFF2A2A3A))))
                .clickable(onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(53.dp).clip(CircleShape).background(VibeColors.Dark), contentAlignment = Alignment.Center) {
                Avatar(h, Modifier.size(48.dp).clip(CircleShape), fontSize = 18.sp)
            }
            Box(Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.5.dp).clip(CircleShape).background(h.status.dot))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${h.name}, ${h.age}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                if (isNew) Text("NEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(CircleShape).background(VibeColors.Pink).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Text(
                (if (h.status == HostStatus.OFFLINE) "Offline" else if (h.status == HostStatus.BUSY) "Busy" else "Online") + " · ${h.city}",
                color = if (live) VibeColors.Green else VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp),
            )
            Text(since, color = Color(0xFF4B5563), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
            Text(
                if (following) "✓ Following" else "+ Follow back", color = if (following) Color(0xFFD1D5DB) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape)
                    .then(if (following) Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape) else Modifier.background(VibeBrush.Button))
                    .clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(VibeColors.Dark).border(1.dp, VibeColors.Line, RoundedCornerShape(14.dp)).clickable(onClick = onChat),
                contentAlignment = Alignment.Center,
            ) { Icon(MeIcons.Chat, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(20.dp)) }
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).then(if (live) Modifier.background(VibeBrush.Button) else Modifier.background(VibeColors.Dark).border(1.dp, VibeColors.Line, RoundedCornerShape(14.dp)))
                    .clickable(onClick = onCall),
                contentAlignment = Alignment.Center,
            ) { Icon(VibeIcons.Video, null, tint = if (live) Color.White else Color(0xFF4B5563), modifier = Modifier.size(20.dp)) }
        }
    }
}
