package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.EnglishTitleMapper

@Entity(tableName = "anime")
data class AnimeEntity(
    @PrimaryKey val id: Int, // Internal / Primary Media ID
    val idMal: Int? = null, // Nullable MyAnimeList ID
    @ColumnInfo(defaultValue = "NULL") val archiveIdentifier: String? = null, // Stable Internet Archive identifier
    val titleRomaji: String, // Japanese Romaji (for JP anime)
    val titleEnglish: String? = null, // Canonical English Title
    val titleNative: String? = null, // Native Kanji/Hanzi Title
    val titlePinyin: String? = null, // Chinese Pinyin Title (for CN donghua)
    val metadataSource: String = "metadata_provider", // Metadata provenance source (e.g. "metadata_provider", "myanimelist", "local")
    val coverImage: String,
    val bannerImage: String? = null,
    val format: String? = "TV",
    val status: String? = "RELEASING", // Airing status: RELEASING, FINISHED, NOT_YET_RELEASED
    val totalEpisodes: Int? = null,
    val currentEpisode: Int = 1, // Latest released episode count
    val watchedEpisodeCount: Int = 0, // User's watched episode count
    val userStatus: String? = null, // User list status: "watching", "completed", "on_hold", "dropped", "plan_to_watch"
    val userScore: Int? = null, // User rating score
    val startDate: String? = null, // User watch start date
    val finishDate: String? = null, // User watch completion date
    val airingDayOfWeek: Int, // 1 = Monday ... 7 = Sunday
    val airingTimeUnix: Long? = null, // Next episode air date timestamp
    val nextEpisodeNumber: Int? = null,
    val genres: String = "", // Comma-separated
    val season: String? = "SPRING",
    val seasonYear: Int? = 2026,
    val score: Int? = 80, // Average community score
    val synopsis: String? = null,
    val countryOfOrigin: String? = "JP",
    val inWatchlist: Boolean = false,
    val isWishlist: Boolean = false,
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isWatched: Boolean = false,
    val lastWatchedTime: Long? = null,
    val userNotes: String? = null,
    val lastCachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculates completion percentage strictly when totalEpisodes is known.
     * Never returns a misleading percentage when totalEpisodes is unknown or null.
     */
    val completionPercentage: Float?
        get() = if (totalEpisodes != null && totalEpisodes > 0) {
            ((watchedEpisodeCount.toFloat() / totalEpisodes) * 100f).coerceIn(0f, 100f)
        } else null

    /**
     * Normalized display tracking status (Watching, Plan to Watch, Completed, On Hold, Dropped).
     */
    val trackingStatusDisplay: String
        get() = when (userStatus?.lowercase()?.trim()) {
            "watching", "current" -> "Watching"
            "completed" -> "Completed"
            "plan_to_watch", "planning" -> "Plan to Watch"
            "on_hold", "paused" -> "On Hold"
            "dropped" -> "Dropped"
            else -> if (inWatchlist) "Watching" else "Plan to Watch"
        }
    /**
     * Checks if this cached entity is fresh within a given TTL (default 30 minutes).
     */
    fun isCacheFresh(ttlMillis: Long = 30 * 60 * 1000L): Boolean {
        return (System.currentTimeMillis() - lastCachedAt) < ttlMillis
    }

    /**
     * True if title is local-only (not synced/available on MyAnimeList).
     */
    val isLocalOnly: Boolean
        get() = idMal == null

    /**
     * Returns true if this is Chinese Animation (Donghua).
     */
    val isDonghua: Boolean
        get() = countryOfOrigin.equals("CN", ignoreCase = true) ||
                !titlePinyin.isNullOrBlank() ||
                genres.contains("Donghua", ignoreCase = true) ||
                genres.contains("Chinese", ignoreCase = true)

    /**
     * Returns true if this media item is adult content (Hentai, Rx, Erotica, Ecchi).
     */
    val isAdult: Boolean
        get() = genres.contains("Hentai", ignoreCase = true) ||
                genres.contains("Ecchi", ignoreCase = true) ||
                genres.contains("Rx", ignoreCase = true) ||
                genres.contains("Erotica", ignoreCase = true) ||
                genres.contains("Adult", ignoreCase = true) ||
                format?.uppercase() == "HENTAI"

    /**
     * Resolves title based on language preference (ENGLISH, AUTO, ORIGINAL, ROMAJI_PINYIN).
     */
    fun getResolvedTitle(preference: String = "ENGLISH"): String {
        return EnglishTitleMapper.resolveTitle(
            titleEnglish = titleEnglish,
            titleRomaji = titleRomaji,
            titleNative = titleNative,
            titlePinyin = titlePinyin,
            isDonghua = isDonghua,
            preference = preference
        )
    }

    /**
     * Default title display using English primary language.
     */
    val displayTitle: String
        get() = getResolvedTitle("ENGLISH")

    /**
     * Calculated latest available released episode count.
     */
    val latestAvailableEpisode: Int
        get() = getAiredEpisodeCount()

    /**
     * Calculated unwatched available episodes count.
     */
    val unwatchedAvailable: Int
        get() = (getAiredEpisodeCount() - watchedEpisodeCount).coerceAtLeast(0)

    /**
     * Dynamically calculates how many episodes have aired as of current time.
     * Accurately handles long-running anime, weekly releases, and automatically increments as new episodes air over time.
     * Guaranteed to never revert or clamp down below the known currentEpisode and strictly capped at totalEpisodes.
     */
    fun getAiredEpisodeCount(nowSeconds: Long = System.currentTimeMillis() / 1000): Int {
        val total = totalEpisodes
        val maxKnownAired = if (total != null && total > 0) minOf(maxOf(currentEpisode, 1), total) else maxOf(currentEpisode, 1)
        val st = status?.uppercase() ?: ""
        if (st == "FINISHED" || st == "FINISHED_AIRING") {
            if (total != null && total > 0) return total
            return maxKnownAired
        }
        if (st == "NOT_YET_RELEASED") return 0

        val baseEp = maxKnownAired
        val baseTime = airingTimeUnix ?: return baseEp
        if (baseTime <= 0) return baseEp

        val totalAired = if (nowSeconds >= baseTime) {
            val diff = nowSeconds - baseTime
            val weeksPassed = (diff / 604800L).toInt()
            baseEp + 1 + weeksPassed
        } else {
            baseEp
        }

        return if (total != null && total > 0) {
            totalAired.coerceIn(1, total)
        } else {
            maxOf(totalAired, baseEp)
        }
    }

    /**
     * Dynamically calculates the next episode number that will air. Returns null if finished or no new episodes.
     */
    fun getEffectiveNextEpisodeNumber(nowSeconds: Long = System.currentTimeMillis() / 1000): Int? {
        val st = status?.uppercase() ?: ""
        if (st == "FINISHED" || st == "FINISHED_AIRING" || st == "CANCELLED" || st == "HIATUS") return null
        val aired = getAiredEpisodeCount(nowSeconds)
        if (totalEpisodes != null && totalEpisodes > 0 && aired >= totalEpisodes) return null
        return aired + 1
    }

    /**
     * Dynamically calculates the next upcoming airing time in Unix seconds.
     * Returns null if completed, cancelled, non-weekly format (e.g. OVA/Hentai/Movie without confirmed future schedule),
     * or if total planned episodes have aired. Never hallucinates arbitrary 7-day dates.
     */
    fun getEffectiveAiringTime(nowSeconds: Long = System.currentTimeMillis() / 1000): Long? {
        val baseTime = airingTimeUnix ?: return null
        if (baseTime <= 0) return null
        val st = status?.uppercase() ?: ""
        if (st == "FINISHED" || st == "FINISHED_AIRING" || st == "CANCELLED" || st == "HIATUS") return null
        if (totalEpisodes != null && totalEpisodes > 0 && getAiredEpisodeCount(nowSeconds) >= totalEpisodes) return null

        // If the confirmed schedule is in the future, return it
        if (baseTime > nowSeconds) return baseTime

        // For non-weekly formats (OVA, Special, Movie, Music) or Hentai without confirmed upcoming schedule:
        // Do NOT hallucinate a weekly 7-day recurring date!
        val isNonWeeklyFormat = format?.uppercase() in listOf("OVA", "SPECIAL", "MOVIE", "MUSIC", "ONA")
        val isAdultOrHentai = genres.contains("Hentai", ignoreCase = true)
        if (isNonWeeklyFormat || isAdultOrHentai || (st != "RELEASING" && st != "CURRENTLY_AIRING" && st != "AIRING")) {
            return null
        }

        // For actively releasing TV/Donghua series: advance weekly
        val diff = nowSeconds - baseTime
        val weeksPassed = 1 + (diff / 604800L).toInt()
        val projectedEpisode = currentEpisode + weeksPassed
        if (totalEpisodes != null && totalEpisodes > 0 && projectedEpisode > totalEpisodes) {
            return null
        }
        return baseTime + (weeksPassed * 604800L)
    }

    val hasActiveUserState: Boolean
        get() = inWatchlist || isWishlist || isFavorite || isPinned ||
                watchedEpisodeCount > 0 || !userStatus.isNullOrBlank() ||
                !userNotes.isNullOrBlank() || !archiveIdentifier.isNullOrBlank()
}
