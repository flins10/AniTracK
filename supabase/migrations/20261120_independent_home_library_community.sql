-- ============================================================================
-- ANIME UNIVERSE — INDEPENDENT HOME, LIBRARY & COMMUNITY ARCHITECTURE
-- Safe, idempotent, non-destructive forward migration
-- Preserves 100% of existing users, user_anime tracking rows, ratings,
-- reviews, collections, community memberships, and likes.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- 1. ADDITIVE COLUMN ON PERMANENT LIBRARY TABLE (public.user_anime)
-- ----------------------------------------------------------------------------
ALTER TABLE public.user_anime
    ADD COLUMN IF NOT EXISTS home_removed_at TIMESTAMPTZ NULL;

-- ----------------------------------------------------------------------------
-- 2. INDEPENDENT HOME TRACKING TABLE (public.home_tracking)
--    Home displays ONLY anime explicitly present in this table.
--    Adding notes, ratings, comments, or Community activity NEVER writes here.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.home_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    anime_id INTEGER NOT NULL REFERENCES public.anime(anilist_id) ON DELETE CASCADE,
    airing_day_of_week SMALLINT CHECK (
        airing_day_of_week IS NULL OR (airing_day_of_week >= 1 AND airing_day_of_week <= 7)
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_home_tracking_user_anime UNIQUE (user_id, anime_id)
);

CREATE INDEX IF NOT EXISTS idx_home_tracking_user_updated
    ON public.home_tracking(user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_home_tracking_anime_id
    ON public.home_tracking(anime_id);

DROP TRIGGER IF EXISTS trg_home_tracking_updated_at ON public.home_tracking;
CREATE TRIGGER trg_home_tracking_updated_at
    BEFORE UPDATE ON public.home_tracking
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.home_tracking ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS home_tracking_select_own ON public.home_tracking;
CREATE POLICY home_tracking_select_own ON public.home_tracking
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS home_tracking_insert_own ON public.home_tracking;
CREATE POLICY home_tracking_insert_own ON public.home_tracking
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS home_tracking_update_own ON public.home_tracking;
CREATE POLICY home_tracking_update_own ON public.home_tracking
    FOR UPDATE USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS home_tracking_delete_own ON public.home_tracking;
CREATE POLICY home_tracking_delete_own ON public.home_tracking
    FOR DELETE USING (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- 3. INDEPENDENT COMMUNITY NOTES & LIKES TABLES
--    Completely decoupled from Home and Library membership.
--    Removing an anime from Home or Library NEVER deletes Community content.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.community_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    anime_id INTEGER NOT NULL REFERENCES public.anime(anilist_id) ON DELETE CASCADE,
    rating NUMERIC(3, 1) CHECK (rating IS NULL OR (rating >= 0 AND rating <= 10)),
    note_text TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_community_notes_user_anime UNIQUE (user_id, anime_id)
);

CREATE INDEX IF NOT EXISTS idx_community_notes_anime_updated
    ON public.community_notes(anime_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_community_notes_user_updated
    ON public.community_notes(user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_community_notes_updated_at
    ON public.community_notes(updated_at DESC);

CREATE OR REPLACE FUNCTION public.handle_community_notes_before_save()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    IF NEW.note_text IS NULL THEN
        NEW.note_text = '';
    ELSIF char_length(NEW.note_text) > 500 THEN
        NEW.note_text = left(trim(NEW.note_text), 500);
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_community_notes_before_save ON public.community_notes;
CREATE TRIGGER trg_community_notes_before_save
    BEFORE INSERT OR UPDATE ON public.community_notes
    FOR EACH ROW EXECUTE FUNCTION public.handle_community_notes_before_save();

ALTER TABLE public.community_notes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS community_notes_select_policy ON public.community_notes;
CREATE POLICY community_notes_select_policy ON public.community_notes
    FOR SELECT USING (
        auth.uid() = user_id
        OR (
            public.is_active_member(auth.uid())
            AND public.is_active_member(user_id)
        )
    );

DROP POLICY IF EXISTS community_notes_insert_own ON public.community_notes;
CREATE POLICY community_notes_insert_own ON public.community_notes
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS community_notes_update_own ON public.community_notes;
CREATE POLICY community_notes_update_own ON public.community_notes
    FOR UPDATE USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS community_notes_delete_own ON public.community_notes;
CREATE POLICY community_notes_delete_own ON public.community_notes
    FOR DELETE USING (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS public.community_note_likes (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    note_id UUID NOT NULL REFERENCES public.community_notes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, note_id)
);

CREATE INDEX IF NOT EXISTS idx_community_note_likes_note_id
    ON public.community_note_likes(note_id);

ALTER TABLE public.community_note_likes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS community_note_likes_select_policy ON public.community_note_likes;
CREATE POLICY community_note_likes_select_policy ON public.community_note_likes
    FOR SELECT USING (
        auth.uid() IS NOT NULL
        AND (auth.uid() = user_id OR public.is_active_member(auth.uid()))
    );

DROP POLICY IF EXISTS community_note_likes_insert_own ON public.community_note_likes;
CREATE POLICY community_note_likes_insert_own ON public.community_note_likes
    FOR INSERT WITH CHECK (
        auth.uid() = user_id
        AND public.is_active_member(auth.uid())
    );

DROP POLICY IF EXISTS community_note_likes_delete_own ON public.community_note_likes;
CREATE POLICY community_note_likes_delete_own ON public.community_note_likes
    FOR DELETE USING (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- 4. NON-DESTRUCTIVE BACKFILL FROM EXISTING public.user_anime
-- ----------------------------------------------------------------------------

-- 4.1 Backfill active Home tracking ONLY for actively watching anime that are
--     NOT officially completed at/past total episodes and NOT explicitly removed.
INSERT INTO public.home_tracking (user_id, anime_id, created_at, updated_at)
SELECT
    ua.user_id,
    ua.anime_id,
    COALESCE(ua.date_added, NOW()),
    COALESCE(ua.updated_at, NOW())
FROM public.user_anime ua
LEFT JOIN public.anime a ON a.anilist_id = ua.anime_id
WHERE ua.status = 'watching'
  AND ua.home_removed_at IS NULL
  AND NOT (
      UPPER(COALESCE(a.status, '')) IN ('FINISHED', 'FINISHED_AIRING')
      AND COALESCE(NULLIF(ua.total_episodes, 0), NULLIF(a.episodes, 0), 0) > 0
      AND ua.episode_progress >= COALESCE(NULLIF(ua.total_episodes, 0), NULLIF(a.episodes, 0))
  )
ON CONFLICT (user_id, anime_id) DO NOTHING;

-- 4.2 Backfill independent Community notes/ratings from existing user_anime rows
INSERT INTO public.community_notes (user_id, anime_id, rating, note_text, created_at, updated_at)
SELECT
    ua.user_id,
    ua.anime_id,
    CASE WHEN COALESCE(ua.personal_rating, 0) > 0 THEN ua.personal_rating ELSE NULL END,
    left(trim(COALESCE(ua.review_notes, '')), 500),
    COALESCE(ua.date_added, NOW()),
    COALESCE(ua.updated_at, NOW())
FROM public.user_anime ua
WHERE COALESCE(ua.personal_rating, 0) > 0
   OR length(trim(COALESCE(ua.review_notes, ''))) > 0
ON CONFLICT (user_id, anime_id) DO NOTHING;

-- 4.3 Backfill existing review_likes into community_note_likes
INSERT INTO public.community_note_likes (user_id, note_id, created_at)
SELECT
    rl.user_id,
    cn.id,
    rl.created_at
FROM public.review_likes rl
JOIN public.user_anime ua ON ua.id = rl.review_id
JOIN public.community_notes cn
  ON cn.user_id = ua.user_id
 AND cn.anime_id = ua.anime_id
ON CONFLICT (user_id, note_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 5. STRICT AUTOMATIC COMPLETION & DROPPED LIFECYCLE TRIGGERS
--    Auto-completes ONLY when ALL 3 conditions hold:
--      1. Official anime status is FINISHED / FINISHED_AIRING
--      2. Official total episode count is known (> 0)
--      3. User's watched episode count >= total episode count
--    Ongoing anime or unknown episode count NEVER auto-complete.
--    Dropped or officially completed anime are automatically removed ONLY from
--    public.home_tracking; Library (user_anime) and Community are preserved.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_user_anime_before_save()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_anime_status TEXT;
    v_anime_episodes INTEGER;
    v_effective_total INTEGER;
    v_official_finished BOOLEAN := FALSE;
BEGIN
    NEW.updated_at = NOW();

    IF NEW.review_notes IS NOT NULL AND char_length(NEW.review_notes) > 500 THEN
        NEW.review_notes = left(trim(NEW.review_notes), 500);
    END IF;

    IF NEW.episode_progress IS NULL OR NEW.episode_progress < 0 THEN
        NEW.episode_progress = 0;
    END IF;

    SELECT a.status, a.episodes
    INTO v_anime_status, v_anime_episodes
    FROM public.anime a
    WHERE a.anilist_id = NEW.anime_id;

    v_effective_total := COALESCE(NULLIF(NEW.total_episodes, 0), NULLIF(v_anime_episodes, 0));

    IF v_effective_total IS NOT NULL AND v_effective_total > 0 THEN
        IF NEW.total_episodes IS NULL OR NEW.total_episodes <= 0 THEN
            NEW.total_episodes = v_effective_total;
        END IF;
        IF NEW.episode_progress > v_effective_total THEN
            NEW.episode_progress = v_effective_total;
        END IF;
    END IF;

    v_official_finished := UPPER(COALESCE(v_anime_status, '')) IN ('FINISHED', 'FINISHED_AIRING');

    -- Strict automatic completion rule: ONLY when officially finished + known total + watched >= total
    IF v_official_finished
       AND v_effective_total IS NOT NULL
       AND v_effective_total > 0
       AND NEW.episode_progress >= v_effective_total
       AND NEW.status <> 'dropped' THEN
        NEW.status = 'completed';
    END IF;

    IF NEW.status = 'completed' AND NEW.date_completed IS NULL THEN
        NEW.date_completed = NOW();
    ELSIF NEW.status <> 'completed' THEN
        NEW.date_completed = NULL;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_user_anime_before_save ON public.user_anime;
CREATE TRIGGER trg_user_anime_before_save
    BEFORE INSERT OR UPDATE ON public.user_anime
    FOR EACH ROW EXECUTE FUNCTION public.handle_user_anime_before_save();

CREATE OR REPLACE FUNCTION public.handle_user_anime_home_lifecycle()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    -- Automatically remove ONLY from Home when Dropped or Completed.
    -- NEVER auto-insert into home_tracking on notes, ratings, or updates.
    IF NEW.status IN ('dropped', 'completed') THEN
        DELETE FROM public.home_tracking
        WHERE user_id = NEW.user_id
          AND anime_id = NEW.anime_id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_user_anime_home_lifecycle ON public.user_anime;
CREATE TRIGGER trg_user_anime_home_lifecycle
    AFTER INSERT OR UPDATE OF status, episode_progress, total_episodes ON public.user_anime
    FOR EACH ROW EXECUTE FUNCTION public.handle_user_anime_home_lifecycle();

-- ----------------------------------------------------------------------------
-- 6. ATOMIC RPC FUNCTIONS FOR HOME, EPISODE INPUT & COMMUNITY
-- ----------------------------------------------------------------------------

-- 6.1 Explicitly add an anime to Home (and ensure a Library row without wiping data)
CREATE OR REPLACE FUNCTION public.add_to_home(
    p_anime_id INTEGER,
    p_airing_day SMALLINT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_existing public.user_anime%ROWTYPE;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    SELECT * INTO v_existing
    FROM public.user_anime
    WHERE user_id = v_user_id AND anime_id = p_anime_id;

    IF NOT FOUND THEN
        INSERT INTO public.user_anime (user_id, anime_id, status, episode_progress, home_removed_at)
        VALUES (v_user_id, p_anime_id, 'watching', 0, NULL)
        ON CONFLICT (user_id, anime_id) DO UPDATE
            SET home_removed_at = NULL,
                updated_at = NOW();
    ELSE
        UPDATE public.user_anime
        SET home_removed_at = NULL,
            status = CASE WHEN status IN ('plan_to_watch', 'dropped') THEN 'watching' ELSE status END,
            updated_at = NOW()
        WHERE user_id = v_user_id AND anime_id = p_anime_id;
    END IF;

    INSERT INTO public.home_tracking (user_id, anime_id, airing_day_of_week)
    VALUES (
        v_user_id,
        p_anime_id,
        CASE WHEN p_airing_day BETWEEN 1 AND 7 THEN p_airing_day ELSE NULL END
    )
    ON CONFLICT (user_id, anime_id) DO UPDATE
        SET airing_day_of_week = COALESCE(EXCLUDED.airing_day_of_week, public.home_tracking.airing_day_of_week),
            updated_at = NOW();

    RETURN jsonb_build_object(
        'in_home', TRUE,
        'anime_id', p_anime_id
    );
END;
$$;

REVOKE ALL ON FUNCTION public.add_to_home(INTEGER, SMALLINT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.add_to_home(INTEGER, SMALLINT) TO authenticated;

-- 6.2 Remove ONLY from Home (preserves Library entry, progress, notes, ratings & Community)
CREATE OR REPLACE FUNCTION public.remove_from_home(p_anime_id INTEGER)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    DELETE FROM public.home_tracking
    WHERE user_id = v_user_id
      AND anime_id = p_anime_id;

    UPDATE public.user_anime
    SET home_removed_at = NOW(),
        updated_at = NOW()
    WHERE user_id = v_user_id
      AND anime_id = p_anime_id;

    RETURN jsonb_build_object(
        'removed_from_home', TRUE,
        'anime_id', p_anime_id,
        'library_preserved', TRUE
    );
END;
$$;

REVOKE ALL ON FUNCTION public.remove_from_home(INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.remove_from_home(INTEGER) TO authenticated;

-- 6.3 Upsert Community Note / Rating independently (NEVER adds to Home)
CREATE OR REPLACE FUNCTION public.upsert_community_note(
    p_anime_id INTEGER,
    p_rating NUMERIC DEFAULT NULL,
    p_note_text TEXT DEFAULT ''
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_clean_note TEXT := left(trim(COALESCE(p_note_text, '')), 500);
    v_clean_rating NUMERIC(3, 1);
    v_row public.community_notes%ROWTYPE;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    IF p_rating IS NOT NULL THEN
        IF p_rating < 0 OR p_rating > 10 THEN
            RAISE EXCEPTION 'Rating must be between 0 and 10';
        END IF;
        v_clean_rating := CASE WHEN p_rating > 0 THEN ROUND(p_rating, 1) ELSE NULL END;
    ELSE
        v_clean_rating := NULL;
    END IF;

    INSERT INTO public.community_notes (user_id, anime_id, rating, note_text)
    VALUES (v_user_id, p_anime_id, v_clean_rating, v_clean_note)
    ON CONFLICT (user_id, anime_id) DO UPDATE
        SET rating = COALESCE(EXCLUDED.rating, public.community_notes.rating),
            note_text = EXCLUDED.note_text,
            updated_at = NOW()
    RETURNING * INTO v_row;

    -- Keep existing Library row rating/note aligned ONLY if already in Library (never inserts or adds to Home)
    UPDATE public.user_anime
    SET personal_rating = COALESCE(v_clean_rating, personal_rating),
        review_notes = CASE WHEN v_clean_note <> '' THEN v_clean_note ELSE review_notes END,
        updated_at = NOW()
    WHERE user_id = v_user_id
      AND anime_id = p_anime_id;

    RETURN jsonb_build_object(
        'id', v_row.id,
        'user_id', v_row.user_id,
        'anime_id', v_row.anime_id,
        'rating', v_row.rating,
        'note_text', v_row.note_text,
        'updated_at', v_row.updated_at,
        'added_to_home', FALSE
    );
END;
$$;

REVOKE ALL ON FUNCTION public.upsert_community_note(INTEGER, NUMERIC, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.upsert_community_note(INTEGER, NUMERIC, TEXT) TO authenticated;

-- 6.4 Direct Custom Episode Progress Update with strict validation & auto-completion
CREATE OR REPLACE FUNCTION public.update_episode_progress(
    p_anime_id INTEGER,
    p_watched_episodes INTEGER
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_anime_status TEXT;
    v_anime_episodes INTEGER;
    v_existing public.user_anime%ROWTYPE;
    v_known_total INTEGER;
    v_official_finished BOOLEAN := FALSE;
    v_auto_completed BOOLEAN := FALSE;
    v_next_status TEXT;
    v_in_home BOOLEAN := FALSE;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    IF p_watched_episodes IS NULL OR p_watched_episodes < 0 THEN
        RAISE EXCEPTION 'Episode count must be a non-negative whole number';
    END IF;

    SELECT a.status, a.episodes
    INTO v_anime_status, v_anime_episodes
    FROM public.anime a
    WHERE a.anilist_id = p_anime_id;

    SELECT * INTO v_existing
    FROM public.user_anime
    WHERE user_id = v_user_id AND anime_id = p_anime_id;

    v_known_total := COALESCE(NULLIF(v_existing.total_episodes, 0), NULLIF(v_anime_episodes, 0));

    IF v_known_total IS NOT NULL AND v_known_total > 0 AND p_watched_episodes > v_known_total THEN
        RAISE EXCEPTION 'Episode count (%) cannot exceed total episodes (%)', p_watched_episodes, v_known_total;
    END IF;

    v_official_finished := UPPER(COALESCE(v_anime_status, '')) IN ('FINISHED', 'FINISHED_AIRING');
    v_auto_completed := v_official_finished
        AND v_known_total IS NOT NULL
        AND v_known_total > 0
        AND p_watched_episodes >= v_known_total;

    -- Preserve current status unless strict official auto-completion triggers
    v_next_status := COALESCE(v_existing.status, 'watching');
    IF v_auto_completed AND v_next_status <> 'dropped' THEN
        v_next_status := 'completed';
    END IF;

    IF NOT FOUND THEN
        INSERT INTO public.user_anime (user_id, anime_id, status, episode_progress, total_episodes)
        VALUES (v_user_id, p_anime_id, v_next_status, p_watched_episodes, v_known_total)
        RETURNING * INTO v_existing;
    ELSE
        UPDATE public.user_anime
        SET episode_progress = p_watched_episodes,
            total_episodes = COALESCE(v_known_total, total_episodes),
            status = v_next_status,
            updated_at = NOW()
        WHERE user_id = v_user_id AND anime_id = p_anime_id
        RETURNING * INTO v_existing;
    END IF;

    IF v_auto_completed THEN
        DELETE FROM public.home_tracking
        WHERE user_id = v_user_id AND anime_id = p_anime_id;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM public.home_tracking
        WHERE user_id = v_user_id AND anime_id = p_anime_id
    ) INTO v_in_home;

    RETURN jsonb_build_object(
        'anime_id', p_anime_id,
        'episode_progress', v_existing.episode_progress,
        'total_episodes', v_existing.total_episodes,
        'status', v_existing.status,
        'auto_completed', v_auto_completed,
        'in_home', v_in_home
    );
END;
$$;

REVOKE ALL ON FUNCTION public.update_episode_progress(INTEGER, INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.update_episode_progress(INTEGER, INTEGER) TO authenticated;

-- 6.5 Toggle Like on independent community_notes
CREATE OR REPLACE FUNCTION public.toggle_community_note_like(p_note_id UUID)
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
        SELECT 1 FROM public.community_note_likes
        WHERE user_id = v_user_id AND note_id = p_note_id
    ) THEN
        DELETE FROM public.community_note_likes
        WHERE user_id = v_user_id AND note_id = p_note_id;
        v_liked := FALSE;
    ELSE
        INSERT INTO public.community_note_likes (user_id, note_id)
        VALUES (v_user_id, p_note_id)
        ON CONFLICT (user_id, note_id) DO NOTHING;
        v_liked := TRUE;
    END IF;

    SELECT COUNT(*)::INTEGER INTO v_count
    FROM public.community_note_likes
    WHERE note_id = p_note_id;

    RETURN jsonb_build_object(
        'note_id', p_note_id,
        'liked', v_liked,
        'likes_count', v_count
    );
END;
$$;

REVOKE ALL ON FUNCTION public.toggle_community_note_like(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.toggle_community_note_like(UUID) TO authenticated;

-- ----------------------------------------------------------------------------
-- 7. UPDATED VIEW & ACCOUNT CLEANUP / STORAGE STATS FUNCTIONS
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW public.community_anime_stats_view AS
SELECT
    a.anilist_id AS anime_id,
    COALESCE(lib.tracking_count, 0)::INTEGER AS tracking_count,
    COALESCE(comm.rating_count, 0)::INTEGER AS rating_count,
    comm.avg_rating_10,
    COALESCE(comm.review_count, 0)::INTEGER AS review_count,
    COALESCE(comm.last_reviewed_at, lib.last_updated_at) AS last_reviewed_at
FROM public.anime a
LEFT JOIN (
    SELECT anime_id, COUNT(*)::INTEGER AS tracking_count, MAX(updated_at) AS last_updated_at
    FROM public.user_anime
    GROUP BY anime_id
) lib ON lib.anime_id = a.anilist_id
LEFT JOIN (
    SELECT
        anime_id,
        COUNT(CASE WHEN rating > 0 THEN 1 END)::INTEGER AS rating_count,
        ROUND(AVG(NULLIF(rating, 0)), 2) AS avg_rating_10,
        COUNT(CASE WHEN COALESCE(rating, 0) > 0 OR length(trim(COALESCE(note_text, ''))) > 0 THEN 1 END)::INTEGER AS review_count,
        MAX(updated_at) AS last_reviewed_at
    FROM public.community_notes
    GROUP BY anime_id
) comm ON comm.anime_id = a.anilist_id
WHERE lib.anime_id IS NOT NULL OR comm.anime_id IS NOT NULL;

CREATE OR REPLACE FUNCTION public.delete_my_account()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, storage, extensions
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_email TEXT;
    v_username TEXT;
    v_email_hash TEXT;
    v_auth_deleted BOOLEAN := FALSE;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    SELECT u.email INTO v_email FROM auth.users u WHERE u.id = v_user_id;
    SELECT p.username INTO v_username FROM public.profiles p WHERE p.id = v_user_id;

    DELETE FROM public.deleted_account_cooldowns
    WHERE email_available_at <= NOW() AND username_available_at <= NOW();

    IF COALESCE(trim(v_email), '') <> '' THEN
        v_email_hash := encode(digest(lower(trim(v_email)), 'sha256'), 'hex');
        DELETE FROM public.deleted_account_cooldowns WHERE email_hash = v_email_hash;
        IF v_username IS NOT NULL AND trim(v_username) <> '' THEN
            DELETE FROM public.deleted_account_cooldowns WHERE username_lower = lower(trim(v_username));
        END IF;

        INSERT INTO public.deleted_account_cooldowns (
            email_hash,
            email_available_at,
            username_lower,
            username_available_at,
            deleted_user_id
        ) VALUES (
            v_email_hash,
            NOW() + INTERVAL '5 minutes',
            CASE
                WHEN v_username IS NOT NULL AND lower(trim(v_username)) NOT LIKE 'traveler_%'
                THEN lower(trim(v_username))
                ELSE NULL
            END,
            NOW() + INTERVAL '24 hours',
            v_user_id
        );
    END IF;

    BEGIN
        DELETE FROM storage.objects
        WHERE bucket_id = 'avatars'
          AND (
              (storage.foldername(name))[1] = v_user_id::text
              OR owner = v_user_id
              OR owner_id = v_user_id::text
          );
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;

    DELETE FROM public.collection_anime ca
    USING public.collections c
    WHERE ca.collection_id = c.id AND c.creator_id = v_user_id;

    DELETE FROM public.collections WHERE creator_id = v_user_id;
    DELETE FROM public.home_tracking WHERE user_id = v_user_id;
    DELETE FROM public.community_note_likes WHERE user_id = v_user_id;
    DELETE FROM public.community_notes WHERE user_id = v_user_id;
    DELETE FROM public.review_likes WHERE user_id = v_user_id;
    DELETE FROM public.user_anime WHERE user_id = v_user_id;
    DELETE FROM public.privacy_settings WHERE user_id = v_user_id;
    DELETE FROM public.community_members WHERE user_id = v_user_id;
    IF to_regclass('public.user_push_tokens') IS NOT NULL THEN
        EXECUTE 'DELETE FROM public.user_push_tokens WHERE user_id = $1' USING v_user_id;
    END IF;
    UPDATE public.invitations SET created_by = NULL WHERE created_by = v_user_id;
    DELETE FROM public.profiles WHERE id = v_user_id;

    DELETE FROM auth.users WHERE id = v_user_id;
    v_auth_deleted := NOT EXISTS (SELECT 1 FROM auth.users WHERE id = v_user_id);

    RETURN jsonb_build_object(
        'deleted', TRUE,
        'auth_deleted', v_auth_deleted,
        'user_id', v_user_id,
        'email_cooldown_minutes', 5,
        'username_cooldown_hours', 24
    );
END;
$$;

ALTER FUNCTION public.delete_my_account() OWNER TO postgres;

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

    DELETE FROM public.deleted_account_cooldowns
    WHERE email_available_at <= NOW() AND username_available_at <= NOW();

    DELETE FROM public.invitations
    WHERE expires_at <= NOW() - INTERVAL '7 days' AND uses_count = 0;

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
    FROM public.community_notes
    WHERE COALESCE(rating, 0) > 0 OR length(trim(COALESCE(note_text, ''))) > 0;

    SELECT COALESCE(SUM(pg_total_relation_size(c.oid)), 0)
    INTO v_user_data_bytes
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relkind = 'r'
      AND c.relname IN (
          'profiles', 'user_anime', 'home_tracking', 'community_notes',
          'community_note_likes', 'collections', 'collection_anime',
          'review_likes', 'community_members', 'privacy_settings'
      );

    SELECT COALESCE(pg_total_relation_size('public.community_notes'::regclass), 0)
    INTO v_review_table_bytes;

    v_bytes_per_user := GREATEST(2048, (v_user_data_bytes / GREATEST(v_user_count, 1)));
    v_bytes_per_review := CASE
        WHEN v_review_count > 0 THEN GREATEST(256, v_review_table_bytes / v_review_count)
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
