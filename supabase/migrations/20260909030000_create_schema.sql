-- Muster: core tables, constraints, indexes.
-- RLS policies in migration 2; functions and triggers in migration 3.
-- RLS is enabled here with no policies, so every table is closed until
-- migration 2 lands.

-- profiles ------------------------------------------------------------
create table profiles (
  id    uuid primary key references auth.users (id) on delete cascade,
  name  text not null check (length(trim(name)) > 0),
  email text not null unique check (email = lower(trim(email))),
  -- Signup is open; creating groups is not. Flipped by hand in the
  -- dashboard for the few people who organise.
  can_create_groups boolean not null default false
);

-- groups --------------------------------------------------------------
create table groups (
  id         uuid primary key default gen_random_uuid(),
  name       text not null check (length(trim(name)) > 0),
  created_by uuid not null references profiles (id),
  created_at timestamptz not null default now(),
  -- Soft delete. Null means live. An archived group is hidden and
  -- frozen, but its members, events and RSVPs all survive so it can be
  -- brought back from the dashboard.
  archived_at timestamptz
);

-- group_members -------------------------------------------------------
create table group_members (
  group_id   uuid not null references groups (id)   on delete cascade,
  profile_id uuid not null references profiles (id) on delete cascade,
  role       text not null check (role in ('admin', 'member')),
  joined_at  timestamptz not null default now(),
  primary key (group_id, profile_id)
);

create index on group_members (profile_id);

-- group_invitations ---------------------------------------------------
create table group_invitations (
  id           uuid primary key default gen_random_uuid(),
  group_id     uuid not null references groups (id) on delete cascade,
  email        text not null check (email = lower(trim(email))),
  invited_by   uuid not null references profiles (id),
  status       text not null default 'pending'
               check (status in ('pending', 'accepted', 'declined')),
  created_at   timestamptz not null default now()
);

-- one open invite per person per group
create unique index group_invitations_one_pending
  on group_invitations (group_id, email)
  where status = 'pending';

create index on group_invitations (email) where status = 'pending';
create index on group_invitations (group_id);

-- events --------------------------------------------------------------
create table events (
  id         uuid primary key default gen_random_uuid(),
  group_id   uuid not null references groups (id) on delete cascade,
  title      text not null check (length(trim(title)) > 0),
  starts_at  timestamptz not null,
  location   text,
  -- ceiling on invitations, not on confirmations
  capacity   int not null check (capacity > 0),
  created_by uuid not null references profiles (id),
  created_at timestamptz not null default now(),
  -- lets child tables reference (id, group_id) as a pair, so their
  -- denormalised group_id cannot drift from the event's
  unique (id, group_id)
);

create index on events (group_id, starts_at);

-- event_invitations (event-level RSVP) --------------------------------
create table event_invitations (
  id           uuid primary key default gen_random_uuid(),
  event_id     uuid not null,
  group_id     uuid not null,
  profile_id   uuid not null,
  status       text not null default 'pending'
               check (status in ('pending', 'in', 'out')),

  unique (event_id, profile_id),

  foreign key (event_id, group_id)
    references events (id, group_id) on delete cascade,

  -- removing someone from a group wipes their RSVPs in that group
  foreign key (group_id, profile_id)
    references group_members (group_id, profile_id) on delete cascade
);

create index on event_invitations (group_id, profile_id);
-- occupancy lookups: pending + in per event
create index on event_invitations (event_id)
  where status in ('pending', 'in');

-- event_standby -------------------------------------------------------
-- Ordered queue. A standby player has no event_invitations row and no
-- status.
create table event_standby (
  event_id   uuid not null,
  group_id   uuid not null,
  profile_id uuid not null,
  position   int  not null check (position > 0),
  added_at   timestamptz not null default now(),

  primary key (event_id, profile_id),

  foreign key (event_id, group_id)
    references events (id, group_id) on delete cascade,

  foreign key (group_id, profile_id)
    references group_members (group_id, profile_id) on delete cascade
);

-- deferrable so a reorder can renumber rows within one transaction
create unique index event_standby_position
  on event_standby (event_id, position);
alter table event_standby
  add constraint event_standby_position_unique
  unique using index event_standby_position deferrable initially deferred;

create index on event_standby (group_id, profile_id);

-- RLS ------------------------------------------------------------------
alter table profiles          enable row level security;
alter table groups            enable row level security;
alter table group_members     enable row level security;
alter table group_invitations enable row level security;
alter table events            enable row level security;
alter table event_invitations enable row level security;
alter table event_standby     enable row level security;
