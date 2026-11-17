package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CommunityReview
import com.example.model.MemberStatus
import com.example.model.SentimentCategory
import com.example.model.StarRatingUtils
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppEmptyState
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.MemberAvatar
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.ui.widgets.ObsidianTopBar
import com.example.ui.widgets.ResilientPosterImage
import com.example.ui.widgets.SentimentPill
import com.example.ui.widgets.StarRatingRow
import com.example.ui.widgets.badgeColor
import com.example.viewmodel.AnimeViewModel
import com.example.viewmodel.ReviewSortOption
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val REVIEW_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

internal fun formatCommunityTimestamp(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching {
        val instant = runCatching { Instant.parse(raw) }.getOrElse { OffsetDateTime.parse(raw).toInstant() }
        REVIEW_DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))
    }.getOrElse {
        raw.take(10)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: AnimeViewModel,
    onSelectAnime: (Int) -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val profile by viewModel.userProfile.collectAsState()
    val reviews by viewModel.communityReviews.collectAsState()
    val isLoading by viewModel.isCommunityLoading.collectAsState()
    val error by viewModel.communityError.collectAsState()
    val reviewSort by viewModel.reviewSortOption.collectAsState()

    var selectedSentimentKey by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedSentiment = remember(selectedSentimentKey) {
        SentimentCategory.entries.firstOrNull { it.key == selectedSentimentKey }
    }

    // Community ONLY displays anime entries that have BOTH a star rating AND a written note
    val ratedAndNotedReviews = remember(reviews) {
        reviews.filter { (it.rating ?: 0.0) > 0.0 && it.reviewNotes.isNotBlank() }
    }

    val filteredReviews = remember(ratedAndNotedReviews, selectedSentiment) {
        if (selectedSentiment == null) ratedAndNotedReviews
        else ratedAndNotedReviews.filter { SentimentCategory.fromRating10(it.rating) == selectedSentiment }
    }

    if (profile == null) {
        ObsidianAmbientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 104.dp)
                    .testTag("community_locked_view")
            ) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                    ObsidianTopBar(
                        title = "Community",
                        subtitle = "Rated & Noted Anime",
                        userProfile = null,
                        onOpenProfile = onNavigateToProfile
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    AppCard(
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier.widthIn(max = 460.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.accentPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = colors.accentPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Sign In for Community Reviews",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Sign in or create an account to browse rated anime showcases and written notes from your circle.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AppPrimaryButton(
                                text = "Sign In / Create Account",
                                onClick = { viewModel.exitLocalPreviewMode() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        return
    }

    if (profile?.memberStatus != MemberStatus.ACTIVE) {
        ObsidianAmbientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 104.dp)
                    .testTag("community_join_gate_view")
            ) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                    ObsidianTopBar(
                        title = "Community",
                        subtitle = "Rated & Noted Anime",
                        userProfile = profile,
                        onOpenProfile = onNavigateToProfile
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    AppCard(
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier.widthIn(max = 460.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.accentPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.GroupAdd,
                                        contentDescription = null,
                                        tint = colors.accentPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Join the Community",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Unlock rated & noted anime showcases shared by members. Your own ratings and notes stay private unless you enable sharing.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (!error.isNullOrBlank()) {
                                Surface(
                                    color = colors.error.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = error!!,
                                        color = colors.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(AppSpacing.sm)
                                    )
                                }
                            }
                            AppPrimaryButton(
                                text = if (isLoading) "Joining..." else "Join Community",
                                enabled = !isLoading,
                                onClick = { viewModel.joinCommunity() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("join_community_button")
                            )
                        }
                    }
                }
            }
        }
        return
    }

    ObsidianAmbientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("community_screen")
            ) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                    ObsidianTopBar(
                        title = "Community",
                        subtitle = "Rated & Noted • ${ratedAndNotedReviews.size} Reviews",
                        userProfile = profile,
                        onOpenProfile = onNavigateToProfile
                    )
                }

                if (!error.isNullOrBlank()) {
                    Surface(
                        color = colors.error.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = error!!,
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(AppSpacing.sm)
                        )
                    }
                }

                // Sentiment Filter Pills + Sort Capsules
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "rev_all") {
                        val active = selectedSentiment == null
                        Surface(
                            shape = CircleShape,
                            color = if (active) colors.accentPrimary.copy(alpha = 0.18f) else colors.elevatedSurface.copy(alpha = 0.65f),
                            border = if (active) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.65f)) else null,
                            modifier = Modifier
                                .height(34.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable { selectedSentimentKey = null }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                Text(
                                    text = "ALL (${ratedAndNotedReviews.size})",
                                    style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 0.8.sp),
                                    color = if (active) colors.highlight else colors.textSecondary
                                )
                            }
                        }
                    }
                    items(SentimentCategory.entries, key = { it.key }) { cat ->
                        val active = selectedSentiment == cat
                        val catColor = cat.badgeColor()
                        Surface(
                            shape = CircleShape,
                            color = if (active) catColor.copy(alpha = 0.22f) else colors.elevatedSurface.copy(alpha = 0.65f),
                            border = if (active) BorderStroke(1.dp, catColor.copy(alpha = 0.72f)) else null,
                            modifier = Modifier
                                .height(34.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable { selectedSentimentKey = if (active) null else cat.key }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 13.dp)) {
                                Text(
                                    text = cat.shortLabel.uppercase(),
                                    style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 0.8.sp),
                                    color = if (active) catColor else colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReviewSortOption.entries.forEach { opt ->
                        val active = reviewSort == opt
                        Surface(
                            shape = CircleShape,
                            color = if (active) colors.accentPrimary.copy(alpha = 0.18f) else colors.elevatedSurface.copy(alpha = 0.58f),
                            border = if (active) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.6f)) else null,
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable { viewModel.setReviewSortOption(opt) }
                        ) {
                            Text(
                                text = opt.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) colors.highlight else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                val pullRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.refreshCommunityData() },
                    state = pullRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isLoading,
                            containerColor = colors.elevatedSurface,
                            color = colors.accentPrimary,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("community_pull_to_refresh")
                ) {
                    if (filteredReviews.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(AppSpacing.xl),
                            contentAlignment = Alignment.Center
                        ) {
                            AppEmptyState(
                                icon = Icons.Filled.RateReview,
                                title = if (ratedAndNotedReviews.isEmpty()) "No rated & noted anime yet" else "No matching reviews",
                                description = if (ratedAndNotedReviews.isEmpty()) {
                                    "Rate an anime with stars and write a note to showcase it here when sharing is enabled."
                                } else {
                                    "Try clearing your sentiment filter to view all rated and noted anime."
                                }
                            )
                        }
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val columns = if (maxWidth >= 720.dp) 2 else 1
                            val chunked = remember(filteredReviews, columns) { filteredReviews.chunked(columns) }
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = AppSpacing.md,
                                    end = AppSpacing.md,
                                    top = 8.dp,
                                    bottom = 112.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = chunked,
                                    key = { row -> row.joinToString("_") { it.id } }
                                ) { rowItems ->
                                    if (columns == 1) {
                                        val firstRev = rowItems.first()
                                        Box(modifier = Modifier.fillMaxWidth().animateItem()) {
                                            EditorialCommunityNoteCard(
                                                review = firstRev,
                                                onClick = { onSelectAnime(firstRev.animeId) },
                                                onToggleLike = { viewModel.toggleReviewLike(firstRev) }
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().animateItem(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            rowItems.forEach { rev ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    EditorialCommunityNoteCard(
                                                        review = rev,
                                                        onClick = { onSelectAnime(rev.animeId) },
                                                        onToggleLike = { viewModel.toggleReviewLike(rev) }
                                                    )
                                                }
                                            }
                                            repeat(columns - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
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
}

/**
 * Editorial Anime Template Card for Community & AnimeDetail:
 * Showcases the anime poster, Cormorant title, 5-star gold rating, sentiment badge,
 * and the reviewer's editorial note without exposing watching progress.
 */
@Composable
internal fun EditorialCommunityNoteCard(
    review: CommunityReview,
    onClick: () -> Unit,
    showAnimeHeader: Boolean = true,
    onToggleLike: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val formattedDate = remember(review.updatedAt) { formatCommunityTimestamp(review.updatedAt) }
    val sentiment = remember(review.rating) { SentimentCategory.fromRating10(review.rating) }
    val starRating = remember(review.rating) { StarRatingUtils.rating10ToStars(review.rating) }

    AppCard(
        shape = RoundedCornerShape(24.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showAnimeHeader) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ResilientPosterImage(
                        imageUrl = review.animeCoverImage,
                        contentDescription = review.animeTitle,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(width = 66.dp, height = 96.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = review.animeTitle,
                            fontFamily = CormorantSerifFamily,
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (starRating > 0f) {
                            StarRatingRow(
                                stars = starRating,
                                starSize = 16.dp,
                                showNumeric = true
                            )
                        }
                        if (sentiment != null) {
                            SentimentPill(sentiment = sentiment, solid = false, fullText = true)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (starRating > 0f) {
                        StarRatingRow(stars = starRating, starSize = 16.dp, showNumeric = true)
                    }
                    if (sentiment != null) {
                        SentimentPill(sentiment = sentiment, solid = false, fullText = true)
                    }
                }
            }

            if (review.reviewNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.elevatedSurface.copy(alpha = if (colors.isDark) 0.55f else 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(colors.accentPrimary.copy(alpha = 0.65f))
                        )
                        Text(
                            text = "“${review.reviewNotes}”",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 20.sp
                            ),
                            color = colors.textPrimary.copy(alpha = 0.90f),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Minimal reviewer attribution footer + Like button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemberAvatar(
                        avatarKey = review.avatarKey,
                        avatarUrl = review.avatarUrl,
                        username = review.username,
                        size = 26.dp
                    )
                    Text(
                        text = review.username,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    if (formattedDate.isNotBlank()) {
                        Text(
                            text = "• $formattedDate",
                            style = TabularNumSmall.copy(fontSize = 10.sp),
                            color = colors.textMuted
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (review.likedByMe) colors.accentPrimary.copy(alpha = 0.2f) else colors.elevatedSurface.copy(alpha = 0.65f),
                    border = if (review.likedByMe) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.6f)) else null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = onToggleLike != null) { onToggleLike?.invoke() }
                        .testTag("like_review_${review.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (review.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (review.likedByMe) "Unlike review" else "Like review",
                            tint = if (review.likedByMe) colors.accentPrimary else colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${review.likesCount}",
                            style = TabularNumSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = if (review.likedByMe) colors.highlight else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
