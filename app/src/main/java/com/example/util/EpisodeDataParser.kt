package com.example.util

import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Parsed episode information representing differentiated episode metrics.
 * @param airedEpisodeCount Currently aired/released episodes.
 * @param totalEpisodes Total planned episodes if finite season/completed; null if continuous/ongoing.
 * @param nextEpisodeNumber The next episode number scheduled to air.
 * @param isFinished True if airing has completed.
 */
data class ParsedEpisodeInfo(
    val airedEpisodeCount: Int,
    val totalEpisodes: Int?,
    val nextEpisodeNumber: Int?,
    val isFinished: Boolean
)

/**
 * Utility to parse and differentiate between 'currently aired episodes' and 'total planned episodes'
 * across different remote data providers (AniList, MyAnimeList, Jikan).
 */
object EpisodeDataParser {
    /**
     * Resolves accurate current episode counts for Chinese origin anime (Donghua)
     * when AniList nextAiringEpisode schedule is missing or unpopulated.
     */
    fun resolveDonghuaAiredEpisodes(
        mediaId: Int = 0,
        title: String? = null,
        startYear: Int? = null,
        startMonth: Int? = null,
        startDay: Int? = null,
        existingAired: Int = 0,
        nowSeconds: Long = System.currentTimeMillis() / 1000
    ): Int {
        val lowerTitle = title?.lowercase() ?: ""
        // Known continuous long-running Donghua anchors (base date: Aug 20, 2026)
        val baseAug2026 = 1787184000L // Aug 20, 2026 00:00 UTC
        val weeksSinceBase = (nowSeconds - baseAug2026) / 604800L

        val anchorEp = when {
            mediaId == 130706 || lowerTitle.contains("wanmei shijie") || lowerTitle.contains("perfect world") -> 278 + weeksSinceBase.toInt()
            mediaId == 125638 || lowerTitle.contains("tunshi xingkong") || lowerTitle.contains("swallowed star") -> 212 + weeksSinceBase.toInt()
            mediaId == 162982 || lowerTitle.contains("jueshi tangmen") || lowerTitle.contains("soul land") -> 164 + weeksSinceBase.toInt()
            mediaId == 150020 || lowerTitle.contains("doupo cangqiong") || lowerTitle.contains("battle through the heavens") -> 212 + weeksSinceBase.toInt()
            mediaId == 118438 || lowerTitle.contains("wu shen zhu zai") || lowerTitle.contains("martial master") -> 522 + weeksSinceBase.toInt()
            mediaId == 176123 || lowerTitle.contains("yi nian yong heng") || lowerTitle.contains("a will eternal") -> 132 + weeksSinceBase.toInt()
            mediaId == 166532 || lowerTitle.contains("xian ni") || lowerTitle.contains("renegade immortal") -> 152 + weeksSinceBase.toInt()
            mediaId == 146116 || lowerTitle.contains("shen yin wangzuo") || lowerTitle.contains("throne of seal") -> 219 + weeksSinceBase.toInt()
            mediaId == 156324 || lowerTitle.contains("zhe tian") || lowerTitle.contains("shrouding the heavens") -> 170 + weeksSinceBase.toInt()
            mediaId == 167389 || lowerTitle.contains("zhu xian") || lowerTitle.contains("jade dynasty") -> 80 + weeksSinceBase.toInt()
            mediaId == 166258 || lowerTitle.contains("fanren xiu xian") || lowerTitle.contains("mortal's journey") -> 152 + weeksSinceBase.toInt()
            mediaId == 156323 || lowerTitle.contains("bai lian cheng shen") || lowerTitle.contains("apotheosis") -> 94 + weeksSinceBase.toInt()
            mediaId == 166531 || lowerTitle.contains("cang yuan tu") || lowerTitle.contains("demon hunter") -> 60 + weeksSinceBase.toInt()
            mediaId == 136113 || lowerTitle.contains("ni tian zhizun") || lowerTitle.contains("against the gods") -> 260 + weeksSinceBase.toInt()
            else -> 0
        }

        if (anchorEp > 0) {
            return maxOf(anchorEp, existingAired)
        }

        // Calculate based on start date if available
        if (startYear != null && startYear >= 2015) {
            try {
                val month = (startMonth ?: 1).coerceIn(1, 12)
                val day = (startDay ?: 1).coerceIn(1, 28)
                val startDate = LocalDate.of(startYear, month, day)
                val startEpoch = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
                if (nowSeconds >= startEpoch) {
                    val weeks = (nowSeconds - startEpoch) / 604800L
                    val calculated = (weeks + 1).toInt()
                    return maxOf(calculated, existingAired, 1)
                }
            } catch (_: Exception) {}
        }

        return maxOf(existingAired, 1)
    }

    /**
     * Parses episode details from AniList media data, properly differentiating
     * between 'currently aired episodes' and 'total planned episodes'.
     */
    fun parseAniListEpisodes(
        apiEpisodes: Int?,
        statusStr: String?,
        nextAiringEpisodeNumber: Int?,
        nextAiringAtUnix: Long?,
        existingAired: Int = 0,
        nowSeconds: Long = System.currentTimeMillis() / 1000,
        mediaId: Int = 0,
        title: String? = null,
        isDonghua: Boolean = false,
        startYear: Int? = null,
        startMonth: Int? = null,
        startDay: Int? = null
    ): ParsedEpisodeInfo {
        val status = statusStr?.uppercase() ?: "RELEASING"
        val isFinished = status == "FINISHED" || status == "FINISHED_AIRING" || status == "CANCELLED"
        if (isFinished) {
            val donghuaResolved = if (isDonghua || mediaId > 0) resolveDonghuaAiredEpisodes(mediaId, title, startYear, startMonth, startDay, existingAired, nowSeconds) else 0
            val total = apiEpisodes ?: maxOf(existingAired, donghuaResolved, 1)
            return ParsedEpisodeInfo(
                airedEpisodeCount = total,
                totalEpisodes = total,
                nextEpisodeNumber = total,
                isFinished = true
            )
        }
        if (status == "NOT_YET_RELEASED") {
            return ParsedEpisodeInfo(
                airedEpisodeCount = 0,
                totalEpisodes = apiEpisodes,
                nextEpisodeNumber = 1,
                isFinished = false
            )
        }

        // Currently releasing or ongoing
        val airedCount: Int
        val nextEp: Int
        val isDonghuaMedia = isDonghua || (mediaId in listOf(130706, 125638, 162982, 150020, 118438, 176123, 166532, 146116, 156324, 167389, 166258, 156323, 166531, 136113))

        if (nextAiringEpisodeNumber != null && nextAiringEpisodeNumber > 2) {
            if (nextAiringAtUnix != null && nowSeconds >= nextAiringAtUnix) {
                val diff = nowSeconds - nextAiringAtUnix
                val extraWeeks = (diff / 604800L).toInt()
                airedCount = nextAiringEpisodeNumber + extraWeeks
                nextEp = airedCount + 1
            } else {
                airedCount = nextAiringEpisodeNumber - 1
                nextEp = nextAiringEpisodeNumber
            }
        } else if (isDonghuaMedia) {
            val resolved = resolveDonghuaAiredEpisodes(mediaId, title, startYear, startMonth, startDay, existingAired, nowSeconds)
            airedCount = resolved
            nextEp = airedCount + 1
        } else if (nextAiringEpisodeNumber != null && nextAiringEpisodeNumber > 0) {
            if (nextAiringAtUnix != null && nowSeconds >= nextAiringAtUnix) {
                val diff = nowSeconds - nextAiringAtUnix
                val extraWeeks = (diff / 604800L).toInt()
                airedCount = nextAiringEpisodeNumber + extraWeeks
                nextEp = airedCount + 1
            } else {
                airedCount = nextAiringEpisodeNumber - 1
                nextEp = nextAiringEpisodeNumber
            }
        } else {
            airedCount = existingAired.coerceAtLeast(1)
            nextEp = airedCount + 1
        }

        val finalAired = if (apiEpisodes != null && apiEpisodes > 0) airedCount.coerceIn(1, apiEpisodes) else airedCount

        return ParsedEpisodeInfo(
            airedEpisodeCount = finalAired,
            totalEpisodes = apiEpisodes,
            nextEpisodeNumber = if (apiEpisodes != null && apiEpisodes > 0 && finalAired >= apiEpisodes) finalAired else nextEp,
            isFinished = false
        )
    }

    /**
     * Parses episode details from MyAnimeList (MAL) data, properly differentiating
     * between 'currently aired episodes' and 'total planned episodes'.
     */
    fun parseMalEpisodes(
        malNumEpisodes: Int?,
        malStatusStr: String?,
        userWatchedEpisodes: Int = 0,
        existingAired: Int = 0,
        malAiredEpisode: Int? = null,
        nowSeconds: Long = System.currentTimeMillis() / 1000
    ): ParsedEpisodeInfo {
        val malStatus = malStatusStr?.lowercase() ?: "currently_airing"
        val isFinished = malStatus == "finished_airing" || malStatus == "finished"
        if (isFinished) {
            val total = maxOf(malNumEpisodes ?: 0, malAiredEpisode ?: 0, existingAired, userWatchedEpisodes, 1)
            return ParsedEpisodeInfo(
                airedEpisodeCount = total,
                totalEpisodes = total,
                nextEpisodeNumber = total,
                isFinished = true
            )
        }

        val totalEps = if (malNumEpisodes != null && malNumEpisodes > 0) malNumEpisodes else null
        val rawAired = maxOf(malAiredEpisode ?: 0, existingAired, userWatchedEpisodes).coerceAtLeast(1)
        val airedCount = if (totalEps != null) rawAired.coerceIn(1, totalEps) else rawAired

        return ParsedEpisodeInfo(
            airedEpisodeCount = airedCount,
            totalEpisodes = totalEps,
            nextEpisodeNumber = if (totalEps != null && airedCount >= totalEps) airedCount else (airedCount + 1),
            isFinished = false
        )
    }

    /**
     * Maps romanized/pinyin titles to clean canonical English titles
     */
    fun getCanonicalEnglish(title: String?): String? {
        if (title.isNullOrBlank()) return null
        val lower = title.lowercase()
        return when {
            lower.contains("wanmei shijie") || lower.contains("perfect world") -> "Perfect World"
            lower.contains("tunshi xingkong") || lower.contains("swallowed star") -> "Swallowed Star"
            lower.contains("jueshi tangmen") || lower.contains("soul land") -> "Soul Land II: Peerless Tang Sect"
            lower.contains("doupo cangqiong") || lower.contains("battle through the heavens") -> "Battle Through The Heavens"
            lower.contains("wu shen zhu zai") || lower.contains("martial master") -> "Martial Master"
            lower.contains("yi nian yong heng") || lower.contains("a will eternal") -> "A Will Eternal"
            lower.contains("xian ni") || lower.contains("renegade immortal") -> "Renegade Immortal"
            lower.contains("shen yin wangzuo") || lower.contains("throne of seal") -> "Throne of Seal"
            lower.contains("zhe tian") || lower.contains("shrouding the heavens") -> "Shrouding the Heavens"
            lower.contains("zhu xian") || lower.contains("jade dynasty") -> "Jade Dynasty"
            lower.contains("fanren xiu xian") || lower.contains("mortal's journey") -> "Record of a Mortal's Journey to Immortality"
            lower.contains("bai lian cheng shen") || lower.contains("apotheosis") -> "Apotheosis"
            lower.contains("da zhu zai") || lower.contains("great ruler") -> "The Great Ruler"
            lower.contains("ni tian xie shen") || lower.contains("against the gods") -> "Against the Gods"
            lower.contains("xing chen bian") || lower.contains("stellar transformations") -> "Stellar Transformations"
            lower.contains("lian qi shi wan nian") || lower.contains("100,000 years") -> "One Hundred Thousand Years of Qi Refining"
            lower.contains("cang yuan tu") || lower.contains("demon hunter") -> "The Demon Hunter"
            lower.contains("meitantei conan") || lower.contains("detective conan") -> "Detective Conan"
            lower.contains("one piece") -> "ONE PIECE"
            else -> title
        }
    }
}
