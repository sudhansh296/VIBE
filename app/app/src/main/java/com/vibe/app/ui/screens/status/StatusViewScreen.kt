package com.vibe.app.ui.screens.status

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Demo
import com.vibe.app.data.StatusStore
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

private const val SLIDE_MS = 5000f

/**
 * Full-screen status viewer (like stories): progress bars, tap right = next, tap left = back, hold = pause, reply, heart, video call.
 * DEMO slides are gradients with text; the real app streams the photo or video the host posted (only if you are allowed to see it).
 */
@Composable
fun StatusViewScreen(startIndex: Int, onClose: () -> Unit, onCall: (Int) -> Unit, onProfile: (Int) -> Unit) {
    val ctx = LocalContext.current
    val items = StatusStore.items
    var who by remember { mutableStateOf(startIndex.coerceIn(0, items.lastIndex)) }
    var slide by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    var replying by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf("") }
    var liked by remember { mutableStateOf(false) }

    val item = items[who]
    val host = remember(item) { Demo.host(item.hostId) }
    val s = item.slides[slide.coerceIn(0, item.slides.lastIndex)]

    fun next() {
        if (slide < item.slides.lastIndex) slide++
        else if (who < items.lastIndex) { who++; slide = 0 }
        else onClose()
    }
    fun prev() {
        if (slide > 0) slide--
        else if (who > 0) { who--; slide = 0 }
        else progress = 0f
    }

    // every slide plays for SLIDE_MS; holding or typing a reply pauses it
    LaunchedEffect(who, slide) {
        progress = 0f
        liked = false
        StatusStore.markSeen(item.hostId)
        var last = androidx.compose.runtime.withFrameNanos { it }
        while (progress < 1f) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            if (!holding && !replying) progress += (now - last) / 1_000_000f / SLIDE_MS
            last = now
        }
        next()
    }

    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(s.bg[0], s.bg[1], s.bg[2])))) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, Color.Black.copy(alpha = 0.7f)))))

        // tap zones: left third = back, right two thirds = next, press and hold = pause
        Row(Modifier.fillMaxSize().padding(top = 100.dp, bottom = 100.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().pointerInput(who, slide) {
                detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false }, onTap = { prev() })
            })
            Box(Modifier.weight(2f).fillMaxHeight().pointerInput(who, slide) {
                detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false }, onTap = { next() })
            })
        }

        Text(
            s.text, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 34.sp,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
        )

        Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item.slides.forEachIndexed { i, _ ->
                    val fill = if (i < slide) 1f else if (i == slide) progress.coerceIn(0f, 1f) else 0f
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(Color.White))
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.weight(1f).clickable { onProfile(host.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(host, Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape), fontSize = 18.sp)
                    Column {
                        Text(host.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(s.ago, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(MeIcons.Close, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.weight(1f).height(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = reply, onValueChange = { reply = it }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (reply.isNotBlank()) { ctx.toast("Reply sent"); reply = "" } }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { replying = it.isFocused },
                    decorationBox = { inner -> Box { if (reply.isEmpty()) Text("Reply to status…", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp); inner() } },
                )
            }
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape).noRippleClick { liked = !liked },
                contentAlignment = Alignment.Center,
            ) { Text(if (liked) "♥" else "♡", color = if (liked) VibeColors.Pink else Color.White, fontSize = 22.sp) }
            Box(Modifier.size(48.dp).clip(CircleShape).background(VibeBrush.Button).clickable { onCall(host.id) }, contentAlignment = Alignment.Center) {
                Icon(CallIcons.Camera, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}
