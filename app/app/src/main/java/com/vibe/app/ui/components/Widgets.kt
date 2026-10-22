package com.vibe.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.vibe.app.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Host
import com.vibe.app.data.HostStatus
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick),
)

/** Placeholder photo: gradient + initial (real photos come from the server later). */
@Composable
fun Avatar(host: Host, modifier: Modifier = Modifier, fontSize: TextUnit = 24.sp) {
    Box(modifier.background(Brush.linearGradient(listOf(host.c1, host.c2))), contentAlignment = Alignment.Center) {
        Text(host.name.take(1), color = Color.White.copy(alpha = 0.9f), fontSize = fontSize, fontWeight = FontWeight.Black)
    }
}

@Composable
fun StatusPill(status: HostStatus, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "p",
    )
    Row(
        modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).graphicsLayer { if (status.pulse) alpha = pulse }.clip(CircleShape).background(status.dot))
        Spacer(Modifier.width(6.dp))
        Text(status.label, color = status.text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CoinDot(size: Dp = 14.dp) {
    Image(painterResource(R.drawable.coin), contentDescription = "coins", modifier = Modifier.size(size))
}

/** VIP / NEW / verified mark shown on a host card. */
@Composable
fun HostBadge(host: Host) {
    when {
        host.vip -> Box(
            Modifier.clip(CircleShape).background(Brush.horizontalGradient(listOf(VibeColors.Gold, Color(0xFFEAB308))))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) { Text("VIP", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
        host.isNew -> Box(Modifier.clip(CircleShape).background(VibeBrush.Button).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("NEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        host.verified -> VerifiedTick()
    }
}

@Composable
fun VerifiedTick(size: Dp = 20.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF3B82F6)), contentAlignment = Alignment.Center) {
        Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

fun genderMark(g: Char) = if (g == 'F') "♀" else "♂"
fun genderColor(g: Char) = if (g == 'F') VibeColors.Pink else VibeColors.Cyan

/** Rotating pink/cyan border (CSS .spin-border). */
@Composable
fun SpinBorder(modifier: Modifier = Modifier, shape: Shape = CircleShape, content: @Composable BoxScope.() -> Unit) {
    val angle by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "a",
    )
    Box(modifier.clip(shape)) {
        Box(
            Modifier.matchParentSize().graphicsLayer { rotationZ = angle; scaleX = 3f; scaleY = 3f }
                .background(Brush.sweepGradient(listOf(Color.Transparent, Color.Transparent, VibeColors.Pink, VibeColors.Cyan, Color.Transparent))),
        )
        Box(Modifier.matchParentSize().padding(2.dp).clip(shape).background(Color(0xFF10101A)))
        // content decides the size (unless the caller fixes it), centered inside
        Box(Modifier.align(Alignment.Center), contentAlignment = Alignment.Center, content = content)
    }
}

/** Equal-width option buttons; one selected (CSS .seg). */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, height: Dp = 48.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier.weight(1f).height(height).clip(shape)
                    .background(if (on) VibeBrush.ChipOn else SolidColor(VibeColors.Card))
                    .border(1.5.dp, if (on) VibeColors.Pink else VibeColors.Line, shape)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) Color.White else VibeColors.ChipText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Card surface used for stats and info blocks. */
fun Modifier.cardSurface(radius: Dp = 16.dp): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(VibeColors.Card)
    .border(1.dp, VibeColors.Line, RoundedCornerShape(radius))

/** Gradient neon outline card (CSS .neon-border). */
fun Modifier.neonBorder(radius: Dp = 16.dp): Modifier = this
    .clip(RoundedCornerShape(radius))
    .border(1.dp, Brush.linearGradient(listOf(VibeColors.Pink, VibeColors.Cyan)), RoundedCornerShape(radius))
