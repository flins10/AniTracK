import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, DELETE, OPTIONS",
};

interface UnregisterDevicePayload {
  device_id?: string;
  push_token?: string;
  hard_delete?: boolean;
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST" && req.method !== "DELETE") {
    return new Response(
      JSON.stringify({ error: "Method not allowed. Use POST or DELETE." }),
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

    const body: UnregisterDevicePayload = await req.json().catch(() => ({}));
    const deviceId = (body.device_id ?? "").trim();
    const pushToken = (body.push_token ?? "").trim();
    const hardDelete = Boolean(body.hard_delete);

    if (!deviceId && !pushToken) {
      return new Response(
        JSON.stringify({ error: "Either device_id or push_token is required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const adminClient = createClient(supabaseUrl, supabaseServiceKey);

    if (hardDelete) {
      let query = adminClient.from("user_devices").delete().eq("user_id", user.id);
      if (deviceId) query = query.eq("device_id", deviceId);
      if (pushToken) query = query.eq("push_token", pushToken);
      const { error: deleteErr } = await query;

      if (deleteErr) {
        return new Response(
          JSON.stringify({ error: "Failed to delete device registration", details: deleteErr.message }),
          { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    } else {
      const nowIso = new Date().toISOString();
      let query = adminClient
        .from("user_devices")
        .update({
          is_active: false,
          push_token: null,
          updated_at: nowIso,
        })
        .eq("user_id", user.id);

      if (deviceId) query = query.eq("device_id", deviceId);
      if (pushToken) query = query.eq("push_token", pushToken);
      const { error: updateErr } = await query;

      if (updateErr) {
        return new Response(
          JSON.stringify({ error: "Failed to deactivate device registration", details: updateErr.message }),
          { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        user_id: user.id,
        device_id: deviceId || null,
        unregistered: true,
        hard_deleted: hardDelete,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unexpected error in unregister-device";
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
