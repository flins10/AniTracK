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
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
    onDismiss: () -> Unit
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
                    // Audio Tracks List
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(280.dp)
                    ) {
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
                    // Subtitle Styling & Timing Offset Sync
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
                                text = "${subtitleStyle.offsetMs} ms",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = subtitleStyle.offsetMs - 500)) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("-500ms", fontSize = 11.sp, color = Color.White) }
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = subtitleStyle.offsetMs - 100)) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("-100ms", fontSize = 11.sp, color = Color.White) }
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = 0)) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("0ms", fontSize = 11.sp, color = Color(0xFF38BDF8)) }
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = subtitleStyle.offsetMs + 100)) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("+100ms", fontSize = 11.sp, color = Color.White) }
                                OutlinedButton(
                                    onClick = { onSubtitleStyleChanged(subtitleStyle.copy(offsetMs = subtitleStyle.offsetMs + 500)) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) { Text("+500ms", fontSize = 11.sp, color = Color.White) }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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

                        Spacer(modifier = Modifier.height(16.dp))

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
