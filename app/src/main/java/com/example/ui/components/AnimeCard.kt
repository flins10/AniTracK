package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.AnimeEntity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.util.PosterColorExtractor

@Composable
fun AnimeCard(
    anime: AnimeEntity,
    onWatchedToggle: (Boolean) -> Unit,
    onFavoriteToggle: () -> Unit,
    onNotificationToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titlePreference: String = "ENGLISH",
    onCustomEpisodeChange: ((Int) -> Unit)? = null,
    onWishlistToggle: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    stealthMode: Boolean = false,
    onWatchClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val airedCount = anime.latestAvailableEpisode
    val watchedCount = anime.watchedEpisodeCount
    val isCompleted = (anime.totalEpisodes != null && watchedCount >= anime.totalEpisodes) || (airedCount > 0 && watchedCount >= airedCount)
    val unwatchedCount = anime.unwatchedAvailable
    val hasUnwatchedNewEp = unwatchedCount > 0
    var showCustomEpisodeDialog by remember { mutableStateOf(false) }

    // Poster Color Tone extracted dynamically for EVERY anime poster artwork without exception
    val posterTone = remember(anime) { PosterColorExtractor.getPosterColorTone(anime) }
    val neonColor = if (hasUnwatchedNewEp) Color(0xFFEF4444) else posterTone.primaryGlow

    val displayPrimary = remember(anime, stealthMode) {
        if (stealthMode) "Encrypted Series #${anime.id}" else anime.displayTitle
    }

    val displaySubtitle = remember(anime) {
        val format = anime.format ?: "TV"
        val genre = anime.genres.split(",").map { it.trim() }.firstOrNull { it.isNotEmpty() }
        if (genre != null) "$format   $genre" else format
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = neonColor.copy(alpha = 0.50f),
                ambientColor = posterTone.ambientGlow.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = posterTone.borderColors.map { it.copy(alpha = 0.85f) },
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .pointerInput(anime.id) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick?.invoke()
                    },
                    onTap = { onClick() }
                )
            }
            .testTag("anime_card_${anime.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Dynamic Cover Artwork Backdrop (Blurred & Scaled for Authentic Glassmorphism)
            AsyncImage(
                model = anime.bannerImage ?: anime.coverImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .scale(1.15f)
                    .blur(22.dp)
                    .clip(RoundedCornerShape(26.dp))
            )

            // Frosted Glass Scrim Overlay (Balanced Translucent Depth - lets vibrant art show through)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0C101E).copy(alpha = 0.58f),
                                Color(0xFF080C18).copy(alpha = 0.70f),
                                Color(0xFF04060C).copy(alpha = 0.82f)
                            )
                        )
                    )
            )

            // Specular Gloss Glare & Ambient Neon Reflection (Top-Left Light Streak)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent,
                                neonColor.copy(alpha = 0.08f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(450f, 450f)
                        )
                    )
            )

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Poster Image Thumbnail with Shiny Glass Rim
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(128.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                1.2.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.12f),
                                        neonColor.copy(alpha = 0.35f)
                                    )
                                ),
                                RoundedCornerShape(18.dp)
                            )
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        if (stealthMode) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Hidden in Stealth Mode",
                                    tint = Color(0xFF38BDF8).copy(alpha = 0.65f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            AsyncImage(
                                model = anime.coverImage,
                                contentDescription = anime.titleRomaji,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        // Frosted Glass Score Badge (e.g., "76%")
                        val badgeScore = remember(anime.score) {
                            val sc = anime.score ?: 76
                            if (sc <= 10) "$sc%" else "${sc}%"
                        }
                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.78f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.60f), Color(0xFFFBBF24).copy(alpha = 0.80f)))),
                            modifier = Modifier
                                .padding(6.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = badgeScore,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                        }

                        // Notification / Live Airing Neon Status Dot
                        if (hasUnwatchedNewEp || anime.notificationsEnabled) {
                            Box(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(if (hasUnwatchedNewEp) Color(0xFFEF4444) else Color(0xFF10B981))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }

                        // Pinned Indicator Badge
                        if (anime.isPinned) {
                            Surface(
                                color = Color(0xFF090E1A).copy(alpha = 0.90f),
                                shape = CircleShape,
                                border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF06B6D4)))),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "📌",
                                        style = TextStyle(fontSize = 12.sp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Info Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Titles
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = displayPrimary,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (displaySubtitle.isNotEmpty()) {
                                Text(
                                    text = displaySubtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Aired vs Total episodes badge & MAL/Local Status Indicator
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val totalText = if (anime.totalEpisodes != null && anime.totalEpisodes > 0) {
                                    "${anime.totalEpisodes}"
                                } else {
                                    "Ongoing"
                                }
                                Text(
                                    text = "Aired: $airedCount / $totalText eps",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    ),
                                    color = neonColor
                                )
                                // Frosted Local vs MAL status indicator badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (anime.idMal != null) Color(0xFF2E51A2).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = if (anime.idMal != null) "MAL #${anime.idMal}" else "Local Title",
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (anime.idMal != null) Color(0xFF93C5FD) else Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                if (unwatchedCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.28f),
                                        border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.65f))
                                    ) {
                                        Text(
                                            text = "$unwatchedCount UNWATCHED",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = Color(0xFFFCA5A5)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom Episode Watched Tracker / Stepper (Oval Frosted Glass with Red Circular Buttons)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.65f))
                                .border(
                                    1.dp,
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color(0xFF38BDF8).copy(alpha = 0.40f),
                                            Color.White.copy(alpha = 0.15f)
                                        )
                                    ),
                                    RoundedCornerShape(22.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Decrement (-) Circular Red Button
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .border(1.dp, Color.White.copy(alpha = 0.60f), CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val newCount = (watchedCount - 1).coerceAtLeast(0)
                                            if (onCustomEpisodeChange != null) onCustomEpisodeChange(newCount)
                                            else onWatchedToggle(newCount > 0)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrement watched episode",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showCustomEpisodeDialog = true
                                        }
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Watched: Ep $watchedCount / $airedCount",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color.White
                                    )
                                    val remaining = (airedCount - watchedCount).coerceAtLeast(0)
                                    Text(
                                        text = if (remaining == 0) "Up to date ✓" else "$remaining new episodes to watch",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (remaining == 0) Color(0xFF34D399) else Color(0xFFFCA5A5)
                                    )
                                }
                            }

                            // Increment (+) Circular Red Button
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .border(1.dp, Color.White.copy(alpha = 0.60f), CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val newCount = watchedCount + 1
                                        if (onCustomEpisodeChange != null) onCustomEpisodeChange(newCount)
                                        else onWatchedToggle(true)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increment watched episode",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Countdown Timer View
                        CountdownView(anime = anime)

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action Icons Row with Frosted Floating Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Prominent Watch Button
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (onWatchClick != null) onWatchClick() else onClick()
                                        }
                                        .testTag("anime_card_watch_btn_${anime.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Watch",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Watch",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }

                                // Favorite toggle (Frosted Circular Button)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (anime.isFavorite) Color(0xFFEF4444).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                                        .border(0.5.dp, if (anime.isFavorite) Color(0xFFEF4444).copy(alpha = 0.50f) else Color.White.copy(alpha = 0.15f), CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onFavoriteToggle()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (anime.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (anime.isFavorite) Color(0xFFEF4444) else Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // Wishlist toggle (Frosted Circular Button)
                                if (onWishlistToggle != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (anime.isWishlist) neonColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                                            .border(0.5.dp, if (anime.isWishlist) neonColor.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.15f), CircleShape)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onWishlistToggle()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (anime.isWishlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Wishlist",
                                            tint = if (anime.isWishlist) neonColor else Color.White.copy(alpha = 0.75f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                // Notification toggle (Frosted Circular Button)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (anime.notificationsEnabled) Color(0xFF10B981).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                                        .border(0.5.dp, if (anime.notificationsEnabled) Color(0xFF10B981).copy(alpha = 0.50f) else Color.White.copy(alpha = 0.15f), CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNotificationToggle()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (anime.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                                        contentDescription = "Notification",
                                        tint = if (anime.notificationsEnabled) Color(0xFF34D399) else Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            // Watched Checkbox Pill (Shiny Glassmorphism Pill)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (anime.isWatched || isCompleted) Color(0xFF10B981).copy(alpha = 0.28f)
                                        else Color.White.copy(alpha = 0.12f)
                                    )
                                    .border(
                                        0.8.dp,
                                        if (anime.isWatched || isCompleted) Color(0xFF34D399).copy(alpha = 0.65f)
                                        else Color.White.copy(alpha = 0.25f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onWatchedToggle(!anime.isWatched)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("watched_checkbox_${anime.id}")
                            ) {
                                Checkbox(
                                    checked = anime.isWatched || isCompleted,
                                    onCheckedChange = { checked ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onWatchedToggle(checked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF10B981),
                                        uncheckedColor = Color.White.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (anime.isWatched || isCompleted) "Watched" else "Watch",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (anime.isWatched || isCompleted) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }
                }

                // Bottom Progress Indicator Bar (Neon Glowing Track)
                val progressFraction = if (airedCount > 0) {
                    (watchedCount.toFloat() / airedCount.toFloat()).coerceIn(0f, 1f)
                } else 0f
                if (progressFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = if (progressFraction >= 1f) Color(0xFF10B981) else neonColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }

    if (showCustomEpisodeDialog) {
        var customEpText by remember { mutableStateOf(watchedCount.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomEpisodeDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = Color(0xFF090E1A),
            title = {
                Text(
                    text = "Custom Episode Selection",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter custom episode number for '${displayPrimary}':",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = customEpText,
                        onValueChange = { customEpText = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = customEpText.toIntOrNull()?.coerceAtLeast(0)
                        if (parsed != null) {
                            if (onCustomEpisodeChange != null) onCustomEpisodeChange(parsed)
                            else onWatchedToggle(parsed > 0)
                        }
                        showCustomEpisodeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomEpisodeDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
