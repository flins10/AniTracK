package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AnimeEntity
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import com.example.data.repository.PreferencesRepository
import com.example.model.ThemeModePreference
import com.example.model.WatchStatus
import com.example.model.WatchlistLayoutMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Anime Universe", appName)
    }

    @Test
    fun `tombstone prevents deleted anime from reappearing and allows readding`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repository = AnimeRepository(
            animeDao = db.animeDao(),
            historyDao = db.episodeHistoryDao(),
            apiCacheDao = db.apiCacheDao(),
            deletedAnimeDao = db.deletedAnimeDao(),
            savedAnimeDao = db.savedAnimeDao()
        )

        val testAnime = AnimeEntity(
            id = 999123,
            titleRomaji = "Test Anime Removal",
            titleEnglish = "Test Anime Removal",
            coverImage = "https://example.com/cover.jpg",
            inWatchlist = true,
            airingDayOfWeek = 1
        )

        repository.addToWatchlist(testAnime, WatchStatus.WATCHING)
        assertFalse(repository.isDeleted(999123, "WATCHLIST"))

        repository.removeFromWatchlist(999123)
        val inWatchlistAfterRemove = db.animeDao().getAnimeByIdSync(999123)?.inWatchlist ?: false
        assertFalse(inWatchlistAfterRemove)
        assertTrue(repository.isDeleted(999123, "WATCHLIST"))

        repository.addToWatchlist(testAnime, WatchStatus.WATCHING)
        val inWatchlistAfterReadd = db.animeDao().getAnimeByIdSync(999123)?.inWatchlist ?: false
        assertTrue(inWatchlistAfterReadd)
        assertFalse(repository.isDeleted(999123, "WATCHLIST"))
    }

    @Test
    fun `preferences repository persists theme, layout, and safe mode`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesRepository(context)
        prefs.setThemeMode(ThemeModePreference.LIGHT)
        prefs.setWatchlistLayout(WatchlistLayoutMode.LIST)
        prefs.setSafeModeEnabled(false)

        val current = prefs.preferencesFlow.first()
        assertEquals(ThemeModePreference.LIGHT, current.themeMode)
        assertEquals(WatchlistLayoutMode.LIST, current.watchlistLayout)
        assertFalse(current.safeModeEnabled)
    }

    @Test
    fun `preferences repository persists supabase url and anon key`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesRepository(context)
        prefs.setSupabaseConfig("https://demo.supabase.co/", "sb_publishable_test_key_123456789")

        val current = prefs.preferencesFlow.first()
        assertEquals("https://demo.supabase.co", current.supabaseUrl)
        assertEquals("sb_publishable_test_key_123456789", current.supabaseAnonKey)
    }

    @Test
    fun `pruneAndCompactStorage retains tracked anime while trimming excess untracked catalog cache`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repository = AnimeRepository(
            animeDao = db.animeDao(),
            historyDao = db.episodeHistoryDao(),
            apiCacheDao = db.apiCacheDao(),
            deletedAnimeDao = db.deletedAnimeDao(),
            savedAnimeDao = db.savedAnimeDao()
        )

        val tracked = AnimeEntity(
            id = 880001,
            titleRomaji = "Tracked Gem",
            titleEnglish = "Tracked Gem",
            coverImage = "https://example.com/tracked.jpg",
            inWatchlist = true
        )
        repository.addToWatchlist(tracked, WatchStatus.WATCHING)

        val untrackedItems = (1..100).map { idx ->
            AnimeEntity(
                id = 890000 + idx,
                titleRomaji = "Discover Entry $idx",
                titleEnglish = "Discover Entry $idx",
                coverImage = "https://example.com/u$idx.jpg",
                inWatchlist = false,
                synopsis = "A".repeat(900),
                lastCachedAt = System.currentTimeMillis() - idx * 1000L
            )
        }
        repository.upsertCatalogItems(untrackedItems, keepFullSynopsis = false)

        repository.pruneAndCompactStorage()

        val trackedStillExists = db.animeDao().getAnimeByIdSync(880001)
        assertTrue(trackedStillExists != null && trackedStillExists.inWatchlist)
        val trimmedSynopsisLen = db.animeDao().getAnimeByIdSync(890001)?.synopsis?.length ?: 0
        assertTrue(trimmedSynopsisLen <= 360)
    }
}
