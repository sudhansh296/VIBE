package com.vibe.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.vibe.app.data.CallResult
import com.vibe.app.data.HostFilter
import com.vibe.app.ui.screens.calls.IncomingCallScreen
import com.vibe.app.ui.screens.calls.OutgoingVideoScreen
import com.vibe.app.ui.screens.calls.RateCallScreen
import com.vibe.app.ui.screens.calls.VideoCallScreen
import com.vibe.app.ui.screens.calls.VoiceCallScreen
import com.vibe.app.data.Session
import com.vibe.app.ui.screens.ChatScreen
import com.vibe.app.ui.screens.CheckInScreen
import com.vibe.app.ui.screens.LevelScreen
import com.vibe.app.ui.screens.MeScreen
import com.vibe.app.ui.screens.VipScreen
import com.vibe.app.ui.screens.WalletScreen
import com.vibe.app.ui.screens.MyMediaScreen
import com.vibe.app.ui.screens.extras.AppStatesScreen
import com.vibe.app.ui.screens.extras.ChangePhoneScreen
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
    val nav = rememberNavController()
    var showSplash by rememberSaveable { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(debugRoute) { if (debugRoute != null) nav.navigate(debugRoute) }
    var filter by remember { mutableStateOf(HostFilter()) }
    val ctx = LocalContext.current
    // already onboarded once -> skip login/OTP/profile setup and open Home
    val start = remember { if (Session.isLoggedIn(ctx)) "home" else "login" }

    // call flow: every call screen sits directly on top of Home, so finishing always lands back on Home
    fun startCall(id: Int, audio: Boolean) = nav.navigate(if (audio) "voice/$id/0" else "calling/$id")
    fun replaceCall(route: String) = nav.navigate(route) { popUpTo("home") }
    fun showRate(r: CallResult) = replaceCall("rate/${r.hostId}/${r.secs}/${r.spent}/${r.gifts}/${if (r.audio) 1 else 0}")
    fun intArg(e: androidx.navigation.NavBackStackEntry, key: String) = e.arguments?.getString(key)?.toIntOrNull() ?: 0

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
                    onGoogle = { nav.navigate("profile") },
                )
            }
            composable("otp/{phone}") { entry ->
                OtpScreen(
                    phone = entry.arguments?.getString("phone").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onVerified = { nav.navigate("profile") { popUpTo("login") } },
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
                    onCall = { id, audio -> startCall(id, audio) },
                    onIncomingDemo = { nav.navigate("incoming/2/audio") },
                    onNotifications = { nav.navigate("notifications") },
                    onOpenChats = { nav.navigate("chats") },
                    onOpenMe = { nav.navigate("me") },
                    onStatusMine = { nav.navigate(if (com.vibe.app.data.StatusStore.mine == null) "status-create" else "my-status") },
                    onStatusView = { i -> nav.navigate("status/$i") },
                )
            }
            composable("filter") {
                FilterScreen(filter, onClose = { nav.popBackStack() }, onApply = { filter = it; nav.popBackStack() })
            }
            composable("host/{id}") { entry ->
                HostProfileScreen(
                    entry.arguments?.getString("id")?.toIntOrNull() ?: 1,
                    onBack = { nav.popBackStack() },
                    onCall = { id, audio -> startCall(id, audio) },
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
                    onCall = { id, audio -> startCall(id, audio) },
                    onOpenMe = { nav.navigate("me") { popUpTo("home") } },
                    onReport = { id -> nav.navigate("report/$id") },
                )
            }
            composable("chat/{id}") { e ->
                ChatScreen(
                    hostId = intArg(e, "id"),
                    onBack = { nav.popBackStack() },
                    onOpenProfile = { id -> nav.navigate("host/$id") },
                    onCall = { id, audio -> startCall(id, audio) },
                    onReport = { id -> nav.navigate("report/$id") },
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    onBack = { nav.popBackStack() },
                    onCall = { id, audio -> startCall(id, audio) },
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
                    onBecomeHost = { nav.navigate("become-host") },
                    onSettings = { nav.navigate("settings") },
                    onEditProfile = { nav.navigate("edit-profile") },
                    onVisitors = { nav.navigate("visitors") },
                    onHelp = { nav.navigate("help") },
                    onFollowing = { nav.navigate("follow/following") },
                    onFollowers = { nav.navigate("follow/followers") },
                    onInvite = { nav.navigate("invite") },
                )
            }
            composable("wallet") { WalletScreen(onBack = { nav.popBackStack() }, onPaymentFailed = { nav.navigate("payment/failed") }, onReceipt = { nav.navigate("receipt") }) }
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
                    onChangePhone = { nav.navigate("change-phone") },
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
                    onCall = { id, audio -> startCall(id, audio) },
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
                    onCall = { id -> nav.popBackStack(); startCall(id, false) },
                    onProfile = { id -> nav.navigate("host/$id") },
                )
            }
            composable("status-create") { StatusCreateScreen(onClose = { nav.popBackStack() }, onShared = { nav.popBackStack() }) }
            composable("my-status") {
                MyStatusScreen(
                    onClose = { nav.popBackStack() },
                    onAdd = { nav.navigate("status-create") },
                    onDeleted = { nav.popBackStack() },
                )
            }

            // photos & videos, safety and rules
            composable("my-media") { MyMediaScreen(onBack = { nav.popBackStack() }, onPreview = { nav.navigate("host/1") }) }
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
            composable("state/{kind}") { e -> AppStatesScreen(e.arguments?.getString("kind").orEmpty(), onClose = { nav.popBackStack() }) }
            composable("payment/{kind}") { e ->
                PaymentResultScreen(
                    e.arguments?.getString("kind").orEmpty(),
                    onWallet = { nav.popBackStack("wallet", false) },
                    onReceipt = { nav.navigate("receipt") },
                    onSupport = { nav.navigate("help") },
                )
            }
            composable("receipt") { ReceiptScreen(onBack = { nav.popBackStack() }, onSupport = { nav.navigate("help") }) }
            composable("follow/{tab}") { e ->
                FollowListScreen(
                    e.arguments?.getString("tab").orEmpty(), onBack = { nav.popBackStack() },
                    onOpenHost = { id -> nav.navigate("host/$id") }, onDiscover = { nav.popBackStack("home", false) },
                    onChat = { id -> nav.navigate("chat/$id") }, onCall = { id, audio -> startCall(id, audio) },
                )
            }
            composable("invite") { InviteScreen(onBack = { nav.popBackStack() }) }
            composable("language") { LanguageScreen(onBack = { nav.popBackStack() }) }
            composable("change-phone") { ChangePhoneScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() }) }
            composable("vip-manage") { VipManageScreen(onBack = { nav.popBackStack() }, onChangePlan = { nav.navigate("vip") }, onReceipts = { nav.navigate("receipt") }) }
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
                RandomMatchScreen(onClose = { nav.popBackStack() }, onConnect = { id -> replaceCall("video/$id") })
            }

            // calls
            composable("calling/{id}") { e ->
                OutgoingVideoScreen(intArg(e, "id"), onConnected = { id -> replaceCall("video/$id") }, onCancel = { nav.popBackStack() })
            }
            composable("video/{id}") { e ->
                VideoCallScreen(intArg(e, "id"), onSwitchToVoice = { id -> replaceCall("voice/$id/1") }, onFinished = { showRate(it) })
            }
            composable("voice/{id}/{connected}") { e ->
                VoiceCallScreen(
                    intArg(e, "id"), startConnected = intArg(e, "connected") == 1,
                    onSwitchToVideo = { id -> replaceCall("video/$id") },
                    onFinished = { r -> if (r == null) nav.popBackStack() else showRate(r) },
                )
            }
            composable("incoming/{id}/{type}") { e ->
                val audio = e.arguments?.getString("type") == "audio"
                val id = intArg(e, "id")
                IncomingCallScreen(id, audio, onAccept = { replaceCall(if (audio) "voice/$id/1" else "video/$id") }, onDone = { nav.popBackStack() })
            }
            composable("rate/{id}/{secs}/{spent}/{gifts}/{audio}") { e ->
                RateCallScreen(
                    CallResult(intArg(e, "id"), intArg(e, "secs"), intArg(e, "spent"), intArg(e, "gifts"), intArg(e, "audio") == 1),
                    onDone = { nav.popBackStack() },
                    onCallAgain = { r -> nav.popBackStack(); startCall(r.hostId, r.audio) },
                )
            }
        }

        AnimatedVisibility(visible = showSplash, enter = EnterTransition.None, exit = fadeOut(tween(450))) {
            SplashScreen(onDone = { showSplash = false })
        }
    }
}
