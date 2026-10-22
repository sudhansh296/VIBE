package com.vibe.app.data

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.vibe.app.BuildConfig
import com.vibe.app.data.api.Api
import com.vibe.app.data.api.AuthStore
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Things the server tells the open app. The screens react (banner, incoming call screen, ringing screen). */
sealed interface LiveEvent {
    /** Someone opened your chat, or has been looking at your profile for a few seconds (hosts). [where] = chat | profile */
    data class Watching(val who: Host, val where: String) : LiveEvent
    /** A call is ringing on this phone (hosts). */
    data class Incoming(val callId: String, val from: Host, val auto: Boolean, val ratePerMinute: Int, val ringSeconds: Int, val kind: String = "direct") : LiveEvent
    /** The call this user started (by the button or by staying on a profile) is ringing at the host (users). */
    data class Ringing(val callId: String, val host: Host, val auto: Boolean, val ringSeconds: Int, val kind: String = "direct", val ratePerMinute: Int = 0) : LiveEvent
    data class Accepted(val callId: String) : LiveEvent
    /** A new line in my bell list. */
    data class Notified(val notice: Notice) : LiveEvent
    /** I (a user) tried to pick up a host's call without coins for the first minute: recharge first. */
    data class NeedCoins(val callId: String, val need: Int) : LiveEvent
    /** The rung user is recharging to pick up; the call rings [seconds] longer. */
    data class Topping(val callId: String, val seconds: Int) : LiveEvent
    /** A chat message arrived (or one of my own, sent from another phone). */
    data class ChatMsg(val peerId: Int, val message: ChatMessage) : LiveEvent
    /** The other person has read my messages. */
    data class ChatRead(val peerId: Int) : LiveEvent
    /** The call is over. Numbers from the server: seconds talked, minutes and coins charged (user) / earned (host), who hung up. */
    /** [earned] = the host's whole take (minutes + gifts during the call); [gifts] = how many gifts came during it. */
    data class Ended(val callId: String, val reason: String, val seconds: Int = 0, val minutes: Int = 0, val coins: Int = 0, val earned: Int = 0, val endedBy: String? = null,
                     val peerId: Int? = null, val role: String? = null, val giftEarned: Int = 0, val gifts: Int = 0) : LiveEvent
    /** Host only: the host's running total for the call went up (a gift arrived). */
    data class Earned(val callId: String, val earned: Int) : LiveEvent
    /** A minute was charged (user: coins spent and left; host: earned so far). */
    data class Billing(val callId: String, val minute: Int, val coins: Int, val coinsLeft: Int?, val earned: Int, val low: Boolean) : LiveEvent
    /** The other phone lost its connection (the call waits [seconds] for it) / came back. */
    data class PeerLost(val callId: String, val seconds: Int) : LiveEvent
    data class PeerBack(val callId: String) : LiveEvent
    /** This phone came back while a call was still on (app restarted / connection back): show it again. */
    data class Resume(val callId: String, val peerId: Int, val state: String) : LiveEvent
    /** A short text inside the running call. */
    data class CallChat(val callId: String, val text: String) : LiveEvent
}

/**
 * The live connection to the server (Socket.IO). A person is online exactly while the app is open: the connection opens when the app
 * comes to the front and closes when it goes away, and the server turns that into "online / offline" for everybody else.
 * The phone only says "I opened this profile" or "I pressed call"; the server does the timing, the checks and the ringing.
 */
object Realtime {
    private var socket: Socket? = null
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wanted = false
    private var inForeground = false
    private var started = false

    private val _events = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    /** The call ringing or running on this phone: who with, my side (user / host), its kind and rate, when it was picked up. */
    data class ActiveCall(val callId: String, val peerId: Int, val role: String, val kind: String, val rate: Int, val startedAt: Long? = null)
    var activeCall by androidx.compose.runtime.mutableStateOf<ActiveCall?>(null)
    /** The last minute charged for the active call (the first one is charged at pick-up, before the call screen is open). */
    // compose state: the call screen applies the latest bill even if it arrived while the screen was still opening
    var lastBilling by androidx.compose.runtime.mutableStateOf<LiveEvent.Billing?>(null)

    /** In-call chat lines (last 6) and gifts sent in the running call. Kept here, not in the call screen, so they survive
     *  the screen closing (Back = small call pill) and messages that arrive meanwhile are not lost. */
    data class CallLine(val mine: Boolean, val text: String)
    val callLines = androidx.compose.runtime.mutableStateListOf<CallLine>()
    var callGifts by androidx.compose.runtime.mutableIntStateOf(0)
    /** Counts socket (re)connects: lists that show who is online reload after each one. */
    var connects by androidx.compose.runtime.mutableIntStateOf(0)
    private var callMemoryFor: String? = null
    /** Starts a fresh memory when a different call begins. */
    fun callMemory(callId: String) { if (callMemoryFor != callId) { callMemoryFor = callId; callLines.clear(); callGifts = 0 } }
    fun addCallLine(callId: String, mine: Boolean, text: String) {
        callMemory(callId); callLines.add(CallLine(mine, text)); while (callLines.size > 6) callLines.removeAt(0)
    }

    /** The call that is ringing or running right now (id from the server). */
    @Volatile var activeCallId: String? = null
        private set

    /** Call once at start: follows the app between front and back. */
    fun init() {
        if (started) return
        started = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { inForeground = true; if (wanted) open() }
            // a running call keeps the connection when the app goes to the background (e.g. the user checks another app)
            override fun onStop(owner: LifecycleOwner) { inForeground = false; if (activeCall == null) close() }
        })
    }

    /** Signed in: keep the connection while the app is open. */
    fun start() { wanted = true; if (inForeground) open() }

    /** Signed out: no connection at all. */
    fun stop() { wanted = false; close() }

    private fun close() {
        socket?.let { it.off(); it.disconnect() }
        socket = null
        activeCallId = null
    }

    private fun open() {
        if (socket != null) return
        val token = AuthStore.accessToken ?: return
        val opts = IO.Options().apply {
            auth = mapOf("token" to token)
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionDelay = 1000
            reconnectionDelayMax = 5000
        }
        val s = IO.socket(BuildConfig.API_BASE, opts)
        socket = s
        s.on(Socket.EVENT_CONNECT) { scope.launch { ChatRepo.refreshUnread() }; main.post { connects++ } }
        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val msg = (args.firstOrNull() as? Exception)?.message ?: args.firstOrNull()?.toString().orEmpty()
            main.post {
                when {
                    // the 15 minute token ran out: get a fresh one (the API does that on its own) and connect again
                    "token_expired" in msg -> { close(); scope.launch { runCatching { Api.get("/me") }; if (wanted && inForeground) open() } }
                    "session_ended" in msg || "unauthorized" in msg -> { close(); scope.launch { runCatching { Api.get("/me") } } } // the API ends the session if it is really over
                }
            }
        }
        s.on("presence") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { HostRepo.presence(o.getInt("id"), o.optBoolean("online").takeIf { o.has("online") }, o.optBoolean("busy").takeIf { o.has("busy") }) }
        }
        s.on("profile_watch") { a ->
            val e = a.firstOrNull() as? JSONObject ?: return@on
            val o = e.optJSONObject("user") ?: return@on
            main.post { val h = HostRepo.fromJson(o.put("role", "user")); HostRepo.cache[h.id] = h; _events.tryEmit(LiveEvent.Watching(h, e.optString("where", "profile"))) }
        }
        s.on("incoming_call") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post {
                // role = my side in this call: "host" (a user calls me) or "user" (a host calls me)
                val role = o.optString("role", "host")
                val from = HostRepo.fromJson(o.getJSONObject("from").put("role", if (role == "host") "user" else "host")); HostRepo.cache[from.id] = from
                activeCallId = o.getString("callId")
                activeCall = ActiveCall(o.getString("callId"), from.id, role, o.optString("kind", "direct"), o.optInt("ratePerMinute"))
                CallEngine.prepare(o.getString("callId"), isCaller = role == "user")
                _events.tryEmit(LiveEvent.Incoming(o.getString("callId"), from, o.optBoolean("auto"), o.optInt("ratePerMinute"), o.optInt("ringSeconds", 30), o.optString("kind", "direct")))
            }
        }
        s.on("call_ringing") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post {
                // role = my side: "user" (I call a host) or "host" (I, a host, call a user); `host` = the person I am calling
                val role = o.optString("role", "user")
                val host = HostRepo.fromJson(o.getJSONObject("host").put("role", if (role == "user") "host" else "user").put("online", true))
                HostRepo.cache[host.id] = HostRepo.cache[host.id]?.copy(status = HostStatus.BUSY) ?: host
                activeCallId = o.getString("callId")
                activeCall = ActiveCall(o.getString("callId"), host.id, role, o.optString("kind", "direct"), o.optInt("ratePerMinute"))
                CallEngine.prepare(o.getString("callId"), isCaller = role == "user")
                _events.tryEmit(LiveEvent.Ringing(o.getString("callId"), host, o.optBoolean("auto"), o.optInt("ringSeconds", 30), o.optString("kind", "direct"), o.optInt("ratePerMinute")))
            }
        }
        s.on("notification") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { NotifRepo.unread++; _events.tryEmit(LiveEvent.Notified(NotifRepo.notice(o))) }
        }
        s.on("chat_message") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post {
                val m = ChatRepo.message(o.getJSONObject("message"))
                ChatRepo.onLive(o.getInt("peerId"), m, o.optJSONObject("from"))
                _events.tryEmit(LiveEvent.ChatMsg(o.getInt("peerId"), m))
            }
        }
        s.on("chat_read") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { ChatRepo.onRead(o.getInt("peerId")); _events.tryEmit(LiveEvent.ChatRead(o.getInt("peerId"))) }
        }
        s.on("chat_typing") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { ChatRepo.typing[o.getInt("peerId")] = System.currentTimeMillis() }
        }
        // a gift was sent (by me or to me): play it over whatever screen is open
        s.on("gift") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { GiftRepo.play(o) }
        }
        s.on("call_need_coins") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { _events.tryEmit(LiveEvent.NeedCoins(o.getString("callId"), o.optInt("need"))) }
        }
        s.on("call_topping") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { _events.tryEmit(LiveEvent.Topping(o.getString("callId"), o.optInt("seconds"))) }
        }
        s.on("call_accepted") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post {
                activeCall = activeCall?.takeIf { it.callId == o.getString("callId") }?.copy(startedAt = System.currentTimeMillis())
                _events.tryEmit(LiveEvent.Accepted(o.getString("callId")))
            }
        }
        s.on("call_ended") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post {
                val id = o.getString("callId")
                val was = activeCall?.takeIf { it.callId == id }
                if (activeCallId == id) activeCallId = null
                if (activeCall?.callId == id) activeCall = null
                if (lastBilling?.callId == id) lastBilling = null
                if (CallEngine.callId == id) CallEngine.stop()
                if (!inForeground) close()   // the call ended while the app was in the background: offline now
                _events.tryEmit(LiveEvent.Ended(id, o.optString("reason"), o.optInt("seconds"), o.optInt("minutes"), o.optInt("coins"), o.optInt("earned"),
                    if (o.isNull("endedBy")) null else o.optString("endedBy"), was?.peerId, was?.role, o.optInt("giftEarned"), o.optInt("gifts")))
            }
        }
        s.on("call_billing") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post {
                if (o.has("coinsLeft")) WalletRepo.setCoins(o.getInt("coinsLeft"))
                lastBilling = LiveEvent.Billing(o.getString("callId"), o.optInt("minute"), o.optInt("coins"), if (o.has("coinsLeft")) o.getInt("coinsLeft") else null, o.optInt("earned"), o.optBoolean("low"))
                _events.tryEmit(LiveEvent.Billing(o.getString("callId"), o.optInt("minute"), o.optInt("coins"), if (o.has("coinsLeft")) o.getInt("coinsLeft") else null, o.optInt("earned"), o.optBoolean("low")))
            }
        }
        s.on("call_peer_lost") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { _events.tryEmit(LiveEvent.PeerLost(o.getString("callId"), o.optInt("seconds"))) }
        }
        s.on("call_peer_back") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { CallEngine.onPeerBack(o.getString("callId")); _events.tryEmit(LiveEvent.PeerBack(o.getString("callId"))) }
        }
        // this phone came back during a running call: keep the call, reconnect the video
        s.on("call_resume") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post {
                activeCallId = o.getString("callId")
                val id = o.getString("callId")
                activeCall = ActiveCall(id, o.optInt("peerId"), o.optString("role"), o.optString("kind"), o.optInt("rate"), if (o.isNull("startedAt")) null else o.optLong("startedAt"))
                if (CallEngine.callId == id) CallEngine.onPeerBack(id)            // same app, connection came back: new video route
                else _events.tryEmit(LiveEvent.Resume(id, o.optInt("peerId"), o.optString("state")))   // app was restarted: reopen the call
            }
        }
        s.on("rtc_signal") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { o.optJSONObject("data")?.let { CallEngine.onSignal(o.getString("callId"), it) } }
        }
        s.on("call_earned") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post { _events.tryEmit(LiveEvent.Earned(o.getString("callId"), o.optInt("earned"))) }
        }
        s.on("call_chat") { a ->
            val o = a.firstOrNull() as? JSONObject ?: return@on
            main.post {
                addCallLine(o.getString("callId"), false, o.optString("text"))
                _events.tryEmit(LiveEvent.CallChat(o.getString("callId"), o.optString("text")))
            }
        }
        s.connect()
    }

    // ---- what the phone may tell the server
    fun profileOpened(hostId: Int) { socket?.emit("profile:open", JSONObject().put("hostId", SafetyStore.publicIdOf(hostId))) }
    fun profileClosed() { socket?.emit("profile:close") }
    /** A user opened / left a host's chat: the server tells the host at once (never starts a call from a chat). */
    fun chatOpened(hostId: Int) { socket?.emit("chat:open", JSONObject().put("hostId", SafetyStore.publicIdOf(hostId))) }
    fun chatClosed() { socket?.emit("chat:close") }
    fun typing(peerId: Int) { socket?.emit("chat:typing", JSONObject().put("peerId", SafetyStore.publicIdOf(peerId))) }
    fun cancelCall(callId: String) { socket?.emit("call:cancel", JSONObject().put("callId", callId)) }
    fun declineCall(callId: String) { socket?.emit("call:decline", JSONObject().put("callId", callId)) }
    /** I am recharging to pick up this host's call (it rings a while longer). */
    fun toppingUp(callId: String) { socket?.emit("call:topping", JSONObject().put("callId", callId)) }
    fun acceptCall(callId: String) { socket?.emit("call:accept", JSONObject().put("callId", callId)) }
    fun endCall(callId: String) { socket?.emit("call:end", JSONObject().put("callId", callId)) }
    fun rtcSignal(callId: String, data: JSONObject) { socket?.emit("rtc:signal", JSONObject().put("callId", callId).put("data", data)) }
    fun callChat(callId: String, text: String) { socket?.emit("call:chat", JSONObject().put("callId", callId).put("text", text)) }

    /** Random match: the server picks an online, free host at the fixed random rate. [onResult] gets null when it rings, else a reason. */
    fun randomStart(gender: String?, skip: List<Int>, onResult: (String?) -> Unit) {
        val s = socket ?: return onResult("Connecting… try again in a moment")
        val msg = JSONObject().put("skip", org.json.JSONArray(skip.map { SafetyStore.publicIdOf(it) }))
        if (gender != null) msg.put("gender", gender)
        s.emit("random:start", msg, Ack { a ->
            val o = a.firstOrNull() as? JSONObject
            main.post {
                onResult(
                    if (o?.optBoolean("ok") == true) null
                    else when (o?.optString("reason")) {
                        "no_hosts" -> "no_hosts"
                        "not_enough_coins" -> "not_enough_coins"
                        "in_call" -> "You are already in a call"
                        else -> "Can't connect right now"
                    },
                )
            }
        })
    }

    /** The user pressed the call button. [onResult] gets null when it is ringing, else a sentence to show. */
    fun startCall(hostId: Int, onResult: (String?) -> Unit) {
        val s = socket ?: return onResult("Connecting… try again in a moment")
        s.emit("call:start", JSONObject().put("hostId", SafetyStore.publicIdOf(hostId)), Ack { a ->
            val o = a.firstOrNull() as? JSONObject
            main.post {
                onResult(
                    if (o?.optBoolean("ok") == true) null
                    else when (o?.optString("reason")) {
                        "offline" -> "Offline right now"
                        "busy" -> "On another call right now"
                        "user_no_coins" -> "They don't have enough coins for a call"
                        "in_call" -> "You are already in a call"
                        "not_enough_coins" -> "Not enough coins · please recharge"
                        else -> "Can't call right now"
                    },
                )
            }
        })
    }
}
