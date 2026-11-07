-- Cancel an unstarted event, locked or not (rule 14c). One-way: there is
-- no uncancel. Idempotent: cancelling a cancelled event keeps the
-- original canceller. Promotion stops through promote_standby's own check.

create function public.cancel_event(event_id uuid)
returns setof public.events
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid;
    ev public.events;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    -- group_id never changes, so reading it unlocked is safe.
    select e.group_id into gid
    from public.events e
    where e.id = cancel_event.event_id;

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
        raise exception 'only an admin can cancel an event';
    end if;

    select e.* into ev
    from public.events e
    where e.id = cancel_event.event_id
    for update;

    if not found then
        raise exception 'event not found';
    end if;

    if ev.starts_at <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if ev.cancelled_at is null then
        update public.events e
        set cancelled_at = pg_catalog.clock_timestamp(),
            cancelled_by = auth.uid()
        where e.id = cancel_event.event_id;
    end if;

    return query
    select e.*
    from public.events e
    where e.id = cancel_event.event_id;
end;
$$;

revoke all on function public.cancel_event(uuid) from public, anon;
grant execute on function public.cancel_event(uuid) to authenticated;
