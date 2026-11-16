package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deleted_anime",
    indices = [Index(value = ["animeId"])]
)
data class DeletedAnimeEntity(
    @PrimaryKey val animeId: Int,
    val title: String? = null,
    val listType: String = "ALL", // "WATCHLIST", "WISHLIST", "ALL"
    val deletedAt: Long = System.currentTimeMillis()
)
