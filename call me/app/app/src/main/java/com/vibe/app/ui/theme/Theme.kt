package com.vibe.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Design tokens, call-me edition: light "Blush" theme (soft blush white, rose pink). */
object VibeColors {
    val Dark = Color(0xFFFFF5F8)        // screen background (blush white)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFFFE0EA)
    val Pink = Color(0xFFFF4D8D)
    val Cyan = Color(0xFFFF7AB0)        // second pink (was cyan)
    val Gold = Color(0xFFC98A0B)        // coins (darker so it reads on white)
    val Green = Color(0xFF12A150)
    val Red = Color(0xFFE5384F)
    val Violet = Color(0xFFFF7AB0)
    val Ink = Color(0xFF2A1520)         // main text
    val TextDim = Color(0xFF7E6070)
    val TextMute = Color(0xFFB093A2)
    val ChipText = Color(0xFF8E6B7E)
}

object VibeBrush {
    val Button = Brush.linearGradient(listOf(VibeColors.Pink, Color(0xFFFF7AB0)))
    val Text = Brush.linearGradient(listOf(VibeColors.Pink, Color(0xFFFF7AB0)))
    val ChipOn = Brush.linearGradient(listOf(VibeColors.Pink.copy(alpha = 0.16f), VibeColors.Cyan.copy(alpha = 0.10f)))
}

@Composable
fun VibeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = VibeColors.Pink,
            secondary = VibeColors.Cyan,
            background = VibeColors.Dark,
            surface = VibeColors.Card,
            onPrimary = Color.White,
            onBackground = VibeColors.Ink,
            onSurface = VibeColors.Ink,
        ),
    ) {
        // one press feedback for the whole app: a small shrink instead of the ripple's square highlight
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.foundation.LocalIndication provides PressScale, content = content)
    }
}
