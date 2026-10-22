package com.vibe.app.data

// DEMO DATA ONLY (host photos and videos, paid locked media). Limits, free count, unlock prices and the host's share are set in the admin
// panel. The SERVER charges the user, credits the host and only then returns the real file, so a locked photo/video is never on the
// phone before it is paid for. Hosts are verified when they apply, so new files go live right away (no per-file review).

object MediaCfg {
    const val MAX_PHOTOS = 6
    const val MAX_VIDEOS = 6
    const val MIN_PHOTOS = 3
    const val FREE_PHOTOS = 3          // the first photos are always free
    const val PRICE_PHOTO = 200        // coins to unlock one locked photo
    const val PRICE_VIDEO = 500        // coins to unlock one locked video
    const val SHARE = "60%"            // host's share of each unlock
    const val INTRO_SECONDS = 15
    const val VIDEO_SECONDS = 30
}

/** One of the host's own photos or videos. st = ok (kept for a possible admin takedown: bad). */
data class MediaItem(
    val shade: Int,
    val st: String = "ok",
    val why: String = "",
    val locked: Boolean = false,
    val intro: Boolean = false,
    val len: Int = 12,
)
