package com.vibe.app.ui.screens.status

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.StoryItem
import com.vibe.app.data.StoryRepo
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import kotlinx.coroutines.launch

private const val PHOTO_MS = 5000f

internal fun storyAgo(at: String): String = runCatching {
    val t = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(at.take(19))!!.time
    val m = (System.currentTimeMillis() - t) / 60_000
    when { m < 1 -> "Just now"; m < 60 -> "$m min ago"; else -> "${m / 60} h ago" }
}.getOrDefault("")

/**
 * One status on screen: a photo (shown for a fixed time) or a video (plays with sound, as long as the video). [paused] holds it;
 * [onProgress] reports 0..1 every frame and [onEnd] fires when it is over. The file request carries the login token.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun StoryMedia(item: StoryItem, paused: Boolean, onProgress: (Float) -> Unit, onEnd: () -> Unit) {
    val ctx = LocalContext.current
    val end by rememberUpdatedState(onEnd)
    val report by rememberUpdatedState(onProgress)
    val hold by rememberUpdatedState(paused)
    if (!item.video) {
        com.vibe.app.ui.components.ServerImage(item.url, Modifier.fillMaxSize(), 0f, ContentScale.Fit)
        LaunchedEffect(item.id) {
            var p = 0f
            var last = withFrameNanos { it }
            while (p < 1f) {
                val now = withFrameNanos { it }
                if (!hold) p += (now - last) / 1_000_000f / PHOTO_MS
                last = now
                report(p.coerceAtMost(1f))
            }
            end()
        }
        return
    }
    val player = remember(item.id) {
        val http = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer ${com.vibe.app.data.api.AuthStore.accessToken.orEmpty()}"))
        androidx.media3.exoplayer.ExoPlayer.Builder(ctx).setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(http)).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(com.vibe.app.ui.components.AuthImages.url(item.url)))
            prepare(); playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val l = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { if (state == androidx.media3.common.Player.STATE_ENDED) end() }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    LaunchedEffect(paused) { player.playWhenReady = !paused }
    LaunchedEffect(player) {
        while (true) {
            withFrameNanos { }
            val d = player.duration
            if (d > 0) report((player.currentPosition.toFloat() / d).coerceIn(0f, 1f))
        }
    }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { c -> androidx.media3.ui.PlayerView(c).apply { useController = false; this.player = player; resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT } },
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * Full-screen status viewer (like stories), from the server: progress bars (a video's bar runs for the video's length), tap right =
 * next, tap left = back, press and hold = pause, reply (goes to the chat), video call. Each status seen is counted on the server.
 */
@Composable
fun StatusViewScreen(startIndex: Int, onClose: () -> Unit, onCall: (Int) -> Unit, onProfile: (Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val people = StoryRepo.people
    if (people.isEmpty()) { LaunchedEffect(Unit) { onClose() }; return }
    var who by remember { mutableStateOf(startIndex.coerceIn(0, people.lastIndex)) }
    // start at the first status this person has that I have not seen yet
    var slide by remember { mutableStateOf(people[who].stories.indexOfFirst { !it.seen }.coerceAtLeast(0)) }
    var progress by remember { mutableStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    var replying by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf("") }

    val p = people[who.coerceIn(0, people.lastIndex)]
    val host = p.person
    val item = p.stories[slide.coerceIn(0, p.stories.lastIndex)]

    fun next() {
        if (slide < p.stories.lastIndex) slide++
        else if (who < people.lastIndex) { who++; slide = 0 }
        else onClose()
    }
    fun prev() {
        if (slide > 0) slide--
        else if (who > 0) { who--; slide = 0 }
    }
    LaunchedEffect(item.id) { progress = 0f; StoryRepo.markSeen(item.id) }

    com.vibe.app.ui.components.CallStatusBarIcons()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.runtime.key(item.id) { StoryMedia(item, paused = holding || replying, onProgress = { progress = it }, onEnd = { next() }) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.7f)))))

        // tap zones: left third = back, right two thirds = next, press and hold = pause
        Row(Modifier.fillMaxSize().padding(top = 100.dp, bottom = 100.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().pointerInput(who, slide) {
                detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false }, onTap = { prev() })
            })
            Box(Modifier.weight(2f).fillMaxHeight().pointerInput(who, slide) {
                detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false }, onTap = { next() })
            })
        }

        item.caption?.let {
            Text(
                it, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 25.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp).padding(horizontal = 28.dp),
            )
        }

        Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                p.stories.forEachIndexed { i, _ ->
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
                        Text(storyAgo(item.at), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
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
                    value = reply, onValueChange = { reply = it.take(300) }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        val t = reply.trim()
                        if (t.isNotEmpty()) {
                            reply = ""
                            // a reply is a normal chat message to the host (the usual free / paid message rules)
                            scope.launch {
                                runCatching { com.vibe.app.data.ChatRepo.send(host.id, "Replying to your status: $t", java.util.UUID.randomUUID().toString()) }
                                    .onSuccess { ctx.toast("Reply sent to ${host.name}") }
                                    .onFailure { ctx.toast(it.message ?: "Could not send") }
                            }
                        }
                    }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { replying = it.isFocused },
                    decorationBox = { inner -> Box { if (reply.isEmpty()) Text("Reply to ${host.name}…", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp); inner() } },
                )
            }
            Box(Modifier.size(48.dp).clip(CircleShape).background(VibeBrush.Button).clickable { onCall(host.id) }, contentAlignment = Alignment.Center) {
                Icon(CallIcons.Camera, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}
