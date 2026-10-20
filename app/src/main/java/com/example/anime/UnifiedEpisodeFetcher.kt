package com.example.anime

import android.util.Log
import com.example.data.local.AnimeEntity
import com.example.data.remote.AiVerificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AggregatedEpisodeInfo(
    val animeId: Int,
    val title: String,
    val titleEnglish: String? = null,
    val latestAiredEpisode: Int,
    val nextEpisodeNumber: Int?,
    val totalEpisodes: Int?,
    val status: String,
    val nextAiringTimeUnix: Long?,
    val sourcesConsulted: List<String>
)

object UnifiedEpisodeFetcher {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /**
     * Cross-checks and aggregates latest episode info across AniList, MAL, and Jikan.
     * For Donghua (Chinese animation), AniList is strictly authoritative for English titles and episode status.
     * Guarantees no hallucinated future airing dates for completed shows or unconfirmed Hentai/OVAs.
     */
    suspend fun fetchAggregatedEpisodeInfo(
        anime: AnimeEntity,
        accessToken: String? = null
    ): AggregatedEpisodeInfo = withContext(Dispatchers.IO) {
        val titleToSearch = (anime.titleEnglish ?: anime.titleRomaji).trim()
        val sourcesConsulted = mutableListOf<String>()
        val isDonghua = anime.isDonghua

        coroutineScope {
            // 1. ANILIST QUERY (Authoritative)
            val anilistJob = async {
                fetchAniListEpisodeInfo(anime.id, anime.idMal, titleToSearch)
            }

            // 2. MAL & JIKAN QUERY (Only for non-Donghua or secondary fallback)
            val malJob = if (!isDonghua) {
                async {
                    val malId = anime.idMal ?: anime.id
                    val fetcher = MalAnimeFetcher(accessToken ?: "")
                    fetcher.getAnimeAiringInfo(malId).getOrNull()
                }
            } else null

            val anilistRes = try { anilistJob.await() } catch (e: Exception) { null }
            val malRes = try { malJob?.await() } catch (e: Exception) { null }

            if (anilistRes != null) sourcesConsulted.add("AniList")
            if (malRes != null) sourcesConsulted.add("MyAnimeList/Jikan")

            val latestAired: Int
            val totalEps: Int?
            val isFinished: Boolean
            val nextEp: Int?
            val nextAiringUnix: Long?

            if (isDonghua) {
                // Strictly use AniList data and known anchors for Donghua
                val anilistAired = anilistRes?.latestAiredEpisode
                val donghuaAired = com.example.util.EpisodeDataParser.resolveDonghuaAiredEpisodes(
                    mediaId = anime.id,
                    title = anime.titleEnglish ?: anime.titleRomaji,
                    existingAired = anilistAired ?: anime.currentEpisode
                )
                latestAired = maxOf(donghuaAired, anilistAired ?: 0, anime.currentEpisode, 1)
                totalEps = anilistRes?.totalEpisodes ?: anime.totalEpisodes
                val isFin = anilistRes?.status == "FINISHED" || (totalEps != null && totalEps > 0 && latestAired >= totalEps)
                isFinished = isFin
                nextEp = if (isFinished) null else (latestAired + 1)
                nextAiringUnix = if (isFinished) null else anilistRes?.nextAiringUnix
            } else {
                val candidatesAired = listOfNotNull(
                    anilistRes?.latestAiredEpisode,
                    malRes?.currentAiredEpisode,
                    anime.currentEpisode
                )
                latestAired = candidatesAired.maxOrNull() ?: anime.currentEpisode
                totalEps = anilistRes?.totalEpisodes
                    ?: malRes?.totalEpisodes
                    ?: anime.totalEpisodes
                val isFinishedByStatus = (anilistRes?.status == "FINISHED" ||
                        malRes?.status?.lowercase() in listOf("finished_airing", "finished") ||
                        anime.status == "FINISHED")
                val isFinishedByCount = totalEps != null && totalEps > 0 && latestAired >= totalEps
                isFinished = isFinishedByStatus || isFinishedByCount
                nextEp = if (isFinished) null else (latestAired + 1)

                // Only attach future airing unix if verified; no hallucinated 7-day increments for completed/hentai
                val isHentaiOrSpecial = anime.genres.contains("Hentai", ignoreCase = true) ||
                        anime.format?.uppercase() in listOf("OVA", "SPECIAL", "MOVIE", "MUSIC")

                nextAiringUnix = if (isFinished || (isHentaiOrSpecial && (anilistRes?.nextAiringUnix == null || anilistRes.nextAiringUnix <= System.currentTimeMillis() / 1000))) {
                    null
                } else {
                    anilistRes?.nextAiringUnix
                        ?: anime.getEffectiveAiringTime()
                }
            }

            val finalStatus = if (isFinished) "FINISHED" else "RELEASING"
            val resolvedEnglishTitle = anilistRes?.titleEnglish ?: anime.titleEnglish

            AggregatedEpisodeInfo(
                animeId = anime.id,
                title = titleToSearch,
                titleEnglish = resolvedEnglishTitle,
                latestAiredEpisode = latestAired,
                nextEpisodeNumber = nextEp,
                totalEpisodes = totalEps,
                status = finalStatus,
                nextAiringTimeUnix = nextAiringUnix,
                sourcesConsulted = sourcesConsulted
            )
        }
    }

    private data class AniListEpisodeResult(
        val titleEnglish: String?,
        val latestAiredEpisode: Int?,
        val nextEpisode: Int?,
        val totalEpisodes: Int?,
        val status: String?,
        val nextAiringUnix: Long?
    )

    private fun fetchAniListEpisodeInfo(anilistId: Int, malId: Int?, title: String): AniListEpisodeResult? {
        try {
            val queryStr = """
                query (${'$'}id: Int, ${'$'}idMal: Int, ${'$'}search: String) {
                  Media (id: ${'$'}id, idMal: ${'$'}idMal, search: ${'$'}search, type: ANIME) {
                    id
                    idMal
                    status
                    episodes
                    title {
                      english
                      romaji
                    }
                    nextAiringEpisode {
                      episode
                      airingAt
                    }
                  }
                }
            """.trimIndent()

            val variablesObj = JSONObject().apply {
                if (malId != null && malId > 0) {
                    put("idMal", malId)
                } else if (anilistId > 0) {
                    put("id", anilistId)
                } else {
                    put("search", title)
                }
            }

            val jsonReq = JSONObject().apply {
                put("query", queryStr)
                put("variables", variablesObj)
            }

            val body = jsonReq.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return fetchAniListByTitleFallback(title)
                }
                val str = response.body?.string() ?: return fetchAniListByTitleFallback(title)
                val root = JSONObject(str)
                val media = root.optJSONObject("data")?.optJSONObject("Media") ?: return fetchAniListByTitleFallback(title)
                val titleObj = media.optJSONObject("title")
                val english = titleObj?.optString("english", null)?.takeIf { it.isNotBlank() && it != "null" }
                val status = media.optString("status", null)
                val totalEps = media.optInt("episodes", 0).takeIf { it > 0 }
                val nextAiring = media.optJSONObject("nextAiringEpisode")

                return if (nextAiring != null) {
                    val nextEp = nextAiring.optInt("episode", 1)
                    val airingAt = nextAiring.optLong("airingAt", 0).takeIf { it > 0 }
                    val now = System.currentTimeMillis() / 1000
                    val latestAired = if (airingAt != null && airingAt > 0 && now >= airingAt) {
                        val extraWeeks = ((now - airingAt) / 604800L).toInt()
                        nextEp + extraWeeks
                    } else {
                        maxOf(1, nextEp - 1)
                    }
                    val actualNextEp = if (airingAt != null && airingAt > 0 && now >= airingAt) latestAired + 1 else nextEp
                    AniListEpisodeResult(
                        titleEnglish = english,
                        latestAiredEpisode = latestAired,
                        nextEpisode = actualNextEp,
                        totalEpisodes = totalEps,
                        status = status,
                        nextAiringUnix = airingAt
                    )
                } else {
                    val aired = totalEps ?: 1
                    AniListEpisodeResult(
                        titleEnglish = english,
                        latestAiredEpisode = aired,
                        nextEpisode = aired,
                        totalEpisodes = totalEps,
                        status = status,
                        nextAiringUnix = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("UnifiedEpisodeFetcher", "AniList fetch error: ${e.message}")
            return fetchAniListByTitleFallback(title)
        }
    }

    private fun fetchAniListByTitleFallback(title: String): AniListEpisodeResult? {
        try {
            val queryStr = """
                query (${'$'}search: String) {
                  Media (search: ${'$'}search, type: ANIME) {
                    id
                    idMal
                    status
                    episodes
                    title {
                      english
                      romaji
                    }
                    nextAiringEpisode {
                      episode
                      airingAt
                    }
                  }
                }
            """.trimIndent()

            val jsonReq = JSONObject().apply {
                put("query", queryStr)
                put("variables", JSONObject().put("search", title))
            }

            val body = jsonReq.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val str = response.body?.string() ?: return null
                val root = JSONObject(str)
                val media = root.optJSONObject("data")?.optJSONObject("Media") ?: return null
                val titleObj = media.optJSONObject("title")
                val english = titleObj?.optString("english", null)?.takeIf { it.isNotBlank() && it != "null" }
                val status = media.optString("status", null)
                val totalEps = media.optInt("episodes", 0).takeIf { it > 0 }
                val nextAiring = media.optJSONObject("nextAiringEpisode")

                return if (nextAiring != null) {
                    val nextEp = nextAiring.optInt("episode", 1)
                    val airingAt = nextAiring.optLong("airingAt", 0).takeIf { it > 0 }
                    val now = System.currentTimeMillis() / 1000
                    val latestAired = if (airingAt != null && airingAt > 0 && now >= airingAt) {
                        val extraWeeks = ((now - airingAt) / 604800L).toInt()
                        nextEp + extraWeeks
                    } else {
                        maxOf(1, nextEp - 1)
                    }
                    val actualNextEp = if (airingAt != null && airingAt > 0 && now >= airingAt) latestAired + 1 else nextEp
                    AniListEpisodeResult(
                        titleEnglish = english,
                        latestAiredEpisode = latestAired,
                        nextEpisode = actualNextEp,
                        totalEpisodes = totalEps,
                        status = status,
                        nextAiringUnix = airingAt
                    )
                } else {
                    val aired = totalEps ?: 1
                    AniListEpisodeResult(
                        titleEnglish = english,
                        latestAiredEpisode = aired,
                        nextEpisode = aired,
                        totalEpisodes = totalEps,
                        status = status,
                        nextAiringUnix = null
                    )
                }
            }
        } catch (e: Exception) {
            return null
        }
    }
}
