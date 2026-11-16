package com.example.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
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
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private val BgObsidian = Color(0xFF06070B)
private val SurfaceObsidian = Color(0xFF0F1219)
private val ElevatedObsidian = Color(0xFF171B26)
private val GoldPrimary = Color(0xFFDFBC74)
private val TextPrimary = Color(0xFFF3F0E6)
private val TextSecondary = Color(0xFFA0A6B8)
private val ErrorCrimson = Color(0xFFE56B6F)

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
    val coverImage: String,
    val status: String = "watching",
    val watchedEpisodes: Int = 0,
    val totalEpisodes: Int? = null,
    val rating10: Double? = null,
    val reviewNotes: String = ""
)

data class DesktopCommunityReview(
    val id: String,
    val username: String,
    val animeId: Int,
    val animeTitle: String,
    val animeCoverImage: String,
    val rating10: Double,
    val reviewNotes: String,
    val updatedAt: String
)

object DesktopStorage {
    private val dir = File(System.getProperty("user.home"), ".auralog").apply { mkdirs() }
    private val sessionFile = File(dir, "session.json")

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

    val isConfigured: Boolean
        get() = supabaseUrl.startsWith("https://") && supabaseKey.length > 20

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
                                rating10 = obj.optDouble("rating").takeIf { !it.isNaN() && it > 0.0 },
                                reviewNotes = obj.optString("review_notes", "")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun upsertVaultItem(session: DesktopSession, item: DesktopAnimeItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("user_id", session.userId)
                .put("anime_id", item.animeId)
                .put("anime_title", item.title)
                .put("anime_cover_image", item.coverImage)
                .put("watch_status", item.status)
                .put("watched_episodes", item.watchedEpisodes)
                .put("total_episodes", item.totalEpisodes ?: JSONObject.NULL)
                .put("rating", item.rating10 ?: JSONObject.NULL)
                .put("review_notes", item.reviewNotes)
                .put("visibility", "community")
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
                                    username = obj.optString("username", "Member"),
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

    suspend fun searchAniList(query: String): List<DesktopAnimeItem> = withContext(Dispatchers.IO) {
        runCatching {
            val gql = if (query.isBlank()) {
                """
                query {
                  Page(page: 1, perPage: 24) {
                    media(type: ANIME, sort: [TRENDING_DESC, POPULARITY_DESC], isAdult: false) {
                      id
                      title { english romaji }
                      coverImage { large }
                      episodes
                    }
                  }
                }
                """.trimIndent()
            } else {
                """
                query(${'$'}search: String) {
                  Page(page: 1, perPage: 24) {
                    media(search: ${'$'}search, type: ANIME, sort: [POPULARITY_DESC], isAdult: false) {
                      id
                      title { english romaji }
                      coverImage { large }
                      episodes
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
                        val title = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
                            ?: titleObj?.optString("romaji")
                            ?: "Anime"
                        val cover = m.optJSONObject("coverImage")?.optString("large").orEmpty()
                        add(
                            DesktopAnimeItem(
                                animeId = m.optInt("id"),
                                title = title,
                                coverImage = cover,
                                totalEpisodes = m.optInt("episodes", 0).takeIf { it > 0 }
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
            .background(ElevatedObsidian),
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
            Icon(Icons.Default.Movie, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.45f))
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1240.dp, 820.dp),
        position = WindowPosition(Alignment.Center)
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "AuraLog — Anime & Donghua Vault",
        state = windowState
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = GoldPrimary,
                background = BgObsidian,
                surface = SurfaceObsidian,
                onPrimary = BgObsidian,
                onBackground = TextPrimary,
                onSurface = TextPrimary,
                error = ErrorCrimson
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = BgObsidian) {
                AuraLogDesktopApp()
            }
        }
    }
}

@Composable
fun AuraLogDesktopApp() {
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    var session by remember { mutableStateOf(DesktopStorage.loadSession()) }
    var currentTab by remember { mutableStateOf(0) }
    var vaultItems by remember { mutableStateOf<List<DesktopAnimeItem>>(emptyList()) }
    var discoverItems by remember { mutableStateOf<List<DesktopAnimeItem>>(emptyList()) }
    var communityReviews by remember { mutableStateOf<List<DesktopCommunityReview>>(emptyList()) }
    var memberStatus by remember { mutableStateOf("pending") }
    var searchQuery by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<DesktopAnimeItem?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session) {
        val active = session
        if (active != null) {
            vaultItems = DesktopApi.fetchVault(active)
            memberStatus = DesktopApi.fetchMemberStatus(active)
            if (memberStatus == "active") {
                communityReviews = DesktopApi.fetchCommunityReviews(active)
            }
        }
        discoverItems = DesktopApi.searchAniList("")
    }

    if (session == null) {
        DesktopAuthScreen(
            onAuthenticated = { newSession ->
                DesktopStorage.saveSession(newSession)
                session = newSession
            }
        )
        return
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation Rail for Desktop
        NavigationRail(
            containerColor = SurfaceObsidian,
            header = {
                Column(
                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GoldPrimary.copy(alpha = 0.16f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("AURALOG", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        ) {
            NavigationRailItem(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Vault") },
                label = { Text("Vault") }
            )
            NavigationRailItem(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
                label = { Text("Discover") }
            )
            NavigationRailItem(
                selected = currentTab == 2,
                onClick = { currentTab = 2 },
                icon = { Icon(Icons.Default.Groups, contentDescription = "Community") },
                label = { Text("Community") }
            )
            NavigationRailItem(
                selected = currentTab == 3,
                onClick = { currentTab = 3 },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") }
            )
        }

        // Main Content Pane
        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
            when (currentTab) {
                0 -> DesktopVaultTab(
                    items = vaultItems,
                    onEdit = { editingItem = it },
                    onIncrementEpisode = { item ->
                        val updated = item.copy(watchedEpisodes = item.watchedEpisodes + 1)
                        vaultItems = vaultItems.map { if (it.animeId == item.animeId) updated else it }
                        scope.launch { session?.let { DesktopApi.upsertVaultItem(it, updated) } }
                    }
                )
                1 -> DesktopDiscoverTab(
                    query = searchQuery,
                    onQueryChange = { q ->
                        searchQuery = q
                        scope.launch { discoverItems = DesktopApi.searchAniList(q) }
                    },
                    items = discoverItems,
                    onAddOrEdit = { item ->
                        val existing = vaultItems.firstOrNull { it.animeId == item.animeId } ?: item
                        editingItem = existing
                    }
                )
                2 -> DesktopCommunityTab(
                    memberStatus = memberStatus,
                    reviews = communityReviews,
                    onJoinCommunity = {
                        scope.launch {
                            val active = session ?: return@launch
                            DesktopApi.joinCommunity(active)
                            memberStatus = "active"
                            communityReviews = DesktopApi.fetchCommunityReviews(active)
                        }
                    }
                )
                3 -> DesktopSettingsTab(
                    session = session!!,
                    statusMessage = statusMessage,
                    onSignOut = {
                        DesktopStorage.saveSession(null)
                        session = null
                    },
                    onDeleteAccount = {
                        scope.launch {
                            val active = session ?: return@launch
                            DesktopApi.deleteAccountCompletely(active)
                            DesktopStorage.saveSession(null)
                            session = null
                        }
                    }
                )
            }
        }
    }

    editingItem?.let { item ->
        DesktopEditEntryDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                editingItem = null
                vaultItems = (vaultItems.filterNot { it.animeId == updated.animeId } + updated)
                scope.launch {
                    val active = session ?: return@launch
                    DesktopApi.upsertVaultItem(active, updated)
                    if (memberStatus == "active") {
                        communityReviews = DesktopApi.fetchCommunityReviews(active)
                    }
                }
            }
        )
    }
}

@Composable
private fun DesktopAuthScreen(onAuthenticated: (DesktopSession) -> Unit) {
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
            color = SurfaceObsidian,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
            modifier = Modifier.width(460.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("AuraLog Vault", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = GoldPrimary)
                Text(
                    text = when {
                        awaitingSignupOtp -> "Enter the 8-digit verification code sent to $email"
                        awaitingResetOtp -> "Enter the 8-digit recovery code and your new password"
                        isSignUp -> "Create your cloud-synced Anime & Donghua account"
                        else -> "Sign in to sync your watchlist & community reviews"
                    },
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = ErrorCrimson, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                if (infoMsg != null) {
                    Text(infoMsg!!, color = GoldPrimary, fontSize = 13.sp, textAlign = TextAlign.Center)
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
                            Text("Back to Sign In", color = TextSecondary)
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
                            Text("Back to Sign In", color = TextSecondary)
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
                                    Text("Forgot Password?", color = TextSecondary)
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
private fun DesktopVaultTab(
    items: List<DesktopAnimeItem>,
    onEdit: (DesktopAnimeItem) -> Unit,
    onIncrementEpisode: (DesktopAnimeItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("My Anime Vault", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text("${items.size} Tracked Titles", fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your vault is empty. Open Discover to add anime to your list.", color = TextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.animeId }) { item ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceObsidian,
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.16f)),
                        modifier = Modifier.fillMaxWidth().clickable { onEdit(item) }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            NetworkPoster(url = item.coverImage, modifier = Modifier.size(72.dp, 104.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${item.status.replace('_', ' ').uppercase()} • ${item.watchedEpisodes}/${item.totalEpisodes ?: "?"} eps",
                                    fontSize = 12.sp,
                                    color = GoldPrimary
                                )
                                if (item.rating10 != null && item.rating10 > 0) {
                                    Text("★ ${String.format("%.1f", item.rating10 / 2.0)} / 5.0", fontSize = 12.sp, color = GoldPrimary)
                                }
                                OutlinedButton(
                                    onClick = { onIncrementEpisode(item) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("+1 Episode", fontSize = 12.sp)
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
private fun DesktopDiscoverTab(
    query: String,
    onQueryChange: (String) -> Unit,
    items: List<DesktopAnimeItem>,
    onAddOrEdit: (DesktopAnimeItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Discover Anime & Donghua", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("Trending & Search via AniList", fontSize = 13.sp, color = TextSecondary)
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search anime title...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.width(340.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 190.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.animeId }) { item ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceObsidian,
                    modifier = Modifier.fillMaxWidth().clickable { onAddOrEdit(item) }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NetworkPoster(url = item.coverImage, modifier = Modifier.fillMaxWidth().height(230.dp))
                        Text(item.title, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${item.totalEpisodes ?: "?"} Episodes", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopCommunityTab(
    memberStatus: String,
    reviews: List<DesktopCommunityReview>,
    onJoinCommunity: () -> Unit
) {
    if (memberStatus != "active") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = SurfaceObsidian,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.width(440.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                    Text("Join the Community", fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Unlock rated & noted anime showcases shared by members. Your own ratings and notes stay private unless you enable sharing.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onJoinCommunity,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Join Community")
                    }
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Community • Rated & Noted", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("${reviews.size} Member Showcases", fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(reviews, key = { it.id }) { rev ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceObsidian,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.16f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        NetworkPoster(url = rev.animeCoverImage, modifier = Modifier.size(68.dp, 98.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(rev.animeTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("★ ${String.format("%.1f", rev.rating10 / 2.0)} / 5.0", color = GoldPrimary, fontSize = 13.sp)
                            Text("“${rev.reviewNotes}”", fontStyle = FontStyle.Italic, color = TextPrimary.copy(alpha = 0.9f))
                            Text("By ${rev.username} • ${rev.updatedAt}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopSettingsTab(
    session: DesktopSession,
    statusMessage: String?,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Account & Desktop Settings", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Surface(shape = RoundedCornerShape(20.dp), color = SurfaceObsidian, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(session.username, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = GoldPrimary)
                Text(session.email, fontSize = 13.sp, color = TextSecondary)
                Text("AuraLog Desktop Edition v2.1.0", fontSize = 12.sp, color = TextSecondary)
            }
        }
        if (statusMessage != null) {
            Text(statusMessage, color = GoldPrimary, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSignOut) {
                Text("Sign Out")
            }
            Button(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson)
            ) {
                Text("Delete Account Permanently", color = Color.White)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Account Permanently?") },
            text = { Text("This permanently deletes your cloud watchlist, profile, community reviews, and login credentials.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson)
                ) {
                    Text("Delete Forever", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DesktopEditEntryDialog(
    item: DesktopAnimeItem,
    onDismiss: () -> Unit,
    onSave: (DesktopAnimeItem) -> Unit
) {
    var status by remember { mutableStateOf(item.status) }
    var episodes by remember { mutableStateOf(item.watchedEpisodes.toString()) }
    var stars by remember { mutableStateOf(((item.rating10 ?: 0.0) / 2.0).toFloat()) }
    var notes by remember { mutableStateOf(item.reviewNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("watching", "completed", "plan_to_watch", "dropped").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.replace('_', ' ').uppercase(), fontSize = 10.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = episodes,
                    onValueChange = { episodes = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Watched Episodes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Star Rating: ${String.format("%.1f", stars)} / 5.0", color = GoldPrimary, fontSize = 13.sp)
                Slider(
                    value = stars,
                    onValueChange = { stars = (Math.round(it * 2f) / 2f) },
                    valueRange = 0f..5f,
                    steps = 9
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Review / Notes (Required with rating for Community showcase)") },
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        item.copy(
                            status = status,
                            watchedEpisodes = episodes.toIntOrNull() ?: 0,
                            rating10 = if (stars > 0f) (stars * 2.0) else null,
                            reviewNotes = notes.trim()
                        )
                    )
                }
            ) {
                Text("Save to Cloud Vault")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
