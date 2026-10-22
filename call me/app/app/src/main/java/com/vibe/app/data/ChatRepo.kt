package com.vibe.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vibe.app.data.api.Api
import org.json.JSONObject

/** A gift as the server describes it: picture and animation files are uploaded in the admin panel (any of mp4, svga, lottie, webp, gif). */
data class GiftItem(
    val id: Int, val name: String, val emoji: String?, val price: Int, val category: String,
    val thumbUrl: String?, val animUrl: String?, val animType: String?,
)

data class ChatMessage(
    val id: Long, val mine: Boolean, val kind: String, val text: String?, val gift: GiftItem?, val quantity: Int,
    val coins: Int, val at: String, val read: Boolean, val clientId: String?,
    /** only on the phone: still being sent / could not be sent */
    val sending: Boolean = false, val failed: Boolean = false,
)

data class ChatSummary(val peer: Host, val last: ChatMessage?, val unread: Int, val lastAt: String)

/** One gift being played on screen: [from] is the sender's name ("You" for this phone). */
data class GiftPlay(val id: Long, val gift: GiftItem, val quantity: Int, val fromName: String, val toName: String?, val mine: Boolean)

/**
 * Chats and gifts. Messages are sent to the server (which decides free / paid and charges the coins); new messages, read ticks,
 * "typing…" and gifts arrive live over Socket.IO (see Realtime). Nothing about prices is decided here.
 */
object ChatRepo {
    /** Total unread messages (tab bar badge). */
    var unread by mutableIntStateOf(0)
    val chats = mutableStateListOf<ChatSummary>()
    /** The chat open on screen right now (its new messages are read at once, not counted as unread). */
    @Volatile var openPeer: Int? = null
    /** People typing to me just now, by public id -> time. */
    val typing = androidx.compose.runtime.mutableStateMapOf<Int, Long>()

    fun gift(o: JSONObject?): GiftItem? = o?.let {
        fun s(k: String) = if (it.isNull(k)) null else it.optString(k).takeIf { v -> v.isNotBlank() }
        GiftItem(it.getInt("id"), it.getString("name"), s("emoji"), it.getInt("price"), it.optString("category"), s("thumbUrl"), s("animUrl"), s("animType"))
    }

    fun message(o: JSONObject) = ChatMessage(
        o.getLong("id"), o.optBoolean("mine"), o.optString("kind"), if (o.isNull("text")) null else o.optString("text"), gift(o.optJSONObject("gift")),
        o.optInt("quantity", 1), o.optInt("coins"), o.optString("at"), o.optBoolean("read"), if (o.isNull("clientId")) null else o.optString("clientId"),
    )

    suspend fun refreshList() {
        val r = Api.get("/chats")
        val a = r.getJSONArray("chats")
        val list = List(a.length()) {
            val o = a.getJSONObject(it)
            val peer = HostRepo.fromJson(o.getJSONObject("peer")).also { p -> HostRepo.cache[p.id] = p }
            ChatSummary(peer, o.optJSONObject("last")?.let(::message), o.optInt("unread"), o.optString("lastAt"))
        }
        chats.clear(); chats.addAll(list)
        unread = r.optInt("unreadTotal")
    }

    suspend fun refreshUnread() { runCatching { unread = Api.get("/chats/unread").optInt("unread") } }

    data class Page(val peer: Host, val messages: List<ChatMessage>, val more: Boolean, val freeLeft: Int?, val messagePrice: Int)

    suspend fun messages(peerId: Int, before: Long? = null): Page {
        val r = Api.get("/chats/${SafetyStore.publicIdOf(peerId)}/messages" + (before?.let { "?before=$it" } ?: ""))
        val peer = HostRepo.fromJson(r.getJSONObject("peer")).also { HostRepo.cache[it.id] = it }
        val a = r.getJSONArray("messages")
        return Page(peer, List(a.length()) { message(a.getJSONObject(it)) }, r.optBoolean("more"),
            if (r.isNull("freeLeft")) null else r.optInt("freeLeft"), r.optInt("messagePrice"))
    }

    data class Sent(val message: ChatMessage, val coins: Int, val freeLeft: Int?)

    suspend fun send(peerId: Int, text: String, clientId: String): Sent {
        val r = Api.post("/chats/${SafetyStore.publicIdOf(peerId)}/messages", JSONObject().put("text", text).put("clientId", clientId))
        val coins = r.optInt("coins"); WalletRepo.setCoins(coins)
        return Sent(message(r.getJSONObject("message")), coins, if (r.isNull("freeLeft")) null else r.optInt("freeLeft"))
    }

    suspend fun markRead(peerId: Int) {
        runCatching { Api.post("/chats/${SafetyStore.publicIdOf(peerId)}/read") }
        val i = chats.indexOfFirst { it.peer.id == peerId }
        if (i >= 0 && chats[i].unread > 0) { unread = (unread - chats[i].unread).coerceAtLeast(0); chats[i] = chats[i].copy(unread = 0) }
    }

    /** A message arrived live: keep the chat list and the badge right (the open chat screen listens itself). */
    fun onLive(peerId: Int, m: ChatMessage, from: JSONObject?) {
        from?.let { val h = HostRepo.fromJson(it); if (HostRepo.cache[h.id] == null) HostRepo.cache[h.id] = h }
        typing.remove(peerId)
        val peer = HostRepo.cache[peerId] ?: return
        val i = chats.indexOfFirst { it.peer.id == peerId }
        val add = if (!m.mine && openPeer != peerId) 1 else 0
        val row = ChatSummary(peer, m, (if (i >= 0) chats[i].unread else 0) + add, m.at)
        if (i >= 0) chats.removeAt(i)
        chats.add(0, row)
        unread += add
    }

    fun onRead(peerId: Int) {
        val i = chats.indexOfFirst { it.peer.id == peerId }
        if (i >= 0) chats[i].last?.let { if (it.mine) chats[i] = chats[i].copy(last = it.copy(read = true)) }
    }

    private var lastTypingSent = 0L
    fun sendTyping(peerId: Int) {
        val now = System.currentTimeMillis()
        if (now - lastTypingSent > 2000) { lastTypingSent = now; Realtime.typing(peerId) }
    }
}

/** The gift catalogue (cached) and sending a gift. Both phones play the animation through [Realtime] events. */
object GiftRepo {
    var catalogue by mutableStateOf<List<GiftItem>>(emptyList())
        private set
    /** Gift events this phone already played (the server also sends the sender's own gift back). */
    private val played = java.util.Collections.synchronizedSet(mutableSetOf<Long>())

    suspend fun load(): List<GiftItem> {
        if (catalogue.isEmpty()) {
            val a = Api.get("/gifts").getJSONArray("gifts")
            catalogue = List(a.length()) { ChatRepo.gift(a.getJSONObject(it))!! }
        }
        return catalogue
    }

    suspend fun reload() { catalogue = emptyList(); load() }

    /** Sends [quantity] of [gift] to a host. Returns the coins left. Plays the animation here at once. */
    suspend fun send(toId: Int, gift: GiftItem, quantity: Int, context: String): Int {
        val r = Api.post(
            "/gifts/send",
            JSONObject().put("toId", SafetyStore.publicIdOf(toId)).put("giftId", gift.id).put("quantity", quantity).put("context", context)
                .put("clientId", "g" + java.util.UUID.randomUUID().toString().take(20)),
        )
        val coins = r.optInt("coins"); WalletRepo.setCoins(coins)
        r.optJSONObject("gift")?.let { e -> play(e) }
        return coins
    }

    /** Puts a gift event on screen once (from the send answer or the live event, whichever comes first). */
    fun play(e: JSONObject) {
        val id = e.getLong("id")
        if (!played.add(id)) return
        val gift = ChatRepo.gift(e.optJSONObject("gift")) ?: return
        val from = e.optJSONObject("from")
        val myId = com.vibe.app.data.api.AuthStore.me?.optLong("id")
        val mine = from?.optLong("id") == myId
        val toName = HostRepo.cache[e.optInt("to")]?.name
        val play = GiftPlay(id, gift, e.optInt("quantity", 1), if (mine) "You" else from?.optString("name").orEmpty().ifBlank { "Someone" }, toName, mine)
        // a received gift plays in full only in that person's chat or call; anywhere else a small banner says who sent what
        val fromId = from?.optInt("id")
        if (!mine && from != null && fromId != ChatRepo.openPeer && Realtime.activeCall?.peerId != fromId) {
            GiftQueue.banner = HostRepo.fromJson(from) to play
            return
        }
        GiftQueue.add(play)
    }
}

/** Gifts waiting to be played one after another, over whatever screen is open. */
object GiftQueue {
    val items = mutableStateListOf<GiftPlay>()
    fun add(p: GiftPlay) { if (items.size < 20) items.add(p) }
    fun done() { if (items.isNotEmpty()) items.removeAt(0) }
    /** A gift received outside its chat / call: shown as a top banner (tap opens the chat). */
    var banner by mutableStateOf<Pair<Host, GiftPlay>?>(null)
}
