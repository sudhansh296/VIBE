package com.vibe.app.ui.screens.host

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.HostDemo
import com.vibe.app.data.HostRepo
import com.vibe.app.data.ServerMedia
import com.vibe.app.data.api.Api
import com.vibe.app.data.api.ApiException
import com.vibe.app.data.api.AuthStore
import com.vibe.app.data.api.Uploads
import com.vibe.app.ui.components.BackButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PillChip
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.ServerImage
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

private val TITLES = listOf("Your profile", "Live selfie", "Verify identity", "Submit")
private val BUTTONS = listOf("Continue", "Continue", "Continue", "Submit for review")
private val ID_NAMES = mapOf("aadhaar" to "Aadhaar", "driving_licence" to "Driving licence", "voter_id" to "Voter ID", "passport" to "Passport")

/** What the server knows about this person's application. */
private data class AppState(
    val status: String = "none", val rejectReason: String? = null, val bio: String = "", val languages: List<String> = emptyList(),
    val panName: String = "", val panMasked: String? = null, val idType: String? = null, val idNumberMasked: String? = null, val hasSelfie: Boolean = false, val hasId: Boolean = false,
    val agencyCode: String = "", val minPhotos: Int = 3, val minVideos: Int = 1, val bioMin: Int = 10, val idTypes: List<String> = emptyList(),
    val missing: List<String> = emptyList(),
)

private fun appOf(o: JSONObject): AppState {
    fun list(a: org.json.JSONArray?) = if (a == null) emptyList() else List(a.length()) { a.getString(it) }
    val r = o.optJSONObject("requirements")
    return AppState(
        o.optString("status", "none"), if (o.isNull("rejectReason")) null else o.optString("rejectReason"), o.optString("bio"), list(o.optJSONArray("languages")),
        o.optString("panName"), if (o.isNull("panMasked")) null else o.optString("panMasked"), if (o.isNull("idType")) null else o.optString("idType"),
        if (o.isNull("idNumberMasked")) null else o.optString("idNumberMasked"),
        o.optBoolean("hasSelfie"), o.optBoolean("hasId"), o.optString("agencyCode"),
        r?.optInt("minPhotos", 3) ?: 3, r?.optInt("minVideos", 1) ?: 1, r?.optInt("bioMinChars", 10) ?: 10, list(r?.optJSONArray("idTypes")), list(o.optJSONArray("missing")),
    )
}

/**
 * Host application: 1 photos + intro video + bio, 2 live selfie, 3 PAN + ID, 4 agency code and submit. Everything is sent to the server,
 * which stores the PAN encrypted, keeps the selfie and ID private, tells us what is still missing and lets the admin decide.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostApplicationScreen(onBack: () -> Unit, onPreviewDashboard: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var app by remember { mutableStateOf(AppState()) }
    var photos by remember { mutableStateOf<List<ServerMedia>>(emptyList()) }
    var videos by remember { mutableStateOf<List<ServerMedia>>(emptyList()) }
    var maxPhotos by remember { mutableStateOf(6) }
    var maxVideoSec by remember { mutableStateOf(30) }
    var defaultRate by remember { mutableStateOf(0) }
    var allLangs by remember { mutableStateOf(HostDemo.languages) }
    var step by remember { mutableStateOf(0) }

    var bio by remember { mutableStateOf("") }
    val langs = remember { mutableStateListOf<String>() }
    var pname by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var idType by remember { mutableStateOf("aadhaar") }
    var agency by remember { mutableStateOf("") }
    var agree by remember { mutableStateOf(false) }
    var selfieUri by remember { mutableStateOf<Uri?>(null) }
    var live by remember { mutableStateOf<String?>(null) }   // intro | selfie | id : a live camera screen on top
    var reapply by remember { mutableStateOf(false) }

    fun mediaFrom(res: JSONObject) {
        photos = List(res.getJSONArray("photos").length()) { HostRepo.item(res.getJSONArray("photos").getJSONObject(it), false) }
        videos = List(res.getJSONArray("videos").length()) { HostRepo.item(res.getJSONArray("videos").getJSONObject(it), true) }
        res.optJSONObject("limits")?.let { maxPhotos = it.optInt("maxPhotos", 6); maxVideoSec = it.optInt("maxVideoSeconds", 30) }
    }
    fun appFrom(res: JSONObject) { app = appOf(res.getJSONObject("application")) }
    /** One server action with a spinner; errors are shown as the server wrote them. */
    fun act(done: String? = null, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { block(); if (done != null) ctx.toast(done) } catch (e: ApiException) { ctx.toast(e.message) }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        try {
            appFrom(Api.get("/host/application"))
            mediaFrom(Api.get("/me/media"))
            runCatching { Api.get("/me") }.onSuccess { AuthStore.saveUser(it.getJSONObject("user")) } // an approval may have happened meanwhile
            val cfg = runCatching { Api.get("/config", auth = false).getJSONObject("config") }.getOrNull()
            cfg?.optJSONArray("allowed_languages")?.let { a -> allLangs = List(a.length()) { a.getString(it) } }
            defaultRate = cfg?.optInt("host_call_rate_default", 0) ?: 0
            bio = app.bio; langs.clear(); langs.addAll(app.languages); pname = app.panName; app.idType?.let { idType = it }; agency = app.agencyCode
        } catch (e: ApiException) { failed = e.message }
        loading = false
    }

    // pickers: photos and the intro video from the gallery, the selfie and the ID with the camera / gallery
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) act("Photo added") {
            val res = Api.upload("/me/media", "file", "photo.jpg", "image/jpeg", Uploads.photoBytes(ctx, uri)); mediaFrom(res); appFrom(Api.get("/host/application"))
        }
    }
    // live camera results: the intro video, the selfie and the ID are always taken now inside the app (never from the gallery)
    fun introRecorded(f: File) {
        live = null
        act("Intro video saved") {
            videos.firstOrNull()?.let { old -> mediaFrom(Api.delete("/me/media/${old.id}")) } // replacing the intro
            mediaFrom(Api.upload("/me/media", "file", "intro.mp4", "video/mp4", kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { f.readBytes() }))
            appFrom(Api.get("/host/application"))
            f.delete()
        }
    }
    fun selfieTaken(f: File) {
        live = null
        act("Selfie saved") {
            val u = Uri.fromFile(f)
            appFrom(Api.upload("/host/application/selfie", "photo", "selfie.jpg", "image/jpeg", Uploads.photoBytes(ctx, u)))
            selfieUri = u
        }
    }
    fun idTaken(f: File, number: String) {
        live = null
        act("ID saved") {
            // number first: if the server says it is not valid, nothing else changes and the person can try again
            appFrom(Api.put("/host/application", JSONObject().put("idType", idType).put("idNumber", number)))
            appFrom(Api.upload("/host/application/id", "photo", "id.jpg", "image/jpeg", Uploads.photoBytes(ctx, Uri.fromFile(f))))
            f.delete()
        }
    }

    val panOk = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$").matches(pan)
    val canNext = when (step) {
        0 -> photos.size >= app.minPhotos && videos.size >= app.minVideos && bio.trim().length >= app.bioMin && langs.isNotEmpty()
        1 -> app.hasSelfie
        2 -> pname.trim().length > 2 && (panOk || app.panMasked != null) && app.hasId && app.idNumberMasked != null && app.idType == idType
        else -> agree
    }

    /** Saves the written fields of the current step, then moves on. */
    fun next() {
        act {
            when (step) {
                0 -> appFrom(Api.put("/host/application", JSONObject().put("bio", bio.trim()).put("languages", org.json.JSONArray(langs.toList()))))
                2 -> appFrom(Api.put("/host/application", JSONObject().put("panName", pname.trim()).put("idType", idType).apply { if (panOk) put("panNumber", pan) }))
                3 -> {
                    appFrom(Api.put("/host/application", JSONObject().put("agencyCode", agency.trim())))
                    appFrom(Api.post("/host/application/submit"))
                    reapply = false
                    return@act
                }
            }
            step++
        }
    }

    BackHandler(enabled = live == null && !loading && showForm(app, reapply) && step > 0) { step-- }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))

        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = VibeColors.Pink, strokeWidth = 3.dp, modifier = Modifier.size(32.dp)) }
        else if (failed != null) Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            BackButton { onBack() }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(failed.orEmpty(), color = VibeColors.TextMute, fontSize = 14.sp, textAlign = TextAlign.Center) }
        }
        else if (showForm(app, reapply)) Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BackButton { if (step > 0) step-- else onBack() }
                Text(TITLES[step], color = VibeColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(4) { i -> Box(Modifier.height(6.dp).width(if (i <= step) 20.dp else 10.dp).clip(CircleShape).background(if (i <= step) VibeColors.Pink else VibeColors.Line)) }
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                when (step) {
                    0 -> {
                        SectionLabel("Photos (at least ${app.minPhotos})", Modifier.padding(top = 8.dp, bottom = 8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val slots = maxPhotos.coerceAtLeast(photos.size).coerceAtMost(9)
                            (0 until slots).chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { i ->
                                        val m = photos.getOrNull(i)
                                        PhotoSlot(i, m, Modifier.weight(1f)) {
                                            if (busy) return@PhotoSlot
                                            if (m == null) pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            else act("Photo removed") { mediaFrom(Api.delete("/me/media/${m.id}")); appFrom(Api.get("/host/application")) }
                                        }
                                    }
                                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                                }
                            }
                        }
                        Text("Clear photos of yourself only. The first photo is your main picture.", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))

                        SectionLabel("Intro video (up to $maxVideoSec seconds)", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        UploadRow(
                            CallIcons.Camera, VibeColors.Cyan, if (videos.isEmpty()) "Record your intro video" else "Record again",
                            if (videos.isNotEmpty()) "Intro recorded · 0:%02d".format(videos.first().durationSec) else "Live with the front camera · say hi and the languages you speak", videos.isNotEmpty(),
                        ) { if (!busy) live = "intro" }

                        SectionLabel("Short bio", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        VibeField(bio, { bio = it.take(150) }, "Tell users what you love talking about", singleLine = false, minHeight = 90.dp)
                        Text("${bio.length}/150", color = VibeColors.TextMute, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))

                        SectionLabel("Languages you speak", Modifier.padding(top = 16.dp, bottom = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            allLangs.forEach { l -> PillChip(l, l in langs, { if (l in langs) langs.remove(l) else if (langs.size < 6) langs.add(l) }) }
                        }
                        Box(Modifier.height(24.dp))
                    }

                    1 -> {
                        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val ovalColor = if (app.hasSelfie) VibeColors.Green else Color.White.copy(alpha = 0.75f)
                            Box(
                                Modifier.size(190.dp, 250.dp).clip(OvalShape).background(Brush.radialGradient(listOf(Color(0xFF5B2A44), Color(0xFF1A0D16)))).border(3.dp, ovalColor, OvalShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                val u = selfieUri
                                if (u != null) com.vibe.app.ui.components.UriImage(u, Modifier.fillMaxSize())
                                else Icon(CallIcons.User, null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(96.dp))
                            }
                            Text(
                                if (app.hasSelfie) "Selfie verified and saved" else "A quick live check: look straight, turn your head, smile",
                                color = if (app.hasSelfie) VibeColors.Green else VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp),
                            )
                            Box(
                                Modifier.padding(top = 20.dp).height(48.dp).clip(CircleShape).background(VibeBrush.Button).clickable { if (!busy) live = "selfie" }.padding(horizontal = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(if (app.hasSelfie) "Do it again" else "Start selfie check", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        }
                        val iShape = RoundedCornerShape(16.dp)
                        Row(
                            Modifier.padding(top = 24.dp).fillMaxWidth().clip(iShape).background(VibeColors.Card).border(1.dp, VibeColors.Ink.copy(alpha = 0.10f), iShape).padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(CallIcons.Lock, null, tint = VibeColors.Cyan, modifier = Modifier.size(20.dp))
                            Text(
                                "Only the VIBE team sees this selfie. It is compared with your photos to make sure they are really you.",
                                color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 17.sp,
                            )
                        }
                    }

                    2 -> {
                        InfoCard("Needed to confirm you are 18+ and for payouts and tax. Your documents are private.", Modifier.padding(top = 8.dp))
                        SectionLabel("Date of birth", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        val fShape = RoundedCornerShape(16.dp)
                        val me = AuthStore.me
                        Row(
                            Modifier.fillMaxWidth().clip(fShape).background(VibeColors.Card).border(1.5.dp, VibeColors.Line, fShape).padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${me?.optString("dob").orEmpty()} · from your profile", color = VibeColors.Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Box(Modifier.clip(CircleShape).background(VibeColors.Green.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${me?.optInt("age") ?: ""} yrs ✓", color = VibeColors.Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        SectionLabel("Full name (as on PAN)", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        VibeField(pname, { pname = it.take(60) }, "Your full name")
                        SectionLabel("PAN number", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        VibeField(pan, { pan = it.uppercase().take(10) }, app.panMasked ?: "ABCDE1234F", caps = true)
                        Text(
                            when {
                                pan.isEmpty() && app.panMasked != null -> "Saved: ${app.panMasked}. Type again only to change it."
                                pan.isEmpty() -> ""
                                panOk -> "PAN format looks right"
                                else -> "PAN is 5 letters, 4 digits, 1 letter"
                            },
                            color = if (panOk || pan.isEmpty()) VibeColors.Green else VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
                        )
                        SectionLabel("ID proof", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            (app.idTypes.ifEmpty { ID_NAMES.keys.toList() }).forEach { t -> PillChip(ID_NAMES[t] ?: t, t == idType, { idType = t }) }
                        }
                        Box(Modifier.padding(top = 12.dp, bottom = 24.dp)) {
                            val idDone = app.hasId && app.idNumberMasked != null && app.idType == idType
                            UploadRow(
                                MeIcons.IdCard, VibeColors.Gold, if (idDone) "ID saved" else "Scan your ${ID_NAMES[idType] ?: "ID"}",
                                if (idDone) "Number ${app.idNumberMasked} · tap to scan again" else "Live photo with the back camera · the number is read for you", idDone,
                            ) { if (!busy) live = "id" }
                        }
                    }

                    else -> {
                        SectionLabel("Starting video rate (coins per minute)", Modifier.padding(top = 8.dp, bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { RateTile("Video call", defaultRate, Modifier.weight(1f)) }
                        Text("VIBE sets your first rate. You can change it in Call settings after approval.", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))

                        SectionLabel("Agency code (optional)", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        VibeField(agency, { agency = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(20) }, "e.g. AG1045", caps = true)
                        Text("Invited by an agency? Enter its code.", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))

                        Row(Modifier.padding(top = 24.dp, bottom = 24.dp).clickable { agree = !agree }, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier.padding(top = 2.dp).size(20.dp).clip(RoundedCornerShape(6.dp)).background(if (agree) VibeColors.Pink else Color.Transparent)
                                    .border(2.dp, if (agree) VibeColors.Pink else Color(0xFFB093A2), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center,
                            ) { if (agree) Icon(MeIcons.Tick, null, tint = VibeColors.Ink, modifier = Modifier.size(12.dp)) }
                            Text(
                                "I am 18 or older, the photos are mine, and I agree to the VIBE host rules and terms. Nudity, sexual content and asking for money or numbers outside VIBE are not allowed and lead to a ban.",
                                color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 17.sp,
                            )
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton(BUTTONS[step], enabled = canNext && !busy, onClick = { next() })
            }
        }
        else when (app.status) {
            "pending" -> ReviewScreen(agencyLine = app.agencyCode.takeIf { it.isNotBlank() }, onHome = onBack)
            "approved" -> ApprovedScreen(onDashboard = onPreviewDashboard)
            else -> RejectedScreen(app.rejectReason, onApplyAgain = { reapply = true; step = 0 })
        }

        when (live) {
            "intro" -> LiveIntroRecorder(maxVideoSec, onClose = { live = null }, onRecorded = ::introRecorded)
            "selfie" -> LiveSelfieCheck(onClose = { live = null }, onDone = ::selfieTaken)
            "id" -> LiveIdScan(idType, ID_NAMES[idType] ?: "ID", onClose = { live = null }, onDone = ::idTaken)
        }

        if (busy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)).clickable(enabled = true) {}, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
        }
    }
}

/** The form shows while nothing is sent yet, or again after a rejection when the person chose to apply again. */
private fun showForm(app: AppState, reapply: Boolean) = app.status == "none" || (app.status == "rejected" && reapply)

private object OvalShape : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density) =
        androidx.compose.ui.graphics.Outline.Generic(androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)) })
}

@Composable
private fun PhotoSlot(index: Int, media: ServerMedia?, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val full = media != null
    Box(
        modifier.aspectRatio(3f / 4f).clip(shape)
            .then(if (full) Modifier.background(Brush.linearGradient(listOf(Color(0xFFFF6FB0), Color(0xFFD6336F)))) else Modifier.background(VibeColors.Card))
            .border(1.5.dp, if (full) VibeColors.Pink else Color(0xFFFFD6E3), shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (media != null) {
            ServerImage(media.path, Modifier.fillMaxSize(), -0.4f)
            Text("Tap to remove", color = Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 2.dp))
        } else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("+", color = VibeColors.TextMute, fontSize = 26.sp, fontWeight = FontWeight.Light)
            Text("Add photo", color = VibeColors.TextMute, fontSize = 11.sp)
        }
        if (full && index == 0) Box(Modifier.align(Alignment.TopStart).padding(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 7.dp, vertical = 2.dp)) {
            Text("MAIN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
    }
}

@Composable
private fun UploadRow(icon: ImageVector, tint: Color, title: String, status: String, done: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (done) VibeColors.Green.copy(alpha = 0.07f) else VibeColors.Card)
            .border(1.5.dp, if (done) VibeColors.Green.copy(alpha = 0.5f) else VibeColors.Line, shape).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.10f)).border(1.dp, tint.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(status, color = VibeColors.TextMute, fontSize = 12.sp)
        }
        if (done) Icon(MeIcons.Tick, null, tint = VibeColors.Green, modifier = Modifier.size(22.dp)) else Text("›", color = Color(0xFFB093A2), fontSize = 20.sp)
    }
}

@Composable
private fun InfoCard(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(VibeColors.Cyan.copy(alpha = 0.05f)).border(1.dp, VibeColors.Cyan.copy(alpha = 0.30f), shape).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(CallIcons.Shield, null, tint = VibeColors.Cyan, modifier = Modifier.size(20.dp))
        Text(text, color = Color(0xFF5A3F4D), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun RateTile(label: String, value: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Text("$value", color = VibeColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ReviewScreen(agencyLine: String?, onHome: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(VibeColors.Dark).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(112.dp).clip(CircleShape).background(VibeColors.Card).border(2.dp, VibeColors.Gold.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(MeIcons.Hourglass, null, tint = VibeColors.Gold, modifier = Modifier.size(48.dp))
        }
        Text("Under review", color = VibeColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
        Text(
            "Our team checks every host by hand so VIBE stays safe and real. We will notify you within 24 hours. You can keep using VIBE normally.",
            color = VibeColors.TextDim, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        val shape = RoundedCornerShape(16.dp)
        Column(Modifier.padding(top = 24.dp).fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(horizontal = 16.dp)) {
            CheckRow("Photos & intro video", null, done = true)
            CheckRow("Live selfie", null, done = true)
            CheckRow("PAN & ID", null, done = true)
            if (agencyLine != null) CheckRow("Agency", agencyLine, done = true)
            CheckRow("Team review", null, done = false)
        }
        Box(Modifier.padding(top = 24.dp)) { NeonButton("Back to VIBE", onHome) }
    }
}

@Composable
private fun ApprovedScreen(onDashboard: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(VibeColors.Dark).statusBarsPadding().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(112.dp).clip(CircleShape).background(VibeColors.Card).border(2.dp, VibeColors.Green.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(MeIcons.Tick, null, tint = VibeColors.Green, modifier = Modifier.size(48.dp))
        }
        Text("You are a host", color = VibeColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
        Text("Your application is approved. Open the app to receive calls.", color = VibeColors.TextDim, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Box(Modifier.padding(top = 24.dp)) { NeonButton("Open host dashboard", onDashboard) }
    }
}

@Composable
private fun CheckRow(title: String, sub: String?, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (done) VibeColors.Pink else VibeColors.Pink.copy(alpha = 0.15f)).border(1.dp, if (done) Color.Transparent else VibeColors.Pink.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (done) Icon(MeIcons.Tick, null, tint = VibeColors.Ink, modifier = Modifier.size(14.dp)) else Text("•", color = VibeColors.Pink) }
        Column(Modifier.weight(1f)) {
            Text(title, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, color = VibeColors.TextMute, fontSize = 11.sp)
        }
        if (!done) Text("Pending", color = VibeColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RejectedScreen(reason: String?, onApplyAgain: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(VibeColors.Dark).statusBarsPadding().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(112.dp).clip(CircleShape).background(VibeColors.Card).border(2.dp, VibeColors.Red.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(CallIcons.Ban, null, tint = VibeColors.Red, modifier = Modifier.size(48.dp))
        }
        Text("Not approved", color = VibeColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
        Text("Your application could not be approved this time.", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        val shape = RoundedCornerShape(16.dp)
        Row(Modifier.padding(top = 20.dp).fillMaxWidth().clip(shape).background(VibeColors.Red.copy(alpha = 0.05f)).border(1.dp, VibeColors.Red.copy(alpha = 0.3f), shape).padding(16.dp)) {
            Text("Reason: ", color = VibeColors.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(reason ?: "Please check your details and apply again.", color = Color(0xFF5A3F4D), fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
        Box(Modifier.padding(top = 24.dp)) { NeonButton("Apply again", onApplyAgain) }
    }
}
