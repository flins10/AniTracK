package com.example.data.repository

data class AnimeSeasonInfo(
    val seasonNumber: Int,
    val title: String,
    val animeId: Int,
    val idMal: Int? = null,
    val totalEpisodes: Int? = null,
    val startEpisode: Int = 1,
    val coverImage: String? = null,
    val bannerImage: String? = null,
    val format: String? = "TV",
    val isCurrentSeason: Boolean = false
) {
    val endEpisode: Int
        get() = if (totalEpisodes != null && totalEpisodes > 0) {
            startEpisode + totalEpisodes - 1
        } else {
            startEpisode + 23
        }

    fun containsEpisode(epNumber: Int): Boolean {
        return epNumber in startEpisode..endEpisode
    }

    /**
     * Converts a cumulative series episode number (e.g. Ep 37 in Season 2)
     * to the season-relative episode number (e.g. Ep 12).
     */
    fun localEpisodeNumber(cumulativeEpNumber: Int): Int {
        return (cumulativeEpNumber - startEpisode + 1).coerceAtLeast(1)
    }
}

object SeasonClassifier {
    /**
     * Automatically and correctly classifies which season an episode belongs to.
     * Prevents mixing up episodes and seasons.
     * Returns the matching AnimeSeasonInfo and the 1-based local episode number for that season.
     */
    fun classifyEpisode(
        cumulativeEpisode: Int,
        seasons: List<AnimeSeasonInfo>
    ): Pair<AnimeSeasonInfo?, Int> {
        if (seasons.isEmpty()) return null to cumulativeEpisode
        val sorted = seasons.sortedBy { it.seasonNumber }
        val found = sorted.firstOrNull { it.containsEpisode(cumulativeEpisode) }
        return if (found != null) {
            found to found.localEpisodeNumber(cumulativeEpisode)
        } else {
            val fallback = sorted.lastOrNull()
            fallback to (fallback?.localEpisodeNumber(cumulativeEpisode) ?: cumulativeEpisode)
        }
    }
}
