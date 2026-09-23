-- Apply after the app uses set_profile_name. Auth's definer triggers still
-- create/restore profiles and sync email; read grants and RLS remain intact.

revoke insert, update, delete, truncate on table public.profiles
    from public, anon, authenticated;

-- The separate column grant survives a table-level UPDATE revoke.
revoke update (name) on table public.profiles
    from public, anon, authenticated;
