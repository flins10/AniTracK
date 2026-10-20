package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_preferences")
data class PlaybackPreferenceEntity(
    @PrimaryKey val prefKey: String,
    val prefValue: String,
    val updatedAt: Long = System.currentTimeMillis()
)
