package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class AniListUserProfile(
    val id: Int,
    val name: String,
    val avatarUrl: String?
)

class AniListOAuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("anitrack_anilist_auth", Context.MODE_PRIVATE)

    var clientId: String
        get() = prefs.getString("anilist_client_id", DEFAULT_CLIENT_ID) ?: DEFAULT_CLIENT_ID
        set(value) = prefs.edit().putString("anilist_client_id", value.trim()).apply()

    var redirectUri: String
        get() = prefs.getString("anilist_redirect_uri", DEFAULT_REDIRECT_URI) ?: DEFAULT_REDIRECT_URI
        set(value) = prefs.edit().putString("anilist_redirect_uri", value.trim()).apply()

    val accessToken: String?
        get() = prefs.getString("anilist_access_token", null)

    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank()

    val userName: String?
        get() = prefs.getString("anilist_user_name", null)

    val userAvatar: String?
        get() = prefs.getString("anilist_user_avatar", null)

    val userId: Int
        get() = prefs.getInt("anilist_user_id", -1)

    fun getAuthorizationUrl(): String {
        return "https://anilist.co/api/v2/oauth/authorize?client_id=$clientId&response_type=token"
    }

    fun saveToken(token: String) {
        val cleanToken = token.trim()
            .removePrefix("Bearer ")
            .trim()
        prefs.edit()
            .putString("anilist_access_token", cleanToken)
            .apply()
    }

    fun saveUserProfile(id: Int, name: String, avatarUrl: String?) {
        prefs.edit()
            .putInt("anilist_user_id", id)
            .putString("anilist_user_name", name)
            .putString("anilist_user_avatar", avatarUrl)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .remove("anilist_access_token")
            .remove("anilist_user_id")
            .remove("anilist_user_name")
            .remove("anilist_user_avatar")
            .apply()
    }

    suspend fun fetchCurrentUserProfile(): Result<AniListUserProfile> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(Exception("Not logged in to AniList"))
        try {
            val client = OkHttpClient()
            val query = """
                query {
                    Viewer {
                        id
                        name
                        avatar {
                            large
                            medium
                        }
                    }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", query)
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val respStr = response.body?.string().orEmpty()
                val json = JSONObject(respStr)
                val viewer = json.getJSONObject("data").getJSONObject("Viewer")
                val id = viewer.getInt("id")
                val name = viewer.getString("name")
                val avatarObj = viewer.optJSONObject("avatar")
                val avatarUrl = avatarObj?.optString("large") ?: avatarObj?.optString("medium")

                saveUserProfile(id, name, avatarUrl)
                Result.success(AniListUserProfile(id, name, avatarUrl))
            } else {
                Result.failure(Exception("HTTP ${response.code}: Failed to fetch AniList profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAniListEntry(
        mediaId: Int,
        status: String?,
        progress: Int?,
        scoreRaw: Int?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(Exception("No AniList authorization token"))
        try {
            val client = OkHttpClient()
            val mutation = """
                mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int, ${'$'}scoreRaw: Int) {
                    SaveMediaListEntry (mediaId: ${'$'}mediaId, status: ${'$'}status, progress: ${'$'}progress, scoreRaw: ${'$'}scoreRaw) {
                        id
                        status
                        progress
                        score (format: POINT_100)
                    }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("mediaId", mediaId)
                if (!status.isNullOrBlank()) {
                    val apiStatus = when (status.uppercase()) {
                        "WATCHING", "CURRENT" -> "CURRENT"
                        "COMPLETED" -> "COMPLETED"
                        "PLAN_TO_WATCH", "PLANNING" -> "PLANNING"
                        "ON_HOLD", "PAUSED" -> "PAUSED"
                        "DROPPED" -> "DROPPED"
                        else -> "CURRENT"
                    }
                    put("status", apiStatus)
                }
                if (progress != null) put("progress", progress)
                if (scoreRaw != null && scoreRaw > 0) put("scoreRaw", scoreRaw * 10)
            }

            val payload = JSONObject().apply {
                put("query", mutation)
                put("variables", variables)
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code}: Unable to save list changes on AniList"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_CLIENT_ID = "24672"
        const val DEFAULT_REDIRECT_URI = "https://anilist.co/api/v2/oauth/pin"
    }
}
