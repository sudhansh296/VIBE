package com.vibe.app.ui.screens.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Host
import com.vibe.app.data.StoryRepo
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.ConfirmDialog
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.launch

/**
 * My status (a host): the statuses I have up, played like a story (tap left / right, hold to pause). The eye opens who viewed it
 * (from the server); delete removes it for everyone; "+" adds another.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyStatusScreen(onClose: () -> Unit, onAdd: () -> Unit, onDeleted: () -> Unit, onEmpty: () -> Unit = onAdd) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val posts = StoryRepo.mine
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { StoryRepo.loadMine() }; loaded = true; if (StoryRepo.mine.isEmpty()) onEmpty() }
    var idx by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var showViews by remember { mutableStateOf(false) }
    var viewers by remember { mutableStateOf<List<Pair<Host, String>>?>(null) }
    if (posts.isEmpty()) { Box(Modifier.fillMaxSize().background(Color.Black)); return }
    val mine = posts[idx.coerceIn(0, posts.lastIndex)]

    com.vibe.app.ui.components.CallStatusBarIcons()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.runtime.key(mine.id) {
            StoryMedia(mine, paused = holding || showViews, onProgress = { progress = it }, onEnd = { if (idx < posts.lastIndex) idx++ else idx = 0 })
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.6f)))))

        Row(Modifier.fillMaxSize().padding(top = 110.dp, bottom = 130.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().pointerInput(idx) { detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false }, onTap = { if (idx > 0) idx-- }) })
            Box(Modifier.weight(2f).fillMaxHeight().pointerInput(idx) { detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false }, onTap = { if (idx < posts.lastIndex) idx++ }) })
        }
        mine.caption?.let {
            Text(it, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 25.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp).padding(horizontal = 32.dp))
        }

        Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                posts.forEachIndexed { i, _ ->
                    val fill = if (i < idx) 1f else if (i == idx) progress else 0f
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(Color.White))
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your status", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("${idx + 1} of ${posts.size} · ${storyAgo(mine.at)} · seen by users", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    Icon(MeIcons.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = 0.35f)).clickable {
                    showViews = true; viewers = null
                    scope.launch { viewers = runCatching { StoryRepo.viewers(mine.id) }.getOrDefault(emptyList()) }
                }.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(MeIcons.Eye, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("  ${mine.views}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(VibeBrush.Button).clickable(onClick = onAdd), contentAlignment = Alignment.Center) {
                    Text("+", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Light)
                }
            }
        }
    }

    if (showViews) ModalBottomSheet(onDismissRequest = { showViews = false }, containerColor = VibeColors.Card) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(MeIcons.Eye, null, tint = Color(0xFF5A3F4D), modifier = Modifier.size(20.dp))
                Text("  ${(viewers?.size ?: mine.views).let { if (it == 1) "1 view" else "$it views" }}", color = VibeColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row(
                    Modifier.height(36.dp).clip(CircleShape).border(1.dp, VibeColors.Red.copy(alpha = 0.5f), CircleShape).clickable { askDelete = true }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(CallIcons.Trash, null, tint = VibeColors.Red, modifier = Modifier.size(16.dp))
                    Text("  Delete", color = VibeColors.Red, fontSize = 12.sp)
                }
            }
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                val list = viewers
                when {
                    list == null -> Text("Loading…", color = VibeColors.TextMute, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
                    list.isEmpty() -> Text("No views yet", color = VibeColors.TextMute, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
                    else -> list.forEach { (h, at) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(h, Modifier.size(44.dp).clip(CircleShape), fontSize = 16.sp)
                            Column(Modifier.weight(1f)) {
                                Text(h.name, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(storyAgo(at), color = VibeColors.TextMute, fontSize = 11.sp)
                            }
                            if (h.vip) Text("VIP", color = Color(0xFF7A4B00), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(CircleShape).background(Color(0xFFFFE9A8)).padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Line))
                    }
                }
            }
        }
    }

    if (askDelete) ConfirmDialog(
        title = "Delete this status?", message = "It will disappear for everyone right away.", confirmText = "Delete",
        onConfirm = {
            askDelete = false; showViews = false
            scope.launch {
                runCatching { StoryRepo.delete(mine.id) }.onSuccess {
                    ctx.toast("Status deleted")
                    if (StoryRepo.mine.isEmpty()) onDeleted() else idx = idx.coerceAtMost(StoryRepo.mine.lastIndex)
                }.onFailure { ctx.toast(it.message ?: "Could not delete") }
            }
        },
        onDismiss = { askDelete = false },
    )
}
