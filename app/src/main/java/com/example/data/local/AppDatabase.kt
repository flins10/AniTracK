package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private class Migration10To11 : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `playback_preferences`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `anime_v11` (
                `id` INTEGER NOT NULL,
                `titleRomaji` TEXT NOT NULL,
                `titleEnglish` TEXT,
                `titleNative` TEXT,
                `titlePinyin` TEXT,
                `metadataSource` TEXT NOT NULL DEFAULT 'anilist',
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
                `studios` TEXT NOT NULL DEFAULT '',
                `season` TEXT,
                `seasonYear` INTEGER,
                `score` INTEGER,
                `popularity` INTEGER,
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
            INSERT OR REPLACE INTO `anime_v11` (
                `id`, `titleRomaji`, `titleEnglish`, `titleNative`, `titlePinyin`,
                `metadataSource`, `coverImage`, `bannerImage`, `format`, `status`,
                `totalEpisodes`, `currentEpisode`, `watchedEpisodeCount`, `userStatus`, `userScore`,
                `startDate`, `finishDate`, `airingDayOfWeek`, `airingTimeUnix`, `nextEpisodeNumber`,
                `genres`, `studios`, `season`, `seasonYear`, `score`, `popularity`, `synopsis`,
                `countryOfOrigin`, `inWatchlist`, `isWishlist`, `isFavorite`, `isPinned`,
                `notificationsEnabled`, `isWatched`, `lastWatchedTime`, `userNotes`, `lastCachedAt`
            )
            SELECT
                `id`, `titleRomaji`, `titleEnglish`, `titleNative`, `titlePinyin`,
                'anilist', `coverImage`, `bannerImage`, `format`, `status`,
                `totalEpisodes`, `currentEpisode`, `watchedEpisodeCount`, `userStatus`, `userScore`,
                `startDate`, `finishDate`, `airingDayOfWeek`, `airingTimeUnix`, `nextEpisodeNumber`,
                `genres`, '', `season`, `seasonYear`, `score`, NULL, `synopsis`,
                `countryOfOrigin`, `inWatchlist`, `isWishlist`, `isFavorite`, `isPinned`,
                `notificationsEnabled`, `isWatched`, `lastWatchedTime`, `userNotes`, `lastCachedAt`
            FROM `anime`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `anime`")
        db.execSQL("ALTER TABLE `anime_v11` RENAME TO `anime`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_anime_v11` (
                `id` INTEGER NOT NULL,
                `titleRomaji` TEXT NOT NULL,
                `titleEnglish` TEXT,
                `titleNative` TEXT,
                `titlePinyin` TEXT,
                `metadataSource` TEXT NOT NULL DEFAULT 'anilist',
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
                `studios` TEXT NOT NULL DEFAULT '',
                `season` TEXT,
                `seasonYear` INTEGER,
                `score` INTEGER,
                `popularity` INTEGER,
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
            INSERT OR REPLACE INTO `saved_anime_v11` (
                `id`, `titleRomaji`, `titleEnglish`, `titleNative`, `titlePinyin`,
                `metadataSource`, `coverImage`, `bannerImage`, `format`, `status`,
                `totalEpisodes`, `currentEpisode`, `watchedEpisodeCount`, `userStatus`, `userScore`,
                `startDate`, `finishDate`, `airingDayOfWeek`, `airingTimeUnix`, `nextEpisodeNumber`,
                `genres`, `studios`, `season`, `seasonYear`, `score`, `popularity`, `synopsis`,
                `countryOfOrigin`, `inWatchlist`, `isWishlist`, `isFavorite`, `isPinned`,
                `notificationsEnabled`, `isWatched`, `lastWatchedTime`, `userNotes`, `savedAt`, `updatedAt`
            )
            SELECT
                `id`, `titleRomaji`, `titleEnglish`, `titleNative`, `titlePinyin`,
                'anilist', `coverImage`, `bannerImage`, `format`, `status`,
                `totalEpisodes`, `currentEpisode`, `watchedEpisodeCount`, `userStatus`, `userScore`,
                `startDate`, `finishDate`, `airingDayOfWeek`, `airingTimeUnix`, `nextEpisodeNumber`,
                `genres`, '', `season`, `seasonYear`, `score`, NULL, `synopsis`,
                `countryOfOrigin`, `inWatchlist`, `isWishlist`, `isFavorite`, `isPinned`,
                `notificationsEnabled`, `isWatched`, `lastWatchedTime`, `userNotes`, `savedAt`, `updatedAt`
            FROM `saved_anime`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `saved_anime`")
        db.execSQL("ALTER TABLE `saved_anime_v11` RENAME TO `saved_anime`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `deleted_anime_v11` (
                `animeId` INTEGER NOT NULL,
                `title` TEXT,
                `listType` TEXT NOT NULL,
                `deletedAt` INTEGER NOT NULL,
                PRIMARY KEY(`animeId`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT OR REPLACE INTO `deleted_anime_v11` (`animeId`, `title`, `listType`, `deletedAt`)
            SELECT `animeId`, NULL, `listType`, `deletedAt` FROM `deleted_anime`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `deleted_anime`")
        db.execSQL("ALTER TABLE `deleted_anime_v11` RENAME TO `deleted_anime`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_anime_animeId` ON `deleted_anime` (`animeId`)")
    }
}

private class Migration11To12 : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `anime` ADD COLUMN `userSeason` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `saved_anime` ADD COLUMN `userSeason` INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(
    entities = [
        AnimeEntity::class,
        EpisodeHistoryEntity::class,
        ApiCacheEntity::class,
        DeletedAnimeEntity::class,
        SavedAnimeEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
    abstract fun episodeHistoryDao(): EpisodeHistoryDao
    abstract fun apiCacheDao(): ApiCacheDao
    abstract fun deletedAnimeDao(): DeletedAnimeDao
    abstract fun savedAnimeDao(): SavedAnimeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_10_11: Migration = Migration10To11()
        val MIGRATION_11_12: Migration = Migration11To12()

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_10_11, MIGRATION_11_12)

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anitrack_anime.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            runCatching {
                                db.execSQL("PRAGMA auto_vacuum = INCREMENTAL")
                                db.execSQL("PRAGMA cache_size = -1024")
                                db.execSQL("PRAGMA temp_store = MEMORY")
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    fun compactDatabase() {
        runCatching {
            openHelper.writableDatabase.execSQL("PRAGMA incremental_vacuum(32)")
        }
    }
}
