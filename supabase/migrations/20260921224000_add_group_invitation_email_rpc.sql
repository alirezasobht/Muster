-- Adds a reusable RPC for sending group invitation emails.
--
-- New group invitations now call this function automatically after the
-- invitation row is created. A future "resend invitation" action can call
-- the same RPC directly.
--
-- Email delivery itself will be added separately through the
-- send-group-invitation Edge Function.

create or replace function public.send_group_invitation_email(
    invitation_id uuid
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    inv public.group_invitations;
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    select *
    into inv
    from public.group_invitations
    where id = invitation_id
      and status = 'pending';

    if not found then
        raise exception 'pending invitation not found';
    end if;

    if not private.is_group_admin(inv.group_id) then
        raise exception 'only an admin can send invitation emails';
    end if;

    -- Email delivery will be added here when the Edge Function is created.
end;
$$;

revoke all
on function public.send_group_invitation_email(uuid)
from public, anon;

grant execute
on function public.send_group_invitation_email(uuid)
to authenticated;


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
    new_invitation_id uuid;
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
    )
    returning id into new_invitation_id;

    perform public.send_group_invitation_email(new_invitation_id);
end;
$$;