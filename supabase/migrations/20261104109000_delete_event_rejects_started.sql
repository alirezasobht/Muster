-- A started event can't be deleted from the app any more; the dashboard
-- still can. Locked and cancelled events stay deletable before kickoff.
-- Cascades from member removal and account deletion don't come through
-- here, so they are unaffected. Otherwise unchanged from 20261102108000;
-- its grants carry over.

create or replace function public.delete_event(event_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    gid uuid;
    ts timestamptz;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select e.group_id into gid
    from public.events e
    where e.id = delete_event.event_id;

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
        raise exception 'only an admin can delete an event';
    end if;

    select e.starts_at into ts
    from public.events e
    where e.id = delete_event.event_id and e.group_id = gid
    for update;

    if not found then
        raise exception 'event not found';
    end if;

    if ts <= pg_catalog.clock_timestamp() then
        raise exception 'event has already started';
    end if;

    delete from public.events e
    where e.id = delete_event.event_id;

    return true;
end;
$$;
