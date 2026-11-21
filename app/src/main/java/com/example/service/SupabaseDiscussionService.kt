package com.example.service

import com.example.config.AppConfig
import com.example.model.AnimeCommentItem
import com.example.model.AnimeDiscussionPage
import com.example.model.AuthSession
import com.example.model.CommentReplyItem
import com.example.model.CommentReplyThread
import com.example.model.ContentMention
import com.example.model.MentionableUser
import com.example.model.NotificationsPage
import com.example.model.ThreadReplyCreationResult
import com.example.model.UnseenDiscussionAnime
import com.example.model.UnseenDiscussionSummary
import com.example.model.UserNotificationItem
import com.example.util.ErrorSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseDiscussionService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun rpcRequest(rpcName: String, token: String, payload: JSONObject): Request {
        return Request.Builder()
            .url("${AppConfig.supabaseUrl}/rest/v1/rpc/$rpcName")
            .addHeader("apikey", AppConfig.supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    suspend fun fetchAnimeDiscussionPage(
        session: AuthSession,
        animeId: Int,
        limit: Int = 15,
        offset: Int = 0
    ): Result<AnimeDiscussionPage> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_anime_id", animeId)
                .put("p_limit", limit.coerceIn(1, 50))
                .put("p_offset", offset.coerceAtLeast(0))
            client.newCall(rpcRequest("get_anime_discussion_page", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to load comments right now.")))
                }
                val obj = JSONObject(body)
                val commentsArr = obj.optJSONArray("comments") ?: JSONArray()
                val comments = buildList {
                    for (i in 0 until commentsArr.length()) {
                        add(parseComment(commentsArr.getJSONObject(i)))
                    }
                }
                Result.success(
                    AnimeDiscussionPage(
                        animeId = obj.optInt("anime_id", animeId),
                        totalComments = obj.optInt("total_comments", comments.size),
                        hasMore = obj.optBoolean("has_more", false),
                        comments = comments
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to load comments right now.")))
        }
    }

    suspend fun searchMentionableUsers(
        session: AuthSession,
        query: String,
        limit: Int = 8
    ): Result<List<MentionableUser>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.success(emptyList())
        try {
            val payload = JSONObject()
                .put("p_query", query.trim())
                .put("p_limit", limit.coerceIn(1, 20))
            client.newCall(rpcRequest("search_mentionable_users", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful || !body.startsWith("[")) {
                    return@withContext Result.success(emptyList())
                }
                val arr = JSONArray(body)
                val users = buildList {
                    for (i in 0 until arr.length()) {
                        val u = arr.getJSONObject(i)
                        add(
                            MentionableUser(
                                id = u.optString("id"),
                                username = u.optString("username"),
                                avatarKey = u.optString("avatar_key", "avatar_1").ifBlank { "avatar_1" },
                                avatarUrl = u.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" }
                            )
                        )
                    }
                }
                Result.success(users)
            }
        } catch (_: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun createAnimeComment(
        session: AuthSession,
        animeId: Int,
        content: String,
        animeTitle: String,
        animeCover: String,
        taggedUserIds: List<String> = emptyList()
    ): Result<Pair<AnimeCommentItem, Int>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_anime_id", animeId)
                .put("p_content", content.trim())
                .put("p_anime_title", animeTitle.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("p_anime_cover", animeCover.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("p_tagged_user_ids", JSONArray(taggedUserIds))
            client.newCall(rpcRequest("create_anime_comment", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to post comment right now.")))
                }
                val obj = JSONObject(body)
                val comment = parseComment(obj.getJSONObject("comment"))
                val total = obj.optInt("total_comments", 1)
                Result.success(comment to total)
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to post comment right now.")))
        }
    }

    suspend fun updateAnimeComment(
        session: AuthSession,
        commentId: String,
        content: String,
        taggedUserIds: List<String> = emptyList()
    ): Result<Triple<String, String, List<ContentMention>>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_comment_id", commentId)
                .put("p_content", content.trim())
                .put("p_tagged_user_ids", JSONArray(taggedUserIds))
            client.newCall(rpcRequest("update_anime_comment", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to update comment right now.")))
                }
                val obj = JSONObject(body)
                Result.success(
                    Triple(
                        obj.optString("content", content.trim()),
                        obj.optString("updated_at", ""),
                        parseMentions(obj.optJSONArray("mentions"))
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to update comment right now.")))
        }
    }

    suspend fun deleteAnimeComment(
        session: AuthSession,
        commentId: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject().put("p_comment_id", commentId)
            client.newCall(rpcRequest("delete_anime_comment", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to delete comment right now.")))
                }
                val obj = JSONObject(body)
                Result.success(obj.optInt("total_comments", 0))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to delete comment right now.")))
        }
    }

    suspend fun createThreadReply(
        session: AuthSession,
        commentId: String,
        targetUserId: String,
        content: String,
        taggedUserIds: List<String> = emptyList()
    ): Result<ThreadReplyCreationResult> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_comment_id", commentId)
                .put("p_target_user_id", targetUserId)
                .put("p_content", content.trim())
                .put("p_tagged_user_ids", JSONArray(taggedUserIds))
            client.newCall(rpcRequest("create_thread_reply", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to post reply right now.")))
                }
                val obj = JSONObject(body)
                val tObj = obj.getJSONObject("thread")
                val rObj = obj.getJSONObject("reply")
                val reply = parseReply(rObj)
                val thread = CommentReplyThread(
                    id = tObj.optString("id"),
                    commentId = tObj.optString("comment_id", commentId),
                    targetUserId = tObj.optString("target_user_id", targetUserId),
                    targetUsername = tObj.optString("target_username", "Member"),
                    targetAvatarKey = tObj.optString("target_avatar_key", "avatar_1").ifBlank { "avatar_1" },
                    targetAvatarUrl = tObj.optString("target_avatar_url").takeIf { it.isNotBlank() && it != "null" },
                    createdAt = tObj.optString("created_at", ""),
                    replies = listOf(reply)
                )
                Result.success(ThreadReplyCreationResult(thread = thread, reply = reply))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to post reply right now.")))
        }
    }

    suspend fun updateThreadReply(
        session: AuthSession,
        replyId: String,
        content: String,
        taggedUserIds: List<String> = emptyList()
    ): Result<Triple<String, String, List<ContentMention>>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_reply_id", replyId)
                .put("p_content", content.trim())
                .put("p_tagged_user_ids", JSONArray(taggedUserIds))
            client.newCall(rpcRequest("update_thread_reply", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to update reply right now.")))
                }
                val obj = JSONObject(body)
                Result.success(
                    Triple(
                        obj.optString("content", content.trim()),
                        obj.optString("updated_at", ""),
                        parseMentions(obj.optJSONArray("mentions"))
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to update reply right now.")))
        }
    }

    suspend fun deleteThreadReply(
        session: AuthSession,
        replyId: String
    ): Result<Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud discussion service is not configured."))
        }
        try {
            val payload = JSONObject().put("p_reply_id", replyId)
            client.newCall(rpcRequest("delete_thread_reply", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to delete reply right now.")))
                }
                val obj = JSONObject(body)
                Result.success(obj.optString("thread_id", "") to obj.optBoolean("thread_removed", false))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to delete reply right now.")))
        }
    }

    suspend fun fetchUnreadNotificationCount(session: AuthSession): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.success(0)
        try {
            client.newCall(rpcRequest("get_unread_notification_count", session.accessToken, JSONObject())).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) return@withContext Result.success(0)
                Result.success(body.toIntOrNull() ?: 0)
            }
        } catch (_: Exception) {
            Result.success(0)
        }
    }

    suspend fun fetchNotifications(
        session: AuthSession,
        limit: Int = 30,
        offset: Int = 0
    ): Result<NotificationsPage> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) {
            return@withContext Result.failure(Exception("Cloud notifications are not configured."))
        }
        try {
            val payload = JSONObject()
                .put("p_limit", limit.coerceIn(1, 60))
                .put("p_offset", offset.coerceAtLeast(0))
            client.newCall(rpcRequest("get_user_notifications", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to load notifications.")))
                }
                val obj = JSONObject(body)
                val arr = obj.optJSONArray("notifications") ?: JSONArray()
                val items = buildList {
                    for (i in 0 until arr.length()) {
                        val n = arr.getJSONObject(i)
                        add(
                            UserNotificationItem(
                                id = n.optString("id"),
                                recipientUserId = n.optString("recipient_user_id"),
                                senderUserId = n.optString("sender_user_id"),
                                senderUsername = n.optString("sender_username", "Member"),
                                senderAvatarKey = n.optString("sender_avatar_key", "avatar_1").ifBlank { "avatar_1" },
                                senderAvatarUrl = n.optString("sender_avatar_url").takeIf { it.isNotBlank() && it != "null" },
                                notificationType = n.optString("notification_type", "mention_comment"),
                                animeId = n.optInt("anime_id", 0),
                                animeTitle = n.optString("anime_title", "Anime"),
                                animeCoverImage = n.optString("anime_cover_image", ""),
                                commentId = n.optString("comment_id", ""),
                                threadId = n.optString("thread_id").takeIf { it.isNotBlank() && it != "null" },
                                replyId = n.optString("reply_id").takeIf { it.isNotBlank() && it != "null" },
                                previewText = n.optString("preview_text", ""),
                                isRead = n.optBoolean("is_read", false),
                                createdAt = n.optString("created_at", "")
                            )
                        )
                    }
                }
                Result.success(
                    NotificationsPage(
                        unreadCount = obj.optInt("unread_count", items.count { !it.isRead }),
                        totalCount = obj.optInt("total_count", items.size),
                        hasMore = obj.optBoolean("has_more", false),
                        notifications = items
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to load notifications.")))
        }
    }

    suspend fun markNotificationRead(session: AuthSession, notificationId: String): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.success(0)
        try {
            val payload = JSONObject().put("p_notification_id", notificationId)
            client.newCall(rpcRequest("mark_notification_read", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to mark notification as read.")))
                }
                val obj = JSONObject(body)
                Result.success(obj.optInt("unread_count", 0))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to mark notification as read.")))
        }
    }

    suspend fun markAllNotificationsRead(session: AuthSession): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.success(0)
        try {
            client.newCall(rpcRequest("mark_all_notifications_read", session.accessToken, JSONObject())).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to mark notifications as read.")))
                }
                Result.success(0)
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to mark notifications as read.")))
        }
    }

    suspend fun fetchUnseenDiscussionAnime(session: AuthSession): Result<UnseenDiscussionSummary> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.success(UnseenDiscussionSummary())
        try {
            client.newCall(rpcRequest("get_unseen_discussion_anime", session.accessToken, JSONObject())).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful || !body.startsWith("{")) {
                    return@withContext Result.success(UnseenDiscussionSummary())
                }
                val obj = JSONObject(body)
                val arr = obj.optJSONArray("anime") ?: JSONArray()
                val items = buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            UnseenDiscussionAnime(
                                animeId = item.optInt("anime_id", 0),
                                animeTitle = item.optString("anime_title", "Anime"),
                                animeCoverImage = item.optString("anime_cover_image", ""),
                                unseenCount = item.optInt("unseen_count", 1),
                                latestCommentId = item.optString("latest_comment_id", ""),
                                latestThreadId = item.optString("latest_thread_id").takeIf { it.isNotBlank() && it != "null" },
                                latestReplyId = item.optString("latest_reply_id").takeIf { it.isNotBlank() && it != "null" },
                                latestSenderUsername = item.optString("latest_sender_username", "Member"),
                                latestNotificationType = item.optString("latest_notification_type", "mention_comment"),
                                latestPreview = item.optString("latest_preview", ""),
                                latestCreatedAt = item.optString("latest_created_at", "")
                            )
                        )
                    }
                }
                Result.success(
                    UnseenDiscussionSummary(
                        totalUnseen = obj.optInt("total_unseen", items.sumOf { it.unseenCount }),
                        anime = items
                    )
                )
            }
        } catch (_: Exception) {
            Result.success(UnseenDiscussionSummary())
        }
    }

    suspend fun markAnimeNotificationsRead(session: AuthSession, animeId: Int): Result<Int> = withContext(Dispatchers.IO) {
        if (!AppConfig.isSupabaseConfigured) return@withContext Result.success(0)
        try {
            val payload = JSONObject().put("p_anime_id", animeId)
            client.newCall(rpcRequest("mark_anime_notifications_read", session.accessToken, payload)).execute().use { resp ->
                val body = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception(parseError(body, "Unable to update read status.")))
                }
                val obj = JSONObject(body)
                Result.success(obj.optInt("unread_count", 0))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorSanitizer.sanitize(e, "Unable to update read status.")))
        }
    }

    private fun parseComment(obj: JSONObject): AnimeCommentItem {
        val threadsArr = obj.optJSONArray("threads") ?: JSONArray()
        val threads = buildList {
            for (i in 0 until threadsArr.length()) {
                val t = threadsArr.getJSONObject(i)
                val repliesArr = t.optJSONArray("replies") ?: JSONArray()
                val replies = buildList {
                    for (j in 0 until repliesArr.length()) {
                        add(parseReply(repliesArr.getJSONObject(j)))
                    }
                }
                add(
                    CommentReplyThread(
                        id = t.optString("id"),
                        commentId = t.optString("comment_id", obj.optString("id")),
                        targetUserId = t.optString("target_user_id"),
                        targetUsername = t.optString("target_username", "Member"),
                        targetAvatarKey = t.optString("target_avatar_key", "avatar_1").ifBlank { "avatar_1" },
                        targetAvatarUrl = t.optString("target_avatar_url").takeIf { it.isNotBlank() && it != "null" },
                        createdAt = t.optString("created_at", ""),
                        replies = replies
                    )
                )
            }
        }
        return AnimeCommentItem(
            id = obj.optString("id"),
            animeId = obj.optInt("anime_id", 0),
            userId = obj.optString("user_id"),
            username = obj.optString("username", "Member"),
            avatarKey = obj.optString("avatar_key", "avatar_1").ifBlank { "avatar_1" },
            avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" },
            content = obj.optString("content", ""),
            createdAt = obj.optString("created_at", ""),
            updatedAt = obj.optString("updated_at", ""),
            mentions = parseMentions(obj.optJSONArray("mentions")),
            threads = threads
        )
    }

    private fun parseReply(obj: JSONObject): CommentReplyItem {
        return CommentReplyItem(
            id = obj.optString("id"),
            threadId = obj.optString("thread_id"),
            userId = obj.optString("user_id"),
            username = obj.optString("username", "Member"),
            avatarKey = obj.optString("avatar_key", "avatar_1").ifBlank { "avatar_1" },
            avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" },
            content = obj.optString("content", ""),
            createdAt = obj.optString("created_at", ""),
            updatedAt = obj.optString("updated_at", ""),
            mentions = parseMentions(obj.optJSONArray("mentions"))
        )
    }

    private fun parseMentions(arr: JSONArray?): List<ContentMention> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                add(
                    ContentMention(
                        id = m.optString("id", ""),
                        mentionedUserId = m.optString("mentioned_user_id", ""),
                        mentionedUsername = m.optString("mentioned_username", ""),
                        occurrencePosition = m.optInt("occurrence_position", 0)
                    )
                )
            }
        }
    }

    private fun parseError(body: String, fallback: String): String {
        val raw = runCatching {
            val obj = JSONObject(body)
            obj.optString("message").takeIf { it.isNotBlank() }
                ?: obj.optString("hint").takeIf { it.isNotBlank() }
                ?: fallback
        }.getOrDefault(fallback)
        if (raw.contains("PGRST202", ignoreCase = true) || raw.contains("Could not find the function", ignoreCase = true)) {
            return "Discussion SQL migration is pending in Supabase. Please run the 20261121 migration in SQL Editor."
        }
        return ErrorSanitizer.sanitize(raw, fallback)
    }
}
