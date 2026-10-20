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
        // Minimum duration for a full episode (9 minutes = 540 seconds) so short clips/reactions never play
        const val MIN_EPISODE_DURATION_SECONDS = 540f
        // Maximum duration for a standard TV episode (34 minutes = 2040 seconds) so long reaction videos (35-90m)
        // and live-action movies (80-150m) never match a regular TV episode
        const val MAX_STANDARD_EPISODE_DURATION_SECONDS = 2040f
        // Maximum duration for Episode 1 (55 minutes = 3300 seconds) to allow double-length TV premieres
        const val MAX_PREMIERE_EPISODE_DURATION_SECONDS = 3300f
        // Canonical TV anime episode duration window (19m to 27.5m = 1140s..1650s)
        const val CANONICAL_EPISODE_MIN_SECONDS = 1140f
        const val CANONICAL_EPISODE_MAX_SECONDS = 1650f

        // Minimum file size for a full episode when duration metadata is missing (15 MB)
        const val MIN_EPISODE_FILE_SIZE_BYTES = 15_000_000L

        private val STOP_WORDS = setOf(
            "the", "and", "for", "with", "from", "into", "unto", "upon",
            "animation", "animated", "anime", "series", "season", "episode", "episodes",
            "sub", "dub", "eng", "english", "subbed", "dubbed", "uncensored",
            "complete", "collection", "batch", "full", "tv", "ova", "ona", "hd", "1080p", "720p", "480p"
        )

        // Keywords that identify non-anime-episode content: reactions, live-action, stage plays, dramas, AMVs, clips, trailers, etc.
        private val EXCLUDED_CONTENT_KEYWORDS = listOf(
            // Reaction / Commentary / Watchalong / Streamer content
            "reaction", "reactions", "reacts", "reacting", "reactor", "reactors", "react",
            "first time watching", "watching for the first time", "blind reaction", "full reaction",
            "uncut reaction", "patreon", "live reaction", "group reaction", "mashup",
            "commentary", "audio commentary", "watch along", "watchalong", "watch party",
            "twitch", "twitch vod", "livestream", "live stream", "stream archive",
            "youtuber", "vtuber", "facecam", "reaction cam", "timer sync",
            "rttv", "yaaboyroshi", "semblance of sanity", "carlie and ange", "strayblitz",
            "heatah", "nicholas light", "alana", "sofy", "fandub", "fan dub",
            // Live-Action / Stage Play / Drama / Cosplay / Real-Life Adaptations
            "live action", "liveaction", "stage play", "stageplay", "stage musical",
            "2 5d", "tv drama", "jdrama", "j drama", "dorama", "cosplay",
            "netflix adaptation", "netflix live", "netflix series", "warner bros live",
            "hollywood adaptation", "fan film", "fanfilm", "real life", "in real life", "irl",
            "tokusatsu", "super sentai", "kamen rider", "cast interview", "behind the scenes",
            "making of", "bloopers", "outtakes", "screen test", "live show", "concert",
            // Clips / Edits / Reviews / Promos / Gameplay
            "amv", "mad", "trailer", "preview", "teaser", "promo", "pv", "cm",
            "opening", "ending", "creditless", "ncop", "nced", "ost", "soundtrack",
            "theme song", "music video", "fan animation", "fanmade", "fan made", "parody",
            "abridged", "review", "explained", "analysis", "breakdown", "podcast",
            "discussion", "thoughts on", "interview", "gameplay", "walkthrough",
            "playthrough", "cutscene", "cutscenes", "short clip", "best moments",
            "funny moments", "highlights", "compilation", "tiktok", "shorts", "reels",
            "speedpaint", "drawing", "unboxing", "figure review", "cover song", "vocal cover",
            "guitar cover", "piano cover", "drum cover", "remix", "nightcore", "8d audio",
            "vhs rip promo", "commercial", "bumper", "toonami", "adult swim"
        )
    }

    /**
     * Determines whether a candidate item is a verified exact match for the target anime
     * and requested episode, inspects its files deeply, and builds a candidate match record.
     */
    fun evaluateCandidate(
        doc: ArchiveDocItem,
        metadata: ArchiveMetadataResponse,
        anime: AnimeEntity,
        requestedEpisode: Int?
    ): ArchiveCandidateMatch? {
        val targetTitles = extractSearchTitles(anime)
        val docTitle = doc.title?.takeIf { it.isNotBlank() }
            ?: metadata.metadata?.title?.takeIf { it.isNotBlank() }
            ?: doc.identifier
        val docDesc = doc.description ?: metadata.metadata?.description.orEmpty()
        val docSubject = doc.subject ?: metadata.metadata?.subject.orEmpty()
        val docCreator = doc.creator ?: metadata.metadata?.creator.orEmpty()
        val identifier = doc.identifier

        // 1. Reject reaction videos, live action, stage plays, dramas, AMVs, trailers, reviews, etc.
        // Inspect title, subject, description, creator, and identifier.
        if (containsExcludedKeywords(docTitle, docSubject, identifier, docDesc, docCreator)) {
            return null
        }

        // 2. Calculate strict title match against item title, identifier, and file names
        val files = metadata.files.orEmpty()
        val isMovieFormat = anime.format.equals("MOVIE", ignoreCase = true)
        val totalEpisodes = anime.totalEpisodes ?: 0
        val isSingleEpisodeWork = isMovieFormat || totalEpisodes == 1

        val compatibleFiles = filterCompatibleVideoFiles(
            files = files,
            requireFullEpisodeLength = (requestedEpisode != null && !isMovieFormat),
            requestedEpisode = requestedEpisode,
            isMovieOrSingleWork = isSingleEpisodeWork
        )
        if (compatibleFiles.isEmpty()) {
            return null
        }

        val itemTitleScore = calculateStrictTitleSimilarity(docTitle, identifier, targetTitles)
        val bestFileTitleScore = compatibleFiles.maxOfOrNull { file ->
            calculateStrictTitleSimilarity(file.title ?: file.name, file.name, targetTitles)
        } ?: 0f

        val effectiveTitleScore = maxOf(itemTitleScore, bestFileTitleScore)
        if (effectiveTitleScore < 0.65f) {
            return null
        }

        // 3. Check season alignment if the target anime specifies a season > 1
        val targetSeason = extractSeasonNumber(targetTitles)
        if (targetSeason != null && targetSeason > 1) {
            val docSeason = extractSeasonNumber(listOf(docTitle, identifier))
            if (docSeason != null && docSeason != targetSeason) {
                return null
            }
        }

        // 4. Strictly resolve the exact file for the requested episode
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
                itemTitleScore = itemTitleScore
            ) ?: return null

            bestFile = exactEpisodeFile
            detectedEp = requestedEpisode
            isEpisodeSpecific = true
        } else {
            bestFile = selectBestFileForEpisode(compatibleFiles, requestedEpisode) ?: compatibleFiles.first()
            detectedEp = detectEpisodeNumber(bestFile.name) ?: detectEpisodeNumber(docTitle) ?: requestedEpisode
            isEpisodeSpecific = true
        }

        // Double check that the chosen file itself is not a reaction/live-action/trailer/short clip
        if (containsExcludedKeywords(bestFile.name, bestFile.title.orEmpty(), "")) {
            return null
        }
        if (!isValidEpisodeDurationAndSize(
                file = bestFile,
                requireFullLength = !isMovieFormat,
                requestedEpisode = requestedEpisode,
                isMovieOrSingleWork = isSingleEpisodeWork
            )
        ) {
            return null
        }

        // Give a strong score bonus if the file duration is within the canonical 19-27.5 min TV anime episode window
        val fileDurationSecs = parseDurationSeconds(bestFile.length)
        val canonicalDurationBonus = if (!isSingleEpisodeWork && fileDurationSecs != null &&
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
        val qualityOptions = extractQualityOptions(doc.identifier, compatibleFiles, requestedEpisode)
        val subtitleRepository = SubtitleRepository()
        val subtitleTracks = subtitleRepository.extractArchiveSubtitles(doc.identifier, files, requestedEpisode)

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
     * reaction videos, commentary, live-action adaptations, stage plays, dramas, cosplay, trailers, AMVs, or short clips.
     * Normalizes underscores, hyphens, dots, and camelCase first so filenames like
     * `One_Piece_Live_Action_01.mp4` or `NarutoReactionEp1.mp4` are always caught.
     */
    fun containsExcludedKeywords(
        title: String,
        subject: String = "",
        identifier: String = "",
        description: String = "",
        creator: String = ""
    ): Boolean {
        val rawCombined = "$title $subject $identifier $creator ${description.take(600)}"
        val normalized = normalizeForKeywordCheck(rawCombined)
        if (normalized.isBlank()) return false

        for (keyword in EXCLUDED_CONTENT_KEYWORDS) {
            val regex = Regex("""\b${Regex.escape(keyword)}\b""")
            if (regex.containsMatchIn(normalized)) {
                return true
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
     * Validates that a file is a genuine episode (not a 2-3 minute clip AND not a 40-180 minute
     * long reaction video or live-action feature movie when a TV episode is requested).
     */
    fun isValidEpisodeDurationAndSize(
        file: ArchiveFileItem,
        requireFullLength: Boolean = true,
        requestedEpisode: Int? = null,
        isMovieOrSingleWork: Boolean = false
    ): Boolean {
        val durationSecs = parseDurationSeconds(file.length)
        val sizeBytes = file.size?.toLongOrNull()

        if (!requireFullLength) {
            return (sizeBytes == null || sizeBytes > 2_000_000L)
        }

        if (durationSecs != null && durationSecs > 0f) {
            // 1. Reject short clips, openings, endings, trailers, and short reactions (< 9 minutes)
            if (durationSecs < MIN_EPISODE_DURATION_SECONDS) {
                return false
            }

            // 2. For multi-episode TV series, reject overly long videos (> 34 min for Ep 2+, > 55 min for Ep 1)
            // which are long uncut reaction streams, commentary VODs, or live-action movies.
            if (!isMovieOrSingleWork && requestedEpisode != null && requestedEpisode > 0) {
                val maxAllowedSecs = if (requestedEpisode == 1) {
                    MAX_PREMIERE_EPISODE_DURATION_SECONDS
                } else {
                    MAX_STANDARD_EPISODE_DURATION_SECONDS
                }
                if (durationSecs > maxAllowedSecs) {
                    return false
                }
            }
        }

        // If file size is known (and not an HLS manifest .m3u8), require at least 15 MB for a full episode
        val isHls = file.name.endsWith(".m3u8", ignoreCase = true)
        if (!isHls && sizeBytes != null && sizeBytes > 0L) {
            if (sizeBytes < MIN_EPISODE_FILE_SIZE_BYTES) {
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
        isMovieOrSingleWork: Boolean = false
    ): List<ArchiveFileItem> {
        return files.filter { file ->
            val name = file.name.lowercase(Locale.ROOT)
            val format = file.format?.lowercase(Locale.ROOT).orEmpty()

            val isExcludedExtension = name.endsWith(".xml") || name.endsWith(".sqlite") || name.endsWith(".torrent") ||
                    name.endsWith(".txt") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                    name.endsWith(".gif") || name.endsWith(".webp") || name.contains("_thumb.") ||
                    name.contains("_ia.thumb") || name.endsWith(".srt") || name.endsWith(".vtt") ||
                    name.endsWith(".ass") || name.endsWith(".ssa") || name.endsWith(".mp3") ||
                    name.endsWith(".flac") || name.endsWith(".ogg") || name.endsWith(".wav") ||
                    name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") ||
                    name.endsWith(".pdf") || name.endsWith(".epub")

            if (isExcludedExtension) return@filter false

            if (containsExcludedKeywords(file.name, file.title.orEmpty(), "")) {
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
                isMovieOrSingleWork = isMovieOrSingleWork
            )
        }
    }

    /**
     * Strictly selects a video file that matches `requestedEpisode` and belongs to the target anime.
     * Never falls back to `files.firstOrNull()` if the episode number or anime title does not match.
     */
    fun selectExactFileForEpisode(
        files: List<ArchiveFileItem>,
        docTitle: String,
        identifier: String,
        requestedEpisode: Int,
        targetTitles: List<String>,
        itemTitleScore: Float = 1.0f
    ): ArchiveFileItem? {
        if (files.isEmpty()) return null

        // 1. Check files whose filename or file title explicitly contains the requested episode number
        val matchingFiles = files.filter { file ->
            val fileEp = detectEpisodeNumber(file.name) ?: detectEpisodeNumber(file.title.orEmpty())
            fileEp == requestedEpisode
        }

        if (matchingFiles.isNotEmpty()) {
            val titleMatchedFiles = matchingFiles.filter { file ->
                calculateStrictTitleSimilarity(file.title ?: file.name, file.name, targetTitles) >= 0.60f
            }
            if (titleMatchedFiles.isNotEmpty()) {
                return pickPreferredFormatFile(titleMatchedFiles)
            }

            // If none of the matching episode files explicitly repeat the anime title in their filename
            // (e.g., files named "Episode 01.mp4" or "01.mp4"), ONLY accept them if the parent Archive Item
            // itself strongly matches the target anime title AND the filename doesn't belong to a different title.
            if (itemTitleScore >= 0.78f) {
                val genericNamedFiles = matchingFiles.filter { file ->
                    val significantFileTokens = extractSignificantTokens(
                        normalizeTitle(file.name.substringBeforeLast('.'))
                    )
                    // Either the filename has no title words (just numbers/Episode) or shares tokens with targetTitles
                    significantFileTokens.isEmpty() ||
                            calculateStrictTitleSimilarity(file.title ?: file.name, file.name, targetTitles) >= 0.45f
                }
                if (genericNamedFiles.isNotEmpty()) {
                    return pickPreferredFormatFile(genericNamedFiles)
                }
            }
            return null
        }

        // 2. If the Archive item itself is a single-episode upload whose item title or identifier
        // explicitly states the requested episode (e.g. "Naruto Episode 01"), and none of the video files
        // have conflicting episode numbers:
        val itemLevelEp = detectEpisodeNumber(docTitle) ?: detectEpisodeNumber(identifier)
        if (itemLevelEp == requestedEpisode && itemTitleScore >= 0.75f) {
            val nonConflictingFiles = files.filter { file ->
                val fEp = detectEpisodeNumber(file.name)
                fEp == null || fEp == requestedEpisode
            }
            if (nonConflictingFiles.isNotEmpty()) {
                return pickPreferredFormatFile(nonConflictingFiles)
            }
        }

        return null
    }

    /**
     * Picks the best file from compatible files matching an episode number,
     * prioritizing canonical ~24m episode duration and original/high-bitrate H.264 MP4.
     */
    fun selectBestFileForEpisode(
        files: List<ArchiveFileItem>,
        preferredEpisode: Int?
    ): ArchiveFileItem? {
        if (files.isEmpty()) return null

        if (preferredEpisode != null && preferredEpisode > 0) {
            val matchingEpisodeFiles = files.filter { file ->
                (detectEpisodeNumber(file.name) ?: detectEpisodeNumber(file.title.orEmpty())) == preferredEpisode
            }
            if (matchingEpisodeFiles.isNotEmpty()) {
                return pickPreferredFormatFile(matchingEpisodeFiles)
            }
            return null
        }

        return pickPreferredFormatFile(files)
    }

    private fun pickPreferredFormatFile(pool: List<ArchiveFileItem>): ArchiveFileItem? {
        if (pool.isEmpty()) return null

        // Prefer files in canonical anime episode duration (19-27.5 mins) if available in pool
        val canonicalDurationPool = pool.filter { file ->
            val dur = parseDurationSeconds(file.length)
            dur != null && dur in CANONICAL_EPISODE_MIN_SECONDS..CANONICAL_EPISODE_MAX_SECONDS
        }
        val activePool = if (canonicalDurationPool.isNotEmpty()) canonicalDurationPool else pool

        // 1. High-quality H.264 MP4 (excluding low-res 512Kb MPEG4 derivatives if a better MP4 exists)
        val highResMp4 = activePool.filter {
            it.name.endsWith(".mp4", ignoreCase = true) &&
                    it.format?.contains("512kb", ignoreCase = true) != true
        }.maxByOrNull { it.size?.toLongOrNull() ?: 0L }
        if (highResMp4 != null) return highResMp4

        // 2. Any MP4
        val anyMp4 = activePool.filter { it.name.endsWith(".mp4", ignoreCase = true) }
            .maxByOrNull { it.size?.toLongOrNull() ?: 0L }
        if (anyMp4 != null) return anyMp4

        // 3. MKV / WebM
        val mkvOrWebm = activePool.filter {
            it.name.endsWith(".mkv", ignoreCase = true) || it.name.endsWith(".webm", ignoreCase = true)
        }.maxByOrNull { it.size?.toLongOrNull() ?: 0L }
        if (mkvOrWebm != null) return mkvOrWebm

        // 4. HLS
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
     * Detects episode numbers from common anime filename/title patterns:
     * - "S01E01", "S1E12", "1x05"
     * - "Episode 01", "Ep. 2", "Ep 03", "E04"
     * - "第01話", "第1集", "第01集"
     * - " - 01 ", " - 01v2", "[01]", "(01)", "_01_", " 01.mp4"
     */
    fun detectEpisodeNumber(text: String): Int? {
        if (text.isBlank()) return null

        // Strip file extension and resolution/codec tags so they are never mistaken for episode numbers
        val cleaned = text
            .replace(Regex("""(?i)\.(mp4|mkv|webm|avi|m3u8|ts|mov|flv|ogv)$"""), "")
            .replace(Regex("""(?i)\b(1080p|720p|480p|360p|2160p|4k|1920x1080|1280x720|854x480|640x480|h\.?264|h\.?265|x264|x265|hevc|av1|aac|flac|10bit|8bit|512kb|mpeg4|bluray|bdrip|webrip|web-dl|dvdrip)\b"""), " ")
            .replace(Regex("""\[[0-9A-Fa-f]{8}\]"""), " ") // CRC32 hashes like [A1B2C3D4]
            .replace(Regex("""\b(19\d{2}|20\d{2})\b"""), " ") // Years like 1999, 2024

        // 1. S01E01 or 1x01 format
        val sxeRegex = Regex("""(?i)(?:s\d{1,2}[._\s-]*e|(?:\b\d{1,2}x))0*(\d{1,4})(?:\b|v\d|_)""")
        sxeRegex.find(cleaned)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..2000) return num
        }

        // 2. Explicit "Episode 01", "Ep. 01", "Ep 1", "EP01", "E01"
        val epRegex = Regex("""(?i)(?:^|[\s._\-\[\(])(?:episode|ep|e)[.\s_-]*0*(\d{1,4})(?:v\d)?(?:$|[\s._\-\]\)])""")
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

        // 5. Brackets or parentheses: "[01]", "(01)"
        val bracketRegex = Regex("""[\[\(]0*(\d{1,4})(?:v\d)?[\]\)]""")
        bracketRegex.find(cleaned)?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..2000) return num
        }

        // 6. Trailing number at end of filename: "Anime_Name_01" or "Anime Name 01"
        val trailingRegex = Regex("""(?:[_\s\-])0*(\d{1,3})(?:v\d)?$""")
        trailingRegex.find(cleaned.trim())?.let { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..500) return num
        }

        return null
    }

    private fun extractSeasonNumber(titles: List<String>): Int? {
        for (title in titles) {
            val sMatch = Regex("""(?i)\b(?:season|s)\s*0*(\d{1,2})\b""").find(title)
            if (sMatch != null) {
                return sMatch.groupValues[1].toIntOrNull()
            }
            val ordMatch = Regex("""(?i)\b(\d{1,2})(?:nd|rd|th)\s+season\b""").find(title)
            if (ordMatch != null) {
                return ordMatch.groupValues[1].toIntOrNull()
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
