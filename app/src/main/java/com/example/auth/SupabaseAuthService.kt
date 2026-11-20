package com.example.auth

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.config.AppConfig
import com.example.model.AuthSession
import com.example.util.ErrorSanitizer
import com.example.util.SecureStorageCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

private val Context.authDataStore by preferencesDataStore(name = "anime_universe_auth_session")

sealed class SignUpOutcome {
    data class Authenticated(val session: AuthSession) : SignUpOutcome()
    data class EmailVerificationRequired(val email: String, val message: String) : SignUpOutcome()
}

sealed class AuthCallbackOutcome {
    data class VerifiedSession(val session: AuthSession, val type: String) : AuthCallbackOutcome()
    data class PasswordRecoverySession(val session: AuthSession) : AuthCallbackOutcome()
}

class SupabaseAuthService(context: Context) {
    companion object {
        const val AUTH_REDIRECT_URI = "animeuniverse://auth-callback"
        const val EMAIL_COOLDOWN_MILLIS = 5L * 60L * 1000L
        const val USERNAME_COOLDOWN_MILLIS = 24L * 60L * 60L * 1000L

        fun sha256Hex(raw: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(raw.trim().lowercase().toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun formatRemainingMinutes(seconds: Long): String {
            val mins = ((seconds + 59L) / 60L).coerceAtLeast(1L)
            return if (mins == 1L) "1 minute" else "$mins minutes"
        }

        fun buildGoogleOAuthUrl(
            baseUrl: String = AppConfig.supabaseUrl,
            redirectUri: String = AUTH_REDIRECT_URI
        ): String {
            val cleanBase = baseUrl.trim().trimEnd('/')
            val encodedRedirect = URLEncoder.encode(redirectUri.trim(), "UTF-8")
            return "$cleanBase/auth/v1/authorize?provider=google&redirect_to=$encodedRedirect"
        }
    }

    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val fastPrefs = appContext.getSharedPreferences("anime_universe_fast_auth", Context.MODE_PRIVATE)

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val EXPIRES_AT = longPreferencesKey("expires_at")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PENDING_USERNAME = stringPreferencesKey("pending_username")
        val PENDING_AVATAR_KEY = stringPreferencesKey("pending_avatar_key")
        val DELETED_EMAIL_COOLDOWNS = stringPreferencesKey("deleted_email_cooldowns")
        val DELETED_USERNAME_COOLDOWNS = stringPreferencesKey("deleted_username_cooldowns")
    }

    fun getFastCachedSession(): AuthSession? {
        return try {
            val storedToken = fastPrefs.getString("access_token", null) ?: return null
            val token = SecureStorageCodec.decrypt(storedToken)
            if (token.isBlank()) return null
            val refresh = SecureStorageCodec.decrypt(fastPrefs.getString("refresh_token", "").orEmpty())
            val userId = fastPrefs.getString("user_id", null) ?: return null
            val email = SecureStorageCodec.decrypt(fastPrefs.getString("email", "").orEmpty())
            val expiresAt = fastPrefs.getLong("expires_at", 0L)
            val onboardingCompleted = fastPrefs.getBoolean("onboarding_completed", true)
            val username = fastPrefs.getString("pending_username", "").orEmpty()
            val avatarKey = fastPrefs.getString("pending_avatar_key", "avatar_1").orEmpty().ifBlank { "avatar_1" }
            AuthSession(
                accessToken = token,
                refreshToken = refresh,
                userId = userId,
                email = email,
                expiresAtEpochSec = expiresAt,
                pendingUsername = username,
                pendingAvatarKey = avatarKey,
                onboardingCompleted = onboardingCompleted
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun mirrorToFastPrefs(session: AuthSession) {
        try {
            val existingUsername = fastPrefs.getString("pending_username", "").orEmpty()
            val existingAvatar = fastPrefs.getString("pending_avatar_key", "avatar_1").orEmpty()
            val finalUsername = session.pendingUsername.ifBlank { existingUsername }
            val finalAvatar = session.pendingAvatarKey.ifBlank { existingAvatar.ifBlank { "avatar_1" } }
            fastPrefs.edit()
                .putString("access_token", SecureStorageCodec.encrypt(session.accessToken))
                .putString("refresh_token", SecureStorageCodec.encrypt(session.refreshToken))
                .putString("user_id", session.userId)
                .putString("email", SecureStorageCodec.encrypt(session.email))
                .putLong("expires_at", session.expiresAtEpochSec)
                .putBoolean("onboarding_completed", session.onboardingCompleted)
                .putString("pending_username", finalUsername)
                .putString("pending_avatar_key", finalAvatar)
                .apply()
        } catch (_: Exception) {
        }
    }

    suspend fun cacheProfileSnapshot(username: String, avatarKey: String) {
        val cleanName = username.trim()
        val cleanAvatar = avatarKey.trim().ifBlank { "avatar_1" }
        try {
            fastPrefs.edit()
                .putString("pending_username", cleanName)
                .putString("pending_avatar_key", cleanAvatar)
                .apply()
        } catch (_: Exception) {
        }
        appContext.authDataStore.edit { prefs ->
            if (cleanName.isNotBlank()) prefs[Keys.PENDING_USERNAME] = cleanName
            prefs[Keys.PENDING_AVATAR_KEY] = cleanAvatar
        }
    }

    suspend fun getSavedSession(): AuthSession? {
        getFastCachedSession()?.let { return it }
        val prefs = appContext.authDataStore.data.first()
        val storedToken = prefs[Keys.ACCESS_TOKEN] ?: return null
        val token = SecureStorageCodec.decrypt(storedToken)
        if (token.isBlank()) return null
        val refresh = SecureStorageCodec.decrypt(prefs[Keys.REFRESH_TOKEN].orEmpty())
        val userId = prefs[Keys.USER_ID] ?: return null
        val email = SecureStorageCodec.decrypt(prefs[Keys.EMAIL].orEmpty())
        val expiresAt = prefs[Keys.EXPIRES_AT] ?: 0L
        val onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: true
        val username = prefs[Keys.PENDING_USERNAME].orEmpty()
        val avatarKey = prefs[Keys.PENDING_AVATAR_KEY].orEmpty().ifBlank { "avatar_1" }
        val session = AuthSession(
            accessToken = token,
            refreshToken = refresh,
            userId = userId,
            email = email,
            expiresAtEpochSec = expiresAt,
            pendingUsername = username,
            pendingAvatarKey = avatarKey,
            onboardingCompleted = onboardingCompleted
        )
        mirrorToFastPrefs(session)
        return session
    }

    internal suspend fun saveSession(session: AuthSession) {
        mirrorToFastPrefs(session)
        appContext.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = SecureStorageCodec.encrypt(session.accessToken)
            prefs[Keys.REFRESH_TOKEN] = SecureStorageCodec.encrypt(session.refreshToken)
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.EMAIL] = SecureStorageCodec.encrypt(session.email)
            prefs[Keys.EXPIRES_AT] = session.expiresAtEpochSec
            prefs[Keys.ONBOARDING_COMPLETED] = session.onboardingCompleted
            if (session.pendingUsername.isNotBlank()) {
                prefs[Keys.PENDING_USERNAME] = session.pendingUsername
            }
            if (session.pendingAvatarKey.isNotBlank()) {
                prefs[Keys.PENDING_AVATAR_KEY] = session.pendingAvatarKey
            }
        }
    }

    suspend fun clearSession() {
        try {
            fastPrefs.edit().clear().apply()
        } catch (_: Exception) {
        }
        appContext.authDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.EXPIRES_AT)
            prefs.remove(Keys.ONBOARDING_COMPLETED)
            prefs.remove(Keys.PENDING_USERNAME)
            prefs.remove(Keys.PENDING_AVATAR_KEY)
        }
    }

    suspend fun recordDeletionCooldowns(
        email: String,
        username: String,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val cleanEmail = email.trim().lowercase()
        val cleanUser = username.trim().lowercase()
        appContext.authDataStore.edit { prefs ->
            if (cleanEmail.contains("@")) {
                val map = parseCooldownMap(prefs[Keys.DELETED_EMAIL_COOLDOWNS], nowMs)
                map[sha256Hex(cleanEmail)] = nowMs + EMAIL_COOLDOWN_MILLIS
                prefs[Keys.DELETED_EMAIL_COOLDOWNS] = encodeCooldownMap(map)
            }
            if (cleanUser.length >= 2 && !cleanUser.startsWith("traveler_")) {
                val map = parseCooldownMap(prefs[Keys.DELETED_USERNAME_COOLDOWNS], nowMs)
                map[sha256Hex(cleanUser)] = nowMs + USERNAME_COOLDOWN_MILLIS
                prefs[Keys.DELETED_USERNAME_COOLDOWNS] = encodeCooldownMap(map)
            }
        }
    }

    suspend fun checkLocalEmailCooldownSeconds(
        email: String,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val clean = email.trim().lowercase()
        if (!clean.contains("@")) return 0L
        val prefs = appContext.authDataStore.data.first()
        val map = parseCooldownMap(prefs[Keys.DELETED_EMAIL_COOLDOWNS], nowMs)
        val until = map[sha256Hex(clean)] ?: return 0L
        return if (until > nowMs) ((until - nowMs + 999L) / 1000L) else 0L
    }

    suspend fun checkLocalUsernameCooldownSeconds(
        username: String,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val clean = username.trim().lowercase()
        if (clean.length < 2) return 0L
        val prefs = appContext.authDataStore.data.first()
        val map = parseCooldownMap(prefs[Keys.DELETED_USERNAME_COOLDOWNS], nowMs)
        val until = map[sha256Hex(clean)] ?: return 0L
        return if (until > nowMs) ((until - nowMs + 999L) / 1000L) else 0L
    }

    private fun parseCooldownMap(raw: String?, nowMs: Long): MutableMap<String, Long> {
        val result = mutableMapOf<String, Long>()
        if (raw.isNullOrBlank()) return result
        runCatching {
            val obj = JSONObject(raw)
            obj.keys().forEach { key ->
                val expiry = obj.optLong(key, 0L)
                if (expiry > nowMs) result[key] = expiry
            }
        }
        return result
    }

    private fun encodeCooldownMap(map: Map<String, Long>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    /**
     * Step 1 of Registration: Email & Password only.
     * Marks `onboarding_completed = false` so after clicking the confirmation link
     * (and being redirected back into the app), the user enters Onboarding Page 1 (Username)
     * followed by Page 2 (Royal PFP Selection).
     */
    suspend fun signUpEmailOnly(
        email: String,
        password: String
    ): Result<SignUpOutcome> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Cloud account service is currently unavailable. Please try again later.")
            )
        }
        try {
            val userMeta = JSONObject()
                .put("onboarding_completed", false)

            val payload = JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .put("data", userMeta)

            val encodedRedirect = URLEncoder.encode(AUTH_REDIRECT_URI, "UTF-8")
            val request = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/signup?redirect_to=$encodedRedirect")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parseErrorMessage(body, "Unable to create your account right now. Please try again."))
                    )
                }
                val json = JSONObject(body)
                val sessionObj = json.optJSONObject("session") ?: json
                val accessToken = sessionObj.optString("access_token", "")
                if (accessToken.isNotBlank()) {
                    val session = parseSession(json, email.trim()).copy(onboardingCompleted = false)
                    saveSession(session)
                    Result.success(SignUpOutcome.Authenticated(session))
                } else {
                    val identities = json.optJSONArray("identities")
                    if (identities != null && identities.length() == 0) {
                        return@withContext Result.failure(
                            Exception("An account with this email already exists. Please sign in instead.")
                        )
                    }
                    Result.success(
                        SignUpOutcome.EmailVerificationRequired(
                            email = email.trim(),
                            message = "We've sent a verification link to ${email.trim()}. Open the link in your email to return here and set up your profile."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to create account right now. Check your connection and try again.")))
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Cloud account service is currently unavailable.")
            )
        }
        try {
            val payload = JSONObject()
                .put("email", email.trim())
                .put("password", password)

            val request = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/token?grant_type=password")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(parseErrorMessage(body, "Incorrect email/username or password.")))
                }
                val json = JSONObject(body)
                val userObj = json.optJSONObject("user") ?: json.optJSONObject("session")?.optJSONObject("user")
                val meta = userObj?.optJSONObject("user_metadata")
                if (meta?.optBoolean("account_deleted", false) == true) {
                    return@withContext Result.failure(
                        Exception("This account has been permanently deleted.")
                    )
                }
                val session = parseSession(json, email.trim())
                saveSession(session)
                Result.success(session)
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to sign in right now. Please try again.")))
        }
    }

    suspend fun verifyPassword(session: AuthSession, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Cloud account service is currently unavailable.")
            )
        }
        if (password.isBlank()) {
            return@withContext Result.failure(Exception("Enter your password to verify."))
        }
        try {
            var targetEmail = session.email.trim()
            if (targetEmail.isBlank()) {
                val userReq = Request.Builder()
                    .url("${AppConfig.supabaseUrl}/auth/v1/user")
                    .addHeader("apikey", AppConfig.supabaseAnonKey)
                    .addHeader("Authorization", "Bearer ${session.accessToken}")
                    .get()
                    .build()
                client.newCall(userReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        targetEmail = JSONObject(resp.body?.string().orEmpty()).optString("email", "").trim()
                    }
                }
            }
            if (targetEmail.isBlank()) {
                return@withContext Result.failure(Exception("Unable to verify account credentials right now."))
            }

            val payload = JSONObject()
                .put("email", targetEmail)
                .put("password", password)

            val request = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/token?grant_type=password")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Incorrect password. Please try again."))
                }
                val verified = parseSession(JSONObject(body), targetEmail)
                Result.success(verified)
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to verify password right now.")))
        }
    }

    suspend fun refreshSessionIfNeeded(current: AuthSession): Result<AuthSession> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis() / 1000L
        if (current.expiresAtEpochSec > now + 60L) {
            return@withContext Result.success(current)
        }
        if (current.refreshToken.isBlank() || !AppConfig.isSupabaseConfigured) {
            return@withContext Result.success(current)
        }
        try {
            val payload = JSONObject().put("refresh_token", current.refreshToken)
            val request = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (response.code == 400 || response.code == 401) {
                        return@withContext Result.failure(Exception("Your session expired. Please sign in again."))
                    }
                    return@withContext Result.success(current)
                }
                val json = JSONObject(body)
                val parsed = parseSession(json, current.email)
                val refreshed = parsed.copy(
                    pendingUsername = parsed.pendingUsername.ifBlank { current.pendingUsername },
                    pendingAvatarKey = parsed.pendingAvatarKey.ifBlank { current.pendingAvatarKey }
                )
                saveSession(refreshed)
                Result.success(refreshed)
            }
        } catch (_: Exception) {
            Result.success(current)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<String> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Cloud account service is currently unavailable.")
            )
        }
        try {
            val encodedRedirect = URLEncoder.encode(AUTH_REDIRECT_URI, "UTF-8")
            val payload = JSONObject().put("email", email.trim())
            val request = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/recover?redirect_to=$encodedRedirect")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(parseErrorMessage(body, "Unable to send password reset link right now.")))
                }
                Result.success("Password reset link sent to ${email.trim()}.")
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to send password reset link right now.")))
        }
    }

    suspend fun resendVerificationEmail(email: String): Result<String> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(IllegalStateException("Cloud account service is currently unavailable."))
        }
        try {
            val encodedRedirect = URLEncoder.encode(AUTH_REDIRECT_URI, "UTF-8")
            val payload = JSONObject()
                .put("type", "signup")
                .put("email", email.trim())
            val request = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/resend?redirect_to=$encodedRedirect")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(parseErrorMessage(body, "Unable to resend verification link right now.")))
                }
                Result.success("Verification code resent to ${email.trim()}. Check your inbox and spam folder.")
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to resend verification link right now.")))
        }
    }

    /**
     * Verifies a 6-digit OTP token (from `{{ .Token }}` in Supabase Email Templates) for either
     * `signup` (email confirmation) or `recovery` (password reset).
     */
    suspend fun verifyEmailOtp(
        email: String,
        otpCode: String,
        type: String = "signup"
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(IllegalStateException("Cloud account service is currently unavailable."))
        }
        val cleanEmail = email.trim()
        val cleanOtp = otpCode.trim().replace(" ", "")
        if (cleanEmail.isBlank() || cleanOtp.length !in 6..8) {
            return@withContext Result.failure(Exception("Enter the verification code from your email."))
        }
        try {
            val payload = JSONObject()
                .put("email", cleanEmail)
                .put("token", cleanOtp)
                .put("type", type)

            val request = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/verify")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(parseErrorMessage(body, "Invalid or expired verification code. Please try again."))
                    )
                }
                val json = JSONObject(body)
                val session = parseSession(json, cleanEmail).let { parsed ->
                    if (type.equals("signup", ignoreCase = true)) parsed.copy(onboardingCompleted = false) else parsed
                }
                saveSession(session)
                Result.success(session)
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to verify code right now. Please try again.")))
        }
    }

    suspend fun updateAuthUserMetadata(
        session: AuthSession,
        username: String,
        avatarKey: String,
        onboardingCompleted: Boolean = true
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.success(
                session.copy(
                    pendingUsername = username.trim(),
                    pendingAvatarKey = avatarKey,
                    onboardingCompleted = onboardingCompleted
                )
            )
        }
        try {
            val meta = JSONObject()
                .put("username", username.trim())
                .put("avatar_key", avatarKey)
                .put("onboarding_completed", onboardingCompleted)
            val payload = JSONObject().put("data", meta)

            val request = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/user")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Content-Type", "application/json")
                .put(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val updated = session.copy(
                    pendingUsername = username.trim(),
                    pendingAvatarKey = avatarKey,
                    onboardingCompleted = onboardingCompleted
                )
                if (response.isSuccessful) {
                    saveSession(updated)
                    Result.success(updated)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(Exception(parseErrorMessage(body, "Unable to finalize your account profile right now.")))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to finalize your account profile right now.")))
        }
    }

    suspend fun handleAuthCallbackUri(uri: Uri): Result<AuthCallbackOutcome> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(IllegalStateException("Cloud account service is currently unavailable."))
        }
        try {
            val fragmentParams = parseKeyValueParams(uri.fragment)
            val errorDesc = fragmentParams["error_description"]
                ?: uri.getQueryParameter("error_description")
                ?: fragmentParams["error"]
                ?: uri.getQueryParameter("error")
            if (!errorDesc.isNullOrBlank()) {
                return@withContext Result.failure(
                    Exception(ErrorSanitizer.sanitize(errorDesc.replace("+", " "), "The verification link has expired or was already used."))
                )
            }

            val type = fragmentParams["type"] ?: uri.getQueryParameter("type") ?: "signup"
            var accessToken = fragmentParams["access_token"] ?: uri.getQueryParameter("access_token")
            var refreshToken = fragmentParams["refresh_token"] ?: uri.getQueryParameter("refresh_token") ?: ""
            var expiresIn = (fragmentParams["expires_in"] ?: uri.getQueryParameter("expires_in"))?.toLongOrNull() ?: 3600L

            val tokenHash = fragmentParams["token_hash"]
                ?: uri.getQueryParameter("token_hash")
                ?: fragmentParams["token"]
                ?: uri.getQueryParameter("token")

            if (accessToken.isNullOrBlank() && !tokenHash.isNullOrBlank()) {
                val verifyPayload = JSONObject()
                    .put("type", if (type.equals("recovery", ignoreCase = true)) "recovery" else "signup")
                    .put("token_hash", tokenHash)
                val verifyReq = Request.Builder()
                    .url("${AppConfig.supabaseUrl}/auth/v1/verify")
                    .addHeader("apikey", AppConfig.supabaseAnonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(verifyPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(verifyReq).execute().use { verifyResp ->
                    val verifyBody = verifyResp.body?.string().orEmpty()
                    if (verifyResp.isSuccessful && verifyBody.trim().startsWith("{")) {
                        val verifyJson = JSONObject(verifyBody)
                        val sessionObj = verifyJson.optJSONObject("session") ?: verifyJson
                        accessToken = sessionObj.optString("access_token").takeIf { it.isNotBlank() }
                        refreshToken = sessionObj.optString("refresh_token", refreshToken)
                        expiresIn = sessionObj.optLong("expires_in", expiresIn)
                    }
                }

                if (accessToken.isNullOrBlank() && uri.scheme?.startsWith("http") == true) {
                    runCatching {
                        val noRedirectClient = client.newBuilder()
                            .followRedirects(false)
                            .followSslRedirects(false)
                            .build()
                        val getReq = Request.Builder().url(uri.toString()).get().build()
                        noRedirectClient.newCall(getReq).execute().use { resp ->
                            val location = resp.header("Location")
                            if (!location.isNullOrBlank()) {
                                val redirectedUri = Uri.parse(location)
                                val locFrag = parseKeyValueParams(redirectedUri.fragment)
                                accessToken = locFrag["access_token"] ?: redirectedUri.getQueryParameter("access_token")
                                refreshToken = locFrag["refresh_token"] ?: redirectedUri.getQueryParameter("refresh_token") ?: refreshToken
                                expiresIn = (locFrag["expires_in"] ?: redirectedUri.getQueryParameter("expires_in"))?.toLongOrNull() ?: expiresIn
                            }
                        }
                    }
                }
            }

            if (accessToken.isNullOrBlank()) {
                return@withContext Result.failure(
                    Exception("Your email is confirmed! Tap \"I've Confirmed — Continue\" or sign in below.")
                )
            }

            val userReq = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/user")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val userObj = client.newCall(userReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseErrorMessage(body, "Unable to complete verification. Please try signing in.")))
                }
                JSONObject(body)
            }
            val meta = userObj.optJSONObject("user_metadata")
            val now = System.currentTimeMillis() / 1000L
            val session = AuthSession(
                accessToken = accessToken!!,
                refreshToken = refreshToken,
                userId = userObj.optString("id", ""),
                email = userObj.optString("email", ""),
                expiresAtEpochSec = now + expiresIn,
                pendingUsername = meta?.optString("username", "").orEmpty(),
                pendingInviteCode = meta?.optString("invite_code", "").orEmpty(),
                pendingAvatarKey = meta?.optString("avatar_key", "").orEmpty(),
                onboardingCompleted = meta?.optBoolean("onboarding_completed", true) ?: true
            )
            saveSession(session)
            if (type.equals("recovery", ignoreCase = true)) {
                Result.success(AuthCallbackOutcome.PasswordRecoverySession(session))
            } else {
                Result.success(AuthCallbackOutcome.VerifiedSession(session, type))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to process verification link. Try signing in below.")))
        }
    }

    suspend fun updatePassword(session: AuthSession, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(IllegalStateException("Cloud account service is currently unavailable."))
        }
        try {
            val payload = JSONObject().put("password", newPassword)
            val request = Request.Builder()
                .url("${AppConfig.supabaseUrl}/auth/v1/user")
                .addHeader("apikey", AppConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Content-Type", "application/json")
                .put(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception(parseErrorMessage(body, "Unable to update password right now.")))
                } else {
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to update password right now.")))
        }
    }

    private fun parseKeyValueParams(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        raw.split("&").forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val k = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[k] = v
            }
        }
        return map
    }

    suspend fun signOut(session: AuthSession?) = withContext(Dispatchers.IO) {
        if (session != null && AppConfig.isSupabaseConfigured) {
            try {
                val request = Request.Builder()
                    .url("${AppConfig.supabaseUrl}/auth/v1/logout")
                    .addHeader("apikey", AppConfig.supabaseAnonKey)
                    .addHeader("Authorization", "Bearer ${session.accessToken}")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().close()
            } catch (_: Exception) {
            }
        }
        clearSession()
    }

    private fun parseSession(json: JSONObject, fallbackEmail: String): AuthSession {
        val sessionObj = json.optJSONObject("session") ?: json
        val userObj = json.optJSONObject("user") ?: sessionObj.optJSONObject("user")
        val meta = userObj?.optJSONObject("user_metadata")
        val expiresIn = sessionObj.optLong("expires_in", 3600L)
        val now = System.currentTimeMillis() / 1000L
        return AuthSession(
            accessToken = sessionObj.getString("access_token"),
            refreshToken = sessionObj.optString("refresh_token", ""),
            userId = userObj?.optString("id", "") ?: "",
            email = userObj?.optString("email", fallbackEmail) ?: fallbackEmail,
            expiresAtEpochSec = now + expiresIn,
            pendingUsername = meta?.optString("username", "").orEmpty(),
            pendingInviteCode = meta?.optString("invite_code", "").orEmpty(),
            pendingAvatarKey = meta?.optString("avatar_key", "").orEmpty(),
            onboardingCompleted = meta?.optBoolean("onboarding_completed", true) ?: true
        )
    }

    private fun parseErrorMessage(body: String, fallback: String): String {
        val extracted = try {
            val obj = JSONObject(body)
            obj.optString("msg").takeIf { it.isNotBlank() }
                ?: obj.optString("error_description").takeIf { it.isNotBlank() }
                ?: obj.optString("message").takeIf { it.isNotBlank() }
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
        return ErrorSanitizer.sanitize(extracted, fallback)
    }
}
