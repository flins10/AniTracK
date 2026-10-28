package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.MovieBoxApiService
import com.example.data.repository.MovieBoxDownloadManager
import com.example.data.repository.MovieBoxDownloadState
import com.example.data.repository.MovieBoxDownloadTask
import com.example.data.repository.MovieBoxRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MovieBoxIntegrationTest {

    @Test
    fun authKeyAndVideoHash_followDocumentedMovieBoxFormat() {
        val authKey = MovieBoxApiService.generateAuthKey(nowEpochSeconds = 1753400000L)
        val videoHash = MovieBoxApiService.generateVideoHash()

        val authRegex = Regex("^\\d+-0-0-[0-9a-f]{32}$")
        val hashRegex = Regex("^[0-9a-f]{32}$")

        assertTrue("Expected authKey format timestamp-0-0-32hex, got: $authKey", authRegex.matches(authKey))
        assertTrue("Expected 32-char hex videoHash, got: $videoHash", hashRegex.matches(videoHash))
    }

    @Test
    fun parseMovieListPayload_parsesJsonAndHtmlAndFiltersAnimationAndBracketTags() {
        val service = MovieBoxApiService()

        val jsonPayload = """
            {
              "results": [
                {
                  "id": 101,
                  "title": "Oppenheimer [Hindi] [Verified]",
                  "release_date": "2023-07-21",
                  "vote_average": 8.9,
                  "genres": ["Drama", "History"],
                  "poster_path": "/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
                  "backdrop_path": "/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg",
                  "overview": "The story of J. Robert Oppenheimer.",
                  "media_type": "movie"
                },
                {
                  "id": 102,
                  "title": "Animated Adventure",
                  "release_date": "2024-01-01",
                  "vote_average": 7.5,
                  "genres": ["Animation", "Family"],
                  "poster_path": "/anim.jpg",
                  "media_type": "movie"
                }
              ]
            }
        """.trimIndent()

        val parsedJson = service.parseMovieListPayload(jsonPayload, defaultType = "Movie")
        assertEquals(1, parsedJson.size)
        val oppenheimer = parsedJson.first()
        assertEquals("Oppenheimer", oppenheimer.cleanTitle)
        assertEquals("2023", oppenheimer.year)
        assertEquals("8.9", oppenheimer.rating)
        assertTrue(oppenheimer.poster.startsWith("https://image.tmdb.org/t/p/w500/"))

        val htmlPayload = """
            <html>
              <head>
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org",
                  "@type": "ItemList",
                  "itemListElement": [
                    {
                      "name": "Dune: Part Two",
                      "datePublished": "2024-03-01",
                      "image": "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
                      "genre": ["Action", "Sci-Fi"],
                      "description": "Paul Atreides unites with Chani and the Fremen."
                    }
                  ]
                }
                </script>
              </head>
              <body></body>
            </html>
        """.trimIndent()

        val parsedHtml = service.parseMovieListPayload(htmlPayload, defaultType = "Movie")
        assertEquals(1, parsedHtml.size)
        assertEquals("Dune: Part Two", parsedHtml.first().cleanTitle)
        assertEquals("2024", parsedHtml.first().year)
    }

    @Test
    fun repositoryDiscoveryPaginationSearchAndDetails_returnLiveActionMoviesAndSeries() = runBlocking {
        // Point base URLs to an unreachable localhost port so tests run fast and deterministically
        val offlineService = MovieBoxApiService(
            baseURL = "http://127.0.0.1:9/api",
            webURL = "http://127.0.0.1:9",
            tmdbApiKey = "",
            omdbApiKey = ""
        )
        val repository = MovieBoxRepository(apiService = offlineService)

        val trendingPage1 = repository.getTrending(page = 1)
        val trendingPage2 = repository.getTrending(page = 2)
        assertTrue("Trending page 1 should not be empty", trendingPage1.isNotEmpty())
        assertTrue("Trending page 2 should not be empty", trendingPage2.isNotEmpty())
        assertNotEquals(
            "Page 1 and Page 2 should contain distinct paginated titles",
            trendingPage1.first().id,
            trendingPage2.first().id
        )

        val latestMovies = repository.getLatestMovies(page = 1)
        assertTrue(latestMovies.all { !it.isSeries })
        assertTrue(latestMovies.none { it.genre.contains("Animation", ignoreCase = true) })

        val tvSeries = repository.getTVSeries(page = 1)
        assertTrue(tvSeries.isNotEmpty())
        assertTrue(tvSeries.all { it.isSeries })

        val searchResults = repository.search("Breaking Bad", page = 1)
        assertTrue(searchResults.any { it.cleanTitle.contains("Breaking Bad", ignoreCase = true) })

        val detail = repository.getMovieDetails(1)
        assertNotNull(detail)
        assertEquals("Breaking Bad", detail.cleanTitle)
        assertTrue(detail.cast.isNotEmpty())
        assertTrue(detail.rating.isNotBlank())
    }

    @Test
    fun streamValidationAndFileSanitization_rejectUtmWebLinksAndAcceptDirectMedia() {
        assertTrue(MovieBoxRepository.isDirectMediaStreamFormat("https://archive.org/download/sample/movie.mp4"))
        assertTrue(MovieBoxRepository.isDirectMediaStreamFormat("https://cdn.example.com/hls/master.m3u8?token=abc"))
        assertTrue(MovieBoxRepository.isDirectMediaStreamFormat("https://cdn.example.com/dash/stream.mpd"))
        assertTrue(MovieBoxRepository.isDirectMediaStreamFormat("https://cdn.example.com/video/film.mkv"))

        assertFalse(MovieBoxRepository.isDirectMediaStreamFormat("https://moviebox.ng/?utm_source=moviebox-clone&utm_medium=web&utm_campaign=stream"))
        assertFalse(MovieBoxRepository.isDirectMediaStreamFormat("https://moviebox.ng/web/search?keyword=Squid%20Game"))
        assertFalse(MovieBoxRepository.isDirectMediaStreamFormat(null))
        assertFalse(MovieBoxRepository.isDirectMediaStreamFormat(""))

        val sanitized = MovieBoxRepository.sanitizeFileName("../../Squid Game: Season 1 [Hindi]!", "1080p", "mp4")
        assertFalse(sanitized.contains(".."))
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains("["))
        assertTrue(sanitized.endsWith("_1080p.mp4"))
    }

    @Test
    fun downloadManager_rejectsInvalidUrlsAndVerifiesOfflineFilesBeforePlayback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val downloadManager = MovieBoxDownloadManager(context)

        // Reject null or UTM web links
        val utmResult = downloadManager.startOrResumeDownload(
            contentId = 1,
            title = "Squid Game",
            quality = "720p",
            downloadUrl = "https://moviebox.ng/?utm_source=moviebox-clone"
        )
        assertTrue("UTM web link should be rejected as non-downloadable", utmResult.isFailure)

        // Verify offline file check requires an existing file > 0 bytes on disk
        val tempFile = File(context.cacheDir, "verified_test_movie.mp4")
        tempFile.delete()

        val missingFileTask = MovieBoxDownloadTask(
            taskId = "test_missing",
            contentId = 99,
            title = "Test Movie",
            quality = "720p",
            downloadUrl = "https://example.com/test.mp4",
            destinationPath = tempFile.absolutePath,
            fileName = tempFile.name,
            bytesDownloaded = 1024L,
            totalBytes = 1024L,
            state = MovieBoxDownloadState.COMPLETED
        )
        assertFalse("Task pointing to non-existent file must not be marked playable offline", missingFileTask.isVerifiedPlayableOffline)

        tempFile.writeBytes(ByteArray(2048) { 0x42 })
        assertTrue("Task with completed state and non-empty file on disk should be playable offline", missingFileTask.isVerifiedPlayableOffline)
        tempFile.delete()
    }
}
