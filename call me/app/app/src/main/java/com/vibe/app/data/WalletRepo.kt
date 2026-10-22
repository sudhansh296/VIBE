package com.vibe.app.data

import com.vibe.app.data.api.Api
import com.vibe.app.data.api.AuthStore
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject

data class CoinPackItem(val id: Int, val coins: Int, val bonus: Int, val total: Int, val priceInr: Double, val tag: String?)
data class LedgerItem(val id: Long, val kind: String, val title: String, val coins: Long, val at: String, val orderId: String?)
data class PaymentInfo(
    val orderId: String, val paymentId: String?, val status: String, val coins: Int, val amountInr: Double,
    val createdAt: String, val paidAt: String?, val failReason: String?,
)

/** What Razorpay's checkout handed back: success (with the proof the server checks) or a failure / cancel. */
sealed interface CheckoutResult {
    data class Paid(val orderId: String, val paymentId: String, val signature: String) : CheckoutResult
    data class Failed(val reason: String) : CheckoutResult
}

/** Razorpay calls the Activity; this hands the result to the screen that opened the checkout. */
object PaymentBridge {
    @Volatile var pending: CompletableDeferred<CheckoutResult>? = null
    fun deliver(r: CheckoutResult) { pending?.complete(r); pending = null }
}

/**
 * Coins: packs, buying, balance, history. The app never decides a price or adds a coin: the server makes the order with the pack's
 * price and credits the coins only after it has checked Razorpay's signature (or Razorpay's webhook told it directly).
 */
object WalletRepo {
    fun price(inr: Double) = if (inr % 1.0 == 0.0) "₹%,d".format(inr.toLong()) else "₹%,.2f".format(inr)

    suspend fun balance(): Int {
        val c = Api.get("/wallet").optInt("coins")
        setCoins(c)
        return c
    }

    /** Keeps the coin number shown everywhere (Home pill, Me, sheets) in step with the server. */
    fun setCoins(c: Int) { AuthStore.user?.let { AuthStore.saveUser(it.put("coins", c)) } }

    suspend fun packs(): Pair<List<CoinPackItem>, Boolean> {
        val r = Api.get("/wallet/packs")
        val a = r.getJSONArray("packs")
        return List(a.length()) {
            val o = a.getJSONObject(it)
            CoinPackItem(o.getInt("id"), o.getInt("coins"), o.optInt("bonusCoins"), o.getInt("totalCoins"), o.getDouble("priceInr"), o.optString("tag").takeIf { t -> t.isNotBlank() && t != "null" })
        } to r.optBoolean("purchaseEnabled", true)
    }

    suspend fun order(packId: Int): JSONObject = Api.post("/wallet/orders", JSONObject().put("packId", packId))

    /** Sends the checkout proof; returns the payment as the server now sees it. */
    suspend fun verify(p: CheckoutResult.Paid): PaymentInfo {
        val r = Api.post("/wallet/verify", JSONObject().put("orderId", p.orderId).put("paymentId", p.paymentId).put("signature", p.signature))
        if (r.has("coins")) setCoins(r.optInt("coins"))
        return info(r.getJSONObject("payment"))
    }

    suspend fun failed(orderId: String, reason: String) { runCatching { Api.post("/wallet/orders/$orderId/failed", JSONObject().put("reason", reason.take(200))) } }

    /** DEV ONLY (pretend Razorpay on a laptop backend without keys). */
    suspend fun devPay(orderId: String): CheckoutResult.Paid {
        val r = Api.post("/wallet/dev/pay/$orderId")
        return CheckoutResult.Paid(r.getString("orderId"), r.getString("paymentId"), r.getString("signature"))
    }

    suspend fun payment(orderId: String): PaymentInfo = info(Api.get("/wallet/payments/$orderId").getJSONObject("payment"))

    suspend fun history(page: Int = 1, earnings: Boolean = false): Pair<List<LedgerItem>, Int?> {
        val r = Api.get(if (earnings) "/host/earnings?page=$page" else "/wallet/history?page=$page")
        val a = r.getJSONArray("items")
        val items = List(a.length()) {
            val o = a.getJSONObject(it)
            LedgerItem(o.getLong("id"), o.getString("kind"), o.getString("title"), o.getLong("coins"), o.optString("at"), o.optString("orderId").takeIf { s -> s.isNotBlank() && s != "null" })
        }
        return items to (if (r.isNull("nextPage")) null else r.optInt("nextPage"))
    }

    suspend fun earnings(): Long = Api.get("/host/earnings").optLong("earnings")

    private fun info(o: JSONObject) = PaymentInfo(
        o.getString("orderId"), o.optString("paymentId").takeIf { it.isNotBlank() && it != "null" }, o.getString("status"), o.optInt("coins"),
        o.optDouble("amountInr"), o.optString("createdAt"), o.optString("paidAt").takeIf { it.isNotBlank() && it != "null" },
        o.optString("failReason").takeIf { it.isNotBlank() && it != "null" },
    )

    /** "2026-10-18T07:56:34.000Z" -> "18 Oct 2026, 1:26 pm" in the phone's time zone. */
    fun niceTime(iso: String): String = try {
        val inF = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val d = inF.parse(iso.take(19)) ?: return iso
        java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.ENGLISH).format(d).replace("AM", "am").replace("PM", "pm")
    } catch (e: Exception) { iso }
}
