package com.example.data.repository

import android.util.LruCache
import com.example.data.remote.ArchiveDocItem
import com.example.data.remote.ArchiveMetadataResponse
import com.example.data.remote.InternetArchiveApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Dedicated repository for Internet Archive search and raw item metadata retrieval.
 * Uses public endpoints:
 * - Search: https://archive.org/advancedsearch.php
 * - Metadata: https://archive.org/metadata/{identifier}
 * Includes memory LRU caching, pagination, and retry with backoff.
 */
class ArchiveSearchRepository(
    private val apiService: InternetArchiveApiService = InternetArchiveApiService.create()
) {
    private val searchCache = LruCache<String, List<ArchiveDocItem>>(80)
    private val metadataCache = LruCache<String, ArchiveMetadataResponse>(120)

    private val matchService = VideoMatchService()

    /**
     * Executes an advanced search on Internet Archive for full-length video items,
     * automatically blocking trailers, reviews, explanations, reactions, clips, and small files.
     */
    suspend fun searchArchive(
        query: String,
        page: Int = 1,
        rows: Int = 20,
        maxRetries: Int = 1
    ): Result<List<ArchiveDocItem>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        val cacheKey = "search_${cleanQuery.lowercase()}_p${page}_r$rows"
        val cached = searchCache.get(cacheKey)
        if (cached != null) {
            return@withContext Result.success(cached)
        }

        var attempt = 0
        var lastException: Exception? = null

        while (attempt <= maxRetries) {
            try {
                val response = apiService.searchArchive(
                    query = cleanQuery,
                    rows = rows,
                    page = page
                )

                if (response.isSuccessful) {
                    val rawDocs = response.body()?.response?.docs.orEmpty()
                    val filteredDocs = rawDocs.filter { doc ->
                        val isBlockedKeyword = matchService.containsExcludedKeywords(
                            title = doc.title.orEmpty(),
                            subject = doc.subject.orEmpty(),
                            identifier = doc.identifier,
                            description = doc.description.orEmpty(),
                            creator = doc.creator.orEmpty(),
                            isLiveActionAllowed = true
                        )
                        val itemBytes = doc.item_size?.toLongOrNull()
                        val isTooSmall = itemBytes != null && itemBytes < VideoMatchService.MIN_EPISODE_FILE_SIZE_BYTES
                        !isBlockedKeyword && !isTooSmall
                    }
                    searchCache.put(cacheKey, filteredDocs)
                    return@withContext Result.success(filteredDocs)
                } else if (response.code() in 500..599 && attempt < maxRetries) {
                    attempt++
                    delay(180L * attempt)
                } else {
                    return@withContext Result.failure(
                        Exception("Internet Archive search failed with HTTP ${response.code()}: ${response.message()}")
                    )
                }
            } catch (e: Exception) {
                lastException = e
                attempt++
                if (attempt <= maxRetries) {
                    delay(180L * attempt)
                }
            }
        }

        Result.failure(lastException ?: Exception("Unknown network failure during Internet Archive search."))
    }

    /**
     * Retrieves item metadata and files list from https://archive.org/metadata/{identifier}
     */
    suspend fun getItemMetadata(
        identifier: String,
        forceRefresh: Boolean = false
    ): Result<ArchiveMetadataResponse> = withContext(Dispatchers.IO) {
        val cleanId = identifier.trim()
        if (cleanId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Identifier cannot be blank"))
        }

        if (!forceRefresh) {
            val cached = metadataCache.get(cleanId)
            if (cached != null) {
                return@withContext Result.success(cached)
            }
        }

        try {
            val response = apiService.getItemMetadata(cleanId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    metadataCache.put(cleanId, body)
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty metadata response from Internet Archive for item '$cleanId'"))
                }
            } else {
                Result.failure(Exception("Failed to fetch item metadata (HTTP ${response.code()}) for '$cleanId'"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearCache() {
        searchCache.evictAll()
        metadataCache.evictAll()
    }
}
