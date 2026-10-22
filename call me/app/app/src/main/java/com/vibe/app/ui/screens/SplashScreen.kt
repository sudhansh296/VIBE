package com.vibe.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay

/**
 * Splash (call-me edition): no video and no image. Two neon rings expand around a glowing V while the VIBE wordmark fades in,
 * then it hands over to the screen underneath after about 2.4 seconds.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val ring = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }
    val pop = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        pop.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        ring.animateTo(1f, tween(1800))
    }
    LaunchedEffect(Unit) {
        delay(600)
        text.animateTo(1f, tween(800))
        delay(1000)
        onDone()
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark), contentAlignment = Alignment.Center) {
        GlowBlob(VibeColors.Pink, 420.dp, Modifier.align(Alignment.TopCenter).offset(0.dp, (-140).dp))
        GlowBlob(VibeColors.Cyan, 360.dp, Modifier.align(Alignment.BottomEnd).offset(120.dp, 120.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(200.dp)) {
                    val c = Offset(size.width / 2, size.height / 2)
                    listOf(0f, 0.35f).forEachIndexed { i, delayFrac ->
                        val p = ((ring.value - delayFrac) / (1f - delayFrac)).coerceIn(0f, 1f)
                        drawCircle(
                            Brush.sweepGradient(listOf(VibeColors.Pink, VibeColors.Cyan, VibeColors.Pink), c).let { b -> b },
                            radius = 40.dp.toPx() + 54.dp.toPx() * p, center = c, alpha = (1f - p) * 0.9f, style = Stroke(if (i == 0) 3.dp.toPx() else 2.dp.toPx()),
                        )
                    }
                }
                Box(
                    Modifier.size(92.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.background(Brush.linearGradient(listOf(VibeColors.Pink, VibeColors.Violet)), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("V", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black) }
            }
            Text(
                "VIBE", style = TextStyle(brush = VibeBrush.Text, fontSize = 46.sp, fontWeight = FontWeight.Black, letterSpacing = 10.sp),
                modifier = Modifier.padding(top = 8.dp).graphicsLayer { alpha = text.value; translationY = (1f - text.value) * 24f },
            )
            Text(
                "Meet. Talk. Vibe.", color = VibeColors.TextDim, fontSize = 14.sp, letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = text.value },
            )
        }
    }
}
