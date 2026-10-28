package com.example.ui.components

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.repository.SubtitleStyleConfig
import com.example.data.repository.SubtitleTrackItem
import com.example.data.repository.VideoQualityOption
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Animated double-tap seek indicator on left / right side of player.
 */
@Composable
fun DoubleTapSeekBubble(
    isForward: Boolean,
    seconds: Int,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = if (isForward) "+${seconds}s" else "-${seconds}s",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Buffered seek bar showing played progress, buffered progress, and scrubber thumb.
 */
@Composable
fun BufferedSeekBar(
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationSafe = durationMs.coerceAtLeast(1L)
    val playedProgress = (currentPositionMs.toFloat() / durationSafe).coerceIn(0f, 1f)
    val bufferedProgress = (bufferedPositionMs.toFloat() / durationSafe).coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val activeProgress = if (isDragging) dragProgress else playedProgress

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.25f))
        )

        // Buffered Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedProgress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.55f))
        )

        // Played Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth(activeProgress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF38BDF8))
        )

        // Invisible touch slider overlay for smooth scrubbing
        Slider(
            value = activeProgress,
            onValueChange = {
                isDragging = true
                dragProgress = it
            },
            onValueChangeFinished = {
                onSeek((dragProgress * durationSafe).toLong())
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("buffered_seek_slider")
        )
    }
}

/**
 * Aspect Ratio / Resize Mode Selection Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AspectRatioSelectionSheet(
    currentMode: Int,
    onModeSelected: (Int) -> Unit,
    zoomScale: Float,
    onResetZoom: () -> Unit,
    onDismiss: () -> Unit
) {
    val modes = listOf(
        Triple(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit to Screen", "Maintains original aspect ratio inside window"),
        Triple(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Crop to Fill", "Fills entire screen without black borders (crops top/bottom or sides)"),
        Triple(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH, "Fit Width", "Scales video to match display width"),
        Triple(AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT, "Fit Height", "Scales video to match display height"),
        Triple(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Stretch to Fill", "Stretches to fill full screen dimensions")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Display & Aspect Ratio",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (zoomScale > 1.05f) {
                    OutlinedButton(
                        onClick = onResetZoom,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Zoom (1.0×)", fontSize = 12.sp, color = Color(0xFF38BDF8))
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            modes.forEach { (mode, label, description) ->
                val isSelected = (currentMode == mode)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onModeSelected(mode)
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF38BDF8))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Quality Selection Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    options: List<VideoQualityOption>,
    activeFile: String?,
    onQualitySelected: (VideoQualityOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Stream Quality",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Switching streams preserves your exact playback position.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (options.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Current stream is playing at highest available resolution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options) { opt ->
                        val isSelected = (opt.file.name == activeFile)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onQualitySelected(opt)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = opt.label,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        val sizeStr = opt.sizeBytes?.let { " • ${it / (1024 * 1024)} MB" }.orEmpty()
                                        Text(
                                            text = "${opt.file.format ?: "MP4"}$sizeStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class PlayerAudioTrack(
    val id: String,
    val languageCode: String?,
    val languageName: String,
    val isSelected: Boolean,
    val groupIndex: Int,
    val trackIndex: Int
)

data class PlayerSubtitleTrack(
    val id: String,
    val label: String,
    val languageCode: String?,
    val isSelected: Boolean,
    val isEmbedded: Boolean,
    val groupIndex: Int? = null,
    val trackIndex: Int? = null,
    val subtitleItem: SubtitleTrackItem? = null
)

/**
 * Audio and Subtitle Tracks Sheet with Subtitle Styling and Device Loading
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSubtitleTracksSheet(
    audioTracks: List<PlayerAudioTrack>,
    subtitleTracks: List<PlayerSubtitleTrack>,
    isSubtitleOff: Boolean,
    subtitleStyle: SubtitleStyleConfig,
    onAudioTrackSelected: (PlayerAudioTrack) -> Unit,
    onSubtitleTrackSelected: (PlayerSubtitleTrack) -> Unit,
    onSubtitleOffSelected: () -> Unit,
    onSubtitleStyleChanged: (SubtitleStyleConfig) -> Unit,
    onLoadLocalSubtitle: (Uri) -> Unit,
    onLoadSubtitleUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    isAudioOnlyMode: Boolean = false,
    onToggleAudioOnlyMode: ((Boolean) -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Subtitles, 1: Audio, 2: Subtitle Settings
    var showUrlInput by remember { mutableStateOf(false) }
    var inputUrl by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onLoadLocalSubtitle(uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF38BDF8),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF38BDF8)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Subtitles (${subtitleTracks.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Audio (${audioTracks.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Styling & Sync", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Subtitles List
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(320.dp)
                    ) {
                        // "Off" option
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSubtitleOff) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSubtitleOff) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSubtitleOffSelected() }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SubtitlesOff, contentDescription = null, tint = Color(0xFF94A3B8))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Off (No Subtitles)", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (isSubtitleOff) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF38BDF8))
                                    }
                                }
                            }
                        }

                        items(subtitleTracks) { track ->
                            val isSelected = !isSubtitleOff && track.isSelected
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSubtitleTrackSelected(track) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(track.label, color = Color.White, fontWeight = FontWeight.Bold)
                                            val sourceLabel = if (track.isEmbedded) {
                                                "Embedded Track"
                                            } else {
                                                track.subtitleItem?.title ?: "Subtitle Track"
                                            }
                                            Text(sourceLabel, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF38BDF8))
                                    }
                                }
                            }
                        }

                        // Load Subtitle Buttons (Local File & URL)
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        filePickerLauncher.launch(arrayOf("*/*"))
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Local .srt/.vtt", fontSize = 11.sp, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = { showUrlInput = !showUrlInput },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load from URL", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            if (showUrlInput) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = inputUrl,
                                        onValueChange = { inputUrl = it },
                                        placeholder = { Text("https://example.com/subs.vtt", fontSize = 12.sp, color = Color(0xFF64748B)) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (inputUrl.isNotBlank()) {
                                                onLoadSubtitleUrl(inputUrl.trim())
                                                showUrlInput = false
                                                inputUrl = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Audio Tracks List & Audio-Only Mode Toggle
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    ) {
                        if (onToggleAudioOnlyMode != null) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAudioOnlyMode) Color(0xFF0284C7).copy(alpha = 0.28f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isAudioOnlyMode) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onToggleAudioOnlyMode(!isAudioOnlyMode) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Headphones,
                                                contentDescription = null,
                                                tint = if (isAudioOnlyMode) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Audio-Only Mode",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "Listen with video hidden & keep playing in background",
                                                    color = Color(0xFF94A3B8),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = isAudioOnlyMode,
                                            onCheckedChange = { onToggleAudioOnlyMode(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF0284C7)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (audioTracks.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Default audio stream active. No multiple audio tracks detected in this media file.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(audioTracks) { track ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (track.isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (track.isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onAudioTrackSelected(track) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = if (track.isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(track.languageName, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        if (track.isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF38BDF8))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Subtitle Styling, Screen Position & Timing Offset Sync
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        // Timing Offset
                        Text(
                            text = "SUBTITLE TIMING OFFSET (SYNC)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (subtitleStyle.offsetMs > 0) "+" else ""}${subtitleStyle.offsetMs} ms",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = (subtitleStyle.offsetMs - 500).coerceIn(-15000L, 15000L))) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("-500ms", fontSize = 11.sp, color = Color.White) }
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = (subtitleStyle.offsetMs - 100).coerceIn(-15000L, 15000L))) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("-100ms", fontSize = 11.sp, color = Color.White) }
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = 0)) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("0ms", fontSize = 11.sp, color = Color(0xFF38BDF8)) }
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = (subtitleStyle.offsetMs + 100).coerceIn(-15000L, 15000L))) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("+100ms", fontSize = 11.sp, color = Color.White) }
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = (subtitleStyle.offsetMs + 500).coerceIn(-15000L, 15000L))) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("+500ms", fontSize = 11.sp, color = Color.White) }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Font Size Selector
                        Text(
                            text = "FONT SIZE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                14f to "Small",
                                18f to "Normal",
                                22f to "Large",
                                26f to "Extra"
                            ).forEach { (size, label) ->
                                val isSelected = (subtitleStyle.fontSizeSp == size)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSubtitleStyleChanged(subtitleStyle.copy(fontSizeSp = size)) }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color.White
                                        ),
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Screen Position (Bottom Padding)
                        Text(
                            text = "SCREEN POSITION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0.03f to "Bottom",
                                0.08f to "Normal",
                                0.15f to "Raised",
                                0.22f to "High"
                            ).forEach { (paddingFrac, label) ->
                                val isSelected = kotlin.math.abs(subtitleStyle.bottomPaddingFraction - paddingFrac) < 0.015f
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSubtitleStyleChanged(subtitleStyle.copy(bottomPaddingFraction = paddingFrac)) }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color.White
                                        ),
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Background & Outline Style Selector
                        Text(
                            text = "OUTLINE & BACKGROUND",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(0x00000000L, 1, "Outline Only"),
                                Triple(0x88000000L, 1, "Semi-Dark"),
                                Triple(0xDD000000L, 1, "Solid Box"),
                                Triple(0x44000000L, 2, "Drop Shadow")
                            ).forEach { (bgHex, edgeType, label) ->
                                val isSelected = subtitleStyle.backgroundColor == bgHex && subtitleStyle.edgeType == edgeType
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onSubtitleStyleChanged(
                                                subtitleStyle.copy(backgroundColor = bgHex, edgeType = edgeType)
                                            )
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color.White
                                        ),
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Text Color Selector
                        Text(
                            text = "TEXT COLOR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(
                                0xFFFFFFFF to "White",
                                0xFFFACC15 to "Yellow",
                                0xFF38BDF8 to "Cyan",
                                0xFF4ADE80 to "Green"
                            ).forEach { (colorHex, name) ->
                                val isSelected = (subtitleStyle.textColor == colorHex)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color(0xFF38BDF8) else Color.Black, CircleShape)
                                        .clickable { onSubtitleStyleChanged(subtitleStyle.copy(textColor = colorHex)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = name, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

enum class MxGestureType {
    NONE,
    BRIGHTNESS,
    VOLUME,
    HORIZONTAL_SEEK
}

/**
 * MX Player-style visual HUD overlay for left-side Brightness swipe, right-side Volume swipe,
 * and horizontal Seek scrubbing preview.
 */
@Composable
fun MxPlayerGestureHud(
    gestureType: MxGestureType,
    fractionValue: Float, // 0f..1f for brightness/volume
    seekPreviewPositionMs: Long = 0L,
    seekDeltaMs: Long = 0L,
    durationMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = gestureType != MxGestureType.NONE,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier
    ) {
        when (gestureType) {
            MxGestureType.BRIGHTNESS, MxGestureType.VOLUME -> {
                val isBrightness = gestureType == MxGestureType.BRIGHTNESS
                val pct = (fractionValue.coerceIn(0f, 1f) * 100).toInt()
                val accentColor = if (isBrightness) Color(0xFFFACC15) else Color(0xFF38BDF8)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xDD0F172A),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when {
                                isBrightness -> Icons.Default.WbSunny
                                pct == 0 -> Icons.Default.VolumeOff
                                else -> Icons.Default.VolumeUp
                            },
                            contentDescription = if (isBrightness) "Brightness" else "Volume",
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBrightness) "Brightness" else "Volume",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Horizontal progress bar inside the compact pill
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fractionValue.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(accentColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }
            MxGestureType.HORIZONTAL_SEEK -> {
                val deltaSec = seekDeltaMs / 1000L
                val sign = if (deltaSec >= 0) "+" else "-"
                val absSec = kotlin.math.abs(deltaSec)
                val deltaText = String.format(java.util.Locale.US, "%s%02d:%02d", sign, absSec / 60, absSec % 60)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xDD0F172A),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (seekDeltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[ $deltaText ]",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (durationMs > 0L) {
                                "${formatTimeMs(seekPreviewPositionMs)} / ${formatTimeMs(durationMs)}"
                            } else {
                                formatTimeMs(seekPreviewPositionMs)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            MxGestureType.NONE -> {}
        }
    }
}

/**
 * Floating "Up Next: Episode X" prompt shown when an episode is near completion.
 */
@Composable
fun UpNextEpisodeBanner(
    visible: Boolean,
    nextEpisodeNumber: Int,
    remainingSeconds: Long,
    onPlayNextNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xEE0F172A),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text(
                        text = "UP NEXT IN ${remainingSeconds.coerceAtLeast(1L)}s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = "Episode $nextEpisodeNumber",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Button(
                    onClick = onPlayNextNow,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss Up Next",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modal Bottom Sheet for browsing and resuming Recently Played Local Device Videos
 * or opening a new local video file via Android's system file picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentLocalVideosSheet(
    recentVideos: List<com.example.data.repository.PlaybackRepository.LocalVideoHistoryItem>,
    onOpenLocalFilePicker: () -> Unit,
    onSelectRecentVideo: (com.example.data.repository.PlaybackRepository.LocalVideoHistoryItem) -> Unit,
    onRemoveRecentVideo: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Local Device Videos",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Play MP4, MKV, WebM, HLS (.m3u8) & DASH (.mpd) offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Button(
                    onClick = onOpenLocalFilePicker,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse Device", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (recentVideos.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recently played local videos yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Browse Device' above to pick any video file from your phone or SD card.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    text = "RECENTLY PLAYED ON DEVICE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    items(recentVideos, key = { it.uriString }) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectRecentVideo(item) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1
                                        )
                                        val progressLabel = buildString {
                                            if (item.lastPositionMs > 3000L) {
                                                append("Resume at ${formatTimeMs(item.lastPositionMs)}")
                                                if (item.durationMs > 0L) {
                                                    append(" / ${formatTimeMs(item.durationMs)}")
                                                }
                                            } else if (item.durationMs > 0L) {
                                                append("Duration: ${formatTimeMs(item.durationMs)}")
                                            } else {
                                                append("Local Video File")
                                            }
                                        }
                                        Text(
                                            text = progressLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onRemoveRecentVideo(item.uriString) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove from history",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun formatTimeMs(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}


