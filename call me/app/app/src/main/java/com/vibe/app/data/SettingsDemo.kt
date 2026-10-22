package com.vibe.app.data

// DEMO DATA ONLY (Edit profile / Settings / Help / Visitors). Everything here comes from the server in the real app:
// FAQ + topics + support hours + tickets (admin panel > Content / Support), visitors (server hides names for non-VIP),
// blocked users, notification and privacy preferences, language list.

data class Faq(val category: String, val question: String, val answer: String, val popular: Boolean = false)
data class Ticket(val title: String, val open: Boolean, val time: String, val newReply: Boolean)
data class Visitor(val hostId: Int, val group: String, val line: String, val online: Boolean, val fresh: Boolean)

object SettingsDemo {
    const val IS_HOST = true           // demo switch: false shows the user-only Settings
    const val BLOCKED_COUNT = 2
    const val APP_VERSION = "1.0.0"
    const val FREE_VISIBLE_VISITORS = 2   // how many visitors a non-VIP user still sees (set by VIBE)
    const val TOTAL_VISITORS = 136
    const val UNPAID_EARNINGS = 12480

    val faq = listOf(
        Faq("Calls", "How am I charged for calls?", "Calls are charged per minute at the start of each minute, using the host rate shown before you call. Each host sets their rate within limits decided by VIBE.", true),
        Faq("Calls", "What happens if the host ends the call?", "If the host ends the call early you are refunded for the minute you were billed but did not use. The server decides this, so the balance shown after the call is always the correct one.", true),
        Faq("Coins", "My payment went through but coins are missing", "Coins are added only after the server confirms your payment. This can take a minute. If they do not appear, use Report a problem and pick Payment, then add the time and amount.", true),
        Faq("Coins", "How does paid chat work?", "Each host gives you a few free messages. After that, every message costs the price shown inside the chat. VIP members chat free."),
        Faq("Coins", "Can I get a refund?", "Coins you already bought are not refundable. If a call or payment failed because of an error on our side, report it and we will return the coins."),
        Faq("Hosting", "Who can become a host?", "Anyone 18 or older. You apply with photos, a 15 second intro video, a live selfie, PAN and an ID. The VIBE team reviews it, usually within 24 hours.", true),
        Faq("Hosting", "How do I withdraw my earnings?", "Open Host dashboard and tap Withdraw. The withdraw page shows your earnings in beans. Add your UPI ID once and ask for at least the minimum; the VIBE team checks each request and pays your own UPI account. If it cannot be paid, your beans come back automatically."),
        Faq("Hosting", "Why was my application not approved?", "The rejection screen shows the reason, for example unclear photos. Fix it and tap Apply again."),
        Faq("Account", "How do I delete my account?", "Go to Settings > Delete account. Hosts should withdraw any earnings first because unpaid earnings are lost."),
        Faq("Account", "Someone is bothering me", "Open their profile or chat, tap the menu and choose Block or Report. Our team reviews every report.", true),
    )
    val tickets = listOf(
        Ticket("Payment · coins not added", true, "Today, 10:20 am", true),
        Ticket("Call · host ended early", false, "12 Oct", false),
    )
    val reportReasons = listOf("Payment", "A call", "A user", "App bug", "Hosting", "Other")
    val reportExtra = mapOf(
        "Payment" to listOf("Amount (₹)", "Payment time or ID"),
        "A call" to listOf("Host name", "When did it happen?"),
        "A user" to listOf("User name or ID"),
        "Hosting" to listOf("Application or withdrawal ID"),
    )

    val visitors = listOf(
        Visitor(2, "Today", "Viewed 5 min ago · 3 times", true, true),
        Visitor(6, "Today", "Viewed 1 h ago", true, false),
        Visitor(4, "Today", "Viewed 3 h ago", false, false),
        Visitor(8, "Yesterday", "Viewed yesterday, 21:10", false, false),
        Visitor(1, "Yesterday", "Viewed yesterday, 18:42", false, false),
    )

    val languages = listOf("Hindi", "English", "Punjabi", "Bengali", "Tamil", "Telugu", "Marathi", "Kannada", "Gujarati")
}
