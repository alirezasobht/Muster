-- Move the caller's profile-name update behind an RPC. Profiles exist
-- independently of groups, so no group membership or liveness is required.

create or replace function public.set_profile_name(name text)
returns setof public.profiles
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    return query
    update public.profiles p
    set name = set_profile_name.name
    where p.id = auth.uid()
    returning p.*;

    if not found then
        raise exception 'profile not found';
    end if;
end;
$$;

revoke all on function public.set_profile_name(text) from public, anon;
grant execute on function public.set_profile_name(text) to authenticated;
