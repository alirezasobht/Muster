-- Muster: let a group's creator see it immediately after INSERT ... RETURNING.
-- Depends on migration 2.
--
-- groups_select requires is_group_member(id), which depends on the
-- group_members row on_group_created writes. That trigger is AFTER INSERT,
-- which fires only after the same statement's RETURNING projection has
-- already been computed and rechecked against groups_select. So every
-- client-side "insert into groups ... returning" fails groups_select's
-- recheck and rolls back with "new row violates row-level security policy
-- for table \"groups\"" (42501) -- indistinguishable from a genuine
-- can_create_groups() rejection, and unconditional: it happens for every
-- creator regardless of the flag.
--
-- Adding created_by = auth.uid() closes the gap without weakening
-- anything: the creator becomes a member microseconds later in the same
-- transaction anyway. group_is_live(id) keeps archived groups invisible
-- to their own creator, matching every other branch of this policy.
alter policy groups_select on groups
using (
  is_group_member(id)
  or (group_is_live(id) and has_pending_invitation(id))
  or (group_is_live(id) and created_by = auth.uid())
);
