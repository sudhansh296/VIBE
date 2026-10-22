package com.vibe.app.ui.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.MediaCfg
import com.vibe.app.data.MediaItem
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CallSheetBg
import com.vibe.app.ui.components.CoinDot
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.noRippleClick
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

private val SHADES = listOf(
    VibeColors.Pink to VibeColors.Violet, VibeColors.Cyan to Color(0xFF1E3A8A), Color(0xFFF59E0B) to Color(0xFF9A3412),
    Color(0xFFA855F7) to Color(0xFF581C87), Color(0xFF10B981) to Color(0xFF065F46), Color(0xFFF43F5E) to Color(0xFF86198F), Color(0xFF38BDF8) to Color(0xFF1E40AF),
)

/**
 * My photos & videos (host): add, lock (users pay to unlock), replace, delete, main photo, review status.
 * DEMO; limits, prices and review results come from the server / admin panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMediaScreen(onBack: () -> Unit, onPreview: () -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf("photo") }
    val photos = remember { mutableStateListOf(MediaItem(0), MediaItem(1), MediaItem(2), MediaItem(3, locked = true), MediaItem(4, st = "review")) }
    val videos = remember { mutableStateListOf(MediaItem(1, intro = true, len = 15), MediaItem(3, locked = true, len = 24), MediaItem(4, st = "review", len = 18)) }
    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(-1) }

    val isPhoto = tab == "photo"
    val list = if (isPhoto) photos else videos
    val max = if (isPhoto) MediaCfg.MAX_PHOTOS else MediaCfg.MAX_VIDEOS
    fun isFree(i: Int, it: MediaItem) = if (isPhoto) i < MediaCfg.FREE_PHOTOS else it.intro

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { PageHeader("Photos & videos", onBack) }
                Row(
                    Modifier.height(36.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable(onClick = onPreview).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(MeIcons.Eye, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("  Preview", color = Color.White, fontSize = 12.sp)
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                // how it works + earnings
                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(listOf(Color(0x4D784C0A), VibeColors.Card, VibeColors.Card)))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.3f), shape).padding(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(CallIcons.Lock, null, tint = VibeColors.Gold, modifier = Modifier.size(24.dp))
                        Text(
                            "Free for everyone: your first ${MediaCfg.FREE_PHOTOS} photos and intro video.\n" +
                                "Lock the rest: users pay coins to unlock, you earn ${MediaCfg.SHARE} of every unlock.",
                            color = Color(0xFFD1D5DB), fontSize = 12.sp, lineHeight = 18.sp,
                        )
                    }
                    Row(Modifier.padding(top = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Unlock earnings this week", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        CoinDot(16.dp)
                        Text(" 1,240", color = VibeColors.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // tabs
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    listOf("photo" to "Photos ${photos.size}/${MediaCfg.MAX_PHOTOS}", "video" to "Videos ${videos.size}/${MediaCfg.MAX_VIDEOS}").forEach { (k, l) ->
                        Column(Modifier.noRippleClick { tab = k }) {
                            Text(l.uppercase(), color = if (tab == k) Color.White else VibeColors.TextMute, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
                            Box(
                                Modifier.padding(top = 10.dp).height(2.dp).size(width = 72.dp, height = 2.dp)
                                    .then(if (tab == k) Modifier.background(Brush.horizontalGradient(listOf(VibeColors.Pink, VibeColors.Cyan))) else Modifier),
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))

                // grid: Add tile first (until the limit), then the items, 3 per row
                val cells = buildList<Int> {
                    if (list.size < max) add(-1)
                    addAll(list.indices)
                }
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cells.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { idx ->
                                if (idx == -1) AddTile(if (isPhoto) "photo" else "video", Modifier.weight(1f)) { showAdd = true }
                                else MediaTile(list[idx], idx, isPhoto, isFree(idx, list[idx]), Modifier.weight(1f)) { selected = idx }
                            }
                            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
                Text(
                    if (isPhoto) "At least ${MediaCfg.MIN_PHOTOS} photos. Photos 1 to ${MediaCfg.FREE_PHOTOS} are always free. Others can be locked at ${MediaCfg.PRICE_PHOTO} coins each."
                    else "Intro video is free (${MediaCfg.INTRO_SECONDS} sec). Other videos are up to ${MediaCfg.VIDEO_SECONDS} sec and can be locked at ${MediaCfg.PRICE_VIDEO} coins each.",
                    color = VibeColors.TextMute, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "Every new photo or video is checked by the VIBE team before it goes live. Limits, unlock prices and your share are set by VIBE.",
                    color = Color(0xFF4B5563), fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                )
            }
        }
    }

    if (showAdd) ModalBottomSheet(onDismissRequest = { showAdd = false }, containerColor = CallSheetBg) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
            Text("Add ${if (isPhoto) "photo" else "video"}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
            fun added(msg: String) {
                list.add(MediaItem(list.size + 2, st = "review"))
                showAdd = false
                ctx.toast(msg)
            }
            if (isPhoto) {
                SheetAction(CallIcons.Camera, VibeColors.Pink, "Take a photo", "Front or back camera") { added("Camera opens here") }
                SheetAction(CallIcons.Photo, VibeColors.Cyan, "Choose from gallery", "Clear photos of yourself only") { added("Gallery opens here") }
            } else {
                SheetAction(CallIcons.Camera, VibeColors.Pink, "Record a video", "Up to ${MediaCfg.VIDEO_SECONDS} seconds") { added("Camera opens · ${MediaCfg.VIDEO_SECONDS} sec max") }
                SheetAction(CallIcons.Photo, VibeColors.Cyan, "Video from gallery", "Up to ${MediaCfg.VIDEO_SECONDS} seconds") { added("Gallery opens here") }
            }
        }
    }

    if (selected in list.indices) {
        val i = selected
        val x = list[i]
        val free = isFree(i, x)
        val ok = x.st == "ok"
        ModalBottomSheet(onDismissRequest = { selected = -1 }, containerColor = CallSheetBg) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
                Text(
                    "${if (isPhoto) "Photo" else "Video"} ${i + 1}" + if (x.st == "review") " · In review" else if (x.st == "bad") " · Rejected" else "",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                )
                if (x.st == "bad") Text(
                    "Reason: ${x.why.ifEmpty { "Not allowed" }}. Replace it with a new one.", color = Color(0xFFD1D5DB), fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(VibeColors.Red.copy(alpha = 0.06f)).border(1.dp, VibeColors.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
                )
                if (isPhoto && i != 0 && ok) SheetAction(MeIcons.Star, VibeColors.Gold, "Set as main photo", "Shown first on your profile") {
                    list.removeAt(i); list.add(0, x); selected = -1; ctx.toast("Main photo updated")
                }
                if (ok && !free) {
                    if (x.locked) SheetAction(MeIcons.Eye, VibeColors.Green, "Make it free", "Everyone can see it") { list[i] = x.copy(locked = false); selected = -1; ctx.toast("Now free for everyone") }
                    else SheetAction(CallIcons.Lock, VibeColors.Gold, "Lock it · ${if (isPhoto) MediaCfg.PRICE_PHOTO else MediaCfg.PRICE_VIDEO} coins", "Users pay to see it and you earn") {
                        list[i] = x.copy(locked = true); selected = -1; ctx.toast("Locked · users pay to unlock")
                    }
                }
                if (ok && free) Text(
                    if (isPhoto) "The first ${MediaCfg.FREE_PHOTOS} photos are always free." else "The intro video is always free.",
                    color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
                SheetAction(CallIcons.Flip, VibeColors.Cyan, "Replace", "Pick a new one (needs review)") { list[i] = x.copy(st = "review", why = ""); selected = -1; ctx.toast("Sent for review") }
                SheetAction(CallIcons.Trash, VibeColors.Red, "Delete", null, danger = true) {
                    when {
                        isPhoto && photos.size <= MediaCfg.MIN_PHOTOS -> ctx.toast("Keep at least ${MediaCfg.MIN_PHOTOS} photos")
                        !isPhoto && x.intro -> ctx.toast("Replace the intro video instead of deleting it")
                        else -> { list.removeAt(i); selected = -1; ctx.toast("Deleted") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTile(kind: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.aspectRatio(3f / 4f).clip(shape).background(VibeColors.Card).border(1.5.dp, Color(0xFF3A3A4C), shape).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text("+", color = VibeColors.Pink, fontSize = 28.sp, fontWeight = FontWeight.Light)
        Text("Add $kind", color = VibeColors.Pink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MediaTile(x: MediaItem, index: Int, isPhoto: Boolean, free: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val (a, b) = SHADES[x.shade % SHADES.size]
    Box(modifier.aspectRatio(3f / 4f).clip(shape).background(Brush.linearGradient(listOf(a, b))).border(1.5.dp, VibeColors.Line, shape).clickable(onClick = onClick)) {
        Icon(
            if (isPhoto) CallIcons.User else CallIcons.Camera, null, tint = Color.White.copy(alpha = if (x.st == "bad") 0.3f else 0.7f),
            modifier = Modifier.align(Alignment.Center).size(34.dp),
        )
        if (x.st == "ok") {
            if (free || !x.locked) Badge("FREE", Color(0xE63EE08F), Color(0xFF04180D), Modifier.align(Alignment.TopStart).padding(6.dp))
            else Row(
                Modifier.align(Alignment.TopStart).padding(6.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(VibeColors.Gold, Color(0xFFEAB308)))).padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(CallIcons.Lock, null, tint = Color.Black, modifier = Modifier.size(11.dp))
                Text(" ${if (isPhoto) MediaCfg.PRICE_PHOTO else MediaCfg.PRICE_VIDEO}", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (isPhoto && index == 0 && x.st == "ok") Badge("MAIN", Color.Black.copy(alpha = 0.55f), Color.White, Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 32.dp))
        Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            Text("⋯", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        val (label, color) = when {
            x.st == "review" -> "In review" to VibeColors.Gold
            x.st == "bad" -> "Rejected\n${x.why}" to VibeColors.Red
            !isPhoto -> "0:%02d".format(x.len) + (if (x.intro) " · Intro" else "") to Color.White.copy(alpha = 0.8f)
            else -> "" to Color.White
        }
        if (label.isNotEmpty()) Text(
            label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 13.sp,
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
            Text(title, color = if (danger) VibeColors.Red else Color.White, fontSize = 15.sp)
            if (sub != null) Text(sub, color = VibeColors.TextMute, fontSize = 11.sp)
        }
        Text("›", color = Color(0xFF4B5563), fontSize = 18.sp)
    }
}
