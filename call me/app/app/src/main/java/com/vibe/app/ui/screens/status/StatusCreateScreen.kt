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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.StoryRepo
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.PickedMedia
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * New status (hosts): pick a photo or a video from the gallery, add a caption, share. The server checks the file (type, size,
 * video length from the admin settings), keeps it for the admin's hours and shows it to users.
 */
@Composable
fun StatusCreateScreen(onClose: () -> Unit, onShared: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<android.net.Uri?>(null) }
    var video by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            video = PickedMedia.isVideo(ctx, uri)
            picked = PickedMedia.copy(ctx, uri, "status_")
        }
    }
    fun openGallery() = pick.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo))

    com.vibe.app.ui.components.CallStatusBarIcons()
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF3B0A2A), Color(0xFF6B1240), Color(0xFF1A0B2E))))) {
        picked?.let { com.vibe.app.ui.components.UriImage(it, Modifier.fillMaxSize(), video) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.75f)))))

        Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(MeIcons.Close, onClose)
            Box(Modifier.weight(1f))
            if (picked != null) GlassIcon(CallIcons.Photo) { openGallery() }
        }

        if (picked == null) Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape).clickable { openGallery() },
                contentAlignment = Alignment.Center,
            ) { Icon(CallIcons.Photo, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
            Text("Choose a photo or video", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
            Text("Users see it on Home for 24 hours", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        } else if (video) Box(Modifier.align(Alignment.Center).size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
            Text("▶", color = VibeColors.Pink, fontSize = 28.sp)
        }

        if (picked != null) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding().navigationBarsPadding().padding(16.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                BasicTextField(
                    value = caption, onValueChange = { caption = it.take(150) },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center), cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { if (caption.isEmpty()) Text("Add a caption (optional)", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp); inner() } },
                )
            }
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().height(56.dp).alpha(if (sending) 0.6f else 1f).clip(RoundedCornerShape(16.dp)).background(VibeBrush.Button)
                    .clickable(enabled = !sending) {
                        val uri = picked ?: return@clickable
                        sending = true
                        scope.launch {
                            runCatching {
                                val bytes = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() } }
                                StoryRepo.post(bytes, if (video) "video/mp4" else "image/jpeg", caption.trim())
                            }.onSuccess { ctx.toast("Status shared"); onShared() }
                                .onFailure { ctx.toast(it.message ?: "Could not share the status") }
                            sending = false
                        }
                    },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(CallIcons.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(if (sending) "  Sharing…" else "  Share status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text("Disappears after 24 hours · Follow the community guidelines", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
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
