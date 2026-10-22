package com.vibe.app.data

// DEMO DATA ONLY (Me / Wallet / VIP / Level / Check-in). Real levels, XP, coin packs, prices, VIP plans, bonuses and
// check-in rewards come from the server (admin panel). The app only displays them and never decides amounts.

data class LevelReward(val level: Int, val xp: Int, val title: String)
data class CoinPack(val coins: Int, val price: Int, val tag: String? = null)
data class WalletTx(val coins: Int, val title: String, val time: String)
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

    val packs = listOf(
        CoinPack(600, 49), CoinPack(1500, 99, "Popular"), CoinPack(3500, 199, "+50% first"),
        CoinPack(8000, 399), CoinPack(18000, 799, "Best value"), CoinPack(40000, 1599),
    )
    val payMethods = listOf("UPI", "GPay", "PhonePe", "Card")

    val history = listOf(
        WalletTx(1500, "Recharge", "Today, 11:20 am"), WalletTx(-420, "Video call · Priya", "Today, 2:10 pm"),
        WalletTx(-200, "Message · Ananya", "Today, 1:30 pm"), WalletTx(20, "Daily check-in", "Sat, 9:02 am"),
        WalletTx(-20, "Gift · Rose", "Fri, 8:14 pm"), WalletTx(-360, "Voice call · Ananya", "Thu, 6:05 pm"),
        WalletTx(3500, "Recharge", "Mon, 10:40 am"),
    )

    val vipPlans = listOf(
        VipPlan("1 Month", 299, 3000, "₹299/month"),
        VipPlan("3 Months", 699, 10000, "₹233/month", "Popular"),
        VipPlan("12 Months", 1999, 45000, "₹167/month", "Best value"),
    )
    val vipBenefits = listOf(
        Benefit("Cheaper calls", "Voice and video calls cost less for every host"),
        Benefit("Free chat with every host", "No per-message charge after the free messages"),
        Benefit("Bonus coins", "Coins added the moment you subscribe"),
        Benefit("VIP badge and profile frame", "Stand out in chats, calls and the host's screen"),
        Benefit("See who visited you", "Full visitor list with time"),
    )

    /** Coins for Day 1..7; the server decides which day it is today. */
    val checkInRewards = listOf(10, 10, 20, 20, 30, 30, 100)
    const val CHECKIN_TODAY = 2   // index of today's box (earlier days are done)
}
