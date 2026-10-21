package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AnimeEntity
import com.example.data.local.AppDatabase
import com.example.data.local.EpisodeHistoryEntity
import com.example.data.remote.AiVerificationService
import com.example.data.remote.AniListOAuthManager
import com.example.data.remote.AniListUserProfile
import com.example.data.remote.AniListMedia
import com.example.data.remote.MalSyncLog
import com.example.data.remote.MalUserProfile
import com.example.data.remote.MyAnimeListOAuthManager
import com.example.data.remote.MyAnimeListService
import com.example.data.repository.AnimeRepository
import com.example.data.repository.YouTubeRepository
import com.example.data.repository.YouTubeVideo
import com.example.data.repository.YouTubeVideoType
import com.example.data.repository.YouTubeSearchResultState
import com.example.data.repository.YouTubeSearchLink
import com.example.data.repository.InternetArchiveRepository
import com.example.data.repository.ArchiveMediaItem
import com.example.data.repository.ArchivePlayableMedia
import com.example.data.repository.ArchiveSearchResultState
import com.example.data.repository.ArchivePlayableMediaResult
import com.example.data.repository.AnimeSeasonInfo
import com.example.util.EpisodeDataParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class AnimeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = AnimeRepository(
        animeDao = database.animeDao(),
        historyDao = database.episodeHistoryDao(),
        apiCacheDao = database.apiCacheDao(),
        deletedAnimeDao = database.deletedAnimeDao(),
        savedAnimeDao = database.savedAnimeDao(),
        playbackPreferenceDao = database.playbackPreferenceDao()
    )
    val malOAuthManager = MyAnimeListOAuthManager(application)
    private val _isStealthModeEnabled = MutableStateFlow(false)
    val isStealthModeEnabled = _isStealthModeEnabled.asStateFlow()
    val selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)

    @OptIn(ExperimentalCoroutinesApi::class)
    val dayAnime: StateFlow<List<AnimeEntity>> = selectedDay
        .flatMapLatest { day -> repository.getPlannerAnimeByDay(day) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val calendarAnime: StateFlow<List<AnimeEntity>> = selectedDay
        .flatMapLatest { day -> repository.getCalendarAnimeByDay(day) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val watchlist: StateFlow<List<AnimeEntity>> = repository.watchlist
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val favorites: StateFlow<List<AnimeEntity>> = repository.favorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val wishlist: StateFlow<List<AnimeEntity>> = repository.wishlist
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val watchHistory: StateFlow<List<EpisodeHistoryEntity>> = repository.watchHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val upcomingAnime: StateFlow<List<AnimeEntity>> = repository.upcomingAnime
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val allOngoingAnime: StateFlow<List<AnimeEntity>> = repository.allOngoingAnime
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val ongoingDonghua: StateFlow<List<AnimeEntity>> = repository.ongoingDonghua
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Theme & Styling customization
    private val _themeColor = MutableStateFlow("LIGHT_BLUE")
    val themeColor = _themeColor.asStateFlow()

    // Title Language Display Preference (ENGLISH, AUTO, ORIGINAL, ROMAJI_PINYIN)
    private val _titleLanguagePreference = MutableStateFlow("ENGLISH")
    val titleLanguagePreference = _titleLanguagePreference.asStateFlow()

    fun setTitleLanguagePreference(preference: String) {
        val clean = preference.uppercase().trim()
        _titleLanguagePreference.value = clean
        _userMessage.value = "Title language set to: $clean"
    }

    // Account & MAL Login State
    private val _isCloudLoggedIn = MutableStateFlow(false)
    val isCloudLoggedIn = _isCloudLoggedIn.asStateFlow()

    private val _isMalLoggedIn = MutableStateFlow(malOAuthManager.isLoggedIn)
    val isMalLoggedIn = _isMalLoggedIn.asStateFlow()

    private val _malUserProfile = MutableStateFlow<MalUserProfile?>(null)
    val malUserProfile = _malUserProfile.asStateFlow()

    private val _isMalSyncing = MutableStateFlow(false)
    val isMalSyncing = _isMalSyncing.asStateFlow()

    private val _malSyncLogs = MutableStateFlow<List<MalSyncLog>>(emptyList())
    val malSyncLogs = _malSyncLogs.asStateFlow()

    private val _cloudUserEmail = MutableStateFlow<String?>(null)
    val cloudUserEmail = _cloudUserEmail.asStateFlow()

    private val _cloudUserName = MutableStateFlow<String?>(null)
    val cloudUserName = _cloudUserName.asStateFlow()

    private val _lastCloudSyncTime = MutableStateFlow<Long?>(null)
    val lastCloudSyncTime = _lastCloudSyncTime.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing = _isCloudSyncing.asStateFlow()

    // Settings preferences
    private val _autoIncrementEpisode = MutableStateFlow(true)
    val autoIncrementEpisode = _autoIncrementEpisode.asStateFlow()

    private val _globalNotificationsEnabled = MutableStateFlow(true)
    val globalNotificationsEnabled = _globalNotificationsEnabled.asStateFlow()

    // Enabled features toggles
    private val _enabledFeatures = MutableStateFlow(
        setOf("STEPPER", "COUNTDOWN", "CLOUD_SYNC", "UPCOMING_CAROUSEL", "AI_ADVISOR", "NOTIFICATION_DOTS")
    )
    val enabledFeatures = _enabledFeatures.asStateFlow()

    // AI Craftsman state
    private val _aiCraftingState = MutableStateFlow(false)
    val aiCraftingState = _aiCraftingState.asStateFlow()

    private val _aiCraftResults = MutableStateFlow<List<String>>(emptyList())
    val aiCraftResults = _aiCraftResults.asStateFlow()

    // Search & Discover filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("ALL")
    val selectedGenre = _selectedGenre.asStateFlow()

    private val prefs = application.getSharedPreferences("anitrack_app_prefs", android.content.Context.MODE_PRIVATE)
    private val _safeModeEnabled = MutableStateFlow(prefs.getBoolean("safe_mode_enabled", true))
    val safeModeEnabled: StateFlow<Boolean> = _safeModeEnabled.asStateFlow()

    fun toggleSafeMode() {
        val newValue = !_safeModeEnabled.value
        _safeModeEnabled.value = newValue
        prefs.edit().putBoolean("safe_mode_enabled", newValue).apply()
        _userMessage.value = if (newValue) "Safe Mode ON (Adult content hidden)" else "Adult Content Unlocked"
        performSearch(_searchQuery.value)
    }

    fun getCurrentDateSeasonAndYear(): Pair<String, Int> {
        val now = java.time.LocalDate.now()
        val year = now.year
        val season = when (now.monthValue) {
            12, 1, 2 -> "WINTER"
            3, 4, 5 -> "SPRING"
            6, 7, 8 -> "SUMMER"
            else -> "FALL"
        }
        return Pair(season, year)
    }

    private val _discoverFilter = MutableStateFlow("ALL_TIME") // "ALL_TIME", "ONGOING", "UPCOMING", "SEASON"
    val discoverFilter = _discoverFilter.asStateFlow()

    private val _selectedSeason = MutableStateFlow<String?>(null) // Default unrestricted
    val selectedSeason = _selectedSeason.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null) // Default unrestricted
    val selectedYear = _selectedYear.asStateFlow()

    private val _filterState = MutableStateFlow(DiscoverFilterState())
    val filterState = _filterState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AnimeEntity>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private val _isNextPageLoading = MutableStateFlow(false)
    val isNextPageLoading = _isNextPageLoading.asStateFlow()

    private var currentPage = 1
    private var hasNextPage = true

    // Refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // Selected Anime Detail
    private val _selectedAnimeDetailId = MutableStateFlow<Int?>(null)
    val selectedAnimeDetailId = _selectedAnimeDetailId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedAnimeDetail: StateFlow<AnimeEntity?> = _selectedAnimeDetailId
        .flatMapLatest { id ->
            if (id != null) repository.getAnimeById(id) else kotlinx.coroutines.flow.flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    // Streaming Source & Player State
    private val streamingSourceRepository = com.example.data.repository.StreamingSourceRepository()

    private val _currentStreamingSource = MutableStateFlow<com.example.data.repository.StreamingSource?>(null)
    val currentStreamingSource = _currentStreamingSource.asStateFlow()

    private val _isStreamingLoading = MutableStateFlow(false)
    val isStreamingLoading = _isStreamingLoading.asStateFlow()

    private val _streamingError = MutableStateFlow<String?>(null)
    val streamingError = _streamingError.asStateFlow()

    fun loadStreamingSource(animeId: Int, episodeNumber: Int) {
        _isStreamingLoading.value = true
        _streamingError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val anime = repository.getAnimeByIdSync(animeId)
                if (anime != null) {
                    val source = streamingSourceRepository.getStreamingSource(anime, episodeNumber)
                    _currentStreamingSource.value = source
                } else {
                    _streamingError.value = "Anime details not found in database."
                }
            } catch (e: Exception) {
                _streamingError.value = e.localizedMessage ?: "Failed to load streaming source."
            } finally {
                _isStreamingLoading.value = false
            }
        }
    }

    // YouTube Integration: Keyless by default, optional Data API
    val youtubeRepository = YouTubeRepository()
    private val _isOptionalYouTubeApiEnabled = MutableStateFlow(
        prefs.getBoolean("optional_youtube_api_enabled", false)
    )
    val isOptionalYouTubeApiEnabled = _isOptionalYouTubeApiEnabled.asStateFlow()

    private val _keylessYouTubeSearchLinks = MutableStateFlow<List<YouTubeSearchLink>>(emptyList())
    val keylessYouTubeSearchLinks = _keylessYouTubeSearchLinks.asStateFlow()

    private val _aniListVerifiedTrailer = MutableStateFlow<YouTubeVideo?>(null)
    val aniListVerifiedTrailer = _aniListVerifiedTrailer.asStateFlow()

    private val _youtubeVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val youtubeVideos = _youtubeVideos.asStateFlow()
    private val _isYouTubeSearching = MutableStateFlow(false)
    val isYouTubeSearching = _isYouTubeSearching.asStateFlow()
    private val _youtubeSearchError = MutableStateFlow<String?>(null)
    val youtubeSearchError = _youtubeSearchError.asStateFlow()
    private val _isYouTubeQuotaExceeded = MutableStateFlow(false)
    val isYouTubeQuotaExceeded = _isYouTubeQuotaExceeded.asStateFlow()
    val isYouTubeKeyConfigured: Boolean
        get() = youtubeRepository.isApiKeyConfigured()

    private val _youtubeRegionCode = MutableStateFlow(prefs.getString("youtube_region_code", "IN") ?: "IN")
    val youtubeRegionCode = _youtubeRegionCode.asStateFlow()

    init {
        val isOptEnabled = prefs.getBoolean("optional_youtube_api_enabled", false)
        youtubeRepository.setOptionalApiEnabled(isOptEnabled)
        val savedCustomKey = prefs.getString("custom_youtube_api_key", null)
        if (!savedCustomKey.isNullOrBlank()) {
            youtubeRepository.setRuntimeApiKey(savedCustomKey)
        }
    }

    fun setOptionalYouTubeApiEnabled(enabled: Boolean) {
        _isOptionalYouTubeApiEnabled.value = enabled
        prefs.edit().putBoolean("optional_youtube_api_enabled", enabled).apply()
        youtubeRepository.setOptionalApiEnabled(enabled)
        if (!enabled) {
            _youtubeSearchError.value = null
            _isYouTubeQuotaExceeded.value = false
            _youtubeVideos.value = emptyList()
            _userMessage.value = "Switched to YouTube Keyless Mode (zero API key required)."
        } else {
            _userMessage.value = "Optional YouTube Data API enabled."
            if (youtubeRepository.isApiKeyConfigured()) {
                _videoCatalogSearchQuery.value.takeIf { it.isNotBlank() }?.let { searchYouTubeVideos(it) }
            }
        }
    }

    fun setCustomYouTubeApiKey(key: String?) {
        val clean = key?.trim()?.takeIf { it.isNotBlank() }
        youtubeRepository.setRuntimeApiKey(clean)
        if (clean != null) {
            prefs.edit().putString("custom_youtube_api_key", clean).apply()
            _userMessage.value = "Custom YouTube API Key saved."
        } else {
            prefs.edit().remove("custom_youtube_api_key").apply()
            _userMessage.value = "Reset to default build YouTube API Key."
        }
    }

    fun setYouTubeRegionCode(regionCode: String) {
        val clean = regionCode.trim().uppercase()
        _youtubeRegionCode.value = clean
        prefs.edit().putString("youtube_region_code", clean).apply()
        _userMessage.value = "YouTube video region set to: $clean"
    }

    fun searchYouTubeVideos(query: String, animeTitle: String? = null) {
        val q = query.trim()
        if (q.isBlank()) {
            _youtubeVideos.value = emptyList()
            _keylessYouTubeSearchLinks.value = emptyList()
            return
        }
        val targetTitle = animeTitle?.trim()?.takeIf { it.isNotBlank() } ?: q
        _keylessYouTubeSearchLinks.value = youtubeRepository.generateKeylessSearchLinks(targetTitle)

        if (!_isOptionalYouTubeApiEnabled.value) {
            // Keyless mode is active by default. No API network request, no missing key error.
            _youtubeVideos.value = emptyList()
            _isYouTubeSearching.value = false
            _youtubeSearchError.value = null
            _isYouTubeQuotaExceeded.value = false
            return
        }

        _isYouTubeSearching.value = true
        _youtubeSearchError.value = null
        _isYouTubeQuotaExceeded.value = false
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = youtubeRepository.searchAnimeVideos(q, regionCode = _youtubeRegionCode.value)) {
                is YouTubeSearchResultState.Success -> {
                    _youtubeVideos.value = result.videos
                    _isYouTubeSearching.value = false
                }
                is YouTubeSearchResultState.Keyless -> {
                    _keylessYouTubeSearchLinks.value = result.searchLinks
                    _youtubeVideos.value = emptyList()
                    _isYouTubeSearching.value = false
                }
                is YouTubeSearchResultState.Empty -> {
                    _youtubeVideos.value = emptyList()
                    _isYouTubeSearching.value = false
                }
                is YouTubeSearchResultState.Error -> {
                    _youtubeVideos.value = emptyList()
                    _youtubeSearchError.value = result.message
                    _isYouTubeQuotaExceeded.value = result.isQuotaExceeded
                    _isYouTubeSearching.value = false
                }
            }
        }
    }

    // Internet Archive integration
    val internetArchiveRepository = InternetArchiveRepository()
    private val _archiveVideos = MutableStateFlow<List<ArchiveMediaItem>>(emptyList())
    val archiveVideos = _archiveVideos.asStateFlow()
    private val _isArchiveSearching = MutableStateFlow(false)
    val isArchiveSearching = _isArchiveSearching.asStateFlow()
    private val _archiveSearchError = MutableStateFlow<String?>(null)
    val archiveSearchError = _archiveSearchError.asStateFlow()

    private val _selectedArchivePlayableMedia = MutableStateFlow<ArchivePlayableMedia?>(null)
    val selectedArchivePlayableMedia = _selectedArchivePlayableMedia.asStateFlow()
    private val _isResolvingArchiveMedia = MutableStateFlow(false)
    val isResolvingArchiveMedia = _isResolvingArchiveMedia.asStateFlow()
    private val _archiveResolutionError = MutableStateFlow<String?>(null)
    val archiveResolutionError = _archiveResolutionError.asStateFlow()

    fun searchInternetArchive(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            _archiveVideos.value = emptyList()
            return
        }
        _isArchiveSearching.value = true
        _archiveSearchError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = internetArchiveRepository.searchAnimeArchive(q)) {
                is ArchiveSearchResultState.Success -> {
                    _archiveVideos.value = result.items
                    _isArchiveSearching.value = false
                }
                is ArchiveSearchResultState.Empty -> {
                    _archiveVideos.value = emptyList()
                    _isArchiveSearching.value = false
                }
                is ArchiveSearchResultState.Error -> {
                    _archiveVideos.value = emptyList()
                    _archiveSearchError.value = result.message
                    _isArchiveSearching.value = false
                }
            }
        }
    }

    fun resolveArchiveMedia(identifier: String, onResolved: (ArchivePlayableMedia) -> Unit = {}) {
        _isResolvingArchiveMedia.value = true
        _archiveResolutionError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = internetArchiveRepository.inspectAndResolvePlayableMedia(identifier)) {
                is ArchivePlayableMediaResult.Success -> {
                    _selectedArchivePlayableMedia.value = result.media
                    _isResolvingArchiveMedia.value = false
                    withContext(Dispatchers.Main) {
                        onResolved(result.media)
                    }
                }
                is ArchivePlayableMediaResult.Unavailable -> {
                    _isResolvingArchiveMedia.value = false
                    _archiveResolutionError.value = result.reason
                    _userMessage.value = result.reason
                }
                is ArchivePlayableMediaResult.Error -> {
                    _isResolvingArchiveMedia.value = false
                    _archiveResolutionError.value = result.message
                    _userMessage.value = result.message
                }
            }
        }
    }

    // AniList OAuth2 integration
    val anilistOAuthManager = AniListOAuthManager(application)
    private val _isAniListLoggedIn = MutableStateFlow(anilistOAuthManager.isLoggedIn)
    val isAniListLoggedIn = _isAniListLoggedIn.asStateFlow()
    private val _anilistUserProfile = MutableStateFlow<AniListUserProfile?>(null)
    val anilistUserProfile = _anilistUserProfile.asStateFlow()
    private val _isAniListSyncing = MutableStateFlow(false)
    val isAniListSyncing = _isAniListSyncing.asStateFlow()

    fun handleAniListToken(token: String) {
        anilistOAuthManager.saveToken(token)
        _isAniListLoggedIn.value = true
        _userMessage.value = "AniList token saved. Fetching user profile..."
        fetchAniListProfileAndSync()
    }

    fun logoutAniList() {
        anilistOAuthManager.logout()
        _isAniListLoggedIn.value = false
        _anilistUserProfile.value = null
        _userMessage.value = "Signed out from AniList."
    }

    fun fetchAniListProfileAndSync() {
        if (!anilistOAuthManager.isLoggedIn) return
        _isAniListSyncing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = anilistOAuthManager.fetchCurrentUserProfile()
            if (result.isSuccess) {
                _anilistUserProfile.value = result.getOrNull()
                _userMessage.value = "Connected to AniList as ${result.getOrNull()?.name}"
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to fetch AniList profile."
            }
            _isAniListSyncing.value = false
        }
    }

    // Full Details with AniList characters, studios, relations & recommendations
    private val _selectedAnimeFullDetails = MutableStateFlow<AniListMedia?>(null)
    val selectedAnimeFullDetails = _selectedAnimeFullDetails.asStateFlow()
    private val _isLoadingFullDetails = MutableStateFlow(false)
    val isLoadingFullDetails = _isLoadingFullDetails.asStateFlow()

    private val _selectedDetailSeason = MutableStateFlow<AnimeSeasonInfo?>(null)
    val selectedDetailSeason = _selectedDetailSeason.asStateFlow()

    private val _activeDetailSeasons = MutableStateFlow<List<AnimeSeasonInfo>>(emptyList())
    val activeDetailSeasons = _activeDetailSeasons.asStateFlow()

    fun selectDetailSeason(season: AnimeSeasonInfo) {
        _selectedDetailSeason.value = season
    }

    fun loadFullDetails(animeId: Int) {
        _isLoadingFullDetails.value = true
        _aniListVerifiedTrailer.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val currentEntity = repository.getAnimeByIdSync(animeId)
            val result = repository.fetchAniListFullDetails(animeId)
            if (result.isSuccess) {
                val fullMedia = result.getOrNull()
                _selectedAnimeFullDetails.value = fullMedia
                val tr = fullMedia?.trailer
                if (tr != null && tr.site?.equals("youtube", ignoreCase = true) == true && !tr.id.isNullOrBlank()) {
                    val title = fullMedia.title?.english ?: fullMedia.title?.romaji ?: "Anime"
                    _aniListVerifiedTrailer.value = YouTubeVideo(
                        id = tr.id,
                        title = "$title - Official Trailer",
                        description = "Verified official trailer from AniList catalog",
                        channelTitle = "AniList Verified",
                        publishedAt = "",
                        thumbnailUrl = tr.thumbnail ?: "https://img.youtube.com/vi/${tr.id}/hqdefault.jpg",
                        durationFormatted = null,
                        isVerifiedOfficial = true,
                        videoType = YouTubeVideoType.TRAILER,
                        sourceBadge = "AniList Verified Trailer"
                    )
                }
                if (currentEntity != null) {
                    val resolvedSeasons = resolveFranchiseSeasons(currentEntity, fullMedia)
                    _activeDetailSeasons.value = resolvedSeasons
                    val matchedCurr = resolvedSeasons.firstOrNull { it.animeId == animeId } ?: resolvedSeasons.firstOrNull()
                    _selectedDetailSeason.value = matchedCurr
                }
            } else if (currentEntity != null) {
                val resolvedSeasons = resolveFranchiseSeasons(currentEntity, null)
                _activeDetailSeasons.value = resolvedSeasons
                _selectedDetailSeason.value = resolvedSeasons.firstOrNull()
            }
            _isLoadingFullDetails.value = false
        }
    }

    fun resolveFranchiseSeasons(anime: AnimeEntity, fullMedia: AniListMedia?): List<AnimeSeasonInfo> {
        val seasons = mutableListOf<AnimeSeasonInfo>()

        if (fullMedia?.relations?.edges != null) {
            val edges = fullMedia.relations.edges.orEmpty()
            val seasonNodes = edges.mapNotNull { edge ->
                val node = edge.node ?: return@mapNotNull null
                val type = edge.relationType?.uppercase() ?: ""
                val title = node.title?.english ?: node.title?.romaji ?: "Season"
                if (type in listOf("PREQUEL", "SEQUEL", "PARENT", "ALTERNATIVE", "SIDE_STORY") ||
                    title.contains("Season", ignoreCase = true) ||
                    title.contains("2nd", ignoreCase = true) ||
                    title.contains("3rd", ignoreCase = true)
                ) {
                    Pair(type, node)
                } else null
            }

            val prequels = seasonNodes.filter { it.first == "PREQUEL" }.map { it.second }.reversed()
            val sequels = seasonNodes.filter { it.first == "SEQUEL" }.map { it.second }
            val others = seasonNodes.filter { it.first !in listOf("PREQUEL", "SEQUEL") }.map { it.second }

            var sNum = 1
            var cumulativeStart = 1
            for (node in prequels) {
                val nodeTitle = node.title?.english ?: node.title?.romaji ?: "Season $sNum"
                val epCount = node.episodes
                seasons.add(
                    AnimeSeasonInfo(
                        seasonNumber = sNum++,
                        title = nodeTitle,
                        animeId = node.id,
                        totalEpisodes = epCount,
                        startEpisode = cumulativeStart,
                        coverImage = node.coverImage?.large ?: node.coverImage?.medium,
                        format = node.format,
                        isCurrentSeason = false
                    )
                )
                if (epCount != null && epCount > 0) {
                    cumulativeStart += epCount
                }
            }

            val currentSeasonEpisodes = anime.totalEpisodes
            seasons.add(
                AnimeSeasonInfo(
                    seasonNumber = sNum++,
                    title = anime.getResolvedTitle(_titleLanguagePreference.value),
                    animeId = anime.id,
                    idMal = anime.idMal,
                    totalEpisodes = currentSeasonEpisodes,
                    startEpisode = cumulativeStart,
                    coverImage = anime.coverImage,
                    bannerImage = anime.bannerImage,
                    format = anime.format,
                    isCurrentSeason = true
                )
            )
            if (currentSeasonEpisodes != null && currentSeasonEpisodes > 0) {
                cumulativeStart += currentSeasonEpisodes
            }

            for (node in sequels) {
                val nodeTitle = node.title?.english ?: node.title?.romaji ?: "Season $sNum"
                val epCount = node.episodes
                seasons.add(
                    AnimeSeasonInfo(
                        seasonNumber = sNum++,
                        title = nodeTitle,
                        animeId = node.id,
                        totalEpisodes = epCount,
                        startEpisode = cumulativeStart,
                        coverImage = node.coverImage?.large ?: node.coverImage?.medium,
                        format = node.format,
                        isCurrentSeason = false
                    )
                )
                if (epCount != null && epCount > 0) {
                    cumulativeStart += epCount
                }
            }

            for (node in others) {
                if (seasons.none { it.animeId == node.id }) {
                    val nodeTitle = node.title?.english ?: node.title?.romaji ?: "Season $sNum"
                    val epCount = node.episodes
                    seasons.add(
                        AnimeSeasonInfo(
                            seasonNumber = sNum++,
                            title = nodeTitle,
                            animeId = node.id,
                            totalEpisodes = epCount,
                            startEpisode = cumulativeStart,
                            coverImage = node.coverImage?.large ?: node.coverImage?.medium,
                            format = node.format,
                            isCurrentSeason = false
                        )
                    )
                    if (epCount != null && epCount > 0) {
                        cumulativeStart += epCount
                    }
                }
            }
        }

        if (seasons.isEmpty()) {
            seasons.add(
                AnimeSeasonInfo(
                    seasonNumber = 1,
                    title = anime.getResolvedTitle(_titleLanguagePreference.value),
                    animeId = anime.id,
                    idMal = anime.idMal,
                    totalEpisodes = anime.totalEpisodes,
                    coverImage = anime.coverImage,
                    bannerImage = anime.bannerImage,
                    format = anime.format,
                    isCurrentSeason = true
                )
            )
        }

        return seasons
    }

    // Unified Video Catalog
    private val _videoCatalogSearchQuery = MutableStateFlow("")
    val videoCatalogSearchQuery = _videoCatalogSearchQuery.asStateFlow()
    private val _videoCatalogSourceFilter = MutableStateFlow("ALL") // "ALL", "YOUTUBE", "ARCHIVE"
    val videoCatalogSourceFilter = _videoCatalogSourceFilter.asStateFlow()

    fun setVideoCatalogSourceFilter(filter: String) {
        _videoCatalogSourceFilter.value = filter
    }

    fun searchVideoCatalog(query: String) {
        _videoCatalogSearchQuery.value = query
        searchYouTubeVideos(query)
        searchInternetArchive(query)
    }

    // Tracking Management
    fun updateUserTracking(
        animeId: Int,
        status: String? = null,
        score: Int? = null,
        notes: String? = null,
        watchedCount: Int? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUserMediaState(
                animeId = animeId,
                userStatus = status,
                userScore = score,
                userNotes = notes,
                watchedCount = watchedCount
            )
            // Remote sync to AniList if user is authenticated with AniList
            if (anilistOAuthManager.isLoggedIn) {
                launch {
                    anilistOAuthManager.updateAniListEntry(
                        mediaId = animeId,
                        status = status,
                        progress = watchedCount,
                        scoreRaw = score
                    )
                }
            }
        }
    }

    fun confirmEpisodeCompletion(animeId: Int, episodeNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWatchedEpisodeCount(animeId, episodeNumber)
            _userMessage.value = "Episode $episodeNumber marked as completed!"
        }
    }

    // User Toast Messages
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Reconcile durable saved_anime table with anime table first so saved items are immediately available
                repository.reconcileSavedAnime()
                repository.seedInitialDataIfEmpty()
                repository.sanitizeAndFixEpisodeCounts()
                repository.reconcileSavedAnime()
            } catch (e: Exception) {
                // Never reset or clear saved lists on initialization error
            }
            performSearch("")
            // Auto refresh live schedule & upcoming release dates in background silently
            launch {
                refreshSchedule(showToast = false)
            }
            // Background async verification with MyAnimeList without delaying UI launch
            launch {
                repository.verifyAndSyncWithMal(malOAuthManager.getValidAccessToken())
            }
            if (malOAuthManager.isLoggedIn) {
                _isMalLoggedIn.value = true
                launch {
                    fetchMalProfileAndSync()
                }
            }
        }
    }

    val malClientId: String
        get() = malOAuthManager.effectiveClientId

    val malRedirectUri: String
        get() = malOAuthManager.effectiveRedirectUri

    fun setCustomMalConfig(clientId: String?, redirectUri: String?) {
        malOAuthManager.customClientId = clientId
        malOAuthManager.customRedirectUri = redirectUri
        _userMessage.value = "MAL OAuth configuration updated"
    }

    /**
     * Start Official MyAnimeList PKCE OAuth Flow
     */
    fun startMalOAuthFlow() {
        malOAuthManager.startOAuthFlow()
    }

    /**
     * Sets user error message from OAuth deep link or other operations
     */
    fun setAuthErrorMessage(message: String) {
        _userMessage.value = message
    }

    /**
     * Gets the full OAuth Authorization URL for in-app WebView or custom browser
     */
    fun getMalOAuthUrl(forceNew: Boolean = false): String {
        return malOAuthManager.buildOAuthAuthorizationUrl(forceNewVerifier = forceNew)
    }

    val connectedMalUsername: String?
        get() = malOAuthManager.connectedUsername

    /**
     * Connects user's MyAnimeList account by username. All data is encrypted with AES-256 GCM.
     */
    fun connectMalAccountByUsername(username: String) {
        viewModelScope.launch {
            val clean = username.trim()
            if (clean.isBlank()) {
                _userMessage.value = "Please enter your MyAnimeList username"
                return@launch
            }
            _isMalSyncing.value = true
            malOAuthManager.connectWithUsername(clean)
            _isMalLoggedIn.value = true
            _userMessage.value = "Connecting to @$clean..."
            fetchMalProfileAndSync(showToast = true)
        }
    }

    /**
     * Handles deep link callback or manual input from MyAnimeList OAuth redirect URI
     */
    fun handleMalOAuthCallback(authorizationCodeOrUrl: String) {
        viewModelScope.launch {
            _isMalSyncing.value = true
            val result = malOAuthManager.handleOAuthCallback(authorizationCodeOrUrl)
            if (result.isSuccess) {
                _isMalLoggedIn.value = true
                _isCloudLoggedIn.value = true
                _userMessage.value = "Authenticated with MyAnimeList! Syncing data..."
                fetchMalProfileAndSync(showToast = true)
            } else {
                _userMessage.value = "MAL Auth failed: ${result.exceptionOrNull()?.message}"
            }
            _isMalSyncing.value = false
        }
    }

    /**
     * Fetches authenticated user's MyAnimeList profile & anime list via OAuth 2.0 API, then updates Room database.
     * Accurately preserves AniList Donghua English titles and ongoing airing info while updating MAL user data.
     */
    fun fetchMalProfileAndSync(showToast: Boolean = false) {
        viewModelScope.launch {
            if (_isMalSyncing.value) return@launch
            _isMalSyncing.value = true
            val token = malOAuthManager.getValidAccessToken()
            if (token != null) {
                val profileRes = MyAnimeListService.getUserProfile(token)
                if (profileRes.isSuccess) {
                    val profile = profileRes.getOrNull()
                    _malUserProfile.value = profile
                    if (profile != null) {
                        _cloudUserName.value = profile.name
                    }
                }
                val listRes = MyAnimeListService.getUserAnimeList(token)
                if (listRes.isSuccess) {
                    val items = listRes.getOrNull() ?: emptyList()
                    for (item in items) {
                        val node = item.node
                        val listStatus = item.listStatus
                        val watched = listStatus?.numEpisodesWatched ?: 0
                        val status = listStatus?.status ?: "watching"
                        val score = listStatus?.score ?: 0
                        val canonicalEnglish = EpisodeDataParser.getCanonicalEnglish(node.title)
                        var existing = repository.getAnimeByMalIdSync(node.id)
                            ?: repository.getAnimeByIdSync(node.id)
                            ?: repository.getAnimeByTitleSync(node.title)
                            ?: if (canonicalEnglish != null) repository.getAnimeByTitleSync(canonicalEnglish) else null

                        val checkAnimeId = existing?.id ?: node.id
                        val checkMalId = node.id

                        if (repository.isDeleted(checkAnimeId, checkMalId, "ALL")) {
                            continue
                        }
                        val isWatchlistDeleted = repository.isDeleted(checkAnimeId, checkMalId, "WATCHLIST")
                        val isWishlistDeleted = repository.isDeleted(checkAnimeId, checkMalId, "WISHLIST")

                        val isWatched = status == "completed"
                        val rawWishlist = status == "plan_to_watch"
                        val malBroadcastDay = MyAnimeListService.parseMalDayOfWeek(node.broadcast?.dayOfTheWeek)
                        val targetWishlist = if (isWishlistDeleted) false else rawWishlist
                        val targetWatchlist = if (isWatchlistDeleted) false else (!targetWishlist && (existing?.inWatchlist ?: true))

                        if (existing != null) {
                            val mergedWatched = maxOf(existing.watchedEpisodeCount, watched)
                            if (existing.isDonghua) {
                                // STRICT REQUIREMENT: Preserve Donghua English title, ongoing episode count, and airing status from AniList.
                                // Only update user's personal watched episode count, watchlist state, and score.
                                val updated = existing.copy(
                                    idMal = node.id,
                                    watchedEpisodeCount = mergedWatched,
                                    isWatched = isWatched || existing.isWatched || (existing.totalEpisodes != null && mergedWatched >= existing.totalEpisodes),
                                    isWishlist = targetWishlist || existing.isWishlist,
                                    inWatchlist = targetWatchlist || existing.inWatchlist,
                                    score = if (score > 0) score else existing.score,
                                    userScore = if (score > 0) score else existing.userScore,
                                    userStatus = status
                                )
                                repository.updateAnime(updated)
                            } else {
                                val parsedInfo = EpisodeDataParser.parseMalEpisodes(
                                    malNumEpisodes = node.numEpisodes,
                                    malStatusStr = node.status,
                                    userWatchedEpisodes = mergedWatched,
                                    existingAired = existing.currentEpisode
                                )
                                val updatedDay = if (malBroadcastDay in 1..7) malBroadcastDay else existing.airingDayOfWeek
                                val updatedAired = maxOf(existing.currentEpisode, parsedInfo.airedEpisodeCount)
                                val updatedTotal = if (existing.totalEpisodes != null && existing.totalEpisodes > 0) {
                                    if (parsedInfo.totalEpisodes != null && parsedInfo.totalEpisodes >= updatedAired) {
                                        parsedInfo.totalEpisodes
                                    } else {
                                        existing.totalEpisodes
                                    }
                                } else {
                                    parsedInfo.totalEpisodes
                                }
                                val isShowFinished = existing.status == "FINISHED" || (parsedInfo.isFinished && (updatedTotal != null && updatedAired >= updatedTotal))
                                val nextEp = if (isShowFinished) updatedAired else maxOf(existing.nextEpisodeNumber ?: (updatedAired + 1), updatedAired + 1)
                                val updated = existing.copy(
                                    idMal = node.id,
                                    watchedEpisodeCount = mergedWatched,
                                    currentEpisode = updatedAired,
                                    nextEpisodeNumber = nextEp,
                                    totalEpisodes = updatedTotal,
                                    status = if (isShowFinished) "FINISHED" else existing.status,
                                    isWatched = isWatched || existing.isWatched || (updatedTotal != null && mergedWatched >= updatedTotal),
                                    isWishlist = targetWishlist || existing.isWishlist,
                                    inWatchlist = targetWatchlist || existing.inWatchlist,
                                    airingDayOfWeek = updatedDay,
                                    score = if (score > 0) score else existing.score,
                                    userScore = if (score > 0) score else existing.userScore,
                                    userStatus = status
                                )
                                repository.updateAnime(updated)
                            }
                        } else {
                            val resolvedEnglish = canonicalEnglish ?: node.title
                            val isDonghua = node.title.contains("Douluo", true) || node.title.contains("Doupo", true) || node.title.contains("Wanmei", true) || node.title.contains("Tunshi", true)
                            val parsedInfo = EpisodeDataParser.parseMalEpisodes(
                                malNumEpisodes = node.numEpisodes,
                                malStatusStr = node.status,
                                userWatchedEpisodes = watched,
                                existingAired = maxOf(watched, 1)
                            )
                            val resolvedDay = if (malBroadcastDay in 1..7) {
                                malBroadcastDay
                            } else {
                                ((node.id.coerceAtLeast(1) % 7) + 1)
                            }
                            val initialAired = maxOf(parsedInfo.airedEpisodeCount, watched, 1)
                            val newEntity = AnimeEntity(
                                id = node.id,
                                idMal = node.id,
                                titleRomaji = node.title,
                                titleEnglish = resolvedEnglish,
                                coverImage = node.mainPicture?.large ?: node.mainPicture?.medium ?: "",
                                totalEpisodes = parsedInfo.totalEpisodes,
                                currentEpisode = initialAired,
                                nextEpisodeNumber = if (parsedInfo.isFinished) initialAired else (initialAired + 1),
                                watchedEpisodeCount = watched,
                                status = if (parsedInfo.isFinished) "FINISHED" else "RELEASING",
                                airingDayOfWeek = resolvedDay,
                                inWatchlist = targetWatchlist,
                                isWishlist = targetWishlist,
                                isWatched = isWatched,
                                score = score,
                                userScore = score,
                                userStatus = status,
                                countryOfOrigin = if (isDonghua) "CN" else "JP",
                                synopsis = node.synopsis
                            )
                            if (targetWatchlist) {
                                repository.addToWatchlist(newEntity)
                            } else {
                                repository.updateAnime(newEntity)
                            }
                        }
                    }
                    repository.ensureDatabaseDeduplicated()
                    _lastCloudSyncTime.value = System.currentTimeMillis()
                    if (showToast) {
                        _userMessage.value = "Synced ${items.size} titles from MyAnimeList"
                    }
                }
            } else if (!malOAuthManager.connectedUsername.isNullOrBlank()) {
                val user = malOAuthManager.connectedUsername!!
                val profileRes = MyAnimeListService.fetchUserPublicProfile(user)
                if (profileRes.isSuccess) {
                    _malUserProfile.value = profileRes.getOrNull()
                }
                val listRes = MyAnimeListService.fetchUserPublicAnimeList(user)
                if (listRes.isSuccess) {
                    val items = listRes.getOrNull() ?: emptyList()
                    for (item in items) {
                        val node = item.node ?: continue
                        val listStatus = item.listStatus
                        val watched = listStatus?.numEpisodesWatched ?: 0
                        val status = listStatus?.status ?: "watching"
                        val score = listStatus?.score ?: 0
                        val canonicalEnglish = EpisodeDataParser.getCanonicalEnglish(node.title)
                        var existing = repository.getAnimeByMalIdSync(node.id)
                            ?: repository.getAnimeByIdSync(node.id)
                            ?: repository.getAnimeByTitleSync(node.title)
                            ?: if (canonicalEnglish != null) repository.getAnimeByTitleSync(canonicalEnglish) else null

                        if (existing != null) {
                            val isWatched = status == "completed"
                            val isWishlist = status == "plan_to_watch"
                            val updated = existing.copy(
                                idMal = node.id,
                                watchedEpisodeCount = watched,
                                isWatched = isWatched || (existing.totalEpisodes != null && watched >= existing.totalEpisodes),
                                isWishlist = isWishlist,
                                inWatchlist = !isWishlist && existing.inWatchlist,
                                userScore = score,
                                userStatus = status
                            )
                            repository.updateAnime(updated)
                        }
                    }
                    repository.ensureDatabaseDeduplicated()
                    _lastCloudSyncTime.value = System.currentTimeMillis()
                    if (showToast) {
                        _userMessage.value = "Synced ${items.size} titles for @$user"
                    }
                }
            } else {
                _isMalLoggedIn.value = false
            }
            _isMalSyncing.value = false
        }
    }

    /**
     * Executes real, 100% verified synchronization with MyAnimeList API.
     */
    fun syncWatchlistToMal() {
        viewModelScope.launch {
            _isMalSyncing.value = true
            val token = malOAuthManager.getValidAccessToken()
            if (token == null) {
                _isMalLoggedIn.value = false
                _userMessage.value = "Sync failed: Not authenticated with MyAnimeList. Please connect your account."
                _isMalSyncing.value = false
                return@launch
            }

            val currentWatchlist = watchlist.value
            if (currentWatchlist.isEmpty()) {
                _userMessage.value = "Watchlist is empty. Nothing to sync."
                _isMalSyncing.value = false
                return@launch
            }

            val newLogs = mutableListOf<MalSyncLog>()
            var verifiedCount = 0

            // Parallel batch processing in chunks of 4 for fast, high-speed MAL syncing
            val chunkedList = currentWatchlist.chunked(4)
            var processed = 0
            for (chunk in chunkedList) {
                val results = coroutineScope {
                    val jobs = chunk.map { anime ->
                        async(Dispatchers.IO) {
                            var targetMalId = anime.idMal
                            if (targetMalId == null || targetMalId <= 0) {
                                val titleToSearch = anime.titleEnglish ?: anime.titleRomaji
                                val searchRes = MyAnimeListService.searchMalAnimeByTitle(token, titleToSearch)
                                if (searchRes.isSuccess) {
                                    targetMalId = searchRes.getOrNull()
                                    if (targetMalId != null && targetMalId > 0) {
                                        repository.updateAnime(anime.copy(idMal = targetMalId))
                                    }
                                }
                            }

                            if (targetMalId == null || targetMalId <= 0) {
                                Pair(
                                    MalSyncLog(
                                        animeTitle = anime.titleRomaji,
                                        malId = 0,
                                        httpStatusCode = 404,
                                        requestUrl = "${com.example.data.remote.MyAnimeListConfig.API_BASE_URL}anime?q=${anime.titleRomaji}",
                                        responseBody = "{\"error\":\"not_found\",\"message\":\"Could not resolve official MAL ID\"}",
                                        success = false,
                                        verificationPassed = false,
                                        message = "Sync failed: Unable to locate official MAL ID for '${anime.titleRomaji}'"
                                    ),
                                    false
                                )
                            } else {
                                val targetStatus = when {
                                    anime.isWishlist -> "plan_to_watch"
                                    anime.isWatched || (anime.totalEpisodes != null && anime.watchedEpisodeCount >= anime.totalEpisodes) -> "completed"
                                    else -> "watching"
                                }
                                val log = MyAnimeListService.updateAndVerifyAnimeStatusDetailed(
                                    accessToken = token,
                                    animeTitle = anime.titleEnglish ?: anime.titleRomaji,
                                    malId = targetMalId,
                                    numEpisodesWatched = anime.watchedEpisodeCount,
                                    status = targetStatus,
                                    score = anime.score
                                )
                                Pair(log, log.success && log.verificationPassed)
                            }
                        }
                    }
                    jobs.awaitAll()
                }

                for (res in results) {
                    val log = res.first
                    val verified = res.second
                    newLogs.add(0, log)
                    if (verified) verifiedCount++
                    processed++
                }
                _userMessage.value = "Syncing with MyAnimeList... ($processed/${currentWatchlist.size})"
                delay(100L)
            }

            _malSyncLogs.value = newLogs + _malSyncLogs.value
            _isMalSyncing.value = false

            if (verifiedCount == currentWatchlist.size) {
                _lastCloudSyncTime.value = System.currentTimeMillis()
                _userMessage.value = "Successfully synced and verified $verifiedCount/${currentWatchlist.size} anime on MyAnimeList!"
            } else if (verifiedCount > 0) {
                _lastCloudSyncTime.value = System.currentTimeMillis()
                _userMessage.value = "Synced $verifiedCount/${currentWatchlist.size} anime. ${currentWatchlist.size - verifiedCount} failed."
            } else {
                _userMessage.value = "Sync failed: 0/${currentWatchlist.size} anime verified on MyAnimeList."
            }
        }
    }

    /**
     * Updates an anime entry's status directly on MyAnimeList with real verification and error handling.
     * Status options: "plan_to_watch", "watching", "completed", "on_hold", "dropped".
     */
    fun updateAnimeMalStatus(
        anime: AnimeEntity,
        malStatus: String,
        watchedCount: Int? = null,
        showToast: Boolean = false,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val token = malOAuthManager.getValidAccessToken()
            if (token == null) {
                _userMessage.value = "Connect MyAnimeList account in Settings to synchronize online."
                onResult?.invoke(false)
                return@launch
            }
            _isMalSyncing.value = true

            // Resolve official MAL ID
            var targetMalId = anime.idMal
            val titleToSearch = anime.titleEnglish ?: anime.titleRomaji
            if (targetMalId == null || targetMalId <= 0) {
                val searchRes = MyAnimeListService.searchMalAnimeByTitle(token, titleToSearch)
                if (searchRes.isSuccess) {
                    targetMalId = searchRes.getOrNull()
                    if (targetMalId != null && targetMalId > 0) {
                        repository.updateAnime(anime.copy(idMal = targetMalId))
                    }
                }
            }

            if (targetMalId == null || targetMalId <= 0) {
                _userMessage.value = "Sync failed: Could not resolve official MAL ID for '$titleToSearch'"
                _isMalSyncing.value = false
                onResult?.invoke(false)
                return@launch
            }

            val targetEpCount = watchedCount ?: anime.watchedEpisodeCount
            var log = MyAnimeListService.updateAndVerifyAnimeStatusDetailed(
                accessToken = token,
                animeTitle = titleToSearch,
                malId = targetMalId,
                numEpisodesWatched = targetEpCount,
                status = malStatus,
                score = anime.score
            )

            // Retry logic if MAL ID was invalid or HTTP 400 occurred
            if (!log.success && (log.httpStatusCode == 400 || log.httpStatusCode == 404)) {
                val searchRes = MyAnimeListService.searchMalAnimeByTitle(token, titleToSearch)
                val reResolvedMalId = searchRes.getOrNull()
                if (reResolvedMalId != null && reResolvedMalId > 0 && reResolvedMalId != targetMalId) {
                    targetMalId = reResolvedMalId
                    repository.updateAnime(anime.copy(idMal = targetMalId))
                    log = MyAnimeListService.updateAndVerifyAnimeStatusDetailed(
                        accessToken = token,
                        animeTitle = titleToSearch,
                        malId = targetMalId,
                        numEpisodesWatched = targetEpCount,
                        status = malStatus,
                        score = anime.score
                    )
                }
            }

            val currentLogs = _malSyncLogs.value.toMutableList()
            currentLogs.add(0, log)
            _malSyncLogs.value = currentLogs
            _isMalSyncing.value = false

            if (log.success && log.verificationPassed) {
                _lastCloudSyncTime.value = System.currentTimeMillis()
                if (showToast) {
                    _userMessage.value = "Updated '$titleToSearch' to '$malStatus' on MyAnimeList (Verified)"
                }
                val cleanMalStatus = MyAnimeListService.normalizeMalStatus(malStatus)
                val isWatched = cleanMalStatus == "completed" || (anime.totalEpisodes != null && targetEpCount >= anime.totalEpisodes)
                val isWishlist = cleanMalStatus == "plan_to_watch"
                val updatedEntity = anime.copy(
                    idMal = targetMalId,
                    watchedEpisodeCount = targetEpCount,
                    isWatched = isWatched,
                    isWishlist = isWishlist,
                    inWatchlist = !isWishlist
                )
                repository.updateAnime(updatedEntity)
                onResult?.invoke(true)
            } else {
                _userMessage.value = "MAL Sync failed for '$titleToSearch': ${log.message}"
                onResult?.invoke(false)
            }
        }
    }

    fun removeAnimeFromMal(anime: AnimeEntity) {
        viewModelScope.launch {
            val token = malOAuthManager.getValidAccessToken() ?: return@launch
            var targetMalId = anime.idMal
            val titleToSearch = anime.titleEnglish ?: anime.titleRomaji
            if (targetMalId == null || targetMalId <= 0) {
                val searchRes = MyAnimeListService.searchMalAnimeByTitle(token, titleToSearch)
                targetMalId = searchRes.getOrNull()
            }
            if (targetMalId != null && targetMalId > 0) {
                val res = MyAnimeListService.deleteAnimeFromMal(token, targetMalId)
                if (res.isSuccess) {
                    _userMessage.value = "Removed '$titleToSearch' from MyAnimeList"
                }
            }
        }
    }

    fun clearSyncLogs() {
        _malSyncLogs.value = emptyList()
        _userMessage.value = "Sync logs cleared"
    }

    /**
     * Updates watch progress locally and synchronizes directly with MyAnimeList API.
     */
    fun updateWatchedEpisodeCount(animeId: Int, newCount: Int, malStatus: String = "watching", showToast: Boolean = false) {
        viewModelScope.launch {
            val oldAnime = repository.getAnimeByIdSync(animeId) ?: return@launch
            val oldCount = oldAnime.watchedEpisodeCount
            // Optimistic update
            repository.updateWatchedEpisodeCount(animeId, newCount)

            if (_isMalLoggedIn.value) {
                val anime = repository.getAnimeByIdSync(animeId) ?: oldAnime
                val statusToSend = if (anime.totalEpisodes != null && newCount >= anime.totalEpisodes) "completed" else malStatus
                updateAnimeMalStatus(anime = anime, malStatus = statusToSend, watchedCount = newCount, showToast = showToast) { success ->
                    if (!success) {
                        viewModelScope.launch {
                            repository.updateWatchedEpisodeCount(animeId, oldCount)
                            _userMessage.value = "MAL sync rejected update. Episode reverted to Ep $oldCount."
                        }
                    }
                }
            } else if (showToast) {
                _userMessage.value = "Updated watched episode to Ep $newCount"
            }
        }
    }

    /**
     * Updates media watch progress, user status, user rating score, and dates locally.
     * Synchronizes with MyAnimeList if title has a MAL ID and MAL account is logged in.
     */
    fun updateUserMediaState(
        animeId: Int,
        userStatus: String? = null,
        userScore: Int? = null,
        watchedCount: Int? = null,
        startDate: String? = null,
        finishDate: String? = null
    ) {
        viewModelScope.launch {
            val token = if (malOAuthManager.isLoggedIn) malOAuthManager.getValidAccessToken() else null
            val result = repository.updateUserMediaState(
                animeId = animeId,
                userStatus = userStatus,
                userScore = userScore,
                watchedCount = watchedCount,
                startDate = startDate,
                finishDate = finishDate,
                accessToken = token
            )
            if (result.isSuccess) {
                _userMessage.value = "Updated media status & progress"
            }
        }
    }

    /**
     * Adds a custom local title untracked or missing from MyAnimeList.
     */
    fun addNewLocalTitle(
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
    ) {
        viewModelScope.launch {
            val created = repository.addNewLocalTitle(
                title = title,
                titleNative = titleNative,
                titlePinyin = titlePinyin,
                countryOfOrigin = countryOfOrigin,
                totalEpisodes = totalEpisodes,
                currentEpisode = currentEpisode,
                synopsis = synopsis,
                coverImage = coverImage,
                format = format,
                genres = genres
            )
            _userMessage.value = "Added '${created.displayTitle}' locally (Untracked on MAL)"
        }
    }

    /**
     * Logout from MyAnimeList
     */
    fun logoutMal() {
        malOAuthManager.logout()
        _isMalLoggedIn.value = false
        _malUserProfile.value = null
        _userMessage.value = "Signed out of MyAnimeList"
    }

    fun setThemeColor(colorName: String) {
        _themeColor.value = colorName
        _userMessage.value = "Theme updated to ${colorName.replace("_", " ")}"
    }

    fun loginCloud(email: String? = null, name: String? = null) {
        viewModelScope.launch {
            _isCloudLoggedIn.value = true
            _cloudUserEmail.value = email
            _cloudUserName.value = name ?: _malUserProfile.value?.name
            syncScheduleAndMAL()
        }
    }

    fun logoutCloud() {
        _isCloudLoggedIn.value = false
        _cloudUserEmail.value = null
        _cloudUserName.value = null
        logoutMal()
        _userMessage.value = "Signed out"
    }

    fun syncScheduleAndMAL() {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            repository.refreshSchedule()
            if (_isMalLoggedIn.value) {
                fetchMalProfileAndSync()
            } else {
                _lastCloudSyncTime.value = System.currentTimeMillis()
                _isCloudSyncing.value = false
                _userMessage.value = "Schedule refreshed & synced!"
            }
        }
    }

    fun toggleFeature(featureKey: String) {
        val current = _enabledFeatures.value.toMutableSet()
        if (current.contains(featureKey)) {
            current.remove(featureKey)
            _userMessage.value = "Feature disabled"
        } else {
            current.add(featureKey)
            _userMessage.value = "Feature enabled"
        }
        _enabledFeatures.value = current
    }

    fun askAiCraft(prompt: String) {
        viewModelScope.launch {
            _aiCraftingState.value = true
            val response = AiVerificationService.craftFeatureWithAi(prompt)
            _aiCraftingState.value = false
            val currentList = _aiCraftResults.value.toMutableList()
            currentList.add(0, response)
            _aiCraftResults.value = currentList
            _userMessage.value = "Feature recommendations generated!"
        }
    }

    fun setStealthMode(enabled: Boolean) {
        _isStealthModeEnabled.value = enabled
        _userMessage.value = if (enabled) "Stealth Camouflage Mode Active" else "Stealth Mode Disabled"
    }

    fun setDay(day: Int) {
        selectedDay.value = day
    }

    fun setMediaType(mediaType: String) {
        val current = _filterState.value
        val updated = current.copy(mediaType = mediaType)
        _filterState.value = updated
        if (mediaType == "DONGHUA") {
            _discoverFilter.value = "DONGHUA"
        } else if (_discoverFilter.value == "DONGHUA") {
            _discoverFilter.value = "ALL_TIME"
        }
        performSearch(_searchQuery.value)
    }

    fun updateFilterState(newFilter: DiscoverFilterState) {
        _filterState.value = newFilter
        _selectedSeason.value = newFilter.season
        _selectedYear.value = newFilter.yearFrom
        performSearch(_searchQuery.value)
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _filterState.value = DiscoverFilterState()
        _selectedSeason.value = null
        _selectedYear.value = null
        _selectedGenre.value = "ALL"
        _discoverFilter.value = "ALL_TIME"
        _searchError.value = null
        performSearch("")
    }

    fun toggleGenreFilter(genre: String) {
        val current = _filterState.value.selectedGenres
        val updated = if (current.contains(genre)) current - genre else current + genre
        _filterState.value = _filterState.value.copy(selectedGenres = updated)
        performSearch(_searchQuery.value)
    }

    fun setSortBy(sort: String) {
        _filterState.value = _filterState.value.copy(sortBy = sort)
        performSearch(_searchQuery.value)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        performSearch(query)
    }

    fun refreshSearch() {
        performSearch(_searchQuery.value)
    }

    fun onGenreSelected(genre: String) {
        _selectedGenre.value = genre
        val current = _filterState.value.selectedGenres
        val updated = if (genre == "ALL" || genre == "All") emptySet() else setOf(genre)
        _filterState.value = _filterState.value.copy(selectedGenres = updated)
        performSearch(_searchQuery.value)
    }

    fun setDiscoverFilter(filter: String) {
        _discoverFilter.value = filter
        val currentMediaType = _filterState.value.mediaType
        val mediaType = if (filter == "DONGHUA") "DONGHUA" else currentMediaType
        val status = when (filter) {
            "ONGOING" -> "RELEASING"
            "UPCOMING" -> "NOT_YET_RELEASED"
            else -> "ALL"
        }
        _filterState.value = _filterState.value.copy(mediaType = mediaType, status = status)
        performSearch(_searchQuery.value)
    }

    fun setSelectedSeason(season: String?) {
        _selectedSeason.value = season
        _filterState.value = _filterState.value.copy(season = season)
        performSearch(_searchQuery.value)
    }

    fun setSelectedYear(year: Int?) {
        _selectedYear.value = year
        _filterState.value = _filterState.value.copy(yearFrom = year, yearTo = year)
        performSearch(_searchQuery.value)
    }

    fun setSeasonAndYear(season: String?, year: Int?) {
        _selectedSeason.value = season
        _selectedYear.value = year
        _filterState.value = _filterState.value.copy(season = season, yearFrom = year, yearTo = year)
        performSearch(_searchQuery.value)
    }

    private var searchJob: Job? = null

    private fun applyPostFilters(list: List<AnimeEntity>, filter: DiscoverFilterState): List<AnimeEntity> {
        var result = list

        // Safe Mode Filter
        if (_safeModeEnabled.value) {
            result = result.filter { !it.isAdult }
        }

        // Media Type Filter
        if (filter.mediaType == "ANIME") {
            result = result.filter { !it.isDonghua }
        } else if (filter.mediaType == "DONGHUA") {
            result = result.filter { it.isDonghua }
        }

        // Airing Status Filter
        if (filter.status != "ALL") {
            result = result.filter { entity ->
                when (filter.status) {
                    "RELEASING", "ONGOING" -> entity.status == "RELEASING"
                    "UPCOMING", "NOT_YET_RELEASED" -> entity.status == "NOT_YET_RELEASED"
                    "FINISHED" -> entity.status == "FINISHED" || entity.status == "finished_airing"
                    else -> true
                }
            }
        }

        // Season Filter (Supporting Autumn & Fall interchangeably)
        if (filter.season != null && filter.season != "ALL") {
            val isAutumn = filter.season.equals("AUTUMN", ignoreCase = true) || filter.season.equals("FALL", ignoreCase = true)
            result = result.filter { anime ->
                if (isAutumn) {
                    anime.season.equals("FALL", ignoreCase = true) || anime.season.equals("AUTUMN", ignoreCase = true)
                } else {
                    anime.season.equals(filter.season, ignoreCase = true)
                }
            }
        }

        // Year Range Filter
        if (filter.yearFrom != null) {
            result = result.filter { entity ->
                entity.seasonYear == null || entity.seasonYear >= filter.yearFrom
            }
        }
        if (filter.yearTo != null) {
            result = result.filter { entity ->
                entity.seasonYear == null || entity.seasonYear <= filter.yearTo
            }
        }

        // Genres Filter
        if (filter.selectedGenres.isNotEmpty()) {
            result = result.filter { entity ->
                val entityGenres = entity.genres.split(",").map { g -> g.trim().lowercase() }
                if (filter.genreMatchAll) {
                    filter.selectedGenres.all { sel -> entityGenres.any { eg -> eg.contains(sel.lowercase()) } }
                } else {
                    filter.selectedGenres.any { sel -> entityGenres.any { eg -> eg.contains(sel.lowercase()) } }
                }
            }
        }

        // Format Filter
        if (filter.format != "ALL") {
            result = result.filter { it.format.equals(filter.format, ignoreCase = true) }
        }

        // Country Filter
        if (filter.country != "ALL") {
            result = result.filter { it.countryOfOrigin.equals(filter.country, ignoreCase = true) }
        }

        // MAL Availability Filter
        if (filter.malAvailability == "MAL_ONLY") {
            result = result.filter { it.idMal != null }
        } else if (filter.malAvailability == "LOCAL_ONLY") {
            result = result.filter { it.idMal == null }
        }

        // Sorting
        result = when (filter.sortBy) {
            "SCORE_DESC" -> result.sortedByDescending { it.score ?: 0 }
            "START_DATE_DESC" -> result.sortedByDescending { it.seasonYear ?: 0 }
            "TITLE_ROMAJI" -> result.sortedBy { it.displayTitle.lowercase() }
            "EPISODES_DESC" -> result.sortedByDescending { it.totalEpisodes ?: 0 }
            else -> result
        }

        return result
    }

    private fun getAniListSort(sortKey: String, hasSearch: Boolean): List<String> {
        if (hasSearch) return listOf("SEARCH_MATCH", "POPULARITY_DESC")
        return when (sortKey) {
            "SCORE_DESC" -> listOf("SCORE_DESC")
            "START_DATE_DESC" -> listOf("START_DATE_DESC")
            "TITLE_ROMAJI" -> listOf("TITLE_ROMAJI")
            "EPISODES_DESC" -> listOf("EPISODES_DESC")
            else -> listOf("POPULARITY_DESC")
        }
    }

    fun ensureDiscoverLoaded() {
        if (_searchResults.value.isEmpty() && !_isSearching.value) {
            performSearch(_searchQuery.value)
        }
    }

    fun loadNextPage() {
        if (_isNextPageLoading.value || !hasNextPage || _isSearching.value) return
        val query = _searchQuery.value
        val filter = _filterState.value
        viewModelScope.launch(Dispatchers.IO) {
            _isNextPageLoading.value = true
            val nextPage = currentPage + 1
            val isDonghuaFilter = filter.mediaType == "DONGHUA" || filter.country == "CN"
            val status = when (filter.status) {
                "RELEASING", "ONGOING" -> "RELEASING"
                "UPCOMING", "NOT_YET_RELEASED" -> "NOT_YET_RELEASED"
                "FINISHED" -> "FINISHED"
                else -> null
            }
            val countryCode = if (isDonghuaFilter) "CN" else if (filter.country != "ALL") filter.country else if (filter.mediaType == "ANIME") "JP" else null
            val primaryGenre = filter.selectedGenres.firstOrNull() ?: if (_selectedGenre.value != "ALL") _selectedGenre.value else null
            val rawSeason = filter.season
            val season = if (rawSeason?.equals("AUTUMN", ignoreCase = true) == true) "FALL" else rawSeason
            val year = filter.yearFrom ?: filter.yearTo
            val apiSort = getAniListSort(filter.sortBy, query.isNotEmpty())

            try {
                val result = repository.searchAniList(
                    query = query,
                    genre = primaryGenre,
                    season = season,
                    seasonYear = year,
                    status = status,
                    countryOfOrigin = countryCode,
                    sort = apiSort,
                    page = nextPage
                )
                val searchRes = result.getOrNull()
                if (searchRes != null && searchRes.items.isNotEmpty()) {
                    currentPage = nextPage
                    hasNextPage = searchRes.hasNextPage
                    val currentList = _searchResults.value
                    val combined = (currentList + searchRes.items).distinctBy { it.id }
                    _searchResults.value = applyPostFilters(combined, filter)
                } else {
                    hasNextPage = false
                }
            } catch (e: Exception) {
                // Ignore pagination errors
            } finally {
                _isNextPageLoading.value = false
            }
        }
    }

    fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            if (query.isNotEmpty()) {
                delay(200)
            }
            currentPage = 1
            hasNextPage = true
            _isNextPageLoading.value = false
            _searchError.value = null
            _isSearching.value = true

            val filter = _filterState.value
            val isDonghuaFilter = filter.mediaType == "DONGHUA" || filter.country == "CN"
            val status = when (filter.status) {
                "RELEASING", "ONGOING" -> "RELEASING"
                "UPCOMING", "NOT_YET_RELEASED" -> "NOT_YET_RELEASED"
                "FINISHED" -> "FINISHED"
                else -> null
            }
            val countryCode = if (isDonghuaFilter) "CN" else if (filter.country != "ALL") filter.country else if (filter.mediaType == "ANIME") "JP" else null
            val primaryGenre = filter.selectedGenres.firstOrNull() ?: if (_selectedGenre.value != "ALL") _selectedGenre.value else null
            val rawSeason = filter.season
            val season = if (rawSeason?.equals("AUTUMN", ignoreCase = true) == true) "FALL" else rawSeason
            val year = filter.yearFrom ?: filter.yearTo
            val apiSort = getAniListSort(filter.sortBy, query.isNotEmpty())

            // 1. Local database search
            val localList = repository.getAllLocalAnime(query, primaryGenre)
            val filteredLocal = applyPostFilters(localList, filter)
            
            if (filteredLocal.isNotEmpty()) {
                _searchResults.value = filteredLocal
            }

            // 2. AniList API Online Search
            try {
                val result = repository.searchAniList(
                    query = query,
                    genre = primaryGenre,
                    season = season,
                    seasonYear = year,
                    status = status,
                    countryOfOrigin = countryCode,
                    sort = apiSort,
                    page = 1
                )
                if (result.isSuccess) {
                    val searchRes = result.getOrNull()
                    val list = searchRes?.items ?: emptyList()
                    hasNextPage = searchRes?.hasNextPage ?: false
                    val combinedList = (filteredLocal + list).distinctBy { it.id }
                    _searchResults.value = applyPostFilters(combinedList, filter)
                    _searchError.value = null
                } else {
                    val exception = result.exceptionOrNull()
                    if (_searchResults.value.isEmpty()) {
                        _searchError.value = exception?.message ?: "Unable to fetch catalogue from AniList."
                    }
                }
            } catch (e: Exception) {
                if (_searchResults.value.isEmpty()) {
                    _searchError.value = e.message ?: "Network error while loading catalogue."
                }
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun removeAnime(
        anime: AnimeEntity,
        scope: RemovalScope = RemovalScope.BOTH,
        listType: String = "WATCHLIST"
    ) {
        viewModelScope.launch {
            val title = anime.displayTitle
            val token = malOAuthManager.getValidAccessToken()
            when (scope) {
                RemovalScope.LOCAL_ONLY -> {
                    if (listType == "WISHLIST") {
                        repository.removeFromWishlist(anime.id, anime.idMal)
                        _userMessage.value = "Removed '$title' from Wishlist"
                    } else if (listType == "ALL") {
                        repository.deleteAnimeCompletely(anime.id, anime.idMal)
                        _userMessage.value = "Removed '$title' from AniTrAk"
                    } else {
                        repository.removeFromPlanner(anime.id, anime.idMal)
                        _userMessage.value = "Removed '$title' from Planner"
                    }
                }
                RemovalScope.PROVIDER_ONLY -> {
                    if (!token.isNullOrBlank()) {
                        var malId = anime.idMal
                        if (malId == null || malId <= 0) {
                            val searchRes = MyAnimeListService.searchMalAnimeByTitle(token, title)
                            malId = searchRes.getOrNull()
                        }
                        if (malId != null && malId > 0) {
                            val res = MyAnimeListService.deleteAnimeFromMal(token, malId)
                            if (res.isSuccess) {
                                _userMessage.value = "Removed '$title' from MyAnimeList"
                            } else {
                                _userMessage.value = "Failed to remove '$title' from MyAnimeList"
                            }
                        } else {
                            _userMessage.value = "Could not locate MAL entry for '$title'"
                        }
                    } else {
                        _userMessage.value = "Not connected to MyAnimeList"
                    }
                }
                RemovalScope.BOTH -> {
                    if (listType == "WISHLIST") {
                        repository.removeFromWishlist(anime.id, anime.idMal)
                    } else if (listType == "ALL") {
                        repository.deleteAnimeCompletely(anime.id, anime.idMal)
                    } else {
                        repository.removeFromWatchlist(anime.id, anime.idMal)
                    }
                    if (_isMalLoggedIn.value && !token.isNullOrBlank()) {
                        removeAnimeFromMal(anime)
                        _userMessage.value = "Removed '$title' from AniTrAk & MyAnimeList"
                    } else {
                        _userMessage.value = "Removed '$title' from AniTrAk"
                    }
                }
            }
        }
    }

    fun togglePlanner(anime: AnimeEntity) {
        viewModelScope.launch {
            val isNowInPlanner = !anime.inWatchlist
            if (isNowInPlanner) {
                repository.addToWatchlist(anime)
                _userMessage.value = "Added '${anime.displayTitle}' to Planner!"
                if (_isMalLoggedIn.value) {
                    updateAnimeMalStatus(anime, "watching", showToast = true)
                }
            } else {
                removeAnime(anime, scope = RemovalScope.BOTH, listType = "WATCHLIST")
            }
        }
    }

    fun toggleWishlist(anime: AnimeEntity) {
        viewModelScope.launch {
            val isNowWishlist = !anime.isWishlist
            if (isNowWishlist) {
                repository.toggleWishlistEntity(anime)
                _userMessage.value = "Added '${anime.displayTitle}' to Wishlist"
                if (_isMalLoggedIn.value) {
                    updateAnimeMalStatus(anime, "plan_to_watch", showToast = true)
                }
            } else {
                removeAnime(anime, scope = RemovalScope.BOTH, listType = "WISHLIST")
            }
        }
    }

    fun toggleWatched(animeId: Int, isWatched: Boolean) {
        viewModelScope.launch {
            repository.toggleWatchedStatus(animeId, isWatched)
        }
    }

    fun toggleFavorite(anime: AnimeEntity) {
        viewModelScope.launch {
            val newStatus = !anime.isFavorite
            repository.toggleFavorite(anime.id, newStatus)
            _userMessage.value = if (newStatus) "Added '${anime.displayTitle}' to Favorites" else "Removed from Favorites"
        }
    }

    fun togglePinAnime(anime: AnimeEntity) {
        viewModelScope.launch {
            val newStatus = !anime.isPinned
            repository.togglePinned(anime.id, newStatus)
            _userMessage.value = if (newStatus) "Pinned '${anime.displayTitle}'" else "Unpinned '${anime.displayTitle}'"
        }
    }

    fun removeFromPlanner(anime: AnimeEntity) {
        removeAnime(anime, scope = RemovalScope.BOTH, listType = "WATCHLIST")
    }

    fun toggleNotification(anime: AnimeEntity) {
        viewModelScope.launch {
            val newStatus = !anime.notificationsEnabled
            repository.toggleNotification(anime.id, newStatus)
            _userMessage.value = if (newStatus) "Notifications enabled for ${anime.displayTitle}" else "Notifications disabled"
        }
    }

    fun toggleAutoIncrementEpisode() {
        _autoIncrementEpisode.value = !_autoIncrementEpisode.value
    }

    fun toggleGlobalNotifications() {
        _globalNotificationsEnabled.value = !_globalNotificationsEnabled.value
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearWatchHistory()
            _userMessage.value = "Watch history cleared"
        }
    }

    fun addToWatchlist(anime: AnimeEntity) {
        viewModelScope.launch {
            repository.addToWatchlist(anime)
            _userMessage.value = "Added '${anime.displayTitle}' to Watchlist!"
            if (_isMalLoggedIn.value) {
                updateAnimeMalStatus(anime, "watching", showToast = true)
            }
        }
    }

    fun removeFromWatchlist(animeId: Int) {
        viewModelScope.launch {
            val anime = repository.getAnimeByIdSync(animeId)
            if (anime != null) {
                removeAnime(anime, scope = RemovalScope.BOTH, listType = "WATCHLIST")
            } else {
                repository.removeFromWatchlist(animeId)
                _userMessage.value = "Removed from Watchlist"
            }
        }
    }

    fun refreshSchedule(showToast: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.refreshSchedule(forceRefresh = showToast)
            _isRefreshing.value = false
            if (showToast) {
                if (result.isSuccess) {
                    _userMessage.value = "Schedule updated from live source!"
                } else {
                    _userMessage.value = "Could not update schedule from online source."
                }
            }
        }
    }

    fun selectAnimeDetail(id: Int?) {
        _selectedAnimeDetailId.value = id
        if (id != null) {
            loadFullDetails(id)
            viewModelScope.launch(Dispatchers.IO) {
                val token = malOAuthManager.getValidAccessToken()
                val updatedAnime = repository.verifyAndSyncSingleAnimeWithMal(id, token) ?: repository.getAnimeByIdSync(id)
                if (updatedAnime != null) {
                    val queryTitle = updatedAnime.titleEnglish ?: updatedAnime.titleRomaji
                    searchYouTubeVideos("$queryTitle trailer", animeTitle = queryTitle)
                    searchInternetArchive(queryTitle)
                }
            }
        }
    }

    /**
     * Clears transient API response cache, non-saved catalogue entries, and image caches
     * while strictly preserving all saved anime, watchlists, wishlists, favorites, history, and preferences.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearMetadataAndApiCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearMetadataAndApiCache()
                val app = getApplication<Application>()
                coil.Coil.imageLoader(app).memoryCache?.clear()
                coil.Coil.imageLoader(app).diskCache?.clear()
                val savedCount = watchlist.value.size + wishlist.value.size + favorites.value.size
                _userMessage.value = "Cache cleared! All saved anime, watchlists, favorites ($savedCount items) & history preserved."
            } catch (e: Exception) {
                _userMessage.value = "Cache cleared safely (saved anime preserved)."
            }
        }
    }

    /**
     * Saves an Internet Archive item directly to the user's durable Watchlist or Wishlist
     * using its stable Internet Archive identifier.
     */
    fun saveArchiveItemToWatchlist(item: ArchiveMediaItem, asWishlist: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = repository.saveArchiveMediaItem(
                archiveIdentifier = item.identifier,
                title = item.title,
                description = item.description,
                thumbnailUrl = item.thumbnailUrl,
                year = item.year?.toIntOrNull(),
                inWatchlist = !asWishlist,
                isWishlist = asWishlist
            )
            _userMessage.value = if (asWishlist) {
                "Saved '${saved.displayTitle}' (Archive ID: ${item.identifier}) to Wishlist"
            } else {
                "Saved '${saved.displayTitle}' (Archive ID: ${item.identifier}) to Watchlist"
            }
        }
    }

    /**
     * Exports all saved anime, watchlists, favorites, watch history, and preferences to a JSON backup file.
     */
    fun exportBackupToUri(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val appPrefs = app.getSharedPreferences("anitrack_prefs", android.content.Context.MODE_PRIVATE)
                val json = repository.exportUserDataJson(appPrefs)
                app.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: throw IllegalStateException("Could not open output stream for backup file")
                _userMessage.value = "Backup exported successfully (${watchlist.value.size} watchlist, ${wishlist.value.size} wishlist, ${favorites.value.size} favorites)!"
            } catch (e: Exception) {
                _userMessage.value = "Backup export failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    /**
     * Imports and merges saved anime, watchlists, favorites, watch history, and preferences from a JSON backup file
     * without deleting or resetting any existing saved records.
     */
    fun importBackupFromUri(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val appPrefs = app.getSharedPreferences("anitrack_prefs", android.content.Context.MODE_PRIVATE)
                val json = app.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException("Could not read selected backup file")
                val result = repository.importUserDataJson(json, appPrefs)
                if (result.success) {
                    _userMessage.value = result.message
                } else {
                    _userMessage.value = "Restore error: ${result.message}"
                }
            } catch (e: Exception) {
                _userMessage.value = "Backup import failed: ${e.localizedMessage ?: "Invalid file"}"
            }
        }
    }

    /**
     * Validates a downloaded APK file's integrity, package name, versionCode, and signing certificate
     * before launching Android's authorized PackageInstaller session (preventing INSTALL_FAILED_UPDATE_INCOMPATIBLE).
     */
    fun validateAndInstallUpdateApk(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val tempApk = java.io.File(app.cacheDir, "pending_update_check.apk")
            try {
                app.contentResolver.openInputStream(uri)?.use { input ->
                    tempApk.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    _userMessage.value = "Cannot read selected APK file."
                    return@launch
                }

                val validation = com.example.util.ApkUpdateValidator.validateUpdateApk(app, tempApk)
                if (!validation.isValid) {
                    tempApk.delete()
                    _userMessage.value = "Update blocked (${validation.errorCode}): ${validation.userMessage}"
                    return@launch
                }

                if (com.example.util.ApkUpdateValidator.canRequestPackageInstalls(app)) {
                    val launched = com.example.util.ApkUpdateValidator.launchAuthorizedSessionInstall(app, tempApk)
                    if (launched.isSuccess) {
                        _userMessage.value = "Verified APK v${validation.versionName} (${validation.versionCode}). Confirm update in system prompt."
                    } else {
                        tempApk.delete()
                        _userMessage.value = "APK Verified (${validation.userMessage})"
                    }
                } else {
                    tempApk.delete()
                    _userMessage.value = "APK Verified: ${validation.userMessage}"
                }
            } catch (e: Exception) {
                tempApk.delete()
                _userMessage.value = "APK verification failed: ${e.localizedMessage}"
            }
        }
    }

    fun getSigningDiagnosticsSummary(): String {
        val app = getApplication<Application>()
        val sha256 = com.example.util.ApkUpdateValidator.getInstalledSigningSha256(app) ?: "Unknown"
        val shortSha = if (sha256.length > 23) sha256.take(23) + "..." else sha256
        val pInfo = app.packageManager.getPackageInfo(app.packageName, 0)
        val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode.toLong()
        return "Pkg: ${app.packageName} • v${pInfo.versionName} ($vCode) • Cert SHA-256: $shortSha"
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

enum class RemovalScope {
    LOCAL_ONLY,
    PROVIDER_ONLY,
    BOTH
}
