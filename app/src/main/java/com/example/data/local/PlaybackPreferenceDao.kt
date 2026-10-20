package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaybackPreferenceDao {
    @Query("SELECT * FROM playback_preferences WHERE prefKey = :key LIMIT 1")
    suspend fun getPreference(key: String): PlaybackPreferenceEntity?

    @Query("SELECT * FROM playback_preferences")
    suspend fun getAllPreferences(): List<PlaybackPreferenceEntity>

    @Query("SELECT * FROM playback_preferences")
    suspend fun getAllPreferencesSync(): List<PlaybackPreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreference(preference: PlaybackPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(preferences: List<PlaybackPreferenceEntity>)

    @Query("DELETE FROM playback_preferences WHERE prefKey = :key")
    suspend fun deletePreference(key: String)
}
