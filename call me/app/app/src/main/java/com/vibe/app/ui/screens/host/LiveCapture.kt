package com.vibe.app.ui.screens.host

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.theme.VibeColors
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ceil

// Live camera steps of the host application: everything is recorded inside the app (no gallery), so the intro video,
// the selfie and the ID photo are really taken now. The server still checks every file; the admin compares them by hand.

private fun granted(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

/** Asks for the camera (and microphone) once; shows [content] only when allowed. */
@Composable
private fun WithCameraPermission(audio: Boolean, onClose: () -> Unit, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val perms = if (audio) arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO) else arrayOf(Manifest.permission.CAMERA)
    var ok by remember { mutableStateOf(perms.all { granted(ctx, it) }) }
    var asked by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> ok = perms.all { r[it] == true || granted(ctx, it) }; asked = true }
    LaunchedEffect(Unit) { if (!ok) ask.launch(perms) }
    if (ok) content()
    else Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                if (audio) "VIBE needs the camera and microphone to record your intro video." else "VIBE needs the camera for this step.",
                color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center,
            )
            if (asked) NeonButton("Allow camera", { ask.launch(perms) })
            Text("Close", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape).clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) { Icon(MeIcons.Close, "Close", tint = Color.White, modifier = Modifier.size(20.dp)) }
}

// ---------------------------------------------------------------- 1. intro video (front camera, stops by itself)

/** Records the intro video with the front camera. Stops by itself just before [maxSec] (the server's limit). */
@SuppressLint("MissingPermission")
@Composable
fun LiveIntroRecorder(maxSec: Int, onClose: () -> Unit, onRecorded: (File) -> Unit) {
    BackHandler(onBack = onClose)
    WithCameraPermission(audio = true, onClose = onClose) {
        val ctx = LocalContext.current
        val owner = LocalLifecycleOwner.current
        val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
        val capture = remember { VideoCapture.withOutput(Recorder.Builder().setQualitySelector(QualitySelector.fromOrderedList(listOf(Quality.SD, Quality.HD))  /* SD keeps 30 s well under the upload limit */).build()) }
        var recording by remember { mutableStateOf<Recording?>(null) }
        val progress = remember { Animatable(0f) }
        val limitMs = (maxSec * 1000L - 700L).coerceAtLeast(3000L) // the server rounds up: stay safely under the limit
        val file = remember { File(ctx.cacheDir, "intro_${System.currentTimeMillis()}.mp4") }

        DisposableEffect(Unit) {
            val f = ProcessCameraProvider.getInstance(ctx)
            f.addListener({
                val p = f.get()
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }, capture)
            }, ContextCompat.getMainExecutor(ctx))
            onDispose { recording?.stop(); runCatching { f.get().unbindAll() } }
        }
        LaunchedEffect(recording) {
            if (recording != null) { progress.snapTo(0f); progress.animateTo(1f, tween(limitMs.toInt(), easing = LinearEasing)) }
        }

        fun start() {
            file.delete()
            val opts = FileOutputOptions.Builder(file).setDurationLimitMillis(limitMs).build()
            recording = capture.output.prepareRecording(ctx, opts).withAudioEnabled().start(ContextCompat.getMainExecutor(ctx)) { e ->
                if (e is VideoRecordEvent.Finalize) {
                    recording = null
                    val ok = !e.hasError() || e.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
                    if (ok && file.length() > 0) onRecorded(file)
                }
            }
        }

        val rec = recording != null
        val dot by animateDpAsState(if (rec) 26.dp else 56.dp, label = "dot")
        val left = ceil(limitMs / 1000f * (1f - progress.value)).toInt().coerceAtLeast(0)
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView({ previewView }, Modifier.fillMaxSize())
            CloseButton(onClose, Modifier.statusBarsPadding().padding(start = 16.dp, top = 12.dp))
            Text(
                "Say hi, your name and the languages you speak", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 20.dp, start = 64.dp, end = 64.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (rec) "0:%02d left".format(left) else "Up to $maxSec seconds", color = Color.White, fontSize = 14.sp)
                Box(Modifier.size(92.dp).clickable { if (rec) recording?.stop() else start() }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(80.dp).border(4.dp, Color.White, CircleShape))
                    Box(Modifier.size(dot).clip(if (rec) RoundedCornerShape(8.dp) else CircleShape).background(VibeColors.Red))
                    Canvas(Modifier.size(92.dp)) {
                        val sw = 4.dp.toPx()
                        drawArc(VibeColors.Pink, -90f, 360f * progress.value, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
                    }
                }
                Text(if (rec) "Tap to stop" else "Tap to record", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

// ---------------------------------------------------------------- 2. selfie with a live face check

private val SELFIE_STEPS = listOf("Look straight at the camera", "Now turn your head to one side", "Now smile")

/**
 * Live selfie: the front camera checks a real face in three moves (straight, turn, smile) with on-phone face detection,
 * then takes the photo by itself. Nothing leaves the phone until the photo is taken.
 */
@Composable
fun LiveSelfieCheck(onClose: () -> Unit, onDone: (File) -> Unit) {
    BackHandler(onBack = onClose)
    WithCameraPermission(audio = false, onClose = onClose) {
        val ctx = LocalContext.current
        val owner = LocalLifecycleOwner.current
        val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
        val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
        var step by remember { mutableIntStateOf(0) }
        var hint by remember { mutableStateOf("") }
        var taking by remember { mutableStateOf(false) }
        val analysisExec = remember { Executors.newSingleThreadExecutor() }
        val detector = remember {
            FaceDetection.getClient(
                FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).setMinFaceSize(0.25f).build(),
            )
        }

        fun takePhoto() {
            if (taking) return
            taking = true
            val f = File(ctx.cacheDir, "selfie_${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(f).build(), ContextCompat.getMainExecutor(ctx), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(r: ImageCapture.OutputFileResults) = onDone(f)
                override fun onError(e: ImageCaptureException) { taking = false; step = 0; hint = "Could not take the photo, try again" }
            })
        }

        DisposableEffect(Unit) {
            var heldSince = 0L
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(analysisExec) { proxy ->
                val media = proxy.image
                if (media == null || taking) { proxy.close(); return@setAnalyzer }
                detector.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener { faces ->
                        val face = faces.singleOrNull()
                        val yaw = face?.headEulerAngleY ?: 0f
                        val pass = when {
                            faces.size > 1 -> { hint = "Only you in the picture please"; false }
                            face == null -> { hint = "Keep your face inside the oval"; false }
                            step == 0 -> abs(yaw) < 10f && abs(face.headEulerAngleX) < 15f && (face.leftEyeOpenProbability ?: 1f) > 0.4f
                            step == 1 -> abs(yaw) > 25f
                            else -> (face.smilingProbability ?: 0f) > 0.7f
                        }
                        if (face != null && faces.size == 1) hint = ""
                        val now = System.currentTimeMillis()
                        if (pass) {
                            if (heldSince == 0L) heldSince = now
                            if (now - heldSince > 500) { heldSince = 0L; if (step < 2) step++ else takePhoto() }
                        } else heldSince = 0L
                    }
                    .addOnCompleteListener { proxy.close() }
            }
            val f = ProcessCameraProvider.getInstance(ctx)
            f.addListener({
                val p = f.get()
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }, imageCapture, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            onDispose { runCatching { f.get().unbindAll() }; detector.close(); analysisExec.shutdown() }
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView({ previewView }, Modifier.fillMaxSize())
            // dark mask with an oval window for the face
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width * 0.66f; val h = w * 1.3f
                val tl = Offset((size.width - w) / 2, size.height * 0.42f - h / 2)
                val oval = androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(tl, Size(w, h))) }
                val all = androidx.compose.ui.graphics.Path().apply { addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size)) }
                drawPath(androidx.compose.ui.graphics.Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, all, oval), Color.Black.copy(alpha = 0.55f))
                drawPath(oval, if (step > 0) VibeColors.Green else Color.White, style = Stroke(4.dp.toPx()))
            }
            CloseButton(onClose, Modifier.statusBarsPadding().padding(start = 16.dp, top = 12.dp))
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (taking) "Hold still…" else SELFIE_STEPS[step], color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                if (hint.isNotEmpty()) Text(hint, color = Color(0xFFFFC2DD), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SELFIE_STEPS.indices.forEach { i ->
                        Box(Modifier.height(6.dp).size(width = if (i == step) 28.dp else 14.dp, height = 6.dp).clip(CircleShape).background(if (i < step || taking) VibeColors.Green else if (i == step) Color.White else Color.White.copy(alpha = 0.35f)))
                    }
                }
                Text("Only the VIBE team sees this selfie", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
    }
}

// ---------------------------------------------------------------- 3. ID photo with the number read on the phone

/** Finds the number of the chosen document in text read from the card. Aadhaar is also checked with its checksum. */
fun findIdNumber(type: String, text: String): String? {
    val t = text.uppercase().replace('O', '0').let { if (type == "aadhaar") it else text.uppercase() }
    return when (type) {
        "aadhaar" -> Regex("""(?<!\d)([2-9]\d{3}) ?(\d{4}) ?(\d{4})(?!\d)""") /* spaces only: never join two lines */.findAll(t).map { it.groupValues.drop(1).joinToString("") }.firstOrNull { verhoeff(it) }
            ?.let { "${it.substring(0, 4)} ${it.substring(4, 8)} ${it.substring(8)}" }
        "voter_id" -> Regex("""\b[A-Z]{3}\d{7}\b""").find(t)?.value
        "passport" -> Regex("""\b[A-Z]\d{7}\b""").find(t)?.value
        "driving_licence" -> Regex("""\b[A-Z]{2}[ -]?\d{2}[ -]?\d{4}[ -]?\d{7}\b""").find(t)?.value?.replace(Regex("[ -]"), "")
        else -> null
    }
}

private val VD = arrayOf(
    intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5), intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6), intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7),
    intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8), intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1), intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2), intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3),
    intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4), intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0),
)
private val VP = arrayOf(
    intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4), intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2), intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7),
    intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0), intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1), intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5), intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8),
)
private fun verhoeff(n: String): Boolean {
    var c = 0
    n.reversed().forEachIndexed { i, ch -> c = VD[c][VP[i % 8][ch - '0']] }
    return c == 0
}

/**
 * Live ID photo: the back camera reads the card while it is held in the frame and picks out the document number.
 * The person takes the photo, checks (or types) the number and then both are sent; the server checks the number's format again.
 */
@Composable
fun LiveIdScan(idType: String, idName: String, onClose: () -> Unit, onDone: (File, String) -> Unit) {
    BackHandler(onBack = onClose)
    WithCameraPermission(audio = false, onClose = onClose) {
        val ctx = LocalContext.current
        val owner = LocalLifecycleOwner.current
        val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
        val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
        var found by remember { mutableStateOf<String?>(null) }
        var photo by remember { mutableStateOf<File?>(null) }
        var number by remember { mutableStateOf("") }
        var taking by remember { mutableStateOf(false) }
        val analysisExec = remember { Executors.newSingleThreadExecutor() }
        val reader = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

        if (photo == null) DisposableEffect(Unit) {
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(analysisExec) { proxy ->
                val media = proxy.image
                if (media == null) { proxy.close(); return@setAnalyzer }
                reader.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener { r -> findIdNumber(idType, r.text)?.let { found = it } }
                    .addOnCompleteListener { proxy.close() }
            }
            val f = ProcessCameraProvider.getInstance(ctx)
            f.addListener({
                val p = f.get()
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }, imageCapture, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            onDispose { runCatching { f.get().unbindAll() } }
        }
        DisposableEffect(Unit) { onDispose { reader.close(); analysisExec.shutdown() } }

        fun capture() {
            if (taking) return
            taking = true
            val f = File(ctx.cacheDir, "id_${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(f).build(), ContextCompat.getMainExecutor(ctx), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(r: ImageCapture.OutputFileResults) { taking = false; number = found.orEmpty(); photo = f }
                override fun onError(e: ImageCaptureException) { taking = false }
            })
        }

        val shot = photo
        if (shot == null) Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView({ previewView }, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width * 0.88f; val h = w / 1.586f   // ID-1 card shape
                val tl = Offset((size.width - w) / 2, size.height * 0.40f - h / 2)
                val rr = androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(tl, Size(w, h)), androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
                val card = androidx.compose.ui.graphics.Path().apply { addRoundRect(rr) }
                val all = androidx.compose.ui.graphics.Path().apply { addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size)) }
                drawPath(androidx.compose.ui.graphics.Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, all, card), Color.Black.copy(alpha = 0.55f))
                drawPath(card, if (found != null) VibeColors.Green else Color.White, style = Stroke(3.dp.toPx()))
            }
            CloseButton(onClose, Modifier.statusBarsPadding().padding(start = 16.dp, top = 12.dp))
            Text(
                "Hold the front of your $idName inside the frame", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp, start = 32.dp, end = 32.dp),
            )
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    found?.let { "Number found: $it" } ?: "Reading the card…", color = if (found != null) Color(0xFF7CFFB2) else Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
                Box(Modifier.size(78.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape).padding(8.dp).clip(CircleShape).background(Color.White).clickable { capture() })
                Text("Make sure all details are clear, then take the photo", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        } else Column(Modifier.fillMaxSize().background(VibeColors.Dark).statusBarsPadding().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Check your $idName", color = VibeColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                CloseButton(onClose)
            }
            com.vibe.app.ui.components.UriImage(android.net.Uri.fromFile(shot), Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)))
            Text("$idName number", color = VibeColors.TextDim, fontSize = 13.sp)
            VibeField(number, { number = it.uppercase().take(20) }, if (idType == "aadhaar") "1234 5678 9012" else "Number on the card", caps = true)
            Text(
                if (found != null) "Read from your card. Fix it if anything is wrong." + (if (idType == "aadhaar") " Only the last 4 digits are saved." else "")
                else "We could not read the number. Please type it." + (if (idType == "aadhaar") " Only the last 4 digits are saved." else ""),
                color = VibeColors.TextMute, fontSize = 12.sp,
            )
            Box(Modifier.weight(1f))
            NeonButton("Use this photo", { onDone(shot, number.trim()) }, enabled = number.trim().length >= 4)
            Text("Retake", color = VibeColors.Pink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally).clickable { photo = null; found = null }.padding(8.dp))
        }
    }
}
