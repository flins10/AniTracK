package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeletedAnimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstone(tombstone: DeletedAnimeEntity)

    @Query("SELECT * FROM deleted_anime WHERE animeId = :animeId LIMIT 1")
    suspend fun getTombstone(animeId: Int): DeletedAnimeEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM deleted_anime WHERE animeId = :animeId AND (listType = 'ALL' OR listType = :listType))")
    suspend fun isDeleted(animeId: Int, listType: String = "ALL"): Boolean

    @Query("DELETE FROM deleted_anime WHERE animeId = :animeId")
    suspend fun removeTombstone(animeId: Int)

    @Query("SELECT * FROM deleted_anime")
    suspend fun getAllTombstones(): List<DeletedAnimeEntity>

    @Query("DELETE FROM deleted_anime")
    suspend fun clearAllTombstones()
}
