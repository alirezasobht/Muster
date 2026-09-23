-- Move RSVP updates and disinvites behind definer RPCs before revoking
-- invitation write grants. Both lock group, event, then invitation.

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

    perform 1 from public.events e
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

revoke all on function public.set_event_rsvp(uuid, uuid, text) from public, anon;
grant execute on function public.set_event_rsvp(uuid, uuid, text) to authenticated;

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

    perform 1 from public.events e
    where e.id = disinvite_player.event_id and e.group_id = gid
    for update;

    if not found then
        raise exception 'no such event';
    end if;

    if not private.is_group_admin(gid) then
        raise exception 'only an admin can disinvite a player';
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

revoke all on function public.disinvite_player(uuid, uuid) from public, anon;
grant execute on function public.disinvite_player(uuid, uuid) to authenticated;
