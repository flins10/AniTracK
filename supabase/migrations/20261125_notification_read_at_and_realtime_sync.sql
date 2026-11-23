-- ============================================================================
-- PERSISTENT NOTIFICATION READ STATUS (read_at) & REALTIME UNREAD COUNT SYNC
-- Idempotent migration on existing public.user_notifications table.
-- Preserves existing schema & strict RLS (authenticated recipient only).
-- ============================================================================

-- 1. Add read_at timestamp column if missing
ALTER TABLE public.user_notifications
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

-- Backfill read_at for already-read rows so historical read rows remain consistent
UPDATE public.user_notifications
SET read_at = COALESCE(read_at, created_at, NOW())
WHERE is_read = TRUE
  AND read_at IS NULL;

-- Fast partial index for exact unread count queries:
-- recipient_user_id = auth.uid() AND is_read = FALSE
CREATE INDEX IF NOT EXISTS idx_user_notifications_recipient_unread_id
    ON public.user_notifications (recipient_user_id, id)
    WHERE is_read = FALSE;

-- 2. Enforce strict Row-Level Security policies (authenticated recipient only)
ALTER TABLE public.user_notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own notifications" ON public.user_notifications;
CREATE POLICY "Users can view own notifications"
    ON public.user_notifications
    FOR SELECT
    TO authenticated
    USING (auth.uid() = recipient_user_id);

DROP POLICY IF EXISTS "Users can update own notifications" ON public.user_notifications;
CREATE POLICY "Users can update own notifications"
    ON public.user_notifications
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = recipient_user_id)
    WITH CHECK (auth.uid() = recipient_user_id);

DROP POLICY IF EXISTS "Users can delete own notifications" ON public.user_notifications;
CREATE POLICY "Users can delete own notifications"
    ON public.user_notifications
    FOR DELETE
    TO authenticated
    USING (auth.uid() = recipient_user_id);

-- 3. Guarantee Realtime UPDATE events include old_record.is_read & record.is_read
ALTER TABLE public.user_notifications REPLICA IDENTITY FULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public'
          AND tablename = 'user_notifications'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.user_notifications;
    END IF;
END
$$;

-- 4. Update RPC helpers to consistently write read_at = NOW() and compute exact unread count
CREATE OR REPLACE FUNCTION public.get_unread_notification_count()
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_count INTEGER := 0;
BEGIN
    IF v_uid IS NULL THEN
        RETURN 0;
    END IF;

    SELECT COUNT(*)::INTEGER
    INTO v_count
    FROM public.user_notifications
    WHERE recipient_user_id = v_uid
      AND is_read = FALSE;

    RETURN COALESCE(v_count, 0);
END;
$$;

CREATE OR REPLACE FUNCTION public.mark_notification_read(
    p_notification_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_now TIMESTAMPTZ := NOW();
    v_unread INTEGER := 0;
    v_updated BOOLEAN := FALSE;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    UPDATE public.user_notifications
    SET is_read = TRUE,
        read_at = COALESCE(read_at, v_now)
    WHERE id = p_notification_id
      AND recipient_user_id = v_uid
      AND is_read = FALSE;

    v_updated := FOUND;

    SELECT COUNT(*)::INTEGER
    INTO v_unread
    FROM public.user_notifications
    WHERE recipient_user_id = v_uid
      AND is_read = FALSE;

    RETURN jsonb_build_object(
        'updated', v_updated,
        'read_at', v_now,
        'unread_count', COALESCE(v_unread, 0)
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.mark_all_notifications_read()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_now TIMESTAMPTZ := NOW();
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    UPDATE public.user_notifications
    SET is_read = TRUE,
        read_at = COALESCE(read_at, v_now)
    WHERE recipient_user_id = v_uid
      AND is_read = FALSE;

    RETURN jsonb_build_object(
        'read_at', v_now,
        'unread_count', 0
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.mark_anime_notifications_read(
    p_anime_id INTEGER
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_now TIMESTAMPTZ := NOW();
    v_unread INTEGER := 0;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    UPDATE public.user_notifications
    SET is_read = TRUE,
        read_at = COALESCE(read_at, v_now)
    WHERE recipient_user_id = v_uid
      AND anime_id = p_anime_id
      AND is_read = FALSE;

    SELECT COUNT(*)::INTEGER
    INTO v_unread
    FROM public.user_notifications
    WHERE recipient_user_id = v_uid
      AND is_read = FALSE;

    RETURN jsonb_build_object(
        'anime_id', p_anime_id,
        'read_at', v_now,
        'unread_count', COALESCE(v_unread, 0)
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.get_user_notifications(
    p_limit INTEGER DEFAULT 30,
    p_offset INTEGER DEFAULT 0
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_limit INTEGER := GREATEST(1, LEAST(COALESCE(p_limit, 30), 60));
    v_offset INTEGER := GREATEST(0, COALESCE(p_offset, 0));
    v_unread INTEGER := 0;
    v_total INTEGER := 0;
    v_items JSONB := '[]'::JSONB;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    SELECT COUNT(*)::INTEGER
    INTO v_unread
    FROM public.user_notifications
    WHERE recipient_user_id = v_uid
      AND is_read = FALSE;

    SELECT COUNT(*)::INTEGER
    INTO v_total
    FROM public.user_notifications
    WHERE recipient_user_id = v_uid;

    SELECT COALESCE(jsonb_agg(item), '[]'::JSONB)
    INTO v_items
    FROM (
        SELECT jsonb_build_object(
            'id', n.id,
            'recipient_user_id', n.recipient_user_id,
            'sender_user_id', n.sender_user_id,
            'sender_username', COALESCE(NULLIF(BTRIM(p.username), ''), 'Member'),
            'sender_avatar_key', COALESCE(NULLIF(BTRIM(p.avatar_key), ''), 'avatar_1'),
            'sender_avatar_url', p.avatar_url,
            'notification_type', n.notification_type,
            'community_id', COALESCE(NULLIF(BTRIM(n.community_id), ''), 'anime_community'),
            'community_name', COALESCE(NULLIF(BTRIM(n.community_name), ''), 'Community'),
            'anime_id', n.anime_id,
            'anime_title', COALESCE(NULLIF(BTRIM(n.anime_title), ''), 'Anime #' || n.anime_id::TEXT),
            'anime_cover_image', COALESCE(n.anime_cover_image, ''),
            'comment_id', n.comment_id,
            'thread_id', n.thread_id,
            'reply_id', n.reply_id,
            'preview_text', COALESCE(n.preview_text, ''),
            'is_read', n.is_read,
            'read_at', n.read_at,
            'created_at', n.created_at
        ) AS item
        FROM public.user_notifications n
        LEFT JOIN public.profiles p ON p.id = n.sender_user_id
        WHERE n.recipient_user_id = v_uid
        ORDER BY n.created_at DESC
        LIMIT v_limit
        OFFSET v_offset
    ) sub;

    RETURN jsonb_build_object(
        'unread_count', COALESCE(v_unread, 0),
        'total_count', COALESCE(v_total, 0),
        'has_more', (v_offset + jsonb_array_length(v_items)) < COALESCE(v_total, 0),
        'notifications', v_items
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_unread_notification_count() TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_notification_read(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_all_notifications_read() TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_anime_notifications_read(INTEGER) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_user_notifications(INTEGER, INTEGER) TO authenticated;
