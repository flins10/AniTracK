package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.model.AnimeCommentItem
import com.example.model.CommentReplyItem
import com.example.model.CommentReplyThread
import com.example.model.CommunityEmoji
import com.example.model.MentionableUser
import com.example.model.NotificationJumpTarget
import com.example.service.CommunityEmojiCacheService
import com.example.ui.screens.formatCommunityTimestamp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.MemberAvatar
import com.example.util.DiscussionRules
import com.example.viewmodel.AnimeViewModel

private val LivTrAkBg = Color(0xFF030304)
private val LivTrAkSurface = Color(0xFF0E0E16)
private val LivTrAkSurface2 = Color(0xFF14141E)
private val LivTrAkGold = Color(0xFFE8C88F)
private val LivTrAkGoldSoft = Color(0xFFF5E4BE)
private val LivTrAkIvory = Color(0xFFF8F4EB)
private val LivTrAkAsh = Color(0xFF9A9AA8)
private val LivTrAkAsh2 = Color(0xFF66667A)
private val LivTrAkHairline = Color(0x14FFFFFF)

private data class ReplyComposerTarget(
    val commentId: String,
    val targetUserId: String,
    val targetUsername: String
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AnimeDiscussionSection(
    animeId: Int,
    animeTitle: String,
    animeCover: String,
    viewModel: AnimeViewModel,
    highlightTarget: NotificationJumpTarget? = null,
    initialReplyTargetUser: MentionableUser? = null,
    onInitialReplyTargetConsumed: () -> Unit = {},
    inModal: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val haptics = LocalHapticFeedback.current
    val profile by viewModel.userProfile.collectAsState()
    val authSession by viewModel.authSession.collectAsState()
    val isSignedIn = profile != null || authSession != null
    val currentUserId = profile?.id?.takeIf { it.isNotBlank() } ?: authSession?.userId
    val comments by viewModel.discussionComments.collectAsState()
    val totalComments by viewModel.discussionTotalCount.collectAsState()
    val hasMore by viewModel.discussionHasMore.collectAsState()
    val isLoading by viewModel.isDiscussionLoading.collectAsState()
    val isSubmitting by viewModel.isDiscussionSubmitting.collectAsState()
    val feedbackError by viewModel.discussionErrorMessage.collectAsState()
    val mentionSuggestions by viewModel.mentionSuggestions.collectAsState()
    val communityEmojis by viewModel.communityEmojis.collectAsState()

    val emojiById = remember(communityEmojis) { communityEmojis.associateBy { it.id } }
    val chronologicalComments = remember(comments) { comments.asReversed() }
    val listState = rememberLazyListState()

    var draft by remember(animeId) { mutableStateOf(TextFieldValue("")) }
    var selectedTags by remember(animeId) { mutableStateOf<List<MentionableUser>>(emptyList()) }
    var activeReplyTarget by remember(animeId) { mutableStateOf<ReplyComposerTarget?>(null) }
    var isEmojiTrayOpen by remember(animeId) { mutableStateOf(false) }
    var showMentionMenu by remember(animeId) { mutableStateOf(false) }
    var editingCommentId by remember(animeId) { mutableStateOf<String?>(null) }
    var editingCommentDraft by remember(animeId) { mutableStateOf("") }
    var editingReplyId by remember(animeId) { mutableStateOf<String?>(null) }
    var editingReplyDraft by remember(animeId) { mutableStateOf("") }
    var openCommentMenuId by remember(animeId) { mutableStateOf<String?>(null) }
    val expandedThreads = remember(animeId) { mutableStateMapOf<String, Boolean>() }
    var scrolledToHighlightKey by remember(animeId) { mutableStateOf<String?>(null) }

    // Non-blocking preload of custom emoji metadata & top frequently used emojis
    LaunchedEffect(animeId) {
        viewModel.loadCommunityEmojis()
    }

    // Auto-expand & scroll to highlighted message when jumping from "Tagged You" notification
    LaunchedEffect(highlightTarget, chronologicalComments) {
        val target = highlightTarget ?: return@LaunchedEffect
        val key = "${target.commentId}_${target.replyId.orEmpty()}"
        if (scrolledToHighlightKey == key) return@LaunchedEffect
        if (!target.threadId.isNullOrBlank()) {
            expandedThreads[target.threadId] = true
        }
        val idx = chronologicalComments.indexOfFirst { it.id == target.commentId }
        if (idx >= 0) {
            scrolledToHighlightKey = key
            listState.animateScrollToItem(idx)
        }
    }

    // When triggered via "Reply" on a Community review, switch composer into reply mode or pre-tag
    LaunchedEffect(initialReplyTargetUser, isLoading, comments) {
        val targetUser = initialReplyTargetUser ?: return@LaunchedEffect
        if (isLoading) return@LaunchedEffect
        val existingComment = comments.firstOrNull { c ->
            (targetUser.id.isNotBlank() && c.userId == targetUser.id) ||
                c.username.equals(targetUser.username, ignoreCase = true)
        }
        if (existingComment != null) {
            existingComment.threads.firstOrNull { it.targetUserId == existingComment.userId }?.let { th ->
                expandedThreads[th.id] = true
            }
            activeReplyTarget = ReplyComposerTarget(
                commentId = existingComment.id,
                targetUserId = existingComment.userId,
                targetUsername = existingComment.username
            )
        } else if (targetUser.id.isNotBlank()) {
            val currentTotal = DiscussionRules.validateContentMentions(draft.text, selectedTags.size).totalEntries
            if (currentTotal < DiscussionRules.MAX_MENTIONS_PER_CONTENT &&
                selectedTags.none { it.id == targetUser.id }
            ) {
                selectedTags = selectedTags + targetUser
            }
        }
        onInitialReplyTargetConsumed()
    }

    val draftValidation = remember(draft.text, selectedTags) {
        DiscussionRules.validateContentMentions(draft.text, explicitTagCount = selectedTags.size)
    }

    val draftEmojiIds = remember(draft.text) {
        CommunityEmojiCacheService.extractEmojiIds(draft.text)
    }

    val insertEmojiIntoDraft: (CommunityEmoji) -> Unit = { emoji ->
        val token = "[emoji:${emoji.id}] "
        val cur = draft.text
        val pos = draft.selection.end.coerceIn(0, cur.length)
        val prefix = cur.substring(0, pos)
        val needsSpace = prefix.isNotEmpty() && !prefix.endsWith(" ")
        val inserted = (if (needsSpace) " " else "") + token
        val nextText = prefix + inserted + cur.substring(pos)
        val nextCursor = pos + inserted.length
        draft = TextFieldValue(nextText, TextRange(nextCursor))
    }

    val sendAction: () -> Unit = {
        if (!isSubmitting && draftValidation.isValid) {
            val replyTarget = activeReplyTarget
            if (replyTarget != null) {
                viewModel.postThreadReply(
                    commentId = replyTarget.commentId,
                    targetUserId = replyTarget.targetUserId,
                    content = draft.text,
                    taggedUserIds = selectedTags.map { it.id }
                ) { threadId ->
                    expandedThreads[threadId] = true
                    draft = TextFieldValue("")
                    selectedTags = emptyList()
                    activeReplyTarget = null
                    showMentionMenu = false
                    isEmojiTrayOpen = false
                    viewModel.clearMentionSuggestions()
                }
            } else {
                viewModel.postAnimeComment(
                    animeId = animeId,
                    animeTitle = animeTitle,
                    animeCover = animeCover,
                    content = draft.text,
                    taggedUserIds = selectedTags.map { it.id }
                ) {
                    draft = TextFieldValue("")
                    selectedTags = emptyList()
                    showMentionMenu = false
                    isEmojiTrayOpen = false
                    viewModel.clearMentionSuggestions()
                }
            }
        }
    }

    val containerModifier = if (inModal) {
        modifier
            .fillMaxSize()
            .background(LivTrAkBg)
            .testTag("anime_discussion_section")
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(LivTrAkSurface)
            .border(1.dp, LivTrAkHairline, RoundedCornerShape(24.dp))
            .testTag("anime_discussion_section")
    }

    Column(modifier = containerModifier) {
        // Compact header ONLY when embedded on AnimeDetailScreen (removed inside modal per user spec)
        if (!inModal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(LivTrAkGold)
                    )
                    Text(
                        text = "LIVE CHAT",
                        style = MicroUpperLabel.copy(fontSize = 9.sp, letterSpacing = 2.sp),
                        color = LivTrAkGold
                    )
                }
                Text(
                    text = "$totalComments ${if (totalComments == 1) "message" else "messages"}",
                    style = TabularNumSmall.copy(fontSize = 11.sp),
                    color = LivTrAkAsh
                )
            }
            HorizontalDivider(color = LivTrAkHairline, thickness = 1.dp)
        }

        // Inline Error Banner (if any)
        AnimatedVisibility(visible = !feedbackError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.error.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("discussion_error_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = feedbackError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearDiscussionFeedback() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = colors.error, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Chat Stream
        val chatListModifier = if (inModal) {
            Modifier
                .weight(1f)
                .fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 420.dp)
        }

        Box(modifier = chatListModifier) {
            when {
                isLoading && chronologicalComments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LivTrAkGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
                chronologicalComments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet. Say hello or drop a custom emoji below!",
                            style = MaterialTheme.typography.bodySmall,
                            color = LivTrAkAsh
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("livtrak_chat_messages_list")
                    ) {
                        if (hasMore) {
                            item(key = "load_older_messages") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = LivTrAkSurface2,
                                        border = BorderStroke(1.dp, LivTrAkHairline),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable(enabled = !isLoading) { viewModel.loadMoreAnimeDiscussion(animeId) }
                                    ) {
                                        Text(
                                            text = if (isLoading) "Loading older..." else "Load older messages",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LivTrAkGoldSoft,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(chronologicalComments, key = { it.id }) { comment ->
                            val isMine = currentUserId != null && currentUserId == comment.userId
                            val isHighlighted = highlightTarget?.commentId == comment.id && highlightTarget.replyId == null
                            val isEditingThis = editingCommentId == comment.id
                            val existingAuthorThread = comment.threads.firstOrNull { it.targetUserId == comment.userId }

                            LivTrAkChatMessageItem(
                                comment = comment,
                                isMine = isMine,
                                isSignedIn = isSignedIn,
                                isHighlighted = isHighlighted,
                                isEditing = isEditingThis,
                                editingDraft = editingCommentDraft,
                                isSubmitting = isSubmitting,
                                emojiById = emojiById,
                                currentUserId = currentUserId,
                                highlightTarget = highlightTarget,
                                expandedThreads = expandedThreads,
                                editingReplyId = editingReplyId,
                                editingReplyDraft = editingReplyDraft,
                                menuExpanded = openCommentMenuId == comment.id,
                                onOpenMenu = {
                                    if (isSignedIn) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        openCommentMenuId = comment.id
                                    }
                                },
                                onDismissMenu = { openCommentMenuId = null },
                                onToggleAuthorThread = {
                                    if (existingAuthorThread != null) {
                                        expandedThreads[existingAuthorThread.id] = !(expandedThreads[existingAuthorThread.id] ?: false)
                                    }
                                },
                                onReplyToComment = {
                                    if (existingAuthorThread != null) {
                                        expandedThreads[existingAuthorThread.id] = true
                                    }
                                    activeReplyTarget = ReplyComposerTarget(
                                        commentId = comment.id,
                                        targetUserId = comment.userId,
                                        targetUsername = comment.username
                                    )
                                },
                                onStartEditComment = {
                                    editingCommentId = comment.id
                                    editingCommentDraft = comment.content
                                },
                                onChangeEditCommentDraft = { editingCommentDraft = it },
                                onCancelEditComment = { editingCommentId = null },
                                onSaveEditComment = {
                                    viewModel.editAnimeComment(comment.id, editingCommentDraft) {
                                        editingCommentId = null
                                    }
                                },
                                onDeleteComment = { viewModel.deleteAnimeComment(comment.id) },
                                onContinueThread = { thread ->
                                    expandedThreads[thread.id] = true
                                    activeReplyTarget = ReplyComposerTarget(
                                        commentId = comment.id,
                                        targetUserId = thread.targetUserId,
                                        targetUsername = thread.targetUsername
                                    )
                                },
                                onReplyToReplyAuthor = { reply ->
                                    comment.threads.firstOrNull { it.targetUserId == reply.userId }?.let { t ->
                                        expandedThreads[t.id] = true
                                    }
                                    activeReplyTarget = ReplyComposerTarget(
                                        commentId = comment.id,
                                        targetUserId = reply.userId,
                                        targetUsername = reply.username
                                    )
                                },
                                onStartEditReply = { reply ->
                                    editingReplyId = reply.id
                                    editingReplyDraft = reply.content
                                },
                                onChangeEditReplyDraft = { editingReplyDraft = it },
                                onCancelEditReply = { editingReplyId = null },
                                onSaveEditReply = { threadId, replyId ->
                                    viewModel.editThreadReply(comment.id, threadId, replyId, editingReplyDraft) {
                                        editingReplyId = null
                                    }
                                },
                                onDeleteReply = { threadId, replyId ->
                                    viewModel.deleteThreadReply(comment.id, threadId, replyId)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Docked Area: Mention Autocomplete + Emoji Tray + Pill Input Bar
        if (isSignedIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LivTrAkSurface)
            ) {
                HorizontalDivider(color = LivTrAkHairline, thickness = 1.dp)

                // Floating @mention autocomplete directly above input
                AnimatedVisibility(visible = showMentionMenu && mentionSuggestions.isNotEmpty()) {
                    MentionAutocompletePanel(
                        suggestions = mentionSuggestions,
                        onSelectUser = { user ->
                            val active = DiscussionRules.detectActiveMentionQuery(draft.text, draft.selection.end)
                            if (active != null) {
                                val (stripped, cursor) = DiscussionRules.stripMentionTrigger(
                                    text = draft.text,
                                    atSignIndex = active.first,
                                    cursorPosition = draft.selection.end
                                )
                                draft = TextFieldValue(stripped, TextRange(cursor))
                            }
                            if (draftValidation.totalEntries < DiscussionRules.MAX_MENTIONS_PER_CONTENT &&
                                selectedTags.none { it.id == user.id }
                            ) {
                                selectedTags = selectedTags + user
                            }
                            showMentionMenu = false
                            viewModel.clearMentionSuggestions()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Active Reply Context Strip
                val replyTarget = activeReplyTarget
                AnimatedVisibility(visible = replyTarget != null) {
                    if (replyTarget != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LivTrAkGold.copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = LivTrAkGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Replying to @${replyTarget.targetUsername}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = LivTrAkGoldSoft
                                )
                            }
                            IconButton(
                                onClick = { activeReplyTarget = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel reply", tint = LivTrAkAsh, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Selected @tag chips + Custom Emoji draft preview chips
                if (selectedTags.isNotEmpty() || draftEmojiIds.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        selectedTags.forEachIndexed { idx, user ->
                            Surface(
                                shape = CircleShape,
                                color = LivTrAkGold.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, LivTrAkGold.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = LivTrAkGoldSoft
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove @${user.username}",
                                        tint = LivTrAkGold,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedTags = selectedTags.filterIndexed { i, _ -> i != idx }
                                            }
                                    )
                                }
                            }
                        }
                        draftEmojiIds.distinct().take(6).forEach { eid ->
                            val emoji = emojiById[eid]
                            if (emoji != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = LivTrAkSurface2,
                                    border = BorderStroke(1.dp, LivTrAkHairline)
                                ) {
                                    Box(modifier = Modifier.padding(3.dp)) {
                                        CachedCustomEmojiImage(
                                            emoji = emoji,
                                            size = 20.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Toggleable Custom Emoji Tray (Dynamic Top Emojis + Scrollable 6-col Grid)
                AnimatedVisibility(visible = isEmojiTrayOpen) {
                    LivTrAkEmojiTray(
                        emojis = communityEmojis,
                        onSelectEmoji = insertEmojiIntoDraft
                    )
                }

                // Pill Input Bar ("Message...") with Smiley Toggle + Circular Gold Send Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isEmojiTrayOpen) LivTrAkGold.copy(alpha = 0.18f) else LivTrAkSurface2,
                        border = BorderStroke(1.dp, if (isEmojiTrayOpen) LivTrAkGold.copy(alpha = 0.5f) else LivTrAkHairline),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                isEmojiTrayOpen = !isEmojiTrayOpen
                                if (isEmojiTrayOpen && communityEmojis.isEmpty()) {
                                    viewModel.loadCommunityEmojis(forceRefresh = true)
                                }
                            }
                            .testTag("chat_emoji_toggle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SentimentSatisfiedAlt,
                                contentDescription = "Toggle custom emojis",
                                tint = if (isEmojiTrayOpen) LivTrAkGold else LivTrAkAsh,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = LivTrAkSurface2,
                        border = BorderStroke(1.dp, LivTrAkHairline),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (draft.text.isEmpty()) {
                                Text(
                                    text = if (replyTarget != null) "Reply to @${replyTarget.targetUsername}..." else "Message...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LivTrAkAsh2
                                )
                            }
                            BasicTextField(
                                value = draft,
                                onValueChange = { next ->
                                    draft = next
                                    val active = DiscussionRules.detectActiveMentionQuery(next.text, next.selection.end)
                                    if (active != null) {
                                        showMentionMenu = true
                                        viewModel.searchMentionUsers(active.second)
                                    } else if (showMentionMenu) {
                                        showMentionMenu = false
                                        viewModel.clearMentionSuggestions()
                                    }
                                },
                                enabled = !isSubmitting,
                                textStyle = TextStyle(
                                    color = LivTrAkIvory,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                cursorBrush = SolidColor(LivTrAkGold),
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("discussion_comment_input")
                            )
                        }
                    }

                    val canSend = !isSubmitting && draftValidation.isValid
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) LivTrAkGold else LivTrAkSurface2,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(enabled = canSend, onClick = sendAction)
                            .testTag("discussion_post_comment_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = LivTrAkBg,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = if (canSend) LivTrAkBg else LivTrAkAsh2,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (draftValidation.totalEntries > DiscussionRules.MAX_MENTIONS_PER_CONTENT) {
                    Text(
                        text = draftValidation.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.error,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                }
            }
        } else {
            Surface(
                color = LivTrAkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sign in to chat, tag friends with @, and send custom emojis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LivTrAkAsh,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LivTrAkChatMessageItem(
    comment: AnimeCommentItem,
    isMine: Boolean,
    isSignedIn: Boolean,
    isHighlighted: Boolean,
    isEditing: Boolean,
    editingDraft: String,
    isSubmitting: Boolean,
    emojiById: Map<String, CommunityEmoji>,
    currentUserId: String?,
    highlightTarget: NotificationJumpTarget?,
    expandedThreads: MutableMap<String, Boolean>,
    editingReplyId: String?,
    editingReplyDraft: String,
    menuExpanded: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onToggleAuthorThread: () -> Unit,
    onReplyToComment: () -> Unit,
    onStartEditComment: () -> Unit,
    onChangeEditCommentDraft: (String) -> Unit,
    onCancelEditComment: () -> Unit,
    onSaveEditComment: () -> Unit,
    onDeleteComment: () -> Unit,
    onContinueThread: (CommentReplyThread) -> Unit,
    onReplyToReplyAuthor: (CommentReplyItem) -> Unit,
    onStartEditReply: (CommentReplyItem) -> Unit,
    onChangeEditReplyDraft: (String) -> Unit,
    onCancelEditReply: () -> Unit,
    onSaveEditReply: (threadId: String, replyId: String) -> Unit,
    onDeleteReply: (threadId: String, replyId: String) -> Unit
) {
    val colors = AppTheme.colors
    val bubbleShape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    }
    val bubbleBg = when {
        isHighlighted -> LivTrAkGold.copy(alpha = 0.24f)
        isMine -> Color(0x26E8C88F)
        else -> Color(0x0DFFFFFF)
    }
    val bubbleBorder = when {
        isHighlighted -> LivTrAkGold
        isMine -> Color(0x4DE8C88F)
        else -> LivTrAkHairline
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                if (!isMine) {
                    MemberAvatar(
                        avatarKey = comment.avatarKey,
                        avatarUrl = comment.avatarUrl,
                        username = comment.username,
                        size = 32.dp,
                        modifier = Modifier.padding(top = 2.dp, end = 10.dp)
                    )
                }

                Column(
                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Sender name + time row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        if (!isMine) {
                            Text(
                                text = comment.username,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                                color = LivTrAkGoldSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val editedTag = if (comment.updatedAt.isNotBlank() && comment.updatedAt != comment.createdAt) " • edited" else ""
                        Text(
                            text = "${formatCommunityTimestamp(comment.createdAt)}$editedTag",
                            style = TabularNumSmall.copy(fontSize = 10.sp),
                            color = LivTrAkAsh2
                        )
                    }

                    // Message Bubble
                    Surface(
                        shape = bubbleShape,
                        color = bubbleBg,
                        border = BorderStroke(1.dp, bubbleBorder),
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .clip(bubbleShape)
                            .pointerInput(comment.id, isSignedIn) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed && isSignedIn) {
                                            onOpenMenu()
                                        }
                                    }
                                }
                            }
                            .combinedClickable(
                                onClick = onToggleAuthorThread,
                                onLongClick = onOpenMenu
                            )
                            .testTag("discussion_comment_card_${comment.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editingDraft,
                                    onValueChange = onChangeEditCommentDraft,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val editCheck = DiscussionRules.validateContentMentions(editingDraft)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AppPrimaryButton(
                                        text = if (isSubmitting) "Saving..." else "Save",
                                        enabled = !isSubmitting && editCheck.isValid,
                                        onClick = onSaveEditComment
                                    )
                                    TextButton(onClick = onCancelEditComment) {
                                        Text("Cancel", color = LivTrAkAsh)
                                    }
                                }
                            } else {
                                RichChatMessageText(
                                    content = comment.content,
                                    emojiById = emojiById,
                                    mentionColor = LivTrAkGold,
                                    textColor = LivTrAkIvory
                                )
                                if (comment.mentions.isNotEmpty()) {
                                    val uniqueTags = comment.mentions.map { it.mentionedUsername }.distinct()
                                    Text(
                                        text = uniqueTags.joinToString("  ") { "@$it" },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                                        color = LivTrAkGold
                                    )
                                }
                            }
                        }
                    }

                    // Compact quick reply count/action pill under bubble
                    if (isSignedIn) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                        ) {
                            Text(
                                text = "Reply",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
                                color = LivTrAkAsh2,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onReplyToComment)
                                    .padding(vertical = 2.dp)
                                    .testTag("reply_to_comment_${comment.id}")
                            )
                            if (comment.totalReplyCount > 0) {
                                Text(
                                    text = "${comment.totalReplyCount} ${if (comment.totalReplyCount == 1) "reply" else "replies"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                                    color = LivTrAkGold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(onClick = onToggleAuthorThread)
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onDismissMenu,
                containerColor = LivTrAkSurface2
            ) {
                DropdownMenuItem(
                    text = { Text("Reply to @${comment.username}", color = LivTrAkIvory) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = LivTrAkGold, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        onDismissMenu()
                        onReplyToComment()
                    }
                )
                if (isMine && !isEditing) {
                    DropdownMenuItem(
                        text = { Text("Edit Message", color = LivTrAkIvory) },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = LivTrAkAsh, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onDismissMenu()
                            onStartEditComment()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Message", color = colors.error) },
                        leadingIcon = {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onDismissMenu()
                            onDeleteComment()
                        }
                    )
                }
            }
        }

        // Compact Expandable Reply Threads under this message
        if (comment.threads.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = if (isMine) 28.dp else 42.dp)
            ) {
                comment.threads.forEach { thread ->
                    val isExpanded = expandedThreads[thread.id]
                        ?: (highlightTarget?.threadId == thread.id || (comment.threads.size == 1 && thread.replies.size <= 2))
                    CompactChatReplyThread(
                        thread = thread,
                        isExpanded = isExpanded,
                        currentUserId = currentUserId,
                        isSignedIn = isSignedIn,
                        isSubmitting = isSubmitting,
                        highlightReplyId = highlightTarget?.replyId,
                        emojiById = emojiById,
                        editingReplyId = editingReplyId,
                        editingReplyDraft = editingReplyDraft,
                        onToggleExpand = { expandedThreads[thread.id] = !isExpanded },
                        onContinueThread = { onContinueThread(thread) },
                        onReplyToAuthor = onReplyToReplyAuthor,
                        onStartEditReply = onStartEditReply,
                        onChangeEditReplyDraft = onChangeEditReplyDraft,
                        onCancelEditReply = onCancelEditReply,
                        onSaveEditReply = { rid -> onSaveEditReply(thread.id, rid) },
                        onDeleteReply = { rid -> onDeleteReply(thread.id, rid) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactChatReplyThread(
    thread: CommentReplyThread,
    isExpanded: Boolean,
    currentUserId: String?,
    isSignedIn: Boolean,
    isSubmitting: Boolean,
    highlightReplyId: String?,
    emojiById: Map<String, CommunityEmoji>,
    editingReplyId: String?,
    editingReplyDraft: String,
    onToggleExpand: () -> Unit,
    onContinueThread: () -> Unit,
    onReplyToAuthor: (CommentReplyItem) -> Unit,
    onStartEditReply: (CommentReplyItem) -> Unit,
    onChangeEditReplyDraft: (String) -> Unit,
    onCancelEditReply: () -> Unit,
    onSaveEditReply: (String) -> Unit,
    onDeleteReply: (String) -> Unit
) {
    val colors = AppTheme.colors
    val haptics = LocalHapticFeedback.current
    var openReplyMenuId by remember(thread.id) { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = LivTrAkSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, LivTrAkHairline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reply_thread_${thread.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(LivTrAkGold)
                    )
                    Text(
                        text = "Thread • @${thread.targetUsername} (${thread.replies.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LivTrAkGoldSoft
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse thread" else "Expand thread",
                    tint = LivTrAkGold,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    thread.replies.forEach { reply ->
                        val isReplyMine = currentUserId == reply.userId
                        val isHighlighted = highlightReplyId == reply.id
                        val isEditingReply = editingReplyId == reply.id

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isHighlighted -> LivTrAkGold.copy(alpha = 0.22f)
                                    isReplyMine -> Color(0x1FE8C88F)
                                    else -> LivTrAkSurface2
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isHighlighted) LivTrAkGold else LivTrAkHairline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .pointerInput(reply.id, isSignedIn) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed && isSignedIn) {
                                                    openReplyMenuId = reply.id
                                                }
                                            }
                                        }
                                    }
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            if (isSignedIn) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                openReplyMenuId = reply.id
                                            }
                                        }
                                    )
                                    .testTag("reply_item_${reply.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        MemberAvatar(
                                            avatarKey = reply.avatarKey,
                                            avatarUrl = reply.avatarUrl,
                                            username = reply.username,
                                            size = 20.dp
                                        )
                                        Text(
                                            text = reply.username,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = LivTrAkGoldSoft
                                        )
                                        Text(
                                            text = formatCommunityTimestamp(reply.createdAt),
                                            style = TabularNumSmall.copy(fontSize = 9.5.sp),
                                            color = LivTrAkAsh2
                                        )
                                    }

                                    if (isEditingReply) {
                                        val check = DiscussionRules.validateContentMentions(editingReplyDraft)
                                        OutlinedTextField(
                                            value = editingReplyDraft,
                                            onValueChange = onChangeEditReplyDraft,
                                            maxLines = 3,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AppPrimaryButton(
                                                text = if (isSubmitting) "Saving..." else "Save",
                                                enabled = !isSubmitting && check.isValid,
                                                onClick = { onSaveEditReply(reply.id) }
                                            )
                                            TextButton(onClick = onCancelEditReply) {
                                                Text("Cancel", color = LivTrAkAsh)
                                            }
                                        }
                                    } else {
                                        RichChatMessageText(
                                            content = reply.content,
                                            emojiById = emojiById,
                                            mentionColor = LivTrAkGold,
                                            textColor = LivTrAkIvory,
                                            fontSizeSp = 13f
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = openReplyMenuId == reply.id,
                                onDismissRequest = { openReplyMenuId = null },
                                containerColor = LivTrAkSurface2
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reply to @${reply.username}", color = LivTrAkIvory) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = LivTrAkGold, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        openReplyMenuId = null
                                        onReplyToAuthor(reply)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Continue thread", color = LivTrAkIvory) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Forum, contentDescription = null, tint = LivTrAkGold, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        openReplyMenuId = null
                                        onContinueThread()
                                    }
                                )
                                if (isReplyMine) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Reply", color = LivTrAkIvory) },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Edit, contentDescription = null, tint = LivTrAkAsh, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            openReplyMenuId = null
                                            onStartEditReply(reply)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Reply", color = colors.error) },
                                        leadingIcon = {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            openReplyMenuId = null
                                            onDeleteReply(reply.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isSignedIn) {
                        Text(
                            text = "↳ Reply in thread",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LivTrAkGold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onContinueThread)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("continue_thread_${thread.id}")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders chat text with inline gold @mentions AND inline circular 28dp custom emojis
 * fetched & cached via Coil memory+disk cache using a single canonical Storage URL.
 */
@Composable
private fun RichChatMessageText(
    content: String,
    emojiById: Map<String, CommunityEmoji>,
    mentionColor: Color,
    textColor: Color,
    fontSizeSp: Float = 14f
) {
    val combinedRegex = remember { Regex("""\[emoji:([a-zA-Z0-9_-]{1,64})]|(@[A-Za-z0-9_.-]{2,32})""") }
    val inlineMap = remember(content, emojiById) {
        val map = LinkedHashMap<String, InlineTextContent>()
        CommunityEmojiCacheService.extractEmojiIds(content).distinct().forEach { eid ->
            val emoji = emojiById[eid]
            if (emoji != null) {
                map["emoji_$eid"] = InlineTextContent(
                    placeholder = Placeholder(
                        width = 28.sp,
                        height = 28.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    CachedCustomEmojiImage(emoji = emoji, size = 26.dp)
                }
            }
        }
        map
    }

    val annotated = remember(content, emojiById, mentionColor, textColor) {
        buildAnnotatedString {
            var cursor = 0
            combinedRegex.findAll(content).forEach { match ->
                if (match.range.first > cursor) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(content.substring(cursor, match.range.first))
                    }
                }
                val emojiId = match.groups[1]?.value
                val mention = match.groups[2]?.value
                when {
                    emojiId != null && emojiById.containsKey(emojiId) -> {
                        appendInlineContent("emoji_$emojiId", "✨")
                    }
                    emojiId != null -> {
                        withStyle(SpanStyle(color = LivTrAkAsh, fontSize = 11.sp)) {
                            append(":$emojiId:")
                        }
                    }
                    mention != null -> {
                        withStyle(SpanStyle(color = mentionColor, fontWeight = FontWeight.SemiBold)) {
                            append(mention)
                        }
                    }
                }
                cursor = match.range.last + 1
            }
            if (cursor < content.length) {
                withStyle(SpanStyle(color = textColor)) {
                    append(content.substring(cursor))
                }
            }
        }
    }

    Text(
        text = annotated,
        inlineContent = inlineMap,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp + 6f).sp
        )
    )
}

/**
 * Toggleable LivTrAk Custom Emoji Tray:
 * - Dynamic "TOP EMOJIS" horizontal strip (sorted by real usage frequency, never hardcoded)
 * - Lazy 6-column circular emoji grid (only loads visible cells, cached in memory + disk)
 */
@Composable
private fun LivTrAkEmojiTray(
    emojis: List<CommunityEmoji>,
    onSelectEmoji: (CommunityEmoji) -> Unit
) {
    val topEmojis = remember(emojis) { emojis.take(8) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LivTrAkSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("livtrak_emoji_tray"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (emojis.isEmpty()) {
            Text(
                text = "Upload emojis to your Supabase \"community_emojis\" or \"community-emojis\" bucket to see them here.",
                style = MaterialTheme.typography.bodySmall,
                color = LivTrAkAsh,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            // Dynamic Top Emojis Strip (ordered by real usage frequency)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TOP EMOJIS",
                    style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 1.6.sp),
                    color = LivTrAkGold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(topEmojis, key = { "top_${it.id}" }) { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = LivTrAkSurface2,
                            border = BorderStroke(1.dp, LivTrAkGold.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { onSelectEmoji(emoji) }
                                .testTag("top_emoji_${emoji.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                CachedCustomEmojiImage(emoji = emoji, size = 34.dp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = LivTrAkHairline, thickness = 1.dp)

            // Scrollable Lazy 6-Column All Emojis Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                items(emojis, key = { "grid_${it.id}" }) { emoji ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LivTrAkSurface2.copy(alpha = 0.7f))
                            .clickable { onSelectEmoji(emoji) }
                            .testTag("grid_emoji_${emoji.id}")
                    ) {
                        CachedCustomEmojiImage(emoji = emoji, size = 36.dp)
                    }
                }
            }
        }
    }
}

/**
 * Lightweight cached custom emoji renderer:
 * - Uses stable memoryCacheKey & diskCacheKey ("livtrak_emoji_<id>") so the same image is never redownloaded
 * - Sends immutable Cache-Control header
 * - Shows a subtle animated circular shimmer while loading and a graceful fallback on error
 */
@Composable
internal fun CachedCustomEmojiImage(
    emoji: CommunityEmoji,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cacheKey = remember(emoji.id) { CommunityEmojiCacheService.cacheKeyFor(emoji.id) }
    val request = remember(emoji.id, emoji.url) {
        ImageRequest.Builder(context)
            .data(emoji.url)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .addHeader("Cache-Control", "public, max-age=31536000, immutable")
            .crossfade(120)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = emoji.id,
        contentScale = ContentScale.Crop,
        loading = { EmojiShimmerPlaceholder(size = size) },
        error = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(LivTrAkSurface2)
            ) {
                Text(
                    text = emoji.id.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = LivTrAkAsh2
                )
            }
        },
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}

@Composable
private fun EmojiShimmerPlaceholder(size: Dp) {
    val transition = rememberInfiniteTransition(label = "emoji_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_shimmer_alpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(LivTrAkGold.copy(alpha = alpha * 0.28f))
    )
}

@Composable
private fun MentionAutocompletePanel(
    suggestions: List<MentionableUser>,
    onSelectUser: (MentionableUser) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LivTrAkSurface2,
        border = BorderStroke(1.dp, LivTrAkGold.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mention_autocomplete_panel")
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            suggestions.take(6).forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectUser(user) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("mention_suggestion_${user.username}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MemberAvatar(
                        avatarKey = user.avatarKey,
                        avatarUrl = user.avatarUrl,
                        username = user.username,
                        size = 26.dp
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LivTrAkGoldSoft,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.AlternateEmail,
                        contentDescription = null,
                        tint = LivTrAkGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
