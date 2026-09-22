-- Add an RPC for an admin to revoke a group invitation.
-- This replaces direct client deletes from public.group_invitations.

create or replace function public.revoke_group_invitation(
    group_id uuid,
    invitation_id uuid
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not signed in';
    end if;

    if not private.is_group_admin(group_id) then
        raise exception 'only an admin can revoke invitations';
    end if;

    delete from public.group_invitations gi
    where gi.group_id = revoke_group_invitation.group_id
      and gi.id = revoke_group_invitation.invitation_id;

    if not found then
        raise exception 'group invitation not found';
    end if;
end;
$$;

revoke all on function public.revoke_group_invitation(uuid, uuid) from public;
revoke all on function public.revoke_group_invitation(uuid, uuid) from anon;
grant execute on function public.revoke_group_invitation(uuid, uuid) to authenticated;
