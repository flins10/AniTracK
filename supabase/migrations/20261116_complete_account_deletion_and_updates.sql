-- ============================================================================
-- ANIME UNIVERSE — COMPLETE ACCOUNT DELETION, CASCADE CONSTRAINTS,
-- IN-APP UPDATES & PUSH NOTIFICATIONS MIGRATION
-- Run this script in the Supabase SQL Editor (idempotent / safe to re-run)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- 1. ENSURE PROPER FOREIGN KEY CASCADE CONSTRAINTS ON ALL USER TABLES
-- ----------------------------------------------------------------------------

-- 1.1 public.profiles -> auth.users(id) ON DELETE CASCADE
ALTER TABLE public.profiles
    DROP CONSTRAINT IF EXISTS profiles_id_fkey,
    ADD CONSTRAINT profiles_id_fkey
        FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE;

-- 1.2 public.user_anime -> auth.users(id) ON DELETE CASCADE
ALTER TABLE public.user_anime
    DROP CONSTRAINT IF EXISTS user_anime_user_id_fkey,
    ADD CONSTRAINT user_anime_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;

-- 1.3 public.collections -> auth.users(id) ON DELETE CASCADE
ALTER TABLE public.collections
    DROP CONSTRAINT IF EXISTS collections_creator_id_fkey,
    ADD CONSTRAINT collections_creator_id_fkey
        FOREIGN KEY (creator_id) REFERENCES auth.users(id) ON DELETE CASCADE;

-- 1.4 public.collection_anime -> public.collections(id) ON DELETE CASCADE
ALTER TABLE public.collection_anime
    DROP CONSTRAINT IF EXISTS collection_anime_collection_id_fkey,
    ADD CONSTRAINT collection_anime_collection_id_fkey
        FOREIGN KEY (collection_id) REFERENCES public.collections(id) ON DELETE CASCADE;

-- 1.5 public.community_members -> auth.users(id) ON DELETE CASCADE
ALTER TABLE public.community_members
    DROP CONSTRAINT IF EXISTS community_members_user_id_fkey,
    ADD CONSTRAINT community_members_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;

-- 1.6 public.privacy_settings -> auth.users(id) ON DELETE CASCADE
ALTER TABLE public.privacy_settings
    DROP CONSTRAINT IF EXISTS privacy_settings_user_id_fkey,
    ADD CONSTRAINT privacy_settings_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;

-- 1.7 public.invitations.created_by -> auth.users(id) ON DELETE SET NULL
ALTER TABLE public.invitations
    DROP CONSTRAINT IF EXISTS invitations_created_by_fkey,
    ADD CONSTRAINT invitations_created_by_fkey
        FOREIGN KEY (created_by) REFERENCES auth.users(id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- 2. REMOTE IN-APP UPDATE RELEASES TABLE & RPC
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.app_releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    platform TEXT NOT NULL DEFAULT 'all' CHECK (platform IN ('all', 'android', 'ios', 'macos', 'windows')),
    version_code INTEGER NOT NULL CHECK (version_code >= 1),
    version_name TEXT NOT NULL CHECK (char_length(trim(version_name)) BETWEEN 1 AND 32),
    release_notes TEXT NOT NULL DEFAULT '',
    download_url TEXT NOT NULL DEFAULT '',
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_releases_platform_version
    ON public.app_releases (platform, version_code DESC, published_at DESC);

ALTER TABLE public.app_releases ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS app_releases_public_read ON public.app_releases;
CREATE POLICY app_releases_public_read ON public.app_releases
    FOR SELECT USING (TRUE);

CREATE OR REPLACE FUNCTION public.get_latest_app_release(p_platform TEXT DEFAULT 'android')
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_norm_platform TEXT := lower(trim(COALESCE(p_platform, 'android')));
    v_row public.app_releases%ROWTYPE;
BEGIN
    SELECT * INTO v_row
    FROM public.app_releases
    WHERE platform IN (v_norm_platform, 'all')
    ORDER BY version_code DESC, published_at DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('available', FALSE);
    END IF;

    RETURN jsonb_build_object(
        'available', TRUE,
        'version_code', v_row.version_code,
        'version_name', v_row.version_name,
        'release_notes', v_row.release_notes,
        'download_url', v_row.download_url,
        'is_mandatory', v_row.is_mandatory,
        'platform', v_row.platform,
        'published_at', v_row.published_at
    );
END;
$$;

REVOKE ALL ON FUNCTION public.get_latest_app_release(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_latest_app_release(TEXT) TO anon, authenticated;

-- Seed baseline v2.1.0 release entry if none exists
INSERT INTO public.app_releases (platform, version_code, version_name, release_notes, download_url, is_mandatory)
SELECT
    'all',
    3,
    '2.1.0',
    '• Permanent one-step account & cloud data deletion' || E'\n' ||
    '• Numbered multi-season switching & spin-off franchise discovery' || E'\n' ||
    '• Tap-middle custom episode jump & adaptive cross-platform UI',
    'https://play.google.com/store/apps/details?id=com.aistudio.anitrack.knyw',
    FALSE
WHERE NOT EXISTS (SELECT 1 FROM public.app_releases WHERE version_code >= 3);

-- ----------------------------------------------------------------------------
-- 3. PUSH NOTIFICATION DEVICE TOKENS TABLE & RPC
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    platform TEXT NOT NULL DEFAULT 'android' CHECK (platform IN ('android', 'ios', 'macos', 'windows', 'web')),
    token TEXT NOT NULL CHECK (char_length(trim(token)) BETWEEN 8 AND 512),
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, token)
);

CREATE INDEX IF NOT EXISTS idx_user_push_tokens_user_id
    ON public.user_push_tokens (user_id);

ALTER TABLE public.user_push_tokens ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS user_push_tokens_select_own ON public.user_push_tokens;
CREATE POLICY user_push_tokens_select_own ON public.user_push_tokens
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS user_push_tokens_insert_own ON public.user_push_tokens;
CREATE POLICY user_push_tokens_insert_own ON public.user_push_tokens
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS user_push_tokens_update_own ON public.user_push_tokens;
CREATE POLICY user_push_tokens_update_own ON public.user_push_tokens
    FOR UPDATE USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS user_push_tokens_delete_own ON public.user_push_tokens;
CREATE POLICY user_push_tokens_delete_own ON public.user_push_tokens
    FOR DELETE USING (auth.uid() = user_id);

CREATE OR REPLACE FUNCTION public.upsert_push_token(
    p_token TEXT,
    p_platform TEXT DEFAULT 'android',
    p_enabled BOOLEAN DEFAULT TRUE
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_clean_token TEXT := trim(COALESCE(p_token, ''));
    v_clean_platform TEXT := lower(trim(COALESCE(p_platform, 'android')));
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;
    IF char_length(v_clean_token) < 8 THEN
        RAISE EXCEPTION 'Invalid push token';
    END IF;
    IF v_clean_platform NOT IN ('android', 'ios', 'macos', 'windows', 'web') THEN
        v_clean_platform := 'android';
    END IF;

    INSERT INTO public.user_push_tokens (user_id, platform, token, notifications_enabled, updated_at)
    VALUES (v_user_id, v_clean_platform, v_clean_token, COALESCE(p_enabled, TRUE), NOW())
    ON CONFLICT (user_id, token) DO UPDATE
        SET platform = EXCLUDED.platform,
            notifications_enabled = EXCLUDED.notifications_enabled,
            updated_at = NOW();

    RETURN jsonb_build_object('synced', TRUE);
END;
$$;

REVOKE ALL ON FUNCTION public.upsert_push_token(TEXT, TEXT, BOOLEAN) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.upsert_push_token(TEXT, TEXT, BOOLEAN) TO authenticated;

-- ----------------------------------------------------------------------------
-- 4. COMPLETE PERMANENT ACCOUNT & DATA DELETION RPC
-- ----------------------------------------------------------------------------
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

    -- Purge expired cooldown entries
    DELETE FROM public.deleted_account_cooldowns
    WHERE email_available_at <= NOW() AND username_available_at <= NOW();

    -- Record 5-minute email cooldown and 24-hour username cooldown
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

    -- 1. Delete user storage objects in 'avatars' bucket
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

    -- 2. Delete all user-owned database rows (preserving shared public.anime catalog)
    DELETE FROM public.collection_anime ca
    USING public.collections c
    WHERE ca.collection_id = c.id AND c.creator_id = v_user_id;

    DELETE FROM public.collections WHERE creator_id = v_user_id;
    DELETE FROM public.user_anime WHERE user_id = v_user_id;
    DELETE FROM public.privacy_settings WHERE user_id = v_user_id;
    DELETE FROM public.community_members WHERE user_id = v_user_id;
    DELETE FROM public.user_push_tokens WHERE user_id = v_user_id;
    UPDATE public.invitations SET created_by = NULL WHERE created_by = v_user_id;
    DELETE FROM public.profiles WHERE id = v_user_id;

    -- 3. Permanently delete the user from Supabase Auth (auth.users)
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
REVOKE ALL ON FUNCTION public.delete_my_account() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.delete_my_account() TO authenticated;

NOTIFY pgrst, 'reload schema';
