import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const VALID_PLATFORMS = new Set(["android", "windows", "macos", "web"]);
const VALID_PROVIDERS = new Set(["fcm", "apns", "wns", "webpush", "tauri_local"]);
const VALID_PERMISSIONS = new Set(["granted", "denied", "prompt", "unsupported"]);

interface RegisterDevicePayload {
  device_id?: string;
  platform?: string;
  push_token?: string | null;
  push_provider?: string;
  permission_status?: string;
  device_name?: string | null;
  app_version?: string;
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

    if (!supabaseUrl || !supabaseAnonKey || !supabaseServiceKey) {
      return new Response(
        JSON.stringify({ error: "Server configuration error: missing Supabase environment variables" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Verify caller identity using the caller's JWT
    const userClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: { user }, error: userError } = await userClient.auth.getUser();
    if (userError || !user) {
      return new Response(
        JSON.stringify({ error: "Unauthorized: invalid or expired session token" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const body: RegisterDevicePayload = await req.json().catch(() => ({}));
    const deviceId = (body.device_id ?? "").trim();
    const platform = (body.platform ?? "").trim().toLowerCase();
    const permissionStatus = (body.permission_status ?? "prompt").trim().toLowerCase();
    const defaultProvider =
      platform === "android"
        ? "fcm"
        : platform === "macos"
        ? "apns"
        : platform === "windows"
        ? "wns"
        : "tauri_local";
    const pushProvider = (body.push_provider ?? defaultProvider).trim().toLowerCase();
    const pushToken = body.push_token ? String(body.push_token).trim() : null;
    const deviceName = body.device_name ? String(body.device_name).trim().slice(0, 120) : null;
    const appVersion = body.app_version ? String(body.app_version).trim().slice(0, 32) : "2.0.0";

    // Input validation
    if (!deviceId || deviceId.length < 4 || deviceId.length > 255) {
      return new Response(
        JSON.stringify({ error: "Invalid device_id: must be between 4 and 255 characters" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!VALID_PLATFORMS.has(platform)) {
      return new Response(
        JSON.stringify({ error: "Invalid platform: must be one of android, windows, macos, web" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!VALID_PERMISSIONS.has(permissionStatus)) {
      return new Response(
        JSON.stringify({ error: "Invalid permission_status: must be granted, denied, prompt, or unsupported" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!VALID_PROVIDERS.has(pushProvider)) {
      return new Response(
        JSON.stringify({ error: "Invalid push_provider: must be fcm, apns, wns, webpush, or tauri_local" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const adminClient = createClient(supabaseUrl, supabaseServiceKey);

    // If this push token was previously claimed by another user on the same device, detach it first
    if (pushToken) {
      await adminClient
        .from("user_devices")
        .update({ is_active: false, push_token: null, updated_at: new Date().toISOString() })
        .eq("push_token", pushToken)
        .neq("user_id", user.id);
    }

    const isActive = permissionStatus === "granted";
    const nowIso = new Date().toISOString();

    const { data: deviceRecord, error: upsertError } = await adminClient
      .from("user_devices")
      .upsert(
        {
          user_id: user.id,
          device_id: deviceId,
          platform,
          push_token: pushToken,
          push_provider: pushProvider,
          permission_status: permissionStatus,
          device_name: deviceName,
          app_version: appVersion,
          is_active: isActive,
          last_error: null,
          invalidated_at: null,
          last_seen_at: nowIso,
          updated_at: nowIso,
        },
        { onConflict: "user_id,device_id" }
      )
      .select("*")
      .single();

    if (upsertError) {
      return new Response(
        JSON.stringify({ error: "Failed to register device", details: upsertError.message }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Ensure user_notification_settings row exists and reflects OS permission state
    await adminClient
      .from("user_notification_settings")
      .upsert(
        {
          user_id: user.id,
          os_permission_status: permissionStatus,
          updated_at: nowIso,
        },
        { onConflict: "user_id" }
      );

    return new Response(
      JSON.stringify({
        success: true,
        device: deviceRecord,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unexpected error in register-device";
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
