-- ============================================================================
-- ANIME UNIVERSE — Migration: Quoted Comment Replies & Real-Time Notifications
-- Date: 2026-11-23
-- Safe, idempotent, non-destructive forward migration.
-- ============================================================================

-- 1. ADD QUOTED & PARENT CONTEXT COLUMNS TO COMMENT_REPLIES
ALTER TABLE public.comment_replies
    ADD COLUMN IF NOT EXISTS parent_comment_id UUID,
    ADD COLUMN IF NOT EXISTS quoted_comment_id UUID REFERENCES public.anime_comments(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS quoted_reply_id UUID REFERENCES public.comment_replies(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS quoted_username TEXT,
    ADD COLUMN IF NOT EXISTS quoted_preview TEXT;

CREATE INDEX IF NOT EXISTS idx_comment_replies_quoted_reply
    ON public.comment_replies (quoted_reply_id)
    WHERE quoted_reply_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_comment_replies_parent_comment
    ON public.comment_replies (parent_comment_id)
    WHERE parent_comment_id IS NOT NULL;

-- Backfill parent_comment_id & quoted_comment_id for existing rows without altering existing reply-to-reply quotes
UPDATE public.comment_replies cr
SET
    quoted_comment_id = COALESCE(cr.quoted_comment_id, t.comment_id),
    parent_comment_id = COALESCE(cr.parent_comment_id, cr.quoted_reply_id, cr.quoted_comment_id, t.comment_id)
FROM public.comment_reply_threads t
WHERE cr.thread_id = t.id
  AND (cr.parent_comment_id IS NULL OR cr.quoted_comment_id IS NULL);

-- 2. ENABLE SUPABASE REALTIME FOR USER NOTIFICATIONS (IDEMPOTENT)
ALTER TABLE public.user_notifications REPLICA IDENTITY FULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        BEGIN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.user_notifications;
        EXCEPTION WHEN duplicate_object THEN
            NULL;
        END;
    END IF;
END $$;

-- 3. UPGRADE RPC: create_thread_reply (STORES QUOTED PARENT & NOTIFIES COMMENT + REPLY + MENTIONS)
DROP FUNCTION IF EXISTS public.create_thread_reply(UUID, UUID, TEXT);
DROP FUNCTION IF EXISTS public.create_thread_reply(UUID, UUID, TEXT, UUID[]);
DROP FUNCTION IF EXISTS public.create_thread_reply(UUID, UUID, TEXT, UUID[], UUID, TEXT, TEXT);

CREATE OR REPLACE FUNCTION public.create_thread_reply(
    p_comment_id UUID,
    p_target_user_id UUID,
    p_content TEXT,
    p_tagged_user_ids UUID[] DEFAULT NULL,
    p_quoted_reply_id UUID DEFAULT NULL,
    p_quoted_username TEXT DEFAULT NULL,
    p_quoted_preview TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_clean TEXT := trim(COALESCE(p_content, ''));
    v_anime_id INTEGER;
    v_comment_author_id UUID;
    v_comment_author_username TEXT;
    v_comment_content TEXT;
    v_quoted_reply_id UUID;
    v_quoted_reply_author_id UUID;
    v_quoted_reply_username TEXT;
    v_quoted_reply_content TEXT;
    v_quoted_username TEXT;
    v_quoted_preview TEXT;
    v_thread_id UUID;
    v_thread_created_at TIMESTAMPTZ;
    v_reply_id UUID;
    v_reply_created_at TIMESTAMPTZ;
    v_reply_updated_at TIMESTAMPTZ;
    v_exact_replied_author_id UUID;
    v_parent_comment_id UUID;
    v_author_username TEXT;
    v_author_avatar_key TEXT;
    v_author_avatar_url TEXT;
    v_target_username TEXT;
    v_target_avatar_key TEXT;
    v_target_avatar_url TEXT;
    v_preview TEXT;
    v_mentions_json JSONB;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in to post a reply.';
    END IF;
    IF char_length(v_clean) < 1 THEN
        RAISE EXCEPTION 'Reply cannot be empty.';
    END IF;
    IF char_length(v_clean) > 2000 THEN
        RAISE EXCEPTION 'Reply cannot exceed 2000 characters.';
    END IF;

    SELECT
        c.anime_id,
        c.user_id,
        COALESCE(cp.username, 'Member'),
        c.content
    INTO
        v_anime_id,
        v_comment_author_id,
        v_comment_author_username,
        v_comment_content
    FROM public.anime_comments c
    LEFT JOIN public.profiles cp ON cp.id = c.user_id
    WHERE c.id = p_comment_id;

    IF v_anime_id IS NULL THEN
        RAISE EXCEPTION 'Original comment not found.';
    END IF;

    SELECT p.username, COALESCE(NULLIF(p.avatar_key, ''), 'avatar_1'), p.avatar_url
    INTO v_author_username, v_author_avatar_key, v_author_avatar_url
    FROM public.profiles p
    WHERE p.id = v_uid;

    IF v_author_username IS NULL THEN
        RAISE EXCEPTION 'Complete your profile before replying.';
    END IF;

    SELECT p.username, COALESCE(NULLIF(p.avatar_key, ''), 'avatar_1'), p.avatar_url
    INTO v_target_username, v_target_avatar_key, v_target_avatar_url
    FROM public.profiles p
    WHERE p.id = p_target_user_id;

    IF v_target_username IS NULL THEN
        RAISE EXCEPTION 'Target user for reply thread does not exist.';
    END IF;

    -- Resolve exact quoted reply whenever replying to a reply (never overwrite with root comment)
    IF p_quoted_reply_id IS NOT NULL THEN
        SELECT
            cr.id,
            cr.user_id,
            COALESCE(rp.username, 'Member'),
            cr.content
        INTO
            v_quoted_reply_id,
            v_quoted_reply_author_id,
            v_quoted_reply_username,
            v_quoted_reply_content
        FROM public.comment_replies cr
        JOIN public.comment_reply_threads t ON t.id = cr.thread_id
        LEFT JOIN public.profiles rp ON rp.id = cr.user_id
        WHERE cr.id = p_quoted_reply_id
          AND t.comment_id = p_comment_id
        LIMIT 1;
    END IF;

    IF v_quoted_reply_id IS NOT NULL THEN
        v_parent_comment_id := v_quoted_reply_id;
        v_quoted_username := COALESCE(
            NULLIF(v_quoted_reply_username, ''),
            NULLIF(trim(COALESCE(p_quoted_username, '')), ''),
            v_target_username
        );
        v_quoted_preview := left(
            trim(
                COALESCE(
                    NULLIF(v_quoted_reply_content, ''),
                    NULLIF(trim(COALESCE(p_quoted_preview, '')), ''),
                    ''
                )
            ),
            180
        );
        v_exact_replied_author_id := v_quoted_reply_author_id;
    ELSE
        v_parent_comment_id := p_comment_id;
        v_quoted_username := COALESCE(
            NULLIF(trim(COALESCE(p_quoted_username, '')), ''),
            NULLIF(v_comment_author_username, ''),
            v_target_username
        );
        v_quoted_preview := left(
            trim(
                COALESCE(
                    NULLIF(trim(COALESCE(p_quoted_preview, '')), ''),
                    v_comment_content,
                    ''
                )
            ),
            180
        );
        v_exact_replied_author_id := v_comment_author_id;
    END IF;

    -- Concurrency-safe atomic thread create-or-continue for (comment_id, target_user_id)
    INSERT INTO public.comment_reply_threads (comment_id, target_user_id)
    VALUES (p_comment_id, p_target_user_id)
    ON CONFLICT (comment_id, target_user_id) DO NOTHING;

    SELECT id, created_at
    INTO v_thread_id, v_thread_created_at
    FROM public.comment_reply_threads
    WHERE comment_id = p_comment_id
      AND target_user_id = p_target_user_id;

    INSERT INTO public.comment_replies (
        thread_id,
        user_id,
        content,
        parent_comment_id,
        quoted_comment_id,
        quoted_reply_id,
        quoted_username,
        quoted_preview
    )
    VALUES (
        v_thread_id,
        v_uid,
        v_clean,
        v_parent_comment_id,
        p_comment_id,
        v_quoted_reply_id,
        v_quoted_username,
        v_quoted_preview
    )
    RETURNING id, created_at, updated_at
    INTO v_reply_id, v_reply_created_at, v_reply_updated_at;

    v_preview := left(v_clean, 160);

    -- 1. Notify author of the EXACT comment or reply being replied to (never self)
    IF v_exact_replied_author_id IS NOT NULL AND v_exact_replied_author_id <> v_uid THEN
        INSERT INTO public.user_notifications (
            recipient_user_id,
            sender_user_id,
            notification_type,
            anime_id,
            comment_id,
            thread_id,
            reply_id,
            preview_text
        ) VALUES (
            v_exact_replied_author_id,
            v_uid,
            'thread_reply',
            v_anime_id,
            p_comment_id,
            v_thread_id,
            v_reply_id,
            v_preview
        )
        ON CONFLICT DO NOTHING;
    END IF;

    -- 2. Also notify thread target user if distinct from exact replied author when replying at thread root (never self)
    IF v_quoted_reply_id IS NULL
       AND p_target_user_id IS NOT NULL
       AND p_target_user_id <> v_uid
       AND p_target_user_id IS DISTINCT FROM v_exact_replied_author_id THEN
        INSERT INTO public.user_notifications (
            recipient_user_id,
            sender_user_id,
            notification_type,
            anime_id,
            comment_id,
            thread_id,
            reply_id,
            preview_text
        ) VALUES (
            p_target_user_id,
            v_uid,
            'thread_reply',
            v_anime_id,
            p_comment_id,
            v_thread_id,
            v_reply_id,
            v_preview
        )
        ON CONFLICT DO NOTHING;
    END IF;

    -- 4. Validate & store up to 5 mentions + notify mentioned users without duplicating prior notifications
    INSERT INTO public.user_notifications (
        recipient_user_id,
        sender_user_id,
        notification_type,
        anime_id,
        comment_id,
        thread_id,
        reply_id,
        preview_text
    )
    SELECT DISTINCT
        m.mentioned_user_id,
        v_uid,
        'mention_reply',
        v_anime_id,
        p_comment_id,
        v_thread_id,
        v_reply_id,
        v_preview
    FROM public._sync_content_mentions(v_reply_id, 'reply', v_anime_id, v_uid, v_clean, p_tagged_user_ids) m
    WHERE m.mentioned_user_id <> v_uid
    ON CONFLICT DO NOTHING;

    SELECT COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'id', cm.id,
                'mentioned_user_id', cm.mentioned_user_id,
                'mentioned_username', cm.mentioned_username,
                'occurrence_position', cm.occurrence_position
            ) ORDER BY cm.occurrence_position ASC
        ),
        '[]'::jsonb
    )
    INTO v_mentions_json
    FROM public.content_mentions cm
    WHERE cm.content_id = v_reply_id
      AND cm.content_type = 'reply';

    RETURN jsonb_build_object(
        'thread', jsonb_build_object(
            'id', v_thread_id,
            'comment_id', p_comment_id,
            'target_user_id', p_target_user_id,
            'target_username', v_target_username,
            'target_avatar_key', v_target_avatar_key,
            'target_avatar_url', v_target_avatar_url,
            'created_at', v_thread_created_at
        ),
        'reply', jsonb_build_object(
            'id', v_reply_id,
            'thread_id', v_thread_id,
            'user_id', v_uid,
            'username', v_author_username,
            'avatar_key', v_author_avatar_key,
            'avatar_url', v_author_avatar_url,
            'content', v_clean,
            'parent_comment_id', v_parent_comment_id,
            'quoted_comment_id', p_comment_id,
            'quoted_reply_id', v_quoted_reply_id,
            'quoted_username', v_quoted_username,
            'quoted_preview', v_quoted_preview,
            'created_at', v_reply_created_at,
            'updated_at', v_reply_updated_at,
            'mentions', v_mentions_json
        )
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_thread_reply(UUID, UUID, TEXT, UUID[], UUID, TEXT, TEXT) TO authenticated;

-- 4. UPGRADE RPC: get_anime_discussion_page (INCLUDES QUOTED SNAPSHOT PER REPLY)
CREATE OR REPLACE FUNCTION public.get_anime_discussion_page(
    p_anime_id INTEGER,
    p_limit INTEGER DEFAULT 15,
    p_offset INTEGER DEFAULT 0
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_limit INTEGER := LEAST(GREATEST(COALESCE(p_limit, 15), 1), 50);
    v_offset INTEGER := GREATEST(COALESCE(p_offset, 0), 0);
    v_total INTEGER;
    v_comments JSONB;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in to view anime discussions.';
    END IF;

    SELECT COUNT(*)::INTEGER
    INTO v_total
    FROM public.anime_comments
    WHERE anime_id = p_anime_id;

    SELECT COALESCE(
        jsonb_agg(comment_obj ORDER BY created_at DESC),
        '[]'::jsonb
    )
    INTO v_comments
    FROM (
        SELECT
            c.created_at,
            jsonb_build_object(
                'id', c.id,
                'anime_id', c.anime_id,
                'user_id', c.user_id,
                'username', COALESCE(p.username, 'Member'),
                'avatar_key', COALESCE(NULLIF(p.avatar_key, ''), 'avatar_1'),
                'avatar_url', p.avatar_url,
                'content', c.content,
                'created_at', c.created_at,
                'updated_at', c.updated_at,
                'mentions', COALESCE((
                    SELECT jsonb_agg(
                        jsonb_build_object(
                            'id', cm.id,
                            'mentioned_user_id', cm.mentioned_user_id,
                            'mentioned_username', cm.mentioned_username,
                            'occurrence_position', cm.occurrence_position
                        ) ORDER BY cm.occurrence_position ASC
                    )
                    FROM public.content_mentions cm
                    WHERE cm.content_id = c.id
                      AND cm.content_type = 'comment'
                ), '[]'::jsonb),
                'threads', COALESCE((
                    SELECT jsonb_agg(
                        jsonb_build_object(
                            'id', t.id,
                            'comment_id', t.comment_id,
                            'target_user_id', t.target_user_id,
                            'target_username', COALESCE(tp.username, 'Member'),
                            'target_avatar_key', COALESCE(NULLIF(tp.avatar_key, ''), 'avatar_1'),
                            'target_avatar_url', tp.avatar_url,
                            'created_at', t.created_at,
                            'replies', COALESCE((
                                SELECT jsonb_agg(
                                    jsonb_build_object(
                                        'id', r.id,
                                        'thread_id', r.thread_id,
                                        'user_id', r.user_id,
                                        'username', COALESCE(rp.username, 'Member'),
                                        'avatar_key', COALESCE(NULLIF(rp.avatar_key, ''), 'avatar_1'),
                                        'avatar_url', rp.avatar_url,
                                        'content', r.content,
                                        'parent_comment_id', COALESCE(r.parent_comment_id, r.quoted_reply_id, r.quoted_comment_id, t.comment_id),
                                        'quoted_comment_id', COALESCE(r.quoted_comment_id, t.comment_id),
                                        'quoted_reply_id', COALESCE(
                                            r.quoted_reply_id,
                                            CASE
                                                WHEN r.parent_comment_id IS NOT NULL AND r.parent_comment_id <> t.comment_id THEN r.parent_comment_id
                                                ELSE NULL
                                            END
                                        ),
                                        'quoted_username', CASE
                                            WHEN COALESCE(r.quoted_reply_id, CASE WHEN r.parent_comment_id IS NOT NULL AND r.parent_comment_id <> t.comment_id THEN r.parent_comment_id ELSE NULL END) IS NOT NULL THEN
                                                COALESCE(NULLIF(r.quoted_username, ''), qrp.username, 'Member')
                                            ELSE
                                                COALESCE(NULLIF(r.quoted_username, ''), p.username, 'Member')
                                        END,
                                        'quoted_preview', CASE
                                            WHEN COALESCE(r.quoted_reply_id, CASE WHEN r.parent_comment_id IS NOT NULL AND r.parent_comment_id <> t.comment_id THEN r.parent_comment_id ELSE NULL END) IS NOT NULL THEN
                                                COALESCE(NULLIF(r.quoted_preview, ''), left(trim(qr.content), 180), '')
                                            ELSE
                                                COALESCE(NULLIF(r.quoted_preview, ''), left(trim(c.content), 180))
                                        END,
                                        'created_at', r.created_at,
                                        'updated_at', r.updated_at,
                                        'mentions', COALESCE((
                                            SELECT jsonb_agg(
                                                jsonb_build_object(
                                                    'id', rm.id,
                                                    'mentioned_user_id', rm.mentioned_user_id,
                                                    'mentioned_username', rm.mentioned_username,
                                                    'occurrence_position', rm.occurrence_position
                                                ) ORDER BY rm.occurrence_position ASC
                                            )
                                            FROM public.content_mentions rm
                                            WHERE rm.content_id = r.id
                                              AND rm.content_type = 'reply'
                                        ), '[]'::jsonb)
                                    ) ORDER BY r.created_at ASC
                                )
                                FROM public.comment_replies r
                                LEFT JOIN public.profiles rp ON rp.id = r.user_id
                                LEFT JOIN public.comment_replies qr
                                    ON qr.id = COALESCE(
                                        r.quoted_reply_id,
                                        CASE
                                            WHEN r.parent_comment_id IS NOT NULL AND r.parent_comment_id <> t.comment_id THEN r.parent_comment_id
                                            ELSE NULL
                                        END
                                    )
                                LEFT JOIN public.profiles qrp ON qrp.id = qr.user_id
                                WHERE r.thread_id = t.id
                            ), '[]'::jsonb)
                        ) ORDER BY t.created_at ASC
                    )
                    FROM public.comment_reply_threads t
                    LEFT JOIN public.profiles tp ON tp.id = t.target_user_id
                    WHERE t.comment_id = c.id
                      AND EXISTS (SELECT 1 FROM public.comment_replies rx WHERE rx.thread_id = t.id)
                ), '[]'::jsonb)
            ) AS comment_obj
        FROM public.anime_comments c
        LEFT JOIN public.profiles p ON p.id = c.user_id
        WHERE c.anime_id = p_anime_id
        ORDER BY c.created_at DESC
        LIMIT v_limit OFFSET v_offset
    ) page_rows;

    RETURN jsonb_build_object(
        'anime_id', p_anime_id,
        'total_comments', v_total,
        'has_more', (v_offset + v_limit) < v_total,
        'comments', v_comments
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_anime_discussion_page(INTEGER, INTEGER, INTEGER) TO authenticated;
