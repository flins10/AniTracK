package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.automirrored.filled.Reply
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NotificationJumpTarget
import com.example.model.UserNotificationItem
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CormorantSerifFamily
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppEmptyState
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.AppSecondaryButton
import com.example.ui.widgets.MemberAvatar
import com.example.ui.widgets.ObsidianAmbientBackground
import com.example.ui.widgets.ObsidianTopBar
import com.example.ui.widgets.ResilientPosterImage
import com.example.viewmodel.AnimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: AnimeViewModel,
    onOpenNotificationTarget: (NotificationJumpTarget) -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val profile by viewModel.userProfile.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val hasMore by viewModel.hasMoreNotifications.collectAsState()
    val isLoading by viewModel.isNotificationsLoading.collectAsState()
    val error by viewModel.notificationsError.collectAsState()

    LaunchedEffect(profile?.id) {
        if (profile != null) {
            viewModel.refreshNotifications(reset = true)
        }
    }

    if (profile == null) {
        ObsidianAmbientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 104.dp)
                    .testTag("notifications_locked_view")
            ) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                    ObsidianTopBar(
                        title = "Notifications",
                        subtitle = "Mentions & Thread Replies",
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
                                text = "Sign In for Notifications",
                                fontFamily = CormorantSerifFamily,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Sign in to receive @mention alerts and thread reply notifications across all anime discussions.",
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

    ObsidianAmbientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 980.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("notifications_screen")
            ) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)) {
                    ObsidianTopBar(
                        title = "Notifications",
                        subtitle = if (unreadCount > 0) "$unreadCount Unread Alerts" else "All Caught Up",
                        userProfile = profile,
                        onOpenProfile = onNavigateToProfile,
                        actions = {
                            if (unreadCount > 0) {
                                TextButton(
                                    onClick = { viewModel.markAllNotificationsRead() },
                                    modifier = Modifier.testTag("mark_all_notifications_read")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DoneAll,
                                        contentDescription = "Mark all read",
                                        tint = colors.accentPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Mark all read",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.accentPrimary
                                    )
                                }
                            }
                        }
                    )
                }

                if (!error.isNullOrBlank()) {
                    Surface(
                        color = colors.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.md, vertical = 4.dp)
                    ) {
                        Text(
                            text = error!!,
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                val pullRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.refreshNotifications(reset = true) },
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
                ) {
                    if (notifications.isEmpty() && !isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(AppSpacing.xl),
                            contentAlignment = Alignment.Center
                        ) {
                            AppEmptyState(
                                icon = Icons.Filled.NotificationsNone,
                                title = "No notifications yet",
                                description = "When someone mentions you with @username or replies to your thread, it will appear here."
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = AppSpacing.md,
                                end = AppSpacing.md,
                                top = 8.dp,
                                bottom = 112.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(notifications, key = { it.id }) { item ->
                                NotificationRowCard(
                                    item = item,
                                    onClick = {
                                        viewModel.markNotificationRead(item.id)
                                        val target = NotificationJumpTarget(
                                            animeId = item.animeId,
                                            commentId = item.commentId,
                                            threadId = item.threadId,
                                            replyId = item.replyId
                                        )
                                        viewModel.setPendingNotificationJump(target)
                                        onOpenNotificationTarget(target)
                                    }
                                )
                            }
                            if (hasMore) {
                                item(key = "load_more_notifications") {
                                    AppSecondaryButton(
                                        text = if (isLoading) "Loading..." else "Load Older Notifications",
                                        enabled = !isLoading,
                                        onClick = { viewModel.loadMoreNotifications() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                            .testTag("load_more_notifications_button")
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

@Composable
private fun NotificationRowCard(
    item: UserNotificationItem,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val isUnread = !item.isRead
    val isReply = item.notificationType == "thread_reply"
    val formattedTime = formatCommunityTimestamp(item.createdAt)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isUnread) colors.accentPrimary.copy(alpha = 0.09f) else colors.cardBackground,
        border = BorderStroke(
            1.dp,
            if (isUnread) colors.accentPrimary.copy(alpha = 0.45f) else colors.borderSubtle.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .testTag("notification_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box {
                MemberAvatar(
                    avatarKey = item.senderAvatarKey,
                    avatarUrl = item.senderAvatarUrl,
                    username = item.senderUsername,
                    size = 42.dp
                )
                Surface(
                    shape = CircleShape,
                    color = if (isReply) colors.highlight else colors.accentPrimary,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isReply) Icons.AutoMirrored.Filled.Reply else Icons.Filled.AlternateEmail,
                            contentDescription = null,
                            tint = colors.onAccentPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary)) {
                                append("@${item.senderUsername} ")
                            }
                            withStyle(SpanStyle(color = colors.textSecondary)) {
                                append(item.actionSummary)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(colors.accentPrimary)
                        )
                    }
                }

                if (item.previewText.isNotBlank()) {
                    Text(
                        text = "“${item.previewText}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when (item.notificationType) {
                            "thread_reply" -> "THREAD REPLY"
                            "mention_reply" -> "REPLY MENTION"
                            else -> "COMMENT MENTION"
                        },
                        style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 1.sp),
                        color = colors.accentPrimary
                    )
                    if (formattedTime.isNotBlank()) {
                        Text(
                            text = "• $formattedTime",
                            style = TabularNumSmall.copy(fontSize = 10.sp),
                            color = colors.textMuted
                        )
                    }
                }
            }

            if (item.animeCoverImage.isNotBlank()) {
                ResilientPosterImage(
                    imageUrl = item.animeCoverImage,
                    contentDescription = item.animeTitle,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(width = 42.dp, height = 60.dp)
                )
            }
        }
    }
}
