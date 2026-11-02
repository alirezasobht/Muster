-- Promotion waits while an event is locked: a slot freed by a cascade
-- (member removed, member left, account deleted) stays open until an
-- admin unlocks, and set_event_locked runs promotion then. Rewritten
-- with an empty search path; the body is otherwise unchanged.

create or replace function public.promote_standby(eid uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    cap      int;
    ts       timestamptz;
    locked   timestamptz;
    gid      uuid;
    occupied int;
    nxt      public.event_standby;
    new_id   uuid;
    new_ids  uuid[] := '{}';
begin
    -- Group before event, as in set_standby_order. FOR SHARE conflicts
    -- with the UPDATE that archives, so promotion cannot land in a group
    -- being archived concurrently. When this runs deferred inside a
    -- membership change, the caller already holds FOR UPDATE on the same
    -- row, which subsumes this.
    select e.group_id into gid from public.events e where e.id = eid;
    if not found then
        return;
    end if;

    perform 1 from public.groups g
    where g.id = gid and g.archived_at is null
    for share;
    if not found then
        return;
    end if;

    select e.capacity, e.starts_at, e.locked_at into cap, ts, locked
    from public.events e
    where e.id = eid
    for update;

    -- Event gone (cascade), already started, or locked. clock_timestamp()
    -- because this runs deferred, at commit, which can be well after the
    -- transaction's now().
    if not found or ts <= pg_catalog.clock_timestamp() or locked is not null then
        return;
    end if;

    loop
        select count(*) into occupied
        from public.event_invitations ei
        where ei.event_id = eid and ei.status in ('pending', 'in');

        exit when occupied >= cap;

        select * into nxt
        from public.event_standby es
        where es.event_id = eid
        order by es.position
        limit 1;

        exit when not found;

        -- Delete first, or the mutual-exclusion trigger rejects the insert.
        delete from public.event_standby es
        where es.event_id = eid and es.profile_id = nxt.profile_id;

        insert into public.event_invitations as ei (event_id, group_id, profile_id, status)
        values (eid, nxt.group_id, nxt.profile_id, 'pending')
        returning ei.id into new_id;
        new_ids := new_ids || new_id;
    end loop;

    perform private.send_event_invitation_email(new_ids);
end;
$$;
