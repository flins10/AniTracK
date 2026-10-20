package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.anime.MalAnimeFetcher
import com.example.anime.UnifiedEpisodeFetcher
import com.example.data.local.AnimeDao
import com.example.data.local.AnimeEntity
import com.example.data.local.ApiCacheDao
import com.example.data.local.ApiCacheEntity
import com.example.data.local.DeletedAnimeDao
import com.example.data.local.DeletedAnimeEntity
import com.example.data.local.EpisodeHistoryDao
import com.example.data.local.EpisodeHistoryEntity
import com.example.data.local.PlaybackPreferenceDao
import com.example.data.local.PlaybackPreferenceEntity
import com.example.data.local.SavedAnimeDao
import com.example.data.local.SavedAnimeEntity
import com.example.data.local.toSavedAnimeEntity
import com.example.data.remote.AniListApiService
import com.example.data.remote.AniListMedia
import com.example.data.remote.AniListQueries
import com.example.data.remote.AniListRequest
import com.example.data.remote.AniListTitle
import com.example.data.remote.MyAnimeListConfig
import com.example.data.remote.MyAnimeListService
import com.example.util.BackupRestoreManager
import com.example.util.EnglishTitleMapper
import com.example.util.EpisodeDataParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

class AnimeRepository(
    private val animeDao: AnimeDao,
    private val historyDao: EpisodeHistoryDao,
    private val apiCacheDao: ApiCacheDao? = null,
    private val deletedAnimeDao: DeletedAnimeDao? = null,
    private val savedAnimeDao: SavedAnimeDao? = null,
    private val playbackPreferenceDao: PlaybackPreferenceDao? = null,
    private val apiService: AniListApiService = AniListApiService.create()
) {
    val watchlist: Flow<List<AnimeEntity>> = if (savedAnimeDao != null) {
        combine(animeDao.getAllWatchlistAnime(), savedAnimeDao.getWatchlistFlow()) { mainList, savedList ->
            mergeAnimeWithSaved(mainList, savedList) { it.inWatchlist }
                .sortedWith(compareByDescending<AnimeEntity> { it.isPinned }.thenBy { it.airingDayOfWeek }.thenBy { it.airingTimeUnix ?: Long.MAX_VALUE })
        }
    } else {
        animeDao.getAllWatchlistAnime()
    }

    val wishlist: Flow<List<AnimeEntity>> = if (savedAnimeDao != null) {
        combine(animeDao.getWishlistAnime(), savedAnimeDao.getWishlistFlow()) { mainList, savedList ->
            mergeAnimeWithSaved(mainList, savedList) { it.isWishlist }
                .sortedBy { (it.titleEnglish ?: it.titleRomaji).lowercase() }
        }
    } else {
        animeDao.getWishlistAnime()
    }

    val favorites: Flow<List<AnimeEntity>> = if (savedAnimeDao != null) {
        combine(animeDao.getFavorites(), savedAnimeDao.getFavoritesFlow()) { mainList, savedList ->
            mergeAnimeWithSaved(mainList, savedList) { it.isFavorite }
                .sortedBy { (it.titleEnglish ?: it.titleRomaji).lowercase() }
        }
    } else {
        animeDao.getFavorites()
    }

    val watchHistory: Flow<List<EpisodeHistoryEntity>> = historyDao.getAllHistory()
    val upcomingAnime: Flow<List<AnimeEntity>> = animeDao.getUpcomingAnime()
    val allOngoingAnime: Flow<List<AnimeEntity>> = animeDao.getAllOngoingAnime()
    val ongoingDonghua: Flow<List<AnimeEntity>> = animeDao.getOngoingDonghua()

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
                    idMal = existing.idMal ?: saved.idMal,
                    archiveIdentifier = existing.archiveIdentifier ?: saved.archiveIdentifier,
                    titleEnglish = existing.titleEnglish?.ifBlank { null } ?: saved.titleEnglish,
                    coverImage = existing.coverImage.ifBlank { saved.coverImage },
                    synopsis = existing.synopsis?.ifBlank { null } ?: saved.synopsis,
                    watchedEpisodeCount = maxOf(existing.watchedEpisodeCount, saved.watchedEpisodeCount),
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
        val hasSavedState = target.inWatchlist || target.isWishlist || target.isFavorite ||
                target.isPinned || target.watchedEpisodeCount > 0 ||
                !target.userStatus.isNullOrBlank() || !target.userNotes.isNullOrBlank() ||
                !target.archiveIdentifier.isNullOrBlank()
        if (hasSavedState) {
            savedDao.upsertSavedAnime(target.toSavedAnimeEntity(existingSavedAt = existingSaved?.savedAt))
        } else if (existingSaved != null) {
            savedDao.deleteSavedAnimeById(target.id)
        }
    }

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

    fun getCalendarAnimeByDay(dayOfWeek: Int): Flow<List<AnimeEntity>> {
        return animeDao.getCalendarAnimeByDay(dayOfWeek)
    }

    fun getAnimeByDay(dayOfWeek: Int): Flow<List<AnimeEntity>> {
        return animeDao.getCalendarAnimeByDay(dayOfWeek)
    }

    fun getAnimeById(id: Int): Flow<AnimeEntity?> {
        return if (savedAnimeDao != null) {
            combine(animeDao.getAnimeById(id), savedAnimeDao.getWatchlistFlow()) { mainEntity, savedList ->
                val saved = savedList.find { it.id == id } ?: savedAnimeDao.getByIdSync(id)
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

    suspend fun getAnimeByMalIdSync(idMal: Int): AnimeEntity? = withContext(Dispatchers.IO) {
        animeDao.getAnimeByMalIdSync(idMal)
    }

    fun searchWatchlist(query: String): Flow<List<AnimeEntity>> {
        return animeDao.searchWatchlist(query)
    }

    suspend fun updateWatchedEpisodeCount(animeId: Int, newCount: Int, accessToken: String? = null) = withContext(Dispatchers.IO) {
        val anime = animeDao.getAnimeByIdSync(animeId) ?: return@withContext
        val clamped = newCount.coerceAtLeast(0)
        val isCompleted = if (anime.totalEpisodes != null && anime.totalEpisodes > 0) {
            clamped >= anime.totalEpisodes
        } else false

        animeDao.updateWatchedEpisodeCount(
            id = animeId,
            count = clamped,
            isWatched = isCompleted || clamped > 0,
            lastWatchedTime = System.currentTimeMillis()
        )
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
        historyDao.insertHistory(
            EpisodeHistoryEntity(
                animeId = anime.id,
                animeTitle = anime.titleEnglish ?: anime.titleRomaji,
                episodeNumber = clamped,
                watchedTimestamp = System.currentTimeMillis(),
                isWatched = true
            )
        )

        // MAL Sync (Only if MAL ID exists & accessToken provided)
        val malId = anime.idMal
        if (malId != null && !accessToken.isNullOrBlank()) {
            try {
                MyAnimeListService.updateAndVerifyAnimeStatusDetailed(
                    accessToken = accessToken,
                    animeTitle = anime.titleEnglish ?: anime.titleRomaji,
                    malId = malId,
                    numEpisodesWatched = clamped,
                    status = if (isCompleted) "completed" else "watching",
                    score = anime.userScore
                )
            } catch (e: Exception) {
                // Network error soft fallback - local tracking succeeds
            }
        }
    }

    suspend fun updateUserMediaState(
        animeId: Int,
        userStatus: String? = null,
        userScore: Int? = null,
        userNotes: String? = null,
        watchedCount: Int? = null,
        startDate: String? = null,
        finishDate: String? = null,
        accessToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val anime = animeDao.getAnimeByIdSync(animeId) ?: return@withContext Result.failure(Exception("Anime not found"))
        val count = (watchedCount ?: anime.watchedEpisodeCount).coerceAtLeast(0)
        val status = userStatus ?: anime.userStatus ?: (if (count > 0) "watching" else null)
        val score = userScore ?: anime.userScore
        val notes = userNotes ?: anime.userNotes
        val isCompleted = (anime.totalEpisodes != null && anime.totalEpisodes > 0 && count >= anime.totalEpisodes) || status?.lowercase() == "completed"

        // 1. Always update local database first
        animeDao.updateUserTrackingState(
            id = animeId,
            userStatus = status,
            userScore = score,
            userNotes = notes,
            watchedCount = count,
            startDate = startDate ?: anime.startDate,
            finishDate = finishDate ?: anime.finishDate,
            isWatched = isCompleted || count > 0,
            lastWatchedTime = System.currentTimeMillis()
        )
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
        historyDao.insertHistory(
            EpisodeHistoryEntity(
                animeId = anime.id,
                animeTitle = anime.titleEnglish ?: anime.titleRomaji,
                episodeNumber = count,
                watchedTimestamp = System.currentTimeMillis(),
                isWatched = true
            )
        )

        // 2. Sync to MAL if title has a MAL ID and user is logged in
        val malId = anime.idMal
        if (malId != null && !accessToken.isNullOrBlank()) {
            try {
                MyAnimeListService.updateAndVerifyAnimeStatusDetailed(
                    accessToken = accessToken,
                    animeTitle = anime.titleEnglish ?: anime.titleRomaji,
                    malId = malId,
                    numEpisodesWatched = count,
                    status = status,
                    score = score
                )
            } catch (e: Exception) {
                // Local state is preserved regardless of MAL errors
            }
        }
        Result.success(Unit)
    }

    suspend fun addNewLocalTitle(
        title: String,
        titleNative: String? = null,
        titlePinyin: String? = null,
        countryOfOrigin: String = "JP",
        totalEpisodes: Int? = null,
        currentEpisode: Int = 1,
        synopsis: String? = null,
        coverImage: String? = null,
        format: String = "TV",
        genres: String = ""
    ): AnimeEntity = withContext(Dispatchers.IO) {
        val newId = (System.currentTimeMillis() % 1_000_000_000).toInt().coerceAtLeast(900000)
        val isDonghua = countryOfOrigin.equals("CN", true) || genres.contains("Donghua", true)
        val entity = AnimeEntity(
            id = newId,
            idMal = null, // Local title without MAL ID
            titleRomaji = title,
            titleEnglish = title,
            titleNative = titleNative,
            titlePinyin = if (isDonghua) (titlePinyin ?: title) else null,
            metadataSource = "local",
            coverImage = coverImage?.ifBlank { null } ?: "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21-A233R1.jpg",
            format = format,
            status = "RELEASING",
            totalEpisodes = totalEpisodes,
            currentEpisode = currentEpisode,
            watchedEpisodeCount = 0,
            airingDayOfWeek = 1,
            genres = if (isDonghua && !genres.contains("Donghua", true)) if (genres.isBlank()) "Donghua" else "$genres, Donghua" else genres,
            synopsis = synopsis ?: "Locally added media title untracked on MyAnimeList.",
            countryOfOrigin = countryOfOrigin,
            inWatchlist = true
        )
        animeDao.insertAnime(entity)
        syncEntityToSavedTable(entity)
        entity
    }

    suspend fun incrementWatchedEpisode(anime: AnimeEntity, newEpisodeCount: Int = anime.watchedEpisodeCount + 1) = withContext(Dispatchers.IO) {
        if (animeDao.getAnimeByIdSync(anime.id) == null) {
            animeDao.insertAnime(anime)
        }
        updateWatchedEpisodeCount(anime.id, newEpisodeCount)
    }

    suspend fun toggleWatchedStatus(animeId: Int, isWatched: Boolean) = withContext(Dispatchers.IO) {
        val anime = animeDao.getAnimeByIdSync(animeId) ?: return@withContext
        val targetCount = if (isWatched) {
            anime.nextEpisodeNumber ?: anime.currentEpisode
        } else {
            (anime.watchedEpisodeCount - 1).coerceAtLeast(0)
        }
        val updated = anime.copy(
            isWatched = isWatched,
            watchedEpisodeCount = targetCount,
            lastWatchedTime = if (isWatched) System.currentTimeMillis() else anime.lastWatchedTime
        )
        animeDao.updateAnime(updated)
        syncEntityToSavedTable(updated)
        if (isWatched) {
            historyDao.insertHistory(
                EpisodeHistoryEntity(
                    animeId = anime.id,
                    animeTitle = anime.titleEnglish ?: anime.titleRomaji,
                    episodeNumber = targetCount,
                    watchedTimestamp = System.currentTimeMillis(),
                    isWatched = true
                )
            )
        } else {
            historyDao.deleteHistoryForEpisode(
                animeId = anime.id,
                episodeNumber = targetCount
            )
        }
    }

    suspend fun toggleFavorite(animeId: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        animeDao.updateFavoriteStatus(animeId, isFavorite)
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
    }

    suspend fun togglePinned(animeId: Int, isPinned: Boolean) = withContext(Dispatchers.IO) {
        animeDao.updatePinnedStatus(animeId, isPinned)
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
    }

    suspend fun recordDeletion(
        animeId: Int,
        malId: Int? = null,
        title: String? = null,
        listType: String = "ALL"
    ) = withContext(Dispatchers.IO) {
        deletedAnimeDao?.insertTombstone(
            DeletedAnimeEntity(
                animeId = animeId,
                malId = malId,
                title = title,
                listType = listType,
                deletedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearDeletion(animeId: Int, malId: Int? = null) = withContext(Dispatchers.IO) {
        deletedAnimeDao?.removeTombstone(animeId, malId)
    }

    suspend fun isDeleted(animeId: Int, malId: Int? = null, listType: String = "ALL"): Boolean = withContext(Dispatchers.IO) {
        deletedAnimeDao?.isDeleted(animeId, malId, listType) ?: false
    }

    suspend fun removeFromPlanner(animeId: Int, malId: Int? = null) = withContext(Dispatchers.IO) {
        val entity = animeDao.getAnimeByIdSync(animeId)
        recordDeletion(animeId, malId ?: entity?.idMal, entity?.displayTitle, listType = "WATCHLIST")
        animeDao.updateWatchlistStatus(animeId, false)
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
    }

    suspend fun removeFromWishlist(animeId: Int, malId: Int? = null) = withContext(Dispatchers.IO) {
        val entity = animeDao.getAnimeByIdSync(animeId)
        recordDeletion(animeId, malId ?: entity?.idMal, entity?.displayTitle, listType = "WISHLIST")
        animeDao.updateWishlistStatus(animeId, false)
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
    }

    suspend fun deleteAnimeCompletely(animeId: Int, malId: Int? = null) = withContext(Dispatchers.IO) {
        val entity = animeDao.getAnimeByIdSync(animeId) ?: savedAnimeDao?.getByIdSync(animeId)?.toAnimeEntity()
        recordDeletion(animeId, malId ?: entity?.idMal, entity?.displayTitle, listType = "ALL")
        animeDao.deleteAnime(animeId)
        savedAnimeDao?.deleteSavedAnimeById(animeId)
    }

    suspend fun toggleWishlist(animeId: Int, isWishlist: Boolean) = withContext(Dispatchers.IO) {
        if (!isWishlist) {
            val entity = animeDao.getAnimeByIdSync(animeId)
            recordDeletion(animeId, entity?.idMal, entity?.displayTitle, listType = "WISHLIST")
        } else {
            val entity = animeDao.getAnimeByIdSync(animeId)
            clearDeletion(animeId, entity?.idMal)
        }
        animeDao.updateWishlistStatus(animeId, isWishlist)
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
    }

    suspend fun toggleWishlistEntity(anime: AnimeEntity) = withContext(Dispatchers.IO) {
        val existing = animeDao.getAnimeByIdSync(anime.id)
        if (existing != null) {
            val newIsWishlist = !existing.isWishlist
            if (!newIsWishlist) {
                recordDeletion(anime.id, anime.idMal, anime.displayTitle, listType = "WISHLIST")
            } else {
                clearDeletion(anime.id, anime.idMal)
            }
            animeDao.updateWishlistStatus(anime.id, newIsWishlist)
            syncEntityToSavedTable(animeDao.getAnimeByIdSync(anime.id))
        } else {
            clearDeletion(anime.id, anime.idMal)
            val inserted = anime.copy(isWishlist = true)
            animeDao.insertAnime(inserted)
            syncEntityToSavedTable(inserted)
        }
    }

    suspend fun toggleNotification(animeId: Int, enabled: Boolean) = withContext(Dispatchers.IO) {
        animeDao.updateNotificationStatus(animeId, enabled)
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
    }

    suspend fun clearWatchHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAllHistory()
    }

    suspend fun updateAnime(anime: AnimeEntity) = withContext(Dispatchers.IO) {
        animeDao.updateAnime(anime)
        syncEntityToSavedTable(anime)
    }

    suspend fun addToWatchlist(anime: AnimeEntity) = withContext(Dispatchers.IO) {
        clearDeletion(anime.id, anime.idMal)
        val existing = animeDao.getAnimeByIdSync(anime.id)
        if (existing != null) {
            val updated = existing.copy(
                inWatchlist = true,
                archiveIdentifier = anime.archiveIdentifier ?: existing.archiveIdentifier
            )
            animeDao.updateAnime(updated)
            syncEntityToSavedTable(updated)
        } else {
            val inserted = anime.copy(inWatchlist = true)
            animeDao.insertAnime(inserted)
            syncEntityToSavedTable(inserted)
        }
    }

    suspend fun removeFromWatchlist(animeId: Int, malId: Int? = null) = withContext(Dispatchers.IO) {
        val entity = animeDao.getAnimeByIdSync(animeId)
        recordDeletion(animeId, malId ?: entity?.idMal, entity?.displayTitle, listType = "WATCHLIST")
        animeDao.updateWatchlistStatus(animeId, false)
        syncEntityToSavedTable(animeDao.getAnimeByIdSync(animeId))
    }

    suspend fun getAnimeByTitleSync(title: String): AnimeEntity? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null
        animeDao.getAnimeByExactTitleSync(title.trim()) ?: run {
            val all = animeDao.getAllAnimeSync()
            val lower = title.trim().lowercase()
            all.firstOrNull {
                it.titleRomaji.equals(lower, true) ||
                it.titleEnglish?.equals(lower, true) == true ||
                it.titleNative?.equals(lower, true) == true ||
                (it.titleEnglish != null && lower.contains(it.titleEnglish.lowercase())) ||
                lower.contains(it.titleRomaji.lowercase())
            }
        }
    }

    suspend fun ensureDatabaseDeduplicated() = withContext(Dispatchers.IO) {
        try {
            val all = animeDao.getAllAnimeSync()
            if (all.isEmpty()) return@withContext
            val toDelete = mutableListOf<Int>()

            fun mergeDuplicateGroup(primary: AnimeEntity, duplicates: List<AnimeEntity>): AnimeEntity {
                val allInGroup = listOf(primary) + duplicates
                val mergedWatched = allInGroup.maxOf { it.watchedEpisodeCount }
                val mergedCurrentEp = allInGroup.maxOf { it.currentEpisode }
                val mergedInWatchlist = allInGroup.any { it.inWatchlist }
                val mergedWishlist = allInGroup.any { it.isWishlist }
                val mergedFavorite = allInGroup.any { it.isFavorite }
                val mergedPinned = allInGroup.any { it.isPinned }
                val mergedWatchedFlag = allInGroup.any { it.isWatched } || mergedWatched > 0
                val mergedMalId = primary.idMal ?: duplicates.firstNotNullOfOrNull { it.idMal }
                val mergedArchiveId = primary.archiveIdentifier?.ifBlank { null }
                    ?: duplicates.firstNotNullOfOrNull { it.archiveIdentifier?.ifBlank { null } }
                val mergedUserStatus = primary.userStatus?.ifBlank { null }
                    ?: duplicates.firstNotNullOfOrNull { it.userStatus?.ifBlank { null } }
                val mergedUserScore = primary.userScore ?: duplicates.firstNotNullOfOrNull { it.userScore }
                val mergedUserNotes = primary.userNotes?.ifBlank { null }
                    ?: duplicates.firstNotNullOfOrNull { it.userNotes?.ifBlank { null } }
                val mergedStartDate = primary.startDate ?: duplicates.firstNotNullOfOrNull { it.startDate }
                val mergedFinishDate = primary.finishDate ?: duplicates.firstNotNullOfOrNull { it.finishDate }
                val mergedLastWatched = allInGroup.mapNotNull { it.lastWatchedTime }.maxOrNull()
                val mergedScore = if (primary.score != null && primary.score > 0) primary.score else duplicates.firstNotNullOfOrNull { it.score }

                return primary.copy(
                    idMal = mergedMalId,
                    archiveIdentifier = mergedArchiveId,
                    currentEpisode = mergedCurrentEp,
                    watchedEpisodeCount = mergedWatched,
                    inWatchlist = mergedInWatchlist,
                    isWishlist = mergedWishlist,
                    isFavorite = mergedFavorite,
                    isPinned = mergedPinned,
                    isWatched = mergedWatchedFlag,
                    userStatus = mergedUserStatus,
                    userScore = mergedUserScore,
                    userNotes = mergedUserNotes,
                    startDate = mergedStartDate,
                    finishDate = mergedFinishDate,
                    lastWatchedTime = mergedLastWatched,
                    score = mergedScore
                )
            }

            // 1. Group by idMal (if non-null and > 0)
            val malGroups = all.filter { it.idMal != null && it.idMal > 0 }.groupBy { it.idMal!! }
            for ((_, group) in malGroups) {
                if (group.size > 1) {
                    val primary = group.minByOrNull { it.id } ?: group.first()
                    val duplicates = group.filter { it.id != primary.id }
                    val updatedPrimary = mergeDuplicateGroup(primary, duplicates)
                    animeDao.updateAnime(updatedPrimary)
                    syncEntityToSavedTable(updatedPrimary)
                    for (dup in duplicates) {
                        savedAnimeDao?.deleteSavedAnimeById(dup.id)
                    }
                    toDelete.addAll(duplicates.map { it.id })
                }
            }

            // 2. Group by normalized title
            val remaining = animeDao.getAllAnimeSync().filter { it.id !in toDelete }
            val titleGroups = remaining.groupBy { (it.titleEnglish ?: it.titleRomaji).trim().lowercase() }
            for ((_, group) in titleGroups) {
                if (group.size > 1) {
                    val primary = group.maxWithOrNull(
                        compareBy<AnimeEntity> { it.idMal != null }
                            .thenBy { !it.archiveIdentifier.isNullOrBlank() }
                            .thenBy { it.inWatchlist || it.isWishlist || it.isFavorite }
                            .thenBy { -it.id }
                    ) ?: group.first()
                    val duplicates = group.filter { it.id != primary.id }
                    val updatedPrimary = mergeDuplicateGroup(primary, duplicates)
                    animeDao.updateAnime(updatedPrimary)
                    syncEntityToSavedTable(updatedPrimary)
                    for (dup in duplicates) {
                        savedAnimeDao?.deleteSavedAnimeById(dup.id)
                    }
                    toDelete.addAll(duplicates.map { it.id })
                }
            }

            if (toDelete.isNotEmpty()) {
                animeDao.deleteAnimeList(toDelete.distinct())
            }
        } catch (e: Exception) {
            // Ignore deduplication errors
        }
    }

    /**
     * Reconciles durable `saved_anime` table and `anime` table in both directions so that
     * no saved record is ever lost across restarts, cache clears, or schema upgrades.
     */
    suspend fun reconcileSavedAnime() = withContext(Dispatchers.IO) {
        val savedDao = savedAnimeDao ?: return@withContext
        val allAnime = animeDao.getAllAnimeSync()
        for (entity in allAnime) {
            val hasSavedState = entity.inWatchlist || entity.isWishlist || entity.isFavorite ||
                    entity.isPinned || entity.watchedEpisodeCount > 0 ||
                    !entity.userStatus.isNullOrBlank() || !entity.userNotes.isNullOrBlank() ||
                    !entity.archiveIdentifier.isNullOrBlank()
            if (hasSavedState) {
                val existingSaved = savedDao.getByIdSync(entity.id)
                savedDao.upsertSavedAnime(entity.toSavedAnimeEntity(existingSavedAt = existingSaved?.savedAt))
            }
        }

        val allSaved = savedDao.getAllSavedAnimeSync()
        for (saved in allSaved) {
            if (!saved.hasActiveUserState) continue
            val existingInMain = animeDao.getAnimeByIdSync(saved.id)
            if (existingInMain == null) {
                animeDao.insertAnime(saved.toAnimeEntity())
            } else {
                val merged = existingInMain.copy(
                    idMal = existingInMain.idMal ?: saved.idMal,
                    archiveIdentifier = existingInMain.archiveIdentifier?.ifBlank { null } ?: saved.archiveIdentifier,
                    titleEnglish = existingInMain.titleEnglish?.ifBlank { null } ?: saved.titleEnglish,
                    coverImage = existingInMain.coverImage.ifBlank { saved.coverImage },
                    synopsis = existingInMain.synopsis?.ifBlank { null } ?: saved.synopsis,
                    watchedEpisodeCount = maxOf(existingInMain.watchedEpisodeCount, saved.watchedEpisodeCount),
                    inWatchlist = existingInMain.inWatchlist || saved.inWatchlist,
                    isWishlist = existingInMain.isWishlist || saved.isWishlist,
                    isFavorite = existingInMain.isFavorite || saved.isFavorite,
                    isPinned = existingInMain.isPinned || saved.isPinned,
                    userStatus = existingInMain.userStatus ?: saved.userStatus,
                    userScore = existingInMain.userScore ?: saved.userScore,
                    userNotes = existingInMain.userNotes ?: saved.userNotes
                )
                animeDao.updateAnime(merged)
                savedDao.upsertSavedAnime(merged.toSavedAnimeEntity(existingSavedAt = saved.savedAt))
            }
        }
    }

    /**
     * Clears transient API response caches and non-saved browse catalogue rows while strictly
     * preserving all saved anime, watchlists, wishlists, favorites, watch history, and preferences.
     */
    suspend fun clearMetadataAndApiCache() = withContext(Dispatchers.IO) {
        reconcileSavedAnime()
        apiCacheDao?.clearAllCache()
        animeDao.clearUnusedCatalogCache()
        reconcileSavedAnime()
    }

    /**
     * Saves or updates an Internet Archive media item using its stable `archiveIdentifier`
     * so that renamed titles or offline/unavailable artwork never lose the saved entry or create duplicates.
     */
    suspend fun saveArchiveMediaItem(
        archiveIdentifier: String,
        title: String,
        description: String?,
        thumbnailUrl: String?,
        year: Int? = null,
        inWatchlist: Boolean = true,
        isWishlist: Boolean = false
    ): AnimeEntity = withContext(Dispatchers.IO) {
        val cleanId = archiveIdentifier.trim()
        val existingByArchive = if (cleanId.isNotEmpty()) {
            animeDao.getAnimeByArchiveIdentifierSync(cleanId)
                ?: savedAnimeDao?.getByArchiveIdentifierSync(cleanId)?.toAnimeEntity()
        } else null

        val stableNumericId = existingByArchive?.id
            ?: (900_000_000 + abs(cleanId.ifEmpty { title }.hashCode() % 90_000_000))

        val cleanTitle = title.trim().ifBlank { existingByArchive?.titleEnglish ?: existingByArchive?.titleRomaji ?: cleanId }
        val cleanCover = thumbnailUrl?.trim()?.ifBlank { null }
            ?: existingByArchive?.coverImage?.ifBlank { null }
            ?: "https://archive.org/services/img/$cleanId"
        val cleanSynopsis = description?.replace(Regex("<[^>]*>"), "")?.trim()?.ifBlank { null }
            ?: existingByArchive?.synopsis

        clearDeletion(stableNumericId, existingByArchive?.idMal)

        val entity = if (existingByArchive != null) {
            existingByArchive.copy(
                archiveIdentifier = cleanId,
                titleRomaji = cleanTitle,
                titleEnglish = cleanTitle,
                coverImage = cleanCover,
                synopsis = cleanSynopsis,
                seasonYear = year ?: existingByArchive.seasonYear,
                inWatchlist = inWatchlist || existingByArchive.inWatchlist,
                isWishlist = isWishlist || existingByArchive.isWishlist,
                lastCachedAt = System.currentTimeMillis()
            )
        } else {
            AnimeEntity(
                id = stableNumericId,
                idMal = null,
                archiveIdentifier = cleanId,
                titleRomaji = cleanTitle,
                titleEnglish = cleanTitle,
                metadataSource = "internet_archive",
                coverImage = cleanCover,
                format = "ONA",
                status = "FINISHED",
                currentEpisode = 1,
                watchedEpisodeCount = 0,
                airingDayOfWeek = 1,
                genres = "Internet Archive",
                seasonYear = year ?: 2026,
                synopsis = cleanSynopsis ?: "Saved from Internet Archive ($cleanId)",
                inWatchlist = inWatchlist,
                isWishlist = isWishlist,
                lastCachedAt = System.currentTimeMillis()
            )
        }

        animeDao.insertAnime(entity)
        syncEntityToSavedTable(entity)
        entity
    }

    data class BackupRestoreOutcome(
        val success: Boolean,
        val message: String,
        val importedAnimeCount: Int = 0
    )

    suspend fun exportUserDataJson(sharedPrefs: SharedPreferences? = null): String = withContext(Dispatchers.IO) {
        reconcileSavedAnime()
        if (sharedPrefs != null && playbackPreferenceDao != null) {
            val safeEntries = sharedPrefs.all.entries
                .filter { (k, v) -> v != null && BackupRestoreManager.isSafePreferenceKey(k) }
                .map { (k, v) -> PlaybackPreferenceEntity(prefKey = "app_pref:$k", prefValue = v.toString()) }
            if (safeEntries.isNotEmpty()) {
                playbackPreferenceDao.upsertPreferences(safeEntries)
            }
        }
        val savedDao = savedAnimeDao ?: throw IllegalStateException("SavedAnimeDao not initialized")
        BackupRestoreManager.exportBackupJson(
            animeDao = animeDao,
            savedAnimeDao = savedDao,
            historyDao = historyDao,
            deletedAnimeDao = deletedAnimeDao,
            playbackPreferenceDao = playbackPreferenceDao
        )
    }

    suspend fun importUserDataJson(
        jsonString: String,
        sharedPrefs: SharedPreferences? = null
    ): BackupRestoreOutcome = withContext(Dispatchers.IO) {
        val savedDao = savedAnimeDao ?: return@withContext BackupRestoreOutcome(false, "SavedAnimeDao unavailable")
        val res = BackupRestoreManager.importBackupJson(
            jsonString = jsonString,
            animeDao = animeDao,
            savedAnimeDao = savedDao,
            historyDao = historyDao,
            deletedAnimeDao = deletedAnimeDao,
            playbackPreferenceDao = playbackPreferenceDao
        )
        res.fold(
            onSuccess = { summary ->
                if (sharedPrefs != null && playbackPreferenceDao != null) {
                    val allPrefs = playbackPreferenceDao.getAllPreferencesSync()
                    val editor = sharedPrefs.edit()
                    for (pref in allPrefs) {
                        if (pref.prefKey.startsWith("app_pref:")) {
                            val rawKey = pref.prefKey.removePrefix("app_pref:")
                            if (BackupRestoreManager.isSafePreferenceKey(rawKey)) {
                                editor.putString(rawKey, pref.prefValue)
                            }
                        }
                    }
                    editor.apply()
                }
                reconcileSavedAnime()
                BackupRestoreOutcome(
                    success = true,
                    message = "Restored ${summary.importedAnimeCount} saved anime, ${summary.importedHistoryCount} history entries & ${summary.importedPreferencesCount} preferences!",
                    importedAnimeCount = summary.importedAnimeCount
                )
            },
            onFailure = { err ->
                BackupRestoreOutcome(
                    success = false,
                    message = err.localizedMessage ?: "Invalid backup JSON"
                )
            }
        )
    }

    suspend fun getAllLocalAnime(query: String? = null, genre: String? = null): List<AnimeEntity> = withContext(Dispatchers.IO) {
        searchLocalDb(query, genre)
    }

    private suspend fun searchLocalDb(query: String?, genre: String?): List<AnimeEntity> {
        val allLocal = if (!query.isNullOrBlank()) {
            animeDao.searchAllAnimeSync(query!!.trim())
        } else {
            animeDao.getAllAnimeSync()
        }
        return if (!genre.isNullOrBlank() && genre != "All") {
            val filterGenre = genre!!
            allLocal.filter { it.genres.contains(filterGenre, ignoreCase = true) }
        } else {
            allLocal
        }
    }

    suspend fun searchAniList(
        query: String? = null,
        genre: String? = null,
        season: String? = null,
        seasonYear: Int? = null,
        status: String? = null,
        countryOfOrigin: String? = null,
        sort: List<String>? = null,
        page: Int = 1,
        forceRefresh: Boolean = false
    ): Result<AniListSearchResult> = withContext(Dispatchers.IO) {
        try {
            val hasSearch = !query.isNullOrBlank()
            val hasGenre = !genre.isNullOrBlank() && genre != "All" && genre != "ALL"
            val hasSeason = !season.isNullOrBlank() && season != "ALL"
            val hasSeasonYear = seasonYear != null && seasonYear > 0
            val hasStatus = !status.isNullOrBlank() && status != "ALL"
            val hasCountry = !countryOfOrigin.isNullOrBlank() && countryOfOrigin != "ALL"
            val cacheKey = "search:${query?.trim() ?: ""}:${genre ?: ""}:${season ?: ""}:${seasonYear ?: ""}:${status ?: ""}:${countryOfOrigin ?: ""}:${sort?.joinToString(",") ?: ""}:$page"

            // 1. Check Room Database Cache first to avoid redundant API calls and accelerate loading
            if (!forceRefresh && page == 1) {
                val cachedEntry = apiCacheDao?.getCache(cacheKey)
                if (cachedEntry != null && !cachedEntry.isExpired) {
                    val ids = cachedEntry.parseEntityIds()
                    if (ids.isNotEmpty()) {
                        val cachedAnime = animeDao.getAnimeByIdsSync(ids)
                        if (cachedAnime.isNotEmpty()) {
                            return@withContext Result.success(AniListSearchResult(cachedAnime, hasNextPage = true, total = cachedAnime.size))
                        }
                    }
                }
                // If specific query or genre, check local Room DB before external query
                if (hasSearch && !hasGenre && !hasSeason && !hasStatus && !hasCountry) {
                    val localMatches = animeDao.searchAllAnimeSync(query!!.trim())
                    if (localMatches.isNotEmpty() && localMatches.any { it.isCacheFresh(60 * 60 * 1000L) }) {
                        return@withContext Result.success(AniListSearchResult(localMatches, hasNextPage = false, total = localMatches.size))
                    }
                }
            }

            // 2. Query AniList API if not cached or forced
            val vars = mutableMapOf<String, Any?>("page" to page, "perPage" to 50)
            if (hasSearch) {
                vars["search"] = query
                vars["sort"] = if (sort != null) sort else listOf("SEARCH_MATCH", "POPULARITY_DESC")
            } else {
                vars["sort"] = if (sort != null) sort else listOf("POPULARITY_DESC")
            }
            if (hasGenre) vars["genre"] = genre
            if (hasSeason) vars["season"] = season
            if (hasSeasonYear) vars["seasonYear"] = seasonYear
            if (hasStatus) vars["status"] = status
            if (hasCountry) vars["countryOfOrigin"] = countryOfOrigin

            val queryString = AniListQueries.buildMediaQuery(
                hasSearch = hasSearch,
                hasGenre = hasGenre,
                hasSeason = hasSeason,
                hasSeasonYear = hasSeasonYear,
                hasStatus = hasStatus,
                hasCountryOfOrigin = hasCountry
            )

            val response = apiService.queryMedia(
                AniListRequest(
                    query = queryString,
                    variables = vars
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                val pageInfo = body?.data?.Page?.pageInfo
                val hasNext = pageInfo?.hasNextPage ?: false
                val total = pageInfo?.total
                val mediaList = body?.data?.Page?.media ?: emptyList()
                val entities = mediaList.map { media -> mapAniListMediaToEntity(media) }

                if (entities.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    for (entity in entities) {
                        val existing = animeDao.getAnimeByIdSync(entity.id)
                        if (existing != null) {
                            val updated = existing.copy(
                                idMal = entity.idMal ?: existing.idMal,
                                titleRomaji = entity.titleRomaji,
                                titleEnglish = entity.titleEnglish ?: existing.titleEnglish,
                                titleNative = entity.titleNative ?: existing.titleNative,
                                coverImage = if (entity.coverImage.isNotEmpty()) entity.coverImage else existing.coverImage,
                                bannerImage = entity.bannerImage ?: existing.bannerImage,
                                format = entity.format,
                                status = entity.status,
                                totalEpisodes = entity.totalEpisodes ?: existing.totalEpisodes,
                                score = entity.score ?: existing.score,
                                synopsis = entity.synopsis ?: existing.synopsis,
                                genres = if (entity.genres.isNotEmpty()) entity.genres else existing.genres,
                                lastCachedAt = now
                            )
                            animeDao.updateAnime(updated)
                            syncEntityToSavedTable(updated)
                        } else {
                            animeDao.insertAnime(entity.copy(lastCachedAt = now))
                        }
                    }

                    // Save cache query mapping to Room ApiCacheEntity
                    apiCacheDao?.insertCache(
                        ApiCacheEntity.fromIds(
                            cacheKey = cacheKey,
                            ids = entities.map { it.id },
                            ttlMillis = 60 * 60 * 1000L // 1 hour TTL
                        )
                    )

                    Result.success(AniListSearchResult(entities, hasNextPage = hasNext, total = total))
                } else {
                    val local = searchLocalDb(query, genre)
                    Result.success(AniListSearchResult(local, hasNextPage = false, total = local.size))
                }
            } else {
                val local = searchLocalDb(query, genre)
                if (local.isNotEmpty()) {
                    Result.success(AniListSearchResult(local, hasNextPage = false, total = local.size))
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: Unable to load catalogue from AniList"))
                }
            }
        } catch (e: Exception) {
            val local = searchLocalDb(query, genre)
            if (local.isNotEmpty()) {
                Result.success(AniListSearchResult(local, hasNextPage = false, total = local.size))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun toggleWatchlist(anime: AnimeEntity) = withContext(Dispatchers.IO) {
        val existing = animeDao.getAnimeByIdSync(anime.id)
        if (existing != null) {
            val newWatchlist = !existing.inWatchlist
            animeDao.updateWatchlistStatus(anime.id, newWatchlist)
            syncEntityToSavedTable(animeDao.getAnimeByIdSync(anime.id))
        } else {
            val inserted = anime.copy(inWatchlist = true)
            animeDao.insertAnime(inserted)
            syncEntityToSavedTable(inserted)
        }
    }

    suspend fun refreshSchedule(forceRefresh: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis() / 1000
            val oneWeekSeconds = 7 * 24 * 3600

            // 0. Check Room Database Cache first to avoid redundant live schedule fetching
            if (!forceRefresh) {
                val cached = apiCacheDao?.getCache("schedule_weekly")
                if (cached != null && !cached.isExpired && animeDao.getAllOngoingAnimeSync().isNotEmpty()) {
                    return@withContext Result.success(Unit)
                }
            }

            // 1. Fetch Top Popular Currently Releasing Anime (Pages 1 and 2 = 100 Top Airing Anime)
            for (page in 1..2) {
                try {
                    val releasingVars = mapOf<String, Any?>("page" to page, "perPage" to 50)
                    val response = apiService.queryMedia(
                        AniListRequest(
                            query = AniListQueries.POPULAR_RELEASING_MEDIA_QUERY,
                            variables = releasingVars
                        )
                    )
                    val mediaList = response.body()?.data?.Page?.media ?: emptyList()
                    for (media in mediaList) {
                        saveOrUpdateAniListMedia(media, now)
                    }
                    if (response.body()?.data?.Page?.pageInfo?.hasNextPage != true) break
                } catch (e: Exception) {
                    // Continue to next page
                }
            }

            // 2. Fetch Popular Ongoing Chinese Animation / Donghua (Pages 1 and 2 = 100 Donghua)
            for (page in 1..2) {
                try {
                    val donghuaVars = mapOf<String, Any?>("page" to page, "perPage" to 50)
                    val response = apiService.queryMedia(
                        AniListRequest(
                            query = AniListQueries.POPULAR_DONGHUA_QUERY,
                            variables = donghuaVars
                        )
                    )
                    val mediaList = response.body()?.data?.Page?.media ?: emptyList()
                    for (media in mediaList) {
                        saveOrUpdateAniListMedia(media, now)
                    }
                    if (response.body()?.data?.Page?.pageInfo?.hasNextPage != true) break
                } catch (e: Exception) {
                    // Continue
                }
            }

            // 3. Fetch Live Weekly Airing Schedule (Pages 1 to 5 = 250 broadcast schedules)
            for (page in 1..5) {
                try {
                    val scheduleVars = mapOf<String, Any?>(
                        "airingAt_greater" to (now - 86400).toInt(),
                        "airingAt_lesser" to (now + oneWeekSeconds).toInt(),
                        "page" to page
                    )
                    val scheduleResponse = apiService.querySchedule(
                        AniListRequest(
                            query = AniListQueries.AIRING_SCHEDULE_QUERY,
                            variables = scheduleVars
                        )
                    )
                    val schedulePage = scheduleResponse.body()?.data?.Page
                    val schedules = schedulePage?.airingSchedules ?: emptyList()
                    if (schedules.isEmpty()) break
                    for (sched in schedules) {
                        val media = sched.media ?: continue
                        val schedAiringAt = sched.airingAt ?: now
                        val schedEp = sched.episode
                        saveOrUpdateAniListMedia(media, now, overrideAiringAt = schedAiringAt, overrideEpisode = schedEp)
                    }
                    if (schedulePage?.pageInfo?.hasNextPage != true) break
                } catch (e: Exception) {
                    // Continue
                }
            }

            // 4. Fetch Upcoming Anime
            try {
                val upcomingVars = mapOf<String, Any?>("page" to 1, "perPage" to 30)
                val upcomingResponse = apiService.queryMedia(
                    AniListRequest(
                        query = AniListQueries.UPCOMING_MEDIA_QUERY,
                        variables = upcomingVars
                    )
                )
                val upcomingList = upcomingResponse.body()?.data?.Page?.media ?: emptyList()
                for (media in upcomingList) {
                    val existing = animeDao.getAnimeByIdSync(media.id)
                    val mapped = mapAniListMediaToEntity(media)
                    if (existing != null) {
                        val updated = existing.copy(
                            titleRomaji = mapped.titleRomaji,
                            titleEnglish = mapped.titleEnglish ?: existing.titleEnglish,
                            coverImage = if (mapped.coverImage.isNotEmpty()) mapped.coverImage else existing.coverImage,
                            status = "NOT_YET_RELEASED",
                            currentEpisode = 0,
                            nextEpisodeNumber = 1,
                            season = mapped.season ?: existing.season,
                            seasonYear = mapped.seasonYear ?: existing.seasonYear,
                            airingTimeUnix = mapped.airingTimeUnix ?: existing.airingTimeUnix,
                            airingDayOfWeek = if (mapped.airingDayOfWeek > 0) mapped.airingDayOfWeek else existing.airingDayOfWeek,
                            countryOfOrigin = mapped.countryOfOrigin ?: existing.countryOfOrigin,
                            lastCachedAt = System.currentTimeMillis()
                        )
                        animeDao.updateAnime(updated)
                    } else {
                        animeDao.insertAnime(mapped.copy(inWatchlist = false, currentEpisode = 0, nextEpisodeNumber = 1, lastCachedAt = System.currentTimeMillis()))
                    }
                }
            } catch (e: Exception) {
                // Continue
            }

            // 5. Sanitize and update all local database records to ensure accurate, non-stale episode counts
            ensureAllOngoingSeeded()
            sanitizeAndFixEpisodeCounts()
            ensureDatabaseDeduplicated()

            // 6. Record schedule cache in Room
            val ongoingList = animeDao.getAllOngoingAnimeSync()
            apiCacheDao?.insertCache(
                ApiCacheEntity.fromIds("schedule_weekly", ongoingList.map { it.id }, ttlMillis = 15 * 60 * 1000L) // 15 min TTL
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyAndSyncSingleAnimeWithMal(
        animeId: Int,
        accessToken: String? = null,
        forceRefresh: Boolean = false
    ): AnimeEntity? = withContext(Dispatchers.IO) {
        try {
            var anime = animeDao.getAnimeByIdSync(animeId) ?: return@withContext null
            // If cached recently (within 30 mins) and has valid data, avoid redundant API queries
            if (!forceRefresh && anime.isCacheFresh(30 * 60 * 1000L) && (anime.idMal != null || anime.isDonghua || anime.status == "FINISHED")) {
                return@withContext anime
            }

            var malId = anime.idMal
            if (malId == null || malId <= 0) {
                val titleToSearch = anime.titleEnglish ?: anime.titleRomaji
                // Check if title has MAL ID cached in Room DB
                val cachedMatch = animeDao.getAnimeByExactTitleSync(titleToSearch)
                if (cachedMatch?.idMal != null && cachedMatch.idMal > 0) {
                    malId = cachedMatch.idMal
                    anime = anime.copy(idMal = malId)
                    animeDao.updateAnime(anime)
                } else {
                    val tokenToUse = accessToken ?: ""
                    val searchRes = MyAnimeListService.searchMalAnimeByTitle(tokenToUse, titleToSearch)
                    if (searchRes.isSuccess) {
                        malId = searchRes.getOrNull()
                        if (malId != null && malId > 0) {
                            anime = anime.copy(idMal = malId)
                            animeDao.updateAnime(anime)
                        }
                    }
                }
            }

            // Cross-query AniList, MAL, and Jikan
            val aggregated = UnifiedEpisodeFetcher.fetchAggregatedEpisodeInfo(anime, accessToken)
            val latestAiredEp = maxOf(aggregated.latestAiredEpisode, anime.currentEpisode)
            val isFinished = aggregated.status == "FINISHED" || (anime.totalEpisodes != null && anime.totalEpisodes > 0 && latestAiredEp >= anime.totalEpisodes)
            val newNextEp = if (isFinished) latestAiredEp else maxOf(anime.nextEpisodeNumber ?: (latestAiredEp + 1), aggregated.nextEpisodeNumber ?: (latestAiredEp + 1), latestAiredEp + 1)
            val resolvedTotal = if (anime.totalEpisodes != null && anime.totalEpisodes > 0 && (aggregated.totalEpisodes == null || aggregated.totalEpisodes < latestAiredEp)) {
                anime.totalEpisodes
            } else {
                aggregated.totalEpisodes ?: anime.totalEpisodes
            }
            val newStatus = if (isFinished) "FINISHED" else aggregated.status
            val newAiringTime = if (isFinished) null else (aggregated.nextAiringTimeUnix ?: anime.airingTimeUnix)
            val now = System.currentTimeMillis()

            if (latestAiredEp != anime.currentEpisode ||
                newNextEp != anime.nextEpisodeNumber ||
                resolvedTotal != anime.totalEpisodes ||
                newStatus != anime.status ||
                newAiringTime != anime.airingTimeUnix
            ) {
                val updated = anime.copy(
                    currentEpisode = latestAiredEp,
                    nextEpisodeNumber = newNextEp,
                    totalEpisodes = resolvedTotal,
                    status = newStatus,
                    airingTimeUnix = newAiringTime,
                    lastCachedAt = now
                )
                animeDao.updateAnime(updated)
                return@withContext updated
            } else {
                animeDao.updateLastCachedTime(anime.id, now)
            }
            return@withContext anime
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun verifyAndSyncWithMal(accessToken: String? = null) = withContext(Dispatchers.IO) {
        try {
            val ongoingAnime = animeDao.getAllOngoingAnimeSync()
            if (ongoingAnime.isEmpty()) return@withContext
            val chunks = ongoingAnime.chunked(4)
            for (chunk in chunks) {
                coroutineScope {
                    val jobs = chunk.map { anime ->
                        async {
                            verifyAndSyncSingleAnimeWithMal(anime.id, accessToken)
                        }
                    }
                    jobs.awaitAll()
                }
            }
        } catch (e: Exception) {
            // Ignore background network errors
        }
    }

    private suspend fun saveOrUpdateAniListMedia(
        media: AniListMedia,
        now: Long,
        overrideAiringAt: Long? = null,
        overrideEpisode: Int? = null
    ) {
        val existing = animeDao.getAnimeByIdSync(media.id)
        val bestEnglish = resolveEnglishTitle(media.title, media.synonyms)
        val rawAiringAt = overrideAiringAt ?: media.nextAiringEpisode?.airingAt ?: existing?.airingTimeUnix ?: (now + 86400)
        val rawEp = overrideEpisode ?: media.nextAiringEpisode?.episode ?: existing?.nextEpisodeNumber
        val dayOfWeek = if (rawAiringAt > 0L) {
            Instant.ofEpochSecond(rawAiringAt)
                .atZone(ZoneId.systemDefault())
                .dayOfWeek
                .value
        } else {
            ((media.id.coerceAtLeast(1) % 7) + 1)
        }
        val country = media.countryOfOrigin ?: if (media.genres?.any { it.contains("Donghua", true) || it.contains("Chinese", true) } == true) "CN" else existing?.countryOfOrigin ?: "JP"
        val pinyinTitle = if (country == "CN") media.title?.romaji else null
        val isDonghuaMedia = country == "CN" || media.genres?.any { it.contains("Donghua", true) || it.contains("Chinese", true) } == true

        val futureAiringAt = if (rawAiringAt > 0 && now >= rawAiringAt) {
            val diff = now - rawAiringAt
            rawAiringAt + ((1 + diff / 604800L) * 604800L)
        } else {
            rawAiringAt
        }

        val parsedInfo = EpisodeDataParser.parseAniListEpisodes(
            apiEpisodes = media.episodes,
            statusStr = media.status ?: existing?.status,
            nextAiringEpisodeNumber = rawEp,
            nextAiringAtUnix = futureAiringAt,
            existingAired = existing?.currentEpisode ?: 0,
            nowSeconds = now,
            mediaId = media.id,
            title = media.title?.romaji ?: media.title?.userPreferred ?: bestEnglish,
            isDonghua = isDonghuaMedia,
            startYear = media.startDate?.year,
            startMonth = media.startDate?.month,
            startDay = media.startDate?.day
        )
        val airedCount = parsedInfo.airedEpisodeCount
        val nextEpToAir = parsedInfo.nextEpisodeNumber ?: (airedCount + 1)
        val resolvedTotalEps = parsedInfo.totalEpisodes

        if (existing != null) {
            val finalAiredCount = maxOf(existing.currentEpisode, airedCount)
            val finalNextEp = maxOf(existing.nextEpisodeNumber ?: nextEpToAir, nextEpToAir, finalAiredCount + 1)
            val finalTotal = if (resolvedTotalEps != null && resolvedTotalEps >= finalAiredCount) resolvedTotalEps else existing.totalEpisodes ?: resolvedTotalEps
            val resetWatched = if (existing.nextEpisodeNumber != null && finalNextEp > existing.nextEpisodeNumber) {
                false
            } else {
                existing.isWatched
            }

            val updatedTitleEnglish = bestEnglish ?: existing.titleEnglish
            val updatedTitleRomaji = media.title?.romaji ?: existing.titleRomaji

            val updated = existing.copy(
                titleRomaji = updatedTitleRomaji,
                titleEnglish = updatedTitleEnglish,
                titlePinyin = pinyinTitle ?: existing.titlePinyin,
                coverImage = media.coverImage?.extraLarge ?: media.coverImage?.large ?: existing.coverImage,
                bannerImage = media.bannerImage ?: existing.bannerImage,
                airingTimeUnix = futureAiringAt,
                nextEpisodeNumber = finalNextEp,
                currentEpisode = finalAiredCount,
                airingDayOfWeek = dayOfWeek,
                totalEpisodes = finalTotal,
                status = if (parsedInfo.isFinished) "FINISHED" else (media.status ?: existing.status),
                score = media.averageScore ?: existing.score,
                genres = if (media.genres?.isNotEmpty() == true) media.genres.joinToString(", ") else existing.genres,
                countryOfOrigin = country,
                isWatched = resetWatched
            )
            animeDao.updateAnime(updated)
        } else {
            val entity = mapAniListMediaToEntity(media).copy(
                titleRomaji = media.title?.romaji ?: media.title?.userPreferred ?: bestEnglish ?: "Untitled",
                titleEnglish = bestEnglish,
                titlePinyin = pinyinTitle,
                airingTimeUnix = futureAiringAt,
                nextEpisodeNumber = nextEpToAir,
                currentEpisode = airedCount,
                totalEpisodes = resolvedTotalEps,
                status = if (parsedInfo.isFinished) "FINISHED" else (media.status ?: "RELEASING"),
                airingDayOfWeek = dayOfWeek,
                countryOfOrigin = country,
                inWatchlist = false
            )
            animeDao.insertAnime(entity)
        }
    }

    suspend fun sanitizeAndFixEpisodeCounts() = withContext(Dispatchers.IO) {
        val allAnime = animeDao.getAllAnimeSync()
        val nowSeconds = System.currentTimeMillis() / 1000
        val seedMap = mapOf(
            21 to 1174,     // One Piece
            235 to 1209,    // Detective Conan
            130706 to 278,  // Perfect World (Wanmei Shijie)
            125638 to 212,  // Swallowed Star (Tunshi Xingkong)
            162982 to 164,  // Soul Land II: Peerless Tang Sect
            150020 to 212,  // Battle Through the Heavens (Doupo Cangqiong)
            118438 to 522,  // Martial Master (Wu Shen Zhu Zai)
            176123 to 132,  // A Will Eternal (Yi Nian Yong Heng)
            166532 to 152,  // Renegade Immortal (Xian Ni)
            146116 to 219,  // Throne of Seal (Shen Yin Wangzuo - Thursday)
            156324 to 170,  // Shrouding the Heavens (Zhe Tian)
            167389 to 80,   // Jade Dynasty (Zhu Xian)
            166258 to 152,  // Record of a Mortal's Journey to Immortality
            162800 to 110,  // The Great Ruler
            168200 to 95,   // Against the Gods
            169100 to 78,   // Stellar Transformations S6
            165000 to 240,  // 100,000 Years of Body Refining
            178789 to 8,    // Mushoku Tensei S3
            189046 to 13,   // Re:Zero S4
            195600 to 19,   // Daemons of the Shadow Realm
            182205 to 19,   // Slime S4
            196187 to 7,    // Smoking Behind Supermarket
            185874 to 4,    // Bleach TYBW
            184492 to 19,   // Iruma-kun S4
            2406 to 2790,   // Sazae-san
            6149 to 1500    // Chibi Maruko-chan
        )

        for (anime in allAnime) {
            val seedEp = seedMap[anime.id] ?: seedMap[anime.idMal]
            val titleFallback = resolveTitleBaseline(anime.titleRomaji, anime.titleEnglish)
            val resolvedSeed = seedEp ?: titleFallback
            val current = anime.currentEpisode
            val baseEp = maxOf(current, resolvedSeed ?: 0)
            val effectiveAiringTime = anime.getEffectiveAiringTime(nowSeconds) ?: anime.airingTimeUnix
            val liveAired = anime.copy(currentEpisode = baseEp, airingTimeUnix = effectiveAiringTime).getAiredEpisodeCount(nowSeconds)
            val targetEp = maxOf(current, baseEp, liveAired)

            val isFinished = anime.status?.uppercase() == "FINISHED" || anime.status == "finished_airing" || (anime.totalEpisodes != null && anime.totalEpisodes > 0 && targetEp >= anime.totalEpisodes)
            val fixedTotalEps = if (isFinished) {
                maxOf(anime.totalEpisodes ?: targetEp, targetEp)
            } else {
                anime.totalEpisodes
            }
            val newStatus = if (isFinished) "FINISHED" else anime.status
            val newNextEp = if (isFinished) targetEp else maxOf(anime.nextEpisodeNumber ?: (targetEp + 1), targetEp + 1)
            val resolvedEnglish = anime.titleEnglish?.trim()?.takeIf { it.isNotEmpty() }
                ?: EnglishTitleMapper.resolve(null, anime.titleRomaji)
            val needsTitleUpdate = !resolvedEnglish.isNullOrBlank() && resolvedEnglish != anime.titleEnglish

            if (targetEp != anime.currentEpisode || anime.nextEpisodeNumber == null || anime.nextEpisodeNumber != newNextEp || fixedTotalEps != anime.totalEpisodes || effectiveAiringTime != anime.airingTimeUnix || newStatus != anime.status || needsTitleUpdate) {
                val updated = anime.copy(
                    currentEpisode = targetEp,
                    nextEpisodeNumber = newNextEp,
                    airingTimeUnix = effectiveAiringTime,
                    totalEpisodes = fixedTotalEps,
                    status = newStatus,
                    titleEnglish = if (needsTitleUpdate) resolvedEnglish else anime.titleEnglish
                )
                animeDao.updateAnime(updated)
            }
        }
    }

    private fun resolveTitleBaseline(titleRomaji: String?, titleEnglish: String?): Int? {
        val t = ((titleRomaji ?: "") + " " + (titleEnglish ?: "")).lowercase()
        return when {
            t.contains("one piece") -> 1174
            t.contains("conan") -> 1209
            t.contains("perfect world") || t.contains("wanmei shijie") -> 278
            t.contains("swallowed star") || t.contains("tunshi xingkong") -> 212
            t.contains("soul land") || t.contains("douluo dalu") -> 164
            t.contains("battle through the heavens") || t.contains("doupo") -> 212
            t.contains("martial master") || t.contains("wu shen") -> 522
            t.contains("will eternal") || t.contains("yi nian yong heng") -> 132
            t.contains("renegade immortal") || t.contains("xian ni") -> 152
            t.contains("throne of seal") || t.contains("shen yin wangzuo") -> 219
            t.contains("shrouding the heavens") || t.contains("zhe tian") -> 170
            t.contains("jade dynasty") || t.contains("zhu xian") -> 80
            t.contains("record of a mortal") || t.contains("fanren") -> 152
            t.contains("great ruler") || t.contains("da zhu zai") -> 110
            t.contains("against the gods") || t.contains("ni tian xie shen") -> 95
            t.contains("stellar transformations") || t.contains("xing chen bian") -> 78
            t.contains("100,000 years") || t.contains("lian qi shi wan nian") -> 240
            t.contains("overshadowed") || t.contains("rakudai kenja") || t.contains("talentless sage") -> 9
            t.contains("supermarket") || t.contains("yani suu") -> 7
            else -> null
        }
    }

    private fun getComprehensiveOngoingSeedList(nowSeconds: Long): List<AnimeEntity> {
        return listOf(
            AnimeEntity(
                id = 99101, idMal = 36981,
                titleRomaji = "Mahou Shoujo Lyrical Nanoha EX: Detonation", titleEnglish = "Magical Girl Lyrical Nanoha EX",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx100780-eS34SThZ0k0U.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/100780-q54b1f4g1501.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 12, currentEpisode = 12,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 180000,
                nextEpisodeNumber = 13, genres = "Action, Fantasy, Mahou Shoujo", score = 76,
                synopsis = "Nanoha and her comrades face planetary crisis, unleashing dimensional magical armaments to safeguard Earth and the universe.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99102, idMal = 48291,
                titleRomaji = "Futsutsuka na Akujo dewa Gozaimasu ga", titleEnglish = "Though I Am an Inept Villainess",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx181977-L9L71O4z7K8m.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/181977-p2M912O4z.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 12, currentEpisode = 12,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 190000,
                nextEpisodeNumber = 13, genres = "Drama, Fantasy, Romance", score = 74,
                synopsis = "Kou Reirin swaps bodies with the court villainess Shu Keigetsu and navigates perilous palace intrigue with unwavering resilience.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99103, idMal = 51294,
                titleRomaji = "Oni no Hanayome", titleEnglish = "The Demon's Bride",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx172901-o9812M3N41L.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/172901-7xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 12, currentEpisode = 12,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 200000,
                nextEpisodeNumber = 13, genres = "Supernatural, Romance, Fantasy", score = 72,
                synopsis = "Unloved by her human family, Yuzu is chosen by the powerful demon lord Kishin as his destined bride, finding newfound devotion and love.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99104, idMal = 54201,
                titleRomaji = "Mushoku Tensei III: Isekai Ittara Honki Dasu", titleEnglish = "Mushoku Tensei: Jobless Reincarnation Season 3",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx178550-9xM821a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/178550-8xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 24, currentEpisode = 8,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 210000,
                nextEpisodeNumber = 9, genres = "Adventure, Drama, Fantasy", score = 88,
                synopsis = "Rudeus Greyrat enters the labyrinth to rescue Zenith alongside Paul and Roxy.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99105, idMal = 55102,
                titleRomaji = "Re:Zero kara Hajimeru Isekai Seikatsu 4th Season", titleEnglish = "Re:ZERO -Starting Life in Another World- Season 4",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx175900-5xN912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/175900-3xM912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 16, currentEpisode = 13,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 220000,
                nextEpisodeNumber = 14, genres = "Drama, Fantasy, Psychological", score = 87,
                synopsis = "Subaru Natsuki confronts the Witch Cult Sin Archbishops in the city of Priestella.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99106, idMal = 56203,
                titleRomaji = "Tensei Shitara Slime Datta Ken 4th Season", titleEnglish = "That Time I Got Reincarnated as a Slime Season 4",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx178901-4xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/178901-2xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 24, currentEpisode = 19,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 230000,
                nextEpisodeNumber = 20, genres = "Action, Adventure, Comedy, Fantasy", score = 84,
                synopsis = "Rimuru Tempest and the Jura Tempest Federation prepare for the Eastern Empire's invasion.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99107, idMal = 57304,
                titleRomaji = "Mairimashita! Iruma-kun 4", titleEnglish = "Welcome to Demon School! Iruma-kun Season 4",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx181200-1xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/181200-9xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 21, currentEpisode = 19,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 240000,
                nextEpisodeNumber = 20, genres = "Comedy, Fantasy, Supernatural", score = 82,
                synopsis = "Iruma Suzuki advances to second year in Babyls Demon School facing music festivals and fierce rivalries.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99108, idMal = 58405,
                titleRomaji = "Yomi no Tsugai", titleEnglish = "Daemons of the Shadow Realm",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx180500-3xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/180500-6xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 24, currentEpisode = 19,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 250000,
                nextEpisodeNumber = 20, genres = "Action, Supernatural", score = 83,
                synopsis = "Yuru uncovers dark secrets regarding his twin sister Asa and mystical Daemons separating day and night.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99109, idMal = 59506,
                titleRomaji = "Super no Ura de Yani Suu Futari", titleEnglish = "Smoking Behind the Supermarket with You",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx181000-7xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/181000-4xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 12, currentEpisode = 7,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 260000,
                nextEpisodeNumber = 8, genres = "Comedy, Romance, Slice of Life", score = 86,
                synopsis = "Overworked salaryman Sasaki finds comfort smoking behind the store with the mysterious, witty Tayama.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99110, idMal = 60607,
                titleRomaji = "BLEACH: Sennen Kessen-hen - Kashin-tan", titleEnglish = "BLEACH: Thousand-Year Blood War - The Calamity",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx171000-2xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/171000-5xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 13, currentEpisode = 4,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 270000,
                nextEpisodeNumber = 5, genres = "Action, Adventure, Supernatural", score = 89,
                synopsis = "Ichigo Kurosaki and the Soul Reapers defend the Soul King Palace against Yhwach and the Schutzstaffel.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99111, idMal = 61708,
                titleRomaji = "Katainaka no Ossan, Kensei ni Naru II", titleEnglish = "From Old Country Bumpkin to Master Swordsman Season 2",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx182000-8xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/182000-1xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 12, currentEpisode = 6,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 280000,
                nextEpisodeNumber = 7, genres = "Action, Fantasy", score = 80,
                synopsis = "Beryl Gardinand continues teaching royal knights while underestimating his supreme swordsmanship.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99112, idMal = 62809,
                titleRomaji = "Gaikotsu Kishi-sama, Tadaima Isekai e Odekakechuu II", titleEnglish = "Skeleton Knight in Another World Season 2",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx182100-9xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/182100-2xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 12, currentEpisode = 5,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 290000,
                nextEpisodeNumber = 6, genres = "Action, Fantasy", score = 78,
                synopsis = "Arc travels across kingdoms righting wrongs and freeing enslaved beastmen with Ponta and Ariane.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99113, idMal = 63910,
                titleRomaji = "Tsuihou Sareta Tensei Juukishi wa Game Chishiki de Musou Suru", titleEnglish = "The Exiled Heavy Knight Knows How to Game the System",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx182200-4xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/182200-7xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 12, currentEpisode = 8,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 300000,
                nextEpisodeNumber = 9, genres = "Action, Adventure, Fantasy", score = 81,
                synopsis = "Elma uses max-stat gaming secrets to turn the discarded Heavy Knight class into an unstoppable juggernaut.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99114, idMal = 64111,
                titleRomaji = "Kimetsu no Yaiba: Mugen Jou-hen", titleEnglish = "Demon Slayer: Kimetsu no Yaiba - Infinity Castle",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx179000-6xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/179000-8xN912a.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = 12, currentEpisode = 3,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 310000,
                nextEpisodeNumber = 4, genres = "Action, Supernatural", score = 91,
                synopsis = "The Demon Slayer Corps plunges into the shifting Infinity Castle for the final war against Muzan Kibutsuji.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99115, idMal = 65212,
                titleRomaji = "Chibi Maruko-chan", titleEnglish = "Chibi Maruko-chan",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx2153-2S8P9L.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/2153-M8O9L.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 1500,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 320000,
                nextEpisodeNumber = 1501, genres = "Comedy, Slice of Life", score = 79,
                synopsis = "Heartwarming everyday comedic adventures of young Maruko and her family in 1970s Japan.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 99116, idMal = 66313,
                titleRomaji = "Sazae-san", titleEnglish = "Sazae-san",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx2406-1S8P9L.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/2406-M8O9L.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 2790,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 330000,
                nextEpisodeNumber = 2791, genres = "Comedy, Slice of Life", score = 77,
                synopsis = "Japan's longest-running animated show portraying traditional, loving family life in suburban Tokyo.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 21, idMal = 21,
                titleRomaji = "ONE PIECE", titleEnglish = "ONE PIECE",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/nx21-A233R1.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-M8O9L.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 1174,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 200000,
                nextEpisodeNumber = 1175, genres = "Action, Adventure, Comedy, Fantasy", score = 88,
                synopsis = "Monkey D. Luffy sets sail with his crew to find the legendary treasure One Piece and become King of the Pirates.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 150020, idMal = 52194,
                titleRomaji = "Doupo Cangqiong: Nian Fan", titleEnglish = "Battle Through The Heavens",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx150020-8xN912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/150020-1xN81.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 212,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 220000,
                nextEpisodeNumber = 213, genres = "Action, Fantasy, Donghua", score = 87,
                synopsis = "Xiao Yan masters heavenly flames and battles through the Dou Qi Continent to fulfill his 3-year agreement and protect his clan.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 118438, idMal = 42132,
                titleRomaji = "Wu Shen Zhu Zai", titleEnglish = "Martial Master",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx118438-6xM82.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/118438-2xM11.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 522,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 100000,
                nextEpisodeNumber = 523, genres = "Action, Fantasy, Donghua", score = 83,
                synopsis = "Betrayed by his friend and lover, Qin Chen reincarnates 300 years later in a illegitimate noble body to reclaim his martial glory.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 166258, idMal = 55801,
                titleRomaji = "Fanren Xiu Xian Chuan", titleEnglish = "Record of a Mortal's Journey to Immortality",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx166258-4xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/166258-5xN81.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 152,
                watchedEpisodeCount = 0, airingDayOfWeek = 7, airingTimeUnix = nowSeconds + 210000,
                nextEpisodeNumber = 153, genres = "Action, Adventure, Fantasy, Donghua", score = 89,
                synopsis = "Han Li, a poor mortal boy, relies on cautious wisdom and a green vial to ascend the treacherous mountain of cultivation.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 235, idMal = 235,
                titleRomaji = "Meitantei Conan", titleEnglish = "Detective Conan",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx235-0S8P9L.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/235-M8O9L.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 1209,
                watchedEpisodeCount = 0, airingDayOfWeek = 6, airingTimeUnix = nowSeconds + 90000,
                nextEpisodeNumber = 1210, genres = "Adventure, Comedy, Mystery", score = 82,
                synopsis = "High school detective Shinichi Kudo is turned into a child by a poison and solves crimes secretly as Conan Edogawa.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "JP"
            ),
            AnimeEntity(
                id = 130706, idMal = 48312,
                titleRomaji = "Wanmei Shijie", titleEnglish = "Perfect World",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx130706-N96L7e0q8T8k.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/130706-9x8M2P1.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 278,
                watchedEpisodeCount = 0, airingDayOfWeek = 5, airingTimeUnix = nowSeconds + 180000,
                nextEpisodeNumber = 279, genres = "Action, Adventure, Fantasy, Donghua", score = 85,
                synopsis = "Born in a world filled with ancient gods and magical beasts, Shi Hao rises from a small village to conquer the heavens.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 125638, idMal = 43232,
                titleRomaji = "Tunshi Xingkong", titleEnglish = "Swallowed Star",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx125638-3x8N9L0q1W.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/125638-8xN92.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 212,
                watchedEpisodeCount = 0, airingDayOfWeek = 3, airingTimeUnix = nowSeconds + 120000,
                nextEpisodeNumber = 213, genres = "Action, Sci-Fi, Donghua", score = 86,
                synopsis = "In a post-apocalyptic future overrun by mutated beasts, Luo Feng awakens fighter powers to protect Earth and reach cosmic realms.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 162982, idMal = 54812,
                titleRomaji = "Douluo Dalu II: Jueshi Tangmen", titleEnglish = "Soul Land II: Peerless Tang Sect",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx162982-9xM8P2a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/162982-7xN21.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 164,
                watchedEpisodeCount = 0, airingDayOfWeek = 6, airingTimeUnix = nowSeconds + 150000,
                nextEpisodeNumber = 165, genres = "Action, Fantasy, Donghua", score = 84,
                synopsis = "Ten thousand years after Tang San, the Tang Sect declines. Huo Yuhao inherits the spirit eyes and rebuilds the legendary sect.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 176123, idMal = 58123,
                titleRomaji = "Yi Nian Yong Heng 3", titleEnglish = "A Will Eternal Season 3",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx176123-5xM92a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/176123-9xN11.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 132,
                watchedEpisodeCount = 0, airingDayOfWeek = 3, airingTimeUnix = nowSeconds + 140000,
                nextEpisodeNumber = 133, genres = "Action, Comedy, Fantasy, Donghua", score = 86,
                synopsis = "Bai Xiaochun seeks immortality through hilarious, cowardly, and brilliant cultivation adventures in the Spirit Stream Sect.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 166532, idMal = 55902,
                titleRomaji = "Xian Ni", titleEnglish = "Renegade Immortal",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx166532-7xM812a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/166532-4xN92.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 152,
                watchedEpisodeCount = 0, airingDayOfWeek = 1, airingTimeUnix = nowSeconds + 80000,
                nextEpisodeNumber = 153, genres = "Action, Fantasy, Donghua", score = 88,
                synopsis = "Wang Lin, an ordinary boy with mediocre talent, defies fate and walks the ruthless path of a renegade immortal.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 146116, idMal = 51201,
                titleRomaji = "Shen Yin Wangzuo", titleEnglish = "Throne of Seal",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx146116-3xN91a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/146116-1xM22.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 219,
                watchedEpisodeCount = 0, airingDayOfWeek = 4, airingTimeUnix = nowSeconds + 190000,
                nextEpisodeNumber = 220, genres = "Action, Fantasy, Donghua", score = 85,
                synopsis = "Long Hao Chen joins the Knight Temple to save humanity from 72 Demon Gods and claim the Divine Throne of Eternity.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 156324, idMal = 53901,
                titleRomaji = "Zhe Tian", titleEnglish = "Shrouding the Heavens",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx156324-1xM82a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/156324-8xN91.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 170,
                watchedEpisodeCount = 0, airingDayOfWeek = 3, airingTimeUnix = nowSeconds + 160000,
                nextEpisodeNumber = 171, genres = "Action, Fantasy, Donghua", score = 84,
                synopsis = "Nine dragon coffins pull Ye Fan from Earth into a vast ancient cosmos of immortal cultivation and celestial battles.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            ),
            AnimeEntity(
                id = 167389, idMal = 56123,
                titleRomaji = "Zhu Xian 2", titleEnglish = "Jade Dynasty Season 2",
                coverImage = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx167389-2xM912a.jpg",
                bannerImage = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/167389-3xN81.jpg",
                format = "TV", status = "RELEASING", totalEpisodes = null, currentEpisode = 80,
                watchedEpisodeCount = 0, airingDayOfWeek = 6, airingTimeUnix = nowSeconds + 170000,
                nextEpisodeNumber = 81, genres = "Action, Fantasy, Donghua", score = 85,
                synopsis = "Zhang Xiaofan practices dual righteous and demonic techniques while searching for a way to awaken Bi Yao.",
                inWatchlist = false, isWatched = false, countryOfOrigin = "CN"
            )
        )
    }

    suspend fun ensureAllOngoingSeeded() = withContext(Dispatchers.IO) {
        val nowSeconds = System.currentTimeMillis() / 1000
        val ongoingList = getComprehensiveOngoingSeedList(nowSeconds)
        for (seed in ongoingList) {
            val existing = animeDao.getAnimeByIdSync(seed.id)
            if (existing == null) {
                animeDao.insertAnime(seed)
            } else {
                val needsImageFix = existing.coverImage.contains("unsplash", ignoreCase = true)
                val updatedImage = if (needsImageFix) seed.coverImage else existing.coverImage
                val updatedBanner = if (existing.bannerImage?.contains("unsplash", ignoreCase = true) == true) seed.bannerImage else existing.bannerImage
                val updatedTitleEnglish = if (existing.titleEnglish.isNullOrBlank() || existing.titleEnglish == existing.titleRomaji) seed.titleEnglish ?: existing.titleEnglish else existing.titleEnglish
                val updatedCurrentEp = maxOf(existing.currentEpisode, seed.currentEpisode)
                val updatedNextEp = maxOf(existing.nextEpisodeNumber ?: (updatedCurrentEp + 1), updatedCurrentEp + 1)
                val updated = existing.copy(
                    coverImage = updatedImage,
                    bannerImage = updatedBanner,
                    titleEnglish = updatedTitleEnglish,
                    currentEpisode = updatedCurrentEp,
                    nextEpisodeNumber = updatedNextEp,
                    countryOfOrigin = seed.countryOfOrigin,
                    genres = if (existing.genres.contains("Donghua", true) || seed.genres.contains("Donghua", true)) {
                        if (!existing.genres.contains("Donghua", true)) "${existing.genres}, Donghua" else existing.genres
                    } else existing.genres
                )
                animeDao.updateAnime(updated)
            }
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        ensureAllOngoingSeeded()
    }

    suspend fun fetchAniListFullDetails(animeId: Int): Result<AniListMedia> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.querySingleMedia(
                AniListRequest(
                    query = AniListQueries.MEDIA_DETAILS_QUERY,
                    variables = mapOf("id" to animeId)
                )
            )
            if (response.isSuccessful) {
                val media = response.body()?.data?.Media
                if (media != null) {
                    val existing = animeDao.getAnimeByIdSync(animeId)
                    if (existing != null) {
                        val studioName = media.studios?.nodes?.firstOrNull()?.name
                        val genresStr = media.genres?.joinToString(", ") ?: existing.genres
                        val enrichedGenres = if (!studioName.isNullOrBlank() && !genresStr.contains(studioName)) {
                            "$genresStr, Studio: $studioName"
                        } else genresStr
                        val updated = existing.copy(
                            synopsis = media.description?.replace(Regex("<[^>]*>"), "")?.trim() ?: existing.synopsis,
                            score = media.averageScore ?: existing.score,
                            totalEpisodes = media.episodes ?: existing.totalEpisodes,
                            genres = enrichedGenres,
                            lastCachedAt = System.currentTimeMillis()
                        )
                        animeDao.updateAnime(updated)
                    }
                    Result.success(media)
                } else {
                    Result.failure(Exception("Anime details not found on AniList"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: Failed to fetch details from AniList"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveEnglishTitle(title: AniListTitle?, synonyms: List<String>?): String? {
        val apiEnglish = title?.english?.trim()?.takeIf { it.isNotEmpty() }
        if (apiEnglish != null) {
            return EnglishTitleMapper.resolve(apiEnglish, title?.romaji, synonyms) ?: apiEnglish
        }
        return EnglishTitleMapper.resolve(null, title?.romaji ?: title?.userPreferred, synonyms)
    }

    private fun mapAniListMediaToEntity(media: AniListMedia): AnimeEntity {
        val nextEp = media.nextAiringEpisode
        val airingAt = nextEp?.airingAt ?: 0L
        val dayOfWeek = if (airingAt > 0L) {
            Instant.ofEpochSecond(airingAt)
                .atZone(ZoneId.systemDefault())
                .dayOfWeek
                .value
        } else {
            // Default based on ID hash for uniform distribution across 1..7 rather than all Monday
            ((media.id.coerceAtLeast(1) % 7) + 1)
        }
        val cleanSynopsis = media.description
            ?.replace(Regex("<[^>]*>"), "") // Strip HTML tags
            ?.trim()

        val bestEnglish = resolveEnglishTitle(media.title, media.synonyms)
        val country = media.countryOfOrigin ?: if (media.genres?.any { it.contains("Donghua", true) || it.contains("Chinese", true) } == true) "CN" else "JP"
        val pinyinTitle = if (country == "CN") media.title?.romaji else null
        val isDonghuaMedia = country == "CN" || media.genres?.any { it.contains("Donghua", true) || it.contains("Chinese", true) } == true

        val parsedInfo = EpisodeDataParser.parseAniListEpisodes(
            apiEpisodes = media.episodes,
            statusStr = media.status,
            nextAiringEpisodeNumber = nextEp?.episode,
            nextAiringAtUnix = nextEp?.airingAt,
            existingAired = 0,
            nowSeconds = System.currentTimeMillis() / 1000,
            mediaId = media.id,
            title = media.title?.romaji ?: media.title?.userPreferred ?: bestEnglish ?: "Untitled",
            isDonghua = isDonghuaMedia,
            startYear = media.startDate?.year,
            startMonth = media.startDate?.month,
            startDay = media.startDate?.day
        )
        val currentEpNum = parsedInfo.airedEpisodeCount
        val nextEpNum = parsedInfo.nextEpisodeNumber ?: (currentEpNum + 1)

        return AnimeEntity(
            id = media.id,
            idMal = media.idMal,
            titleRomaji = media.title?.romaji ?: media.title?.userPreferred ?: bestEnglish ?: "Untitled",
            titleEnglish = bestEnglish,
            titleNative = media.title?.native,
            titlePinyin = pinyinTitle,
            coverImage = media.coverImage?.extraLarge ?: media.coverImage?.large ?: "",
            bannerImage = media.bannerImage,
            format = media.format ?: "TV",
            status = if (parsedInfo.isFinished) "FINISHED" else (media.status ?: "RELEASING"),
            totalEpisodes = parsedInfo.totalEpisodes ?: media.episodes,
            currentEpisode = currentEpNum,
            airingDayOfWeek = dayOfWeek,
            airingTimeUnix = nextEp?.airingAt,
            nextEpisodeNumber = nextEpNum,
            genres = media.genres?.joinToString(", ") ?: "",
            season = media.season,
            seasonYear = media.seasonYear,
            score = media.averageScore,
            synopsis = cleanSynopsis,
            countryOfOrigin = country,
            inWatchlist = false,
            isFavorite = false,
            notificationsEnabled = true,
            isWatched = false,
            lastCachedAt = System.currentTimeMillis()
        )
    }
}

data class AniListSearchResult(
    val items: List<AnimeEntity>,
    val hasNextPage: Boolean = false,
    val total: Int? = null
)
