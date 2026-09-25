-- Account deletion, step 2. Without force, returns the live groups where
-- the caller is the only admin and deletes nothing if there are any. With
-- force, or when there are none, archives those groups and deletes the
-- account. An empty result means the account is gone.

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

    -- Invitations match by address, not profile, so the cascade misses them.
    select pg_catalog.lower(pg_catalog.btrim(u.email))
    into caller_email
    from auth.users u
    where u.id = uid;

    delete from public.group_invitations gi
    where gi.email = caller_email;

    delete from auth.users u
    where u.id = uid;
end;
$$;

revoke all on function public.delete_account(boolean) from public, anon;
grant execute on function public.delete_account(boolean) to authenticated;
