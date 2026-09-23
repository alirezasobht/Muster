-- Move event creation behind an RPC. Lock the live group before inserting
-- its event, matching the group-before-event order of the existing RPCs.

create or replace function public.create_event(
    group_id uuid,
    title text,
    starts_at timestamptz,
    capacity integer,
    location text default null
)
returns setof public.events
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    perform 1 from public.groups g
    where g.id = create_event.group_id and g.archived_at is null
    for share;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(create_event.group_id) then
        raise exception 'only an admin can create an event';
    end if;

    return query
    insert into public.events as e (
        group_id, title, starts_at, location, capacity, created_by
    )
    values (
        create_event.group_id,
        create_event.title,
        create_event.starts_at,
        create_event.location,
        create_event.capacity,
        auth.uid()
    )
    returning e.*;

    if not found then
        raise exception 'event was not created';
    end if;
end;
$$;

revoke all on function public.create_event(uuid, text, timestamptz, integer, text)
    from public, anon;
grant execute on function public.create_event(uuid, text, timestamptz, integer, text)
    to authenticated;
