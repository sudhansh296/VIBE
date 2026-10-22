package com.vibe.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.Notice
import com.vibe.app.data.NotifRepo
import com.vibe.app.ui.components.BackButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Removes the extra font padding above/below text so small numbers sit exactly in the middle of their badge. */
internal val centerLineStyle = TextStyle(
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both),
)

private val filters = listOf("all" to "All", "calls" to "Calls", "social" to "Social", "wallet" to "Wallet")

/** Which tab a server kind belongs to. */
private fun groupOf(kind: String) = when (kind) {
    "missed_call" -> "calls"
    "gift", "follower" -> "social"
    else -> "wallet"   // coins, vip, check-in, level, invite, withdrawals
}

private fun toneOf(kind: String): Color = when (kind) {
    "missed_call", "withdraw_rejected" -> VibeColors.Red
    "gift", "follower" -> VibeColors.Pink
    "withdraw_paid", "level", "invite" -> VibeColors.Green
    else -> VibeColors.Gold
}

private fun iconOf(kind: String): ImageVector = when (kind) {
    "missed_call" -> CallIcons.Missed
    "follower" -> CallIcons.Follow
    "gift" -> CallIcons.Gift
    "coins", "checkin", "invite", "level", "withdraw_paid", "withdraw_rejected" -> CallIcons.Coin
    else -> CallIcons.Bell
}

private val utc = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
private fun millis(at: String) = runCatching { utc.parse(at.take(19))!!.time }.getOrDefault(0L)

/** "Today" / "Yesterday" / "Earlier" for the group header. */
private fun groupTitle(at: String): String {
    val d = millis(at)
    val today = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
    return when { d >= today -> "Today"; d >= today - 86_400_000 -> "Yesterday"; else -> "Earlier" }
}

private fun timeOf(at: String): String {
    val d = millis(at)
    val mins = (System.currentTimeMillis() - d) / 60_000
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "$mins min ago"
        mins < 24 * 60 -> "${mins / 60} h ago"
        else -> java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.ENGLISH).format(java.util.Date(d)).replace("AM", "am").replace("PM", "pm")
    }
}

/**
 * Notifications from the server (missed calls, gifts, followers, coins, VIP, check-in, levels, invites, withdrawals).
 * Opening the list marks everything read; swipe left removes one. Push notifications (app closed) come in Phase 7.
 */
@Composable
fun NotificationsScreen(onBack: () -> Unit, onCall: (id: Int) -> Unit, onChat: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<Notice>() }
    var filter by remember { mutableStateOf("all") }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf<String?>(null) }
    var more by remember { mutableStateOf(false) }
    var newOnOpen by remember { mutableStateOf(0) }

    suspend fun load() {
        loading = true
        runCatching { NotifRepo.page() }.onSuccess { (list, m) ->
            items.clear(); items.addAll(list); more = m; failed = null
            newOnOpen = list.count { !it.read }
        }.onFailure { failed = it.message ?: "Could not load" }
        loading = false
        NotifRepo.readAll()   // seen now: the dot on the bell goes away
    }
    LaunchedEffect(Unit) { load() }
    // a new one while the list is open
    LaunchedEffect(Unit) {
        com.vibe.app.data.Realtime.events.collect { e -> if (e is com.vibe.app.data.LiveEvent.Notified) { items.add(0, e.notice); NotifRepo.readAll() } }
    }

    val shown = items.filter { filter == "all" || groupOf(it.kind) == filter }
    val rows: List<Any> = buildList {
        var g = ""
        shown.forEach { n -> val t = groupTitle(n.at); if (t != g) { g = t; add(g) }; add(n) }
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 260.dp, Modifier.align(Alignment.TopEnd).offset(80.dp, (-90).dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Notifications", color = VibeColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (newOnOpen > 0) "$newOnOpen new" else "You are all caught up", color = VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 0.4.sp)
                }
            }

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                filters.forEach { (k, label) ->
                    val on = k == filter
                    val count = items.count { !it.read && (k == "all" || groupOf(it.kind) == k) }
                    Column(Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { filter = k }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label.uppercase(), color = if (on) VibeColors.Ink else VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 1.5.sp, lineHeight = 11.sp, style = centerLineStyle)
                            if (count > 0) Box(
                                Modifier.padding(start = 6.dp).height(17.dp).widthIn(min = 17.dp).clip(CircleShape).background(VibeColors.Pink).padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp, style = centerLineStyle) }
                        }
                        Box(Modifier.padding(top = 12.dp).height(2.dp).fillMaxWidth().then(if (on) Modifier.background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))) else Modifier))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.1f)))

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp)) {
                failed?.let { msg ->
                    item(key = "err") { Text("$msg · tap to retry", color = VibeColors.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp).clickable { scope.launch { load() } }) }
                }
                if (!loading && rows.isEmpty() && failed == null) item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(R.drawable.ghost_empty), null, Modifier.size(150.dp))
                        Text("Nothing here yet", color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 8.dp))
                        Text("Missed calls, gifts, followers and coins will show up here.", color = VibeColors.TextMute, fontSize = 12.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp))
                    }
                }
                items(rows, key = { if (it is Notice) "n${it.id}" else "g$it" }) { r ->
                    if (r is Notice) SwipeRow(onDismiss = { items.remove(r); scope.launch { NotifRepo.delete(r.id) } }) {
                        NoticeItem(r, onAction = { peer -> if (r.kind == "missed_call") onCall(peer) else onChat(peer) })
                    } else {
                        Text(r.toString().uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.8.sp, modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp))
                    }
                }
                if (more) item(key = "more") {
                    Text(
                        "Show older", color = VibeColors.Pink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).clickable {
                            scope.launch { runCatching { NotifRepo.page(items.lastOrNull()?.id) }.onSuccess { (l, m) -> items.addAll(l); more = m } }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeItem(n: Notice, onAction: (peerId: Int) -> Unit) {
    val tone = toneOf(n.kind)
    val shape = RoundedCornerShape(18.dp)
    // a missed call can be returned (a user calls the host back; a host calls the user)
    val action = if (n.kind == "missed_call" && n.peerId != null) "Call back" else null
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (!n.read) Color(0xFFFFF0F6) else Color(0xFFFFFFFF))
            .border(1.dp, if (!n.read) VibeColors.Pink.copy(alpha = 0.22f) else VibeColors.Ink.copy(alpha = 0.08f), shape)
            .drawBehind {
                if (!n.read) drawRoundRect(
                    brush = Brush.verticalGradient(listOf(VibeColors.Pink, VibeColors.Cyan)),
                    topLeft = Offset(0f, 16.dp.toPx()), size = Size(3.dp.toPx(), size.height - 32.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(tone.copy(alpha = 0.07f)).border(1.dp, tone.copy(alpha = 0.33f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(iconOf(n.kind), null, tint = tone, modifier = Modifier.size(21.dp)) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(n.title, color = VibeColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
            n.body?.let { Text(it, color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 17.sp) }
            Text(timeOf(n.at), color = VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (action != null && n.peerId != null) Box(
            Modifier.height(32.dp).clip(CircleShape).background(VibeColors.Pink).clickable { onAction(n.peerId) }.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text(action, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
    }
}

/** Drag a row to the left past ~90dp to remove it. */
@Composable
private fun SwipeRow(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val x = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 90.dp.toPx() }
    Box(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp))) {
        Box(Modifier.matchParentSize().background(VibeColors.Red.copy(alpha = 0.14f)), contentAlignment = Alignment.CenterEnd) {
            Icon(CallIcons.Trash, null, tint = VibeColors.Red, modifier = Modifier.padding(end = 24.dp).size(22.dp))
        }
        Box(
            Modifier.offset { IntOffset(x.value.roundToInt(), 0) }.pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (x.value < -threshold) { x.animateTo(-2000f, tween(200)); onDismiss() } else x.animateTo(0f, tween(200))
                        }
                    },
                    onDragCancel = { scope.launch { x.animateTo(0f, tween(200)) } },
                ) { change, dx ->
                    change.consume()
                    scope.launch { x.snapTo((x.value + dx).coerceAtMost(0f)) }
                }
            },
        ) { content() }
    }
}
