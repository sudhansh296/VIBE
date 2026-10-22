package com.vibe.app.ui

import kotlinx.coroutines.launch
import com.vibe.app.ui.components.toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.vibe.app.data.CallResult
import com.vibe.app.data.HostFilter
import com.vibe.app.ui.screens.calls.IncomingCallScreen
import com.vibe.app.ui.screens.calls.OutgoingVideoScreen
import com.vibe.app.ui.screens.calls.RateCallScreen
import com.vibe.app.data.Session
import com.vibe.app.ui.screens.ChatScreen
import com.vibe.app.ui.screens.CheckInScreen
import com.vibe.app.ui.screens.LevelScreen
import com.vibe.app.ui.screens.MeScreen
import com.vibe.app.ui.screens.VipScreen
import com.vibe.app.ui.screens.WalletScreen
import com.vibe.app.ui.screens.MyMediaScreen
import com.vibe.app.ui.screens.extras.AppStatesScreen
import com.vibe.app.ui.screens.extras.FollowListScreen
import com.vibe.app.ui.screens.extras.GiftsHistoryScreen
import com.vibe.app.ui.screens.extras.InviteScreen
import com.vibe.app.ui.screens.extras.LanguageScreen
import com.vibe.app.ui.screens.extras.PaymentResultScreen
import com.vibe.app.ui.screens.extras.PermissionScreen
import com.vibe.app.ui.screens.extras.ReceiptScreen
import com.vibe.app.ui.screens.extras.VipManageScreen
import com.vibe.app.ui.screens.safety.BlockedUsersScreen
import com.vibe.app.ui.screens.safety.GuidelinesScreen
import com.vibe.app.ui.screens.safety.PrivacyScreen
import com.vibe.app.ui.screens.safety.ReportUserScreen
import com.vibe.app.ui.screens.safety.SafetyCenterScreen
import com.vibe.app.ui.screens.safety.TermsScreen
import com.vibe.app.ui.screens.status.MyStatusScreen
import com.vibe.app.ui.screens.status.StatusCreateScreen
import com.vibe.app.ui.screens.status.StatusViewScreen
import com.vibe.app.ui.screens.DeleteAccountScreen
import com.vibe.app.ui.screens.EditProfileScreen
import com.vibe.app.ui.screens.HelpScreen
import com.vibe.app.ui.screens.SettingsScreen
import com.vibe.app.ui.screens.VisitorsScreen
import com.vibe.app.ui.screens.host.BecomeHostScreen
import com.vibe.app.ui.screens.host.HostApplicationScreen
import com.vibe.app.ui.screens.host.HostDashboardScreen
import com.vibe.app.ui.screens.host.HostRatesScreen
import com.vibe.app.ui.screens.host.WithdrawScreen
import com.vibe.app.ui.screens.ChatsScreen
import com.vibe.app.ui.screens.NotificationsScreen
import com.vibe.app.ui.screens.FilterScreen
import com.vibe.app.ui.screens.HomeScreen
import com.vibe.app.ui.screens.HostProfileScreen
import com.vibe.app.ui.screens.RandomMatchScreen
import com.vibe.app.ui.screens.LoginScreen
import com.vibe.app.ui.screens.OtpScreen
import com.vibe.app.ui.screens.ProfileSetupScreen
import com.vibe.app.ui.screens.SplashScreen
import com.vibe.app.ui.theme.VibeColors

@Composable
fun VibeApp(debugRoute: String? = null) {
    val appCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    androidx.compose.runtime.LaunchedEffect(Unit) { com.vibe.app.ui.components.MyProfile.restore(appCtx) }
    val nav = rememberNavController()
    // id of the call made small with Back (its screen is closed); the end summary is shown here only for that call
    var minimizedCall by remember { mutableStateOf<String?>(null) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(debugRoute) { if (debugRoute != null) nav.navigate(debugRoute) }
    var filter by remember { mutableStateOf(HostFilter()) }
    val ctx = LocalContext.current
    // already onboarded once -> skip login/OTP/profile setup and open Home
    val start = remember { if (Session.isLoggedIn(ctx)) "home" else "login" }

    // call flow: every call screen sits directly on top of Home, so finishing always lands back on Home
    // video calls only. A person from the server is called through the server (it rings the host and checks everything);
    // the ringing screen opens when the server says "ringing". The built-in test hosts keep the demo flow.
    fun startCall(id: Int) {
        com.vibe.app.data.Realtime.startCall(id) { err -> if (err != null) ctx.toast(err) }
    }
    fun replaceCall(route: String) = nav.navigate(route) { popUpTo("home") }
    // after a real login: a returning user with a full profile goes straight Home, a new one fills the profile first
    fun afterLogin(profileComplete: Boolean) {
        com.vibe.app.data.Realtime.start()
        if (profileComplete) { Session.markLoggedIn(ctx); nav.navigate("home") { popUpTo(nav.graph.id) { inclusive = true } } }
        else nav.navigate("profile") { popUpTo("login") }
    }
    // a friend's code from an invite link: someone already signed in gets it applied now (new people at the end of sign-up)
    val linkCode = com.vibe.app.data.InviteLink.pending
    androidx.compose.runtime.LaunchedEffect(linkCode) { if (linkCode != null) com.vibe.app.data.InviteLink.applyNow(ctx) }
    var watcher by remember { mutableStateOf<com.vibe.app.data.LiveEvent.Watching?>(null) }   // banner: someone opened my chat / is on my profile
    androidx.compose.runtime.LaunchedEffect(watcher) { if (watcher != null) { kotlinx.coroutines.delay(5000); watcher = null } }
    // live connection: online while the app is open; the server tells us about watchers and calls
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.vibe.app.data.Realtime.init()
        com.vibe.app.data.CallEngine.init(ctx)
        if (com.vibe.app.data.api.AuthStore.hasSession) { com.vibe.app.data.Realtime.start(); com.vibe.app.data.ChatRepo.refreshUnread(); com.vibe.app.data.NotifRepo.refreshUnread() }
        com.vibe.app.data.Realtime.events.collect { e ->
            when (e) {
                is com.vibe.app.data.LiveEvent.Watching -> watcher = e
                is com.vibe.app.data.LiveEvent.Incoming -> nav.navigate("incoming/${e.from.id}")
                is com.vibe.app.data.LiveEvent.Ringing -> nav.navigate("calling/${e.host.id}")
                // the app was restarted while a call was on: a running call opens again (and keeps being charged, so it must be
                // visible); a call that was still ringing is cancelled, its screen is gone
                // the call ended while it was minimised (its screen is not open): show the summary like a normal end
                is com.vibe.app.data.LiveEvent.Ended -> {
                    val wasSmall = minimizedCall == e.callId && nav.currentBackStackEntry?.destination?.route != "video/{id}"
                    if (minimizedCall == e.callId) minimizedCall = null
                    if (wasSmall && e.peerId != null && e.seconds > 0) {
                        if (e.role == "user") nav.navigate("rate/${e.peerId}/${e.seconds}/${e.coins}/${com.vibe.app.data.Realtime.callGifts}?call=${e.callId}")
                        else ctx.toast("Call ended · %02d:%02d · +%,d earned".format(e.seconds / 60, e.seconds % 60, e.earned))
                    }
                }
                is com.vibe.app.data.LiveEvent.Resume -> {
                    val ac = com.vibe.app.data.Realtime.activeCall
                    if (e.state == "active") nav.navigate("video/${e.peerId}") { popUpTo("home") }
                    else if (ac?.role == "host") com.vibe.app.data.Realtime.declineCall(e.callId) else com.vibe.app.data.Realtime.cancelCall(e.callId)
                }
                else -> {}
            }
        }
    }
    // when signed in: fresh profile and block list from the server
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (com.vibe.app.data.api.AuthStore.hasSession) {
            runCatching { com.vibe.app.data.api.Api.get("/me") }.onSuccess { com.vibe.app.data.api.AuthStore.saveUser(it.getJSONObject("user")) }
            runCatching { com.vibe.app.data.SafetyStore.sync() }
        }
    }
    // the server ended the session (expired, logged out, blocked or deleted): back to login
    val ended = com.vibe.app.data.api.AuthStore.sessionEnded
    androidx.compose.runtime.LaunchedEffect(ended) {
        if (ended > 0) {
            ctx.getSharedPreferences("vibe_session", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            ctx.toast(com.vibe.app.data.api.AuthStore.endReason)
            nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }
    // the server can stop the app (maintenance) or force an update; checked on every start
    var gate by remember { mutableStateOf<org.json.JSONObject?>(null) }
    suspend fun checkGate(): Boolean {
        val c = runCatching { com.vibe.app.data.api.Api.get("/config", auth = false).getJSONObject("config") }.getOrNull() ?: return false
        gate = c
        val route = when {
            c.optBoolean("maintenance_mode", false) -> "app-blocked/maint"
            versionLess(com.vibe.app.BuildConfig.VERSION_NAME, c.optString("min_app_version", "0")) -> "app-blocked/force"
            else -> null
        }
        if (route != null) nav.navigate(route) { popUpTo(nav.graph.id) { inclusive = true } }
        return route != null
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { checkGate() }
    val gateScope = androidx.compose.runtime.rememberCoroutineScope()
    fun intArg(e: androidx.navigation.NavBackStackEntry, key: String) = e.arguments?.getString(key)?.toIntOrNull() ?: 0

    // a running call that was made small (back on the call screen): a strip at the top, every screen moves down under it
    val backEntry by nav.currentBackStackEntryAsState()
    val ac = com.vibe.app.data.Realtime.activeCall
    val showBar = ac?.startedAt != null && backEntry?.destination?.route != "video/{id}"
    // where the small call pill was dragged to (kept while the app runs); null = default spot, top right
    val pillPos = remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        // Login is composed underneath while the splash video plays, so the hand-off is just a fade.
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 12 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable("login") {
                LoginScreen(
                    onOtpSent = { phone -> nav.navigate("otp/$phone") },
                    onGoogle = { r -> afterLogin(r.profileComplete) },
                )
            }
            composable("otp/{phone}") { entry ->
                OtpScreen(
                    phone = entry.arguments?.getString("phone").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onVerified = { r -> afterLogin(r.profileComplete) },
                )
            }
            composable("profile") {
                ProfileSetupScreen(
                    onBack = { nav.popBackStack() },
                    onDone = {
                        Session.markLoggedIn(ctx)
                        nav.navigate("permission/cam") { popUpTo(nav.graph.id) { inclusive = true } }
                    },
                )
            }
            composable("home") {
                HomeScreen(
                    filter = filter,
                    onOpenFilter = { nav.navigate("filter") },
                    onOpenHost = { id -> nav.navigate("host/$id") },
                    onRandomMatch = { nav.navigate("match") },
                    onResetFilter = { filter = HostFilter() },
                    onCall = { id -> startCall(id) },
                    onNotifications = { nav.navigate("notifications") },
                    onOpenChats = { nav.navigate("chats") },
                    onOpenMe = { nav.navigate("me") },
                    onStatusMine = { nav.navigate("my-status") },   // it checks the server and opens "add" when nothing is up
                    onStatusView = { i -> nav.navigate("status/$i") },
                    onWallet = { nav.navigate("wallet") },
                )
            }
            composable("filter") {
                FilterScreen(filter, onClose = { nav.popBackStack() }, onApply = { filter = it; nav.popBackStack() })
            }
            composable("host/{id}") { entry ->
                HostProfileScreen(
                    entry.arguments?.getString("id")?.toIntOrNull() ?: 1,
                    onBack = { nav.popBackStack() },
                    onCall = { id -> startCall(id) },
                    onChat = { id -> nav.navigate("chat/$id") },
                    onWallet = { nav.navigate("wallet") },
                    onReport = { id -> nav.navigate("report/$id") },
                )
            }

            // messages
            composable("chats") {
                ChatsScreen(
                    onHome = { nav.popBackStack() },
                    onOpenChat = { id -> nav.navigate("chat/$id") },
                    onOpenHost = { id -> nav.navigate("host/$id") },
                    onNotifications = { nav.navigate("notifications") },
                    onCall = { id -> startCall(id) },
                    onOpenMe = { nav.navigate("me") { popUpTo("home") } },
                    onReport = { id -> nav.navigate("report/$id") },
                )
            }
            composable("chat/{id}") { e ->
                ChatScreen(
                    hostId = intArg(e, "id"),
                    onBack = { nav.popBackStack() },
                    onOpenProfile = { id -> nav.navigate("host/$id") },
                    onCall = { id -> startCall(id) },
                    onReport = { id -> nav.navigate("report/$id") },
                    onWallet = { nav.navigate("wallet") },
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    onBack = { nav.popBackStack() },
                    onCall = { id -> startCall(id) },
                    onChat = { id -> nav.navigate("chat/$id") },
                )
            }
            // me
            composable("me") {
                MeScreen(
                    onHome = { nav.popBackStack("home", false) },
                    onChats = { nav.navigate("chats") { popUpTo("home") } },
                    onNotifications = { nav.navigate("notifications") },
                    onWallet = { nav.navigate("wallet") },
                    onVip = { nav.navigate("vip") },
                    onCheckIn = { nav.navigate("checkin") },
                    onLevel = { nav.navigate("level") },
                    onBecomeHost = { nav.navigate(if (com.vibe.app.data.api.AuthStore.me?.optString("role") == "host") "host-dashboard" else "become-host") },
                    onSettings = { nav.navigate("settings") },
                    onEditProfile = { nav.navigate("edit-profile") },
                    onVisitors = { nav.navigate("visitors") },
                    onHelp = { nav.navigate("help") },
                    onFollowing = { nav.navigate("follow/following") },
                    onFollowers = { nav.navigate("follow/followers") },
                    onInvite = { nav.navigate("invite") },
                )
            }
            composable("wallet") {
                WalletScreen(
                    onBack = { nav.popBackStack() },
                    onResult = { kind, order -> nav.navigate("payment/$kind?order=$order") },
                    onReceipt = { order -> nav.navigate("receipt?order=$order") },
                )
            }
            composable("vip") { VipScreen(onBack = { nav.popBackStack() }, onManage = { nav.navigate("vip-manage") }) }
            composable("level") { LevelScreen(onBack = { nav.popBackStack() }) }
            composable("checkin") { CheckInScreen(onBack = { nav.popBackStack() }) }

            // settings
            composable("settings") {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onEditProfile = { nav.navigate("edit-profile") },
                    onCallSettings = { nav.navigate("host-rates") },
                    onWithdraw = { nav.navigate("withdraw") },
                    onHelp = { nav.navigate("help") },
                    onDeleteAccount = { nav.navigate("delete-account") },
                    onSignedOut = { nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } } },
                    onBlocked = { nav.navigate("blocked") },
                    onSafety = { nav.navigate("safety") },
                    onTerms = { nav.navigate("terms") },
                    onPrivacy = { nav.navigate("privacy") },
                    onGuidelines = { nav.navigate("guidelines") },
                    onLanguage = { nav.navigate("language") },
                    onStatesDemo = { nav.navigate("state/loading") },
                )
            }
            composable("edit-profile") { EditProfileScreen(onBack = { nav.popBackStack() }, onHostMedia = { nav.navigate("my-media") }) }
            composable("help") { HelpScreen(onBack = { nav.popBackStack() }) }
            composable("visitors") {
                VisitorsScreen(
                    onBack = { nav.popBackStack() },
                    onChat = { id -> nav.navigate("chat/$id") },
                    onCall = { id -> startCall(id) },
                    onVip = { nav.navigate("vip") },
                )
            }
            composable("delete-account") {
                DeleteAccountScreen(
                    onBack = { nav.popBackStack() },
                    onWithdraw = { nav.navigate("withdraw") },
                    onDeleted = { nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } } },
                )
            }

            // status
            composable("status/{i}") { e ->
                StatusViewScreen(
                    startIndex = intArg(e, "i"),
                    onClose = { nav.popBackStack() },
                    onCall = { id -> nav.popBackStack(); startCall(id) },
                    onProfile = { id -> nav.navigate("host/$id") },
                )
            }
            composable("status-create") { StatusCreateScreen(onClose = { nav.popBackStack() }, onShared = { nav.navigate("my-status") { popUpTo("home") } }) }
            composable("my-status") {
                MyStatusScreen(
                    onClose = { nav.popBackStack() },
                    onAdd = { nav.navigate("status-create") },
                    onEmpty = { nav.navigate("status-create") { popUpTo("my-status") { inclusive = true } } },
                    onDeleted = { nav.popBackStack() },
                )
            }

            // photos & videos, safety and rules
            composable("my-media") { MyMediaScreen(onBack = { nav.popBackStack() }) }
            composable("report/{id}") { e ->
                ReportUserScreen(intArg(e, "id"), onBack = { nav.popBackStack() }, onSafetyCenter = { nav.navigate("safety") })
            }
            composable("blocked") { BlockedUsersScreen(onBack = { nav.popBackStack() }) }
            composable("terms") { TermsScreen(onBack = { nav.popBackStack() }, onPrivacy = { nav.navigate("privacy") }) }
            composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }, onTerms = { nav.navigate("terms") }) }
            composable("guidelines") {
                GuidelinesScreen(onBack = { nav.popBackStack() }, onReport = { nav.navigate("report/1") }, onBlocked = { nav.navigate("blocked") })
            }
            composable("safety") {
                SafetyCenterScreen(
                    onBack = { nav.popBackStack() }, onReport = { nav.navigate("report/1") }, onBlocked = { nav.navigate("blocked") },
                    onGuidelines = { nav.navigate("guidelines") }, onSettings = { nav.navigate("settings") },
                    onTerms = { nav.navigate("terms") }, onPrivacy = { nav.navigate("privacy") }, onSupport = { nav.navigate("help") },
                )
            }

            // permissions, app states, payments and the rest
            composable("permission/{kind}") { e ->
                val kind = e.arguments?.getString("kind").orEmpty()
                PermissionScreen(kind, onDone = {
                    if (kind == "cam") nav.navigate("permission/notif") { popUpTo("permission/cam") { inclusive = true } }
                    else nav.navigate("home") { popUpTo(nav.graph.id) { inclusive = true } }
                })
            }
            composable("app-blocked/{kind}") { e ->
                androidx.activity.compose.BackHandler { (ctx as? android.app.Activity)?.finish() } // no way around it except leaving the app
                AppStatesScreen(
                    e.arguments?.getString("kind").orEmpty(), onClose = {}, demo = false,
                    message = gate?.optString("maintenance_message")?.takeIf { it.isNotBlank() },
                    minVersion = gate?.optString("min_app_version"),
                    onCheckAgain = {
                        gateScope.launch {
                            if (!checkGate()) nav.navigate(if (Session.isLoggedIn(ctx)) "home" else "login") { popUpTo(nav.graph.id) { inclusive = true } }
                            else ctx.toast("Still under maintenance, try again soon")
                        }
                    },
                )
            }
            composable("state/{kind}") { e -> AppStatesScreen(e.arguments?.getString("kind").orEmpty(), onClose = { nav.popBackStack() }) }
            composable("payment/{kind}?order={order}") { e ->
                val order = e.arguments?.getString("order")?.takeIf { it.isNotBlank() }
                PaymentResultScreen(
                    e.arguments?.getString("kind").orEmpty(), order,
                    // back to a fresh Wallet (new balance)
                    onWallet = { nav.navigate("wallet") { popUpTo("wallet") { inclusive = true } } },
                    onReceipt = { nav.navigate("receipt?order=${order.orEmpty()}") },
                    onSupport = { nav.navigate("help") },
                )
            }
            composable("receipt?order={order}") { e ->
                ReceiptScreen(e.arguments?.getString("order")?.takeIf { it.isNotBlank() }, onBack = { nav.popBackStack() }, onSupport = { nav.navigate("help") })
            }
            composable("follow/{tab}") { e ->
                FollowListScreen(
                    e.arguments?.getString("tab").orEmpty(), onBack = { nav.popBackStack() },
                    onOpenHost = { id -> nav.navigate("host/$id") }, onDiscover = { nav.popBackStack("home", false) },
                    onChat = { id -> nav.navigate("chat/$id") }, onCall = { id -> startCall(id) },
                )
            }
            composable("invite") { InviteScreen(onBack = { nav.popBackStack() }) }
            composable("language") { LanguageScreen(onBack = { nav.popBackStack() }) }
            composable("vip-manage") { VipManageScreen(onBack = { nav.popBackStack() }, onChangePlan = { nav.navigate("vip") }, onReceipts = { nav.navigate("receipt?order=") }) }
            composable("gifts") { GiftsHistoryScreen(onBack = { nav.popBackStack() }) }

            // host
            composable("become-host") { BecomeHostScreen(onBack = { nav.popBackStack() }, onApply = { nav.navigate("host-apply") }) }
            composable("host-apply") {
                HostApplicationScreen(
                    onBack = { nav.popBackStack() },
                    onPreviewDashboard = { nav.navigate("host-dashboard") { popUpTo("become-host") { inclusive = true } } },
                )
            }
            composable("host-dashboard") {
                HostDashboardScreen(onBack = { nav.popBackStack() }, onRates = { nav.navigate("host-rates") }, onWithdraw = { nav.navigate("withdraw") }, onGifts = { nav.navigate("gifts") })
            }
            composable("host-rates") { HostRatesScreen(onBack = { nav.popBackStack() }) }
            composable("withdraw") { WithdrawScreen(onBack = { nav.popBackStack() }) }
            composable("match") {
                RandomMatchScreen(onClose = { nav.popBackStack() }, onConnect = { id -> replaceCall("video/$id") }, onWallet = { nav.navigate("wallet") })
            }

            // calls
            composable("calling/{id}") { e ->
                OutgoingVideoScreen(intArg(e, "id"), onConnected = { id -> replaceCall("video/$id") }, onCancel = { nav.popBackStack() })
            }
            composable("video/{id}") { e ->
                val id = intArg(e, "id")
                // a real call through the server (video over WebRTC); the built-in test hosts keep the demo screen
                com.vibe.app.ui.screens.calls.LiveCallScreen(id, onFinished = { r, callId ->
                    if (r.secs <= 0) nav.popBackStack("home", false)
                    else replaceCall("rate/${r.hostId}/${r.secs}/${r.spent}/${r.gifts}?call=${callId.orEmpty()}")
                }, onMinimize = {
                    minimizedCall = com.vibe.app.data.Realtime.activeCall?.callId
                    if (!nav.popBackStack()) nav.navigate("home")
                })
            }
            composable("incoming/{id}") { e ->
                val id = intArg(e, "id")
                IncomingCallScreen(id, onAccept = { replaceCall("video/$id") }, onDone = { nav.popBackStack() })
            }
            composable("rate/{id}/{secs}/{spent}/{gifts}?call={call}") { e ->
                RateCallScreen(
                    CallResult(intArg(e, "id"), intArg(e, "secs"), intArg(e, "spent"), intArg(e, "gifts")),
                    callId = e.arguments?.getString("call")?.takeIf { it.isNotBlank() },
                    onReport = { id -> nav.navigate("report/$id") },
                    onDone = { nav.popBackStack() },
                    onCallAgain = { r -> nav.popBackStack(); startCall(r.hostId) },
                )
            }
        }

        if (showBar && ac != null) MiniCallBar(
            ac, pos = pillPos,
            onOpen = { minimizedCall = null; nav.navigate("video/${ac.peerId}") { launchSingleTop = true } },
            onEnd = { com.vibe.app.data.Realtime.endCall(ac.callId) },
        )

        // gifts sent or received play here, centred over whatever screen is open
        com.vibe.app.ui.components.GiftOverlay()

        AnimatedVisibility(
            visible = watcher != null, enter = androidx.compose.animation.slideInVertically { -it } + fadeIn(), exit = androidx.compose.animation.slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
        ) {
            // tap: straight into that chat, or to the person's profile
            watcher?.let { w -> WatchBanner(w.who, if (w.where == "chat") "is in your chat right now" else "is looking at your profile right now") { watcher = null; nav.navigate(if (w.where == "chat") "chat/${w.who.id}" else "host/${w.who.id}") } }
        }
        // a gift received away from that chat / call
        val giftNote = com.vibe.app.data.GiftQueue.banner
        androidx.compose.runtime.LaunchedEffect(giftNote) { if (giftNote != null) { kotlinx.coroutines.delay(5000); com.vibe.app.data.GiftQueue.banner = null } }
        AnimatedVisibility(
            visible = giftNote != null && watcher == null, enter = androidx.compose.animation.slideInVertically { -it } + fadeIn(), exit = androidx.compose.animation.slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
        ) {
            giftNote?.let { (who, g) ->
                val qty = if (g.quantity > 1) "${g.quantity}× " else "a "
                WatchBanner(who, "sent you $qty${g.gift.name}", action = "Open") { com.vibe.app.data.GiftQueue.banner = null; nav.navigate("chat/${who.id}") }
            }
        }

        AnimatedVisibility(visible = showSplash, enter = EnterTransition.None, exit = fadeOut(tween(450))) {
            SplashScreen(onDone = { showSplash = false })
        }
    }
}

/** True when version a (e.g. "1.0.0") is older than b. */
private fun versionLess(a: String, b: String): Boolean {
    val x = a.split('.').map { it.toIntOrNull() ?: 0 }
    val y = b.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(x.size, y.size)) {
        val p = x.getOrElse(i) { 0 }; val q = y.getOrElse(i) { 0 }
        if (p != q) return p < q
    }
    return false
}


/** Top banner: someone opened my chat, has been on my profile for a few seconds, or sent me a gift. */
@Composable
private fun WatchBanner(who: com.vibe.app.data.Host, line: String, action: String = "View", onOpen: () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    androidx.compose.foundation.layout.Row(
        Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = VibeColors.Pink, spotColor = VibeColors.Pink).clip(shape)
            .background(androidx.compose.ui.graphics.Color.White).clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        com.vibe.app.ui.components.Avatar(who, Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape), fontSize = 18.sp)
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            androidx.compose.material3.Text(who.name, color = VibeColors.Ink, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1)
            androidx.compose.material3.Text(line, color = VibeColors.TextDim, fontSize = 12.sp, maxLines = 1)
        }
        androidx.compose.material3.Text(action, color = VibeColors.Pink, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}


/**
 * The small call pill: floats over every screen while a call runs with its screen closed (after Back on the call screen).
 * Drag it anywhere; tap = back to the full call; the red button ends the call.
 */
@Composable
private fun MiniCallBar(
    call: com.vibe.app.data.Realtime.ActiveCall,
    pos: androidx.compose.runtime.MutableState<androidx.compose.ui.geometry.Offset?>,
    onOpen: () -> Unit,
    onEnd: () -> Unit,
) {
    val peer = com.vibe.app.data.HostRepo.cache[call.peerId] ?: com.vibe.app.data.Demo.host(call.peerId)
    var secs by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(call.callId) {
        while (true) { secs = (((System.currentTimeMillis() - (call.startedAt ?: System.currentTimeMillis())) / 1000).toInt()).coerceAtLeast(0); kotlinx.coroutines.delay(500) }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val topInset = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(density)
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val areaW = constraints.maxWidth.toFloat(); val areaH = constraints.maxHeight.toFloat()
        val size = remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        val margin = with(density) { 12.dp.toPx() }
        val firstY = topInset + with(density) { 64.dp.toPx() }
        // the pill stays fully on screen; first time it sits top right, just under the status bar
        fun place(o: androidx.compose.ui.geometry.Offset?): androidx.compose.ui.geometry.Offset {
            val w = size.value.width; val h = size.value.height
            val p = o ?: androidx.compose.ui.geometry.Offset(areaW - w - margin, firstY)
            return androidx.compose.ui.geometry.Offset(
                p.x.coerceIn(margin, (areaW - w - margin).coerceAtLeast(margin)),
                p.y.coerceIn(topInset + margin, (areaH - h - margin).coerceAtLeast(topInset + margin)),
            )
        }
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
        androidx.compose.foundation.layout.Row(
            Modifier
                .offset { val p = place(pos.value); androidx.compose.ui.unit.IntOffset(p.x.toInt(), p.y.toInt()) }
                .onSizeChanged { size.value = it }
                .pointerInput(areaW, areaH) {
                    detectDragGestures { change, drag -> change.consume(); pos.value = place(place(pos.value) + drag) }
                }
                .shadow(10.dp, shape, ambientColor = VibeColors.Pink, spotColor = VibeColors.Pink).clip(shape)
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(com.vibe.app.ui.components.CallDark, VibeColors.Pink)))
                .clickable(onClick = onOpen).padding(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            com.vibe.app.ui.components.NeonRingAvatar(peer.name.take(1), 34.dp, 12.sp, host = peer)
            androidx.compose.foundation.layout.Column(Modifier.padding(start = 8.dp, end = 10.dp).widthIn(max = 110.dp)) {
                androidx.compose.material3.Text(peer.name, color = androidx.compose.ui.graphics.Color.White, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (com.vibe.app.data.CallEngine.connected) VibeColors.Green else androidx.compose.ui.graphics.Color(0xFFFFC2DD)))
                    androidx.compose.material3.Text("  %02d:%02d".format(secs / 60, secs % 60), color = androidx.compose.ui.graphics.Color(0xFFFFE3EF), fontSize = 11.sp, maxLines = 1)
                }
            }
            // red: end the call
            androidx.compose.foundation.layout.Box(
                Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape).background(com.vibe.app.ui.components.CallRed).clickable(onClick = onEnd),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) { androidx.compose.material3.Icon(com.vibe.app.ui.components.CallIcons.EndCall, "End call", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp)) }
        }
    }
}
