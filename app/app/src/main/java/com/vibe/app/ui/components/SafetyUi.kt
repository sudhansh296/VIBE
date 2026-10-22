package com.vibe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vibe.app.ui.theme.VibeColors

/** "Block {name}?" confirmation. The server stores the block; the blocked person is not told. */
@Composable
fun BlockConfirmDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(VibeColors.Red.copy(alpha = 0.10f)).border(1.dp, VibeColors.Red.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(CallIcons.Ban, null, tint = VibeColors.Red, modifier = Modifier.size(28.dp)) }
            Text("Block $name?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("They can't message or call you", "They won't see you online or in their lists", "They are not told that you blocked them").forEach {
                    Text("•  $it", color = VibeColors.TextDim, fontSize = 13.sp)
                }
            }
            Text(
                "You can unblock anyone from Settings > Blocked users.", color = VibeColors.TextMute, fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A1A26)).border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp)).noRippleClick(onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("Cancel", color = Color.White, fontWeight = FontWeight.SemiBold) }
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(VibeColors.Red, Color(0xFFB3122F)))).noRippleClick(onConfirm),
                    contentAlignment = Alignment.Center,
                ) { Text("Block", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** Simple confirmation dialog (title, message, two buttons). */
@Composable
fun ConfirmDialog(title: String, message: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(24.dp)
        Column(Modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(24.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(message, color = VibeColors.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A1A26)).border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp)).noRippleClick(onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("Cancel", color = Color.White, fontWeight = FontWeight.SemiBold) }
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(com.vibe.app.ui.theme.VibeBrush.Button).noRippleClick(onConfirm),
                    contentAlignment = Alignment.Center,
                ) { Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
