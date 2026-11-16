package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AnimeEntity
import com.example.data.local.ApiCacheEntity
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import com.example.model.WatchStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
            savedAnimeDao = db.savedAnimeDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `clearing cache never deletes saved anime, favorites, or watch history`() = runBlocking {
        val savedWatchlist = AnimeEntity(
            id = 101,
            titleRomaji = "Cowboy Bebop",
            titleEnglish = "Cowboy Bebop",
            coverImage = "https://example.com/bebop.jpg",
            totalEpisodes = 26,
            inWatchlist = true,
            watchedEpisodeCount = 5,
            airingDayOfWeek = 1
        )
        val savedFavorite = AnimeEntity(
            id = 103,
            titleRomaji = "Samurai Champloo",
            titleEnglish = "Samurai Champloo",
            coverImage = "https://example.com/champloo.jpg",
            isFavorite = true,
            inWatchlist = false,
            airingDayOfWeek = 3
        )
        val transientBrowsed = AnimeEntity(
            id = 999,
            titleRomaji = "Transient Browsed Show",
            titleEnglish = "Transient Browsed Show",
            coverImage = "https://example.com/transient.jpg",
            inWatchlist = false,
            isWishlist = false,
            isFavorite = false,
            watchedEpisodeCount = 0,
            airingDayOfWeek = 4
        )

        repository.addToWatchlist(savedWatchlist, WatchStatus.WATCHING)
        db.animeDao().insertAnime(savedFavorite)
        repository.toggleFavorite(103, true)
        db.animeDao().insertAnime(transientBrowsed)
        repository.incrementEpisode(101)

        db.apiCacheDao().insertCache(ApiCacheEntity.fromIds("search:test", listOf(101, 999)))
        repository.clearMetadataAndApiCache()

        assertNull(db.apiCacheDao().getCache("search:test"))
        assertNull(db.animeDao().getAnimeByIdSync(999))

        val watchlistNow = repository.watchlist.first()
        val favoritesNow = repository.favorites.first()
        val historyNow = repository.watchHistory.first()

        assertTrue(watchlistNow.any { it.id == 101 && it.watchedEpisodeCount == 6 })
        assertTrue(favoritesNow.any { it.id == 103 && it.isFavorite })
        assertTrue(historyNow.any { it.animeId == 101 && it.episodeNumber == 6 })
    }

    @Test
    fun `saved anime remain visible even when main anime table cache row is dropped`() = runBlocking {
        val astro = AnimeEntity(
            id = 2747,
            titleRomaji = "Tetsuwan Atom",
            titleEnglish = "Astro Boy",
            coverImage = "https://example.com/astro.jpg",
            totalEpisodes = 193,
            airingDayOfWeek = 2
        )
        repository.addToWatchlist(astro, WatchStatus.WATCHING)
        db.animeDao().deleteAnimeList(listOf(2747))

        val watchlistWhileMissingMainRow = repository.watchlist.first()
        assertTrue(watchlistWhileMissingMainRow.any { it.id == 2747 && it.displayTitle == "Astro Boy" })

        repository.reconcileSavedAnime()
        val restored = db.animeDao().getAnimeByIdSync(2747)
        assertNotNull(restored)
        assertEquals("Astro Boy", restored?.titleEnglish)
    }

    @Test
    fun `backup export and import preserves saved anime non-destructively`() = runBlocking {
        val item1 = AnimeEntity(
            id = 701,
            titleRomaji = "Ghost in the Shell",
            titleEnglish = "Ghost in the Shell",
            coverImage = "https://example.com/gits.jpg",
            totalEpisodes = 26,
            inWatchlist = true,
            isFavorite = true,
            watchedEpisodeCount = 12,
            userScore = 90,
            userNotes = "Essential cyberpunk classic",
            airingDayOfWeek = 5
        )
        repository.addToWatchlist(item1, WatchStatus.WATCHING)
        repository.toggleFavorite(701, true)
        repository.incrementEpisode(701)

        val exportedJson = repository.exportBackupJson()
        assertTrue(exportedJson.contains("Ghost in the Shell"))
        assertTrue(exportedJson.contains("Essential cyberpunk classic"))

        repository.removeFromWatchlist(701)
        val item2 = AnimeEntity(
            id = 702,
            titleRomaji = "Akira",
            titleEnglish = "Akira",
            coverImage = "https://example.com/akira.jpg",
            totalEpisodes = 1,
            inWatchlist = true,
            watchedEpisodeCount = 1,
            airingDayOfWeek = 6
        )
        repository.addToWatchlist(item2, WatchStatus.COMPLETED)

        val importResult = repository.importBackupJson(exportedJson)
        assertTrue(importResult.success)

        val watchlistAfterImport = repository.watchlist.first()
        assertTrue(watchlistAfterImport.any { it.id == 701 && it.watchedEpisodeCount == 13 && it.isFavorite })
        assertTrue(watchlistAfterImport.any { it.id == 702 && it.displayTitle == "Akira" })
    }

    @Test
    fun `database migration v10 to v11 drops playback_preferences and preserves saved anime without idMal`() {
        val dbName = "migration_v11_test_${System.currentTimeMillis()}.db"
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `anime` (
                            `id` INTEGER NOT NULL,
                            `idMal` INTEGER,
                            `archiveIdentifier` TEXT,
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
                            `watchedEpisodeCount` INTEGER NOT NULL,
                            `userStatus` TEXT,
                            `userScore` INTEGER,
                            `startDate` TEXT,
                            `finishDate` TEXT,
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
                            `isWishlist` INTEGER NOT NULL,
                            `isFavorite` INTEGER NOT NULL,
                            `isPinned` INTEGER NOT NULL DEFAULT 0,
                            `notificationsEnabled` INTEGER NOT NULL,
                            `isWatched` INTEGER NOT NULL,
                            `lastWatchedTime` INTEGER,
                            `userNotes` TEXT,
                            `lastCachedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `saved_anime` (
                            `id` INTEGER NOT NULL,
                            `idMal` INTEGER,
                            `archiveIdentifier` TEXT,
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
                            `watchedEpisodeCount` INTEGER NOT NULL,
                            `userStatus` TEXT,
                            `userScore` INTEGER,
                            `startDate` TEXT,
                            `finishDate` TEXT,
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
                            `isWishlist` INTEGER NOT NULL,
                            `isFavorite` INTEGER NOT NULL,
                            `isPinned` INTEGER NOT NULL DEFAULT 0,
                            `notificationsEnabled` INTEGER NOT NULL,
                            `isWatched` INTEGER NOT NULL,
                            `lastWatchedTime` INTEGER,
                            `userNotes` TEXT,
                            `savedAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
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
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `playback_preferences` (
                            `mediaKey` TEXT NOT NULL,
                            `positionMs` INTEGER NOT NULL,
                            PRIMARY KEY(`mediaKey`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `anime` (`id`, `idMal`, `archiveIdentifier`, `titleRomaji`, `titleEnglish`, `coverImage`, `currentEpisode`, `watchedEpisodeCount`, `airingDayOfWeek`, `genres`, `inWatchlist`, `isWishlist`, `isFavorite`, `isPinned`, `notificationsEnabled`, `isWatched`, `lastCachedAt`)
                        VALUES (888, 9253, 'ia_sg', 'Steins;Gate', 'Steins;Gate', 'https://example.com/sg.jpg', 24, 12, 3, 'Sci-Fi', 1, 0, 1, 1, 1, 0, 1000)
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `saved_anime` (`id`, `idMal`, `archiveIdentifier`, `titleRomaji`, `titleEnglish`, `coverImage`, `currentEpisode`, `watchedEpisodeCount`, `airingDayOfWeek`, `genres`, `inWatchlist`, `isWishlist`, `isFavorite`, `isPinned`, `notificationsEnabled`, `isWatched`, `savedAt`, `updatedAt`)
                        VALUES (888, 9253, 'ia_sg', 'Steins;Gate', 'Steins;Gate', 'https://example.com/sg.jpg', 24, 12, 3, 'Sci-Fi', 1, 0, 1, 1, 1, 0, 1000, 1000)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val sqliteDb = factory.create(config).writableDatabase
        AppDatabase.MIGRATION_10_11.migrate(sqliteDb)

        val playbackTable = sqliteDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='playback_preferences'")
        assertEquals(0, playbackTable.count)
        playbackTable.close()

        val c1 = sqliteDb.query("SELECT id, titleEnglish, watchedEpisodeCount, metadataSource, studios FROM anime WHERE id = 888")
        assertTrue(c1.moveToFirst())
        assertEquals(888, c1.getInt(0))
        assertEquals("Steins;Gate", c1.getString(1))
        assertEquals(12, c1.getInt(2))
        assertEquals("anilist", c1.getString(3))
        c1.close()

        val c2 = sqliteDb.query("SELECT id, titleEnglish, watchedEpisodeCount, isFavorite FROM saved_anime WHERE id = 888")
        assertTrue(c2.moveToFirst())
        assertEquals(888, c2.getInt(0))
        assertEquals("Steins;Gate", c2.getString(1))
        assertEquals(12, c2.getInt(2))
        assertEquals(1, c2.getInt(3))
        c2.close()

        sqliteDb.close()
        context.deleteDatabase(dbName)
    }
}
