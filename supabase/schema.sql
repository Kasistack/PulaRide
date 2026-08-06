-- PulaRide Supabase schema (free tier, Postgres)
-- Run this in Supabase Dashboard -> SQL Editor.

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
  last_updated timestamptz default now()
);

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

-- Accepted rides
create table if not exists public.rides (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.ride_requests(id) on delete cascade,
  driver_id uuid not null references auth.users(id) on delete cascade,
  rider_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'EN_ROUTE_PICKUP',
  fare double precision not null,
  created_at timestamptz default now()
);

-- In-ride chat messages
create table if not exists public.messages (
  id uuid primary key default gen_random_uuid(),
  ride_id uuid not null references public.rides(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  sender_name text,
  body text not null,
  created_at timestamptz default now()
);

-- Row Level Security: users can read drivers & bids, manage only their own rows
alter table public.drivers enable row level security;
alter table public.ride_requests enable row level security;
alter table public.bids enable row level security;
alter table public.rides enable row level security;
alter table public.messages enable row level security;

create policy "drivers_read" on public.drivers for select using (true);
create policy "drivers_write" on public.drivers for all using (auth.uid() = user_id);

create policy "requests_read" on public.ride_requests for select using (true);
create policy "requests_write" on public.ride_requests for all using (auth.uid() = rider_id);

create policy "bids_read" on public.bids for select using (true);
create policy "bids_write" on public.bids for all using (auth.uid() = driver_id);

create policy "rides_read" on public.rides for select using (true);
create policy "rides_write" on public.rides for all using (auth.uid() = rider_id or auth.uid() = driver_id);

create policy "messages_read" on public.messages for select using (true);
create policy "messages_write" on public.messages for insert with check (auth.uid() = sender_id);

-- Enable email OTP sign-in (no SMS cost)
-- Supabase -> Authentication -> Providers: enable "Email" (keep "Confirm email" OFF for OTP).
