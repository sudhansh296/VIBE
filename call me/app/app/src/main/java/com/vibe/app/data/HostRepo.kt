package com.vibe.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.vibe.app.data.api.Api
import org.json.JSONArray
import org.json.JSONObject

/** One photo or video of a host as the server describes it to the person looking (locked ones need coins first). */
data class ServerMedia(
    val id: Int, val video: Boolean, val durationSec: Int, val locked: Boolean, val price: Int,
    val isMain: Boolean = false, val lockedByHost: Boolean = false, val free: Boolean = false,
) {
    /** Where the file is (needs the login token: see AuthImage). */
    val path get() = "/media/$id/file"
}

data class Profile(val host: Host, val media: List<ServerMedia>, val stats: ProfileStats? = null)

/** A host's public numbers from the server (calls, answer rate, gifts received, "what people say"). */
data class ProfileStats(
    val calls: Int, val answerRate: Int?, val giftsTotal: Int,
    val gifts: List<Triple<String, String?, Int>>,   // name, emoji, how many
    val tags: List<Pair<String, Int>>,
)

/**
 * The people from the server: the Home list, one profile, follow. The app only shows what the server sends and asks the server to do
 * the rest (the server decides who may see whom, what is locked and what it costs).
 */
object HostRepo {
    /** Every person seen so far, by public id. Screens read through Demo.host(). */
    val cache = mutableStateMapOf<Int, Host>()
    /** Ids shown on Home, in order. */
    val home = mutableStateListOf<Int>()
    var nextPage by mutableStateOf<Int?>(null)
        private set

    private val palette = listOf(Color(0xFFFF9EC3) to Color(0xFFFF4D8D), Color(0xFFFFB3CF) to Color(0xFFFF7AB0), Color(0xFFFF8FB8) to Color(0xFFF0447F))

    private fun text(o: JSONObject, key: String) = if (o.isNull(key)) "" else o.optString(key)

    fun fromJson(o: JSONObject): Host {
        val id = o.getInt("id")
        val langs = o.optJSONArray("languages")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
        val g = when (text(o, "gender")) { "female" -> 'F'; "male" -> 'M'; else -> 'O' }
        val isHost = text(o, "role") == "host"
        // hosts and users alike: the server says who is online / on a call (users' status is sent to hosts only).
        // Short cards inside events (a gift, a message, a call) carry no status: then the one already known stays.
        val known = cache[id]
        val status = when {
            !o.has("online") && !o.has("busy") && known != null -> known.status
            o.optBoolean("busy") -> HostStatus.BUSY
            o.optBoolean("online") -> HostStatus.ONLINE
            else -> HostStatus.OFFLINE
        }
        val (c1, c2) = palette[id.mod(palette.size)]
        val fresh = Host(
            id = id, name = text(o, "name").ifBlank { "VIBE user" }, age = o.optInt("age", 0), gender = g, city = text(o, "city"),
            rate = o.optInt("callRate", 0), status = status, level = o.optInt("level", 1),
            rating = if (o.isNull("rating")) 0.0 else o.optDouble("rating", 0.0), ratingCount = o.optInt("ratingCount", 0), followers = compact(o.optInt("followers", 0)),
            calls = 0, answerRate = 0, langs = langs, bio = text(o, "bio"), c1 = c1, c2 = c2, verified = isHost, isNew = o.optBoolean("isNew"),
            photo = text(o, "dp").ifBlank { null }, following = o.optBoolean("following"), real = true, vip = o.optBoolean("vip"),
        )
        // a short card (gift, message, call) only has a few fields: the rest stay as already known
        if (known == null) return fresh
        return fresh.copy(
            name = if (o.has("name")) fresh.name else known.name,
            age = if (o.has("age")) fresh.age else known.age,
            gender = if (o.has("gender")) fresh.gender else known.gender,
            city = if (o.has("city")) fresh.city else known.city,
            rate = if (o.has("callRate")) fresh.rate else known.rate,
            level = if (o.has("level")) fresh.level else known.level,
            rating = if (o.has("rating")) fresh.rating else known.rating,
            ratingCount = if (o.has("ratingCount")) fresh.ratingCount else known.ratingCount,
            followers = if (o.has("followers")) fresh.followers else known.followers,
            langs = if (o.has("languages")) fresh.langs else known.langs,
            bio = if (o.has("bio")) fresh.bio else known.bio,
            photo = if (o.has("dp")) fresh.photo else known.photo,
            vip = if (o.has("vip")) fresh.vip else known.vip,
            isNew = if (o.has("isNew")) fresh.isNew else known.isNew,
            following = if (o.has("following")) fresh.following else known.following,
        )
    }

    private fun compact(n: Int) = if (n >= 1000) "%.1fk".format(n / 1000.0).replace(".0k", "k") else n.toString()

    private fun put(list: JSONArray): List<Host> = List(list.length()) { fromJson(list.getJSONObject(it)) }.onEach { cache[it.id] = it }

    /** The query for /discover from the Home filter. [quick]: all, F, M or online. */
    private fun query(tab: String, f: HostFilter, quick: String, page: Int): String {
        val q = mutableListOf("tab=$tab", "page=$page")
        when {
            quick == "F" || f.show == "F" -> q += "gender=female"
            quick == "M" || f.show == "M" -> q += "gender=male"
        }
        if (quick == "online" || f.onlineOnly) q += "online=1"
        if (f.minAge > 18) q += "ageMin=${f.minAge}"
        if (f.maxAge < 40) q += "ageMax=${f.maxAge}"
        if (f.maxRate < 200) q += "maxPrice=${f.maxRate}"
        if (f.langs.isNotEmpty()) q += "languages=" + java.net.URLEncoder.encode(f.langs.joinToString(","), "UTF-8")
        return q.joinToString("&")
    }

    /** Loads the first page (replaces the list) or the next one (adds to it). */
    suspend fun loadHome(tab: String, f: HostFilter, quick: String, more: Boolean = false) {
        val page = if (more) (nextPage ?: return) else 1
        val res = Api.get("/discover?" + query(tab, f, quick, page))
        val hosts = put(res.getJSONArray("people"))
        if (!more) home.clear()
        home.addAll(hosts.map { it.id }.filter { it !in home })
        nextPage = if (res.isNull("nextPage")) null else res.optInt("nextPage")
    }

    /** One profile. Counts as a visit on the server. */
    suspend fun profile(id: Int): Profile {
        val res = Api.get("/profile/${SafetyStore.publicIdOf(id)}")
        val h = fromJson(res.getJSONObject("person")); cache[h.id] = h
        val m = res.optJSONObject("media")
        val items = buildList {
            m?.optJSONArray("photos")?.let { a -> for (i in 0 until a.length()) add(item(a.getJSONObject(i), false)) }
            m?.optJSONArray("videos")?.let { a -> for (i in 0 until a.length()) add(item(a.getJSONObject(i), true)) }
        }
        val st = res.optJSONObject("stats")?.let { s ->
            val g = s.optJSONArray("gifts"); val t = s.optJSONArray("tags")
            ProfileStats(
                s.optInt("calls"), if (s.isNull("answerRate")) null else s.optInt("answerRate"), s.optInt("giftsTotal"),
                List(g?.length() ?: 0) { i -> g!!.getJSONObject(i).let { Triple(it.optString("name"), it.optString("emoji").takeIf { e -> e.isNotBlank() && e != "null" }, it.optInt("count")) } },
                List(t?.length() ?: 0) { i -> t!!.getJSONObject(i).let { it.optString("tag") to it.optInt("count") } },
            )
        }
        return Profile(h, items, st)
    }

    fun item(o: JSONObject, video: Boolean) = ServerMedia(
        id = o.getInt("id"), video = video, durationSec = o.optInt("durationSec"), locked = o.optBoolean("locked"), price = o.optInt("price"),
        isMain = o.optBoolean("isMain"), lockedByHost = o.optBoolean("lockedByHost"), free = o.optBoolean("free"),
    )

    /** Pays for a locked file. Returns the coins left. */
    suspend fun unlock(mediaId: Int): Int {
        val res = Api.post("/media/$mediaId/unlock")
        val coins = res.optInt("coins")
        com.vibe.app.data.api.AuthStore.user?.let { u -> com.vibe.app.data.api.AuthStore.saveUser(u.put("coins", coins)) }
        return coins
    }

    suspend fun follow(id: Int, on: Boolean) {
        val pid = SafetyStore.publicIdOf(id)
        val res = if (on) Api.post("/follow/$pid") else Api.delete("/follow/$pid")
        // the server answers with the new follower count
        cache[id]?.let { cache[id] = it.copy(following = on, followers = compact(res.optInt("followers"))) }
    }

    suspend fun people(path: String): List<Host> = put(Api.get(path).getJSONArray("people"))

    /** Live status change from the server (a host opened / closed the app, started / ended a call). */
    fun presence(publicId: Int, online: Boolean?, busy: Boolean?) {
        val h = cache[publicId] ?: return
        var st = h.status
        if (online != null) st = if (online) HostStatus.ONLINE else HostStatus.OFFLINE
        if (busy != null && online != false) st = if (busy) HostStatus.BUSY else HostStatus.ONLINE
        if (st != h.status) cache[publicId] = h.copy(status = st)
    }
}
