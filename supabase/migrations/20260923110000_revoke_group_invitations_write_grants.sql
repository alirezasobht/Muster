-- Apply after the invitation RPC hardening migration. All client writes
-- use definers; read grants and invitation-visibility RLS remain intact.
-- No column-level write grants were introduced for group_invitations.

revoke insert, update, delete, truncate on table public.group_invitations
    from public, anon, authenticated;
