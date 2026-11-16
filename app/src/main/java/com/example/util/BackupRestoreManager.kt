package com.example.util

import com.example.data.local.AnimeDao
import com.example.data.local.DeletedAnimeDao
import com.example.data.local.EpisodeHistoryDao
import com.example.data.local.SavedAnimeDao
import com.example.data.local.SavedAnimeEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder

data class BackupSummary(
    val importedAnimeCount: Int,
    val importedHistoryCount: Int
)

data class BackupJsonModel(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val savedAnime: List<SavedAnimeEntity> = emptyList()
)

object BackupRestoreManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportBackupJson(
        animeDao: AnimeDao,
        savedAnimeDao: SavedAnimeDao,
        historyDao: EpisodeHistoryDao,
        deletedAnimeDao: DeletedAnimeDao?
    ): String {
        val savedAnimeList = savedAnimeDao.getAllSavedAnimeSync()
        val backup = BackupJsonModel(savedAnime = savedAnimeList)
        return gson.toJson(backup)
    }

    suspend fun importBackupJson(
        jsonString: String,
        animeDao: AnimeDao,
        savedAnimeDao: SavedAnimeDao,
        historyDao: EpisodeHistoryDao,
        deletedAnimeDao: DeletedAnimeDao?
    ): Result<BackupSummary> {
        return try {
            val backup = gson.fromJson(jsonString, BackupJsonModel::class.java)
                ?: return Result.failure(IllegalArgumentException("Invalid backup JSON format"))

            if (backup.savedAnime.isNotEmpty()) {
                savedAnimeDao.upsertSavedAnimeList(backup.savedAnime)
            }

            Result.success(
                BackupSummary(
                    importedAnimeCount = backup.savedAnime.size,
                    importedHistoryCount = 0
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
