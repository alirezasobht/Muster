-- Muster: functions and triggers.
-- Depends on migrations 1 and 2.
--
-- Ordering note: several triggers here are CONSTRAINT TRIGGERs marked
-- DEFERRABLE INITIALLY DEFERRED. They fire at COMMIT rather than per
-- statement, which is what makes multi-step operations work — a queue
-- reorder passes through invalid intermediate states, and a cascade
-- delete arrives after the row that caused it is already gone.

-- ---------------------------------------------------------------------
-- Profile creation (rule 10)
-- ---------------------------------------------------------------------
create function handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into profiles (id, name, email)
  values (
    new.id,
    coalesce(
      nullif(trim(new.raw_user_meta_data ->> 'name'), ''),
      split_part(new.email, '@', 1)
    ),
    lower(trim(new.email))
  );
  return new;
end;
$$;

create trigger on_auth_user_created
after insert on auth.users
for each row execute function handle_new_user();

-- Supabase Auth has its own email-change flow, outside these tables. If
-- an address changed there and profiles kept the old one, invitation
-- matching would silently target an address the user no longer owns.
-- The app does not offer email change; this keeps the dashboard and the
-- Auth API honest if it happens anyway.
create function sync_user_email()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  update profiles
  set email = lower(trim(new.email))
  where id = new.id;
  return new;
end;
$$;

create trigger on_auth_user_email_changed
after update of email on auth.users
for each row
when (new.email is distinct from old.email)
execute function sync_user_email();

-- ---------------------------------------------------------------------
-- Group creation: creator becomes first admin (rule 9)
-- ---------------------------------------------------------------------
create function handle_new_group()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into group_members (group_id, profile_id, role)
  values (new.id, new.created_by, 'admin');
  return new;
end;
$$;

create trigger on_group_created
after insert on groups
for each row execute function handle_new_group();

-- ---------------------------------------------------------------------
-- A group must keep at least one admin (rule 2)
-- ---------------------------------------------------------------------
-- Deferred, so a transaction may promote someone and demote the old
-- admin in either order. The groups-row check lets group deletion
-- cascade through without tripping this.
create function ensure_admin_remains()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  gid uuid := coalesce(new.group_id, old.group_id);
begin
  -- Serialises membership changes within a group. Deferring the check
  -- to commit makes multi-step changes possible but does not serialise
  -- transactions: with two admins, two concurrent demotions each still
  -- see the other's uncommitted admin row, both pass, and the group is
  -- left with none.
  perform 1 from groups where id = gid for update;
  if not found then
    return null;
  end if;

  if not exists (
    select 1 from group_members
    where group_id = gid and role = 'admin'
  ) then
    raise exception 'a group must keep at least one admin';
  end if;

  return null;
end;
$$;

create constraint trigger group_keeps_an_admin
after update or delete on group_members
deferrable initially deferred
for each row execute function ensure_admin_remains();

-- ---------------------------------------------------------------------
-- Invitation accept / decline (rule 8)
-- ---------------------------------------------------------------------
-- security definer because the invitee has no membership row yet, so no
-- RLS route to insert one. The guard below is therefore the whole
-- security model: a pending invitation whose email is the caller's.
--
-- FOR SHARE on the group, not just the live check: a plain check reads
-- a snapshot, so an archive committing in between would let acceptance
-- write membership into an archived group. FOR SHARE conflicts with the
-- UPDATE that archives, so the two serialise. Lock order is always
-- invitation then group.
create function accept_group_invitation(invitation_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  inv group_invitations;
begin
  select gi.* into inv
  from group_invitations gi
  join groups g on g.id = gi.group_id
  where gi.id = invitation_id
    and gi.status = 'pending'
    and g.archived_at is null
    and gi.email = (select email from profiles where id = auth.uid())
  for update of gi
  for share of g;

  if not found then
    raise exception 'no pending invitation for you';
  end if;

  insert into group_members (group_id, profile_id, role)
  values (inv.group_id, auth.uid(), 'member')
  on conflict do nothing;

  update group_invitations
  set status = 'accepted'
  where id = inv.id;
end;
$$;

create function decline_group_invitation(invitation_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  inv group_invitations;
begin
  -- Same shape as accept, and for the same reason: FOR SHARE on the
  -- group conflicts with the UPDATE that archives it, so the live check
  -- cannot be overtaken between reading and writing.
  select gi.* into inv
  from group_invitations gi
  join groups g on g.id = gi.group_id
  where gi.id = invitation_id
    and gi.status = 'pending'
    and g.archived_at is null
    and gi.email = (select email from profiles where id = auth.uid())
  for update of gi
  for share of g;

  if not found then
    raise exception 'no pending invitation for you';
  end if;

  update group_invitations
  set status = 'declined'
  where id = inv.id;
end;
$$;

revoke execute on function accept_group_invitation(uuid) from public;
revoke execute on function decline_group_invitation(uuid) from public;
grant execute on function accept_group_invitation(uuid) to authenticated;
grant execute on function decline_group_invitation(uuid) to authenticated;

-- ---------------------------------------------------------------------
-- Standby queue: one function for add, remove and reorder
-- ---------------------------------------------------------------------
-- The client sends the whole queue as it should end up. Positions are
-- rewritten 1..n in one transaction, so `position` is never computed
-- client-side and the queue cannot drift from what the admin saw.
-- event_standby has no write policies; this is the only way in.
create function set_standby_order(eid uuid, ordered_players uuid[])
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  ev  events;
  gid uuid;
  pid uuid;
  i   int := 0;
begin
  -- Group lock before event lock. That order is fixed across every path
  -- that takes both: ensure_admin_remains locks the group, and its
  -- cascade reaches promote_standby, which locks the event. Taking them
  -- the other way round here would deadlock rather than protect.
  --
  -- events.group_id is immutable (column grant in migration 2), so
  -- reading it unlocked is safe.
  select group_id into gid from events where id = eid;
  if not found then
    raise exception 'no such event';
  end if;

  perform 1 from groups
  where id = gid and archived_at is null
  for share;
  if not found then
    raise exception 'group is archived';
  end if;

  select * into ev from events where id = eid for update;
  if not found then
    raise exception 'no such event';
  end if;

  -- security definer means RLS is not checking any of this
  if not is_group_admin(ev.group_id) then
    raise exception 'only an admin can change the standby queue';
  end if;

  -- clock_timestamp(), not now(): now() is fixed at transaction start,
  -- so a request that began before kickoff and then waited on the event
  -- lock would still pass the cutoff after it.
  if ev.starts_at <= clock_timestamp() then
    raise exception 'event has already started';
  end if;

  -- array_length returns null for an empty array, not 0, so coalesce
  -- before comparing or clearing the queue raises 'duplicate player'.
  if coalesce(array_length(ordered_players, 1), 0) is distinct from
     (select count(distinct x) from unnest(ordered_players) x) then
    raise exception 'duplicate player in queue';
  end if;

  foreach pid in array coalesce(ordered_players, '{}'::uuid[]) loop
    if not exists (
      select 1 from group_members
      where group_id = ev.group_id and profile_id = pid
    ) then
      raise exception 'player % is not a member of this group',
        coalesce((select name from profiles where id = pid), pid::text);
    end if;

    -- Someone promoted between the admin loading the screen and
    -- submitting. Fail loudly so the client reloads, rather than
    -- letting the mutual-exclusion trigger raise something opaque.
    if exists (
      select 1 from event_invitations
      where event_id = eid and profile_id = pid
    ) then
      raise exception 'queue is out of date, reload';
    end if;
  end loop;

  delete from event_standby
  where event_id = eid
    and profile_id <> all (coalesce(ordered_players, '{}'::uuid[]));

  foreach pid in array coalesce(ordered_players, '{}'::uuid[]) loop
    i := i + 1;
    insert into event_standby (event_id, group_id, profile_id, position)
    values (eid, ev.group_id, pid, i)
    on conflict (event_id, profile_id)
      do update set position = excluded.position;
  end loop;
end;
$$;

revoke execute on function set_standby_order(uuid, uuid[]) from public;
grant execute on function set_standby_order(uuid, uuid[]) to authenticated;

-- Demoting an invited player to the queue is two ordinary calls: delete
-- their event_invitations row, then set_standby_order with them added.
-- The delete commits first, so promotion pulls the next queued player
-- into the freed slot and the demoted player joins behind them.

-- ---------------------------------------------------------------------
-- Event freeze (rule 14)
-- ---------------------------------------------------------------------
-- Inserts and updates only. Deletes are deliberately exempt: blocking
-- them would make an old group undeletable, since its cascade would
-- trip this on every historic row.
create function reject_if_event_started()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  ts timestamptz;
begin
  select starts_at into ts from events where id = new.event_id;
  if found and ts <= clock_timestamp() then
    raise exception 'event has already started';
  end if;
  return new;
end;
$$;

create trigger event_invitations_frozen
before insert or update on event_invitations
for each row execute function reject_if_event_started();

create trigger event_standby_frozen
before insert or update on event_standby
for each row execute function reject_if_event_started();

create function reject_if_event_started_self()
returns trigger
language plpgsql
as $$
begin
  if old.starts_at <= clock_timestamp() then
    raise exception 'event has already started';
  end if;
  return new;
end;
$$;

create trigger events_frozen
before update on events
for each row execute function reject_if_event_started_self();

-- ---------------------------------------------------------------------
-- Mutual exclusion: invited or queued, never both (rule 13)
-- ---------------------------------------------------------------------
create function reject_if_queued()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if exists (
    select 1 from event_standby
    where event_id = new.event_id and profile_id = new.profile_id
  ) then
    raise exception 'player is on the standby queue for this event';
  end if;
  return new;
end;
$$;

create trigger event_invitations_not_queued
before insert on event_invitations
for each row execute function reject_if_queued();

create function reject_if_invited()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if exists (
    select 1 from event_invitations
    where event_id = new.event_id and profile_id = new.profile_id
  ) then
    raise exception 'player is already invited to this event';
  end if;
  return new;
end;
$$;

create trigger event_standby_not_invited
before insert on event_standby
for each row execute function reject_if_invited();

-- ---------------------------------------------------------------------
-- Capacity invariant (rule 11)
-- ---------------------------------------------------------------------
-- pending + in <= capacity. Spans rows, so a trigger rather than a
-- CHECK. The FOR UPDATE on the event row serialises concurrent writers:
-- without it two simultaneous inserts both read 19 and both commit.
create function enforce_capacity()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  cap      int;
  occupied int;
begin
  -- The lock comes before any early return. Mutual exclusion has no
  -- lock of its own, and relies on this one: an 'out' insert that
  -- returned early could race a standby insert for the same player,
  -- each seeing the other's uncommitted row and neither rejecting.
  select capacity into cap from events where id = new.event_id for update;
  if not found then
    return new;
  end if;

  if new.status not in ('pending', 'in') then
    return new;
  end if;

  select count(*) into occupied
  from event_invitations
  where event_id = new.event_id
    and status in ('pending', 'in')
    and id <> new.id;

  if occupied + 1 > cap then
    raise exception 'event is full';
  end if;

  return new;
end;
$$;

create trigger event_invitations_capacity
before insert or update on event_invitations
for each row execute function enforce_capacity();

-- No matching guard on events: capacity is not updatable (column grant
-- in migration 2), so inviting is the only way to reach the invariant.

-- ---------------------------------------------------------------------
-- Standby promotion (rule 12)
-- ---------------------------------------------------------------------
-- While a slot is free and the queue is non-empty, invite the player at
-- the front. Takes the same event lock as enforce_capacity, so two
-- concurrent declines cannot promote the same person twice.
create function promote_standby(eid uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  cap      int;
  ts       timestamptz;
  gid      uuid;
  occupied int;
  nxt      event_standby;
begin
  -- Group before event, as in set_standby_order. FOR SHARE conflicts
  -- with the UPDATE that archives, so promotion cannot land in a group
  -- being archived concurrently. When this runs deferred inside a
  -- membership change, the caller already holds FOR UPDATE on the same
  -- row, which subsumes this.
  select group_id into gid from events where id = eid;
  if not found then
    return;
  end if;

  perform 1 from groups
  where id = gid and archived_at is null
  for share;
  if not found then
    return;
  end if;

  select capacity, starts_at into cap, ts
  from events where id = eid
  for update;

  -- event gone (cascade) or already started. clock_timestamp() because
  -- this runs deferred, at commit, which can be well after the
  -- transaction's now().
  if not found or ts <= clock_timestamp() then
    return;
  end if;

  loop
    select count(*) into occupied
    from event_invitations
    where event_id = eid and status in ('pending', 'in');

    exit when occupied >= cap;

    select * into nxt
    from event_standby
    where event_id = eid
    order by position
    limit 1;

    exit when not found;

    -- delete first, or the mutual-exclusion trigger rejects the insert
    delete from event_standby
    where event_id = eid and profile_id = nxt.profile_id;

    insert into event_invitations (event_id, group_id, profile_id, status)
    values (eid, nxt.group_id, nxt.profile_id, 'pending');
  end loop;
end;
$$;

revoke execute on function promote_standby(uuid) from public;

create function trigger_promote_standby()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  perform promote_standby(coalesce(new.event_id, old.event_id));
  return null;
end;
$$;

-- Deferred on all three paths, for different reasons:
--   event_invitations update  — a decline frees a slot
--   event_invitations delete  — arrives via ON DELETE CASCADE when a
--                               member is removed from the group; no
--                               client code runs
--   event_standby             — an insert or a reorder can make someone
--                               first, and a reorder is only valid once
--                               it commits
create constraint trigger event_invitations_promote
after update or delete on event_invitations
deferrable initially deferred
for each row execute function trigger_promote_standby();

create constraint trigger event_standby_promote
after insert or update on event_standby
deferrable initially deferred
for each row execute function trigger_promote_standby();
