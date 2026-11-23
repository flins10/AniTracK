package com.example.desktop

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javazoom.jl.player.Player
import org.jetbrains.skia.Image as SkiaImage
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.random.Random
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

internal val PureObsidian = Color(0xFF0A0C12)
internal val ObsidianSidebar = Color(0xFF0E1017)
internal val ObsidianCard = Color(0xFF12151E)
internal val ObsidianQuoteBox = Color(0xFF171B26)
internal val ObsidianPillBg = Color(0xFF171A24)
internal val ObsidianInputBg = Color(0xFF141822)
internal val ObsidianBorder = Color(0xFF262B3A)
internal val ChampagneGold = Color(0xFFE5C17B)
internal val ChampagneGoldSoft = Color(0xFFF3DFA8)
internal val ChampagneGoldBronze = Color(0xFFC29B53)
internal val ChampagneOnGold = Color(0xFF110E08)
internal val IvoryText = Color(0xFFF4EFE6)
internal val WarmSilver = Color(0xFF9499A6)
internal val AshMuted = Color(0xFF636875)
internal val EmeraldWorth = Color(0xFF5CE092)
internal val EmeraldWorthBg = Color(0xFF153328)
internal val AmberBoring = Color(0xFFF0B45B)
internal val CrimsonWaste = Color(0xFFF26D7D)
internal val SilverAvg = Color(0xFFB5BAC7)

internal val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()
internal val gson = Gson()
private val imageCache = ConcurrentHashMap<String, ImageBitmap>()

// Hardened 4-shard SHA-256 bit-rotated Vault (Matches Android VaultMaterial & Web app.js)
private val V_NONCE = byteArrayOf(
    0x4b, 0x91.toByte(), 0x2e, 0xc7.toByte(), 0x83.toByte(), 0x15, 0xf4.toByte(), 0x6a,
    0x39, 0xd2.toByte(), 0x58, 0x0c, 0xe6.toByte(), 0x71, 0xa4.toByte(), 0xbd.toByte()
)
private val U_S0 = intArrayOf(122, 155, 146, 165, 244, 26, 51, 60, 154, 168)
private val U_S1 = intArrayOf(240, 60, 74, 204, 143, 231, 223, 175, 178, 32)
private val U_S2 = intArrayOf(85, 29, 14, 44, 1, 104, 158, 164, 175, 109)
private val U_S3 = intArrayOf(134, 93, 99, 51, 141, 35, 252, 172, 23, 201)

private val K_S0 = intArrayOf(34, 4, 67, 148, 18, 93, 205, 57, 54, 74, 16, 215)
private val K_S1 = intArrayOf(186, 113, 144, 138, 96, 215, 143, 229, 30, 194, 74, 121)
private val K_S2 = intArrayOf(53, 207, 73, 120, 33, 171, 232, 142, 185, 22, 216)
private val K_S3 = intArrayOf(207, 192, 33, 75, 28, 172, 50, 117, 66, 179, 98)

private fun unsealVaultV2(
    s0: IntArray,
    s1: IntArray,
    s2: IntArray,
    s3: IntArray,
    len: Int,
    label: String,
    salt: Int
): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(V_NONCE)
    digest.update(label.toByteArray(Charsets.UTF_8))
    val key = digest.digest()
    val shards = arrayOf(s0, s1, s2, s3)
    val idx = IntArray(4)
    val out = ByteArray(len)
    for (i in 0 until len) {
        val lane = i and 3
        val obf = shards[lane][idx[lane]++] and 0xFF
        val rot = (i % 7) + 1
        val unrot = ((obf ushr rot) or ((obf shl (8 - rot)) and 0xFF)) and 0xFF
        val k1 = key[i % key.size].toInt() and 0xFF
        val k2 = (i * 131 + salt * 37 + 0x5d) and 0xFF
        out[i] = (unrot xor k1 xor k2).toByte()
    }
    return String(out, Charsets.UTF_8)
}

internal val SUPABASE_URL: String by lazy {
    System.getenv("SUPABASE_URL")?.takeIf { it.isNotBlank() }
        ?: unsealVaultV2(U_S0, U_S1, U_S2, U_S3, 40, "au_vault_u_v2", 0x37).trimEnd('/')
}
internal val SUPABASE_ANON_KEY: String by lazy {
    System.getenv("SUPABASE_ANON_KEY")?.takeIf { it.isNotBlank() }
        ?: unsealVaultV2(K_S0, K_S1, K_S2, K_S3, 46, "au_vault_k_v2", 0x6B).trim()
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
internal fun DesktopAvatarImage(
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
    val inWatchlist: Boolean = false,
    val inHome: Boolean = false
)

data class DesktopCollectionItem(
    val id: String,
    val title: String,
    val description: String = "",
    val count: Int = 0
)

private fun isDesktopHentaiOrAdult(genres: Collection<String>, isAdultFlag: Boolean = false): Boolean {
    if (isAdultFlag) return true
    return genres.any { raw ->
        val g = raw.trim().lowercase()
        g == "hentai" || g == "adult" || g.contains("18+")
    }
}

data class DesktopCommunityReview(
    val id: String,
    val userId: String = "",
    val animeId: Int,
    val animeTitle: String,
    val animeCover: String,
    val stars: Float,
    val notes: String,
    val username: String,
    val avatarKey: String,
    val updatedAt: String,
    val formattedDate: String,
    val genres: List<String> = emptyList(),
    val isAdult: Boolean = isDesktopHentaiOrAdult(genres)
)

data class DesktopCommunityAnimeGroup(
    val animeId: Int,
    val animeTitle: String,
    val animeCover: String,
    val averageStars: Float,
    val ratedCount: Int,
    val notedCount: Int,
    val latestUpdatedAt: String,
    val formattedLatestDate: String,
    val leadReview: DesktopCommunityReview,
    val reviews: List<DesktopCommunityReview>,
    val genres: List<String> = emptyList(),
    val isAdult: Boolean = isDesktopHentaiOrAdult(genres)
) {
    val notesAndRatingsCount: Int get() = reviews.size
    val distinctContributors: List<DesktopCommunityReview>
        get() = reviews.distinctBy { it.userId.ifBlank { it.username } }
}

private fun groupDesktopCommunityReviews(
    reviews: List<DesktopCommunityReview>
): List<DesktopCommunityAnimeGroup> {
    return reviews
        .filter { it.notes.isNotBlank() || it.stars > 0f }
        .groupBy { it.animeId }
        .values
        .mapNotNull { items ->
            val sorted = items.sortedByDescending { it.updatedAt }
            if (sorted.isEmpty()) return@mapNotNull null
            val lead = sorted.firstOrNull { it.notes.isNotBlank() } ?: sorted.first()
            val rated = sorted.mapNotNull { it.stars.takeIf { s -> s > 0f } }
            val avgStars = if (rated.isNotEmpty()) rated.average().toFloat() else 0f
            val notedCount = sorted.count { it.notes.isNotBlank() }
            val bestTitle = sorted.firstOrNull { it.animeTitle.isNotBlank() }?.animeTitle ?: "Anime #${lead.animeId}"
            val bestCover = sorted.firstOrNull { it.animeCover.isNotBlank() }?.animeCover.orEmpty()
            val latestIso = sorted.maxOfOrNull { it.updatedAt }.orEmpty()
            val mergedGenres = sorted.flatMap { it.genres }.distinct()
            val isAdult = sorted.any { it.isAdult } || isDesktopHentaiOrAdult(mergedGenres)
            DesktopCommunityAnimeGroup(
                animeId = lead.animeId,
                animeTitle = bestTitle,
                animeCover = bestCover,
                averageStars = avgStars,
                ratedCount = rated.size,
                notedCount = notedCount,
                latestUpdatedAt = latestIso,
                formattedLatestDate = lead.formattedDate,
                leadReview = lead,
                reviews = sorted,
                genres = mergedGenres,
                isAdult = isAdult
            )
        }
}

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
    val onboardingCompleted: Boolean = true,
    val watchlist: List<DesktopAnimeItem> = emptyList(),
    val collections: List<DesktopCollectionItem> = emptyList()
)

private fun isDesktopPlaceholderOrEmailName(name: String?): Boolean {
    val clean = name?.trim().orEmpty()
    return clean.isBlank() ||
        clean.contains("@") ||
        clean.startsWith("traveler_", ignoreCase = true) ||
        clean.equals("Wanderer", ignoreCase = true)
}

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
            val metaObj = userObj?.getAsJsonObject("user_metadata")
            val metaUsername = metaObj?.get("username").asStringOrNull()?.trim().orEmpty()
            val metaAvatar = metaObj?.get("avatar_key").asStringOrNull()?.trim().orEmpty()
            val metaOnboarded = metaObj?.get("onboarding_completed")?.takeIf { !it.isJsonNull }?.asBoolean ?: true
            val safeName = when {
                !isDesktopPlaceholderOrEmailName(metaUsername) -> metaUsername
                !identifier.contains("@") && identifier.isNotBlank() -> identifier.trim()
                else -> ""
            }
            DesktopPersistedState(
                accessToken = token,
                refreshToken = refresh,
                expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn,
                userId = userId,
                email = userEmail,
                username = safeName,
                avatarKey = DesktopAvatar.fromKey(metaAvatar).key,
                isGuest = false,
                onboardingCompleted = metaOnboarded && !isDesktopPlaceholderOrEmailName(safeName)
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
        val userMeta = JsonObject().apply {
            addProperty("onboarding_completed", false)
        }
        val payload = JsonObject().apply {
            addProperty("email", email)
            addProperty("password", password)
            add("data", userMeta)
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
                username = "",
                avatarKey = DesktopAvatar.AVATAR_1.key,
                isGuest = false,
                onboardingCompleted = false
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
                username = "",
                avatarKey = DesktopAvatar.AVATAR_1.key,
                isGuest = false,
                onboardingCompleted = false
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

internal suspend fun refreshTokenIfNeeded(state: DesktopPersistedState): DesktopPersistedState = withContext(Dispatchers.IO) {
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
    val refreshedSession: DesktopPersistedState,
    val username: String?,
    val avatarKey: String?,
    val watchlist: List<DesktopAnimeItem>?,
    val homeIds: Set<Int>?,
    val collections: List<DesktopCollectionItem>?,
    val communityReviews: List<DesktopCommunityReview>
)

internal fun parsePostgrestError(body: String, fallback: String): String {
    return runCatching {
        val obj = gson.fromJson(body, JsonObject::class.java)
        obj?.get("message").asStringOrNull()
            ?: obj?.get("error_description").asStringOrNull()
            ?: obj?.get("details").asStringOrNull()
            ?: obj?.get("hint").asStringOrNull()
            ?: obj?.get("msg").asStringOrNull()
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
}

private suspend fun fetchFullCloudSnapshot(session: DesktopPersistedState): CloudSyncSnapshot = withContext(Dispatchers.IO) {
    val active = refreshTokenIfNeeded(session)
    val bearer = active.accessToken.ifBlank { SUPABASE_ANON_KEY }
    val uid = active.userId

    var resolvedUsername: String? = null
    var resolvedAvatar: String? = null
    var watchlistResult: List<DesktopAnimeItem>? = null
    var homeIdsResult: Set<Int>? = null
    var homeDayOverrides = mutableMapOf<Int, Int>()
    var collectionsResult: List<DesktopCollectionItem>? = null

    if (uid.isNotBlank() && active.accessToken.isNotBlank()) {
        // 0. Independent Home Tracking membership
        runCatching {
            val htReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/home_tracking?user_id=eq.$uid&select=anime_id,airing_day_of_week")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $bearer")
                .build()
            httpClient.newCall(htReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java) ?: JsonArray()
                    val ids = mutableSetOf<Int>()
                    arr.forEach { el ->
                        val o = el.asJsonObject
                        val aid = o.get("anime_id").asIntOrNull() ?: return@forEach
                        ids.add(aid)
                        o.get("airing_day_of_week").asIntOrNull()?.takeIf { it in 1..7 }?.let { dow ->
                            homeDayOverrides[aid] = dow
                        }
                    }
                    homeIdsResult = ids
                }
            }
        }

        // 1. User Library (public.user_anime)
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
                        val rawEng = animeObj?.get("title_english").asStringOrNull()
                        val romaji = animeObj?.get("title_romaji").asStringOrNull().orEmpty()
                        val title = DesktopEnglishTitleMapper.resolve(
                            titleEnglish = rawEng,
                            titleRomaji = romaji,
                            synonyms = null,
                            isDonghua = false
                        ) ?: rawEng?.takeIf { it.isNotBlank() }
                            ?: romaji.takeIf { it.isNotBlank() }
                            ?: "Anime #$id"
                        val rawRating = row.get("personal_rating").asFloatOrNull() ?: 0f
                        val userStars = if (rawRating > 5f) rawRating / 2f else rawRating
                        val avg100 = animeObj?.get("average_score").asFloatOrNull() ?: 0f
                        val catalogStars = if (avg100 > 10f) avg100 / 20f else if (avg100 > 5f) avg100 / 2f else avg100
                        val nextAiringAt = animeObj?.get("next_airing_at").asLongOrNull()
                        val officialStatus = animeObj?.get("status").asStringOrNull() ?: "RELEASING"
                        val totalEps = row.get("total_episodes").asIntOrNull()?.takeIf { it > 0 }
                            ?: animeObj?.get("episodes").asIntOrNull()?.takeIf { it > 0 }
                        val watchedEps = row.get("episode_progress").asIntOrNull() ?: 0
                        val rawWatchStatus = row.get("status").asStringOrNull() ?: "watching"
                        val autoDone = DesktopTrackingRules.shouldAutoComplete(officialStatus, totalEps, watchedEps)
                        val resolvedWatchStatus = if (autoDone && rawWatchStatus != "dropped") "completed" else rawWatchStatus
                        val explicitHome = homeIdsResult
                        val resolvedInHome = if (explicitHome != null) {
                            explicitHome.contains(id) &&
                                resolvedWatchStatus != "dropped" &&
                                resolvedWatchStatus != "completed" &&
                                !autoDone
                        } else {
                            resolvedWatchStatus == "watching" && !autoDone
                        }
                        DesktopAnimeItem(
                            id = id,
                            title = title,
                            titleRomaji = romaji,
                            coverImage = animeObj?.get("cover_image").asStringOrNull().orEmpty(),
                            bannerImage = animeObj?.get("banner_image").asStringOrNull().orEmpty(),
                            format = animeObj?.get("format").asStringOrNull() ?: "TV",
                            status = officialStatus,
                            totalEpisodes = totalEps,
                            watchedEpisodes = watchedEps,
                            watchStatus = resolvedWatchStatus,
                            userStars = userStars,
                            catalogStars = catalogStars,
                            airingDayOfWeek = homeDayOverrides[id] ?: computeDayOfWeek(id, nextAiringAt),
                            seasonYear = animeObj?.get("release_year").asIntOrNull(),
                            notes = row.get("review_notes").asStringOrNull().orEmpty(),
                            updatedAt = row.get("updated_at").asStringOrNull().orEmpty(),
                            inWatchlist = true,
                            inHome = resolvedInHome
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

    // 4. Community Reviews (Prefer independent community_notes table, seamlessly fall back/merge with user_anime)
    val communityList = mutableListOf<DesktopCommunityReview>()
    runCatching {
        val leftOrRemovedUsers = mutableSetOf<String>()
        val cmReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/community_members?select=user_id,status&limit=500")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $bearer")
            .build()
        httpClient.newCall(cmReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java) ?: JsonArray()
                arr.forEach { el ->
                    val o = el.asJsonObject
                    val mUid = o.get("user_id").asStringOrNull() ?: return@forEach
                    val st = o.get("status").asStringOrNull()?.lowercase().orEmpty()
                    if (st == "left" || st == "removed") {
                        leftOrRemovedUsers.add(mUid)
                    }
                }
            }
        }

        val profMap = mutableMapOf<String, Pair<String, String>>()
        val profReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/profiles?select=id,username,avatar_key,avatar_url&limit=300")
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

        val seenKeys = mutableSetOf<String>()
        // 4a. Independent community_notes table
        val cnSelect = "id,user_id,anime_id,rating,note_text,updated_at,anime(anilist_id,title_english,title_romaji,cover_image,episodes,genres)"
        val cnReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/community_notes?select=$cnSelect&order=updated_at.desc&limit=200")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $bearer")
            .build()
        runCatching {
            httpClient.newCall(cnReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java) ?: JsonArray()
                    arr.forEach { el ->
                        val row = el.asJsonObject
                        val rowUserId = row.get("user_id").asStringOrNull().orEmpty()
                        if (rowUserId.isNotBlank() && leftOrRemovedUsers.contains(rowUserId)) return@forEach
                        val rawRating = row.get("rating").asFloatOrNull() ?: 0f
                        val stars = if (rawRating > 5f) rawRating / 2f else rawRating
                        val notes = row.get("note_text").asStringOrNull()?.trim().orEmpty()
                        if (stars <= 0f && notes.isBlank()) return@forEach

                        val animeObj = row.get("anime")?.takeIf { it.isJsonObject }?.asJsonObject
                        val animeId = animeObj?.get("anilist_id").asIntOrNull() ?: row.get("anime_id").asIntOrNull() ?: 0
                        val key = "$rowUserId:$animeId"
                        if (!seenKeys.add(key)) return@forEach
                        val rawEng = animeObj?.get("title_english").asStringOrNull()
                        val rawRom = animeObj?.get("title_romaji").asStringOrNull()
                        val title = DesktopEnglishTitleMapper.resolve(rawEng, rawRom, null, false)
                            ?: rawEng?.takeIf { it.isNotBlank() }
                            ?: rawRom?.takeIf { it.isNotBlank() }
                            ?: "Anime #$animeId"
                        val cover = animeObj?.get("cover_image").asStringOrNull().orEmpty()
                        val genres = animeObj?.get("genres")?.takeIf { it.isJsonArray }?.asJsonArray
                            ?.mapNotNull { it.asStringOrNull()?.trim()?.takeIf { g -> g.isNotBlank() } }
                            .orEmpty()
                        val (uname, avKey) = profMap[rowUserId] ?: (
                            if (rowUserId == uid) (resolvedUsername ?: active.username) to (resolvedAvatar ?: active.avatarKey)
                            else "Anime Lover" to DesktopAvatar.AVATAR_3.key
                        )
                        val updatedIso = row.get("updated_at").asStringOrNull().orEmpty()
                        communityList.add(
                            DesktopCommunityReview(
                                id = row.get("id").asStringOrNull() ?: "$rowUserId-$animeId",
                                userId = rowUserId,
                                animeId = animeId,
                                animeTitle = title,
                                animeCover = cover,
                                stars = stars,
                                notes = notes,
                                username = uname,
                                avatarKey = avKey,
                                updatedAt = updatedIso,
                                formattedDate = formatIsoDate(updatedIso),
                                genres = genres
                            )
                        )
                    }
                }
            }
        }

        // 4b. Legacy user_anime reviews fallback (adds only entries not already in community_notes)
        val selectCols = "id,user_id,anime_id,status,episode_progress,personal_rating,review_notes,updated_at,anime(anilist_id,title_english,title_romaji,cover_image,episodes,genres)"
        val revReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/user_anime?select=$selectCols&order=updated_at.desc&limit=150")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $bearer")
            .build()
        httpClient.newCall(revReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val arr = gson.fromJson(resp.body?.string().orEmpty(), JsonArray::class.java) ?: JsonArray()
                arr.forEach { el ->
                    val row = el.asJsonObject
                    val rowUserId = row.get("user_id").asStringOrNull().orEmpty()
                    if (rowUserId.isNotBlank() && leftOrRemovedUsers.contains(rowUserId)) return@forEach
                    val rawRating = row.get("personal_rating").asFloatOrNull() ?: 0f
                    val stars = if (rawRating > 5f) rawRating / 2f else rawRating
                    val notes = row.get("review_notes").asStringOrNull()?.trim().orEmpty()
                    if (stars <= 0f && notes.isBlank()) return@forEach

                    val animeObj = row.get("anime")?.takeIf { it.isJsonObject }?.asJsonObject
                    val animeId = animeObj?.get("anilist_id").asIntOrNull() ?: row.get("anime_id").asIntOrNull() ?: 0
                    val key = "$rowUserId:$animeId"
                    if (!seenKeys.add(key)) return@forEach
                    val rawEng = animeObj?.get("title_english").asStringOrNull()
                    val rawRom = animeObj?.get("title_romaji").asStringOrNull()
                    val title = DesktopEnglishTitleMapper.resolve(rawEng, rawRom, null, false)
                        ?: rawEng?.takeIf { it.isNotBlank() }
                        ?: rawRom?.takeIf { it.isNotBlank() }
                        ?: "Anime #$animeId"
                    val cover = animeObj?.get("cover_image").asStringOrNull().orEmpty()
                    val genres = animeObj?.get("genres")?.takeIf { it.isJsonArray }?.asJsonArray
                        ?.mapNotNull { it.asStringOrNull()?.trim()?.takeIf { g -> g.isNotBlank() } }
                        .orEmpty()
                    val (uname, avKey) = profMap[rowUserId] ?: (
                        if (rowUserId == uid) (resolvedUsername ?: active.username) to (resolvedAvatar ?: active.avatarKey)
                        else "Anime Lover" to DesktopAvatar.AVATAR_3.key
                    )
                    val updatedIso = row.get("updated_at").asStringOrNull().orEmpty()
                    communityList.add(
                        DesktopCommunityReview(
                            id = row.get("id").asStringOrNull() ?: "$rowUserId-$animeId",
                            userId = rowUserId,
                            animeId = animeId,
                            animeTitle = title,
                            animeCover = cover,
                            stars = stars,
                            notes = notes,
                            username = uname,
                            avatarKey = avKey,
                            updatedAt = updatedIso,
                            formattedDate = formatIsoDate(updatedIso),
                            genres = genres
                        )
                    )
                }
            }
        }
    }

    CloudSyncSnapshot(
        refreshedSession = active,
        username = resolvedUsername,
        avatarKey = resolvedAvatar,
        watchlist = watchlistResult,
        homeIds = homeIdsResult,
        collections = collectionsResult,
        communityReviews = communityList
    )
}

private fun ensureAnimeCatalogSync(active: DesktopPersistedState, item: DesktopAnimeItem) {
    val animePayload = JsonObject().apply {
        addProperty("anilist_id", item.id)
        addProperty("title_english", item.title)
        addProperty("title_romaji", item.titleRomaji.ifBlank { item.title })
        addProperty("cover_image", item.coverImage)
        if (item.bannerImage.isNotBlank()) addProperty("banner_image", item.bannerImage)
        if (item.totalEpisodes != null && item.totalEpisodes > 0) addProperty("episodes", item.totalEpisodes)
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
}

private fun ensureActiveCommunityMemberSync(active: DesktopPersistedState) {
    val cmPayload = JsonObject().apply {
        addProperty("user_id", active.userId)
        addProperty("role", "member")
        addProperty("status", "active")
    }
    val cmReq = Request.Builder()
        .url("$SUPABASE_URL/rest/v1/community_members?on_conflict=user_id")
        .addHeader("apikey", SUPABASE_ANON_KEY)
        .addHeader("Authorization", "Bearer ${active.accessToken}")
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
        .post(gson.toJson(cmPayload).toRequestBody("application/json".toMediaType()))
        .build()
    runCatching { httpClient.newCall(cmReq).execute().close() }

    val privPayload = JsonObject().apply {
        addProperty("user_id", active.userId)
        addProperty("reviews_visibility", "community")
        addProperty("activity_visibility", "community")
    }
    val privReq = Request.Builder()
        .url("$SUPABASE_URL/rest/v1/privacy_settings?on_conflict=user_id")
        .addHeader("apikey", SUPABASE_ANON_KEY)
        .addHeader("Authorization", "Bearer ${active.accessToken}")
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
        .post(gson.toJson(privPayload).toRequestBody("application/json".toMediaType()))
        .build()
    runCatching { httpClient.newCall(privReq).execute().close() }
}

private suspend fun saveAnimeToSupabase(
    session: DesktopPersistedState,
    item: DesktopAnimeItem,
    syncHomeMembership: Boolean = true
): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    if (session.isGuest || session.userId.isBlank() || session.accessToken.isBlank()) {
        return@withContext Result.success(session)
    }
    runCatching {
        val active = refreshTokenIfNeeded(session)
        ensureAnimeCatalogSync(active, item)

        // 1. Upsert permanent Library row in user_anime
        val userAnimePayload = JsonObject().apply {
            addProperty("user_id", active.userId)
            addProperty("anime_id", item.id)
            addProperty("status", item.watchStatus)
            addProperty("episode_progress", item.watchedEpisodes)
            if (item.totalEpisodes != null && item.totalEpisodes > 0) addProperty("total_episodes", item.totalEpisodes)
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
        httpClient.newCall(uaReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException(parsePostgrestError(resp.body?.string().orEmpty(), "Could not save anime (${resp.code})"))
            }
        }

        // 2. Sync independent Home tracking ONLY when explicitly requested or removed by lifecycle
        if (syncHomeMembership) {
            val shouldDropFromHome = DesktopTrackingRules.shouldRemoveFromHome(
                watchStatus = item.watchStatus,
                officialStatus = item.status,
                totalEpisodes = item.totalEpisodes,
                watchedEpisodes = item.watchedEpisodes
            )
            if (item.inHome && !shouldDropFromHome) {
                val htPayload = JsonObject().apply {
                    addProperty("user_id", active.userId)
                    addProperty("anime_id", item.id)
                    if (item.airingDayOfWeek in 1..7) addProperty("airing_day_of_week", item.airingDayOfWeek)
                }
                val htReq = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/home_tracking?on_conflict=user_id,anime_id")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${active.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(gson.toJson(htPayload).toRequestBody("application/json".toMediaType()))
                    .build()
                runCatching { httpClient.newCall(htReq).execute().close() }
            } else if (!item.inHome || shouldDropFromHome) {
                val delHtReq = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/home_tracking?user_id=eq.${active.userId}&anime_id=eq.${item.id}")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${active.accessToken}")
                    .delete()
                    .build()
                runCatching { httpClient.newCall(delHtReq).execute().close() }
            }
        }

        // 3. Also mirror rating/notes to independent community_notes if present (NEVER adds to Home)
        if (item.userStars > 0f || item.notes.isNotBlank()) {
            val cnPayload = JsonObject().apply {
                addProperty("user_id", active.userId)
                addProperty("anime_id", item.id)
                if (item.userStars > 0f) addProperty("rating", item.userStars * 2f)
                addProperty("note_text", item.notes.trim().take(500))
                addProperty("updated_at", Instant.now().toString())
            }
            val cnReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/community_notes?on_conflict=user_id,anime_id")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${active.accessToken}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(gson.toJson(cnPayload).toRequestBody("application/json".toMediaType()))
                .build()
            runCatching { httpClient.newCall(cnReq).execute().close() }
            ensureActiveCommunityMemberSync(active)
        }
        active
    }
}

private suspend fun addAnimeToHomeInSupabase(
    session: DesktopPersistedState,
    item: DesktopAnimeItem
): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    if (session.isGuest || session.userId.isBlank() || session.accessToken.isBlank()) {
        return@withContext Result.success(session)
    }
    runCatching {
        val active = refreshTokenIfNeeded(session)
        ensureAnimeCatalogSync(active, item)
        val rpcPayload = JsonObject().apply {
            addProperty("p_anime_id", item.id)
            if (item.airingDayOfWeek in 1..7) addProperty("p_airing_day", item.airingDayOfWeek)
        }
        val rpcReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/rpc/add_to_home")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(rpcPayload).toRequestBody("application/json".toMediaType()))
            .build()
        val rpcOk = httpClient.newCall(rpcReq).execute().use { it.isSuccessful }
        if (!rpcOk) {
            // Fallback before migration is applied
            saveAnimeToSupabase(active, item.copy(inWatchlist = true, inHome = true), syncHomeMembership = true).getOrThrow()
        } else {
            active
        }
    }
}

private suspend fun removeAnimeFromHomeInSupabase(
    session: DesktopPersistedState,
    animeId: Int
): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    if (session.isGuest || session.userId.isBlank() || session.accessToken.isBlank()) {
        return@withContext Result.success(session)
    }
    runCatching {
        val active = refreshTokenIfNeeded(session)
        val rpcPayload = JsonObject().apply { addProperty("p_anime_id", animeId) }
        val rpcReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/rpc/remove_from_home")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(rpcPayload).toRequestBody("application/json".toMediaType()))
            .build()
        val rpcOk = httpClient.newCall(rpcReq).execute().use { it.isSuccessful }
        if (!rpcOk) {
            val delReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/home_tracking?user_id=eq.${active.userId}&anime_id=eq.$animeId")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${active.accessToken}")
                .delete()
                .build()
            runCatching { httpClient.newCall(delReq).execute().close() }
        }
        active
    }
}

private suspend fun submitCommunityNoteInSupabase(
    session: DesktopPersistedState,
    animeId: Int,
    animeTitle: String,
    animeCover: String,
    stars: Float,
    note: String
): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    if (session.isGuest || session.userId.isBlank() || session.accessToken.isBlank()) {
        return@withContext Result.success(session)
    }
    runCatching {
        val active = refreshTokenIfNeeded(session)
        ensureAnimeCatalogSync(
            active,
            DesktopAnimeItem(id = animeId, title = animeTitle, coverImage = animeCover)
        )
        ensureActiveCommunityMemberSync(active)

        val rpcPayload = JsonObject().apply {
            addProperty("p_anime_id", animeId)
            if (stars > 0f) addProperty("p_rating", stars * 2f)
            addProperty("p_note_text", note.trim().take(500))
        }
        val rpcReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/rpc/upsert_community_note")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(rpcPayload).toRequestBody("application/json".toMediaType()))
            .build()
        val rpcOk = httpClient.newCall(rpcReq).execute().use { it.isSuccessful }
        if (!rpcOk) {
            // Direct community_notes table fallback (NEVER touches home_tracking!)
            val cnPayload = JsonObject().apply {
                addProperty("user_id", active.userId)
                addProperty("anime_id", animeId)
                if (stars > 0f) addProperty("rating", stars * 2f)
                addProperty("note_text", note.trim().take(500))
                addProperty("updated_at", Instant.now().toString())
            }
            val cnReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/community_notes?on_conflict=user_id,anime_id")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${active.accessToken}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(gson.toJson(cnPayload).toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(cnReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException(parsePostgrestError(resp.body?.string().orEmpty(), "Could not post Community note (${resp.code})"))
                }
            }
        }
        active
    }
}

private suspend fun updateEpisodeProgressInSupabase(
    session: DesktopPersistedState,
    item: DesktopAnimeItem,
    watchedEpisodes: Int
): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    if (session.isGuest || session.userId.isBlank() || session.accessToken.isBlank()) {
        return@withContext Result.success(session)
    }
    runCatching {
        val active = refreshTokenIfNeeded(session)
        ensureAnimeCatalogSync(active, item)
        val rpcPayload = JsonObject().apply {
            addProperty("p_anime_id", item.id)
            addProperty("p_watched_episodes", watchedEpisodes)
        }
        val rpcReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/rpc/update_episode_progress")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(rpcPayload).toRequestBody("application/json".toMediaType()))
            .build()
        val rpcRes = httpClient.newCall(rpcReq).execute().use { resp ->
            resp.isSuccessful to resp.body?.string().orEmpty()
        }
        if (!rpcRes.first) {
            // Fallback to direct user_anime update when RPC hasn't been deployed yet
            val autoDone = DesktopTrackingRules.shouldAutoComplete(item.status, item.totalEpisodes, watchedEpisodes)
            val nextStatus = if (autoDone && item.watchStatus != "dropped") "completed" else item.watchStatus
            val updated = item.copy(
                watchedEpisodes = watchedEpisodes,
                watchStatus = nextStatus,
                inHome = if (autoDone) false else item.inHome
            )
            saveAnimeToSupabase(active, updated, syncHomeMembership = autoDone).getOrThrow()
        } else {
            active
        }
    }
}

private suspend fun deleteAnimeFromSupabase(
    session: DesktopPersistedState,
    animeId: Int
): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    if (session.isGuest || session.userId.isBlank() || session.accessToken.isBlank()) {
        return@withContext Result.success(session)
    }
    runCatching {
        val active = refreshTokenIfNeeded(session)
        // Remove from Home tracking (if present) and permanent Library (explicit delete) — NEVER deletes community_notes!
        val delHt = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/home_tracking?user_id=eq.${active.userId}&anime_id=eq.$animeId")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .delete()
            .build()
        runCatching { httpClient.newCall(delHt).execute().close() }

        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/user_anime?user_id=eq.${active.userId}&anime_id=eq.$animeId")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .delete()
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException(parsePostgrestError(resp.body?.string().orEmpty(), "Could not delete anime (${resp.code})"))
            }
        }
        active
    }
}

private suspend fun createCollectionInSupabase(
    session: DesktopPersistedState,
    title: String,
    description: String
): Result<Pair<DesktopCollectionItem, DesktopPersistedState>> = withContext(Dispatchers.IO) {
    val cleanTitle = title.trim()
    val cleanDesc = description.trim()
    if (cleanTitle.isEmpty()) {
        return@withContext Result.failure(IllegalArgumentException("Collection name cannot be empty."))
    }
    if (session.isGuest || session.userId.isBlank() || session.accessToken.isBlank()) {
        return@withContext Result.success(
            DesktopCollectionItem(
                id = "local-${System.currentTimeMillis()}",
                title = cleanTitle,
                description = cleanDesc,
                count = 0
            ) to session
        )
    }
    runCatching {
        val active = refreshTokenIfNeeded(session)
        val payload = JsonObject().apply {
            addProperty("creator_id", active.userId)
            addProperty("name", cleanTitle.take(64))
            addProperty("description", cleanDesc)
            // Schema constraint: CHECK (visibility IN ('private', 'community'))
            addProperty("visibility", "community")
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
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException(parsePostgrestError(body, "Could not create collection (${resp.code})"))
            }
            val arr = gson.fromJson(body, JsonArray::class.java)
            val row = arr?.firstOrNull()?.asJsonObject
            val item = DesktopCollectionItem(
                id = row?.get("id").asStringOrNull() ?: "local-${System.currentTimeMillis()}",
                title = row?.get("name").asStringOrNull()?.takeIf { it.isNotBlank() } ?: cleanTitle,
                description = row?.get("description").asStringOrNull() ?: cleanDesc,
                count = 0
            )
            item to active
        }
    }
}

private suspend fun updateProfileInSupabase(
    session: DesktopPersistedState,
    username: String,
    avatarKey: String
): Result<DesktopPersistedState> = withContext(Dispatchers.IO) {
    val cleanUsername = username.trim()
    if (cleanUsername.length < 2 || cleanUsername.length > 32) {
        return@withContext Result.failure(IllegalArgumentException("Username must be between 2 and 32 characters."))
    }
    if (session.isGuest || session.userId.isBlank() || session.accessToken.isBlank()) {
        return@withContext Result.success(session.copy(username = cleanUsername, avatarKey = avatarKey))
    }
    runCatching {
        val active = refreshTokenIfNeeded(session)

        // 1. Validate username uniqueness / cooldown if changed
        if (!cleanUsername.equals(active.username.trim(), ignoreCase = true)) {
            val checkPayload = JsonObject().apply { addProperty("p_username", cleanUsername) }
            val checkReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/rpc/check_username_status")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${active.accessToken}")
                .addHeader("Content-Type", "application/json")
                .post(gson.toJson(checkPayload).toRequestBody("application/json".toMediaType()))
                .build()
            runCatching {
                httpClient.newCall(checkReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val obj = gson.fromJson(resp.body?.string().orEmpty(), JsonObject::class.java)
                        val available = obj?.get("available")?.takeIf { !it.isJsonNull }?.asBoolean ?: true
                        if (!available) {
                            val reason = obj?.get("reason").asStringOrNull() ?: "That username is already taken."
                            throw IllegalArgumentException(reason)
                        }
                    }
                }
            }.onFailure { err ->
                if (err is IllegalArgumentException) throw err
            }
        }

        // 2. PATCH ONLY valid public.profiles columns (NO email column!)
        val patchPayload = JsonObject().apply {
            addProperty("username", cleanUsername)
            addProperty("avatar_key", DesktopAvatar.fromKey(avatarKey).key)
            add("avatar_url", com.google.gson.JsonNull.INSTANCE)
            addProperty("updated_at", Instant.now().toString())
        }
        val patchReq = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/profiles?id=eq.${active.userId}")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .patch(gson.toJson(patchPayload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(patchReq).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException(parsePostgrestError(body, "Could not update profile (${resp.code})"))
            }
            val arr = runCatching { gson.fromJson(body, JsonArray::class.java) }.getOrNull()
            if (arr == null || arr.size() == 0) {
                // Row didn't exist yet -> INSERT profile row cleanly without email
                val insertPayload = JsonObject().apply {
                    addProperty("id", active.userId)
                    addProperty("username", cleanUsername)
                    addProperty("avatar_key", DesktopAvatar.fromKey(avatarKey).key)
                    add("avatar_url", com.google.gson.JsonNull.INSTANCE)
                    addProperty("role", "member")
                    addProperty("can_generate_invites", false)
                    addProperty("updated_at", Instant.now().toString())
                }
                val insReq = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/profiles")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${active.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(gson.toJson(insertPayload).toRequestBody("application/json".toMediaType()))
                    .build()
                httpClient.newCall(insReq).execute().use { insResp ->
                    val insBody = insResp.body?.string().orEmpty()
                    if (!insResp.isSuccessful) {
                        throw IllegalStateException(parsePostgrestError(insBody, "Could not create profile (${insResp.code})"))
                    }
                }
            }
        }

        // 3. Also sync Auth user metadata so onboarding_completed = true across all devices
        runCatching {
            val authMeta = JsonObject().apply {
                add(
                    "data",
                    JsonObject().apply {
                        addProperty("username", cleanUsername)
                        addProperty("avatar_key", DesktopAvatar.fromKey(avatarKey).key)
                        addProperty("onboarding_completed", true)
                    }
                )
            }
            val authReq = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/user")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${active.accessToken}")
                .addHeader("Content-Type", "application/json")
                .put(gson.toJson(authMeta).toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(authReq).execute().close()
        }

        val finalState = active.copy(
            username = cleanUsername,
            avatarKey = DesktopAvatar.fromKey(avatarKey).key,
            onboardingCompleted = true
        )
        DesktopStorage.saveActive(finalState)
        finalState
    }
}

private suspend fun checkDesktopUsernameAvailable(
    session: DesktopPersistedState,
    username: String
): Result<Unit> = withContext(Dispatchers.IO) {
    val clean = username.trim()
    if (clean.length !in 2..32) {
        return@withContext Result.failure(IllegalArgumentException("Username must be between 2 and 32 characters."))
    }
    if (clean.contains("@")) {
        return@withContext Result.failure(IllegalArgumentException("Usernames cannot contain @ or be an email address."))
    }
    if (!clean.matches(Regex("^[A-Za-z0-9_ .-]+$"))) {
        return@withContext Result.failure(IllegalArgumentException("Use letters, numbers, spaces, dots, hyphens, or underscores."))
    }
    runCatching {
        val active = refreshTokenIfNeeded(session)
        val payload = JsonObject().apply { addProperty("p_username", clean) }
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/rpc/check_username_status")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${active.accessToken.ifBlank { SUPABASE_ANON_KEY }}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
                val obj = gson.fromJson(resp.body?.string().orEmpty(), JsonObject::class.java)
                val available = obj?.get("available")?.takeIf { !it.isJsonNull }?.asBoolean ?: true
                if (!available) {
                    val reason = obj?.get("reason").asStringOrNull() ?: "That username is already taken. Please pick another."
                    throw IllegalArgumentException(reason)
                }
            }
        }
    }
}

private data class DesktopDiscoverPage(
    val items: List<DesktopAnimeItem>,
    val hasNextPage: Boolean
)

private fun currentDesktopAnimeSeason(): String {
    return when (LocalDate.now().monthValue) {
        in 1..3 -> "WINTER"
        in 4..6 -> "SPRING"
        in 7..9 -> "SUMMER"
        else -> "FALL"
    }
}

private suspend fun fetchAniListDiscover(
    mode: String,
    genre: String,
    search: String,
    safeMode: Boolean,
    page: Int = 1,
    perPage: Int = 30
): DesktopDiscoverPage = withContext(Dispatchers.IO) {
    val trimmedSearch = search.trim()
    val baseSort = when (mode) {
        "popular" -> "POPULARITY_DESC"
        "top" -> "SCORE_DESC"
        "seasonal" -> "POPULARITY_DESC"
        "donghua" -> "POPULARITY_DESC"
        "airing" -> "TRENDING_DESC"
        else -> "TRENDING_DESC"
    }
    val sortExpr = if (trimmedSearch.isNotEmpty()) {
        if (baseSort == "SEARCH_MATCH") "[SEARCH_MATCH]" else "[SEARCH_MATCH, $baseSort]"
    } else {
        "[$baseSort]"
    }
    val statusFilter = if (mode == "airing") ", status: RELEASING" else ""
    val seasonalFilter = if (mode == "seasonal") {
        ", season: ${currentDesktopAnimeSeason()}, seasonYear: ${LocalDate.now().year}"
    } else ""
    val countryFilter = if (mode == "donghua") ", countryOfOrigin: \"CN\"" else ""
    val adultFilter = if (safeMode) ", isAdult: false" else ""

    val extraQueries = if (page == 1 && trimmedSearch.length >= 2) {
        DesktopEnglishTitleMapper.expandSearchQueries(
            rawQuery = trimmedSearch,
            donghuaOnly = mode == "donghua",
            maxExtra = 2
        )
    } else {
        emptyList()
    }

    val mediaSelection = """
              id
              title { english romaji }
              synonyms
              popularity
              countryOfOrigin
              coverImage { extraLarge large }
              bannerImage
              format
              status
              seasonYear
              episodes
              duration
              averageScore
              genres
              nextAiringEpisode { airingAt episode }
              description(asHtml: false)
    """.trimIndent()

    val declArgs = mutableListOf("${'$'}page: Int", "${'$'}perPage: Int", "${'$'}search: String", "${'$'}genre: String")
    extraQueries.indices.forEach { idx -> declArgs.add("${'$'}alias$idx: String") }
    val aliasBlocks = buildString {
        extraQueries.indices.forEach { idx ->
            append(
                """
          alias$idx: Page(page: 1, perPage: 12) {
            media(type: ANIME, sort: [SEARCH_MATCH, POPULARITY_DESC], search: ${'$'}alias$idx, genre: ${'$'}genre$statusFilter$seasonalFilter$countryFilter$adultFilter) {
$mediaSelection
            }
          }
                """.trimEnd()
            )
            append('\n')
        }
    }

    val query = """
        query(${declArgs.joinToString(", ")}) {
          primary: Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            pageInfo {
              hasNextPage
              currentPage
            }
            media(type: ANIME, sort: $sortExpr, search: ${'$'}search, genre: ${'$'}genre$statusFilter$seasonalFilter$countryFilter$adultFilter) {
$mediaSelection
            }
          }
$aliasBlocks        }
    """.trimIndent()

    data class RawParsed(
        val item: DesktopAnimeItem,
        val officialEnglish: String?,
        val synonyms: List<String>,
        val popularity: Int
    )

    fun parseMediaArray(mediaArr: JsonArray?): List<RawParsed> {
        if (mediaArr == null) return emptyList()
        return mediaArr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val obj = el.asJsonObject
            val id = obj.get("id").asIntOrNull() ?: return@mapNotNull null
            val titleObj = obj.get("title")?.takeIf { it.isJsonObject }?.asJsonObject
            val officialEnglish = titleObj?.get("english").asStringOrNull()
            val romaji = titleObj?.get("romaji").asStringOrNull().orEmpty()
            val synonyms = obj.get("synonyms")?.takeIf { it.isJsonArray }?.asJsonArray
                ?.mapNotNull { it.asStringOrNull()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                ?: emptyList()
            val country = obj.get("countryOfOrigin").asStringOrNull() ?: "JP"
            val isDonghua = country.equals("CN", ignoreCase = true)
            val title = DesktopEnglishTitleMapper.resolve(
                titleEnglish = officialEnglish,
                titleRomaji = romaji,
                synonyms = synonyms,
                isDonghua = isDonghua
            ) ?: officialEnglish?.takeIf { it.isNotBlank() }
                ?: romaji.ifBlank { "Anime #$id" }
            val coverObj = obj.get("coverImage")?.takeIf { it.isJsonObject }?.asJsonObject
            val cover = coverObj?.get("extraLarge").asStringOrNull()
                ?: coverObj?.get("large").asStringOrNull().orEmpty()
            val banner = obj.get("bannerImage").asStringOrNull().orEmpty()
            val format = obj.get("format").asStringOrNull() ?: "TV"
            val status = obj.get("status").asStringOrNull() ?: "RELEASING"
            val rawEps = obj.get("episodes").asIntOrNull()?.takeIf { it > 0 }
            val nextAiringObj = obj.get("nextAiringEpisode")?.takeIf { it.isJsonObject }?.asJsonObject
            val nextAiring = nextAiringObj?.get("airingAt").asLongOrNull()
            val nextEpNum = nextAiringObj?.get("episode").asIntOrNull()
            val eps = when {
                format.equals("MOVIE", ignoreCase = true) -> rawEps ?: 1
                status.equals("RELEASING", ignoreCase = true) && isDonghua && rawEps != null && nextEpNum != null && nextEpNum > rawEps -> null
                else -> rawEps
            }
            val year = obj.get("seasonYear").asIntOrNull()
            val score100 = obj.get("averageScore").asIntOrNull() ?: 0
            val popularity = obj.get("popularity").asIntOrNull() ?: 0
            val desc = obj.get("description").asStringOrNull().orEmpty()
                .replace(Regex("<[^>]+>"), "")
            val genresList = obj.get("genres")?.takeIf { it.isJsonArray }?.asJsonArray
                ?.mapNotNull { it.asStringOrNull() } ?: emptyList()

            RawParsed(
                item = DesktopAnimeItem(
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
                ),
                officialEnglish = officialEnglish,
                synonyms = synonyms,
                popularity = popularity
            )
        }
    }

    val vars = JsonObject().apply {
        addProperty("page", page)
        addProperty("perPage", perPage)
        if (trimmedSearch.isNotEmpty()) addProperty("search", trimmedSearch)
        if (genre != "All Genres") addProperty("genre", genre)
        extraQueries.forEachIndexed { idx, alias -> addProperty("alias$idx", alias) }
    }
    val payload = JsonObject().apply {
        addProperty("query", query)
        add("variables", vars)
    }

    var primaryHasNext = false
    var anilistSuccess = false
    val mergedById = LinkedHashMap<Int, RawParsed>()

    var attempt = 0
    while (attempt < 3) {
        attempt++
        var retryWaitMs = 0L
        val ok = runCatching {
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 429 || resp.code in 500..599) {
                    val retrySec = resp.header("Retry-After")?.toLongOrNull()
                    retryWaitMs = if (retrySec != null && retrySec in 1..4) retrySec * 1000L else 850L * attempt
                    return@use false
                }
                if (!resp.isSuccessful) return@use false
                val body = resp.body?.string().orEmpty()
                val root = gson.fromJson(body, JsonObject::class.java)
                val dataObj = root?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject ?: return@use false
                val primaryObj = dataObj.get("primary")?.takeIf { it.isJsonObject }?.asJsonObject
                val primaryParsed = parseMediaArray(primaryObj?.get("media")?.takeIf { it.isJsonArray }?.asJsonArray)
                primaryHasNext = (primaryObj?.get("pageInfo")?.takeIf { it.isJsonObject }?.asJsonObject
                    ?.get("hasNextPage")?.takeIf { !it.isJsonNull }?.asBoolean ?: false) && primaryParsed.isNotEmpty()
                primaryParsed.forEach { if (!mergedById.containsKey(it.item.id)) mergedById[it.item.id] = it }
                extraQueries.indices.forEach { idx ->
                    val aliasObj = dataObj.get("alias$idx")?.takeIf { it.isJsonObject }?.asJsonObject
                    parseMediaArray(aliasObj?.get("media")?.takeIf { it.isJsonArray }?.asJsonArray)
                        .forEach { if (!mergedById.containsKey(it.item.id)) mergedById[it.item.id] = it }
                }
                true
            }
        }.getOrDefault(false)
        if (ok) {
            anilistSuccess = true
            break
        }
        if (attempt < 3) delay(if (retryWaitMs > 0L) retryWaitMs else 700L * attempt)
    }

    // Automatic Jikan v4 fallback if AniList rate-limited or failed during a keyword search
    if (!anilistSuccess && trimmedSearch.isNotEmpty()) {
        runCatching {
            val encodedQ = java.net.URLEncoder.encode(trimmedSearch, "UTF-8")
            val jikanUrl = "https://api.jikan.moe/v4/anime?q=$encodedQ&page=$page&limit=${perPage.coerceIn(1, 25)}${if (safeMode) "&sfw=true" else ""}"
            val req = Request.Builder().url(jikanUrl).header("Accept", "application/json").build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val body = resp.body?.string().orEmpty()
                val root = gson.fromJson(body, JsonObject::class.java)
                val arr = root?.get("data")?.takeIf { it.isJsonArray }?.asJsonArray ?: return@use
                primaryHasNext = root.get("pagination")?.takeIf { it.isJsonObject }?.asJsonObject
                    ?.get("has_next_page")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
                arr.forEach { el ->
                    if (!el.isJsonObject) return@forEach
                    val obj = el.asJsonObject
                    val id = obj.get("mal_id").asIntOrNull() ?: return@forEach
                    val rawTitle = obj.get("title").asStringOrNull().orEmpty()
                    val officialEng = obj.get("title_english").asStringOrNull()
                    val syns = obj.get("title_synonyms")?.takeIf { it.isJsonArray }?.asJsonArray
                        ?.mapNotNull { it.asStringOrNull()?.trim()?.takeIf { s -> s.isNotEmpty() } } ?: emptyList()
                    val jType = obj.get("type").asStringOrNull()?.uppercase() ?: "TV"
                    val mappedFmt = when (jType) {
                        "MOVIE" -> "MOVIE"
                        "OVA" -> "OVA"
                        "ONA" -> "ONA"
                        "SPECIAL", "TV SPECIAL" -> "SPECIAL"
                        else -> "TV"
                    }
                    val isDonghua = mappedFmt == "ONA" && DesktopEnglishTitleMapper.isLikelyPinyin(rawTitle)
                    val title = DesktopEnglishTitleMapper.resolve(officialEng, rawTitle, syns, isDonghua)
                        ?: officialEng ?: rawTitle.ifBlank { "Anime #$id" }
                    val jpg = obj.get("images")?.takeIf { it.isJsonObject }?.asJsonObject
                        ?.get("jpg")?.takeIf { it.isJsonObject }?.asJsonObject
                    val cover = jpg?.get("large_image_url").asStringOrNull() ?: jpg?.get("image_url").asStringOrNull().orEmpty()
                    val jStatus = obj.get("status").asStringOrNull()?.lowercase().orEmpty()
                    val mappedStatus = when {
                        jStatus.contains("currently airing") -> "RELEASING"
                        jStatus.contains("not yet") -> "NOT_YET_RELEASED"
                        else -> "FINISHED"
                    }
                    val eps = obj.get("episodes").asIntOrNull()?.takeIf { it > 0 } ?: if (mappedFmt == "MOVIE") 1 else null
                    val scoreFloat = obj.get("score")?.takeIf { !it.isJsonNull }?.asFloat ?: 0f
                    val year = obj.get("year").asIntOrNull()
                    val pop = obj.get("members").asIntOrNull() ?: 0
                    val genresList = obj.get("genres")?.takeIf { it.isJsonArray }?.asJsonArray
                        ?.mapNotNull { it.takeIf { g -> g.isJsonObject }?.asJsonObject?.get("name").asStringOrNull() } ?: emptyList()
                    if (!mergedById.containsKey(id)) {
                        mergedById[id] = RawParsed(
                            item = DesktopAnimeItem(
                                id = id,
                                title = title,
                                titleRomaji = rawTitle,
                                coverImage = cover,
                                bannerImage = "",
                                format = mappedFmt,
                                status = mappedStatus,
                                totalEpisodes = eps,
                                catalogStars = if (scoreFloat > 0f) (scoreFloat / 2f) else 0f,
                                airingDayOfWeek = (id % 7) + 1,
                                seasonYear = year,
                                genres = genresList,
                                synopsis = obj.get("synopsis").asStringOrNull().orEmpty()
                            ),
                            officialEnglish = officialEng,
                            synonyms = syns,
                            popularity = pop
                        )
                    }
                }
            }
        }
    }

    val finalItems = if (page == 1 && trimmedSearch.isNotEmpty()) {
        mergedById.values
            .sortedWith(
                compareByDescending<RawParsed> {
                    DesktopEnglishTitleMapper.scoreKeywordMatch(
                        rawQuery = trimmedSearch,
                        resolvedEnglish = it.item.title,
                        officialEnglish = it.officialEnglish,
                        romaji = it.item.titleRomaji,
                        synonyms = it.synonyms
                    )
                }.thenByDescending { it.popularity }
            )
            .map { it.item }
    } else {
        mergedById.values.map { it.item }
    }

    DesktopDiscoverPage(items = finalItems, hasNextPage = primaryHasNext && finalItems.isNotEmpty())
}

data class DesktopSeasonEntry(
    val id: Int,
    val seasonNumber: Int,
    val title: String,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val seasonYear: Int?,
    val isCurrent: Boolean,
    val partNumber: Int? = null,
    val shortLabel: String = seasonNumber.toString(),
    val displayLabel: String = if (partNumber != null && partNumber > 1) {
        "Season $seasonNumber • Part $partNumber"
    } else {
        "Season $seasonNumber"
    }
)

private data class DesktopChainNode(
    val id: Int,
    val type: String?,
    val countryOfOrigin: String?,
    val titleEnglish: String?,
    val titleUserPreferred: String?,
    val titleRomaji: String?,
    val synonyms: List<String>,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val duration: Int?,
    val seasonYear: Int?,
    val startYear: Int?,
    val startMonth: Int?,
    val startDay: Int?,
    val edges: List<Pair<String, DesktopChainNode>>
) {
    val displayTitle: String
        get() {
            val isDonghua = countryOfOrigin.equals("CN", ignoreCase = true)
            return DesktopEnglishTitleMapper.resolve(
                titleEnglish = titleEnglish,
                titleRomaji = titleRomaji ?: titleUserPreferred,
                synonyms = synonyms,
                isDonghua = isDonghua
            ) ?: titleEnglish?.takeIf { it.isNotBlank() }
                ?: titleUserPreferred?.takeIf { it.isNotBlank() }
                ?: titleRomaji?.takeIf { it.isNotBlank() }
                ?: "Anime #$id"
        }
}

private object DesktopSeasonResolver {
    private const val MAX_WAVES = 25
    private const val MAX_NODES = 60
    private const val BATCH_SIZE = 25
    private val NON_SEASON_FORMATS = setOf("MOVIE", "OVA", "MUSIC", "MANGA", "NOVEL", "ONE_SHOT")
    private val MAIN_EPISODIC_FORMATS = setOf("TV", "ONA", "TV_SHORT")
    private val EXPLICIT_SEASON_TITLE_REGEX = Regex(
        "(?i)(?:\\bseason\\s*(\\d{1,2})\\b|\\b(\\d{1,2})(?:st|nd|rd|th)\\s+season\\b|\\bs(\\d{1,2})\\b|\\bdi\\s*(\\d{1,2})\\s*ji\\b|\\bdai\\s*(\\d{1,2})\\s*ki\\b|\\bfinal\\s+season\\b|\\bfinal\\s+chapters?\\b)"
    )
    private val EXPLICIT_SEASON_NUM_REGEX = Regex(
        "(?i)(?:\\bseason\\s*(\\d{1,2})\\b|\\b(\\d{1,2})(?:st|nd|rd|th)\\s+season\\b|\\bs(\\d{1,2})\\b|\\bdi\\s*(\\d{1,2})\\s*ji\\b|\\bdai\\s*(\\d{1,2})\\s*ki\\b)"
    )
    private val PINYIN_SEASON_NUM_REGEX = Regex("(?i)\\bdi\\s*(yi|er|san|si|wu|liu|qi|ba|jiu|shi)\\s*ji\\b")
    private val EXPLICIT_PART_REGEX = Regex("(?i)(?:\\bpart\\s*(\\d{1,2})\\b|\\bcour\\s*(\\d{1,2})\\b|\\b(\\d{1,2})(?:st|nd|rd|th)\\s+cour\\b)")
    private val SECOND_HALF_REGEX = Regex("(?i)\\b(?:second\\s+half|2nd\\s+half|xia\\s*ji|xia\\s*pian)\\b")
    private val ANNUAL_REGEX = Regex("(?i)\\b(?:nian\\s*fan|annual\\s+(?:edition|season))\\b")
    private val TRAILING_ROMAN_REGEX = Regex("(?:^|\\s)(II|III|IV|V|VI)(?:$|[:\\-])")
    private val NON_SEASON_SIDE_REGEX = Regex(
        "(?i)(?:\\bmini\\s*theater\\b|\\bxiao\\s*juchang\\b|\\bchibi\\b|\\bq[- ]?ban\\b|\\bpetite\\b|\\bmini\\s*anime\\b|\\bshort\\s*theater\\b|\\bgekijou\\b|\\blittle\\s*theater\\b|\\brecap\\b|\\bsummary\\b|\\bdigest\\b|\\bpreview\\b|\\bteaser\\b|\\btrailer\\b|\\bpv\\b|\\bcm\\b|\\bcommercial\\b|\\bpromotional\\b|\\bspecial\\s+program\\b|\\bmusic\\s+video\\b|\\btebie\\s*pian\\b|\\bfanwai\\b|\\bomake\\b|\\bpicture\\s*drama\\b|\\bprologue\\b|\\bepisode\\s*0\\b|\\bep\\.?\\s*0\\b|\\bova\\b|\\boad\\b|\\bmovie\\b|\\bjuchang\\s*ban\\b|\\bgekijouban\\b|\\bspecials?\\b|\\bspecial\\s*\\d+\\b|\\bsan\\s*nian\\s*zhi\\s*yue\\b|\\bthree[- ]year\\s+agreement\\b|\\byuan\\s*qi\\b|\\borigin\\s*(?::|$))"
    )
    private val NON_SEASON_BEFORE_ROMAN_REGEX = Regex("(?i)\\b(?:part|cour|movie|film|special|ova|oad|episode|ep|chapter|act|vol|volume)\\s*$")
    private val nodeCache = java.util.concurrent.ConcurrentHashMap<Int, DesktopChainNode>()

    private const val BATCH_QUERY = """
        query(${'$'}ids: [Int]) {
          Page(page: 1, perPage: 50) {
            media(id_in: ${'$'}ids, type: ANIME) {
              id
              type
              countryOfOrigin
              title { english romaji userPreferred }
              synonyms
              format
              status
              episodes
              duration
              seasonYear
              startDate { year month day }
              relations {
                edges {
                  relationType
                  node {
                    id
                    type
                    countryOfOrigin
                    title { english romaji userPreferred }
                    synonyms
                    format
                    status
                    episodes
                    duration
                    seasonYear
                    startDate { year month day }
                  }
                }
              }
            }
          }
        }
    """

    private fun parseChainNode(obj: JsonObject): DesktopChainNode {
        val id = obj.get("id").asInt
        val titleObj = obj.get("title")?.takeIf { it.isJsonObject }?.asJsonObject
        val startObj = obj.get("startDate")?.takeIf { it.isJsonObject }?.asJsonObject
        val synonyms = obj.get("synonyms")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { it.asStringOrNull()?.trim()?.takeIf { s -> s.isNotEmpty() } }
            ?: emptyList()
        val relEdges = mutableListOf<Pair<String, DesktopChainNode>>()
        val edgesArr = obj.get("relations")?.takeIf { it.isJsonObject }?.asJsonObject
            ?.get("edges")?.takeIf { it.isJsonArray }?.asJsonArray
        edgesArr?.forEach { el ->
            val edgeObj = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            val relType = edgeObj.get("relationType").asStringOrNull()?.uppercase()?.trim() ?: return@forEach
            if (relType != "PREQUEL" && relType != "SEQUEL") return@forEach
            val nodeObj = edgeObj.get("node")?.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            val nodeType = nodeObj.get("type").asStringOrNull()
            if (!nodeType.isNullOrBlank() && !nodeType.equals("ANIME", ignoreCase = true)) return@forEach
            relEdges.add(relType to parseChainNode(nodeObj))
        }
        return DesktopChainNode(
            id = id,
            type = obj.get("type").asStringOrNull(),
            countryOfOrigin = obj.get("countryOfOrigin").asStringOrNull(),
            titleEnglish = titleObj?.get("english").asStringOrNull(),
            titleUserPreferred = titleObj?.get("userPreferred").asStringOrNull(),
            titleRomaji = titleObj?.get("romaji").asStringOrNull(),
            synonyms = synonyms,
            format = obj.get("format").asStringOrNull(),
            status = obj.get("status").asStringOrNull(),
            episodes = obj.get("episodes").asIntOrNull(),
            duration = obj.get("duration").asIntOrNull(),
            seasonYear = obj.get("seasonYear").asIntOrNull(),
            startYear = startObj?.get("year").asIntOrNull(),
            startMonth = startObj?.get("month").asIntOrNull(),
            startDay = startObj?.get("day").asIntOrNull(),
            edges = relEdges
        )
    }

    private fun extractSeasonAndPart(node: DesktopChainNode): Triple<Int?, Int?, Boolean> {
        val primary = listOfNotNull(node.titleEnglish, node.titleUserPreferred, node.titleRomaji)
            .filter { it.isNotBlank() }.joinToString(" | ")
        val combined = (listOfNotNull(node.titleEnglish, node.titleUserPreferred, node.titleRomaji) + node.synonyms)
            .filter { it.isNotBlank() }.joinToString(" | ")
        val isAnnual = ANNUAL_REGEX.containsMatchIn(combined)

        var parsedSeason: Int? = null
        for (src in listOf(primary, combined)) {
            if (parsedSeason != null) break
            EXPLICIT_SEASON_NUM_REGEX.find(src)?.let { m ->
                val n = m.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toIntOrNull()
                if (n != null && n in 1..20) parsedSeason = n
            }
            if (parsedSeason == null) {
                PINYIN_SEASON_NUM_REGEX.find(src)?.let { m ->
                    parsedSeason = when (m.groupValues[1].lowercase()) {
                        "yi" -> 1; "er" -> 2; "san" -> 3; "si" -> 4; "wu" -> 5
                        "liu" -> 6; "qi" -> 7; "ba" -> 8; "jiu" -> 9; "shi" -> 10
                        else -> null
                    }
                }
            }
        }
        if (parsedSeason == null && !NON_SEASON_SIDE_REGEX.containsMatchIn(primary)) {
            for (t in listOfNotNull(node.titleEnglish, node.titleUserPreferred, node.titleRomaji).filter { it.isNotBlank() }) {
                val m = TRAILING_ROMAN_REGEX.find(t)
                if (m != null && !NON_SEASON_BEFORE_ROMAN_REGEX.containsMatchIn(t.substring(0, m.range.first).trim())) {
                    parsedSeason = when (m.groupValues[1]) {
                        "II" -> 2; "III" -> 3; "IV" -> 4; "V" -> 5; "VI" -> 6
                        else -> null
                    }
                    if (parsedSeason != null) break
                }
            }
        }
        var parsedPart: Int? = null
        for (src in listOf(primary, combined)) {
            if (parsedPart != null) break
            EXPLICIT_PART_REGEX.find(src)?.let { m ->
                val p = m.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toIntOrNull()
                if (p != null && p in 1..6) parsedPart = p
            }
            if (parsedPart == null && SECOND_HALF_REGEX.containsMatchIn(src)) parsedPart = 2
        }
        return Triple(parsedSeason, parsedPart, isAnnual)
    }

    private fun isSeasonCandidate(node: DesktopChainNode, franchiseHasFullSeries: Boolean): Boolean {
        if (!node.type.isNullOrBlank() && !node.type.equals("ANIME", ignoreCase = true)) return false
        if (node.status.equals("CANCELLED", ignoreCase = true)) return false
        val fmt = node.format?.uppercase()?.trim().orEmpty()
        if (fmt in NON_SEASON_FORMATS) return false

        val primary = listOfNotNull(node.titleEnglish, node.titleUserPreferred, node.titleRomaji)
            .filter { it.isNotBlank() }.joinToString(" | ")
        val hasExplicitSeason = EXPLICIT_SEASON_TITLE_REGEX.containsMatchIn(primary)
        val hasSideMarker = NON_SEASON_SIDE_REGEX.containsMatchIn(primary)

        if (fmt == "SPECIAL") {
            return Regex("(?i)(?:\\bfinal\\s+season\\b|\\bfinal\\s+chapters?\\b|\\bseason\\s+\\d+\\b)").containsMatchIn(primary) &&
                !Regex("(?i)(?:\\brecap\\b|\\bsummary\\b|\\bdigest\\b|\\bpreview\\b|\\bpv\\b|\\bspecial\\s+program\\b|\\bchibi\\b|\\bmini\\b|\\bxiao\\s*juchang\\b)").containsMatchIn(primary)
        }
        if (fmt.isNotEmpty() && fmt !in MAIN_EPISODIC_FORMATS) return false
        if (hasSideMarker && !hasExplicitSeason) return false

        if (franchiseHasFullSeries) {
            if (fmt == "TV_SHORT" && !hasExplicitSeason) return false
            if (node.duration != null && node.duration in 1..6 && !hasExplicitSeason) return false
            val (sNum, pNum, isAnn) = extractSeasonAndPart(node)
            if (fmt == "ONA" && node.status.equals("FINISHED", ignoreCase = true) &&
                node.episodes != null && node.episodes in 1..3 &&
                sNum == null && pNum == null && !isAnn
            ) return false
        }
        return true
    }

    private fun compareChrono(a: DesktopChainNode?, b: DesktopChainNode?, aId: Int, bId: Int): Int {
        val aYear = a?.startYear ?: a?.seasonYear
        val bYear = b?.startYear ?: b?.seasonYear
        val aUndated = if (aYear == null && a?.status.equals("NOT_YET_RELEASED", ignoreCase = true)) 1 else 0
        val bUndated = if (bYear == null && b?.status.equals("NOT_YET_RELEASED", ignoreCase = true)) 1 else 0
        if (aUndated != bUndated) return aUndated.compareTo(bUndated)
        val yCmp = (aYear ?: 9999).compareTo(bYear ?: 9999)
        if (yCmp != 0) return yCmp
        val mCmp = (a?.startMonth ?: 13).compareTo(b?.startMonth ?: 13)
        if (mCmp != 0) return mCmp
        val dCmp = (a?.startDay ?: 32).compareTo(b?.startDay ?: 32)
        if (dCmp != 0) return dCmp
        return aId.compareTo(bId)
    }

    private suspend fun fetchBatch(ids: List<Int>): List<DesktopChainNode> {
        val unique = ids.distinct().filter { it > 0 }
        if (unique.isEmpty()) return emptyList()
        val cached = mutableListOf<DesktopChainNode>()
        val missing = mutableListOf<Int>()
        for (id in unique) {
            val hit = nodeCache[id]
            if (hit != null) cached.add(hit) else missing.add(id)
        }
        if (missing.isEmpty()) return cached

        val fetched = mutableListOf<DesktopChainNode>()
        var attempt = 0
        while (attempt < 3) {
            attempt++
            val result = runCatching {
                val idArr = JsonArray().apply { missing.forEach { add(it) } }
                val vars = JsonObject().apply { add("ids", idArr) }
                val payload = JsonObject().apply {
                    addProperty("query", BATCH_QUERY)
                    add("variables", vars)
                }
                val req = Request.Builder()
                    .url("https://graphql.anilist.co")
                    .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.code == 429) return@use -1
                    if (!resp.isSuccessful) return@use 0
                    val body = resp.body?.string().orEmpty()
                    val root = gson.fromJson(body, JsonObject::class.java)
                    val arr = root?.getAsJsonObject("data")?.getAsJsonObject("Page")?.getAsJsonArray("media")
                        ?: return@use 1
                    arr.forEach { el ->
                        val node = parseChainNode(el.asJsonObject)
                        nodeCache[node.id] = node
                        fetched.add(node)
                    }
                    1
                }
            }.getOrDefault(0)
            if (result == 1) break
            if (attempt < 3) delay(if (result == -1) 900L * attempt else 500L * attempt)
        }
        return cached + fetched
    }

    suspend fun resolveSeasons(rootId: Int): List<DesktopSeasonEntry> = withContext(Dispatchers.IO) {
        val initialNodes = fetchBatch(listOf(rootId))
        val root = initialNodes.firstOrNull { it.id == rootId } ?: return@withContext emptyList()
        if (root.edges.isEmpty()) return@withContext emptyList()

        val nodesById = LinkedHashMap<Int, DesktopChainNode>()
        val outgoing = HashMap<Int, MutableSet<Int>>()
        val expanded = HashSet<Int>()
        val queued = HashSet<Int>()
        val frontier = ArrayDeque<Int>()

        fun merge(candidate: DesktopChainNode) {
            val prev = nodesById[candidate.id]
            if (prev == null) {
                nodesById[candidate.id] = candidate
                return
            }
            nodesById[candidate.id] = if (candidate.edges.isNotEmpty() && prev.edges.isEmpty()) {
                candidate.copy(
                    countryOfOrigin = candidate.countryOfOrigin ?: prev.countryOfOrigin,
                    synonyms = if (candidate.synonyms.isNotEmpty()) candidate.synonyms else prev.synonyms,
                    startYear = candidate.startYear ?: prev.startYear,
                    startMonth = candidate.startMonth ?: prev.startMonth,
                    startDay = candidate.startDay ?: prev.startDay,
                    seasonYear = candidate.seasonYear ?: prev.seasonYear,
                    episodes = candidate.episodes ?: prev.episodes,
                    duration = candidate.duration ?: prev.duration
                )
            } else {
                prev.copy(
                    countryOfOrigin = prev.countryOfOrigin ?: candidate.countryOfOrigin,
                    synonyms = if (prev.synonyms.isNotEmpty()) prev.synonyms else candidate.synonyms,
                    startYear = prev.startYear ?: candidate.startYear,
                    startMonth = prev.startMonth ?: candidate.startMonth,
                    startDay = prev.startDay ?: candidate.startDay,
                    seasonYear = prev.seasonYear ?: candidate.seasonYear,
                    episodes = prev.episodes ?: candidate.episodes,
                    duration = prev.duration ?: candidate.duration,
                    edges = if (prev.edges.isNotEmpty()) prev.edges else candidate.edges
                )
            }
        }

        fun enqueue(id: Int) {
            if (id <= 0 || id in expanded || id in queued) return
            if (nodesById.size >= MAX_NODES && id !in nodesById) return
            queued.add(id)
            frontier.addLast(id)
        }

        fun ingest(node: DesktopChainNode) {
            merge(node)
            expanded.add(node.id)
            for ((rel, target) in node.edges) {
                if (target.id <= 0) continue
                if (target.id !in nodesById && nodesById.size >= MAX_NODES) continue
                merge(target)
                if (rel == "SEQUEL") outgoing.getOrPut(node.id) { LinkedHashSet() }.add(target.id)
                else outgoing.getOrPut(target.id) { LinkedHashSet() }.add(node.id)
                enqueue(target.id)
            }
        }

        ingest(root)

        var wave = 0
        while (frontier.isNotEmpty() && wave < MAX_WAVES && expanded.size < MAX_NODES) {
            wave++
            val batch = mutableListOf<Int>()
            while (frontier.isNotEmpty() && batch.size < BATCH_SIZE) {
                val nextId = frontier.removeFirst()
                if (nextId !in expanded) batch.add(nextId)
            }
            if (batch.isEmpty()) break
            val fetched = fetchBatch(batch)
            val fetchedIds = HashSet<Int>()
            for (n in fetched) {
                fetchedIds.add(n.id)
                ingest(n)
            }
            for (reqId in batch) {
                if (reqId !in fetchedIds) expanded.add(reqId)
            }
        }

        val franchiseHasFullSeries = nodesById.values.any { n ->
            val fmt = n.format?.uppercase()?.trim().orEmpty()
            (fmt == "TV" || fmt == "ONA") &&
                ((n.duration != null && n.duration >= 12) ||
                    (n.episodes != null && n.episodes >= 10) ||
                    (n.status.equals("RELEASING", ignoreCase = true) && (n.duration == null || n.duration >= 10)))
        }

        val seasonIds = nodesById.values.filter { isSeasonCandidate(it, franchiseHasFullSeries) }.map { it.id }.toSet()
        if (rootId !in seasonIds || seasonIds.size <= 1) return@withContext emptyList()

        val seasonOutgoing = HashMap<Int, MutableSet<Int>>()
        val inDegree = HashMap<Int, Int>()
        for (sid in seasonIds) {
            seasonOutgoing[sid] = LinkedHashSet()
            inDegree[sid] = 0
        }

        for (startId in seasonIds) {
            val seen = HashSet<Int>()
            val queue = ArrayDeque<Int>()
            outgoing[startId].orEmpty().forEach { if (seen.add(it)) queue.addLast(it) }
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                if (curr == startId) continue
                if (curr in seasonIds) {
                    if (seasonOutgoing.getValue(startId).add(curr)) {
                        inDegree[curr] = (inDegree[curr] ?: 0) + 1
                    }
                } else {
                    outgoing[curr].orEmpty().forEach { if (seen.add(it)) queue.addLast(it) }
                }
            }
        }

        val cmp = Comparator<Int> { aId, bId -> compareChrono(nodesById[aId], nodesById[bId], aId, bId) }
        val ready = java.util.PriorityQueue(cmp)
        seasonIds.filter { (inDegree[it] ?: 0) == 0 }.forEach { ready.add(it) }

        val orderedIds = mutableListOf<Int>()
        while (ready.isNotEmpty()) {
            val curr = ready.poll()
            orderedIds.add(curr)
            for (next in seasonOutgoing[curr].orEmpty()) {
                val rem = (inDegree[next] ?: 1) - 1
                inDegree[next] = rem
                if (rem == 0) ready.add(next)
            }
        }
        if (orderedIds.size < seasonIds.size) {
            val placed = orderedIds.toHashSet()
            orderedIds.addAll(seasonIds.filter { it !in placed }.sortedWith(cmp))
        }

        val entries = mutableListOf<DesktopSeasonEntry>()
        var currentSeasonNum = 0
        for (id in orderedIds) {
            val n = nodesById.getValue(id)
            val (parsedSeason, parsedPart, isAnnual) = extractSeasonAndPart(n)
            val assignedSeason: Int
            val assignedPart: Int?
            if (entries.isEmpty()) {
                assignedSeason = parsedSeason?.coerceIn(1, 20) ?: 1
                assignedPart = parsedPart
                currentSeasonNum = assignedSeason
            } else if (parsedPart != null && parsedPart >= 2 && (parsedSeason == null || parsedSeason == currentSeasonNum)) {
                assignedSeason = currentSeasonNum
                assignedPart = parsedPart
            } else if (parsedSeason != null && parsedSeason == currentSeasonNum && parsedPart != null) {
                assignedSeason = currentSeasonNum
                assignedPart = parsedPart
            } else if (parsedSeason != null && parsedSeason in (currentSeasonNum + 1)..(currentSeasonNum + 3)) {
                assignedSeason = parsedSeason
                assignedPart = parsedPart
                currentSeasonNum = assignedSeason
            } else {
                currentSeasonNum += 1
                assignedSeason = currentSeasonNum
                assignedPart = parsedPart
            }
            val shortLabel = if (assignedPart != null && assignedPart > 1) "$assignedSeason Pt $assignedPart" else assignedSeason.toString()
            val displayLabel = when {
                assignedPart != null && assignedPart > 1 -> "Season $assignedSeason • Part $assignedPart"
                isAnnual -> "Season $assignedSeason • Annual"
                else -> "Season $assignedSeason"
            }
            entries.add(
                DesktopSeasonEntry(
                    id = id,
                    seasonNumber = assignedSeason,
                    title = n.displayTitle,
                    format = n.format,
                    status = n.status,
                    episodes = n.episodes,
                    seasonYear = n.seasonYear ?: n.startYear,
                    isCurrent = id == rootId,
                    partNumber = assignedPart,
                    shortLabel = shortLabel,
                    displayLabel = displayLabel
                )
            )
        }
        entries
    }

    suspend fun fetchSingleAnimeItem(anilistId: Int): DesktopAnimeItem? = withContext(Dispatchers.IO) {
        val query = """
            query(${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id
                title { english romaji }
                synonyms
                countryOfOrigin
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
        """.trimIndent()
        runCatching {
            val vars = JsonObject().apply { addProperty("id", anilistId) }
            val payload = JsonObject().apply {
                addProperty("query", query)
                add("variables", vars)
            }
            val req = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val root = gson.fromJson(resp.body?.string().orEmpty(), JsonObject::class.java)
                val obj = root?.getAsJsonObject("data")?.get("Media")?.takeIf { it.isJsonObject }?.asJsonObject
                    ?: return@use null
                val id = obj.get("id").asInt
                val titleObj = obj.getAsJsonObject("title")
                val officialEnglish = titleObj?.get("english").asStringOrNull()
                val romaji = titleObj?.get("romaji").asStringOrNull().orEmpty()
                val synonyms = obj.get("synonyms")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { it.asStringOrNull()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                    ?: emptyList()
                val isDonghua = obj.get("countryOfOrigin").asStringOrNull().equals("CN", ignoreCase = true)
                val title = DesktopEnglishTitleMapper.resolve(
                    titleEnglish = officialEnglish,
                    titleRomaji = romaji,
                    synonyms = synonyms,
                    isDonghua = isDonghua
                ) ?: officialEnglish?.takeIf { it.isNotBlank() }
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
                val desc = obj.get("description").asStringOrNull().orEmpty().replace(Regex("<[^>]+>"), "")
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
        }.getOrNull()
    }
}

// ============================================================================
// ENTRY POINT & ROOT WORKSPACE
// ============================================================================

fun main() = application {
    val windowIcon = remember { loadDesktopWindowIconPainter() }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Anime Universe",
        icon = windowIcon,
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
    var previousScreen by remember { mutableStateOf(DesktopScreen.HOME) }
    var selectedAnime by remember { mutableStateOf<DesktopAnimeItem?>(null) }

    // Normalize persisted watchlist for backward compatibility with older vaults that didn't store inHome
    var watchlist by remember {
        mutableStateOf(
            initialPersisted.watchlist.map { item ->
                val autoDone = DesktopTrackingRules.shouldAutoComplete(item.status, item.totalEpisodes, item.watchedEpisodes)
                val normalizedStatus = if (autoDone && item.watchStatus != "dropped") "completed" else item.watchStatus
                val normalizedHome = item.inHome && normalizedStatus != "dropped" && normalizedStatus != "completed" && !autoDone
                item.copy(watchStatus = normalizedStatus, inHome = normalizedHome)
            }
        )
    }
    var collections by remember { mutableStateOf(initialPersisted.collections) }
    var communityReviews by remember { mutableStateOf<List<DesktopCommunityReview>>(emptyList()) }

    // Optimistic sync guards so 15s cloud poll never reverts in-flight edits or re-adds removed shows
    val dirtyAnimeUntilMs = remember { ConcurrentHashMap<Int, Long>() }
    val deletedAnimeUntilMs = remember { ConcurrentHashMap<Int, Long>() }
    val removedHomeUntilMs = remember { ConcurrentHashMap<Int, Long>() }
    var profileDirtyUntilMs by remember { mutableStateOf(0L) }

    // Non-intrusive toast/feedback banner
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastIsError by remember { mutableStateOf(false) }

    fun showToast(message: String, isError: Boolean = false) {
        toastMessage = message
        toastIsError = isError
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3600L)
            toastMessage = null
        }
    }

    // Remove from Home Confirmation Dialog State
    var pendingRemoveFromHome by remember { mutableStateOf<DesktopAnimeItem?>(null) }

    // Home State
    var homeAllWeek by remember { mutableStateOf(true) }
    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek.value) }

    // Discover State
    var discoverItems by remember { mutableStateOf<List<DesktopAnimeItem>>(emptyList()) }
    var discoverLoading by remember { mutableStateOf(false) }
    var discoverLoadingMore by remember { mutableStateOf(false) }
    var discoverPage by remember { mutableStateOf(1) }
    var discoverHasNextPage by remember { mutableStateOf(true) }
    var discoverLastPageStartIndex by remember { mutableStateOf(0) }
    var discoverRequestToken by remember { mutableStateOf(0) }
    var discoverMode by remember { mutableStateOf("popular") }
    var discoverGenre by remember { mutableStateOf("All Genres") }
    var discoverSearch by remember { mutableStateOf("") }
    var safeMode by remember { mutableStateOf(true) }
    var quickJumpQuery by remember { mutableStateOf("") }
    var discoverListView by remember { mutableStateOf(false) }
    var discoverShowFilters by remember { mutableStateOf(true) }

    // Library State
    var libStatusFilter by remember { mutableStateOf("all") }
    var libRatingFilter by remember { mutableStateOf("all") }
    var libSearchQuery by remember { mutableStateOf("") }
    var libSortMode by remember { mutableStateOf("recent") }
    var libListView by remember { mutableStateOf(false) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }

    // Community State
    var commRatingFilter by remember { mutableStateOf("all") }
    var commSortMode by remember { mutableStateOf("newest") }

    // Unseen Community Tags & Deep-link Jump State (Synchronized via Supabase DB + Realtime INSERT/UPDATE)
    val notificationSyncReducer = remember { DesktopNotificationSyncReducer() }
    var unseenDiscussionAnime by remember { mutableStateOf<List<DesktopUnseenDiscussionAnime>>(emptyList()) }
    var unreadNotificationCount by remember { mutableStateOf(0) }
    var notificationRealtimeEpoch by remember { mutableStateOf(0) }
    var pendingNotificationJump by remember { mutableStateOf<DesktopNotificationJumpTarget?>(null) }

    fun reconcileNotificationsFromCloud(explicitSession: DesktopPersistedState = sessionState) {
        if (!isAuthenticated || explicitSession.isGuest || explicitSession.userId.isBlank()) {
            notificationSyncReducer.reset()
            unseenDiscussionAnime = emptyList()
            unreadNotificationCount = 0
            return
        }
        val capturedUid = explicitSession.userId
        val startGen = notificationSyncReducer.currentGeneration()
        scope.launch {
            val snapPair = DesktopDiscussionService.fetchAuthoritativeUnreadSnapshot(explicitSession).getOrNull()
            val activeSession = snapPair?.second ?: explicitSession
            if (sessionState.userId != capturedUid) return@launch
            if (activeSession.accessToken != sessionState.accessToken && activeSession.accessToken.isNotBlank()) {
                sessionState = sessionState.copy(
                    accessToken = activeSession.accessToken,
                    refreshToken = activeSession.refreshToken,
                    expiresAt = activeSession.expiresAt
                )
                DesktopStorage.saveActive(sessionState)
            }
            val summaryRes = DesktopDiscussionService.fetchUnseenDiscussionAnime(activeSession)
            if (sessionState.userId != capturedUid) return@launch
            val snap = snapPair?.first
            if (snap != null) {
                if (notificationSyncReducer.applyAuthoritativeUnreadIds(snap.unreadIds, startGen)) {
                    val exactCount = notificationSyncReducer.unreadCount
                    unreadNotificationCount = exactCount
                    unseenDiscussionAnime = if (exactCount == 0) {
                        emptyList()
                    } else {
                        summaryRes.getOrNull()?.first?.unseenAnime
                            ?.filter { (snap.unreadCountByAnime[it.animeId] ?: 0) > 0 }
                            ?: unseenDiscussionAnime.filter { (snap.unreadCountByAnime[it.animeId] ?: 0) > 0 }
                    }
                }
            } else {
                summaryRes.onSuccess { (summary, _) ->
                    if (notificationSyncReducer.confirmServerUnreadCount(summary.totalUnreadCount, startGen)) {
                        unreadNotificationCount = notificationSyncReducer.unreadCount
                        unseenDiscussionAnime = if (notificationSyncReducer.unreadCount == 0) emptyList() else summary.unseenAnime
                    }
                }
            }
        }
    }

    fun refreshUnseenDiscussionAnime() {
        reconcileNotificationsFromCloud(sessionState)
    }

    // Supabase Realtime INSERT + UPDATE listener on public.user_notifications with reconnect reconciliation
    DisposableEffect(isAuthenticated, sessionState.isGuest, sessionState.userId, sessionState.accessToken, notificationRealtimeEpoch) {
        if (!isAuthenticated || sessionState.isGuest || sessionState.userId.isBlank() || sessionState.accessToken.isBlank()) {
            notificationSyncReducer.reset()
            unreadNotificationCount = 0
            unseenDiscussionAnime = emptyList()
            onDispose { }
        } else {
            val capturedUid = sessionState.userId
            var disposed = false
            val ws = DesktopDiscussionService.subscribeToUserNotificationsRealtime(
                session = sessionState,
                onEvent = { ev ->
                    scope.launch {
                        if (disposed || sessionState.userId != capturedUid) return@launch
                        when (ev) {
                            is DesktopNotificationRealtimeEvent.Insert -> {
                                val out = notificationSyncReducer.onRealtimeInsert(
                                    id = ev.id,
                                    recipientId = ev.recipientUserId,
                                    currentUid = capturedUid,
                                    isRead = ev.isRead
                                )
                                if (out.delta != 0) {
                                    unreadNotificationCount = out.unreadCount
                                    reconcileNotificationsFromCloud(sessionState)
                                }
                            }
                            is DesktopNotificationRealtimeEvent.Update -> {
                                val out = notificationSyncReducer.onRealtimeUpdate(
                                    id = ev.id,
                                    recipientId = ev.recipientUserId,
                                    currentUid = capturedUid,
                                    oldIsRead = ev.oldIsRead,
                                    newIsRead = ev.newIsRead,
                                    readAt = ev.readAt
                                )
                                unreadNotificationCount = out.unreadCount
                                if (out.unreadCount == 0) {
                                    unseenDiscussionAnime = emptyList()
                                }
                                if (out.needsReconcile) {
                                    reconcileNotificationsFromCloud(sessionState)
                                }
                            }
                        }
                    }
                },
                onConnected = {
                    if (!disposed) {
                        scope.launch { reconcileNotificationsFromCloud(sessionState) }
                    }
                },
                onDisconnected = {
                    if (!disposed) {
                        scope.launch {
                            delay(5_000L)
                            if (!disposed && isAuthenticated && !sessionState.isGuest && sessionState.userId == capturedUid) {
                                notificationRealtimeEpoch += 1
                            }
                        }
                    }
                }
            )
            onDispose {
                disposed = true
                runCatching { ws?.close(1000, "closed") }
            }
        }
    }

    // Independent Home active tracking list
    val homeWatchlist = remember(watchlist) {
        watchlist.filter { item ->
            item.inHome &&
                item.watchStatus != "dropped" &&
                item.watchStatus != "completed" &&
                !DesktopTrackingRules.shouldAutoComplete(item.status, item.totalEpisodes, item.watchedEpisodes)
        }
    }

    fun openDetailFrom(source: DesktopScreen, item: DesktopAnimeItem) {
        previousScreen = source
        val latest = watchlist.firstOrNull { it.id == item.id } ?: item
        selectedAnime = latest
        currentScreen = DesktopScreen.DETAIL
    }

    fun openInLibrary(item: DesktopAnimeItem) {
        libStatusFilter = "all"
        libRatingFilter = "all"
        libSearchQuery = item.title
        currentScreen = DesktopScreen.LIBRARY
    }

    fun openInCommunity(item: DesktopAnimeItem) {
        commRatingFilter = "all"
        currentScreen = DesktopScreen.COMMUNITY
    }

    fun persistLocalWatchlist(nextList: List<DesktopAnimeItem>, updatedSession: DesktopPersistedState = sessionState) {
        watchlist = nextList
        val nextSession = updatedSession.copy(watchlist = nextList)
        sessionState = nextSession
        DesktopStorage.saveActive(nextSession)
    }

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

        val nowMs = System.currentTimeMillis()
        if (capturedAuth && !sessionState.isGuest && capturedUserId.isNotBlank()) {
            var nextState = sessionState.copy(
                accessToken = snap.refreshedSession.accessToken.ifBlank { sessionState.accessToken },
                refreshToken = snap.refreshedSession.refreshToken.ifBlank { sessionState.refreshToken },
                expiresAt = maxOf(snap.refreshedSession.expiresAt, sessionState.expiresAt)
            )
            if (nowMs >= profileDirtyUntilMs) {
                if (!snap.username.isNullOrBlank()) {
                    if (isDesktopPlaceholderOrEmailName(snap.username)) {
                        nextState = nextState.copy(username = "", onboardingCompleted = false)
                    } else {
                        nextState = nextState.copy(username = snap.username, onboardingCompleted = true)
                    }
                }
                if (!snap.avatarKey.isNullOrBlank()) {
                    nextState = nextState.copy(avatarKey = snap.avatarKey)
                }
            }
            if (snap.watchlist != null) {
                val localById = watchlist.associateBy { it.id }
                val mergedCloud = snap.watchlist
                    .filter { item ->
                        val delUntil = deletedAnimeUntilMs[item.id] ?: 0L
                        nowMs >= delUntil
                    }
                    .map { cloudItem ->
                        val dirtyUntil = dirtyAnimeUntilMs[cloudItem.id] ?: 0L
                        val localItem = localById[cloudItem.id]
                        if (nowMs < dirtyUntil && localItem != null) {
                            localItem
                        } else {
                            val homeRemUntil = removedHomeUntilMs[cloudItem.id] ?: 0L
                            val finalInHome = if (nowMs < homeRemUntil) false else cloudItem.inHome
                            cloudItem.copy(inHome = finalInHome)
                        }
                    }
                val cloudIds = mergedCloud.map { it.id }.toSet()
                val localPendingAdditions = watchlist.filter { item ->
                    !cloudIds.contains(item.id) &&
                        nowMs < (dirtyAnimeUntilMs[item.id] ?: 0L) &&
                        nowMs >= (deletedAnimeUntilMs[item.id] ?: 0L)
                }
                val finalList = localPendingAdditions + mergedCloud
                watchlist = finalList
                nextState = nextState.copy(watchlist = finalList)
                selectedAnime?.let { sel ->
                    finalList.firstOrNull { it.id == sel.id }?.let { updatedSel ->
                        if (nowMs >= (dirtyAnimeUntilMs[sel.id] ?: 0L)) {
                            selectedAnime = updatedSel
                        }
                    }
                }
            }
            if (snap.collections != null) {
                collections = snap.collections
                nextState = nextState.copy(collections = snap.collections)
            }
            sessionState = nextState
            DesktopStorage.saveActive(nextState)
            reconcileNotificationsFromCloud(nextState)
        }
        if (snap.communityReviews.isNotEmpty()) {
            communityReviews = snap.communityReviews
        }
    }

    fun handleAddToHome(item: DesktopAnimeItem) {
        val existing = watchlist.firstOrNull { it.id == item.id } ?: item
        val nextStatus = if (existing.watchStatus == "plan_to_watch" || existing.watchStatus == "dropped") "watching" else existing.watchStatus
        val autoDone = DesktopTrackingRules.shouldAutoComplete(existing.status, existing.totalEpisodes, existing.watchedEpisodes)
        val updated = existing.copy(
            inWatchlist = true,
            inHome = !autoDone,
            watchStatus = if (autoDone) "completed" else nextStatus,
            updatedAt = Instant.now().toString()
        )
        dirtyAnimeUntilMs[updated.id] = System.currentTimeMillis() + 20_000L
        deletedAnimeUntilMs.remove(updated.id)
        removedHomeUntilMs.remove(updated.id)
        val nextList = listOf(updated) + watchlist.filter { it.id != updated.id }
        persistLocalWatchlist(nextList)
        if (selectedAnime?.id == updated.id) selectedAnime = updated
        showToast(if (autoDone) "${updated.title} is already completed in Library" else "Added ${updated.title} to Home & Library")
        if (isAuthenticated && !sessionState.isGuest) {
            scope.launch {
                val res = if (autoDone) {
                    saveAnimeToSupabase(sessionState, updated, syncHomeMembership = true)
                } else {
                    addAnimeToHomeInSupabase(sessionState, updated)
                }
                res.onSuccess { active ->
                    sessionState = sessionState.copy(
                        accessToken = active.accessToken,
                        refreshToken = active.refreshToken,
                        expiresAt = active.expiresAt
                    )
                    DesktopStorage.saveActive(sessionState)
                }.onFailure { err ->
                    showToast(err.message ?: "Failed to sync addition to cloud", isError = true)
                }
            }
        }
    }

    fun handleConfirmRemoveFromHome(item: DesktopAnimeItem) {
        pendingRemoveFromHome = null
        val existing = watchlist.firstOrNull { it.id == item.id } ?: item
        val updated = existing.copy(
            inWatchlist = true,
            inHome = false,
            updatedAt = Instant.now().toString()
        )
        removedHomeUntilMs[updated.id] = System.currentTimeMillis() + 25_000L
        dirtyAnimeUntilMs[updated.id] = System.currentTimeMillis() + 20_000L
        val nextList = watchlist.map { if (it.id == updated.id) updated else it }
        persistLocalWatchlist(nextList)
        if (selectedAnime?.id == updated.id) selectedAnime = updated
        showToast("Removed ${updated.title} from Home — kept in Library & Community")
        if (isAuthenticated && !sessionState.isGuest) {
            scope.launch {
                removeAnimeFromHomeInSupabase(sessionState, updated.id)
                    .onSuccess { active ->
                        sessionState = sessionState.copy(
                            accessToken = active.accessToken,
                            refreshToken = active.refreshToken,
                            expiresAt = active.expiresAt
                        )
                        DesktopStorage.saveActive(sessionState)
                    }
                    .onFailure { err ->
                        showToast(err.message ?: "Could not remove from Home on server", isError = true)
                    }
            }
        }
    }

    suspend fun handleCommitEpisodeProgress(item: DesktopAnimeItem, targetEpisodes: Int): Result<Unit> {
        val current = watchlist.firstOrNull { it.id == item.id } ?: item
        val autoDone = DesktopTrackingRules.shouldAutoComplete(current.status, current.totalEpisodes, targetEpisodes)
        val nextStatus = if (autoDone && current.watchStatus != "dropped") "completed" else current.watchStatus
        val nextInHome = if (autoDone) false else current.inHome
        val updated = current.copy(
            inWatchlist = true,
            watchedEpisodes = targetEpisodes,
            watchStatus = nextStatus,
            inHome = nextInHome,
            updatedAt = Instant.now().toString()
        )
        val prevList = watchlist
        dirtyAnimeUntilMs[updated.id] = System.currentTimeMillis() + 20_000L
        if (autoDone) {
            removedHomeUntilMs[updated.id] = System.currentTimeMillis() + 25_000L
        }
        val nextList = if (watchlist.any { it.id == updated.id }) {
            watchlist.map { if (it.id == updated.id) updated else it }
        } else {
            listOf(updated) + watchlist
        }
        persistLocalWatchlist(nextList)
        if (selectedAnime?.id == updated.id) selectedAnime = updated

        if (!isAuthenticated || sessionState.isGuest) {
            if (autoDone) showToast("${updated.title} completed — moved to Library")
            return Result.success(Unit)
        }
        val res = updateEpisodeProgressInSupabase(sessionState, updated, targetEpisodes)
        return res.fold(
            onSuccess = { active ->
                sessionState = sessionState.copy(
                    accessToken = active.accessToken,
                    refreshToken = active.refreshToken,
                    expiresAt = active.expiresAt
                )
                DesktopStorage.saveActive(sessionState)
                if (autoDone) {
                    showToast("${updated.title} completed — archived in Library")
                }
                Result.success(Unit)
            },
            onFailure = { err ->
                // Restore previous value on failure
                dirtyAnimeUntilMs.remove(updated.id)
                persistLocalWatchlist(prevList)
                if (selectedAnime?.id == updated.id) {
                    selectedAnime = prevList.firstOrNull { it.id == updated.id } ?: current
                }
                showToast(err.message ?: "Failed to save episode progress", isError = true)
                Result.failure(err)
            }
        )
    }

    fun handleSaveAnimeFromDetail(editedItem: DesktopAnimeItem, returnToPrevious: Boolean = true) {
        val autoDone = DesktopTrackingRules.shouldAutoComplete(editedItem.status, editedItem.totalEpisodes, editedItem.watchedEpisodes)
        val finalStatus = if (autoDone && editedItem.watchStatus != "dropped") "completed" else editedItem.watchStatus
        val dropFromHome = DesktopTrackingRules.shouldRemoveFromHome(finalStatus, editedItem.status, editedItem.totalEpisodes, editedItem.watchedEpisodes)
        val finalInHome = if (dropFromHome) false else editedItem.inHome
        val finalItem = editedItem.copy(
            inWatchlist = true,
            watchStatus = finalStatus,
            inHome = finalInHome,
            updatedAt = Instant.now().toString()
        )
        dirtyAnimeUntilMs[finalItem.id] = System.currentTimeMillis() + 20_000L
        deletedAnimeUntilMs.remove(finalItem.id)
        if (!finalInHome) {
            removedHomeUntilMs[finalItem.id] = System.currentTimeMillis() + 25_000L
        } else {
            removedHomeUntilMs.remove(finalItem.id)
        }
        selectedAnime = finalItem
        val nextList = listOf(finalItem) + watchlist.filter { it.id != finalItem.id }
        persistLocalWatchlist(nextList)
        val msg = when {
            finalStatus == "dropped" -> "${finalItem.title} marked Dropped — removed from Home, kept in Library"
            autoDone -> "${finalItem.title} completed — moved to Library"
            else -> "Saved ${finalItem.title}"
        }
        showToast(msg)
        if (isAuthenticated && !sessionState.isGuest) {
            scope.launch {
                saveAnimeToSupabase(sessionState, finalItem, syncHomeMembership = true)
                    .onSuccess { active ->
                        sessionState = sessionState.copy(
                            accessToken = active.accessToken,
                            refreshToken = active.refreshToken,
                            expiresAt = active.expiresAt
                        )
                        DesktopStorage.saveActive(sessionState)
                    }
                    .onFailure { err ->
                        showToast(err.message ?: "Cloud sync failed for ${finalItem.title}", isError = true)
                    }
            }
        }
        if (returnToPrevious) {
            currentScreen = if (previousScreen == DesktopScreen.DETAIL) DesktopScreen.LIBRARY else previousScreen
        }
    }

    fun handleDeleteFromLibrary(toRemove: DesktopAnimeItem) {
        val prevList = watchlist
        deletedAnimeUntilMs[toRemove.id] = System.currentTimeMillis() + 30_000L
        dirtyAnimeUntilMs.remove(toRemove.id)
        removedHomeUntilMs[toRemove.id] = System.currentTimeMillis() + 30_000L
        val nextList = watchlist.filter { it.id != toRemove.id }
        persistLocalWatchlist(nextList)
        showToast("Deleted ${toRemove.title} from Library (Community posts preserved)")
        if (isAuthenticated && !sessionState.isGuest) {
            scope.launch {
                deleteAnimeFromSupabase(sessionState, toRemove.id)
                    .onSuccess { active ->
                        sessionState = sessionState.copy(
                            accessToken = active.accessToken,
                            refreshToken = active.refreshToken,
                            expiresAt = active.expiresAt
                        )
                        DesktopStorage.saveActive(sessionState)
                    }
                    .onFailure { err ->
                        deletedAnimeUntilMs.remove(toRemove.id)
                        persistLocalWatchlist(prevList)
                        showToast(err.message ?: "Could not delete ${toRemove.title}", isError = true)
                    }
            }
        }
        if (currentScreen == DesktopScreen.DETAIL) {
            currentScreen = if (previousScreen == DesktopScreen.DETAIL) DesktopScreen.LIBRARY else previousScreen
        }
    }

    // Initial cloud sync + automatic 15-second background sync & notification polling loop
    LaunchedEffect(isAuthenticated, isGuestMode, sessionState.userId, sessionState.guestId) {
        if (isAuthenticated || isGuestMode) {
            syncWithCloud()
            if (isAuthenticated && !sessionState.isGuest) {
                refreshUnseenDiscussionAnime()
            }
            while (isActive) {
                delay(15_000L)
                syncWithCloud()
                if (isAuthenticated && !sessionState.isGuest) {
                    refreshUnseenDiscussionAnime()
                }
            }
        }
    }

    fun loadMoreDiscover() {
        if (discoverLoading || discoverLoadingMore || !discoverHasNextPage) return
        val activeToken = discoverRequestToken
        val nextPage = discoverPage + 1
        discoverLoadingMore = true
        scope.launch {
            val result = fetchAniListDiscover(
                mode = discoverMode,
                genre = discoverGenre,
                search = discoverSearch,
                safeMode = safeMode,
                page = nextPage,
                perPage = 30
            )
            if (activeToken != discoverRequestToken) return@launch
            val existingIds = discoverItems.mapTo(HashSet()) { it.id }
            val uniqueNew = result.items.filter { it.id !in existingIds }
            if (uniqueNew.isNotEmpty()) {
                discoverLastPageStartIndex = discoverItems.size
                discoverItems = discoverItems + uniqueNew
            }
            discoverPage = nextPage
            discoverHasNextPage = result.hasNextPage
            discoverLoadingMore = false
        }
    }

    // Discover catalog loader (debounces search typing and resets pagination cleanly)
    LaunchedEffect(discoverMode, discoverGenre, safeMode, discoverSearch) {
        if (discoverSearch.isNotBlank()) {
            delay(260L)
        }
        val token = ++discoverRequestToken
        discoverLoading = true
        discoverLoadingMore = false
        discoverPage = 1
        discoverHasNextPage = true
        discoverLastPageStartIndex = 0
        val result = fetchAniListDiscover(
            mode = discoverMode,
            genre = discoverGenre,
            search = discoverSearch,
            safeMode = safeMode,
            page = 1,
            perPage = 30
        )
        if (token != discoverRequestToken) return@LaunchedEffect
        discoverItems = result.items
        discoverHasNextPage = result.hasNextPage
        discoverLoading = false
    }

    val needsDesktopOnboarding = isAuthenticated &&
        (!sessionState.onboardingCompleted || isDesktopPlaceholderOrEmailName(sessionState.username))

    if ((!isAuthenticated && !isGuestMode) || needsDesktopOnboarding) {
        DesktopAuthScreen(
            pendingOnboardingSession = if (needsDesktopOnboarding) sessionState else null,
            onAuthenticated = { newSession ->
                val prevGuestId = DesktopStorage.getOrCreateGuestId()
                val guestItemsToMigrate = DesktopStorage.loadGuest(prevGuestId).watchlist
                val existingUserState = DesktopStorage.loadUser(newSession.userId)
                val resolvedName = newSession.username.takeIf { !isDesktopPlaceholderOrEmailName(it) }
                    ?: existingUserState.username.takeIf { !isDesktopPlaceholderOrEmailName(it) }
                    ?: ""
                val mergedSession = newSession.copy(
                    isGuest = false,
                    guestId = "",
                    username = resolvedName,
                    avatarKey = newSession.avatarKey.ifBlank { existingUserState.avatarKey.ifBlank { DesktopAvatar.AVATAR_1.key } },
                    onboardingCompleted = newSession.onboardingCompleted && !isDesktopPlaceholderOrEmailName(resolvedName),
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
                    val res = createCollectionInSupabase(sessionState, title, desc)
                    res.onSuccess { (createdCol, active) ->
                        collections = listOf(createdCol) + collections
                        sessionState = sessionState.copy(
                            accessToken = active.accessToken,
                            refreshToken = active.refreshToken,
                            expiresAt = active.expiresAt,
                            collections = collections
                        )
                        DesktopStorage.saveActive(sessionState)
                        showToast("Collection \"${createdCol.title}\" created")
                    }.onFailure { err ->
                        showToast(err.message ?: "Failed to create collection", isError = true)
                    }
                }
            }
        )
    }

    pendingRemoveFromHome?.let { target ->
        DesktopRemoveFromHomeDialog(
            animeTitle = target.title,
            onDismiss = { pendingRemoveFromHome = null },
            onConfirm = { handleConfirmRemoveFromHome(target) }
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(PureObsidian)
    ) {
        val isWide = maxWidth >= 820.dp
        val mainTabs = listOf(DesktopScreen.HOME, DesktopScreen.EXPLORE, DesktopScreen.LIBRARY, DesktopScreen.COMMUNITY)
        val sidebarTabs = mainTabs + listOf(DesktopScreen.PROFILE)

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

                            // Navigation Links (Home, Explore, Library, Community with Red Dot, Profile)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                sidebarTabs.forEach { tab ->
                                    val selected = currentScreen == tab
                                    val showCommunityRedDot = tab == DesktopScreen.COMMUNITY &&
                                        (unreadNotificationCount > 0 || unseenDiscussionAnime.isNotEmpty())
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) Color(0xFF2B271F) else Color.Transparent,
                                        border = if (selected) BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                currentScreen = tab
                                                if (tab == DesktopScreen.COMMUNITY) {
                                                    refreshUnseenDiscussionAnime()
                                                }
                                            }
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
                                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (showCommunityRedDot) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEF4444))
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            SandroneSidebarMascot(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp)
                            )
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
                                watchlist = homeWatchlist,
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
                                onOpenDetail = { item -> openDetailFrom(DesktopScreen.HOME, item) },
                                onRequestRemoveFromHome = { item -> pendingRemoveFromHome = item },
                                onAddToHome = { item -> handleAddToHome(item) },
                                onViewInLibrary = { item -> openInLibrary(item) },
                                onViewCommunity = { item -> openInCommunity(item) },
                                onCommitEpisodes = { item, eps -> handleCommitEpisodeProgress(item, eps) }
                            )
                        }

                        DesktopScreen.EXPLORE -> {
                            DesktopDiscoverScreen(
                                discoverItems = discoverItems,
                                watchlist = watchlist,
                                loading = discoverLoading,
                                loadingMore = discoverLoadingMore,
                                hasNextPage = discoverHasNextPage,
                                lastPageStartIndex = discoverLastPageStartIndex,
                                avatarKey = sessionState.avatarKey,
                                discoverMode = discoverMode,
                                discoverGenre = discoverGenre,
                                searchQuery = discoverSearch,
                                safeMode = safeMode,
                                isListView = discoverListView,
                                showFilters = discoverShowFilters,
                                onToggleListView = { discoverListView = !discoverListView },
                                onToggleFilters = { discoverShowFilters = !discoverShowFilters },
                                onSearchChange = { q -> discoverSearch = q },
                                onLoadMore = { loadMoreDiscover() },
                                onSelectMode = { m -> discoverMode = m },
                                onSelectGenre = { g -> discoverGenre = g },
                                onToggleSafe = { safeMode = !safeMode },
                                onOpenProfile = { currentScreen = DesktopScreen.PROFILE },
                                onOpenDetail = { item -> openDetailFrom(DesktopScreen.EXPLORE, item) },
                                onAddToHome = { item -> handleAddToHome(item) },
                                onRequestRemoveFromHome = { item -> pendingRemoveFromHome = item },
                                onViewInLibrary = { item -> openInLibrary(item) },
                                onViewCommunity = { item -> openInCommunity(item) }
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
                                isListView = libListView,
                                onToggleListView = { libListView = !libListView },
                                onStatusChange = { libStatusFilter = it },
                                onRatingChange = { libRatingFilter = it },
                                onSearchChange = { libSearchQuery = it },
                                onSortChange = { libSortMode = it },
                                onCreateCollectionClick = { showCreateCollectionDialog = true },
                                onOpenProfile = { currentScreen = DesktopScreen.PROFILE },
                                onOpenDetail = { item -> openDetailFrom(DesktopScreen.LIBRARY, item) },
                                onAddToHome = { item -> handleAddToHome(item) },
                                onRequestRemoveFromHome = { item -> pendingRemoveFromHome = item },
                                onViewCommunity = { item -> openInCommunity(item) },
                                onCommitEpisodes = { item, eps -> handleCommitEpisodeProgress(item, eps) }
                            )
                        }

                        DesktopScreen.COMMUNITY -> {
                            val combinedReviews = remember(communityReviews, watchlist, sessionState) {
                                val localUserReviews = watchlist
                                    .filter { it.userStars > 0f || it.notes.isNotBlank() }
                                    .map { item ->
                                        DesktopCommunityReview(
                                            id = "${sessionState.userId.ifBlank { "local" }}-${item.id}",
                                            userId = sessionState.userId,
                                            animeId = item.id,
                                            animeTitle = item.title,
                                            animeCover = item.coverImage,
                                            stars = item.userStars,
                                            notes = item.notes,
                                            username = sessionState.username,
                                            avatarKey = sessionState.avatarKey,
                                            updatedAt = item.updatedAt.ifBlank { Instant.now().toString() },
                                            formattedDate = formatIsoDate(item.updatedAt),
                                            genres = item.genres
                                        )
                                    }
                                if (communityReviews.isNotEmpty()) {
                                    val cloudWithoutLocalDuplicates = communityReviews.filterNot { c ->
                                        localUserReviews.any { l ->
                                            l.animeId == c.animeId &&
                                                (l.userId.isNotBlank() && l.userId == c.userId || l.username.equals(c.username, ignoreCase = true))
                                        }
                                    }
                                    localUserReviews + cloudWithoutLocalDuplicates
                                } else {
                                    localUserReviews
                                }
                            }
                            DesktopCommunityScreen(
                                reviews = combinedReviews,
                                unseenDiscussionAnime = unseenDiscussionAnime,
                                unreadNotificationCount = unreadNotificationCount,
                                session = sessionState,
                                isAuthenticated = isAuthenticated,
                                currentUserId = sessionState.userId,
                                currentUsername = sessionState.username,
                                avatarKey = sessionState.avatarKey,
                                ratingFilter = commRatingFilter,
                                sortMode = commSortMode,
                                onRatingChange = { commRatingFilter = it },
                                onSortChange = { commSortMode = it },
                                onOpenProfile = { currentScreen = DesktopScreen.PROFILE },
                                onSessionUpdated = { active ->
                                    sessionState = sessionState.copy(
                                        accessToken = active.accessToken,
                                        refreshToken = active.refreshToken,
                                        expiresAt = active.expiresAt
                                    )
                                    DesktopStorage.saveActive(sessionState)
                                },
                                onShowToast = { msg, isErr -> showToast(msg, isError = isErr) },
                                onNotificationMarkedRead = { notifId ->
                                    val readAtIso = Instant.now().toString()
                                    val nextCount = notificationSyncReducer.markLocalRead(notifId)
                                    val mutGen = notificationSyncReducer.currentGeneration()
                                    unreadNotificationCount = nextCount
                                    if (nextCount == 0) unseenDiscussionAnime = emptyList()
                                    scope.launch {
                                        DesktopDiscussionService.markNotificationRead(sessionState, notifId, readAtIso)
                                            .onSuccess { exact ->
                                                if (notificationSyncReducer.confirmServerUnreadCount(exact, mutGen)) {
                                                    unreadNotificationCount = notificationSyncReducer.unreadCount
                                                    if (notificationSyncReducer.unreadCount == 0) unseenDiscussionAnime = emptyList()
                                                }
                                                reconcileNotificationsFromCloud(sessionState)
                                            }
                                            .onFailure {
                                                reconcileNotificationsFromCloud(sessionState)
                                            }
                                    }
                                },
                                onAllNotificationsMarkedRead = {
                                    val readAtIso = Instant.now().toString()
                                    notificationSyncReducer.markAllLocalRead()
                                    val mutGen = notificationSyncReducer.currentGeneration()
                                    unreadNotificationCount = 0
                                    unseenDiscussionAnime = emptyList()
                                    scope.launch {
                                        DesktopDiscussionService.markAllNotificationsRead(sessionState, readAtIso)
                                            .onSuccess { exact ->
                                                if (notificationSyncReducer.confirmServerUnreadCount(exact, mutGen)) {
                                                    unreadNotificationCount = notificationSyncReducer.unreadCount
                                                    if (notificationSyncReducer.unreadCount == 0) unseenDiscussionAnime = emptyList()
                                                }
                                            }
                                            .onFailure {
                                                reconcileNotificationsFromCloud(sessionState)
                                            }
                                    }
                                },
                                onMarkedAnimeSeen = { seenAnimeId ->
                                    val readAtIso = Instant.now().toString()
                                    val mutGen = notificationSyncReducer.beginLocalMutation()
                                    val remaining = unseenDiscussionAnime.filterNot { it.animeId == seenAnimeId }
                                    unseenDiscussionAnime = remaining
                                    unreadNotificationCount = remaining.sumOf { it.unseenCount.coerceAtLeast(1) }
                                    scope.launch {
                                        DesktopDiscussionService.markAnimeNotificationsRead(sessionState, seenAnimeId, readAtIso)
                                            .onSuccess { exact ->
                                                if (notificationSyncReducer.confirmServerUnreadCount(exact, mutGen)) {
                                                    unreadNotificationCount = notificationSyncReducer.unreadCount
                                                    if (notificationSyncReducer.unreadCount == 0) unseenDiscussionAnime = emptyList()
                                                }
                                                reconcileNotificationsFromCloud(sessionState)
                                            }
                                            .onFailure {
                                                reconcileNotificationsFromCloud(sessionState)
                                            }
                                    }
                                },
                                onOpenAnime = { animeId, fallbackTitle, fallbackCover ->
                                    val unseenEntry = unseenDiscussionAnime.firstOrNull { it.animeId == animeId }
                                    if (unseenEntry != null) {
                                        pendingNotificationJump = DesktopNotificationJumpTarget(
                                            animeId = animeId,
                                            commentId = unseenEntry.latestCommentId,
                                            threadId = unseenEntry.latestThreadId,
                                            replyId = unseenEntry.latestReplyId
                                        )
                                    }
                                    val found = watchlist.firstOrNull { it.id == animeId }
                                        ?: discoverItems.firstOrNull { it.id == animeId }
                                    if (found != null) {
                                        openDetailFrom(DesktopScreen.COMMUNITY, found)
                                    } else {
                                        val stub = DesktopAnimeItem(
                                            id = animeId,
                                            title = fallbackTitle.ifBlank { "Anime #$animeId" },
                                            coverImage = fallbackCover
                                        )
                                        openDetailFrom(DesktopScreen.COMMUNITY, stub)
                                        scope.launch {
                                            DesktopSeasonResolver.fetchSingleAnimeItem(animeId)?.let { fetched ->
                                                if (selectedAnime?.id == animeId) {
                                                    selectedAnime = fetched
                                                }
                                            }
                                        }
                                    }
                                },
                                onSubmitCommentNote = { animeId, animeTitle, animeCover, stars, note ->
                                    // Independent Community post: NEVER adds anime to Home!
                                    val nowIso = Instant.now().toString()
                                    val cleanNote = note.trim()
                                    val matchedGenres = watchlist.firstOrNull { it.id == animeId }?.genres
                                        ?: discoverItems.firstOrNull { it.id == animeId }?.genres
                                        ?: communityReviews.firstOrNull { it.animeId == animeId }?.genres
                                        ?: emptyList()
                                    val newRev = DesktopCommunityReview(
                                        id = "${sessionState.userId.ifBlank { "local" }}-$animeId",
                                        userId = sessionState.userId,
                                        animeId = animeId,
                                        animeTitle = animeTitle,
                                        animeCover = animeCover,
                                        stars = stars,
                                        notes = cleanNote,
                                        username = sessionState.username,
                                        avatarKey = sessionState.avatarKey,
                                        updatedAt = nowIso,
                                        formattedDate = formatIsoDate(nowIso),
                                        genres = matchedGenres
                                    )
                                    communityReviews = listOf(newRev) + communityReviews.filterNot {
                                        it.animeId == animeId && (
                                            (sessionState.userId.isNotBlank() && it.userId == sessionState.userId) ||
                                                it.username.equals(sessionState.username, ignoreCase = true)
                                        )
                                    }
                                    // Update local Library rating/note ONLY if already in Library (NEVER add to Home or Library!)
                                    if (watchlist.any { it.id == animeId }) {
                                        val nextList = watchlist.map {
                                            if (it.id == animeId) it.copy(
                                                userStars = if (stars > 0f) stars else it.userStars,
                                                notes = cleanNote.ifBlank { it.notes },
                                                updatedAt = nowIso
                                            ) else it
                                        }
                                        persistLocalWatchlist(nextList)
                                    }
                                    showToast("Posted note to Community")
                                    if (isAuthenticated && !sessionState.isGuest) {
                                        scope.launch {
                                            submitCommunityNoteInSupabase(sessionState, animeId, animeTitle, animeCover, stars, cleanNote)
                                                .onSuccess { active ->
                                                    sessionState = sessionState.copy(
                                                        accessToken = active.accessToken,
                                                        refreshToken = active.refreshToken,
                                                        expiresAt = active.expiresAt
                                                    )
                                                    DesktopStorage.saveActive(sessionState)
                                                }
                                                .onFailure { err ->
                                                    showToast(err.message ?: "Failed to post Community note", isError = true)
                                                }
                                        }
                                    }
                                }
                            )
                        }

                        DesktopScreen.PROFILE, DesktopScreen.SETTINGS -> {
                            DesktopProfileScreen(
                                session = sessionState,
                                watchlist = watchlist,
                                collectionsCount = collections.size,
                                onUpdateProfile = { newName, newAv ->
                                    val cleanName = newName.trim()
                                    if (cleanName.length < 2 || cleanName.length > 32) {
                                        Result.failure(IllegalArgumentException("Username must be 2–32 characters."))
                                    } else {
                                        profileDirtyUntilMs = System.currentTimeMillis() + 25_000L
                                        val prev = sessionState
                                        val optimistic = sessionState.copy(username = cleanName, avatarKey = newAv)
                                        sessionState = optimistic
                                        DesktopStorage.saveActive(optimistic)
                                        if (isAuthenticated && !optimistic.isGuest) {
                                            val res = updateProfileInSupabase(optimistic, cleanName, newAv)
                                            res.fold(
                                                onSuccess = { saved ->
                                                    sessionState = saved.copy(watchlist = watchlist, collections = collections)
                                                    DesktopStorage.saveActive(sessionState)
                                                    showToast("Profile updated!")
                                                    Result.success(Unit)
                                                },
                                                onFailure = { err ->
                                                    profileDirtyUntilMs = 0L
                                                    sessionState = prev
                                                    DesktopStorage.saveActive(prev)
                                                    showToast(err.message ?: "Could not save profile", isError = true)
                                                    Result.failure(err)
                                                }
                                            )
                                        } else {
                                            showToast("Profile updated!")
                                            Result.success(Unit)
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
                                    unseenDiscussionAnime = emptyList()
                                    unreadNotificationCount = 0
                                    selectedAnime = null
                                    isAuthenticated = false
                                    isGuestMode = false
                                }
                            )
                        }

                        DesktopScreen.DETAIL -> {
                            val item = selectedAnime
                            if (item != null) {
                                val liveItem = watchlist.firstOrNull { it.id == item.id } ?: item
                                DesktopDetailScreen(
                                    item = liveItem,
                                    session = sessionState,
                                    isAuthenticated = isAuthenticated,
                                    notificationJumpTarget = pendingNotificationJump,
                                    onNotificationJumpConsumed = { pendingNotificationJump = null },
                                    onMarkedAnimeSeen = { seenAnimeId ->
                                        val readAtIso = Instant.now().toString()
                                        val mutGen = notificationSyncReducer.beginLocalMutation()
                                        val remaining = unseenDiscussionAnime.filterNot { it.animeId == seenAnimeId }
                                        unseenDiscussionAnime = remaining
                                        unreadNotificationCount = remaining.sumOf { it.unseenCount.coerceAtLeast(1) }
                                        scope.launch {
                                            DesktopDiscussionService.markAnimeNotificationsRead(sessionState, seenAnimeId, readAtIso)
                                                .onSuccess { exact ->
                                                    if (notificationSyncReducer.confirmServerUnreadCount(exact, mutGen)) {
                                                        unreadNotificationCount = notificationSyncReducer.unreadCount
                                                        if (notificationSyncReducer.unreadCount == 0) unseenDiscussionAnime = emptyList()
                                                    }
                                                    reconcileNotificationsFromCloud(sessionState)
                                                }
                                                .onFailure {
                                                    reconcileNotificationsFromCloud(sessionState)
                                                }
                                        }
                                    },
                                    onSessionUpdated = { active ->
                                        sessionState = sessionState.copy(
                                            accessToken = active.accessToken,
                                            refreshToken = active.refreshToken,
                                            expiresAt = active.expiresAt
                                        )
                                        DesktopStorage.saveActive(sessionState)
                                    },
                                    onShowToast = { msg, err -> showToast(msg, err) },
                                    onBack = {
                                        currentScreen = if (previousScreen == DesktopScreen.DETAIL) DesktopScreen.HOME else previousScreen
                                    },
                                    onSelectSeason = { seasonId ->
                                        val existingSeason = watchlist.firstOrNull { it.id == seasonId }
                                        if (existingSeason != null) {
                                            selectedAnime = existingSeason
                                        } else {
                                            scope.launch {
                                                val fetched = DesktopSeasonResolver.fetchSingleAnimeItem(seasonId)
                                                if (fetched != null) {
                                                    selectedAnime = fetched
                                                } else {
                                                    showToast("Unable to load selected season right now.", isError = true)
                                                }
                                            }
                                        }
                                    },
                                    onSave = { updatedItem -> handleSaveAnimeFromDetail(updatedItem, returnToPrevious = false) },
                                    onCommitEpisodes = { eps -> handleCommitEpisodeProgress(liveItem, eps) },
                                    onAddToHome = { handleAddToHome(liveItem) },
                                    onRequestRemoveFromHome = { pendingRemoveFromHome = liveItem },
                                    onDelete = { toRemove -> handleDeleteFromLibrary(toRemove) }
                                )
                            }
                        }
                    }
                }

                // Floating Toast Banner (top-right)
                toastMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (toastIsError) Color(0xFF361920) else Color(0xFF2B271F),
                        border = BorderStroke(
                            1.dp,
                            if (toastIsError) CrimsonWaste.copy(alpha = 0.75f) else ChampagneGold.copy(alpha = 0.75f)
                        ),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 18.dp, end = 24.dp)
                            .widthIn(max = 420.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (toastIsError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (toastIsError) CrimsonWaste else ChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = msg,
                                color = IvoryText,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = { toastMessage = null },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = WarmSilver, modifier = Modifier.size(14.dp))
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
    pendingOnboardingSession: DesktopPersistedState? = null,
    onAuthenticated: (DesktopPersistedState) -> Unit,
    onContinueGuest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSignUp by remember { mutableStateOf(false) }
    var awaitingOtp by remember { mutableStateOf(false) }
    // 0 = Sign in / Sign up, 1 = Choose Username, 2 = Choose Profile Picture
    var onboardingStep by remember(pendingOnboardingSession?.userId) {
        mutableStateOf(if (pendingOnboardingSession != null) 1 else 0)
    }
    var onboardingSession by remember(pendingOnboardingSession?.userId) {
        mutableStateOf(pendingOnboardingSession)
    }
    var onboardUsername by remember(pendingOnboardingSession?.userId) { mutableStateOf("") }
    var onboardAvatarKey by remember(pendingOnboardingSession?.userId) {
        mutableStateOf(pendingOnboardingSession?.avatarKey?.ifBlank { DesktopAvatar.AVATAR_1.key } ?: DesktopAvatar.AVATAR_1.key)
    }
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
                    if (onboardingStep == 1 && onboardingSession != null) {
                        Column(
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 26.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "STEP 1 OF 2 • USERNAME",
                                color = ChampagneGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp
                            )
                            Text(
                                text = "Choose Your Username",
                                fontFamily = FontFamily.Serif,
                                fontSize = 26.sp,
                                color = IvoryText
                            )
                            Text(
                                text = "Pick a unique display name so your email stays private in Community.",
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
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(19.dp))
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (onboardUsername.isEmpty()) {
                                            Text("e.g. GojoFan", color = AshMuted, fontSize = 15.sp)
                                        }
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = onboardUsername,
                                            onValueChange = { raw ->
                                                onboardUsername = raw.take(32)
                                                errorMessage = null
                                            },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = IvoryText,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            if (!errorMessage.isNullOrBlank()) {
                                Text(errorMessage!!, color = CrimsonWaste, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                            }
                            if (!infoMessage.isNullOrBlank()) {
                                Text(infoMessage!!, color = ChampagneGold, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFF3E2BA), Color(0xFFDFB870))))
                                    .clickable(enabled = !isLoading) {
                                        val activeSess = onboardingSession ?: return@clickable
                                        isLoading = true
                                        errorMessage = null
                                        scope.launch {
                                            val checkRes = checkDesktopUsernameAvailable(activeSess, onboardUsername)
                                            isLoading = false
                                            checkRes.onSuccess {
                                                onboardUsername = onboardUsername.trim()
                                                infoMessage = null
                                                onboardingStep = 2
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Please choose a different username."
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isLoading) "CHECKING..." else "CONTINUE TO PROFILE PICTURE ›",
                                    color = ChampagneOnGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.4.sp
                                )
                            }
                        }
                    } else if (onboardingStep == 2 && onboardingSession != null) {
                        Column(
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 26.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "STEP 2 OF 2 • PROFILE PICTURE",
                                color = ChampagneGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp
                            )
                            Text(
                                text = "Choose Your Profile Picture",
                                fontFamily = FontFamily.Serif,
                                fontSize = 25.sp,
                                color = IvoryText,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Select a portrait for $onboardUsername",
                                color = WarmSilver,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                DesktopAvatar.entries.chunked(3).forEach { rowAvatars ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        rowAvatars.forEach { av ->
                                            val selected = onboardAvatarKey == av.key
                                            val bmp = remember(av.fileName) { loadLocalDrawableBitmap(av.fileName) }
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        onboardAvatarKey = av.key
                                                        errorMessage = null
                                                    }
                                                    .padding(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(66.dp)
                                                        .clip(CircleShape)
                                                        .background(Brush.linearGradient(listOf(av.primaryColor, av.secondaryColor)))
                                                        .border(
                                                            width = if (selected) 3.dp else 1.dp,
                                                            color = if (selected) ChampagneGold else Color.White.copy(alpha = 0.22f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (bmp != null) {
                                                        Image(
                                                            bitmap = bmp,
                                                            contentDescription = av.displayName,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                        )
                                                    }
                                                    if (selected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .size(20.dp)
                                                                .clip(CircleShape)
                                                                .background(ChampagneGold),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Filled.Check,
                                                                contentDescription = null,
                                                                tint = ChampagneOnGold,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = av.displayName.substringBefore(" "),
                                                    color = if (selected) ChampagneGold else WarmSilver,
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (!errorMessage.isNullOrBlank()) {
                                Text(errorMessage!!, color = CrimsonWaste, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFF3E2BA), Color(0xFFDFB870))))
                                    .clickable(enabled = !isLoading) {
                                        val activeSess = onboardingSession ?: return@clickable
                                        isLoading = true
                                        errorMessage = null
                                        scope.launch {
                                            val saveRes = updateProfileInSupabase(
                                                session = activeSess,
                                                username = onboardUsername.trim(),
                                                avatarKey = onboardAvatarKey
                                            )
                                            isLoading = false
                                            saveRes.onSuccess { completed ->
                                                onboardingStep = 0
                                                onboardingSession = null
                                                onAuthenticated(completed.copy(onboardingCompleted = true))
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Could not save profile."
                                                if ((err.message ?: "").contains("username", ignoreCase = true) ||
                                                    (err.message ?: "").contains("taken", ignoreCase = true)
                                                ) {
                                                    onboardingStep = 1
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isLoading) "SAVING PROFILE..." else "ENTER ANIME UNIVERSE",
                                    color = ChampagneOnGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.6.sp
                                )
                            }
                            Text(
                                text = "‹ Change Username",
                                color = WarmSilver,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable(enabled = !isLoading) {
                                    errorMessage = null
                                    onboardingStep = 1
                                }
                            )
                        }
                    } else if (awaitingOtp) {
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
                                text = "Enter the verification code sent to $pendingEmail",
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
                                            res.onSuccess { verifiedState ->
                                                awaitingOtp = false
                                                otpCode = ""
                                                onboardingSession = verifiedState.copy(onboardingCompleted = false, username = "")
                                                onboardUsername = ""
                                                onboardAvatarKey = DesktopAvatar.AVATAR_1.key
                                                infoMessage = "Email verified! Choose your username and profile picture."
                                                onboardingStep = 1
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Invalid code"
                                            }
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
                                            infoMessage = "Fresh verification code sent to $pendingEmail"
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
                                                if (!state.onboardingCompleted || isDesktopPlaceholderOrEmailName(state.username)) {
                                                    onboardingSession = state.copy(onboardingCompleted = false, username = "")
                                                    onboardUsername = ""
                                                    onboardAvatarKey = DesktopAvatar.AVATAR_1.key
                                                    infoMessage = "Choose your username and profile picture to finish setup."
                                                    onboardingStep = 1
                                                } else {
                                                    onAuthenticated(state)
                                                }
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
    onOpenDetail: (DesktopAnimeItem) -> Unit,
    onRequestRemoveFromHome: (DesktopAnimeItem) -> Unit,
    onAddToHome: (DesktopAnimeItem) -> Unit,
    onViewInLibrary: (DesktopAnimeItem) -> Unit,
    onViewCommunity: (DesktopAnimeItem) -> Unit,
    onCommitEpisodes: suspend (DesktopAnimeItem, Int) -> Result<Unit>
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
                        isOnHome = true,
                        showEpisodeStepper = true,
                        onClick = { onOpenDetail(show) },
                        onRequestRemoveFromHome = { onRequestRemoveFromHome(show) },
                        onAddToHome = { onAddToHome(show) },
                        onViewInLibrary = { onViewInLibrary(show) },
                        onViewCommunity = { onViewCommunity(show) },
                        onCommitEpisodes = { eps -> onCommitEpisodes(show, eps) }
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
    loadingMore: Boolean,
    hasNextPage: Boolean,
    lastPageStartIndex: Int,
    avatarKey: String,
    discoverMode: String,
    discoverGenre: String,
    searchQuery: String,
    safeMode: Boolean,
    isListView: Boolean,
    showFilters: Boolean,
    onToggleListView: () -> Unit,
    onToggleFilters: () -> Unit,
    onSearchChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onSelectMode: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onToggleSafe: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDetail: (DesktopAnimeItem) -> Unit,
    onAddToHome: (DesktopAnimeItem) -> Unit,
    onRequestRemoveFromHome: (DesktopAnimeItem) -> Unit,
    onViewInLibrary: (DesktopAnimeItem) -> Unit,
    onViewCommunity: (DesktopAnimeItem) -> Unit
) {
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    // Scroll back to top when filter/search resets the catalog
    LaunchedEffect(discoverMode, discoverGenre, safeMode, searchQuery) {
        gridState.scrollToItem(0)
        listState.scrollToItem(0)
    }

    // Automatically load next page once the 3rd row of the latest batch is crossed
    LaunchedEffect(gridState, isListView, discoverItems.size, lastPageStartIndex, hasNextPage, loading, loadingMore) {
        if (isListView) return@LaunchedEffect
        snapshotFlow {
            val layout = gridState.layoutInfo
            val visible = layout.visibleItemsInfo
            val lastVisible = visible.lastOrNull()?.index ?: -1
            val cols = visible.map { it.column }.distinct().size.coerceAtLeast(1)
            Pair(lastVisible, cols)
        }.collect { (lastVisible, cols) ->
            val count = discoverItems.size
            if (count > 0 && lastVisible >= 0 && hasNextPage && !loading && !loadingMore) {
                val triggerIdx = minOf(count - 1, maxOf(0, lastPageStartIndex + cols * 3 - 1))
                if (lastVisible >= triggerIdx) {
                    onLoadMore()
                }
            }
        }
    }

    LaunchedEffect(listState, isListView, discoverItems.size, lastPageStartIndex, hasNextPage, loading, loadingMore) {
        if (!isListView) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.collect { lastVisible ->
            val count = discoverItems.size
            if (count > 0 && lastVisible >= 0 && hasNextPage && !loading && !loadingMore) {
                val triggerIdx = minOf(count - 1, maxOf(0, lastPageStartIndex + 6))
                if (lastVisible >= triggerIdx) {
                    onLoadMore()
                }
            }
        }
    }
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
            CircleIconActionButton(
                icon = if (isListView) Icons.Outlined.GridView else Icons.AutoMirrored.Filled.ViewList,
                active = isListView,
                onClick = onToggleListView
            )
            CircleIconActionButton(
                icon = Icons.Outlined.Tune,
                active = showFilters,
                onClick = onToggleFilters
            )
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

    if (showFilters) {
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
            if (loading) "LOADING..." else "${discoverItems.size} TITLES",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = ChampagneGold
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (isListView) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            items(discoverItems, key = { it.id }) { item ->
                val tracked = watchlist.firstOrNull { it.id == item.id }
                val merged = tracked ?: item
                val isOnHome = tracked?.inHome == true
                DesktopAnimeListRow(
                    item = merged,
                    isOnHome = isOnHome,
                    showEpisodeStepper = false,
                    onClick = { onOpenDetail(merged) },
                    onAddToHome = { onAddToHome(merged) },
                    onRequestRemoveFromHome = { onRequestRemoveFromHome(merged) },
                    onViewInLibrary = { onViewInLibrary(merged) },
                    onViewCommunity = { onViewCommunity(merged) },
                    onCommitEpisodes = null
                )
            }
            if (discoverItems.isNotEmpty() && (loadingMore || hasNextPage)) {
                item(key = "discover_list_footer") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loadingMore) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ChampagneGold
                                )
                                Text("Loading more anime...", color = ChampagneGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onLoadMore,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f))
                            ) {
                                Text("Load More Anime", color = ChampagneGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 195.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            items(discoverItems, key = { it.id }) { item ->
                val tracked = watchlist.firstOrNull { it.id == item.id }
                val merged = tracked ?: item
                val isOnHome = tracked?.inHome == true
                ScreenshotPosterCard(
                    item = merged,
                    showSentimentBadge = tracked != null,
                    showFormatBadge = true,
                    isOnHome = isOnHome,
                    showQuickHomeButton = true,
                    showEpisodeStepper = false,
                    onClick = { onOpenDetail(merged) },
                    onRequestRemoveFromHome = { onRequestRemoveFromHome(merged) },
                    onAddToHome = { onAddToHome(merged) },
                    onViewInLibrary = { onViewInLibrary(merged) },
                    onViewCommunity = { onViewCommunity(merged) },
                    onCommitEpisodes = null
                )
            }
            if (discoverItems.isNotEmpty() && (loadingMore || hasNextPage)) {
                item(key = "discover_grid_footer", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loadingMore) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ChampagneGold
                                )
                                Text("Loading more anime...", color = ChampagneGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onLoadMore,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f))
                            ) {
                                Text("Load More Anime", color = ChampagneGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
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
    isListView: Boolean,
    onToggleListView: () -> Unit,
    onStatusChange: (String) -> Unit,
    onRatingChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onCreateCollectionClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDetail: (DesktopAnimeItem) -> Unit,
    onAddToHome: (DesktopAnimeItem) -> Unit,
    onRequestRemoveFromHome: (DesktopAnimeItem) -> Unit,
    onViewCommunity: (DesktopAnimeItem) -> Unit,
    onCommitEpisodes: suspend (DesktopAnimeItem, Int) -> Result<Unit>
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
            CircleIconActionButton(
                icon = if (isListView) Icons.Outlined.GridView else Icons.AutoMirrored.Filled.ViewList,
                active = isListView,
                onClick = onToggleListView
            )
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
        val trimmedQuery = searchQuery.trim()
        watchlist
            .filter { item ->
                val resolvedTitle = DesktopEnglishTitleMapper.resolve(item.title, item.titleRomaji, null, false) ?: item.title
                (statusFilter == "all" || item.watchStatus == statusFilter) &&
                    (ratingFilter == "all" || SentimentTier.fromStars(item.userStars)?.key == ratingFilter) &&
                    (trimmedQuery.isEmpty() || DesktopEnglishTitleMapper.matchesAllKeywords(
                        trimmedQuery,
                        resolvedTitle,
                        item.title,
                        item.titleRomaji,
                        item.genres.joinToString(" ")
                    ))
            }
            .let { list ->
                val baseSorted = when (sortMode) {
                    "title" -> list.sortedBy { it.title.lowercase() }
                    "rating" -> list.sortedByDescending { it.userStars }
                    "progress" -> list.sortedByDescending { it.watchedEpisodes }
                    else -> list
                }
                if (trimmedQuery.isNotEmpty()) {
                    baseSorted.sortedByDescending { item ->
                        val resolvedTitle = DesktopEnglishTitleMapper.resolve(item.title, item.titleRomaji, null, false) ?: item.title
                        DesktopEnglishTitleMapper.scoreKeywordMatch(
                            rawQuery = trimmedQuery,
                            resolvedEnglish = resolvedTitle,
                            officialEnglish = item.title,
                            romaji = item.titleRomaji
                        )
                    }
                } else {
                    baseSorted
                }
            }
    }

    if (isListView) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            items(filteredList, key = { it.id }) { item ->
                DesktopAnimeListRow(
                    item = item,
                    isOnHome = item.inHome,
                    showEpisodeStepper = true,
                    onClick = { onOpenDetail(item) },
                    onAddToHome = { onAddToHome(item) },
                    onRequestRemoveFromHome = { onRequestRemoveFromHome(item) },
                    onViewInLibrary = { onOpenDetail(item) },
                    onViewCommunity = { onViewCommunity(item) },
                    onCommitEpisodes = { eps -> onCommitEpisodes(item, eps) }
                )
            }
        }
    } else {
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
                    isOnHome = item.inHome,
                    showEpisodeStepper = true,
                    onClick = { onOpenDetail(item) },
                    onRequestRemoveFromHome = { onRequestRemoveFromHome(item) },
                    onAddToHome = { onAddToHome(item) },
                    onViewInLibrary = { onOpenDetail(item) },
                    onViewCommunity = { onViewCommunity(item) },
                    onCommitEpisodes = { eps -> onCommitEpisodes(item, eps) }
                )
            }
        }
    }
}

// ============================================================================
// SCREENSHOT 5: COMMUNITY SCREEN (1:1 Match)
// ============================================================================

@Composable
private fun DesktopCommunityScreen(
    reviews: List<DesktopCommunityReview>,
    unseenDiscussionAnime: List<DesktopUnseenDiscussionAnime>,
    unreadNotificationCount: Int,
    session: DesktopPersistedState,
    isAuthenticated: Boolean,
    currentUserId: String,
    currentUsername: String,
    avatarKey: String,
    ratingFilter: String,
    sortMode: String,
    onRatingChange: (String) -> Unit,
    onSortChange: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onSessionUpdated: (DesktopPersistedState) -> Unit,
    onShowToast: (String, Boolean) -> Unit,
    onNotificationMarkedRead: (String) -> Unit,
    onAllNotificationsMarkedRead: () -> Unit,
    onMarkedAnimeSeen: (Int) -> Unit,
    onOpenAnime: (animeId: Int, fallbackTitle: String, fallbackCover: String) -> Unit,
    onSubmitCommentNote: (animeId: Int, animeTitle: String, animeCover: String, stars: Float, note: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val animeGroups = remember(reviews) { groupDesktopCommunityReviews(reviews) }
    val unseenByAnimeId = remember(unseenDiscussionAnime) { unseenDiscussionAnime.associateBy { it.animeId } }
    val totalNotesAndRatings = remember(animeGroups) { animeGroups.sumOf { it.notesAndRatingsCount } }
    var activeCommunitySection by remember { mutableStateOf("feed") } // "feed" | "tagged"
    var selectedCommentsAnimeId by remember { mutableStateOf<Int?>(null) }
    var selectedFallbackGroup by remember { mutableStateOf<DesktopCommunityAnimeGroup?>(null) }
    var externalModalJumpTarget by remember { mutableStateOf<DesktopNotificationJumpTarget?>(null) }

    var taggedNotifications by remember { mutableStateOf<List<DesktopUserNotification>>(emptyList()) }
    var taggedHasMore by remember { mutableStateOf(false) }
    var taggedLoading by remember { mutableStateOf(false) }

    fun loadTaggedNotifications(reset: Boolean) {
        if (!isAuthenticated || session.isGuest) return
        val offset = if (reset) 0 else taggedNotifications.size
        taggedLoading = true
        scope.launch {
            DesktopDiscussionService.fetchNotifications(session, limit = 30, offset = offset)
                .onSuccess { (page, active) ->
                    onSessionUpdated(active)
                    taggedHasMore = page.hasMore
                    taggedNotifications = if (reset) {
                        page.notifications
                    } else {
                        val existing = taggedNotifications.map { it.id }.toSet()
                        taggedNotifications + page.notifications.filterNot { it.id in existing }
                    }
                }
            taggedLoading = false
        }
    }

    LaunchedEffect(session.userId, isAuthenticated) {
        if (isAuthenticated && !session.isGuest) {
            launch {
                val emojis = DesktopEmojiCacheService.getEmojis(session)
                if (emojis.isNotEmpty()) DesktopEmojiCacheService.preloadTopEmojis(emojis, count = 8)
            }
            loadTaggedNotifications(reset = true)
        }
    }

    val selectedCommentsGroup = remember(animeGroups, selectedCommentsAnimeId, selectedFallbackGroup) {
        val id = selectedCommentsAnimeId ?: return@remember null
        animeGroups.firstOrNull { it.animeId == id } ?: selectedFallbackGroup?.takeIf { it.animeId == id }
    }

    fun openTaggedAnimeInModal(
        animeId: Int,
        animeTitle: String,
        animeCover: String,
        jumpTarget: DesktopNotificationJumpTarget,
        notificationId: String? = null
    ) {
        val readAtIso = Instant.now().toString()
        if (!notificationId.isNullOrBlank()) {
            taggedNotifications = taggedNotifications.map {
                if (it.id == notificationId && !it.isRead) it.copy(isRead = true, readAt = readAtIso) else it
            }
            onNotificationMarkedRead(notificationId)
        }
        taggedNotifications = taggedNotifications.map {
            if (it.animeId == animeId && !it.isRead) it.copy(isRead = true, readAt = readAtIso) else it
        }
        onMarkedAnimeSeen(animeId)
        val existing = animeGroups.firstOrNull { it.animeId == animeId }
        if (existing == null) {
            val fallbackLead = DesktopCommunityReview(
                id = "tagged_fallback_$animeId",
                userId = "",
                username = "Community",
                avatarKey = "avatar_1",
                animeId = animeId,
                animeTitle = animeTitle.ifBlank { "Anime #$animeId" },
                animeCover = animeCover,
                stars = 0f,
                notes = "",
                updatedAt = "",
                formattedDate = ""
            )
            selectedFallbackGroup = DesktopCommunityAnimeGroup(
                animeId = animeId,
                animeTitle = animeTitle.ifBlank { "Anime #$animeId" },
                animeCover = animeCover,
                averageStars = 0f,
                ratedCount = 0,
                notedCount = 0,
                latestUpdatedAt = "",
                formattedLatestDate = "",
                leadReview = fallbackLead,
                reviews = emptyList()
            )
        } else {
            selectedFallbackGroup = null
        }
        externalModalJumpTarget = jumpTarget
        selectedCommentsAnimeId = animeId
    }

    // Top Bar
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Community", fontFamily = FontFamily.Serif, fontSize = 34.sp, color = IvoryText)
            Text(
                "${animeGroups.size} ANIME • $totalNotesAndRatings NOTES & RATINGS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
                color = WarmSilver
            )
        }
        AvatarCircleButton(avatarKey = avatarKey, onClick = onOpenProfile)
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Dedicated Community Feed vs "Tagged You" Switcher
    val totalUnreadTags = unreadNotificationCount.coerceAtLeast(0)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val feedActive = activeCommunitySection == "feed"
        val taggedActive = activeCommunitySection == "tagged"
        Surface(
            shape = CircleShape,
            color = if (feedActive) Color(0xFF2B271F) else ObsidianPillBg,
            border = BorderStroke(1.dp, if (feedActive) ChampagneGold.copy(alpha = 0.7f) else ObsidianBorder),
            modifier = Modifier
                .height(40.dp)
                .clip(CircleShape)
                .clickable { activeCommunitySection = "feed" }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Community Feed",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (feedActive) ChampagneGoldSoft else WarmSilver
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = if (taggedActive) Color(0xFF2B271F) else ObsidianPillBg,
            border = BorderStroke(
                1.dp,
                when {
                    taggedActive -> ChampagneGold.copy(alpha = 0.7f)
                    totalUnreadTags > 0 -> Color(0xFFEF4444).copy(alpha = 0.6f)
                    else -> ObsidianBorder
                }
            ),
            modifier = Modifier
                .height(40.dp)
                .clip(CircleShape)
                .clickable {
                    activeCommunitySection = "tagged"
                    loadTaggedNotifications(reset = true)
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                Text(
                    "@ Tagged You",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (taggedActive) ChampagneGoldSoft else WarmSilver
                )
                if (totalUnreadTags > 0) {
                    Surface(shape = CircleShape, color = Color(0xFFEF4444)) {
                        Text(
                            if (totalUnreadTags > 99) "99+" else "$totalUnreadTags",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (activeCommunitySection == "tagged") {
        DesktopTaggedYouSection(
            notifications = taggedNotifications,
            unreadCount = totalUnreadTags,
            loading = taggedLoading,
            hasMore = taggedHasMore,
            onRefresh = { loadTaggedNotifications(reset = true) },
            onMarkAllRead = {
                val readAtIso = Instant.now().toString()
                taggedNotifications = taggedNotifications.map {
                    if (!it.isRead) it.copy(isRead = true, readAt = readAtIso) else it
                }
                onAllNotificationsMarkedRead()
            },
            onLoadMore = { loadTaggedNotifications(reset = false) },
            onOpenNotification = { n ->
                openTaggedAnimeInModal(
                    animeId = n.animeId,
                    animeTitle = n.animeTitle,
                    animeCover = n.animeCover,
                    jumpTarget = DesktopNotificationJumpTarget(
                        animeId = n.animeId,
                        commentId = n.commentId,
                        threadId = n.threadId,
                        replyId = n.replyId
                    ),
                    notificationId = n.id
                )
            }
        )
    } else {
    // Sentiment Filter Row: ALL, CINEMA, WORTH, AVG, BORING, WASTE
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
                        "ALL (${animeGroups.size})",
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

    val filteredGroups = remember(animeGroups, ratingFilter, sortMode, unseenByAnimeId) {
        animeGroups
            .filter { grp ->
                ratingFilter == "all" ||
                    SentimentTier.fromStars(grp.averageStars)?.key == ratingFilter ||
                    grp.reviews.any { SentimentTier.fromStars(it.stars)?.key == ratingFilter }
            }
            .let { list ->
                if (sortMode == "highest") list.sortedByDescending { it.averageStars }
                else list.sortedByDescending { it.latestUpdatedAt }
            }
            .sortedByDescending { unseenByAnimeId.containsKey(it.animeId) }
    }

    val unseenOnlyAnime = remember(unseenDiscussionAnime, filteredGroups) {
        val visibleIds = filteredGroups.map { it.animeId }.toSet()
        unseenDiscussionAnime.filter { !visibleIds.contains(it.animeId) }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        items(unseenOnlyAnime, key = { "unseen_${it.animeId}" }) { u ->
            val actionText = if (u.latestType == "thread_reply") {
                "${u.latestSenderUsername} replied in your thread"
            } else {
                "${u.latestSenderUsername} tagged you in a discussion"
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianCard,
                border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        openTaggedAnimeInModal(
                            animeId = u.animeId,
                            animeTitle = u.animeTitle,
                            animeCover = u.animeCover,
                            jumpTarget = DesktopNotificationJumpTarget(
                                animeId = u.animeId,
                                commentId = u.latestCommentId,
                                threadId = u.latestThreadId,
                                replyId = u.latestReplyId
                            )
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (u.animeCover.isNotBlank()) {
                        NetworkPosterImage(
                            url = u.animeCover,
                            contentDescription = u.animeTitle,
                            modifier = Modifier
                                .size(width = 64.dp, height = 90.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Text(
                                u.animeTitle.ifBlank { "Anime #${u.animeId}" },
                                fontFamily = FontFamily.Serif,
                                fontSize = 21.sp,
                                color = IvoryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "$actionText • Click to view discussion",
                            color = ChampagneGoldSoft,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        items(filteredGroups, key = { it.animeId }) { grp ->
            val tier = SentimentTier.fromStars(grp.averageStars)
            val filledStars = kotlin.math.round(grp.averageStars).toInt().coerceIn(0, 5)
            val contributors = grp.distinctContributors
            val extraCount = (contributors.size - 1).coerceAtLeast(0)
            val lead = grp.leadReview
            val unseenEntry = unseenByAnimeId[grp.animeId]

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianCard,
                border = BorderStroke(
                    width = if (unseenEntry != null) 1.2.dp else 1.dp,
                    color = if (unseenEntry != null) Color(0xFFEF4444).copy(alpha = 0.55f) else ObsidianBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenAnime(grp.animeId, grp.animeTitle, grp.animeCover) }
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row: 74x104 Poster + Serif Title + 5-Star Row + Sentiment Pill
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        NetworkPosterImage(
                            url = grp.animeCover,
                            contentDescription = grp.animeTitle,
                            modifier = Modifier
                                .size(width = 74.dp, height = 104.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (unseenEntry != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                }
                                Text(
                                    grp.animeTitle,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 23.sp,
                                    color = IvoryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (grp.isAdult) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                                    ) {
                                        Text(
                                            "18+",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFCA5A5),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (unseenEntry != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEF4444).copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                                ) {
                                    val label = if (unseenEntry.latestType == "thread_reply") {
                                        "${unseenEntry.latestSenderUsername} replied • Open"
                                    } else {
                                        "${unseenEntry.latestSenderUsername} tagged you • Open"
                                    }
                                    Text(
                                        label,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (grp.averageStars > 0f) {
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
                                        String.format(Locale.US, "%.1f", grp.averageStars),
                                        fontSize = 16.sp,
                                        color = IvoryText
                                    )
                                    if (grp.ratedCount > 1) {
                                        Text(
                                            "(${grp.ratedCount})",
                                            fontSize = 12.sp,
                                            color = WarmSilver
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = ChampagneGold.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        "COMMUNITY NOTES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp,
                                        color = ChampagneGold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            if (tier != null) {
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
                    }

                    // Quote Box with vertical gold bar (clicking opens all notes & ratings)
                    val previewNote = if (lead.notes.isNotBlank()) {
                        "“${lead.notes}”"
                    } else {
                        "Rated ${String.format(Locale.US, "%.1f", lead.stars)}★ by ${lead.username}"
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianQuoteBox,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedCommentsAnimeId = grp.animeId }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
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
                                    previewNote,
                                    fontStyle = if (lead.notes.isNotBlank()) FontStyle.Italic else FontStyle.Normal,
                                    fontSize = 15.5.sp,
                                    color = IvoryText
                                )
                            }
                            if (grp.notesAndRatingsCount > 1) {
                                Text(
                                    "Click comment icon to view all ${grp.notesAndRatingsCount} notes & ratings",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ChampagneGold,
                                    modifier = Modifier.padding(start = 17.dp)
                                )
                            }
                        }
                    }

                    // Bottom Footer: Reviewer Avatar(s) + Name on left, Themed Comment Icon + Date on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val visibleContributors = contributors.take(3)
                            Box(
                                modifier = Modifier.width((30 + (visibleContributors.size - 1) * 16).dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                visibleContributors.forEachIndexed { idx, c ->
                                    val avPreset = DesktopAvatar.fromKey(c.avatarKey)
                                    Box(
                                        modifier = Modifier
                                            .offset(x = (idx * 16).dp)
                                            .size(30.dp)
                                            .border(1.5.dp, ObsidianCard, CircleShape)
                                            .clip(CircleShape)
                                    ) {
                                        DesktopAvatarImage(
                                            avatarKey = avPreset.key,
                                            username = c.username,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            Text(
                                if (extraCount > 0) "${contributors.first().username} +$extraCount" else contributors.first().username,
                                fontSize = 14.sp,
                                color = WarmSilver
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (grp.formattedLatestDate.isNotBlank()) {
                                Text(grp.formattedLatestDate, fontSize = 12.5.sp, color = WarmSilver)
                            }
                            // Themed Comment Icon Pill matching Obsidian & Champagne Gold theme
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2B271F),
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .clickable { selectedCommentsAnimeId = grp.animeId }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Comments (${grp.notesAndRatingsCount})",
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        "${grp.notesAndRatingsCount}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChampagneGoldSoft
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    } // end else (feed section)

    if (selectedCommentsGroup != null) {
        val myExisting = selectedCommentsGroup.reviews.firstOrNull {
            (currentUserId.isNotBlank() && it.userId == currentUserId) ||
                it.username.equals(currentUsername, ignoreCase = true)
        }
        val unseenForGroup = unseenByAnimeId[selectedCommentsGroup.animeId]
        var draftStars by remember(selectedCommentsGroup.animeId) { mutableStateOf(myExisting?.stars ?: 0f) }
        var draftNote by remember(selectedCommentsGroup.animeId) { mutableStateOf(myExisting?.notes.orEmpty()) }
        var modalTab by remember(selectedCommentsGroup.animeId, externalModalJumpTarget) {
            mutableStateOf("discussion")
        }
        var pendingReplyUser by remember(selectedCommentsGroup.animeId) { mutableStateOf<DesktopMentionUser?>(null) }
        var openNoteMenuId by remember(selectedCommentsGroup.animeId) { mutableStateOf<String?>(null) }
        var modalJumpTarget by remember(selectedCommentsGroup.animeId, externalModalJumpTarget) {
            mutableStateOf(
                externalModalJumpTarget ?: unseenForGroup?.let {
                    DesktopNotificationJumpTarget(
                        animeId = it.animeId,
                        commentId = it.latestCommentId,
                        threadId = it.latestThreadId,
                        replyId = it.latestReplyId
                    )
                }
            )
        }

        Dialog(
            onDismissRequest = {
                selectedCommentsAnimeId = null
                selectedFallbackGroup = null
                externalModalJumpTarget = null
            }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF030304),
                border = BorderStroke(1.dp, Color(0x33E8C88F)),
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .height(780.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // LivTrAk Hub Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 18.dp, top = 20.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NetworkPosterImage(
                            url = selectedCommentsGroup.animeCover,
                            contentDescription = selectedCommentsGroup.animeTitle,
                            modifier = Modifier
                                .size(width = 60.dp, height = 86.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val id = selectedCommentsGroup.animeId
                                    val title = selectedCommentsGroup.animeTitle
                                    val cover = selectedCommentsGroup.animeCover
                                    selectedCommentsAnimeId = null
                                    selectedFallbackGroup = null
                                    externalModalJumpTarget = null
                                    onOpenAnime(id, title, cover)
                                }
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 2.dp)
                                .clickable {
                                    val id = selectedCommentsGroup.animeId
                                    val title = selectedCommentsGroup.animeTitle
                                    val cover = selectedCommentsGroup.animeCover
                                    selectedCommentsAnimeId = null
                                    selectedFallbackGroup = null
                                    externalModalJumpTarget = null
                                    onOpenAnime(id, title, cover)
                                },
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                "COMMUNITY HUB",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                color = ChampagneGold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    selectedCommentsGroup.animeTitle,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 22.sp,
                                    color = IvoryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (selectedCommentsGroup.isAdult) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                                    ) {
                                        Text(
                                            "18+",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFCA5A5),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            val avgStr = if (selectedCommentsGroup.averageStars > 0f) {
                                "★ ${String.format(java.util.Locale.US, "%.1f", selectedCommentsGroup.averageStars)} avg • "
                            } else ""
                            val mCount = selectedCommentsGroup.distinctContributors.size
                            Text(
                                "${avgStr}$mCount ${if (mCount == 1) "member" else "members"}",
                                fontSize = 12.sp,
                                color = WarmSilver
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0x14FFFFFF),
                            border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    selectedCommentsAnimeId = null
                                    selectedFallbackGroup = null
                                    externalModalJumpTarget = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = IvoryText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // LivTrAk Underlined Tabs: Live Chat | Reviews (N)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        val discActive = modalTab == "discussion"
                        val notesActive = modalTab == "notes"
                        Column(
                            modifier = Modifier.clickable { modalTab = "discussion" }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Live Chat",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (discActive) ChampagneGold else WarmSilver
                                )
                                if (unseenForGroup != null && !discActive) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(modifier = Modifier.width(68.dp).height(2.dp).background(if (discActive) ChampagneGold else Color.Transparent))
                        }
                        Column(
                            modifier = Modifier.clickable { modalTab = "notes" }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Reviews (${selectedCommentsGroup.reviews.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (notesActive) ChampagneGold else WarmSilver
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(modifier = Modifier.width(82.dp).height(2.dp).background(if (notesActive) ChampagneGold else Color.Transparent))
                        }
                    }

                    HorizontalDivider(color = Color(0x14FFFFFF))

                    if (modalTab == "discussion") {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            DesktopLivTrAkChatSection(
                                anime = DesktopAnimeItem(
                                    id = selectedCommentsGroup.animeId,
                                    title = selectedCommentsGroup.animeTitle,
                                    coverImage = selectedCommentsGroup.animeCover
                                ),
                                session = session,
                                isAuthenticated = isAuthenticated,
                                jumpTarget = modalJumpTarget,
                                onJumpTargetConsumed = {
                                    modalJumpTarget = null
                                    externalModalJumpTarget = null
                                },
                                onSessionUpdated = onSessionUpdated,
                                onShowToast = onShowToast,
                                onMarkedAnimeSeen = onMarkedAnimeSeen,
                                initialReplyUser = pendingReplyUser,
                                onInitialReplyUserConsumed = { pendingReplyUser = null },
                                inModal = true
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                        // All Notes & Ratings List with Reply Button + Right-Click Reply Menu
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(selectedCommentsGroup.reviews, key = { it.id }) { rev ->
                                val revTier = SentimentTier.fromStars(rev.stars)
                                val revFilled = kotlin.math.round(rev.stars).toInt().coerceIn(0, 5)
                                val avPreset = DesktopAvatar.fromKey(rev.avatarKey)
                                val triggerReply = {
                                    pendingReplyUser = DesktopMentionUser(
                                        userId = rev.userId,
                                        username = rev.username,
                                        avatarKey = rev.avatarKey
                                    )
                                    modalTab = "discussion"
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = ObsidianCard,
                                        border = BorderStroke(1.dp, ObsidianBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(rev.id) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                                            openNoteMenuId = rev.id
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    DesktopAvatarImage(
                                                        avatarKey = avPreset.key,
                                                        username = rev.username,
                                                        modifier = Modifier.size(34.dp).clip(CircleShape)
                                                    )
                                                    Column {
                                                        Text(
                                                            rev.username,
                                                            fontSize = 14.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = IvoryText
                                                        )
                                                        if (rev.formattedDate.isNotBlank()) {
                                                            Text(rev.formattedDate, fontSize = 11.sp, color = WarmSilver)
                                                        }
                                                    }
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = ChampagneGold.copy(alpha = 0.14f),
                                                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .clickable { triggerReply() }
                                                    ) {
                                                        Text(
                                                            "↩ Reply",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = ChampagneGoldSoft,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    if (revTier != null) {
                                                        Surface(shape = CircleShape, color = revTier.pillBg) {
                                                            Text(
                                                                revTier.shortLabel,
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                letterSpacing = 1.sp,
                                                                color = revTier.textColor,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (rev.stars > 0f) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    (1..5).forEach { i ->
                                                        Text(
                                                            "★",
                                                            fontSize = 15.sp,
                                                            color = if (i <= revFilled) ChampagneGold else Color(0xFF393E4F)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        String.format(Locale.US, "%.1f", rev.stars),
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = IvoryText
                                                    )
                                                }
                                            }

                                            if (rev.notes.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = ObsidianQuoteBox,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .height(26.dp)
                                                                .clip(CircleShape)
                                                                .background(ChampagneGold)
                                                        )
                                                        Text(
                                                            "“${rev.notes}”",
                                                            fontStyle = FontStyle.Italic,
                                                            fontSize = 14.sp,
                                                            color = IvoryText
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = openNoteMenuId == rev.id,
                                        onDismissRequest = { openNoteMenuId = null },
                                        containerColor = ObsidianCard
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("↩ Reply to @${rev.username}", color = IvoryText, fontSize = 13.sp) },
                                            onClick = {
                                                openNoteMenuId = null
                                                triggerReply()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = ObsidianBorder)

                        // Inline Composer: Add/Update note & rating in this template
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (myExisting != null) "UPDATE YOUR NOTE & RATING" else "ADD YOUR NOTE & RATING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp,
                                    color = ChampagneGold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    (1..5).forEach { idx ->
                                        val active = draftStars >= idx - 0.5f
                                        Text(
                                            "★",
                                            fontSize = 20.sp,
                                            color = if (active) ChampagneGold else Color(0xFF393E4F),
                                            modifier = Modifier.clickable {
                                                draftStars = if (draftStars == idx.toFloat()) idx - 0.5f else idx.toFloat()
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = draftNote,
                                onValueChange = { draftNote = it },
                                placeholder = { Text("Share your note for ${selectedCommentsGroup.animeTitle}...", color = WarmSilver, fontSize = 13.sp) },
                                maxLines = 3,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ChampagneGold,
                                    unfocusedBorderColor = ObsidianBorder,
                                    focusedTextColor = IvoryText,
                                    unfocusedTextColor = IvoryText
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            var commPosting by remember(selectedCommentsGroup.animeId) { mutableStateOf(false) }
                            var commSuccessMsg by remember(selectedCommentsGroup.animeId) { mutableStateOf<String?>(null) }

                            Button(
                                onClick = {
                                    if (commPosting) return@Button
                                    commPosting = true
                                    onSubmitCommentNote(
                                        selectedCommentsGroup.animeId,
                                        selectedCommentsGroup.animeTitle,
                                        selectedCommentsGroup.animeCover,
                                        draftStars,
                                        draftNote
                                    )
                                    commSuccessMsg = if (draftNote.trim().isNotEmpty()) "Comment posted successfully" else "Rating saved"
                                    draftNote = ""
                                    commPosting = false
                                },
                                enabled = !commPosting && (draftNote.trim().isNotEmpty() || draftStars > 0f),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChampagneGold,
                                    contentColor = ChampagneOnGold
                                ),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Text(
                                    if (commPosting) "POSTING..." else if (myExisting != null) "UPDATE NOTE IN COMMUNITY" else "POST NOTE TO COMMUNITY",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontSize = 12.5.sp
                                )
                            }
                            commSuccessMsg?.let { msg ->
                                DesktopFeedbackBanner(message = msg, isError = false)
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SHARED POSTER CARD & TOP BAR ACTIONS (Matches Screenshots 2, 3, 4)
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScreenshotPosterCard(
    item: DesktopAnimeItem,
    showSentimentBadge: Boolean,
    showFormatBadge: Boolean,
    isOnHome: Boolean = item.inHome,
    showQuickHomeButton: Boolean = false,
    showEpisodeStepper: Boolean = false,
    onClick: () -> Unit,
    onRequestRemoveFromHome: (() -> Unit)? = null,
    onAddToHome: (() -> Unit)? = null,
    onViewInLibrary: (() -> Unit)? = null,
    onViewCommunity: (() -> Unit)? = null,
    onCommitEpisodes: (suspend (Int) -> Result<Unit>)? = null
) {
    val tier = if (showSentimentBadge) SentimentTier.fromStars(item.userStars) else null
    val displayStars = if (item.userStars > 0f) item.userStars else item.catalogStars
    var menuExpanded by remember(item.id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(22.dp))
                    .pointerInput(item.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press &&
                                    (event.buttons.isSecondaryPressed || event.keyboardModifiers.isCtrlPressed)
                                ) {
                                    event.changes.forEach { it.consume() }
                                    menuExpanded = true
                                }
                            }
                        }
                    }
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { menuExpanded = true }
                    )
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

                    // Bottom-right quick + Add / In Home button on Discover cards
                    if (showQuickHomeButton && (onAddToHome != null || onRequestRemoveFromHome != null)) {
                        Surface(
                            shape = CircleShape,
                            color = if (isOnHome) Color(0xE6142A23) else Color(0xE6E5C17B),
                            border = BorderStroke(
                                1.dp,
                                if (isOnHome) EmeraldWorth.copy(alpha = 0.7f) else ChampagneGold
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (isOnHome) onRequestRemoveFromHome?.invoke() else onAddToHome?.invoke()
                                }
                        ) {
                            Text(
                                text = if (isOnHome) "✓ Home" else "+ Add",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnHome) EmeraldWorth else ChampagneOnGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            DesktopPosterContextMenu(
                expanded = menuExpanded,
                isOnHome = isOnHome,
                onDismiss = { menuExpanded = false },
                onRemoveFromHome = {
                    menuExpanded = false
                    onRequestRemoveFromHome?.invoke()
                },
                onAddToHome = {
                    menuExpanded = false
                    onAddToHome?.invoke()
                },
                onViewDetails = {
                    menuExpanded = false
                    onClick()
                },
                onViewLibrary = {
                    menuExpanded = false
                    onViewInLibrary?.invoke()
                },
                onViewCommunity = {
                    menuExpanded = false
                    onViewCommunity?.invoke()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = IvoryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp)
        )

        if (showEpisodeStepper && onCommitEpisodes != null) {
            Spacer(modifier = Modifier.height(6.dp))
            DesktopInlineEpisodeStepper(
                watchedEpisodes = item.watchedEpisodes,
                totalEpisodes = item.totalEpisodes,
                compact = true,
                onCommitEpisode = onCommitEpisodes,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DesktopPosterContextMenu(
    expanded: Boolean,
    isOnHome: Boolean,
    onDismiss: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onAddToHome: () -> Unit,
    onViewDetails: () -> Unit,
    onViewLibrary: () -> Unit,
    onViewCommunity: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
    ) {
        if (isOnHome) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Remove from Home",
                        color = CrimsonWaste,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = CrimsonWaste,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = onRemoveFromHome
            )
        } else {
            DropdownMenuItem(
                text = {
                    Text(
                        "Add to Home",
                        color = ChampagneGold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = onAddToHome
            )
        }
        DropdownMenuItem(
            text = { Text("View Anime Details", color = IvoryText, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = WarmSilver, modifier = Modifier.size(18.dp))
            },
            onClick = onViewDetails
        )
        DropdownMenuItem(
            text = { Text("View in Library", color = IvoryText, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = WarmSilver, modifier = Modifier.size(18.dp))
            },
            onClick = onViewLibrary
        )
        DropdownMenuItem(
            text = { Text("View Community", color = IvoryText, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Group, contentDescription = null, tint = WarmSilver, modifier = Modifier.size(18.dp))
            },
            onClick = onViewCommunity
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopAnimeListRow(
    item: DesktopAnimeItem,
    isOnHome: Boolean,
    showEpisodeStepper: Boolean,
    onClick: () -> Unit,
    onAddToHome: () -> Unit,
    onRequestRemoveFromHome: () -> Unit,
    onViewInLibrary: () -> Unit,
    onViewCommunity: () -> Unit,
    onCommitEpisodes: (suspend (Int) -> Result<Unit>)?
) {
    var menuExpanded by remember(item.id) { mutableStateOf(false) }
    val displayStars = if (item.userStars > 0f) item.userStars else item.catalogStars

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .pointerInput(item.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press &&
                                (event.buttons.isSecondaryPressed || event.keyboardModifiers.isCtrlPressed)
                            ) {
                                event.changes.forEach { it.consume() }
                                menuExpanded = true
                            }
                        }
                    }
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NetworkPosterImage(
                    url = item.coverImage,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(width = 58.dp, height = 84.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IvoryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${item.format} • ${item.watchStatus.replace('_', ' ').uppercase()}${if (displayStars > 0f) " • ★ ${String.format(Locale.US, "%.1f", displayStars)}" else ""}",
                        fontSize = 11.5.sp,
                        color = WarmSilver
                    )
                }
                if (showEpisodeStepper && onCommitEpisodes != null) {
                    DesktopInlineEpisodeStepper(
                        watchedEpisodes = item.watchedEpisodes,
                        totalEpisodes = item.totalEpisodes,
                        compact = true,
                        onCommitEpisode = onCommitEpisodes,
                        modifier = Modifier.width(156.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (isOnHome) Color(0xFF142A23) else ChampagneGold,
                    border = BorderStroke(1.dp, if (isOnHome) EmeraldWorth.copy(alpha = 0.6f) else ChampagneGold),
                    modifier = Modifier.clip(CircleShape).clickable {
                        if (isOnHome) onRequestRemoveFromHome() else onAddToHome()
                    }
                ) {
                    Text(
                        if (isOnHome) "✓ Home" else "+ Home",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnHome) EmeraldWorth else ChampagneOnGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        DesktopPosterContextMenu(
            expanded = menuExpanded,
            isOnHome = isOnHome,
            onDismiss = { menuExpanded = false },
            onRemoveFromHome = {
                menuExpanded = false
                onRequestRemoveFromHome()
            },
            onAddToHome = {
                menuExpanded = false
                onAddToHome()
            },
            onViewDetails = {
                menuExpanded = false
                onClick()
            },
            onViewLibrary = {
                menuExpanded = false
                onViewInLibrary()
            },
            onViewCommunity = {
                menuExpanded = false
                onViewCommunity()
            }
        )
    }
}

@Composable
private fun CircleIconActionButton(
    icon: ImageVector,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (active) Color(0xFF2B271F) else ObsidianPillBg,
        border = BorderStroke(1.dp, if (active) ChampagneGold.copy(alpha = 0.7f) else ObsidianBorder),
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (active) ChampagneGold else IvoryText, modifier = Modifier.size(20.dp))
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
    onUpdateProfile: suspend (String, String) -> Result<Unit>,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var nameInput by remember(session.username) { mutableStateOf(session.username) }
    var selectedAvatar by remember(session.avatarKey) { mutableStateOf(session.avatarKey) }
    var isSaving by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackError by remember { mutableStateOf(false) }

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
                        username = nameInput.ifBlank { session.username },
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
                    onValueChange = {
                        nameInput = it
                        feedbackMessage = null
                    },
                    label = { Text("Display Username") },
                    singleLine = true,
                    enabled = !isSaving,
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
                                .clickable(enabled = !isSaving) {
                                    selectedAvatar = av.key
                                    feedbackMessage = null
                                }
                        )
                    }
                }

                feedbackMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 12.5.sp,
                        color = if (feedbackError) CrimsonWaste else EmeraldWorth
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            if (isSaving) return@Button
                            isSaving = true
                            feedbackMessage = null
                            scope.launch {
                                val targetName = nameInput.trim().ifBlank { session.username }
                                val res = onUpdateProfile(targetName, selectedAvatar)
                                isSaving = false
                                res.onSuccess {
                                    feedbackError = false
                                    feedbackMessage = "Profile saved and synced."
                                }.onFailure { err ->
                                    feedbackError = true
                                    feedbackMessage = err.message ?: "Failed to update profile."
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = ChampagneOnGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Save Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        enabled = !isSaving,
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
    session: DesktopPersistedState,
    isAuthenticated: Boolean,
    notificationJumpTarget: DesktopNotificationJumpTarget?,
    onNotificationJumpConsumed: () -> Unit,
    onMarkedAnimeSeen: (Int) -> Unit,
    onSessionUpdated: (DesktopPersistedState) -> Unit,
    onShowToast: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    onSelectSeason: (Int) -> Unit,
    onSave: (DesktopAnimeItem) -> Unit,
    onCommitEpisodes: suspend (Int) -> Result<Unit>,
    onAddToHome: () -> Unit,
    onRequestRemoveFromHome: () -> Unit,
    onDelete: (DesktopAnimeItem) -> Unit
) {
    var watchStatus by remember(item.id, item.watchStatus) { mutableStateOf(item.watchStatus) }
    var watchedEps by remember(item.id, item.watchedEpisodes) { mutableStateOf(item.watchedEpisodes) }
    var stars by remember(item.id, item.userStars) { mutableStateOf(item.userStars) }
    var notes by remember(item.id, item.notes) { mutableStateOf(item.notes) }
    var seasons by remember(item.id) { mutableStateOf<List<DesktopSeasonEntry>>(emptyList()) }
    var isSavingDetail by remember(item.id) { mutableStateOf(false) }
    var detailSavedFeedback by remember(item.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id) {
        seasons = DesktopSeasonResolver.resolveSeasons(item.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
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

            // Explicit Home tracking toggle (independent of Library & Community)
            if (item.inHome) {
                OutlinedButton(
                    onClick = onRequestRemoveFromHome,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, CrimsonWaste.copy(alpha = 0.65f))
                ) {
                    Text("Remove from Home", color = CrimsonWaste, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                }
            } else {
                Button(
                    onClick = onAddToHome,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                ) {
                    Text("+ Add to Home", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
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
                    if (seasons.size > 1) {
                        val activeSeason = seasons.firstOrNull { it.isCurrent || it.id == item.id }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SEASONS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp,
                                color = ChampagneGold
                            )
                            val distinctSeasonCount = seasons.map { it.seasonNumber }.distinct().size
                            val metaText = if (activeSeason != null) {
                                val yr = activeSeason.seasonYear?.let { " • $it" }.orEmpty()
                                val totalLabel = if (distinctSeasonCount == 1) {
                                    "1 Season (${seasons.size} Parts)"
                                } else {
                                    "$distinctSeasonCount Seasons"
                                }
                                "${activeSeason.displayLabel} • $totalLabel$yr"
                            } else {
                                "$distinctSeasonCount Seasons"
                            }
                            Text(
                                metaText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmSilver
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons, key = { it.id }) { s ->
                                val isSelected = s.isCurrent || s.id == item.id
                                val isUpcoming = s.status.equals("NOT_YET_RELEASED", ignoreCase = true)
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) ChampagneGold else ObsidianPillBg,
                                    border = BorderStroke(1.dp, if (isSelected) ChampagneGold else ObsidianBorder),
                                    modifier = Modifier
                                        .widthIn(min = 48.dp)
                                        .height(40.dp)
                                        .clip(CircleShape)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable(enabled = !isSelected) { onSelectSeason(s.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = s.shortLabel,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ChampagneOnGold else IvoryText
                                        )
                                        if (isUpcoming && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF59E0B))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
                    }

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

                    val isMovieFormat = item.format.equals("MOVIE", ignoreCase = true)
                    Text(
                        if (isMovieFormat) "MOVIE PROGRESS (0 = UNWATCHED, 1 = WATCHED)" else "EPISODE PROGRESS (CLICK NUMBER TO TYPE DIRECTLY)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = ChampagneGold
                    )

                    DesktopInlineEpisodeStepper(
                        watchedEpisodes = watchedEps,
                        totalEpisodes = item.totalEpisodes ?: if (isMovieFormat) 1 else null,
                        compact = false,
                        onCommitEpisode = { nextEps ->
                            val res = onCommitEpisodes(nextEps)
                            res.onSuccess {
                                watchedEps = nextEps
                                if (DesktopTrackingRules.shouldAutoComplete(item.status, item.totalEpisodes, nextEps)) {
                                    watchStatus = "completed"
                                }
                            }
                            res
                        },
                        modifier = Modifier.widthIn(max = 280.dp)
                    )

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
                            enabled = !isSavingDetail,
                            onClick = {
                                if (isSavingDetail) return@Button
                                isSavingDetail = true
                                val hadRating = item.userStars > 0f
                                val effectiveStatus = if (
                                    watchStatus != "dropped" &&
                                    DesktopTrackingRules.shouldAutoComplete(item.status, item.totalEpisodes, watchedEps)
                                ) "completed" else watchStatus
                                val keepHome = item.inHome &&
                                    effectiveStatus != "dropped" &&
                                    effectiveStatus != "completed" &&
                                    !DesktopTrackingRules.shouldAutoComplete(item.status, item.totalEpisodes, watchedEps)
                                onSave(
                                    item.copy(
                                        inWatchlist = true,
                                        inHome = keepHome,
                                        watchStatus = effectiveStatus,
                                        watchedEpisodes = watchedEps,
                                        userStars = stars,
                                        notes = notes,
                                        updatedAt = Instant.now().toString()
                                    )
                                )
                                detailSavedFeedback = when {
                                    notes.trim().isNotEmpty() && stars > 0f -> "Rating (${String.format(Locale.US, "%.1f", stars)}★) & review note saved successfully"
                                    stars > 0f && hadRating -> "Rating updated to ${String.format(Locale.US, "%.1f", stars)}★"
                                    stars > 0f -> "Rating saved (${String.format(Locale.US, "%.1f", stars)}★)"
                                    notes.trim().isNotEmpty() -> "Review note saved successfully"
                                    else -> "Progress saved successfully"
                                }
                                isSavingDetail = false
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                        ) {
                            Text(if (isSavingDetail) "Saving..." else "Save & Sync to Cloud", fontWeight = FontWeight.Bold)
                        }
                        if (item.inWatchlist) {
                            OutlinedButton(
                                onClick = { onDelete(item) },
                                shape = CircleShape,
                                border = BorderStroke(1.dp, CrimsonWaste.copy(alpha = 0.6f))
                            ) {
                                Text("Delete from Library", color = CrimsonWaste)
                            }
                        }
                    }

                    detailSavedFeedback?.let { msg ->
                        DesktopFeedbackBanner(message = msg, isError = false)
                    }

                    if (item.synopsis.isNotBlank()) {
                        Text(item.synopsis, color = WarmSilver, fontSize = 13.5.sp, lineHeight = 20.sp)
                    }
                }
            }
        }

        DesktopLivTrAkChatSection(
            anime = item,
            session = session,
            isAuthenticated = isAuthenticated,
            jumpTarget = notificationJumpTarget,
            onJumpTargetConsumed = onNotificationJumpConsumed,
            onSessionUpdated = onSessionUpdated,
            onShowToast = onShowToast,
            onMarkedAnimeSeen = onMarkedAnimeSeen,
            inModal = false
        )

        Spacer(modifier = Modifier.height(24.dp))
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

private object SandroneAudioPlayer {
    private const val IMAGE_FILE = "sandrone.png"
    private val VOICE_CLIPS = listOf(
        "s.mp3",
        "s1.mp3",
        "s2.mp3",
        "s3.mp3",
        "s4.mp3",
        "s5.mp3",
        "s6.mp3",
        "s7.mp3",
        "s8.mp3",
        "s9.mp3"
    )

    private val lock = Any()
    private val playToken = AtomicLong(0L)
    private var currentPlayer: Player? = null
    private var lastClipIndex: Int = -1
    @Volatile
    private var cachedBitmap: ImageBitmap? = null

    private fun openAssetStream(fileName: String): InputStream? {
        val resourceStream = SandroneAudioPlayer::class.java.getResourceAsStream("/sandrone/$fileName")
        if (resourceStream != null) return BufferedInputStream(resourceStream)
        val fallbackPaths = listOf(
            File("public/assets/sandrone/$fileName"),
            File("../public/assets/sandrone/$fileName"),
            File("assets/sandrone/$fileName"),
            File("../assets/sandrone/$fileName")
        )
        val existingFile = fallbackPaths.firstOrNull { it.isFile && it.canRead() }
        return existingFile?.let { BufferedInputStream(FileInputStream(it)) }
    }

    fun loadBitmap(): ImageBitmap? {
        cachedBitmap?.let { return it }
        return runCatching {
            val bytes = openAssetStream(IMAGE_FILE)?.use { it.readBytes() } ?: return null
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap().also { cachedBitmap = it }
        }.getOrNull()
    }

    fun playRandom(onPlaybackStateChanged: (Boolean) -> Unit) {
        if (VOICE_CLIPS.isEmpty()) return
        val nextIndex = synchronized(lock) {
            var idx = Random.nextInt(VOICE_CLIPS.size)
            if (VOICE_CLIPS.size > 1 && idx == lastClipIndex) {
                idx = (idx + 1 + Random.nextInt(VOICE_CLIPS.size - 1)) % VOICE_CLIPS.size
            }
            lastClipIndex = idx
            idx
        }
        val clipName = VOICE_CLIPS[nextIndex]
        val token = playToken.incrementAndGet()
        stopCurrentPlayer()
        onPlaybackStateChanged(true)

        thread(start = true, isDaemon = true, name = "sandrone-voice-$token") {
            var player: Player? = null
            try {
                val stream = openAssetStream(clipName)
                if (stream == null) {
                    if (playToken.get() == token) onPlaybackStateChanged(false)
                    return@thread
                }
                stream.use { input ->
                    player = Player(input)
                    synchronized(lock) {
                        if (playToken.get() != token) {
                            runCatching { player?.close() }
                            return@thread
                        }
                        currentPlayer = player
                    }
                    player?.play()
                }
            } catch (_: Throwable) {
                // Ignore playback interruption when rapidly clicking or closing window
            } finally {
                synchronized(lock) {
                    if (currentPlayer === player) {
                        currentPlayer = null
                    }
                }
                runCatching { player?.close() }
                if (playToken.get() == token) {
                    onPlaybackStateChanged(false)
                }
            }
        }
    }

    private fun stopCurrentPlayer() {
        val prev = synchronized(lock) {
            val p = currentPlayer
            currentPlayer = null
            p
        }
        runCatching { prev?.close() }
    }

    fun stop() {
        playToken.incrementAndGet()
        stopCurrentPlayer()
    }
}

@Composable
private fun SandroneSidebarMascot(modifier: Modifier = Modifier) {
    val bitmap = remember { SandroneAudioPlayer.loadBitmap() } ?: return
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var isPressedBounce by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressedBounce -> 0.92f
            isPlaying -> 1.03f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    DisposableEffect(Unit) {
        onDispose {
            SandroneAudioPlayer.stop()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .size(196.dp)
                .clip(RoundedCornerShape(24.dp))
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isPressedBounce = true
                    scope.launch {
                        delay(130)
                        isPressedBounce = false
                    }
                    SandroneAudioPlayer.playRandom { playing ->
                        scope.launch {
                            isPlaying = playing
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ChampagneGold.copy(alpha = 0.24f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            Image(
                bitmap = bitmap,
                contentDescription = "Sandrone — click to play a random voice line",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(188.dp)
                    .scale(scale)
            )
        }
    }
}

