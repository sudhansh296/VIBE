package com.vibe.app.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.vibe.app.R

/**
 * "Video call in progress": keeps the call alive while VIBE is in the background (the user pressed Home or opened another app).
 * Android stops the camera and microphone of background apps; a foreground service of type camera + microphone with a visible
 * notification is the allowed way to keep them on (like WhatsApp). Tapping the notification brings the call back; "End call" ends it.
 */
class CallService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_END) {
            Realtime.activeCall?.callId?.let { Realtime.endCall(it) }
            stopSelf()
            return START_NOT_STICKY
        }
        val name = intent?.getStringExtra(EXTRA_NAME) ?: "VIBE"
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Ongoing calls", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while a video call is running"
                setShowBadge(false)
            })
        }
        val open = packageManager.getLaunchIntentForPackage(packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val openPi = PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val endPi = PendingIntent.getService(this, 2, Intent(this, CallService::class.java).setAction(ACTION_END), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_app)
            .setContentTitle("Video call with $name")
            .setContentText("Tap to return to the call")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setUsesChronometer(true)
            .setContentIntent(openPi)
            .addAction(0, "End call", endPi)
            .build()
        val types = if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        runCatching { ServiceCompat.startForeground(this, NOTE_ID, n, types) }.onFailure { android.util.Log.e("VibeCall", "startForeground failed", it); stopSelf() }
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL = "vibe_calls"
        private const val NOTE_ID = 4201
        private const val EXTRA_NAME = "name"
        private const val ACTION_END = "com.vibe.callme.END_CALL"

        /** Call is on: show the notification and keep camera + microphone running in the background. Must start while the app is open. */
        fun start(ctx: Context, peerName: String) {
            runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, CallService::class.java).putExtra(EXTRA_NAME, peerName)) }
                .onFailure { android.util.Log.e("VibeCall", "start service failed", it) }
            android.util.Log.i("VibeCall", "call service start requested")
        }

        fun stop(ctx: Context) { runCatching { ctx.stopService(Intent(ctx, CallService::class.java)) } }
    }
}
