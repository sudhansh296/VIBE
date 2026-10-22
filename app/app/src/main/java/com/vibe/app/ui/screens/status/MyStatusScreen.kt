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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.StatusStore
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.ConfirmDialog
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

private class Viewer(val name: String, val time: String, val tag: String, val c1: Color, val c2: Color)

private val VIEWERS = listOf(
    Viewer("Aisha", "2m ago", "heart", VibeColors.Pink, VibeColors.Violet),
    Viewer("Kavya", "10m ago", "Replied", Color(0xFFA855F7), Color(0xFF581C87)),
    Viewer("Sana", "18m ago", "", Color(0xFFF6D365), Color(0xFF9A3412)),
    Viewer("Zoya", "31m ago", "heart", Color(0xFFF43F5E), Color(0xFF86198F)),
)

/** My status: preview, views list, expires in, delete. DEMO; views, hearts and replies are recorded by the server. */
@Composable
fun MyStatusScreen(onClose: () -> Unit, onAdd: () -> Unit, onDeleted: () -> Unit) {
    val ctx = LocalContext.current
    val mine = StatusStore.mine
    var askDelete by remember { mutableStateOf(false) }
    val bg = mine?.bg ?: StatusStore.backgrounds[4]

    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(bg[0], bg[1], bg[2])))) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.6f)))))

        Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your status", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("35m ago · ${if (mine?.everyone == false) "Followers only" else "Everyone"}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(MeIcons.Close, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        }

        Text(
            mine?.text ?: "Weekend vibes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 32.sp,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 96.dp, start = 32.dp, end = 32.dp),
        )

        // views sheet
        val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = 460.dp).clip(shape).background(VibeColors.Card.copy(alpha = 0.97f))
                .border(1.dp, VibeColors.Line, shape).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(MeIcons.Eye, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(20.dp))
                Text("  ${mine?.views ?: 24} views", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row(
                    Modifier.height(36.dp).clip(CircleShape).border(1.dp, VibeColors.Red.copy(alpha = 0.5f), CircleShape).clickable { askDelete = true }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(CallIcons.Trash, null, tint = VibeColors.Red, modifier = Modifier.size(16.dp))
                    Text("  Delete", color = VibeColors.Red, fontSize = 12.sp)
                }
            }
            Text("Expires in 23h 25m", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))

            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                VIEWERS.forEach { v ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(v.c1, v.c2))), contentAlignment = Alignment.Center) {
                            Text(v.name.take(1), color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(v.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(v.time, color = VibeColors.TextMute, fontSize = 11.sp)
                        }
                        if (v.tag == "heart") Text("♥", color = VibeColors.Pink, fontSize = 20.sp)
                        else if (v.tag.isNotEmpty()) Box(Modifier.clip(CircleShape).background(VibeColors.Pink.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(v.tag, color = VibeColors.Pink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
                }
            }
            Box(
                Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A1A26)).border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp)).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Text("Add new status", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
        }
    }

    if (askDelete) ConfirmDialog(
        title = "Delete this status?", message = "It will disappear for everyone right away.", confirmText = "Delete",
        onConfirm = { askDelete = false; StatusStore.mine = null; ctx.toast("Status deleted"); onDeleted() }, onDismiss = { askDelete = false },
    )
}
