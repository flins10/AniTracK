package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.AnimeEntity
import com.example.data.repository.AnimeRepository
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
    assertEquals("AniTrAk", appName)
  }

  @Test
  fun `tombstone prevents deleted anime from reappearing after sync and allows readding`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val repository = AnimeRepository(db.animeDao(), db.episodeHistoryDao(), db.apiCacheDao(), db.deletedAnimeDao())

    val testAnime = AnimeEntity(
      id = 999123,
      idMal = 999123,
      titleRomaji = "Test Anime Removal",
      titleEnglish = "Test Anime Removal",
      coverImage = "https://example.com/cover.jpg",
      inWatchlist = true,
      airingDayOfWeek = 1
    )

    // 1. Add anime to watchlist
    repository.addToWatchlist(testAnime)
    assertFalse(repository.isDeleted(999123, 999123, "WATCHLIST"))

    // 2. Remove from watchlist (records tombstone)
    repository.removeFromWatchlist(999123, 999123)
    val inWatchlistAfterRemove = db.animeDao().getAnimeByIdSync(999123)?.inWatchlist ?: false
    assertFalse(inWatchlistAfterRemove)
    assertTrue(repository.isDeleted(999123, 999123, "WATCHLIST"))

    // 3. Intentional re-adding clears tombstone
    repository.addToWatchlist(testAnime)
    val inWatchlistAfterReadd = db.animeDao().getAnimeByIdSync(999123)?.inWatchlist ?: false
    assertTrue(inWatchlistAfterReadd)
    assertFalse(repository.isDeleted(999123, 999123, "WATCHLIST"))
  }

  @Test
  fun `mal oauth manager saves and retrieves credentials without encryption`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val oauthManager = com.example.data.remote.MyAnimeListOAuthManager(context)

    oauthManager.saveClientCredentials("test_mal_client_id_999", "anitrack://mal-callback")
    assertEquals("test_mal_client_id_999", oauthManager.customClientId)
    assertEquals("anitrack://mal-callback", oauthManager.customRedirectUri)
    assertTrue(oauthManager.hasConfiguredClientId)

    oauthManager.connectWithUsername("test_user")
    assertEquals("test_user", oauthManager.connectedUsername)
    assertTrue(oauthManager.isLoggedIn)

    oauthManager.logout()
    assertFalse(oauthManager.isLoggedIn)
  }
}
