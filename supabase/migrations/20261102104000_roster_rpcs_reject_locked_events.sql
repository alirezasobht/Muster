-- RSVP changes, disinvites and resends stop while an event is locked.
-- The freeze trigger already rejects a locked RSVP update; checking here
-- first gives the clean message. Disinvite is a delete, which the
-- triggers exempt, so it needs its own check. Otherwise unchanged.

create or replace function public.set_event_rsvp(
    event_id uuid,
    profile_id uuid,
    status text
)
returns table (group_id uuid)
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid;
    ts timestamptz;
    locked timestamptz;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select e.group_id into gid
    from public.events e
    where e.id = set_event_rsvp.event_id;

    if not found then
        raise exception 'no such event';
    end if;

    perform 1 from public.groups g
    where g.id = gid and g.archived_at is null
    for share;

    if not found then
        raise exception 'group not found or archived';
    end if;

    select e.starts_at, e.locked_at into ts, locked
    from public.events e
    where e.id = set_event_rsvp.event_id and e.group_id = gid
    for update;

    if not found then
        raise exception 'no such event';
    end if;

    if not private.is_group_member(gid) then
        raise exception 'only a group member can change an RSVP';
    end if;

    if set_event_rsvp.profile_id is distinct from auth.uid()
       and not private.is_group_admin(gid) then
        raise exception 'only an admin can change another player''s RSVP';
    end if;

    if ts <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if locked is not null then
        raise exception 'event is locked';
    end if;

    -- Existing constraints and triggers enforce status, capacity, freeze
    -- and deferred promotion. Return the group for the app's notifications.
    return query
    update public.event_invitations ei
    set status = set_event_rsvp.status
    where ei.event_id = set_event_rsvp.event_id
      and ei.profile_id = set_event_rsvp.profile_id
      and ei.group_id = gid
    returning ei.group_id;

    if not found then
        raise exception 'event invitation not found';
    end if;
end;
$$;

create or replace function public.disinvite_player(
    event_id uuid,
    profile_id uuid
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid;
    locked timestamptz;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select e.group_id into gid
    from public.events e
    where e.id = disinvite_player.event_id;

    if not found then
        raise exception 'no such event';
    end if;

    perform 1 from public.groups g
    where g.id = gid and g.archived_at is null
    for share;

    if not found then
        raise exception 'group not found or archived';
    end if;

    select e.locked_at into locked
    from public.events e
    where e.id = disinvite_player.event_id and e.group_id = gid
    for update;

    if not found then
        raise exception 'no such event';
    end if;

    if not private.is_group_admin(gid) then
        raise exception 'only an admin can disinvite a player';
    end if;

    if locked is not null then
        raise exception 'event is locked';
    end if;

    -- Deletes remain exempt from the freeze. Deferred promotion still runs
    -- through its existing trigger, which skips events that have started.
    delete from public.event_invitations ei
    where ei.event_id = disinvite_player.event_id
      and ei.profile_id = disinvite_player.profile_id
      and ei.group_id = gid;

    if not found then
        raise exception 'event invitation not found';
    end if;

    return true;
end;
$$;

create or replace function public.resend_event_invitation(
    event_id uuid,
    profile_id uuid,
    tz text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid;
    ts timestamptz;
    locked timestamptz;
    inv public.event_invitations;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select e.group_id into gid
    from public.events e
    where e.id = resend_event_invitation.event_id;

    if not found then
        raise exception 'no such event';
    end if;

    perform 1 from public.groups g
    where g.id = gid and g.archived_at is null
    for share;

    if not found then
        raise exception 'group not found or archived';
    end if;

    select e.starts_at, e.locked_at into ts, locked
    from public.events e
    where e.id = resend_event_invitation.event_id and e.group_id = gid
    for update;

    if not found then
        raise exception 'no such event';
    end if;

    if not private.is_group_admin(gid) then
        raise exception 'only an admin can resend an event invitation';
    end if;

    if ts <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if locked is not null then
        raise exception 'event is locked';
    end if;

    select ei.* into inv
    from public.event_invitations ei
    where ei.event_id = resend_event_invitation.event_id
      and ei.profile_id = resend_event_invitation.profile_id
      and ei.group_id = gid
      and ei.status = 'pending'
    for update;

    if not found then
        raise exception 'event invitation not found';
    end if;

    if private.sent_within_today(inv.last_sent_at, resend_event_invitation.tz) then
        raise exception 'invitation sent too recently';
    end if;

    perform private.send_event_invitation_email(array[inv.id]);
end;
$$;
