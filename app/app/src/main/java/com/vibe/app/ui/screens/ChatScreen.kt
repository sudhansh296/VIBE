package com.vibe.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.ChatDemo
import com.vibe.app.data.ChatMsg
import com.vibe.app.data.Demo
import com.vibe.app.data.Host
import com.vibe.app.data.HostStatus
import com.vibe.app.ui.components.BackButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CallSheetBg
import com.vibe.app.ui.components.GiftSheet
import com.vibe.app.ui.components.VibeIcons
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val meBrush = Brush.linearGradient(listOf(VibeColors.Pink, Color(0xFFC3157A), Color(0xFF7A1FD1)))
private val white70 = Color.White.copy(alpha = 0.7f)

/**
 * One-to-one chat. hostId 0 = official VIBE Team.
 * DEMO paid-chat logic: the first [Demo.FREE_MESSAGES] messages to a host are free, then the host's own message price.
 * In the real app the SERVER counts, charges the coins before saving the message and pays the host; host replies are free.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    hostId: Int,
    onBack: () -> Unit,
    onOpenProfile: (Int) -> Unit,
    onCall: (id: Int, audio: Boolean) -> Unit,
    onReport: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val team = hostId == 0
    val h = remember(hostId) { Demo.host(if (team) 1 else hostId) }

    val initial = remember(hostId) { ChatDemo.messagesFor(hostId) }
    var freeLeft by remember(hostId) {
        mutableIntStateOf(
            if (team) Int.MAX_VALUE
            else maxOf(0, Demo.FREE_MESSAGES - initial.count { (it is ChatMsg.Text && it.me) || (it is ChatMsg.Photo && it.me) }),
        )
    }
    val msgs = remember(hostId) {
        mutableStateListOf<ChatMsg>().apply {
            addAll(initial)
            if (!team) add(ChatMsg.Note(priceNote(freeLeft, h)))
        }
    }
    var balance by remember { mutableIntStateOf(Demo.BALANCE) }
    var input by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var showGifts by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showBlock by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(msgs.size, typing) {
        listState.animateScrollToItem(msgs.size + (if (typing) 1 else 0))
    }

    fun call(audio: Boolean) = when (h.status) {
        HostStatus.BUSY -> ctx.toast("${h.name} is on another call")
        HostStatus.OFFLINE -> ctx.toast("${h.name} is offline")
        HostStatus.ONLINE -> onCall(h.id, audio)
    }

    fun send(raw: String) {
        val t = raw.trim()
        if (t.isEmpty()) return
        var paid = 0
        if (!team) {
            if (freeLeft <= 0) {
                if (balance < h.messagePrice) {
                    msgs.add(ChatMsg.Note("Not enough coins · ${h.messagePrice} needed per message · Recharge"))
                    return
                }
                balance -= h.messagePrice
                paid = h.messagePrice
            } else {
                freeLeft--
            }
        }
        msgs.add(ChatMsg.Text(me = true, text = t, time = "Now", state = 0, paid = paid))
        val idx = msgs.lastIndex
        if (!team && paid == 0 && freeLeft == 0) msgs.add(ChatMsg.Note("Free messages used · each message now costs ${h.messagePrice} coins"))
        input = ""
        scope.launch {
            delay(600)
            (msgs.getOrNull(idx) as? ChatMsg.Text)?.let { msgs[idx] = it.copy(state = 1) }
            if (team || h.status == HostStatus.OFFLINE) return@launch
            delay(700)
            (msgs.getOrNull(idx) as? ChatMsg.Text)?.let { msgs[idx] = it.copy(state = 2) }
            typing = true
            delay(1700)
            typing = false
            msgs.add(ChatMsg.Text(false, listOf("Haha sahi 😂", "Aur batao?", "Call karo na", "❤️").random(), "Now"))
        }
    }

    // system-bar insets are applied per bar below, so the header colour continues behind the status bar
    // and the input bar colour continues behind the navigation bar
    Column(Modifier.fillMaxSize().background(VibeColors.Dark).imePadding()) {
        // header
        Row(
            Modifier.fillMaxWidth().background(VibeColors.Card).statusBarsPadding().padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // plain chevron like the HTML design (no box)
            Box(Modifier.size(40.dp).noRippleClick(onBack), contentAlignment = Alignment.Center) {
                Icon(CallIcons.Chevron, "Back", tint = Color.White, modifier = Modifier.size(26.dp).rotate(90f))
            }
            Row(
                Modifier.weight(1f).padding(start = 4.dp).noRippleClick { if (!team) onOpenProfile(h.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (team) Box(
                    Modifier.size(44.dp).clip(CircleShape).border(1.dp, Brush.linearGradient(listOf(VibeColors.Pink, VibeColors.Cyan)), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.ghost_home), null, Modifier.size(32.dp)) }
                else PersonAvatar(h, 40.dp, 15)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(if (team) "VIBE Team" else h.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    when {
                        typing -> Text("typing…", color = VibeColors.Green, fontSize = 12.sp)
                        team -> Text("Official · replies in ~10 min", color = VibeColors.TextDim, fontSize = 12.sp)
                        h.status == HostStatus.ONLINE -> Text("Online", color = VibeColors.Green, fontSize = 12.sp)
                        h.status == HostStatus.BUSY -> Text("On a call", color = Color(0xFFFDBA74), fontSize = 12.sp)
                        else -> Text("Last seen 2h ago", color = VibeColors.TextDim, fontSize = 12.sp)
                    }
                }
            }
            if (!team && h.voice) HeaderCircle(CallIcons.Phone, VibeColors.Green) { call(true) }
            if (!team && h.video) HeaderCircle(VibeIcons.Video, VibeColors.Pink) { call(false) }
            // three drawn dots (a "⋮" character renders differently on some phones)
            Column(
                Modifier.size(width = 36.dp, height = 40.dp).noRippleClick { showMore = true },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) { repeat(3) { Box(Modifier.size(4.dp).clip(CircleShape).background(Color.White)) } }
        }

        // messages + quick replies (they scroll with the chat)
        LazyColumn(
            state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(msgs.size) { i -> MessageItem(msgs[i], h) }
            if (typing) item { TypingBubble() }
            item { QuickReplies(onPick = { send(it) }) }
        }

        // input bar
        Row(
            Modifier.fillMaxWidth().background(VibeColors.Card).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InputCircle(onClick = { ctx.toast("Photo picker opens here") }) {
                Icon(CallIcons.Photo, "Photo", tint = VibeColors.ChipText, modifier = Modifier.size(20.dp))
            }
            Box(
                Modifier.weight(1f).padding(horizontal = 8.dp).height(44.dp).clip(CircleShape).background(VibeColors.Card)
                    .border(1.5.dp, VibeColors.Line, CircleShape).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (input.isEmpty()) Text("Type a message…", color = VibeColors.TextMute, fontSize = 14.sp)
                BasicTextField(
                    value = input, onValueChange = { input = it.take(500) }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = SolidColor(VibeColors.Pink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send(input) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable { showGifts = true },
                contentAlignment = Alignment.Center,
            ) { Text("🎁", fontSize = 20.sp) }
            Box(
                Modifier.padding(start = 8.dp).size(44.dp).clip(CircleShape).background(VibeBrush.Button).clickable { send(input) },
                contentAlignment = Alignment.Center,
            ) { Icon(CallIcons.Send, "Send", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }

    if (showGifts) GiftSheet(balance, { showGifts = false }) { g, n ->
        val total = g.price * n
        showGifts = false
        if (total > balance) ctx.toast("Not enough coins")
        else {
            balance -= total
            msgs.add(ChatMsg.GiftMsg(true, g.emoji, if (n > 1) "${g.name} ×$n" else g.name, total, "Now"))
        }
    }

    if (showBlock) com.vibe.app.ui.components.BlockConfirmDialog(
        name = h.name,
        onConfirm = { com.vibe.app.data.SafetyStore.block(h.id); showBlock = false; ctx.toast("${h.name} blocked"); onBack() },
        onDismiss = { showBlock = false },
    )

    if (showMore) ModalBottomSheet(onDismissRequest = { showMore = false }, containerColor = CallSheetBg) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 22.dp)) {
                if (team) Box(
                    Modifier.size(48.dp).clip(CircleShape).border(1.dp, Brush.linearGradient(listOf(VibeColors.Pink, VibeColors.Cyan)), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.ghost_home), null, Modifier.size(34.dp)) }
                else PersonAvatar(h, 48.dp, 17)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(if (team) "VIBE Team" else h.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        if (team) "Official account" else when (h.status) { HostStatus.ONLINE -> "Online now"; HostStatus.BUSY -> "On a call"; HostStatus.OFFLINE -> "Offline" },
                        color = VibeColors.TextDim, fontSize = 12.sp,
                    )
                }
                if (!team) Row(
                    Modifier.height(36.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { showMore = false; onOpenProfile(h.id) }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(CallIcons.User, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("  Profile", color = Color.White, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                QuickAction(if (muted) CallIcons.BellOff else CallIcons.Bell, if (muted) "Unmute" else "Mute", muted, Modifier.weight(1f)) {
                    muted = !muted; ctx.toast(if (muted) "Notifications muted" else "Notifications on")
                }
                QuickAction(CallIcons.Search, "Search", false, Modifier.weight(1f)) { showMore = false; ctx.toast("Search in this chat") }
                QuickAction(CallIcons.Trash, "Clear", false, Modifier.weight(1f)) {
                    showMore = false; msgs.clear(); ctx.toast("Chat cleared")
                }
            }
            if (!team) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                DangerRow(CallIcons.Ban, "Block ${h.name}", "They can't message or call you") { showMore = false; showBlock = true }
                DangerRow(CallIcons.Flag, "Report", "Tell our safety team what went wrong") { showMore = false; onReport(h.id) }
            }
        }
    }
}

private fun priceNote(free: Int, h: Host): String =
    if (free > 0) "$free free message${if (free > 1) "s" else ""} left · then ${h.messagePrice} coins per message"
    else "${h.messagePrice} coins per message · set by ${h.name}"

@Composable
private fun HeaderCircle(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 6.dp).size(36.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp)) }
}

/** Round button in the input bar; [content] draws the icon. */
@Composable
private fun InputCircle(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.noRippleClick(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(if (on) Color.White else Color.Transparent)
                .border(1.dp, if (on) Color.White else Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = if (on) VibeColors.Dark else Color(0xFFE5E7EB), modifier = Modifier.size(22.dp)) }
        Text(label.uppercase(), color = VibeColors.TextDim, fontSize = 10.sp, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 8.dp))
    }
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

@Composable
private fun MessageItem(m: ChatMsg, h: Host) {
    when (m) {
        is ChatMsg.Text -> TextBubble(m)
        is ChatMsg.Note -> CenterPill(m.text, VibeColors.ChipText, 11)
        is ChatMsg.Day -> CenterPill(m.text, VibeColors.TextMute, 11)
        ChatMsg.Safety -> Text(
            "Never share OTPs, bank details or pay anyone outside VIBE. Report anyone who asks.",
            color = VibeColors.TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VibeColors.Card)
                .border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        )
        is ChatMsg.CallLine -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val tint = if (m.missed) VibeColors.Red else VibeColors.ChipText
                Icon(if (m.voice) CallIcons.Phone else VibeIcons.Video, null, tint = tint, modifier = Modifier.size(14.dp))
                Text("  ${m.text}", color = tint, fontSize = 12.sp)
                Text("  · ${m.time}", color = VibeColors.TextMute, fontSize = 12.sp)
            }
        }
        is ChatMsg.GiftMsg -> Box(Modifier.fillMaxWidth(), contentAlignment = if (m.me) Alignment.CenterEnd else Alignment.CenterStart) {
            Row(
                Modifier.clip(RoundedCornerShape(18.dp)).background(VibeColors.Card).border(1.dp, VibeColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(m.emoji, fontSize = 30.sp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("${if (m.me) "You sent" else "${h.name} sent"} a ${m.name}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${m.price} coins · ${m.time}", color = VibeColors.Gold, fontSize = 11.sp)
                }
            }
        }
        is ChatMsg.Photo -> Box(Modifier.fillMaxWidth(), contentAlignment = if (m.me) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(
                Modifier.size(190.dp, 224.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(h.c1, h.c2))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CallIcons.Photo, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                Text(m.time, color = white70, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
            }
        }
    }
}

@Composable
private fun TextBubble(m: ChatMsg.Text) {
    val shape = if (m.me) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.me) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 280.dp).clip(shape)
                .then(if (m.me) Modifier.background(meBrush) else Modifier.background(VibeColors.Card).border(1.dp, VibeColors.Line, shape))
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            // time and ticks follow the text on the same line (like the HTML design), so bubbles stay compact
            Text(
                buildAnnotatedString {
                    append(m.text)
                    withStyle(SpanStyle(fontSize = 10.sp, color = if (m.me) white70 else VibeColors.TextMute)) {
                        append("  ")
                        if (m.paid > 0) append("−${m.paid} coins · ")
                        append(m.time)
                    }
                    if (m.me) withStyle(SpanStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (m.state == 2) VibeColors.Cyan else white70)) {
                        append(if (m.state == 0) " ✓" else " ✓✓")
                    }
                },
                color = Color.White, fontSize = 14.sp, lineHeight = 19.sp,
            )
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

/** One line of chips under the last message. */
@Composable
private fun QuickReplies(onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        Chip("👋 Hi", null, VibeColors.ChipText) { onPick("👋 Hi") }
        Chip("How are you?", null, VibeColors.ChipText) { onPick("How are you?") }
        Chip("Call?", CallIcons.Phone, VibeColors.Green) { onPick("Call?") }
        Chip("Video?", VibeIcons.Video, VibeColors.Pink) { onPick("Video call?") }
        Chip("😂", null, VibeColors.ChipText) { onPick("😂") }
    }
}

@Composable
private fun Chip(label: String, icon: ImageVector?, iconTint: Color, onClick: () -> Unit) {
    Row(
        Modifier.height(30.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = iconTint, modifier = Modifier.size(12.dp).padding(end = 0.dp))
        Text(if (icon != null) " $label" else label, color = VibeColors.ChipText, fontSize = 10.5.sp, maxLines = 1)
    }
}
