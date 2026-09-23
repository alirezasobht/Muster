-- Member writes use definer RPCs. Group creation and invitation acceptance
-- also insert through definers; read grants and RLS remain intact.

revoke insert, update, delete, truncate on table public.group_members
    from public, anon, authenticated;

-- The separate column grant survives a table-level UPDATE revoke.
revoke update (role) on table public.group_members
    from public, anon, authenticated;
