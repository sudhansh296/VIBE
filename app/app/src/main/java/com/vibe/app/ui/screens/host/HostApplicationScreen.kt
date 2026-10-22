package com.vibe.app.ui.screens.host

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.HostDemo
import com.vibe.app.ui.components.BackButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PillChip
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlin.math.ceil

private val TITLES = listOf("Your profile", "Live selfie", "Verify identity", "Call settings")
private val BUTTONS = listOf("Continue", "Continue", "Continue", "Submit for review")
private val POSES = listOf("Look straight", "Turn left", "Smile")
private val PHOTO_COLORS = listOf(
    VibeColors.Pink to VibeColors.Violet, VibeColors.Cyan to Color(0xFF1E3A8A), Color(0xFFF59E0B) to Color(0xFF9A3412),
)
private const val INTRO_SECONDS = 15
private const val MIN_PHOTOS = 3

private object OvalShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply { addOval(Rect(0f, 0f, size.width, size.height)) })
}

/**
 * Host application: 1 photos + intro video + bio, 2 live selfie, 3 PAN + ID, 4 call settings + optional agency code.
 * DEMO: the selfie and the intro video are simulated here. In the real app CameraX (front camera) shows a live preview, the
 * selfie/video/photos/ID upload to the server, and the server + admin panel decide the review result and rejection reason.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostApplicationScreen(onBack: () -> Unit, onPreviewDashboard: () -> Unit) {
    val ctx = LocalContext.current
    var stage by remember { mutableStateOf("form") }   // form | review | rejected
    var step by remember { mutableStateOf(0) }

    // step 1
    val photos = remember { mutableStateListOf(true, false, false, false, false, false) }
    var video by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }
    val langs = remember { mutableStateListOf("Hindi") }
    // step 2
    var pose by remember { mutableStateOf(-1) }
    var selfie by remember { mutableStateOf(false) }
    // step 3
    var pname by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var idType by remember { mutableStateOf(0) }
    var idUp by remember { mutableStateOf(false) }
    // step 4
    var voiceOn by remember { mutableStateOf(true) }
    var videoOn by remember { mutableStateOf(true) }
    var agency by remember { mutableStateOf("") }
    var agree by remember { mutableStateOf(false) }
    // intro recorder overlay
    var showCam by remember { mutableStateOf(false) }

    val panOk = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$").matches(pan)
    val agencyName = HostDemo.agencies[agency]
    val canNext = when (step) {
        0 -> photos.count { it } >= MIN_PHOTOS && video && bio.trim().length >= 10 && langs.isNotEmpty()
        1 -> selfie
        2 -> pname.trim().length > 3 && panOk && idUp
        else -> agree && (voiceOn || videoOn)
    }

    // selfie check: 3 poses, about 1.2 s each (simulated)
    LaunchedEffect(pose) {
        if (pose in 0..2) {
            delay(1200)
            if (pose == 2) selfie = true
            pose = if (pose == 2) -1 else pose + 1
        }
    }

    BackHandler(enabled = stage == "form" && step > 0 && !showCam) { step-- }
    BackHandler(enabled = showCam) { showCam = false }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))

        if (stage == "form") Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BackButton { if (step > 0) step-- else onBack() }
                Text(TITLES[step], color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(4) { i -> Box(Modifier.height(6.dp).width(if (i <= step) 20.dp else 10.dp).clip(CircleShape).background(if (i <= step) VibeColors.Pink else VibeColors.Line)) }
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                when (step) {
                    0 -> {
                        SectionLabel("Photos (at least $MIN_PHOTOS)", Modifier.padding(top = 8.dp, bottom = 8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (row in 0..1) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (col in 0..2) {
                                    val i = row * 3 + col
                                    PhotoSlot(i, photos[i], Modifier.weight(1f)) { photos[i] = !photos[i] }
                                }
                            }
                        }
                        Text("Clear photos of yourself only. The first photo is your main picture.", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))

                        SectionLabel("Intro video ($INTRO_SECONDS seconds)", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        UploadRow(
                            CallIcons.Camera, VibeColors.Cyan, "Record a $INTRO_SECONDS sec intro",
                            if (video) "Intro recorded · 0:$INTRO_SECONDS" else "Say hi and the languages you speak", video,
                        ) { showCam = true }

                        SectionLabel("Short bio", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        VibeField(bio, { bio = it.take(150) }, "Tell users what you love talking about", singleLine = false, minHeight = 90.dp)
                        Text("${bio.length}/150", color = VibeColors.TextMute, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))

                        SectionLabel("Languages you speak", Modifier.padding(top = 16.dp, bottom = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HostDemo.languages.forEach { l -> PillChip(l, l in langs, { if (l in langs) langs.remove(l) else langs.add(l) }) }
                        }
                        Box(Modifier.height(24.dp))
                    }

                    1 -> {
                        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            SelfieOval(pose, selfie)
                            Text(
                                if (selfie) "Selfie verified" else if (pose >= 0) POSES[pose] + "…" else "Keep your face inside the oval",
                                color = if (selfie) VibeColors.Green else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp),
                            )
                            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                POSES.forEachIndexed { i, p ->
                                    val done = selfie || i < pose
                                    val on = i == pose
                                    Box(
                                        Modifier.height(34.dp).clip(CircleShape).border(1.5.dp, if (done) VibeColors.Green.copy(alpha = 0.6f) else if (on) VibeColors.Pink else VibeColors.Line, CircleShape)
                                            .background(VibeColors.Card).padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { Text((if (done) "✓" else "${i + 1}") + " " + p, color = if (done) VibeColors.Green else if (on) Color.White else Color(0xFF8B8B9A), fontSize = 12.sp) }
                                }
                            }
                            if (!selfie && pose < 0) Box(
                                Modifier.padding(top = 24.dp).height(48.dp).clip(CircleShape).background(VibeBrush.Button).clickable { pose = 0 }.padding(horizontal = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("Start selfie check", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        }
                        val iShape = RoundedCornerShape(16.dp)
                        Row(
                            Modifier.padding(top = 24.dp).fillMaxWidth().clip(iShape).background(VibeColors.Card).border(1.dp, Color.White.copy(alpha = 0.10f), iShape).padding(16.dp),
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
                        Row(
                            Modifier.fillMaxWidth().clip(fShape).background(VibeColors.Card).border(1.5.dp, VibeColors.Line, fShape).padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("14 Mar 2002 · from your profile", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Box(Modifier.clip(CircleShape).background(VibeColors.Green.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("24 yrs ✓", color = VibeColors.Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        SectionLabel("Full name (as on PAN)", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        VibeField(pname, { pname = it }, "Your full name")
                        SectionLabel("PAN number", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        VibeField(pan, { pan = it.uppercase().take(10) }, "ABCDE1234F", caps = true)
                        Text(
                            if (pan.isEmpty()) "" else if (panOk) "PAN format looks right" else "PAN is 5 letters, 4 digits, 1 letter",
                            color = if (panOk) VibeColors.Green else VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
                        )
                        SectionLabel("ID proof", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Aadhaar", "PAN", "Passport").forEachIndexed { i, t -> PillChip(t, i == idType, { idType = i }, Modifier.weight(1f)) }
                        }
                        Box(Modifier.padding(top = 12.dp, bottom = 24.dp)) {
                            UploadRow(MeIcons.IdCard, VibeColors.Gold, "Upload ID photo", if (idUp) "ID uploaded" else "Front side, all details visible", idUp) { idUp = true }
                        }
                    }

                    else -> {
                        SectionLabel("Accept", Modifier.padding(top = 8.dp, bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BigChip("Voice calls", voiceOn, Modifier.weight(1f)) { if (voiceOn && !videoOn) ctx.toast("Keep at least one call type on") else voiceOn = !voiceOn }
                            BigChip("Video calls", videoOn, Modifier.weight(1f)) { if (videoOn && !voiceOn) ctx.toast("Keep at least one call type on") else videoOn = !videoOn }
                        }
                        SectionLabel("Starting rates (coins per minute)", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RateTile("Voice", HostDemo.voice.start, Modifier.weight(1f))
                            RateTile("Video", HostDemo.video.start, Modifier.weight(1f))
                        }
                        Text("VIBE sets your first rates. Higher rates unlock as your host level grows.", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))

                        SectionLabel("Agency code (optional)", Modifier.padding(top = 20.dp, bottom = 8.dp))
                        val chip: @Composable () -> Unit = { Text("✓ $agencyName", color = VibeColors.Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        VibeField(agency, { agency = it.uppercase().take(10) }, "e.g. AG1045", caps = true, trailing = if (agencyName != null) chip else null)
                        Text(
                            if (agency.isEmpty()) "Invited by an agency? Enter its code. You can also join or leave one later." else if (agencyName != null) "Code verified" else "Checking the code…",
                            color = if (agencyName != null) VibeColors.Green else VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
                        )

                        Row(Modifier.padding(top = 24.dp, bottom = 24.dp).clickable { agree = !agree }, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier.padding(top = 2.dp).size(20.dp).clip(RoundedCornerShape(6.dp)).background(if (agree) VibeColors.Pink else Color.Transparent)
                                    .border(2.dp, if (agree) VibeColors.Pink else Color(0xFF4B5563), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center,
                            ) { if (agree) Icon(MeIcons.Tick, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                            Text(
                                "I am 18 or older, the photos are mine, and I agree to the VIBE host rules and terms. Nudity, sexual content and asking for money or numbers outside VIBE are not allowed and lead to a ban.",
                                color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 17.sp,
                            )
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton(BUTTONS[step], enabled = canNext, onClick = { if (step < 3) step++ else stage = "review" })
            }
        }

        if (stage == "review") ReviewScreen(
            agencyLine = agencyName?.let { "$it · $agency" }, onPreviewDashboard = onPreviewDashboard, onPreviewRejected = { stage = "rejected" },
        )
        if (stage == "rejected") RejectedScreen(onApplyAgain = { stage = "form"; step = 0 })

        if (showCam) IntroRecorder(
            onClose = { showCam = false },
            onDone = { video = true; showCam = false; ctx.toast("Intro video saved") },
        )
    }
}

@Composable
private fun PhotoSlot(index: Int, full: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val (a, b) = PHOTO_COLORS[index % 3]
    Box(
        modifier.aspectRatio(3f / 4f).clip(shape)
            .then(if (full) Modifier.background(Brush.linearGradient(listOf(a, b))) else Modifier.background(VibeColors.Card))
            .border(1.5.dp, if (full) VibeColors.Pink else Color(0xFF3A3A4C), shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (full) Icon(CallIcons.User, null, tint = Color.White, modifier = Modifier.size(26.dp))
            else Text("+", color = VibeColors.TextMute, fontSize = 26.sp, fontWeight = FontWeight.Light)
            Text(if (full) "Tap to remove" else "Add photo", color = if (full) Color.White else VibeColors.TextMute, fontSize = 11.sp)
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
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(status, color = VibeColors.TextMute, fontSize = 12.sp)
        }
        if (done) Icon(MeIcons.Tick, null, tint = VibeColors.Green, modifier = Modifier.size(22.dp)) else Text("›", color = Color(0xFF4B5563), fontSize = 20.sp)
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
        Text(text, color = Color(0xFFD1D5DB), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun BigChip(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.height(56.dp).clip(shape).background(if (on) VibeBrush.ChipOn else Brush.linearGradient(listOf(VibeColors.Card, VibeColors.Card)))
            .border(1.5.dp, if (on) VibeColors.Pink else VibeColors.Line, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (on) Color.White else VibeColors.ChipText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun RateTile(label: String, value: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = VibeColors.TextMute, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Text("$value", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Oval selfie frame (simulated). Real app: CameraX front-camera preview is drawn inside the oval. */
@Composable
private fun SelfieOval(pose: Int, done: Boolean) {
    val scanning = pose >= 0 && !done
    val scan by rememberInfiniteTransition(label = "scan").animateFloat(0.06f, 0.94f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "y")
    val color = if (done) VibeColors.Green else if (scanning) VibeColors.Pink else Color.White.copy(alpha = 0.75f)
    Box(
        Modifier.size(190.dp, 250.dp).clip(OvalShape).background(Brush.radialGradient(listOf(Color(0xFF5B2A44), Color(0xFF1A0D16))))
            .border(3.dp, color, OvalShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(CallIcons.User, null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(96.dp))
        if (scanning) Box(
            Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopStart).offset(y = 250.dp * scan)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, VibeColors.Cyan, Color.Transparent))),
        )
    }
}

/** Full-screen intro recorder (simulated): tap the red button, a ring fills for 15 seconds and recording stops by itself. */
@Composable
private fun IntroRecorder(onClose: () -> Unit, onDone: () -> Unit) {
    var recording by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(recording) {
        if (recording) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(INTRO_SECONDS * 1000, easing = LinearEasing))
            onDone()
        }
    }
    val dot by animateDpAsState(if (recording) 26.dp else 56.dp, label = "dot")
    val left = ceil(INTRO_SECONDS * (1f - progress.value)).toInt().coerceAtLeast(0)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A0D16), Color.Black))).clickable(enabled = false) {}) {
        Icon(CallIcons.User, null, tint = Color.White.copy(alpha = 0.12f), modifier = Modifier.size(180.dp).align(Alignment.Center))
        Box(
            Modifier.statusBarsPadding().padding(start = 16.dp, top = 12.dp).size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable { recording = false; onClose() },
            contentAlignment = Alignment.Center,
        ) { Icon(MeIcons.Close, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Text(
            "Say hi and the languages you speak", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 20.dp, start = 64.dp, end = 64.dp),
        )
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("0:%02d".format(left), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Box(Modifier.size(92.dp).clickable(enabled = !recording) { recording = true }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(80.dp).border(4.dp, Color.White, CircleShape))
                Box(Modifier.size(dot).clip(if (recording) RoundedCornerShape(8.dp) else CircleShape).background(VibeColors.Red))
                Canvas(Modifier.size(92.dp)) {
                    val sw = 4.dp.toPx()
                    drawArc(
                        VibeColors.Pink, -90f, 360f * progress.value, false, Offset(sw / 2, sw / 2),
                        Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round),
                    )
                }
            }
            Text("Tap to record · stops by itself after $INTRO_SECONDS seconds", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ReviewScreen(agencyLine: String?, onPreviewDashboard: () -> Unit, onPreviewRejected: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(VibeColors.Dark).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(112.dp).clip(CircleShape).background(VibeColors.Card).border(2.dp, VibeColors.Gold.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(MeIcons.Hourglass, null, tint = VibeColors.Gold, modifier = Modifier.size(48.dp))
        }
        Text("Under review", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
        Text(
            "Our team checks every host by hand so VIBE stays safe and real. We will notify you within 24 hours. You can keep using VIBE normally.",
            color = VibeColors.TextDim, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        val shape = RoundedCornerShape(16.dp)
        Column(Modifier.padding(top = 24.dp).fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(horizontal = 16.dp)) {
            val rows = buildList<Pair<String, String?>> {
                add("Photos & intro video" to null)
                add("Live selfie" to null)
                add("PAN & ID" to null)
                if (agencyLine != null) add("Agency" to agencyLine)
            }
            rows.forEach { (t, s) -> CheckRow(t, s, done = true) }
            CheckRow("Team review", null, done = false)
        }
        Box(Modifier.padding(top = 24.dp)) { NeonButton("Preview host dashboard", onPreviewDashboard) }
        Text(
            "Preview the \"not approved\" screen", color = VibeColors.TextMute, fontSize = 11.sp,
            modifier = Modifier.padding(top = 16.dp).clickable(onClick = onPreviewRejected),
        )
    }
}

@Composable
private fun CheckRow(title: String, sub: String?, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (done) VibeColors.Pink else VibeColors.Pink.copy(alpha = 0.15f)).border(1.dp, if (done) Color.Transparent else VibeColors.Pink.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (done) Icon(MeIcons.Tick, null, tint = Color.White, modifier = Modifier.size(14.dp)) else Text("•", color = VibeColors.Pink) }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, color = VibeColors.TextMute, fontSize = 11.sp)
        }
        if (!done) Text("Pending", color = VibeColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RejectedScreen(onApplyAgain: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(VibeColors.Dark).statusBarsPadding().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(112.dp).clip(CircleShape).background(VibeColors.Card).border(2.dp, VibeColors.Red.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(CallIcons.Ban, null, tint = VibeColors.Red, modifier = Modifier.size(48.dp))
        }
        Text("Not approved", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))
        Text("Your application could not be approved this time.", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        val shape = RoundedCornerShape(16.dp)
        Row(Modifier.padding(top = 20.dp).fillMaxWidth().clip(shape).background(VibeColors.Red.copy(alpha = 0.05f)).border(1.dp, VibeColors.Red.copy(alpha = 0.3f), shape).padding(16.dp)) {
            // reason text comes from the admin review
            Text("Reason: ", color = VibeColors.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Photos are not clear. Please upload 3 clear photos of your own face.", color = Color(0xFFD1D5DB), fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
        Box(Modifier.padding(top = 24.dp)) { NeonButton("Apply again", onApplyAgain) }
    }
}
