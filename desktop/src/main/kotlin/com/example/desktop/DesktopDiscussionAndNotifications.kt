package com.example.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DesktopMentionUser(
    val userId: String,
    val username: String,
    val avatarKey: String = "avatar_1"
)

data class DesktopThreadReply(
    val id: String,
    val threadId: String,
    val commentId: String,
    val animeId: Int,
    val userId: String,
    val username: String,
    val avatarKey: String = "avatar_1",
    val body: String,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class DesktopReplyThread(
    val id: String,
    val commentId: String,
    val animeId: Int,
    val targetUserId: String,
    val targetUsername: String,
    val targetAvatarKey: String = "avatar_1",
    val replyCount: Int = 0,
    val replies: List<DesktopThreadReply> = emptyList()
)

data class DesktopAnimeComment(
    val id: String,
    val animeId: Int,
    val userId: String,
    val username: String,
    val avatarKey: String = "avatar_1",
    val body: String,
    val createdAt: String = "",
    val updatedAt: String = "",
    val threads: List<DesktopReplyThread> = emptyList()
)

data class DesktopDiscussionPage(
    val comments: List<DesktopAnimeComment>,
    val totalComments: Int,
    val hasMore: Boolean
)

data class DesktopUserNotification(
    val id: String,
    val type: String,
    val animeId: Int,
    val animeTitle: String = "",
    val animeCover: String = "",
    val commentId: String? = null,
    val threadId: String? = null,
    val replyId: String? = null,
    val senderUserId: String = "",
    val senderUsername: String = "Member",
    val senderAvatarKey: String = "avatar_1",
    val contentPreview: String = "",
    val isRead: Boolean = false,
    val createdAt: String = ""
)

data class DesktopNotificationsPage(
    val notifications: List<DesktopUserNotification>,
    val unreadCount: Int,
    val totalCount: Int,
    val hasMore: Boolean
)

data class DesktopNotificationJumpTarget(
    val animeId: Int,
    val commentId: String? = null,
    val threadId: String? = null,
    val replyId: String? = null
)

data class DesktopMentionValidation(
    val isValid: Boolean,
    val totalMentions: Int,
    val uniqueUserCount: Int,
    val error: String? = null
)

data class DesktopActiveMentionQuery(
    val query: String,
    val startIndex: Int,
    val endIndex: Int
)

object DesktopDiscussionRules {
    const val MAX_MENTIONS_PER_CONTENT = 5
    private val MENTION_REGEX = Regex("""(^|[^A-Za-z0-9_])@([A-Za-z0-9_]{2,32})\b""")
    private val ACTIVE_QUERY_REGEX = Regex("""(?:^|[^A-Za-z0-9_])@([A-Za-z0-9_]{0,32})$""")

    data class MentionToken(val username: String, val start: Int, val end: Int)

    fun extractMentionTokens(text: String): List<MentionToken> {
        val result = mutableListOf<MentionToken>()
        MENTION_REGEX.findAll(text).forEach { match ->
            val prefixLen = match.groupValues[1].length
            val username = match.groupValues[2]
            val start = match.range.first + prefixLen
            result += MentionToken(username = username, start = start, end = start + 1 + username.length)
        }
        return result
    }

    fun validateMentions(text: String): DesktopMentionValidation {
        val tokens = extractMentionTokens(text)
        val unique = tokens.map { it.username.lowercase(Locale.US) }.toSet().size
        return if (tokens.size > MAX_MENTIONS_PER_CONTENT) {
            DesktopMentionValidation(
                isValid = false,
                totalMentions = tokens.size,
                uniqueUserCount = unique,
                error = "Maximum $MAX_MENTIONS_PER_CONTENT @mention entries allowed per comment or reply (currently ${tokens.size})."
            )
        } else {
            DesktopMentionValidation(
                isValid = true,
                totalMentions = tokens.size,
                uniqueUserCount = unique
            )
        }
    }

    fun detectActiveMentionQuery(text: String, cursor: Int): DesktopActiveMentionQuery? {
        val safeCursor = cursor.coerceIn(0, text.length)
        val before = text.substring(0, safeCursor)
        val match = ACTIVE_QUERY_REGEX.find(before) ?: return null
        val query = match.groupValues[1]
        val start = safeCursor - query.length - 1
        return DesktopActiveMentionQuery(query = query, startIndex = start, endIndex = safeCursor)
    }

    fun insertAutocomplete(current: TextFieldValue, active: DesktopActiveMentionQuery, username: String): TextFieldValue {
        val prefix = current.text.substring(0, active.startIndex.coerceIn(0, current.text.length))
        val suffix = current.text.substring(active.endIndex.coerceIn(0, current.text.length))
        val needsSpace = suffix.isEmpty() || !suffix[0].isWhitespace()
        val insertion = "@$username${if (needsSpace) " " else ""}"
        val nextText = prefix + insertion + suffix
        val nextCursor = prefix.length + insertion.length
        return TextFieldValue(text = nextText, selection = TextRange(nextCursor))
    }

    fun buildHighlightedText(text: String, highlightColor: Color): AnnotatedString {
        val tokens = extractMentionTokens(text)
        if (tokens.isEmpty()) return AnnotatedString(text)
        return buildAnnotatedString {
            var cursor = 0
            for (tok in tokens) {
                if (tok.start > cursor) append(text.substring(cursor, tok.start))
                withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                    append(text.substring(tok.start, tok.end))
                }
                cursor = tok.end
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    }

    fun formatRelativeTime(iso: String): String {
        if (iso.isBlank()) return ""
        return runCatching {
            val instant = Instant.parse(iso)
            val secs = Duration.between(instant, Instant.now()).seconds
            when {
                secs < 45 -> "Just now"
                secs < 3600 -> "${secs / 60}m ago"
                secs < 86400 -> "${secs / 3600}h ago"
                secs < 86400 * 7 -> "${secs / 86400}d ago"
                else -> DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
                    .withZone(ZoneId.systemDefault())
                    .format(instant)
            }
        }.getOrElse { iso.take(10) }
    }

    fun mergeReplyIntoComments(
        comments: List<DesktopAnimeComment>,
        commentId: String,
        thread: DesktopReplyThread,
        reply: DesktopThreadReply
    ): List<DesktopAnimeComment> {
        return comments.map { comment ->
            if (comment.id != commentId) return@map comment
            val existingThreads = comment.threads.toMutableList()
            val idx = existingThreads.indexOfFirst {
                it.id == thread.id || it.targetUserId == thread.targetUserId
            }
            if (idx >= 0) {
                val existing = existingThreads[idx]
                val nextReplies = if (existing.replies.any { it.id == reply.id }) {
                    existing.replies.map { if (it.id == reply.id) reply else it }
                } else {
                    existing.replies + reply
                }
                existingThreads[idx] = existing.copy(
                    id = thread.id.ifBlank { existing.id },
                    targetUsername = thread.targetUsername.ifBlank { existing.targetUsername },
                    targetAvatarKey = thread.targetAvatarKey.ifBlank { existing.targetAvatarKey },
                    replyCount = nextReplies.size,
                    replies = nextReplies
                )
            } else {
                existingThreads += thread.copy(replyCount = 1, replies = listOf(reply))
            }
            comment.copy(threads = existingThreads)
        }
    }
}

internal object DesktopDiscussionService {
    private val jsonMedia = "application/json".toMediaType()

    private suspend fun callRpc(
        session: DesktopPersistedState,
        rpcName: String,
        payload: JsonObject,
        requireAuth: Boolean = true
    ): Result<Pair<JsonElement?, DesktopPersistedState>> = withContext(Dispatchers.IO) {
        runCatching {
            val active = if (!session.isGuest && session.accessToken.isNotBlank()) {
                refreshTokenIfNeeded(session)
            } else {
                if (requireAuth) throw IllegalStateException("Please sign in first.")
                session
            }
            val reqBuilder = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/rpc/$rpcName")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(gson.toJson(payload).toRequestBody(jsonMedia))
            if (!active.isGuest && active.accessToken.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${active.accessToken}")
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException(parsePostgrestError(raw, "Request failed (${resp.code})"))
                }
                val parsed = if (raw.isBlank()) null else JsonParser.parseString(raw)
                parsed to active
            }
        }
    }

    private fun JsonObject.str(key: String, fallback: String = ""): String {
        val el = this.get(key) ?: return fallback
        if (el.isJsonNull) return fallback
        return runCatching { el.asString }.getOrElse { fallback }
    }

    private fun JsonObject.intVal(key: String, fallback: Int = 0): Int {
        val el = this.get(key) ?: return fallback
        if (el.isJsonNull) return fallback
        return runCatching { el.asInt }.getOrElse { fallback }
    }

    private fun JsonObject.boolVal(key: String, fallback: Boolean = false): Boolean {
        val el = this.get(key) ?: return fallback
        if (el.isJsonNull) return fallback
        return runCatching { el.asBoolean }.getOrElse { fallback }
    }

    private fun parseReply(obj: JsonObject): DesktopThreadReply = DesktopThreadReply(
        id = obj.str("id"),
        threadId = obj.str("thread_id"),
        commentId = obj.str("comment_id"),
        animeId = obj.intVal("anime_id"),
        userId = obj.str("user_id"),
        username = obj.str("username", "Member"),
        avatarKey = obj.str("avatar_key", "avatar_1"),
        body = obj.str("content").ifBlank { obj.str("body") },
        createdAt = obj.str("created_at"),
        updatedAt = obj.str("updated_at")
    )

    private fun parseThread(obj: JsonObject): DesktopReplyThread {
        val repliesArr = obj.get("replies")?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
        val replies = repliesArr.mapNotNull { it.takeIf { el -> el.isJsonObject }?.asJsonObject?.let(::parseReply) }
        return DesktopReplyThread(
            id = obj.str("id"),
            commentId = obj.str("comment_id"),
            animeId = obj.intVal("anime_id"),
            targetUserId = obj.str("target_user_id"),
            targetUsername = obj.str("target_username", "Member"),
            targetAvatarKey = obj.str("target_avatar_key", "avatar_1"),
            replyCount = obj.intVal("reply_count", replies.size),
            replies = replies
        )
    }

    private fun parseComment(obj: JsonObject): DesktopAnimeComment {
        val threadsArr = obj.get("threads")?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
        val threads = threadsArr.mapNotNull { it.takeIf { el -> el.isJsonObject }?.asJsonObject?.let(::parseThread) }
        return DesktopAnimeComment(
            id = obj.str("id"),
            animeId = obj.intVal("anime_id"),
            userId = obj.str("user_id"),
            username = obj.str("username", "Member"),
            avatarKey = obj.str("avatar_key", "avatar_1"),
            body = obj.str("content").ifBlank { obj.str("body") },
            createdAt = obj.str("created_at"),
            updatedAt = obj.str("updated_at"),
            threads = threads
        )
    }

    suspend fun fetchDiscussionPage(
        session: DesktopPersistedState,
        animeId: Int,
        limit: Int = 15,
        offset: Int = 0
    ): Result<Pair<DesktopDiscussionPage, DesktopPersistedState>> {
        val payload = JsonObject().apply {
            addProperty("p_anime_id", animeId)
            addProperty("p_limit", limit)
            addProperty("p_offset", offset)
        }
        return callRpc(session, "get_anime_discussion_page", payload, requireAuth = false).map { (el, active) ->
            val obj = el?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
            val arr = obj.get("comments")?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
            val comments = arr.mapNotNull { it.takeIf { c -> c.isJsonObject }?.asJsonObject?.let(::parseComment) }
            DesktopDiscussionPage(
                comments = comments,
                totalComments = obj.intVal("total_comments", comments.size),
                hasMore = obj.boolVal("has_more", false)
            ) to active
        }
    }

    suspend fun searchMentionableUsers(
        session: DesktopPersistedState,
        query: String,
        limit: Int = 6
    ): Result<List<DesktopMentionUser>> {
        if (session.isGuest || session.accessToken.isBlank()) return Result.success(emptyList())
        val payload = JsonObject().apply {
            addProperty("p_query", query)
            addProperty("p_limit", limit)
        }
        return callRpc(session, "search_mentionable_users", payload, requireAuth = true).map { (el, _) ->
            val arr = el?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
            arr.mapNotNull { item ->
                item.takeIf { it.isJsonObject }?.asJsonObject?.let { u ->
                    DesktopMentionUser(
                        userId = u.str("id").ifBlank { u.str("user_id") },
                        username = u.str("username"),
                        avatarKey = u.str("avatar_key", "avatar_1")
                    )
                }
            }
        }
    }

    suspend fun createComment(
        session: DesktopPersistedState,
        animeId: Int,
        body: String,
        animeTitle: String,
        animeCover: String
    ): Result<Pair<DesktopAnimeComment, DesktopPersistedState>> {
        val payload = JsonObject().apply {
            addProperty("p_anime_id", animeId)
            addProperty("p_content", body)
            addProperty("p_anime_title", animeTitle)
            addProperty("p_anime_cover", animeCover)
        }
        return callRpc(session, "create_anime_comment", payload, requireAuth = true).map { (el, active) ->
            val obj = el?.takeIf { it.isJsonObject }?.asJsonObject
                ?: throw IllegalStateException("Unexpected response when posting comment.")
            val commentObj = obj.get("comment")?.takeIf { it.isJsonObject }?.asJsonObject ?: obj
            parseComment(commentObj) to active
        }
    }

    suspend fun updateComment(
        session: DesktopPersistedState,
        commentId: String,
        body: String
    ): Result<Pair<DesktopAnimeComment, DesktopPersistedState>> {
        val payload = JsonObject().apply {
            addProperty("p_comment_id", commentId)
            addProperty("p_content", body)
        }
        return callRpc(session, "update_anime_comment", payload, requireAuth = true).map { (el, active) ->
            val obj = el?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
            parseComment(obj) to active
        }
    }

    suspend fun deleteComment(
        session: DesktopPersistedState,
        commentId: String
    ): Result<DesktopPersistedState> {
        val payload = JsonObject().apply { addProperty("p_comment_id", commentId) }
        return callRpc(session, "delete_anime_comment", payload, requireAuth = true).map { it.second }
    }

    suspend fun createThreadReply(
        session: DesktopPersistedState,
        commentId: String,
        targetUserId: String,
        body: String
    ): Result<Triple<DesktopReplyThread, DesktopThreadReply, DesktopPersistedState>> {
        val payload = JsonObject().apply {
            addProperty("p_comment_id", commentId)
            addProperty("p_target_user_id", targetUserId)
            addProperty("p_content", body)
        }
        return callRpc(session, "create_thread_reply", payload, requireAuth = true).map { (el, active) ->
            val obj = el?.takeIf { it.isJsonObject }?.asJsonObject
                ?: throw IllegalStateException("Unexpected response when posting reply.")
            val threadObj = obj.get("thread")?.asJsonObject ?: JsonObject()
            val replyObj = obj.get("reply")?.asJsonObject ?: JsonObject()
            Triple(parseThread(threadObj), parseReply(replyObj), active)
        }
    }

    suspend fun updateThreadReply(
        session: DesktopPersistedState,
        replyId: String,
        body: String
    ): Result<Pair<DesktopThreadReply, DesktopPersistedState>> {
        val payload = JsonObject().apply {
            addProperty("p_reply_id", replyId)
            addProperty("p_content", body)
        }
        return callRpc(session, "update_thread_reply", payload, requireAuth = true).map { (el, active) ->
            val obj = el?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
            parseReply(obj) to active
        }
    }

    suspend fun deleteThreadReply(
        session: DesktopPersistedState,
        replyId: String
    ): Result<DesktopPersistedState> {
        val payload = JsonObject().apply { addProperty("p_reply_id", replyId) }
        return callRpc(session, "delete_thread_reply", payload, requireAuth = true).map { it.second }
    }

    suspend fun getUnreadNotificationCount(session: DesktopPersistedState): Result<Int> {
        if (session.isGuest || session.accessToken.isBlank()) return Result.success(0)
        return callRpc(session, "get_unread_notification_count", JsonObject(), requireAuth = true).map { (el, _) ->
            runCatching { el?.asInt ?: 0 }.getOrElse { 0 }.coerceAtLeast(0)
        }
    }

    suspend fun fetchNotifications(
        session: DesktopPersistedState,
        limit: Int = 25,
        offset: Int = 0
    ): Result<Pair<DesktopNotificationsPage, DesktopPersistedState>> {
        val payload = JsonObject().apply {
            addProperty("p_limit", limit)
            addProperty("p_offset", offset)
        }
        return callRpc(session, "get_user_notifications", payload, requireAuth = true).map { (el, active) ->
            val obj = el?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
            val arr = obj.get("notifications")?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
            val list = arr.mapNotNull { item ->
                item.takeIf { it.isJsonObject }?.asJsonObject?.let { n ->
                    DesktopUserNotification(
                        id = n.str("id"),
                        type = n.str("notification_type").ifBlank { n.str("type", "mention_comment") },
                        animeId = n.intVal("anime_id"),
                        animeTitle = n.str("anime_title"),
                        animeCover = n.str("anime_cover_image").ifBlank { n.str("anime_cover") },
                        commentId = n.str("comment_id").takeIf { it.isNotBlank() },
                        threadId = n.str("thread_id").takeIf { it.isNotBlank() },
                        replyId = n.str("reply_id").takeIf { it.isNotBlank() },
                        senderUserId = n.str("sender_user_id"),
                        senderUsername = n.str("sender_username", "Member"),
                        senderAvatarKey = n.str("sender_avatar_key", "avatar_1"),
                        contentPreview = n.str("preview_text").ifBlank { n.str("content_preview") },
                        isRead = n.boolVal("is_read", false),
                        createdAt = n.str("created_at")
                    )
                }
            }
            DesktopNotificationsPage(
                notifications = list,
                unreadCount = obj.intVal("unread_count", list.count { !it.isRead }),
                totalCount = obj.intVal("total_count", list.size),
                hasMore = obj.boolVal("has_more", false)
            ) to active
        }
    }

    suspend fun markNotificationRead(session: DesktopPersistedState, notificationId: String): Result<Unit> {
        val payload = JsonObject().apply { addProperty("p_notification_id", notificationId) }
        return callRpc(session, "mark_notification_read", payload, requireAuth = true).map { Unit }
    }

    suspend fun markAllNotificationsRead(session: DesktopPersistedState): Result<Unit> {
        return callRpc(session, "mark_all_notifications_read", JsonObject(), requireAuth = true).map { Unit }
    }
}

private data class DesktopActiveReplyTarget(
    val commentId: String,
    val targetUserId: String,
    val targetUsername: String
)

@Composable
internal fun DesktopAnimeDiscussionSection(
    anime: DesktopAnimeItem,
    session: DesktopPersistedState,
    isAuthenticated: Boolean,
    jumpTarget: DesktopNotificationJumpTarget?,
    onJumpTargetConsumed: () -> Unit,
    onSessionUpdated: (DesktopPersistedState) -> Unit,
    onShowToast: (String, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var comments by remember(anime.id) { mutableStateOf<List<DesktopAnimeComment>>(emptyList()) }
    var totalComments by remember(anime.id) { mutableStateOf(0) }
    var hasMore by remember(anime.id) { mutableStateOf(false) }
    var loading by remember(anime.id) { mutableStateOf(true) }
    var loadingMore by remember(anime.id) { mutableStateOf(false) }
    var submitting by remember(anime.id) { mutableStateOf(false) }
    var successMsg by remember(anime.id) { mutableStateOf<String?>(null) }
    var errorMsg by remember(anime.id) { mutableStateOf<String?>(null) }

    var draft by remember(anime.id) { mutableStateOf(TextFieldValue("")) }
    var activeReplyTarget by remember(anime.id) { mutableStateOf<DesktopActiveReplyTarget?>(null) }
    var replyDraft by remember(anime.id) { mutableStateOf(TextFieldValue("")) }

    var editingCommentId by remember(anime.id) { mutableStateOf<String?>(null) }
    var editingCommentDraft by remember(anime.id) { mutableStateOf(TextFieldValue("")) }
    var editingReplyId by remember(anime.id) { mutableStateOf<String?>(null) }
    var editingReplyCommentId by remember(anime.id) { mutableStateOf<String?>(null) }
    var editingReplyThreadId by remember(anime.id) { mutableStateOf<String?>(null) }
    var editingReplyDraft by remember(anime.id) { mutableStateOf(TextFieldValue("")) }

    var expandedThreads by remember(anime.id) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var highlightedTarget by remember(anime.id) { mutableStateOf<DesktopNotificationJumpTarget?>(null) }

    var mentionSuggestions by remember(anime.id) { mutableStateOf<List<DesktopMentionUser>>(emptyList()) }
    var mentionScope by remember(anime.id) { mutableStateOf<String?>(null) }
    var activeMentionQuery by remember(anime.id) { mutableStateOf<DesktopActiveMentionQuery?>(null) }

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
                    errorMsg = err.message ?: "Unable to load discussion."
                }
            loading = false
            loadingMore = false
        }
    }

    LaunchedEffect(anime.id) {
        loadPage(reset = true)
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

    LaunchedEffect(activeMentionQuery, mentionScope, isAuthenticated) {
        val q = activeMentionQuery
        if (!isAuthenticated || session.isGuest || q == null || mentionScope == null) {
            mentionSuggestions = emptyList()
            return@LaunchedEffect
        }
        delay(160L)
        DesktopDiscussionService.searchMentionableUsers(session, q.query, limit = 6)
            .onSuccess { mentionSuggestions = it }
            .onFailure { mentionSuggestions = emptyList() }
    }

    val draftValidation = remember(draft.text) { DesktopDiscussionRules.validateMentions(draft.text) }
    val replyValidation = remember(replyDraft.text) { DesktopDiscussionRules.validateMentions(replyDraft.text) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "COMMUNITY DISCUSSION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        color = ChampagneGold
                    )
                    Text(
                        "Comments ($totalComments)",
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        color = IvoryText
                    )
                }
                OutlinedButton(
                    onClick = { loadPage(reset = true) },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = WarmSilver, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh", color = IvoryText, fontSize = 12.sp)
                }
            }

            if (isAuthenticated && !session.isGuest) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { next ->
                        draft = next
                        val q = DesktopDiscussionRules.detectActiveMentionQuery(next.text, next.selection.end)
                        activeMentionQuery = q
                        mentionScope = if (q != null) "comment" else null
                    },
                    enabled = !submitting,
                    placeholder = {
                        Text(
                            "Share your thoughts on ${anime.title}... Use @username to tag members (max 5)",
                            color = WarmSilver,
                            fontSize = 13.sp
                        )
                    },
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mentionScope == "comment" && mentionSuggestions.isNotEmpty() && activeMentionQuery != null) {
                    DesktopMentionSuggestionsCard(
                        suggestions = mentionSuggestions,
                        onPick = { user ->
                            draft = DesktopDiscussionRules.insertAutocomplete(draft, activeMentionQuery!!, user.username)
                            activeMentionQuery = null
                            mentionScope = null
                            mentionSuggestions = emptyList()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DesktopMentionCounterChip(validation = draftValidation)
                    Button(
                        onClick = {
                            if (submitting) return@Button
                            val clean = draft.text.trim()
                            if (clean.isEmpty()) {
                                errorMsg = "Please write a comment before posting."
                                successMsg = null
                                return@Button
                            }
                            if (!draftValidation.isValid) {
                                errorMsg = draftValidation.error
                                successMsg = null
                                return@Button
                            }
                            submitting = true
                            errorMsg = null
                            successMsg = null
                            scope.launch {
                                DesktopDiscussionService.createComment(session, anime.id, clean, anime.title, anime.coverImage)
                                    .onSuccess { (created, active) ->
                                        onSessionUpdated(active)
                                        comments = listOf(created) + comments.filterNot { it.id == created.id }
                                        totalComments = maxOf(comments.size, totalComments + 1)
                                        draft = TextFieldValue("")
                                        activeMentionQuery = null
                                        mentionScope = null
                                        successMsg = "Comment posted successfully"
                                        onShowToast("Comment posted successfully", false)
                                    }
                                    .onFailure { err ->
                                        errorMsg = err.message ?: "Failed to post comment."
                                        onShowToast(errorMsg!!, true)
                                    }
                                submitting = false
                            }
                        },
                        enabled = !submitting && draftValidation.isValid,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                color = ChampagneOnGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Posting...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Post Comment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianPillBg,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Sign in to post comments, @mention members, and reply in threads.",
                        color = WarmSilver,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            successMsg?.let { msg ->
                DesktopFeedbackBanner(message = msg, isError = false)
            }
            errorMsg?.let { msg ->
                DesktopFeedbackBanner(message = msg, isError = true)
            }

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ChampagneGold, strokeWidth = 2.dp)
                    }
                }
                comments.isEmpty() -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianPillBg,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No comments yet. Start the discussion for ${anime.title}!",
                            color = WarmSilver,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        comments.forEach { comment ->
                            val isOwnComment = isAuthenticated && !session.isGuest && comment.userId == session.userId
                            val isEditingThis = editingCommentId == comment.id
                            val isCommentHighlighted = highlightedTarget?.commentId == comment.id &&
                                highlightedTarget?.threadId == null && highlightedTarget?.replyId == null

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = ObsidianPillBg,
                                border = BorderStroke(
                                    width = if (isCommentHighlighted) 1.6.dp else 1.dp,
                                    color = if (isCommentHighlighted) ChampagneGold else ObsidianBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DesktopAvatarImage(
                                            avatarKey = comment.avatarKey,
                                            username = comment.username,
                                            modifier = Modifier.size(34.dp).clip(CircleShape)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(comment.username, color = IvoryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            val editedSuffix = if (comment.updatedAt.isNotBlank() && comment.updatedAt != comment.createdAt) " • edited" else ""
                                            Text(
                                                DesktopDiscussionRules.formatRelativeTime(comment.createdAt) + editedSuffix,
                                                color = AshMuted,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    if (isEditingThis) {
                                        val editCheck = DesktopDiscussionRules.validateMentions(editingCommentDraft.text)
                                        OutlinedTextField(
                                            value = editingCommentDraft,
                                            onValueChange = { editingCommentDraft = it },
                                            minLines = 2,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DesktopMentionCounterChip(validation = editCheck)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(onClick = { editingCommentId = null }) {
                                                    Text("Cancel", color = WarmSilver)
                                                }
                                                Button(
                                                    enabled = !submitting && editCheck.isValid && editingCommentDraft.text.trim().isNotEmpty(),
                                                    onClick = {
                                                        submitting = true
                                                        scope.launch {
                                                            DesktopDiscussionService.updateComment(session, comment.id, editingCommentDraft.text.trim())
                                                                .onSuccess { (updated, active) ->
                                                                    onSessionUpdated(active)
                                                                    comments = comments.map {
                                                                        if (it.id == comment.id) it.copy(body = updated.body.ifBlank { editingCommentDraft.text.trim() }, updatedAt = updated.updatedAt)
                                                                        else it
                                                                    }
                                                                    editingCommentId = null
                                                                    successMsg = "Comment updated successfully"
                                                                }
                                                                .onFailure { errorMsg = it.message ?: "Failed to update comment." }
                                                            submitting = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                                                ) {
                                                    Text("Save", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = DesktopDiscussionRules.buildHighlightedText(comment.body, ChampagneGold),
                                            color = IvoryText,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (isAuthenticated && !session.isGuest) {
                                            TextButton(
                                                onClick = {
                                                    activeReplyTarget = DesktopActiveReplyTarget(
                                                        commentId = comment.id,
                                                        targetUserId = comment.userId,
                                                        targetUsername = comment.username
                                                    )
                                                    replyDraft = TextFieldValue("")
                                                    errorMsg = null
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Reply to @${comment.username}", color = ChampagneGoldSoft, fontSize = 12.sp)
                                            }
                                        }
                                        if (isOwnComment && !isEditingThis) {
                                            TextButton(
                                                onClick = {
                                                    editingCommentId = comment.id
                                                    editingCommentDraft = TextFieldValue(comment.body, TextRange(comment.body.length))
                                                }
                                            ) {
                                                Icon(Icons.Outlined.Edit, contentDescription = null, tint = WarmSilver, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Edit", color = WarmSilver, fontSize = 12.sp)
                                            }
                                            TextButton(
                                                onClick = {
                                                    if (submitting) return@TextButton
                                                    submitting = true
                                                    scope.launch {
                                                        DesktopDiscussionService.deleteComment(session, comment.id)
                                                            .onSuccess { active ->
                                                                onSessionUpdated(active)
                                                                comments = comments.filterNot { it.id == comment.id }
                                                                totalComments = (totalComments - 1).coerceAtLeast(0)
                                                                successMsg = "Comment deleted"
                                                            }
                                                            .onFailure { errorMsg = it.message ?: "Failed to delete comment." }
                                                        submitting = false
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = CrimsonWaste, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Delete", color = CrimsonWaste, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    // Target-user grouped Reply Threads
                                    if (comment.threads.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            comment.threads.forEach { thread ->
                                                val isExpanded = expandedThreads[thread.id] == true
                                                val isThreadHighlighted = highlightedTarget?.threadId == thread.id &&
                                                    highlightedTarget?.replyId == null

                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = ObsidianCard,
                                                    border = BorderStroke(
                                                        width = if (isThreadHighlighted) 1.5.dp else 1.dp,
                                                        color = if (isThreadHighlighted) ChampagneGold else ObsidianBorder
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    expandedThreads = expandedThreads + (thread.id to !isExpanded)
                                                                }
                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = buildAnnotatedString {
                                                                    append("Thread for ")
                                                                    withStyle(SpanStyle(color = ChampagneGold, fontWeight = FontWeight.Bold)) {
                                                                        append("@${thread.targetUsername}")
                                                                    }
                                                                    withStyle(SpanStyle(color = WarmSilver)) {
                                                                        val rc = thread.replies.size
                                                                        append(" • $rc ${if (rc == 1) "reply" else "replies"}")
                                                                    }
                                                                },
                                                                color = IvoryText,
                                                                fontSize = 12.5.sp
                                                            )
                                                            Icon(
                                                                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                                                contentDescription = null,
                                                                tint = ChampagneGold,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }

                                                        if (isExpanded) {
                                                            Column(
                                                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                thread.replies.forEach { reply ->
                                                                    val isOwnReply = isAuthenticated && !session.isGuest && reply.userId == session.userId
                                                                    val isEditingReply = editingReplyId == reply.id
                                                                    val isReplyHighlighted = highlightedTarget?.replyId == reply.id

                                                                    Surface(
                                                                        shape = RoundedCornerShape(12.dp),
                                                                        color = ObsidianPillBg,
                                                                        border = BorderStroke(
                                                                            width = if (isReplyHighlighted) 1.5.dp else 1.dp,
                                                                            color = if (isReplyHighlighted) ChampagneGold else ObsidianBorder.copy(alpha = 0.7f)
                                                                        ),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    ) {
                                                                        Column(
                                                                            modifier = Modifier.padding(10.dp),
                                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                                        ) {
                                                                            Row(
                                                                                verticalAlignment = Alignment.CenterVertically,
                                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                            ) {
                                                                                DesktopAvatarImage(
                                                                                    avatarKey = reply.avatarKey,
                                                                                    username = reply.username,
                                                                                    modifier = Modifier.size(26.dp).clip(CircleShape)
                                                                                )
                                                                                Text(reply.username, color = IvoryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                                                val editedTag = if (reply.updatedAt.isNotBlank() && reply.updatedAt != reply.createdAt) " • edited" else ""
                                                                                Text(
                                                                                    DesktopDiscussionRules.formatRelativeTime(reply.createdAt) + editedTag,
                                                                                    color = AshMuted,
                                                                                    fontSize = 11.sp
                                                                                )
                                                                            }

                                                                            if (isEditingReply) {
                                                                                val rCheck = DesktopDiscussionRules.validateMentions(editingReplyDraft.text)
                                                                                OutlinedTextField(
                                                                                    value = editingReplyDraft,
                                                                                    onValueChange = { editingReplyDraft = it },
                                                                                    minLines = 2,
                                                                                    modifier = Modifier.fillMaxWidth()
                                                                                )
                                                                                Row(
                                                                                    modifier = Modifier.fillMaxWidth(),
                                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                                    verticalAlignment = Alignment.CenterVertically
                                                                                ) {
                                                                                    DesktopMentionCounterChip(validation = rCheck)
                                                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                                        TextButton(onClick = { editingReplyId = null }) {
                                                                                            Text("Cancel", color = WarmSilver)
                                                                                        }
                                                                                        Button(
                                                                                            enabled = !submitting && rCheck.isValid && editingReplyDraft.text.trim().isNotEmpty(),
                                                                                            onClick = {
                                                                                                submitting = true
                                                                                                scope.launch {
                                                                                                    DesktopDiscussionService.updateThreadReply(session, reply.id, editingReplyDraft.text.trim())
                                                                                                        .onSuccess { (updated, active) ->
                                                                                                            onSessionUpdated(active)
                                                                                                            comments = comments.map { c ->
                                                                                                                if (c.id != comment.id) c else c.copy(
                                                                                                                    threads = c.threads.map { th ->
                                                                                                                        if (th.id != thread.id) th else th.copy(
                                                                                                                            replies = th.replies.map { rr ->
                                                                                                                                if (rr.id == reply.id) rr.copy(body = updated.body.ifBlank { editingReplyDraft.text.trim() }, updatedAt = updated.updatedAt)
                                                                                                                                else rr
                                                                                                                            }
                                                                                                                        )
                                                                                                                    }
                                                                                                                )
                                                                                                            }
                                                                                                            editingReplyId = null
                                                                                                            successMsg = "Reply updated"
                                                                                                        }
                                                                                                        .onFailure { errorMsg = it.message ?: "Failed to update reply." }
                                                                                                    submitting = false
                                                                                                }
                                                                                            },
                                                                                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                                                                                        ) {
                                                                                            Text("Save", fontWeight = FontWeight.Bold)
                                                                                        }
                                                                                    }
                                                                                }
                                                                            } else {
                                                                                Text(
                                                                                    text = DesktopDiscussionRules.buildHighlightedText(reply.body, ChampagneGold),
                                                                                    color = IvoryText,
                                                                                    fontSize = 13.5.sp,
                                                                                    lineHeight = 19.sp
                                                                                )
                                                                            }

                                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                                if (isAuthenticated && !session.isGuest) {
                                                                                    TextButton(
                                                                                        onClick = {
                                                                                            activeReplyTarget = DesktopActiveReplyTarget(
                                                                                                commentId = comment.id,
                                                                                                targetUserId = reply.userId,
                                                                                                targetUsername = reply.username
                                                                                            )
                                                                                            replyDraft = TextFieldValue("")
                                                                                        }
                                                                                    ) {
                                                                                        Text("Reply to @${reply.username}", color = ChampagneGoldSoft, fontSize = 11.5.sp)
                                                                                    }
                                                                                }
                                                                                if (isOwnReply && !isEditingReply) {
                                                                                    TextButton(
                                                                                        onClick = {
                                                                                            editingReplyId = reply.id
                                                                                            editingReplyCommentId = comment.id
                                                                                            editingReplyThreadId = thread.id
                                                                                            editingReplyDraft = TextFieldValue(reply.body, TextRange(reply.body.length))
                                                                                        }
                                                                                    ) {
                                                                                        Text("Edit", color = WarmSilver, fontSize = 11.5.sp)
                                                                                    }
                                                                                    TextButton(
                                                                                        onClick = {
                                                                                            if (submitting) return@TextButton
                                                                                            submitting = true
                                                                                            scope.launch {
                                                                                                DesktopDiscussionService.deleteThreadReply(session, reply.id)
                                                                                                    .onSuccess { active ->
                                                                                                        onSessionUpdated(active)
                                                                                                        comments = comments.map { c ->
                                                                                                            if (c.id != comment.id) c else c.copy(
                                                                                                                threads = c.threads.mapNotNull { th ->
                                                                                                                    if (th.id != thread.id) th else {
                                                                                                                        val kept = th.replies.filterNot { it.id == reply.id }
                                                                                                                        if (kept.isEmpty()) null else th.copy(replies = kept, replyCount = kept.size)
                                                                                                                    }
                                                                                                                }
                                                                                                            )
                                                                                                        }
                                                                                                        successMsg = "Reply deleted"
                                                                                                    }
                                                                                                    .onFailure { errorMsg = it.message ?: "Failed to delete reply." }
                                                                                                submitting = false
                                                                                            }
                                                                                        }
                                                                                    ) {
                                                                                        Text("Delete", color = CrimsonWaste, fontSize = 11.5.sp)
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }

                                                                if (isAuthenticated && !session.isGuest) {
                                                                    TextButton(
                                                                        onClick = {
                                                                            activeReplyTarget = DesktopActiveReplyTarget(
                                                                                commentId = comment.id,
                                                                                targetUserId = thread.targetUserId,
                                                                                targetUsername = thread.targetUsername
                                                                            )
                                                                            replyDraft = TextFieldValue("")
                                                                        }
                                                                    ) {
                                                                        Text("+ Join @${thread.targetUsername}'s thread", color = ChampagneGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Inline reply composer for this comment
                                    val replyTarget = activeReplyTarget
                                    if (replyTarget != null && replyTarget.commentId == comment.id) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = ChampagneGold.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
                                            modifier = Modifier.fillMaxWidth()
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
                                                        "Replying in @${replyTarget.targetUsername}'s thread",
                                                        color = ChampagneGold,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    TextButton(onClick = { activeReplyTarget = null }) {
                                                        Text("Cancel", color = WarmSilver, fontSize = 12.sp)
                                                    }
                                                }
                                                OutlinedTextField(
                                                    value = replyDraft,
                                                    onValueChange = { next ->
                                                        replyDraft = next
                                                        val q = DesktopDiscussionRules.detectActiveMentionQuery(next.text, next.selection.end)
                                                        activeMentionQuery = q
                                                        mentionScope = if (q != null) "reply" else null
                                                    },
                                                    enabled = !submitting,
                                                    placeholder = {
                                                        Text(
                                                            "Write a reply... (@${replyTarget.targetUsername} is notified automatically)",
                                                            color = WarmSilver,
                                                            fontSize = 12.5.sp
                                                        )
                                                    },
                                                    minLines = 2,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                if (mentionScope == "reply" && mentionSuggestions.isNotEmpty() && activeMentionQuery != null) {
                                                    DesktopMentionSuggestionsCard(
                                                        suggestions = mentionSuggestions,
                                                        onPick = { user ->
                                                            replyDraft = DesktopDiscussionRules.insertAutocomplete(replyDraft, activeMentionQuery!!, user.username)
                                                            activeMentionQuery = null
                                                            mentionScope = null
                                                            mentionSuggestions = emptyList()
                                                        }
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    DesktopMentionCounterChip(validation = replyValidation)
                                                    Button(
                                                        enabled = !submitting && replyValidation.isValid,
                                                        onClick = {
                                                            if (submitting) return@Button
                                                            val clean = replyDraft.text.trim()
                                                            if (clean.isEmpty()) {
                                                                errorMsg = "Please write a reply before posting."
                                                                return@Button
                                                            }
                                                            submitting = true
                                                            errorMsg = null
                                                            successMsg = null
                                                            scope.launch {
                                                                DesktopDiscussionService.createThreadReply(
                                                                    session = session,
                                                                    commentId = replyTarget.commentId,
                                                                    targetUserId = replyTarget.targetUserId,
                                                                    body = clean
                                                                ).onSuccess { (threadDto, replyDto, active) ->
                                                                    onSessionUpdated(active)
                                                                    comments = DesktopDiscussionRules.mergeReplyIntoComments(
                                                                        comments,
                                                                        replyTarget.commentId,
                                                                        threadDto,
                                                                        replyDto
                                                                    )
                                                                    expandedThreads = expandedThreads + (threadDto.id to true)
                                                                    replyDraft = TextFieldValue("")
                                                                    activeReplyTarget = null
                                                                    successMsg = "Reply posted in @${replyTarget.targetUsername}'s thread"
                                                                    onShowToast("Reply posted successfully", false)
                                                                }.onFailure { err ->
                                                                    errorMsg = err.message ?: "Failed to post reply."
                                                                    onShowToast(errorMsg!!, true)
                                                                }
                                                                submitting = false
                                                            }
                                                        },
                                                        shape = CircleShape,
                                                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                                                    ) {
                                                        Text(if (submitting) "Posting..." else "Post Reply", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (hasMore) {
                        OutlinedButton(
                            onClick = { if (!loadingMore) loadPage(reset = false) },
                            enabled = !loadingMore,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (loadingMore) "Loading more..." else "Load More Comments", color = ChampagneGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesktopFeedbackBanner(message: String, isError: Boolean) {
    val accent = if (isError) CrimsonWaste else EmeraldWorth
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(message, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DesktopMentionCounterChip(validation: DesktopMentionValidation) {
    val accent = if (validation.isValid) WarmSilver else CrimsonWaste
    Surface(
        shape = CircleShape,
        color = if (validation.isValid) ObsidianPillBg else CrimsonWaste.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, if (validation.isValid) ObsidianBorder else CrimsonWaste.copy(alpha = 0.5f))
    ) {
        Text(
            "${validation.totalMentions}/${DesktopDiscussionRules.MAX_MENTIONS_PER_CONTENT} @mentions",
            color = accent,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun DesktopMentionSuggestionsCard(
    suggestions: List<DesktopMentionUser>,
    onPick: (DesktopMentionUser) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            suggestions.forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPick(user) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DesktopAvatarImage(
                        avatarKey = user.avatarKey,
                        username = user.username,
                        modifier = Modifier.size(26.dp).clip(CircleShape)
                    )
                    Text("@${user.username}", color = IvoryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
internal fun DesktopNotificationsScreen(
    session: DesktopPersistedState,
    isAuthenticated: Boolean,
    notifications: List<DesktopUserNotification>,
    unreadCount: Int,
    hasMore: Boolean,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenNotification: (DesktopUserNotification) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    "COMMUNITY ALERTS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = ChampagneGold
                )
                Text(
                    "Notifications",
                    fontFamily = FontFamily.Serif,
                    fontSize = 34.sp,
                    color = IvoryText
                )
                Text(
                    if (unreadCount > 0) "$unreadCount unread notification${if (unreadCount == 1) "" else "s"}" else "All caught up",
                    color = WarmSilver,
                    fontSize = 13.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRefresh,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = WarmSilver, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh", color = IvoryText, fontSize = 12.5.sp)
                }
                if (unreadCount > 0) {
                    Button(
                        onClick = onMarkAllRead,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ChampagneOnGold)
                    ) {
                        Text("Mark all read", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }

        error?.let { DesktopFeedbackBanner(message = it, isError = true) }

        when {
            !isAuthenticated || session.isGuest -> {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Sign in to receive @mention and thread reply notifications.",
                        color = WarmSilver,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(28.dp)
                    )
                }
            }
            loading -> {
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
                        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(34.dp))
                        Text("No notifications yet", color = IvoryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "When someone @mentions you or replies to your thread, it will show up here.",
                            color = WarmSilver,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(notifications, key = { it.id }) { n ->
                        val actionCopy = when (n.type) {
                            "thread_reply" -> "replied to your thread"
                            "mention_reply" -> "mentioned you in a reply"
                            else -> "mentioned you in a comment"
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (!n.isRead) Color(0xFF1D1B17) else ObsidianCard,
                            border = BorderStroke(
                                1.dp,
                                if (!n.isRead) ChampagneGold.copy(alpha = 0.55f) else ObsidianBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onOpenNotification(n) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DesktopAvatarImage(
                                    avatarKey = n.senderAvatarKey,
                                    username = n.senderUsername,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(SpanStyle(color = ChampagneGoldSoft, fontWeight = FontWeight.Bold)) {
                                                append("@${n.senderUsername}")
                                            }
                                            append(" $actionCopy")
                                            if (n.animeTitle.isNotBlank()) {
                                                append(" on ")
                                                withStyle(SpanStyle(color = ChampagneGold, fontWeight = FontWeight.SemiBold)) {
                                                    append(n.animeTitle)
                                                }
                                            }
                                        },
                                        color = IvoryText,
                                        fontSize = 14.sp
                                    )
                                    if (n.contentPreview.isNotBlank()) {
                                        Text(
                                            "“${n.contentPreview}”",
                                            color = WarmSilver,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        DesktopDiscussionRules.formatRelativeTime(n.createdAt),
                                        color = AshMuted,
                                        fontSize = 11.5.sp
                                    )
                                }
                                if (!n.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(ChampagneGold)
                                    )
                                }
                            }
                        }
                    }

                    if (hasMore) {
                        item {
                            OutlinedButton(
                                onClick = onLoadMore,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Load More Notifications", color = ChampagneGold)
                            }
                        }
                    }
                }
            }
        }
    }
}
