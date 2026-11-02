-- Lock or unlock an unstarted event (rule 14b). Idempotent: locking a
-- locked event keeps the original locker. Unlocking runs promotion, which
-- paused while locked, so slots freed meanwhile fill from the queue.

create function public.set_event_locked(event_id uuid, locked boolean)
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

    if set_event_locked.locked is null then
        raise exception 'locked must be true or false';
    end if;

    -- group_id never changes, so reading it unlocked is safe.
    select e.group_id into gid
    from public.events e
    where e.id = set_event_locked.event_id;

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
        raise exception 'only an admin can lock an event';
    end if;

    select e.* into ev
    from public.events e
    where e.id = set_event_locked.event_id
    for update;

    if not found then
        raise exception 'event not found';
    end if;

    if ev.starts_at <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    if set_event_locked.locked and ev.locked_at is null then
        update public.events e
        set locked_at = pg_catalog.clock_timestamp(),
            locked_by = auth.uid()
        where e.id = set_event_locked.event_id;
    elsif not set_event_locked.locked and ev.locked_at is not null then
        update public.events e
        set locked_at = null,
            locked_by = null
        where e.id = set_event_locked.event_id;

        -- After clearing, or promote_standby skips the still-locked event.
        perform public.promote_standby(set_event_locked.event_id);
    end if;

    return query
    select e.*
    from public.events e
    where e.id = set_event_locked.event_id;
end;
$$;

revoke all on function public.set_event_locked(uuid, boolean) from public, anon;
grant execute on function public.set_event_locked(uuid, boolean) to authenticated;
