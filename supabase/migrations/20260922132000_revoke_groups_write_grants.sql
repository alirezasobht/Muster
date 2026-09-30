-- Apply after the app uses create_group and archive_group. Reads and their
-- RLS policies remain available; definer RPCs and triggers own all writes.

revoke insert, update, delete, truncate on table public.groups
    from public, anon, authenticated;

-- Revoking table UPDATE does not remove the separate column grants.
revoke update (name, archived_at) on table public.groups
    from public, anon, authenticated;
