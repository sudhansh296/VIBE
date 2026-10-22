package com.vibe.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Blocks and reports go to the server (it enforces them). Statuses below are still demo (in memory) until their phase.

object SafetyStore {
    /** Demo-host ids this user blocked: a copy of the server's block list (the server is the one that enforces it). */
    val blocked = mutableStateListOf<Int>()
    /** When each one was blocked, from the server. */
    val blockedAt = mutableStateMapOf<Int, String>()

    // the 8 test hosts exist on the server with these public ids (backend: npm run seed:demo)
    fun publicIdOf(hostId: Int): Long = if (hostId in 1..8) 40362380L + hostId else hostId.toLong()
    private fun hostIdOf(publicId: Long): Int? = (publicId - 40362380L).toInt().takeIf { it in 1..8 } ?: publicId.toInt()
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    /** Loads the block list from the server. */
    suspend fun sync() {
        val res = com.vibe.app.data.api.Api.get("/blocks")
        val arr = res.getJSONArray("blocks")
        blocked.clear(); blockedAt.clear()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = hostIdOf(o.getLong("id")) ?: continue
            if (id > 8) HostRepo.cache[id] = HostRepo.cache[id] ?: HostRepo.fromJson(o)   // name + photo for the Blocked list
            blocked.add(id)
            blockedAt[id] = "Blocked " + o.optString("blockedAt").take(10)
        }
    }

    /** Blocks on the server; the list updates at once and is put back if the server refuses. */
    fun block(id: Int, onError: (String) -> Unit = {}) {
        if (id !in blocked) blocked.add(id)
        scope.launch {
            try { com.vibe.app.data.api.Api.post("/blocks/${publicIdOf(id)}") }
            catch (e: com.vibe.app.data.api.ApiException) { blocked.remove(id); onError(e.message) }
        }
    }

    fun unblock(id: Int, onError: (String) -> Unit = {}) {
        blocked.remove(id)
        scope.launch {
            try { com.vibe.app.data.api.Api.delete("/blocks/${publicIdOf(id)}") }
            catch (e: com.vibe.app.data.api.ApiException) { if (id !in blocked) blocked.add(id); onError(e.message) }
        }
    }

    /** Server codes for [reportReasons], same order. */
    val reportCodes = listOf("fake_profile", "nudity", "abuse", "asked_money", "under_18", "spam", "other")
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

/** A photo or video attached to a status. [photo] picks one of the host's photos (0..2); a video also has a length in seconds. */
class StatusMedia(val photo: Int, val video: Boolean = false, val secs: Int = 0, val uri: android.net.Uri? = null)
class StatusSlide(val bg: List<Color>, val text: String, val ago: String, val media: StatusMedia? = null)
class StatusItem(val hostId: Int, val slides: List<StatusSlide>)
class MyStatus(val text: String, val bg: List<Color>, val everyone: Boolean, val views: Int, val media: StatusMedia? = null, val capDy: Float = 0f)

object StatusStore {
    val items = listOf(
        StatusItem(1, listOf(
            StatusSlide(listOf(Color(0xFFFF9A8B), Color(0xFFFF2A85), Color(0xFF3B0A2E)), "Golden hour in Mumbai", "2h ago", StatusMedia(0)),
            StatusSlide(listOf(Color(0xFFA18CD1), Color(0xFF7A1FD1), Color(0xFF1A0B2E)), "Chai + old songs tonight. Who is joining?", "1h ago", StatusMedia(1, video = true, secs = 12)),
        )),
        StatusItem(3, listOf(StatusSlide(listOf(Color(0xFFF6D365), Color(0xFFFDA085), Color(0xFF4A1D12)), "New dance reel coming soon", "3h ago", StatusMedia(1)))),
        StatusItem(6, listOf(StatusSlide(listOf(Color(0xFF84FAB0), Color(0xFF0EA5A0), Color(0xFF06242A)), "Jaipur sunsets never get old", "5h ago", StatusMedia(2)))),
        StatusItem(2, listOf(StatusSlide(listOf(Color(0xFF4FACFE), Color(0xFF1E3A8A), Color(0xFF0A1230)), "Match day vibes", "9h ago", StatusMedia(0, video = true, secs = 8)))),
    )

    /** Online people who have no status right now (shown without a ring). */
    val onlineOnly = listOf(5, 8)

    /** Host ids whose status the user has already seen (their ring turns grey). */
    val seen = mutableStateListOf<Int>()
    fun markSeen(hostId: Int) { if (hostId !in seen) seen.add(hostId) }

    /** The user's own live statuses (WhatsApp style: up to [MAX_POSTS] at the same time). */
    const val MAX_POSTS = 6
    val posts = mutableStateListOf<MyStatus>()
    val mine: MyStatus? get() = posts.lastOrNull()

    val backgrounds = listOf(
        listOf(Color(0xFFFF9A8B), Color(0xFFFF2A85), Color(0xFF3B0A2E)),
        listOf(Color(0xFFA18CD1), Color(0xFF7A1FD1), Color(0xFF1A0B2E)),
        listOf(Color(0xFF84FAB0), Color(0xFF0EA5A0), Color(0xFF06242A)),
        listOf(Color(0xFFF6D365), Color(0xFFFDA085), Color(0xFF4A1D12)),
        listOf(Color(0xFF4FACFE), Color(0xFF1E3A8A), Color(0xFF0A1230)),
        listOf(Color(0xFF434343), Color(0xFF1C1C28), Color(0xFF000000)),
    )
}
