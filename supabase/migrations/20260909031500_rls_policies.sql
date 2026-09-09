-- Muster: RLS policies.
-- Depends on migration 1 (tables, RLS already enabled).
--
-- Helper functions live here rather than in migration 3 because the
-- policies below call them. They must be `security definer`: a policy on
-- group_members that queries group_members would recurse infinitely.
-- `stable` lets the planner cache them within a statement.

-- helpers --------------------------------------------------------------
-- Both helpers exclude archived groups. That single condition is what
-- hides and freezes an archived group everywhere: every policy on
-- groups, events, event_invitations and event_standby routes through
-- one of these, so no separate archived check is needed downstream.
create function is_group_member(gid uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1
    from group_members gm
    join groups g on g.id = gm.group_id
    where gm.group_id = gid
      and gm.profile_id = auth.uid()
      and g.archived_at is null
  );
$$;

create function is_group_admin(gid uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1
    from group_members gm
    join groups g on g.id = gm.group_id
    where gm.group_id = gid
      and gm.profile_id = auth.uid()
      and gm.role = 'admin'
      and g.archived_at is null
  );
$$;

-- Reads from profiles, not the JWT, so it matches the lowercase-
-- normalised value stored on the invitation.
create function my_email()
returns text
language sql
security definer
stable
set search_path = public
as $$
  select email from profiles where id = auth.uid();
$$;

create function can_create_groups()
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select coalesce(
    (select can_create_groups from profiles where id = auth.uid()),
    false
  );
$$;

-- Archived check on its own, for the paths that are not membership-
-- based — an invitee has no membership row to check.
create function group_is_live(gid uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1 from groups where id = gid and archived_at is null
  );
$$;

-- Lets an invitee see the group's name before accepting. Without it the
-- invitation screen has an id and nothing to display.
create function has_pending_invitation(gid uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1
    from group_invitations gi
    join profiles p on p.id = auth.uid()
    where gi.group_id = gid
      and gi.email = p.email
      and gi.status = 'pending'
  );
$$;

-- profiles -------------------------------------------------------------
-- Readable by yourself and by anyone sharing a group with you, so player
-- names can be shown on event lists.
create policy profiles_select on profiles
for select to authenticated
using (
  id = auth.uid()
  or exists (
    select 1
    from group_members mine
    join group_members theirs on theirs.group_id = mine.group_id
    where mine.profile_id = auth.uid()
      and theirs.profile_id = profiles.id
  )
);

create policy profiles_update on profiles
for update to authenticated
using (id = auth.uid())
with check (id = auth.uid());

-- can_create_groups is not self-editable; it is set in the dashboard.
revoke update on profiles from authenticated;
grant update (name) on profiles to authenticated;

-- No insert policy: the signup trigger (migration 3) creates the row.
-- No delete policy: profile deletion is not a feature.

-- groups ---------------------------------------------------------------
-- Members see their groups. An invitee sees the group row — the whole
-- row, since RLS filters rows and not columns — so the invitation
-- screen has something to show. Every other table stays behind
-- is_group_member.
create policy groups_select on groups
for select to authenticated
using (
  is_group_member(id)
  or (group_is_live(id) and has_pending_invitation(id))
);

-- Signup is open to anyone, but creating a group is allowlisted: the
-- flag is off by default and flipped by hand. The creator's admin
-- membership row is written by a trigger in migration 3, in the same
-- transaction.
create policy groups_insert on groups
for insert to authenticated
with check (created_by = auth.uid() and can_create_groups());

-- Archiving is an update setting archived_at. WITH CHECK deliberately
-- does not call is_group_admin: the helper excludes archived groups, so
-- re-checking it against the new row would reject the very update that
-- archives it. Column privileges bound what can be written instead.
create policy groups_update on groups
for update to authenticated
using (is_group_admin(id))
with check (true);

revoke update on groups from authenticated;
grant update (name, archived_at) on groups to authenticated;

-- No delete policy. Groups are archived, never destroyed — the records
-- have to survive for a restore to mean anything.

-- group_members --------------------------------------------------------
create policy group_members_select on group_members
for select to authenticated
using (is_group_member(group_id));

-- No insert policy. Rows arrive only two ways, both security definer:
-- the group-creation trigger, and accept_group_invitation().

-- Promote / demote. The last-admin guard is a trigger in migration 3.
create policy group_members_update on group_members
for update to authenticated
using (is_group_admin(group_id))
with check (is_group_admin(group_id));

-- role is the only thing an admin may change. Without this an admin
-- could rewrite profile_id on an existing row and add any registered
-- user to the group without them ever accepting an invitation, or move
-- a membership row into another group they administer.
revoke update on group_members from authenticated;
grant update (role) on group_members to authenticated;

-- Admins remove members; anyone may remove themselves (leaving).
create policy group_members_delete on group_members
for delete to authenticated
using (is_group_admin(group_id) or profile_id = auth.uid());

-- group_invitations ----------------------------------------------------
-- Two audiences: the invitee, who has no membership row yet and is
-- matched by email, and the group's members, who see the pending list.
-- Both are gated on the group being live, or an archived group's
-- invitations would stay visible.
create policy group_invitations_select on group_invitations
for select to authenticated
using (
  group_is_live(group_id)
  and (email = my_email() or is_group_member(group_id))
);

create policy group_invitations_insert on group_invitations
for insert to authenticated
with check (is_group_admin(group_id) and invited_by = auth.uid());

-- No update policy. Accept and decline both go through the RPCs in
-- migration 3.

-- Revoking an invitation.
create policy group_invitations_delete on group_invitations
for delete to authenticated
using (is_group_admin(group_id));

-- events ---------------------------------------------------------------
create policy events_select on events
for select to authenticated
using (is_group_member(group_id));

create policy events_insert on events
for insert to authenticated
with check (is_group_admin(group_id) and created_by = auth.uid());

-- The starts_at freeze is a trigger in migration 3.
create policy events_update on events
for update to authenticated
using (is_group_admin(group_id))
with check (is_group_admin(group_id));

-- capacity is set once, at creation, and never edited. That keeps the
-- pending + in <= capacity invariant to a single entry point (inviting
-- players) instead of two. Pinning group_id also stops an admin of two
-- groups moving an event between them.
revoke update on events from authenticated;
grant update (title, starts_at, location) on events to authenticated;

create policy events_delete on events
for delete to authenticated
using (is_group_admin(group_id));

-- event_invitations ----------------------------------------------------
-- Everyone in the group reads every RSVP; that is the event screen.
create policy event_invitations_select on event_invitations
for select to authenticated
using (is_group_member(group_id));

create policy event_invitations_insert on event_invitations
for insert to authenticated
with check (is_group_admin(group_id));

-- Your own RSVP, or an admin changing anyone's.
create policy event_invitations_update on event_invitations
for update to authenticated
using (profile_id = auth.uid() or is_group_admin(group_id))
with check (profile_id = auth.uid() or is_group_admin(group_id));

create policy event_invitations_delete on event_invitations
for delete to authenticated
using (is_group_admin(group_id));

-- RLS is row-level only, so without this a member could satisfy the
-- policy above and still rewrite event_id or profile_id on their own
-- row. Column privileges close that. status is the only field anyone
-- edits by hand.
revoke update on event_invitations from authenticated;
grant update (status) on event_invitations to authenticated;

-- event_standby --------------------------------------------------------
-- Read-only over the API. Every write goes through set_standby_order()
-- in migration 3, which takes the whole queue at once — so adding,
-- removing and reordering are one operation and `position` is never
-- client-computed.
create policy event_standby_select on event_standby
for select to authenticated
using (is_group_member(group_id));

revoke insert, update, delete on event_standby from authenticated;
