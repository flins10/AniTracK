package com.example.service

import com.example.data.local.AnimeEntity
import com.example.data.remote.AniListAiringSchedule
import com.example.data.remote.AniListApiService
import com.example.data.remote.AniListMedia
import com.example.data.remote.AniListQueries
import com.example.data.remote.AniListRequest
import com.example.data.remote.AnimeMetadataProvider
import com.example.model.AnimeCharacterItem
import com.example.model.AnimeRecommendationItem
import com.example.model.AnimeRelationItem
import com.example.model.AnimeRichDetail
import com.example.model.AnimeSeasonEntry
import com.example.util.EnglishTitleMapper
import com.example.util.EpisodeDataParser
import com.example.util.ErrorSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class AniListMetadataProvider(
    private val apiService: AniListApiService = AniListApiService.create()
) : AnimeMetadataProvider {

    private val seasonNodeCache = ConcurrentHashMap<Int, AniListMedia>()

    override suspend fun searchCatalog(
        query: String?,
        genre: String?,
        season: String?,
        seasonYear: Int?,
        status: String?,
        format: String?,
        countryOfOrigin: String?,
        sort: String,
        page: Int,
        perPage: Int
    ): Result<Pair<List<AniListMedia>, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query?.trim()?.takeIf { it.isNotBlank() }
            val cleanGenre = genre?.takeIf { it.isNotBlank() && it != "ALL" }
            val cleanSeason = season?.takeIf { it.isNotBlank() && it != "ALL" }
            val cleanStatus = status?.takeIf { it.isNotBlank() && it != "ALL" }
            val cleanFormat = format?.takeIf { it.isNotBlank() && it != "ALL" }
            val cleanCountry = countryOfOrigin?.takeIf { it.isNotBlank() && it != "ALL" }

            val gqlQuery = AniListQueries.buildMediaQuery(
                hasSearch = cleanQuery != null,
                hasGenre = cleanGenre != null,
                hasSeason = cleanSeason != null,
                hasSeasonYear = seasonYear != null,
                hasStatus = cleanStatus != null,
                hasFormat = cleanFormat != null,
                hasCountryOfOrigin = cleanCountry != null
            )

            val variables = mutableMapOf<String, Any?>(
                "page" to page,
                "perPage" to perPage,
                "sort" to listOf(sort)
            )
            if (cleanQuery != null) variables["search"] = cleanQuery
            if (cleanGenre != null) variables["genre"] = cleanGenre
            if (cleanSeason != null) variables["season"] = cleanSeason
            if (seasonYear != null) variables["seasonYear"] = seasonYear
            if (cleanStatus != null) variables["status"] = cleanStatus
            if (cleanFormat != null) variables["format"] = cleanFormat
            if (cleanCountry != null) variables["countryOfOrigin"] = cleanCountry

            val response = apiService.queryMedia(AniListRequest(query = gqlQuery, variables = variables))
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Unable to load catalog right now. Please try again."))
            }
            val pageData = response.body()?.data?.Page
            val media = pageData?.media.orEmpty()
            val hasNext = pageData?.pageInfo?.hasNextPage ?: false
            Result.success(media to hasNext)
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to reach catalog right now.")))
        }
    }

    override suspend fun getTrendingAnime(page: Int, perPage: Int): Result<List<AniListMedia>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.queryMedia(
                AniListRequest(
                    query = AniListQueries.TRENDING_MEDIA_QUERY,
                    variables = mapOf("page" to page, "perPage" to perPage)
                )
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Unable to load trending titles right now."))
            }
            Result.success(response.body()?.data?.Page?.media.orEmpty())
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to reach catalog right now.")))
        }
    }

    override suspend fun getPopularReleasing(page: Int, perPage: Int): Result<List<AniListMedia>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.queryMedia(
                AniListRequest(
                    query = AniListQueries.POPULAR_RELEASING_MEDIA_QUERY,
                    variables = mapOf("page" to page, "perPage" to perPage)
                )
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Unable to load currently airing titles right now."))
            }
            Result.success(response.body()?.data?.Page?.media.orEmpty())
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to reach catalog right now.")))
        }
    }

    override suspend fun getAiringSchedule(
        startUnix: Int,
        endUnix: Int,
        page: Int
    ): Result<List<AniListAiringSchedule>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.querySchedule(
                AniListRequest(
                    query = AniListQueries.AIRING_SCHEDULE_QUERY,
                    variables = mapOf(
                        "airingAt_greater" to startUnix,
                        "airingAt_lesser" to endUnix,
                        "page" to page
                    )
                )
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Unable to load weekly schedule right now."))
            }
            Result.success(response.body()?.data?.Page?.airingSchedules.orEmpty())
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to load weekly schedule right now.")))
        }
    }

    override suspend fun getMediaDetails(anilistId: Int): Result<AniListMedia?> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.querySingleMedia(
                AniListRequest(
                    query = AniListQueries.MEDIA_DETAILS_QUERY,
                    variables = mapOf("id" to anilistId)
                )
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Unable to load series details right now."))
            }
            val media = response.body()?.data?.Media
            if (media != null) {
                seasonNodeCache[media.id] = media
            }
            Result.success(media)
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to load series details right now.")))
        }
    }

    private suspend fun fetchChainNode(anilistId: Int): AniListMedia? {
        seasonNodeCache[anilistId]?.let { return it }
        return try {
            val response = apiService.querySingleMedia(
                AniListRequest(
                    query = AniListQueries.MEDIA_SEASON_CHAIN_QUERY,
                    variables = mapOf("id" to anilistId)
                )
            )
            val media = if (response.isSuccessful) response.body()?.data?.Media else null
            if (media != null) {
                seasonNodeCache[media.id] = media
            }
            media
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getRichDetailWithSeasons(rootMedia: AniListMedia): AnimeRichDetail = withContext(Dispatchers.IO) {
        seasonNodeCache[rootMedia.id] = rootMedia
        val base = mapRichDetail(rootMedia)
        if (!isEpisodicSeasonFormat(rootMedia.format)) {
            return@withContext base.copy(seasons = emptyList())
        }

        val hasImmediateLink = rootMedia.relations?.edges.orEmpty().any { edge ->
            val rel = edge.relationType?.uppercase().orEmpty()
            val node = edge.node ?: return@any false
            (rel == "PREQUEL" || rel == "SEQUEL") && isAnimeMediaType(node.type)
        }
        if (!hasImmediateLink) {
            return@withContext base.copy(seasons = emptyList())
        }

        val visited = mutableSetOf(rootMedia.id)
        val bridgeParts = mutableListOf<AnimeRelationItem>()

        fun pickRelationTarget(media: AniListMedia, relation: String): AniListMedia? {
            val candidates = media.relations?.edges.orEmpty()
                .filter { edge ->
                    edge.relationType.equals(relation, ignoreCase = true) &&
                        edge.node != null &&
                        isAnimeMediaType(edge.node.type) &&
                        !visited.contains(edge.node.id)
                }
                .mapNotNull { it.node }
            return candidates.firstOrNull { isEpisodicSeasonFormat(it.format) }
                ?: candidates.firstOrNull()
        }

        // Walk backward via PREQUEL (up to 6 hops)
        val prequelsNewestFirst = mutableListOf<AniListMedia>()
        var cursor: AniListMedia = rootMedia
        repeat(6) {
            val prevEdgeNode = pickRelationTarget(cursor, "PREQUEL") ?: return@repeat
            visited.add(prevEdgeNode.id)
            val fullPrev = fetchChainNode(prevEdgeNode.id) ?: prevEdgeNode
            prequelsNewestFirst.add(fullPrev)
            if (!isEpisodicSeasonFormat(fullPrev.format)) {
                toRelationItem(fullPrev, "PREQUEL")?.let { bridgeParts.add(it) }
            }
            cursor = fullPrev
        }

        // Walk forward via SEQUEL (up to 6 hops)
        val sequels = mutableListOf<AniListMedia>()
        cursor = rootMedia
        repeat(6) {
            val nextEdgeNode = pickRelationTarget(cursor, "SEQUEL") ?: return@repeat
            visited.add(nextEdgeNode.id)
            val fullNext = fetchChainNode(nextEdgeNode.id) ?: nextEdgeNode
            sequels.add(fullNext)
            if (!isEpisodicSeasonFormat(fullNext.format)) {
                toRelationItem(fullNext, "SEQUEL")?.let { bridgeParts.add(it) }
            }
            cursor = fullNext
        }

        val orderedChain = prequelsNewestFirst.asReversed() + listOf(rootMedia) + sequels
        val episodicSeasons = orderedChain.filter { isEpisodicSeasonFormat(it.format) }
        val mergedRelations = (base.relations + bridgeParts).distinctBy { it.id }

        if (episodicSeasons.size <= 1) {
            return@withContext base.copy(
                seasons = emptyList(),
                relations = mergedRelations
            )
        }

        val seasonEntries = episodicSeasons.mapIndexed { idx, node ->
            val title = node.title?.english?.takeIf { it.isNotBlank() }
                ?: node.title?.userPreferred?.takeIf { it.isNotBlank() }
                ?: node.title?.romaji?.takeIf { it.isNotBlank() }
                ?: "Season ${idx + 1}"
            AnimeSeasonEntry(
                id = node.id,
                seasonNumber = idx + 1,
                title = title,
                coverImage = node.coverImage?.extraLarge
                    ?: node.coverImage?.large
                    ?: node.coverImage?.medium.orEmpty(),
                format = node.format,
                status = node.status,
                episodes = node.episodes,
                seasonYear = node.seasonYear,
                isCurrent = node.id == rootMedia.id
            )
        }

        base.copy(
            relations = mergedRelations,
            seasons = seasonEntries
        )
    }

    companion object {
        fun isAnimeMediaType(type: String?): Boolean {
            return type.isNullOrBlank() || type.equals("ANIME", ignoreCase = true)
        }

        fun isEpisodicSeasonFormat(format: String?): Boolean {
            if (format.isNullOrBlank()) return true
            return format.uppercase().trim() in setOf("TV", "TV_SHORT", "ONA")
        }

        private fun toRelationItem(node: AniListMedia, rawRelationType: String?): AnimeRelationItem? {
            if (!isAnimeMediaType(node.type)) return null
            val title = node.title?.english?.takeIf { it.isNotBlank() }
                ?: node.title?.userPreferred?.takeIf { it.isNotBlank() }
                ?: node.title?.romaji?.takeIf { it.isNotBlank() }
                ?: return null
            return AnimeRelationItem(
                id = node.id,
                relationType = rawRelationType?.replace('_', ' ') ?: "Related",
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
        fun cleanDescription(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            return raw
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#39;", "'")
                .trim()
        }

        fun mapMediaToEntity(media: AniListMedia, existing: AnimeEntity? = null): AnimeEntity {
            val genresList = media.genres.orEmpty()
            val studiosList = media.studios?.nodes.orEmpty().mapNotNull { it.name?.trim()?.takeIf { n -> n.isNotBlank() } }
            val isDonghua = media.countryOfOrigin.equals("CN", ignoreCase = true) ||
                genresList.any { it.contains("Donghua", ignoreCase = true) }

            val resolvedEnglish = media.title?.english?.trim()?.takeIf { it.isNotBlank() }
                ?: EnglishTitleMapper.resolve(null, media.title?.romaji, media.synonyms)
                ?: existing?.titleEnglish

            val parsedEp = EpisodeDataParser.parseAniListEpisodes(
                apiEpisodes = media.episodes,
                statusStr = media.status,
                nextAiringEpisodeNumber = media.nextAiringEpisode?.episode,
                nextAiringAtUnix = media.nextAiringEpisode?.airingAt,
                existingAired = existing?.currentEpisode ?: 0,
                mediaId = media.id,
                title = resolvedEnglish ?: media.title?.romaji,
                isDonghua = isDonghua,
                startYear = media.startDate?.year,
                startMonth = media.startDate?.month,
                startDay = media.startDate?.day
            )

            val airingTime = media.nextAiringEpisode?.airingAt ?: existing?.airingTimeUnix
            val dayOfWeek = if (airingTime != null && airingTime > 0) {
                Instant.ofEpochSecond(airingTime).atZone(ZoneId.systemDefault()).dayOfWeek.value
            } else {
                existing?.airingDayOfWeek ?: 1
            }

            return AnimeEntity(
                id = media.id,
                titleRomaji = media.title?.romaji ?: existing?.titleRomaji ?: "Untitled Anime",
                titleEnglish = resolvedEnglish,
                titleNative = media.title?.native ?: existing?.titleNative,
                titlePinyin = if (isDonghua) media.title?.romaji else existing?.titlePinyin,
                metadataSource = "anilist",
                coverImage = media.coverImage?.extraLarge
                    ?: media.coverImage?.large
                    ?: media.coverImage?.medium
                    ?: existing?.coverImage.orEmpty(),
                bannerImage = media.bannerImage ?: existing?.bannerImage,
                format = media.format ?: existing?.format ?: "TV",
                status = media.status ?: existing?.status ?: "RELEASING",
                totalEpisodes = parsedEp.totalEpisodes ?: existing?.totalEpisodes,
                currentEpisode = parsedEp.airedEpisodeCount,
                watchedEpisodeCount = existing?.watchedEpisodeCount ?: 0,
                userStatus = existing?.userStatus,
                userScore = existing?.userScore,
                startDate = existing?.startDate,
                finishDate = existing?.finishDate,
                airingDayOfWeek = dayOfWeek,
                airingTimeUnix = airingTime,
                nextEpisodeNumber = media.nextAiringEpisode?.episode ?: parsedEp.nextEpisodeNumber,
                genres = if (genresList.isNotEmpty()) genresList.joinToString(", ") else existing?.genres.orEmpty(),
                studios = if (studiosList.isNotEmpty()) studiosList.joinToString(", ") else existing?.studios.orEmpty(),
                season = media.season ?: existing?.season,
                seasonYear = media.seasonYear ?: existing?.seasonYear,
                score = media.averageScore ?: existing?.score,
                popularity = media.popularity ?: existing?.popularity,
                synopsis = cleanDescription(media.description) ?: existing?.synopsis,
                countryOfOrigin = media.countryOfOrigin ?: existing?.countryOfOrigin ?: "JP",
                inWatchlist = existing?.inWatchlist ?: false,
                isWishlist = existing?.isWishlist ?: false,
                isFavorite = existing?.isFavorite ?: false,
                isPinned = existing?.isPinned ?: false,
                notificationsEnabled = existing?.notificationsEnabled ?: true,
                isWatched = existing?.isWatched ?: false,
                lastWatchedTime = existing?.lastWatchedTime,
                userNotes = existing?.userNotes,
                lastCachedAt = System.currentTimeMillis()
            )
        }

        fun mapRichDetail(media: AniListMedia): AnimeRichDetail {
            val studios = media.studios?.nodes.orEmpty()
                .mapNotNull { it.name?.trim()?.takeIf { n -> n.isNotBlank() } }
            val characters = media.characters?.nodes.orEmpty().mapNotNull { ch ->
                val full = ch.name?.full?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                AnimeCharacterItem(
                    id = ch.id ?: full.hashCode(),
                    name = full,
                    imageUrl = ch.image?.large ?: ch.image?.medium.orEmpty()
                )
            }
            val relations = media.relations?.edges.orEmpty().mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                toRelationItem(node, edge.relationType)
            }.distinctBy { it.id }
            val recs = media.recommendations?.nodes.orEmpty().mapNotNull { node ->
                val rec = node.mediaRecommendation ?: return@mapNotNull null
                val title = rec.title?.english?.takeIf { it.isNotBlank() }
                    ?: rec.title?.romaji?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                AnimeRecommendationItem(
                    id = rec.id,
                    title = title,
                    coverImage = rec.coverImage?.large ?: rec.coverImage?.medium.orEmpty(),
                    format = rec.format,
                    averageScore = rec.averageScore
                )
            }
            return AnimeRichDetail(
                animeId = media.id,
                studios = studios,
                characters = characters,
                relations = relations,
                recommendations = recs
            )
        }
    }
}
