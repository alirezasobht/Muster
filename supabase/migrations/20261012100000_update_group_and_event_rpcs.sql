-- Group and event editing behind RPCs. update_group takes every editable
-- group field, so a new property extends it rather than adding a call.
-- update_event leaves capacity and the event's group as created.

create or replace function public.update_group(group_id uuid, name text)
returns setof public.groups
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    -- The lock the update itself takes: it waits out an archive, and the
    -- share locks of in-flight roster and invitation writes.
    perform 1 from public.groups g
    where g.id = update_group.group_id and g.archived_at is null
    for no key update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(update_group.group_id) then
        raise exception 'only an admin can edit a group';
    end if;

    return query
    update public.groups g
    set name = update_group.name
    where g.id = update_group.group_id
    returning g.*;
end;
$$;

revoke all on function public.update_group(uuid, text) from public, anon;
grant execute on function public.update_group(uuid, text) to authenticated;

create or replace function public.update_event(
    event_id uuid,
    title text,
    starts_at timestamptz,
    location text default null
)
returns setof public.events
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid;
    current_start timestamptz;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    -- group_id never changes, so reading it unlocked is safe.
    select e.group_id into gid
    from public.events e
    where e.id = update_event.event_id;

    if not found then
        raise exception 'event not found';
    end if;

    perform 1 from public.groups g
    where g.id = gid and g.archived_at is null
    for share;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(gid) then
        raise exception 'only an admin can edit an event';
    end if;

    select e.starts_at into current_start
    from public.events e
    where e.id = update_event.event_id
    for update;

    -- events_frozen would reject this too; checking first keeps it from
    -- reporting as a bad new time.
    if current_start <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    -- Moving an event into the past would freeze it on the spot.
    if update_event.starts_at <= pg_catalog.clock_timestamp() then
        raise exception 'event start time must be in the future';
    end if;

    return query
    update public.events e
    set title = update_event.title,
        starts_at = update_event.starts_at,
        location = update_event.location
    where e.id = update_event.event_id
    returning e.*;
end;
$$;

revoke all on function public.update_event(uuid, text, timestamptz, text) from public, anon;
grant execute on function public.update_event(uuid, text, timestamptz, text) to authenticated;
