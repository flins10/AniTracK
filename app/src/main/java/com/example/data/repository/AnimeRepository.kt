package com.example.data.repository

import com.example.data.local.AnimeDao
import com.example.data.local.AnimeEntity
import com.example.data.local.ApiCacheDao
import com.example.data.local.ApiCacheEntity
import com.example.data.local.DeletedAnimeDao
import com.example.data.local.DeletedAnimeEntity
import com.example.data.local.EpisodeHistoryDao
import com.example.data.local.EpisodeHistoryEntity
import com.example.data.local.SavedAnimeDao
import com.example.data.local.SavedAnimeEntity
import com.example.data.local.toAnimeEntity
import com.example.data.local.toSavedAnimeEntity
import com.example.model.ProgressRules
import com.example.model.SeasonRules
import com.example.model.WatchStatus
import com.example.service.AniListMetadataProvider
import com.example.util.BackupRestoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

data class ImportDataResult(
    val success: Boolean,
    val message: String,
    val importedCount: Int = 0
)

class AnimeRepository(
    private val animeDao: AnimeDao,
    private val historyDao: EpisodeHistoryDao,
    private val apiCacheDao: ApiCacheDao? = null,
    private val deletedAnimeDao: DeletedAnimeDao? = null,
    private val savedAnimeDao: SavedAnimeDao? = null,
    private val metadataProvider: AniListMetadataProvider = AniListMetadataProvider(),
    private val onCompactDatabase: (() -> Unit)? = null
) {
    val watchlist: Flow<List<AnimeEntity>> = if (savedAnimeDao != null) {
        combine(animeDao.getAllWatchlistAnime(), savedAnimeDao.getWatchlistFlow()) { mainList, savedList ->
            mergeAnimeWithSaved(mainList, savedList) { it.inWatchlist }
                .sortedWith(
                    compareByDescending<AnimeEntity> { it.isPinned }
                        .thenByDescending { it.lastWatchedTime ?: 0L }
                        .thenBy { it.airingDayOfWeek }
                )
        }
    } else {
        animeDao.getAllWatchlistAnime()
    }

    val wishlist: Flow<List<AnimeEntity>> = if (savedAnimeDao != null) {
        combine(animeDao.getWishlistAnime(), savedAnimeDao.getWishlistFlow()) { mainList, savedList ->
            mergeAnimeWithSaved(mainList, savedList) { it.isWishlist }
                .sortedBy { it.displayTitle.lowercase() }
        }
    } else {
        animeDao.getWishlistAnime()
    }

    val favorites: Flow<List<AnimeEntity>> = if (savedAnimeDao != null) {
        combine(animeDao.getFavorites(), savedAnimeDao.getFavoritesFlow()) { mainList, savedList ->
            mergeAnimeWithSaved(mainList, savedList) { it.isFavorite }
                .sortedBy { it.displayTitle.lowercase() }
        }
    } else {
        animeDao.getFavorites()
    }

    val watchHistory: Flow<List<EpisodeHistoryEntity>> = historyDao.getAllHistory()
    val upcomingAnime: Flow<List<AnimeEntity>> = animeDao.getUpcomingAnime()
    val allOngoingAnime: Flow<List<AnimeEntity>> = animeDao.getAllOngoingAnime()

    fun getPlannerAnimeByDay(dayOfWeek: Int): Flow<List<AnimeEntity>> {
        return if (savedAnimeDao != null) {
            combine(animeDao.getPlannerAnimeByDay(dayOfWeek), savedAnimeDao.getWatchlistFlow()) { mainList, savedList ->
                val daySaved = savedList.filter { it.airingDayOfWeek == dayOfWeek }
                mergeAnimeWithSaved(mainList, daySaved) { it.inWatchlist && it.airingDayOfWeek == dayOfWeek }
                    .sortedWith(compareByDescending<AnimeEntity> { it.isPinned }.thenBy { it.airingTimeUnix ?: Long.MAX_VALUE })
            }
        } else {
            animeDao.getPlannerAnimeByDay(dayOfWeek)
        }
    }

    fun getAnimeById(id: Int): Flow<AnimeEntity?> {
        return if (savedAnimeDao != null) {
            combine(animeDao.getAnimeById(id), savedAnimeDao.getWatchlistFlow()) { mainEntity, _ ->
                val saved = savedAnimeDao.getByIdSync(id)
                if (mainEntity != null && saved != null) {
                    mergeAnimeWithSaved(listOf(mainEntity), listOf(saved)) { true }.firstOrNull()
                } else {
                    mainEntity ?: saved?.toAnimeEntity()
                }
            }
        } else {
            animeDao.getAnimeById(id)
        }
    }

    suspend fun getAnimeByIdSync(id: Int): AnimeEntity? = withContext(Dispatchers.IO) {
        val mainEntity = animeDao.getAnimeByIdSync(id)
        val savedEntity = savedAnimeDao?.getByIdSync(id)
        if (mainEntity != null && savedEntity != null) {
            mergeAnimeWithSaved(listOf(mainEntity), listOf(savedEntity)) { true }.firstOrNull()
        } else {
            mainEntity ?: savedEntity?.toAnimeEntity()
        }
    }

    suspend fun upsertCatalogItems(items: List<AnimeEntity>, keepFullSynopsis: Boolean = false) = withContext(Dispatchers.IO) {
        for (item in items) {
            val existing = getAnimeByIdSync(item.id)
            val isTracked = existing?.hasActiveUserState == true || item.hasActiveUserState
            val compactedGenres = item.genres.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(5)
                .joinToString(", ")
            val compactedStudios = item.studios.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(3)
                .joinToString(", ")
            val compactedSynopsis = when {
                keepFullSynopsis || isTracked -> item.synopsis?.take(600) ?: existing?.synopsis?.take(600)
                else -> item.synopsis?.take(160)
            }
            val merged = if (existing != null) {
                item.copy(
                    genres = compactedGenres.ifBlank { existing.genres },
                    studios = compactedStudios.ifBlank { existing.studios },
                    synopsis = compactedSynopsis,
                    inWatchlist = existing.inWatchlist,
                    isWishlist = existing.isWishlist,
                    isFavorite = existing.isFavorite,
                    isPinned = existing.isPinned,
                    watchedEpisodeCount = ProgressRules.clampEpisodeProgress(existing.watchedEpisodeCount, item.totalEpisodes ?: existing.totalEpisodes),
                    userSeason = existing.userSeason,
                    userStatus = existing.userStatus,
                    userScore = existing.userScore,
                    userNotes = existing.userNotes,
                    isWatched = existing.isWatched,
                    notificationsEnabled = existing.notificationsEnabled,
                    lastWatchedTime = existing.lastWatchedTime
                )
            } else {
                item.copy(
                    genres = compactedGenres,
                    studios = compactedStudios,
                    synopsis = compactedSynopsis
                )
            }
            animeDao.insertAnime(merged)
            syncEntityToSavedTable(merged)
        }
        pruneAndCompactStorage()
    }

    suspend fun addToWatchlist(anime: AnimeEntity, initialStatus: WatchStatus = WatchStatus.WATCHING): AnimeEntity = withContext(Dispatchers.IO) {
        deletedAnimeDao?.removeTombstone(anime.id)
        val existing = getAnimeByIdSync(anime.id)
        val targetTotal = anime.totalEpisodes ?: existing?.totalEpisodes
        val progress = ProgressRules.clampEpisodeProgress(existing?.watchedEpisodeCount ?: anime.watchedEpisodeCount, targetTotal)
        val resolvedSeason = SeasonRules.resolveInitialSeason(
            existing?.userSeason ?: anime.userSeason,
            (existing ?: anime).displayTitle
        )
        val updated = (existing ?: anime).copy(
            inWatchlist = true,
            isWishlist = initialStatus == WatchStatus.PLAN_TO_WATCH,
            isWatched = initialStatus == WatchStatus.COMPLETED,
            userStatus = initialStatus.dbValue,
            watchedEpisodeCount = progress,
            userSeason = resolvedSeason,
            lastWatchedTime = System.currentTimeMillis()
        )
        animeDao.insertAnime(updated)
        syncEntityToSavedTable(updated)
        updated
    }

    suspend fun removeFromWatchlist(animeId: Int) = withContext(Dispatchers.IO) {
        deletedAnimeDao?.insertTombstone(DeletedAnimeEntity(animeId = animeId, listType = "WATCHLIST"))
        val existing = animeDao.getAnimeByIdSync(animeId)
        if (existing != null) {
            val updated = existing.copy(
                inWatchlist = false,
                isWishlist = false,
                isPinned = false,
                isWatched = false,
                userStatus = null
            )
            animeDao.updateAnime(updated)
            syncEntityToSavedTable(updated)
        } else {
            savedAnimeDao?.deleteSavedAnimeById(animeId)
        }
    }

    suspend fun incrementEpisode(animeId: Int): AnimeEntity? = withContext(Dispatchers.IO) {
        val current = getAnimeByIdSync(animeId) ?: return@withContext null
        val (nextCount, reachedEnd) = ProgressRules.incrementEpisode(current.watchedEpisodeCount, current.totalEpisodes)
        val nextStatus = when {
            reachedEnd -> WatchStatus.COMPLETED
            current.userStatus == WatchStatus.PLAN_TO_WATCH.dbValue || current.userStatus == null -> WatchStatus.WATCHING
            else -> current.watchStatusEnum
        }
        updateUserTrackingState(
            animeId = animeId,
            status = nextStatus,
            episodeProgress = nextCount,
            score = current.userScore,
            notes = current.userNotes
        )
    }

    suspend fun updateUserTrackingState(
        animeId: Int,
        status: WatchStatus,
        episodeProgress: Int? = null,
        seasonNumber: Int? = null,
        score: Int? = null,
        notes: String? = null,
        isFavorite: Boolean? = null
    ): AnimeEntity? = withContext(Dispatchers.IO) {
        val existing = getAnimeByIdSync(animeId) ?: return@withContext null
        val clampedProgress = ProgressRules.clampEpisodeProgress(
            episodeProgress ?: existing.watchedEpisodeCount,
            existing.totalEpisodes
        )
        val clampedSeason = seasonNumber?.let { SeasonRules.clampSeason(it) } ?: existing.effectiveSeasonNumber
        val finalProgress = if (status == WatchStatus.COMPLETED && (existing.totalEpisodes ?: 0) > 0) {
            existing.totalEpisodes!!
        } else {
            clampedProgress
        }
        val updated = existing.copy(
            inWatchlist = true,
            isWishlist = status == WatchStatus.PLAN_TO_WATCH,
            isWatched = status == WatchStatus.COMPLETED,
            isFavorite = isFavorite ?: existing.isFavorite,
            userStatus = status.dbValue,
            watchedEpisodeCount = finalProgress,
            userSeason = clampedSeason,
            userScore = score ?: existing.userScore,
            userNotes = notes ?: existing.userNotes,
            lastWatchedTime = System.currentTimeMillis()
        )
        animeDao.insertAnime(updated)
        syncEntityToSavedTable(updated)
        val latest = historyDao.getLatestHistoryForAnime(updated.id)
        if (latest == null || latest.episodeNumber != finalProgress) {
            historyDao.insertHistory(
                EpisodeHistoryEntity(
                    animeId = updated.id,
                    animeTitle = updated.displayTitle,
                    episodeNumber = finalProgress,
                    watchedTimestamp = System.currentTimeMillis(),
                    isWatched = true
                )
            )
            historyDao.pruneToMaxEntries(150)
        }
        updated
    }

    suspend fun updateSeasonProgress(
        animeId: Int,
        seasonNumber: Int,
        resetEpisodes: Boolean = false
    ): AnimeEntity? = withContext(Dispatchers.IO) {
        val existing = getAnimeByIdSync(animeId) ?: return@withContext null
        val clampedSeason = SeasonRules.clampSeason(seasonNumber)
        val nextEpisode = if (resetEpisodes) 0 else existing.watchedEpisodeCount
        val nextStatus = when {
            resetEpisodes -> WatchStatus.WATCHING
            !existing.inWatchlist -> WatchStatus.WATCHING
            else -> existing.watchStatusEnum
        }
        updateUserTrackingState(
            animeId = animeId,
            status = nextStatus,
            episodeProgress = nextEpisode,
            seasonNumber = clampedSeason,
            score = existing.userScore,
            notes = existing.userNotes,
            isFavorite = existing.isFavorite
        )
    }

    suspend fun toggleFavorite(animeId: Int, isFavorite: Boolean): AnimeEntity? = withContext(Dispatchers.IO) {
        val existing = getAnimeByIdSync(animeId) ?: return@withContext null
        val updated = existing.copy(isFavorite = isFavorite)
        animeDao.insertAnime(updated)
        syncEntityToSavedTable(updated)
        updated
    }

    suspend fun togglePinned(animeId: Int, isPinned: Boolean) = withContext(Dispatchers.IO) {
        val existing = getAnimeByIdSync(animeId) ?: return@withContext
        val updated = existing.copy(isPinned = isPinned)
        animeDao.insertAnime(updated)
        syncEntityToSavedTable(updated)
    }

    suspend fun toggleNotification(animeId: Int, enabled: Boolean) = withContext(Dispatchers.IO) {
        animeDao.updateNotificationStatus(animeId, enabled)
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
    }

    suspend fun isDeleted(animeId: Int, listType: String = "ALL"): Boolean = withContext(Dispatchers.IO) {
        deletedAnimeDao?.isDeleted(animeId, listType) ?: false
    }

    suspend fun getTrackedAnimeSnapshot(): List<AnimeEntity> = withContext(Dispatchers.IO) {
        val mainTracked = animeDao.getAllTrackedAnimeSync()
        val savedList = savedAnimeDao?.getAllSavedAnimeSync().orEmpty()
        mergeAnimeWithSaved(mainTracked, savedList) { it.hasActiveUserState || it.userScore != null }
    }

    suspend fun getDeletedAnimeIdsSnapshot(): List<Int> = withContext(Dispatchers.IO) {
        deletedAnimeDao?.getAllTombstones()?.map { it.animeId }.orEmpty()
    }

    suspend fun clearUserTrackingStateOnly() = withContext(Dispatchers.IO) {
        deletedAnimeDao?.clearAllTombstones()
        historyDao.clearAllHistory()
        savedAnimeDao?.clearAll()
        animeDao.clearAllUserTrackingFields()
    }

    suspend fun restoreOwnerTrackedItems(
        items: List<AnimeEntity>,
        deletedIds: List<Int> = emptyList()
    ) = withContext(Dispatchers.IO) {
        clearUserTrackingStateOnly()
        for (item in items) {
            val catalogExisting = animeDao.getAnimeByIdSync(item.id)
            val merged = if (catalogExisting != null) {
                catalogExisting.copy(
                    inWatchlist = item.inWatchlist,
                    isWishlist = item.isWishlist,
                    isFavorite = item.isFavorite,
                    isPinned = item.isPinned,
                    watchedEpisodeCount = ProgressRules.clampEpisodeProgress(item.watchedEpisodeCount, catalogExisting.totalEpisodes ?: item.totalEpisodes),
                    userSeason = SeasonRules.clampSeason(item.userSeason),
                    userStatus = item.userStatus,
                    userScore = item.userScore,
                    startDate = item.startDate,
                    finishDate = item.finishDate,
                    isWatched = item.isWatched,
                    lastWatchedTime = item.lastWatchedTime,
                    userNotes = item.userNotes
                )
            } else {
                item
            }
            animeDao.insertAnime(merged)
            syncEntityToSavedTable(merged)
        }
        for (delId in deletedIds.distinct()) {
            deletedAnimeDao?.insertTombstone(DeletedAnimeEntity(animeId = delId, listType = "WATCHLIST"))
        }
    }

    suspend fun syncCloudWatchlistEntries(
        cloudItems: List<AnimeEntity>,
        replaceMissingFromCloud: Boolean = false,
        protectedPendingIds: Set<Int> = emptySet()
    ) = withContext(Dispatchers.IO) {
        val cloudIds = cloudItems.map { it.id }.toSet()
        for (item in cloudItems) {
            val existing = getAnimeByIdSync(item.id)
            val merged = if (existing != null) {
                item.copy(
                    isPinned = existing.isPinned,
                    notificationsEnabled = existing.notificationsEnabled,
                    airingDayOfWeek = if (item.airingTimeUnix != null) item.airingDayOfWeek else existing.airingDayOfWeek,
                    airingTimeUnix = item.airingTimeUnix ?: existing.airingTimeUnix,
                    nextEpisodeNumber = item.nextEpisodeNumber ?: existing.nextEpisodeNumber,
                    userSeason = maxOf(item.userSeason, existing.userSeason)
                )
            } else {
                item
            }
            animeDao.insertAnime(merged)
            syncEntityToSavedTable(merged)
        }
        if (replaceMissingFromCloud) {
            val localTracked = getTrackedAnimeSnapshot()
            for (localItem in localTracked) {
                if (localItem.id !in cloudIds && localItem.id !in protectedPendingIds) {
                    val cleared = localItem.copy(
                        inWatchlist = false,
                        isWishlist = false,
                        isFavorite = false,
                        isPinned = false,
                        watchedEpisodeCount = 0,
                        userSeason = 1,
                        userStatus = null,
                        userScore = null,
                        startDate = null,
                        finishDate = null,
                        isWatched = false,
                        lastWatchedTime = null,
                        userNotes = null
                    )
                    animeDao.insertAnime(cleared)
                    savedAnimeDao?.deleteSavedAnimeById(localItem.id)
                }
            }
        }
    }

    suspend fun refreshSchedule(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis() / 1000
            val start = (now - 86400).toInt()
            val end = (now + 7 * 86400).toInt()
            val scheduleRes = metadataProvider.getAiringSchedule(start, end, page = 1)
            val releasingRes = metadataProvider.getPopularReleasing(page = 1, perPage = 25)

            val entities = mutableListOf<AnimeEntity>()
            scheduleRes.getOrNull().orEmpty().forEach { sched ->
                val media = sched.media ?: return@forEach
                val existing = getAnimeByIdSync(media.id)
                val mapped = AniListMetadataProvider.mapMediaToEntity(media, existing).let { entity ->
                    val at = sched.airingAt ?: entity.airingTimeUnix
                    val dow = if (at != null && at > 0) {
                        Instant.ofEpochSecond(at).atZone(ZoneId.systemDefault()).dayOfWeek.value
                    } else entity.airingDayOfWeek
                    entity.copy(
                        airingTimeUnix = at,
                        nextEpisodeNumber = sched.episode ?: entity.nextEpisodeNumber,
                        airingDayOfWeek = dow
                    )
                }
                entities.add(mapped)
            }

            releasingRes.getOrNull().orEmpty().forEach { media ->
                if (entities.none { it.id == media.id }) {
                    val existing = getAnimeByIdSync(media.id)
                    entities.add(AniListMetadataProvider.mapMediaToEntity(media, existing))
                }
            }

            if (entities.isNotEmpty()) {
                upsertCatalogItems(entities)
            }

            if (scheduleRes.isFailure && releasingRes.isFailure) {
                Result.failure(scheduleRes.exceptionOrNull() ?: Exception("Unable to refresh schedule right now."))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pruneAndCompactStorage(
        ttlMillis: Long = 24L * 60L * 60L * 1000L,
        maxUntrackedRows: Int = 80
    ): Int = withContext(Dispatchers.IO) {
        apiCacheDao?.deleteExpiredCache()
        apiCacheDao?.pruneToMaxEntries(20)
        historyDao.pruneToMaxEntries(150)
        val removed = animeDao.pruneUntrackedCatalogCache(
            staleBeforeEpochMs = System.currentTimeMillis() - ttlMillis,
            maxUntrackedRows = maxUntrackedRows
        )
        if (removed > 0) {
            onCompactDatabase?.invoke()
        }
        removed
    }

    suspend fun clearMetadataAndApiCache(): Int = withContext(Dispatchers.IO) {
        apiCacheDao?.clearAllCache()
        val cleared = animeDao.clearUnusedCatalogCache()
        onCompactDatabase?.invoke()
        cleared
    }

    suspend fun clearAllUserData() = withContext(Dispatchers.IO) {
        apiCacheDao?.clearAllCache()
        deletedAnimeDao?.clearAllTombstones()
        historyDao.clearAllHistory()
        savedAnimeDao?.clearAll()
        animeDao.clearAllAnime()
        onCompactDatabase?.invoke()
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val savedDao = savedAnimeDao ?: return@withContext "{}"
        BackupRestoreManager.exportBackupJson(animeDao, savedDao, historyDao, deletedAnimeDao)
    }

    suspend fun importBackupJson(json: String): ImportDataResult = withContext(Dispatchers.IO) {
        val savedDao = savedAnimeDao ?: return@withContext ImportDataResult(false, "Local storage unavailable")
        val res = BackupRestoreManager.importBackupJson(json, animeDao, savedDao, historyDao, deletedAnimeDao)
        if (res.isSuccess) {
            val summary = res.getOrThrow()
            val allSaved = savedDao.getAllSavedAnimeSync()
            animeDao.insertAnimeList(allSaved.map { it.toAnimeEntity() })
            ImportDataResult(true, "Imported ${summary.importedAnimeCount} anime entries.", summary.importedAnimeCount)
        } else {
            ImportDataResult(false, res.exceptionOrNull()?.localizedMessage ?: "Import failed")
        }
    }

    suspend fun reconcileSavedAnime() = withContext(Dispatchers.IO) {
        val savedDao = savedAnimeDao ?: return@withContext
        val saved = savedDao.getAllSavedAnimeSync()
        for (s in saved) {
            val main = animeDao.getAnimeByIdSync(s.id)
            if (main == null) {
                animeDao.insertAnime(s.toAnimeEntity())
            }
        }
    }

    private fun mergeAnimeWithSaved(
        mainList: List<AnimeEntity>,
        savedList: List<SavedAnimeEntity>,
        predicate: (AnimeEntity) -> Boolean
    ): List<AnimeEntity> {
        val byId = mainList.associateBy { it.id }.toMutableMap()
        for (saved in savedList) {
            val existing = byId[saved.id]
            val merged = if (existing != null) {
                existing.copy(
                    titleEnglish = existing.titleEnglish?.ifBlank { null } ?: saved.titleEnglish,
                    coverImage = existing.coverImage.ifBlank { saved.coverImage },
                    synopsis = existing.synopsis?.ifBlank { null } ?: saved.synopsis,
                    studios = existing.studios.ifBlank { saved.studios },
                    watchedEpisodeCount = maxOf(existing.watchedEpisodeCount, saved.watchedEpisodeCount),
                    userSeason = if (existing.inWatchlist) existing.userSeason else saved.userSeason,
                    inWatchlist = existing.inWatchlist || saved.inWatchlist,
                    isWishlist = existing.isWishlist || saved.isWishlist,
                    isFavorite = existing.isFavorite || saved.isFavorite,
                    isPinned = existing.isPinned || saved.isPinned,
                    userStatus = existing.userStatus ?: saved.userStatus,
                    userScore = existing.userScore ?: saved.userScore,
                    userNotes = existing.userNotes ?: saved.userNotes
                )
            } else {
                saved.toAnimeEntity()
            }
            if (predicate(merged)) {
                byId[saved.id] = merged
            }
        }
        return byId.values.filter(predicate)
    }

    private suspend fun syncEntityToSavedTable(entity: AnimeEntity?) {
        val savedDao = savedAnimeDao ?: return
        val target = entity ?: return
        val existingSaved = savedDao.getByIdSync(target.id)
        if (target.hasActiveUserState) {
            savedDao.upsertSavedAnime(target.toSavedAnimeEntity(existingSavedAt = existingSaved?.savedAt))
        } else if (existingSaved != null) {
            savedDao.deleteSavedAnimeById(target.id)
        }
    }
}
