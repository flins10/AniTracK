package com.example.data.repository

import com.example.BuildConfig
import com.example.data.remote.YouTubeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.regex.Pattern

enum class YouTubeVideoType {
    TRAILER,
    FULL_EPISODE,
    CLIP,
    OPENING_ENDING,
    GENERAL
}

data class YouTubeSearchLink(
    val title: String,
    val query: String,
    val description: String,
    val category: String,
    val webSearchUrl: String = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")
)

data class YouTubeVideo(
    val id: String,
    val title: String,
    val description: String,
    val channelTitle: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val durationFormatted: String?,
    val isVerifiedOfficial: Boolean,
    val videoType: YouTubeVideoType,
    val sourceBadge: String = "YouTube Data API",
    val watchUrl: String = "https://www.youtube.com/watch?v=$id",
    val embedUrl: String = "https://www.youtube.com/embed/$id?autoplay=1&enablejsapi=1&playsinline=1"
)

sealed class YouTubeSearchResultState {
    data class Success(val videos: List<YouTubeVideo>, val nextPageToken: String?) : YouTubeSearchResultState()
    data class Keyless(val searchLinks: List<YouTubeSearchLink>, val directTrailer: YouTubeVideo? = null) : YouTubeSearchResultState()
    data class Error(val message: String, val isQuotaExceeded: Boolean = false, val isMissingKey: Boolean = false) : YouTubeSearchResultState()
    object Empty : YouTubeSearchResultState()
}

class YouTubeRepository(
    private val apiService: YouTubeApiService = YouTubeApiService.create()
) {
    // Official licensed anime distributors on YouTube
    private val officialDistributors = setOf(
        "muse asia", "muse india", "muse dash", "ani-one asia", "ani-one",
        "crunchyroll", "crunchyroll collection", "toho animation", "kadokawa anime",
        "anime log", "tms anime", "pony canyon", "aniplex", "aniplex usa",
        "twin engine", "king records", "warner bros. japan anime", "bilibili",
        "bilibili global", "netflix anime", "sentai", "sentai filmworks",
        "retrocrush", "viz media", "funimation"
    )

    private var optionalApiEnabled: Boolean = false
    private var runtimeApiKey: String? = null

    fun setOptionalApiEnabled(enabled: Boolean) {
        optionalApiEnabled = enabled
    }

    fun isOptionalApiEnabled(): Boolean = optionalApiEnabled

    fun setRuntimeApiKey(key: String?) {
        runtimeApiKey = key?.trim()?.takeIf { it.isNotBlank() }
    }

    fun getApiKey(): String {
        if (!optionalApiEnabled) return ""
        val runtime = runtimeApiKey
        if (!runtime.isNullOrBlank()) return runtime
        return try {
            val field = BuildConfig::class.java.getField("YOUTUBE_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun isApiKeyConfigured(): Boolean {
        if (!optionalApiEnabled) return false
        val key = getApiKey()
        return key.isNotBlank() && key != "YOUR_YOUTUBE_API_KEY" && key != "MY_YOUTUBE_API_KEY"
    }

    fun generateKeylessSearchLinks(rawQuery: String): List<YouTubeSearchLink> {
        val clean = rawQuery.replace(Regex("(?i)\\b(trailer|official|anime|search)\\b"), "").trim()
        val base = if (clean.isNotBlank()) clean else rawQuery.trim()
        return listOf(
            YouTubeSearchLink(
                title = "Official Trailers & PVs",
                query = "$base official trailer pv",
                description = "Watch official promotional videos, teasers, and trailers on YouTube",
                category = "Trailers"
            ),
            YouTubeSearchLink(
                title = "Episodes & Legal Streaming",
                query = "$base full episode english sub",
                description = "Search officially distributed full episodes and legal streaming broadcasts",
                category = "Episodes"
            ),
            YouTubeSearchLink(
                title = "Opening & Ending Themes (OST)",
                query = "$base opening ending theme song ost",
                description = "Listen to creditless OP, ED themes, and licensed original soundtracks",
                category = "Music"
            ),
            YouTubeSearchLink(
                title = "Clips, Fights & Highlights",
                query = "$base anime fight scene clip highlights",
                description = "Watch high-definition clips and iconic fight scenes",
                category = "Clips"
            ),
            YouTubeSearchLink(
                title = "Official Distributors (Muse Asia & Ani-One)",
                query = "$base Muse Asia Ani-One Crunchyroll",
                description = "Explore authorized distributors and official animation channels",
                category = "Distributors"
            )
        )
    }

    suspend fun searchAnimeVideos(
        query: String,
        regionCode: String = "IN",
        maxResults: Int = 15,
        pageToken: String? = null
    ): YouTubeSearchResultState = withContext(Dispatchers.IO) {
        if (!optionalApiEnabled) {
            // Keyless mode is active by default. Return structured keyless search links without error.
            return@withContext YouTubeSearchResultState.Keyless(generateKeylessSearchLinks(query))
        }

        val apiKey = getApiKey()
        if (!isApiKeyConfigured()) {
            return@withContext YouTubeSearchResultState.Error(
                message = "Optional YouTube Data API is enabled, but no API key was configured. Please enter a key in Settings or turn off optional API to use Keyless Mode.",
                isMissingKey = true
            )
        }

        try {
            val response = apiService.searchVideos(
                query = query,
                regionCode = regionCode,
                maxResults = maxResults,
                pageToken = pageToken,
                apiKey = apiKey
            )

            if (response.isSuccessful) {
                val searchBody = response.body()
                val items = searchBody?.items.orEmpty()
                if (items.isEmpty()) {
                    return@withContext YouTubeSearchResultState.Empty
                }

                val videoIds = items.mapNotNull { it.id?.videoId }.filter { it.isNotBlank() }
                if (videoIds.isEmpty()) {
                    return@withContext YouTubeSearchResultState.Empty
                }

                // Retrieve video details including ISO duration
                val detailsMap = try {
                    val detailsResponse = apiService.getVideoDetails(
                        ids = videoIds.joinToString(","),
                        apiKey = apiKey
                    )
                    detailsResponse.body()?.items?.associateBy { it.id.orEmpty() } ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }

                val videos = items.mapNotNull { item ->
                    val videoId = item.id?.videoId ?: return@mapNotNull null
                    val snippet = item.snippet
                    val title = cleanHtmlEntities(snippet?.title.orEmpty())
                    val description = snippet?.description.orEmpty()
                    val channelTitle = cleanHtmlEntities(snippet?.channelTitle.orEmpty())
                    val publishedAt = snippet?.publishedAt.orEmpty().take(10)
                    val thumbnail = snippet?.thumbnails?.high?.url
                        ?: snippet?.thumbnails?.medium?.url
                        ?: snippet?.thumbnails?.default?.url
                        ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                    val durationIso = detailsMap[videoId]?.contentDetails?.duration
                    val durationFormatted = parseIsoDuration(durationIso)

                    val isOfficial = isOfficialDistributor(channelTitle)
                    val type = classifyVideoType(title, description, durationIso)

                    YouTubeVideo(
                        id = videoId,
                        title = title,
                        description = description,
                        channelTitle = channelTitle,
                        publishedAt = publishedAt,
                        thumbnailUrl = thumbnail,
                        durationFormatted = durationFormatted,
                        isVerifiedOfficial = isOfficial,
                        videoType = type
                    )
                }

                // Sort: prioritize verified official distributors, then trailers and episodes
                val sortedVideos = videos.sortedWith(
                    compareByDescending<YouTubeVideo> { it.isVerifiedOfficial }
                        .thenBy { it.videoType == YouTubeVideoType.GENERAL }
                )

                YouTubeSearchResultState.Success(sortedVideos, searchBody?.nextPageToken)
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string().orEmpty()
                val isQuota = code == 403 && (errorBody.contains("quotaExceeded", ignoreCase = true) || errorBody.contains("rateLimitExceeded", ignoreCase = true))

                val msg = when {
                    isQuota -> "YouTube Data API daily quota limit reached. Please check Google Cloud Console quota or wait for daily reset."
                    code == 403 -> "YouTube API key unauthorized or API is not enabled in Google Cloud project."
                    else -> "YouTube API request failed (HTTP $code)."
                }
                YouTubeSearchResultState.Error(msg, isQuotaExceeded = isQuota)
            }
        } catch (e: Exception) {
            YouTubeSearchResultState.Error("Network error connecting to YouTube: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun isOfficialDistributor(channelTitle: String): Boolean {
        val lower = channelTitle.lowercase()
        return officialDistributors.any { lower.contains(it) }
    }

    private fun classifyVideoType(title: String, description: String, durationIso: String?): YouTubeVideoType {
        val text = "$title $description".lowercase()
        return when {
            text.contains("official trailer") || text.contains("pv ") || text.contains("trailer") || text.contains("teaser") -> YouTubeVideoType.TRAILER
            text.contains("full episode") || text.contains("episode ") || text.contains("ep ") || text.contains("ep.") -> YouTubeVideoType.FULL_EPISODE
            text.contains("opening") || text.contains("ending") || text.contains(" ost") || text.contains("theme song") || text.contains("op ") || text.contains("ed ") -> YouTubeVideoType.OPENING_ENDING
            text.contains("clip") || text.contains("scene") || text.contains("highlight") -> YouTubeVideoType.CLIP
            else -> {
                // If duration is longer than 15 minutes, likely full episode
                if (durationIso != null && isLongDuration(durationIso)) YouTubeVideoType.FULL_EPISODE
                else YouTubeVideoType.GENERAL
            }
        }
    }

    private fun isLongDuration(iso: String): Boolean {
        // e.g. PT23M45S
        val matcher = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?").matcher(iso)
        if (matcher.find()) {
            val hours = matcher.group(1)?.toIntOrNull() ?: 0
            val minutes = matcher.group(2)?.toIntOrNull() ?: 0
            return hours > 0 || minutes >= 15
        }
        return false
    }

    fun parseIsoDuration(isoDuration: String?): String? {
        if (isoDuration.isNullOrBlank()) return null
        val matcher = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?").matcher(isoDuration)
        if (!matcher.find()) return null

        val hours = matcher.group(1)?.toIntOrNull() ?: 0
        val minutes = matcher.group(2)?.toIntOrNull() ?: 0
        val seconds = matcher.group(3)?.toIntOrNull() ?: 0

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
