package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import com.example.util.PosterColorExtractor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.data.local.AnimeEntity
import com.example.viewmodel.AnimeViewModel
import com.example.viewmodel.DiscoverFilterState

private val ALL_GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy",
    "Romance", "Sci-Fi", "Slice of Life", "Donghua", "Supernatural",
    "Mystery", "Sports", "Thriller", "Mecha", "Music",
    "Psychological", "Horror", "Isekai", "Ecchi", "Hentai",
    "Mahou Shoujo", "Martial Arts", "Historical", "Military", "School",
    "Shounen", "Shoujo", "Seinen", "Josei", "Cyberpunk",
    "Space", "Vampire", "Super Power", "Cultivation", "Xianxia",
    "Demons", "Game", "Mythology", "Suspense", "Detective", "Gourmet", "Parody"
)

private val SORTS = listOf(
    "POPULARITY_DESC" to "Popularity",
    "SCORE_DESC" to "Highest Rated",
    "START_DATE_DESC" to "Newest Release",
    "TITLE_ROMAJI" to "Title (A-Z)",
    "EPISODES_DESC" to "Episode Count"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: AnimeViewModel,
    onSelectAnime: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onBarsVisibilityChange: ((Boolean) -> Unit)? = null,
    onOpenPlayer: (animeId: Int, episodeNumber: Int) -> Unit = { _, _ -> }
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val isNextPageLoading by viewModel.isNextPageLoading.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val titlePreference by viewModel.titleLanguagePreference.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.ensureDiscoverLoaded()
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val watchlistIds = remember(watchlist) { watchlist.map { it.id }.toSet() }

    // Scroll state & auto-hiding bars logic with smooth gesture detection
    val gridState = rememberLazyGridState()

    val shouldLoadMore = remember {
        androidx.compose.runtime.derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsNumber > 0 && lastVisibleItemIndex >= totalItemsNumber - 6
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextPage()
        }
    }

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

    PullToRefreshBox(
        isRefreshing = isSearching,
        onRefresh = { viewModel.refreshSearch() },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .nestedScroll(nestedScrollConnection)
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 155.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Section: The Frosted Search Bar & Catalogue Discovery Header
            item(span = { GridItemSpan(maxLineSpan) }) {
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
                        .padding(bottom = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CATALOGUE DISCOVERY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Explore Anime & Donghua",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = Color.White
                                )
                            }
                            if (filterState.activeFilterCount > 0 || searchQuery.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.resetFilters() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset All", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Compact Frosted Search Input Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = {
                                Text(
                                    "Search anime or donghua...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
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
                                .testTag("search_input_field")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Media Type Selector Tabs (All / Anime / Donghua)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "ALL" to "All",
                                "ANIME" to "Anime",
                                "DONGHUA" to "Donghua"
                            ).forEach { (typeKey, label) ->
                                val isSelected = filterState.mediaType == typeKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setMediaType(typeKey) },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("media_tab_$typeKey")
                                )
                            }
                        }

                        // Season Quick Selector Bar (Fall, Winter, Spring, Summer)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val seasonList = listOf(
                                null to "All Seasons",
                                "FALL" to "Fall",
                                "WINTER" to "Winter",
                                "SPRING" to "Spring",
                                "SUMMER" to "Summer"
                            )
                            items(seasonList) { (seasKey, seasLabel) ->
                                val isSelected = if (seasKey == null) {
                                    filterState.season == null
                                } else {
                                    filterState.season?.equals(seasKey, ignoreCase = true) == true ||
                                    (seasKey == "FALL" && filterState.season?.equals("AUTUMN", ignoreCase = true) == true)
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected && seasKey != null) {
                                            viewModel.setSelectedSeason(null)
                                        } else {
                                            viewModel.setSelectedSeason(seasKey)
                                        }
                                    },
                                    label = { Text(seasLabel, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.testTag("season_chip_${seasKey ?: "ALL"}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Control Bar: Filters Button & Sort Dropdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Filters Sheet Trigger
                            Button(
                                onClick = { showFilterSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (filterState.activeFilterCount > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = if (filterState.activeFilterCount > 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (filterState.activeFilterCount > 0) "Filters (${filterState.activeFilterCount})" else "Filters",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (filterState.activeFilterCount > 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Sort Dropdown Menu
                            Box {
                                OutlinedButton(
                                    onClick = { showSortMenu = true },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val currentSortLabel = SORTS.firstOrNull { it.first == filterState.sortBy }?.second ?: "Popularity"
                                    Text(
                                        text = "Sort: $currentSortLabel",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SORTS.forEach { (sortKey, sortLabel) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    sortLabel,
                                                    fontWeight = if (filterState.sortBy == sortKey) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setSortBy(sortKey)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Active Filter Chips Bar (with removable X on each)
                        if (filterState.activeFilterCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (filterState.mediaType != "ALL") {
                                    item {
                                        ActiveFilterChip(
                                            label = if (filterState.mediaType == "DONGHUA") "Donghua" else "Anime",
                                            onRemove = { viewModel.setMediaType("ALL") }
                                        )
                                    }
                                }
                                if (filterState.status != "ALL") {
                                    item {
                                        ActiveFilterChip(
                                            label = "Status: ${filterState.status}",
                                            onRemove = { viewModel.updateFilterState(filterState.copy(status = "ALL")) }
                                        )
                                    }
                                }
                                if (filterState.season != null) {
                                    item {
                                        val seasonLabel = when (filterState.season?.uppercase()) {
                                            "FALL", "AUTUMN" -> "Fall"
                                            "WINTER" -> "Winter"
                                            "SPRING" -> "Spring"
                                            "SUMMER" -> "Summer"
                                            else -> filterState.season
                                        }
                                        ActiveFilterChip(
                                            label = "Season: $seasonLabel",
                                            onRemove = { viewModel.setSelectedSeason(null) }
                                        )
                                    }
                                }
                                if (filterState.yearFrom != null || filterState.yearTo != null) {
                                    item {
                                        val yearText = if (filterState.yearFrom == filterState.yearTo) "${filterState.yearFrom}" else "${filterState.yearFrom ?: "Any"} - ${filterState.yearTo ?: "Any"}"
                                        ActiveFilterChip(
                                            label = "Year: $yearText",
                                            onRemove = { viewModel.setSelectedYear(null) }
                                        )
                                    }
                                }
                                items(filterState.selectedGenres.toList()) { genre ->
                                    ActiveFilterChip(
                                        label = genre,
                                        onRemove = { viewModel.toggleGenreFilter(genre) }
                                    )
                                }
                                if (filterState.format != "ALL") {
                                    item {
                                        ActiveFilterChip(
                                            label = "Format: ${filterState.format}",
                                            onRemove = { viewModel.updateFilterState(filterState.copy(format = "ALL")) }
                                        )
                                    }
                                }
                                if (filterState.country != "ALL") {
                                    item {
                                        ActiveFilterChip(
                                            label = "Country: ${filterState.country}",
                                            onRemove = { viewModel.updateFilterState(filterState.copy(country = "ALL")) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${searchResults.size} titles found",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (isSearching && searchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (searchError != null && searchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Unable to load catalogue",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = searchError ?: "Check your network connection and retry.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refreshSearch() },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Retry Catalogue")
                            }
                        }
                    }
                }
            } else if (searchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No media titles found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try clearing active filters or modifying search keywords.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetFilters() },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Reset All Filters")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = searchResults,
                    key = { it.id }
                ) { anime ->
                    val isInWatchlist = watchlistIds.contains(anime.id)
                    SearchResultCard(
                        anime = anime,
                        isInWatchlist = isInWatchlist,
                        titlePreference = titlePreference,
                        onAddToggle = {
                            if (isInWatchlist) {
                                viewModel.removeFromWatchlist(anime.id)
                            } else {
                                viewModel.addToWatchlist(anime)
                            }
                        },
                        onClick = { onSelectAnime(anime.id) },
                        onWatchClick = { onOpenPlayer(anime.id, 1) }
                    )
                }

                if (isNextPageLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Filter Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            FilterBottomSheetContent(
                viewModel = viewModel,
                filterState = filterState,
                onUpdateFilter = { updated -> viewModel.updateFilterState(updated) },
                onReset = {
                    viewModel.resetFilters()
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheetContent(
    viewModel: AnimeViewModel,
    filterState: DiscoverFilterState,
    onUpdateFilter: (DiscoverFilterState) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val (currentSeason, currentYear) = remember { viewModel.getCurrentDateSeasonAndYear() }
    var tempState by remember(filterState) {
        mutableStateOf(
            if (filterState.season == null && filterState.yearFrom == null) {
                filterState.copy(season = currentSeason, yearFrom = currentYear, yearTo = currentYear)
            } else {
                filterState
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Detailed Catalogue Filters",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            TextButton(onClick = onReset) {
                Text("Reset All")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Media Type
        Text("Media Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL" to "All", "ANIME" to "Anime", "DONGHUA" to "Donghua").forEach { (type, name) ->
                FilterChip(
                    selected = tempState.mediaType == type,
                    onClick = { tempState = tempState.copy(mediaType = type) },
                    label = { Text(name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Airing Status
        Text("Airing Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "ALL" to "All",
                "RELEASING" to "Airing",
                "UPCOMING" to "Upcoming",
                "FINISHED" to "Finished"
            ).forEach { (st, name) ->
                FilterChip(
                    selected = tempState.status == st,
                    onClick = { tempState = tempState.copy(status = st) },
                    label = { Text(name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Season
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Season", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "Auto-selected based on date",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                null to "All",
                "FALL" to "Fall",
                "WINTER" to "Winter",
                "SPRING" to "Spring",
                "SUMMER" to "Summer"
            ).forEach { (seas, name) ->
                val isSelected = if (seas == null) {
                    tempState.season == null
                } else {
                    tempState.season?.equals(seas, ignoreCase = true) == true ||
                    (seas == "FALL" && tempState.season?.equals("AUTUMN", ignoreCase = true) == true)
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { tempState = tempState.copy(season = if (isSelected && seas != null) null else seas) },
                    label = { Text(name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Release Year Range
        Text("Release Year Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val currentYearValue = remember { java.time.LocalDate.now().year }
            val years = remember(currentYearValue) {
                listOf(null) + ((currentYearValue + 3) downTo 2000).toList()
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text("Release Year:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(years) { y ->
                        FilterChip(
                            selected = tempState.yearFrom == y,
                            onClick = { tempState = tempState.copy(yearFrom = y, yearTo = y) },
                            label = { Text(if (y != null && y > currentYearValue) "$y (upcoming)" else y?.toString() ?: "Any") }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Multi-Select Genres
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Genres (Multi-Select)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Match ALL", style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = tempState.genreMatchAll,
                    onCheckedChange = { tempState = tempState.copy(genreMatchAll = it) },
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ALL_GENRES.forEach { genre ->
                val isSelected = tempState.selectedGenres.contains(genre)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val current = tempState.selectedGenres
                        val updated = if (isSelected) current - genre else current + genre
                        tempState = tempState.copy(selectedGenres = updated)
                    },
                    label = { Text(genre) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Format
        Text("Format", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL" to "All", "TV" to "TV Series", "MOVIE" to "Movie", "OVA" to "OVA", "ONA" to "ONA").forEach { (fmt, name) ->
                FilterChip(
                    selected = tempState.format == fmt,
                    onClick = { tempState = tempState.copy(format = fmt) },
                    label = { Text(name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 7. Country of Origin
        Text("Country of Origin", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL" to "All Countries", "JP" to "Japan", "CN" to "China").forEach { (ct, name) ->
                FilterChip(
                    selected = tempState.country == ct,
                    onClick = { tempState = tempState.copy(country = ct) },
                    label = { Text(name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onUpdateFilter(tempState)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Apply Filters (${tempState.activeFilterCount})")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SearchResultCard(
    anime: AnimeEntity,
    isInWatchlist: Boolean,
    titlePreference: String,
    onAddToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWatchClick: (() -> Unit)? = null
) {
    val resolvedTitle = remember(anime, titlePreference) {
        anime.displayTitle
    }
    val posterTone = remember(anime) { PosterColorExtractor.getPosterColorTone(anime) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = posterTone.primaryGlow.copy(alpha = 0.55f),
                ambientColor = posterTone.ambientGlow.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.3.dp,
                brush = Brush.linearGradient(
                    colors = posterTone.borderColors.map { it.copy(alpha = 0.85f) }
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onClick() }
            .testTag("search_card_${anime.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090E1B).copy(alpha = 0.85f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient frosted neon glow behind card surface
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                posterTone.primaryGlow.copy(alpha = 0.14f),
                                Color.Transparent,
                                posterTone.ambientGlow.copy(alpha = 0.20f)
                            )
                        )
                    )
            )

            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = anime.coverImage,
                        contentDescription = resolvedTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    // Frosted gradient overlay on artwork
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        Color(0xFF090E1B).copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Top frosted specular glare reflection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    if (anime.score != null && anime.score > 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(bottomEnd = 12.dp),
                            border = BorderStroke(0.6.dp, Color(0xFFFDCB6E).copy(alpha = 0.40f)),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFDCB6E),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${anime.score}%",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    if (anime.isDonghua) {
                        Surface(
                            color = Color(0xFFE11D48).copy(alpha = 0.90f),
                            shape = RoundedCornerShape(bottomStart = 12.dp),
                            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = "CN DONGHUA",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
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
                        text = resolvedTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (anime.isDonghua) "Donghua" else "Anime"} • ${anime.format ?: "TV"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = posterTone.secondaryAccent
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (anime.idMal != null) Color(0xFF2E51A2).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(0.6.dp, if (anime.idMal != null) Color(0xFF38BDF8).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = if (anime.idMal != null) "MAL" else "AniList",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (anime.idMal != null) Color(0xFF7DD3FC) else Color.White.copy(alpha = 0.80f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    val totalText = if (anime.totalEpisodes != null && anime.totalEpisodes > 0) "${anime.totalEpisodes} eps" else "Ongoing"
                    Text(
                        text = totalText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { if (onWatchClick != null) onWatchClick() else onClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("search_watch_btn_${anime.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Watch",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            )
                        }

                        Button(
                            onClick = onAddToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isInWatchlist) Color(0xFF059669) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("add_watchlist_btn_${anime.id}")
                        ) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isInWatchlist) "Added ✓" else "+ Add",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
