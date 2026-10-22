package com.vibe.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// DEMO STATE ONLY (in memory). In the real app the SERVER keeps the block list (a blocked person can't see, message or call you),
// every report goes to the admin moderation queue, and statuses (24 hours) are stored, moderated and expired by the server.

object SafetyStore {
    /** Host ids blocked by this user (demo: two people are already blocked). */
    val blocked = mutableStateListOf(4, 8)

    fun block(id: Int) { if (id !in blocked) blocked.add(id) }
    fun unblock(id: Int) { blocked.remove(id) }
    fun isBlocked(id: Int) = id in blocked

    val reportReasons = listOf(
        "Fake profile or photos" to "Pretending to be someone else or using stolen photos",
        "Nudity or sexual content" to "Explicit photos, videos or behaviour",
        "Abuse or harassment" to "Bad words, threats or bullying",
        "Asked for money or my number" to "Asking to pay outside VIBE, or for phone, UPI or social ids",
        "Looks under 18" to "This person may be a minor",
        "Spam or scam" to "Ads, links or tricks to take money",
        "Something else" to "",
    )

    /** When each demo-blocked person was blocked. */
    val blockedWhen = mapOf(4 to "Blocked 3 Oct", 8 to "Blocked 28 Sep")
}

class StatusSlide(val bg: List<Color>, val text: String, val ago: String)
class StatusItem(val hostId: Int, val slides: List<StatusSlide>)
class MyStatus(val text: String, val bg: List<Color>, val everyone: Boolean, val views: Int)

object StatusStore {
    val items = listOf(
        StatusItem(1, listOf(
            StatusSlide(listOf(Color(0xFFFF9A8B), Color(0xFFFF2A85), Color(0xFF3B0A2E)), "Golden hour in Mumbai", "2h ago"),
            StatusSlide(listOf(Color(0xFFA18CD1), Color(0xFF7A1FD1), Color(0xFF1A0B2E)), "Chai + old songs tonight. Who is joining?", "1h ago"),
        )),
        StatusItem(3, listOf(StatusSlide(listOf(Color(0xFFF6D365), Color(0xFFFDA085), Color(0xFF4A1D12)), "New dance reel coming soon", "3h ago"))),
        StatusItem(6, listOf(StatusSlide(listOf(Color(0xFF84FAB0), Color(0xFF0EA5A0), Color(0xFF06242A)), "Jaipur sunsets never get old", "5h ago"))),
        StatusItem(2, listOf(StatusSlide(listOf(Color(0xFF4FACFE), Color(0xFF1E3A8A), Color(0xFF0A1230)), "Match day vibes", "9h ago"))),
    )

    /** Online people who have no status right now (shown without a ring). */
    val onlineOnly = listOf(5, 8)

    /** Host ids whose status the user has already seen (their ring turns grey). */
    val seen = mutableStateListOf<Int>()
    fun markSeen(hostId: Int) { if (hostId !in seen) seen.add(hostId) }

    var mine by mutableStateOf<MyStatus?>(null)

    val backgrounds = listOf(
        listOf(Color(0xFFFF9A8B), Color(0xFFFF2A85), Color(0xFF3B0A2E)),
        listOf(Color(0xFFA18CD1), Color(0xFF7A1FD1), Color(0xFF1A0B2E)),
        listOf(Color(0xFF84FAB0), Color(0xFF0EA5A0), Color(0xFF06242A)),
        listOf(Color(0xFFF6D365), Color(0xFFFDA085), Color(0xFF4A1D12)),
        listOf(Color(0xFF4FACFE), Color(0xFF1E3A8A), Color(0xFF0A1230)),
        listOf(Color(0xFF434343), Color(0xFF1C1C28), Color(0xFF000000)),
    )
}
