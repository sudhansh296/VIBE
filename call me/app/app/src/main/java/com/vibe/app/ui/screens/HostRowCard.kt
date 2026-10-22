package com.vibe.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Host
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.Glyph
import com.vibe.app.ui.components.HostPhoto
import com.vibe.app.ui.theme.VibeColors

private val cardPalette = listOf(
    Color(0xFFFF9EC3) to Color(0xFFFF4D8D),
    Color(0xFFFFB3CF) to Color(0xFFFF7AB0),
    Color(0xFFFF8FB8) to Color(0xFFF0447F),
    Color(0xFFFFC2DD) to Color(0xFFFF6FA3),
)

/**
 * Host card, blush poster style: soft pink gradient card in a 2 column grid. White status pill top-left, white price pill top-right,
 * name + city bottom-left, round white video-call button bottom-right. Video calls only, so there is one price.
 */
@Composable
fun HostRowCard(h: Host, onClick: () -> Unit, onCall: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val isHost = !h.real || h.verified     // server people: only hosts are verified; users have no price
    // a host looking at users: status + call button too (calls go both ways), but no price pill
    val viewerIsHost = com.vibe.app.data.api.AuthStore.me?.optString("role") == "host"
    val showStatus = isHost || viewerIsHost
    val canCall = h.status.canCall
    val (c1, c2) = cardPalette[h.id.mod(cardPalette.size)]
    Box(
        Modifier.fillMaxWidth().aspectRatio(0.8f).shadow(10.dp, shape, ambientColor = VibeColors.Pink, spotColor = VibeColors.Pink)
            .clip(shape).background(Brush.linearGradient(listOf(c1, c2))).clickable(onClick = onClick),
    ) {
        // the host's photo fills the card; a soft pink shade at the bottom keeps the name readable
        HostPhoto(h, Modifier.fillMaxSize(), 0, -0.35f, fontSize = 64.sp)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.50f to Color.Transparent, 1f to Color(0xFFB0124F).copy(alpha = 0.72f))))

        // top-left: status (VIP / NEW below it), top-right: price
        Column(Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (showStatus) Row(
                Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(h.status.dot))
                Text("  " + h.status.label.lowercase().replaceFirstChar { it.uppercase() }, color = h.status.text, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (h.isNew && !h.vip) Row(Modifier.clip(CircleShape).background(Color(0xFFFFE9A8)).padding(horizontal = 7.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Glyph.Star, null, tint = Color(0xFF7A4B00), modifier = Modifier.size(10.dp))
                Text(" New", color = Color(0xFF7A4B00), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (isHost) Row(
            Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 8.dp).clip(CircleShape).background(Color.White).padding(start = 4.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoinDot(18.dp)
            Text(" ${h.rate}", color = VibeColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }

        // bottom: name, city and the call button
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 11.dp, end = 8.dp, bottom = 9.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(com.vibe.app.ui.components.nameAge(h.name, h.age), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (h.verified) Icon(Glyph.Check, null, tint = Color.White, modifier = Modifier.padding(start = 3.dp).size(13.dp))
                }
                Row(Modifier.padding(top = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(h.city, color = Color.White.copy(alpha = 0.92f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    // VIP sits right by the city
                    if (h.vip) Text("VIP", color = Color(0xFF7A4B00), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 5.dp).clip(CircleShape).background(Color(0xFFFFE9A8)).padding(horizontal = 6.dp, vertical = 1.dp))
                }
            }
            if (showStatus) Box(
                Modifier.size(38.dp).clip(CircleShape).background(if (canCall) Color.White else Color.White.copy(alpha = 0.5f)).clickable(onClick = onCall),
                contentAlignment = Alignment.Center,
            ) { Icon(Glyph.Video, "Video call", tint = if (canCall) VibeColors.Pink else VibeColors.Pink.copy(alpha = 0.4f), modifier = Modifier.size(22.dp)) }
        }
    }
}
