-- ============================================================================
-- ANIME UNIVERSE — Complete Supabase PostgreSQL Schema, RPCs, RLS & Storage
-- Private, invite-only anime tracking & community platform (max 50 active members)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- 1. TABLES
-- ----------------------------------------------------------------------------

-- 1.1 Profiles
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT NOT NULL UNIQUE,
    avatar_key TEXT NOT NULL DEFAULT 'avatar_1',
    avatar_url TEXT,
    bio TEXT DEFAULT '',
    role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    can_generate_invites BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT username_length_check CHECK (char_length(trim(username)) BETWEEN 2 AND 32)
);

-- 1.2 Shared Anime Catalog (Public AniList metadata cached once per anime)
CREATE TABLE IF NOT EXISTS public.anime (
    anilist_id INTEGER PRIMARY KEY,
    title_english TEXT,
    title_romaji TEXT NOT NULL,
    title_native TEXT,
    cover_image TEXT NOT NULL DEFAULT '',
    banner_image TEXT,
    description TEXT,
    genres TEXT[] NOT NULL DEFAULT '{}',
    release_year INTEGER,
    season TEXT,
    episodes INTEGER,
    average_score INTEGER,
    popularity INTEGER,
    format TEXT,
    status TEXT,
    studios TEXT[] NOT NULL DEFAULT '{}',
    next_airing_at BIGINT,
    next_episode_number INTEGER,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.3 User Anime Tracking Entries
CREATE TABLE IF NOT EXISTS public.user_anime (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    anime_id INTEGER NOT NULL REFERENCES public.anime(anilist_id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'plan_to_watch'
        CHECK (status IN ('watching', 'completed', 'plan_to_watch', 'on_hold', 'dropped')),
    episode_progress INTEGER NOT NULL DEFAULT 0 CHECK (episode_progress >= 0),
    user_season INTEGER NOT NULL DEFAULT 1 CHECK (user_season >= 1 AND user_season <= 99),
    total_episodes INTEGER CHECK (total_episodes IS NULL OR total_episodes >= 0),
    personal_rating NUMERIC(3, 1) CHECK (personal_rating IS NULL OR (personal_rating >= 0 AND personal_rating <= 10)),
    review_notes TEXT DEFAULT '',
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    date_added TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    date_completed TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_anime UNIQUE (user_id, anime_id),
    CONSTRAINT episode_progress_cap_check CHECK (
        total_episodes IS NULL OR total_episodes <= 0 OR episode_progress <= total_episodes
    )
);

ALTER TABLE public.user_anime
    ADD COLUMN IF NOT EXISTS user_season INTEGER NOT NULL DEFAULT 1;

-- 1.4 Custom Collections
CREATE TABLE IF NOT EXISTS public.collections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL CHECK (char_length(trim(name)) BETWEEN 1 AND 64),
    description TEXT DEFAULT '',
    visibility TEXT NOT NULL DEFAULT 'private' CHECK (visibility IN ('private', 'community')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.5 Collection -> Anime Join Table
CREATE TABLE IF NOT EXISTS public.collection_anime (
    collection_id UUID NOT NULL REFERENCES public.collections(id) ON DELETE CASCADE,
    anime_id INTEGER NOT NULL REFERENCES public.anime(anilist_id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (collection_id, anime_id)
);

-- 1.6 Community Memberships (50-member active capacity limit)
CREATE TABLE IF NOT EXISTS public.community_members (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'left', 'removed')),
    role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.7 Invitations
CREATE TABLE IF NOT EXISTS public.invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    max_uses INTEGER NOT NULL DEFAULT 1 CHECK (max_uses >= 1 AND max_uses <= 50),
    uses_count INTEGER NOT NULL DEFAULT 0 CHECK (uses_count >= 0 AND uses_count <= max_uses),
    expires_at TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.8 Privacy Settings
CREATE TABLE IF NOT EXISTS public.privacy_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    watchlist_visibility TEXT NOT NULL DEFAULT 'private' CHECK (watchlist_visibility IN ('private', 'community')),
    reviews_visibility TEXT NOT NULL DEFAULT 'private' CHECK (reviews_visibility IN ('private', 'community')),
    collections_visibility TEXT NOT NULL DEFAULT 'private' CHECK (collections_visibility IN ('private', 'community')),
    activity_visibility TEXT NOT NULL DEFAULT 'private' CHECK (activity_visibility IN ('private', 'community')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 2. INDEXES
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_profiles_username ON public.profiles(username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_profiles_username_lower_unique ON public.profiles(lower(trim(username)));
CREATE INDEX IF NOT EXISTS idx_anime_popularity ON public.anime(popularity DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_user_anime_user_status ON public.user_anime(user_id, status);
CREATE INDEX IF NOT EXISTS idx_user_anime_anime_updated ON public.user_anime(anime_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_anime_updated_at ON public.user_anime(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_collections_creator ON public.collections(creator_id);
CREATE INDEX IF NOT EXISTS idx_collection_anime_anime ON public.collection_anime(anime_id);
CREATE INDEX IF NOT EXISTS idx_community_members_status ON public.community_members(status);
CREATE INDEX IF NOT EXISTS idx_invitations_code ON public.invitations(code);

-- ----------------------------------------------------------------------------
-- 3. TRIGGERS (Auto updated_at & completion timestamp)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.handle_user_anime_before_save()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
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

DROP TRIGGER IF EXISTS trg_profiles_updated_at ON public.profiles;
CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS trg_anime_updated_at ON public.anime;
CREATE TRIGGER trg_anime_updated_at
    BEFORE UPDATE ON public.anime
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS trg_user_anime_before_save ON public.user_anime;
CREATE TRIGGER trg_user_anime_before_save
    BEFORE INSERT OR UPDATE ON public.user_anime
    FOR EACH ROW EXECUTE FUNCTION public.handle_user_anime_before_save();

DROP TRIGGER IF EXISTS trg_collections_updated_at ON public.collections;
CREATE TRIGGER trg_collections_updated_at
    BEFORE UPDATE ON public.collections
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS trg_community_members_updated_at ON public.community_members;
CREATE TRIGGER trg_community_members_updated_at
    BEFORE UPDATE ON public.community_members
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS trg_privacy_settings_updated_at ON public.privacy_settings;
CREATE TRIGGER trg_privacy_settings_updated_at
    BEFORE UPDATE ON public.privacy_settings
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- Auto-create personal profile & default private settings on Auth Sign Up
-- Privacy rule: NEVER derive public username from the user's email address.
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_onboarding_pending BOOLEAN := COALESCE(lower(NEW.raw_user_meta_data->>'onboarding_completed'), 'false') = 'false';
    v_meta_username TEXT := trim(COALESCE(NEW.raw_user_meta_data->>'username', ''));
    v_raw_name TEXT := CASE
        WHEN v_onboarding_pending OR v_meta_username = '' OR position('@' in v_meta_username) > 0
            THEN 'traveler_' || left(replace(NEW.id::text, '-', ''), 8)
        ELSE v_meta_username
    END;
    v_clean_name TEXT;
    v_avatar_key TEXT := COALESCE(NULLIF(trim(NEW.raw_user_meta_data->>'avatar_key'), ''), 'avatar_1');
BEGIN
    IF char_length(v_raw_name) < 2 THEN
        v_raw_name := 'traveler_' || left(replace(NEW.id::text, '-', ''), 8);
    END IF;
    v_clean_name := left(v_raw_name, 32);

    INSERT INTO public.profiles (id, username, avatar_key, role, can_generate_invites)
    VALUES (NEW.id, v_clean_name, v_avatar_key, 'member', FALSE)
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO public.privacy_settings (user_id)
    VALUES (NEW.id)
    ON CONFLICT (user_id) DO NOTHING;

    RETURN NEW;
EXCEPTION WHEN unique_violation THEN
    INSERT INTO public.profiles (id, username, avatar_key, role, can_generate_invites)
    VALUES (NEW.id, 'traveler_' || left(replace(NEW.id::text, '-', ''), 12), v_avatar_key, 'member', FALSE)
    ON CONFLICT (id) DO NOTHING;
    INSERT INTO public.privacy_settings (user_id) VALUES (NEW.id) ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();

-- ----------------------------------------------------------------------------
-- 4. SECURITY DEFINER HELPERS FOR RLS (No infinite recursion)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.is_active_member(p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.community_members cm
        WHERE cm.user_id = p_user_id AND cm.status = 'active'
    );
$$;

CREATE OR REPLACE FUNCTION public.is_community_admin(p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.community_members cm
        JOIN public.profiles p ON p.id = cm.user_id
        WHERE cm.user_id = p_user_id
          AND cm.status = 'active'
          AND (cm.role = 'admin' OR p.role = 'admin')
    );
$$;

CREATE OR REPLACE FUNCTION public.can_invite(p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.community_members cm
        JOIN public.profiles p ON p.id = cm.user_id
        WHERE cm.user_id = p_user_id
          AND cm.status = 'active'
          AND (cm.role = 'admin' OR p.role = 'admin' OR p.can_generate_invites = TRUE)
    );
$$;

CREATE OR REPLACE FUNCTION public.can_view_user_domain(p_owner_id UUID, p_domain TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_viewer_id UUID := auth.uid();
    v_visibility TEXT;
BEGIN
    IF v_viewer_id IS NULL THEN
        RETURN FALSE;
    END IF;

    IF v_viewer_id = p_owner_id THEN
        RETURN TRUE;
    END IF;

    -- Both viewer and owner must be active community members
    IF NOT public.is_active_member(v_viewer_id) OR NOT public.is_active_member(p_owner_id) THEN
        RETURN FALSE;
    END IF;

    SELECT CASE p_domain
        WHEN 'watchlist' THEN ps.watchlist_visibility
        WHEN 'reviews' THEN ps.reviews_visibility
        WHEN 'collections' THEN ps.collections_visibility
        WHEN 'activity' THEN ps.activity_visibility
        ELSE 'private'
    END
    INTO v_visibility
    FROM public.privacy_settings ps
    WHERE ps.user_id = p_owner_id;

    RETURN COALESCE(v_visibility, 'private') = 'community';
END;
$$;

-- ----------------------------------------------------------------------------
-- 5. ATOMIC INVITATION & 50-MEMBER CAPACITY RPCs
-- ----------------------------------------------------------------------------

-- 5.1 Validate invitation before/during registration
CREATE OR REPLACE FUNCTION public.validate_invitation_code(p_code TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_invite RECORD;
    v_active_count INTEGER;
    v_clean_code TEXT := upper(trim(COALESCE(p_code, '')));
BEGIN
    SELECT COUNT(*) INTO v_active_count
    FROM public.community_members
    WHERE status = 'active';

    IF v_active_count >= 50 THEN
        RETURN jsonb_build_object(
            'valid', FALSE,
            'reason', 'Community is currently at full 50/50 member capacity.',
            'active_count', v_active_count,
            'max_capacity', 50
        );
    END IF;

    SELECT * INTO v_invite
    FROM public.invitations
    WHERE upper(code) = v_clean_code
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN jsonb_build_object(
            'valid', FALSE,
            'reason', 'Invalid invitation code.',
            'active_count', v_active_count,
            'max_capacity', 50
        );
    END IF;

    IF NOT v_invite.is_active THEN
        RETURN jsonb_build_object(
            'valid', FALSE,
            'reason', 'This invitation code has been deactivated.',
            'active_count', v_active_count,
            'max_capacity', 50
        );
    END IF;

    IF v_invite.expires_at <= NOW() THEN
        RETURN jsonb_build_object(
            'valid', FALSE,
            'reason', 'This invitation code has expired.',
            'active_count', v_active_count,
            'max_capacity', 50
        );
    END IF;

    IF v_invite.uses_count >= v_invite.max_uses THEN
        RETURN jsonb_build_object(
            'valid', FALSE,
            'reason', 'This invitation code has reached its usage limit.',
            'active_count', v_active_count,
            'max_capacity', 50
        );
    END IF;

    RETURN jsonb_build_object(
        'valid', TRUE,
        'reason', 'Valid invitation code.',
        'active_count', v_active_count,
        'max_capacity', 50
    );
END;
$$;

-- 5.2 Join community (supports public sign-up OR optional invite code; prevents removed users from rejoining)
CREATE OR REPLACE FUNCTION public.join_community(
    p_username TEXT,
    p_avatar_key TEXT DEFAULT 'avatar_1',
    p_code TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_clean_code TEXT := upper(trim(COALESCE(p_code, '')));
    v_clean_username TEXT := trim(COALESCE(p_username, ''));
    v_invite RECORD;
    v_active_count INTEGER;
    v_has_admin BOOLEAN;
    v_existing_member RECORD;
    v_assigned_role TEXT := 'member';
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    IF char_length(v_clean_username) < 2 OR char_length(v_clean_username) > 32 THEN
        RAISE EXCEPTION 'Username must be between 2 and 32 characters';
    END IF;

    PERFORM pg_advisory_xact_lock(92837465);

    SELECT * INTO v_existing_member
    FROM public.community_members
    WHERE user_id = v_user_id;

    IF FOUND AND v_existing_member.status = 'removed' THEN
        RAISE EXCEPTION 'Your account has been removed from this community';
    END IF;

    IF FOUND AND v_existing_member.status = 'active' THEN
        RETURN jsonb_build_object('joined', TRUE, 'status', 'active', 'already_member', TRUE);
    END IF;

    SELECT COUNT(*) INTO v_active_count
    FROM public.community_members
    WHERE status = 'active';

    IF v_clean_code <> '' THEN
        SELECT * INTO v_invite
        FROM public.invitations
        WHERE upper(code) = v_clean_code
        FOR UPDATE;

        IF NOT FOUND OR NOT v_invite.is_active THEN
            RAISE EXCEPTION 'Invalid or inactive invitation code';
        END IF;

        IF v_invite.expires_at <= NOW() THEN
            RAISE EXCEPTION 'Invitation code has expired';
        END IF;

        IF v_invite.uses_count >= v_invite.max_uses THEN
            RAISE EXCEPTION 'Invitation code usage limit reached';
        END IF;

        UPDATE public.invitations
        SET uses_count = uses_count + 1,
            is_active = CASE WHEN (uses_count + 1) >= max_uses THEN FALSE ELSE TRUE END
        WHERE id = v_invite.id;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM public.community_members WHERE status = 'active' AND role = 'admin'
    ) INTO v_has_admin;

    IF NOT v_has_admin THEN
        v_assigned_role := 'admin';
    END IF;

    INSERT INTO public.profiles (id, username, avatar_key, role, can_generate_invites)
    VALUES (
        v_user_id,
        v_clean_username,
        COALESCE(NULLIF(trim(p_avatar_key), ''), 'avatar_1'),
        v_assigned_role,
        (v_assigned_role = 'admin')
    )
    ON CONFLICT (id) DO UPDATE
        SET username = EXCLUDED.username,
            avatar_key = EXCLUDED.avatar_key,
            updated_at = NOW();

    INSERT INTO public.community_members (user_id, status, role)
    VALUES (v_user_id, 'active', v_assigned_role)
    ON CONFLICT (user_id) DO UPDATE
        SET status = 'active',
            joined_at = NOW(),
            updated_at = NOW();

    INSERT INTO public.privacy_settings (
        user_id,
        watchlist_visibility,
        reviews_visibility,
        collections_visibility,
        activity_visibility
    )
    VALUES (v_user_id, 'private', 'private', 'private', 'private')
    ON CONFLICT (user_id) DO NOTHING;

    RETURN jsonb_build_object(
        'joined', TRUE,
        'status', 'active',
        'role', v_assigned_role,
        'active_count', v_active_count + 1
    );
END;
$$;

-- Backward-compatible wrapper if older clients call redeem_invitation_and_join
CREATE OR REPLACE FUNCTION public.redeem_invitation_and_join(
    p_code TEXT,
    p_username TEXT,
    p_avatar_key TEXT DEFAULT 'avatar_1'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN public.join_community(p_username, p_avatar_key, p_code);
END;
$$;

-- 5.3 Create invitation code (admin or authorized invite creator only)
CREATE OR REPLACE FUNCTION public.create_invitation_code(
    p_max_uses INTEGER DEFAULT 1,
    p_expires_in_hours INTEGER DEFAULT 168,
    p_custom_code TEXT DEFAULT NULL
)
RETURNS public.invitations
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_code TEXT;
    v_row public.invitations;
BEGIN
    IF v_user_id IS NULL OR NOT public.can_invite(v_user_id) THEN
        RAISE EXCEPTION 'Only community admins or authorized invite creators can generate invitation codes';
    END IF;

    IF p_max_uses < 1 OR p_max_uses > 50 THEN
        RAISE EXCEPTION 'max_uses must be between 1 and 50';
    END IF;

    v_code := COALESCE(
        NULLIF(upper(trim(p_custom_code)), ''),
        'AU-' || upper(substring(encode(gen_random_bytes(5), 'hex') from 1 for 8))
    );

    INSERT INTO public.invitations (code, created_by, max_uses, uses_count, expires_at, is_active)
    VALUES (
        v_code,
        v_user_id,
        p_max_uses,
        0,
        NOW() + make_interval(hours => GREATEST(1, LEAST(p_expires_in_hours, 8760))),
        TRUE
    )
    RETURNING * INTO v_row;

    RETURN v_row;
END;
$$;

-- 5.4 Leave community cleanly
CREATE OR REPLACE FUNCTION public.leave_community()
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

    UPDATE public.community_members
    SET status = 'left',
        updated_at = NOW()
    WHERE user_id = v_user_id;

    UPDATE public.privacy_settings
    SET watchlist_visibility = 'private',
        reviews_visibility = 'private',
        collections_visibility = 'private',
        activity_visibility = 'private',
        updated_at = NOW()
    WHERE user_id = v_user_id;

    RETURN jsonb_build_object('status', 'left');
END;
$$;

-- 5.5 Community capacity counter
CREATE OR REPLACE FUNCTION public.get_community_capacity()
RETURNS JSONB
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT jsonb_build_object(
        'active_count', (SELECT COUNT(*) FROM public.community_members WHERE status = 'active'),
        'max_capacity', 50
    );
$$;

-- 5.6 Case-insensitive username availability check (active profiles + 24-hour deletion cooldown)
CREATE OR REPLACE FUNCTION public.is_username_available(p_username TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_clean TEXT := trim(COALESCE(p_username, ''));
    v_caller UUID := auth.uid();
BEGIN
    IF char_length(v_clean) < 2 OR char_length(v_clean) > 32 THEN
        RETURN FALSE;
    END IF;
    IF EXISTS (
        SELECT 1
        FROM public.profiles p
        WHERE lower(trim(p.username)) = lower(v_clean)
          AND (v_caller IS NULL OR p.id <> v_caller)
    ) THEN
        RETURN FALSE;
    END IF;
    IF EXISTS (
        SELECT 1
        FROM public.deleted_account_cooldowns d
        WHERE d.username_lower = lower(v_clean)
          AND d.username_available_at > NOW()
    ) THEN
        RETURN FALSE;
    END IF;
    RETURN TRUE;
END;
$$;

-- 5.6b Detailed username status (distinguishes taken vs 24-hour post-deletion cooldown)
CREATE OR REPLACE FUNCTION public.check_username_status(p_username TEXT)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_clean TEXT := trim(COALESCE(p_username, ''));
    v_caller UUID := auth.uid();
    v_cooldown_until TIMESTAMPTZ;
BEGIN
    IF char_length(v_clean) < 2 OR char_length(v_clean) > 32 THEN
        RETURN jsonb_build_object('available', FALSE, 'cooldown_seconds', 0, 'reason', 'Username must be between 2 and 32 characters.');
    END IF;
    IF EXISTS (
        SELECT 1
        FROM public.profiles p
        WHERE lower(trim(p.username)) = lower(v_clean)
          AND (v_caller IS NULL OR p.id <> v_caller)
    ) THEN
        RETURN jsonb_build_object('available', FALSE, 'cooldown_seconds', 0, 'reason', 'That username is already taken.');
    END IF;
    SELECT MAX(d.username_available_at) INTO v_cooldown_until
    FROM public.deleted_account_cooldowns d
    WHERE d.username_lower = lower(v_clean)
      AND d.username_available_at > NOW();
    IF v_cooldown_until IS NOT NULL THEN
        RETURN jsonb_build_object(
            'available', FALSE,
            'cooldown_seconds', GREATEST(1, CEIL(EXTRACT(EPOCH FROM (v_cooldown_until - NOW())))::BIGINT),
            'reason', 'That username was recently deleted and will be available again after 24 hours.'
        );
    END IF;
    RETURN jsonb_build_object('available', TRUE, 'cooldown_seconds', 0, 'reason', '');
END;
$$;

-- 5.7 Resolve email from username or email for dual sign-in & password reset
CREATE OR REPLACE FUNCTION public.resolve_login_email(p_identifier TEXT)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
    v_clean TEXT := trim(leading '@' FROM trim(COALESCE(p_identifier, '')));
    v_email TEXT;
BEGIN
    IF v_clean = '' THEN
        RETURN NULL;
    END IF;
    IF position('@' IN v_clean) > 1 THEN
        RETURN lower(v_clean);
    END IF;
    SELECT u.email INTO v_email
    FROM public.profiles p
    JOIN auth.users u ON u.id = p.id
    WHERE lower(trim(p.username)) = lower(v_clean)
    LIMIT 1;
    RETURN v_email;
END;
$$;

-- 5.8 Account Deletion Cooldowns & Full Database Purge
CREATE TABLE IF NOT EXISTS public.deleted_account_cooldowns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_hash TEXT NOT NULL,
    email_available_at TIMESTAMPTZ NOT NULL,
    username_lower TEXT,
    username_available_at TIMESTAMPTZ NOT NULL,
    deleted_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_deleted_cooldowns_email_hash
    ON public.deleted_account_cooldowns(email_hash, email_available_at DESC);
CREATE INDEX IF NOT EXISTS idx_deleted_cooldowns_username
    ON public.deleted_account_cooldowns(username_lower, username_available_at DESC);

ALTER TABLE public.deleted_account_cooldowns ENABLE ROW LEVEL SECURITY;

-- 5.9 Check if an email has cleared its 5-minute post-deletion cooldown
CREATE OR REPLACE FUNCTION public.can_register_email(p_email TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions
AS $$
DECLARE
    v_clean TEXT := lower(trim(COALESCE(p_email, '')));
    v_hash TEXT;
    v_until TIMESTAMPTZ;
    v_remaining BIGINT;
BEGIN
    IF v_clean = '' OR position('@' IN v_clean) < 2 THEN
        RETURN jsonb_build_object('allowed', FALSE, 'retry_after_seconds', 0);
    END IF;

    -- Purge expired cooldown records to minimize storage
    DELETE FROM public.deleted_account_cooldowns
    WHERE email_available_at <= NOW() AND username_available_at <= NOW();

    v_hash := encode(digest(v_clean, 'sha256'), 'hex');
    SELECT MAX(email_available_at) INTO v_until
    FROM public.deleted_account_cooldowns
    WHERE email_hash = v_hash AND email_available_at > NOW();

    IF v_until IS NOT NULL THEN
        v_remaining := GREATEST(1, CEIL(EXTRACT(EPOCH FROM (v_until - NOW())))::BIGINT);
        RETURN jsonb_build_object('allowed', FALSE, 'retry_after_seconds', v_remaining);
    END IF;

    RETURN jsonb_build_object('allowed', TRUE, 'retry_after_seconds', 0);
END;
$$;

-- 5.10 Complete Account & User Data Deletion (records 5-min email & 24-hr username cooldowns, purges storage, user tables, and auth.users)
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

    -- 1. Delete user avatar objects from storage.objects ('avatars' bucket)
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
    IF to_regclass('public.user_push_tokens') IS NOT NULL THEN
        EXECUTE 'DELETE FROM public.user_push_tokens WHERE user_id = $1' USING v_user_id;
    END IF;
    UPDATE public.invitations SET created_by = NULL WHERE created_by = v_user_id;
    DELETE FROM public.profiles WHERE id = v_user_id;

    -- 3. Permanently delete the user from Supabase Auth (auth.users cascades to identities, sessions, refresh_tokens)
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

-- 5.11 Remote App Releases & Push Notification Tokens
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
-- 6. ROW-LEVEL SECURITY (RLS) POLICIES
-- ----------------------------------------------------------------------------
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.anime ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_anime ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.collections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.collection_anime ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.community_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.privacy_settings ENABLE ROW LEVEL SECURITY;

-- 6.1 profiles (personal account self-CRUD + active-member community visibility)
DROP POLICY IF EXISTS profiles_select_policy ON public.profiles;
CREATE POLICY profiles_select_policy ON public.profiles
    FOR SELECT USING (
        auth.uid() = id
        OR (public.is_active_member(auth.uid()) AND public.is_active_member(id))
    );

DROP POLICY IF EXISTS profiles_insert_self ON public.profiles;
CREATE POLICY profiles_insert_self ON public.profiles
    FOR INSERT WITH CHECK (
        auth.uid() = id
        AND role = 'member'
        AND can_generate_invites = FALSE
    );

DROP POLICY IF EXISTS profiles_update_self ON public.profiles;
CREATE POLICY profiles_update_self ON public.profiles
    FOR UPDATE USING (auth.uid() = id)
    WITH CHECK (
        auth.uid() = id
        -- Prevent privilege self-escalation
        AND role = (SELECT role FROM public.profiles WHERE id = auth.uid())
        AND can_generate_invites = (SELECT can_generate_invites FROM public.profiles WHERE id = auth.uid())
    );

-- 6.2 anime (shared public AniList metadata cache, any signed-in user)
DROP POLICY IF EXISTS anime_select_authenticated ON public.anime;
CREATE POLICY anime_select_authenticated ON public.anime
    FOR SELECT USING (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS anime_insert_active_member ON public.anime;
CREATE POLICY anime_insert_active_member ON public.anime
    FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS anime_update_active_member ON public.anime;
CREATE POLICY anime_update_active_member ON public.anime
    FOR UPDATE USING (auth.uid() IS NOT NULL);

-- 6.3 user_anime (owner CRUD for any signed-in user + community-gated SELECT of friends' ratings/notes)
DROP POLICY IF EXISTS user_anime_select_policy ON public.user_anime;
CREATE POLICY user_anime_select_policy ON public.user_anime
    FOR SELECT USING (
        auth.uid() = user_id
        OR (
            public.is_active_member(auth.uid())
            AND public.is_active_member(user_id)
            AND (
                public.can_view_user_domain(user_id, 'watchlist')
                OR public.can_view_user_domain(user_id, 'reviews')
                OR public.can_view_user_domain(user_id, 'activity')
            )
        )
    );

DROP POLICY IF EXISTS user_anime_insert_own ON public.user_anime;
CREATE POLICY user_anime_insert_own ON public.user_anime
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS user_anime_update_own ON public.user_anime;
CREATE POLICY user_anime_update_own ON public.user_anime
    FOR UPDATE USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS user_anime_delete_own ON public.user_anime;
CREATE POLICY user_anime_delete_own ON public.user_anime
    FOR DELETE USING (auth.uid() = user_id);

-- 6.4 collections (owner CRUD + community visibility)
DROP POLICY IF EXISTS collections_select_policy ON public.collections;
CREATE POLICY collections_select_policy ON public.collections
    FOR SELECT USING (
        auth.uid() = creator_id
        OR (
            visibility = 'community'
            AND public.can_view_user_domain(creator_id, 'collections')
        )
    );

DROP POLICY IF EXISTS collections_insert_own ON public.collections;
CREATE POLICY collections_insert_own ON public.collections
    FOR INSERT WITH CHECK (auth.uid() = creator_id);

DROP POLICY IF EXISTS collections_update_own ON public.collections;
CREATE POLICY collections_update_own ON public.collections
    FOR UPDATE USING (auth.uid() = creator_id)
    WITH CHECK (auth.uid() = creator_id);

DROP POLICY IF EXISTS collections_delete_own ON public.collections;
CREATE POLICY collections_delete_own ON public.collections
    FOR DELETE USING (auth.uid() = creator_id);

-- 6.5 collection_anime (inherits parent collection permissions)
DROP POLICY IF EXISTS collection_anime_select_policy ON public.collection_anime;
CREATE POLICY collection_anime_select_policy ON public.collection_anime
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.collections c
            WHERE c.id = collection_id
              AND (
                  c.creator_id = auth.uid()
                  OR (c.visibility = 'community' AND public.can_view_user_domain(c.creator_id, 'collections'))
              )
        )
    );

DROP POLICY IF EXISTS collection_anime_modify_own ON public.collection_anime;
CREATE POLICY collection_anime_modify_own ON public.collection_anime
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM public.collections c
            WHERE c.id = collection_id AND c.creator_id = auth.uid()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.collections c
            WHERE c.id = collection_id AND c.creator_id = auth.uid()
        )
    );

-- 6.6 community_members
DROP POLICY IF EXISTS community_members_select_policy ON public.community_members;
CREATE POLICY community_members_select_policy ON public.community_members
    FOR SELECT USING (
        auth.uid() = user_id
        OR (public.is_active_member(auth.uid()) AND status = 'active')
    );

DROP POLICY IF EXISTS community_members_admin_update ON public.community_members;
CREATE POLICY community_members_admin_update ON public.community_members
    FOR UPDATE USING (public.is_community_admin(auth.uid()));

-- 6.7 invitations (strictly admin or invite creator authorized)
DROP POLICY IF EXISTS invitations_select_admin ON public.invitations;
CREATE POLICY invitations_select_admin ON public.invitations
    FOR SELECT USING (public.can_invite(auth.uid()));

DROP POLICY IF EXISTS invitations_insert_admin ON public.invitations;
CREATE POLICY invitations_insert_admin ON public.invitations
    FOR INSERT WITH CHECK (public.can_invite(auth.uid()) AND created_by = auth.uid());

DROP POLICY IF EXISTS invitations_update_admin ON public.invitations;
CREATE POLICY invitations_update_admin ON public.invitations
    FOR UPDATE USING (public.can_invite(auth.uid()));

-- 6.8 privacy_settings
DROP POLICY IF EXISTS privacy_settings_select_policy ON public.privacy_settings;
CREATE POLICY privacy_settings_select_policy ON public.privacy_settings
    FOR SELECT USING (
        auth.uid() = user_id
        OR (public.is_active_member(auth.uid()) AND public.is_active_member(user_id))
    );

DROP POLICY IF EXISTS privacy_settings_insert_own ON public.privacy_settings;
CREATE POLICY privacy_settings_insert_own ON public.privacy_settings
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS privacy_settings_update_own ON public.privacy_settings;
CREATE POLICY privacy_settings_update_own ON public.privacy_settings
    FOR UPDATE USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- 6.9 EXPLICIT RPC EXECUTION PERMISSIONS
-- ----------------------------------------------------------------------------
REVOKE ALL ON FUNCTION public.validate_invitation_code(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.validate_invitation_code(TEXT) TO anon, authenticated;

REVOKE ALL ON FUNCTION public.join_community(TEXT, TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.join_community(TEXT, TEXT, TEXT) TO authenticated;

REVOKE ALL ON FUNCTION public.redeem_invitation_and_join(TEXT, TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.redeem_invitation_and_join(TEXT, TEXT, TEXT) TO authenticated;

REVOKE ALL ON FUNCTION public.create_invitation_code(INTEGER, INTEGER, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.create_invitation_code(INTEGER, INTEGER, TEXT) TO authenticated;

REVOKE ALL ON FUNCTION public.leave_community() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.leave_community() TO authenticated;

REVOKE ALL ON FUNCTION public.get_community_capacity() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_community_capacity() TO authenticated;

REVOKE ALL ON FUNCTION public.is_username_available(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_username_available(TEXT) TO anon, authenticated;

REVOKE ALL ON FUNCTION public.check_username_status(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.check_username_status(TEXT) TO anon, authenticated;

REVOKE ALL ON FUNCTION public.resolve_login_email(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.resolve_login_email(TEXT) TO anon, authenticated;

REVOKE ALL ON FUNCTION public.can_register_email(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.can_register_email(TEXT) TO anon, authenticated;

REVOKE ALL ON FUNCTION public.delete_my_account() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.delete_my_account() TO authenticated;

-- ----------------------------------------------------------------------------
-- 7. SUPABASE STORAGE BUCKET & POLICIES FOR CUSTOM AVATARS
-- ----------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'avatars',
    'avatars',
    TRUE,
    2097152, -- 2 MB
    ARRAY['image/jpeg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO UPDATE
    SET file_size_limit = EXCLUDED.file_size_limit,
        allowed_mime_types = EXCLUDED.allowed_mime_types;

DROP POLICY IF EXISTS avatars_public_read ON storage.objects;
CREATE POLICY avatars_public_read ON storage.objects
    FOR SELECT USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS avatars_owner_upload ON storage.objects;
CREATE POLICY avatars_owner_upload ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'avatars'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS avatars_owner_update ON storage.objects;
CREATE POLICY avatars_owner_update ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'avatars'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS avatars_owner_delete ON storage.objects;
CREATE POLICY avatars_owner_delete ON storage.objects
    FOR DELETE USING (
        bucket_id = 'avatars'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- Backfill profiles & privacy settings for any Auth users created before running this schema
INSERT INTO public.profiles (id, username, avatar_key, role, can_generate_invites)
SELECT
    u.id,
    left(
        CASE
            WHEN char_length(trim(COALESCE(u.raw_user_meta_data->>'username', split_part(COALESCE(u.email, ''), '@', 1)))) >= 2
            THEN trim(COALESCE(u.raw_user_meta_data->>'username', split_part(COALESCE(u.email, ''), '@', 1)))
            ELSE 'Member_' || substring(u.id::text from 1 for 4)
        END,
        32
    ),
    COALESCE(NULLIF(trim(u.raw_user_meta_data->>'avatar_key'), ''), 'avatar_1'),
    'member',
    FALSE
FROM auth.users u
WHERE NOT EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = u.id)
ON CONFLICT DO NOTHING;

INSERT INTO public.privacy_settings (user_id)
SELECT u.id FROM auth.users u
WHERE NOT EXISTS (SELECT 1 FROM public.privacy_settings ps WHERE ps.user_id = u.id)
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 8. REVIEW LIKES & 500 MB STORAGE MONITORING (v2.2)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.review_likes (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    review_id UUID NOT NULL REFERENCES public.user_anime(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, review_id)
);

CREATE INDEX IF NOT EXISTS idx_review_likes_review_id
    ON public.review_likes(review_id);

CREATE INDEX IF NOT EXISTS idx_user_anime_rated_noted_feed
    ON public.user_anime (updated_at DESC)
    WHERE personal_rating > 0 AND review_notes <> '';

ALTER TABLE public.review_likes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS review_likes_select_policy ON public.review_likes;
CREATE POLICY review_likes_select_policy ON public.review_likes
    FOR SELECT USING (
        auth.uid() IS NOT NULL
        AND (auth.uid() = user_id OR public.is_active_member(auth.uid()))
    );

DROP POLICY IF EXISTS review_likes_insert_own ON public.review_likes;
CREATE POLICY review_likes_insert_own ON public.review_likes
    FOR INSERT WITH CHECK (auth.uid() = user_id AND public.is_active_member(auth.uid()));

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
    SELECT COUNT(*)::INTEGER INTO v_count FROM public.review_likes WHERE review_id = p_review_id;
    RETURN jsonb_build_object('review_id', p_review_id, 'liked', v_liked, 'likes_count', v_count);
END;
$$;

REVOKE ALL ON FUNCTION public.toggle_review_like(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.toggle_review_like(UUID) TO authenticated;

CREATE OR REPLACE VIEW public.community_anime_stats_view
WITH (security_invoker = true) AS
SELECT
    ua.anime_id,
    COUNT(ua.id)::INTEGER AS tracking_count,
    COUNT(ua.personal_rating)::INTEGER AS rating_count,
    ROUND(AVG(ua.personal_rating), 2) AS avg_rating_10,
    COUNT(CASE WHEN ua.personal_rating > 0 AND length(trim(COALESCE(ua.review_notes, ''))) > 0 THEN 1 END)::INTEGER AS review_count,
    MAX(ua.updated_at) AS last_reviewed_at
FROM public.user_anime ua
GROUP BY ua.anime_id;

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
    FROM public.user_anime
    WHERE personal_rating > 0 AND length(trim(COALESCE(review_notes, ''))) > 0;
    SELECT COALESCE(SUM(pg_total_relation_size(c.oid)), 0)
    INTO v_user_data_bytes
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind = 'r'
      AND c.relname IN ('profiles', 'user_anime', 'collections', 'collection_anime', 'review_likes', 'community_members', 'privacy_settings');
    SELECT COALESCE(pg_total_relation_size('public.user_anime'::regclass), 0) INTO v_review_table_bytes;
    v_bytes_per_user := GREATEST(2048, (v_user_data_bytes / GREATEST(v_user_count, 1)));
    v_bytes_per_review := CASE WHEN v_entry_count > 0 THEN GREATEST(256, v_review_table_bytes / v_entry_count) ELSE 380 END;
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

-- ----------------------------------------------------------------------------
-- 9. INDEPENDENT HOME, LIBRARY & COMMUNITY ARCHITECTURE (v2.3)
-- ----------------------------------------------------------------------------
ALTER TABLE public.user_anime
    ADD COLUMN IF NOT EXISTS home_removed_at TIMESTAMPTZ NULL;

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
        OR (public.is_active_member(auth.uid()) AND public.is_active_member(user_id))
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
    FOR INSERT WITH CHECK (auth.uid() = user_id AND public.is_active_member(auth.uid()));
DROP POLICY IF EXISTS community_note_likes_delete_own ON public.community_note_likes;
CREATE POLICY community_note_likes_delete_own ON public.community_note_likes
    FOR DELETE USING (auth.uid() = user_id);

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
    SELECT a.status, a.episodes INTO v_anime_status, v_anime_episodes
    FROM public.anime a WHERE a.anilist_id = NEW.anime_id;
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
    IF v_official_finished AND v_effective_total IS NOT NULL AND v_effective_total > 0
       AND NEW.episode_progress >= v_effective_total AND NEW.status <> 'dropped' THEN
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

CREATE OR REPLACE FUNCTION public.handle_user_anime_home_lifecycle()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF NEW.status IN ('dropped', 'completed') THEN
        DELETE FROM public.home_tracking
        WHERE user_id = NEW.user_id AND anime_id = NEW.anime_id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_user_anime_home_lifecycle ON public.user_anime;
CREATE TRIGGER trg_user_anime_home_lifecycle
    AFTER INSERT OR UPDATE OF status, episode_progress, total_episodes ON public.user_anime
    FOR EACH ROW EXECUTE FUNCTION public.handle_user_anime_home_lifecycle();

CREATE OR REPLACE FUNCTION public.add_to_home(p_anime_id INTEGER, p_airing_day SMALLINT DEFAULT NULL)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_existing public.user_anime%ROWTYPE;
BEGIN
    IF v_user_id IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    SELECT * INTO v_existing FROM public.user_anime WHERE user_id = v_user_id AND anime_id = p_anime_id;
    IF NOT FOUND THEN
        INSERT INTO public.user_anime (user_id, anime_id, status, episode_progress, home_removed_at)
        VALUES (v_user_id, p_anime_id, 'watching', 0, NULL)
        ON CONFLICT (user_id, anime_id) DO UPDATE SET home_removed_at = NULL, updated_at = NOW();
    ELSE
        UPDATE public.user_anime
        SET home_removed_at = NULL,
            status = CASE WHEN status IN ('plan_to_watch', 'dropped') THEN 'watching' ELSE status END,
            updated_at = NOW()
        WHERE user_id = v_user_id AND anime_id = p_anime_id;
    END IF;
    INSERT INTO public.home_tracking (user_id, anime_id, airing_day_of_week)
    VALUES (v_user_id, p_anime_id, CASE WHEN p_airing_day BETWEEN 1 AND 7 THEN p_airing_day ELSE NULL END)
    ON CONFLICT (user_id, anime_id) DO UPDATE
        SET airing_day_of_week = COALESCE(EXCLUDED.airing_day_of_week, public.home_tracking.airing_day_of_week),
            updated_at = NOW();
    RETURN jsonb_build_object('in_home', TRUE, 'anime_id', p_anime_id);
END;
$$;

REVOKE ALL ON FUNCTION public.add_to_home(INTEGER, SMALLINT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.add_to_home(INTEGER, SMALLINT) TO authenticated;

CREATE OR REPLACE FUNCTION public.remove_from_home(p_anime_id INTEGER)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID := auth.uid();
BEGIN
    IF v_user_id IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    DELETE FROM public.home_tracking WHERE user_id = v_user_id AND anime_id = p_anime_id;
    UPDATE public.user_anime SET home_removed_at = NOW(), updated_at = NOW()
    WHERE user_id = v_user_id AND anime_id = p_anime_id;
    RETURN jsonb_build_object('removed_from_home', TRUE, 'anime_id', p_anime_id, 'library_preserved', TRUE);
END;
$$;

REVOKE ALL ON FUNCTION public.remove_from_home(INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.remove_from_home(INTEGER) TO authenticated;

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
    IF v_user_id IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    IF p_rating IS NOT NULL THEN
        IF p_rating < 0 OR p_rating > 10 THEN RAISE EXCEPTION 'Rating must be between 0 and 10'; END IF;
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
    UPDATE public.user_anime
    SET personal_rating = COALESCE(v_clean_rating, personal_rating),
        review_notes = CASE WHEN v_clean_note <> '' THEN v_clean_note ELSE review_notes END,
        updated_at = NOW()
    WHERE user_id = v_user_id AND anime_id = p_anime_id;
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
    IF v_user_id IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    IF p_watched_episodes IS NULL OR p_watched_episodes < 0 THEN
        RAISE EXCEPTION 'Episode count must be a non-negative whole number';
    END IF;
    SELECT a.status, a.episodes INTO v_anime_status, v_anime_episodes
    FROM public.anime a WHERE a.anilist_id = p_anime_id;
    SELECT * INTO v_existing FROM public.user_anime
    WHERE user_id = v_user_id AND anime_id = p_anime_id;
    v_known_total := COALESCE(NULLIF(v_existing.total_episodes, 0), NULLIF(v_anime_episodes, 0));
    IF v_known_total IS NOT NULL AND v_known_total > 0 AND p_watched_episodes > v_known_total THEN
        RAISE EXCEPTION 'Episode count (%) cannot exceed total episodes (%)', p_watched_episodes, v_known_total;
    END IF;
    v_official_finished := UPPER(COALESCE(v_anime_status, '')) IN ('FINISHED', 'FINISHED_AIRING');
    v_auto_completed := v_official_finished AND v_known_total IS NOT NULL AND v_known_total > 0 AND p_watched_episodes >= v_known_total;
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
        DELETE FROM public.home_tracking WHERE user_id = v_user_id AND anime_id = p_anime_id;
    END IF;
    SELECT EXISTS (SELECT 1 FROM public.home_tracking WHERE user_id = v_user_id AND anime_id = p_anime_id) INTO v_in_home;
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
    IF v_user_id IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    IF EXISTS (SELECT 1 FROM public.community_note_likes WHERE user_id = v_user_id AND note_id = p_note_id) THEN
        DELETE FROM public.community_note_likes WHERE user_id = v_user_id AND note_id = p_note_id;
        v_liked := FALSE;
    ELSE
        INSERT INTO public.community_note_likes (user_id, note_id) VALUES (v_user_id, p_note_id)
        ON CONFLICT (user_id, note_id) DO NOTHING;
        v_liked := TRUE;
    END IF;
    SELECT COUNT(*)::INTEGER INTO v_count FROM public.community_note_likes WHERE note_id = p_note_id;
    RETURN jsonb_build_object('note_id', p_note_id, 'liked', v_liked, 'likes_count', v_count);
END;
$$;

REVOKE ALL ON FUNCTION public.toggle_community_note_like(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.toggle_community_note_like(UUID) TO authenticated;

-- ----------------------------------------------------------------------------
-- 10. COMPLETE TAURI 2 NATIVE & REMOTE PUSH NOTIFICATION SYSTEM (v2.4)
-- Tables: user_devices, user_notification_settings, notifications
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.set_current_timestamp_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TABLE IF NOT EXISTS public.user_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL CHECK (char_length(trim(device_id)) >= 4 AND char_length(device_id) <= 255),
    platform TEXT NOT NULL CHECK (platform IN ('android', 'windows', 'macos', 'web')),
    push_token TEXT,
    push_provider TEXT NOT NULL DEFAULT 'tauri_local' CHECK (push_provider IN ('fcm', 'apns', 'wns', 'webpush', 'tauri_local')),
    permission_status TEXT NOT NULL DEFAULT 'prompt' CHECK (permission_status IN ('granted', 'denied', 'prompt', 'unsupported')),
    device_name TEXT,
    app_version TEXT DEFAULT '2.0.0',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_error TEXT,
    invalidated_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT user_devices_user_device_unique UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_user_devices_user_id ON public.user_devices (user_id);
CREATE INDEX IF NOT EXISTS idx_user_devices_active_delivery ON public.user_devices (user_id, is_active, permission_status) WHERE is_active = TRUE AND permission_status = 'granted';
CREATE INDEX IF NOT EXISTS idx_user_devices_push_token ON public.user_devices (push_token) WHERE push_token IS NOT NULL;

DROP TRIGGER IF EXISTS trg_user_devices_updated_at ON public.user_devices;
CREATE TRIGGER trg_user_devices_updated_at
    BEFORE UPDATE ON public.user_devices
    FOR EACH ROW EXECUTE FUNCTION public.set_current_timestamp_updated_at();

ALTER TABLE public.user_devices ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own devices" ON public.user_devices;
CREATE POLICY "Users can view own devices" ON public.user_devices FOR SELECT TO authenticated USING (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can insert own devices" ON public.user_devices;
CREATE POLICY "Users can insert own devices" ON public.user_devices FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can update own devices" ON public.user_devices;
CREATE POLICY "Users can update own devices" ON public.user_devices FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can delete own devices" ON public.user_devices;
CREATE POLICY "Users can delete own devices" ON public.user_devices FOR DELETE TO authenticated USING (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS public.user_notification_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    master_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    messages_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updates_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    replies_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    mentions_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    os_permission_status TEXT NOT NULL DEFAULT 'prompt' CHECK (os_permission_status IN ('granted', 'denied', 'prompt', 'unsupported')),
    onboarding_dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_user_notification_settings_updated_at ON public.user_notification_settings;
CREATE TRIGGER trg_user_notification_settings_updated_at
    BEFORE UPDATE ON public.user_notification_settings
    FOR EACH ROW EXECUTE FUNCTION public.set_current_timestamp_updated_at();

ALTER TABLE public.user_notification_settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own notification settings" ON public.user_notification_settings;
CREATE POLICY "Users can view own notification settings" ON public.user_notification_settings FOR SELECT TO authenticated USING (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can insert own notification settings" ON public.user_notification_settings;
CREATE POLICY "Users can insert own notification settings" ON public.user_notification_settings FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can update own notification settings" ON public.user_notification_settings;
CREATE POLICY "Users can update own notification settings" ON public.user_notification_settings FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can delete own notification settings" ON public.user_notification_settings;
CREATE POLICY "Users can delete own notification settings" ON public.user_notification_settings FOR DELETE TO authenticated USING (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    sender_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    title TEXT NOT NULL CHECK (char_length(trim(title)) > 0 AND char_length(title) <= 200),
    message TEXT NOT NULL CHECK (char_length(trim(message)) > 0 AND char_length(message) <= 2000),
    type TEXT NOT NULL DEFAULT 'message' CHECK (type IN ('message', 'update', 'reminder', 'reply', 'mention', 'system')),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    dedupe_key TEXT,
    legacy_notification_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON public.notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON public.notifications (user_id, is_read, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_user_dedupe ON public.notifications (user_id, dedupe_key) WHERE dedupe_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_legacy_id ON public.notifications (legacy_notification_id) WHERE legacy_notification_id IS NOT NULL;

DROP TRIGGER IF EXISTS trg_notifications_updated_at ON public.notifications;
CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE ON public.notifications
    FOR EACH ROW EXECUTE FUNCTION public.set_current_timestamp_updated_at();

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own notifications" ON public.notifications;
CREATE POLICY "Users can view own notifications" ON public.notifications FOR SELECT TO authenticated USING (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can insert own notifications" ON public.notifications;
CREATE POLICY "Users can insert own notifications" ON public.notifications FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id OR auth.uid() = sender_user_id);
DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
CREATE POLICY "Users can update own notifications" ON public.notifications FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can delete own notifications" ON public.notifications;
CREATE POLICY "Users can delete own notifications" ON public.notifications FOR DELETE TO authenticated USING (auth.uid() = user_id);

NOTIFY pgrst, 'reload schema';
