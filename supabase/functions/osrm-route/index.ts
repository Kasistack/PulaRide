// Supabase Edge Function: osrm-route
// POST /functions/v1/osrm-route
// Body: { "pickup": [lng,lat], "dropoff": [lng,lat] }
// Returns road geometry + distance + duration from YOUR self-hosted OSRM.
//
// SECURITY/PRIVACY: user origin/destination pairs never go to a third-party
// demo server. Point OSRM_BASE_URL at an OSRM instance you control.
// No auth required (route geometry is not sensitive), but rate-limit at the
// edge if exposed publicly.

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const OSRM_BASE = Deno.env.get("OSRM_BASE_URL") ?? "https://router.project-osrm.org/";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const body = await req.json().catch(() => ({}));
    const p = body.pickup, d = body.dropoff;
    if (!Array.isArray(p) || !Array.isArray(d) || p.length !== 2 || d.length !== 2) {
      return json(400, { error: "BAD_INPUT" });
    }
    const coords = `${p[0]},${p[1]};${d[0]},${d[1]}`;
    const url = `${OSRM_BASE}route/v1/driving/${coords}?overview=full&geometries=geojson`;
    const r = await fetch(url, { headers: { "content-type": "application/json" } });
    const j = await r.json().catch(() => ({}));
    if (j?.code !== "Ok" || !j.routes?.length) {
      return json(502, { error: "OSRM_FAILED", detail: j });
    }
    const route = j.routes[0];
    return json(200, {
      distance_km: (route.distance ?? 0) / 1000,
      duration_min: (route.duration ?? 0) / 60,
      geometry: route.geometry,
    }, cors);
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
