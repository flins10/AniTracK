package com.animeuniverse.app

import android.content.Intent
import android.os.Bundle
import org.json.JSONObject
import java.io.File

class MainActivity : TauriActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure Android notification channel and background fallback worker are registered.
        // Note: We do NOT force the Android OS permission dialog immediately on launch here;
        // instead, the user sees the "Stay in the Loop!" first-time permission popup in-app,
        // and clicking "Enable Notifications" invokes the native Android POST_NOTIFICATIONS dialog.
        AnimeNotifScheduler.schedulePeriodic(applicationContext)
        captureNotificationClickIntent(intent)
        refreshFcmPushTokenIfAvailable()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureNotificationClickIntent(intent)
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

    /**
     * Retrieves the device FCM registration token via reflection if Firebase Messaging
     * is configured in the Android build, writing it to `device_push_token.json` so the
     * Tauri Rust bridge and Supabase `register-device` Edge Function can sync it automatically.
     */
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
