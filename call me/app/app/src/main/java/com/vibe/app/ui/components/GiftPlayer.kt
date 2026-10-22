package com.vibe.app.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.vibe.app.data.GiftPlay
import com.vibe.app.data.GiftQueue
import coil.request.onAnimationEnd
import coil.request.repeatCount
import kotlinx.coroutines.delay

/**
 * Plays sent / received gifts one after another, centred over whatever screen is open (chat, profile, call, anything).
 * The animation file is whatever the admin uploaded: MP4, SVGA, Lottie JSON, animated WEBP or GIF. Gifts without an animation
 * show their picture (or emoji) with a short pop. A tap closes it; it also closes by itself when the animation ends.
 * Place once, above everything, in VibeApp.
 */
@Composable
fun GiftOverlay() {
    val p = GiftQueue.items.firstOrNull() ?: return
    key(p.id) { GiftPlayerView(p) { GiftQueue.done() } }
}

@Composable
private fun GiftPlayerView(p: GiftPlay, onDone: () -> Unit) {
    var closed by remember { mutableStateOf(false) }
    fun close() { if (!closed) { closed = true; onDone() } }
    LaunchedEffect(Unit) { delay(15_000); close() }   // never stays on screen for ever (a broken file, a stuck player)
    val type = p.gift.animType?.takeIf { p.gift.animUrl != null }
    Box(
        Modifier.fillMaxSize()
            .then(if (type == "mp4") Modifier.background(Color.Black.copy(alpha = 0.55f)) else Modifier)   // an MP4 brings its own background
            .noRippleClick { close() },
        contentAlignment = Alignment.Center,
    ) {
        val url = p.gift.animUrl
        when (type) {
            "mp4" -> Mp4Gift(url!!, ::close)
            "svga" -> SvgaGift(url!!, ::close)
            "lottie" -> LottieGift(url!!, ::close)
            "webp", "gif" -> AnimatedImageGift(url!!, ::close)
            else -> PopGift(p, ::close)
        }
        val qty = if (p.quantity > 1) "${p.quantity}× " else "a "
        val text = when {
            p.mine && p.toName != null -> "You sent ${p.toName} $qty${p.gift.name}"
            p.mine -> "You sent $qty${p.gift.name}"
            else -> "${p.fromName} sent you $qty${p.gift.name}"
        }
        Text(
            text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp, start = 24.dp, end = 24.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun Mp4Gift(url: String, onEnd: () -> Unit) {
    val ctx = LocalContext.current
    val player = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(url)); prepare(); playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        val l = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { if (state == androidx.media3.common.Player.STATE_ENDED) onEnd() }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { onEnd() }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    AndroidView(
        factory = { c ->
            androidx.media3.ui.PlayerView(c).apply {
                this.player = player; useController = false
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun SvgaGift(url: String, onEnd: () -> Unit) {
    AndroidView(
        factory = { c ->
            com.opensource.svgaplayer.SVGAImageView(c).apply {
                loops = 1
                clearsAfterStop = true
                callback = object : com.opensource.svgaplayer.SVGACallback {
                    override fun onFinished() = onEnd()
                    override fun onPause() {}
                    override fun onRepeat() {}
                    override fun onStep(frame: Int, percentage: Double) {}
                }
                val parser = com.opensource.svgaplayer.SVGAParser.shareParser().also { it.init(c.applicationContext) }
                parser.decodeFromURL(java.net.URL(url), object : com.opensource.svgaplayer.SVGAParser.ParseCompletion {
                    override fun onComplete(videoItem: com.opensource.svgaplayer.SVGAVideoEntity) { setVideoItem(videoItem); startAnimation() }
                    override fun onError() = onEnd()
                })
            }
        },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.stopAnimation(true) },
    )
}

@Composable
private fun LottieGift(url: String, onEnd: () -> Unit) {
    val result = rememberLottieComposition(LottieCompositionSpec.Url(url))
    val progress by animateLottieCompositionAsState(result.value, iterations = 1)
    LaunchedEffect(result.isFailure) { if (result.isFailure) onEnd() }
    LaunchedEffect(progress, result.value) { if (result.value != null && progress >= 1f) onEnd() }
    LottieAnimation(result.value, { progress }, Modifier.fillMaxSize())
}

@Composable
private fun AnimatedImageGift(url: String, onEnd: () -> Unit) {
    val ctx = LocalContext.current
    val loader = remember {
        coil.ImageLoader.Builder(ctx.applicationContext).components {
            if (Build.VERSION.SDK_INT >= 28) add(coil.decode.ImageDecoderDecoder.Factory()) else add(coil.decode.GifDecoder.Factory())
        }.build()
    }
    coil.compose.AsyncImage(
        model = coil.request.ImageRequest.Builder(ctx).data(url)
            .repeatCount(0)                                   // play once
            .onAnimationEnd { onEnd() }
            .listener(onError = { _, _ -> onEnd() })
            .build(),
        imageLoader = loader, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
    )
    // some files do not report their end: close after a while anyway
    LaunchedEffect(Unit) { delay(6000); onEnd() }
}

/** No animation uploaded yet: the gift's picture (or emoji) pops in the middle and fades away. */
@Composable
private fun PopGift(p: GiftPlay, onEnd: () -> Unit) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        delay(1200)
        alpha.animateTo(0f, tween(400))
        onEnd()
    }
    Column(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }, horizontalAlignment = Alignment.CenterHorizontally) {
        val thumb = p.gift.thumbUrl
        if (thumb != null) coil.compose.AsyncImage(thumb, null, Modifier.size(180.dp))
        else Text(p.gift.emoji ?: "🎁", fontSize = 120.sp)
        if (p.quantity > 1) Text("×${p.quantity}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A gift's small picture for lists and sheets: the uploaded thumbnail, else its emoji. */
@Composable
fun GiftThumb(g: com.vibe.app.data.GiftItem, size: androidx.compose.ui.unit.Dp) {
    if (g.thumbUrl != null) coil.compose.AsyncImage(g.thumbUrl, g.name, Modifier.size(size))
    else Text(g.emoji ?: "🎁", fontSize = (size.value * 0.8f).sp)
}
