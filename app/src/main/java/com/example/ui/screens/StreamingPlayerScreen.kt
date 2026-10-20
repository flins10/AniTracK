package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
    var showJumpDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var customSearchQuery by remember { mutableStateOf("") }

    // Double-tap Seek Animation State
    var showDoubleTapLeft by remember { mutableStateOf(false) }
    var showDoubleTapRight by remember { mutableStateOf(false) }

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
                            text = anime?.displayTitle ?: "Streaming Player",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Episode $currentEpisode${if (totalEpisodes > 1) " / $totalEpisodes" else ""}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    // Custom Episode Selector Button
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
                                                text = "Ep $currentEpisode${if (totalEpisodes > 1) " / $totalEpisodes" else ""} (S$currentSeasonNumber) • Custom",
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
                                            Text("Custom Ep...", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
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
                                            text = "AVAILABLE STREAMS (${activeCandidates.size})",
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

        // Speed Ruler Bottom Sheet
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

        // Tracks Bottom Sheet (Audio & Subtitles & Sync)
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
                onDismiss = { showTracksSheet = false }
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

        // Custom Episode Selection Dialog (100-Episode Range Selector)
        if (showJumpDialog) {
            val totalEpPlayer = maxOf(totalEpisodes, currentEpisode, 1)
            val batchSizePlayer = 100
            val totalBatchesPlayer = ((totalEpPlayer + batchSizePlayer - 1) / batchSizePlayer).coerceAtLeast(1)
            var playerBatchIdx by remember { mutableIntStateOf(((currentEpisode - 1) / batchSizePlayer).coerceIn(0, totalBatchesPlayer - 1)) }
            var selectedPlayerEp by remember { mutableIntStateOf(currentEpisode) }

            AlertDialog(
                onDismissRequest = { showJumpDialog = false },
                containerColor = Color(0xFF0F172A),
                titleContentColor = Color.White,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Episode", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        Text("Selected: Ep $selectedPlayerEp", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)))
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Select Episode Range (100 episodes per box):",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )

                        // 100-Episode Range Selector Tabs
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(totalBatchesPlayer) { bIdx ->
                                val sEp = (bIdx * batchSizePlayer) + 1
                                val rEnd = (bIdx + 1) * batchSizePlayer
                                val eEp = if (rEnd < totalEpPlayer) rEnd else totalEpPlayer
                                val isSel = (bIdx == playerBatchIdx)

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.clickable { playerBatchIdx = bIdx }
                                ) {
                                    Text(
                                        text = "$sEp - $eEp",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSel) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // 100-Episode Box Container
                        val startPEp = (playerBatchIdx * batchSizePlayer) + 1
                        val rawPEnd = (playerBatchIdx + 1) * batchSizePlayer
                        val endPEp = if (rawPEnd < totalEpPlayer) rawPEnd else totalEpPlayer
                        val pEpisodesList = (startPEp..endPEp).toList()

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF131C2E),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                items(pEpisodesList) { epNum ->
                                    val isCur = (epNum == selectedPlayerEp)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCur) Color(0xFF0284C7) else Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, if (isCur) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.clickable { selectedPlayerEp = epNum }
                                    ) {
                                        Text(
                                            text = "Ep $epNum",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isCur) Color.White else Color(0xFFCBD5E1),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            playerViewModel.loadEpisode(animeId = animeId, episodeNumber = selectedPlayerEp, seasonNumber = currentSeasonNumber, fallbackAnime = anime ?: fallbackAnime)
                            showJumpDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Watch", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
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

    var isMuted by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(isTouchLocked) {
                if (isTouchLocked) return@pointerInput
                detectTransformGestures { _, pan, zoom, _ ->
                    onUserInteraction()
                    val newZoom = (zoomScale * zoom).coerceIn(1.0f, 3.0f)
                    playerViewModel.setZoomScale(newZoom)
                    if (newZoom > 1.05f) {
                        playerViewModel.setPanOffset(panOffsetX + pan.x, panOffsetY + pan.y)
                    } else {
                        playerViewModel.setPanOffset(0f, 0f)
                    }
                }
            }
            .pointerInput(isTouchLocked) {
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
                            // Center Double Tap: Zoom Toggle
                            if (zoomScale > 1.05f) {
                                playerViewModel.resetZoom()
                            } else {
                                playerViewModel.setZoomScale(1.5f)
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
                        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * (subtitleStyle.fontSizeSp / 18f))
                        setStyle(
                            CaptionStyleCompat(
                                subtitleStyle.textColor.toInt(),
                                subtitleStyle.backgroundColor.toInt(),
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                android.graphics.Color.BLACK,
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
                    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * (subtitleStyle.fontSizeSp / 18f))
                    setStyle(
                        CaptionStyleCompat(
                            subtitleStyle.textColor.toInt(),
                            subtitleStyle.backgroundColor.toInt(),
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            android.graphics.Color.BLACK,
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
        val showSkipIntro = (currentPositionMs in 1000L..120_000L) && showControls && !isTouchLocked
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
                    text = "Episode ${noMatchState.requestedEpisode ?: currentEpisode} Unavailable",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = noMatchState.reason.ifBlank { "No exact full-length episode match was found. Try searching an alternate title below or selecting another episode." },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
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
                                text = "${anime?.displayTitle ?: "Anime"} • Ep $currentEpisode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Episode $currentEpisode",
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
                        Text(
                            text = "${formatMinSec(currentPositionMs)} / ${formatMinSec(durationMs)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
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
 * Standalone ExoPlayerVideoView for direct Internet Archive dialogs with speed ruler and buffered seek bar.
 */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExoPlayerVideoView(
    mediaUrl: String,
    modifier: Modifier = Modifier,
    initialPositionMs: Long = 0L,
    onPositionUpdate: ((Long) -> Unit)? = null,
    onPlaybackCompleted: (() -> Unit)? = null,
    onFullscreenToggle: (() -> Unit)? = null,
    isFullscreenMode: Boolean = false,
    onTryFallback: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var userInteractionToken by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    val trackSelector = remember { DefaultTrackSelector(context) }

    val exoPlayer = remember(context, trackSelector) {
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .build().apply {
                playWhenReady = true
            }
    }

    LaunchedEffect(mediaUrl, retryTrigger) {
        playbackError = null
        isBuffering = true
        val mimeType = when {
            mediaUrl.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            mediaUrl.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            else -> MimeTypes.VIDEO_MP4
        }
        val mediaItem = MediaItem.Builder()
            .setUri(mediaUrl)
            .setMimeType(mimeType)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (initialPositionMs > 5000L) {
            exoPlayer.seekTo(initialPositionMs)
        }
        exoPlayer.setPlaybackSpeed(playbackSpeed)
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

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                isPlaying = false
                playbackError = error.localizedMessage ?: "Playback error encountered (${error.errorCodeName})"
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
            if (exoPlayer.playbackState == Player.STATE_READY) {
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

    LaunchedEffect(showControls, isPlaying, userInteractionToken) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable {
                showControls = !showControls
                userInteractionToken = System.currentTimeMillis()
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showControls && playbackError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Right: Speed Selector Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clickable { showSpeedSheet = true }
                ) {
                    Text(
                        text = String.format(Locale.ROOT, "%.1f×", playbackSpeed),
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Center Play/Pause & Seek
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    IconButton(
                        onClick = {
                            userInteractionToken = System.currentTimeMillis()
                            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            currentPosition = target
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
                            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(maxDur)
                            exoPlayer.seekTo(target)
                            currentPosition = target
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White)
                    }
                }

                // Bottom Overlay Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.85f))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
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
                        Text(
                            text = "${formatMinSec(currentPosition)} / ${formatMinSec(duration)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    exoPlayer.volume = if (isMuted) 0f else 1f
                                    userInteractionToken = System.currentTimeMillis()
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

                            if (onFullscreenToggle != null) {
                                IconButton(
                                    onClick = {
                                        userInteractionToken = System.currentTimeMillis()
                                        onFullscreenToggle()
                                    },
                                    modifier = Modifier.size(36.dp)
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
        }

        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(color = Color(0xFF38BDF8))
        }

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

