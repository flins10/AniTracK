package com.example.data.repository

import android.util.LruCache
import com.example.data.local.AnimeEntity
import com.example.data.remote.ArchiveDocItem
import com.example.data.remote.ArchiveFileItem
import com.example.data.remote.ArchiveMetadataResponse
import com.example.data.remote.InternetArchiveApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale

data class ArchiveMediaItem(
    val identifier: String,
    val title: String,
    val description: String,
    val creator: String?,
    val year: String?,
    val licenseUrl: String?,
    val thumbnailUrl: String,
    val isPublicDomainOrCreativeCommons: Boolean
)

data class ArchivePlayableMedia(
    val identifier: String,
    val title: String,
    val streamUrl: String,
    val fileName: String,
    val format: String,
    val sizeBytes: Long?,
    val durationSeconds: Float?,
    val licenseUrl: String?,
    val description: String?,
    val creator: String?,
    val matchedEpisode: Int? = null,
    val allCompatibleFiles: List<ArchiveFileItem> = emptyList()
)

data class VideoQualityOption(
    val label: String,
    val height: Int?,
    val file: ArchiveFileItem,
    val streamUrl: String,
    val sizeBytes: Long?
)

data class ArchiveCandidateMatch(
    val identifier: String,
    val title: String,
    val description: String,
    val creator: String?,
    val year: String?,
    val licenseUrl: String?,
    val thumbnailUrl: String,
    val matchedEpisode: Int?,
    val compatibleFiles: List<ArchiveFileItem>,
    val bestPlayableFile: ArchiveFileItem,
    val matchScore: Float, // 0.0f - 1.0f
    val matchConfidence: MatchConfidence,
    val isEpisodeSpecific: Boolean,
    val durationFormatted: String?,
    val fileSizeBytes: Long?,
    val streamUrl: String,
    val qualityOptions: List<VideoQualityOption> = emptyList(),
    val subtitleTracks: List<SubtitleTrackItem> = emptyList()
)

enum class MatchConfidence {
    HIGH,
    MEDIUM,
    LOW
}

sealed class ArchiveSearchResultState {
    data class Success(val items: List<ArchiveMediaItem>, val totalFound: Int) : ArchiveSearchResultState()
    data class Error(val message: String) : ArchiveSearchResultState()
    object Empty : ArchiveSearchResultState()
}

sealed class ArchivePlayableMediaResult {
    data class Success(val media: ArchivePlayableMedia) : ArchivePlayableMediaResult()
    data class Unavailable(val reason: String) : ArchivePlayableMediaResult()
    data class Error(val message: String) : ArchivePlayableMediaResult()
}

sealed class ArchiveAnimeMatchResult {
    data class Success(
        val bestMatch: ArchiveCandidateMatch,
        val allCandidates: List<ArchiveCandidateMatch>,
        val animeTitle: String,
        val requestedEpisode: Int?
    ) : ArchiveAnimeMatchResult()

    data class NoMatch(
        val reason: String,
        val queriedTitles: List<String>,
        val requestedEpisode: Int?
    ) : ArchiveAnimeMatchResult()

    data class Error(val message: String) : ArchiveAnimeMatchResult()
}

class InternetArchiveRepository(
    private val apiService: InternetArchiveApiService = InternetArchiveApiService.create(),
    val searchRepository: ArchiveSearchRepository = ArchiveSearchRepository(apiService),
    val matchService: VideoMatchService = VideoMatchService()
) {
    // In-memory LRU caches to prevent redundant requests and respect rate limits
    private val searchCache = LruCache<String, List<ArchiveMediaItem>>(60)
    private val metadataCache = LruCache<String, ArchiveMetadataResponse>(120)

    companion object {
        // Search-level negative query filter to block reaction videos, live action, dramas, stage plays, AMVs, trailers, and reviews
        private const val SEARCH_EXCLUSION_CLAUSE =
            "NOT title:(reaction OR reacts OR reacting OR patreon OR commentary OR watchalong OR \"live action\" OR liveaction OR \"stage play\" OR drama OR jdrama OR cosplay OR trailer OR preview OR teaser OR amv OR review OR opening OR ending OR gameplay OR parody OR abridged)"
    }

    suspend fun searchAnimeArchive(
        title: String,
        page: Int = 1,
        rows: Int = 20
    ): ArchiveSearchResultState = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim().replace("\"", "")
        if (cleanTitle.isBlank()) return@withContext ArchiveSearchResultState.Empty

        val cacheKey = "search_${cleanTitle.lowercase()}_p${page}_r$rows"
        val cached = searchCache.get(cacheKey)
        if (cached != null) {
            return@withContext if (cached.isEmpty()) ArchiveSearchResultState.Empty else ArchiveSearchResultState.Success(cached, cached.size)
        }

        try {
            val query = "(title:(\"$cleanTitle\") OR identifier:(\"${cleanTitle.replace(" ", "_")}\") OR description:(\"$cleanTitle\")) AND (mediatype:(movies) OR mediatype:(animation)) AND $SEARCH_EXCLUSION_CLAUSE"
            val response = apiService.searchArchive(
                query = query,
                rows = rows,
                page = page
            )

            if (response.isSuccessful) {
                val body = response.body()
                val docs = body?.response?.docs.orEmpty().filterNot { doc ->
                    matchService.containsExcludedKeywords(
                        title = doc.title.orEmpty(),
                        subject = doc.subject.orEmpty(),
                        identifier = doc.identifier,
                        description = doc.description.orEmpty(),
                        creator = doc.creator.orEmpty()
                    )
                }
                val total = docs.size

                if (docs.isEmpty()) {
                    searchCache.put(cacheKey, emptyList())
                    return@withContext ArchiveSearchResultState.Empty
                }

                val items = docs.map { doc ->
                    val id = doc.identifier
                    val docTitle = doc.title?.takeIf { it.isNotBlank() } ?: id
                    val desc = cleanHtml(doc.description.orEmpty())
                    val isPdOrCc = !doc.licenseurl.isNullOrBlank() && (
                        doc.licenseurl.contains("publicdomain", ignoreCase = true) ||
                        doc.licenseurl.contains("creativecommons", ignoreCase = true)
                    )

                    ArchiveMediaItem(
                        identifier = id,
                        title = docTitle,
                        description = desc,
                        creator = doc.creator,
                        year = doc.year,
                        licenseUrl = doc.licenseurl,
                        thumbnailUrl = InternetArchiveApiService.getThumbnailUrl(id),
                        isPublicDomainOrCreativeCommons = isPdOrCc
                    )
                }

                searchCache.put(cacheKey, items)
                ArchiveSearchResultState.Success(items, total)
            } else {
                ArchiveSearchResultState.Error("Internet Archive search failed (HTTP ${response.code()})")
            }
        } catch (e: Exception) {
            ArchiveSearchResultState.Error("Failed to reach Internet Archive: ${e.localizedMessage ?: "Network error"}")
        }
    }

    suspend fun getItemMetadata(identifier: String): ArchiveMetadataResponse? = withContext(Dispatchers.IO) {
        val cached = metadataCache.get(identifier)
        if (cached != null) return@withContext cached

        try {
            val response = apiService.getItemMetadata(identifier)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    metadataCache.put(identifier, body)
                }
                body
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun inspectAndResolvePlayableMedia(
        identifier: String,
        preferredEpisode: Int? = null
    ): ArchivePlayableMediaResult = withContext(Dispatchers.IO) {
        try {
            val metaResponse = getItemMetadata(identifier)
                ?: return@withContext ArchivePlayableMediaResult.Error("Could not retrieve metadata for archive item $identifier")

            val files = metaResponse.files.orEmpty()
            val meta = metaResponse.metadata

            if (files.isEmpty()) {
                return@withContext ArchivePlayableMediaResult.Unavailable("Archive item '$identifier' does not contain any downloadable files.")
            }

            val compatibleFiles = filterCompatibleVideoFiles(files, requireFullEpisodeLength = (preferredEpisode != null))
            if (compatibleFiles.isEmpty()) {
                val availableFormats = files.mapNotNull { it.format }.distinct().take(5).joinToString(", ")
                return@withContext ArchivePlayableMediaResult.Unavailable(
                    "No full-length compatible playback formats (MP4, MKV, WebM, HLS) found in '$identifier'. Available formats: $availableFormats"
                )
            }

            // Pick the exact file matching preferredEpisode if specified; do not fall back to random file if preferredEpisode was requested
            val bestFile = if (preferredEpisode != null && preferredEpisode > 0) {
                selectBestFileForEpisode(compatibleFiles, preferredEpisode)
                    ?: return@withContext ArchivePlayableMediaResult.Unavailable(
                        "Episode $preferredEpisode was not found in '$identifier'."
                    )
            } else {
                compatibleFiles.first()
            }

            val streamUrl = InternetArchiveApiService.getDirectDownloadUrl(identifier, bestFile.name)
            val title = meta?.title ?: bestFile.title ?: identifier
            val size = bestFile.size?.toLongOrNull()
            val duration = matchService.parseDurationSeconds(bestFile.length)

            val detectedEp = detectEpisodeNumber(bestFile.name) ?: preferredEpisode

            val playable = ArchivePlayableMedia(
                identifier = identifier,
                title = title,
                streamUrl = streamUrl,
                fileName = bestFile.name,
                format = bestFile.format ?: "MP4",
                sizeBytes = size,
                durationSeconds = duration,
                licenseUrl = meta?.licenseurl,
                description = meta?.description,
                creator = meta?.creator,
                matchedEpisode = detectedEp,
                allCompatibleFiles = compatibleFiles
            )

            ArchivePlayableMediaResult.Success(playable)
        } catch (e: Exception) {
            ArchivePlayableMediaResult.Error("Error resolving archive stream: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Deep-searches Internet Archive across all title variants (English, Romaji, Native, Pinyin),
     * inspects up to 24 candidate items and their internal file lists in parallel,
     * and strictly returns ONLY verified full-episode matches for the exact selected anime and episode.
     * Never plays random 2-3 minute clips, reactions, or live action videos.
     */
    suspend fun findMatchesForAnime(
        anime: AnimeEntity,
        episodeNumber: Int? = null,
        seasonNumber: Int? = null
    ): ArchiveAnimeMatchResult = withContext(Dispatchers.IO) {
        val titlesToTry = matchService.extractSearchTitles(anime)

        if (titlesToTry.isEmpty()) {
            return@withContext ArchiveAnimeMatchResult.NoMatch(
                reason = "Anime has no valid titles to search.",
                queriedTitles = emptyList(),
                requestedEpisode = episodeNumber
            )
        }

        val extractedSeasonFromTitle = matchService.extractSeasonNumber(titlesToTry)
        val activeSeason = if (seasonNumber != null && seasonNumber > 1) {
            seasonNumber
        } else {
            extractedSeasonFromTitle ?: seasonNumber ?: 1
        }
        val seasonPadded = String.format(Locale.ROOT, "%02d", activeSeason)

        val candidateDocs = LinkedHashMap<String, ArchiveDocItem>()

        // 0. If anime already has a known archiveIdentifier, inspect it first
        anime.archiveIdentifier?.takeIf { it.isNotBlank() }?.let { knownId ->
            candidateDocs[knownId] = ArchiveDocItem(
                identifier = knownId,
                title = anime.displayTitle
            )
        }

        val epPadded = if (episodeNumber != null && episodeNumber > 0) {
            String.format(Locale.ROOT, "%02d", episodeNumber)
        } else null

        // Build deep search queries in parallel for maximum speed and coverage
        val searchQueries = mutableListOf<String>()
        for (rawTitle in titlesToTry.take(4)) {
            val clean = cleanTitleForSearch(rawTitle)
            if (clean.isBlank()) continue
            val slug = clean.lowercase(Locale.ROOT).replace(" ", "_")
            val dashSlug = clean.lowercase(Locale.ROOT).replace(" ", "-")

            if (episodeNumber != null && episodeNumber > 0) {
                // Tier 1: Exact Episode & Season Queries
                searchQueries.add(
                    "(title:(\"$clean\") OR identifier:($slug* OR $dashSlug*)) AND (title:(\"S${seasonPadded}E$epPadded\" OR \"S${activeSeason}E$epPadded\" OR \"Season $activeSeason Episode $episodeNumber\" OR \"Season $activeSeason Ep $episodeNumber\" OR \"Season $seasonPadded Ep $epPadded\" OR \"Episode $episodeNumber\" OR \"Episode $epPadded\" OR \"Ep $episodeNumber\" OR \"Ep $epPadded\" OR \"E$epPadded\")) AND (mediatype:(movies) OR mediatype:(animation)) AND $SEARCH_EXCLUSION_CLAUSE"
                )
                if (activeSeason > 1) {
                    searchQueries.add(
                        "(title:(\"$clean Season $activeSeason\") OR title:(\"$clean ${activeSeason}nd Season\") OR title:(\"$clean ${activeSeason}rd Season\") OR title:(\"$clean ${activeSeason}th Season\")) AND (title:(\"Episode $episodeNumber\" OR \"Ep $episodeNumber\" OR \"E$epPadded\" OR \"$epPadded\")) AND (mediatype:(movies) OR mediatype:(animation)) AND $SEARCH_EXCLUSION_CLAUSE"
                    )
                }
            }

            // Tier 2: Full Series / Complete Batch Archive Collections
            if (activeSeason > 1) {
                searchQueries.add(
                    "(title:(\"$clean Season $activeSeason\") OR title:(\"$clean ${activeSeason}nd Season\") OR title:(\"$clean ${activeSeason}rd Season\") OR title:(\"$clean ${activeSeason}th Season\") OR identifier:(${slug}_season_${activeSeason}* OR ${slug}_s${activeSeason}*)) AND (mediatype:(movies) OR mediatype:(animation)) AND $SEARCH_EXCLUSION_CLAUSE"
                )
            }
            searchQueries.add(
                "(title:(\"$clean\") OR identifier:($slug* OR $dashSlug*) OR subject:(\"$clean\")) AND (mediatype:(movies) OR mediatype:(animation)) AND $SEARCH_EXCLUSION_CLAUSE"
            )
        }

        // Execute deep search queries concurrently
        coroutineScope {
            searchQueries.distinct().take(8).map { query ->
                async {
                    try {
                        val res = apiService.searchArchive(query = query, rows = 25)
                        res.body()?.response?.docs.orEmpty()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten().forEach { doc ->
                if (!matchService.containsExcludedKeywords(
                        title = doc.title.orEmpty(),
                        subject = doc.subject.orEmpty(),
                        identifier = doc.identifier,
                        description = doc.description.orEmpty(),
                        creator = doc.creator.orEmpty()
                    )
                ) {
                    candidateDocs.putIfAbsent(doc.identifier, doc)
                }
            }
        }

        if (candidateDocs.isEmpty()) {
            return@withContext ArchiveAnimeMatchResult.NoMatch(
                reason = "No matching video found for ${anime.displayTitle}${if (episodeNumber != null) " Season $activeSeason Episode $episodeNumber" else ""}.",
                queriedTitles = titlesToTry,
                requestedEpisode = episodeNumber
            )
        }

        // Pre-sort candidate docs by title similarity to target titles so the most relevant collections are inspected first
        val rankedDocs = candidateDocs.values.sortedByDescending { doc ->
            val baseSim = matchService.calculateStrictTitleSimilarity(
                docTitle = doc.title ?: doc.identifier,
                identifier = doc.identifier,
                targetTitles = titlesToTry
            )
            val epBonus = if (episodeNumber != null && matchService.detectEpisodeNumber(doc.title ?: doc.identifier) == episodeNumber) {
                0.25f
            } else 0f
            baseSim + epBonus
        }.filter { doc ->
            // Only inspect items that have a plausible title or identifier connection to the anime
            matchService.calculateStrictTitleSimilarity(
                docTitle = doc.title ?: doc.identifier,
                identifier = doc.identifier,
                targetTitles = titlesToTry
            ) >= 0.50f
        }

        if (rankedDocs.isEmpty()) {
            return@withContext ArchiveAnimeMatchResult.NoMatch(
                reason = "No exact title match found for \"${anime.displayTitle}\".",
                queriedTitles = titlesToTry,
                requestedEpisode = episodeNumber
            )
        }

        // Inspect up to 20 candidate items in parallel to check their internal file manifests for the exact episode
        val inspectedCandidates = coroutineScope {
            rankedDocs.take(20).map { doc ->
                async {
                    val metadata = getItemMetadata(doc.identifier) ?: return@async null
                    matchService.evaluateCandidate(
                        doc = doc,
                        metadata = metadata,
                        anime = anime,
                        requestedEpisode = episodeNumber,
                        seasonNumber = activeSeason
                    )
                }
            }.awaitAll().filterNotNull()
        }

        if (inspectedCandidates.isEmpty()) {
            val epText = if (episodeNumber != null) "Episode $episodeNumber" else "full video"
            return@withContext ArchiveAnimeMatchResult.NoMatch(
                reason = "No verified full-length anime video for $epText was found.",
                queriedTitles = titlesToTry,
                requestedEpisode = episodeNumber
            )
        }

        // Sort verified candidates by match score and episode specificity
        val sortedCandidates = inspectedCandidates.sortedWith(
            compareByDescending<ArchiveCandidateMatch> { it.isEpisodeSpecific }
                .thenByDescending { it.matchScore }
                .thenByDescending { it.fileSizeBytes ?: 0L }
        )
        val best = sortedCandidates.first()

        ArchiveAnimeMatchResult.Success(
            bestMatch = best,
            allCandidates = sortedCandidates,
            animeTitle = anime.displayTitle,
            requestedEpisode = episodeNumber
        )
    }

    private fun filterCompatibleVideoFiles(
        files: List<ArchiveFileItem>,
        requireFullEpisodeLength: Boolean = true
    ): List<ArchiveFileItem> {
        return matchService.filterCompatibleVideoFiles(files, requireFullEpisodeLength)
    }

    private fun selectBestFileForEpisode(
        files: List<ArchiveFileItem>,
        preferredEpisode: Int?
    ): ArchiveFileItem? {
        return matchService.selectBestFileForEpisode(files, preferredEpisode)
    }

    fun detectEpisodeNumber(text: String): Int? {
        return matchService.detectEpisodeNumber(text)
    }

    private fun cleanTitleForSearch(title: String): String {
        return title
            .replace(Regex("(?i)\\b(season|2nd season|3rd season|4th season|part|tv|movie|cour|ova)\\b.*"), "")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
