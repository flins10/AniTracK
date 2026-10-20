package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private fun addColumnIfMissing(
    db: SupportSQLiteDatabase,
    tableName: String,
    columnName: String,
    columnDefSql: String
) {
    val cursor = db.query("PRAGMA table_info(`$tableName`)")
    var exists = false
    try {
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex >= 0 && cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                exists = true
                break
            }
        }
    } finally {
        cursor.close()
    }
    if (!exists) {
        db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefSql")
    }
}

private fun ensureBaseTablesAndColumnsUpToV9(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `anime` (
            `id` INTEGER NOT NULL,
            `idMal` INTEGER,
            `archiveIdentifier` TEXT DEFAULT NULL,
            `titleRomaji` TEXT NOT NULL,
            `titleEnglish` TEXT,
            `titleNative` TEXT,
            `titlePinyin` TEXT,
            `metadataSource` TEXT NOT NULL DEFAULT 'metadata_provider',
            `coverImage` TEXT NOT NULL,
            `bannerImage` TEXT,
            `format` TEXT,
            `status` TEXT,
            `totalEpisodes` INTEGER,
            `currentEpisode` INTEGER NOT NULL,
            `watchedEpisodeCount` INTEGER NOT NULL DEFAULT 0,
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
            `isWishlist` INTEGER NOT NULL DEFAULT 0,
            `isFavorite` INTEGER NOT NULL,
            `isPinned` INTEGER NOT NULL DEFAULT 0,
            `notificationsEnabled` INTEGER NOT NULL,
            `isWatched` INTEGER NOT NULL,
            `lastWatchedTime` INTEGER,
            `userNotes` TEXT,
            `lastCachedAt` INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(`id`)
        )
        """.trimIndent()
    )
    addColumnIfMissing(db, "anime", "idMal", "INTEGER")
    addColumnIfMissing(db, "anime", "titlePinyin", "TEXT")
    addColumnIfMissing(db, "anime", "metadataSource", "TEXT NOT NULL DEFAULT 'metadata_provider'")
    addColumnIfMissing(db, "anime", "watchedEpisodeCount", "INTEGER NOT NULL DEFAULT 0")
    addColumnIfMissing(db, "anime", "userStatus", "TEXT")
    addColumnIfMissing(db, "anime", "userScore", "INTEGER")
    addColumnIfMissing(db, "anime", "startDate", "TEXT")
    addColumnIfMissing(db, "anime", "finishDate", "TEXT")
    addColumnIfMissing(db, "anime", "countryOfOrigin", "TEXT DEFAULT 'JP'")
    addColumnIfMissing(db, "anime", "isWishlist", "INTEGER NOT NULL DEFAULT 0")
    addColumnIfMissing(db, "anime", "isPinned", "INTEGER NOT NULL DEFAULT 0")
    addColumnIfMissing(db, "anime", "lastWatchedTime", "INTEGER")
    addColumnIfMissing(db, "anime", "userNotes", "TEXT")
    addColumnIfMissing(db, "anime", "lastCachedAt", "INTEGER NOT NULL DEFAULT 0")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `episode_history` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `animeId` INTEGER NOT NULL,
            `animeTitle` TEXT NOT NULL,
            `episodeNumber` INTEGER NOT NULL,
            `watchedTimestamp` INTEGER NOT NULL,
            `isWatched` INTEGER NOT NULL DEFAULT 1
        )
        """.trimIndent()
    )
    addColumnIfMissing(db, "episode_history", "isWatched", "INTEGER NOT NULL DEFAULT 1")

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
            `animeId` INTEGER NOT NULL,
            `idMal` INTEGER,
            `deletedAt` INTEGER NOT NULL,
            PRIMARY KEY(`animeId`)
        )
        """.trimIndent()
    )
}

private class MigrationTo9(startVer: Int) : Migration(startVer, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        ensureBaseTablesAndColumnsUpToV9(db)
    }
}

private class MigrationTo10(startVer: Int) : Migration(startVer, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        ensureBaseTablesAndColumnsUpToV9(db)
        AppDatabase.MIGRATION_9_10.migrate(db)
    }
}

private class Migration9To10 : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        ensureBaseTablesAndColumnsUpToV9(db)
        addColumnIfMissing(db, "anime", "archiveIdentifier", "TEXT DEFAULT NULL")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `saved_anime` (
                `id` INTEGER NOT NULL,
                `idMal` INTEGER,
                `archiveIdentifier` TEXT DEFAULT NULL,
                `titleRomaji` TEXT NOT NULL,
                `titleEnglish` TEXT,
                `titleNative` TEXT,
                `titlePinyin` TEXT,
                `metadataSource` TEXT NOT NULL,
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
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_anime_idMal` ON `saved_anime` (`idMal`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_anime_archiveIdentifier` ON `saved_anime` (`archiveIdentifier`)")

        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT OR REPLACE INTO `saved_anime` (
                `id`, `idMal`, `archiveIdentifier`, `titleRomaji`, `titleEnglish`, `titleNative`, `titlePinyin`,
                `metadataSource`, `coverImage`, `bannerImage`, `format`, `status`, `totalEpisodes`, `currentEpisode`,
                `watchedEpisodeCount`, `userStatus`, `userScore`, `startDate`, `finishDate`, `airingDayOfWeek`,
                `airingTimeUnix`, `nextEpisodeNumber`, `genres`, `season`, `seasonYear`, `score`, `synopsis`,
                `countryOfOrigin`, `inWatchlist`, `isWishlist`, `isFavorite`, `isPinned`, `notificationsEnabled`,
                `isWatched`, `lastWatchedTime`, `userNotes`, `savedAt`, `updatedAt`
            )
            SELECT
                `id`, `idMal`, `archiveIdentifier`, `titleRomaji`, `titleEnglish`, `titleNative`, `titlePinyin`,
                COALESCE(`metadataSource`, 'local'), `coverImage`, `bannerImage`, `format`, `status`, `totalEpisodes`, `currentEpisode`,
                `watchedEpisodeCount`, `userStatus`, `userScore`, `startDate`, `finishDate`, `airingDayOfWeek`,
                `airingTimeUnix`, `nextEpisodeNumber`, `genres`, `season`, `seasonYear`, `score`, `synopsis`,
                `countryOfOrigin`, `inWatchlist`, `isWishlist`, `isFavorite`, `isPinned`, `notificationsEnabled`,
                `isWatched`, `lastWatchedTime`, `userNotes`, $now, $now
            FROM `anime`
            WHERE `inWatchlist` = 1
               OR `isWishlist` = 1
               OR `isFavorite` = 1
               OR `isPinned` = 1
               OR `watchedEpisodeCount` > 0
               OR (`userStatus` IS NOT NULL AND `userStatus` != '')
               OR (`userNotes` IS NOT NULL AND `userNotes` != '')
               OR (`archiveIdentifier` IS NOT NULL AND `archiveIdentifier` != '')
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `playback_preferences` (
                `prefKey` TEXT NOT NULL,
                `prefValue` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`prefKey`)
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [
        AnimeEntity::class,
        EpisodeHistoryEntity::class,
        ApiCacheEntity::class,
        DeletedAnimeEntity::class,
        SavedAnimeEntity::class,
        PlaybackPreferenceEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
    abstract fun episodeHistoryDao(): EpisodeHistoryDao
    abstract fun apiCacheDao(): ApiCacheDao
    abstract fun deletedAnimeDao(): DeletedAnimeDao
    abstract fun savedAnimeDao(): SavedAnimeDao
    abstract fun playbackPreferenceDao(): PlaybackPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_9_10: Migration = Migration9To10()

        val ALL_MIGRATIONS: Array<Migration> = buildList<Migration> {
            for (startVer in 1..8) {
                add(MigrationTo9(startVer))
                add(MigrationTo10(startVer))
            }
            add(MIGRATION_9_10)
        }.toTypedArray()

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anitrack_anime.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
