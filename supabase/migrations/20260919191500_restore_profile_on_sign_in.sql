-- Restores a missing public.profiles row whenever an existing Auth user
-- successfully signs in.
--
-- Supabase updates auth.users.last_sign_in_at after a successful login.
-- This trigger uses that update to ensure the user has a matching profile
-- before the authentication transaction completes.
--
-- The restored profile uses the existing auth.users.id UUID, preserving
-- the user's identity and any existing relationships that reference it.
--
-- Existing profiles are left unchanged.

create or replace function public.restore_profile_on_sign_in()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.profiles (
        id,
        name,
        email
    )
    values (
        new.id,
        nullif(trim(new.raw_user_meta_data ->> 'name'), ''),
        lower(trim(new.email))
    )
    on conflict (id) do nothing;

    return new;
end;
$$;

revoke all
on function public.restore_profile_on_sign_in()
from public, anon, authenticated;

drop trigger if exists on_auth_user_sign_in_restore_profile
on auth.users;

create trigger on_auth_user_sign_in_restore_profile
after update of last_sign_in_at
on auth.users
for each row
when (
    new.last_sign_in_at is distinct from old.last_sign_in_at
)
execute function public.restore_profile_on_sign_in();