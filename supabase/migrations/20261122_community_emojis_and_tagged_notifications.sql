-- ============================================================================
-- LIVTRAK / ANIME UNIVERSE — Migration:
--   1) Community Custom Emojis (Supabase Storage Metadata + Dynamic Top Usage)
--   2) Enhanced "Tagged You" Community Notifications (Deduplicated + RLS)
-- Date: 2026-11-22
-- Safe, idempotent, non-destructive forward migration.
-- Preserves all existing auth, profiles, anime, comments, threads & RLS.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- PART 1: CUSTOM EMOJIS METADATA & STORAGE AUTO-SYNC
-- Never stores image binaries/base64 in DB — only lightweight IDs & paths.
-- Supports both "community_emojis" and "community-emojis" public buckets.
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.community_emojis (
    id TEXT PRIMARY KEY CHECK (id ~ '^[a-z0-9][a-z0-9_-]{0,63}$'),
    bucket_name TEXT NOT NULL DEFAULT 'community_emojis'
        CHECK (bucket_name IN ('community_emojis', 'community-emojis')),
    storage_path TEXT NOT NULL UNIQUE,
    file_name TEXT NOT NULL,
    mime_type TEXT NOT NULL DEFAULT 'image/gif',
    usage_count BIGINT NOT NULL DEFAULT 0 CHECK (usage_count >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_community_emojis_top_usage
    ON public.community_emojis (is_active, usage_count DESC, id ASC);

ALTER TABLE public.community_emojis ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS community_emojis_select_policy ON public.community_emojis;
CREATE POLICY community_emojis_select_policy ON public.community_emojis
    FOR SELECT USING (is_active = TRUE);

-- Ensure public read access on Supabase Storage for both bucket name variants
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'storage'
          AND tablename = 'objects'
          AND policyname = 'community_emojis_public_read'
    ) THEN
        CREATE POLICY community_emojis_public_read
            ON storage.objects
            FOR SELECT
            USING (bucket_id IN ('community_emojis', 'community-emojis'));
    END IF;
EXCEPTION WHEN insufficient_privilege THEN
    RAISE NOTICE 'Skipping storage.objects policy creation (managed by Supabase Dashboard bucket public toggle).';
END $$;

-- Helper: derive clean deterministic slug ID from a storage object path
CREATE OR REPLACE FUNCTION public._emoji_slug_from_path(p_path TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    v_file TEXT;
    v_base TEXT;
    v_slug TEXT;
BEGIN
    v_file := regexp_replace(COALESCE(p_path, ''), '^.*/', '');
    v_base := regexp_replace(v_file, '\.[^.]+$', '');
    v_slug := lower(regexp_replace(v_base, '[^a-zA-Z0-9]+', '_', 'g'));
    v_slug := trim(BOTH '_' FROM v_slug);
    IF v_slug IS NULL OR v_slug = '' THEN
        v_slug := 'emoji_' || substr(md5(COALESCE(p_path, '')), 1, 8);
    END IF;
    IF left(v_slug, 1) !~ '[a-z0-9]' THEN
        v_slug := 'e_' || v_slug;
    END IF;
    RETURN left(v_slug, 64);
END;
$$;

-- Trigger function: automatically sync storage.objects uploads/deletes into public.community_emojis
CREATE OR REPLACE FUNCTION public._sync_storage_emoji_object()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, storage
AS $$
DECLARE
    v_slug TEXT;
    v_ext TEXT;
    v_mime TEXT;
    v_file TEXT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.bucket_id IN ('community_emojis', 'community-emojis') THEN
            DELETE FROM public.community_emojis
            WHERE bucket_name = OLD.bucket_id
              AND storage_path = OLD.name;
        END IF;
        RETURN OLD;
    END IF;

    IF NEW.bucket_id NOT IN ('community_emojis', 'community-emojis') THEN
        RETURN NEW;
    END IF;

    -- Ignore folder placeholders or hidden files
    IF NEW.name IS NULL OR NEW.name = '' OR right(NEW.name, 1) = '/' OR NEW.name LIKE '.%' OR NEW.name LIKE '%/.%' THEN
        RETURN NEW;
    END IF;

    v_ext := lower(COALESCE(substring(NEW.name FROM '\.([^.]+)$'), ''));
    IF v_ext NOT IN ('gif', 'webp', 'png', 'jpg', 'jpeg', 'apng') THEN
        RETURN NEW;
    END IF;

    v_mime := CASE v_ext
        WHEN 'gif' THEN 'image/gif'
        WHEN 'webp' THEN 'image/webp'
        WHEN 'png' THEN 'image/png'
        WHEN 'apng' THEN 'image/apng'
        ELSE 'image/jpeg'
    END;
    v_file := regexp_replace(NEW.name, '^.*/', '');
    v_slug := public._emoji_slug_from_path(NEW.name);

    -- If another file already took the same base slug, append short deterministic hash suffix
    IF EXISTS (
        SELECT 1 FROM public.community_emojis ce
        WHERE ce.id = v_slug
          AND ce.storage_path <> NEW.name
    ) THEN
        v_slug := left(v_slug, 55) || '_' || substr(md5(NEW.name), 1, 8);
    END IF;

    INSERT INTO public.community_emojis (
        id,
        bucket_name,
        storage_path,
        file_name,
        mime_type,
        is_active,
        updated_at
    ) VALUES (
        v_slug,
        NEW.bucket_id,
        NEW.name,
        v_file,
        v_mime,
        TRUE,
        NOW()
    )
    ON CONFLICT (storage_path) DO UPDATE SET
        bucket_name = EXCLUDED.bucket_name,
        file_name = EXCLUDED.file_name,
        mime_type = EXCLUDED.mime_type,
        is_active = TRUE,
        updated_at = NOW();

    RETURN NEW;
END;
$$;

DO $$
BEGIN
    DROP TRIGGER IF EXISTS trg_sync_community_emojis ON storage.objects;
    CREATE TRIGGER trg_sync_community_emojis
        AFTER INSERT OR UPDATE OR DELETE ON storage.objects
        FOR EACH ROW
        EXECUTE FUNCTION public._sync_storage_emoji_object();
EXCEPTION WHEN insufficient_privilege THEN
    RAISE NOTICE 'Skipping storage.objects trigger creation (will rely on backfill & sync RPC).';
END $$;

-- Idempotent backfill: register all images already uploaded in community_emojis / community-emojis
DO $$
DECLARE
    rec RECORD;
    v_slug TEXT;
    v_ext TEXT;
    v_mime TEXT;
    v_file TEXT;
BEGIN
    FOR rec IN
        SELECT o.bucket_id, o.name
        FROM storage.objects o
        WHERE o.bucket_id IN ('community_emojis', 'community-emojis')
          AND o.name IS NOT NULL
          AND right(o.name, 1) <> '/'
          AND o.name NOT LIKE '.%'
          AND o.name NOT LIKE '%/.%'
        ORDER BY o.created_at ASC, o.name ASC
    LOOP
        v_ext := lower(COALESCE(substring(rec.name FROM '\.([^.]+)$'), ''));
        IF v_ext NOT IN ('gif', 'webp', 'png', 'jpg', 'jpeg', 'apng') THEN
            CONTINUE;
        END IF;
        v_mime := CASE v_ext
            WHEN 'gif' THEN 'image/gif'
            WHEN 'webp' THEN 'image/webp'
            WHEN 'png' THEN 'image/png'
            WHEN 'apng' THEN 'image/apng'
            ELSE 'image/jpeg'
        END;
        v_file := regexp_replace(rec.name, '^.*/', '');
        v_slug := public._emoji_slug_from_path(rec.name);
        IF EXISTS (
            SELECT 1 FROM public.community_emojis ce
            WHERE ce.id = v_slug
              AND ce.storage_path <> rec.name
        ) THEN
            v_slug := left(v_slug, 55) || '_' || substr(md5(rec.name), 1, 8);
        END IF;

        INSERT INTO public.community_emojis (
            id, bucket_name, storage_path, file_name, mime_type, is_active, updated_at
        ) VALUES (
            v_slug, rec.bucket_id, rec.name, v_file, v_mime, TRUE, NOW()
        )
        ON CONFLICT (storage_path) DO UPDATE SET
            bucket_name = EXCLUDED.bucket_name,
            file_name = EXCLUDED.file_name,
            mime_type = EXCLUDED.mime_type,
            is_active = TRUE,
            updated_at = NOW();
    END LOOP;
END $$;

-- RPC: Fetch all active emojis efficiently in one call (ordered by dynamic usage_count DESC)
-- Also auto-syncs any newly uploaded storage objects if the storage trigger couldn't attach.
CREATE OR REPLACE FUNCTION public.get_community_emojis()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, storage
AS $$
DECLARE
    rec RECORD;
    v_slug TEXT;
    v_ext TEXT;
    v_mime TEXT;
    v_file TEXT;
    v_items JSONB;
    v_version TIMESTAMPTZ;
BEGIN
    -- Lightweight sync of any new objects uploaded to the bucket that aren't in community_emojis yet
    BEGIN
        FOR rec IN
            SELECT o.bucket_id, o.name
            FROM storage.objects o
            WHERE o.bucket_id IN ('community_emojis', 'community-emojis')
              AND o.name IS NOT NULL
              AND right(o.name, 1) <> '/'
              AND o.name NOT LIKE '.%'
              AND o.name NOT LIKE '%/.%'
              AND NOT EXISTS (
                  SELECT 1 FROM public.community_emojis ce
                  WHERE ce.storage_path = o.name
                    AND ce.bucket_name = o.bucket_id
              )
            ORDER BY o.created_at ASC
            LIMIT 200
        LOOP
            v_ext := lower(COALESCE(substring(rec.name FROM '\.([^.]+)$'), ''));
            IF v_ext NOT IN ('gif', 'webp', 'png', 'jpg', 'jpeg', 'apng') THEN
                CONTINUE;
            END IF;
            v_mime := CASE v_ext
                WHEN 'gif' THEN 'image/gif'
                WHEN 'webp' THEN 'image/webp'
                WHEN 'png' THEN 'image/png'
                WHEN 'apng' THEN 'image/apng'
                ELSE 'image/jpeg'
            END;
            v_file := regexp_replace(rec.name, '^.*/', '');
            v_slug := public._emoji_slug_from_path(rec.name);
            IF EXISTS (
                SELECT 1 FROM public.community_emojis ce
                WHERE ce.id = v_slug AND ce.storage_path <> rec.name
            ) THEN
                v_slug := left(v_slug, 55) || '_' || substr(md5(rec.name), 1, 8);
            END IF;

            INSERT INTO public.community_emojis (
                id, bucket_name, storage_path, file_name, mime_type, is_active, updated_at
            ) VALUES (
                v_slug, rec.bucket_id, rec.name, v_file, v_mime, TRUE, NOW()
            )
            ON CONFLICT (storage_path) DO NOTHING;
        END LOOP;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;

    SELECT MAX(updated_at) INTO v_version
    FROM public.community_emojis
    WHERE is_active = TRUE;

    SELECT COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'id', ce.id,
                'bucket_name', ce.bucket_name,
                'storage_path', ce.storage_path,
                'file_name', ce.file_name,
                'mime_type', ce.mime_type,
                'usage_count', ce.usage_count,
                'updated_at', ce.updated_at
            )
            ORDER BY ce.usage_count DESC, ce.id ASC
        ),
        '[]'::jsonb
    )
    INTO v_items
    FROM public.community_emojis ce
    WHERE ce.is_active = TRUE;

    RETURN jsonb_build_object(
        'version', COALESCE(v_version, NOW()),
        'emojis', v_items
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_community_emojis() TO anon, authenticated;

-- RPC: Batch increment usage_count for emojis used in a sent message (dynamic Top Emojis)
CREATE OR REPLACE FUNCTION public.increment_emoji_usage(
    p_emoji_ids TEXT[]
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_updated INTEGER := 0;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    IF p_emoji_ids IS NULL OR array_length(p_emoji_ids, 1) IS NULL THEN
        RETURN jsonb_build_object('updated', 0);
    END IF;

    WITH counted AS (
        SELECT lower(trim(eid)) AS emoji_id, LEAST(COUNT(*), 5)::BIGINT AS delta
        FROM unnest(p_emoji_ids) AS eid
        WHERE eid IS NOT NULL AND trim(eid) <> ''
        GROUP BY lower(trim(eid))
        LIMIT 20
    )
    UPDATE public.community_emojis ce
    SET usage_count = ce.usage_count + c.delta,
        updated_at = NOW()
    FROM counted c
    WHERE ce.id = c.emoji_id
      AND ce.is_active = TRUE;

    GET DIAGNOSTICS v_updated = ROW_COUNT;

    RETURN jsonb_build_object('updated', v_updated);
END;
$$;

GRANT EXECUTE ON FUNCTION public.increment_emoji_usage(TEXT[]) TO authenticated;

-- ----------------------------------------------------------------------------
-- PART 2: DEDICATED "TAGGED YOU" COMMUNITY NOTIFICATIONS (REUSING EXISTING
--         public.user_notifications TABLE & RED-DOT SYSTEM)
-- ----------------------------------------------------------------------------

-- Add community_id and community_name columns non-destructively
ALTER TABLE public.user_notifications
    ADD COLUMN IF NOT EXISTS community_id TEXT NOT NULL DEFAULT 'livtrak_community';

ALTER TABLE public.user_notifications
    ADD COLUMN IF NOT EXISTS community_name TEXT NOT NULL DEFAULT 'LivTrAk Community';

-- Performance indexes for fast per-user "Tagged You" feed & red-dot counts
CREATE INDEX IF NOT EXISTS idx_user_notifications_recipient_created
    ON public.user_notifications (recipient_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_notifications_recipient_anime_read
    ON public.user_notifications (recipient_user_id, anime_id, is_read);

-- Guarantee deduplication indexes exist (prevents duplicate notifications per tagging event)
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_notifications_dedup_comment
    ON public.user_notifications (recipient_user_id, comment_id)
    WHERE reply_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_notifications_dedup_reply
    ON public.user_notifications (recipient_user_id, reply_id)
    WHERE reply_id IS NOT NULL;

-- Ensure strict RLS: users can ONLY read, update, or delete their OWN notifications
ALTER TABLE public.user_notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS user_notifications_select_policy ON public.user_notifications;
CREATE POLICY user_notifications_select_policy ON public.user_notifications
    FOR SELECT USING (auth.uid() = recipient_user_id);

DROP POLICY IF EXISTS user_notifications_update_policy ON public.user_notifications;
CREATE POLICY user_notifications_update_policy ON public.user_notifications
    FOR UPDATE USING (auth.uid() = recipient_user_id)
    WITH CHECK (auth.uid() = recipient_user_id);

DROP POLICY IF EXISTS user_notifications_delete_policy ON public.user_notifications;
CREATE POLICY user_notifications_delete_policy ON public.user_notifications
    FOR DELETE USING (auth.uid() = recipient_user_id);

-- Upgrade get_user_notifications(p_limit, p_offset) to return community_id, community_name,
-- anime thumbnail & title (works for both new and older anime templates), ordered latest first.
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
                'community_id', COALESCE(NULLIF(n.community_id, ''), 'livtrak_community'),
                'community_name', COALESCE(NULLIF(n.community_name, ''), 'LivTrAk Community'),
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

-- Upgrade get_unseen_discussion_anime() to also include community_id & community_name
CREATE OR REPLACE FUNCTION public.get_unseen_discussion_anime()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_total INTEGER := 0;
    v_anime JSONB := '[]'::jsonb;
BEGIN
    IF v_uid IS NULL THEN
        RETURN jsonb_build_object('total_unseen', 0, 'anime', '[]'::jsonb);
    END IF;

    SELECT COUNT(*)::INTEGER
    INTO v_total
    FROM public.user_notifications
    WHERE recipient_user_id = v_uid
      AND is_read = FALSE;

    SELECT COALESCE(
        jsonb_agg(entry_obj ORDER BY latest_created_at DESC),
        '[]'::jsonb
    )
    INTO v_anime
    FROM (
        SELECT
            grouped.latest_created_at,
            jsonb_build_object(
                'anime_id', grouped.anime_id,
                'anime_title', COALESCE(NULLIF(a.title_english, ''), NULLIF(a.title_romaji, ''), 'Anime #' || grouped.anime_id::TEXT),
                'anime_cover_image', COALESCE(a.cover_image, ''),
                'community_id', COALESCE(NULLIF(latest.community_id, ''), 'livtrak_community'),
                'community_name', COALESCE(NULLIF(latest.community_name, ''), 'LivTrAk Community'),
                'unseen_count', grouped.unseen_count,
                'latest_notification_id', latest.id,
                'latest_comment_id', latest.comment_id,
                'latest_thread_id', latest.thread_id,
                'latest_reply_id', latest.reply_id,
                'latest_sender_username', COALESCE(sp.username, 'Member'),
                'latest_notification_type', latest.notification_type,
                'latest_preview', latest.preview_text,
                'latest_created_at', grouped.latest_created_at
            ) AS entry_obj
        FROM (
            SELECT
                anime_id,
                COUNT(*)::INTEGER AS unseen_count,
                MAX(created_at) AS latest_created_at
            FROM public.user_notifications
            WHERE recipient_user_id = v_uid
              AND is_read = FALSE
            GROUP BY anime_id
        ) grouped
        LEFT JOIN LATERAL (
            SELECT n.*
            FROM public.user_notifications n
            WHERE n.recipient_user_id = v_uid
              AND n.anime_id = grouped.anime_id
              AND n.is_read = FALSE
            ORDER BY n.created_at DESC
            LIMIT 1
        ) latest ON TRUE
        LEFT JOIN public.profiles sp ON sp.id = latest.sender_user_id
        LEFT JOIN public.anime a ON a.anilist_id = grouped.anime_id
    ) sub;

    RETURN jsonb_build_object(
        'total_unseen', COALESCE(v_total, 0),
        'anime', v_anime
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_unseen_discussion_anime() TO authenticated;
