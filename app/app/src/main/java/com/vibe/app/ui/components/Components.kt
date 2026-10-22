package com.vibe.app.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

/** Soft neon light blob used in page backgrounds. */
@Composable
fun GlowBlob(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(Brush.radialGradient(listOf(color.copy(alpha = 0.30f), Color.Transparent))))
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(if (enabled) VibeBrush.Button else SolidColor(VibeColors.Line))
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        else Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(40.dp).clip(shape).background(VibeColors.Card)
            .border(1.dp, VibeColors.Line, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Onboarding progress: `done` of `total` steps highlighted. */
@Composable
fun StepDots(done: Int, total: Int = 3) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) {
            Box(Modifier.width(24.dp).height(6.dp).clip(CircleShape).background(if (it < done) VibeColors.Pink else VibeColors.Line))
        }
    }
}

/** Rounded input container whose border glows pink when focused. */
@Composable
fun FieldBox(
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    error: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val color = when {
        error -> VibeColors.Red
        focused -> VibeColors.Pink
        else -> VibeColors.Line
    }
    Row(
        modifier.height(56.dp).clip(shape).background(VibeColors.Card).border(1.5.dp, color, shape).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(if (selected) VibeBrush.ChipOn else SolidColor(VibeColors.Card))
            .border(1.5.dp, if (selected) VibeColors.Pink else VibeColors.Line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Color.White else VibeColors.ChipText, fontSize = 14.sp)
    }
}

/** Gentle up/down float offset in dp for mascots. */
@Composable
fun rememberFloat(): State<Float> = rememberInfiniteTransition(label = "float").animateFloat(
    initialValue = 0f,
    targetValue = -8f,
    animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "floatY",
)

/** Heartbeat scale for the chat mascot. */
@Composable
fun rememberHeartbeat(): State<Float> = rememberInfiniteTransition(label = "beat").animateFloat(
    initialValue = 1f,
    targetValue = 1.07f,
    animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "beatScale",
)

/** Expanding neon ring that fades out, looping. */
@Composable
fun RingWave(color: Color, size: Dp, delayMs: Int = 0) {
    val p = rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart, StartOffset(delayMs)),
        label = "ringP",
    )
    Box(
        Modifier.size(size)
            .graphicsLayer {
                val s = 0.8f + 0.9f * p.value
                scaleX = s; scaleY = s; alpha = 0.9f * (1f - p.value)
            }
            .border(2.dp, color, CircleShape),
    )
}
