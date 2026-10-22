package com.vibe.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

// DEMO data only. Real chats, call history and notifications come from the backend (sockets for live updates);
// message prices, free-message counts and coin charges are decided by the server.

/** hostId 0 = the official VIBE Team account. */
data class ChatPreview(
    val hostId: Int,
    val text: String,
    val time: String,
    val unread: Int = 0,
    val kind: String = "text", // text | typing | missed | video | gift
    val mine: Boolean = false,
    val seen: Boolean = false,
    val pinned: Boolean = false,
)

/** dir: out | in | missed */
data class CallLogItem(val hostId: Int, val video: Boolean, val dir: String, val time: String, val detail: String)

/** action: "call:<hostId>:audio|video", "chat:<hostId>" or "follow". Text uses **bold** markers. */
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

sealed class ChatMsg {
    /** state: 0 sent, 1 delivered, 2 seen. paid = coins charged for this message (0 = free). */
    data class Text(val me: Boolean, val text: String, val time: String, val state: Int = 0, val paid: Int = 0) : ChatMsg()
    data class Note(val text: String) : ChatMsg()
    data class Day(val text: String) : ChatMsg()
    data class CallLine(val text: String, val voice: Boolean, val missed: Boolean, val time: String) : ChatMsg()
    data class GiftMsg(val me: Boolean, val emoji: String, val name: String, val price: Int, val time: String) : ChatMsg()
    data class Photo(val me: Boolean, val time: String) : ChatMsg()
    data object Safety : ChatMsg()
}

/** "Hi **Priya**" -> Hi + bold white Priya. */
fun boldText(s: String): AnnotatedString = buildAnnotatedString {
    s.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Medium)) { append(part) } else append(part)
    }
}

object ChatDemo {
    val chats = listOf(
        ChatPreview(0, "Welcome! Here are 100 free coins 🎁", "9:02", unread = 1, pinned = true),
        ChatPreview(1, "Same!! Call karte hain? ☕", "2m", unread = 2),
        ChatPreview(2, "", "now", kind = "typing"),
        ChatPreview(3, "Haha see you tomorrow 😄", "1h", mine = true, seen = true),
        ChatPreview(6, "🎁 You sent a Rose", "3h", kind = "gift"),
        ChatPreview(4, "Missed voice call", "Yesterday", kind = "missed"),
        ChatPreview(5, "Kesariya on loop since morning", "Mon", mine = true),
        ChatPreview(8, "Video call · 12:40", "Sun", kind = "video"),
    )

    val calls = listOf(
        CallLogItem(1, true, "out", "Today, 2:10 pm", "06:24 · 420 coins"),
        CallLogItem(4, false, "missed", "Yesterday, 9:41 pm", "Missed"),
        CallLogItem(3, false, "in", "Yesterday, 6:05 pm", "12:02 · 360 coins"),
        CallLogItem(6, true, "out", "Mon, 11:20 pm", "No answer · 0 coins"),
        CallLogItem(2, true, "in", "Sun, 8:15 pm", "03:10 · 200 coins"),
    )

    val following = listOf(1, 3, 5, 6, 7)

    val notifications = listOf(
        AppNotification(1, "Today", "calls", "missed", "missed", 4, "**Rahul** missed your voice call", "9:41 pm", "call:4:audio", "Call back", primary = true, unread = true),
        AppNotification(2, "Today", "social", "social", "follow", 1, "**Priya** started following you", "2:12 pm", "follow", "Follow back", unread = true),
        AppNotification(3, "Today", "social", "social", "gift", 3, "**Ananya** sent you a Rose", "1:05 pm", "chat:3", "Say thanks", unread = true),
        AppNotification(4, "Today", "wallet", "wallet", "coin", null, "Recharge successful **+1,500 coins**", "11:20 am"),
        AppNotification(5, "Today", "social", "online", "online", 6, "**Kabir** is online now", "10:02 am", "chat:6", "Chat"),
        AppNotification(6, "Yesterday", "system", "system", "check", null, "Your host application was **approved**. You can start earning.", "8:30 pm"),
        AppNotification(7, "Yesterday", "calls", "call", "video", 2, "Video call with **Arjun** · 03:10 · 200 coins", "6:15 pm"),
        AppNotification(8, "Earlier", "wallet", "wallet", "gift", null, "Daily check-in **+20 free coins**", "Sat"),
        AppNotification(9, "Earlier", "system", "system", "shield", null, "We removed an account you reported. Thanks for keeping VIBE safe.", "Thu"),
    )

    fun messagesFor(hostId: Int): List<ChatMsg> = if (hostId == 0) listOf(
        ChatMsg.Day("Today"),
        ChatMsg.Text(false, "Welcome to VIBE! 💖 Here are 100 free coins to start.", "9:02 am"),
        ChatMsg.Text(false, "Tip: become a host from the Home screen and earn for every minute you talk.", "9:02 am"),
    ) else listOf(
        ChatMsg.Safety,
        ChatMsg.Day("Yesterday"),
        ChatMsg.Text(true, "Hi ${Demo.host(hostId).name}! Loved your playlist 🎵", "9:30 pm", state = 2),
        ChatMsg.Text(false, "Haha thank you 😄 which song did you like?", "9:31 pm"),
        ChatMsg.CallLine("Missed voice call", voice = true, missed = true, time = "9:41 pm"),
        ChatMsg.Day("Today"),
        ChatMsg.GiftMsg(true, "🌹", "Rose", 20, "1:58 pm"),
        ChatMsg.CallLine("Video call · 06:24", voice = false, missed = false, time = "2:10 pm"),
        ChatMsg.Photo(false, "2:20 pm"),
        ChatMsg.Text(false, "Same!! Call karte hain? I'm free for 20 min ☕", "2:21 pm"),
    )
}
