-- ============================================================================
-- MIGRATION: 20261126_complete_tauri_notification_system.sql
-- Complete Native & Remote Push Notification System for Tauri 2 + Supabase
-- Supports: Android APK (FCM), Windows EXE (WNS), macOS DMG (APNs), and Web
-- Tables:
--   1. public.user_devices
--   2. public.user_notification_settings
--   3. public.notifications (synchronized with public.user_notifications)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- 0. AUTOMATIC updated_at TRIGGER FUNCTION
-- ============================================================================
CREATE OR REPLACE FUNCTION public.set_current_timestamp_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

-- ============================================================================
-- 1. TABLE: public.user_devices
-- Stores per-user registered devices, platform, native push tokens, and OS permission status.
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.user_devices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  device_id TEXT NOT NULL CHECK (char_length(trim(device_id)) >= 4 AND char_length(device_id) <= 255),
  platform TEXT NOT NULL CHECK (platform IN ('android', 'windows', 'macos', 'web')),
  push_token TEXT,
  push_provider TEXT NOT NULL DEFAULT 'tauri_local' CHECK (push_provider IN ('fcm', 'apns', 'wns', 'webpush', 'tauri_local')),
  permission_status TEXT NOT NULL DEFAULT 'prompt' CHECK (permission_status IN ('granted', 'denied', 'prompt', 'unsupported')),
  device_name TEXT,
  os_version TEXT,
  app_version TEXT DEFAULT '2.4.0',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  last_error TEXT,
  invalidated_at TIMESTAMPTZ,
  last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT user_devices_user_device_unique UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_user_devices_user_id
  ON public.user_devices (user_id);

CREATE INDEX IF NOT EXISTS idx_user_devices_active_delivery
  ON public.user_devices (user_id, is_active, permission_status)
  WHERE is_active = TRUE AND permission_status = 'granted';

CREATE INDEX IF NOT EXISTS idx_user_devices_push_token
  ON public.user_devices (push_token)
  WHERE push_token IS NOT NULL;

DROP TRIGGER IF EXISTS trg_user_devices_updated_at ON public.user_devices;
CREATE TRIGGER trg_user_devices_updated_at
  BEFORE UPDATE ON public.user_devices
  FOR EACH ROW
  EXECUTE FUNCTION public.set_current_timestamp_updated_at();

ALTER TABLE public.user_devices ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own devices" ON public.user_devices;
CREATE POLICY "Users can view own devices"
  ON public.user_devices FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own devices" ON public.user_devices;
CREATE POLICY "Users can insert own devices"
  ON public.user_devices FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own devices" ON public.user_devices;
CREATE POLICY "Users can update own devices"
  ON public.user_devices FOR UPDATE
  TO authenticated
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own devices" ON public.user_devices;
CREATE POLICY "Users can delete own devices"
  ON public.user_devices FOR DELETE
  TO authenticated
  USING (auth.uid() = user_id);

-- ============================================================================
-- 2. TABLE: public.user_notification_settings
-- Stores master notification toggle, category preferences, and onboarding state.
-- ============================================================================
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

CREATE INDEX IF NOT EXISTS idx_user_notification_settings_user_id
  ON public.user_notification_settings (user_id);

-- Keep notifications_enabled and master_enabled synchronized if either is updated
CREATE OR REPLACE FUNCTION public.sync_user_notification_settings_columns()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    IF NEW.notifications_enabled IS DISTINCT FROM TRUE AND NEW.master_enabled = TRUE THEN
      NEW.master_enabled := NEW.notifications_enabled;
    ELSIF NEW.master_enabled IS DISTINCT FROM TRUE AND NEW.notifications_enabled = TRUE THEN
      NEW.notifications_enabled := NEW.master_enabled;
    END IF;
  ELSIF TG_OP = 'UPDATE' THEN
    IF NEW.notifications_enabled IS DISTINCT FROM OLD.notifications_enabled THEN
      NEW.master_enabled := NEW.notifications_enabled;
    ELSIF NEW.master_enabled IS DISTINCT FROM OLD.master_enabled THEN
      NEW.notifications_enabled := NEW.master_enabled;
    END IF;
  END IF;
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_user_notification_settings_updated_at ON public.user_notification_settings;
CREATE TRIGGER trg_user_notification_settings_updated_at
  BEFORE INSERT OR UPDATE ON public.user_notification_settings
  FOR EACH ROW
  EXECUTE FUNCTION public.sync_user_notification_settings_columns();

ALTER TABLE public.user_notification_settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own notification settings" ON public.user_notification_settings;
CREATE POLICY "Users can view own notification settings"
  ON public.user_notification_settings FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own notification settings" ON public.user_notification_settings;
CREATE POLICY "Users can insert own notification settings"
  ON public.user_notification_settings FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own notification settings" ON public.user_notification_settings;
CREATE POLICY "Users can update own notification settings"
  ON public.user_notification_settings FOR UPDATE
  TO authenticated
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own notification settings" ON public.user_notification_settings;
CREATE POLICY "Users can delete own notification settings"
  ON public.user_notification_settings FOR DELETE
  TO authenticated
  USING (auth.uid() = user_id);

-- Auto-create default notification settings when a new user signs up
CREATE OR REPLACE FUNCTION public.handle_new_user_notification_settings()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.user_notification_settings (user_id)
  VALUES (NEW.id)
  ON CONFLICT (user_id) DO NOTHING;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_on_auth_user_created_notif_settings ON auth.users;
CREATE TRIGGER trg_on_auth_user_created_notif_settings
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user_notification_settings();

-- Backfill settings for any existing users
INSERT INTO public.user_notification_settings (user_id)
SELECT id FROM auth.users
ON CONFLICT (user_id) DO NOTHING;

-- ============================================================================
-- 3. TABLE: public.notifications
-- Stores all user notifications (messages, updates, reminders, replies, mentions)
-- with read/unread status, JSONB metadata, deduplication key, and timestamps.
-- ============================================================================
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

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
  ON public.notifications (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
  ON public.notifications (user_id, is_read, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_user_dedupe
  ON public.notifications (user_id, dedupe_key)
  WHERE dedupe_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_legacy_id
  ON public.notifications (legacy_notification_id)
  WHERE legacy_notification_id IS NOT NULL;

DROP TRIGGER IF EXISTS trg_notifications_updated_at ON public.notifications;
CREATE TRIGGER trg_notifications_updated_at
  BEFORE UPDATE ON public.notifications
  FOR EACH ROW
  EXECUTE FUNCTION public.set_current_timestamp_updated_at();

-- Automatically stamp read_at when is_read transitions to TRUE
CREATE OR REPLACE FUNCTION public.sync_notifications_read_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.is_read = TRUE AND (OLD.is_read IS DISTINCT FROM TRUE OR NEW.read_at IS NULL) THEN
    NEW.read_at := COALESCE(NEW.read_at, NOW());
  ELSIF NEW.is_read = FALSE THEN
    NEW.read_at := NULL;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_notifications_read_at ON public.notifications;
CREATE TRIGGER trg_sync_notifications_read_at
  BEFORE INSERT OR UPDATE OF is_read ON public.notifications
  FOR EACH ROW
  EXECUTE FUNCTION public.sync_notifications_read_at();

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own notifications" ON public.notifications;
CREATE POLICY "Users can view own notifications"
  ON public.notifications FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own notifications" ON public.notifications;
CREATE POLICY "Users can insert own notifications"
  ON public.notifications FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = user_id OR auth.uid() = sender_user_id);

DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
CREATE POLICY "Users can update own notifications"
  ON public.notifications FOR UPDATE
  TO authenticated
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own notifications" ON public.notifications;
CREATE POLICY "Users can delete own notifications"
  ON public.notifications FOR DELETE
  TO authenticated
  USING (auth.uid() = user_id);

-- ============================================================================
-- 4. BIDIRECTIONAL BRIDGE WITH public.user_notifications
-- Ensures existing community replies and @mentions in public.user_notifications
-- automatically populate public.notifications and stay synchronized on read/delete.
-- ============================================================================
CREATE OR REPLACE FUNCTION public.bridge_legacy_user_notification_to_notifications()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_title TEXT;
  v_message TEXT;
  v_type TEXT;
  v_sender_name TEXT := 'Someone';
  v_sender_avatar TEXT := 'avatar_1';
  v_anime_title TEXT := 'Community Discussion';
  v_anime_cover TEXT := '';
  v_prefs RECORD;
BEGIN
  IF TG_OP = 'INSERT' THEN
    SELECT * INTO v_prefs
    FROM public.user_notification_settings
    WHERE user_id = NEW.recipient_user_id;

    IF FOUND AND (v_prefs.master_enabled = FALSE OR v_prefs.notifications_enabled = FALSE) THEN
      RETURN NEW;
    END IF;

    v_type := CASE
      WHEN NEW.notification_type IN ('mention_comment', 'mention_reply') THEN 'mention'
      WHEN NEW.notification_type = 'thread_reply' THEN 'reply'
      ELSE 'message'
    END;

    IF FOUND AND v_type IN ('message', 'reply', 'mention') AND v_prefs.messages_enabled = FALSE THEN
      RETURN NEW;
    END IF;

    SELECT COALESCE(p.username, 'Someone'), COALESCE(p.avatar_key, 'avatar_1')
    INTO v_sender_name, v_sender_avatar
    FROM public.profiles p
    WHERE p.id = NEW.sender_user_id;

    SELECT COALESCE(a.title_english, a.title_romaji, 'Anime #' || NEW.anime_id::text), COALESCE(a.cover_image, '')
    INTO v_anime_title, v_anime_cover
    FROM public.anime a
    WHERE a.anilist_id = NEW.anime_id;

    v_title := CASE
      WHEN NEW.notification_type IN ('mention_comment', 'mention_reply')
        THEN COALESCE(v_sender_name, 'Someone') || ' mentioned you in ' || COALESCE(v_anime_title, 'Community')
      ELSE COALESCE(v_sender_name, 'Someone') || ' replied to you in ' || COALESCE(v_anime_title, 'Community')
    END;

    v_message := COALESCE(NULLIF(trim(NEW.preview_text), ''), 'Tap to open the discussion in Anime Universe.');

    INSERT INTO public.notifications (
      user_id,
      sender_user_id,
      title,
      message,
      type,
      is_read,
      read_at,
      metadata,
      dedupe_key,
      legacy_notification_id,
      created_at
    )
    VALUES (
      NEW.recipient_user_id,
      NEW.sender_user_id,
      left(v_title, 200),
      left(v_message, 2000),
      v_type,
      COALESCE(NEW.is_read, FALSE),
      NEW.read_at,
      jsonb_build_object(
        'screen', 'community',
        'anime_id', NEW.anime_id,
        'anime_title', COALESCE(v_anime_title, ''),
        'anime_cover_image', COALESCE(v_anime_cover, ''),
        'comment_id', NEW.comment_id,
        'thread_id', NEW.thread_id,
        'reply_id', NEW.reply_id,
        'sender_user_id', NEW.sender_user_id,
        'sender_username', COALESCE(v_sender_name, 'Someone'),
        'sender_avatar_key', COALESCE(v_sender_avatar, 'avatar_1'),
        'legacy_id', NEW.id
      ),
      'legacy_un_' || NEW.id::text,
      NEW.id,
      COALESCE(NEW.created_at, NOW())
    )
    ON CONFLICT (legacy_notification_id) WHERE legacy_notification_id IS NOT NULL
    DO UPDATE SET
      is_read = EXCLUDED.is_read,
      read_at = EXCLUDED.read_at,
      updated_at = NOW();

    RETURN NEW;
  ELSIF TG_OP = 'UPDATE' THEN
    UPDATE public.notifications
    SET
      is_read = NEW.is_read,
      read_at = NEW.read_at,
      updated_at = NOW()
    WHERE legacy_notification_id = NEW.id
      AND is_read IS DISTINCT FROM NEW.is_read;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    DELETE FROM public.notifications
    WHERE legacy_notification_id = OLD.id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'user_notifications'
  ) THEN
    -- Ensure read_at column exists on user_notifications
    ALTER TABLE public.user_notifications
      ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

    DROP TRIGGER IF EXISTS trg_bridge_legacy_user_notifications ON public.user_notifications;
    CREATE TRIGGER trg_bridge_legacy_user_notifications
      AFTER INSERT OR UPDATE OF is_read, read_at OR DELETE ON public.user_notifications
      FOR EACH ROW
      EXECUTE FUNCTION public.bridge_legacy_user_notification_to_notifications();

    -- Backfill existing legacy notifications into public.notifications
    INSERT INTO public.notifications (
      user_id,
      sender_user_id,
      title,
      message,
      type,
      is_read,
      read_at,
      metadata,
      dedupe_key,
      legacy_notification_id,
      created_at
    )
    SELECT
      un.recipient_user_id,
      un.sender_user_id,
      left(
        CASE
          WHEN un.notification_type IN ('mention_comment', 'mention_reply')
            THEN COALESCE(p.username, 'Someone') || ' mentioned you in ' || COALESCE(a.title_english, a.title_romaji, 'Anime #' || un.anime_id::text)
          ELSE COALESCE(p.username, 'Someone') || ' replied to you in ' || COALESCE(a.title_english, a.title_romaji, 'Anime #' || un.anime_id::text)
        END,
        200
      ),
      left(COALESCE(NULLIF(trim(un.preview_text), ''), 'Tap to open the discussion in Anime Universe.'), 2000),
      CASE
        WHEN un.notification_type IN ('mention_comment', 'mention_reply') THEN 'mention'
        WHEN un.notification_type = 'thread_reply' THEN 'reply'
        ELSE 'message'
      END,
      COALESCE(un.is_read, FALSE),
      un.read_at,
      jsonb_build_object(
        'screen', 'community',
        'anime_id', un.anime_id,
        'anime_title', COALESCE(a.title_english, a.title_romaji, ''),
        'anime_cover_image', COALESCE(a.cover_image, ''),
        'comment_id', un.comment_id,
        'thread_id', un.thread_id,
        'reply_id', un.reply_id,
        'sender_user_id', un.sender_user_id,
        'sender_username', COALESCE(p.username, 'Someone'),
        'sender_avatar_key', COALESCE(p.avatar_key, 'avatar_1'),
        'legacy_id', un.id
      ),
      'legacy_un_' || un.id::text,
      un.id,
      COALESCE(un.created_at, NOW())
    FROM public.user_notifications un
    LEFT JOIN public.profiles p ON p.id = un.sender_user_id
    LEFT JOIN public.anime a ON a.anilist_id = un.anime_id
    ON CONFLICT (legacy_notification_id) WHERE legacy_notification_id IS NOT NULL
    DO NOTHING;
  END IF;
END $$;

-- Sync read/delete changes from public.notifications back to public.user_notifications
CREATE OR REPLACE FUNCTION public.sync_notification_changes_to_legacy()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF TG_OP = 'UPDATE' AND NEW.legacy_notification_id IS NOT NULL THEN
    UPDATE public.user_notifications
    SET
      is_read = NEW.is_read,
      read_at = NEW.read_at
    WHERE id = NEW.legacy_notification_id
      AND is_read IS DISTINCT FROM NEW.is_read;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' AND OLD.legacy_notification_id IS NOT NULL THEN
    DELETE FROM public.user_notifications
    WHERE id = OLD.legacy_notification_id;
    RETURN OLD;
  END IF;
  RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_notification_changes_to_legacy ON public.notifications;
CREATE TRIGGER trg_sync_notification_changes_to_legacy
  AFTER UPDATE OF is_read OR DELETE ON public.notifications
  FOR EACH ROW
  EXECUTE FUNCTION public.sync_notification_changes_to_legacy();

-- ============================================================================
-- 5. HELPER RPC FUNCTIONS FOR SECURE CLIENT & EDGE FUNCTION OPERATIONS
-- ============================================================================

CREATE OR REPLACE FUNCTION public.upsert_user_device(
  p_device_id TEXT,
  p_platform TEXT,
  p_push_token TEXT DEFAULT NULL,
  p_push_provider TEXT DEFAULT 'tauri_local',
  p_permission_status TEXT DEFAULT 'prompt',
  p_device_name TEXT DEFAULT NULL,
  p_app_version TEXT DEFAULT '2.4.0'
)
RETURNS public.user_devices
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid UUID := auth.uid();
  v_row public.user_devices;
BEGIN
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'Authentication required';
  END IF;

  INSERT INTO public.user_devices (
    user_id,
    device_id,
    platform,
    push_token,
    push_provider,
    permission_status,
    device_name,
    app_version,
    is_active,
    last_error,
    invalidated_at,
    last_active_at,
    last_seen_at
  )
  VALUES (
    v_uid,
    trim(p_device_id),
    lower(trim(p_platform)),
    NULLIF(trim(COALESCE(p_push_token, '')), ''),
    COALESCE(NULLIF(trim(p_push_provider), ''), 'tauri_local'),
    COALESCE(NULLIF(trim(p_permission_status), ''), 'prompt'),
    p_device_name,
    COALESCE(p_app_version, '2.4.0'),
    CASE WHEN COALESCE(p_permission_status, 'prompt') = 'denied' THEN FALSE ELSE TRUE END,
    NULL,
    NULL,
    NOW(),
    NOW()
  )
  ON CONFLICT (user_id, device_id)
  DO UPDATE SET
    platform = EXCLUDED.platform,
    push_token = COALESCE(EXCLUDED.push_token, public.user_devices.push_token),
    push_provider = EXCLUDED.push_provider,
    permission_status = EXCLUDED.permission_status,
    device_name = COALESCE(EXCLUDED.device_name, public.user_devices.device_name),
    app_version = COALESCE(EXCLUDED.app_version, public.user_devices.app_version),
    is_active = CASE WHEN EXCLUDED.permission_status = 'denied' THEN FALSE ELSE TRUE END,
    last_error = NULL,
    invalidated_at = NULL,
    last_active_at = NOW(),
    last_seen_at = NOW(),
    updated_at = NOW()
  RETURNING * INTO v_row;

  INSERT INTO public.user_notification_settings (user_id, os_permission_status)
  VALUES (v_uid, COALESCE(NULLIF(trim(p_permission_status), ''), 'prompt'))
  ON CONFLICT (user_id)
  DO UPDATE SET
    os_permission_status = EXCLUDED.os_permission_status,
    updated_at = NOW();

  RETURN v_row;
END;
$$;

GRANT EXECUTE ON FUNCTION public.upsert_user_device(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated;

DROP FUNCTION IF EXISTS public.mark_app_notifications_read(UUID, BIGINT);
CREATE OR REPLACE FUNCTION public.mark_app_notifications_read(
  p_notification_id UUID DEFAULT NULL,
  p_legacy_id UUID DEFAULT NULL
)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid UUID := auth.uid();
  v_updated INTEGER := 0;
BEGIN
  IF v_uid IS NULL THEN
    RETURN 0;
  END IF;

  IF p_notification_id IS NULL AND p_legacy_id IS NULL THEN
    UPDATE public.notifications
    SET is_read = TRUE, read_at = COALESCE(read_at, NOW()), updated_at = NOW()
    WHERE user_id = v_uid AND is_read = FALSE;
    GET DIAGNOSTICS v_updated = ROW_COUNT;

    UPDATE public.user_notifications
    SET is_read = TRUE, read_at = COALESCE(read_at, NOW())
    WHERE recipient_user_id = v_uid AND is_read = FALSE;
  ELSE
    UPDATE public.notifications
    SET is_read = TRUE, read_at = COALESCE(read_at, NOW()), updated_at = NOW()
    WHERE user_id = v_uid
      AND (
        (p_notification_id IS NOT NULL AND id = p_notification_id)
        OR (p_legacy_id IS NOT NULL AND legacy_notification_id = p_legacy_id)
      )
      AND is_read = FALSE;
    GET DIAGNOSTICS v_updated = ROW_COUNT;

    IF p_legacy_id IS NOT NULL THEN
      UPDATE public.user_notifications
      SET is_read = TRUE, read_at = COALESCE(read_at, NOW())
      WHERE recipient_user_id = v_uid AND id = p_legacy_id AND is_read = FALSE;
    END IF;
  END IF;

  RETURN v_updated;
END;
$$;

GRANT EXECUTE ON FUNCTION public.mark_app_notifications_read(UUID, UUID) TO authenticated;

-- ============================================================================
-- 6. REALTIME PUBLICATION CONFIGURATION
-- ============================================================================
ALTER TABLE public.notifications REPLICA IDENTITY FULL;
ALTER TABLE public.user_notification_settings REPLICA IDENTITY FULL;
ALTER TABLE public.user_devices REPLICA IDENTITY FULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
      AND schemaname = 'public'
      AND tablename = 'notifications'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
      AND schemaname = 'public'
      AND tablename = 'user_notification_settings'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.user_notification_settings;
  END IF;
END $$;

NOTIFY pgrst, 'reload schema';
