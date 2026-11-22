package com.example.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class DesktopLiveReplyTarget(
    val commentId: String,
    val targetUserId: String,
    val targetUsername: String,
    val quotedReplyId: String? = null,
    val quotedUsername: String = targetUsername,
    val quotedPreview: String = ""
)

@Composable
private fun DesktopQuotedReplyPreviewCard(
    quotedUsername: String,
    quotedPreview: String,
    onClick: () -> Unit
) {
    val cleanSnippet = remember(quotedPreview) {
        DesktopEmojiCacheService.formatPreviewSnippet(quotedPreview).ifBlank { "Original message" }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x14E8C88F))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ChampagneGold)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "@$quotedUsername",
                color = ChampagneGold,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = cleanSnippet,
                color = WarmSilver,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopClickableMentionChipsRow(
    mentions: List<DesktopMentionOccurrence>,
    enabled: Boolean,
    onMentionClick: (String) -> Unit
) {
    val uniqueNames = remember(mentions) { mentions.map { it.mentionedUsername }.distinct() }
    if (uniqueNames.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        uniqueNames.forEach { uname ->
            Surface(
                shape = CircleShape,
                color = ChampagneGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(CircleShape)
                    .then(if (enabled) Modifier.clickable { onMentionClick(uname) } else Modifier)
            ) {
                Text(
                    text = "@$uname",
                    color = ChampagneGoldSoft,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopLivTrAkChatSection(
    anime: DesktopAnimeItem,
    session: DesktopPersistedState,
    isAuthenticated: Boolean,
    jumpTarget: DesktopNotificationJumpTarget?,
    onJumpTargetConsumed: () -> Unit,
    onSessionUpdated: (DesktopPersistedState) -> Unit,
    onShowToast: (String, Boolean) -> Unit,
    onMarkedAnimeSeen: (Int) -> Unit = {},
    initialReplyUser: DesktopMentionUser? = null,
    onInitialReplyUserConsumed: () -> Unit = {},
    inModal: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var comments by remember(anime.id) { mutableStateOf<List<DesktopAnimeComment>>(emptyList()) }
    var totalComments by remember(anime.id) { mutableStateOf(0) }
    var hasMore by remember(anime.id) { mutableStateOf(false) }
    var loading by remember(anime.id) { mutableStateOf(true) }
    var loadingMore by remember(anime.id) { mutableStateOf(false) }
    var submitting by remember(anime.id) { mutableStateOf(false) }
    var errorMsg by remember(anime.id) { mutableStateOf<String?>(null) }

    var draft by remember(anime.id) { mutableStateOf(TextFieldValue("")) }
    var selectedTags by remember(anime.id) { mutableStateOf<List<DesktopMentionUser>>(emptyList()) }
    var activeReplyTarget by remember(anime.id) { mutableStateOf<DesktopLiveReplyTarget?>(null) }
    var isEmojiTrayOpen by remember(anime.id) { mutableStateOf(false) }
    var communityEmojis by remember { mutableStateOf(DesktopEmojiCacheService.readCachedMetaSync()) }

    var editingCommentId by remember(anime.id) { mutableStateOf<String?>(null) }
    var editingCommentDraft by remember(anime.id) { mutableStateOf(TextFieldValue("")) }
    var editingReplyId by remember(anime.id) { mutableStateOf<String?>(null) }
    var editingReplyDraft by remember(anime.id) { mutableStateOf(TextFieldValue("")) }

    var expandedThreads by remember(anime.id) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var highlightedTarget by remember(anime.id) { mutableStateOf<DesktopNotificationJumpTarget?>(null) }
    var scrolledToHighlightKey by remember(anime.id) { mutableStateOf<String?>(null) }

    var mentionSuggestions by remember(anime.id) { mutableStateOf<List<DesktopMentionUser>>(emptyList()) }
    var activeMentionQuery by remember(anime.id) { mutableStateOf<DesktopActiveMentionQuery?>(null) }

    val emojiById = remember(communityEmojis) { communityEmojis.associateBy { it.id.lowercase() } }
    val chronologicalComments = remember(comments) { comments.asReversed() }
    val draftEmojiIds = remember(draft.text) { DesktopEmojiCacheService.extractEmojiIds(draft.text) }

    fun openReplyFor(
        comment: DesktopAnimeComment,
        targetUserId: String,
        targetUsername: String,
        quotedReplyId: String? = null,
        quotedPreview: String = comment.body
    ) {
        val cleanSnippet = DesktopEmojiCacheService.formatPreviewSnippet(quotedPreview)
            .ifBlank { quotedPreview.trim() }
            .take(180)
        activeReplyTarget = DesktopLiveReplyTarget(
            commentId = comment.id,
            targetUserId = targetUserId,
            targetUsername = targetUsername,
            quotedReplyId = quotedReplyId,
            quotedUsername = targetUsername,
            quotedPreview = cleanSnippet
        )
        errorMsg = null
        comment.threads.firstOrNull { it.targetUserId == targetUserId }?.let { existing ->
            expandedThreads = expandedThreads + (existing.id to true)
        }
    }

    fun jumpToQuotedTarget(commentId: String, replyId: String?) {
        val targetComment = comments.firstOrNull { it.id == commentId }
        val owningThreadId = if (!replyId.isNullOrBlank()) {
            targetComment?.threads?.firstOrNull { th -> th.replies.any { it.id == replyId } }?.id
        } else null
        if (owningThreadId != null) {
            expandedThreads = expandedThreads + (owningThreadId to true)
        }
        scrolledToHighlightKey = null
        highlightedTarget = DesktopNotificationJumpTarget(
            animeId = anime.id,
            commentId = commentId,
            threadId = owningThreadId,
            replyId = replyId
        )
    }

    fun appendMentionToDraft(username: String) {
        if (!isAuthenticated || session.isGuest) return
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) return
        val cur = draft.text
        if (cur.contains("@$clean", ignoreCase = true) ||
            selectedTags.any { it.username.equals(clean, ignoreCase = true) }
        ) {
            return
        }
        val check = DesktopDiscussionRules.validateMentions(cur, selectedTags.size)
        if (check.totalMentions >= DesktopDiscussionRules.MAX_MENTIONS_PER_CONTENT) {
            onShowToast("Maximum ${DesktopDiscussionRules.MAX_MENTIONS_PER_CONTENT} mentions allowed.", true)
            return
        }
        val needsSpace = cur.isNotEmpty() && !cur.last().isWhitespace()
        val nextText = cur + (if (needsSpace) " " else "") + "@$clean "
        draft = TextFieldValue(nextText, TextRange(nextText.length))
        activeMentionQuery = null
    }

    fun loadPage(reset: Boolean) {
        val offset = if (reset) 0 else comments.size
        if (reset) {
            loading = true
            errorMsg = null
        } else {
            loadingMore = true
        }
        scope.launch {
            DesktopDiscussionService.fetchDiscussionPage(session, anime.id, limit = 15, offset = offset)
                .onSuccess { (page, active) ->
                    onSessionUpdated(active)
                    comments = if (reset) {
                        page.comments
                    } else {
                        comments + page.comments.filterNot { inc -> comments.any { it.id == inc.id } }
                    }
                    totalComments = page.totalComments
                    hasMore = page.hasMore
                    val nextExpanded = expandedThreads.toMutableMap()
                    page.comments.forEach { c ->
                        c.threads.forEach { th ->
                            if (!nextExpanded.containsKey(th.id) && th.replies.size <= 2) {
                                nextExpanded[th.id] = true
                            }
                        }
                    }
                    if (jumpTarget != null && jumpTarget.animeId == anime.id) {
                        jumpTarget.threadId?.let { nextExpanded[it] = true }
                        highlightedTarget = jumpTarget
                        onJumpTargetConsumed()
                    }
                    expandedThreads = nextExpanded
                }
                .onFailure { err ->
                    errorMsg = err.message ?: "Unable to load live chat."
                }
            loading = false
            loadingMore = false
        }
    }

    LaunchedEffect(anime.id, isAuthenticated) {
        loadPage(reset = true)
        launch {
            val loaded = DesktopEmojiCacheService.getEmojis(session)
            if (loaded.isNotEmpty()) {
                communityEmojis = loaded
                DesktopEmojiCacheService.preloadTopEmojis(loaded, count = 8)
            }
        }
        if (isAuthenticated && !session.isGuest) {
            DesktopDiscussionService.markAnimeNotificationsRead(session, anime.id)
                .onSuccess { onMarkedAnimeSeen(anime.id) }
        }
    }

    LaunchedEffect(jumpTarget) {
        if (jumpTarget != null && jumpTarget.animeId == anime.id) {
            jumpTarget.threadId?.let { tid ->
                expandedThreads = expandedThreads + (tid to true)
            }
            highlightedTarget = jumpTarget
            onJumpTargetConsumed()
        }
    }

    LaunchedEffect(highlightedTarget, chronologicalComments) {
        val target = highlightedTarget ?: return@LaunchedEffect
        val key = "${target.commentId.orEmpty()}_${target.replyId.orEmpty()}"
        if (key == "_" || scrolledToHighlightKey == key) return@LaunchedEffect
        val idx = chronologicalComments.indexOfFirst { it.id == target.commentId }
        if (idx >= 0) {
            scrolledToHighlightKey = key
            listState.animateScrollToItem(idx)
        }
    }

    LaunchedEffect(initialReplyUser, loading, comments) {
        val target = initialReplyUser ?: return@LaunchedEffect
        if (loading) return@LaunchedEffect
        val existingComment = comments.firstOrNull { c ->
            (target.userId.isNotBlank() && c.userId == target.userId) ||
                c.username.equals(target.username, ignoreCase = true)
        }
        if (existingComment != null) {
            openReplyFor(
                comment = existingComment,
                targetUserId = existingComment.userId,
                targetUsername = existingComment.username,
                quotedPreview = existingComment.body
            )
        } else if (target.userId.isNotBlank()) {
            val check = DesktopDiscussionRules.validateMentions(draft.text, selectedTags.size)
            if (check.totalMentions < DesktopDiscussionRules.MAX_MENTIONS_PER_CONTENT &&
                selectedTags.none { it.userId == target.userId }
            ) {
                selectedTags = selectedTags + target
            }
        }
        onInitialReplyUserConsumed()
    }

    LaunchedEffect(activeMentionQuery, isAuthenticated) {
        val q = activeMentionQuery
        if (!isAuthenticated || session.isGuest || q == null) {
            mentionSuggestions = emptyList()
            return@LaunchedEffect
        }
        delay(110L)
        DesktopDiscussionService.searchMentionableUsers(session, q.query, limit = 8)
            .onSuccess { mentionSuggestions = it }
            .onFailure { mentionSuggestions = emptyList() }
    }

    val draftValidation = remember(draft.text, selectedTags) {
        DesktopDiscussionRules.validateMentions(draft.text, selectedTags.size)
    }

    val insertEmoji: (DesktopCommunityEmoji) -> Unit = { emoji ->
        val token = "[emoji:${emoji.id}] "
        val cur = draft.text
        val pos = draft.selection.end.coerceIn(0, cur.length)
        val prefix = cur.substring(0, pos)
        val needsSpace = prefix.isNotEmpty() && !prefix.endsWith(" ")
        val inserted = (if (needsSpace) " " else "") + token
        draft = TextFieldValue(prefix + inserted + cur.substring(pos), TextRange(pos + inserted.length))
    }

    val sendAction: () -> Unit = {
        val clean = draft.text.trim()
        if (!submitting && clean.isNotEmpty() && draftValidation.isValid) {
            submitting = true
            errorMsg = null
            val tagIds = selectedTags.map { it.userId }
            val replyTarget = activeReplyTarget
            val usedEmojiIds = DesktopEmojiCacheService.extractEmojiIds(clean)
            scope.launch {
                if (replyTarget != null) {
                    DesktopDiscussionService.createThreadReply(
                        session = session,
                        commentId = replyTarget.commentId,
                        targetUserId = replyTarget.targetUserId,
                        body = clean,
                        taggedUserIds = tagIds,
                        quotedReplyId = replyTarget.quotedReplyId,
                        quotedUsername = replyTarget.quotedUsername,
                        quotedPreview = replyTarget.quotedPreview
                    ).onSuccess { (thread, reply, active) ->
                        onSessionUpdated(active)
                        val mergedReply = reply.copy(
                            parentCommentId = reply.parentCommentId
                                ?: reply.quotedReplyId
                                ?: replyTarget.quotedReplyId
                                ?: replyTarget.commentId,
                            quotedCommentId = reply.quotedCommentId ?: replyTarget.commentId,
                            quotedReplyId = reply.quotedReplyId ?: replyTarget.quotedReplyId,
                            quotedUsername = reply.quotedUsername?.takeIf { it.isNotBlank() } ?: replyTarget.quotedUsername,
                            quotedPreview = reply.quotedPreview?.takeIf { it.isNotBlank() } ?: replyTarget.quotedPreview
                        )
                        comments = DesktopDiscussionRules.mergeReplyIntoComments(comments, replyTarget.commentId, thread, mergedReply)
                        expandedThreads = expandedThreads + (thread.id to true)
                        draft = TextFieldValue("")
                        selectedTags = emptyList()
                        activeReplyTarget = null
                        activeMentionQuery = null
                        isEmojiTrayOpen = false
                        if (usedEmojiIds.isNotEmpty()) {
                            communityEmojis = DesktopEmojiCacheService.recordUsage(active, usedEmojiIds)
                        }
                    }.onFailure { err ->
                        errorMsg = err.message ?: "Failed to send reply."
                        onShowToast(errorMsg!!, true)
                    }
                } else {
                    DesktopDiscussionService.createComment(
                        session = session,
                        animeId = anime.id,
                        body = clean,
                        animeTitle = anime.title,
                        animeCover = anime.coverImage,
                        taggedUserIds = tagIds
                    ).onSuccess { (created, active) ->
                        onSessionUpdated(active)
                        comments = listOf(created) + comments.filterNot { it.id == created.id }
                        totalComments = maxOf(comments.size, totalComments + 1)
                        draft = TextFieldValue("")
                        selectedTags = emptyList()
                        activeMentionQuery = null
                        isEmojiTrayOpen = false
                        if (usedEmojiIds.isNotEmpty()) {
                            communityEmojis = DesktopEmojiCacheService.recordUsage(active, usedEmojiIds)
                        }
                    }.onFailure { err ->
                        errorMsg = err.message ?: "Failed to send message."
                        onShowToast(errorMsg!!, true)
                    }
                }
                submitting = false
            }
        }
    }

    val rootMod = if (inModal) {
        Modifier.fillMaxSize().background(Color(0xFF030304))
    } else {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0E0E16))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
    }

    Column(modifier = rootMod) {
        if (!inModal) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(ChampagneGold))
                    Text("LIVE CHAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, color = ChampagneGold)
                }
                Text("$totalComments ${if (totalComments == 1) "message" else "messages"}", fontSize = 12.sp, color = WarmSilver)
            }
            HorizontalDivider(color = Color(0x14FFFFFF))
        }

        errorMsg?.let {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                DesktopFeedbackBanner(message = it, isError = true)
            }
        }

        val streamModifier = if (inModal) {
            Modifier.weight(1f).fillMaxWidth()
        } else {
            Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 440.dp)
        }

        Box(modifier = streamModifier) {
            when {
                loading && chronologicalComments.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ChampagneGold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
                chronologicalComments.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No messages yet. Say hello or drop a custom emoji below!",
                            color = WarmSilver,
                            fontSize = 13.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (hasMore) {
                            item(key = "load_older") {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF14141E),
                                        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                                        modifier = Modifier.clip(CircleShape).clickable(enabled = !loadingMore) {
                                            loadPage(reset = false)
                                        }
                                    ) {
                                        Text(
                                            if (loadingMore) "Loading older..." else "Load older messages",
                                            color = ChampagneGoldSoft,
                                            fontSize = 11.5.sp,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(chronologicalComments, key = { it.id }) { comment ->
                            val isMine = session.userId.isNotBlank() && session.userId == comment.userId
                            val isHighlighted = highlightedTarget?.commentId == comment.id && highlightedTarget?.replyId == null
                            val isEditingThis = editingCommentId == comment.id
                            var menuExpanded by remember(comment.id) { mutableStateOf(false) }
                            val bubbleShape = if (isMine) {
                                RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
                            } else {
                                RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
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
                                            DesktopAvatarImage(
                                                avatarKey = comment.avatarKey,
                                                username = comment.username,
                                                modifier = Modifier.padding(top = 2.dp, end = 10.dp).size(32.dp).clip(CircleShape)
                                            )
                                        }
                                        Column(
                                            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                                            modifier = Modifier.widthIn(max = 440.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                if (!isMine) {
                                                    Text(comment.username, color = ChampagneGoldSoft, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Text(
                                                    DesktopDiscussionRules.formatRelativeTime(comment.createdAt),
                                                    color = Color(0xFF66667A),
                                                    fontSize = 10.sp
                                                )
                                            }
                                            val soloCommentEmojiId = remember(comment.body, comment.mentions, isEditingThis) {
                                                if (!isEditingThis && comment.mentions.isEmpty()) {
                                                    DesktopEmojiCacheService.extractSoloEmojiId(comment.body)
                                                } else null
                                            }
                                            val soloCommentEmoji = soloCommentEmojiId?.let { emojiById[it] }
                                            if (soloCommentEmojiId != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .then(
                                                            if (isHighlighted) {
                                                                Modifier
                                                                    .background(ChampagneGold.copy(alpha = 0.16f))
                                                                    .border(1.5.dp, ChampagneGold, RoundedCornerShape(14.dp))
                                                                    .padding(4.dp)
                                                            } else Modifier
                                                        )
                                                        .pointerInput(comment.id, isAuthenticated) {
                                                            awaitPointerEventScope {
                                                                while (true) {
                                                                    val ev = awaitPointerEvent()
                                                                    if (ev.type == PointerEventType.Press && ev.buttons.isSecondaryPressed && isAuthenticated && !session.isGuest) {
                                                                        menuExpanded = true
                                                                    }
                                                                }
                                                            }
                                                        }
                                                ) {
                                                    if (soloCommentEmoji != null) {
                                                        DesktopCachedEmojiImage(emoji = soloCommentEmoji, size = 90.dp)
                                                    } else {
                                                        Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(ChampagneGold.copy(alpha = 0.12f)))
                                                    }
                                                }
                                            } else {
                                                Surface(
                                                    shape = bubbleShape,
                                                    color = when {
                                                        isHighlighted -> ChampagneGold.copy(alpha = 0.24f)
                                                        isMine -> Color(0x26E8C88F)
                                                        else -> Color(0x0DFFFFFF)
                                                    },
                                                    border = BorderStroke(
                                                        1.dp,
                                                        when {
                                                            isHighlighted -> ChampagneGold
                                                            isMine -> Color(0x4DE8C88F)
                                                            else -> Color(0x14FFFFFF)
                                                        }
                                                    ),
                                                    modifier = Modifier
                                                        .clip(bubbleShape)
                                                        .pointerInput(comment.id, isAuthenticated) {
                                                            awaitPointerEventScope {
                                                                while (true) {
                                                                    val ev = awaitPointerEvent()
                                                                    if (ev.type == PointerEventType.Press && ev.buttons.isSecondaryPressed && isAuthenticated && !session.isGuest) {
                                                                        menuExpanded = true
                                                                    }
                                                                }
                                                            }
                                                        }
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        if (isEditingThis) {
                                                            OutlinedTextField(
                                                                value = editingCommentDraft,
                                                                onValueChange = { editingCommentDraft = it },
                                                                maxLines = 4,
                                                                shape = RoundedCornerShape(12.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                Button(
                                                                    onClick = {
                                                                        val updated = editingCommentDraft.text.trim()
                                                                        if (updated.isNotEmpty() && !submitting) {
                                                                            submitting = true
                                                                            scope.launch {
                                                                                DesktopDiscussionService.updateComment(session, comment.id, updated)
                                                                                    .onSuccess { (u, active) ->
                                                                                        onSessionUpdated(active)
                                                                                        comments = comments.map { if (it.id == comment.id) it.copy(body = u.body, updatedAt = u.updatedAt, mentions = u.mentions) else it }
                                                                                        editingCommentId = null
                                                                                    }
                                                                                    .onFailure { onShowToast(it.message ?: "Update failed", true) }
                                                                                submitting = false
                                                                            }
                                                                        }
                                                                    },
                                                                    shape = CircleShape,
                                                                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                                                                ) { Text("Save", fontWeight = FontWeight.Bold) }
                                                                TextButton(onClick = { editingCommentId = null }) { Text("Cancel", color = WarmSilver) }
                                                            }
                                                        } else {
                                                            DesktopRichChatText(
                                                                content = comment.body,
                                                                emojiById = emojiById,
                                                                onMentionClick = if (isAuthenticated && !session.isGuest) ::appendMentionToDraft else null
                                                            )
                                                            DesktopClickableMentionChipsRow(
                                                                mentions = comment.mentions,
                                                                enabled = isAuthenticated && !session.isGuest,
                                                                onMentionClick = ::appendMentionToDraft
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (isAuthenticated && !session.isGuest) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                                                ) {
                                                    Text(
                                                        "Reply",
                                                        color = Color(0xFF66667A),
                                                        fontSize = 10.5.sp,
                                                        modifier = Modifier.clickable {
                                                            openReplyFor(comment, comment.userId, comment.username, quotedPreview = comment.body)
                                                        }
                                                    )
                                                    if (isMine && !isEditingThis) {
                                                        Text(
                                                            "Edit",
                                                            color = Color(0xFF66667A),
                                                            fontSize = 10.5.sp,
                                                            modifier = Modifier.clickable {
                                                                editingCommentId = comment.id
                                                                editingCommentDraft = TextFieldValue(comment.body)
                                                            }
                                                        )
                                                        Text(
                                                            "Delete",
                                                            color = CrimsonWaste.copy(alpha = 0.85f),
                                                            fontSize = 10.5.sp,
                                                            modifier = Modifier.clickable {
                                                                scope.launch {
                                                                    DesktopDiscussionService.deleteComment(session, comment.id)
                                                                        .onSuccess { active ->
                                                                            onSessionUpdated(active)
                                                                            comments = comments.filterNot { it.id == comment.id }
                                                                            totalComments = (totalComments - 1).coerceAtLeast(0)
                                                                        }
                                                                        .onFailure { onShowToast(it.message ?: "Delete failed", true) }
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        containerColor = Color(0xFF14141E)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Reply to @${comment.username}", color = IvoryText) },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                menuExpanded = false
                                                openReplyFor(comment, comment.userId, comment.username, quotedPreview = comment.body)
                                            }
                                        )
                                        if (isMine && !isEditingThis) {
                                            DropdownMenuItem(
                                                text = { Text("Edit Message", color = IvoryText) },
                                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = WarmSilver, modifier = Modifier.size(16.dp)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    editingCommentId = comment.id
                                                    editingCommentDraft = TextFieldValue(comment.body)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete Message", color = CrimsonWaste) },
                                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = CrimsonWaste, modifier = Modifier.size(16.dp)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    scope.launch {
                                                        DesktopDiscussionService.deleteComment(session, comment.id)
                                                            .onSuccess { active ->
                                                                onSessionUpdated(active)
                                                                comments = comments.filterNot { it.id == comment.id }
                                                                totalComments = (totalComments - 1).coerceAtLeast(0)
                                                            }
                                                            .onFailure { onShowToast(it.message ?: "Delete failed", true) }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                // Compact Expandable Threads
                                if (comment.threads.isNotEmpty()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(start = if (isMine) 32.dp else 42.dp)
                                    ) {
                                        comment.threads.forEach { thread ->
                                            val isExpanded = expandedThreads[thread.id] ?: (highlightedTarget?.threadId == thread.id)
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = Color(0xFF0E0E16),
                                                border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().clickable {
                                                            expandedThreads = expandedThreads + (thread.id to !isExpanded)
                                                        },
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            "Thread • @${thread.targetUsername} (${thread.replies.size})",
                                                            color = ChampagneGoldSoft,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Icon(
                                                            imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                                            contentDescription = null,
                                                            tint = ChampagneGold,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    if (isExpanded) {
                                                        thread.replies.forEach { reply ->
                                                            val isReplyMine = session.userId.isNotBlank() && session.userId == reply.userId
                                                            val isReplyHighlighted = highlightedTarget?.replyId == reply.id
                                                            val isEditingThisReply = editingReplyId == reply.id
                                                            val soloReplyEmojiId = remember(reply.body, reply.mentions, isEditingThisReply) {
                                                                if (!isEditingThisReply && reply.mentions.isEmpty()) {
                                                                    DesktopEmojiCacheService.extractSoloEmojiId(reply.body)
                                                                } else null
                                                            }
                                                            val soloReplyEmoji = soloReplyEmojiId?.let { emojiById[it] }
                                                            val refReplyId = reply.quotedReplyId?.takeIf { it.isNotBlank() }
                                                                ?: reply.parentCommentId?.takeIf { it.isNotBlank() && it != comment.id }
                                                            val matchedTargetReply = remember(refReplyId, comment.threads) {
                                                                if (refReplyId != null) {
                                                                    comment.threads.asSequence().flatMap { it.replies.asSequence() }.firstOrNull { it.id == refReplyId }
                                                                } else null
                                                            }
                                                            val qUser = if (refReplyId != null) {
                                                                matchedTargetReply?.username?.takeIf { it.isNotBlank() }
                                                                    ?: reply.quotedUsername?.takeIf { it.isNotBlank() }
                                                                    ?: "Member"
                                                            } else {
                                                                reply.quotedUsername?.takeIf { it.isNotBlank() } ?: comment.username
                                                            }
                                                            val qPrev = if (refReplyId != null) {
                                                                reply.quotedPreview?.takeIf { it.isNotBlank() }
                                                                    ?: matchedTargetReply?.body?.takeIf { it.isNotBlank() }
                                                                    ?: "Original reply unavailable"
                                                            } else {
                                                                reply.quotedPreview?.takeIf { it.isNotBlank() } ?: comment.body
                                                            }
                                                            val qCommentId = reply.quotedCommentId ?: comment.id
                                                            val qReplyId = refReplyId
                                                            val replyActionsContent: @Composable () -> Unit = {
                                                                if (isAuthenticated && !session.isGuest && !isEditingThisReply) {
                                                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                                        Text(
                                                                            "Reply",
                                                                            color = Color(0xFF66667A),
                                                                            fontSize = 10.sp,
                                                                            modifier = Modifier.clickable {
                                                                                openReplyFor(
                                                                                    comment = comment,
                                                                                    targetUserId = reply.userId,
                                                                                    targetUsername = reply.username,
                                                                                    quotedReplyId = reply.id,
                                                                                    quotedPreview = reply.body
                                                                                )
                                                                            }
                                                                        )
                                                                        if (isReplyMine) {
                                                                            Text(
                                                                                "Edit",
                                                                                color = Color(0xFF66667A),
                                                                                fontSize = 10.sp,
                                                                                modifier = Modifier.clickable {
                                                                                    editingReplyId = reply.id
                                                                                    editingReplyDraft = TextFieldValue(reply.body)
                                                                                }
                                                                            )
                                                                            Text(
                                                                                "Delete",
                                                                                color = CrimsonWaste.copy(alpha = 0.85f),
                                                                                fontSize = 10.sp,
                                                                                modifier = Modifier.clickable {
                                                                                    scope.launch {
                                                                                        DesktopDiscussionService.deleteThreadReply(session, reply.id)
                                                                                            .onSuccess { active ->
                                                                                                onSessionUpdated(active)
                                                                                                comments = comments.map { c ->
                                                                                                    if (c.id != comment.id) c else c.copy(
                                                                                                        threads = c.threads.mapNotNull { th ->
                                                                                                            if (th.id != thread.id) th else {
                                                                                                                val rem = th.replies.filterNot { it.id == reply.id }
                                                                                                                if (rem.isEmpty()) null else th.copy(replies = rem, replyCount = rem.size)
                                                                                                            }
                                                                                                        }
                                                                                                    )
                                                                                                }
                                                                                            }
                                                                                            .onFailure { onShowToast(it.message ?: "Delete failed", true) }
                                                                                    }
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            if (soloReplyEmojiId != null) {
                                                                Column(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clip(RoundedCornerShape(10.dp))
                                                                        .then(
                                                                            if (isReplyHighlighted) {
                                                                                Modifier
                                                                                    .background(ChampagneGold.copy(alpha = 0.18f))
                                                                                    .border(1.dp, ChampagneGold, RoundedCornerShape(10.dp))
                                                                                    .padding(8.dp)
                                                                            } else {
                                                                                Modifier.padding(vertical = 2.dp)
                                                                            }
                                                                        ),
                                                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                                                ) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                    ) {
                                                                        DesktopAvatarImage(
                                                                            avatarKey = reply.avatarKey,
                                                                            username = reply.username,
                                                                            modifier = Modifier.size(18.dp).clip(CircleShape)
                                                                        )
                                                                        Text(reply.username, color = ChampagneGoldSoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                                        Text(DesktopDiscussionRules.formatRelativeTime(reply.createdAt), color = Color(0xFF66667A), fontSize = 9.5.sp)
                                                                    }
                                                                    DesktopQuotedReplyPreviewCard(
                                                                        quotedUsername = qUser,
                                                                        quotedPreview = qPrev,
                                                                        onClick = { jumpToQuotedTarget(qCommentId, qReplyId) }
                                                                    )
                                                                    if (soloReplyEmoji != null) {
                                                                        DesktopCachedEmojiImage(emoji = soloReplyEmoji, size = 90.dp)
                                                                    } else {
                                                                        Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(ChampagneGold.copy(alpha = 0.12f)))
                                                                    }
                                                                    replyActionsContent()
                                                                }
                                                            } else {
                                                                Surface(
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    color = when {
                                                                        isReplyHighlighted -> ChampagneGold.copy(alpha = 0.22f)
                                                                        isReplyMine -> Color(0x1FE8C88F)
                                                                        else -> Color(0xFF14141E)
                                                                    },
                                                                    border = BorderStroke(1.dp, if (isReplyHighlighted) ChampagneGold else Color(0x14FFFFFF)),
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Column(
                                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                                                    ) {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                        ) {
                                                                            DesktopAvatarImage(
                                                                                avatarKey = reply.avatarKey,
                                                                                username = reply.username,
                                                                                modifier = Modifier.size(18.dp).clip(CircleShape)
                                                                            )
                                                                            Text(reply.username, color = ChampagneGoldSoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                                            Text(DesktopDiscussionRules.formatRelativeTime(reply.createdAt), color = Color(0xFF66667A), fontSize = 9.5.sp)
                                                                        }
                                                                        if (!isEditingThisReply) {
                                                                            DesktopQuotedReplyPreviewCard(
                                                                                quotedUsername = qUser,
                                                                                quotedPreview = qPrev,
                                                                                onClick = { jumpToQuotedTarget(qCommentId, qReplyId) }
                                                                            )
                                                                        }
                                                                        if (isEditingThisReply) {
                                                                            OutlinedTextField(
                                                                                value = editingReplyDraft,
                                                                                onValueChange = { editingReplyDraft = it },
                                                                                maxLines = 3,
                                                                                modifier = Modifier.fillMaxWidth()
                                                                            )
                                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                                Button(
                                                                                    onClick = {
                                                                                        val upd = editingReplyDraft.text.trim()
                                                                                        if (upd.isNotEmpty() && !submitting) {
                                                                                            submitting = true
                                                                                            scope.launch {
                                                                                                DesktopDiscussionService.updateThreadReply(session, reply.id, upd)
                                                                                                    .onSuccess { (u, active) ->
                                                                                                        onSessionUpdated(active)
                                                                                                        comments = comments.map { c ->
                                                                                                            if (c.id != comment.id) c else c.copy(
                                                                                                                threads = c.threads.map { th ->
                                                                                                                    if (th.id != thread.id) th else th.copy(
                                                                                                                        replies = th.replies.map { r -> if (r.id == reply.id) u else r }
                                                                                                                    )
                                                                                                                }
                                                                                                            )
                                                                                                        }
                                                                                                        editingReplyId = null
                                                                                                    }
                                                                                                    .onFailure { onShowToast(it.message ?: "Update failed", true) }
                                                                                                submitting = false
                                                                                            }
                                                                                        }
                                                                                    },
                                                                                    shape = CircleShape,
                                                                                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                                                                                ) { Text("Save", fontWeight = FontWeight.Bold) }
                                                                                TextButton(onClick = { editingReplyId = null }) { Text("Cancel", color = WarmSilver) }
                                                                            }
                                                                        } else {
                                                                            DesktopRichChatText(
                                                                                content = reply.body,
                                                                                emojiById = emojiById,
                                                                                fontSizeSp = 12.5f,
                                                                                onMentionClick = if (isAuthenticated && !session.isGuest) ::appendMentionToDraft else null
                                                                            )
                                                                            DesktopClickableMentionChipsRow(
                                                                                mentions = reply.mentions,
                                                                                enabled = isAuthenticated && !session.isGuest,
                                                                                onMentionClick = ::appendMentionToDraft
                                                                            )
                                                                        }
                                                                        replyActionsContent()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (isAuthenticated && !session.isGuest) {
                                                            Text(
                                                                "↳ Reply in thread",
                                                                color = ChampagneGold,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                modifier = Modifier.clickable {
                                                                    val lastReply = thread.replies.lastOrNull()
                                                                    openReplyFor(
                                                                        comment = comment,
                                                                        targetUserId = lastReply?.userId ?: thread.targetUserId,
                                                                        targetUsername = lastReply?.username ?: thread.targetUsername,
                                                                        quotedReplyId = lastReply?.id,
                                                                        quotedPreview = lastReply?.body ?: comment.body
                                                                    )
                                                                }
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
                    }
                }
            }
        }

        // Bottom Docked Composer Area
        if (isAuthenticated && !session.isGuest) {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0E0E16))) {
                HorizontalDivider(color = Color(0x14FFFFFF))

                if (mentionSuggestions.isNotEmpty() && activeMentionQuery != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF14141E),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            mentionSuggestions.take(6).forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val currentTotal = DesktopDiscussionRules.extractMentionTokens(draft.text).size + selectedTags.size
                                            if (currentTotal < DesktopDiscussionRules.MAX_MENTIONS_PER_CONTENT) {
                                                draft = DesktopDiscussionRules.stripMentionTrigger(draft, activeMentionQuery!!)
                                                if (selectedTags.none { it.userId == user.userId }) {
                                                    selectedTags = selectedTags + user
                                                }
                                            }
                                            activeMentionQuery = null
                                            mentionSuggestions = emptyList()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DesktopAvatarImage(avatarKey = user.avatarKey, username = user.username, modifier = Modifier.size(24.dp).clip(CircleShape))
                                    Text("@${user.username}", color = ChampagneGoldSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Icon(Icons.Outlined.AlternateEmail, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                val rTarget = activeReplyTarget
                if (rTarget != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ChampagneGold.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(ChampagneGold)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                "Replying to @${rTarget.quotedUsername}",
                                color = ChampagneGoldSoft,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (rTarget.quotedPreview.isNotBlank()) {
                                Text(
                                    rTarget.quotedPreview,
                                    color = WarmSilver,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Cancel reply",
                            tint = WarmSilver,
                            modifier = Modifier.size(16.dp).clickable { activeReplyTarget = null }
                        )
                    }
                }

                if (selectedTags.isNotEmpty() || draftEmojiIds.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        selectedTags.forEachIndexed { idx, user ->
                            Surface(
                                shape = CircleShape,
                                color = ChampagneGold.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("@${user.username}", color = ChampagneGoldSoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Remove",
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(12.dp).clickable {
                                            selectedTags = selectedTags.filterIndexed { i, _ -> i != idx }
                                        }
                                    )
                                }
                            }
                        }
                        draftEmojiIds.distinct().take(6).forEach { eid ->
                            val e = emojiById[eid]
                            if (e != null) {
                                Surface(shape = CircleShape, color = Color(0xFF14141E), border = BorderStroke(1.dp, Color(0x14FFFFFF))) {
                                    Box(modifier = Modifier.padding(3.dp)) { DesktopCachedEmojiImage(emoji = e, size = 20.dp) }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = isEmojiTrayOpen) {
                    DesktopEmojiTray(emojis = communityEmojis, onSelectEmoji = insertEmoji)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isEmojiTrayOpen) ChampagneGold.copy(alpha = 0.18f) else Color(0xFF14141E),
                        border = BorderStroke(1.dp, if (isEmojiTrayOpen) ChampagneGold.copy(alpha = 0.55f) else Color(0x14FFFFFF)),
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable {
                            isEmojiTrayOpen = !isEmojiTrayOpen
                            if (isEmojiTrayOpen && communityEmojis.isEmpty()) {
                                scope.launch {
                                    val loaded = DesktopEmojiCacheService.getEmojis(session, forceRefresh = true)
                                    if (loaded.isNotEmpty()) communityEmojis = loaded
                                }
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SentimentSatisfiedAlt,
                                contentDescription = "Toggle custom emojis",
                                tint = if (isEmojiTrayOpen) ChampagneGold else WarmSilver,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF14141E),
                        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                        modifier = Modifier.weight(1f).heightIn(min = 42.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (draft.text.isEmpty()) {
                                Text(
                                    if (rTarget != null) "Reply to @${rTarget.targetUsername}..." else "Message...",
                                    color = Color(0xFF66667A),
                                    fontSize = 13.5.sp
                                )
                            }
                            BasicTextField(
                                value = draft,
                                onValueChange = { next ->
                                    draft = next
                                    activeMentionQuery = DesktopDiscussionRules.detectActiveMentionQuery(next.text, next.selection.end)
                                },
                                enabled = !submitting,
                                textStyle = TextStyle(color = IvoryText, fontSize = 13.5.sp, lineHeight = 19.sp),
                                cursorBrush = SolidColor(ChampagneGold),
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val canSend = !submitting && draft.text.trim().isNotEmpty() && draftValidation.isValid
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) ChampagneGold else Color(0xFF14141E),
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(enabled = canSend, onClick = sendAction)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (submitting) {
                                CircularProgressIndicator(strokeWidth = 2.dp, color = Color(0xFF030304), modifier = Modifier.size(16.dp))
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (canSend) Color(0xFF030304) else Color(0xFF66667A),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "Sign in to chat, tag members with @, and use custom emojis.",
                color = WarmSilver,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
internal fun DesktopTaggedYouSection(
    notifications: List<DesktopUserNotification>,
    unreadCount: Int,
    loading: Boolean,
    hasMore: Boolean,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenNotification: (DesktopUserNotification) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TAGGED YOU", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, color = ChampagneGold)
                Text(
                    if (unreadCount > 0) "$unreadCount unread mention${if (unreadCount == 1) "" else "s"} — click to jump directly to the anime template"
                    else "All caught up on community mentions",
                    color = WarmSilver,
                    fontSize = 12.5.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRefresh,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = WarmSilver, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh", color = IvoryText, fontSize = 12.sp)
                }
                if (unreadCount > 0) {
                    Button(
                        onClick = onMarkAllRead,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark all read", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        when {
            loading && notifications.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChampagneGold)
                }
            }
            notifications.isEmpty() -> {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(36.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.AlternateEmail, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(30.dp))
                        Text("No tags yet", color = IvoryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "When someone tags you in any community anime template (new or old), it appears here for one-click jump.",
                            color = WarmSilver,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 48.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notifications, key = { it.id }) { n ->
                        val isUnread = !n.isRead
                        val cleanPreview = remember(n.contentPreview) {
                            DesktopEmojiCacheService.formatPreviewSnippet(n.contentPreview)
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isUnread) Color(0xFF14141E) else ObsidianCard,
                            border = BorderStroke(
                                1.dp,
                                if (isUnread) Color(0xFFEF4444).copy(alpha = 0.55f) else ObsidianBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onOpenNotification(n) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (n.animeCover.isNotBlank()) {
                                    NetworkPosterImage(
                                        url = n.animeCover,
                                        contentDescription = n.animeTitle,
                                        modifier = Modifier
                                            .size(width = 56.dp, height = 80.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isUnread) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                            }
                                            Surface(shape = CircleShape, color = ChampagneGold.copy(alpha = 0.14f)) {
                                                Text(
                                                    n.communityName.uppercase(),
                                                    color = ChampagneGold,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            DesktopDiscussionRules.formatRelativeTime(n.createdAt),
                                            color = Color(0xFF66667A),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        n.animeTitle.ifBlank { "Anime #${n.animeId}" },
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 20.sp,
                                        color = IvoryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        DesktopAvatarImage(
                                            avatarKey = n.senderAvatarKey,
                                            username = n.senderUsername,
                                            modifier = Modifier.size(20.dp).clip(CircleShape)
                                        )
                                        val actionText = if (n.type == "thread_reply") "replied to you" else "tagged you"
                                        Text(
                                            "@${n.senderUsername} $actionText",
                                            color = if (isUnread) ChampagneGoldSoft else WarmSilver,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (cleanPreview.isNotBlank()) {
                                        Text(
                                            "“$cleanPreview”",
                                            color = WarmSilver,
                                            fontSize = 12.5.sp,
                                            fontStyle = FontStyle.Italic,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (hasMore) {
                        item(key = "load_more_tags") {
                            OutlinedButton(
                                onClick = onLoadMore,
                                enabled = !loading,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (loading) "Loading..." else "Load older tags", color = ChampagneGold)
                            }
                        }
                    }
                }
            }
        }
    }
}
