-- ============================================================================
-- ANIME UNIVERSE — Migration: Flat Live Chat, Inline Quotes & True Realtime
-- Date: 2026-11-24
-- Safe, idempotent, non-destructive forward migration.
-- ============================================================================

-- 1. ADD FLAT reply_to JSONB COLUMN ON ANIME_COMMENTS
ALTER TABLE public.anime_comments
    ADD COLUMN IF NOT EXISTS reply_to JSONB;

CREATE INDEX IF NOT EXISTS idx_anime_comments_anime_created_asc
    ON public.anime_comments (anime_id, created_at ASC);

-- 2. ENABLE SUPABASE REALTIME FOR ANIME_COMMENTS, COMMENT_REPLIES & USER_NOTIFICATIONS
ALTER TABLE public.anime_comments REPLICA IDENTITY FULL;
ALTER TABLE public.comment_replies REPLICA IDENTITY FULL;
ALTER TABLE public.user_notifications REPLICA IDENTITY FULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        BEGIN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.anime_comments;
        EXCEPTION WHEN duplicate_object THEN
            NULL;
        END;
        BEGIN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.comment_replies;
        EXCEPTION WHEN duplicate_object THEN
            NULL;
        END;
        BEGIN
            ALTER PUBLICATION supabase_realtime ADD TABLE public.user_notifications;
        EXCEPTION WHEN duplicate_object THEN
            NULL;
        END;
    END IF;
END $$;

-- 3. UPGRADE RPC: create_anime_comment (SUPPORTS FLAT reply_to QUOTE + REPLY NOTIFICATION)
DROP FUNCTION IF EXISTS public.create_anime_comment(INTEGER, TEXT, TEXT, TEXT, UUID[]);
DROP FUNCTION IF EXISTS public.create_anime_comment(INTEGER, TEXT, TEXT, TEXT, UUID[], JSONB);

CREATE OR REPLACE FUNCTION public.create_anime_comment(
    p_anime_id INTEGER,
    p_content TEXT,
    p_anime_title TEXT DEFAULT NULL,
    p_anime_cover TEXT DEFAULT NULL,
    p_tagged_user_ids UUID[] DEFAULT NULL,
    p_reply_to JSONB DEFAULT NULL
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
    v_reply_to_clean JSONB := NULL;
    v_ref_message_id UUID := NULL;
    v_ref_author_id UUID := NULL;
    v_ref_sender_name TEXT := NULL;
    v_ref_content TEXT := NULL;
    v_ref_sticker TEXT := NULL;
    v_ref_preview TEXT := NULL;
    v_sticker_match TEXT[];
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in to post a message.';
    END IF;

    IF p_anime_id IS NULL OR p_anime_id <= 0 THEN
        RAISE EXCEPTION 'Invalid anime ID.';
    END IF;

    IF char_length(v_clean) < 1 THEN
        RAISE EXCEPTION 'Message cannot be empty.';
    END IF;

    IF char_length(v_clean) > 2000 THEN
        RAISE EXCEPTION 'Message cannot exceed 2000 characters.';
    END IF;

    SELECT p.username, COALESCE(NULLIF(p.avatar_key, ''), 'avatar_1'), p.avatar_url
    INTO v_username, v_avatar_key, v_avatar_url
    FROM public.profiles p
    WHERE p.id = v_uid;

    IF v_username IS NULL THEN
        RAISE EXCEPTION 'Complete your profile before chatting.';
    END IF;

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

    -- Resolve optional flat reply_to quote object ({ messageId, senderName, previewText, stickerCode })
    IF p_reply_to IS NOT NULL AND jsonb_typeof(p_reply_to) = 'object' THEN
        BEGIN
            v_ref_message_id := NULLIF(trim(COALESCE(p_reply_to->>'messageId', '')), '')::UUID;
        EXCEPTION WHEN OTHERS THEN
            v_ref_message_id := NULL;
        END;

        IF v_ref_message_id IS NOT NULL THEN
            -- Look up target message in anime_comments first
            SELECT c.user_id, COALESCE(cp.username, 'Member'), c.content
            INTO v_ref_author_id, v_ref_sender_name, v_ref_content
            FROM public.anime_comments c
            LEFT JOIN public.profiles cp ON cp.id = c.user_id
            WHERE c.id = v_ref_message_id
              AND c.anime_id = p_anime_id
            LIMIT 1;

            -- Fallback lookup in legacy comment_replies
            IF v_ref_author_id IS NULL THEN
                SELECT cr.user_id, COALESCE(rp.username, 'Member'), cr.content
                INTO v_ref_author_id, v_ref_sender_name, v_ref_content
                FROM public.comment_replies cr
                JOIN public.comment_reply_threads t ON t.id = cr.thread_id
                JOIN public.anime_comments pc ON pc.id = t.comment_id
                LEFT JOIN public.profiles rp ON rp.id = cr.user_id
                WHERE cr.id = v_ref_message_id
                  AND pc.anime_id = p_anime_id
                LIMIT 1;
            END IF;
        END IF;

        v_ref_sender_name := COALESCE(
            NULLIF(v_ref_sender_name, ''),
            NULLIF(trim(COALESCE(p_reply_to->>'senderName', '')), ''),
            'Member'
        );

        v_ref_preview := COALESCE(
            NULLIF(trim(COALESCE(p_reply_to->>'previewText', '')), ''),
            NULLIF(trim(COALESCE(v_ref_content, '')), ''),
            ''
        );

        v_ref_sticker := lower(NULLIF(trim(COALESCE(p_reply_to->>'stickerCode', '')), ''));
        IF v_ref_sticker IS NULL AND v_ref_preview <> '' THEN
            v_sticker_match := regexp_match(v_ref_preview, '\[emoji:([a-z0-9][a-z0-9_-]{0,63})\]', 'i');
            IF v_sticker_match IS NULL THEN
                v_sticker_match := regexp_match(v_ref_preview, ':([a-z0-9][a-z0-9_-]{0,63}):', 'i');
            END IF;
            IF v_sticker_match IS NOT NULL THEN
                v_ref_sticker := lower(v_sticker_match[1]);
            END IF;
        END IF;

        IF v_ref_message_id IS NOT NULL OR v_ref_preview <> '' OR v_ref_sticker IS NOT NULL THEN
            v_reply_to_clean := jsonb_build_object(
                'messageId', COALESCE(v_ref_message_id::TEXT, ''),
                'senderName', v_ref_sender_name,
                'previewText', left(v_ref_preview, 180),
                'stickerCode', v_ref_sticker
            );
        END IF;
    END IF;

    INSERT INTO public.anime_comments (anime_id, user_id, content, reply_to)
    VALUES (p_anime_id, v_uid, v_clean, v_reply_to_clean)
    RETURNING id, created_at, updated_at
    INTO v_comment_id, v_created_at, v_updated_at;

    v_preview := left(v_clean, 160);

    -- Notify exact replied-to message author (never self)
    IF v_ref_author_id IS NOT NULL AND v_ref_author_id <> v_uid THEN
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
            v_ref_author_id,
            v_uid,
            'thread_reply',
            p_anime_id,
            v_comment_id,
            NULL::UUID,
            NULL::UUID,
            v_preview
        )
        ON CONFLICT DO NOTHING;
    END IF;

    -- Extract, validate & store mentions + notify mentioned users (deduplicated)
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
        NULL::UUID,
        NULL::UUID,
        v_preview
    FROM public._sync_content_mentions(v_comment_id, 'comment', p_anime_id, v_uid, v_clean, p_tagged_user_ids) m
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

    SELECT (
        (SELECT COUNT(*) FROM public.anime_comments WHERE anime_id = p_anime_id) +
        (SELECT COUNT(*)
         FROM public.comment_replies cr
         JOIN public.comment_reply_threads t ON t.id = cr.thread_id
         JOIN public.anime_comments pc ON pc.id = t.comment_id
         WHERE pc.anime_id = p_anime_id)
    )::INTEGER
    INTO v_total_comments;

    RETURN jsonb_build_object(
        'comment', jsonb_build_object(
            'id', v_comment_id,
            'anime_id', p_anime_id,
            'user_id', v_uid,
            'username', v_username,
            'avatar_key', v_avatar_key,
            'avatar_url', v_avatar_url,
            'content', v_clean,
            'reply_to', v_reply_to_clean,
            'source_type', 'comment',
            'created_at', v_created_at,
            'updated_at', v_updated_at,
            'mentions', v_mentions_json,
            'threads', '[]'::jsonb
        ),
        'total_comments', v_total_comments
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_anime_comment(INTEGER, TEXT, TEXT, TEXT, UUID[], JSONB) TO authenticated;

-- 4. UPGRADE RPC: get_anime_discussion_page (SINGLE FLAT CHRONOLOGICAL STREAM)
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
        RAISE EXCEPTION 'Sign in to view live chat.';
    END IF;

    SELECT (
        (SELECT COUNT(*) FROM public.anime_comments WHERE anime_id = p_anime_id) +
        (SELECT COUNT(*)
         FROM public.comment_replies cr
         JOIN public.comment_reply_threads t ON t.id = cr.thread_id
         JOIN public.anime_comments pc ON pc.id = t.comment_id
         WHERE pc.anime_id = p_anime_id)
    )::INTEGER
    INTO v_total;

    SELECT COALESCE(
        jsonb_agg(msg_obj ORDER BY created_at DESC),
        '[]'::jsonb
    )
    INTO v_comments
    FROM (
        SELECT * FROM (
            -- Flat top-level / new flat reply messages in anime_comments
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
                    'reply_to', c.reply_to,
                    'source_type', 'comment',
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
                    'threads', '[]'::jsonb
                ) AS msg_obj
            FROM public.anime_comments c
            LEFT JOIN public.profiles p ON p.id = c.user_id
            WHERE c.anime_id = p_anime_id

            UNION ALL

            -- Legacy comment_replies flattened into the same chronological stream
            SELECT
                r.created_at,
                jsonb_build_object(
                    'id', r.id,
                    'anime_id', pc.anime_id,
                    'user_id', r.user_id,
                    'username', COALESCE(rp.username, 'Member'),
                    'avatar_key', COALESCE(NULLIF(rp.avatar_key, ''), 'avatar_1'),
                    'avatar_url', rp.avatar_url,
                    'content', r.content,
                    'reply_to', jsonb_build_object(
                        'messageId', COALESCE(
                            r.quoted_reply_id,
                            CASE
                                WHEN r.parent_comment_id IS NOT NULL AND r.parent_comment_id <> t.comment_id THEN r.parent_comment_id
                                ELSE COALESCE(r.quoted_comment_id, t.comment_id)
                            END
                        )::TEXT,
                        'senderName', CASE
                            WHEN COALESCE(r.quoted_reply_id, CASE WHEN r.parent_comment_id IS NOT NULL AND r.parent_comment_id <> t.comment_id THEN r.parent_comment_id ELSE NULL END) IS NOT NULL THEN
                                COALESCE(NULLIF(r.quoted_username, ''), qrp.username, 'Member')
                            ELSE
                                COALESCE(NULLIF(r.quoted_username, ''), pcp.username, 'Member')
                        END,
                        'previewText', CASE
                            WHEN COALESCE(r.quoted_reply_id, CASE WHEN r.parent_comment_id IS NOT NULL AND r.parent_comment_id <> t.comment_id THEN r.parent_comment_id ELSE NULL END) IS NOT NULL THEN
                                COALESCE(NULLIF(r.quoted_preview, ''), left(trim(qr.content), 180), '')
                            ELSE
                                COALESCE(NULLIF(r.quoted_preview, ''), left(trim(pc.content), 180), '')
                        END,
                        'stickerCode', NULL
                    ),
                    'source_type', 'reply',
                    'thread_id', r.thread_id,
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
                    ), '[]'::jsonb),
                    'threads', '[]'::jsonb
                ) AS msg_obj
            FROM public.comment_replies r
            JOIN public.comment_reply_threads t ON t.id = r.thread_id
            JOIN public.anime_comments pc ON pc.id = t.comment_id
            LEFT JOIN public.profiles pcp ON pcp.id = pc.user_id
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
            WHERE pc.anime_id = p_anime_id
        ) flat_feed
        ORDER BY created_at DESC
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
