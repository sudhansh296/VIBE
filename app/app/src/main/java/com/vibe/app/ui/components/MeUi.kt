package com.vibe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.theme.VibeColors

/** Extra outline icons for the Me / Wallet pages (same 24x24 stroke style as [CallIcons]). */
object MeIcons {
    private fun outline(name: String, width: Float, vararg paths: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(addPathNodes(it), stroke = SolidColor(Color.White), strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()

    val Calendar = outline("Calendar", 1.7f, "M6 5h12a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z", "M3 10h18M8 3v4M16 3v4")
    val Star = outline("Star", 1.7f, "m12 3 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9L12 3Z")
    val Edit = outline("Edit", 1.8f, "M4 20h4L19 9l-4-4L4 16v4Z", "M13 7l4 4")
    val Settings = outline("Settings", 1.8f, "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0",
        "M18 6a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z", "M12 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z", "M20 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z")
    val Help = outline("Help", 1.7f, "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z", "M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.7.4-1 .9-1 1.7M12 17h.01")
    val Chat = outline("Chat", 1.7f, "M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12Z")
    val Eye = outline("Eye", 1.7f, "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z", "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z")
    val Discount = outline("Discount", 1.7f, "M19 5 5 19", "M9 7.5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0Z", "M18 16.5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0Z")
    val Up = outline("Up", 2f, "M12 19V5M5 12l7-7 7 7")
    val Down = outline("Down", 2f, "M12 5v14M5 12l7 7 7-7")
    val Tick = outline("Tick", 3.2f, "m5 12 5 5 9-10")
    val Hourglass = outline("Hourglass", 1.5f, "M6 3h12M6 21h12M7 3c0 5 5 6 5 9s-5 4-5 9M17 3c0 5-5 6-5 9s5 4 5 9")
    val IdCard = outline("IdCard", 1.8f, "M6 5h12a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z",
        "M11 10a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z", "M5.5 16a3 3 0 0 1 6 0M14 10h4M14 13h3")
    val Bank = outline("Bank", 1.7f, "M3 10 12 4l9 6M5 10v8M9 10v8M15 10v8M19 10v8M3 20h18")
    val Close = outline("Close", 2f, "M6 6l12 12M18 6 6 18")
    val Phone = outline("PhoneBody", 1.8f, "M9.5 2h5A2.5 2.5 0 0 1 17 4.5v15a2.5 2.5 0 0 1-2.5 2.5h-5A2.5 2.5 0 0 1 7 19.5v-15A2.5 2.5 0 0 1 9.5 2z", "M11 18h2")
    val Globe = outline("Globe", 1.8f, "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z", "M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18")
    val Doc = outline("Doc", 1.8f, "M7 3h7l5 5v13H7z", "M14 3v5h5M10 13h6M10 17h6")
    val SignOut = outline("SignOut", 1.8f, "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9")
    val Clock = outline("Clock", 1.8f, "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z", "M12 7v5l3 2")
    val ThumbUp = outline("ThumbUp", 1.8f, "M7 11v9H4v-9h3Z", "M7 11l4-8a2 2 0 0 1 2 2v4h6a2 2 0 0 1 2 2l-1 6a2 2 0 0 1-2 2H7")
    val WifiOff = outline("WifiOff", 1.6f, "M2 8.8a15 15 0 0 1 20 0M5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0", "m3 3 18 18")
    val Warning = outline("Warning", 1.6f, "M12 3 2 20h20Z", "M12 10v4M12 17h.01")
    val Wrench = outline("Wrench", 1.6f, "M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.4-.6-.6-2.4Z")
    val Download = outline("Download", 1.6f, "M12 3v12M7 10l5 5 5-5M5 21h14")
    val Share = outline("Share", 1.8f, "M21 5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z", "M9 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z", "M21 19a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z", "m8.6 13.5 6.8 4M15.4 6.5l-6.8 4")
    val Link = outline("Link", 1.8f, "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3A4 4 0 0 0 11 18.7l1-1")
    val Mail = outline("Mail", 1.8f, "M6 5h12a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z", "m3 8 9 6 9-6")
    val Bell = outline("BellOutline", 1.6f, "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val ThumbDown = outline("ThumbDown", 1.8f, "M17 13V4h3v9h-3Z", "M17 13l-4 8a2 2 0 0 1-2-2v-4H5a2 2 0 0 1-2-2l1-6a2 2 0 0 1 2-2h11")
    val Group = outline("Group", 1.7f, "M13 8a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z", "M2 21a7 7 0 0 1 14 0M17 11a3 3 0 1 0 0-6M22 21a6 6 0 0 0-4-5.6")
}

/** Back button + page title (Wallet, VIP, Level, Check-in). */
@Composable
fun PageHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackButton(onBack)
        if (title.isNotEmpty()) Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Small tinted rounded icon tile used in menu rows and benefit lists. */
@Composable
fun IconTile(tint: Color, size: Dp = 38.dp, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(size).background(tint.copy(alpha = 0.10f), shape).border(1.dp, tint.copy(alpha = 0.40f), shape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun TileIcon(icon: ImageVector, tint: Color, size: Dp = 38.dp) = IconTile(tint, size) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
}

/** Small uppercase section label ("CHOOSE A PACK"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium, modifier = modifier)
}
