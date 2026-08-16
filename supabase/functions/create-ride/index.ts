// Supabase Edge Function: create-ride
// POST /functions/v1/create-ride
// Body: { "requestId": uuid, "bidId": uuid }
// Authorization: Bearer <user_jwt>  (must be the DRIVER who owns the bid)
//
// This is the ONLY way a ride row is created. It calls the SECURITY DEFINER
// SQL function create_ride(), which validates the request/bid/fare and pins
// the rider + fare server-side. The Smega charge (if the ride is paid via
// wallet/Smega) is handled by the smega-payment function before this is called.
//
// Requires env (project secrets): SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const auth = req.headers.get("Authorization") ?? "";
    const token = auth.replace(/^Bearer\s+/i, "");
    if (!token) return json(401, { error: "NO_AUTH" });

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, // service role: bypasses RLS for the write
      { auth: { persistSession: false } },
    );

    // Verify the caller and get their uid.
    const {
      data: { user },
      error: ue,
    } = await supabase.auth.getUser(token);
    if (ue || !user) return json(401, { error: "BAD_TOKEN" });
    const driverId = user.id;

    const body = await req.json().catch(() => ({}));
    const requestId = body.requestId;
    const bidId = body.bidId;
    if (!requestId || !bidId) return json(400, { error: "MISSING_FIELDS" });

    // Run the server-side validator. It raises SQLSTATE 'P0001' with message
    // RIDE_REQUEST_NOT_FOUND / BID_NOT_FOUND_OR_MISMATCH / BID_ALREADY_USED.
    const { data, error } = await supabase.rpc("create_ride", {
      p_request_id: requestId,
      p_bid_id: bidId,
      p_driver_id: driverId,
    });
    if (error) {
      const code = mapError(error.message);
      return json(code, { error: error.message });
    }
    return json(200, { ride: data }, cors);
  } catch (e) {
    return json(500, { error: String(e) }, cors);
  }
});

function json(status: number, body: unknown, extra?: Record<string, string>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", ...cors, ...(extra ?? {}) },
  });
}

function mapError(msg: string): number {
  switch (msg) {
    case "RIDE_REQUEST_NOT_FOUND":
    case "BID_NOT_FOUND_OR_MISMATCH":
      return 404;
    case "BID_ALREADY_USED":
      return 409;
    default:
      return 400;
  }
}
