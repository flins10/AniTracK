package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimeCard
import com.example.viewmodel.AnimeViewModel
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    viewModel: AnimeViewModel,
    onNavigateToSearch: () -> Unit,
    onSelectAnime: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onBarsVisibilityChange: ((Boolean) -> Unit)? = null,
    onOpenPlayer: (animeId: Int, episodeNumber: Int) -> Unit = { _, _ -> }
) {
    val watchlistAnime by viewModel.watchlist.collectAsState()
    val wishlistAnimeList by viewModel.wishlist.collectAsState()
    val isMalSyncing by viewModel.isMalSyncing.collectAsState()
    val isMalLoggedIn by viewModel.isMalLoggedIn.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isStealthModeEnabled by viewModel.isStealthModeEnabled.collectAsState()

    var selectedStatusTab by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val statusTabs = listOf("ALL", "WATCHING", "COMPLETED", "ON HOLD", "DROPPED", "PLAN TO WATCH")

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

    // Combine all user tracked anime
    val combinedList = remember(watchlistAnime, wishlistAnimeList) {
        (watchlistAnime + wishlistAnimeList).distinctBy { it.id }
    }

    val statusFilteredList = remember(combinedList, selectedStatusTab) {
        when (selectedStatusTab) {
            "WATCHING" -> combinedList.filter { it.inWatchlist && !it.isWatched && !it.isWishlist }
            "COMPLETED" -> combinedList.filter { it.isWatched }
            "PLAN TO WATCH" -> combinedList.filter { it.isWishlist }
            "ON HOLD", "DROPPED" -> combinedList.filter { it.inWatchlist && !it.isWatched && !it.isWishlist }
            else -> combinedList
        }
    }

    val filteredList = remember(statusFilteredList, searchQuery) {
        if (searchQuery.isBlank()) statusFilteredList
        else statusFilteredList.filter {
            (it.titleRomaji.contains(searchQuery, ignoreCase = true)) ||
            (it.titleEnglish?.contains(searchQuery, ignoreCase = true) == true) ||
            (it.titleNative?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing || isMalSyncing,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Section: Frosted Library Header
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMalLoggedIn) "MYANIMELIST SYNCED LIST" else "MY ANIME TRACKER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "My Anime Library",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "${combinedList.size} titles in library • ${if (isMalLoggedIn) "Connected to MAL" else "Local mode"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.70f)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Search Icon Button to search anywhere
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.10f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    IconButton(
                                        onClick = onNavigateToSearch,
                                        modifier = Modifier.testTag("wishlist_global_search_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search Anime Anywhere",
                                            tint = Color.White
                                        )
                                    }
                                }

                                if (isMalLoggedIn) {
                                    IconButton(
                                        onClick = { viewModel.fetchMalProfileAndSync() },
                                        enabled = !isMalSyncing,
                                        modifier = Modifier.testTag("my_list_sync_btn")
                                    ) {
                                        if (isMalSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "Sync with MAL",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Filter Tabs
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(statusTabs) { tab ->
                                FilterChip(
                                    selected = selectedStatusTab == tab,
                                    onClick = { selectedStatusTab = tab },
                                    label = { Text(text = tab, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        labelColor = Color.White.copy(alpha = 0.85f)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedStatusTab == tab,
                                        borderColor = Color.White.copy(alpha = 0.18f),
                                        selectedBorderColor = Color.White.copy(alpha = 0.45f)
                                    )
                                )
                            }
                        }

                        if (combinedList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search my library...", color = Color.White.copy(alpha = 0.5f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wishlist_search_input")
                            )
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (combinedList.isEmpty()) "Your Library is Empty" else "No anime in '$selectedStatusTab'",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (combinedList.isEmpty())
                                    "Add anime to your watchlist or connect your MyAnimeList account to sync your collection."
                                else "Try changing filter tab or search query.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            if (combinedList.isEmpty()) {
                                Button(
                                    onClick = onNavigateToSearch,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("wishlist_browse_btn")
                                ) {
                                    Text(text = "Browse & Discover Anime")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredList,
                    key = { it.id }
                ) { anime ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
