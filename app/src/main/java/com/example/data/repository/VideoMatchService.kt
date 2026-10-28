package com.example.data.repository

import com.example.data.local.AnimeEntity
import com.example.data.remote.ArchiveDocItem
import com.example.data.remote.ArchiveFileItem
import com.example.data.remote.ArchiveMetadataResponse
import com.example.data.remote.InternetArchiveApiService
import java.util.Locale

/**
 * Service responsible for matching Anime series and episode requests
 * with candidate items and files.
 *
 * Strictly enforces:
 * - Exact title token alignment across English, Romaji, Native, and Pinyin titles
 * - Exact episode number alignment (never falling back to random episodes when an episode is requested)
 * - Strict TV episode duration window (9 to 34 minutes for regular episodes) to prevent both 2-3 min clips
 *   AND 35-180 min long reaction videos or live-action feature movies from ever matching a TV episode
 * - Comprehensive exclusion of reaction videos, commentary, watchalongs, live-action adaptations,
 *   stage plays, dramas, cosplay, fan edits, AMVs, trailers, openings/endings, reviews, and gameplay
 */
class VideoMatchService {

    companion object {
        // Minimum duration for a full episode (9 minutes = 540 seconds) so short clips/reactions/trailers never play
        const val MIN_EPISODE_DURATION_SECONDS = 540f
        // Minimum duration for a full feature movie (35 minutes = 2100 seconds) so trailers/reviews/explanations never play
        const val MIN_MOVIE_DURATION_SECONDS = 2100f
        // Maximum duration for a standard TV anime episode (34 minutes = 2040 seconds)
        const val MAX_STANDARD_EPISODE_DURATION_SECONDS = 2040f
        // Maximum duration for Episode 1 (55 minutes = 3300 seconds) to allow double-length TV premieres
        const val MAX_PREMIERE_EPISODE_DURATION_SECONDS = 3300f
        // Maximum duration for a live-action TV series episode (95 minutes = 5700 seconds)
        const val MAX_LIVE_ACTION_SERIES_EPISODE_SECONDS = 5700f
        // Canonical TV anime episode duration window (19m to 27.5m = 1140s..1650s)
        const val CANONICAL_EPISODE_MIN_SECONDS = 1140f
        const val CANONICAL_EPISODE_MAX_SECONDS = 1650f

        // Minimum file size for a full episode when duration metadata is missing (15 MB)
        const val MIN_EPISODE_FILE_SIZE_BYTES = 15_000_000L
        // Minimum file size for a full movie (45 MB) so short trailers/reviews/explanations never pass
        const val MIN_MOVIE_FILE_SIZE_BYTES = 45_000_000L

        private val STOP_WORDS = setOf(
            "the", "and", "for", "with", "from", "into", "unto", "upon",
            "animation", "animated", "anime", "series", "season", "episode", "episodes",
            "sub", "dub", "eng", "english", "subbed", "dubbed", "uncensored",
            "complete", "collection", "batch", "full", "tv", "ova", "ona", "hd", "1080p", "720p", "480p",
            "movie", "film", "hindi", "bollywood", "hollywood"
        )

        // Universal blocked keywords in titles, identifiers, subjects, and filenames:
        // trailers, teasers, reviews, explanations, breakdowns, reactions, recaps, promos, clips, extras, etc.
        private val UNIVERSAL_EXCLUDED_KEYWORDS = listOf(
            // Trailers / Teasers / Promos / Previews
            "trailer", "trailers", "official trailer", "teaser", "teasers", "promo", "promotional",
            "preview", "pv", "tv spot", "commercial", "bumper", "sneak peek", "first look",
            // Reviews / Explanations / Breakdowns / Recaps / Video Essays
            "review", "reviews", "movie review", "film review", "series review", "episode review",
            "explained", "explanation", "ending explained", "story explained", "plot explained",
            "breakdown", "analysis", "video essay", "recap", "in minutes",
            "spoiler review", "non spoiler", "theory", "theories",
            "top 10", "ranking", "tier list", "everything wrong with", "honest trailer",
            "pitch meeting", "facts you didnt know", "easter eggs", "hidden details",
            // Reaction / Commentary / Watchalong / Streamer content
            "reaction", "reactions", "reacts", "reacting", "reactor", "reactors", "react",
            "first time watching", "watching for the first time", "blind reaction", "full reaction",
            "uncut reaction", "patreon", "live reaction", "group reaction", "mashup",
            "commentary", "audio commentary", "watch along", "watchalong", "watch party",
            "twitch", "twitch vod", "livestream", "live stream", "stream archive",
            "youtuber", "vtuber", "facecam", "reaction cam", "timer sync",
            "rttv", "yaaboyroshi", "semblance of sanity", "carlie and ange", "strayblitz",
            "heatah", "nicholas light", "fandub", "fan dub",
            // Behind the scenes / Interviews / Clips / Shorts / Edits / Audio / Gameplay / Extras
            "cast interview", "interview", "behind the scenes", "making of", "featurette",
            "bloopers", "outtakes", "deleted scenes", "screen test", "live show", "concert",
            "amv", "creditless", "ncop", "nced", "ost", "soundtrack",
            "theme song", "music video", "fan animation", "fanmade", "fan made", "fan edit",
            "fan film", "fanfilm", "tribute", "parody", "spoof", "abridged", "podcast",
            "discussion", "thoughts on", "gameplay", "walkthrough", "playthrough", "lets play",
            "cutscene", "cutscenes", "short clip", "clips", "best moments", "funny moments",
            "highlights", "tiktok", "shorts", "reels", "speedpaint", "drawing",
            "unboxing", "figure review", "cover song", "vocal cover", "guitar cover",
            "piano cover", "drum cover", "remix", "nightcore", "8d audio", "vhs rip promo",
            "extras", "bonus", "special features", "sample", "among us", "vlog", "vlogs",
            "hentai", "futa", "asmr"
        )

        // Phrases checked in description text (strictly non-movie phrases so plot summaries aren't falsely flagged)
        private val DESCRIPTION_EXCLUDED_PHRASES = listOf(
            "official trailer", "movie review", "film review", "ending explained", "plot explained",
            "video essay", "first time watching", "watching for the first time", "blind reaction",
            "full reaction", "uncut reaction", "live reaction", "patreon", "watchalong",
            "audio commentary", "watch along", "watch party", "twitch vod", "behind the scenes",
            "deleted scenes", "fan animation", "fan edit", "fan film", "stage play",
            "walkthrough", "playthrough"
        )

        // Additional exclusions strictly for Anime (preventing live-action adaptations / cosplay from matching anime)
        private val ANIME_ONLY_EXCLUDED_KEYWORDS = listOf(
            "live action", "liveaction", "stage play", "stageplay", "stage musical",
            "2 5d", "tv drama", "jdrama", "j drama", "dorama", "cosplay",
            "netflix adaptation", "netflix live", "warner bros live",
            "hollywood adaptation", "real life", "in real life", "irl",
            "tokusatsu", "super sentai", "kamen rider"
        )
    }

    /**
     * Determines whether a candidate item is a verified exact match for the target anime/movie/series
     * and requested episode, inspects its files deeply, and builds a candidate match record.
     */
    fun evaluateCandidate(
        doc: ArchiveDocItem,
        metadata: ArchiveMetadataResponse,
        anime: AnimeEntity,
        requestedEpisode: Int?,
        seasonNumber: Int? = null,
        isLiveActionAllowed: Boolean = false
    ): ArchiveCandidateMatch? {
        val targetTitles = extractSearchTitles(anime)
        val docTitle = doc.title?.takeIf { it.isNotBlank() }
            ?: metadata.metadata?.title?.takeIf { it.isNotBlank() }
            ?: doc.identifier
        val docDesc = doc.description ?: metadata.metadata?.description.orEmpty()
        val docSubject = doc.subject ?: metadata.metadata?.subject.orEmpty()
        val docCreator = doc.creator ?: metadata.metadata?.creator.orEmpty()
        val identifier = doc.identifier

        // 1. Reject trailers, reviews, explanations, reactions, clips, etc.
        if (containsExcludedKeywords(docTitle, docSubject, identifier, docDesc, docCreator, isLiveActionAllowed)) {
            return null
        }

        // 2. Calculate strict title match against item title, identifier, and file names
        val files = metadata.files.orEmpty()
        val isMovieFormat = anime.format.equals("MOVIE", ignoreCase = true)
        val totalEpisodes = anime.totalEpisodes ?: 0
        val isSingleEpisodeWork = isMovieFormat || (totalEpisodes == 1 && (requestedEpisode == null || requestedEpisode <= 1))

        val compatibleFiles = filterCompatibleVideoFiles(
            files = files,
            requireFullEpisodeLength = true,
            requestedEpisode = requestedEpisode,
            isMovieOrSingleWork = isSingleEpisodeWork,
            isLiveActionAllowed = isLiveActionAllowed
        )
        if (compatibleFiles.isEmpty()) {
            return null
        }

        val itemTitleScore = calculateStrictTitleSimilarity(docTitle, identifier, targetTitles)
        val bestFileTitleScore = compatibleFiles.maxOfOrNull { file ->
            calculateStrictTitleSimilarity(file.title ?: file.name, file.name, targetTitles)
        } ?: 0f

        val isDirectKnownIdentifier = anime.archiveIdentifier?.equals(identifier, ignoreCase = true) == true
        val effectiveTitleScore = if (isDirectKnownIdentifier) {
            maxOf(itemTitleScore, bestFileTitleScore, 0.92f)
        } else {
            maxOf(itemTitleScore, bestFileTitleScore)
        }
        if (effectiveTitleScore < 0.65f) {
            return null
        }

        // 3. Check season alignment if the target specifies a season
        val targetSeason = seasonNumber ?: extractSeasonNumber(targetTitles)
        if (targetSeason != null && targetSeason > 1 && !isDirectKnownIdentifier) {
            val docSeason = extractSeasonNumber(listOf(docTitle, identifier))
            if (docSeason != null && docSeason != targetSeason) {
                return null
            }
        }

        // 4. Strictly resolve the exact file for the requested episode and season
        val bestFile: ArchiveFileItem
        val detectedEp: Int?
        val isEpisodeSpecific: Boolean

        if (requestedEpisode != null && requestedEpisode > 0 && !isSingleEpisodeWork) {
            val exactEpisodeFile = selectExactFileForEpisode(
                files = compatibleFiles,
                docTitle = docTitle,
                identifier = identifier,
                requestedEpisode = requestedEpisode,
                targetTitles = targetTitles,
                itemTitleScore = effectiveTitleScore,
                seasonNumber = targetSeason
            ) ?: return null

            bestFile = exactEpisodeFile
            detectedEp = requestedEpisode
            isEpisodeSpecific = true
        } else {
            bestFile = selectBestFileForEpisode(compatibleFiles, null, targetTitles) ?: compatibleFiles.first()
            detectedEp = detectEpisodeNumber(bestFile.name) ?: detectEpisodeNumber(docTitle) ?: requestedEpisode
            isEpisodeSpecific = true
        }

        // Double check that the chosen file itself is not a trailer/review/explanation/reaction/short clip
        if (containsExcludedKeywords(bestFile.name, bestFile.title.orEmpty(), "", isLiveActionAllowed = isLiveActionAllowed)) {
            return null
        }
        if (!isValidEpisodeDurationAndSize(
                file = bestFile,
                requireFullLength = true,
                requestedEpisode = requestedEpisode,
                isMovieOrSingleWork = isSingleEpisodeWork,
                isLiveActionAllowed = isLiveActionAllowed
            )
        ) {
            return null
        }

        // Give a strong score bonus if the file duration is within the canonical 19-27.5 min TV anime episode window
        val fileDurationSecs = parseDurationSeconds(bestFile.length)
        val canonicalDurationBonus = if (!isSingleEpisodeWork && !isLiveActionAllowed && fileDurationSecs != null &&
            fileDurationSecs in CANONICAL_EPISODE_MIN_SECONDS..CANONICAL_EPISODE_MAX_SECONDS
        ) {
            0.12f
        } else {
            0f
        }

        val finalScore = (effectiveTitleScore * 0.7f + 0.2f + canonicalDurationBonus).coerceIn(0.0f, 1.0f)
        val confidence = when {
            finalScore >= 0.85f -> MatchConfidence.HIGH
            finalScore >= 0.70f -> MatchConfidence.MEDIUM
            else -> MatchConfidence.LOW
        }

        val sizeBytes = bestFile.size?.toLongOrNull()
        val durationFormatted = formatSeconds(fileDurationSecs)
        val streamUrl = InternetArchiveApiService.getDirectDownloadUrl(doc.identifier, bestFile.name)
        val qualityOptions = extractQualityOptions(doc.identifier, compatibleFiles, if (isSingleEpisodeWork) null else requestedEpisode)
        val subtitleRepository = SubtitleRepository()
        val subtitleTracks = subtitleRepository.extractArchiveSubtitles(doc.identifier, files, if (isSingleEpisodeWork) null else requestedEpisode)

        return ArchiveCandidateMatch(
            identifier = doc.identifier,
            title = docTitle,
            description = cleanHtml(docDesc),
            creator = doc.creator ?: metadata.metadata?.creator,
            year = doc.year ?: metadata.metadata?.year,
            licenseUrl = doc.licenseurl ?: metadata.metadata?.licenseurl,
            thumbnailUrl = InternetArchiveApiService.getThumbnailUrl(doc.identifier),
            matchedEpisode = detectedEp,
            compatibleFiles = compatibleFiles,
            bestPlayableFile = bestFile,
            matchScore = finalScore,
            matchConfidence = confidence,
            isEpisodeSpecific = isEpisodeSpecific,
            durationFormatted = durationFormatted,
            fileSizeBytes = sizeBytes,
            streamUrl = streamUrl,
            qualityOptions = qualityOptions,
            subtitleTracks = subtitleTracks
        )
    }

    /**
     * Checks whether title, subject, identifier, description, or creator contains keywords indicating
     * trailers, reviews, explanations, breakdowns, reactions, commentary, clips, or promos.
     */
    fun containsExcludedKeywords(
        title: String,
        subject: String = "",
        identifier: String = "",
        description: String = "",
        creator: String = "",
        isLiveActionAllowed: Boolean = false
    ): Boolean {
        val rawHeader = "$title $subject $identifier $creator"
        val normalizedHeader = normalizeForKeywordCheck(rawHeader)
        if (normalizedHeader.isNotBlank()) {
            // Reject YouTube or Twitch auto-archived clips by identifier prefix
            val lowerId = identifier.lowercase(Locale.ROOT)
            if (lowerId.startsWith("youtube-") || lowerId.startsWith("twitch-")) {
                return true
            }
            for (keyword in UNIVERSAL_EXCLUDED_KEYWORDS) {
                val regex = Regex("""\b${Regex.escape(keyword)}\b""")
                if (regex.containsMatchIn(normalizedHeader)) {
                    return true
                }
            }
            if (!isLiveActionAllowed) {
                for (keyword in ANIME_ONLY_EXCLUDED_KEYWORDS) {
                    val regex = Regex("""\b${Regex.escape(keyword)}\b""")
                    if (regex.containsMatchIn(normalizedHeader)) {
                        return true
                    }
                }
            }
        }

        if (description.isNotBlank()) {
            val normalizedDesc = normalizeForKeywordCheck(description.take(400))
            for (phrase in DESCRIPTION_EXCLUDED_PHRASES) {
                val regex = Regex("""\b${Regex.escape(phrase)}\b""")
                if (regex.containsMatchIn(normalizedDesc)) {
                    return true
                }
            }
            if (!isLiveActionAllowed) {
                for (keyword in ANIME_ONLY_EXCLUDED_KEYWORDS) {
                    val regex = Regex("""\b${Regex.escape(keyword)}\b""")
                    if (regex.containsMatchIn(normalizedDesc)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun normalizeForKeywordCheck(text: String): String {
        return text
            .replace(Regex("""([a-z])([A-Z])"""), "$1 $2") // Split camelCase
            .lowercase(Locale.ROOT)
            .replace(Regex("""[_\-./\[\]()+:;|,\\"{}]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Validates that a file is a genuine full-length episode or full-length movie
     * (never a 1-20 minute trailer, review, explanation video, or clip).
     */
    fun isValidEpisodeDurationAndSize(
        file: ArchiveFileItem,
        requireFullLength: Boolean = true,
        requestedEpisode: Int? = null,
        isMovieOrSingleWork: Boolean = false,
        isLiveActionAllowed: Boolean = false
    ): Boolean {
        val durationSecs = parseDurationSeconds(file.length)
        val sizeBytes = file.size?.toLongOrNull()

        if (!requireFullLength) {
            return (sizeBytes == null || sizeBytes > 15_000_000L)
        }

        if (durationSecs != null && durationSecs > 0f) {
            if (isMovieOrSingleWork) {
                // Full-length movie must be at least 35 minutes (blocks all trailers, reviews, explanations, clips)
                if (durationSecs < MIN_MOVIE_DURATION_SECONDS) {
                    return false
                }
            } else {
                // 1. Reject short clips, openings, endings, trailers, reviews, explanations (< 9 minutes)
                if (durationSecs < MIN_EPISODE_DURATION_SECONDS) {
                    return false
                }

                // 2. Upper bound check for TV series episodes (allow multi-episode compilations only if live action or premiere)
                if (requestedEpisode != null && requestedEpisode > 0) {
                    val maxAllowedSecs = when {
                        isLiveActionAllowed -> MAX_LIVE_ACTION_SERIES_EPISODE_SECONDS
                        requestedEpisode == 1 -> MAX_PREMIERE_EPISODE_DURATION_SECONDS
                        else -> MAX_STANDARD_EPISODE_DURATION_SECONDS
                    }
                    if (durationSecs > maxAllowedSecs) {
                        return false
                    }
                }
            }
        }

        // File size check (blocks small trailers/reviews/clips when duration metadata is absent)
        val isHls = file.name.endsWith(".m3u8", ignoreCase = true)
        if (!isHls && sizeBytes != null && sizeBytes > 0L) {
            val minRequiredBytes = if (isMovieOrSingleWork) MIN_MOVIE_FILE_SIZE_BYTES else MIN_EPISODE_FILE_SIZE_BYTES
            if (sizeBytes < minRequiredBytes) {
                return false
            }
        }

        // If both duration and size are missing on a non-HLS file, reject unverified stub
        if (!isHls && durationSecs == null && sizeBytes == null) {
            return false
        }

        return true
    }

    /**
     * Parses `length` field which can be either float seconds ("1420.5")
     * or formatted "MM:SS" / "HH:MM:SS" ("23:40" or "00:23:40").
     */
    fun parseDurationSeconds(lengthStr: String?): Float? {
        if (lengthStr.isNullOrBlank()) return null
        val trimmed = lengthStr.trim()
        trimmed.toFloatOrNull()?.let { return it }

        if (trimmed.contains(":")) {
            val parts = trimmed.split(":").mapNotNull { it.trim().toFloatOrNull() }
            return when (parts.size) {
                2 -> parts[0] * 60f + parts[1]
                3 -> parts[0] * 3600f + parts[1] * 60f + parts[2]
                else -> null
            }
        }
        return null
    }

    /**
     * Extracts distinct quality options from compatible files strictly for the requested episode.
     */
    fun extractQualityOptions(
        identifier: String,
        compatibleFiles: List<ArchiveFileItem>,
        requestedEpisode: Int?
    ): List<VideoQualityOption> {
        val relevantFiles = if (requestedEpisode != null && requestedEpisode > 0) {
            val epFiles = compatibleFiles.filter { detectEpisodeNumber(it.name) == requestedEpisode }
            if (epFiles.isNotEmpty()) epFiles else compatibleFiles.take(1)
        } else {
            compatibleFiles
        }

        return relevantFiles.map { file ->
            val height = file.height?.toIntOrNull() ?: detectResolutionFromName(file.name)
            val label = when {
                height != null && height >= 1080 -> "1080p (Full HD)"
                height != null && height >= 720 -> "720p (HD)"
                height != null && height >= 480 -> "480p (SD)"
                height != null && height >= 360 -> "360p"
                file.name.contains(".m3u8", ignoreCase = true) -> "Adaptive HLS (Auto)"
                file.format?.contains("512Kb", ignoreCase = true) == true -> "Low Bitrate (Mobile)"
                else -> file.format?.takeIf { it.isNotBlank() } ?: "Direct Stream"
            }
            VideoQualityOption(
                label = label,
                height = height,
                file = file,
                streamUrl = InternetArchiveApiService.getDirectDownloadUrl(identifier, file.name),
                sizeBytes = file.size?.toLongOrNull()
            )
        }.distinctBy { it.label }.sortedByDescending { it.height ?: 0 }
    }

    private fun detectResolutionFromName(filename: String): Int? {
        val lower = filename.lowercase(Locale.ROOT)
        return when {
            lower.contains("1080p") || lower.contains("1920x1080") -> 1080
            lower.contains("720p") || lower.contains("1280x720") -> 720
            lower.contains("480p") || lower.contains("854x480") || lower.contains("640x480") -> 480
            lower.contains("360p") || lower.contains("640x360") -> 360
            else -> null
        }
    }

    /**
     * Filters files in an archive item down to playable video files.
     * Excludes metadata XMLs, torrents, covers, subtitle files, reaction/live-action/promo clips, and invalid durations.
     */
    fun filterCompatibleVideoFiles(
        files: List<ArchiveFileItem>,
        requireFullEpisodeLength: Boolean = true,
        requestedEpisode: Int? = null,
        isMovieOrSingleWork: Boolean = false,
        isLiveActionAllowed: Boolean = false
    ): List<ArchiveFileItem> {
        return files.filter { file ->
            val name = file.name.lowercase(Locale.ROOT)
            val format = file.format?.lowercase(Locale.ROOT).orEmpty()

            // Exclude bonus/extras/sample subfolders
            if (name.contains("/extras/") || name.contains("/bonus/") || name.contains("/samples/") || name.contains("/featurettes/")) {
                return@filter false
            }

            val isExcludedExtension = name.endsWith(".xml") || name.endsWith(".sqlite") || name.endsWith(".torrent") ||
                    name.endsWith(".txt") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                    name.endsWith(".gif") || name.endsWith(".webp") || name.contains("_thumb.") ||
                    name.contains("_ia.thumb") || name.endsWith(".srt") || name.endsWith(".vtt") ||
                    name.endsWith(".ass") || name.endsWith(".ssa") || name.endsWith(".mp3") ||
                    name.endsWith(".flac") || name.endsWith(".ogg") || name.endsWith(".wav") ||
                    name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") ||
                    name.endsWith(".pdf") || name.endsWith(".epub")

            if (isExcludedExtension) return@filter false

            if (containsExcludedKeywords(file.name, file.title.orEmpty(), "", isLiveActionAllowed = isLiveActionAllowed)) {
                return@filter false
            }

            val isVideo = name.endsWith(".mp4") || name.endsWith(".m3u8") || name.endsWith(".webm") ||
                    name.endsWith(".mkv") || format.contains("h.264") || format.contains("mpeg4") ||
                    format.contains("hls") || format.contains("matroska") || format.contains("vp9")

            if (!isVideo) return@filter false

            isValidEpisodeDurationAndSize(
                file = file,
                requireFullLength = requireFullEpisodeLength,
                requestedEpisode = requestedEpisode,
                isMovieOrSingleWork = isMovieOrSingleWork,
                isLiveActionAllowed = isLiveActionAllowed
            )
        }
    }

    /**
     * Strictly selects a video file that matches `requestedEpisode` and belongs to the target anime/series.
     */
    fun selectExactFileForEpisode(
        files: List<ArchiveFileItem>,
        docTitle: String,
        identifier: String,
        requestedEpisode: Int,
        targetTitles: List<String>,
        itemTitleScore: Float = 1.0f,
        seasonNumber: Int? = null
    ): ArchiveFileItem? {
        if (files.isEmpty()) return null

        val targetSeason = seasonNumber ?: extractSeasonNumber(targetTitles)

        // Strict season check: If Archive Item declares a conflicting season, never match files from it
        if (targetSeason != null) {
            val itemSeason = detectSeasonNumber(docTitle) ?: detectSeasonNumber(identifier)
            if (itemSeason != null && itemSeason != targetSeason) {
                return null
            }
        }

        var candidateFiles = files
        if (targetSeason != null) {
            // Prioritize files explicitly stamped with the requested season (e.g. S02E01)
            val strictSeasonFiles = files.filter { file ->
                val fSeason = detectSeasonNumber(file.name) ?: detectSeasonNumber(file.title.orEmpty())
                fSeason == targetSeason
            }
            if (strictSeasonFiles.isNotEmpty()) {
                candidateFiles = strictSeasonFiles
            } else {
                // Otherwise only accept files that do NOT declare a conflicting season number
                val nonConflictingSeasonFiles = files.filter { file ->
                    val fSeason = detectSeasonNumber(file.name) ?: detectSeasonNumber(file.title.orEmpty())
                    fSeason == null || fSeason == targetSeason
                }
                if (nonConflictingSeasonFiles.isNotEmpty()) {
                    candidateFiles = nonConflictingSeasonFiles
                }
            }
        }

        // 1. Check files whose filename or file title explicitly contains the requested episode number
        val matchingFiles = candidateFiles.filter { file ->
            val fileEp = detectEpisodeNumber(file.name) ?: detectEpisodeNumber(file.title.orEmpty())
            fileEp == requestedEpisode
        }

        if (matchingFiles.isNotEmpty()) {
            val titleMatchedFiles = matchingFiles.filter { file ->
                calculateStrictTitleSimilarity(file.title ?: file.name, file.name, targetTitles) >= 0.55f
            }
            if (titleMatchedFiles.isNotEmpty()) {
                return pickPreferredFormatFile(titleMatchedFiles, isSingleEpisodeWork = false)
            }

            // When the parent Archive Item already matches the anime/series title (itemTitleScore >= 0.70f),
            // episode files are frequently named by episode number + episode subtitle (e.g. "01 - Angel Attack.mp4")
            // or release group code (e.g. "HxH99.Remastered.01.v5.0.mkv").
            if (itemTitleScore >= 0.70f) {
                return pickPreferredFormatFile(matchingFiles, isSingleEpisodeWork = false)
            }
            return null
        }

        // 2. If the Archive item itself is a single-episode upload whose item title or identifier
        // explicitly states the requested episode (e.g. "Naruto Episode 01"), and none of the video files
        // have conflicting episode numbers:
        val itemLevelEp = detectEpisodeNumber(docTitle) ?: detectEpisodeNumber(identifier)
        if (itemLevelEp == requestedEpisode && itemTitleScore >= 0.72f) {
            val nonConflictingFiles = candidateFiles.filter { file ->
                val fEp = detectEpisodeNumber(file.name)
                fEp == null || fEp == requestedEpisode
            }
            if (nonConflictingFiles.isNotEmpty()) {
                return pickPreferredFormatFile(nonConflictingFiles, isSingleEpisodeWork = false)
            }
        }

        // 3. If the Archive item strongly matches the series title (e.g. SherlockHolmesGranada or byomkesh-bakshi-movie-world)
        // and contains multiple episodes named by episode title rather than numbers:
        val filesWithDetectedEp = candidateFiles.count {
            (detectEpisodeNumber(it.name) ?: detectEpisodeNumber(it.title.orEmpty())) != null
        }
        if (itemTitleScore >= 0.80f && filesWithDetectedEp == 0 && candidateFiles.size >= 2) {
            val groupedByStem = candidateFiles.groupBy { file ->
                file.name
                    .replace(Regex("""(?i)\.ia\.(mp4|mkv|webm)$"""), "")
                    .replace(Regex("""(?i)\.(mp4|mkv|webm|m3u8)$"""), "")
                    .lowercase(Locale.ROOT)
            }.entries.sortedBy { it.key }
            if (requestedEpisode - 1 in groupedByStem.indices) {
                return pickPreferredFormatFile(groupedByStem[requestedEpisode - 1].value, isSingleEpisodeWork = false)
            }
        }

        return null
    }

    /**
     * Picks the best file from compatible files matching an episode number or target movie title,
     * prioritizing canonical ~24m episode duration for TV episodes and high-bitrate H.264 MP4 / MKV for movies.
     */
    fun selectBestFileForEpisode(
        files: List<ArchiveFileItem>,
        preferredEpisode: Int?,
        targetTitles: List<String> = emptyList()
    ): ArchiveFileItem? {
        if (files.isEmpty()) return null

        if (preferredEpisode != null && preferredEpisode > 0) {
            val matchingEpisodeFiles = files.filter { file ->
                (detectEpisodeNumber(file.name) ?: detectEpisodeNumber(file.title.orEmpty())) == preferredEpisode
            }
            if (matchingEpisodeFiles.isNotEmpty()) {
                return pickPreferredFormatFile(matchingEpisodeFiles, isSingleEpisodeWork = false)
            }
            return null
        }

        // When selecting a movie / single work from an item that may have multiple files,
        // strongly prefer files whose filename matches targetTitles so multi-movie packs play the exact requested movie!
        if (targetTitles.isNotEmpty() && files.size > 1) {
            val scoredFiles = files.map { file ->
                file to calculateStrictTitleSimilarity(file.title ?: file.name, file.name, targetTitles)
            }
            val maxScore = scoredFiles.maxOfOrNull { it.second } ?: 0f
            if (maxScore >= 0.60f) {
                val matchingMovieFiles = scoredFiles.filter { it.second >= maxScore - 0.15f }.map { it.first }
                return pickPreferredFormatFile(matchingMovieFiles, isSingleEpisodeWork = true)
            }
        }

        return pickPreferredFormatFile(files, isSingleEpisodeWork = true)
    }

    private fun pickPreferredFormatFile(
        pool: List<ArchiveFileItem>,
        isSingleEpisodeWork: Boolean = false
    ): ArchiveFileItem? {
        if (pool.isEmpty()) return null

        // Only prefer canonical 19-27.5m duration when selecting a TV episode (never for feature movies!)
        val activePool = if (!isSingleEpisodeWork) {
            val canonicalDurationPool = pool.filter { file ->
                val dur = parseDurationSeconds(file.length)
                dur != null && dur in CANONICAL_EPISODE_MIN_SECONDS..CANONICAL_EPISODE_MAX_SECONDS
            }
            if (canonicalDurationPool.isNotEmpty()) canonicalDurationPool else pool
        } else {
            pool
        }

        val maxPoolSize = activePool.maxOfOrNull { it.size?.toLongOrNull() ?: 0L } ?: 0L

        // 1. High-quality H.264 MP4 (excluding low-res 512Kb MPEG4 derivatives, and for movies ensuring it's not a tiny bonus clip)
        val highResMp4 = activePool.filter {
            val size = it.size?.toLongOrNull() ?: 0L
            it.name.endsWith(".mp4", ignoreCase = true) &&
                    it.format?.contains("512kb", ignoreCase = true) != true &&
                    (!isSingleEpisodeWork || maxPoolSize <= 0L || size >= 150_000_000L || size >= (maxPoolSize * 0.25).toLong())
        }.maxByOrNull { it.size?.toLongOrNull() ?: 0L }
        if (highResMp4 != null) return highResMp4

        // 2. Any MP4 (if not dwarfed by a main MKV movie file)
        val anyMp4 = activePool.filter {
            val size = it.size?.toLongOrNull() ?: 0L
            it.name.endsWith(".mp4", ignoreCase = true) &&
                    (!isSingleEpisodeWork || maxPoolSize <= 0L || size >= 100_000_000L || size >= (maxPoolSize * 0.20).toLong())
        }.maxByOrNull { it.size?.toLongOrNull() ?: 0L }
        if (anyMp4 != null) return anyMp4

        // 3. MKV / WebM
        val mkvOrWebm = activePool.filter {
            it.name.endsWith(".mkv", ignoreCase = true) || it.name.endsWith(".webm", ignoreCase = true)
        }.maxByOrNull { it.size?.toLongOrNull() ?: 0L }
        if (mkvOrWebm != null) return mkvOrWebm

        // 4. Fallback to any MP4 or HLS
        val fallbackMp4 = activePool.filter { it.name.endsWith(".mp4", ignoreCase = true) }
            .maxByOrNull { it.size?.toLongOrNull() ?: 0L }
        if (fallbackMp4 != null) return fallbackMp4

        val hls = activePool.firstOrNull { it.name.endsWith(".m3u8", ignoreCase = true) }
        if (hls != null) return hls

        return activePool.firstOrNull()
    }

    /**
     * Finds next alternate compatible file in the item for the same episode if the active file fails.
     */
    fun findAlternativePlayableFile(
        allCompatible: List<ArchiveFileItem>,
        failedFileName: String,
        requestedEpisode: Int? = null
    ): ArchiveFileItem? {
        val failedEp = requestedEpisode ?: detectEpisodeNumber(failedFileName)
        return allCompatible.firstOrNull { file ->
            if (file.name == failedFileName) return@firstOrNull false
            if (failedEp != null) {
                val ep = detectEpisodeNumber(file.name)
                ep == null || ep == failedEp
            } else {
                true
            }
        }
    }

    /**
     * Detects episode numbers from common anime/series filename/title patterns:
     * - "S01E01", "S1E12", "1x05"
     * - "Episode 01", "Ep. 2", "Ep 03", "E04", "Capitulo 1", "Episodio 1"
     * - "第01話", "第1集", "第01集"
     * - " - 01 ", " - 01v2", "[01]", "(01)", "_01_", ".01.", "01 - Title.mp4", " 01.mp4"
     */
    fun detectEpisodeNumber(text: String): Int? {
        if (text.isBlank()) return null

        // Strip file extension (including .ia.mp4) and resolution/codec tags so they are never mistaken for episode numbers
        val cleaned = text
            .replace(Regex("""(?i)(?:\.ia)?\.(mp4|mkv|webm|avi|m3u8|ts|mov|flv|ogv)$"""), "")
            .replace(Regex("""(?i)\b(1080p|720p|480p|360p|2160p|4k|1920x1080|1280x720|854x480|640x480|h\.?264|h\.?265|x264|x265|hevc|av1|aac|flac|10bit|8bit|512kb|mpeg4|bluray|bdrip|webrip|web-dl|dvdrip|aac2\.0|ddp5\.1|dd5\.1)\b"""), " ")
            .replace(Regex("""\[[0-9A-Fa-f]{8}\]"""), " ") // CRC32 hashes like [A1B2C3D4]
            .replace(Regex("""\b(19\d{2}|20\d{2})\b"""), " ") // Years like 1999, 2024

        // 1. S01E01 or 1x01 format (including S01.E01 or S02E01)
        val sxeRegex = Regex("""(?i)(?:s\d{1,2}[._\s-]*e|(?:\b\d{1,2}x))0*(\d{1,4})(?:\b|v\d|_|\.)""")
        sxeRegex.find(cleaned)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..2000) return num
        }

        // 2. Explicit "Episode 01", "Ep. 01", "Ep 1", "EP01", "E01", "Capitulo 1", "Episodio 1", "Bölüm 1"
        val epRegex = Regex("""(?i)(?:^|[\s._\-\[\(/])(?:episode|episodio|ep|capitulo|capítulo|cap|bölüm|bolum|e)[.\s_-]*0*(\d{1,4})(?:v\d)?(?:$|[\s._\-\]\)/])""")
        epRegex.find(cleaned)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..2000) return num
        }

        // 3. CJK Episode markers: 第01話, 第1集, 第01回
        val cjkRegex = Regex("""第\s*0*(\d{1,4})\s*[話话集回]""")
        cjkRegex.find(cleaned)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..2000) return num
        }

        // 4. Dash separator: "Title - 01", "Title_-_01", "Title - 01v2"
        val dashRegex = Regex("""[\s_]+-\s*0*(\d{1,4})(?:v\d)?(?:$|[\s._\-\[\(])""")
        dashRegex.find(cleaned)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..2000) return num
        }

        // 5. Leading episode number at start of filename (e.g. "01 - Angel Attack", "01. Title", "Season 1/01 Pilot")
        val fileNameOnly = cleaned.substringAfterLast('/').trim()
        val leadingRegex = Regex("""^0*(\d{1,3})(?:v\d)?(?:\s*[-._]\s*|\s+)[A-Za-z\u3040-\u30ff\u4e00-\u9fff]""")
        leadingRegex.find(fileNameOnly)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..500) return num
        }

        // 6. Brackets or parentheses: "[01]", "(01)"
        val bracketRegex = Regex("""[\[\(]0*(\d{1,4})(?:v\d)?[\]\)]""")
        bracketRegex.find(cleaned)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..2000) return num
        }

        // 7. Dot or underscore delimited number before version or end: "HxH99.Remastered.01.v5.0" or "Anime.01"
        val dotRegex = Regex("""[._]0*(\d{1,3})(?:\.v\d+(?:\.\d+)?)?$""")
        dotRegex.find(fileNameOnly)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..500) return num
        }

        // 8. Trailing number at end of filename: "Anime_Name_01" or "Anime Name 01"
        val trailingRegex = Regex("""(?:[_\s\-])0*(\d{1,3})(?:v\d)?$""")
        trailingRegex.find(fileNameOnly)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..500) return num
        }

        return null
    }

    fun detectSeasonNumber(filename: String): Int? {
        val lower = filename.lowercase(Locale.ROOT)
        val sEpMatch = Regex("""(?i)\b(?:s|season)\s*0*(\d{1,2})\s*(?:e|ep|episode|\x20|-)\s*0*(\d{1,3})\b""").find(lower)
        if (sEpMatch != null) {
            return sEpMatch.groupValues[1].toIntOrNull()
        }
        val xMatch = Regex("""\b0*(\d{1,2})x0*(\d{1,3})\b""").find(lower)
        if (xMatch != null) {
            return xMatch.groupValues[1].toIntOrNull()
        }
        val seasonOnlyMatch = Regex("""(?i)\b(?:season|s)\s*0*(\d{1,2})\b""").find(lower)
        if (seasonOnlyMatch != null) {
            return seasonOnlyMatch.groupValues[1].toIntOrNull()
        }
        val ordMatch = Regex("""(?i)\b(\d{1,2})(?:nd|rd|th|st)\s+(?:season|cour|part)\b""").find(lower)
        if (ordMatch != null) {
            return ordMatch.groupValues[1].toIntOrNull()
        }
        val partMatch = Regex("""(?i)\b(?:part|cour)\s*0*(\d{1,2})\b""").find(lower)
        if (partMatch != null) {
            return partMatch.groupValues[1].toIntOrNull()
        }
        val romanMatch = Regex("""(?i)\b(?:season|s|part)\s*(ii|iii|iv|v|vi|vii|viii|ix|x)\b""").find(lower)
        if (romanMatch != null) {
            return when (romanMatch.groupValues[1].lowercase(Locale.ROOT)) {
                "ii" -> 2
                "iii" -> 3
                "iv" -> 4
                "v" -> 5
                "vi" -> 6
                "vii" -> 7
                "viii" -> 8
                "ix" -> 9
                "x" -> 10
                else -> null
            }
        }
        return null
    }

    fun extractSeasonNumber(titles: List<String>): Int? {
        for (title in titles) {
            val sMatch = Regex("""(?i)\b(?:season|s)\s*0*(\d{1,2})\b""").find(title)
            if (sMatch != null) {
                val num = sMatch.groupValues[1].toIntOrNull()
                if (num != null && num in 1..20) return num
            }
            val ordMatch = Regex("""(?i)\b(\d{1,2})(?:nd|rd|th|st)\s+(?:season|cour|part)\b""").find(title)
            if (ordMatch != null) {
                val num = ordMatch.groupValues[1].toIntOrNull()
                if (num != null && num in 1..20) return num
            }
            val partMatch = Regex("""(?i)\b(?:part|cour)\s*0*(\d{1,2})\b""").find(title)
            if (partMatch != null) {
                val num = partMatch.groupValues[1].toIntOrNull()
                if (num != null && num in 1..20) return num
            }
            val romanMatch = Regex("""(?i)\b(?:season|s|part)\s*(ii|iii|iv|v|vi|vii|viii|ix|x)\b""").find(title)
            if (romanMatch != null) {
                val num = when (romanMatch.groupValues[1].lowercase(Locale.ROOT)) {
                    "ii" -> 2
                    "iii" -> 3
                    "iv" -> 4
                    "v" -> 5
                    "vi" -> 6
                    "vii" -> 7
                    "viii" -> 8
                    "ix" -> 9
                    "x" -> 10
                    else -> null
                }
                if (num != null) return num
            }
        }
        return null
    }

    fun extractSearchTitles(anime: AnimeEntity): List<String> {
        val titles = mutableListOf<String>()
        anime.titleEnglish?.takeIf { it.isNotBlank() }?.let { titles.add(it) }
        if (anime.titleRomaji.isNotBlank() && !titles.contains(anime.titleRomaji)) {
            titles.add(anime.titleRomaji)
        }
        anime.titleNative?.takeIf { it.isNotBlank() }?.let { titles.add(it) }
        anime.titlePinyin?.takeIf { it.isNotBlank() }?.let { titles.add(it) }

        val expanded = mutableListOf<String>()
        for (t in titles) {
            expanded.add(t)
            if (t.contains(":")) {
                val mainPrefix = t.substringBefore(":").trim()
                if (mainPrefix.length >= 5 && !expanded.contains(mainPrefix)) {
                    expanded.add(mainPrefix)
                }
            }
        }
        return expanded
    }

    /**
     * Strict title similarity calculation that verifies all significant words of the target anime
     * title are present in the candidate title or identifier, preventing false positives where
     * only 1 common word overlaps.
     */
    fun calculateStrictTitleSimilarity(
        docTitle: String,
        identifier: String,
        targetTitles: List<String>
    ): Float {
        val cleanDoc = normalizeTitle(docTitle)
        val cleanId = normalizeTitle(identifier.replace("_", " ").replace("-", " "))
        var maxScore = 0.0f

        for (target in targetTitles) {
            val cleanTarget = normalizeTitle(target)
            if (cleanTarget.isBlank()) continue

            if (cleanDoc == cleanTarget || cleanId == cleanTarget) return 1.0f

            val targetTokens = extractSignificantTokens(cleanTarget)
            if (targetTokens.isEmpty()) continue

            val docTokens = extractSignificantTokens(cleanDoc)
            val idTokens = extractSignificantTokens(cleanId)
            val combinedDocTokens = docTokens + idTokens

            // Check exact phrase containment with word boundaries
            val phraseRegex = Regex("""\b${Regex.escape(cleanTarget)}\b""")
            if (phraseRegex.containsMatchIn(cleanDoc) || phraseRegex.containsMatchIn(cleanId)) {
                val score = 0.90f
                if (score > maxScore) maxScore = score
            }

            // Token coverage: what fraction of the target title's significant words appear in the candidate?
            val matchedTokens = targetTokens.count { tToken ->
                combinedDocTokens.any { dToken -> dToken == tToken }
            }
            val coverage = matchedTokens.toFloat() / targetTokens.size.toFloat()

            // Require at least 75% of target title's significant tokens (or 100% if target has <= 2 tokens)
            val requiredCoverage = if (targetTokens.size <= 2) 1.0f else 0.75f
            if (coverage >= requiredCoverage) {
                val precision = if (docTokens.isNotEmpty()) {
                    targetTokens.intersect(docTokens).size.toFloat() / docTokens.size.toFloat()
                } else 0.5f
                val score = (coverage * 0.75f) + (precision * 0.25f)
                if (score > maxScore) maxScore = score
            }
        }
        return maxScore
    }

    fun calculateTitleSimilarity(docTitle: String, targetTitles: List<String>): Float {
        return calculateStrictTitleSimilarity(docTitle, "", targetTitles)
    }

    private fun extractSignificantTokens(normalizedTitle: String): Set<String> {
        val allTokens = normalizedTitle.split(" ").filter { it.isNotBlank() }
        val filtered = allTokens.filter { token ->
            token.length >= 2 && !STOP_WORDS.contains(token) && token.toIntOrNull() == null
        }.toSet()
        return if (filtered.isNotEmpty()) filtered else allTokens.toSet()
    }

    private fun normalizeTitle(title: String): String {
        return title
            .replace(Regex("""([a-z])([A-Z])"""), "$1 $2")
            .lowercase(Locale.ROOT)
            .replace("'", "")
            .replace("’", "")
            .replace(Regex("""(?i)\.(mp4|mkv|webm|avi|m3u8)$"""), " ")
            .replace(Regex("""[^a-z0-9\u3040-\u30ff\u4e00-\u9fff\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
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

    private fun formatSeconds(seconds: Float?): String? {
        if (seconds == null || seconds <= 0) return null
        val totalSecs = seconds.toInt()
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, secs)
        }
    }
}
