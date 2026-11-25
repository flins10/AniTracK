package com.animeuniverse.app

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.io.File

/**
 * Production Firebase Cloud Messaging (FCM) service for Anime Universe Android APK.
 * - Persists new/refreshed FCM tokens to `filesDir/device_push_token.json` so Tauri's
 *   `get_device_notification_status` and Supabase `register-device` Edge Function sync them.
 * - Displays high-priority native system notifications when data/notification payloads arrive,
 *   while strictly respecting OS permission (`POST_NOTIFICATIONS`) and the user's local
 *   `notif_prefs.json` master/category toggles.
 */
class AnimeFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val cleanToken = token.trim()
        if (cleanToken.isEmpty()) return
        runCatching {
            val payload = JSONObject().apply {
                put("token", cleanToken)
                put("provider", "fcm")
                put("updated_at", System.currentTimeMillis())
            }
            File(applicationContext.filesDir, "device_push_token.json").writeText(payload.toString())
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        AnimeNotifScheduler.ensureChannels(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val areEnabled = runCatching {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        }.getOrDefault(false)
        if (!areEnabled) return

        // Check local notification preferences synced from user_notification_settings
        val prefsFile = File(applicationContext.filesDir, "notif_prefs.json")
        if (prefsFile.exists()) {
            val prefs = runCatching { JSONObject(prefsFile.readText()) }.getOrNull()
            if (prefs != null) {
                if (!prefs.optBoolean("master_enabled", true)) return
                val msgType = remoteMessage.data["type"] ?: "message"
                when (msgType) {
                    "message", "reply", "mention" -> if (!prefs.optBoolean("messages_enabled", true)) return
                    "update" -> if (!prefs.optBoolean("updates_enabled", true)) return
                    "reminder" -> if (!prefs.optBoolean("reminders_enabled", true)) return
                }
            }
        }

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Anime Universe"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: "You have a new notification in Anime Universe."
        val notificationId = remoteMessage.data["notification_id"]
            ?: remoteMessage.messageId
            ?: System.currentTimeMillis().toString()
        val targetScreen = remoteMessage.data["target_screen"] ?: "notifications"
        val topic = remoteMessage.data["topic"] ?: "general"
        val messageId = remoteMessage.data["message_id"] ?: ""
        val type = remoteMessage.data["type"] ?: "message"

        val launchIntent = (applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?: Intent(applicationContext, MainActivity::class.java)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_id", notificationId)
            putExtra("target_screen", targetScreen)
            putExtra("topic", topic)
            putExtra("message_id", messageId)
            putExtra("type", type)
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId.hashCode(),
            launchIntent,
            pendingFlags
        )

        val channelId = AnimeNotifScheduler.channelForType(type)
        val smallIconRes = AnimeNotifScheduler.resolveSmallIcon(applicationContext)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(notificationId.hashCode(), notification)
        }
    }
}
