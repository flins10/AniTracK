package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnimeCommentItem
import com.example.model.CommentReplyItem
import com.example.model.CommentReplyThread
import com.example.model.ContentMention
import com.example.model.MentionableUser
import com.example.model.NotificationJumpTarget
import com.example.ui.screens.formatCommunityTimestamp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MicroUpperLabel
import com.example.ui.theme.TabularNumSmall
import com.example.ui.widgets.AppCard
import com.example.ui.widgets.AppPrimaryButton
import com.example.ui.widgets.AppSecondaryButton
import com.example.ui.widgets.MemberAvatar
import com.example.util.DiscussionRules
import com.example.viewmodel.AnimeViewModel

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
    val feedbackSuccess by viewModel.discussionSuccessMessage.collectAsState()
    val feedbackError by viewModel.discussionErrorMessage.collectAsState()
    val mentionSuggestions by viewModel.mentionSuggestions.collectAsState()

    var commentDraft by remember(animeId) { mutableStateOf(TextFieldValue("")) }
    var selectedCommentTags by remember(animeId) { mutableStateOf<List<MentionableUser>>(emptyList()) }
    var activeReplyTarget by remember(animeId) { mutableStateOf<ReplyComposerTarget?>(null) }
    var replyDraft by remember(animeId) { mutableStateOf(TextFieldValue("")) }
    var selectedReplyTags by remember(animeId) { mutableStateOf<List<MentionableUser>>(emptyList()) }
    var editingCommentId by remember(animeId) { mutableStateOf<String?>(null) }
    var editingCommentDraft by remember(animeId) { mutableStateOf("") }
    var editingReplyId by remember(animeId) { mutableStateOf<String?>(null) }
    var editingReplyDraft by remember(animeId) { mutableStateOf("") }
    var activeAutocompleteTarget by remember(animeId) { mutableStateOf<String?>(null) } // "comment" or "reply"
    var openCommentMenuId by remember(animeId) { mutableStateOf<String?>(null) }
    val expandedThreads = remember(animeId) { mutableStateMapOf<String, Boolean>() }

    // Auto-expand the target thread when jumping from an unseen red-dot discussion
    LaunchedEffect(highlightTarget, comments) {
        val targetThreadId = highlightTarget?.threadId
        if (!targetThreadId.isNullOrBlank()) {
            expandedThreads[targetThreadId] = true
        }
    }

    // When triggered via "Reply" on a Community note, open that user's existing comment thread or pre-tag them in the composer
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
            replyDraft = TextFieldValue("")
            selectedReplyTags = emptyList()
        } else if (targetUser.id.isNotBlank()) {
            val currentTotal = DiscussionRules.validateContentMentions(commentDraft.text, selectedCommentTags.size).totalEntries
            if (currentTotal < DiscussionRules.MAX_MENTIONS_PER_CONTENT &&
                selectedCommentTags.none { it.id == targetUser.id }
            ) {
                selectedCommentTags = selectedCommentTags + targetUser
            }
        }
        onInitialReplyTargetConsumed()
    }

    val commentValidation = remember(commentDraft.text, selectedCommentTags) {
        DiscussionRules.validateContentMentions(commentDraft.text, explicitTagCount = selectedCommentTags.size)
    }

    AppCard(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("anime_discussion_section")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section header with live count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(1.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, colors.accentPrimary)))
                    )
                    Text(
                        text = "ANIME DISCUSSION",
                        style = MicroUpperLabel.copy(fontSize = 8.5.sp, letterSpacing = 2.4.sp),
                        color = colors.accentPrimary
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = colors.accentPrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "$totalComments ${if (totalComments == 1) "COMMENT" else "COMMENTS"}",
                        style = MicroUpperLabel.copy(fontSize = 8.sp, letterSpacing = 1.sp),
                        color = colors.highlight,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("discussion_comment_count")
                    )
                }
            }

            Text(
                text = "Tap Reply or long-press any comment to reply in a thread • Type @ + letter to tag",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )

            // Clear Success / Error Feedback Banners
            AnimatedVisibility(visible = !feedbackSuccess.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.success.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, colors.success.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discussion_success_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = feedbackSuccess.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.success,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearDiscussionFeedback() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = colors.success, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !feedbackError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.error.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discussion_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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

            // Comment Composer (authenticated users)
            if (isSignedIn) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { next ->
                            commentDraft = next
                            val active = DiscussionRules.detectActiveMentionQuery(next.text, next.selection.end)
                            if (active != null) {
                                activeAutocompleteTarget = "comment"
                                viewModel.searchMentionUsers(active.second)
                            } else if (activeAutocompleteTarget == "comment") {
                                activeAutocompleteTarget = null
                                viewModel.clearMentionSuggestions()
                            }
                        },
                        enabled = !isSubmitting,
                        placeholder = {
                            Text(
                                text = "Write a comment... Type @ + first letter to tag users (max 5)",
                                fontSize = 13.sp,
                                color = colors.textMuted
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentPrimary,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discussion_comment_input")
                    )

                    if (selectedCommentTags.isNotEmpty()) {
                        SelectedTagChipsRow(
                            tags = selectedCommentTags,
                            onRemoveAt = { idx ->
                                selectedCommentTags = selectedCommentTags.filterIndexed { i, _ -> i != idx }
                            }
                        )
                    }

                    if (activeAutocompleteTarget == "comment" && mentionSuggestions.isNotEmpty()) {
                        MentionAutocompletePanel(
                            suggestions = mentionSuggestions,
                            onSelectUser = { user ->
                                val active = DiscussionRules.detectActiveMentionQuery(commentDraft.text, commentDraft.selection.end)
                                if (active != null) {
                                    val (stripped, cursor) = DiscussionRules.stripMentionTrigger(
                                        text = commentDraft.text,
                                        atSignIndex = active.first,
                                        cursorPosition = commentDraft.selection.end
                                    )
                                    commentDraft = TextFieldValue(stripped, TextRange(cursor))
                                }
                                if (commentValidation.totalEntries < DiscussionRules.MAX_MENTIONS_PER_CONTENT) {
                                    selectedCommentTags = selectedCommentTags + user
                                }
                                activeAutocompleteTarget = null
                                viewModel.clearMentionSuggestions()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val overLimit = commentValidation.totalEntries > DiscussionRules.MAX_MENTIONS_PER_CONTENT
                        Text(
                            text = "${commentValidation.totalEntries}/${DiscussionRules.MAX_MENTIONS_PER_CONTENT} tagged",
                            style = TabularNumSmall.copy(fontSize = 11.sp),
                            color = if (overLimit) colors.error else colors.textMuted
                        )

                        Surface(
                            shape = CircleShape,
                            color = if (!isSubmitting && commentValidation.isValid) colors.accentPrimary else colors.elevatedSurface,
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .clip(CircleShape)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable(enabled = !isSubmitting && commentValidation.isValid) {
                                    viewModel.postAnimeComment(
                                        animeId = animeId,
                                        animeTitle = animeTitle,
                                        animeCover = animeCover,
                                        content = commentDraft.text,
                                        taggedUserIds = selectedCommentTags.map { it.id }
                                    ) {
                                        commentDraft = TextFieldValue("")
                                        selectedCommentTags = emptyList()
                                        activeAutocompleteTarget = null
                                        viewModel.clearMentionSuggestions()
                                    }
                                }
                                .testTag("discussion_post_comment_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = colors.onAccentPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Posting...",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textSecondary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = if (commentValidation.isValid) colors.onAccentPrimary else colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Post Comment",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (commentValidation.isValid) colors.onAccentPrimary else colors.textMuted
                                    )
                                }
                            }
                        }
                    }

                    if (commentValidation.totalEntries > DiscussionRules.MAX_MENTIONS_PER_CONTENT) {
                        Text(
                            text = commentValidation.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.elevatedSurface.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sign in to post comments, tag friends with @username, and join reply threads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.55f), thickness = 1.dp)

            if (isLoading && comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accentPrimary, modifier = Modifier.size(24.dp))
                }
            } else if (comments.isEmpty()) {
                Text(
                    text = "No discussion comments yet. Start the conversation above!",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    comments.forEach { comment ->
                        val isHighlightedComment = highlightTarget?.commentId == comment.id && highlightTarget.replyId == null
                        val isOwner = currentUserId != null && currentUserId == comment.userId
                        val isEditingThis = editingCommentId == comment.id
                        val existingAuthorThread = comment.threads.firstOrNull { it.targetUserId == comment.userId }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isHighlightedComment) colors.accentPrimary.copy(alpha = 0.12f) else colors.elevatedSurface.copy(alpha = 0.42f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isHighlightedComment) colors.accentPrimary else colors.borderSubtle.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .pointerInput(comment.id, isSignedIn) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed && isSignedIn) {
                                                    openCommentMenuId = comment.id
                                                }
                                            }
                                        }
                                    }
                                    .combinedClickable(
                                        onClick = {
                                            if (existingAuthorThread != null) {
                                                expandedThreads[existingAuthorThread.id] = !(expandedThreads[existingAuthorThread.id] ?: false)
                                            }
                                        },
                                        onLongClick = {
                                            if (isSignedIn) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                openCommentMenuId = comment.id
                                            }
                                        }
                                    )
                                    .testTag("discussion_comment_card_${comment.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Author header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            MemberAvatar(
                                                avatarKey = comment.avatarKey,
                                                avatarUrl = comment.avatarUrl,
                                                username = comment.username,
                                                size = 34.dp
                                            )
                                            Column {
                                                Text(
                                                    text = "@${comment.username}",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val editedLabel = if (comment.updatedAt.isNotBlank() && comment.updatedAt != comment.createdAt) " • edited" else ""
                                                Text(
                                                    text = "${formatCommunityTimestamp(comment.createdAt)}$editedLabel",
                                                    style = TabularNumSmall.copy(fontSize = 10.sp),
                                                    color = colors.textMuted
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isSignedIn) {
                                                TextButton(
                                                    onClick = {
                                                        if (existingAuthorThread != null) {
                                                            expandedThreads[existingAuthorThread.id] = true
                                                        }
                                                        activeReplyTarget = ReplyComposerTarget(
                                                            commentId = comment.id,
                                                            targetUserId = comment.userId,
                                                            targetUsername = comment.username
                                                        )
                                                        replyDraft = TextFieldValue("")
                                                        selectedReplyTags = emptyList()
                                                    },
                                                    modifier = Modifier.testTag("reply_to_comment_${comment.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                                        contentDescription = "Reply to ${comment.username}",
                                                        tint = colors.accentPrimary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Reply", style = MaterialTheme.typography.labelSmall, color = colors.accentPrimary)
                                                }
                                            }
                                            if (isOwner && !isEditingThis) {
                                                IconButton(
                                                    onClick = {
                                                        editingCommentId = comment.id
                                                        editingCommentDraft = comment.content
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "Edit comment", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteAnimeComment(comment.id) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete comment", tint = colors.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Comment body or inline edit field
                                    if (isEditingThis) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = editingCommentDraft,
                                                onValueChange = { editingCommentDraft = it },
                                                maxLines = 4,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            val editCheck = DiscussionRules.validateContentMentions(editingCommentDraft)
                                            if (!editCheck.isValid && editingCommentDraft.isNotBlank()) {
                                                Text(editCheck.errorMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.error)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                AppPrimaryButton(
                                                    text = if (isSubmitting) "Saving..." else "Save",
                                                    enabled = !isSubmitting && editCheck.isValid,
                                                    onClick = {
                                                        viewModel.editAnimeComment(comment.id, editingCommentDraft) {
                                                            editingCommentId = null
                                                        }
                                                    }
                                                )
                                                TextButton(onClick = { editingCommentId = null }) {
                                                    Text("Cancel", color = colors.textSecondary)
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = highlightMentionsText(comment.content, colors.accentPrimary, colors.textPrimary),
                                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                                        )
                                        if (comment.mentions.isNotEmpty()) {
                                            TaggedMentionsDisplayRow(mentions = comment.mentions)
                                        }
                                    }

                                    // Target-User Grouped Reply Threads
                                    if (comment.threads.isNotEmpty()) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            comment.threads.forEach { thread ->
                                                val isExpanded = expandedThreads[thread.id]
                                                    ?: (highlightTarget?.threadId == thread.id || comment.threads.size == 1 && thread.replies.size <= 2)
                                                ReplyThreadContainer(
                                                    thread = thread,
                                                    isExpanded = isExpanded,
                                                    currentUserId = currentUserId,
                                                    isSignedIn = isSignedIn,
                                                    isSubmitting = isSubmitting,
                                                    highlightReplyId = highlightTarget?.replyId,
                                                    editingReplyId = editingReplyId,
                                                    editingReplyDraft = editingReplyDraft,
                                                    onToggleExpand = { expandedThreads[thread.id] = !isExpanded },
                                                    onContinueThread = {
                                                        expandedThreads[thread.id] = true
                                                        activeReplyTarget = ReplyComposerTarget(
                                                            commentId = comment.id,
                                                            targetUserId = thread.targetUserId,
                                                            targetUsername = thread.targetUsername
                                                        )
                                                        replyDraft = TextFieldValue("")
                                                        selectedReplyTags = emptyList()
                                                    },
                                                    onReplyToAuthor = { replyItem ->
                                                        comment.threads.firstOrNull { it.targetUserId == replyItem.userId }?.let { t ->
                                                            expandedThreads[t.id] = true
                                                        }
                                                        activeReplyTarget = ReplyComposerTarget(
                                                            commentId = comment.id,
                                                            targetUserId = replyItem.userId,
                                                            targetUsername = replyItem.username
                                                        )
                                                        replyDraft = TextFieldValue("")
                                                        selectedReplyTags = emptyList()
                                                    },
                                                    onStartEditReply = { replyItem ->
                                                        editingReplyId = replyItem.id
                                                        editingReplyDraft = replyItem.content
                                                    },
                                                    onChangeEditReplyDraft = { editingReplyDraft = it },
                                                    onCancelEditReply = { editingReplyId = null },
                                                    onSaveEditReply = { replyId ->
                                                        viewModel.editThreadReply(comment.id, thread.id, replyId, editingReplyDraft) {
                                                            editingReplyId = null
                                                        }
                                                    },
                                                    onDeleteReply = { replyId ->
                                                        viewModel.deleteThreadReply(comment.id, thread.id, replyId)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Inline Reply Composer when replying under this comment
                                    val currentTarget = activeReplyTarget
                                    if (currentTarget != null && currentTarget.commentId == comment.id && profile != null) {
                                        val replyValidation = DiscussionRules.validateContentMentions(
                                            replyDraft.text,
                                            explicitTagCount = selectedReplyTags.size
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = colors.cardBackground,
                                            border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.45f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("discussion_reply_composer_${comment.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Replying in thread to @${currentTarget.targetUsername}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = colors.accentPrimary
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            activeReplyTarget = null
                                                            selectedReplyTags = emptyList()
                                                            activeAutocompleteTarget = null
                                                            viewModel.clearMentionSuggestions()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Close, contentDescription = "Close reply box", tint = colors.textMuted, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                                OutlinedTextField(
                                                    value = replyDraft,
                                                    onValueChange = { next ->
                                                        replyDraft = next
                                                        val active = DiscussionRules.detectActiveMentionQuery(next.text, next.selection.end)
                                                        if (active != null) {
                                                            activeAutocompleteTarget = "reply"
                                                            viewModel.searchMentionUsers(active.second)
                                                        } else if (activeAutocompleteTarget == "reply") {
                                                            activeAutocompleteTarget = null
                                                            viewModel.clearMentionSuggestions()
                                                        }
                                                    },
                                                    enabled = !isSubmitting,
                                                    placeholder = {
                                                        Text("Write your reply... Type @ + letter to tag (max 5)", fontSize = 12.5.sp, color = colors.textMuted)
                                                    },
                                                    maxLines = 3,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("discussion_reply_input")
                                                )

                                                if (selectedReplyTags.isNotEmpty()) {
                                                    SelectedTagChipsRow(
                                                        tags = selectedReplyTags,
                                                        onRemoveAt = { idx ->
                                                            selectedReplyTags = selectedReplyTags.filterIndexed { i, _ -> i != idx }
                                                        }
                                                    )
                                                }

                                                if (activeAutocompleteTarget == "reply" && mentionSuggestions.isNotEmpty()) {
                                                    MentionAutocompletePanel(
                                                        suggestions = mentionSuggestions,
                                                        onSelectUser = { user ->
                                                            val active = DiscussionRules.detectActiveMentionQuery(replyDraft.text, replyDraft.selection.end)
                                                            if (active != null) {
                                                                val (stripped, cursor) = DiscussionRules.stripMentionTrigger(
                                                                    text = replyDraft.text,
                                                                    atSignIndex = active.first,
                                                                    cursorPosition = replyDraft.selection.end
                                                                )
                                                                replyDraft = TextFieldValue(stripped, TextRange(cursor))
                                                            }
                                                            if (replyValidation.totalEntries < DiscussionRules.MAX_MENTIONS_PER_CONTENT) {
                                                                selectedReplyTags = selectedReplyTags + user
                                                            }
                                                            activeAutocompleteTarget = null
                                                            viewModel.clearMentionSuggestions()
                                                        }
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${replyValidation.totalEntries}/${DiscussionRules.MAX_MENTIONS_PER_CONTENT} tagged",
                                                        style = TabularNumSmall.copy(fontSize = 10.5.sp),
                                                        color = if (replyValidation.totalEntries > DiscussionRules.MAX_MENTIONS_PER_CONTENT) colors.error else colors.textMuted
                                                    )
                                                    AppPrimaryButton(
                                                        text = if (isSubmitting) "Replying..." else "Post Reply",
                                                        enabled = !isSubmitting && replyValidation.isValid,
                                                        onClick = {
                                                            viewModel.postThreadReply(
                                                                commentId = currentTarget.commentId,
                                                                targetUserId = currentTarget.targetUserId,
                                                                content = replyDraft.text,
                                                                taggedUserIds = selectedReplyTags.map { it.id }
                                                            ) { createdThreadId ->
                                                                expandedThreads[createdThreadId] = true
                                                                replyDraft = TextFieldValue("")
                                                                selectedReplyTags = emptyList()
                                                                activeReplyTarget = null
                                                                activeAutocompleteTarget = null
                                                                viewModel.clearMentionSuggestions()
                                                            }
                                                        },
                                                        modifier = Modifier.testTag("discussion_post_reply_button")
                                                    )
                                                }
                                                if (replyValidation.totalEntries > DiscussionRules.MAX_MENTIONS_PER_CONTENT) {
                                                    Text(replyValidation.errorMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Long-press (Phone) / Right-click (Desktop) Action Menu for Comment
                            DropdownMenu(
                                expanded = openCommentMenuId == comment.id,
                                onDismissRequest = { openCommentMenuId = null },
                                containerColor = colors.backgroundSecondary
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reply to @${comment.username}", color = colors.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        openCommentMenuId = null
                                        if (existingAuthorThread != null) {
                                            expandedThreads[existingAuthorThread.id] = true
                                        }
                                        activeReplyTarget = ReplyComposerTarget(
                                            commentId = comment.id,
                                            targetUserId = comment.userId,
                                            targetUsername = comment.username
                                        )
                                        replyDraft = TextFieldValue("")
                                        selectedReplyTags = emptyList()
                                    }
                                )
                                if (existingAuthorThread != null) {
                                    val opened = expandedThreads[existingAuthorThread.id] ?: false
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (opened) "Collapse @${comment.username}'s Thread" else "Open @${comment.username}'s Thread",
                                                color = colors.textPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Forum, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            openCommentMenuId = null
                                            expandedThreads[existingAuthorThread.id] = !opened
                                        }
                                    )
                                }
                                if (isOwner) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Comment", color = colors.textPrimary) },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            openCommentMenuId = null
                                            editingCommentId = comment.id
                                            editingCommentDraft = comment.content
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Comment", color = colors.error) },
                                        leadingIcon = {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            openCommentMenuId = null
                                            viewModel.deleteAnimeComment(comment.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (hasMore) {
                        AppSecondaryButton(
                            text = if (isLoading) "Loading more comments..." else "Load More Comments",
                            enabled = !isLoading,
                            onClick = { viewModel.loadMoreAnimeDiscussion(animeId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("discussion_load_more_button")
                        )
                    }
                }
            }
        }
    }
}

private data class ReplyComposerTarget(
    val commentId: String,
    val targetUserId: String,
    val targetUsername: String
)

@Composable
private fun MentionAutocompletePanel(
    suggestions: List<MentionableUser>,
    onSelectUser: (MentionableUser) -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.backgroundSecondary,
        border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mention_autocomplete_panel")
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            suggestions.take(6).forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onSelectUser(user) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedTagChipsRow(
    tags: List<MentionableUser>,
    onRemoveAt: (Int) -> Unit
) {
    val colors = AppTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        tags.forEachIndexed { idx, user ->
            Surface(
                shape = CircleShape,
                color = colors.accentPrimary.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AlternateEmail,
                        contentDescription = null,
                        tint = colors.accentPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.highlight
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove tag ${user.username}",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable { onRemoveAt(idx) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaggedMentionsDisplayRow(
    mentions: List<ContentMention>
) {
    val colors = AppTheme.colors
    val grouped = remember(mentions) {
        mentions.groupBy { it.mentionedUsername }
            .map { (username, list) -> username to list.size }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        grouped.forEach { (username, count) ->
            Surface(
                shape = CircleShape,
                color = colors.accentPrimary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, colors.accentPrimary.copy(alpha = 0.30f))
            ) {
                Text(
                    text = if (count > 1) "Tagged: $username ×$count" else "Tagged: $username",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                    color = colors.accentPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReplyThreadContainer(
    thread: CommentReplyThread,
    isExpanded: Boolean,
    currentUserId: String?,
    isSignedIn: Boolean,
    isSubmitting: Boolean,
    highlightReplyId: String?,
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
    val replyCount = thread.replies.size
    var openReplyMenuId by remember(thread.id) { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.backgroundSecondary.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, colors.borderSubtle.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp)
            .testTag("reply_thread_${thread.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Thread Header Pill (collapsible + clearly identifies target user)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .clip(CircleShape)
                            .background(colors.accentPrimary)
                    )
                    Text(
                        text = "Thread for @${thread.targetUsername}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.highlight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "• $replyCount ${if (replyCount == 1) "reply" else "replies"}",
                        style = TabularNumSmall.copy(fontSize = 10.5.sp),
                        color = colors.textMuted
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse thread" else "Expand thread",
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    thread.replies.forEach { reply ->
                        val isHighlighted = highlightReplyId == reply.id
                        val isReplyOwner = currentUserId == reply.userId
                        val isEditingReply = editingReplyId == reply.id

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isHighlighted) colors.accentPrimary.copy(alpha = 0.14f) else colors.cardBackground.copy(alpha = 0.85f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isHighlighted) colors.accentPrimary else colors.borderSubtle.copy(alpha = 0.4f)
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
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            MemberAvatar(
                                                avatarKey = reply.avatarKey,
                                                avatarUrl = reply.avatarUrl,
                                                username = reply.username,
                                                size = 26.dp
                                            )
                                            Column {
                                                Text(
                                                    text = "@${reply.username}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val edited = if (reply.updatedAt.isNotBlank() && reply.updatedAt != reply.createdAt) " • edited" else ""
                                                Text(
                                                    text = "${formatCommunityTimestamp(reply.createdAt)}$edited",
                                                    style = TabularNumSmall.copy(fontSize = 9.5.sp),
                                                    color = colors.textMuted
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isSignedIn) {
                                                TextButton(
                                                    onClick = { onReplyToAuthor(reply) },
                                                    modifier = Modifier.heightIn(min = 32.dp)
                                                ) {
                                                    Text(
                                                        text = "Reply",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.accentPrimary
                                                    )
                                                }
                                            }
                                            if (isReplyOwner && !isEditingReply) {
                                                IconButton(onClick = { onStartEditReply(reply) }, modifier = Modifier.size(30.dp)) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "Edit reply", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                                }
                                                IconButton(onClick = { onDeleteReply(reply.id) }, modifier = Modifier.size(30.dp)) {
                                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete reply", tint = colors.error, modifier = Modifier.size(15.dp))
                                                }
                                            }
                                        }
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
                                        if (!check.isValid && editingReplyDraft.isNotBlank()) {
                                            Text(check.errorMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.error)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AppPrimaryButton(
                                                text = if (isSubmitting) "Saving..." else "Save",
                                                enabled = !isSubmitting && check.isValid,
                                                onClick = { onSaveEditReply(reply.id) }
                                            )
                                            TextButton(onClick = onCancelEditReply) {
                                                Text("Cancel", color = colors.textSecondary)
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = highlightMentionsText(reply.content, colors.accentPrimary, colors.textPrimary),
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp)
                                        )
                                        if (reply.mentions.isNotEmpty()) {
                                            TaggedMentionsDisplayRow(mentions = reply.mentions)
                                        }
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = openReplyMenuId == reply.id,
                                onDismissRequest = { openReplyMenuId = null },
                                containerColor = colors.backgroundSecondary
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reply to @${reply.username}", color = colors.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        openReplyMenuId = null
                                        onReplyToAuthor(reply)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Continue @${thread.targetUsername}'s Thread", color = colors.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Forum, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        openReplyMenuId = null
                                        onContinueThread()
                                    }
                                )
                                if (isReplyOwner) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Reply", color = colors.textPrimary) },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Edit, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
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
                        TextButton(
                            onClick = onContinueThread,
                            modifier = Modifier.testTag("continue_thread_${thread.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = colors.accentPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reply in @${thread.targetUsername}'s thread",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accentPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun highlightMentionsText(
    content: String,
    accentColor: Color,
    defaultColor: Color
): AnnotatedString {
    val tokens = DiscussionRules.extractMentionTokens(content)
    if (tokens.isEmpty()) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = defaultColor)) { append(content) }
        }
    }
    return buildAnnotatedString {
        var cursor = 0
        tokens.forEach { tok ->
            if (tok.startIndex > cursor) {
                withStyle(SpanStyle(color = defaultColor)) {
                    append(content.substring(cursor, tok.startIndex))
                }
            }
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.SemiBold)) {
                append(content.substring(tok.startIndex, tok.endIndex))
            }
            cursor = tok.endIndex
        }
        if (cursor < content.length) {
            withStyle(SpanStyle(color = defaultColor)) {
                append(content.substring(cursor))
            }
        }
    }
}
