package com.example.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.example.data.local.AnimeEntity
import com.example.model.ProgressRules
import com.example.model.SeasonRules
import com.example.model.SentimentCategory
import com.example.model.StarRatingUtils
import com.example.model.WatchStatus
import com.example.ui.components.formatCompactCountdown
import com.example.ui.theme.AppMotion
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneOnGold
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.ObsidianAmber
import com.example.ui.theme.ObsidianCrimson
import com.example.ui.theme.ObsidianEmerald
import com.example.ui.theme.StatusCompletedColor
import com.example.ui.theme.StatusDroppedColor
import com.example.ui.theme.StatusOnHoldColor
import com.example.ui.theme.StatusPlanToWatchColor
import com.example.ui.theme.StatusWatchingColor
import com.example.ui.theme.TabularNumMedium
import com.example.ui.theme.TabularNumSmall
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val POSTER_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

fun WatchStatus.badgeColor(): Color = when (this) {
    WatchStatus.WATCHING -> StatusWatchingColor
    WatchStatus.COMPLETED -> StatusCompletedColor
    WatchStatus.PLAN_TO_WATCH -> StatusPlanToWatchColor
    WatchStatus.ON_HOLD -> StatusOnHoldColor
    WatchStatus.DROPPED -> StatusDroppedColor
}

@Composable
fun SentimentCategory.badgeColor(): Color {
    val colors = AppTheme.colors
    return when (this) {
        SentimentCategory.CINEMA -> colors.accentPrimary
        SentimentCategory.WORTH -> colors.success
        SentimentCategory.AVG -> if (colors.isDark) Color(0xFF9EA6BA) else Color(0xFF4C5366)
        SentimentCategory.BORING -> colors.warning
        SentimentCategory.WASTE -> colors.error
    }
}

@Composable
fun SentimentCategory.textOnSolidBadge(): Color {
    val colors = AppTheme.colors
    return when (this) {
        SentimentCategory.CINEMA -> colors.onAccentPrimary
        SentimentCategory.WORTH -> if (colors.isDark) Color(0xFF042614) else Color.White
        SentimentCategory.AVG -> if (colors.isDark) Color(0xFF0C0E14) else Color.White
        SentimentCategory.BORING -> if (colors.isDark) Color(0xFF2B1906) else Color.White
        SentimentCategory.WASTE -> Color.White
    }
}

@Composable
fun WatchStatusPill(status: WatchStatus, modifier: Modifier = Modifier) {
    val color = status.badgeColor()
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Text(
            text = status.label.uppercase(),
            style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.6.sp),
            color = color,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
        )
    }
}

@Composable
fun SentimentPill(
    sentiment: SentimentCategory,
    solid: Boolean = false,
    fullText: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = if (solid) sentiment.badgeColor().copy(alpha = 0.92f) else sentiment.badgeColor().copy(alpha = 0.15f)
    val fgColor = if (solid) sentiment.textOnSolidBadge() else sentiment.badgeColor()
    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = (if (fullText) sentiment.fullLabel else sentiment.shortLabel).uppercase(),
            style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.7.sp, fontWeight = FontWeight.ExtraBold),
            color = fgColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
        )
    }
}

/**
 * Editorial 2:3 Poster Tracker Card matching the HTML prototype (.poster-card) with:
 * - Poster with bottom scrim, top-left sentiment badge, top-right star score, bottom-left time, bottom-right EP badge
 * - 2-line title, thin progress bar, interactive - / watched/total / + stepper
 * - Next EP countdown or Completed banner, plus compact Rate & Note pills
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterTrackerCard(
    anime: AnimeEntity,
    titlePreference: String = "ENGLISH",
    isInUserList: Boolean = anime.inWatchlist,
    showTrackingStepper: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDecrementEpisode: (() -> Unit)? = null,
    onIncrementEpisode: (() -> Unit)? = null,
    onCommitCustomEpisode: (suspend (Int) -> Result<Unit>)? = null,
    onOpenQuickEdit: (() -> Unit)? = null,
    onQuickAdd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val title = anime.getResolvedTitle(titlePreference)
    val total = anime.totalEpisodes
    val sentiment = SentimentCategory.fromScore100(anime.userScore)
    val isCompleted = isInUserList && (
        anime.watchStatusEnum == WatchStatus.COMPLETED ||
            ProgressRules.shouldAutoComplete(anime.status, total, anime.watchedEpisodeCount)
    )
    val canIncrement = !isInUserList || total == null || total <= 0 || anime.watchedEpisodeCount < total
    val canDecrement = isInUserList && anime.watchedEpisodeCount > 0

    val nowSeconds = remember { System.currentTimeMillis() / 1000 }
    val nextTime = anime.getEffectiveAiringTime(nowSeconds)
    val nextEpNum = anime.getEffectiveNextEpisodeNumber(nowSeconds)
    val airClockText = remember(anime.airingTimeUnix) {
        anime.airingTimeUnix?.takeIf { it > 0L }?.let { unix ->
            runCatching {
                Instant.ofEpochSecond(unix).atZone(ZoneId.systemDefault()).format(POSTER_TIME_FORMATTER)
            }.getOrNull()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val cardScale by animateFloatAsState(
        targetValue = when {
            reducedMotion -> 1f
            pressed -> 0.97f
            hovered -> 1.055f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "poster_card_scale"
    )
    val cardElevation by animateDpAsState(
        targetValue = when {
            hovered -> 18.dp
            colors.isDark -> 2.dp
            else -> 5.dp
        },
        animationSpec = tween(AppMotion.FAST_MS),
        label = "poster_card_elevation"
    )
    val rimColor by animateColorAsState(
        targetValue = if (hovered) colors.accentPrimary.copy(alpha = 0.80f) else colors.borderSubtle,
        animationSpec = tween(AppMotion.FAST_MS),
        label = "poster_card_rim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (hovered) 4f else 0f)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .hoverable(interactionSource = interactionSource)
            .testTag("anime_grid_card_${anime.id}")
    ) {
        // 2:3 Seamless Rounded Poster Frame with Hover Lift & Rim Glow
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardBackground,
            border = BorderStroke(if (hovered) 1.6.dp else 0.75.dp, rimColor),
            shadowElevation = cardElevation,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .pointerHoverIcon(PointerIcon.Hand)
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ResilientPosterImage(
                    imageUrl = anime.coverImage,
                    contentDescription = title,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom editorial gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.38f to Color.Transparent,
                                0.72f to Color.Black.copy(alpha = 0.52f),
                                1f to Color.Black.copy(alpha = 0.92f)
                            )
                        )
                )

                // Top-left Sentiment pill
                if (sentiment != null) {
                    SentimentPill(
                        sentiment = sentiment,
                        solid = true,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(9.dp)
                    )
                }

                // Top-right 5-Star Rating pill
                val userStars = StarRatingUtils.score100ToStars(anime.userScore)
                val catalogStars = StarRatingUtils.score100ToStars(anime.score)
                val displayStars = if (userStars > 0f) userStars else catalogStars
                if (displayStars > 0f) {
                    RatingBadge(
                        scoreText = StarRatingUtils.formatStars(displayStars),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(9.dp)
                    )
                }

                // Bottom-left Air time (if airing) or Year (crisp tabular font)
                val bottomLeftLabel = airClockText ?: anime.seasonYear?.toString()
                if (!bottomLeftLabel.isNullOrBlank()) {
                    Text(
                        text = bottomLeftLabel,
                        style = TabularNumSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 11.dp, vertical = 9.dp)
                    )
                }

                // Bottom-right EP badge or Format pill
                val epBadge = when {
                    isInUserList -> "EP ${maxOf(anime.watchedEpisodeCount, anime.currentEpisode)}"
                    anime.currentEpisode > 1 -> "EP ${anime.currentEpisode}"
                    else -> anime.formatLabel()
                }
                Surface(
                    shape = CircleShape,
                    color = colors.accentPrimary.copy(alpha = 0.96f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(9.dp)
                ) {
                    Text(
                        text = epBadge,
                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.ExtraBold),
                        color = colors.onAccentPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                    )
                }
            }
        }

        // Info & Fluid Tracker below poster (no boxy outlines)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, lineHeight = 18.sp),
                color = if (hovered) colors.accentPrimary else colors.textPrimary,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = onClick)
            )

            if (isInUserList && showTrackingStepper) {
                val progress = if (total != null && total > 0) {
                    (anime.watchedEpisodeCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                } else if (anime.watchedEpisodeCount > 0) 0.5f else 0.04f

                AppProgressBar(
                    progress = progress,
                    isCompleted = isCompleted,
                    heightDp = 3
                )

                // Capsule Stepper with inline editable custom episode input
                InlineEpisodeNumberStepper(
                    animeId = anime.id,
                    watchedEpisodeCount = anime.watchedEpisodeCount,
                    totalEpisodes = total,
                    canDecrement = canDecrement,
                    canIncrement = canIncrement,
                    compact = true,
                    onDecrement = onDecrementEpisode,
                    onIncrement = onIncrementEpisode,
                    onCommitCustomEpisode = onCommitCustomEpisode,
                    onFallbackTap = { onOpenQuickEdit?.invoke() ?: onClick() }
                )

                // Soft Next EP / Completed pill
                if (isCompleted) {
                    Surface(
                        shape = CircleShape,
                        color = colors.success.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ COMPLETED",
                            style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.sp),
                            color = colors.success,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                } else if (nextTime != null && nextTime > nowSeconds && nextEpNum != null) {
                    Surface(
                        shape = CircleShape,
                        color = colors.accentPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EP $nextEpNum",
                                style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.6.sp),
                                color = colors.accentPrimary
                            )
                            Text(
                                text = formatCompactCountdown(nextTime - nowSeconds),
                                style = TabularNumSmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                color = colors.highlight
                            )
                        }
                    }
                } else {
                    Text(
                        text = anime.watchStatusEnum.label.uppercase(),
                        style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 1.1.sp),
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Compact Star Rate / Note pills
                if (onOpenQuickEdit != null) {
                    val hasUserScore = (anime.userScore ?: 0) > 0
                    val hasNote = !anime.userNotes.isNullOrBlank()
                    val starVal = StarRatingUtils.score100ToStars(anime.userScore)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (hasUserScore) colors.accentPrimary.copy(alpha = 0.16f) else colors.elevatedSurface.copy(alpha = 0.72f),
                            modifier = Modifier
                                .weight(1f)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable { onOpenQuickEdit() }
                        ) {
                            Text(
                                text = if (hasUserScore) "★ ${StarRatingUtils.formatStars(starVal)}" else "☆ Rate",
                                style = TabularNumSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                                color = if (hasUserScore) colors.highlight else colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (hasNote) colors.accentPrimary.copy(alpha = 0.16f) else colors.elevatedSurface.copy(alpha = 0.72f),
                            modifier = Modifier
                                .weight(1f)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable { onOpenQuickEdit() }
                        ) {
                            Text(
                                text = if (hasNote) "✎ Noted" else "✎ Note",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.2.sp),
                                color = if (hasNote) colors.highlight else colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            } else {
                // Discover / Non-watchlist clean footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isInUserList) {
                            "${anime.watchedEpisodeCount}/${total?.takeIf { it > 0 } ?: "∞"}"
                        } else {
                            listOfNotNull(anime.formatLabel(), total?.takeIf { it > 0 }?.let { "$it ep" }).joinToString(" • ")
                        },
                        style = TabularNumSmall.copy(fontSize = 10.5.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (onQuickAdd != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (isInUserList) colors.accentPrimary.copy(alpha = 0.16f) else colors.elevatedSurface.copy(alpha = 0.86f),
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable(enabled = !isInUserList || canIncrement, onClick = onQuickAdd)
                                .testTag("anime_quick_action_${anime.id}")
                        ) {
                            Text(
                                text = when {
                                    !isInUserList -> "+ Add"
                                    canIncrement -> "+1 Ep"
                                    else -> "✓ Done"
                                },
                                style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.6.sp),
                                color = if (isInUserList) colors.accentPrimary else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeGridCard(
    anime: AnimeEntity,
    titlePreference: String = "ENGLISH",
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onQuickAddOrIncrement: (() -> Unit)? = null,
    isInUserList: Boolean = anime.inWatchlist,
    modifier: Modifier = Modifier
) {
    PosterTrackerCard(
        anime = anime,
        titlePreference = titlePreference,
        isInUserList = isInUserList,
        showTrackingStepper = false,
        onClick = onClick,
        onLongClick = onLongClick,
        onQuickAdd = onQuickAddOrIncrement,
        modifier = modifier
    )
}

/**
 * Editorial Library Row matching the HTML .lib-item layout:
 * 48x68 poster, title, EP X/Y • ★ score, Sentiment & WatchStatus pills, +1 Ep / Edit actions
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeListRowCard(
    anime: AnimeEntity,
    titlePreference: String = "ENGLISH",
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onIncrementEpisode: (() -> Unit)? = null,
    onEditEntry: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val title = anime.getResolvedTitle(titlePreference)
    val status = anime.watchStatusEnum
    val total = anime.totalEpisodes
    val sentiment = SentimentCategory.fromScore100(anime.userScore)
    val canIncrement = total == null || total <= 0 || anime.watchedEpisodeCount < total

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val rowScale by animateFloatAsState(
        targetValue = when {
            reducedMotion -> 1f
            pressed -> 0.985f
            hovered -> 1.022f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow),
        label = "list_card_scale"
    )
    val elevation by animateDpAsState(
        targetValue = when {
            hovered -> 14.dp
            colors.isDark -> 2.dp
            else -> 4.dp
        },
        animationSpec = tween(AppMotion.FAST_MS),
        label = "list_card_elevation"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            hovered -> colors.accentPrimary.copy(alpha = 0.72f)
            anime.isPinned -> colors.accentPrimary.copy(alpha = 0.42f)
            else -> colors.borderSubtle
        },
        animationSpec = tween(AppMotion.FAST_MS),
        label = "list_card_border"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (anime.isPinned) colors.accentPrimary.copy(alpha = 0.10f) else colors.cardBackground,
        border = BorderStroke(if (hovered) 1.2.dp else 0.75.dp, borderColor),
        shadowElevation = elevation,
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (hovered) 2f else 0f)
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("anime_list_card_${anime.id}")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResilientPosterImage(
                    imageUrl = anime.coverImage,
                    contentDescription = title,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.width(52.dp).height(74.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (anime.isPinned) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = colors.accentPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    val starScore = StarRatingUtils.score100ToStars(anime.userScore)
                    val ratingText = if (starScore > 0f) "★ ${StarRatingUtils.formatStars(starScore)}" else "Unrated"
                    val seasonPrefix = if (anime.effectiveSeasonNumber > 1) "S${anime.effectiveSeasonNumber} • " else ""
                    Text(
                        text = "${seasonPrefix}EP ${anime.watchedEpisodeCount}/${total?.takeIf { it > 0 } ?: "∞"} • $ratingText",
                        style = TabularNumSmall.copy(fontSize = 11.sp),
                        color = colors.textSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (sentiment != null) {
                            SentimentPill(sentiment = sentiment, solid = false, fullText = true)
                        } else {
                            WatchStatusPill(status = status)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onIncrementEpisode != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (canIncrement) colors.accentPrimary.copy(alpha = 0.16f) else colors.elevatedSurface,
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .clickable(enabled = canIncrement, onClick = onIncrementEpisode)
                                .testTag("increment_ep_btn_${anime.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text(
                                    text = if (canIncrement) "+1" else "✓",
                                    style = TabularNumSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (canIncrement) colors.highlight else colors.success
                                )
                            }
                        }
                    }
                    if (onEditEntry != null) {
                        Surface(
                            shape = CircleShape,
                            color = colors.elevatedSurface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onEditEntry)
                                .testTag("edit_entry_btn_${anime.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 11.dp)) {
                                Text("Rate", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                            }
                        }
                    }
                    if (onToggleFavorite != null) {
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (anime.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (anime.isFavorite) "Remove favorite" else "Favorite",
                                tint = if (anime.isFavorite) colors.error else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            if (total != null && total > 0) {
                val fraction = (anime.watchedEpisodeCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                Box(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                    AppProgressBar(progress = fraction, isCompleted = status == WatchStatus.COMPLETED, heightDp = 2)
                }
            }
        }
    }
}

private val REVIEW_TAG_PROMPTS = listOf(
    "Peak Animation",
    "Incredible OST",
    "Emotional Story",
    "Must Watch",
    "Slow Pacing"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TrackingEditorDialog(
    anime: AnimeEntity,
    onDismiss: () -> Unit,
    onSave: (status: WatchStatus, progress: Int, seasonNumber: Int, scoreTenths: Int?, notes: String, isFavorite: Boolean) -> Unit,
    onRemove: (() -> Unit)? = null,
    shareReviewsWithCommunity: Boolean? = null,
    onToggleShareReviews: ((Boolean) -> Unit)? = null
) {
    val colors = AppTheme.colors
    var selectedStatus by remember(anime) { mutableStateOf(anime.watchStatusEnum) }
    var progress by remember(anime) { mutableIntStateOf(anime.watchedEpisodeCount) }
    var progressText by remember(anime) { mutableStateOf(anime.watchedEpisodeCount.toString()) }
    var stars by remember(anime) { mutableFloatStateOf(StarRatingUtils.score100ToStars(anime.userScore)) }
    var notes by remember(anime) { mutableStateOf(anime.userNotes ?: "") }
    var favorite by remember(anime) { mutableStateOf(anime.isFavorite) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val applyEpisode: (Int) -> Unit = { target ->
        val clamped = ProgressRules.clampEpisodeProgress(target, anime.totalEpisodes)
        progress = clamped
        progressText = clamped.toString()
        if (selectedStatus != WatchStatus.DROPPED && ProgressRules.shouldAutoComplete(anime.status, anime.totalEpisodes, clamped)) {
            selectedStatus = WatchStatus.COMPLETED
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundSecondary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
                .testTag("rating_review_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Editorial Header: Poster + Title + Favorite Heart
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ResilientPosterImage(
                    imageUrl = anime.coverImage,
                    contentDescription = anime.displayTitle,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(width = 56.dp, height = 80.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "RATING & REVIEW STUDIO",
                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.8.sp),
                        color = colors.accentPrimary
                    )
                    Text(
                        text = anime.displayTitle,
                        fontFamily = CormorantSerifFamily,
                        fontSize = 23.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.textPrimary
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (favorite) colors.error.copy(alpha = 0.14f) else colors.elevatedSurface.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { favorite = !favorite }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (favorite) "Favorited" else "Mark as Favorite",
                            tint = if (favorite) colors.error else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Watch Status Capsules
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WATCH STATUS", style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.6.sp), color = colors.textSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WatchStatus.entries.forEach { st ->
                        val selected = selectedStatus == st
                        Surface(
                            shape = CircleShape,
                            color = if (selected) colors.accentPrimary.copy(alpha = 0.2f) else colors.elevatedSurface.copy(alpha = 0.45f),
                            border = if (selected) BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.65f)) else null,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    selectedStatus = st
                                    if (st == WatchStatus.COMPLETED && (anime.totalEpisodes ?: 0) > 0) {
                                        applyEpisode(anime.totalEpisodes!!)
                                    }
                                }
                        ) {
                            Text(
                                text = st.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) colors.highlight else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Episode Progress Module (Typed custom number + - / +)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.elevatedSurface.copy(alpha = 0.48f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EPISODE PROGRESS",
                            style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 1.4.sp),
                            color = colors.textSecondary
                        )
                        Text(
                            text = "of ${anime.totalEpisodes?.takeIf { it > 0 }?.toString() ?: "∞"}",
                            style = TabularNumSmall,
                            color = colors.textMuted
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconButton(
                            icon = Icons.Filled.Remove,
                            contentDescription = "Decrease episode",
                            onClick = { applyEpisode(progress - 1) },
                            containerColor = colors.cardBackground
                        )
                        OutlinedTextField(
                            value = progressText,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(5)
                                if (digits.isEmpty()) {
                                    progressText = ""
                                    progress = 0
                                } else {
                                    val parsed = digits.toIntOrNull() ?: 0
                                    val clamped = ProgressRules.clampEpisodeProgress(parsed, anime.totalEpisodes)
                                    progress = clamped
                                    progressText = clamped.toString()
                                    if (anime.totalEpisodes != null && anime.totalEpisodes > 0 && clamped >= anime.totalEpisodes) {
                                        selectedStatus = WatchStatus.COMPLETED
                                    } else if (clamped > 0 && selectedStatus == WatchStatus.PLAN_TO_WATCH) {
                                        selectedStatus = WatchStatus.WATCHING
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = TabularNumMedium.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.cardBackground,
                                unfocusedContainerColor = colors.cardBackground,
                                focusedBorderColor = colors.accentPrimary,
                                unfocusedBorderColor = colors.borderSubtle
                            ),
                            modifier = Modifier
                                .width(84.dp)
                                .testTag("editor_episode_input")
                        )
                        AppIconButton(
                            icon = Icons.Filled.Add,
                            contentDescription = "Increase episode",
                            onClick = { applyEpisode(progress + 1) },
                            tint = colors.onAccentPrimary,
                            containerColor = colors.accentPrimary
                        )
                    }
                }
            }

            // Interactive 5-Star Rating Studio
            InteractiveStarRatingSelector(
                stars = stars,
                onStarsChanged = { stars = it },
                onClear = { stars = 0f }
            )

            // Editorial Review / Note Composer
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(14.dp))
                        Text(
                            text = "REVIEW & NOTE",
                            style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.6.sp),
                            color = colors.accentPrimary
                        )
                    }
                    Text(
                        text = "${notes.trim().length} chars",
                        style = TabularNumSmall.copy(fontSize = 10.sp),
                        color = colors.textMuted
                    )
                }

                // Quick review highlight tags
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    REVIEW_TAG_PROMPTS.forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = colors.elevatedSurface.copy(alpha = 0.48f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    notes = if (notes.isBlank()) tag else "${notes.trimEnd()} • $tag"
                                }
                        ) {
                            Text(
                                text = "+ $tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = {
                        Text(
                            text = "Share your thoughts on direction, pacing, animation, or standout moments...",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.cardBackground.copy(alpha = 0.85f),
                        unfocusedContainerColor = colors.cardBackground.copy(alpha = 0.65f),
                        focusedBorderColor = colors.accentPrimary.copy(alpha = 0.6f),
                        unfocusedBorderColor = if (colors.isDark) Color.White.copy(alpha = 0.08f) else colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accentPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_notes_input")
                )

                // Direct Community Sharing Toggle inside Review Studio (when signed in)
                if (shareReviewsWithCommunity != null && onToggleShareReviews != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.elevatedSurface.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Share Rating & Note in Community",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (shareReviewsWithCommunity) {
                                        "Visible in Community when both star rating & note are filled"
                                    } else {
                                        "Private — only visible in your personal library"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Switch(
                                checked = shareReviewsWithCommunity,
                                onCheckedChange = onToggleShareReviews
                            )
                        }
                    }
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text("Remove", color = colors.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.weight(1f))
                AppPrimaryButton(
                    text = "Save Review",
                    onClick = {
                        val scoreInt = if (stars > 0f) StarRatingUtils.starsToScore100(stars) else null
                        val finalEp = progressText.toIntOrNull() ?: progress
                        onSave(
                            selectedStatus,
                            ProgressRules.clampEpisodeProgress(finalEp, anime.totalEpisodes),
                            anime.effectiveSeasonNumber,
                            scoreInt,
                            notes.trim(),
                            favorite
                        )
                    },
                    modifier = Modifier.testTag("save_tracking_button")
                )
            }
        }
    }
}

/**
 * Compact inline custom episode stepper (`[ 850 ] / 1150`) supporting direct numeric typing,
 * auto-selecting existing digits, Enter/Done save, Escape cancel, explicit Save button,
 * loading indicator, inline validation error, and automatic rollback on sync failure.
 */
@Composable
fun InlineEpisodeNumberStepper(
    animeId: Int,
    watchedEpisodeCount: Int,
    totalEpisodes: Int?,
    canDecrement: Boolean,
    canIncrement: Boolean,
    compact: Boolean = true,
    onDecrement: (() -> Unit)? = null,
    onIncrement: (() -> Unit)? = null,
    onCommitCustomEpisode: (suspend (Int) -> Result<Unit>)? = null,
    onFallbackTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var isEditing by remember(animeId) { mutableStateOf(false) }
    var isSaving by remember(animeId) { mutableStateOf(false) }
    var errorMessage by remember(animeId) { mutableStateOf<String?>(null) }
    var fieldValue by remember(animeId, watchedEpisodeCount) {
        val text = watchedEpisodeCount.toString()
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(0, text.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            val currentText = watchedEpisodeCount.toString()
            fieldValue = TextFieldValue(text = currentText, selection = TextRange(0, currentText.length))
            errorMessage = null
            runCatching { focusRequester.requestFocus() }
        }
    }

    val cancelEditing: () -> Unit = {
        val currentText = watchedEpisodeCount.toString()
        fieldValue = TextFieldValue(text = currentText, selection = TextRange(0, currentText.length))
        errorMessage = null
        isEditing = false
    }

    val saveEditing: () -> Unit = {
        val commit = onCommitCustomEpisode
        if (commit != null && !isSaving) {
            when (val validation = ProgressRules.validateEpisodeInput(fieldValue.text, totalEpisodes)) {
                is com.example.model.EpisodeInputValidation.Invalid -> {
                    errorMessage = validation.message
                }
                is com.example.model.EpisodeInputValidation.Valid -> {
                    val targetEp = validation.episodes
                    if (targetEp == watchedEpisodeCount) {
                        errorMessage = null
                        isEditing = false
                    } else {
                        isSaving = true
                        errorMessage = null
                        scope.launch {
                            val res = commit(targetEp)
                            isSaving = false
                            if (res.isSuccess) {
                                isEditing = false
                                errorMessage = null
                            } else {
                                val restoredText = watchedEpisodeCount.toString()
                                fieldValue = TextFieldValue(text = restoredText, selection = TextRange(0, restoredText.length))
                                errorMessage = res.exceptionOrNull()?.message ?: "Save failed. Previous value restored."
                            }
                        }
                    }
                }
            }
        }
    }

    val totalLabel = totalEpisodes?.takeIf { it > 0 }?.toString() ?: "∞"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = if (compact) CircleShape else RoundedCornerShape(22.dp),
            color = colors.elevatedSurface.copy(alpha = if (colors.isDark) 0.72f else 0.88f),
            border = if (isEditing) BorderStroke(1.2.dp, colors.accentPrimary.copy(alpha = 0.78f)) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isEditing && onCommitCustomEpisode != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 4.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.cardBackground,
                        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = if (compact) 30.dp else 40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            BasicTextField(
                                value = fieldValue,
                                onValueChange = { next ->
                                    if (next.text.length <= 6) {
                                        fieldValue = next
                                        errorMessage = null
                                    }
                                },
                                enabled = !isSaving,
                                singleLine = true,
                                cursorBrush = SolidColor(colors.accentPrimary),
                                textStyle = TabularNumMedium.copy(
                                    fontSize = if (compact) 13.5.sp else 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { saveEditing() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.Escape -> {
                                                    cancelEditing()
                                                    true
                                                }
                                                Key.Enter, Key.NumPadEnter -> {
                                                    saveEditing()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                                    .testTag("inline_episode_input_$animeId")
                            )
                            Text(
                                text = "/$totalLabel",
                                style = TabularNumSmall.copy(fontSize = if (compact) 11.sp else 13.sp),
                                color = colors.textMuted,
                                maxLines = 1
                            )
                        }
                    }

                    if (isSaving) {
                        Box(
                            modifier = Modifier.size(if (compact) 28.dp else 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = colors.accentPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = colors.accentPrimary,
                            modifier = Modifier
                                .size(if (compact) 28.dp else 36.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable(onClick = saveEditing)
                                .testTag("inline_episode_save_$animeId")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Save episode count",
                                    tint = colors.onAccentPrimary,
                                    modifier = Modifier.size(if (compact) 15.dp else 18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = colors.cardBackground,
                            modifier = Modifier
                                .size(if (compact) 28.dp else 36.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clip(CircleShape)
                                .clickable(onClick = cancelEditing)
                                .testTag("inline_episode_cancel_$animeId")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cancel episode edit",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(if (compact) 14.dp else 17.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (compact) 4.dp else 8.dp, vertical = if (compact) 4.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.cardBackground.copy(alpha = if (canDecrement) 1f else 0.45f),
                        modifier = Modifier
                            .size(if (compact) 30.dp else 44.dp)
                            .then(if (canDecrement) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                            .clip(CircleShape)
                            .clickable(enabled = canDecrement && onDecrement != null) {
                                onDecrement?.invoke()
                            }
                            .testTag("anime_decrement_action_$animeId")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "−",
                                fontSize = if (compact) 16.sp else 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (canDecrement) colors.textPrimary else colors.textMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clip(CircleShape)
                            .clickable {
                                if (onCommitCustomEpisode != null) {
                                    isEditing = true
                                } else {
                                    onFallbackTap?.invoke()
                                }
                            }
                            .padding(vertical = if (compact) 2.dp else 6.dp)
                            .testTag("inline_episode_trigger_$animeId"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("$watchedEpisodeCount")
                                withStyle(
                                    SpanStyle(
                                        color = colors.textMuted,
                                        fontSize = if (compact) 11.5.sp else 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append(" / $totalLabel")
                                }
                            },
                            style = TabularNumMedium.copy(
                                fontSize = if (compact) 14.5.sp else 22.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (canIncrement) colors.accentPrimary else colors.cardBackground.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(if (compact) 30.dp else 44.dp)
                            .then(if (canIncrement) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                            .clip(CircleShape)
                            .clickable(enabled = canIncrement && onIncrement != null) {
                                onIncrement?.invoke()
                            }
                            .testTag("anime_quick_action_$animeId")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "+",
                                fontSize = if (compact) 16.sp else 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canIncrement) colors.onAccentPrimary else colors.textMuted
                            )
                        }
                    }
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.error,
                maxLines = 2,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .testTag("inline_episode_error_$animeId")
            )
        }
    }
}

/**
 * Confirmation dialog for removing an anime ONLY from the Home tracking list.
 * Preserves Library status, progress, notes, ratings, reviews, and Community discussions.
 */
@Composable
fun RemoveFromHomeConfirmDialog(
    animeTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = RoundedCornerShape(AppRadius.xl),
        containerColor = colors.backgroundSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .padding(horizontal = 24.dp)
            .testTag("remove_from_home_confirm_dialog"),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Remove from Home?",
                    fontFamily = CormorantSerifFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                if (animeTitle.isNotBlank()) {
                    Text(
                        text = animeTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Text(
                text = "This anime will be removed from your Home tracking list but will remain in your Library. Your notes, ratings, reviews, and Community discussions will not be affected.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_remove_from_home_button")
            ) {
                Text("Remove from Home", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("cancel_remove_from_home_button")
            ) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

/**
 * Long-press context menu sheet for anime cards across Home and Library.
 * Includes the 4 required actions: Remove from Home (or Add to Home), View Anime Details,
 * View in Library, and View Community, plus optional Quick Edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeCardContextMenuSheet(
    anime: AnimeEntity,
    titlePreference: String = "ENGLISH",
    isOnHome: Boolean = anime.inHome,
    onDismiss: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onAddToHome: () -> Unit,
    onViewDetails: () -> Unit,
    onViewLibrary: () -> Unit,
    onViewCommunity: () -> Unit,
    onOpenQuickEdit: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val title = anime.getResolvedTitle(titlePreference)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundSecondary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
                .testTag("anime_card_context_menu"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ResilientPosterImage(
                    imageUrl = anime.coverImage,
                    contentDescription = title,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(width = 48.dp, height = 68.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        fontFamily = CormorantSerifFamily,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (anime.inWatchlist) {
                            WatchStatusPill(status = anime.watchStatusEnum)
                        }
                        if (isOnHome) {
                            Surface(
                                shape = CircleShape,
                                color = colors.accentPrimary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "ON HOME",
                                    style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 0.7.sp),
                                    color = colors.accentPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)

            if (isOnHome) {
                ContextMenuActionRow(
                    icon = Icons.Filled.RemoveCircleOutline,
                    label = "Remove from Home",
                    subtitle = "Keep in Library, notes & Community intact",
                    tint = colors.error,
                    tag = "context_action_remove_from_home",
                    onClick = onRemoveFromHome
                )
            } else {
                ContextMenuActionRow(
                    icon = Icons.Filled.Home,
                    label = "Add to Home",
                    subtitle = "Pin to your active weekly tracking dashboard",
                    tint = colors.accentPrimary,
                    tag = "context_action_add_to_home",
                    onClick = onAddToHome
                )
            }

            ContextMenuActionRow(
                icon = Icons.Filled.Info,
                label = "View Anime Details",
                subtitle = "Episodes, seasons, cast & reviews",
                tint = colors.textPrimary,
                tag = "context_action_view_details",
                onClick = onViewDetails
            )

            ContextMenuActionRow(
                icon = Icons.Filled.Bookmarks,
                label = "View in Library",
                subtitle = "Open permanent anime collection",
                tint = colors.textPrimary,
                tag = "context_action_view_library",
                onClick = onViewLibrary
            )

            ContextMenuActionRow(
                icon = Icons.Filled.Group,
                label = "View Community",
                subtitle = "See discussions, notes & ratings",
                tint = colors.textPrimary,
                tag = "context_action_view_community",
                onClick = onViewCommunity
            )

            if (onOpenQuickEdit != null) {
                HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
                ContextMenuActionRow(
                    icon = Icons.Filled.Edit,
                    label = "Edit Status, Rating & Notes",
                    subtitle = "Update status, star rating, or review note",
                    tint = colors.highlight,
                    tag = "context_action_edit_tracking",
                    onClick = onOpenQuickEdit
                )
            }
        }
    }
}

@Composable
private fun ContextMenuActionRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.elevatedSurface.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = tint,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
