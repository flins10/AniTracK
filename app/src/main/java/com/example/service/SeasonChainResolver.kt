package com.example.service

import com.example.data.remote.AniListMedia
import com.example.model.AnimeRelationItem
import com.example.model.AnimeSeasonEntry
import java.util.PriorityQueue

data class ResolvedSeasonChain(
    val seasons: List<AnimeSeasonEntry>,
    val bridgeRelations: List<AnimeRelationItem>
)

data class ParsedSeasonPart(
    val seasonNumber: Int?,
    val partNumber: Int?,
    val isAnnualEdition: Boolean = false
)

/**
 * Resolves the complete chronological season chain of an anime or donghua franchise by running
 * a batched Breadth-First Search across AniList PREQUEL / SEQUEL edges (traversing through
 * Movie / OVA / Special bridges), strictly excluding non-season entries (Movies, OVAs, Specials,
 * Chibi/Mini-Theaters, PVs, Recaps, short bonus ONAs), and computing accurate season + part
 * numbers so ongoing Season 1 Donghua never inflate into "Season 5+".
 */
object SeasonChainResolver {
    private const val MAX_BFS_WAVES = 25
    private const val MAX_DISCOVERED_NODES = 60
    private const val BATCH_SIZE = 25

    private val NON_SEASON_FORMATS = setOf("MOVIE", "OVA", "MUSIC", "MANGA", "NOVEL", "ONE_SHOT")
    private val MAIN_EPISODIC_FORMATS = setOf("TV", "ONA", "TV_SHORT")

    private val EXPLICIT_SEASON_TITLE_REGEX = Regex(
        pattern = """(?i)(?:\bseason\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+season\b|\bs(\d{1,2})\b|\bdi\s*(\d{1,2})\s*ji\b|\bdai\s*(\d{1,2})\s*ki\b|\bfinal\s+season\b|\bfinal\s+chapters?\b)"""
    )
    private val EXPLICIT_SEASON_NUM_REGEX = Regex(
        pattern = """(?i)(?:\bseason\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+season\b|\bs(\d{1,2})\b|\bdi\s*(\d{1,2})\s*ji\b|\bdai\s*(\d{1,2})\s*ki\b)"""
    )
    private val PINYIN_SEASON_NUM_REGEX = Regex(
        pattern = """(?i)\bdi\s*(yi|er|san|si|wu|liu|qi|ba|jiu|shi)\s*ji\b"""
    )
    private val EXPLICIT_PART_REGEX = Regex(
        pattern = """(?i)(?:\bpart\s*(\d{1,2})\b|\bcour\s*(\d{1,2})\b|\b(\d{1,2})(?:st|nd|rd|th)\s+cour\b)"""
    )
    private val SECOND_HALF_REGEX = Regex(
        pattern = """(?i)\b(?:second\s+half|2nd\s+half|xia\s*ji|xia\s*pian)\b"""
    )
    private val ANNUAL_EDITION_REGEX = Regex(
        pattern = """(?i)\b(?:nian\s*fan|annual\s+(?:edition|season))\b"""
    )
    private val TRAILING_ROMAN_SEASON_REGEX = Regex(
        pattern = """(?:^|\s)(II|III|IV|V|VI)(?:$|[:\-])"""
    )
    private val NON_SEASON_SIDE_REGEX = Regex(
        pattern = """(?i)(?:\bmini\s*theater\b|\bxiao\s*juchang\b|\bchibi\b|\bq[- ]?ban\b|\bpetite\b|\bmini\s*anime\b|\bshort\s*theater\b|\bgekijou\b|\blittle\s*theater\b|\brecap\b|\bsummary\b|\bdigest\b|\bpreview\b|\bteaser\b|\btrailer\b|\bpv\b|\bcm\b|\bcommercial\b|\bpromotional\b|\bspecial\s+program\b|\bmusic\s+video\b|\btebie\s*pian\b|\bfanwai\b|\bomake\b|\bpicture\s*drama\b|\bprologue\b|\bepisode\s*0\b|\bep\.?\s*0\b|\bova\b|\boad\b|\bmovie\b|\bjuchang\s*ban\b|\bgekijouban\b|\bspecials?\b|\bspecial\s*\d+\b|\bsan\s*nian\s*zhi\s*yue\b|\bthree[- ]year\s+agreement\b|\byuan\s*qi\b|\borigin\s*(?::|$))"""
    )
    private val NON_SEASON_BEFORE_ROMAN_REGEX = Regex(
        pattern = """(?i)\b(?:part|cour|movie|film|special|ova|oad|episode|ep|chapter|act|vol|volume)\s*$"""
    )

    fun isAnimeMediaType(type: String?): Boolean {
        return type.isNullOrBlank() || type.equals("ANIME", ignoreCase = true)
    }

    private fun combinedTitlesOf(media: AniListMedia): String {
        return buildList {
            media.title?.english?.let { if (it.isNotBlank()) add(it) }
            media.title?.userPreferred?.let { if (it.isNotBlank()) add(it) }
            media.title?.romaji?.let { if (it.isNotBlank()) add(it) }
            media.synonyms.orEmpty().forEach { if (it.isNotBlank()) add(it) }
        }.joinToString(" | ")
    }

    private fun primaryTitlesOf(media: AniListMedia): String {
        return listOfNotNull(
            media.title?.english?.takeIf { it.isNotBlank() },
            media.title?.userPreferred?.takeIf { it.isNotBlank() },
            media.title?.romaji?.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
    }

    fun extractSeasonAndPart(media: AniListMedia): ParsedSeasonPart {
        val primary = primaryTitlesOf(media)
        val combined = combinedTitlesOf(media)
        val isAnnual = ANNUAL_EDITION_REGEX.containsMatchIn(combined)

        var parsedSeason: Int? = null
        for (source in listOf(primary, combined)) {
            if (parsedSeason != null) break
            EXPLICIT_SEASON_NUM_REGEX.find(source)?.let { m ->
                val n = m.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toIntOrNull()
                if (n != null && n in 1..20) parsedSeason = n
            }
            if (parsedSeason == null) {
                PINYIN_SEASON_NUM_REGEX.find(source)?.let { m ->
                    parsedSeason = when (m.groupValues[1].lowercase()) {
                        "yi" -> 1; "er" -> 2; "san" -> 3; "si" -> 4; "wu" -> 5
                        "liu" -> 6; "qi" -> 7; "ba" -> 8; "jiu" -> 9; "shi" -> 10
                        else -> null
                    }
                }
            }
        }

        if (parsedSeason == null && !NON_SEASON_SIDE_REGEX.containsMatchIn(primary)) {
            val singleTitles = listOfNotNull(
                media.title?.english?.takeIf { it.isNotBlank() },
                media.title?.userPreferred?.takeIf { it.isNotBlank() },
                media.title?.romaji?.takeIf { it.isNotBlank() }
            )
            for (t in singleTitles) {
                val match = TRAILING_ROMAN_SEASON_REGEX.find(t)
                if (match != null) {
                    val before = t.substring(0, match.range.first).trim()
                    if (!NON_SEASON_BEFORE_ROMAN_REGEX.containsMatchIn(before)) {
                        parsedSeason = when (match.groupValues[1]) {
                            "II" -> 2; "III" -> 3; "IV" -> 4; "V" -> 5; "VI" -> 6
                            else -> null
                        }
                        if (parsedSeason != null) break
                    }
                }
            }
        }

        var parsedPart: Int? = null
        for (source in listOf(primary, combined)) {
            if (parsedPart != null) break
            EXPLICIT_PART_REGEX.find(source)?.let { m ->
                val p = m.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toIntOrNull()
                if (p != null && p in 1..6) parsedPart = p
            }
            if (parsedPart == null && SECOND_HALF_REGEX.containsMatchIn(source)) {
                parsedPart = 2
            }
        }

        return ParsedSeasonPart(
            seasonNumber = parsedSeason,
            partNumber = parsedPart,
            isAnnualEdition = isAnnual
        )
    }

    /**
     * Checks whether a media node is a genuine main-series season (not a Movie, OVA, Special,
     * Chibi/Mini-Theater, PV, Recap, or short promotional ONA side entry).
     */
    fun isSeasonCandidate(
        media: AniListMedia,
        franchiseHasFullLengthSeries: Boolean = false
    ): Boolean {
        if (!isAnimeMediaType(media.type)) return false
        if (media.status.equals("CANCELLED", ignoreCase = true)) return false
        val format = media.format?.uppercase()?.trim().orEmpty()
        if (format in NON_SEASON_FORMATS) return false

        val primaryTitles = primaryTitlesOf(media)
        val hasExplicitSeason = EXPLICIT_SEASON_TITLE_REGEX.containsMatchIn(primaryTitles)
        val hasNonSeasonSideMarker = NON_SEASON_SIDE_REGEX.containsMatchIn(primaryTitles)

        if (format == "SPECIAL") {
            // Only treat a SPECIAL as a season if it is explicitly a main "Final Season" / "Season N"
            // (e.g. Attack on Titan Final Season TV Specials) AND not a recap/preview/bonus special.
            val isExplicitMainSpecial = Regex(
                pattern = """(?i)(?:\bfinal\s+season\b|\bfinal\s+chapters?\b|\bseason\s+\d+\b)"""
            ).containsMatchIn(primaryTitles)
            return isExplicitMainSpecial && !Regex(
                pattern = """(?i)(?:\brecap\b|\bsummary\b|\bdigest\b|\bpreview\b|\bpv\b|\bspecial\s+program\b|\bchibi\b|\bmini\b|\bxiao\s*juchang\b)"""
            ).containsMatchIn(primaryTitles)
        }

        if (format.isNotEmpty() && format !in MAIN_EPISODIC_FORMATS) return false

        // Reject Chibi / Mini-Theater / PV / Recap / Bonus Special ONAs & TV_SHORTs even if AniList filed them as ONA
        if (hasNonSeasonSideMarker && !hasExplicitSeason) {
            return false
        }

        // Reject TV_SHORT & ultra-short chibi/promo ONA nodes when the franchise is a full-length series
        val dur = media.duration
        if (franchiseHasFullLengthSeries) {
            if (format == "TV_SHORT" && !hasExplicitSeason) return false
            if (dur != null && dur in 1..6 && !hasExplicitSeason) return false
            // Short 1-3 episode finished bonus ONAs (e.g. donghua "Special Arc" / "Prologue" / "Bonus")
            val eps = media.episodes
            val isFinished = media.status.equals("FINISHED", ignoreCase = true)
            val parsed = extractSeasonAndPart(media)
            if (format == "ONA" && isFinished && eps != null && eps in 1..3 &&
                parsed.seasonNumber == null && parsed.partNumber == null && !parsed.isAnnualEdition
            ) {
                return false
            }
        }

        return true
    }

    fun displayTitleOf(media: AniListMedia, fallback: String): String {
        val isDonghua = media.countryOfOrigin.equals("CN", ignoreCase = true) ||
            media.genres.orEmpty().any { it.contains("Donghua", ignoreCase = true) }
        return com.example.util.EnglishTitleMapper.resolve(
            titleEnglish = media.title?.english,
            titleRomaji = media.title?.romaji,
            synonyms = media.synonyms,
            isDonghua = isDonghua
        )?.takeIf { it.isNotBlank() }
            ?: media.title?.english?.takeIf { it.isNotBlank() }
            ?: media.title?.userPreferred?.takeIf { it.isNotBlank() }
            ?: media.title?.romaji?.takeIf { it.isNotBlank() }
            ?: fallback
    }

    suspend fun resolve(
        rootMedia: AniListMedia,
        fetchBatch: suspend (List<Int>) -> List<AniListMedia>
    ): ResolvedSeasonChain {
        val nodesById = LinkedHashMap<Int, AniListMedia>()
        val outgoing = HashMap<Int, MutableSet<Int>>() // predecessor -> successors
        val expandedIds = HashSet<Int>()
        val frontier = ArrayDeque<Int>()
        val queuedIds = HashSet<Int>()

        fun mergeNode(candidate: AniListMedia) {
            val existing = nodesById[candidate.id]
            if (existing == null) {
                nodesById[candidate.id] = candidate
                return
            }
            val existingHasRelations = !existing.relations?.edges.isNullOrEmpty()
            val candidateHasRelations = !candidate.relations?.edges.isNullOrEmpty()
            nodesById[candidate.id] = if (candidateHasRelations && !existingHasRelations) {
                candidate.copy(
                    startDate = candidate.startDate ?: existing.startDate,
                    seasonYear = candidate.seasonYear ?: existing.seasonYear,
                    episodes = candidate.episodes ?: existing.episodes,
                    duration = candidate.duration ?: existing.duration
                )
            } else {
                existing.copy(
                    startDate = existing.startDate ?: candidate.startDate,
                    seasonYear = existing.seasonYear ?: candidate.seasonYear,
                    episodes = existing.episodes ?: candidate.episodes,
                    duration = existing.duration ?: candidate.duration,
                    relations = if (existingHasRelations) existing.relations else candidate.relations
                )
            }
        }

        fun enqueue(id: Int) {
            if (id <= 0) return
            if (id in expandedIds || id in queuedIds) return
            if (nodesById.size >= MAX_DISCOVERED_NODES && id !in nodesById) return
            queuedIds.add(id)
            frontier.addLast(id)
        }

        fun ingestRelations(media: AniListMedia) {
            mergeNode(media)
            expandedIds.add(media.id)
            val edges = media.relations?.edges.orEmpty()
            for (edge in edges) {
                val rel = edge.relationType?.uppercase()?.trim() ?: continue
                if (rel != "PREQUEL" && rel != "SEQUEL") continue
                val target = edge.node ?: continue
                if (target.id <= 0 || !isAnimeMediaType(target.type)) continue
                if (target.id !in nodesById && nodesById.size >= MAX_DISCOVERED_NODES) continue
                mergeNode(target)
                if (rel == "SEQUEL") {
                    outgoing.getOrPut(media.id) { LinkedHashSet() }.add(target.id)
                } else {
                    outgoing.getOrPut(target.id) { LinkedHashSet() }.add(media.id)
                }
                enqueue(target.id)
            }
        }

        ingestRelations(rootMedia)

        var wave = 0
        while (frontier.isNotEmpty() && wave < MAX_BFS_WAVES && expandedIds.size < MAX_DISCOVERED_NODES) {
            wave++
            val batch = mutableListOf<Int>()
            while (frontier.isNotEmpty() && batch.size < BATCH_SIZE) {
                val nextId = frontier.removeFirst()
                if (nextId !in expandedIds) {
                    batch.add(nextId)
                }
            }
            if (batch.isEmpty()) break

            val fetched = fetchBatch(batch)
            val fetchedIds = HashSet<Int>()
            for (node in fetched) {
                fetchedIds.add(node.id)
                ingestRelations(node)
            }
            for (requestedId in batch) {
                if (requestedId !in fetchedIds) {
                    expandedIds.add(requestedId)
                }
            }
        }

        // Determine if this franchise has full-length TV/ONA series so we can accurately filter out
        // chibi shorts, mini-theaters, and 1-3 ep promotional/special ONAs.
        val franchiseHasFullSeries = nodesById.values.any { n ->
            val fmt = n.format?.uppercase()?.trim().orEmpty()
            (fmt == "TV" || fmt == "ONA") &&
                ((n.duration != null && n.duration >= 12) ||
                    (n.episodes != null && n.episodes >= 10) ||
                    (n.status.equals("RELEASING", ignoreCase = true) && (n.duration == null || n.duration >= 10)))
        }

        val seasonIds = nodesById.values
            .filter { isSeasonCandidate(it, franchiseHasFullSeries) }
            .map { it.id }
            .toSet()

        val bridgeRelations = nodesById.values
            .filter { it.id != rootMedia.id && it.id !in seasonIds && !it.status.equals("CANCELLED", ignoreCase = true) }
            .mapNotNull { node ->
                val title = displayTitleOf(node, "").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                AnimeRelationItem(
                    id = node.id,
                    relationType = node.format?.replace('_', ' ') ?: "Related",
                    title = title,
                    coverImage = node.coverImage?.large ?: node.coverImage?.medium.orEmpty(),
                    format = node.format,
                    status = node.status,
                    averageScore = node.averageScore,
                    mediaType = node.type ?: "ANIME",
                    episodes = node.episodes,
                    seasonYear = node.seasonYear
                )
            }
            .distinctBy { it.id }

        // If the currently opened item is NOT a main season (e.g. a Movie, OVA, Special, or Chibi Short),
        // do NOT render a misleading Season switcher pretending this Movie is a numbered season!
        if (rootMedia.id !in seasonIds || seasonIds.size <= 1) {
            return ResolvedSeasonChain(
                seasons = emptyList(),
                bridgeRelations = bridgeRelations
            )
        }

        // Project transitive reachability through non-season bridges (e.g. S1 -> Movie -> S2)
        // onto the season-only DAG.
        val seasonOutgoing = HashMap<Int, MutableSet<Int>>()
        val inDegree = HashMap<Int, Int>()
        for (sid in seasonIds) {
            seasonOutgoing[sid] = LinkedHashSet()
            inDegree[sid] = 0
        }

        for (startId in seasonIds) {
            val seen = HashSet<Int>()
            val queue = ArrayDeque<Int>()
            outgoing[startId].orEmpty().forEach { next ->
                if (seen.add(next)) queue.addLast(next)
            }
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                if (curr == startId) continue
                if (curr in seasonIds) {
                    if (seasonOutgoing.getValue(startId).add(curr)) {
                        inDegree[curr] = (inDegree[curr] ?: 0) + 1
                    }
                } else {
                    outgoing[curr].orEmpty().forEach { next ->
                        if (seen.add(next)) queue.addLast(next)
                    }
                }
            }
        }

        val comparator = Comparator<Int> { aId, bId ->
            compareChronologically(nodesById[aId], nodesById[bId], aId, bId)
        }

        val ready = PriorityQueue(comparator)
        for (sid in seasonIds) {
            if ((inDegree[sid] ?: 0) == 0) {
                ready.add(sid)
            }
        }

        val orderedIds = mutableListOf<Int>()
        while (ready.isNotEmpty()) {
            val curr = ready.poll()
            orderedIds.add(curr)
            for (next in seasonOutgoing[curr].orEmpty()) {
                val remaining = (inDegree[next] ?: 1) - 1
                inDegree[next] = remaining
                if (remaining == 0) {
                    ready.add(next)
                }
            }
        }

        if (orderedIds.size < seasonIds.size) {
            val placed = orderedIds.toHashSet()
            val remaining = seasonIds.filter { it !in placed }.sortedWith(comparator)
            orderedIds.addAll(remaining)
        }

        // Intelligent Season + Part assignment (prevents Part 1/Part 2 splits from inflating Season counts)
        val seasonEntries = mutableListOf<AnimeSeasonEntry>()
        var currentSeasonNum = 0
        for (id in orderedIds) {
            val node = nodesById.getValue(id)
            val parsed = extractSeasonAndPart(node)

            val assignedSeason: Int
            val assignedPart: Int?

            if (seasonEntries.isEmpty()) {
                assignedSeason = parsed.seasonNumber?.coerceIn(1, 20) ?: 1
                assignedPart = parsed.partNumber
                currentSeasonNum = assignedSeason
            } else if (parsed.partNumber != null && parsed.partNumber >= 2 &&
                (parsed.seasonNumber == null || parsed.seasonNumber == currentSeasonNum)
            ) {
                // Explicit Part 2 / Cour 2 of the SAME season (do not increment seasonNumber!)
                assignedSeason = currentSeasonNum
                assignedPart = parsed.partNumber
            } else if (parsed.seasonNumber != null && parsed.seasonNumber == currentSeasonNum && parsed.partNumber != null) {
                assignedSeason = currentSeasonNum
                assignedPart = parsed.partNumber
            } else if (parsed.seasonNumber != null && parsed.seasonNumber in (currentSeasonNum + 1)..(currentSeasonNum + 3)) {
                assignedSeason = parsed.seasonNumber
                assignedPart = parsed.partNumber
                currentSeasonNum = assignedSeason
            } else {
                currentSeasonNum += 1
                assignedSeason = currentSeasonNum
                assignedPart = parsed.partNumber
            }

            val shortLabel = if (assignedPart != null && assignedPart > 1) {
                "$assignedSeason Pt $assignedPart"
            } else {
                assignedSeason.toString()
            }
            val displayLabel = when {
                assignedPart != null && assignedPart > 1 -> "Season $assignedSeason • Part $assignedPart"
                parsed.isAnnualEdition -> "Season $assignedSeason • Annual"
                else -> "Season $assignedSeason"
            }

            seasonEntries.add(
                AnimeSeasonEntry(
                    id = node.id,
                    seasonNumber = assignedSeason,
                    title = displayTitleOf(node, displayLabel),
                    coverImage = node.coverImage?.extraLarge
                        ?: node.coverImage?.large
                        ?: node.coverImage?.medium.orEmpty(),
                    format = node.format,
                    status = node.status,
                    episodes = node.episodes,
                    seasonYear = node.seasonYear ?: node.startDate?.year,
                    isCurrent = node.id == rootMedia.id,
                    partNumber = assignedPart,
                    shortLabel = shortLabel,
                    displayLabel = displayLabel
                )
            )
        }

        return ResolvedSeasonChain(
            seasons = seasonEntries,
            bridgeRelations = bridgeRelations
        )
    }

    private fun compareChronologically(a: AniListMedia?, b: AniListMedia?, aId: Int, bId: Int): Int {
        val aYear = a?.startDate?.year ?: a?.seasonYear
        val bYear = b?.startDate?.year ?: b?.seasonYear

        val aUndatedUpcoming = if (aYear == null && a?.status.equals("NOT_YET_RELEASED", ignoreCase = true)) 1 else 0
        val bUndatedUpcoming = if (bYear == null && b?.status.equals("NOT_YET_RELEASED", ignoreCase = true)) 1 else 0
        if (aUndatedUpcoming != bUndatedUpcoming) return aUndatedUpcoming.compareTo(bUndatedUpcoming)

        val yCmp = (aYear ?: 9999).compareTo(bYear ?: 9999)
        if (yCmp != 0) return yCmp

        val mCmp = (a?.startDate?.month ?: 13).compareTo(b?.startDate?.month ?: 13)
        if (mCmp != 0) return mCmp

        val dCmp = (a?.startDate?.day ?: 32).compareTo(b?.startDate?.day ?: 32)
        if (dCmp != 0) return dCmp

        return aId.compareTo(bId)
    }
}
