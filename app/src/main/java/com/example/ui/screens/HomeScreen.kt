package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.local.AnimeEntity
import com.example.model.VisibilityLevel
import com.example.model.WatchStatus
import com.example.ui.theme.AppMotion
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumLarge
import com.example.ui.widgets.AnimeCardContextMenuSheet
import com.example.ui.widgets.AppEmptyState
import com.example.ui.widgets.AppIconButton
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.ui.widgets.ObsidianTopBar
import com.example.ui.widgets.PosterTrackerCard
import com.example.ui.widgets.RemoveFromHomeConfirmDialog
import com.example.ui.widgets.TrackingEditorDialog
import com.example.viewmodel.AnimeViewModel
import java.time.LocalDate

private val DAY_LABELS = listOf(
    1 to ("Mon" to "Monday"),
    2 to ("Tue" to "Tuesday"),
    3 to ("Wed" to "Wednesday"),
    4 to ("Thu" to "Thursday"),
    5 to ("Fri" to "Friday"),
    6 to ("Sat" to "Saturday"),
    7 to ("Sun" to "Sunday")
)

private data class DayAnimeSection(
    val key: String,
    val title: String,
    val isToday: Boolean,
    val items: List<AnimeEntity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AnimeViewModel,
    onSelectAnime: (Int) -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToMyList: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onOpenQuickSearch: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val userProfile by viewModel.userProfile.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val privacy by viewModel.privacySettings.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val homeAnime by viewModel.homeAnime.collectAsState()
    val dayAnime by viewModel.homeDayAnime.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val isRefreshing by viewModel.isHomeRefreshing.collectAsState()
    val homeError by viewModel.homeError.collectAsState()

    var showAllWeekDays by rememberSaveable { mutableStateOf(true) }
    var contextAnime by remember { mutableStateOf<AnimeEntity?>(null) }
    var editingAnime by remember { mutableStateOf<AnimeEntity?>(null) }
    var pendingRemoveFromHome by remember { mutableStateOf<AnimeEntity?>(null) }

    val savedIds = remember(watchlist) { watchlist.map { it.id }.toSet() }
    val todayDate = remember { LocalDate.now() }
    val todayDow = todayDate.dayOfWeek.value

    val sortedHomeAnime = remember(homeAnime) {
        homeAnime.sortedWith(
            compareByDescending<AnimeEntity> { it.isPinned }
                .thenBy { it.watchStatusEnum != WatchStatus.WATCHING }
                .thenBy { it.airingTimeUnix ?: Long.MAX_VALUE }
                .thenByDescending { it.lastWatchedTime ?: 0L }
        )
    }

    val dayCounts = remember(homeAnime) {
        (1..7).associateWith { day ->
            homeAnime.count { it.airingDayOfWeek == day }
        }
    }

    // Order days starting from Today wrapping Mon..Sun, then Anytime
    val dayWiseSections = remember(sortedHomeAnime, todayDow) {
        if (sortedHomeAnime.isEmpty()) emptyList()
        else buildList {
            val orderedDays = (0 until 7).map { offset -> ((todayDow - 1 + offset) % 7) + 1 }
            orderedDays.forEach { dow ->
                val showsForDay = sortedHomeAnime.filter { it.airingDayOfWeek == dow }
                if (showsForDay.isNotEmpty()) {
                    val dayName = DAY_LABELS.firstOrNull { it.first == dow }?.second?.second ?: "Day $dow"
                    add(
                        DayAnimeSection(
                            key = "day_sec_$dow",
                            title = if (dow == todayDow) "Today • $dayName" else dayName,
                            isToday = dow == todayDow,
                            items = showsForDay
                        )
                    )
                }
            }
            val anytimeShows = sortedHomeAnime.filter { it.airingDayOfWeek !in 1..7 }
            if (anytimeShows.isNotEmpty()) {
                add(
                    DayAnimeSection(
                        key = "day_sec_anytime",
                        title = "Active Tracking",
                        isToday = false,
                        items = anytimeShows
                    )
                )
            }
        }
    }

    val selectedDayHeading = DAY_LABELS.firstOrNull { it.first == selectedDay }?.second?.second ?: "Schedule"

    val pullRefreshState = rememberPullToRefreshState()

    ObsidianAmbientBackground {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshHome() },
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    containerColor = colors.elevatedSurface,
                    color = colors.accentPrimary,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                val columnCount = when {
                    maxWidth >= 1160.dp -> 5
                    maxWidth >= 860.dp -> 4
                    maxWidth >= 580.dp -> 3
                    else -> 2
                }

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 1280.dp)
                        .fillMaxSize()
                        .statusBarsPadding()
                        .testTag("home_screen"),
                    contentPadding = PaddingValues(
                        start = AppSpacing.md,
                        end = AppSpacing.md,
                        top = AppSpacing.xs,
                        bottom = 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    // Editorial Header
                    item {
                        ObsidianTopBar(
                            title = "ANIME ",
                            accentTitleTail = "UNIVERSE",
                            subtitle = "Weekly Schedule",
                            userProfile = userProfile,
                            onOpenProfile = onNavigateToProfile,
                            actions = {
                                AppIconButton(
                                    icon = Icons.Filled.Search,
                                    contentDescription = "Quick search",
                                    onClick = onOpenQuickSearch,
                                    modifier = Modifier.testTag("home_search_button")
                                )
                            }
                        )
                    }

                    // "This Week" Section Title + Library Link
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "This Week",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Surface(
                                shape = CircleShape,
                                color = colors.elevatedSurface.copy(alpha = 0.78f),
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clip(CircleShape)
                                    .clickable(onClick = onNavigateToMyList)
                            ) {
                                Text(
                                    text = "LIBRARY ›",
                                    style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 1.6.sp),
                                    color = colors.accentPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Horizontal Week Strip (Plain "All" without date + Mon..Sun with crisp dates)
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item(key = "day_all") {
                                WeekDayPill(
                                    dowLabel = "All",
                                    dateText = null,
                                    countText = null,
                                    selected = showAllWeekDays,
                                    isToday = false,
                                    onClick = { showAllWeekDays = true },
                                    modifier = Modifier.testTag("week_pill_all")
                                )
                            }
                            items(DAY_LABELS, key = { it.first }) { (dayInt, pair) ->
                                val dayDate = todayDate.plusDays((dayInt - todayDow).toLong())
                                val count = if (!showAllWeekDays && selectedDay == dayInt) {
                                    dayAnime.size
                                } else {
                                    dayCounts[dayInt] ?: 0
                                }
                                WeekDayPill(
                                    dowLabel = pair.first.uppercase(),
                                    dateText = "%02d".format(dayDate.dayOfMonth),
                                    countText = count.toString(),
                                    selected = !showAllWeekDays && selectedDay == dayInt,
                                    isToday = dayInt == todayDow,
                                    onClick = {
                                        showAllWeekDays = false
                                        viewModel.setSelectedDay(dayInt)
                                    }
                                )
                            }
                        }
                    }

                    if (!homeError.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = colors.error.copy(alpha = 0.10f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(AppSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = homeError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.refreshHome() }) {
                                        Text("Retry", color = colors.accentPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Day-Wise Added Watchlist Only (No Trending Now, No Community Pulse)
                    if (showAllWeekDays) {
                        if (dayWiseSections.isEmpty()) {
                            item {
                                AppEmptyState(
                                    icon = Icons.Filled.Explore,
                                    title = "Your schedule is ready",
                                    description = "Add anime or Donghua from Explore to organize your day-wise watchlist, episodes, and star ratings.",
                                    actionLabel = "Browse Explore",
                                    onAction = onNavigateToDiscover
                                )
                            }
                        } else {
                            dayWiseSections.forEach { section ->
                                item(key = "${section.key}_header") {
                                    DaySectionHeader(
                                        title = section.title,
                                        count = section.items.size,
                                        isToday = section.isToday
                                    )
                                }
                                val rows = section.items.chunked(columnCount)
                                items(
                                    items = rows,
                                    key = { row -> "${section.key}_${row.joinToString("_") { it.id.toString() }}" }
                                ) { rowItems ->
                                    TrackerGridRow(
                                        rowItems = rowItems,
                                        columnCount = columnCount,
                                        savedIds = savedIds,
                                        titlePreference = preferences.titleLanguage,
                                        onSelectAnime = onSelectAnime,
                                        onOpenContext = { contextAnime = it },
                                        onOpenQuickEdit = { editingAnime = it },
                                        onDecrement = { viewModel.decrementEpisode(it) },
                                        onIncrement = { viewModel.incrementEpisode(it) },
                                        onCommitCustomEpisode = { id, ep ->
                                            viewModel.commitEpisodeProgress(id, ep).map { }
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "selected_day_header") {
                            DaySectionHeader(
                                title = if (selectedDay == todayDow) "Today • $selectedDayHeading" else selectedDayHeading,
                                count = dayAnime.size,
                                isToday = selectedDay == todayDow
                            )
                        }
                        if (dayAnime.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = colors.cardBackground.copy(alpha = 0.72f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = colors.accentPrimary.copy(alpha = 0.14f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.CalendarToday,
                                                    contentDescription = null,
                                                    tint = colors.accentPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Nothing added for $selectedDayHeading",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = "View all scheduled days or add airing shows from Explore.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = colors.accentPrimary.copy(alpha = 0.16f),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { showAllWeekDays = true }
                                        ) {
                                            Text(
                                                text = "All Days",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colors.highlight,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            val rows = dayAnime.chunked(columnCount)
                            items(
                                items = rows,
                                key = { row -> "day_${selectedDay}_${row.joinToString("_") { it.id.toString() }}" }
                            ) { rowItems ->
                                TrackerGridRow(
                                    rowItems = rowItems,
                                    columnCount = columnCount,
                                    savedIds = savedIds,
                                    titlePreference = preferences.titleLanguage,
                                    onSelectAnime = onSelectAnime,
                                    onOpenContext = { contextAnime = it },
                                    onOpenQuickEdit = { editingAnime = it },
                                    onDecrement = { viewModel.decrementEpisode(it) },
                                    onIncrement = { viewModel.incrementEpisode(it) },
                                    onCommitCustomEpisode = { id, ep ->
                                        viewModel.commitEpisodeProgress(id, ep).map { }
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    contextAnime?.let { target ->
        val liveTarget = homeAnime.firstOrNull { it.id == target.id }
            ?: watchlist.firstOrNull { it.id == target.id }
            ?: target
        AnimeCardContextMenuSheet(
            anime = liveTarget,
            titlePreference = preferences.titleLanguage,
            isOnHome = liveTarget.inHome,
            onDismiss = { contextAnime = null },
            onRemoveFromHome = {
                contextAnime = null
                pendingRemoveFromHome = liveTarget
            },
            onAddToHome = {
                viewModel.addToHome(liveTarget)
                contextAnime = null
            },
            onViewDetails = {
                contextAnime = null
                onSelectAnime(liveTarget.id)
            },
            onViewLibrary = {
                contextAnime = null
                onNavigateToMyList()
            },
            onViewCommunity = {
                contextAnime = null
                onNavigateToCommunity()
            },
            onOpenQuickEdit = {
                contextAnime = null
                editingAnime = liveTarget
            }
        )
    }

    pendingRemoveFromHome?.let { target ->
        RemoveFromHomeConfirmDialog(
            animeTitle = target.getResolvedTitle(preferences.titleLanguage),
            onDismiss = { pendingRemoveFromHome = null },
            onConfirm = {
                viewModel.removeFromHome(target.id)
                pendingRemoveFromHome = null
            }
        )
    }

    editingAnime?.let { target ->
        TrackingEditorDialog(
            anime = target,
            onDismiss = { editingAnime = null },
            onSave = { status, progress, seasonNumber, score, notes, fav ->
                viewModel.updateTracking(target.id, status, progress, score, notes, fav, seasonNumber)
                editingAnime = null
            },
            onRemove = {
                viewModel.removeFromWatchlist(target.id)
                editingAnime = null
            },
            shareReviewsWithCommunity = if (userProfile != null) {
                privacy.reviewsVisibility == VisibilityLevel.COMMUNITY
            } else null,
            onToggleShareReviews = if (userProfile != null) {
                { share ->
                    viewModel.updatePrivacySettings(
                        privacy.copy(reviewsVisibility = if (share) VisibilityLevel.COMMUNITY else VisibilityLevel.PRIVATE)
                    )
                }
            } else null
        )
    }
}

@Composable
private fun DaySectionHeader(
    title: String,
    count: Int,
    isToday: Boolean
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.accentPrimary)
                )
            }
            Text(
                text = title,
                fontFamily = CormorantSerifFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = if (isToday) colors.highlight else colors.textPrimary
            )
        }
        Surface(
            shape = CircleShape,
            color = colors.elevatedSurface.copy(alpha = 0.52f)
        ) {
            Text(
                text = "$count ${if (count == 1) "SHOW" else "SHOWS"}",
                style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                color = colors.accentPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TrackerGridRow(
    rowItems: List<AnimeEntity>,
    columnCount: Int,
    savedIds: Set<Int>,
    titlePreference: String,
    onSelectAnime: (Int) -> Unit,
    onOpenContext: (AnimeEntity) -> Unit,
    onOpenQuickEdit: (AnimeEntity) -> Unit,
    onDecrement: (Int) -> Unit,
    onIncrement: (Int) -> Unit,
    onCommitCustomEpisode: suspend (Int, Int) -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        rowItems.forEach { anime ->
            Box(modifier = Modifier.weight(1f)) {
                PosterTrackerCard(
                    anime = anime,
                    titlePreference = titlePreference,
                    isInUserList = savedIds.contains(anime.id) || anime.inWatchlist,
                    showTrackingStepper = true,
                    onClick = { onSelectAnime(anime.id) },
                    onLongClick = { onOpenContext(anime) },
                    onDecrementEpisode = { onDecrement(anime.id) },
                    onIncrementEpisode = { onIncrement(anime.id) },
                    onCommitCustomEpisode = { ep -> onCommitCustomEpisode(anime.id, ep) },
                    onOpenQuickEdit = { onOpenQuickEdit(anime) }
                )
            }
        }
        repeat(columnCount - rowItems.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeekDayPill(
    dowLabel: String,
    dateText: String?,
    countText: String?,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered) 1.045f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "week_pill_scale"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> colors.accentPrimary.copy(alpha = 0.72f)
            hovered -> colors.accentPrimary.copy(alpha = 0.45f)
            else -> colors.borderSubtle
        },
        animationSpec = tween(AppMotion.FAST_MS),
        label = "week_pill_border"
    )
    val bgBrush = if (selected) {
        Brush.verticalGradient(
            listOf(colors.accentPrimary.copy(alpha = 0.24f), colors.accentPrimary.copy(alpha = 0.10f))
        )
    } else {
        Brush.verticalGradient(
            listOf(colors.cardBackground, colors.elevatedSurface.copy(alpha = 0.65f))
        )
    }

    Surface(
        shape = shape,
        color = colors.cardBackground,
        border = BorderStroke(if (selected) 1.2.dp else 0.75.dp, borderColor),
        shadowElevation = if (hovered || selected) 6.dp else 1.dp,
        modifier = modifier
            .width(if (dateText == null) 68.dp else 60.dp)
            .height(74.dp)
            .zIndex(if (hovered) 2f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush),
            contentAlignment = Alignment.Center
        ) {
            if (dateText == null) {
                Text(
                    text = dowLabel,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    ),
                    color = if (selected) colors.highlight else colors.textPrimary
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dowLabel,
                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.2.sp),
                        color = if (selected) colors.highlight else colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = dateText,
                        style = TabularNumLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        ),
                        color = if (selected) colors.highlight else colors.textPrimary
                    )
                    if (countText != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isToday && !selected) "TODAY" else countText,
                            style = MicroUpperLabel.copy(fontSize = 7.5.sp, letterSpacing = 0.8.sp),
                            color = when {
                                selected -> colors.accentPrimary
                                isToday -> colors.success
                                else -> colors.textMuted
                            }
                        )
                    }
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.accentPrimary)
                )
            }
        }
    }
}
