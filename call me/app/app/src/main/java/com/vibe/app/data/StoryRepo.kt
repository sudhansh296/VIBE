package com.vibe.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vibe.app.data.api.Api
import org.json.JSONObject

/** One status (photo or video) as the server sends it; [url] is a protected file path. */
data class StoryItem(
    val id: Long, val video: Boolean, val url: String, val durationSec: Int?, val caption: String?, val at: String,
    val seen: Boolean, val views: Int = 0,
)

/** A host with statuses up right now. */
data class StoryPerson(val person: Host, val stories: List<StoryItem>, val allSeen: Boolean)

/** Status (stories) from the server: hosts post, users watch (same rules as profiles). Files expire after the admin's hours. */
object StoryRepo {
    val people = mutableStateListOf<StoryPerson>()
    val mine = mutableStateListOf<StoryItem>()
    var canPost by mutableStateOf(false)

    private fun item(o: JSONObject) = StoryItem(
        o.getLong("id"), o.optString("kind") == "video", o.optString("url"), if (o.isNull("durationSec")) null else o.optInt("durationSec"),
        if (o.isNull("caption")) null else o.optString("caption"), o.optString("at"), o.optBoolean("seen"), o.optInt("views"),
    )

    suspend fun load() {
        val r = Api.get("/stories")
        canPost = r.optBoolean("canPost")
        val a = r.getJSONArray("people")
        val list = List(a.length()) { i ->
            val o = a.getJSONObject(i)
            val h = HostRepo.fromJson(o.getJSONObject("person")); HostRepo.cache[h.id] = HostRepo.cache[h.id] ?: h
            val s = o.getJSONArray("stories")
            StoryPerson(HostRepo.cache[h.id] ?: h, List(s.length()) { item(s.getJSONObject(it)) }, o.optBoolean("allSeen"))
        }
        people.clear(); people.addAll(list)
        if (canPost) runCatching { loadMine() }
    }

    suspend fun loadMine() {
        val r = Api.get("/stories/mine")
        val a = r.getJSONArray("stories")
        mine.clear(); mine.addAll(List(a.length()) { item(a.getJSONObject(it)) })
    }

    /** I watched [id]: counted on the server; the ring turns grey once all of a person's statuses are seen. */
    suspend fun markSeen(id: Long) {
        runCatching { Api.post("/stories/$id/view") }
        val i = people.indexOfFirst { p -> p.stories.any { it.id == id } }
        if (i >= 0) {
            val p = people[i]
            val list = p.stories.map { if (it.id == id) it.copy(seen = true) else it }
            people[i] = p.copy(stories = list, allSeen = list.all { it.seen })
        }
    }

    suspend fun post(bytes: ByteArray, mime: String, caption: String?): StoryItem {
        val r = Api.upload("/stories", "file", if (mime.startsWith("video")) "status.mp4" else "status.jpg", mime, bytes,
            caption?.takeIf { it.isNotBlank() }?.let { mapOf("caption" to it) } ?: emptyMap())
        val s = item(r.getJSONObject("story"))
        mine.add(s)
        return s
    }

    suspend fun delete(id: Long) { Api.delete("/stories/$id"); mine.removeAll { it.id == id } }

    /** Who viewed my status: name + when. */
    suspend fun viewers(id: Long): List<Pair<Host, String>> {
        val a = Api.get("/stories/$id/viewers").getJSONArray("viewers")
        return List(a.length()) { a.getJSONObject(it).let { o -> HostRepo.fromJson(o) to o.optString("viewedAt") } }
    }
}
