package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.PlaybackPreferenceDao
import com.example.data.local.PlaybackPreferenceEntity
import com.example.data.remote.ArchiveFileItem
import com.example.data.remote.InternetArchiveApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Repository responsible for managing playback sessions, resume offsets,
 * stream fallback resolution, and player settings.
 *
 * Persists playback positions and preferences in both SharedPreferences and durable Room SQLite
 * (`PlaybackPreferenceDao`) so settings and resume positions survive cache clears and APK updates.
 */
class PlaybackRepository(
    context: Context,
    private val playbackPreferenceDao: PlaybackPreferenceDao? = null,
    private val videoMatchService: VideoMatchService = VideoMatchService()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val PREFS_NAME = "archive_playback_prefs"
        private const val KEY_RESUME_PREFIX = "resume_pos_"
        private const val KEY_VOLUME = "pref_player_volume"
        private const val KEY_SPEED = "pref_playback_speed"
        private const val MIN_RESUME_THRESHOLD_MS = 5000L // Don't resume if watched < 5s
        private const val NEAR_END_THRESHOLD_MS = 30000L  // Reset if finished
    }

    /**
     * Gets saved playback position in milliseconds. Returns 0 if watched near the end or start.
     */
    fun getSavedPositionMs(animeId: Int, episodeNumber: Int, durationMs: Long = 0L): Long {
        val key = "$KEY_RESUME_PREFIX${animeId}_ep$episodeNumber"
        val pos = prefs.getLong(key, 0L)
        if (pos < MIN_RESUME_THRESHOLD_MS) return 0L
        if (durationMs > 0 && pos >= (durationMs - NEAR_END_THRESHOLD_MS)) {
            return 0L
        }
        return pos
    }

    /**
     * Saves the current playback position in milliseconds by animeId and episodeNumber.
     */
    fun savePositionMs(animeId: Int, episodeNumber: Int, positionMs: Long) {
        val key = "$KEY_RESUME_PREFIX${animeId}_ep$episodeNumber"
        prefs.edit().putLong(key, positionMs).apply()
        ioScope.launch {
            playbackPreferenceDao?.upsertPreference(
                PlaybackPreferenceEntity(prefKey = key, prefValue = positionMs.toString())
            )
        }
    }

    /**
     * Saves playback position for a stable media or Internet Archive key directly to Room and SharedPreferences.
     */
    suspend fun savePosition(mediaKey: String, positionMs: Long) {
        val key = "$KEY_RESUME_PREFIX$mediaKey"
        prefs.edit().putLong(key, positionMs).apply()
        playbackPreferenceDao?.upsertPreference(
            PlaybackPreferenceEntity(prefKey = key, prefValue = positionMs.toString())
        )
    }

    /**
     * Saves preferred playback speed to Room and SharedPreferences.
     */
    suspend fun savePlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_SPEED, speed).apply()
        playbackPreferenceDao?.upsertPreference(
            PlaybackPreferenceEntity(prefKey = KEY_SPEED, prefValue = speed.toString())
        )
    }

    /**
     * Clears resume position once an episode is marked as completed.
     */
    fun clearResumePosition(animeId: Int, episodeNumber: Int) {
        val key = "$KEY_RESUME_PREFIX${animeId}_ep$episodeNumber"
        prefs.edit().remove(key).apply()
        ioScope.launch {
            playbackPreferenceDao?.deletePreference(key)
        }
    }

    /**
     * Provides an alternate compatible file from the same candidate if the current file fails during playback.
     */
    fun getFallbackStream(
        identifier: String,
        allCompatibleFiles: List<ArchiveFileItem>,
        failedFileName: String,
        requestedEpisode: Int? = null
    ): Pair<ArchiveFileItem, String>? {
        val nextFile = videoMatchService.findAlternativePlayableFile(
            allCompatible = allCompatibleFiles,
            failedFileName = failedFileName,
            requestedEpisode = requestedEpisode
        ) ?: return null
        val directUrl = InternetArchiveApiService.getDirectDownloadUrl(identifier, nextFile.name)
        return Pair(nextFile, directUrl)
    }

    var isPlayerMuted: Boolean
        get() = prefs.getBoolean("player_muted", false)
        set(value) {
            prefs.edit().putBoolean("player_muted", value).apply()
            ioScope.launch {
                playbackPreferenceDao?.upsertPreference(
                    PlaybackPreferenceEntity(prefKey = "player_muted", prefValue = value.toString())
                )
            }
        }
}
