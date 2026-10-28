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
        // Search-level negative query filter for Anime (blocks trailers, reviews, explanations, reactions, clips, live action, YouTube/Twitch)
        private const val SEARCH_EXCLUSION_CLAUSE =
            "NOT collection:(youtubearchive OR twitch) AND NOT title:(trailer OR teaser OR review OR reviews OR explained OR explanation OR breakdown OR analysis OR recap OR reaction OR reacts OR reacting OR patreon OR commentary OR watchalong OR \"live action\" OR liveaction OR \"stage play\" OR cosplay OR promo OR preview OR amv OR gameplay OR parody OR abridged OR interview OR shorts)"

        // Search-level negative query filter for Live-Action Movies & Series (blocks trailers, reviews, explanations, reactions, clips, YouTube/Twitch)
        private const val LIVE_ACTION_SEARCH_EXCLUSION_CLAUSE =
            "NOT collection:(youtubearchive OR twitch) AND NOT title:(trailer OR teaser OR review OR reviews OR explained OR explanation OR breakdown OR analysis OR recap OR reaction OR reacts OR reacting OR commentary OR watchalong OR promo OR preview OR amv OR gameplay OR parody OR spoof OR interview OR shorts)"

        // Verified Internet Archive identifiers for popular Anime, Bollywood, and Hollywood titles
        private val VERIFIED_TITLE_IDENTIFIERS: Map<String, List<String>> = mapOf(
            // Anime Series & Movies
            "naruto" to listOf("naruto-completo", "DevPointNarutoS1203204"),
            "naruto shippuden" to listOf("naruto-completo", "DevPointNarutoS1203204"),
            "one piece" to listOf("one-piece-0001-1000-1999-horrible-subs"),
            "death note" to listOf("death-note-complete-2006-2007"),
            "bleach" to listOf("bleach-complete"),
            "hunter x hunter" to listOf("U.U.THunterXHunter2011Ep01-50_720pUPByDarkSniper", "hunter-x-hunter-1999-remastered-v4-0"),
            "cowboy bebop" to listOf("db-bebop-of-the-cowboys-1080p"),
            "fullmetal alchemist" to listOf("20230208_20230208_2248"),
            "fullmetal alchemist brotherhood" to listOf("20230208_20230208_2248"),
            "dragon ball z" to listOf("dbkai", "dbz-westwood-remaster"),
            "dragon ball kai" to listOf("dbkai"),
            "steins gate" to listOf("anime-time-steins-gate"),
            "chainsaw man" to listOf("chainsaw-man-latino-"),
            "spy x family" to listOf("ver-spy-x-family-capitulo-8-sub-espanol-latino-en-hd-online-o-anime-lhd"),
            "berserk" to listOf("berserk-1997_202403"),
            "neon genesis evangelion" to listOf("evangelion-vhs-rip", "evangelioncastellano"),
            "evangelion" to listOf("evangelion-vhs-rip", "evangelioncastellano"),
            "jujutsu kaisen" to listOf("jujutsu-kaisen-the-culling-game-part-1-5-sub-v-2-1080p.mp-4_202601", "jujutsu-kaisen-episode-4-english-sub"),
            "attack on titan" to listOf("Shingeki_no_Kyojin", "attack-on-titan-1-2-colossal-cut-hd-720p"),
            "shingeki no kyojin" to listOf("Shingeki_no_Kyojin", "attack-on-titan-1-2-colossal-cut-hd-720p"),
            "solo leveling" to listOf("solo-leveling_20250830"),
            "ore dake level up na ken" to listOf("solo-leveling_20250830"),
            "demon slayer" to listOf("www.-9xflix.com-demon-slayer-kimetsu-no-yaiba-infinity-castle-2025-dual-audio-hi"),
            "kimetsu no yaiba" to listOf("www.-9xflix.com-demon-slayer-kimetsu-no-yaiba-infinity-castle-2025-dual-audio-hi"),
            "my hero academia" to listOf("boku-no-hero-academia-temporada-5", "my-hero-academia-heroes-rising-2019"),
            "boku no hero academia" to listOf("boku-no-hero-academia-temporada-5", "my-hero-academia-heroes-rising-2019"),
            "sword art online" to listOf("SAOS1AllEps_BD1080pUPByDarkSniper_201501"),
            "black clover" to listOf("black-clover-3"),
            "fairy tail" to listOf("fairy-tail-episode-1", "fairy-tail-81-160"),
            "gintama" to listOf("gintama-2011", "Gintama202AnimePower"),
            "haikyu" to listOf("haikyuu-s3-dub", "Haikyuu-the-dumpster-battle-english-sub"),
            "haikyuu" to listOf("haikyuu-s3-dub", "Haikyuu-the-dumpster-battle-english-sub"),
            "vinland saga" to listOf("db-vinland-saga-s-1-dual-audio-bd-1080p", "vinland-saga-latino_202107"),
            "cyberpunk edgerunners" to listOf("cyberpunk-edgerunners-e-"),
            "dandadan" to listOf("dandadan-ep-1"),
            "yu yu hakusho" to listOf("YuYuHakushoAnimax", "yu-yu-hakusho_202308"),
            "inuyasha" to listOf("inuyasha-01-100", "inuyasha-101-167"),
            "trigun" to listOf("trigun-full-serie-completa", "episode-1-4"),
            "samurai champloo" to listOf("samurai-champloo_202107", "samurai-champloo-espanol"),
            "serial experiments lain" to listOf("serial-experiments-lain-english", "09-sel-protocol"),
            "parasyte" to listOf("parasyte.-part.-1.2014.1080p.-blu-ray.x-264.-aac-5.1", "ParasytePart2"),
            "kiseijuu" to listOf("parasyte.-part.-1.2014.1080p.-blu-ray.x-264.-aac-5.1", "ParasytePart2"),
            "lupin the third" to listOf("lupin-the-third-part-1"),
            "future boy conan" to listOf("FutureBoyConan"),
            "sherlock hound" to listOf("SherlockHound"),
            "astro boy" to listOf("astro-boy-1963"),
            // Bollywood Movies
            "3 idiots" to listOf("3-idiots_202108"),
            "dangal" to listOf("dangal-hindi-educational-movie-hd"),
            "lagaan" to listOf("lagaan-2001-dv-drip-charme-leon-silver-rg"),
            "taare zameen par" to listOf("taare-zameen-par-hindi-educational-movie"),
            "zindagi na milegi dobara" to listOf("zindagi-na-milegi-dobara-2011-webhd-720-p-hritik-roshan-abhay-deol"),
            "gangs of wasseypur" to listOf("gangs-of-wasseypur-2012-part-1-hindi-1080p"),
            "sholay" to listOf("sholay-1975_202402"),
            "bajrangi bhaijaan" to listOf("bajrangi-bhaijaan-2015_202508"),
            "kabhi khushi kabhie gham" to listOf("kabhi-khushi-kabhie-gham-2001-dv-drip-x-264-aac-chapters-dr-c"),
            "dilwale dulhania le jayenge" to listOf("ddlj_202007"),
            "chak de india" to listOf("chak-de-india"),
            "munna bhai mbbs" to listOf("munna-bhai-mbbs-2003-xvid-mm-tdbb"),
            "munna bhai m b b s" to listOf("munna-bhai-mbbs-2003-xvid-mm-tdbb"),
            "bhool bhulaiyaa 2" to listOf("bhool.-bhulaiyaa.-2.2022.-bolly-4u.org-web-dl-hindi-1080p-1.9-gb"),
            "drishyam" to listOf("drishyam-2015-1080p-blu-ray-x-264-dd-5.1"),
            "tumbbad" to listOf("www.-5-movie-rulz.casa-tumbbad-2018-1080p-hq-hdrip-x-265-hevc-dd-5.1-192-kbps-te"),
            "swades" to listOf("swades-2004-blu-ray-1080p-10bit-hevc-hindi-dd-5.1-h-265-esubs-ronin-swades-2004-"),
            // Hollywood & International Movies
            "interstellar" to listOf("interstellar-2014_202409"),
            "oppenheimer" to listOf("oppenheimer.-2023.1080-p.-bluray.-egydead.-com"),
            "inception" to listOf("26.10.2001-inception"),
            "the dark knight rises" to listOf("The.Dark.Knight.Rises.2012.1080pWww.compucalitv.com"),
            "dark knight rises" to listOf("The.Dark.Knight.Rises.2012.1080pWww.compucalitv.com"),
            "fight club" to listOf("fight-club_202006"),
            "the godfather" to listOf("godfather_202312"),
            "godfather" to listOf("godfather_202312"),
            "the shawshank redemption" to listOf("The-Shawshank-Redemption_1994_AI_1080p.BluRay.10b.HEVC.DTS-MA.5.1_BLUDUAINE"),
            "shawshank redemption" to listOf("The-Shawshank-Redemption_1994_AI_1080p.BluRay.10b.HEVC.DTS-MA.5.1_BLUDUAINE"),
            "deadpool 2" to listOf("deadpool.2.2018.1080p.unratedduallatcinecalidad.to_202002"),
            "jurassic park" to listOf("jurassic-park_202405"),
            "memento" to listOf("memento.-2000.1080p.-blu-ray.x-264.-yify"),
            "django unchained" to listOf("egy-best.-django.-unchained.-2012.-blu-ray.-1080p.x-264"),
            "inglourious basterds" to listOf("inglourious-basterds-2009_202605"),
            "parasite" to listOf("parasite-2019_202408"),
            // Live-Action Series
            "breaking bad" to listOf("breaking-bad-1"),
            "stranger things" to listOf("stranger.-things.-s-01.720p.-blu-ray.x-264-galaxy-tv"),
            "sacred games" to listOf("sacred-games-s02.-hevc"),
            "mirzapur" to listOf("mirzapur-s-02-e-10-hindi-480p-web-dl"),
            "sherlock holmes" to listOf("SherlockHolmesGranada"),
            "the adventures of sherlock holmes" to listOf("SherlockHolmesGranada"),
            "byomkesh bakshi" to listOf("byomkesh-bakshi-movie-world")
        )

        fun resolveVerifiedIdentifiersForTitles(titles: List<String>): List<String> {
            val result = mutableListOf<String>()
            for (raw in titles) {
                val norm = raw.lowercase(Locale.ROOT)
                    .replace(Regex("""(?i)\b(season\s*\d+|2nd season|3rd season|4th season|final season|part\s*\d+|cour\s*\d+)\b.*"""), "")
                    .replace(Regex("""[^a-z0-9\s]"""), " ")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
                if (norm.isBlank()) continue
                VERIFIED_TITLE_IDENTIFIERS[norm]?.let { result.addAll(it) }
                // Also check prefix before colon if any
                for ((key, ids) in VERIFIED_TITLE_IDENTIFIERS) {
                    if (norm == key || norm.startsWith("$key ") || key.startsWith("$norm ")) {
                        result.addAll(ids)
                    }
                }
            }
            return result.distinct()
        }
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
            val response = withTimeoutOrNull(6500L) {
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
            val response = withTimeoutOrNull(8500L) {
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
        val verifiedIds = resolveVerifiedIdentifiersForTitles(listOf(title))
        val syntheticEntity = AnimeEntity(
            id = title.hashCode(),
            titleRomaji = title,
            titleEnglish = title,
            coverImage = "",
            format = if (isSeries) "TV" else "MOVIE",
            status = "FINISHED",
            totalEpisodes = if (isSeries) 24 else 1,
            airingDayOfWeek = 1,
            archiveIdentifier = verifiedIds.firstOrNull()
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

            // Pick the exact file matching preferredEpisode if specified
            val bestFile = if (!isMovie && preferredEpisode != null && preferredEpisode > 0) {
                selectBestFileForEpisode(compatibleFiles, preferredEpisode)
                    ?: run {
                        // Support series archives where files are named by episode title rather than numbers
                        val anyHasEpNumber = compatibleFiles.any { detectEpisodeNumber(it.name) != null }
                        if (!anyHasEpNumber) {
                            val grouped = compatibleFiles.groupBy { file ->
                                file.name
                                    .replace(Regex("""(?i)\.ia\.(mp4|mkv|webm)$"""), "")
                                    .replace(Regex("""(?i)\.(mp4|mkv|webm|m3u8)$"""), "")
                                    .lowercase(Locale.ROOT)
                            }.entries.sortedBy { it.key }
                            val index = (preferredEpisode - 1).coerceIn(0, grouped.lastIndex)
                            selectBestFileForEpisode(grouped[index].value, null)
                        } else if (compatibleFiles.size == 1) {
                            compatibleFiles.first()
                        } else {
                            null
                        }
                    }
                    ?: return@withContext ArchivePlayableMediaResult.Unavailable(
                        "Can't find Episode $preferredEpisode in '$identifier'."
                    )
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

        // 0. FAST PATH: Check explicit archiveIdentifier + verified title identifiers first!
        val fastPathIds = buildList {
            anime.archiveIdentifier?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
            addAll(resolveVerifiedIdentifiersForTitles(titlesToTry))
        }.distinct()

        if (fastPathIds.isNotEmpty()) {
            val fastMatches = coroutineScope {
                fastPathIds.map { candidateId ->
                    async {
                        val fastMeta = getItemMetadata(candidateId) ?: return@async null
                        val fastDoc = ArchiveDocItem(
                            identifier = candidateId,
                            title = fastMeta.metadata?.title ?: anime.displayTitle
                        )
                        matchService.evaluateCandidate(
                            doc = fastDoc,
                            metadata = fastMeta,
                            anime = anime.copy(archiveIdentifier = candidateId),
                            requestedEpisode = if (isMovieFormat) null else episodeNumber,
                            seasonNumber = activeSeason,
                            isLiveActionAllowed = isLiveActionAllowed
                        )
                    }
                }.awaitAll().filterNotNull()
            }
            if (fastMatches.isNotEmpty()) {
                val sortedFast = fastMatches.sortedWith(
                    compareByDescending<ArchiveCandidateMatch> { it.isEpisodeSpecific }
                        .thenByDescending { it.matchScore }
                        .thenByDescending { it.fileSizeBytes ?: 0L }
                )
                return@withContext ArchiveAnimeMatchResult.Success(
                    bestMatch = sortedFast.first(),
                    allCandidates = sortedFast,
                    animeTitle = anime.displayTitle,
                    requestedEpisode = episodeNumber
                )
            }
        }

        val candidateDocs = LinkedHashMap<String, ArchiveDocItem>()
        val epPadded = if (!isMovieFormat && episodeNumber != null && episodeNumber > 0) {
            String.format(Locale.ROOT, "%02d", episodeNumber)
        } else null

        val exclusionClause = if (isLiveActionAllowed) LIVE_ACTION_SEARCH_EXCLUSION_CLAUSE else SEARCH_EXCLUSION_CLAUSE
        val mediaTypeClause = if (isLiveActionAllowed) "mediatype:(movies)" else "(mediatype:(movies) OR mediatype:(animation))"

        // Build targeted search queries (sorted by downloads desc via InternetArchiveApiService)
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

        // Execute search queries concurrently with a 7.5s timeout
        coroutineScope {
            searchQueries.distinct().take(4).map { query ->
                async {
                    try {
                        val res = withTimeoutOrNull(7500L) {
                            apiService.searchArchive(query = query, rows = 20)
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

        // Inspect top 8 candidate items in parallel
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
            .replace(Regex("(?i)\\b(season\\s*\\d+|2nd season|3rd season|4th season|final season|part\\s*\\d+|cour\\s*\\d+)\\b.*"), "")
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
