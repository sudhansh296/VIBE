package com.vibe.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.theme.VibeColors

/**
 * Full-screen "message" layout used by permissions, offline / error / maintenance / update and payment results:
 * big icon, title, text, optional content (bullets, receipt rows), primary + secondary button.
 */
@Composable
fun StateScreen(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    primary: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    note: String? = null,
    circle: Boolean = false,
    top: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val floatY by rememberFloat()
    Box(modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(tint, 320.dp, Modifier.align(Alignment.TopCenter).offset(0.dp, (-120).dp))
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            top?.invoke()
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(96.dp).graphicsLayer { if (!circle) translationY = floatY.dp.toPx() }
                        .clip(if (circle) CircleShape else RoundedCornerShape(28.dp)).background(tint.copy(alpha = 0.10f))
                        .border(if (circle) 2.dp else 1.dp, tint.copy(alpha = if (circle) 1f else 0.4f), if (circle) CircleShape else RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = tint, modifier = Modifier.size(if (circle) 48.dp else 40.dp)) }
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp))
                Text(subtitle, color = VibeColors.TextDim, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp))
                if (note != null) Text(
                    note, color = tint, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp).clip(CircleShape).background(tint.copy(alpha = 0.08f)).border(1.dp, tint.copy(alpha = 0.4f), CircleShape).padding(horizontal = 16.dp, vertical = 6.dp),
                )
                if (content != null) Box(Modifier.padding(top = 24.dp)) { content() }
            }
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp, 8.dp, 20.dp, 20.dp)) {
                NeonButton(primary, onPrimary)
                if (secondary != null && onSecondary != null) Text(
                    secondary, color = VibeColors.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSecondary).padding(top = 14.dp, bottom = 4.dp),
                )
            }
        }
    }
}

/** Card with rows of "label ... value" (receipts, plan details, why-we-need-it lists). */
@Composable
fun KeyValueCard(rows: List<Pair<String, String>>, modifier: Modifier = Modifier, valueColor: (Int) -> Color = { Color.White }) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(horizontal = 16.dp)) {
        rows.forEachIndexed { i, (k, v) ->
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(k, color = VibeColors.TextMute, fontSize = 13.sp)
                Text(v, color = valueColor(i), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
            if (i < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
        }
    }
}

/** Shimmering grey block for loading skeletons. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, radius: Dp = 16.dp) {
    val a by rememberInfiniteTransition(label = "sk").animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(modifier.alpha(a).clip(RoundedCornerShape(radius)).background(Color(0xFF1E1E2C)))
}
