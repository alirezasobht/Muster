-- Hold live-group locks for every invitation write. Existing invitation
-- rows are locked before their group, matching accept/decline's lock order.
-- Constraints and triggers still enforce duplicate and existing-member rules.

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

    perform 1 from public.groups g
    where g.id = invite_group_member_by_email.group_id and g.archived_at is null
    for share;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(invite_group_member_by_email.group_id) then
        raise exception 'only an admin can invite members';
    end if;

    normalized_email := pg_catalog.lower(pg_catalog.btrim(invite_group_member_by_email.email));

    if normalized_email is null or normalized_email = '' then
        raise exception 'email is required';
    end if;

    insert into public.group_invitations as gi (group_id, email, invited_by, status)
    values (
        invite_group_member_by_email.group_id,
        normalized_email,
        auth.uid(),
        'pending'
    )
    returning gi.id into new_invitation_id;

    if not found then
        raise exception 'group invitation was not created';
    end if;

    perform public.send_group_invitation_email(new_invitation_id);
end;
$$;

revoke all on function public.invite_group_member_by_email(uuid, text) from public, anon;
grant execute on function public.invite_group_member_by_email(uuid, text) to authenticated;

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

    perform 1 from public.group_invitations gi
    where gi.group_id = revoke_group_invitation.group_id
      and gi.id = revoke_group_invitation.invitation_id
    for update;

    if not found then
        raise exception 'group invitation not found';
    end if;

    perform 1 from public.groups g
    where g.id = revoke_group_invitation.group_id and g.archived_at is null
    for share;

    if not found then
        raise exception 'group not found or archived';
    end if;

    if not private.is_group_admin(revoke_group_invitation.group_id) then
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

revoke all on function public.revoke_group_invitation(uuid, uuid) from public, anon;
grant execute on function public.revoke_group_invitation(uuid, uuid) to authenticated;

create or replace function public.accept_group_invitation(invitation_id uuid)
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

    select gi.* into inv
    from public.group_invitations gi
    where gi.id = accept_group_invitation.invitation_id
      and gi.status = 'pending'
      and gi.email = (select p.email from public.profiles p where p.id = auth.uid())
    for update;

    if not found then
        raise exception 'no pending invitation for you';
    end if;

    perform 1 from public.groups g
    where g.id = inv.group_id and g.archived_at is null
    for share;

    if not found then
        raise exception 'no pending invitation for you';
    end if;

    insert into public.group_members (group_id, profile_id, role)
    values (inv.group_id, auth.uid(), 'member')
    on conflict do nothing;

    update public.group_invitations gi
    set status = 'accepted'
    where gi.id = inv.id and gi.status = 'pending';

    if not found then
        raise exception 'no pending invitation for you';
    end if;
end;
$$;

revoke all on function public.accept_group_invitation(uuid) from public, anon;
grant execute on function public.accept_group_invitation(uuid) to authenticated;

create or replace function public.decline_group_invitation(invitation_id uuid)
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

    select gi.* into inv
    from public.group_invitations gi
    where gi.id = decline_group_invitation.invitation_id
      and gi.status = 'pending'
      and gi.email = (select p.email from public.profiles p where p.id = auth.uid())
    for update;

    if not found then
        raise exception 'no pending invitation for you';
    end if;

    perform 1 from public.groups g
    where g.id = inv.group_id and g.archived_at is null
    for share;

    if not found then
        raise exception 'no pending invitation for you';
    end if;

    update public.group_invitations gi
    set status = 'declined'
    where gi.id = inv.id and gi.status = 'pending';

    if not found then
        raise exception 'no pending invitation for you';
    end if;
end;
$$;

revoke all on function public.decline_group_invitation(uuid) from public, anon;
grant execute on function public.decline_group_invitation(uuid) to authenticated;
