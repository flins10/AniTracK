package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    @Query("SELECT * FROM anime WHERE inWatchlist = 1 ORDER BY isPinned DESC, airingDayOfWeek ASC, airingTimeUnix ASC")
    fun getAllWatchlistAnime(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE inWatchlist = 1 AND airingDayOfWeek = :dayOfWeek ORDER BY isPinned DESC, airingTimeUnix ASC")
    fun getPlannerAnimeByDay(dayOfWeek: Int): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE (UPPER(status) = 'RELEASING' OR UPPER(status) = 'CURRENTLY_AIRING' OR UPPER(status) = 'AIRING') AND airingDayOfWeek = :dayOfWeek ORDER BY isPinned DESC, airingTimeUnix ASC")
    fun getCalendarAnimeByDay(dayOfWeek: Int): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE (UPPER(status) = 'RELEASING' OR UPPER(status) = 'CURRENTLY_AIRING' OR UPPER(status) = 'AIRING') AND airingDayOfWeek = :dayOfWeek ORDER BY isPinned DESC, airingTimeUnix ASC")
    fun getAnimeByDay(dayOfWeek: Int): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE isFavorite = 1 ORDER BY COALESCE(titleEnglish, titleRomaji) ASC")
    fun getFavorites(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE isWishlist = 1 ORDER BY COALESCE(titleEnglish, titleRomaji) ASC")
    fun getWishlistAnime(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE id = :id")
    fun getAnimeById(id: Int): Flow<AnimeEntity?>

    @Query("SELECT * FROM anime WHERE id = :id")
    suspend fun getAnimeByIdSync(id: Int): AnimeEntity?

    @Query("SELECT * FROM anime WHERE inWatchlist = 1 AND (titleRomaji LIKE '%' || :query || '%' OR titleEnglish LIKE '%' || :query || '%' OR titleNative LIKE '%' || :query || '%')")
    fun searchWatchlist(query: String): Flow<List<AnimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnime(anime: AnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeList(animeList: List<AnimeEntity>)

    @Update
    suspend fun updateAnime(anime: AnimeEntity)

    @Query("DELETE FROM anime WHERE id = :id")
    suspend fun deleteAnime(id: Int)

    @Query("UPDATE anime SET inWatchlist = :inWatchlist WHERE id = :id")
    suspend fun updateWatchlistStatus(id: Int, inWatchlist: Boolean)

    @Query("UPDATE anime SET isWishlist = :isWishlist WHERE id = :id")
    suspend fun updateWishlistStatus(id: Int, isWishlist: Boolean)

    @Query("UPDATE anime SET watchedEpisodeCount = :count, isWatched = :isWatched, lastWatchedTime = :lastWatchedTime WHERE id = :id")
    suspend fun updateWatchedEpisodeCount(id: Int, count: Int, isWatched: Boolean, lastWatchedTime: Long?)

    @Query("SELECT * FROM anime WHERE inWatchlist = 1 OR userStatus IS NOT NULL ORDER BY isPinned DESC, lastWatchedTime DESC, id ASC")
    fun getAllTrackedAnime(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE (inWatchlist = 1 OR userStatus IS NOT NULL) AND (LOWER(userStatus) = LOWER(:status) OR (:status = 'watching' AND userStatus IS NULL AND inWatchlist = 1)) ORDER BY isPinned DESC, lastWatchedTime DESC, id ASC")
    fun getAnimeByUserStatus(status: String): Flow<List<AnimeEntity>>

    @Query("UPDATE anime SET userStatus = :userStatus, userScore = :userScore, userNotes = :userNotes, watchedEpisodeCount = :watchedCount, startDate = :startDate, finishDate = :finishDate, isWatched = :isWatched, lastWatchedTime = :lastWatchedTime WHERE id = :id")
    suspend fun updateUserTrackingState(
        id: Int,
        userStatus: String?,
        userScore: Int?,
        userNotes: String?,
        watchedCount: Int,
        startDate: String?,
        finishDate: String?,
        isWatched: Boolean,
        lastWatchedTime: Long?
    )

    @Query("UPDATE anime SET userNotes = :notes WHERE id = :id")
    suspend fun updateUserNotes(id: Int, notes: String?)

    @Query("UPDATE anime SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("UPDATE anime SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinnedStatus(id: Int, isPinned: Boolean)

    @Query("UPDATE anime SET notificationsEnabled = :enabled WHERE id = :id")
    suspend fun updateNotificationStatus(id: Int, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM anime")
    suspend fun getCount(): Int

    @Query("SELECT * FROM anime WHERE status = 'NOT_YET_RELEASED' OR (airingTimeUnix IS NOT NULL AND airingTimeUnix > :nowSeconds AND status != 'RELEASING') ORDER BY airingTimeUnix ASC, seasonYear ASC")
    fun getUpcomingAnime(nowSeconds: Long = System.currentTimeMillis() / 1000): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE UPPER(status) = 'RELEASING' OR UPPER(status) = 'CURRENTLY_AIRING' OR UPPER(status) = 'AIRING' ORDER BY airingDayOfWeek ASC, airingTimeUnix ASC")
    fun getAllOngoingAnime(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime WHERE (UPPER(status) = 'RELEASING' OR UPPER(status) = 'CURRENTLY_AIRING' OR UPPER(status) = 'AIRING') AND (countryOfOrigin = 'CN' OR genres LIKE '%Donghua%' OR titlePinyin IS NOT NULL) ORDER BY airingDayOfWeek ASC, airingTimeUnix ASC")
    fun getOngoingDonghua(): Flow<List<AnimeEntity>>

    @Query("SELECT * FROM anime")
    suspend fun getAllAnimeSync(): List<AnimeEntity>

    @Query("SELECT * FROM anime WHERE (titleRomaji LIKE '%' || :query || '%' OR titleEnglish LIKE '%' || :query || '%' OR genres LIKE '%' || :query || '%')")
    suspend fun searchAllAnimeSync(query: String): List<AnimeEntity>

    @Query("SELECT * FROM anime WHERE id IN (:ids)")
    suspend fun getAnimeByIdsSync(ids: List<Int>): List<AnimeEntity>

    @Query("DELETE FROM anime WHERE id IN (:ids)")
    suspend fun deleteAnimeList(ids: List<Int>)

    @Query("""
        DELETE FROM anime
        WHERE inWatchlist = 0
          AND isWishlist = 0
          AND isFavorite = 0
          AND isPinned = 0
          AND watchedEpisodeCount <= 0
          AND (userStatus IS NULL OR userStatus = '')
          AND (userNotes IS NULL OR userNotes = '')
    """)
    suspend fun clearUnusedCatalogCache(): Int

    @Query("""
        DELETE FROM anime
        WHERE inWatchlist = 0
          AND isWishlist = 0
          AND isFavorite = 0
          AND isPinned = 0
          AND watchedEpisodeCount <= 0
          AND (userStatus IS NULL OR userStatus = '')
          AND (userNotes IS NULL OR userNotes = '')
          AND (
              lastCachedAt < :staleBeforeEpochMs
              OR id NOT IN (
                  SELECT id FROM anime
                  WHERE inWatchlist = 0
                    AND isWishlist = 0
                    AND isFavorite = 0
                    AND isPinned = 0
                    AND watchedEpisodeCount <= 0
                    AND (userStatus IS NULL OR userStatus = '')
                    AND (userNotes IS NULL OR userNotes = '')
                  ORDER BY lastCachedAt DESC
                  LIMIT :maxUntrackedRows
              )
          )
    """)
    suspend fun pruneUntrackedCatalogCache(
        staleBeforeEpochMs: Long,
        maxUntrackedRows: Int = 80
    ): Int

    @Query("DELETE FROM anime")
    suspend fun clearAllAnime()
}
