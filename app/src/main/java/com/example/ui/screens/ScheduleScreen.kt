package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimeCard
import com.example.viewmodel.AnimeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.TextStyle
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.local.AnimeEntity

private val WEEKDAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
private val WEEKDAY_LABELS = listOf("M", "T", "W", "TH", "F", "S", "SU")
private val FULL_WEEKDAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: AnimeViewModel,
    onNavigateToSearch: () -> Unit,
    onSelectAnime: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onBarsVisibilityChange: ((Boolean) -> Unit)? = null,
    onOpenPlayer: (animeId: Int, episodeNumber: Int) -> Unit = { _, _ -> }
) {
    val selectedDay by viewModel.selectedDay.collectAsState()
    val dayAnimeList by viewModel.dayAnime.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isStealthModeEnabled by viewModel.isStealthModeEnabled.collectAsState()

    val today = remember { LocalDate.now() }
    val currentDayOfWeek = remember(today) { today.dayOfWeek.value }
    val targetDate = remember(selectedDay, today, currentDayOfWeek) {
        today.plusDays((selectedDay - currentDayOfWeek).toLong())
    }
    val selectedDayTitle = FULL_WEEKDAYS.getOrNull(selectedDay - 1) ?: "Sunday"
    val selectedDateText = remember(targetDate) {
        targetDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)).uppercase()
    }

    LaunchedEffect(Unit) {
        viewModel.setDay(LocalDate.now().dayOfWeek.value)
    }

    val watchedCount = remember(dayAnimeList) { dayAnimeList.count { it.isWatched } }
    val totalCount = dayAnimeList.size
    val progressPercent = if (totalCount > 0) (watchedCount * 100) / totalCount else 0

    // Selected Anime for Long-Press Pin/Remove Action Dialog
    var selectedAnimeForAction by remember { mutableStateOf<AnimeEntity?>(null) }

    // Scroll state & Auto-Hiding bars logic with ultra-smooth gesture detection
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

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.syncScheduleAndMAL() },
        state = pullRefreshState,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Transparent)
            .nestedScroll(nestedScrollConnection)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Section (Dynamic Island + Day Strip): slides smoothly WITH the anime template cards
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // --- TOP SECTION: THE DYNAMIC ISLAND ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = Color(0xFF090E1D).copy(alpha = 0.82f),
                            border = BorderStroke(
                                1.4.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFF06B6D4),
                                        Color.White.copy(alpha = 0.50f),
                                        Color(0xFF38BDF8)
                                    )
                                )
                            ),
                            shadowElevation = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dynamic_island_top")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Inner Left: Large elegant white dropdown text "Sunday ▼" & date "OCTOBER 11, 2026"
                                Column(
                                    modifier = Modifier
                                        .clickable {
                                            val nextDay = if (selectedDay >= 7) 1 else selectedDay + 1
                                            viewModel.setDay(nextDay)
                                        }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = selectedDayTitle,
                                            style = TextStyle(
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Select Day",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedDateText,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    )
                                }

                                // Inner Right: Single borderless circular glass-like Search icon
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF38BDF8).copy(alpha = 0.28f),
                                                    Color(0xFF06B6D4).copy(alpha = 0.12f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .clickable { onNavigateToSearch() }
                                        .testTag("search_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Anime",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- CALENDAR DAY CLASSIFICATION STRIP (ONLY: M, T, W, TH, F, S, SU) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WEEKDAY_LABELS.forEachIndexed { index, dayLabel ->
                            val dayNumber = index + 1
                            val isSelected = dayNumber == selectedDay

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF22D3EE),
                                                    Color(0xFF0284C7)
                                                )
                                            )
                                        )
                                        .border(
                                            1.5.dp,
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.White,
                                                    Color(0xFF38BDF8)
                                                )
                                            ),
                                            CircleShape
                                        )
                                        .clickable { viewModel.setDay(dayNumber) }
                                        .testTag("tab_active_$dayLabel"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLabel,
                                        style = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(
                                            0.8.dp,
                                            Color.White.copy(alpha = 0.12f),
                                            CircleShape
                                        )
                                        .clickable { viewModel.setDay(dayNumber) }
                                        .testTag("tab_$dayLabel"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLabel,
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF94A3B8)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // --- MIDDLE SECTION: "Anime Count" Glass Progress Banner ---
            if (totalCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                1.2.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color(0xFF06B6D4).copy(alpha = 0.60f),
                                        Color.White.copy(alpha = 0.12f)
                                    )
                                ),
                                RoundedCornerShape(24.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF0F172A).copy(alpha = 0.85f),
                                            Color(0xFF090E1B).copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Anime Count: $totalCount Shows Scheduled",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "($watchedCount of $totalCount watched this $selectedDayTitle)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }

                                // Circular Progress Indicator Ring
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { if (totalCount > 0) watchedCount.toFloat() / totalCount else 0f },
                                        modifier = Modifier.size(44.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = Color.White.copy(alpha = 0.12f),
                                        strokeWidth = 4.5.dp
                                    )
                                    Text(
                                        text = "$progressPercent%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (dayAnimeList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No anime scheduled for $selectedDayTitle",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add currently airing anime to your watchlist to track their weekly release schedule.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onNavigateToSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("add_anime_cta")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Discover & Add Anime")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = dayAnimeList,
                    key = { it.id }
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
                            onLongClick = { selectedAnimeForAction = anime },
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

    // Long Press Pin / Remove Action Dialog
    if (selectedAnimeForAction != null) {
        val anime = selectedAnimeForAction!!
        AlertDialog(
            onDismissRequest = { selectedAnimeForAction = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFF090E1A),
            title = {
                Text(
                    text = "Manage Anime",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                )
            },
            text = {
                Column {
                    Text(
                        text = anime.displayTitle,
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Pin / Unpin Option
                    Surface(
                        onClick = {
                            viewModel.togglePinAnime(anime)
                            selectedAnimeForAction = null
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF131B2E),
                        border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (anime.isPinned) "📌 Unpin from Planner" else "📌 Pin to Top of Planner",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Remove from AniTrAk Only Option
                    Surface(
                        onClick = {
                            viewModel.removeAnime(anime, com.example.viewmodel.RemovalScope.LOCAL_ONLY, "WATCHLIST")
                            selectedAnimeForAction = null
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF131B2E),
                        border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🗑 Remove from AniTrAk Only",
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Remove from Both Option
                    Surface(
                        onClick = {
                            viewModel.removeAnime(anime, com.example.viewmodel.RemovalScope.BOTH, "WATCHLIST")
                            selectedAnimeForAction = null
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF131B2E),
                        border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🗑 Remove from AniTrAk & MAL",
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // View Details Option
                    Surface(
                        onClick = {
                            selectedAnimeForAction = null
                            onSelectAnime(anime.id)
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF131B2E),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.20f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ℹ️ View Details",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
