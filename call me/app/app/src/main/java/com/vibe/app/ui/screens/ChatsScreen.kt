package com.vibe.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.Demo
import com.vibe.app.data.Host
import com.vibe.app.data.HostStatus
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CallIconButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.FieldBox
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.VibeIcons
import com.vibe.app.ui.components.VibeTabBar
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import com.vibe.app.data.ChatRepo
import com.vibe.app.data.HostRepo

/** Messages: Chats (real, live over Socket.IO), Calls (history comes with real calls) and Following (from the server). */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onHome: () -> Unit,
    onOpenChat: (Int) -> Unit,
    onOpenHost: (Int) -> Unit,
    onNotifications: () -> Unit,
    onCall: (id: Int) -> Unit,
    onOpenMe: () -> Unit,
    onReport: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf("chats") }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var actionsFor by remember { mutableStateOf<Host?>(null) }
    val q = query.trim().lowercase()
    fun matches(h: Host) = q.isEmpty() || h.name.lowercase().contains(q)
    var loading by remember { mutableStateOf(ChatRepo.chats.isEmpty()) }
    val following = remember { androidx.compose.runtime.mutableStateListOf<Int>() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { ChatRepo.refreshList() }.onFailure { ctx.toast(it.message ?: "Could not load chats") }
        loading = false
    }
    // call history (Calls tab), from the server
    val callLog = remember { androidx.compose.runtime.mutableStateListOf<CallLogRow>() }
    var callsLoading by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(tab) {
        if (tab == "calls") {
            callsLoading = true
            runCatching { com.vibe.app.data.api.Api.get("/calls").getJSONArray("items") }.onSuccess { a ->
                callLog.clear()
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val peer = o.optJSONObject("peer")?.let { p -> HostRepo.fromJson(p).also { h -> HostRepo.cache[h.id] = HostRepo.cache[h.id] ?: h } }
                    callLog.add(CallLogRow(o.getString("id"), peer, o.optString("kind"), o.optBoolean("outgoing"), o.optString("result"), o.optInt("seconds"),
                        if (o.has("coins")) o.optInt("coins") else null, if (o.has("earned")) o.optInt("earned") else null, o.optString("at")))
                }
            }
            callsLoading = false
        }
    }
    androidx.compose.runtime.LaunchedEffect(tab) {
        if (tab == "following") runCatching { HostRepo.people("/me/following") }.onSuccess { l -> following.clear(); following.addAll(l.map { it.id }) }
    }
    // "typing…" in the list for 4 seconds after the last key press
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); now = System.currentTimeMillis() } }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 260.dp, Modifier.align(Alignment.TopEnd).offset(80.dp, (-90).dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Messages", color = VibeColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                CallIconButton(CallIcons.Search, { searching = !searching; if (!searching) query = "" })
                Box(Modifier.width(8.dp))
                Box {
                    CallIconButton(CallIcons.Bell, onNotifications)
                    if (com.vibe.app.data.NotifRepo.unread > 0) Box(Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp).size(10.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.dp).clip(CircleShape).background(VibeColors.Pink))
                }
            }

            if (searching) FieldBox(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp), focused = true) {
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = VibeColors.Ink, fontSize = 14.sp), cursorBrush = SolidColor(VibeColors.Pink),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search chats", color = VibeColors.TextMute, fontSize = 14.sp); inner() } },
                )
            }

            // tabs
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("chats" to "Chats", "calls" to "Calls", "following" to "Following").forEach { (k, l) ->
                    Column(Modifier.width(IntrinsicSize.Max).noRippleClick { tab = k }) {
                        Text(l, color = if (tab == k) VibeColors.Ink else VibeColors.TextMute, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            Modifier.padding(top = 8.dp).height(2.dp).fillMaxWidth()
                                .then(if (tab == k) Modifier.background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))) else Modifier),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Line))

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 130.dp)) {
                when (tab) {
                    "chats" -> {
                        val list = ChatRepo.chats.map { it.copy(peer = HostRepo.cache[it.peer.id] ?: it.peer) }.filter { matches(it.peer) && !com.vibe.app.data.SafetyStore.isBlocked(it.peer.id) }
                        if (loading && list.isEmpty()) item {
                            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 3.dp, modifier = Modifier.size(28.dp)) }
                        } else if (list.isEmpty()) item { EmptyNote(if (q.isEmpty()) "No chats yet · say hi to someone from their profile" else "No chats found") }
                        items(list, key = { it.peer.id }) { c ->
                            ChatRow(c, typing = (ChatRepo.typing[c.peer.id] ?: 0L) > now - 4000, onClick = { onOpenChat(c.peer.id) }, onLongClick = { actionsFor = c.peer })
                        }
                    }
                    "calls" -> {
                        if (callsLoading && callLog.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 3.dp, modifier = Modifier.size(28.dp)) } }
                        else if (callLog.isEmpty()) item { EmptyNote("No calls yet") }
                        items(callLog.filter { c -> c.peer?.let { matches(it) } ?: false }, key = { it.id }) { c -> CallRow(c, onOpenHost, onCall) }
                    }
                    else -> {
                        val list = following.mapNotNull { HostRepo.cache[it] }.filter { matches(it) }
                        if (list.isEmpty()) item { EmptyNote("You are not following anyone yet") }
                        items(list, key = { it.id }) { h -> FollowRow(h, onOpenHost, onOpenChat) }
                    }
                }
            }
        }

        VibeTabBar(
            selected = 1, unread = ChatRepo.unread,
            onSelect = { if (it == 0) onHome() else if (it == 2) onOpenMe() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    actionsFor?.let { h ->
        ModalBottomSheet(onDismissRequest = { actionsFor = null }, containerColor = VibeColors.Card) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                Text(h.name, color = VibeColors.TextDim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                listOf(Triple(CallIcons.User, "View profile", false), Triple(CallIcons.Ban, "Block", true), Triple(CallIcons.Flag, "Report", true)).forEach { (icon, label, danger) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            actionsFor = null
                            when (label) {
                                "Block" -> { com.vibe.app.data.SafetyStore.block(h.id); ctx.toast("${h.name} blocked") }
                                "Report" -> onReport(h.id)
                                else -> onOpenHost(h.id)
                            }
                        }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, null, tint = if (danger) VibeColors.Red else VibeColors.Ink, modifier = Modifier.size(22.dp))
                        Text(label, color = if (danger) VibeColors.Red else VibeColors.Ink, fontSize = 15.sp, modifier = Modifier.padding(start = 14.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun PersonAvatar(h: Host, size: Dp, fontSize: Int = 20) {
    Box(Modifier.size(size)) {
        Avatar(h, Modifier.size(size).clip(CircleShape), fontSize.sp)
        if (h.status != HostStatus.OFFLINE) Box(
            Modifier.align(Alignment.BottomEnd).size(14.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.5.dp).clip(CircleShape).background(h.status.dot),
        )
    }
}

private fun shortTime(iso: String): String = try {
    val ms = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(iso.take(19))!!.time
    val off = java.util.TimeZone.getDefault().getOffset(ms); val day = 86_400_000L
    when ((System.currentTimeMillis() + off) / day - (ms + off) / day) {
        0L -> java.text.SimpleDateFormat("h:mm a", java.util.Locale.ENGLISH).format(java.util.Date(ms)).replace("AM", "am").replace("PM", "pm")
        1L -> "Yesterday"
        in 2L..6L -> java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).format(java.util.Date(ms))
        else -> java.text.SimpleDateFormat("d MMM", java.util.Locale.ENGLISH).format(java.util.Date(ms))
    }
} catch (e: Exception) { "" }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(c: com.vibe.app.data.ChatSummary, typing: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(c.peer, 56.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.peer.name, color = VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Text(shortTime(c.lastAt), color = if (c.unread > 0) VibeColors.Pink else VibeColors.TextMute, fontSize = 12.sp)
            }
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Preview(c, typing) }
                if (c.unread > 0) Box(
                    Modifier.padding(start = 8.dp).size(20.dp).clip(CircleShape).background(Brush.linearGradient(listOf(VibeColors.Pink, Color(0xFFFF7AB0)))),
                    contentAlignment = Alignment.Center,
                ) { Text(if (c.unread > 99) "99+" else "${c.unread}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp, style = centerLineStyle) }
            }
        }
    }
}

@Composable
private fun Preview(c: com.vibe.app.data.ChatSummary, typing: Boolean) {
    val grey = if (c.unread > 0) VibeColors.Ink else VibeColors.TextDim
    val m = c.last
    when {
        typing -> Text("typing…", color = VibeColors.Green, fontSize = 14.sp)
        m == null -> Text("", fontSize = 14.sp)
        m.kind == "call" -> Row(verticalAlignment = Alignment.CenterVertically) {
            val secs = m.text?.toIntOrNull() ?: 0
            Icon(VibeIcons.Video, null, tint = if (secs == 0) VibeColors.Red else VibeColors.TextDim, modifier = Modifier.size(14.dp))
            Text(if (secs > 0) "  Video call · %02d:%02d".format(secs / 60, secs % 60) else "  Missed video call", color = if (secs == 0) VibeColors.Red else grey, fontSize = 14.sp, maxLines = 1)
        }
        m.kind == "gift" -> Text(
            "🎁 " + (if (m.mine) "You sent " else "Sent you ") + (if (m.quantity > 1) "${m.quantity}× " else "a ") + (m.gift?.name ?: "gift"),
            color = grey, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        m.mine -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (m.read) "✓✓ " else "✓ ", color = if (m.read) VibeColors.Cyan else VibeColors.TextMute, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(m.text.orEmpty(), color = grey, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        else -> Text(m.text.orEmpty(), color = grey, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One call in the history. */
private data class CallLogRow(
    val id: String, val peer: Host?, val kind: String, val outgoing: Boolean, val result: String, val seconds: Int, val coins: Int?, val earned: Int?, val at: String,
)

@Composable
private fun CallRow(c: CallLogRow, onOpenHost: (Int) -> Unit, onCall: (Int) -> Unit) {
    val h = c.peer ?: return
    val live = HostRepo.cache[h.id] ?: h
    val missed = c.result != "answered"
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        PersonAvatar(live, 48.dp, 17)
        Column(Modifier.weight(1f).padding(start = 12.dp).noRippleClick { onOpenHost(h.id) }) {
            Text(h.name, color = if (missed) VibeColors.Red else VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text(if (c.outgoing) "↗ " else "↙ ", color = if (missed) VibeColors.Red else if (c.outgoing) VibeColors.Green else VibeColors.Cyan, fontSize = 12.sp)
                Icon(VibeIcons.Video, null, tint = VibeColors.TextDim, modifier = Modifier.size(12.dp))
                Text("  ${shortTime(c.at)}" + if (c.kind == "random") " · Random match" else "", color = VibeColors.TextDim, fontSize = 12.sp)
            }
            Text(
                when {
                    c.result == "answered" -> "%02d:%02d".format(c.seconds / 60, c.seconds % 60) + (c.coins?.let { " · %,d coins".format(it) } ?: c.earned?.let { " · +%,d earned".format(it) } ?: "")
                    c.result == "declined" -> "Declined"
                    c.result == "cancelled" -> if (c.outgoing) "Cancelled" else "Missed"
                    else -> if (c.outgoing) "No answer" else "Missed"
                },
                color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp),
            )
        }
        // users call hosts back; hosts do not call users
        if (c.outgoing) Box(
            Modifier.size(40.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable { onCall(h.id) },
            contentAlignment = Alignment.Center,
        ) { Icon(VibeIcons.Video, null, tint = VibeColors.Pink, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun FollowRow(h: Host, onOpenHost: (Int) -> Unit, onOpenChat: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        PersonAvatar(h, 56.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp).noRippleClick { onOpenHost(h.id) }) {
            Text(com.vibe.app.ui.components.nameAge(h.name, h.age), color = VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                when (h.status) { HostStatus.ONLINE -> "Online now"; HostStatus.BUSY -> "On a call"; HostStatus.OFFLINE -> "Offline" },
                color = h.status.text, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            Modifier.height(36.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape)
                .clickable { onOpenChat(h.id) }.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Chat", color = VibeColors.Ink, fontSize = 13.sp) }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp))
}
