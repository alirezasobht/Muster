-- Muster: pending-invitation lookup with inviter name.
-- Depends on migrations 1-3.
--
-- Home needs "Invited by <name>" on each invitation card. profiles_select
-- only allows reading your own profile or a co-member's, and an invitee
-- has no group_members row yet — so a plain client-side join cannot reach
-- the inviter's profile. security definer sidesteps that without touching
-- profiles_select: the function runs as its owner, not the caller, so it
-- can read any profile row, but it must therefore do its own access
-- control rather than rely on RLS — same discipline as
-- accept_group_invitation and decline_group_invitation.
create function get_my_pending_invitations()
returns table (
  invitation_id uuid,
  group_id uuid,
  group_name text,
  inviter_name text
)
language sql
security definer
stable
set search_path = public
as $$
  select
    gi.id,
    gi.group_id,
    g.name,
    p.name
  from group_invitations gi
  join groups g on g.id = gi.group_id
  join profiles p on p.id = gi.invited_by
  where gi.status = 'pending'
    and g.archived_at is null
    and gi.email = my_email();
$$;

revoke execute on function get_my_pending_invitations() from public;
grant execute on function get_my_pending_invitations() to authenticated;
