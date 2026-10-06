-- Anyone can have one live group they created; addresses on
-- private.group_creator_emails have no limit. Deleting an account removes
-- its address from the list.

create or replace function private.can_create_groups()
returns boolean
language sql
security definer
stable
set search_path = ''
as $$
    select exists (
        select 1
        from private.group_creator_emails g
        join public.profiles p on p.email = g.email
        where p.id = auth.uid()
    )
    or not exists (
        select 1
        from public.groups g
        where g.created_by = auth.uid()
          and g.archived_at is null
    );
$$;

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

    -- Serialises the caller's creates, so two at once can't both pass the limit.
    perform 1 from public.profiles p
    where p.id = auth.uid()
    for update;

    if not found then
        raise exception 'profile not found';
    end if;

    if not private.can_create_groups() then
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

create or replace function public.delete_account(force boolean)
returns table(group_id uuid, name text)
language plpgsql
security definer
set search_path = ''
as $$
declare
    uid uuid := auth.uid();
    caller_email text;
    sole_admin_ids uuid[];
begin
    if uid is null then
        raise exception 'not signed in';
    end if;

    -- Groups first, in id order: the profile delete cascades into
    -- promote_standby, which locks group then event.
    perform 1
    from public.groups g
    join public.group_members gm on gm.group_id = g.id
    where gm.profile_id = uid
      and gm.role = 'admin'
      and g.archived_at is null
    order by g.id
    for update of g;

    select coalesce(pg_catalog.array_agg(g.id order by g.id), '{}')
    into sole_admin_ids
    from public.groups g
    join public.group_members gm on gm.group_id = g.id
    where gm.profile_id = uid
      and gm.role = 'admin'
      and g.archived_at is null
      and not exists (
          select 1 from public.group_members other
          where other.group_id = g.id
            and other.role = 'admin'
            and other.profile_id <> uid
      );

    if not coalesce(delete_account.force, false)
       and pg_catalog.cardinality(sole_admin_ids) > 0 then
        return query
        select g.id, g.name
        from public.groups g
        where g.id = any (sole_admin_ids)
        order by g.name;
        return;
    end if;

    update public.groups g
    set archived_at = pg_catalog.clock_timestamp()
    where g.id = any (sole_admin_ids);

    -- Invitations and the allowlist match by address, not profile, so the
    -- cascade misses them.
    select pg_catalog.lower(pg_catalog.btrim(u.email))
    into caller_email
    from auth.users u
    where u.id = uid;

    delete from public.group_invitations gi
    where gi.email = caller_email;

    delete from private.group_creator_emails gce
    where gce.email = caller_email;

    delete from auth.users u
    where u.id = uid;
end;
$$;
