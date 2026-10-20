package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AnimeEntity
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import com.example.data.repository.YouTubeRepository
import com.example.data.repository.YouTubeSearchResultState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URLDecoder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YouTubeKeylessWorkflowTest {

    @Test
    fun `default state is keyless mode and api key is not required`() {
        val repository = YouTubeRepository()

        // 1. By default, optional YouTube Data API is disabled
        assertFalse("Optional YouTube API must be disabled by default", repository.isOptionalApiEnabled())

        // 2. No API key is required or active
        assertFalse("No API key should be active or required in default mode", repository.isApiKeyConfigured())
        assertEquals("", repository.getApiKey())
    }

    @Test
    fun `keyless mode generates structured search links from anime title`() = runBlocking {
        val repository = YouTubeRepository()
        val animeTitle = "Frieren: Beyond Journey's End"

        // Search in default keyless mode
        val result = repository.searchAnimeVideos(animeTitle)

        assertTrue("Result should be Keyless state", result is YouTubeSearchResultState.Keyless)
        val keylessState = result as YouTubeSearchResultState.Keyless

        val links = keylessState.searchLinks
        assertTrue("Should produce category links for discovery", links.isNotEmpty())

        // Verify categories exist
        val categories = links.map { it.category }
        assertTrue("Contains Trailers category", categories.contains("Trailers"))
        assertTrue("Contains Episodes category", categories.contains("Episodes"))
        assertTrue("Contains Music category", categories.contains("Music"))
        assertTrue("Contains Distributors category", categories.contains("Distributors"))

        // Verify all links contain official YouTube search URLs
        for (link in links) {
            assertTrue("Link must start with official YouTube search URL", link.webSearchUrl.startsWith("https://www.youtube.com/results?search_query="))
            val decodedUrl = URLDecoder.decode(link.webSearchUrl, "UTF-8")
            assertTrue("Decoded query URL must include title segment", decodedUrl.contains("Frieren"))
        }
    }

    @Test
    fun `anime tracking, watchlists, episode progress, and local persistence function fully without YouTube API key`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val animeRepo = AnimeRepository(db.animeDao(), db.episodeHistoryDao(), db.apiCacheDao(), db.deletedAnimeDao())

        val testAnime = AnimeEntity(
            id = 154587,
            idMal = 52991,
            titleRomaji = "Sousou no Frieren",
            titleEnglish = "Frieren: Beyond Journey's End",
            coverImage = "https://example.com/frieren.jpg",
            totalEpisodes = 28,
            watchedEpisodeCount = 0,
            userScore = 0,
            userStatus = "watching",
            inWatchlist = true,
            airingDayOfWeek = 5
        )

        // 1. Save and add to watchlist
        animeRepo.addToWatchlist(testAnime)
        val saved = animeRepo.getAnimeByIdSync(154587)
        assertNotNull(saved)
        assertEquals("Frieren: Beyond Journey's End", saved?.titleEnglish)
        assertTrue(saved?.inWatchlist == true)

        // 2. Increment watched episode progress
        animeRepo.updateWatchedEpisodeCount(154587, 1)
        val updatedProgress = animeRepo.getAnimeByIdSync(154587)
        assertEquals(1, updatedProgress?.watchedEpisodeCount)

        // 3. Update score and status
        animeRepo.updateUserMediaState(
            animeId = 154587,
            userStatus = "completed",
            userScore = 10,
            watchedCount = 28
        )
        val completed = animeRepo.getAnimeByIdSync(154587)
        assertEquals(10, completed?.userScore)
        assertEquals("completed", completed?.userStatus)
        assertEquals(28, completed?.watchedEpisodeCount)
    }

    @Test
    fun `optional api mode can be toggled without breaking keyless fallback`() = runBlocking {
        val repository = YouTubeRepository()

        // Initially keyless
        assertFalse(repository.isOptionalApiEnabled())
        var result = repository.searchAnimeVideos("Attack on Titan")
        assertTrue(result is YouTubeSearchResultState.Keyless)

        // Enable optional API without configuring a key
        repository.setOptionalApiEnabled(true)
        assertTrue(repository.isOptionalApiEnabled())

        result = repository.searchAnimeVideos("Attack on Titan")
        assertTrue("Without a key, enabling optional API returns error guiding user to toggle off or add key", result is YouTubeSearchResultState.Error)
        val err = result as YouTubeSearchResultState.Error
        assertTrue(err.isMissingKey)

        // Disable optional API -> seamlessly returns to keyless mode
        repository.setOptionalApiEnabled(false)
        assertFalse(repository.isOptionalApiEnabled())

        result = repository.searchAnimeVideos("Attack on Titan")
        assertTrue("Disabling optional API immediately restores keyless search links", result is YouTubeSearchResultState.Keyless)
    }
}
