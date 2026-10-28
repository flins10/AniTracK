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
        private const val KEY_LOCAL_VIDEO_RESUME_PREFIX = "local_video_pos_"
        private const val KEY_LOCAL_VIDEO_HISTORY = "local_video_recent_history_v1"
        private const val KEY_SKIP_INTERVAL_SEC = "pref_skip_interval_sec"
        private const val KEY_VOLUME = "pref_player_volume"
        private const val KEY_SPEED = "pref_playback_speed"
        private const val MIN_RESUME_THRESHOLD_MS = 3000L // Resume if watched >= 3s
        private const val NEAR_END_THRESHOLD_MS = 15000L  // Reset if within 15s of end
        private const val MAX_LOCAL_HISTORY_ITEMS = 25
    }

    /**
     * Represents a recently played local device video file (stored separately from anime history).
     */
    data class LocalVideoHistoryItem(
        val uriString: String,
        val displayName: String,
        val durationMs: Long,
        val lastPositionMs: Long,
        val lastPlayedAt: Long,
        val mimeType: String? = null
    )

    /**
     * Preferred skip interval in seconds (e.g., 5, 10, 15, 30, 85).
     */
    var skipIncrementSeconds: Int
        get() = prefs.getInt(KEY_SKIP_INTERVAL_SEC, 10).coerceIn(5, 90)
        set(value) {
            val coerced = value.coerceIn(5, 90)
            prefs.edit().putInt(KEY_SKIP_INTERVAL_SEC, coerced).apply()
            ioScope.launch {
                playbackPreferenceDao?.upsertPreference(
                    PlaybackPreferenceEntity(prefKey = KEY_SKIP_INTERVAL_SEC, prefValue = coerced.toString())
                )
            }
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
     * Gets saved playback position for a local device video URI (completely separate from anime watch history).
     */
    fun getLocalVideoSavedPositionMs(uriString: String, durationMs: Long = 0L): Long {
        val key = "$KEY_LOCAL_VIDEO_RESUME_PREFIX${uriString.hashCode()}"
        val pos = prefs.getLong(key, 0L)
        if (pos < MIN_RESUME_THRESHOLD_MS) return 0L
        if (durationMs > 30_000L && pos >= (durationMs - 10_000L)) {
            return 0L
        }
        return pos
    }

    /**
     * Saves progress and updates the Recently Played Local Videos list (separate from anime history).
     */
    fun saveLocalVideoProgress(
        uriString: String,
        displayName: String,
        positionMs: Long,
        durationMs: Long,
        mimeType: String? = null
    ) {
        if (uriString.isBlank()) return
        val posKey = "$KEY_LOCAL_VIDEO_RESUME_PREFIX${uriString.hashCode()}"
        val safePos = positionMs.coerceAtLeast(0L)
        val safeDur = durationMs.coerceAtLeast(0L)
        prefs.edit().putLong(posKey, safePos).apply()

        val currentList = getRecentLocalVideos().toMutableList()
        val existingIndex = currentList.indexOfFirst { it.uriString == uriString }
        val existingDur = if (existingIndex >= 0) currentList[existingIndex].durationMs else 0L
        val updatedItem = LocalVideoHistoryItem(
            uriString = uriString,
            displayName = displayName.ifBlank { "Local Video" },
            durationMs = if (safeDur > 0L) safeDur else existingDur,
            lastPositionMs = safePos,
            lastPlayedAt = System.currentTimeMillis(),
            mimeType = mimeType ?: currentList.getOrNull(existingIndex)?.mimeType
        )
        if (existingIndex >= 0) {
            currentList.removeAt(existingIndex)
        }
        currentList.add(0, updatedItem)
        val trimmed = currentList.take(MAX_LOCAL_HISTORY_ITEMS)
        persistLocalVideoHistoryList(trimmed)
    }

    /**
     * Returns the list of recently played local device videos ordered by most recently played.
     */
    fun getRecentLocalVideos(): List<LocalVideoHistoryItem> {
        val raw = prefs.getString(KEY_LOCAL_VIDEO_HISTORY, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size >= 5) {
                    val uri = parts[0]
                    val name = parts[1]
                    val dur = parts[2].toLongOrNull() ?: 0L
                    val pos = parts[3].toLongOrNull() ?: 0L
                    val playedAt = parts[4].toLongOrNull() ?: 0L
                    val mime = parts.getOrNull(5)?.takeIf { it.isNotBlank() }
                    if (uri.isNotBlank()) {
                        LocalVideoHistoryItem(
                            uriString = uri,
                            displayName = name,
                            durationMs = dur,
                            lastPositionMs = pos,
                            lastPlayedAt = playedAt,
                            mimeType = mime
                        )
                    } else null
                } else null
            }
            .toList()
    }

    /**
     * Removes a single local video from the recent local videos list.
     */
    fun removeLocalVideoFromHistory(uriString: String) {
        val posKey = "$KEY_LOCAL_VIDEO_RESUME_PREFIX${uriString.hashCode()}"
        prefs.edit().remove(posKey).apply()
        val updated = getRecentLocalVideos().filterNot { it.uriString == uriString }
        persistLocalVideoHistoryList(updated)
    }

    /**
     * Clears all local video history without touching anime watch history.
     */
    fun clearLocalVideoHistory() {
        persistLocalVideoHistoryList(emptyList())
    }

    private fun persistLocalVideoHistoryList(items: List<LocalVideoHistoryItem>) {
        val serialized = items.joinToString("\n") { item ->
            val safeName = item.displayName.replace("\t", " ").replace("\n", " ")
            val safeMime = (item.mimeType ?: "").replace("\t", " ").replace("\n", " ")
            "${item.uriString}\t$safeName\t${item.durationMs}\t${item.lastPositionMs}\t${item.lastPlayedAt}\t$safeMime"
        }
        prefs.edit().putString(KEY_LOCAL_VIDEO_HISTORY, serialized).apply()
        ioScope.launch {
            playbackPreferenceDao?.upsertPreference(
                PlaybackPreferenceEntity(prefKey = KEY_LOCAL_VIDEO_HISTORY, prefValue = serialized)
            )
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
