package com.vibe.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

// DEMO data only (notifications list, until Phase 7). Chats are real since Phase 4; call history comes with real calls (sockets for live updates);
// message prices, free-message counts and coin charges are decided by the server.

/** action: "call:<hostId>", "chat:<hostId>" or "follow". Text uses **bold** markers. */
data class AppNotification(
    val id: Int,
    val group: String,
    val type: String, // calls | social | wallet | system
    val tone: String, // missed | call | social | wallet | system | online
    val icon: String,
    val hostId: Int?,
    val text: String,
    val time: String,
    val action: String? = null,
    val actionLabel: String = "",
    val primary: Boolean = false,
    val unread: Boolean = false,
)

/** "Hi **Priya**" -> Hi + bold white Priya. */
fun boldText(s: String): AnnotatedString = buildAnnotatedString {
    s.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(color = com.vibe.app.ui.theme.VibeColors.Ink, fontWeight = FontWeight.SemiBold)) { append(part) } else append(part)
    }
}

object ChatDemo {
    val notifications = listOf(
        AppNotification(1, "Today", "calls", "missed", "missed", 4, "**Riya** missed your video call", "9:41 pm", "call:4:video", "Call back", primary = true, unread = true),
        AppNotification(2, "Today", "social", "social", "follow", 1, "**Priya** started following you", "2:12 pm", "follow", "Follow back", unread = true),
        AppNotification(3, "Today", "social", "social", "gift", 3, "**Ananya** sent you a Rose", "1:05 pm", "chat:3", "Say thanks", unread = true),
        AppNotification(4, "Today", "wallet", "wallet", "coin", null, "Recharge successful **+1,500 coins**", "11:20 am"),
        AppNotification(5, "Today", "social", "online", "online", 6, "**Kavya** is online now", "10:02 am", "chat:6", "Chat"),
        AppNotification(6, "Yesterday", "system", "system", "check", null, "Your host application was **approved**. You can start earning.", "8:30 pm"),
        AppNotification(7, "Yesterday", "calls", "call", "video", 2, "Video call with **Arjun** · 03:10 · 200 coins", "6:15 pm"),
        AppNotification(8, "Earlier", "wallet", "wallet", "gift", null, "Daily check-in **+20 free coins**", "Sat"),
        AppNotification(9, "Earlier", "system", "system", "shield", null, "We removed an account you reported. Thanks for keeping VIBE safe.", "Thu"),
    )
}
