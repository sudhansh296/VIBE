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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.platform.LocalContext
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
import com.vibe.app.data.AppNotification
import com.vibe.app.data.ChatDemo
import com.vibe.app.data.Demo
import com.vibe.app.data.boldText
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.BackButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.neonBorder
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Removes the extra font padding above/below text so small numbers sit exactly in the middle of their badge. */
internal val centerLineStyle = TextStyle(
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both),
)

private val filters = listOf("all" to "All", "calls" to "Calls", "social" to "Social", "wallet" to "Wallet", "system" to "VIBE")

/** Notifications. DEMO list; the real list comes from the server (and also arrives as push notifications). */
@Composable
fun NotificationsScreen(onBack: () -> Unit, onCall: (id: Int, audio: Boolean) -> Unit, onChat: (Int) -> Unit) {
    val ctx = LocalContext.current
    val items = remember { mutableStateListOf<AppNotification>().apply { addAll(ChatDemo.notifications) } }
    var filter by remember { mutableStateOf("all") }
    var offerOn by remember { mutableStateOf(true) }
    // last removed notification (index + item); stays undoable for 5 seconds
    var undo by remember { mutableStateOf<Pair<Int, AppNotification>?>(null) }
    LaunchedEffect(undo) { if (undo != null) { delay(5000); undo = null } }
    fun removeWithUndo(id: Int) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) { undo = i to items[i]; items.removeAt(i) }
    }

    fun unreadOf(f: String) = items.count { it.unread && (f == "all" || it.type == f) }
    fun update(id: Int, change: (AppNotification) -> AppNotification) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) items[i] = change(items[i])
    }
    fun act(n: AppNotification) {
        update(n.id) { it.copy(unread = false) }
        val parts = n.action?.split(":") ?: return
        when (parts[0]) {
            "call" -> onCall(parts[1].toInt(), parts.getOrNull(2) == "audio")
            "chat" -> onChat(parts[1].toInt())
            "follow" -> { update(n.id) { it.copy(action = null) }; ctx.toast("Following back") }
        }
    }

    val total = unreadOf("all")
    val showOffer = offerOn && (filter == "all" || filter == "system")
    val filtered = items.filter { filter == "all" || it.type == filter }
    val rows: List<Any> = buildList {
        var g = ""
        filtered.forEach { n -> if (n.group != g) { g = n.group; add(g) }; add(n) }
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 260.dp, Modifier.align(Alignment.TopEnd).offset(80.dp, (-90).dp))

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Notifications", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (total > 0) "$total new" else "You are all caught up", color = VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 0.4.sp)
                }
                Row(
                    Modifier.alpha(if (total > 0) 1f else 0.35f).height(36.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable(enabled = total > 0) { items.indices.forEach { i -> items[i] = items[i].copy(unread = false) }; ctx.toast("All caught up") }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(CallIcons.Check, null, tint = Color(0xFFE5E7EB), modifier = Modifier.size(15.dp))
                    Text("  Read all", color = Color(0xFFE5E7EB), fontSize = 11.sp, letterSpacing = 0.4.sp)
                }
            }

            // filter tabs with unread counts
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                filters.forEach { (k, label) ->
                    val on = k == filter
                    val count = unreadOf(k)
                    Column(Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { filter = k }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label.uppercase(), color = if (on) Color.White else VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 1.5.sp,
                                lineHeight = 11.sp, style = centerLineStyle,
                            )
                            if (count > 0) Box(
                                Modifier.padding(start = 6.dp).height(17.dp).widthIn(min = 17.dp).clip(CircleShape).background(VibeColors.Pink).padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp, style = centerLineStyle) }
                        }
                        Box(
                            Modifier.padding(top = 12.dp).height(2.dp).fillMaxWidth()
                                .then(if (on) Modifier.background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))) else Modifier),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp)) {
                if (showOffer) item(key = "offer") { OfferCard(onClose = { offerOn = false }, onView = { ctx.toast("Level screen comes in Batch 5") }) }
                if (rows.isEmpty() && !showOffer) item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(R.drawable.ghost_empty), null, Modifier.size(150.dp))
                        Text("Nothing here yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 8.dp))
                        Text("New calls, follows, gifts and offers will show up here.", color = VibeColors.TextMute, fontSize = 12.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp))
                    }
                }
                items(rows, key = { if (it is AppNotification) "n${it.id}" else "g$it" }) { r ->
                    if (r is AppNotification) SwipeRow(onDismiss = { removeWithUndo(r.id) }) {
                        NotificationItem(r, onTap = { update(r.id) { it.copy(unread = false) } }, onAction = { act(r) })
                    } else {
                        Text(r.toString().uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.8.sp, modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp))
                    }
                }
            }
        }

        // "Notification removed · UNDO" bar, visible for 5 seconds
        undo?.let { (index, removed) ->
            val bar = RoundedCornerShape(16.dp)
            Row(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth().clip(bar)
                    .background(Color(0xFF1A1A24)).border(1.dp, Color.White.copy(alpha = 0.12f), bar).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notification removed", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    "UNDO", color = VibeColors.Pink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                    modifier = Modifier.clickable { items.add(index.coerceIn(0, items.size), removed); undo = null }.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private fun toneColor(t: String): Color = when (t) {
    "missed" -> VibeColors.Red
    "call", "online" -> VibeColors.Green
    "social" -> VibeColors.Pink
    "wallet" -> VibeColors.Gold
    else -> VibeColors.Cyan
}

private fun notifIcon(k: String): ImageVector = when (k) {
    "missed" -> CallIcons.Missed
    "follow" -> CallIcons.Follow
    "gift" -> CallIcons.Gift
    "coin" -> CallIcons.Coin
    "check" -> CallIcons.Check
    "news" -> CallIcons.News
    "shield" -> CallIcons.Shield
    "online" -> CallIcons.Online
    "video" -> CallIcons.Camera
    else -> CallIcons.Bell
}

@Composable
private fun NotificationItem(n: AppNotification, onTap: () -> Unit, onAction: () -> Unit) {
    val tone = toneColor(n.tone)
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (n.unread) Color(0xFF11101A) else Color(0xFF0D0D14))
            .border(1.dp, if (n.unread) VibeColors.Pink.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f), shape)
            .drawBehind {
                // thin pink→cyan strip on the left edge marks an unread item
                if (n.unread) drawRoundRect(
                    brush = Brush.verticalGradient(listOf(VibeColors.Pink, VibeColors.Cyan)),
                    topLeft = Offset(0f, 16.dp.toPx()), size = Size(3.dp.toPx(), size.height - 32.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            .clickable(onClick = onTap).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val host = n.hostId?.let { Demo.host(it) }
        if (host != null) Box(Modifier.size(46.dp)) {
            Avatar(host, Modifier.size(46.dp).clip(CircleShape), 16.sp)
            Box(
                Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(19.dp).clip(CircleShape).background(VibeColors.Dark)
                    .border(1.dp, tone.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(notifIcon(n.icon), null, tint = tone, modifier = Modifier.size(10.dp)) }
        } else Box(
            Modifier.size(46.dp).clip(CircleShape).background(tone.copy(alpha = 0.07f)).border(1.dp, tone.copy(alpha = 0.33f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(notifIcon(n.icon), null, tint = tone, modifier = Modifier.size(21.dp)) }

        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(boldText(n.text), color = Color(0xFFD1D5DB), fontSize = 13.sp, lineHeight = 18.sp)
            Text(n.time, color = VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (n.action != null) Box(
            Modifier.height(32.dp).clip(CircleShape).background(if (n.primary) Color.White else Color.Transparent)
                .border(1.dp, if (n.primary) Color.White else Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable(onClick = onAction).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text(n.actionLabel, color = if (n.primary) VibeColors.Dark else Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
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

@Composable
private fun OfferCard(onClose: () -> Unit, onView: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp).neonBorder(20.dp).background(Color(0xFF0D0D14))) {
        Column(Modifier.padding(16.dp)) {
            Text("WEEKEND OFFER", color = VibeColors.Pink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp)
            Text("2× XP on every call till Sunday", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 4.dp, end = 32.dp))
            Text("Talk more, level up faster, unlock better rewards.", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Box(
                Modifier.padding(top = 12.dp).height(32.dp).clip(CircleShape).background(Color.White).clickable(onClick = onView).padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("View rewards", color = VibeColors.Dark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
        }
        Text("✕", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).clickable(onClick = onClose))
    }
}
