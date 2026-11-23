package com.example.util

import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Parsed episode information representing differentiated episode metrics.
 * @param airedEpisodeCount Currently aired/released episodes.
 * @param totalEpisodes Total planned episodes if finite season/completed; null if continuous/ongoing.
 * @param nextEpisodeNumber The next episode number scheduled to air (null if finished/movie released).
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
 * for AniList anime & donghua metadata, including proper Movie / OVA / Special / ongoing Donghua rules.
 */
object EpisodeDataParser {

    private val NON_EPISODIC_FORMATS = setOf("MOVIE", "OVA", "SPECIAL", "MUSIC")

    // Known continuous long-running Donghua matched STRICTLY by AniList mediaId
    // so movies, specials, and Season 1 entries of the same franchise are NEVER hijacked.
    private val CONTINUOUS_DONGHUA_BASE_EPOCH = 1787184000L // Aug 20, 2026 00:00 UTC
    private val CONTINUOUS_DONGHUA_ANCHORS_BY_ID = mapOf(
        130706 to 278, // Perfect World (Wanmei Shijie)
        125638 to 212, // Swallowed Star (Tunshi Xingkong)
        162982 to 164, // Soul Land II: Peerless Tang Clan (Jueshi Tangmen)
        150020 to 212, // Battle Through the Heavens Nianfan (Doupo Cangqiong)
        118438 to 522, // Martial Master (Wu Shen Zhu Zai)
        176123 to 132, // A Will Eternal S3/Ongoing (Yi Nian Yong Heng)
        166532 to 152, // Renegade Immortal (Xian Ni)
        146116 to 219, // Throne of Seal (Shen Yin Wangzuo)
        156324 to 170, // Shrouding the Heavens (Zhe Tian)
        167389 to 80,  // Jade Dynasty (Zhu Xian)
        166258 to 152, // Record of a Mortal's Journey to Immortality (Fanren Xiu Xian)
        156323 to 94,  // Apotheosis (Bai Lian Cheng Shen)
        166531 to 60,  // The Demon Hunter (Cang Yuan Tu)
        136113 to 260  // Against the Sky Supreme (Ni Tian Zhizun)
    )

    /**
     * Resolves current aired episode counts for actively RELEASING Chinese origin anime (Donghua)
     * ONLY when AniList nextAiringEpisode schedule is missing or unpopulated.
     */
    fun resolveDonghuaAiredEpisodes(
        mediaId: Int = 0,
        title: String? = null,
        startYear: Int? = null,
        startMonth: Int? = null,
        startDay: Int? = null,
        existingAired: Int = 0,
        nowSeconds: Long = System.currentTimeMillis() / 1000,
        apiEpisodes: Int? = null,
        format: String? = null
    ): Int {
        val normFormat = format?.uppercase()?.trim().orEmpty()
        if (normFormat in NON_EPISODIC_FORMATS) {
            return apiEpisodes?.takeIf { it > 0 } ?: if (normFormat == "MOVIE") 1 else maxOf(existingAired, 1)
        }

        // Strict ID-only lookup for continuous long-running Donghua anchors
        val anchorBase = CONTINUOUS_DONGHUA_ANCHORS_BY_ID[mediaId]
        if (anchorBase != null) {
            val weeksSinceBase = ((nowSeconds - CONTINUOUS_DONGHUA_BASE_EPOCH) / 604800L).toInt()
            val projected = (anchorBase + weeksSinceBase).coerceAtLeast(1)
            return maxOf(projected, existingAired)
        }

        // Safe startDate calculation ONLY for recent ongoing series (started within the last 18 months)
        // so hiatus/multi-year entries never get inflated into hundreds of fake episodes.
        if (startYear != null && startYear >= 2018) {
            try {
                val month = (startMonth ?: 1).coerceIn(1, 12)
                val day = (startDay ?: 1).coerceIn(1, 28)
                val startDate = LocalDate.of(startYear, month, day)
                val startEpoch = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
                if (nowSeconds >= startEpoch) {
                    val elapsedWeeks = ((nowSeconds - startEpoch) / 604800L).toInt()
                    if (elapsedWeeks in 0..78) {
                        val calculated = elapsedWeeks + 1
                        val bounded = if (apiEpisodes != null && apiEpisodes > 0) {
                            calculated.coerceIn(1, apiEpisodes)
                        } else {
                            calculated
                        }
                        return maxOf(bounded, existingAired, 1)
                    }
                } else {
                    // Start date is still in the future -> 0 aired so far
                    return existingAired.coerceAtLeast(0)
                }
            } catch (_: Exception) {}
        }

        return existingAired.coerceAtLeast(1)
    }

    /**
     * Parses episode details from AniList media data, properly differentiating
     * between Movies, Finished shows, Not-Yet-Released entries, and Ongoing Anime/Donghua.
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
        startDay: Int? = null,
        format: String? = null
    ): ParsedEpisodeInfo {
        val status = statusStr?.uppercase()?.trim() ?: "RELEASING"
        val normFormat = format?.uppercase()?.trim().orEmpty()
        val cleanApiEps = apiEpisodes?.takeIf { it > 0 }
        val isMovie = normFormat == "MOVIE"

        // 1. NOT_YET_RELEASED (Movies & Series alike: 0 episodes have aired)
        if (status == "NOT_YET_RELEASED") {
            return ParsedEpisodeInfo(
                airedEpisodeCount = 0,
                totalEpisodes = cleanApiEps ?: if (isMovie) 1 else null,
                nextEpisodeNumber = 1,
                isFinished = false
            )
        }

        // 2. FINISHED / CANCELLED (never extrapolate weeks from startDate!)
        val isFinished = status == "FINISHED" || status == "FINISHED_AIRING" || status == "CANCELLED"
        if (isFinished) {
            val total = cleanApiEps ?: if (isMovie) 1 else existingAired.takeIf { it > 0 }
            val aired = total ?: maxOf(existingAired, 1)
            return ParsedEpisodeInfo(
                airedEpisodeCount = aired,
                totalEpisodes = total,
                nextEpisodeNumber = null,
                isFinished = true
            )
        }

        // 3. MOVIE that is RELEASING / HIATUS
        if (isMovie) {
            val total = cleanApiEps ?: 1
            return ParsedEpisodeInfo(
                airedEpisodeCount = total,
                totalEpisodes = total,
                nextEpisodeNumber = null,
                isFinished = false
            )
        }

        // 4. HIATUS (do not project new weekly episodes while on hiatus)
        if (status == "HIATUS") {
            val aired = when {
                nextAiringEpisodeNumber != null && nextAiringEpisodeNumber >= 1 ->
                    (nextAiringEpisodeNumber - 1).coerceAtLeast(existingAired)
                else -> existingAired.coerceAtLeast(1)
            }
            val boundedAired = if (cleanApiEps != null) aired.coerceAtMost(cleanApiEps) else aired
            return ParsedEpisodeInfo(
                airedEpisodeCount = boundedAired,
                totalEpisodes = cleanApiEps,
                nextEpisodeNumber = if (cleanApiEps != null && boundedAired >= cleanApiEps) null else boundedAired + 1,
                isFinished = false
            )
        }

        // 5. Actively RELEASING / ONGOING anime or donghua
        val airedCount: Int
        val nextEp: Int?
        val isKnownAnchorDonghua = CONTINUOUS_DONGHUA_ANCHORS_BY_ID.containsKey(mediaId)

        if (nextAiringEpisodeNumber != null && nextAiringEpisodeNumber >= 1) {
            // ALWAYS trust AniList's official nextAiringEpisode schedule (including Ep 1 and Ep 2!)
            if (nextAiringAtUnix != null && nowSeconds >= nextAiringAtUnix) {
                val diff = nowSeconds - nextAiringAtUnix
                val extraWeeks = (diff / 604800L).toInt().coerceAtMost(12)
                airedCount = nextAiringEpisodeNumber + extraWeeks
                nextEp = airedCount + 1
            } else {
                airedCount = (nextAiringEpisodeNumber - 1).coerceAtLeast(0)
                nextEp = nextAiringEpisodeNumber
            }
        } else if (isDonghua || isKnownAnchorDonghua) {
            airedCount = resolveDonghuaAiredEpisodes(
                mediaId = mediaId,
                title = title,
                startYear = startYear,
                startMonth = startMonth,
                startDay = startDay,
                existingAired = existingAired,
                nowSeconds = nowSeconds,
                apiEpisodes = cleanApiEps,
                format = normFormat
            )
            nextEp = airedCount + 1
        } else {
            airedCount = existingAired.coerceAtLeast(0)
            nextEp = airedCount + 1
        }

        // For ongoing continuous Donghua where AniList still has a stale/lower planned episode count
        // (e.g. episodes=52 while Ep 78 has already aired), do NOT artificially clamp or cap totalEpisodes!
        val isStaleOngoingCap = cleanApiEps != null && airedCount > cleanApiEps && (isDonghua || isKnownAnchorDonghua)
        val effectiveTotal = if (isStaleOngoingCap) null else cleanApiEps
        val finalAired = if (effectiveTotal != null) airedCount.coerceIn(0, effectiveTotal) else airedCount.coerceAtLeast(0)
        val finalNext = when {
            effectiveTotal != null && finalAired >= effectiveTotal -> null
            else -> nextEp ?: (finalAired + 1)
        }

        return ParsedEpisodeInfo(
            airedEpisodeCount = finalAired,
            totalEpisodes = effectiveTotal,
            nextEpisodeNumber = finalNext,
            isFinished = false
        )
    }

    /**
     * Maps romanized/pinyin titles to clean canonical English titles
     */
    fun getCanonicalEnglish(title: String?): String? {
        if (title.isNullOrBlank()) return null
        return EnglishTitleMapper.resolve(
            titleEnglish = null,
            titleRomaji = title,
            synonyms = null,
            isDonghua = false
        ) ?: title
    }
}
