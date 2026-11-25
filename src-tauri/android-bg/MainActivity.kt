package com.animeuniverse.app

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File

class MainActivity : TauriActivity() {
    companion object {
        private const val REQ_CODE_POST_NOTIFICATIONS = 7001
        private const val PREFS_NAME = "anime_universe_notif_bridge"
        private const val KEY_HAS_REQUESTED_ONCE = "has_requested_post_notifications_once"
    }

    private var activeWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnimeNotifScheduler.schedulePeriodic(applicationContext)
        captureNotificationClickIntent(intent)
        refreshFcmPushTokenIfAvailable()
    }

    override fun onWebViewCreate(webView: WebView) {
        super.onWebViewCreate(webView)
        activeWebView = webView
        runCatching {
            webView.addJavascriptInterface(AnimeAndroidNotifBridge(this), "AnimeNativeBridge")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureNotificationClickIntent(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE_POST_NOTIFICATIONS) {
            val status = resolveCurrentOsPermissionStatus()
            notifyWebViewPermissionResult(status)
        }
    }

    fun resolveCurrentOsPermissionStatus(): String {
        AnimeNotifScheduler.ensureChannel(applicationContext)
        val areEnabledInManager = runCatching {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        }.getOrDefault(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val runtimeGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (runtimeGranted && areEnabledInManager) {
                return "granted"
            }
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val requestedBefore = prefs.getBoolean(KEY_HAS_REQUESTED_ONCE, false)
            val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            return if (!requestedBefore || showRationale) {
                "prompt"
            } else {
                "denied"
            }
        } else {
            return if (areEnabledInManager) "granted" else "denied"
        }
    }

    fun triggerNativePermissionRequest() {
        runOnUiThread {
            AnimeNotifScheduler.ensureChannel(applicationContext)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val alreadyGranted = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (alreadyGranted) {
                    val status = resolveCurrentOsPermissionStatus()
                    notifyWebViewPermissionResult(status)
                    return@runOnUiThread
                }

                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_HAS_REQUESTED_ONCE, true).apply()

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_CODE_POST_NOTIFICATIONS
                )
            } else {
                val status = resolveCurrentOsPermissionStatus()
                notifyWebViewPermissionResult(status)
            }
        }
    }

    fun launchAppNotificationSettings(): Boolean {
        return runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            startActivity(intent)
            true
        }.getOrElse {
            runCatching {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(fallback)
                true
            }.getOrDefault(false)
        }
    }

    fun postDirectLocalNotification(title: String, body: String): Boolean {
        if (resolveCurrentOsPermissionStatus() != "granted") {
            return false
        }
        return runCatching {
            AnimeNotifScheduler.ensureChannel(applicationContext)
            val launchIntent = (applicationContext.packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(applicationContext, MainActivity::class.java)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("notification_id", "local_${System.currentTimeMillis()}")
                putExtra("target_screen", "notifications")
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                launchIntent,
                flags
            )
            val cleanTitle = title.trim().ifEmpty { "Anime Universe" }
            val cleanBody = body.trim().ifEmpty { "You have a new notification." }
            val notification = NotificationCompat.Builder(applicationContext, AnimeNotifScheduler.CHANNEL_ID)
                .setSmallIcon(applicationContext.applicationInfo.icon)
                .setContentTitle(cleanTitle)
                .setContentText(cleanBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cleanBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(applicationContext)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
            true
        }.getOrDefault(false)
    }

    private fun notifyWebViewPermissionResult(status: String) {
        val safeStatus = status.replace("'", "")
        runOnUiThread {
            activeWebView?.evaluateJavascript(
                "window.__onAnimeNativeNotifPermResult && window.__onAnimeNativeNotifPermResult('$safeStatus');",
                null
            )
        }
    }

    private fun captureNotificationClickIntent(incomingIntent: Intent?) {
        val extras = incomingIntent?.extras ?: return
        val notificationId = extras.getString("notification_id")
            ?: extras.getString("google.message_id")
            ?: return
        val targetScreen = extras.getString("target_screen") ?: "notifications"
        val topic = extras.getString("topic") ?: "general"
        val messageId = extras.getString("message_id") ?: ""
        val type = extras.getString("type") ?: "message"

        runCatching {
            val payload = JSONObject().apply {
                put("notification_id", notificationId)
                put("target_screen", targetScreen)
                put("topic", topic)
                put("message_id", messageId)
                put("type", type)
                put("clicked_at", System.currentTimeMillis())
            }
            val file = File(applicationContext.filesDir, "pending_notif_click.json")
            file.writeText(payload.toString())
        }
    }

    private fun refreshFcmPushTokenIfAvailable() {
        runCatching {
            val fcmClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging")
            val getInstanceMethod = fcmClass.getMethod("getInstance")
            val fcmInstance = getInstanceMethod.invoke(null) ?: return
            val getTokenMethod = fcmClass.getMethod("getToken")
            val taskObj = getTokenMethod.invoke(fcmInstance) ?: return

            val listenerClass = Class.forName("com.google.android.gms.tasks.OnCompleteListener")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "onComplete" && args != null && args.isNotEmpty()) {
                    val completedTask = args[0]
                    val isSuccessful = completedTask.javaClass.getMethod("isSuccessful").invoke(completedTask) as? Boolean ?: false
                    if (isSuccessful) {
                        val token = completedTask.javaClass.getMethod("getResult").invoke(completedTask) as? String
                        if (!token.isNullOrBlank()) {
                            val tokenJson = JSONObject().apply {
                                put("token", token.trim())
                                put("provider", "fcm")
                                put("updated_at", System.currentTimeMillis())
                            }
                            File(applicationContext.filesDir, "device_push_token.json").writeText(tokenJson.toString())
                        }
                    }
                }
                null
            }
            val addOnCompleteMethod = taskObj.javaClass.getMethod("addOnCompleteListener", listenerClass)
            addOnCompleteMethod.invoke(taskObj, proxy)
        }
    }
}

class AnimeAndroidNotifBridge(private val activity: MainActivity) {
    @JavascriptInterface
    fun getPermissionStatus(): String {
        return activity.resolveCurrentOsPermissionStatus()
    }

    @JavascriptInterface
    fun requestPermission() {
        activity.triggerNativePermissionRequest()
    }

    @JavascriptInterface
    fun openNotificationSettings(): Boolean {
        return activity.launchAppNotificationSettings()
    }

    @JavascriptInterface
    fun sendLocalNotification(title: String, body: String): Boolean {
        return activity.postDirectLocalNotification(title, body)
    }
}
