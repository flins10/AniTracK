package com.example.viewmodel

/**
 * Deterministic, deduplicated state machine for synchronizing notification read state
 * and unread badge counts across:
 *  - Startup / reconnect / app-resume authoritative DB fetches (`recipient_user_id = uid AND is_read = false`)
 *  - Immediate optimistic local read updates when a user opens a notification
 *  - Supabase Realtime INSERT and UPDATE events (without double-counting or stale overwrites)
 */
class NotificationSyncReducer {
    private val knownReadState = HashMap<String, Boolean>()
    private val unreadIds = LinkedHashSet<String>()
    private val processedEventKeys = LinkedHashSet<String>()
    private var mutationGeneration: Long = 0L

    var unreadCount: Int = 0
        private set

    data class RealtimeOutcome(
        val unreadCount: Int,
        val delta: Int,
        val needsReconcile: Boolean = false
    )

    @Synchronized
    fun reset() {
        knownReadState.clear()
        unreadIds.clear()
        processedEventKeys.clear()
        mutationGeneration = 0L
        unreadCount = 0
    }

    @Synchronized
    fun currentGeneration(): Long = mutationGeneration

    @Synchronized
    fun beginLocalMutation(): Long {
        mutationGeneration += 1L
        return mutationGeneration
    }

    /**
     * Applies an authoritative unread snapshot fetched directly from Supabase
     * (`recipient_user_id = uid AND is_read = false`).
     * Ignores stale responses whose fetch started before a newer local mutation.
     */
    @Synchronized
    fun applyAuthoritativeUnreadIds(
        fetchedUnreadIds: Set<String>,
        fetchStartedGeneration: Long
    ): Boolean {
        if (fetchStartedGeneration < mutationGeneration) {
            return false
        }
        val cleanIds = fetchedUnreadIds.filter { it.isNotBlank() }.toSet()
        // Any previously tracked unread ID that is no longer unread on the server is now read.
        for (prevId in unreadIds) {
            if (prevId !in cleanIds) {
                knownReadState[prevId] = true
            }
        }
        unreadIds.clear()
        for (id in cleanIds) {
            unreadIds.add(id)
            knownReadState[id] = false
        }
        unreadCount = unreadIds.size
        return true
    }

    /**
     * Records read/unread states from a fetched notifications page without
     * clobbering a newer optimistic read mutation.
     */
    @Synchronized
    fun applyFetchedPage(
        items: List<Pair<String, Boolean>>,
        authoritativeUnreadCount: Int?,
        fetchStartedGeneration: Long
    ): Boolean {
        if (fetchStartedGeneration < mutationGeneration) {
            return false
        }
        for ((id, isRead) in items) {
            if (id.isBlank()) continue
            knownReadState[id] = isRead
            if (isRead) {
                unreadIds.remove(id)
            } else {
                unreadIds.add(id)
            }
        }
        if (authoritativeUnreadCount != null) {
            unreadCount = authoritativeUnreadCount.coerceAtLeast(0)
            if (unreadCount == 0) {
                for (id in unreadIds) {
                    knownReadState[id] = true
                }
                unreadIds.clear()
            }
        }
        return true
    }

    /**
     * Immediately transitions a single notification from unread -> read locally.
     * Decrements [unreadCount] at most once so a subsequent Realtime UPDATE echo
     * for the same notification will NOT double-decrement.
     */
    @Synchronized
    fun markLocalRead(notificationId: String): Int {
        if (notificationId.isBlank()) return unreadCount
        mutationGeneration += 1L
        val prev = knownReadState[notificationId]
        val wasInUnreadSet = unreadIds.remove(notificationId)
        knownReadState[notificationId] = true
        if (wasInUnreadSet || prev == false) {
            unreadCount = (unreadCount - 1).coerceAtLeast(0)
        }
        return unreadCount
    }

    /**
     * Immediately marks multiple notifications (e.g. all notifications for one anime) as read.
     */
    @Synchronized
    fun markLocalReadMany(notificationIds: Collection<String>): Int {
        if (notificationIds.isEmpty()) return unreadCount
        mutationGeneration += 1L
        var decremented = 0
        for (id in notificationIds) {
            if (id.isBlank()) continue
            val prev = knownReadState[id]
            val wasInUnreadSet = unreadIds.remove(id)
            knownReadState[id] = true
            if (wasInUnreadSet || prev == false) {
                decremented += 1
            }
        }
        if (decremented > 0) {
            unreadCount = (unreadCount - decremented).coerceAtLeast(0)
        }
        return unreadCount
    }

    /**
     * Immediately marks all known notifications as read and sets unreadCount to 0.
     */
    @Synchronized
    fun markAllLocalRead(): Int {
        mutationGeneration += 1L
        for (id in knownReadState.keys.toList()) {
            knownReadState[id] = true
        }
        unreadIds.clear()
        unreadCount = 0
        return 0
    }

    /**
     * Processes a Supabase Realtime INSERT event on `user_notifications`.
     * Increases [unreadCount] exactly once when a brand-new unread notification arrives
     * for the authenticated recipient.
     */
    @Synchronized
    fun onRealtimeInsert(
        notificationId: String,
        recipientUserId: String,
        currentUserId: String,
        isRead: Boolean
    ): RealtimeOutcome {
        if (notificationId.isBlank() || currentUserId.isBlank() || recipientUserId != currentUserId) {
            return RealtimeOutcome(unreadCount = unreadCount, delta = 0)
        }
        val eventKey = "INSERT:$notificationId:$isRead"
        if (!recordEventKey(eventKey)) {
            return RealtimeOutcome(unreadCount = unreadCount, delta = 0)
        }
        val existingState = knownReadState[notificationId]
        if (existingState != null) {
            // Already known from startup/page fetch or prior event — prevent duplicate increment.
            return RealtimeOutcome(unreadCount = unreadCount, delta = 0)
        }
        knownReadState[notificationId] = isRead
        if (!isRead) {
            unreadIds.add(notificationId)
            unreadCount += 1
            return RealtimeOutcome(unreadCount = unreadCount, delta = 1)
        }
        return RealtimeOutcome(unreadCount = unreadCount, delta = 0)
    }

    /**
     * Processes a Supabase Realtime UPDATE event on `user_notifications`.
     * Decreases [unreadCount] exactly once when `is_read` transitions from `false` to `true`,
     * ignoring echoes of updates we already applied locally or duplicate deliveries.
     */
    @Synchronized
    fun onRealtimeUpdate(
        notificationId: String,
        recipientUserId: String,
        currentUserId: String,
        oldIsRead: Boolean?,
        newIsRead: Boolean,
        readAt: String? = null
    ): RealtimeOutcome {
        if (notificationId.isBlank() || currentUserId.isBlank() || recipientUserId != currentUserId) {
            return RealtimeOutcome(unreadCount = unreadCount, delta = 0)
        }
        val eventKey = "UPDATE:$notificationId:$newIsRead:${readAt.orEmpty()}"
        if (!recordEventKey(eventKey)) {
            return RealtimeOutcome(unreadCount = unreadCount, delta = 0)
        }

        val knownState = knownReadState[notificationId]
        if (newIsRead) {
            if (knownState == true) {
                // Already transitioned to read locally or via an earlier UPDATE.
                unreadIds.remove(notificationId)
                return RealtimeOutcome(unreadCount = unreadCount, delta = 0)
            }
            val wasUnreadInSet = unreadIds.remove(notificationId)
            knownReadState[notificationId] = true
            val confirmedFalseToTrue = (oldIsRead == false) || (knownState == false) || wasUnreadInSet
            return if (confirmedFalseToTrue) {
                unreadCount = (unreadCount - 1).coerceAtLeast(0)
                RealtimeOutcome(unreadCount = unreadCount, delta = -1)
            } else {
                // Replica identity didn't provide old_record and row wasn't in local map: reconcile from DB.
                RealtimeOutcome(unreadCount = unreadCount, delta = 0, needsReconcile = true)
            }
        } else {
            // Rare case: notification explicitly marked back to unread (true -> false)
            if (knownState == false || notificationId in unreadIds) {
                return RealtimeOutcome(unreadCount = unreadCount, delta = 0)
            }
            knownReadState[notificationId] = false
            unreadIds.add(notificationId)
            return if (oldIsRead == true || knownState == true) {
                unreadCount += 1
                RealtimeOutcome(unreadCount = unreadCount, delta = 1)
            } else {
                RealtimeOutcome(unreadCount = unreadCount, delta = 0, needsReconcile = true)
            }
        }
    }

    /**
     * Reconciles exact server count returned directly after a completed mutation
     * (only if no newer mutation started since).
     */
    @Synchronized
    fun confirmServerUnreadCount(
        serverCount: Int,
        mutationGen: Long
    ): Boolean {
        if (mutationGen < mutationGeneration) return false
        val safeCount = serverCount.coerceAtLeast(0)
        unreadCount = safeCount
        if (safeCount == 0) {
            for (id in unreadIds) {
                knownReadState[id] = true
            }
            unreadIds.clear()
        }
        return true
    }

    private fun recordEventKey(key: String): Boolean {
        if (key in processedEventKeys) return false
        processedEventKeys.add(key)
        if (processedEventKeys.size > MAX_TRACKED_EVENTS) {
            val iterator = processedEventKeys.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
        return true
    }

    companion object {
        private const val MAX_TRACKED_EVENTS = 500
    }
}
