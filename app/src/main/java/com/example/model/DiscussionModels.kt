package com.example.model

data class MentionableUser(
    val id: String,
    val username: String,
    val avatarKey: String = BuiltInAvatar.AVATAR_1.key,
    val avatarUrl: String? = null
)

data class ContentMention(
    val id: String = "",
    val mentionedUserId: String,
    val mentionedUsername: String,
    val occurrencePosition: Int
)

data class CommentReplyItem(
    val id: String,
    val threadId: String,
    val userId: String,
    val username: String,
    val avatarKey: String = BuiltInAvatar.AVATAR_1.key,
    val avatarUrl: String? = null,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
    val mentions: List<ContentMention> = emptyList()
)

data class CommentReplyThread(
    val id: String,
    val commentId: String,
    val targetUserId: String,
    val targetUsername: String,
    val targetAvatarKey: String = BuiltInAvatar.AVATAR_1.key,
    val targetAvatarUrl: String? = null,
    val createdAt: String,
    val replies: List<CommentReplyItem> = emptyList()
)

data class AnimeCommentItem(
    val id: String,
    val animeId: Int,
    val userId: String,
    val username: String,
    val avatarKey: String = BuiltInAvatar.AVATAR_1.key,
    val avatarUrl: String? = null,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
    val mentions: List<ContentMention> = emptyList(),
    val threads: List<CommentReplyThread> = emptyList()
) {
    val totalReplyCount: Int
        get() = threads.sumOf { it.replies.size }
}

data class AnimeDiscussionPage(
    val animeId: Int,
    val totalComments: Int = 0,
    val hasMore: Boolean = false,
    val comments: List<AnimeCommentItem> = emptyList()
)

data class ThreadReplyCreationResult(
    val thread: CommentReplyThread,
    val reply: CommentReplyItem
)

data class CommunityEmoji(
    val id: String,
    val bucketName: String = "community_emojis",
    val storagePath: String = "",
    val fileName: String = "",
    val mimeType: String = "image/gif",
    val usageCount: Long = 0L,
    val url: String = "",
    val score: Long = usageCount
)

data class UserNotificationItem(
    val id: String,
    val recipientUserId: String,
    val senderUserId: String,
    val senderUsername: String,
    val senderAvatarKey: String = BuiltInAvatar.AVATAR_1.key,
    val senderAvatarUrl: String? = null,
    val notificationType: String, // "mention_comment" | "mention_reply" | "thread_reply"
    val communityId: String = "anime_community",
    val communityName: String = "Community",
    val animeId: Int,
    val animeTitle: String,
    val animeCoverImage: String = "",
    val commentId: String,
    val threadId: String? = null,
    val replyId: String? = null,
    val previewText: String = "",
    val isRead: Boolean = false,
    val createdAt: String = ""
) {
    val actionSummary: String
        get() = when (notificationType) {
            "thread_reply" -> "replied to you on $animeTitle"
            "mention_reply" -> "tagged you in a reply on $animeTitle"
            else -> "tagged you on $animeTitle"
        }
}

data class NotificationsPage(
    val unreadCount: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val notifications: List<UserNotificationItem> = emptyList()
)

data class NotificationJumpTarget(
    val animeId: Int,
    val commentId: String,
    val threadId: String? = null,
    val replyId: String? = null
)

data class UnseenDiscussionAnime(
    val animeId: Int,
    val animeTitle: String,
    val animeCoverImage: String = "",
    val communityId: String = "anime_community",
    val communityName: String = "Community",
    val unseenCount: Int = 1,
    val latestCommentId: String = "",
    val latestThreadId: String? = null,
    val latestReplyId: String? = null,
    val latestSenderUsername: String = "Member",
    val latestNotificationType: String = "mention_comment",
    val latestPreview: String = "",
    val latestCreatedAt: String = ""
)

data class UnseenDiscussionSummary(
    val totalUnseen: Int = 0,
    val anime: List<UnseenDiscussionAnime> = emptyList()
)
