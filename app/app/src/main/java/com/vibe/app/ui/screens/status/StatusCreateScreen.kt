package com.vibe.app.ui.screens.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.MyStatus
import com.vibe.app.data.StatusStore
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

/**
 * New status: text on a colour (or a photo / video picked from gallery or camera in the real app), audience, share.
 * DEMO: nothing is uploaded; the server would store the media, moderate it and remove it after 24 hours.
 */
@Composable
fun StatusCreateScreen(onClose: () -> Unit, onShared: () -> Unit) {
    val ctx = LocalContext.current
    var bg by remember { mutableStateOf(0) }
    var caption by remember { mutableStateOf("") }
    var everyone by remember { mutableStateOf(true) }
    val colors = StatusStore.backgrounds[bg]

    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(colors[0], colors[1], colors[2])))) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.75f)))))

        Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(MeIcons.Close, onClose)
            Box(Modifier.weight(1f))
            GlassIcon(CallIcons.Photo) { ctx.toast("Choose a photo or video from your gallery") }
            GlassIcon(CallIcons.Camera) { ctx.toast("Camera opens here") }
        }

        Box(Modifier.align(Alignment.Center).padding(horizontal = 32.dp)) {
            BasicTextField(
                value = caption, onValueChange = { caption = it.take(120) },
                textStyle = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 34.sp),
                cursorBrush = SolidColor(Color.White), modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (caption.isEmpty()) Text("Type a status", color = Color.White.copy(alpha = 0.5f), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        inner()
                    }
                },
            )
        }

        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                StatusStore.backgrounds.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c[0], c[1])))
                            .border(if (i == bg) 3.dp else 2.dp, if (i == bg) Color.White else Color.White.copy(alpha = 0.35f), CircleShape).clickable { bg = i },
                    )
                }
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AudienceChip("Everyone", everyone, Modifier.weight(1f)) { everyone = true }
                AudienceChip("Followers only", !everyone, Modifier.weight(1f)) { everyone = false }
            }
            val can = caption.trim().isNotEmpty()
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().height(56.dp).alpha(if (can) 1f else 0.45f).clip(RoundedCornerShape(16.dp)).background(VibeBrush.Button)
                    .clickable(enabled = can) {
                        StatusStore.mine = MyStatus(caption.trim(), colors, everyone, 24)
                        ctx.toast("Status shared")
                        onShared()
                    },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(CallIcons.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("  Share status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text(
                "Disappears after 24 hours · Follow the community guidelines", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun GlassIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun AudienceChip(text: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.height(46.dp).clip(shape).background(if (on) VibeColors.Pink.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.3f))
            .border(1.5.dp, if (on) VibeColors.Pink else Color.White.copy(alpha = 0.25f), shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (on) Color.White else Color.White.copy(alpha = 0.8f), fontSize = 13.sp) }
}
