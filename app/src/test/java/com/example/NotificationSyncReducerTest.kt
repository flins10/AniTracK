package com.example

import com.example.viewmodel.NotificationSyncReducer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSyncReducerTest {

    private val uid = "user-123"

    @Test
    fun startupFetchAndLocalReadThenRealtimeEchoDoesNotDoubleDecrement() {
        val reducer = NotificationSyncReducer()
        val gen0 = reducer.currentGeneration()

        // Startup authoritative fetch returns 2 unread notifications
        assertTrue(reducer.applyAuthoritativeUnreadIds(setOf("n1", "n2"), gen0))
        assertEquals(2, reducer.unreadCount)

        // User opens n1 locally -> immediately drops to 1
        assertEquals(1, reducer.markLocalRead("n1"))
        assertEquals(1, reducer.unreadCount)

        // Supabase Realtime echoes UPDATE (false -> true) for n1 -> MUST NOT double-decrement
        val echo = reducer.onRealtimeUpdate(
            notificationId = "n1",
            recipientUserId = uid,
            currentUserId = uid,
            oldIsRead = false,
            newIsRead = true,
            readAt = "2026-11-23T12:00:00Z"
        )
        assertEquals(0, echo.delta)
        assertEquals(1, echo.unreadCount)
        assertFalse(echo.needsReconcile)
    }

    @Test
    fun realtimeUpdateFromAnotherDeviceDecrementsOnceAndIgnoresDuplicates() {
        val reducer = NotificationSyncReducer()
        reducer.applyAuthoritativeUnreadIds(setOf("n1", "n2"), reducer.currentGeneration())
        assertEquals(2, reducer.unreadCount)

        // Another device marks n2 read -> Realtime UPDATE arrives
        val first = reducer.onRealtimeUpdate(
            notificationId = "n2",
            recipientUserId = uid,
            currentUserId = uid,
            oldIsRead = false,
            newIsRead = true,
            readAt = "2026-11-23T12:01:00Z"
        )
        assertEquals(-1, first.delta)
        assertEquals(1, first.unreadCount)

        // Duplicate delivery of same UPDATE -> ignored
        val dup = reducer.onRealtimeUpdate(
            notificationId = "n2",
            recipientUserId = uid,
            currentUserId = uid,
            oldIsRead = false,
            newIsRead = true,
            readAt = "2026-11-23T12:01:00Z"
        )
        assertEquals(0, dup.delta)
        assertEquals(1, dup.unreadCount)

        // Last unread notification read -> count stays at 0
        val second = reducer.onRealtimeUpdate(
            notificationId = "n1",
            recipientUserId = uid,
            currentUserId = uid,
            oldIsRead = false,
            newIsRead = true,
            readAt = "2026-11-23T12:02:00Z"
        )
        assertEquals(-1, second.delta)
        assertEquals(0, second.unreadCount)
    }

    @Test
    fun realtimeInsertIncrementsExactlyOnceAndFiltersOtherRecipients() {
        val reducer = NotificationSyncReducer()
        reducer.applyAuthoritativeUnreadIds(emptySet(), reducer.currentGeneration())
        assertEquals(0, reducer.unreadCount)

        // Different recipient must be ignored
        val other = reducer.onRealtimeInsert("n9", "other-user", uid, isRead = false)
        assertEquals(0, other.delta)
        assertEquals(0, reducer.unreadCount)

        // New unread notification for authenticated recipient -> +1
        val inserted = reducer.onRealtimeInsert("n3", uid, uid, isRead = false)
        assertEquals(1, inserted.delta)
        assertEquals(1, reducer.unreadCount)

        // Duplicate INSERT delivery -> ignored
        val dupInsert = reducer.onRealtimeInsert("n3", uid, uid, isRead = false)
        assertEquals(0, dupInsert.delta)
        assertEquals(1, reducer.unreadCount)
    }

    @Test
    fun staleFetchCannotOverwriteNewerLocalReadMutation() {
        val reducer = NotificationSyncReducer()
        reducer.applyAuthoritativeUnreadIds(setOf("n1"), reducer.currentGeneration())
        assertEquals(1, reducer.unreadCount)

        // A background fetch begins before the user opens n1
        val staleGen = reducer.currentGeneration()

        // User opens n1 while fetch is still in flight
        assertEquals(0, reducer.markLocalRead("n1"))
        assertEquals(0, reducer.unreadCount)

        // Stale fetch completes with old unread set {"n1"} -> MUST BE IGNORED
        assertFalse(reducer.applyAuthoritativeUnreadIds(setOf("n1"), staleGen))
        assertEquals(0, reducer.unreadCount)

        // Fresh reconnect reconciliation with empty set keeps count at 0
        val freshGen = reducer.currentGeneration()
        assertTrue(reducer.applyAuthoritativeUnreadIds(emptySet(), freshGen))
        assertEquals(0, reducer.unreadCount)
    }

    @Test
    fun markAllReadAndUnknownUpdateTriggersSafeReconcile() {
        val reducer = NotificationSyncReducer()
        reducer.applyAuthoritativeUnreadIds(setOf("n1", "n2", "n3"), reducer.currentGeneration())
        assertEquals(3, reducer.unreadCount)

        assertEquals(0, reducer.markAllLocalRead())
        assertEquals(0, reducer.unreadCount)

        // Unknown row UPDATE without old_record requests authoritative reconcile without going negative
        val unknown = reducer.onRealtimeUpdate(
            notificationId = "n-unknown",
            recipientUserId = uid,
            currentUserId = uid,
            oldIsRead = null,
            newIsRead = true,
            readAt = "2026-11-23T12:05:00Z"
        )
        assertEquals(0, unknown.unreadCount)
        assertTrue(unknown.needsReconcile)
    }
}
