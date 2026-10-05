-- Addresses that always get profiles.can_create_groups, so an account on
-- the list (an App Review account, say) keeps the flag after it is deleted
-- and signs up again. Edited in the dashboard. Only ever turns the flag on:
-- removing an address revokes nothing.

create table private.group_creator_emails (
    email text primary key check (email = lower(trim(email)))
);

revoke all on table private.group_creator_emails from public, anon, authenticated;


-- Covers every path that creates or re-addresses a profile:
-- handle_new_user, restore_profile_on_sign_in and sync_user_email.
create function private.apply_group_creator_allowlist()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if exists (
        select 1
        from private.group_creator_emails
        where email = new.email
    ) then
        new.can_create_groups := true;
    end if;
    return new;
end;
$$;

revoke execute on function private.apply_group_creator_allowlist() from public, anon, authenticated;

create trigger profiles_group_creator_allowlist
before insert or update of email on public.profiles
for each row execute function private.apply_group_creator_allowlist();


-- An address added later applies to its existing profile straight away.
create function private.grant_listed_group_creator()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.profiles
    set can_create_groups = true
    where email = new.email
      and not can_create_groups;
    return null;
end;
$$;

revoke execute on function private.grant_listed_group_creator() from public, anon, authenticated;

create trigger group_creator_emails_grant
after insert on private.group_creator_emails
for each row execute function private.grant_listed_group_creator();
