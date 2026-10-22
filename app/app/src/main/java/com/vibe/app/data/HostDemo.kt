package com.vibe.app.data

// DEMO DATA ONLY (host pages). Rate ranges, the level-based unlock limit, earning share, coin value, fees, minimums, agencies,
// earnings and payouts all come from the server (admin panel). The app only displays them; the server re-checks every value.

data class RateCfg(val min: Int, val max: Int, val step: Int, val unlocked: Int, val start: Int, val chips: List<Int>)
data class Earning(val title: String, val detail: String, val amount: Int, val time: String, val kind: String)
data class Payout(val coins: Int, val status: String, val note: String)

object HostDemo {
    const val SHARE_PCT = 60
    const val COIN_INR = 0.1
    const val AVAILABLE = 12480
    const val MIN_WITHDRAW = 5000
    const val FEE_PCT = 2.0
    const val TDS_PCT = 1.0

    val voice = RateCfg(20, 200, 10, 60, 30, listOf(20, 30, 40, 60, 100, 200))
    val video = RateCfg(40, 400, 10, 120, 60, listOf(40, 60, 80, 120, 200, 400))
    val message = RateCfg(50, 1000, 50, 400, 200, listOf(100, 200, 300, 400, 600))

    val week = listOf("Mon" to 420, "Tue" to 680, "Wed" to 310, "Thu" to 920, "Fri" to 760, "Sat" to 1180, "Sun" to 540)
    val month = listOf("W1" to 3200, "W2" to 4100, "W3" to 3650, "W4" to 4810)

    val recent = listOf(
        Earning("Video call · Rahul", "12 min", 432, "Today, 2:10 pm", "call"),
        Earning("Voice call · Sam", "8 min", 144, "Today, 12:40 pm", "call"),
        Earning("Messages · Karan", "14 msgs", 168, "Today, 11:05 am", "msg"),
        Earning("Gift · Rose x5", "", 90, "Yesterday", "gift"),
        Earning("Video call · Arjun", "under 1 min", 0, "Yesterday", "call"),
    )

    val payouts = listOf(
        Payout(8000, "Paid", "12 Oct · UTR 4245…891"),
        Payout(6000, "Rejected", "28 Sep · Wrong UPI · coins returned"),
        Payout(10000, "Paid", "14 Sep · UTR 3811…207"),
    )

    val agencies = mapOf("AG1045" to "Star Agency", "AG4821" to "Moon Agency")
    val languages = listOf("Hindi", "English", "Punjabi", "Bengali", "Tamil", "Telugu", "Marathi", "Kannada", "Gujarati")
}
