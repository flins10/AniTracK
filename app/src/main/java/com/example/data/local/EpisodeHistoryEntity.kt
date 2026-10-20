package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episode_history")
data class EpisodeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val animeId: Int,
    val animeTitle: String,
    val episodeNumber: Int,
    val watchedTimestamp: Long = System.currentTimeMillis(),
    val isWatched: Boolean = true,
    val playbackPositionMs: Long = 0L,
    val durationMs: Long = 0L
)
