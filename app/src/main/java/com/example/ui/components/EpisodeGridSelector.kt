package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.AnimeSeasonInfo

/**
 * 5-Column Direct Episode Selection Grid matching reference design.
 * Features:
 * - 25-episode range tabs ("01-25", "26-50", "51-60")
 * - 5-column grid of rounded square episode selectors
 * - Distinct visual states:
 *     * Selected: Glowing vibrant crimson/pink (0xFFE11D48)
 *     * Watched: Deep burgundy (0xFF4C0519) with white text
 *     * Unwatched: Dark slate (0xFF1E293B) with light text
 * - Direct tap to select and play/mark episodes without typing
 */
@Composable
fun EpisodeGridSelector(
    totalEpisodes: Int,
    selectedEpisode: Int,
    watchedEpisodeCount: Int,
    onEpisodeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    seasons: List<AnimeSeasonInfo> = emptyList(),
    selectedSeasonNumber: Int = 1,
    onSeasonSelected: ((Int) -> Unit)? = null,
    rangeChunkSize: Int = 25
) {
    val effectiveTotal = if (totalEpisodes > 0) totalEpisodes else 24

    // Calculate ranges of 25 episodes
    val ranges = remember(effectiveTotal, rangeChunkSize) {
        val list = mutableListOf<Pair<Int, Int>>()
        var start = 1
        while (start <= effectiveTotal) {
            val end = (start + rangeChunkSize - 1).coerceAtMost(effectiveTotal)
            list.add(start to end)
            start += rangeChunkSize
        }
        list
    }

    // Determine which range contains the selected episode
    var selectedRangeIndex by remember(selectedEpisode, ranges) {
        val foundIdx = ranges.indexOfFirst { selectedEpisode in it.first..it.second }
        mutableIntStateOf(if (foundIdx >= 0) foundIdx else 0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Season Tabs (if multi-season)
        if (seasons.size > 1 && onSeasonSelected != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasons.sortedBy { it.seasonNumber }.forEach { season ->
                    val isSeasonActive = season.seasonNumber == selectedSeasonNumber
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSeasonActive) Color(0xFFE11D48) else Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSeasonActive) Color(0xFFFB7185) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .clickable { onSeasonSelected(season.seasonNumber) }
                            .testTag("season_tab_${season.seasonNumber}")
                    ) {
                        Text(
                            text = if (season.title.isNotBlank()) "S${season.seasonNumber}: ${season.title.take(16)}"
                            else "Season ${season.seasonNumber}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSeasonActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Episode Range Tabs ("01-25", "26-50", "51-60")
        if (ranges.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ranges.forEachIndexed { idx, range ->
                    val isRangeActive = idx == selectedRangeIndex
                    val startStr = "%02d".format(range.first)
                    val endStr = "%02d".format(range.second)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRangeActive) Color(0xFFE11D48) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isRangeActive) Color(0xFFFB7185) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .clickable { selectedRangeIndex = idx }
                            .testTag("range_tab_${range.first}_${range.second}")
                    ) {
                        Text(
                            text = "$startStr-$endStr",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isRangeActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = if (isRangeActive) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Current Range Episodes
        val currentRange = ranges.getOrNull(selectedRangeIndex) ?: (1 to effectiveTotal)
        val episodesInCurrentRange = remember(currentRange) {
            (currentRange.first..currentRange.second).toList()
        }

        // 5-Column Episode Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .testTag("episode_grid_5col")
        ) {
            items(episodesInCurrentRange, key = { it }) { ep ->
                val isSelected = ep == selectedEpisode
                val isWatched = ep <= watchedEpisodeCount

                val backgroundColor = when {
                    isSelected -> Color(0xFFE11D48) // Glowing Vibrant Crimson
                    isWatched -> Color(0xFF4C0519) // Deep Burgundy (Watched)
                    else -> Color(0xFF1E293B)      // Dark Slate (Unwatched)
                }

                val borderColor = when {
                    isSelected -> Color(0xFFFB7185)
                    isWatched -> Color(0xFF9F1239)
                    else -> Color(0xFF334155)
                }

                val textColor = when {
                    isSelected -> Color.White
                    isWatched -> Color(0xFFFECDD3)
                    else -> Color(0xFFCBD5E1)
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(backgroundColor)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable { onEpisodeSelected(ep) }
                        .testTag("ep_tile_$ep"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%02d".format(ep),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * High-performance, frost glow dialog allowing direct episode selection from 5-column grid.
 * Replaces any text-input custom dialogs completely.
 */
@Composable
fun EpisodeGridSelectorDialog(
    animeTitle: String,
    totalEpisodes: Int,
    currentEpisode: Int,
    watchedEpisodeCount: Int,
    onDismissRequest: () -> Unit,
    onWatchEpisode: (Int) -> Unit,
    onSetWatchedCount: (Int) -> Unit,
    seasons: List<AnimeSeasonInfo> = emptyList(),
    selectedSeasonNumber: Int = 1,
    onSeasonSelected: ((Int) -> Unit)? = null
) {
    var pickedEpisode by remember { mutableIntStateOf(currentEpisode.coerceAtLeast(1)) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF090D16)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.4f),
                            Color(0xFFE11D48).copy(alpha = 0.3f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
                .testTag("episode_grid_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Episode Selection",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = animeTitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // The 5-Column Grid Selector
                EpisodeGridSelector(
                    totalEpisodes = totalEpisodes,
                    selectedEpisode = pickedEpisode,
                    watchedEpisodeCount = watchedEpisodeCount,
                    onEpisodeSelected = { ep ->
                        pickedEpisode = ep
                    },
                    seasons = seasons,
                    selectedSeasonNumber = selectedSeasonNumber,
                    onSeasonSelected = onSeasonSelected
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mark as Watched
                    OutlinedButton(
                        onClick = {
                            onSetWatchedCount(pickedEpisode)
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF10B981)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dialog_mark_watched_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mark Ep $pickedEpisode",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )
                    }

                    // Watch Episode Directly
                    Button(
                        onClick = {
                            onWatchEpisode(pickedEpisode)
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE11D48)
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("dialog_watch_episode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Watch Ep $pickedEpisode",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
