package com.example

import com.example.data.local.AnimeEntity
import com.example.data.remote.AniKotoApiService
import com.example.data.remote.AniKotoDownloadResponse
import com.example.data.remote.AniKotoEpisodeDto
import com.example.data.remote.AniKotoEpisodesResponse
import com.example.data.remote.AniKotoEpisodesResultDto
import com.example.data.remote.AniKotoInfoDto
import com.example.data.remote.AniKotoInfoResponse
import com.example.data.remote.AniKotoSearchPageDto
import com.example.data.remote.AniKotoSearchResponse
import com.example.data.remote.AniKotoServerDto
import com.example.data.remote.AniKotoServersResponse
import com.example.data.remote.AniKotoStreamInfoResponse
import com.example.data.remote.AniKotoStreamQualitiesResponse
import com.example.data.remote.AniKotoStreamResolveDto
import com.example.data.remote.AniKotoStreamResolveResponse
import com.example.data.remote.AniKotoSubtitleDto
import com.example.data.remote.AniKotoSummaryDto
import com.example.data.remote.AniKotoTrendingResponse
import com.example.data.repository.AniKotoRepository
import com.example.data.repository.AniKotoResult
import com.example.data.repository.InternetArchiveRepository
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException

/**
 * Unit and Robolectric tests verifying the AniKotoAPI integration:
 * - Real response schema deserialization (`/trending`, `/search`, `/info`, `/episodes/{animeId}`).
 * - Link/slug normalization (`/anime/naruto`, `naruto-eybxz/ep-1`) and input validation against injection/SSRF.
 * - Direct anime streaming resolution via `/servers` and `/stream/resolve`.
 * - Graceful handling of missing fields, empty states, and network/timeout errors.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AniKotoIntegrationTest {

    private val gson = Gson()

    @Test
    fun `normalizeAnimeSlug handles anime prefix, episode suffix, and blocks unsafe URLs`() {
        assertEquals("naruto", AniKotoRepository.normalizeAnimeSlug("/anime/naruto"))
        assertEquals("naruto-eybxz", AniKotoRepository.normalizeAnimeSlug("naruto-eybxz/ep-1"))
        assertEquals("naruto-eybxz", AniKotoRepository.normalizeAnimeSlug("/anime/naruto-eybxz/ep-220"))
        assertEquals("/anime/naruto-eybxz", AniKotoRepository.toCanonicalAnimeLink("/anime/naruto-eybxz/ep-1"))

        // Reject SSRF / path traversal / query injection attempts
        assertNull(AniKotoRepository.normalizeAnimeSlug("https://evil.example.com/anime/naruto"))
        assertNull(AniKotoRepository.normalizeAnimeSlug("../etc/passwd"))
        assertNull(AniKotoRepository.normalizeAnimeSlug("/anime/naruto?admin=true"))
    }

    @Test
    fun `verified trending and search JSON schemas parse accurately`() {
        val trendingJson = """
            {
              "success": true,
              "results": [
                {
                  "slug": "the-eternal-supreme-li-yunxiao-bd1c8/ep-31",
                  "poster": "https://cdn.anipixcdn.co/thumbnail/37b9ccdafd5e83b27cf23bb1e0089e63.jpg",
                  "title": "The Eternal Supreme, Li Yunxiao",
                  "japaneseTitle": "Wangu Zhizun: Li Yunxiao Zhuan",
                  "sub": 31,
                  "dub": 0,
                  "total": 34,
                  "type": "ONA"
                }
              ]
            }
        """.trimIndent()

        val trendingResponse = gson.fromJson(trendingJson, AniKotoTrendingResponse::class.java)
        assertTrue(trendingResponse.success)
        val mappedTrending = AniKotoRepository.mapSummaryDto(trendingResponse.results!!.first())
        assertNotNull(mappedTrending)
        assertEquals("the-eternal-supreme-li-yunxiao-bd1c8", mappedTrending!!.slug)
        assertEquals("/anime/the-eternal-supreme-li-yunxiao-bd1c8", mappedTrending.rawLink)
        assertEquals("The Eternal Supreme, Li Yunxiao", mappedTrending.title)
        assertEquals(31, mappedTrending.subCount)
        assertEquals(34, mappedTrending.totalEpisodes)

        val searchJson = """
            {
              "success": true,
              "results": {
                "totalPages": 1,
                "data": [
                  {
                    "slug": "naruto-eybxz/ep-1",
                    "animeId": "958",
                    "poster": "https://cdn.anipixcdn.co/thumbnail/71a3cb155f8dc89bf3d0365288219936.jpg",
                    "title": "Naruto",
                    "japaneseTitle": "Naruto",
                    "sub": 220,
                    "dub": 220,
                    "total": 220,
                    "type": "TV",
                    "rating": "7.89",
                    "genres": ["Action", "Adventure", "Shounen"]
                  }
                ]
              }
            }
        """.trimIndent()

        val searchResponse = gson.fromJson(searchJson, AniKotoSearchResponse::class.java)
        assertTrue(searchResponse.success)
        val mappedSearch = AniKotoRepository.mapSummaryDto(searchResponse.results!!.data!!.first())
        assertNotNull(mappedSearch)
        assertEquals("naruto-eybxz", mappedSearch!!.slug)
        assertEquals("958", mappedSearch.animeId)
        assertEquals("7.89", mappedSearch.rating)
        assertEquals(listOf("Action", "Adventure", "Shounen"), mappedSearch.genres)
    }

    @Test
    fun `verified info and episodes JSON schemas parse and handle missing fields gracefully`() {
        val infoJson = """
            {
              "success": true,
              "results": {
                "slug": "naruto-eybxz",
                "animeId": 958,
                "title": "Naruto",
                "japaneseTitle": "Naruto",
                "altNames": "Naruto;NARUTO",
                "poster": "https://cdn.anipixcdn.co/thumbnail/71a3cb155f8dc89bf3d0365288219936.jpg",
                "synopsis": "Moments prior to Naruto Uzumaki's birth...",
                "type": "TV",
                "status": "Finished Airing",
                "malScore": "7.89",
                "episodes": "220",
                "studios": ["Studio Pierrot"],
                "genres": ["Action", "Adventure"],
                "rating": "7.89   7.89 /10"
              }
            }
        """.trimIndent()

        val infoResponse = gson.fromJson(infoJson, AniKotoInfoResponse::class.java)
        val episodes = listOf(
            AniKotoEpisodeDto(
                id = "16638",
                episodeNo = 1,
                slug = "1",
                title = "",
                active = true,
                serverIds = "dXNCT3hNQzk3THhSTW8ySnM5"
            ),
            AniKotoEpisodeDto(
                id = "16639",
                episodeNo = 2,
                slug = "2",
                title = "My Name is Konohamaru!",
                active = true,
                serverIds = "bEp6eG5Z"
            )
        )
        val detail = AniKotoRepository.mapInfoDto(
            dto = infoResponse.results!!,
            fallbackSlug = "naruto-eybxz",
            episodesDto = episodes,
            totalEpisodesFromEndpoint = 220
        )

        assertEquals("naruto-eybxz", detail.slug)
        assertEquals("958", detail.animeId)
        assertEquals("7.89", detail.rating)
        assertEquals(220, detail.totalEpisodesCount)
        assertEquals(2, detail.episodes.size)
        assertEquals("Episode 1", detail.episodes[0].title) // Graceful fallback for empty episode title
        assertEquals("dXNCT3hNQzk3THhSTW8ySnM5", detail.episodes[0].serverIds)
        assertEquals("My Name is Konohamaru!", detail.episodes[1].title)
    }

    @Test
    fun `repository resolves bare link via search, maps episodes, resolves anime stream, and handles timeouts safely`() = runBlocking {
        val fakeApi = object : AniKotoApiService {
            override suspend fun getTrending(): AniKotoTrendingResponse {
                throw SocketTimeoutException("Simulated timeout")
            }

            override suspend fun searchAnime(q: String, keyword: String, page: Int): AniKotoSearchResponse {
                return AniKotoSearchResponse(
                    success = true,
                    results = AniKotoSearchPageDto(
                        totalPages = 1,
                        data = listOf(
                            AniKotoSummaryDto(
                                slug = "naruto-eybxz/ep-1",
                                animeId = "958",
                                title = "Naruto",
                                poster = "https://cdn.anipixcdn.co/poster.jpg",
                                sub = 220,
                                dub = 220,
                                total = 220,
                                type = "TV",
                                rating = "7.89",
                                genres = listOf("Action")
                            )
                        )
                    )
                )
            }

            override suspend fun getAnimeInfo(link: String, slug: String, id: String): AniKotoInfoResponse {
                assertEquals("/anime/naruto-eybxz", link)
                assertEquals("naruto-eybxz", slug)
                return AniKotoInfoResponse(
                    success = true,
                    results = AniKotoInfoDto(
                        slug = "naruto-eybxz",
                        animeId = 958,
                        title = "Naruto",
                        poster = "https://cdn.anipixcdn.co/poster.jpg",
                        synopsis = "Ninja story",
                        episodes = "220",
                        rating = "7.89",
                        genres = listOf("Action", "Shounen")
                    )
                )
            }

            override suspend fun getAnimeEpisodes(animeId: String): AniKotoEpisodesResponse {
                assertEquals("958", animeId)
                return AniKotoEpisodesResponse(
                    success = true,
                    results = AniKotoEpisodesResultDto(
                        animeId = 958,
                        slug = "958",
                        totalEpisodes = 2,
                        episodes = listOf(
                            AniKotoEpisodeDto(
                                id = "16638",
                                episodeNo = 1,
                                slug = "1",
                                title = "Enter: Naruto Uzumaki!",
                                active = true,
                                serverIds = "server_token_ep1"
                            ),
                            AniKotoEpisodeDto(
                                id = "16639",
                                episodeNo = 2,
                                slug = "2",
                                title = "My Name is Konohamaru!",
                                active = true,
                                serverIds = "server_token_ep2"
                            )
                        )
                    )
                )
            }

            override suspend fun getEpisodeServers(ids: String): AniKotoServersResponse {
                assertEquals("server_token_ep1", ids)
                return AniKotoServersResponse(
                    success = true,
                    results = listOf(
                        AniKotoServerDto(
                            type = "sub",
                            epId = "16638",
                            linkId = "link_hd1_16638",
                            svId = "41",
                            name = "HD-1"
                        )
                    )
                )
            }

            override suspend fun resolveStreamUrl(linkId: String, slug: String?): AniKotoStreamResolveResponse {
                assertEquals("link_hd1_16638", linkId)
                return AniKotoStreamResolveResponse(
                    success = true,
                    results = AniKotoStreamResolveDto(
                        url = "https://cdn.anikotostream.example/hls/naruto-ep1/master.m3u8",
                        type = "hls",
                        server = "HD-1",
                        subtitles = listOf(
                            AniKotoSubtitleDto(
                                url = "https://cdn.anikotostream.example/subs/naruto-ep1-en.vtt",
                                label = "English"
                            )
                        )
                    )
                )
            }

            override suspend fun getStreamInfo(linkId: String): AniKotoStreamInfoResponse {
                return AniKotoStreamInfoResponse(success = false)
            }

            override suspend fun getStreamQualities(playlistUrl: String): AniKotoStreamQualitiesResponse {
                return AniKotoStreamQualitiesResponse(success = false)
            }

            override suspend fun getDownloadLinks(slug: String, ep: Int): AniKotoDownloadResponse {
                return AniKotoDownloadResponse(success = false)
            }
        }

        val repo = AniKotoRepository(fakeApi)

        // 1. Timeout handling never crashes and sets isTimeout = true
        val trendingRes = repo.getTrendingAnime(forceRefresh = true)
        assertTrue(trendingRes is AniKotoResult.Error)
        assertTrue((trendingRes as AniKotoResult.Error).isTimeout)

        // 2. Search works with GET /search?q=naruto
        val searchRes = repo.searchAnime("naruto")
        assertTrue(searchRes is AniKotoResult.Success)
        assertEquals(1, (searchRes as AniKotoResult.Success).data.size)

        // 3. Info works with GET /info?link=/anime/naruto (resolves naruto -> naruto-eybxz and fetches episodes/958)
        val infoRes = repo.getAnimeInfo("/anime/naruto", forceRefresh = true)
        assertTrue(infoRes is AniKotoResult.Success)
        val detail = (infoRes as AniKotoResult.Success).data
        assertEquals("naruto-eybxz", detail.slug)
        assertEquals("Naruto", detail.title)
        assertEquals(2, detail.episodes.size)

        // 4. Stream resolution uses AniKotoAPI (/servers -> /stream/resolve) and returns playable HLS stream + subtitles
        val animeEntity = AnimeEntity(
            id = 20,
            titleRomaji = "Naruto",
            titleEnglish = "Naruto",
            coverImage = "https://cdn.anipixcdn.co/poster.jpg",
            airingDayOfWeek = 4,
            totalEpisodes = 220,
            watchedEpisodeCount = 0
        )
        val streamRes = repo.resolveAnimeStream(
            anime = animeEntity,
            episodeNumber = 1,
            seasonNumber = 1,
            archiveRepository = InternetArchiveRepository()
        )
        assertTrue(streamRes is AniKotoResult.Success)
        val resolvedStream = (streamRes as AniKotoResult.Success).data
        assertEquals("naruto-eybxz", resolvedStream.aniKotoSlug)
        assertEquals("HD-1", resolvedStream.serverName)
        assertEquals("SUB", resolvedStream.audioType)
        assertEquals("https://cdn.anikotostream.example/hls/naruto-ep1/master.m3u8", resolvedStream.playableMedia.streamUrl)
        assertEquals("HLS", resolvedStream.playableMedia.format)
        assertEquals(1, resolvedStream.primaryCandidate.subtitleTracks.size)
    }
}
