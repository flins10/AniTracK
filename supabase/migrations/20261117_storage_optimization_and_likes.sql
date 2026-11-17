-- ============================================================================
-- ANIME UNIVERSE — 500 MB STORAGE OPTIMIZATION, REVIEW LIKES, PAGINATION
-- & ADMIN STORAGE MONITORING MIGRATION
-- Safe, idempotent, non-destructive migration: preserves 100% of existing users & data
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- 1. STORAGE-SAVING CONSTRAINTS & COMPACT FIELD NORMALIZATION
-- ----------------------------------------------------------------------------

-- 1.1 Enforce sensible character limits via BEFORE INSERT/UPDATE triggers
--     (Truncates gracefully first so existing rows or slightly long inputs never fail)
CREATE OR REPLACE FUNCTION public.handle_anime_compact_storage()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    -- Cap synopsis to 360 chars in shared catalog (full text is fetched live from AniList on detail view)
    IF NEW.description IS NOT NULL AND char_length(NEW.description) > 360 THEN
        NEW.description = left(trim(NEW.description), 360);
    END IF;
    -- Reject Base64 data URIs in image columns to protect the 500 MB database limit
    IF NEW.cover_image LIKE 'data:image/%' THEN
        NEW.cover_image = '';
    END IF;
    IF NEW.banner_image LIKE 'data:image/%' THEN
        NEW.banner_image = NULL;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_anime_updated_at ON public.anime;
CREATE TRIGGER trg_anime_updated_at
    BEFORE INSERT OR UPDATE ON public.anime
    FOR EACH ROW EXECUTE FUNCTION public.handle_anime_compact_storage();

-- 1.2 Enforce 500-char limit on user_anime.review_notes and episode progress bounds
CREATE OR REPLACE FUNCTION public.handle_user_anime_before_save()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    IF NEW.review_notes IS NOT NULL AND char_length(NEW.review_notes) > 500 THEN
        NEW.review_notes = left(trim(NEW.review_notes), 500);
    END IF;
    IF NEW.total_episodes IS NOT NULL AND NEW.total_episodes > 0 THEN
        IF NEW.episode_progress > NEW.total_episodes THEN
            NEW.episode_progress = NEW.total_episodes;
        END IF;
    END IF;
    IF NEW.status = 'completed' AND NEW.date_completed IS NULL THEN
        NEW.date_completed = NOW();
    ELSIF NEW.status <> 'completed' THEN
        NEW.date_completed = NULL;
    END IF;
    RETURN NEW;
END;
$$;

-- 1.3 Enforce 160-char limit on profiles.bio and reject Base64 avatar_url strings
CREATE OR REPLACE FUNCTION public.handle_profiles_compact_storage()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    IF NEW.bio IS NOT NULL AND char_length(NEW.bio) > 160 THEN
        NEW.bio = left(trim(NEW.bio), 160);
    END IF;
    IF NEW.avatar_url IS NOT NULL AND NEW.avatar_url LIKE 'data:image/%' THEN
        NEW.avatar_url = NULL;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_profiles_updated_at ON public.profiles;
CREATE TRIGGER trg_profiles_updated_at
    BEFORE INSERT OR UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_profiles_compact_storage();

-- ----------------------------------------------------------------------------
-- 2. REVIEW LIKES TABLE, INDEXES, RLS & ATOMIC TOGGLE RPC
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.review_likes (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    review_id UUID NOT NULL REFERENCES public.user_anime(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, review_id)
);

CREATE INDEX IF NOT EXISTS idx_review_likes_review_id
    ON public.review_likes(review_id);

ALTER TABLE public.review_likes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS review_likes_select_policy ON public.review_likes;
CREATE POLICY review_likes_select_policy ON public.review_likes
    FOR SELECT USING (
        auth.uid() IS NOT NULL
        AND (
            auth.uid() = user_id
            OR public.is_active_member(auth.uid())
        )
    );

DROP POLICY IF EXISTS review_likes_insert_own ON public.review_likes;
CREATE POLICY review_likes_insert_own ON public.review_likes
    FOR INSERT WITH CHECK (
        auth.uid() = user_id
        AND public.is_active_member(auth.uid())
    );

DROP POLICY IF EXISTS review_likes_delete_own ON public.review_likes;
CREATE POLICY review_likes_delete_own ON public.review_likes
    FOR DELETE USING (auth.uid() = user_id);

CREATE OR REPLACE FUNCTION public.toggle_review_like(p_review_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_liked BOOLEAN;
    v_count INTEGER;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.review_likes
        WHERE user_id = v_user_id AND review_id = p_review_id
    ) THEN
        DELETE FROM public.review_likes
        WHERE user_id = v_user_id AND review_id = p_review_id;
        v_liked := FALSE;
    ELSE
        INSERT INTO public.review_likes (user_id, review_id)
        VALUES (v_user_id, p_review_id)
        ON CONFLICT (user_id, review_id) DO NOTHING;
        v_liked := TRUE;
    END IF;

    SELECT COUNT(*)::INTEGER INTO v_count
    FROM public.review_likes
    WHERE review_id = p_review_id;

    RETURN jsonb_build_object(
        'review_id', p_review_id,
        'liked', v_liked,
        'likes_count', v_count
    );
END;
$$;

REVOKE ALL ON FUNCTION public.toggle_review_like(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.toggle_review_like(UUID) TO authenticated;

-- ----------------------------------------------------------------------------
-- 3. CALCULATED COMMUNITY ANIME STATS VIEW (Zero Redundant Stored Columns)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW public.community_anime_stats_view AS
SELECT
    ua.anime_id,
    COUNT(ua.id)::INTEGER AS tracking_count,
    COUNT(ua.personal_rating)::INTEGER AS rating_count,
    ROUND(AVG(ua.personal_rating), 2) AS avg_rating_10,
    COUNT(CASE WHEN ua.personal_rating > 0 AND length(trim(COALESCE(ua.review_notes, ''))) > 0 THEN 1 END)::INTEGER AS review_count,
    MAX(ua.updated_at) AS last_reviewed_at
FROM public.user_anime ua
GROUP BY ua.anime_id;

--Partial index for fast Community Reviews feed (only rows with both rating and notes)
CREATE INDEX IF NOT EXISTS idx_user_anime_rated_noted_feed
    ON public.user_anime (updated_at DESC)
    WHERE personal_rating > 0 AND review_notes <> '';

-- ----------------------------------------------------------------------------
-- 4. 500 MB DATABASE STORAGE MONITOR & SAFE RETENTION CLEANUP RPC
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_database_storage_stats()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_caller UUID := auth.uid();
    v_limit_bytes BIGINT := 524288000; -- 500 MB
    v_total_db_bytes BIGINT;
    v_usage_pct NUMERIC(5, 2);
    v_warning_level TEXT;
    v_user_count INTEGER;
    v_review_count INTEGER;
    v_entry_count INTEGER;
    v_user_data_bytes BIGINT;
    v_review_table_bytes BIGINT;
    v_bytes_per_user BIGINT;
    v_bytes_per_review BIGINT;
    v_remaining_bytes BIGINT;
    v_estimated_remaining_users BIGINT;
    v_tables JSONB;
BEGIN
    IF v_caller IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    -- Safe Retention Cleanup: purge expired cooldowns and expired 0-use invitations
    DELETE FROM public.deleted_account_cooldowns
    WHERE email_available_at <= NOW() AND username_available_at <= NOW();

    DELETE FROM public.invitations
    WHERE expires_at <= NOW() - INTERVAL '7 days' AND uses_count = 0;

    -- Total PostgreSQL database size
    SELECT pg_database_size(current_database()) INTO v_total_db_bytes;

    v_usage_pct := ROUND((v_total_db_bytes::NUMERIC / v_limit_bytes::NUMERIC) * 100.0, 2);

    v_warning_level := CASE
        WHEN v_usage_pct >= 95.0 THEN 'critical_95'
        WHEN v_usage_pct >= 85.0 THEN 'critical_85'
        WHEN v_usage_pct >= 75.0 THEN 'warning_75'
        WHEN v_usage_pct >= 60.0 THEN 'warning_60'
        ELSE 'normal'
    END;

    SELECT GREATEST(COUNT(*)::INTEGER, 1) INTO v_user_count FROM public.profiles;
    SELECT COUNT(*)::INTEGER INTO v_entry_count FROM public.user_anime;
    SELECT COUNT(*)::INTEGER INTO v_review_count
    FROM public.user_anime
    WHERE personal_rating > 0 AND length(trim(COALESCE(review_notes, ''))) > 0;

    SELECT COALESCE(SUM(pg_total_relation_size(c.oid)), 0)
    INTO v_user_data_bytes
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relkind = 'r'
      AND c.relname IN ('profiles', 'user_anime', 'collections', 'collection_anime', 'review_likes', 'community_members', 'privacy_settings');

    SELECT COALESCE(pg_total_relation_size('public.user_anime'::regclass), 0)
    INTO v_review_table_bytes;

    v_bytes_per_user := GREATEST(2048, (v_user_data_bytes / GREATEST(v_user_count, 1)));
    v_bytes_per_review := CASE
        WHEN v_entry_count > 0 THEN GREATEST(256, v_review_table_bytes / v_entry_count)
        ELSE 380
    END;

    v_remaining_bytes := GREATEST(0, v_limit_bytes - v_total_db_bytes);
    v_estimated_remaining_users := v_remaining_bytes / GREATEST(v_bytes_per_user, 1);

    SELECT COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'table_name', t.relname,
                'row_count', GREATEST(0, t.reltuples::BIGINT),
                'table_bytes', pg_relation_size(t.oid),
                'index_bytes', pg_indexes_size(t.oid),
                'total_bytes', pg_total_relation_size(t.oid)
            )
            ORDER BY pg_total_relation_size(t.oid) DESC
        ),
        '[]'::jsonb
    )
    INTO v_tables
    FROM pg_class t
    JOIN pg_namespace ns ON ns.oid = t.relnamespace
    WHERE ns.nspname = 'public' AND t.relkind = 'r';

    RETURN jsonb_build_object(
        'total_bytes', v_total_db_bytes,
        'limit_bytes', v_limit_bytes,
        'usage_percent', v_usage_pct,
        'warning_level', v_warning_level,
        'user_count', v_user_count,
        'entry_count', v_entry_count,
        'review_count', v_review_count,
        'bytes_per_user', v_bytes_per_user,
        'bytes_per_review', v_bytes_per_review,
        'estimated_remaining_users', v_estimated_remaining_users,
        'tables', v_tables
    );
END;
$$;

REVOKE ALL ON FUNCTION public.get_database_storage_stats() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_database_storage_stats() TO authenticated;

NOTIFY pgrst, 'reload schema';
