package com.vibe.app.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.CheckoutResult
import com.vibe.app.data.CoinPackItem
import com.vibe.app.data.PaymentBridge
import com.vibe.app.data.WalletRepo
import com.vibe.app.data.api.ApiException
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Buying one coin pack, used by the Wallet and by the quick top-up during a call. The server makes the order (price and coins fixed
 * there), Razorpay takes the payment, the server checks the proof and adds the coins. [buy] reports success / pending / failed.
 * On a laptop backend without Razorpay keys a "TEST PAYMENT" sheet stands in for the checkout ([TestSheet] must be placed once).
 */
class CoinCheckout(private val ctx: Context, private val scope: CoroutineScope) {
    var paying by mutableStateOf(false)
        private set
    private var testOrder by mutableStateOf<JSONObject?>(null)
    private var testResult: CompletableDeferred<Boolean>? = null

    fun buy(pack: CoinPackItem, onResult: (kind: String, orderId: String) -> Unit) = pay({ WalletRepo.order(pack.id) }, onResult)

    /** VIP plan: same payment path; the server adds the VIP days and the bonus coins once the payment is proven. */
    fun buyVip(planId: Int, onResult: (kind: String, orderId: String) -> Unit) =
        pay({ com.vibe.app.data.api.Api.post("/vip/orders", JSONObject().put("planId", planId)) }, onResult)

    private fun pay(makeOrder: suspend () -> JSONObject, onResult: (kind: String, orderId: String) -> Unit) {
        if (paying) return
        paying = true
        scope.launch {
            var orderId: String? = null
            try {
                val o = makeOrder()
                orderId = o.getString("orderId")
                val result: CheckoutResult = if (o.optString("provider") == "fake") {
                    val d = CompletableDeferred<Boolean>()
                    testResult = d; testOrder = o
                    if (d.await()) WalletRepo.devPay(orderId) else CheckoutResult.Failed("Payment failed (test)")
                } else {
                    val activity = ctx as? Activity ?: throw ApiException(0, "no_activity", "Please try again")
                    val d = CompletableDeferred<CheckoutResult>()
                    PaymentBridge.pending = d
                    val co = com.razorpay.Checkout()
                    co.setKeyID(o.getString("keyId"))
                    co.open(
                        activity,
                        JSONObject().put("name", "VIBE").put("description", o.optString("description")).put("order_id", orderId)
                            .put("amount", o.getInt("amountPaise")).put("currency", "INR")
                            .put("prefill", JSONObject().put("contact", o.optString("phone")))
                            .put("theme", JSONObject().put("color", "#FF4D8D")),
                    )
                    d.await()
                }
                when (result) {
                    is CheckoutResult.Paid -> {
                        // the server checks the proof; if it cannot be reached now, Razorpay's webhook still adds the coins: "pending"
                        val p = try { WalletRepo.verify(result) } catch (e: ApiException) { if (e.status == 0 || e.status >= 500) null else throw e }
                        onResult(if (p?.status == "paid") "success" else "pending", orderId)
                    }
                    is CheckoutResult.Failed -> {
                        WalletRepo.failed(orderId, result.reason)
                        if (result.reason == "Payment cancelled") ctx.toast("Payment cancelled · no money taken") else onResult("failed", orderId)
                    }
                }
            } catch (e: ApiException) {
                ctx.toast(e.message)
                if (orderId != null && e.code == "bad_signature") onResult("failed", orderId)
            }
            paying = false
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TestSheet() {
        val o = testOrder ?: return
        fun answer(ok: Boolean) { testResult?.complete(ok); testResult = null; testOrder = null }
        ModalBottomSheet(onDismissRequest = { answer(false) }, containerColor = VibeColors.Card) {
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TEST PAYMENT", color = VibeColors.Gold, fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Text(WalletRepo.price(o.optInt("amountPaise") / 100.0), color = VibeColors.Ink, fontSize = 36.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
                Text(if (o.optBoolean("vip")) "${o.optString("description")} · +${"%,d".format(o.optInt("coins"))} bonus coins" else "${"%,d".format(o.optInt("coins"))} coins", color = VibeColors.TextDim, fontSize = 13.sp)
                Text(
                    "Razorpay keys are not set on the server yet, so this is a pretend payment. No real money is taken.",
                    color = VibeColors.TextMute, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp),
                )
                NeonButton("Pay (test)", { answer(true) }, modifier = Modifier.padding(top = 20.dp))
                Text("Make it fail", color = VibeColors.Red, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp).clickable { answer(false) }.padding(8.dp))
            }
        }
    }
}

@Composable
fun rememberCoinCheckout(): CoinCheckout {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember { CoinCheckout(ctx, scope) }
}
