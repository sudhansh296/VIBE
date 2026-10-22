package com.vibe.app.data

import androidx.compose.ui.graphics.Color
import com.vibe.app.ui.theme.VibeColors

// DEMO DATA ONLY for UI review. In the real app hosts, rates, status and balance come from the backend API;
// the app never decides prices or balances (see security rule: server decides everything).

enum class HostStatus(val label: String, val dot: Color, val text: Color, val pulse: Boolean = false) {
    ONLINE("ONLINE", VibeColors.Green, VibeColors.Green, pulse = true),
    BUSY("BUSY", Color(0xFFFB923C), Color(0xFFFDBA74)),
    OFFLINE("OFFLINE", Color(0xFF6B7280), Color(0xFF9CA3AF));

    val canCall get() = this == ONLINE
}

data class Host(
    val id: Int,
    val name: String,
    val age: Int,
    val gender: Char,
    val city: String,
    val rate: Int,
    val status: HostStatus,
    val level: Int,
    val rating: Double,
    val ratingCount: Int,
    val followers: String,
    val calls: Int,
    val answerRate: Int,
    val langs: List<String>,
    val bio: String,
    val c1: Color,
    val c2: Color,
    val verified: Boolean = false,
    val vip: Boolean = false,
    val isNew: Boolean = false,
    /** Which call types this host accepts (host setting). */
    val voice: Boolean = true,
    val video: Boolean = true,
) {
    /** Voice calls are cheaper than video; both rates are set by the host and enforced by the server. */
    val audioRate: Int get() = maxOf(20, Math.round(rate / 2f / 10f) * 10)

    /** Price per chat message after the free ones. Each host sets their own; the server charges it. (demo values) */
    val messagePrice: Int get() = when (id) { 2 -> 100; 3 -> 300; 7 -> 500; else -> 200 }
}

/** Result of a finished call, shown on the rate screen. */
data class CallResult(val hostId: Int, val secs: Int, val spent: Int, val gifts: Int, val audio: Boolean)

data class HostFilter(
    val show: String = "all",
    val callType: String = "any", // any | voice | video
    val minAge: Int = 18,
    val maxAge: Int = 40,
    val maxRate: Int = 200,
    val langs: Set<String> = emptySet(),
    val onlineOnly: Boolean = false,
    val verifiedOnly: Boolean = false,
    val sort: String = "online",
) {
    val isDefault get() = this == HostFilter()

    fun matches(h: Host, quick: String): Boolean {
        if (quick == "online" && h.status == HostStatus.OFFLINE) return false
        if ((quick == "voice" || callType == "voice") && !h.voice) return false
        if (callType == "video" && !h.video) return false
        if ((quick == "F" || quick == "M") && h.gender != quick[0]) return false
        if (h.age < minAge || h.age > maxAge) return false
        if (maxRate < 200 && h.rate > maxRate) return false
        if (onlineOnly && h.status == HostStatus.OFFLINE) return false
        if (verifiedOnly && !h.verified) return false
        if (langs.isNotEmpty() && h.langs.none { it in langs }) return false
        return true
    }

    fun sort(list: List<Host>): List<Host> = when (sort) {
        "price" -> list.sortedBy { it.rate }
        "rating" -> list.sortedByDescending { it.rating }
        "new" -> list.sortedByDescending { it.isNew }
        else -> list.sortedBy { it.status == HostStatus.OFFLINE }
    }
}

object Demo {
    const val BALANCE = 2850
    const val UNREAD = 3
    const val FREE_MESSAGES = 5 // free messages per host before the host's message price applies (server enforces)

    val allLanguages = listOf("Hindi", "English", "Bengali", "Marathi", "Telugu", "Tamil", "Gujarati", "Kannada", "Malayalam", "Punjabi")

    val hosts = listOf(
        Host(1, "Priya", 24, 'F', "Mumbai", 60, HostStatus.ONLINE, 12, 4.8, 1240, "12.4k", 3210, 96, listOf("Hindi", "English"),
            "Music lover, chai addict ☕ Let's talk about movies, travel and life 💫", Color(0xFFFF2A85), Color(0xFF7A1FD1), verified = true),
        Host(2, "Arjun", 26, 'M', "Delhi", 50, HostStatus.ONLINE, 8, 4.6, 640, "5.1k", 1480, 91, listOf("Hindi", "Punjabi"),
            "Cricket, gym and good conversations 🏏", Color(0xFF00F2FE), Color(0xFF1E3A8A), verified = true),
        Host(3, "Ananya", 22, 'F', "Bangalore", 80, HostStatus.ONLINE, 15, 4.9, 2100, "21k", 5120, 98, listOf("English", "Kannada"),
            "Dancer 💃 Ask me about K-dramas", Color(0xFFA855F7), Color(0xFF581C87), vip = true),
        Host(4, "Rahul", 27, 'M', "Pune", 40, HostStatus.BUSY, 5, 4.4, 210, "1.9k", 520, 84, listOf("Marathi", "Hindi"),
            "Software guy, guitar at night 🎸", Color(0xFFF59E0B), Color(0xFF9A3412)),
        Host(5, "Diya", 23, 'F', "Chennai", 60, HostStatus.ONLINE, 3, 4.7, 88, "980", 140, 93, listOf("Tamil", "English"),
            "New here 👋 Foodie and bookworm", Color(0xFF10B981), Color(0xFF065F46), isNew = true),
        Host(6, "Kabir", 25, 'M', "Jaipur", 70, HostStatus.ONLINE, 10, 4.8, 930, "8.7k", 2400, 95, listOf("Hindi", "English"),
            "Traveller ✈️ 18 states and counting", Color(0xFF38BDF8), Color(0xFF1E40AF), verified = true),
        Host(7, "Meera", 28, 'F', "Kolkata", 100, HostStatus.OFFLINE, 20, 4.9, 3400, "40k", 9100, 97, listOf("Bengali", "Hindi", "English"),
            "Singer 🎤 Rabindra sangeet to Bollywood", Color(0xFFF43F5E), Color(0xFF86198F), vip = true, voice = false),
        Host(8, "Vikram", 29, 'M', "Hyderabad", 60, HostStatus.ONLINE, 7, 4.5, 410, "3.3k", 990, 89, listOf("Telugu", "Hindi"),
            "Chef by passion 🍳 Biryani debates welcome", Color(0xFF6366F1), Color(0xFF4C1D95), video = false),
    )

    fun host(id: Int) = hosts.firstOrNull { it.id == id } ?: hosts.first()

    /** Gift catalogue: shown in UI, but price and deduction are validated by the server. */
    val gifts = listOf(Gift("🌹", "Rose", 20), Gift("☕", "Chai", 50), Gift("🍫", "Choco", 99), Gift("🧸", "Teddy", 199),
        Gift("💐", "Bouquet", 299), Gift("💍", "Ring", 999), Gift("🏎️", "Car", 2999), Gift("🏰", "Castle", 9999))

    /** In-call gift catalogue by tab. Real catalogue + animations (SVGA/MP4/Lottie) come from the admin panel via the API. */
    val giftCategories = linkedMapOf(
        "Popular" to listOf(Gift("🌹", "Rose", 20), Gift("☕", "Chai", 50), Gift("🍫", "Choco", 99), Gift("🧸", "Teddy", 199),
            Gift("🎈", "Balloon", 30), Gift("🍦", "Ice cream", 40), Gift("🎵", "Song", 80), Gift("⭐", "Star", 150)),
        "Love" to listOf(Gift("💖", "Heart", 25), Gift("💋", "Kiss", 60), Gift("💐", "Bouquet", 299), Gift("💌", "Letter", 120),
            Gift("🥰", "Hug", 90), Gift("💘", "Cupid", 499), Gift("💍", "Ring", 999), Gift("💑", "Couple", 1499)),
        "Luxury" to listOf(Gift("👑", "Crown", 1999), Gift("🏎️", "Car", 2999), Gift("🛥️", "Yacht", 4999), Gift("✈️", "Jet", 6999),
            Gift("🏰", "Castle", 9999), Gift("💎", "Diamond", 3999), Gift("🚀", "Rocket", 7999), Gift("🌌", "Galaxy", 19999)),
    )

    /** Quick top-up packs: coins to rupees. Prices come from the server in the real app. */
    val topUpPacks = listOf(600 to 49, 1500 to 99, 3500 to 199)
}

data class Gift(val emoji: String, val name: String, val price: Int)
