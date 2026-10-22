package com.vibe.app.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Host
import com.vibe.app.data.HostRepo
import com.vibe.app.data.HostStatus
import com.vibe.app.data.api.Api
import com.vibe.app.data.api.ApiException
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.VibeIcons
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private data class Visit(val who: Host, val whenMs: Long, val times: Int)

private fun parseTime(iso: String): Long = try {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(iso.take(19))?.time ?: 0L
} catch (e: Exception) { 0L }

private fun dayLabel(ms: Long): String {
    val day = 86_400_000L
    val today = (System.currentTimeMillis() + TimeZone.getDefault().rawOffset) / day
    val d = (ms + TimeZone.getDefault().rawOffset) / day
    return when (today - d) { 0L -> "Today"; 1L -> "Yesterday"; in 2L..6L -> "This week"; else -> "Earlier" }
}

/** Visitors: everyone who viewed your profile, newest first, with online status. The list comes from the server (blocked people never appear). */
@Composable
fun VisitorsScreen(onBack: () -> Unit, onChat: (Int) -> Unit, onCall: (Int) -> Unit, onVip: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf<String?>(null) }
    var total by remember { mutableStateOf(0) }
    var visits by remember { mutableStateOf<List<Visit>>(emptyList()) }
    var locked by remember { mutableStateOf(false) }   // not VIP: the server sends only how many, never who

    LaunchedEffect(Unit) {
        try {
            val res = Api.get("/me/visitors")
            total = res.optInt("total"); locked = res.optBoolean("locked")
            val arr = res.getJSONArray("visitors")
            visits = List(arr.length()) {
                val o = arr.getJSONObject(it)
                val h = HostRepo.fromJson(o); HostRepo.cache[h.id] = HostRepo.cache[h.id] ?: h
                Visit(HostRepo.cache[h.id] ?: h, parseTime(o.optString("visitedAt")), o.optInt("visits", 1))
            }
        } catch (e: ApiException) { failed = e.message }
        loading = false
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Visitors", onBack)
            if (loading) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            } else if (failed != null) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(failed.orEmpty(), color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
            } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Row {
                    Text("$total", color = VibeColors.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(" people viewed your profile", color = VibeColors.TextDim, fontSize = 14.sp)
                }
                if (locked && total > 0) LockedVisitors(total, onVip)
                else if (visits.isEmpty()) Text("No visitors yet. Add more photos to get noticed.", color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp))
                var last = ""
                visits.forEach { v ->
                    val g = dayLabel(v.whenMs)
                    if (g != last) {
                        last = g
                        Text(g, color = VibeColors.TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                    }
                    VisitorRow(v, onChat, onCall)
                }
                Box(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun VisitorRow(v: Visit, onChat: (Int) -> Unit, onCall: (Int) -> Unit) {
    val h = v.who
    val ago = android.text.format.DateUtils.getRelativeTimeSpanString(v.whenMs, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS).toString()
    val canCall = h.verified && h.status == HostStatus.ONLINE   // only hosts can be called, and only while online
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp)) {
                Avatar(h, Modifier.size(52.dp).clip(CircleShape), fontSize = 20.sp)
                if (h.verified && h.status == HostStatus.ONLINE) Box(Modifier.align(Alignment.BottomEnd).size(13.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.dp).clip(CircleShape).background(VibeColors.Green))
            }
            Column(Modifier.weight(1f)) {
                Text(com.vibe.app.ui.components.nameAge(h.name, h.age), color = VibeColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Viewed $ago" + if (v.times > 1) " · ${v.times} visits" else "", color = VibeColors.TextMute, fontSize = 11.sp)
            }
        }
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Card).border(1.dp, VibeColors.Line, RoundedCornerShape(12.dp)).clickable { onChat(h.id) },
            contentAlignment = Alignment.Center,
        ) { Icon(MeIcons.Chat, null, tint = Color(0xFF5A3F4D), modifier = Modifier.size(20.dp)) }
        if (canCall) Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(VibeBrush.Button).clickable { onCall(h.id) },
            contentAlignment = Alignment.Center,
        ) { Icon(VibeIcons.Video, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.05f)))
}

/** Not VIP: blurred stand-ins (no names or photos come from the server) and the way to see who it was. */
@Composable
private fun LockedVisitors(total: Int, onVip: () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    Column(Modifier.padding(top = 16.dp)) {
        repeat(minOf(total, 5)) { i ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFFC2DD), androidx.compose.ui.graphics.Color(0xFFFF8FB8)))),
                )
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Box(Modifier.size(width = (90 + (i * 23) % 50).dp, height = 12.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).background(VibeColors.Ink.copy(alpha = 0.10f)))
                    Box(Modifier.padding(top = 6.dp).size(width = 60.dp, height = 10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp)).background(VibeColors.Ink.copy(alpha = 0.06f)))
                }
            }
        }
        Column(
            Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape)
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(VibeColors.Gold.copy(alpha = 0.16f), VibeColors.Card)))
                .border(1.dp, VibeColors.Gold.copy(alpha = 0.5f), shape).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("See who visited you", color = VibeColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("The full visitor list is a VIP benefit.", color = VibeColors.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Box(
                Modifier.padding(top = 14.dp).height(44.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(VibeColors.Gold, androidx.compose.ui.graphics.Color(0xFFE0A030))))
                    .clickable(onClick = onVip).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Get VIP", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
    }
}
