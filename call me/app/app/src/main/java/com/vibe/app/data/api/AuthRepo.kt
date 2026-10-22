package com.vibe.app.data.api

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.vibe.app.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Result of a finished login: did the server just create the account, and is its profile filled in yet. */
data class LoginResult(val isNewUser: Boolean, val profileComplete: Boolean)

/**
 * Login flow: Firebase proves who the person is (SMS OTP or Google), then our backend checks Firebase's token and issues
 * its own tokens. Nothing here decides money or rules; it only signs the user in.
 */
object AuthRepo {
    private val firebase get() = FirebaseAuth.getInstance()

    // phone OTP in progress (kept here so the OTP screen and "resend" can use it)
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var autoCredential: PhoneAuthCredential? = null

    sealed interface OtpStart {
        /** SMS sent: show the OTP screen. */
        data object CodeSent : OtpStart
        /** Android read the SMS by itself (or it is a test number): the user is already verified. */
        data object AutoVerified : OtpStart
        data class Failed(val message: String) : OtpStart
    }

    // DEBUG builds only: the test-host numbers 60000002xx log in with the backend's dev login (code 123456) instead of Firebase,
    // so a second test phone can be a host. The server only accepts this with DEV_AUTH on and outside production.
    private var devPhone: String? = null
    private fun isDevNumber(phone10: String) = com.vibe.app.BuildConfig.DEBUG && phone10.startsWith("60000002")

    /** Sends the OTP to +91[phone10]. Suspends until Firebase reports "sent", "verified" or an error. */
    suspend fun sendOtp(activity: Activity, phone10: String, resend: Boolean = false): OtpStart {
        if (isDevNumber(phone10)) {
            return try {
                Api.post("/auth/dev/request-otp", JSONObject().put("phone", phone10), auth = false)
                devPhone = phone10; OtpStart.CodeSent
            } catch (e: ApiException) { OtpStart.Failed(e.message ?: "Could not send the code") }
        }
        devPhone = null
        return firebaseOtp(activity, phone10, resend)
    }

    private suspend fun firebaseOtp(activity: Activity, phone10: String, resend: Boolean): OtpStart = suspendCancellableCoroutine { cont ->
        autoCredential = null
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id; resendToken = token
                if (cont.isActive) cont.resume(OtpStart.CodeSent)
            }
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                autoCredential = credential
                if (cont.isActive) cont.resume(OtpStart.AutoVerified)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                if (cont.isActive) cont.resume(OtpStart.Failed(friendly(e)))
            }
        }
        val options = PhoneAuthOptions.newBuilder(firebase)
            .setPhoneNumber("+91$phone10")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .apply { if (resend) resendToken?.let { setForceResendingToken(it) } }
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /** True when Android already verified the number by itself, so the OTP screen can finish without a code. */
    val hasAutoCredential: Boolean get() = autoCredential != null

    /** Checks the 6-digit code (or uses the auto credential), then logs in to our backend. */
    suspend fun verifyOtp(code: String?): LoginResult {
        devPhone?.let { phone ->
            val res = Api.post("/auth/dev/verify-otp", JSONObject().put("phone", phone).put("code", code.orEmpty()), auth = false)
            val u = res.getJSONObject("user")
            AuthStore.save(res.getString("accessToken"), res.getString("refreshToken"), u)
            devPhone = null
            return LoginResult(res.optBoolean("isNewUser"), u.optBoolean("profileComplete"))
        }
        val credential = autoCredential ?: run {
            val id = verificationId ?: throw ApiException(0, "otp_expired", "The code has expired, tap Resend")
            PhoneAuthProvider.getCredential(id, code.orEmpty())
        }
        try {
            firebase.signInWithCredential(credential).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw ApiException(400, "otp_wrong", "That code is not correct")
        } catch (e: FirebaseException) {
            throw ApiException(0, "otp_failed", friendly(e))
        }
        autoCredential = null
        return backendLogin()
    }

    /** "Continue with Google": pick an account, sign in to Firebase with it, then to our backend. Returns null if the user cancelled. */
    suspend fun googleSignIn(activity: Activity): LoginResult? {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(activity.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = try {
            CredentialManager.create(activity).getCredential(activity, request)
        } catch (e: GetCredentialCancellationException) {
            return null
        } catch (e: NoCredentialException) {
            throw ApiException(0, "no_google_account", "Add a Google account to this phone first")
        } catch (e: Exception) {
            throw ApiException(0, "google_failed", "Google sign-in failed, try again")
        }
        val cred = result.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw ApiException(0, "google_failed", "Google sign-in failed, try again")
        }
        val googleIdToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
        try {
            firebase.signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
        } catch (e: FirebaseException) {
            throw ApiException(0, "google_failed", friendly(e))
        }
        return backendLogin()
    }

    /** Sends Firebase's fresh ID token to our server, which verifies it with Google and gives back our own tokens. */
    private suspend fun backendLogin(): LoginResult {
        val user = firebase.currentUser ?: throw ApiException(0, "login_failed", "Login failed, try again")
        val idToken = user.getIdToken(true).await().token ?: throw ApiException(0, "login_failed", "Login failed, try again")
        val res = Api.post("/auth/firebase", JSONObject().put("idToken", idToken), auth = false)
        val u = res.getJSONObject("user")
        AuthStore.save(res.getString("accessToken"), res.getString("refreshToken"), u)
        firebase.signOut() // Firebase was only needed to prove the identity; from here on our own tokens are used
        return LoginResult(res.optBoolean("isNewUser"), u.optBoolean("profileComplete"))
    }

    /** Logs out on the server and on this phone. */
    suspend fun logout(ctx: Context) {
        val rt = AuthStore.refreshToken
        if (rt != null) runCatching { Api.post("/auth/logout", JSONObject().put("refreshToken", rt), auth = false) }
        AuthStore.clear()
        runCatching { firebase.signOut() }
        runCatching { CredentialManager.create(ctx).clearCredentialState(androidx.credentials.ClearCredentialStateRequest()) }
    }

    private fun friendly(e: Exception): String = when (e) {
        is FirebaseTooManyRequestsException -> "Too many tries from this phone. Please try again later."
        is FirebaseAuthInvalidCredentialsException -> "That number or code is not valid"
        else -> e.message?.takeIf { it.length < 120 } ?: "Something went wrong, try again"
    }
}
