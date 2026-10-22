package com.vibe.app.ui.screens

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vibe.app.R
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay

/** Full-screen res/raw/splash.mp4 (center-cropped), cross-fades to login as it ends, tap to replay. Falls back to a static logo on error. */
@OptIn(UnstableApi::class)
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${ctx.packageName}/${R.raw.splash}")))
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) finished = true }
            override fun onPlayerError(error: PlaybackException) { failed = true }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            // release() can block the main thread briefly; do it after the fade, once the login screen is idle
            Handler(Looper.getMainLooper()).postDelayed({ player.release() }, 1000)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(VibeColors.Dark)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!failed) { finished = false; player.seekTo(0); player.play() }
            },
    ) {
        if (failed) FallbackSplash()
        else AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                (LayoutInflater.from(c).inflate(R.layout.splash_player, null) as PlayerView).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    this.player = player
                }
            },
        )
    }

    // start the cross-fade to login ~300ms before the video ends so there is no pause
    LaunchedEffect(failed) {
        if (failed) { delay(3000); onDone(); return@LaunchedEffect }
        while (true) {
            val d = player.duration
            if (finished || (d > 0 && d - player.currentPosition <= 300)) { onDone(); break }
            delay(50)
        }
    }
}

@Composable
private fun FallbackSplash() {
    val y by rememberFloat()
    Box(Modifier.fillMaxSize()) {
        GlowBlob(VibeColors.Pink, 320.dp, Modifier.offset((-90).dp, (-60).dp))
        GlowBlob(VibeColors.Cyan, 320.dp, Modifier.align(Alignment.BottomEnd).offset(100.dp, (-80).dp))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.ghost_home), null, Modifier.size(176.dp).graphicsLayer { translationY = y.dp.toPx() })
            Text("VIBE", style = TextStyle(brush = VibeBrush.Text, fontSize = 72.sp, fontWeight = FontWeight.Black))
            Text("Talk · Connect · Earn", color = VibeColors.ChipText, fontSize = 16.sp)
        }
    }
}
