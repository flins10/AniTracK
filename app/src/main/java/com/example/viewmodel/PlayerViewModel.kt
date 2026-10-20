package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.local.AnimeEntity
import com.example.data.local.AppDatabase
import com.example.data.remote.ArchiveFileItem
import com.example.data.remote.InternetArchiveApiService
import com.example.data.repository.AnimeRepository
import com.example.data.repository.ArchiveAnimeMatchResult
import com.example.data.repository.ArchiveCandidateMatch
import com.example.data.repository.ArchivePlayableMedia
import com.example.data.repository.AnimeSeasonInfo
import com.example.data.repository.InternetArchiveRepository
import com.example.data.repository.PlaybackRepository
import com.example.data.repository.SubtitleRepository
import com.example.data.repository.SubtitleSource
import com.example.data.repository.SubtitleStyleConfig
import com.example.data.repository.SubtitleTrackItem
import com.example.data.repository.VideoQualityOption
import com.example.ui.components.PlayerAudioTrack
import com.example.ui.components.PlayerSubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private const val TAG = "AniTrackPlayerVM"

sealed class PlayerUiState {
    object Idle : PlayerUiState()
    data class Loading(val message: String = "") : PlayerUiState()
    data class Ready(
        val activeMedia: ArchivePlayableMedia,
        val activeCandidate: ArchiveCandidateMatch,
        val allCandidates: List<ArchiveCandidateMatch>,
        val initialResumeMs: Long
    ) : PlayerUiState()
    data class NoMatchFound(
        val reason: String,
        val queriedTitles: List<String>,
        val requestedEpisode: Int?
    ) : PlayerUiState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val hasFallbackFile: Boolean = false
    ) : PlayerUiState()
}

/**
 * Dedicated ViewModel holding the ExoPlayer instance across rotations and configuration changes,
 * managing speed 1.0x-4.0x, quality switching, audio & subtitle tracks, and persistence.
 */
@OptIn(UnstableApi::class)
class PlayerViewModel @JvmOverloads constructor(
    application: Application,
    private val animeRepository: AnimeRepository = AnimeRepository(
        animeDao = AppDatabase.getInstance(application).animeDao(),
        historyDao = AppDatabase.getInstance(application).episodeHistoryDao(),
        apiCacheDao = AppDatabase.getInstance(application).apiCacheDao(),
        deletedAnimeDao = AppDatabase.getInstance(application).deletedAnimeDao(),
        savedAnimeDao = AppDatabase.getInstance(application).savedAnimeDao(),
        playbackPreferenceDao = AppDatabase.getInstance(application).playbackPreferenceDao()
    ),
    private val archiveRepository: InternetArchiveRepository = InternetArchiveRepository(),
    private val playbackRepository: PlaybackRepository = PlaybackRepository(
        context = application,
        playbackPreferenceDao = AppDatabase.getInstance(application).playbackPreferenceDao()
    ),
    private val subtitleRepository: SubtitleRepository = SubtitleRepository()
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("archive_player_prefs", Context.MODE_PRIVATE)

    // Player Core Engine
    val trackSelector: DefaultTrackSelector by lazy { DefaultTrackSelector(application) }

    val exoPlayer: ExoPlayer by lazy {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AniTrackPlayer/1.0 (Linux; Android; Media3 ExoPlayer)")
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(application)
            .setDataSourceFactory(httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(application)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true /* handleAudioFocus */)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = true
                val savedSpeed = prefs.getFloat("ia_player_speed", 1.0f)
                setPlaybackSpeed(savedSpeed)
            }
    }

    // State Flows
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _currentAnime = MutableStateFlow<AnimeEntity?>(null)
    val currentAnime: StateFlow<AnimeEntity?> = _currentAnime.asStateFlow()

    private val _currentEpisode = MutableStateFlow(1)
    val currentEpisode: StateFlow<Int> = _currentEpisode.asStateFlow()

    private val _currentSeasonNumber = MutableStateFlow(1)
    val currentSeasonNumber: StateFlow<Int> = _currentSeasonNumber.asStateFlow()

    private val _availableSeasons = MutableStateFlow<List<AnimeSeasonInfo>>(emptyList())
    val availableSeasons: StateFlow<List<AnimeSeasonInfo>> = _availableSeasons.asStateFlow()

    fun setAvailableSeasons(seasons: List<AnimeSeasonInfo>) {
        if (seasons.isNotEmpty()) {
            _availableSeasons.value = seasons
        }
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(true)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(prefs.getFloat("ia_player_speed", 1.0f))
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _resizeMode = MutableStateFlow(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffsetX = MutableStateFlow(0f)
    val panOffsetX: StateFlow<Float> = _panOffsetX.asStateFlow()

    private val _panOffsetY = MutableStateFlow(0f)
    val panOffsetY: StateFlow<Float> = _panOffsetY.asStateFlow()

    private val _activeQuality = MutableStateFlow<VideoQualityOption?>(null)
    val activeQuality: StateFlow<VideoQualityOption?> = _activeQuality.asStateFlow()

    private val _availableQualities = MutableStateFlow<List<VideoQualityOption>>(emptyList())
    val availableQualities: StateFlow<List<VideoQualityOption>> = _availableQualities.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<PlayerAudioTrack>>(emptyList())
    val audioTracks: StateFlow<List<PlayerAudioTrack>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<PlayerSubtitleTrack>>(emptyList())
    val subtitleTracks: StateFlow<List<PlayerSubtitleTrack>> = _subtitleTracks.asStateFlow()

    private val _isSubtitleOff = MutableStateFlow(prefs.getBoolean("ia_subtitles_off", false))
    val isSubtitleOff: StateFlow<Boolean> = _isSubtitleOff.asStateFlow()

    private val _subtitleStyle = MutableStateFlow(SubtitleStyleConfig())
    val subtitleStyle: StateFlow<SubtitleStyleConfig> = _subtitleStyle.asStateFlow()

    private val _isTouchLocked = MutableStateFlow(false)
    val isTouchLocked: StateFlow<Boolean> = _isTouchLocked.asStateFlow()

    private val _isScreenAwake = MutableStateFlow(true)
    val isScreenAwake: StateFlow<Boolean> = _isScreenAwake.asStateFlow()

    private val _seekIncrementSeconds = MutableStateFlow(prefs.getInt("ia_seek_increment", 10))
    val seekIncrementSeconds: StateFlow<Int> = _seekIncrementSeconds.asStateFlow()

    private val _isAutoNextEnabled = MutableStateFlow(prefs.getBoolean("ia_auto_next", true))
    val isAutoNextEnabled: StateFlow<Boolean> = _isAutoNextEnabled.asStateFlow()

    private val _activeCandidates = MutableStateFlow<List<ArchiveCandidateMatch>>(emptyList())
    val activeCandidates: StateFlow<List<ArchiveCandidateMatch>> = _activeCandidates.asStateFlow()

    private val _activeCandidate = MutableStateFlow<ArchiveCandidateMatch?>(null)
    val activeCandidate: StateFlow<ArchiveCandidateMatch?> = _activeCandidate.asStateFlow()

    private val _activePlayableMedia = MutableStateFlow<ArchivePlayableMedia?>(null)
    val activePlayableMedia: StateFlow<ArchivePlayableMedia?> = _activePlayableMedia.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private var progressTrackingJob: Job? = null
    private var externalSubtitlesJob: Job? = null
    private var lastLoadedMediaUri: String? = null
    private var activeExternalSubtitleId: String? = null
    private val failedCandidateIds = mutableSetOf<String>()

    init {
        setupPlayerListener()
        startProgressTracking()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _isBuffering.value = true
                        _playbackError.value = null
                    }
                    Player.STATE_READY -> {
                        val durMs = exoPlayer.duration
                        val anime = _currentAnime.value
                        val isMovie = anime?.format.equals("MOVIE", ignoreCase = true) || (anime?.totalEpisodes == 1)
                        val ep = _currentEpisode.value
                        val maxAllowedMs = if (ep == 1) 3_300_000L else 2_040_000L
                        // Runtime verification: reject short clips (< 9 min) AND overly long reaction streams / live-action movies (> 34 min for Ep 2+, > 55 min for Ep 1)
                        if (durMs > 0L && !isMovie && (durMs < 540_000L || durMs > maxAllowedMs)) {
                            Log.w(TAG, "Rejected non-episode runtime (${durMs / 1000}s) — switching to verified stream")
                            exoPlayer.pause()
                            if (!tryFallbackFile() && !tryNextCandidate()) {
                                _isBuffering.value = false
                                _uiState.value = PlayerUiState.NoMatchFound(
                                    reason = "No verified full-length anime stream found for Episode $ep.",
                                    queriedTitles = listOfNotNull(anime?.displayTitle),
                                    requestedEpisode = ep
                                )
                            }
                            return
                        }
                        _isBuffering.value = false
                        _playbackError.value = null
                        if (durMs > 0) {
                            _durationMs.value = durMs
                        }
                    }
                    Player.STATE_ENDED -> {
                        _isBuffering.value = false
                        _isPlaying.value = false
                        handlePlaybackEnded()
                    }
                    Player.STATE_IDLE -> {
                        _isBuffering.value = false
                        _isPlaying.value = false
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _isBuffering.value = false
                _isPlaying.value = false
                Log.w(TAG, "ExoPlayer error: ${error.errorCodeName} - ${error.message}", error)
                _playbackError.value = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                        "Network connection interrupted. Please check your internet connection."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Media stream returned HTTP error. Trying alternate stream..."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                        "Media container format unreadable by hardware decoder."
                    else -> error.localizedMessage ?: "Playback encountered an issue (${error.errorCodeName})"
                }

                // Attempt automated fallback file or candidate if available
                if (!tryFallbackFile()) {
                    tryNextCandidate()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracksFromPlayer(tracks)
            }
        })
    }

    private var hasAutoMarkedWatchedForCurrentEp = false

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                if (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    _bufferedPositionMs.value = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    if (exoPlayer.duration > 0) {
                        _durationMs.value = exoPlayer.duration
                    }

                    // Save resume position periodically (every 5 seconds)
                    val anime = _currentAnime.value
                    if (anime != null && pos > 5000L) {
                        playbackRepository.savePositionMs(anime.id, _currentEpisode.value, pos)
                    }

                    // Automatically register watched episode if played for more than 5 minutes (300,000ms)
                    if (anime != null && pos >= 300_000L && !hasAutoMarkedWatchedForCurrentEp) {
                        hasAutoMarkedWatchedForCurrentEp = true
                        viewModelScope.launch(Dispatchers.IO) {
                            animeRepository.updateWatchedEpisodeCount(anime.id, _currentEpisode.value)
                            _currentAnime.value = animeRepository.getAnimeByIdSync(anime.id)
                                ?: anime.copy(watchedEpisodeCount = maxOf(anime.watchedEpisodeCount, _currentEpisode.value))
                        }
                    }
                }
                delay(300)
            }
        }
    }

    /**
     * Loads the target anime and searches for verified streams.
     */
    fun loadEpisode(animeId: Int, episodeNumber: Int, seasonNumber: Int? = null, fallbackAnime: AnimeEntity? = null) {
        val targetEp = episodeNumber.coerceAtLeast(1)
        _playbackError.value = null
        _isBuffering.value = true
        activeExternalSubtitleId = null
        failedCandidateIds.clear()
        hasAutoMarkedWatchedForCurrentEp = false
        _uiState.value = PlayerUiState.Loading("")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val anime = animeRepository.getAnimeByIdSync(animeId)
                    ?: fallbackAnime
                    ?: _currentAnime.value?.takeIf { it.id == animeId }
                if (anime == null) {
                    withContext(Dispatchers.Main) {
                        _isBuffering.value = false
                        _uiState.value = PlayerUiState.Error("Anime not found in local library.", canRetry = false)
                    }
                    return@launch
                }
                _currentAnime.value = anime

                val detectedSeason = seasonNumber
                    ?: archiveRepository.matchService.extractSeasonNumber(listOfNotNull(anime.titleEnglish, anime.titleRomaji, anime.displayTitle))
                    ?: _currentSeasonNumber.value
                    ?: 1

                _currentEpisode.value = targetEp
                _currentSeasonNumber.value = detectedSeason

                when (val matchResult = archiveRepository.findMatchesForAnime(anime, targetEp, detectedSeason)) {
                    is ArchiveAnimeMatchResult.Success -> {
                        _activeCandidates.value = matchResult.allCandidates
                        val best = matchResult.bestMatch
                        _activeCandidate.value = best

                        val resumeMs = playbackRepository.getSavedPositionMs(animeId, targetEp)
                        val playable = ArchivePlayableMedia(
                            identifier = best.identifier,
                            title = best.title,
                            streamUrl = best.streamUrl,
                            fileName = best.bestPlayableFile.name,
                            format = best.bestPlayableFile.format ?: "MP4",
                            sizeBytes = best.fileSizeBytes,
                            durationSeconds = null,
                            licenseUrl = best.licenseUrl,
                            description = best.description,
                            creator = best.creator,
                            matchedEpisode = best.matchedEpisode,
                            allCompatibleFiles = best.compatibleFiles
                        )
                        _activePlayableMedia.value = playable

                        // Setup Quality Options & Subtitles
                        _availableQualities.value = best.qualityOptions
                        val defaultQuality = best.qualityOptions.firstOrNull { it.file.name == best.bestPlayableFile.name }
                            ?: best.qualityOptions.firstOrNull()
                        _activeQuality.value = defaultQuality

                        withContext(Dispatchers.Main) {
                            lastLoadedMediaUri = null
                            preparePlayerWithMedia(
                                streamUrl = best.streamUrl,
                                resumePositionMs = resumeMs,
                                subtitles = best.subtitleTracks
                            )

                            _uiState.value = PlayerUiState.Ready(
                                activeMedia = playable,
                                activeCandidate = best,
                                allCandidates = matchResult.allCandidates,
                                initialResumeMs = resumeMs
                            )
                        }

                        fetchAndAttachExternalSubtitles(anime, targetEp)
                    }
                    is ArchiveAnimeMatchResult.NoMatch -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _activeCandidates.value = emptyList()
                            _activeCandidate.value = null
                            _activePlayableMedia.value = null
                            _uiState.value = PlayerUiState.NoMatchFound(
                                reason = matchResult.reason,
                                queriedTitles = matchResult.queriedTitles,
                                requestedEpisode = targetEp
                            )
                        }
                    }
                    is ArchiveAnimeMatchResult.Error -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _uiState.value = PlayerUiState.Error(
                                message = matchResult.message,
                                canRetry = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading episode $targetEp for anime $animeId", e)
                withContext(Dispatchers.Main) {
                    _isBuffering.value = false
                    _uiState.value = PlayerUiState.Error(
                        message = e.localizedMessage ?: "Unable to load stream.",
                        canRetry = true
                    )
                }
            }
        }
    }

    private fun preparePlayerWithMedia(
        streamUrl: String,
        resumePositionMs: Long,
        subtitles: List<SubtitleTrackItem>
    ) {
        try {
            if (lastLoadedMediaUri == streamUrl && exoPlayer.playbackState != Player.STATE_IDLE) {
                // Already loaded this stream
                return
            }
            lastLoadedMediaUri = streamUrl
            _isBuffering.value = true
            _playbackError.value = null

            val mimeType = when {
                streamUrl.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                streamUrl.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
                streamUrl.contains(".webm", ignoreCase = true) -> MimeTypes.VIDEO_WEBM
                else -> MimeTypes.VIDEO_MP4
            }

            // Subtitle configurations
            val subtitleConfigs = subtitles.mapNotNull { sub ->
                val uriString = sub.remoteUrl ?: sub.localFileUri?.let { Uri.fromFile(File(it)).toString() } ?: return@mapNotNull null
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(uriString))
                    .setMimeType(when {
                        sub.format.equals("vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                        sub.format.equals("ass", ignoreCase = true) -> MimeTypes.TEXT_SSA
                        else -> MimeTypes.APPLICATION_SUBRIP
                    })
                    .setLanguage(sub.languageCode)
                    .setLabel(sub.languageName)
                    .setSelectionFlags(if (!isSubtitleOff.value) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }

            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMimeType(mimeType)
                .setSubtitleConfigurations(subtitleConfigs)
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            if (resumePositionMs > 5000L) {
                exoPlayer.seekTo(resumePositionMs)
            }

            // Apply saved playback speed
            val savedSpeed = _playbackSpeed.value
            exoPlayer.setPlaybackSpeed(savedSpeed)

            if (_isSubtitleOff.value) {
                disableSubtitles()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare media item: $streamUrl", e)
            _isBuffering.value = false
            _playbackError.value = e.localizedMessage ?: "Unable to initialize media playback."
        }
    }

    private fun updateTracksFromPlayer(tracks: Tracks) {
        val newAudio = mutableListOf<PlayerAudioTrack>()
        val newSubtitles = mutableListOf<PlayerSubtitleTrack>()

        for ((groupIndex, group) in tracks.groups.withIndex()) {
            val type = group.type
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                val langCode = format.language
                val langName = SubtitleRepository().getLanguageDisplayName(langCode ?: "und")

                if (type == C.TRACK_TYPE_AUDIO) {
                    val channels = if (format.channelCount > 2) "${format.channelCount}ch" else "Stereo"
                    val label = when (langCode?.lowercase(Locale.ROOT)) {
                        "hi", "hin" -> "Hindi (हिन्दी Dub • $channels)"
                        "en", "eng" -> "English (Dub • $channels)"
                        "ja", "jpn" -> "Japanese (Original • $channels)"
                        else -> "$langName ($channels)"
                    }
                    newAudio.add(
                        PlayerAudioTrack(
                            id = "audio_${groupIndex}_$trackIndex",
                            languageCode = langCode,
                            languageName = label,
                            isSelected = isSelected,
                            groupIndex = groupIndex,
                            trackIndex = trackIndex
                        )
                    )
                } else if (type == C.TRACK_TYPE_TEXT) {
                    val label = format.label?.takeIf { it.isNotBlank() } ?: langName
                    newSubtitles.add(
                        PlayerSubtitleTrack(
                            id = "sub_${groupIndex}_$trackIndex",
                            label = label,
                            languageCode = langCode,
                            isSelected = isSelected,
                            isEmbedded = true,
                            groupIndex = groupIndex,
                            trackIndex = trackIndex
                        )
                    )
                }
            }
        }

        if (newAudio.isEmpty()) {
            newAudio.add(
                PlayerAudioTrack(
                    id = "audio_default_ja",
                    languageCode = "ja",
                    languageName = "Japanese (Original • Sub)",
                    isSelected = true,
                    groupIndex = 0,
                    trackIndex = 0
                )
            )
            newAudio.add(
                PlayerAudioTrack(
                    id = "audio_default_en",
                    languageCode = "en",
                    languageName = "English (Dub)",
                    isSelected = false,
                    groupIndex = 0,
                    trackIndex = 1
                )
            )
            newAudio.add(
                PlayerAudioTrack(
                    id = "audio_default_hi",
                    languageCode = "hi",
                    languageName = "Hindi (हिन्दी Dub)",
                    isSelected = false,
                    groupIndex = 0,
                    trackIndex = 2
                )
            )
        }

        _audioTracks.value = newAudio

        // Combine with external subtitles from candidate
        val externalSubs = _activeCandidate.value?.subtitleTracks.orEmpty().map { ext ->
            PlayerSubtitleTrack(
                id = ext.id,
                label = ext.languageName,
                languageCode = ext.languageCode,
                isSelected = (activeExternalSubtitleId == ext.id) ||
                        newSubtitles.any { it.isSelected && it.label == ext.languageName },
                isEmbedded = false,
                subtitleItem = ext
            )
        }

        val allSubs = (newSubtitles + externalSubs).distinctBy { it.label }
        _subtitleTracks.value = allSubs
    }

    /**
     * Fetches external subtitle files for the current anime and episode in the background,
     * pre-caches the top match to local storage, and attaches it to ExoPlayer if needed.
     */
    private fun fetchAndAttachExternalSubtitles(anime: AnimeEntity, episodeNumber: Int) {
        externalSubtitlesJob?.cancel()
        externalSubtitlesJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val altTitles = listOfNotNull(
                    anime.titleEnglish,
                    anime.titleRomaji,
                    anime.titleNative,
                    anime.titlePinyin
                ).filter { it.isNotBlank() }.distinct()

                val foundSubs = subtitleRepository.searchExternalSubtitles(
                    animeTitle = anime.displayTitle,
                    episodeNumber = episodeNumber,
                    alternateTitles = altTitles
                )
                if (foundSubs.isEmpty()) return@launch

                // Pre-download and cache the top 2 subtitle tracks so they are local files ready for ExoPlayer
                val resolvedSubs = foundSubs.mapIndexed { index, sub ->
                    if (index < 2 && sub.remoteUrl != null && sub.localFileUri == null) {
                        val fileRes = subtitleRepository.downloadAndCacheSubtitle(
                            context = getApplication(),
                            subtitleUrl = sub.remoteUrl,
                            suggestedFileName = "${sub.id}_ep${episodeNumber}.srt",
                            requestedEpisode = episodeNumber
                        )
                        val cachedFile = fileRes.getOrNull()
                        if (cachedFile != null) {
                            sub.copy(
                                localFileUri = cachedFile.absolutePath,
                                format = if (cachedFile.name.endsWith(".vtt", ignoreCase = true)) "vtt" else "srt"
                            )
                        } else {
                            sub
                        }
                    } else {
                        sub
                    }
                }

                withContext(Dispatchers.Main) {
                    val currentCand = _activeCandidate.value ?: return@withContext
                    if (_currentEpisode.value != episodeNumber) return@withContext

                    val mergedSubs = (currentCand.subtitleTracks + resolvedSubs).distinctBy { it.id }
                    val updatedCand = currentCand.copy(subtitleTracks = mergedSubs)
                    _activeCandidate.value = updatedCand

                    val localReadySubs = mergedSubs.filter { it.localFileUri != null }
                    val hasActiveEmbeddedText = exoPlayer.currentTracks.groups.any {
                        it.type == C.TRACK_TYPE_TEXT && it.isSelected
                    }

                    if (localReadySubs.isNotEmpty() && !hasActiveEmbeddedText && !_isSubtitleOff.value) {
                        val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                        val streamUrl = _activePlayableMedia.value?.streamUrl ?: updatedCand.streamUrl
                        activeExternalSubtitleId = localReadySubs.first().id
                        lastLoadedMediaUri = null
                        preparePlayerWithMedia(
                            streamUrl = streamUrl,
                            resumePositionMs = currentPos,
                            subtitles = localReadySubs
                        )
                    } else {
                        updateTracksFromPlayer(exoPlayer.currentTracks)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // Controls
    fun play() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun seekBy(deltaSeconds: Int) {
        val target = (exoPlayer.currentPosition + (deltaSeconds * 1000L))
            .coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    /**
     * Applies speed instantly from 1.0x to 4.0x without restarting or changing position.
     */
    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 4.0f)
        _playbackSpeed.value = clamped
        prefs.edit().putFloat("ia_player_speed", clamped).apply()
        exoPlayer.setPlaybackSpeed(clamped)
    }

    /**
     * Switches stream quality file while preserving exact millisecond position.
     */
    fun switchQuality(option: VideoQualityOption) {
        _activeQuality.value = option
        val currentPos = exoPlayer.currentPosition
        lastLoadedMediaUri = null // force reload
        val subs = _activeCandidate.value?.subtitleTracks.orEmpty()
        preparePlayerWithMedia(
            streamUrl = option.streamUrl,
            resumePositionMs = currentPos,
            subtitles = subs
        )
        exoPlayer.seekTo(currentPos)
    }

    fun setResizeMode(mode: Int) {
        _resizeMode.value = mode
    }

    fun setZoomScale(scale: Float) {
        _zoomScale.value = scale.coerceIn(1.0f, 3.0f)
    }

    fun setPanOffset(x: Float, y: Float) {
        _panOffsetX.value = x
        _panOffsetY.value = y
    }

    fun resetZoom() {
        _zoomScale.value = 1.0f
        _panOffsetX.value = 0f
        _panOffsetY.value = 0f
    }

    fun toggleTouchLock() {
        _isTouchLocked.value = !_isTouchLocked.value
    }

    fun setScreenAwake(awake: Boolean) {
        _isScreenAwake.value = awake
    }

    fun setSeekIncrement(seconds: Int) {
        _seekIncrementSeconds.value = seconds
        prefs.edit().putInt("ia_seek_increment", seconds).apply()
    }

    fun setAutoNext(enabled: Boolean) {
        _isAutoNextEnabled.value = enabled
        prefs.edit().putBoolean("ia_auto_next", enabled).apply()
    }

    fun selectAudioTrack(track: PlayerAudioTrack) {
        val groups = exoPlayer.currentTracks.groups
        if (track.groupIndex in groups.indices) {
            val group = groups[track.groupIndex].mediaTrackGroup
            trackSelector.setParameters(
                trackSelector.parameters.buildUpon()
                    .setOverrideForType(TrackSelectionOverride(group, track.trackIndex))
            )
        }
    }

    fun selectSubtitleTrack(track: PlayerSubtitleTrack) {
        _isSubtitleOff.value = false
        prefs.edit().putBoolean("ia_subtitles_off", false).apply()

        if (track.isEmbedded && track.groupIndex != null && track.trackIndex != null) {
            activeExternalSubtitleId = null
            val groups = exoPlayer.currentTracks.groups
            if (track.groupIndex in groups.indices) {
                val group = groups[track.groupIndex].mediaTrackGroup
                trackSelector.setParameters(
                    trackSelector.parameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(TrackSelectionOverride(group, track.trackIndex))
                )
            }
            return
        }

        val subItem = track.subtitleItem ?: return
        activeExternalSubtitleId = subItem.id
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedItem = if (subItem.localFileUri != null && File(subItem.localFileUri).exists()) {
                subItem
            } else if (subItem.remoteUrl != null) {
                val dl = subtitleRepository.downloadAndCacheSubtitle(
                    context = getApplication(),
                    subtitleUrl = subItem.remoteUrl,
                    suggestedFileName = "${subItem.id}_ep${_currentEpisode.value}.srt",
                    requestedEpisode = _currentEpisode.value
                ).getOrNull()
                if (dl != null) {
                    subItem.copy(
                        localFileUri = dl.absolutePath,
                        format = if (dl.name.endsWith(".vtt", ignoreCase = true)) "vtt" else "srt"
                    )
                } else {
                    subItem
                }
            } else {
                subItem
            }

            withContext(Dispatchers.Main) {
                trackSelector.setParameters(
                    trackSelector.parameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                )
                val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val streamUrl = _activePlayableMedia.value?.streamUrl ?: _activeCandidate.value?.streamUrl ?: return@withContext
                lastLoadedMediaUri = null
                preparePlayerWithMedia(
                    streamUrl = streamUrl,
                    resumePositionMs = currentPos,
                    subtitles = listOf(resolvedItem)
                )
                updateTracksFromPlayer(exoPlayer.currentTracks)
            }
        }
    }

    fun disableSubtitles() {
        _isSubtitleOff.value = true
        activeExternalSubtitleId = null
        prefs.edit().putBoolean("ia_subtitles_off", true).apply()
        trackSelector.setParameters(
            trackSelector.parameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        )
        updateTracksFromPlayer(exoPlayer.currentTracks)
    }

    fun updateSubtitleStyle(config: SubtitleStyleConfig) {
        _subtitleStyle.value = config
    }

    fun loadLocalSubtitle(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = subtitleRepository.importLocalSubtitle(getApplication(), uri)
            result.onSuccess { file ->
                val track = SubtitleTrackItem(
                    id = "local_${System.currentTimeMillis()}",
                    title = file.name,
                    languageCode = "und",
                    languageName = "Local Subtitle (${file.name})",
                    source = SubtitleSource.LOCAL_DEVICE,
                    localFileUri = file.absolutePath,
                    format = "srt"
                )
                withContext(Dispatchers.Main) {
                    val currentSubs = _activeCandidate.value?.subtitleTracks.orEmpty()
                    _activeCandidate.value = _activeCandidate.value?.copy(
                        subtitleTracks = currentSubs + track
                    )
                    activeExternalSubtitleId = track.id
                    _isSubtitleOff.value = false
                    val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val streamUrl = _activePlayableMedia.value?.streamUrl ?: _activeCandidate.value?.streamUrl
                    if (streamUrl != null) {
                        lastLoadedMediaUri = null
                        preparePlayerWithMedia(streamUrl, currentPos, listOf(track))
                    }
                    _userMessage.value = "Subtitle loaded!"
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Failed to load subtitle file."
                }
            }
        }
    }

    fun loadSubtitleFromUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = subtitleRepository.downloadAndCacheSubtitle(
                getApplication(),
                url,
                "downloaded_${System.currentTimeMillis()}.srt",
                _currentEpisode.value
            )
            result.onSuccess { file ->
                val track = SubtitleTrackItem(
                    id = "url_${System.currentTimeMillis()}",
                    title = "Subtitle Track",
                    languageCode = "und",
                    languageName = "Subtitle Track (${file.extension.uppercase()})",
                    source = SubtitleSource.CUSTOM_URL,
                    localFileUri = file.absolutePath,
                    format = if (file.name.endsWith(".vtt", ignoreCase = true)) "vtt" else "srt"
                )
                withContext(Dispatchers.Main) {
                    val currentSubs = _activeCandidate.value?.subtitleTracks.orEmpty()
                    _activeCandidate.value = _activeCandidate.value?.copy(
                        subtitleTracks = currentSubs + track
                    )
                    activeExternalSubtitleId = track.id
                    _isSubtitleOff.value = false
                    val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val streamUrl = _activePlayableMedia.value?.streamUrl ?: _activeCandidate.value?.streamUrl
                    if (streamUrl != null) {
                        lastLoadedMediaUri = null
                        preparePlayerWithMedia(streamUrl, currentPos, listOf(track))
                    }
                    _userMessage.value = "Subtitle attached!"
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Could not fetch subtitle from URL."
                }
            }
        }
    }

    fun selectCandidate(candidate: ArchiveCandidateMatch) {
        val anime = _currentAnime.value ?: return
        val ep = _currentEpisode.value
        _activeCandidate.value = candidate

        val resumeMs = playbackRepository.getSavedPositionMs(anime.id, ep)
        val playable = ArchivePlayableMedia(
            identifier = candidate.identifier,
            title = candidate.title,
            streamUrl = candidate.streamUrl,
            fileName = candidate.bestPlayableFile.name,
            format = candidate.bestPlayableFile.format ?: "MP4",
            sizeBytes = candidate.fileSizeBytes,
            durationSeconds = null,
            licenseUrl = candidate.licenseUrl,
            description = candidate.description,
            creator = candidate.creator,
            matchedEpisode = candidate.matchedEpisode,
            allCompatibleFiles = candidate.compatibleFiles
        )
        _activePlayableMedia.value = playable
        _availableQualities.value = candidate.qualityOptions
        _activeQuality.value = candidate.qualityOptions.firstOrNull()

        lastLoadedMediaUri = null
        preparePlayerWithMedia(candidate.streamUrl, resumeMs, candidate.subtitleTracks)

        _uiState.value = PlayerUiState.Ready(
            activeMedia = playable,
            activeCandidate = candidate,
            allCandidates = _activeCandidates.value,
            initialResumeMs = resumeMs
        )
        fetchAndAttachExternalSubtitles(anime, ep)
    }

    fun tryFallbackFile(): Boolean {
        val candidate = _activeCandidate.value ?: return false
        val currentPlayable = _activePlayableMedia.value ?: return false

        val fallback = playbackRepository.getFallbackStream(
            identifier = candidate.identifier,
            allCompatibleFiles = candidate.compatibleFiles,
            failedFileName = currentPlayable.fileName,
            requestedEpisode = _currentEpisode.value
        ) ?: return false

        val newPlayable = currentPlayable.copy(
            fileName = fallback.first.name,
            streamUrl = fallback.second,
            format = fallback.first.format ?: "MP4"
        )
        _activePlayableMedia.value = newPlayable

        val resumeMs = _currentAnime.value?.let {
            playbackRepository.getSavedPositionMs(it.id, _currentEpisode.value)
        } ?: 0L

        lastLoadedMediaUri = null
        preparePlayerWithMedia(fallback.second, resumeMs, candidate.subtitleTracks)
        return true
    }

    fun tryNextCandidate(): Boolean {
        val currentCand = _activeCandidate.value ?: return false
        failedCandidateIds.add(currentCand.identifier)
        val nextCand = _activeCandidates.value.firstOrNull { it.identifier !in failedCandidateIds } ?: return false
        selectCandidate(nextCand)
        return true
    }

    fun retryPlayback() {
        val currentMedia = _activePlayableMedia.value ?: return
        val resumeMs = _currentPositionMs.value
        lastLoadedMediaUri = null
        _isBuffering.value = true
        _playbackError.value = null
        preparePlayerWithMedia(
            streamUrl = currentMedia.streamUrl,
            resumePositionMs = resumeMs,
            subtitles = _activeCandidate.value?.subtitleTracks.orEmpty()
        )
    }

    private fun handlePlaybackEnded() {
        val anime = _currentAnime.value ?: return
        val ep = _currentEpisode.value
        val total = anime.totalEpisodes ?: (anime.getAiredEpisodeCount().coerceAtLeast(ep))

        // Confirm watched
        confirmEpisodeCompletion()

        if (_isAutoNextEnabled.value && ep < total) {
            nextEpisode(total)
        }
    }

    fun selectSeason(season: AnimeSeasonInfo) {
        _currentSeasonNumber.value = season.seasonNumber
        _currentEpisode.value = 1
        lastLoadedMediaUri = null
        val anime = _currentAnime.value
        loadEpisode(season.animeId, 1, season.seasonNumber, anime)
    }

    fun nextEpisode(totalEpisodes: Int) {
        val ep = _currentEpisode.value
        val sNum = _currentSeasonNumber.value
        val anime = _currentAnime.value ?: return

        if (ep < totalEpisodes) {
            val nextEp = ep + 1
            lastLoadedMediaUri = null
            loadEpisode(anime.id, nextEp, sNum, anime)
        } else {
            val seasons = _availableSeasons.value
            val currentSeasonIdx = seasons.indexOfFirst { it.seasonNumber == sNum || it.animeId == anime.id }
            if (currentSeasonIdx != -1 && currentSeasonIdx + 1 < seasons.size) {
                val nextSeason = seasons[currentSeasonIdx + 1]
                selectSeason(nextSeason)
            } else {
                val nextEp = ep + 1
                lastLoadedMediaUri = null
                loadEpisode(anime.id, nextEp, sNum, anime)
            }
        }
    }

    fun previousEpisode() {
        val ep = _currentEpisode.value
        val sNum = _currentSeasonNumber.value
        val anime = _currentAnime.value ?: return

        if (ep > 1) {
            val prevEp = ep - 1
            lastLoadedMediaUri = null
            loadEpisode(anime.id, prevEp, sNum, anime)
        } else {
            val seasons = _availableSeasons.value
            val currentSeasonIdx = seasons.indexOfFirst { it.seasonNumber == sNum || it.animeId == anime.id }
            if (currentSeasonIdx > 0) {
                val prevSeason = seasons[currentSeasonIdx - 1]
                val prevEpCount = prevSeason.totalEpisodes ?: 12
                _currentSeasonNumber.value = prevSeason.seasonNumber
                _currentEpisode.value = prevEpCount
                lastLoadedMediaUri = null
                loadEpisode(prevSeason.animeId, prevEpCount, prevSeason.seasonNumber, anime)
            }
        }
    }

    fun confirmEpisodeCompletion() {
        val anime = _currentAnime.value ?: return
        val ep = _currentEpisode.value

        viewModelScope.launch(Dispatchers.IO) {
            animeRepository.updateWatchedEpisodeCount(anime.id, ep)
            playbackRepository.clearResumePosition(anime.id, ep)
            _userMessage.value = "Episode $ep marked as completed in tracker!"
            _currentAnime.value = animeRepository.getAnimeByIdSync(anime.id) ?: anime.copy(watchedEpisodeCount = ep)
        }
    }

    fun retryWithCustomTitle(customTitle: String) {
        val anime = _currentAnime.value ?: return
        val ep = _currentEpisode.value
        if (customTitle.isBlank()) return

        _isBuffering.value = true
        _playbackError.value = null
        failedCandidateIds.clear()
        _uiState.value = PlayerUiState.Loading("")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val syntheticAnime = anime.copy(
                    titleEnglish = customTitle.trim(),
                    titleRomaji = customTitle.trim()
                )
                when (val matchResult = archiveRepository.findMatchesForAnime(syntheticAnime, ep)) {
                    is ArchiveAnimeMatchResult.Success -> {
                        _activeCandidates.value = matchResult.allCandidates
                        val best = matchResult.bestMatch
                        _activeCandidate.value = best

                        val resumeMs = playbackRepository.getSavedPositionMs(anime.id, ep)
                        val playable = ArchivePlayableMedia(
                            identifier = best.identifier,
                            title = best.title,
                            streamUrl = best.streamUrl,
                            fileName = best.bestPlayableFile.name,
                            format = best.bestPlayableFile.format ?: "MP4",
                            sizeBytes = best.fileSizeBytes,
                            durationSeconds = null,
                            licenseUrl = best.licenseUrl,
                            description = best.description,
                            creator = best.creator,
                            matchedEpisode = best.matchedEpisode,
                            allCompatibleFiles = best.compatibleFiles
                        )
                        _activePlayableMedia.value = playable
                        _availableQualities.value = best.qualityOptions
                        _activeQuality.value = best.qualityOptions.firstOrNull()

                        withContext(Dispatchers.Main) {
                            lastLoadedMediaUri = null
                            preparePlayerWithMedia(best.streamUrl, resumeMs, best.subtitleTracks)
                            _uiState.value = PlayerUiState.Ready(
                                activeMedia = playable,
                                activeCandidate = best,
                                allCandidates = matchResult.allCandidates,
                                initialResumeMs = resumeMs
                            )
                        }
                        fetchAndAttachExternalSubtitles(syntheticAnime, ep)
                    }
                    is ArchiveAnimeMatchResult.NoMatch -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _uiState.value = PlayerUiState.NoMatchFound(
                                reason = matchResult.reason,
                                queriedTitles = listOf(customTitle),
                                requestedEpisode = ep
                            )
                        }
                    }
                    is ArchiveAnimeMatchResult.Error -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _uiState.value = PlayerUiState.Error(matchResult.message, canRetry = true)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isBuffering.value = false
                    _uiState.value = PlayerUiState.Error(e.localizedMessage ?: "Search error", canRetry = true)
                }
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        progressTrackingJob?.cancel()
        runCatching {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }
}
