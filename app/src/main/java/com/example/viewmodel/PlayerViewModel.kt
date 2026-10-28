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
import android.content.Intent
import android.provider.OpenableColumns
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
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
import com.example.data.repository.AniKotoRepository
import com.example.data.repository.AniKotoResult
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
import kotlinx.coroutines.withTimeoutOrNull
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
    private val aniKotoRepository: AniKotoRepository = AniKotoRepository(),
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
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(12_000)
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(application, httpDataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(application)
            .setDataSourceFactory(dataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 12_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_200,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500
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

    // Local Video Playback & Audio-Only Mode States
    private val _isLocalVideoMode = MutableStateFlow(false)
    val isLocalVideoMode: StateFlow<Boolean> = _isLocalVideoMode.asStateFlow()

    private val _localVideoTitle = MutableStateFlow<String?>(null)
    val localVideoTitle: StateFlow<String?> = _localVideoTitle.asStateFlow()

    private val _localVideoUri = MutableStateFlow<String?>(null)
    val localVideoUri: StateFlow<String?> = _localVideoUri.asStateFlow()

    private val _recentLocalVideos = MutableStateFlow<List<PlaybackRepository.LocalVideoHistoryItem>>(
        playbackRepository.getRecentLocalVideos()
    )
    val recentLocalVideos: StateFlow<List<PlaybackRepository.LocalVideoHistoryItem>> = _recentLocalVideos.asStateFlow()

    private val _isAudioOnlyMode = MutableStateFlow(false)
    val isAudioOnlyMode: StateFlow<Boolean> = _isAudioOnlyMode.asStateFlow()

    private var progressTrackingJob: Job? = null
    private var externalSubtitlesJob: Job? = null
    private var lastLoadedMediaUri: String? = null
    private var lastExplicitMimeType: String? = null
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
                        val ep = _currentEpisode.value
                        // Runtime verification: reject short clips/trailers (< 9 minutes) for online streams
                        if (!_isLocalVideoMode.value && durMs in 1L until 540_000L) {
                            Log.w(TAG, "Rejected short clip runtime (${durMs / 1000}s) — switching to verified stream")
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
                            if (_isLocalVideoMode.value) {
                                val uriStr = _localVideoUri.value
                                val titleStr = _localVideoTitle.value ?: "Local Video"
                                if (!uriStr.isNullOrBlank()) {
                                    playbackRepository.saveLocalVideoProgress(
                                        uriString = uriStr,
                                        displayName = titleStr,
                                        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                                        durationMs = durMs,
                                        mimeType = lastExplicitMimeType
                                    )
                                    _recentLocalVideos.value = playbackRepository.getRecentLocalVideos()
                                }
                            }
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
                _playbackError.value = mapPlaybackErrorToUserMessage(error, _isLocalVideoMode.value)

                if (!_isLocalVideoMode.value) {
                    // Attempt automated fallback file or candidate if available
                    if (!tryFallbackFile()) {
                        tryNextCandidate()
                    }
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
            var tickCount = 0
            while (isActive) {
                if (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    _bufferedPositionMs.value = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    if (exoPlayer.duration > 0) {
                        _durationMs.value = exoPlayer.duration
                    }

                    tickCount++
                    if (_isLocalVideoMode.value) {
                        val uriStr = _localVideoUri.value
                        val titleStr = _localVideoTitle.value ?: "Local Video"
                        if (!uriStr.isNullOrBlank() && pos > 2000L && tickCount % 8 == 0) {
                            playbackRepository.saveLocalVideoProgress(
                                uriString = uriStr,
                                displayName = titleStr,
                                positionMs = pos,
                                durationMs = _durationMs.value,
                                mimeType = lastExplicitMimeType
                            )
                            _recentLocalVideos.value = playbackRepository.getRecentLocalVideos()
                        }
                    } else {
                        // Save anime resume position periodically
                        val anime = _currentAnime.value
                        if (anime != null && pos > 5000L && tickCount % 8 == 0) {
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
        _isLocalVideoMode.value = false
        _localVideoTitle.value = null
        _localVideoUri.value = null
        lastExplicitMimeType = null
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

                // Primary Anime Stream Resolver: AniKotoAPI (`https://anikototvapi.vercel.app/api`)
                val aniKotoStreamRes = withTimeoutOrNull(18_000L) {
                    aniKotoRepository.resolveAnimeStream(
                        anime = anime,
                        episodeNumber = targetEp,
                        seasonNumber = detectedSeason,
                        archiveRepository = archiveRepository
                    )
                }

                when (aniKotoStreamRes) {
                    is AniKotoResult.Success -> {
                        val streamData = aniKotoStreamRes.data
                        val best = streamData.primaryCandidate
                        val allCandidates = streamData.allCandidates
                        val playable = streamData.playableMedia

                        _activeCandidates.value = allCandidates
                        _activeCandidate.value = best
                        _activePlayableMedia.value = playable

                        val resumeMs = playbackRepository.getSavedPositionMs(animeId, targetEp)

                        // Setup Quality Options & Subtitles
                        _availableQualities.value = best.qualityOptions
                        val defaultQuality = best.qualityOptions.firstOrNull { it.file.name == best.bestPlayableFile.name }
                            ?: best.qualityOptions.firstOrNull()
                        _activeQuality.value = defaultQuality

                        withContext(Dispatchers.Main) {
                            lastLoadedMediaUri = null
                            preparePlayerWithMedia(
                                streamUrl = playable.streamUrl,
                                resumePositionMs = resumeMs,
                                subtitles = best.subtitleTracks
                            )

                            _uiState.value = PlayerUiState.Ready(
                                activeMedia = playable,
                                activeCandidate = best,
                                allCandidates = allCandidates,
                                initialResumeMs = resumeMs
                            )
                        }

                        fetchAndAttachExternalSubtitles(anime, targetEp)
                    }
                    is AniKotoResult.Empty -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _activeCandidates.value = emptyList()
                            _activeCandidate.value = null
                            _activePlayableMedia.value = null
                            _uiState.value = PlayerUiState.NoMatchFound(
                                reason = aniKotoStreamRes.message,
                                queriedTitles = listOfNotNull(anime.displayTitle),
                                requestedEpisode = targetEp
                            )
                        }
                    }
                    is AniKotoResult.Error -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _activeCandidates.value = emptyList()
                            _activeCandidate.value = null
                            _activePlayableMedia.value = null
                            _uiState.value = PlayerUiState.NoMatchFound(
                                reason = aniKotoStreamRes.message,
                                queriedTitles = listOfNotNull(anime.displayTitle),
                                requestedEpisode = targetEp
                            )
                        }
                    }
                    null -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _activeCandidates.value = emptyList()
                            _activeCandidate.value = null
                            _activePlayableMedia.value = null
                            _uiState.value = PlayerUiState.NoMatchFound(
                                reason = "Can't find Episode $targetEp on AniKotoAPI.",
                                queriedTitles = listOfNotNull(anime.displayTitle),
                                requestedEpisode = targetEp
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
        subtitles: List<SubtitleTrackItem>,
        explicitMimeType: String? = null
    ) {
        try {
            if (lastLoadedMediaUri == streamUrl && exoPlayer.playbackState != Player.STATE_IDLE) {
                // Already loaded this stream
                return
            }
            lastLoadedMediaUri = streamUrl
            lastExplicitMimeType = explicitMimeType
            _isBuffering.value = true
            _playbackError.value = null

            val mimeType = resolveMediaMimeType(streamUrl, explicitMimeType)
            val currentOffsetMs = _subtitleStyle.value.offsetMs

            // Subtitle configurations (with real timing offset applied to local/cached subtitle files)
            val subtitleConfigs = subtitles.mapNotNull { sub ->
                val uriString = when {
                    sub.localFileUri != null -> {
                        val baseFile = File(sub.localFileUri)
                        val effectiveFile = if (currentOffsetMs != 0L && baseFile.exists()) {
                            subtitleRepository.applyTimingOffsetToSubtitleFile(baseFile, currentOffsetMs)
                        } else {
                            baseFile
                        }
                        Uri.fromFile(effectiveFile).toString()
                    }
                    sub.remoteUrl != null -> sub.remoteUrl
                    else -> null
                } ?: return@mapNotNull null

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

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(streamUrl)
                .setSubtitleConfigurations(subtitleConfigs)

            if (!mimeType.isNullOrBlank()) {
                mediaItemBuilder.setMimeType(mimeType)
            }

            exoPlayer.setMediaItem(mediaItemBuilder.build())
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            if (resumePositionMs > 3000L) {
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
        val previousOffset = _subtitleStyle.value.offsetMs
        _subtitleStyle.value = config
        if (config.offsetMs != previousOffset && !_isSubtitleOff.value) {
            val activeSubs = _activeCandidate.value?.subtitleTracks.orEmpty().filter { it.localFileUri != null }
            val streamUrl = _activePlayableMedia.value?.streamUrl ?: _activeCandidate.value?.streamUrl
            if (activeSubs.isNotEmpty() && streamUrl != null) {
                val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                lastLoadedMediaUri = null
                preparePlayerWithMedia(
                    streamUrl = streamUrl,
                    resumePositionMs = currentPos,
                    subtitles = activeSubs,
                    explicitMimeType = lastExplicitMimeType
                )
            }
        }
    }

    fun setAudioOnlyMode(enabled: Boolean) {
        _isAudioOnlyMode.value = enabled
        trackSelector.setParameters(
            trackSelector.parameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, enabled)
        )
        _userMessage.value = if (enabled) "Audio-Only mode enabled" else "Video display restored"
    }

    /**
     * Opens and plays a local device video file (`content://` or `file://`) directly in ExoPlayer,
     * independent of AniKotoAPI and internet connectivity.
     */
    fun loadLocalVideo(uri: Uri, customDisplayName: String? = null) {
        val app = getApplication<Application>()
        val resolver = app.contentResolver

        // Request persistable read permission if granted by the picker
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Verify the file is accessible and resolve its display name & MIME type
        var resolvedName = customDisplayName
        var resolvedMime = runCatching { resolver.getType(uri) }.getOrNull()

        if (uri.scheme == "content") {
            val accessible = runCatching {
                resolver.openFileDescriptor(uri, "r")?.use { true } ?: false
            }.getOrDefault(false)
            if (!accessible) {
                _isBuffering.value = false
                _playbackError.value = "Local video file is inaccessible or has been moved/deleted from device storage."
                _uiState.value = PlayerUiState.Error(
                    message = "Local video file could not be opened. It may have been moved, renamed, or deleted.",
                    canRetry = false
                )
                return
            }
            if (resolvedName.isNullOrBlank()) {
                runCatching {
                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) {
                                resolvedName = cursor.getString(idx)
                            }
                        }
                    }
                }
            }
        } else if (uri.scheme == "file") {
            val f = uri.path?.let { File(it) }
            if (f == null || !f.exists() || !f.canRead()) {
                _isBuffering.value = false
                _playbackError.value = "Local video file does not exist or cannot be read."
                _uiState.value = PlayerUiState.Error(
                    message = "Local video file not found at ${uri.path}.",
                    canRetry = false
                )
                return
            }
            if (resolvedName.isNullOrBlank()) {
                resolvedName = f.name
            }
        }

        val displayName = resolvedName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "Local Device Video"

        val uriStr = uri.toString()
        _isLocalVideoMode.value = true
        _localVideoUri.value = uriStr
        _localVideoTitle.value = displayName
        _playbackError.value = null
        activeExternalSubtitleId = null

        val savedResumeMs = playbackRepository.getLocalVideoSavedPositionMs(uriStr)
        val formatLabel = when {
            displayName.endsWith(".mkv", ignoreCase = true) || resolvedMime?.contains("matroska") == true -> "MKV"
            displayName.endsWith(".webm", ignoreCase = true) || resolvedMime?.contains("webm") == true -> "WEBM"
            displayName.endsWith(".m3u8", ignoreCase = true) || resolvedMime?.contains("mpegurl", ignoreCase = true) == true -> "HLS"
            displayName.endsWith(".mpd", ignoreCase = true) || resolvedMime?.contains("dash", ignoreCase = true) == true -> "DASH"
            else -> "MP4"
        }

        val localFileItem = ArchiveFileItem(
            name = displayName,
            source = "local_device",
            format = formatLabel,
            size = null,
            length = null,
            title = displayName
        )

        val localPlayable = ArchivePlayableMedia(
            identifier = "local_device_media",
            title = displayName,
            streamUrl = uriStr,
            fileName = displayName,
            format = formatLabel,
            sizeBytes = 0L,
            durationSeconds = null,
            licenseUrl = null,
            description = "Playing directly from device storage",
            creator = "Local Storage",
            matchedEpisode = null,
            allCompatibleFiles = listOf(localFileItem)
        )

        val localCandidate = ArchiveCandidateMatch(
            identifier = "local_device_media",
            title = displayName,
            description = "Local video file on device",
            creator = "Local Device",
            year = null,
            licenseUrl = null,
            thumbnailUrl = "",
            matchedEpisode = null,
            compatibleFiles = listOf(localFileItem),
            bestPlayableFile = localFileItem,
            matchScore = 1.0f,
            matchConfidence = com.example.data.repository.MatchConfidence.HIGH,
            isEpisodeSpecific = true,
            durationFormatted = null,
            fileSizeBytes = 0L,
            streamUrl = uriStr,
            qualityOptions = emptyList(),
            subtitleTracks = emptyList()
        )

        _activePlayableMedia.value = localPlayable
        _activeCandidate.value = localCandidate
        _activeCandidates.value = listOf(localCandidate)
        _availableQualities.value = emptyList()
        _activeQuality.value = null

        playbackRepository.saveLocalVideoProgress(
            uriString = uriStr,
            displayName = displayName,
            positionMs = savedResumeMs,
            durationMs = _durationMs.value,
            mimeType = resolvedMime
        )
        _recentLocalVideos.value = playbackRepository.getRecentLocalVideos()

        lastLoadedMediaUri = null
        preparePlayerWithMedia(
            streamUrl = uriStr,
            resumePositionMs = savedResumeMs,
            subtitles = emptyList(),
            explicitMimeType = resolvedMime
        )

        _uiState.value = PlayerUiState.Ready(
            activeMedia = localPlayable,
            activeCandidate = localCandidate,
            allCandidates = listOf(localCandidate),
            initialResumeMs = savedResumeMs
        )
    }

    fun removeLocalVideoFromHistory(uriString: String) {
        playbackRepository.removeLocalVideoFromHistory(uriString)
        _recentLocalVideos.value = playbackRepository.getRecentLocalVideos()
    }

    fun refreshRecentLocalVideos() {
        _recentLocalVideos.value = playbackRepository.getRecentLocalVideos()
    }

    fun loadLocalSubtitle(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = subtitleRepository.importLocalSubtitle(getApplication(), uri)
            result.onSuccess { file ->
                val ext = if (file.name.endsWith(".vtt", ignoreCase = true)) "vtt" else "srt"
                val track = SubtitleTrackItem(
                    id = "local_${System.currentTimeMillis()}",
                    title = file.name,
                    languageCode = "und",
                    languageName = "Local Subtitle (${file.name})",
                    source = SubtitleSource.LOCAL_DEVICE,
                    localFileUri = file.absolutePath,
                    format = ext
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
                        preparePlayerWithMedia(
                            streamUrl = streamUrl,
                            resumePositionMs = currentPos,
                            subtitles = listOf(track),
                            explicitMimeType = lastExplicitMimeType
                        )
                    }
                    _userMessage.value = "Subtitle loaded!"
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    _userMessage.value = err.localizedMessage ?: "Failed to load subtitle file."
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
                        preparePlayerWithMedia(
                            streamUrl = streamUrl,
                            resumePositionMs = currentPos,
                            subtitles = listOf(track),
                            explicitMimeType = lastExplicitMimeType
                        )
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
            subtitles = _activeCandidate.value?.subtitleTracks.orEmpty(),
            explicitMimeType = lastExplicitMimeType
        )
    }

    private fun handlePlaybackEnded() {
        if (_isLocalVideoMode.value) {
            val uriStr = _localVideoUri.value
            if (!uriStr.isNullOrBlank()) {
                playbackRepository.saveLocalVideoProgress(
                    uriString = uriStr,
                    displayName = _localVideoTitle.value ?: "Local Video",
                    positionMs = 0L,
                    durationMs = _durationMs.value,
                    mimeType = lastExplicitMimeType
                )
                _recentLocalVideos.value = playbackRepository.getRecentLocalVideos()
            }
            return
        }
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
                val aniKotoStreamRes = withTimeoutOrNull(10_500L) {
                    aniKotoRepository.resolveAnimeStream(
                        anime = syntheticAnime,
                        episodeNumber = ep,
                        seasonNumber = _currentSeasonNumber.value,
                        archiveRepository = archiveRepository
                    )
                }
                when (aniKotoStreamRes) {
                    is AniKotoResult.Success -> {
                        val streamData = aniKotoStreamRes.data
                        val best = streamData.primaryCandidate
                        val playable = streamData.playableMedia

                        _activeCandidates.value = streamData.allCandidates
                        _activeCandidate.value = best

                        val resumeMs = playbackRepository.getSavedPositionMs(anime.id, ep)
                        _activePlayableMedia.value = playable
                        _availableQualities.value = best.qualityOptions
                        _activeQuality.value = best.qualityOptions.firstOrNull()

                        withContext(Dispatchers.Main) {
                            lastLoadedMediaUri = null
                            preparePlayerWithMedia(playable.streamUrl, resumeMs, best.subtitleTracks)
                            _uiState.value = PlayerUiState.Ready(
                                activeMedia = playable,
                                activeCandidate = best,
                                allCandidates = streamData.allCandidates,
                                initialResumeMs = resumeMs
                            )
                        }
                        fetchAndAttachExternalSubtitles(syntheticAnime, ep)
                    }
                    is AniKotoResult.Empty -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _uiState.value = PlayerUiState.NoMatchFound(
                                reason = aniKotoStreamRes.message,
                                queriedTitles = listOf(customTitle),
                                requestedEpisode = ep
                            )
                        }
                    }
                    is AniKotoResult.Error -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _uiState.value = PlayerUiState.NoMatchFound(
                                reason = aniKotoStreamRes.message,
                                queriedTitles = listOf(customTitle),
                                requestedEpisode = ep
                            )
                        }
                    }
                    null -> {
                        withContext(Dispatchers.Main) {
                            _isBuffering.value = false
                            _uiState.value = PlayerUiState.NoMatchFound(
                                reason = "Can't find \"$customTitle\" on AniKotoAPI.",
                                queriedTitles = listOf(customTitle),
                                requestedEpisode = ep
                            )
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

    companion object {
        /**
         * Resolves the Media3 MIME type for MP4, WebM, MKV, HLS (.m3u8), MPEG-DASH (.mpd),
         * or local content URIs.
         */
        fun resolveMediaMimeType(uriOrUrl: String, explicitMimeType: String? = null): String? {
            if (!explicitMimeType.isNullOrBlank()) {
                val lowerMime = explicitMimeType.lowercase(Locale.ROOT)
                return when {
                    lowerMime.contains("mpegurl") || lowerMime.contains("m3u8") -> MimeTypes.APPLICATION_M3U8
                    lowerMime.contains("dash") || lowerMime.contains("mpd") -> MimeTypes.APPLICATION_MPD
                    lowerMime.contains("webm") -> MimeTypes.VIDEO_WEBM
                    lowerMime.contains("matroska") || lowerMime.contains("mkv") -> MimeTypes.VIDEO_MATROSKA
                    lowerMime.contains("mp4") -> MimeTypes.VIDEO_MP4
                    else -> explicitMimeType
                }
            }
            val cleanPath = uriOrUrl.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
            return when {
                cleanPath.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
                cleanPath.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
                cleanPath.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
                cleanPath.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
                cleanPath.endsWith(".mp4") || cleanPath.endsWith(".m4v") -> MimeTypes.VIDEO_MP4
                uriOrUrl.startsWith("content://") -> null // Let Media3 ContentDataSource sniff container
                else -> MimeTypes.VIDEO_MP4
            }
        }

        /**
         * Maps ExoPlayer [PlaybackException] codes into clear, actionable user-facing error descriptions.
         */
        fun mapPlaybackErrorToUserMessage(error: PlaybackException, isLocalVideo: Boolean = false): String {
            return when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network connection interrupted. Please check your internet connection and tap Retry."
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                    "Media server returned an HTTP error (${error.errorCodeName}). Trying alternate stream..."
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                    if (isLocalVideo) "Local video file was not found. It may have been moved or deleted."
                    else "Requested media file was not found on the server."
                PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                    "Permission denied while reading the video file. Please re-select the file using 'Open Video'."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                    "The media container or stream manifest is corrupted or unreadable."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
                    "Unsupported media container format. Supported formats: MP4, MKV, WebM, HLS (.m3u8), and DASH (.mpd)."
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                    "Unsupported video/audio codec on this device's hardware decoder (${error.errorCodeName})."
                PlaybackException.ERROR_CODE_DECODING_FAILED ->
                    "Hardware decoder encountered a frame decoding error. Tap Retry to resume."
                else -> error.localizedMessage ?: "Playback encountered an issue (${error.errorCodeName})"
            }
        }
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
