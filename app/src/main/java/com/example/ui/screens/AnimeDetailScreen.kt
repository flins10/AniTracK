package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoLibrary
import com.example.data.repository.YouTubeSearchLink
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.data.repository.ArchivePlayableMedia
import com.example.data.repository.YouTubeVideo
import com.example.data.repository.YouTubeVideoType
import com.example.ui.components.ArchiveDirectPlayerDialog
import com.example.ui.components.EmbeddedYouTubePlayerDialog
import com.example.ui.components.EpisodeGridSelector
import com.example.ui.components.EpisodeGridSelectorDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.CountdownView
import com.example.viewmodel.AnimeViewModel
import com.example.viewmodel.RemovalScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.TextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    animeId: Int,
    viewModel: AnimeViewModel,
    onBack: () -> Unit,
    onOpenPlayer: (animeId: Int, episodeNumber: Int, seasonNumber: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(animeId) {
        if (viewModel.selectedAnimeDetailId.value != animeId) {
            viewModel.selectAnimeDetail(animeId)
        }
    }

    val animeState by viewModel.selectedAnimeDetail.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val titlePreference by viewModel.titleLanguagePreference.collectAsState()
    val anime = animeState ?: return

    val isInWatchlist = watchlist.any { it.id == anime.id }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var showRemoveScopeDialog by remember { mutableStateOf(false) }

    val youtubeVideos by viewModel.youtubeVideos.collectAsState()
    val isYouTubeSearching by viewModel.isYouTubeSearching.collectAsState()
    val isYouTubeQuotaExceeded by viewModel.isYouTubeQuotaExceeded.collectAsState()
    val isOptionalYouTubeApiEnabled by viewModel.isOptionalYouTubeApiEnabled.collectAsState()
    val keylessYouTubeSearchLinks by viewModel.keylessYouTubeSearchLinks.collectAsState()
    val aniListVerifiedTrailer by viewModel.aniListVerifiedTrailer.collectAsState()
    val archiveVideos by viewModel.archiveVideos.collectAsState()
    val isArchiveSearching by viewModel.isArchiveSearching.collectAsState()
    val fullDetails by viewModel.selectedAnimeFullDetails.collectAsState()
    val isResolvingArchive by viewModel.isResolvingArchiveMedia.collectAsState()
    val activeSeasons by viewModel.activeDetailSeasons.collectAsState()
    val selectedSeason by viewModel.selectedDetailSeason.collectAsState()

    var activeYouTubeVideo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var activeArchiveMedia by remember { mutableStateOf<ArchivePlayableMedia?>(null) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var notesInput by remember(anime.userNotes) { mutableStateOf(anime.userNotes ?: "") }
    var showScoreDialog by remember { mutableStateOf(false) }

    val defaultEpToWatch = (anime.watchedEpisodeCount + 1).coerceIn(1, anime.totalEpisodes ?: maxOf(anime.getAiredEpisodeCount(), 1))
    var selectedEpisodeToWatch by remember(anime.id, anime.watchedEpisodeCount) { mutableStateOf(defaultEpToWatch) }
    var showCustomEpisodeSelectDialog by remember { mutableStateOf(false) }
    var showCustomWatchedInputDialog by remember { mutableStateOf(false) }
    var episodeSearchQuery by remember { mutableStateOf("") }
    var selectedRangeBatchIndex by remember { mutableIntStateOf(0) }

    val resolvedTitle = remember(anime, titlePreference) {
        anime.displayTitle
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Hero Banner & Cover Area
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                ) {
                    // Background Image
                    AsyncImage(
                        model = anime.bannerImage ?: anime.coverImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    // Gradient scrim
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    // Top Bar Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Floating Poster & Main Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .width(115.dp)
                                .height(165.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(24.dp)
                                )
                        ) {
                            AsyncImage(
                                model = anime.coverImage,
                                contentDescription = anime.titleRomaji,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            if (anime.score != null && anime.score > 0) {
                                Surface(
                                    color = Color(0xFFFDCB6E),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${anime.score}% Score",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Text(
                                text = resolvedTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            val subtitleText = when {
                                anime.isDonghua && !anime.titlePinyin.isNullOrEmpty() -> "Pinyin: ${anime.titlePinyin}"
                                !anime.titleNative.isNullOrEmpty() -> anime.titleNative
                                else -> null
                            }
                            if (subtitleText != null) {
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Details Body
            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Quick Info Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val airedEpisodes = anime.getAiredEpisodeCount()
                        val isFinished = anime.status?.uppercase() == "FINISHED" || anime.status == "finished_airing"
                        val totalValueText = if (isFinished && anime.totalEpisodes != null && anime.totalEpisodes > 0) {
                            "${anime.totalEpisodes} Ep"
                        } else if (anime.totalEpisodes != null && anime.totalEpisodes > airedEpisodes) {
                            "${anime.totalEpisodes} Ep"
                        } else {
                            "Ongoing"
                        }

                        InfoBadge(
                            label = "Aired",
                            value = "$airedEpisodes Ep",
                            modifier = Modifier.weight(1f)
                        )
                        InfoBadge(
                            label = "Total",
                            value = totalValueText,
                            modifier = Modifier.weight(1f)
                        )
                        InfoBadge(
                            label = "Status",
                            value = anime.status ?: "Airing",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Genres FlowRow
                    val genresList = remember(anime.genres) {
                        anime.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genresList.forEach { genre ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (activeSeasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AVAILABLE SEASONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeSeasons) { seasonItem ->
                                val isSelectedSeason = (selectedSeason?.seasonNumber == seasonItem.seasonNumber) ||
                                        (selectedSeason?.animeId == seasonItem.animeId)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelectedSeason) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelectedSeason) Color(0xFF38BDF8) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.selectDetailSeason(seasonItem)
                                            if (seasonItem.animeId != anime.id) {
                                                viewModel.selectAnimeDetail(seasonItem.animeId)
                                            }
                                            selectedEpisodeToWatch = 1
                                        }
                                        .testTag("season_chip_${seasonItem.seasonNumber}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = seasonItem.title.ifBlank { "Season ${seasonItem.seasonNumber}" },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelectedSeason) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        if (seasonItem.totalEpisodes != null && seasonItem.totalEpisodes > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${seasonItem.totalEpisodes} Ep)",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSelectedSeason) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Prominent Watch Button & Custom Episode Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onOpenPlayer(anime.id, selectedEpisodeToWatch, selectedSeason?.seasonNumber ?: 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("detail_watch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Watch",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White)
                            )
                        }

                        OutlinedButton(
                            onClick = { showCustomEpisodeSelectDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("detail_episode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Select Episode",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ep $selectedEpisodeToWatch",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5-COLUMN EPISODE SELECTOR & RANGE TABS
                    val totalAnimeEpisodes = (selectedSeason?.totalEpisodes ?: anime.totalEpisodes ?: maxOf(anime.getAiredEpisodeCount(), selectedEpisodeToWatch, 12)).coerceAtLeast(1)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECT EPISODE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFF38BDF8)
                                )
                                Surface(
                                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "$totalAnimeEpisodes Ep Total",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            EpisodeGridSelector(
                                totalEpisodes = totalAnimeEpisodes,
                                selectedEpisode = selectedEpisodeToWatch,
                                watchedEpisodeCount = anime.watchedEpisodeCount,
                                onEpisodeSelected = { epNum ->
                                    selectedEpisodeToWatch = epNum
                                    onOpenPlayer(anime.id, epNum, selectedSeason?.seasonNumber ?: 1)
                                },
                                seasons = activeSeasons,
                                selectedSeasonNumber = selectedSeason?.seasonNumber ?: 1,
                                onSeasonSelected = { seasonNum ->
                                    val matched = activeSeasons.firstOrNull { it.seasonNumber == seasonNum }
                                    if (matched != null) {
                                        viewModel.selectDetailSeason(matched)
                                        if (matched.animeId != anime.id) {
                                            viewModel.selectAnimeDetail(matched.animeId)
                                        }
                                        selectedEpisodeToWatch = 1
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row (Add to Schedule, Wishlist, Favorite)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isInWatchlist) showRemoveScopeDialog = true
                                else viewModel.addToWatchlist(anime)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isInWatchlist) Color(0xFF00B894) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_watchlist_toggle")
                        ) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isInWatchlist) "In Schedule" else "Add Schedule",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Wishlist button
                        OutlinedButton(
                            onClick = { viewModel.toggleWishlist(anime) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("detail_wishlist_toggle"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (anime.isWishlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Wishlist",
                                tint = if (anime.isWishlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Favorite button
                        OutlinedButton(
                            onClick = { viewModel.toggleFavorite(anime) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("detail_favorite_toggle"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (anime.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (anime.isFavorite) Color(0xFFFD79A8) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Multi-Source Airing Verification Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE AIRING CONSENSUS ENGINE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Aired Ep: ${anime.currentEpisode}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Cross-referenced via AniList • MyAnimeList • Jikan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MyAnimeList Library Status Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(28.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MYANIMELIST LIBRARY STATUS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (anime.idMal != null && anime.idMal > 0) "MAL ID: ${anime.idMal}" else "Auto-match",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val statusOptions = listOf(
                                "Plan to Watch" to "plan_to_watch",
                                "Watching" to "watching",
                                "Completed" to "completed",
                                "On Hold" to "on_hold",
                                "Dropped" to "dropped"
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                statusOptions.forEach { (label, malValue) ->
                                    val isSelected = when (malValue) {
                                        "plan_to_watch" -> anime.isWishlist
                                        "completed" -> anime.isWatched
                                        "watching" -> isInWatchlist && !anime.isWatched
                                        else -> false
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateAnimeMalStatus(anime, malValue)
                                        },
                                        label = { Text(label) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Next Release & Notification Card (Bento Box)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(28.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "NEXT EPISODE SCHEDULE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val effectiveAiring = anime.getEffectiveAiringTime()
                            val effectiveNextEp = anime.getEffectiveNextEpisodeNumber()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CountdownView(airingTimeUnix = effectiveAiring)
                                Text(
                                    text = "Episode $effectiveNextEp",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }

                            if (effectiveAiring != null && effectiveAiring > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "IST Release Time",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Release (IST): ${com.example.util.IstTimeFormatter.formatIstDateTime(effectiveAiring)}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Release Alerts",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Notify when new episode airs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = anime.notificationsEnabled,
                                    onCheckedChange = { viewModel.toggleNotification(anime) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("detail_notification_switch")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Synopsis Card
                    if (!anime.synopsis.isNullOrEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    RoundedCornerShape(28.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "SYNOPSIS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = anime.synopsis ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isSynopsisExpanded) "Show Less" else "Read More",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Episode Tracker Card
                    val liveAiredCount = anime.getAiredEpisodeCount()
                    val isShowFinished = anime.status?.uppercase() == "FINISHED" || anime.status == "finished_airing"
                    val totalEp = if (anime.totalEpisodes != null && anime.totalEpisodes > 0) {
                        anime.totalEpisodes
                    } else {
                        maxOf(liveAiredCount, anime.watchedEpisodeCount + 10, 12)
                    }
                    val watchedCount = anime.watchedEpisodeCount
                    val remainingEp = (totalEp - watchedCount).coerceAtLeast(0)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (anime.isWatched || watchedCount >= totalEp) Color(0xFF00B894).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(28.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "EPISODE TRACKER & SELECTOR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Watched Ep $watchedCount of $totalEp",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                    Text(
                                        text = if (remainingEp == 0) "All $totalEp episodes completed! 🎉" else "$remainingEp episodes left to watch",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (remainingEp == 0) Color(0xFF00B894) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(
                                    checked = anime.isWatched || watchedCount >= totalEp,
                                    onCheckedChange = { checked -> viewModel.toggleWatched(anime.id, checked) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00B894)),
                                    modifier = Modifier.testTag("detail_episode_checkbox")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Stepper Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val newCount = (watchedCount - 1).coerceAtLeast(0)
                                        viewModel.updateWatchedEpisodeCount(anime.id, newCount)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrement watched episode",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "-1 Ep",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showCustomWatchedInputDialog = true }
                                        .testTag("detail_watched_pill")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Ep $watchedCount / $totalEp",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Set watched episode",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val newCount = watchedCount + 1
                                        viewModel.updateWatchedEpisodeCount(anime.id, newCount)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "+1 Ep",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increment watched episode"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onOpenPlayer(anime.id, selectedEpisodeToWatch, selectedSeason?.seasonNumber ?: 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Watch Ep $selectedEpisodeToWatch Now",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                            }

                            val progressFraction = if (totalEp > 0) (watchedCount.toFloat() / totalEp.toFloat()).coerceIn(0f, 1f) else 0f
                            if (progressFraction > 0f) {
                                Spacer(modifier = Modifier.height(14.dp))
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF00B894),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Direct Episode Selection",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            EpisodeGridSelector(
                                totalEpisodes = totalEp,
                                selectedEpisode = selectedEpisodeToWatch,
                                watchedEpisodeCount = watchedCount,
                                onEpisodeSelected = { ep ->
                                    selectedEpisodeToWatch = ep
                                    onOpenPlayer(anime.id, ep, selectedSeason?.seasonNumber ?: 1)
                                },
                                seasons = activeSeasons,
                                selectedSeasonNumber = selectedSeason?.seasonNumber ?: 1,
                                onSeasonSelected = { seasonNum ->
                                    val matched = activeSeasons.firstOrNull { it.seasonNumber == seasonNum }
                                    if (matched != null) {
                                        viewModel.selectDetailSeason(matched)
                                        if (matched.animeId != anime.id) {
                                            viewModel.selectAnimeDetail(matched.animeId)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ========================================================
                    // SECTION: UNIFIED ANIME TRACKING & PERSONAL NOTES
                    // ========================================================
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(28.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TRACKING STATUS & RATING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = anime.trackingStatusDisplay,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Status Chips Flow
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val statuses = listOf(
                                    "watching" to "Watching",
                                    "plan_to_watch" to "Planning",
                                    "completed" to "Completed",
                                    "on_hold" to "On Hold",
                                    "dropped" to "Dropped"
                                )
                                statuses.forEach { (statusCode, label) ->
                                    val isSelected = anime.userStatus?.lowercase() == statusCode ||
                                            (statusCode == "watching" && anime.userStatus == null && isInWatchlist)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateUserTracking(anime.id, status = statusCode)
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Personal Score & Notes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Personal Score",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (anime.userScore != null && anime.userScore > 0) "${anime.userScore}/10" else "Not Rated",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showScoreDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (anime.userScore != null) "Change Score" else "Rate",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Personal Notes Preview
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNotesDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Personal Notes",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (!anime.userNotes.isNullOrBlank()) anime.userNotes else "Tap to write your thoughts or notes on this anime...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (!anime.userNotes.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ========================================================
                    // SECTION: STUDIOS, CHARACTERS & RELATIONS
                    // ========================================================
                    val studios = fullDetails?.studios?.nodes.orEmpty().filter { !it.name.isNullOrBlank() }
                    val characters = fullDetails?.characters?.nodes.orEmpty().filter { !it.name?.full.isNullOrBlank() }
                    val relations = fullDetails?.relations?.edges.orEmpty().filter { it.node?.title != null }

                    if (studios.isNotEmpty() || characters.isNotEmpty() || relations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    RoundedCornerShape(28.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "CAST, STUDIOS & RELATIONS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (studios.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Apartment,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Studio: " + studios.joinToString(", ") { it.name ?: "" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (characters.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Characters",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(characters.take(10)) { charItem ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(64.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E293B)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (charItem.image?.medium != null) {
                                                        AsyncImage(
                                                            model = charItem.image.medium,
                                                            contentDescription = charItem.name?.full,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = charItem.name?.full ?: "",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                if (relations.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Related Anime / Sequels",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        relations.take(6).forEach { relation ->
                                            val node = relation.node ?: return@forEach
                                            val relationType = relation.relationType ?: "RELATED"
                                            val title = node.title?.english ?: node.title?.romaji ?: "Anime"
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                                modifier = Modifier.clickable {
                                                    viewModel.selectAnimeDetail(node.id)
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = relationType.replace("_", " "),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ========================================================
                    // SECTION: INTERNET ARCHIVE FULL EPISODE & MOVIE STREAMS
                    // ========================================================
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                Color(0xFF38BDF8).copy(alpha = 0.3f),
                                RoundedCornerShape(28.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ARCHIVE.ORG",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "FULL EPISODE & MOVIE STREAMS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                if (isArchiveSearching || isResolvingArchive) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (archiveVideos.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    archiveVideos.take(4).forEach { archiveItem ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.resolveArchiveMedia(archiveItem.identifier) { playable ->
                                                        activeArchiveMedia = playable
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 70.dp, height = 50.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF0F172A)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = archiveItem.thumbnailUrl,
                                                        contentDescription = archiveItem.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Play",
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = archiveItem.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = listOfNotNull(archiveItem.creator, archiveItem.year).joinToString(" • ").ifBlank { "Full video stream on Internet Archive" },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (!isArchiveSearching) {
                                Text(
                                    text = "Tap 'Watch Episode' above to stream directly in the unified ExoPlayer, or 'Can't find' will be displayed if unavailable on Internet Archive.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ========================================================
        // PLAYBACK & INTERACTION DIALOGS
        // ========================================================
        activeArchiveMedia?.let { playable ->
            ArchiveDirectPlayerDialog(
                playable = playable,
                isLoading = isResolvingArchive,
                unavailableReason = null,
                onDismiss = { activeArchiveMedia = null },
                onConfirmWatched = {
                    viewModel.confirmEpisodeCompletion(anime.id, anime.watchedEpisodeCount + 1)
                }
            )
        }

        if (showScoreDialog) {
            AlertDialog(
                onDismissRequest = { showScoreDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFF090E1A),
                title = {
                    Text(
                        text = "Rate '${anime.displayTitle}'",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Select your score from 1 (Appalling) to 10 (Masterpiece):",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (1..10).forEach { scoreVal ->
                                val isSelected = anime.userScore == scoreVal
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E293B),
                                    modifier = Modifier.clickable {
                                        viewModel.updateUserTracking(anime.id, score = scoreVal)
                                        showScoreDialog = false
                                    }
                                ) {
                                    Text(
                                        text = "$scoreVal ★",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else Color.White
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showScoreDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        if (showNotesDialog) {
            AlertDialog(
                onDismissRequest = { showNotesDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFF090E1A),
                title = {
                    Text(
                        text = "Personal Notes",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Save personal notes, thoughts, or viewing notes for this anime:",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            placeholder = { Text("Write your thoughts here...", color = Color(0xFF64748B)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateUserTracking(anime.id, notes = notesInput.trim())
                            showNotesDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("Save Notes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotesDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        if (showCustomEpisodeSelectDialog) {
            val totalEpDialog = (selectedSeason?.totalEpisodes ?: anime.totalEpisodes ?: maxOf(anime.getAiredEpisodeCount(), selectedEpisodeToWatch, 12)).coerceAtLeast(1)
            EpisodeGridSelectorDialog(
                animeTitle = resolvedTitle,
                totalEpisodes = totalEpDialog,
                currentEpisode = selectedEpisodeToWatch,
                watchedEpisodeCount = anime.watchedEpisodeCount,
                onDismissRequest = { showCustomEpisodeSelectDialog = false },
                onWatchEpisode = { ep ->
                    selectedEpisodeToWatch = ep
                    showCustomEpisodeSelectDialog = false
                    onOpenPlayer(anime.id, ep, selectedSeason?.seasonNumber ?: 1)
                },
                onSetWatchedCount = { ep ->
                    selectedEpisodeToWatch = ep
                    viewModel.updateWatchedEpisodeCount(anime.id, ep)
                    showCustomEpisodeSelectDialog = false
                },
                seasons = activeSeasons,
                selectedSeasonNumber = selectedSeason?.seasonNumber ?: 1,
                onSeasonSelected = { sNum ->
                    val matched = activeSeasons.firstOrNull { it.seasonNumber == sNum }
                    if (matched != null) {
                        viewModel.selectDetailSeason(matched)
                        if (matched.animeId != anime.id) {
                            viewModel.selectAnimeDetail(matched.animeId)
                        }
                    }
                }
            )
        }

        if (showCustomWatchedInputDialog) {
            val totalEpDialog = (selectedSeason?.totalEpisodes ?: anime.totalEpisodes ?: maxOf(anime.getAiredEpisodeCount(), selectedEpisodeToWatch, 12)).coerceAtLeast(1)
            EpisodeGridSelectorDialog(
                animeTitle = resolvedTitle,
                totalEpisodes = totalEpDialog,
                currentEpisode = anime.watchedEpisodeCount.coerceAtLeast(1),
                watchedEpisodeCount = anime.watchedEpisodeCount,
                onDismissRequest = { showCustomWatchedInputDialog = false },
                onWatchEpisode = { ep ->
                    viewModel.updateWatchedEpisodeCount(anime.id, ep)
                    selectedEpisodeToWatch = ep
                    showCustomWatchedInputDialog = false
                    onOpenPlayer(anime.id, ep, selectedSeason?.seasonNumber ?: 1)
                },
                onSetWatchedCount = { ep ->
                    viewModel.updateWatchedEpisodeCount(anime.id, ep)
                    showCustomWatchedInputDialog = false
                },
                seasons = activeSeasons,
                selectedSeasonNumber = selectedSeason?.seasonNumber ?: 1,
                onSeasonSelected = { sNum ->
                    val matched = activeSeasons.firstOrNull { it.seasonNumber == sNum }
                    if (matched != null) {
                        viewModel.selectDetailSeason(matched)
                        if (matched.animeId != anime.id) {
                            viewModel.selectAnimeDetail(matched.animeId)
                        }
                    }
                }
            )
        }

        if (showRemoveScopeDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveScopeDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFF090E1A),
                title = {
                    Text(
                        text = "Remove Anime Options",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Choose how to remove '${anime.displayTitle}':",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.removeAnime(anime, RemovalScope.LOCAL_ONLY, "WATCHLIST")
                                showRemoveScopeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove from AniTrAk Only", color = Color.White)
                        }
                        Button(
                            onClick = {
                                viewModel.removeAnime(anime, RemovalScope.PROVIDER_ONLY, "WATCHLIST")
                                showRemoveScopeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove from MyAnimeList Only", color = Color.White)
                        }
                        Button(
                            onClick = {
                                viewModel.removeAnime(anime, RemovalScope.BOTH, "WATCHLIST")
                                showRemoveScopeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove from Both", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showRemoveScopeDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            RoundedCornerShape(20.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
