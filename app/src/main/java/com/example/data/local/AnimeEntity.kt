package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ProgressRules
import com.example.model.SeasonRules
import com.example.model.WatchStatus
import com.example.util.EnglishTitleMapper

@Entity(tableName = "anime")
data class AnimeEntity(
    @PrimaryKey val id: Int, // AniList Media ID
    val titleRomaji: String,
    val titleEnglish: String? = null,
    val titleNative: String? = null,
    val titlePinyin: String? = null,
    val metadataSource: String = "anilist",
    val coverImage: String,
    val bannerImage: String? = null,
    val format: String? = "TV",
    val status: String? = "RELEASING", // RELEASING, FINISHED, NOT_YET_RELEASED
    val totalEpisodes: Int? = null,
    val currentEpisode: Int = 1,
    val watchedEpisodeCount: Int = 0,
    @ColumnInfo(defaultValue = "1") val userSeason: Int = 1,
    val userStatus: String? = null, // watching, completed, plan_to_watch, on_hold, dropped
    val userScore: Int? = null, // 0..100 scale (e.g. 85 = 8.5/10)
    val startDate: String? = null,
    val finishDate: String? = null,
    val airingDayOfWeek: Int = 1, // 1 = Monday ... 7 = Sunday
    val airingTimeUnix: Long? = null,
    val nextEpisodeNumber: Int? = null,
    val genres: String = "",
    @ColumnInfo(defaultValue = "") val studios: String = "",
    val season: String? = null,
    val seasonYear: Int? = null,
    val score: Int? = null,
    val popularity: Int? = null,
    val synopsis: String? = null,
    val countryOfOrigin: String? = "JP",
    val inWatchlist: Boolean = false,
    @ColumnInfo(defaultValue = "0") val inHome: Boolean = false,
    val isWishlist: Boolean = false,
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isWatched: Boolean = false,
    val lastWatchedTime: Long? = null,
    val userNotes: String? = null,
    val lastCachedAt: Long = System.currentTimeMillis()
) {
    val completionPercentage: Float?
        get() = if (totalEpisodes != null && totalEpisodes > 0) {
            ((watchedEpisodeCount.toFloat() / totalEpisodes) * 100f).coerceIn(0f, 100f)
        } else null

    val watchStatusEnum: WatchStatus
        get() = WatchStatus.fromDbValue(
            userStatus ?: when {
                isWatched -> "completed"
                isWishlist -> "plan_to_watch"
                inWatchlist -> "watching"
                else -> "plan_to_watch"
            }
        )

    val trackingStatusDisplay: String
        get() = watchStatusEnum.label

    val effectiveSeasonNumber: Int
        get() = SeasonRules.resolveInitialSeason(userSeason, titleEnglish ?: titleRomaji, format)

    val isMovie: Boolean
        get() = format.equals("MOVIE", ignoreCase = true)

    val effectiveProgressDisplay: String
        get() {
            if (isMovie) {
                val total = totalEpisodes?.takeIf { it > 0 } ?: 1
                val clamped = ProgressRules.clampEpisodeProgress(watchedEpisodeCount, total)
                return if (clamped >= total) "Movie • Watched" else "Movie • $clamped / $total"
            }
            val clamped = ProgressRules.clampEpisodeProgress(watchedEpisodeCount, totalEpisodes)
            val epPart = if (totalEpisodes != null && totalEpisodes > 0) "$clamped / $totalEpisodes eps" else "$clamped eps"
            return if (effectiveSeasonNumber > 1) "S$effectiveSeasonNumber • $epPart" else epPart
        }

    fun isCacheFresh(ttlMillis: Long = 30 * 60 * 1000L): Boolean {
        return (System.currentTimeMillis() - lastCachedAt) < ttlMillis
    }

    val isDonghua: Boolean
        get() = countryOfOrigin.equals("CN", ignoreCase = true) ||
            !titlePinyin.isNullOrBlank() ||
            genres.contains("Donghua", ignoreCase = true)

    val isAdult: Boolean
        get() = genres.contains("Hentai", ignoreCase = true) ||
            genres.contains("Ecchi", ignoreCase = true) ||
            genres.contains("Erotica", ignoreCase = true) ||
            genres.contains("Adult", ignoreCase = true) ||
            format?.uppercase() == "HENTAI"

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

    val displayTitle: String
        get() = getResolvedTitle("ENGLISH")

    val latestAvailableEpisode: Int
        get() = getAiredEpisodeCount()

    val unwatchedAvailable: Int
        get() = (getAiredEpisodeCount() - watchedEpisodeCount).coerceAtLeast(0)

    fun getAiredEpisodeCount(nowSeconds: Long = System.currentTimeMillis() / 1000): Int {
        val total = totalEpisodes
        val st = status?.uppercase()?.trim() ?: ""
        if (st == "NOT_YET_RELEASED") return 0
        if (isMovie) {
            return if (st == "FINISHED" || st == "FINISHED_AIRING") {
                total?.takeIf { it > 0 } ?: 1
            } else {
                currentEpisode.coerceIn(0, total?.takeIf { it > 0 } ?: 1)
            }
        }
        if (st == "FINISHED" || st == "FINISHED_AIRING") {
            if (total != null && total > 0) return total
            return currentEpisode.coerceAtLeast(1)
        }

        val baseEp = if (total != null && total > 0) {
            currentEpisode.coerceIn(0, total)
        } else {
            currentEpisode.coerceAtLeast(0)
        }
        val baseTime = airingTimeUnix ?: return baseEp
        if (baseTime <= 0) return baseEp

        val isNonWeeklyFormat = format?.uppercase() in listOf("OVA", "SPECIAL", "MOVIE", "MUSIC")
        if (isNonWeeklyFormat || (st != "RELEASING" && st != "CURRENTLY_AIRING" && st != "AIRING")) {
            return baseEp
        }

        val totalAired = if (nowSeconds >= baseTime) {
            val diff = nowSeconds - baseTime
            val weeksPassed = (diff / 604800L).toInt().coerceAtMost(12)
            baseEp + 1 + weeksPassed
        } else {
            baseEp
        }

        return if (total != null && total > 0) {
            totalAired.coerceIn(0, total)
        } else {
            maxOf(totalAired, baseEp)
        }
    }

    fun getEffectiveNextEpisodeNumber(nowSeconds: Long = System.currentTimeMillis() / 1000): Int? {
        val st = status?.uppercase()?.trim() ?: ""
        if (st == "FINISHED" || st == "FINISHED_AIRING" || st == "CANCELLED" || st == "HIATUS") return null
        if (isMovie) {
            return if (st == "NOT_YET_RELEASED") 1 else null
        }
        val aired = getAiredEpisodeCount(nowSeconds)
        if (totalEpisodes != null && totalEpisodes > 0 && aired >= totalEpisodes) return null
        return aired + 1
    }

    fun getEffectiveAiringTime(nowSeconds: Long = System.currentTimeMillis() / 1000): Long? {
        val baseTime = airingTimeUnix ?: return null
        if (baseTime <= 0) return null
        val st = status?.uppercase()?.trim() ?: ""
        if (st == "FINISHED" || st == "FINISHED_AIRING" || st == "CANCELLED" || st == "HIATUS") return null
        if (totalEpisodes != null && totalEpisodes > 0 && getAiredEpisodeCount(nowSeconds) >= totalEpisodes) return null
        if (baseTime > nowSeconds) return baseTime

        val isNonWeeklyFormat = format?.uppercase() in listOf("OVA", "SPECIAL", "MOVIE", "MUSIC")
        val isAdultOrHentai = genres.contains("Hentai", ignoreCase = true)
        if (isNonWeeklyFormat || isAdultOrHentai || (st != "RELEASING" && st != "CURRENTLY_AIRING" && st != "AIRING")) {
            return null
        }

        val diff = nowSeconds - baseTime
        val weeksPassed = 1 + (diff / 604800L).toInt()
        if (weeksPassed > 12) return null
        val projectedEpisode = currentEpisode + weeksPassed
        if (totalEpisodes != null && totalEpisodes > 0 && projectedEpisode > totalEpisodes) {
            return null
        }
        return baseTime + (weeksPassed * 604800L)
    }

    val hasActiveUserState: Boolean
        get() = inWatchlist || inHome || isWishlist || isFavorite || isPinned ||
            watchedEpisodeCount > 0 || !userStatus.isNullOrBlank() ||
            !userNotes.isNullOrBlank()
}
