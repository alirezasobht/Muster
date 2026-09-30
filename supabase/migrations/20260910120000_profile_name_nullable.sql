-- Muster: profiles.name becomes nullable.
--
-- The account is created when the first sign-in code is requested, which
-- is before anyone has been asked for a name. The old fallback invented
-- one from the email's local part, which made "never set" and "genuinely
-- called that" indistinguishable. Null now means never set, and the app
-- gates on it.
--
-- The existing non-blank CHECK needs no change: a CHECK passes when its
-- expression is null, so it still rejects '' and '   ' while allowing null.

alter table profiles alter column name drop not null;

create or replace function handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into profiles (id, name, email)
  values (
    new.id,
    nullif(trim(new.raw_user_meta_data ->> 'name'), ''),
    lower(trim(new.email))
  );
  return new;
end;
$$;
