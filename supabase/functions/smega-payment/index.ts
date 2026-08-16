// Supabase Edge Function: smega-payment
// POST /functions/v1/smega-payment
// Body: { "amount": number, "payerId": string, "pin": string, "kind": "TOPUP" | "CHARGE", "ref": string? }
// Authorization: Bearer <user_jwt>
//
// SECURITY: the Smega merchant secret lives ONLY here as a project secret
// (SMEGA_API_KEY / SMEGA_APP_ID / SMEGA_SECRET_TOKEN). The client never sees it.
// After Smega authorises, we credit/debit the user's Supabase wallet via the
// SECURITY DEFINER functions credit_wallet / debit_wallet. The wallet balance
// is the single source of truth — never client-supplied.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const SMEGA_URL = "https://smegaapi.btc.bw/api/transact/jsonTxn";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const auth = req.headers.get("Authorization") ?? "";
    const token = auth.replace(/^Bearer\s+/i, "");
    if (!token) return json(401, { error: "NO_AUTH" });

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
      { auth: { persistSession: false } },
    );
    const {
      data: { user },
      error: ue,
    } = await supabase.auth.getUser(token);
    if (ue || !user) return json(401, { error: "BAD_TOKEN" });

    const body = await req.json().catch(() => ({}));
    const amount = Number(body.amount);
    const payerId = String(body.payerId ?? "").trim();
    const pin = String(body.pin ?? "");
    const kind = body.kind === "CHARGE" ? "CHARGE" : "TOPUP";
    const ref = body.ref ? String(body.ref) : `smega_${Date.now()}`;

    if (!(amount > 0) || !/^\d{7,}$/.test(payerId) || !pin) {
      return json(400, { error: "BAD_INPUT" });
    }

    // 1) Call Smega with the SERVER-HELD merchant secret.
    const smegaRes = await fetch(SMEGA_URL, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        merchantId: {
          apiKey: Deno.env.get("SMEGA_API_KEY"),
          appId: Deno.env.get("SMEGA_APP_ID"),
          secretToken: Deno.env.get("SMEGA_SECRET_TOKEN"),
        },
        customer: { payerId, pin, amount },
        gatewayId: "SMEGA",
      }),
    });
    const smega = await smegaRes.json().catch(() => ({}));
    if (smega?.txnStatus !== "AUTHORIZED") {
      return json(402, {
        error: "SMEGA_NOT_AUTHORIZED",
        message: smega?.message ?? "Payment not authorized",
      });
    }

    // 2) Update the wallet through the secure function (owner-checked).
    const fn = kind === "TOPUP" ? "credit_wallet" : "debit_wallet";
    const { data: balance, error: we } = await supabase.rpc(fn, {
      p_user: user.id,
      p_amount: amount,
      p_kind: kind,
      p_ref: ref,
    });
    if (we) return json(400, { error: we.message });

    return json(200, { status: "SUCCESS", balance, ref }, cors);
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
