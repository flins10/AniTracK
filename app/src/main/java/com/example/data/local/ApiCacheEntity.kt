package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_cache")
data class ApiCacheEntity(
    @PrimaryKey val cacheKey: String,
    val entityIds: String, // Comma-separated list of media IDs cached in the anime table
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (60 * 60 * 1000L) // Default 1 hour TTL
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    fun parseEntityIds(): List<Int> {
        if (entityIds.isBlank()) return emptyList()
        return entityIds.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }

    companion object {
        fun fromIds(cacheKey: String, ids: List<Int>, ttlMillis: Long = 60 * 60 * 1000L): ApiCacheEntity {
            val now = System.currentTimeMillis()
            return ApiCacheEntity(
                cacheKey = cacheKey,
                entityIds = ids.joinToString(","),
                cachedAt = now,
                expiresAt = now + ttlMillis
            )
        }
    }
}
