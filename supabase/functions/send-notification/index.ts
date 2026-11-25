import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const VALID_TYPES = new Set(["message", "update", "reminder", "reply", "mention", "system"]);

interface SendNotificationPayload {
  recipient_user_id?: string;
  title?: string;
  message?: string;
  type?: string;
  metadata?: Record<string, unknown>;
  dedupe_key?: string;
  deliver_remote_push?: boolean;
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

    const body: SendNotificationPayload = await req.json().catch(() => ({}));
    const recipientUserId = (body.recipient_user_id ?? user.id).trim();
    const title = (body.title ?? "").trim();
    const message = (body.message ?? "").trim();
    const type = (body.type ?? "message").trim().toLowerCase();
    const metadata = body.metadata && typeof body.metadata === "object" ? body.metadata : {};
    const dedupeKey = body.dedupe_key ? String(body.dedupe_key).trim() : null;
    const deliverRemotePush = body.deliver_remote_push !== false;

    if (!title || title.length > 200) {
      return new Response(
        JSON.stringify({ error: "Invalid title: must be 1–200 characters" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!message || message.length > 2000) {
      return new Response(
        JSON.stringify({ error: "Invalid message: must be 1–2000 characters" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!VALID_TYPES.has(type)) {
      return new Response(
        JSON.stringify({ error: "Invalid notification type: must be message, update, reminder, reply, mention, or system" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const adminClient = createClient(supabaseUrl, supabaseServiceKey);

    // Check recipient's notification preferences
    const { data: recipientSettings } = await adminClient
      .from("user_notification_settings")
      .select("*")
      .eq("user_id", recipientUserId)
      .maybeSingle();

    if (recipientSettings) {
      if (recipientSettings.master_enabled === false) {
        return new Response(
          JSON.stringify({
            success: true,
            skipped: true,
            reason: "recipient_master_notifications_disabled",
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      if (
        (type === "message" || type === "reply" || type === "mention") &&
        recipientSettings.messages_enabled === false
      ) {
        return new Response(
          JSON.stringify({
            success: true,
            skipped: true,
            reason: "recipient_messages_category_disabled",
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      if (type === "update" && recipientSettings.updates_enabled === false) {
        return new Response(
          JSON.stringify({
            success: true,
            skipped: true,
            reason: "recipient_updates_category_disabled",
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      if (type === "reminder" && recipientSettings.reminders_enabled === false) {
        return new Response(
          JSON.stringify({
            success: true,
            skipped: true,
            reason: "recipient_reminders_category_disabled",
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    }

    // Deduplication check if dedupe_key is provided
    if (dedupeKey) {
      const { data: existingNotif } = await adminClient
        .from("notifications")
        .select("*")
        .eq("user_id", recipientUserId)
        .eq("dedupe_key", dedupeKey)
        .maybeSingle();

      if (existingNotif) {
        return new Response(
          JSON.stringify({
            success: true,
            deduplicated: true,
            notification: existingNotif,
          }),
          { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    }

    // Insert notification record into public.notifications
    const { data: notification, error: insertError } = await adminClient
      .from("notifications")
      .insert({
        user_id: recipientUserId,
        sender_user_id: user.id,
        title,
        message,
        type,
        is_read: false,
        metadata: {
          ...metadata,
          sender_user_id: user.id,
        },
        dedupe_key: dedupeKey,
      })
      .select("*")
      .single();

    if (insertError) {
      return new Response(
        JSON.stringify({ error: "Failed to create notification", details: insertError.message }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Trigger remote push delivery via deliver-push Edge Function if enabled
    let pushDeliveryResult: unknown = null;
    if (deliverRemotePush) {
      try {
        const deliverUrl = `${supabaseUrl}/functions/v1/deliver-push`;
        const deliverResp = await fetch(deliverUrl, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${supabaseServiceKey}`,
          },
          body: JSON.stringify({
            notification_id: notification.id,
            user_id: recipientUserId,
            title,
            message,
            type,
            metadata: notification.metadata,
          }),
        });
        pushDeliveryResult = await deliverResp.json().catch(() => null);
      } catch (pushErr) {
        pushDeliveryResult = {
          delivered: false,
          error: pushErr instanceof Error ? pushErr.message : "deliver-push invocation failed",
        };
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        notification,
        push_delivery: pushDeliveryResult,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unexpected error in send-notification";
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
