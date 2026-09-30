-- supabase/migrations/20260915110000_groups_select_creator_direct.sql

-- Muster: the creator branch of groups_select must not call a function.
-- Depends on migration 6.
--
-- 20260915100000 added `group_is_live(id) and created_by = auth.uid()`.
-- That still fails for INSERT ... RETURNING: group_is_live() is stable
-- and looks the row up in groups, but a stable function runs against the
-- statement's snapshot, which excludes the row that same statement is
-- inserting. The branch never fires, and the insert rolls back with
-- "new row violates row-level security policy for table groups" (42501)
-- -- the same message a genuine can_create_groups rejection produces.
--
-- Testing archived_at directly fixes it: a plain column reference is
-- evaluated against the new tuple, needing no lookup. The meaning is
-- unchanged -- an archived group stays invisible to its own creator.
--
-- The remote database already has this form; migration 6 was corrected
-- by hand after it was applied. This migration exists so a rebuild from
-- scratch produces the same policy as production.
alter policy groups_select on groups
using (
  is_group_member(id)
  or (group_is_live(id) and has_pending_invitation(id))
  or (created_by = auth.uid() and archived_at is null)
);