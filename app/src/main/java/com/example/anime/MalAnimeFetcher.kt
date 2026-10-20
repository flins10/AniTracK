package com.example.anime

import com.example.data.remote.MyAnimeListConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// ============================================================
// MAL API MODELS
// ============================================================

data class MalAnimeResponse(
    val id: Int,
    val title: String?,
    val status: String?,
    val num_episodes: Int?,
    val start_date: String?,
    val end_date: String?
)

data class MalEpisodeResponse(
    val data: List<MalEpisodeWrapper>?,
    val paging: MalPaging?
)

data class MalEpisodeWrapper(
    val node: MalEpisode?
)

data class MalEpisode(
    val mal_id: Int,
    val title: String?,
    val aired: String?,
    val score: Double?,
    val url: String?
)

data class MalPaging(
    val next: String?
)

// ============================================================
// RESULT FOR YOUR ANDROID APP
// ============================================================

data class AnimeAiringInfo(
    val malId: Int,
    val title: String?,
    val status: String?,
    /*
     * MAL's total episode value.
     * null means MAL has not supplied a total.
     */
    val totalEpisodes: Int?,
    /*
     * Latest episode whose MAL "aired" timestamp
     * is <= the current time.
     */
    val currentAiredEpisode: Int?,
    val currentAiredEpisodeTitle: String?,
    /*
     * ISO-8601 timestamp supplied by MAL.
     */
    val currentAiredAt: String?,
    /*
     * Next episode after the current aired episode.
     */
    val nextEpisode: Int?,
    val nextEpisodeTitle: String?,
    /*
     * ISO-8601 timestamp supplied by MAL.
     */
    val nextEpisodeReleaseAt: String?,
    /*
     * True only when a future episode with a valid MAL
     * aired timestamp exists.
     */
    val hasScheduledNextEpisode: Boolean,
    /*
     * Number of episodes MAL has currently exposed.
     */
    val malEpisodesFetched: Int,
    /*
     * Timestamp when your app performed the check.
     */
    val checkedAt: String,
    /*
     * Always "MyAnimeList".
     */
    val source: String = "MyAnimeList"
)

// ============================================================
// MAL API
// ============================================================

interface MalApi {
    @GET("anime/{animeId}")
    suspend fun getAnime(
        @Header("X-MAL-CLIENT-ID")
        clientId: String,
        @Path("animeId")
        animeId: Int,
        @Query("fields")
        fields: String =
            "id,title,status,num_episodes,start_date,end_date"
    ): Response<MalAnimeResponse>

    @GET("anime/{animeId}/episodes")
    suspend fun getEpisodes(
        @Header("X-MAL-CLIENT-ID")
        clientId: String,
        @Path("animeId")
        animeId: Int,
        @Query("limit")
        limit: Int = 100,
        @Query("offset")
        offset: Int = 0
    ): Response<MalEpisodeResponse>
}

// ============================================================
// RETROFIT CLIENT
// ============================================================

object MalClient {
    private val retrofit: Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.myanimelist.net/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val api: MalApi =
        retrofit.create(MalApi::class.java)
}

// ============================================================
// FETCHER
// ============================================================

class MalAnimeFetcher(
    private val clientId: String = ""
) {
    private val api = MalClient.api

    suspend fun getAnimeAiringInfo(
        animeId: Int
    ): Result<AnimeAiringInfo> = withContext(Dispatchers.IO) {
        try {
            if (animeId <= 0) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid MAL anime ID.")
                )
            }

            val validClientId = clientId.ifBlank { "" }

            // ------------------------------------------------
            // 1. GET ANIME METADATA (MAL API or Jikan fallback)
            // ------------------------------------------------
            var animeTitle: String? = null
            var animeStatus: String? = null
            var numEpisodes: Int? = null

            val animeResponse = try {
                if (validClientId.isNotBlank()) {
                    api.getAnime(clientId = validClientId, animeId = animeId)
                } else null
            } catch (e: Exception) {
                null
            }

            if (animeResponse != null && animeResponse.isSuccessful) {
                val anime = animeResponse.body()
                if (anime != null) {
                    animeTitle = anime.title
                    animeStatus = anime.status
                    numEpisodes = anime.num_episodes
                }
            }

            // ------------------------------------------------
            // 2. FETCH EPISODES (Try MAL API then Fallback to Jikan)
            // ------------------------------------------------
            val allEpisodes = mutableListOf<MalEpisode>()
            var fetchedFromMal = false

            if (animeTitle != null && validClientId.isNotBlank()) {
                try {
                    var offset = 0
                    val limit = 100

                    while (true) {
                        val response = api.getEpisodes(
                            clientId = validClientId,
                            animeId = animeId,
                            limit = limit,
                            offset = offset
                        )

                        if (!response.isSuccessful) break

                        val page = response.body() ?: break
                        val pageEpisodes = page.data?.mapNotNull { it.node } ?: emptyList()

                        if (pageEpisodes.isEmpty()) break

                        allEpisodes.addAll(pageEpisodes)
                        fetchedFromMal = true

                        if (page.paging?.next == null) break
                        offset += limit
                    }
                } catch (e: Exception) {
                    // MAL episode endpoint failed or return HTTP 400
                }
            }

            // If MAL failed or returned 0 episodes, fetch from Jikan
            if (!fetchedFromMal || allEpisodes.isEmpty()) {
                return@withContext fetchJikanAnimeAndEpisodes(
                    animeId = animeId,
                    existingTitle = animeTitle,
                    existingStatus = animeStatus,
                    existingNumEpisodes = numEpisodes
                )
            }

            // ------------------------------------------------
            // 3. BUILD RESULT
            // ------------------------------------------------
            buildAiringInfoResult(
                animeId = animeId,
                title = animeTitle,
                status = animeStatus,
                numEpisodes = numEpisodes,
                allEpisodes = allEpisodes
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchJikanAnimeAndEpisodes(
        animeId: Int,
        existingTitle: String?,
        existingStatus: String?,
        existingNumEpisodes: Int?
    ): Result<AnimeAiringInfo> = withContext(Dispatchers.IO) {
        try {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            var title = existingTitle
            var status = existingStatus
            var numEpisodes = existingNumEpisodes

            // 1. Fetch anime details if missing
            if (title == null || status == null) {
                val detailsUrl = "https://api.jikan.moe/v4/anime/$animeId"
                val req = Request.Builder()
                    .url(detailsUrl)
                    .addHeader("User-Agent", "OtakuApp/1.0")
                    .get()
                    .build()
                try {
                    okHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val str = resp.body?.string() ?: ""
                            val json = JSONObject(str).optJSONObject("data")
                            if (json != null) {
                                title = title ?: json.optString("title", null)
                                status = status ?: json.optString("status", null)
                                if (numEpisodes == null && json.has("episodes") && !json.isNull("episodes")) {
                                    val eps = json.optInt("episodes", 0)
                                    if (eps > 0) numEpisodes = eps
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Fetch episodes list from Jikan
            val jikanEpisodes = mutableListOf<MalEpisode>()
            val episodesUrl = "https://api.jikan.moe/v4/anime/$animeId/episodes"
            val epReq = Request.Builder()
                .url(episodesUrl)
                .addHeader("User-Agent", "OtakuApp/1.0")
                .get()
                .build()

            try {
                okHttpClient.newCall(epReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val str = resp.body?.string() ?: ""
                        val root = JSONObject(str)
                        val dataArr = root.optJSONArray("data")
                        val pagination = root.optJSONObject("pagination")
                        val lastPage = pagination?.optInt("last_visible_page", 1) ?: 1

                        if (dataArr != null) {
                            for (i in 0 until dataArr.length()) {
                                val obj = dataArr.optJSONObject(i) ?: continue
                                val epId = obj.optInt("mal_id", i + 1)
                                val epTitle = obj.optString("title", null)
                                val epAired = obj.optString("aired", null)
                                jikanEpisodes.add(
                                    MalEpisode(
                                        mal_id = epId,
                                        title = epTitle,
                                        aired = epAired,
                                        score = null,
                                        url = null
                                    )
                                )
                            }
                        }

                        // Fetch last page if multi-page to get highest episode number
                        if (lastPage > 1) {
                            val lastPageUrl = "https://api.jikan.moe/v4/anime/$animeId/episodes?page=$lastPage"
                            val lpReq = Request.Builder()
                                .url(lastPageUrl)
                                .addHeader("User-Agent", "OtakuApp/1.0")
                                .get()
                                .build()
                            okHttpClient.newCall(lpReq).execute().use { lpResp ->
                                if (lpResp.isSuccessful) {
                                    val lpStr = lpResp.body?.string() ?: ""
                                    val lpDataArr = JSONObject(lpStr).optJSONArray("data")
                                    if (lpDataArr != null) {
                                        for (i in 0 until lpDataArr.length()) {
                                            val obj = lpDataArr.optJSONObject(i) ?: continue
                                            val epId = obj.optInt("mal_id", i + 1)
                                            val epTitle = obj.optString("title", null)
                                            val epAired = obj.optString("aired", null)
                                            if (jikanEpisodes.none { it.mal_id == epId }) {
                                                jikanEpisodes.add(
                                                    MalEpisode(
                                                        mal_id = epId,
                                                        title = epTitle,
                                                        aired = epAired,
                                                        score = null,
                                                        url = null
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            buildAiringInfoResult(
                animeId = animeId,
                title = title,
                status = status,
                numEpisodes = numEpisodes,
                allEpisodes = jikanEpisodes
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildAiringInfoResult(
        animeId: Int,
        title: String?,
        status: String?,
        numEpisodes: Int?,
        allEpisodes: List<MalEpisode>
    ): Result<AnimeAiringInfo> {
        val now = Instant.now()
        val episodesWithDates = allEpisodes.mapNotNull { episode ->
            val airedText = episode.aired?.takeIf { it.isNotBlank() }
            val airedInstant = parseIsoOrDateInstant(airedText)
            if (airedInstant != null) {
                DatedEpisode(episode = episode, airedAt = airedInstant)
            } else {
                null
            }
        }

        val airedEpisodes = episodesWithDates
            .filter { !it.airedAt.isAfter(now) }
            .sortedBy { it.airedAt }

        val currentEpisode = airedEpisodes.lastOrNull()
        val futureEpisodes = episodesWithDates
            .filter { it.airedAt.isAfter(now) }
            .sortedBy { it.airedAt }

        val nextEpisode = futureEpisodes.firstOrNull()

        val maxFetchedEpId = if (allEpisodes.isNotEmpty()) allEpisodes.maxOf { it.mal_id } else null
        val isFinished = status?.lowercase() in listOf("finished_airing", "finished", "currently_finished")
        val finalCurrentAired = if (isFinished) {
            numEpisodes ?: maxFetchedEpId ?: currentEpisode?.episode?.mal_id ?: 1
        } else {
            maxOf(maxFetchedEpId ?: 0, currentEpisode?.episode?.mal_id ?: 0, 1)
        }

        val nextEpId = if (isFinished) {
            numEpisodes ?: finalCurrentAired
        } else {
            nextEpisode?.episode?.mal_id ?: (finalCurrentAired + 1)
        }

        val result = AnimeAiringInfo(
            malId = animeId,
            title = title,
            status = status,
            totalEpisodes = numEpisodes,
            currentAiredEpisode = finalCurrentAired,
            currentAiredEpisodeTitle = currentEpisode?.episode?.title,
            currentAiredAt = currentEpisode?.airedAt?.toString(),
            nextEpisode = nextEpId,
            nextEpisodeTitle = nextEpisode?.episode?.title,
            nextEpisodeReleaseAt = nextEpisode?.airedAt?.toString(),
            hasScheduledNextEpisode = nextEpisode != null,
            malEpisodesFetched = allEpisodes.size,
            checkedAt = now.toString()
        )
        return Result.success(result)
    }

    private fun parseIsoOrDateInstant(text: String?): Instant? {
        if (text.isNullOrBlank()) return null
        val clean = text.trim()
        return try {
            Instant.parse(clean)
        } catch (_: Exception) {
            try {
                java.time.OffsetDateTime.parse(clean).toInstant()
            } catch (_: Exception) {
                try {
                    java.time.ZonedDateTime.parse(clean).toInstant()
                } catch (_: Exception) {
                    try {
                        val datePart = clean.take(10)
                        java.time.LocalDate.parse(datePart).atStartOfDay(ZoneId.of("UTC")).toInstant()
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }

    private data class DatedEpisode(
        val episode: MalEpisode,
        val airedAt: Instant
    )
}

// ============================================================
// DATE/TIME FORMATTER
// ============================================================

object AnimeDateFormatter {
    private val formatter = DateTimeFormatter.ofPattern(
        "EEEE, dd MMMM yyyy   hh:mm a"
    )

    fun formatForDevice(isoTimestamp: String?): String? {
        if (isoTimestamp == null) return null
        return try {
            Instant.parse(isoTimestamp)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        } catch (_: Exception) {
            null
        }
    }
}
