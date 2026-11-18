package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AnimeEntity
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import com.example.data.repository.OwnerVaultStore
import com.example.model.WatchStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserAndGuestIsolationRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AnimeRepository
    private lateinit var vaultStore: OwnerVaultStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("anime_universe_identity_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
        repository = AnimeRepository(
            animeDao = db.animeDao(),
            historyDao = db.episodeHistoryDao(),
            apiCacheDao = db.apiCacheDao(),
            deletedAnimeDao = db.deletedAnimeDao(),
            savedAnimeDao = db.savedAnimeDao()
        )
        vaultStore = OwnerVaultStore(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleAnime(
        id: Int,
        title: String,
        status: WatchStatus? = WatchStatus.WATCHING,
        episodesWatched: Int = 0,
        score: Int? = null,
        notes: String? = null,
        inWatchlist: Boolean = true
    ) = AnimeEntity(
        id = id,
        titleRomaji = title,
        titleEnglish = title,
        coverImage = "https://example.com/$id.jpg",
        totalEpisodes = 24,
        inWatchlist = inWatchlist,
        isWishlist = status == WatchStatus.PLAN_TO_WATCH,
        isWatched = status == WatchStatus.COMPLETED,
        userStatus = status?.dbValue,
        watchedEpisodeCount = episodesWatched,
        userScore = score,
        userNotes = notes,
        airingDayOfWeek = 1
    )

    @Test
    fun `guest IDs are unique UUIDs persisted across restarts and rotated cleanly after logout`() {
        val guest1 = vaultStore.getOrCreateGuestId()
        assertTrue(guest1.startsWith("guest_"))

        // Simulated restart with fresh OwnerVaultStore instance
        val restartedStore = OwnerVaultStore(context)
        assertEquals(guest1, restartedStore.getOrCreateGuestId())

        // Rotating creates a brand-new unique guest ID
        val guest2 = restartedStore.rotateGuestId()
        assertTrue(guest2.startsWith("guest_"))
        assertNotEquals(guest1, guest2)
        assertEquals(guest2, OwnerVaultStore(context).getOrCreateGuestId())
    }

    @Test
    fun `user A user B and guest C data remain strictly isolated even on overlapping catalog anime`() = runBlocking {
        val userAKey = OwnerVaultStore.userKey("user-aaa")
        val userBKey = OwnerVaultStore.userKey("user-bbb")
        val guestId = vaultStore.getOrCreateGuestId()
        val guestKey = OwnerVaultStore.guestKey(guestId)

        // User A tracks Frieren (101) with 9/10 rating and Solo Leveling (102)
        val userAFrieren = sampleAnime(101, "Frieren", WatchStatus.COMPLETED, episodesWatched = 24, score = 90, notes = "User A loved it")
        val userASolo = sampleAnime(102, "Solo Leveling", WatchStatus.WATCHING, episodesWatched = 7, score = 80, notes = "User A watching")
        repository.restoreOwnerTrackedItems(listOf(userAFrieren, userASolo))
        vaultStore.saveOwnerVault(userAKey, repository.getTrackedAnimeSnapshot(), emptyList())

        // User A logs out -> clear active personal tracking state
        repository.clearUserTrackingStateOnly()
        assertTrue(repository.watchlist.first().isEmpty())
        assertTrue(repository.getTrackedAnimeSnapshot().isEmpty())

        // User B signs in and tracks Frieren (101) with completely different status & rating, plus Jujutsu Kaisen (103)
        val userBFrieren = sampleAnime(101, "Frieren", WatchStatus.DROPPED, episodesWatched = 3, score = 40, notes = "User B dropped")
        val userBJjk = sampleAnime(103, "Jujutsu Kaisen", WatchStatus.PLAN_TO_WATCH, episodesWatched = 0, score = null)
        vaultStore.saveOwnerVault(userBKey, listOf(userBFrieren, userBJjk), emptyList())

        val loadedB = vaultStore.loadOwnerVault(userBKey)
        repository.restoreOwnerTrackedItems(loadedB.items, loadedB.deletedAnimeIds)

        val bWatchlist = repository.watchlist.first()
        assertEquals(2, bWatchlist.size)
        assertFalse(bWatchlist.any { it.id == 102 }) // User A's Solo Leveling must NEVER appear in User B
        val bFrieren = repository.getAnimeByIdSync(101)!!
        assertEquals("User B dropped", bFrieren.userNotes)
        assertEquals(40, bFrieren.userScore)
        assertEquals(WatchStatus.DROPPED.dbValue, bFrieren.userStatus)

        // User B logs out and Guest enters -> must start empty with zero leakage from User A or B
        repository.clearUserTrackingStateOnly()
        val loadedGuestEmpty = vaultStore.loadOwnerVault(guestKey)
        repository.restoreOwnerTrackedItems(loadedGuestEmpty.items, loadedGuestEmpty.deletedAnimeIds)
        assertTrue(repository.watchlist.first().isEmpty())

        // Guest adds Dandadan (104)
        val guestDandadan = sampleAnime(104, "Dandadan", WatchStatus.WATCHING, episodesWatched = 4, score = 100, notes = "Guest hype")
        repository.addToWatchlist(guestDandadan, WatchStatus.WATCHING)
        vaultStore.saveOwnerVault(guestKey, repository.getTrackedAnimeSnapshot(), emptyList())

        // Switch back to User A -> User A still has 101 & 102 intact, zero items from B or Guest
        val loadedA = vaultStore.loadOwnerVault(userAKey)
        repository.restoreOwnerTrackedItems(loadedA.items, loadedA.deletedAnimeIds)
        val aWatchlist = repository.watchlist.first()
        assertEquals(setOf(101, 102), aWatchlist.map { it.id }.toSet())
        assertNull(aWatchlist.firstOrNull { it.id == 104 })
        assertEquals("User A loved it", repository.getAnimeByIdSync(101)?.userNotes)
        assertEquals(24, repository.getAnimeByIdSync(101)?.watchedEpisodeCount)
    }

    @Test
    fun `guest to account migration imports unique guest shows without duplicating or overwriting existing account items`() = runBlocking {
        val accountItems = listOf(
            sampleAnime(201, "One Piece", WatchStatus.WATCHING, episodesWatched = 1100, score = 95, notes = "Account progress"),
            sampleAnime(202, "Bleach", WatchStatus.COMPLETED, episodesWatched = 24, score = 85)
        )
        val guestItems = listOf(
            sampleAnime(201, "One Piece", WatchStatus.PLAN_TO_WATCH, episodesWatched = 1, score = 60, notes = "Guest duplicate"),
            sampleAnime(203, "Vinland Saga", WatchStatus.COMPLETED, episodesWatched = 24, score = 100, notes = "Guest unique gem")
        )

        val toMigrate = OwnerVaultStore.computeNonDuplicatingMigrationItems(
            existingAccountItems = accountItems,
            guestItems = guestItems
        )

        assertEquals(1, toMigrate.size)
        assertEquals(203, toMigrate.first().id)
        assertEquals("Guest unique gem", toMigrate.first().userNotes)

        // Verify cloud sync with replaceMissingFromCloud reconciles remote deletions cleanly
        repository.restoreOwnerTrackedItems(accountItems + toMigrate)
        assertEquals(setOf(201, 202, 203), repository.watchlist.first().map { it.id }.toSet())

        // Simulate another device removing 202 (Bleach) in cloud
        repository.syncCloudWatchlistEntries(
            cloudItems = listOf(accountItems.first(), toMigrate.first()),
            replaceMissingFromCloud = true
        )
        assertEquals(setOf(201, 203), repository.watchlist.first().map { it.id }.toSet())
        assertFalse(repository.getAnimeByIdSync(202)?.inWatchlist == true)
    }
}
