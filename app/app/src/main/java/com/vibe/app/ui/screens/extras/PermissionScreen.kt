package com.vibe.app.ui.screens.extras

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.StateScreen
import com.vibe.app.ui.theme.VibeColors

private class Why(val title: String, val sub: String)

/**
 * Permission explanation screens shown before Android's own dialog: kind = "cam" (camera + microphone) or "notif" (notifications).
 * If the user denies, the screen switches to the "permission is turned off" version that opens the phone's app settings.
 */
@Composable
fun PermissionScreen(kind: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var denied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) onDone() else denied = true
    }

    val cam = kind == "cam"
    val tint = if (denied) VibeColors.Red else if (cam) VibeColors.Cyan else VibeColors.Pink
    val whys = when {
        denied -> listOf(Why("1. Open phone settings", "Tap the button below"), Why("2. Open Permissions", if (cam) "Find Camera and Microphone" else "Find Notifications"), Why("3. Choose Allow", "Come back to VIBE and try again"))
        cam -> listOf(
            Why("Video calls", "Show your face to the person you talk to"), Why("Voice calls", "Let them hear you"),
            Why("Selfie check and photos", "Verify you are real and add your photos and videos"),
            Why("Your privacy", "Calls are not recorded. The camera is used only when you start a call or tap the camera button"),
        )
        else -> listOf(
            Why("Incoming calls", "Ring even when the app is closed. Very important for hosts"), Why("Messages and replies", "Know when someone writes to you"),
            Why("Money and safety", "Payment, payout and account updates"), Why("You are in control", "Change this any time in Settings"),
        )
    }

    StateScreen(
        icon = if (denied) CallIcons.Ban else if (cam) CallIcons.Camera else MeIcons.Bell,
        tint = tint,
        title = if (denied) "Permission is turned off" else if (cam) "Allow camera and microphone" else "Never miss a call",
        subtitle = if (denied) (if (cam) "Calls and photos will not work until you allow camera and microphone in your phone settings." else "You will not hear incoming calls until you allow notifications in your phone settings.")
        else if (cam) "VIBE needs them to make video and voice calls and to verify you." else "Turn on notifications so you know when someone calls or messages you.",
        primary = if (denied) "Open phone settings" else if (cam) "Allow" else "Turn on notifications",
        onPrimary = {
            if (denied) {
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else if (cam) launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            else if (Build.VERSION.SDK_INT >= 33) launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            else onDone()   // below Android 13 notifications are allowed by default
        },
        secondary = if (denied) "Maybe later" else "Not now",
        onSecondary = onDone,
        content = {
            val shape = RoundedCornerShape(16.dp)
            Column(Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(horizontal = 16.dp)) {
                whys.forEachIndexed { i, w ->
                    Row(Modifier.padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(tint))
                        Column {
                            Text(w.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(w.sub, color = VibeColors.TextMute, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    if (i < whys.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
                }
            }
        },
    )
}
