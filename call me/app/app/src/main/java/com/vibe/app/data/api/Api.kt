package com.vibe.app.data.api

import com.vibe.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A failed call. [code] is the server's error code (e.g. "under_age"), [message] is safe to show to the user. */
class ApiException(val status: Int, val code: String, override val message: String) : Exception(message)

/**
 * Talks to the VIBE backend. Every request carries the access token; when it has expired the client swaps the refresh
 * token for a new pair once and retries. The app sends only what the user typed or chose; the server decides the rest.
 */
object Api {
    private val json = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val refreshLock = Mutex()

    suspend fun get(path: String, auth: Boolean = true) = call("GET", path, null, auth)
    suspend fun post(path: String, body: JSONObject = JSONObject(), auth: Boolean = true) = call("POST", path, body.toString().toRequestBody(json), auth)
    suspend fun put(path: String, body: JSONObject, auth: Boolean = true) = call("PUT", path, body.toString().toRequestBody(json), auth)
    suspend fun delete(path: String, auth: Boolean = true) = call("DELETE", path, null, auth)

    /** Uploads one file as multipart form-data (field [field]), plus plain text [fields] (e.g. a caption). */
    suspend fun upload(path: String, field: String, fileName: String, mime: String, bytes: ByteArray, fields: Map<String, String> = emptyMap()): JSONObject {
        val body = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart(field, fileName, bytes.toRequestBody(mime.toMediaType()))
            .apply { fields.forEach { (k, v) -> addFormDataPart(k, v) } }
            .build()
        return call("POST", path, body, auth = true)
    }

    private suspend fun call(method: String, path: String, body: RequestBody?, auth: Boolean, retried: Boolean = false): JSONObject {
        val token = if (auth) AuthStore.accessToken else null
        val (status, res) = send(method, path, body, token)
        if (status == 401 && auth && !retried && refresh(token)) return call(method, path, body, auth, retried = true)
        if (status !in 200..299) {
            val err = res.optJSONObject("error")
            val code = err?.optString("code").orEmpty()
            if (auth && (status == 401 || code == "account_blocked")) AuthStore.endSession(err?.optString("message").orEmpty().ifEmpty { "Please log in again" })
            throw ApiException(status, err?.optString("code").orEmpty().ifEmpty { "http_$status" }, err?.optString("message").orEmpty().ifEmpty { "Something went wrong" })
        }
        return res
    }

    private suspend fun send(method: String, path: String, body: RequestBody?, token: String?): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(BuildConfig.API_BASE + path).method(method, body).apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        try {
            http.newCall(req).execute().use { r ->
                val text = r.body?.string().orEmpty()
                r.code to (runCatching { JSONObject(text) }.getOrNull() ?: JSONObject())
            }
        } catch (e: IOException) {
            throw ApiException(0, "offline", "Can't reach VIBE. Check your internet and try again.")
        }
    }

    /** Swaps the refresh token for a new pair. Returns false when the session is over (then the user must log in again). */
    private suspend fun refresh(failedToken: String?): Boolean = refreshLock.withLock {
        // another request already refreshed while we waited: just use the new token (never reuse an old refresh token,
        // the server treats that as theft and logs every device out)
        if (AuthStore.accessToken != null && AuthStore.accessToken != failedToken) return true
        val rt = AuthStore.refreshToken ?: return false
        val (status, res) = send("POST", "/auth/refresh", JSONObject().put("refreshToken", rt).toString().toRequestBody(json), null)
        if (status != 200) { AuthStore.endSession(); return false }
        AuthStore.save(res.getString("accessToken"), res.getString("refreshToken"), res.optJSONObject("user"))
        true
    }
}
