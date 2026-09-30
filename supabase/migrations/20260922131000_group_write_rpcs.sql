-- Move group creation and archiving behind RPCs. Creator and archive time
-- come from the database; the existing trigger still creates the admin row.

create or replace function public.create_group(name text)
returns setof public.groups
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    -- Hold the allowlist decision until the group has been created.
    perform 1 from public.profiles p
    where p.id = auth.uid() and p.can_create_groups
    for share;

    if not found then
        -- Keep the client's existing NotAllowedToCreateGroups mapping.
        raise exception 'permission denied to create "groups"'
            using errcode = '42501';
    end if;

    return query
    insert into public.groups (name, created_by)
    values (create_group.name, auth.uid())
    returning *;

    if not found then
        raise exception 'group was not created';
    end if;
end;
$$;

revoke all on function public.create_group(text) from public, anon;
grant execute on function public.create_group(text) to authenticated;

create or replace function public.archive_group(group_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    -- Conflicts with the share locks that protect live-group operations.
    -- No event rows are touched: the group remains the first lock.
    perform 1 from public.groups g
    where g.id = archive_group.group_id and g.archived_at is null
    for update;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(group_id) then
        raise exception 'only an admin can archive a group';
    end if;

    update public.groups g
    set archived_at = pg_catalog.clock_timestamp()
    where g.id = archive_group.group_id and g.archived_at is null;

    if not found then
        raise exception 'group not found or archived';
    end if;
end;
$$;

revoke all on function public.archive_group(uuid) from public, anon;
grant execute on function public.archive_group(uuid) to authenticated;
