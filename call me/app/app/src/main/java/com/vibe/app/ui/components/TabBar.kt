package com.vibe.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.theme.VibeColors

private val tabGrey = Color(0xFFB093A2)
private val tabLit = Color(0xFFFF4D8D)
private val barColor = Color(0xFFFFFFFF)

/**
 * Bottom navigation (call-me edition, footer option C): a floating white pill with Home on the left, Me on the right and a raised
 * round Chats button in the middle. Tabs: 0 = Home, 1 = Chats, 2 = Me.
 */
@Composable
fun VibeTabBar(selected: Int, unread: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    // solid fade behind the whole bar (and the system navigation area) so cards never show through under it
    Column(modifier.fillMaxWidth().background(Brush.verticalGradient(0f to Color.Transparent, 0.30f to VibeColors.Dark.copy(alpha = 0.94f), 1f to VibeColors.Dark))) {
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            val shape = RoundedCornerShape(28.dp)
            Row(
                Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp).fillMaxWidth().height(64.dp).clip(shape).background(barColor)
                    .border(1.dp, VibeColors.Line, shape),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SideTab(Glyph.Home, "Home", selected == 0, Modifier.weight(1f)) { onSelect(0) }
                Spacer(Modifier.width(84.dp))
                SideTab(Glyph.User, "Me", selected == 2, Modifier.weight(1f)) { onSelect(2) }
            }
            CenterButton(selected == 1, unread, Modifier.align(Alignment.TopCenter)) { onSelect(1) }
        }
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun SideTab(icon: ImageVector, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint by animateColorAsState(if (on) tabLit else tabGrey, label = "tint")
    Column(modifier.height(64.dp).noRippleClick(onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(25.dp))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun CenterButton(on: Boolean, unread: Int, modifier: Modifier, onClick: () -> Unit) {
    val s by animateFloatAsState(if (on) 1.08f else 1f, label = "scale")
    Box(modifier.scale(s).noRippleClick(onClick)) {
        Box(
            Modifier.size(66.dp).clip(CircleShape).background(VibeColors.Dark).padding(5.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFFF4D8D), Color(0xFFFF7AB0)))),
            contentAlignment = Alignment.Center,
        ) { Icon(Glyph.Chat, "Chats", tint = Color.White, modifier = Modifier.size(28.dp)) }
        if (unread > 0) Box(
            Modifier.align(Alignment.TopEnd).offset(4.dp, (-2).dp).size(24.dp).clip(CircleShape).background(Color.White).padding(2.5.dp).clip(CircleShape).background(Color(0xFFE5384F)),
            contentAlignment = Alignment.Center,
        ) { Text("$unread", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 11.sp, style = com.vibe.app.ui.screens.centerLineStyle) }
    }
}
