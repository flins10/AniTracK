package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.VideoFile
import com.example.ui.components.EpisodeGridSelectorDialog
import com.example.ui.components.MxGestureType
import com.example.ui.components.MxPlayerGestureHud
import com.example.ui.components.RecentLocalVideosSheet
import com.example.ui.components.UpNextEpisodeBanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.example.data.repository.ArchiveCandidateMatch
import com.example.ui.components.AspectRatioSelectionSheet
import com.example.ui.components.AudioSubtitleTracksSheet
import com.example.ui.components.BufferedSeekBar
import com.example.ui.components.DoubleTapSeekBubble
import com.example.ui.components.PlaybackSpeedRuler
import com.example.ui.components.QualitySelectionSheet
import com.example.viewmodel.AnimeViewModel
import com.example.viewmodel.PlayerUiState
import com.example.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import java.util.Locale

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Modern, full-featured Android streaming player for Internet Archive media:
 * - Persistent playback through rotations (ExoPlayer kept in PlayerViewModel)
 * - Safe areas and edge-to-edge system insets (no clipped controls)
 * - Scrollable 1.0x to 4.0x speed ruler with quick presets & reset
 * - Actual quality selection with seamless position preservation
 * - Multilingual audio & subtitles (ja, zh, hi, en, etc.) + local subtitle loader + offset sync
 * - Pinch-to-zoom, pan, double-tap zoom/seek, aspect ratio modes
 * - Touch lock, Screen-awake, Picture-in-Picture, audio focus
 * - Clean UI without raw API dumps or cryptic identifiers
 */
@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun StreamingPlayerScreen(
    animeId: Int,
    initialEpisode: Int,
    initialSeason: Int = 1,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    viewModel: AnimeViewModel? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Player State from ViewModel
    val uiState by playerViewModel.uiState.collectAsState()
    val anime by playerViewModel.currentAnime.collectAsState()
    val currentEpisode by playerViewModel.currentEpisode.collectAsState()
    val currentSeasonNumber by playerViewModel.currentSeasonNumber.collectAsState()
    val availableSeasons by playerViewModel.availableSeasons.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isBuffering by playerViewModel.isBuffering.collectAsState()
    val currentPositionMs by playerViewModel.currentPositionMs.collectAsState()
    val bufferedPositionMs by playerViewModel.bufferedPositionMs.collectAsState()
    val durationMs by playerViewModel.durationMs.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val playbackError by playerViewModel.playbackError.collectAsState()
    val resizeMode by playerViewModel.resizeMode.collectAsState()
    val zoomScale by playerViewModel.zoomScale.collectAsState()
    val panOffsetX by playerViewModel.panOffsetX.collectAsState()
    val panOffsetY by playerViewModel.panOffsetY.collectAsState()
    val activeQuality by playerViewModel.activeQuality.collectAsState()
    val availableQualities by playerViewModel.availableQualities.collectAsState()
    val audioTracks by playerViewModel.audioTracks.collectAsState()
    val subtitleTracks by playerViewModel.subtitleTracks.collectAsState()
    val isSubtitleOff by playerViewModel.isSubtitleOff.collectAsState()
    val subtitleStyle by playerViewModel.subtitleStyle.collectAsState()
    val isTouchLocked by playerViewModel.isTouchLocked.collectAsState()
    val isScreenAwake by playerViewModel.isScreenAwake.collectAsState()
    val seekIncrement by playerViewModel.seekIncrementSeconds.collectAsState()
    val isAutoNext by playerViewModel.isAutoNextEnabled.collectAsState()
    val activeCandidates by playerViewModel.activeCandidates.collectAsState()
    val activeCandidate by playerViewModel.activeCandidate.collectAsState()
    val userMessage by playerViewModel.userMessage.collectAsState()
    val isLocalVideoMode by playerViewModel.isLocalVideoMode.collectAsState()
    val localVideoTitle by playerViewModel.localVideoTitle.collectAsState()
    val recentLocalVideos by playerViewModel.recentLocalVideos.collectAsState()
    val isAudioOnlyMode by playerViewModel.isAudioOnlyMode.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // Screen UI State
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var userInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Bottom Sheets & Dialogs
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }
    var showAspectRatioSheet by remember { mutableStateOf(false) }
    var showCandidatesSheet by remember { mutableStateOf(false) }
    var showLocalVideosSheet by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var customSearchQuery by remember { mutableStateOf("") }

    // System file picker for opening local device videos (MP4, MKV, WebM, HLS, DASH)
    val localVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            playerViewModel.loadLocalVideo(uri)
            showLocalVideosSheet = false
        }
    }

    // Double-tap Seek Animation State
    var showDoubleTapLeft by remember { mutableStateOf(false) }
    var showDoubleTapRight by remember { mutableStateOf(false) }

    // Lifecycle Handling: Pause cleanly when backgrounded unless in PiP or Audio-Only mode
    DisposableEffect(lifecycleOwner, activity, isAudioOnlyMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    activity?.isInPictureInPictureMode == true
                } else false
                if (!inPip && !isAudioOnlyMode) {
                    playerViewModel.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Screen Awake Window Flag Handling
    DisposableEffect(isScreenAwake) {
        val window = activity?.window
        if (isScreenAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Edge-to-Edge System Bars Visibility Handling in Fullscreen
    DisposableEffect(isFullscreen, isLandscape) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            if (isFullscreen || isLandscape) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = activity?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto-hide controls timer (4s)
    LaunchedEffect(showControls, isPlaying, userInteractionTime, isTouchLocked) {
        if (showControls && isPlaying && !isTouchLocked) {
            delay(4000)
            showControls = false
        }
    }

    // Resolve fallback anime from AnimeViewModel if watching an anime not yet in local DB
    val detailState = viewModel?.selectedAnimeDetail?.collectAsState()
    val searchState = viewModel?.searchResults?.collectAsState()
    val watchlistState = viewModel?.watchlist?.collectAsState()
    val favoritesState = viewModel?.favorites?.collectAsState()
    val wishlistState = viewModel?.wishlist?.collectAsState()
    val ongoingState = viewModel?.allOngoingAnime?.collectAsState()

    val fallbackAnime = remember(
        animeId,
        detailState?.value,
        searchState?.value,
        watchlistState?.value,
        favoritesState?.value,
        wishlistState?.value,
        ongoingState?.value
    ) {
        detailState?.value?.takeIf { it.id == animeId }
            ?: searchState?.value?.find { it.id == animeId }
            ?: watchlistState?.value?.find { it.id == animeId }
            ?: favoritesState?.value?.find { it.id == animeId }
            ?: wishlistState?.value?.find { it.id == animeId }
            ?: ongoingState?.value?.find { it.id == animeId }
    }

    val detailSeasonsState = viewModel?.activeDetailSeasons?.collectAsState()
    LaunchedEffect(detailSeasonsState?.value) {
        detailSeasonsState?.value?.let { seasons ->
            if (seasons.isNotEmpty()) {
                playerViewModel.setAvailableSeasons(seasons)
            }
        }
    }

    // Load episode on mount or when episode/season changes
    LaunchedEffect(animeId, initialEpisode, initialSeason, fallbackAnime) {
        playerViewModel.loadEpisode(animeId, initialEpisode, initialSeason, fallbackAnime)
    }

    BackHandler {
        when {
            isTouchLocked -> playerViewModel.toggleTouchLock()
            isFullscreen -> {
                isFullscreen = false
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            isLandscape -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> onBack()
        }
    }

    val totalEpisodes = anime?.totalEpisodes ?: maxOf(anime?.getAiredEpisodeCount() ?: 1, currentEpisode)

    // Fullscreen or in-app column
    val isPlayerFillingScreen = isFullscreen || isLandscape

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060913))
            .testTag("streaming_player_screen")
    ) {
        if (isPlayerFillingScreen) {
            // Fullscreen Player Container
            VideoPlayerSurfaceContainer(
                playerViewModel = playerViewModel,
                isLandscape = isLandscape,
                isFullscreen = true,
                showControls = showControls,
                onToggleControls = {
                    if (!isTouchLocked) {
                        showControls = !showControls
                        userInteractionTime = System.currentTimeMillis()
                    }
                },
                onUserInteraction = { userInteractionTime = System.currentTimeMillis() },
                onFullscreenToggle = {
                    isFullscreen = false
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                },
                onOrientationToggle = {
                    val target = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    activity?.requestedOrientation = target
                },
                onOpenSpeed = { showSpeedSheet = true },
                onOpenQuality = { showQualitySheet = true },
                onOpenTracks = { showTracksSheet = true },
                onOpenAspectRatio = { showAspectRatioSheet = true },
                onEnterPip = { enterPipMode(context) },
                onShowDoubleTapLeft = {
                    showDoubleTapLeft = true
                    userInteractionTime = System.currentTimeMillis()
                },
                onShowDoubleTapRight = {
                    showDoubleTapRight = true
                    userInteractionTime = System.currentTimeMillis()
                },
                onBack = {
                    isFullscreen = false
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Standard Portrait Screen Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = if (isLocalVideoMode) (localVideoTitle ?: "Local Device Video") else (anime?.displayTitle ?: "Streaming Player"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isLocalVideoMode) {
                                Surface(
                                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ANIKOTO API",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF38BDF8)
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (isLocalVideoMode) "Local Device Storage" else "Episode $currentEpisode${if (totalEpisodes > 1) " / $totalEpisodes" else ""}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Open Local Device Video Button
                        OutlinedButton(
                            onClick = {
                                playerViewModel.refreshRecentLocalVideos()
                                showLocalVideosSheet = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("open_local_video_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Open Local Video", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Local", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        if (!isLocalVideoMode) {
                            // Episode Selector Button
                            OutlinedButton(
                                onClick = { showJumpDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("player_custom_episode_button")
                            ) {
                                Text("Ep $currentEpisode ▾", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // Main Media Window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    VideoPlayerSurfaceContainer(
                        playerViewModel = playerViewModel,
                        isLandscape = false,
                        isFullscreen = false,
                        showControls = showControls,
                        onToggleControls = {
                            if (!isTouchLocked) {
                                showControls = !showControls
                                userInteractionTime = System.currentTimeMillis()
                            }
                        },
                        onUserInteraction = { userInteractionTime = System.currentTimeMillis() },
                        onFullscreenToggle = {
                            isFullscreen = true
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        },
                        onOrientationToggle = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        },
                        onOpenSpeed = { showSpeedSheet = true },
                        onOpenQuality = { showQualitySheet = true },
                        onOpenTracks = { showTracksSheet = true },
                        onOpenAspectRatio = { showAspectRatioSheet = true },
                        onEnterPip = { enterPipMode(context) },
                        onShowDoubleTapLeft = {
                            showDoubleTapLeft = true
                            userInteractionTime = System.currentTimeMillis()
                        },
                        onShowDoubleTapRight = {
                            showDoubleTapRight = true
                            userInteractionTime = System.currentTimeMillis()
                        },
                        onBack = onBack,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Below Video Content Tabs & Controls
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Action Buttons Row (Speed, Quality, Subtitles, Aspect Ratio)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSpeedSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(String.format(Locale.ROOT, "%.1f×", playbackSpeed), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showQualitySheet = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(activeQuality?.label?.substringBefore(" ") ?: "Quality", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { showTracksSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Subtitles, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSubtitleOff) "Sub: Off" else "Tracks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showAspectRatioSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Zoom", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Seasons Selector Card (if seasons available)
                    if (availableSeasons.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "AVAILABLE SEASONS",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(availableSeasons) { season ->
                                            val isSelectedSeason = season.seasonNumber == currentSeasonNumber || season.animeId == anime?.id
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelectedSeason) Color(0xFF0284C7) else Color(0xFF1E293B),
                                                border = BorderStroke(1.dp, if (isSelectedSeason) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)),
                                                modifier = Modifier.clickable {
                                                    playerViewModel.selectSeason(season)
                                                }
                                            ) {
                                                Text(
                                                    text = season.title.ifBlank { "Season ${season.seasonNumber}" },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Episode Navigation Buttons & Completion
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { playerViewModel.previousEpisode() },
                                            enabled = currentEpisode > 1 || (availableSeasons.indexOfFirst { it.seasonNumber == currentSeasonNumber } > 0),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Episode", tint = if (currentEpisode > 1 || (availableSeasons.indexOfFirst { it.seasonNumber == currentSeasonNumber } > 0)) Color.White else Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable { showJumpDialog = true }
                                        ) {
                                            Text(
                                                text = "Ep $currentEpisode${if (totalEpisodes > 1) " / $totalEpisodes" else ""} (S$currentSeasonNumber)",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { playerViewModel.nextEpisode(maxOf(totalEpisodes, currentEpisode + 1)) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.SkipNext, contentDescription = "Next Episode", tint = Color.White)
                                        }
                                    }

                                    Button(
                                        onClick = { showCompletionDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Mark Watched", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Quick Custom Episode Selection Chips Row
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val totalEpCount = maxOf(totalEpisodes, currentEpisode, 1)
                                    val startEp = maxOf(1, currentEpisode - 25)
                                    val endEp = minOf(totalEpCount, maxOf(startEp + 50, currentEpisode + 25))
                                    val epList = (startEp..endEp).toList()

                                    items(epList) { epNum ->
                                        val isCurrent = epNum == currentEpisode
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrent) Color(0xFF0284C7) else Color(0xFF1E293B),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isCurrent) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)
                                            ),
                                            modifier = Modifier.clickable {
                                                playerViewModel.loadEpisode(animeId, epNum, currentSeasonNumber, fallbackAnime)
                                            }
                                        ) {
                                            Text(
                                                text = "Ep $epNum",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                    item {
                                        OutlinedButton(
                                            onClick = { showJumpDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Select Ep...", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Auto-Play Next Episode", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    Switch(
                                        checked = isAutoNext,
                                        onCheckedChange = { playerViewModel.setAutoNext(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF0284C7)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Available Verified Streams from Internet Archive
                    if (activeCandidates.size > 1) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ANIKOTO API STREAMS (${activeCandidates.size})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                        )
                                        TextButton(onClick = { showCandidatesSheet = true }) {
                                            Text("View All", fontSize = 12.sp, color = Color(0xFF38BDF8))
                                        }
                                    }

                                    activeCandidates.take(3).forEach { cand ->
                                        val isSelected = (cand.identifier == activeCandidate?.identifier)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.06f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { playerViewModel.selectCandidate(cand) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = cand.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${cand.bestPlayableFile.format ?: "MP4"} • ${if (cand.isEpisodeSpecific) "Exact Episode Match" else "Verified Stream"}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // In-App Alternate Title Search
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1322)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SEARCH ALTERNATE TITLE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customSearchQuery,
                                        onValueChange = { customSearchQuery = it },
                                        placeholder = { Text("Enter alternate anime title...", color = Color(0xFF64748B), fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val q = customSearchQuery.trim()
                                            if (q.isNotBlank()) {
                                                playerViewModel.retryWithCustomTitle(q)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Animated Double-Tap Seek Bubbles
        DoubleTapSeekBubble(
            isForward = false,
            seconds = seekIncrement,
            visible = showDoubleTapLeft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        )

        DoubleTapSeekBubble(
            isForward = true,
            seconds = seekIncrement,
            visible = showDoubleTapRight,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        )

        LaunchedEffect(showDoubleTapLeft) {
            if (showDoubleTapLeft) {
                delay(650)
                showDoubleTapLeft = false
            }
        }

        LaunchedEffect(showDoubleTapRight) {
            if (showDoubleTapRight) {
                delay(650)
                showDoubleTapRight = false
            }
        }

        // Speed Ruler & Skip Interval Bottom Sheet
        if (showSpeedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSpeedSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PlaybackSpeedRuler(
                        currentSpeed = playbackSpeed,
                        onSpeedSelected = { speed ->
                            playerViewModel.setPlaybackSpeed(speed)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SKIP / SEEK INTERVAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { sec ->
                            val isSelected = (seekIncrement == sec)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { playerViewModel.setSeekIncrement(sec) }
                            ) {
                                Text(
                                    text = "${sec}s",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Quality Selection Bottom Sheet
        if (showQualitySheet) {
            QualitySelectionSheet(
                options = availableQualities,
                activeFile = activeQuality?.file?.name,
                onQualitySelected = { playerViewModel.switchQuality(it) },
                onDismiss = { showQualitySheet = false }
            )
        }

        // Tracks Bottom Sheet (Audio & Subtitles & Sync & Audio-Only Mode)
        if (showTracksSheet) {
            AudioSubtitleTracksSheet(
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                isSubtitleOff = isSubtitleOff,
                subtitleStyle = subtitleStyle,
                onAudioTrackSelected = { playerViewModel.selectAudioTrack(it) },
                onSubtitleTrackSelected = { playerViewModel.selectSubtitleTrack(it) },
                onSubtitleOffSelected = { playerViewModel.disableSubtitles() },
                onSubtitleStyleChanged = { playerViewModel.updateSubtitleStyle(it) },
                onLoadLocalSubtitle = { playerViewModel.loadLocalSubtitle(it) },
                onLoadSubtitleUrl = { playerViewModel.loadSubtitleFromUrl(it) },
                onDismiss = { showTracksSheet = false },
                isAudioOnlyMode = isAudioOnlyMode,
                onToggleAudioOnlyMode = { playerViewModel.setAudioOnlyMode(it) }
            )
        }

        // Aspect Ratio & Zoom Bottom Sheet
        if (showAspectRatioSheet) {
            AspectRatioSelectionSheet(
                currentMode = resizeMode,
                onModeSelected = { playerViewModel.setResizeMode(it) },
                zoomScale = zoomScale,
                onResetZoom = { playerViewModel.resetZoom() },
                onDismiss = { showAspectRatioSheet = false }
            )
        }

        // Recent Local Device Videos Sheet
        if (showLocalVideosSheet) {
            RecentLocalVideosSheet(
                recentVideos = recentLocalVideos,
                onOpenLocalFilePicker = {
                    localVideoPickerLauncher.launch(
                        arrayOf("video/*", "application/x-mpegURL", "application/dash+xml")
                    )
                },
                onSelectRecentVideo = { item ->
                    playerViewModel.loadLocalVideo(Uri.parse(item.uriString), item.displayName)
                    showLocalVideosSheet = false
                },
                onRemoveRecentVideo = { uriStr ->
                    playerViewModel.removeLocalVideoFromHistory(uriStr)
                },
                onDismiss = { showLocalVideosSheet = false }
            )
        }

        // Candidates Selection Bottom Sheet
        if (showCandidatesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCandidatesSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Available Streams",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activeCandidates) { cand ->
                            val isSelected = (cand.identifier == activeCandidate?.identifier)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerViewModel.selectCandidate(cand)
                                        showCandidatesSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cand.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                        Text(
                                            text = "${cand.bestPlayableFile.format ?: "MP4"} • ${if (cand.isEpisodeSpecific) "Exact Episode Match" else "Verified Stream"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF38BDF8))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Episode Completion Dialog
        if (showCompletionDialog) {
            AlertDialog(
                onDismissRequest = { showCompletionDialog = false },
                containerColor = Color(0xFF0F172A),
                titleContentColor = Color.White,
                textContentColor = Color(0xFF94A3B8),
                title = { Text("Episode $currentEpisode Completed 🎉") },
                text = { Text("Mark Episode $currentEpisode as watched in your library tracker?") },
                confirmButton = {
                    Button(
                        onClick = {
                            playerViewModel.confirmEpisodeCompletion()
                            showCompletionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCompletionDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        // Direct Episode Selection Dialog (5-Column Grid matching reference design)
        if (showJumpDialog) {
            val totalEpPlayer = maxOf(totalEpisodes, currentEpisode, 1)
            EpisodeGridSelectorDialog(
                animeTitle = anime?.displayTitle ?: "Episode Selection",
                totalEpisodes = totalEpPlayer,
                currentEpisode = currentEpisode,
                watchedEpisodeCount = anime?.watchedEpisodeCount ?: (currentEpisode - 1).coerceAtLeast(0),
                onDismissRequest = { showJumpDialog = false },
                onWatchEpisode = { ep ->
                    playerViewModel.loadEpisode(
                        animeId = animeId,
                        episodeNumber = ep,
                        seasonNumber = currentSeasonNumber,
                        fallbackAnime = anime ?: fallbackAnime
                    )
                    showJumpDialog = false
                },
                onSetWatchedCount = { ep ->
                    anime?.let { currentAnime ->
                        viewModel?.updateWatchedEpisodeCount(currentAnime.id, ep)
                    }
                    showJumpDialog = false
                }
            )
        }
    }
}

/**
 * Surface hosting the ExoPlayer View with Pinch-to-Zoom, Overlay Controls,
 * Safe Area padding (navigationBarsPadding, statusBarsPadding), and gestures.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerSurfaceContainer(
    playerViewModel: PlayerViewModel,
    isLandscape: Boolean,
    isFullscreen: Boolean,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onUserInteraction: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onOrientationToggle: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenTracks: () -> Unit,
    onOpenAspectRatio: () -> Unit,
    onEnterPip: () -> Unit,
    onShowDoubleTapLeft: () -> Unit,
    onShowDoubleTapRight: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val view = LocalView.current
    val uiState by playerViewModel.uiState.collectAsState()
    val anime by playerViewModel.currentAnime.collectAsState()
    val currentEpisode by playerViewModel.currentEpisode.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isBuffering by playerViewModel.isBuffering.collectAsState()
    val currentPositionMs by playerViewModel.currentPositionMs.collectAsState()
    val bufferedPositionMs by playerViewModel.bufferedPositionMs.collectAsState()
    val durationMs by playerViewModel.durationMs.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val playbackError by playerViewModel.playbackError.collectAsState()
    val resizeMode by playerViewModel.resizeMode.collectAsState()
    val zoomScale by playerViewModel.zoomScale.collectAsState()
    val panOffsetX by playerViewModel.panOffsetX.collectAsState()
    val panOffsetY by playerViewModel.panOffsetY.collectAsState()
    val isTouchLocked by playerViewModel.isTouchLocked.collectAsState()
    val subtitleStyle by playerViewModel.subtitleStyle.collectAsState()
    val seekIncrement by playerViewModel.seekIncrementSeconds.collectAsState()
    val isLocalVideoMode by playerViewModel.isLocalVideoMode.collectAsState()
    val localVideoTitle by playerViewModel.localVideoTitle.collectAsState()
    val isAudioOnlyMode by playerViewModel.isAudioOnlyMode.collectAsState()

    var isMuted by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var dismissedUpNextForEp by remember(currentEpisode) { mutableStateOf(false) }

    // MX Player Gesture HUD State
    var activeGestureType by remember { mutableStateOf(MxGestureType.NONE) }
    var brightnessFraction by remember {
        val currentWinBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (currentWinBrightness in 0.01f..1.0f) currentWinBrightness else 0.7f)
    }
    var volumeFraction by remember {
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVol / 2)
        mutableFloatStateOf((curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f))
    }
    var seekPreviewPosMs by remember { mutableLongStateOf(0L) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(activeGestureType) {
        if (activeGestureType == MxGestureType.BRIGHTNESS || activeGestureType == MxGestureType.VOLUME) {
            delay(1100)
            activeGestureType = MxGestureType.NONE
        }
    }

    val totalEpisodes = anime?.totalEpisodes ?: maxOf(anime?.getAiredEpisodeCount() ?: 1, currentEpisode)

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black)
            .mxPlayerSwipeAndPinchGestures(
                isTouchLocked = isTouchLocked,
                onUserInteraction = onUserInteraction,
                onPinchZoomAndPan = { zoomChange, panX, panY ->
                    val newZoom = (zoomScale * zoomChange).coerceIn(1.0f, 3.0f)
                    playerViewModel.setZoomScale(newZoom)
                    if (newZoom > 1.05f) {
                        playerViewModel.setPanOffset(panOffsetX + panX, panOffsetY + panY)
                    } else {
                        playerViewModel.setPanOffset(0f, 0f)
                    }
                },
                onBrightnessDelta = { deltaFrac ->
                    val updated = (brightnessFraction + deltaFrac).coerceIn(0.02f, 1.0f)
                    brightnessFraction = updated
                    activeGestureType = MxGestureType.BRIGHTNESS
                    activity?.window?.let { win ->
                        val lp = win.attributes
                        lp.screenBrightness = updated
                        win.attributes = lp
                    }
                },
                onVolumeDelta = { deltaFrac ->
                    val updated = (volumeFraction + deltaFrac).coerceIn(0f, 1f)
                    volumeFraction = updated
                    activeGestureType = MxGestureType.VOLUME
                    isMuted = (updated <= 0.01f)
                    playerViewModel.exoPlayer.volume = updated
                    audioManager?.let { am ->
                        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        val targetStep = kotlin.math.round(updated * maxVol).toInt().coerceIn(0, maxVol)
                        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, targetStep, 0) }
                    }
                },
                getCurrentPositionMs = { currentPositionMs },
                getDurationMs = { durationMs },
                onHorizontalSeekPreview = { previewPos, delta ->
                    seekPreviewPosMs = previewPos
                    seekDeltaMs = delta
                    activeGestureType = MxGestureType.HORIZONTAL_SEEK
                },
                onHorizontalSeekCommit = { targetPos ->
                    playerViewModel.seekTo(targetPos)
                    activeGestureType = MxGestureType.NONE
                },
                onGestureEnded = {
                    if (activeGestureType == MxGestureType.HORIZONTAL_SEEK) {
                        activeGestureType = MxGestureType.NONE
                    }
                }
            )
            .pointerInput(isTouchLocked, seekIncrement, zoomScale) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        if (isTouchLocked) return@detectTapGestures
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.35f) {
                            // Left Double Tap: Rewind
                            playerViewModel.seekBy(-seekIncrement)
                            onShowDoubleTapLeft()
                        } else if (offset.x > screenWidth * 0.65f) {
                            // Right Double Tap: Forward
                            playerViewModel.seekBy(seekIncrement)
                            onShowDoubleTapRight()
                        } else {
                            // Center Double Tap: Play/Pause or Zoom Reset
                            if (zoomScale > 1.05f) {
                                playerViewModel.resetZoom()
                            } else {
                                playerViewModel.togglePlayPause()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Player Surface with Zoom & Pan Transforms applied
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerViewModel.exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    this.resizeMode = resizeMode
                    subtitleView?.apply {
                        setApplyEmbeddedStyles(false)
                        setApplyEmbeddedFontSizes(false)
                        setBottomPaddingFraction(subtitleStyle.bottomPaddingFraction)
                        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * (subtitleStyle.fontSizeSp / 18f))
                        val edgeConst = when (subtitleStyle.edgeType) {
                            0 -> CaptionStyleCompat.EDGE_TYPE_NONE
                            2 -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                            3 -> CaptionStyleCompat.EDGE_TYPE_RAISED
                            else -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                        }
                        setStyle(
                            CaptionStyleCompat(
                                subtitleStyle.textColor.toInt(),
                                subtitleStyle.backgroundColor.toInt(),
                                android.graphics.Color.TRANSPARENT,
                                edgeConst,
                                subtitleStyle.outlineColor.toInt(),
                                null
                            )
                        )
                    }
                }
            },
            update = { playerView ->
                playerView.player = playerViewModel.exoPlayer
                playerView.resizeMode = resizeMode
                playerView.subtitleView?.apply {
                    setBottomPaddingFraction(subtitleStyle.bottomPaddingFraction)
                    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * (subtitleStyle.fontSizeSp / 18f))
                    val edgeConst = when (subtitleStyle.edgeType) {
                        0 -> CaptionStyleCompat.EDGE_TYPE_NONE
                        2 -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                        3 -> CaptionStyleCompat.EDGE_TYPE_RAISED
                        else -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                    }
                    setStyle(
                        CaptionStyleCompat(
                            subtitleStyle.textColor.toInt(),
                            subtitleStyle.backgroundColor.toInt(),
                            android.graphics.Color.TRANSPARENT,
                            edgeConst,
                            subtitleStyle.outlineColor.toInt(),
                            null
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffsetX
                    translationY = panOffsetY
                }
        )

        // Audio-Only Mode Overlay
        if (isAudioOnlyMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070C18)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7).copy(alpha = 0.22f),
                        border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "Audio-Only Mode",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Audio-Only Playback Active",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (isLocalVideoMode) (localVideoTitle ?: "Local Video") else "${anime?.displayTitle ?: "Anime"} • Ep $currentEpisode",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { playerViewModel.setAudioOnlyMode(false) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Restore Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // MX Player Gesture HUD (Brightness / Volume / Horizontal Seek Scrub)
        MxPlayerGestureHud(
            gestureType = activeGestureType,
            fractionValue = if (activeGestureType == MxGestureType.BRIGHTNESS) brightnessFraction else volumeFraction,
            seekPreviewPositionMs = seekPreviewPosMs,
            seekDeltaMs = seekDeltaMs,
            durationMs = durationMs,
            modifier = Modifier.align(Alignment.Center)
        )

        // Floating Reset Zoom Button if Zoomed In
        if (zoomScale > 1.05f && showControls && !isTouchLocked) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .clickable { playerViewModel.resetZoom() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Zoom (${String.format(Locale.ROOT, "%.1f×", zoomScale)})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Skip Intro Button (+85s)
        val showSkipIntro = !isLocalVideoMode && (currentPositionMs in 1000L..120_000L) && showControls && !isTouchLocked
        if (showSkipIntro) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.8f),
                border = BorderStroke(1.2.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 80.dp)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onUserInteraction()
                        playerViewModel.seekBy(85)
                    }
                    .testTag("skip_intro_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Skip Intro (+85s)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Near-End "Up Next: Episode X" Prompt Banner
        val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(0L)
        val showUpNextBanner = !isLocalVideoMode &&
                durationMs > 60_000L &&
                remainingMs in 1_000L..45_000L &&
                currentEpisode < totalEpisodes &&
                !dismissedUpNextForEp &&
                !isTouchLocked

        UpNextEpisodeBanner(
            visible = showUpNextBanner,
            nextEpisodeNumber = currentEpisode + 1,
            remainingSeconds = remainingMs / 1000L,
            onPlayNextNow = {
                playerViewModel.nextEpisode(totalEpisodes)
            },
            onDismiss = {
                dismissedUpNextForEp = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 82.dp)
        )

        // Touch Lock Indicator / Unlock Button
        if (isTouchLocked) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        playerViewModel.toggleTouchLock()
                    }
                    .testTag("unlock_touch_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Screen Locked • Tap to Unlock", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        val noMatchState = uiState as? PlayerUiState.NoMatchFound
        val errorState = uiState as? PlayerUiState.Error
        val effectiveError = playbackError ?: errorState?.message

        // Buffering Indicator (no text during loading)
        if ((isBuffering || uiState is PlayerUiState.Loading) && effectiveError == null && noMatchState == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                CircularProgressIndicator(color = Color(0xFF38BDF8), strokeWidth = 3.dp)
            }
        }

        // No Match Found Overlay
        if (noMatchState != null && effectiveError == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1322).copy(alpha = 0.95f))
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Can't Find Episode ${noMatchState.requestedEpisode ?: currentEpisode}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = noMatchState.reason.ifBlank { "Can't find a full-length video for this episode on the Internet Archive." },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            anime?.let {
                                playerViewModel.loadEpisode(animeId = it.id, episodeNumber = currentEpisode, seasonNumber = playerViewModel.currentSeasonNumber.value, fallbackAnime = it)
                            } ?: playerViewModel.retryPlayback()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Close")
                    }
                }
            }
        }

        // Playback Error Overlay
        if (effectiveError != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1322).copy(alpha = 0.95f))
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Streaming Issue Encountered",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = effectiveError,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { playerViewModel.retryPlayback() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }

                    OutlinedButton(
                        onClick = { playerViewModel.tryFallbackFile() },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Try Alternate File", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Try Alt File")
                    }
                }
            }
        }

        // Interactive Player Controls Overlay
        AnimatedVisibility(
            visible = showControls && playbackError == null && !isTouchLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
            ) {
                // Top Overlay Bar (Back, Title, Speed, Quality, Subtitles, Lock, PiP)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLocalVideoMode) (localVideoTitle ?: "Local Device Video") else "${anime?.displayTitle ?: "Anime"} • Ep $currentEpisode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isLocalVideoMode) "Local Device Storage" else "Episode $currentEpisode",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    // Top Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Speed Indicator Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable {
                                    onUserInteraction()
                                    onOpenSpeed()
                                }
                                .testTag("top_speed_button")
                        ) {
                            Text(
                                text = String.format(Locale.ROOT, "%.1f×", playbackSpeed),
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Subtitle & Audio
                        IconButton(
                            onClick = {
                                onUserInteraction()
                                onOpenTracks()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Subtitles, contentDescription = "Tracks", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Aspect Ratio / Zoom
                        IconButton(
                            onClick = {
                                onUserInteraction()
                                onOpenAspectRatio()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // PiP Button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    onUserInteraction()
                                    onEnterPip()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Lock Screen Button
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                playerViewModel.toggleTouchLock()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Lock", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Center Main Playback Controls (-10s | Play/Pause | +10s)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    // Rewind
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onUserInteraction()
                            playerViewModel.seekBy(-seekIncrement)
                            onShowDoubleTapLeft()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .testTag("rewind_button")
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "-${seekIncrement}s", tint = Color.White, modifier = Modifier.size(26.dp))
                    }

                    // Main Play / Pause
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onUserInteraction()
                            playerViewModel.togglePlayPause()
                        },
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7))
                            .border(2.dp, Color.White, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Forward
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onUserInteraction()
                            playerViewModel.seekBy(seekIncrement)
                            onShowDoubleTapRight()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .testTag("fast_forward_button")
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "+${seekIncrement}s", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }

                // Bottom Overlay Bar (Buffered Progress, Timestamps, Mute, Rotate, Fullscreen)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.70f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .navigationBarsPadding() // CRITICAL: ensures no clipping on Android navigation bars
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Custom Buffered SeekBar
                    BufferedSeekBar(
                        currentPositionMs = currentPositionMs,
                        bufferedPositionMs = bufferedPositionMs,
                        durationMs = durationMs,
                        onSeek = { targetMs ->
                            onUserInteraction()
                            playerViewModel.seekTo(targetMs)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val rightTimeLabel = if (showRemainingTime && durationMs > currentPositionMs) {
                            "-${formatMinSec(durationMs - currentPositionMs)}"
                        } else {
                            formatMinSec(durationMs)
                        }
                        Text(
                            text = "${formatMinSec(currentPositionMs)} / $rightTimeLabel",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.clickable {
                                showRemainingTime = !showRemainingTime
                            }
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Mute Toggle
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    playerViewModel.exoPlayer.volume = if (isMuted) 0f else 1f
                                    onUserInteraction()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Orientation Toggle
                            IconButton(
                                onClick = {
                                    onUserInteraction()
                                    onOrientationToggle()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            // Fullscreen Toggle
                            IconButton(
                                onClick = {
                                    onUserInteraction()
                                    onFullscreenToggle()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun enterPipMode(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val activity = context.findActivity() ?: return
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            // PiP failed
        }
    }
}

private fun formatMinSec(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Conflict-free MX Player touch gesture detector supporting:
 * - Single-finger left-side vertical swipe -> Screen Brightness adjustment
 * - Single-finger right-side vertical swipe -> Media Volume adjustment
 * - Single-finger horizontal swipe -> Smooth timestamp scrubbing preview + seek on release
 * - Two-finger pinch & pan -> Video zoom & pan
 * Automatically ignores touches starting inside top/bottom system bar / seekbar exclusion zones
 * or when [isTouchLocked] is true.
 */
private fun Modifier.mxPlayerSwipeAndPinchGestures(
    isTouchLocked: Boolean,
    onUserInteraction: () -> Unit,
    onPinchZoomAndPan: (zoomChange: Float, panX: Float, panY: Float) -> Unit,
    onBrightnessDelta: (deltaFraction: Float) -> Unit,
    onVolumeDelta: (deltaFraction: Float) -> Unit,
    getCurrentPositionMs: () -> Long,
    getDurationMs: () -> Long,
    onHorizontalSeekPreview: (previewPosMs: Long, deltaMs: Long) -> Unit,
    onHorizontalSeekCommit: (targetPosMs: Long) -> Unit,
    onGestureEnded: () -> Unit
): Modifier = this.pointerInput(isTouchLocked) {
    if (isTouchLocked) return@pointerInput
    val topExclusionPx = 48.dp.toPx()
    val bottomExclusionPx = 72.dp.toPx()
    val edgeExclusionPx = 20.dp.toPx()
    val slop = viewConfiguration.touchSlop * 1.2f

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val widthPx = size.width.toFloat().coerceAtLeast(1f)
        val heightPx = size.height.toFloat().coerceAtLeast(1f)

        // Exclude top action bar, bottom seekbar, and system edge back-gestures
        if (down.position.y < topExclusionPx ||
            down.position.y > (heightPx - bottomExclusionPx) ||
            down.position.x < edgeExclusionPx ||
            down.position.x > (widthPx - edgeExclusionPx)
        ) {
            return@awaitEachGesture
        }

        var gestureMode = 0 // 0=none, 2=pinch, 11=brightness, 12=volume, 13=horizontal_seek
        var totalDx = 0f
        var totalDy = 0f
        var seekStartPosMs = 0L
        var seekTargetPosMs = 0L

        do {
            val event = awaitPointerEvent()
            val pressedChanges = event.changes.filter { it.pressed }

            if (pressedChanges.size >= 2) {
                gestureMode = 2
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (zoomChange != 1f || panChange.x != 0f || panChange.y != 0f) {
                    onUserInteraction()
                    onPinchZoomAndPan(zoomChange, panChange.x, panChange.y)
                    event.changes.forEach { it.consume() }
                }
            } else if (pressedChanges.size == 1 && gestureMode != 2) {
                val change = pressedChanges.first()
                val dx = change.position.x - change.previousPosition.x
                val dy = change.position.y - change.previousPosition.y
                totalDx += dx
                totalDy += dy

                if (gestureMode == 0) {
                    val dist = kotlin.math.hypot(totalDx, totalDy)
                    if (dist > slop) {
                        onUserInteraction()
                        if (kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx) * 1.15f) {
                            gestureMode = if (down.position.x < widthPx * 0.5f) 11 else 12
                        } else if (kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy) * 1.15f) {
                            gestureMode = 13
                            seekStartPosMs = getCurrentPositionMs()
                            seekTargetPosMs = seekStartPosMs
                        }
                    }
                }

                when (gestureMode) {
                    11 -> {
                        val deltaFrac = -dy / (heightPx * 0.65f)
                        onBrightnessDelta(deltaFrac)
                        change.consume()
                    }
                    12 -> {
                        val deltaFrac = -dy / (heightPx * 0.65f)
                        onVolumeDelta(deltaFrac)
                        change.consume()
                    }
                    13 -> {
                        val dur = getDurationMs()
                        val fullSpanMs = if (dur > 0L) minOf(dur, 180_000L).coerceAtLeast(30_000L) else 90_000L
                        val deltaMs = ((totalDx / widthPx) * fullSpanMs).toLong()
                        val maxDur = if (dur > 0L) dur else Long.MAX_VALUE
                        seekTargetPosMs = (seekStartPosMs + deltaMs).coerceIn(0L, maxDur)
                        onHorizontalSeekPreview(seekTargetPosMs, seekTargetPosMs - seekStartPosMs)
                        change.consume()
                    }
                }
            }
        } while (event.changes.any { it.pressed })

        if (gestureMode == 13) {
            onHorizontalSeekCommit(seekTargetPosMs)
        } else if (gestureMode != 0) {
            onGestureEnded()
        }
    }
}

/**
 * Unified Full-Featured Pro ExoPlayer View used by both Anime and Movie/Series players.
 * Supports:
 * - Fast buffering & cross-protocol redirect support for Internet Archive streams
 * - Automatic fallback across candidate files & bounded buffering timeout ("Can't find" error state)
 * - Play/Pause, Rewind (-10s), Fast-Forward (+10s), Previous/Next Episode navigation
 * - Double-tap left (-10s) / right (+10s) seek with ripple indicator
 * - Vertical swipe on left (Brightness) and right (Volume) with live HUD
 * - Interactive BufferedSeekBar with scrubbing preview & Remaining Time toggle
 * - Playback Speed selector (0.5x - 2.0x), Aspect Ratio / Resize mode (Fit, Fill, Zoom)
 * - Audio & Subtitle (CC) track selector, Stream Quality/Source file switcher
 * - Skip Intro (+85s), Screen Lock mode, Picture-in-Picture (PiP), and Fullscreen/Landscape toggle
 */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExoPlayerVideoView(
    mediaUrl: String,
    modifier: Modifier = Modifier,
    title: String = "",
    subtitleBadge: String? = null,
    fallbackUrls: List<String> = emptyList(),
    streamLabels: List<Pair<String, String>> = emptyList(),
    initialPositionMs: Long = 0L,
    onPositionUpdate: ((Long) -> Unit)? = null,
    onPlaybackCompleted: (() -> Unit)? = null,
    onFullscreenToggle: (() -> Unit)? = null,
    isFullscreenMode: Boolean = false,
    onPreviousEpisode: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    onOpenEpisodeSelector: (() -> Unit)? = null,
    onTryFallback: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    val allCandidateUrls = remember(mediaUrl, fallbackUrls) {
        (listOf(mediaUrl) + fallbackUrls).map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
    var currentUrlIndex by remember(mediaUrl) { mutableIntStateOf(0) }
    val activeStreamUrl = allCandidateUrls.getOrElse(currentUrlIndex) { mediaUrl }

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showRemainingTime by remember { mutableStateOf(false) }

    var showControls by remember { mutableStateOf(true) }
    var isTouchLocked by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    var showSpeedSheet by remember { mutableStateOf(false) }
    var showAspectRatioSheet by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }

    var doubleTapSeekDir by remember { mutableIntStateOf(0) }
    var doubleTapSeekAmount by remember { mutableIntStateOf(0) }
    var gestureHudText by remember { mutableStateOf<String?>(null) }
    var activeGestureType by remember { mutableStateOf(MxGestureType.NONE) }
    var gestureSeekTargetMs by remember { mutableLongStateOf(0L) }
    var gestureSeekDeltaMs by remember { mutableLongStateOf(0L) }
    var playerVolume by remember { mutableFloatStateOf(1.0f) }
    var screenBrightness by remember { mutableFloatStateOf(0.75f) }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val lifecycleOwner = LocalLifecycleOwner.current

    var availableAudioTracks by remember { mutableStateOf<List<Pair<String, TrackSelectionOverride?>>>(emptyList()) }
    var selectedAudioLabel by remember { mutableStateOf("Default Audio") }
    var availableSubtitleTracks by remember { mutableStateOf<List<Pair<String, TrackSelectionOverride?>>>(emptyList()) }
    var isSubtitlesOff by remember { mutableStateOf(false) }
    var videoResolutionBadge by remember { mutableStateOf("HD") }

    var userInteractionToken by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(view) {
        val prevKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = prevKeepScreenOn
        }
    }

    val trackSelector = remember { DefaultTrackSelector(context) }

    val exoPlayer = remember(context, trackSelector) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AniTrackPlayer/1.0 (Linux; Android; Media3 ExoPlayer)")
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(12_000)
            .setAllowCrossProtocolRedirects(true)

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(defaultDataSourceFactory)

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

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = true
            }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val activity = context.findActivity()
                val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    activity?.isInPictureInPictureMode == true
                } else false
                if (!inPip) {
                    exoPlayer.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(activeStreamUrl, retryTrigger) {
        if (activeStreamUrl.isBlank()) {
            isBuffering = false
            playbackError = "Can't find video on Internet Archive."
            return@LaunchedEffect
        }
        playbackError = null
        isBuffering = true
        val mimeType = PlayerViewModel.resolveMediaMimeType(activeStreamUrl)
        val mediaItemBuilder = MediaItem.Builder().setUri(activeStreamUrl)
        if (mimeType != null) {
            mediaItemBuilder.setMimeType(mimeType)
        }
        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        if (initialPositionMs > 5000L && currentPosition == 0L) {
            exoPlayer.seekTo(initialPositionMs)
        }
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Watchdog so buffering never hangs forever if an archive server node is unresponsive
    LaunchedEffect(activeStreamUrl, retryTrigger, isBuffering) {
        val isLocalUri = activeStreamUrl.startsWith("content://") || activeStreamUrl.startsWith("file://")
        if (!isLocalUri && isBuffering && playbackError == null) {
            delay(11_000L)
            if (isBuffering && exoPlayer.playbackState != Player.STATE_READY) {
                if (currentUrlIndex + 1 < allCandidateUrls.size) {
                    currentUrlIndex += 1
                } else {
                    isBuffering = false
                    playbackError = "Can't find a responsive video stream on Internet Archive."
                }
            }
        }
    }

    LaunchedEffect(doubleTapSeekDir, doubleTapSeekAmount) {
        if (doubleTapSeekDir != 0) {
            delay(650)
            doubleTapSeekDir = 0
            doubleTapSeekAmount = 0
        }
    }

    LaunchedEffect(gestureHudText) {
        if (gestureHudText != null) {
            delay(900)
            gestureHudText = null
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        playbackError = null
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        playbackError = null
                        if (exoPlayer.duration > 0) {
                            duration = exoPlayer.duration
                        }
                        val vHeight = exoPlayer.videoSize.height
                        if (vHeight > 0) {
                            videoResolutionBadge = when {
                                vHeight >= 1080 -> "1080p FHD"
                                vHeight >= 720 -> "720p HD"
                                vHeight >= 480 -> "480p SD"
                                else -> "${vHeight}p"
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        onPlaybackCompleted?.invoke()
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                        isPlaying = false
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioList = mutableListOf<Pair<String, TrackSelectionOverride?>>()
                val subList = mutableListOf<Pair<String, TrackSelectionOverride?>>()
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until group.length) {
                            val fmt = group.getTrackFormat(i)
                            val lang = fmt.language?.let { Locale(it).displayLanguage }?.takeIf { it.isNotBlank() } ?: "Audio Track ${i + 1}"
                            val label = listOfNotNull(lang, fmt.label).distinct().joinToString(" • ")
                            val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                            audioList.add(label to override)
                            if (group.isTrackSelected(i)) {
                                selectedAudioLabel = label
                            }
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until group.length) {
                            val fmt = group.getTrackFormat(i)
                            val lang = fmt.language?.let { Locale(it).displayLanguage }?.takeIf { it.isNotBlank() } ?: "Subtitles ${i + 1}"
                            val label = listOfNotNull(lang, fmt.label).distinct().joinToString(" • ")
                            val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                            subList.add(label to override)
                        }
                    }
                }
                availableAudioTracks = audioList
                availableSubtitleTracks = subList
            }

            override fun onPlayerError(error: PlaybackException) {
                if (currentUrlIndex + 1 < allCandidateUrls.size) {
                    currentUrlIndex += 1
                } else {
                    isBuffering = false
                    isPlaying = false
                    val isLocal = activeStreamUrl.startsWith("content://") || activeStreamUrl.startsWith("file://")
                    playbackError = PlayerViewModel.mapPlaybackErrorToUserMessage(error, isLocal)
                    onTryFallback?.invoke()
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                currentPosition = pos
                bufferedPosition = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                onPositionUpdate?.invoke(pos)
                if (exoPlayer.duration > 0) {
                    duration = exoPlayer.duration
                }
            }
            delay(250)
        }
    }

    LaunchedEffect(showControls, isPlaying, userInteractionToken, isTouchLocked) {
        if (showControls && isPlaying && !isTouchLocked) {
            delay(3800)
            showControls = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(isTouchLocked) {
                detectTapGestures(
                    onTap = {
                        userInteractionToken = System.currentTimeMillis()
                        showControls = !showControls
                    },
                    onDoubleTap = { offset ->
                        if (isTouchLocked) return@detectTapGestures
                        userInteractionToken = System.currentTimeMillis()
                        val widthPx = size.width.toFloat().coerceAtLeast(1f)
                        when {
                            offset.x < widthPx * 0.35f -> {
                                val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                                doubleTapSeekDir = -1
                                doubleTapSeekAmount += 10
                            }
                            offset.x > widthPx * 0.65f -> {
                                val maxDur = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                                val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(maxDur)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                                doubleTapSeekDir = 1
                                doubleTapSeekAmount += 10
                            }
                            else -> {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        }
                    }
                )
            }
            .mxPlayerSwipeAndPinchGestures(
                isTouchLocked = isTouchLocked,
                onUserInteraction = {
                    userInteractionToken = System.currentTimeMillis()
                },
                onPinchZoomAndPan = { zoomChange, panX, panY ->
                    if (zoomChange != 1f) {
                        zoomScale = (zoomScale * zoomChange).coerceIn(1.0f, 3.0f)
                        if (zoomScale == 1.0f) {
                            panOffsetX = 0f
                            panOffsetY = 0f
                        }
                    } else if (zoomScale > 1.02f) {
                        panOffsetX = (panOffsetX + panX).coerceIn(-500f, 500f)
                        panOffsetY = (panOffsetY + panY).coerceIn(-300f, 300f)
                    }
                },
                onBrightnessDelta = { delta ->
                    activeGestureType = MxGestureType.BRIGHTNESS
                    val nextB = (screenBrightness + delta).coerceIn(0.05f, 1.0f)
                    screenBrightness = nextB
                    val activity = context.findActivity()
                    activity?.window?.let { win ->
                        val lp = win.attributes
                        lp.screenBrightness = nextB
                        win.attributes = lp
                    }
                },
                onVolumeDelta = { delta ->
                    activeGestureType = MxGestureType.VOLUME
                    val nextV = (playerVolume + delta).coerceIn(0.0f, 1.0f)
                    playerVolume = nextV
                    isMuted = nextV <= 0.01f
                    exoPlayer.volume = nextV
                    audioManager?.let { am ->
                        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        val targetIndex = (nextV * maxVol + 0.5f).toInt().coerceIn(0, maxVol)
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
                    }
                },
                getCurrentPositionMs = { currentPosition },
                getDurationMs = { duration },
                onHorizontalSeekPreview = { targetMs, deltaMs ->
                    activeGestureType = MxGestureType.HORIZONTAL_SEEK
                    gestureSeekTargetMs = targetMs
                    gestureSeekDeltaMs = deltaMs
                },
                onHorizontalSeekCommit = { targetMs ->
                    exoPlayer.seekTo(targetMs)
                    currentPosition = targetMs
                    activeGestureType = MxGestureType.NONE
                },
                onGestureEnded = {
                    activeGestureType = MxGestureType.NONE
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    this.resizeMode = resizeMode
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoomScale,
                    scaleY = zoomScale,
                    translationX = panOffsetX,
                    translationY = panOffsetY
                )
        )

        // Double-Tap Seek Ripple Feedback
        if (doubleTapSeekDir != 0) {
            Box(
                modifier = Modifier
                    .align(if (doubleTapSeekDir < 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 36.dp)
            ) {
                DoubleTapSeekBubble(
                    isForward = doubleTapSeekDir > 0,
                    seconds = doubleTapSeekAmount,
                    visible = true
                )
            }
        }

        // MX Player Brightness / Volume / Horizontal Seek Scrub HUD
        MxPlayerGestureHud(
            gestureType = activeGestureType,
            fractionValue = when (activeGestureType) {
                MxGestureType.BRIGHTNESS -> screenBrightness
                MxGestureType.VOLUME -> if (isMuted) 0f else playerVolume
                else -> 0f
            },
            seekPreviewPositionMs = gestureSeekTargetMs,
            seekDeltaMs = gestureSeekDeltaMs,
            durationMs = duration,
            modifier = Modifier.align(Alignment.Center)
        )

        // Volume / Brightness HUD
        if (gestureHudText != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
            ) {
                Text(
                    text = gestureHudText!!,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Screen Touch Lock Pill when locked
        if (isTouchLocked && showControls) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .clickable {
                        isTouchLocked = false
                        userInteractionToken = System.currentTimeMillis()
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Controls Locked • Tap to Unlock",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Full Player Controls Overlay
        AnimatedVisibility(
            visible = showControls && playbackError == null && !isTouchLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Control Bar: Title, Resolution Badge, Audio/Subtitles, Quality, Speed, Lock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = videoResolutionBadge,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (!subtitleBadge.isNullOrBlank()) {
                                Text(
                                    text = subtitleBadge,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Audio & Subtitles Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.clickable {
                                userInteractionToken = System.currentTimeMillis()
                                showTracksSheet = true
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Audio & Subtitles", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Audio/CC", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Quality / Source Selector (if multiple streams exist)
                        if (allCandidateUrls.size > 1 || streamLabels.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.clickable {
                                    userInteractionToken = System.currentTimeMillis()
                                    showQualitySheet = true
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.HighQuality, contentDescription = "Quality", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Source ${currentUrlIndex + 1}/${allCandidateUrls.size}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Speed Selector Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.clickable {
                                userInteractionToken = System.currentTimeMillis()
                                showSpeedSheet = true
                            }
                        ) {
                            Text(
                                text = String.format(Locale.ROOT, "%.1f×", playbackSpeed),
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        // Lock Controls Button
                        IconButton(
                            onClick = {
                                isTouchLocked = true
                                showControls = true
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Lock Controls", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Center Transport Controls: Prev Ep, Rewind -10s, Play/Pause, FastForward +10s, Next Ep
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    if (onPreviousEpisode != null) {
                        IconButton(
                            onClick = {
                                userInteractionToken = System.currentTimeMillis()
                                onPreviousEpisode()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Episode", tint = Color.White)
                        }
                    }

                    IconButton(
                        onClick = {
                            userInteractionToken = System.currentTimeMillis()
                            val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            currentPosition = target
                            doubleTapSeekDir = -1
                            doubleTapSeekAmount = 10
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            userInteractionToken = System.currentTimeMillis()
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0)
                                }
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            userInteractionToken = System.currentTimeMillis()
                            val maxDur = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                            val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(maxDur)
                            exoPlayer.seekTo(target)
                            currentPosition = target
                            doubleTapSeekDir = 1
                            doubleTapSeekAmount = 10
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White)
                    }

                    if (onNextEpisode != null) {
                        IconButton(
                            onClick = {
                                userInteractionToken = System.currentTimeMillis()
                                onNextEpisode()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Episode", tint = Color.White)
                        }
                    }
                }

                // Bottom Overlay Bar: Quick Pills + SeekBar + Time & Utility Icons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Quick Action Row: Skip Intro (+85s), Aspect Ratio, Episode Selector, Reset Zoom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // +85s Skip Intro Pill
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.clickable {
                                    userInteractionToken = System.currentTimeMillis()
                                    val maxDur = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                                    val target = (exoPlayer.currentPosition + 85_000L).coerceAtMost(maxDur)
                                    exoPlayer.seekTo(target)
                                    currentPosition = target
                                }
                            ) {
                                Text(
                                    text = "+85s Skip",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // Aspect Ratio / Resize Mode Pill
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.clickable {
                                    userInteractionToken = System.currentTimeMillis()
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                    val modeName = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop / Zoom"
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch Fill"
                                        else -> "Fit Screen"
                                    }
                                    gestureHudText = "Aspect: $modeName"
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
                                            else -> "Fit"
                                        },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (zoomScale > 1.02f) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.18f),
                                    modifier = Modifier.clickable {
                                        zoomScale = 1.0f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                                ) {
                                    Text(
                                        text = "Reset ${String.format(Locale.ROOT, "%.1f×", zoomScale)}",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (onOpenEpisodeSelector != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                modifier = Modifier.clickable {
                                    userInteractionToken = System.currentTimeMillis()
                                    onOpenEpisodeSelector()
                                }
                            ) {
                                Text(
                                    text = "Episodes",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    BufferedSeekBar(
                        currentPositionMs = currentPosition,
                        bufferedPositionMs = bufferedPosition,
                        durationMs = duration,
                        onSeek = { targetMs ->
                            userInteractionToken = System.currentTimeMillis()
                            exoPlayer.seekTo(targetMs)
                            currentPosition = targetMs
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val rightTimeText = if (showRemainingTime && duration > currentPosition) {
                            "-${formatMinSec(duration - currentPosition)}"
                        } else {
                            formatMinSec(duration)
                        }
                        Text(
                            text = "${formatMinSec(currentPosition)} / $rightTimeText",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.clickable {
                                showRemainingTime = !showRemainingTime
                            }
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Loop / Repeat Button
                            IconButton(
                                onClick = {
                                    isLooping = !isLooping
                                    exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                    gestureHudText = if (isLooping) "Loop: On" else "Loop: Off"
                                    userInteractionToken = System.currentTimeMillis()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Loop",
                                    tint = if (isLooping) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Volume / Mute Button
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    exoPlayer.volume = if (isMuted) 0f else playerVolume
                                    userInteractionToken = System.currentTimeMillis()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // PiP Button
                            IconButton(
                                onClick = {
                                    enterPipMode(context)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Rotate / Fullscreen Toggle
                            IconButton(
                                onClick = {
                                    userInteractionToken = System.currentTimeMillis()
                                    if (onFullscreenToggle != null) {
                                        onFullscreenToggle()
                                    } else {
                                        context.findActivity()?.let { act ->
                                            act.requestedOrientation = if (act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                            } else {
                                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fast Buffering Indicator
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = Color(0xFF38BDF8),
                strokeWidth = 3.dp,
                modifier = Modifier.size(42.dp)
            )
        }

        // Clear "Can't Find" / Playback Error State (Never stays black or spins forever)
        if (playbackError != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090E1A).copy(alpha = 0.96f))
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Can't Find Playable Video",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = playbackError ?: "Can't find this video on the Internet Archive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            playbackError = null
                            currentUrlIndex = 0
                            retryTrigger += 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }
                    if (allCandidateUrls.size > 1) {
                        OutlinedButton(
                            onClick = {
                                playbackError = null
                                currentUrlIndex = (currentUrlIndex + 1) % allCandidateUrls.size
                                retryTrigger += 1
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Next Source", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Alternate Stream")
                        }
                    }
                }
            }
        }

        // Speed Selection Sheet
        if (showSpeedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSpeedSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF0F172A)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    PlaybackSpeedRuler(
                        currentSpeed = playbackSpeed,
                        onSpeedSelected = { speed ->
                            playbackSpeed = speed
                            exoPlayer.setPlaybackSpeed(speed)
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Audio & Subtitle Track Sheet
        if (showTracksSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTracksSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Audio & Subtitles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "AUDIO TRACKS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (availableAudioTracks.isEmpty()) {
                        Text("Original Audio Track (Active)", color = Color.White, fontSize = 13.sp)
                    } else {
                        availableAudioTracks.forEach { (label, override) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (override != null) {
                                            trackSelector.parameters = trackSelector.parameters
                                                .buildUpon()
                                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                .addOverride(override)
                                                .build()
                                            selectedAudioLabel = label
                                        }
                                        showTracksSheet = false
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = Color.White, fontSize = 13.sp)
                                if (label == selectedAudioLabel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "SUBTITLES / CAPTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isSubtitlesOff = true
                                trackSelector.parameters = trackSelector.parameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                    .build()
                                showTracksSheet = false
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Off", color = Color.White, fontSize = 13.sp)
                        if (isSubtitlesOff || availableSubtitleTracks.isEmpty()) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        }
                    }
                    availableSubtitleTracks.forEach { (label, override) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isSubtitlesOff = false
                                    if (override != null) {
                                        trackSelector.parameters = trackSelector.parameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                            .addOverride(override)
                                            .build()
                                    }
                                    showTracksSheet = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = Color.White, fontSize = 13.sp)
                            if (!isSubtitlesOff) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Stream Source / Quality Selection Sheet
        if (showQualitySheet) {
            ModalBottomSheet(
                onDismissRequest = { showQualitySheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Video Stream Source",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    allCandidateUrls.forEachIndexed { idx, url ->
                        val label = streamLabels.getOrNull(idx)?.first
                            ?: url.substringAfterLast('/').let { android.net.Uri.decode(it) }.ifBlank { "Stream Source ${idx + 1}" }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentUrlIndex = idx
                                    showQualitySheet = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (idx == currentUrlIndex) Color(0xFF38BDF8) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (idx == currentUrlIndex) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (idx == currentUrlIndex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

/**
 * Top-level InAppDirectWebViewPlayer kept for official YouTube trailers if opened via dialog
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppDirectWebViewPlayer(
    embedUrl: String,
    modifier: Modifier = Modifier,
    onShowCustomView: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null,
    onHideCustomView: (() -> Unit)? = null
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (view != null && callback != null) {
                            onShowCustomView?.invoke(view, callback)
                        }
                    }

                    override fun onHideCustomView() {
                        onHideCustomView?.invoke()
                    }
                }
                loadUrl(embedUrl)
            }
        },
        update = { webView ->
            if (webView.url != embedUrl) {
                webView.loadUrl(embedUrl)
            }
        },
        modifier = modifier
    )
}

