-- Makes private.group_creator_emails the source of group-creation rights.
-- profiles.can_create_groups stays as a mirror for 1.0.x clients, which
-- read it directly, until it is dropped.

insert into private.group_creator_emails (email)
select email
from public.profiles
where can_create_groups
on conflict do nothing;

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
    );
$$;


-- The caller's profile as the app sees it, can_create_groups from the
-- allowlist. A named type rather than returns table: set_profile_name's
-- name parameter would clash with a name output column.
create type public.my_profile as (
    id uuid,
    name text,
    email text,
    can_create_groups boolean
);

create function public.get_my_profile()
returns setof public.my_profile
language sql
security definer
stable
set search_path = ''
as $$
    select p.id, p.name, p.email, private.can_create_groups()
    from public.profiles p
    where p.id = auth.uid();
$$;

revoke execute on function public.get_my_profile() from public, anon;
grant execute on function public.get_my_profile() to authenticated;


-- Returns my_profile too, so the flag comes from the allowlist here as well.
-- The return type changes, so it is dropped first; 1.0.x clients decode the
-- same field names.
drop function public.set_profile_name(text);

create function public.set_profile_name(name text)
returns setof public.my_profile
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    update public.profiles p
    set name = set_profile_name.name
    where p.id = auth.uid();

    if not found then
        raise exception 'profile not found';
    end if;

    return query
    select p.id, p.name, p.email, private.can_create_groups()
    from public.profiles p
    where p.id = auth.uid();
end;
$$;

revoke all on function public.set_profile_name(text) from public, anon;
grant execute on function public.set_profile_name(text) to authenticated;
