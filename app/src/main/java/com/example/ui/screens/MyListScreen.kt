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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AnimeEntity
import com.example.model.AnimeCollection
import com.example.model.SentimentCategory
import com.example.model.VisibilityLevel
import com.example.model.WatchStatus
import com.example.model.WatchlistLayoutMode
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AnimeCardContextMenuSheet
import com.example.ui.widgets.AnimeListRowCard
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppEmptyState
import com.example.ui.widgets.AppIconButton
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.AppSearchField
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.ui.widgets.ObsidianTopBar
import com.example.ui.widgets.PosterTrackerCard
import com.example.ui.widgets.RemoveFromHomeConfirmDialog
import com.example.ui.widgets.TrackingEditorDialog
import com.example.ui.widgets.badgeColor
import com.example.viewmodel.AnimeViewModel

private enum class MyListSort(val label: String) {
    RECENT("Recent"),
    TITLE("Title"),
    RATING("Rating"),
    PROGRESS("Progress")
}

@Composable
fun MyListScreen(
    viewModel: AnimeViewModel,
    onSelectAnime: (Int) -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val watchlist by viewModel.watchlist.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val privacy by viewModel.privacySettings.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val statusTabs = remember { listOf<WatchStatus?>(null) + WatchStatus.entries }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedSentimentKey by rememberSaveable { mutableStateOf<String?>(null) }
    var filterText by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(MyListSort.RECENT) }

    var editingAnime by remember { mutableStateOf<AnimeEntity?>(null) }
    var contextAnime by remember { mutableStateOf<AnimeEntity?>(null) }
    var pendingRemoveFromHome by remember { mutableStateOf<AnimeEntity?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var inspectingCollection by remember { mutableStateOf<AnimeCollection?>(null) }
    var editingCollection by remember { mutableStateOf<AnimeCollection?>(null) }
    var pendingDeleteCollection by remember { mutableStateOf<AnimeCollection?>(null) }

    val selectedStatus = statusTabs[selectedTabIndex]
    val selectedSentiment = remember(selectedSentimentKey) {
        SentimentCategory.entries.firstOrNull { it.key == selectedSentimentKey }
    }

    val filteredAnime = remember(watchlist, selectedStatus, selectedSentiment, filterText, sortMode, preferences.titleLanguage) {
        watchlist.filter { anime ->
            val matchesStatus = selectedStatus == null || anime.watchStatusEnum == selectedStatus
            val matchesSentiment = selectedSentiment == null || SentimentCategory.fromScore100(anime.userScore) == selectedSentiment
            val title = anime.getResolvedTitle(preferences.titleLanguage)
            val matchesQuery = filterText.isBlank() || com.example.util.EnglishTitleMapper.matchesAllKeywords(
                filterText,
                title,
                anime.titleEnglish,
                anime.titleRomaji,
                anime.titlePinyin,
                anime.genres
            )
            matchesStatus && matchesSentiment && matchesQuery
        }.let { list ->
            when (sortMode) {
                MyListSort.RECENT -> list.sortedWith(compareByDescending<AnimeEntity> { it.isPinned }.thenByDescending { it.lastWatchedTime ?: 0L })
                MyListSort.TITLE -> list.sortedBy { it.getResolvedTitle(preferences.titleLanguage).lowercase() }
                MyListSort.RATING -> list.sortedByDescending { it.userScore ?: 0 }
                MyListSort.PROGRESS -> list.sortedByDescending { it.watchedEpisodeCount }
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
                    .testTag("mylist_screen")
            ) {
                // Header
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                    ObsidianTopBar(
                        title = "Library",
                        subtitle = "${watchlist.size} Tracked • ${collections.size} Collections",
                        userProfile = profile,
                        onOpenProfile = onNavigateToProfile,
                        actions = {
                            if (profile != null) {
                                AppIconButton(
                                    icon = Icons.Filled.CollectionsBookmark,
                                    contentDescription = "Create collection",
                                    onClick = { showCreateCollectionDialog = true },
                                    modifier = Modifier.testTag("create_collection_button")
                                )
                            }
                            AppIconButton(
                                icon = if (preferences.watchlistLayout == WatchlistLayoutMode.GRID) {
                                    Icons.Filled.ViewList
                                } else {
                                    Icons.Filled.GridView
                                },
                                contentDescription = "Toggle list layout",
                                onClick = {
                                    val next = if (preferences.watchlistLayout == WatchlistLayoutMode.GRID) {
                                        WatchlistLayoutMode.LIST
                                    } else {
                                        WatchlistLayoutMode.GRID
                                    }
                                    viewModel.setWatchlistLayout(next)
                                }
                            )
                        }
                    )
                }

                // Status Tabs (Capsule Pills)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(statusTabs) { idx, st ->
                        val count = if (st == null) watchlist.size else watchlist.count { it.watchStatusEnum == st }
                        val selected = selectedTabIndex == idx
                        val bgBrush = if (selected) {
                            Brush.linearGradient(listOf(colors.accentPrimary.copy(alpha = 0.22f), colors.accentPrimary.copy(alpha = 0.08f)))
                        } else {
                            Brush.linearGradient(listOf(colors.elevatedSurface.copy(alpha = 0.52f), colors.cardBackground.copy(alpha = 0.72f)))
                        }
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .background(bgBrush)
                                .clickable { selectedTabIndex = idx }
                                .padding(horizontal = 14.dp)
                                .testTag("mylist_tab_${st?.dbValue ?: "all"}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = (st?.label ?: "All").uppercase(),
                                    style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 0.8.sp),
                                    color = if (selected) colors.highlight else colors.textSecondary
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = colors.elevatedSurface
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = TabularNumSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = if (selected) colors.accentPrimary else colors.textMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Sentiment Rating Filter Strip (All Ratings, Cinema, Worth, Avg, Boring, Waste)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(key = "sentiment_all") {
                        val active = selectedSentiment == null
                        Surface(
                            shape = CircleShape,
                            color = if (active) colors.accentPrimary.copy(alpha = 0.18f) else colors.elevatedSurface.copy(alpha = 0.65f),
                            modifier = Modifier
                                .height(31.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable { selectedSentimentKey = null }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text(
                                    text = "ALL RATINGS",
                                    style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.7.sp),
                                    color = if (active) colors.highlight else colors.textSecondary
                                )
                            }
                        }
                    }
                    items(SentimentCategory.entries, key = { it.key }) { cat ->
                        val active = selectedSentiment == cat
                        val catColor = cat.badgeColor()
                        val cnt = watchlist.count { SentimentCategory.fromScore100(it.userScore) == cat }
                        Surface(
                            shape = CircleShape,
                            color = if (active) catColor.copy(alpha = 0.22f) else colors.elevatedSurface.copy(alpha = 0.65f),
                            border = if (active) BorderStroke(1.dp, catColor.copy(alpha = 0.65f)) else null,
                            modifier = Modifier
                                .height(31.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable { selectedSentimentKey = if (active) null else cat.key }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 11.dp)
                            ) {
                                Text(
                                    text = cat.shortLabel.uppercase(),
                                    style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.7.sp),
                                    color = catColor
                                )
                                Text(
                                    text = cnt.toString(),
                                    style = TabularNumSmall.copy(fontSize = 9.sp),
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppSearchField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = "Filter your collection..."
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MyListSort.entries) { opt ->
                            FilterChip(
                                selected = sortMode == opt,
                                onClick = { sortMode = opt },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentPrimary.copy(alpha = 0.18f),
                                    selectedLabelColor = colors.highlight
                                ),
                                label = { Text("Sort: ${opt.label}", style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }

                val isGrid = preferences.watchlistLayout == WatchlistLayoutMode.GRID
                LazyVerticalGrid(
                    columns = if (isGrid) GridCells.Adaptive(minSize = 164.dp) else GridCells.Fixed(1),
                    contentPadding = PaddingValues(
                        start = AppSpacing.md,
                        end = AppSpacing.md,
                        top = 6.dp,
                        bottom = 112.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (profile != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Collections",
                                            fontFamily = CormorantSerifFamily,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary
                                        )
                                        if (collections.isNotEmpty()) {
                                            Text(
                                                text = "Long-press to delete",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textMuted,
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }
                                    }
                                    TextButton(onClick = { showCreateCollectionDialog = true }) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accentPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("NEW", style = MicroUpperLabel, color = colors.accentPrimary)
                                    }
                                }
                                if (collections.isEmpty()) {
                                    AppCard(shape = RoundedCornerShape(22.dp)) {
                                        Text(
                                            text = "Create custom collections such as \"Absolute Cinema\" or \"Donghua Favorites\".",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(AppSpacing.md)
                                        )
                                    }
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                        items(collections, key = { it.id }) { col ->
                                            AppCard(
                                                shape = RoundedCornerShape(22.dp),
                                                onClick = { inspectingCollection = col },
                                                onLongClick = { pendingDeleteCollection = col },
                                                modifier = Modifier
                                                    .width(206.dp)
                                                    .testTag("collection_card_${col.id}")
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(AppSpacing.sm),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = col.name,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = colors.textPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Icon(
                                                            imageVector = if (col.visibility == VisibilityLevel.COMMUNITY) Icons.Filled.Public else Icons.Filled.Lock,
                                                            contentDescription = col.visibility.label,
                                                            modifier = Modifier.size(15.dp),
                                                            tint = colors.accentPrimary
                                                        )
                                                    }
                                                    Text(
                                                        text = "${col.animeIds.size} entries • ${col.visibility.label}",
                                                        style = TabularNumSmall,
                                                        color = colors.textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (filteredAnime.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppSpacing.xl),
                                contentAlignment = Alignment.Center
                            ) {
                                AppEmptyState(
                                    icon = Icons.Filled.Bookmarks,
                                    title = "No anime in ${selectedStatus?.label ?: "Library"}",
                                    description = if (filterText.isNotBlank() || selectedSentiment != null) {
                                        "No entries matched your current filter."
                                    } else {
                                        "Explore the catalog to add anime & Donghua to your collection."
                                    },
                                    actionLabel = "Browse Explore",
                                    onAction = onNavigateToDiscover
                                )
                            }
                        }
                    } else {
                        items(filteredAnime, key = { it.id }) { anime ->
                            if (isGrid) {
                                PosterTrackerCard(
                                    anime = anime,
                                    titlePreference = preferences.titleLanguage,
                                    isInUserList = true,
                                    showTrackingStepper = true,
                                    onClick = { onSelectAnime(anime.id) },
                                    onLongClick = { contextAnime = anime },
                                    onDecrementEpisode = { viewModel.decrementEpisode(anime.id) },
                                    onIncrementEpisode = { viewModel.incrementEpisode(anime.id) },
                                    onCommitCustomEpisode = { ep ->
                                        viewModel.commitEpisodeProgress(anime.id, ep).map { }
                                    },
                                    onOpenQuickEdit = { editingAnime = anime },
                                    modifier = Modifier.animateItem()
                                )
                            } else {
                                AnimeListRowCard(
                                    anime = anime,
                                    titlePreference = preferences.titleLanguage,
                                    onClick = { onSelectAnime(anime.id) },
                                    onLongClick = { contextAnime = anime },
                                    onIncrementEpisode = { viewModel.incrementEpisode(anime.id) },
                                    onEditEntry = { editingAnime = anime },
                                    onToggleFavorite = { viewModel.toggleFavorite(anime.id, !anime.isFavorite) },
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
        val liveTarget = watchlist.firstOrNull { it.id == target.id } ?: target
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
            shareReviewsWithCommunity = if (profile != null) {
                privacy.reviewsVisibility == VisibilityLevel.COMMUNITY
            } else null,
            onToggleShareReviews = if (profile != null) {
                { share ->
                    viewModel.updatePrivacySettings(
                        privacy.copy(reviewsVisibility = if (share) VisibilityLevel.COMMUNITY else VisibilityLevel.PRIVATE)
                    )
                }
            } else null
        )
    }

    if (showCreateCollectionDialog) {
        CollectionEditorDialog(
            initialName = "",
            initialDescription = "",
            initialVisibility = VisibilityLevel.PRIVATE,
            title = "New Collection",
            onDismiss = { showCreateCollectionDialog = false },
            onConfirm = { name, desc, vis ->
                viewModel.createCollection(name, desc, vis)
                showCreateCollectionDialog = false
            }
        )
    }

    editingCollection?.let { col ->
        CollectionEditorDialog(
            initialName = col.name,
            initialDescription = col.description,
            initialVisibility = col.visibility,
            title = "Edit Collection",
            onDismiss = { editingCollection = null },
            onConfirm = { name, _, vis ->
                viewModel.renameOrUpdateCollection(col.id, name, vis)
                editingCollection = null
            }
        )
    }

    inspectingCollection?.let { col ->
        val freshCol = collections.firstOrNull { it.id == col.id } ?: col
        val memberAnime = watchlist.filter { freshCol.animeIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { inspectingCollection = null },
            shape = RoundedCornerShape(AppRadius.xl),
            containerColor = colors.backgroundSecondary,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(freshCol.name, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        inspectingCollection = null
                        editingCollection = freshCol
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename collection", tint = colors.textSecondary)
                    }
                    IconButton(onClick = {
                        viewModel.deleteCollection(freshCol.id)
                        inspectingCollection = null
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete collection", tint = colors.error)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Text("Visibility: ${freshCol.visibility.label}", style = MaterialTheme.typography.labelMedium, color = colors.accentPrimary)
                    if (freshCol.description.isNotBlank()) {
                        Text(freshCol.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    if (memberAnime.isEmpty()) {
                        Text(
                            "No anime added yet. Open any anime detail page and tap \"Add to Collection\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    } else {
                        memberAnime.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.getResolvedTitle(preferences.titleLanguage),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            inspectingCollection = null
                                            onSelectAnime(item.id)
                                        }
                                )
                                TextButton(onClick = { viewModel.removeAnimeFromCollection(freshCol.id, item.id) }) {
                                    Text("Remove", color = colors.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingCollection = null }) { Text("Close", color = colors.accentPrimary) }
            }
        )
    }

    pendingDeleteCollection?.let { targetCol ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCollection = null },
            shape = RoundedCornerShape(AppRadius.xl),
            containerColor = colors.backgroundSecondary,
            title = {
                Text(
                    text = "Delete Category?",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Delete \"${targetCol.name}\"? Your tracked anime entries in Library will not be removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCollection(targetCol.id)
                        if (inspectingCollection?.id == targetCol.id) {
                            inspectingCollection = null
                        }
                        pendingDeleteCollection = null
                    },
                    modifier = Modifier.testTag("confirm_delete_collection_button")
                ) {
                    Text("Delete", color = colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCollection = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun CollectionEditorDialog(
    initialName: String,
    initialDescription: String,
    initialVisibility: VisibilityLevel,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, VisibilityLevel) -> Unit
) {
    val colors = AppTheme.colors
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var visibility by remember { mutableStateOf(initialVisibility) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(AppRadius.xl),
        containerColor = colors.backgroundSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Collection Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(AppRadius.md),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    maxLines = 3,
                    shape = RoundedCornerShape(AppRadius.md),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    VisibilityLevel.entries.forEach { vis ->
                        FilterChip(
                            selected = visibility == vis,
                            onClick = { visibility = vis },
                            shape = CircleShape,
                            label = { Text(vis.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppPrimaryButton(
                text = "Save",
                onClick = { onConfirm(name, description, visibility) },
                enabled = name.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) }
        }
    )
}
