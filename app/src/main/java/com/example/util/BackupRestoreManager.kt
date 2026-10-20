package com.example.util

import com.example.data.local.AnimeDao
import com.example.data.local.DeletedAnimeDao
import com.example.data.local.EpisodeHistoryDao
import com.example.data.local.PlaybackPreferenceDao
import com.example.data.local.PlaybackPreferenceEntity
import com.example.data.local.SavedAnimeDao
import com.example.data.local.SavedAnimeEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder

data class BackupSummary(
    val importedAnimeCount: Int,
    val importedHistoryCount: Int,
    val importedPreferencesCount: Int
)

data class BackupJsonModel(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val savedAnime: List<SavedAnimeEntity> = emptyList(),
    val playbackPreferences: List<PlaybackPreferenceEntity> = emptyList()
)

object BackupRestoreManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun isSafePreferenceKey(key: String): Boolean {
        return key.startsWith("pref_") ||
                key.startsWith("resume_pos_") ||
                key.startsWith("app_setting_") ||
                key.contains("theme") ||
                key.contains("subtitle") ||
                key.contains("player")
    }

    suspend fun exportBackupJson(
        animeDao: AnimeDao,
        savedAnimeDao: SavedAnimeDao,
        historyDao: EpisodeHistoryDao,
        deletedAnimeDao: DeletedAnimeDao?,
        playbackPreferenceDao: PlaybackPreferenceDao?
    ): String {
        val savedAnimeList = savedAnimeDao.getAllSavedAnimeSync()
        val prefs = playbackPreferenceDao?.getAllPreferencesSync() ?: emptyList()
        val backup = BackupJsonModel(
            savedAnime = savedAnimeList,
            playbackPreferences = prefs
        )
        return gson.toJson(backup)
    }

    suspend fun importBackupJson(
        jsonString: String,
        animeDao: AnimeDao,
        savedAnimeDao: SavedAnimeDao,
        historyDao: EpisodeHistoryDao,
        deletedAnimeDao: DeletedAnimeDao?,
        playbackPreferenceDao: PlaybackPreferenceDao?
    ): Result<BackupSummary> {
        return try {
            val backup = gson.fromJson(jsonString, BackupJsonModel::class.java)
                ?: return Result.failure(IllegalArgumentException("Invalid JSON schema"))

            if (backup.savedAnime.isNotEmpty()) {
                savedAnimeDao.upsertSavedAnimeList(backup.savedAnime)
            }

            if (backup.playbackPreferences.isNotEmpty() && playbackPreferenceDao != null) {
                playbackPreferenceDao.upsertPreferences(backup.playbackPreferences)
            }

            Result.success(
                BackupSummary(
                    importedAnimeCount = backup.savedAnime.size,
                    importedHistoryCount = 0,
                    importedPreferencesCount = backup.playbackPreferences.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
