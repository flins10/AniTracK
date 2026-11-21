package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AnimeEntity
import com.example.model.WatchStatus
import com.example.model.WatchlistLayoutMode
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldSoft
import com.example.ui.theme.ChampagneOnGold
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.widgets.AnimeGridCard
import com.example.ui.widgets.AnimeListRowCard
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppEmptyState
import com.example.ui.widgets.AppIconButton
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.AppSearchField
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.ui.widgets.ObsidianTopBar
import com.example.ui.widgets.QuickContextActionSheet
import com.example.ui.widgets.ShimmerSkeletonBox
import com.example.viewmodel.AnimeViewModel
import com.example.viewmodel.DiscoverFilterState
import java.time.LocalDate
import java.time.Year

private val ANILIST_GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy",
    "Horror", "Mystery", "Psychological", "Romance", "Sci-Fi",
    "Slice of Life", "Sports", "Supernatural", "Thriller"
)

private val SORT_OPTIONS = listOf(
    "POPULARITY_DESC" to "Popular",
    "TRENDING_DESC" to "Trending",
    "SCORE_DESC" to "Highest-Rated",
    "START_DATE_DESC" to "Newest",
    "TITLE_ROMAJI" to "Title"
)

private data class QuickDiscoveryMode(
    val id: String,
    val label: String,
    val isActive: (DiscoverFilterState, String, Int) -> Boolean,
    val transform: (DiscoverFilterState, String, Int) -> DiscoverFilterState
)

private fun currentAnimeSeason(): String {
    return when (LocalDate.now().monthValue) {
        1, 2, 3 -> "WINTER"
        4, 5, 6 -> "SPRING"
        7, 8, 9 -> "SUMMER"
        else -> "FALL"
    }
}

private val DISCOVERY_SECTIONS = listOf(
    QuickDiscoveryMode(
        id = "trending",
        label = "Trending",
        isActive = { f, _, _ -> f.sortBy == "TRENDING_DESC" && f.mediaType == "ALL" && f.season == null },
        transform = { f, _, _ ->
            f.copy(mediaType = "ALL", country = "ALL", season = null, yearFrom = null, sortBy = "TRENDING_DESC")
        }
    ),
    QuickDiscoveryMode(
        id = "popular",
        label = "Popular",
        isActive = { f, _, _ -> f.sortBy == "POPULARITY_DESC" && f.mediaType == "ALL" && f.season == null && f.status == "ALL" },
        transform = { f, _, _ ->
            f.copy(mediaType = "ALL", country = "ALL", status = "ALL", season = null, yearFrom = null, sortBy = "POPULARITY_DESC")
        }
    ),
    QuickDiscoveryMode(
        id = "highest_rated",
        label = "Top Rated",
        isActive = { f, _, _ -> f.sortBy == "SCORE_DESC" && f.mediaType == "ALL" },
        transform = { f, _, _ ->
            f.copy(mediaType = "ALL", country = "ALL", season = null, yearFrom = null, sortBy = "SCORE_DESC")
        }
    ),
    QuickDiscoveryMode(
        id = "seasonal",
        label = "Seasonal",
        isActive = { f, curSeason, curYear -> f.season == curSeason && f.yearFrom == curYear },
        transform = { f, curSeason, curYear ->
            f.copy(mediaType = "ALL", country = "ALL", season = curSeason, yearFrom = curYear, sortBy = "POPULARITY_DESC")
        }
    ),
    QuickDiscoveryMode(
        id = "donghua",
        label = "Donghua",
        isActive = { f, _, _ -> f.mediaType == "DONGHUA" },
        transform = { f, _, _ ->
            f.copy(mediaType = "DONGHUA", country = "CN", season = null, yearFrom = null)
        }
    ),
    QuickDiscoveryMode(
        id = "airing",
        label = "Airing",
        isActive = { f, _, _ -> f.status == "RELEASING" && f.mediaType == "ALL" },
        transform = { f, _, _ ->
            f.copy(mediaType = "ALL", country = "ALL", status = "RELEASING")
        }
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: AnimeViewModel,
    onSelectAnime: (Int) -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val userProfile by viewModel.userProfile.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isNextPageLoading by viewModel.isNextPageLoading.collectAsState()
    val hasNextPage by viewModel.hasNextDiscoverPageState.collectAsState()
    val lastPageStartIndex by viewModel.discoverLastPageStartIndex.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()

    val savedIds = remember(watchlist) { watchlist.map { it.id }.toSet() }
    var showFilterSheet by remember { mutableStateOf(false) }
    var contextAnime by remember { mutableStateOf<AnimeEntity?>(null) }

    val curSeason = remember { currentAnimeSeason() }
    val curYear = remember { Year.now().value }
    val activeSectionTitle = remember(filterState, curSeason, curYear) {
        DISCOVERY_SECTIONS.firstOrNull { it.isActive(filterState, curSeason, curYear) }?.label ?: "Catalog Results"
    }

    val isGrid = preferences.watchlistLayout == WatchlistLayoutMode.GRID
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.ensureDiscoverLoaded()
    }

    LaunchedEffect(filterState, searchQuery, preferences.safeModeEnabled) {
        gridState.scrollToItem(0)
    }

    // Automatically load next page once the 3rd row of the active batch is crossed
    LaunchedEffect(gridState, isGrid, results.size, lastPageStartIndex, hasNextPage, isSearching, isNextPageLoading) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            val lastVisibleItemIndex = visible.lastOrNull()?.index ?: -1
            val cols = if (isGrid) {
                visible.filter { it.index > 0 }.map { it.column }.distinct().size.coerceAtLeast(2)
            } else {
                1
            }
            // Account for the header item at index 0
            val lastVisibleResultIdx = lastVisibleItemIndex - 1
            Pair(lastVisibleResultIdx, cols)
        }.collect { (lastVisibleResultIdx, cols) ->
            val count = results.size
            if (count > 0 && lastVisibleResultIdx >= 0 && hasNextPage && !isSearching && !isNextPageLoading) {
                val rowThreshold = if (isGrid) cols * 3 else 4
                val triggerIdx = minOf(count - 1, maxOf(0, lastPageStartIndex + rowThreshold - 1))
                if (lastVisibleResultIdx >= triggerIdx) {
                    viewModel.loadNextDiscoverPage()
                }
            }
        }
    }

    ObsidianAmbientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1280.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("discover_screen")
            ) {
                // Header + Search + Gold Capsule Discovery Dock + Genres
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ObsidianTopBar(
                        title = "Discover",
                        subtitle = "The Global Catalog",
                        userProfile = userProfile,
                        onOpenProfile = onNavigateToProfile,
                        actions = {
                            AppIconButton(
                                icon = if (preferences.watchlistLayout == WatchlistLayoutMode.GRID) {
                                    Icons.Filled.ViewList
                                } else {
                                    Icons.Filled.GridView
                                },
                                contentDescription = "Toggle grid or list view",
                                onClick = {
                                    val nextLayout = if (preferences.watchlistLayout == WatchlistLayoutMode.GRID) {
                                        WatchlistLayoutMode.LIST
                                    } else {
                                        WatchlistLayoutMode.GRID
                                    }
                                    viewModel.setWatchlistLayout(nextLayout)
                                }
                            )

                            BadgedBox(
                                badge = {
                                    if (filterState.activeFilterCount > 0) {
                                        Badge(containerColor = colors.accentPrimary, contentColor = colors.onAccentPrimary) {
                                            Text(filterState.activeFilterCount.toString())
                                        }
                                    }
                                }
                            ) {
                                AppIconButton(
                                    icon = Icons.Filled.FilterList,
                                    contentDescription = "Open filters",
                                    onClick = { showFilterSheet = true },
                                    modifier = Modifier.testTag("discover_filter_button")
                                )
                            }
                        }
                    )

                    AppSearchField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = "Search 25,000+ anime & Donghua...",
                        modifier = Modifier.testTag("discover_search_input")
                    )

                    // Catalogue Discovery Dock (Capsule Pills with Champagne Gold Active Gradient)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DISCOVERY_SECTIONS, key = { it.id }) { section ->
                            val active = section.isActive(filterState, curSeason, curYear)
                            val bgBrush = if (active) {
                                if (colors.isDark) Brush.linearGradient(listOf(ChampagneGoldSoft, ChampagneGold))
                                else Brush.linearGradient(listOf(colors.accentPrimary, colors.accentSecondary))
                            } else {
                                Brush.linearGradient(listOf(colors.elevatedSurface.copy(alpha = 0.58f), colors.cardBackground.copy(alpha = 0.76f)))
                            }
                            Box(
                                modifier = Modifier
                                    .height(35.dp)
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clip(CircleShape)
                                    .background(bgBrush)
                                    .clickable {
                                        viewModel.updateFilterState(section.transform(filterState, curSeason, curYear))
                                    }
                                    .padding(horizontal = 15.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = section.label.uppercase(),
                                    style = MicroUpperLabel.copy(fontSize = 9.5.sp, letterSpacing = 0.9.sp),
                                    color = if (active) {
                                        if (colors.isDark) ChampagneOnGold else colors.onAccentPrimary
                                    } else {
                                        colors.textSecondary
                                    }
                                )
                            }
                        }
                        item(key = "safe_mode_chip") {
                            val safeOn = preferences.safeModeEnabled
                            Surface(
                                shape = CircleShape,
                                color = if (safeOn) colors.elevatedSurface.copy(alpha = 0.72f) else colors.error.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .height(35.dp)
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clip(CircleShape)
                                    .clickable { viewModel.setSafeModeEnabled(!safeOn) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 13.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = if (safeOn) colors.success else colors.error,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (safeOn) "SAFE ON" else "18+ SHOWN",
                                        style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 0.7.sp),
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Genre Pills
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item(key = "genre_all") {
                            GenreFilterPill(
                                label = "All Genres",
                                selected = filterState.selectedGenres.isEmpty(),
                                onClick = { viewModel.updateFilterState(filterState.copy(selectedGenres = emptySet())) }
                            )
                        }
                        items(ANILIST_GENRES.take(10), key = { it }) { genre ->
                            val selected = filterState.selectedGenres.contains(genre)
                            GenreFilterPill(
                                label = genre,
                                selected = selected,
                                onClick = {
                                    val next = if (selected) filterState.selectedGenres - genre else setOf(genre)
                                    viewModel.updateFilterState(filterState.copy(selectedGenres = next))
                                }
                            )
                        }
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isSearching && results.isNotEmpty(),
                    onRefresh = { viewModel.performSearch(resetPage = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isSearching && results.isEmpty()) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 156.dp),
                            contentPadding = PaddingValues(start = AppSpacing.md, end = AppSpacing.md, top = 8.dp, bottom = 112.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(6) {
                                ShimmerSkeletonBox(
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.56f)
                                )
                            }
                        }
                    } else if (!searchError.isNullOrBlank() && results.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(AppSpacing.xl), contentAlignment = Alignment.Center) {
                            AppEmptyState(
                                icon = Icons.Filled.SearchOff,
                                title = "Catalog unavailable",
                                description = searchError ?: "Failed to load catalog from AniList.",
                                actionLabel = "Retry",
                                onAction = { viewModel.performSearch(resetPage = true) }
                            )
                        }
                    } else if (results.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(AppSpacing.xl), contentAlignment = Alignment.Center) {
                            AppEmptyState(
                                icon = Icons.Filled.SearchOff,
                                title = "No matching titles",
                                description = "Adjust your search query, genre, year, or format filters to see more anime.",
                                actionLabel = "Reset Filters",
                                onAction = { viewModel.resetFilters() }
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = if (isGrid) GridCells.Adaptive(minSize = 164.dp) else GridCells.Fixed(1),
                            contentPadding = PaddingValues(start = AppSpacing.md, end = AppSpacing.md, top = 8.dp, bottom = 112.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Search Results" else activeSectionTitle,
                                        fontFamily = CormorantSerifFamily,
                                        fontSize = 23.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${results.size} TITLES",
                                        style = MicroUpperLabel.copy(fontSize = 9.5.sp, letterSpacing = 1.5.sp),
                                        color = colors.accentPrimary
                                    )
                                }
                            }

                            items(results, key = { it.id }) { anime ->
                                val liveAnime = remember(anime, watchlist) {
                                    watchlist.firstOrNull { it.id == anime.id } ?: anime
                                }
                                val inList = savedIds.contains(anime.id) || liveAnime.inWatchlist
                                if (isGrid) {
                                    AnimeGridCard(
                                        anime = liveAnime,
                                        titlePreference = preferences.titleLanguage,
                                        isInUserList = inList,
                                        onClick = { onSelectAnime(liveAnime.id) },
                                        onLongClick = { contextAnime = liveAnime },
                                        onQuickAddOrIncrement = {
                                            if (inList) viewModel.incrementEpisode(liveAnime.id)
                                            else viewModel.addToWatchlist(liveAnime, WatchStatus.WATCHING)
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                } else {
                                    AnimeListRowCard(
                                        anime = liveAnime,
                                        titlePreference = preferences.titleLanguage,
                                        onClick = { onSelectAnime(liveAnime.id) },
                                        onLongClick = { contextAnime = liveAnime },
                                        onIncrementEpisode = {
                                            if (inList) viewModel.incrementEpisode(liveAnime.id)
                                            else viewModel.addToWatchlist(liveAnime, WatchStatus.WATCHING)
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }

                            if (isNextPageLoading || hasNextPage) {
                                item(
                                    key = "discover_pagination_footer",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isNextPageLoading) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    color = colors.accentPrimary,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Loading more anime...",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = CircleShape,
                                                color = colors.elevatedSurface.copy(alpha = 0.72f),
                                                border = BorderStroke(1.dp, colors.borderStrong.copy(alpha = 0.6f)),
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable { viewModel.loadNextDiscoverPage() }
                                                    .testTag("discover_load_more_button")
                                            ) {
                                                Text(
                                                    text = "LOAD MORE ANIME",
                                                    style = MicroUpperLabel.copy(fontSize = 10.sp, letterSpacing = 1.2.sp),
                                                    color = colors.accentPrimary,
                                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        DiscoverFilterBottomSheet(
            current = filterState,
            safeModeEnabled = preferences.safeModeEnabled,
            onToggleSafeMode = { viewModel.setSafeModeEnabled(it) },
            onDismiss = { showFilterSheet = false },
            onApply = { updated ->
                viewModel.updateFilterState(updated)
                showFilterSheet = false
            },
            onReset = {
                viewModel.resetFilters()
                showFilterSheet = false
            }
        )
    }

    contextAnime?.let { target ->
        val liveTarget = watchlist.firstOrNull { it.id == target.id } ?: target
        val isTargetInList = savedIds.contains(target.id) || liveTarget.inWatchlist
        QuickContextActionSheet(
            anime = liveTarget,
            titlePreference = preferences.titleLanguage,
            isInUserList = isTargetInList,
            onDismiss = { contextAnime = null },
            onSave = { status, progress, seasonNumber, scoreTenths, fav ->
                viewModel.updateTracking(
                    animeId = liveTarget.id,
                    status = status,
                    episodeProgress = progress,
                    scoreTenths = scoreTenths,
                    notes = liveTarget.userNotes.orEmpty(),
                    isFavorite = fav,
                    seasonNumber = seasonNumber
                )
                contextAnime = null
            },
            onOpenFullEditor = {
                contextAnime = null
                onSelectAnime(liveTarget.id)
            },
            onRemove = if (isTargetInList) {
                {
                    viewModel.removeFromWatchlist(liveTarget.id)
                    contextAnime = null
                }
            } else null
        )
    }
}

@Composable
private fun GenreFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = CircleShape,
        color = if (selected) colors.accentPrimary.copy(alpha = 0.18f) else colors.elevatedSurface.copy(alpha = 0.65f),
        border = if (selected) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.65f)) else null,
        modifier = Modifier
            .height(30.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (selected) colors.highlight else colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DiscoverFilterBottomSheet(
    current: DiscoverFilterState,
    safeModeEnabled: Boolean,
    onToggleSafeMode: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onApply: (DiscoverFilterState) -> Unit,
    onReset: () -> Unit
) {
    val colors = AppTheme.colors
    var draft by remember(current) { mutableStateOf(current) }
    var yearInput by remember(current) { mutableStateOf(current.yearFrom?.toString() ?: "") }
    val currentYear = remember { Year.now().value }
    val quickYears = remember(currentYear) { listOf(currentYear, currentYear - 1, currentYear - 2, currentYear - 5) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.backgroundSecondary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Catalog Filters", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                TextButton(onClick = onReset) { Text("Reset", color = colors.accentPrimary) }
            }

            Text("ORIGIN", style = MicroUpperLabel, color = colors.accentPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                listOf("ALL" to "All Catalog", "ANIME" to "Anime (JP)", "DONGHUA" to "Donghua (CN)").forEach { (key, label) ->
                    FilterChip(
                        selected = draft.mediaType == key,
                        onClick = {
                            draft = draft.copy(
                                mediaType = key,
                                country = when (key) {
                                    "DONGHUA" -> "CN"
                                    "ANIME" -> "JP"
                                    else -> "ALL"
                                }
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = colors.highlight
                        ),
                        label = { Text(label) }
                    )
                }
            }

            Text("SORT BY", style = MicroUpperLabel, color = colors.accentPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                SORT_OPTIONS.forEach { (key, label) ->
                    FilterChip(
                        selected = draft.sortBy == key,
                        onClick = { draft = draft.copy(sortBy = key) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = colors.highlight
                        ),
                        label = { Text(label) }
                    )
                }
            }

            Text("FORMAT", style = MicroUpperLabel, color = colors.accentPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                listOf("ALL", "TV", "MOVIE", "OVA", "ONA", "SPECIAL").forEach { fmt ->
                    FilterChip(
                        selected = draft.format == fmt,
                        onClick = { draft = draft.copy(format = fmt) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = colors.highlight
                        ),
                        label = { Text(fmt) }
                    )
                }
            }

            Text("AIRING STATUS", style = MicroUpperLabel, color = colors.accentPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                listOf(
                    "ALL" to "All",
                    "RELEASING" to "Airing",
                    "FINISHED" to "Finished",
                    "NOT_YET_RELEASED" to "Upcoming"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = draft.status == key,
                        onClick = { draft = draft.copy(status = key) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = colors.highlight
                        ),
                        label = { Text(label) }
                    )
                }
            }

            Text("SEASON & YEAR", style = MicroUpperLabel, color = colors.accentPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                listOf(null to "Any", "WINTER" to "Winter", "SPRING" to "Spring", "SUMMER" to "Summer", "FALL" to "Fall").forEach { (key, label) ->
                    FilterChip(
                        selected = draft.season == key,
                        onClick = { draft = draft.copy(season = key) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = colors.highlight
                        ),
                        label = { Text(label) }
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                FilterChip(
                    selected = draft.yearFrom == null,
                    onClick = { yearInput = ""; draft = draft.copy(yearFrom = null) },
                    shape = CircleShape,
                    label = { Text("Any Year") }
                )
                quickYears.forEach { yr ->
                    FilterChip(
                        selected = draft.yearFrom == yr,
                        onClick = { yearInput = yr.toString(); draft = draft.copy(yearFrom = yr) },
                        shape = CircleShape,
                        label = { Text(yr.toString()) }
                    )
                }
            }
            OutlinedTextField(
                value = yearInput,
                onValueChange = { raw ->
                    yearInput = raw.filter { it.isDigit() }.take(4)
                    draft = draft.copy(yearFrom = yearInput.toIntOrNull())
                },
                label = { Text("Custom Release Year") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("GENRES", style = MicroUpperLabel, color = colors.accentPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                ANILIST_GENRES.forEach { genre ->
                    val selected = draft.selectedGenres.contains(genre)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = if (selected) draft.selectedGenres - genre else draft.selectedGenres + genre
                            draft = draft.copy(selectedGenres = next)
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = colors.highlight
                        ),
                        label = { Text(genre) }
                    )
                }
            }

            AppCard(shape = RoundedCornerShape(22.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Safe Mode (Hide 18+ Adult Tags)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("Filter explicit catalog results discreetly", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Switch(checked = safeModeEnabled, onCheckedChange = onToggleSafeMode)
                }
            }

            AppPrimaryButton(
                text = "Apply Filters",
                onClick = { onApply(draft) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
        }
    }
}
