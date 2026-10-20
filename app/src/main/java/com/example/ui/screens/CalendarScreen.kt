package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.AnimeEntity
import com.example.ui.components.AnimeCard
import com.example.viewmodel.AnimeViewModel
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: AnimeViewModel,
    onNavigateToSearch: () -> Unit,
    onSelectAnime: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onBarsVisibilityChange: ((Boolean) -> Unit)? = null,
    onOpenPlayer: (animeId: Int, episodeNumber: Int) -> Unit = { _, _ -> }
) {
    val watchlist by viewModel.watchlist.collectAsState()
    val upcomingList by viewModel.upcomingAnime.collectAsState()
    val allOngoingList by viewModel.allOngoingAnime.collectAsState()
    val safeModeEnabled by viewModel.safeModeEnabled.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isStealthModeEnabled by viewModel.isStealthModeEnabled.collectAsState()

    // Scroll state & auto-hiding bars logic with smooth gesture detection
    val listState = rememberLazyListState()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -12f) {
                    onBarsVisibilityChange?.invoke(false)
                } else if (delta > 12f) {
                    onBarsVisibilityChange?.invoke(true)
                }
                return Offset.Zero
            }
        }
    }

    // Filter upcoming anime (status NOT_YET_RELEASED or future release date)
    val sortedUpcoming = remember(upcomingList, watchlist, safeModeEnabled) {
        val combined = (upcomingList + watchlist.filter { it.status == "NOT_YET_RELEASED" }).distinctBy { it.id }
        val filtered = if (safeModeEnabled) combined.filter { !it.isAdult } else combined
        filtered.sortedBy { it.airingTimeUnix ?: Long.MAX_VALUE }
    }

    // Filter ongoing releasing anime (strictly exclude completed/finished anime)
    val sortedOngoing = remember(allOngoingList, watchlist, safeModeEnabled) {
        val combined = (allOngoingList + watchlist.filter {
            val st = it.status?.uppercase() ?: ""
            st == "RELEASING" || st == "CURRENTLY_AIRING" || st == "AIRING"
        }).distinctBy { it.id }
        val filtered = combined.filter { anime ->
            val st = anime.status?.uppercase() ?: ""
            (st == "RELEASING" || st == "CURRENTLY_AIRING" || st == "AIRING") && (!safeModeEnabled || !anime.isAdult)
        }
        filtered.sortedBy { it.airingTimeUnix ?: Long.MAX_VALUE }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.syncScheduleAndMAL() },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .nestedScroll(nestedScrollConnection)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Frosted Glass Header
            item {
                Surface(
                    color = Color(0xFF080C1B).copy(alpha = 0.95f),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color(0xFF38BDF8).copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ANIME CALENDAR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Upcoming & Ongoing",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Live release dates updated from MAL & AniList",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.70f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                IconButton(
                                    onClick = onNavigateToSearch,
                                    modifier = Modifier.testTag("calendar_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Anime Anywhere",
                                        tint = Color.White
                                    )
                                }
                            }
                            // Safe Mode Toggle
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (safeModeEnabled) Color(0xFFEF4444).copy(alpha = 0.18f) else Color(0xFF10B981).copy(alpha = 0.18f),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (safeModeEnabled) Color(0xFFEF4444).copy(alpha = 0.85f) else Color(0xFF10B981).copy(alpha = 0.85f)
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.toggleSafeMode() }
                                    .testTag("safe_mode_toggle_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (safeModeEnabled) "OFF" else "ON",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        ),
                                        color = if (safeModeEnabled) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (safeModeEnabled) "Safe Mode" else "Unlocked",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                            // Refresh Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.syncScheduleAndMAL() },
                                    modifier = Modifier.testTag("refresh_calendar_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Schedule",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. UPCOMING ANIME SECTION AT TOP
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Upcoming Anime",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Future season premieres & release dates",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (sortedUpcoming.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Refreshing upcoming anime release dates...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = sortedUpcoming,
                                key = { "upcoming_${it.id}" }
                            ) { anime ->
                                UpcomingAnimeCard(
                                    anime = anime,
                                    onWishlistToggle = { viewModel.toggleWishlist(anime) },
                                    onClick = { onSelectAnime(anime.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. ALL ONGOING ANIME SECTION
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "All Ongoing Anime",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Live countdown timeline & watch progress",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (sortedOngoing.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No ongoing anime found",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the refresh icon above to fetch current weekly release schedules from MAL & AniList.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = sortedOngoing,
                    key = { "ongoing_${it.id}" }
                ) { anime ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        AnimeCard(
                            anime = anime,
                            onWatchedToggle = { isWatched -> viewModel.toggleWatched(anime.id, isWatched) },
                            onFavoriteToggle = { viewModel.toggleFavorite(anime) },
                            onNotificationToggle = { viewModel.toggleNotification(anime) },
                            onCustomEpisodeChange = { newCount -> viewModel.updateWatchedEpisodeCount(anime.id, newCount) },
                            onWishlistToggle = { viewModel.toggleWishlist(anime) },
                            onClick = { onSelectAnime(anime.id) },
                            stealthMode = isStealthModeEnabled,
                            onWatchClick = {
                                val nextEp = (anime.watchedEpisodeCount + 1).coerceIn(1, anime.totalEpisodes ?: maxOf(anime.getAiredEpisodeCount(), 1))
                                onOpenPlayer(anime.id, nextEp)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingAnimeCard(
    anime: AnimeEntity,
    onWishlistToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color(0xFF38BDF8).copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.15f)
                )
            )
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A).copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                AsyncImage(
                    model = anime.coverImage,
                    contentDescription = anime.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Translucent cinematic gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
                // Frosted Date Pill Badge (Top Left)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.60f)),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatReleaseDate(anime),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFFE0F2FE),
                            fontSize = 10.sp
                        )
                    }
                }

                // Wishlist Quick Action Button (Frosted Circle Top Right)
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(28.dp)
                        .align(Alignment.TopEnd)
                        .clickable { onWishlistToggle() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (anime.isWishlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Wishlist",
                            tint = if (anime.isWishlist) Color(0xFF38BDF8) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = anime.displayTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (anime.genres.isNotEmpty()) anime.genres.split(",").take(2).joinToString(" • ") else "Upcoming",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.70f)
                )
            }
        }
    }
}

private fun formatReleaseDate(anime: AnimeEntity): String {
    val airingUnix = anime.airingTimeUnix
    val nowSeconds = System.currentTimeMillis() / 1000
    return if (airingUnix != null && airingUnix > nowSeconds) {
        val diffSeconds = airingUnix - nowSeconds
        val days = diffSeconds / 86400
        val istFormatted = com.example.util.IstTimeFormatter.formatIstDateTime(airingUnix)
        if (days > 0) "$istFormatted ($days d)" else istFormatted
    } else if (!anime.season.isNullOrBlank() && anime.seasonYear != null && anime.seasonYear > 0) {
        val seasonCapitalized = anime.season.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        "$seasonCapitalized ${anime.seasonYear}"
    } else if (anime.seasonYear != null && anime.seasonYear > 0) {
        "Expected ${anime.seasonYear}"
    } else {
        "TBA"
    }
}
