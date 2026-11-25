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
        // Create all notification channels immediately so Android Settings -> Notifications
        // lists every category cleanly on Android 8–16 without ever crashing startup.
        runCatching { AnimeNotifScheduler.ensureChannels(applicationContext) }
        runCatching { captureNotificationClickIntent(intent) }
        runCatching {
            addOnNewIntentListener { newIntent ->
                runCatching {
                    setIntent(newIntent)
                    captureNotificationClickIntent(newIntent)
                }
            }
        }
        runCatching {
            window?.decorView?.post {
                runCatching { AnimeNotifScheduler.schedulePeriodic(applicationContext) }
                runCatching { refreshFcmPushTokenIfAvailable() }
            }
        }.onFailure {
            runCatching { AnimeNotifScheduler.schedulePeriodic(applicationContext) }
            runCatching { refreshFcmPushTokenIfAvailable() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Immediately notify WebView if user just enabled/disabled notifications in Android Settings
        runCatching {
            val status = resolveCurrentOsPermissionStatus()
            notifyWebViewPermissionChanged(status)
        }
    }

    override fun onWebViewCreate(webView: WebView) {
        super.onWebViewCreate(webView)
        activeWebView = webView
        runCatching {
            webView.addJavascriptInterface(AnimeAndroidNotifBridge(this), "AnimeNativeBridge")
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE_POST_NOTIFICATIONS) {
            runCatching {
                val status = resolveCurrentOsPermissionStatus()
                notifyWebViewPermissionResult(status)
                notifyWebViewPermissionChanged(status)
            }
        }
    }

    fun resolveCurrentOsPermissionStatus(): String {
        return runCatching {
            AnimeNotifScheduler.ensureChannels(applicationContext)
            val areEnabledInManager = runCatching {
                NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
            }.getOrDefault(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val runtimeGranted = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (runtimeGranted && areEnabledInManager) {
                    return@runCatching "granted"
                }
                // If runtime permission is granted but user disabled the master toggle in Android Settings
                if (runtimeGranted && !areEnabledInManager) {
                    return@runCatching "denied"
                }

                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val requestedBefore = prefs.getBoolean(KEY_HAS_REQUESTED_ONCE, false)
                val showRationale = runCatching {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }.getOrDefault(false)
                if (!requestedBefore || showRationale) "prompt" else "denied"
            } else {
                if (areEnabledInManager) "granted" else "denied"
            }
        }.getOrDefault("prompt")
    }

    fun triggerNativePermissionRequest() {
        runOnUiThread {
            runCatching {
                AnimeNotifScheduler.ensureChannels(applicationContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val runtimeGranted = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (runtimeGranted) {
                        val status = resolveCurrentOsPermissionStatus()
                        notifyWebViewPermissionResult(status)
                        return@runCatching
                    }

                    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val requestedBefore = prefs.getBoolean(KEY_HAS_REQUESTED_ONCE, false)
                    val showRationale = runCatching {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }.getOrDefault(false)

                    // If Android permanently denied POST_NOTIFICATIONS, calling requestPermissions
                    // silently returns DENIED without showing any dialog. Return "denied" immediately
                    // so the app displays the 1-tap "Open System Settings" guide.
                    if (requestedBefore && !showRationale) {
                        notifyWebViewPermissionResult("denied")
                        return@runCatching
                    }

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
            }.onFailure {
                notifyWebViewPermissionResult("denied")
            }
        }
    }

    fun launchAppNotificationSettings(): Boolean {
        runOnUiThread {
            runCatching {
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
            }.onFailure {
                runCatching {
                    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(fallback)
                }
            }
        }
        return true
    }

    fun postDirectLocalNotification(title: String, body: String, type: String = "message"): Boolean {
        if (resolveCurrentOsPermissionStatus() != "granted") {
            return false
        }
        return runCatching {
            AnimeNotifScheduler.ensureChannels(applicationContext)
            val launchIntent = (applicationContext.packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(applicationContext, MainActivity::class.java)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("notification_id", "local_${System.currentTimeMillis()}")
                putExtra("target_screen", "notifications")
                putExtra("type", type)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val notifIdInt = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                notifIdInt,
                launchIntent,
                flags
            )
            val cleanTitle = title.trim().ifEmpty { "Anime Universe" }
            val cleanBody = body.trim().ifEmpty { "You have a new notification." }
            val channelId = AnimeNotifScheduler.channelForType(type)
            val smallIconRes = AnimeNotifScheduler.resolveSmallIcon(applicationContext)

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(smallIconRes)
                .setContentTitle(cleanTitle)
                .setContentText(cleanBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cleanBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(applicationContext).notify(notifIdInt, notification)
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

    private fun notifyWebViewPermissionChanged(status: String) {
        val safeStatus = status.replace("'", "")
        runOnUiThread {
            activeWebView?.evaluateJavascript(
                "window.__onAnimeNativeNotifPermChanged && window.__onAnimeNativeNotifPermChanged('$safeStatus');",
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
        return activity.postDirectLocalNotification(title, body, "message")
    }

    @JavascriptInterface
    fun sendLocalNotificationWithType(title: String, body: String, type: String): Boolean {
        return activity.postDirectLocalNotification(title, body, type)
    }
}
