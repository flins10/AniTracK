package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeletedAnimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstone(tombstone: DeletedAnimeEntity)

    @Query("SELECT * FROM deleted_anime WHERE animeId = :animeId OR (malId IS NOT NULL AND malId = :malId AND :malId > 0) LIMIT 1")
    suspend fun getTombstone(animeId: Int, malId: Int? = null): DeletedAnimeEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM deleted_anime WHERE (animeId = :animeId OR (malId IS NOT NULL AND malId = :malId AND :malId > 0)) AND (listType = 'ALL' OR listType = :listType))")
    suspend fun isDeleted(animeId: Int, malId: Int? = null, listType: String = "ALL"): Boolean

    @Query("DELETE FROM deleted_anime WHERE animeId = :animeId OR (malId IS NOT NULL AND malId = :malId AND :malId > 0)")
    suspend fun removeTombstone(animeId: Int, malId: Int? = null)

    @Query("SELECT * FROM deleted_anime")
    suspend fun getAllTombstones(): List<DeletedAnimeEntity>

    @Query("DELETE FROM deleted_anime")
    suspend fun clearAllTombstones()
}
