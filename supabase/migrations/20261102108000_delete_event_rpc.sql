-- Delete an event, started or locked or neither (CONTEXT.md → Deleting
-- events). Invitations and the queue go by ON DELETE CASCADE, which the
-- freeze and lock triggers exempt. The cascaded invitation deletes queue
-- deferred promotion, which finds the event gone and returns.

create function public.delete_event(event_id uuid)
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

    perform 1 from public.events e
    where e.id = delete_event.event_id and e.group_id = gid
    for update;

    if not found then
        raise exception 'event not found';
    end if;

    delete from public.events e
    where e.id = delete_event.event_id;

    return true;
end;
$$;

revoke all on function public.delete_event(uuid) from public, anon;
grant execute on function public.delete_event(uuid) to authenticated;
