package com.example.data.remote

import com.example.config.AppConfig
import com.example.data.local.AnimeEntity
import com.example.model.AnimeRichDetail
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AniListRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class AniListResponseData<T>(
    val data: T? = null,
    val errors: List<AniListError>? = null
)

@JsonClass(generateAdapter = true)
data class AniListError(
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListMediaData(
    val Page: AniListMediaPage? = null
)

@JsonClass(generateAdapter = true)
data class AniListSingleMediaData(
    val Media: AniListMedia? = null
)

@JsonClass(generateAdapter = true)
data class AniListMediaPage(
    val pageInfo: AniListPageInfo? = null,
    val media: List<AniListMedia>? = null
)

@JsonClass(generateAdapter = true)
data class AniListScheduleData(
    val Page: AniListSchedulePage? = null
)

@JsonClass(generateAdapter = true)
data class AniListSchedulePage(
    val pageInfo: AniListPageInfo? = null,
    val airingSchedules: List<AniListAiringSchedule>? = null
)

@JsonClass(generateAdapter = true)
data class AniListPageInfo(
    val total: Int? = null,
    val hasNextPage: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class AniListFuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

@JsonClass(generateAdapter = true)
data class AniListMedia(
    val id: Int = 0,
    val type: String? = null,
    val title: AniListTitle? = null,
    val coverImage: AniListCoverImage? = null,
    val bannerImage: String? = null,
    val format: String? = "TV",
    val status: String? = "RELEASING",
    val episodes: Int? = null,
    val genres: List<String>? = emptyList(),
    val averageScore: Int? = null,
    val popularity: Int? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val description: String? = null,
    val countryOfOrigin: String? = null,
    val synonyms: List<String>? = emptyList(),
    val nextAiringEpisode: AniListNextAiringEpisode? = null,
    val startDate: AniListFuzzyDate? = null,
    val studios: AniListStudioConnection? = null,
    val characters: AniListCharacterConnection? = null,
    val relations: AniListRelationsConnection? = null,
    val recommendations: AniListRecommendationsConnection? = null
)

@JsonClass(generateAdapter = true)
data class AniListStudioConnection(
    val nodes: List<AniListStudio>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class AniListStudio(
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListCharacterConnection(
    val nodes: List<AniListCharacter>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class AniListCharacter(
    val id: Int? = null,
    val name: AniListCharacterName? = null,
    val image: AniListCharacterImage? = null
)

@JsonClass(generateAdapter = true)
data class AniListCharacterName(
    val full: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListCharacterImage(
    val medium: String? = null,
    val large: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListRelationsConnection(
    val edges: List<AniListRelationEdge>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class AniListRelationEdge(
    val relationType: String? = null,
    val node: AniListMedia? = null
)

@JsonClass(generateAdapter = true)
data class AniListRecommendationsConnection(
    val nodes: List<AniListRecommendationNode>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class AniListRecommendationNode(
    val mediaRecommendation: AniListMedia? = null
)

@JsonClass(generateAdapter = true)
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
    val userPreferred: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val color: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListNextAiringEpisode(
    val id: Int? = null,
    val airingAt: Long? = null,
    val timeUntilAiring: Long? = null,
    val episode: Int? = null
)

@JsonClass(generateAdapter = true)
data class AniListAiringSchedule(
    val id: Int? = null,
    val airingAt: Long? = null,
    val episode: Int? = null,
    val media: AniListMedia? = null
)

/**
 * Abstraction layer for public anime metadata so providers can be swapped or extended cleanly.
 */
interface AnimeMetadataProvider {
    suspend fun searchCatalog(
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
    ): Result<Pair<List<AniListMedia>, Boolean>>

    suspend fun getTrendingAnime(page: Int = 1, perPage: Int = 20): Result<List<AniListMedia>>
    suspend fun getPopularReleasing(page: Int = 1, perPage: Int = 25): Result<List<AniListMedia>>
    suspend fun getAiringSchedule(startUnix: Int, endUnix: Int, page: Int = 1): Result<List<AniListAiringSchedule>>
    suspend fun getMediaDetails(anilistId: Int): Result<AniListMedia?>
    suspend fun getRichDetailWithSeasons(rootMedia: AniListMedia): AnimeRichDetail {
        return com.example.service.AniListMetadataProvider.mapRichDetail(rootMedia)
    }
}

interface AniListApiService {
    @POST("/")
    suspend fun queryMedia(
        @Body request: AniListRequest
    ): Response<AniListResponseData<AniListMediaData>>

    @POST("/")
    suspend fun querySchedule(
        @Body request: AniListRequest
    ): Response<AniListResponseData<AniListScheduleData>>

    @POST("/")
    suspend fun querySingleMedia(
        @Body request: AniListRequest
    ): Response<AniListResponseData<AniListSingleMediaData>>

    companion object {
        fun create(): AniListApiService {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(AppConfig.ANILIST_GRAPHQL_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AniListApiService::class.java)
        }
    }
}

object AniListQueries {
    fun buildMediaQuery(
        hasSearch: Boolean,
        hasGenre: Boolean,
        hasSeason: Boolean,
        hasSeasonYear: Boolean,
        hasStatus: Boolean,
        hasFormat: Boolean = false,
        hasCountryOfOrigin: Boolean = false
    ): String {
        val queryArgs = mutableListOf("\$page: Int", "\$perPage: Int", "\$sort: [MediaSort]")
        val mediaArgs = mutableListOf("type: ANIME", "isAdult: false", "sort: \$sort")

        if (hasSearch) {
            queryArgs.add("\$search: String")
            mediaArgs.add("search: \$search")
        }
        if (hasGenre) {
            queryArgs.add("\$genre: String")
            mediaArgs.add("genre: \$genre")
        }
        if (hasSeason) {
            queryArgs.add("\$season: MediaSeason")
            mediaArgs.add("season: \$season")
        }
        if (hasSeasonYear) {
            queryArgs.add("\$seasonYear: Int")
            mediaArgs.add("seasonYear: \$seasonYear")
        }
        if (hasStatus) {
            queryArgs.add("\$status: MediaStatus")
            mediaArgs.add("status: \$status")
        }
        if (hasFormat) {
            queryArgs.add("\$format: MediaFormat")
            mediaArgs.add("format: \$format")
        }
        if (hasCountryOfOrigin) {
            queryArgs.add("\$countryOfOrigin: CountryCode")
            mediaArgs.add("countryOfOrigin: \$countryOfOrigin")
        }

        val queryDecl = queryArgs.joinToString(", ")
        val mediaDecl = mediaArgs.joinToString(", ")

        return """
            query ($queryDecl) {
              Page (page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                  hasNextPage
                  total
                }
                media ($mediaDecl) {
                  id
                  title {
                    romaji
                    english
                    native
                    userPreferred
                  }
                  coverImage {
                    large
                    extraLarge
                  }
                  bannerImage
                  format
                  status
                  episodes
                  genres
                  averageScore
                  popularity
                  season
                  seasonYear
                  description(asHtml: false)
                  countryOfOrigin
                  synonyms
                  studios(isMain: true) {
                    nodes {
                      name
                    }
                  }
                  nextAiringEpisode {
                    id
                    airingAt
                    timeUntilAiring
                    episode
                  }
                  startDate {
                    year
                    month
                    day
                  }
                }
              }
            }
        """.trimIndent()
    }

    val TRENDING_MEDIA_QUERY = """
        query (${'$'}page: Int, ${'$'}perPage: Int) {
          Page (page: ${'$'}page, perPage: ${'$'}perPage) {
            pageInfo {
              hasNextPage
            }
            media (type: ANIME, isAdult: false, sort: [TRENDING_DESC, POPULARITY_DESC]) {
              id
              title {
                romaji
                english
                native
                userPreferred
              }
              coverImage {
                large
                extraLarge
              }
              bannerImage
              format
              status
              episodes
              genres
              averageScore
              popularity
              season
              seasonYear
              description(asHtml: false)
              countryOfOrigin
              synonyms
              studios(isMain: true) {
                nodes {
                  name
                }
              }
              nextAiringEpisode {
                id
                airingAt
                timeUntilAiring
                episode
              }
              startDate {
                year
                month
                day
              }
            }
          }
        }
    """.trimIndent()

    val POPULAR_RELEASING_MEDIA_QUERY = """
        query (${'$'}page: Int, ${'$'}perPage: Int) {
          Page (page: ${'$'}page, perPage: ${'$'}perPage) {
            pageInfo {
              hasNextPage
            }
            media (status: RELEASING, type: ANIME, isAdult: false, sort: [POPULARITY_DESC]) {
              id
              title {
                romaji
                english
                native
                userPreferred
              }
              coverImage {
                large
                extraLarge
              }
              bannerImage
              format
              status
              episodes
              genres
              averageScore
              popularity
              season
              seasonYear
              description(asHtml: false)
              countryOfOrigin
              synonyms
              studios(isMain: true) {
                nodes {
                  name
                }
              }
              nextAiringEpisode {
                id
                airingAt
                timeUntilAiring
                episode
              }
              startDate {
                year
                month
                day
              }
            }
          }
        }
    """.trimIndent()

    val AIRING_SCHEDULE_QUERY = """
        query (${'$'}airingAt_greater: Int, ${'$'}airingAt_lesser: Int, ${'$'}page: Int) {
          Page (page: ${'$'}page, perPage: 50) {
            pageInfo {
              hasNextPage
              total
            }
            airingSchedules (airingAt_greater: ${'$'}airingAt_greater, airingAt_lesser: ${'$'}airingAt_lesser, sort: TIME) {
              id
              airingAt
              episode
              media {
                id
                title {
                  romaji
                  english
                  native
                  userPreferred
                }
                coverImage {
                  large
                  extraLarge
                }
                bannerImage
                format
                status
                episodes
                genres
                averageScore
                popularity
                season
                seasonYear
                description(asHtml: false)
                countryOfOrigin
                synonyms
                studios(isMain: true) {
                  nodes {
                    name
                  }
                }
                nextAiringEpisode {
                  id
                  airingAt
                  timeUntilAiring
                  episode
                }
                startDate {
                  year
                  month
                  day
                }
              }
            }
          }
        }
    """.trimIndent()

    val MEDIA_DETAILS_QUERY = """
        query (${'$'}id: Int) {
          Media (id: ${'$'}id, type: ANIME) {
            id
            title {
              romaji
              english
              native
              userPreferred
            }
            coverImage {
              large
              extraLarge
            }
            bannerImage
            format
            status
            episodes
            genres
            averageScore
            popularity
            season
            seasonYear
            startDate {
              year
              month
              day
            }
            description(asHtml: false)
            countryOfOrigin
            synonyms
            nextAiringEpisode {
              id
              airingAt
              timeUntilAiring
              episode
            }
            studios(isMain: true) {
              nodes {
                name
              }
            }
            characters(perPage: 8, sort: [ROLE, RELEVANCE]) {
              nodes {
                id
                name {
                  full
                }
                image {
                  medium
                }
              }
            }
            relations {
              edges {
                relationType
                node {
                  id
                  type
                  title {
                    romaji
                    english
                    userPreferred
                  }
                  coverImage {
                    medium
                    large
                    extraLarge
                  }
                  format
                  status
                  episodes
                  seasonYear
                  startDate {
                    year
                    month
                    day
                  }
                  averageScore
                }
              }
            }
            recommendations(perPage: 8, sort: [RATING_DESC]) {
              nodes {
                mediaRecommendation {
                  id
                  title {
                    romaji
                    english
                    userPreferred
                  }
                  coverImage {
                    medium
                    large
                  }
                  format
                  averageScore
                }
              }
            }
          }
        }
    """.trimIndent()

    val MEDIA_SEASON_CHAIN_QUERY = """
        query (${'$'}id: Int) {
          Media (id: ${'$'}id, type: ANIME) {
            id
            type
            title {
              romaji
              english
              userPreferred
            }
            coverImage {
              medium
              large
              extraLarge
            }
            format
            status
            episodes
            seasonYear
            startDate {
              year
              month
              day
            }
            averageScore
            relations {
              edges {
                relationType
                node {
                  id
                  type
                  title {
                    romaji
                    english
                    userPreferred
                  }
                  coverImage {
                    medium
                    large
                    extraLarge
                  }
                  format
                  status
                  episodes
                  seasonYear
                  startDate {
                    year
                    month
                    day
                  }
                  averageScore
                }
              }
            }
          }
        }
    """.trimIndent()

    val MEDIA_SEASON_BATCH_QUERY = """
        query (${'$'}ids: [Int]) {
          Page (page: 1, perPage: 50) {
            media (id_in: ${'$'}ids, type: ANIME) {
              id
              type
              title {
                romaji
                english
                userPreferred
              }
              coverImage {
                medium
                large
                extraLarge
              }
              format
              status
              episodes
              seasonYear
              startDate {
                year
                month
                day
              }
              averageScore
              relations {
                edges {
                  relationType
                  node {
                    id
                    type
                    title {
                      romaji
                      english
                      userPreferred
                    }
                    coverImage {
                      medium
                      large
                      extraLarge
                    }
                    format
                    status
                    episodes
                    seasonYear
                    startDate {
                      year
                      month
                      day
                    }
                    averageScore
                  }
                }
              }
            }
          }
        }
    """.trimIndent()
}
