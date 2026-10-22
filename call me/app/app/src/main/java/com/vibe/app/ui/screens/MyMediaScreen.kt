package com.vibe.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.HostRepo
import kotlinx.coroutines.launch
import com.vibe.app.data.ServerMedia
import com.vibe.app.data.api.Api
import com.vibe.app.data.api.ApiException
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.ServerImage
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import org.json.JSONObject

/** The server's limits for photos and videos (admin editable). Nothing here is decided by the app. */
private data class Limits(
    val maxPhotos: Int = 6, val maxVideos: Int = 6, val freePhotos: Int = 3, val freeVideos: Int = 1, val pricePhoto: Int = 0, val priceVideo: Int = 0,
    val maxVideoSeconds: Int = 30, val minPhotos: Int = 3, val minVideos: Int = 1,
)

private fun limitsOf(o: JSONObject?) = if (o == null) Limits() else Limits(
    o.optInt("maxPhotos", 6), o.optInt("maxVideos", 6), o.optInt("freePhotos", 3), o.optInt("freeVideos", 1), o.optInt("pricePhoto"), o.optInt("priceVideo"),
    o.optInt("maxVideoSeconds", 30), o.optInt("minPhotos", 3), o.optInt("minVideos", 1),
)

/**
 * My photos & videos: add from the gallery, lock (users pay coins to unlock), main photo, delete. Every action goes to the server,
 * which checks the limits (how many, how long, how big), the always-free ones and the minimum a host must keep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMediaScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("photo") }
    var photos by remember { mutableStateOf<List<ServerMedia>>(emptyList()) }
    var videos by remember { mutableStateOf<List<ServerMedia>>(emptyList()) }
    var lim by remember { mutableStateOf(Limits()) }
    var share by remember { mutableStateOf(60) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ServerMedia?>(null) }

    fun apply(res: JSONObject) {
        photos = List(res.getJSONArray("photos").length()) { HostRepo.item(res.getJSONArray("photos").getJSONObject(it), false) }
        videos = List(res.getJSONArray("videos").length()) { HostRepo.item(res.getJSONArray("videos").getJSONObject(it), true) }
        lim = limitsOf(res.optJSONObject("limits"))
    }
    /** Runs one server action with a busy spinner; the server answers with the fresh lists. */
    fun act(done: String? = null, block: suspend () -> JSONObject) {
        if (busy) return
        busy = true
        scope.launch {
            try { apply(block()); if (done != null) ctx.toast(done) } catch (e: ApiException) { ctx.toast(e.message) }
            busy = false
        }
    }
    LaunchedEffect(Unit) {
        try {
            apply(Api.get("/me/media"))
            share = runCatching { Api.get("/config", auth = false).getJSONObject("config").optInt("host_share_pct", 60) }.getOrDefault(60)
        } catch (e: ApiException) { failed = e.message }
        loading = false
    }

    val isPhoto = tab == "photo"
    val list = if (isPhoto) photos else videos
    val max = if (isPhoto) lim.maxPhotos else lim.maxVideos
    val price = if (isPhoto) lim.pricePhoto else lim.priceVideo

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = ctx.contentResolver.getType(uri) ?: if (isPhoto) "image/jpeg" else "video/mp4"
        act(if (isPhoto) "Photo added" else "Video added") {
            if (isPhoto) Api.upload("/me/media", "file", "photo.jpg", "image/jpeg", com.vibe.app.data.api.Uploads.photoBytes(ctx, uri))
            else Api.upload("/me/media", "file", "video.mp4", mime, com.vibe.app.data.api.Uploads.videoBytes(ctx, uri))
        }
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Photos & videos", onBack)

            if (loading) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 3.dp, modifier = Modifier.size(32.dp)) }
            else if (failed != null) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(failed.orEmpty(), color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
            } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(Color(0x33F2B84B), VibeColors.Card, VibeColors.Card)))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.3f), shape).padding(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(CallIcons.Lock, null, tint = VibeColors.Gold, modifier = Modifier.size(24.dp))
                        Text(
                            "Free for everyone: your first ${lim.freePhotos} photos and ${lim.freeVideos} video.\n" +
                                "Lock the rest: users pay coins to unlock, you earn $share% of every unlock.",
                            color = Color(0xFF5A3F4D), fontSize = 12.sp, lineHeight = 18.sp,
                        )
                    }
                }

                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    listOf("photo" to "Photos ${photos.size}/${lim.maxPhotos}", "video" to "Videos ${videos.size}/${lim.maxVideos}").forEach { (k, l) ->
                        Column(Modifier.noRippleClick { tab = k }) {
                            Text(l.uppercase(), color = if (tab == k) VibeColors.Ink else VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
                            Box(
                                Modifier.padding(top = 10.dp).height(2.dp).size(width = 72.dp, height = 2.dp)
                                    .then(if (tab == k) Modifier.background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))) else Modifier),
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.10f)))

                val cells = buildList<Int> {
                    if (list.size < max) add(-1)
                    addAll(list.indices)
                }
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cells.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { idx ->
                                if (idx == -1) AddTile(if (isPhoto) "photo" else "video", Modifier.weight(1f)) {
                                    if (!busy) picker.launch(PickVisualMediaRequest(if (isPhoto) ActivityResultContracts.PickVisualMedia.ImageOnly else ActivityResultContracts.PickVisualMedia.VideoOnly))
                                }
                                else MediaTile(list[idx], isPhoto, price, Modifier.weight(1f)) { selected = list[idx] }
                            }
                            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
                Text(
                    if (isPhoto) "At least ${lim.minPhotos} photos. The first ${lim.freePhotos} are always free. Others can be locked at ${lim.pricePhoto} coins each."
                    else "The first ${lim.freeVideos} video is free. Videos are up to ${lim.maxVideoSeconds} seconds. Others can be locked at ${lim.priceVideo} coins each.",
                    color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "New photos and videos go live right away. Limits, unlock prices and your share are set by VIBE.",
                    color = Color(0xFFB093A2), fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                )
            }
        }
        if (busy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)).clickable(enabled = true) {}, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
        }
    }

    selected?.let { x ->
        val i = list.indexOfFirst { it.id == x.id }
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = VibeColors.Card) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
                Text("${if (isPhoto) "Photo" else "Video"} ${i + 1}", color = VibeColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (isPhoto && !x.isMain && !x.lockedByHost) SheetAction(MeIcons.Star, VibeColors.Gold, "Set as main photo", "Shown first on your card and profile") {
                    selected = null; act("Main photo updated") { Api.post("/me/media/${x.id}/main") }
                }
                if (!x.free) {
                    if (x.lockedByHost) SheetAction(MeIcons.Eye, VibeColors.Green, "Make it free", "Everyone can see it") {
                        selected = null; act("Now free for everyone") { Api.put("/me/media/${x.id}", JSONObject().put("locked", false)) }
                    } else SheetAction(CallIcons.Lock, VibeColors.Gold, "Lock it · $price coins", "Users pay to see it and you earn") {
                        selected = null; act("Locked · users pay to unlock") { Api.put("/me/media/${x.id}", JSONObject().put("locked", true)) }
                    }
                } else Text(
                    if (isPhoto) "The first ${lim.freePhotos} photos are always free." else "The first ${lim.freeVideos} video is always free.",
                    color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
                SheetAction(CallIcons.Trash, VibeColors.Red, "Delete", null, danger = true) {
                    selected = null; act("Deleted") { Api.delete("/me/media/${x.id}") }
                }
            }
        }
    }
}

@Composable
private fun AddTile(kind: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.aspectRatio(3f / 4f).clip(shape).background(VibeColors.Card).border(1.5.dp, Color(0xFFFFD6E3), shape).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text("+", color = VibeColors.Pink, fontSize = 28.sp, fontWeight = FontWeight.Light)
        Text("Add $kind", color = VibeColors.Pink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MediaTile(x: ServerMedia, isPhoto: Boolean, price: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.aspectRatio(3f / 4f).clip(shape).background(Brush.linearGradient(listOf(Color(0xFFFF9EC3), Color(0xFFFF4D8D)))).border(1.5.dp, VibeColors.Line, shape).clickable(onClick = onClick)) {
        ServerImage(x.path, Modifier.fillMaxSize(), -0.4f)   // a video shows its first frame
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.18f), 0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.25f))))
        if (!isPhoto) Box(Modifier.align(Alignment.Center).size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
            Text("▶", color = VibeColors.Pink, fontSize = 16.sp)
        }
        Column(Modifier.align(Alignment.TopStart).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!x.lockedByHost) Badge("FREE", Color.White, Color(0xFF12A150), Modifier)
            else Row(
                Modifier.clip(CircleShape).background(Brush.horizontalGradient(listOf(VibeColors.Gold, Color(0xFFEAB308)))).padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(CallIcons.Lock, null, tint = Color.White, modifier = Modifier.size(11.dp))
                Text(" $price", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (isPhoto && x.isMain) Badge("MAIN", VibeColors.Pink, Color.White, Modifier)
        }
        Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.spacedBy(2.5.dp)) { repeat(3) { Box(Modifier.size(3.5.dp).clip(CircleShape).background(VibeColors.Pink)) } }
        }
        if (!isPhoto) Text(
            "0:%02d".format(x.durationSec), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))).padding(top = 18.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
        )
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color, modifier: Modifier) {
    Text(text, color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, modifier = modifier.clip(CircleShape).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
private fun SheetAction(icon: ImageVector, tint: Color, title: String, sub: String?, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        com.vibe.app.ui.components.TileIcon(icon, tint, 38.dp)
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) VibeColors.Red else VibeColors.Ink, fontSize = 15.sp)
            if (sub != null) Text(sub, color = VibeColors.TextMute, fontSize = 11.sp)
        }
        Text("›", color = Color(0xFFB093A2), fontSize = 18.sp)
    }
}
