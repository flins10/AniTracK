package com.example.data.repository

import android.content.Context
import com.example.data.local.AnimeEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class OwnerVaultSnapshot(
    val ownerKey: String,
    val items: List<AnimeEntity> = emptyList(),
    val deletedAnimeIds: List<Int> = emptyList(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Isolated per-user and per-guest local vault persistence store.
 * Each registered user (`user_<userId>`) and each guest session (`guest_<guestId>`)
 * maintains a completely separate persistent snapshot so personal watchlists, ratings,
 * reviews, progress, and stats never leak across users or guest sessions.
 */
class OwnerVaultStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val vaultsDir: File by lazy {
        File(appContext.filesDir, VAULTS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    fun getOrCreateGuestId(): String {
        val existing = prefs.getString(KEY_GUEST_ID, null)
        if (!existing.isNullOrBlank() && existing.startsWith("guest_")) {
            return existing
        }
        val fresh = generateGuestId()
        prefs.edit().putString(KEY_GUEST_ID, fresh).commit()
        return fresh
    }

    fun rotateGuestId(): String {
        val fresh = generateGuestId()
        prefs.edit().putString(KEY_GUEST_ID, fresh).commit()
        return fresh
    }

    fun isGuestModeActive(): Boolean = prefs.getBoolean(KEY_ACTIVE_GUEST_MODE, false)

    fun setGuestModeActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_ACTIVE_GUEST_MODE, active).commit()
    }

    fun getLastActiveOwnerKey(): String? = prefs.getString(KEY_LAST_OWNER, null)?.takeIf { it.isNotBlank() }

    fun setLastActiveOwnerKey(ownerKey: String?) {
        prefs.edit().apply {
            if (ownerKey.isNullOrBlank()) remove(KEY_LAST_OWNER)
            else putString(KEY_LAST_OWNER, ownerKey)
        }.commit()
    }

    fun saveOwnerVault(
        ownerKey: String,
        items: List<AnimeEntity>,
        deletedAnimeIds: List<Int> = emptyList()
    ) {
        if (ownerKey.isBlank()) return
        runCatching {
            val file = vaultFileFor(ownerKey)
            val root = JSONObject()
            root.put("ownerKey", ownerKey)
            root.put("updatedAtEpochMs", System.currentTimeMillis())

            val itemsArr = JSONArray()
            items.forEach { itemsArr.put(encodeEntity(it)) }
            root.put("items", itemsArr)

            val delArr = JSONArray()
            deletedAnimeIds.distinct().forEach { delArr.put(it) }
            root.put("deletedAnimeIds", delArr)

            file.writeText(root.toString(), Charsets.UTF_8)
        }
    }

    fun loadOwnerVault(ownerKey: String): OwnerVaultSnapshot {
        if (ownerKey.isBlank()) return OwnerVaultSnapshot(ownerKey = ownerKey)
        return runCatching {
            val file = vaultFileFor(ownerKey)
            if (!file.exists()) return OwnerVaultSnapshot(ownerKey = ownerKey)
            val raw = file.readText(Charsets.UTF_8)
            val root = JSONObject(raw)
            val itemsArr = root.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.optJSONObject(i) ?: continue
                    add(decodeEntity(obj))
                }
            }
            val delArr = root.optJSONArray("deletedAnimeIds") ?: JSONArray()
            val deleted = buildList {
                for (i in 0 until delArr.length()) {
                    add(delArr.optInt(i))
                }
            }
            OwnerVaultSnapshot(
                ownerKey = root.optString("ownerKey", ownerKey),
                items = items,
                deletedAnimeIds = deleted,
                updatedAtEpochMs = root.optLong("updatedAtEpochMs", 0L)
            )
        }.getOrDefault(OwnerVaultSnapshot(ownerKey = ownerKey))
    }

    fun deleteOwnerVault(ownerKey: String) {
        if (ownerKey.isBlank()) return
        runCatching {
            val file = vaultFileFor(ownerKey)
            if (file.exists()) file.delete()
        }
    }

    private fun vaultFileFor(ownerKey: String): File {
        val safeName = ownerKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(vaultsDir, "$safeName.json")
    }

    private fun encodeEntity(e: AnimeEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("titleRomaji", e.titleRomaji)
        putOpt("titleEnglish", e.titleEnglish)
        putOpt("titleNative", e.titleNative)
        putOpt("titlePinyin", e.titlePinyin)
        put("metadataSource", e.metadataSource)
        put("coverImage", e.coverImage)
        putOpt("bannerImage", e.bannerImage)
        putOpt("format", e.format)
        putOpt("status", e.status)
        if (e.totalEpisodes != null) put("totalEpisodes", e.totalEpisodes)
        put("currentEpisode", e.currentEpisode)
        put("watchedEpisodeCount", e.watchedEpisodeCount)
        put("userSeason", e.userSeason)
        putOpt("userStatus", e.userStatus)
        if (e.userScore != null) put("userScore", e.userScore)
        putOpt("startDate", e.startDate)
        putOpt("finishDate", e.finishDate)
        put("airingDayOfWeek", e.airingDayOfWeek)
        if (e.airingTimeUnix != null) put("airingTimeUnix", e.airingTimeUnix)
        if (e.nextEpisodeNumber != null) put("nextEpisodeNumber", e.nextEpisodeNumber)
        put("genres", e.genres)
        put("studios", e.studios)
        putOpt("season", e.season)
        if (e.seasonYear != null) put("seasonYear", e.seasonYear)
        if (e.score != null) put("score", e.score)
        if (e.popularity != null) put("popularity", e.popularity)
        putOpt("synopsis", e.synopsis)
        putOpt("countryOfOrigin", e.countryOfOrigin)
        put("inWatchlist", e.inWatchlist)
        put("isWishlist", e.isWishlist)
        put("isFavorite", e.isFavorite)
        put("isPinned", e.isPinned)
        put("notificationsEnabled", e.notificationsEnabled)
        put("isWatched", e.isWatched)
        if (e.lastWatchedTime != null) put("lastWatchedTime", e.lastWatchedTime)
        putOpt("userNotes", e.userNotes)
        put("lastCachedAt", e.lastCachedAt)
    }

    private fun decodeEntity(o: JSONObject): AnimeEntity = AnimeEntity(
        id = o.getInt("id"),
        titleRomaji = o.optString("titleRomaji", "Anime"),
        titleEnglish = o.optString("titleEnglish").takeIf { it.isNotBlank() && it != "null" },
        titleNative = o.optString("titleNative").takeIf { it.isNotBlank() && it != "null" },
        titlePinyin = o.optString("titlePinyin").takeIf { it.isNotBlank() && it != "null" },
        metadataSource = o.optString("metadataSource", "anilist"),
        coverImage = o.optString("coverImage", ""),
        bannerImage = o.optString("bannerImage").takeIf { it.isNotBlank() && it != "null" },
        format = o.optString("format", "TV").takeIf { it.isNotBlank() && it != "null" },
        status = o.optString("status", "RELEASING").takeIf { it.isNotBlank() && it != "null" },
        totalEpisodes = if (o.isNull("totalEpisodes")) null else o.optInt("totalEpisodes"),
        currentEpisode = o.optInt("currentEpisode", 1),
        watchedEpisodeCount = o.optInt("watchedEpisodeCount", 0),
        userSeason = o.optInt("userSeason", 1).coerceIn(1, 99),
        userStatus = o.optString("userStatus").takeIf { it.isNotBlank() && it != "null" },
        userScore = if (o.isNull("userScore")) null else o.optInt("userScore"),
        startDate = o.optString("startDate").takeIf { it.isNotBlank() && it != "null" },
        finishDate = o.optString("finishDate").takeIf { it.isNotBlank() && it != "null" },
        airingDayOfWeek = o.optInt("airingDayOfWeek", 1).coerceIn(1, 7),
        airingTimeUnix = if (o.isNull("airingTimeUnix")) null else o.optLong("airingTimeUnix"),
        nextEpisodeNumber = if (o.isNull("nextEpisodeNumber")) null else o.optInt("nextEpisodeNumber"),
        genres = o.optString("genres", ""),
        studios = o.optString("studios", ""),
        season = o.optString("season").takeIf { it.isNotBlank() && it != "null" },
        seasonYear = if (o.isNull("seasonYear")) null else o.optInt("seasonYear"),
        score = if (o.isNull("score")) null else o.optInt("score"),
        popularity = if (o.isNull("popularity")) null else o.optInt("popularity"),
        synopsis = o.optString("synopsis").takeIf { it.isNotBlank() && it != "null" },
        countryOfOrigin = o.optString("countryOfOrigin", "JP").takeIf { it.isNotBlank() && it != "null" },
        inWatchlist = o.optBoolean("inWatchlist", false),
        isWishlist = o.optBoolean("isWishlist", false),
        isFavorite = o.optBoolean("isFavorite", false),
        isPinned = o.optBoolean("isPinned", false),
        notificationsEnabled = o.optBoolean("notificationsEnabled", true),
        isWatched = o.optBoolean("isWatched", false),
        lastWatchedTime = if (o.isNull("lastWatchedTime")) null else o.optLong("lastWatchedTime"),
        userNotes = o.optString("userNotes").takeIf { it.isNotBlank() && it != "null" },
        lastCachedAt = o.optLong("lastCachedAt", System.currentTimeMillis())
    )

    companion object {
        private const val PREFS_NAME = "anime_universe_identity_prefs"
        private const val VAULTS_DIR = "owner_vaults"
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_ACTIVE_GUEST_MODE = "active_guest_mode"
        private const val KEY_LAST_OWNER = "last_active_owner_key"

        fun userKey(userId: String): String = "user_${userId.trim()}"
        fun guestKey(guestId: String): String = "guest_${guestId.trim()}"
        fun generateGuestId(): String = "guest_${UUID.randomUUID()}"

        /**
         * Safely computes the subset of guest items that should be migrated into an authenticated account
         * without duplicating or overwriting anime already present in that account's library.
         */
        fun computeNonDuplicatingMigrationItems(
            existingAccountItems: List<AnimeEntity>,
            guestItems: List<AnimeEntity>
        ): List<AnimeEntity> {
            val existingIds = existingAccountItems.map { it.id }.toSet()
            return guestItems.filter { it.hasActiveUserState && !existingIds.contains(it.id) }
        }
    }
}
