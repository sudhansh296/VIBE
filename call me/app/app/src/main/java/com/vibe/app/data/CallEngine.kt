package com.vibe.app.data

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vibe.app.data.api.Api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStreamTrack
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSink
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * The video call itself (WebRTC). Camera and microphone on this phone, one peer connection to the other phone.
 * The two phones exchange offer / answer / network candidates through our server (Socket.IO "rtc:signal"); the video and
 * voice then go directly between the phones (or through our TURN relay when a network blocks that). Coins and the clock are
 * on the server, never here.
 *
 * Who starts: the user's phone makes the offer, the host's phone answers.
 * Fast start: the connection is built WHILE IT RINGS (prepare) with empty audio / video slots, so nothing of either person
 * is sent before pick-up and the receiver's camera stays off. At pick-up (goLive) each phone just puts its camera and
 * microphone into those slots; no second offer / answer is needed, so the picture appears almost at once.
 */
object CallEngine {
    private var appCtx: Context? = null
    val egl: EglBase by lazy { EglBase.create() }
    private var factory: PeerConnectionFactory? = null
    private var pc: PeerConnection? = null
    private var capturer: CameraVideoCapturer? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    /** My camera's video (state, so a video view made before the camera started still picks it up). */
    var localVideo by mutableStateOf<VideoTrack?>(null)
        private set
    private var localAudio: AudioTrack? = null
    private val remoteSinks = mutableListOf<VideoSink>()
    private var pendingCandidates = mutableListOf<IceCandidate>()
    // offer / answer / ICE that arrived before the connection existed (the other phone was quicker)
    private var pendingSignals = mutableListOf<JSONObject>()
    // media may flow (picked up); asked for before the connection existed = attach as soon as it does
    private var live = false
    // ICE servers from our server, reused for a few minutes (TURN passwords are short-lived)
    private var iceCache: Pair<List<PeerConnection.IceServer>, Long>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** The call this engine is connected for, and whether this phone makes the offer. */
    var callId: String? = null
        private set
    private var caller = false

    // what the call screen shows
    var remoteVideo by mutableStateOf<VideoTrack?>(null)
        private set
    var connected by mutableStateOf(false)
        private set
    /** The first picture from the other phone has arrived (until then the call screen shows "Connecting video…"). */
    var remoteShowing by mutableStateOf(false)
        private set
    var micOn by mutableStateOf(true)
        private set
    var cameraOn by mutableStateOf(true)
        private set
    var frontCamera by mutableStateOf(true)
        private set
    var speakerOn by mutableStateOf(true)
        private set

    fun init(ctx: Context) {
        if (factory != null) return
        appCtx = ctx.applicationContext
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(appCtx).createInitializationOptions())
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
            .createPeerConnectionFactory()
    }

    /** Front camera + microphone on (also used as the preview while it rings). */
    fun startLocal(ctx: Context) {
        init(ctx)
        if (localVideo != null) return
        val f = factory ?: return
        val enumerator = Camera2Enumerator(ctx)
        val name = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) } ?: enumerator.deviceNames.firstOrNull() ?: return
        frontCamera = enumerator.isFrontFacing(name)
        capturer = enumerator.createCapturer(name, null)
        surfaceHelper = SurfaceTextureHelper.create("vibe-capture", egl.eglBaseContext)
        videoSource = f.createVideoSource(false)
        capturer?.initialize(surfaceHelper, ctx, videoSource!!.capturerObserver)
        capturer?.startCapture(1280, 720, 30)
        localVideo = f.createVideoTrack("vibe-video", videoSource)
        audioSource = f.createAudioSource(MediaConstraints())
        localAudio = f.createAudioTrack("vibe-audio", audioSource)
        micOn = true; cameraOn = true
        setSpeaker(true)
    }

    /**
     * While it rings: build the connection for [id] with empty audio / video slots (nothing is sent, no camera needed).
     * [isCaller] = this phone makes the offer (the user's side). Safe to call again for the same call.
     */
    fun prepare(id: String, isCaller: Boolean) {
        if (callId == id) return
        val ctx = appCtx ?: return
        init(ctx)
        // a connection left from an earlier call must never keep running next to this one
        pc?.let { old -> runCatching { old.close() }; runCatching { old.dispose() } }; pc = null
        pendingCandidates = mutableListOf(); pendingSignals = mutableListOf(); remoteVideo = null
        callId = id; caller = isCaller; connected = false; remoteShowing = false; live = false
        scope.launch {
            val servers = iceServers()
            if (callId != id) return@launch                      // ended / replaced meanwhile
            createPeer(servers)
            if (caller) makeOffer(false)
            pendingSignals.toList().also { pendingSignals = mutableListOf() }.forEach { onSignal(id, it) }
            if (live) attachLocal()
        }
    }

    /** Picked up: camera + microphone on and into the connection. [isCaller] = this phone makes the offer. */
    fun connect(ctx: Context, id: String, isCaller: Boolean) {
        init(ctx)
        prepare(id, isCaller)                                      // not prepared while ringing (e.g. app reopened)
        startLocal(ctx)
        live = true
        attachLocal()
    }

    /** Puts my camera + microphone into the connection's slots (no new offer needed). */
    private fun attachLocal() {
        val p = pc ?: return                                       // not built yet: done when it is (see prepare)
        p.setAudioRecording(true); p.setAudioPlayout(true)         // picked up: microphone + speaker on
        p.transceivers.forEach { t ->
            when (t.mediaType) {
                MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO -> if (t.sender.track() == null) localVideo?.let { t.sender.setTrack(it, false) }
                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO -> if (t.sender.track() == null) localAudio?.let { t.sender.setTrack(it, false) }
                else -> {}
            }
        }
    }

    private suspend fun iceServers(): List<PeerConnection.IceServer> {
        iceCache?.let { (list, at) -> if (System.currentTimeMillis() - at < 5 * 60_000) return list }
        val ice = runCatching { Api.get("/calls/ice").getJSONArray("iceServers") }.getOrNull()
        val servers = mutableListOf<PeerConnection.IceServer>()
        if (ice != null) for (i in 0 until ice.length()) {
            val o = ice.getJSONObject(i)
            val urls = o.getJSONArray("urls").let { a -> List(a.length()) { a.getString(it) } }
            val b = PeerConnection.IceServer.builder(urls)
            if (o.has("username")) b.setUsername(o.getString("username")).setPassword(o.getString("credential"))
            servers.add(b.createIceServer())
        }
        if (servers.isEmpty()) servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        else iceCache = servers to System.currentTimeMillis()
        return servers
    }

    private fun createPeer(servers: List<PeerConnection.IceServer>) {
        val f = factory ?: return
        val config = PeerConnection.RTCConfiguration(servers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        pc = f.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(c: IceCandidate) {
                signal(JSONObject().put("type", "ice").put("candidate", c.sdp).put("sdpMid", c.sdpMid).put("sdpMLineIndex", c.sdpMLineIndex))
            }
            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                scope.launch {
                    connected = state == PeerConnection.PeerConnectionState.CONNECTED
                    // the network changed / broke: the caller asks for a new route (ICE restart)
                    if (state == PeerConnection.PeerConnectionState.FAILED && caller) makeOffer(true)
                }
            }
            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track()
                if (track is VideoTrack) scope.launch {
                    remoteVideo = track; remoteSinks.forEach { track.addSink(it) }
                    // nothing is sent before pick-up, so the first frame is the real "video is on" moment
                    // (never remove a sink inside onFrame: WebRTC holds its lock there, so that would freeze the video)
                    val first = object : VideoSink {
                        @Volatile var seen = false
                        override fun onFrame(f: org.webrtc.VideoFrame) {
                            if (seen) return
                            seen = true
                            val me = this
                            scope.launch { remoteShowing = true; runCatching { track.removeSink(me) } }
                        }
                    }
                    track.addSink(first)
                }
            }
            override fun onSignalingChange(s: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState) {}
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>) {}
            override fun onAddStream(s: MediaStream) {}
            override fun onRemoveStream(s: MediaStream) {}
            override fun onDataChannel(d: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(r: RtpReceiver, s: Array<out MediaStream>) {}
        })
        // the phone's microphone and speaker stay OFF until pick-up (WebRTC would start them as soon as it connects,
        // so the room could be heard while it still rings)
        pc?.setAudioRecording(live); pc?.setAudioPlayout(live)
        if (caller) {
            val init = RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV, listOf("vibe"))
            pc?.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO, init)
            pc?.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO, init)
        }
    }

    private fun constraints() = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }

    private fun makeOffer(restart: Boolean) {
        val p = pc ?: return
        val c = constraints().apply { if (restart) mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true")) }
        p.createOffer(created { offer ->
            p.setLocalDescription(setDone {}, offer)
            signal(JSONObject().put("type", "offer").put("sdp", offer.description))
        }, c)
    }

    /** Something from the other phone (via the server). */
    fun onSignal(forCall: String, data: JSONObject) {
        if (forCall != callId) return
        val p = pc ?: run { pendingSignals.add(data); return }     // keep it until the connection exists
        when (data.optString("type")) {
            "offer" -> {
                p.setRemoteDescription(setDone {
                    flushCandidates()
                    // answer with send + receive slots, so my camera can go in at pick-up without another offer
                    p.transceivers.forEach { t -> t.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV }
                    if (live) attachLocal()
                    p.createAnswer(created { answer ->
                        p.setLocalDescription(setDone {}, answer)
                        signal(JSONObject().put("type", "answer").put("sdp", answer.description))
                    }, constraints())
                }, SessionDescription(SessionDescription.Type.OFFER, data.getString("sdp")))
            }
            "answer" -> { p.setRemoteDescription(setDone { flushCandidates() }, SessionDescription(SessionDescription.Type.ANSWER, data.getString("sdp"))) }
            "ice" -> {
                val c = IceCandidate(data.optString("sdpMid"), data.optInt("sdpMLineIndex"), data.getString("candidate"))
                if (p.remoteDescription == null) pendingCandidates.add(c) else p.addIceCandidate(c)
            }
        }
    }

    /** The other phone came back after losing its connection: the caller offers a fresh route. */
    fun onPeerBack(forCall: String) { if (forCall == callId && caller) scope.launch { makeOffer(true) } }

    private fun flushCandidates() {
        val p = pc ?: return
        pendingCandidates.forEach { p.addIceCandidate(it) }
        pendingCandidates = mutableListOf()
    }

    private fun signal(data: JSONObject) { callId?.let { Realtime.rtcSignal(it, data) } }

    // ---- what the call buttons do
    fun setMic(on: Boolean) { localAudio?.setEnabled(on); micOn = on }

    fun setCamera(on: Boolean) {
        localVideo?.setEnabled(on)
        runCatching { if (on) capturer?.startCapture(1280, 720, 30) else capturer?.stopCapture() }
        cameraOn = on
    }

    fun flipCamera() {
        capturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) { scope.launch { frontCamera = isFront } }
            override fun onCameraSwitchError(error: String?) {}
        })
    }

    fun setSpeaker(on: Boolean) {
        val am = appCtx?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = on
        speakerOn = on
    }

    // ---- showing the video
    fun addRemoteSink(s: VideoSink) { remoteSinks.add(s); remoteVideo?.addSink(s) }
    fun removeRemoteSink(s: VideoSink) { remoteSinks.remove(s); remoteVideo?.removeSink(s) }

    /** Everything off: camera, microphone, connection. Safe to call more than once. */
    fun stop() {
        appCtx?.let { CallService.stop(it) }
        runCatching { pc?.close() }; runCatching { pc?.dispose() }; pc = null
        runCatching { capturer?.stopCapture() }; runCatching { capturer?.dispose() }; capturer = null
        runCatching { localVideo?.dispose() }; localVideo = null
        runCatching { localAudio?.dispose() }; localAudio = null
        runCatching { videoSource?.dispose() }; videoSource = null
        runCatching { audioSource?.dispose() }; audioSource = null
        runCatching { surfaceHelper?.dispose() }; surfaceHelper = null
        remoteSinks.clear(); pendingCandidates = mutableListOf(); pendingSignals = mutableListOf()
        remoteVideo = null; connected = false; remoteShowing = false; live = false; callId = null
        (appCtx?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { am ->
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = false
            am.mode = AudioManager.MODE_NORMAL
        }
    }

    /** Short helpers for the WebRTC callbacks (success only; failures leave the call to the server's clock). */
    private fun created(onCreate: (SessionDescription) -> Unit) = object : SdpObserver {
        override fun onCreateSuccess(d: SessionDescription) = onCreate(d)
        override fun onSetSuccess() {}
        override fun onCreateFailure(e: String?) {}
        override fun onSetFailure(e: String?) {}
    }
    private fun setDone(onSet: () -> Unit) = object : SdpObserver {
        override fun onCreateSuccess(d: SessionDescription) {}
        override fun onSetSuccess() = onSet()
        override fun onCreateFailure(e: String?) {}
        override fun onSetFailure(e: String?) {}
    }
}

/**
 * Draws a video track in a TextureView. Unlike a SurfaceView it is clipped like any other view, so rounded corners and
 * shapes work (no dark square behind the small "me" window). The picture is cropped to fill the view.
 */
class TextureVideoView(ctx: Context) : android.view.TextureView(ctx), android.view.TextureView.SurfaceTextureListener, VideoSink {
    private val renderer = org.webrtc.EglRenderer("vibe-video")
    /** The track this view is showing now (so it can be swapped without leaking sinks). */
    var track: VideoTrack? = null
        private set

    init {
        renderer.init(CallEngine.egl.eglBaseContext, EglBase.CONFIG_PLAIN, org.webrtc.GlRectDrawer())
        surfaceTextureListener = this
        isOpaque = false
    }

    fun show(t: VideoTrack?) {
        if (t === track) return
        runCatching { track?.removeSink(this) }   // the old track may already be gone when a call ended
        renderer.clearImage()                       // never leave the previous person's last picture on screen
        track = t
        runCatching { t?.addSink(this) }
    }

    fun setMirror(m: Boolean) = renderer.setMirror(m)

    override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, w: Int, h: Int) {
        renderer.createEglSurface(st)
        if (h > 0) renderer.setLayoutAspectRatio(w.toFloat() / h)
    }
    override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, w: Int, h: Int) { if (h > 0) renderer.setLayoutAspectRatio(w.toFloat() / h) }
    override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
        val done = java.util.concurrent.CountDownLatch(1)
        renderer.releaseEglSurface { done.countDown() }
        runCatching { done.await(500, java.util.concurrent.TimeUnit.MILLISECONDS) }
        return true
    }
    override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
    override fun onFrame(frame: org.webrtc.VideoFrame) = renderer.onFrame(frame)

    fun release() { show(null); renderer.release() }
}

/** A video view for Compose: the other person's video ([remote] = true) or my own camera. */
@androidx.compose.runtime.Composable
fun VideoView(modifier: androidx.compose.ui.Modifier, remote: Boolean, mirror: Boolean = false) {
    // read as state: when the camera starts or the other person's video arrives, the view switches to it
    val track = if (remote) CallEngine.remoteVideo else CallEngine.localVideo
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { c -> TextureVideoView(c) },
        update = { v -> v.show(track); v.setMirror(mirror) },
        onRelease = { v -> v.release() },
        modifier = modifier,
    )
}
