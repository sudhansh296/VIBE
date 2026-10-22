package com.vibe.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.ui.theme.VibeColors

private val labelGrey = Color(0xFF8B8B9A)
private val labelBrush = Brush.horizontalGradient(listOf(VibeColors.Cyan, VibeColors.Pink))

/**
 * Bottom navigation (design 2B "center notch"): Home and Me on the sides, a floating chat ghost in a curved notch.
 * Tabs: 0 = Home, 1 = Chats, 2 = Me.
 */
@Composable
fun VibeTabBar(selected: Int, unread: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        TabBarContent(selected, unread, onSelect)
        // solid fill under the system navigation bar so content never shows through
        Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(VibeColors.Dark))
    }
}

@Composable
private fun TabBarContent(selected: Int, unread: Int, onSelect: (Int) -> Unit) {
    Box(Modifier.fillMaxWidth().height(104.dp)) {
        Canvas(Modifier.matchParentSize()) { drawNotchBar() }

        SideTab(R.drawable.ghost_home, "Home", selected == 0, { onSelect(0) },
            Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 12.dp))
        SideTab(R.drawable.ghost_profile, "Me", selected == 2, { onSelect(2) },
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 12.dp))

        CenterChat(selected == 1, unread, { onSelect(1) }, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
        Text(
            "Chats",
            style = if (selected == 1) TextStyle(brush = labelBrush, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            else TextStyle(color = labelGrey, fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
        )
    }
}

/** Bar shape from the HTML design's 420x104 SVG path, scaled to the real width. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNotchBar() {
    val sx = size.width / 420f
    val sy = size.height / 104f
    fun Path.edge() {
        moveTo(0f, 30 * sy); lineTo(138 * sx, 30 * sy)
        cubicTo(158 * sx, 30 * sy, 164 * sx, 34 * sy, 170 * sx, 46 * sy)
        cubicTo(180 * sx, 62 * sy, 194 * sx, 72 * sy, 210 * sx, 72 * sy)
        cubicTo(226 * sx, 72 * sy, 240 * sx, 62 * sy, 250 * sx, 46 * sy)
        cubicTo(256 * sx, 34 * sy, 262 * sx, 30 * sy, 282 * sx, 30 * sy)
        lineTo(size.width, 30 * sy)
    }
    val fill = Path().apply { edge(); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
    val line = Path().apply { edge() }
    drawPath(fill, Brush.verticalGradient(listOf(Color(0xF516121F), VibeColors.Dark), startY = 30 * sy, endY = size.height))
    val edge = Brush.horizontalGradient(
        0f to VibeColors.Cyan.copy(alpha = 0.15f), 0.35f to VibeColors.Pink.copy(alpha = 0.9f), 0.5f to VibeColors.Pink,
        0.65f to Color(0xFFA855F7).copy(alpha = 0.9f), 1f to VibeColors.Cyan.copy(alpha = 0.15f),
    )
    drawPath(line, edge, style = Stroke(width = 8.dp.toPx()), alpha = 0.12f)
    drawPath(line, edge, style = Stroke(width = 4.dp.toPx()), alpha = 0.3f)
    drawPath(line, edge, style = Stroke(width = 1.5.dp.toPx()))
}

@Composable
private fun SideTab(icon: Int, label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val bounce = spring<Float>(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow)
    val lift by animateFloatAsState(if (active) -3f else 0f, bounce, label = "lift")
    val grow by animateFloatAsState(if (active) 1.04f else 1f, bounce, label = "grow")
    val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "press")
    val dot by animateDpAsState(if (active) 20.dp else 0.dp, spring(dampingRatio = 0.4f), label = "dot")

    Column(
        modifier.width(96.dp).graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(interactionSource = press, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(50.dp).graphicsLayer { translationY = lift.dp.toPx(); scaleX = grow; scaleY = grow }) {
            if (active) Box(
                Modifier.matchParentSize().padding(4.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(VibeColors.Cyan.copy(alpha = 0.45f), VibeColors.Pink.copy(alpha = 0.18f), Color.Transparent))),
            )
            Image(painterResource(icon), label, Modifier.matchParentSize().graphicsLayer { alpha = if (active) 1f else 0.55f })
        }
        Text(
            label,
            style = if (active) TextStyle(brush = labelBrush, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            else TextStyle(color = labelGrey, fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
        )
        Box(
            Modifier.padding(top = 3.dp).width(dot).height(4.dp).clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))),
        )
    }
}

@Composable
private fun CenterChat(active: Boolean, unread: Int, onClick: () -> Unit, modifier: Modifier) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "press")
    val t = rememberInfiniteTransition(label = "chat")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val beat by t.animateFloat(1f, 1f, infiniteRepeatable(keyframes {
        durationMillis = 1800
        1f at 0; 1.08f at 216; 0.97f at 432; 1f at 648
    }), label = "beat")

    Box(
        modifier.size(96.dp).graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(interactionSource = press, indication = null, onClick = onClick),
    ) {
        // light pool under the ghost instead of a circle
        Box(
            Modifier.align(Alignment.BottomCenter).offset(y = 6.dp).size(70.dp, 14.dp)
                .graphicsLayer { scaleX = 1f - 0.25f * bob; alpha = 0.9f - 0.35f * bob }
                .drawBehind {
                    drawOval(Brush.radialGradient(listOf(VibeColors.Pink.copy(alpha = 0.75f), VibeColors.Cyan.copy(alpha = 0.25f), Color.Transparent),
                        center = center, radius = size.width / 2))
                },
        )
        Box(Modifier.matchParentSize().graphicsLayer { translationY = -7.dp.toPx() * bob; rotationZ = -2f + 4f * bob }) {
            Box(
                Modifier.matchParentSize().padding(10.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(VibeColors.Pink.copy(alpha = if (active) 0.5f else 0.3f), VibeColors.Cyan.copy(alpha = 0.12f), Color.Transparent))),
            )
            Image(painterResource(R.drawable.ghost_chat), "Chats", Modifier.matchParentSize().graphicsLayer { scaleX = beat; scaleY = beat })
            if (unread > 0) HeartBadge(unread, Modifier.align(Alignment.TopEnd).offset((-3).dp, 3.dp))
        }
    }
}

/** Glossy heart with the unread count; sits over the heart drawn in the chat ghost. */
@Composable
private fun HeartBadge(count: Int, modifier: Modifier) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium)) }
    val beat by rememberInfiniteTransition(label = "heart").animateFloat(1f, 1f, infiniteRepeatable(keyframes {
        durationMillis = 2400
        1f at 0; 1.18f at 192; 0.96f at 384; 1.08f at 576; 1f at 768
    }), label = "hb")

    Box(modifier.size(33.dp, 31.dp).graphicsLayer { val s = pop.value * beat; scaleX = s; scaleY = s }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Brush.radialGradient(listOf(VibeColors.Pink.copy(alpha = 0.55f), Color.Transparent)), radius = size.minDimension * 0.75f)
            scale(size.width / 30f, size.height / 28f, pivot = Offset.Zero) {
                val heart = Path().apply {
                    moveTo(15f, 27f)
                    cubicTo(6f, 20f, 1f, 15f, 1f, 9f); cubicTo(1f, 4.5f, 4.5f, 1f, 9f, 1f)
                    cubicTo(11.6f, 1f, 13.8f, 2.3f, 15f, 4.3f); cubicTo(16.2f, 2.3f, 18.4f, 1f, 21f, 1f)
                    cubicTo(25.5f, 1f, 29f, 4.5f, 29f, 9f); cubicTo(29f, 15f, 24f, 20f, 15f, 27f); close()
                }
                drawPath(heart, Brush.linearGradient(listOf(Color(0xFFFF5FA6), VibeColors.Pink, Color(0xFFB3127A)), Offset.Zero, Offset(30f, 28f)))
                drawPath(heart, VibeColors.Dark, style = Stroke(width = 1.6f))
                rotate(-25f, pivot = Offset(9.5f, 7f)) {
                    drawOval(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.75f), Color.Transparent), 4f, 10f),
                        topLeft = Offset(4.5f, 4f), size = Size(10f, 6f))
                }
            }
        }
        Text(if (count > 99) "99+" else "$count", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 3.dp))
    }
}
