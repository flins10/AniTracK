// Supabase Edge Function: delete-account
// Deploy with: supabase functions deploy delete-account --no-verify-jwt
// Required environment variables (automatically injected by Supabase Runtime):
//   SUPABASE_URL
//   SUPABASE_ANON_KEY
//   SUPABASE_SERVICE_ROLE_KEY (Server-side only — NEVER expose in client code)

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.7";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function jsonResponse(body: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

async function sha256Hex(input: string): Promise<string> {
  const normalized = input.trim().toLowerCase();
  const data = new TextEncoder().encode(normalized);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed. Use POST." }, 405);
  }

  try {
    const authHeader = req.headers.get("Authorization") ?? "";
    if (!authHeader.toLowerCase().startsWith("bearer ")) {
      return jsonResponse({ error: "Missing or invalid Authorization Bearer token" }, 401);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

    if (!supabaseUrl || !supabaseAnonKey || !supabaseServiceRoleKey) {
      return jsonResponse({ error: "Server configuration incomplete" }, 500);
    }

    // 1. Verify the caller's JWT token using the user-scoped client (NEVER trust a client-supplied user_id)
    const userClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
      auth: { persistSession: false, autoRefreshToken: false },
    });

    const { data: userData, error: userError } = await userClient.auth.getUser();
    if (userError || !userData?.user) {
      return jsonResponse({ error: "Unauthorized or expired session token" }, 401);
    }

    const userId = userData.user.id;
    const userEmail = (userData.user.email ?? "").trim().toLowerCase();

    // 2. Create privileged Admin client (service_role key stays strictly in this Edge Function)
    const adminClient = createClient(supabaseUrl, supabaseServiceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    // 3. Look up profile username before deleting so we can record cooldowns
    const { data: profileRow } = await adminClient
      .from("profiles")
      .select("username")
      .eq("id", userId)
      .maybeSingle();

    const usernameLower = (
      profileRow?.username ??
      (userData.user.user_metadata?.username as string | undefined) ??
      ""
    )
      .trim()
      .toLowerCase();

    // 4. Record 5-minute email cooldown & 24-hour username cooldown in deleted_account_cooldowns
    const nowMs = Date.now();
    const emailAvailableAt = new Date(nowMs + 5 * 60 * 1000).toISOString();
    const usernameAvailableAt = new Date(nowMs + 24 * 60 * 60 * 1000).toISOString();
    const emailHash = userEmail ? await sha256Hex(userEmail) : null;

    if (emailHash) {
      await adminClient.from("deleted_account_cooldowns").delete().eq("email_hash", emailHash);
    }
    if (usernameLower) {
      await adminClient.from("deleted_account_cooldowns").delete().eq("username_lower", usernameLower);
    }
    if (emailHash || usernameLower) {
      await adminClient.from("deleted_account_cooldowns").insert({
        email_hash: emailHash,
        email_available_at: emailAvailableAt,
        username_lower: usernameLower || null,
        username_available_at: usernameAvailableAt,
        deleted_user_id: userId,
      });
    }

    // 5. Delete all user files in Supabase Storage bucket "avatars" under `<userId>/`
    let deletedStorageFiles = 0;
    const { data: avatarFiles } = await adminClient.storage.from("avatars").list(userId, {
      limit: 100,
      offset: 0,
    });
    if (avatarFiles && avatarFiles.length > 0) {
      const pathsToRemove = avatarFiles
        .filter((f) => f.name && f.name !== ".emptyFolderPlaceholder")
        .map((f) => `${userId}/${f.name}`);
      if (pathsToRemove.length > 0) {
        const { error: removeError } = await adminClient.storage
          .from("avatars")
          .remove(pathsToRemove);
        if (!removeError) {
          deletedStorageFiles = pathsToRemove.length;
        }
      }
    }

    // 6. Delete user-owned rows across all personal tables (preserving shared `public.anime` catalog)
    const { data: userCollections } = await adminClient
      .from("collections")
      .select("id")
      .eq("creator_id", userId);

    if (userCollections && userCollections.length > 0) {
      const collectionIds = userCollections.map((c) => c.id);
      await adminClient.from("collection_anime").delete().in("collection_id", collectionIds);
    }

    await adminClient.from("collections").delete().eq("creator_id", userId);
    await adminClient.from("user_anime").delete().eq("user_id", userId);
    await adminClient.from("privacy_settings").delete().eq("user_id", userId);
    await adminClient.from("community_members").delete().eq("user_id", userId);
    await adminClient.from("notifications").delete().eq("user_id", userId);
    await adminClient.from("user_devices").delete().eq("user_id", userId);
    await adminClient.from("user_notification_settings").delete().eq("user_id", userId);
    await adminClient.from("user_notifications").delete().eq("recipient_user_id", userId);
    await adminClient.from("user_push_tokens").delete().eq("user_id", userId);
    await adminClient.from("invitations").update({ created_by: null }).eq("created_by", userId);
    await adminClient.from("profiles").delete().eq("id", userId);

    // 7. Permanently delete the user from Supabase Auth (`auth.users`, `auth.identities`, `auth.sessions`)
    const { error: deleteAuthError } = await adminClient.auth.admin.deleteUser(userId);
    if (deleteAuthError && !deleteAuthError.message.toLowerCase().includes("not found")) {
      return jsonResponse(
        {
          error: `Failed to delete authentication user: ${deleteAuthError.message}`,
          deleted_data: true,
          deleted_auth_user: false,
        },
        500,
      );
    }

    return jsonResponse({
      deleted: true,
      deleted_data: true,
      deleted_auth_user: true,
      deleted_storage_files: deletedStorageFiles,
      user_id: userId,
      email_cooldown_seconds: 300,
      username_cooldown_seconds: 86400,
    });
  } catch (err) {
    return jsonResponse({ error: String(err) }, 500);
  }
});
