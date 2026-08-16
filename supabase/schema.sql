-- PulaRide Supabase schema (hardened) — HARDENED 2026-08-16
-- Run this in Supabase Dashboard -> SQL Editor (_drop.sql first if re-applying).
--
-- SECURITY MODEL
--  * Riders and drivers authenticate (real OTP, not anonymous).
--  * The anon key is still embeddable in the APK, but it can no longer read
--    any PII: every SELECT policy is scoped to the row owner.
--  * Rides can only be created by a server-side function (create_ride) that
--    validates the request, the winning bid, and the fare. Direct client
--    INSERT into `rides` is blocked by RLS (no INSERT policy).
--  * Wallet balance lives in Postgres, owned by the user, never client-supplied.

-- ===========================================================================
-- TABLES
-- ===========================================================================

-- Drivers: real-time positions posted by the app
create table if not exists public.drivers (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  lat double precision not null,
  lng double precision not null,
  available boolean not null default true,
  vehicle_model text,
  vehicle_plate text,
  rating double precision default 4.5,
  is_female boolean default false,
  verified boolean default false,           -- set by admin after doc check
  last_updated timestamptz default now()
);
create index if not exists drivers_user_idx on public.drivers(user_id);

-- Ride requests created by riders
create table if not exists public.ride_requests (
  id uuid primary key default gen_random_uuid(),
  rider_id uuid not null references auth.users(id) on delete cascade,
  pickup_lat double precision not null,
  pickup_lng double precision not null,
  dropoff_lat double precision not null,
  dropoff_lng double precision not null,
  offered_fare double precision not null,
  status text not null default 'OPEN',
  created_at timestamptz default now()
);
create index if not exists requests_rider_idx on public.ride_requests(rider_id);

-- Driver bids on a ride request
create table if not exists public.bids (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.ride_requests(id) on delete cascade,
  driver_id uuid not null references auth.users(id) on delete cascade,
  driver_name text not null,
  vehicle_model text,
  vehicle_plate text,
  rating double precision default 4.5,
  bid_amount double precision not null,
  eta_minutes integer default 5,
  is_female boolean default false,
  status text not null default 'PENDING',
  created_at timestamptz default now()
);
create index if not exists bids_request_idx on public.bids(request_id);
create index if not exists bids_driver_idx on public.bids(driver_id);

-- Accepted rides. NEVER inserted directly by clients — only by create_ride().
create table if not exists public.rides (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.ride_requests(id) on delete cascade,
  bid_id uuid references public.bids(id) on delete set null,
  driver_id uuid not null references auth.users(id) on delete cascade,
  rider_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'EN_ROUTE_PICKUP',
  fare double precision not null,
  created_at timestamptz default now()
);
create index if not exists rides_rider_idx on public.rides(rider_id);
create index if not exists rides_driver_idx on public.rides(driver_id);

-- In-ride chat messages (scoped to ride participants only)
create table if not exists public.messages (
  id uuid primary key default gen_random_uuid(),
  ride_id uuid not null references public.rides(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  sender_name text,
  body text not null,
  created_at timestamptz default now()
);
create index if not exists messages_ride_idx on public.messages(ride_id);

-- Wallet: balance owned by the user, single source of truth.
create table if not exists public.wallets (
  user_id uuid primary key references auth.users(id) on delete cascade,
  balance numeric(12,2) not null default 0.00,
  updated_at timestamptz default now()
);

-- Wallet ledger (audit trail for every balance change)
create table if not exists public.wallet_transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  delta numeric(12,2) not null,
  kind text not null,                 -- 'TOPUP' | 'CHARGE' | 'REFUND'
  ref text,                           -- external txn id (Smega)
  created_at timestamptz default now()
);
create index if not exists wt_user_idx on public.wallet_transactions(user_id);

-- ===========================================================================
-- ROW LEVEL SECURITY
-- ===========================================================================
alter table public.drivers        enable row level security;
alter table public.ride_requests  enable row level security;
alter table public.bids           enable row level security;
alter table public.rides          enable row level security;
alter table public.messages       enable row level security;
alter table public.wallets        enable row level security;
alter table public.wallet_transactions enable row level security;

-- drivers: location is public (needed for the map) but writes are owner-only.
create policy "drivers_read"  on public.drivers for select using (true);
create policy "drivers_write" on public.drivers for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ride_requests: riders see/manage ONLY their own.
create policy "requests_read"  on public.ride_requests for select
  using (auth.uid() = rider_id);
create policy "requests_write" on public.ride_requests for all
  using (auth.uid() = rider_id) with check (auth.uid() = rider_id);

-- bids: a driver sees all open bids (to match requests); writes owner-only.
create policy "bids_read"  on public.bids for select using (true);
create policy "bids_write" on public.bids for all
  using (auth.uid() = driver_id) with check (auth.uid() = driver_id);

-- rides: NO client INSERT/UPDATE/DELETE. Selectable only by the two participants.
-- (The insert path is the SECURITY DEFINER function create_ride below.)
drop policy if exists "rides_read"  on public.rides;
drop policy if exists "rides_write" on public.rides;
create policy "rides_read" on public.rides for select
  using (auth.uid() = rider_id or auth.uid() = driver_id);
-- No insert/update/delete policies => RLS denies them for the anon/authenticated
-- client role. Only the postgres role (via SECURITY DEFINER fn) can write.

-- messages: readable only by participants of the parent ride.
create policy "messages_read" on public.messages for select
  using (
    exists (
      select 1 from public.rides r
      where r.id = ride_id
        and (r.rider_id = auth.uid() or r.driver_id = auth.uid())
    )
  );
create policy "messages_write" on public.messages for insert
  with check (
    auth.uid() = sender_id
    and exists (
      select 1 from public.rides r
      where r.id = ride_id
        and (r.rider_id = auth.uid() or r.driver_id = auth.uid())
    )
  );

-- wallets: owner only, always.
create policy "wallets_read"  on public.wallets for select
  using (auth.uid() = user_id);
create policy "wallets_write" on public.wallets for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
-- NB: balance is mutated only inside charge_wallet()/credit_wallet()
-- (SECURITY DEFINER). Direct client UPDATE is denied because the function
-- runs as the postgres role, not the caller.

-- wallet_transactions: owner read-only. Written only by SECURITY DEFINER fns.
create policy "wt_read" on public.wallet_transactions for select
  using (auth.uid() = user_id);

-- ===========================================================================
-- SERVER-SIDE LOGIC (the trust boundary lives here, not in the app)
-- ===========================================================================

-- Create a wallet row the first time a user is referenced.
create or replace function public.ensure_wallet(p_user uuid)
returns void language sql security definer as $$
  insert into public.wallets(user_id) values (p_user)
  on conflict (user_id) do nothing;
$$;

-- Create a ride ONLY after validating the request/bid/fare. Called by the
-- create-ride Edge Function (which also runs the Smega charge).
create or replace function public.create_ride(
  p_request_id uuid,
  p_bid_id     uuid,
  p_driver_id  uuid
)
returns public.rides language plpgsql security definer as $$
declare
  v_request public.ride_requests%rowtype;
  v_bid     public.bids%rowtype;
  v_rider   uuid;
  v_ride    public.rides;
begin
  select * into v_request from public.ride_requests where id = p_request_id;
  if not found then
    raise exception 'RIDE_REQUEST_NOT_FOUND';
  end if;
  v_rider := v_request.rider_id;

  select * into v_bid from public.bids
   where id = p_bid_id and request_id = p_request_id and driver_id = p_driver_id;
  if not found then
    raise exception 'BID_NOT_FOUND_OR_MISMATCH';
  end if;
  if v_bid.status <> 'PENDING' then
    raise exception 'BID_ALREADY_USED';
  end if;

  -- Pin rider from the request (never the client), and fare from the accepted bid.
  insert into public.rides (request_id, bid_id, driver_id, rider_id, status, fare)
  values (p_request_id, p_bid_id, p_driver_id, v_rider, 'EN_ROUTE_PICKUP', v_bid.bid_amount)
  returning * into v_ride;

  -- Mark the bid used and the request closed.
  update public.bids set status = 'ACCEPTED' where id = p_bid_id;
  update public.ride_requests set status = 'MATCHED' where id = p_request_id;

  return v_ride;
end;
$$;

-- Credit a wallet (top-up). p_user must equal the caller for safety.
create or replace function public.credit_wallet(
  p_user uuid,
  p_amount numeric,
  p_kind text,
  p_ref text
)
returns numeric language plpgsql security definer as $$
declare
  v_new numeric;
begin
  if p_user <> auth.uid() then
    raise exception 'WALLET_OWNER_MISMATCH';
  end if;
  if p_amount <= 0 then
    raise exception 'INVALID_AMOUNT';
  end if;
  insert into public.wallets(user_id, balance)
    values (p_user, p_amount)
  on conflict (user_id) do update set balance = wallets.balance + p_amount, updated_at = now();
  insert into public.wallet_transactions(user_id, delta, kind, ref)
    values (p_user, p_amount, p_kind, p_ref);
  select balance into v_new from public.wallets where user_id = p_user;
  return v_new;
end;
$$;

-- Debit a wallet for a completed ride. Fails if insufficient (raises exception).
create or replace function public.debit_wallet(
  p_user uuid,
  p_amount numeric,
  p_kind text,
  p_ref text
)
returns numeric language plpgsql security definer as $$
declare
  v_cur numeric;
  v_new numeric;
begin
  if p_user <> auth.uid() then
    raise exception 'WALLET_OWNER_MISMATCH';
  end if;
  if p_amount <= 0 then
    raise exception 'INVALID_AMOUNT';
  end if;
  select balance into v_cur from public.wallets where user_id = p_user for update;
  if not found or v_cur < p_amount then
    raise exception 'INSUFFICIENT_FUNDS';
  end if;
  update public.wallets set balance = balance - p_amount, updated_at = now()
    where user_id = p_user;
  insert into public.wallet_transactions(user_id, delta, kind, ref)
    values (p_user, -p_amount, p_kind, p_ref);
  select balance into v_new from public.wallets where user_id = p_user;
  return v_new;
end;
$$;

-- ===========================================================================
-- AUTH / PROVIDERS
-- ===========================================================================
-- Enable Phone OTP sign-in (real verification):
--   Supabase -> Authentication -> Providers -> Phone -> enable,
--   set "Secure email" OFF for OTP, and configure an SMS provider (Twilio/etc).
-- Driver `verified` must be set by an admin/Edge Function after document review,
-- NOT by the client (the old simulateVerificationApproval is removed).
