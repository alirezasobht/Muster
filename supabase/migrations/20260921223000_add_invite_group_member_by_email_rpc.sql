create or replace function public.invite_group_member_by_email(
    group_id uuid,
    email text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    normalized_email text;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    if not private.is_group_admin(group_id) then
        raise exception 'only an admin can invite members';
    end if;

    normalized_email := lower(trim(email));

    if normalized_email = '' then
        raise exception 'email is required';
    end if;

    insert into public.group_invitations (
        group_id,
        email,
        invited_by,
        status
    )
    values (
        group_id,
        normalized_email,
        auth.uid(),
        'pending'
    );
end;
$$;

revoke all
on function public.invite_group_member_by_email(uuid, text)
from public, anon;

grant execute
on function public.invite_group_member_by_email(uuid, text)
to authenticated;