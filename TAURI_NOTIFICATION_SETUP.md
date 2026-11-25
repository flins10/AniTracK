# Complete Native & Remote Notification System Setup Guide (Tauri 2 + Supabase)

This document covers the complete setup, deployment, and provider configuration for **Anime Universe** across **Android APK**, **Windows EXE**, and **macOS DMG**.

---

## Architecture Overview: Local vs. Remote Push Notifications

- **Local OS Notifications (`tauri-plugin-notification` + Rust background worker)**:
  - Displayed directly by the OS notification manager when the Tauri app is running in the foreground or minimized to the system tray (Windows/macOS) or polled via Android `WorkManager` (`AnimeNotifWorker.kt`).
  - Strictly gated by **OS Notification Permission** (`granted`) and **User Notification Settings** (`notifications_enabled`, `messages_enabled`, `updates_enabled`, `reminders_enabled`).
- **Remote Push Notifications (`deliver-push` Edge Function -> FCM / APNs / WNS)**:
  - Supabase SQL and Supabase Realtime **cannot** wake a closed app process by themselves.
  - When the app is completely closed, `send-notification` (or a database webhook) invokes the `deliver-push` Supabase Edge Function, which authenticates with the platform's native push gateway using server-side secrets:
    - **Android APK**: Firebase Cloud Messaging (**FCM HTTP v1 API**) -> wakes `AnimeFirebaseMessagingService.kt`.
    - **macOS DMG**: Apple Push Notification service (**APNs HTTP/2 API** with ES256 `.p8` token) -> delivered by macOS `apsd`.
    - **Windows EXE**: Windows Push Notification Services (**WNS OAuth2 Toast XML**) -> delivered by Windows Action Center (`WpnService`).

---

## Step 1: Run the Supabase SQL Script

1. Open your **Supabase Dashboard** -> **SQL Editor** -> **New Query**.
2. Paste and execute the complete migration script located at:
   - `supabase/migrations/20261126_complete_tauri_notification_system.sql`
3. This script idempotently creates and configures:
   - `public.user_devices` (user ID, device ID, platform, push token, permission status, active flag, timestamps, unique `(user_id, device_id)` constraint, indexes, and RLS policies).
   - `public.user_notification_settings` (master `notifications_enabled` toggle, `messages_enabled`, `updates_enabled`, `reminders_enabled`, timestamps, and RLS policies).
   - `public.notifications` (title, message, type, read/unread status, `read_at`, JSONB metadata, timestamps, indexes, RLS policies, and bidirectional sync with `public.user_notifications`).
   - Helper RPCs (`public.upsert_user_device`, `public.mark_app_notifications_read`) and `supabase_realtime` publication enrollment.

---

## Step 2: Configure Native Push Providers (FCM, APNs, WNS)

### A. Android — Firebase Cloud Messaging (FCM HTTP v1)
1. In the **Firebase Console**, open your project (or create one matching your Android package name in `src-tauri/tauri.conf.json`).
2. Download `google-services.json` and place it in `src-tauri/gen/android/app/google-services.json`.
3. Ensure `AnimeFirebaseMessagingService` is registered inside `AndroidManifest.xml`:
   ```xml
   <service
       android:name=".AnimeFirebaseMessagingService"
       android:exported="false">
       <intent-filter>
           <action android:name="com.google.firebase.MESSAGING_EVENT" />
       </intent-filter>
   </service>
   ```
4. In Firebase Console -> **Project Settings** -> **Service accounts** -> **Generate new private key**.
5. Extract from the downloaded JSON:
   - `project_id` -> `FCM_PROJECT_ID`
   - `client_email` -> `FCM_CLIENT_EMAIL`
   - `private_key` -> `FCM_PRIVATE_KEY`

### B. macOS — Apple Push Notification service (APNs HTTP/2)
1. In the **Apple Developer Portal** -> **Certificates, Identifiers & Profiles**:
   - Enable **Push Notifications** for your macOS App ID (`bundle.identifier` in `src-tauri/tauri.conf.json`).
2. Under **Keys**, create a new key with **Apple Push Notifications service (APNs)** enabled and download the `.p8` file.
3. Record:
   - **Key ID** (10-character string) -> `APNS_KEY_ID`
   - **Team ID** (10-character Apple Developer Team ID) -> `APNS_TEAM_ID`
   - **Bundle ID** -> `APNS_BUNDLE_ID`
   - **`.p8` file contents** -> `APNS_PRIVATE_KEY`
   - **Environment** (`production` for notarized/signed DMG or `development` for local debug) -> `APNS_ENV`

### C. Windows — Windows Push Notification Services (WNS)
1. Register your Windows application in **Microsoft Partner Center** (Windows & Xbox -> App overview) to give your packaged Windows build an **App Identity** (`Package/Identity/Name` and `Publisher` in `AppxManifest.xml` / MSIX identity).
2. In **Microsoft Partner Center** -> **WNS / MPNS** -> **Live Services site** (or Azure Entra App Registration linked to the Partner Center app):
   - Copy the **Package SID** (`ms-app://...`) -> `WNS_PACKAGE_SID`
   - Copy the **Client Secret** -> `WNS_CLIENT_SECRET`

---

## Step 3: Set Required Supabase Secrets

Never place any of these secrets in frontend code. Run the following command via the **Supabase CLI** (or add them in **Supabase Dashboard -> Edge Functions -> Secrets**):

```bash
supabase secrets set \
  FCM_PROJECT_ID="your-firebase-project-id" \
  FCM_CLIENT_EMAIL="firebase-adminsdk-xxxxx@your-project.iam.gserviceaccount.com" \
  FCM_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n" \
  APNS_TEAM_ID="ABCDE12345" \
  APNS_KEY_ID="12345ABCDE" \
  APNS_BUNDLE_ID="com.animeuniverse.app" \
  APNS_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----" \
  APNS_ENV="production" \
  WNS_PACKAGE_SID="ms-app://s-1-15-2-..." \
  WNS_CLIENT_SECRET="your-wns-client-secret" \
  INTERNAL_PUSH_WEBHOOK_SECRET="generate-a-strong-random-32-byte-hex-secret"
```

*(Note: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` are automatically injected by Supabase into every Edge Function environment.)*

---

## Step 4: Deploy All 6 Supabase Edge Functions

Deploy all functions from your workspace root using the Supabase CLI:

```bash
supabase functions deploy register-device
supabase functions deploy unregister-device
supabase functions deploy update-notification-settings
supabase functions deploy send-notification
supabase functions deploy deliver-push
supabase functions deploy cleanup-invalid-device-tokens
```

### Edge Function Reference & Example Requests / Responses

#### 1. `register-device`
- **Endpoint**: `POST https://<project-ref>.supabase.co/functions/v1/register-device`
- **Headers**: `Authorization: Bearer <user_access_token>`, `Content-Type: application/json`
- **Request**:
  ```json
  {
    "device_id": "android_f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
    "platform": "android",
    "push_token": "fcm_token_or_null",
    "permission_status": "granted",
    "app_version": "2.4.0",
    "os_version": "Android 14",
    "device_name": "Pixel 8 Pro"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "ok": true,
    "device": {
      "id": "8e10c21a-39f7-4f1b-9c25-56d4910a9181",
      "user_id": "c7a6b112-8b99-4d40-87e1-112233445566",
      "device_id": "android_f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
      "platform": "android",
      "permission_status": "granted",
      "is_active": true
    }
  }
  ```

#### 2. `unregister-device`
- **Endpoint**: `POST https://<project-ref>.supabase.co/functions/v1/unregister-device`
- **Headers**: `Authorization: Bearer <user_access_token>`, `Content-Type: application/json`
- **Request**:
  ```json
  {
    "device_id": "android_f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
    "hard_delete": false
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "ok": true,
    "mode": "deactivated",
    "device_id": "android_f81d4fae-7dec-11d0-a765-00a0c91e6bf6"
  }
  ```

#### 3. `update-notification-settings`
- **Endpoint**: `POST https://<project-ref>.supabase.co/functions/v1/update-notification-settings`
- **Headers**: `Authorization: Bearer <user_access_token>`, `Content-Type: application/json`
- **Request**:
  ```json
  {
    "notifications_enabled": true,
    "messages_enabled": true,
    "updates_enabled": true,
    "reminders_enabled": false,
    "device_id": "android_f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
    "permission_status": "granted"
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "ok": true,
    "settings": {
      "user_id": "c7a6b112-8b99-4d40-87e1-112233445566",
      "notifications_enabled": true,
      "messages_enabled": true,
      "updates_enabled": true,
      "reminders_enabled": false
    }
  }
  ```

#### 4. `send-notification`
- **Endpoint**: `POST https://<project-ref>.supabase.co/functions/v1/send-notification`
- **Headers**: `Authorization: Bearer <user_access_token>`, `Content-Type: application/json`
- **Request**:
  ```json
  {
    "recipient_user_id": "c7a6b112-8b99-4d40-87e1-112233445566",
    "title": "Someone tagged you!",
    "message": "@akira mentioned you in Frieren: Beyond Journey's End",
    "type": "message",
    "metadata": {
      "screen": "community",
      "anime_id": "52991",
      "comment_id": "9f12c0a1-1234-4a5b-8910-abcdef123456"
    },
    "deliver_remote_push": true
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "ok": true,
    "notification": {
      "id": "d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f7a",
      "user_id": "c7a6b112-8b99-4d40-87e1-112233445566",
      "title": "Someone tagged you!",
      "message": "@akira mentioned you in Frieren: Beyond Journey's End",
      "type": "message",
      "is_read": false
    },
    "push_delivery": {
      "ok": true,
      "delivered_count": 1,
      "total_devices": 1
    }
  }
  ```

#### 5. `deliver-push`
- **Endpoint**: `POST https://<project-ref>.supabase.co/functions/v1/deliver-push`
- **Headers**: `Authorization: Bearer <service_role_or_user_jwt>` (or `x-internal-webhook-secret: <INTERNAL_PUSH_WEBHOOK_SECRET>`)
- **Request**:
  ```json
  {
    "user_id": "c7a6b112-8b99-4d40-87e1-112233445566",
    "notification_id": "d4e5f6a7-b8c9-4d0e-9f1a-2b3c4d5e6f7a",
    "title": "Stay in the Loop!",
    "message": "Your notification pipeline is active.",
    "type": "update",
    "metadata": {
      "screen": "notifications"
    }
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "ok": true,
    "delivered_count": 1,
    "invalid_tokens_cleaned": 0,
    "total_devices": 1,
    "results": [
      {
        "device_id": "android_f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
        "platform": "android",
        "delivered": true,
        "provider": "fcm_v1",
        "status": 200
      }
    ]
  }
  ```

#### 6. `cleanup-invalid-device-tokens`
- **Endpoint**: `POST https://<project-ref>.supabase.co/functions/v1/cleanup-invalid-device-tokens`
- **Headers**: `x-internal-webhook-secret: <INTERNAL_PUSH_WEBHOOK_SECRET>` (or `Authorization: Bearer <user_access_token>` for user-scoped cleanup)
- **Request**:
  ```json
  {
    "stale_days": 90,
    "hard_delete": false
  }
  ```
- **Response (`200 OK`)**:
  ```json
  {
    "ok": true,
    "scope": "global",
    "cutoff_iso": "2026-08-26T00:00:00.000Z",
    "summary": {
      "explicit_cleaned": 0,
      "denied_deactivated": 2,
      "stale_removed": 1
    }
  }
  ```

---

## Step 5: Connect & Test the Tauri 2 App

1. **First Launch Permission Flow**:
   - Launch the app and sign in.
   - If OS notification permission is not yet granted and the user has not clicked `"Maybe Later"`, the **"Stay in the Loop!"** modal appears with an animated bell illustration.
   - Clicking **"Enable Notifications"** invokes `request_os_notification_permission` in Rust (`src-tauri/src/lib.rs`) and `tauri-plugin-notification` to display the real OS permission dialog, registers the device via `register-device`, and persists preferences via `update-notification-settings`.
   - Clicking **"Maybe Later"** records the dismissal in local storage (`anime_first_time_notif_popup_dismissed_v1`) and syncs actual OS status without falsely granting permission.
2. **Application Settings (`Settings -> Notifications`)**:
   - View live **OS Permission** status (`GRANTED`, `DENIED`, or `NOT REQUESTED`) and **Device Registration** status.
   - Toggle **Master Notifications** and individual categories (**Messages**, **Updates**, **Reminders**).
   - Click **"Send Test Notification"** to verify end-to-end Supabase persistence + native OS toast delivery.
   - Click **"System Settings"** if OS permission was previously denied to jump directly to Android/Windows/macOS notification settings.
