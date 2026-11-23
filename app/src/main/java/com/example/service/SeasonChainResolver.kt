package com.example.service

import com.example.data.remote.AniListMedia
import com.example.model.AnimeRelationItem
import com.example.model.AnimeSeasonEntry
import java.util.PriorityQueue

data class ResolvedSeasonChain(
    val seasons: List<AnimeSeasonEntry>,
    val bridgeRelations: List<AnimeRelationItem>
)

/**
 * Resolves the complete chronological season chain of an anime franchise by running
 * a batched Breadth-First Search across all AniList PREQUEL / SEQUEL edges (including
 * through Movie / OVA / Special bridges) and ordering eligible seasons with Kahn's
 * topological sort backed by release-date priority tie-breaking.
 */
object SeasonChainResolver {
    private const val MAX_BFS_WAVES = 25
    private const val MAX_DISCOVERED_NODES = 60
    private const val BATCH_SIZE = 25

    private val EPISODIC_FORMATS = setOf("TV", "TV_SHORT", "ONA")
    private val SPECIAL_SEASON_TITLE_REGEX = Regex(
        pattern = "(final\\s+season|final\\s+chapters?|season\\s+\\d+|\\bpart\\s+\\d+)",
        option = RegexOption.IGNORE_CASE
    )
    private val RECAP_TITLE_REGEX = Regex(
        pattern = "(recap|summary|digest|preview|special\\s+program|pv)",
        option = RegexOption.IGNORE_CASE
    )

    fun isAnimeMediaType(type: String?): Boolean {
        return type.isNullOrBlank() || type.equals("ANIME", ignoreCase = true)
    }

    fun isSeasonCandidate(media: AniListMedia): Boolean {
        if (!isAnimeMediaType(media.type)) return false
        if (media.status.equals("CANCELLED", ignoreCase = true)) return false
        val format = media.format?.uppercase()?.trim().orEmpty()
        if (format.isEmpty() || format in EPISODIC_FORMATS) return true
        if (format == "SPECIAL") {
            val combinedTitle = listOfNotNull(
                media.title?.english,
                media.title?.userPreferred,
                media.title?.romaji
            ).joinToString(" ")
            if (combinedTitle.isNotBlank() &&
                SPECIAL_SEASON_TITLE_REGEX.containsMatchIn(combinedTitle) &&
                !RECAP_TITLE_REGEX.containsMatchIn(combinedTitle)
            ) {
                return true
            }
        }
        return false
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
                    episodes = candidate.episodes ?: existing.episodes
                )
            } else {
                existing.copy(
                    startDate = existing.startDate ?: candidate.startDate,
                    seasonYear = existing.seasonYear ?: candidate.seasonYear,
                    episodes = existing.episodes ?: candidate.episodes,
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
            // Mark any unreturned IDs as expanded so we don't loop on deleted/private AniList IDs
            for (requestedId in batch) {
                if (requestedId !in fetchedIds) {
                    expandedIds.add(requestedId)
                }
            }
        }

        val seasonIds = nodesById.values
            .filter { isSeasonCandidate(it) }
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

        if (seasonIds.size <= 1) {
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
                    // Do not expand beyond the next season; direct season-to-season edges suffice
                } else {
                    // Traverse through Movie / OVA / Special bridge nodes
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

        // Cycle safety fallback: append any remaining nodes in strict chronological order
        if (orderedIds.size < seasonIds.size) {
            val placed = orderedIds.toHashSet()
            val remaining = seasonIds.filter { it !in placed }.sortedWith(comparator)
            orderedIds.addAll(remaining)
        }

        val seasonEntries = orderedIds.mapIndexed { idx, id ->
            val node = nodesById.getValue(id)
            AnimeSeasonEntry(
                id = node.id,
                seasonNumber = idx + 1,
                title = displayTitleOf(node, "Season ${idx + 1}"),
                coverImage = node.coverImage?.extraLarge
                    ?: node.coverImage?.large
                    ?: node.coverImage?.medium.orEmpty(),
                format = node.format,
                status = node.status,
                episodes = node.episodes,
                seasonYear = node.seasonYear ?: node.startDate?.year,
                isCurrent = node.id == rootMedia.id
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
