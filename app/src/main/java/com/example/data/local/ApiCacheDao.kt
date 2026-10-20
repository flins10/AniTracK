package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ApiCacheDao {
    @Query("SELECT * FROM api_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCache(key: String): ApiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: ApiCacheEntity)

    @Query("DELETE FROM api_cache WHERE cacheKey = :key")
    suspend fun deleteCache(key: String)

    @Query("DELETE FROM api_cache WHERE expiresAt < :now")
    suspend fun deleteExpiredCache(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM api_cache")
    suspend fun clearAllCache()
}
