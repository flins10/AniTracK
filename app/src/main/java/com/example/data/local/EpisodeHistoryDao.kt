package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeHistoryDao {
    @Query("SELECT * FROM episode_history WHERE animeId = :animeId AND episodeNumber = :episodeNumber LIMIT 1")
    suspend fun getHistoryForEpisode(animeId: Int, episodeNumber: Int): EpisodeHistoryEntity?

    @Query("SELECT * FROM episode_history ORDER BY watchedTimestamp DESC")
    fun getAllHistory(): Flow<List<EpisodeHistoryEntity>>

    @Query("SELECT * FROM episode_history ORDER BY watchedTimestamp DESC")
    suspend fun getAllHistorySync(): List<EpisodeHistoryEntity>

    @Query("SELECT * FROM episode_history WHERE animeId = :animeId ORDER BY episodeNumber DESC")
    fun getHistoryForAnime(animeId: Int): Flow<List<EpisodeHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: EpisodeHistoryEntity)

    @Query("DELETE FROM episode_history WHERE id = :historyId")
    suspend fun deleteHistory(historyId: Long)

    @Query("DELETE FROM episode_history WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun deleteHistoryForEpisode(animeId: Int, episodeNumber: Int)

    @Query("DELETE FROM episode_history")
    suspend fun clearAllHistory()
}
