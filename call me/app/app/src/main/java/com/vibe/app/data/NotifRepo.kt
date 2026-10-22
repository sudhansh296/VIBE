package com.vibe.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.vibe.app.data.api.Api
import org.json.JSONObject

/** One line of the bell list, as the server sends it. [peerId] = the person it is about (missed call, gift, follower), if any. */
data class Notice(val id: Long, val kind: String, val title: String, val body: String?, val at: String, val read: Boolean, val peerId: Int?)

/** The bell list (server side) and its unread count for the dot on the bell. */
object NotifRepo {
    var unread by mutableIntStateOf(0)

    fun notice(o: JSONObject): Notice {
        val d = o.optJSONObject("data")
        return Notice(
            o.getLong("id"), o.optString("kind"), o.optString("title"), if (o.isNull("body")) null else o.optString("body"),
            o.optString("at"), o.optBoolean("read"), d?.takeIf { it.has("peerId") && !it.isNull("peerId") }?.optInt("peerId"),
        )
    }

    suspend fun refreshUnread() { runCatching { unread = Api.get("/notifications/unread").optInt("unread") } }

    /** Newest first; [before] = the oldest id already shown. Returns the items and whether there are more. */
    suspend fun page(before: Long? = null): Pair<List<Notice>, Boolean> {
        val r = Api.get("/notifications" + (before?.let { "?before=$it" } ?: ""))
        unread = r.optInt("unread")
        val a = r.getJSONArray("items")
        return List(a.length()) { notice(a.getJSONObject(it)) } to r.optBoolean("more")
    }

    suspend fun readAll() { runCatching { Api.post("/notifications/read"); unread = 0 } }
    suspend fun delete(id: Long) { runCatching { Api.delete("/notifications/$id") } }
}
