package com.vibe.app.data

// DEMO DATA ONLY (Me / VIP / Level / Check-in; the Wallet is real since Phase 3). Real levels, XP, VIP plans, bonuses and
// check-in rewards will come from the server (admin panel). The app only displays them and never decides amounts.

data class LevelReward(val level: Int, val xp: Int, val title: String)
data class VipPlan(val name: String, val price: Int, val bonusCoins: Int, val perMonth: String, val tag: String? = null)
data class Benefit(val title: String, val sub: String)

object MeDemo {
    const val NAME = "Sudhanshu"
    const val USER_ID = "10159076"
    const val LEVEL = 3
    const val XP = 620
    const val XP_NEXT = 1000

    val rewards = listOf(
        LevelReward(1, 0, "Profile frame"), LevelReward(2, 300, "Chat bubble style"), LevelReward(3, 500, "Free daily gift"),
        LevelReward(4, 1000, "5% off calls"), LevelReward(5, 2000, "Animated entry"), LevelReward(10, 8000, "Custom badge"),
        LevelReward(15, 20000, "10% off calls"), LevelReward(20, 50000, "Legend crown"),
    )

    val vipPlans = listOf(
        VipPlan("1 Month", 299, 3000, "₹299/month"),
        VipPlan("3 Months", 699, 10000, "₹233/month", "Popular"),
        VipPlan("12 Months", 1999, 45000, "₹167/month", "Best value"),
    )
    val vipBenefits = listOf(
        Benefit("Cheaper calls", "Video calls cost less for every host"),
        Benefit("Free chat with every host", "No per-message charge after the free messages"),
        Benefit("Bonus coins", "Coins added the moment you subscribe"),
        Benefit("VIP badge and profile frame", "Stand out in chats, calls and the host's screen"),
        Benefit("See who visited you", "Full visitor list with time"),
    )

    /** Coins for Day 1..7; the server decides which day it is today. */
    val checkInRewards = listOf(10, 10, 20, 20, 30, 30, 100)
    const val CHECKIN_TODAY = 2   // index of today's box (earlier days are done)
}
