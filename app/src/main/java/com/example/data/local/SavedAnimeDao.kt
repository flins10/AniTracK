package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedAnimeDao {
    @Query("SELECT * FROM saved_anime ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllSavedAnimeFlow(): Flow<List<SavedAnimeEntity>>

    @Query("SELECT * FROM saved_anime")
    suspend fun getAllSavedAnimeSync(): List<SavedAnimeEntity>

    @Query("SELECT * FROM saved_anime WHERE inWatchlist = 1 ORDER BY isPinned DESC, airingDayOfWeek ASC, airingTimeUnix ASC")
    fun getWatchlistFlow(): Flow<List<SavedAnimeEntity>>

    @Query("SELECT * FROM saved_anime WHERE isWishlist = 1 ORDER BY COALESCE(titleEnglish, titleRomaji) ASC")
    fun getWishlistFlow(): Flow<List<SavedAnimeEntity>>

    @Query("SELECT * FROM saved_anime WHERE isFavorite = 1 ORDER BY COALESCE(titleEnglish, titleRomaji) ASC")
    fun getFavoritesFlow(): Flow<List<SavedAnimeEntity>>

    @Query("SELECT * FROM saved_anime WHERE id = :id LIMIT 1")
    suspend fun getByIdSync(id: Int): SavedAnimeEntity?

    @Query("SELECT * FROM saved_anime WHERE id = :id LIMIT 1")
    suspend fun getSavedAnimeByIdSync(id: Int): SavedAnimeEntity?

    @Query("SELECT * FROM saved_anime WHERE idMal = :idMal AND :idMal > 0 LIMIT 1")
    suspend fun getByMalIdSync(idMal: Int): SavedAnimeEntity?

    @Query("SELECT * FROM saved_anime WHERE archiveIdentifier = :archiveIdentifier AND :archiveIdentifier != '' LIMIT 1")
    suspend fun getByArchiveIdentifierSync(archiveIdentifier: String): SavedAnimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedAnime(entity: SavedAnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedAnimeList(entities: List<SavedAnimeEntity>)

    @Update
    suspend fun updateSavedAnime(entity: SavedAnimeEntity)

    @Query("DELETE FROM saved_anime WHERE id = :id")
    suspend fun deleteSavedAnimeById(id: Int)
}
