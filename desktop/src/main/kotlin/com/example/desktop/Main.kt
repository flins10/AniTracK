package com.example.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// ============================================================================
// ANIME UNIVERSE — COMPOSE DESKTOP (WINDOWS .EXE & MACOS .DMG)
// 1:1 Screenshot Visual Parity & Real Supabase Cloud Auto-Sync
// ============================================================================

private val PureObsidian = Color(0xFF0A0C12)
private val ObsidianSidebar = Color(0xFF0E1017)
private val ObsidianCard = Color(0xFF12151E)
private val ObsidianQuoteBox = Color(0xFF171B26)
private val ObsidianPillBg = Color(0xFF171A24)
private val ObsidianInputBg = Color(0xFF141822)
private val ObsidianBorder = Color(0xFF262B3A)
private val ChampagneGold = Color(0xFFE5C17B)
private val ChampagneGoldSoft = Color(0xFFF3DFA8)
private val ChampagneGoldBronze = Color(0xFFC29B53)
private val ChampagneOnGold = Color(0xFF110E08)
private val IvoryText = Color(0xFFF4EFE6)
private val WarmSilver = Color(0xFF9499A6)
private val AshMuted = Color(0xFF636875)
private val EmeraldWorth = Color(0xFF5CE092)
private val EmeraldWorthBg = Color(0xFF153328)
private val AmberBoring = Color(0xFFF0B45B)
private val CrimsonWaste = Color(0xFFF26D7D)
private val SilverAvg = Color(0xFFB5BAC7)

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()
private val gson = Gson()
private val imageCache = ConcurrentHashMap<String, ImageBitmap>()

// Vault Shards (Matches Android VaultMaterial & Web app.js)
private val U0 = intArrayOf(142, 189, 7, 111, 21, 58, 232, 139, 161, 70, 101, 29, 63, 238)
private val U1 = intArrayOf(43, 201, 206, 190, 74, 112, 5, 42, 241, 204, 185, 85, 61)
private val U2 = intArrayOf(96, 73, 56, 236, 149, 160, 65, 121, 4, 40, 231, 158, 171)
private val K0 = intArrayOf(33, 201, 136, 171, 66, 89, 93, 40, 211, 176, 163, 127, 113, 73, 97, 192)
private val K1 = intArrayOf(105, 3, 52, 217, 130, 142, 98, 95, 28, 103, 247, 138, 144, 105, 66)
private val K2 = intArrayOf(159, 77, 121, 19, 3, 255, 136, 136, 105, 67, 65, 20, 182, 173, 170)

private fun reconstructVault(s0: IntArray, s1: IntArray, s2: IntArray, len: Int, seedByte: Int): String {
    val bytes = ByteArray(len)
    var i0 = 0
    var i1 = 0
    var i2 = 0
    for (i in 0 until len) {
        val masked = when (i % 3) {
            0 -> s0[i0++]
            1 -> s1[i1++]
            else -> s2[i2++]
        }
        val salt = ((i * 73 + seedByte * 31 + 19) xor 0x5A) and 0xFF
        bytes[i] = ((masked xor salt) and 0xFF).toByte()
    }
    return String(bytes, Charsets.UTF_8)
}

private val SUPABASE_URL: String by lazy {
    System.getenv("SUPABASE_URL")?.takeIf { it.isNotBlank() }
        ?: reconstructVault(U0, U1, U2, 40, 0x37).trimEnd('/')
}
private val SUPABASE_ANON_KEY: String by lazy {
    System.getenv("SUPABASE_ANON_KEY")?.takeIf { it.isNotBlank() }
        ?: reconstructVault(K0, K1, K2, 46, 0x6B).trim()
}

enum class DesktopAvatar(
    val key: String,
    val displayName: String,
    val fileName: String,
    val resPath: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    AVATAR_1("avatar_1", "Crimson Wolf", "avatar_1.png", "drawable/avatar_1.png", Color(0xFF9F1239), Color(0xFF334155)),
    AVATAR_2("avatar_2", "Azure Sovereign", "avatar_2.png", "drawable/avatar_2.png", Color(0xFF0284C7), Color(0xFF6366F1)),
    AVATAR_3("avatar_3", "Amethyst Dragon", "avatar_3.png", "drawable/avatar_3.png", Color(0xFFA855F7), Color(0xFFE11D48)),
    AVATAR_4("avatar_4", "Frost Empress", "avatar_4.png", "drawable/avatar_4.png", Color(0xFF38BDF8), Color(0xFF818CF8)),
    AVATAR_5("avatar_5", "Emerald Flora", "avatar_5.png", "drawable/avatar_5.png", Color(0xFF10B981), Color(0xFF38BDF8)),
    AVATAR_6("avatar_6", "Amber Scholar", "avatar_6.png", "drawable/avatar_6.png", Color(0xFFF59E0B), Color(0xFF92400E)),
    AVATAR_7("avatar_7", "Autumn Vanguard", "avatar_7.png", "drawable/avatar_7.png", Color(0xFFEA580C), Color(0xFF06B6D4)),
    AVATAR_8("avatar_8", "Indigo Flame", "avatar_8.png", "drawable/avatar_8.png", Color(0xFF4F46E5), Color(0xFF0EA5E9)),
    AVATAR_9("avatar_9", "Solar Halo", "avatar_9.png", "drawable/avatar_9.png", Color(0xFFFBBF24), Color(0xFF64748B));

    companion object {
        fun fromKey(k: String?, fallbackUrl: String? = null): DesktopAvatar {
            val candidates = listOfNotNull(k, fallbackUrl).map { it.trim() }.filter { it.isNotEmpty() && it != "null" }
            for (raw in candidates) {
                val lower = raw.lowercase()
                entries.firstOrNull { it.key == lower || it.displayName.lowercase() == lower }?.let { return it }
                val match = Regex("""avatar[_-]?([1-9])""").find(lower)
                    ?: Regex("""^([1-9])$""").find(lower)
                val idx = match?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (idx != null && idx in 1..9) {
                    return entries[idx - 1]
                }
            }
            return AVATAR_1
        }
    }
}

private fun loadLocalDrawableBitmap(fileName: String): ImageBitmap? {
    val cacheKey = "local:$fileName"
    imageCache[cacheKey]?.let { return it }
    val streamPaths = listOf(
        "/drawable/$fileName",
        "drawable/$fileName",
        "/$fileName",
        fileName
    )
    for (p in streamPaths) {
        val bytes = runCatching {
            DesktopAvatar::class.java.getResourceAsStream(p)?.use { it.readBytes() }
                ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(p.removePrefix("/"))?.use { it.readBytes() }
        }.getOrNull()
        if (bytes != null && bytes.isNotEmpty()) {
            val bmp = runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
            if (bmp != null) {
                imageCache[cacheKey] = bmp
                return bmp
            }
        }
    }
    val fileCandidates = listOf(
        File("desktop/src/main/resources/drawable/$fileName"),
        File("src/main/resources/drawable/$fileName"),
        File("app/src/main/res/drawable/$fileName"),
        File("../app/src/main/res/drawable/$fileName"),
        File("public/assets/$fileName"),
        File("../public/assets/$fileName"),
        File("assets/$fileName")
    )
    for (f in fileCandidates) {
        if (f.exists() && f.isFile) {
            val bmp = runCatching { SkiaImage.makeFromEncoded(f.readBytes()).toComposeImageBitmap() }.getOrNull()
            if (bmp != null) {
                imageCache[cacheKey] = bmp
                return bmp
            }
        }
    }
    return null
}

@Composable
private fun DesktopAvatarImage(
    avatarKey: String?,
    username: String = "A",
    modifier: Modifier = Modifier
) {
    val preset = DesktopAvatar.fromKey(avatarKey)
    val bitmap = remember(preset.fileName) { loadLocalDrawableBitmap(preset.fileName) }
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(preset.primaryColor, preset.secondaryColor))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = preset.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            runCatching { painterResource(preset.resPath) }.getOrNull()?.let { painter ->
                Image(
                    painter = painter,
                    contentDescription = preset.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Text(
                text = username.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = IvoryText
            )
        }
    }
}

enum class SentimentTier(
    val key: String,
    val shortLabel: String,
    val fullLabel: String,
    val textColor: Color,
    val pillBg: Color
) {
    CINEMA("cinema", "CINEMA", "ABSOLUTE CINEMA", ChampagneGold, Color(0xFF332917)),
    WORTH("worth", "WORTH", "WORTH WATCHING", EmeraldWorth, EmeraldWorthBg),
    AVG("avg", "AVG", "ONE TIME WATCH", SilverAvg, Color(0xFF232734)),
    BORING("boring", "BORING", "BORING", AmberBoring, Color(0xFF332615)),
    WASTE("waste", "WASTE", "TOTAL WASTE", CrimsonWaste, Color(0xFF361920));

    companion object {
        fun fromStars(stars: Float): SentimentTier? {
            if (stars <= 0f) return null
            return when {
                stars >= 4.5f -> CINEMA
                stars >= 3.5f -> WORTH
                stars >= 2.5f -> AVG
                stars >= 1.5f -> BORING
                else -> WASTE
            }
        }
    }
}

data class DesktopAnimeItem(
    val id: Int,
    val title: String,
    val titleRomaji: String = "",
    val coverImage: String,
    val bannerImage: String = "",
    val format: String = "TV",
    val status: String = "RELEASING",
    val totalEpisodes: Int? = null,
    val watchedEpisodes: Int = 0,
    val watchStatus: String = "watching",
    val userStars: Float = 0f,
    val catalogStars: Float = 0f,
    val airingDayOfWeek: Int = 1,
    val seasonYear: Int? = null,
    val genres: List<String> = emptyList(),
    val synopsis: String = "",
    val notes: String = "",
    val updatedAt: String = "",
    val inWatchlist: Boolean = false
)

data class DesktopCollectionItem(
    val id: String,
    val title: String,
    val description: String = "",
    val count: Int = 0
)

data class DesktopCommunityReview(
    val id: String,
    val animeId: Int,
    val animeTitle: String,
    val animeCover: String,
    val stars: Float,
    val notes: String,
    val username: String,
    val avatarKey: String,
    val updatedAt: String,
    val formattedDate: String
)

data class DesktopPersistedState(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0L,
    val userId: String = "",
    val guestId: String = "",
    val email: String = "",
    val username: String = "Wanderer",
    val avatarKey: String = DesktopAvatar.AVATAR_3.key,
    val isGuest: Boolean = false,
    val watchlist: List<DesktopAnimeItem> = emptyList(),
    val collections: List<DesktopCollectionItem> = emptyList()
)

private data class DesktopActiveIdentity(
    val mode: String = "none",
    val userId: String = "",
    val guestId: String = ""
)

private object DesktopStorage {
    private val rootDir: File by lazy {
        val dir = File(System.getProperty("user.home"), ".anime_universe")
        if (!dir.exists()) dir.mkdirs()
        dir
    }
    private val legacyStateFile: File by lazy { File(rootDir, "desktop_state_v2.json") }
    private val identityFile: File by lazy { File(rootDir, "desktop_active_identity.json") }
    private val guestIdFile: File by lazy { File(rootDir, "desktop_guest_id.txt") }

    private fun userVaultFile(userId: String): File {
        val safe = userId.trim().replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(rootDir, "user_$safe.json")
    }

    private fun guestVaultFile(guestId: String): File {
        val safe = guestId.trim().replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(rootDir, "$safe.json")
    }

    private fun migrateLegacyStateOnce() {
        if (!legacyStateFile.exists()) return
        runCatching {
            val legacy = gson.fromJson(legacyStateFile.readText(Charsets.UTF_8), DesktopPersistedState::class.java)
            if (!identityFile.exists() && legacy != null && !legacy.isGuest && legacy.userId.isNotBlank() && legacy.accessToken.isNotBlank()) {
                val cleanUser = legacy.copy(isGuest = false, guestId = "")
                userVaultFile(cleanUser.userId).writeText(gson.toJson(cleanUser), Charsets.UTF_8)
                identityFile.writeText(
                    gson.toJson(DesktopActiveIdentity(mode = "user", userId = cleanUser.userId)),
                    Charsets.UTF_8
                )
            }
            legacyStateFile.delete()
        }
    }

    fun getOrCreateGuestId(): String {
        val existing = runCatching {
            if (guestIdFile.exists()) guestIdFile.readText(Charsets.UTF_8).trim() else ""
        }.getOrDefault("")
        if (existing.startsWith("guest_") && existing.length > 8) return existing
        val fresh = "guest_${UUID.randomUUID()}"
        runCatching { guestIdFile.writeText(fresh, Charsets.UTF_8) }
        return fresh
    }

    fun rotateGuestId(): String {
        val fresh = "guest_${UUID.randomUUID()}"
        runCatching { guestIdFile.writeText(fresh, Charsets.UTF_8) }
        return fresh
    }

    fun loadUser(userId: String): DesktopPersistedState {
        if (userId.isBlank()) return DesktopPersistedState()
        return runCatching {
            val f = userVaultFile(userId)
            if (!f.exists()) return DesktopPersistedState(userId = userId, isGuest = false)
            val parsed = gson.fromJson(f.readText(Charsets.UTF_8), DesktopPersistedState::class.java)
                ?: return DesktopPersistedState(userId = userId, isGuest = false)
            parsed.copy(isGuest = false, guestId = "", userId = userId)
        }.getOrDefault(DesktopPersistedState(userId = userId, isGuest = false))
    }

    fun loadGuest(guestId: String): DesktopPersistedState {
        val gid = guestId.takeIf { it.startsWith("guest_") } ?: getOrCreateGuestId()
        return runCatching {
            val f = guestVaultFile(gid)
            if (!f.exists()) {
                return DesktopPersistedState(
                    isGuest = true,
                    guestId = gid,
                    userId = "",
                    accessToken = "",
                    refreshToken = "",
                    email = "",
                    username = "Guest"
                )
            }
            val parsed = gson.fromJson(f.readText(Charsets.UTF_8), DesktopPersistedState::class.java)
                ?: return DesktopPersistedState(isGuest = true, guestId = gid, username = "Guest")
            parsed.copy(
                isGuest = true,
                guestId = gid,
                userId = "",
                accessToken = "",
                refreshToken = "",
                expiresAt = 0L,
                email = "",
                username = parsed.username.takeIf { it.isNotBlank() && it != "Wanderer" } ?: "Guest"
            )
        }.getOrDefault(DesktopPersistedState(isGuest = true, guestId = gid, username = "Guest"))
    }

    fun loadActive(): DesktopPersistedState {
        migrateLegacyStateOnce()
        val id = runCatching {
            if (!identityFile.exists()) return DesktopPersistedState()
            gson.fromJson(identityFile.readText(Charsets.UTF_8), DesktopActiveIdentity::class.java)
                ?: DesktopActiveIdentity()
        }.getOrDefault(DesktopActiveIdentity())
        return when {
            id.mode == "user" && id.userId.isNotBlank() -> {
                val u = loadUser(id.userId)
                if (u.accessToken.isNotBlank() && u.userId.isNotBlank()) u else DesktopPersistedState()
            }
            id.mode == "guest" -> {
                val gid = id.guestId.takeIf { it.startsWith("guest_") } ?: getOrCreateGuestId()
                loadGuest(gid)
            }
            else -> DesktopPersistedState()
        }
    }

    fun saveActive(state: DesktopPersistedState) {
        runCatching {
            if (!state.isGuest && state.userId.isNotBlank() && state.accessToken.isNotBlank()) {
                val cleanUser = state.copy(isGuest = false, guestId = "")
                userVaultFile(cleanUser.userId).writeText(gson.toJson(cleanUser), Charsets.UTF_8)
                identityFile.writeText(
                    gson.toJson(DesktopActiveIdentity(mode = "user", userId = cleanUser.userId)),
                    Charsets.UTF_8
                )
            } else if (state.isGuest) {
                val gid = state.guestId.takeIf { it.startsWith("guest_") } ?: getOrCreateGuestId()
                val cleanGuest = state.copy(
                    isGuest = true,
                    guestId = gid,
                    userId = "",
                    accessToken = "",
                    refreshToken = "",
                    expiresAt = 0L,
                    email = "",
                    username = state.username.takeIf { it.isNotBlank() && it != "Wanderer" } ?: "Guest"
                )
                guestVaultFile(gid).writeText(gson.toJson(cleanGuest), Charsets.UTF_8)
                identityFile.writeText(
                    gson.toJson(DesktopActiveIdentity(mode = "guest", guestId = gid)),
                    Charsets.UTF_8
                )
            }
        }
    }

    fun deleteGuestVault(guestId: String) {
        if (guestId.isBlank()) return
        runCatching {
            val f = guestVaultFile(guestId)
            if (f.exists()) f.delete()
        }
    }

    fun onSignOut(wasAuthenticated: Boolean) {
        runCatching {
            identityFile.writeText(gson.toJson(DesktopActiveIdentity(mode = "none")), Charsets.UTF_8)
            if (wasAuthenticated) {
                rotateGuestId()
            }
        }
    }
}

enum class DesktopScreen(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home),
    EXPLORE("explore", "Explore", Icons.Outlined.Explore),
    LIBRARY("library", "Library", Icons.Outlined.BookmarkBorder),
    COMMUNITY("community", "Community", Icons.Outlined.Group),
    PROFILE("profile", "Profile", Icons.Outlined.Person),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings),
    DETAIL("detail", "Detail", Icons.Outlined.Movie)
}

@Composable
fun NetworkPosterImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(url) { mutableStateOf(imageCache[url]) }
    LaunchedEffect(url) {
        if (url.isNotBlank() && bitmap == null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val req = Request.Builder().url(url).build()
                    httpClient.newCall(req).execute().use { resp ->
                        val bytes = resp.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            val decoded = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                            imageCache[url] = decoded
                            bitmap = decoded
                        }
                    }
                }
            }
        }
    }
    Box(modifier = modifier.background(ObsidianPillBg), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Outlined.Movie, contentDescription = null, tint = WarmSilver.copy(alpha = 0.35f))
        }
    }
}

// ============================================================================
// SUPABASE AUTH & POSTGREST CLOUD SYNC HELPERS
// ============================================================================

private fun JsonElement?.asStringOrNull(): String? =
    if (this == null || this.isJsonNull) null else runCatching { this.asString }.getOrNull()

private fun JsonElement?.asIntOrNull(): Int? =
    if (this == null || this.isJsonNull) null else runCatching { this.asInt }.getOrNull()

private fun JsonElement?.asLongOrNull(): Long? =
    if (this == null || this.isJsonNull) null else runCatching { this.asLong }.getOrNull()

private fun JsonElement?.asFloatOrNull(): Float? =
    if (this == null || this.isJsonNull) null else runCatching { this.asFloat }.getOrNull()

private fun computeDayOfWeek(anilistId: Int, nextAiringAt: Long?): Int {
    if (nextAiringAt != null && nextAiringAt > 0L) {
        return runCatching {
            Instant.ofEpochSecond(nextAiringAt).atZone(ZoneId.systemDefault()).dayOfWeek.value
        }.getOrDefault((abs(anilistId) % 7) + 1)
    }
    return (abs(anilistId) % 7) + 1
}

private fun formatIsoDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "Recent"
    return runCatching {
        val inst = Instant.parse(iso)
        val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
            .withZone(ZoneId.systemDefault())
        fmt.format(inst)
    }.getOrDefault("Recent")
}

private suspend fun resolveLoginEmail(identifier: String): String = withContext(Dispatchers.IO) {
    val clean = identifier.trim()
    if (clean.contains("@")) return@withContext clean
    runCatching {
        val bodyObj = JsonObject().apply { addProperty("p_identifier", clean) }
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/rpc/resolve_login_email")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(bodyObj).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val raw = resp.body?.string()?.trim()?.removeSurrounding("\"").orEmpty()
            if (resp.isSuccessful && raw.contains("@")) return@withContext raw
        }
    }
    runCatching {
        val encoded = URLEncoder.encode(clean, "UTF-8")
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/profiles?username=ilike.$encoded&select=email&limit=1")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java)
            val em = arr?.firstOrNull()?.asJsonObject?.get("email").asStringOrNull()
            if (!em.isNullOrBlank() && em.contains("@")) return@withContext em
        }
    }
    clean
}

private suspend fun signInSupabase(identifier: String, password: String): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    runCatching {
        val email = resolveLoginEmail(identifier)
        if (!email.contains("@")) {
            throw IllegalArgumentException("Username \"$identifier\" not found. Please sign in with your email address.")
        }
        val payload = JsonObject().apply {
            addProperty("email", email)
            addProperty("password", password)
        }
        val req = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val obj = gson.fromJson(text, JsonObject::class.java) ?: JsonObject()
            if (!resp.isSuccessful) {
                val msg = obj.get("msg").asStringOrNull()
                    ?: obj.get("error_description").asStringOrNull()
                    ?: obj.get("message").asStringOrNull()
                    ?: "Invalid login credentials"
                throw IllegalArgumentException(msg)
            }
            val token = obj.get("access_token").asStringOrNull().orEmpty()
            val refresh = obj.get("refresh_token").asStringOrNull().orEmpty()
            val expiresIn = obj.get("expires_in").asLongOrNull() ?: 3600L
            val userObj = obj.getAsJsonObject("user")
            val userId = userObj?.get("id").asStringOrNull().orEmpty()
            val userEmail = userObj?.get("email").asStringOrNull() ?: email
            val fallbackName = if (!identifier.contains("@") && identifier.isNotBlank()) identifier.trim() else userEmail.substringBefore("@")
            DesktopPersistedState(
                accessToken = token,
                refreshToken = refresh,
                expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn,
                userId = userId,
                email = userEmail,
                username = fallbackName,
                avatarKey = DesktopAvatar.AVATAR_3.key,
                isGuest = false
            )
        }
    }
}

private class OtpVerificationRequiredException(val email: String) :
    Exception("Enter the 8-digit verification code sent to $email")

private suspend fun signUpSupabase(emailInput: String, password: String): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    runCatching {
        val email = emailInput.trim().lowercase()
        if (!email.contains("@") || !email.contains(".")) {
            throw IllegalArgumentException("Please enter a valid email address to create an account.")
        }
        val payload = JsonObject().apply {
            addProperty("email", email)
            addProperty("password", password)
        }
        val req = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/signup")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val obj = gson.fromJson(text, JsonObject::class.java) ?: JsonObject()
            if (!resp.isSuccessful) {
                val msg = obj.get("msg").asStringOrNull()
                    ?: obj.get("error_description").asStringOrNull()
                    ?: obj.get("message").asStringOrNull()
                    ?: "Could not create account"
                throw IllegalArgumentException(msg)
            }
            val token = obj.get("access_token").asStringOrNull().orEmpty()
            val refresh = obj.get("refresh_token").asStringOrNull().orEmpty()
            val expiresIn = obj.get("expires_in").asLongOrNull() ?: 3600L
            val userObj = obj.getAsJsonObject("user") ?: obj
            val identities = userObj.getAsJsonArray("identities")
            if (identities != null && identities.size() == 0) {
                throw IllegalArgumentException("An account with this email already exists. Please Sign In instead.")
            }
            val userId = userObj.get("id").asStringOrNull().orEmpty()
            if (token.isBlank()) {
                throw OtpVerificationRequiredException(email)
            }
            DesktopPersistedState(
                accessToken = token,
                refreshToken = refresh,
                expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn,
                userId = userId,
                email = email,
                username = email.substringBefore("@"),
                avatarKey = DesktopAvatar.AVATAR_1.key,
                isGuest = false
            )
        }
    }
}

private suspend fun verifySignupOtpSupabase(emailInput: String, otpCode: String): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    runCatching {
        val cleanEmail = emailInput.trim().lowercase()
        val cleanOtp = otpCode.filter { it.isDigit() }
        if (cleanOtp.length < 6) {
            throw IllegalArgumentException("Please enter the verification code from your email.")
        }
        val payload = JsonObject().apply {
            addProperty("email", cleanEmail)
            addProperty("token", cleanOtp)
            addProperty("type", "signup")
        }
        val req = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/verify")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val obj = gson.fromJson(text, JsonObject::class.java) ?: JsonObject()
            if (!resp.isSuccessful) {
                val msg = obj.get("msg").asStringOrNull()
                    ?: obj.get("error_description").asStringOrNull()
                    ?: obj.get("message").asStringOrNull()
                    ?: "Invalid or expired verification code."
                throw IllegalArgumentException(msg)
            }
            val token = obj.get("access_token").asStringOrNull().orEmpty()
            val refresh = obj.get("refresh_token").asStringOrNull().orEmpty()
            val expiresIn = obj.get("expires_in").asLongOrNull() ?: 3600L
            val userObj = obj.getAsJsonObject("user") ?: obj
            val userId = userObj.get("id").asStringOrNull().orEmpty()
            if (token.isBlank() || userId.isBlank()) {
                throw IllegalArgumentException("Verification failed. Please check the code and try again.")
            }
            DesktopPersistedState(
                accessToken = token,
                refreshToken = refresh,
                expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn,
                userId = userId,
                email = cleanEmail,
                username = cleanEmail.substringBefore("@"),
                avatarKey = DesktopAvatar.AVATAR_1.key,
                isGuest = false
            )
        }
    }
}

private suspend fun resendSignupOtpSupabase(emailInput: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val cleanEmail = emailInput.trim().lowercase()
        val payload = JsonObject().apply {
            addProperty("type", "signup")
            addProperty("email", cleanEmail)
        }
        val req = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/resend")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().close()
    }
}

private suspend fun refreshTokenIfNeeded(state: DesktopPersistedState): DesktopPersistedState = withContext(Dispatchers.IO) {
    if (state.isGuest || state.userId.isBlank() || state.accessToken.isBlank() || state.refreshToken.isBlank()) {
        return@withContext state
    }
    val nowSec = System.currentTimeMillis() / 1000L
    if (state.expiresAt > nowSec + 120L) return@withContext state
    runCatching {
        val payload = JsonObject().apply { addProperty("refresh_token", state.refreshToken) }
        val req = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/token?grant_type=refresh_token")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext state
            val obj = gson.fromJson(resp.body?.string().orEmpty(), JsonObject::class.java) ?: return@withContext state
            val newAccess = obj.get("access_token").asStringOrNull() ?: return@withContext state
            val newRefresh = obj.get("refresh_token").asStringOrNull() ?: state.refreshToken
            val expiresIn = obj.get("expires_in").asLongOrNull() ?: 3600L
            val updated = state.copy(
                accessToken = newAccess,
                refreshToken = newRefresh,
                expiresAt = nowSec + expiresIn
            )
            DesktopStorage.saveActive(updated)
            updated
        }
    }.getOrDefault(state)
}

private data class CloudSyncSnapshot(
    val username: String?,
    val avatarKey: String?,
    val watchlist: List<DesktopAnimeItem>?,
    val collections: List<DesktopCollectionItem>?,
    val communityReviews: List<DesktopCommunityReview>
)

private suspend fun fetchFullCloudSnapshot(session: DesktopPersistedState): CloudSyncSnapshot = withContext(Dispatchers.IO) {
    val active = refreshTokenIfNeeded(session)
    val bearer = active.accessToken.ifBlank { SUPABASE_ANON_KEY }
    val uid = active.userId

    var resolvedUsername: String? = null
    var resolvedAvatar: String? = null
    var watchlistResult: List<DesktopAnimeItem>? = null
    var collectionsResult: List<DesktopCollectionItem>? = null

    if (uid.isNotBlank() && active.accessToken.isNotBlank()) {
        // 1. User Watchlist (Notice: NO user_season column so PostgREST never returns 400!)
        runCatching {
            val selectCols = "id,user_id,anime_id,status,episode_progress,total_episodes,personal_rating,review_notes,is_favorite,updated_at,anime(anilist_id,title_english,title_romaji,title_native,cover_image,banner_image,genres,release_year,season,episodes,average_score,popularity,format,status,studios,next_airing_at,next_episode_number)"
            val url = "$SUPABASE_URL/rest/v1/user_anime?user_id=eq.$uid&select=$selectCols&order=updated_at.desc"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $bearer")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java) ?: JsonArray()
                    watchlistResult = arr.mapNotNull { el ->
                        val row = el.asJsonObject
                        val animeObj = row.get("anime")?.takeIf { it.isJsonObject }?.asJsonObject
                        val id = animeObj?.get("anilist_id").asIntOrNull() ?: row.get("anime_id").asIntOrNull() ?: return@mapNotNull null
                        val title = animeObj?.get("title_english").asStringOrNull()?.takeIf { it.isNotBlank() }
                            ?: animeObj?.get("title_romaji").asStringOrNull()?.takeIf { it.isNotBlank() }
                            ?: "Anime #$id"
                        val romaji = animeObj?.get("title_romaji").asStringOrNull().orEmpty()
                        val rawRating = row.get("personal_rating").asFloatOrNull() ?: 0f
                        val userStars = if (rawRating > 5f) rawRating / 2f else rawRating
                        val avg100 = animeObj?.get("average_score").asFloatOrNull() ?: 0f
                        val catalogStars = if (avg100 > 10f) avg100 / 20f else if (avg100 > 5f) avg100 / 2f else avg100
                        val nextAiringAt = animeObj?.get("next_airing_at").asLongOrNull()
                        DesktopAnimeItem(
                            id = id,
                            title = title,
                            titleRomaji = romaji,
                            coverImage = animeObj?.get("cover_image").asStringOrNull().orEmpty(),
                            bannerImage = animeObj?.get("banner_image").asStringOrNull().orEmpty(),
                            format = animeObj?.get("format").asStringOrNull() ?: "TV",
                            status = animeObj?.get("status").asStringOrNull() ?: "RELEASING",
                            totalEpisodes = row.get("total_episodes").asIntOrNull() ?: animeObj?.get("episodes").asIntOrNull(),
                            watchedEpisodes = row.get("episode_progress").asIntOrNull() ?: 0,
                            watchStatus = row.get("status").asStringOrNull() ?: "watching",
                            userStars = userStars,
                            catalogStars = catalogStars,
                            airingDayOfWeek = computeDayOfWeek(id, nextAiringAt),
                            seasonYear = animeObj?.get("release_year").asIntOrNull(),
                            notes = row.get("review_notes").asStringOrNull().orEmpty(),
                            updatedAt = row.get("updated_at").asStringOrNull().orEmpty(),
                            inWatchlist = true
                        )
                    }
                }
            }
        }

        // 2. Collections
        runCatching {
            val url = "$SUPABASE_URL/rest/v1/collections?creator_id=eq.$uid&select=*,collection_anime(anime_id)&order=updated_at.desc"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $bearer")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java) ?: JsonArray()
                    collectionsResult = arr.mapNotNull { el ->
                        val row = el.asJsonObject
                        val cid = row.get("id").asStringOrNull() ?: return@mapNotNull null
                        val title = row.get("name").asStringOrNull()?.takeIf { it.isNotBlank() }
                            ?: row.get("title").asStringOrNull()?.takeIf { it.isNotBlank() }
                            ?: "Collection"
                        val desc = row.get("description").asStringOrNull().orEmpty()
                        val itemsArr = row.get("collection_anime")?.takeIf { it.isJsonArray }?.asJsonArray
                        DesktopCollectionItem(
                            id = cid,
                            title = title,
                            description = desc,
                            count = itemsArr?.size() ?: 0
                        )
                    }
                }
            }
        }

        // 3. User Profile
        runCatching {
            val url = "$SUPABASE_URL/rest/v1/profiles?id=eq.$uid&select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $bearer")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java)
                    val prof = arr?.firstOrNull()?.asJsonObject
                    if (prof != null) {
                        resolvedUsername = prof.get("username").asStringOrNull()?.takeIf { it.isNotBlank() }
                        resolvedAvatar = DesktopAvatar.fromKey(
                            prof.get("avatar_key").asStringOrNull(),
                            prof.get("avatar_url").asStringOrNull()
                        ).key
                    }
                }
            }
        }
    }

    // 4. Community Reviews (All users' rated or noted entries)
    val communityList = mutableListOf<DesktopCommunityReview>()
    runCatching {
        val profMap = mutableMapOf<String, Pair<String, String>>()
        val profReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/profiles?select=id,username,avatar_key,avatar_url&limit=200")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $bearer")
            .build()
        httpClient.newCall(profReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java) ?: JsonArray()
                arr.forEach { el ->
                    val o = el.asJsonObject
                    val pid = o.get("id").asStringOrNull() ?: return@forEach
                    val uname = o.get("username").asStringOrNull() ?: "Reviewer"
                    val av = DesktopAvatar.fromKey(
                        o.get("avatar_key").asStringOrNull(),
                        o.get("avatar_url").asStringOrNull()
                    ).key
                    profMap[pid] = uname to av
                }
            }
        }

        val selectCols = "id,user_id,anime_id,status,episode_progress,personal_rating,review_notes,updated_at,anime(anilist_id,title_english,title_romaji,cover_image,episodes)"
        val revReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/user_anime?select=$selectCols&order=updated_at.desc&limit=80")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $bearer")
            .build()
        httpClient.newCall(revReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java) ?: JsonArray()
                arr.forEach { el ->
                    val row = el.asJsonObject
                    val rawRating = row.get("personal_rating").asFloatOrNull() ?: 0f
                    val stars = if (rawRating > 5f) rawRating / 2f else rawRating
                    val notes = row.get("review_notes").asStringOrNull()?.trim().orEmpty()
                    if (stars <= 0f && notes.isBlank()) return@forEach

                    val animeObj = row.get("anime")?.takeIf { it.isJsonObject }?.asJsonObject
                    val animeId = animeObj?.get("anilist_id").asIntOrNull() ?: row.get("anime_id").asIntOrNull() ?: 0
                    val title = animeObj?.get("title_english").asStringOrNull()?.takeIf { it.isNotBlank() }
                        ?: animeObj?.get("title_romaji").asStringOrNull()?.takeIf { it.isNotBlank() }
                        ?: "Anime #$animeId"
                    val cover = animeObj?.get("cover_image").asStringOrNull().orEmpty()
                    val rowUserId = row.get("user_id").asStringOrNull().orEmpty()
                    val (uname, avKey) = profMap[rowUserId] ?: (
                        if (rowUserId == uid) (resolvedUsername ?: session.username) to (resolvedAvatar ?: session.avatarKey)
                        else "Anime Lover" to DesktopAvatar.AVATAR_3.key
                    )
                    val updatedIso = row.get("updated_at").asStringOrNull().orEmpty()
                    communityList.add(
                        DesktopCommunityReview(
                            id = row.get("id").asStringOrNull() ?: "$rowUserId-$animeId",
                            animeId = animeId,
                            animeTitle = title,
                            animeCover = cover,
                            stars = stars,
                            notes = notes,
                            username = uname,
                            avatarKey = avKey,
                            updatedAt = updatedIso,
                            formattedDate = formatIsoDate(updatedIso)
                        )
                    )
                }
            }
        }
    }

    CloudSyncSnapshot(
        username = resolvedUsername,
        avatarKey = resolvedAvatar,
        watchlist = watchlistResult,
        collections = collectionsResult,
        communityReviews = communityList
    )
}

private suspend fun saveAnimeToSupabase(session: DesktopPersistedState, item: DesktopAnimeItem) = withContext(Dispatchers.IO) {
    if (session.userId.isBlank() || session.accessToken.isBlank()) return@withContext
    val active = refreshTokenIfNeeded(session)
    runCatching {
        // 1. Upsert anime catalog row
        val animePayload = JsonObject().apply {
            addProperty("anilist_id", item.id)
            addProperty("title_english", item.title)
            addProperty("title_romaji", item.titleRomaji.ifBlank { item.title })
            addProperty("cover_image", item.coverImage)
            addProperty("banner_image", item.bannerImage)
            if (item.totalEpisodes != null) addProperty("episodes", item.totalEpisodes)
            addProperty("format", item.format)
            addProperty("status", item.status)
            addProperty("average_score", (item.catalogStars * 20f).toInt())
        }
        val animeReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/anime?on_conflict=anilist_id")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(gson.toJson(animePayload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(animeReq).execute().close()

        // 2. Upsert user_anime row (personal_rating stored on 0..10 scale; NO user_season column)
        val userAnimePayload = JsonObject().apply {
            addProperty("user_id", active.userId)
            addProperty("anime_id", item.id)
            addProperty("status", item.watchStatus)
            addProperty("episode_progress", item.watchedEpisodes)
            if (item.totalEpisodes != null) addProperty("total_episodes", item.totalEpisodes)
            if (item.userStars > 0f) addProperty("personal_rating", item.userStars * 2f)
            addProperty("review_notes", item.notes)
            addProperty("updated_at", Instant.now().toString())
        }
        val uaReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/user_anime?on_conflict=user_id,anime_id")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(gson.toJson(userAnimePayload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(uaReq).execute().close()
    }
}

private suspend fun deleteAnimeFromSupabase(session: DesktopPersistedState, animeId: Int) = withContext(Dispatchers.IO) {
    if (session.userId.isBlank() || session.accessToken.isBlank()) return@withContext
    val active = refreshTokenIfNeeded(session)
    runCatching {
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/user_anime?user_id=eq.${active.userId}&anime_id=eq.$animeId")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .delete()
            .build()
        httpClient.newCall(req).execute().close()
    }
}

private suspend fun createCollectionInSupabase(
    session: DesktopPersistedState,
    title: String,
    description: String
): DesktopCollectionItem? = withContext(Dispatchers.IO) {
    if (session.isGuest || session.userId.isBlank() || session.accessToken.isBlank()) return@withContext null
    val active = refreshTokenIfNeeded(session)
    runCatching {
        val payload = JsonObject().apply {
            addProperty("creator_id", active.userId)
            addProperty("name", title.trim())
            addProperty("description", description.trim())
            addProperty("visibility", "public")
        }
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/collections")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java)
            val row = arr?.firstOrNull()?.asJsonObject ?: return@withContext null
            DesktopCollectionItem(
                id = row.get("id").asStringOrNull() ?: "local-${System.currentTimeMillis()}",
                title = row.get("name").asStringOrNull()?.takeIf { it.isNotBlank() }
                    ?: row.get("title").asStringOrNull()?.takeIf { it.isNotBlank() }
                    ?: title,
                description = row.get("description").asStringOrNull() ?: description,
                count = 0
            )
        }
    }.getOrNull()
}

private suspend fun updateProfileInSupabase(
    session: DesktopPersistedState,
    username: String,
    avatarKey: String
) = withContext(Dispatchers.IO) {
    if (session.userId.isBlank() || session.accessToken.isBlank()) return@withContext
    val active = refreshTokenIfNeeded(session)
    runCatching {
        val payload = JsonObject().apply {
            addProperty("id", active.userId)
            addProperty("username", username.trim())
            addProperty("avatar_key", avatarKey)
            if (active.email.isNotBlank()) addProperty("email", active.email)
            addProperty("updated_at", Instant.now().toString())
        }
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/profiles?on_conflict=id")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().close()
    }
}

private suspend fun fetchAniListDiscover(
    mode: String,
    genre: String,
    search: String,
    safeMode: Boolean
): List<DesktopAnimeItem> = withContext(Dispatchers.IO) {
    val sort = when (mode) {
        "popular" -> "POPULARITY_DESC"
        "top" -> "SCORE_DESC"
        "seasonal" -> "POPULARITY_DESC"
        "donghua" -> "TRENDING_DESC"
        "airing" -> "TRENDING_DESC"
        else -> "TRENDING_DESC"
    }
    val statusFilter = if (mode == "airing" || mode == "seasonal") ", status: RELEASING" else ""
    val countryFilter = if (mode == "donghua") ", countryOfOrigin: \"CN\"" else ""
    val adultFilter = if (safeMode) ", isAdult: false" else ""

    val query = """
        query(${'$'}search: String, ${'$'}genre: String) {
          Page(page: 1, perPage: 24) {
            media(type: ANIME, sort: [$sort], search: ${'$'}search, genre: ${'$'}genre$statusFilter$countryFilter$adultFilter) {
              id
              title { english romaji }
              coverImage { extraLarge large }
              bannerImage
              format
              status
              seasonYear
              episodes
              averageScore
              genres
              nextAiringEpisode { airingAt episode }
              description(asHtml: false)
            }
          }
        }
    """.trimIndent()
    val vars = JsonObject().apply {
        if (search.isNotBlank()) addProperty("search", search.trim())
        if (genre != "All Genres") addProperty("genre", genre)
    }
    val payload = JsonObject().apply {
        addProperty("query", query)
        add("variables", vars)
    }
    runCatching {
        val req = Request.Builder()
            .url("https://graphql.anilist.co")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            val root = gson.fromJson(body, JsonObject::class.java)
            val mediaArr = root?.getAsJsonObject("data")?.getAsJsonObject("Page")?.getAsJsonArray("media")
                ?: return@withContext emptyList()
            mediaArr.map { el ->
                val obj = el.asJsonObject
                val id = obj.get("id").asInt
                val titleObj = obj.getAsJsonObject("title")
                val romaji = titleObj?.get("romaji").asStringOrNull().orEmpty()
                val title = titleObj?.get("english").asStringOrNull()?.takeIf { it.isNotBlank() }
                    ?: romaji.ifBlank { "Anime #$id" }
                val coverObj = obj.getAsJsonObject("coverImage")
                val cover = coverObj?.get("extraLarge").asStringOrNull()
                    ?: coverObj?.get("large").asStringOrNull().orEmpty()
                val banner = obj.get("bannerImage").asStringOrNull().orEmpty()
                val format = obj.get("format").asStringOrNull() ?: "TV"
                val status = obj.get("status").asStringOrNull() ?: "RELEASING"
                val eps = obj.get("episodes").asIntOrNull()
                val year = obj.get("seasonYear").asIntOrNull()
                val score100 = obj.get("averageScore").asIntOrNull() ?: 0
                val nextAiring = obj.get("nextAiringEpisode")?.takeIf { it.isJsonObject }?.asJsonObject
                    ?.get("airingAt").asLongOrNull()
                val desc = obj.get("description").asStringOrNull().orEmpty()
                    .replace(Regex("<[^>]+>"), "")
                val genresList = obj.get("genres")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { it.asStringOrNull() } ?: emptyList()

                DesktopAnimeItem(
                    id = id,
                    title = title,
                    titleRomaji = romaji,
                    coverImage = cover,
                    bannerImage = banner,
                    format = format,
                    status = status,
                    totalEpisodes = eps,
                    catalogStars = if (score100 > 0) (score100 / 20f) else 0f,
                    airingDayOfWeek = computeDayOfWeek(id, nextAiring),
                    seasonYear = year,
                    genres = genresList,
                    synopsis = desc
                )
            }
        }
    }.getOrDefault(emptyList())
}

// ============================================================================
// ENTRY POINT & ROOT WORKSPACE
// ============================================================================

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Anime Universe",
        state = rememberWindowState(size = DpSize(1340.dp, 860.dp))
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = ChampagneGold,
                onPrimary = ChampagneOnGold,
                secondary = ChampagneGoldBronze,
                background = PureObsidian,
                surface = ObsidianSidebar,
                surfaceVariant = ObsidianCard,
                onBackground = IvoryText,
                onSurface = IvoryText,
                onSurfaceVariant = WarmSilver
            )
        ) {
            AnimeUniverseDesktopRoot()
        }
    }
}

@Composable
fun AnimeUniverseDesktopRoot() {
    val scope = rememberCoroutineScope()

    // Synchronous restore from isolated per-user / per-guest vault so logged-in PC users NEVER see a login screen flash on startup
    val initialPersisted = remember { DesktopStorage.loadActive() }
    var sessionState by remember { mutableStateOf(initialPersisted) }
    var isAuthenticated by remember { mutableStateOf(!initialPersisted.isGuest && initialPersisted.accessToken.isNotBlank() && initialPersisted.userId.isNotBlank()) }
    var isGuestMode by remember { mutableStateOf(initialPersisted.isGuest) }

    var currentScreen by remember { mutableStateOf(DesktopScreen.HOME) }
    var selectedAnime by remember { mutableStateOf<DesktopAnimeItem?>(null) }

    var watchlist by remember { mutableStateOf(initialPersisted.watchlist) }
    var collections by remember { mutableStateOf(initialPersisted.collections) }
    var communityReviews by remember { mutableStateOf<List<DesktopCommunityReview>>(emptyList()) }

    // Home State
    var homeAllWeek by remember { mutableStateOf(true) }
    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek.value) }

    // Discover State
    var discoverItems by remember { mutableStateOf<List<DesktopAnimeItem>>(emptyList()) }
    var discoverLoading by remember { mutableStateOf(false) }
    var discoverMode by remember { mutableStateOf("popular") }
    var discoverGenre by remember { mutableStateOf("All Genres") }
    var discoverSearch by remember { mutableStateOf("") }
    var safeMode by remember { mutableStateOf(true) }
    var quickJumpQuery by remember { mutableStateOf("") }

    // Library State
    var libStatusFilter by remember { mutableStateOf("all") }
    var libRatingFilter by remember { mutableStateOf("all") }
    var libSearchQuery by remember { mutableStateOf("") }
    var libSortMode by remember { mutableStateOf("recent") }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }

    // Community State
    var commRatingFilter by remember { mutableStateOf("all") }
    var commSortMode by remember { mutableStateOf("newest") }

    suspend fun syncWithCloud() {
        val capturedAuth = isAuthenticated
        val capturedGuest = isGuestMode
        val capturedUserId = sessionState.userId
        val capturedGuestId = sessionState.guestId
        if (!capturedAuth && !capturedGuest) return

        val snap = fetchFullCloudSnapshot(sessionState)
        // Guard against race if user switched accounts or signed out while network call was in-flight
        if (isAuthenticated != capturedAuth || isGuestMode != capturedGuest) return
        if (capturedAuth && sessionState.userId != capturedUserId) return
        if (capturedGuest && sessionState.guestId != capturedGuestId) return

        if (capturedAuth && !sessionState.isGuest && capturedUserId.isNotBlank()) {
            var nextState = sessionState
            if (!snap.username.isNullOrBlank()) {
                nextState = nextState.copy(username = snap.username)
            }
            if (!snap.avatarKey.isNullOrBlank()) {
                nextState = nextState.copy(avatarKey = snap.avatarKey)
            }
            if (snap.watchlist != null) {
                watchlist = snap.watchlist
                nextState = nextState.copy(watchlist = snap.watchlist)
            }
            if (snap.collections != null) {
                collections = snap.collections
                nextState = nextState.copy(collections = snap.collections)
            }
            sessionState = nextState
            DesktopStorage.saveActive(nextState)
        }
        if (snap.communityReviews.isNotEmpty()) {
            communityReviews = snap.communityReviews
        }
    }

    // Initial cloud sync + automatic 15-second background sync loop
    LaunchedEffect(isAuthenticated, isGuestMode, sessionState.userId, sessionState.guestId) {
        if (isAuthenticated || isGuestMode) {
            syncWithCloud()
            while (isActive) {
                delay(15_000L)
                syncWithCloud()
            }
        }
    }

    // Discover catalog loader
    LaunchedEffect(discoverMode, discoverGenre, safeMode) {
        discoverLoading = true
        discoverItems = fetchAniListDiscover(discoverMode, discoverGenre, discoverSearch, safeMode)
        discoverLoading = false
    }

    if (!isAuthenticated && !isGuestMode) {
        DesktopAuthScreen(
            onAuthenticated = { newSession ->
                val prevGuestId = DesktopStorage.getOrCreateGuestId()
                val guestItemsToMigrate = DesktopStorage.loadGuest(prevGuestId).watchlist
                val existingUserState = DesktopStorage.loadUser(newSession.userId)
                val mergedSession = newSession.copy(
                    isGuest = false,
                    guestId = "",
                    username = existingUserState.username.takeIf { it.isNotBlank() && it != "Wanderer" } ?: newSession.username,
                    avatarKey = existingUserState.avatarKey.ifBlank { newSession.avatarKey },
                    watchlist = existingUserState.watchlist,
                    collections = existingUserState.collections
                )
                selectedAnime = null
                communityReviews = emptyList()
                watchlist = mergedSession.watchlist
                collections = mergedSession.collections
                sessionState = mergedSession
                isAuthenticated = true
                isGuestMode = false
                currentScreen = DesktopScreen.HOME
                DesktopStorage.saveActive(mergedSession)

                scope.launch {
                    syncWithCloud()
                    if (guestItemsToMigrate.isNotEmpty()) {
                        val existingIds = watchlist.map { it.id }.toSet()
                        val uniqueGuestItems = guestItemsToMigrate.filter { !existingIds.contains(it.id) }
                        if (uniqueGuestItems.isNotEmpty()) {
                            uniqueGuestItems.forEach { gItem ->
                                saveAnimeToSupabase(sessionState, gItem.copy(inWatchlist = true))
                            }
                            watchlist = (uniqueGuestItems + watchlist).distinctBy { it.id }
                            sessionState = sessionState.copy(watchlist = watchlist)
                            DesktopStorage.saveActive(sessionState)
                            syncWithCloud()
                        }
                        DesktopStorage.deleteGuestVault(prevGuestId)
                        DesktopStorage.rotateGuestId()
                    }
                }
            },
            onContinueGuest = {
                val gid = DesktopStorage.getOrCreateGuestId()
                val guestState = DesktopStorage.loadGuest(gid)
                selectedAnime = null
                communityReviews = emptyList()
                sessionState = guestState
                watchlist = guestState.watchlist
                collections = guestState.collections
                isAuthenticated = false
                isGuestMode = true
                currentScreen = DesktopScreen.HOME
                DesktopStorage.saveActive(guestState)
            }
        )
        return
    }

    if (showCreateCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateCollectionDialog = false },
            onCreate = { title, desc ->
                showCreateCollectionDialog = false
                scope.launch {
                    val created = if (isAuthenticated && !sessionState.isGuest) {
                        createCollectionInSupabase(sessionState, title, desc)
                    } else null
                    val finalCol = created ?: DesktopCollectionItem(
                        id = "local-${System.currentTimeMillis()}",
                        title = title,
                        description = desc,
                        count = 0
                    )
                    collections = listOf(finalCol) + collections
                    sessionState = sessionState.copy(collections = collections)
                    DesktopStorage.saveActive(sessionState)
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(PureObsidian)
    ) {
        val isWide = maxWidth >= 820.dp
        val mainTabs = listOf(DesktopScreen.HOME, DesktopScreen.EXPLORE, DesktopScreen.LIBRARY, DesktopScreen.COMMUNITY)
        val sidebarTabs = mainTabs + DesktopScreen.PROFILE

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWide) {
                // 1:1 Left Sidebar without top brand header row
                Surface(
                    color = ObsidianSidebar,
                    modifier = Modifier.width(264.dp).fillMaxHeight()
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp, vertical = 22.dp)
                        ) {
                            // Quick Jump Search Pill (matches Screenshots 2-5)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianInputBg,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = "Quick Jump",
                                        tint = WarmSilver,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (quickJumpQuery.isEmpty()) {
                                            Text("Quick Jump...", color = WarmSilver, fontSize = 14.sp)
                                        }
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = quickJumpQuery,
                                            onValueChange = { q ->
                                                quickJumpQuery = q
                                                discoverSearch = q
                                                currentScreen = DesktopScreen.EXPLORE
                                                scope.launch {
                                                    discoverItems = fetchAniListDiscover(discoverMode, discoverGenre, q, safeMode)
                                                }
                                            },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = IvoryText,
                                                fontSize = 14.sp
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // Navigation Links (Home, Explore, Library, Community, Profile)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                sidebarTabs.forEach { tab ->
                                    val selected = currentScreen == tab
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) Color(0xFF2B271F) else Color.Transparent,
                                        border = if (selected) BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { currentScreen = tab }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (selected) {
                                                    when (tab) {
                                                        DesktopScreen.HOME -> Icons.Filled.Home
                                                        DesktopScreen.EXPLORE -> Icons.Outlined.Explore
                                                        DesktopScreen.LIBRARY -> Icons.Filled.Bookmark
                                                        DesktopScreen.COMMUNITY -> Icons.Filled.Group
                                                        else -> Icons.Outlined.Person
                                                    }
                                                } else tab.icon,
                                                contentDescription = tab.title,
                                                tint = if (selected) ChampagneGold else WarmSilver,
                                                modifier = Modifier.size(21.dp)
                                            )
                                            Text(
                                                tab.title,
                                                color = if (selected) ChampagneGoldSoft else WarmSilver,
                                                fontSize = 15.5.sp,
                                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.06f)))
                    }
                }
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(PureObsidian)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    when (currentScreen) {
                        DesktopScreen.HOME -> {
                            DesktopHomeScreen(
                                watchlist = watchlist,
                                avatarKey = sessionState.avatarKey,
                                homeAllWeek = homeAllWeek,
                                selectedDay = selectedDay,
                                onSelectAllWeek = { homeAllWeek = true },
                                onSelectDay = { day ->
                                    homeAllWeek = false
                                    selectedDay = day
                                },
                                onOpenExplore = { currentScreen = DesktopScreen.EXPLORE },
                                onOpenLibrary = { currentScreen = DesktopScreen.LIBRARY },
                                onOpenProfile = { currentScreen = DesktopScreen.PROFILE },
                                onOpenDetail = { item ->
                                    selectedAnime = item
                                    currentScreen = DesktopScreen.DETAIL
                                }
                            )
                        }

                        DesktopScreen.EXPLORE -> {
                            DesktopDiscoverScreen(
                                discoverItems = discoverItems,
                                watchlist = watchlist,
                                loading = discoverLoading,
                                avatarKey = sessionState.avatarKey,
                                discoverMode = discoverMode,
                                discoverGenre = discoverGenre,
                                searchQuery = discoverSearch,
                                safeMode = safeMode,
                                onSearchChange = { q ->
                                    discoverSearch = q
                                    scope.launch {
                                        discoverLoading = true
                                        discoverItems = fetchAniListDiscover(discoverMode, discoverGenre, q, safeMode)
                                        discoverLoading = false
                                    }
                                },
                                onSelectMode = { m -> discoverMode = m },
                                onSelectGenre = { g -> discoverGenre = g },
                                onToggleSafe = { safeMode = !safeMode },
                                onOpenProfile = { currentScreen = DesktopScreen.PROFILE },
                                onOpenDetail = { item ->
                                    val existing = watchlist.firstOrNull { it.id == item.id }
                                    selectedAnime = existing ?: item
                                    currentScreen = DesktopScreen.DETAIL
                                }
                            )
                        }

                        DesktopScreen.LIBRARY -> {
                            DesktopLibraryScreen(
                                watchlist = watchlist,
                                collections = collections,
                                avatarKey = sessionState.avatarKey,
                                statusFilter = libStatusFilter,
                                ratingFilter = libRatingFilter,
                                searchQuery = libSearchQuery,
                                sortMode = libSortMode,
                                onStatusChange = { libStatusFilter = it },
                                onRatingChange = { libRatingFilter = it },
                                onSearchChange = { libSearchQuery = it },
                                onSortChange = { libSortMode = it },
                                onCreateCollectionClick = { showCreateCollectionDialog = true },
                                onOpenProfile = { currentScreen = DesktopScreen.PROFILE },
                                onOpenDetail = { item ->
                                    selectedAnime = item
                                    currentScreen = DesktopScreen.DETAIL
                                }
                            )
                        }

                        DesktopScreen.COMMUNITY -> {
                            val combinedReviews = remember(communityReviews, watchlist, sessionState) {
                                if (communityReviews.isNotEmpty()) {
                                    communityReviews
                                } else {
                                    watchlist.filter { it.userStars > 0f || it.notes.isNotBlank() }.map { item ->
                                        DesktopCommunityReview(
                                            id = "local-${item.id}",
                                            animeId = item.id,
                                            animeTitle = item.title,
                                            animeCover = item.coverImage,
                                            stars = item.userStars,
                                            notes = item.notes,
                                            username = sessionState.username,
                                            avatarKey = sessionState.avatarKey,
                                            updatedAt = item.updatedAt,
                                            formattedDate = formatIsoDate(item.updatedAt)
                                        )
                                    }
                                }
                            }
                            DesktopCommunityScreen(
                                reviews = combinedReviews,
                                avatarKey = sessionState.avatarKey,
                                ratingFilter = commRatingFilter,
                                sortMode = commSortMode,
                                onRatingChange = { commRatingFilter = it },
                                onSortChange = { commSortMode = it },
                                onOpenProfile = { currentScreen = DesktopScreen.PROFILE },
                                onOpenAnime = { animeId ->
                                    val found = watchlist.firstOrNull { it.id == animeId }
                                        ?: discoverItems.firstOrNull { it.id == animeId }
                                    if (found != null) {
                                        selectedAnime = found
                                        currentScreen = DesktopScreen.DETAIL
                                    }
                                }
                            )
                        }

                        DesktopScreen.PROFILE -> {
                            DesktopProfileScreen(
                                session = sessionState,
                                watchlist = watchlist,
                                collectionsCount = collections.size,
                                onUpdateProfile = { newName, newAv ->
                                    val updated = sessionState.copy(username = newName, avatarKey = newAv)
                                    sessionState = updated
                                    DesktopStorage.saveActive(updated)
                                    if (isAuthenticated && !updated.isGuest) {
                                        scope.launch {
                                            updateProfileInSupabase(updated, newName, newAv)
                                            syncWithCloud()
                                        }
                                    }
                                },
                                onSignOut = {
                                    val wasAuth = isAuthenticated && !sessionState.isGuest
                                    DesktopStorage.saveActive(sessionState)
                                    DesktopStorage.onSignOut(wasAuth)
                                    sessionState = DesktopPersistedState()
                                    watchlist = emptyList()
                                    collections = emptyList()
                                    communityReviews = emptyList()
                                    selectedAnime = null
                                    isAuthenticated = false
                                    isGuestMode = false
                                }
                            )
                        }

                        DesktopScreen.SETTINGS -> {
                            DesktopProfileScreen(
                                session = sessionState,
                                watchlist = watchlist,
                                collectionsCount = collections.size,
                                onUpdateProfile = { newName, newAv ->
                                    val updated = sessionState.copy(username = newName, avatarKey = newAv)
                                    sessionState = updated
                                    DesktopStorage.saveActive(updated)
                                    if (isAuthenticated && !updated.isGuest) {
                                        scope.launch { updateProfileInSupabase(updated, newName, newAv) }
                                    }
                                },
                                onSignOut = {
                                    val wasAuth = isAuthenticated && !sessionState.isGuest
                                    DesktopStorage.saveActive(sessionState)
                                    DesktopStorage.onSignOut(wasAuth)
                                    sessionState = DesktopPersistedState()
                                    watchlist = emptyList()
                                    collections = emptyList()
                                    communityReviews = emptyList()
                                    selectedAnime = null
                                    isAuthenticated = false
                                    isGuestMode = false
                                }
                            )
                        }

                        DesktopScreen.DETAIL -> {
                            val item = selectedAnime
                            if (item != null) {
                                DesktopDetailScreen(
                                    item = item,
                                    onBack = { currentScreen = DesktopScreen.LIBRARY },
                                    onSave = { updatedItem ->
                                        selectedAnime = updatedItem
                                        val nextList = listOf(updatedItem) + watchlist.filter { it.id != updatedItem.id }
                                        watchlist = nextList
                                        sessionState = sessionState.copy(watchlist = nextList)
                                        DesktopStorage.saveActive(sessionState)
                                        if (isAuthenticated && !sessionState.isGuest) {
                                            scope.launch {
                                                saveAnimeToSupabase(sessionState, updatedItem)
                                                syncWithCloud()
                                            }
                                        }
                                        currentScreen = DesktopScreen.LIBRARY
                                    },
                                    onDelete = { toRemove ->
                                        val nextList = watchlist.filter { it.id != toRemove.id }
                                        watchlist = nextList
                                        sessionState = sessionState.copy(watchlist = nextList)
                                        DesktopStorage.saveActive(sessionState)
                                        if (isAuthenticated && !sessionState.isGuest) {
                                            scope.launch {
                                                deleteAnimeFromSupabase(sessionState, toRemove.id)
                                                syncWithCloud()
                                            }
                                        }
                                        currentScreen = DesktopScreen.LIBRARY
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREENSHOT 1: AUTH SCREEN (1:1 Match)
// ============================================================================

@Composable
private fun DesktopAuthScreen(
    onAuthenticated: (DesktopPersistedState) -> Unit,
    onContinueGuest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSignUp by remember { mutableStateOf(false) }
    var awaitingOtp by remember { mutableStateOf(false) }
    var pendingEmail by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    val mascotBitmap = remember { loadLocalDrawableBitmap("img_login_mascot.png") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF282019), Color(0xFF0A0B10)),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF11131C),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 158.dp)
                ) {
                    if (awaitingOtp) {
                        Column(
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 26.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Check Your Inbox",
                                fontFamily = FontFamily.Serif,
                                fontSize = 26.sp,
                                color = IvoryText
                            )
                            Text(
                                text = "Enter the 8-digit verification code sent to $pendingEmail",
                                color = WarmSilver,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF151823),
                                border = BorderStroke(1.dp, Color(0xFF2A2F3F)),
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Filled.VpnKey, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(19.dp))
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (otpCode.isEmpty()) {
                                            Text("ex-12345678", color = AshMuted, fontSize = 15.sp)
                                        }
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = otpCode,
                                            onValueChange = { raw ->
                                                otpCode = raw.filter { it.isDigit() }.take(8)
                                                errorMessage = null
                                            },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = IvoryText,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            if (!errorMessage.isNullOrBlank()) {
                                Text(errorMessage!!, color = CrimsonWaste, fontSize = 12.5.sp)
                            }
                            if (!infoMessage.isNullOrBlank()) {
                                Text(infoMessage!!, color = ChampagneGold, fontSize = 12.5.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFF3E2BA), Color(0xFFDFB870))))
                                    .clickable(enabled = !isLoading) {
                                        isLoading = true
                                        errorMessage = null
                                        scope.launch {
                                            val res = verifySignupOtpSupabase(pendingEmail, otpCode)
                                            isLoading = false
                                            res.onSuccess { state -> onAuthenticated(state) }
                                                .onFailure { err -> errorMessage = err.message ?: "Invalid code" }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isLoading) "VERIFYING..." else "CONFIRM & CONTINUE",
                                    color = ChampagneOnGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.6.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Resend Code",
                                    color = ChampagneGold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            resendSignupOtpSupabase(pendingEmail)
                                            infoMessage = "Fresh 8-digit code sent to $pendingEmail"
                                        }
                                    }
                                )
                                Text(
                                    "Change Email",
                                    color = WarmSilver,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable {
                                        awaitingOtp = false
                                        otpCode = ""
                                        errorMessage = null
                                        infoMessage = null
                                    }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 26.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (isSignUp) "Email Address" else "Email or Username",
                                color = IvoryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF151823),
                                border = BorderStroke(1.dp, Color(0xFF2A2F3F)),
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (identifier.isEmpty()) {
                                            Text(
                                                if (isSignUp) "you@example.com" else "you@example.com or username",
                                                color = AshMuted,
                                                fontSize = 15.sp
                                            )
                                        }
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = identifier,
                                            onValueChange = {
                                                identifier = it
                                                errorMessage = null
                                            },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = IvoryText,
                                                fontSize = 15.sp
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Password",
                                color = IvoryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF151823),
                                border = BorderStroke(1.dp, Color(0xFF2A2F3F)),
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.VpnKey,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (password.isEmpty()) {
                                            Text("••••••••", color = AshMuted, fontSize = 15.sp)
                                        }
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = password,
                                            onValueChange = {
                                                password = it
                                                errorMessage = null
                                            },
                                            singleLine = true,
                                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = IvoryText,
                                                fontSize = 15.sp
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = WarmSilver,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    "Forgot Password?",
                                    color = WarmSilver,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable {
                                        errorMessage = "Enter your email above or reset via the Web application."
                                    }
                                )
                            }

                            if (!errorMessage.isNullOrBlank()) {
                                Text(
                                    text = errorMessage!!,
                                    color = CrimsonWaste,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFF3E2BA), Color(0xFFDFB870))
                                        )
                                    )
                                    .clickable(enabled = !isLoading) {
                                        if (identifier.isBlank() || password.isBlank()) {
                                            errorMessage = "Please enter your email/username and password."
                                            return@clickable
                                        }
                                        isLoading = true
                                        errorMessage = null
                                        scope.launch {
                                            val res = if (isSignUp) {
                                                signUpSupabase(identifier, password)
                                            } else {
                                                signInSupabase(identifier, password)
                                            }
                                            isLoading = false
                                            res.onSuccess { state ->
                                                onAuthenticated(state)
                                            }.onFailure { err ->
                                                if (err is OtpVerificationRequiredException) {
                                                    pendingEmail = err.email
                                                    awaitingOtp = true
                                                    otpCode = ""
                                                    infoMessage = "We sent an 8-digit verification code to ${err.email}"
                                                } else if ((err.message ?: "").contains("not confirmed", ignoreCase = true)) {
                                                    val em = identifier.trim().lowercase()
                                                    pendingEmail = em
                                                    awaitingOtp = true
                                                    otpCode = ""
                                                    resendSignupOtpSupabase(em)
                                                    infoMessage = "Enter the 8-digit verification code sent to $em"
                                                } else {
                                                    errorMessage = err.message ?: "Authentication failed"
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isLoading) "PLEASE WAIT..." else if (isSignUp) "CREATE ACCOUNT" else "SIGN IN",
                                    color = ChampagneOnGold,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }

                // Purple witch mascot perched atop the card
                if (mascotBitmap != null) {
                    Image(
                        bitmap = mascotBitmap,
                        contentDescription = "Anime Universe Mascot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(width = 210.dp, height = 200.dp)
                            .align(Alignment.TopCenter)
                    )
                } else {
                    runCatching { painterResource("drawable/img_login_mascot.png") }.getOrNull()?.let { painter ->
                        Image(
                            painter = painter,
                            contentDescription = "Anime Universe Mascot",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(width = 210.dp, height = 200.dp)
                                .align(Alignment.TopCenter)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isSignUp) "Already have an account?" else "Don't have an account?",
                    color = WarmSilver,
                    fontSize = 14.5.sp
                )
                Text(
                    if (isSignUp) "Sign In" else "Sign Up",
                    color = ChampagneGold,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        isSignUp = !isSignUp
                        awaitingOtp = false
                        errorMessage = null
                        infoMessage = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                "Continue without an account",
                color = WarmSilver.copy(alpha = 0.78f),
                fontSize = 13.5.sp,
                modifier = Modifier.clickable { onContinueGuest() }
            )
        }
    }
}

// ============================================================================
// SCREENSHOT 3: HOME SCREEN (Weekly Schedule)
// ============================================================================

@Composable
private fun DesktopHomeScreen(
    watchlist: List<DesktopAnimeItem>,
    avatarKey: String,
    homeAllWeek: Boolean,
    selectedDay: Int,
    onSelectAllWeek: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onOpenExplore: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDetail: (DesktopAnimeItem) -> Unit
) {
    val today = LocalDate.now()
    val todayDow = today.dayOfWeek.value
    val monday = today.minusDays((todayDow - 1).toLong())

    // Top Bar matching Screenshot 3
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                buildAnnotatedString {
                    append("ANIME ")
                    withStyle(SpanStyle(color = ChampagneGold, fontStyle = FontStyle.Italic)) {
                        append("UNIVERSE")
                    }
                },
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                color = IvoryText
            )
            Text(
                "WEEKLY SCHEDULE",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
                color = WarmSilver
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ObsidianPillBg,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenExplore)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = IvoryText, modifier = Modifier.size(20.dp))
                }
            }
            AvatarCircleButton(avatarKey = avatarKey, onClick = onOpenProfile)
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // "This Week" + "LIBRARY ›" dark pill button
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("This Week", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = IvoryText)
        Surface(
            shape = CircleShape,
            color = ObsidianPillBg,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenLibrary)
        ) {
            Text(
                "LIBRARY ›",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                color = ChampagneGold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // "All" pill + 7 Day Pills matching Screenshot 3
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (homeAllWeek) Color(0xFF2B271F) else ObsidianPillBg,
                border = BorderStroke(1.5.dp, if (homeAllWeek) ChampagneGold else ObsidianBorder),
                modifier = Modifier
                    .width(68.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onSelectAllWeek)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "All",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (homeAllWeek) ChampagneGoldSoft else IvoryText
                    )
                    if (homeAllWeek) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ChampagneGold)
                        )
                    }
                }
            }
        }

        items(DayOfWeek.entries) { dow ->
            val dayDate = monday.plusDays((dow.value - 1).toLong())
            val count = watchlist.count { it.airingDayOfWeek == dow.value }
            val isToday = dow.value == todayDow
            val isSelected = !homeAllWeek && selectedDay == dow.value

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isSelected) Color(0xFF2B271F) else ObsidianPillBg,
                border = BorderStroke(1.dp, if (isSelected) ChampagneGold else ObsidianBorder),
                modifier = Modifier
                    .width(68.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onSelectDay(dow.value) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        dow.name.take(3),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = WarmSilver
                    )
                    Text(
                        dayDate.dayOfMonth.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IvoryText
                    )
                    if (isToday) {
                        Text(
                            "TODAY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = EmeraldWorth
                        )
                    } else {
                        Text(
                            count.toString(),
                            fontSize = 10.5.sp,
                            color = WarmSilver
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    if (watchlist.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("No Shows Scheduled Yet", fontFamily = FontFamily.Serif, fontSize = 24.sp, color = IvoryText)
                Text("Add anime from Explore to automatically populate your weekly release calendar.", color = WarmSilver, fontSize = 13.5.sp)
                Button(
                    onClick = onOpenExplore,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                ) {
                    Text("Explore Catalog", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        val daysToRender = if (homeAllWeek) {
            DayOfWeek.entries.filter { d -> watchlist.any { it.airingDayOfWeek == d.value } }
        } else {
            listOf(DayOfWeek.of(selectedDay))
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 195.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            daysToRender.forEach { dow ->
                val dayShows = watchlist.filter { it.airingDayOfWeek == dow.value }
                val dayName = dow.name.lowercase().replaceFirstChar { it.uppercase() }
                item(span = { GridItemSpan(maxLineSpan) }, key = "header-${dow.value}") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dayName, fontFamily = FontFamily.Serif, fontSize = 25.sp, color = IvoryText)
                        Surface(
                            shape = CircleShape,
                            color = ObsidianPillBg,
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Text(
                                "${dayShows.size} ${if (dayShows.size == 1) "SHOW" else "SHOWS"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.4.sp,
                                color = ChampagneGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                items(dayShows, key = { "home-${dow.value}-${it.id}" }) { show ->
                    ScreenshotPosterCard(
                        item = show,
                        showSentimentBadge = true,
                        showFormatBadge = false,
                        onClick = { onOpenDetail(show) }
                    )
                }
            }
        }
    }
}

// ============================================================================
// SCREENSHOT 4: DISCOVER SCREEN (Global Catalog)
// ============================================================================

@Composable
private fun DesktopDiscoverScreen(
    discoverItems: List<DesktopAnimeItem>,
    watchlist: List<DesktopAnimeItem>,
    loading: Boolean,
    avatarKey: String,
    discoverMode: String,
    discoverGenre: String,
    searchQuery: String,
    safeMode: Boolean,
    onSearchChange: (String) -> Unit,
    onSelectMode: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onToggleSafe: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDetail: (DesktopAnimeItem) -> Unit
) {
    // Top Bar matching Screenshot 4
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Discover", fontFamily = FontFamily.Serif, fontSize = 34.sp, color = IvoryText)
            Text(
                "THE GLOBAL CATALOG",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
                color = WarmSilver
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircleIconActionButton(icon = Icons.AutoMirrored.Filled.ViewList, onClick = {})
            CircleIconActionButton(icon = Icons.Outlined.Tune, onClick = {})
            AvatarCircleButton(avatarKey = avatarKey, onClick = onOpenProfile)
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Search Bar matching Screenshot 4
    Surface(
        shape = CircleShape,
        color = ObsidianInputBg,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = WarmSilver, modifier = Modifier.size(20.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                    Text("Search 25,000+ anime & Donghua...", color = WarmSilver, fontSize = 15.sp)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = IvoryText, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Mode Row: TRENDING, POPULAR (solid gold when active), TOP RATED, SEASONAL, DONGHUA, AIRING, SAFE ON
    val modes = listOf(
        "trending" to "TRENDING",
        "popular" to "POPULAR",
        "top" to "TOP RATED",
        "seasonal" to "SEASONAL",
        "donghua" to "DONGHUA",
        "airing" to "AIRING"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(modes) { (mKey, mLabel) ->
            val active = discoverMode == mKey
            Surface(
                shape = CircleShape,
                color = if (active) ChampagneGold else ObsidianPillBg,
                border = if (active) null else BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.height(38.dp).clip(CircleShape).clickable { onSelectMode(mKey) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text(
                        mLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (active) ChampagneOnGold else WarmSilver
                    )
                }
            }
        }
        item {
            Surface(
                shape = CircleShape,
                color = if (safeMode) EmeraldWorthBg else ObsidianPillBg,
                border = BorderStroke(1.dp, if (safeMode) EmeraldWorth.copy(alpha = 0.5f) else ObsidianBorder),
                modifier = Modifier.height(38.dp).clip(CircleShape).clickable { onToggleSafe() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = if (safeMode) EmeraldWorth else WarmSilver,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        if (safeMode) "SAFE ON" else "SAFE OFF",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (safeMode) EmeraldWorth else WarmSilver
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Genre Row matching Screenshot 4
    val genres = listOf(
        "All Genres", "Action", "Adventure", "Comedy", "Drama",
        "Fantasy", "Romance", "Sci-Fi", "Slice of Life", "Supernatural", "Mystery", "Thriller"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(genres) { g ->
            val active = discoverGenre == g
            Surface(
                shape = CircleShape,
                color = if (active) Color(0xFF2B271F) else Color.Transparent,
                border = BorderStroke(1.dp, if (active) ChampagneGold.copy(alpha = 0.65f) else ObsidianBorder),
                modifier = Modifier.height(36.dp).clip(CircleShape).clickable { onSelectGenre(g) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        g,
                        fontSize = 13.sp,
                        color = if (active) ChampagneGoldSoft else WarmSilver
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Section Header: "Popular" on left, "24 TITLES" in gold on right
    val modeHeading = modes.firstOrNull { it.first == discoverMode }?.second
        ?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Popular"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modeHeading, fontFamily = FontFamily.Serif, fontSize = 26.sp, color = IvoryText)
        Text(
            "${discoverItems.size} TITLES",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = ChampagneGold
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 195.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        items(discoverItems, key = { it.id }) { item ->
            val tracked = watchlist.firstOrNull { it.id == item.id }
            val merged = tracked ?: item
            ScreenshotPosterCard(
                item = merged,
                showSentimentBadge = tracked != null,
                showFormatBadge = true,
                onClick = { onOpenDetail(merged) }
            )
        }
    }
}

// ============================================================================
// SCREENSHOT 2: LIBRARY SCREEN (1:1 Match)
// ============================================================================

@Composable
private fun DesktopLibraryScreen(
    watchlist: List<DesktopAnimeItem>,
    collections: List<DesktopCollectionItem>,
    avatarKey: String,
    statusFilter: String,
    ratingFilter: String,
    searchQuery: String,
    sortMode: String,
    onStatusChange: (String) -> Unit,
    onRatingChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onCreateCollectionClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDetail: (DesktopAnimeItem) -> Unit
) {
    // Top Bar matching Screenshot 2
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Library", fontFamily = FontFamily.Serif, fontSize = 34.sp, color = IvoryText)
            Text(
                "${watchlist.size} TRACKED • ${collections.size} COLLECTIONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
                color = WarmSilver
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircleIconActionButton(icon = Icons.Outlined.CollectionsBookmark, onClick = onCreateCollectionClick)
            CircleIconActionButton(icon = Icons.AutoMirrored.Filled.ViewList, onClick = {})
            AvatarCircleButton(avatarKey = avatarKey, onClick = onOpenProfile)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Status Filter Row with circular badge counts matching Screenshot 2
    val statusTabs = listOf(
        Triple("all", "ALL", watchlist.size),
        Triple("watching", "WATCHING", watchlist.count { it.watchStatus == "watching" }),
        Triple("completed", "COMPLETED", watchlist.count { it.watchStatus == "completed" }),
        Triple("plan_to_watch", "PLAN TO WATCH", watchlist.count { it.watchStatus == "plan_to_watch" }),
        Triple("on_hold", "ON HOLD", watchlist.count { it.watchStatus == "on_hold" }),
        Triple("dropped", "DROPPED", watchlist.count { it.watchStatus == "dropped" })
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(statusTabs) { (key, label, count) ->
            val active = statusFilter == key
            Surface(
                shape = CircleShape,
                color = if (active) Color(0xFF2B271F) else ObsidianPillBg,
                border = BorderStroke(1.dp, if (active) ChampagneGold.copy(alpha = 0.6f) else ObsidianBorder),
                modifier = Modifier.height(42.dp).clip(CircleShape).clickable { onStatusChange(key) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = if (active) ChampagneGoldSoft else IvoryText
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            count.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) ChampagneGold else WarmSilver,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Sentiment Filter Row matching Screenshot 2 (ALL RATINGS, CINEMA 1, WORTH 3, AVG 0, BORING 0, WASTE 0)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            val active = ratingFilter == "all"
            Surface(
                shape = CircleShape,
                color = if (active) Color(0xFF2B271F) else ObsidianPillBg,
                border = BorderStroke(1.dp, if (active) ChampagneGold.copy(alpha = 0.6f) else ObsidianBorder),
                modifier = Modifier.height(38.dp).clip(CircleShape).clickable { onRatingChange("all") }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "ALL RATINGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = if (active) ChampagneGoldSoft else WarmSilver
                    )
                }
            }
        }
        items(SentimentTier.entries) { tier ->
            val count = watchlist.count { SentimentTier.fromStars(it.userStars) == tier }
            val active = ratingFilter == tier.key
            Surface(
                shape = CircleShape,
                color = if (active) tier.pillBg else ObsidianPillBg,
                border = BorderStroke(1.dp, if (active) tier.textColor else ObsidianBorder),
                modifier = Modifier.height(38.dp).clip(CircleShape).clickable { onRatingChange(tier.key) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        tier.shortLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = tier.textColor
                    )
                    Text(
                        count.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmSilver
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Search Input: "Filter your collection..."
    Surface(
        shape = CircleShape,
        color = ObsidianInputBg,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = WarmSilver, modifier = Modifier.size(20.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                    Text("Filter your collection...", color = WarmSilver, fontSize = 15.sp)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = IvoryText, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Sort Chips: Sort: Recent, Sort: Title, Sort: Rating, Sort: Progress
    val sorts = listOf(
        "recent" to "Sort: Recent",
        "title" to "Sort: Title",
        "rating" to "Sort: Rating",
        "progress" to "Sort: Progress"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sorts) { (sKey, sLabel) ->
            val active = sortMode == sKey
            Surface(
                shape = CircleShape,
                color = if (active) Color(0xFF2B271F) else Color.Transparent,
                border = BorderStroke(1.dp, if (active) ChampagneGold.copy(alpha = 0.6f) else ObsidianBorder),
                modifier = Modifier.height(38.dp).clip(CircleShape).clickable { onSortChange(sKey) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        sLabel,
                        fontSize = 13.sp,
                        color = if (active) ChampagneGoldSoft else IvoryText
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val filteredList = remember(watchlist, statusFilter, ratingFilter, searchQuery, sortMode) {
        watchlist
            .filter { item ->
                (statusFilter == "all" || item.watchStatus == statusFilter) &&
                    (ratingFilter == "all" || SentimentTier.fromStars(item.userStars)?.key == ratingFilter) &&
                    (searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true))
            }
            .let { list ->
                when (sortMode) {
                    "title" -> list.sortedBy { it.title.lowercase() }
                    "rating" -> list.sortedByDescending { it.userStars }
                    "progress" -> list.sortedByDescending { it.watchedEpisodes }
                    else -> list
                }
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 195.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Collections Header + Empty/List Card matching Screenshot 2
        item(span = { GridItemSpan(maxLineSpan) }, key = "collections-section") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Collections", fontFamily = FontFamily.Serif, fontSize = 24.sp, color = IvoryText)
                    Text(
                        "+  NEW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = ChampagneGold,
                        modifier = Modifier.clickable(onClick = onCreateCollectionClick)
                    )
                }

                if (collections.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianInputBg,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Create custom collections such as \"Absolute Cinema\" or \"Donghua Favorites\".",
                            color = WarmSilver,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                        )
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(collections, key = { it.id }) { col ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = ObsidianInputBg,
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.width(240.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(col.title, fontFamily = FontFamily.Serif, fontSize = 18.sp, color = IvoryText)
                                    Text(
                                        col.description.ifBlank { "${col.count} anime" },
                                        fontSize = 12.sp,
                                        color = WarmSilver,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        items(filteredList, key = { it.id }) { item ->
            ScreenshotPosterCard(
                item = item,
                showSentimentBadge = true,
                showFormatBadge = false,
                onClick = { onOpenDetail(item) }
            )
        }
    }
}

// ============================================================================
// SCREENSHOT 5: COMMUNITY SCREEN (1:1 Match)
// ============================================================================

@Composable
private fun DesktopCommunityScreen(
    reviews: List<DesktopCommunityReview>,
    avatarKey: String,
    ratingFilter: String,
    sortMode: String,
    onRatingChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAnime: (Int) -> Unit
) {
    // Top Bar matching Screenshot 5
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Community", fontFamily = FontFamily.Serif, fontSize = 34.sp, color = IvoryText)
            Text(
                "RATED & NOTED • ${reviews.size} REVIEWS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
                color = WarmSilver
            )
        }
        AvatarCircleButton(avatarKey = avatarKey, onClick = onOpenProfile)
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Sentiment Filter Row: ALL (4), CINEMA, WORTH, AVG, BORING, WASTE
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            val active = ratingFilter == "all"
            Surface(
                shape = CircleShape,
                color = if (active) Color(0xFF2B271F) else ObsidianPillBg,
                border = BorderStroke(1.dp, if (active) ChampagneGold.copy(alpha = 0.65f) else ObsidianBorder),
                modifier = Modifier.height(42.dp).clip(CircleShape).clickable { onRatingChange("all") }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text(
                        "ALL (${reviews.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = if (active) ChampagneGoldSoft else WarmSilver
                    )
                }
            }
        }
        items(SentimentTier.entries) { tier ->
            val active = ratingFilter == tier.key
            Surface(
                shape = CircleShape,
                color = if (active) Color(0xFF2B271F) else ObsidianPillBg,
                border = BorderStroke(1.dp, if (active) ChampagneGold.copy(alpha = 0.65f) else ObsidianBorder),
                modifier = Modifier.height(42.dp).clip(CircleShape).clickable { onRatingChange(tier.key) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text(
                        tier.shortLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = if (active) ChampagneGoldSoft else WarmSilver
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Sort Row: Newest, Highest Rated
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("newest" to "Newest", "highest" to "Highest Rated").forEach { (sKey, sLabel) ->
            val active = sortMode == sKey
            Surface(
                shape = CircleShape,
                color = if (active) Color(0xFF2B271F) else ObsidianPillBg,
                border = BorderStroke(1.dp, if (active) ChampagneGold.copy(alpha = 0.65f) else ObsidianBorder),
                modifier = Modifier.height(38.dp).clip(CircleShape).clickable { onSortChange(sKey) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text(
                        sLabel,
                        fontSize = 13.sp,
                        color = if (active) ChampagneGoldSoft else WarmSilver
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val filteredReviews = remember(reviews, ratingFilter, sortMode) {
        reviews
            .filter { r -> ratingFilter == "all" || SentimentTier.fromStars(r.stars)?.key == ratingFilter }
            .let { list ->
                if (sortMode == "highest") list.sortedByDescending { it.stars }
                else list
            }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        items(filteredReviews, key = { it.id }) { rev ->
            val tier = SentimentTier.fromStars(rev.stars) ?: SentimentTier.WORTH
            val filledStars = rev.stars.toInt().coerceIn(0, 5)
            val avatarPreset = DesktopAvatar.fromKey(rev.avatarKey)

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenAnime(rev.animeId) }
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row: 68x96 Poster + Serif Title + 5-Star Row + Sentiment Pill
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        NetworkPosterImage(
                            url = rev.animeCover,
                            contentDescription = rev.animeTitle,
                            modifier = Modifier
                                .size(width = 74.dp, height = 104.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                rev.animeTitle,
                                fontFamily = FontFamily.Serif,
                                fontSize = 23.sp,
                                color = IvoryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                (1..5).forEach { i ->
                                    Text(
                                        "★",
                                        fontSize = 18.sp,
                                        color = if (i <= filledStars) ChampagneGold else Color(0xFF393E4F)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    String.format(Locale.US, "%.1f", if (rev.stars > 0f) rev.stars else 4.0f),
                                    fontSize = 16.sp,
                                    color = IvoryText
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = tier.pillBg
                            ) {
                                Text(
                                    tier.fullLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = tier.textColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Quote Box with vertical gold bar matching Screenshot 5
                    if (rev.notes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianQuoteBox,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(36.dp)
                                        .clip(CircleShape)
                                        .background(ChampagneGold)
                                )
                                Text(
                                    "“${rev.notes}”",
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 15.5.sp,
                                    color = IvoryText
                                )
                            }
                        }
                    }

                    // Bottom Footer: Reviewer Avatar + Name on left, Date on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DesktopAvatarImage(
                                avatarKey = avatarPreset.key,
                                username = rev.username,
                                modifier = Modifier.size(30.dp).clip(CircleShape)
                            )
                            Text(rev.username, fontSize = 14.sp, color = WarmSilver)
                        }
                        Text(rev.formattedDate, fontSize = 13.sp, color = WarmSilver)
                    }
                }
            }
        }
    }
}

// ============================================================================
// SHARED POSTER CARD & TOP BAR ACTIONS (Matches Screenshots 2, 3, 4)
// ============================================================================

@Composable
private fun ScreenshotPosterCard(
    item: DesktopAnimeItem,
    showSentimentBadge: Boolean,
    showFormatBadge: Boolean,
    onClick: () -> Unit
) {
    val tier = if (showSentimentBadge) SentimentTier.fromStars(item.userStars) else null
    val displayStars = if (item.userStars > 0f) item.userStars else item.catalogStars

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(22.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NetworkPosterImage(
                    url = item.coverImage,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-left Sentiment Pill (e.g., WORTH in emerald, CINEMA in gold)
                if (tier != null) {
                    Surface(
                        shape = CircleShape,
                        color = tier.textColor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            tier.shortLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = ChampagneOnGold,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                        )
                    }
                }

                // Top-right Star Rating Pill (★ 4.0)
                if (displayStars > 0f) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xD911141E),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            "★ ${String.format(Locale.US, "%.1f", displayStars)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = IvoryText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Bottom-left Format Pill on Discover (e.g. TV)
                if (showFormatBadge) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC11141E),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            item.format,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = IvoryText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = IvoryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun CircleIconActionButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = ObsidianPillBg,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = IvoryText, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AvatarCircleButton(
    avatarKey: String,
    onClick: () -> Unit
) {
    DesktopAvatarImage(
        avatarKey = avatarKey,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.5.dp, ChampagneGold.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick)
    )
}

// ============================================================================
// PROFILE & DETAIL SCREENS
// ============================================================================

@Composable
private fun DesktopProfileScreen(
    session: DesktopPersistedState,
    watchlist: List<DesktopAnimeItem>,
    collectionsCount: Int,
    onUpdateProfile: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var nameInput by remember(session.username) { mutableStateOf(session.username) }
    var selectedAvatar by remember(session.avatarKey) { mutableStateOf(session.avatarKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Profile", fontFamily = FontFamily.Serif, fontSize = 34.sp, color = IvoryText)

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    val preset = DesktopAvatar.fromKey(selectedAvatar)
                    DesktopAvatarImage(
                        avatarKey = preset.key,
                        username = session.username,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .border(2.dp, ChampagneGold, CircleShape)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(session.username, fontFamily = FontFamily.Serif, fontSize = 26.sp, color = IvoryText)
                        Text(
                            session.email.ifBlank {
                                if (session.isGuest) "Isolated Guest Vault (${session.guestId.take(14)})"
                                else "Cloud Synced Account"
                            },
                            fontSize = 13.5.sp,
                            color = WarmSilver
                        )
                        Text(
                            "${watchlist.size} Tracked • ${watchlist.count { it.watchStatus == "completed" }} Completed • $collectionsCount Collections",
                            fontSize = 12.5.sp,
                            color = ChampagneGold
                        )
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "CHOOSE AVATAR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    color = ChampagneGold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(DesktopAvatar.entries) { av ->
                        val isSel = av.key == selectedAvatar
                        DesktopAvatarImage(
                            avatarKey = av.key,
                            username = av.displayName,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .border(
                                    if (isSel) 2.5.dp else 1.dp,
                                    if (isSel) ChampagneGold else ObsidianBorder,
                                    CircleShape
                                )
                                .clickable { selectedAvatar = av.key }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onUpdateProfile(nameInput.ifBlank { session.username }, selectedAvatar) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, CrimsonWaste.copy(alpha = 0.6f))
                    ) {
                        Text(if (session.isGuest) "Sign In / Switch" else "Sign Out", color = CrimsonWaste)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopDetailScreen(
    item: DesktopAnimeItem,
    onBack: () -> Unit,
    onSave: (DesktopAnimeItem) -> Unit,
    onDelete: (DesktopAnimeItem) -> Unit
) {
    var watchStatus by remember(item.id) { mutableStateOf(item.watchStatus) }
    var watchedEps by remember(item.id) { mutableStateOf(item.watchedEpisodes) }
    var stars by remember(item.id) { mutableStateOf(item.userStars) }
    var notes by remember(item.id) { mutableStateOf(item.notes) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = IvoryText)
            }
            Text(
                item.title,
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                color = IvoryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                NetworkPosterImage(
                    url = item.coverImage,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(width = 180.dp, height = 260.dp)
                        .clip(RoundedCornerShape(18.dp))
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "WATCH STATUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        color = ChampagneGold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val statuses = listOf(
                            "watching" to "Watching",
                            "completed" to "Completed",
                            "plan_to_watch" to "Plan to Watch",
                            "on_hold" to "On Hold",
                            "dropped" to "Dropped"
                        )
                        items(statuses) { (k, label) ->
                            val active = watchStatus == k
                            Surface(
                                shape = CircleShape,
                                color = if (active) ChampagneGold else ObsidianPillBg,
                                border = BorderStroke(1.dp, if (active) ChampagneGold else ObsidianBorder),
                                modifier = Modifier.clip(CircleShape).clickable { watchStatus = k }
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) ChampagneOnGold else IvoryText,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Episodes Watched:", color = WarmSilver, fontSize = 14.sp)
                        Surface(
                            shape = CircleShape,
                            color = ObsidianPillBg,
                            modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                                watchedEps = (watchedEps - 1).coerceAtLeast(0)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text("−", color = IvoryText) }
                        }
                        Text(
                            "$watchedEps / ${item.totalEpisodes ?: "∞"}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = IvoryText
                        )
                        Surface(
                            shape = CircleShape,
                            color = ChampagneGold,
                            modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                                watchedEps += 1
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text("+", color = ChampagneOnGold) }
                        }
                    }

                    Text(
                        "YOUR RATING & SENTIMENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        color = ChampagneGold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..5).forEach { i ->
                            Text(
                                "★",
                                fontSize = 28.sp,
                                color = if (stars >= i) ChampagneGold else AshMuted,
                                modifier = Modifier.clickable { stars = i.toFloat() }
                            )
                        }
                        SentimentTier.fromStars(stars)?.let { tier ->
                            Surface(shape = CircleShape, color = tier.pillBg) {
                                Text(
                                    tier.fullLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tier.textColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Write a review note for Community...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                onSave(
                                    item.copy(
                                        inWatchlist = true,
                                        watchStatus = watchStatus,
                                        watchedEpisodes = watchedEps,
                                        userStars = stars,
                                        notes = notes,
                                        updatedAt = Instant.now().toString()
                                    )
                                )
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                        ) {
                            Text("Save & Sync to Cloud", fontWeight = FontWeight.Bold)
                        }
                        if (item.inWatchlist) {
                            OutlinedButton(
                                onClick = { onDelete(item) },
                                shape = CircleShape,
                                border = BorderStroke(1.dp, CrimsonWaste.copy(alpha = 0.6f))
                            ) {
                                Text("Remove", color = CrimsonWaste)
                            }
                        }
                    }

                    if (item.synopsis.isNotBlank()) {
                        Text(item.synopsis, color = WarmSilver, fontSize = 13.5.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text("New Collection", fontFamily = FontFamily.Serif, color = IvoryText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g., Absolute Cinema") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Optional description...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onCreate(title, description) },
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WarmSilver)
            }
        }
    )
}
