package com.vibe.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icon set of the call-me edition: bold, rounded, mostly SOLID glyphs drawn in code (no image files).
 * Tint them with Icon(tint = ...).
 */
object Glyph {
    private fun solid(name: String, vararg paths: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { addPath(addPathNodes(it), fill = SolidColor(Color.White)) }
    }.build()

    private fun line(name: String, width: Float, vararg paths: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach {
            addPath(addPathNodes(it), stroke = SolidColor(Color.White), strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }.build()

    val Home = solid("GHome", "M12 2.8 2.6 10.6a1 1 0 0 0-.36.77V20a1.2 1.2 0 0 0 1.2 1.2H9v-6.2h6v6.2h5.56a1.2 1.2 0 0 0 1.2-1.2v-8.63a1 1 0 0 0-.36-.77Z")
    val Chat = solid("GChat", "M6 3.5h12A3.5 3.5 0 0 1 21.5 7v7a3.5 3.5 0 0 1-3.5 3.5h-5.2L8 21.3v-3.8H6A3.5 3.5 0 0 1 2.5 14V7A3.5 3.5 0 0 1 6 3.5Z")
    val User = solid("GUser", "M12 3a4.6 4.6 0 1 0 0 9.2A4.6 4.6 0 0 0 12 3Z", "M3 20.4C3 16.6 7 14 12 14s9 2.6 9 6.4a.9.9 0 0 1-.9.9H3.9a.9.9 0 0 1-.9-.9Z")
    val Bolt = solid("GBolt", "M13.6 1.8 4.4 13.3a.8.8 0 0 0 .62 1.3H10l-1.3 7.2a.5.5 0 0 0 .9.38l9-11.5a.8.8 0 0 0-.63-1.3H13.9l1.2-7.2a.5.5 0 0 0-1.5-.38Z")
    val Video = solid("GVideo", "M5.6 5.5h6.8A3.6 3.6 0 0 1 16 9.1v5.8a3.6 3.6 0 0 1-3.6 3.6H5.6A3.6 3.6 0 0 1 2 14.9V9.1a3.6 3.6 0 0 1 3.6-3.6Z", "M22 8.4a.8.8 0 0 0-1.2-.7l-3 1.9v4.8l3 1.9a.8.8 0 0 0 1.2-.7Z")
    val Bell = solid("GBell", "M12 2.5a6.3 6.3 0 0 0-6.3 6.3v3.2L3.8 15.6a1 1 0 0 0 .9 1.5h14.6a1 1 0 0 0 .9-1.5L18.3 12V8.8A6.3 6.3 0 0 0 12 2.5Z", "M9.4 19h5.2a2.6 2.6 0 0 1-5.2 0Z")
    val Star = solid("GStar", "m12 2.6 2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.2 1.2-6.5L2.5 9.5l6.6-.9Z")
    val Check = line("GCheck", 2.8f, "m5 12.5 4.5 4.5L19 7.5")
}
