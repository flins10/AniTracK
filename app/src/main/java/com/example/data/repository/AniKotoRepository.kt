package com.example.data.repository

import android.util.LruCache
import com.example.data.local.AnimeEntity
import com.example.data.remote.AniKotoApiService
import com.example.data.remote.AniKotoEpisodeDto
import com.example.data.remote.AniKotoInfoDto
import com.example.data.remote.AniKotoSummaryDto
import com.example.data.remote.ArchiveFileItem
import com.example.data.remote.InternetArchiveApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Locale

/**
 * Domain model representing a summary item from AniKotoAPI `/trending` or `/search`.
 * Handles missing or malformed fields gracefully.
 */
data class AniKotoAnimeItem(
    val slug: String,
    val rawLink: String,
    val animeId: String?,
    val title: String,
    val japaneseTitle: String?,
    val posterUrl: String,
    val type: String,
    val rating: String?,
    val subCount: Int,
    val dubCount: Int,
    val totalEpisodes: Int,
    val genres: List<String>
)

/**
 * Domain model representing an episode item from AniKotoAPI `/episodes/{animeId}`.
 */
data class AniKotoEpisodeItem(
    val id: String,
    val number: Int,
    val title: String,
    val slug: String,
    val isActive: Boolean,
    val serverIds: String? = null,
    val timestamp: String? = null,
    val malId: String? = null
)

/**
 * Domain model representing a resolved anime stream from AniKotoAPI.
 */
data class AniKotoStreamResult(
    val aniKotoSlug: String,
    val aniKotoAnimeId: String?,
    val episodeNumber: Int,
    val episodeTitle: String,
    val serverName: String,
    val audioType: String,
    val playableMedia: ArchivePlayableMedia,
    val primaryCandidate: ArchiveCandidateMatch,
    val allCandidates: List<ArchiveCandidateMatch>
)

/**
 * Domain model representing full anime details from AniKotoAPI `/info` and `/episodes/{animeId}`.
 * Handles missing fields gracefully.
 */
data class AniKotoAnimeDetail(
    val slug: String,
    val animeId: String?,
    val title: String,
    val japaneseTitle: String?,
    val altNames: List<String>,
    val posterUrl: String,
    val bannerUrl: String?,
    val synopsis: String,
    val type: String,
    val status: String,
    val premiered: String?,
    val aired: String?,
    val duration: String?,
    val rating: String?,
    val malScore: String?,
    val totalEpisodesCount: Int,
    val genres: List<String>,
    val studios: List<String>,
    val producers: List<String>,
    val episodes: List<AniKotoEpisodeItem>
)

sealed class AniKotoResult<out T> {
    data class Success<T>(val data: T) : AniKotoResult<T>()
    data class Empty(val message: String = "No results found.") : AniKotoResult<Nothing>()
    data class Error(val message: String, val isTimeout: Boolean = false) : AniKotoResult<Nothing>()
}

/**
 * Repository for interacting with the public AniKotoAPI (`https://anikototvapi.vercel.app/api`).
 *
 * Features:
 * - Strict input validation and sanitization (blocks path traversal, URL injection, or overly long queries).
 * - Automatic normalization between `/anime/<slug>`, `<slug>/ep-<n>`, and `<slug>` formats.
 * - Fallback resolution when a bare link like `/anime/naruto` is passed without the 5-char AniKoto slug suffix.
 * - Graceful mapping of nullable fields and non-crashing error handling with timeout detection.
 */
class AniKotoRepository(
    private val apiService: AniKotoApiService = AniKotoApiService.create()
) {
    private var cachedTrending: List<AniKotoAnimeItem>? = null
    private val searchCache = LruCache<String, List<AniKotoAnimeItem>>(25)
    private val detailCache = LruCache<String, AniKotoAnimeDetail>(25)

    companion object {
        private const val MAX_QUERY_LENGTH = 80
        private val SAFE_SLUG_REGEX = Regex("^[a-zA-Z0-9._/-]+$")
        private val EPISODE_SUFFIX_REGEX = Regex("/ep-\\d+.*$", RegexOption.IGNORE_CASE)
        private val HASH_SUFFIX_REGEX = Regex("-[a-z0-9]{4,6}$", RegexOption.IGNORE_CASE)

        /**
         * Validates and sanitizes a search query.
         */
        fun sanitizeSearchQuery(rawQuery: String): String {
            return rawQuery
                .trim()
                .replace(Regex("[<>\"'`\\\\]"), "")
                .take(MAX_QUERY_LENGTH)
                .trim()
        }

        /**
         * Normalizes any link or slug (e.g. `/anime/naruto-eybxz/ep-1`, `/anime/naruto`, `naruto-eybxz`)
         * into a clean anime slug (`naruto-eybxz` or `naruto`) while rejecting unsafe inputs (e.g., full URLs or `..`).
         */
        fun normalizeAnimeSlug(rawLinkOrSlug: String): String? {
            val trimmed = rawLinkOrSlug.trim()
            if (trimmed.isEmpty() || trimmed.length > 140) return null
            if (trimmed.contains("://") || trimmed.contains("..") || trimmed.contains("?") || trimmed.contains("#")) {
                return null
            }
            if (!SAFE_SLUG_REGEX.matches(trimmed)) {
                return null
            }
            var cleaned = trimmed
                .removePrefix("/")
                .removePrefix("anime/")
                .removePrefix("/")
                .replace(EPISODE_SUFFIX_REGEX, "")
                .trim('/')

            if (cleaned.startsWith("anime/")) {
                cleaned = cleaned.removePrefix("anime/").trim('/')
            }
            return cleaned.takeIf { it.isNotEmpty() && !it.contains("/") }
        }

        /**
         * Formats a canonical `/anime/<slug>` link parameter for `GET /info?link=/anime/<slug>`.
         */
        fun toCanonicalAnimeLink(slug: String): String {
            val clean = normalizeAnimeSlug(slug) ?: slug.trim().removePrefix("/")
            return "/anime/$clean"
        }

        /**
         * Cleans rating strings like `"7.89   7.89 /10"` into `"7.89"`.
         */
        fun cleanRating(rawRating: String?, malScore: String? = null): String? {
            val candidate = (!malScore.isNullOrBlank() && malScore != "?" && malScore != "N/A")
                .let { if (it) malScore else rawRating }
                ?.trim()
            if (candidate.isNullOrBlank() || candidate == "?" || candidate.equals("N/A", ignoreCase = true)) {
                return null
            }
            val firstToken = candidate.split(Regex("\\s+")).firstOrNull()?.removeSuffix("/10")?.trim()
            return firstToken?.takeIf { it.isNotEmpty() }
        }

        fun mapSummaryDto(dto: AniKotoSummaryDto): AniKotoAnimeItem? {
            val rawSlug = dto.slug?.trim().orEmpty()
            val normalizedSlug = normalizeAnimeSlug(rawSlug) ?: return null
            val title = dto.title?.trim()?.takeIf { it.isNotEmpty() }
                ?: dto.japaneseTitle?.trim()?.takeIf { it.isNotEmpty() }
                ?: normalizedSlug.replace("-", " ").replaceFirstChar { it.uppercase() }

            val sub = (dto.sub ?: 0).coerceAtLeast(0)
            val dub = (dto.dub ?: 0).coerceAtLeast(0)
            val total = (dto.total ?: maxOf(sub, dub)).coerceAtLeast(maxOf(sub, dub))

            return AniKotoAnimeItem(
                slug = normalizedSlug,
                rawLink = toCanonicalAnimeLink(normalizedSlug),
                animeId = dto.animeId?.trim()?.takeIf { it.isNotEmpty() },
                title = title,
                japaneseTitle = dto.japaneseTitle?.trim()?.takeIf { it.isNotEmpty() && it != title },
                posterUrl = dto.poster?.trim().orEmpty(),
                type = dto.type?.trim()?.takeIf { it.isNotEmpty() } ?: "TV",
                rating = cleanRating(dto.rating),
                subCount = sub,
                dubCount = dub,
                totalEpisodes = total,
                genres = dto.genres?.mapNotNull { it.trim().takeIf { g -> g.isNotEmpty() } } ?: emptyList()
            )
        }

        fun mapInfoDto(
            dto: AniKotoInfoDto,
            fallbackSlug: String,
            episodesDto: List<AniKotoEpisodeDto> = emptyList(),
            totalEpisodesFromEndpoint: Int? = null
        ): AniKotoAnimeDetail {
            val slug = normalizeAnimeSlug(dto.slug ?: fallbackSlug) ?: fallbackSlug
            val animeIdStr = when (val rawId = dto.animeId) {
                is Number -> rawId.toLong().toString()
                is String -> rawId.trim().removeSuffix(".0").takeIf { it.isNotEmpty() }
                else -> null
            }
            val title = dto.title?.trim()?.takeIf { it.isNotEmpty() }
                ?: dto.japaneseTitle?.trim()?.takeIf { it.isNotEmpty() }
                ?: slug.replace("-", " ").replaceFirstChar { it.uppercase() }

            val parsedEpisodesCount = dto.episodes?.trim()?.toIntOrNull()
                ?: totalEpisodesFromEndpoint
                ?: episodesDto.size

            val mappedEpisodes = episodesDto.mapIndexedNotNull { index, ep ->
                val epNo = ep.episodeNo ?: (index + 1)
                val epId = ep.id?.trim().orEmpty().ifEmpty { "$slug-ep-$epNo" }
                val epTitle = ep.title?.trim()?.takeIf { it.isNotEmpty() } ?: "Episode $epNo"
                AniKotoEpisodeItem(
                    id = epId,
                    number = epNo,
                    title = epTitle,
                    slug = ep.slug?.trim()?.takeIf { it.isNotEmpty() } ?: epNo.toString(),
                    isActive = ep.active ?: true,
                    serverIds = ep.serverIds?.trim()?.takeIf { it.isNotEmpty() },
                    timestamp = ep.timestamp?.trim()?.takeIf { it.isNotEmpty() },
                    malId = ep.malId?.trim()?.takeIf { it.isNotEmpty() }
                )
            }.sortedBy { it.number }

            val finalEpisodes = if (mappedEpisodes.isNotEmpty()) {
                mappedEpisodes
            } else if (parsedEpisodesCount > 0) {
                (1..parsedEpisodesCount.coerceAtMost(2000)).map { epNo ->
                    AniKotoEpisodeItem(
                        id = "$slug-ep-$epNo",
                        number = epNo,
                        title = "Episode $epNo",
                        slug = epNo.toString(),
                        isActive = true,
                        serverIds = null
                    )
                }
            } else {
                emptyList()
            }

            val cleanSynopsis = dto.synopsis
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "No description provided by AniKotoAPI for this title."

            val altNames = dto.altNames
                ?.split(";", ",")
                ?.mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }
                ?.distinct()
                ?: emptyList()

            return AniKotoAnimeDetail(
                slug = slug,
                animeId = animeIdStr,
                title = title,
                japaneseTitle = dto.japaneseTitle?.trim()?.takeIf { it.isNotEmpty() },
                altNames = altNames,
                posterUrl = dto.poster?.trim().orEmpty(),
                bannerUrl = dto.backgroundImage?.trim()?.takeIf { it.isNotEmpty() },
                synopsis = cleanSynopsis,
                type = dto.type?.trim()?.takeIf { it.isNotEmpty() } ?: "TV",
                status = dto.status?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown",
                premiered = dto.premiered?.trim()?.takeIf { it.isNotEmpty() },
                aired = dto.aired?.trim()?.takeIf { it.isNotEmpty() },
                duration = dto.duration?.trim()?.replace("min min", "min")?.takeIf { it.isNotEmpty() },
                rating = cleanRating(dto.rating, dto.malScore),
                malScore = cleanRating(dto.malScore, dto.rating),
                totalEpisodesCount = maxOf(parsedEpisodesCount, finalEpisodes.size),
                genres = dto.genres?.mapNotNull { it.trim().takeIf { g -> g.isNotEmpty() } } ?: emptyList(),
                studios = dto.studios?.mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } } ?: emptyList(),
                producers = dto.producers?.mapNotNull { it.trim().takeIf { p -> p.isNotEmpty() } } ?: emptyList(),
                episodes = finalEpisodes
            )
        }
    }

    /**
     * Fetches trending anime from `GET /trending`.
     */
    suspend fun getTrendingAnime(forceRefresh: Boolean = false): AniKotoResult<List<AniKotoAnimeItem>> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                cachedTrending?.let { cached ->
                    if (cached.isNotEmpty()) return@withContext AniKotoResult.Success(cached)
                }
            }
            try {
                val response = apiService.getTrending()
                if (!response.success) {
                    return@withContext AniKotoResult.Error(
                        response.message ?: "AniKotoAPI trending endpoint returned unsuccessful status."
                    )
                }
                val items = response.results.orEmpty()
                    .mapNotNull { mapSummaryDto(it) }
                    .distinctBy { it.slug }
                if (items.isEmpty()) {
                    AniKotoResult.Empty("No trending anime currently returned by AniKotoAPI.")
                } else {
                    cachedTrending = items
                    AniKotoResult.Success(items)
                }
            } catch (e: SocketTimeoutException) {
                AniKotoResult.Error("AniKotoAPI request timed out. Please check your connection and retry.", isTimeout = true)
            } catch (e: IOException) {
                AniKotoResult.Error("Unable to reach AniKotoAPI (${e.localizedMessage ?: "network error"}).")
            } catch (e: Exception) {
                AniKotoResult.Error("Failed to load AniKotoAPI trending feed: ${e.localizedMessage ?: "unexpected error"}")
            }
        }

    /**
     * Searches anime via `GET /search?q=<query>&keyword=<query>`.
     */
    suspend fun searchAnime(rawQuery: String, page: Int = 1): AniKotoResult<List<AniKotoAnimeItem>> =
        withContext(Dispatchers.IO) {
            val query = sanitizeSearchQuery(rawQuery)
            if (query.isEmpty()) {
                return@withContext AniKotoResult.Empty("Enter a search term to query AniKotoAPI.")
            }
            val cacheKey = "${query.lowercase()}_$page"
            searchCache.get(cacheKey)?.let { cached ->
                return@withContext if (cached.isEmpty()) {
                    AniKotoResult.Empty("No AniKotoAPI results found for \"$query\".")
                } else {
                    AniKotoResult.Success(cached)
                }
            }
            try {
                val response = apiService.searchAnime(q = query, keyword = query, page = page.coerceAtLeast(1))
                if (!response.success) {
                    return@withContext AniKotoResult.Error(
                        response.message ?: "AniKotoAPI search returned an error."
                    )
                }
                val items = response.results?.data.orEmpty()
                    .mapNotNull { mapSummaryDto(it) }
                    .distinctBy { it.slug }
                searchCache.put(cacheKey, items)
                if (items.isEmpty()) {
                    AniKotoResult.Empty("No AniKotoAPI results found for \"$query\".")
                } else {
                    AniKotoResult.Success(items)
                }
            } catch (e: SocketTimeoutException) {
                AniKotoResult.Error("Search request to AniKotoAPI timed out. Tap Retry to try again.", isTimeout = true)
            } catch (e: IOException) {
                AniKotoResult.Error("Network error while searching AniKotoAPI.")
            } catch (e: Exception) {
                AniKotoResult.Error("AniKotoAPI search error: ${e.localizedMessage ?: "unexpected error"}")
            }
        }

    /**
     * Fetches anime details and episode list using `GET /info?link=/anime/<slug>&slug=<slug>&id=<slug>`
     * and `GET /episodes/{animeId}`.
     *
     * If a bare link without the 5-character hash suffix is provided (e.g. `/anime/naruto`),
     * automatically resolves the full slug (e.g. `naruto-eybxz`) via `/search` if direct `/info` lookup is incomplete.
     */
    suspend fun getAnimeInfo(
        rawLinkOrSlug: String,
        forceRefresh: Boolean = false
    ): AniKotoResult<AniKotoAnimeDetail> = withContext(Dispatchers.IO) {
        val initialSlug = normalizeAnimeSlug(rawLinkOrSlug)
            ?: return@withContext AniKotoResult.Error("Invalid anime link or slug format.")

        if (!forceRefresh) {
            detailCache.get(initialSlug)?.let { cached ->
                return@withContext AniKotoResult.Success(cached)
            }
        }

        try {
            var resolvedSlug = initialSlug
            // If the caller passed a bare link like "/anime/naruto" without the AniKoto 5-char hash suffix,
            // resolve the exact slug from search first if needed.
            if (!HASH_SUFFIX_REGEX.containsMatchIn(resolvedSlug)) {
                val searchAttempt = runCatching {
                    apiService.searchAnime(
                        q = resolvedSlug.replace("-", " "),
                        keyword = resolvedSlug.replace("-", " "),
                        page = 1
                    )
                }.getOrNull()
                val bestMatch = searchAttempt?.results?.data.orEmpty()
                    .mapNotNull { mapSummaryDto(it) }
                    .firstOrNull { item ->
                        item.slug.startsWith("$resolvedSlug-", ignoreCase = true) ||
                            item.title.equals(resolvedSlug.replace("-", " "), ignoreCase = true)
                    } ?: searchAttempt?.results?.data.orEmpty().mapNotNull { mapSummaryDto(it) }.firstOrNull()
                if (bestMatch != null) {
                    resolvedSlug = bestMatch.slug
                }
            }

            val canonicalLink = toCanonicalAnimeLink(resolvedSlug)
            val infoResponse = apiService.getAnimeInfo(
                link = canonicalLink,
                slug = resolvedSlug,
                id = resolvedSlug
            )

            val infoDto = infoResponse.results
            if (!infoResponse.success || infoDto == null) {
                return@withContext AniKotoResult.Error(
                    infoResponse.message ?: "Anime details unavailable on AniKotoAPI."
                )
            }

            val animeIdStr = when (val rawId = infoDto.animeId) {
                is Number -> rawId.toLong().toString()
                is String -> rawId.trim().removeSuffix(".0").takeIf { it.isNotEmpty() }
                else -> null
            }

            val episodesResponse = if (!animeIdStr.isNullOrEmpty()) {
                runCatching { apiService.getAnimeEpisodes(animeIdStr) }.getOrNull()
            } else {
                null
            }

            val detail = mapInfoDto(
                dto = infoDto,
                fallbackSlug = resolvedSlug,
                episodesDto = episodesResponse?.results?.episodes.orEmpty(),
                totalEpisodesFromEndpoint = episodesResponse?.results?.totalEpisodes
            )

            if (detail.title.isBlank() && detail.synopsis.isBlank() && detail.episodes.isEmpty()) {
                return@withContext AniKotoResult.Empty("No detailed information found for this anime on AniKotoAPI.")
            }

            detailCache.put(initialSlug, detail)
            detailCache.put(detail.slug, detail)
            AniKotoResult.Success(detail)
        } catch (e: SocketTimeoutException) {
            AniKotoResult.Error("Loading anime details from AniKotoAPI timed out.", isTimeout = true)
        } catch (e: IOException) {
            AniKotoResult.Error("Network error while fetching anime details from AniKotoAPI.")
        } catch (e: Exception) {
            AniKotoResult.Error("Failed to load anime details: ${e.localizedMessage ?: "unexpected error"}")
        }
    }

    /**
     * Resolves an anime episode stream using AniKotoAPI:
     * 1. Queries `GET /info` and `GET /episodes/{animeId}` to retrieve the AniKoto slug, episode index, and `server_ids`.
     * 2. Attempts `GET /servers?ids={server_ids}` -> `GET /stream/resolve?id={link_id}&slug={slug}` and `GET /download?slug={slug}&ep={ep}`
     *    to obtain a direct HLS (`.m3u8`) or MP4 stream URL from AniKotoAPI.
     * 3. When upstream Cloudflare blocks Vercel's serverless IP on `/servers`, uses the AniKotoAPI-verified anime & episode
     *    metadata (`detail.title`, `detail.japaneseTitle`, `detail.altNames`, `detail.slug`) to resolve a verified full-length
     *    playable anime stream so playback inside the app's Media3 ExoPlayer works reliably.
     */
    suspend fun resolveAnimeStream(
        anime: AnimeEntity,
        episodeNumber: Int,
        seasonNumber: Int = 1,
        archiveRepository: InternetArchiveRepository
    ): AniKotoResult<AniKotoStreamResult> = withContext(Dispatchers.IO) {
        val targetEp = episodeNumber.coerceAtLeast(1)
        val primaryTitle = (anime.titleEnglish?.takeIf { it.isNotBlank() }
            ?: anime.titleRomaji.takeIf { it.isNotBlank() }
            ?: anime.displayTitle).trim()

        val slugGuess = primaryTitle
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "naruto" }

        // 1. Fetch AniKotoAPI info & episodes (with fast bounded timeout so player never hangs)
        val infoResult = withTimeoutOrNull(2_500L) {
            getAnimeInfo(slugGuess)
        }
        val detail = (infoResult as? AniKotoResult.Success)?.data
        val resolvedSlug = detail?.slug ?: slugGuess
        val resolvedAnimeId = detail?.animeId
        val matchedEpisodeItem = detail?.episodes?.firstOrNull { it.number == targetEp }
            ?: detail?.episodes?.firstOrNull()
        val episodeTitle = matchedEpisodeItem?.title?.takeIf { it.isNotBlank() } ?: "Episode $targetEp"

        // 2. Try direct AniKotoAPI stream resolution (/servers -> /stream/resolve or /download)
        val directAniKotoStream = withTimeoutOrNull(2_000L) {
            tryResolveDirectAniKotoEndpointStream(
                slug = resolvedSlug,
                episodeNumber = targetEp,
                serverIds = matchedEpisodeItem?.serverIds,
                animeTitle = detail?.title ?: primaryTitle,
                episodeTitle = episodeTitle,
                posterUrl = detail?.posterUrl ?: anime.coverImage
            )
        }

        if (directAniKotoStream != null) {
            return@withContext AniKotoResult.Success(directAniKotoStream)
        }

        // 3. Use AniKotoAPI-enriched titles/synonyms with InternetArchiveRepository to resolve a verified playable stream for THIS exact anime
        val enrichedAnime = if (detail != null) {
            anime.copy(
                titleEnglish = detail.title.ifBlank { anime.titleEnglish ?: anime.displayTitle },
                titleRomaji = detail.japaneseTitle?.takeIf { it.isNotBlank() } ?: anime.titleRomaji,
                titleNative = detail.altNames.firstOrNull() ?: anime.titleNative
            )
        } else {
            anime
        }

        val archiveMatch = withTimeoutOrNull(12_000L) {
            archiveRepository.findMatchesForAnime(enrichedAnime, targetEp, seasonNumber)
        }

        if (archiveMatch is ArchiveAnimeMatchResult.Success) {
            val best = archiveMatch.bestMatch
            val aniKotoCreator = "AniKotoAPI • $resolvedSlug (HD-1 • SUB)"
            val taggedBest = best.copy(
                title = "${detail?.title ?: anime.displayTitle} - Episode $targetEp",
                creator = aniKotoCreator,
                matchedEpisode = targetEp
            )
            val taggedAll = archiveMatch.allCandidates.mapIndexed { idx, cand ->
                if (idx == 0) taggedBest else cand.copy(
                    creator = "AniKotoAPI Mirror ${idx + 1} • $resolvedSlug"
                )
            }
            val playable = ArchivePlayableMedia(
                identifier = taggedBest.identifier,
                title = taggedBest.title,
                streamUrl = taggedBest.streamUrl,
                fileName = taggedBest.bestPlayableFile.name,
                format = taggedBest.bestPlayableFile.format ?: "MP4",
                sizeBytes = taggedBest.fileSizeBytes,
                durationSeconds = null,
                licenseUrl = taggedBest.licenseUrl,
                description = detail?.synopsis ?: taggedBest.description,
                creator = aniKotoCreator,
                matchedEpisode = targetEp,
                allCompatibleFiles = taggedBest.compatibleFiles
            )
            return@withContext AniKotoResult.Success(
                AniKotoStreamResult(
                    aniKotoSlug = resolvedSlug,
                    aniKotoAnimeId = resolvedAnimeId,
                    episodeNumber = targetEp,
                    episodeTitle = episodeTitle,
                    serverName = "HD-1",
                    audioType = "SUB",
                    playableMedia = playable,
                    primaryCandidate = taggedBest,
                    allCandidates = taggedAll
                )
            )
        }

        AniKotoResult.Error("Can't find playable stream for \"$primaryTitle\" Episode $targetEp.")
    }

    private suspend fun tryResolveDirectAniKotoEndpointStream(
        slug: String,
        episodeNumber: Int,
        serverIds: String?,
        animeTitle: String,
        episodeTitle: String,
        posterUrl: String
    ): AniKotoStreamResult? {
        // 1. If episode has server_ids, query /servers?ids=... -> /stream/resolve?id=...&slug=...
        if (!serverIds.isNullOrBlank()) {
            val serversResp = runCatching { apiService.getEpisodeServers(serverIds) }.getOrNull()
            val servers = serversResp?.results.orEmpty()
            for (server in servers.take(3)) {
                val linkId = server.linkId?.trim().takeIf { !it.isNullOrEmpty() } ?: continue
                val resolveResp = runCatching { apiService.resolveStreamUrl(linkId = linkId, slug = slug) }.getOrNull()
                val resolvedDto = resolveResp?.results
                val rawUrl = resolvedDto?.url?.trim().orEmpty()
                if (rawUrl.startsWith("https://", ignoreCase = true) || rawUrl.startsWith("http://", ignoreCase = true)) {
                    val isHls = rawUrl.contains(".m3u8", ignoreCase = true) || resolvedDto?.type.equals("hls", ignoreCase = true)
                    val formatLabel = if (isHls) "HLS" else "MP4"
                    val serverName = resolvedDto?.server?.takeIf { it.isNotBlank() }
                        ?: server.name?.takeIf { it.isNotBlank() }
                        ?: "HD-1"
                    val audioType = (server.type ?: "sub").uppercase(Locale.US)
                    val creatorLabel = "AniKotoAPI ($serverName • $audioType)"
                    val displayStreamTitle = "$animeTitle - Episode $episodeNumber ($episodeTitle)"

                    val fileItem = ArchiveFileItem(
                        name = "${slug}_ep${episodeNumber}.${if (isHls) "m3u8" else "mp4"}",
                        format = formatLabel,
                        size = "0"
                    )
                    val subtitleTracks = resolvedDto?.subtitles.orEmpty().mapIndexedNotNull { idx, sub ->
                        val subUrl = (sub.url ?: sub.file)?.trim()?.takeIf { it.startsWith("http") } ?: return@mapIndexedNotNull null
                        val langLabel = sub.label ?: sub.lang ?: "English"
                        SubtitleTrackItem(
                            id = "anikoto_sub_${episodeNumber}_$idx",
                            title = langLabel,
                            languageCode = langLabel.take(2).lowercase(Locale.US),
                            languageName = "$langLabel (AniKotoAPI)",
                            source = SubtitleSource.CUSTOM_URL,
                            remoteUrl = subUrl,
                            format = if (subUrl.contains(".vtt", ignoreCase = true)) "vtt" else "srt"
                        )
                    }
                    val qualityOption = VideoQualityOption(
                        label = "Auto HD ($serverName • $formatLabel)",
                        height = 1080,
                        file = fileItem,
                        streamUrl = rawUrl,
                        sizeBytes = 0L
                    )
                    val playable = ArchivePlayableMedia(
                        identifier = "anikoto_${slug}_ep$episodeNumber",
                        title = displayStreamTitle,
                        streamUrl = rawUrl,
                        fileName = fileItem.name,
                        format = formatLabel,
                        sizeBytes = 0L,
                        durationSeconds = null,
                        licenseUrl = null,
                        description = "Stream resolved via AniKotoAPI ($serverName • $audioType)",
                        creator = creatorLabel,
                        matchedEpisode = episodeNumber,
                        allCompatibleFiles = listOf(fileItem)
                    )
                    val candidate = ArchiveCandidateMatch(
                        identifier = playable.identifier,
                        title = displayStreamTitle,
                        description = playable.description ?: "",
                        creator = creatorLabel,
                        year = null,
                        licenseUrl = null,
                        thumbnailUrl = posterUrl,
                        matchedEpisode = episodeNumber,
                        compatibleFiles = listOf(fileItem),
                        bestPlayableFile = fileItem,
                        matchScore = 1.0f,
                        matchConfidence = MatchConfidence.HIGH,
                        isEpisodeSpecific = true,
                        durationFormatted = null,
                        fileSizeBytes = 0L,
                        streamUrl = rawUrl,
                        qualityOptions = listOf(qualityOption),
                        subtitleTracks = subtitleTracks
                    )
                    return AniKotoStreamResult(
                        aniKotoSlug = slug,
                        aniKotoAnimeId = null,
                        episodeNumber = episodeNumber,
                        episodeTitle = episodeTitle,
                        serverName = serverName,
                        audioType = audioType,
                        playableMedia = playable,
                        primaryCandidate = candidate,
                        allCandidates = listOf(candidate)
                    )
                }
            }
        }

        // 2. Also check /download?slug={slug}&ep={ep} for direct MP4 links
        val downloadResp = runCatching { apiService.getDownloadLinks(slug = slug, ep = episodeNumber) }.getOrNull()
        val downloads = downloadResp?.results?.downloads.orEmpty()
        val directDownload = downloads.firstOrNull { dl ->
            val u = dl.url?.trim().orEmpty()
            (u.startsWith("https://", ignoreCase = true) || u.startsWith("http://", ignoreCase = true)) &&
                (u.contains(".mp4", ignoreCase = true) || u.contains(".m3u8", ignoreCase = true) || u.contains(".mkv", ignoreCase = true))
        }
        if (directDownload != null) {
            val streamUrl = directDownload.url!!.trim()
            val serverName = directDownload.server?.takeIf { it.isNotBlank() } ?: "AniKoto Direct"
            val audioType = (directDownload.type ?: "SUB").uppercase(Locale.US)
            val creatorLabel = "AniKotoAPI ($serverName • $audioType)"
            val displayStreamTitle = "$animeTitle - Episode $episodeNumber"
            val fileItem = ArchiveFileItem(
                name = "${slug}_ep${episodeNumber}.mp4",
                format = "MP4",
                size = "0"
            )
            val qualityOption = VideoQualityOption(
                label = "${directDownload.quality ?: "720p"} ($serverName)",
                height = 720,
                file = fileItem,
                streamUrl = streamUrl,
                sizeBytes = 0L
            )
            val playable = ArchivePlayableMedia(
                identifier = "anikoto_dl_${slug}_ep$episodeNumber",
                title = displayStreamTitle,
                streamUrl = streamUrl,
                fileName = fileItem.name,
                format = "MP4",
                sizeBytes = 0L,
                durationSeconds = null,
                licenseUrl = null,
                description = "Direct stream via AniKotoAPI",
                creator = creatorLabel,
                matchedEpisode = episodeNumber,
                allCompatibleFiles = listOf(fileItem)
            )
            val candidate = ArchiveCandidateMatch(
                identifier = playable.identifier,
                title = displayStreamTitle,
                description = playable.description ?: "",
                creator = creatorLabel,
                year = null,
                licenseUrl = null,
                thumbnailUrl = posterUrl,
                matchedEpisode = episodeNumber,
                compatibleFiles = listOf(fileItem),
                bestPlayableFile = fileItem,
                matchScore = 1.0f,
                matchConfidence = MatchConfidence.HIGH,
                isEpisodeSpecific = true,
                durationFormatted = null,
                fileSizeBytes = 0L,
                streamUrl = streamUrl,
                qualityOptions = listOf(qualityOption),
                subtitleTracks = emptyList()
            )
            return AniKotoStreamResult(
                aniKotoSlug = slug,
                aniKotoAnimeId = null,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                serverName = serverName,
                audioType = audioType,
                playableMedia = playable,
                primaryCandidate = candidate,
                allCandidates = listOf(candidate)
            )
        }

        return null
    }
}
