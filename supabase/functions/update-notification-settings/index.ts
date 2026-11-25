import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, PATCH, OPTIONS",
};

const VALID_PERMISSIONS = new Set(["granted", "denied", "prompt", "unsupported"]);

interface UpdateSettingsPayload {
  master_enabled?: boolean;
  messages_enabled?: boolean;
  updates_enabled?: boolean;
  reminders_enabled?: boolean;
  replies_enabled?: boolean;
  mentions_enabled?: boolean;
  os_permission_status?: string;
  onboarding_dismissed?: boolean;
  device_id?: string;
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST" && req.method !== "PATCH") {
    return new Response(
      JSON.stringify({ error: "Method not allowed. Use POST or PATCH." }),
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

    const body: UpdateSettingsPayload = await req.json().catch(() => ({}));
    const nowIso = new Date().toISOString();

    const updateFields: Record<string, unknown> = {
      user_id: user.id,
      updated_at: nowIso,
    };

    if (typeof body.master_enabled === "boolean") {
      updateFields.master_enabled = body.master_enabled;
    }
    if (typeof body.messages_enabled === "boolean") {
      updateFields.messages_enabled = body.messages_enabled;
      updateFields.replies_enabled = body.messages_enabled;
      updateFields.mentions_enabled = body.messages_enabled;
    }
    if (typeof body.updates_enabled === "boolean") {
      updateFields.updates_enabled = body.updates_enabled;
    }
    if (typeof body.reminders_enabled === "boolean") {
      updateFields.reminders_enabled = body.reminders_enabled;
    }
    if (typeof body.replies_enabled === "boolean") {
      updateFields.replies_enabled = body.replies_enabled;
    }
    if (typeof body.mentions_enabled === "boolean") {
      updateFields.mentions_enabled = body.mentions_enabled;
    }
    if (typeof body.onboarding_dismissed === "boolean") {
      updateFields.onboarding_dismissed = body.onboarding_dismissed;
    }
    if (typeof body.os_permission_status === "string") {
      const normalizedPerm = body.os_permission_status.trim().toLowerCase();
      if (!VALID_PERMISSIONS.has(normalizedPerm)) {
        return new Response(
          JSON.stringify({ error: "Invalid os_permission_status value" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
      updateFields.os_permission_status = normalizedPerm;
    }

    const adminClient = createClient(supabaseUrl, supabaseServiceKey);

    const { data: settings, error: upsertError } = await adminClient
      .from("user_notification_settings")
      .upsert(updateFields, { onConflict: "user_id" })
      .select("*")
      .single();

    if (upsertError) {
      return new Response(
        JSON.stringify({ error: "Failed to update notification settings", details: upsertError.message }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // If device_id or os_permission_status was provided, sync device active state
    if (body.device_id && typeof updateFields.os_permission_status === "string") {
      const perm = updateFields.os_permission_status as string;
      await adminClient
        .from("user_devices")
        .update({
          permission_status: perm,
          is_active: perm === "granted" && settings.master_enabled !== false,
          updated_at: nowIso,
        })
        .eq("user_id", user.id)
        .eq("device_id", body.device_id.trim());
    }

    return new Response(
      JSON.stringify({
        success: true,
        settings,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unexpected error in update-notification-settings";
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
