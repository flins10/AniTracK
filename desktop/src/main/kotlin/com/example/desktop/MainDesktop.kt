package com.example.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.skia.Image as SkiaImage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// ── Exact Anime Universe Palettes (Obsidian & Champagne Gold + Gallery Porcelain & Bronze) ──
private val ObsidianNight = Color(0xFF0C0E14)
private val ObsidianSurface = Color(0xFF141821)
private val ObsidianElevated = Color(0xFF1C212E)
private val ObsidianGlass = Color(0xFF232938)
private val ChampagneGold = Color(0xFFE6C586)
private val WarmAmber = Color(0xFFD4A359)
private val SakuraCoral = Color(0xFFE57373)
private val JadeSuccess = Color(0xFF56C288)
private val AzureInfo = Color(0xFF64B5F6)
private val IvoryText = Color(0xFFF3F4F8)
private val SlateSecondary = Color(0xFFA0A8B8)
private val MutedZinc = Color(0xFF687082)
private val HairlineDark = Color(0xFF262C3C)

private val PorcelainBg = Color(0xFFF5F6FA)
private val PorcelainSurface = Color(0xFFFFFFFF)
private val PorcelainElevated = Color(0xFFEBEEF5)
private val BronzeAccent = Color(0xFF9A670E)
private val InkText = Color(0xFF131620)
private val SlateLightSecondary = Color(0xFF576072)
private val HairlineLight = Color(0xFFE1E5F0)

data class BuiltInAvatar(
    val id: String,
    val label: String,
    val glyph: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

val BUILT_IN_AVATARS = listOf(
    BuiltInAvatar("avatar_1", "Crimson Wolf", "狼", Color(0xFFE57373), Color(0xFF3A1C24)),
    BuiltInAvatar("avatar_2", "Azure Sovereign", "蒼", Color(0xFF64B5F6), Color(0xFF16293D)),
    BuiltInAvatar("avatar_3", "Amethyst Dragon", "龍", Color(0xFFB388FF), Color(0xFF281A42)),
    BuiltInAvatar("avatar_4", "Frost Empress", "雪", Color(0xFF4DD0E1), Color(0xFF123238)),
    BuiltInAvatar("avatar_5", "Emerald Flora", "花", Color(0xFF81C784), Color(0xFF1B3320)),
    BuiltInAvatar("avatar_6", "Amber Scholar", "星", Color(0xFFFFB74D), Color(0xFF3B2812)),
    BuiltInAvatar("avatar_7", "Autumn Vanguard", "焔", Color(0xFFFF8A65), Color(0xFF3D1E16)),
    BuiltInAvatar("avatar_8", "Indigo Flame", "影", Color(0xFF7986CB), Color(0xFF1A203D)),
    BuiltInAvatar("avatar_9", "Solar Halo", "陽", Color(0xFFE6C586), Color(0xFF332917))
)

enum class DesktopSentiment(val label: String, val color: Color) {
    ABSOLUTE_CINEMA("Absolute Cinema", Color(0xFFE6C586)),
    WORTH_WATCHING("Worth Watching", Color(0xFF56C288)),
    AVERAGE("Average", Color(0xFF64B5F6)),
    BORING("Boring", Color(0xFFFFB74D)),
    WASTE_OF_TIME("Waste of Time", Color(0xFFE57373));

    companion object {
        fun fromRating10(r10: Double?): DesktopSentiment? {
            if (r10 == null || r10 <= 0.0) return null
            val stars = r10 / 2.0
            return when {
                stars >= 4.5 -> ABSOLUTE_CINEMA
                stars >= 3.5 -> WORTH_WATCHING
                stars >= 2.5 -> AVERAGE
                stars >= 1.5 -> BORING
                else -> WASTE_OF_TIME
            }
        }
    }
}

data class DesktopSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val username: String
)

data class DesktopAnimeItem(
    val animeId: Int,
    val title: String,
    val romajiTitle: String = "",
    val nativeTitle: String = "",
    val coverImage: String,
    val bannerImage: String = "",
    val status: String = "watching",
    val watchedEpisodes: Int = 0,
    val totalEpisodes: Int? = null,
    val seasonNumber: Int = 1,
    val rating10: Double? = null,
    val reviewNotes: String = "",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val format: String = "TV",
    val mediaStatus: String = "RELEASING",
    val averageScore: Int? = null,
    val seasonYear: Int? = null,
    val genres: List<String> = emptyList(),
    val description: String = "",
    val nextAiringEpisode: Int? = null,
    val nextAiringAt: Long? = null
) {
    val stars: Double?
        get() = rating10?.let { (Math.round(it) / 2.0).coerceIn(0.5, 5.0) }

    val sentiment: DesktopSentiment?
        get() = DesktopSentiment.fromRating10(rating10)

    fun displayTitle(pref: String): String = when (pref) {
        "ROMAJI" -> romajiTitle.ifBlank { title }
        "NATIVE" -> nativeTitle.ifBlank { title }
        else -> title.ifBlank { romajiTitle }
    }
}

data class DesktopCommunityReview(
    val id: String,
    val userId: String,
    val username: String,
    val avatarKey: String,
    val animeId: Int,
    val animeTitle: String,
    val animeCoverImage: String,
    val rating10: Double,
    val reviewNotes: String,
    val updatedAt: String
) {
    val stars: Double
        get() = (Math.round(rating10) / 2.0).coerceIn(0.5, 5.0)
    val sentiment: DesktopSentiment?
        get() = DesktopSentiment.fromRating10(rating10)
}

data class DesktopPrefs(
    val isDarkTheme: Boolean = true,
    val defaultGridLayout: Boolean = true,
    val titleLanguage: String = "ENGLISH",
    val shareReviewsPublicly: Boolean = true,
    val avatarKey: String = "avatar_1",
    val bio: String = "Tracking weekly anime & donghua releases."
)

object DesktopStorage {
    private val dir = File(System.getProperty("user.home"), ".anime_universe").apply { mkdirs() }
    private val sessionFile = File(dir, "session.json")
    private val prefsFile = File(dir, "prefs.json")
    private val localVaultFile = File(dir, "local_vault.json")
    private val collectionsFile = File(dir, "collections.json")

    fun saveSession(session: DesktopSession?) {
        runCatching {
            if (session == null) {
                if (sessionFile.exists()) sessionFile.delete()
                return
            }
            val json = JSONObject()
                .put("accessToken", session.accessToken)
                .put("refreshToken", session.refreshToken)
                .put("userId", session.userId)
                .put("email", session.email)
                .put("username", session.username)
            sessionFile.writeText(json.toString())
        }
    }

    fun loadSession(): DesktopSession? = runCatching {
        if (!sessionFile.exists()) return null
        val json = JSONObject(sessionFile.readText())
        val token = json.optString("accessToken")
        val userId = json.optString("userId")
        if (token.isBlank() || userId.isBlank()) return null
        DesktopSession(
            accessToken = token,
            refreshToken = json.optString("refreshToken"),
            userId = userId,
            email = json.optString("email"),
            username = json.optString("username", "Member")
        )
    }.getOrNull()

    fun savePrefs(prefs: DesktopPrefs) {
        runCatching {
            val json = JSONObject()
                .put("isDarkTheme", prefs.isDarkTheme)
                .put("defaultGridLayout", prefs.defaultGridLayout)
                .put("titleLanguage", prefs.titleLanguage)
                .put("shareReviewsPublicly", prefs.shareReviewsPublicly)
                .put("avatarKey", prefs.avatarKey)
                .put("bio", prefs.bio)
            prefsFile.writeText(json.toString())
        }
    }

    fun loadPrefs(): DesktopPrefs = runCatching {
        if (!prefsFile.exists()) return DesktopPrefs()
        val j = JSONObject(prefsFile.readText())
        DesktopPrefs(
            isDarkTheme = j.optBoolean("isDarkTheme", true),
            defaultGridLayout = j.optBoolean("defaultGridLayout", true),
            titleLanguage = j.optString("titleLanguage", "ENGLISH"),
            shareReviewsPublicly = j.optBoolean("shareReviewsPublicly", true),
            avatarKey = j.optString("avatarKey", "avatar_1"),
            bio = j.optString("bio", "Tracking weekly anime & donghua releases.")
        )
    }.getOrDefault(DesktopPrefs())

    fun saveLocalVault(items: List<DesktopAnimeItem>) {
        runCatching {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(
                    JSONObject()
                        .put("animeId", item.animeId)
                        .put("title", item.title)
                        .put("romajiTitle", item.romajiTitle)
                        .put("nativeTitle", item.nativeTitle)
                        .put("coverImage", item.coverImage)
                        .put("status", item.status)
                        .put("watchedEpisodes", item.watchedEpisodes)
                        .put("totalEpisodes", item.totalEpisodes ?: 0)
                        .put("seasonNumber", item.seasonNumber)
                        .put("rating10", item.rating10 ?: 0.0)
                        .put("reviewNotes", item.reviewNotes)
                        .put("isPinned", item.isPinned)
                        .put("isFavorite", item.isFavorite)
                        .put("format", item.format)
                )
            }
            localVaultFile.writeText(arr.toString())
        }
    }

    fun loadLocalVault(): List<DesktopAnimeItem> = runCatching {
        if (!localVaultFile.exists()) return emptyList()
        val arr = JSONArray(localVaultFile.readText())
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    DesktopAnimeItem(
                        animeId = o.optInt("animeId"),
                        title = o.optString("title", "Anime"),
                        romajiTitle = o.optString("romajiTitle", ""),
                        nativeTitle = o.optString("nativeTitle", ""),
                        coverImage = o.optString("coverImage", ""),
                        status = o.optString("status", "watching"),
                        watchedEpisodes = o.optInt("watchedEpisodes", 0),
                        totalEpisodes = o.optInt("totalEpisodes", 0).takeIf { it > 0 },
                        seasonNumber = o.optInt("seasonNumber", 1),
                        rating10 = o.optDouble("rating10", 0.0).takeIf { it > 0.0 },
                        reviewNotes = o.optString("reviewNotes", ""),
                        isPinned = o.optBoolean("isPinned", false),
                        isFavorite = o.optBoolean("isFavorite", false),
                        format = o.optString("format", "TV")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

object DesktopApi {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val supabaseUrl: String by lazy {
        VaultMaterial.decodeEndpoint().trim().trimEnd('/')
    }
    val supabaseKey: String by lazy {
        VaultMaterial.decodeCredential().trim()
    }

    suspend fun signIn(email: String, password: String): Result<DesktopSession> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().put("email", email.trim()).put("password", password).toString()
            val req = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IllegalStateException(parseAuthErr(body, "Sign in failed."))
                val session = parseSession(body, email)
                val userObj = JSONObject(body).optJSONObject("user")
                val meta = userObj?.optJSONObject("user_metadata")
                if (meta?.optBoolean("account_deleted", false) == true) {
                    throw IllegalStateException("This account has been permanently deleted.")
                }
                session
            }
        }
    }

    suspend fun signUp(email: String, password: String, username: String): Result<DesktopSession?> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .put("data", JSONObject().put("username", username.trim()))
                .toString()
            val req = Request.Builder()
                .url("$supabaseUrl/auth/v1/signup")
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IllegalStateException(parseAuthErr(body, "Sign up failed."))
                val json = JSONObject(body)
                if (json.optString("access_token").isBlank()) null else parseSession(body, email)
            }
        }
    }

    suspend fun verifyOtp(email: String, token: String, type: String): Result<DesktopSession> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = token.trim().filter { it.isDigit() }
            if (clean.length != 8) throw IllegalArgumentException("Please enter the 8-digit verification code.")
            val payload = JSONObject()
                .put("email", email.trim())
                .put("token", clean)
                .put("type", type)
                .toString()
            val req = Request.Builder()
                .url("$supabaseUrl/auth/v1/verify")
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IllegalStateException(parseAuthErr(body, "Invalid or expired 8-digit code."))
                parseSession(body, email)
            }
        }
    }

    suspend fun sendPasswordRecovery(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().put("email", email.trim()).toString()
            val req = Request.Builder()
                .url("$supabaseUrl/auth/v1/recover")
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IllegalStateException(parseAuthErr(body, "Could not send recovery code."))
            }
        }
    }

    suspend fun updatePassword(accessToken: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().put("password", newPassword).toString()
            val req = Request.Builder()
                .url("$supabaseUrl/auth/v1/user")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .put(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IllegalStateException(parseAuthErr(body, "Could not update password."))
            }
        }
    }

    suspend fun fetchVault(session: DesktopSession): List<DesktopAnimeItem> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedUid = URLEncoder.encode(session.userId, "UTF-8")
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_anime_entries?user_id=eq.$encodedUid&order=updated_at.desc")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@use emptyList()
                val arr = JSONArray(body)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        add(
                            DesktopAnimeItem(
                                animeId = obj.optInt("anime_id"),
                                title = obj.optString("anime_title", "Anime"),
                                coverImage = obj.optString("anime_cover_image", ""),
                                status = obj.optString("watch_status", "watching"),
                                watchedEpisodes = obj.optInt("watched_episodes", 0),
                                totalEpisodes = obj.optInt("total_episodes", 0).takeIf { it > 0 },
                                seasonNumber = obj.optInt("season_number", 1).coerceAtLeast(1),
                                rating10 = obj.optDouble("rating").takeIf { !it.isNaN() && it > 0.0 },
                                reviewNotes = obj.optString("review_notes", "")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun upsertVaultItem(session: DesktopSession, item: DesktopAnimeItem, sharePublicly: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("user_id", session.userId)
                .put("anime_id", item.animeId)
                .put("anime_title", item.title)
                .put("anime_cover_image", item.coverImage)
                .put("watch_status", item.status)
                .put("watched_episodes", item.watchedEpisodes)
                .put("total_episodes", item.totalEpisodes ?: JSONObject.NULL)
                .put("season_number", item.seasonNumber)
                .put("rating", item.rating10 ?: JSONObject.NULL)
                .put("review_notes", item.reviewNotes)
                .put("visibility", if (sharePublicly) "community" else "private")
                .toString()
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_anime_entries?on_conflict=user_id,anime_id")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().close()
        }
    }

    suspend fun deleteVaultItem(session: DesktopSession, animeId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedUid = URLEncoder.encode(session.userId, "UTF-8")
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_anime_entries?user_id=eq.$encodedUid&anime_id=eq.$animeId")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .delete()
                .build()
            http.newCall(req).execute().close()
        }
    }

    suspend fun fetchMemberStatus(session: DesktopSession): String = withContext(Dispatchers.IO) {
        runCatching {
            val encodedUid = URLEncoder.encode(session.userId, "UTF-8")
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.$encodedUid&select=member_status")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val arr = JSONArray(resp.body?.string().orEmpty())
                if (arr.length() > 0) arr.getJSONObject(0).optString("member_status", "pending") else "pending"
            }
        }.getOrDefault("pending")
    }

    suspend fun updateProfile(session: DesktopSession, username: String, avatarKey: String, bio: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedUid = URLEncoder.encode(session.userId, "UTF-8")
            val payload = JSONObject()
                .put("username", username.trim())
                .put("avatar_url", avatarKey)
                .put("bio", bio.trim())
                .toString()
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/profiles?id=eq.$encodedUid")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Content-Type", "application/json")
                .patch(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().close()
        }
    }

    suspend fun joinCommunity(session: DesktopSession): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().put("p_invite_code", "").toString()
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/rpc/join_community_with_invite")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().close()
        }
    }

    suspend fun fetchCommunityReviews(session: DesktopSession): List<DesktopCommunityReview> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/community_reviews?order=updated_at.desc&limit=100")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@use emptyList()
                val arr = JSONArray(body)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val r = obj.optDouble("rating", 0.0)
                        val notes = obj.optString("review_notes", "")
                        if (r > 0.0 && notes.isNotBlank()) {
                            add(
                                DesktopCommunityReview(
                                    id = obj.optString("id", "$i"),
                                    userId = obj.optString("user_id", ""),
                                    username = obj.optString("username", "Member"),
                                    avatarKey = obj.optString("avatar_url", "avatar_1"),
                                    animeId = obj.optInt("anime_id"),
                                    animeTitle = obj.optString("anime_title", "Anime"),
                                    animeCoverImage = obj.optString("anime_cover_image", ""),
                                    rating10 = r,
                                    reviewNotes = notes,
                                    updatedAt = obj.optString("updated_at", "").take(10)
                                )
                            )
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun searchAniList(
        query: String,
        mode: String = "TRENDING",
        format: String = "ALL",
        status: String = "ALL",
        genre: String = "ALL"
    ): List<DesktopAnimeItem> = withContext(Dispatchers.IO) {
        runCatching {
            val sortExpr = when (mode) {
                "POPULAR" -> "[POPULARITY_DESC]"
                "TOP_RATED" -> "[SCORE_DESC, POPULARITY_DESC]"
                else -> "[TRENDING_DESC, POPULARITY_DESC]"
            }
            val extraArgs = mutableListOf<String>()
            if (mode == "DONGHUA") extraArgs.add("countryOfOrigin: \"CN\"")
            if (mode == "AIRING" || status == "RELEASING") extraArgs.add("status: RELEASING")
            else if (status != "ALL") extraArgs.add("status: $status")
            if (format != "ALL") extraArgs.add("format: $format")
            if (genre != "ALL") extraArgs.add("genre: \"$genre\"")

            val extraFilterStr = if (extraArgs.isEmpty()) "" else ", " + extraArgs.joinToString(", ")

            val gql = if (query.isBlank()) {
                """
                query {
                  Page(page: 1, perPage: 36) {
                    media(type: ANIME, sort: $sortExpr, isAdult: false$extraFilterStr) {
                      id
                      title { english romaji native }
                      coverImage { extraLarge large }
                      bannerImage
                      episodes
                      format
                      status
                      averageScore
                      seasonYear
                      genres
                      description(asHtml: false)
                      nextAiringEpisode { episode airingAt }
                    }
                  }
                }
                """.trimIndent()
            } else {
                """
                query(${'$'}search: String) {
                  Page(page: 1, perPage: 36) {
                    media(search: ${'$'}search, type: ANIME, sort: [POPULARITY_DESC], isAdult: false$extraFilterStr) {
                      id
                      title { english romaji native }
                      coverImage { extraLarge large }
                      bannerImage
                      episodes
                      format
                      status
                      averageScore
                      seasonYear
                      genres
                      description(asHtml: false)
                      nextAiringEpisode { episode airingAt }
                    }
                  }
                }
                """.trimIndent()
            }
            val payload = JSONObject()
                .put("query", gql)
                .apply {
                    if (query.isNotBlank()) put("variables", JSONObject().put("search", query.trim()))
                }
                .toString()
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val mediaArr = JSONObject(body)
                    .optJSONObject("data")
                    ?.optJSONObject("Page")
                    ?.optJSONArray("media") ?: return@use emptyList()
                buildList {
                    for (i in 0 until mediaArr.length()) {
                        val m = mediaArr.getJSONObject(i)
                        val titleObj = m.optJSONObject("title")
                        val eng = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }.orEmpty()
                        val rom = titleObj?.optString("romaji")?.takeIf { it.isNotBlank() && it != "null" }.orEmpty()
                        val nat = titleObj?.optString("native")?.takeIf { it.isNotBlank() && it != "null" }.orEmpty()
                        val coverObj = m.optJSONObject("coverImage")
                        val cover = coverObj?.optString("extraLarge")?.ifBlank { coverObj.optString("large") }.orEmpty()
                        val genresArr = m.optJSONArray("genres")
                        val gList = mutableListOf<String>()
                        if (genresArr != null) {
                            for (g in 0 until genresArr.length()) gList.add(genresArr.optString(g))
                        }
                        val nextAir = m.optJSONObject("nextAiringEpisode")
                        add(
                            DesktopAnimeItem(
                                animeId = m.optInt("id"),
                                title = eng.ifBlank { rom.ifBlank { "Anime" } },
                                romajiTitle = rom,
                                nativeTitle = nat,
                                coverImage = cover,
                                bannerImage = m.optString("bannerImage", ""),
                                totalEpisodes = m.optInt("episodes", 0).takeIf { it > 0 },
                                format = m.optString("format", "TV"),
                                mediaStatus = m.optString("status", "FINISHED"),
                                averageScore = m.optInt("averageScore", 0).takeIf { it > 0 },
                                seasonYear = m.optInt("seasonYear", 0).takeIf { it > 0 },
                                genres = gList,
                                description = m.optString("description", "").replace(Regex("<[^>]*>"), "").trim(),
                                nextAiringEpisode = nextAir?.optInt("episode"),
                                nextAiringAt = nextAir?.optLong("airingAt")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun deleteAccountCompletely(session: DesktopSession): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedUid = URLEncoder.encode(session.userId, "UTF-8")
            listOf(
                "$supabaseUrl/rest/v1/user_anime_entries?user_id=eq.$encodedUid",
                "$supabaseUrl/rest/v1/profiles?id=eq.$encodedUid"
            ).forEach { url ->
                runCatching {
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer ${session.accessToken}")
                        .delete()
                        .build()
                    http.newCall(req).execute().close()
                }
            }
            runCatching {
                val rpcReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/rpc/delete_own_account")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer ${session.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .post("{}".toRequestBody(jsonMedia))
                    .build()
                http.newCall(rpcReq).execute().close()
            }
            runCatching {
                val rnd = ByteArray(24).also { SecureRandom().nextBytes(it) }
                val scrambled = "Del!" + rnd.joinToString("") { "%02x".format(it) } + "9A#"
                val lockBody = JSONObject()
                    .put("password", scrambled)
                    .put("data", JSONObject().put("account_deleted", true))
                    .toString()
                val lockReq = Request.Builder()
                    .url("$supabaseUrl/auth/v1/user")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer ${session.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .put(lockBody.toRequestBody(jsonMedia))
                    .build()
                http.newCall(lockReq).execute().close()
            }
            Unit
        }
    }

    private fun parseSession(body: String, fallbackEmail: String): DesktopSession {
        val json = JSONObject(body)
        val user = json.optJSONObject("user")
        val meta = user?.optJSONObject("user_metadata")
        val email = user?.optString("email").orEmpty().ifBlank { fallbackEmail }
        val username = meta?.optString("username").orEmpty().ifBlank { email.substringBefore("@") }
        return DesktopSession(
            accessToken = json.optString("access_token"),
            refreshToken = json.optString("refresh_token"),
            userId = user?.optString("id").orEmpty(),
            email = email,
            username = username
        )
    }

    private fun parseAuthErr(raw: String, fallback: String): String = runCatching {
        val j = JSONObject(raw)
        j.optString("msg").takeIf { it.isNotBlank() }
            ?: j.optString("error_description").takeIf { it.isNotBlank() }
            ?: j.optString("message").takeIf { it.isNotBlank() }
            ?: fallback
    }.getOrDefault(fallback)
}

private val imageCache = ConcurrentHashMap<String, ImageBitmap>()

@Composable
fun NetworkPoster(url: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf(imageCache[url]) }
    LaunchedEffect(url) {
        if (url.isNotBlank() && bitmap == null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val req = Request.Builder().url(url).build()
                    DesktopApi.http.newCall(req).execute().use { resp ->
                        val bytes = resp.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            val img = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                            imageCache[url] = img
                            bitmap = img
                        }
                    }
                }
            }
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            )
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1340.dp, 860.dp),
        position = WindowPosition(Alignment.Center)
    )
    var prefs by remember { mutableStateOf(DesktopStorage.loadPrefs()) }

    val colorScheme = if (prefs.isDarkTheme) {
        darkColorScheme(
            primary = ChampagneGold,
            onPrimary = ObsidianNight,
            secondary = WarmAmber,
            background = ObsidianNight,
            surface = ObsidianSurface,
            surfaceVariant = ObsidianElevated,
            onBackground = IvoryText,
            onSurface = IvoryText,
            onSurfaceVariant = SlateSecondary,
            outline = HairlineDark,
            error = SakuraCoral
        )
    } else {
        lightColorScheme(
            primary = BronzeAccent,
            onPrimary = Color.White,
            secondary = WarmAmber,
            background = PorcelainBg,
            surface = PorcelainSurface,
            surfaceVariant = PorcelainElevated,
            onBackground = InkText,
            onSurface = InkText,
            onSurfaceVariant = SlateLightSecondary,
            outline = HairlineLight,
            error = SakuraCoral
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Anime Universe — Track Anime & Share Taste",
        state = windowState
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                AnimeUniverseDesktopApp(
                    prefs = prefs,
                    onUpdatePrefs = { updated ->
                        prefs = updated
                        DesktopStorage.savePrefs(updated)
                    }
                )
            }
        }
    }
}

@Composable
fun AnimeUniverseDesktopApp(
    prefs: DesktopPrefs,
    onUpdatePrefs: (DesktopPrefs) -> Unit
) {
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    var session by remember { mutableStateOf(DesktopStorage.loadSession()) }
    var localPreviewMode by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) } // 0:Home, 1:Explore, 2:Library, 3:Community, 4:Profile, 5:Settings

    var vaultItems by remember { mutableStateOf(DesktopStorage.loadLocalVault()) }
    var discoverItems by remember { mutableStateOf<List<DesktopAnimeItem>>(emptyList()) }
    var communityReviews by remember { mutableStateOf<List<DesktopCommunityReview>>(emptyList()) }
    var memberStatus by remember { mutableStateOf("pending") }

    // Explore state
    var searchQuery by remember { mutableStateOf("") }
    var discoverMode by remember { mutableStateOf("TRENDING") }
    var discoverFormat by remember { mutableStateOf("ALL") }
    var discoverStatus by remember { mutableStateOf("ALL") }
    var discoverGenre by remember { mutableStateOf("ALL") }

    // Library state
    var libraryStatusFilter by remember { mutableStateOf("ALL") }
    var librarySentimentFilter by remember { mutableStateOf<DesktopSentiment?>(null) }
    var librarySearchQuery by remember { mutableStateOf("") }
    var isGridView by remember(prefs.defaultGridLayout) { mutableStateOf(prefs.defaultGridLayout) }

    // Community state
    var communitySentimentFilter by remember { mutableStateOf<DesktopSentiment?>(null) }

    // Detail inspector modal
    var selectedItem by remember { mutableStateOf<DesktopAnimeItem?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session, localPreviewMode) {
        val active = session
        if (active != null) {
            val remote = DesktopApi.fetchVault(active)
            if (remote.isNotEmpty()) {
                vaultItems = remote
                DesktopStorage.saveLocalVault(remote)
            }
            memberStatus = DesktopApi.fetchMemberStatus(active)
            if (memberStatus == "active") {
                communityReviews = DesktopApi.fetchCommunityReviews(active)
            }
        }
        discoverItems = DesktopApi.searchAniList("", discoverMode, discoverFormat, discoverStatus, discoverGenre)
    }

    if (session == null && !localPreviewMode) {
        DesktopAuthScreen(
            onAuthenticated = { newSession ->
                DesktopStorage.saveSession(newSession)
                session = newSession
            },
            onContinueWithoutAccount = {
                localPreviewMode = true
            }
        )
        return
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // ── Left Desktop Sidebar (MidnightDesktopSidebar) ──
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.width(248.dp).fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Brand Identity Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "AU",
                                color = ObsidianNight,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        Column {
                            Text(
                                "ANIME UNIVERSE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                letterSpacing = 1.4.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Track anime. Share taste.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    val navItems = listOf(
                        Triple(0, "Home", Icons.Default.CalendarToday),
                        Triple(1, "Explore", Icons.Default.Explore),
                        Triple(2, "Library", Icons.Default.VideoLibrary),
                        Triple(3, "Community", Icons.Default.Groups),
                        Triple(4, "Profile", Icons.Default.Person),
                        Triple(5, "Settings", Icons.Default.Settings)
                    )

                    navItems.forEach { (idx, label, icon) ->
                        val selected = currentTab == idx
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentTab = idx }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Bottom Account / Quick Theme Switcher Card
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (prefs.isDarkTheme) "Obsidian Dark" else "Porcelain Light",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { onUpdatePrefs(prefs.copy(isDarkTheme = !prefs.isDarkTheme)) }) {
                            Icon(
                                if (prefs.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().clickable { currentTab = 4 }
                    ) {
                        val avatar = BUILT_IN_AVATARS.find { it.id == prefs.avatarKey } ?: BUILT_IN_AVATARS.first()
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(avatar.secondaryColor)
                                    .border(1.dp, avatar.primaryColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(avatar.glyph, color = avatar.primaryColor, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    session?.username ?: "Local Preview",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (session != null) "Cloud Synced" else "Local Device Mode",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Main Desktop Workspace ──
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // Top Desktop Toolbar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (currentTab) {
                                0 -> "WEEKLY SCHEDULE"
                                1 -> "DISCOVER ANIME & DONGHUA"
                                2 -> "MY LIBRARY"
                                3 -> "COMMUNITY REVIEWS"
                                4 -> "PROFILE & STATS"
                                else -> "SETTINGS & VAULT"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when (currentTab) {
                                0 -> "Track airing episodes & daily release countdowns"
                                1 -> "Search 20,000+ anime & donghua with live AniList filters"
                                2 -> "${vaultItems.size} titles in your collection"
                                3 -> "Rated & written editorial notes from Anime Universe members"
                                4 -> "Customize your avatar, bio, and taste breakdown"
                                else -> "Appearance, title language, cloud sync & account controls"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Grid / Table Layout Toggle
                        if (currentTab == 1 || currentTab == 2) {
                            OutlinedButton(
                                onClick = { isGridView = !isGridView },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    if (isGridView) Icons.Default.TableRows else Icons.Default.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isGridView) "Table View" else "Poster Grid", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { currentTab = 1 },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Quick Search", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (statusBanner != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(statusBanner!!, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        TextButton(onClick = { statusBanner = null }) {
                            Text("Dismiss", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Active Tab Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp)) {
                when (currentTab) {
                    0 -> DesktopHomeScheduleTab(
                        vaultItems = vaultItems,
                        discoverItems = discoverItems,
                        titleLang = prefs.titleLanguage,
                        onSelect = { selectedItem = it },
                        onIncrementEpisode = { item ->
                            val nextEp = item.watchedEpisodes + 1
                            val updated = item.copy(
                                watchedEpisodes = nextEp,
                                status = if (item.totalEpisodes != null && nextEp >= item.totalEpisodes) "completed" else item.status
                            )
                            vaultItems = (vaultItems.filterNot { it.animeId == updated.animeId } + updated)
                            DesktopStorage.saveLocalVault(vaultItems)
                            scope.launch { session?.let { DesktopApi.upsertVaultItem(it, updated, prefs.shareReviewsPublicly) } }
                        }
                    )

                    1 -> DesktopExploreTab(
                        query = searchQuery,
                        mode = discoverMode,
                        format = discoverFormat,
                        status = discoverStatus,
                        genre = discoverGenre,
                        isGridView = isGridView,
                        titleLang = prefs.titleLanguage,
                        items = discoverItems,
                        vaultItems = vaultItems,
                        onUpdateFilters = { q, m, f, s, g ->
                            searchQuery = q
                            discoverMode = m
                            discoverFormat = f
                            discoverStatus = s
                            discoverGenre = g
                            scope.launch {
                                discoverItems = DesktopApi.searchAniList(q, m, f, s, g)
                            }
                        },
                        onSelect = { item ->
                            val existing = vaultItems.firstOrNull { it.animeId == item.animeId }
                            selectedItem = existing?.copy(
                                description = item.description.ifBlank { existing.description },
                                genres = item.genres.ifEmpty { existing.genres },
                                averageScore = item.averageScore ?: existing.averageScore
                            ) ?: item
                        }
                    )

                    2 -> DesktopLibraryTab(
                        items = vaultItems,
                        statusFilter = libraryStatusFilter,
                        sentimentFilter = librarySentimentFilter,
                        searchQuery = librarySearchQuery,
                        isGridView = isGridView,
                        titleLang = prefs.titleLanguage,
                        onStatusFilterChange = { libraryStatusFilter = it },
                        onSentimentFilterChange = { librarySentimentFilter = it },
                        onSearchChange = { librarySearchQuery = it },
                        onSelect = { selectedItem = it },
                        onIncrementEpisode = { item ->
                            val nextEp = item.watchedEpisodes + 1
                            val updated = item.copy(
                                watchedEpisodes = nextEp,
                                status = if (item.totalEpisodes != null && nextEp >= item.totalEpisodes) "completed" else item.status
                            )
                            vaultItems = vaultItems.map { if (it.animeId == item.animeId) updated else it }
                            DesktopStorage.saveLocalVault(vaultItems)
                            scope.launch { session?.let { DesktopApi.upsertVaultItem(it, updated, prefs.shareReviewsPublicly) } }
                        }
                    )

                    3 -> DesktopCommunityTab(
                        memberStatus = if (localPreviewMode) "active" else memberStatus,
                        reviews = communityReviews,
                        sentimentFilter = communitySentimentFilter,
                        onSentimentFilterChange = { communitySentimentFilter = it },
                        onJoinCommunity = {
                            scope.launch {
                                val active = session
                                if (active != null) {
                                    DesktopApi.joinCommunity(active)
                                    memberStatus = "active"
                                    communityReviews = DesktopApi.fetchCommunityReviews(active)
                                } else {
                                    memberStatus = "active"
                                }
                            }
                        }
                    )

                    4 -> DesktopProfileTab(
                        session = session,
                        prefs = prefs,
                        vaultItems = vaultItems,
                        onSaveProfile = { newUsername, newAvatar, newBio ->
                            val updatedPrefs = prefs.copy(avatarKey = newAvatar, bio = newBio)
                            onUpdatePrefs(updatedPrefs)
                            session?.let { active ->
                                val updatedSession = active.copy(username = newUsername)
                                session = updatedSession
                                DesktopStorage.saveSession(updatedSession)
                                scope.launch {
                                    DesktopApi.updateProfile(active, newUsername, newAvatar, newBio)
                                    statusBanner = "Profile updated across Anime Universe."
                                }
                            }
                        }
                    )

                    5 -> DesktopSettingsTab(
                        session = session,
                        prefs = prefs,
                        onUpdatePrefs = onUpdatePrefs,
                        onSyncCloud = {
                            scope.launch {
                                val active = session
                                if (active != null) {
                                    vaultItems.forEach { DesktopApi.upsertVaultItem(active, it, prefs.shareReviewsPublicly) }
                                    vaultItems = DesktopApi.fetchVault(active)
                                    statusBanner = "Cloud Vault synchronized (${vaultItems.size} titles)."
                                } else {
                                    statusBanner = "Sign in to a cloud account to sync across devices."
                                }
                            }
                        },
                        onSignOut = {
                            DesktopStorage.saveSession(null)
                            session = null
                            localPreviewMode = false
                        },
                        onDeleteAccount = {
                            scope.launch {
                                val active = session ?: return@launch
                                DesktopApi.deleteAccountCompletely(active)
                                DesktopStorage.saveSession(null)
                                session = null
                                localPreviewMode = false
                            }
                        }
                    )
                }
            }
        }
    }

    selectedItem?.let { item ->
        DesktopAnimeInspectorDialog(
            item = item,
            titleLang = prefs.titleLanguage,
            onDismiss = { selectedItem = null },
            onSave = { updated ->
                selectedItem = null
                vaultItems = (vaultItems.filterNot { it.animeId == updated.animeId } + updated)
                DesktopStorage.saveLocalVault(vaultItems)
                scope.launch {
                    val active = session ?: return@launch
                    DesktopApi.upsertVaultItem(active, updated, prefs.shareReviewsPublicly)
                    if (memberStatus == "active") {
                        communityReviews = DesktopApi.fetchCommunityReviews(active)
                    }
                }
                statusBanner = "Saved \"${updated.title}\" to your library."
            },
            onRemove = { toRemove ->
                selectedItem = null
                vaultItems = vaultItems.filterNot { it.animeId == toRemove.animeId }
                DesktopStorage.saveLocalVault(vaultItems)
                scope.launch {
                    session?.let { DesktopApi.deleteVaultItem(it, toRemove.animeId) }
                }
                statusBanner = "Removed \"${toRemove.title}\" from your library."
            }
        )
    }
}

@Composable
private fun DesktopHomeScheduleTab(
    vaultItems: List<DesktopAnimeItem>,
    discoverItems: List<DesktopAnimeItem>,
    titleLang: String,
    onSelect: (DesktopAnimeItem) -> Unit,
    onIncrementEpisode: (DesktopAnimeItem) -> Unit
) {
    val days = listOf("ALL", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val todayShort = when (LocalDate.now().dayOfWeek) {
        DayOfWeek.MONDAY -> "MON"
        DayOfWeek.TUESDAY -> "TUE"
        DayOfWeek.WEDNESDAY -> "WED"
        DayOfWeek.THURSDAY -> "THU"
        DayOfWeek.FRIDAY -> "FRI"
        DayOfWeek.SATURDAY -> "SAT"
        DayOfWeek.SUNDAY -> "SUN"
    }
    var selectedDay by remember { mutableStateOf("ALL") }

    val activeWatching = vaultItems.filter { it.status == "watching" || it.isPinned }
    val scheduleSource = if (activeWatching.isNotEmpty()) activeWatching else discoverItems.take(12)

    val filteredSchedule = scheduleSource.filter { item ->
        if (selectedDay == "ALL") true
        else {
            val dayBuckets = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            val assigned = dayBuckets[Math.abs(item.animeId) % 7]
            assigned == selectedDay
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Day Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.forEach { day ->
                val isSelected = selectedDay == day
                val label = if (day == "ALL") "All Week" else if (day == todayShort) "Today • $day" else day
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDay = day },
                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) }
                )
            }
        }

        if (filteredSchedule.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No scheduled releases for this day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 310.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSchedule, key = { it.animeId }) { item ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(item) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NetworkPoster(url = item.coverImage, modifier = Modifier.width(76.dp).height(108.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = item.nextAiringEpisode?.let { "EP $it AIRING" } ?: item.format,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (item.isPinned) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    item.displayTitle(titleLang),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Season ${item.seasonNumber} • Ep ${item.watchedEpisodes} / ${item.totalEpisodes ?: "?"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { onIncrementEpisode(item) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("+1 Ep", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onSelect(item) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Inspect", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopExploreTab(
    query: String,
    mode: String,
    format: String,
    status: String,
    genre: String,
    isGridView: Boolean,
    titleLang: String,
    items: List<DesktopAnimeItem>,
    vaultItems: List<DesktopAnimeItem>,
    onUpdateFilters: (String, String, String, String, String) -> Unit,
    onSelect: (DesktopAnimeItem) -> Unit
) {
    val modes = listOf(
        "TRENDING" to "Trending",
        "POPULAR" to "Popular",
        "TOP_RATED" to "Top Rated",
        "DONGHUA" to "Donghua (CN)",
        "AIRING" to "Currently Airing"
    )
    val formats = listOf("ALL", "TV", "MOVIE", "ONA", "OVA")
    val genres = listOf("ALL", "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Romance", "Sci-Fi", "Supernatural")

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { onUpdateFilters(it, mode, format, status, genre) },
                placeholder = { Text("Search anime, donghua, studios, or seasons...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Mode Chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { (key, label) ->
                FilterChip(
                    selected = mode == key,
                    onClick = { onUpdateFilters(query, key, format, status, genre) },
                    label = { Text(label) }
                )
            }
            Spacer(Modifier.width(8.dp))
            formats.forEach { fmt ->
                FilterChip(
                    selected = format == fmt,
                    onClick = { onUpdateFilters(query, mode, fmt, status, genre) },
                    label = { Text(if (fmt == "ALL") "All Formats" else fmt) }
                )
            }
        }

        // Genre Chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genres.forEach { g ->
                FilterChip(
                    selected = genre == g,
                    onClick = { onUpdateFilters(query, mode, format, status, g) },
                    label = { Text(if (g == "ALL") "All Genres" else g, fontSize = 12.sp) }
                )
            }
        }

        if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 185.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.animeId }) { item ->
                    val tracked = vaultItems.firstOrNull { it.animeId == item.animeId }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(item) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box {
                                NetworkPoster(url = item.coverImage, modifier = Modifier.fillMaxWidth().height(230.dp))
                                if (tracked != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                    ) {
                                        Text(
                                            tracked.status.uppercase(),
                                            color = ObsidianNight,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                item.displayTitle(titleLang),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${item.format} • ${item.totalEpisodes ?: "?"} eps" +
                                    (item.averageScore?.let { " • ★ ${it}%" } ?: ""),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            DesktopAnimeTable(items = items, titleLang = titleLang, onSelect = onSelect)
        }
    }
}

@Composable
private fun DesktopLibraryTab(
    items: List<DesktopAnimeItem>,
    statusFilter: String,
    sentimentFilter: DesktopSentiment?,
    searchQuery: String,
    isGridView: Boolean,
    titleLang: String,
    onStatusFilterChange: (String) -> Unit,
    onSentimentFilterChange: (DesktopSentiment?) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelect: (DesktopAnimeItem) -> Unit,
    onIncrementEpisode: (DesktopAnimeItem) -> Unit
) {
    val statuses = listOf(
        "ALL" to "All",
        "watching" to "Watching",
        "completed" to "Completed",
        "plan_to_watch" to "Plan to Watch",
        "on_hold" to "On Hold",
        "dropped" to "Dropped"
    )

    val filtered = items.filter { item ->
        val matchesStatus = statusFilter == "ALL" || item.status.equals(statusFilter, ignoreCase = true)
        val matchesSentiment = sentimentFilter == null || item.sentiment == sentimentFilter
        val matchesQuery = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesSentiment && matchesQuery
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Filter your library by title...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }

        // Status Tabs
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { (key, label) ->
                FilterChip(
                    selected = statusFilter == key,
                    onClick = { onStatusFilterChange(key) },
                    label = { Text(label) }
                )
            }
        }

        // Sentiment Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = sentimentFilter == null,
                onClick = { onSentimentFilterChange(null) },
                label = { Text("All Verdicts", fontSize = 12.sp) }
            )
            DesktopSentiment.entries.forEach { sentiment ->
                FilterChip(
                    selected = sentimentFilter == sentiment,
                    onClick = { onSentimentFilterChange(if (sentimentFilter == sentiment) null else sentiment) },
                    label = { Text(sentiment.label, fontSize = 12.sp, color = sentiment.color) }
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No matching titles in your library. Use Explore to add anime or donghua.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 290.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.animeId }) { item ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(item) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NetworkPoster(url = item.coverImage, modifier = Modifier.width(80.dp).height(114.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    item.displayTitle(titleLang),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "S${item.seasonNumber} • Ep ${item.watchedEpisodes} / ${item.totalEpisodes ?: "?"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                item.stars?.let { stars ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "★ $stars / 5",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        item.sentiment?.let { sent ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = sent.color.copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    sent.label,
                                                    color = sent.color,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { onIncrementEpisode(item) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("+1 Ep", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onSelect(item) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Edit", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            DesktopAnimeTable(items = filtered, titleLang = titleLang, onSelect = onSelect)
        }
    }
}

@Composable
private fun DesktopAnimeTable(
    items: List<DesktopAnimeItem>,
    titleLang: String,
    onSelect: (DesktopAnimeItem) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TITLE", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2.5f))
                Text("STATUS", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("PROGRESS", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("RATING & VERDICT", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.animeId }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(2.5f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NetworkPoster(url = item.coverImage, modifier = Modifier.size(42.dp))
                            Text(
                                item.displayTitle(titleLang),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            item.status.replace("_", " ").uppercase(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "S${item.seasonNumber} • ${item.watchedEpisodes}/${item.totalEpisodes ?: "?"}",
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier.weight(1.5f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.stars?.let { "★ $it" } ?: "Unrated",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            item.sentiment?.let { sent ->
                                Text(sent.label, fontSize = 11.sp, color = sent.color)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun DesktopCommunityTab(
    memberStatus: String,
    reviews: List<DesktopCommunityReview>,
    sentimentFilter: DesktopSentiment?,
    onSentimentFilterChange: (DesktopSentiment?) -> Unit,
    onJoinCommunity: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (memberStatus != "active") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.width(460.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Join the Anime Universe Community", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Unlock community reviews and share your rated anime notes with other members.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onJoinCommunity, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                            Text("Join Community")
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sentimentFilter == null,
                    onClick = { onSentimentFilterChange(null) },
                    label = { Text("All Reviews") }
                )
                DesktopSentiment.entries.forEach { s ->
                    FilterChip(
                        selected = sentimentFilter == s,
                        onClick = { onSentimentFilterChange(if (sentimentFilter == s) null else s) },
                        label = { Text(s.label, color = s.color) }
                    )
                }
            }

            val filtered = reviews.filter { sentimentFilter == null || it.sentiment == sentimentFilter }
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No community reviews match this filter yet. Rate an anime and write review notes to publish!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { rev ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                NetworkPoster(url = rev.animeCoverImage, modifier = Modifier.width(74.dp).height(104.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(rev.animeTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "★ ${rev.stars} / 5",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            rev.sentiment?.let { sent ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = sent.color.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        sent.label,
                                                        color = sent.color,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        "\"${rev.reviewNotes}\"",
                                        fontSize = 14.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                    Text(
                                        "Reviewed by @${rev.username} • ${rev.updatedAt}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopProfileTab(
    session: DesktopSession?,
    prefs: DesktopPrefs,
    vaultItems: List<DesktopAnimeItem>,
    onSaveProfile: (String, String, String) -> Unit
) {
    var username by remember(session) { mutableStateOf(session?.username ?: "AnimeFan") }
    var selectedAvatar by remember(prefs.avatarKey) { mutableStateOf(prefs.avatarKey) }
    var bio by remember(prefs.bio) { mutableStateOf(prefs.bio) }

    val totalEpisodes = vaultItems.sumOf { it.watchedEpisodes }
    val ratedItems = vaultItems.mapNotNull { it.stars }
    val meanStars = if (ratedItems.isNotEmpty()) "%.2f".format(ratedItems.average()) else "—"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Stats Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                "Tracked Anime" to "${vaultItems.size}",
                "Episodes Watched" to "$totalEpisodes",
                "Rated Titles" to "${ratedItems.size}",
                "Mean Score (5★)" to meanStars
            ).forEach { (label, value) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Built-In Anime Avatars & Profile Editor
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Choose Built-In Anime Avatar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BUILT_IN_AVATARS.forEach { avatar ->
                        val isSelected = selectedAvatar == avatar.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = avatar.secondaryColor,
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) avatar.primaryColor else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.width(120.dp).clickable { selectedAvatar = avatar.id }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(avatar.glyph, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = avatar.primaryColor)
                                Text(avatar.label, fontSize = 11.sp, color = IvoryText, maxLines = 1)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Display Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio & Taste Signature") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = { onSaveProfile(username, selectedAvatar, bio) }) {
                    Text("Save Profile")
                }
            }
        }
    }
}

@Composable
private fun DesktopSettingsTab(
    session: DesktopSession?,
    prefs: DesktopPrefs,
    onUpdatePrefs: (DesktopPrefs) -> Unit,
    onSyncCloud: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Appearance & Title Language", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Theme Palette:", modifier = Modifier.width(160.dp))
                    FilterChip(
                        selected = prefs.isDarkTheme,
                        onClick = { onUpdatePrefs(prefs.copy(isDarkTheme = true)) },
                        label = { Text("Obsidian & Champagne Gold (Dark)") }
                    )
                    FilterChip(
                        selected = !prefs.isDarkTheme,
                        onClick = { onUpdatePrefs(prefs.copy(isDarkTheme = false)) },
                        label = { Text("Gallery Porcelain & Bronze (Light)") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Preferred Title Language:", modifier = Modifier.width(160.dp))
                    listOf("ENGLISH", "ROMAJI", "NATIVE").forEach { lang ->
                        FilterChip(
                            selected = prefs.titleLanguage == lang,
                            onClick = { onUpdatePrefs(prefs.copy(titleLanguage = lang)) },
                            label = { Text(lang) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Default View Mode:", modifier = Modifier.width(160.dp))
                    FilterChip(
                        selected = prefs.defaultGridLayout,
                        onClick = { onUpdatePrefs(prefs.copy(defaultGridLayout = true)) },
                        label = { Text("Poster Grid") }
                    )
                    FilterChip(
                        selected = !prefs.defaultGridLayout,
                        onClick = { onUpdatePrefs(prefs.copy(defaultGridLayout = false)) },
                        label = { Text("Desktop Table") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Community Sharing:", modifier = Modifier.width(160.dp))
                    Switch(
                        checked = prefs.shareReviewsPublicly,
                        onCheckedChange = { onUpdatePrefs(prefs.copy(shareReviewsPublicly = it)) }
                    )
                    Text(
                        if (prefs.shareReviewsPublicly) "Publicly share rated review notes" else "Private vault only",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Account & Cloud Synchronization", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Signed in as: ${session?.email ?: "Local Preview Mode"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onSyncCloud) {
                        Text("Sync Cloud Vault Now")
                    }
                    OutlinedButton(onClick = onSignOut) {
                        Text(if (session != null) "Sign Out" else "Go to Sign In")
                    }
                }
            }
        }

        if (session != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SakuraCoral.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Danger Zone", fontWeight = FontWeight.Bold, color = SakuraCoral)
                    Text(
                        "Permanently delete your account, watchlist, and community reviews from the server.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!confirmDelete) {
                        Button(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SakuraCoral)
                        ) {
                            Text("Delete Account Permanently", color = Color.White)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onDeleteAccount,
                                colors = ButtonDefaults.buttonColors(containerColor = SakuraCoral)
                            ) {
                                Text("Confirm Permanent Deletion", color = Color.White)
                            }
                            OutlinedButton(onClick = { confirmDelete = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopAnimeInspectorDialog(
    item: DesktopAnimeItem,
    titleLang: String,
    onDismiss: () -> Unit,
    onSave: (DesktopAnimeItem) -> Unit,
    onRemove: (DesktopAnimeItem) -> Unit
) {
    var status by remember { mutableStateOf(item.status) }
    var seasonNumber by remember { mutableStateOf(item.seasonNumber.toString()) }
    var watched by remember { mutableStateOf(item.watchedEpisodes.toString()) }
    var stars by remember { mutableStateOf(item.stars ?: 0.0) }
    var notes by remember { mutableStateOf(item.reviewNotes) }
    var isPinned by remember { mutableStateOf(item.isPinned) }
    var isFavorite by remember { mutableStateOf(item.isFavorite) }

    DialogWindow(
        onCloseRequest = onDismiss,
        title = item.displayTitle(titleLang),
        state = rememberDialogState(size = DpSize(680.dp, 640.dp))
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NetworkPoster(url = item.coverImage, modifier = Modifier.width(110.dp).height(156.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.displayTitle(titleLang), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (item.romajiTitle.isNotBlank() && item.romajiTitle != item.title) {
                            Text(item.romajiTitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${item.format} • ${item.totalEpisodes ?: "?"} Episodes" +
                                (item.seasonYear?.let { " • $it" } ?: ""),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.genres.isNotEmpty()) {
                            Text(item.genres.joinToString(" • "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (item.description.isNotBlank()) {
                            Text(
                                item.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Watch Status Chips
                Text("Watch Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "watching" to "Watching",
                        "completed" to "Completed",
                        "plan_to_watch" to "Plan to Watch",
                        "on_hold" to "On Hold",
                        "dropped" to "Dropped"
                    ).forEach { (k, label) ->
                        FilterChip(
                            selected = status == k,
                            onClick = { status = k },
                            label = { Text(label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = seasonNumber,
                        onValueChange = { seasonNumber = it.filter { c -> c.isDigit() } },
                        label = { Text("Season #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = watched,
                        onValueChange = { watched = it.filter { c -> c.isDigit() } },
                        label = { Text("Watched Episodes (of ${item.totalEpisodes ?: "?"})") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                    FilterChip(
                        selected = isPinned,
                        onClick = { isPinned = !isPinned },
                        label = { Text("Pin") }
                    )
                    FilterChip(
                        selected = isFavorite,
                        onClick = { isFavorite = !isFavorite },
                        label = { Text("Favorite") }
                    )
                }

                // 5-Star Half-Star Rating + Live Verdict
                val currentSentiment = DesktopSentiment.fromRating10(if (stars > 0) stars * 2.0 else null)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Rating (0.5★ increments): ${if (stars > 0) "$stars / 5.0 ★" else "Unrated"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    currentSentiment?.let { sent ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = sent.color.copy(alpha = 0.16f)
                        ) {
                            Text(
                                sent.label,
                                color = sent.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Slider(
                    value = stars.toFloat(),
                    onValueChange = { v -> stars = (Math.round(v * 2.0) / 2.0) },
                    valueRange = 0f..5f,
                    steps = 9
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Review Notes (Shared to Community if rated & non-empty)") },
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { onRemove(item) }) {
                        Text("Remove from Library", color = SakuraCoral)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onSave(
                                    item.copy(
                                        status = status,
                                        seasonNumber = (seasonNumber.toIntOrNull() ?: 1).coerceAtLeast(1),
                                        watchedEpisodes = watched.toIntOrNull() ?: 0,
                                        rating10 = if (stars > 0.0) stars * 2.0 else null,
                                        reviewNotes = notes.trim(),
                                        isPinned = isPinned,
                                        isFavorite = isFavorite
                                    )
                                )
                            }
                        ) {
                            Text("Save Entry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopAuthScreen(
    onAuthenticated: (DesktopSession) -> Unit,
    onContinueWithoutAccount: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSignUp by remember { mutableStateOf(false) }
    var awaitingSignupOtp by remember { mutableStateOf(false) }
    var awaitingResetOtp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.width(470.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AU", color = ObsidianNight, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }

                Text(
                    "ANIME UNIVERSE",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when {
                        awaitingSignupOtp -> "Enter the 8-digit verification code sent to $email"
                        awaitingResetOtp -> "Enter the 8-digit recovery code and your new password"
                        isSignUp -> "Create your cloud-synced Anime Universe account"
                        else -> "Sign in to sync your watchlist & community reviews"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = SakuraCoral, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                if (infoMsg != null) {
                    Text(infoMsg!!, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                when {
                    awaitingSignupOtp -> {
                        val isOtpComplete = otpCode.length == 8
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { input -> otpCode = input.filter { it.isDigit() }.take(8) },
                            label = { Text("8-Digit Verification Code") },
                            placeholder = { Text("ex-12345678") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                loading = true
                                errorMsg = null
                                scope.launch {
                                    DesktopApi.verifyOtp(email, otpCode, "signup")
                                        .onSuccess { onAuthenticated(it) }
                                        .onFailure { errorMsg = it.message }
                                    loading = false
                                }
                            },
                            enabled = !loading && isOtpComplete,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(if (loading) "Verifying..." else "Verify & Continue")
                        }
                        TextButton(onClick = { awaitingSignupOtp = false; otpCode = "" }) {
                            Text("Back to Sign In")
                        }
                    }

                    awaitingResetOtp -> {
                        val isResetReady = otpCode.length == 8 && newPassword.length >= 6
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { input -> otpCode = input.filter { it.isDigit() }.take(8) },
                            label = { Text("8-Digit Recovery Code") },
                            placeholder = { Text("ex-12345678") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password (min 6 chars)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                loading = true
                                errorMsg = null
                                scope.launch {
                                    DesktopApi.verifyOtp(email, otpCode, "recovery")
                                        .onSuccess { recoveredSession ->
                                            DesktopApi.updatePassword(recoveredSession.accessToken, newPassword)
                                                .onSuccess { onAuthenticated(recoveredSession) }
                                                .onFailure { errorMsg = it.message }
                                        }
                                        .onFailure { errorMsg = it.message }
                                    loading = false
                                }
                            },
                            enabled = !loading && isResetReady,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(if (loading) "Updating Password..." else "Confirm New Password")
                        }
                        TextButton(onClick = { awaitingResetOtp = false; otpCode = "" }) {
                            Text("Back to Sign In")
                        }
                    }

                    else -> {
                        if (isSignUp) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                loading = true
                                errorMsg = null
                                scope.launch {
                                    if (isSignUp) {
                                        DesktopApi.signUp(email, password, username.ifBlank { email.substringBefore("@") })
                                            .onSuccess { s ->
                                                if (s != null) onAuthenticated(s)
                                                else {
                                                    awaitingSignupOtp = true
                                                    infoMsg = "8-digit verification code sent to $email"
                                                }
                                            }
                                            .onFailure { errorMsg = it.message }
                                    } else {
                                        DesktopApi.signIn(email, password)
                                            .onSuccess { onAuthenticated(it) }
                                            .onFailure { errorMsg = it.message }
                                    }
                                    loading = false
                                }
                            },
                            enabled = !loading && email.isNotBlank() && password.length >= 6,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(if (loading) "Please wait..." else if (isSignUp) "Create Account" else "Sign In")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { isSignUp = !isSignUp; errorMsg = null }) {
                                Text(if (isSignUp) "Already have an account? Sign In" else "Create Account")
                            }
                            if (!isSignUp) {
                                TextButton(
                                    onClick = {
                                        if (email.isBlank()) {
                                            errorMsg = "Enter your email first to receive an 8-digit recovery code."
                                        } else {
                                            scope.launch {
                                                DesktopApi.sendPasswordRecovery(email)
                                                awaitingResetOtp = true
                                                infoMsg = "8-digit recovery code sent to $email"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Forgot Password?")
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        OutlinedButton(
                            onClick = onContinueWithoutAccount,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue in Local Preview Mode")
                        }
                    }
                }
            }
        }
    }
}
