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
import kotlinx.coroutines.withTimeoutOrNull
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
        // Search-level negative query filter for Anime (blocks trailers, reviews, explanations, reactions, clips, live action)
        private const val SEARCH_EXCLUSION_CLAUSE =
            "NOT title:(trailer OR teaser OR review OR reviews OR explained OR explanation OR breakdown OR analysis OR recap OR summary OR reaction OR reacts OR reacting OR patreon OR commentary OR watchalong OR \"live action\" OR liveaction OR \"stage play\" OR cosplay OR promo OR preview OR amv OR opening OR ending OR gameplay OR parody OR abridged OR interview OR clip)"

        // Search-level negative query filter for Live-Action Movies & Series (blocks trailers, reviews, explanations, reactions, clips, animation)
        private const val LIVE_ACTION_SEARCH_EXCLUSION_CLAUSE =
            "NOT title:(trailer OR teaser OR review OR reviews OR explained OR explanation OR breakdown OR analysis OR recap OR summary OR reaction OR reacts OR reacting OR commentary OR watchalong OR promo OR preview OR amv OR gameplay OR parody OR spoof OR interview OR clip OR shorts)"
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
            val query = "(title:(\"$cleanTitle\") OR identifier:(\"${cleanTitle.replace(" ", "_")}\")) AND (mediatype:(movies) OR mediatype:(animation)) AND $SEARCH_EXCLUSION_CLAUSE"
            val response = withTimeoutOrNull(4500L) {
                apiService.searchArchive(
                    query = query,
                    rows = rows,
                    page = page
                )
            } ?: return@withContext ArchiveSearchResultState.Empty

            if (response.isSuccessful) {
                val body = response.body()
                val docs = body?.response?.docs.orEmpty().filterNot { doc ->
                    val size = doc.item_size?.toLongOrNull()
                    (size != null && size in 1 until 15_000_000L) ||
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
            val response = withTimeoutOrNull(4500L) {
                apiService.getItemMetadata(identifier)
            } ?: return@withContext null

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

    suspend fun findBestLiveActionStream(
        title: String,
        episodeNumber: Int? = null,
        isSeries: Boolean = false
    ): ArchivePlayableMediaResult = withContext(Dispatchers.IO) {
        val syntheticEntity = AnimeEntity(
            id = title.hashCode(),
            titleRomaji = title,
            titleEnglish = title,
            coverImage = "",
            format = if (isSeries) "TV" else "MOVIE",
            status = "FINISHED",
            totalEpisodes = if (isSeries) 24 else 1,
            airingDayOfWeek = 1
        )
        when (val matchResult = findMatchesForAnime(
            anime = syntheticEntity,
            episodeNumber = if (isSeries) episodeNumber else null,
            seasonNumber = 1,
            isLiveActionAllowed = true
        )) {
            is ArchiveAnimeMatchResult.Success -> {
                val best = matchResult.bestMatch
                ArchivePlayableMediaResult.Success(
                    ArchivePlayableMedia(
                        identifier = best.identifier,
                        title = best.title,
                        streamUrl = best.streamUrl,
                        fileName = best.bestPlayableFile.name,
                        format = best.bestPlayableFile.format ?: "MP4",
                        sizeBytes = best.fileSizeBytes,
                        durationSeconds = null,
                        licenseUrl = best.licenseUrl,
                        description = best.description,
                        creator = best.creator,
                        matchedEpisode = best.matchedEpisode ?: episodeNumber,
                        allCompatibleFiles = best.compatibleFiles
                    )
                )
            }
            is ArchiveAnimeMatchResult.NoMatch -> ArchivePlayableMediaResult.Unavailable(matchResult.reason)
            is ArchiveAnimeMatchResult.Error -> ArchivePlayableMediaResult.Error(matchResult.message)
        }
    }

    suspend fun inspectAndResolvePlayableMedia(
        identifier: String,
        preferredEpisode: Int? = null,
        isMovie: Boolean = (preferredEpisode == null),
        isMovieOrSingleWork: Boolean = isMovie,
        isLiveActionAllowed: Boolean = false
    ): ArchivePlayableMediaResult = withContext(Dispatchers.IO) {
        try {
            val metaResponse = getItemMetadata(identifier)
                ?: return@withContext ArchivePlayableMediaResult.Error("Can't find video metadata for $identifier")

            val files = metaResponse.files.orEmpty()
            val meta = metaResponse.metadata

            if (files.isEmpty()) {
                return@withContext ArchivePlayableMediaResult.Unavailable("Can't find playable video files in '$identifier'.")
            }

            // Strictly block trailer, review, explanation, reaction, and promo items
            if (matchService.containsExcludedKeywords(
                    title = meta?.title.orEmpty(),
                    subject = meta?.subject.orEmpty(),
                    identifier = identifier,
                    description = meta?.description.orEmpty(),
                    creator = meta?.creator.orEmpty(),
                    isLiveActionAllowed = isLiveActionAllowed
                )
            ) {
                return@withContext ArchivePlayableMediaResult.Unavailable("Blocked trailer, review, or explanation video.")
            }

            val compatibleFiles = matchService.filterCompatibleVideoFiles(
                files = files,
                requireFullEpisodeLength = true,
                requestedEpisode = preferredEpisode,
                isMovieOrSingleWork = isMovie,
                isLiveActionAllowed = isLiveActionAllowed
            )
            if (compatibleFiles.isEmpty()) {
                return@withContext ArchivePlayableMediaResult.Unavailable(
                    "Can't find full-length video in '$identifier'."
                )
            }

            // Pick the exact file matching preferredEpisode if specified; do not fall back to random file if preferredEpisode was requested
            val bestFile = if (!isMovie && preferredEpisode != null && preferredEpisode > 0) {
                selectBestFileForEpisode(compatibleFiles, preferredEpisode)
                    ?: if (compatibleFiles.size == 1 && detectEpisodeNumber(compatibleFiles.first().name) == null) {
                        compatibleFiles.first()
                    } else {
                        return@withContext ArchivePlayableMediaResult.Unavailable(
                            "Can't find Episode $preferredEpisode in '$identifier'."
                        )
                    }
            } else {
                selectBestFileForEpisode(compatibleFiles, null) ?: compatibleFiles.first()
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
            ArchivePlayableMediaResult.Error("Can't find stream: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Fast, verified search on Internet Archive across title variants (Anime or Live-Action Movie/Series).
     * Strictly blocks trailers, reviews, explanations, reactions, and short clips.
     * Completes within a bounded timeout so it never hangs or loads forever.
     */
    suspend fun findMatchesForAnime(
        anime: AnimeEntity,
        episodeNumber: Int? = null,
        seasonNumber: Int? = null,
        isLiveActionAllowed: Boolean = false
    ): ArchiveAnimeMatchResult = withContext(Dispatchers.IO) {
        val titlesToTry = matchService.extractSearchTitles(anime)

        if (titlesToTry.isEmpty()) {
            return@withContext ArchiveAnimeMatchResult.NoMatch(
                reason = "Can't find video on Internet Archive.",
                queriedTitles = emptyList(),
                requestedEpisode = episodeNumber
            )
        }

        val isMovieFormat = anime.format.equals("MOVIE", ignoreCase = true) || (anime.totalEpisodes == 1 && (episodeNumber == null || episodeNumber <= 1))
        val minItemSizeBytes = if (isMovieFormat) VideoMatchService.MIN_MOVIE_FILE_SIZE_BYTES else VideoMatchService.MIN_EPISODE_FILE_SIZE_BYTES

        val extractedSeasonFromTitle = matchService.extractSeasonNumber(titlesToTry)
        val activeSeason = if (seasonNumber != null && seasonNumber > 1) {
            seasonNumber
        } else {
            extractedSeasonFromTitle ?: seasonNumber ?: 1
        }
        val seasonPadded = String.format(Locale.ROOT, "%02d", activeSeason)

        // 0. FAST PATH: If item already has a known archiveIdentifier, inspect it immediately first!
        val knownId = anime.archiveIdentifier?.trim()?.takeIf { it.isNotBlank() }
        if (knownId != null) {
            val fastMeta = getItemMetadata(knownId)
            if (fastMeta != null) {
                val fastDoc = ArchiveDocItem(
                    identifier = knownId,
                    title = fastMeta.metadata?.title ?: anime.displayTitle
                )
                val directMatch = matchService.evaluateCandidate(
                    doc = fastDoc,
                    metadata = fastMeta,
                    anime = anime,
                    requestedEpisode = if (isMovieFormat) null else episodeNumber,
                    seasonNumber = activeSeason,
                    isLiveActionAllowed = isLiveActionAllowed
                )
                if (directMatch != null) {
                    return@withContext ArchiveAnimeMatchResult.Success(
                        bestMatch = directMatch,
                        allCandidates = listOf(directMatch),
                        animeTitle = anime.displayTitle,
                        requestedEpisode = episodeNumber
                    )
                }
            }
        }

        val candidateDocs = LinkedHashMap<String, ArchiveDocItem>()
        val epPadded = if (!isMovieFormat && episodeNumber != null && episodeNumber > 0) {
            String.format(Locale.ROOT, "%02d", episodeNumber)
        } else null

        val exclusionClause = if (isLiveActionAllowed) LIVE_ACTION_SEARCH_EXCLUSION_CLAUSE else SEARCH_EXCLUSION_CLAUSE
        val mediaTypeClause = if (isLiveActionAllowed) "mediatype:(movies)" else "(mediatype:(movies) OR mediatype:(animation))"

        // Build targeted search queries (fast, no slow prefix wildcards)
        val searchQueries = mutableListOf<String>()
        for (rawTitle in titlesToTry.take(2)) {
            val clean = cleanTitleForSearch(rawTitle)
            if (clean.isBlank()) continue

            if (!isMovieFormat && episodeNumber != null && episodeNumber > 0) {
                searchQueries.add(
                    "title:(\"$clean\") AND title:(\"S${seasonPadded}E$epPadded\" OR \"Episode $episodeNumber\" OR \"Episode $epPadded\" OR \"Ep $episodeNumber\" OR \"Ep $epPadded\") AND $mediaTypeClause AND $exclusionClause"
                )
            }
            searchQueries.add(
                "title:(\"$clean\") AND $mediaTypeClause AND $exclusionClause"
            )
        }

        // Execute search queries concurrently with a strict 4.5s timeout
        coroutineScope {
            searchQueries.distinct().take(4).map { query ->
                async {
                    try {
                        val res = withTimeoutOrNull(4500L) {
                            apiService.searchArchive(query = query, rows = 15)
                        }
                        res?.body()?.response?.docs.orEmpty()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten().forEach { doc ->
                val itemSize = doc.item_size?.toLongOrNull()
                val isLargeEnough = (itemSize == null || itemSize >= minItemSizeBytes)
                if (isLargeEnough && !matchService.containsExcludedKeywords(
                        title = doc.title.orEmpty(),
                        subject = doc.subject.orEmpty(),
                        identifier = doc.identifier,
                        description = doc.description.orEmpty(),
                        creator = doc.creator.orEmpty(),
                        isLiveActionAllowed = isLiveActionAllowed
                    )
                ) {
                    candidateDocs.putIfAbsent(doc.identifier, doc)
                }
            }
        }

        if (candidateDocs.isEmpty()) {
            return@withContext ArchiveAnimeMatchResult.NoMatch(
                reason = "Can't find \"${anime.displayTitle}\" on Internet Archive.",
                queriedTitles = titlesToTry,
                requestedEpisode = episodeNumber
            )
        }

        // Pre-sort candidate docs by title similarity and file size
        val rankedDocs = candidateDocs.values.sortedByDescending { doc ->
            val baseSim = matchService.calculateStrictTitleSimilarity(
                docTitle = doc.title ?: doc.identifier,
                identifier = doc.identifier,
                targetTitles = titlesToTry
            )
            val epBonus = if (!isMovieFormat && episodeNumber != null && matchService.detectEpisodeNumber(doc.title ?: doc.identifier) == episodeNumber) {
                0.25f
            } else 0f
            baseSim + epBonus
        }.filter { doc ->
            matchService.calculateStrictTitleSimilarity(
                docTitle = doc.title ?: doc.identifier,
                identifier = doc.identifier,
                targetTitles = titlesToTry
            ) >= 0.55f
        }

        if (rankedDocs.isEmpty()) {
            return@withContext ArchiveAnimeMatchResult.NoMatch(
                reason = "Can't find \"${anime.displayTitle}\" on Internet Archive.",
                queriedTitles = titlesToTry,
                requestedEpisode = episodeNumber
            )
        }

        // Inspect top 8 candidate items in parallel with fast timeout
        val inspectedCandidates = coroutineScope {
            rankedDocs.take(8).map { doc ->
                async {
                    val metadata = getItemMetadata(doc.identifier) ?: return@async null
                    matchService.evaluateCandidate(
                        doc = doc,
                        metadata = metadata,
                        anime = anime,
                        requestedEpisode = if (isMovieFormat) null else episodeNumber,
                        seasonNumber = activeSeason,
                        isLiveActionAllowed = isLiveActionAllowed
                    )
                }
            }.awaitAll().filterNotNull()
        }

        if (inspectedCandidates.isEmpty()) {
            return@withContext ArchiveAnimeMatchResult.NoMatch(
                reason = "Can't find full video for \"${anime.displayTitle}\" on Internet Archive.",
                queriedTitles = titlesToTry,
                requestedEpisode = episodeNumber
            )
        }

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
