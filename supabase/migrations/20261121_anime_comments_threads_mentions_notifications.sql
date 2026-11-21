-- ============================================================================
-- ANIME UNIVERSE — Migration: Anime Comments, @Mentions, Target-User Reply
-- Threads & Real-Time Notifications
-- Date: 2026-11-21
-- Safe, idempotent, non-destructive forward migration.
-- Preserves all existing tables, data, community_notes, and user_anime rows.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. ANIME COMMENTS TABLE
CREATE TABLE IF NOT EXISTS public.anime_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anime_id INTEGER NOT NULL REFERENCES public.anime(anilist_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL CHECK (char_length(trim(content)) BETWEEN 1 AND 2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_anime_comments_anime_created
    ON public.anime_comments (anime_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_anime_comments_user
    ON public.anime_comments (user_id, created_at DESC);

-- 2. COMMENT REPLY THREADS TABLE (Strictly unique per original comment + target user)
CREATE TABLE IF NOT EXISTS public.comment_reply_threads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id UUID NOT NULL REFERENCES public.anime_comments(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_comment_reply_thread_target UNIQUE (comment_id, target_user_id)
);

CREATE INDEX IF NOT EXISTS idx_comment_reply_threads_comment_created
    ON public.comment_reply_threads (comment_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_comment_reply_threads_target
    ON public.comment_reply_threads (target_user_id);

-- 3. COMMENT REPLIES TABLE
CREATE TABLE IF NOT EXISTS public.comment_replies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id UUID NOT NULL REFERENCES public.comment_reply_threads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL CHECK (char_length(trim(content)) BETWEEN 1 AND 2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comment_replies_thread_created
    ON public.comment_replies (thread_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_comment_replies_user
    ON public.comment_replies (user_id, created_at DESC);

-- 4. CONTENT MENTIONS TABLE (Stores individual mention occurrences, allowing duplicates up to 5)
CREATE TABLE IF NOT EXISTS public.content_mentions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id UUID NOT NULL,
    content_type TEXT NOT NULL CHECK (content_type IN ('comment', 'reply')),
    anime_id INTEGER NOT NULL REFERENCES public.anime(anilist_id) ON DELETE CASCADE,
    mentioned_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    mentioned_username TEXT NOT NULL,
    occurrence_position INTEGER NOT NULL CHECK (occurrence_position >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_content_mentions_lookup
    ON public.content_mentions (content_type, content_id, occurrence_position ASC);
CREATE INDEX IF NOT EXISTS idx_content_mentions_user_anime
    ON public.content_mentions (mentioned_user_id, anime_id, created_at DESC);

-- 5. USER NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS public.user_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    sender_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    notification_type TEXT NOT NULL CHECK (notification_type IN ('mention_comment', 'mention_reply', 'thread_reply')),
    anime_id INTEGER NOT NULL REFERENCES public.anime(anilist_id) ON DELETE CASCADE,
    comment_id UUID NOT NULL REFERENCES public.anime_comments(id) ON DELETE CASCADE,
    thread_id UUID REFERENCES public.comment_reply_threads(id) ON DELETE CASCADE,
    reply_id UUID REFERENCES public.comment_replies(id) ON DELETE CASCADE,
    preview_text TEXT NOT NULL DEFAULT '',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_notifications_no_self CHECK (recipient_user_id <> sender_user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_notifications_recipient_feed
    ON public.user_notifications (recipient_user_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_notifications_comment
    ON public.user_notifications (comment_id);

-- Deduplication indexes: at most 1 notification per recipient per comment or per reply event
-- (Prevents spam when someone mentions @user multiple times in the same comment/reply,
--  or mentions the thread target user in a direct reply)
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_notifications_dedup_comment
    ON public.user_notifications (recipient_user_id, comment_id)
    WHERE reply_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_notifications_dedup_reply
    ON public.user_notifications (recipient_user_id, reply_id)
    WHERE reply_id IS NOT NULL;

-- 6. ROW LEVEL SECURITY POLICIES
ALTER TABLE public.anime_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comment_reply_threads ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comment_replies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.content_mentions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_notifications ENABLE ROW LEVEL SECURITY;

-- anime_comments RLS
DROP POLICY IF EXISTS anime_comments_select_policy ON public.anime_comments;
CREATE POLICY anime_comments_select_policy ON public.anime_comments
    FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS anime_comments_insert_policy ON public.anime_comments;
CREATE POLICY anime_comments_insert_policy ON public.anime_comments
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS anime_comments_update_policy ON public.anime_comments;
CREATE POLICY anime_comments_update_policy ON public.anime_comments
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS anime_comments_delete_policy ON public.anime_comments;
CREATE POLICY anime_comments_delete_policy ON public.anime_comments
    FOR DELETE USING (auth.uid() = user_id);

-- comment_reply_threads RLS
DROP POLICY IF EXISTS comment_reply_threads_select_policy ON public.comment_reply_threads;
CREATE POLICY comment_reply_threads_select_policy ON public.comment_reply_threads
    FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS comment_reply_threads_insert_policy ON public.comment_reply_threads;
CREATE POLICY comment_reply_threads_insert_policy ON public.comment_reply_threads
    FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- comment_replies RLS
DROP POLICY IF EXISTS comment_replies_select_policy ON public.comment_replies;
CREATE POLICY comment_replies_select_policy ON public.comment_replies
    FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS comment_replies_insert_policy ON public.comment_replies;
CREATE POLICY comment_replies_insert_policy ON public.comment_replies
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS comment_replies_update_policy ON public.comment_replies;
CREATE POLICY comment_replies_update_policy ON public.comment_replies
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS comment_replies_delete_policy ON public.comment_replies;
CREATE POLICY comment_replies_delete_policy ON public.comment_replies
    FOR DELETE USING (auth.uid() = user_id);

-- content_mentions RLS
DROP POLICY IF EXISTS content_mentions_select_policy ON public.content_mentions;
CREATE POLICY content_mentions_select_policy ON public.content_mentions
    FOR SELECT USING (auth.uid() IS NOT NULL);

-- user_notifications RLS (strictly recipient-private)
DROP POLICY IF EXISTS user_notifications_select_policy ON public.user_notifications;
CREATE POLICY user_notifications_select_policy ON public.user_notifications
    FOR SELECT USING (auth.uid() = recipient_user_id);

DROP POLICY IF EXISTS user_notifications_update_policy ON public.user_notifications;
CREATE POLICY user_notifications_update_policy ON public.user_notifications
    FOR UPDATE USING (auth.uid() = recipient_user_id) WITH CHECK (auth.uid() = recipient_user_id);

DROP POLICY IF EXISTS user_notifications_delete_policy ON public.user_notifications;
CREATE POLICY user_notifications_delete_policy ON public.user_notifications
    FOR DELETE USING (auth.uid() = recipient_user_id);

-- ============================================================================
-- 7. INTERNAL HELPER: EXTRACT, VALIDATE & STORE @MENTIONS (MAX 5 ENTRIES,
--    ALLOWING DUPLICATES, PRESERVING OCCURRENCE POSITION)
-- ============================================================================
CREATE OR REPLACE FUNCTION public._sync_content_mentions(
    p_content_id UUID,
    p_content_type TEXT,
    p_anime_id INTEGER,
    p_author_id UUID,
    p_content TEXT
)
RETURNS TABLE (
    mentioned_user_id UUID,
    mentioned_username TEXT,
    occurrence_position INTEGER
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_len INTEGER := char_length(COALESCE(p_content, ''));
    v_pos INTEGER := 1;
    v_ch TEXT;
    v_prev TEXT;
    v_end INTEGER;
    v_handle TEXT;
    v_profile_id UUID;
    v_profile_username TEXT;
    v_total_tag_entries INTEGER := 0;
BEGIN
    DELETE FROM public.content_mentions cm
    WHERE cm.content_id = p_content_id
      AND cm.content_type = p_content_type;

    IF v_len = 0 THEN
        RETURN;
    END IF;

    WHILE v_pos <= v_len LOOP
        v_ch := substr(p_content, v_pos, 1);
        IF v_ch = '@' THEN
            v_prev := CASE WHEN v_pos > 1 THEN substr(p_content, v_pos - 1, 1) ELSE '' END;
            -- Only treat '@' as mention trigger when at start or preceded by non-word character
            IF v_pos = 1 OR v_prev !~ '[A-Za-z0-9_]' THEN
                v_end := v_pos + 1;
                WHILE v_end <= v_len AND substr(p_content, v_end, 1) ~ '[A-Za-z0-9_.-]' LOOP
                    v_end := v_end + 1;
                END LOOP;

                v_handle := trim(BOTH '.' FROM substr(p_content, v_pos + 1, v_end - v_pos - 1));
                IF char_length(v_handle) BETWEEN 2 AND 32 THEN
                    v_total_tag_entries := v_total_tag_entries + 1;
                    IF v_total_tag_entries > 5 THEN
                        RAISE EXCEPTION 'Maximum 5 @mentions allowed per comment or reply.';
                    END IF;

                    SELECT p.id, p.username
                    INTO v_profile_id, v_profile_username
                    FROM public.profiles p
                    WHERE LOWER(p.username) = LOWER(v_handle)
                    LIMIT 1;

                    -- Store valid registered mentions; skip self-mentions
                    IF v_profile_id IS NOT NULL AND v_profile_id <> p_author_id THEN
                        INSERT INTO public.content_mentions (
                            content_id,
                            content_type,
                            anime_id,
                            mentioned_user_id,
                            mentioned_username,
                            occurrence_position
                        ) VALUES (
                            p_content_id,
                            p_content_type,
                            p_anime_id,
                            v_profile_id,
                            v_profile_username,
                            v_pos - 1
                        );

                        mentioned_user_id := v_profile_id;
                        mentioned_username := v_profile_username;
                        occurrence_position := v_pos - 1;
                        RETURN NEXT;
                    END IF;
                END IF;
                v_pos := v_end;
                CONTINUE;
            END IF;
        END IF;
        v_pos := v_pos + 1;
    END LOOP;
END;
$$;

REVOKE ALL ON FUNCTION public._sync_content_mentions(UUID, TEXT, INTEGER, UUID, TEXT) FROM PUBLIC;

-- ============================================================================
-- 8. RPC: USERNAME AUTOCOMPLETE FOR @MENTIONS (EXCLUDES SELF)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.search_mentionable_users(
    p_query TEXT DEFAULT '',
    p_limit INTEGER DEFAULT 8
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_clean TEXT := trim(COALESCE(p_query, ''));
    v_limit INTEGER := LEAST(GREATEST(COALESCE(p_limit, 8), 1), 20);
    v_result JSONB;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    SELECT COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'id', sub.id,
                'username', sub.username,
                'avatar_key', COALESCE(NULLIF(sub.avatar_key, ''), 'avatar_1'),
                'avatar_url', sub.avatar_url
            )
        ),
        '[]'::jsonb
    )
    INTO v_result
    FROM (
        SELECT p.id, p.username, p.avatar_key, p.avatar_url
        FROM public.profiles p
        WHERE p.id <> v_uid
          AND p.username IS NOT NULL
          AND char_length(trim(p.username)) >= 2
          AND p.username NOT LIKE '%@%'
          AND (
              v_clean = ''
              OR LOWER(p.username) LIKE LOWER(v_clean) || '%'
              OR LOWER(p.username) LIKE '%' || LOWER(v_clean) || '%'
          )
        ORDER BY
            CASE WHEN v_clean <> '' AND LOWER(p.username) LIKE LOWER(v_clean) || '%' THEN 0 ELSE 1 END,
            LOWER(p.username) ASC
        LIMIT v_limit
    ) sub;

    RETURN v_result;
END;
$$;

GRANT EXECUTE ON FUNCTION public.search_mentionable_users(TEXT, INTEGER) TO authenticated;

-- ============================================================================
-- 9. RPC: CREATE ANIME COMMENT (ATOMIC: ENSURES ANIME STUB + COMMENT + MENTIONS
--    + DEDUPLICATED MENTION NOTIFICATIONS)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.create_anime_comment(
    p_anime_id INTEGER,
    p_content TEXT,
    p_anime_title TEXT DEFAULT NULL,
    p_anime_cover TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_clean TEXT := trim(COALESCE(p_content, ''));
    v_comment_id UUID;
    v_created_at TIMESTAMPTZ;
    v_updated_at TIMESTAMPTZ;
    v_username TEXT;
    v_avatar_key TEXT;
    v_avatar_url TEXT;
    v_preview TEXT;
    v_total_comments INTEGER;
    v_mentions_json JSONB;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in to post a comment.';
    END IF;

    IF p_anime_id IS NULL OR p_anime_id <= 0 THEN
        RAISE EXCEPTION 'Invalid anime ID.';
    END IF;

    IF char_length(v_clean) < 1 THEN
        RAISE EXCEPTION 'Comment cannot be empty.';
    END IF;

    IF char_length(v_clean) > 2000 THEN
        RAISE EXCEPTION 'Comment cannot exceed 2000 characters.';
    END IF;

    -- Ensure caller has a profile
    SELECT p.username, COALESCE(NULLIF(p.avatar_key, ''), 'avatar_1'), p.avatar_url
    INTO v_username, v_avatar_key, v_avatar_url
    FROM public.profiles p
    WHERE p.id = v_uid;

    IF v_username IS NULL THEN
        RAISE EXCEPTION 'Complete your profile before commenting.';
    END IF;

    -- Ensure shared anime catalog row exists so commenting works even on untracked Explore anime
    INSERT INTO public.anime (anilist_id, title_romaji, title_english, cover_image)
    VALUES (
        p_anime_id,
        COALESCE(NULLIF(trim(p_anime_title), ''), 'Anime #' || p_anime_id::TEXT),
        NULLIF(trim(p_anime_title), ''),
        NULLIF(trim(p_anime_cover), '')
    )
    ON CONFLICT (anilist_id) DO UPDATE SET
        title_english = COALESCE(public.anime.title_english, EXCLUDED.title_english),
        cover_image = COALESCE(public.anime.cover_image, EXCLUDED.cover_image);

    INSERT INTO public.anime_comments (anime_id, user_id, content)
    VALUES (p_anime_id, v_uid, v_clean)
    RETURNING id, created_at, updated_at
    INTO v_comment_id, v_created_at, v_updated_at;

    v_preview := left(v_clean, 160);

    -- Extract, validate (max 5 entries, duplicates allowed), store mentions & generate deduplicated notifications
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
        'mention_comment',
        p_anime_id,
        v_comment_id,
        NULL,
        NULL,
        v_preview
    FROM public._sync_content_mentions(v_comment_id, 'comment', p_anime_id, v_uid, v_clean) m
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
    WHERE cm.content_id = v_comment_id
      AND cm.content_type = 'comment';

    SELECT COUNT(*)::INTEGER
    INTO v_total_comments
    FROM public.anime_comments
    WHERE anime_id = p_anime_id;

    RETURN jsonb_build_object(
        'comment', jsonb_build_object(
            'id', v_comment_id,
            'anime_id', p_anime_id,
            'user_id', v_uid,
            'username', v_username,
            'avatar_key', v_avatar_key,
            'avatar_url', v_avatar_url,
            'content', v_clean,
            'created_at', v_created_at,
            'updated_at', v_updated_at,
            'mentions', v_mentions_json,
            'threads', '[]'::jsonb
        ),
        'total_comments', v_total_comments
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_anime_comment(INTEGER, TEXT, TEXT, TEXT) TO authenticated;

-- ============================================================================
-- 10. RPC: UPDATE & DELETE ANIME COMMENT (STRICT OWNER CHECK)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.update_anime_comment(
    p_comment_id UUID,
    p_content TEXT
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
    v_updated_at TIMESTAMPTZ;
    v_mentions_json JSONB;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;
    IF char_length(v_clean) < 1 OR char_length(v_clean) > 2000 THEN
        RAISE EXCEPTION 'Comment must be between 1 and 2000 characters.';
    END IF;

    UPDATE public.anime_comments
    SET content = v_clean,
        updated_at = NOW()
    WHERE id = p_comment_id
      AND user_id = v_uid
    RETURNING anime_id, updated_at
    INTO v_anime_id, v_updated_at;

    IF v_anime_id IS NULL THEN
        RAISE EXCEPTION 'Comment not found or you do not have permission to edit it.';
    END IF;

    -- Re-validate & re-store mentions; notify any newly mentioned users without duplicating existing ones
    INSERT INTO public.user_notifications (
        recipient_user_id,
        sender_user_id,
        notification_type,
        anime_id,
        comment_id,
        preview_text
    )
    SELECT DISTINCT
        m.mentioned_user_id,
        v_uid,
        'mention_comment',
        v_anime_id,
        p_comment_id,
        left(v_clean, 160)
    FROM public._sync_content_mentions(p_comment_id, 'comment', v_anime_id, v_uid, v_clean) m
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
    WHERE cm.content_id = p_comment_id
      AND cm.content_type = 'comment';

    RETURN jsonb_build_object(
        'id', p_comment_id,
        'content', v_clean,
        'updated_at', v_updated_at,
        'mentions', v_mentions_json
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.update_anime_comment(UUID, TEXT) TO authenticated;

CREATE OR REPLACE FUNCTION public.delete_anime_comment(
    p_comment_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_anime_id INTEGER;
    v_total_comments INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    SELECT anime_id INTO v_anime_id
    FROM public.anime_comments
    WHERE id = p_comment_id
      AND user_id = v_uid;

    IF v_anime_id IS NULL THEN
        RAISE EXCEPTION 'Comment not found or you do not have permission to delete it.';
    END IF;

    -- Clean mentions belonging to replies under this comment & to the comment itself
    DELETE FROM public.content_mentions
    WHERE (content_type = 'comment' AND content_id = p_comment_id)
       OR (
           content_type = 'reply'
           AND content_id IN (
               SELECT cr.id
               FROM public.comment_replies cr
               JOIN public.comment_reply_threads t ON t.id = cr.thread_id
               WHERE t.comment_id = p_comment_id
           )
       );

    DELETE FROM public.anime_comments
    WHERE id = p_comment_id
      AND user_id = v_uid;

    SELECT COUNT(*)::INTEGER
    INTO v_total_comments
    FROM public.anime_comments
    WHERE anime_id = v_anime_id;

    RETURN jsonb_build_object(
        'deleted', TRUE,
        'comment_id', p_comment_id,
        'total_comments', v_total_comments
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.delete_anime_comment(UUID) TO authenticated;

-- ============================================================================
-- 11. RPC: CREATE THREAD REPLY (CONCURRENCY-SAFE TARGET-USER THREAD CREATION/
--     CONTINUATION + MAX 5 MENTIONS + AUTOMATIC THREAD & MENTION NOTIFICATIONS)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.create_thread_reply(
    p_comment_id UUID,
    p_target_user_id UUID,
    p_content TEXT
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
    v_thread_id UUID;
    v_thread_created_at TIMESTAMPTZ;
    v_reply_id UUID;
    v_reply_created_at TIMESTAMPTZ;
    v_reply_updated_at TIMESTAMPTZ;
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

    SELECT anime_id INTO v_anime_id
    FROM public.anime_comments
    WHERE id = p_comment_id;

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

    -- Concurrency-safe atomic thread create-or-continue for (comment_id, target_user_id)
    INSERT INTO public.comment_reply_threads (comment_id, target_user_id)
    VALUES (p_comment_id, p_target_user_id)
    ON CONFLICT (comment_id, target_user_id) DO NOTHING;

    SELECT id, created_at
    INTO v_thread_id, v_thread_created_at
    FROM public.comment_reply_threads
    WHERE comment_id = p_comment_id
      AND target_user_id = p_target_user_id;

    INSERT INTO public.comment_replies (thread_id, user_id, content)
    VALUES (v_thread_id, v_uid, v_clean)
    RETURNING id, created_at, updated_at
    INTO v_reply_id, v_reply_created_at, v_reply_updated_at;

    v_preview := left(v_clean, 160);

    -- 1. Automatically notify thread target user (even if no @mention was typed), skipping self
    IF p_target_user_id <> v_uid THEN
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

    -- 2. Validate & store up to 5 mentions (duplicates allowed) + notify mentioned users without duplicating target notification
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
    FROM public._sync_content_mentions(v_reply_id, 'reply', v_anime_id, v_uid, v_clean) m
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
            'created_at', v_reply_created_at,
            'updated_at', v_reply_updated_at,
            'mentions', v_mentions_json
        )
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_thread_reply(UUID, UUID, TEXT) TO authenticated;

-- ============================================================================
-- 12. RPC: UPDATE & DELETE THREAD REPLY (STRICT OWNER CHECK)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.update_thread_reply(
    p_reply_id UUID,
    p_content TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_clean TEXT := trim(COALESCE(p_content, ''));
    v_thread_id UUID;
    v_comment_id UUID;
    v_anime_id INTEGER;
    v_updated_at TIMESTAMPTZ;
    v_mentions_json JSONB;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;
    IF char_length(v_clean) < 1 OR char_length(v_clean) > 2000 THEN
        RAISE EXCEPTION 'Reply must be between 1 and 2000 characters.';
    END IF;

    SELECT cr.thread_id, t.comment_id, c.anime_id
    INTO v_thread_id, v_comment_id, v_anime_id
    FROM public.comment_replies cr
    JOIN public.comment_reply_threads t ON t.id = cr.thread_id
    JOIN public.anime_comments c ON c.id = t.comment_id
    WHERE cr.id = p_reply_id
      AND cr.user_id = v_uid;

    IF v_anime_id IS NULL THEN
        RAISE EXCEPTION 'Reply not found or you do not have permission to edit it.';
    END IF;

    UPDATE public.comment_replies
    SET content = v_clean,
        updated_at = NOW()
    WHERE id = p_reply_id
      AND user_id = v_uid
    RETURNING updated_at INTO v_updated_at;

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
        v_comment_id,
        v_thread_id,
        p_reply_id,
        left(v_clean, 160)
    FROM public._sync_content_mentions(p_reply_id, 'reply', v_anime_id, v_uid, v_clean) m
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
    WHERE cm.content_id = p_reply_id
      AND cm.content_type = 'reply';

    RETURN jsonb_build_object(
        'id', p_reply_id,
        'content', v_clean,
        'updated_at', v_updated_at,
        'mentions', v_mentions_json
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.update_thread_reply(UUID, TEXT) TO authenticated;

CREATE OR REPLACE FUNCTION public.delete_thread_reply(
    p_reply_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_thread_id UUID;
    v_remaining INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    SELECT thread_id INTO v_thread_id
    FROM public.comment_replies
    WHERE id = p_reply_id
      AND user_id = v_uid;

    IF v_thread_id IS NULL THEN
        RAISE EXCEPTION 'Reply not found or you do not have permission to delete it.';
    END IF;

    DELETE FROM public.content_mentions
    WHERE content_type = 'reply'
      AND content_id = p_reply_id;

    DELETE FROM public.comment_replies
    WHERE id = p_reply_id
      AND user_id = v_uid;

    -- Prune empty thread if all replies in it were deleted
    SELECT COUNT(*)::INTEGER INTO v_remaining
    FROM public.comment_replies
    WHERE thread_id = v_thread_id;

    IF v_remaining = 0 THEN
        DELETE FROM public.comment_reply_threads
        WHERE id = v_thread_id;
    END IF;

    RETURN jsonb_build_object(
        'deleted', TRUE,
        'reply_id', p_reply_id,
        'thread_id', v_thread_id,
        'thread_removed', (v_remaining = 0)
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.delete_thread_reply(UUID) TO authenticated;

-- ============================================================================
-- 13. RPC: PAGINATED ANIME DISCUSSION FETCH (COMMENTS + THREADS + REPLIES)
-- ============================================================================
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

-- ============================================================================
-- 14. RPCs: NOTIFICATIONS FEED, UNREAD COUNTER, MARK READ, MARK ALL READ
-- ============================================================================
CREATE OR REPLACE FUNCTION public.get_unread_notification_count()
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_count INTEGER;
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

GRANT EXECUTE ON FUNCTION public.get_unread_notification_count() TO authenticated;

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
    v_limit INTEGER := LEAST(GREATEST(COALESCE(p_limit, 30), 1), 60);
    v_offset INTEGER := GREATEST(COALESCE(p_offset, 0), 0);
    v_unread INTEGER;
    v_total INTEGER;
    v_items JSONB;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    SELECT
        COUNT(*) FILTER (WHERE is_read = FALSE)::INTEGER,
        COUNT(*)::INTEGER
    INTO v_unread, v_total
    FROM public.user_notifications
    WHERE recipient_user_id = v_uid;

    SELECT COALESCE(
        jsonb_agg(item_obj ORDER BY created_at DESC),
        '[]'::jsonb
    )
    INTO v_items
    FROM (
        SELECT
            n.created_at,
            jsonb_build_object(
                'id', n.id,
                'recipient_user_id', n.recipient_user_id,
                'sender_user_id', n.sender_user_id,
                'sender_username', COALESCE(sp.username, 'Member'),
                'sender_avatar_key', COALESCE(NULLIF(sp.avatar_key, ''), 'avatar_1'),
                'sender_avatar_url', sp.avatar_url,
                'notification_type', n.notification_type,
                'anime_id', n.anime_id,
                'anime_title', COALESCE(NULLIF(a.title_english, ''), NULLIF(a.title_romaji, ''), 'Anime #' || n.anime_id::TEXT),
                'anime_cover_image', COALESCE(a.cover_image, ''),
                'comment_id', n.comment_id,
                'thread_id', n.thread_id,
                'reply_id', n.reply_id,
                'preview_text', n.preview_text,
                'is_read', n.is_read,
                'created_at', n.created_at
            ) AS item_obj
        FROM public.user_notifications n
        LEFT JOIN public.profiles sp ON sp.id = n.sender_user_id
        LEFT JOIN public.anime a ON a.anilist_id = n.anime_id
        WHERE n.recipient_user_id = v_uid
        ORDER BY n.created_at DESC
        LIMIT v_limit OFFSET v_offset
    ) rows_sub;

    RETURN jsonb_build_object(
        'unread_count', COALESCE(v_unread, 0),
        'total_count', COALESCE(v_total, 0),
        'has_more', (v_offset + v_limit) < COALESCE(v_total, 0),
        'notifications', v_items
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_user_notifications(INTEGER, INTEGER) TO authenticated;

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
    v_unread INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    UPDATE public.user_notifications
    SET is_read = TRUE
    WHERE id = p_notification_id
      AND recipient_user_id = v_uid;

    SELECT COUNT(*)::INTEGER
    INTO v_unread
    FROM public.user_notifications
    WHERE recipient_user_id = v_uid
      AND is_read = FALSE;

    RETURN jsonb_build_object(
        'updated', TRUE,
        'unread_count', COALESCE(v_unread, 0)
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.mark_notification_read(UUID) TO authenticated;

CREATE OR REPLACE FUNCTION public.mark_all_notifications_read()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    UPDATE public.user_notifications
    SET is_read = TRUE
    WHERE recipient_user_id = v_uid
      AND is_read = FALSE;

    RETURN jsonb_build_object(
        'updated', TRUE,
        'unread_count', 0
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.mark_all_notifications_read() TO authenticated;

-- ============================================================================
-- 15. EXTEND delete_my_account() SAFELY TO CLEAN NEW DISCUSSION TABLES
-- ============================================================================
CREATE OR REPLACE FUNCTION public.delete_my_account()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, storage
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_email TEXT;
    v_username TEXT;
    v_auth_deleted BOOLEAN := FALSE;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    SELECT LOWER(TRIM(u.email)) INTO v_email FROM auth.users u WHERE u.id = v_uid;
    SELECT TRIM(p.username) INTO v_username FROM public.profiles p WHERE p.id = v_uid;

    IF v_email IS NOT NULL AND v_email <> '' AND to_regclass('public.deleted_account_cooldowns') IS NOT NULL THEN
        INSERT INTO public.deleted_account_cooldowns (email_lower, deleted_at, reuse_allowed_at)
        VALUES (v_email, NOW(), NOW() + INTERVAL '5 minutes')
        ON CONFLICT (email_lower) DO UPDATE SET
            deleted_at = NOW(),
            reuse_allowed_at = NOW() + INTERVAL '5 minutes';
    END IF;

    IF v_username IS NOT NULL AND v_username <> '' AND to_regclass('public.deleted_username_cooldowns') IS NOT NULL THEN
        INSERT INTO public.deleted_username_cooldowns (username_lower, deleted_at, reuse_allowed_at)
        VALUES (LOWER(v_username), NOW(), NOW() + INTERVAL '24 hours')
        ON CONFLICT (username_lower) DO UPDATE SET
            deleted_at = NOW(),
            reuse_allowed_at = NOW() + INTERVAL '24 hours';
    END IF;

    BEGIN
        DELETE FROM storage.objects
        WHERE bucket_id = 'avatars'
          AND (name LIKE v_uid::TEXT || '/%' OR owner = v_uid);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;

    IF to_regclass('public.user_notifications') IS NOT NULL THEN
        DELETE FROM public.user_notifications WHERE recipient_user_id = v_uid OR sender_user_id = v_uid;
    END IF;
    IF to_regclass('public.content_mentions') IS NOT NULL THEN
        DELETE FROM public.content_mentions WHERE mentioned_user_id = v_uid;
    END IF;
    IF to_regclass('public.comment_replies') IS NOT NULL THEN
        DELETE FROM public.comment_replies WHERE user_id = v_uid;
    END IF;
    IF to_regclass('public.comment_reply_threads') IS NOT NULL THEN
        DELETE FROM public.comment_reply_threads WHERE target_user_id = v_uid;
    END IF;
    IF to_regclass('public.anime_comments') IS NOT NULL THEN
        DELETE FROM public.anime_comments WHERE user_id = v_uid;
    END IF;

    IF to_regclass('public.user_push_tokens') IS NOT NULL THEN
        DELETE FROM public.user_push_tokens WHERE user_id = v_uid;
    END IF;
    IF to_regclass('public.review_likes') IS NOT NULL THEN
        DELETE FROM public.review_likes WHERE user_id = v_uid;
    END IF;
    IF to_regclass('public.community_notes') IS NOT NULL THEN
        DELETE FROM public.community_notes WHERE user_id = v_uid;
    END IF;
    IF to_regclass('public.home_tracking') IS NOT NULL THEN
        DELETE FROM public.home_tracking WHERE user_id = v_uid;
    END IF;
    IF to_regclass('public.collection_items') IS NOT NULL AND to_regclass('public.collections') IS NOT NULL THEN
        DELETE FROM public.collection_items WHERE collection_id IN (SELECT id FROM public.collections WHERE creator_id = v_uid);
    END IF;
    IF to_regclass('public.collections') IS NOT NULL THEN
        DELETE FROM public.collections WHERE creator_id = v_uid;
    END IF;
    IF to_regclass('public.user_anime') IS NOT NULL THEN
        DELETE FROM public.user_anime WHERE user_id = v_uid;
    END IF;
    IF to_regclass('public.privacy_settings') IS NOT NULL THEN
        DELETE FROM public.privacy_settings WHERE user_id = v_uid;
    END IF;
    IF to_regclass('public.invitations') IS NOT NULL THEN
        DELETE FROM public.invitations WHERE created_by = v_uid;
    END IF;
    IF to_regclass('public.community_members') IS NOT NULL THEN
        DELETE FROM public.community_members WHERE user_id = v_uid;
    END IF;
    DELETE FROM public.profiles WHERE id = v_uid;

    BEGIN
        DELETE FROM auth.users WHERE id = v_uid;
        v_auth_deleted := TRUE;
    EXCEPTION WHEN OTHERS THEN
        v_auth_deleted := FALSE;
    END;

    RETURN jsonb_build_object(
        'deleted', TRUE,
        'auth_deleted', v_auth_deleted
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.delete_my_account() TO authenticated;
