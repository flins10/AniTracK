import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface CleanupPayload {
  stale_days?: number;
  invalid_tokens?: string[];
  device_ids?: string[];
  hard_delete_invalid?: boolean;
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
    const isServiceRole = token === supabaseServiceKey;
    const adminClient = createClient(supabaseUrl, supabaseServiceKey);

    let scopedUserId: string | null = null;
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
      scopedUserId = user.id;
    }

    const body: CleanupPayload = await req.json().catch(() => ({}));
    const staleDays = Math.min(Math.max(Number(body.stale_days ?? 90), 7), 365);
    const invalidTokens = Array.isArray(body.invalid_tokens)
      ? body.invalid_tokens.map((t) => String(t).trim()).filter(Boolean)
      : [];
    const deviceIds = Array.isArray(body.device_ids)
      ? body.device_ids.map((d) => String(d).trim()).filter(Boolean)
      : [];
    const hardDeleteInvalid = Boolean(body.hard_delete_invalid);

    const nowIso = new Date().toISOString();
    const cutoffIso = new Date(Date.now() - staleDays * 24 * 60 * 60 * 1000).toISOString();

    let invalidatedByTokenCount = 0;
    let invalidatedByDeviceCount = 0;
    let staleCleanedCount = 0;

    // 1. Deactivate specific invalid push tokens reported by FCM / APNs / WNS
    if (invalidTokens.length > 0) {
      let query = adminClient
        .from("user_devices")
        .update({
          is_active: false,
          push_token: null,
          last_error: "Token invalidated by cleanup-invalid-device-tokens",
          invalidated_at: nowIso,
          updated_at: nowIso,
        })
        .in("push_token", invalidTokens);

      if (scopedUserId) query = query.eq("user_id", scopedUserId);
      const { data: rows } = await query.select("id");
      invalidatedByTokenCount = rows?.length ?? 0;
    }

    // 2. Deactivate specific device_ids
    if (deviceIds.length > 0) {
      let query = adminClient
        .from("user_devices")
        .update({
          is_active: false,
          push_token: null,
          last_error: "Device deactivated by cleanup-invalid-device-tokens",
          invalidated_at: nowIso,
          updated_at: nowIso,
        })
        .in("device_id", deviceIds);

      if (scopedUserId) query = query.eq("user_id", scopedUserId);
      const { data: rows } = await query.select("id");
      invalidatedByDeviceCount = rows?.length ?? 0;
    }

    // 3. Deactivate or remove stale devices that have not been seen in stale_days
    if (hardDeleteInvalid) {
      let delQuery = adminClient
        .from("user_devices")
        .delete()
        .or(`is_active.eq.false,last_seen_at.lt.${cutoffIso}`);
      if (scopedUserId) delQuery = delQuery.eq("user_id", scopedUserId);
      const { data: delRows } = await delQuery.select("id");
      staleCleanedCount = delRows?.length ?? 0;
    } else {
      let staleQuery = adminClient
        .from("user_devices")
        .update({
          is_active: false,
          last_error: `Device inactive for > ${staleDays} days`,
          invalidated_at: nowIso,
          updated_at: nowIso,
        })
        .eq("is_active", true)
        .lt("last_seen_at", cutoffIso);

      if (scopedUserId) staleQuery = staleQuery.eq("user_id", scopedUserId);
      const { data: staleRows } = await staleQuery.select("id");
      staleCleanedCount = staleRows?.length ?? 0;
    }

    return new Response(
      JSON.stringify({
        success: true,
        invalidated_by_token: invalidatedByTokenCount,
        invalidated_by_device_id: invalidatedByDeviceCount,
        stale_devices_cleaned: staleCleanedCount,
        stale_cutoff: cutoffIso,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unexpected error in cleanup-invalid-device-tokens";
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
