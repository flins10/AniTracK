package com.example.service

import com.example.data.remote.AniListFuzzyDate
import com.example.data.remote.AniListMedia
import com.example.model.AnimeMovieOvaEntry
import com.example.model.AnimeRelationItem
import com.example.model.AnimeSeasonEntry
import com.example.model.AnimeSeasonSegment
import com.example.model.FranchiseSeasonOverrideMode
import com.example.util.EnglishTitleMapper
import java.time.LocalDate
import java.util.PriorityQueue
import kotlin.math.abs

/**
 * Multi-hop franchise graph resolver that:
 * 1. Recursively traverses SEQUEL, PREQUEL, and PARENT links across multiple hops (up to 60 nodes),
 *    walking THROUGH intermediate MOVIE, OVA, and SPECIAL nodes before filtering formats so
 *    chains like Season 1 -> Movie -> Season 2 -> Season 3 stay fully connected.
 * 2. Includes both TV and ONA (plus eligible TV_SHORT) main-series entries while strictly
 *    excluding MOVIE, OVA, SPECIAL, MUSIC, and SIDE_STORY / SPIN_OFF / SUMMARY entries from
 *    numbered SEASONS pills (1, 2, 3...).
 * 3. Merges all collected MOVIE, OVA, and SPECIAL nodes from the franchise graph into one unified
 *    chronologically sorted "MOVIES & OVAs" list (`moviesAndOvas`).
 * 4. Distinguishes real multi-season shows (> 21 days broadcast break or explicit season labels
 *    like Season 2, 2nd Season, 3rd Season, II, III) from artificially split continuous runs
 *    (same base title after stripping trailing split numbers like 2, 3, 4, 5 or Part 2, and
 *    <= 21 days broadcast break between endDate and startDate), merging continuous runs into
 *    a single SEASON 1 pill with summed episode counts and segment offset mappings.
 * 5. Supports per-franchise manual override mode (AUTO, MERGE_ONE, SPLIT_ALL).
 */
object SeasonChainResolver {

    private const val MAX_BFS_WAVES = 25
    private const val MAX_GRAPH_NODES = 60
    private const val BATCH_CHUNK_SIZE = 25
    private const val CONTINUOUS_RUN_MAX_GAP_DAYS = 21L

    private val NON_SEASON_FORMATS = setOf(
        "MOVIE",
        "OVA",
        "SPECIAL",
        "MUSIC",
        "MANGA",
        "NOVEL",
        "ONE_SHOT"
    )

    private val MOVIE_OVA_SPECIAL_FORMATS = setOf(
        "MOVIE",
        "OVA",
        "SPECIAL"
    )

    private val MAIN_EPISODIC_FORMATS = setOf(
        "TV",
        "ONA",
        "TV_SHORT"
    )

    private val MAINLINE_TRAVERSAL_RELATIONS = setOf(
        "SEQUEL",
        "PREQUEL",
        "PARENT"
    )

    private val SIDE_COLLECTION_RELATIONS = setOf(
        "SIDE_STORY",
        "SPIN_OFF",
        "SUMMARY",
        "ALTERNATIVE"
    )

    // Explicit season title markers ("Season 2", "2nd Season", "S2", "Di 2 Ji", "Dai 2 Ki", "Final Season")
    private val EXPLICIT_SEASON_TITLE_REGEX = Regex(
        pattern = """(?i)(?:\bseason\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+season\b|\bs(\d{1,2})\b|\bdi\s*(\d{1,2})\s*ji\b|\bdai\s*(\d{1,2})\s*ki\b|\bfinal\s+season\b|\bfinal\s+chapters?\b)"""
    )

    private val EXPLICIT_SEASON_NUMBER_REGEX = Regex(
        pattern = """(?i)(?:\bseason\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+season\b|\bs(\d{1,2})\b|\bdi\s*(\d{1,2})\s*ji\b|\bdai\s*(\d{1,2})\s*ki\b)"""
    )

    private val PINYIN_SEASON_NUMBER_REGEX = Regex(
        pattern = """(?i)\bdi\s*(yi|er|san|si|wu|liu|qi|ba|jiu|shi)\s*ji\b"""
    )

    // Explicit Part / Cour markers ("Part 2", "Cour 2", "2nd Cour")
    private val EXPLICIT_PART_NUMBER_REGEX = Regex(
        pattern = """(?i)(?:\bpart\s*(\d{1,2})\b|\bcour\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+(?:part|cour)\b)"""
    )

    private val SECOND_HALF_MARKER_REGEX = Regex(
        pattern = """(?i)\b(?:second\s+half|2nd\s+half|xia\s*ji|xia\s*pian)\b"""
    )

    private val ANNUAL_MARKER_REGEX = Regex(
        pattern = """(?i)\b(?:nian\s*fan|annual\s+(?:edition|season))\b"""
    )

    // Case-sensitive Roman numeral sequel suffix ("Title II", "Title III", "Title IV")
    private val TRAILING_ROMAN_SEQUEL_REGEX = Regex(
        pattern = """(?:^|\s)(II|III|IV|V|VI)(?:$|[:\-])"""
    )

    private val NON_SEASON_Side_TITLE_REGEX = Regex(
        pattern = """(?i)(?:\bmini\s*theater\b|\bxiao\s*juchang\b|\bchibi\b|\bq[- ]?ban\b|\bpetite\b|\bmini\s*anime\b|\bshort\s*theater\b|\bgekijou\b|\blittle\s*theater\b|\brecap\b|\bsummary\b|\bdigest\b|\bpreview\b|\bteaser\b|\btrailer\b|\bpv\b|\bcm\b|\bcommercial\b|\bpromotional\b|\bspecial\s+program\b|\bmusic\s+video\b|\btebie\s*pian\b|\bfanwai\b|\bomake\b|\bpicture\s*drama\b|\bprologue\b|\bepisode\s*0\b|\bep\.?\s*0\b|\bova\b|\boad\b|\bmovie\b|\bjuchang\s*ban\b|\bgekijouban\b|\bspecials?\b|\bspecial\s*\d+\b|\bsan\s*nian\s*zhi\s*yue\b|\bthree[- ]year\s+agreement\b|\byuan\s*qi\b|\borigin\s*(?::|$))"""
    )

    private val NON_SEASON_CONTEXT_BEFORE_ROMAN_REGEX = Regex(
        pattern = """(?i)\b(?:part|cour|movie|film|special|ova|oad|episode|ep|chapter|act|vol|volume)\s*$"""
    )

    private val BASE_TITLE_SPLIT_SUFFIX_REGEX = Regex(
        pattern = """(?i)(?:[:\-\s]+(?:(?:season\s*1\s*)?(?:part|cour)\s*\d{1,2}|\d{1,2}(?:st|nd|rd|th)\s+(?:part|cour)|second\s+half|2nd\s+half|xia\s*ji|xia\s*pian|nian\s*fan|annual\s*(?:edition|season)?)|(?<!\bseason)(?<!\bs)(?<!\bno\.)(?<!\bno)(?<!#)\s+[2-9])\s*$"""
    )

    data class ResolvedSeasonChain(
        val seasons: List<AnimeSeasonEntry>,
        val bridgeRelations: List<AnimeRelationItem>,
        val moviesAndOvas: List<AnimeMovieOvaEntry> = emptyList(),
        val franchiseId: Int = 0,
        val rawMainSeasonNodeCount: Int = 0
    )

    private data class ParsedSeasonPart(
        val explicitSeasonNumber: Int?,
        val explicitPartNumber: Int?,
        val isAnnualEdition: Boolean,
        val hasFinalSeasonMarker: Boolean
    )

    private fun extractSeasonAndPart(node: AniListMedia): ParsedSeasonPart {
        val primaryTitles = listOfNotNull(
            node.title?.english,
            node.title?.userPreferred,
            node.title?.romaji
        ).filter { it.isNotBlank() }

        val allTitles = (primaryTitles + node.synonyms.orEmpty()).filter { it.isNotBlank() }
        val primaryJoined = primaryTitles.joinToString(" | ")
        val allJoined = allTitles.joinToString(" | ")

        val isAnnual = ANNUAL_MARKER_REGEX.containsMatchIn(allJoined)
        val hasFinalMarker = Regex("""(?i)\b(?:final\s+season|final\s+chapters?)\b""").containsMatchIn(primaryJoined)

        var parsedSeason: Int? = null
        for (source in listOf(primaryJoined, allJoined)) {
            if (parsedSeason != null) break
            EXPLICIT_SEASON_NUMBER_REGEX.find(source)?.let { match ->
                val num = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toIntOrNull()
                if (num != null && num in 1..20) {
                    parsedSeason = num
                }
            }
            if (parsedSeason == null) {
                PINYIN_SEASON_NUMBER_REGEX.find(source)?.let { match ->
                    parsedSeason = when (match.groupValues[1].lowercase()) {
                        "yi" -> 1
                        "er" -> 2
                        "san" -> 3
                        "si" -> 4
                        "wu" -> 5
                        "liu" -> 6
                        "qi" -> 7
                        "ba" -> 8
                        "jiu" -> 9
                        "shi" -> 10
                        else -> null
                    }
                }
            }
        }

        if (parsedSeason == null && !NON_SEASON_Side_TITLE_REGEX.containsMatchIn(primaryJoined)) {
            for (title in primaryTitles) {
                val match = TRAILING_ROMAN_SEQUEL_REGEX.find(title)
                if (match != null) {
                    val prefix = title.substring(0, match.range.first).trim()
                    if (!NON_SEASON_CONTEXT_BEFORE_ROMAN_REGEX.containsMatchIn(prefix)) {
                        parsedSeason = when (match.groupValues[1]) {
                            "II" -> 2
                            "III" -> 3
                            "IV" -> 4
                            "V" -> 5
                            "VI" -> 6
                            else -> null
                        }
                        if (parsedSeason != null) break
                    }
                }
            }
        }

        var parsedPart: Int? = null
        for (source in listOf(primaryJoined, allJoined)) {
            if (parsedPart != null) break
            EXPLICIT_PART_NUMBER_REGEX.find(source)?.let { match ->
                val num = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toIntOrNull()
                if (num != null && num in 1..6) {
                    parsedPart = num
                }
            }
            if (parsedPart == null && SECOND_HALF_MARKER_REGEX.containsMatchIn(source)) {
                parsedPart = 2
            }
        }

        return ParsedSeasonPart(
            explicitSeasonNumber = parsedSeason,
            explicitPartNumber = parsedPart,
            isAnnualEdition = isAnnual,
            hasFinalSeasonMarker = hasFinalMarker
        )
    }

    /**
     * Strictly filters main-series candidates (`TV`, `ONA`, and eligible `TV_SHORT`).
     * Excludes `MOVIE`, `OVA`, `SPECIAL`, `MUSIC`, `CANCELLED`, and side-stories/recaps/chibis.
     */
    fun isSeasonCandidate(
        node: AniListMedia,
        franchiseHasFullLengthSeries: Boolean = true
    ): Boolean {
        if (!AniListMetadataProvider.isAnimeMediaType(node.type)) return false
        if (node.status.equals("CANCELLED", ignoreCase = true)) return false

        val fmt = node.format?.uppercase()?.trim().orEmpty()
        if (fmt in NON_SEASON_FORMATS) return false
        if (fmt.isNotEmpty() && fmt !in MAIN_EPISODIC_FORMATS) return false

        val primaryTitles = listOfNotNull(
            node.title?.english,
            node.title?.userPreferred,
            node.title?.romaji
        ).filter { it.isNotBlank() }.joinToString(" | ")

        val hasExplicitSeasonWord = EXPLICIT_SEASON_TITLE_REGEX.containsMatchIn(primaryTitles)
        val hasSideMarker = NON_SEASON_Side_TITLE_REGEX.containsMatchIn(primaryTitles)

        if (hasSideMarker && !hasExplicitSeasonWord) return false

        if (franchiseHasFullLengthSeries) {
            if (fmt == "TV_SHORT" && !hasExplicitSeasonWord) return false
            val dur = node.duration
            if (dur != null && dur in 1..6 && !hasExplicitSeasonWord) return false

            val eps = node.episodes
            val parsed = extractSeasonAndPart(node)
            if (
                fmt == "ONA" &&
                node.status.equals("FINISHED", ignoreCase = true) &&
                eps != null && eps in 1..3 &&
                parsed.explicitSeasonNumber == null &&
                parsed.explicitPartNumber == null &&
                !parsed.isAnnualEdition
            ) {
                return false
            }
        }

        return true
    }

    fun normalizeBaseTitle(rawTitle: String?): String {
        if (rawTitle.isNullOrBlank()) return ""
        var cleaned = rawTitle.trim()
        repeat(3) {
            val next = BASE_TITLE_SPLIT_SUFFIX_REGEX.replace(cleaned, "").trim()
            if (next == cleaned || next.isBlank()) return@repeat
            cleaned = next
        }
        return cleaned
            .lowercase()
            .replace(Regex("""[^a-z0-9\u4e00-\u9fff]+"""), " ")
            .trim()
    }

    private fun extractNormalizedBaseTitles(node: AniListMedia): Set<String> {
        val isDonghua = node.countryOfOrigin.equals("CN", ignoreCase = true) ||
            node.genres.orEmpty().any { it.contains("Donghua", ignoreCase = true) }
        val mapped = EnglishTitleMapper.resolve(
            titleEnglish = node.title?.english,
            titleRomaji = node.title?.romaji,
            synonyms = node.synonyms,
            isDonghua = isDonghua
        )
        return listOfNotNull(
            mapped,
            node.title?.english,
            node.title?.userPreferred,
            node.title?.romaji
        )
            .map { normalizeBaseTitle(it) }
            .filter { it.length >= 2 }
            .toSet()
    }

    private fun sharesSameBaseTitle(prev: AniListMedia, curr: AniListMedia): Boolean {
        val prevBases = extractNormalizedBaseTitles(prev)
        val currBases = extractNormalizedBaseTitles(curr)
        if (prevBases.isEmpty() || currBases.isEmpty()) return false
        if (prevBases.intersect(currBases).isNotEmpty()) return true
        for (a in prevBases) {
            for (b in currBases) {
                if (a.length >= 5 && b.length >= 5 && (a == b || a.startsWith("$b ") || b.startsWith("$a "))) {
                    return true
                }
            }
        }
        return false
    }

    private fun toEpochDayOrNull(date: AniListFuzzyDate?, defaultMonth: Int = 1, defaultDay: Int = 1): Long? {
        val y = date?.year ?: return null
        if (y < 1900 || y > 2100) return null
        val m = (date.month ?: defaultMonth).coerceIn(1, 12)
        val maxDay = try {
            LocalDate.of(y, m, 1).lengthOfMonth()
        } catch (_: Exception) {
            28
        }
        val d = (date.day ?: defaultDay).coerceIn(1, maxDay)
        return try {
            LocalDate.of(y, m, d).toEpochDay()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Computes broadcast gap in days between `prev`'s endDate (or estimated end date if endDate is
     * missing on AniList) and `curr`'s startDate.
     */
    fun computeBroadcastGapDays(prev: AniListMedia, curr: AniListMedia): Long? {
        val currStart = toEpochDayOrNull(curr.startDate) ?: return null
        val prevEndDirect = toEpochDayOrNull(prev.endDate)
        if (prevEndDirect != null) {
            return currStart - prevEndDirect
        }
        val prevStart = toEpochDayOrNull(prev.startDate) ?: return null
        val prevEps = prev.episodes
        if (prevEps != null && prevEps > 0) {
            val estimatedPrevEnd = prevStart + ((prevEps - 1).coerceAtLeast(0) * 7L)
            return currStart - estimatedPrevEnd
        }
        return null
    }

    private fun hasExplicitSeasonBreakLabel(prev: AniListMedia, curr: AniListMedia): Boolean {
        val prevParsed = extractSeasonAndPart(prev)
        val currParsed = extractSeasonAndPart(curr)
        if (currParsed.hasFinalSeasonMarker && !prevParsed.hasFinalSeasonMarker) return true
        val cSeason = currParsed.explicitSeasonNumber ?: return false
        val pSeason = prevParsed.explicitSeasonNumber ?: 1
        if (cSeason >= 2 && cSeason != pSeason) return true
        return false
    }

    private fun shouldMergeContinuousRun(
        prev: AniListMedia,
        curr: AniListMedia,
        hadMovieOrOvaBridgeBetween: Boolean
    ): Boolean {
        if (hadMovieOrOvaBridgeBetween) return false
        if (hasExplicitSeasonBreakLabel(prev, curr)) return false
        if (!sharesSameBaseTitle(prev, curr)) return false

        val gapDays = computeBroadcastGapDays(prev, curr)
        if (gapDays != null) {
            return gapDays in -14L..CONTINUOUS_RUN_MAX_GAP_DAYS
        }
        return false
    }

    suspend fun resolve(
        rootMedia: AniListMedia,
        overrideMode: FranchiseSeasonOverrideMode = FranchiseSeasonOverrideMode.AUTO,
        fetchBatch: suspend (List<Int>) -> List<AniListMedia>
    ): ResolvedSeasonChain {
        val nodesById = LinkedHashMap<Int, AniListMedia>()
        val outgoing = HashMap<Int, MutableSet<Int>>()
        val parentUndirected = HashMap<Int, MutableSet<Int>>()
        val mainlineEligibleIds = LinkedHashSet<Int>()
        val sideOnlyAttachedIds = LinkedHashSet<Int>()
        val expandedIds = HashSet<Int>()
        val queuedIds = HashSet<Int>()
        val frontier = ArrayDeque<Int>()

        fun mergeNode(existing: AniListMedia?, candidate: AniListMedia): AniListMedia {
            if (existing == null) return candidate
            val candidateEdges = candidate.relations?.edges.orEmpty()
            val existingEdges = existing.relations?.edges.orEmpty()
            return if (candidateEdges.isNotEmpty() && existingEdges.isEmpty()) {
                candidate.copy(
                    countryOfOrigin = candidate.countryOfOrigin ?: existing.countryOfOrigin,
                    synonyms = if (!candidate.synonyms.isNullOrEmpty()) candidate.synonyms else existing.synonyms,
                    startDate = candidate.startDate ?: existing.startDate,
                    endDate = candidate.endDate ?: existing.endDate,
                    seasonYear = candidate.seasonYear ?: existing.seasonYear,
                    episodes = candidate.episodes ?: existing.episodes,
                    duration = candidate.duration ?: existing.duration
                )
            } else {
                existing.copy(
                    countryOfOrigin = existing.countryOfOrigin ?: candidate.countryOfOrigin,
                    synonyms = if (!existing.synonyms.isNullOrEmpty()) existing.synonyms else candidate.synonyms,
                    startDate = existing.startDate ?: candidate.startDate,
                    endDate = existing.endDate ?: candidate.endDate,
                    seasonYear = existing.seasonYear ?: candidate.seasonYear,
                    episodes = existing.episodes ?: candidate.episodes,
                    duration = existing.duration ?: candidate.duration,
                    relations = if (existingEdges.isNotEmpty()) existing.relations else candidate.relations
                )
            }
        }

        fun enqueueForExpansion(id: Int) {
            if (id <= 0 || id in expandedIds || id in queuedIds) return
            if (nodesById.size >= MAX_GRAPH_NODES && id !in nodesById) return
            queuedIds.add(id)
            frontier.addLast(id)
        }

        fun ingestFullNode(node: AniListMedia) {
            nodesById[node.id] = mergeNode(nodesById[node.id], node)
            expandedIds.add(node.id)
            val nodeFmt = node.format?.uppercase()?.trim().orEmpty()
            val nodeIsMainlineOrBridge = node.id in mainlineEligibleIds || nodeFmt in MOVIE_OVA_SPECIAL_FORMATS

            for (edge in node.relations?.edges.orEmpty()) {
                val rel = edge.relationType?.uppercase()?.trim().orEmpty()
                val target = edge.node ?: continue
                if (target.id <= 0 || !AniListMetadataProvider.isAnimeMediaType(target.type)) continue
                if (target.id !in nodesById && nodesById.size >= MAX_GRAPH_NODES) continue

                val targetFmt = target.format?.uppercase()?.trim().orEmpty()

                if (rel in MAINLINE_TRAVERSAL_RELATIONS) {
                    nodesById[target.id] = mergeNode(nodesById[target.id], target)
                    if (nodeIsMainlineOrBridge) {
                        mainlineEligibleIds.add(target.id)
                        sideOnlyAttachedIds.remove(target.id)
                    }
                    when (rel) {
                        "SEQUEL" -> outgoing.getOrPut(node.id) { LinkedHashSet() }.add(target.id)
                        "PREQUEL" -> outgoing.getOrPut(target.id) { LinkedHashSet() }.add(node.id)
                        "PARENT" -> {
                            parentUndirected.getOrPut(node.id) { LinkedHashSet() }.add(target.id)
                            parentUndirected.getOrPut(target.id) { LinkedHashSet() }.add(node.id)
                        }
                    }
                    enqueueForExpansion(target.id)
                } else if (rel in SIDE_COLLECTION_RELATIONS) {
                    nodesById[target.id] = mergeNode(nodesById[target.id], target)
                    if (target.id !in mainlineEligibleIds) {
                        sideOnlyAttachedIds.add(target.id)
                    }
                    // Always walk through MOVIE, OVA, SPECIAL nodes even if linked via SIDE_STORY/SUMMARY
                    if (targetFmt in MOVIE_OVA_SPECIAL_FORMATS) {
                        enqueueForExpansion(target.id)
                    }
                }
            }
        }

        mainlineEligibleIds.add(rootMedia.id)
        ingestFullNode(rootMedia)

        var wave = 0
        while (frontier.isNotEmpty() && wave < MAX_BFS_WAVES && expandedIds.size < MAX_GRAPH_NODES) {
            wave++
            val currentBatchIds = mutableListOf<Int>()
            while (frontier.isNotEmpty() && currentBatchIds.size < BATCH_CHUNK_SIZE) {
                val nextId = frontier.removeFirst()
                if (nextId !in expandedIds) {
                    currentBatchIds.add(nextId)
                }
            }
            if (currentBatchIds.isEmpty()) break

            val fetchedNodes = try {
                fetchBatch(currentBatchIds)
            } catch (_: Exception) {
                emptyList()
            }

            val fetchedIdSet = HashSet<Int>()
            for (fetched in fetchedNodes) {
                if (fetched.id > 0) {
                    fetchedIdSet.add(fetched.id)
                    ingestFullNode(fetched)
                }
            }
            for (requestedId in currentBatchIds) {
                if (requestedId !in fetchedIdSet) {
                    expandedIds.add(requestedId)
                }
            }
        }

        val franchiseHasFullLengthSeries = nodesById.values.any { n ->
            val fmt = n.format?.uppercase()?.trim().orEmpty()
            (fmt == "TV" || fmt == "ONA") &&
                ((n.duration != null && n.duration >= 12) ||
                    (n.episodes != null && n.episodes >= 10) ||
                    (n.status.equals("RELEASING", ignoreCase = true) && (n.duration == null || n.duration >= 10)))
        }

        // Collect all valid TV / ONA main-series candidates reached via mainline (SEQUEL/PREQUEL/PARENT)
        val seasonCandidateIds = nodesById.values
            .filter { node ->
                node.id in mainlineEligibleIds &&
                    node.id !in sideOnlyAttachedIds &&
                    isSeasonCandidate(node, franchiseHasFullLengthSeries)
            }
            .map { it.id }
            .toSet()

        // Collect all MOVIE, OVA, and SPECIAL entries across the entire franchise graph
        val movieOvaSpecialNodes = nodesById.values
            .filter { node ->
                AniListMetadataProvider.isAnimeMediaType(node.type) &&
                    !node.status.equals("CANCELLED", ignoreCase = true) &&
                    node.format?.uppercase()?.trim() in MOVIE_OVA_SPECIAL_FORMATS
            }
            .sortedWith { a, b -> compareChronological(a, b, a.id, b.id) }

        val moviesAndOvas = movieOvaSpecialNodes.map { node ->
            val fmt = node.format?.uppercase()?.trim() ?: "SPECIAL"
            val badge = when (fmt) {
                "MOVIE" -> "MOVIE"
                "OVA" -> "OVA"
                else -> "SPECIAL"
            }
            val totalEps = node.episodes?.takeIf { it > 0 } ?: 1
            AnimeMovieOvaEntry(
                id = node.id,
                title = resolveCleanTitle(node),
                coverImage = node.coverImage?.extraLarge
                    ?: node.coverImage?.large
                    ?: node.coverImage?.medium.orEmpty(),
                formatBadge = badge,
                status = node.status,
                episodes = totalEps,
                releaseYear = node.startDate?.year ?: node.seasonYear,
                isCurrent = node.id == rootMedia.id
            )
        }

        // Non-season nodes remain available in bridgeRelations
        val bridgeRelations = nodesById.values
            .filter { it.id != rootMedia.id && it.id !in seasonCandidateIds }
            .sortedWith { a, b -> compareChronological(a, b, a.id, b.id) }
            .mapNotNull { node ->
                val title = resolveCleanTitle(node).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                AnimeRelationItem(
                    id = node.id,
                    relationType = when (node.format?.uppercase()?.trim()) {
                        "MOVIE" -> "MOVIE"
                        "OVA" -> "OVA"
                        "SPECIAL" -> "SPECIAL"
                        else -> "RELATED"
                    },
                    title = title,
                    coverImage = node.coverImage?.large ?: node.coverImage?.medium.orEmpty(),
                    format = node.format,
                    status = node.status,
                    averageScore = node.averageScore,
                    mediaType = node.type ?: "ANIME",
                    episodes = node.episodes,
                    seasonYear = node.seasonYear ?: node.startDate?.year
                )
            }

        val canonicalFranchiseId = (seasonCandidateIds + movieOvaSpecialNodes.map { it.id } + rootMedia.id)
            .filter { it > 0 }
            .minOrNull() ?: rootMedia.id

        if (seasonCandidateIds.isEmpty()) {
            return ResolvedSeasonChain(
                seasons = emptyList(),
                bridgeRelations = bridgeRelations,
                moviesAndOvas = moviesAndOvas,
                franchiseId = canonicalFranchiseId,
                rawMainSeasonNodeCount = 0
            )
        }

        // Contract directed paths across intermediate non-season bridges (e.g. S1 -> Movie -> S2)
        val seasonOutgoing = HashMap<Int, MutableSet<Int>>()
        val directNonBridgeOutgoing = HashMap<Int, MutableSet<Int>>()
        val inDegree = HashMap<Int, Int>()
        for (sid in seasonCandidateIds) {
            seasonOutgoing[sid] = LinkedHashSet()
            directNonBridgeOutgoing[sid] = LinkedHashSet()
            inDegree[sid] = 0
        }

        for (startId in seasonCandidateIds) {
            for (directNext in outgoing[startId].orEmpty()) {
                if (directNext in seasonCandidateIds && directNext != startId) {
                    directNonBridgeOutgoing.getValue(startId).add(directNext)
                }
            }
            val visited = HashSet<Int>()
            val queue = ArrayDeque<Int>()
            for (next in outgoing[startId].orEmpty()) {
                if (visited.add(next)) queue.addLast(next)
            }
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                if (curr == startId) continue
                if (curr in seasonCandidateIds) {
                    if (seasonOutgoing.getValue(startId).add(curr)) {
                        inDegree[curr] = (inDegree[curr] ?: 0) + 1
                    }
                } else {
                    for (next in outgoing[curr].orEmpty()) {
                        if (visited.add(next)) queue.addLast(next)
                    }
                }
            }
        }

        val chronoComparator = Comparator<Int> { aId, bId ->
            compareChronological(nodesById[aId], nodesById[bId], aId, bId)
        }
        val readyQueue = PriorityQueue(chronoComparator)
        for (sid in seasonCandidateIds) {
            if ((inDegree[sid] ?: 0) == 0) {
                readyQueue.add(sid)
            }
        }

        val orderedIds = mutableListOf<Int>()
        while (readyQueue.isNotEmpty()) {
            val curr = readyQueue.poll()
            orderedIds.add(curr)
            for (neighbor in seasonOutgoing[curr].orEmpty()) {
                val remaining = (inDegree[neighbor] ?: 1) - 1
                inDegree[neighbor] = remaining
                if (remaining == 0) {
                    readyQueue.add(neighbor)
                }
            }
        }

        if (orderedIds.size < seasonCandidateIds.size) {
            val placed = orderedIds.toHashSet()
            val remaining = seasonCandidateIds.filter { it !in placed }.sortedWith(chronoComparator)
            orderedIds.addAll(remaining)
        }

        val sortedMainNodes = orderedIds.map { nodesById.getValue(it) }

        // Group consecutive main entries according to overrideMode or the 21-day continuous run rule
        val groupedMainNodes: List<List<AniListMedia>> = when (overrideMode) {
            FranchiseSeasonOverrideMode.MERGE_ONE -> {
                listOf(sortedMainNodes)
            }
            FranchiseSeasonOverrideMode.SPLIT_ALL -> {
                sortedMainNodes.map { listOf(it) }
            }
            FranchiseSeasonOverrideMode.AUTO -> {
                val groups = mutableListOf<MutableList<AniListMedia>>()
                for (node in sortedMainNodes) {
                    if (groups.isEmpty()) {
                        groups.add(mutableListOf(node))
                    } else {
                        val currentGroup = groups.last()
                        val prevNode = currentGroup.last()
                        val connectedViaBridge = prevNode.id in seasonOutgoing &&
                            node.id in seasonOutgoing[prevNode.id].orEmpty() &&
                            node.id !in directNonBridgeOutgoing[prevNode.id].orEmpty()
                        if (shouldMergeContinuousRun(prevNode, node, connectedViaBridge)) {
                            currentGroup.add(node)
                        } else {
                            groups.add(mutableListOf(node))
                        }
                    }
                }
                groups
            }
        }

        // If there is only 1 raw main node and no movies/OVAs, return empty seasons switcher
        if (sortedMainNodes.size <= 1 && moviesAndOvas.isEmpty()) {
            return ResolvedSeasonChain(
                seasons = emptyList(),
                bridgeRelations = bridgeRelations,
                moviesAndOvas = emptyList(),
                franchiseId = canonicalFranchiseId,
                rawMainSeasonNodeCount = sortedMainNodes.size
            )
        }

        val finalSeasons = groupedMainNodes.mapIndexed { index, groupNodes ->
            val seasonNum = index + 1
            val leadNode = groupNodes.firstOrNull { it.id == rootMedia.id } ?: groupNodes.first()
            val isCurrentSeason = groupNodes.any { it.id == rootMedia.id }

            var runningOffset = 0
            var allSegmentsHaveKnownEpisodes = true
            var knownEpisodesSum = 0
            val segments = groupNodes.map { segNode ->
                val segEps = segNode.episodes?.takeIf { it > 0 }
                if (segEps == null) {
                    allSegmentsHaveKnownEpisodes = false
                }
                val segStartOffset = runningOffset
                if (segEps != null) {
                    runningOffset += segEps
                    knownEpisodesSum += segEps
                }
                AnimeSeasonSegment(
                    id = segNode.id,
                    title = resolveCleanTitle(segNode),
                    episodes = segEps,
                    startEpisodeOffset = segStartOffset,
                    status = segNode.status,
                    format = segNode.format,
                    seasonYear = segNode.seasonYear ?: segNode.startDate?.year
                )
            }

            val mergedTotalEpisodes = when {
                groupNodes.size == 1 -> leadNode.episodes?.takeIf { it > 0 }
                allSegmentsHaveKnownEpisodes && knownEpisodesSum > 0 -> knownEpisodesSum
                knownEpisodesSum > 0 -> knownEpisodesSum
                else -> null
            }

            val mergedStatus = when {
                groupNodes.any { it.status.equals("RELEASING", ignoreCase = true) } -> "RELEASING"
                groupNodes.all { it.status.equals("FINISHED", ignoreCase = true) } -> "FINISHED"
                else -> leadNode.status
            }

            val shortLabel = seasonNum.toString()
            val displayLabel = if (groupNodes.size > 1) {
                "Season $seasonNum (${groupNodes.size} Parts Merged)"
            } else {
                "Season $seasonNum"
            }

            AnimeSeasonEntry(
                id = leadNode.id,
                seasonNumber = seasonNum,
                title = resolveCleanTitle(groupNodes.first()),
                coverImage = leadNode.coverImage?.extraLarge
                    ?: leadNode.coverImage?.large
                    ?: leadNode.coverImage?.medium.orEmpty(),
                format = leadNode.format,
                status = mergedStatus,
                episodes = mergedTotalEpisodes,
                seasonYear = groupNodes.first().seasonYear ?: groupNodes.first().startDate?.year,
                isCurrent = isCurrentSeason,
                partNumber = null,
                shortLabel = shortLabel,
                displayLabel = displayLabel,
                subEntries = segments,
                isMergedContinuousRun = groupNodes.size > 1
            )
        }

        return ResolvedSeasonChain(
            seasons = finalSeasons,
            bridgeRelations = bridgeRelations,
            moviesAndOvas = moviesAndOvas,
            franchiseId = canonicalFranchiseId,
            rawMainSeasonNodeCount = sortedMainNodes.size
        )
    }

    private fun compareChronological(a: AniListMedia?, b: AniListMedia?, aId: Int, bId: Int): Int {
        val aYear = a?.startDate?.year ?: a?.seasonYear
        val bYear = b?.startDate?.year ?: b?.seasonYear

        val aUndatedFuture = if (aYear == null && a?.status.equals("NOT_YET_RELEASED", ignoreCase = true)) 1 else 0
        val bUndatedFuture = if (bYear == null && b?.status.equals("NOT_YET_RELEASED", ignoreCase = true)) 1 else 0
        if (aUndatedFuture != bUndatedFuture) return aUndatedFuture.compareTo(bUndatedFuture)

        val yearA = aYear ?: 9999
        val yearB = bYear ?: 9999
        if (yearA != yearB) return yearA.compareTo(yearB)

        val monthA = a?.startDate?.month ?: 13
        val monthB = b?.startDate?.month ?: 13
        if (monthA != monthB) return monthA.compareTo(monthB)

        val dayA = a?.startDate?.day ?: 32
        val dayB = b?.startDate?.day ?: 32
        if (dayA != dayB) return dayA.compareTo(dayB)

        return aId.compareTo(bId)
    }

    private fun resolveCleanTitle(node: AniListMedia): String {
        val isDonghua = node.countryOfOrigin.equals("CN", ignoreCase = true) ||
            node.genres.orEmpty().any { it.contains("Donghua", ignoreCase = true) }
        return EnglishTitleMapper.resolve(
            titleEnglish = node.title?.english,
            titleRomaji = node.title?.romaji,
            synonyms = node.synonyms,
            isDonghua = isDonghua
        )?.takeIf { it.isNotBlank() }
            ?: node.title?.english?.takeIf { it.isNotBlank() }
            ?: node.title?.userPreferred?.takeIf { it.isNotBlank() }
            ?: node.title?.romaji?.takeIf { it.isNotBlank() }
            ?: "Season"
    }
}
