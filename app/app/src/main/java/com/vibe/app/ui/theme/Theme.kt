package com.vibe.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Design tokens from C:\live ui design (same as ui/assets/theme.js). */
object VibeColors {
    val Dark = Color(0xFF07070C)
    val Card = Color(0xFF12121C)
    val Surface = Color(0xFF1E0F17)
    val Line = Color(0xFF262636)
    val Pink = Color(0xFFFF2A85)
    val Cyan = Color(0xFF00F2FE)
    val Gold = Color(0xFFF6DC9B)
    val Green = Color(0xFF3EE08F)
    val Red = Color(0xFFFF4D5E)
    val Violet = Color(0xFF7A1FD1)
    val TextDim = Color(0xFF9CA3AF)
    val TextMute = Color(0xFF6B7280)
    val ChipText = Color(0xFFC7C7D1)
}

object VibeBrush {
    val Button = Brush.linearGradient(listOf(VibeColors.Pink, Color(0xFFC3157A), VibeColors.Violet))
    val Text = Brush.linearGradient(listOf(VibeColors.Cyan, VibeColors.Pink, VibeColors.Gold))
    val ChipOn = Brush.linearGradient(listOf(VibeColors.Pink.copy(alpha = 0.22f), VibeColors.Cyan.copy(alpha = 0.12f)))
}

@Composable
fun VibeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = VibeColors.Pink,
            secondary = VibeColors.Cyan,
            background = VibeColors.Dark,
            surface = VibeColors.Card,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content,
    )
}
