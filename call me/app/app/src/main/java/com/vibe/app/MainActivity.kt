package com.vibe.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.vibe.app.data.CheckoutResult
import com.vibe.app.data.PaymentBridge
import com.vibe.app.ui.VibeApp
import com.vibe.app.ui.theme.VibeTheme

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // design review only: ignored in release builds
        val debuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val route = if (debuggable) intent.getStringExtra("route") else null
        com.vibe.app.data.InviteLink.init(this)
        com.vibe.app.data.InviteLink.fromIntent(this, intent)
        setContent { VibeTheme { VibeApp(debugRoute = route) } }
    }

    // an invite link tapped while the app is already open
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.vibe.app.data.InviteLink.fromIntent(this, intent)
    }

    // Razorpay checkout results: passed to the Wallet screen, which sends the proof to the server (the app itself adds no coins)
    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        val order = data?.orderId; val pay = data?.paymentId ?: razorpayPaymentId; val sig = data?.signature
        PaymentBridge.deliver(if (order != null && pay != null && sig != null) CheckoutResult.Paid(order, pay, sig) else CheckoutResult.Failed("Payment could not be confirmed"))
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        val msg = runCatching { org.json.JSONObject(response.orEmpty()).getJSONObject("error").getString("description") }.getOrNull()
        PaymentBridge.deliver(CheckoutResult.Failed(if (code == com.razorpay.Checkout.PAYMENT_CANCELED) "Payment cancelled" else msg ?: "Payment failed"))
    }
}
