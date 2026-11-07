-- A cancelled event takes no edits. Otherwise unchanged from
-- 20261102106000.

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
    locked timestamptz;
    cancelled timestamptz;
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

    select e.starts_at, e.locked_at, e.cancelled_at into current_start, locked, cancelled
    from public.events e
    where e.id = update_event.event_id
    for update;

    -- events_frozen would reject both of these too; checking first keeps
    -- them from reporting as a bad new time.
    if current_start <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if cancelled is not null then
        raise exception 'event is cancelled';
    end if;

    if locked is not null then
        raise exception 'event is locked';
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
