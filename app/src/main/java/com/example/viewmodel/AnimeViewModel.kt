package com.example.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthCallbackOutcome
import com.example.auth.SignUpOutcome
import com.example.auth.SupabaseAuthService
import com.example.config.AppConfig
import com.example.data.local.AnimeEntity
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import com.example.data.repository.AppPreferencesState
import com.example.data.repository.OwnerVaultStore
import com.example.data.repository.PreferencesRepository
import com.example.model.AnimeCollection
import com.example.model.AnimeRichDetail
import com.example.model.AuthSession
import com.example.model.BuiltInAvatar
import com.example.model.CommunityActivityItem
import com.example.model.CommunityCapacity
import com.example.model.CommunityMember
import com.example.model.CommunityReview
import com.example.model.DatabaseStorageStats
import com.example.model.InvitationCode
import com.example.model.MemberStatus
import com.example.model.PrivacySettings
import com.example.model.StarRatingUtils
import com.example.model.ThemeModePreference
import com.example.model.UserProfile
import com.example.model.VisibilityLevel
import com.example.model.WatchStatus
import com.example.model.WatchlistLayoutMode
import com.example.service.AniListMetadataProvider
import com.example.service.AppUpdateInfo
import com.example.service.SupabaseDatabaseService
import com.example.util.ErrorSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

enum class ReviewSortOption(val label: String) {
    NEWEST("Newest"),
    HIGHEST_RATED("Highest Rated"),
    MOST_LIKED("Most Liked")
}

enum class OnboardingStage {
    NONE,
    USERNAME,
    AVATAR
}

data class DynamicIslandFeedback(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val subtitle: String,
    val animeId: Int? = null,
    val previousEpisode: Int? = null,
    val previousStatus: WatchStatus? = null,
    val canUndo: Boolean = false
)

class AnimeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val metadataProvider = AniListMetadataProvider()
    private val repository = AnimeRepository(
        animeDao = database.animeDao(),
        historyDao = database.episodeHistoryDao(),
        apiCacheDao = database.apiCacheDao(),
        deletedAnimeDao = database.deletedAnimeDao(),
        savedAnimeDao = database.savedAnimeDao(),
        metadataProvider = metadataProvider,
        onCompactDatabase = { database.compactDatabase() }
    )
    private val preferencesRepo = PreferencesRepository(application)
    private val ownerVaultStore = OwnerVaultStore(application)
    private val supabaseAuth = SupabaseAuthService(application)
    private val supabaseDb = SupabaseDatabaseService()

    @Volatile
    private var activeOwnerKey: String? = null
    private val dirtyPendingSyncIds = mutableSetOf<Int>()

    // App Preferences (DataStore)
    val preferences: StateFlow<AppPreferencesState> = preferencesRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferencesState())

    // Global Feedback & Dynamic Island Morph State
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()
    fun clearUserMessage() { _userMessage.value = null }
    fun showMessage(message: String) { _userMessage.value = message }

    private val _dynamicIslandFeedback = MutableStateFlow<DynamicIslandFeedback?>(null)
    val dynamicIslandFeedback = _dynamicIslandFeedback.asStateFlow()
    fun dismissDynamicIslandFeedback() { _dynamicIslandFeedback.value = null }

    private val _focusedArtworkUrl = MutableStateFlow<String?>(null)
    val focusedArtworkUrl = _focusedArtworkUrl.asStateFlow()
    fun updateFocusedArtwork(url: String?) {
        if (!url.isNullOrBlank()) {
            _focusedArtworkUrl.value = url
        }
    }

    // Auth & Membership State (synchronously hydrated from fastPrefs for signed-in users to eliminate startup login flash)
    private val initialFastSession = supabaseAuth.getFastCachedSession()
    private val _authSession = MutableStateFlow<AuthSession?>(initialFastSession)
    val authSession = _authSession.asStateFlow()

    private val _isRestoringSession = MutableStateFlow(false)
    val isRestoringSession = _isRestoringSession.asStateFlow()

    private val _isLocalPreviewMode = MutableStateFlow(false)
    val isLocalPreviewMode = _isLocalPreviewMode.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(
        if (initialFastSession != null && initialFastSession.onboardingCompleted) {
            UserProfile(
                id = initialFastSession.userId,
                email = initialFastSession.email,
                username = initialFastSession.pendingUsername.ifBlank {
                    initialFastSession.email.substringBefore("@").ifBlank { "Member" }
                },
                avatarKey = initialFastSession.pendingAvatarKey.ifBlank { BuiltInAvatar.AVATAR_1.key },
                memberStatus = MemberStatus.ACTIVE
            )
        } else null
    )
    val userProfile = _userProfile.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _pendingEmailVerification = MutableStateFlow<String?>(null)
    val pendingEmailVerification = _pendingEmailVerification.asStateFlow()

    private val _needsInviteRedemption = MutableStateFlow(false)
    val needsInviteRedemption = _needsInviteRedemption.asStateFlow()

    private val _isSupabaseReady = MutableStateFlow(AppConfig.isSupabaseConfigured)
    val isSupabaseReady = _isSupabaseReady.asStateFlow()

    private val _isPasswordRecoveryPending = MutableStateFlow(false)
    val isPasswordRecoveryPending = _isPasswordRecoveryPending.asStateFlow()

    private val _onboardingStage = MutableStateFlow(OnboardingStage.NONE)
    val onboardingStage = _onboardingStage.asStateFlow()

    private val _pendingOnboardingUsername = MutableStateFlow("")
    val pendingOnboardingUsername = _pendingOnboardingUsername.asStateFlow()

    private val _isCheckingUsername = MutableStateFlow(false)
    val isCheckingUsername = _isCheckingUsername.asStateFlow()

    private val _usernameAvailabilityError = MutableStateFlow<String?>(null)
    val usernameAvailabilityError = _usernameAvailabilityError.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError = _profileError.asStateFlow()

    private val _isProfileSaving = MutableStateFlow(false)
    val isProfileSaving = _isProfileSaving.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount = _isDeletingAccount.asStateFlow()

    private val _deleteAccountError = MutableStateFlow<String?>(null)
    val deleteAccountError = _deleteAccountError.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate = _availableUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate = _isCheckingUpdate.asStateFlow()

    private var pendingSignUpEmail: String? = null
    private var pendingSignUpPassword: String? = null
    private var verificationPollJob: Job? = null

    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings = _privacySettings.asStateFlow()

    // Watchlist & Schedule
    val watchlist: StateFlow<List<AnimeEntity>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<AnimeEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    @OptIn(ExperimentalCoroutinesApi::class)
    val dayAnime: StateFlow<List<AnimeEntity>> = selectedDay
        .flatMapLatest { day -> repository.getPlannerAnimeByDay(day) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Home Highlights
    private val _trendingAnime = MutableStateFlow<List<AnimeEntity>>(emptyList())
    val trendingAnime = _trendingAnime.asStateFlow()

    private val _isHomeRefreshing = MutableStateFlow(false)
    val isHomeRefreshing = _isHomeRefreshing.asStateFlow()

    private val _homeError = MutableStateFlow<String?>(null)
    val homeError = _homeError.asStateFlow()

    // Discover & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(DiscoverFilterState())
    val filterState = _filterState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AnimeEntity>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _isNextPageLoading = MutableStateFlow(false)
    val isNextPageLoading = _isNextPageLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private var currentDiscoverPage = 1
    private var hasNextDiscoverPage = true
    private var searchJob: Job? = null

    // Anime Detail
    private val _selectedAnimeId = MutableStateFlow<Int?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedAnimeDetail: StateFlow<AnimeEntity?> = _selectedAnimeId
        .flatMapLatest { id -> if (id != null) repository.getAnimeById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedRichDetail = MutableStateFlow<AnimeRichDetail?>(null)
    val selectedRichDetail = _selectedRichDetail.asStateFlow()

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading = _isDetailLoading.asStateFlow()

    private val _animeReviews = MutableStateFlow<List<CommunityReview>>(emptyList())
    val animeReviews = _animeReviews.asStateFlow()

    // Collections
    private val _collections = MutableStateFlow<List<AnimeCollection>>(emptyList())
    val collections = _collections.asStateFlow()

    // Community
    private val _communityCapacity = MutableStateFlow(CommunityCapacity())
    val communityCapacity = _communityCapacity.asStateFlow()

    private val _communityMembers = MutableStateFlow<List<CommunityMember>>(emptyList())
    val communityMembers = _communityMembers.asStateFlow()

    private val _communityReviews = MutableStateFlow<List<CommunityReview>>(emptyList())
    val communityReviews = _communityReviews.asStateFlow()

    private val _communityActivity = MutableStateFlow<List<CommunityActivityItem>>(emptyList())
    val communityActivity = _communityActivity.asStateFlow()

    private val _isCommunityLoading = MutableStateFlow(false)
    val isCommunityLoading = _isCommunityLoading.asStateFlow()

    private val _communityError = MutableStateFlow<String?>(null)
    val communityError = _communityError.asStateFlow()

    private val _reviewSortOption = MutableStateFlow(ReviewSortOption.NEWEST)
    val reviewSortOption = _reviewSortOption.asStateFlow()

    // Selected Member Inspection
    private val _inspectedMemberWatchlist = MutableStateFlow<List<AnimeEntity>>(emptyList())
    val inspectedMemberWatchlist = _inspectedMemberWatchlist.asStateFlow()

    private val _inspectedMemberCollections = MutableStateFlow<List<AnimeCollection>>(emptyList())
    val inspectedMemberCollections = _inspectedMemberCollections.asStateFlow()

    private val _isMemberInspectLoading = MutableStateFlow(false)
    val isMemberInspectLoading = _isMemberInspectLoading.asStateFlow()

    // Admin Invitations & 500 MB Storage Monitoring
    private val _invitations = MutableStateFlow<List<InvitationCode>>(emptyList())
    val invitations = _invitations.asStateFlow()

    private val _storageStats = MutableStateFlow<DatabaseStorageStats?>(null)
    val storageStats = _storageStats.asStateFlow()

    private val _isStorageStatsLoading = MutableStateFlow(false)
    val isStorageStatsLoading = _isStorageStatsLoading.asStateFlow()

    private var hasTriggeredInitialAuthRestore = false

    init {
        restoreSessionAndInitialData()
        startAutoCloudSyncLoop()
    }

    private fun buildOptimisticProfile(session: AuthSession): UserProfile {
        return _userProfile.value ?: UserProfile(
            id = session.userId,
            email = session.email,
            username = session.pendingUsername.ifBlank {
                session.email.substringBefore("@").ifBlank { "Member" }
            },
            avatarKey = session.pendingAvatarKey.ifBlank { BuiltInAvatar.AVATAR_1.key },
            memberStatus = MemberStatus.ACTIVE
        )
    }

    private suspend fun persistCurrentOwnerVaultSnapshot() {
        val key = activeOwnerKey ?: return
        val items = repository.getTrackedAnimeSnapshot()
        val deletedIds = repository.getDeletedAnimeIdsSnapshot()
        ownerVaultStore.saveOwnerVault(key, items, deletedIds)
    }

    private suspend fun switchToOwnerVault(targetOwnerKey: String) {
        val currentKey = activeOwnerKey ?: ownerVaultStore.getLastActiveOwnerKey()
        if (currentKey != null && currentKey != targetOwnerKey) {
            val prevItems = repository.getTrackedAnimeSnapshot()
            val prevDeleted = repository.getDeletedAnimeIdsSnapshot()
            ownerVaultStore.saveOwnerVault(currentKey, prevItems, prevDeleted)
            dirtyPendingSyncIds.clear()
        }
        val alreadyOwnsRoom = currentKey == targetOwnerKey && activeOwnerKey == targetOwnerKey
        if (!alreadyOwnsRoom) {
            val targetSnap = ownerVaultStore.loadOwnerVault(targetOwnerKey)
            if (currentKey != targetOwnerKey) {
                repository.restoreOwnerTrackedItems(targetSnap.items, targetSnap.deletedAnimeIds)
            } else {
                // Same owner across app restart: merge snapshot safely without losing offline edits
                repository.reconcileSavedAnime()
                if (targetSnap.items.isNotEmpty()) {
                    val currentIds = repository.getTrackedAnimeSnapshot().map { it.id }.toSet()
                    val missingFromRoom = targetSnap.items.filter { it.id !in currentIds }
                    if (missingFromRoom.isNotEmpty()) {
                        repository.syncCloudWatchlistEntries(missingFromRoom, replaceMissingFromCloud = false)
                    }
                }
            }
            activeOwnerKey = targetOwnerKey
            ownerVaultStore.setLastActiveOwnerKey(targetOwnerKey)
            syncDisplayedCatalogPersonalFlags()
        }
    }

    private fun syncDisplayedCatalogPersonalFlags() {
        val trackedById = watchlist.value.associateBy { it.id }
        fun clearFlags(entity: AnimeEntity): AnimeEntity {
            val mine = trackedById[entity.id]
            return if (mine != null) {
                entity.copy(
                    inWatchlist = mine.inWatchlist,
                    isWishlist = mine.isWishlist,
                    isFavorite = mine.isFavorite,
                    isPinned = mine.isPinned,
                    watchedEpisodeCount = mine.watchedEpisodeCount,
                    userSeason = mine.userSeason,
                    userStatus = mine.userStatus,
                    userScore = mine.userScore,
                    isWatched = mine.isWatched,
                    userNotes = mine.userNotes
                )
            } else {
                entity.copy(
                    inWatchlist = false,
                    isWishlist = false,
                    isFavorite = false,
                    isPinned = false,
                    watchedEpisodeCount = 0,
                    userSeason = 1,
                    userStatus = null,
                    userScore = null,
                    isWatched = false,
                    userNotes = null
                )
            }
        }
        _searchResults.value = _searchResults.value.map(::clearFlags)
        _trendingAnime.value = _trendingAnime.value.map(::clearFlags)
    }

    private suspend fun migrateGuestVaultToAccountIfNeeded(session: AuthSession) {
        val guestId = ownerVaultStore.getOrCreateGuestId()
        val guestKey = OwnerVaultStore.guestKey(guestId)
        val guestSnapshot = ownerVaultStore.loadOwnerVault(guestKey)
        if (guestSnapshot.items.isEmpty()) {
            ownerVaultStore.setGuestModeActive(false)
            return
        }
        val currentAccountItems = repository.getTrackedAnimeSnapshot()
        val toMigrate = OwnerVaultStore.computeNonDuplicatingMigrationItems(currentAccountItems, guestSnapshot.items)
        if (toMigrate.isNotEmpty()) {
            toMigrate.forEach { dirtyPendingSyncIds.add(it.id) }
            repository.syncCloudWatchlistEntries(toMigrate, replaceMissingFromCloud = false)
            toMigrate.forEach { item ->
                supabaseDb.upsertUserAnimeEntry(session, item).onSuccess {
                    dirtyPendingSyncIds.remove(item.id)
                }
            }
            persistCurrentOwnerVaultSnapshot()
        }
        ownerVaultStore.deleteOwnerVault(guestKey)
        ownerVaultStore.setGuestModeActive(false)
        ownerVaultStore.rotateGuestId()
    }

    private fun restoreSessionAndInitialData() {
        viewModelScope.launch {
            // Read saved session from DataStore immediately before any network calls
            val saved = supabaseAuth.getSavedSession()
            if (saved != null) {
                _authSession.value = saved
                _isLocalPreviewMode.value = false
                ownerVaultStore.setGuestModeActive(false)
                switchToOwnerVault(OwnerVaultStore.userKey(saved.userId))
                if (saved.onboardingCompleted && _userProfile.value == null) {
                    _userProfile.value = buildOptimisticProfile(saved)
                }
            } else if (!_isLocalPreviewMode.value) {
                // On Login screen with no active account: unblock AuthScreen immediately, clear transient Room rows while keeping per-guest vault intact on disk
                _isRestoringSession.value = false
                repository.clearUserTrackingStateOnly()
                ownerVaultStore.setLastActiveOwnerKey(null)
                activeOwnerKey = null
            }
            _isRestoringSession.value = false

            preferencesRepo.preferencesFlow.collect { prefs ->
                AppConfig.updateRuntimeSupabaseConfig(prefs.supabaseUrl, prefs.supabaseAnonKey)
                val ready = AppConfig.isSupabaseConfigured
                _isSupabaseReady.value = ready
                if (ready && !hasTriggeredInitialAuthRestore) {
                    hasTriggeredInitialAuthRestore = true
                    val currentSaved = _authSession.value ?: supabaseAuth.getSavedSession()
                    if (currentSaved != null) {
                        if (currentSaved.onboardingCompleted && _userProfile.value == null) {
                            _userProfile.value = buildOptimisticProfile(currentSaved)
                        }
                        val refreshed = supabaseAuth.refreshSessionIfNeeded(currentSaved)
                        if (refreshed.isSuccess) {
                            val activeSession = refreshed.getOrThrow()
                            _authSession.value = activeSession
                            val loaded = supabaseDb.fetchCurrentProfile(activeSession).getOrNull()
                            if (loaded != null) {
                                supabaseAuth.cacheProfileSnapshot(loaded.username, loaded.avatarKey)
                            }
                            val needsOnboarding = !activeSession.onboardingCompleted ||
                                (loaded != null && loaded.username.startsWith("traveler_", ignoreCase = true))
                            if (needsOnboarding) {
                                _userProfile.value = loaded
                                _onboardingStage.value = OnboardingStage.USERNAME
                            } else {
                                _onboardingStage.value = OnboardingStage.NONE
                                if (loaded != null) {
                                    _userProfile.value = loaded
                                }
                                loadAuthenticatedWorkspace(activeSession)
                            }
                        } else {
                            supabaseAuth.clearSession()
                            repository.clearUserTrackingStateOnly()
                            ownerVaultStore.setLastActiveOwnerKey(null)
                            activeOwnerKey = null
                            _authSession.value = null
                            _userProfile.value = null
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.pruneAndCompactStorage()
            refreshHome()
            checkForUpdates(manual = false)
        }
    }

    private fun startAutoCloudSyncLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                triggerSilentCloudSync()
            }
        }
    }

    fun triggerSilentCloudSync() {
        val current = _authSession.value ?: return
        if (_onboardingStage.value != OnboardingStage.NONE || !current.onboardingCompleted) return
        val capturedUserId = current.userId
        viewModelScope.launch {
            val activeSession = supabaseAuth.refreshSessionIfNeeded(current).getOrDefault(current)
            if (_authSession.value?.userId != capturedUserId) return@launch
            if (activeSession.accessToken != current.accessToken) {
                _authSession.value = activeSession
            }
            if (dirtyPendingSyncIds.isNotEmpty()) {
                val pendingSnapshot = repository.getTrackedAnimeSnapshot().filter { it.id in dirtyPendingSyncIds }
                pendingSnapshot.forEach { item ->
                    supabaseDb.upsertUserAnimeEntry(activeSession, item).onSuccess {
                        dirtyPendingSyncIds.remove(item.id)
                    }
                }
            }
            supabaseDb.fetchUserWatchlist(activeSession).onSuccess { cloudItems ->
                if (_authSession.value?.userId != capturedUserId) return@onSuccess
                repository.syncCloudWatchlistEntries(
                    cloudItems = cloudItems,
                    replaceMissingFromCloud = true,
                    protectedPendingIds = dirtyPendingSyncIds.toSet()
                )
                persistCurrentOwnerVaultSnapshot()
                syncDisplayedCatalogPersonalFlags()
            }
            supabaseDb.fetchCollections(activeSession, creatorUserId = capturedUserId).onSuccess { cols ->
                if (_authSession.value?.userId == capturedUserId) {
                    _collections.value = cols
                }
            }
            if (_userProfile.value?.memberStatus == MemberStatus.ACTIVE) {
                val membersRes = supabaseDb.fetchCommunityMembers(activeSession)
                membersRes.onSuccess { members ->
                    if (_authSession.value?.userId != capturedUserId) return@onSuccess
                    _communityMembers.value = members
                    supabaseDb.fetchCommunityFeed(activeSession, members).onSuccess { (reviews, activity) ->
                        if (_authSession.value?.userId == capturedUserId) {
                            _communityReviews.value = sortReviews(reviews, _reviewSortOption.value)
                            _communityActivity.value = activity
                        }
                    }
                }
            }
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            if (manual) _isCheckingUpdate.value = true
            val res = supabaseDb.checkAppUpdate(
                currentVersionCode = AppConfig.APP_VERSION_CODE,
                currentVersionName = AppConfig.APP_VERSION
            )
            res.onSuccess { info ->
                if (info != null) {
                    _availableUpdate.value = info
                } else if (manual) {
                    _userMessage.value = "You're on the latest version (v${AppConfig.APP_VERSION})."
                }
            }.onFailure {
                if (manual) {
                    _userMessage.value = "You're on the latest version (v${AppConfig.APP_VERSION})."
                }
            }
            if (manual) _isCheckingUpdate.value = false
        }
    }

    fun dismissUpdatePrompt() {
        val current = _availableUpdate.value
        if (current != null && !current.isMandatory) {
            _availableUpdate.value = null
        }
    }

    fun saveSupabaseConfig(url: String, anonKey: String) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = anonKey.trim()
        if (!cleanUrl.startsWith("https://") || !cleanUrl.contains(".")) {
            _authError.value = "Invalid cloud configuration."
            return
        }
        if (cleanKey.length <= 20) {
            _authError.value = "Invalid cloud configuration."
            return
        }
        viewModelScope.launch {
            _authError.value = null
            preferencesRepo.setSupabaseConfig(cleanUrl, cleanKey)
            AppConfig.updateRuntimeSupabaseConfig(cleanUrl, cleanKey)
            _isSupabaseReady.value = AppConfig.isSupabaseConfigured
            _userMessage.value = "Cloud sync configuration updated."
        }
    }

    // ------------------------------------------------------------------------
    // AUTH & INVITE FLOWS
    // ------------------------------------------------------------------------

    fun enterLocalPreviewMode() {
        val guestId = ownerVaultStore.getOrCreateGuestId()
        ownerVaultStore.setGuestModeActive(true)
        _authSession.value = null
        _userProfile.value = null
        _privacySettings.value = PrivacySettings()
        _collections.value = emptyList()
        _communityMembers.value = emptyList()
        _communityReviews.value = emptyList()
        _communityActivity.value = emptyList()
        _animeReviews.value = emptyList()
        _selectedAnimeId.value = null
        _selectedRichDetail.value = null
        _isLocalPreviewMode.value = true
        viewModelScope.launch {
            switchToOwnerVault(OwnerVaultStore.guestKey(guestId))
            refreshHome()
            ensureDiscoverLoaded()
        }
    }

    fun exitLocalPreviewMode() {
        _isLocalPreviewMode.value = false
        ownerVaultStore.setGuestModeActive(false)
        viewModelScope.launch {
            persistCurrentOwnerVaultSnapshot()
        }
    }

    fun clearAuthMessages() {
        _authError.value = null
        _pendingEmailVerification.value = null
        _usernameAvailabilityError.value = null
        verificationPollJob?.cancel()
        verificationPollJob = null
    }

    fun clearUsernameError() {
        _usernameAvailabilityError.value = null
    }

    fun clearProfileError() {
        _profileError.value = null
    }

    fun clearDeleteAccountError() {
        _deleteAccountError.value = null
    }

    fun signUpEmailOnly(email: String, password: String) {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _authError.value = "Enter a valid email address."
            return
        }
        if (password.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return
        }
        pendingSignUpEmail = cleanEmail
        pendingSignUpPassword = password

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _pendingEmailVerification.value = null

            val localWaitSec = supabaseAuth.checkLocalEmailCooldownSeconds(cleanEmail)
            if (localWaitSec > 0L) {
                val waitText = SupabaseAuthService.formatRemainingMinutes(localWaitSec)
                _authError.value = "This email was recently deleted. You can register with it again in $waitText."
                _isAuthLoading.value = false
                return@launch
            }

            val remoteCooldown = supabaseDb.checkEmailRegistrationAllowed(cleanEmail).getOrNull()
            if (remoteCooldown != null && !remoteCooldown.allowed) {
                val waitText = SupabaseAuthService.formatRemainingMinutes(remoteCooldown.retryAfterSeconds.coerceAtLeast(60L))
                _authError.value = "This email was recently deleted. You can register with it again in $waitText."
                _isAuthLoading.value = false
                return@launch
            }

            val result = supabaseAuth.signUpEmailOnly(
                email = cleanEmail,
                password = password
            )
            result.onSuccess { outcome ->
                when (outcome) {
                    is SignUpOutcome.Authenticated -> {
                        _authSession.value = outcome.session
                        _isLocalPreviewMode.value = false
                        _onboardingStage.value = OnboardingStage.USERNAME
                        _needsInviteRedemption.value = false
                    }
                    is SignUpOutcome.EmailVerificationRequired -> {
                        _pendingEmailVerification.value = outcome.message
                        startEmailVerificationPolling(cleanEmail, password)
                    }
                }
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Unable to create your account right now.")
            }
            _isAuthLoading.value = false
        }
    }

    private fun startEmailVerificationPolling(email: String, password: String) {
        verificationPollJob?.cancel()
        verificationPollJob = viewModelScope.launch {
            // Automatically detect when the user clicks the confirmation link in Gmail/Chrome
            repeat(60) {
                delay(3500L)
                if (!isActive || _authSession.value != null || _pendingEmailVerification.value.isNullOrBlank()) {
                    return@launch
                }
                val res = supabaseAuth.signInWithEmail(email, password)
                if (res.isSuccess) {
                    val session = res.getOrThrow()
                    _authSession.value = session
                    _pendingEmailVerification.value = null
                    _isLocalPreviewMode.value = false
                    _needsInviteRedemption.value = false
                    val existingProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
                    val needsOnboarding = !session.onboardingCompleted ||
                        existingProfile == null ||
                        existingProfile.username.startsWith("traveler_", ignoreCase = true)
                    if (needsOnboarding) {
                        _userProfile.value = existingProfile
                        _onboardingStage.value = OnboardingStage.USERNAME
                        _userMessage.value = "Email verified! Choose your unique username."
                    } else {
                        _onboardingStage.value = OnboardingStage.NONE
                        _userProfile.value = existingProfile
                        loadAuthenticatedWorkspace(session)
                    }
                    return@launch
                }
            }
        }
    }

    fun handlePastedAuthLink(rawLink: String) {
        val clean = rawLink.trim()
        if (clean.isBlank()) {
            _authError.value = "Paste the link from your email or browser address bar."
            return
        }
        runCatching {
            val uri = Uri.parse(clean)
            handleDeepLinkUri(uri, allowAnyScheme = true)
        }.onFailure {
            _authError.value = "Invalid link format."
        }
    }

    fun confirmEmailAndContinue(email: String, password: String) {
        val targetEmail = email.trim().ifBlank { pendingSignUpEmail.orEmpty() }
        val targetPassword = password.ifBlank { pendingSignUpPassword.orEmpty() }
        if (targetEmail.isBlank() || targetPassword.isBlank()) {
            _authError.value = "Sign in with your email and password to continue setup."
            return
        }
        signInWithIdentifier(targetEmail, targetPassword)
    }

    /**
     * Silently checks in the background while on the "Check Your Inbox" screen whether
     * the user clicked the verification link in Gmail (even if Chrome opened a blank page).
     * As soon as Supabase marks the email confirmed, this automatically signs the user in
     * and opens the Username + Royal PFP onboarding screen.
     */
    suspend fun checkEmailVerifiedSilently(email: String, password: String): Boolean {
        val targetEmail = email.trim().ifBlank { pendingSignUpEmail.orEmpty() }
        val targetPassword = password.ifBlank { pendingSignUpPassword.orEmpty() }
        if (targetEmail.isBlank() || targetPassword.isBlank() || _authSession.value != null) {
            return false
        }
        val res = supabaseAuth.signInWithEmail(targetEmail, targetPassword)
        if (res.isSuccess) {
            val session = res.getOrThrow()
            _authSession.value = session
            _pendingEmailVerification.value = null
            _authError.value = null
            _isLocalPreviewMode.value = false
            _needsInviteRedemption.value = false
            val loadedProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
            val needsOnboarding = !session.onboardingCompleted ||
                loadedProfile == null ||
                loadedProfile.username.startsWith("traveler_", ignoreCase = true)
            if (needsOnboarding) {
                _userProfile.value = loadedProfile
                _onboardingStage.value = OnboardingStage.USERNAME
                _userMessage.value = "Email verified! Claim your unique username."
            } else {
                _onboardingStage.value = OnboardingStage.NONE
                _userProfile.value = loadedProfile
                pendingSignUpEmail = null
                pendingSignUpPassword = null
                loadAuthenticatedWorkspace(session)
            }
            return true
        }
        return false
    }

    fun processPastedAuthLink(rawLink: String) {
        val trimmed = rawLink.trim()
        if (trimmed.isBlank()) {
            _authError.value = "Paste the link from your email or browser address bar."
            return
        }
        runCatching { Uri.parse(trimmed) }.getOrNull()?.let { uri ->
            handleDeepLinkUri(uri, allowAnyScheme = true)
        } ?: run {
            _authError.value = "Invalid link format."
        }
    }

    fun signInWithIdentifier(identifier: String, password: String) {
        val cleanIdentifier = identifier.trim()
        if (cleanIdentifier.isBlank() || password.isBlank()) {
            _authError.value = "Enter your email or username and password."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _pendingEmailVerification.value = null

            val resolvedEmailRes = if (cleanIdentifier.contains("@") && !cleanIdentifier.startsWith("@")) {
                Result.success(cleanIdentifier)
            } else {
                supabaseDb.resolveLoginEmail(cleanIdentifier)
            }

            if (resolvedEmailRes.isFailure) {
                _authError.value = ErrorSanitizer.sanitize(
                    resolvedEmailRes.exceptionOrNull(),
                    "No account found with that username."
                )
                _isAuthLoading.value = false
                return@launch
            }

            val resolvedEmail = resolvedEmailRes.getOrThrow()
            val res = supabaseAuth.signInWithEmail(resolvedEmail, password)
            res.onSuccess { session ->
                _authSession.value = session
                _isLocalPreviewMode.value = false
                _needsInviteRedemption.value = false
                val existingProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
                val needsOnboarding = !session.onboardingCompleted ||
                    existingProfile == null ||
                    existingProfile.username.startsWith("traveler_", ignoreCase = true)

                if (needsOnboarding) {
                    _userProfile.value = existingProfile
                    _onboardingStage.value = OnboardingStage.USERNAME
                } else {
                    _onboardingStage.value = OnboardingStage.NONE
                    _userProfile.value = existingProfile
                    supabaseAuth.cacheProfileSnapshot(existingProfile.username, existingProfile.avatarKey)
                    pendingSignUpEmail = null
                    pendingSignUpPassword = null
                    loadAuthenticatedWorkspace(session)
                }
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Incorrect email/username or password.")
            }
            _isAuthLoading.value = false
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        pendingInviteCode: String = "",
        pendingAvatarKey: String = BuiltInAvatar.AVATAR_1.key
    ) = signInWithIdentifier(email, password)

    fun verifyAndAdvanceUsername(username: String) {
        val clean = username.trim()
        when {
            clean.length !in 2..32 -> {
                _usernameAvailabilityError.value = "Username must be between 2 and 32 characters."
                return
            }
            clean.contains("@") -> {
                _usernameAvailabilityError.value = "Usernames cannot contain the @ symbol."
                return
            }
            !clean.matches(Regex("^[A-Za-z0-9_ .-]+$")) -> {
                _usernameAvailabilityError.value = "Use letters, numbers, spaces, dots, hyphens, or underscores."
                return
            }
        }
        val session = _authSession.value
        viewModelScope.launch {
            _isCheckingUsername.value = true
            _usernameAvailabilityError.value = null
            _authError.value = null
            val localUserCooldown = supabaseAuth.checkLocalUsernameCooldownSeconds(clean)
            if (localUserCooldown > 0L) {
                _usernameAvailabilityError.value = "That username was recently deleted and will be available again after 24 hours."
                _isCheckingUsername.value = false
                return@launch
            }
            supabaseDb.checkUsernameStatus(
                username = clean,
                accessToken = session?.accessToken,
                currentUserId = session?.userId
            ).onSuccess { status ->
                if (status.available) {
                    _pendingOnboardingUsername.value = clean
                    _onboardingStage.value = OnboardingStage.AVATAR
                } else {
                    _usernameAvailabilityError.value = status.reason
                        ?: "That username is already taken. Please choose a different unique username."
                }
            }.onFailure { err ->
                _usernameAvailabilityError.value = ErrorSanitizer.sanitize(err, "Unable to check username right now.")
            }
            _isCheckingUsername.value = false
        }
    }

    fun backToUsernameStep() {
        _authError.value = null
        _onboardingStage.value = OnboardingStage.USERNAME
    }

    fun completeRoyalAvatarOnboarding(avatarKey: String) {
        val session = _authSession.value ?: return
        val chosenUsername = _pendingOnboardingUsername.value.trim()
        if (chosenUsername.length < 2) {
            _onboardingStage.value = OnboardingStage.USERNAME
            _usernameAvailabilityError.value = "Choose your unique username first."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            _usernameAvailabilityError.value = null

            val saveRes = supabaseDb.completeOnboardingProfile(
                session = session,
                username = chosenUsername,
                avatarKey = avatarKey
            )
            saveRes.onSuccess { profile ->
                val metaRes = supabaseAuth.updateAuthUserMetadata(
                    session = session,
                    username = chosenUsername,
                    avatarKey = avatarKey,
                    onboardingCompleted = true
                )
                val updatedSession = metaRes.getOrElse {
                    session.copy(
                        pendingUsername = chosenUsername,
                        pendingAvatarKey = avatarKey,
                        onboardingCompleted = true
                    )
                }
                _authSession.value = updatedSession
                _userProfile.value = profile
                _onboardingStage.value = OnboardingStage.NONE
                _needsInviteRedemption.value = false
                pendingSignUpEmail = null
                pendingSignUpPassword = null
                loadAuthenticatedWorkspace(updatedSession)
                _userMessage.value = "Welcome to ANIME UNIVERSE, $chosenUsername!"
            }.onFailure { err ->
                val cleanMsg = ErrorSanitizer.sanitize(err, "Unable to complete profile setup right now.")
                if (cleanMsg.contains("taken", ignoreCase = true) || cleanMsg.contains("username", ignoreCase = true)) {
                    _usernameAvailabilityError.value = cleanMsg
                    _onboardingStage.value = OnboardingStage.USERNAME
                } else {
                    _authError.value = cleanMsg
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun completeProfileSetup(
        username: String,
        avatarKey: String = BuiltInAvatar.AVATAR_1.key
    ) {
        _pendingOnboardingUsername.value = username.trim()
        completeRoyalAvatarOnboarding(avatarKey)
    }

    fun verifySignUpOtp(email: String, otpCode: String) {
        val targetEmail = email.trim().ifBlank { pendingSignUpEmail.orEmpty() }
        val cleanCode = otpCode.trim().replace(" ", "")
        if (targetEmail.isBlank()) {
            _authError.value = "Enter your email address first."
            return
        }
        if (cleanCode.length != 8) {
            _authError.value = "Enter the 8-digit code sent to your email."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            supabaseAuth.verifyEmailOtp(
                email = targetEmail,
                otpCode = cleanCode,
                type = "signup"
            ).onSuccess { session ->
                verificationPollJob?.cancel()
                _authSession.value = session
                _pendingEmailVerification.value = null
                _isLocalPreviewMode.value = false
                _needsInviteRedemption.value = false
                val existingProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
                val needsOnboarding = !session.onboardingCompleted ||
                    existingProfile == null ||
                    existingProfile.username.startsWith("traveler_", ignoreCase = true)
                if (needsOnboarding) {
                    _userProfile.value = existingProfile
                    _onboardingStage.value = OnboardingStage.USERNAME
                    _userMessage.value = "Email verified! Claim your unique username."
                } else {
                    _onboardingStage.value = OnboardingStage.NONE
                    _userProfile.value = existingProfile
                    loadAuthenticatedWorkspace(session)
                }
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Invalid or expired verification code.")
            }
            _isAuthLoading.value = false
        }
    }

    fun sendPasswordReset(identifier: String, onSent: () -> Unit = {}) {
        val clean = identifier.trim()
        if (clean.isBlank()) {
            _authError.value = "Enter your email address or username to reset your password."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val emailRes = if (clean.contains("@") && !clean.startsWith("@")) {
                Result.success(clean)
            } else {
                supabaseDb.resolveLoginEmail(clean)
            }
            emailRes.onSuccess { targetEmail ->
                supabaseAuth.sendPasswordReset(targetEmail).onSuccess { msg ->
                    _pendingEmailVerification.value = msg
                    onSent()
                }.onFailure { err ->
                    _authError.value = ErrorSanitizer.sanitize(err, "Unable to send password reset code right now.")
                }
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "No account found matching that username.")
            }
            _isAuthLoading.value = false
        }
    }

    fun verifyRecoveryCodeAndSetPassword(
        identifier: String,
        otpOrLink: String,
        newPassword: String,
        onCompleted: () -> Unit = {}
    ) {
        val cleanId = identifier.trim()
        val cleanCode = otpOrLink.trim().replace(" ", "")
        if (cleanId.isBlank()) {
            _authError.value = "Enter your email address or username."
            return
        }
        if (cleanCode.length != 8) {
            _authError.value = "Enter the 8-digit reset code from your email."
            return
        }
        if (newPassword.length < 6) {
            _authError.value = "New password must be at least 6 characters."
            return
        }
        if (cleanCode.startsWith("http://", ignoreCase = true) ||
            cleanCode.startsWith("https://", ignoreCase = true) ||
            cleanCode.startsWith("animeuniverse://", ignoreCase = true)
        ) {
            handlePastedAuthLink(cleanCode)
            onCompleted()
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val emailRes = if (cleanId.contains("@") && !cleanId.startsWith("@")) {
                Result.success(cleanId)
            } else {
                supabaseDb.resolveLoginEmail(cleanId)
            }
            if (emailRes.isFailure) {
                _authError.value = ErrorSanitizer.sanitize(emailRes.exceptionOrNull(), "No account found matching that username.")
                _isAuthLoading.value = false
                return@launch
            }
            val targetEmail = emailRes.getOrThrow()
            val verifyRes = supabaseAuth.verifyEmailOtp(
                email = targetEmail,
                otpCode = cleanCode,
                type = "recovery"
            )
            if (verifyRes.isFailure) {
                _authError.value = ErrorSanitizer.sanitize(verifyRes.exceptionOrNull(), "Invalid or expired reset code.")
                _isAuthLoading.value = false
                return@launch
            }
            val session = verifyRes.getOrThrow()
            supabaseAuth.updatePassword(session, newPassword).onSuccess {
                _authSession.value = session
                _isPasswordRecoveryPending.value = false
                _pendingEmailVerification.value = null
                loadAuthenticatedWorkspace(session)
                _userMessage.value = "Password updated successfully!"
                onCompleted()
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Unable to update password right now.")
            }
            _isAuthLoading.value = false
        }
    }

    fun resendVerificationEmail(email: String) {
        val target = email.trim().ifBlank { pendingSignUpEmail.orEmpty() }
        if (target.isBlank()) return
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            supabaseAuth.resendVerificationEmail(target).onSuccess { msg ->
                _pendingEmailVerification.value = msg
                _userMessage.value = msg
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Unable to resend verification link right now.")
            }
            _isAuthLoading.value = false
        }
    }

    fun handleDeepLinkUri(uri: Uri?, allowAnyScheme: Boolean = false) {
        if (uri == null) return
        val isCustomScheme = uri.scheme == "animeuniverse" && uri.host == "auth-callback"
        val hasAuthTokens = uri.fragment?.contains("access_token") == true ||
            uri.getQueryParameter("access_token") != null ||
            uri.getQueryParameter("token_hash") != null ||
            uri.getQueryParameter("token") != null
        if (!isCustomScheme && !(allowAnyScheme && hasAuthTokens)) return
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            supabaseAuth.handleAuthCallbackUri(uri).onSuccess { outcome ->
                when (outcome) {
                    is AuthCallbackOutcome.VerifiedSession -> {
                        val session = outcome.session
                        _authSession.value = session
                        _pendingEmailVerification.value = null
                        _isLocalPreviewMode.value = false
                        _needsInviteRedemption.value = false
                        val existingProfile = supabaseDb.fetchCurrentProfile(session).getOrNull()
                        val needsOnboarding = !session.onboardingCompleted ||
                            existingProfile == null ||
                            existingProfile.username.startsWith("traveler_", ignoreCase = true)
                        if (needsOnboarding) {
                            _userProfile.value = existingProfile
                            _onboardingStage.value = OnboardingStage.USERNAME
                            _userMessage.value = "Email verified! Claim your unique username."
                        } else {
                            _onboardingStage.value = OnboardingStage.NONE
                            _userProfile.value = existingProfile
                            pendingSignUpEmail = null
                            pendingSignUpPassword = null
                            loadAuthenticatedWorkspace(session)
                            _userMessage.value = "Email verified! Welcome to ANIME UNIVERSE."
                        }
                    }
                    is AuthCallbackOutcome.PasswordRecoverySession -> {
                        _authSession.value = outcome.session
                        _isPasswordRecoveryPending.value = true
                    }
                }
            }.onFailure { err ->
                val clean = ErrorSanitizer.sanitize(err, "Email confirmed! Sign in below to continue.")
                if (clean.contains("confirmed", ignoreCase = true)) {
                    _pendingEmailVerification.value = clean
                } else {
                    _authError.value = clean
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun submitNewPassword(newPassword: String) {
        val session = _authSession.value ?: return
        if (newPassword.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            supabaseAuth.updatePassword(session, newPassword).onSuccess {
                _isPasswordRecoveryPending.value = false
                loadAuthenticatedWorkspace(session)
                _userMessage.value = "Password updated successfully!"
            }.onFailure { err ->
                _authError.value = ErrorSanitizer.sanitize(err, "Unable to update password right now.")
            }
            _isAuthLoading.value = false
        }
    }

    fun dismissPasswordRecovery() {
        _isPasswordRecoveryPending.value = false
    }

    fun signOut() {
        val current = _authSession.value
        viewModelScope.launch {
            persistCurrentOwnerVaultSnapshot()
            supabaseAuth.signOut(current)
            repository.clearUserTrackingStateOnly()
            dirtyPendingSyncIds.clear()
            activeOwnerKey = null
            ownerVaultStore.setLastActiveOwnerKey(null)
            ownerVaultStore.setGuestModeActive(false)
            ownerVaultStore.rotateGuestId()
            _authSession.value = null
            _userProfile.value = null
            _privacySettings.value = PrivacySettings()
            _collections.value = emptyList()
            _communityMembers.value = emptyList()
            _communityReviews.value = emptyList()
            _communityActivity.value = emptyList()
            _animeReviews.value = emptyList()
            _inspectedMemberWatchlist.value = emptyList()
            _inspectedMemberCollections.value = emptyList()
            _invitations.value = emptyList()
            _storageStats.value = null
            _selectedAnimeId.value = null
            _selectedRichDetail.value = null
            _dynamicIslandFeedback.value = null
            _needsInviteRedemption.value = false
            _onboardingStage.value = OnboardingStage.NONE
            _pendingOnboardingUsername.value = ""
            _usernameAvailabilityError.value = null
            _profileError.value = null
            _deleteAccountError.value = null
            pendingSignUpEmail = null
            pendingSignUpPassword = null
            _isLocalPreviewMode.value = false
            syncDisplayedCatalogPersonalFlags()
            _userMessage.value = "Signed out."
        }
    }

    fun deleteAccount(
        password: String,
        onSuccess: () -> Unit = {}
    ) {
        val session = _authSession.value
        val currentProfile = _userProfile.value
        if (session == null || currentProfile == null) {
            _deleteAccountError.value = "No active account session found."
            return
        }
        if (password.isBlank()) {
            _deleteAccountError.value = "Enter your password to confirm deletion."
            return
        }
        viewModelScope.launch {
            _isDeletingAccount.value = true
            _deleteAccountError.value = null

            val verifyRes = supabaseAuth.verifyPassword(session, password)
            if (verifyRes.isFailure) {
                _deleteAccountError.value = ErrorSanitizer.sanitize(
                    verifyRes.exceptionOrNull(),
                    "Incorrect password. Please try again."
                )
                _isDeletingAccount.value = false
                return@launch
            }

            val verifiedSession = verifyRes.getOrThrow()
            val deleteRes = supabaseDb.deleteAccountCompletely(verifiedSession)
            if (deleteRes.isFailure) {
                _deleteAccountError.value = ErrorSanitizer.sanitize(
                    deleteRes.exceptionOrNull(),
                    "Unable to delete account right now."
                )
                _isDeletingAccount.value = false
                return@launch
            }

            supabaseAuth.recordDeletionCooldowns(
                email = verifiedSession.email,
                username = currentProfile.username
            )
            supabaseAuth.clearSession()
            repository.clearAllUserData()
            ownerVaultStore.deleteOwnerVault(OwnerVaultStore.userKey(verifiedSession.userId))
            dirtyPendingSyncIds.clear()
            activeOwnerKey = null
            ownerVaultStore.setLastActiveOwnerKey(null)
            ownerVaultStore.setGuestModeActive(false)
            ownerVaultStore.rotateGuestId()

            _authSession.value = null
            _userProfile.value = null
            _privacySettings.value = PrivacySettings()
            _collections.value = emptyList()
            _communityMembers.value = emptyList()
            _communityReviews.value = emptyList()
            _communityActivity.value = emptyList()
            _animeReviews.value = emptyList()
            _invitations.value = emptyList()
            _inspectedMemberWatchlist.value = emptyList()
            _inspectedMemberCollections.value = emptyList()
            _storageStats.value = null
            _selectedAnimeId.value = null
            _selectedRichDetail.value = null
            _dynamicIslandFeedback.value = null
            _needsInviteRedemption.value = false
            _onboardingStage.value = OnboardingStage.NONE
            _pendingOnboardingUsername.value = ""
            _usernameAvailabilityError.value = null
            _profileError.value = null
            _deleteAccountError.value = null
            pendingSignUpEmail = null
            pendingSignUpPassword = null
            _isLocalPreviewMode.value = false
            syncDisplayedCatalogPersonalFlags()
            _isDeletingAccount.value = false
            _userMessage.value = "Account deleted. Your email can re-register after 5 minutes and your username after 24 hours."
            onSuccess()
        }
    }

    fun joinCommunity(inviteCode: String = "") {
        val session = _authSession.value
        val profile = _userProfile.value
        if (session == null || profile == null) {
            _userMessage.value = "Sign in first to join the community."
            return
        }
        viewModelScope.launch {
            _isCommunityLoading.value = true
            _communityError.value = null
            supabaseDb.joinCommunity(
                session = session,
                username = profile.username,
                avatarKey = profile.avatarKey,
                inviteCode = inviteCode
            ).onSuccess {
                _privacySettings.value = _privacySettings.value.copy(
                    reviewsVisibility = VisibilityLevel.COMMUNITY,
                    activityVisibility = VisibilityLevel.COMMUNITY
                )
                loadAuthenticatedWorkspace(session)
                _userMessage.value = "Joined community! Your notes and ratings are now shared automatically."
            }.onFailure { err ->
                val msg = ErrorSanitizer.sanitize(err, "Unable to join community.")
                _communityError.value = msg
                _userMessage.value = msg
            }
            _isCommunityLoading.value = false
        }
    }

    fun leaveCommunity() {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.leaveCommunity(session).onSuccess {
                _userProfile.value = _userProfile.value?.copy(memberStatus = MemberStatus.LEFT)
                _communityMembers.value = emptyList()
                _communityReviews.value = emptyList()
                _communityActivity.value = emptyList()
                _userMessage.value = "You have left the community."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Unable to leave community.")
            }
        }
    }

    private fun loadAuthenticatedWorkspace(session: AuthSession) {
        val capturedUserId = session.userId
        viewModelScope.launch {
            switchToOwnerVault(OwnerVaultStore.userKey(capturedUserId))
            val loaded = supabaseDb.fetchCurrentProfile(session).getOrNull()
                ?: supabaseDb.ensurePersonalProfile(session).getOrNull()
            if (_authSession.value?.userId != capturedUserId) return@launch
            if (loaded != null) {
                _userProfile.value = loaded
                supabaseAuth.cacheProfileSnapshot(loaded.username, loaded.avatarKey)
                _needsInviteRedemption.value = false
                supabaseDb.fetchPrivacySettings(session).onSuccess {
                    if (_authSession.value?.userId == capturedUserId) _privacySettings.value = it
                }
                val cloudWatchlistRes = supabaseDb.fetchUserWatchlist(session)
                cloudWatchlistRes.onSuccess { cloudItems ->
                    if (_authSession.value?.userId != capturedUserId) return@onSuccess
                    repository.syncCloudWatchlistEntries(
                        cloudItems = cloudItems,
                        replaceMissingFromCloud = true,
                        protectedPendingIds = dirtyPendingSyncIds.toSet()
                    )
                    migrateGuestVaultToAccountIfNeeded(session)
                    persistCurrentOwnerVaultSnapshot()
                    syncDisplayedCatalogPersonalFlags()
                }.onFailure {
                    migrateGuestVaultToAccountIfNeeded(session)
                    persistCurrentOwnerVaultSnapshot()
                }
                supabaseDb.fetchCollections(session, creatorUserId = capturedUserId).onSuccess {
                    if (_authSession.value?.userId == capturedUserId) _collections.value = it
                }
                if (loaded.memberStatus == MemberStatus.ACTIVE) {
                    refreshCommunityData()
                    if (loaded.isInviteCreator) {
                        fetchInvitations()
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // HOME & ANILIST METADATA DISCOVERY
    // ------------------------------------------------------------------------

    fun setSelectedDay(day: Int) {
        selectedDay.value = day.coerceIn(1, 7)
    }

    fun refreshHome() {
        viewModelScope.launch {
            _isHomeRefreshing.value = true
            _homeError.value = null
            val trendingRes = metadataProvider.getTrendingAnime(page = 1, perPage = 16)
            trendingRes.onSuccess { mediaList ->
                val entities = mediaList.map { AniListMetadataProvider.mapMediaToEntity(it) }
                repository.upsertCatalogItems(entities)
                _trendingAnime.value = entities
            }.onFailure { err ->
                _homeError.value = ErrorSanitizer.sanitize(err, "Unable to refresh schedule right now.")
            }
            repository.refreshSchedule()
            if (_authSession.value != null && _userProfile.value?.memberStatus == MemberStatus.ACTIVE) {
                refreshCommunityData()
            }
            _isHomeRefreshing.value = false
        }
    }

    fun ensureDiscoverLoaded() {
        if (_searchResults.value.isEmpty() && !_isSearching.value) {
            performSearch(resetPage = true)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        performSearch(resetPage = true)
    }

    fun updateFilterState(newState: DiscoverFilterState) {
        _filterState.value = newState
        performSearch(resetPage = true)
    }

    fun resetFilters() {
        _filterState.value = DiscoverFilterState()
        _searchQuery.value = ""
        performSearch(resetPage = true)
    }

    fun performSearch(resetPage: Boolean = true) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (resetPage) {
                currentDiscoverPage = 1
                hasNextDiscoverPage = true
                _isSearching.value = true
            } else {
                if (!hasNextDiscoverPage || _isNextPageLoading.value) return@launch
                _isNextPageLoading.value = true
            }
            _searchError.value = null

            val filters = _filterState.value
            val country = when {
                filters.mediaType == "DONGHUA" -> "CN"
                filters.country != "ALL" -> filters.country
                else -> null
            }
            val res = metadataProvider.searchCatalog(
                query = _searchQuery.value,
                genre = filters.selectedGenres.firstOrNull(),
                season = filters.season,
                seasonYear = filters.yearFrom,
                status = filters.status,
                format = filters.format,
                countryOfOrigin = country,
                sort = filters.sortBy,
                page = currentDiscoverPage,
                perPage = 24
            )
            res.onSuccess { (mediaList, hasNext) ->
                val entities = mediaList.map { media ->
                    val existing = repository.getAnimeByIdSync(media.id)
                    AniListMetadataProvider.mapMediaToEntity(media, existing)
                }.filter { entity ->
                    val safeOk = !preferences.value.safeModeEnabled || !entity.isAdult
                    val multiGenreOk = filters.selectedGenres.isEmpty() || filters.selectedGenres.all { g ->
                        entity.genres.contains(g, ignoreCase = true)
                    }
                    safeOk && multiGenreOk
                }
                repository.upsertCatalogItems(entities)
                hasNextDiscoverPage = hasNext
                if (resetPage) {
                    _searchResults.value = entities
                } else {
                    _searchResults.value = (_searchResults.value + entities).distinctBy { it.id }
                }
                if (hasNext) currentDiscoverPage += 1
            }.onFailure { err ->
                _searchError.value = ErrorSanitizer.sanitize(err, "Unable to load catalog right now.")
            }

            _isSearching.value = false
            _isNextPageLoading.value = false
        }
    }

    fun loadNextDiscoverPage() {
        if (hasNextDiscoverPage && !_isSearching.value && !_isNextPageLoading.value) {
            performSearch(resetPage = false)
        }
    }

    // ------------------------------------------------------------------------
    // DETAIL SCREEN
    // ------------------------------------------------------------------------

    fun selectAnimeDetail(animeId: Int) {
        _selectedAnimeId.value = animeId
        _selectedRichDetail.value = null
        _animeReviews.value = emptyList()
        viewModelScope.launch {
            _isDetailLoading.value = true
            metadataProvider.getMediaDetails(animeId).onSuccess { media ->
                if (media != null) {
                    val existing = repository.getAnimeByIdSync(media.id)
                    val mapped = AniListMetadataProvider.mapMediaToEntity(media, existing)
                    repository.upsertCatalogItems(listOf(mapped), keepFullSynopsis = true)
                    _selectedRichDetail.value = AniListMetadataProvider.mapRichDetail(media)
                    val enriched = metadataProvider.getRichDetailWithSeasons(media)
                    if (_selectedAnimeId.value == animeId) {
                        _selectedRichDetail.value = enriched
                    }
                }
            }
            val session = _authSession.value
            if (session != null && _userProfile.value?.memberStatus == MemberStatus.ACTIVE) {
                supabaseDb.fetchCommunityFeed(session, _communityMembers.value, animeIdFilter = animeId)
                    .onSuccess { (reviews, _) ->
                        _animeReviews.value = reviews
                    }
            }
            _isDetailLoading.value = false
        }
    }

    // ------------------------------------------------------------------------
    // WATCHLIST, PROGRESS, COLLECTIONS
    // ------------------------------------------------------------------------

    fun addToWatchlist(anime: AnimeEntity, status: WatchStatus = WatchStatus.WATCHING) {
        viewModelScope.launch {
            val saved = repository.addToWatchlist(anime, status)
            syncSingleEntryToCloud(saved)
            val syncText = if (_authSession.value != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Added • $syncText",
                subtitle = "${saved.displayTitle} (${status.label})",
                animeId = saved.id
            )
            _userMessage.value = "Added ${saved.displayTitle} to ${status.label}."
        }
    }

    fun incrementEpisode(animeId: Int) {
        viewModelScope.launch {
            val before = repository.getAnimeByIdSync(animeId) ?: return@launch
            val prevEpisode = before.watchedEpisodeCount
            val prevStatus = before.watchStatusEnum
            val updated = repository.incrementEpisode(animeId) ?: return@launch
            syncSingleEntryToCloud(updated)
            val syncText = if (_authSession.value != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = if (updated.watchStatusEnum == WatchStatus.COMPLETED) {
                    "Completed • $syncText"
                } else {
                    "Ep ${updated.watchedEpisodeCount} Logged • $syncText"
                },
                subtitle = updated.displayTitle,
                animeId = updated.id,
                previousEpisode = prevEpisode,
                previousStatus = prevStatus,
                canUndo = true
            )
            _userMessage.value = if (updated.watchStatusEnum == WatchStatus.COMPLETED) {
                "Completed ${updated.displayTitle}!"
            } else {
                "${updated.displayTitle}: Ep ${updated.watchedEpisodeCount}"
            }
        }
    }

    fun decrementEpisode(animeId: Int) {
        viewModelScope.launch {
            val before = repository.getAnimeByIdSync(animeId) ?: return@launch
            if (before.watchedEpisodeCount <= 0) return@launch
            val prevEpisode = before.watchedEpisodeCount
            val prevStatus = before.watchStatusEnum
            val targetEpisode = (prevEpisode - 1).coerceAtLeast(0)
            val nextStatus = if (prevStatus == WatchStatus.COMPLETED && (before.totalEpisodes == null || targetEpisode < before.totalEpisodes)) {
                WatchStatus.WATCHING
            } else {
                prevStatus
            }
            val updated = repository.updateUserTrackingState(
                animeId = animeId,
                status = nextStatus,
                episodeProgress = targetEpisode,
                score = before.userScore,
                notes = before.userNotes,
                isFavorite = before.isFavorite
            ) ?: return@launch
            syncSingleEntryToCloud(updated)
            val syncText = if (_authSession.value != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Ep ${updated.watchedEpisodeCount} Logged • $syncText",
                subtitle = updated.displayTitle,
                animeId = updated.id,
                previousEpisode = prevEpisode,
                previousStatus = prevStatus,
                canUndo = true
            )
        }
    }

    fun setEpisodeProgress(animeId: Int, targetEpisode: Int) {
        viewModelScope.launch {
            val before = repository.getAnimeByIdSync(animeId) ?: return@launch
            val prevEpisode = before.watchedEpisodeCount
            val prevStatus = before.watchStatusEnum
            val clamped = com.example.model.ProgressRules.clampEpisodeProgress(targetEpisode, before.totalEpisodes)
            val nextStatus = when {
                before.totalEpisodes != null && before.totalEpisodes > 0 && clamped >= before.totalEpisodes -> WatchStatus.COMPLETED
                !before.inWatchlist || before.watchStatusEnum == WatchStatus.PLAN_TO_WATCH -> WatchStatus.WATCHING
                else -> before.watchStatusEnum
            }
            val updated = repository.updateUserTrackingState(
                animeId = animeId,
                status = nextStatus,
                episodeProgress = clamped,
                score = before.userScore,
                notes = before.userNotes,
                isFavorite = before.isFavorite
            ) ?: return@launch
            syncSingleEntryToCloud(updated)
            val syncText = if (_authSession.value != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Ep ${updated.watchedEpisodeCount} Logged • $syncText",
                subtitle = updated.displayTitle,
                animeId = updated.id,
                previousEpisode = prevEpisode,
                previousStatus = prevStatus,
                canUndo = true
            )
        }
    }

    fun undoLastEpisodeAction() {
        val feedback = _dynamicIslandFeedback.value ?: return
        val targetId = feedback.animeId ?: return
        val prevEp = feedback.previousEpisode ?: return
        val prevStatus = feedback.previousStatus ?: WatchStatus.WATCHING
        viewModelScope.launch {
            val current = repository.getAnimeByIdSync(targetId) ?: return@launch
            val restored = repository.updateUserTrackingState(
                animeId = targetId,
                status = prevStatus,
                episodeProgress = prevEp,
                score = current.userScore,
                notes = current.userNotes,
                isFavorite = current.isFavorite
            )
            if (restored != null) {
                syncSingleEntryToCloud(restored)
            }
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Reverted to Ep $prevEp",
                subtitle = feedback.subtitle,
                animeId = targetId,
                canUndo = false
            )
        }
    }

    fun updateTracking(
        animeId: Int,
        status: WatchStatus,
        episodeProgress: Int,
        scoreTenths: Int?,
        notes: String,
        isFavorite: Boolean,
        seasonNumber: Int? = null
    ) {
        viewModelScope.launch {
            val updated = repository.updateUserTrackingState(
                animeId = animeId,
                status = status,
                episodeProgress = episodeProgress,
                seasonNumber = seasonNumber,
                score = scoreTenths,
                notes = notes,
                isFavorite = isFavorite
            ) ?: return@launch
            syncSingleEntryToCloud(updated)
            val syncText = if (_authSession.value != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Updated • $syncText",
                subtitle = "${updated.displayTitle} (S${updated.effectiveSeasonNumber} • ${status.label})",
                animeId = updated.id
            )
            _userMessage.value = "Saved tracking updates for ${updated.displayTitle}."
        }
    }

    fun setSeasonProgress(
        animeId: Int,
        seasonNumber: Int,
        resetEpisodes: Boolean = false
    ) {
        viewModelScope.launch {
            val updated = repository.updateSeasonProgress(
                animeId = animeId,
                seasonNumber = seasonNumber,
                resetEpisodes = resetEpisodes
            ) ?: return@launch
            syncSingleEntryToCloud(updated)
            val syncText = if (_authSession.value != null) "Synced" else "Saved"
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Season ${updated.effectiveSeasonNumber} • $syncText",
                subtitle = if (resetEpisodes) "${updated.displayTitle} • Ready for Ep 1" else updated.displayTitle,
                animeId = updated.id
            )
        }
    }

    fun toggleFavorite(animeId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            val updated = repository.toggleFavorite(animeId, isFavorite) ?: return@launch
            syncSingleEntryToCloud(updated)
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = if (isFavorite) "Favorited" else "Removed Favorite",
                subtitle = updated.displayTitle,
                animeId = updated.id
            )
        }
    }

    fun removeFromWatchlist(animeId: Int) {
        viewModelScope.launch {
            repository.removeFromWatchlist(animeId)
            _searchResults.value = _searchResults.value.map { item ->
                if (item.id == animeId) {
                    item.copy(inWatchlist = false, isWishlist = false, isPinned = false, isWatched = false, userStatus = null)
                } else item
            }
            val session = _authSession.value
            if (session != null) {
                dirtyPendingSyncIds.remove(animeId)
                supabaseDb.deleteUserAnimeEntry(session, animeId)
                _communityReviews.value = _communityReviews.value.filterNot {
                    it.animeId == animeId && it.userId == session.userId
                }
                _animeReviews.value = _animeReviews.value.filterNot {
                    it.animeId == animeId && it.userId == session.userId
                }
            }
            persistCurrentOwnerVaultSnapshot()
            _dynamicIslandFeedback.value = DynamicIslandFeedback(
                title = "Removed from List",
                subtitle = "Watchlist entry cleared",
                animeId = animeId
            )
            _userMessage.value = "Removed from My List."
        }
    }

    private suspend fun syncSingleEntryToCloud(entity: AnimeEntity) {
        persistCurrentOwnerVaultSnapshot()
        val session = _authSession.value ?: return
        dirtyPendingSyncIds.add(entity.id)
        supabaseDb.upsertUserAnimeEntry(session, entity).onSuccess {
            dirtyPendingSyncIds.remove(entity.id)
            val profile = _userProfile.value
            if (profile != null && profile.memberStatus == MemberStatus.ACTIVE) {
                val trimmedNote = entity.userNotes?.trim().orEmpty()
                val rating10 = entity.userScore?.takeIf { it > 0 }?.let { it / 10.0 }
                if (entity.hasActiveUserState && (trimmedNote.isNotBlank() || rating10 != null)) {
                    val existing = _communityReviews.value.firstOrNull {
                        it.animeId == entity.id && it.userId == session.userId
                    }
                    val optimisticReview = CommunityReview(
                        id = existing?.id ?: "${session.userId}-${entity.id}",
                        userId = session.userId,
                        username = profile.username,
                        avatarKey = profile.avatarKey,
                        avatarUrl = profile.avatarUrl,
                        animeId = entity.id,
                        animeTitle = entity.displayTitle,
                        animeCoverImage = entity.coverImage,
                        rating = rating10,
                        reviewNotes = trimmedNote,
                        status = entity.watchStatusEnum,
                        episodeProgress = entity.watchedEpisodeCount,
                        totalEpisodes = entity.totalEpisodes,
                        updatedAt = Instant.now().toString(),
                        likesCount = existing?.likesCount ?: 0,
                        likedByMe = existing?.likedByMe ?: false
                    )
                    val updatedCommunity = listOf(optimisticReview) + _communityReviews.value.filterNot {
                        it.animeId == entity.id && it.userId == session.userId
                    }
                    _communityReviews.value = sortReviews(updatedCommunity, _reviewSortOption.value)
                    if (_selectedAnimeId.value == entity.id) {
                        _animeReviews.value = listOf(optimisticReview) + _animeReviews.value.filterNot {
                            it.userId == session.userId
                        }
                    }
                } else {
                    _communityReviews.value = _communityReviews.value.filterNot {
                        it.animeId == entity.id && it.userId == session.userId
                    }
                    if (_selectedAnimeId.value == entity.id) {
                        _animeReviews.value = _animeReviews.value.filterNot { it.userId == session.userId }
                    }
                }
                refreshCommunityData()
            }
        }
    }

    fun submitCommunityCommentNote(
        animeId: Int,
        animeTitle: String,
        coverImage: String,
        ratingStars: Float,
        noteText: String
    ) {
        val cleanNote = noteText.trim()
        if (cleanNote.isBlank() && ratingStars <= 0f) {
            _userMessage.value = "Write a note or select a star rating first."
            return
        }
        viewModelScope.launch {
            val existing = repository.getAnimeByIdSync(animeId)
            val resolvedScoreTenths = if (ratingStars > 0f) {
                StarRatingUtils.starsToScore100(ratingStars)
            } else {
                existing?.userScore
            }
            val updated = if (existing != null) {
                repository.updateUserTrackingState(
                    animeId = animeId,
                    status = if (existing.inWatchlist) existing.watchStatusEnum else WatchStatus.WATCHING,
                    episodeProgress = existing.watchedEpisodeCount,
                    seasonNumber = existing.userSeason,
                    score = resolvedScoreTenths,
                    notes = cleanNote.ifBlank { existing.userNotes.orEmpty() },
                    isFavorite = existing.isFavorite
                )
            } else {
                val seeded = AnimeEntity(
                    id = animeId,
                    titleRomaji = animeTitle.ifBlank { "Anime #$animeId" },
                    titleEnglish = animeTitle.ifBlank { "Anime #$animeId" },
                    coverImage = coverImage,
                    airingDayOfWeek = ((animeId.coerceAtLeast(1) % 7) + 1)
                )
                repository.addToWatchlist(seeded, WatchStatus.WATCHING)
                repository.updateUserTrackingState(
                    animeId = animeId,
                    status = WatchStatus.WATCHING,
                    episodeProgress = 0,
                    seasonNumber = 1,
                    score = resolvedScoreTenths,
                    notes = cleanNote,
                    isFavorite = false
                )
            }
            if (updated != null) {
                syncSingleEntryToCloud(updated)
                _userMessage.value = "Added your note to $animeTitle in Community."
            }
        }
    }

    fun createCollection(name: String, description: String, visibility: VisibilityLevel) {
        val session = _authSession.value
        if (session == null || _userProfile.value == null) {
            _userMessage.value = "Sign in to create cloud collections."
            return
        }
        if (name.isBlank()) return
        viewModelScope.launch {
            supabaseDb.createCollection(session, name, description, visibility).onSuccess { created ->
                _collections.value = listOf(created) + _collections.value
                _userMessage.value = "Created collection \"${created.name}\"."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Failed to create collection.")
            }
        }
    }

    fun renameOrUpdateCollection(collectionId: String, name: String, visibility: VisibilityLevel) {
        val session = _authSession.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            supabaseDb.updateCollection(session, collectionId, name, visibility).onSuccess {
                _collections.value = _collections.value.map {
                    if (it.id == collectionId) it.copy(name = name.trim(), visibility = visibility) else it
                }
                _userMessage.value = "Collection updated."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Failed to update collection.")
            }
        }
    }

    fun deleteCollection(collectionId: String) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.deleteCollection(session, collectionId).onSuccess {
                _collections.value = _collections.value.filterNot { it.id == collectionId }
                _userMessage.value = "Collection deleted."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Failed to delete collection.")
            }
        }
    }

    fun addAnimeToCollection(collectionId: String, anime: AnimeEntity) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.addAnimeToCollection(session, collectionId, anime).onSuccess {
                _collections.value = _collections.value.map { col ->
                    if (col.id == collectionId && !col.animeIds.contains(anime.id)) {
                        col.copy(animeIds = col.animeIds + anime.id)
                    } else col
                }
                _userMessage.value = "Added to collection."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Failed to add to collection.")
            }
        }
    }

    fun syncCloudNow() {
        val session = _authSession.value
        if (session == null) {
            _userMessage.value = "Sign in to sync your library to the cloud."
            return
        }
        val capturedUserId = session.userId
        viewModelScope.launch {
            supabaseDb.fetchUserWatchlist(session).onSuccess { cloudItems ->
                if (_authSession.value?.userId != capturedUserId) return@onSuccess
                repository.syncCloudWatchlistEntries(
                    cloudItems = cloudItems,
                    replaceMissingFromCloud = true,
                    protectedPendingIds = dirtyPendingSyncIds.toSet()
                )
                persistCurrentOwnerVaultSnapshot()
                syncDisplayedCatalogPersonalFlags()
                _userMessage.value = "Library synced cleanly."
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Unable to sync right now.")
            }
        }
    }

    fun removeAnimeFromCollection(collectionId: String, animeId: Int) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.removeAnimeFromCollection(session, collectionId, animeId).onSuccess {
                _collections.value = _collections.value.map { col ->
                    if (col.id == collectionId) col.copy(animeIds = col.animeIds.filterNot { it == animeId }) else col
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // COMMUNITY, PROFILE, PRIVACY, INVITATIONS
    // ------------------------------------------------------------------------

    fun refreshCommunityData() {
        val session = _authSession.value ?: return
        if (_userProfile.value?.memberStatus != MemberStatus.ACTIVE) return
        viewModelScope.launch {
            _isCommunityLoading.value = true
            _communityError.value = null
            supabaseDb.fetchCommunityCapacity(session).onSuccess { _communityCapacity.value = it }
            val membersRes = supabaseDb.fetchCommunityMembers(session)
            membersRes.onSuccess { members ->
                _communityMembers.value = members
                supabaseDb.fetchCommunityFeed(session, members).onSuccess { (reviews, activity) ->
                    _communityReviews.value = sortReviews(reviews, _reviewSortOption.value)
                    _communityActivity.value = activity
                }.onFailure { err ->
                    _communityError.value = ErrorSanitizer.sanitize(err, "Unable to load community feed.")
                }
            }.onFailure { err ->
                _communityError.value = ErrorSanitizer.sanitize(err, "Unable to load community members.")
            }
            _isCommunityLoading.value = false
        }
    }

    fun setReviewSortOption(option: ReviewSortOption) {
        _reviewSortOption.value = option
        _communityReviews.value = sortReviews(_communityReviews.value, option)
    }

    private fun sortReviews(list: List<CommunityReview>, option: ReviewSortOption): List<CommunityReview> {
        return when (option) {
            ReviewSortOption.NEWEST -> list.sortedByDescending { it.updatedAt }
            ReviewSortOption.HIGHEST_RATED -> list.sortedWith(
                compareByDescending<CommunityReview> { it.rating ?: -1.0 }
                    .thenByDescending { it.updatedAt }
            )
            ReviewSortOption.MOST_LIKED -> list.sortedWith(
                compareByDescending<CommunityReview> { it.likesCount }
                    .thenByDescending { it.rating ?: -1.0 }
                    .thenByDescending { it.updatedAt }
            )
        }
    }

    fun toggleReviewLike(review: CommunityReview) {
        val session = _authSession.value ?: return
        val optimisticLiked = !review.likedByMe
        val optimisticCount = maxOf(0, review.likesCount + if (optimisticLiked) 1 else -1)
        _communityReviews.value = _communityReviews.value.map {
            if (it.id == review.id) it.copy(likedByMe = optimisticLiked, likesCount = optimisticCount) else it
        }
        _animeReviews.value = _animeReviews.value.map {
            if (it.id == review.id) it.copy(likedByMe = optimisticLiked, likesCount = optimisticCount) else it
        }
        viewModelScope.launch {
            supabaseDb.toggleReviewLike(session, review.id, review.likedByMe, review.likesCount)
                .onSuccess { (liked, count) ->
                    _communityReviews.value = _communityReviews.value.map {
                        if (it.id == review.id) it.copy(likedByMe = liked, likesCount = count) else it
                    }
                    _animeReviews.value = _animeReviews.value.map {
                        if (it.id == review.id) it.copy(likedByMe = liked, likesCount = count) else it
                    }
                }
        }
    }

    fun refreshStorageStats() {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            _isStorageStatsLoading.value = true
            supabaseDb.fetchDatabaseStorageStats(session).onSuccess { stats ->
                _storageStats.value = stats
            }
            _isStorageStatsLoading.value = false
        }
    }

    fun inspectMemberProfile(member: CommunityMember) {
        val session = _authSession.value ?: return
        _inspectedMemberWatchlist.value = emptyList()
        _inspectedMemberCollections.value = emptyList()
        viewModelScope.launch {
            _isMemberInspectLoading.value = true
            val canViewWatchlist = member.userId == session.userId ||
                member.privacy.watchlistVisibility == VisibilityLevel.COMMUNITY
            if (canViewWatchlist) {
                supabaseDb.fetchUserWatchlist(session, member.userId).onSuccess {
                    _inspectedMemberWatchlist.value = it
                }
            }
            val canViewCollections = member.userId == session.userId ||
                member.privacy.collectionsVisibility == VisibilityLevel.COMMUNITY
            if (canViewCollections) {
                supabaseDb.fetchCollections(session, creatorUserId = member.userId).onSuccess { allCols ->
                    _inspectedMemberCollections.value = allCols.filter {
                        it.creatorId == member.userId && (member.userId == session.userId || it.visibility == VisibilityLevel.COMMUNITY)
                    }
                }
            }
            _isMemberInspectLoading.value = false
        }
    }

    fun updateProfile(
        username: String,
        avatarKey: String,
        clearCustomAvatar: Boolean = true,
        bio: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val session = _authSession.value ?: return
        val current = _userProfile.value ?: return
        val cleanName = username.trim()
        if (cleanName.length !in 2..32) {
            _profileError.value = "Username must be between 2 and 32 characters."
            return
        }
        if (cleanName.contains("@")) {
            _profileError.value = "Usernames cannot contain the @ symbol."
            return
        }
        val targetAvatarUrl = if (clearCustomAvatar) null else current.avatarUrl
        viewModelScope.launch {
            _isProfileSaving.value = true
            _profileError.value = null
            if (!cleanName.equals(current.username, ignoreCase = true) &&
                supabaseAuth.checkLocalUsernameCooldownSeconds(cleanName) > 0L
            ) {
                _profileError.value = "That username was recently deleted and will be available again after 24 hours."
                _isProfileSaving.value = false
                return@launch
            }
            supabaseDb.updateProfile(session, cleanName, avatarKey, targetAvatarUrl, bio).onSuccess {
                _userProfile.value = current.copy(
                    username = cleanName,
                    avatarKey = avatarKey,
                    avatarUrl = targetAvatarUrl,
                    bio = ""
                )
                supabaseAuth.cacheProfileSnapshot(cleanName, avatarKey)
                refreshCommunityData()
                _userMessage.value = "Profile updated."
                onSuccess()
            }.onFailure { err ->
                val clean = ErrorSanitizer.sanitize(err, "Unable to update profile.")
                _profileError.value = clean
            }
            _isProfileSaving.value = false
        }
    }

    fun updatePrivacySettings(updated: PrivacySettings) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            val previous = _privacySettings.value
            _privacySettings.value = updated
            supabaseDb.savePrivacySettings(session, updated).onSuccess {
                refreshCommunityData()
                _userMessage.value = "Privacy settings updated."
            }.onFailure { err ->
                _privacySettings.value = previous
                _userMessage.value = ErrorSanitizer.sanitize(err, "Unable to save privacy settings.")
            }
        }
    }

    fun fetchInvitations() {
        val session = _authSession.value ?: return
        if (_userProfile.value?.isInviteCreator != true) return
        viewModelScope.launch {
            supabaseDb.fetchInvitations(session).onSuccess { _invitations.value = it }
        }
    }

    fun generateInvitation(maxUses: Int = 1, expiresInHours: Int = 168) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.createInvitation(session, maxUses, expiresInHours).onSuccess { invite ->
                _invitations.value = listOf(invite) + _invitations.value
                _userMessage.value = "Generated invite code: ${invite.code}"
            }.onFailure { err ->
                _userMessage.value = ErrorSanitizer.sanitize(err, "Unable to create invitation.")
            }
        }
    }

    fun revokeInvitation(invitationId: String) {
        val session = _authSession.value ?: return
        viewModelScope.launch {
            supabaseDb.revokeInvitation(session, invitationId).onSuccess {
                _invitations.value = _invitations.value.map {
                    if (it.id == invitationId) it.copy(isActive = false) else it
                }
                _userMessage.value = "Invitation revoked."
            }
        }
    }

    // ------------------------------------------------------------------------
    // APP PREFERENCES & BACKUP
    // ------------------------------------------------------------------------

    fun setThemeMode(mode: ThemeModePreference) = viewModelScope.launch { preferencesRepo.setThemeMode(mode) }
    fun setAccentTheme(accent: String) = viewModelScope.launch { preferencesRepo.setAccentTheme(accent) }
    fun setWatchlistLayout(layout: WatchlistLayoutMode) = viewModelScope.launch { preferencesRepo.setWatchlistLayout(layout) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepo.setNotificationsEnabled(enabled) }
    fun setTitleLanguage(lang: String) = viewModelScope.launch { preferencesRepo.setTitleLanguage(lang) }
    fun setSafeModeEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepo.setSafeModeEnabled(enabled)
        performSearch(resetPage = true)
    }

    fun clearCatalogCache() {
        viewModelScope.launch {
            val count = repository.clearMetadataAndApiCache()
            _userMessage.value = "Cleared $count cached catalog entries. Your saved list is safe."
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackupJson()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                _userMessage.value = "Backup exported successfully."
            } catch (e: Exception) {
                _userMessage.value = "Export failed: ${ErrorSanitizer.sanitize(e, "Unable to write backup file.")}"
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: throw IllegalStateException("Unable to read selected file.")
                val res = repository.importBackupJson(json)
                _userMessage.value = res.message
            } catch (e: Exception) {
                _userMessage.value = "Import failed: ${ErrorSanitizer.sanitize(e, "Unable to read backup file.")}"
            }
        }
    }
}
