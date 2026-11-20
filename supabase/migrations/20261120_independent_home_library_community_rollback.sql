-- ============================================================================
-- ANIME UNIVERSE — REVERSIBLE ROLLBACK SCRIPT
-- Safely reverts 20261120_independent_home_library_community.sql without
-- deleting any existing user_anime Library rows, profiles, or user data.
-- ============================================================================

-- 1. Preserve any new community notes/ratings back into existing user_anime rows
UPDATE public.user_anime ua
SET personal_rating = COALESCE(cn.rating, ua.personal_rating),
    review_notes = CASE
        WHEN length(trim(COALESCE(cn.note_text, ''))) > 0 THEN cn.note_text
        ELSE ua.review_notes
    END,
    updated_at = GREATEST(ua.updated_at, cn.updated_at)
FROM public.community_notes cn
WHERE ua.user_id = cn.user_id
  AND ua.anime_id = cn.anime_id;

-- 2. Drop new RPCs and triggers
DROP FUNCTION IF EXISTS public.add_to_home(INTEGER, SMALLINT);
DROP FUNCTION IF EXISTS public.remove_from_home(INTEGER);
DROP FUNCTION IF EXISTS public.upsert_community_note(INTEGER, NUMERIC, TEXT);
DROP FUNCTION IF EXISTS public.update_episode_progress(INTEGER, INTEGER);
DROP FUNCTION IF EXISTS public.toggle_community_note_like(UUID);

DROP TRIGGER IF EXISTS trg_user_anime_home_lifecycle ON public.user_anime;
DROP FUNCTION IF EXISTS public.handle_user_anime_home_lifecycle();

-- 3. Restore prior user_anime BEFORE trigger
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

-- 4. Restore prior community_anime_stats_view
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

-- 5. Drop added tables & helper column
DROP TABLE IF EXISTS public.community_note_likes;
DROP TABLE IF EXISTS public.community_notes;
DROP FUNCTION IF EXISTS public.handle_community_notes_before_save();
DROP TABLE IF EXISTS public.home_tracking;

ALTER TABLE public.user_anime
    DROP COLUMN IF EXISTS home_removed_at;

NOTIFY pgrst, 'reload schema';
