package com.vibe.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** App icons drawn as vectors (no emoji), so they look the same on every phone. */
object VibeIcons {
    /** Filled video camera: rounded body + lens wedge. Tint with Icon(tint = …). */
    val Video: ImageVector = ImageVector.Builder("Video", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = addPathNodes("M5 5.5h8.5a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3v-7a3 3 0 0 1 3-3z"),
            fill = SolidColor(Color.White),
        )
        addPath(
            pathData = addPathNodes("M17.5 10.2l3.4-2.3c.5-.3 1.1 0 1.1.6v6.9c0 .6-.6.9-1.1.6l-3.4-2.3z"),
            fill = SolidColor(Color.White),
        )
    }.build()
}
