package com.example

import com.example.model.AnimeCommentItem
import com.example.model.CommentReplyItem
import com.example.model.CommentReplyThread
import com.example.util.DiscussionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscussionRulesTest {

    @Test
    fun duplicateMentionsAreAllowedAndEachOccurrenceIsTracked() {
        val result = DiscussionRules.validateContentMentions("Hey @alex check this out @alex @alex!")
        assertTrue(result.isValid)
        assertEquals(3, result.totalEntries)
        assertEquals(1, result.uniqueUsernamesLower.size)
        assertEquals(setOf("alex"), result.uniqueUsernamesLower)
        assertEquals(listOf(4, 25, 31), result.tokens.map { it.startIndex })
    }

    @Test
    fun maxFiveMentionEntriesEnforcedIncludingDuplicates() {
        val fiveOk = DiscussionRules.validateContentMentions("@a1 @a1 @a1 @a1 @a1")
        assertTrue(fiveOk.isValid)
        assertEquals(5, fiveOk.totalEntries)
        assertEquals(1, fiveOk.uniqueUsernamesLower.size)

        val sixRejected = DiscussionRules.validateContentMentions("@a1 @a1 @a1 @a1 @a1 @a1")
        assertFalse(sixRejected.isValid)
        assertEquals(6, sixRejected.totalEntries)
        assertNotNull(sixRejected.errorMessage)
    }

    @Test
    fun emailAddressInsideTextIsNotTreatedAsMention() {
        val tokens = DiscussionRules.extractMentionTokens("Reach me at user@example.com or ping @mira")
        assertEquals(1, tokens.size)
        assertEquals("mira", tokens.first().username)
    }

    @Test
    fun activeMentionQueryDetectsAndInsertsAutocomplete() {
        val detected = DiscussionRules.detectActiveMentionQuery("Great episode @mi", 17)
        assertEquals(14 to "mi", detected)

        val (updated, cursor) = DiscussionRules.insertMentionAutocomplete("Great episode @mi", 14, 17, "mira_chan")
        assertEquals("Great episode @mira_chan ", updated)
        assertEquals(updated.length, cursor)

        assertNull(DiscussionRules.detectActiveMentionQuery("Great episode @mira ", 20))
    }

    @Test
    fun multipleUsersReplyingToSameTargetJoinSingleThread() {
        val original = AnimeCommentItem(
            id = "c1",
            animeId = 100,
            userId = "userA",
            username = "UserA",
            content = "Episode 12 was incredible!",
            createdAt = "2026-11-21T10:00:00Z",
            updatedAt = "2026-11-21T10:00:00Z"
        )
        val threadA = CommentReplyThread(
            id = "t1",
            commentId = "c1",
            targetUserId = "userA",
            targetUsername = "UserA",
            createdAt = "2026-11-21T10:01:00Z"
        )
        val replyB = CommentReplyItem("r1", "t1", "userB", "UserB", content = "Agreed!", createdAt = "2026-11-21T10:01:00Z", updatedAt = "2026-11-21T10:01:00Z")
        val replyC = CommentReplyItem("r2", "t1", "userC", "UserC", content = "Same here!", createdAt = "2026-11-21T10:02:00Z", updatedAt = "2026-11-21T10:02:00Z")

        val afterB = DiscussionRules.mergeReplyIntoComment(original, threadA, replyB)
        val afterC = DiscussionRules.mergeReplyIntoComment(afterB, threadA, replyC)

        assertEquals(1, afterC.threads.size)
        assertEquals(2, afterC.threads.first().replies.size)
        assertEquals(listOf("r1", "r2"), afterC.threads.first().replies.map { it.id })
    }

    @Test
    fun repliesTargetingDifferentUsersCreateSeparateThreads() {
        val original = AnimeCommentItem(
            id = "c1",
            animeId = 100,
            userId = "userA",
            username = "UserA",
            content = "Top 5 anime of the year",
            createdAt = "2026-11-21T10:00:00Z",
            updatedAt = "2026-11-21T10:00:00Z"
        )
        val tA = CommentReplyThread("tA", "c1", "userA", "UserA", createdAt = "2026-11-21T10:01:00Z")
        val tB = CommentReplyThread("tB", "c1", "userB", "UserB", createdAt = "2026-11-21T10:02:00Z")
        val tC = CommentReplyThread("tC", "c1", "userC", "UserC", createdAt = "2026-11-21T10:03:00Z")

        var current = DiscussionRules.mergeReplyIntoComment(
            original, tA, CommentReplyItem("r1", "tA", "userB", "UserB", content = "To A", createdAt = "", updatedAt = "")
        )
        current = DiscussionRules.mergeReplyIntoComment(
            current, tB, CommentReplyItem("r2", "tB", "userC", "UserC", content = "To B", createdAt = "", updatedAt = "")
        )
        current = DiscussionRules.mergeReplyIntoComment(
            current, tC, CommentReplyItem("r3", "tC", "userD", "UserD", content = "To C", createdAt = "", updatedAt = "")
        )

        assertEquals(3, current.threads.size)
        assertEquals(listOf("userA", "userB", "userC"), current.threads.map { it.targetUserId })
    }

    @Test
    fun notificationsAutoNotifyThreadTargetDeduplicateRepeatedMentionsAndSkipSelf() {
        // Replying to UserA without any @mention still notifies UserA
        val implicitReply = DiscussionRules.resolveNotificationRecipients(
            senderUserId = "userB",
            targetUserId = "userA",
            mentionedUserIds = emptyList()
        )
        assertEquals(listOf("userA" to "thread_reply"), implicitReply)

        // Replying to UserA while mentioning UserA twice, UserC twice, and self (UserB)
        val combined = DiscussionRules.resolveNotificationRecipients(
            senderUserId = "userB",
            targetUserId = "userA",
            mentionedUserIds = listOf("userA", "userA", "userC", "userC", "userB")
        )
        assertEquals(
            listOf("userA" to "thread_reply", "userC" to "mention_reply"),
            combined
        )

        // Comment mentioning self only produces no notification
        val selfComment = DiscussionRules.resolveNotificationRecipients(
            senderUserId = "userB",
            targetUserId = null,
            mentionedUserIds = listOf("userB")
        )
        assertTrue(selfComment.isEmpty())
    }
}
