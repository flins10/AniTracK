package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Authentication manager for MyAnimeList (MAL).
 * Manages OAuth 2.0 PKCE flow, tokens, and direct username tracking.
 */
class MyAnimeListOAuthManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("anitrack_oauth_store", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    var customClientId: String?
        get() = prefs.getString("custom_client_id", null) ?: prefs.getString("enc_client_id", null)
        set(value) {
            prefs.edit().putString("custom_client_id", value?.trim()).remove("enc_client_id").commit()
        }

    var customRedirectUri: String?
        get() = prefs.getString("custom_redirect_uri", null) ?: prefs.getString("enc_redirect_uri", null)
        set(value) {
            prefs.edit().putString("custom_redirect_uri", value?.trim()).remove("enc_redirect_uri").commit()
        }

    val effectiveClientId: String
        get() = customClientId?.takeIf { it.isNotBlank() } ?: ""

    val effectiveRedirectUri: String
        get() = customRedirectUri?.takeIf { it.isNotBlank() } ?: MyAnimeListConfig.DEFAULT_REDIRECT_URI

    val hasConfiguredClientId: Boolean
        get() = !customClientId.isNullOrBlank()

    var connectedUsername: String?
        get() = prefs.getString("connected_username", null) ?: prefs.getString("enc_connected_username", null)
        set(value) {
            prefs.edit().putString("connected_username", value?.trim()).remove("enc_connected_username").commit()
        }

    var storedCodeVerifier: String?
        get() = prefs.getString("code_verifier", null) ?: prefs.getString("enc_code_verifier", null)
        private set(value) {
            prefs.edit().putString("code_verifier", value?.trim()).remove("enc_code_verifier").commit()
        }

    var accessToken: String?
        get() = prefs.getString("access_token", null) ?: prefs.getString("enc_access_token", null)
        private set(value) {
            prefs.edit().putString("access_token", value?.trim()).remove("enc_access_token").commit()
        }

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null) ?: prefs.getString("enc_refresh_token", null)
        private set(value) {
            prefs.edit().putString("refresh_token", value?.trim()).remove("enc_refresh_token").commit()
        }

    var tokenExpiresAt: Long
        get() = prefs.getLong("expires_at", 0L)
        private set(value) = prefs.edit().putLong("expires_at", value).commit().let { }

    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank() || !connectedUsername.isNullOrBlank()

    val isOAuthConnected: Boolean
        get() = !accessToken.isNullOrBlank()

    /**
     * Saves user's custom Client ID and redirect URI.
     */
    fun saveClientCredentials(clientId: String, redirectUri: String? = null) {
        customClientId = clientId.trim()
        if (!redirectUri.isNullOrBlank()) {
            customRedirectUri = redirectUri.trim()
        }
    }

    /**
     * Saves connected username for public library synchronization without OAuth token.
     */
    fun connectWithUsername(username: String) {
        connectedUsername = username.trim()
    }

    /**
     * Generates a 64-character URL-safe alphanumeric PKCE code verifier conforming to MAL OAuth2.
     */
    fun generateCodeVerifier(): String {
        val allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        val verifier = (1..64).map { allowedChars[random.nextInt(allowedChars.length)] }.joinToString("")
        storedCodeVerifier = verifier
        return verifier
    }

    /**
     * Builds the complete OAuth 2.0 Authorization URL with PKCE parameters.
     */
    fun buildOAuthAuthorizationUrl(forceNewVerifier: Boolean = true): String {
        val verifier = if (forceNewVerifier || storedCodeVerifier.isNullOrBlank()) {
            generateCodeVerifier()
        } else {
            storedCodeVerifier!!
        }
        val clientIdToUse = effectiveClientId
        return Uri.parse(MyAnimeListConfig.AUTH_URL).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientIdToUse)
            .appendQueryParameter("code_challenge", verifier)
            .appendQueryParameter("code_challenge_method", "plain")
            .appendQueryParameter("redirect_uri", effectiveRedirectUri)
            .appendQueryParameter("state", "anitrack_${System.currentTimeMillis()}")
            .build()
            .toString()
    }

    /**
     * Starts the MyAnimeList OAuth 2.0 Authorization flow via external browser / Custom Tab redirect.
     */
    fun startOAuthFlow(): Boolean {
        if (!hasConfiguredClientId) {
            return false
        }
        val authUrl = buildOAuthAuthorizationUrl(forceNewVerifier = true)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("MALOAuthManager", "Failed to launch browser for OAuth: ${e.message}")
            false
        }
    }

    /**
     * Extracts authorization code from redirect deep link URI or raw code string.
     */
    fun extractAuthorizationCode(input: String): String {
        val trimmed = input.trim()
        return try {
            if (trimmed.contains("code=")) {
                val parsed = Uri.parse(trimmed)
                val code = parsed.getQueryParameter("code")
                if (!code.isNullOrBlank()) {
                    code.substringBefore("&").substringBefore("#")
                } else {
                    trimmed.substringAfter("code=").substringBefore("&").substringBefore("#")
                }
            } else {
                trimmed.substringBefore("&").substringBefore("#")
            }
        } catch (e: Exception) {
            trimmed.substringAfter("code=").substringBefore("&").substringBefore("#")
        }
    }

    /**
     * Exchanges authorization code for OAuth 2.0 access & refresh tokens with PKCE verifier.
     */
    suspend fun handleOAuthCallback(authorizationCodeOrUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanCode = extractAuthorizationCode(authorizationCodeOrUrl)
            if (cleanCode.isBlank()) {
                return@withContext Result.failure(Exception("No authorization code found in redirect URL."))
            }

            val verifier = storedCodeVerifier
            if (verifier.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Authentication session expired. Please tap 'Connect with MyAnimeList' to try again."))
            }

            val clientIdToUse = effectiveClientId
            if (clientIdToUse.isBlank()) {
                return@withContext Result.failure(Exception("No Client ID configured. Please enter your MAL Client ID."))
            }

            val formBody = FormBody.Builder()
                .add("client_id", clientIdToUse)
                .add("code", cleanCode)
                .add("code_verifier", verifier)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", effectiveRedirectUri)
                .build()

            val request = Request.Builder()
                .url(MyAnimeListConfig.TOKEN_URL)
                .header("User-Agent", "AniTrack-Android/1.0")
                .header("Accept", "application/json")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val str = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(str)
                    val accToken = json.getString("access_token")
                    val refToken = json.optString("refresh_token", "")
                    val expiresIn = json.optLong("expires_in", 2592000L) // Default 30 days
                    accessToken = accToken
                    refreshToken = refToken
                    tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                    Result.success(accToken)
                } else {
                    val errorMsg = try {
                        val json = JSONObject(str)
                        val err = json.optString("error", "Error")
                        val msg = json.optString("message", "")
                        val hint = json.optString("hint", "")
                        listOf(err, msg, hint).filter { it.isNotBlank() }.joinToString(": ")
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $str"
                    }
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Automatically refreshes the OAuth access token if expired or near expiration.
     */
    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        val currentAccToken = accessToken
        val refToken = refreshToken
        if (currentAccToken.isNullOrBlank()) return@withContext null

        // If token expires in less than 5 minutes, refresh token
        if (System.currentTimeMillis() + 300_000 >= tokenExpiresAt && !refToken.isNullOrBlank()) {
            refreshOAuthToken(refToken)
        } else {
            currentAccToken
        }
    }

    private suspend fun refreshOAuthToken(currentRefreshToken: String): String? {
        val clientIdToUse = effectiveClientId
        if (clientIdToUse.isBlank()) return null

        return try {
            val formBody = FormBody.Builder()
                .add("client_id", clientIdToUse)
                .add("grant_type", "refresh_token")
                .add("refresh_token", currentRefreshToken)
                .build()

            val request = Request.Builder()
                .url(MyAnimeListConfig.TOKEN_URL)
                .header("User-Agent", "AniTrack-Android/1.0")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val str = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(str)
                    val accToken = json.getString("access_token")
                    val newRefToken = json.optString("refresh_token", currentRefreshToken)
                    val expiresIn = json.optLong("expires_in", 2592000L)
                    accessToken = accToken
                    refreshToken = newRefToken
                    tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                    accToken
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Logout and clear local encrypted credentials and tokens.
     */
    fun logout() {
        prefs.edit().clear().commit()
    }
}
