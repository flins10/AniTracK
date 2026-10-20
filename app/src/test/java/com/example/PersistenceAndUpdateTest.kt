package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AnimeEntity
import com.example.data.local.ApiCacheEntity
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import com.example.data.repository.PlaybackRepository
import com.example.util.ApkUpdateValidator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PersistenceAndUpdateTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AnimeRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
        repository = AnimeRepository(
            animeDao = db.animeDao(),
            historyDao = db.episodeHistoryDao(),
            apiCacheDao = db.apiCacheDao(),
            deletedAnimeDao = db.deletedAnimeDao(),
            savedAnimeDao = db.savedAnimeDao(),
            playbackPreferenceDao = db.playbackPreferenceDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `clearing cache never deletes saved anime, favorites, wishlist, watch history, or playback preferences`() = runBlocking {
        // 1. Save a Watchlist item, a Wishlist item, and a Favorite item
        val savedWatchlist = AnimeEntity(
            id = 101,
            idMal = 101,
            archiveIdentifier = "ia_cowboy_bebop_1998",
            titleRomaji = "Cowboy Bebop",
            titleEnglish = "Cowboy Bebop",
            coverImage = "https://archive.org/services/img/ia_cowboy_bebop_1998",
            inWatchlist = true,
            watchedEpisodeCount = 5,
            airingDayOfWeek = 1
        )
        val savedWishlist = AnimeEntity(
            id = 102,
            idMal = 102,
            titleRomaji = "Trigun",
            titleEnglish = "Trigun",
            coverImage = "https://example.com/trigun.jpg",
            isWishlist = true,
            airingDayOfWeek = 2
        )
        val savedFavorite = AnimeEntity(
            id = 103,
            idMal = 103,
            titleRomaji = "Samurai Champloo",
            titleEnglish = "Samurai Champloo",
            coverImage = "https://example.com/champloo.jpg",
            isFavorite = true,
            inWatchlist = false,
            airingDayOfWeek = 3
        )
        // Transient browsed item (not saved in watchlist, wishlist, or favorites)
        val transientBrowsed = AnimeEntity(
            id = 999,
            idMal = 999,
            titleRomaji = "Transient Browsed Show",
            titleEnglish = "Transient Browsed Show",
            coverImage = "https://example.com/transient.jpg",
            inWatchlist = false,
            isWishlist = false,
            isFavorite = false,
            watchedEpisodeCount = 0,
            airingDayOfWeek = 4
        )

        repository.addToWatchlist(savedWatchlist)
        repository.toggleWishlistEntity(savedWishlist)
        db.animeDao().insertAnime(savedFavorite)
        repository.toggleFavorite(103, true)
        db.animeDao().insertAnime(transientBrowsed)

        // Record episode watch history
        repository.incrementWatchedEpisode(savedWatchlist, 6)

        // Insert transient API cache entry
        db.apiCacheDao().insertCache(ApiCacheEntity.fromIds("search:test", listOf(101, 999)))

        // Clear metadata and API cache
        repository.clearMetadataAndApiCache()

        // Verify transient API cache and transient browsed anime were cleared
        val apiCacheAfter = db.apiCacheDao().getCache("search:test")
        assertEquals(null, apiCacheAfter)
        val transientAfter = db.animeDao().getAnimeByIdSync(999)
        assertEquals(null, transientAfter)

        // Verify saved Watchlist, Wishlist, Favorites, and Watch History remain 100% intact
        val watchlistNow = repository.watchlist.first()
        val wishlistNow = repository.wishlist.first()
        val favoritesNow = repository.favorites.first()
        val historyNow = repository.watchHistory.first()

        assertTrue(watchlistNow.any { it.id == 101 && it.watchedEpisodeCount == 6 && it.archiveIdentifier == "ia_cowboy_bebop_1998" })
        assertTrue(wishlistNow.any { it.id == 102 && it.isWishlist })
        assertTrue(favoritesNow.any { it.id == 103 && it.isFavorite })
        assertTrue(historyNow.any { it.animeId == 101 && it.episodeNumber == 6 })
    }

    @Test
    fun `saved anime remain visible even when main anime cache table is empty or Internet Archive is offline`() = runBlocking {
        // Save an Internet Archive item using stable archiveIdentifier
        val savedEntity = repository.saveArchiveMediaItem(
            archiveIdentifier = "astro-boy-1963-ep01",
            title = "Astro Boy (1963)",
            description = "Classic Tezuka anime on Internet Archive",
            thumbnailUrl = "https://archive.org/services/img/astro-boy-1963-ep01",
            year = 1963,
            inWatchlist = true,
            isWishlist = false
        )

        // Simulate catastrophic loss of cached browse row in `anime` table while `saved_anime` persists
        db.animeDao().deleteAnimeList(listOf(savedEntity.id))

        // Watchlist Flow must still emit the saved entry from durable `saved_anime` table!
        val watchlistWhileOffline = repository.watchlist.first()
        assertTrue(watchlistWhileOffline.any { it.archiveIdentifier == "astro-boy-1963-ep01" && it.displayTitle == "Astro Boy (1963)" })

        // Reconciling restores the row into `anime` table as well
        repository.reconcileSavedAnime()
        val restoredInAnimeTable = db.animeDao().getAnimeByArchiveIdentifierSync("astro-boy-1963-ep01")
        assertNotNull(restoredInAnimeTable)
        assertEquals("Astro Boy (1963)", restoredInAnimeTable?.titleEnglish)

        // Updating renamed title via stable archiveIdentifier updates metadata without changing identity or removing entry
        val renamedEntity = repository.saveArchiveMediaItem(
            archiveIdentifier = "astro-boy-1963-ep01",
            title = "Astro Boy: Mighty Atom (1963 Remastered)",
            description = "",
            thumbnailUrl = "",
            year = 1963,
            inWatchlist = true,
            isWishlist = false
        )
        assertEquals(savedEntity.id, renamedEntity.id)
        assertEquals("Astro Boy: Mighty Atom (1963 Remastered)", renamedEntity.titleEnglish)
        // Existing non-blank thumbnail and description must be preserved when remote refresh has blank artwork/synopsis
        assertEquals("https://archive.org/services/img/astro-boy-1963-ep01", renamedEntity.coverImage)
        assertEquals("Classic Tezuka anime on Internet Archive", renamedEntity.synopsis)
    }

    @Test
    fun `deduplication merges all saved flags, notes, scores, and archiveIdentifier without data loss`() = runBlocking {
        // Insert two rows representing the same show (one from local/seed, one with wishlist/favorite/notes)
        val primaryRow = AnimeEntity(
            id = 500,
            idMal = 500,
            titleRomaji = "Kimetsu no Yaiba",
            titleEnglish = "Demon Slayer",
            coverImage = "https://example.com/ds.jpg",
            inWatchlist = true,
            watchedEpisodeCount = 3,
            airingDayOfWeek = 7
        )
        val duplicateRow = AnimeEntity(
            id = 99500,
            idMal = 500,
            archiveIdentifier = "demon-slayer-ia-archive",
            titleRomaji = "Kimetsu no Yaiba",
            titleEnglish = "Demon Slayer",
            coverImage = "https://example.com/ds2.jpg",
            inWatchlist = false,
            isWishlist = true,
            isFavorite = true,
            isPinned = true,
            watchedEpisodeCount = 10,
            userScore = 95,
            userNotes = "Must watch finale",
            airingDayOfWeek = 7
        )

        db.animeDao().insertAnime(primaryRow)
        db.animeDao().insertAnime(duplicateRow)

        repository.ensureDatabaseDeduplicated()

        val merged = db.animeDao().getAnimeByIdSync(500)
        assertNotNull(merged)
        assertTrue(merged!!.inWatchlist)
        assertTrue(merged.isWishlist)
        assertTrue(merged.isFavorite)
        assertTrue(merged.isPinned)
        assertEquals(10, merged.watchedEpisodeCount)
        assertEquals(95, merged.userScore)
        assertEquals("Must watch finale", merged.userNotes)
        assertEquals("demon-slayer-ia-archive", merged.archiveIdentifier)

        val savedRecord = db.savedAnimeDao().getSavedAnimeByIdSync(500)
        assertNotNull(savedRecord)
        assertTrue(savedRecord!!.isFavorite)
        assertEquals(10, savedRecord.watchedEpisodeCount)
    }

    @Test
    fun `backup export and import preserves all saved anime, history, and preferences non-destructively`() = runBlocking {
        val prefs = context.getSharedPreferences("test_backup_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("accent_color", "EMERALD_GREEN").putString("oauth_secret_token", "DO_NOT_EXPORT"). commit()

        val item1 = AnimeEntity(
            id = 701,
            idMal = 701,
            archiveIdentifier = "ia_item_701",
            titleRomaji = "Ghost in the Shell",
            titleEnglish = "Ghost in the Shell",
            coverImage = "https://example.com/gits.jpg",
            inWatchlist = true,
            isFavorite = true,
            watchedEpisodeCount = 12,
            userScore = 90,
            airingDayOfWeek = 5
        )
        repository.addToWatchlist(item1)
        repository.toggleFavorite(701, true)
        repository.incrementWatchedEpisode(item1, 13)

        val playbackRepo = PlaybackRepository(context, db.playbackPreferenceDao())
        playbackRepo.savePosition("ia_item_701_ep13", 420_000L)
        playbackRepo.savePlaybackSpeed(1.5f)

        val exportedJson = repository.exportUserDataJson(prefs)
        assertTrue(exportedJson.contains("Ghost in the Shell"))
        assertTrue(exportedJson.contains("ia_item_701"))
        // Verify sensitive token is never exported
        assertFalse(exportedJson.contains("DO_NOT_EXPORT"))

        // Now add a second saved item that was created AFTER the backup, and delete item1 to test non-destructive merge
        repository.deleteAnimeCompletely(701, 701)
        val item2 = AnimeEntity(
            id = 702,
            idMal = 702,
            titleRomaji = "Akira",
            titleEnglish = "Akira",
            coverImage = "https://example.com/akira.jpg",
            inWatchlist = true,
            watchedEpisodeCount = 1,
            airingDayOfWeek = 6
        )
        repository.addToWatchlist(item2)

        // Import the backup JSON — both item1 (from backup) and item2 (already saved) must exist!
        val importResult = repository.importUserDataJson(exportedJson, prefs)
        assertTrue(importResult.success)

        val watchlistAfterImport = repository.watchlist.first()
        assertTrue(watchlistAfterImport.any { it.id == 701 && it.watchedEpisodeCount == 13 && it.isFavorite })
        assertTrue(watchlistAfterImport.any { it.id == 702 && it.displayTitle == "Akira" })
    }

    @Test
    fun `database migration v9 to v10 preserves all rows and populates saved_anime table`() {
        val dbName = "migration_test_${System.currentTimeMillis()}.db"
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `anime` (
                            `id` INTEGER NOT NULL,
                            `idMal` INTEGER,
                            `titleRomaji` TEXT NOT NULL,
                            `titleEnglish` TEXT,
                            `titleNative` TEXT,
                            `titlePinyin` TEXT,
                            `coverImage` TEXT NOT NULL,
                            `bannerImage` TEXT,
                            `format` TEXT,
                            `status` TEXT,
                            `totalEpisodes` INTEGER,
                            `currentEpisode` INTEGER NOT NULL,
                            `watchedEpisodeCount` INTEGER NOT NULL DEFAULT 0,
                            `airingDayOfWeek` INTEGER NOT NULL,
                            `airingTimeUnix` INTEGER,
                            `nextEpisodeNumber` INTEGER,
                            `genres` TEXT NOT NULL,
                            `season` TEXT,
                            `seasonYear` INTEGER,
                            `score` INTEGER,
                            `synopsis` TEXT,
                            `countryOfOrigin` TEXT,
                            `inWatchlist` INTEGER NOT NULL,
                            `isWishlist` INTEGER NOT NULL DEFAULT 0,
                            `isFavorite` INTEGER NOT NULL,
                            `isPinned` INTEGER NOT NULL DEFAULT 0,
                            `notificationsEnabled` INTEGER NOT NULL,
                            `isWatched` INTEGER NOT NULL,
                            `userStatus` TEXT,
                            `userScore` INTEGER,
                            `userNotes` TEXT,
                            `startDate` TEXT,
                            `finishDate` TEXT,
                            `totalRewatches` INTEGER NOT NULL DEFAULT 0,
                            `lastCachedAt` INTEGER NOT NULL DEFAULT 0,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `episode_history` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `animeId` INTEGER NOT NULL,
                            `animeTitle` TEXT NOT NULL,
                            `animeCover` TEXT NOT NULL,
                            `episodeNumber` INTEGER NOT NULL,
                            `watchedTimestamp` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `api_cache` (
                            `cacheKey` TEXT NOT NULL,
                            `entityIdsCsv` TEXT NOT NULL,
                            `cachedAt` INTEGER NOT NULL,
                            `ttlMillis` INTEGER NOT NULL,
                            PRIMARY KEY(`cacheKey`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `deleted_anime` (
                            `key` TEXT NOT NULL,
                            `animeId` INTEGER NOT NULL,
                            `idMal` INTEGER,
                            `listType` TEXT NOT NULL,
                            `deletedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`key`)
                        )
                        """.trimIndent()
                    )
                    // Insert a v9 user-saved anime row
                    db.execSQL(
                        """
                        INSERT INTO `anime` (`id`, `idMal`, `titleRomaji`, `titleEnglish`, `coverImage`, `currentEpisode`, `watchedEpisodeCount`, `airingDayOfWeek`, `genres`, `inWatchlist`, `isWishlist`, `isFavorite`, `isPinned`, `notificationsEnabled`, `isWatched`)
                        VALUES (888, 888, 'Steins;Gate', 'Steins;Gate', 'https://example.com/sg.jpg', 24, 12, 3, 'Sci-Fi', 1, 0, 1, 1, 1, 0)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            })
            .build()

        val helper = factory.create(config)
        val sqliteDb = helper.writableDatabase
        // Run MIGRATION_9_10 directly on the v9 database
        AppDatabase.MIGRATION_9_10.migrate(sqliteDb)

        // Verify original `anime` row is preserved with new `archiveIdentifier` column
        val cursor1 = sqliteDb.query("SELECT id, titleEnglish, watchedEpisodeCount, isFavorite FROM anime WHERE id = 888")
        assertTrue(cursor1.moveToFirst())
        assertEquals(888, cursor1.getInt(0))
        assertEquals("Steins;Gate", cursor1.getString(1))
        assertEquals(12, cursor1.getInt(2))
        assertEquals(1, cursor1.getInt(3))
        cursor1.close()

        // Verify `saved_anime` table was automatically populated during migration
        val cursor2 = sqliteDb.query("SELECT id, titleEnglish, watchedEpisodeCount, isFavorite, isPinned FROM saved_anime WHERE id = 888")
        assertTrue(cursor2.moveToFirst())
        assertEquals(888, cursor2.getInt(0))
        assertEquals("Steins;Gate", cursor2.getString(1))
        assertEquals(12, cursor2.getInt(2))
        assertEquals(1, cursor2.getInt(3))
        assertEquals(1, cursor2.getInt(4))
        cursor2.close()

        sqliteDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `ApkUpdateValidator safely rejects missing, empty, or corrupted APK files`() {
        val missingFile = File(context.cacheDir, "non_existent_update.apk")
        val missingResult = ApkUpdateValidator.validateUpdateApk(context, missingFile)
        assertFalse(missingResult.isValid)
        assertEquals("INVALID_FILE", missingResult.errorCode)

        val corruptFile = File(context.cacheDir, "corrupt_update.apk")
        corruptFile.writeText("not a valid zip or apk file")
        val corruptResult = ApkUpdateValidator.validateUpdateApk(context, corruptFile)
        assertFalse(corruptResult.isValid)
        assertEquals("CORRUPT_APK", corruptResult.errorCode)
        corruptFile.delete()
    }
}
