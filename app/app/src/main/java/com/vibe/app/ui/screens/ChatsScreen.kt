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
import com.vibe.app.data.CallLogItem
import com.vibe.app.data.ChatDemo
import com.vibe.app.data.ChatPreview
import com.vibe.app.data.Demo
import com.vibe.app.data.Host
import com.vibe.app.data.HostStatus
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CallIconButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CallSheetBg
import com.vibe.app.ui.components.FieldBox
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.VibeIcons
import com.vibe.app.ui.components.VibeTabBar
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

/** Messages: Chats / Calls / Following. DEMO lists; the server provides the real ones. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onHome: () -> Unit,
    onOpenChat: (Int) -> Unit,
    onOpenHost: (Int) -> Unit,
    onNotifications: () -> Unit,
    onCall: (id: Int, audio: Boolean) -> Unit,
    onOpenMe: () -> Unit,
    onReport: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf("chats") }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var actionsFor by remember { mutableStateOf<Host?>(null) }
    val q = query.trim().lowercase()
    fun matches(id: Int) = q.isEmpty() || (if (id == 0) "vibe team" else Demo.host(id).name.lowercase()).contains(q)

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 260.dp, Modifier.align(Alignment.TopEnd).offset(80.dp, (-90).dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Messages", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                CallIconButton(CallIcons.Search, { searching = !searching; if (!searching) query = "" })
                Box(Modifier.width(8.dp))
                Box {
                    CallIconButton(CallIcons.Bell, onNotifications)
                    Box(Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp).size(10.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.dp).clip(CircleShape).background(VibeColors.Pink))
                }
            }

            if (searching) FieldBox(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp), focused = true) {
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = SolidColor(VibeColors.Pink),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search chats", color = VibeColors.TextMute, fontSize = 14.sp); inner() } },
                )
            }

            // tabs
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("chats" to "Chats", "calls" to "Calls", "following" to "Following").forEach { (k, l) ->
                    Column(Modifier.width(IntrinsicSize.Max).noRippleClick { tab = k }) {
                        Text(l, color = if (tab == k) Color.White else VibeColors.TextMute, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                        val list = ChatDemo.chats.filter { matches(it.hostId) }
                        if (list.isEmpty()) item { EmptyNote("No chats found") }
                        items(list, key = { it.hostId }) { c ->
                            ChatRow(
                                c,
                                onClick = { onOpenChat(c.hostId) },
                                onLongClick = { if (c.hostId != 0) actionsFor = Demo.host(c.hostId) },
                            )
                        }
                    }
                    "calls" -> items(ChatDemo.calls.filter { matches(it.hostId) }, key = { "${it.hostId}-${it.time}" }) { c ->
                        CallRow(c, onOpenHost, onCall)
                    }
                    else -> items(ChatDemo.following.filter { matches(it) }, key = { it }) { id -> FollowRow(Demo.host(id), onOpenHost, onOpenChat) }
                }
            }
        }

        VibeTabBar(
            selected = 1, unread = Demo.UNREAD,
            onSelect = { if (it == 0) onHome() else if (it == 2) onOpenMe() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    actionsFor?.let { h ->
        ModalBottomSheet(onDismissRequest = { actionsFor = null }, containerColor = CallSheetBg) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                Text(h.name, color = VibeColors.TextDim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                listOf(
                    Triple(CallIcons.User, "Pin chat", false), Triple(CallIcons.BellOff, "Mute notifications", false),
                    Triple(CallIcons.Trash, "Delete chat", false), Triple(CallIcons.Ban, "Block", true), Triple(CallIcons.Flag, "Report", true),
                ).forEach { (icon, label, danger) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            actionsFor = null
                            when (label) {
                                "Block" -> { com.vibe.app.data.SafetyStore.block(h.id); ctx.toast("${h.name} blocked") }
                                "Report" -> onReport(h.id)
                                else -> ctx.toast("$label · ${h.name}")
                            }
                        }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, null, tint = if (danger) VibeColors.Red else Color.White, modifier = Modifier.size(22.dp))
                        Text(label, color = if (danger) VibeColors.Red else Color.White, fontSize = 15.sp, modifier = Modifier.padding(start = 14.dp))
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

@Composable
private fun TeamBadge() {
    Box(Modifier.size(56.dp).clip(CircleShape).border(1.dp, Brush.linearGradient(listOf(VibeColors.Pink, VibeColors.Cyan)), CircleShape), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ghost_home), null, Modifier.size(40.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(c: ChatPreview, onClick: () -> Unit, onLongClick: () -> Unit) {
    val team = c.hostId == 0
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (team) TeamBadge() else PersonAvatar(Demo.host(c.hostId), 56.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (team) "VIBE Team" else Demo.host(c.hostId).name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(c.time, color = if (c.unread > 0) VibeColors.Pink else VibeColors.TextMute, fontSize = 12.sp)
            }
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Preview(c) }
                if (c.unread > 0) Box(
                    Modifier.padding(start = 8.dp).size(20.dp).clip(CircleShape).background(Brush.linearGradient(listOf(VibeColors.Pink, Color(0xFF7A1FD1)))),
                    contentAlignment = Alignment.Center,
                ) { Text("${c.unread}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp, style = centerLineStyle) }
            }
        }
    }
}

@Composable
private fun Preview(c: ChatPreview) {
    val grey = if (c.unread > 0) Color.White else VibeColors.TextDim
    when {
        c.kind == "typing" -> Text("typing…", color = VibeColors.Green, fontSize = 14.sp)
        c.kind == "missed" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(CallIcons.Phone, null, tint = VibeColors.Red, modifier = Modifier.size(14.dp))
            Text("  ${c.text}", color = VibeColors.Red, fontSize = 14.sp, maxLines = 1)
        }
        c.kind == "video" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(VibeIcons.Video, null, tint = VibeColors.TextDim, modifier = Modifier.size(14.dp))
            Text("  ${c.text}", color = grey, fontSize = 14.sp, maxLines = 1)
        }
        c.mine -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✓✓ ", color = if (c.seen) VibeColors.Cyan else VibeColors.TextMute, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(c.text, color = grey, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        else -> Text(c.text, color = grey, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CallRow(c: CallLogItem, onOpenHost: (Int) -> Unit, onCall: (Int, Boolean) -> Unit) {
    val h = Demo.host(c.hostId)
    val missed = c.dir == "missed"
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        PersonAvatar(h, 48.dp, 17)
        Column(Modifier.weight(1f).padding(start = 12.dp).noRippleClick { onOpenHost(h.id) }) {
            Text(h.name, color = if (missed) VibeColors.Red else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    if (c.dir == "out") "↗ " else "↙ ",
                    color = if (c.dir == "out") VibeColors.Green else if (missed) VibeColors.Red else VibeColors.Cyan, fontSize = 12.sp,
                )
                Icon(if (c.video) VibeIcons.Video else CallIcons.Phone, null, tint = VibeColors.TextDim, modifier = Modifier.size(12.dp))
                Text("  ${c.time}", color = VibeColors.TextDim, fontSize = 12.sp)
            }
            Text(c.detail, color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (h.voice) RoundCallButton(CallIcons.Phone, VibeColors.Green) { onCall(h.id, true) }
        if (h.video) { Box(Modifier.width(8.dp)); RoundCallButton(VibeIcons.Video, VibeColors.Pink) { onCall(h.id, false) } }
    }
}

@Composable
private fun RoundCallButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun FollowRow(h: Host, onOpenHost: (Int) -> Unit, onOpenChat: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        PersonAvatar(h, 56.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp).noRippleClick { onOpenHost(h.id) }) {
            Text("${h.name}, ${h.age}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                when (h.status) { HostStatus.ONLINE -> "Online now"; HostStatus.BUSY -> "On a call"; HostStatus.OFFLINE -> "Last seen 2h ago" },
                color = h.status.text, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            Modifier.height(36.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape)
                .clickable { onOpenChat(h.id) }.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Chat", color = Color.White, fontSize = 13.sp) }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp))
}
