package com.vibe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.theme.VibeColors

/** Rounded card holding a list of settings rows (dividers are added by [SettingsDivider]). */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape)) { content() }
}

@Composable
fun SettingsDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1C1C28)))

/**
 * One settings row: tinted icon tile, title (+ optional small subtitle), optional value text and either a chevron or a custom
 * [trailing] control (for example a switch).
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    value: String? = null,
    titleColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TileIcon(icon, tint, 36.dp)
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 15.sp)
            if (sub != null) Text(sub, color = VibeColors.TextMute, fontSize = 11.sp)
        }
        if (value != null) Text(value, color = VibeColors.TextMute, fontSize = 12.sp)
        if (trailing != null) trailing() else if (onClick != null) Text("›", color = Color(0xFF4B5563), fontSize = 18.sp)
    }
}

/** Small uppercase group heading above a [SettingsGroup]. */
@Composable
fun SettingsHeading(text: String) {
    Text(
        text.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
    )
}

/** Icon-only decorative size helper so callers can drop a quick icon inside custom rows. */
@Composable
fun SmallIcon(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp = 16.dp) = Icon(icon, null, tint = tint, modifier = Modifier.size(size))
