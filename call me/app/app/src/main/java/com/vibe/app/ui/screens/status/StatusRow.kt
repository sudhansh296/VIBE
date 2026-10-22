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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.Host
import com.vibe.app.data.StoryRepo
import com.vibe.app.ui.components.Avatar
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

private enum class Ring { NEW, SEEN, NONE }

/**
 * Home status row, from the server: "My status" for hosts (they post), then hosts with a status up (ring pink to cyan = new,
 * grey = all seen). Users see hosts' statuses; the row hides itself when there is nothing to show.
 */
@Composable
fun StatusRow(onMine: () -> Unit, onView: (Int) -> Unit, onHost: (Int) -> Unit) {
    LaunchedEffect(Unit) { runCatching { StoryRepo.load() } }
    val people = StoryRepo.people
    val canPost = StoryRepo.canPost
    if (!canPost && people.isEmpty()) return
    val hasMine = StoryRepo.mine.isNotEmpty()

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text("Status", color = Color(0xFF5A3F4D), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Disappears in 24 hours", color = VibeColors.TextMute, fontSize = 11.sp)
        }
        LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (canPost) item {
                Cell("My status", onMine) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        RingBorder(if (hasMine) Ring.NEW else Ring.NONE, dashed = !hasMine)
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFD9E6), Color(0xFFFFC2D8)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            com.vibe.app.ui.components.MyDp(Modifier.fillMaxSize(), (com.vibe.app.data.api.AuthStore.me?.optString("name")?.takeIf { it.isNotBlank() && it != "null" } ?: "S").take(1), 20.sp)
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape).background(VibeBrush.Button).border(2.dp, VibeColors.Dark, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    }
                }
            }
            items(people.size) { i ->
                val p = people[i]
                Cell(p.person.name, { onView(i) }) { Face(p.person, if (p.allSeen) Ring.SEEN else Ring.NEW) }
            }
        }
    }
}

@Composable
private fun Cell(label: String, onClick: () -> Unit, avatar: @Composable () -> Unit) {
    Column(Modifier.width(64.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        avatar()
        Text(label, color = VibeColors.TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Face(h: Host, ring: Ring) {
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        RingBorder(ring, dashed = false)
        Avatar(h, Modifier.size(56.dp).clip(CircleShape), fontSize = 20.sp)
        Box(Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(VibeColors.Dark).padding(2.5.dp).clip(CircleShape).background(h.status.dot))
    }
}

@Composable
private fun RingBorder(ring: Ring, dashed: Boolean) {
    val m = Modifier.size(64.dp).clip(CircleShape)
    when (ring) {
        Ring.NEW -> Box(m.border(2.dp, Brush.sweepGradient(listOf(VibeColors.Pink, VibeColors.Cyan, VibeColors.Pink)), CircleShape))
        Ring.SEEN -> Box(m.border(2.dp, VibeColors.Ink.copy(alpha = 0.2f), CircleShape))
        Ring.NONE -> Box(m.border(1.dp, VibeColors.Ink.copy(alpha = if (dashed) 0.25f else 0.1f), CircleShape))
    }
}
