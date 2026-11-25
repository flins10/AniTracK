package com.animeuniverse.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object AnimeNotifScheduler {
    private const val WORK_NAME = "anime_universe_bg_notifications"
    const val CHANNEL_ID = "anime_community_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Anime Universe Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Messages, reminders, updates, and community alerts from Anime Universe"
                    enableVibration(true)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun schedulePeriodic(context: Context) {
        ensureChannel(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val req = PeriodicWorkRequestBuilder<AnimeNotifWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}

class AnimeNotifBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AnimeNotifScheduler.schedulePeriodic(context)
        }
    }
}

class AnimeNotifWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        AnimeNotifScheduler.ensureChannel(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        // Check local notification preferences before polling
        val prefsFile = File(applicationContext.filesDir, "notif_prefs.json")
        var masterEnabled = true
        var messagesEnabled = true
        var updatesEnabled = true
        var remindersEnabled = true
        if (prefsFile.exists()) {
            runCatching {
                val p = JSONObject(prefsFile.readText())
                masterEnabled = p.optBoolean("master_enabled", true)
                messagesEnabled = p.optBoolean("messages_enabled", true)
                updatesEnabled = p.optBoolean("updates_enabled", true)
                remindersEnabled = p.optBoolean("reminders_enabled", true)
            }
        }
        if (!masterEnabled) return Result.success()

        val sessionFile = File(applicationContext.filesDir, "notif_session.json")
        if (!sessionFile.exists()) return Result.success()

        val cfg = runCatching { JSONObject(sessionFile.readText()) }.getOrNull() ?: return Result.success()
        val supabaseUrl = cfg.optString("supabase_url").trimEnd('/')
        val anonKey = cfg.optString("anon_key")
        var accessToken = cfg.optString("access_token")
        var refreshToken = cfg.optString("refresh_token")
        val userId = cfg.optString("user_id")
        if (supabaseUrl.isBlank() || anonKey.isBlank() || accessToken.isBlank() || userId.isBlank()) {
            return Result.success()
        }

        val primaryEndpoint = "$supabaseUrl/rest/v1/notifications?select=id,title,message,type,metadata,sender_user_id&user_id=eq.$userId&is_read=eq.false&order=created_at.desc&limit=10"
        var (code, body) = httpGet(primaryEndpoint, anonKey, accessToken)

        if (code == 401 && refreshToken.isNotBlank()) {
            val refreshed = refreshAccessToken(supabaseUrl, anonKey, refreshToken)
            if (refreshed != null) {
                accessToken = refreshed.first
                if (refreshed.second.isNotBlank()) refreshToken = refreshed.second
                cfg.put("access_token", accessToken)
                cfg.put("refresh_token", refreshToken)
                runCatching { sessionFile.writeText(cfg.toString()) }
                val retry = httpGet(primaryEndpoint, anonKey, accessToken)
                code = retry.first
                body = retry.second
            }
        }

        // Fallback to user_notifications if public.notifications is not yet migrated
        if (code !in 200..299) {
            val fallbackEndpoint = "$supabaseUrl/rest/v1/user_notifications?select=id,notification_type,preview_text,sender_user_id&recipient_user_id=eq.$userId&is_read=eq.false&order=created_at.desc&limit=10"
            val fallbackResp = httpGet(fallbackEndpoint, anonKey, accessToken)
            code = fallbackResp.first
            body = fallbackResp.second
        }

        if (code !in 200..299 || body.isBlank()) return Result.success()

        val rows = runCatching { JSONArray(body) }.getOrNull() ?: return Result.success()
        val seenFile = File(applicationContext.filesDir, "seen_notifs.json")
        val seen = LinkedHashSet<String>()
        if (seenFile.exists()) {
            runCatching {
                val arr = JSONArray(seenFile.readText())
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (id.isNotBlank()) seen.add(id)
                }
            }
        }

        var changed = false
        val nm = NotificationManagerCompat.from(applicationContext)

        for (i in rows.length() - 1 downTo 0) {
            val row = rows.optJSONObject(i) ?: continue
            val id = row.optString("id").trim()
            if (id.isBlank() || seen.contains(id)) continue
            seen.add(id)
            changed = true

            val type = row.optString("type").ifBlank { row.optString("notification_type", "message") }
            val allowed = when (type) {
                "message", "reply", "mention" -> messagesEnabled
                "update" -> updatesEnabled
                "reminder" -> remindersEnabled
                else -> true
            }
            if (!allowed) continue

            val title = row.optString("title").trim().ifEmpty {
                when (type) {
                    "reply" -> "Anime Universe • New Reply"
                    "mention" -> "Anime Universe • Tagged You"
                    "update" -> "Anime Universe • Update"
                    "reminder" -> "Anime Universe • Reminder"
                    else -> "Anime Universe • Notification"
                }
            }
            val preview = row.optString("message").trim()
                .ifEmpty { row.optString("preview_text").trim() }
                .ifEmpty { "You have a new notification in Anime Universe." }

            val metadata = row.optJSONObject("metadata")
            val targetScreen = metadata?.optString("target_screen")?.ifBlank { "notifications" } ?: "notifications"
            val topic = metadata?.optString("topic") ?: "general"

            val launchIntent = (applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                ?: Intent(applicationContext, MainActivity::class.java)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("notification_id", id)
                putExtra("target_screen", targetScreen)
                putExtra("topic", topic)
                putExtra("type", type)
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(applicationContext, id.hashCode(), launchIntent, pendingFlags)

            val notification = NotificationCompat.Builder(applicationContext, AnimeNotifScheduler.CHANNEL_ID)
                .setSmallIcon(applicationContext.applicationInfo.icon)
                .setContentTitle(title)
                .setContentText(preview)
                .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            runCatching { nm.notify(id.hashCode(), notification) }
        }

        if (changed) {
            val out = JSONArray()
            seen.toList().takeLast(400).forEach { out.put(it) }
            runCatching { seenFile.writeText(out.toString()) }
        }

        return Result.success()
    }

    private fun httpGet(urlStr: String, anonKey: String, accessToken: String): Pair<Int, String> {
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            code to text
        }.getOrDefault(0 to "")
    }

    private fun refreshAccessToken(supabaseUrl: String, anonKey: String, refreshToken: String): Pair<String, String>? {
        return runCatching {
            val conn = (URL("$supabaseUrl/auth/v1/token?grant_type=refresh_token").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Content-Type", "application/json")
            }
            val payload = JSONObject().put("refresh_token", refreshToken).toString()
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code !in 200..299) return null
            val json = JSONObject(text)
            val newAccess = json.optString("access_token")
            val newRefresh = json.optString("refresh_token")
            if (newAccess.isBlank()) null else (newAccess to newRefresh)
        }.getOrNull()
    }
}
