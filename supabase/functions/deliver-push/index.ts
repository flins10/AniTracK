import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface DeliverPushPayload {
  notification_id?: string;
  user_id?: string;
  title?: string;
  message?: string;
  type?: string;
  metadata?: Record<string, unknown>;
}

interface DeviceRow {
  id: string;
  user_id: string;
  device_id: string;
  platform: "android" | "windows" | "macos" | "web";
  push_token: string | null;
  push_provider: string;
  permission_status: string;
  is_active: boolean;
}

// Base64URL encoding helper for Web Crypto JWT signing (FCM RS256 & APNs ES256)
function base64UrlEncode(input: Uint8Array | string): string {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input;
  let binary = "";
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const cleaned = pem
    .replace(/-----BEGIN [A-Z ]+-----/g, "")
    .replace(/-----END [A-Z ]+-----/g, "")
    .replace(/\\n/g, "")
    .replace(/\s+/g, "");
  const binary = atob(cleaned);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

// ============================================================================
// 1. ANDROID: Firebase Cloud Messaging (FCM HTTP v1 API)
// Uses Google Service Account OAuth2 Bearer Token signed via RS256
// ============================================================================
async function getFcmAccessToken(clientEmail: string, privateKeyPem: string): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claimSet = {
    iss: clientEmail,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };

  const unsignedJwt = `${base64UrlEncode(JSON.stringify(header))}.${base64UrlEncode(JSON.stringify(claimSet))}`;
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(privateKeyPem),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(unsignedJwt)
  );
  const signedJwt = `${unsignedJwt}.${base64UrlEncode(new Uint8Array(signature))}`;

  const tokenResp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: signedJwt,
    }),
  });

  if (!tokenResp.ok) {
    const errText = await tokenResp.text();
    throw new Error(`FCM OAuth2 token exchange failed (${tokenResp.status}): ${errText}`);
  }

  const tokenData = await tokenResp.json();
  return tokenData.access_token;
}

async function sendAndroidFcmPush(
  device: DeviceRow,
  payload: { notification_id: string; title: string; message: string; type: string; metadata: Record<string, unknown> }
): Promise<{ delivered: boolean; invalidToken?: boolean; providerResponse?: unknown; error?: string }> {
  const projectId = Deno.env.get("FIREBASE_PROJECT_ID") ?? Deno.env.get("FCM_PROJECT_ID") ?? "";
  const clientEmail = Deno.env.get("FIREBASE_CLIENT_EMAIL") ?? Deno.env.get("FCM_CLIENT_EMAIL") ?? "";
  const privateKey = Deno.env.get("FIREBASE_PRIVATE_KEY") ?? Deno.env.get("FCM_PRIVATE_KEY") ?? "";

  if (!projectId || !clientEmail || !privateKey) {
    return {
      delivered: false,
      error: "FCM credentials not configured (FIREBASE_PROJECT_ID/FCM_PROJECT_ID, FIREBASE_CLIENT_EMAIL/FCM_CLIENT_EMAIL, FIREBASE_PRIVATE_KEY/FCM_PRIVATE_KEY)",
    };
  }

  const accessToken = await getFcmAccessToken(clientEmail, privateKey);
  const fcmUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;

  const response = await fetch(fcmUrl, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      message: {
        token: device.push_token,
        notification: {
          title: payload.title,
          body: payload.message,
        },
        data: {
          notification_id: String(payload.notification_id),
          type: String(payload.type),
          target_screen: String(payload.metadata.target_screen ?? "notifications"),
          topic: String(payload.metadata.topic ?? "general"),
          message_id: String(payload.metadata.message_id ?? ""),
        },
        android: {
          priority: "HIGH",
          notification: {
            channel_id:
              payload.type === "reminder"
                ? "anime_reminders"
                : payload.type === "update" || payload.type === "system"
                ? "anime_updates"
                : "anime_community_alerts",
            sound: "default",
            click_action: "OPEN_ANIME_UNIVERSE_NOTIFICATION",
          },
        },
      },
    }),
  });

  const respBody = await response.json().catch(() => ({}));
  if (!response.ok) {
    const errorCode = respBody?.error?.details?.[0]?.errorCode ?? respBody?.error?.status ?? "";
    const isInvalidToken =
      response.status === 404 ||
      errorCode === "UNREGISTERED" ||
      errorCode === "INVALID_ARGUMENT";
    return {
      delivered: false,
      invalidToken: isInvalidToken,
      providerResponse: respBody,
      error: `FCM HTTP v1 error (${response.status}): ${errorCode || JSON.stringify(respBody)}`,
    };
  }

  return { delivered: true, providerResponse: respBody };
}

// ============================================================================
// 2. macOS: Apple Push Notification service (APNs Token-Based HTTP/2 API)
// Uses ES256 (.p8) Provider Authentication Token
// ============================================================================
async function getApnsProviderJwt(keyId: string, teamId: string, p8PrivateKey: string): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "ES256", kid: keyId };
  const claims = { iss: teamId, iat: now };

  const unsignedJwt = `${base64UrlEncode(JSON.stringify(header))}.${base64UrlEncode(JSON.stringify(claims))}`;
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(p8PrivateKey),
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    { name: "ECDSA", hash: { name: "SHA-256" } },
    cryptoKey,
    new TextEncoder().encode(unsignedJwt)
  );
  return `${unsignedJwt}.${base64UrlEncode(new Uint8Array(signature))}`;
}

async function sendMacOsApnsPush(
  device: DeviceRow,
  payload: { notification_id: string; title: string; message: string; type: string; metadata: Record<string, unknown> }
): Promise<{ delivered: boolean; invalidToken?: boolean; providerResponse?: unknown; error?: string }> {
  const keyId = Deno.env.get("APNS_KEY_ID") ?? "";
  const teamId = Deno.env.get("APNS_TEAM_ID") ?? "";
  const privateKey = Deno.env.get("APNS_PRIVATE_KEY") ?? "";
  const bundleId = Deno.env.get("APNS_BUNDLE_ID") ?? "com.animeuniverse.app";
  const apnsEnv = (Deno.env.get("APNS_ENV") ?? "production").toLowerCase();

  if (!keyId || !teamId || !privateKey) {
    return {
      delivered: false,
      error: "APNs credentials not configured (APNS_KEY_ID, APNS_TEAM_ID, APNS_PRIVATE_KEY)",
    };
  }

  const jwt = await getApnsProviderJwt(keyId, teamId, privateKey);
  const host =
    apnsEnv === "sandbox" || apnsEnv === "development"
      ? "https://api.sandbox.push.apple.com"
      : "https://api.push.apple.com";

  const response = await fetch(`${host}/3/device/${device.push_token}`, {
    method: "POST",
    headers: {
      authorization: `bearer ${jwt}`,
      "apns-topic": bundleId,
      "apns-push-type": "alert",
      "apns-priority": "10",
      "content-type": "application/json",
    },
    body: JSON.stringify({
      aps: {
        alert: {
          title: payload.title,
          body: payload.message,
        },
        sound: "default",
      },
      notification_id: payload.notification_id,
      type: payload.type,
      metadata: payload.metadata,
    }),
  });

  if (!response.ok) {
    const respBody = await response.json().catch(() => ({}));
    const reason = respBody?.reason ?? "";
    const isInvalidToken =
      response.status === 410 ||
      reason === "BadDeviceToken" ||
      reason === "Unregistered" ||
      reason === "DeviceTokenNotForTopic";
    return {
      delivered: false,
      invalidToken: isInvalidToken,
      providerResponse: respBody,
      error: `APNs error (${response.status}): ${reason}`,
    };
  }

  return {
    delivered: true,
    providerResponse: { apnsId: response.headers.get("apns-id") },
  };
}

// ============================================================================
// 3. WINDOWS: Windows Push Notification Services (WNS OAuth2 + Toast XML)
// ============================================================================
function escapeXml(unsafe: string): string {
  return unsafe
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

async function sendWindowsWnsPush(
  device: DeviceRow,
  payload: { notification_id: string; title: string; message: string; type: string; metadata: Record<string, unknown> }
): Promise<{ delivered: boolean; invalidToken?: boolean; providerResponse?: unknown; error?: string }> {
  const packageSid = Deno.env.get("WNS_PACKAGE_SID") ?? "";
  const clientSecret = Deno.env.get("WNS_CLIENT_SECRET") ?? "";

  if (!packageSid || !clientSecret) {
    return {
      delivered: false,
      error: "WNS credentials not configured (WNS_PACKAGE_SID, WNS_CLIENT_SECRET)",
    };
  }

  const tokenResp = await fetch("https://login.live.com/accesstoken.srf", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "client_credentials",
      client_id: packageSid,
      client_secret: clientSecret,
      scope: "notify.windows.com",
    }),
  });

  if (!tokenResp.ok) {
    const errText = await tokenResp.text();
    return {
      delivered: false,
      error: `WNS OAuth2 token exchange failed (${tokenResp.status}): ${errText}`,
    };
  }

  const { access_token: wnsAccessToken } = await tokenResp.json();
  const launchArgs = escapeXml(
    JSON.stringify({
      notification_id: payload.notification_id,
      type: payload.type,
      target_screen: payload.metadata.target_screen ?? "notifications",
      topic: payload.metadata.topic ?? "general",
    })
  );

  const toastXml = `<?xml version="1.0" encoding="utf-8"?>
<toast launch="${launchArgs}">
  <visual>
    <binding template="ToastGeneric">
      <text>${escapeXml(payload.title)}</text>
      <text>${escapeXml(payload.message)}</text>
    </binding>
  </visual>
</toast>`;

  const wnsResp = await fetch(device.push_token!, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${wnsAccessToken}`,
      "Content-Type": "text/xml",
      "X-WNS-Type": "wns/toast",
    },
    body: toastXml,
  });

  if (!wnsResp.ok) {
    const isInvalidToken = wnsResp.status === 404 || wnsResp.status === 410;
    return {
      delivered: false,
      invalidToken: isInvalidToken,
      error: `WNS push failed (${wnsResp.status}): ${wnsResp.headers.get("X-WNS-Error-Description") ?? ""}`,
    };
  }

  return {
    delivered: true,
    providerResponse: {
      wnsStatus: wnsResp.headers.get("X-WNS-NotificationStatus"),
      wnsMsgId: wnsResp.headers.get("X-WNS-Msg-ID"),
    },
  };
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ error: "Method not allowed. Use POST." }),
      { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return new Response(
        JSON.stringify({ error: "Missing or invalid Authorization Bearer token" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

    const token = authHeader.replace("Bearer ", "").trim();
    const adminClient = createClient(supabaseUrl, supabaseServiceKey);

    // Allow either internal service_role invocation or an authenticated user sending to themselves
    let authorizedUserId: string | null = null;
    const isServiceRole = token === supabaseServiceKey;
    if (!isServiceRole) {
      const userClient = createClient(supabaseUrl, supabaseAnonKey, {
        global: { headers: { Authorization: authHeader } },
      });
      const { data: { user }, error: userError } = await userClient.auth.getUser();
      if (userError || !user) {
        return new Response(
          JSON.stringify({ error: "Unauthorized" }),
          { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
      authorizedUserId = user.id;
    }

    const body: DeliverPushPayload = await req.json().catch(() => ({}));
    const targetUserId = (body.user_id ?? authorizedUserId ?? "").trim();
    const notificationId = (body.notification_id ?? crypto.randomUUID()).trim();
    const title = (body.title ?? "Anime Universe").trim();
    const message = (body.message ?? "You have a new notification.").trim();
    const type = (body.type ?? "message").trim();
    const metadata = body.metadata && typeof body.metadata === "object" ? body.metadata : {};

    if (!targetUserId) {
      return new Response(
        JSON.stringify({ error: "user_id is required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Fetch active, permission-granted devices with a valid push token for the recipient
    const { data: devices, error: devicesError } = await adminClient
      .from("user_devices")
      .select("*")
      .eq("user_id", targetUserId)
      .eq("is_active", true)
      .eq("permission_status", "granted");

    if (devicesError) {
      return new Response(
        JSON.stringify({ error: "Failed to load user devices", details: devicesError.message }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const activeDevices = (devices ?? []) as DeviceRow[];
    const results: Array<Record<string, unknown>> = [];
    const invalidDeviceIds: string[] = [];

    for (const device of activeDevices) {
      if (!device.push_token) {
        results.push({
          device_id: device.device_id,
          platform: device.platform,
          delivered: false,
          mode: "background_worker_or_realtime",
          reason: "No remote push_token registered; delivered via Tauri background worker & Realtime",
        });
        continue;
      }

      let outcome: { delivered: boolean; invalidToken?: boolean; providerResponse?: unknown; error?: string };
      if (device.platform === "android" || device.push_provider === "fcm") {
        outcome = await sendAndroidFcmPush(device, { notification_id: notificationId, title, message, type, metadata });
      } else if (device.platform === "macos" || device.push_provider === "apns") {
        outcome = await sendMacOsApnsPush(device, { notification_id: notificationId, title, message, type, metadata });
      } else if (device.platform === "windows" || device.push_provider === "wns") {
        outcome = await sendWindowsWnsPush(device, { notification_id: notificationId, title, message, type, metadata });
      } else {
        outcome = {
          delivered: false,
          error: `Unsupported remote push platform/provider: ${device.platform}/${device.push_provider}`,
        };
      }

      if (outcome.invalidToken) {
        invalidDeviceIds.push(device.id);
      }

      results.push({
        device_id: device.device_id,
        platform: device.platform,
        provider: device.push_provider,
        ...outcome,
      });
    }

    // Automatically deactivate invalid/expired tokens discovered during delivery
    if (invalidDeviceIds.length > 0) {
      const nowIso = new Date().toISOString();
      await adminClient
        .from("user_devices")
        .update({
          is_active: false,
          push_token: null,
          last_error: "Invalid or expired push token rejected by provider",
          invalidated_at: nowIso,
          updated_at: nowIso,
        })
        .in("id", invalidDeviceIds);
    }

    return new Response(
      JSON.stringify({
        success: true,
        user_id: targetUserId,
        notification_id: notificationId,
        total_devices: activeDevices.length,
        invalidated_tokens: invalidDeviceIds.length,
        results,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unexpected error in deliver-push";
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
