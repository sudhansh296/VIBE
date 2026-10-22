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
import androidx.compose.runtime.setValue
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

/**
 * Drawable id of a host's photo number [n] (0 = main photo). The test hosts ship with 3 portraits each (host_<id>_<n>);
 * the real app loads the host's uploaded photos from the server. Returns 0 when there is none.
 */
/** The test host's (Priya, id 1) own photo choice: which of her photos is MAIN. Set from Photos & videos > Set as main photo. */
object OwnPhotos {
    var main by androidx.compose.runtime.mutableIntStateOf(0)
}

@Composable
fun hostPhotoRes(host: Host, n: Int = 0, raw: Boolean = false): Int {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // photo 0 of the test host is whatever she chose as MAIN; everywhere the main photo shows (card, DP, status ring) it follows that choice
    val k = if (!raw && host.id == 1 && n == 0) OwnPhotos.main else n
    return remember(host.id, k) { ctx.resources.getIdentifier("host_${host.id}_${k.mod(3)}", "drawable", ctx.packageName) }
}

/** Fills its parent with the host's photo (cropped to fit), or the gradient + initial when the host has no photo. */
@Composable
fun HostPhoto(host: Host, modifier: Modifier = Modifier, n: Int = 0, focusY: Float = -0.45f, fontSize: TextUnit = 24.sp, raw: Boolean = false) {
    val res = if (host.real) 0 else hostPhotoRes(host, n, raw)
    Box(modifier, contentAlignment = Alignment.Center) {
        if (host.real && host.photo != null && n == 0) ServerImage(host.photo, Modifier.matchParentSize(), focusY)
        else if (res != 0) Image(
            painterResource(res), null, Modifier.matchParentSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, alignment = androidx.compose.ui.BiasAlignment(0f, focusY),
        ) else Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(host.c1, host.c2))), contentAlignment = Alignment.Center) {
            Text(host.name.take(1), color = Color.White.copy(alpha = 0.9f), fontSize = fontSize, fontWeight = FontWeight.Black)
        }
    }
}

/** Round/small avatar: the host's main photo (face area), falling back to gradient + initial. */
@Composable
fun Avatar(host: Host, modifier: Modifier = Modifier, fontSize: TextUnit = 24.sp) {
    HostPhoto(host, modifier, 0, -0.6f, fontSize)
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
        Text(status.label, color = if (status == HostStatus.OFFLINE) Color.White.copy(alpha = 0.9f) else status.text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CoinDot(size: Dp = 14.dp) {
    Image(painterResource(R.drawable.coin_img), contentDescription = "coins", modifier = Modifier.size(size))
}

/** VIP / NEW / verified mark shown on a host card. */
@Composable
fun HostBadge(host: Host) {
    when {
        host.vip -> Box(
            Modifier.clip(CircleShape).background(Brush.horizontalGradient(listOf(VibeColors.Gold, Color(0xFFEAB308))))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) { Text("VIP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
        host.isNew -> Box(Modifier.clip(CircleShape).background(VibeBrush.Button).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("NEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        host.verified -> VerifiedTick()
    }
}

@Composable
fun VerifiedTick(size: Dp = 20.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF3B82F6)), contentAlignment = Alignment.Center) {
        Text("✓", color = VibeColors.Ink, fontSize = 11.sp, fontWeight = FontWeight.Black)
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
        Box(Modifier.matchParentSize().padding(2.dp).clip(shape).background(Color(0xFFFFFFFF)))
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
            ) { Text(label, color = if (on) VibeColors.Pink else VibeColors.ChipText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
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

/** "Name  26": the name in the caller's weight, the age a little lighter after a gap (no comma). */
fun nameAge(name: String, age: Int): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    append(name)
    if (age > 0) {
        val s = pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Normal))
        append("  $age")
        pop(s)
    }
}
