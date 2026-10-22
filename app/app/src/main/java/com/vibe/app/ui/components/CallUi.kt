package com.vibe.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Demo
import com.vibe.app.data.Gift
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay

// Call screens: layout of design option C (outlined buttons, light type, spaced labels)
// with the colours of option D (monochrome, one thin pink/cyan ring and one dock line).

val CallLabel = Color(0xFF8B8B9A)
val CallText = Color(0xFFF5F5F7)
val CallSheetBg = Color(0xFF0F0F17)
val CallRed = Color(0xFFFF3B4E)

object CallIcons {
    private fun outline(name: String, width: Float, vararg paths: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(addPathNodes(it), stroke = SolidColor(Color.White), strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()

    private fun filled(name: String, vararg paths: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { addPath(addPathNodes(it), fill = SolidColor(Color.White)) }
        }.build()

    val Mic = outline("Mic", 1.6f, "M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3Z", "M19 11a7 7 0 0 1-14 0M12 18v3")
    val Speaker = outline("Speaker", 1.6f, "M11 5 6 9H3v6h3l5 4V5Z", "M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13")
    val Camera = outline("Camera", 1.6f, "M5.5 6h7.5a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3H5.5a3 3 0 0 1-3-3V9a3 3 0 0 1 3-3z", "m16 11 5-3v8l-5-3")
    val Flip = outline("Flip", 1.6f, "M20 8h-3l-2-3H9L7 8H4a1 1 0 0 0-1 1v9a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V9a1 1 0 0 0-1-1Z",
        "M9.5 13.5a2.5 2.5 0 0 1 4.3-1.8M14.5 13.5a2.5 2.5 0 0 1-4.3 1.8")
    val Gift = outline("Gift", 1.6f, "M4 8h16a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z",
        "M12 8v13M19 12v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8", "M7.5 8a2.5 2.5 0 0 1 0-5C10 3 12 8 12 8s2-5 4.5-5a2.5 2.5 0 0 1 0 5")
    val Chevron = outline("Chevron", 2f, "m6 9 6 6 6-6")
    val Lock = outline("Lock", 2f, "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z", "M8 11V8a4 4 0 0 1 8 0v3")
    val Send = outline("Send", 1.8f, "m22 2-7 20-4-9-9-4Z", "M22 2 11 13")
    val Bell = outline("Bell", 1.8f, "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val BellOff = outline("BellOff", 1.7f, "M6 8a6 6 0 0 1 9.3-5M18 8c0 7 3 9 3 9H7", "M10.3 21a1.94 1.94 0 0 0 3.4 0M3 3l18 18")
    val Search = outline("Search", 1.8f, "M18 11a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z", "m20 20-3.5-3.5")
    val Trash = outline("Trash", 1.7f, "M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2M19 6l-1 14a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1L5 6")
    val Ban = outline("Ban", 1.7f, "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z", "m5.6 5.6 12.8 12.8")
    val Flag = outline("Flag", 1.7f, "M5 21V4M5 4h11l-2 4 2 4H5")
    val User = outline("User", 1.7f, "M16 8a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z", "M4 21a8 8 0 0 1 16 0")
    val Photo = outline("Photo", 1.7f, "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3Z", "M11 9a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z", "m21 15-5-5L5 21")
    val Missed = outline("Missed", 1.7f, "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2", "m16 3 5 5M21 3l-5 5")
    val Follow = outline("Follow", 1.7f, "M13 8a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z", "M2 21a7 7 0 0 1 14 0", "M19 8v6M16 11h6")
    val Coin = outline("Coin", 1.7f, "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z", "M12 7v10M9.5 9.5h4a1.5 1.5 0 0 1 0 3h-3a1.5 1.5 0 0 0 0 3h4")
    val Check = outline("Check", 1.7f, "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z", "m8 12 3 3 5-6")
    val News = outline("News", 1.7f, "M3 11v2a1 1 0 0 0 1 1h2l5 4V6L6 10H4a1 1 0 0 0-1 1Z", "M15 9a4 4 0 0 1 0 6M18 7a7 7 0 0 1 0 10")
    val Shield = outline("Shield", 1.7f, "M12 3 5 6v5c0 5 3 8 7 10 4-2 7-5 7-10V6Z", "m9 12 2 2 4-4")
    val Online = outline("Online", 1.7f, "M14.5 12a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0Z", "M7.5 7.5a6.4 6.4 0 0 0 0 9M16.5 7.5a6.4 6.4 0 0 1 0 9")
    val EndCall = filled("EndCall", "M12 9c-3.2 0-6.1 1-8.4 2.6-.4.3-.6.7-.6 1.2v2.4c0 .7.6 1.2 1.3 1.1l3-.5c.6-.1 1-.6 1-1.2v-1.7c1.2-.4 2.4-.6 3.7-.6s2.5.2 3.7.6v1.7c0 .6.4 1.1 1 1.2l3 .5c.7.1 1.3-.4 1.3-1.1v-2.4c0-.5-.2-.9-.6-1.2C18.1 10 15.2 9 12 9Z")
    val Phone = filled("Phone", "M6.6 10.8a15.1 15.1 0 0 0 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1A17 17 0 0 1 3 4c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.3 0 .7-.2 1l-2.3 2.2Z")
}

/** Dark call background with a faint pink wash at the top. */
@Composable
fun CallBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(VibeColors.Dark)
            .drawBehind { drawRect(Brush.verticalGradient(listOf(VibeColors.Pink.copy(alpha = 0.08f), Color.Transparent), endY = size.height * 0.42f)) },
        content = content,
    )
}

/** Avatar inside a thin rotating pink/cyan ring; [ringing] adds an expanding ripple. */
@Composable
fun NeonRingAvatar(initial: String, size: Dp, fontSize: TextUnit, ringing: Boolean = false, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "ring")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "spin")
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing)), label = "ripple")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (ringing) Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2 * (0.95f + 0.4f * ripple)
            drawCircle(Color.White.copy(alpha = 0.22f * (1f - ripple)), radius = r, style = Stroke(1.dp.toPx()))
        }
        Canvas(Modifier.size(size).rotate(spin)) {
            drawCircle(Brush.sweepGradient(listOf(VibeColors.Pink, VibeColors.Cyan, VibeColors.Pink)),
                radius = this.size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
        }
        Box(
            Modifier.size(size - 14.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF1D1A28), Color(0xFF121019)))),
            contentAlignment = Alignment.Center,
        ) { Text(initial, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Light) }
    }
}

/** Outlined round control with an UPPERCASE spaced label underneath. */
@Composable
fun CallButton(
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit,
    on: Boolean = false,
    pink: Boolean = false,
    small: Boolean = false,
) {
    val d = if (small) 46.dp else 56.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.noRippleClick(onClick)) {
        Box(
            Modifier.size(d).clip(CircleShape)
                .background(if (on) Color.White else VibeColors.Dark.copy(alpha = 0.3f))
                .border(1.dp, if (on) Color.White else if (pink) VibeColors.Pink.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = if (on) VibeColors.Dark else if (pink) VibeColors.Pink else CallText,
                modifier = Modifier.size(if (small) 20.dp else 23.dp))
        }
        if (label != null && !small) Text(label.uppercase(), color = CallLabel, fontSize = 9.sp, letterSpacing = 1.4.sp,
            modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun EndCallButton(onClick: () -> Unit, small: Boolean = false, label: String? = null) {
    val d = if (small) 46.dp else 64.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.noRippleClick(onClick)) {
        Box(
            Modifier.size(d).clip(CircleShape).background(CallRed).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(CallIcons.EndCall, "End call", tint = Color.White, modifier = Modifier.size(if (small) 22.dp else 28.dp)) }
        if (label != null) Text(label.uppercase(), color = CallLabel, fontSize = 9.sp, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun AcceptCallButton(icon: ImageVector, onClick: () -> Unit) {
    val y by rememberInfiniteTransition(label = "nudge").animateFloat(0f, -6f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.noRippleClick(onClick)) {
        Box(
            Modifier.graphicsLayer { translationY = y.dp.toPx() }.size(64.dp).clip(CircleShape).background(VibeColors.Green).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, "Accept", tint = VibeColors.Dark, modifier = Modifier.size(27.dp)) }
        Text("ACCEPT", color = CallLabel, fontSize = 9.sp, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

/** Thin outlined capsule used for balance, timer and small actions. */
@Composable
fun CallPill(modifier: Modifier = Modifier, borderColor: Color = Color.White.copy(alpha = 0.14f), content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.height(32.dp).clip(CircleShape).background(VibeColors.Dark.copy(alpha = 0.45f)).border(1.dp, borderColor, CircleShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun BalancePill(balance: Int, rate: Int? = null) {
    CallPill {
        CoinDot(14.dp)
        Text("%,d".format(balance), color = VibeColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        if (rate != null) Text("· $rate/min", color = CallLabel, fontSize = 12.sp)
    }
}

@Composable
fun EncryptedLabel() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(CallIcons.Lock, null, tint = CallText.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
        Text("  ENCRYPTED", color = CallText.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 0.8.sp)
    }
}

@Composable
fun CallIconButton(icon: ImageVector, onClick: () -> Unit, tint: Color = CallText, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(VibeColors.Dark.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

/** UPPERCASE spaced status line, e.g. "RINGING…" or the live timer. */
@Composable
fun CallStatus(text: String, live: Boolean = false, color: Color = Color(0xFFC7C7D1)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (live) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(VibeColors.Green))
            Spacer(Modifier.width(8.dp))
        }
        Text(text.uppercase(), color = color, fontSize = 13.sp, letterSpacing = 1.8.sp)
    }
}

/** "30 COINS / MIN · AFTER PICK-UP" line. */
@Composable
fun CallRateLine(rate: Int, note: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoinDot(12.dp); Spacer(Modifier.width(6.dp))
        Text("$rate", color = VibeColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Text(" COINS / MIN · ${note.uppercase()}", color = Color(0xFF6B6B7B), fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

/** Thin pink→cyan line on top of the control area (design D). */
fun Modifier.dockLine(): Modifier = this.drawBehind {
    drawLine(
        Brush.horizontalGradient(listOf(Color.Transparent, VibeColors.Pink, VibeColors.Cyan, Color.Transparent), startX = size.width * 0.24f, endX = size.width * 0.76f),
        start = Offset(size.width * 0.24f, 0f), end = Offset(size.width * 0.76f, 0f), strokeWidth = 1.dp.toPx(),
    )
}

/** Stand-in for a camera stream (WebRTC renders the real video here). */
@Composable
fun CameraStandIn(c1: Color, c2: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Brush.radialGradient(listOf(c1, c2)))) {
        Box(Modifier.align(Alignment.BottomCenter).offset(y = (-170).dp).size(190.dp, 240.dp).clip(RoundedCornerShape(50))
            .background(Brush.radialGradient(listOf(c1.copy(alpha = 0.9f), c2))).graphicsLayer { alpha = 0.6f })
        Box(Modifier.align(Alignment.BottomCenter).offset(y = 40.dp).size(380.dp, 230.dp).clip(RoundedCornerShape(topStartPercent = 50, topEndPercent = 50))
            .background(Brush.verticalGradient(listOf(c1.copy(alpha = 0.7f), c2))).graphicsLayer { alpha = 0.7f })
    }
}

@Composable
fun CallShade() {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        0f to VibeColors.Dark.copy(alpha = 0.75f), 0.34f to Color.Transparent, 0.58f to Color.Transparent, 1f to VibeColors.Dark.copy(alpha = 0.85f))))
}

@Composable
fun LowBalanceBanner(minutesLeft: Int, onTopUp: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(VibeColors.Dark.copy(alpha = 0.7f))
            .border(1.dp, VibeColors.Gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(if (minutesLeft <= 0) "Less than a minute left" else "About $minutesLeft min left", color = VibeColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Top up to keep talking", color = VibeColors.TextDim, fontSize = 11.sp)
        }
        Box(
            Modifier.height(30.dp).clip(CircleShape).background(VibeColors.Gold).clickable(onClick = onTopUp).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text("TOP UP", color = VibeColors.Dark, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp) }
    }
}

/** Big gift animation in the middle of the call; real apps play the admin-uploaded SVGA/MP4 here. */
@Composable
fun GiftBurst(gift: Gift?, qty: Int, onDone: () -> Unit) {
    if (gift == null) return
    val anim = remember(gift, qty) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(gift, qty) { anim.animateTo(1f, tween(1800)); onDone() }
    val p = anim.value
    val scale = when { p < 0.25f -> 0.2f + p / 0.25f * 0.95f; p < 0.4f -> 1.15f - (p - 0.25f) / 0.15f * 0.15f; p < 0.8f -> 1f; else -> 1f + (p - 0.8f) * 1.5f }
    val alpha = when { p < 0.2f -> p / 0.2f; p < 0.8f -> 1f; else -> 1f - (p - 0.8f) / 0.2f }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(gift.emoji, fontSize = 100.sp)
            Text("${gift.name.uppercase()}${if (qty > 1) " ×$qty" else ""}", color = Color.White, fontSize = 13.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Light)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftSheet(balance: Int, onDismiss: () -> Unit, onSend: (Gift, Int) -> Unit) {
    var tab by remember { mutableStateOf(Demo.giftCategories.keys.first()) }
    var pick by remember { mutableIntStateOf(0) }
    var qty by remember { mutableIntStateOf(1) }
    val gifts = Demo.giftCategories.getValue(tab)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CallSheetBg) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 26.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Demo.giftCategories.keys.forEach { k ->
                        Column(Modifier.noRippleClick { tab = k; pick = 0 }) {
                            Text(k.uppercase(), color = if (k == tab) Color.White else Color(0xFF6B6B7B), fontSize = 11.sp, letterSpacing = 1.5.sp)
                            Box(Modifier.padding(top = 6.dp).width(24.dp).height(1.dp).background(if (k == tab) VibeColors.Pink else Color.Transparent))
                        }
                    }
                }
                BalancePill(balance)
            }
            LazyVerticalGrid(GridCells.Fixed(4), Modifier.padding(top = 16.dp).height(212.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(gifts) { i, g -> GiftTile(g, i == pick) { pick = i } }
            }
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(1, 5, 10).forEach { n ->
                    Box(
                        Modifier.padding(end = 6.dp).size(44.dp, 40.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (n == qty) VibeColors.Pink.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f))
                            .border(1.dp, if (n == qty) VibeColors.Pink else Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .clickable { qty = n },
                        contentAlignment = Alignment.Center,
                    ) { Text("×$n", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
                val g = gifts[pick]
                Box(
                    Modifier.weight(1f).height(44.dp).clip(CircleShape).background(Color.White).clickable { onSend(g, qty) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Send ${if (qty > 1) "$qty× " else ""}${g.name} · ${"%,d".format(g.price * qty)}", color = VibeColors.Dark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Gift animations are uploaded from the admin panel", color = Color(0xFF6B6B7B), fontSize = 10.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        }
    }
}

@Composable
private fun GiftTile(g: Gift, on: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.aspectRatio(0.82f).clip(RoundedCornerShape(18.dp))
            .background(if (on) VibeColors.Pink.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f))
            .border(1.dp, if (on) VibeColors.Pink else Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(g.emoji, fontSize = 28.sp)
        Text(g.name, color = Color(0xFFD1D5DB), fontSize = 10.sp, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) { CoinDot(10.dp); Text(" ${g.price}", color = VibeColors.Gold, fontSize = 10.sp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpSheet(onDismiss: () -> Unit, onPay: (coins: Int) -> Unit) {
    var pick by remember { mutableIntStateOf(1) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CallSheetBg) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text("Quick top-up", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Light)
            Text("Your call keeps going while you pay", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Demo.topUpPacks.forEachIndexed { i, (coins, price) ->
                    val on = i == pick
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .background(if (on) VibeColors.Pink.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f))
                            .border(1.dp, if (on) VibeColors.Pink else Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                            .clickable { pick = i }.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CoinDot(28.dp)
                        Text("%,d".format(coins), color = VibeColors.Gold, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                        Text("₹$price", color = VibeColors.TextDim, fontSize = 12.sp)
                    }
                }
            }
            Box(
                Modifier.padding(top = 20.dp).fillMaxWidth().height(48.dp).clip(CircleShape).background(Color.White)
                    .clickable { onPay(Demo.topUpPacks[pick].first) },
                contentAlignment = Alignment.Center,
            ) { Text("Pay ₹${Demo.topUpPacks[pick].second}", color = VibeColors.Dark, fontWeight = FontWeight.Bold) }
            Text("Prices from the server · Razorpay (UPI / card)", color = Color(0xFF6B6B7B), fontSize = 10.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndCallSheet(summary: String, onDismiss: () -> Unit, onEnd: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CallSheetBg) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("End this call?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Light)
            Text(summary.uppercase(), color = VibeColors.TextDim, fontSize = 11.sp, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.weight(1f).height(48.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("Keep talking", color = Color.White, fontSize = 14.sp) }
                Box(
                    Modifier.weight(1f).height(48.dp).clip(CircleShape).background(CallRed).clickable(onClick = onEnd),
                    contentAlignment = Alignment.Center,
                ) { Text("End call", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

fun formatCallTime(secs: Int) = "%02d:%02d".format(secs / 60, secs % 60)

/** Random "voice level" 0..1 updated a few times a second while [active]. */
@Composable
fun rememberVoiceLevel(active: Boolean): Float {
    var level by remember { mutableStateOf(0f) }
    LaunchedEffect(active) {
        while (active) { level = kotlin.random.Random.nextFloat(); delay(180) }
        level = 0f
    }
    return level
}
