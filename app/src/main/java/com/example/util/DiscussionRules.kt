package com.example.util

import com.example.model.AnimeCommentItem
import com.example.model.CommentReplyItem
import com.example.model.CommentReplyThread

data class ExtractedMentionToken(
    val username: String,
    val startIndex: Int,
    val endIndex: Int
)

data class MentionValidationResult(
    val isValid: Boolean,
    val totalEntries: Int,
    val uniqueUsernamesLower: Set<String>,
    val tokens: List<ExtractedMentionToken>,
    val errorMessage: String? = null
)

object DiscussionRules {
    const val MAX_MENTIONS_PER_CONTENT = 5
    const val MAX_CONTENT_LENGTH = 2000

    private fun isWordChar(ch: Char): Boolean = ch.isLetterOrDigit() || ch == '_'
    private fun isHandleChar(ch: Char): Boolean = ch.isLetterOrDigit() || ch == '_' || ch == '.' || ch == '-'

    /**
     * Extracts every @username mention occurrence in order, preserving duplicates
     * and recording each occurrence's start/end index.
     */
    fun extractMentionTokens(text: String): List<ExtractedMentionToken> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<ExtractedMentionToken>()
        var i = 0
        val n = text.length
        while (i < n) {
            if (text[i] == '@' && (i == 0 || !isWordChar(text[i - 1]))) {
                var end = i + 1
                while (end < n && isHandleChar(text[end])) {
                    end++
                }
                val rawHandle = text.substring(i + 1, end)
                val trimmedHandle = rawHandle.trim('.')
                if (trimmedHandle.length in 2..32) {
                    result.add(
                        ExtractedMentionToken(
                            username = trimmedHandle,
                            startIndex = i,
                            endIndex = i + 1 + trimmedHandle.length
                        )
                    )
                }
                i = end
            } else {
                i++
            }
        }
        return result
    }

    /**
     * Validates content length and @mention limits:
     * - Up to 5 mention entries per comment or reply.
     * - Duplicate mentions of the same user ARE allowed and each counts toward the 5-entry cap.
     * - Repeated mentions do NOT inflate the unique-user set.
     */
    fun validateContentMentions(
        text: String,
        maxMentions: Int = MAX_MENTIONS_PER_CONTENT
    ): MentionValidationResult {
        val trimmed = text.trim()
        val tokens = extractMentionTokens(text)
        val uniqueLower = tokens.map { it.username.lowercase() }.toSet()
        return when {
            trimmed.isEmpty() -> MentionValidationResult(
                isValid = false,
                totalEntries = tokens.size,
                uniqueUsernamesLower = uniqueLower,
                tokens = tokens,
                errorMessage = "Please enter a message before posting."
            )
            trimmed.length > MAX_CONTENT_LENGTH -> MentionValidationResult(
                isValid = false,
                totalEntries = tokens.size,
                uniqueUsernamesLower = uniqueLower,
                tokens = tokens,
                errorMessage = "Message cannot exceed $MAX_CONTENT_LENGTH characters."
            )
            tokens.size > maxMentions -> MentionValidationResult(
                isValid = false,
                totalEntries = tokens.size,
                uniqueUsernamesLower = uniqueLower,
                tokens = tokens,
                errorMessage = "Maximum $maxMentions @mentions allowed per comment or reply (${tokens.size}/$maxMentions)."
            )
            else -> MentionValidationResult(
                isValid = true,
                totalEntries = tokens.size,
                uniqueUsernamesLower = uniqueLower,
                tokens = tokens,
                errorMessage = null
            )
        }
    }

    /**
     * Detects whether the user is actively typing an @username query right before [cursorPosition].
     * Returns (indexOfAtSign, partialQuery) or null if no active mention trigger is present.
     */
    fun detectActiveMentionQuery(text: String, cursorPosition: Int = text.length): Pair<Int, String>? {
        val safeCursor = cursorPosition.coerceIn(0, text.length)
        var i = safeCursor - 1
        while (i >= 0) {
            val ch = text[i]
            if (ch == '@') {
                if (i == 0 || !isWordChar(text[i - 1])) {
                    val query = text.substring(i + 1, safeCursor)
                    return if (query.length <= 32) i to query else null
                }
                return null
            }
            if (!isHandleChar(ch)) {
                return null
            }
            i--
        }
        return null
    }

    /**
     * Inserts a selected autocomplete username at [atSignIndex]..[cursorPosition].
     */
    fun insertMentionAutocomplete(
        text: String,
        atSignIndex: Int,
        cursorPosition: Int,
        selectedUsername: String
    ): Pair<String, Int> {
        val safeStart = atSignIndex.coerceIn(0, text.length)
        val safeEnd = cursorPosition.coerceIn(safeStart, text.length)
        val prefix = text.substring(0, safeStart)
        val suffix = text.substring(safeEnd)
        val insertion = "@$selectedUsername "
        val updated = prefix + insertion + suffix.trimStart(' ')
        return updated to (prefix.length + insertion.length)
    }

    /**
     * Merges a newly created reply into its comment's thread list using the target-user uniqueness rule:
     * - If a thread for (commentId, targetUserId) already exists, append the reply into that thread.
     * - Otherwise create a separate thread for that target user.
     */
    fun mergeReplyIntoComment(
        comment: AnimeCommentItem,
        thread: CommentReplyThread,
        reply: CommentReplyItem
    ): AnimeCommentItem {
        val existingIdx = comment.threads.indexOfFirst {
            it.id == thread.id ||
                (it.commentId == thread.commentId && it.targetUserId == thread.targetUserId)
        }
        val updatedThreads = if (existingIdx >= 0) {
            comment.threads.mapIndexed { idx, existing ->
                if (idx == existingIdx) {
                    val dedupedReplies = existing.replies.filterNot { it.id == reply.id } + reply
                    existing.copy(replies = dedupedReplies)
                } else {
                    existing
                }
            }
        } else {
            comment.threads + thread.copy(replies = listOf(reply))
        }
        return comment.copy(threads = updatedThreads)
    }

    /**
     * Computes deduplicated notification recipients for a comment or thread reply:
     * - Direct thread replies automatically notify [targetUserId] even without an @mention.
     * - Explicit @mentions notify each mentioned user once per event (duplicates deduplicated).
     * - Self-mentions and self-replies never notify [senderUserId].
     */
    fun resolveNotificationRecipients(
        senderUserId: String,
        targetUserId: String?,
        mentionedUserIds: List<String>
    ): List<Pair<String, String>> {
        val seen = linkedSetOf<String>()
        val result = mutableListOf<Pair<String, String>>()

        if (!targetUserId.isNullOrBlank() && targetUserId != senderUserId) {
            seen.add(targetUserId)
            result.add(targetUserId to "thread_reply")
        }

        val mentionType = if (targetUserId != null) "mention_reply" else "mention_comment"
        mentionedUserIds.forEach { uid ->
            if (uid.isNotBlank() && uid != senderUserId && seen.add(uid)) {
                result.add(uid to mentionType)
            }
        }
        return result
    }
}
