package com.vibe.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.ChatMessage
import com.vibe.app.data.ChatRepo
import com.vibe.app.data.Demo
import com.vibe.app.data.Host
import com.vibe.app.data.HostRepo
import com.vibe.app.data.HostStatus
import com.vibe.app.data.LiveEvent
import com.vibe.app.data.Realtime
import com.vibe.app.data.api.ApiException
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GiftThumb
import com.vibe.app.ui.components.VibeIcons
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val meBrush = Brush.linearGradient(listOf(VibeColors.Pink, Color(0xFFFF7AB0)))
private val white70 = Color.White.copy(alpha = 0.7f)

private fun epoch(iso: String): Long = try {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(iso.take(19))?.time ?: System.currentTimeMillis()
} catch (e: Exception) { System.currentTimeMillis() }
private fun clock(iso: String) = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(java.util.Date(epoch(iso))).replace("AM", "am").replace("PM", "pm")
private fun dayOf(iso: String): String {
    val ms = epoch(iso); val off = TimeZone.getDefault().getOffset(ms)
    val day = 86_400_000L
    val d = (ms + off) / day; val today = (System.currentTimeMillis() + off) / day
    return when (today - d) { 0L -> "Today"; 1L -> "Yesterday"; else -> SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(java.util.Date(ms)) }
}

/**
 * One chat with a host (or, for a host, with a user). Messages come from the server and arrive live over Socket.IO.
 * The server decides what is free and what costs coins (the user pays after the free messages; a host's messages are free).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    hostId: Int,
    onBack: () -> Unit,
    onOpenProfile: (Int) -> Unit,
    onCall: (id: Int) -> Unit,
    onReport: (Int) -> Unit,
    onWallet: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val h = HostRepo.cache[hostId] ?: Demo.host(hostId)
    val iAmHost = com.vibe.app.data.api.AuthStore.me?.optString("role") == "host"
    val msgs = remember(hostId) { mutableStateListOf<ChatMessage>() }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf<String?>(null) }
    var more by remember { mutableStateOf(false) }
    var loadingOlder by remember { mutableStateOf(false) }
    var freeLeft by remember { mutableStateOf<Int?>(null) }
    var price by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var showGifts by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showBlock by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // typing… shown for 4 seconds after the other person's last key press
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val typing = (ChatRepo.typing[hostId] ?: 0L) > now - 4000

    suspend fun load() {
        loading = true; failed = null
        try {
            val p = ChatRepo.messages(hostId)
            msgs.clear(); msgs.addAll(p.messages); more = p.more; freeLeft = p.freeLeft; price = p.messagePrice
            ChatRepo.markRead(hostId)
        } catch (e: ApiException) { failed = e.message }
        loading = false
    }
    LaunchedEffect(hostId) { load() }
    // jump to the newest message once the list is on screen (scrolling before it is shown would wait for ever)
    LaunchedEffect(loading) { if (!loading && failed == null) listState.scrollToItem(msgs.size + 1) }
    DisposableEffect(hostId) {
        ChatRepo.openPeer = hostId
        // a user opening a host's chat: the host gets a banner at once (only users -> hosts; the server checks it again)
        val toHost = h.verified && !iAmHost
        if (toHost) Realtime.chatOpened(hostId)
        onDispose {
            if (ChatRepo.openPeer == hostId) ChatRepo.openPeer = null
            if (toHost) Realtime.chatClosed()
        }
    }
    // live: new messages for this chat, and "seen" ticks
    LaunchedEffect(hostId) {
        Realtime.events.collect { e ->
            when {
                e is LiveEvent.ChatMsg && e.peerId == hostId -> {
                    val m = e.message
                    val i = msgs.indexOfFirst { it.id == m.id || (m.clientId != null && it.clientId == m.clientId) }
                    if (i >= 0) msgs[i] = m else { msgs.add(m); listState.animateScrollToItem(msgs.size + 1) }
                    if (!m.mine) ChatRepo.markRead(hostId)
                }
                e is LiveEvent.ChatRead && e.peerId == hostId -> msgs.indices.forEach { i -> if (msgs[i].mine && !msgs[i].read) msgs[i] = msgs[i].copy(read = true) }
            }
        }
    }
    // older messages when scrolled to the top
    LaunchedEffect(listState, more) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { first ->
            if (first == 0 && more && !loadingOlder && msgs.isNotEmpty()) {
                loadingOlder = true
                runCatching { ChatRepo.messages(hostId, before = msgs.first { it.id > 0 }.id) }.onSuccess { p ->
                    msgs.addAll(0, p.messages); more = p.more
                    listState.scrollToItem(p.messages.size)
                }
                loadingOlder = false
            }
        }
    }

    fun call() = when (h.status) {
        HostStatus.BUSY -> ctx.toast("${h.name} is on another call")
        HostStatus.OFFLINE -> ctx.toast("${h.name} is offline")
        HostStatus.ONLINE -> onCall(h.id)
    }

    fun send(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) return
        input = ""; note = null
        val clientId = "m" + java.util.UUID.randomUUID().toString().take(20)
        val temp = ChatMessage(-System.nanoTime(), true, "text", t, null, 1, 0, SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(java.util.Date()), false, clientId, sending = true)
        msgs.add(temp)
        scope.launch {
            listState.animateScrollToItem(msgs.size + 1)
            try {
                val r = ChatRepo.send(hostId, t, clientId)
                val i = msgs.indexOfFirst { it.clientId == clientId }
                if (i >= 0) msgs[i] = r.message
                freeLeft = r.freeLeft
            } catch (e: ApiException) {
                val i = msgs.indexOfFirst { it.clientId == clientId }
                if (e.code == "not_enough_coins") {
                    if (i >= 0) msgs.removeAt(i)
                    input = t
                    note = "Not enough coins · $price coins per message · Recharge"
                } else {
                    if (i >= 0) msgs[i] = msgs[i].copy(sending = false, failed = true)
                    ctx.toast(e.message)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(VibeColors.Dark).imePadding()) {
        // header
        Row(
            Modifier.fillMaxWidth().background(VibeColors.Card).statusBarsPadding().padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).noRippleClick(onBack), contentAlignment = Alignment.Center) {
                Icon(CallIcons.Chevron, "Back", tint = VibeColors.Ink, modifier = Modifier.size(26.dp).rotate(90f))
            }
            Row(Modifier.weight(1f).padding(start = 4.dp).noRippleClick { onOpenProfile(h.id) }, verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(h, 40.dp, 15)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(h.name, color = VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    when {
                        typing -> Text("typing…", color = VibeColors.Green, fontSize = 12.sp)
                        !h.verified -> Text(h.city, color = VibeColors.TextDim, fontSize = 12.sp)       // a user (hosts see users)
                        h.status == HostStatus.ONLINE -> Text("Online", color = VibeColors.Green, fontSize = 12.sp)
                        h.status == HostStatus.BUSY -> Text("On a call", color = Color(0xFFFDBA74), fontSize = 12.sp)
                        else -> Text("Offline", color = VibeColors.TextDim, fontSize = 12.sp)
                    }
                }
            }
            if (h.verified && !iAmHost) HeaderCircle(VibeIcons.Video, VibeColors.Pink) { call() }
            // a host may call the user too (the user pays the host's rate); the server says if they are offline or can't pay
            else if (iAmHost && !h.verified) HeaderCircle(VibeIcons.Video, VibeColors.Pink) { onCall(h.id) }
            Column(
                Modifier.size(width = 36.dp, height = 40.dp).noRippleClick { showMore = true },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) { repeat(3) { Box(Modifier.size(4.dp).clip(CircleShape).background(VibeColors.Ink)) } }
        }

        if (loading && msgs.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
        } else if (failed != null) Box(Modifier.weight(1f).fillMaxWidth().clickable { scope.launch { load() } }, contentAlignment = Alignment.Center) {
            Text("${failed}\nTap to try again", color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center)
        } else LazyColumn(
            state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SafetyNote() }
            items(msgs.size, key = { msgs[it].clientId ?: msgs[it].id.toString() }) { i ->
                val m = msgs[i]
                val day = dayOf(m.at)
                if (i == 0 || dayOf(msgs[i - 1].at) != day) Box(Modifier.padding(bottom = 8.dp)) { CenterPill(day, VibeColors.TextMute, 11) }
                when {
                    m.kind == "gift" && m.gift != null -> GiftBubble(m, h)
                    m.kind == "call" -> CallLine(m, iAmHost)
                    else -> TextBubble(m) { if (m.failed) { msgs.remove(m); send(m.text.orEmpty()) } }
                }
            }
            if (typing) item { TypingBubble() }
            item {
                val n = note ?: when {
                    iAmHost || freeLeft == null -> null
                    freeLeft!! > 0 -> "$freeLeft free message${if (freeLeft!! > 1) "s" else ""} left · then $price coins per message"
                    else -> "$price coins per message · set by ${h.name}"
                }
                if (n != null) CenterPill(n, if (note != null) VibeColors.Red else VibeColors.ChipText, 11)
            }
            if (msgs.isEmpty()) item { QuickReplies(onPick = { send(it) }) }
        }

        // input bar
        Row(
            Modifier.fillMaxWidth().background(VibeColors.Card).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.weight(1f).padding(end = 8.dp).height(44.dp).clip(CircleShape).background(VibeColors.Card)
                    .border(1.5.dp, VibeColors.Line, CircleShape).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (input.isEmpty()) Text("Type a message…", color = VibeColors.TextMute, fontSize = 14.sp)
                BasicTextField(
                    value = input, onValueChange = { input = it.take(1000); if (it.isNotEmpty()) ChatRepo.sendTyping(hostId) }, singleLine = true,
                    textStyle = TextStyle(color = VibeColors.Ink, fontSize = 14.sp), cursorBrush = SolidColor(VibeColors.Pink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send(input) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // only users send gifts (to hosts)
            if (!iAmHost) Box(
                Modifier.size(44.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable { showGifts = true },
                contentAlignment = Alignment.Center,
            ) { Text("🎁", fontSize = 20.sp) }
            Box(
                Modifier.padding(start = 8.dp).size(44.dp).clip(CircleShape).background(VibeBrush.Button).clickable { send(input) },
                contentAlignment = Alignment.Center,
            ) { Icon(CallIcons.Send, "Send", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }

    // the gift lands in this chat through the live message; its animation plays over the screen (GiftOverlay)
    if (showGifts) com.vibe.app.ui.components.GiftSheet(h.id, "chat", onDismiss = { showGifts = false }, onNeedCoins = { showGifts = false; onWallet() })

    if (showBlock) com.vibe.app.ui.components.BlockConfirmDialog(
        name = h.name,
        onConfirm = { com.vibe.app.data.SafetyStore.block(h.id); showBlock = false; ctx.toast("${h.name} blocked"); onBack() },
        onDismiss = { showBlock = false },
    )

    if (showMore) ModalBottomSheet(onDismissRequest = { showMore = false }, containerColor = VibeColors.Card) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                PersonAvatar(h, 48.dp, 17)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(h.name, color = VibeColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    if (h.verified) Text(when (h.status) { HostStatus.ONLINE -> "Online now"; HostStatus.BUSY -> "On a call"; HostStatus.OFFLINE -> "Offline" }, color = VibeColors.TextDim, fontSize = 12.sp)
                }
                Row(
                    Modifier.height(36.dp).clip(CircleShape).border(1.dp, VibeColors.Ink.copy(alpha = 0.2f), CircleShape)
                        .clickable { showMore = false; onOpenProfile(h.id) }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(CallIcons.User, null, tint = VibeColors.Ink, modifier = Modifier.size(16.dp))
                    Text("  Profile", color = VibeColors.Ink, fontSize = 12.sp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.1f)))
            DangerRow(CallIcons.Ban, "Block ${h.name}", "They can't message or call you") { showMore = false; showBlock = true }
            DangerRow(CallIcons.Flag, "Report", "Tell our safety team what went wrong") { showMore = false; onReport(h.id) }
        }
    }
}

@Composable
private fun SafetyNote() {
    Text(
        "Never share OTPs, bank details or pay anyone outside VIBE. Report anyone who asks.",
        color = VibeColors.TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VibeColors.Card)
            .border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun HeaderCircle(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 6.dp).size(36.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun DangerRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VibeColors.Red, modifier = Modifier.size(22.dp))
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, color = VibeColors.Red, fontSize = 15.sp)
            Text(sub, color = VibeColors.TextMute, fontSize = 12.sp)
        }
    }
}

/** "Missed video call" / "Video call · 06:24" in the middle of the chat (the server writes these when a call ends). */
@Composable
private fun CallLine(m: ChatMessage, iAmHost: Boolean) {
    val secs = m.text?.toIntOrNull() ?: 0
    val iRang = m.mine   // calls go both ways: the line belongs to whoever rang
    val text = when {
        secs > 0 -> "Video call · %02d:%02d".format(secs / 60, secs % 60)
        m.text == "declined" -> if (iRang) "Video call declined" else "You declined a video call"
        m.text == "cancelled" -> if (iRang) "Video call cancelled" else "Missed video call"
        else -> if (iRang) "No answer" else "Missed video call"
    }
    val missed = secs == 0 && (!iRang || m.text == "missed")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tint = if (missed) VibeColors.Red else VibeColors.ChipText
            Icon(VibeIcons.Video, null, tint = tint, modifier = Modifier.size(14.dp))
            Text("  $text", color = tint, fontSize = 12.sp)
            Text("  · ${clock(m.at)}", color = VibeColors.TextMute, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GiftBubble(m: ChatMessage, h: Host) {
    val g = m.gift ?: return
    val qty = if (m.quantity > 1) "${m.quantity}× " else "a "
    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Row(
            Modifier.clip(RoundedCornerShape(18.dp)).background(VibeColors.Card).border(1.dp, VibeColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GiftThumb(g, 36.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(if (m.mine) "You sent $qty${g.name}" else "${h.name} sent you $qty${g.name}", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${"%,d".format(m.coins)} coins · ${clock(m.at)}", color = VibeColors.Gold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TextBubble(m: ChatMessage, onRetry: () -> Unit) {
    val shape = if (m.mine) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start) {
            Column(
                Modifier.widthIn(max = 280.dp).clip(shape)
                    .then(if (m.mine) Modifier.background(meBrush) else Modifier.background(VibeColors.Card).border(1.dp, VibeColors.Line, shape))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                // time, coins and ticks follow the text on the same line, so bubbles stay compact
                Text(
                    buildAnnotatedString {
                        append(m.text.orEmpty())
                        withStyle(SpanStyle(fontSize = 10.sp, color = if (m.mine) white70 else VibeColors.TextMute)) {
                            append("  ")
                            if (m.mine && m.coins > 0) append("−${m.coins} coins · ")
                            append(clock(m.at))
                        }
                        // on the pink bubble: seen = bright white double tick, sent = soft single tick
                        if (m.mine) withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (m.read) Color.White else white70)) {
                            append(when { m.sending -> " ◷"; m.failed -> " !"; m.read -> " ✓✓"; else -> " ✓" })
                        }
                    },
                    color = if (m.mine) Color.White else VibeColors.Ink, fontSize = 14.sp, lineHeight = 19.sp,
                )
            }
            if (m.failed) Text("Not sent · tap to try again", color = VibeColors.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp).clickable(onClick = onRetry))
        }
    }
}

@Composable
private fun CenterPill(text: String, color: Color, size: Int) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text, color = color, fontSize = size.sp, textAlign = TextAlign.Center,
            modifier = Modifier.clip(CircleShape).background(VibeColors.Card.copy(alpha = 0.8f)).border(1.dp, VibeColors.Line, CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun TypingBubble() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(
            "• • •", color = VibeColors.TextDim, fontSize = 16.sp,
            modifier = Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(VibeColors.Card)
                .border(1.dp, VibeColors.Line, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** Ice-breakers for an empty chat. */
@Composable
private fun QuickReplies(onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        Chip("👋 Hi", null, VibeColors.ChipText) { onPick("👋 Hi") }
        Chip("How are you?", null, VibeColors.ChipText) { onPick("How are you?") }
        Chip("Video?", VibeIcons.Video, VibeColors.Pink) { onPick("Video call?") }
        Chip("😊", null, VibeColors.ChipText) { onPick("😊") }
    }
}

@Composable
private fun Chip(label: String, icon: ImageVector?, iconTint: Color, onClick: () -> Unit) {
    Row(
        Modifier.height(30.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = iconTint, modifier = Modifier.size(12.dp))
        Text(if (icon != null) " $label" else label, color = VibeColors.ChipText, fontSize = 10.5.sp, maxLines = 1)
    }
}
