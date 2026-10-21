package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.InternetArchiveApiService
import com.example.data.repository.ArchivePlayableMedia
import com.example.ui.screens.ExoPlayerVideoView
import com.example.ui.screens.InAppDirectWebViewPlayer

@Composable
fun EmbeddedYouTubePlayerDialog(
    videoId: String,
    title: String,
    channelTitle: String? = null,
    onDismiss: () -> Unit
) {
    val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&enablejsapi=1&rel=0"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("youtube_player_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF090D1A),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Player",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                ) {
                    InAppDirectWebViewPlayer(
                        embedUrl = embedUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Playing directly inside the app.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveDirectPlayerDialog(
    playable: ArchivePlayableMedia?,
    onDismiss: () -> Unit,
    onConfirmWatched: (() -> Unit)? = null,
    isLoading: Boolean = false,
    unavailableReason: String? = null,
    displayTitle: String = "",
    currentEpisode: Int? = null,
    totalEpisodes: Int = 1,
    onSelectEpisode: ((Int) -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    var showEpisodeSelectorDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("archive_player_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF070B16),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header
                val resolvedHeaderTitle = playable?.title?.takeIf { it.isNotBlank() } ?: displayTitle
                val epNum = playable?.matchedEpisode ?: currentEpisode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "EXOPLAYER HD",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (epNum != null && epNum > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "EPISODE $epNum",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF10B981)),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (playable != null && playable.format.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = playable.format.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF10B981)),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = resolvedHeaderTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Player",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Player Surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                color = Color(0xFF38BDF8),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        unavailableReason != null || playable == null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Can't Find Video",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = unavailableReason ?: "Can't find a full-length video for \"$resolvedHeaderTitle\" on the Internet Archive.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (onRetry != null) {
                                        Button(
                                            onClick = onRetry,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Retry")
                                        }
                                    }
                                    if (totalEpisodes > 1 && onSelectEpisode != null) {
                                        OutlinedButton(
                                            onClick = { showEpisodeSelectorDialog = true },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                                        ) {
                                            Text("Select Episode")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text("Close")
                                    }
                                }
                            }
                        }
                        else -> {
                            val fallbackUrls = remember(playable) {
                                playable.allCompatibleFiles.map { file ->
                                    InternetArchiveApiService.getDirectDownloadUrl(playable.identifier, file.name)
                                }.filter { it != playable.streamUrl }
                            }
                            val streamLabels = remember(playable) {
                                val primary = listOf(playable.fileName to playable.format)
                                val others = playable.allCompatibleFiles
                                    .filter { it.name != playable.fileName }
                                    .map { it.name to (it.format ?: "MP4") }
                                primary + others
                            }

                            ExoPlayerVideoView(
                                mediaUrl = playable.streamUrl,
                                modifier = Modifier.fillMaxSize(),
                                title = playable.title,
                                subtitleBadge = if (epNum != null && epNum > 0) "Episode $epNum" else playable.format.uppercase(),
                                fallbackUrls = fallbackUrls,
                                streamLabels = streamLabels,
                                isFullscreenMode = true,
                                onPreviousEpisode = if (totalEpisodes > 1 && epNum != null && epNum > 1 && onSelectEpisode != null) {
                                    { onSelectEpisode(epNum - 1) }
                                } else null,
                                onNextEpisode = if (totalEpisodes > 1 && epNum != null && epNum < totalEpisodes && onSelectEpisode != null) {
                                    { onSelectEpisode(epNum + 1) }
                                } else null,
                                onOpenEpisodeSelector = if (totalEpisodes > 1 && onSelectEpisode != null) {
                                    { showEpisodeSelectorDialog = true }
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer with Title info, Episode Selector Button, and Confirmation Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playable?.description?.takeIf { it.isNotBlank() } ?: resolvedHeaderTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (totalEpisodes > 1 && onSelectEpisode != null) {
                            OutlinedButton(
                                onClick = { showEpisodeSelectorDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                            ) {
                                Text("Episodes", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        if (onConfirmWatched != null && playable != null) {
                            Button(
                                onClick = {
                                    onConfirmWatched()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+1 Ep Watched", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEpisodeSelectorDialog && onSelectEpisode != null) {
        EpisodeGridSelectorDialog(
            animeTitle = displayTitle.ifBlank { playable?.title.orEmpty() },
            totalEpisodes = totalEpisodes,
            currentEpisode = currentEpisode ?: playable?.matchedEpisode ?: 1,
            watchedEpisodeCount = 0,
            onDismissRequest = { showEpisodeSelectorDialog = false },
            onWatchEpisode = { ep ->
                showEpisodeSelectorDialog = false
                onSelectEpisode(ep)
            },
            onSetWatchedCount = { ep ->
                showEpisodeSelectorDialog = false
                onSelectEpisode(ep)
            }
        )
    }
}

